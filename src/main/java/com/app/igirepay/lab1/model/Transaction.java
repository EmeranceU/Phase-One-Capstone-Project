package com.app.igirepay.lab1.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Transaction {

	private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private Integer databaseId;
	private Integer customerDatabaseId;
	private Integer accountDatabaseId;
	private Integer destinationAccountDatabaseId;
	private String transactionId;
	private String customerId;
	private String accountId;
	private String destinationAccountId;
	private String referenceId;
	private BigDecimal amount;
	private String transactionType;
	private LocalDateTime timestamp;

	public Transaction(String transactionId, String customerId, String accountId, String destinationAccountId, String referenceId, BigDecimal amount, String transactionType, LocalDateTime timestamp) {
		this.transactionId = transactionId;
		this.customerId = customerId;
		this.accountId = accountId;
		this.destinationAccountId = destinationAccountId;
		this.referenceId = referenceId;
		this.amount = amount;
		this.transactionType = transactionType;
		this.timestamp = timestamp;
	}

	public String getTransactionId() {
		return transactionId;
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

	public Integer getAccountDatabaseId() {
		return accountDatabaseId;
	}

	public void setAccountDatabaseId(Integer accountDatabaseId) {
		this.accountDatabaseId = accountDatabaseId;
	}

	public Integer getDestinationAccountDatabaseId() {
		return destinationAccountDatabaseId;
	}

	public void setDestinationAccountDatabaseId(Integer destinationAccountDatabaseId) {
		this.destinationAccountDatabaseId = destinationAccountDatabaseId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getDestinationAccountId() {
		return destinationAccountId;
	}

	public void setDestinationAccountId(String destinationAccountId) {
		this.destinationAccountId = destinationAccountId;
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
		StringBuilder builder = new StringBuilder();
		builder.append(formatTransactionType(transactionType))
				.append(" • ")
				.append(formatAmount(amount))
				.append(" RWF\n");

		if (isTransferType(transactionType) && destinationAccountId != null && !destinationAccountId.isBlank()) {
			builder.append("To Account: ").append(destinationAccountId).append('\n');
		}

		builder.append("Reference ID: ")
				.append(referenceId == null || referenceId.isBlank() ? "-" : referenceId)
				.append('\n')
				.append("Date: ")
				.append(formatTimestamp(timestamp));
		return builder.toString();
	}

	private String formatTransactionType(String value) {
		if (value == null || value.isBlank()) {
			return "Transaction";
		}

		String normalized = value.trim().toUpperCase(Locale.US);
		if ("DEPOSIT".equals(normalized)) {
			return "Deposit";
		}

		if ("WITHDRAWAL".equals(normalized)) {
			return "Withdrawal";
		}

		if ("TRANSFER".equals(normalized) || "TRANSFER_IN".equals(normalized) || "TRANSFER_OUT".equals(normalized)) {
			return "Transfer";
		}

		return normalized.substring(0, 1) + normalized.substring(1).toLowerCase(Locale.US);
	}

	private boolean isTransferType(String value) {
		if (value == null) {
			return false;
		}

		String normalized = value.trim().toUpperCase(Locale.US);
		return "TRANSFER".equals(normalized) || "TRANSFER_IN".equals(normalized) || "TRANSFER_OUT".equals(normalized);
	}

	private String formatAmount(BigDecimal value) {
		BigDecimal amountValue = value == null ? BigDecimal.ZERO : value;
		return NumberFormat.getNumberInstance(Locale.US).format(amountValue.stripTrailingZeros());
	}

	private String formatTimestamp(LocalDateTime value) {
		return value == null ? "-" : value.format(DISPLAY_TIMESTAMP);
	}
}
