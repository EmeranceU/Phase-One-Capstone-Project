package com.app.igirepay.lab2.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.app.igirepay.lab1.model.Loan;
import com.app.igirepay.lab2.dao.LoanDAO;

public class LoanDAOImpl implements LoanDAO {

    private static final String SAVE_SQL = "INSERT INTO loans (customer_id, amount, interest_rate, approved, repayment_status) VALUES (?, ?, ?, ?, ?)";

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
        throw new UnsupportedOperationException("findById not implemented yet");
    }

    @Override
    public List<Loan> findAll() {
        throw new UnsupportedOperationException("findAll not implemented yet");
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
        throw new UnsupportedOperationException("findByCustomerDatabaseId not implemented yet");
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
}