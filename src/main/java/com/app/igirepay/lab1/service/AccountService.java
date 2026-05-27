package com.app.igirepay.lab1.service;

import com.app.igirepay.lab1.model.Account;
import com.app.igirepay.lab1.model.Customer;
import com.app.igirepay.lab2.dao.AccountDAO;
import com.app.igirepay.lab2.dao.CustomerDAO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AccountService {

    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;

    public AccountService() {
        this(null, null);
    }

    public AccountService(CustomerDAO customerDAO) {
        this(customerDAO, null);
    }

    public AccountService(CustomerDAO customerDAO, AccountDAO accountDAO) {
        this.customerDAO = customerDAO;
        this.accountDAO = accountDAO;
    }

    public boolean addCustomer(Customer customer) {
        if (customer == null || customer.getCustomerId() == null || customers.containsKey(customer.getCustomerId())) {
            return false;
        }

        customers.put(customer.getCustomerId(), customer);

        if (customerDAO != null) {
            try {
                customerDAO.save(customer);
            } catch (Exception exception) {
                System.err.println("Warning: Failed to persist customer to PostgreSQL: " + exception.getMessage());
            }
        }

        return true;
    }

    public Customer findCustomerById(String customerId) {
        if (customerId == null) {
            return null;
        }

        Customer customer = customers.get(customerId);
        if (customer != null || customerDAO == null) {
            return customer;
        }

        try {
            Customer databaseCustomer = customerDAO.findById(parseId(customerId));
            if (databaseCustomer != null) {
                customers.put(databaseCustomer.getCustomerId(), databaseCustomer);
            }
            return databaseCustomer;
        } catch (Exception exception) {
            System.err.println("Warning: Failed to look up customer in PostgreSQL: " + exception.getMessage());
            return null;
        }
    }

    public boolean addAccount(Account account) {
        if (account == null || account.getAccountId() == null || accounts.containsKey(account.getAccountId())) {
            return false;
        }

        accounts.put(account.getAccountId(), account);

        if (accountDAO != null) {
            try {
                accountDAO.save(account);
            } catch (Exception exception) {
                System.err.println("Warning: Failed to persist account to PostgreSQL: " + exception.getMessage());
            }
        }

        return true;
    }

    public boolean addAccountToCustomer(String customerId, Account account) {
        Customer customer = findCustomerById(customerId);
        if (customer == null || account == null) {
            return false;
        }

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

        if (customer.getDatabaseId() != null && accountDAO != null) {
            try {
                return accountDAO.findByCustomerDatabaseId(customer.getDatabaseId());
            } catch (Exception exception) {
                System.err.println("Warning: Failed to load accounts from PostgreSQL: " + exception.getMessage());
            }
        }

        return customer.getAccounts();
    }

    public void loadFromDatabase() {
        customers.clear();
        accounts.clear();

        if (customerDAO != null) {
            for (Customer customer : customerDAO.findAll()) {
                customers.put(customer.getCustomerId(), customer);
            }
        }

        if (accountDAO != null) {
            for (Account account : accountDAO.findAll()) {
                accounts.put(account.getAccountId(), account);
                Customer owner = customers.get(account.getCustomerId());
                if (owner != null && owner.findAccountById(account.getAccountId()) == null) {
                    owner.addAccount(account);
                }
            }
        }
    }

    private int parseId(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Customer id must be numeric for PostgreSQL lookup", exception);
        }
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
