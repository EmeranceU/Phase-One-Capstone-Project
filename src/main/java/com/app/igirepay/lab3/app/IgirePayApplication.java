package com.app.igirepay.lab3.app;

import com.app.igirepay.lab3.util.AppContext;
import com.app.igirepay.lab3.util.SceneNavigator;

import javafx.application.Application;
import javafx.stage.Stage;

public class IgirePayApplication extends Application {

    @Override
    public void start(Stage stage) {
        AppContext.getInstance().reloadAllFromDatabase();
        SceneNavigator.switchScene(stage, "/com/app/igirepay/lab3/view/login.fxml", "IgirePay");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
