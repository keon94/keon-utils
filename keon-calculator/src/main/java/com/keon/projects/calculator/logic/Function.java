package com.keon.projects.calculator.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

class Function {

    private static final Map<String, Ifunction> F_MAP = new HashMap<>();
    
    private Function() {}

    static {
        F_MAP.put("sin", Ifunction.constarg(1, x -> Math.sin(x[0])));
        F_MAP.put("cos", Ifunction.constarg(1, x -> Math.cos(x[0])));
        F_MAP.put("tan", Ifunction.constarg(1, x -> Math.tan(x[0])));
        F_MAP.put("exp", Ifunction.constarg(1, x -> Math.exp(x[0])));
        F_MAP.put("ln", Ifunction.constarg(1, x -> Math.log(x[0])));
        F_MAP.put("avg", Ifunction.vararg(1, ExtendedMath::avg));
        F_MAP.put("sqrt", Ifunction.constarg(1, x -> Math.sqrt(x[0])));
    }

    public static String[] getFunctions() {
        return F_MAP.keySet().toArray(new String[F_MAP.size()]);
    }
    
    public static Ifunction get(final String fName) {
        final Ifunction f = F_MAP.get(fName);
        if(f == null) {
            throw new UnsupportedOperationException(fName + " is not a supported function");
        }
        return f;
    }
}

class Ifunction {

    private final boolean vararg;
    private final int expectedArgCount;
    private final ToDoubleFunction<double[]> f;

    private Ifunction(final boolean vararg, final int expectedArgCount,
            ToDoubleFunction<double[]> f) {
        this.expectedArgCount = expectedArgCount;
        this.f = f;
        this.vararg = vararg;
    }

    static Ifunction constarg(final int expectedArgCount, final ToDoubleFunction<double[]> f) {
        return new Ifunction(false, expectedArgCount, f);
    }

    static Ifunction vararg(final int expectedArgCount, final ToDoubleFunction<double[]> f) {
        return new Ifunction(true, expectedArgCount, f);
    }

    Double apply(final double... args) {
        assert (expectedArgCount >= 0);
        if (vararg) {
            if (args.length < expectedArgCount) {
                throw new ArgumentCountException(
                        "expected minimum of: " + expectedArgCount + ". Got " + args.length + " args");
            }
        } else if (args.length != expectedArgCount) {
            throw new ArgumentCountException("expected: " + expectedArgCount + ". Got " + args.length + " args");
        }
        return f.applyAsDouble(args);
    }
}

class Constants {
    
    private Constants() {}
    
    static final String PI = "'pi'";
    static final String E = "'e'";
    
    static String evalConstants(final String exp) {
        return exp.replace(PI, Double.toString(Math.PI)).replace(E, Double.toString(Math.E));
    }

}
