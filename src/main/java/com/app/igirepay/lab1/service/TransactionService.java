package com.app.igirepay.lab1.service;

import java.math.BigDecimal;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import com.app.igirepay.lab1.exception.DuplicateTransactionException;
import com.app.igirepay.lab1.exception.InsufficientBalanceException;
import com.app.igirepay.lab1.exception.InvalidAmountException;
import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.Transaction;
import com.app.igirepay.lab1.model.WalletAccount;
import com.app.igirepay.lab2.dao.AccountDAO;
import com.app.igirepay.lab2.dao.TransactionDAO;

public class TransactionService {

    private final Set<String> processedReferenceIds = new HashSet<>();
    private final List<Transaction> transactionHistory = new ArrayList<>();
    private final List<String> failedTransactionLogs = new ArrayList<>();
    private final TransactionDAO transactionDAO;
    private final AccountDAO accountDAO;

    public TransactionService() {
        this(null, null);
    }

    public TransactionService(TransactionDAO transactionDAO) {
        this(transactionDAO, null);
    }

    public TransactionService(TransactionDAO transactionDAO, AccountDAO accountDAO) {
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
            transactions.addAll(transactionDAO.findBySourceAccountDatabaseId(accountDatabaseId));
            transactions.addAll(transactionDAO.findByDestinationAccountDatabaseId(accountDatabaseId));
            return transactions;
        } catch (Exception exception) {
            System.err.println("Warning: Failed to load transaction history from PostgreSQL: " + exception.getMessage());
            return List.of();
        }
    }

    public Path exportTransactionHistoryToCsv(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("customer must not be null");
        }

        List<Transaction> transactions = customer.getDatabaseId() != null
                ? getTransactionHistoryForCustomerFromDB(customer.getDatabaseId())
                : getTransactionHistoryForCustomer(customer.getCustomerId());

        if (transactions.isEmpty()) {
            throw new IllegalStateException("No transactions available to export.");
        }

        String customerSuffix = customer.getCustomerId() == null ? "customer" : customer.getCustomerId();
        String fileName = "transactions_" + customerSuffix + "_" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        Path exportPath = Paths.get(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(exportPath, StandardCharsets.UTF_8)) {
            writer.write("transaction_id,reference_id,transaction_type,amount,timestamp,source_account,destination_account");
            writer.newLine();

            for (Transaction transaction : transactions) {
                writer.write(csvValue(transaction.getTransactionId()));
                writer.write(',');
                writer.write(csvValue(transaction.getReferenceId()));
                writer.write(',');
                writer.write(csvValue(transaction.getTransactionType()));
                writer.write(',');
                writer.write(transaction.getAmount() == null ? "" : transaction.getAmount().toPlainString());
                writer.write(',');
                writer.write(csvValue(transaction.getTimestamp() == null ? null : transaction.getTimestamp().toString()));
                writer.write(',');
                writer.write(csvValue(transaction.getAccountId()));
                writer.write(',');
                writer.write(csvValue(transaction.getDestinationAccountId()));
                writer.newLine();
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to export transaction history", exception);
        }

        return exportPath;
    }

    public DailyTransactionSummary getDailyTransactionSummary(Customer customer, AccountService accountService) {
        if (customer == null) {
            throw new IllegalArgumentException("customer must not be null");
        }

        List<Transaction> transactions = customer.getDatabaseId() != null
                ? getTransactionHistoryForCustomerFromDB(customer.getDatabaseId())
                : getTransactionHistoryForCustomer(customer.getCustomerId());

        List<Account> customerAccounts = accountService == null || customer.getCustomerId() == null
                ? List.of()
                : accountService.getAccountsForCustomer(customer.getCustomerId());
        Set<Integer> customerAccountIds = customerAccounts.stream()
                .map(Account::getDatabaseId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        int deposits = 0;
        int withdrawals = 0;
        int transfers = 0;
        BigDecimal moneyIn = BigDecimal.ZERO;
        BigDecimal moneyOut = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (transaction == null || transaction.getTimestamp() == null || !today.equals(transaction.getTimestamp().toLocalDate())) {
                continue;
            }

            String transactionType = transaction.getTransactionType() == null ? "" : transaction.getTransactionType().trim().toUpperCase();
            BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();

            if ("DEPOSIT".equals(transactionType)) {
                deposits++;
                moneyIn = moneyIn.add(amount);
                continue;
            }

            if ("WITHDRAWAL".equals(transactionType)) {
                withdrawals++;
                moneyOut = moneyOut.add(amount);
                continue;
            }

            if ("TRANSFER".equals(transactionType)) {
                transfers++;

                if (customerAccountIds.contains(transaction.getDestinationAccountDatabaseId())) {
                    moneyIn = moneyIn.add(amount);
                }

                if (customerAccountIds.contains(transaction.getAccountDatabaseId())) {
                    moneyOut = moneyOut.add(amount);
                }
            }
        }

        return new DailyTransactionSummary(deposits, withdrawals, transfers, moneyIn, moneyOut);
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        return '"' + escaped + '"';
    }

    public void loadFromDatabase() {
        if (transactionDAO == null) {
            return;
        }

        transactionHistory.clear();
        processedReferenceIds.clear();
        for (Transaction transaction : transactionDAO.findAll()) {
            loadTransaction(transaction);
        }
    }

    public void loadTransaction(Transaction transaction) {
        if (transaction == null) {
            return;
        }

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

        if (!(sourceAccount instanceof WalletAccount)) {
            throw new IllegalArgumentException("Only wallet accounts can transfer money.");
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

        if (transactionDAO != null && account != null) {
            try {
                if (account.getCustomerDatabaseId() != null) {
                    transaction.setCustomerDatabaseId(account.getCustomerDatabaseId());
                }

                if (account.getDatabaseId() != null) {
                    transaction.setAccountDatabaseId(account.getDatabaseId());
                }

                if (destinationAccount != null && destinationAccount.getDatabaseId() != null) {
                    transaction.setDestinationAccountDatabaseId(destinationAccount.getDatabaseId());
                }

                transactionDAO.save(transaction);

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
    }
}
