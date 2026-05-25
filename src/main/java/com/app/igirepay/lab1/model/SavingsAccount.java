package com.app.igirepay.lab1.model;

import java.math.BigDecimal;

public class SavingsAccount extends Account {

	private static final BigDecimal WITHDRAWAL_FEE = new BigDecimal("2.00");

	public SavingsAccount(String accountId, String customerId, BigDecimal balance, String pin) {
		super(accountId, customerId, balance, pin);
	}

	public BigDecimal getWithdrawalFee() {
		return WITHDRAWAL_FEE;
	}

	@Override
	public boolean withdraw(BigDecimal amount) {
		requirePositiveAmount(amount);

		BigDecimal totalAmount = amount.add(WITHDRAWAL_FEE);
		if (!hasEnoughBalance(totalAmount)) {
			return false;
		}

		decreaseBalance(totalAmount);
		return true;
	}

	@Override
	public boolean processTransaction(Transaction transaction) {
		if (transaction == null) {
			return false;
		}

		if (isDepositType(transaction.getTransactionType())) {
			deposit(transaction.getAmount());
			return true;
		}

		if (isWithdrawalType(transaction.getTransactionType())) {
			return withdraw(transaction.getAmount());
		}

		return false;
	}

	@Override
	public String toString() {
		return "SavingsAccount{" +
				"accountId='" + getAccountId() + '\'' +
				", customerId='" + getCustomerId() + '\'' +
				", balance=" + getBalance() +
				", withdrawalFee=" + WITHDRAWAL_FEE +
				'}';
	}
}
