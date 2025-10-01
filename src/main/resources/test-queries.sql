------------------------------------------------------------
-- 1. Список всех заказов за последние 7 дней с клиентом и товаром
SELECT
    o.id AS order_id,
    o.order_date,
    c.first_name || ' ' || c.last_name AS customer_name,
    p.description AS product_description,
    o.quantity,
    os.name AS status
FROM orders o
JOIN customers c ON o.customer_id = c.id
JOIN products p ON o.product_id = p.id
JOIN order_status os ON o.status_id = os.id
WHERE o.order_date >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY o.order_date DESC;

------------------------------------------------------------
-- 2. Топ-3 популярных товара по общему количеству заказов
SELECT
    p.description,
    SUM(o.quantity) AS total_sold
FROM orders o
JOIN products p ON o.product_id = p.id
GROUP BY p.description
ORDER BY total_sold DESC
LIMIT 3;

------------------------------------------------------------
-- 3. Количество заказов по каждому статусу
SELECT
    os.name AS status,
    COUNT(o.id) AS total_orders
FROM order_status os
LEFT JOIN orders o ON o.status_id = os.id
GROUP BY os.name
ORDER BY total_orders DESC;

------------------------------------------------------------
-- 4. Клиенты, сделавшие более одного заказа
SELECT
    c.id,
    c.first_name,
    c.last_name,
    COUNT(o.id) AS order_count
FROM customers c
JOIN orders o ON o.customer_id = c.id
GROUP BY c.id
HAVING COUNT(o.id) > 1
ORDER BY order_count DESC;

------------------------------------------------------------
-- 5. Все товары с остатком меньше 10 штук
SELECT
    id,
    description,
    quantity
FROM products
WHERE quantity < 10
ORDER BY quantity ASC;

------------------------------------------------------------
-- 6. Обновление количества товара (эмуляция покупки)
-- Уменьшаем quantity на 2, если достаточно на складе
UPDATE products
SET quantity = quantity - 2
WHERE id = 1 AND quantity >= 2;

------------------------------------------------------------
-- 7. Изменение статуса заказа
-- Пример: перевести заказ с id = 1 в статус "Доставлен"
UPDATE orders
SET status_id = (SELECT id FROM order_status WHERE name = 'Доставлен')
WHERE id = 1;

------------------------------------------------------------
-- 8. Обновление email клиента
-- Пример: изменить email клиента с id = 2
UPDATE customers
SET email = 'new_email@example.com'
WHERE id = 2;

------------------------------------------------------------
-- 9. Удаление клиентов, у которых нет заказов
DELETE FROM customers
WHERE id NOT IN (
    SELECT DISTINCT customer_id FROM orders
);

------------------------------------------------------------
-- 10. Удаление заказов старше 1 года
DELETE FROM orders
WHERE order_date < CURRENT_DATE - INTERVAL '1 year';


