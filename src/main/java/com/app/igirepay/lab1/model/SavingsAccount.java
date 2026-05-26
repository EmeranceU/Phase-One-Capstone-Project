package com.app.igirepay.lab1.model;

import java.math.BigDecimal;

public class SavingsAccount extends Account {

	private static final BigDecimal WITHDRAWAL_FEE = new BigDecimal("2.00");
	private BigDecimal minimumBalance;

	public SavingsAccount(String accountId, String customerId, BigDecimal balance) {
		super(accountId, customerId, balance);
		this.minimumBalance = new BigDecimal("100.00");
	}

	public SavingsAccount(String accountId, String customerId, BigDecimal balance, BigDecimal minimumBalance) {
		super(accountId, customerId, balance);
		this.minimumBalance = minimumBalance == null ? new BigDecimal("100.00") : minimumBalance;
	}

	public BigDecimal getWithdrawalFee() {
		return WITHDRAWAL_FEE;
	}

	public BigDecimal getMinimumBalance() {
		return minimumBalance;
	}

	public void setMinimumBalance(BigDecimal minimumBalance) {
		this.minimumBalance = minimumBalance == null ? new BigDecimal("100.00") : minimumBalance;
	}

	@Override
	public boolean withdraw(BigDecimal amount) {
		requirePositiveAmount(amount);

		BigDecimal totalAmount = amount.add(WITHDRAWAL_FEE);
		BigDecimal remainingBalance = getBalance().subtract(totalAmount);
		if (remainingBalance.compareTo(minimumBalance) < 0) {
			return false;
		}

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

		String transactionType = transaction.getTransactionType();
		BigDecimal amount = transaction.getAmount();

		if (isDepositType(transactionType)) {
			deposit(amount);
			return true;
		}

		if (isWithdrawalType(transactionType)) {
			return withdraw(amount);
		}

		if (isTransferInType(transactionType)) {
			deposit(amount);
			return true;
		}

		if (isTransferOutType(transactionType)) {
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

	@Override
	public String toString() {
		return "SavingsAccount{" +
				"accountId='" + getAccountId() + '\'' +
				", customerId='" + getCustomerId() + '\'' +
				", balance=" + getBalance() +
				", withdrawalFee=" + WITHDRAWAL_FEE +
				", minimumBalance=" + minimumBalance +
				'}';
	}
}
