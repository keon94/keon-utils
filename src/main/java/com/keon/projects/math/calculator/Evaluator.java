package com.keon.projects.math.calculator;

import static com.keon.projects.math.calculator.Calculator.FNS;
import static com.keon.projects.math.calculator.Calculator.OPS;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class Evaluator {

    private Evaluator() {
    }

    /**
     * Evaluates the expression in ()
     * 
     * @param exp
     * @return
     */
    static IVPair<Double> eval(final String exp) {
        final List<Object> objs = new ArrayList<>(); // chars or doubles
        int idx = 0;
        while (idx < exp.length()) {
            IVPair<?> p;
            if ((p = evalOperator(exp.substring(idx))).val != null) {
                idx += p.idx;
                objs.add(p.val);
            } else if ((p = evalNumber(exp.substring(idx))).val != null) {
                idx += p.idx;
                objs.add(p.val);
            } else if ((p = evalExpression(exp.substring(idx))).val != null) {
                idx += p.idx;
                objs.add(p.val);
            } else if ((p = evalFunction(exp.substring(idx))).val != null) {
                idx += p.idx;
                objs.add(p.val);
            } else {
                idx++;
            }
        }
        final double res = calculate(objs);
        return new IVPair<>(idx, res);
    }

    private static double calculate(final List<Object> args) {
        Object arg0;
        if ((arg0 = args.get(0)) instanceof Character) {
            if (((char) arg0) == Operator.PLUS || ((char) arg0) == Operator.MINUS) {
                args.add(0, 0D);
            } else {
                throw new RuntimeException("Bad init arg: " + args);
            }
        }
        return Calculator.calculate(args.toArray());
    }

    private static IVPair<Double> evalNumber(final String s) {
        int idx = 0;
        int sign = 1;
        if (!Character.isDigit(s.charAt(0))) {
            if (s.charAt(0) == Operator.MINUS) {
                sign = -1;
            } else if (s.charAt(0) != Operator.PLUS) {
                return new IVPair<>(0, null);
            }
            idx = 1;
        }
        boolean decimal = false;
        while (idx < s.length()) {
            if (s.charAt(idx) == '.') {
                if (decimal) {
                    throw new RuntimeException("decimal error near " + s);
                }
                decimal = true;
                idx++;
            } else if (Character.isDigit(s.charAt(idx))) {
                idx++;
            } else {
                break;
            }
        }
        return new IVPair<>(idx, sign * Double.parseDouble(s.substring(0, idx)));
    }

    private static IVPair<Double> evalFunction(final String s) {
        int idx = 0;
        String function = null;
        for (int i = 0; i < FNS.length; ++i) {
            if (s.startsWith(FNS[i])) {
                idx += FNS[i].length();
                function = FNS[i];
                break;
            }
        }
        if (function == null) {
            return new IVPair<>(0, null);
        }
        final String expr = getExpression(s.substring(idx));
        if (expr == null) {
            throw new MalformedFunctionException("No () provided to " + function);
        }
        final double[] args = evalArgs(expr);
        idx += (expr.length() + 2);
        final double res = Function.get(function).apply(args);
        return new IVPair<>(idx, res);
    }

    private static IVPair<Double> evalExpression(final String s) {
        final String expr = getExpression(s);
        if (expr == null) {
            return new IVPair<>(0, null);
        }
        final IVPair<Double> eval = eval(expr);
        return new IVPair<Double>(eval.idx + 2, eval.val);
    }

    private static IVPair<Character> evalOperator(final String s) {
        char c = s.charAt(0);
        for (int i = 0; i < OPS.length; ++i) {
            if (c == OPS[i]) {
                return new IVPair<>(1, c);
            }
        }
        return new IVPair<>(0, null);
    }

    /**
     * Expected format: (...)
     */
    private static String getExpression(final String s) {
        if (s.charAt(0) != '(') {
            return null;
        }
        final Stack<Character> parastack = new Stack<>();
        parastack.add('(');
        int idx = 1;
        for (; idx < s.length(); ++idx) {
            final char c = s.charAt(idx);
            if (c == '(') {
                parastack.push(c);
            } else if (c == ')') {
                parastack.pop();
                if (parastack.empty()) {
                    break;
                }
            }
        }
        if (!parastack.empty()) {
            throw new UnbalancedParanthesisException("Unbalanced paranthesis found near " + s);
        }
        final String expr = s.substring(1, idx);
        return expr;
    }

    private static double[] evalArgs(final String args) {
        String argsIter = args;
        final List<Double> arglist = new ArrayList<>();
        while (true) {
            int nextArgIdx = getNextArgIndex(argsIter);
            if (nextArgIdx == -1) {
                break;
            }
            final String thisArg = argsIter.substring(0, nextArgIdx);
            final double x = eval(thisArg).val;
            arglist.add(x);
            if (nextArgIdx == argsIter.length()) {
                break;
            }
            argsIter = argsIter.substring(nextArgIdx + 1);
        }
        return arglist.stream().mapToDouble(d -> d).toArray();
    }

    /**
     * expects format: a OR a,b OR a,b,c etc. returns the next appropriate comma
     */
    private static int getNextArgIndex(final String args) {
        if (args.isEmpty()) {
            return -1;
        }
        if (args.charAt(0) == ',') {
            throw new RuntimeException("Saw ,, near " + args);
        }
        final Stack<Character> parastack = new Stack<>();
        int idx = 0;
        for (; idx < args.length(); ++idx) {
            final char c = args.charAt(idx);
            if (c == '(') {
                parastack.push(c);
            } else if (c == ')') {
                parastack.pop();
            } else if (c == ',' && parastack.empty()) {
                break;
            }
        }
        if (!parastack.empty()) {
            throw new UnbalancedParanthesisException("Unbalanced paranthesis found in " + args);
        }
        return idx;
    }

    static class IVPair<T> {
        final int idx;
        final T val;

        IVPair(final int idx, final T val) {
            this.idx = idx;
            this.val = val;
        }
    }

}
