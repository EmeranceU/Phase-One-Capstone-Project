package com.app.igirepay.lab1.model;

import java.util.ArrayList;
import java.util.List;

public class Customer {

	private String customerId;
	private String fullName;
	private String email;
	private String phoneNumber;
	private final List<Account> accounts;

	public Customer(String customerId, String fullName, String email, String phoneNumber) {
		this.customerId = customerId;
		this.fullName = fullName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.accounts = new ArrayList<>();
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public List<Account> getAccounts() {
		return List.copyOf(accounts);
	}

	public boolean addAccount(Account account) {
		if (account == null) {
			return false;
		}

		boolean alreadyExists = accounts.stream()
				.anyMatch(existingAccount -> existingAccount.getAccountId().equals(account.getAccountId()));
		if (alreadyExists) {
			return false;
		}

		accounts.add(account);
		return true;
	}

	public boolean removeAccount(String accountId) {
		return accounts.removeIf(account -> account.getAccountId().equals(accountId));
	}

	public Account findAccountById(String accountId) {
		return accounts.stream()
				.filter(account -> account.getAccountId().equals(accountId))
				.findFirst()
				.orElse(null);
	}

	@Override
	public String toString() {
		return "Customer{" +
				"customerId='" + customerId + '\'' +
				", fullName='" + fullName + '\'' +
				", email='" + email + '\'' +
				", phoneNumber='" + phoneNumber + '\'' +
				", accounts=" + accounts.size() +
				'}';
	}
}
