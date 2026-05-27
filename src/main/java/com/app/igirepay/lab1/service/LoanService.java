package com.app.igirepay.lab1.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.Loan;
import com.app.igirepay.lab1.model.SavingsAccount;
import com.app.igirepay.lab1.util.FileHandler;
import com.app.igirepay.lab2.dao.LoanDAO;

public class LoanService {

	private static final BigDecimal MINIMUM_SAVINGS_BALANCE = new BigDecimal("100.00");
	private static final int MINIMUM_SUCCESSFUL_TRANSACTIONS = 5;
	private static final BigDecimal SAVINGS_LIMIT_RATE = new BigDecimal("0.30");
	private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("5.00");
	private static final BigDecimal TRANSACTION_ACTIVITY_UNIT = new BigDecimal("100.00");

	private final AccountService accountService;
	private final TransactionService transactionService;
	private final FileHandler fileHandler;
	private final LoanDAO loanDAO;
	private final List<Loan> loanHistory = new ArrayList<>();
	private final List<String> failedLoanLogs = new ArrayList<>();
	private int nextLoanId = 1;

	public LoanService(AccountService accountService, TransactionService transactionService) {
		this(accountService, transactionService, new FileHandler(), null);
	}

	public LoanService(AccountService accountService, TransactionService transactionService, FileHandler fileHandler) {
		this(accountService, transactionService, fileHandler, null);
	}

	/**
	 * Primary constructor allowing optional LoanDAO for JDBC-backed persistence.
	 * If loanDAO is null, file-based persistence (FileHandler) is used as fallback.
	 */
	public LoanService(AccountService accountService, TransactionService transactionService, FileHandler fileHandler, LoanDAO loanDAO) {
		this.accountService = accountService;
		this.transactionService = transactionService;
		this.fileHandler = fileHandler == null ? new FileHandler() : fileHandler;
		this.loanDAO = loanDAO;
	}

	public Loan requestLoan(Customer customer, BigDecimal amount) {
		Loan loan = createLoan(customer, amount);
		if (customer == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			rejectLoan(loan, "REJECTED: invalid loan amount");
			return loan;
		}

		BigDecimal savingsBalance = calculateHighestSavingsBalance(customer.getCustomerId());
		int successfulTransactionCount = calculateSuccessfulTransactionCount(customer.getCustomerId());
		boolean hasMinimumSavings = savingsBalance.compareTo(MINIMUM_SAVINGS_BALANCE) >= 0;
		boolean hasMinimumTransactions = successfulTransactionCount >= MINIMUM_SUCCESSFUL_TRANSACTIONS;
		BigDecimal savingsLimit = calculateSavingsLimit(customer.getCustomerId());
		BigDecimal transactionLimit = calculateTransactionLimit(customer.getCustomerId());
		BigDecimal loanLimit = savingsLimit.max(transactionLimit);
		if (hasMinimumSavings && hasMinimumTransactions && amount.compareTo(loanLimit) <= 0 && loanLimit.compareTo(BigDecimal.ZERO) > 0) {
			approveLoan(loan);
			return loan;
		}

		rejectLoan(loan, buildRejectionReason(hasMinimumSavings, hasMinimumTransactions, amount, loanLimit));
		return loan;
	}

	public BigDecimal calculateLoanLimit(Customer customer) {
		if (customer == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal savingsLimit = calculateSavingsLimit(customer.getCustomerId());
		BigDecimal transactionLimit = calculateTransactionLimit(customer.getCustomerId());
		return savingsLimit.max(transactionLimit);
	}

	public boolean evaluateLoanEligibility(Customer customer) {
		if (customer == null) {
			return false;
		}

		String customerId = customer.getCustomerId();
		return calculateHighestSavingsBalance(customerId).compareTo(MINIMUM_SAVINGS_BALANCE) >= 0
				&& calculateSuccessfulTransactionCount(customerId) >= MINIMUM_SUCCESSFUL_TRANSACTIONS;
	}

	public List<Loan> getLoanHistory() {
		return Collections.unmodifiableList(loanHistory);
	}

	public List<Loan> getLoanHistoryForCustomer(String customerId) {
		if (customerId == null) {
			return List.of();
		}

		return loanHistory.stream()
				.filter(loan -> customerId.equals(loan.getCustomerId()))
				.collect(Collectors.toList());
	}

	public List<String> getFailedLoanLogs() {
		return Collections.unmodifiableList(failedLoanLogs);
	}

	// Load a loan from file into memory without persisting
	public void loadLoan(Loan loan) {
		if (loan == null) return;
		loanHistory.add(loan);
		if (!loan.isApproved()) {
			String logEntry = loan.getLoanId() + " - " + loan.getRepaymentStatus();
			failedLoanLogs.add(logEntry);
		}
	}

	private Loan createLoan(Customer customer, BigDecimal amount) {
		Loan loan = new Loan(String.valueOf(nextLoanId++),
				customer == null ? null : customer.getCustomerId(),
				amount,
				DEFAULT_INTEREST_RATE,
				false,
				"PENDING",
				LocalDateTime.now());
		if (customer != null && customer.getDatabaseId() != null) {
			loan.setCustomerDatabaseId(customer.getDatabaseId());
		}
		return loan;
	}

	private void approveLoan(Loan loan) {
		loan.setApproved(true);
		loan.setRepaymentStatus("APPROVED");
		loanHistory.add(loan);
		persistLoan(loan);
	}

	private void rejectLoan(Loan loan, String reason) {
		loan.setApproved(false);
		loan.setRepaymentStatus(reason);
		loanHistory.add(loan);
		String logEntry = loan.getLoanId() + " - " + reason;
		failedLoanLogs.add(logEntry);
		// persist failed log to file for now (no DB table for failed logs)
		fileHandler.saveFailedLoan(logEntry);
		persistLoan(loan);
	}

	private void persistLoan(Loan loan) {
		if (loan == null) return;
		if (loanDAO != null) {
			// Ensure customerDatabaseId is set when possible
			if (loan.getCustomerDatabaseId() == null) {
				try {
					if (loan.getCustomerId() != null) {
						loan.setCustomerDatabaseId(Integer.parseInt(loan.getCustomerId()));
					}
				} catch (NumberFormatException ignored) {
				}
			}
			loanDAO.save(loan);
		} else {
			fileHandler.saveLoan(loan);
		}
	}

	/**
	 * Load all loans from the configured persistence. If a LoanDAO is present, load from database,
	 * otherwise rely on in-memory/file-based loans already loaded.
	 */
	public void loadLoansFromPersistence() {
		if (loanDAO != null) {
			List<Loan> loans = loanDAO.findAll();
			for (Loan l : loans) {
				loadLoan(l);
			}
		}
	}

	/**
	 * Retrieve loan history for a given customer database id. If DAO is configured, query DB directly.
	 */
	public List<Loan> getLoanHistoryForCustomerDatabaseId(Integer customerDatabaseId) {
		if (customerDatabaseId == null) return List.of();
		if (loanDAO != null) {
			return loanDAO.findByCustomerDatabaseId(customerDatabaseId);
		}

		return loanHistory.stream()
				.filter(loan -> customerDatabaseId.equals(loan.getCustomerDatabaseId()))
				.collect(Collectors.toList());
	}

	private String buildRejectionReason(boolean hasMinimumSavings,
	                                  boolean hasMinimumTransactions,
	                                  BigDecimal amount,
	                                  BigDecimal loanLimit) {
		if (!hasMinimumSavings && !hasMinimumTransactions) {
			return "REJECTED: insufficient savings balance and not enough transaction history";
		}

		if (!hasMinimumSavings) {
			return "REJECTED: insufficient savings balance";
		}

		if (!hasMinimumTransactions) {
			return "REJECTED: not enough successful transactions (minimum 5 required)";
		}

		if (amount.compareTo(loanLimit) > 0) {
			return "REJECTED: loan amount exceeds eligibility limit";
		}

		return "REJECTED: loan request not eligible";
	}

	private BigDecimal calculateHighestSavingsBalance(String customerId) {
		if (customerId == null) {
			return BigDecimal.ZERO;
		}

		return accountService.getAccountsForCustomer(customerId).stream()
				.filter(SavingsAccount.class::isInstance)
				.map(SavingsAccount.class::cast)
				.map(SavingsAccount::getBalance)
				.max(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);
	}

	private BigDecimal calculateSavingsLimit(String customerId) {
		if (customerId == null) {
			return BigDecimal.ZERO;
		}

		return accountService.getAccountsForCustomer(customerId).stream()
				.filter(SavingsAccount.class::isInstance)
				.map(SavingsAccount.class::cast)
				.map(SavingsAccount::getBalance)
				.map(balance -> balance.multiply(SAVINGS_LIMIT_RATE))
				.map(balance -> balance.setScale(2, RoundingMode.HALF_UP))
				.max(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);
	}

	private int calculateSuccessfulTransactionCount(String customerId) {
		if (customerId == null) {
			return 0;
		}

		return transactionService.getTransactionHistoryForCustomer(customerId).size();
	}

	private BigDecimal calculateTransactionLimit(String customerId) {
		int successfulTransactionCount = calculateSuccessfulTransactionCount(customerId);
		return TRANSACTION_ACTIVITY_UNIT.multiply(BigDecimal.valueOf(successfulTransactionCount));
	}
}