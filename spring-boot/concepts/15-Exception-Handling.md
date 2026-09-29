# Topic 15 — Exception Handling in Spring Boot

Exception handling is one of the most commonly discussed Spring MVC interview topics because it connects several things you already learned:

`Controller → Service → Exception → DispatcherServlet → Exception Resolver → HTTP Response`

The key idea is simple:

> **An exception is an internal Java event; exception handling converts that event into a meaningful HTTP response for the client.**

Spring MVC has built-in infrastructure for this. When a request handler throws an exception, `DispatcherServlet` delegates it to a chain of `HandlerExceptionResolver`s that try to turn the exception into an appropriate response. ([Home][1])

---

# 1. Why do we need exception handling?

Suppose we have:

```java
@GetMapping("/users/{id}")
public UserDto getUser(@PathVariable Long id) {
    return userService.getUser(id);
}
```

And the service does:

```java
public UserDto getUser(Long id) {
    User user = repository.findById(id)
            .orElseThrow(() ->
                    new UserNotFoundException("User not found: " + id));

    return convertToDto(user);
}
```

The service throws:

```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
```

Without explicit handling, what should the client receive?

Ideally:

```http
HTTP/1.1 404 Not Found
```

with something like:

```json
{
  "status": 404,
  "message": "User not found: 123"
}
```

We don't want:

```text
java.util.NoSuchElementException
    at ...
    at ...
    at ...
```

or a database exception leaking to the client.

There are several reasons to centralize this:

* consistent API responses
* correct HTTP status codes
* no duplicate `try/catch` in every controller
* no leakage of internal implementation details
* easier logging and monitoring
* easier validation-error formatting

---

# 2. First understand the exception flow

This is the most important conceptual part.

Imagine:

```text
Client
   |
   v
DispatcherServlet
   |
   v
Controller
   |
   v
Service
   |
   v
Repository
```

Suppose the repository causes an exception:

```text
Repository
    |
    X Exception
    |
    v
Service
    |
    X propagates
    |
    v
Controller
    |
    X propagates
    |
    v
DispatcherServlet
    |
    v
HandlerExceptionResolver chain
    |
    +--> @ExceptionHandler
    |
    +--> @ResponseStatus / ResponseStatusException
    |
    +--> Default Spring MVC exception handling
    |
    v
HTTP response
```

Spring MVC's `DispatcherServlet` uses `HandlerExceptionResolver` implementations for this resolution process. The built-in infrastructure includes `ExceptionHandlerExceptionResolver`, `ResponseStatusExceptionResolver`, and `DefaultHandlerExceptionResolver`. ([Home][2])

This leads to the first interview question:

### What happens when a controller throws an exception?

A strong answer:

> The exception propagates out of the controller invocation. `DispatcherServlet` delegates exception handling to its configured `HandlerExceptionResolver` chain. Depending on the exception, Spring may invoke an `@ExceptionHandler`, map a `@ResponseStatus`, or apply its default handling for known MVC exceptions. If no resolver handles it, the exception can propagate to the servlet container's error handling. ([Home][2])

That's a very interview-friendly explanation.

---

# 3. `@ExceptionHandler`

The simplest Spring mechanism is:

```java
@ExceptionHandler(UserNotFoundException.class)
public ResponseEntity<String> handleUserNotFound(UserNotFoundException ex) {
    return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ex.getMessage());
}
```

The important thing is:

```java
@ExceptionHandler(UserNotFoundException.class)
```

This tells Spring:

> "When this controller encounters this exception, use this method to handle it."

Example:

```java
@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }
}
```

Now:

```text
GET /users/123
```

could result in:

```http
404 Not Found
```

with:

```text
User not found: 123
```

---

# 4. Where does `@ExceptionHandler` apply?

This is extremely important.

If you put it inside:

```java
UserController
```

then normally it applies to that controller's exception handling.

For example:

```java
@RestController
public class UserController {

    @ExceptionHandler(UserNotFoundException.class)
    ...
}
```

It does not automatically mean:

> "Every controller in my application uses this."

For application-wide handling, we use:

```java
@ControllerAdvice
```

or:

```java
@RestControllerAdvice
```

Spring's documentation explicitly distinguishes local `@ExceptionHandler` methods from global advice methods. Global methods are applied after local controller methods. ([Home][3])

---

# 5. `@ControllerAdvice`

Imagine we have:

```text
UserController
OrderController
ProductController
PaymentController
```

and every controller needs the same exception handling.

Doing this:

```java
@ExceptionHandler(...)
```

inside every controller creates duplication.

Instead:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
}
```

Now exception-handling methods can be centralized.

Example:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(
            UserNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }
}
```

This can apply across controllers.

`@ControllerAdvice` itself is a Spring component and is discovered through component scanning. ([Home][3])

---

# 6. `@RestControllerAdvice`

For REST APIs, you'll usually see:

```java
@RestControllerAdvice
```

instead of:

```java
@ControllerAdvice
```

Why?

Because:

```java
@RestControllerAdvice
```

is effectively:

```java
@ControllerAdvice
+
@ResponseBody
```

So the return value from your exception handler is written to the HTTP response body instead of being interpreted as a view. ([Home][3])

Therefore:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

is usually the natural choice for a REST API.

---

# 7. The classic comparison

## `@ExceptionHandler`

Local exception handling.

```java
@RestController
public class UserController {

    @ExceptionHandler(UserNotFoundException.class)
    ...
}
```

Scope:

```text
This controller
```

## `@ControllerAdvice`

Global/cross-controller exception handling.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
}
```

## `@RestControllerAdvice`

Global exception handling specifically suited to REST responses.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

### EPAM interview answer

> `@ExceptionHandler` handles exceptions from a controller, while `@ControllerAdvice` centralizes exception handling across controllers. `@RestControllerAdvice` is a convenience form of `@ControllerAdvice` with response-body semantics, making it particularly suitable for REST APIs. ([Home][3])

---

# 8. Production-style custom exception

Instead of throwing generic exceptions everywhere:

```java
throw new RuntimeException("User not found");
```

create domain/application-specific exceptions.

```java
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User not found: " + id);
    }
}
```

Then:

```java
public UserDto getUser(Long id) {

    User user = userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));

    return mapToDto(user);
}
```

And globally:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(
            UserNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }
}
```

This gives us a clean separation:

```text
Service
    |
    | throws business/application exception
    v
Global Exception Handler
    |
    | translates exception -> HTTP
    v
REST response
```

That separation is architecturally valuable.

---

# 9. Should the service throw `ResponseStatusException`?

This is a very common interview/design question.

You can do:

```java
public UserDto getUser(Long id) {

    return repository.findById(id)
            .map(this::toDto)
            .orElseThrow(() ->
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found"));
}
```

This works.

But notice what happened.

Your:

```text
Service layer
```

now knows about:

```text
HTTP
HttpStatus
ResponseStatusException
```

That introduces web-layer coupling.

A cleaner layered architecture is often:

```text
Controller
    ↓
Service
    ↓
Repository
```

with:

```text
Service → UserNotFoundException
```

and then:

```text
GlobalExceptionHandler
        ↓
404
```

Conceptually:

```text
Business logic
      ≠
HTTP translation
```

This separation becomes especially useful if the service is later reused by:

* another REST controller
* messaging
* scheduled jobs
* CLI
* batch processing

So in a layered enterprise application, I'd generally prefer domain/application exceptions plus centralized web exception mapping.

---

# 10. `@ResponseStatus`

Another mechanism is:

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
```

Then:

```java
throw new UserNotFoundException("User not found");
```

Spring can map this exception to:

```http
404
```

because `ResponseStatusExceptionResolver` handles exceptions annotated with `@ResponseStatus`. ([Home][2])

This is convenient.

But compare:

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
class UserNotFoundException
```

with:

```java
@ExceptionHandler(UserNotFoundException.class)
```

The second approach generally gives you much more flexibility over:

* response body
* headers
* error structure
* logging
* multiple exception types
* API-specific error codes

So `@ResponseStatus` is useful, but centralized `@ExceptionHandler` handling is often more suitable for a larger REST API.

---

# 11. `ResponseEntityExceptionHandler`

Spring provides a convenient base class:

```java
ResponseEntityExceptionHandler
```

It is specifically intended as a base class for global MVC exception handling and handles many Spring MVC exceptions while producing RFC 9457-style error responses. ([Home][4])

A common pattern is:

```java
@RestControllerAdvice
public class GlobalExceptionHandler
        extends ResponseEntityExceptionHandler {
}
```

Now we can override specific methods.

For example:

```java
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

    // custom validation response
}
```

The idea is:

```text
Spring MVC exception
       ↓
ResponseEntityExceptionHandler
       ↓
your customization
```

This is particularly useful when you want consistent handling of framework-generated exceptions.

---

# 12. `ProblemDetail` — modern Spring error handling

Modern Spring supports the **Problem Details for HTTP APIs** specification, RFC 9457. The main Spring abstractions are:

```text
ProblemDetail
ErrorResponse
ErrorResponseException
ResponseEntityExceptionHandler
```

`ProblemDetail` represents the RFC 9457 problem structure. Spring MVC supports returning `ProblemDetail` or `ErrorResponse` from controller and exception-handler methods. ([Home][5])

A typical response might look like:

```json
{
  "type": "https://example.com/problems/user-not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "User 123 was not found",
  "instance": "/api/users/123"
}
```

The standard fields are conceptually:

```text
type
title
status
detail
instance
```

---

# 13. Using `ProblemDetail`

Example:

```java
@ExceptionHandler(UserNotFoundException.class)
public ProblemDetail handleUserNotFound(
        UserNotFoundException ex) {

    ProblemDetail problemDetail =
            ProblemDetail.forStatusAndDetail(
                    HttpStatus.NOT_FOUND,
                    ex.getMessage());

    problemDetail.setTitle("User Not Found");

    return problemDetail;
}
```

The HTTP status is determined from the `status` property of the `ProblemDetail`. Spring also supports rendering it with the appropriate problem-detail media type during content negotiation. ([Home][5])

---

# 14. Adding custom fields to `ProblemDetail`

Real-world APIs frequently need application-specific information.

For example:

```json
{
  "type": "https://example.com/problems/user-not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "User 123 was not found",
  "instance": "/api/users/123",
  "code": "USER_NOT_FOUND",
  "traceId": "abc123"
}
```

`code` and `traceId` are application-specific extension properties, not standard RFC fields.

Spring's `ProblemDetail` supports non-standard properties. ([Home][5])

Example:

```java
problemDetail.setProperty("code", "USER_NOT_FOUND");
problemDetail.setProperty("traceId", traceId);
```

This is a very useful pattern for production APIs.

---

# 15. Why standardized errors are useful

Suppose every endpoint returns a different format.

Endpoint A:

```json
{
  "message": "User not found"
}
```

Endpoint B:

```json
{
  "error": "Order missing"
}
```

Endpoint C:

```json
{
  "reason": "Product does not exist"
}
```

Clients now need custom logic for every API.

A standard structure is much easier:

```json
{
  "type": "...",
  "title": "...",
  "status": 404,
  "detail": "...",
  "instance": "...",
  "code": "..."
}
```

Then a frontend or another microservice knows what structure to expect.

---

# 16. Validation exceptions

This is another major interview topic.

Suppose:

```java
public record CreateUserRequest(
        @NotBlank String name,
        @Email String email,
        @Min(18) int age
) {
}
```

Controller:

```java
@PostMapping
public UserDto createUser(
        @Valid @RequestBody CreateUserRequest request) {

    return userService.createUser(request);
}
```

Suppose the client sends:

```json
{
  "name": "",
  "email": "abc",
  "age": 15
}
```

Validation fails before your service logic runs.

Spring MVC commonly raises:

```text
MethodArgumentNotValidException
```

when validation is applied individually to an argument such as `@RequestBody`. Method-level constraint validation can instead produce:

```text
HandlerMethodValidationException
```

depending on how the constraints are declared. ([Home][6])

---

# 17. `MethodArgumentNotValidException`

For example:

```java
@PostMapping
public UserDto createUser(
        @Valid @RequestBody CreateUserRequest request) {
    ...
}
```

You can handle:

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<?> handleValidation(
        MethodArgumentNotValidException ex) {

    ...
}
```

You can extract field errors:

```java
Map<String, String> errors = new HashMap<>();

ex.getBindingResult()
        .getFieldErrors()
        .forEach(error ->
                errors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));
```

For example:

```json
{
  "name": "must not be blank",
  "email": "must be a valid email",
  "age": "must be greater than or equal to 18"
}
```

---

# 18. `HandlerMethodValidationException`

Modern Spring MVC also has method validation.

For example:

```java
@GetMapping
public UserDto getUser(
        @RequestParam
        @Min(1)
        Long id) {
    ...
}
```

Here validation can be applied directly to the method parameter.

Spring MVC can raise:

```text
HandlerMethodValidationException
```

when method validation detects errors. ([Home][6])

This distinction is worth remembering:

```text
@Valid @RequestBody DTO
        ↓
MethodArgumentNotValidException

Direct method parameter constraints
        ↓
HandlerMethodValidationException
```

---

# 19. Other common Spring MVC exceptions

You should recognize these in an interview:

| Exception                                                       | Typical meaning                        | Typical HTTP status |
| --------------------------------------------------------------- | -------------------------------------- | ------------------: |
| `MethodArgumentNotValidException`                               | Request DTO validation failed          |                 400 |
| `HandlerMethodValidationException`                              | Method parameter validation failed     |                 400 |
| `HttpMessageNotReadableException`                               | Malformed JSON / body can't be read    |                 400 |
| `MethodArgumentTypeMismatchException` / `TypeMismatchException` | Parameter conversion failed            |                 400 |
| `MissingServletRequestParameterException`                       | Required query parameter missing       |                 400 |
| `HttpRequestMethodNotSupportedException`                        | Wrong HTTP method                      |                 405 |
| `HttpMediaTypeNotSupportedException`                            | Unsupported request `Content-Type`     |                 415 |
| `HttpMediaTypeNotAcceptableException`                           | Cannot satisfy requested response type |                 406 |
| `NoHandlerFoundException`                                       | No matching handler                    |                 404 |
| `NoResourceFoundException`                                      | Resource not found                     |                 404 |

Spring's default MVC exception resolver maps many known exceptions to HTTP status codes automatically. ([Home][7])

---

# 20. Example: malformed JSON

Suppose your API expects:

```json
{
  "name": "John",
  "age": 30
}
```

But the client sends:

```json
{
  "name": "John",
  "age":
}
```

Jackson cannot deserialize it.

Spring may raise:

```text
HttpMessageNotReadableException
```

which is typically a:

```http
400 Bad Request
```

Spring's `DefaultHandlerExceptionResolver` handles this class of MVC exceptions. ([Home][7])

---

# 21. Example: wrong path variable type

Controller:

```java
@GetMapping("/users/{id}")
public UserDto getUser(@PathVariable Long id) {
    ...
}
```

Request:

```text
GET /users/abc
```

Spring can't convert:

```text
"abc"
```

to:

```java
Long
```

so you can get a type-mismatch exception.

This is another example of why exception handling isn't just about exceptions thrown manually by your service.

Some exceptions are generated by Spring itself during request processing.

---

# 22. The exception resolver chain

This is the deeper Spring internals part.

`DispatcherServlet` uses a chain of:

```java
HandlerExceptionResolver
```

A resolver decides:

> "Can I handle this exception?"

Spring's built-in resolvers include:

### `ExceptionHandlerExceptionResolver`

Looks for:

```java
@ExceptionHandler
```

methods in:

* controllers
* controller advice classes

([Home][8])

### `ResponseStatusExceptionResolver`

Deals with:

```java
@ResponseStatus
```

and related response-status exception handling. ([Home][2])

### `DefaultHandlerExceptionResolver`

Handles standard Spring MVC exceptions and maps them to HTTP status codes. ([Home][7])

Conceptually:

```text
                    DispatcherServlet
                           |
                           v
              HandlerExceptionResolver
                       chain
                           |
             +-------------+-------------+
             |             |             |
             v             v             v
         @Exception     @Response     Default MVC
          Handler        Status        exceptions
```

---

# 23. What does `HandlerExceptionResolver` return?

The resolver contract allows it to:

```text
ModelAndView
```

or an empty `ModelAndView` when handled, or:

```text
null
```

to indicate that the exception wasn't resolved and the next resolver should get a chance. ([Home][9])

This is important because the chain can be thought of as:

```text
Exception
   |
   v
Resolver #1
   |
   +--> handled? YES --> response
   |
   NO
   |
   v
Resolver #2
   |
   +--> handled? YES --> response
   |
   NO
   |
   v
Resolver #3
```

Eventually, if nobody resolves it, it can bubble up to container-level error handling. ([Home][2])

---

# 24. Local handler vs global handler

Suppose:

```java
@RestController
public class UserController {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail localHandler(...) {
        ...
    }
}
```

and:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail globalHandler(...) {
        ...
    }
}
```

Which one is used?

Generally, the local controller handler is considered first.

Spring explicitly documents that global `@ExceptionHandler` methods from advice are applied after local controller methods. ([Home][3])

So mentally:

```text
Controller-level handler
        ↓
Global advice
```

This is useful when you want a special case in one controller while maintaining a global default.

---

# 25. Exception specificity

Suppose you have:

```java
@ExceptionHandler(RuntimeException.class)
public ProblemDetail handleRuntimeException(...) {
    ...
}
```

and:

```java
@ExceptionHandler(UserNotFoundException.class)
public ProblemDetail handleUserNotFound(...) {
    ...
}
```

and:

```java
UserNotFoundException extends RuntimeException
```

You generally want:

```text
UserNotFoundException
```

to be handled by the more specific handler.

This is one reason not to rely exclusively on:

```java
@ExceptionHandler(Exception.class)
```

A giant catch-all handler can easily make debugging and specialized responses harder.

---

# 26. A production-style global handler

Here's the pattern I'd expect you to be comfortable writing in an interview:

```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UserNotFoundException ex) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage());

        problem.setTitle("User Not Found");
        problem.setProperty("code", "USER_NOT_FOUND");

        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(
            IllegalStateException ex) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        ex.getMessage());

        problem.setTitle("Invalid State");
        problem.setProperty("code", "INVALID_STATE");

        return problem;
    }
}
```

The architecture becomes:

```text
Controller
    |
Service
    |
    +---- success ---> DTO
    |
    +---- exception --> GlobalExceptionHandler
                              |
                              v
                       ProblemDetail
                              |
                              v
                        HTTP response
```

---

# 27. Validation handler

A production API often wants validation errors in a more useful form.

Conceptually:

```java
@RestControllerAdvice
public class GlobalExceptionHandler
        extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed");

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        ));

        problem.setProperty("errors", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problem);
    }
}
```

The exact override signature can vary across major Spring generations, so in an actual project you should use the signature provided by the Spring version your application is compiling against. The conceptual pattern remains the same.

---

# 28. Custom exception + error code

I strongly recommend understanding this pattern conceptually.

Instead of only:

```java
"User not found"
```

your system can define:

```text
USER_NOT_FOUND
```

Then:

```json
{
  "type": "https://example.com/problems/user-not-found",
  "title": "User Not Found",
  "status": 404,
  "detail": "User 123 was not found",
  "code": "USER_NOT_FOUND"
}
```

Why is `code` useful?

Because text can change:

```text
"User not found"
```

could become:

```text
"No user exists with id 123"
```

But the machine-readable code remains:

```text
USER_NOT_FOUND
```

That allows clients to react to the semantic error instead of parsing English text.

---

# 29. Logging vs responding

A critical production rule:

> **Log detailed technical information on the server; return safe information to the client.**

Suppose database access fails:

```text
PSQLException:
duplicate key violates unique constraint ...
```

Don't blindly return the entire exception:

```json
{
  "message": "duplicate key violates unique constraint users_email_key ..."
}
```

That can reveal implementation details.

Instead:

```json
{
  "type": "...",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred",
  "code": "INTERNAL_ERROR",
  "traceId": "abc123"
}
```

Meanwhile the server logs the full stack trace.

This gives you:

```text
Client
  → safe error

Server logs
  → detailed technical error
```

---

# 30. Don't do this everywhere

A common beginner implementation is:

```java
try {
    service.doSomething();
} catch (Exception e) {
    return ResponseEntity
            .status(500)
            .body("Something went wrong");
}
```

inside every controller method.

For example:

```java
@GetMapping
public ResponseEntity<?> getUser() {
    try {
        ...
    } catch (...) {
        ...
    }
}
```

Then:

```java
@PostMapping
public ResponseEntity<?> createUser() {
    try {
        ...
    } catch (...) {
        ...
    }
}
```

Then:

```java
@PutMapping
public ResponseEntity<?> updateUser() {
    try {
        ...
    } catch (...) {
        ...
    }
}
```

This creates:

```text
duplication
+
inconsistent responses
+
large controllers
```

A global advice is usually cleaner.

---

# 31. `Exception` catch-all

You may still want a final safety net:

```java
@ExceptionHandler(Exception.class)
public ProblemDetail handleGenericException(Exception ex) {
    ...
}
```

This can prevent unexpected exceptions from producing an inconsistent response.

But don't make it your only handler.

Prefer:

```text
specific exceptions
        ↓
specific responses

unknown exceptions
        ↓
generic 500 response
```

For example:

```java
@ExceptionHandler(UserNotFoundException.class)
...
```

```java
@ExceptionHandler(OrderNotFoundException.class)
...
```

```java
@ExceptionHandler(InsufficientBalanceException.class)
...
```

Then finally:

```java
@ExceptionHandler(Exception.class)
...
```

---

# 32. Business exceptions vs technical exceptions

This distinction is excellent for interviews.

### Business/application exception

Example:

```text
UserNotFoundException
InsufficientBalanceException
OrderAlreadyCancelledException
DuplicateOrderException
```

These describe application semantics.

### Technical/infrastructure exception

Examples:

```text
SQLException
DataAccessException
TimeoutException
JsonProcessingException
```

These describe implementation/infrastructure failures.

You usually don't want your API contract to depend directly on database implementation exceptions.

Instead:

```text
Database exception
      ↓
application/service handling
      ↓
domain/application exception or appropriate error
      ↓
global HTTP mapping
```

---

# 33. One important design principle

Don't mix these responsibilities:

```text
Service:
    "User does not exist."

Global HTTP layer:
    "That becomes HTTP 404."
```

The service shouldn't necessarily have to know:

```java
HttpStatus.NOT_FOUND
```

unless that is deliberately part of your architecture.

This is a common clean-layering discussion in Spring interviews.

---

# 34. `ProblemDetail` vs custom Error DTO

You may encounter:

```java
public class ErrorResponse {
    private int status;
    private String message;
    private String code;
    private String path;
}
```

That's perfectly possible.

So why use `ProblemDetail`?

Because it aligns with a standard HTTP API error representation rather than inventing another format. Modern Spring has first-class support for RFC 9457 via `ProblemDetail` and related abstractions. ([Home][5])

But a custom DTO isn't "wrong."

The choice depends on:

```text
organization standards
existing API contract
framework version
client requirements
```

In an interview, don't say:

> "Custom DTO is wrong."

Say:

> "Spring provides first-class RFC 9457 support through `ProblemDetail`, which is useful for a standardized error representation. A custom DTO is also possible when an organization already has a different API contract."

That's a much stronger answer.

---

# 35. `ErrorResponse`

Modern Spring also introduces:

```java
ErrorResponse
```

This is an abstraction representing HTTP error response details including:

```text
HTTP status
HTTP headers
RFC 9457 response body
```

Spring MVC exceptions implement this contract. ([Home][5])

So conceptually:

```text
Exception
   |
   +--> may implement ErrorResponse
   |
   v
HTTP status + headers + ProblemDetail body
```

That's why modern Spring exception handling is much more structured than the old "just return a string" approach.

---

# 36. `ErrorResponseException`

There is also:

```java
ErrorResponseException
```

which serves as a convenient base implementation for exceptions that need to carry HTTP error-response information. ([Home][5])

This is more of a framework-level concept than something you need in every application.

For most business exceptions, you can still use:

```java
RuntimeException
```

plus:

```java
@RestControllerAdvice
```

to map them.

---

# 37. Content negotiation

This is a subtle point.

A `ProblemDetail` isn't merely a random Java object.

Spring has dedicated handling so that it participates in content negotiation using problem-detail media types such as:

```text
application/problem+json
```

for JSON responses. ([Home][5])

This is another reason modern Spring's error handling is more standardized.

---

# 38. `@ExceptionHandler` method parameters

An exception handler can accept the exception:

```java
@ExceptionHandler(UserNotFoundException.class)
public ProblemDetail handle(
        UserNotFoundException ex) {
    ...
}
```

The exception parameter itself can act as the mapping hint when no explicit exception class is specified.

Spring allows `@ExceptionHandler` methods to have flexible method signatures. ([Home][10])

For interview purposes, remember the simplest pattern:

```java
@ExceptionHandler(MyException.class)
public ResponseEntity<?> handle(MyException ex)
```

That's usually enough.

---

# 39. Handling exceptions from service layer

A very important misconception:

Some developers think:

> "`@ExceptionHandler` only catches exceptions thrown directly inside the controller."

Not exactly.

If the controller calls:

```java
service.getUser(id);
```

and the service throws:

```java
UserNotFoundException
```

that exception propagates through the call stack and can be handled by the controller's exception-handling infrastructure.

So:

```text
Controller
   |
   v
Service
   |
   X exception
   |
   v
DispatcherServlet exception handling
```

The exception does not need to be physically thrown from the controller method body.

---

# 40. What about repository exceptions?

Same principle.

Suppose:

```java
repository.save(entity);
```

causes:

```text
DataIntegrityViolationException
```

That can propagate upward.

A global handler can map it:

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ProblemDetail handleDataIntegrity(...) {
    ...
}
```

However, blindly exposing database-specific causes is usually a bad API design.

You may instead translate certain known persistence conditions into an application-specific exception.

---

# 41. A clean architecture

A good structure might look like:

```text
controller/
    UserController.java

service/
    UserService.java

exception/
    UserNotFoundException.java
    DuplicateUserException.java

handler/
    GlobalExceptionHandler.java
```

Example:

```text
UserController
      |
      v
UserService
      |
      +---- UserNotFoundException
      |
      v
GlobalExceptionHandler
      |
      v
ProblemDetail
```

This makes the codebase much easier to reason about.

---

# 42. Complete example

Let's put everything together.

### Exception

```java
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User " + id + " not found");
    }
}
```

### Service

```java
@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public UserDto getUser(Long id) {

        User user = repository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(id));

        return mapToDto(user);
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName()
        );
    }
}
```

### Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        return service.getUser(id);
    }
}
```

### Global handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(
            UserNotFoundException ex) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        ex.getMessage());

        problem.setTitle("User Not Found");
        problem.setProperty("code", "USER_NOT_FOUND");

        return problem;
    }
}
```

Request:

```http
GET /api/users/123
```

Flow:

```text
HTTP request
     ↓
DispatcherServlet
     ↓
UserController
     ↓
UserService
     ↓
Repository
     ↓
UserNotFoundException
     ↓
Exception handling infrastructure
     ↓
GlobalExceptionHandler
     ↓
ProblemDetail
     ↓
HTTP 404
```

Response:

```json
{
  "type": "about:blank",
  "title": "User Not Found",
  "status": 404,
  "detail": "User 123 not found",
  "code": "USER_NOT_FOUND"
}
```

The exact `type` value depends on how you configure the problem detail.

---

# 43. Another complete example — validation

DTO:

```java
public record CreateUserRequest(

        @NotBlank
        String name,

        @Email
        @NotBlank
        String email,

        @Min(18)
        int age
) {
}
```

Controller:

```java
@PostMapping
public UserDto createUser(
        @Valid @RequestBody CreateUserRequest request) {

    return service.createUser(request);
}
```

Global handling can convert validation failures into:

```json
{
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "errors": {
    "name": "must not be blank",
    "email": "must be a valid email",
    "age": "must be greater than or equal to 18"
  }
}
```

This is generally much more useful to frontend clients than:

```json
{
  "message": "Bad Request"
}
```

---

# 44. What belongs in a good API error response?

A practical contract might contain:

```text
type
title
status
detail
instance
code
traceId
errors
```

The first group maps to the standardized problem-detail model; application-specific properties such as `code`, `traceId`, and `errors` are extensions. ([Home][5])

Think of them like this:

| Field      | Purpose                                        |
| ---------- | ---------------------------------------------- |
| `type`     | Machine-readable problem type URI              |
| `title`    | Short human-readable summary                   |
| `status`   | HTTP status                                    |
| `detail`   | Explanation of this occurrence                 |
| `instance` | Specific occurrence/resource                   |
| `code`     | Your application's machine-readable error code |
| `traceId`  | Correlation/debugging identifier               |
| `errors`   | Validation-specific details                    |

---

# 45. Don't leak stack traces

Bad:

```json
{
  "exception": "org.postgresql.util.PSQLException",
  "stackTrace": "...",
  "sql": "insert into users ..."
}
```

Good:

```json
{
  "status": 500,
  "title": "Internal Server Error",
  "detail": "An unexpected error occurred",
  "code": "INTERNAL_ERROR",
  "traceId": "8f4c..."
}
```

And server logs contain:

```text
ERROR ... PSQLException
    at ...
    at ...
```

This separation is important for both security and maintainability.

---

# 46. Error handling flow — interview depth

Let's go one level deeper.

Suppose:

```java
@GetMapping("/users/{id}")
public UserDto getUser(@PathVariable Long id) {
    throw new UserNotFoundException(id);
}
```

The invocation fails.

The `DispatcherServlet` delegates exception handling to:

```java
HandlerExceptionResolver
```

One of the important resolvers is:

```java
ExceptionHandlerExceptionResolver
```

It searches for an applicable:

```java
@ExceptionHandler
```

method in:

```text
controller
        ↓
controller advice
```

If a matching method is found:

```text
exception
   ↓
@ExceptionHandler
   ↓
return value
   ↓
HTTP response
```

If not, another resolver can try.

That's the Spring MVC engine behind the annotations. ([Home][2])

---

# 47. Why `DispatcherServlet` is involved

You already studied `DispatcherServlet` in the REST/MVC topic.

You can now connect the concepts:

### Successful request

```text
DispatcherServlet
      ↓
HandlerMapping
      ↓
HandlerAdapter
      ↓
Controller
      ↓
Response
```

### Failed request

```text
DispatcherServlet
      ↓
HandlerMapping
      ↓
HandlerAdapter
      ↓
Controller
      ↓
Exception
      ↓
HandlerExceptionResolver
      ↓
Error Response
```

That's an excellent mental model.

---

# 48. What happens for a framework exception?

Consider:

```http
POST /users
Content-Type: application/json
```

with invalid JSON.

The controller may not even get your DTO.

Spring/Jackson fails while reading the request body.

So:

```text
DispatcherServlet
      ↓
argument resolution / message conversion
      ↓
HttpMessageNotReadableException
      ↓
exception resolver
      ↓
400
```

This is why global error handling must cover both:

```text
your application exceptions
```

and:

```text
Spring MVC exceptions
```

---

# 49. Why `ResponseEntityExceptionHandler` is useful here

Instead of manually writing:

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
...
@ExceptionHandler(MethodArgumentNotValidException.class)
...
@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
...
```

you can extend:

```java
ResponseEntityExceptionHandler
```

and customize relevant protected methods.

It is specifically designed as a base class for this sort of centralized MVC exception handling. ([Home][4])

This is a particularly important pattern for enterprise Spring applications.

---

# 50. Testing exception handling

Don't just test the happy path.

Using `MockMvc`, for example:

```java
mockMvc.perform(
        get("/api/users/999")
)
.andExpect(status().isNotFound());
```

You can also verify JSON:

```java
.andExpect(jsonPath("$.status").value(404))
.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
```

For validation:

```java
mockMvc.perform(
        post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson)
)
.andExpect(status().isBadRequest());
```

The important testing idea is:

```text
Given invalid condition
    ↓
expected exception
    ↓
expected HTTP status
    ↓
expected error contract
```

---

# 51. Common interview questions

## Q1. What is `@ExceptionHandler`?

> An annotation used on a controller or advice method to handle specific exceptions raised during request processing.

---

## Q2. What is `@ControllerAdvice`?

> A centralized component for cross-controller concerns such as exception handling, model attributes, and binder configuration. Its exception-handler methods can apply across controllers. ([Home][3])

---

## Q3. Difference between `@ControllerAdvice` and `@RestControllerAdvice`?

> `@RestControllerAdvice` combines `@ControllerAdvice` with `@ResponseBody`, so handler return values are written directly to the HTTP response body. ([Home][3])

---

## Q4. What happens when a controller throws an exception?

> `DispatcherServlet` delegates it to a chain of `HandlerExceptionResolver`s. One of them may invoke an `@ExceptionHandler`, another may process `@ResponseStatus`, and another may handle standard Spring MVC exceptions. ([Home][2])

---

## Q5. What is `HandlerExceptionResolver`?

> A Spring MVC strategy interface used to resolve exceptions thrown during request mapping or handler execution and turn them into alternative handling, typically an error response or error view. ([Home][9])

---

## Q6. What is `ResponseEntityExceptionHandler`?

> A convenient base class for global Spring MVC exception handling, especially within `@ControllerAdvice`. It provides handling for Spring MVC exceptions and can be customized by overriding specific methods. ([Home][4])

---

## Q7. What is `ProblemDetail`?

> Spring's representation of an RFC 9457 problem detail response, providing standardized HTTP API error fields with support for application-specific extension properties. ([Home][5])

---

## Q8. `@ResponseStatus` vs `@ExceptionHandler`?

`@ResponseStatus` is a simpler status mapping.

`@ExceptionHandler` gives you much more control over the actual response.

---

## Q9. Why not use `try-catch` in every controller?

Because it causes duplication and mixes error-handling policy with endpoint logic.

---

## Q10. Should service methods throw `ResponseStatusException`?

Usually, don't make your core service logic depend unnecessarily on HTTP concerns. Prefer application/domain exceptions and map them at the web boundary.

---

# 52. Common mistakes

### Mistake 1

```java
@ExceptionHandler(Exception.class)
```

and nothing else.

This loses useful specificity.

---

### Mistake 2

Returning:

```java
ex.getMessage()
```

for every internal exception.

That can leak sensitive implementation details.

---

### Mistake 3

Putting exception handling in every controller.

This produces duplication.

---

### Mistake 4

Using HTTP-specific exceptions deep inside business logic without considering layering.

For example:

```java
service
   ↓
ResponseStatusException
   ↓
HttpStatus.NOT_FOUND
```

This couples service code to MVC.

---

### Mistake 5

Returning different error JSON for every endpoint.

Clients hate this.

---

### Mistake 6

Forgetting validation exceptions.

Many applications handle business exceptions but completely overlook:

```text
MethodArgumentNotValidException
HandlerMethodValidationException
HttpMessageNotReadableException
```

---

# 53. The mental model you should memorize

Think in these four layers:

```text
1. Application throws exception
             ↓
2. DispatcherServlet catches/delegates
             ↓
3. HandlerExceptionResolver resolves
             ↓
4. HTTP error response is rendered
```

And within layer 3:

```text
@Controller
   @ExceptionHandler
        ↓
@ControllerAdvice / @RestControllerAdvice
        ↓
Spring MVC exception handling
        ↓
fallback/container error handling
```

---

# 54. Production recommendation

For a typical Spring Boot REST application, a clean architecture is:

```text
Custom application exceptions
        ↓
@RestControllerAdvice
        ↓
ResponseEntityExceptionHandler
        ↓
ProblemDetail / ErrorResponse
        ↓
standardized JSON
```

Example:

```text
UserNotFoundException
DuplicateUserException
OrderNotFoundException
BusinessRuleViolationException
        ↓
GlobalExceptionHandler
        ↓
404 / 409 / 422 / etc.
        ↓
ProblemDetail
```

This keeps:

```text
business logic
```

separate from:

```text
HTTP error translation
```

---

# 55. One nuance about HTTP status codes

Don't memorize:

```text
every exception X = always status Y
```

for your application.

For example, a business rule exception might reasonably map to different statuses depending on the API contract.

What you should know firmly is the common HTTP semantics:

```text
400 → malformed/invalid request
401 → authentication required/failed
403 → authenticated but forbidden
404 → resource not found
405 → method not allowed
406 → unacceptable response representation
409 → conflict
415 → unsupported media type
422 → semantically invalid request, when your API uses this convention
500 → unexpected server error
503 → service temporarily unavailable
```

For Spring MVC's own built-in exception mappings, use the framework's documented defaults; for domain exceptions, your API design determines the mapping. ([Home][7])

---

# 56. Final revision sheet

### Core annotations

```java
@ExceptionHandler
@ControllerAdvice
@RestControllerAdvice
@ResponseStatus
```

### Core classes/interfaces

```java
HandlerExceptionResolver
ExceptionHandlerExceptionResolver
ResponseStatusExceptionResolver
DefaultHandlerExceptionResolver
ResponseEntityExceptionHandler
ProblemDetail
ErrorResponse
ErrorResponseException
ResponseStatusException
```

### Most important relationship

```text
Exception
   ↓
DispatcherServlet
   ↓
HandlerExceptionResolver
   ↓
@ExceptionHandler / default handling
   ↓
HTTP response
```

### Best practical pattern

```java
public class UserNotFoundException
        extends RuntimeException {
}
```

then:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handle(...) {
        ...
    }
}
```

### Validation

```text
@Valid @RequestBody
        ↓
MethodArgumentNotValidException

method parameter constraints
        ↓
HandlerMethodValidationException
```

([Home][6])

---

# EPAM-level questions you should be able to answer now

By the end of this topic, you should be able to explain without notes:

1. What happens internally when a controller throws an exception?
2. How does `DispatcherServlet` handle exceptions?
3. What is `HandlerExceptionResolver`?
4. How does `@ExceptionHandler` work?
5. Difference between local `@ExceptionHandler` and `@ControllerAdvice`.
6. Difference between `@ControllerAdvice` and `@RestControllerAdvice`.
7. What is `ResponseEntityExceptionHandler`?
8. Why use custom exceptions?
9. `@ResponseStatus` vs `ResponseStatusException` vs `@ExceptionHandler`.
10. Why shouldn't the service layer necessarily throw `ResponseStatusException`?
11. How do you handle `MethodArgumentNotValidException`?
12. Difference between `MethodArgumentNotValidException` and `HandlerMethodValidationException`.
13. What is `ProblemDetail`?
14. What is RFC 9457?
15. How do you return validation errors in a consistent format?
16. Why shouldn't stack traces be returned to clients?
17. What happens if neither your handler nor Spring's resolvers handle an exception?
18. How would you design exception handling for a microservice REST API?

### The one answer to remember

> **`@ExceptionHandler` handles exceptions, `@ControllerAdvice` centralizes that handling across controllers, `@RestControllerAdvice` is the REST-oriented form, and `HandlerExceptionResolver` is the underlying Spring MVC mechanism used by `DispatcherServlet` to turn exceptions into HTTP responses. Modern Spring also provides `ProblemDetail`/`ErrorResponse` for standardized RFC 9457 error responses.** ([Home][5])

**Next topic: Spring Data JPA — Repository architecture, `JpaRepository`, persistence context, entity lifecycle, and how Spring Data actually generates repository implementations.**

[1]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet.html?utm_source=chatgpt.com "DispatcherServlet :: Spring Framework"
[2]: https://docs.spring.io/spring-framework/reference/7.1/web/webmvc/mvc-servlet/exceptionhandlers.html?utm_source=chatgpt.com "Exceptions :: Spring Framework"
[3]: https://docs.spring.io/spring-framework/reference/7.1/web/webmvc/mvc-controller/ann-advice.html?utm_source=chatgpt.com "Controller Advice :: Spring Framework"
[4]: https://docs.spring.io/spring-framework/docs/7.0.0-M1/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseEntityExceptionHandler.html?utm_source=chatgpt.com "ResponseEntityExceptionHandler (Spring Framework 7.0.0-M1 API)"
[5]: https://docs.spring.io/spring-framework/reference/7.1/web/webmvc/mvc-ann-rest-exceptions.html?utm_source=chatgpt.com "Error Responses :: Spring Framework"
[6]: https://docs.spring.io/spring-framework/reference/7.1/web/webmvc/mvc-controller/ann-validation.html?utm_source=chatgpt.com "Validation :: Spring Framework"
[7]: https://docs.spring.io/spring-framework/docs/7.0.0-M8/javadoc-api/org/springframework/web/servlet/mvc/support/DefaultHandlerExceptionResolver.html?utm_source=chatgpt.com "DefaultHandlerExceptionResolver (Spring Framework 7.0.0-M8 API)"
[8]: https://docs.spring.io/spring-framework/docs/7.0.0-M8/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ExceptionHandlerExceptionResolver.html?utm_source=chatgpt.com "ExceptionHandlerExceptionResolver (Spring Framework 7.0.0-M8 API)"
[9]: https://docs.spring.io/spring-framework/docs/7.0.0-M1/javadoc-api/org/springframework/web/servlet/HandlerExceptionResolver.html?utm_source=chatgpt.com "HandlerExceptionResolver (Spring Framework 7.0.0-M1 API)"
[10]: https://docs.spring.io/spring-framework/docs/7.0.0-M5/javadoc-api/org/springframework/web/bind/annotation/ExceptionHandler.html?utm_source=chatgpt.com "ExceptionHandler (Spring Framework 7.0.0-M5 API)"

