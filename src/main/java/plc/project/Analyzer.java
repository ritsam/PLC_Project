package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }

        boolean foundMainFunct = false;

        //visit all functions+define
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
            if (function.getName().equals("main") && function.getParameters().isEmpty()) {
                foundMainFunct = true;
                if (!function.getReturnTypeName().orElse("").equals("Integer")) {
                    throw new RuntimeException("Main function must have an Integer return type.");
                }
            }
        }
        if (!foundMainFunct) {
            throw new RuntimeException("Main function not found.");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) { //in prog
        Environment.Type variableType = Environment.getType(ast.getTypeName());

        //global var declaration has an initial value, visit + process
        if (ast.getValue().isPresent()) {
            Ast.Expression valueExpression = ast.getValue().get();
            if (valueExpression instanceof Ast.Expression.Literal) {
                visit((Ast.Expression.Literal) valueExpression);
            }
            else if (valueExpression instanceof Ast.Expression.Binary) {
                visit((Ast.Expression.Binary) valueExpression);
            }
            Environment.Type valueType = valueExpression.getType();

            requireAssignable(variableType, valueType);
        }

        //define var in its scope
        try {
            Environment.Variable definedVariable = scope.defineVariable(ast.getName(), ast.getName(), variableType, ast.getMutable(), Environment.NIL);
            ast.setVariable(definedVariable);
        }
        catch (RuntimeException e) {
            throw new RuntimeException("Error defining global variable: " + ast.getName(), e);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> paramTypes = ast.getParameterTypeNames().stream().map(Environment::getType).collect(Collectors.toList());
        Environment.Type returnType = ast.getReturnTypeName().map(Environment::getType).orElse(Environment.Type.NIL);

        Environment.Function envFunction = scope.defineFunction(ast.getName(), ast.getName(), paramTypes, returnType, args -> Environment.NIL);
        ast.setFunction(envFunction);

        //new scope for functions body with defined parameters
        Scope originalScope = this.scope;
        this.scope = new Scope(originalScope);
        for (int i = 0; i < ast.getParameters().size(); i++) {
            this.scope.defineVariable(ast.getParameters().get(i), ast.getParameterTypeNames().get(i), paramTypes.get(i), true, Environment.NIL);
        }

        //visit each statement in functions body
        for (Ast.Statement statement : ast.getStatements()) {
            if (statement instanceof Ast.Statement.Expression) {
                visit((Ast.Statement.Expression) statement);
            }
            else if (statement instanceof Ast.Statement.Declaration) {
                visit((Ast.Statement.Declaration) statement);
            }
            else if (statement instanceof Ast.Statement.Assignment) {
                visit((Ast.Statement.Assignment) statement);
            }
            else if (statement instanceof Ast.Statement.If) {
                visit((Ast.Statement.If) statement);
            }
            else if (statement instanceof Ast.Statement.While) {
                visit((Ast.Statement.While) statement);
            }
            else if (statement instanceof Ast.Statement.Return) {
                visit((Ast.Statement.Return) statement);
            }
            //any other statement types
        }
//restore scope
        this.scope = originalScope;

        return null;
    }

    @Override

    public Void visit(Ast.Statement.Expression ast) { //in progress
        //throw new UnsupportedOperationException();  // TODO
        if (ast.getExpression() instanceof Ast.Expression.Function) {
            Ast.Expression.Function functExpr = (Ast.Expression.Function) ast.getExpression();
            visit(functExpr);
        } else {
            throw new RuntimeException("Only function calls are allowed as statements. Invalid expression type. ");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) { //in progress
        Environment.Type variableType;
        if (ast.getTypeName().isPresent()) {
            variableType = Environment.getType(ast.getTypeName().get());
        } else {
            if (ast.getValue().isPresent()) {
                Ast.Expression valueExpression = ast.getValue().get();
                if (valueExpression instanceof Ast.Expression.Literal) {
                    visit((Ast.Expression.Literal) valueExpression);
                } else if (valueExpression instanceof Ast.Expression.Binary) {
                    visit((Ast.Expression.Binary) valueExpression);
                }
                variableType = valueExpression.getType();
            } else {
                throw new RuntimeException("Variable declaration is missing a type name or an initial value.");
            }
        }
        if (ast.getValue().isPresent()) {
            Environment.Type valueType = ast.getValue().get().getType();
            requireAssignable(variableType, valueType);
        }
        try {
            scope.defineVariable(ast.getName(), ast.getName(), variableType, true, Environment.NIL);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error defining variable: " + ast.getName(), e);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("No Access Expression");
        }
        Environment.Type valueType = ast.getValue().getType();
        Environment.Type receiverType = ast.getReceiver().getType();
        requireAssignable(receiverType, valueType);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        Ast.Function currentFunction = this.function;

        visit(ast.getCondition());

        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("The condition of an 'if' statement must be of type Boolean.");
        }

        Scope originalScope = this.scope;
        this.scope = new Scope(originalScope);

        for (Ast.Statement thenStatement : ast.getThenStatements()) {
            visit(thenStatement);
        }
        this.scope = originalScope;

        if (ast.getElseStatements() != null) {
            this.scope = new Scope(originalScope);
            for (Ast.Statement elseStatement : ast.getElseStatements()) {
                visit(elseStatement);
            }
            this.scope = originalScope;
        }

        this.function = currentFunction;

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        //throw new UnsupportedOperationException();  // TODO
            Scope originalScope = this.scope;
            scope = new Scope(originalScope);
        try {
            Ast.Expression swi = ast.getCondition();
            visit(swi);
            boolean hasCase = false;
            for (Ast.Statement.Case switchCase : ast.getCases()) {
                if (switchCase.getValue().isEmpty() || swi.equals(switchCase.getValue().get())) {
                    hasCase = true;
                    visit(switchCase);
                    break;
                }
            }
        }
        finally{
            scope = originalScope;
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        //throw new UnsupportedOperationException();  // TODO
        Scope originalScope = this.scope;
        scope = new Scope(originalScope);
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
        }
        for (Ast.Statement stmt : ast.getStatements()) {
            visit(stmt);
        }
        scope = originalScope;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        // TODO done
        try{
            visit(ast.getCondition());
            if (ast.getCondition().getType() != Environment.Type.BOOLEAN) {
                scope = new Scope(scope);
                for (Ast.Statement st : ast.getStatements()) {
                    visit(st);
                }
            }
        }
        catch (RuntimeException r) {
            throw new RuntimeException(r);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) { // TODO done
        try{
            visit(ast.getValue());
            Environment.Variable returnType = scope.lookupVariable("returnType");
            requireAssignable(returnType.getType(), ast.getValue().getType());
        }
        catch (RuntimeException e) {
            throw new RuntimeException( e);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        //throw new UnsupportedOperationException();  // TODO
        //exception: integer and decimal
        try {
            if (ast.getLiteral() == Environment.NIL) {
                ast.setType(Environment.Type.NIL);
            }
            if (ast.getLiteral() instanceof Boolean) {
                ast.setType(Environment.Type.BOOLEAN);
            }
            if (ast.getLiteral() instanceof Character) {
                ast.setType(Environment.Type.CHARACTER);
            }
            if (ast.getLiteral() instanceof String) {
                ast.setType(Environment.Type.STRING);
            }
            if (ast.getLiteral() instanceof BigInteger) {
                if (((BigInteger)(ast.getLiteral())).compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0 && (((BigInteger) (ast.getLiteral())).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 ))
                    ast.setType(Environment.Type.INTEGER);
                else
                    throw new RuntimeException("Integer is not in range.");
                return null;
            }
            if (ast.getLiteral() instanceof BigDecimal) {
                if (((BigDecimal)(ast.getLiteral())).compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) <= 0 && (((BigDecimal)(ast.getLiteral())).compareTo(BigDecimal.valueOf(Double.MIN_VALUE)) >= 0))
                    ast.setType(Environment.Type.DECIMAL);
                else
                    throw new RuntimeException("Decimal is not in range.");
                return null;
            }
        }
        catch (RuntimeException r) {
            throw new RuntimeException(r);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        // TODO done
        try {
            if (ast.getExpression() instanceof Ast.Expression.Binary) {
                visit(ast.getExpression());
                ast.setType(ast.getExpression().getType());
            }
        }
        catch (RuntimeException r) {
            throw new RuntimeException("No Ast.Expression.Binary");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());

        Environment.Type leftType = ast.getLeft().getType();
        Environment.Type rightType = ast.getRight().getType();

        switch (ast.getOperator()) {
            case "&&":
            case "||":
                if (!leftType.equals(Environment.Type.BOOLEAN) || !rightType.equals(Environment.Type.BOOLEAN)) {
                    throw new RuntimeException("Logical operations require both operands to be Boolean.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "<":
            case ">":
            case "==":
            case "!=":
                if (!leftType.equals(rightType)) {
                    throw new RuntimeException("Comparison operations require operands of the same type.");
                }
                ast.setType(Environment.Type.BOOLEAN);
                break;
            case "+":
                if (leftType.equals(Environment.Type.STRING) || rightType.equals(Environment.Type.STRING)) {
                    ast.setType(Environment.Type.STRING);
                } else if ((leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.DECIMAL)) ||
                        (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.INTEGER))) {
                    // Throw a RuntimeException for Integer + Decimal or Decimal + Integer
                    throw new RuntimeException("Cannot perform addition between Integer and Decimal.");
                } else if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else if (leftType.equals(Environment.Type.DECIMAL) && rightType.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("Addition requires compatible types.");
                }
                break;
            case "-":
            case "*":
            case "/":
                if (leftType.equals(Environment.Type.DECIMAL) || rightType.equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else {
                    throw new RuntimeException("Arithmetic operations require numeric types.");
                }
                break;
            case "^":
                if (leftType.equals(Environment.Type.INTEGER) && rightType.equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else {
                    throw new RuntimeException("Exponentiation requires Integer operands.");
                }
                break;
            default:
                throw new RuntimeException("Unsupported binary operator: " + ast.getOperator());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        Environment.Variable variable = scope.lookupVariable(ast.getName());
        if (variable == null) {
            throw new RuntimeException("Variable '" + ast.getName() + "' not found.");
        }
        ast.setVariable(variable);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        //throw new UnsupportedOperationException();  // TODO
        try {
            if (!type.equals(target) && !target.equals(Environment.Type.ANY) && !(target.equals(Environment.Type.COMPARABLE) && (type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING)))) {
                throw new RuntimeException("Type " + type + " is not assignable to " + target);
            }
        }
        catch (RuntimeException r) {
            throw new RuntimeException(r);
        }
    }

}