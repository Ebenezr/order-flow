-- ============================================================================
-- INITIAL DATA SETUP FOR ORDER FLOW DATABASE
-- ============================================================================

-- ============================================================================
-- SAMPLE INVENTORY DATA
-- ============================================================================
INSERT INTO inventory (ingredient_id, ingredient_name, category, available_quantity) VALUES
('ING001', 'Tomato', 'Vegetables', 100),
('ING002', 'Lettuce', 'Vegetables', 150),
('ING003', 'Cheese', 'Dairy', 80),
('ING004', 'Chicken Breast', 'Meat', 50),
('ING005', 'Beef Patty', 'Meat', 60),
('ING006', 'Onion', 'Vegetables', 120),
('ING007', 'Pickles', 'Condiments', 70),
('ING008', 'Mayonnaise', 'Condiments', 40),
('ING009', 'Mustard', 'Condiments', 35),
('ING010', 'Bacon', 'Meat', 45),
('ING011', 'Eggs', 'Dairy', 200),
('ING012', 'Bread', 'Grains', 90),
('ING013', 'Rice', 'Grains', 150),
('ING014', 'Potatoes', 'Vegetables', 110),
('ING015', 'Salt', 'Spices', 500);

-- ============================================================================
-- SAMPLE ORDERS DATA (Optional - for testing)
-- ============================================================================
-- Uncomment the following lines to add sample order data
/*
INSERT INTO orders (order_id, customer_id, status, subtotal, vat, service_charge, discount, total_amount) VALUES
('ORD001', 'CUST001', 'COMPLETED', 50.00, 7.50, 5.00, 0.00, 62.50),
('ORD002', 'CUST002', 'PENDING', 75.00, 11.25, 7.50, 5.00, 88.75),
('ORD003', 'CUST003', 'CANCELLED', 100.00, 15.00, 10.00, 10.00, 115.00);

INSERT INTO order_items (order_id, product_id, product_name, quantity, price, category) VALUES
('ORD001', 'PROD001', 'Cheeseburger', 1, 50.00, 'Main'),
('ORD002', 'PROD002', 'Chicken Sandwich', 2, 37.50, 'Main'),
('ORD003', 'PROD003', 'Salad', 1, 100.00, 'Salads');

INSERT INTO payment_transactions (transaction_id, order_id, amount, status) VALUES
('TXN001', 'ORD001', 62.50, 'COMPLETED'),
('TXN002', 'ORD002', 88.75, 'PENDING'),
('TXN003', 'ORD003', 115.00, 'FAILED');

INSERT INTO receipts (receipt_number, order_id, transaction_id, subtotal, vat, service_charge, discount, total, vat_number, business_name) VALUES
('RCP001', 'ORD001', 'TXN001', 50.00, 7.50, 5.00, 0.00, 62.50, 'VAT123456', 'Blind Restaurant'),
('RCP002', 'ORD002', 'TXN002', 75.00, 11.25, 7.50, 5.00, 88.75, 'VAT123456', 'Blind Restaurant');

INSERT INTO kitchen_orders (kitchen_order_id, order_id, status) VALUES
('KO001', 'ORD001', 'COMPLETED'),
('KO002', 'ORD002', 'PENDING'),
('KO003', 'ORD003', 'CANCELLED');

INSERT INTO inventory_reservations (reservation_id, order_id, ingredient_id, quantity, status, expires_at) VALUES
('RES001', 'ORD001', 'ING003', 2, 'CONFIRMED', DATE_ADD(NOW(), INTERVAL 30 MINUTE)),
('RES002', 'ORD001', 'ING004', 1, 'CONFIRMED', DATE_ADD(NOW(), INTERVAL 30 MINUTE)),
('RES003', 'ORD002', 'ING005', 2, 'CONFIRMED', DATE_ADD(NOW(), INTERVAL 30 MINUTE));
*/

-- ============================================================================
-- NOTE: These are optional sample inserts. Uncomment as needed for testing.
-- ============================================================================

