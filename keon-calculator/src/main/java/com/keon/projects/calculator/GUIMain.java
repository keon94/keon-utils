package com.keon.projects.calculator;

import com.keon.projects.calculator.gui.MainView;

import javafx.application.Application;

public class GUIMain implements IMain {

    @Override
    public void main(String... args) {
        Application.launch(MainView.class, args);
        new MainView();
    }

}
