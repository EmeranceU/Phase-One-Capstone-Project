package com.app.igirepay.lab1.service;

import com.app.igirepay.lab1.exception.DuplicateTransactionException;
import com.app.igirepay.lab1.exception.InsufficientBalanceException;
import com.app.igirepay.lab1.exception.InvalidAmountException;
import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Transaction;
import com.app.igirepay.lab1.util.FileHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionService {

    private final Set<String> processedReferenceIds = new HashSet<>();
    private final List<Transaction> transactionHistory = new ArrayList<>();
    private final List<String> failedTransactionLogs = new ArrayList<>();
    private final FileHandler fileHandler;

    public TransactionService() {
        this(new FileHandler());
    }

    public TransactionService(FileHandler fileHandler) {
        this.fileHandler = fileHandler == null ? new FileHandler() : fileHandler;
    }

    public Transaction processDeposit(Account account, Transaction transaction)
            throws DuplicateTransactionException, InvalidAmountException {
        try {
            validateAccountAndTransaction(account, transaction);
            validateReferenceId(transaction.getReferenceId());
            validateAmount(transaction.getAmount());

            account.deposit(transaction.getAmount());
            recordSuccess(transaction);
            return transaction;
        } catch (DuplicateTransactionException exception) {
            recordFailure(transaction, exception.getMessage());
            throw exception;
        } catch (InvalidAmountException exception) {
            recordFailure(transaction, exception.getMessage());
            throw exception;
        } catch (IllegalArgumentException exception) {
            recordFailure(transaction, exception.getMessage());
            throw new InvalidAmountException(exception.getMessage());
        }
    }

    public Transaction processWithdrawal(Account account, Transaction transaction)
            throws DuplicateTransactionException, InvalidAmountException, InsufficientBalanceException {
        try {
            validateAccountAndTransaction(account, transaction);
            validateReferenceId(transaction.getReferenceId());
            validateAmount(transaction.getAmount());

            boolean processed = account.withdraw(transaction.getAmount());
            if (!processed) {
                recordFailure(transaction, "Insufficient balance for withdrawal.");
                throw new InsufficientBalanceException("Insufficient balance for withdrawal.");
            }

            recordSuccess(transaction);
            return transaction;
        } catch (DuplicateTransactionException exception) {
            recordFailure(transaction, exception.getMessage());
            throw exception;
        } catch (InvalidAmountException exception) {
            recordFailure(transaction, exception.getMessage());
            throw exception;
        } catch (IllegalArgumentException exception) {
            recordFailure(transaction, exception.getMessage());
            throw new InvalidAmountException(exception.getMessage());
        }
    }

    public Transaction processTransaction(Account account, Transaction transaction)
            throws DuplicateTransactionException, InvalidAmountException, InsufficientBalanceException {
        try {
            validateAccountAndTransaction(account, transaction);
            validateReferenceId(transaction.getReferenceId());
            validateAmount(transaction.getAmount());

            boolean processed = account.processTransaction(transaction);
            if (!processed) {
                if (isDebitTransaction(transaction.getTransactionType())) {
                    recordFailure(transaction, "Insufficient balance for transaction.");
                    throw new InsufficientBalanceException("Insufficient balance for transaction.");
                }

                recordFailure(transaction, "Transaction type is not supported for this account.");
                throw new InvalidAmountException("Transaction type is not supported for this account.");
            }

            recordSuccess(transaction);
            return transaction;
        } catch (DuplicateTransactionException exception) {
            recordFailure(transaction, exception.getMessage());
            throw exception;
        } catch (InvalidAmountException exception) {
            recordFailure(transaction, exception.getMessage());
            throw exception;
        } catch (IllegalArgumentException exception) {
            recordFailure(transaction, exception.getMessage());
            throw new InvalidAmountException(exception.getMessage());
        }
    }

    public Set<String> getProcessedReferenceIds() {
        return Collections.unmodifiableSet(processedReferenceIds);
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    public List<String> getFailedTransactionLogs() {
        return Collections.unmodifiableList(failedTransactionLogs);
    }

    private void validateAccountAndTransaction(Account account, Transaction transaction) {
        if (account == null) {
            throw new IllegalArgumentException("account must not be null");
        }

        if (transaction == null) {
            throw new IllegalArgumentException("transaction must not be null");
        }
    }

    private void validateReferenceId(String referenceId) throws DuplicateTransactionException {
        if (referenceId == null || referenceId.trim().isEmpty()) {
            throw new IllegalArgumentException("referenceId must not be blank");
        }

        if (processedReferenceIds.contains(referenceId)) {
            throw new DuplicateTransactionException("Duplicate transaction reference ID: " + referenceId);
        }
    }

    private void validateAmount(BigDecimal amount) throws InvalidAmountException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
    }

    private boolean isDebitTransaction(String transactionType) {
        if (transactionType == null) {
            return false;
        }

        return transactionType.equalsIgnoreCase("WITHDRAWAL")
                || transactionType.equalsIgnoreCase("TRANSFER")
                || transactionType.equalsIgnoreCase("TRANSFER_OUT");
    }

    private void recordSuccess(Transaction transaction) {
        processedReferenceIds.add(transaction.getReferenceId());
        transactionHistory.add(transaction);
        fileHandler.saveTransactionHistory(transaction);
    }

    private void recordFailure(Transaction transaction, String message) {
        String referenceId = transaction == null ? "UNKNOWN" : transaction.getReferenceId();
        String logEntry = referenceId + " - " + message;
        failedTransactionLogs.add(logEntry);
        fileHandler.saveFailedTransactionLogs(logEntry);
    }
}