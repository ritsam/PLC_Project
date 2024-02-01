package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false),
                Arguments.of("Leading underscore", "_apple", false),
                Arguments.of("@ leading", "@gmail", true),
                Arguments.of("@ middle", "gm@il", false),
                Arguments.of("@ only", "@", true),
                Arguments.of("_ only", "___", false),
                Arguments.of("middle hyphen", "ritika-lara", true),
                Arguments.of("space", " ", false),
                Arguments.of("empty", "", false),
                Arguments.of("with underscore", "user_name", true),
                Arguments.of("single char", "a", true),
                Arguments.of("hyphenated", "a-b-c", true),
                Arguments.of("Leading spaces", "   words", false),
                Arguments.of("mixed symbols", ".&#$!*", false),
                Arguments.of("decimal", "32", false),
                Arguments.of("all caps", "ABC", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Neg Zero", "-0", false),
                Arguments.of("Leading Zeros", "007", false),
                Arguments.of("Neg Zero multiple", "-00", false),
                Arguments.of("Neg sign", "-", false),
                Arguments.of("Trailing Zeros", "35000", true),
                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Zero", "0", true),
                Arguments.of("Multiple Zeros", "0000", false),
                Arguments.of("Symbols", "10&%$", false),
                Arguments.of("Empty", " ", false),
                Arguments.of("Lead Space", " 435", false),
                Arguments.of("Trail space", "123 ", false),
                Arguments.of("Chars", "abc", false),
                Arguments.of("Neg Leading 0", "-01234", false),
                Arguments.of("Positive", "+10", false),
                Arguments.of("Neg and Space", "- 564", false),
                Arguments.of("Multiple signs", "--76", false),
                Arguments.of("Sign After", "54-", false),
                Arguments.of("Mixed", "10ab67c", false),
                Arguments.of("Plus", "+", false),
                Arguments.of("Comma", "1,234", false),
                Arguments.of("Middle Space", "78 342", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("char decimal", "5.toString()", false),
                Arguments.of("Integer Only", "1", false),
                Arguments.of("neg whole", "-1", false),
                Arguments.of("start 0", "0.51", true),
                Arguments.of("neg lead 0", "-0.51", true),
                Arguments.of("neg leading 0s", "-000.51", false),
                Arguments.of("chars", "test", false),
                Arguments.of("symbols", "10.&^%$#", false),
                Arguments.of("empty", "", false),
                Arguments.of("space", "0. 1", false),
                Arguments.of("zero", "0.0", true),
                Arguments.of("trailing 0s", "7.000", true),
                Arguments.of("leading 0s", "000.51", false),
                Arguments.of("neg trailing 0s", "-3.4000", true),
                Arguments.of("lead 0 pt 2", "030.51", false),
                Arguments.of("sign", "-", false),
                Arguments.of("just space", " ", false),
                Arguments.of("Multiple signs", "--6.8", false),
                Arguments.of("pos", "+", false),
                Arguments.of("pos decimal", "+1.1", false),
                Arguments.of("pipe sign", "|1", false),
                Arguments.of("invalid decimal", "1:2", false),
                Arguments.of("neg 0", "-0.0", false),
                Arguments.of("trailing digits", "123", false),
                Arguments.of("unicode", "႑", false),
                Arguments.of("double", "1..0", false)
                );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'", false),
                Arguments.of("unterminated", "\'", false),
                Arguments.of("Num", "\'7\'", true),
                Arguments.of("Space", "\' \'", false),
                Arguments.of("Slash", "\'\\\'", false),
                Arguments.of("Tab", "\'\\t\'", true),
                Arguments.of("Missing front", "a\'", false),
                Arguments.of("Missing end", "\'a", false),
                Arguments.of("Char newline", "\'a\\t\'", false),
                Arguments.of("Invalid Escape", "\'\\q\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Escape", "\"1\\t2\"", true),
                Arguments.of("escape quote", "\"1\\'2\"", true),
                Arguments.of("space", "\" \"", true),
                Arguments.of("escape char", "\"1\\q2\"", false),
                Arguments.of("empty", "", false),
                Arguments.of("missing quote 2", "unterminated\"", false),
                Arguments.of("extra slash", "\"1\\2\"", false),
                Arguments.of("slash only", "\"\\\"", false),
                Arguments.of("slash only", "\"\\\\\"", true),
                Arguments.of("double quotes", "\"\"Hello\"\"", false),
                Arguments.of("slash symbol", "\"\"\\*\"\"", false),
                Arguments.of("escapes", "\"\\bnrt\"", true),
                Arguments.of("lone char", "c", false),
                Arguments.of("lone char with quote", "\"c\"", true),
                Arguments.of("Symbols", "\"@#$%^&*!\"", true),
                Arguments.of("Multiple Quotes", "\"\"\"\"", true),

                //Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Another Invalid Escape", "\\x", false),
                Arguments.of("Backspace", "\\b", false),
                Arguments.of("Double slashes no Quotes", "\\\\", false),
                Arguments.of("Double slashes w Quotes", "\\\"\"\\", false),
                Arguments.of("Letters in/outside quotes", "c\"c\"\"\"c", false),
                Arguments.of("Char Reference no Quotes", "\\n\\r\\t", false),
                Arguments.of("Char Reference w Quotes", "\"\n\\r\\t\"", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("equals twice", "==", true),
                Arguments.of("equal once", "=", true),
                Arguments.of("& operator", "&&", true),
                Arguments.of("Or operator", "||", true),
                Arguments.of("Char", "a", false),
                Arguments.of("Char", "$", true),
                Arguments.of("Num", "5", false),
                Arguments.of("Add", "+", true),
                Arguments.of("Subtract", "-", true),
                Arguments.of("Semicolon", ";", true),
                Arguments.of("Closing", ")", true),
                Arguments.of("Space operator", "= =", false),
                Arguments.of("front space", " =", false),
                Arguments.of("Empty", "", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                ))
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
