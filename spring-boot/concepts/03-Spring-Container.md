# Topic 3 — Spring IoC Container, `BeanFactory`, `ApplicationContext`, and Bean Creation

This is where the previous topic becomes concrete.

We said:

> Spring takes control of creating and wiring objects.

Now the obvious interviewer question is:

> **"Which component in Spring actually does that?"**

Answer:

> **The Spring IoC container.**

And the two names you need to know are:

```text
BeanFactory
ApplicationContext
```

`ApplicationContext` extends `BeanFactory` and adds application-oriented capabilities such as event publication, message/resource handling, and easier AOP integration. Spring generally recommends using an `ApplicationContext` for normal applications. ([Home][1])

---

# 1. What is a Spring Bean?

Before understanding the container, let's define a **bean**.

A Spring bean is simply:

> **An object that is instantiated, assembled, and managed by the Spring IoC container.** ([Home][1])

For example:

```java
@Service
public class OrderService {
}
```

Spring can discover this class and create an instance:

```text
OrderService object
        ↓
registered in Spring container
        ↓
managed as a Spring bean
```

Compare:

```java
OrderService service = new OrderService();
```

This is just a normal Java object.

Whereas:

```java
@Service
public class OrderService {
}
```

when discovered by Spring becomes a Spring-managed bean.

### Very important interview point

> **Not every object in a Spring application is a Spring bean.**

Domain objects are often ordinary Java objects:

```java
Order order = new Order();
```

Repositories, services, controllers, infrastructure components, etc. are commonly Spring beans.

Spring's own documentation also notes that fine-grained domain objects are typically not configured as container beans; repositories and business logic create/load them as needed. ([Home][2])

---

# 2. What is the IoC Container?

Think of the container as the **object manager** for your application.

Its responsibilities include:

```text
                  Spring IoC Container
                           |
        ┌──────────────────┼──────────────────┐
        ↓                  ↓                  ↓
   Create beans       Resolve dependencies   Configure beans
        ↓                  ↓                  ↓
   Initialize         Inject them            Manage lifecycle
```

The container gets instructions from **configuration metadata**.

That metadata can come from:

```text
@Component / @Service / @Repository / ...
@Configuration + @Bean
XML
programmatic configuration
```

Spring then turns that metadata into bean definitions and uses them to create and manage beans. ([Home][2])

---

# 3. The key concept: `BeanDefinition`

This is one of those things interviewers may ask when they want to see whether you actually understand Spring internally.

Suppose:

```java
@Service
public class OrderService {
}
```

Don't imagine that Spring immediately stores an `OrderService` object just because it sees the annotation.

Conceptually, there is an intermediate description of how the bean should be created.

That description is a:

```text
BeanDefinition
```

Spring's documentation describes a `BeanDefinition` as metadata containing things such as the bean's class, scope, lifecycle information, dependencies, and other configuration settings. It is essentially a **recipe** for creating the bean. ([Home][3])

Think:

```text
@Service class
      ↓
metadata discovered
      ↓
BeanDefinition
      ↓
recipe for creating bean
      ↓
actual bean instance
```

---

# 4. BeanDefinition vs Bean

This distinction is extremely useful.

### BeanDefinition

A **recipe / metadata**:

```text
Class = OrderService
Scope = singleton
Dependencies = PaymentService
...
```

### Bean

The **actual object**:

```text
OrderService@5a07e868
```

So:

```text
BeanDefinition
      ↓
"How should I create this?"
      ↓
Bean
      ↓
"Here is the actual object."
```

This distinction also explains why one bean definition can describe multiple instances when a non-singleton scope is used. ([Home][4])

---

# 5. What does the container actually do?

A simplified lifecycle is:

```text
1. Read configuration metadata
2. Register BeanDefinitions
3. Create required beans
4. Resolve dependencies
5. Inject dependencies
6. Apply bean post-processors
7. Run initialization callbacks
8. Make bean available
```

And eventually during shutdown:

```text
9. Destroy applicable beans
```

Spring exposes several extension points around this process, including `BeanPostProcessor`, which can act before and after initialization and can even wrap beans with proxies. ([Home][5])

This is the high-level picture you should have in your head.

---

# 6. `BeanFactory`

Now let's look at the first container interface.

```java
BeanFactory
```

It provides the fundamental IoC functionality:

```text
Bean creation
Dependency wiring
Bean lookup
Basic container functionality
```

Spring describes `BeanFactory` as the underlying basis of its IoC functionality. ([Home][6])

Example:

```java
BeanFactory factory = ...;

OrderService service =
        factory.getBean(OrderService.class);
```

The factory knows how to provide the bean.

---

# 7. `ApplicationContext`

`ApplicationContext` extends `BeanFactory`.

Conceptually:

```text
BeanFactory
    ↑
    |
ApplicationContext
```

So:

```text
ApplicationContext
   =
BeanFactory functionality
   +
additional application features
```

Some important additions include:

```text
Event publication
Internationalization/message resources
Resource loading
AOP integration
Application-aware facilities
Web-aware contexts
```

The official documentation calls `ApplicationContext` a complete superset of `BeanFactory` and recommends it for normal application use. ([Home][1])

---

# 8. `BeanFactory` vs `ApplicationContext`

This is a **classic interview question**.

| `BeanFactory`                           | `ApplicationContext`              |
| --------------------------------------- | --------------------------------- |
| Basic IoC container                     | Advanced IoC container            |
| Bean creation/wiring                    | Bean creation/wiring              |
| Lower-level API                         | Higher-level API                  |
| Fewer built-in application features     | Events, resources, messages, etc. |
| More manual bootstrapping may be needed | More complete setup               |
| Rarely used directly in typical apps    | Common choice for applications    |

The most important sentence:

> **`ApplicationContext` is a superset of `BeanFactory`.**

---

# 9. Then why does `BeanFactory` still exist?

Because Spring is designed to be highly modular and extensible.

`BeanFactory` is a lower-level foundation.

Spring's current documentation notes that `DefaultListableBeanFactory` is a key delegate underneath higher-level contexts such as `GenericApplicationContext`. ([Home][6])

So conceptually:

```text
ApplicationContext
       |
       ↓
higher-level container behavior
       |
       ↓
BeanFactory infrastructure
```

This layered design gives Spring flexibility.

---

# 10. A very important difference: eager initialization

Here's a common interview trap.

By default, an `ApplicationContext` eagerly creates singleton beans during context initialization.

Meaning:

```text
Application starts
      ↓
ApplicationContext initializes
      ↓
singleton beans are created
```

This is useful because many configuration/environment errors are discovered during startup rather than much later.

A bean can be made lazy with `@Lazy`, in which case it is normally created when first requested. ([Home][7])

So interview answer:

> `ApplicationContext` generally pre-instantiates non-lazy singleton beans, whereas lower-level `BeanFactory` behavior is more lazy and depends on how it is bootstrapped.

Don't overstate this as "BeanFactory always lazy" in every possible setup; the exact behavior depends on the concrete implementation and bootstrapping.

---

# 11. Example of eager vs lazy

```java
@Service
public class OrderService {
}
```

Normally:

```text
Application startup
       ↓
OrderService created
```

With:

```java
@Service
@Lazy
public class OrderService {
}
```

conceptually:

```text
Application startup
       ↓
OrderService NOT created yet
       ↓
First time requested
       ↓
OrderService created
```

And there's a subtlety:

If a lazy bean is required by a non-lazy singleton, Spring may still create that lazy bean during startup to satisfy the dependency. ([Home][7])

That's a great interview follow-up.

---

# 12. How does `@Service` become a bean?

Suppose:

```java
@Service
public class PaymentService {
}
```

How does Spring know about it?

Through **component scanning**.

Conceptually:

```text
@ComponentScan
      ↓
scan package(s)
      ↓
find @Component and stereotype annotations
      ↓
register bean definitions
      ↓
container can create beans
```

Spring's classpath scanning can detect candidate components and register corresponding bean definitions with the container. ([Home][8])

And:

```text
@Service
@Repository
@Controller
@RestController
```

are all part of the component-model approach.

We'll study exactly how the stereotype annotations work later.

---

# 13. What about `@Bean`?

Not every bean comes from component scanning.

You can explicitly define one:

```java
@Configuration
public class AppConfig {

    @Bean
    public PaymentService paymentService() {
        return new StripePaymentService();
    }
}
```

Now:

```text
@Bean method
     ↓
BeanDefinition
     ↓
Spring container
     ↓
PaymentService bean
```

`@Bean` is a method-level annotation used to declare a bean; by default, the bean name is the method name. ([Home][9])

---

# 14. Two common ways beans enter the container

### Component scanning

```java
@Service
public class OrderService {
}
```

Spring discovers it.

### Explicit configuration

```java
@Configuration
public class Config {

    @Bean
    public OrderService orderService() {
        return new OrderService();
    }
}
```

You explicitly tell Spring how to construct it.

So:

```text
               Spring Container
                       |
             ┌─────────┴─────────┐
             ↓                   ↓
      Component scanning      @Bean
             ↓                   ↓
      BeanDefinition        BeanDefinition
```

---

# 15. Dependency resolution

Suppose we have:

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

And:

```java
@Service
public class StripePaymentService
        implements PaymentService {
}
```

The container has to answer:

> "What object should I pass to the `PaymentService` constructor parameter?"

Conceptually:

```text
OrderService requires PaymentService
                ↓
Search container for matching bean
                ↓
StripePaymentService found
                ↓
Pass it to constructor
                ↓
OrderService created
```

Dependency resolution becomes more interesting when there are multiple candidates.

That's where:

```text
@Primary
@Qualifier
```

come in.

We'll cover that separately.

---

# 16. How does Spring know the dependency before creating the bean?

This is why constructor injection and bean metadata matter.

Spring can inspect the bean definition and constructor/injection metadata and determine what dependencies are required.

Conceptually:

```text
OrderService
constructor:
OrderService(PaymentService)
             ↑
             |
       dependency metadata
             |
             ↓
container resolves candidate
```

This is part of why the container is more than simply:

```java
Map<String, Object>
```

It maintains definitions, dependency relationships, lifecycle information, scopes, post-processors, and more.

---

# 17. The container doesn't simply "call new"

When people say:

> "Spring creates the bean."

Don't imagine the entire process is literally:

```java
new OrderService();
```

and that's it.

There may be:

```text
Constructor selection
Dependency resolution
Property injection
Aware callbacks
BeanPostProcessor
Initialization callbacks
Proxy creation
Scope management
```

For example, a bean may eventually be wrapped with a proxy for:

```text
@Transactional
@Async
AOP
security
caching
```

`BeanPostProcessor` is one of the extension points used to customize or wrap beans during creation. Spring's own AOP infrastructure uses post-processors for proxy wrapping. ([Home][5])

This becomes extremely important later.

---

# 18. A simplified bean creation flow

Suppose:

```java
@Service
public class OrderService {
    
    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

The conceptual process is:

```text
ApplicationContext
       ↓
BeanDefinition for OrderService
       ↓
Need OrderService
       ↓
Determine constructor
       ↓
See PaymentService dependency
       ↓
Resolve PaymentService bean
       ↓
Create/inject PaymentService
       ↓
Create OrderService
       ↓
BeanPostProcessors
       ↓
Initialization
       ↓
Ready
```

That's the process I want you to visualize.

---

# 19. `BeanPostProcessor`

This is an important advanced concept.

A `BeanPostProcessor` allows Spring to perform processing around bean initialization.

It has callbacks conceptually like:

```text
Before initialization
       ↓
bean
       ↓
After initialization
```

And it can even:

```text
original bean
     ↓
wrap it
     ↓
proxy
```

Spring documents that post-processors can customize beans and may wrap them in proxies; AOP infrastructure relies on this extension point. ([Home][5])

You don't need to master implementation details yet.

Just understand:

> **Bean creation in Spring is extensible; the final object you receive may not always be the raw object originally instantiated.**

That statement will become very important when we discuss:

```text
@Transactional
@Async
AOP
Spring Security
```

---

# 20. `BeanFactoryPostProcessor` vs `BeanPostProcessor`

This is a common advanced interview question.

They sound similar but operate at different levels.

### `BeanFactoryPostProcessor`

Works on:

```text
BeanDefinitions / container metadata
```

before normal bean instantiation.

Conceptually:

```text
BeanDefinitions
      ↓
BeanFactoryPostProcessor
      ↓
modified metadata
      ↓
bean creation
```

### `BeanPostProcessor`

Works on:

```text
actual bean instances
```

during bean creation/initialization.

Conceptually:

```text
Bean instance
      ↓
BeanPostProcessor
      ↓
processed bean / proxy
```

Spring describes `BeanFactoryPostProcessor` as operating on configuration metadata, while `BeanPostProcessor` operates on bean instances. ([Home][5])

### Easy memory trick

```text
FactoryPostProcessor
       ↓
Definition / factory metadata

BeanPostProcessor
       ↓
Actual bean
```

---

# 21. ApplicationContext features

Why do we usually prefer `ApplicationContext`?

Because it provides much more than basic bean creation.

Some important capabilities:

```text
Bean management
+
Event publishing
+
Message/i18n support
+
Resource loading
+
AOP integration
+
Application-specific context functionality
```

Spring explicitly lists event publication, message resources, and easier AOP integration among `ApplicationContext` additions. ([Home][1])

---

# 22. Application events

For example:

```java
applicationContext.publishEvent(
        new OrderCreatedEvent(order)
);
```

Other beans can listen for that event.

Conceptually:

```text
OrderService
     |
     | publish event
     ↓
ApplicationContext
     |
     +---- Listener A
     |
     +---- Listener B
```

This is one reason `ApplicationContext` is more than just a bean factory.

---

# 23. Can we manually access beans?

Yes.

For example:

```java
ApplicationContext context = ...;

OrderService service =
        context.getBean(OrderService.class);
```

or:

```java
context.getBean("orderService");
```

The `ApplicationContext` provides `getBean(...)` methods for retrieving beans. ([Home][2])

But here's an important design point:

You generally **shouldn't** make your application classes repeatedly call:

```java
context.getBean(...)
```

because that starts moving toward the **Service Locator** pattern.

Spring's documentation specifically cautions that using `ApplicationContextAware` to programmatically retrieve collaborators couples the code to Spring and moves away from IoC; dependency injection is preferred. ([Home][10])

Prefer:

```java
public OrderService(PaymentService paymentService)
```

over:

```java
PaymentService paymentService =
        context.getBean(PaymentService.class);
```

---

# 24. A very important interview distinction

### Dependency Injection

```text
Spring gives you what you need.
```

### Service Locator

```text
You ask Spring for what you need.
```

Compare:

```java
public OrderService(PaymentService paymentService) {
    this.paymentService = paymentService;
}
```

versus:

```java
public OrderService(ApplicationContext context) {
    this.paymentService =
            context.getBean(PaymentService.class);
}
```

The first is proper DI.

The second is much closer to Service Locator.

---

# 25. Bean scopes — just the foundation for now

A `BeanDefinition` can specify a scope.

The most common:

```text
singleton
prototype
```

Spring also supports web-related scopes such as:

```text
request
session
application
websocket
```

depending on the context. ([Home][4])

The default scope is:

```text
singleton
```

Meaning Spring normally maintains one bean instance per bean definition within the relevant container context.

We'll spend an entire topic on this because interviewers love asking:

> "Is Spring singleton the same thing as Singleton design pattern?"

Answer: **No.**

---

# 26. Container hierarchy

Another advanced concept you'll encounter is that an application can have multiple contexts with parent-child relationships.

Conceptually:

```text
Parent ApplicationContext
          |
          ↓
Child ApplicationContext
```

A child context can generally access beans from the parent, while the parent doesn't see child beans.

This is especially relevant in some web/application architectures.

You don't need to memorize this yet, but recognize the term:

> **ApplicationContext hierarchy**

---

# 27. Spring Container vs JVM

Another common confusion:

Spring doesn't replace Java's object model.

The JVM still handles:

```text
Memory
Garbage collection
Threads
Class loading
Execution
```

Spring manages:

```text
Application-level objects
Dependencies
Bean lifecycle
Configuration
Proxies
Application infrastructure
```

So:

```text
JVM
 ↓
runs Java program

Spring
 ↓
manages application components
```

---

# 28. Spring Container vs Database

Another useful distinction.

Spring doesn't store your business data.

For example:

```text
Order
Customer
Invoice
Product
```

might be persisted by:

```text
PostgreSQL
MySQL
Oracle
```

Spring manages components such as:

```text
OrderService
OrderRepository
DataSource
TransactionManager
```

This distinction becomes useful when discussing JPA/Hibernate later.

---

# 29. EPAM interview questions you should be able to answer

### Q1. What is a Spring Bean?

> An object instantiated, assembled, and managed by the Spring IoC container.

### Q2. What is the IoC container?

> The Spring component responsible for managing bean definitions, creating beans, wiring dependencies, and managing their lifecycle.

### Q3. What is `BeanDefinition`?

> Metadata describing how a bean should be created and managed, including its class, scope, dependencies, lifecycle configuration, and other settings.

### Q4. `BeanFactory` vs `ApplicationContext`?

> `ApplicationContext` extends `BeanFactory` and provides additional application-oriented capabilities such as events, resource/message handling, and easier AOP integration. It is generally preferred for normal applications.

### Q5. Is `ApplicationContext` a container?

> Yes. It represents the higher-level Spring IoC container.

### Q6. How does Spring know what beans to create?

> Through configuration metadata such as component scanning, `@Configuration`/`@Bean` methods, and other configuration sources.

### Q7. Does Spring create every object?

> No. Only objects registered and managed as Spring beans are managed by the container.

### Q8. What is `BeanPostProcessor`?

> An extension point that allows Spring to process bean instances before and after initialization and potentially wrap them, for example with proxies.

### Q9. `BeanFactoryPostProcessor` vs `BeanPostProcessor`?

> The former operates on bean-definition/container metadata; the latter operates on bean instances.

### Q10. Why is `ApplicationContext` generally preferred?

> Because it provides the complete `BeanFactory` functionality plus additional application-level features and integrates more fully with Spring infrastructure.

---

# 30. The interviewer can take this much deeper

Suppose the interviewer asks:

> "Explain how Spring creates a bean."

Don't answer:

> "Spring sees `@Service` and creates it."

That's too shallow.

A better answer:

> Spring first obtains configuration metadata and registers bean definitions with the container. When a bean needs to be created, the container determines how to instantiate it, resolves its dependencies, creates and injects those dependencies, applies relevant post-processors, performs initialization callbacks, and then exposes the resulting bean through the container.

That answer demonstrates actual understanding.

---

# 31. The full mental model

Keep this diagram in your head:

```text
              Configuration Metadata
                      |
          ┌───────────┼───────────┐
          ↓           ↓           ↓
     @Component     @Bean         XML/etc.
          |           |           |
          └───────────┼───────────┘
                      ↓
               BeanDefinitions
                      |
                      ↓
              Spring IoC Container
                      |
          ┌───────────┼────────────┐
          ↓           ↓            ↓
      instantiate   resolve      configure
                     deps
          └───────────┼────────────┘
                      ↓
               BeanPostProcessor
                      ↓
                  initialize
                      ↓
                 managed bean
```

And:

```text
ApplicationContext
        |
        ↓
   BeanFactory
        |
        ↓
core bean management
        +
events
resources
AOP integration
etc.
```

---

# 32. One subtle point worth remembering

A lot of developers say:

> "Spring stores singleton beans in a map."

That's a useful simplification, but it's not the whole story.

Internally Spring has sophisticated bean registries, definitions, singleton management, dependency resolution, post-processors, scopes, lifecycle handling, circular-reference handling, proxy infrastructure, and more.

For interviews:

```text
"Spring has a container that manages beans"
```

is correct.

But when asked for internals, you should talk about:

```text
BeanDefinition
BeanFactory
ApplicationContext
BeanPostProcessor
BeanFactoryPostProcessor
dependency resolution
lifecycle
scopes
```

That shows depth.

---

# 33. What you should know before moving on

You should now be comfortable with this chain:

```text
Spring Application
       ↓
ApplicationContext
       ↓
BeanFactory infrastructure
       ↓
BeanDefinitions
       ↓
Bean creation
       ↓
Dependency resolution
       ↓
Dependency injection
       ↓
BeanPostProcessor
       ↓
Initialization
       ↓
Managed Bean
```

And the key distinctions:

```text
Object ≠ necessarily Spring Bean

BeanDefinition ≠ Bean

BeanFactory ≠ ApplicationContext

DI ≠ @Autowired

BeanPostProcessor ≠ BeanFactoryPostProcessor
```

Those five distinctions alone eliminate a lot of common Spring interview confusion.

---

# Next: Bean Lifecycle

The next topic is one of the **highest-value Spring interview topics**:

# Topic 4 — Spring Bean Lifecycle

We'll trace a bean from:

```text
BeanDefinition
     ↓
Instantiation
     ↓
Dependency Injection
     ↓
Aware callbacks
     ↓
@PostConstruct
     ↓
BeanPostProcessor
     ↓
Initialization
     ↓
Ready
     ↓
@PreDestroy
     ↓
Destruction
```

We'll also answer the classic interview question:

> **"In what order do `@PostConstruct`, `InitializingBean`, custom init methods, and `BeanPostProcessor` callbacks execute?"**

That's where Spring knowledge starts becoming genuinely deep. ([Home][5])

[1]: https://docs.spring.io/spring-framework/reference/core/beans/introduction.html?utm_source=chatgpt.com "Introduction to the Spring IoC Container and Beans :: Spring Framework"
[2]: https://docs.spring.io/spring-framework/reference/6.2/core/beans/basics.html?utm_source=chatgpt.com "Container Overview :: Spring Framework"
[3]: https://docs.spring.io/spring/reference/7.1-SNAPSHOT/core/beans/definition.html?utm_source=chatgpt.com "Bean Overview :: Spring Framework"
[4]: https://docs.spring.io/spring-framework/reference/core/beans/factory-scopes.html?utm_source=chatgpt.com "Bean Scopes :: Spring Framework"
[5]: https://docs.spring.io/spring-framework/reference/core/beans/factory-extension.html?utm_source=chatgpt.com "Container Extension Points :: Spring Framework"
[6]: https://docs.spring.io/spring-framework/reference/7.1/core/beans/beanfactory.html?utm_source=chatgpt.com "The BeanFactory API :: Spring Framework"
[7]: https://docs.spring.io/spring-framework/reference/7.1/core/beans/dependencies/factory-lazy-init.html?utm_source=chatgpt.com "Lazy-initialized Beans :: Spring Framework"
[8]: https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html?utm_source=chatgpt.com "Classpath Scanning and Managed Components :: Spring Framework"
[9]: https://docs.spring.io/spring-framework/reference/core/beans/java/bean-annotation.html?utm_source=chatgpt.com "Using the @Bean Annotation :: Spring Framework"
[10]: https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html?utm_source=chatgpt.com "Customizing the Nature of a Bean :: Spring Framework"

