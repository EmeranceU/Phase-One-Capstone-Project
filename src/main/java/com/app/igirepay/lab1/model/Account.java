package com.app.igirepay.lab1.model;

import java.math.BigDecimal;
import java.util.Objects;


public abstract class Account {

	private Integer databaseId;
	private Integer customerDatabaseId;
	private String accountId;
	private String customerId;
	private BigDecimal balance;

	protected Account(String accountId, String customerId, BigDecimal balance) {
		this.accountId = accountId;
		this.customerId = customerId;
		this.balance = balance == null ? BigDecimal.ZERO : balance;
	}

	public String getAccountId() {
		return accountId;
	}

	public Integer getDatabaseId() {
		return databaseId;
	}

	public void setDatabaseId(Integer databaseId) {
		this.databaseId = databaseId;
	}

	public Integer getCustomerDatabaseId() {
		return customerDatabaseId;
	}

	public void setCustomerDatabaseId(Integer customerDatabaseId) {
		this.customerDatabaseId = customerDatabaseId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance == null ? BigDecimal.ZERO : balance;
	}

	public BigDecimal deposit(BigDecimal amount) {
		requirePositiveAmount(amount);
		balance = balance.add(amount);
		return balance;
	}

	public abstract boolean withdraw(BigDecimal amount);

	public abstract boolean processTransaction(Transaction transaction);

	protected boolean isDepositType(String transactionType) {
		return transactionType != null && transactionType.equalsIgnoreCase("DEPOSIT");
	}

	protected boolean isWithdrawalType(String transactionType) {
		if (transactionType == null) {
			return false;
		}
		return transactionType.equalsIgnoreCase("WITHDRAWAL")
				|| transactionType.equalsIgnoreCase("TRANSFER")
				|| transactionType.equalsIgnoreCase("TRANSFER_OUT");
	}

	protected void requirePositiveAmount(BigDecimal amount) {
		Objects.requireNonNull(amount, "amount must not be null");
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("amount must be greater than zero");
		}
	}

	protected boolean hasEnoughBalance(BigDecimal amount) {
		return balance.compareTo(amount) >= 0;
	}

	protected void decreaseBalance(BigDecimal amount) {
		balance = balance.subtract(amount);
	}

	@Override
	public String toString() {
		return "Account{" +
				"databaseId=" + databaseId +
				", customerDatabaseId=" + customerDatabaseId +
				", accountId='" + accountId + '\'' +
				", customerId='" + customerId + '\'' +
				", balance=" + balance +
				'}';
	}
}
