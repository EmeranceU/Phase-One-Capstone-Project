package com.app.igirepay.lab2.dao.impl;

import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.SavingsAccount;
import com.app.igirepay.lab2.dao.AccountDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class AccountDAOImpl implements AccountDAO {

    private static final String SAVE_SQL = "INSERT INTO accounts (customer_id, account_type, balance) VALUES (?, ?, ?)";

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
        throw new UnsupportedOperationException("findById not implemented yet");
    }

    @Override
    public List<Account> findAll() {
        throw new UnsupportedOperationException("findAll not implemented yet");
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
        throw new UnsupportedOperationException("findByCustomerDatabaseId not implemented yet");
    }

    @Override
    public Account findByBusinessAccountId(String accountId) {
        throw new UnsupportedOperationException("findByBusinessAccountId not implemented yet");
    }

    @Override
    public List<Account> findWalletAccounts() {
        throw new UnsupportedOperationException("findWalletAccounts not implemented yet");
    }

    @Override
    public List<Account> findSavingsAccounts() {
        throw new UnsupportedOperationException("findSavingsAccounts not implemented yet");
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
}
