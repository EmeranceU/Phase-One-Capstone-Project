package com.app.igirepay.lab3.util;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class SceneNavigator {

    private static final String THEME = "/com/app/igirepay/lab3/css/igirepay.css";

    private SceneNavigator() {
    }

    public static void switchScene(Stage stage, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(SceneNavigator.class.getResource(THEME).toExternalForm());

            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load screen: " + fxmlPath, exception);
        }
    }

    public static void switchScene(ActionEvent event, String fxmlPath, String title) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        switchScene(stage, fxmlPath, title);
    }
}
