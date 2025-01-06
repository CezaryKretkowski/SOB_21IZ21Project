package org.example.data_base;

import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final String dbUrl = "jdbc:sqlite:database.db";
    private Connection connection;

    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(dbUrl);
            System.out.println("Connected to SQLite database.");
            initializeDatabase();
        } catch (SQLException e) {
            System.out.println("Failed to connect to SQLite database: " + e.getMessage());
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        String createLogsTableSQL = """
            CREATE TABLE IF NOT EXISTS logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                query TEXT NOT NULL,
                result TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            );
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createLogsTableSQL);
            System.out.println("Database initialized: logs table is ready.");
        } catch (SQLException e) {
            System.out.println("Failed to initialize database: " + e.getMessage());
        }
    }

    public String executeQuery(String query) {
        try (Statement stmt = connection.createStatement()) {
            boolean isResultSet = stmt.execute(query);
            StringBuilder result = new StringBuilder();

            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    for (int i = 1; i <= columnCount; i++) {
                        result.append(metaData.getColumnName(i)).append("\t");
                    }
                    result.append("\n");

                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            result.append(rs.getString(i)).append("\t");
                        }
                        result.append("\n");
                    }
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                result.append("Update count: ").append(updateCount);
            }

            logQuery(query, result.toString());

            return result.toString();
        } catch (SQLException e) {
            return "SQL Error: " + e.getMessage();
        }
    }

    private void logQuery(String query, String result) {
        String insertLogSQL = "INSERT INTO logs (query, result) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertLogSQL)) {
            pstmt.setString(1, query);
            pstmt.setString(2, result);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to log query: " + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("SQLite connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Failed to close SQLite connection: " + e.getMessage());
        }
    }
}


