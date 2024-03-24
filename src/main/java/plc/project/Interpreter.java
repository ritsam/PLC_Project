package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;


public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        // throw new UnsupportedOperationException(); //TODO done
        List<Ast.Global> global = ast.getGlobals();
        for (int i = 0; i < global.size(); i++) {
            Ast.Global g = global.get(i);
            visit(g);
        }
        List<Ast.Function> function = ast.getFunctions();
        for (int i = 0; i < function.size(); i++) {
            Ast.Function f = function.get(i);
            visit(f);
        }
        try {
            return scope.lookupFunction("main", 0).invoke(Collections.emptyList());
        } catch (RuntimeException e) {
            throw new RuntimeException("Main function not found in source.");
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        //throw new UnsupportedOperationException(); //TODO done
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        //TODO done
        visit(ast.getExpression()); //evalutes
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        //TODO done (in lecture)
        if(ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        }
        else{
            scope.defineVariable(ast.getName(),true,Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        //throw new UnsupportedOperationException(); //TODO done
        Ast.Expression receiverExpression = ast.getReceiver();
        if (!(receiverExpression instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Only variables or list elements can be assigned.");
        }
        Ast.Expression.Access access = (Ast.Expression.Access) receiverExpression;
        Environment.Variable variable = scope.lookupVariable(access.getName());
        if (!variable.getMutable()) {
            throw new RuntimeException("Immutable variable- cant assign.");
        }
        if (access.getOffset().isPresent()) {
            Environment.PlcObject listObject = variable.getValue();
            if (!(listObject.getValue() instanceof List)) {
                throw new RuntimeException("Variable is not a list - offset is present.");
            }
            List<Environment.PlcObject> list = (List<Environment.PlcObject>) listObject.getValue();
            int index = requireType(Number.class, visit(access.getOffset().get())).intValue();
            if (index < 0 || index >= list.size()) {
                throw new RuntimeException("List index out of bounds.");
            }
            list.set(index, visit(ast.getValue()));
        } else {
            variable.setValue(visit(ast.getValue()));
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        //TODO done
        while (requireType(Boolean.class, visit(ast.getCondition())) != null) {
            try {
                scope = new Scope(scope);
                if (!((Boolean) visit(ast.getCondition()).getValue())) { //false-else
                    for (Ast.Statement e : ast.getElseStatements()) {
                        visit(e);
                    }
                } else {
                    for (Ast.Statement t : ast.getThenStatements()) { //then
                        visit(t);
                    }
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();//TODO

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        //throw new UnsupportedOperationException(); //TODO done
        for (Ast.Statement stmt : ast.getStatements()) {
            visit(stmt);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        //TODO done (in lecture)
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Statement stmt : ast.getStatements()) {
                    visit(stmt);
                }
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        //TODO done
        Environment.PlcObject value = visit(ast.getValue());
        throw new Return(value);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        //TODO done
        //	Returns literal asPlcObject
        return (ast.getLiteral() == null) ? Environment.NIL : Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        //TODO done
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.Variable variable = scope.lookupVariable(ast.getName());
        if (ast.getOffset().isPresent()) {
            if (!(variable.getValue().getValue() instanceof List)) {
                throw new RuntimeException("Variable '" + ast.getName() + "' is not a list.");
            }
            List<Environment.PlcObject> list = (List<Environment.PlcObject>) variable.getValue().getValue();
            int index = requireType(BigInteger.class, visit(ast.getOffset().get())).intValue();
            if (index < 0 || index >= list.size()) {
                throw new RuntimeException("List index out of bounds.");
            }
            return list.get(index);
        } else {
            return variable.getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        //throw new UnsupportedOperationException(); //TODO done
        try {
            scope = new Scope(scope);
            List<Environment.PlcObject> arg = new ArrayList<>();
            for (Ast.Expression ar : ast.getArguments()) {
                arg.add(visit(ar));
            }
            return scope.lookupFunction(ast.getName(), arg.size()).invoke(arg);
        } finally {
            scope = scope.getParent();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}