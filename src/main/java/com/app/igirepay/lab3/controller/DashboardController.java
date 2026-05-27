package com.app.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.app.igirepay.lab1.exception.DuplicateTransactionException;
import com.app.igirepay.lab1.exception.InsufficientBalanceException;
import com.app.igirepay.lab1.exception.InvalidAmountException;
import com.app.igirepay.lab1.exception.InvalidPinException;
import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.Loan;
import com.app.igirepay.lab1.model.SavingsAccount;
import com.app.igirepay.lab1.model.Transaction;
import com.app.igirepay.lab1.model.WalletAccount;
import com.app.igirepay.lab3.util.AppContext;
import com.app.igirepay.lab3.util.SceneNavigator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class DashboardController {

    private final AppContext context = AppContext.getInstance();

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private TextField initialBalanceField;

    @FXML
    private TextField accountIdField;

    @FXML
    private TextField amountField;

    @FXML
    private TextField recipientPhoneField;

    @FXML
    private TextField referenceField;

    @FXML
    private TextField loanAmountField;

    @FXML
    private PasswordField currentPinField;

    @FXML
    private PasswordField newPinField;

    @FXML
    private TextArea outputArea;

    @FXML
    public void initialize() {
        Customer customer = context.getCurrentCustomer();
        if (customer == null) {
            welcomeLabel.setText("Welcome");
            setMessage("No active login session. Please login again.", true);
            return;
        }

        welcomeLabel.setText("Welcome, " + customer.getFullName());
        showAccounts();
    }

    @FXML
    private void handleCreateWalletAccount() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        BigDecimal initialBalance = parseAmount(initialBalanceField.getText(), "Initial balance");
        if (initialBalance == null) {
            return;
        }

        WalletAccount account = new WalletAccount(context.nextWalletAccountId(), customer.getCustomerId(), initialBalance);
        boolean created = context.getAccountService().addAccountToCustomer(customer.getCustomerId(), account);
        context.reloadAllFromDatabase();

        if (created) {
            setMessage("Wallet account created: " + account.getAccountId(), false);
            showAccounts();
            return;
        }

        setMessage("Failed to create wallet account.", true);
    }

    @FXML
    private void handleCreateSavingsAccount() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        BigDecimal initialBalance = parseAmount(initialBalanceField.getText(), "Initial balance");
        if (initialBalance == null) {
            return;
        }

        SavingsAccount account = new SavingsAccount(context.nextSavingsAccountId(), customer.getCustomerId(), initialBalance);
        boolean created = context.getAccountService().addAccountToCustomer(customer.getCustomerId(), account);
        context.reloadAllFromDatabase();

        if (created) {
            setMessage("Savings account created: " + account.getAccountId(), false);
            showAccounts();
            return;
        }

        setMessage("Failed to create savings account.", true);
    }

    @FXML
    private void handleDepositMoney() {
        processSimpleTransaction("DEPOSIT", "Deposit completed.");
    }

    @FXML
    private void handleWithdrawMoney() {
        processSimpleTransaction("WITHDRAWAL", "Withdrawal completed.");
    }

    @FXML
    private void handleTransferMoney() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        Account sourceAccount = findOwnedAccount(accountIdField.getText());
        if (sourceAccount == null) {
            return;
        }

        BigDecimal amount = parseAmount(amountField.getText(), "Amount");
        String recipientPhone = safeText(recipientPhoneField.getText());
        String reference = safeText(referenceField.getText());

        if (amount == null || recipientPhone.isEmpty() || reference.isEmpty()) {
            setMessage("Amount, recipient phone, and reference are required.", true);
            return;
        }

        Customer recipient = context.findCustomerByPhone(recipientPhone);
        if (recipient == null) {
            setMessage("Recipient not found.", true);
            return;
        }

        Account destination = context.findWalletAccountForCustomer(recipient);
        if (destination == null) {
            setMessage("Recipient has no wallet account.", true);
            return;
        }

        Transaction transaction = new Transaction(
                context.nextTransactionBusinessId(),
                customer.getCustomerId(),
                sourceAccount.getAccountId(),
                destination.getAccountId(),
                reference,
                amount,
                "TRANSFER",
                LocalDateTime.now()
        );

        try {
            context.getTransactionService().processTransfer(context.getAccountService(), customer, transaction);
            context.reloadAllFromDatabase();
            setMessage("Transfer completed.", false);
            showAccounts();
        } catch (DuplicateTransactionException | InvalidAmountException | InsufficientBalanceException exception) {
            setMessage(exception.getMessage(), true);
        }
    }

    @FXML
    private void handleCheckBalance() {
        showAccounts();
        setMessage("Balance loaded.", false);
    }

    @FXML
    private void handleTransactionHistory() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        List<Transaction> transactions = customer.getDatabaseId() != null
                ? context.getTransactionService().getTransactionHistoryForCustomerFromDB(customer.getDatabaseId())
                : context.getTransactionService().getTransactionHistoryForCustomer(customer.getCustomerId());

        if (transactions.isEmpty()) {
            outputArea.setText("No transaction history available.");
            return;
        }

        String text = transactions.stream().map(Transaction::toString).collect(Collectors.joining("\n\n"));
        outputArea.setText(text);
    }

    @FXML
    private void handleRequestLoan() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        BigDecimal amount = parseAmount(loanAmountField.getText(), "Loan amount");
        if (amount == null) {
            return;
        }

        Loan loan = context.getLoanService().requestLoan(customer, amount);
        context.reloadAllFromDatabase();

        if (loan.isApproved()) {
            setMessage("Loan approved.", false);
        } else {
            setMessage(loan.getRepaymentStatus(), true);
        }

        outputArea.setText(loan.toString());
    }

    @FXML
    private void handleViewLoanHistory() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        List<Loan> loans = customer.getDatabaseId() != null
                ? context.getLoanService().getLoanHistoryForCustomerDatabaseId(customer.getDatabaseId())
                : context.getLoanService().getLoanHistoryForCustomer(customer.getCustomerId());

        if (loans.isEmpty()) {
            outputArea.setText("No loan history available.");
            return;
        }

        String text = loans.stream().map(Loan::toString).collect(Collectors.joining("\n\n"));
        outputArea.setText(text);
    }

    @FXML
    private void handleChangePin() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        String currentPin = safeText(currentPinField.getText());
        String newPin = safeText(newPinField.getText());
        if (currentPin.isEmpty() || newPin.isEmpty()) {
            setMessage("Current PIN and new PIN are required.", true);
            return;
        }

        try {
            context.getAuthService().changePin(customer, currentPin, newPin);
            context.reloadAllFromDatabase();
            setMessage("PIN changed successfully.", false);
            currentPinField.clear();
            newPinField.clear();
        } catch (InvalidPinException exception) {
            setMessage(exception.getMessage(), true);
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        context.setCurrentCustomer(null);
        SceneNavigator.switchScene(event, "/com/app/igirepay/lab3/view/login.fxml", "IgirePay Login");
    }

    private void processSimpleTransaction(String type, String successMessage) {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        Account account = findOwnedAccount(accountIdField.getText());
        if (account == null) {
            return;
        }

        BigDecimal amount = parseAmount(amountField.getText(), "Amount");
        String reference = safeText(referenceField.getText());
        if (amount == null || reference.isEmpty()) {
            setMessage("Amount and reference are required.", true);
            return;
        }

        Transaction transaction = new Transaction(
                context.nextTransactionBusinessId(),
                customer.getCustomerId(),
                account.getAccountId(),
                null,
                reference,
                amount,
                type,
                LocalDateTime.now()
        );

        try {
            context.getTransactionService().processTransaction(account, transaction);
            context.reloadAllFromDatabase();
            setMessage(successMessage, false);
            showAccounts();
        } catch (DuplicateTransactionException | InvalidAmountException | InsufficientBalanceException exception) {
            setMessage(exception.getMessage(), true);
        }
    }

    private Customer requireCurrentCustomer() {
        Customer customer = context.getCurrentCustomer();
        if (customer == null) {
            setMessage("Your session expired. Login again.", true);
        }
        return customer;
    }

    private Account findOwnedAccount(String accountIdValue) {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return null;
        }

        String accountId = safeText(accountIdValue);
        if (accountId.isEmpty()) {
            setMessage("Account ID is required.", true);
            return null;
        }

        List<Account> ownedAccounts = context.getAccountService().getAccountsForCustomer(customer.getCustomerId());
        return ownedAccounts.stream()
                .filter(account -> account.getAccountId().equals(accountId))
                .findFirst()
                .orElseGet(() -> {
                    setMessage("Account not found for your profile.", true);
                    return null;
                });
    }

    private void showAccounts() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        List<Account> accounts = context.getAccountService().getAccountsForCustomer(customer.getCustomerId());
        if (accounts.isEmpty()) {
            outputArea.setText("No accounts yet. Create wallet or savings account.");
            return;
        }

        String text = accounts.stream()
                .map(account -> account.getClass().getSimpleName() + " | " + account.getAccountId() + " | Balance: " + account.getBalance())
                .collect(Collectors.joining("\n"));
        outputArea.setText(text);
    }

    private BigDecimal parseAmount(String raw, String label) {
        String text = safeText(raw);
        if (text.isEmpty()) {
            setMessage(label + " is required.", true);
            return null;
        }

        try {
            BigDecimal amount = new BigDecimal(text);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                setMessage(label + " must be greater than zero.", true);
                return null;
            }
            return amount;
        } catch (NumberFormatException exception) {
            setMessage(label + " must be a valid number.", true);
            return null;
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void setMessage(String message, boolean error) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("status-success", "status-error");
        messageLabel.getStyleClass().add(error ? "status-error" : "status-success");
    }
}
