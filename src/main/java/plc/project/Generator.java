package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);

        for (Ast.Global global : ast.getGlobals()) {
            newline(indent + 1);
            visit(global);
        }

        if (!ast.getGlobals().isEmpty()) {
            newline(0);
        }

        newline(indent + 1);
        print("public static void main(String[] args) {");
        newline(indent + 2);
        print("System.exit(new Main().main());");
        newline(indent + 1);
        print("}");
        newline(0);

        for (Ast.Function function : ast.getFunctions()) {
            newline(indent + 1);
            printFunction(function);
            newline(0);
        }

        newline(indent);
        print("}");
        return null;
    }

    private void printFunction(Ast.Function function) {
        String returnType = "void";
        if (function.getReturnTypeName().isPresent()) {
            returnType = function.getReturnTypeName().get().equals("Integer") ? "int" : function.getReturnTypeName().get();
        }
        print(returnType, " ", function.getName(), "(");

        List<String> parameters = function.getParameters();
        List<String> parameterTypeNames = function.getParameterTypeNames();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                print(", ");
            }
            String paramType = parameterTypeNames.get(i).equals("Integer") ? "int" : parameterTypeNames.get(i);
            print(paramType, " ", parameters.get(i));
        }

        print(") {");
        newline(indent + 2);

        if (!function.getStatements().isEmpty()) {
            for (Ast.Statement statement : function.getStatements()) {
                visit(statement);
                if (statement != function.getStatements().get(function.getStatements().size() - 1)) {
                    newline(indent + 2);
                }
            }
        } else {
            print(" ");
        }

        newline(indent + 1);
        print("}");
    }

    @Override
    public Void visit(Ast.Global ast) {
        String type = ast.getTypeName();
        String javaType = type.equals("Decimal") ? "double[]" : type.equals("Integer") ? "int" : type;

        //if the variable is immutable
        String declarationPrefix = ast.getMutable() ? "" : "final ";
        print(declarationPrefix + javaType + " " + ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            Ast.Expression value = ast.getValue().get();
            if (value instanceof Ast.Expression.PlcList) {
                print("{");
                List<Ast.Expression> elements = ((Ast.Expression.PlcList)value).getValues();
                for (int i = 0; i < elements.size(); i++) {
                    if (i > 0) {
                        print(", ");
                    }
                    Ast.Expression element = elements.get(i);
                    if (element instanceof Ast.Expression.Literal) {
                        Object literal = ((Ast.Expression.Literal)element).getLiteral();
                        if (literal instanceof BigDecimal) {
                            print(((BigDecimal)literal).toPlainString());
                        } else if (literal instanceof BigInteger) {
                            print(literal.toString());
                        } else if (literal instanceof String) {
                            print('"' + literal.toString() + '"');
                        } else if (literal instanceof Boolean) {
                            print(literal.toString());
                        } else {
                            throw new RuntimeException("Unsupported literal type: " + literal.getClass().getSimpleName());
                        }
                    } else {
                        throw new RuntimeException("Non-literal expression cannot be used in array initialization.");
                    }
                }
                print("}");
            } else {
                if (value instanceof Ast.Expression.Literal) {
                    Object literal = ((Ast.Expression.Literal)value).getLiteral();
                    if (literal instanceof BigDecimal) {
                        print(((BigDecimal)literal).toPlainString());
                    } else if (literal instanceof BigInteger) {
                        print(literal.toString());
                    } else if (literal instanceof String) {
                        print('"' + literal.toString() + '"');
                    } else if (literal instanceof Boolean) {
                        print(literal.toString());
                    } else {
                        throw new RuntimeException("Unsupported literal type.");
                    }
                } else {
                    throw new RuntimeException("Unsupported expression type for global initialization.");
                }
            }
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        String returnType = ast.getReturnTypeName().orElse("void");
        print(returnType, " ", ast.getName(), "(");

        List<String> parameters = ast.getParameters();
        List<String> parameterTypeNames = ast.getParameterTypeNames();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                print(", ");
            }
            print(parameterTypeNames.get(i), " ", parameters.get(i));
        }

        print(") {");
        newline(indent + 1);

        if (!ast.getStatements().isEmpty()) {
            for (Ast.Statement statement : ast.getStatements()) {
                visit(statement);
                newline(indent + 1);
            }
        } else {
            print(" ");
        }

        newline(indent);
        print("}");
        newline(indent);
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        visit(ast.getExpression());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        //throw new UnsupportedOperationException();
        print(ast.getVariable().getType().getJvmName()," ", ast.getVariable().getJvmName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            print(ast.getValue().get());
        }
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        boolean hasStatements = false;

        for (Ast.Statement statement : ast.getThenStatements()) {
            newline(indent);
            visit(statement);
            hasStatements = true;
        }

        indent--;
        if (hasStatements) {
            newline(indent);
        } else {
            print(" ");
        }
        print("}");

        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            indent++;
            boolean hasElseStatements = false;

            for (Ast.Statement statement : ast.getElseStatements()) {
                newline(indent);
                visit(statement);
                hasElseStatements = true;
            }

            indent--;
            if (hasElseStatements) {
                newline(indent);
            } else {
                print(" ");
            }
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (");
        visit(ast.getCondition());
        print(") {");
        newline(indent + 1);

        indent++;
        boolean firstCase = true;
        for (Ast.Statement.Case caseStmt : ast.getCases()) {
            if (!firstCase) {
                newline(indent);
            }
            visit(caseStmt);
            firstCase = false;
        }

        indent--;
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) { //TODO //in progress
        if (ast.getValue().isPresent()) {
            print("case ");
            visit(ast.getValue().get());
        } else {
            print("default");
        }
        print(":");
        newline(indent + 1);

        indent++;
        boolean firstStatement = true;
        for (Ast.Statement statement : ast.getStatements()) {
            if (!firstStatement) {
                newline(indent);
            }
            visit(statement);
            firstStatement = false;
        }

        if (ast.getValue().isPresent()) {
            newline(indent);
            print("break;");
        }

        indent--;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");

        //stmts
        if(!ast.getStatements().isEmpty()) {
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent+1);
                print(ast.getStatements().get(i));
            }
            indent--;
            newline(indent);
            print("}");
        }
        else{
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
       // throw new UnsupportedOperationException();
        print("return ");
        print(ast.getValue(),";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getType() == Environment.Type.CHARACTER) {
            print("'", ast.getLiteral(), "'");
        }
        else if (ast.getType() == Environment.Type.STRING) {
            print("\"", ast.getLiteral(), "\"");
        }
        else if (ast.getType() == Environment.Type.INTEGER) {
            BigInteger inte = (BigInteger) ast.getLiteral();
            print(inte.toString());
        }
        else if (ast.getType() == Environment.Type.DECIMAL) {
            BigDecimal dec = (BigDecimal) ast.getLiteral();
            print(dec.toString());
        }
        else if (ast.getType() == Environment.Type.BOOLEAN) {
            Boolean bool = (Boolean) ast.getLiteral();
            print(bool.toString());
        }
        else {
            throw new RuntimeException("Unsupported literal type: " + ast.getType());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        print(" ", ast.getOperator(), " ");
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent()) {
            print(ast.getName());
            print("[");
            print(ast.getOffset().get());
            print("]");
        } else {
            print(ast.getName());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        print(ast.getFunction().getJvmName());
        print("(");
        if (!ast.getArguments().isEmpty()) {
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if (i != ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        print("{");
        List<Ast.Expression> elements = ast.getValues();

        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                print(", ");
            }
            visit(elements.get(i));
        }

        print("}");
        return null;
    }

}
