CREATE TABLE products (
  id SERIAL PRIMARY KEY,
  description TEXT,
  price NUMERIC(10,2),
  quantity INT,
  category VARCHAR(50)
);
