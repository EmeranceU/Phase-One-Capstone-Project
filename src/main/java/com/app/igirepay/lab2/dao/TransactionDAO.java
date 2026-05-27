package com.app.igirepay.lab2.dao;

import java.util.List;

import com.app.igirepay.lab1.model.Transaction;

public interface TransactionDAO extends GenericDAO<Transaction, Integer> {

    Transaction findByReferenceId(String referenceId);

    List<Transaction> findBySourceAccountDatabaseId(Integer accountDatabaseId);

    List<Transaction> findByDestinationAccountDatabaseId(Integer accountDatabaseId);

    List<Transaction> findByCustomerDatabaseId(Integer customerDatabaseId);
}