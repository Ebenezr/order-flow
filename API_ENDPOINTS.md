# Order Flow — API Endpoints Documentation

All responses are wrapped in:

```json
{
  "header": {
    "requestRefId": "string",
    "responseCode": 0,
    "responseMessage": "string",
    "customerMessage": "string",
    "timestamp": "2024-01-01T00:00:00"
  },
  "body": <T>
}
```

---

## 1. Order Controller

**Base path:** `/api/v1/orders`

### POST `/api/v1/orders` — Create Order

| | Details |
|---|---|
| **Query Params** | `customerId` (String, required) |
| **Headers** | `X-Correlation-Id` (String, required) |
| **Request Body** | `Flux<CreateOrderItemRequest>` — JSON array: |

```json
[
  { "productId": "string", "quantity": 0 }
]
```

**Response:** `ApiResponse<Order>`

```json
{
  "id": 0,
  "orderId": "string",
  "customerId": "string",
  "status": "PENDING | CONFIRMED | PREPARING | READY | COMPLETED | CANCELLED",
  "createdAt": "datetime",
  "updatedAt": "datetime",
  "subtotal": 0.00,
  "vat": 0.00,
  "serviceCharge": 0.00,
  "discount": 0.00,
  "totalAmount": 0.00,
  "cancellationReason": "string"
}
```

---

### GET `/api/v1/orders/{orderId}` — Get Order

| | Details |
|---|---|
| **Path Params** | `orderId` (String) |

**Response:** `ApiResponse<Order>` (same shape as above)

---

### GET `/api/v1/orders/{orderId}/items` — Get Order Items

| | Details |
|---|---|
| **Path Params** | `orderId` (String) |

**Response:** `ApiResponse<List<OrderItem>>`

```json
[
  {
    "id": 0,
    "orderId": "string",
    "productId": "string",
    "productName": "string",
    "quantity": 0,
    "price": 0.00,
    "category": "string",
    "productSnapshot": "string (JSON)"
  }
]
```

---

### POST `/api/v1/orders/{orderId}/complete` — Complete Order

| | Details |
|---|---|
| **Path Params** | `orderId` (String) |
| **Request Body** | None |

**Response:** `ApiResponse<Order>`

---

### POST `/api/v1/orders/{orderId}/cancel` — Cancel Order

| | Details |
|---|---|
| **Path Params** | `orderId` (String) |
| **Headers** | `X-Correlation-Id` (String, required) |
| **Request Body** | `String` — cancellation reason (plain text) |

**Response:** `ApiResponse<Order>`

---

## 2. Menu Query Controller

**Base path:** `/api/v1/menu`

### GET `/api/v1/menu` — Get Menu (Paged)

| | Details |
|---|---|
| **Query Params** | `currentPage` (int, default 0), `pageSize` (int, default 10), `category` (String, optional), `tag` (String, optional), `available` (Boolean, default true) |

**Response:** `ApiResponse<?>` — paginated menu items

---

### GET `/api/v1/menu/{productId}` — Get Single Menu Item

| | Details |
|---|---|
| **Path Params** | `productId` (String) |

**Response:** `ApiResponse<MenuItem>`

```json
{
  "id": "string",
  "productId": "string",
  "name": "string",
  "price": 0.0,
  "available": true,
  "imageUrl": "string",
  "category": "string",
  "tags": ["string"],
  "description": "string",
  "recipe": [{ /* Ingredient */ }]
}
```

---

### GET `/api/v1/menu/grouped` — Get Menu Grouped by Category

**Response:** `ApiResponse<?>` — menu items grouped by category

---

### GET `/api/v1/menu/grouped/tag` — Get Menu Grouped by Tag

**Response:** `ApiResponse<?>` — menu items grouped by tag

---

## 3. Menu Admin Controller

**Base path:** `/api/v1/admin/menu`

### POST `/api/v1/admin/menu/items` — Create Menu Item

| | Details |
|---|---|
| **Request Body** | `MenuItem` |

```json
{
  "productId": "string",
  "name": "string",
  "price": 0.0,
  "available": true,
  "imageUrl": "string",
  "category": "string",
  "tags": ["string"],
  "description": "string",
  "recipe": [{ /* Ingredient */ }]
}
```

**Response:** `ApiResponse<MenuItem>`

---

### PUT `/api/v1/admin/menu/items/{productId}` — Update Menu Item

| | Details |
|---|---|
| **Path Params** | `productId` (String) |
| **Request Body** | `MenuItem` (same as create) |

**Response:** `ApiResponse<MenuItem>`

---

### PUT `/api/v1/admin/menu/items/{productId}/availability` — Set Item Availability

| | Details |
|---|---|
| **Path Params** | `productId` (String) |
| **Query Params** | `available` (boolean, required) |

**Response:** `ApiResponse<MenuItem>`

---

## 4. Kitchen Controller

**Base path:** `/api/v1/kitchen`

### GET `/api/v1/kitchen/orders` — Get Kitchen Orders

| | Details |
|---|---|
| **Query Params** | `currentPage` (int, default 0), `pageSize` (int, default 10), `status` (String, optional) |

**Response:** `ApiResponse<?>` — paginated kitchen orders

---

### PUT `/api/v1/kitchen/orders/{orderId}/start` — Start Preparing

| | Details |
|---|---|
| **Path Params** | `orderId` (String) |

**Response:** `ApiResponse<?>` — updated kitchen order

---

### PUT `/api/v1/kitchen/orders/{orderId}/ready` — Mark Ready

| | Details |
|---|---|
| **Path Params** | `orderId` (String) |

**Response:** `ApiResponse<?>` — updated kitchen order

---

## 5. Report Controller

**Base path:** `/api/v1/reports`

All report endpoints accept an optional `date` query param (`yyyy-MM-dd`, defaults to today).

### GET `/api/v1/reports/daily-sales` — Daily Sales Report

| Query Params | `date` (LocalDate, optional) |
|---|---|

**Response:** `ApiResponse<DailySalesReport>`

```json
{
  "date": "2024-01-01",
  "totalOrders": 0,
  "totalRevenue": 0.0,
  "vatCollected": 0.0,
  "topSellingItems": [
    { "productId": "string", "productName": "string", "totalQuantity": 0, "totalRevenue": 0.0 }
  ]
}
```

---

### GET `/api/v1/reports/kitchen-performance` — Kitchen Performance Report

**Response:** `ApiResponse<KitchenPerformanceReport>`

```json
{
  "date": "2024-01-01",
  "totalKitchenOrders": 0,
  "avgPrepTimeMinutes": 0.0,
  "minPrepTimeMinutes": 0.0,
  "maxPrepTimeMinutes": 0.0,
  "slowItems": [
    { "productId": "string", "productName": "string", "orderId": "string", "prepTimeMinutes": 0 }
  ]
}
```

---

### GET `/api/v1/reports/payments` — Payment Report

**Response:** `ApiResponse<PaymentReport>`

```json
{
  "date": "2024-01-01",
  "successfulPayments": 0,
  "failedPayments": 0,
  "failureRate": 0.0
}
```

---

### GET `/api/v1/reports/cancellations` — Cancellation Report

**Response:** `ApiResponse<CancellationReport>`

```json
{
  "date": "2024-01-01",
  "cancelledOrders": 0,
  "reasons": { "reason_key": 0 }
}
```

---

### GET `/api/v1/reports/processing-times` — Processing Time Report

**Response:** `ApiResponse<ProcessingTimeReport>`

```json
{
  "date": "2024-01-01",
  "avgInventoryReserveTimeMs": 0.0,
  "avgPaymentProcessingMs": 0.0,
  "avgKitchenPrepMinutes": 0.0
}
```

---

### GET `/api/v1/reports/revenue-by-category` — Revenue by Category

**Response:** `ApiResponse<List<CategoryRevenue>>`

```json
[
  { "category": "string", "revenue": 0.0 }
]
```

---

### GET `/api/v1/reports/monthly-summary` — Monthly Summary Report

| Query Params | `year` (Integer, optional), `month` (Integer, optional) — defaults to current month |
|---|---|

**Response:** `ApiResponse<MonthlySummaryReport>`

```json
{
  "period": "string",
  "grossSales": 0.0,
  "netSales": 0.0,
  "vatCollected": 0.0
}
```

