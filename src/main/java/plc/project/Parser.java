package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList; //new
import java.util.Optional; //new
/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        //TODO  2b
        try {
            if (match("LET")) {
                return parseDeclarationStatement();
            } else if (match("SWITCH")) {
                return parseSwitchStatement();
            } else if (match("IF")) {
                return parseIfStatement();
            } else if (match("WHILE")) {
                return parseWhileStatement();
            } else if (match("RETURN")) {
                return parseReturnStatement();
            } else {
                Ast.Expression expression = parseExpression();
                if (match("=")) {
                    Ast.Expression right = parseExpression();
                    match(";"); //both = and ;
                    return new Ast.Statement.Assignment(expression, right);
                } else if (match(";")) {
                    return new Ast.Statement.Expression(expression);
                } else {
                    throw new ParseException("Expected '=' or ';'", tokens.get(-1).getIndex());
                }
            }
        } catch (ParseException pe) {
            throw new ParseException(pe.getMessage(), pe.getIndex());
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO 2b
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO 2b
        match("RETURN");
        Ast.Expression expression = parseExpression();
        if(match(";")) {
            return new Ast.Statement.Return(expression);
        }
        else{
            throw new ParseException("no ;" + " INDEX:" + tokens.get(-1).getIndex(), tokens.get(-1).getIndex());
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        //TODO 2a
        try {
            return parseLogicalExpression();
        } catch (ParseException pe) {
            throw new ParseException(pe.getMessage(), pe.getIndex());
        }
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO 2a
        Ast.Expression currentExpression = parseComparisonExpression();
        while (match("&&") || match("||")) {
            String operation = tokens.get(-1).getLiteral();
            Ast.Expression rightExpression = parseComparisonExpression();
            currentExpression = new Ast.Expression.Binary(operation, currentExpression, rightExpression);
        }
        return currentExpression;
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO 2a
        try {
            Ast.Expression output = parseAdditiveExpression();
            while (match("!=") || match("==") || match(">=") || match(">") || match("<=") || match("<")) {
                String operation = tokens.get(-1).getLiteral();
                Ast.Expression rightExpr = parseComparisonExpression();
                output = new Ast.Expression.Binary(operation, output, rightExpr);
            }
            return output;
        } catch(ParseException p) {
            throw new ParseException(p.getMessage(), p.getIndex());
        }
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression expression=parseMultiplicativeExpression(); //TODO 2a
        while(match("+")||match("-")){
            String add= tokens.get(-1).getLiteral(); //store operator
            Ast.Expression right=parseMultiplicativeExpression();
            expression=new Ast.Expression.Binary(add,expression,right);
        }
        return expression;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression expression=parsePrimaryExpression(); //TODO 2a
        while(match("/")||match("*")){
            String multi= tokens.get(-1).getLiteral();
            Ast.Expression right=parseAdditiveExpression();
            expression=new Ast.Expression.Binary(multi,expression,right);
        }
        return expression;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException { //TODO 2a
        if (match("NIL")) {
            return new Ast.Expression.Literal(null);
        } else if (match("TRUE")) {
            return new Ast.Expression.Literal(true);
        } else if (match("FALSE")) {
            return new Ast.Expression.Literal(false);
        } else if (match(Token.Type.INTEGER)) {
            BigInteger in = new BigInteger(tokens.get(-1).getLiteral());
            return new Ast.Expression.Literal(in);
        } else if (match(Token.Type.DECIMAL)) {
            BigDecimal dec = new BigDecimal(tokens.get(-1).getLiteral());
            return new Ast.Expression.Literal(dec);
        } else if (match(Token.Type.CHARACTER)) {
            String ch = tokens.get(-1).getLiteral();
            return new Ast.Expression.Literal(ch.charAt(1));
        } else if (match(Token.Type.STRING)) {
            String str = tokens.get(-1).getLiteral();
            str = str.replace("\\n", "\n");
            str = str.replace("\\t", "\t");
            str = str.replace("\\b", "\b");
            str = str.replace("\\r", "\r");
            str = str.replace("\\'", "'");
            str = str.replace("\\\\", "\\");
            str = str.replace("\\\"", "\"");
            str = str.substring(1, str.length() - 1);
            return new Ast.Expression.Literal(str);
        } else if (match("(")) {
            Ast.Expression.Group expression = new Ast.Expression.Group(parseExpression());
            if (match(")")) {
                return expression;
            }
            throw new ParseException("Need close parenthesis", tokens.get(-1).getIndex());
        }
        else if (peek(Token.Type.IDENTIFIER)) {
            String id = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (match("(")) {
                List<Ast.Expression> arguments = new ArrayList<>();
                if (!match(")")) {
                    do {
                        arguments.add(parseExpression());
                    } while (match(","));
                    match(")");
                }
                return new Ast.Expression.Function(id, arguments);
            }
            // check for access to array, uses Optional
            if (match("[")) {
                Ast.Expression index = parseExpression();
                if (match("]")) {
                    return new Ast.Expression.Access(Optional.of(index), id); //error?
                } else {
                    throw new ParseException("Expected closing bracket for array access", tokens.get(-1).getIndex());
                }
            }
            return new Ast.Expression.Access(Optional.empty(), id);
        }
        throw new ParseException("Unexpected token: " + tokens.get(0).getType(), tokens.get(0).getIndex());
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++)
                tokens.advance();
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}