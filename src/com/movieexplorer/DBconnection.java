package com.movieexplorer;

import java.sql.Connection;

import java.sql.DriverManager;

class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/movie_explorer";
    private static final String USER = "root";
    private static final String PASSWORD = "Maruthi@2345";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
