package com.keon.projects.math.calculator;

public class Calculator {

    static final String[] OPS = Operator.getOps();
    static final String[] FNS = Function.getFunctions();

    private final Object[] args;

    private Calculator(final Object[] objs) {
        this.args = objs;
    }

    public static double eval(final String exp) {
        return Evaluator.eval(preprocess(exp)).val;
    }

    static double calculate(Object[] objs) {
        if (objs.length == 0)
            return 0;
        double x0 = (double) objs[0];
        if (objs.length == 1)
            return x0;
        return new Calculator(objs).calculate0(x0, 2, objs.length);
    }
    
    private static String preprocess(String exp) {
        return Constants.evalConstants(exp.replace(" ", ""));
    }

    // k0 should be on a number
    private double calculate0(double v, final int k0, final int kf) {
        for (int k = k0; k < kf;) {
            final Operator op1 = new Operator((String) args[k - 1]);
            double x = (double) args[k];
            if (k + 1 < kf) {
                final Operator op2 = new Operator((String) args[k + 1]);
                if (op2.isSuperiorTo(op1)) {
                    int kff = getNextCompatibleOccurrenceOf(k + 1, op1); //e.g for + it will be + or -
                    double y = calculate0(x, k + 2, kff);
                    v = op1.apply(v, y);
                    k = kff + 1;
                } else {
                    v = op1.apply(v, x);
                    k += 2;
                }
            } else {
                v = op1.apply(v, x);
                k += 2;
            }
        }
        return v;
    }

    private int getNextCompatibleOccurrenceOf(final int k0, final Operator op) {
        for (int k = k0; k < args.length; k += 2) {
            if (op.isInSameClassAs(new Operator((String) args[k]))) {
                return k;
            }
        }
        return args.length;
    }

}
