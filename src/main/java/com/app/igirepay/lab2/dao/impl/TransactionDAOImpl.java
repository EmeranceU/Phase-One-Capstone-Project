package com.app.igirepay.lab2.dao.impl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.app.igirepay.lab1.model.Transaction;
import com.app.igirepay.lab2.dao.TransactionDAO;

public class TransactionDAOImpl implements TransactionDAO {

    private static final String SAVE_SQL = "INSERT INTO transactions (transaction_id, reference_id, source_account_id, destination_account_id, transaction_type, amount, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String FIND_BY_ID_SQL = "SELECT id, transaction_id, reference_id, source_account_id, destination_account_id, transaction_type, amount, created_at FROM transactions WHERE id = ?";
    private static final String FIND_ALL_SQL = "SELECT id, transaction_id, reference_id, source_account_id, destination_account_id, transaction_type, amount, created_at FROM transactions ORDER BY id";
    private static final String FIND_BY_REFERENCE_SQL = "SELECT id, transaction_id, reference_id, source_account_id, destination_account_id, transaction_type, amount, created_at FROM transactions WHERE reference_id = ?";
    private static final String FIND_BY_SOURCE_SQL = "SELECT id, transaction_id, reference_id, source_account_id, destination_account_id, transaction_type, amount, created_at FROM transactions WHERE source_account_id = ? ORDER BY id";
    private static final String FIND_BY_DESTINATION_SQL = "SELECT id, transaction_id, reference_id, source_account_id, destination_account_id, transaction_type, amount, created_at FROM transactions WHERE destination_account_id = ? ORDER BY id";
    private static final String FIND_BY_CUSTOMER_SQL = "SELECT DISTINCT t.id, t.transaction_id, t.reference_id, t.source_account_id, t.destination_account_id, t.transaction_type, t.amount, t.created_at FROM transactions t LEFT JOIN accounts source_account ON t.source_account_id = source_account.id LEFT JOIN accounts destination_account ON t.destination_account_id = destination_account.id WHERE source_account.customer_id = ? OR destination_account.customer_id = ? ORDER BY t.id";

    @Override
    public void save(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction must not be null");
        }

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, resolveTransactionId(transaction));
            statement.setString(2, transaction.getReferenceId());
            setNullableInt(statement, 3, resolveSourceAccountDatabaseId(transaction));
            setNullableInt(statement, 4, resolveDestinationAccountDatabaseId(transaction));
            statement.setString(5, transaction.getTransactionType());
            statement.setBigDecimal(6, transaction.getAmount());
            statement.setTimestamp(7, Timestamp.valueOf(resolveTimestamp(transaction)));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int databaseId = keys.getInt(1);
                    transaction.setDatabaseId(databaseId);
                    if (transaction.getTransactionId() == null || transaction.getTransactionId().isBlank()) {
                        transaction.setTransactionId(String.valueOf(databaseId));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save transaction", exception);
        }
    }

    @Override
    public Transaction findById(Integer id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTransaction(resultSet);
                }
                return null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find transaction by database ID", exception);
        }
    }

    @Override
    public List<Transaction> findAll() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            return mapTransactionList(resultSet);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load transactions", exception);
        }
    }

    @Override
    public void update(Transaction transaction) {
        throw new UnsupportedOperationException("update not implemented yet");
    }

    @Override
    public void delete(Integer id) {
        throw new UnsupportedOperationException("delete not implemented yet");
    }

    @Override
    public Transaction findByReferenceId(String referenceId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_REFERENCE_SQL)) {

            statement.setString(1, referenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapTransaction(resultSet);
                }
                return null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find transaction by reference ID", exception);
        }
    }

    @Override
    public List<Transaction> findBySourceAccountDatabaseId(Integer accountDatabaseId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_SOURCE_SQL)) {

            statement.setInt(1, accountDatabaseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactionList(resultSet);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find transactions by source account database ID", exception);
        }
    }

    @Override
    public List<Transaction> findByDestinationAccountDatabaseId(Integer accountDatabaseId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_DESTINATION_SQL)) {

            statement.setInt(1, accountDatabaseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactionList(resultSet);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find transactions by destination account database ID", exception);
        }
    }

    @Override
    public List<Transaction> findByCustomerDatabaseId(Integer customerDatabaseId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_CUSTOMER_SQL)) {

            statement.setInt(1, customerDatabaseId);
            statement.setInt(2, customerDatabaseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapTransactionList(resultSet, customerDatabaseId);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find transactions by customer database ID", exception);
        }
    }

    private String resolveTransactionId(Transaction transaction) {
        String transactionId = transaction.getTransactionId();
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction must have a transaction ID");
        }
        return transactionId;
    }

    private Integer resolveSourceAccountDatabaseId(Transaction transaction) {
        if (transaction.getTransactionType() != null && transaction.getTransactionType().equalsIgnoreCase("DEPOSIT")) {
            return null;
        }
        return transaction.getAccountDatabaseId();
    }

    private Integer resolveDestinationAccountDatabaseId(Transaction transaction) {
        if (transaction.getTransactionType() != null && transaction.getTransactionType().equalsIgnoreCase("WITHDRAWAL")) {
            return null;
        }

        if (transaction.getTransactionType() != null && transaction.getTransactionType().equalsIgnoreCase("DEPOSIT")) {
            return transaction.getAccountDatabaseId();
        }

        return transaction.getDestinationAccountDatabaseId();
    }

    private LocalDateTime resolveTimestamp(Transaction transaction) {
        if (transaction.getTimestamp() != null) {
            return transaction.getTimestamp();
        }
        return LocalDateTime.now();
    }

    private void setNullableInt(PreparedStatement statement, int parameterIndex, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, java.sql.Types.BIGINT);
        } else {
            statement.setInt(parameterIndex, value);
        }
    }

    private List<Transaction> mapTransactionList(ResultSet resultSet) throws SQLException {
        return mapTransactionList(resultSet, null);
    }

    private List<Transaction> mapTransactionList(ResultSet resultSet, Integer customerDatabaseId) throws SQLException {
        List<Transaction> transactions = new ArrayList<>();
        while (resultSet.next()) {
            transactions.add(mapTransaction(resultSet, customerDatabaseId));
        }
        return transactions;
    }

    private Transaction mapTransaction(ResultSet resultSet) throws SQLException {
        return mapTransaction(resultSet, null);
    }

    private Transaction mapTransaction(ResultSet resultSet, Integer customerDatabaseId) throws SQLException {
        int databaseId = resultSet.getInt("id");
        String transactionId = resultSet.getString("transaction_id");
        String referenceId = resultSet.getString("reference_id");
        Integer sourceAccountDatabaseId = getNullableInteger(resultSet, "source_account_id");
        Integer destinationAccountDatabaseId = getNullableInteger(resultSet, "destination_account_id");
        String transactionType = resultSet.getString("transaction_type");
        BigDecimal amount = resultSet.getBigDecimal("amount");
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        LocalDateTime timestamp = createdAt == null ? null : createdAt.toLocalDateTime();

        Transaction transaction = new Transaction(
                transactionId,
            customerDatabaseId == null ? null : String.valueOf(customerDatabaseId),
                sourceAccountDatabaseId == null ? null : String.valueOf(sourceAccountDatabaseId),
                destinationAccountDatabaseId == null ? null : String.valueOf(destinationAccountDatabaseId),
                referenceId,
                amount,
                transactionType,
                timestamp
        );
        transaction.setDatabaseId(databaseId);
        transaction.setCustomerDatabaseId(customerDatabaseId);
        transaction.setAccountDatabaseId(sourceAccountDatabaseId);
        transaction.setDestinationAccountDatabaseId(destinationAccountDatabaseId);
        return transaction;
    }

    private Integer getNullableInteger(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }
}