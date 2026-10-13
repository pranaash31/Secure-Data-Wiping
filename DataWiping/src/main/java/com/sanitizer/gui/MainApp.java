package com.sanitizer.gui;

import com.sanitizer.gui.navigation.NavigationManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        NavigationManager.getInstance().init(stage);
    }

    @Override
    public void stop() {
        NavigationManager.getInstance().shutdownApplication();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
