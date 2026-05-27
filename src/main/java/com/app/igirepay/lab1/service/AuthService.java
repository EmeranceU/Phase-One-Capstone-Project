package com.app.igirepay.lab1.service;

import java.util.Objects;

import com.app.igirepay.lab1.exception.InvalidPinException;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab2.dao.CustomerDAO;

public class AuthService {

    private final CustomerDAO customerDAO;

    public AuthService(CustomerDAO customerDAO) {
        this.customerDAO = Objects.requireNonNull(customerDAO, "customerDAO must not be null");
    }

    public Customer login(String phoneNumber, String pin) throws InvalidPinException {
        if (phoneNumber == null || pin == null) {
            throw new InvalidPinException("Invalid phone number or PIN.");
        }

        try {
            Customer customer = customerDAO.findByPhone(phoneNumber);
            if (!isPinValid(customer, pin)) {
                throw new InvalidPinException("Invalid phone number or PIN.");
            }
            return customer;
        } catch (InvalidPinException exception) {
            throw exception;
        } catch (Exception exception) {
            System.err.println("Warning: Failed to lookup customer in PostgreSQL: " + exception.getMessage());
            throw new InvalidPinException("Invalid phone number or PIN.");
        }
    }

    public boolean changePin(String phoneNumber, String currentPin, String newPin) throws InvalidPinException {
        return changePin(login(phoneNumber, currentPin), currentPin, newPin);
    }

    public boolean changePin(Customer customer, String currentPin, String newPin) throws InvalidPinException {
        if (!isPinValid(customer, currentPin)) {
            throw new InvalidPinException("Invalid current PIN.");
        }

        if (newPin == null || newPin.trim().isEmpty()) {
            throw new InvalidPinException("New PIN must not be blank.");
        }

        String normalizedPin = newPin.trim();
        customer.setPin(normalizedPin);

        try {
            Customer persistedCustomer = customerDAO.findByPhone(customer.getPhoneNumber());
            if (persistedCustomer == null) {
                persistedCustomer = customer;
            }
            persistedCustomer.setPin(normalizedPin);
            customerDAO.update(persistedCustomer);
        } catch (Exception exception) {
            System.err.println("Warning: Failed to update PIN in PostgreSQL: " + exception.getMessage());
        }

        return true;
    }

    public boolean validateCustomerPin(Customer customer, String pin) {
        return isPinValid(customer, pin);
    }

    private boolean isPinValid(Customer customer, String pin) {
        return customer != null
                && customer.getPin() != null
                && pin != null
                && customer.getPin().equals(pin.trim());
    }
}
