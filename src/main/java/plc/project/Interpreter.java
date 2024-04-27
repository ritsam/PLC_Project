package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Objects;


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
        for (Ast.Global global : ast.getGlobals()) {
            visit(global);
        }
        for (Ast.Function function : ast.getFunctions()) {
            visit(function);
        }
        try {
            Environment.Function mainFunction = scope.lookupFunction("main", 0);
            return mainFunction.invoke(Collections.emptyList());
        } catch (RuntimeException e) {
            throw new RuntimeException("Main function not found in source.", e);
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
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope currentScope = scope;
            //new child scope
            scope = new Scope(currentScope);
            try {
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                }
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } catch (Return returnValue) {
                return returnValue.value;
            } finally {
                scope = currentScope;
            }
            return Environment.NIL;
        });
        return Environment.NIL;
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
        Ast.Expression receiverExpression = ast.getReceiver();
        if (!(receiverExpression instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Only variables or list elements can be assigned.");
        }
        Ast.Expression.Access access = (Ast.Expression.Access) receiverExpression;
        Environment.Variable variable = scope.lookupVariable(access.getName());
        if (!variable.getMutable()) {
            throw new RuntimeException("Immutable variable cannot be assigned.");
        }
        if (access.getOffset().isPresent()) {
            Environment.PlcObject listObject = variable.getValue();
            if (!(listObject.getValue() instanceof List)) {
                throw new RuntimeException("Variable is not a list - offset is present.");
            }
            List list = (List) listObject.getValue();
            int index = requireType(Number.class, visit(access.getOffset().get())).intValue();
            if (index < 0 || index >= list.size()) {
                throw new RuntimeException("List index out of bounds.");
            }
            Object newValue = visit(ast.getValue()).getValue();
            list.set(index, newValue);
        } else {
            variable.setValue(visit(ast.getValue()));
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        //TODO done
        Environment.PlcObject conditionObject = visit(ast.getCondition());
        Object rawValue = conditionObject.getValue();
        Boolean conditionResult;
        if (rawValue instanceof Boolean) {
            conditionResult = (Boolean) rawValue;
        } else {
            throw new RuntimeException("IF statement condition is not a Boolean. rawVAL = " + rawValue);
        }
        scope = new Scope(scope);
        try {
            if (conditionResult) {
                for (Ast.Statement stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            } else {
                for (Ast.Statement stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        Environment.PlcObject conditionValue = visit(ast.getCondition());
        Scope originalScope = scope;
        scope = new Scope(originalScope);
        try {
            boolean matchFound = false;

            for (Ast.Statement.Case caseStmt : ast.getCases()) {
                if (matchFound || !caseStmt.getValue().isPresent()) {
                    for (Ast.Statement statement : caseStmt.getStatements()) {
                        visit(statement);
                    }
                    if (!matchFound) break;
                } else {
                    Environment.PlcObject caseValue = visit(caseStmt.getValue().get());
                    if (conditionValue.equals(caseValue)) {
                        matchFound = true;
                        for (Ast.Statement statement : caseStmt.getStatements()) {
                            visit(statement);
                        }
                        break;
                    }
                }
            }
        } finally {
            scope = originalScope;
        }
        return Environment.NIL;
//        Environment.PlcObject conditionValue = visit(ast.getCondition());
//        Scope originalScope = scope;
//        scope = new Scope(originalScope);
//        try {
//            boolean matchFound = false;
//
//            for (Ast.Statement.Case caseStmt : ast.getCases()) {
//                if (matchFound || !caseStmt.getValue().isPresent()) {
//                    for (Ast.Statement statement : caseStmt.getStatements()) {
//                        visit(statement);
//                    }
//                    break;
//                } else {
//                    Environment.PlcObject caseValue = visit(caseStmt.getValue().get());
//                    if (conditionValue.equals(caseValue)) {
//                        matchFound = true;
//                        for (Ast.Statement statement : caseStmt.getStatements()) {
//                            visit(statement);
//                        }
//                        break;
//                    }
//                }
//            }
//        } finally {
//            scope = originalScope;
//        }
//        return Environment.NIL;
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
        //throw new UnsupportedOperationException(); //TODO
        switch (ast.getOperator()) {
            case "&&":
                return Environment.create(requireType(Boolean.class, visit(ast.getLeft())) && requireType(Boolean.class, visit(ast.getRight())));
            case "||":
                return Environment.create(requireType(Boolean.class, visit(ast.getLeft())) || requireType(Boolean.class, visit(ast.getRight())));
            case "<": ////
                if(visit(ast.getLeft()).getValue() instanceof Comparable) {
                    Environment.PlcObject right = visit(ast.getRight());
                    if(requireType(visit(ast.getLeft()).getValue().getClass(), right) != null) {
                        return Environment.create(((Comparable) visit(ast.getLeft()).getValue()).compareTo(right.getValue()) < 0);
                    }
                }
                break;
            case ">": ///
                if(visit(ast.getLeft()).getValue() instanceof Comparable) {
                    Environment.PlcObject right = visit(ast.getRight());
                    if(requireType(visit(ast.getLeft()).getValue().getClass(), right) != null) {
                        return Environment.create(((Comparable) visit(ast.getLeft()).getValue()).compareTo(right.getValue()) > 0);
                    }
                }
                break;
            case "==":
                return Environment.create(Objects.equals(visit(ast.getLeft()), visit(ast.getRight())));
            case "!=":
                return Environment.create(!Objects.equals(visit(ast.getLeft()), visit(ast.getRight())));
            case "+":
                Environment.PlcObject left = visit(ast.getLeft());
                Environment.PlcObject right = visit(ast.getRight());
                if (left.getValue() instanceof String || right.getValue() instanceof String) {
                    return Environment.create(left.getValue().toString() + right.getValue().toString());
                }
                else if (left.getValue() instanceof BigInteger && right.getValue() instanceof BigInteger) {
                    return Environment.create(((BigInteger) left.getValue()).add((BigInteger) right.getValue()));
                }
                else if (left.getValue() instanceof BigDecimal && right.getValue() instanceof BigDecimal) {
                    return Environment.create(((BigDecimal) left.getValue()).add((BigDecimal) right.getValue()));
                }
                else {
                    throw new RuntimeException("Invalid operands for + operator.");
                }
            case "-":
                return Environment.create(requireType(BigInteger.class, visit(ast.getLeft())).subtract(requireType(BigInteger.class, visit(ast.getRight()))));
            case "*":
                return Environment.create(requireType(BigInteger.class, visit(ast.getLeft())).multiply(requireType(BigInteger.class, visit(ast.getRight()))));
            case "/":
                BigInteger rightDiv = requireType(BigInteger.class, visit(ast.getRight()));
                if (rightDiv.equals(BigInteger.ZERO)) throw new RuntimeException("Division by zero.");
                return Environment.create(requireType(BigInteger.class, visit(ast.getLeft())).divide(rightDiv));
            case "^":
                return Environment.create(requireType(BigInteger.class, visit(ast.getLeft())).pow(requireType(BigInteger.class, visit(ast.getRight())).intValueExact()));
            default:
                throw new RuntimeException("Unsupported operator: " + ast.getOperator());
        }
        throw new RuntimeException("Unsupported operator: " + ast.getOperator());
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
        //throw new UnsupportedOperationException(); //TODO
        List<Object> evaluatedList = ast.getValues().stream()
                .map(expr -> visit(expr).getValue()) // Extracting the raw value
                .collect(Collectors.toList());
        return Environment.create(evaluatedList);
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