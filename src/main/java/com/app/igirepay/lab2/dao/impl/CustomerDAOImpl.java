package com.app.igirepay.lab2.dao.impl;

import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab2.dao.CustomerDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class CustomerDAOImpl implements CustomerDAO {

    private static final String SAVE_SQL = "INSERT INTO customers (full_name, phone_number, pin) VALUES (?, ?, ?)";

    @Override
    public void save(Customer customer) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SAVE_SQL, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getPhoneNumber());
            statement.setString(3, customer.getPin());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setCustomerId(String.valueOf(keys.getInt(1)));
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save customer", exception);
        }
    }

    @Override
    public Customer findByPhone(String phoneNumber) {
        throw new UnsupportedOperationException("findByPhone not implemented yet");
    }

    @Override
    public Customer findById(Integer id) {
        throw new UnsupportedOperationException("findById not implemented yet");
    }

    @Override
    public List<Customer> findAll() {
        throw new UnsupportedOperationException("findAll not implemented yet");
    }

    @Override
    public void update(Customer customer) {
        throw new UnsupportedOperationException("update not implemented yet");
    }

    @Override
    public void delete(Integer id) {
        throw new UnsupportedOperationException("delete not implemented yet");
    }
}
