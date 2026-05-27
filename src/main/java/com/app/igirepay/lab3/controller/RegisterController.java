package com.app.igirepay.lab3.controller;

import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab3.util.AppContext;
import com.app.igirepay.lab3.util.SceneNavigator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    private final AppContext context = AppContext.getInstance();

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private PasswordField pinField;

    @FXML
    private Label messageLabel;

    @FXML
    private void handleRegister(ActionEvent event) {
        String fullName = text(fullNameField);
        String email = text(emailField);
        String phone = text(phoneField);
        String pin = text(pinField);

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || pin.isEmpty()) {
            setMessage("All fields are required.", true);
            return;
        }

        Customer customer = new Customer(context.nextCustomerBusinessId(), fullName, email, phone, pin);
        boolean created = context.getAccountService().addCustomer(customer);
        context.reloadAllFromDatabase();

        if (created) {
            setMessage("Registration successful. You can login now.", false);
            clearForm();
            return;
        }

        setMessage("Registration failed. Customer ID may already exist.", true);
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        SceneNavigator.switchScene(event, "/com/app/igirepay/lab3/view/login.fxml", "IgirePay Login");
    }

    private String text(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void clearForm() {
        fullNameField.clear();
        emailField.clear();
        phoneField.clear();
        pinField.clear();
    }

    private void setMessage(String message, boolean error) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add(error ? "status-error" : "status-success");
    }
}
