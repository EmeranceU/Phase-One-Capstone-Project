package com.app.igirepay.lab1.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Loan {

	private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private Integer databaseId;
	private Integer customerDatabaseId;
	private String loanId;
	private String customerId;
	private BigDecimal amount;
	private BigDecimal interestRate;
	private boolean approved;
	private String repaymentStatus;
	private LocalDateTime timestamp;

	public Loan(String loanId, String customerId, BigDecimal amount, BigDecimal interestRate, boolean approved, String repaymentStatus, LocalDateTime timestamp) {
		this.loanId = loanId;
		this.customerId = customerId;
		this.amount = amount;
		this.interestRate = interestRate;
		this.approved = approved;
		this.repaymentStatus = repaymentStatus;
		this.timestamp = timestamp;
	}

	public String getLoanId() {
		return loanId;
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

	public void setLoanId(String loanId) {
		this.loanId = loanId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(BigDecimal interestRate) {
		this.interestRate = interestRate;
	}

	public boolean isApproved() {
		return approved;
	}

	public void setApproved(boolean approved) {
		this.approved = approved;
	}

	public String getRepaymentStatus() {
		return repaymentStatus;
	}

	public void setRepaymentStatus(String repaymentStatus) {
		this.repaymentStatus = repaymentStatus;
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
		builder.append("Loan Amount: ")
				.append(formatAmount(amount))
				.append(" RWF\n")
				.append("Interest Rate: ")
				.append(formatInterestRate(interestRate))
				.append("\n")
				.append("Status: ")
				.append(approved ? "Approved" : "Rejected")
				.append("\n");

		if (!approved) {
			builder.append("Reason: ")
					.append(cleanReason(repaymentStatus))
					.append("\n");
		}

		builder.append("Date: ")
				.append(formatTimestamp(timestamp));
		return builder.toString();
	}

	private String formatAmount(BigDecimal value) {
		BigDecimal amountValue = value == null ? BigDecimal.ZERO : value;
		return NumberFormat.getNumberInstance(Locale.US).format(amountValue.stripTrailingZeros());
	}

	private String formatInterestRate(BigDecimal value) {
		BigDecimal rateValue = value == null ? BigDecimal.ZERO : value.stripTrailingZeros();
		return NumberFormat.getNumberInstance(Locale.US).format(rateValue) + "%";
	}

	private String cleanReason(String value) {
		if (value == null || value.isBlank()) {
			return "-";
		}

		String trimmed = value.trim();
		String prefix = "REJECTED:";
		if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
			return trimmed.substring(prefix.length()).trim();
		}

		return trimmed;
	}

	private String formatTimestamp(LocalDateTime value) {
		return value == null ? "-" : value.format(DISPLAY_TIMESTAMP);
	}
}