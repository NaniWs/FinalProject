package com.example.app;

import org.flywaydb.core.Flyway;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Properties;

public class App {

    public static void main(String[] args) {
        try {
            Properties props = loadProperties();

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");

            // Запуск миграций Flyway
            System.out.println("Starting DB migrations...");
            runMigrations(url, user, password);
            System.out.println("Migrations finished.");

            // Выполнение скрипта test-queries.sql
            System.out.println("Executing SQL script...");
            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                runSqlScriptFromResource(conn, "/test-queries.sql");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Properties loadProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find application.properties");
            }
            props.load(input);
        }
        return props;
    }


    private static void runMigrations(String url, String user, String password) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }

    private static void runSqlScriptFromResource(Connection conn, String resourcePath) throws Exception {
        InputStream inputStream = App.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new RuntimeException("SQL script not found: " + resourcePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Пропускаем комментарии и пустые строки
                if (line.isEmpty() || line.startsWith("--") || line.startsWith("//")) {
                    continue;
                }
                sqlBuilder.append(line).append(" ");
                // Если команда заканчивается точкой с запятой, исполняем
                if (line.endsWith(";")) {
                    String sql = sqlBuilder.toString();
                    executeAndPrint(conn, sql);
                    sqlBuilder.setLength(0); // очистить builder
                }
            }
            // Если что-то осталось без точки с запятой
            if (sqlBuilder.length() > 0) {
                executeAndPrint(conn, sqlBuilder.toString());
            }
        }
    }

    private static void executeAndPrint(Connection conn, String sql) {
        System.out.println("\n--- Executing SQL ---");
        System.out.println(sql);

        sql = sql.trim();
        // Удаляем последний символ ";" если есть
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }

        try (Statement stmt = conn.createStatement()) {
            boolean hasResultSet = stmt.execute(sql);

            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    printResultSet(rs);
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                System.out.println("Update count: " + updateCount);
            }
        } catch (SQLException e) {
            System.err.println("Error executing SQL: " + e.getMessage());
        }
    }

    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        // Заголовки колонок
        for (int i = 1; i <= columnCount; i++) {
            System.out.print(meta.getColumnName(i) + "\t");
        }
        System.out.println();

        // Данные
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(rs.getString(i) + "\t");
            }
            System.out.println();
        }
    }
}
