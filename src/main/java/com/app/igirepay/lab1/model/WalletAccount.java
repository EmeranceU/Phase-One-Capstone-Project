package com.app.igirepay.lab1.model;

import java.math.BigDecimal;

public class WalletAccount extends Account {

	public WalletAccount(String accountId, String customerId, BigDecimal balance, String pin) {
		super(accountId, customerId, balance, pin);
	}

	@Override
	public boolean withdraw(BigDecimal amount) {
		requirePositiveAmount(amount);
		if (!hasEnoughBalance(amount)) {
			return false;
		}

		decreaseBalance(amount);
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
}
