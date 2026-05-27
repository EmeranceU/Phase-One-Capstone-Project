package com.app.igirepay.lab2.dao;

import java.util.List;

import com.app.igirepay.lab1.model.Account;

public interface AccountDAO extends GenericDAO<Account, Integer> {

    List<Account> findByCustomerDatabaseId(Integer customerDatabaseId);

    Account findByBusinessAccountId(String accountId);

    List<Account> findWalletAccounts();

    List<Account> findSavingsAccounts();
}
