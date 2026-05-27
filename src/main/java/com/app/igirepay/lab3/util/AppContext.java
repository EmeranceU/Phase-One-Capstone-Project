package com.app.igirepay.lab3.util;

import java.util.Comparator;
import java.util.List;

import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.model.SavingsAccount;
import com.app.igirepay.lab1.model.Transaction;
import com.app.igirepay.lab1.model.WalletAccount;
import com.app.igirepay.lab1.service.AccountService;
import com.app.igirepay.lab1.service.AuthService;
import com.app.igirepay.lab1.service.LoanService;
import com.app.igirepay.lab1.service.TransactionService;
import com.app.igirepay.lab2.dao.AccountDAO;
import com.app.igirepay.lab2.dao.CustomerDAO;
import com.app.igirepay.lab2.dao.TransactionDAO;
import com.app.igirepay.lab2.dao.impl.AccountDAOImpl;
import com.app.igirepay.lab2.dao.impl.CustomerDAOImpl;
import com.app.igirepay.lab2.dao.impl.LoanDAOImpl;
import com.app.igirepay.lab2.dao.impl.TransactionDAOImpl;

public final class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final AuthService authService;
    private final LoanService loanService;

    private int nextCustomerId = 1;
    private int nextWalletId = 1;
    private int nextSavingsId = 1;
    private int nextTransactionId = 1;

    private Customer currentCustomer;

    private AppContext() {
        customerDAO = new CustomerDAOImpl();
        accountDAO = new AccountDAOImpl();
        transactionDAO = new TransactionDAOImpl();

        accountService = new AccountService(customerDAO, accountDAO);
        transactionService = new TransactionService(transactionDAO, accountDAO);
        authService = new AuthService(customerDAO);
        loanService = new LoanService(accountService, transactionService, new LoanDAOImpl());

        reloadAllFromDatabase();
    }

    public static AppContext getInstance() {
        return INSTANCE;
    }

    public synchronized void reloadAllFromDatabase() {
        accountService.loadFromDatabase();
        transactionService.loadFromDatabase();
        loanService.loadFromDatabase();

        if (currentCustomer != null) {
            currentCustomer = customerDAO.findByPhone(currentCustomer.getPhoneNumber());
        }

        syncIdCounters();
    }

    public synchronized void setCurrentCustomer(Customer customer) {
        this.currentCustomer = customer;
    }

    public synchronized Customer getCurrentCustomer() {
        return currentCustomer;
    }

    public synchronized String nextCustomerBusinessId() {
        return String.valueOf(nextCustomerId++);
    }

    public synchronized String nextWalletAccountId() {
        return "WAL-" + nextWalletId++;
    }

    public synchronized String nextSavingsAccountId() {
        return "SAV-" + nextSavingsId++;
    }

    public synchronized String nextTransactionBusinessId() {
        return String.valueOf(nextTransactionId++);
    }

    public Customer findCustomerByPhone(String phoneNumber) {
        return customerDAO.findByPhone(phoneNumber);
    }

    public Account findWalletAccountForCustomer(Customer customer) {
        if (customer == null) {
            return null;
        }

        List<Account> accounts = accountService.getAccountsForCustomer(customer.getCustomerId());
        return accounts.stream()
                .filter(WalletAccount.class::isInstance)
                .findFirst()
                .orElse(null);
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public LoanService getLoanService() {
        return loanService;
    }

    private void syncIdCounters() {
        nextCustomerId = accountService.getCustomers().stream()
                .map(Customer::getCustomerId)
                .map(AppContext::extractNumericId)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        nextWalletId = accountService.getAccounts().stream()
                .filter(WalletAccount.class::isInstance)
                .map(Account::getAccountId)
                .map(AppContext::extractNumericId)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        nextSavingsId = accountService.getAccounts().stream()
                .filter(SavingsAccount.class::isInstance)
                .map(Account::getAccountId)
                .map(AppContext::extractNumericId)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        nextTransactionId = transactionService.getTransactionHistory().stream()
                .map(Transaction::getTransactionId)
                .map(AppContext::extractNumericId)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    private static int extractNumericId(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        String digits = value.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return 0;
        }

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
