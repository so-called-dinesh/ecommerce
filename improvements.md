----> on 2026-08-20
Critical bugs (app-level)
1. DELETE does nothing — ProductController.deleteProduct calls getProductById(id) twice but never calls productService.deleteProduct(id) (ProductController.java:59-65). The endpoint validates existence and returns "deleted" without deleting.
2. UPDATE ignores the path id — updateProduct(@PathVariable int id, ...) never uses id; it saves whatever id is inside the @RequestPart body (ProductController.java:47-57). PUT /product/5 with a body whose id is 10 updates product 10. Also image is mandatory, so a metadata-only update NPEs in addOrUpdateProduct (ProductService.java:27-32), which unconditionally calls image.getOriginalFilename()/getBytes().
3. GET /order/getAllOrders crashes — getAllOrdersResponses is not @Transactional (OrderService.java:94), so lazy-loading order.getOrderItems() outside the session throws LazyInitializationException.
4. Stack overflow from Lombok @Data — Order and OrderItem are both @Data with a bidirectional @OneToMany/@ManyToOne cycle (Order.java:32, OrderItem.java:30). toString()/hashCode() recurse infinitely; any logging of an order/order item blows the stack. Need @ToString.Exclude/@EqualsAndHashCode.Exclude.
5. Possible NPEs in placeOrder — product.getProductAvailable() and product.getStockQuantity() are nullable (Product.java:43,48) and get unboxed without checks (OrderService.java:49,52). A product created without those fields 500s the order endpoint.
6. OrderItem.id is primitive int with @NotNull + IDENTITY (OrderItem.java:20-21) — @NotNull is meaningless on a primitive and it disables JDBC batch inserts; use Long.
Weaker issues
- Unused imports (Product, Order imports org.springframework.cglib.core.Local, OrderItemRequest imports PostMapping, OrderController imports OrderItem, OrderedSetType).
- Duplicate DB hit in getProductById (ProductController.java:31-32); double existence check in delete.
- searchProducts uses :keyword without @Param (ProductRepo.java:18) — works only because the Spring Boot parent enables -parameters; add @Param("keyword") to be safe.
- orderId collision — 8-char UUID suffix on a @Column(unique=true) (Order.java:21, OrderService.java:36) can collide; no DataIntegrityViolationException handler → generic 500.
- Generic exception handler leaks ex.getMessage() and returns it to clients (GlobalExceptionHandler.java:57); no handlers for HttpMessageNotReadableException, MissingServletRequestPartException (missing image), ConstraintViolationException, or MethodArgumentTypeMismatchException (bad path id).
- getAllProducts/getProductById serialize byte[] imageData as base64 into JSON (Product.java:52) — heavy payloads.
- Inconsistent CORS — origins = "*" on ProductController vs. wide-open @CrossOrigin on OrderController (OrderController.java:19).
- No API tests beyond contextLoads; no pagination; no order status/cancel/lookup-by-id endpoints.
- Misc: empty <name/>/<description/>/<licenses> in pom.xml; leading space in " updated" response; spring-boot-starter-security-test included while security itself is commented out.
Suggested improvements (priority order)
1. Fix the delete/update/id-handling bugs and the non-transactional order listing (items 1–3).
2. Add @ToString.Exclude/@EqualsAndHashCode.Exclude on the cyclic relationships; make OrderItem.id a Long.
3. Enable .env support (uncomment spring-dotenv) or ship a docker-compose.yml for Postgres, and switch the test to H2/Testcontainers so tests run without external DB.
4. Null-guard image/stock/availability in the services; return 204/proper messages instead of NPEs.
5. Add handlers for the common binding/constraint exceptions and stop leaking getMessage() to clients.
6. Add controller/service integration tests (e.g., MockMvc + H2) covering order placement, stock decrement, and product CRUD.


