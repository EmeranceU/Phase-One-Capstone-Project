package com.app.igirepay.lab2.dao;

import com.app.igirepay.lab1.model.Account;

import java.util.List;

public interface AccountDAO extends GenericDAO<Account, Integer> {

    List<Account> findByCustomerDatabaseId(Integer customerDatabaseId);

    Account findByBusinessAccountId(String accountId);

    List<Account> findWalletAccounts();

    List<Account> findSavingsAccounts();
}
