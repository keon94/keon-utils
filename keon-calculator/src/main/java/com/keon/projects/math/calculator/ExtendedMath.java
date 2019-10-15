package com.keon.projects.math.calculator;

class ExtendedMath {
    
    private ExtendedMath() {}
    
    static double avg(double... args) {
        double total = 0.0;
        for(final double arg : args) {
            total += arg;
        }
        return total/args.length;
    }
   

    public static double root(double a, double b) {
        if(a < 0) {
            if(b%2 == 0)
                return Double.NaN;
            return -1 * Math.pow(-a, 1/b);
        }
        return Math.pow(a, 1/b);
        
    }
}
