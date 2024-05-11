package plc.project;

import java.sql.Array;
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
    public List<Token> lex() {
        //throw new UnsupportedOperationException(); //TODO
        List<Token> tokens = new ArrayList<>();
        while(chars.has(0)){
            if(match(" ") || match("\b") || match("\n") || match("\r") || match("\t")){
                chars.skip();
            }
            else{
                tokens.add(lexToken());
            }
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
    public Token lexToken() {
        if (peek("[A-Za-z]")) {
            return lexIdentifier();
        }
        else if (peek("-") || peek("\\.") || peek("\\d")) {
            return lexNumber();
        }
        else if (peek("\'")) {
            return lexCharacter();
        }
        else if (peek("\"")) {
            return lexString();
        }
        else {
            return lexOperator();
        }
//        throw new UnsupportedOperationException();
        //TODO
    }

    public Token lexIdentifier() {
        if(peek("[A-Za-z]")){
            match("[A-Za-z]");
            while (peek("[A-Za-z0-9_-]")) {
                match("[A-Za-z0-9_-]");
            }
        }
        return chars.emit(Token.Type.IDENTIFIER);
        //TODO
    }

    public Token lexNumber() {

        // catches trailing decimal, since when the decimal is reached, LexNumber is called again
        // also stops leading decimal
        if(peek("\\.")){
            match("\\.");
            return chars.emit(Token.Type.OPERATOR);
        }
        if (peek("[-]")) {
            match("[-]");
        }
        if (peek("[0]")){ // handles leading zeroes
            match("[0]");
            if (peek("[0-9]"))
                return chars.emit(Token.Type.INTEGER);
        }
        while(peek("[0-9]")){
            match("[0-9]");
        }

        if (peek("\\.") && !chars.has(1)) { // trailing decimal
            return chars.emit(Token.Type.INTEGER);
        }

        if (peek("[\\.]","\\d")) {
            match("[\\.]");
            while (peek("\\d")) {
                match("\\d");
            }
//            if (peek("[\\.]")) {
//                match("[\\.]");
//                return chars.emit(Token.Type.OPERATOR);
//            }
            return chars.emit(Token.Type.DECIMAL);
        }

        return chars.emit(Token.Type.INTEGER);
        //throw new UnsupportedOperationException();
        //TODO
    }

    public Token lexCharacter() {
        if(peek("\'")){
            match("\'");
        }
        if (peek("[^\'\n\r\\\\]")) {
            match("[^\'\n\r\\\\]");
        }
        else if (peek("\\\\","[bnrt'\"\\\\]")) {
            lexEscape();
        } else {
            throw new ParseException("Invalid character literal", chars.index);
        }
        if(peek("\'")){
            match("\'");
        }
        else {
            throw new ParseException("Invalid character literal", chars.index);
        }
        return chars.emit(Token.Type.CHARACTER);
        //throw new UnsupportedOperationException();
        //TODO
    }

    public Token lexString() {
        if(peek("\"")){
            match("\"");
        }
//
        while (peek("[^\"\\n\\r\\\\]")) {
            match("[^\"\\n\\r\\\\]");
        }

        if (peek("\\\\")) {
            lexEscape();
            while (peek("[^\"\\n\\r\\\\]")) {
                match("[^\"\\n\\r\\\\]");
            }
        }
        if(peek("\"")){
            match("\"");
        }
        else{
            throw new ParseException("Missing Double Quotes", chars.index);
        }
        return chars.emit(Token.Type.STRING);
//        throw new UnsupportedOperationException();
        //TODO
    }

    public void lexEscape() {
        if (peek("\\\\","[bnrt'\"\\\\]")) {
            match("\\\\","[bnrt'\"\\\\]");
        }
        else {
            match("\\\\","[^bnrt\'\"\\\\]");
            throw new ParseException("Invalid Escape", chars.index);
        }
        //TODO
    }

    public Token lexOperator() {
        //[!=] '='? | '&&' | '||' | 'any character'
        if(peek("[<>!=]" ,"=")){
            match("[<>!=]","=");
        }
        else if(peek("\\&", "\\&")){
            match("\\&", "\\&");
        }
        else if(peek("\\|","\\|")){
            System.out.println("lol");
            match("\\|","\\|");
        }
        else if(peek("[!@#$%^&*()-=+;:\\[\\]]")){
            match("[!@#$%^&*()-=+;:\\[\\]]");
        }
        else{
            System.out.println(chars.get(0));
            throw new ParseException("unidentified operator",chars.index);
        }
        return chars.emit(Token.Type.OPERATOR);
        //throw new UnsupportedOperationException();
        //TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        //throw new UnsupportedOperationException(); //TODO (in Lecture)
        for(int i = 0; i < patterns.length; i++){
            if(!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true;
    }
    //peek('a')
    //peek('a','b')

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        //throw new UnsupportedOperationException(); //TODO (in Lecture)
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i<patterns.length;i++){
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
