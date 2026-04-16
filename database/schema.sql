-- ============================================================================
-- ORDER FLOW DATABASE SCHEMA
-- Generated from Entity Classes
-- ============================================================================

-- ============================================================================
-- ORDERS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    customer_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    subtotal DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    vat DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    service_charge DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    cancellation_reason VARCHAR(500),
    INDEX idx_order_id (order_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- ============================================================================
-- ORDER_ITEMS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(255) NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    category VARCHAR(100),
    product_snapshot LONGTEXT,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
);

-- ============================================================================
-- PAYMENT_TRANSACTIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_id VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- ============================================================================
-- RECEIPTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS receipts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receipt_number VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255),
    subtotal DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    vat DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    service_charge DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    vat_number VARCHAR(50),
    business_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES payment_transactions(transaction_id) ON DELETE SET NULL,
    INDEX idx_receipt_number (receipt_number),
    INDEX idx_order_id (order_id),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_created_at (created_at)
);

-- ============================================================================
-- KITCHEN_ORDERS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS kitchen_orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    kitchen_order_id VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    INDEX idx_kitchen_order_id (kitchen_order_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- ============================================================================
-- INVENTORY TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ingredient_id VARCHAR(255) NOT NULL UNIQUE,
    ingredient_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    available_quantity INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ingredient_id (ingredient_id),
    INDEX idx_category (category),
    INDEX idx_updated_at (updated_at)
);

-- ============================================================================
-- INVENTORY_RESERVATIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS inventory_reservations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    ingredient_id VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (ingredient_id) REFERENCES inventory(ingredient_id) ON DELETE CASCADE,
    INDEX idx_reservation_id (reservation_id),
    INDEX idx_order_id (order_id),
    INDEX idx_ingredient_id (ingredient_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_expires_at (expires_at)
);

-- ============================================================================
-- PROCESSED_EVENTS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_processed_at (processed_at)
);


-- ============================================================================
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- ============================================================================

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at ON orders(status, created_at);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id_product_id ON order_items(order_id, product_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_id_status ON payment_transactions(order_id, status);
CREATE INDEX IF NOT EXISTS idx_kitchen_orders_order_id_status ON kitchen_orders(order_id, status);
CREATE INDEX IF NOT EXISTS idx_inventory_reservations_order_id_status ON inventory_reservations(order_id, status);

-- ============================================================================
-- NOTE: MenuItem and Ingredient are stored in MongoDB (no SQL tables needed)
-- - menu_items collection in MongoDB
-- - Ingredient is an embedded document within MenuItem
-- ============================================================================

CREATE TABLE IF NOT EXISTS kitchen_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kitchen_order_id VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_kitchen_order_order
    FOREIGN KEY (order_id)
    REFERENCES orders(order_id)
                                                            ON DELETE CASCADE,

    UNIQUE KEY uk_kitchen_order_order (order_id),

    INDEX idx_kitchen_order_id (kitchen_order_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_kitchen_status_created (status, created_at)
);

CREATE TABLE IF NOT EXISTS receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    receipt_number VARCHAR(255) NOT NULL UNIQUE,

    order_id VARCHAR(255) NOT NULL,
    transaction_id VARCHAR(255) NOT NULL,

    subtotal DECIMAL(10,2) NOT NULL,
    vat DECIMAL(10,2) NOT NULL,
    service_charge DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total DECIMAL(10,2) NOT NULL CHECK (total >= 0),

    currency VARCHAR(10) DEFAULT 'KES',

    vat_number VARCHAR(100),
    business_name VARCHAR(255),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_receipt_order
    FOREIGN KEY (order_id)
    REFERENCES orders(order_id)
    ON DELETE CASCADE,

    CONSTRAINT fk_receipt_transaction
    FOREIGN KEY (transaction_id)
    REFERENCES payment_transactions(transaction_id)
    ON DELETE CASCADE,

    INDEX idx_receipt_order (order_id),
    INDEX idx_receipt_transaction (transaction_id),
    INDEX idx_receipt_created (created_at)
    );