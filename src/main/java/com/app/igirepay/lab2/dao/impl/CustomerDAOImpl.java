package com.app.igirepay.lab2.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab2.dao.CustomerDAO;

public class CustomerDAOImpl implements CustomerDAO {

    private static final String SAVE_SQL = "INSERT INTO customers (full_name, phone_number, pin) VALUES (?, ?, ?)";
    private static final String FIND_BY_PHONE_SQL = "SELECT id, full_name, phone_number, pin FROM customers WHERE phone_number = ?";
    private static final String FIND_BY_ID_SQL = "SELECT id, full_name, phone_number, pin FROM customers WHERE id = ?";
    private static final String FIND_ALL_SQL = "SELECT id, full_name, phone_number, pin FROM customers ORDER BY id";
    private static final String UPDATE_SQL = "UPDATE customers SET full_name = ?, phone_number = ?, pin = ? WHERE id = ?";
    private static final String DELETE_SQL = "DELETE FROM customers WHERE id = ?";

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
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_PHONE_SQL)) {

            statement.setString(1, phoneNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomer(resultSet);
                }
                return null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find customer by phone", exception);
        }
    }

    @Override
    public Customer findById(Integer id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapCustomer(resultSet);
                }
                return null;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to find customer by id", exception);
        }
    }

    @Override
    public List<Customer> findAll() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
             ResultSet resultSet = statement.executeQuery()) {

            List<Customer> customers = new java.util.ArrayList<>();
            while (resultSet.next()) {
                customers.add(mapCustomer(resultSet));
            }
            return customers;
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to get all customers", exception);
        }
    }

    @Override
    public void update(Customer customer) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {

            statement.setString(1, customer.getFullName());
            statement.setString(2, customer.getPhoneNumber());
            statement.setString(3, customer.getPin());
            statement.setInt(4, parseCustomerId(customer.getCustomerId()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to update customer", exception);
        }
    }

    @Override
    public void delete(Integer id) {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {

            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to delete customer", exception);
        }
    }

    private Customer mapCustomer(ResultSet resultSet) throws SQLException {
        String customerId = String.valueOf(resultSet.getInt("id"));
        String fullName = resultSet.getString("full_name");
        String phoneNumber = resultSet.getString("phone_number");
        String pin = resultSet.getString("pin");
        return new Customer(customerId, fullName, null, phoneNumber, pin);
    }

    private int parseCustomerId(String customerId) {
        try {
            return Integer.parseInt(customerId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Customer ID must be numeric for JDBC update", exception);
        }
    }
}
