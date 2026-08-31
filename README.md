# E-Commerce API

A Spring Boot RESTful e-commerce backend that manages **products** and **orders**. It supports image uploads, product search, stock validation at request time, and ships with interactive API documentation out of the box.

This is a learning/prototype project — see [Roadmap](#roadmap) for what is not yet built.

---

## Tech Stack

| Layer        | Technology                                            |
|--------------|-------------------------------------------------------|
| Language     | Java 21                                               |
| Framework    | Spring Boot 4.0.6                                     |
| Web Layer    | Spring MVC (`spring-boot-starter-webmvc`)             |
| Data Access  | Spring Data JPA / Hibernate                           |
| Database     | PostgreSQL                                            |
| Validation   | Jakarta Bean Validation                               |
| API Docs     | Springdoc OpenAPI (Swagger UI)                        |
| Build Tool   | Maven (wrapper included)                              |
| Utility      | Lombok, Spring Boot DevTools                          |

---

## Project Structure

```
ecommerce/
├── pom.xml                        # Maven build definition & dependencies
├── mvnw / mvnw.cmd                # Maven wrapper scripts
└── src/
    ├── main/
    │   ├── java/com/dinesh/ecommerce/
    │   │   ├── EcommerceApplication.java        # Spring Boot entry point
    │   │   ├── controllers/
    │   │   │   ├── ProductController.java       # Product REST endpoints
    │   │   │   ├── OrderController.java         # Order REST endpoints
    │   │   │   ├── TestController.java          # Smoke-test endpoint
    │   │   │   └── GlobalExceptionHandler.java  # Centralized exception handling
    │   │   ├── service/
    │   │   │   ├── ProductService.java          # Product business logic
    │   │   │   └── OrderService.java            # Order business logic
    │   │   ├── repo/
    │   │   │   ├── ProductRepo.java             # Product persistence
    │   │   │   └── OrderRepo.java               # Order persistence
    │   │   ├── model/
    │   │   │   ├── Product.java                 # Product entity
    │   │   │   ├── Order.java                   # Order entity
    │   │   │   ├── OrderItem.java               # Order line-item entity
    │   │   │   └── dto/                         # Request/response records
    │   │   └── Exceptions/                      # Domain-specific exceptions
    │   └── resources/
    │       └── application.properties           # Configuration (env-driven)
    └── test/
        └── java/com/dinesh/ecommerce/
            └── EcommerceApplicationTests.java   # Context-load test
```

---

## Data Model

| Entity      | Key Fields                                           | Relationships                      |
|-------------|------------------------------------------------------|------------------------------------|
| `Product`   | `name`, `price`, `stockQuantity`, `category`, image  | `1:N` to `OrderItem`               |
| `Order`     | `orderId` (unique), `customerName`, `email`, `status`| `1:N` to `OrderItem` (cascade all) |
| `OrderItem` | `quantity`, `totalPrice`                             | `N:1` to `Product`, `N:1` to `Order` |

- `Order.orderId` is a human-friendly unique business key (e.g. `ORD1A2B3C4D`).
- `OrderItem.totalPrice` is computed as `product.price * quantity` at order time, storing a snapshot of the agreed price.
- Stock is stored on `Product` and decremented when an order is placed.

---

## Order Placement

`POST /order/place` is wrapped in `@Transactional`. For each item in the request, the service:

1. Looks up the product (404 if missing)
2. Checks `productAvailable` and `stockQuantity` (409 if unavailable or insufficient)
3. Decrements stock and saves the product
4. Builds the order with line items and saves via cascade

#### Concurrency caveat

The stock check is a read-then-write inside `@Transactional` with default isolation. This is **not safe under concurrent requests** — two simultaneous requests can both read the same stock value, both see sufficient stock, and both decrement, resulting in overselling. The transaction guarantees atomicity of the commit (stock decrement + order creation succeed or roll back together), but does **not** prevent the race. See [Roadmap](#roadmap) for planned mitigations.

---

## Prerequisites

- **Java 21** or later
- **Maven 3.9+** (or use the bundled `mvnw` wrapper)
- **PostgreSQL** instance

---

## Configuration

Environment variables for the database connection:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/ecommerce"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
```

On Windows (PowerShell):

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/ecommerce"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your_password"
```

---

## Running the Application

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Build the jar and run tests:

```bash
mvn clean package
java -jar target/ecommerce-0.0.1-SNAPSHOT.jar
```

> The context-load test requires a reachable PostgreSQL instance with the environment variables above set.

---

## API Reference

Once running, interactive docs are available at:

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

### Health / Smoke Test

| Method | Endpoint  | Description            |
|--------|-----------|------------------------|
| GET    | `/test`   | Returns a text response |

### Products

| Method | Endpoint                    | Description                                  |
|--------|-----------------------------|----------------------------------------------|
| GET    | `/product/getAll`           | List all products                            |
| GET    | `/product/{id}`             | Get a product by ID                          |
| GET    | `/product/search?keyword=x` | Search name, description, brand, or category |
| POST   | `/product/add`              | Create a product (multipart)                 |
| PUT    | `/product/{id}`             | Update a product by ID (multipart)           |
| DELETE | `/product/{id}`             | Delete a product by ID                       |

#### Multipart Form (add/update)

| Part      | Type       | Required | Notes                      |
|-----------|------------|----------|----------------------------|
| `product` | JSON part  | Yes      | Product fields (validated) |
| `image`   | File part  | No       | Product image              |

Example `product` part:

```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse",
  "brand": "Logitech",
  "price": 1499.00,
  "category": "Electronics",
  "stockQuantity": 25,
  "releaseDate": "15-08-2026",
  "productAvailable": true
}
```

> `releaseDate` uses the `dd-MM-yyyy` format.

**cURL example — create a product:**

```bash
curl -X POST http://localhost:8080/product/add \
  -F 'product={"name":"Wireless Mouse","description":"Ergonomic wireless mouse","brand":"Logitech","price":1499.00,"category":"Electronics","stockQuantity":25,"productAvailable":true};type=application/json' \
  -F 'image=@mouse.jpg'
```

### Orders

| Method | Endpoint              | Description           |
|--------|-----------------------|-----------------------|
| POST   | `/order/place`        | Place an order        |
| GET    | `/order/getAllOrders` | List all orders       |

#### Place Order

```json
{
  "customerName": "Dinesh",
  "email": "dinesh@example.com",
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

**cURL example:**

```bash
curl -X POST http://localhost:8080/order/place \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Dinesh","email":"dinesh@example.com","items":[{"productId":1,"quantity":2}]}'
```

**Validation rules enforced:**

| Field          | Rule                                  |
|----------------|---------------------------------------|
| `customerName` | Not blank                             |
| `email`        | Not blank, valid email                |
| `items`        | At least one item                     |
| `productId`    | Positive                              |
| `quantity`     | At least 1                            |

Order placement also verifies the product exists, is available, and has enough stock at the time of the request — otherwise the transaction is rejected and rolled back. This check is **not race-condition-safe** (see [Concurrency caveat](#concurrency-caveat)).

---

## Error Handling

All errors are normalized by `GlobalExceptionHandler` into a consistent shape.

| HTTP Status | Scenario                                        |
|-------------|-------------------------------------------------|
| 400         | Bean-validation failure / bad request           |
| 404         | Resource not found (e.g., product, order item)  |
| 409         | Insufficient stock / duplicate resource         |
| 500         | Unexpected server error                         |

Standard error body:

```json
{
  "status": 404,
  "message": "Product not found with ID: 99",
  "timestamp": "2026-08-21T10:15:30"
}
```

Validation errors return a field-to-message map:

```json
{
  "email": "email must be valid",
  "items": "order must have atleast one item"
}
```

---

## Testing

Currently the project includes a single Spring context smoke test (`EcommerceApplicationTests.java`). It boots the full application context and verifies it loads without errors. There are no service-level, controller-level, or repository-level tests yet.

**Run tests:**

```bash
mvn test
```

> The context-load test needs PostgreSQL reachable with the environment variables configured.

---

## Deployment

The build produces a self-contained, executable jar.

```bash
mvn clean package
java -jar target/ecommerce-0.0.1-SNAPSHOT.jar
```

Example `Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/ecommerce-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV DB_URL=jdbc:postgresql://db:5432/ecommerce
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Roadmap

- **Stock safety**: use pessimistic locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`), an atomic `UPDATE ... SET stock_quantity = stock_quantity - ? WHERE stock_quantity >= ?`, or `SELECT ... FOR UPDATE` to prevent overselling under concurrent requests.
- **Security**: enable the (currently commented-out) Spring Security starter — add authn/authz, password hashing, and JWT session handling.
- **Migrations**: replace `ddl-auto=update` with Flyway/Liquibase and `ddl-auto=validate`.
- **Pagination & filtering**: add pageable queries to list endpoints; filter products by category and price range.
- **Order lifecycle**: order status transitions (cancel/ship/deliver), order lookup by `orderId`, and order cancellation with stock restore.
- **Image delivery**: serve images via a dedicated streaming endpoint instead of embedding base64 in JSON; add size/type limits.
- **Test coverage**: service/controller integration tests (MockMvc + H2/Testcontainers), plus repository tests.
- **Observability**: structured logging, metrics (Micrometer), and health/readiness probes.
- **CI/CD**: GitHub Actions pipeline for build, test, and container publish.
- **CORS**: scope `@CrossOrigin` to specific allowed origins instead of `*`.

---

## License

Proprietary / All rights reserved.
