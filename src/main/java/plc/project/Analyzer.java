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
    public Void visit(Ast.Global ast) {
        Environment.Type variableType = Environment.getType(ast.getTypeName());

        //global var declaration has an initial value, visit + process
        if (ast.getValue().isPresent()) {
            Ast.Expression valueExpression = ast.getValue().get();
            if (valueExpression instanceof Ast.Expression.Literal) {
                visit((Ast.Expression.Literal) valueExpression);
            } else if (valueExpression instanceof Ast.Expression.Binary) {
                visit((Ast.Expression.Binary) valueExpression);
            }
            Environment.Type valueType = valueExpression.getType();

            requireAssignable(variableType, valueType);
        }

        //define var in its scope
        try {
            Environment.Variable definedVariable = scope.defineVariable(ast.getName(), ast.getName(), variableType, ast.getMutable(), Environment.NIL);
            ast.setVariable(definedVariable);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error defining global variable: " + ast.getName(), e);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        //throw new UnsupportedOperationException();  // TODO
        Environment.Type valueType = ast.getValue().getType();
        Environment.Type receiverType = ast.getReceiver().getType();
        requireAssignable(receiverType, valueType);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException();  // TODO
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
        if (!type.equals(target) && !target.equals(Environment.Type.ANY) && !(target.equals(Environment.Type.COMPARABLE) && (type.equals(Environment.Type.INTEGER) || type.equals(Environment.Type.DECIMAL) || type.equals(Environment.Type.CHARACTER) || type.equals(Environment.Type.STRING)))) {
            throw new RuntimeException("Type " + type + " is not assignable to " + target);
        }
    }

}
