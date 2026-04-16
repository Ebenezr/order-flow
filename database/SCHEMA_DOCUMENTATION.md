# ORDER FLOW DATABASE SCHEMA DOCUMENTATION

## Overview
This document describes the complete database schema for the Order Flow application, including:
- 7 SQL tables (MySQL/PostgreSQL)
- 1 MongoDB collection (menu_items)

---

## SQL TABLES (Relational Database)

### 1. ORDERS Table
**Description**: Main orders table storing all customer orders

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| order_id | VARCHAR(255) | NOT NULL, UNIQUE | Business order identifier |
| customer_id | VARCHAR(255) | NOT NULL | Customer reference |
| status | VARCHAR(50) | NOT NULL | Order status (PENDING, COMPLETED, CANCELLED) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last updated timestamp |
| subtotal | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | Subtotal amount |
| vat | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | VAT amount |
| service_charge | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | Service charge |
| discount | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | Discount amount |
| total_amount | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | Total order amount |
| cancellation_reason | VARCHAR(500) | NULL | Reason for cancellation |

**Indexes**: order_id, customer_id, status, created_at, (status, created_at)

---

### 2. ORDER_ITEMS Table
**Description**: Line items for each order

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| order_id | VARCHAR(255) | NOT NULL, FK | References orders(order_id) |
| product_id | VARCHAR(255) | NOT NULL | Product reference |
| product_name | VARCHAR(255) | NOT NULL | Product name snapshot |
| quantity | INT | NOT NULL | Item quantity |
| price | DECIMAL(19,2) | NOT NULL | Unit price |
| category | VARCHAR(100) | NULL | Product category |
| product_snapshot | LONGTEXT | NULL | JSON snapshot of product |

**Indexes**: order_id, product_id, (order_id, product_id)
**Foreign Keys**: order_id -> orders(order_id) ON DELETE CASCADE

---

### 3. PAYMENT_TRANSACTIONS Table
**Description**: Payment transaction records

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| transaction_id | VARCHAR(255) | NOT NULL, UNIQUE | Payment transaction ID |
| order_id | VARCHAR(255) | NOT NULL, FK | References orders(order_id) |
| amount | DECIMAL(19,2) | NOT NULL | Transaction amount |
| status | VARCHAR(50) | NOT NULL | Payment status (COMPLETED, PENDING, FAILED) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last updated timestamp |

**Indexes**: transaction_id, order_id, status, created_at, (order_id, status)
**Foreign Keys**: order_id -> orders(order_id) ON DELETE CASCADE

---

### 4. RECEIPTS Table
**Description**: Receipt records for completed transactions

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| receipt_number | VARCHAR(255) | NOT NULL, UNIQUE | Receipt number |
| order_id | VARCHAR(255) | NOT NULL, FK | References orders(order_id) |
| transaction_id | VARCHAR(255) | NULL, FK | References payment_transactions(transaction_id) |
| subtotal | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | Subtotal |
| vat | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | VAT amount |
| service_charge | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | Service charge |
| discount | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | Discount |
| total | DECIMAL(19,2) | NOT NULL, DEFAULT 0 | Total amount |
| vat_number | VARCHAR(50) | NULL | VAT registration number |
| business_name | VARCHAR(255) | NULL | Business name |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Indexes**: receipt_number, order_id, transaction_id, created_at
**Foreign Keys**: 
- order_id -> orders(order_id) ON DELETE CASCADE
- transaction_id -> payment_transactions(transaction_id) ON DELETE SET NULL

---

### 5. KITCHEN_ORDERS Table
**Description**: Kitchen order tracking

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| kitchen_order_id | VARCHAR(255) | NOT NULL, UNIQUE | Kitchen order ID |
| order_id | VARCHAR(255) | NOT NULL, FK | References orders(order_id) |
| status | VARCHAR(50) | NOT NULL | Kitchen status (NEW, PREPARING, READY, COMPLETED) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last updated timestamp |

**Indexes**: kitchen_order_id, order_id, status, created_at, (order_id, status)
**Foreign Keys**: order_id -> orders(order_id) ON DELETE CASCADE

---

### 6. INVENTORY Table
**Description**: Ingredient inventory management

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| ingredient_id | VARCHAR(255) | NOT NULL, UNIQUE | Ingredient identifier |
| ingredient_name | VARCHAR(255) | NOT NULL | Ingredient name |
| category | VARCHAR(100) | NULL | Ingredient category |
| available_quantity | INT | NOT NULL, DEFAULT 0 | Available quantity |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last updated timestamp |

**Indexes**: ingredient_id, category, updated_at

---

### 7. INVENTORY_RESERVATIONS Table
**Description**: Inventory reservation tracking for orders

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| id | BIGINT | PK, AUTO_INCREMENT | Primary key |
| reservation_id | VARCHAR(255) | NOT NULL, UNIQUE | Reservation ID |
| order_id | VARCHAR(255) | NOT NULL, FK | References orders(order_id) |
| ingredient_id | VARCHAR(255) | NOT NULL, FK | References inventory(ingredient_id) |
| quantity | INT | NOT NULL | Reserved quantity |
| status | VARCHAR(50) | NOT NULL | Reservation status (CONFIRMED, EXPIRED, CANCELLED) |
| created_at | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| expires_at | TIMESTAMP | NULL | Reservation expiration time |

**Indexes**: reservation_id, order_id, ingredient_id, status, created_at, expires_at, (order_id, status)
**Foreign Keys**:
- order_id -> orders(order_id) ON DELETE CASCADE
- ingredient_id -> inventory(ingredient_id) ON DELETE CASCADE

---

## MONGODB COLLECTIONS

### 1. MENU_ITEMS Collection
**Description**: Menu items with embedded ingredients (MongoDB)

**Fields**:
- `_id`: ObjectId (MongoDB Primary Key)
- `productId`: String (UNIQUE) - Menu item identifier
- `name`: String - Menu item name
- `price`: Double - Item price
- `available`: Boolean - Availability status
- `imageUrl`: String - Product image URL
- `category`: String - Category classification
- `tags`: Array of Strings - Search tags
- `description`: String - Detailed description
- `recipe`: Array of Objects (Embedded) - Ingredients
  - `ingredientId`: String - Reference to inventory ingredient
  - `quantity`: Integer - Quantity needed

**Indexes**:
- productId (unique)
- category
- available
- (category, available)
- Full-text index on (name, description)

**Note**: Ingredient is NOT a separate collection; it's embedded within MenuItem documents

---

## RELATIONAL DIAGRAM

```
orders (main entity)
  ├── order_items (1:N)
  ├── payment_transactions (1:N)
  ├── receipts (1:N)
  ├── kitchen_orders (1:N)
  └── inventory_reservations (1:N)

inventory
  └── inventory_reservations (1:N)

payment_transactions
  └── receipts (0:N)
```

---

## KEY RELATIONSHIPS

1. **Order to Order Items**: One order can have multiple line items (1:N)
2. **Order to Payment**: One order can have multiple payment transactions (1:N)
3. **Order to Receipt**: One order typically has one receipt (1:1 or 1:0)
4. **Order to Kitchen Order**: One order generates one kitchen order (1:1)
5. **Order to Inventory Reservations**: One order reserves multiple ingredients (1:N)
6. **Inventory to Reservations**: One ingredient can have multiple reservations (1:N)

---

## SQL FILE LOCATIONS

- **Schema Creation**: `/database/schema.sql`
- **Initial Data**: `/database/initial_data.sql`
- **MongoDB Schema**: See documentation above

---

## NOTES

1. All timestamps use UTC format
2. Monetary values use DECIMAL(19,2) for precision
3. Foreign key constraints use CASCADE DELETE where appropriate
4. All id columns are auto-incrementing BIGINT for scalability
5. MenuItem and Ingredient are stored in MongoDB (flexible schema for recipe)
6. All other entities use traditional SQL with ACID compliance
7. Indexes are created for common query patterns and foreign keys

