package com.app.igirepay.lab1.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab1.util.FileHandler;
import com.app.igirepay.lab2.dao.AccountDAO;
import com.app.igirepay.lab2.dao.CustomerDAO;

public class AccountService {

    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final FileHandler fileHandler;
    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;

    public AccountService() {
        this(new FileHandler(), null, null);
    }

    public AccountService(FileHandler fileHandler) {
        this(fileHandler, null, null);
    }

    public AccountService(FileHandler fileHandler, CustomerDAO customerDAO) {
        this(fileHandler, customerDAO, null);
    }

    public AccountService(FileHandler fileHandler, CustomerDAO customerDAO, AccountDAO accountDAO) {
        this.fileHandler = fileHandler == null ? new FileHandler() : fileHandler;
        this.customerDAO = customerDAO;
        this.accountDAO = accountDAO;
    }

    public boolean addCustomer(Customer customer) {
        if (customer == null || customer.getCustomerId() == null || customers.containsKey(customer.getCustomerId())) {
            return false;
        }

        customers.put(customer.getCustomerId(), customer);
        fileHandler.saveCustomer(customer);
        
        if (customerDAO != null) {
            try {
                customerDAO.save(customer);
            } catch (Exception exception) {
                System.err.println("Warning: Failed to persist customer to PostgreSQL: " + exception.getMessage());
            }
        }
        
        return true;
    }

    // Add customer during startup load without persisting back to file
    public boolean addCustomerFromFile(Customer customer) {
        if (customer == null || customer.getCustomerId() == null || customers.containsKey(customer.getCustomerId())) {
            return false;
        }

        customers.put(customer.getCustomerId(), customer);
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
        
        if (accountDAO != null) {
            try {
                accountDAO.save(account);
            } catch (Exception exception) {
                System.err.println("Warning: Failed to persist account to PostgreSQL: " + exception.getMessage());
            }
        }
        
        return true;
    }

    // Add account during startup load without persisting back to file; attach to customer if present
    public boolean addAccountFromFile(Account account) {
        if (account == null || account.getAccountId() == null || accounts.containsKey(account.getAccountId())) {
            return false;
        }

        accounts.put(account.getAccountId(), account);
        Customer owner = customers.get(account.getCustomerId());
        if (owner != null) {
            owner.addAccount(account);
        }
        return true;
    }

    public boolean addAccountToCustomer(String customerId, Account account) {
        Customer customer = findCustomerById(customerId);
        if (customer == null || account == null) {
            return false;
        }

        // Set customer database ID for JDBC persistence if available
        if (customer.getDatabaseId() != null) {
            account.setCustomerDatabaseId(customer.getDatabaseId());
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

        Account account = accounts.get(accountId);
        if (account != null || accountDAO == null) {
            return account;
        }

        try {
            Account databaseAccount = accountDAO.findByBusinessAccountId(accountId);
            if (databaseAccount != null) {
                accounts.put(databaseAccount.getAccountId(), databaseAccount);
                Customer owner = customers.get(databaseAccount.getCustomerId());
                if (owner != null && owner.findAccountById(databaseAccount.getAccountId()) == null) {
                    owner.addAccount(databaseAccount);
                }
            }
            return databaseAccount;
        } catch (Exception exception) {
            System.err.println("Warning: Failed to look up account in PostgreSQL: " + exception.getMessage());
            return null;
        }
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

        // If customer has database ID and accountDAO is available, load from PostgreSQL
        if (customer.getDatabaseId() != null && accountDAO != null) {
            try {
                return accountDAO.findByCustomerDatabaseId(customer.getDatabaseId());
            } catch (Exception exception) {
                System.err.println("Warning: Failed to load accounts from PostgreSQL: " + exception.getMessage());
            }
        }
        
        // Fall back to in-memory accounts
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