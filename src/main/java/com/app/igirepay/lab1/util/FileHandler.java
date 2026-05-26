package com.app.igirepay.lab1.util;

import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.Loan;
import com.app.igirepay.lab1.model.Transaction;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

public class FileHandler {

    private static final Path CUSTOMERS_FILE = Paths.get("customers.txt");
    private static final Path ACCOUNTS_FILE = Paths.get("accounts.txt");
    private static final Path TRANSACTION_HISTORY_FILE = Paths.get("transaction_history.txt");
    private static final Path FAILED_TRANSACTIONS_FILE = Paths.get("failed_transactions.txt");
    private static final Path LOANS_FILE = Paths.get("loans.txt");
    private static final Path FAILED_LOANS_FILE = Paths.get("failed_loans.txt");

    public void saveCustomer(Customer customer) {
        if (customer == null) {
            return;
        }

        writeLine(CUSTOMERS_FILE, formatCustomer(customer));
    }

    public java.util.List<com.app.igirepay.lab1.model.Customer> loadCustomersFromFile() {
        Path path = CUSTOMERS_FILE;
        java.util.List<com.app.igirepay.lab1.model.Customer> result = new java.util.ArrayList<>();
        if (!Files.exists(path)) {
            return result;
        }

        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;
                String id = parts[0].trim();
                String name = parts[1].trim();
                String email = parts[2].trim();
                String phone = parts[3].trim();
                String pin = parts[4].trim();
                result.add(new com.app.igirepay.lab1.model.Customer(id, name, email, phone, pin));
            }
        } catch (IOException ex) {
            System.err.println("Unable to read customers file: " + ex.getMessage());
        }

        return result;
    }

    public java.util.List<com.app.igirepay.lab1.model.Account> loadAccountsFromFile() {
        Path path = ACCOUNTS_FILE;
        java.util.List<com.app.igirepay.lab1.model.Account> result = new java.util.ArrayList<>();
        if (!Files.exists(path)) {
            return result;
        }

        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) continue;
                String accountId = parts[0].trim();
                String customerId = parts[1].trim();
                String type = parts[2].trim();
                String balanceText = parts[3].trim();
                java.math.BigDecimal balance = java.math.BigDecimal.ZERO;
                try { balance = new java.math.BigDecimal(balanceText); } catch (Exception ignored) {}

                com.app.igirepay.lab1.model.Account account;
                if ("WalletAccount".equalsIgnoreCase(type)) {
                    account = new com.app.igirepay.lab1.model.WalletAccount(accountId, customerId, balance);
                } else {
                    account = new com.app.igirepay.lab1.model.SavingsAccount(accountId, customerId, balance);
                }

                result.add(account);
            }
        } catch (IOException ex) {
            System.err.println("Unable to read accounts file: " + ex.getMessage());
        }

        return result;
    }

    public java.util.List<com.app.igirepay.lab1.model.Transaction> loadTransactionsFromFile() {
        Path path = TRANSACTION_HISTORY_FILE;
        java.util.List<com.app.igirepay.lab1.model.Transaction> result = new java.util.ArrayList<>();
        if (!Files.exists(path)) {
            return result;
        }

        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\|", -1);
                // expected: timestamp | transactionId | customerId | accountId | destinationAccountId | referenceId | transactionType | amount
                if (parts.length < 8) continue;
                java.time.LocalDateTime timestamp = java.time.LocalDateTime.parse(parts[0].trim());
                String txId = parts[1].trim();
                String customerId = parts[2].trim();
                String accountId = parts[3].trim();
                String destinationAccountId = parts[4].trim();
                String referenceId = parts[5].trim();
                String txType = parts[6].trim();
                java.math.BigDecimal amount = java.math.BigDecimal.ZERO;
                try { amount = new java.math.BigDecimal(parts[7].trim()); } catch (Exception ignored) {}

                result.add(new com.app.igirepay.lab1.model.Transaction(txId, customerId, accountId, destinationAccountId, referenceId, amount, txType, timestamp));
            }
        } catch (IOException ex) {
            System.err.println("Unable to read transaction history file: " + ex.getMessage());
        }

        return result;
    }

    public java.util.List<com.app.igirepay.lab1.model.Loan> loadLoansFromFile() {
        Path path = LOANS_FILE;
        java.util.List<com.app.igirepay.lab1.model.Loan> result = new java.util.ArrayList<>();
        if (!Files.exists(path)) {
            return result;
        }

        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\|", -1);
                // expected: timestamp | loanId | customerId | amount | interestRate | approved | repaymentStatus
                if (parts.length < 7) continue;
                java.time.LocalDateTime timestamp = java.time.LocalDateTime.parse(parts[0].trim());
                String loanId = parts[1].trim();
                String customerId = parts[2].trim();
                java.math.BigDecimal amount = java.math.BigDecimal.ZERO;
                try { amount = new java.math.BigDecimal(parts[3].trim()); } catch (Exception ignored) {}
                java.math.BigDecimal interest = java.math.BigDecimal.ZERO;
                try { interest = new java.math.BigDecimal(parts[4].trim()); } catch (Exception ignored) {}
                boolean approved = Boolean.parseBoolean(parts[5].trim());
                String repaymentStatus = parts[6].trim();

                result.add(new com.app.igirepay.lab1.model.Loan(loanId, customerId, amount, interest, approved, repaymentStatus, timestamp));
            }
        } catch (IOException ex) {
            System.err.println("Unable to read loans file: " + ex.getMessage());
        }

        return result;
    }

    public void loadAllData(com.app.igirepay.lab1.service.AccountService accountService,
                            com.app.igirepay.lab1.service.TransactionService transactionService,
                            com.app.igirepay.lab1.service.LoanService loanService) {
        if (accountService == null || transactionService == null) return;

        // Load customers
        for (com.app.igirepay.lab1.model.Customer c : loadCustomersFromFile()) {
            accountService.addCustomerFromFile(c);
        }

        // Load accounts and attach to customers
        for (com.app.igirepay.lab1.model.Account a : loadAccountsFromFile()) {
            accountService.addAccountFromFile(a);
        }

        // Load transactions
        for (com.app.igirepay.lab1.model.Transaction t : loadTransactionsFromFile()) {
            transactionService.loadTransaction(t);
        }

        // Load loans if provided
        if (loanService != null) {
            for (com.app.igirepay.lab1.model.Loan l : loadLoansFromFile()) {
                loanService.loadLoan(l);
            }
        }
    }

    public void saveAccount(Account account) {
        if (account == null) {
            return;
        }

        writeLine(ACCOUNTS_FILE, formatAccount(account));
    }

    public void saveTransactionHistory(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        writeLine(TRANSACTION_HISTORY_FILE, formatTransaction(transaction));
    }

    public void saveFailedTransactionLogs(String logEntry) {
        if (logEntry == null || logEntry.trim().isEmpty()) {
            return;
        }

        writeLine(FAILED_TRANSACTIONS_FILE, logEntry);
    }

    public void saveLoan(Loan loan) {
        if (loan == null) {
            return;
        }

        writeLine(LOANS_FILE, formatLoan(loan));
    }

    public void saveFailedLoan(String logEntry) {
        if (logEntry == null || logEntry.trim().isEmpty()) {
            return;
        }

        writeLine(FAILED_LOANS_FILE, logEntry);
    }

    private void writeLine(Path filePath, String line) {
        try {
            Files.write(filePath, Collections.singletonList(line), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            System.err.println("Unable to write to " + filePath.getFileName() + ": " + exception.getMessage());
        }
    }

    private String formatCustomer(Customer customer) {
        return customer.getCustomerId() + " | "
                + customer.getFullName() + " | "
                + customer.getEmail() + " | "
                + customer.getPhoneNumber() + " | "
                + customer.getPin();
    }

    private String formatAccount(Account account) {
        return account.getAccountId() + " | "
                + account.getCustomerId() + " | "
                + account.getClass().getSimpleName() + " | "
                + account.getBalance();
    }

    private String formatTransaction(Transaction transaction) {
        BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();
        return transaction.getTimestamp() + " | "
                + transaction.getTransactionId() + " | "
                + transaction.getCustomerId() + " | "
                + transaction.getAccountId() + " | "
                + transaction.getDestinationAccountId() + " | "
                + transaction.getReferenceId() + " | "
                + transaction.getTransactionType() + " | "
                + amount;
    }

    private String formatLoan(Loan loan) {
        return loan.getTimestamp() + " | "
                + loan.getLoanId() + " | "
                + loan.getCustomerId() + " | "
                + loan.getAmount() + " | "
                + loan.getInterestRate() + " | "
                + loan.isApproved() + " | "
                + loan.getRepaymentStatus();
    }
}