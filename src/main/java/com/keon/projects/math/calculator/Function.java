package com.keon.projects.math.calculator;

import java.util.HashMap;
import java.util.Map;

class Function {

    private static final Map<String, Ifunction> F_MAP = new HashMap<>();

    static {
        F_MAP.put("sin", Ifunction.constarg(1, x -> Math.sin(x[0])));
        F_MAP.put("cos", Ifunction.constarg(1, x -> Math.cos(x[0])));
        F_MAP.put("tan", Ifunction.constarg(1, x -> Math.tan(x[0])));
        F_MAP.put("exp", Ifunction.constarg(1, x -> Math.exp(x[0])));
        F_MAP.put("ln", Ifunction.constarg(1, x -> Math.log(x[0])));
        F_MAP.put("avg", Ifunction.vararg(1, ExtendedMath::avg));
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
    private final java.util.function.Function<double[], Double> f;

    private Ifunction(final boolean vararg, final int expectedArgCount,
            java.util.function.Function<double[], Double> f) {
        this.expectedArgCount = expectedArgCount;
        this.f = f;
        this.vararg = vararg;
    }

    static Ifunction constarg(final int expectedArgCount, java.util.function.Function<double[], Double> f) {
        return new Ifunction(false, expectedArgCount, f);
    }

    static Ifunction vararg(final int expectedArgCount, java.util.function.Function<double[], Double> f) {
        return new Ifunction(true, expectedArgCount, f);
    }

    Double apply(final double... args) {
        assert (expectedArgCount >= 0);
        if (vararg) {
            if (args.length < expectedArgCount) {
                throw new RuntimeException(
                        "expected minimum of: " + expectedArgCount + ". Got " + args.length + " args");
            }
        } else if (args.length != expectedArgCount) {
            throw new RuntimeException("expected: " + expectedArgCount + ". Got " + args.length + " args");
        }
        return f.apply(args);
    }
}
