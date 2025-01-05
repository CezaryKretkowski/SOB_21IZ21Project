package org.example.data_base;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {
    public static void main(String[] args) {
        String url = "jdbc:sqlite:database.db";

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                System.out.println("Database created successfully.");
                try (Statement stmt = conn.createStatement()) {
                    String createTableSQL = """
                        CREATE TABLE IF NOT EXISTS logs (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            query TEXT NOT NULL,
                            result TEXT,
                            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
                        );
                        """;
                    stmt.execute(createTableSQL);
                    System.out.println("Table `logs` created successfully.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database setup failed: " + e.getMessage());
        }
    }
}

