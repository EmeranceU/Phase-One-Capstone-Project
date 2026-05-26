package com.app.igirepay.lab1.service;

import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.util.FileHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

public class AccountService {

    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final FileHandler fileHandler;

    public AccountService() {
        this(new FileHandler());
    }

    public AccountService(FileHandler fileHandler) {
        this.fileHandler = fileHandler == null ? new FileHandler() : fileHandler;
    }

    public boolean addCustomer(Customer customer) {
        if (customer == null || customer.getCustomerId() == null || customers.containsKey(customer.getCustomerId())) {
            return false;
        }

        customers.put(customer.getCustomerId(), customer);
        fileHandler.saveCustomer(customer);
        return true;
    }

    public Customer findCustomerById(String customerId) {
        if (customerId == null) {
            return null;
        }

        return customers.get(customerId);
    }

    public boolean addAccount(Account account) {
        if (account == null || account.getAccountId() == null || accounts.containsKey(account.getAccountId())) {
            return false;
        }

        accounts.put(account.getAccountId(), account);
        fileHandler.saveAccount(account);
        return true;
    }

    public boolean addAccountToCustomer(String customerId, Account account) {
        Customer customer = findCustomerById(customerId);
        if (customer == null || account == null) {
            return false;
        }

        if (!addAccount(account)) {
            return false;
        }

        customer.addAccount(account);
        return true;
    }

    public Account findAccountById(String accountId) {
        if (accountId == null) {
            return null;
        }

        return accounts.get(accountId);
    }

    public List<Customer> getCustomers() {
        return List.copyOf(customers.values());
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accounts.values()));
    }

    public List<Account> getAccountsForCustomer(String customerId) {
        Customer customer = findCustomerById(customerId);
        if (customer == null) {
            return List.of();
        }

        return customer.getAccounts();
    }

    public boolean removeAccount(String accountId) {
        if (accountId == null) {
            return false;
        }

        Account removedAccount = accounts.remove(accountId);
        if (removedAccount == null) {
            return false;
        }

        customers.values().forEach(customer -> customer.removeAccount(accountId));
        return true;
    }
}