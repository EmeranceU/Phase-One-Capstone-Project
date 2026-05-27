package com.app.igirepay.lab1.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Loan {

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
		return "Loan{" +
				"databaseId=" + databaseId +
				", customerDatabaseId=" + customerDatabaseId +
				", loanId='" + loanId + '\'' +
				", customerId='" + customerId + '\'' +
				", amount=" + amount +
				", interestRate=" + interestRate +
				", approved=" + approved +
				", repaymentStatus='" + repaymentStatus + '\'' +
				", timestamp=" + timestamp +
				'}';
	}
}