package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {


    public Scope scope;
    private Ast.Function function;

    private Environment.Type type;


    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        ast.getGlobals().forEach(this::visit);
        ast.getFunctions().forEach(this::visit);
        if (!scope.lookupFunction("main", 0).getReturnType().equals(Environment.Type.INTEGER)) {
            throw new RuntimeException("Main method needs integer return");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        //Additionally, throws a RuntimeException if:
        //The value, if present, is not assignable to the global.
        //For a value to be assignable, its type must be a subtype of the global's type as defined above (section Assignable Types).
        ast.getValue().ifPresent(value -> {
            if (value instanceof Ast.Expression.PlcList) {
                ((Ast.Expression.PlcList) value).setType(Environment.getType(ast.getTypeName()));
            }
            visit(value);
            requireAssignable(Environment.getType(ast.getTypeName()), value.getType());
        });
        scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), ast.getMutable(), Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> types = new ArrayList<>();
        for (int i = 0; i < ast.getParameterTypeNames().size(); i++){
            types.add(Environment.getType(ast.getParameterTypeNames().get(i)));
        }
        Environment.Type returnType;
        if (ast.getReturnTypeName() != null){
            returnType = Environment.getType(ast.getReturnTypeName().get());
            type = returnType;
        } else {
            returnType = Environment.Type.NIL;
        }

        scope.defineFunction(ast.getName(), ast.getName(), types, returnType, args->Environment.NIL);
        Environment.Function function = scope.lookupFunction(ast.getName(), ast.getParameterTypeNames().size());
        ast.setFunction(function);

        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()){
                if (stmt instanceof Ast.Statement.Return){
                    visit(stmt);
                    requireAssignable(returnType, ((Ast.Statement.Return) stmt).getValue().getType());
                } else {
                    visit(stmt);
                }
            }
        }
//        catch (Interpreter.Return returnValue) {
//            visit(returnValue);
//        }
        finally {
            scope = scope.getParent();
        }

        return null;
////        throw new UnsupportedOperationException();  // TODO

    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {
            throw new RuntimeException("Not function expr");
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type type;
        if (!(ast.getTypeName().isPresent())) {
            if (!(ast.getValue().isPresent())) {
                throw new RuntimeException("Not assignable");
            }
            visit(ast.getValue().get());
            type = ast.getValue().get().getType();
        } else {
            type = Environment.getType(ast.getTypeName().get());
            if (ast.getValue().isPresent()) {
                visit(ast.getValue().get());
                requireAssignable(type, ast.getValue().get().getType());
            }
        }
        scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Not access");
        }
        //The receiver is not an access expression (since any other type is not assignable).
        //The value is not assignable to the receiver (see Ast.Global for additional details).
        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Invalid condition in th if");
        }
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Then block is empty");
        }
        scope = new Scope(scope);
        ast.getElseStatements().forEach(this::visit);
        scope = scope.getParent();
        scope = new Scope(scope);
        ast.getThenStatements().forEach(this::visit);
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());
        List<Ast.Statement.Case> caseList = ast.getCases();
        for (int i = 0; i < caseList.size(); i++) {
            if (caseList.get(i).getValue().isPresent()) {
                //Check this RuntimeException
                if (i == caseList.lastIndexOf(caseList)) {
                    throw new RuntimeException("Last case doesnt have value");
                }
                visit(caseList.get(i).getValue().get());
                if (!caseList.get(i).getValue().get().getType().equals(ast.getCondition().getType())) {
                    throw new RuntimeException("Condition and case dont match");
                }
            }
            visit(caseList.get(i));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        scope = new Scope(scope);
        ast.getStatements().forEach(this::visit);
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        if (!ast.getCondition().getType().equals(Environment.Type.BOOLEAN)) {
            throw new RuntimeException("Not boolean");
        }
        scope = new Scope(scope);
        ast.getStatements().forEach(this::visit);
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        //Validates a return statement. Throws a RuntimeException if:
        //The value is not assignable to the return type of the function within which the statement is contained.
        //As hinted in Ast.Function, you will need to coordinate between these visits to accomplish this.
        visit(ast.getValue());
        requireAssignable(this.type, ast.getValue().getType());
        return null;
    }



    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() instanceof BigInteger) {
            BigInteger integerValue = (BigInteger) ast.getLiteral();
            //Check these RuntimeEceptions make sure they are right bc im not sure if that how it supposed to be
            if (integerValue.abs().compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new RuntimeException("Integer too big");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        else if (ast.getLiteral() instanceof BigDecimal) {
            BigDecimal decimalValue = (BigDecimal) ast.getLiteral();
            try {
                double doubleVal = decimalValue.doubleValue();
                if (Double.isInfinite(doubleVal)) {
                    throw new RuntimeException("Decimal too big");
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Decimal to big");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        else if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        else {
            ast.setType(Environment.Type.NIL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Not binary.");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        String operator = ast.getOperator();
        if ("&&".equals(operator) || "||".equals(operator)) {
            visit(ast.getLeft());
            visit(ast.getRight());
            if (ast.getLeft().getType().equals(Environment.Type.BOOLEAN) && ast.getRight().getType().equals(Environment.Type.BOOLEAN)) {
                ast.setType(Environment.Type.BOOLEAN);
            } else {
                throw new RuntimeException("Expecting boolean values either side");
            }
        } else if ("<".equals(operator) || ">".equals(operator) || "==".equals(operator) || "!=".equals(operator)) {
            visit(ast.getLeft());
            visit(ast.getRight());
            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
            if (ast.getLeft().getType().equals(ast.getRight().getType())) {
                ast.setType(Environment.Type.BOOLEAN);
            } else {
                throw new RuntimeException("Left right not equal");
            }
        } else if ("+".equals(operator)) {
            visit(ast.getLeft());
            visit(ast.getRight());
            if (ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)) {
                ast.setType(Environment.Type.STRING);
            } else {
                if (ast.getLeft().getType().equals(Environment.Type.INTEGER) && ast.getRight().getType().equals(Environment.Type.INTEGER)) {
                    ast.setType(Environment.Type.INTEGER);
                } else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL) && ast.getRight().getType().equals(Environment.Type.DECIMAL)) {
                    ast.setType(Environment.Type.DECIMAL);
                } else {
                    throw new RuntimeException("Invalid binary expression");
                }
            }
        } else if ("-".equals(operator) || "*".equals(operator) || "/".equals(operator)) {
            visit(ast.getLeft());
            visit(ast.getRight());
            if (ast.getLeft().getType().equals(Environment.Type.INTEGER) && ast.getRight().getType().equals(Environment.Type.INTEGER)) {
                ast.setType(Environment.Type.INTEGER);
            } else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL) && ast.getRight().getType().equals(Environment.Type.DECIMAL)) {
                ast.setType(Environment.Type.DECIMAL);
            } else {
                throw new RuntimeException("Invalid binary expression");
            }
        } else if ("^".equals(operator)) {
            visit(ast.getLeft());
            visit(ast.getRight());
            if ((ast.getLeft().getType().equals(Environment.Type.INTEGER) || ast.getLeft().getType().equals(Environment.Type.DECIMAL)) && ast.getRight().getType().equals(Environment.Type.INTEGER)) {
                ast.setType(ast.getLeft().getType());
            } else {
                throw new RuntimeException("Invalid binary expression");
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getOffset().isPresent()) {
            visit(ast.getOffset().get());
            if (!ast.getOffset().get().getType().equals(Environment.Type.INTEGER)) {
                throw new RuntimeException("Offset not integer");
            }
        }
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        List<Ast.Expression> args = ast.getArguments();
        List<Environment.Type> params = ast.getFunction().getParameterTypes();
        for (int i = 0; i < args.size(); i++) {
            visit(args.get(i));
            requireAssignable(params.get(i), args.get(i).getType());
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        //Validates the list. Throws a RuntimeException if:
        //
        //The expressions are not assignable to the list type.
        //For a value to be assignable, its type must be a subtype of the list's type as defined in Ast.Global.
        List<Ast.Expression> values = ast.getValues();
        for (Ast.Expression val : values) {
            visit(val);
            requireAssignable(ast.getType(), val.getType());
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (!target.equals(Environment.Type.ANY) && !target.equals(type)) {
            if (target.equals(Environment.Type.COMPARABLE)) {
                List<String> comparableTypes = Arrays.asList("Integer", "Decimal", "Character", "String");
                if (!comparableTypes.contains(type.getName())) {
                    throw new RuntimeException("Invalid assignment: attempting to assign " + type.getName() + " to a " + target.getName() + " variable.");
                }
            } else {
                throw new RuntimeException("Invalid assignment: attempting to assign " + type.getName() + " to a " + target.getName() + " variable.");
            }
        }
    }
}
