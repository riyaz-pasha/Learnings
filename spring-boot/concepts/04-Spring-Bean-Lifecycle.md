# Topic 4 — Spring Bean Lifecycle

This is a **very common interview topic** because it tests whether you understand what Spring is actually doing behind annotations.

A beginner thinks:

```text
@Service
   ↓
Spring creates object
```

But the real picture is closer to:

```text
Bean definition
      ↓
Instantiation
      ↓
Dependency injection
      ↓
Aware callbacks
      ↓
BeanPostProcessor
      ↓
@PostConstruct
      ↓
InitializingBean
      ↓
custom init method
      ↓
BeanPostProcessor
      ↓
READY
      ↓
@PreDestroy
      ↓
DisposableBean
      ↓
custom destroy method
```

The exact lifecycle has more details and extension points, but this is the mental model we want. Spring's official documentation explicitly documents the initialization and destruction callback mechanisms and the role of `BeanPostProcessor`. ([Home][1])

---

# 1. Why does Bean Lifecycle exist?

Suppose you have:

```java
@Service
public class PaymentService {

    private DatabaseClient databaseClient;

    public void process() {
        // business logic
    }
}
```

Maybe before the service can be used, you need to:

```text
load configuration
validate configuration
create cache
open a resource
initialize a client
prepare data structures
```

And when the application shuts down:

```text
close resource
flush data
stop connection
release thread pool
clear cache
```

You could manually do this:

```java
PaymentService service = new PaymentService();

service.initialize();

// use service

service.cleanup();
```

But Spring manages the lifecycle for you.

So the lifecycle answers:

> **When and how does Spring create, initialize, use, and destroy a bean?**

---

# 2. High-level lifecycle

Let's start with the simplified version.

```text
                  Spring Container
                        |
                        ↓
                BeanDefinition
                        |
                        ↓
                  Instantiate
                        |
                        ↓
              Dependency Injection
                        |
                        ↓
                Aware callbacks
                        |
                        ↓
       BeanPostProcessor (before init)
                        |
                        ↓
                  @PostConstruct
                        |
                        ↓
              afterPropertiesSet()
                        |
                        ↓
             custom init-method
                        |
                        ↓
       BeanPostProcessor (after init)
                        |
                        ↓
                    READY
                        |
                    application
                        |
                        ↓
                 context shutdown
                        |
                        ↓
                  @PreDestroy
                        |
                        ↓
                    destroy()
                        |
                        ↓
               custom destroy-method
```

The official Spring documentation specifies the relative ordering of `@PostConstruct`, `InitializingBean.afterPropertiesSet()`, and a custom initialization method; similarly for destruction callbacks. ([Home][1])

---

# 3. Step 1 — BeanDefinition exists

Before creating the object, Spring needs metadata describing the bean.

For:

```java
@Service
public class PaymentService {
}
```

Spring conceptually has:

```text
BeanDefinition
-------------------------
Class: PaymentService
Scope: singleton
Dependencies: ...
Init method: ...
Destroy method: ...
...
```

Think of this as the **recipe**.

Not the actual object.

```text
BeanDefinition
    ↓
"How should I create/manage this?"

Bean instance
    ↓
"The actual PaymentService object"
```

This happens before normal bean instantiation.

---

# 4. Step 2 — Bean Instantiation

Spring now creates the object.

Conceptually:

```java
new PaymentService(...)
```

But the actual creation process can involve dependency resolution, constructor selection, factory methods, scopes, and other container logic.

Suppose:

```java
@Service
public class PaymentService {

    private final PaymentGateway gateway;

    public PaymentService(PaymentGateway gateway) {
        this.gateway = gateway;
    }
}
```

Spring has to resolve:

```text
PaymentGateway
      ↓
create/find dependency
      ↓
pass it into PaymentService constructor
```

So construction and dependency injection are connected.

---

# 5. Step 3 — Dependency Injection

For constructor injection:

```java
public PaymentService(PaymentGateway gateway) {
    this.gateway = gateway;
}
```

Spring resolves:

```text
PaymentGateway
```

and supplies it.

At this point:

```text
PaymentService
      |
      ↓
PaymentGateway
```

has been wired.

For setter/field injection, the corresponding injection work happens as part of bean population/configuration.

---

# 6. Is the bean ready after constructor execution?

**No.**

This is an important interview question.

Consider:

```java
@Service
public class PaymentService {

    private final PaymentGateway gateway;

    public PaymentService(PaymentGateway gateway) {
        this.gateway = gateway;
        System.out.println("Constructor");
    }

    @PostConstruct
    public void init() {
        System.out.println("PostConstruct");
    }
}
```

The constructor has run, but Spring still has lifecycle processing to perform.

So:

```text
Constructor finished
       ≠
Bean fully initialized
```

That distinction is crucial.

---

# 7. Step 4 — Aware Interfaces

Spring provides several `Aware` interfaces that allow a bean to receive certain container-related information.

Examples:

```text
BeanNameAware
BeanFactoryAware
ApplicationContextAware
EnvironmentAware
```

For example:

```java
@Component
public class MyComponent implements BeanNameAware {

    @Override
    public void setBeanName(String name) {
        System.out.println("Bean name = " + name);
    }
}
```

Spring can tell the bean:

```text
"You are registered under this bean name."
```

Similarly:

```java
ApplicationContextAware
```

can provide access to the current `ApplicationContext`.

Spring documents these interfaces as part of its bean lifecycle customization facilities. ([Home][1])

### Interview point

Don't confuse:

```text
Aware callbacks
```

with:

```text
Dependency Injection
```

They are related to container interaction, but they solve different needs.

---

# 8. Why are `Aware` interfaces sometimes discouraged?

Suppose:

```java
@Component
public class OrderService
        implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    public void setApplicationContext(
            ApplicationContext context) {
        this.context = context;
    }
}
```

Now the class explicitly knows about Spring.

That creates framework coupling.

Usually this:

```java
public OrderService(PaymentService paymentService)
```

is cleaner than:

```java
ApplicationContext context;
context.getBean(PaymentService.class);
```

Spring's documentation similarly recommends dependency injection over programmatic lookup because direct `ApplicationContext` access couples application code to Spring. ([Home][1])

---

# 9. Step 5 — `BeanPostProcessor` BEFORE initialization

Now we reach one of the most important lifecycle concepts.

A `BeanPostProcessor` can process the bean:

```text
after instantiation/configuration
before initialization callbacks
```

Spring's `BeanPostProcessor` interface has two main callbacks:

```java
postProcessBeforeInitialization(...)
postProcessAfterInitialization(...)
```

The container invokes the first before initialization callbacks and the second afterward. ([Home][2])

Conceptually:

```text
Bean created
    ↓
BeanPostProcessor
BeforeInitialization
```

---

# 10. Why does `BeanPostProcessor` exist?

It allows Spring or application code to add behavior around bean creation.

For example:

```text
detect annotations
perform custom validation
inject special dependencies
wrap object
create proxy
```

A simplified custom processor:

```java
@Component
public class LoggingPostProcessor
        implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(
            Object bean,
            String beanName) {

        System.out.println(
            "Before init: " + beanName
        );

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(
            Object bean,
            String beanName) {

        System.out.println(
            "After init: " + beanName
        );

        return bean;
    }
}
```

Now every eligible bean can pass through this processor.

---

# 11. Step 6 — `@PostConstruct`

Now the bean's `@PostConstruct` method runs.

Example:

```java
@Service
public class CacheService {

    @PostConstruct
    public void initialize() {
        System.out.println("Initializing cache");
    }
}
```

This is intended for initialization work that should happen after the bean has been constructed and dependencies have been supplied.

Modern Spring recommends `@PostConstruct` rather than implementing Spring-specific lifecycle interfaces when possible, because it avoids coupling the class to Spring-specific interfaces. ([Home][1])

---

# 12. Where does `@PostConstruct` come from?

In modern Spring applications, use:

```java
import jakarta.annotation.PostConstruct;
```

not:

```java
javax.annotation.PostConstruct
```

The `javax.annotation` package was removed from the JDK; in Jakarta EE 9 the annotations moved to `jakarta.annotation`. ([Home][3])

This is particularly relevant for **Spring Boot 3+** interviews.

---

# 13. What should go inside `@PostConstruct`?

Good examples:

```text
validate configuration
initialize in-memory structures
prepare local caches
derive data from configuration
```

For example:

```java
@PostConstruct
public void initialize() {
    validateConfiguration();
    buildLookupTable();
}
```

But don't treat it as a place to perform huge amounts of slow startup work.

Current Spring documentation warns that initialization callbacks run under the singleton creation lock and recommends later lifecycle mechanisms such as `SmartInitializingSingleton` or `ContextRefreshedEvent` for expensive post-initialization activity. ([Home][1])

That is a very good advanced interview point.

---

# 14. Step 7 — `InitializingBean`

A bean can implement:

```java
InitializingBean
```

Example:

```java
@Service
public class CacheService
        implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        System.out.println(
            "InitializingBean callback"
        );
    }
}
```

Spring calls:

```java
afterPropertiesSet()
```

after the container has supplied the bean's required properties. ([Home][1])

---

# 15. Is `InitializingBean` recommended?

Generally, not for normal application code.

Why?

Because:

```java
public class CacheService
        implements InitializingBean
```

creates an explicit dependency on Spring's API.

By contrast:

```java
@PostConstruct
public void initialize()
```

keeps the class less coupled to Spring-specific lifecycle interfaces.

Spring's current documentation explicitly recommends `@PostConstruct` or a generic initialization method instead of `InitializingBean` when possible. ([Home][1])

---

# 16. Step 8 — Custom initialization method

You can configure a normal method as an initialization callback.

For example:

```java
@Bean(initMethod = "init")
public CacheService cacheService() {
    return new CacheService();
}
```

with:

```java
public void init() {
    System.out.println("Custom initialization");
}
```

This has a useful advantage:

```text
Your class doesn't need to implement
a Spring-specific interface.
```

Spring supports custom initialization methods as an alternative to lifecycle interfaces. ([Home][4])

---

# 17. Initialization order — VERY IMPORTANT

Suppose a bean uses all three mechanisms:

```java
@PostConstruct
public void postConstruct() {
    System.out.println("1");
}
```

```java
@Override
public void afterPropertiesSet() {
    System.out.println("2");
}
```

and:

```java
@Bean(initMethod = "init")
```

with:

```java
public void init() {
    System.out.println("3");
}
```

The documented order is:

```text
1. @PostConstruct
2. afterPropertiesSet()
3. custom init method
```

Spring documents this ordering explicitly. ([Home][1])

But remember the `BeanPostProcessor` boundaries:

```text
postProcessBeforeInitialization()
        ↓
@PostConstruct
        ↓
afterPropertiesSet()
        ↓
custom init method
        ↓
postProcessAfterInitialization()
```

This is the sequence worth knowing for interviews. ([Home][2])

---

# 18. Full initialization diagram

Here's the version I recommend memorizing:

```text
       BeanDefinition
             ↓
       Instantiation
             ↓
   Dependency Injection
             ↓
       Aware callbacks
             ↓
 BeanPostProcessor
 BeforeInitialization
             ↓
        @PostConstruct
             ↓
    afterPropertiesSet()
             ↓
      custom init method
             ↓
 BeanPostProcessor
 AfterInitialization
             ↓
        READY BEAN
```

This diagram is excellent for interviews.

---

# 19. Can `postProcessAfterInitialization()` replace the bean?

Yes.

This is a very important detail.

A `BeanPostProcessor` doesn't necessarily have to return the same object.

For example:

```java
@Override
public Object postProcessAfterInitialization(
        Object bean,
        String beanName) {

    return createProxy(bean);
}
```

The container can then expose the returned object.

This is one reason the object you receive from Spring might actually be a **proxy** rather than the raw object.

Spring's documentation explicitly notes that a post-processor can wrap a bean with a proxy, and that Spring AOP uses post-processors to provide proxying. ([Home][2])

This becomes critical when we discuss:

```text
@Transactional
@Async
@Cacheable
Spring Security
AOP
```

---

# 20. Why is this important for `@Transactional`?

Suppose:

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        // ...
    }
}
```

The object exposed by Spring may be a proxy around the target:

```text
Caller
   ↓
Proxy
   ↓
OrderService target
```

Conceptually:

```text
caller
  ↓
transaction proxy
  ↓
begin transaction
  ↓
real OrderService
  ↓
commit/rollback
```

This is one of the reasons:

```java
orderService.createOrder()
```

behaves differently from directly constructing:

```java
new OrderService()
```

We will eventually spend a dedicated topic on this because it is one of the most common Spring interview follow-ups.

---

# 21. Step 9 — Bean is ready

After initialization callbacks and post-processing finish:

```text
READY
```

The bean can now be used by the application.

So:

```text
constructor
    ≠
initialized bean
```

and:

```text
initialized bean
    ≠
necessarily raw object
```

because post-processing may wrap it.

---

# 22. What happens during application shutdown?

Now we go in reverse toward destruction.

When the Spring container shuts down, eligible beans get destruction callbacks.

For example:

```java
@PreDestroy
public void cleanup() {
    System.out.println("Cleaning resources");
}
```

This is the standard modern approach for cleanup callbacks. Spring documents `@PreDestroy` alongside `@PostConstruct` as the preferred annotation-based lifecycle mechanism. ([Home][1])

---

# 23. `@PreDestroy`

Example:

```java
@Component
public class ConnectionManager {

    @PostConstruct
    public void init() {
        System.out.println("Opening resources");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("Closing resources");
    }
}
```

Conceptually:

```text
Application startup
       ↓
@PostConstruct
       ↓
bean used
       ↓
application shutdown
       ↓
@PreDestroy
```

---

# 24. `DisposableBean`

Another mechanism:

```java
@Component
public class CacheService
        implements DisposableBean {

    @Override
    public void destroy() {
        System.out.println("Cleanup");
    }
}
```

Spring calls:

```java
destroy()
```

when the containing bean factory is destroyed. ([Home][1])

Again, for application code, `@PreDestroy` is generally preferred because it avoids coupling to Spring-specific interfaces.

---

# 25. Custom destroy method

You can also define:

```java
@Bean(destroyMethod = "cleanup")
public CacheService cacheService() {
    return new CacheService();
}
```

Then:

```java
public void cleanup() {
    // cleanup
}
```

Spring will call the configured destruction method when appropriate. ([Home][4])

---

# 26. Destruction order

When all three mechanisms are configured with different methods, Spring documents this order:

```text
1. @PreDestroy
2. DisposableBean.destroy()
3. custom destroy method
```

([Home][1])

So the complete mental model becomes:

```text
INITIALIZATION

BeanPostProcessor
 BeforeInitialization
        ↓
@PostConstruct
        ↓
afterPropertiesSet()
        ↓
custom init()
        ↓
BeanPostProcessor
 AfterInitialization
        ↓
READY


DESTRUCTION

@PreDestroy
        ↓
destroy()
        ↓
custom destroy()
```

---

# 27. Does `@PreDestroy` always run?

No.

This is an excellent interview trap.

The lifecycle callbacks are tied to the **Spring container lifecycle**.

If you do:

```java
PaymentService service =
        new PaymentService(...);
```

Spring doesn't manage that object.

Therefore:

```java
@PreDestroy
```

isn't magically executed by Java.

Similarly, abrupt process termination can prevent graceful cleanup.

So:

```text
Spring-managed bean
       +
graceful context shutdown
       ↓
destruction callbacks
```

is the right mental model.

---

# 28. Prototype beans — important exception

Now comes an important scope-related lifecycle question.

For singleton beans, Spring normally manages both creation and destruction callbacks.

For prototype beans:

```java
@Scope("prototype")
```

Spring creates a new instance each time it's requested, but it does **not** manage the full destruction lifecycle of the prototype instance after handing it to the caller.

So you shouldn't assume:

```text
prototype bean
    ↓
@PreDestroy automatically called by Spring
```

That is generally not the case.

We'll cover this deeply in the bean scopes topic.

---

# 29. `@PostConstruct` vs constructor

This is another very common question.

### Constructor

```java
public PaymentService(PaymentGateway gateway) {
    this.gateway = gateway;
}
```

Used for:

```text
constructing object
receiving required dependencies
establishing basic invariants
```

### `@PostConstruct`

```java
@PostConstruct
public void init() {
}
```

Used for:

```text
initialization after dependency injection
```

So:

```text
Constructor
    ↓
dependency object supplied
    ↓
@PostConstruct
```

Do not attempt to use injected fields before they are injected.

---

# 30. Example interview trap

Consider:

```java
@Component
public class TestService {

    @Autowired
    private Repository repository;

    public TestService() {
        System.out.println(repository);
    }

    @PostConstruct
    public void init() {
        System.out.println(repository);
    }
}
```

What happens?

During constructor execution:

```text
repository == null
```

because field injection hasn't happened yet.

Later:

```text
@PostConstruct
```

runs after dependency injection, so `repository` is available.

This is one reason constructor injection is cleaner.

With:

```java
public TestService(Repository repository) {
    this.repository = repository;
}
```

the dependency is available immediately during construction.

---

# 31. An interesting advanced callback: `SmartInitializingSingleton`

There is another lifecycle mechanism:

```java
SmartInitializingSingleton
```

It provides:

```java
afterSingletonsInstantiated()
```

This callback runs after regular singleton initialization is complete.

This can be useful when something needs to happen **after all regular singleton beans have been instantiated**. Spring specifically points to this or a context refresh event for expensive post-initialization activity that should occur outside the normal singleton creation lock. ([Home][1])

Example:

```java
@Component
public class StartupTask
        implements SmartInitializingSingleton {

    @Override
    public void afterSingletonsInstantiated() {
        System.out.println(
            "All regular singletons are initialized"
        );
    }
}
```

This is advanced but worth knowing for senior-level interviews.

---

# 32. `ApplicationReadyEvent` vs `@PostConstruct`

These are **not the same thing**.

`@PostConstruct` belongs to an individual bean's initialization.

Something like:

```text
@PostConstruct
```

means:

> "This particular bean has reached its initialization point."

Whereas an application readiness event is part of the broader application startup lifecycle.

Conceptually:

```text
Bean A initialized
Bean B initialized
Bean C initialized
      ↓
application startup continues
      ↓
application ready
```

So don't use `@PostConstruct` as a synonym for:

> "The entire application is fully started."

That's an important distinction in production systems.

---

# 33. Expensive startup work

Suppose:

```java
@PostConstruct
public void init() {

    load10MillionRecords();
}
```

This may be problematic because it can delay startup and, depending on the operation, interact poorly with container initialization locking.

A better architecture might use:

```text
SmartInitializingSingleton
ContextRefreshedEvent
ApplicationReadyEvent
background initialization
```

depending on what exactly needs to happen.

Spring's current documentation specifically recommends later lifecycle mechanisms when expensive post-initialization work is required. ([Home][1])

---

# 34. `BeanPostProcessor` vs lifecycle callback

This distinction causes confusion.

### `@PostConstruct`

A lifecycle callback **on your bean**.

```java
@PostConstruct
public void init() {}
```

### `BeanPostProcessor`

An extension point that allows another Spring-managed component to **process beans**.

```java
postProcessBeforeInitialization(bean, name)
postProcessAfterInitialization(bean, name)
```

So:

```text
Bean
 ↓
@PostConstruct

versus

BeanPostProcessor
 ↓
process Bean
```

---

# 35. Where does `@PostConstruct` actually get executed?

This is a nice advanced question.

Spring's support for `@PostConstruct` is implemented through:

```text
CommonAnnotationBeanPostProcessor
```

Spring's current documentation states that this post-processor recognizes `@PostConstruct` and `@PreDestroy`. ([Home][5])

So conceptually:

```text
@PostConstruct
      ↓
CommonAnnotationBeanPostProcessor
      ↓
invokes callback
```

That's a very good answer when an interviewer asks:

> "How does Spring know to execute `@PostConstruct`?"

---

# 36. Why is `BeanPostProcessor` called a "post processor"?

Because it processes beans **around their initialization**.

Think:

```text
Instantiation
     ↓
population/configuration
     ↓
BPP before initialization
     ↓
initialization callbacks
     ↓
BPP after initialization
```

It's not necessarily "post" in the sense of:

> everything is finished.

It's a named lifecycle extension mechanism.

---

# 37. Does every bean go through every callback?

No.

For example:

```text
Does bean implement InitializingBean?
      ↓
No
      ↓
afterPropertiesSet() doesn't run
```

Likewise:

```text
Does bean have @PostConstruct?
      ↓
No
      ↓
no @PostConstruct callback
```

And:

```text
Does bean have custom init method?
      ↓
No
      ↓
no custom init callback
```

The lifecycle infrastructure invokes the callbacks that apply to that bean.

---

# 38. One subtle point: multiple `BeanPostProcessor`s

You can have:

```text
BPP A
BPP B
BPP C
```

Spring can order applicable post-processors; ordering can be controlled using mechanisms such as `Ordered` where applicable. ([Home][2])

Conceptually:

```text
Bean
 ↓
BPP A
 ↓
BPP B
 ↓
BPP C
 ↓
@PostConstruct
...
```

The precise processing chain can become more complicated depending on the infrastructure involved.

For interviews, remember:

> Multiple `BeanPostProcessor`s can participate, and Spring provides ordering mechanisms for applicable post-processors.

---

# 39. The full example

Let's build one bean containing several lifecycle mechanisms.

```java
@Component
public class PaymentService
        implements InitializingBean, DisposableBean {

    public PaymentService() {
        System.out.println("1. Constructor");
    }

    @PostConstruct
    public void postConstruct() {
        System.out.println("3. @PostConstruct");
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("4. afterPropertiesSet");
    }

    public void customInit() {
        System.out.println("5. custom init");
    }

    @PreDestroy
    public void preDestroy() {
        System.out.println("7. @PreDestroy");
    }

    @Override
    public void destroy() {
        System.out.println("8. destroy");
    }

    public void customDestroy() {
        System.out.println("9. custom destroy");
    }
}
```

With:

```java
@Bean(
    initMethod = "customInit",
    destroyMethod = "customDestroy"
)
public PaymentService paymentService() {
    return new PaymentService();
}
```

The simplified visible ordering is:

```text
1. Constructor

2. BeanPostProcessor.beforeInitialization()

3. @PostConstruct

4. afterPropertiesSet()

5. customInit()

6. BeanPostProcessor.afterInitialization()

-------------------------
Bean is ready
-------------------------

7. @PreDestroy

8. destroy()

9. customDestroy()
```

The documented ordering of the lifecycle callback mechanisms is exactly as shown for the three initialization mechanisms and three destruction mechanisms. ([Home][1])

---

# 40. Important correction to a common oversimplification

People sometimes memorize:

```text
Constructor
→ @PostConstruct
→ BeanPostProcessor
```

That's incomplete.

For interview purposes, distinguish:

```text
BeanPostProcessor.beforeInitialization()
        ↓
@PostConstruct
        ↓
afterPropertiesSet()
        ↓
custom init()
        ↓
BeanPostProcessor.afterInitialization()
```

That's the useful sequence to remember. ([Home][2])

---

# 41. What happens when initialization fails?

Suppose:

```java
@PostConstruct
public void init() {
    throw new RuntimeException("Initialization failed");
}
```

The bean fails to initialize.

Consequently, application startup may fail if the bean is required during context initialization.

This is actually desirable for many configuration errors:

```text
bad configuration
      ↓
bean initialization fails
      ↓
application fails early
```

rather than:

```text
application starts
      ↓
hours later first request
      ↓
failure
```

That's one reason initialization callbacks should often be used for **validation and required startup state**, rather than arbitrary business activity.

---

# 42. What about dependencies?

Suppose:

```text
OrderService
     ↓
PaymentService
```

If `OrderService` depends on `PaymentService`, Spring has to make sure the dependency is available before the dependent bean is usable.

Conceptually:

```text
PaymentService
     ↓
initialized
     ↓
OrderService
     ↓
initialized
```

But real startup ordering can involve more than direct dependency relationships, especially with lifecycle processors, lazy beans, and explicit ordering constructs such as `@DependsOn`.

The key principle:

> **Dependency relationships influence bean creation order.**

---

# 43. `@DependsOn`

Sometimes you explicitly need:

```java
@DependsOn("databaseInitializer")
@Component
public class OrderService {
}
```

This says:

```text
databaseInitializer
       ↓
must initialize first
       ↓
OrderService
```

Spring documents `@DependsOn` as a way to force specified beans to initialize before the current bean beyond what its direct dependencies imply. ([Home][6])

This is useful but shouldn't be your default approach for ordinary dependencies.

---

# 44. Common interview traps

### Trap 1

> "Constructor is the first lifecycle callback."

A better answer:

> Construction is the object-instantiation step; lifecycle processing continues afterward.

---

### Trap 2

> "`@PostConstruct` means application started."

No.

It means that particular bean reached its post-construction callback.

---

### Trap 3

> "BeanPostProcessor happens after initialization."

Not exactly.

It participates both:

```text
before initialization
and
after initialization
```

That's why it has two callbacks. ([Home][2])

---

### Trap 4

> "`@PreDestroy` always runs."

No.

It depends on Spring-managed lifecycle and graceful destruction.

---

### Trap 5

> "`InitializingBean` is the recommended approach."

Generally no. Spring recommends `@PostConstruct` or generic callback methods to avoid Spring-specific coupling. ([Home][1])

---

### Trap 6

> "Spring creates a raw object and gives it to us."

Not necessarily.

A bean can be wrapped/replaced by post-processing, including proxy creation for Spring AOP. ([Home][2])

---

# 45. How I'd answer the interview question

### Interviewer:

**Explain Spring Bean Lifecycle.**

A strong answer:

> A Spring bean starts with its bean definition in the container. Spring instantiates the bean, resolves and injects its dependencies, invokes applicable `Aware` callbacks, and then applies `BeanPostProcessor` processing. During initialization, `@PostConstruct`, `InitializingBean.afterPropertiesSet()`, and any configured custom initialization method can execute in that order. Finally, `postProcessAfterInitialization()` runs and the resulting bean is available for use. During context shutdown, Spring invokes applicable destruction callbacks such as `@PreDestroy`, `DisposableBean.destroy()`, and a configured destroy method.

That's a strong **60–90 second answer**.

---

# 46. EPAM-style follow-up questions

You should now be able to handle:

**Q:** What is Bean lifecycle?

**Q:** What is `@PostConstruct`?

**Q:** What is `@PreDestroy`?

**Q:** What is `InitializingBean`?

**Q:** What is `DisposableBean`?

**Q:** `@PostConstruct` vs `afterPropertiesSet()`?

**Q:** Why is `@PostConstruct` preferred?

**Q:** What is `BeanPostProcessor`?

**Q:** What are `postProcessBeforeInitialization()` and `postProcessAfterInitialization()`?

**Q:** In what order are lifecycle callbacks called?

**Q:** Can `BeanPostProcessor` return a different object?

**Q:** Why can a Spring bean actually be a proxy?

**Q:** Does `@PreDestroy` work for prototype beans?

**Q:** What happens if `@PostConstruct` throws an exception?

**Q:** What is `SmartInitializingSingleton`?

**Q:** What is `ApplicationContextAware`?

**Q:** Why shouldn't we use `ApplicationContext.getBean()` everywhere?

---

# 47. Revision diagram

This is the diagram I'd memorize before an interview:

```text
                 BeanDefinition
                       ↓
                 Instantiation
                       ↓
              Dependency Injection
                       ↓
                Aware callbacks
                       ↓
     BeanPostProcessor.beforeInitialization()
                       ↓
                  @PostConstruct
                       ↓
             afterPropertiesSet()
                       ↓
                custom init()
                       ↓
     BeanPostProcessor.afterInitialization()
                       ↓
                 ┌──────────┐
                 │   READY  │
                 └──────────┘
                       ↓
                  application
                       ↓
                Context shutdown
                       ↓
                  @PreDestroy
                       ↓
                    destroy()
                       ↓
               custom destroy()
```

And the **three callback orders**:

```text
Initialization:
@PostConstruct
    ↓
afterPropertiesSet()
    ↓
custom init()

Destruction:
@PreDestroy
    ↓
destroy()
    ↓
custom destroy()
```

([Home][1])

---

# 48. One final conceptual connection

You have now learned:

```text
Topic 1
Spring vs Spring Boot
        ↓
Topic 2
IoC / Dependency Injection
        ↓
Topic 3
IoC Container / ApplicationContext
        ↓
Topic 4
Bean Lifecycle
```

Everything is beginning to connect:

```text
@SpringBootApplication
        ↓
ApplicationContext
        ↓
BeanDefinitions
        ↓
Beans
        ↓
Dependency Injection
        ↓
Bean Lifecycle
        ↓
BeanPostProcessor
        ↓
Proxies
        ↓
AOP / @Transactional / Security / Caching
```

The next topic is **Bean Scopes**, and this one has several interview traps:

```text
singleton
prototype
request
session
application
websocket
```

We'll especially cover:

> **"Is a Spring singleton the same as the Singleton design pattern?"**

and:

> **"What happens when a singleton depends on a prototype bean?"**

Those are very common deeper Spring questions.

[1]: https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html?utm_source=chatgpt.com "Customizing the Nature of a Bean :: Spring Framework"
[2]: https://docs.spring.io/spring-framework/reference/core/beans/factory-extension.html?utm_source=chatgpt.com "Container Extension Points :: Spring Framework"
[3]: https://docs.spring.io/spring-framework/reference/6.2/core/beans/annotation-config/postconstruct-and-predestroy-annotations.html?utm_source=chatgpt.com "Using @PostConstruct and @PreDestroy :: Spring Framework"
[4]: https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com "Using the @Bean Annotation :: Spring Framework"
[5]: https://docs.spring.io/spring/reference/core/beans/annotation-config/postconstruct-and-predestroy-annotations.html?utm_source=chatgpt.com "Using @PostConstruct and @PreDestroy :: Spring Framework"
[6]: https://docs.spring.io/spring-framework/reference/core/beans/java/composing-configuration-classes.html?utm_source=chatgpt.com "Composing Java-based Configurations :: Spring Framework"

