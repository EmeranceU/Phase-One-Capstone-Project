package com.app.igirepay.lab1.service;

import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.Loan;
import com.app.igirepay.lab1.model.SavingsAccount;
import com.app.igirepay.lab1.model.WalletAccount;
import com.app.igirepay.lab1.util.FileHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LoanService {

	private static final BigDecimal SAVINGS_LIMIT_RATE = new BigDecimal("0.30");
	private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("5.00");
	private static final BigDecimal WALLET_ACTIVITY_UNIT = new BigDecimal("100.00");

	private final AccountService accountService;
	private final TransactionService transactionService;
	private final FileHandler fileHandler;
	private final List<Loan> loanHistory = new ArrayList<>();
	private final List<String> failedLoanLogs = new ArrayList<>();
	private int nextLoanId = 1;

	public LoanService(AccountService accountService, TransactionService transactionService) {
		this(accountService, transactionService, new FileHandler());
	}

	public LoanService(AccountService accountService, TransactionService transactionService, FileHandler fileHandler) {
		this.accountService = accountService;
		this.transactionService = transactionService;
		this.fileHandler = fileHandler == null ? new FileHandler() : fileHandler;
	}

	public Loan requestLoan(Customer customer, BigDecimal amount) {
		Loan loan = createLoan(customer, amount);
		if (customer == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			rejectLoan(loan, "REJECTED: invalid loan amount");
			return loan;
		}

		BigDecimal savingsLimit = calculateSavingsLimit(customer.getCustomerId());
		BigDecimal walletLimit = calculateWalletLimit(customer.getCustomerId());
		BigDecimal loanLimit = savingsLimit.max(walletLimit);
		if (amount.compareTo(loanLimit) <= 0 && loanLimit.compareTo(BigDecimal.ZERO) > 0) {
			approveLoan(loan);
			return loan;
		}

		rejectLoan(loan, buildRejectionReason(savingsLimit, walletLimit, amount, loanLimit));
		return loan;
	}

	public BigDecimal calculateLoanLimit(Customer customer) {
		if (customer == null) {
			return BigDecimal.ZERO;
		}

		BigDecimal savingsLimit = calculateSavingsLimit(customer.getCustomerId());
		BigDecimal walletLimit = calculateWalletLimit(customer.getCustomerId());
		return savingsLimit.max(walletLimit);
	}

	public boolean evaluateLoanEligibility(Customer customer) {
		return calculateLoanLimit(customer).compareTo(BigDecimal.ZERO) > 0;
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

	private Loan createLoan(Customer customer, BigDecimal amount) {
		return new Loan(String.valueOf(nextLoanId++),
				customer == null ? null : customer.getCustomerId(),
				amount,
				DEFAULT_INTEREST_RATE,
				false,
				"PENDING",
				LocalDateTime.now());
	}

	private void approveLoan(Loan loan) {
		loan.setApproved(true);
		loan.setRepaymentStatus("APPROVED");
		loanHistory.add(loan);
		fileHandler.saveLoan(loan);
	}

	private void rejectLoan(Loan loan, String reason) {
		loan.setApproved(false);
		loan.setRepaymentStatus(reason);
		loanHistory.add(loan);
		String logEntry = loan.getLoanId() + " - " + reason;
		failedLoanLogs.add(logEntry);
		fileHandler.saveFailedLoan(logEntry);
		fileHandler.saveLoan(loan);
	}

	private String buildRejectionReason(BigDecimal savingsLimit, BigDecimal walletLimit, BigDecimal amount, BigDecimal loanLimit) {
		if (savingsLimit.compareTo(BigDecimal.ZERO) <= 0 && walletLimit.compareTo(BigDecimal.ZERO) <= 0) {
			return "REJECTED: insufficient savings balance and not enough transaction activity";
		}

		if (savingsLimit.compareTo(BigDecimal.ZERO) <= 0) {
			return "REJECTED: insufficient savings balance";
		}

		if (walletLimit.compareTo(BigDecimal.ZERO) <= 0) {
			return "REJECTED: not enough transaction activity";
		}

		if (amount.compareTo(loanLimit) > 0) {
			return "REJECTED: loan amount exceeds eligibility limit";
		}

		return "REJECTED: loan request not eligible";
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

	private BigDecimal calculateWalletLimit(String customerId) {
		if (customerId == null) {
			return BigDecimal.ZERO;
		}

		boolean hasWalletAccount = accountService.getAccountsForCustomer(customerId).stream()
				.anyMatch(WalletAccount.class::isInstance);
		if (!hasWalletAccount) {
			return BigDecimal.ZERO;
		}

		int successfulTransactionCount = transactionService.getTransactionHistoryForCustomer(customerId).size();
		return WALLET_ACTIVITY_UNIT.multiply(BigDecimal.valueOf(successfulTransactionCount));
	}
}