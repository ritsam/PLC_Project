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
//        print(ast.getName(), "(");
//        for (int i = 0; i < ast.getParameters().size(); i++) {
//            if (i > 0) print(", ");
//            visit(ast.getParameters().get(i));
//        }
//        print(")");
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
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
        }
        else if (ast.getType() == Environment.Type.DECIMAL) { //precision
            BigDecimal dec = (BigDecimal) ast.getLiteral();
            print(dec.toString());
        }
        if (ast.getType() == Environment.Type.BOOLEAN) {
            Boolean bool = (Boolean) ast.getLiteral();
            print(bool);
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO
    }

}
