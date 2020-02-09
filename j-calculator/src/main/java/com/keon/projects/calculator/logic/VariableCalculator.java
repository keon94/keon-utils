package com.keon.projects.calculator.logic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A calculator that let's you define variables and perform algebra with them.
 * The variable definitions must be noncyclic - this does not solve equations.
 * E.g. x=y+2 ; y = z^2 ; z=2; -> y=4; x=6
 * 
 * @author Keon Amini
 *
 */
public class VariableCalculator {

    private static final Pattern VAR_EXPR_PATTERN = Pattern.compile("(.+?)=(.+)");
    private final Map<String, Expression> varExpressions = new HashMap<>();

    public VariableCalculator(final String... vars) {
        for (final String varExp : vars) {
            final Matcher matcher = VAR_EXPR_PATTERN.matcher(varExp);
            if (matcher.find()) {
                final String var = matcher.group(1);
                validateVarName(var);
                final String expr = matcher.group(2);
                varExpressions.put(var, new Expression(expr));
            } else {
                throw new RuntimeException("Bad variable format: " + varExp);
            }
        }

        for (final Entry<String, Expression> e : varExpressions.entrySet()) {
            e.setValue(evalVariable(new HashSet<>(), e.getKey()));
        }

    }

    public double eval(final String expr) {
        if(expr.trim().isEmpty())
            return 0;
        return Calculator.eval(evalVariables(expr));
    }

    private String evalVariables(String expr) {
        final Iterator<Entry<String, Expression>> iter = varExpressions.entrySet().iterator();
        Entry<String, Expression> varEntry;
        while (iter.hasNext()) {
            varEntry = iter.next();
            expr = expr.replace(varEntry.getKey(), varEntry.getValue().expr);
            iter.remove();
        }
        return expr;
    }

    /**
     * 
     * @param unknowns
     * @param var
     * @return
     */
    private Expression evalVariable(final Set<String> unknowns, final String var) {
        final Expression e = varExpressions.get(var);
        if(e.simplified)
            return e;
        unknowns.add(var);
        for (final String v : varExpressions.keySet()) {
            if (!v.equals(var) && e.expr.contains(v)) {
                if(unknowns.contains(v)) {
                    throw new RuntimeException("Cyclic Equations are not allowed - Cycle found between 1+ symbols in " + unknowns + " and " + var + " in " + var + "=" + e.expr);
                }
                final String res = evalVariable(unknowns, v).expr;
                unknowns.remove(v);
                e.expr = e.expr.replace(v, res);
            }
        }
        final String res = Double.toString(Calculator.eval(e.expr));
        unknowns.remove(var);
        e.simplified = true;
        e.expr = res;
        return e;
    }

    private void validateVarName(final String var) {

    }

    private void validateExpressions() {
        //
    }

    public static class Input {
        public final String expr;
        public final String[] vars;

        public Input(final String[] vars, final String expr) {
            this.vars = vars;
            this.expr = expr;
        }
    }
    
    private static class Expression {
        String expr;
        boolean simplified;
        Expression(final String expr) {
            this.expr = expr;
            this.simplified = false;
        }
    }

}
