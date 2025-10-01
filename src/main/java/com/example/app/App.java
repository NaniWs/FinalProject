package com.example.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Properties;

public class App {

    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    public static void main(String[] args) {
        loadProperties();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to DB!");

            // Запускаем скрипт из ресурсов
            runSqlScriptFromResource(connection, "test-queries.sql");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Загружаем параметры из application.properties
    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("Unable to find application.properties");
                System.exit(1);
            }
            props.load(input);
            DB_URL = props.getProperty("db.url");
            DB_USER = props.getProperty("db.user");
            DB_PASSWORD = props.getProperty("db.password");
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    // Читаем SQL скрипт из файла ресурсов и выполняем запросы по отдельности
    private static void runSqlScriptFromResource(Connection conn, String resourceFileName) {
        try (InputStream input = App.class.getClassLoader().getResourceAsStream(resourceFileName)) {
            if (input == null) {
                System.err.println("Unable to find " + resourceFileName);
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder sqlBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // Игнорируем комментарии
                if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }
                sqlBuilder.append(line).append(" ");
                // Если строка заканчивается на ; — считаем запрос готовым к выполнению
                if (line.trim().endsWith(";")) {
                    String sql = sqlBuilder.toString().trim();
                    // Убираем последний символ ;
                    sql = sql.substring(0, sql.length() - 1).trim();

                    System.out.println("\n=== SQL Query ===");
                    System.out.println(sql);
                    System.out.println("=== Result ===");
                    executeAndPrintQuery(conn, sql);

                    sqlBuilder.setLength(0); // очищаем буфер
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Выполняем запрос и выводим результат (только SELECT, для остальных — сообщение)
    private static void executeAndPrintQuery(Connection conn, String sql) {
        try {
            Statement stmt = conn.createStatement();
            boolean hasResultSet = stmt.execute(sql);

            if (hasResultSet) {
                ResultSet rs = stmt.getResultSet();
                printResultSet(rs);
                rs.close();
            } else {
                int updateCount = stmt.getUpdateCount();
                System.out.println("Query executed successfully, affected rows: " + updateCount);
            }

            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error executing query: " + e.getMessage());
        }
    }

    // Выводим содержимое ResultSet в табличном виде
    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        // Заголовок
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
