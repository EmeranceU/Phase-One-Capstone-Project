package com.app.igirepay.lab1.service;

import com.app.igirepay.lab1.model.Account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountService {

    private final List<Account> accounts = new ArrayList<>();

    public boolean addAccount(Account account) {
        if (account == null || account.getAccountId() == null) {
            return false;
        }

        if (findAccountById(account.getAccountId()) != null) {
            return false;
        }

        accounts.add(account);
        return true;
    }

    public Account findAccountById(String accountId) {
        if (accountId == null) {
            return null;
        }

        return accounts.stream()
                .filter(account -> accountId.equals(account.getAccountId()))
                .findFirst()
                .orElse(null);
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public boolean removeAccount(String accountId) {
        return accounts.removeIf(account -> accountId != null && accountId.equals(account.getAccountId()));
    }
}