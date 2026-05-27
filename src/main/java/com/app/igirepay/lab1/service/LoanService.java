package com.app.igirepay.lab1.service;

import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.Loan;
import com.app.igirepay.lab1.model.SavingsAccount;
import com.app.igirepay.lab2.dao.LoanDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LoanService {

    private static final BigDecimal MINIMUM_SAVINGS_BALANCE = new BigDecimal("100.00");
    private static final int MINIMUM_SUCCESSFUL_TRANSACTIONS = 5;
    private static final BigDecimal SAVINGS_LIMIT_RATE = new BigDecimal("0.30");
    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("5.00");
    private static final BigDecimal TRANSACTION_ACTIVITY_UNIT = new BigDecimal("100.00");

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final LoanDAO loanDAO;
    private final List<Loan> loanHistory = new ArrayList<>();
    private final List<String> failedLoanLogs = new ArrayList<>();
    private int nextLoanId = 1;

    public LoanService(AccountService accountService, TransactionService transactionService) {
        this(accountService, transactionService, null);
    }

    public LoanService(AccountService accountService, TransactionService transactionService, LoanDAO loanDAO) {
        this.accountService = accountService;
        this.transactionService = transactionService;
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

    public void loadFromDatabase() {
        if (loanDAO == null) {
            return;
        }

        loanHistory.clear();
        failedLoanLogs.clear();
        for (Loan loan : loanDAO.findAll()) {
            loadLoan(loan);
        }
    }

    public List<Loan> getLoanHistoryForCustomerDatabaseId(Integer customerDatabaseId) {
        if (customerDatabaseId == null || loanDAO == null) {
            return List.of();
        }

        return loanDAO.findByCustomerDatabaseId(customerDatabaseId);
    }

    public void loadLoan(Loan loan) {
        if (loan == null) {
            return;
        }

        loanHistory.add(loan);
        if (!loan.isApproved()) {
            failedLoanLogs.add(loan.getLoanId() + " - " + loan.getRepaymentStatus());
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
        failedLoanLogs.add(loan.getLoanId() + " - " + reason);
        persistLoan(loan);
    }

    private void persistLoan(Loan loan) {
        if (loan == null || loanDAO == null) {
            return;
        }

        loanDAO.save(loan);
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

        return transactionService.getTransactionHistoryForCustomerFromDB(
                accountService.findCustomerById(customerId) == null ? null : accountService.findCustomerById(customerId).getDatabaseId()).size();
    }

    private BigDecimal calculateTransactionLimit(String customerId) {
        int successfulTransactionCount = calculateSuccessfulTransactionCount(customerId);
        return TRANSACTION_ACTIVITY_UNIT.multiply(BigDecimal.valueOf(successfulTransactionCount));
    }
}
