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

		String transactionType = transaction.getTransactionType();
 		BigDecimal amount = transaction.getAmount();

		if (isDepositType(transactionType)) {
			deposit(amount);
			return true;
		}

		if (isTransferInType(transactionType)) {
			deposit(amount);
			return true;
		}

		if (isWithdrawalType(transactionType) || isTransferOutType(transactionType)) {
			return withdraw(amount);
		}

		return false;
	}

	private boolean isTransferInType(String transactionType) {
		return transactionType != null && transactionType.equalsIgnoreCase("TRANSFER_IN");
	}

	private boolean isTransferOutType(String transactionType) {
		return transactionType != null && transactionType.equalsIgnoreCase("TRANSFER_OUT");
	}
}
