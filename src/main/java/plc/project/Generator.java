package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

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
            visit(function);
            newline(0);
        }
//close class
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if (ast.getMutable()) {
            print(ast.getTypeName(), " ", ast.getName(), ";");
        } else {
            print("final ", ast.getTypeName(), " ", ast.getName(), ";");
        }
        newline(indent);
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {

        throw new UnsupportedOperationException(); //TODO
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

        for (Ast.Statement.Case caseStmt : ast.getCases()) {
            visit(caseStmt);
        }
        indent--;
        newline(indent);
        print("}");
        newline(indent);

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) { //TODO
        print("case ", ast.getValue(), ":");
        indent++;

        for (Ast.Statement stmt : ast.getStatements()) {
            newline(indent);
            visit(stmt);
        }
        indent--;
        newline(indent);
        print("break;");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (",ast.getCondition());
        print(") {");
        indent++;

        //stmts
        if(!ast.getStatements().isEmpty()) {
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                print(ast.getStatements().get(i));
            }
        }
        indent--;
        newline(indent);
        print("}");
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
            print("'",ast.getLiteral(),"'");
        }
        else if (ast.getType() == Environment.Type.STRING) {
            print("\"",ast.getLiteral(),"\"");
        }
        else if (ast.getType() == Environment.Type.INTEGER) {
            BigInteger inte = (BigInteger) ast.getLiteral();
            print(inte.intValue());
            //print(((BigInteger) ast.getLiteral()).intValue());
        }
        else if (ast.getType() == Environment.Type.DECIMAL) { //precision
            BigDecimal dec = (BigDecimal) ast.getLiteral();
            print(dec.toString());
            //print(ast.getLiteral().toString());
        }
        if (ast.getType() == Environment.Type.BOOLEAN) {
            Boolean bool = (Boolean) ast.getLiteral();
            print(bool);
            //print(ast.getLiteral());
        }
        else {
            throw new RuntimeException("Literal Error");
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
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if (i != ast.getArguments().size() - 1) {
                print(", ");
            }
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {

        throw new UnsupportedOperationException(); //TODO
    }

}
