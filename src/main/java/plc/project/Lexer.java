package plc.project;

import java.util.List;
import java.util.ArrayList;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {//TODO- not sure if right
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {
            if (!(peek("[ \b\n\r\t]"))){ //not empty space
                tokens.add(lexToken());
            }
            chars.advance();
            chars.skip();
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() { //done
        if (peek("[A-Za-z@]")) {
            return lexIdentifier();
        }
        else if (peek("[-]", "[1-9]") || peek("[0-9]")){ //integer or decimal
            return lexNumber();
        }
        else if(peek("\'")){
            return lexCharacter();
        }
        else if(peek("\"")){
            return lexString();
        }
        else {
            return lexOperator();
        }

    }
    public Token lexIdentifier() {// done
        if (peek("@")) { //@ can only start
            chars.advance();
        }
        while (peek("[A-Za-z0-9_-]")) {
            chars.advance();
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {//TODO --almost done
        if (match("-")) {
            // If negative sign is present, check for leading zero or digit
            if (peek("0", "[1-9]")) {
                throw new ParseException("Leading 0", chars.index);
            }
        } else {
            // If no negative sign, check for leading zero
            if (match("0")) {
                // Check for decimal and digits
                if (match("[.]", "[0-9]")) {
                    match("[.]");
                    while (peek("[0-9]")) {
                        match("[0-9]");
                    }
                    return chars.emit(Token.Type.DECIMAL);
                } else {
                    // Leading zero without decimal is an error
                    return chars.emit(Token.Type.INTEGER);
                }
            }
        }

        // Handle digits before decimal point
        while (peek("[0-9]")) {
            match("[0-9]");
        }

        // Check for decimal point and digits after it
        if (peek("[.]", "[0-9]")) {
            match("[.]", "[0-9]");
            while (peek("[0-9]")) {
                match("[0-9]");
            }
            return chars.emit(Token.Type.DECIMAL);
        }
        // If no decimal point, it's an integer
        return chars.emit(Token.Type.INTEGER);
    }
    public Token lexCharacter() { //done
        // open
        if (peek("\'")) {
            match("\'");
        } else {
            throw new ParseException("Invalid character", chars.index);
        }

        if (peek("^'\\n\\r\\\\") || peek("\\\\")) {
            lexEscape();
        } else if (peek("[^'\n\r\\\\ ]")) {
            match("[^'\n\r\\\\ ]");
        } else {
            throw new ParseException("Invalid character", chars.index);
        }

        // close
        if (peek("\'")) {
            match("\'");
            return chars.emit(Token.Type.CHARACTER);
        } else {
            throw new ParseException("Invalid character", chars.index);
        }
    }


    public Token lexString() { //TODO
        //^"\n\r\\] | escape
        //open string
        if (peek("\"")) {
            match("\"");
        }
        else {
            throw new ParseException("Invalid string", chars.index); //no open
        }
        while (!peek("\"")) {  //middle
            if(peek("[^\\\\\"\\n\\r]")) { // other chars
                match("[^\\\\\"\\n\\r]");
            }
            else if (peek("\\\\")) { // escape seq
                lexEscape();
            }
            else {
            throw new ParseException("Invalid string", chars.index);
        }
        }
        //close string
        if (peek("\"")) {
            match("\"");
        }
        else {
            throw new ParseException("Invalid string", chars.index); //no close
        }
        return chars.emit(Token.Type.STRING);
    }

    public void lexEscape() { //done
        // escape ::= '\' [bnrt'"\\]
        if (peek("\\\\", "[bnrt\'\"\\\\]")) {
            match("\\\\", "[bnrt\'\"\\\\]");
        }
        else{
            throw new ParseException("Invalid Escape", chars.index);
        }
    }
    public Token lexOperator() {
        // operator ::= [!=] '='? | '&&' | '||' | 'any character'
        if (match("!", "=") || match("[|]", "[|]") || match("&", "&") || match("=", "=")) {
            return chars.emit(Token.Type.OPERATOR);
        }
        else {
            // If none of the specific operators are matched, treat as a single character operator
            match(".");
            return chars.emit(Token.Type.OPERATOR);
        }
        //throw new ParseException("Invalid char", chars.index);
    }
    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for(int i=0;i<patterns.length;i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek=peek(patterns);
        if(peek){
            for(int i=0;i<patterns.length;i++){
                chars.advance();
            }
        }
        return peek;
    }


    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
