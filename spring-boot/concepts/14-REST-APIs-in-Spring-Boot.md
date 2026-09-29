# Topic 14 — REST APIs in Spring Boot: HTTP Methods, DTOs, `ResponseEntity`, Validation, Status Codes, Idempotency, Pagination, and API Design

Now that you understand the Spring MVC request pipeline, we're going to focus on **how to design REST APIs properly**.

This is one of the most important areas for a Spring Boot interview because interviewers can start with:

> "How do you create a REST API in Spring Boot?"

and quickly move to:

> "Why use DTOs?"

> "PUT vs PATCH?"

> "What HTTP status should POST return?"

> "When do you use `ResponseEntity`?"

> "What does idempotent mean?"

> "How do you validate request bodies?"

> "How do you handle pagination?"

> "How do you version APIs?"

> "What should happen when a resource doesn't exist?"

We'll build all of that from first principles.

---

# 1. What Is REST?

REST = **Representational State Transfer**.

It's an architectural style for designing networked applications.

The key idea is that we expose **resources** through HTTP.

For example:

```text
Customer
Order
Invoice
Product
Payment
```

could become:

```text
/customers
/orders
/invoices
/products
/payments
```

Instead of designing APIs around actions such as:

```text
/createOrder
/updateOrder
/deleteOrder
```

REST commonly models operations around resources and HTTP methods.

For example:

```http
GET    /orders/123
POST   /orders
PUT    /orders/123
PATCH  /orders/123
DELETE /orders/123
```

Spring MVC directly supports mapping controller methods to these HTTP methods through `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, and `@DeleteMapping`. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html?utm_source=chatgpt.com))

---

# 2. Resource-Oriented Thinking

Suppose we have:

```text
Order
```

Instead of:

```text id="l1"
POST /createOrder
POST /cancelOrder
POST /updateOrder
GET  /getOrder
```

a resource-oriented design might be:

```text id="2m0w"
POST   /orders
GET    /orders/123
PUT    /orders/123
PATCH  /orders/123
DELETE /orders/123
```

The HTTP method communicates the operation.

Think:

```text id="3a8j"
URL
 ↓
Which resource?

HTTP method
 ↓
What operation/semantics?
```

This is the core REST idea.

---

# 3. HTTP Methods

The methods you absolutely need to know:

```text id="xsl4"
GET
POST
PUT
PATCH
DELETE
```

Also understand:

```text id="uvm2"
HEAD
OPTIONS
```

but these are less central to normal CRUD API design.

---

# 4. GET

Purpose:

> Retrieve a representation of a resource.

Example:

```http id="0s23"
GET /orders/123
```

Controller:

```java id="f4h2v0"
@GetMapping("/orders/{id}")
public OrderResponse getOrder(
        @PathVariable Long id) {

    return orderService.getOrder(id);
}
```

Conceptually:

```text id="bn1i"
GET
 ↓
read
```

A GET request should not normally modify the application's resource state.

HTTP semantics define GET as a safe method. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110))

---

# 5. POST

POST is commonly used to:

> Submit a representation for processing, often resulting in creation of a new resource.

Example:

```http id="cz4rm"
POST /orders
Content-Type: application/json
```

Body:

```json id="n9v3ae"
{
  "productId": 10,
  "quantity": 2
}
```

Controller:

```java id="d7j9kf"
@PostMapping("/orders")
public OrderResponse createOrder(
        @RequestBody CreateOrderRequest request) {

    return orderService.createOrder(request);
}
```

POST is **not generally idempotent**. Repeating the same POST can create multiple resources. HTTP semantics classify POST as non-idempotent. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110))

---

# 6. PUT

PUT usually means:

> Replace the representation/state of the target resource with the supplied representation.

Example:

```http id="2p4k1f"
PUT /orders/123
```

```java id="kxarx2"
@PutMapping("/orders/{id}")
public OrderResponse updateOrder(
        @PathVariable Long id,
        @RequestBody UpdateOrderRequest request) {

    return orderService.replaceOrder(id, request);
}
```

The key word is:

> **replace**

---

# 7. PATCH

PATCH means:

> Apply a partial modification to a resource.

Example:

```http id="6d4p6q"
PATCH /orders/123
```

Body:

```json id="1jyd28"
{
  "status": "SHIPPED"
}
```

Controller:

```java id="iyj4mp"
@PatchMapping("/orders/{id}")
public OrderResponse updateStatus(
        @PathVariable Long id,
        @RequestBody UpdateOrderStatusRequest request) {

    return orderService.updateStatus(
            id,
            request.status());
}
```

Think:

```text id="1ycg1b"
PUT
 ↓
replace

PATCH
 ↓
partial modification
```

---

# 8. DELETE

Example:

```http id="ew4kgp"
DELETE /orders/123
```

Controller:

```java id="5hldmy"
@DeleteMapping("/orders/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void deleteOrder(
        @PathVariable Long id) {

    orderService.deleteOrder(id);
}
```

DELETE requests can be idempotent even though the server state changes on the first request. HTTP semantics classify DELETE as idempotent. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110))

---

# 9. The Most Important Interview Question: PUT vs PATCH

This comes up constantly.

### PUT

```text id="6xq7h"
replace/update complete representation
```

### PATCH

```text id="b2ccjf"
partial modification
```

Example resource:

```json id="2n8i5s"
{
  "name": "John",
  "email": "john@example.com",
  "phone": "123456",
  "status": "ACTIVE"
}
```

A PUT might send:

```json id="vdw27w"
{
  "name": "John",
  "email": "new@example.com",
  "phone": "123456",
  "status": "ACTIVE"
}
```

whereas PATCH might send only:

```json id="9nffol"
{
  "email": "new@example.com"
}
```

HTTP semantics define PUT as replacing the target resource representation and PATCH separately as partial modification through a patch document. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110), [RFC 5789](https://www.rfc-editor.org/rfc/rfc5789))

---

# 10. Is PUT Idempotent?

Yes.

This is a subtle but extremely important concept.

Suppose:

```http id="f9u5d2"
PUT /users/123

{
  "name": "John"
}
```

Send it once:

```text id="8q1u6l"
name = John
```

Send it ten times:

```text id="7xv1q6"
name = John
```

The intended final state is still:

```text id="1h8bzq"
name = John
```

That is idempotency.

HTTP defines PUT as idempotent. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110))

---

# 11. What Does Idempotent Actually Mean?

This is commonly misunderstood.

Idempotent does **not** mean:

> "The request has no side effects."

It means:

> **Making the same request multiple times has the same intended effect on the server state as making it once.**

For example:

```text id="gbqv4z"
DELETE /orders/123
```

First request:

```text id="9az1c"
order deleted
```

Second request:

```text id="3h5q4s"
order already deleted
```

The server's final state is still:

```text id="p2n5s1"
order doesn't exist
```

DELETE is therefore idempotent by HTTP semantics, although the responses can differ. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110))

---

# 12. POST Is Not Normally Idempotent

Consider:

```http id="y8l4xi"
POST /orders
```

First request:

```text id="jstt5c"
Order 100
```

Second identical request:

```text id="c7a7kd"
Order 101
```

You might now have:

```text id="e7m8k2"
two orders
```

Therefore POST is not generally idempotent.

HTTP semantics classify POST as non-idempotent. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110))

---

# 13. Why Is Idempotency Important in Distributed Systems?

Imagine:

```text id="wnj8m7"
Client
  ↓
POST /payments
  ↓
network timeout
```

The client doesn't know whether:

```text id="k10f1l"
payment failed
```

or:

```text id="bp58q4"
payment succeeded but response was lost
```

The client retries.

Without idempotency protection:

```text id="3ko9qa"
POST
 ↓
Payment #1

retry
 ↓
Payment #2
```

That's catastrophic.

So payment APIs often use an:

```text id="x8d4lq"
Idempotency-Key
```

such as:

```http id="emzujp"
Idempotency-Key: 8c9f...
```

The server can use that key to ensure the same logical operation isn't processed twice.

This is an **application-level idempotency mechanism**, not a change to HTTP's definition of POST.

---

# 14. Idempotency Key Concept

Conceptually:

```text id="c21rfd"
Client
  |
  | POST + Idempotency-Key=A
  ↓
Server
  |
  ↓
process operation
  |
  ↓
store result for key A
```

Retry:

```text id="z4v4j9"
POST + Idempotency-Key=A
         ↓
same logical request
         ↓
return previous result
```

So:

```text id="0p0s0g"
network retry
    ≠
duplicate business operation
```

This is extremely useful knowledge for microservices/payment interviews.

---

# 15. HTTP Safety vs Idempotency

Two different concepts.

### Safe

Method doesn't intentionally change resource state.

```text id="x9m3u4"
GET
HEAD
OPTIONS
```

### Idempotent

Repeated execution has the same intended effect on resource state as one execution.

```text id="6x4knh"
GET
PUT
DELETE
HEAD
OPTIONS
```

PATCH can also be designed idempotently, but PATCH is not inherently idempotent by HTTP semantics; it depends on the patch operation/document and server implementation. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110), [RFC 5789](https://www.rfc-editor.org/rfc/rfc5789))

So:

```text id="5gvjbd"
safe
    ≠
idempotent
```

---

# 16. HTTP Status Codes

You must know these well.

### Success

```text id="7sga2d"
200 OK
201 Created
202 Accepted
204 No Content
```

### Client errors

```text id="k4slg1"
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
405 Method Not Allowed
409 Conflict
415 Unsupported Media Type
422 Unprocessable Content
```

### Server errors

```text id="c35az2"
500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
504 Gateway Timeout
```

The exact statuses appropriate to an API depend on its semantics and error model.

---

# 17. The Big Difference: 401 vs 403

Very common question.

### 401 Unauthorized

The request lacks valid authentication credentials.

Think:

```text id="w5bq1x"
Who are you?
```

### 403 Forbidden

The server understood who you are (or otherwise has sufficient authentication context) but refuses to authorize the action.

Think:

```text id="9g3g5g"
I know who you are,
but you cannot do this.
```

HTTP's authentication framework defines 401 around missing/invalid authentication credentials and 403 around refusing to authorize the request. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110))

This will become very important when we reach Spring Security.

---

# 18. 404 Not Found

If:

```http id="j6pdla"
GET /orders/123
```

and Order 123 doesn't exist, a typical REST API returns:

```http id="m1ko5m"
404 Not Found
```

A controller might throw:

```java id="j02e6m"
throw new OrderNotFoundException(id);
```

which your global exception handler converts into a 404 response.

---

# 19. 400 Bad Request

Typically means:

> The request cannot be processed because the client supplied invalid request data.

Examples:

```text id="e4lm4d"
malformed JSON
invalid query parameter
invalid path parameter format
validation failure
```

But precise semantics depend on the API and framework behavior.

Spring MVC's request-body validation errors can result in a 400 response by default in relevant cases. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html?utm_source=chatgpt.com))

---

# 20. 409 Conflict

This is useful when the request conflicts with the current resource state.

Examples:

```text id="d1p7yy"
duplicate username
optimistic locking conflict
business state conflict
attempt to create an existing resource
```

For example:

```http
POST /users
```

with:

```json
{
  "email": "john@example.com"
}
```

If the email must be unique and already exists, a 409 can be appropriate depending on the API design.

Don't mechanically return 409 for every validation problem.

---

# 21. 201 Created

For successful resource creation, a very common response is:

```http
201 Created
```

Example:

```java id="5qsv0u"
@PostMapping("/orders")
@ResponseStatus(HttpStatus.CREATED)
public OrderResponse createOrder(
        @RequestBody CreateOrderRequest request) {

    return orderService.createOrder(request);
}
```

Often you also want a:

```http
Location: /orders/123
```

header identifying the newly created resource.

Spring's `ResponseEntity` makes it easy to construct this response.

---

# 22. `ResponseEntity`

This is one of the most important Spring MVC classes for REST APIs.

Spring describes `ResponseEntity` as an extension of `@ResponseBody` semantics that lets you control:

```text id="2c6p8v"
status
headers
body
```

while the body can still be rendered via `HttpMessageConverter`. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html?utm_source=chatgpt.com))

Example:

```java id="ewxg27"
@GetMapping("/orders/{id}")
public ResponseEntity<OrderResponse> getOrder(
        @PathVariable Long id) {

    OrderResponse order =
            orderService.findOrder(id);

    return ResponseEntity.ok(order);
}
```

---

# 23. Why Use `ResponseEntity`?

Without it:

```java id="oau7jt"
@GetMapping("/{id}")
public OrderResponse getOrder(...) {
    return order;
}
```

you generally rely on framework/controller annotations for status behavior.

With:

```java id="qgpxc4"
ResponseEntity<OrderResponse>
```

you can dynamically control:

```text id="f8u3r8"
status
headers
body
```

For example:

```java id="z1jwjg"
return ResponseEntity
        .status(HttpStatus.CREATED)
        .header("Location", location)
        .body(order);
```

---

# 24. `ResponseEntity` When You Need Dynamic Status

Consider:

```java id="9q0qsy"
@GetMapping("/{id}")
public ResponseEntity<OrderResponse> getOrder(
        @PathVariable Long id) {

    return orderService.findOrder(id)
            .map(order ->
                ResponseEntity.ok(order))
            .orElseGet(() ->
                ResponseEntity.notFound().build());
}
```

Now:

```text id="xjr2v5"
found
  ↓
200

not found
  ↓
404
```

This is a common reason to use `ResponseEntity`.

---

# 25. When NOT to Overuse `ResponseEntity`

You don't have to return:

```java id="6grmzh"
ResponseEntity<T>
```

from every controller method.

For a simple API:

```java id="xuvj2f"
@GetMapping
public List<OrderResponse> getOrders() {
    return service.getOrders();
}
```

is perfectly reasonable.

If the endpoint always returns:

```text id="84xx1b"
200 OK
```

you don't gain much by wrapping everything in:

```text id="e2fy02"
ResponseEntity<List<OrderResponse>>
```

Use `ResponseEntity` when response status/header control actually matters.

---

# 26. `@ResponseStatus`

For a fixed status:

```java id="i3vnpp"
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public OrderResponse create(...) {
}
```

This is very simple.

Use:

```text id="clpzpo"
@ResponseStatus
```

when the response status is fixed.

Use:

```text id="guat43"
ResponseEntity
```

when you need dynamic status/header/body control.

---

# 27. DTOs — One of the Most Important Design Topics

Suppose your JPA entity is:

```java id="m4h0gi"
@Entity
public class User {

    @Id
    private Long id;

    private String name;

    private String email;

    private String password;

    private String internalFlag;
}
```

Would you return this directly from:

```java
@GetMapping("/users/{id}")
```

Usually, **no**.

Instead, use a DTO:

```java id="b9jcn6"
public record UserResponse(
        Long id,
        String name,
        String email
) {
}
```

Why?

Because your database/entity model and your API contract have different responsibilities.

---

# 28. Entity vs DTO

Think:

```text id="6r6wq9"
JPA Entity
   ↓
database persistence model

DTO
   ↓
API contract
```

These should not automatically be treated as the same thing.

---

# 29. Why Returning Entities Can Be Dangerous

Consider:

```java id="3rgzqu"
@GetMapping("/{id}")
public User getUser(...) {
    return userRepository.findById(id).orElseThrow();
}
```

Potential issues:

```text id="cyqt9q"
internal fields accidentally exposed
password accidentally exposed
database structure coupled to API
lazy-loading problems
bidirectional relationship recursion
large object graph
API changes coupled to DB model
```

For example:

```text id="fhk9dz"
User
  ↓
Orders
  ↓
User
  ↓
Orders
  ↓
...
```

JSON serialization can become problematic.

---

# 30. DTO Advantages

DTOs provide:

```text id="3ld4o3"
API contract isolation
security
smaller responses
controlled fields
validation boundaries
easier API evolution
database/API decoupling
```

Suppose later the database changes:

```text id="g2j6r3"
first_name
last_name
```

while the API expects:

```text id="85ms9w"
fullName
```

You can keep the API stable while changing persistence internals.

---

# 31. Request DTO vs Response DTO

It's often useful to have different DTOs.

### Request

```java id="7z1xsr"
public record CreateUserRequest(
        String name,
        String email,
        String password
) {}
```

### Response

```java id="lek6cc"
public record UserResponse(
        Long id,
        String name,
        String email
) {}
```

Notice:

```text id="ufb6g0"
password
    ↓
request

password
    ↓
NOT response
```

This is a very common real-world design.

---

# 32. Why Not Use One DTO For Everything?

You can, but it often creates awkward semantics.

For example:

```text id="3fbq8y"
CreateUserRequest
    password required

UserResponse
    password must never appear
```

One object now has conflicting responsibilities.

Separate DTOs make intent explicit.

---

# 33. Record DTOs

Modern Java makes DTOs very concise:

```java id="fhg9ie"
public record CreateOrderRequest(
        @NotNull Long productId,
        @Positive int quantity
) {}
```

and:

```java id="55a2y0"
public record OrderResponse(
        Long id,
        String status,
        BigDecimal amount
) {}
```

This is an excellent current approach for immutable API models.

---

# 34. Validation

Spring MVC integrates with Jakarta Bean Validation.

Suppose:

```java id="8un6fj"
public record CreateOrderRequest(

        @NotNull
        Long productId,

        @Positive
        int quantity
) {
}
```

Controller:

```java id="7q1m6k"
@PostMapping("/orders")
public OrderResponse create(
        @Valid @RequestBody CreateOrderRequest request) {

    return orderService.create(request);
}
```

Spring MVC applies validation to the `@RequestBody` parameter. Current Spring Framework documentation describes `@Valid`/`@Validated` support for request-body validation. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

---

# 35. Common Validation Annotations

Know these:

```text id="f42b8h"
@NotNull
@NotBlank
@NotEmpty
@Size
@Min
@Max
@Positive
@PositiveOrZero
@Negative
@Email
@Pattern
```

Examples:

```java id="89r9gn"
@NotBlank
String name;
```

```java id="ic6d4v"
@Email
String email;
```

```java id="m61knd"
@Positive
int quantity;
```

---

# 36. `@Valid` vs `@Validated`

This is an important interview question.

### `@Valid`

Comes from Jakarta Validation:

```java id="g4k39f"
import jakarta.validation.Valid;
```

Primarily triggers validation of the object and nested `@Valid` content.

### `@Validated`

Spring's annotation:

```java id="0vp51k"
import org.springframework.validation.annotation.Validated;
```

It supports Spring-specific validation features including validation groups.

Current Spring MVC documentation explains the distinction and notes that validation can occur at the argument level or method level depending on the annotations and method signature. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

---

# 37. Validation Failure

Suppose:

```json id="ps9w9n"
{
    "productId": null,
    "quantity": -2
}
```

and:

```java id="l8f2xo"
@Valid @RequestBody CreateOrderRequest request
```

Validation fails.

For individual request-object validation, Spring MVC raises:

```text id="0xlse4"
MethodArgumentNotValidException
```

and its default handling results in:

```http
400 Bad Request
```

in the normal case. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html?utm_source=chatgpt.com))

---

# 38. Modern Spring MVC Has Two Validation Paths

This is a useful **current Spring 7** detail.

Validation can happen:

### Object-level argument validation

```java id="nq93v5"
@Valid @RequestBody RequestDto request
```

and may result in:

```text id="itnuzj"
MethodArgumentNotValidException
```

### Method validation

Suppose:

```java id="m5g9d4"
@GetMapping
public User get(
        @Min(1) @RequestParam Long id) {
}
```

Method validation may produce:

```text id="xk4z9m"
HandlerMethodValidationException
```

Current Spring documentation explicitly says applications should be prepared to handle both exception types because the applicable path depends on the controller method signature. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

This is newer than many older interview tutorials, so it's worth knowing.

---

# 39. Nested Validation

Suppose:

```java id="wh5ek6"
public record CreateOrderRequest(
        @Valid CustomerRequest customer,
        @Valid List<OrderItemRequest> items
) {
}
```

`@Valid` tells Bean Validation to validate nested objects as well.

This is why:

```text id="7dzp6h"
@Valid
```

is not itself a constraint like:

```text id="hwd5ey"
@NotNull
```

It triggers traversal/validation of nested constraints.

Current Spring's validation documentation explicitly notes that `@Valid` is not itself a constraint annotation. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

---

# 40. Path Validation

Suppose:

```java id="qonl3b"
@GetMapping("/users/{id}")
public UserResponse get(
        @PathVariable
        @Min(1)
        Long id) {
}
```

The value itself can be method-validated.

That's different from validating:

```java id="5s2zzg"
@Valid @RequestBody UserRequest request
```

The first can involve method validation.

The second is object validation.

Again, current Spring MVC distinguishes these validation mechanisms. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

---

# 41. API Error Responses

A good API shouldn't return something inconsistent like:

```json id="m8d67y"
{
    "error": "something went wrong"
}
```

for one endpoint and:

```json id="q7s0gn"
{
    "message": "...",
    "status": 400,
    "details": [...]
}
```

for another.

You want a consistent error model.

Modern Spring Framework supports **Problem Details for HTTP APIs**, based on RFC 9457. The central Spring abstraction is:

```text id="5j4b9x"
ProblemDetail
```

Spring also provides:

```text id="g09x9e"
ErrorResponse
ErrorResponseException
ResponseEntityExceptionHandler
```

for error handling. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html?utm_source=chatgpt.com))

---

# 42. `ProblemDetail`

Conceptually, a response can look like:

```json id="b7t9k8"
{
  "type": "...",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/orders"
}
```

Spring's `ProblemDetail` is designed around RFC 9457's standard problem-details representation. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html?utm_source=chatgpt.com))

This is worth learning because newer Spring interview material increasingly includes standardized error responses.

---

# 43. `@RestControllerAdvice`

A common pattern:

```java id="r26zgc"
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            OrderNotFoundException ex) {

        ProblemDetail problem =
                ProblemDetail.forStatus(
                        HttpStatus.NOT_FOUND);

        problem.setDetail(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problem);
    }
}
```

Conceptually:

```text id="5q2z2q"
Controller A ──┐
Controller B ──┼──> RestControllerAdvice
Controller C ──┘
```

We'll dedicate a complete topic to exception handling later because this deserves much more depth.

---

# 44. Pagination

Suppose:

```http
GET /orders
```

and there are:

```text
2 million orders
```

You don't return all 2 million.

Instead:

```http
GET /orders?page=0&size=20
```

or cursor-based approaches.

Pagination is critical for production APIs.

---

# 45. Offset/Page Pagination

A typical Spring endpoint:

```java id="x4d7x1"
@GetMapping
public Page<OrderResponse> getOrders(
        @RequestParam(defaultValue = "0")
        int page,

        @RequestParam(defaultValue = "20")
        int size) {

    return orderService.getOrders(page, size);
}
```

Conceptually:

```text id="a8n0w5"
page = 0
size = 20

 ↓

records 0-19
```

Then:

```text id="10d2kx"
page = 1
size = 20

 ↓

records 20-39
```

---

# 46. Sorting

You often combine:

```http
GET /orders?page=0&size=20&sort=createdAt,desc
```

with:

```text id="26ks3a"
Pagination
+
Sorting
```

Spring Data later provides convenient abstractions such as:

```text id="5q8o4y"
Pageable
Page
Slice
Sort
```

We'll cover those in detail once we reach Spring Data JPA.

---

# 47. Why Cursor Pagination Exists

Offset pagination can become expensive for large changing datasets.

Example:

```text id="f1x9xx"
page=100000
```

The database may have to process a large offset before returning the desired rows, depending on the database/query plan.

A cursor approach can instead say:

```http
GET /orders?cursor=eyJpZCI6MTIz...
```

and continue from a known position.

Conceptually:

```text id="2csy9c"
Request 1
  ↓
items + nextCursor

Request 2
  ↓
cursor
  ↓
next page
```

Cursor pagination can provide more stable pagination for high-volume/changing datasets, but the correct design depends on the database, ordering requirements, and API semantics.

---

# 48. Filtering

Don't create dozens of endpoints:

```text id="qftnqv"
/orders/paid
/orders/unpaid
/orders/shipped
/orders/cancelled
```

Often you can use:

```http
GET /orders?status=PAID
```

or:

```http
GET /orders?status=PAID&customerId=123
```

Conceptually:

```text id="41nq1n"
resource
   +
query parameters
   ↓
filtering
```

This maps naturally to Spring's `@RequestParam`.

---

# 49. Search

For search:

```http
GET /products?query=laptop
```

For complex filtering:

```http
GET /products?
    category=electronics&
    minPrice=500&
    maxPrice=1500&
    brand=Dell
```

You can model this as a request DTO/filter object instead of a huge method signature when complexity grows.

---

# 50. Sorting + Filtering + Pagination

A realistic API:

```http
GET /orders
    ?status=PAID
    &customerId=123
    &page=0
    &size=20
    &sort=createdAt,desc
```

Conceptually:

```text id="9v5j8a"
HTTP request
    ↓
filter
    ↓
sort
    ↓
paginate
    ↓
database query
    ↓
response
```

This becomes particularly important in your Spring Data JPA work later.

---

# 51. API Versioning

Another interview topic.

Suppose:

```text id="v7rlfz"
v1
v2
```

You could version through the URI:

```http
GET /api/v1/orders
GET /api/v2/orders
```

or headers/media types.

Modern Spring Framework MVC has explicit API-version mapping support with an `ApiVersionStrategy`, and `@RequestMapping` has a `version` attribute once versioning is configured. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html?utm_source=chatgpt.com))

So current Spring is more sophisticated than the old:

> "Spring only supports URL-based API versioning."

It doesn't.

---

# 52. URI Versioning

Most straightforward:

```text id="9guv3n"
GET /api/v1/orders
GET /api/v2/orders
```

Pros:

```text id="phvubj"
easy to understand
easy to test
easy to route
```

Cons:

```text id="02d5s0"
version becomes part of URI
```

---

# 53. Header/Media-Type Versioning

Another approach:

```http id="b2h1p2"
Accept: application/vnd.company.orders.v2+json
```

or a dedicated version header.

This keeps the URI stable but makes debugging and caching/tooling somewhat more complex.

There is no single universal versioning strategy; choose based on your organization's compatibility and operational requirements.

---

# 54. CORS

Another common Spring Boot REST topic:

> What if frontend is running on `http://localhost:3000` and backend on `http://localhost:8080`?

Different origins.

Browser same-origin policy can restrict requests.

Spring MVC provides CORS configuration mechanisms, including:

```text id="1s4v0w"
@CrossOrigin
WebMvcConfigurer
global CORS configuration
```

Spring's MVC documentation has dedicated CORS support. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html?utm_source=chatgpt.com))

We'll cover CORS in much more detail later, especially after Spring Security.

---

# 55. `@CrossOrigin`

Example:

```java id="38u4ta"
@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {
}
```

This allows cross-origin requests from the configured origin.

But don't blindly use:

```java id="u8s9h2"
@CrossOrigin(origins = "*")
```

for security-sensitive APIs.

CORS controls browser cross-origin behavior; it is **not authentication or authorization**.

---

# 56. `OPTIONS`

Browser CORS interactions can involve an:

```http
OPTIONS
```

preflight request.

Spring MVC can handle CORS preflight requests through its CORS infrastructure.

This is why sometimes developers see:

```text
OPTIONS /api/orders
```

before:

```text
POST /api/orders
```

and wonder:

> "Why is my browser sending OPTIONS?"

It can be a CORS preflight.

---

# 57. API Contract

A good REST API should define:

```text id="f7dr1l"
URL
HTTP method
request headers
request parameters
request body
response body
status codes
error format
authentication
pagination
versioning
```

For example:

```text id="73meq4"
POST /api/v1/orders

Request:
{
  "productId": 100,
  "quantity": 2
}

Response:
201 Created

{
  "id": 123,
  "status": "CREATED"
}
```

This is an **API contract**.

---

# 58. Don't Expose Database Models as API Contracts

This principle is worth repeating:

```text id="8no8qa"
Database Model
      ≠
API Contract
```

Because database changes are often implementation details.

Suppose your entity changes:

```text id="t2fpwc"
amount
 ↓
grossAmount
netAmount
taxAmount
```

You may still want the external API to expose:

```json id="ycqjpe"
{
  "total": 100
}
```

DTOs give you that boundary.

---

# 59. Controller Responsibilities

A healthy controller usually handles:

```text id="h1eyjp"
HTTP concerns
input binding
validation trigger
calling service
returning response
```

It should avoid becoming:

```text id="0j5zx7"
business logic engine
database access layer
transaction coordinator
```

A typical architecture:

```text id="a1z0ry"
Controller
    ↓
Service
    ↓
Repository
```

Think:

```text id="tft2w1"
Controller
   = HTTP boundary

Service
   = business logic

Repository
   = data access
```

We'll later connect this to SOLID and clean architecture.

---

# 60. Don't Put Business Logic in Controllers

Avoid:

```java id="te1cez"
@PostMapping
public OrderResponse create(
        @RequestBody CreateOrderRequest request) {

    // validate business rules
    // calculate price
    // reserve inventory
    // charge payment
    // persist order
    // send notification

    ...
}
```

Prefer:

```java id="1b21bi"
@PostMapping
public OrderResponse create(
        @Valid @RequestBody CreateOrderRequest request) {

    return orderService.create(request);
}
```

Controller stays thin.

---

# 61. Request DTO → Service Model

Sometimes you don't even want your service layer to depend on HTTP DTOs.

For example:

```text id="d8gsx8"
CreateOrderRequest
      ↓
OrderCommand
      ↓
OrderService
```

This creates an additional separation:

```text id="oig3j3"
HTTP representation
        ≠
business/application model
```

Whether this extra layer is worthwhile depends on the complexity of your application.

For many standard CRUD services:

```text id="j8wq8w"
Controller DTO → Service
```

is sufficient.

---

# 62. `ResponseEntity` vs DTO

Another common confusion:

```text id="1nlbzj"
DTO
   =
what data?

ResponseEntity
   =
how should HTTP response be represented?
```

Example:

```java id="2xar0t"
ResponseEntity<OrderResponse>
```

means:

```text id="9qk6mb"
body = OrderResponse
status = ...
headers = ...
```

They solve different problems.

---

# 63. Request DTO vs Entity vs Response DTO

Visualize:

```text id="rqrlfu"
HTTP Request
    ↓
CreateOrderRequest
    ↓
Service
    ↓
Order entity/domain object
    ↓
Repository
    ↓
database
```

Returning:

```text id="1n4h3v"
database/entity
    ↓
OrderResponse
    ↓
HTTP Response
```

So:

```text id="2i2h8v"
Request DTO
     ↓
application logic
     ↓
Entity/domain
     ↓
Response DTO
```

This is a clean API boundary.

---

# 64. What Does a Good POST Response Look Like?

Suppose:

```http
POST /orders
```

creates:

```text
Order 123
```

A strong REST response can be:

```http
201 Created
Location: /orders/123
Content-Type: application/json
```

```json
{
  "id": 123,
  "status": "CREATED"
}
```

You can construct it with:

```java id="u7lqhm"
URI location =
        URI.create("/orders/" + order.id());

return ResponseEntity
        .created(location)
        .body(response);
```

`ResponseEntity` supports the full HTTP response including status and headers. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html?utm_source=chatgpt.com))

---

# 65. Should POST Always Return 201?

Not necessarily.

HTTP semantics don't say every POST must return 201.

Depending on what the endpoint does, POST can legitimately result in:

```text id="xik4s2"
200 OK
201 Created
202 Accepted
204 No Content
```

The right status depends on whether the operation has completed, created a resource, or been accepted for asynchronous processing.

So in an interview, don't say:

> "POST always returns 201."

Say:

> "201 Created is appropriate when POST results in successful creation of a resource; other statuses such as 200 or 202 can be appropriate depending on the operation."

---

# 66. `202 Accepted`

Suppose:

```http
POST /reports
```

starts a long-running report generation job.

You don't wait 10 minutes.

Instead:

```http
202 Accepted
```

might indicate:

```text id="g7x9v1"
request accepted
processing asynchronously
```

The client may receive:

```json id="v9m5ae"
{
  "jobId": "abc123",
  "status": "PROCESSING"
}
```

This is an important microservices/distributed-system concept.

---

# 67. `204 No Content`

Useful for:

```http
DELETE /orders/123
```

or an update operation where no response body is required.

Example:

```java id="l28v4r"
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)
public void delete(
        @PathVariable Long id) {
    service.delete(id);
}
```

Response:

```http
204 No Content
```

with no body.

---

# 68. Conditional Requests

For advanced REST design, you'll eventually encounter:

```text id="q0s5rb"
ETag
If-Match
If-None-Match
Last-Modified
If-Modified-Since
```

These are useful for:

```text id="gt5xn3"
caching
concurrency control
avoiding unnecessary transfers
```

Spring MVC supports HTTP caching-related features. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet.html?utm_source=chatgpt.com))

This becomes particularly interesting when we reach:

> optimistic locking + REST API concurrency.

---

# 69. ETag

Suppose:

```http
GET /orders/123
```

returns:

```http
ETag: "abc123"
```

Client later sends:

```http
If-None-Match: "abc123"
```

If the resource hasn't changed, the server can return:

```http
304 Not Modified
```

This allows the client to reuse its cached representation.

ETag is defined by HTTP semantics in RFC 9110.

---

# 70. `If-Match` and Optimistic Concurrency

Suppose two users load:

```text
Order 123
version = 5
```

User A updates.

Now version becomes:

```text
6
```

User B tries to update the old version.

With conditional requests:

```http
If-Match: "version-5"
```

the server can detect that the resource changed and refuse the stale update.

This can be combined with persistence-layer optimistic locking.

We'll connect these concepts later.

---

# 71. HTTP API + Database Transaction

A common enterprise flow:

```text
HTTP POST
   ↓
Controller
   ↓
Service
   ↓
@Transactional
   ↓
Repository
   ↓
Database
   ↓
response
```

This is where our later `@Transactional` topic will become relevant.

Don't put:

```java id="9q1y6v"
@Transactional
```

randomly on controllers just because you can.

Transaction boundaries are typically better aligned with application/service operations.

We'll dedicate a complete section to this later.

---

# 72. A Complete REST Controller

Here's a reasonable example:

```java id="r5k5yj"
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id) {

        return orderService.findById(id)
                .map(order ->
                        ResponseEntity.ok(order))
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid
            @RequestBody
            CreateOrderRequest request) {

        OrderResponse response =
                orderService.create(request);

        URI location =
                URI.create(
                    "/api/v1/orders/" +
                    response.id());

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> replaceOrder(
            @PathVariable Long id,
            @Valid
            @RequestBody
            ReplaceOrderRequest request) {

        return ResponseEntity.ok(
                orderService.replace(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderResponse> patchOrder(
            @PathVariable Long id,
            @RequestBody
            PatchOrderRequest request) {

        return ResponseEntity.ok(
                orderService.patch(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id) {

        orderService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
```

This demonstrates:

```text id="8ej7pv"
REST resource
HTTP methods
DTOs
validation
ResponseEntity
201 Created
204 No Content
404 Not Found
```

---

# 73. Why `ResponseEntity` Works Nicely Here

Each endpoint can have a different response:

```text id="01pdjk"
GET
 ↓
200 / 404

POST
 ↓
201

PUT
 ↓
200 / 404

DELETE
 ↓
204 / 404
```

That's why `ResponseEntity` can be useful for APIs with dynamic outcomes.

---

# 74. EPAM Interview Question: "What Is REST?"

Strong answer:

> REST is an architectural style for distributed systems where application functionality is modeled around resources and standardized interactions such as HTTP methods, representations, status codes, and stateless requests. In a Spring MVC application, REST APIs are commonly implemented with annotated controllers and HTTP message conversion.

Don't say:

> "REST means JSON over HTTP."

That's far too narrow.

---

# 75. EPAM Interview Question: "PUT vs PATCH?"

Strong answer:

> PUT is generally used to replace the target resource representation, while PATCH applies a partial modification. PUT is idempotent under HTTP semantics, whereas PATCH is not inherently idempotent; its behavior depends on the specific patch semantics and implementation. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110), [RFC 5789](https://www.rfc-editor.org/rfc/rfc5789))

---

# 76. EPAM Interview Question: "What Is Idempotency?"

Strong answer:

> An operation is idempotent when repeating the same request has the same intended effect on the server state as making it once. It doesn't mean the request has no side effects and it doesn't necessarily mean every response is identical.

Excellent answer.

---

# 77. EPAM Interview Question: "When Would You Use `ResponseEntity`?"

Strong answer:

> I use `ResponseEntity` when the endpoint needs to control the HTTP response dynamically, such as returning different status codes depending on the outcome, setting response headers, or controlling the response body. If the response status is fixed and straightforward, a plain return value with `@ResponseStatus` can be simpler. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responseentity.html?utm_source=chatgpt.com))

---

# 78. EPAM Interview Question: "Why Use DTOs?"

Strong answer:

> DTOs separate the external API contract from the persistence/domain model. This helps prevent accidental exposure of internal fields, reduces coupling to the database schema, gives us control over the request and response shape, and makes API evolution easier.

---

# 79. EPAM Interview Question: "Why Not Return JPA Entities?"

Answer:

> Returning entities can expose internal fields and relationships, create serialization problems with lazy loading or bidirectional relationships, and couple the API contract directly to the persistence model. DTOs provide a more controlled API boundary.

---

# 80. EPAM Interview Question: "Where Should Validation Happen?"

A good layered answer:

> Basic HTTP/input validation should happen at the API boundary using Bean Validation, for example `@Valid` with request DTOs. Business rules still belong in the service/domain layer because they must be enforced regardless of whether the operation arrived through HTTP or another entry point.

This distinction is very important:

```text id="dw1w9v"
Input validation
    ↓
Controller boundary

Business validation
    ↓
Service/domain
```

---

# 81. EPAM Scenario

> "A client sends malformed JSON. Does the controller execute?"

Normally:

```text id="2p1x5b"
JSON
 ↓
HttpMessageConverter
 ↓
deserialization fails
 ↓
controller method isn't successfully invoked
```

`@RequestBody` uses an `HttpMessageConverter` to deserialize the request body. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html?utm_source=chatgpt.com))

---

# 82. EPAM Scenario

> "The JSON is valid but violates `@NotNull`. Does the controller execute?"

With:

```java id="hqb8fc"
@Valid @RequestBody CreateOrderRequest request
```

validation occurs before normal controller-method execution.

If validation fails, Spring raises the appropriate validation exception, commonly `MethodArgumentNotValidException` for object-level request validation. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

---

# 83. EPAM Scenario

> "Should validation only be done in the controller?"

No.

There are two levels:

```text id="gr4w3s"
HTTP/input validation
     ↓
DTO constraints

Business invariants
     ↓
service/domain
```

For example:

```text
quantity must be positive
```

can be DTO validation.

But:

```text
order cannot be cancelled after shipment
```

is business logic.

It must not rely only on controller validation.

---

# 84. EPAM Scenario

> "When should DELETE return 200 vs 204?"

There isn't one universal answer.

Use:

```text id="vdpz8x"
204
```

when the operation succeeds and there's intentionally no response body.

Use:

```text id="8v6u2j"
200
```

when you want to return a representation/body describing the outcome.

The key is consistency with your API contract.

---

# 85. EPAM Scenario

> "POST /payments timed out, so the client retries. How can you prevent duplicate payment?"

Think:

```text id="rv7r91"
idempotency key
+
server-side deduplication
+
persist operation/result
```

For example:

```http
Idempotency-Key: abc123
```

Then:

```text id="cu79t3"
first request
 ↓
process payment
 ↓
store result against key

retry
 ↓
same key
 ↓
return original result
```

This is an excellent microservices/distributed systems interview answer.

---

# 86. EPAM Scenario

> "Why shouldn't we return 500 for every exception?"

Because HTTP status codes communicate where the failure belongs.

For example:

```text id="0o9k2v"
invalid request        → 400
not authenticated      → 401
not authorized         → 403
resource absent        → 404
state conflict         → 409
unsupported media type → 415
server failure         → 500
```

The error model should reflect the semantics of the actual failure rather than treating every exception as a server bug.

---

# 87. EPAM Scenario

> "Why should an API have a consistent error response?"

Because clients need predictable machine-readable behavior.

Suppose every endpoint returns:

```json id="yx0b3w"
{
  "type": "...",
  "title": "...",
  "status": 400,
  "detail": "...",
  "instance": "..."
}
```

Then clients can consistently handle:

```text id="r5v3fj"
validation
not found
authorization
conflicts
server errors
```

Modern Spring supports RFC 9457 problem-details responses through `ProblemDetail` and related error-response infrastructure. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html?utm_source=chatgpt.com))

---

# 88. REST API Design Cheat Sheet

A useful resource model:

```text id="2bkmc8"
GET    /orders
GET    /orders/{id}
POST   /orders
PUT    /orders/{id}
PATCH  /orders/{id}
DELETE /orders/{id}
```

Query operations:

```text id="ddp4vr"
GET /orders
    ?status=PAID
    &page=0
    &size=20
    &sort=createdAt,desc
```

Avoid overly action-oriented URLs where an HTTP method/resource model already expresses the operation.

---

# 89. The Complete Request-to-Response Picture

Now combine today's topic with Topic 13:

```text id="syf06j"
HTTP Request
     ↓
Servlet Container
     ↓
Filter
     ↓
DispatcherServlet
     ↓
HandlerMapping
     ↓
HandlerAdapter
     ↓
Argument Resolution
     ↓
@RequestBody
     ↓
HttpMessageConverter
     ↓
Request DTO
     ↓
Validation
     ↓
Controller
     ↓
Service
     ↓
Repository
     ↓
Database
     ↓
Domain/Entity
     ↓
Response DTO
     ↓
ResponseEntity
     ↓
HttpMessageConverter
     ↓
JSON
     ↓
HTTP Response
```

This is an extremely important Spring Boot interview diagram.

---

# 90. The REST Design Mental Model

Think of five boundaries:

```text id="7ja2tr"
HTTP
 ↓
Controller
 ↓
Service
 ↓
Repository
 ↓
Database
```

At the boundary:

```text id="nktspb"
HTTP
  ↕
DTO
```

Inside:

```text id="dqf7pf"
Service
  ↕
domain/entity/model
```

This separation protects your architecture.

---

# 91. What You Should Know Cold

### HTTP

```text
GET     → retrieve
POST    → submit/create/process
PUT     → replace
PATCH   → partial modification
DELETE  → delete
```

with HTTP semantics defined in RFC 9110 and PATCH in RFC 5789. ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110), [RFC 5789](https://www.rfc-editor.org/rfc/rfc5789))

### Status

```text
200 → success
201 → created
202 → accepted for processing
204 → success, no body
400 → bad request
401 → authentication required/failed
403 → forbidden
404 → not found
409 → conflict
415 → unsupported media type
500 → server failure
```

### Spring

```text
@RequestBody
@PathVariable
@RequestParam
@ResponseBody
ResponseEntity
@Valid
@RestControllerAdvice
ProblemDetail
```

---

# 92. The 12 EPAM Questions From This Topic

Be prepared to answer:

**1. What is REST?**

**2. PUT vs PATCH?**

**3. GET vs POST?**

**4. Is POST idempotent?**

**5. Is DELETE idempotent?**

**6. What does idempotent actually mean?**

**7. 401 vs 403?**

**8. 400 vs 422?**

**9. When would you use `ResponseEntity`?**

**10. Why use DTOs instead of entities?**

**11. How does `@Valid @RequestBody` work?**

**12. How do you design pagination/filtering/sorting?**

You should now be able to answer all twelve at a good interview level.

---

# 93. One Current-Spring Detail Worth Knowing

Modern Spring MVC has evolved beyond many older interview tutorials.

Three examples:

```text id="5r7w4x"
1. ProblemDetail / RFC 9457 error responses

2. Built-in method validation behavior
   with HandlerMethodValidationException

3. API versioning support via ApiVersionStrategy
   and @RequestMapping(version=...)
```

These are documented in current Spring Framework 7 MVC documentation. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com))

So when you're preparing for current interviews, don't rely exclusively on articles written for Spring Boot 2.x.

---

# 94. Final Mental Model

```text id="6m5bbi"
                       REST API
                          |
             ┌────────────┴────────────┐
             ↓                         ↓
         Request                    Response
             |                         |
       ┌─────┴─────┐             ┌─────┴──────┐
       ↓           ↓             ↓            ↓
    Path/Query   Body         Status        Headers
       |           |             |            |
       ↓           ↓             └──────┬─────┘
@PathVariable  @RequestBody             |
@RequestParam       ↓                   |
                  DTO                    |
                   ↓                     |
                @Valid                   |
                   ↓                     |
               Controller               |
                   ↓                     |
                Service                  |
                   ↓                     |
               Repository               |
                   ↓                     |
                Database                 |
                   ↓                     |
             Response DTO ───────────────┘
                   ↓
             ResponseEntity
                   ↓
         HttpMessageConverter
                   ↓
                  JSON
```

That's the REST architecture you should have in your head.

---

# Next Topic — Exception Handling in Spring Boot

This is the natural next step because our REST API currently has failures such as:

```text
OrderNotFoundException
MethodArgumentNotValidException
IllegalArgumentException
database exception
business exception
```

Next we'll go deep into:

```text
@ExceptionHandler
@ControllerAdvice
@RestControllerAdvice
ResponseEntityExceptionHandler
ProblemDetail
ErrorResponse
HTTP status mapping
custom exceptions
validation errors
multiple exception handlers
exception-handler precedence
global vs local handling
```

We'll also work through an actual production-style error response:

```json
{
  "type": "...",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/orders",
  "errors": [
    {
      "field": "quantity",
      "message": "must be greater than 0"
    }
  ]
}
```

and understand **exactly how Spring gets from a thrown exception to that JSON response**.

