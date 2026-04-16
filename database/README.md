# Database Setup Guide - Order Flow Application

This directory contains SQL and MongoDB scripts for setting up the Order Flow application database.

## Files Overview

### 1. **schema.sql**
Main SQL schema definition file containing:
- All 7 relational database tables
- Foreign key relationships
- Indexes for query optimization
- Column definitions with constraints

**Tables included:**
- `orders` - Main order records
- `order_items` - Order line items
- `payment_transactions` - Payment records
- `receipts` - Receipt records
- `kitchen_orders` - Kitchen order tracking
- `inventory` - Ingredient inventory
- `inventory_reservations` - Inventory reservation tracking

### 2. **initial_data.sql**
Sample data initialization script with:
- 15 inventory items (ingredients)
- Optional sample orders, payments, receipts, and reservations
- Comments for easy management

### 3. **mongodb_init.js**
MongoDB collection initialization with:
- Index creation for optimal query performance
- 8 sample menu items across multiple categories
- Embedded ingredient definitions within each menu item

### 4. **SCHEMA_DOCUMENTATION.md**
Comprehensive documentation including:
- Detailed field descriptions for each table
- Data types and constraints
- Relationship diagrams
- Index information
- MongoDB collection structure

---

## Setup Instructions

### Prerequisites
- MySQL 5.7+ or PostgreSQL 10+ (for SQL tables)
- MongoDB 3.6+ (for menu items)
- Database client access

### SQL Database Setup (MySQL/PostgreSQL)

#### Method 1: Using Command Line

**MySQL:**
```bash
# Option A: Direct execution
mysql -u <username> -p <database_name> < /Users/ebukosia/Developer/order-flow/database/schema.sql
mysql -u <username> -p <database_name> < /Users/ebukosia/Developer/order-flow/database/initial_data.sql

# Option B: Interactive
mysql -u <username> -p
CREATE DATABASE order_flow;
USE order_flow;
SOURCE /Users/ebukosia/Developer/order-flow/database/schema.sql;
SOURCE /Users/ebukosia/Developer/order-flow/database/initial_data.sql;
```

**PostgreSQL:**
```bash
# Create database
createdb -U <username> order_flow

# Execute scripts
psql -U <username> -d order_flow -f /Users/ebukosia/Developer/order-flow/database/schema.sql
psql -U <username> -d order_flow -f /Users/ebukosia/Developer/order-flow/database/initial_data.sql
```

#### Method 2: Using GUI Tools
1. Open your database client (MySQL Workbench, pgAdmin, DBeaver, etc.)
2. Create a new database: `order_flow`
3. Open and execute `schema.sql`
4. Open and execute `initial_data.sql`

### MongoDB Setup

#### Method 1: Using MongoDB Shell

```bash
# Connect to MongoDB
mongosh

# Switch to order_flow database
use order_flow

# Run initialization script
load('/Users/ebukosia/Developer/order-flow/database/mongodb_init.js')
```

#### Method 2: Using MongoDB Compass
1. Connect to your MongoDB instance
2. Create database: `order_flow`
3. Open MongoDB Shell in Compass
4. Paste the contents of `mongodb_init.js` and execute

#### Method 3: Using Docker (if MongoDB is containerized)
```bash
# Copy script into container
docker cp database/mongodb_init.js <container_id>:/tmp/

# Execute script
docker exec <container_id> mongosh order_flow /tmp/mongodb_init.js
```

---

## Verification

### SQL Verification
```sql
-- Check all tables were created
SHOW TABLES;

-- Verify row counts
SELECT 'orders' as table_name, COUNT(*) as row_count FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL
SELECT 'payment_transactions', COUNT(*) FROM payment_transactions
UNION ALL
SELECT 'receipts', COUNT(*) FROM receipts
UNION ALL
SELECT 'kitchen_orders', COUNT(*) FROM kitchen_orders
UNION ALL
SELECT 'inventory', COUNT(*) FROM inventory
UNION ALL
SELECT 'inventory_reservations', COUNT(*) FROM inventory_reservations;

-- Verify inventory items
SELECT * FROM inventory;
```

### MongoDB Verification
```javascript
// Check menu items collection
db.menu_items.countDocuments();

// List all menu items
db.menu_items.find().pretty();

// Verify indexes
db.menu_items.getIndexes();

// Check menu items by category
db.menu_items.aggregate([
    { $group: { _id: "$category", count: { $sum: 1 } } }
]);
```

---

## Data Structure Summary

### SQL Tables (7 tables)
| Table | Purpose | Records |
|-------|---------|---------|
| orders | Main order records | - |
| order_items | Order line items | - |
| payment_transactions | Payment tracking | - |
| receipts | Receipt records | - |
| kitchen_orders | Kitchen orders | - |
| inventory | Ingredient stock | 15 initial items |
| inventory_reservations | Reservation tracking | - |

### MongoDB Collection (1 collection)
| Collection | Purpose | Documents |
|-----------|---------|-----------|
| menu_items | Menu items with recipes | 8 initial items |

---

## Key Features

### Foreign Keys & Relationships
- All order-related tables reference `orders.order_id`
- `inventory_reservations` references both `orders` and `inventory`
- `receipts` can reference both `orders` and `payment_transactions`
- CASCADE DELETE enabled for data integrity

### Indexes
- All primary and foreign keys indexed
- Composite indexes for common query patterns
- Full-text index on MongoDB menu items for search
- Status and timestamp indexes for filtering

### Data Types
- Monetary values: `DECIMAL(19,2)` for precision
- IDs: `BIGINT` auto-increment for scalability
- Timestamps: `TIMESTAMP` with automatic management
- Large text: `LONGTEXT` for JSON snapshots

---

## Common Operations

### Insert a New Menu Item (MongoDB)
```javascript
db.menu_items.insertOne({
    "productId": "PROD009",
    "name": "Grilled Cheese",
    "price": 25.00,
    "available": true,
    "imageUrl": "https://example.com/grilled-cheese.jpg",
    "category": "Sandwiches",
    "tags": ["sandwich", "cheese"],
    "description": "Melted cheese between toasted bread",
    "recipe": [
        { "ingredientId": "ING003", "quantity": 2 },
        { "ingredientId": "ING012", "quantity": 2 }
    ]
});
```

### Query Orders by Status (SQL)
```sql
SELECT * FROM orders WHERE status = 'COMPLETED' ORDER BY created_at DESC;
```

### Check Inventory Levels (SQL)
```sql
SELECT ingredient_name, available_quantity 
FROM inventory 
WHERE available_quantity < 20 
ORDER BY available_quantity ASC;
```

---

## Troubleshooting

### MySQL Issues
- **Connection refused**: Check if MySQL service is running (`mysql.server start`)
- **Access denied**: Verify username/password credentials
- **Database doesn't exist**: Create it with `CREATE DATABASE order_flow;`

### MongoDB Issues
- **Connection refused**: Check if MongoDB service is running (`brew services start mongodb-community`)
- **Collection not found**: Verify `use order_flow` command was executed
- **Duplicate key error**: Drop collection and re-run: `db.menu_items.drop();`

### Common Errors & Fixes
- **Foreign Key Constraint Error**: Ensure parent records exist before inserting child records
- **Duplicate Key Error**: Check UNIQUE constraints and remove conflicting data
- **Column too long**: For large text, use LONGTEXT instead of VARCHAR

---

## Docker Integration (Optional)

### Using Docker Compose for Complete Setup
```yaml
# Add to docker-compose.yml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: order_flow
      MYSQL_ROOT_PASSWORD: root
    ports:
      - "3306:3306"
    volumes:
      - ./database/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
      - ./database/initial_data.sql:/docker-entrypoint-initdb.d/02-data.sql

  mongodb:
    image: mongo:5.0
    ports:
      - "27017:27017"
    volumes:
      - ./database/mongodb_init.js:/docker-entrypoint-initdb.d/init.js
```

---

## Database Backup & Recovery

### MySQL Backup
```bash
# Full database backup
mysqldump -u <username> -p <database_name> > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore from backup
mysql -u <username> -p <database_name> < backup_20260415_120000.sql
```

### MongoDB Backup
```bash
# Export collection
mongoexport --db order_flow --collection menu_items --out menu_items_backup.json

# Import collection
mongoimport --db order_flow --collection menu_items --file menu_items_backup.json
```

---

## Support & Documentation

- **SQL Documentation**: See `SCHEMA_DOCUMENTATION.md`
- **Application Config**: Check `application.properties`
- **Entity Classes**: See `/src/main/java/com/blind/orderflow/*/entity/`

---

## Notes

1. All timestamps are in UTC
2. SQL uses auto-increment for IDs (highly scalable)
3. MongoDB uses ObjectId for automatic ID generation
4. Monetary values use DECIMAL for precision (no floating-point errors)
5. Foreign key constraints ensure referential integrity
6. Indexes are optimized for common query patterns


