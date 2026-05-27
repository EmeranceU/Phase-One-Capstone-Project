package com.app.igirepay.lab2.dao.impl;

import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.SavingsAccount;
import com.app.igirepay.lab1.model.WalletAccount;
import com.app.igirepay.lab2.dao.AccountDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class AccountDAOImpl implements AccountDAO {

    private static final String SAVE_SQL = "INSERT INTO accounts (customer_id, account_type, balance) VALUES (?, ?, ?)";
    private static final String FIND_BY_ID_SQL = "SELECT id, customer_id, account_type, balance FROM accounts WHERE id = ?";
    private static final String FIND_ALL_SQL = "SELECT id, customer_id, account_type, balance FROM accounts ORDER BY id";
    private static final String FIND_BY_CUSTOMER_SQL = "SELECT id, customer_id, account_type, balance FROM accounts WHERE customer_id = ? ORDER BY id";
    private static final String FIND_BY_BUSINESS_ID_SQL = "SELECT id, customer_id, account_type, balance FROM accounts WHERE CAST(id AS TEXT) = ?";
    private static final String FIND_BY_TYPE_SQL = "SELECT id, customer_id, account_type, balance FROM accounts WHERE account_type = ? ORDER BY id";

    @Override
    public void save(Account account) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, resolveCustomerDatabaseId(account));
            statement.setString(2, resolveAccountType(account));
            statement.setBigDecimal(3, account.getBalance());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int databaseId = keys.getInt(1);
                    account.setDatabaseId(databaseId);
                    if (account.getAccountId() == null || account.getAccountId().isBlank()) {
                        account.setAccountId(String.valueOf(databaseId));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save account", exception);
        }
    }

    @Override
    public Account findById(Integer id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAccount(resultSet);
                }
                return null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find account by database ID", exception);
        }
    }

    @Override
    public List<Account> findAll() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            return mapAccountList(resultSet);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load accounts", exception);
        }
    }

    @Override
    public void update(Account account) {
        throw new UnsupportedOperationException("update not implemented yet");
    }

    @Override
    public void delete(Integer id) {
        throw new UnsupportedOperationException("delete not implemented yet");
    }

    @Override
    public List<Account> findByCustomerDatabaseId(Integer customerDatabaseId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_CUSTOMER_SQL)) {

            statement.setInt(1, customerDatabaseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAccountList(resultSet);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find accounts by customer database ID", exception);
        }
    }

    @Override
    public Account findByBusinessAccountId(String accountId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_BUSINESS_ID_SQL)) {

            statement.setString(1, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapAccount(resultSet);
                }
                return null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find account by business account ID", exception);
        }
    }

    @Override
    public List<Account> findWalletAccounts() {
        return findByAccountType("WALLET");
    }

    @Override
    public List<Account> findSavingsAccounts() {
        return findByAccountType("SAVINGS");
    }

    private int resolveCustomerDatabaseId(Account account) {
        if (account.getCustomerDatabaseId() != null) {
            return account.getCustomerDatabaseId();
        }

        String customerId = account.getCustomerId();
        try {
            return Integer.parseInt(customerId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Account must have a numeric customer database ID", exception);
        }
    }

    private String resolveAccountType(Account account) {
        if (account instanceof SavingsAccount) {
            return "SAVINGS";
        }
        return "WALLET";
    }

    private List<Account> findByAccountType(String accountType) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_TYPE_SQL)) {

            statement.setString(1, accountType);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAccountList(resultSet);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find accounts by account type", exception);
        }
    }

    private List<Account> mapAccountList(ResultSet resultSet) throws SQLException {
        List<Account> accounts = new java.util.ArrayList<>();
        while (resultSet.next()) {
            accounts.add(mapAccount(resultSet));
        }
        return accounts;
    }

    private Account mapAccount(ResultSet resultSet) throws SQLException {
        int databaseId = resultSet.getInt("id");
        int customerDatabaseId = resultSet.getInt("customer_id");

        WalletAccount account = new WalletAccount(
                String.valueOf(databaseId),
                String.valueOf(customerDatabaseId),
                resultSet.getBigDecimal("balance")
        );
        account.setDatabaseId(databaseId);
        account.setCustomerDatabaseId(customerDatabaseId);
        return account;
    }
}
