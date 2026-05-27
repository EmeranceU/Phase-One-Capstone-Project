package com.app.igirepay.lab2.dao;

import com.app.igirepay.lab1.model.Customer;

public interface CustomerDAO extends GenericDAO<Customer, Integer> {

    Customer findByPhone(String phoneNumber);
}
