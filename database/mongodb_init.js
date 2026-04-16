// ============================================================================
// MONGODB INITIALIZATION SCRIPT
// Order Flow Application - Menu Items Collection
// ============================================================================

// Drop collection if it exists (for fresh start)
// db.menu_items.drop();

// Create indexes for menu_items collection
db.menu_items.createIndex({ "productId": 1 }, { unique: true });
db.menu_items.createIndex({ "category": 1 });
db.menu_items.createIndex({ "available": 1 });
db.menu_items.createIndex({ "category": 1, "available": 1 });
db.menu_items.createIndex({ "name": "text", "description": "text" });

// ============================================================================
// INSERT SAMPLE MENU ITEMS
// ============================================================================

// Burgers
db.menu_items.insertMany([
    {
        "productId": "PROD001",
        "name": "Classic Burger",
        "price": 45.00,
        "available": true,
        "imageUrl": "https://example.com/images/classic-burger.jpg",
        "category": "Main Courses",
        "tags": ["burger", "beef"],
        "description": "Classic beef burger with lettuce, tomato, and pickles",
        "recipe": [
            { "ingredientId": "ING005", "quantity": 1 },
            { "ingredientId": "ING002", "quantity": 1 },
            { "ingredientId": "ING001", "quantity": 1 },
            { "ingredientId": "ING007", "quantity": 2 },
            { "ingredientId": "ING012", "quantity": 1 }
        ]
    },
    {
        "productId": "PROD002",
        "name": "Cheeseburger",
        "price": 50.00,
        "available": true,
        "imageUrl": "https://example.com/images/cheeseburger.jpg",
        "category": "Main Courses",
        "tags": ["burger", "beef", "cheese"],
        "description": "Beef burger with melted cheese, lettuce, tomato, and pickles",
        "recipe": [
            { "ingredientId": "ING005", "quantity": 1 },
            { "ingredientId": "ING003", "quantity": 2 },
            { "ingredientId": "ING002", "quantity": 1 },
            { "ingredientId": "ING001", "quantity": 1 },
            { "ingredientId": "ING012", "quantity": 1 }
        ]
    },
    {
        "productId": "PROD003",
        "name": "Bacon Cheeseburger",
        "price": 55.00,
        "available": true,
        "imageUrl": "https://example.com/images/bacon-cheeseburger.jpg",
        "category": "Main Courses",
        "tags": ["burger", "beef", "cheese", "bacon"],
        "description": "Beef burger with crispy bacon, melted cheese, lettuce, tomato",
        "recipe": [
            { "ingredientId": "ING005", "quantity": 1 },
            { "ingredientId": "ING003", "quantity": 2 },
            { "ingredientId": "ING010", "quantity": 2 },
            { "ingredientId": "ING002", "quantity": 1 },
            { "ingredientId": "ING001", "quantity": 1 },
            { "ingredientId": "ING012", "quantity": 1 }
        ]
    },
    {
        "productId": "PROD004",
        "name": "Chicken Sandwich",
        "price": 45.00,
        "available": true,
        "imageUrl": "https://example.com/images/chicken-sandwich.jpg",
        "category": "Main Courses",
        "tags": ["sandwich", "chicken"],
        "description": "Grilled chicken breast with lettuce, tomato, and mayonnaise",
        "recipe": [
            { "ingredientId": "ING004", "quantity": 1 },
            { "ingredientId": "ING002", "quantity": 1 },
            { "ingredientId": "ING001", "quantity": 1 },
            { "ingredientId": "ING008", "quantity": 1 },
            { "ingredientId": "ING012", "quantity": 1 }
        ]
    }
]);

// Salads
db.menu_items.insertMany([
    {
        "productId": "PROD005",
        "name": "Caesar Salad",
        "price": 35.00,
        "available": true,
        "imageUrl": "https://example.com/images/caesar-salad.jpg",
        "category": "Salads",
        "tags": ["salad", "vegetarian"],
        "description": "Fresh lettuce with parmesan cheese and Caesar dressing",
        "recipe": [
            { "ingredientId": "ING002", "quantity": 2 },
            { "ingredientId": "ING003", "quantity": 1 }
        ]
    },
    {
        "productId": "PROD006",
        "name": "Garden Salad",
        "price": 30.00,
        "available": true,
        "imageUrl": "https://example.com/images/garden-salad.jpg",
        "category": "Salads",
        "tags": ["salad", "vegetarian", "fresh"],
        "description": "Mixed greens with tomatoes, onions, and house dressing",
        "recipe": [
            { "ingredientId": "ING002", "quantity": 2 },
            { "ingredientId": "ING001", "quantity": 1 },
            { "ingredientId": "ING006", "quantity": 1 }
        ]
    }
]);

// Sides
db.menu_items.insertMany([
    {
        "productId": "PROD007",
        "name": "French Fries",
        "price": 15.00,
        "available": true,
        "imageUrl": "https://example.com/images/fries.jpg",
        "category": "Sides",
        "tags": ["fries", "sides"],
        "description": "Crispy golden french fries",
        "recipe": [
            { "ingredientId": "ING014", "quantity": 3 },
            { "ingredientId": "ING015", "quantity": 1 }
        ]
    },
    {
        "productId": "PROD008",
        "name": "Rice Pilaf",
        "price": 12.00,
        "available": true,
        "imageUrl": "https://example.com/images/rice-pilaf.jpg",
        "category": "Sides",
        "tags": ["rice", "sides"],
        "description": "Fluffy rice with seasonings",
        "recipe": [
            { "ingredientId": "ING013", "quantity": 1 }
        ]
    }
]);

// Verify data insertion
print("Total menu items inserted:", db.menu_items.countDocuments());
print("Menu items by category:");
db.menu_items.aggregate([
    { $group: { _id: "$category", count: { $sum: 1 } } }
]).forEach(function(doc) { print(doc._id + ": " + doc.count); });

