package com.keon.projects;

import java.util.Scanner;

import com.keon.projects.math.calculator.Calculator;

public class CalculatorMain {

    public static void main(String[] args) throws InterruptedException {
        // TODO Auto-generated method stub
        System.out.println("Press q to quit at any time");
        try (final Scanner in = new Scanner(System.in)) {
            while (true) {
                try {
                    System.out.print("> ");
                    final String input = in.nextLine();
                    if (input.equalsIgnoreCase("q")) {
                        return;
                    }
                    final double v = Calculator.eval(input);
                    System.out.println(v);
                } catch (final Exception e) {
                    e.printStackTrace(System.out);
                }
            }
        }
    }
}
