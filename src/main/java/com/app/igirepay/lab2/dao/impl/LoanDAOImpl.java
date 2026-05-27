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

import com.app.igirepay.lab1.model.Loan;
import com.app.igirepay.lab2.dao.LoanDAO;

public class LoanDAOImpl implements LoanDAO {

    private static final String SAVE_SQL = "INSERT INTO loans (customer_id, amount, interest_rate, approved, repayment_status) VALUES (?, ?, ?, ?, ?)";
    private static final String FIND_BY_ID_SQL = "SELECT id, customer_id, amount, interest_rate, approved, repayment_status, created_at FROM loans WHERE id = ?";
    private static final String FIND_ALL_SQL = "SELECT id, customer_id, amount, interest_rate, approved, repayment_status, created_at FROM loans ORDER BY id";
    private static final String FIND_BY_CUSTOMER_SQL = "SELECT id, customer_id, amount, interest_rate, approved, repayment_status, created_at FROM loans WHERE customer_id = ? ORDER BY id";

    @Override
    public void save(Loan loan) {
        if (loan == null) {
            throw new IllegalArgumentException("Loan must not be null");
        }

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, resolveCustomerDatabaseId(loan));
            statement.setBigDecimal(2, loan.getAmount());
            statement.setBigDecimal(3, loan.getInterestRate());
            statement.setBoolean(4, loan.isApproved());
            statement.setString(5, loan.getRepaymentStatus());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int databaseId = keys.getInt(1);
                    loan.setDatabaseId(databaseId);
                    if (loan.getLoanId() == null || loan.getLoanId().isBlank()) {
                        loan.setLoanId(String.valueOf(databaseId));
                    }
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save loan", exception);
        }
    }

    @Override
    public Loan findById(Integer id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapLoan(resultSet);
                }
                return null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find loan by database ID", exception);
        }
    }

    @Override
    public List<Loan> findAll() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            return mapLoanList(resultSet);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load loans", exception);
        }
    }

    @Override
    public void update(Loan loan) {
        throw new UnsupportedOperationException("update not implemented yet");
    }

    @Override
    public void delete(Integer id) {
        throw new UnsupportedOperationException("delete not implemented yet");
    }

    @Override
    public List<Loan> findByCustomerDatabaseId(Integer customerDatabaseId) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_CUSTOMER_SQL)) {

            statement.setInt(1, customerDatabaseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapLoanList(resultSet);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find loans by customer database ID", exception);
        }
    }

    private int resolveCustomerDatabaseId(Loan loan) {
        if (loan.getCustomerDatabaseId() != null) {
            return loan.getCustomerDatabaseId();
        }

        String customerId = loan.getCustomerId();
        try {
            return Integer.parseInt(customerId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Loan must have a numeric customer database ID", exception);
        }
    }

    private List<Loan> mapLoanList(ResultSet resultSet) throws SQLException {
        List<Loan> loans = new ArrayList<>();
        while (resultSet.next()) {
            loans.add(mapLoan(resultSet));
        }
        return loans;
    }

    private Loan mapLoan(ResultSet resultSet) throws SQLException {
        int databaseId = resultSet.getInt("id");
        int customerDatabaseId = resultSet.getInt("customer_id");
        BigDecimal amount = resultSet.getBigDecimal("amount");
        BigDecimal interestRate = resultSet.getBigDecimal("interest_rate");
        boolean approved = resultSet.getBoolean("approved");
        String repaymentStatus = resultSet.getString("repayment_status");
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        LocalDateTime timestamp = createdAt != null ? createdAt.toLocalDateTime() : null;

        Loan loan = new Loan(
                String.valueOf(databaseId),
                String.valueOf(customerDatabaseId),
                amount,
                interestRate,
                approved,
                repaymentStatus,
                timestamp
        );
        loan.setDatabaseId(databaseId);
        loan.setCustomerDatabaseId(customerDatabaseId);
        return loan;
    }
}