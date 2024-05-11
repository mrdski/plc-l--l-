package plc.project;

import java.io.PrintWriter;
import java.util.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;


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

        indent++;
        for (int i = 0; i < ast.getGlobals().size(); i++){
            newline(0);
            newline(indent);
            print(ast.getGlobals().get(i));
        }
        indent--;

        newline(0);
        indent++;
        newline(indent);
        print("public static void main(String[] args) {");
        indent++;
        newline(indent);
        print("System.exit(new Main().main());");
        indent--;
        newline(indent);
        print("}");
        indent--;

        newline(indent);
        indent++;
        for (int i = 0; i < ast.getFunctions().size(); i++){

            newline(indent);
            print(ast.getFunctions().get(i));
            newline(0);
        }
        indent--;

        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        String typeName = switch (ast.getTypeName()) {
            case "Integer" -> "int";
            case "Decimal" -> "double";
            case "Boolean" -> "boolean";
            case "Character" -> "char";
            case "String" -> "String";
            default -> throw new IllegalArgumentException("Wrong type " + ast.getTypeName());
        };

        if(ast.getValue().isPresent()){
            if (ast.getValue().get() instanceof Ast.Expression.PlcList){
                typeName = typeName + "[]";
            }
        }

        if(ast.getMutable() == false){
            print("final ");
        }

        print(typeName, " ", ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {

        print(ast.getFunction().getReturnType().getJvmName(), " ", ast.getName(), "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {

            String typeName = switch (ast.getParameterTypeNames().get(i)) {
                case "Integer" -> "int";
                case "Decimal" -> "double";
                case "Boolean" -> "boolean";
                case "Character" -> "char";
                case "String" -> "String";
                default -> throw new IllegalArgumentException("Wrong type " + ast.getParameterTypeNames().get(i));
            };

            print(typeName, " ", ast.getParameters().get(i));
            if (i != ast.getParameters().size() - 1) {
                print(", ");
            }
        }
        print(") {");
        if (!ast.getStatements().isEmpty()) {
            indent++;
            for (int i = 0; i<ast.getStatements().size(); i++) {
                newline(indent);
                print(ast.getStatements().get(i));
            }
            indent--;
            newline(indent);
        }
        print("}");
        return null;

    }



    @Override
    public Void visit(Ast.Statement.Expression ast) {
//
//        if (ast.getExpression() instanceof Ast.Expression.Literal){
//            print((Ast.Expression.Literal)ast.getExpression());
//        }
//        if (ast.getExpression() instanceof Ast.Expression.Group){
//            print((Ast.Expression.Group)ast.getExpression());
//        }
//        if (ast.getExpression() instanceof Ast.Expression.Binary){
//            print((Ast.Expression.Binary)ast.getExpression());
//        }
//        if (ast.getExpression() instanceof Ast.Expression.Access){
//            print((Ast.Expression.Access)ast.getExpression());
//        }
//        if (ast.getExpression() instanceof Ast.Expression.Function){
//            print((Ast.Expression.Function)ast.getExpression());
//        }
//        if (ast.getExpression() instanceof Ast.Expression.PlcList){
//            print((Ast.Expression.PlcList)ast.getExpression());
//        }
//
//        return null;
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        String typeString = "";
        Environment.Type typeType = ast.getVariable().getType();

        if (ast.getTypeName().isPresent()){
            typeString = ast.getTypeName().get();
        }

        if (typeType != null){
            if (typeType == Environment.Type.INTEGER){
                print("int ");
            }
            if (typeType == Environment.Type.DECIMAL){
                print("double ");
            }
            if (typeType == Environment.Type.BOOLEAN){
                print("bool ");
            }
            if (typeType == Environment.Type.CHARACTER){
                print("char ");
            }
            if (typeType == Environment.Type.STRING){
                print("String ");
            }
        } else {
            switch (typeString){
                case ("Integer"):
                    print("int ");
                    break;
                case ("Decimal"):
                    print("double ");
                    break;
                case ("Boolean"):
                    print("bool ");
                    break;
                case ("Character"):
                    print("char ");
                    break;
                case ("String"):
                    print("String ");
                    break;
            }
        }

        print(ast.getName());

        if (ast.getValue().isEmpty()){
            print(";");
        } else {
            print(" = ", ast.getValue().get());
//            if (ast.getValue().get() instanceof Ast.Expression.Literal){
//                print((Ast.Expression.Literal)ast.getValue().get());
//            }
//            if (ast.getValue().get() instanceof Ast.Expression.Group){
//                print((Ast.Expression.Group)ast.getValue().get());
//            }
//            if (ast.getValue().get() instanceof Ast.Expression.Binary){
//                print((Ast.Expression.Binary)ast.getValue().get());
//            }
//            if (ast.getValue().get() instanceof Ast.Expression.Access){
//                print((Ast.Expression.Access)ast.getValue().get());
//            }
//            if (ast.getValue().get() instanceof Ast.Expression.Function){
//                print((Ast.Expression.Function)ast.getValue().get());
//            }
//            if (ast.getValue().get() instanceof Ast.Expression.PlcList){
//                print((Ast.Expression.PlcList)ast.getValue().get());
//            }
            print(";");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver());
        print(" = ");
        print(ast.getValue());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        print(ast.getCondition());
        print(") {");
        indent++;
        for (int i = 0; i < ast.getThenStatements().size(); i++) {
            newline(indent);
            print(ast.getThenStatements().get(i));
        }
        indent--;
        newline(indent);
        print("}");
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            indent++;
            for (int i = 0; i < ast.getElseStatements().size(); i++) {
                newline(indent);
                print(ast.getElseStatements().get(i));
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }



    @Override
    public Void visit(Ast.Statement.Switch ast) {
        print("switch (");
        print(ast.getCondition());
        print(") {");
        indent++;
        for (int i = 0; i < ast.getCases().size(); i++){
            newline(indent);
            print(ast.getCases().get(i));
        }
        indent--;
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().isEmpty()) {
            print("default:");
        } else {
            print("case ");
            print(ast.getValue().get());
            print(":");
        }

        indent++;
        for (int i = 0; i < ast.getStatements().size(); i++){
            newline(indent);
            print(ast.getStatements().get(i));
        }
        indent--;

        indent++;
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
        print(ast.getCondition());
        print(") {");

        if (ast.getStatements().isEmpty()){
            print("}");
            return null;
        }

        indent++;
        for (int i = 0; i < ast.getStatements().size(); i++){
            newline(indent);
            print(ast.getStatements().get(i));
        }
        indent--;
        newline(indent);
        print("}");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;

    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        if (ast.getType() == Environment.Type.STRING){
            print("\"");
        }
        if (ast.getType() == Environment.Type.CHARACTER){
            print("'");
        }

        print(ast.getLiteral());

        if (ast.getType() == Environment.Type.STRING){
            print("\"");
        }
        if (ast.getType() == Environment.Type.CHARACTER){
            print("'");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;

    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        print(ast.getLeft()," ", ast.getOperator()," ", ast.getRight());
        return null;

    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getName());
        if (ast.getOffset().isPresent()) {
            print("[", ast.getOffset().get(), "]");
        }
        return null;

    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        print(ast.getFunction().getJvmName(), "(");
        if (!ast.getArguments().isEmpty()) {
            for (int i = 0; i < ast.getArguments().size(); i++) {
                visit(ast.getArguments().get(i));
                if (i < ast.getArguments().size() - 1) {
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
        if (!ast.getValues().isEmpty()) {
            for (int i = 0; i < ast.getValues().size(); i++) {
                visit(ast.getValues().get(i));
                if (i < ast.getValues().size() - 1) {
                    print(", ");
                }
            }
        }
        print("}");
        return null;
    }

}
