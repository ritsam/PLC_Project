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
    private Environment.Type currentFunctionReturnType = null; //for the statement.return function
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
        //for the statement.return function
        currentFunctionReturnType = Environment.getType(ast.getReturnTypeName().orElse("Nil"));

        Environment.Function envFunction = scope.defineFunction(ast.getName(), ast.getName(), paramTypes, currentFunctionReturnType, args -> Environment.NIL);
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
        }
//restore scope
        this.scope = originalScope;
        currentFunctionReturnType = null; //reset at end of visit (for statement.return function)
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
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type declaredType;
        if (ast.getTypeName().isPresent()) {
            declaredType = Environment.getType(ast.getTypeName().get());
        } else if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            declaredType = ast.getValue().get().getType();
        } else {
            throw new RuntimeException("Declaration requires either a type or an initial value.");
        }

        Environment.Variable definedVariable = scope.defineVariable(ast.getName(), ast.getName(), declaredType, true, Environment.NIL);

        ast.setVariable(definedVariable);

        if (ast.getValue().isPresent()) {
            requireAssignable(declaredType, ast.getValue().get().getType());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Assignment receiver must be an access expression.");
        }
        visit(ast.getValue());

        visit(ast.getReceiver());

        Environment.Type valueType = ast.getValue().getType();
        Ast.Expression.Access accessReceiver = (Ast.Expression.Access) ast.getReceiver();

        if (accessReceiver.getVariable() == null) {
            throw new RuntimeException("Variable not found: " + accessReceiver.getName());
        }

        Environment.Type receiverType = accessReceiver.getVariable().getType();

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

        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("The 'then' block of an 'if' statement cannot be empty.");
        }

        Scope originalScope = this.scope;
        this.scope = new Scope(originalScope);

        for (Ast.Statement thenStatement : ast.getThenStatements()) {
            visit(thenStatement);
        }
        this.scope = originalScope;

        if (!ast.getElseStatements().isEmpty()) {
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
        visit(ast.getCondition());
        Environment.Type conditionType = ast.getCondition().getType();

        boolean defaultCaseFound = false;
        Scope originalScope = this.scope;

        for (int i = 0; i < ast.getCases().size(); i++) {
            Ast.Statement.Case switchCase = ast.getCases().get(i);
            this.scope = new Scope(originalScope);

            if (switchCase.getValue().isPresent()) {
                //validate case value type matches condition type
                visit(switchCase.getValue().get());
                Environment.Type caseValueType = switchCase.getValue().get().getType();
                if (!caseValueType.equals(conditionType)) {
                    throw new RuntimeException("Case value type does not match the type of the condition.");
                }
            }
            else {
                if (defaultCaseFound || i != ast.getCases().size() - 1) {
                    throw new RuntimeException("DEFAULT case must be the last case and appear only once.");
                }
                defaultCaseFound = true;
            }

            for (Ast.Statement statement : switchCase.getStatements()) {
                visit(statement);
            }
        }

        this.scope = originalScope;

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
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
        visit(ast.getCondition());

        if (ast.getCondition().getType() != Environment.Type.BOOLEAN) {
            throw new RuntimeException("Condition of while statement must be of type Boolean.");
        }

        Scope originalScope = scope;
        scope = new Scope(originalScope);

        try {
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
            }
        }
        finally {
            scope = originalScope;
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        if (currentFunctionReturnType == null) {
            throw new IllegalStateException("Return statement not within a function.");
        }
        try{
            visit(ast.getValue());
            requireAssignable(currentFunctionReturnType, ast.getValue().getType());
        }
        catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
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
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Grouped expression must be a binary expression.");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
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
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        if (function == null) {
            throw new RuntimeException("Function '" + ast.getName() + "' with " + ast.getArguments().size() + " arguments not found.");
        }
        ast.setFunction(function);

        for (int i = 0; i < ast.getArguments().size(); i++) {
            Ast.Expression argument = ast.getArguments().get(i);
            visit(argument);

            Environment.Type expectedType = function.getParameterTypes().get(i);
            Environment.Type actualType = argument.getType();
            requireAssignable(expectedType, actualType);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) { /// IN PROGRESS
        if (ast.getValues().isEmpty()) {
            return null;
        }

        visit(ast.getValues().get(0));
        Environment.Type expectedType = ast.getValues().get(0).getType();

        for (int i = 1; i < ast.getValues().size(); i++) {
            Ast.Expression value = ast.getValues().get(i);
            visit(value);

            Environment.Type valueType = value.getType();
            if (!valueType.equals(expectedType)) {
                throw new RuntimeException("All elements in the list must have the same type. Expected " + expectedType + ", found " + valueType + ".");
            }
        }

        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
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