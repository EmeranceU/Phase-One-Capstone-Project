package com.app.igirepay.lab3.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
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
import com.app.igirepay.lab1.service.DailyTransactionSummary;
import com.app.igirepay.lab3.util.AppContext;
import com.app.igirepay.lab3.util.SceneNavigator;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Modality;
import javafx.stage.Window;

public class DashboardController {

    private final AppContext context = AppContext.getInstance();

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Label phoneLabel;

    @FXML
    private Label walletAccountIdLabel;

    @FXML
    private Label walletBalanceLabel;

    @FXML
    private Label savingsAccountIdLabel;

    @FXML
    private Label savingsBalanceLabel;

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
            welcomeLabel.setText("Welcome to IgirePay");
            phoneLabel.setText("Phone: -");
            setMessage("No active login session. Please login again.", true);
            renderAccountSummary(null);
            return;
        }

        welcomeLabel.setText("Welcome back, " + customer.getFullName());
        phoneLabel.setText("Phone: " + customer.getPhoneNumber());
        renderAccountSummary(customer);
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
        String result = context.getAccountService().createWalletAccountForCustomer(customer.getCustomerId(), account);
        context.reloadAllFromDatabase();
        renderAccountSummary(context.getCurrentCustomer());

        if (result != null && result.startsWith("Wallet account created")) {
            setMessage(result, false);
            showPopup(result, AlertType.INFORMATION);
            showAccounts();
            return;
        }

        setMessage(result == null ? "Failed to create wallet account." : result, true);
        showPopup(result == null ? "Failed to create wallet account." : result, AlertType.ERROR);
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
        String result = context.getAccountService().createSavingsAccountForCustomer(customer.getCustomerId(), account);
        context.reloadAllFromDatabase();
        renderAccountSummary(context.getCurrentCustomer());

        if (result != null && result.startsWith("Savings account created")) {
            setMessage(result, false);
            showPopup(result, AlertType.INFORMATION);
            showAccounts();
            return;
        }

        setMessage(result == null ? "Failed to create savings account." : result, true);
        showPopup(result == null ? "Failed to create savings account." : result, AlertType.ERROR);
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

        Account destination = resolveTransferDestination(recipientPhone);
        if (destination == null) {
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
            showPopup("Transfer completed.", AlertType.INFORMATION);
            renderAccountSummary(context.getCurrentCustomer());
            showAccounts();
        } catch (DuplicateTransactionException | InvalidAmountException | InsufficientBalanceException exception) {
            setMessage(exception.getMessage(), true);
            showPopup(exception.getMessage(), AlertType.ERROR);
        }
    }

    @FXML
    private void handleCheckBalance() {
        renderAccountSummary(context.getCurrentCustomer());
        showAccounts();
        setMessage("Balance loaded.", false);
        showPopup("Balance loaded.", AlertType.INFORMATION);
    }

    @FXML
    private void handleDeleteInactiveAccount() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        String result = context.getAccountService().deleteInactiveAccount(customer.getCustomerId(), accountIdField.getText());
        context.reloadAllFromDatabase();
        renderAccountSummary(context.getCurrentCustomer());

        if ("Account deleted successfully.".equals(result)) {
            setMessage(result, false);
            showPopup(result, AlertType.INFORMATION);
            showAccounts();
            return;
        }

        setMessage(result, true);
        showPopup(result, AlertType.ERROR);
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
            setMessage("No transaction history available.", false);
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
            showPopup("Loan approved.", AlertType.INFORMATION);
        } else {
            setMessage(loan.getRepaymentStatus(), true);
            showPopup(loan.getRepaymentStatus(), AlertType.ERROR);
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
            setMessage("No loan history available.", false);
            return;
        }

        String text = loans.stream().map(Loan::toString).collect(Collectors.joining("\n\n"));
        outputArea.setText(text);
    }

    @FXML
    private void handleExportTransactions() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        try {
            Path exportPath = context.getTransactionService().exportTransactionHistoryToCsv(customer);
            String message = "Transactions exported successfully.";
            setMessage(message, false);
            showPopup(message, AlertType.INFORMATION);
            outputArea.setText("Exported to: " + exportPath.toAbsolutePath());
        } catch (IllegalStateException exception) {
            setMessage(exception.getMessage(), true);
            showPopup(exception.getMessage(), AlertType.ERROR);
        } catch (RuntimeException exception) {
            setMessage("Failed to export transactions.", true);
            showPopup("Failed to export transactions.", AlertType.ERROR);
        }
    }

    @FXML
    private void handleDailySummary() {
        Customer customer = requireCurrentCustomer();
        if (customer == null) {
            return;
        }

        DailyTransactionSummary summary = context.getTransactionService().getDailyTransactionSummary(customer, context.getAccountService());
        String message = summary.toDisplayString();
        setMessage("Daily summary loaded.", false);
        showPopup(message, AlertType.INFORMATION);
        outputArea.setText(message);
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
            showPopup("PIN changed successfully.", AlertType.INFORMATION);
            currentPinField.clear();
            newPinField.clear();
        } catch (InvalidPinException exception) {
            setMessage(exception.getMessage(), true);
            showPopup(exception.getMessage(), AlertType.ERROR);
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
            showPopup(successMessage, AlertType.INFORMATION);
            renderAccountSummary(context.getCurrentCustomer());
            showAccounts();
        } catch (DuplicateTransactionException | InvalidAmountException | InsufficientBalanceException exception) {
            setMessage(exception.getMessage(), true);
            showPopup(exception.getMessage(), AlertType.ERROR);
        }
    }

    private Customer requireCurrentCustomer() {
        Customer customer = context.getCurrentCustomer();
        if (customer == null) {
            setMessage("Your session expired. Login again.", true);
            showPopup("Your session expired. Login again.", AlertType.ERROR);
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
            setMessage("No accounts yet. Create wallet or savings account.", false);
            return;
        }

        String text = accounts.stream()
            .map(this::formatAccountSummary)
            .collect(Collectors.joining("\n"));
        outputArea.setText(text);
    }

    private void renderAccountSummary(Customer customer) {
        if (customer == null) {
            walletAccountIdLabel.setText("No wallet account");
            walletBalanceLabel.setText("Balance: -");
            savingsAccountIdLabel.setText("No savings account");
            savingsBalanceLabel.setText("Balance: -");
            return;
        }

        Account walletAccount = context.getAccountService().getWalletAccountForCustomer(customer.getCustomerId());
        Account savingsAccount = context.getAccountService().getSavingsAccountForCustomer(customer.getCustomerId());

        walletAccountIdLabel.setText(walletAccount == null ? "No wallet account" : walletAccount.getAccountId());
        walletBalanceLabel.setText(walletAccount == null ? "Balance: -" : "Balance: " + formatAmount(walletAccount.getBalance()));

        savingsAccountIdLabel.setText(savingsAccount == null ? "No savings account" : savingsAccount.getAccountId());
        savingsBalanceLabel.setText(savingsAccount == null ? "Balance: -" : "Balance: " + formatAmount(savingsAccount.getBalance()));
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

    private Account resolveTransferDestination(String rawDestination) {
        String destinationValue = safeText(rawDestination);
        if (destinationValue.isEmpty()) {
            setMessage("Destination account ID or phone is required.", true);
            return null;
        }

        Account destinationAccount = context.getAccountService().findAccountById(destinationValue);
        if (destinationAccount != null) {
            return destinationAccount;
        }

        Customer recipient = context.findCustomerByPhone(destinationValue);
        if (recipient == null) {
            setMessage("Destination account not found.", true);
            return null;
        }

        Account walletAccount = context.getAccountService().getWalletAccountForCustomer(recipient.getCustomerId());
        if (walletAccount == null) {
            setMessage("Recipient has no wallet account.", true);
            return null;
        }

        return walletAccount;
    }

    private String formatAccountSummary(Account account) {
        return account.getClass().getSimpleName() + " | " + account.getAccountId() + " | Balance: " + formatAmount(account.getBalance());
    }

    private String formatAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return NumberFormat.getNumberInstance(Locale.US).format(value.stripTrailingZeros()) + " RWF";
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void setMessage(String message, boolean error) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        messageLabel.setManaged(true);
        messageLabel.getStyleClass().removeAll("status-success", "status-error", "status-info");
        messageLabel.getStyleClass().add(error ? "status-error" : "status-info");
    }

    private void showPopup(String message, AlertType alertType) {
        if (message == null || message.isBlank()) {
            return;
        }

        Alert alert = new Alert(alertType);
        alert.setTitle("IgirePay");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Window owner = messageLabel.getScene() == null ? null : messageLabel.getScene().getWindow();
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        alert.showAndWait();
    }
}
