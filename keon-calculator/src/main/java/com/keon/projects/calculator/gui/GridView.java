package com.keon.projects.calculator.gui;

import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;

class GridView {

    private final TextView textView;
    
    private GridPane standardGrid;
    private GridPane advancedGrid;

    GridView(final TextView textView) {
        this.textView = textView;
    }

    GridPane standardGrid() {
        
        if(standardGrid != null) {
            return standardGrid;
        }
                
        final GridBuilder gb = new GridBuilder(new Insets(1, 1, 1, 1));

        gb.set(button -> {
            button.setMinWidth(gb.getGrid().getPrefWidth());
            button.setMaxWidth(Double.MAX_VALUE);
        }, new Button[][] { 
                { button("%"), button("CE"), button("C"), button("<-"), button("/") }, //
                { button("sqrt"), button("7"), button("8"), button("9"), button("*") }, //
                { button("x^2"), button("4"), button("5"), button("6"), button("-") }, //
                { button("x^3"), button("1"), button("2"), button("3"), button("+") }, //
                { button("1/x"), button("+/-"), button("0"), button("."), button("=") } //
        });

        standardGrid = gb.getGrid();
        return standardGrid;
    }

    public GridPane advancedGrid() {
        
        if(advancedGrid != null) {
            return advancedGrid;
        }
        
        final GridBuilder gb = new GridBuilder(new Insets(1, 1, 1, 1));

        gb.set(button -> {
            button.setMinWidth(gb.getGrid().getPrefWidth());
            button.setMaxWidth(Double.MAX_VALUE);
        }, new Button[][] { //TODO make the buttons
                { button("%"), button("%"), button("CE"), button("C"), button("<-"), button("/") }, //
                { button("%"), button("sqrt"), button("7"), button("8"), button("9"), button("*") }, //
                { button("%"), button("x^2"), button("4"), button("5"), button("6"), button("-") }, //
                { button("%"), button("x^3"), button("1"), button("2"), button("3"), button("+") }, //
                { button("%"), button("1/x"), button("+/-"), button("0"), button("."), button("=") } //
        });

        advancedGrid = gb.getGrid();
        return advancedGrid;
    }

    private Button button(final String name) {
        final Button button = new Button(name);
        button.setOnAction(e -> textView.execute(name));
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private static class GridBuilder {

        private final GridPane grid;

        GridBuilder(final Insets insets) {
            this.grid = new GridPane();
            grid.setPadding(insets);
            grid.setVgap(1);
            grid.setHgap(1);
        }

        public GridPane getGrid() {
            return this.grid;
        }

        public <T extends Region> void set(final Consumer<T> action, final T[][] nodes) {
            for (int row = 0; row < nodes.length; ++row) {
                for (int col = 0; col < nodes[row].length; ++col) {
                    final T node = nodes[row][col];
                    node.prefHeightProperty().bind(grid.heightProperty());
                    node.prefWidthProperty().bind(grid.widthProperty());
                    action.accept(node);
                    GridPane.setConstraints(node, col, row);
                    grid.getChildren().add(node);
                }
            }
        }
    }
}