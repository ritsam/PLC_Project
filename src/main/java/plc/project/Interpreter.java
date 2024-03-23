package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        return scope.lookupFunction("main", 0).invoke(Collections.emptyList());
        //throw new RuntimeException("Main function not found in source.");        
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
        //throw new UnsupportedOperationException(); //TODO 
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Scope currentScope = scope; 
            //new child scope
            scope = new Scope(currentScope); 
            try {
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    cope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                }
                for (Ast.Statement statement : ast.getStatements()) {
                    visit(statement);
                }
            } 
            catch (Return returnValue) {
                return returnValue.value; 
            }   
            finally {
                scope = currentScope; 
            }
            return Environment.NIL; 
        });
        return Environment.NIL; 
    }

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
        Ast.Expression acc = ast.getReceiver();
        if(acc instanceof Ast.Expression.Access) {
            if(((Ast.Expression.Access) acc).getReceiver().isPresent()) {
                visit(((Ast.Expression.Access) acc).getReceiver().get()).setGlobal(((Ast.Expression.Access) acc).getName(), visit(ast.getValue()));
            } 
            else {
                Environment.Variable var = scope.lookupVariable(((Ast.Expression.Access) acc).getName());
                var.setValue(visit(ast.getValue()));
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
         //TODO done
        while (requireType(Boolean.class, visit(ast.getCondition())) != null) {
            try {
                scope = new Scope(scope);
                //false-else
                if (!((Boolean) visit(ast.getCondition()).getValue())) { 
                    for (Ast.Statement e : ast.getElseStatements()) {
                        visit(e);
                    }
                } else {
                    //then
                    for (Ast.Statement t : ast.getThenStatements()) { 
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
        //throw new UnsupportedOperationException();//TODO
        Environment.PlcObject conditionValue = visit(ast.getCondition()); 
        Scope currentScope = scope; 
        scope = new Scope(currentScope); 
        try {
            boolean matchedCaseExecuted = false; 
            for (Ast.Statement.Case caseStmt : ast.getCases()) {
                // matched case
                if (caseStmt.getValue().isPresent() && Objects.equals(visit(caseStmt.getValue().get()), conditionValue)) {            
                    for (Ast.Statement statement : caseStmt.getStatements()) {
                        visit(statement);
                    }
                    matchedCaseExecuted = true;
                    break; 
                }
            }
            // no case matched + has default case then execte stmts
            if (!matchedCaseExecuted) {
                for (Ast.Statement.Case caseStmt : ast.getCases()) {
                    if (!caseStmt.getValue().isPresent()) { 
                        for (Ast.Statement statement : caseStmt.getStatements()) {
                            visit(statement);
                        }
                        break; 
                    }
                }
            }
        } finally {
            scope = currentScope; 
        }
        return Environment.NIL; 
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
            case "<":
            case ">":
                Comparable leftComparable = requireType(Comparable.class, visit(ast.getLeft()).getValue());
                Comparable rightComparable = requireType(Comparable.class, visit(ast.getRight()).getValue());
                int comparison = leftComparable.compareTo(rightComparable);
                if (ast.getOperator().equals("<")) {
                    return Environment.create(comparison < 0);
                } 
                else {
                    return Environment.create(comparison > 0);
                }
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
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        throw new UnsupportedOperationException(); //TODO done
        Ast.Expression acc = ast.getReceiver();
        if(acc.isPresent()) {
            return visit(acc.get()).getField(ast.getName()).getValue();
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        //throw new UnsupportedOperationException(); //TODO done
        try {
            scope = new Scope(scope);
            List<Environment.PlcObject> arg = new ArrayList<Environment.PlcObject>();
            for (Ast.Expression ar : ast.getArguments()) {
                arg.add(visit(ar));
            }
            if (ast.getReceiver().isPresent()) {
                return visit(ast.getReceiver().get()).callMethod(ast.getName(), arg);
            }
            else {
                return scope.lookupFunction(ast.getName(), arg.size()).invoke(arg);
            }
        } finally {
            scope = scope.getParent();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        //throw new UnsupportedOperationException(); //TODO
        List<Environment.PlcObject> evaluatedList = new ArrayList<>();        
        for (Ast.Expression expression : ast.getValues()) {
            evaluatedList.add(visit(expression));
        }
        //returns list wrapped in a PlcObject
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
