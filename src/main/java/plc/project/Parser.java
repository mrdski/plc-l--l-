package plc.project;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
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
//    private final TokenStream tokens;
    private final TokenStream tokens;
    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }
    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        List<Ast.Global> glob = new ArrayList<>();
        List<Ast.Function> funct = new ArrayList<>();
        while(tokens.has(0)){
            if(peek("LIST") || peek("VAL") || peek("VAR")){
                glob.add(parseGlobal());
            } else if(peek("FUN")){
                funct.add(parseFunction());
            } else{
//                System.out.println(tokens.get(-1).getLiteral());
                if (tokens.index == 0){
                    //System.out.println("Expected LIST, VAL, VAR, or FUN: 0");
                    throw new ParseException("Expected LIST, VAL, VAR, or FUN", 0);
                }
                else {
                    //System.out.println("Expected LIST, VAL, VAR, or FUN" + (tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1));
                    throw new ParseException("Expected LIST, VAL, VAR, or FUN", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
                }
            }
        }
        return new Ast.Source(glob, funct);
    }
    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if(peek("LIST")){
            match("LIST");
            return parseList();
        }
        if(peek("VAR")){
            match("VAR");
            return parseMutable();
        }
        if(peek("VAL")){
            match("VAL");
            return parseImmutable();
        }
        else{
            throw new ParseException("Expected LIST, VAR, or VAL",
                    tokens.get(0).getIndex()); // not reachable i think
        }
    }
    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if (peek(Token.Type.IDENTIFIER)){
            match(Token.Type.IDENTIFIER);
        } else {
            throw new ParseException("No identifier in list", tokens.get(-
                    1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        }
        String name = tokens.get(-1).getLiteral();

        if (peek(":")){
            match(":");
        } else {
            throw new ParseException("No : in list", tokens.get(-
                    1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        }

        if (peek(Token.Type.IDENTIFIER)){
            match(Token.Type.IDENTIFIER);
        } else {
            throw new ParseException("No type in list", tokens.get(-
                    1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        }
        String type = tokens.get(-1).getLiteral();

        if(peek("=")){
            match("=");
            if (peek("[")){ match("[");}
            else {
                throw new ParseException("No [", tokens.get(-1).getIndex()+
                        tokens.get(-1).getLiteral().length() + 1);
            }
            List<Ast.Expression> expr = new ArrayList<>();
            while(!peek("]")){
                expr.add(parseExpression());
                if(peek(",")){
                    match(",");
                }
            }
            if (peek("]")){ match("]");}
            else {
                throw new ParseException("No ]", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            if (peek(";")){ match(";");}
            else {
                throw new ParseException("No ;", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            Ast.Expression.PlcList plcList = new Ast.Expression.PlcList(expr);
            plcList.setType(Environment.getType(type));
            return new Ast.Global(name, true, Optional.of(plcList));
        } else {
            throw new ParseException("No equals sign in list", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        }
    }
    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("VAR");
        if (peek(Token.Type.IDENTIFIER)){
            match(Token.Type.IDENTIFIER);
        } else {
            throw new ParseException("No identifier in mutable", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        }
        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> value = Optional.empty();
        if(peek(":")){
            match(":");
        }
        if(peek("Integer")){
            match("Integer");
        }
        if (peek("=")) {
            match("=");
            value = Optional.of(parseExpression());
        }
        if (peek(";")){
            match(";");
        }
//        else {
//            throw new ParseException("No equals sign in list", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
//        }
//        if (peek(";")){ match(";");}
//        else {
//            throw new ParseException("No ;", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
//        }
        return new Ast.Global(name, true, value);
    }
    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
//        match("VAL");
        if (peek(Token.Type.IDENTIFIER)){
            match(Token.Type.IDENTIFIER);
        } else {
            throw new ParseException("No identifier in immutable", tokens.get(-
                    1).getIndex() + tokens.get(-1).getLiteral().length() + 1);
        }
        String name = tokens.get(-1).getLiteral();
        Optional<Ast.Expression> valuet = Optional.empty();
        if(peek(":")){
            match(":");
        }
        if(peek("Integer")){
            match("Integer");
        }
        if (peek("=")){
            match("=");
        }
        Ast.Expression value = parseExpression();
        if (peek(";")){
            match(";");
        }
//        else {
//            throw new ParseException("No = or ;", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
//        }
//        if (peek(";")){ match(";");
//            System.out.println(tokens.get(-1).getLiteral());}
//        else {
//            System.out.println(tokens.get(-1).getLiteral());
//            throw new ParseException("No ;", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
//        }
        return new Ast.Global(name, false, Optional.of(value));
    }
    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("FUN");
        if (peek(Token.Type.IDENTIFIER)){
            match(Token.Type.IDENTIFIER);
        }
        else{
            throw new ParseException("No Indentifier for FUN", tokens.get(-
                    1).getIndex()+ tokens.get(-1).getLiteral().length());
        }
        String name = tokens.get(-1).getLiteral();
//        System.out.println(tokens.get(-1).getLiteral());
        if(peek(Token.Type.IDENTIFIER)){
            match(Token.Type.IDENTIFIER);
        }
        List<String> para = new ArrayList<>();
        if(peek("(")){
            match("(");
            while (!peek(")")) {
                if(peek(Token.Type.IDENTIFIER)){
                    String param = tokens.get(0).getLiteral();
                    para.add(param);
                    match(Token.Type.IDENTIFIER);
                }
                if(peek(":")){match(":");}
                if(peek("Integer")){
                    match("Integer");
                }
                if (peek(",")) {
                    match(",");
                }
            }
        }
        if (peek(")")){ match(")");}
//        else {
//            throw new ParseException("No )", tokens.get(-1).getIndex()+
//                    tokens.get(-1).getLiteral().length());
//        }
        if(peek(":")){match(":");}
        if(peek("Integer")){
            match("Integer");
        }
        if (peek("DO")){ match("DO");}
        else {
            throw new ParseException("No DO", tokens.get(-1).getIndex()+
                    tokens.get(-1).getLiteral().length());
        }
        List<Ast.Statement> stmts = parseBlock();
        if (peek("END")){ match("END");}
        else {
            throw new ParseException("No END", tokens.get(-1).getIndex()+
                    tokens.get(-1).getLiteral().length());
        }
//        System.out.println(tokens.get(-1).getLiteral());
        return new Ast.Function(name, para, stmts);
    }
    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();
        while(!peek("END") && !peek("ELSE") && !peek("CASE") && !peek("DEFAULT")){
            if (!tokens.has(0)){
                throw new ParseException("No ending statement", tokens.get(-
                        1).getIndex()+ tokens.get(-1).getLiteral().length());
            }
            statements.add(parseStatement());
            match(";");
        }
        return statements;
        //throw new UnsupportedOperationException(); //TODO
    }
    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if (peek(Token.Type.IDENTIFIER)){
            if (peek("LET")){
                return parseDeclarationStatement();
            }else if (peek("RETURN")){
                return parseReturnStatement();
            }else if (peek("SWITCH")){
                return parseSwitchStatement();
            }else if (peek("WHILE")){
                return parseWhileStatement();
            }else if (peek("IF")) {
                return parseIfStatement();
            }else{ // expression and assignment
                Ast.Expression receiver = parseExpression();
                if (peek(";")){
                    match(";");
                    return new Ast.Statement.Expression(receiver);
                }
                if (peek("=")){
                    match("=");
                } else {
//                    System.out.println("Missing Semicolon Index: " + (tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length()));
                    throw new ParseException("Missing semicolon", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
                }
                if (peek(";")){
                    throw new ParseException("Missing value",
                            tokens.get(0).getIndex());
                }
                Ast.Expression value = parseExpression();
                if (peek(";")){
                    return new Ast.Statement.Assignment(receiver,value);
                }
                else{
                    throw new ParseException("Missing semicolon", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
                }
            }
        }
        return new Ast.Statement.Expression(parseExpression());
    }
    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws
            ParseException {
// throw new UnsupportedOperationException(); //TODO
        match("LET");
        String name;
        Ast.Expression value;
        if (peek(Token.Type.IDENTIFIER)){
            match(Token.Type.IDENTIFIER);
            name = tokens.get(-1).getLiteral();
        }
        else {
            throw new ParseException("No identifier in declaration", tokens.get(-
                    1).getIndex()+ tokens.get(-1).getLiteral().length());
        }
        if (peek(";")){
            return new Ast.Statement.Declaration(name, Optional.empty());
        }
        if (!peek("=")){
            throw new ParseException("No = in declaration", tokens.get(-
                    1).getIndex()+ tokens.get(-1).getLiteral().length() + 1);
        } else{
            match("=");
            value = parseExpression();
        }
        if (peek(";")){
            return new Ast.Statement.Declaration(name, Optional.of(value));
        } else {
            throw new ParseException("Missing semicolon", tokens.get(-1).getIndex()
                    + tokens.get(-1).getLiteral().length());
        }
    }
    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
// throw new UnsupportedOperationException(); //TODO
        match("IF");
        Ast.Expression condition = parseExpression();
        if (peek("DO")) {
            match("DO");
        }
        else {
            if (tokens.has(0)){
//                System.out.println(tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length() + 1);
                throw new ParseException("No DO in if statement", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length() + 1);
            } else {
                //System.out.println(tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
                throw new ParseException("No DO in if statement", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
            }
        }
        List<Ast.Statement> thenStatements = parseBlock();
        List<Ast.Statement> elseStatements = Collections.emptyList();
        if (peek("ELSE")) {
            match("ELSE");
            elseStatements = parseBlock();
        }
        if (peek("END")){ match("END");}
        else {
            throw new ParseException("No END in if statement", tokens.get(-
                    1).getIndex()+ tokens.get(-1).getLiteral().length());
        }
        return new Ast.Statement.If(condition, thenStatements, elseStatements);
// match("IF");
// Ast.Expression condition = parseExpression();
// if (peek("DO")){ match("DO");}
// else {
// throw new ParseException("No DO in if statement", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
// }
// List<Ast.Statement> thenStatements = parseBlock();
// List<Ast.Statement> elseStatements = Collections.emptyList();
// if (peek("END")){
// return new Ast.Statement.If(condition, thenStatements,elseStatements);
// }
// if (peek("ELSE")){
// match("ELSE");
// elseStatements = parseBlock();
// return new Ast.Statement.If(condition, thenStatements,elseStatements);
// } else {
// throw new ParseException("weird if statement", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
// }
    }
    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        match("SWITCH");
        Ast.Expression condition = parseExpression();
        List<Ast.Statement.Case> cases = new ArrayList<>();
        while (!peek("DEFAULT")){
            if (peek("CASE")){
                cases.add(parseCaseStatement());
            }
            else if(peek("END")){
                if (tokens.has(0)){ // incorrect token
                    throw new ParseException("No DEFAULT in switch statement",
                            tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length() + 1);
                } else { // missing token
                    throw new ParseException("No DEFAULT in switch statement",
                            tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
                }
            }
        }
        match("DEFAULT");
        if (peek("END")){
            throw new ParseException("No default block in switch statement",
                    tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length() + 1);
        }
        List<Ast.Statement> default_ = parseBlock();
        cases.add(new Ast.Statement.Case(Optional.empty(), default_));
        if (peek("END")){
            return new Ast.Statement.Switch(condition, cases);
        } else if (tokens.has(0)){ // incorrect token
            throw new ParseException("No END in switch statement", tokens.get(-
                    1).getIndex()+ tokens.get(-1).getLiteral().length() + 1);
        } else { // missing token
            throw new ParseException("No END in switch statement", tokens.get(-
                    1).getIndex()+ tokens.get(-1).getLiteral().length());
        }
    }
    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        match("CASE");
        Ast.Expression value = parseExpression();
        if (!peek(":")){
            throw new ParseException("Expected semicolon in CASE", tokens.get(-
                    1).getIndex()+ tokens.get(-1).getLiteral().length() + 1);
        }
        match(":");
        if (peek("CASE") || peek("DEFAULT")){
            throw new ParseException("Expected block in CASE", tokens.get(-
                    1).getIndex()+ tokens.get(-1).getLiteral().length() + 1);
        }
        List<Ast.Statement> statements = parseBlock();
        return new Ast.Statement.Case(Optional.of(value), statements);
    }
    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
// throw new UnsupportedOperationException(); //TODO
// match("WHILE");
// Ast.Expression condition = parseExpression();
// match("DO");
// List<Ast.Statement> statements = parseBlock();
// match("END");
// return new Ast.Statement.While(condition, statements);
        match("WHILE"); // matches WHILE
        Ast.Expression condition = parseExpression();
        if (peek("DO")){ match("DO");}
        else {
            throw new ParseException("No DO in while loop", tokens.get(-
                    1).getIndex()+ tokens.get(-1).getLiteral().length());
        }
        List<Ast.Statement> statements = parseBlock();
        if (peek("END")){ match("END");}
        else {
            throw new ParseException("No END in while loop", tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
        }
        return new Ast.Statement.While(condition, statements);
    }
    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
// throw new UnsupportedOperationException(); //TODO
        match("RETURN");
        Ast.Expression value = parseExpression();
        if (peek(";")){ match(";");}
        else {
            throw new ParseException("No ;", tokens.get(-1).getIndex()+
                    tokens.get(-1).getLiteral().length());
        }
        return new Ast.Statement.Return(value);
    }
    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        return parseLogicalExpression();
    }
    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expression left = parseComparisonExpression();
        while(peek("&&") || peek("||")){
            match(Token.Type.OPERATOR);
            String op = tokens.get(-1).getLiteral();
            Ast.Expression right = parseComparisonExpression();
            left = new Ast.Expression.Binary(op,left,right);
        }
        return left;
    }
    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expression left = parseAdditiveExpression();
        while(peek("<") || peek(">") || peek("==") || peek("!=")){
            match(Token.Type.OPERATOR);
            String op = tokens.get(-1).getLiteral();
            Ast.Expression right = parseAdditiveExpression();
            left = new Ast.Expression.Binary(op,left,right);
        }
        return left;
    }
    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expression left = parseMultiplicativeExpression();
        while(peek("+") || peek("-")){
            match(Token.Type.OPERATOR);
            String op = tokens.get(-1).getLiteral();
            if (!tokens.has(0)){
                throw new ParseException("Missing Operand", tokens.get(-
                        1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            Ast.Expression right = parseMultiplicativeExpression();
            left = new Ast.Expression.Binary(op,left,right);
        }
        return left;
    }
    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        Ast.Expression left = parsePrimaryExpression();
        while(peek("*") || peek("/") || peek("^")){
            match(Token.Type.OPERATOR);
            String op = tokens.get(-1).getLiteral();
            Ast.Expression right = parsePrimaryExpression();
            left = new Ast.Expression.Binary(op,left,right);
        }
        return left;
    }
    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        //throw new UnsupportedOperationException(); //TODO
        if(peek("(") || peek(")")){
            if(peek("(")){
                match("(");
                Ast.Expression.Group group = new
                        Ast.Expression.Group(parseExpression());
                if(peek(")")){
                    match(")");
                    return group;
                }
            }
            throw new ParseException("Expected closing parenthesis `)`.",
                    tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
        }
        else if(peek(Token.Type.IDENTIFIER)){
            if(peek("NIL")){
                match(Token.Type.IDENTIFIER);
                return new Ast.Expression.Literal(null);
            }
            else if(peek("TRUE")){
                match(Token.Type.IDENTIFIER);
                return new Ast.Expression.Literal(true);
            }
            else if(peek("FALSE")){
                match(Token.Type.IDENTIFIER);
                return new Ast.Expression.Literal(false);
            }
            else{
                String name = tokens.get(0).getLiteral();
                match(Token.Type.IDENTIFIER);
// String name = tokens.get(-1).getLiteral();
                if (peek("(")) {
                    match("(");
                    List<Ast.Expression> arguments = new ArrayList<>();
                    while (!peek(")")) {
                        match(",");
                        if (peek(")")){
                            throw new ParseException("Trailing Comma",
                                    tokens.get(0).getIndex());
                        }
                        arguments.add(parseExpression());
                    }
                    match(")");
                    return new Ast.Expression.Function(name, arguments);
                }
                else { if (peek("[")) {
                    String name2 = tokens.get(-1).getLiteral();
                    match("[");
                    if (peek("]")){
                        throw new ParseException("Nothing inside brackets",
                                tokens.get(0).getIndex());
                    }
                    Ast.Expression exp = parseExpression();
                    if (peek("]")){
                        match("]");
                        return new Ast.Expression.Access(Optional.ofNullable(exp),
                                name2);
                    } else {
                        throw new ParseException("Missing Closing Bracket",
                                tokens.get(-1).getIndex()+ tokens.get(-1).getLiteral().length());
                    }
                }
                    return new Ast.Expression.Access(Optional.empty(), name);
                }
            }
        }
        else if(peek(Token.Type.INTEGER)){
            match(Token.Type.INTEGER);
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-
                    1).getLiteral()));
        }
        else if(peek(Token.Type.DECIMAL)){
            match(Token.Type.DECIMAL);
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(-
                    1).getLiteral()));
        }
        else if(peek(Token.Type.CHARACTER)){
            match(Token.Type.CHARACTER);
            return new Ast.Expression.Literal(tokens.get(-
                    1).getLiteral().charAt(1));
        }
        else if(peek(Token.Type.STRING)){
            String newString = tokens.get(0).getLiteral();
            newString = newString.replace("\\b", "\b");
            newString = newString.replace("\\n", "\n");
            newString = newString.replace("\\r", "\r");
            newString = newString.replace("\\t", "\t");
            newString = newString.replace("\\\'", "\'");
            newString = newString.replace("\\\"", "\"");
            newString = newString.replace("\\\\", "\\");
            newString = newString.substring(1,newString.length()-1);
            match(Token.Type.STRING);
            return new Ast.Expression.Literal(newString);
        }
        else{
            throw new ParseException("Invalid Expression",
                    tokens.get(0).getIndex());
        }
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
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        for(int i = 0; i < patterns.length; i++){
            if(!tokens.has(i)){
                return false;
            }
            else if(patterns[i] instanceof Token.Type){
                if(patterns[i] != tokens.get(i).getType()){
                    return false;
                }
            }
            else if(patterns[i] instanceof String){
                if(!patterns[i].equals(tokens.get(i).getLiteral())){
                    return false;
                }
            }
            else{
                throw new AssertionError("Invalid pattern object: " +
                        patterns[i].getClass());
            }
        }
        return true;
    }
    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        //throw new UnsupportedOperationException(); //TODO (in lecture)
        boolean peek = peek(patterns);
        if(peek){
            for(int i = 0; i<patterns.length;i++){
                tokens.advance();
            }
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