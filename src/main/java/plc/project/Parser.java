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
        //TODO
        try {
            List<Ast.Global> g = new ArrayList<>();
            List<Ast.Function> f = new ArrayList<>();
            while (tokens.has(0)) {
                if (peek("LIST") || peek("VAR") || peek("VAL")) {
                    g.add(parseGlobal());
                } else if (match("FUN")) {
                    f.add(parseFunction());
                }
            }
            return new Ast.Source(g,f);
        } catch (ParseException pe) {
            throw new ParseException(pe.getMessage(), pe.getIndex());
        }
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        //--Unexpected ParseException (Expected global declaration)

        if (peek("LIST")) {
            return parseList();
        } else if (peek("VAR")) {
            return parseMutable();
        } else if (peek("VAL")) {
            return parseImmutable();
        } else {
            throw new ParseException("Expected global declaration", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        // 'LIST' identifier '=' '[' expression (',' expression)* ']'
        //TODO
        match("LIST");
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after LIST", tokens.get(-1).getIndex());
        }
        String listName = tokens.get(-1).getLiteral(); //identifier
        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        List<Ast.Expression> exprs = new ArrayList<>();
        if (match("[")) {
            do {
                exprs.add(parseExpression());
            }
            while (match(","));
            if (!match("]")) {
                throw new ParseException("Expected ]", tokens.get(-1).getIndex());
            }
        } else {
            throw new ParseException("Expected [", tokens.get(-1).getIndex());
        }

        return new Ast.Global(listName, false, value);
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("VAR");
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after VAR", tokens.get(-1).getIndex());
        }
        String identifier = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> value = Optional.empty();
        // check for optional expression after =
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        match(";");
        return new Ast.Global(identifier, true, value);
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("VAL");
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after VAL", tokens.get(-1).getIndex());
        }
        String identifier = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> value = Optional.empty();
        // check for optional expression after =
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        else {
            throw new ParseException("Expected = after VAL identifier", tokens.get(-1).getIndex());
        }
        match(";");
        return new Ast.Global(identifier, false, value);
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("FUN");
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected identifier after FUN", tokens.get(-1).getIndex());
        }
        String functionName = tokens.get(-1).getLiteral();
        List<String> parameters = new ArrayList<>();
        //parameters
        if (match("(")) {
            // check for parameters
            if (!match(")")) {
                do {
                    if (!match(Token.Type.IDENTIFIER)) {
                        throw new ParseException("Expected identifier for function parameter", tokens.get(-1).getIndex());
                    }
                    parameters.add(tokens.get(-1).getLiteral());
                } while (match(","));
                match(")");
            }
        }
        match("DO");
        // parse body of function
        List<Ast.Statement> statements = parseBlock();
        match("END");
        return new Ast.Function(functionName, parameters, statements);
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        List<Ast.Statement> statements = new ArrayList<>();
        while (!match("END")) {
            int start = tokens.index;
            Ast.Statement statement = parseStatement();
            if (tokens.index == start) {
                throw new ParseException("Potential Error Encountered", tokens.get(start).getIndex());
            }
            statements.add(statement);
            if(!peek("END")) {
                throw new ParseException("Missing 'END'", -1);
            }
        }
        return statements;
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
        //TODO
        // 'LET' identifier ('=' expression)? ';'
        match("LET");
        if (!match(Token.Type.IDENTIFIER)){
            throw new ParseException("Expected Identifier", -1);
        }
        String name = tokens.get(-1).getLiteral(); //identifier
        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        if (!match(";")) {
            throw new ParseException("Expected ';'", tokens.get(-1).getIndex());
        }
        return new Ast.Statement.Declaration(name, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        //TODO 'IF' expression 'DO' block ('ELSE' block)? 'END'
        try {
            if (match("IF")) {
                Ast.Expression condition = parseExpression();
                if (!match("DO")) {
                    throw new ParseException("Expected 'DO' after IF condition", tokens.get(-1).getIndex());
                }
                List<Ast.Statement> then = new ArrayList<>(); // then block
                List<Ast.Statement> el = new ArrayList<>(); //ELSE
                //first match +parse all thens
                while (!peek("ELSE") && !peek("END")) {
                    then.add(parseStatement());
                }
                if (match("ELSE")) {
                    while (!peek("END")) {
                        el.add(parseStatement());
                    }
                }
                //match("END");
                if(match("END")){
                    return new Ast.Statement.If(condition, then, el);
                }
                else {
                    throw new ParseException("Missing END", tokens.get(-1).getIndex());
                }
            }
            throw new ParseException("Invalid statement", tokens.get(-1).getIndex());
        } catch (ParseException e) {
            throw new ParseException("Error if statement: " + e.getMessage(), tokens.get(-1).getIndex());
        }
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        //TODO
        //'SWITCH' expression ('CASE' expression ':' block)* 'DEFAULT' block 'END'
        try {
            match("SWITCH");
            Ast.Expression expr = parseExpression();
            List<Ast.Statement.Case> cases = new ArrayList<>();
            while (peek("CASE")) {
                cases.add(parseCaseStatement());
            }
            List<Ast.Statement> def = new ArrayList<>();
            if (match("DEFAULT")) {
                def = parseBlock();
            }
            match("END");
            return new Ast.Statement.Switch(expr, cases);
        }
        catch (ParseException e) {
            throw new ParseException("Error switch statement: " + e.getMessage(), tokens.get(-1).getIndex());
        }
    }


    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        //TODO
        //'CASE' expression ':' block
        try {
            match("CASE");
            Ast.Expression caseExpr = parseExpression();
            match(":");
            List<Ast.Statement> caseBlock = parseBlock();
            return new Ast.Statement.Case(Optional.of(caseExpr), caseBlock);
        }
        catch (ParseException pe) {
            throw new ParseException(pe.getMessage(), pe.getIndex());
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        //TODO  Missing END: Unexpected java.lang.IndexOutOfBoundsException
        try {
            match("WHILE");
            Ast.Expression condition = parseExpression();
            if(peek("DO")){
                match("DO");
            }
            else{
                throw new ParseException("Expected 'DO'", -1);
            }
            List<Ast.Statement> statements = parseBlock();
            if(peek("END")){
                match("END");
            }
            /*else {
                throw new ParseException("Missing 'END'", -1);
            }*/
            return new Ast.Statement.While(condition, statements);
        }
        catch (ParseException e) {
            throw new ParseException("Error in while: " + e.getMessage(), e.getIndex());
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException { //TODO
        match("RETURN");
        Ast.Expression expression = parseExpression();
        match(";");
        return new Ast.Statement.Return(expression);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException { //TODO 2a
        return parseLogicalExpression();
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
        Ast.Expression currentExpression = parseAdditiveExpression();
        while (match("!=") || match("==") || match(">=") || match(">") || match("<=") || match("<")) {
            String operation = tokens.get(-1).getLiteral();
            Ast.Expression rightExpr = parseAdditiveExpression();
            currentExpression = new Ast.Expression.Binary(operation, currentExpression, rightExpr);
        }
        return currentExpression;
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
            Ast.Expression right=parsePrimaryExpression();
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
            if (!match(")")) {
                throw new ParseException("Missing )", tokens.get(-1).getIndex());
            }
            return expression;
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
                    if (!match(")")) {
                        throw new ParseException("Missing closing parenthesis", tokens.get(-1).getIndex());
                    }
                }
                return new Ast.Expression.Function(id, arguments);
            }
            // check for access to array, uses Optional
            if (match("[")) {
                Ast.Expression index = parseExpression();
                if (!match("]")) {
                    throw new ParseException("Missing closing bracket", tokens.get(-1).getIndex());
                }
                return new Ast.Expression.Access(Optional.of(index), id);
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