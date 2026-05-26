package com.app.igirepay.lab1.exception;

public class DuplicateTransactionException extends Exception {

    public DuplicateTransactionException(String message) {
        super(message);
    }
}