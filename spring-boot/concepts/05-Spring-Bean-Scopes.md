# Topic 5 — Spring Bean Scopes

Bean scope answers a very simple but extremely important question:

> **How many instances of a particular Spring bean should exist, and how long should those instances live?**

This becomes especially important when your application is:

* handling many HTTP requests
* using shared state
* using multithreading
* mixing singleton and prototype beans
* dealing with request/session data

Spring currently supports **six built-in scopes**: `singleton`, `prototype`, `request`, `session`, `application`, and `websocket`; the last four require a web-aware application context. A custom scope can also be created. ([Home][1])

---

# 1. What is a Bean Scope?

Suppose we have:

```java
@Service
public class OrderService {
}
```

Spring needs to decide:

```text
How many OrderService objects?

When should they be created?

How long should they live?

When should they be discarded?
```

That's what scope determines.

Conceptually:

```text
Bean Definition
      ↓
      scope
      ↓
how instances are created and managed
```

For example:

```text
singleton
   ↓
one instance per container

prototype
   ↓
new instance whenever requested

request
   ↓
one instance per HTTP request
```

---

# 2. The Six Main Spring Scopes

| Scope         | Instance lifetime                                     |
| ------------- | ----------------------------------------------------- |
| `singleton`   | One instance per bean definition per Spring container |
| `prototype`   | New instance each time requested                      |
| `request`     | One instance per HTTP request                         |
| `session`     | One instance per HTTP session                         |
| `application` | One instance per `ServletContext`                     |
| `websocket`   | One instance per WebSocket session                    |

`singleton` is the default. ([Home][1])

The first two are the most important for interviews:

```text
singleton
prototype
```

Then we'll look at web scopes.

---

# 3. Singleton Scope

Let's start with:

```java
@Service
public class OrderService {
}
```

Because no scope is specified, the bean is:

```text
singleton
```

Spring normally creates one instance corresponding to that bean definition within the container and reuses that instance. ([Home][1])

Imagine:

```text
ApplicationContext
       |
       ↓
OrderService
       |
       +---- same instance
       |
       +---- same instance
       |
       +---- same instance
```

If three different beans depend on it:

```java
@Service
public class PaymentService {

    private final OrderService orderService;

    public PaymentService(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

```java
@Service
public class NotificationService {

    private final OrderService orderService;

    public NotificationService(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

conceptually:

```text
             OrderService
                  ↑
            ┌─────┴─────┐
            │           │
     PaymentService  NotificationService
```

Both can hold the **same `OrderService` instance**.

---

# 4. Very Important: Spring Singleton ≠ Singleton Design Pattern

This is a **classic interview question**.

An interviewer asks:

> "Is a Spring singleton the same as the Singleton design pattern?"

Answer:

> **No.**

Spring's singleton is **per container and per bean definition**. The traditional GoF Singleton pattern is generally about ensuring a single instance of a class, historically described in the context of a ClassLoader. Spring itself explicitly distinguishes its singleton scope from the GoF pattern. ([Home][1])

For example:

```java
@Component
public class MyService {
}
```

Spring may maintain one instance of this bean in one container.

But you could have:

```text
ApplicationContext A
    ↓
MyService instance A

ApplicationContext B
    ↓
MyService instance B
```

So "one instance" is not a universal property of the Java class.

---

# 5. Spring Singleton Is Per Bean Definition

Here's a more subtle point.

Suppose:

```java
@Configuration
public class AppConfig {

    @Bean
    public PaymentService stripePaymentService() {
        return new StripePaymentService();
    }

    @Bean
    public PaymentService anotherPaymentService() {
        return new StripePaymentService();
    }
}
```

You have **two bean definitions**.

So there can be:

```text
stripePaymentService
        ↓
instance A

anotherPaymentService
        ↓
instance B
```

Both are singleton scoped, but they are **two separate singleton bean instances** because they correspond to different bean definitions. This follows directly from Spring's definition of singleton scope as one instance **per bean definition per container**. ([Home][1])

This is an excellent advanced interview point.

---

# 6. Does Singleton Mean Thread-Safe?

**Absolutely not.**

This is one of the biggest Spring interview traps.

Suppose:

```java
@Service
public class CounterService {

    private int count = 0;

    public void increment() {
        count++;
    }
}
```

Because `CounterService` is a singleton:

```text
many requests
      ↓
same CounterService instance
```

Therefore multiple threads can access:

```java
count
```

simultaneously.

That does **not** make `count++` thread-safe.

You need appropriate concurrency control if mutable shared state is required.

So:

```text
singleton
    ≠
thread-safe
```

The usual recommendation in web applications is to keep singleton services **stateless** wherever practical.

---

# 7. Why are Spring Services Usually Singleton?

Consider:

```java
@Service
public class InvoiceService {

    public Invoice getInvoice(Long id) {
        // ...
    }
}
```

The service doesn't need to maintain request-specific state.

It simply:

```text
receives input
   ↓
performs logic
   ↓
returns result
```

We don't need:

```text
one InvoiceService per HTTP request
```

We can efficiently reuse one instance:

```text
             InvoiceService
              singleton
                  |
        ┌─────────┼─────────┐
        ↓         ↓         ↓
    request 1  request 2  request 3
```

This is one reason stateless service beans are commonly singleton scoped. Spring's documentation likewise recommends singleton scope for stateless beans and prototype for stateful beans. ([Home][1])

---

# 8. State is the key consideration

Think:

### Stateless bean

```java
@Service
public class PriceCalculator {

    public BigDecimal calculate(Product product) {
        // calculate
    }
}
```

No per-user data is stored inside the object.

Singleton is usually fine.

---

### Stateful bean

```java
@Service
public class ShoppingCart {

    private List<Product> products = new ArrayList<>();
}
```

Now the object contains mutable state.

Ask:

> Is this state supposed to be shared across all users?

If the answer is no, a singleton is probably inappropriate.

For example:

```text
User A
   ↓
Cart A

User B
   ↓
Cart B
```

You don't want:

```text
User A
    ↓
same singleton cart
    ↑
User B
```

That could create correctness and security problems.

---

# 9. Prototype Scope

Now:

```java
@Component
@Scope("prototype")
public class ReportBuilder {
}
```

Prototype means:

> Every time the container is asked for a new instance of this bean, it creates another instance. ([Home][1])

Conceptually:

```text
request bean
    ↓
ReportBuilder A

request bean
    ↓
ReportBuilder B

request bean
    ↓
ReportBuilder C
```

So:

```text
singleton
    ↓
A A A A

prototype
    ↓
A B C D
```

---

# 10. When is Prototype Useful?

Prototype is useful when the object contains **state that should not be shared** and you want Spring to create the object for you.

For example:

```java
@Component
@Scope("prototype")
public class ReportContext {

    private String reportType;

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
}
```

Each request for the bean can produce a separate object.

However, in modern Spring applications, you should not use prototype simply because an object has state. Often plain object construction is simpler:

```java
new ReportContext()
```

The important question is whether the object benefits from **Spring-managed creation/dependencies/lifecycle**.

---

# 11. How do we configure Prototype?

Using:

```java
@Scope("prototype")
```

Example:

```java
@Component
@Scope("prototype")
public class ReportBuilder {
}
```

Or with `@Bean`:

```java
@Configuration
public class AppConfig {

    @Bean
    @Scope("prototype")
    public ReportBuilder reportBuilder() {
        return new ReportBuilder();
    }
}
```

Spring's `@Scope` annotation can be used on `@Bean` methods or component classes. ([Home][2])

You can also use the shortcut-style annotation in suitable contexts:

```java
@PrototypeScope
```

but for interviews, know:

```java
@Scope("prototype")
```

first.

---

# 12. Singleton vs Prototype

This table is worth memorizing:

|                                     | Singleton                         | Prototype                             |
| ----------------------------------- | --------------------------------- | ------------------------------------- |
| Default?                            | Yes                               | No                                    |
| Instances                           | One per bean definition/container | New instance per request to container |
| Typical use                         | Stateless services                | Stateful objects                      |
| Shared?                             | Yes                               | No                                    |
| Full destruction lifecycle managed? | Yes                               | No                                    |
| Common in Spring apps?              | Very common                       | Less common                           |

Spring explicitly states that initialization callbacks apply to prototype beans, but configured destruction callbacks are not automatically invoked for prototypes because the container does not retain the prototype after handing it to the client. ([Home][1])

---

# 13. Prototype Lifecycle — Very Important

People often assume:

```text
prototype
  ↓
new object
  ↓
@PreDestroy
```

No.

For prototype beans, Spring handles:

```text
creation
configuration
initialization
```

but **does not manage the complete destruction lifecycle**. ([Home][1])

So:

```text
Prototype
   ↓
Spring creates it
   ↓
Spring initializes it
   ↓
Spring hands it to client
   ↓
Spring no longer tracks that instance normally
```

If the object owns expensive resources, the client/application must deal with cleanup appropriately.

This is a very common interview question.

---

# 14. The Most Important Trap: Singleton Depends on Prototype

This is probably the **number-one scope interview scenario**.

Suppose:

```java
@Component
@Scope("prototype")
public class PrototypeBean {
}
```

and:

```java
@Service
public class SingletonBean {

    private final PrototypeBean prototypeBean;

    public SingletonBean(PrototypeBean prototypeBean) {
        this.prototypeBean = prototypeBean;
    }
}
```

You might think:

> "Every time I use `singletonBean`, Spring will give me a new PrototypeBean."

**No.**

This is wrong.

---

# 15. What Actually Happens?

`SingletonBean` is created once.

When Spring creates it:

```text
Create SingletonBean
       ↓
Needs PrototypeBean
       ↓
Create PrototypeBean A
       ↓
Inject PrototypeBean A
       ↓
SingletonBean ready
```

The singleton now holds:

```text
SingletonBean
      |
      ↓
PrototypeBean A
```

Later:

```text
get SingletonBean
      ↓
same SingletonBean
      ↓
same PrototypeBean A
```

Spring's documentation explicitly warns that prototype dependencies are resolved when the singleton is instantiated; the prototype instance injected at that time is the one supplied to that singleton. ([Home][1])

This surprises many developers.

---

# 16. Why doesn't Spring inject a new Prototype every time?

Because constructor injection happens during bean creation.

Consider:

```java
public SingletonBean(PrototypeBean prototypeBean) {
    this.prototypeBean = prototypeBean;
}
```

The constructor runs once:

```text
singleton creation
      ↓
constructor
      ↓
prototype dependency supplied
```

After that:

```text
singleton already exists
```

Spring isn't repeatedly executing the constructor.

Therefore:

```text
prototype scope
+
normal injection into singleton
=
prototype instance obtained at singleton creation
```

Not:

```text
new prototype on every method call
```

---

# 17. How do we get a NEW Prototype from a Singleton?

This is where things get interesting.

There are several approaches.

## Option 1 — `ObjectProvider`

Modern Spring commonly provides:

```java
ObjectProvider<PrototypeBean>
```

Example:

```java
@Service
public class SingletonBean {

    private final ObjectProvider<PrototypeBean> provider;

    public SingletonBean(ObjectProvider<PrototypeBean> provider) {
        this.provider = provider;
    }

    public void execute() {

        PrototypeBean bean =
                provider.getObject();

        // use bean
    }
}
```

Now:

```text
execute()
   ↓
provider.getObject()
   ↓
Prototype A

execute()
   ↓
provider.getObject()
   ↓
Prototype B
```

This is a very useful solution because the singleton receives a provider rather than a single prototype instance.

---

# 18. `ObjectProvider` Mental Model

Instead of:

```text
Singleton
   ↓
PrototypeBean instance
```

you have:

```text
Singleton
   ↓
ObjectProvider<PrototypeBean>
   ↓
"Give me a PrototypeBean"
```

So:

```text
call 1 → Prototype A
call 2 → Prototype B
call 3 → Prototype C
```

This is often a clean Spring-native solution.

---

# 19. Option 2 — `ObjectFactory`

Another option is:

```java
ObjectFactory<PrototypeBean>
```

Conceptually similar:

```java
PrototypeBean bean =
        factory.getObject();
```

`ObjectProvider` is a richer interface built for dependency lookup with additional convenience operations.

For interview purposes:

```text
ObjectProvider
    ↓
common modern solution
```

---

# 20. Option 3 — `@Lookup`

Spring also supports method injection via:

```java
@Lookup
```

For example:

```java
@Component
public abstract class SingletonBean {

    public void execute() {

        PrototypeBean bean = getPrototypeBean();

        // use bean
    }

    @Lookup
    protected abstract PrototypeBean getPrototypeBean();
}
```

Spring can override the lookup method through its bean machinery to obtain the appropriate scoped object.

This is an interesting advanced technique and is explicitly associated with the problem of injecting a prototype bean into a singleton. ([Home][1])

For practical modern code, `ObjectProvider` is often easier to reason about.

---

# 21. Web Scopes

Now let's move to the web-specific scopes.

These are:

```text
request
session
application
websocket
```

Spring's documentation says these require a web-aware `ApplicationContext`. ([Home][1])

---

# 22. Request Scope

A request-scoped bean lives for one HTTP request.

Example:

```java
@Component
@RequestScope
public class RequestContext {
}
```

Conceptually:

```text
HTTP Request 1
      ↓
RequestContext A

HTTP Request 2
      ↓
RequestContext B

HTTP Request 3
      ↓
RequestContext C
```

So:

```text
one request
    =
one instance
```

Spring documents `@RequestScope` as the annotation-based way to assign a bean to request scope. ([Home][1])

---

# 23. When would Request Scope be useful?

Suppose you need request-specific state:

```text
correlation ID
request metadata
temporary request context
some request-local information
```

For example:

```java
@RequestScope
@Component
public class RequestContext {

    private String correlationId;

    // getter/setter
}
```

Within one request:

```text
Controller
    ↓
Service
    ↓
Repository
```

all accessing `RequestContext` can see the instance associated with **that request**.

A different HTTP request gets a different instance.

---

# 24. Session Scope

Session scope lasts for an HTTP session.

Example:

```java
@Component
@SessionScope
public class UserPreferences {
}
```

Conceptually:

```text
Session A
   ↓
UserPreferences A

Session B
   ↓
UserPreferences B
```

Within Session A, the same scoped bean instance is reused.

When that HTTP session ends, the session-scoped bean is discarded. ([Home][1])

---

# 25. Application Scope

Application scope corresponds to the lifecycle of the web application's `ServletContext`.

Example:

```java
@Component
@ApplicationScope
public class ApplicationSettings {
}
```

It is similar to singleton but not identical.

This is an important interview distinction.

Spring documents application scope as:

> one instance per `ServletContext`

whereas Spring singleton is:

> one instance per bean definition per Spring `ApplicationContext`. ([Home][1])

---

# 26. Singleton vs Application Scope

This sounds like:

```text
singleton = one
application = one
```

so why are they different?

Because **"one" is measured at different boundaries**.

### Singleton

```text
Spring ApplicationContext
        ↓
one instance
```

### Application scope

```text
ServletContext
        ↓
one instance
```

A web application may have more than one Spring `ApplicationContext`.

Therefore:

```text
Spring singleton
    ≠
Servlet application scoped bean
```

even though they often behave similarly in simple applications. ([Home][1])

---

# 27. WebSocket Scope

WebSocket scope is associated with the lifetime of a WebSocket session.

Example:

```java
@Component
@Scope(
    scopeName = "websocket",
    proxyMode = ScopedProxyMode.TARGET_CLASS
)
public class WebSocketState {
}
```

Conceptually:

```text
WebSocket connection A
       ↓
WebSocketState A

WebSocket connection B
       ↓
WebSocketState B
```

Spring's documentation specifically describes WebSocket-scoped beans and notes that they are commonly injected through scoped proxies into longer-lived beans such as controllers. ([Home][3])

This is much less likely to be a basic interview question, but know the concept.

---

# 28. The Problem: Long-lived Bean Depends on Short-lived Bean

Now we're getting into one of the deeper Spring topics.

Suppose:

```text
Singleton Controller
       ↓
Request-scoped bean
```

The controller lives much longer than any individual request.

So what should this reference mean?

You cannot simply think:

```text
Controller
   ↓
RequestBean A forever
```

because RequestBean A only belongs to one request.

Spring solves this through mechanisms such as **scoped proxies**. ([Home][1])

---

# 29. Scoped Proxy

Suppose:

```java
@Component
@RequestScope
public class RequestContext {
}
```

and:

```java
@Service
public class OrderService {

    private final RequestContext requestContext;

    public OrderService(RequestContext requestContext) {
        this.requestContext = requestContext;
    }
}
```

`OrderService` is singleton.

How can it hold a request-scoped object?

The conceptual solution is:

```text
OrderService
     |
     ↓
RequestContext proxy
     |
     ├── Request 1 → RequestContext A
     |
     ├── Request 2 → RequestContext B
     |
     └── Request 3 → RequestContext C
```

The singleton doesn't hold one permanent request instance.

It holds something capable of resolving the correct target for the current scope.

Spring's documentation describes scoped proxies precisely for injecting shorter-lived scoped beans into longer-lived beans. ([Home][1])

---

# 30. Why Does a Proxy Solve It?

Suppose:

```java
requestContext.getUserId()
```

is called during Request 1.

The proxy can conceptually say:

```text
Current request = Request 1

Give me RequestContext
associated with Request 1
```

For Request 2:

```text
Current request = Request 2

Give me RequestContext
associated with Request 2
```

So:

```text
same proxy
       ↓
different target instances
```

This is a very important Spring concept.

It connects directly to the later topic:

> **Spring AOP and proxies**

---

# 31. `proxyMode`

You may see:

```java
@Scope(
    value = WebApplicationContext.SCOPE_REQUEST,
    proxyMode = ScopedProxyMode.TARGET_CLASS
)
```

or:

```java
@RequestScope
```

with appropriate proxy configuration.

Two commonly discussed proxy modes are:

```text
TARGET_CLASS
INTERFACES
```

Conceptually:

```text
TARGET_CLASS
    ↓
class-based proxy

INTERFACES
    ↓
interface-based proxy
```

This connects to JDK dynamic proxies vs class-based proxies, which we'll study in the AOP topic.

---

# 32. Thread Scope

Spring also has a thread scope implementation, but it is **not registered by default**. The built-in documentation mentions `SimpleThreadScope` and custom scope registration. ([Home][1])

Conceptually:

```text
Thread 1
   ↓
Bean A

Thread 2
   ↓
Bean B
```

It's worth knowing the term but don't confuse it with:

```text
request scope
```

A servlet request often runs on a thread, but the concepts are not interchangeable.

---

# 33. Custom Scope

Spring's scope mechanism is extensible.

You can implement:

```java
org.springframework.beans.factory.config.Scope
```

and register your own scope. ([Home][1])

For example, theoretically you could have:

```text
tenant scope
job scope
conversation scope
workflow scope
```

The application decides what lifecycle boundary the scope represents.

This is advanced Spring knowledge.

---

# 34. A crucial concept: Scope belongs to the Bean Definition

Remember the previous topic:

```text
BeanDefinition
```

Think:

```text
BeanDefinition
    |
    +---- class
    +---- dependencies
    +---- lifecycle
    +---- scope
```

So scope isn't something that the Java class inherently owns.

For example:

```java
@Scope("prototype")
```

can be changed through configuration.

This is one reason Spring's scope mechanism is flexible. Spring explicitly describes scope as configuration on the bean definition rather than something that must be hardcoded into the object's class design. ([Home][1])

---

# 35. What happens with `@Configuration` and `@Bean`?

You can specify:

```java
@Configuration
public class AppConfig {

    @Bean
    @Scope("prototype")
    public ReportBuilder reportBuilder() {
        return new ReportBuilder();
    }
}
```

Spring recognizes the scope on that bean definition. ([Home][2])

So scope is independent of whether the bean came from:

```text
@Component scanning
```

or:

```text
@Bean configuration
```

---

# 36. Common Interview Question

### Q: What is the default Spring bean scope?

Answer:

> Singleton.

And more precisely:

> One instance per bean definition per Spring IoC container.

That precision is better than simply saying:

> One object for the entire application.

---

# 37. Common Interview Question

### Q: Is Spring singleton thread-safe?

Answer:

> No. Singleton controls the number/lifetime of bean instances, not concurrent access semantics. A singleton bean can be accessed by multiple threads, so mutable shared state must be designed for thread safety.

This is an **excellent interview answer**.

---

# 38. Common Interview Question

### Q: When should you use prototype scope?

Answer:

> Prototype scope is appropriate when Spring should create a fresh bean instance whenever the bean is requested and the object has state that should not be shared between consumers. However, prototype should not be used automatically for every stateful object; normal object construction may sometimes be simpler.

That's much more mature than:

> "Use prototype for every stateful class."

---

# 39. Common Interview Question

### Q: What happens if a singleton depends on a prototype?

Answer:

> With ordinary dependency injection, the prototype dependency is resolved when the singleton is created, so that singleton receives one prototype instance. It does not receive a new prototype instance on every method call.

This is directly documented by Spring. ([Home][1])

Then:

> If the singleton needs a new prototype instance repeatedly at runtime, use something such as `ObjectProvider`, `ObjectFactory`, or method injection such as `@Lookup`.

---

# 40. Common Interview Question

### Q: What happens if a singleton depends on a request-scoped bean?

Answer:

> Because their lifetimes differ, Spring can use a scoped proxy so the singleton holds a proxy that resolves the actual request-scoped target for the current request.

This is a very good senior-level answer. ([Home][1])

---

# 41. Common Interview Question

### Q: Does Spring manage destruction of prototype beans?

Answer:

> Spring performs initialization lifecycle processing for prototype beans, but it doesn't track them for the complete destruction lifecycle. Therefore destruction callbacks such as `@PreDestroy` are not automatically invoked for prototype instances by the container. ([Home][1])

---

# 42. Common Interview Question

### Q: What's the difference between request and session scope?

Easy:

```text
request
    ↓
one HTTP request

session
    ↓
one HTTP session
```

So:

```text
Request 1 → Bean A
Request 2 → Bean B
```

even for the same user/session.

But:

```text
Session X
 Request 1 ──┐
 Request 2 ──┼── same session bean
 Request 3 ──┘
```

for a session-scoped bean. ([Home][1])

---

# 43. EPAM-style scenario question

Imagine an interviewer says:

> "I have a Spring singleton service with a field containing the current logged-in user's ID. Is that safe?"

You should immediately think:

```text
singleton
   +
request-specific mutable state
   ↓
danger
```

Suppose:

```java
@Service
public class UserService {

    private String currentUserId;

    public void setCurrentUserId(String id) {
        this.currentUserId = id;
    }
}
```

With concurrent requests:

```text
Request A → user = Alice
Request B → user = Bob
```

The same singleton may be accessed concurrently:

```text
          UserService
         /           \
      Alice          Bob
```

State can overwrite each other.

The correct architecture depends on the use case, but request/user-specific information should generally not be stored in mutable fields of singleton beans.

This connects directly to **concurrency**, which is why Spring scope questions sometimes turn into Java multithreading questions.

---

# 44. Scope and Thread Safety

Remember this formula:

```text
Scope
    +
State
    +
Concurrency
```

must be considered together.

For example:

```text
Singleton + stateless
    → usually straightforward

Singleton + mutable shared state
    → concurrency concerns

Prototype + independent state
    → isolated instances

Request + request-specific state
    → natural fit
```

Scope isn't just a configuration detail.

It's part of your application's concurrency model.

---

# 45. One more subtle interview point

A singleton bean being one instance does **not** mean that every dependency it has is singleton.

Example:

```text
Singleton Service
      |
      +---- singleton Repository
      |
      +---- prototype dependency
```

Different dependencies can have different scopes.

The problem is making those different lifetimes interact correctly.

That's why Spring provides:

```text
scoped proxy
ObjectProvider
ObjectFactory
@Lookup
```

depending on the situation.

---

# 46. Revision Diagram

Memorize this:

```text
                 SPRING SCOPES
                      |
       ┌──────────────┼──────────────┐
       ↓              ↓              ↓
   singleton       prototype      web scopes
       |              |              |
    one per        new object      request
    container       per lookup     session
                                   application
                                   websocket
```

And:

```text
singleton
    ↓
same instance

prototype
    ↓
new instance per container request

request
    ↓
one per HTTP request

session
    ↓
one per HTTP session

application
    ↓
one per ServletContext

websocket
    ↓
one per WebSocket session
```

Spring's current framework documentation confirms these six built-in scopes and the default singleton behavior. ([Home][1])

---

# 47. The 7 Scope Questions You Should Know Cold

Before moving on, be able to answer these without hesitation:

**1. What is a Spring bean scope?**

> It defines the lifecycle and instance-sharing semantics of a Spring bean.

**2. What is the default scope?**

> Singleton.

**3. Is Spring singleton the same as GoF Singleton?**

> No. Spring singleton is per bean definition per container.

**4. Does singleton mean thread-safe?**

> No.

**5. What happens when singleton injects prototype normally?**

> The prototype is created when the singleton is instantiated and that instance is injected; it isn't recreated per method invocation.

**6. How can a singleton obtain a new prototype instance repeatedly?**

> `ObjectProvider`, `ObjectFactory`, `@Lookup`, or an appropriate provider/factory design.

**7. Does Spring automatically call destroy callbacks for prototype beans?**

> No.

---

# 48. The bigger Spring mental model so far

You now have:

```text
                    Spring Boot
                        |
                        ↓
                ApplicationContext
                        |
                        ↓
                  BeanDefinition
                        |
             ┌──────────┴──────────┐
             ↓                     ↓
          Bean scope          Dependencies
             ↓                     ↓
    singleton/prototype/...       DI
             |                     |
             └──────────┬──────────┘
                        ↓
                  Bean Lifecycle
                        ↓
                   Bean instance
                        ↓
                 Post-processing
                        ↓
                      Proxy
```

We're now ready for one of the most frequently used parts of Spring:

# Topic 6 — Dependency Injection in Spring: `@Autowired`, Constructor vs Setter vs Field Injection, `@Qualifier`, `@Primary`, and resolving multiple beans

This is where we'll go much deeper than the basic DI topic.

We'll cover questions such as:

> Why is constructor injection preferred?

> How does Spring decide which implementation to inject?

> What happens when two beans implement the same interface?

> `@Primary` vs `@Qualifier`?

> What happens if there are **three** implementations?

> Can you use `@Autowired` on a field?

> What if there is no default constructor?

> What if there are multiple constructors?

> How does Spring actually resolve an ambiguous dependency?

Those are **very common Spring interview follow-ups**.

[1]: https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html?utm_source=chatgpt.com "Bean Scopes :: Spring Framework"
[2]: https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com "Using the @Bean Annotation :: Spring Framework"
[3]: https://docs.spring.io/spring-framework/reference/6.2/web/websocket/stomp/scope.html?utm_source=chatgpt.com "WebSocket Scope :: Spring Framework"

