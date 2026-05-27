package com.app.igirepay.lab2.dao;

import java.util.List;

import com.app.igirepay.lab1.model.Loan;

public interface LoanDAO extends GenericDAO<Loan, Integer> {

    List<Loan> findByCustomerDatabaseId(Integer customerDatabaseId);
}