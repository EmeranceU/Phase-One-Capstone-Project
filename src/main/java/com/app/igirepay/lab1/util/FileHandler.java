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