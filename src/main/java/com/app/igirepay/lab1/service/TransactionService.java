package com.app.igirepay.lab1.service;

import com.app.igirepay.lab1.exception.DuplicateTransactionException;
import com.app.igirepay.lab1.exception.InsufficientBalanceException;
import com.app.igirepay.lab1.exception.InvalidAmountException;
import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.Transaction;
import com.app.igirepay.lab1.util.FileHandler;
import com.app.igirepay.lab2.dao.AccountDAO;
import com.app.igirepay.lab2.dao.TransactionDAO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TransactionService {

    private final Set<String> processedReferenceIds = new HashSet<>();
    private final List<Transaction> transactionHistory = new ArrayList<>();
    private final List<String> failedTransactionLogs = new ArrayList<>();
    private final FileHandler fileHandler;
    private final TransactionDAO transactionDAO;
    private final AccountDAO accountDAO;

    public TransactionService() {
        this(new FileHandler(), null, null);
    }

    public TransactionService(FileHandler fileHandler) {
        this(fileHandler, null, null);
    }

    public TransactionService(FileHandler fileHandler, TransactionDAO transactionDAO) {
        this(fileHandler, transactionDAO, null);
    }

    public TransactionService(FileHandler fileHandler, TransactionDAO transactionDAO, AccountDAO accountDAO) {
        this.fileHandler = fileHandler == null ? new FileHandler() : fileHandler;
        this.transactionDAO = transactionDAO;
        this.accountDAO = accountDAO;
    }

    public Transaction processDeposit(Account account, Transaction transaction)
            throws DuplicateTransactionException, InvalidAmountException {
        try {
            validateAccountAndTransaction(account, transaction);
            validateReferenceId(transaction.getReferenceId());
            validateAmount(transaction.getAmount());

            account.deposit(transaction.getAmount());
            recordSuccess(transaction, account, null);
            return transaction;
        } catch (DuplicateTransactionException | InvalidAmountException exception) {
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

            recordSuccess(transaction, account, null);
            return transaction;
        } catch (DuplicateTransactionException | InvalidAmountException exception) {
            recordFailure(transaction, exception.getMessage());
            throw exception;
        } catch (IllegalArgumentException exception) {
            recordFailure(transaction, exception.getMessage());
            throw new InvalidAmountException(exception.getMessage());
        }
    }

    public Transaction processTransfer(AccountService accountService, Customer loggedInCustomer, Transaction transaction)
            throws DuplicateTransactionException, InvalidAmountException, InsufficientBalanceException {
        try {
            validateTransferContext(accountService, loggedInCustomer, transaction);
            validateReferenceId(transaction.getReferenceId());
            validateAmount(transaction.getAmount());

            Account sourceAccount = accountService.findAccountById(transaction.getAccountId());
            Account destinationAccount = accountService.findAccountById(transaction.getDestinationAccountId());

            validateTransferAccounts(loggedInCustomer, sourceAccount, destinationAccount);
            if (sourceAccount.getAccountId().equals(destinationAccount.getAccountId())) {
                throw new IllegalArgumentException("Source and destination accounts must be different.");
            }

            boolean withdrawn = sourceAccount.withdraw(transaction.getAmount());
            if (!withdrawn) {
                recordFailure(transaction, "Insufficient balance for transfer.");
                throw new InsufficientBalanceException("Insufficient balance for transfer.");
            }

            destinationAccount.deposit(transaction.getAmount());
            recordSuccess(transaction, sourceAccount, destinationAccount);
            return transaction;
        } catch (DuplicateTransactionException | InvalidAmountException exception) {
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

            recordSuccess(transaction, account, null);
            return transaction;
        } catch (DuplicateTransactionException | InvalidAmountException exception) {
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

    public List<Transaction> getTransactionHistoryForCustomer(String customerId) {
        if (customerId == null) {
            return List.of();
        }

        return transactionHistory.stream()
                .filter(transaction -> customerId.equals(transaction.getCustomerId()))
                .collect(Collectors.toList());
    }

    public List<Transaction> getTransactionHistoryForCustomerFromDB(Integer customerDatabaseId) {
        if (customerDatabaseId == null || transactionDAO == null) {
            return List.of();
        }
        
        try {
            return transactionDAO.findByCustomerDatabaseId(customerDatabaseId);
        } catch (Exception exception) {
            System.err.println("Warning: Failed to load transaction history from PostgreSQL: " + exception.getMessage());
            return List.of();
        }
    }

    public List<Transaction> getTransactionHistoryForAccountFromDB(Integer accountDatabaseId) {
        if (accountDatabaseId == null || transactionDAO == null) {
            return List.of();
        }
        
        try {
            List<Transaction> transactions = new ArrayList<>();
            // Get transactions where this account is the source
            transactions.addAll(transactionDAO.findBySourceAccountDatabaseId(accountDatabaseId));
            // Get transactions where this account is the destination
            transactions.addAll(transactionDAO.findByDestinationAccountDatabaseId(accountDatabaseId));
            return transactions;
        } catch (Exception exception) {
            System.err.println("Warning: Failed to load transaction history from PostgreSQL: " + exception.getMessage());
            return List.of();
        }
    }

    // Load a transaction from file into memory without writing back to disk
    public void loadTransaction(Transaction transaction) {
        if (transaction == null) return;
        if (transaction.getReferenceId() != null && !transaction.getReferenceId().isBlank()) {
            processedReferenceIds.add(transaction.getReferenceId());
        }
        transactionHistory.add(transaction);
    }

    private void validateAccountAndTransaction(Account account, Transaction transaction) {
        if (account == null) {
            throw new IllegalArgumentException("account must not be null");
        }

        if (transaction == null) {
            throw new IllegalArgumentException("transaction must not be null");
        }
    }

    private void validateTransferContext(AccountService accountService, Customer loggedInCustomer, Transaction transaction) {
        if (accountService == null) {
            throw new IllegalArgumentException("accountService must not be null");
        }

        if (loggedInCustomer == null) {
            throw new IllegalArgumentException("loggedInCustomer must not be null");
        }

        if (transaction == null) {
            throw new IllegalArgumentException("transaction must not be null");
        }

        if (transaction.getAccountId() == null || transaction.getAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("source account must not be blank");
        }

        if (transaction.getDestinationAccountId() == null || transaction.getDestinationAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("destination account must not be blank");
        }
    }

    private void validateTransferAccounts(Customer loggedInCustomer, Account sourceAccount, Account destinationAccount) {
        if (sourceAccount == null) {
            throw new IllegalArgumentException("source account must exist");
        }

        if (destinationAccount == null) {
            throw new IllegalArgumentException("destination account must exist");
        }

        if (loggedInCustomer.getCustomerId() == null || !loggedInCustomer.getCustomerId().equals(sourceAccount.getCustomerId())) {
            throw new IllegalArgumentException("Source account must belong to the logged-in customer.");
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

    private void recordSuccess(Transaction transaction, Account account, Account destinationAccount) {
        processedReferenceIds.add(transaction.getReferenceId());
        transactionHistory.add(transaction);
        fileHandler.saveTransactionHistory(transaction);
        
        // Persist transaction and account updates to PostgreSQL
        if (transactionDAO != null && account != null) {
            try {
                // Set account database ID on transaction for JDBC persistence
                if (account.getDatabaseId() != null) {
                    transaction.setAccountDatabaseId(account.getDatabaseId());
                }
                
                // Set destination account database ID for transfers
                if (destinationAccount != null && destinationAccount.getDatabaseId() != null) {
                    transaction.setDestinationAccountDatabaseId(destinationAccount.getDatabaseId());
                }
                
                // Persist transaction
                transactionDAO.save(transaction);
                
                // Persist account balance updates
                if (accountDAO != null) {
                    accountDAO.update(account);
                    if (destinationAccount != null) {
                        accountDAO.update(destinationAccount);
                    }
                }
            } catch (Exception exception) {
                System.err.println("Warning: Failed to persist transaction to PostgreSQL: " + exception.getMessage());
            }
        }
    }

    private void recordFailure(Transaction transaction, String message) {
        String referenceId = transaction == null ? "UNKNOWN" : transaction.getReferenceId();
        String logEntry = referenceId + " - " + message;
        failedTransactionLogs.add(logEntry);
        fileHandler.saveFailedTransactionLogs(logEntry);
    }
}