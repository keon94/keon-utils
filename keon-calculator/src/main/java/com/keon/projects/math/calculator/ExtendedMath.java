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

}
