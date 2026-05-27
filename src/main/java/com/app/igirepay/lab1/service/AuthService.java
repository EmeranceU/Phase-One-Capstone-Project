package com.app.igirepay.lab1.service;

import com.app.igirepay.lab1.exception.InvalidPinException;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.util.FileHandler;
import com.app.igirepay.lab2.dao.CustomerDAO;

import java.util.Objects;

public class AuthService {

	private final AccountService accountService;
	private final FileHandler fileHandler;
	private final CustomerDAO customerDAO;

	public AuthService(AccountService accountService) {
		this(accountService, new FileHandler(), null);
	}

	public AuthService(AccountService accountService, FileHandler fileHandler) {
		this(accountService, fileHandler, null);
	}

	public AuthService(AccountService accountService, FileHandler fileHandler, CustomerDAO customerDAO) {
		this.accountService = Objects.requireNonNull(accountService, "accountService must not be null");
		this.fileHandler = fileHandler == null ? new FileHandler() : fileHandler;
		this.customerDAO = customerDAO;
	}

	public Customer login(String phoneNumber, String pin) throws InvalidPinException {
		Customer customer = findCustomerByPhoneNumber(phoneNumber);
		if (customer == null && customerDAO != null) {
			try {
				customer = customerDAO.findByPhone(phoneNumber);
			} catch (Exception exception) {
				System.err.println("Warning: Failed to lookup customer in PostgreSQL: " + exception.getMessage());
			}
		}
		
		if (!isPinValid(customer, pin)) {
			throw new InvalidPinException("Invalid phone number or PIN.");
		}

		return customer;
	}

	public boolean changePin(String phoneNumber, String currentPin, String newPin) throws InvalidPinException {
		Customer customer = login(phoneNumber, currentPin);
		if (newPin == null || newPin.trim().isEmpty()) {
			throw new InvalidPinException("New PIN must not be blank.");
		}

		customer.setPin(newPin.trim());
		fileHandler.saveCustomer(customer);
		
		if (customerDAO != null) {
			try {
				customerDAO.update(customer);
			} catch (Exception exception) {
				System.err.println("Warning: Failed to update PIN in PostgreSQL: " + exception.getMessage());
			}
		}
		
		return true;
	}

	public boolean validateCustomerPin(Customer customer, String pin) {
		return isPinValid(customer, pin);
	}

	private Customer findCustomerByPhoneNumber(String phoneNumber) {
		if (phoneNumber == null) {
			return null;
		}

		return accountService.getCustomers().stream()
				.filter(customer -> phoneNumber.equals(customer.getPhoneNumber()))
				.findFirst()
				.orElse(null);
	}

	private boolean isPinValid(Customer customer, String pin) {
		return customer != null
				&& customer.getPin() != null
				&& pin != null
				&& customer.getPin().equals(pin.trim());
	}
}