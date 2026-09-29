# Topic 13 — Spring MVC Architecture, `DispatcherServlet`, and the HTTP Request Lifecycle

This is one of the **most important practical Spring topics** for an EPAM interview.

We've learned how Spring creates and wires your beans. Now we're going to see what happens when an actual HTTP request reaches your application.

Suppose a client sends:

```http
GET /api/orders/123
```

and you have:

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }
}
```

The interesting question is:

> **How does `/api/orders/123` ultimately reach `getOrder()`?**

The simplified answer is:

```text
HTTP Client
    ↓
Embedded Web Server
    ↓
Servlet Filters
    ↓
DispatcherServlet
    ↓
HandlerMapping
    ↓
HandlerAdapter
    ↓
Controller Method
    ↓
Service
    ↓
Repository
    ↓
Return Object
    ↓
HttpMessageConverter
    ↓
JSON Response
```

Spring MVC is designed around the **Front Controller pattern**, where `DispatcherServlet` is the central servlet and delegates actual request-processing work to specialized components. ([Home][1])

---

# 1. First: What Is Spring MVC?

Spring MVC is Spring's **Servlet-based web framework**.

It provides:

```text
HTTP request handling
Controller mapping
Request parameter binding
Validation
Exception handling
View rendering
HTTP message conversion
Interceptors
Filters integration
```

At the center:

```text
                DispatcherServlet
                       |
        ┌──────────────┼──────────────┐
        ↓              ↓              ↓
HandlerMapping   HandlerAdapter   ExceptionResolver
        |
        ↓
Controllers
```

Spring MVC uses a set of well-defined strategy interfaces so that different request-mapping, invocation, exception-handling, and rendering implementations can participate. ([Home][2])

---

# 2. Spring MVC vs Spring Boot

Don't confuse these.

### Spring MVC

The web framework:

```text
DispatcherServlet
Controller
HandlerMapping
HandlerAdapter
HttpMessageConverter
ViewResolver
```

### Spring Boot

The application/bootstrap layer that makes using Spring MVC easier through:

```text
auto-configuration
embedded server
starter dependencies
external configuration
etc.
```

So:

```text
Spring Boot
    ↓
configures Spring MVC
    ↓
Spring MVC handles HTTP requests
```

This connects directly to the auto-configuration topic.

---

# 3. Where Does `DispatcherServlet` Come From?

When you're using Spring Boot with the servlet web stack, Boot configures the web infrastructure and registers the `DispatcherServlet`.

You generally don't write:

```java
new DispatcherServlet(...)
```

in a normal Boot application.

The servlet container receives the HTTP request and routes it to `DispatcherServlet`.

Spring MVC's documentation describes `DispatcherServlet` as the central servlet implementing the Front Controller pattern. ([Home][1])

---

# 4. What Is the Front Controller Pattern?

Without a front controller, you could imagine:

```text
Request A → Controller A
Request B → Controller B
Request C → Controller C
```

Instead, Spring MVC uses:

```text
                   DispatcherServlet
                         |
          ┌──────────────┼──────────────┐
          ↓              ↓              ↓
      Controller A   Controller B   Controller C
```

Every request first reaches one central entry point.

That central entry point coordinates the rest of the processing.

That's the **Front Controller pattern**.

---

# 5. Why Is This Useful?

Because common concerns can be centralized:

```text
request mapping
exception handling
interceptors
locale
multipart handling
response rendering
```

instead of implementing them independently inside every controller.

Spring's documentation explicitly describes `DispatcherServlet` as providing a shared request-processing algorithm while delegating actual work to configurable components. ([Home][1])

---

# 6. The Big Request Flow

Let's follow:

```http
GET /api/orders/123
Accept: application/json
```

through the application.

```text
Client
  ↓
HTTP request
  ↓
Tomcat / servlet container
  ↓
Filter chain
  ↓
DispatcherServlet
  ↓
HandlerMapping
  ↓
HandlerExecutionChain
  ↓
HandlerAdapter
  ↓
Controller method
  ↓
Service
  ↓
Repository
  ↓
Order object
  ↓
HttpMessageConverter
  ↓
JSON
  ↓
HTTP response
```

Now let's break every piece down.

---

# 7. Step 1 — Client Sends HTTP Request

For example:

```http
GET /api/orders/123 HTTP/1.1
Host: example.com
Accept: application/json
Authorization: Bearer ...
```

The request reaches the server process.

At this point we're still in the Servlet/container world.

---

# 8. Step 2 — Embedded Server Receives Request

In a typical Spring Boot servlet application:

```text
HTTP
 ↓
embedded servlet container
 ↓
Servlet processing
```

Depending on the configuration, the server might be Tomcat or another supported servlet container.

The container is responsible for low-level servlet concerns such as:

```text
network connection
HTTP parsing
request/response objects
servlet invocation
filter chain
```

Spring MVC operates **on top of the Servlet API**.

---

# 9. Step 3 — Servlet Filters

Before the request reaches `DispatcherServlet`, Servlet filters can execute.

For example:

```java
@Component
public class LoggingFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        System.out.println("Before");

        chain.doFilter(request, response);

        System.out.println("After");
    }
}
```

Conceptually:

```text
Request
  ↓
Filter A
  ↓
Filter B
  ↓
DispatcherServlet
  ↓
...
  ↓
response
  ↑
Filter B
  ↑
Filter A
```

Servlet filters are designed to apply interception-style logic before and after the target servlet and the rest of the filter chain. ([Home][3])

---

# 10. Filter vs Interceptor

This is a **classic Spring interview question**.

### Filter

Servlet-level:

```text
Servlet container
   ↓
Filter
   ↓
DispatcherServlet
```

### HandlerInterceptor

Spring MVC-level:

```text
DispatcherServlet
   ↓
HandlerInterceptor
   ↓
Controller
```

So:

```text
Filter
  → Servlet/container layer

Interceptor
  → Spring MVC handler layer
```

Spring's MVC documentation says `HandlerInterceptor` operates around the handler execution chain, while Servlet `Filter` operates around the target servlet. ([Home][3])

We'll later have a dedicated topic for filters/interceptors because the differences become important in security, logging, and tracing.

---

# 11. Step 4 — `DispatcherServlet`

Eventually the request reaches:

```text
DispatcherServlet
```

Now Spring MVC takes over.

Remember:

```text
DispatcherServlet
     =
central request coordinator
```

It does **not** contain every piece of logic itself.

Instead, it delegates to other components.

Spring's docs call these components the servlet's **special beans**. ([Home][2])

---

# 12. The Important `DispatcherServlet` Delegates

You should know these:

```text
HandlerMapping
HandlerAdapter
HandlerExceptionResolver
ViewResolver
LocaleResolver
MultipartResolver
FlashMapManager
```

The first three are especially important for interviews.

Spring's official documentation lists these special bean types and their responsibilities. ([Home][2])

---

# 13. Step 5 — `HandlerMapping`

The first major question is:

> **Which controller/handler should process this request?**

That's the job of:

```text
HandlerMapping
```

For example:

```http
GET /api/orders/123
```

Spring needs to find:

```java
OrderController.getOrder(...)
```

A major implementation is:

```text
RequestMappingHandlerMapping
```

which supports `@RequestMapping`-style annotated controller methods. ([Home][2])

---

# 14. What Does `RequestMappingHandlerMapping` Know?

Suppose:

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        ...
    }
}
```

Spring effectively has mapping information corresponding to:

```text
HTTP method = GET
Path = /api/orders/{id}
Handler = OrderController#getOrder
```

During application startup, Spring processes controller mappings and builds the data needed to resolve incoming requests.

Then at request time:

```text
GET /api/orders/123
       ↓
RequestMappingHandlerMapping
       ↓
OrderController#getOrder
```

Spring's request-mapping documentation describes `@RequestMapping` as mapping requests to controller methods based on path, HTTP method, request parameters, headers, and media types. ([Home][4])

---

# 15. What Is a Handler?

A **handler** is the thing Spring has identified to process the request.

For an annotated MVC controller, it is typically represented as a:

```text
HandlerMethod
```

conceptually containing:

```text
controller bean
+
method
+
method metadata
```

So:

```text
Request
   ↓
HandlerMapping
   ↓
HandlerMethod
   ↓
OrderController.getOrder()
```

You don't normally manipulate `HandlerMethod` yourself, but understanding that the mapping resolves a specific handler method is useful.

---

# 16. HandlerExecutionChain

The result from a `HandlerMapping` is not simply:

```text
OrderController.getOrder()
```

It can include:

```text
handler
+
interceptors
```

This is effectively a:

```text
HandlerExecutionChain
```

Conceptually:

```text
HandlerExecutionChain

    Handler
       +
    Interceptor A
       +
    Interceptor B
       +
    Interceptor C
```

Spring's documentation says `HandlerMapping` maps a request to a handler **along with a list of interceptors** for pre/post-processing. ([Home][2])

---

# 17. Step 6 — Interceptors

Suppose:

```java
@Component
public class TimingInterceptor
        implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        return true;
    }

    @Override
    public void postHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            ModelAndView modelAndView) {
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {
    }
}
```

The main lifecycle is:

```text
preHandle
    ↓
controller
    ↓
postHandle
    ↓
afterCompletion
```

If:

```java
preHandle(...)
```

returns:

```java
false
```

the handler is not invoked and the remainder of that handler execution chain is bypassed. ([Home][5])

---

# 18. Important Interceptor Caveat

For REST controllers:

```java
@ResponseBody
ResponseEntity
```

the response may already be written and committed **inside the `HandlerAdapter` before `postHandle` executes**.

Therefore, `postHandle` is too late to modify that response body/header in those cases.

Spring recommends `ResponseBodyAdvice` when you need to customize a response body from that part of the MVC pipeline. ([Home][5])

That's an excellent advanced interview detail.

---

# 19. Step 7 — `HandlerAdapter`

Now `DispatcherServlet` has:

```text
HandlerMethod
```

But the servlet doesn't want to know how every kind of handler should be invoked.

That's why we have:

```text
HandlerAdapter
```

Its job is essentially:

> **Adapt the selected handler into something `DispatcherServlet` can invoke.**

Spring's documentation explicitly says that `HandlerAdapter` shields `DispatcherServlet` from the details of how a handler is actually invoked, including annotation-based controller invocation. ([Home][2])

---

# 20. Why Do We Need `HandlerAdapter`?

Imagine Spring supported multiple handler styles:

```text
annotated controller
HttpRequestHandler
some legacy handler
custom handler
```

If `DispatcherServlet` directly understood all of them:

```text
if annotated controller...
else if legacy...
else if custom...
...
```

the servlet would become tightly coupled to every handler type.

Instead:

```text
DispatcherServlet
      ↓
HandlerAdapter
      ↓
specific handler invocation
```

This is a classic **Adapter pattern**.

That's why the name is:

```text
HandlerAdapter
```

---

# 21. Annotated Controller Adapter

For annotated controllers, the important implementation is:

```text
RequestMappingHandlerAdapter
```

Conceptually:

```text
DispatcherServlet
       ↓
RequestMappingHandlerAdapter
       ↓
resolve controller method arguments
       ↓
invoke controller
       ↓
process return value
```

This is where much of the magic behind:

```java
@RequestParam
@PathVariable
@RequestBody
@ResponseBody
```

happens.

---

# 22. Step 8 — Controller Method Arguments

Suppose:

```java
@GetMapping("/{id}")
public Order getOrder(
        @PathVariable Long id,
        @RequestHeader("X-Request-ID") String requestId) {

    ...
}
```

Spring has to produce:

```text
id = 123
requestId = "abc-456"
```

from the HTTP request.

It doesn't happen through normal Java parameter passing.

Spring MVC uses **argument resolvers** to resolve controller method arguments. The framework supports many argument types, including `@PathVariable`, `@RequestParam`, `@RequestHeader`, `@RequestBody`, `@ModelAttribute`, and others. ([Home][6])

---

# 23. `HandlerMethodArgumentResolver`

Conceptually:

```text
Controller method parameter
          ↓
Can some resolver handle it?
          ↓
Yes
          ↓
resolve argument
```

Examples:

```text
@PathVariable
    ↓
Path variable resolver

@RequestParam
    ↓
Request parameter resolver

@RequestHeader
    ↓
Request header resolver

@RequestBody
    ↓
Request response/body infrastructure
    +
HttpMessageConverter
```

You don't need to memorize every resolver class.

The important concept:

> **Spring examines each controller method parameter and uses the appropriate argument-resolution mechanism to produce its value.**

---

# 24. `@PathVariable`

Suppose:

```java
@GetMapping("/orders/{id}")
public Order getOrder(
        @PathVariable Long id) {
    ...
}
```

Request:

```http
GET /orders/123
```

Spring extracts:

```text
{id = "123"}
```

then converts:

```text
"123"
 ↓
Long
 ↓
123
```

Spring MVC performs type conversion for string-based request values such as `@PathVariable`, `@RequestParam`, headers, cookies, and similar inputs. ([Home][7])

---

# 25. `@RequestParam`

Example:

```java
@GetMapping("/orders")
public List<Order> findOrders(
        @RequestParam String status) {
}
```

Request:

```http
GET /orders?status=PAID
```

Spring resolves:

```text
status
   ↓
"PAID"
```

and converts it to the declared Java type.

For example:

```java
@RequestParam Integer page
```

can turn:

```text
?page=3
```

into:

```text
Integer 3
```

Spring's method-argument documentation describes `@RequestParam` as access to request parameters, with conversion to the declared argument type. ([Home][6])

---

# 26. `@RequestHeader`

Example:

```java
@GetMapping
public Order getOrder(
        @RequestHeader("X-Correlation-ID")
        String correlationId) {
}
```

Request:

```http
X-Correlation-ID: abc123
```

Spring resolves:

```text
correlationId = "abc123"
```

Again, type conversion can occur if the declared argument type is not `String`. ([Home][6])

---

# 27. `@CookieValue`

Similarly:

```java
@GetMapping
public String get(
        @CookieValue("sessionId")
        String sessionId) {
}
```

Spring extracts the cookie.

This is another example of method-argument resolution.

---

# 28. The Most Important One — `@RequestBody`

Suppose:

```java
@PostMapping("/orders")
public Order createOrder(
        @RequestBody CreateOrderRequest request) {
    ...
}
```

Client sends:

```json
{
  "productId": 100,
  "quantity": 2
}
```

How does JSON become:

```java
CreateOrderRequest
```

?

Through:

```text
HttpMessageConverter
```

Spring's documentation explicitly states that `@RequestBody` causes the request body to be read and deserialized into an object through an `HttpMessageConverter`. ([Home][8])

---

# 29. JSON Request Flow

This is worth memorizing:

```text
JSON HTTP body
      ↓
@RequestBody
      ↓
HttpMessageConverter
      ↓
JSON deserialization
      ↓
CreateOrderRequest object
      ↓
controller method
```

If Jackson is configured as the JSON converter, Jackson performs the actual JSON ↔ Java mapping.

So:

```text
Spring MVC
     ↓
chooses message converter
     ↓
Jackson converter
     ↓
Jackson
     ↓
Java object
```

---

# 30. What Is `HttpMessageConverter`?

It is an abstraction for:

```text
HTTP body
   ↔
Java object / other representation
```

Spring's framework documentation defines `HttpMessageConverter` as the interface for reading and writing HTTP request and response bodies. ([Home][9])

Conceptually:

```text
Request:

HTTP Body
   ↓
Converter
   ↓
Java object


Response:

Java object
   ↓
Converter
   ↓
HTTP Body
```

This is one of the most important Spring MVC interfaces.

---

# 31. JSON Converter

For JSON, the application commonly uses a Jackson-backed HTTP message converter.

For example:

```text
Content-Type: application/json
```

and:

```java
@RequestBody OrderRequest request
```

means:

```text
application/json
      ↓
JSON converter
      ↓
OrderRequest
```

And when returning:

```java
OrderResponse
```

with `@ResponseBody`/`@RestController`:

```text
OrderResponse
      ↓
JSON converter
      ↓
application/json
```

Spring documents that `@ResponseBody` return values are serialized through `HttpMessageConverter`s. ([Home][10])

---

# 32. Step 9 — Controller Method Executes

Now all the parameters have been resolved.

Spring invokes:

```java
OrderController.getOrder(123L)
```

At this point, normal Java application code runs:

```text
Controller
    ↓
Service
    ↓
Repository
```

Spring MVC doesn't automatically force you to use a service/repository architecture.

That's your application design.

The framework's responsibility at this point is primarily web request handling.

---

# 33. Controller → Service

Typical:

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(
            @PathVariable Long id) {

        return orderService.getOrder(id);
    }
}
```

Then:

```text
Controller
     ↓
OrderService
```

This is normal Spring DI, not something intrinsically required by MVC.

---

# 34. Service → Repository

Then:

```java
@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

Then:

```text
Controller
   ↓
Service
   ↓
Repository
   ↓
Database
```

Again, this is application architecture layered on top of Spring.

---

# 35. Step 10 — Controller Returns

Suppose:

```java
@GetMapping("/{id}")
public OrderResponse getOrder(
        @PathVariable Long id) {

    return new OrderResponse(...);
}
```

Now Spring has:

```text
OrderResponse object
```

But the client asked for HTTP.

How does Java become JSON?

---

# 36. `@ResponseBody`

For:

```java
@GetMapping("/{id}")
@ResponseBody
public OrderResponse getOrder(...) {
    ...
}
```

Spring writes the return value to the response body through an `HttpMessageConverter`. ([Home][10])

With:

```java
@RestController
```

the response-body semantics are applied to controller methods automatically because `@RestController` combines `@Controller` and `@ResponseBody`. ([Home][10])

---

# 37. `@RestController`

So:

```java
@RestController
public class OrderController {
}
```

is effectively:

```text
@Controller
+
@ResponseBody
```

at the class level.

Therefore:

```java
@GetMapping
public OrderResponse getOrder() {
    return response;
}
```

is interpreted as:

```text
return object
   ↓
message converter
   ↓
HTTP response body
```

rather than:

```text
return String
   ↓
view name
```

Spring explicitly documents this composed-annotation behavior. ([Home][10])

---

# 38. `@Controller` Behaves Differently

Consider:

```java
@Controller
public class OrderPageController {

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }
}
```

Here:

```text
"orders"
```

is interpreted as a logical view name.

The process becomes:

```text
controller
   ↓
"orders"
   ↓
ViewResolver
   ↓
actual View
   ↓
HTML response
```

Spring's return-value documentation distinguishes a `String` view name from `@ResponseBody`, where the latter is written through message converters. ([Home][11])

---

# 39. ViewResolver

For traditional MVC applications:

```text
Controller
    ↓
logical view name
    ↓
ViewResolver
    ↓
actual View
```

For example:

```text
"orders"
```

might resolve to something like:

```text
templates/orders.html
```

depending on the view technology/configuration.

`ViewResolver` is one of the special beans that `DispatcherServlet` delegates to for resolving logical view names. ([Home][2])

For a pure REST API:

```text
ViewResolver
```

is usually not part of the normal response path because the response is written directly by message conversion.

---

# 40. REST Response Flow

For our REST example:

```text
Controller
    ↓
OrderResponse object
    ↓
@ResponseBody semantics
    ↓
RequestMappingHandlerAdapter
    ↓
HttpMessageConverter
    ↓
JSON
    ↓
HTTP response
```

This is one of the most important diagrams in Spring MVC.

---

# 41. Content Negotiation

Now suppose the client sends:

```http
Accept: application/json
```

Spring MVC considers the requested media type when determining what representation to produce.

You can explicitly constrain an endpoint:

```java
@GetMapping(
    value = "/{id}",
    produces = "application/json"
)
```

Spring MVC supports `produces` and `consumes` conditions as part of request mapping and uses media types for content negotiation. ([Home][4])

---

# 42. `consumes` vs `produces`

This is a classic interview question.

### `consumes`

Describes the **request body media type** the endpoint accepts.

```java
@PostMapping(
    value = "/orders",
    consumes = "application/json"
)
```

Means:

```text
"I accept JSON request bodies."
```

### `produces`

Describes the **response media type** the endpoint can produce.

```java
@GetMapping(
    value = "/orders/{id}",
    produces = "application/json"
)
```

Means:

```text
"I produce JSON."
```

So:

```text
consumes
   → incoming representation

produces
   → outgoing representation
```

---

# 43. `Accept` vs `Content-Type`

Another extremely common question.

### `Content-Type`

Describes:

> **What format is this request body?**

Example:

```http
Content-Type: application/json
```

### `Accept`

Describes:

> **What response formats can the client accept?**

Example:

```http
Accept: application/json
```

So:

```text
Content-Type
     ↓
request body

Accept
     ↓
desired response
```

This distinction is fundamental to REST/API interviews.

---

# 44. Step 11 — Exception Handling

Suppose controller/service throws:

```java
throw new OrderNotFoundException(id);
```

The request processing doesn't simply crash.

Spring MVC has:

```text
HandlerExceptionResolver
```

which participates in translating exceptions into appropriate responses or views. Spring documents `HandlerExceptionResolver` as the strategy for resolving exceptions raised during request processing. ([Home][2])

---

# 45. `@ExceptionHandler`

You might write:

```java
@ExceptionHandler(OrderNotFoundException.class)
@ResponseStatus(HttpStatus.NOT_FOUND)
public ErrorResponse handle(
        OrderNotFoundException ex) {

    return new ErrorResponse(ex.getMessage());
}
```

Spring detects and invokes the appropriate exception-handler method.

We'll later dedicate an entire topic to:

```text
@ExceptionHandler
@ControllerAdvice
@RestControllerAdvice
ResponseEntity
ProblemDetail
```

because that's an important EPAM REST question.

---

# 46. Global Exception Handling

Instead of putting:

```java
@ExceptionHandler
```

inside every controller, use:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

Conceptually:

```text
Controller A ──┐
Controller B ──┼──> Global exception handling
Controller C ──┘
```

This is one of the most common Spring REST patterns.

We will cover it separately.

---

# 47. Step 12 — Response Is Written

Eventually:

```text
Java object
   ↓
HttpMessageConverter
   ↓
HTTP body
```

For example:

```json
{
  "id": 123,
  "status": "PAID"
}
```

and headers:

```http
Content-Type: application/json
```

The servlet/container then sends the HTTP response back to the client.

---

# 48. Complete REST Request Example

Suppose:

```http
GET /api/orders/123
Accept: application/json
```

Controller:

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping("/{id}")
    public OrderResponse getOrder(
            @PathVariable Long id) {

        return orderService.getOrder(id);
    }
}
```

Now trace it:

```text
1. Client
      ↓
2. HTTP GET /api/orders/123
      ↓
3. Servlet container
      ↓
4. Filter chain
      ↓
5. DispatcherServlet
      ↓
6. RequestMappingHandlerMapping
      ↓
7. HandlerMethod
   OrderController#getOrder
      ↓
8. HandlerInterceptor.preHandle()
      ↓
9. RequestMappingHandlerAdapter
      ↓
10. Resolve @PathVariable
      ↓
11. Invoke controller
      ↓
12. OrderService
      ↓
13. Repository
      ↓
14. OrderResponse returned
      ↓
15. @ResponseBody semantics
      ↓
16. HttpMessageConverter
      ↓
17. JSON
      ↓
18. HTTP response
      ↓
19. Interceptor completion
      ↓
20. Client
```

This is the **core Spring MVC request lifecycle**.

---

# 49. POST Request With `@RequestBody`

Now let's trace a POST:

```http
POST /api/orders
Content-Type: application/json

{
  "productId": 100,
  "quantity": 2
}
```

Controller:

```java
@PostMapping
public OrderResponse create(
        @Valid @RequestBody CreateOrderRequest request) {

    return orderService.create(request);
}
```

Flow:

```text
HTTP request
     ↓
DispatcherServlet
     ↓
HandlerMapping
     ↓
HandlerAdapter
     ↓
@RequestBody
     ↓
HttpMessageConverter
     ↓
JSON → CreateOrderRequest
     ↓
Validation
     ↓
Controller
     ↓
Service
     ↓
response object
     ↓
HttpMessageConverter
     ↓
JSON response
```

Spring explicitly documents `@RequestBody` deserialization through `HttpMessageConverter`, and combining it with `@Valid` triggers Bean Validation; validation failures normally result in `MethodArgumentNotValidException` and a 400 response. ([Home][8])

---

# 50. Where Does Validation Happen?

Suppose:

```java
public record CreateOrderRequest(
        @NotNull Long productId,
        @Positive int quantity
) {}
```

and:

```java
@PostMapping
public OrderResponse create(
        @Valid @RequestBody CreateOrderRequest request) {
}
```

Conceptually:

```text
JSON
 ↓
message conversion
 ↓
CreateOrderRequest
 ↓
validation
 ↓
if valid → controller
if invalid → exception handling
```

Spring MVC integrates Bean Validation when it is present. ([Home][12])

---

# 51. What Happens If JSON Is Invalid?

Suppose:

```json
{
  "productId":
}
```

Jackson/message conversion can't create the target object.

Conceptually:

```text
HTTP body
   ↓
HttpMessageConverter
   ↓
deserialization failure
   ↓
request processing exception
   ↓
exception resolver
   ↓
error response
```

The exact exception and final response depend on the framework version/configuration and your exception handling.

The important thing is:

> **Invalid request body can fail during message conversion before your controller method executes.**

This is an excellent interview scenario.

---

# 52. `@RequestBody` vs `@RequestParam`

Another very common question:

### `@RequestParam`

For request parameters:

```http
/orders?status=PAID
```

```java
@RequestParam String status
```

### `@RequestBody`

For HTTP body:

```json
{
  "status": "PAID"
}
```

```java
@RequestBody OrderRequest request
```

Spring's method-argument documentation distinguishes request parameters from HTTP body access. ([Home][6])

---

# 53. `@PathVariable` vs `@RequestParam`

### Path variable

```http
/orders/123
```

```java
@PathVariable Long id
```

Used when the value is part of the URI structure.

### Request parameter

```http
/orders?id=123
```

```java
@RequestParam Long id
```

Used as a query/form parameter.

Conceptually:

```text
Path:
/orders/{id}

Query:
/orders?id=123
```

This is basic REST knowledge, but absolutely interview-worthy.

---

# 54. A Very Interesting Point: Simple Parameters Without Annotations

Spring MVC can sometimes resolve a simple method parameter as a request parameter even without explicitly writing `@RequestParam`.

For example:

```java
@GetMapping
public Order find(String status) {
}
```

Spring's current method-argument documentation states that an otherwise-unmatched simple argument can be resolved as a request parameter. ([Home][6])

However, in production code:

```java
@RequestParam String status
```

is generally clearer and communicates intent explicitly.

---

# 55. `@ModelAttribute`

For MVC form-style applications, you may see:

```java
@PostMapping("/orders")
public String create(
        @ModelAttribute OrderForm form) {
}
```

Spring binds request values to the model object.

Conceptually:

```text
request parameters
      ↓
data binding
      ↓
OrderForm
```

Spring's documentation describes `@ModelAttribute` as binding request parameters, URI variables, and headers to a model object, with data binding and validation. ([Home][13])

This is especially common in traditional server-side HTML applications.

---

# 56. `@RequestBody` vs `@ModelAttribute`

This is another excellent interview question.

### JSON API

```java
@RequestBody OrderRequest request
```

Conceptually:

```text
JSON
 ↓
HttpMessageConverter
 ↓
OrderRequest
```

### Form-style request

```java
@ModelAttribute OrderForm form
```

Conceptually:

```text
request parameters
 ↓
data binding
 ↓
OrderForm
```

Don't confuse the two.

---

# 57. `HandlerAdapter` Does More Than Invoke the Method

This is an important deeper point.

Don't imagine:

```text
HandlerAdapter
   ↓
method.invoke()
```

and nothing else.

For annotated controllers, it coordinates things such as:

```text
argument resolution
type conversion
validation
controller invocation
return-value handling
message conversion
```

So:

```text
RequestMappingHandlerAdapter
```

is effectively the engine behind much of the annotated-controller programming model.

---

# 58. `HandlerMethodArgumentResolver`

Examples of things it can resolve:

```text
@PathVariable
@RequestParam
@RequestHeader
@CookieValue
@RequestBody
@ModelAttribute
HttpServletRequest
HttpServletResponse
Principal
SessionStatus
Model
```

The current Spring MVC documentation lists a large range of supported controller method arguments. ([Home][6])

This means controller methods are deliberately flexible:

```java
public ResponseEntity<Order> get(
        @PathVariable Long id,
        @RequestHeader String token,
        HttpServletRequest request) {
}
```

Spring figures out how to supply each argument.

---

# 59. Custom Argument Resolvers

This is an advanced but practical capability.

Suppose you want:

```java
@GetMapping
public Order get(
        @CurrentUser User user) {
}
```

You can create a custom:

```text
HandlerMethodArgumentResolver
```

to resolve:

```java
@CurrentUser
```

from the request/security context.

Then:

```text
@CurrentUser
    ↓
custom resolver
    ↓
User object
```

Spring MVC allows custom argument resolvers through MVC configuration. This is a useful example of how the framework's strategy-based architecture is extensible. ([Home][14])

---

# 60. Custom Converters

Likewise, you may need:

```text
String → CustomerId
```

instead of:

```text
String → Long
```

You can register a custom `Converter` or formatter.

Spring MVC provides a configurable conversion service and supports custom formatters/converters. ([Home][15])

For example:

```java
@Component
public class CustomerIdConverter
        implements Converter<String, CustomerId> {

    @Override
    public CustomerId convert(String source) {
        return new CustomerId(source);
    }
}
```

Then:

```java
@GetMapping("/{customerId}")
public Customer get(
        @PathVariable CustomerId customerId) {
}
```

Spring can perform the conversion.

---

# 61. `DispatcherServlet` Doesn't Directly Search Your Controllers

This is a subtle interview correction.

Don't say:

> "DispatcherServlet scans controllers and finds the URL."

More accurately:

```text
DispatcherServlet
     ↓
HandlerMapping
     ↓
find matching handler
```

Component scanning and controller mapping registration happen through Spring configuration infrastructure.

At request time, `DispatcherServlet` **delegates to `HandlerMapping`** rather than manually scanning controller classes on every request. ([Home][2])

This is much more accurate.

---

# 62. Does Spring Scan Controllers on Every Request?

No.

You wouldn't want:

```text
every request
    ↓
scan thousands of classes
    ↓
find controller
```

Instead, mapping metadata is established as part of application setup, and request-time handling uses the registered mappings.

Conceptually:

```text
startup
  ↓
discover controller mappings
  ↓
build mapping registry

request
  ↓
lookup registered mapping
```

This is an important performance/design concept.

---

# 63. `@RequestMapping`

You can write:

```java
@RequestMapping(
    value = "/orders",
    method = RequestMethod.GET
)
```

But Spring provides shortcut annotations:

```java
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
@PatchMapping
```

These are composed annotations built on `@RequestMapping`. Spring's documentation explicitly describes them as shortcut/composed variants. ([Home][4])

---

# 64. Class-Level and Method-Level Mapping

Example:

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id) {
    }

    @PostMapping
    public Order create(
            @RequestBody CreateOrderRequest request) {
    }
}
```

Spring combines:

```text
class-level:
 /api/orders

method-level:
 /{id}
```

resulting in:

```text
GET  /api/orders/{id}
POST /api/orders
```

Spring's request-mapping documentation describes class-level mappings as shared mappings that method-level mappings further narrow. ([Home][4])

---

# 65. Request Matching Is More Than URL

A mapping can consider:

```text
path
HTTP method
request parameters
headers
consumes
produces
```

For example:

```java
@PostMapping(
    value = "/orders",
    consumes = "application/json",
    produces = "application/json"
)
```

So Spring can distinguish not just:

```text
"/orders"
```

but:

```text
"/orders"
+
POST
+
JSON
```

Spring explicitly documents these request-mapping conditions. ([Home][4])

---

# 66. What Happens If No Controller Mapping Matches?

Then no suitable handler is found.

Depending on the servlet configuration and framework behavior, Spring MVC can result in a 404/not-found handling path.

Modern Spring MVC has specific configuration around `NoHandlerFoundException`, and the official docs discuss the handling of unmatched requests and default servlet behavior. ([Home][16])

For a typical REST API, the observable result is:

```http
404 Not Found
```

unless some custom infrastructure handles it differently.

---

# 67. What Happens If Two Mappings Match?

Suppose:

```java
@GetMapping("/orders/{id}")
```

and another mapping could also match the same request.

Spring MVC applies mapping specificity rules to determine the most suitable mapping.

If there is an unresolved ambiguity, application startup or request mapping registration can fail with an ambiguous mapping problem, depending on the exact conflict.

The important interview point:

> Spring's mapping system attempts to select the most specific matching handler; genuinely ambiguous mappings are an error rather than an arbitrary choice.

---

# 68. `DispatcherServlet` Is a Servlet

This is worth emphasizing.

It is not merely a Spring bean called:

```text
DispatcherServlet
```

It is a real:

```text
jakarta.servlet.Servlet
```

registered with the servlet container.

So:

```text
Tomcat
   ↓
Servlet
   ↓
DispatcherServlet
   ↓
Spring MVC
```

This explains why:

```text
Filter
```

runs at the Servlet layer, while:

```text
HandlerInterceptor
```

runs within Spring MVC's handler-processing pipeline.

---

# 69. `DispatcherServlet` Uses a `WebApplicationContext`

The servlet is associated with a Spring web application context.

The Spring MVC processing documentation notes that the `WebApplicationContext` is bound to the request and provides the beans that `DispatcherServlet` needs. ([Home][16])

Conceptually:

```text
Servlet container
       ↓
DispatcherServlet
       ↓
WebApplicationContext
       ↓
Spring MVC infrastructure beans
```

---

# 70. Why Do We Call It `WebApplicationContext`?

Because it's an `ApplicationContext` specialized for web applications.

Think:

```text
ApplicationContext
     ↓
general application container

WebApplicationContext
     ↓
web-aware ApplicationContext
```

This is relevant when you start discussing:

```text
request scope
session scope
ServletContext
```

from our earlier scope topic.

---

# 71. `@EnableWebMvc` — IMPORTANT BOOT INTERVIEW QUESTION

You may see:

```java
@Configuration
@EnableWebMvc
public class WebConfig {
}
```

This explicitly enables Spring MVC Java configuration.

However, in Spring Boot applications, blindly adding `@EnableWebMvc` can change the way Boot's MVC auto-configuration applies.

Spring's documentation specifically advises that with Spring Boot you may want to use `WebMvcConfigurer` without `@EnableWebMvc` to retain Boot's MVC customizations. ([Home][17])

So a common Boot pattern is:

```java
@Configuration
public class WebConfig
        implements WebMvcConfigurer {

    @Override
    public void addInterceptors(
            InterceptorRegistry registry) {
        // customize
    }
}
```

without:

```java
@EnableWebMvc
```

unless you deliberately want full control over MVC configuration.

---

# 72. What Does `WebMvcConfigurer` Do?

It gives you hooks to customize MVC behavior:

```text
interceptors
formatters
CORS
resource handling
view controllers
argument resolvers
message converters
path matching
etc.
```

Spring provides `WebMvcConfigurer` specifically as a programmatic MVC customization API. ([Home][14])

This is extremely common in real applications.

---

# 73. Does Implementing `WebMvcConfigurer` Disable Boot Auto-Configuration?

No.

This is a common old-tutorial misconception.

Simply doing:

```java
@Configuration
public class WebConfig
        implements WebMvcConfigurer {
}
```

does not have the same effect as:

```java
@EnableWebMvc
```

Spring's official documentation explicitly recommends `WebMvcConfigurer` without `@EnableWebMvc` in Boot when you want to keep Boot's MVC customizations. ([Home][17])

---

# 74. Message Converter Customization

Suppose you want a custom Jackson configuration.

You can configure message conversion through Spring MVC mechanisms.

Spring's current documentation provides `WebMvcConfigurer` support for customizing the `HttpMessageConverter` setup. ([Home][18])

Conceptually:

```text
Controller
   ↓
HttpMessageConverter
   ↓
customized JSON serialization
```

This is useful for things such as:

```text
date formats
custom serialization
custom media types
JSON configuration
XML support
```

---

# 75. Full Request Flow — Detailed Version

Now let's put everything into one diagram:

```text
                    CLIENT
                       |
                       | HTTP Request
                       ↓
              Servlet Container
                    Tomcat/etc.
                       |
                       ↓
                 Filter Chain
            ┌──────────┴──────────┐
            ↓                     ↓
         Filter A              Filter B
            |
            ↓
       DispatcherServlet
            |
            ↓
       HandlerMapping
            |
            ↓
      HandlerExecutionChain
        ┌───────┴────────┐
        ↓                ↓
    Interceptors      HandlerMethod
        |                |
        ↓                ↓
    preHandle      HandlerAdapter
                         |
                         ↓
              Argument Resolvers
                         |
              ┌──────────┼──────────┐
              ↓          ↓          ↓
        @PathVariable @RequestParam @RequestBody
                                    |
                                    ↓
                           HttpMessageConverter
                                    |
                                    ↓
                               Java object
                                    |
                                    ↓
                            Controller method
                                    |
                                    ↓
                                Service
                                    |
                                    ↓
                               Repository
                                    |
                                    ↓
                                 Database
                                    |
                                    ↓
                            Controller return
                                    |
                                    ↓
                         Return Value Handling
                                    |
                         ┌──────────┴──────────┐
                         ↓                     ↓
                    @ResponseBody          View name
                         ↓                     ↓
                 HttpMessageConverter     ViewResolver
                         ↓                     ↓
                       JSON                  View
                         |
                         ↓
                    HTTP Response
                         |
                         ↓
                    Client
```

This is the diagram I want you to understand, not merely memorize.

---

# 76. The Two Critical Paths

For interviews, simplify the system into two paths.

## Incoming request

```text
HTTP
 ↓
DispatcherServlet
 ↓
HandlerMapping
 ↓
HandlerAdapter
 ↓
Argument Resolution
 ↓
Controller
```

## Outgoing response

```text
Controller return value
 ↓
Return Value Handling
 ↓
HttpMessageConverter
 ↓
HTTP response
```

For traditional MVC view applications:

```text
Controller
 ↓
View name
 ↓
ViewResolver
 ↓
View
```

---

# 77. The Three Components You Absolutely Must Know

If the interviewer asks:

> "Explain Spring MVC internally."

Start with these:

### `DispatcherServlet`

> Central Front Controller that coordinates request processing. ([Home][1])

### `HandlerMapping`

> Determines which handler should process the request. ([Home][2])

### `HandlerAdapter`

> Invokes the selected handler while hiding the details of how that handler is invoked. ([Home][2])

Then:

```text
HttpMessageConverter
```

for request/response body conversion. ([Home][9])

Those four names are **high-value interview vocabulary**.

---

# 78. EPAM Interview Question

### "What is DispatcherServlet?"

Strong answer:

> `DispatcherServlet` is Spring MVC's Front Controller. It receives incoming servlet requests and delegates request processing to components such as `HandlerMapping`, `HandlerAdapter`, exception resolvers, and view/message-conversion infrastructure. It coordinates the overall request lifecycle rather than containing all request-processing logic itself. ([Home][1])

---

# 79. EPAM Interview Question

### "What is HandlerMapping?"

> `HandlerMapping` maps an incoming request to an appropriate handler, along with any applicable interceptors. `RequestMappingHandlerMapping` is the main mapping implementation for annotated controllers such as those using `@GetMapping` and `@RequestMapping`. ([Home][2])

---

# 80. EPAM Interview Question

### "What is HandlerAdapter?"

> `HandlerAdapter` allows `DispatcherServlet` to invoke different kinds of handlers without knowing their invocation details. For annotated controllers, `RequestMappingHandlerAdapter` handles method-argument resolution, invocation, and return-value processing. ([Home][2])

---

# 81. EPAM Interview Question

### "How does @RequestBody work?"

Strong answer:

> Spring MVC identifies the parameter as a request-body argument, selects an appropriate `HttpMessageConverter` based on the request/media type and target type, and uses it to deserialize the HTTP body into the declared Java type. For JSON requests, a Jackson-based converter is commonly used. ([Home][8])

---

# 82. EPAM Interview Question

### "How does Spring convert a Java object to JSON?"

Answer:

```text
Controller return value
      ↓
@ResponseBody / @RestController
      ↓
HttpMessageConverter
      ↓
Jackson JSON converter
      ↓
JSON
      ↓
HTTP response
```

Spring explicitly documents that `@ResponseBody` return values are written through `HttpMessageConverter`s. ([Home][10])

---

# 83. EPAM Interview Question

### "Filter vs Interceptor?"

Strong answer:

> A Servlet `Filter` operates at the Servlet/container layer and can wrap processing of the target servlet and other filters. A Spring MVC `HandlerInterceptor` operates around handler execution after `DispatcherServlet` has selected a handler. Interceptors provide `preHandle`, `postHandle`, and `afterCompletion` callbacks. ([Home][3])

---

# 84. EPAM Interview Question

### "What happens if the controller returns an object?"

For a `@RestController`:

```text
object
 ↓
@ResponseBody semantics
 ↓
HttpMessageConverter
 ↓
JSON/XML/etc.
 ↓
HTTP response
```

For a normal `@Controller`:

```text
String view name
 ↓
ViewResolver
 ↓
View
```

Spring explicitly documents these two return-value paths. ([Home][10])

---

# 85. EPAM Scenario

> **Your `@RestController` method isn't being called. What would you investigate?**

Think systematically:

```text
1. Is the controller a Spring bean?
2. Is its package being component-scanned?
3. Is the mapping correct?
4. Is the HTTP method correct?
5. Is the path correct?
6. Are consumes/produces constraints preventing a match?
7. Is another mapping conflicting?
8. Is the request reaching the correct application/context path?
9. Is a filter/interceptor/security layer rejecting it first?
```

This is a much better debugging answer than:

> "Check the controller."

---

# 86. EPAM Scenario

> **The controller is being called, but `@RequestBody` is failing. What do you check?**

Think:

```text
Content-Type
     ↓
application/json?

Body valid JSON?

Target DTO structure correct?

Jackson configuration?

Custom HttpMessageConverter?

Validation failure after conversion?
```

Remember the sequence:

```text
HTTP body
 ↓
message conversion
 ↓
Java object
 ↓
validation
 ↓
controller
```

Therefore a malformed JSON body can fail **before controller execution**. ([Home][8])

---

# 87. EPAM Scenario

> **The controller returns an object, but you're not getting JSON. Why?**

Check:

```text
@RestController?
@ResponseBody?
Content negotiation?
Accept header?
HttpMessageConverter available?
Object serializable?
```

Because:

```text
return object
```

doesn't by itself mean:

```text
JSON
```

The response-body semantics and appropriate message converter have to be involved. ([Home][10])

---

# 88. EPAM Scenario

> **Why would `postHandle()` be too late to modify a REST response?**

Answer:

> For `@ResponseBody` and `ResponseEntity` methods, Spring MVC can write and commit the response inside the `HandlerAdapter` before `postHandle()` is called. Therefore, response customization at that point can be too late; `ResponseBodyAdvice` is the appropriate MVC extension for response-body customization. ([Home][5])

That's a very strong senior-level answer.

---

# 89. EPAM Scenario

> **Why is DispatcherServlet called a Front Controller?**

Answer:

> Because it provides a centralized entry point for Spring MVC requests. It receives the request and delegates to specialized components for handler selection, invocation, exception handling, and response rendering.

Exactly matches Spring's architecture. ([Home][1])

---

# 90. EPAM Scenario

> **Why do we need HandlerAdapter if HandlerMapping already found the controller?**

Excellent question.

Answer:

> `HandlerMapping` answers "which handler should process the request?" `HandlerAdapter` answers "how should that handler be invoked?" The adapter decouples `DispatcherServlet` from the details of different handler types and, for annotated controllers, coordinates argument resolution and return-value handling. ([Home][2])

That's an interview-quality distinction.

---

# 91. `DispatcherServlet` Is an Orchestrator

This is perhaps the most important conceptual takeaway.

Don't think:

```text
DispatcherServlet
    =
the entire MVC framework
```

Think:

```text
DispatcherServlet
    =
orchestrator

HandlerMapping
    =
find handler

HandlerAdapter
    =
invoke handler

ArgumentResolver
    =
provide method arguments

HttpMessageConverter
    =
convert request/response body

HandlerExceptionResolver
    =
handle exceptions

ViewResolver
    =
resolve views
```

That's the architecture.

---

# 92. Why This Design Is Powerful

Each responsibility is replaceable/configurable.

For example:

```text
Need different URL mapping?
    ↓
HandlerMapping

Need different argument type?
    ↓
ArgumentResolver

Need different JSON representation?
    ↓
HttpMessageConverter

Need different exception behavior?
    ↓
HandlerExceptionResolver

Need different view technology?
    ↓
ViewResolver
```

Spring calls these its **strategy interfaces/components**. ([Home][2])

This is classic framework design.

---

# 93. Current Spring MVC Detail: `PathPattern`

If an interviewer asks how modern Spring MVC matches paths, current Spring Framework uses:

```text
PathPattern
```

for request mapping, while the older `AntPathMatcher` approach is deprecated in this context. ([Home][4])

So don't confidently say:

> "Spring MVC uses AntPathMatcher for all controller mapping."

That is outdated.

For a current interview:

> "Modern Spring MVC uses `PathPattern` for request mapping path matching."

That's more current. ([Home][4])

---

# 94. Don't Confuse MVC With WebFlux

Spring has two web programming models:

```text
Spring MVC
    ↓
Servlet
    ↓
blocking/traditional servlet model

Spring WebFlux
    ↓
reactive
    ↓
non-blocking reactive model
```

Today's topic is:

```text
Spring MVC
```

So:

```text
DispatcherServlet
HandlerMapping
HandlerAdapter
```

belong to the MVC/Servlet path.

WebFlux has corresponding but different infrastructure.

This distinction becomes important when interviewers ask:

> MVC vs WebFlux?

We'll cover that later.

---

# 95. The Interview Mental Model

When an interviewer asks:

> "Walk me through an HTTP request in Spring Boot."

Your answer should flow naturally:

```text
Client sends request
        ↓
Servlet container receives it
        ↓
Filters execute
        ↓
DispatcherServlet receives request
        ↓
HandlerMapping finds the handler
        ↓
Interceptors' preHandle executes
        ↓
HandlerAdapter invokes the controller
        ↓
Argument resolvers create controller arguments
        ↓
@RequestBody uses HttpMessageConverter if needed
        ↓
Controller calls service/repository
        ↓
Controller returns result
        ↓
Return-value handling occurs
        ↓
HttpMessageConverter serializes response for REST
        ↓
Interceptors complete
        ↓
HTTP response returned
```

That is an **excellent interview answer**.

---

# 96. Revision Cheat Sheet

### `DispatcherServlet`

```text
Front Controller
central MVC request coordinator
```

### `HandlerMapping`

```text
Request
  ↓
which handler?
```

### `HandlerAdapter`

```text
selected handler
  ↓
how do I invoke it?
```

### `HandlerMethodArgumentResolver`

```text
controller parameter
  ↓
where does its value come from?
```

### `HttpMessageConverter`

```text
HTTP body ↔ Java object
```

### `HandlerExceptionResolver`

```text
exception
  ↓
appropriate response/view
```

### `ViewResolver`

```text
logical view name
  ↓
actual view
```

### `HandlerInterceptor`

```text
before/after handler execution
```

### Filter

```text
Servlet-level interception
```

---

# 97. The Most Important Diagram From This Topic

Memorize this:

```text
                         HTTP REQUEST
                              |
                              ↓
                     Servlet Container
                              |
                              ↓
                        Filter Chain
                              |
                              ↓
                     DispatcherServlet
                              |
                              ↓
                       HandlerMapping
                              |
                              ↓
                    HandlerExecutionChain
                       /              \
                Interceptors       HandlerMethod
                                      |
                                      ↓
                              HandlerAdapter
                                      |
                    ┌─────────────────┼──────────────────┐
                    ↓                 ↓                  ↓
              @PathVariable     @RequestParam      @RequestBody
                                                        |
                                                        ↓
                                               HttpMessageConverter
                                                        |
                                                        ↓
                                                  Java Object
                                                        |
                                                        ↓
                                                   Controller
                                                        |
                                                        ↓
                                                    Service
                                                        |
                                                        ↓
                                                   Repository
                                                        |
                                                        ↓
                                                   Database
                                                        |
                                                        ↓
                                               Return Value
                                                        |
                                      ┌─────────────────┴──────────────┐
                                      ↓                                ↓
                              @ResponseBody                      View name
                                      ↓                                ↓
                              HttpMessageConverter                 ViewResolver
                                      ↓                                ↓
                                    JSON                              View
                                      |
                                      ↓
                              HTTP RESPONSE
```

---

# 98. What You Should Know Cold for EPAM

You should now be able to explain without hesitation:

**What is Spring MVC?**

Spring's Servlet-based web framework.

**What is `DispatcherServlet`?**

The Front Controller that orchestrates MVC request processing. ([Home][1])

**What does `HandlerMapping` do?**

Finds the handler for the request. ([Home][2])

**What does `HandlerAdapter` do?**

Invokes the selected handler without `DispatcherServlet` needing to know handler-specific invocation details. ([Home][2])

**What is `HttpMessageConverter`?**

Reads/writes HTTP request and response bodies. ([Home][9])

**How does `@RequestBody` work?**

Body → `HttpMessageConverter` → Java object. ([Home][8])

**How does `@RestController` work?**

`@Controller` + `@ResponseBody`. ([Home][10])

**How does object → JSON happen?**

`@ResponseBody` semantics → `HttpMessageConverter` → JSON converter. ([Home][10])

**Filter vs Interceptor?**

Servlet/container layer vs Spring MVC handler layer. ([Home][3])

**`@RequestParam` vs `@PathVariable`?**

Query/request parameter vs URI template variable.

**`consumes` vs `produces`?**

Request media type vs response media type. ([Home][4])

**`@Controller` vs `@RestController`?**

View-oriented controller vs response-body-oriented REST controller. ([Home][10])

---

# 99. Where We Stand

Our Spring Boot knowledge now looks like:

```text
Spring Boot
    |
    ├── IoC / DI
    ├── Beans
    ├── Bean lifecycle
    ├── Scopes
    ├── Component scanning
    ├── @Configuration / @Bean
    ├── Auto-configuration
    ├── External configuration
    ├── Application startup
    └── Spring MVC
           |
           ├── DispatcherServlet
           ├── HandlerMapping
           ├── HandlerAdapter
           ├── Argument Resolution
           ├── Message Conversion
           ├── Interceptors
           ├── Exception Handling
           └── View Resolution
```

We're now moving from **Spring infrastructure** into actual **REST API development**.

---

# Next Topic — REST APIs in Spring Boot

Next we'll focus entirely on the REST side:

```text
@GetMapping
@PostMapping
@PutMapping
@PatchMapping
@DeleteMapping

@PathVariable
@RequestParam
@RequestHeader
@RequestBody
@ResponseBody
ResponseEntity
HttpStatus

DTOs
Validation
Pagination
Sorting
API design
Idempotency
HTTP semantics
```

We'll especially go deep into questions such as:

> **PUT vs PATCH — what's the real difference?**

> **When should you return `ResponseEntity`?**

> **Should the controller return an Entity or DTO?**

> **How do you design proper REST URLs?**

> **What status code should a POST return?**

> **What does idempotent actually mean?**

> **How do you handle pagination and filtering in a production API?**

> **What happens internally when `@RequestBody` receives JSON?**

Those are the questions that tend to turn basic Spring Boot REST knowledge into a strong interview answer.

[1]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet.html?utm_source=chatgpt.com "DispatcherServlet :: Spring Framework"
[2]: https://docs.spring.io/spring-framework/reference/7.1/web/webmvc/mvc-servlet/special-bean-types.html?utm_source=chatgpt.com "Special Bean Types :: Spring Framework"
[3]: https://docs.spring.io/spring/reference/web/webmvc/filters.html?utm_source=chatgpt.com "Filters :: Spring Framework"
[4]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html?utm_source=chatgpt.com "Mapping Requests :: Spring Framework"
[5]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html?utm_source=chatgpt.com "Interception :: Spring Framework"
[6]: https://docs.spring.io/spring/reference/web/webmvc/mvc-controller/ann-methods/arguments.html?utm_source=chatgpt.com "Method Arguments :: Spring Framework"
[7]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/typeconversion.html?utm_source=chatgpt.com "Type Conversion :: Spring Framework"
[8]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestbody.html?utm_source=chatgpt.com "@RequestBody :: Spring Framework"
[9]: https://docs.spring.io/spring-framework/reference/web/webmvc/message-converters.html?utm_source=chatgpt.com "HTTP Message Conversion :: Spring Framework"
[10]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/responsebody.html?utm_source=chatgpt.com "@ResponseBody :: Spring Framework"
[11]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/return-types.html?utm_source=chatgpt.com "Return Values :: Spring Framework"
[12]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/validation.html?utm_source=chatgpt.com "Validation :: Spring Framework"
[13]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html?utm_source=chatgpt.com "@ModelAttribute :: Spring Framework"
[14]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/customize.html?utm_source=chatgpt.com "MVC Config API :: Spring Framework"
[15]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/conversion.html?utm_source=chatgpt.com "Type Conversion :: Spring Framework"
[16]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/sequence.html?utm_source=chatgpt.com "Processing :: Spring Framework"
[17]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/enable.html?utm_source=chatgpt.com "Enable MVC Configuration :: Spring Framework"
[18]: https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/message-converters.html?utm_source=chatgpt.com "Message Converters :: Spring Framework"

