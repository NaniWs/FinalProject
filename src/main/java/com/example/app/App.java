package com.example.app;

import java.sql.*;
import java.util.Properties;
import java.io.FileInputStream;

public class App {

    public static void main(String[] args) {
        Properties props = new Properties();

        try {
            // Загружаем параметры подключения из файла
            props.load(new FileInputStream("src/main/resources/application.properties"));

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String password = props.getProperty("db.password");

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                conn.setAutoCommit(false); // Управление транзакцией вручную

                try {
                    // Вставляем покупателя с подробными данными
                    int customerId = insertCustomer(conn, "Иван", "Иванов", "+7 999 1234567", "ivan123@example.com");

                    // Вставляем товар с описанием, категорией и количеством
                    int productId = insertProduct(conn, "Ноутбук ASUS, 16GB RAM", 1200.0, 10, "Электроника");

                    // Создаём заказ для покупателя с указанным товаром и количеством
                    int orderId = createOrder(conn, productId, customerId, 2);

                    // Выводим последние 5 заказов
                    printRecentOrders(conn);

                    // Обновляем цену и количество товара
                    updateProduct(conn, productId, 1150.0, 8);

                    // Удаляем тестовые данные
                    deleteTestData(conn, customerId, productId, orderId);

                    conn.commit(); // Фиксируем изменения
                } catch (SQLException e) {
                    conn.rollback(); // Откатываем при ошибке
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Вставка покупателя
    private static int insertCustomer(Connection conn, String firstName, String lastName, String phone, String email) throws SQLException {
        String sql = "INSERT INTO customers(first_name, last_name, phone, email) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int id = rs.getInt("id");
            System.out.println("Добавлен покупатель: " + firstName + " " + lastName + " (ID: " + id + ")");
            return id;
        }
    }

    // Вставка товара
    private static int insertProduct(Connection conn, String description, double price, int quantity, String category) throws SQLException {
        String sql = "INSERT INTO products(description, price, quantity, category) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, description);
            stmt.setDouble(2, price);
            stmt.setInt(3, quantity);
            stmt.setString(4, category);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int id = rs.getInt("id");
            System.out.println("Добавлен товар: " + description + " (ID: " + id + ")");
            return id;
        }
    }

    // Создание заказа
    private static int createOrder(Connection conn, int productId, int customerId, int quantity) throws SQLException {
        // По умолчанию ставим статус заказа с id=1 (например, "Новый")
        String sql = "INSERT INTO orders(product_id, customer_id, order_date, quantity, status_id) VALUES (?, ?, current_timestamp, ?, 1) RETURNING id";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            stmt.setInt(2, customerId);
            stmt.setInt(3, quantity);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            int id = rs.getInt("id");
            System.out.println("Создан заказ (ID: " + id + ")");
            return id;
        }
    }

    // Вывод последних 5 заказов с деталями
    private static void printRecentOrders(Connection conn) throws SQLException {
        String sql = """
            SELECT o.id AS order_id,
                   c.first_name || ' ' || c.last_name AS customer,
                   p.description AS product,
                   o.quantity,
                   s.name AS status,
                   o.order_date
            FROM orders o
            JOIN customers c ON o.customer_id = c.id
            JOIN products p ON o.product_id = p.id
            JOIN order_status s ON o.status_id = s.id
            ORDER BY o.order_date DESC
            LIMIT 5
            """;

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\nПоследние 5 заказов:");
            while (rs.next()) {
                System.out.printf("Заказ #%d | Покупатель: %s | Товар: %s | Кол-во: %d | Статус: %s | Дата: %s%n",
                        rs.getInt("order_id"),
                        rs.getString("customer"),
                        rs.getString("product"),
                        rs.getInt("quantity"),
                        rs.getString("status"),
                        rs.getTimestamp("order_date"));
            }
        }
    }

    // Обновление товара (цена и количество)
    private static void updateProduct(Connection conn, int productId, double newPrice, int newQuantity) throws SQLException {
        String sql = "UPDATE products SET price = ?, quantity = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, newPrice);
            stmt.setInt(2, newQuantity);
            stmt.setInt(3, productId);
            int rows = stmt.executeUpdate();
            System.out.println("Обновлён товар ID " + productId + " (обновлено строк: " + rows + ")");
        }
    }

    // Удаление тестовых записей из таблиц orders, products и customers
    private static void deleteTestData(Connection conn, int customerId, int productId, int orderId) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM orders WHERE id = " + orderId);
            stmt.executeUpdate("DELETE FROM products WHERE id = " + productId);
            stmt.executeUpdate("DELETE FROM customers WHERE id = " + customerId);
            System.out.println("Удалены тестовые записи.");
        }
    }
}
