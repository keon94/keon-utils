package com.keon.projects.calculator.gui;

import java.util.HashMap;
import java.util.Map;

import com.keon.projects.calculator.logic.VariableCalculator;

import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;

class TextView {

    private final Map<String, Runnable> specialFunctions = new HashMap<>();

    /** shared **/
    final TextArea textArea = new TextArea();
    private final StringBuilder input = new StringBuilder();
    
    //mutable
    private double result = 0;
    private boolean toReset = false;

    TextView(final Pane layout) {
        specialFunctions.put("%", () -> append("/100"));
        specialFunctions.put("CE", () -> reset());
        specialFunctions.put("C", () -> reset());
        specialFunctions.put("<-", () -> back());
        specialFunctions.put("=", () -> compute());
        specialFunctions.put("sqrt", () -> append("^0.5"));
        specialFunctions.put("x^2", () -> append("^2"));
        specialFunctions.put("x^3", () -> append("^3"));
        specialFunctions.put("1/x", () -> append("^(-1)"));
        specialFunctions.put("+/-", () -> append("*(-1)"));

        textArea.prefHeightProperty().bind(layout.heightProperty());
        textArea.setDisable(true);
        textArea.setFont(Font.font(18));
        textArea.setStyle("-fx-text-fill: black;");
    }

    void execute(final String name) {
        if (toReset) {
            textArea.setText("" + result);
            input.setLength(0);
            if(Math.abs(result) < Double.MAX_VALUE)
                input.append(result);
            toReset = false;
        }
        final Runnable r = specialFunctions.get(name);
        if (r == null) {
            append(name);
        } else {
            r.run();
        }
    }

    private void reset() {
        textArea.clear();
        input.setLength(0);
    }

    private void append(final String o) {
        input.append(o);
        textArea.setText(input.toString());
    }

    private void back() {
        final String text = textArea.getText();
        if (!text.isEmpty())
            textArea.deleteText(text.length() - 1, text.length());
    }

    private void compute() {
        final String text = textArea.getText();
        result = new VariableCalculator().eval(text);
        textArea.setText(text + "\n    " + new VariableCalculator().eval(text));
        toReset = true;
    }

}
