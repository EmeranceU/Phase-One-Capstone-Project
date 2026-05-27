package com.app.igirepay.lab3.controller;

import com.app.igirepay.lab1.exception.InvalidPinException;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab3.util.AppContext;
import com.app.igirepay.lab3.util.SceneNavigator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    private final AppContext context = AppContext.getInstance();

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField pinField;

    @FXML
    private Label messageLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String pin = pinField.getText() == null ? "" : pinField.getText().trim();

        if (phone.isEmpty() || pin.isEmpty()) {
            setMessage("Phone and PIN are required.", true);
            return;
        }

        try {
            Customer customer = context.getAuthService().login(phone, pin);
            context.setCurrentCustomer(customer);
            context.reloadAllFromDatabase();
            SceneNavigator.switchScene(event, "/com/app/igirepay/lab3/view/dashboard.fxml", "IgirePay Dashboard");
        } catch (InvalidPinException exception) {
            setMessage(exception.getMessage(), true);
        }
    }

    @FXML
    private void handleOpenRegister(ActionEvent event) {
        SceneNavigator.switchScene(event, "/com/app/igirepay/lab3/view/register.fxml", "IgirePay Register");
    }

    private void setMessage(String message, boolean error) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add(error ? "status-error" : "status-success");
    }
}
