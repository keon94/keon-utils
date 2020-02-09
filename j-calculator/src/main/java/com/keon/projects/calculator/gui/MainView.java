package com.keon.projects.calculator.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainView extends Application {

    private GridView gridView;
    private Stage window;

    @Override
    public void start(final Stage window) throws Exception {

        this.window = window;

        window.setTitle("Calculator");

        final Pane layout = new VBox();

        final ToolBar toolbar = toolbar(layout);
        layout.getChildren().add(toolbar);

        final TextView textView = new TextView(layout);
        gridView = new GridView(textView);

        layout.getChildren().add(textView.textArea);

        final GridPane grid = gridView.standardGrid();
        grid.prefHeightProperty().bind(layout.heightProperty());
        layout.getChildren().add(grid);

        final Scene scene = new Scene(layout, 300, 250);
        window.setScene(scene);
        window.show();
    }

    private ToolBar toolbar(final Pane layout) {
        final MenuItem std = new MenuItem("Standard");
        std.setOnAction(e -> {
            final GridPane grid = gridView.standardGrid();
            grid.prefHeightProperty().bind(layout.heightProperty());
            final boolean removed = layout.getChildren().removeIf(n -> n == gridView.advancedGrid());
            if (removed)
                layout.getChildren().add(grid);
            resize(300, 250);
        });
        final MenuItem adv = new MenuItem("Advanced");
        adv.setOnAction(e -> {
            final GridPane grid = gridView.advancedGrid();
            grid.prefHeightProperty().bind(layout.heightProperty());
            final boolean removed = layout.getChildren().removeIf(n -> n == gridView.standardGrid());
            if (removed)
                layout.getChildren().add(grid);
            resize(600, 500);
        });
        final MenuButton menu = new MenuButton("Calculator", null, std, adv);
        return new ToolBar(menu);
    }

    private void resize(final int xDim, final int yDim) {
        window.setHeight(xDim);
        window.setWidth(yDim);
    }

}
