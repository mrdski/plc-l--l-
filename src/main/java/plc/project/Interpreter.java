package plc.project;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
public class Interpreter implements Ast.Visitor<Environment.PlcObject> {
    private Scope scope = new Scope(null);
    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }
    public Scope getScope() {
        return scope;
    }
    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        ast.getGlobals().forEach(this::visit);
        ast.getFunctions().forEach(this::visit);
        return scope.lookupFunction("main", 0).invoke(new ArrayList<>());
    }
    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), ast.getMutable(),
                    visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
        }
        return Environment.NIL;
    }
    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        Scope originalScope = scope;
        scope.defineFunction(ast.getName(),ast.getParameters().size(), args -> {
            scope = new Scope(originalScope);
            for (int i = 0; i < ast.getParameters().size(); i++) {
                scope.defineVariable(ast.getParameters().get(i), true,
                        args.get(i));
            }
            try {
                ast.getStatements().forEach(this::visit);
            }
            catch (Return returnValue) {
                return returnValue.value;
            }
            finally {
                scope = originalScope;
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        } else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }
        return Environment.NIL;
    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        Environment.PlcObject value = visit(ast.getValue());
        Ast.Expression.Access access = (Ast.Expression.Access)ast.getReceiver();
        Environment.Variable variable = scope.lookupVariable(access.getName());
        if (access.getOffset().isPresent()) {
            requireType(List.class, variable.getValue());
            int offset =
                    ((BigInteger)visit(access.getOffset().get()).getValue()).intValue();
            ((List<Object>)variable.getValue().getValue()).set(offset,
                    value.getValue());
        }
        else {
            variable.setValue(value);
        }
        return Environment.NIL;
    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Environment.PlcObject condition = visit(ast.getCondition());
        requireType(Boolean.class, condition);
        scope = new Scope(scope);
        try{
            if ((Boolean)condition.getValue()) {
                ast.getThenStatements().forEach(this::visit);
            }
            else {
                ast.getElseStatements().forEach(this::visit);
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        Environment.PlcObject condition = visit(ast.getCondition());
        scope = new Scope(scope);
        try{
            for (Ast.Statement.Case caseStatement:ast.getCases()) {
                if (caseStatement.getValue().isPresent()) {
                    Environment.PlcObject caseValue =
                            visit(caseStatement.getValue().get());
                    if (condition.getValue().equals(caseValue.getValue())) {
                        caseStatement.getStatements().forEach(this::visit);
                        return Environment.NIL;
                    }
                }
                else {
                    caseStatement.getStatements().forEach(this::visit);
                    return Environment.NIL;
                }
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        ast.getStatements().forEach(this::visit);
        return Environment.NIL;
    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))){
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }
    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new Return(visit(ast.getValue()));
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        else{
            return Environment.create(ast.getLiteral());
        }
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());
        Object leftValue = left.getValue();
        switch (ast.getOperator()) {
            case "&&":
                if (!(Boolean)leftValue) {
                    return Environment.create(false);
                }
                else {
                    return Environment.create((Boolean)leftValue &&
                            (Boolean)visit(ast.getRight()).getValue());
                }
            case "||":
                if ((Boolean)leftValue) {
                    return Environment.create(true);
                }
                else {
                    return Environment.create((Boolean)leftValue ||
                            (Boolean)visit(ast.getRight()).getValue());
                }
            case "<":
                return Environment.create(((Comparable)
                        leftValue).compareTo(visit(ast.getRight()).getValue())<0);
            case ">":
                return Environment.create(((Comparable)
                        leftValue).compareTo(visit(ast.getRight()).getValue())>0);
            case "==":
                return
                        Environment.create(leftValue.equals(visit(ast.getRight()).getValue()));
            case "!=":
                return Environment.create(!
                        leftValue.equals(visit(ast.getRight()).getValue()));
            case "+":
                if (leftValue instanceof String && visit(ast.getRight()).getValue()
                        instanceof String) {
                    return Environment.create(leftValue.toString() +
                            visit(ast.getRight()).getValue().toString());
                } else if (leftValue instanceof BigDecimal &&
                        visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(new
                            BigDecimal(leftValue.toString()).add(new
                            BigDecimal(visit(ast.getRight()).getValue().toString())));
                } else if (leftValue instanceof BigInteger &&
                        visit(ast.getRight()).getValue() instanceof BigInteger) {
                    return
                            Environment.create(((BigInteger)leftValue).add((BigInteger)visit(ast.getRight()).getValue()));
                } else {
                    throw new RuntimeException("Mismatched Types +");
                }
            case "-":
                if (leftValue instanceof BigDecimal &&
                        visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(new
                            BigDecimal(leftValue.toString()).subtract(new
                            BigDecimal(visit(ast.getRight()).getValue().toString())));
                }
                else if (leftValue instanceof BigInteger &&
                        visit(ast.getRight()).getValue() instanceof BigInteger){
                    return
                            Environment.create(((BigInteger)leftValue).subtract((BigInteger)visit(ast.getRight(
                            )).getValue()));
                } else {
                    throw new RuntimeException("Mismatched Types -");
                }
            case "*":
                if (leftValue instanceof BigDecimal &&
                        visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(new
                            BigDecimal(leftValue.toString()).multiply(new
                            BigDecimal(visit(ast.getRight()).getValue().toString())));
                }
                else if (leftValue instanceof BigInteger &&
                        visit(ast.getRight()).getValue() instanceof BigInteger){
                    return
                            Environment.create(((BigInteger)leftValue).multiply((BigInteger)visit(ast.getRight(
                            )).getValue()));
                } else {
                    throw new RuntimeException("Mismatched Types *");
                }
            case "/":
                if (leftValue instanceof BigDecimal &&
                        visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    return Environment.create(new
                            BigDecimal(leftValue.toString()).divide(new
                            BigDecimal(visit(ast.getRight()).getValue().toString()), RoundingMode.HALF_EVEN));
                }
                else if (leftValue instanceof BigInteger &&
                        visit(ast.getRight()).getValue() instanceof BigInteger){
                    return
                            Environment.create(((BigInteger)leftValue).divide((BigInteger)visit(ast.getRight())
                                    .getValue()));
                } else {
                    throw new RuntimeException("Mismatched Types /");
                }
            case "^":
                if (leftValue instanceof BigDecimal ||
                        visit(ast.getRight()).getValue() instanceof BigDecimal) {
                    throw new RuntimeException("Can't use ^ operator with decimals");
                }
                if (leftValue instanceof BigInteger) {
                    BigInteger base = (BigInteger)leftValue;
                    BigInteger exponent =
                            (BigInteger)visit(ast.getRight()).getValue();
                    return Environment.create(base.pow(exponent.intValue()));
                }
                else{
                    throw new UnsupportedOperationException();
                }
            default:
                throw new UnsupportedOperationException();
        }
        // throw new UnsupportedOperationException();
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Environment.Variable variable = scope.lookupVariable(ast.getName());
        if (variable == null) {
            return Environment.NIL;
        }
        if (ast.getOffset().isPresent()) {
            Environment.PlcObject offset = visit(ast.getOffset().get());
            requireType(BigInteger.class, offset);
            // Check if variable value is a list
            if (variable.getValue().getValue() instanceof List) {
                return Environment.create(((List<Environment.PlcObject>)
                        variable.getValue().getValue()).get(((BigInteger) offset.getValue()).intValue()));
            }
            else {
                throw new UnsupportedOperationException();
            }
        }
        else {
            return variable.getValue();
        }
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        Environment.Function function =
                scope.lookupFunction(ast.getName(),ast.getArguments().size());
        List<Environment.PlcObject> arguments =
                ast.getArguments().stream().map(this::visit).collect(Collectors.toList());
        return function.invoke(arguments);
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> list = new ArrayList<>();
        for (Ast.Expression expr:ast.getValues()) {
            list.add(visit(expr).getValue());
        }
        return Environment.create(list);
    }
    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }
    /**
     * Exception class for returning values.
     */
    public static class Return extends RuntimeException {
        public final Environment.PlcObject value;
        public Return(Environment.PlcObject value) {
            this.value = value;
        }
    }
}