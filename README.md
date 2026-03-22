# order-flow

A reactive, event-driven order management system for restaurants. It handles the full lifecycle of an order — from menu selection to kitchen preparation — using Spring WebFlux and Kafka for asynchronous orchestration.

---

#  System Overview

`Customer → Menu → Order → Inventory → Payment → Kitchen → Completion`


## Flow is **event-driven**, not request-chained:

1. Order created
2. Inventory reserved
3. Payment processed
4. Kitchen prepares order
5. Order completed

Each step communicates via Kafka events.

---

#  Technology Stack

- **Java** 21
- **Spring Boot** 4.0.3
    - WebFlux (reactive APIs)
    - Security (role-based access)
    - Data R2DBC (MySQL)
    - Data MongoDB
    - Kafka
- **Kafka** (event backbone)
- **MongoDB** (menu + flexible data)
- **MySQL** (orders, transactions)
- **Docker Compose** (local infra)
- **Swagger / OpenAPI**

---

#  Core Services (Modules)

Each module is isolated but runs in the same codebase (modular monolith).

---

##  Menu Service

**Purpose**: Manage and expose menu items.

### Features
- List menu items
- Pagination
- Category + tag filtering
- Availability toggle
- Recipe definition (ingredient mapping)

### Data (MongoDB)
```json
{
  "productId": "uuid",
  "name": "Cappuccino",
  "price": 3.5,
  "category": "DRINKS",
  "tags": ["HOT", "FAST_MOVING"],
  "available": true,
  "imageUrl": "...",
  "recipe": [
    { "ingredientId": "milk", "quantity": 1 }
  ]
}
```
---

## Order Service

Purpose: Owns order lifecycle.

### Responsibilities
* Create order
* Store order items (snapshot from menu)
* Manage state transitions

#### States

`CREATED → PENDING_PAYMENT → CONFIRMED → IN_KITCHEN → READY → COMPLETED ↘ FAILED / CANCELLED`

#### Notes
* Uses state machine validation
* Emits order.created event

---

## Inventory Service

Purpose: Manage ingredient stock.

### Responsibilities
* Reserve stock on order creation
* Confirm stock after payment
* Release stock on failure or timeout

#### Reservation Lifecycle
`RESERVED → CONFIRMED → RELEASED`

#### Notes
* Expiry handled via scheduled job
* Prevents stock locking

---

## Payment Service

Purpose: Simulate or process payments.

### Flow
* Consumes inventory.reserved
* Determines success/failure
* Emits:
   * payment.completed
   * payment.failed
### Rules
* Must be idempotent
* Must emit only one outcome

---

## Kitchen Service

Purpose: Track food preparation.

## Flow
* Triggered by payment.completed
* Creates kitchen order
* Updates status:
`RECEIVED → IN_PROGRESS → READY`

---

## Receipt Service

Purpose

* Generates receipt after successful payment
---

## Report Service (optional)

### Purpose

* Aggregations
* Sales metrics
* Reporting

---

## Event-Driven Architecture
### Kafka Topics

```
order.created
inventory.reserved
inventory.failed
payment.completed
payment.failed
kitchen.order-ready
```
### Example flow

```aiignore
1. order.created
2. → inventory reserves stock
3. → inventory.reserved
4. → payment processes
5. → payment.completed
6. → kitchen receives order
7. → order confirmed
```
---
## Security
* Basic authentication (extendable to JWT)
* Role-based access:

```aiignore
ADMIN → manage menu
CUSTOMER → place orders
KITCHEN → kitchen operations
```
---
## Pagination Standard

### All paginated APIs return:

```json
{
"data": [],
"pageNumber": 0,
"pageSize": 10,
"totalCount": 100,
"totalPages": 10,
"hasMore": true
}
```
---
## System Guarantees
### Idempotency

Kafka is at-least-once delivery.

System uses:

`processed_event table`

`eventId → stored → duplicates skipped`

### Inventory Safety
* No deduction without reservation
* No confirmation without payment
* Auto-release on failure/timeout

### Order Consistency
* State transitions validated
* Invalid transitions rejected
---
## Scheduled Jobs
### Inventory Cleanup

* Runs every 1 minute
* Releases expired reservations

### Order Cleanup
* Runs every 1 minute
* Cancels unpaid orders
---

## Running Locally
```bash
docker-compose up -d
```

### Services:

* Kafka
* Kafka UI → http://localhost:8085
* MongoDB
* MySQL

## Run app:

```bash
./mvnw spring-boot:run
```

---
## Observability

Log format:

`CorrelationId | Module | Process | Duration | Message`

Example:

`abc-123 | ORDER | CREATE_ORDER | 120ms | Process completed`

---

## Production-Ready Features
* Event-driven architecture
* Reactive APIs
* Inventory reservation model
* Pagination + filtering
* Correlation tracing

## Known Gaps
* Idempotency persistence verification
* Payment integration (currently simulated)
* Kafka retry + DLQ
* WebSocket (real-time updates)
* Image storage (MinIO)
---

## Design Principles
* Non-blocking everywhere
* Event-driven over direct calls
* Data consistency via events
* Fail fast, recover via events