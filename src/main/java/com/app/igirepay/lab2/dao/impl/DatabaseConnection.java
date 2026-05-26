package com.app.igirepay.lab2.dao.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/igirepay_db";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "postgres";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }
}