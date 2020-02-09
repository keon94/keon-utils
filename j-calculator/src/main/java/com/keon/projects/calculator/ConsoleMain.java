package com.keon.projects.calculator;

import com.keon.projects.calculator.logic.VariableCalculator;
import com.keon.projects.calculator.logic.VariableCalculator.Input;
import com.keon.projects.console.ConsoleInputManager;

public class ConsoleMain implements IMain {

    @Override
    public void main(final String... args) {
        System.out.println("Press q to quit at any time");
        try (final var console = new ConsoleInputManager(System.in, System.out)) {
            while (true) {
                System.out.print(ConsoleInputManager.LINE_BEGIN);
                final String input = console.getRequestedInput();
                if (input.equalsIgnoreCase("q")) {
                    break;
                }
                final Input pinput = processInput(input);
                final double v;
                try {
                    v = new VariableCalculator(pinput.vars).eval(pinput.expr);
                    System.out.println(v);
                } catch (final Exception e) {
                    e.printStackTrace();
                    onExceptionExec(input);
                }
            }
        } catch (final Throwable t) {
            t.printStackTrace();
        }
        System.exit(0);
    }

    private static Input processInput(final String input) {
        final String[] parts = input.split(";");
        final String[] vars = new String[parts.length - 1];
        System.arraycopy(parts, 0, vars, 0, parts.length - 1);
        return new Input(vars, parts[parts.length - 1]);
    }

    private static void onExceptionExec(final String input) {
        try {
            final String[] cmd = input.replaceAll("\\s\\s+", "\\s").split("\\s");
            new ProcessBuilder(cmd).inheritIO().start().waitFor();
        } catch (final Exception e) {
            System.err.println(input + " could not be run on the shell either: ");
            e.printStackTrace();
        }
    }

}
