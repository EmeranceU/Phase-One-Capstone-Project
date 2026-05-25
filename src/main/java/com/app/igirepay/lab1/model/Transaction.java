package com.app.igirepay.lab1.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaction {

	private String transactionId;
	private String referenceId;
	private BigDecimal amount;
	private String transactionType;
	private LocalDateTime timestamp;

	public Transaction(String transactionId, String referenceId, BigDecimal amount, String transactionType, LocalDateTime timestamp) {
		this.transactionId = transactionId;
		this.referenceId = referenceId;
		this.amount = amount;
		this.transactionType = transactionType;
		this.timestamp = timestamp;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "Transaction{" +
				"transactionId='" + transactionId + '\'' +
				", referenceId='" + referenceId + '\'' +
				", amount=" + amount +
				", transactionType='" + transactionType + '\'' +
				", timestamp=" + timestamp +
				'}';
	}
}
