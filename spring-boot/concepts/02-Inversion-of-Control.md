# Topic 2 — IoC and Dependency Injection

This is probably the **single most important Spring concept** to understand properly.

If you understand IoC and DI deeply, a lot of Spring starts making sense:

```text
IoC / DI
   ↓
Beans
   ↓
ApplicationContext
   ↓
@Autowired / Constructor Injection
   ↓
@Service / @Repository / @Controller
   ↓
AOP / @Transactional
   ↓
Spring Boot
```

A common mistake is memorizing:

> “IoC means Spring creates objects.”

That's only the surface.

We need to understand **what control is being inverted, why that matters, and how dependency injection actually solves coupling**.

---

# 1. First: What is a dependency?

Suppose we have:

```java
public class OrderService {

    private PaymentService paymentService;

}
```

`OrderService` needs `PaymentService` to do its work.

So:

```text
OrderService
     ↓
PaymentService
```

We say:

> `OrderService` has a dependency on `PaymentService`.

For example:

```java
public class OrderService {

    private PaymentService paymentService;

    public void placeOrder() {
        paymentService.processPayment();
    }
}
```

Without `PaymentService`, `OrderService` cannot perform its job.

That's a dependency.

---

# 2. The traditional approach

Let's create an implementation:

```java
public class StripePaymentService implements PaymentService {

    @Override
    public void processPayment() {
        System.out.println("Processing payment using Stripe");
    }
}
```

Now:

```java
public class OrderService {

    private PaymentService paymentService;

    public OrderService() {
        this.paymentService = new StripePaymentService();
    }

    public void placeOrder() {
        paymentService.processPayment();
    }
}
```

Looks fine.

But there's a problem.

Look carefully at:

```java
this.paymentService = new StripePaymentService();
```

`OrderService` is responsible for **creating its own dependency**.

So `OrderService` knows:

```text
I need PaymentService
+
I will decide which implementation
+
I will create it
```

That creates tight coupling.

---

# 3. Why is this bad?

Suppose tomorrow we want:

```text
StripePaymentService
        ↓
RazorpayPaymentService
```

or perhaps:

```text
Stripe
Razorpay
PayPal
MockPayment
TestPayment
```

Our service currently says:

```java
new StripePaymentService();
```

So the business class is now coupled to a specific implementation.

We may have to do:

```java
public class OrderService {

    private PaymentService paymentService;

    public OrderService() {
        this.paymentService = new RazorpayPaymentService();
    }
}
```

Every time the implementation changes, we modify `OrderService`.

That's the problem DI is designed to address.

---

# 4. Dependency Inversion starts appearing here

Instead of:

```text
OrderService
     ↓
StripePaymentService
```

we want:

```text
OrderService
     ↓
PaymentService interface
     ↑
     |
StripePaymentService
```

So:

```java
public interface PaymentService {

    void processPayment();
}
```

and:

```java
public class StripePaymentService implements PaymentService {

    @Override
    public void processPayment() {
        System.out.println("Stripe payment");
    }
}
```

Now `OrderService` should depend on the abstraction:

```java
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void placeOrder() {
        paymentService.processPayment();
    }
}
```

Notice what's changed.

`OrderService` no longer says:

```java
new StripePaymentService()
```

Instead, somebody gives it a `PaymentService`.

That "somebody" is the important part.

---

# 5. Dependency Injection

We can manually inject the dependency:

```java
PaymentService paymentService = new StripePaymentService();

OrderService orderService =
        new OrderService(paymentService);
```

Here:

```text
Caller
  |
  | creates dependency
  ↓
StripePaymentService
  |
  | injects
  ↓
OrderService
```

This is **Dependency Injection**.

The dependency is supplied from outside rather than the class constructing it itself.

That's the key idea.

---

# 6. Definition of Dependency Injection

A strong interview definition:

> **Dependency Injection is a design technique in which an object's dependencies are provided to it from outside rather than the object creating those dependencies itself.**

Notice something important:

**Dependency Injection is not inherently a Spring concept.**

You can perform DI manually in plain Java.

For example:

```java
PaymentService paymentService = new StripePaymentService();

OrderService orderService =
        new OrderService(paymentService);
```

No Spring involved.

Spring simply **automates and manages DI** for you.

---

# 7. So what is IoC?

Now we reach the bigger concept.

Traditionally:

```text
Your code
   ↓
creates objects
   ↓
controls dependencies
   ↓
controls lifecycle
```

With Spring:

```text
Spring Container
       ↓
creates objects
       ↓
resolves dependencies
       ↓
injects dependencies
       ↓
manages lifecycle
```

The control over object creation and wiring has moved from your application code to the framework.

That is:

# Inversion of Control

Instead of:

```text
Application controls object creation
```

we have:

```text
Framework controls object creation
```

The **control has been inverted**.

---

# 8. IoC vs DI

This distinction is very frequently asked.

They are related, but not identical.

### IoC

IoC is the **broader principle**.

> Control over object creation, configuration, and lifecycle is transferred from application code to another component/framework.

### DI

DI is one **way of implementing IoC**.

> Dependencies are supplied externally to an object.

So:

```text
IoC
 |
 +---- Dependency Injection
 |
 +---- other mechanisms / patterns
```

In Spring, IoC is primarily achieved through its **IoC container and dependency injection mechanism**.

---

# 9. A simple analogy

Imagine a restaurant.

Without DI:

```text
Chef
 |
 +---- goes to market
 |
 +---- buys vegetables
 |
 +---- buys meat
 |
 +---- prepares ingredients
```

The chef is responsible for everything.

With DI:

```text
Supplier
 |
 +---- provides vegetables
 +---- provides meat
        ↓
       Chef
```

The chef says:

> “I need these ingredients.”

The chef doesn't need to know:

> “Where were they purchased?”

That responsibility belongs elsewhere.

Similarly:

```text
OrderService
     |
     | needs
     ↓
PaymentService
```

It shouldn't necessarily care:

```text
Which implementation?
How created?
How configured?
What dependencies does it itself need?
```

Spring handles that.

---

# 10. Without Spring vs with Spring

## Without Spring

```java
PaymentService paymentService =
        new StripePaymentService();

OrderService orderService =
        new OrderService(paymentService);
```

You manually construct the object graph.

---

## With Spring

```java
@Service
public class StripePaymentService
        implements PaymentService {

    @Override
    public void processPayment() {
        System.out.println("Stripe payment");
    }
}
```

And:

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

Spring sees:

```text
StripePaymentService
        ↓
implements PaymentService

OrderService
        ↓
needs PaymentService
```

The Spring container can resolve the dependency and inject it.

Conceptually:

```text
Spring Container
      |
      +---- StripePaymentService
      |
      +---- OrderService
                  |
                  +---- PaymentService
                           ↓
                    StripePaymentService
```

This is the essence of Spring DI.

---

# 11. What is the Spring IoC Container?

This is your next important term.

The **Spring IoC Container** is responsible for managing Spring beans and their dependencies.

At a high level:

```text
Spring Container
       |
       +---- create beans
       +---- configure beans
       +---- inject dependencies
       +---- manage lifecycle
       +---- manage scopes
```

The two major container interfaces you'll hear about are:

```text
BeanFactory
ApplicationContext
```

We'll cover these separately in depth.

For now, remember:

> The IoC container is the component responsible for managing Spring-managed objects (beans).

---

# 12. What exactly does the container manage?

Suppose:

```java
@Service
public class OrderService {
}
```

Spring may create an instance:

```text
OrderService@5f2a3
```

and register it as a bean.

Then another bean can depend on it:

```java
@Service
public class CheckoutService {

    private final OrderService orderService;

    public CheckoutService(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

Spring can construct:

```text
OrderService
      ↓
CheckoutService
```

The application doesn't need to manually write:

```java
new OrderService()
new CheckoutService(orderService)
```

---

# 13. The Object Graph

This is an excellent mental model for Spring.

Suppose:

```java
@Service
class OrderService {
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
}
```

and:

```java
@Service
class PaymentService {
    private final PaymentGateway gateway;
}
```

Then Spring builds something conceptually like:

```text
                 Spring Container
                       |
        +--------------+--------------+
        |              |              |
        ↓              ↓              ↓
 OrderService    InventoryService   PaymentService
      |                              |
      |                              ↓
      |                         PaymentGateway
      |
      +---- PaymentService
```

This entire network is the **object graph**.

One of Spring's major jobs is constructing and wiring that graph.

This is a very useful way to think about the framework.

---

# 14. Why does DI reduce coupling?

Compare these two.

### Tight coupling

```java
public class OrderService {

    private StripePaymentService paymentService =
            new StripePaymentService();
}
```

`OrderService` directly depends on:

```text
StripePaymentService
```

---

### Looser coupling

```java
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

Now it depends on:

```text
PaymentService
```

and can accept:

```text
StripePaymentService
RazorpayPaymentService
MockPaymentService
TestPaymentService
```

provided they implement the interface.

That's one of the biggest practical benefits of DI.

---

# 15. DI also makes testing easier

Imagine:

```java
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

For testing:

```java
PaymentService mockPaymentService =
        mock(PaymentService.class);

OrderService orderService =
        new OrderService(mockPaymentService);
```

No real payment gateway required.

Without DI:

```java
public class OrderService {

    private final PaymentService paymentService =
            new StripePaymentService();
}
```

Replacing that dependency becomes much harder.

So DI improves:

```text
Loose coupling
+
Testability
+
Maintainability
+
Flexibility
```

---

# 16. Three types of Dependency Injection

Spring supports three common injection styles.

## 1. Constructor Injection

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

This is generally the preferred approach.

---

## 2. Setter Injection

```java
@Service
public class OrderService {

    private PaymentService paymentService;

    @Autowired
    public void setPaymentService(
            PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

Dependency is supplied through a setter.

Useful when a dependency is optional or needs to be changeable after construction.

---

## 3. Field Injection

```java
@Service
public class OrderService {

    @Autowired
    private PaymentService paymentService;
}
```

This is easy to write, but generally less desirable.

We'll discuss exactly **why field injection is discouraged** later.

---

# 17. Constructor Injection — why is it preferred?

Consider:

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

Advantages:

### Dependency is explicit

You can immediately see:

```text
OrderService
    requires
PaymentService
```

---

### Object can be immutable

```java
private final PaymentService paymentService;
```

Once constructed, the reference doesn't have to change.

---

### Easier unit testing

```java
new OrderService(mockPaymentService);
```

---

### Invalid object is harder to create

You can't easily create:

```java
new OrderService();
```

without supplying the required dependency.

---

### No reflection required for the field

The dependency is supplied through normal Java construction.

---

# 18. Important modern Spring detail

Suppose there is only **one constructor**:

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

You generally don't need:

```java
@Autowired
```

on that constructor.

Spring can use the single constructor.

This is something interviewers sometimes ask.

---

# 19. What if there are multiple constructors?

Suppose:

```java
@Service
public class OrderService {

    public OrderService() {
    }

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

Now Spring may need help determining which constructor should be used for injection.

That's where constructor-level:

```java
@Autowired
```

can be relevant.

---

# 20. A deeper question: Who actually creates the object?

Suppose:

```java
@Service
public class OrderService {
}
```

You might say:

> Spring creates the object.

That's broadly correct.

But the deeper answer is:

```text
ApplicationContext
       ↓
uses BeanDefinition metadata
       ↓
determines how bean should be created
       ↓
instantiates bean
       ↓
resolves dependencies
       ↓
injects dependencies
       ↓
applies post-processing/lifecycle steps
       ↓
stores/manages bean
```

We'll study this entire lifecycle later.

For now, the critical distinction is:

```text
new OrderService()
```

means **you control object creation**.

Whereas a Spring-managed bean means:

```text
Spring container
      ↓
controls creation/wiring/lifecycle
```

That's the essence of IoC.

---

# 21. Is Dependency Injection the same as `@Autowired`?

No.

This is a common interview trap.

Wrong:

> Dependency Injection is `@Autowired`.

Correct:

> `@Autowired` is one mechanism Spring can use to perform dependency injection. Dependency Injection itself is a broader design principle.

You can have DI without Spring:

```java
PaymentService paymentService =
        new StripePaymentService();

OrderService service =
        new OrderService(paymentService);
```

No annotation.

No Spring.

Still DI.

---

# 22. Is Spring required for IoC?

No.

IoC is a general design principle.

Dependency injection containers are one way to implement it.

Spring provides a sophisticated IoC container.

Other frameworks/containers can also implement dependency injection.

So:

```text
IoC ≠ Spring-specific
DI ≠ @Autowired
Spring = framework that provides a powerful IoC container
```

---

# 23. IoC is bigger than dependency injection

Think about all the responsibilities that can move to the framework:

```text
Application code
     |
     | previously controls
     ↓
Object creation
Dependency wiring
Lifecycle
Configuration
Transactions
Security integration
AOP
```

With Spring:

```text
Spring
 |
 +---- creates beans
 +---- injects dependencies
 +---- manages lifecycle
 +---- applies proxies
 +---- handles configuration
```

That's why "IoC" is broader than simply "injecting dependencies."

---

# 24. Real-world example

Imagine an e-commerce system:

```text
OrderController
        ↓
OrderService
        ↓
PaymentService
        ↓
PaymentGateway
```

You don't want:

```java
public class OrderService {

    private PaymentService paymentService =
            new StripePaymentService();

    private InventoryService inventoryService =
            new MySqlInventoryService();

}
```

because now business logic knows implementation details.

Instead:

```java
public class OrderService {

    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    public OrderService(
            PaymentService paymentService,
            InventoryService inventoryService) {

        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }
}
```

Spring becomes responsible for wiring the application:

```text
StripePaymentService
MySqlInventoryService
        ↓
       Spring
        ↓
OrderService
```

The business class focuses on business logic.

---

# 25. Very important: DI does not magically eliminate coupling

This is a subtle interview point.

Consider:

```java
public OrderService(StripePaymentService paymentService)
```

You're using constructor injection, but you still depend directly on:

```text
StripePaymentService
```

So DI alone doesn't guarantee good architecture.

Better:

```java
public OrderService(PaymentService paymentService)
```

Now we're depending on an abstraction.

So the combination is:

```text
Abstraction
+
Dependency Injection
        ↓
Reduced coupling
```

This connects directly with the **Dependency Inversion Principle** from SOLID.

---

# 26. IoC + DI + DIP

These three are easy to confuse.

### IoC

A broad principle:

> Control is transferred to another component/framework.

### DI

A mechanism/design technique:

> Dependencies are provided from outside.

### DIP

A SOLID principle:

> High-level modules should not depend directly on low-level modules; both should depend on abstractions.

They work nicely together:

```text
DIP
 ↓
Depend on abstraction

DI
 ↓
Inject implementation

IoC
 ↓
Container controls the wiring
```

---

# 27. EPAM-style interview question

### Question:

**What is IoC in Spring?**

A strong answer:

> IoC, or Inversion of Control, means that the responsibility for creating, configuring, wiring, and managing application objects is transferred from application code to the Spring container. Instead of classes creating their own dependencies using `new`, Spring manages the objects as beans and injects their dependencies.

That's much stronger than:

> "IoC means Spring creates objects."

---

# 28. EPAM-style question

### Question:

**What is Dependency Injection?**

Good answer:

> Dependency Injection is a design technique where an object's dependencies are provided from outside instead of being constructed by the object itself. In Spring, the IoC container resolves and injects those dependencies into Spring-managed beans.

---

# 29. EPAM-style question

### Question:

**Difference between IoC and DI?**

Good answer:

> IoC is the broader principle of transferring control of object creation and management away from application code. Dependency Injection is one of the primary techniques used to implement that inversion of control.

---

# 30. EPAM-style question

### Question:

**Why do we use Dependency Injection?**

Mention the real benefits:

```text
Loose coupling
Testability
Maintainability
Flexibility
Easier replacement of implementations
Centralized object configuration
```

A good answer:

> DI reduces the responsibility of classes for constructing their dependencies. This reduces coupling, makes implementations easier to replace, improves unit testing, and separates business logic from object creation and configuration.

---

# 31. EPAM follow-up: "Can you give an example?"

Use this:

```java
public interface PaymentService {
    void pay();
}
```

```java
@Service
public class StripePaymentService
        implements PaymentService {

    @Override
    public void pay() {
        System.out.println("Stripe");
    }
}
```

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void placeOrder() {
        paymentService.pay();
    }
}
```

Then explain:

> `OrderService` doesn't instantiate `StripePaymentService`. It only declares that it needs a `PaymentService`. Spring resolves the implementation and injects it.

That's a solid practical explanation.

---

# 32. A tricky question

### Interviewer:

> Can we achieve dependency injection without Spring?

Answer:

> Yes. Dependency Injection is a general design technique and doesn't require Spring. We can manually create dependencies and pass them through constructors or setters. Spring automates and manages that process using its IoC container.

Example:

```java
PaymentService paymentService =
        new StripePaymentService();

OrderService orderService =
        new OrderService(paymentService);
```

This is DI.

---

# 33. Another tricky question

### Interviewer:

> Why is `new` considered a problem in Spring applications?

Be careful.

`new` itself is **not bad**.

The problem is when business classes use `new` to construct their dependencies.

For example:

```java
public class OrderService {

    private PaymentService paymentService =
            new StripePaymentService();
}
```

Now the class controls:

```text
dependency selection
+
dependency creation
```

This makes substitution and testing harder.

But using `new` for simple local objects is completely normal Java.

For example:

```java
Order order = new Order();
```

There is no requirement that every object in a Spring application must be a Spring bean.

That's an important distinction.

---

# 34. Another common question

### Interviewer:

> Does Spring manage every object in your application?

No.

Only objects that are registered as Spring beans are managed by the Spring container.

For example:

```java
@Service
public class OrderService {
}
```

is managed.

But:

```java
public class Order {
}
```

doesn't automatically become a Spring bean simply because the class exists.

You can still do:

```java
Order order = new Order();
```

and it remains an ordinary Java object.

---

# 35. Another tricky scenario

Suppose you do:

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

But there are two implementations:

```java
@Service
public class StripePaymentService
        implements PaymentService {
}
```

and:

```java
@Service
public class RazorpayPaymentService
        implements PaymentService {
}
```

Spring now sees:

```text
PaymentService
      ↑
  ┌───┴────┐
  |        |
Stripe   Razorpay
```

Which one should be injected?

This leads to:

```text
@Primary
@Qualifier
```

We'll cover these in detail later.

This is exactly how interviewers often take a basic DI question and turn it into a 10-minute discussion.

---

# 36. The biggest mental model to remember

Don't think:

```text
@Autowired
    ↓
magic happens
```

Think:

```text
                         Spring Container
                                |
                   ┌────────────┴────────────┐
                   ↓                         ↓
             create beans              resolve dependencies
                   |                         |
                   └────────────┬────────────┘
                                ↓
                           inject them
                                ↓
                         managed object graph
```

And application classes become:

```text
"I declare what I need."
```

rather than:

```text
"I create everything I need."
```

That's the fundamental shift.

---

# 37. A complete example

Let's put everything together.

### Interface

```java
public interface NotificationService {

    void send(String message);
}
```

### Implementation

```java
@Service
public class EmailNotificationService
        implements NotificationService {

    @Override
    public void send(String message) {
        System.out.println("Sending email: " + message);
    }
}
```

### Consumer

```java
@Service
public class OrderService {

    private final NotificationService notificationService;

    public OrderService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void createOrder() {

        // business logic

        notificationService.send("Order created");
    }
}
```

### What happens conceptually?

```text
Spring starts
    ↓
scans components
    ↓
finds EmailNotificationService
    ↓
registers it as a bean
    ↓
finds OrderService
    ↓
sees constructor needs NotificationService
    ↓
finds EmailNotificationService
    ↓
injects it
    ↓
OrderService becomes ready
```

So the final object graph is:

```text
OrderService
      |
      | NotificationService
      ↓
EmailNotificationService
```

---

# 38. Interview revision sheet

At the end of this topic, you should be able to answer these immediately:

**What is IoC?**

> Transfer of control over object creation/configuration/lifecycle from application code to the Spring container.

**What is DI?**

> Supplying an object's dependencies from outside rather than constructing them internally.

**Is DI specific to Spring?**

> No.

**How does Spring implement IoC?**

> Primarily through its IoC container and dependency injection.

**What is the benefit of DI?**

> Loose coupling, testability, maintainability, and easier replacement of implementations.

**What are the types of DI?**

> Constructor, setter, and field injection.

**Which injection style is generally preferred?**

> Constructor injection.

**Does `@Autowired` mean DI?**

> No. `@Autowired` is one Spring mechanism for performing injection.

**Does every Java object become a Spring bean?**

> No. Only Spring-managed/registered beans are managed by the container.

**Can DI exist without Spring?**

> Yes.

---

# 39. The chain you should now understand

This entire sequence is fundamental:

```text
Class has a dependency
        ↓
Don't construct it internally
        ↓
Provide dependency externally
        ↓
Dependency Injection
        ↓
Spring container manages the process
        ↓
Inversion of Control
        ↓
Spring manages the object graph
        ↓
Beans
```

Once this becomes intuitive, annotations such as:

```java
@Service
@Repository
@Controller
@Autowired
@Bean
@Configuration
@Component
```

stop looking like unrelated magic.

They become different ways of telling/configuring the Spring container **what objects exist and how they should be wired**.

---

## One level deeper: what we're going to tackle next

The natural next question is:

> **"Okay, Spring manages these objects. What exactly is a Spring Bean, and how does the Spring Container create and manage it?"**

That leads to:

# Topic 3 — Spring Container: `BeanFactory` vs `ApplicationContext` + how Spring manages Beans

That topic is important enough to deserve a deep treatment, including:

```text
BeanDefinition
      ↓
Container
      ↓
Instantiation
      ↓
Dependency Injection
      ↓
BeanPostProcessor
      ↓
Initialization
      ↓
Ready bean
```

and the **`BeanFactory` vs `ApplicationContext` interview question**, which is a frequent follow-up.

