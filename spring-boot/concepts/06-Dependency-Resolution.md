# Topic 6 — Dependency Injection in Spring: `@Autowired`, Constructor/Setter/Field Injection, `@Primary`, `@Qualifier`, and Dependency Resolution

This is the topic where the basic idea of DI becomes **actual Spring behavior**.

So far:

```text
IoC
 ↓
Spring Container
 ↓
Beans
 ↓
Dependency Injection
```

Now we answer:

> **How exactly does Spring decide what to inject?**

Suppose:

```java
public interface PaymentService {
    void pay();
}
```

and we have:

```java
@Service
public class StripePaymentService implements PaymentService {
}
```

Then:

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

This is easy when there is exactly one `PaymentService`.

But real applications often have:

```text
PaymentService
    ├── StripePaymentService
    ├── RazorpayPaymentService
    └── PaypalPaymentService
```

Now Spring has to choose.

That's where this entire topic becomes important.

---

# 1. What is `@Autowired`?

`@Autowired` tells Spring that a constructor, field, or method/parameter represents a dependency injection point. Spring then tries to resolve a matching bean from the container. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

For example:

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    @Autowired
    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

Conceptually:

```text
OrderService needs PaymentService
             ↓
Spring searches its beans
             ↓
find matching candidate
             ↓
inject it
```

But remember:

> **`@Autowired` is not Dependency Injection itself.**

It is a Spring mechanism for expressing an injection point.

---

# 2. Constructor Injection

The preferred style for required dependencies is constructor injection.

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
}
```

If this class has only one constructor, `@Autowired` is not required. Spring will use that constructor automatically. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

So both are valid:

```java
@Autowired
public OrderService(PaymentService paymentService) {
    this.paymentService = paymentService;
}
```

and:

```java
public OrderService(PaymentService paymentService) {
    this.paymentService = paymentService;
}
```

when it's the only constructor.

---

# 3. Why Constructor Injection Is Usually Preferred

This is not just stylistic preference.

## Dependency is explicit

Look at:

```java
public OrderService(PaymentService paymentService)
```

You immediately know:

```text
OrderService
    requires
PaymentService
```

---

## Object can be immutable

```java
private final PaymentService paymentService;
```

After construction, the dependency doesn't need to be reassigned.

---

## Easier unit testing

You can simply do:

```java
PaymentService mockPaymentService = mock(PaymentService.class);

OrderService service =
        new OrderService(mockPaymentService);
```

No Spring container is required.

---

## Required dependencies cannot be accidentally forgotten

With constructor injection:

```java
new OrderService();
```

won't compile.

That's a useful guarantee.

With field injection:

```java
new OrderService();
```

can compile even though the dependency hasn't been injected.

---

# 4. Field Injection

Example:

```java
@Service
public class OrderService {

    @Autowired
    private PaymentService paymentService;
}
```

Spring supports this. `@Autowired` can be applied to fields. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

But just because Spring supports it doesn't mean it is the preferred design.

Problems include:

```text
dependencies hidden in class body
↓
object can exist before required dependencies are available
↓
harder plain-Java unit testing
↓
mutable field
↓
tighter coupling to injection framework
```

Compare:

```java
public OrderService(PaymentService paymentService) {
    this.paymentService = paymentService;
}
```

with:

```java
@Autowired
private PaymentService paymentService;
```

The constructor version communicates the dependency much more clearly.

---

# 5. Setter Injection

Spring also supports setter/method injection:

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

Spring allows `@Autowired` on setter methods and even arbitrary methods with multiple arguments. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

This can be useful for dependencies that are genuinely optional or replaceable.

For a mandatory dependency, constructor injection is usually clearer.

---

# 6. Constructor vs Setter vs Field

|                                          | Constructor | Setter    | Field                   |
| ---------------------------------------- | ----------- | --------- | ----------------------- |
| Dependency visible?                      | Yes         | Yes       | Less obvious            |
| Supports `final`                         | Yes         | No        | No                      |
| Easy unit testing                        | Excellent   | Good      | Poorer                  |
| Object can be created without dependency | No          | Yes       | Yes                     |
| Best for required dependency             | Usually     | Sometimes | Generally not preferred |
| Spring support                           | Yes         | Yes       | Yes                     |

A useful rule:

```text
Required dependency
    → constructor

Optional/configurable dependency
    → setter can make sense

Field injection
    → supported, but usually avoid in application design
```

---

# 7. What Does Spring Match First?

This is the heart of dependency resolution.

Suppose:

```java
public interface PaymentService {
}
```

and only:

```java
@Service
public class StripePaymentService
        implements PaymentService {
}
```

Spring can resolve it by type:

```text
PaymentService
     ↓
StripePaymentService
```

Spring's autowiring is fundamentally **type-driven**. Qualifiers and other resolution rules are then used to narrow candidates. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

---

# 8. One Bean — Easy Case

```java
@Service
public class StripePaymentService
        implements PaymentService {
}
```

and:

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
Required type:
PaymentService

Candidates:
StripePaymentService
```

One candidate.

Therefore:

```text
inject StripePaymentService
```

---

# 9. Multiple Beans — The Problem

Now:

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

Now:

```text
Required:
PaymentService

Candidates:
StripePaymentService
RazorpayPaymentService
```

Which one?

Spring cannot simply guess arbitrarily.

For a single-valued dependency, ambiguity must be resolved.

This is where we use:

```text
@Primary
@Qualifier
```

and, in modern Spring, potentially `@Fallback` as well. ([docs.spring.io](https://docs.spring.io/spring/reference/core/beans/annotation-config/autowired-primary.html?utm_source=chatgpt.com))

---

# 10. `@Primary`

Suppose:

```java
@Service
@Primary
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

Now the candidates are:

```text
StripePaymentService   ← @Primary
RazorpayPaymentService
```

If one primary candidate exists, Spring prefers it for a single-valued injection point. ([docs.spring.io](https://docs.spring.io/spring/reference/core/beans/annotation-config/autowired-primary.html?utm_source=chatgpt.com))

So:

```java
public OrderService(PaymentService paymentService)
```

gets:

```text
StripePaymentService
```

---

# 11. What Does `@Primary` Really Mean?

This is important.

`@Primary` means:

> **"Prefer this bean when multiple candidates exist for a single-valued injection point."**

It does **not** mean:

> "This is the only bean."

The other beans still exist.

For example:

```text
PaymentService
   |
   ├── Stripe   @Primary
   └── Razorpay
```

Both are valid Spring beans.

Only the default selection has changed.

---

# 12. What if Two Beans Are `@Primary`?

Suppose:

```java
@Service
@Primary
public class StripePaymentService
        implements PaymentService {
}
```

and:

```java
@Service
@Primary
public class RazorpayPaymentService
        implements PaymentService {
}
```

Now there are:

```text
2 candidates
2 primary candidates
```

`@Primary` doesn't resolve the ambiguity because there isn't exactly one primary candidate.

You need another mechanism, such as `@Qualifier` or a more explicit design.

---

# 13. `@Qualifier`

Suppose we want to choose explicitly.

```java
@Service("stripe")
public class StripePaymentService
        implements PaymentService {
}
```

```java
@Service("razorpay")
public class RazorpayPaymentService
        implements PaymentService {
}
```

Then:

```java
@Service
public class OrderService {

    private final PaymentService paymentService;

    public OrderService(
            @Qualifier("stripe")
            PaymentService paymentService) {

        this.paymentService = paymentService;
    }
}
```

Now Spring knows:

```text
PaymentService
      ↓
@Qualifier("stripe")
      ↓
StripePaymentService
```

Spring's documentation describes `@Qualifier` as a way to narrow the candidate set among beans matching the required type. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

---

# 14. `@Primary` vs `@Qualifier`

This is one of the most common interview questions.

### `@Primary`

Means:

> "Use this as the preferred/default candidate."

Example:

```java
@Primary
@Service
class StripePaymentService
```

Good when:

```text
Stripe is the normal implementation
Razorpay is an alternative
```

---

### `@Qualifier`

Means:

> "For this particular injection point, choose this matching candidate."

Example:

```java
public OrderService(
    @Qualifier("razorpay")
    PaymentService paymentService
)
```

Good when:

```text
different parts of the application
need different implementations
```

---

# 15. Real-world example of `@Primary` + `@Qualifier`

Suppose:

```text
PaymentService
   |
   ├── Stripe    @Primary
   ├── Razorpay
   └── PayPal
```

Most services:

```java
public CheckoutService(PaymentService paymentService)
```

get:

```text
Stripe
```

But one special service:

```java
public InternationalCheckoutService(
    @Qualifier("paypal")
    PaymentService paymentService)
```

gets:

```text
PayPal
```

This is a very realistic use case.

---

# 16. Important: `@Qualifier` Is Not Simply "Bean Name Lookup"

This is subtle and interview-worthy.

People often say:

> "`@Qualifier` means inject a bean by name."

That's an oversimplification.

Spring's documentation describes `@Autowired` as **type-driven injection with qualifiers used to narrow the type-selected candidates**. A qualifier value can coincide with a bean name, but it is not semantically identical to explicit bean-name lookup. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

So:

```java
@Qualifier("stripe")
PaymentService paymentService
```

roughly means:

```text
Find PaymentService candidates
      ↓
narrow to qualifier "stripe"
```

rather than conceptually:

```text
ignore type
↓
go fetch bean whose ID is "stripe"
```

That's an important distinction.

---

# 17. `@Qualifier` Can Be Put on Constructor Parameters

You don't need field injection.

This is preferred:

```java
public OrderService(
        @Qualifier("stripe")
        PaymentService paymentService) {

    this.paymentService = paymentService;
}
```

Spring explicitly supports qualifier annotations on constructor arguments and method parameters. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

This lets you have:

```text
constructor injection
+
explicit implementation selection
```

together.

---

# 18. Bean Name as a Fallback

There is an interesting current Spring behavior.

Suppose:

```java
@Service
public class StripePaymentService {
}
```

The default bean name is commonly:

```text
stripePaymentService
```

And:

```java
public OrderService(
        PaymentService stripePaymentService)
```

Under current Spring's dependency-resolution rules, when there is no other resolution indicator and multiple candidates remain, Spring may match the **injection-point name** with the bean name. For this parameter-name matching behavior, Spring 6.1+ requires Java parameter metadata (`-parameters`). ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

So in modern Spring:

```text
parameter name
      ↓
stripePaymentService
      ↓
matching bean name
```

can help resolve ambiguity.

But do **not** rely on this as your primary design technique.

For explicit intent, prefer:

```java
@Qualifier("stripe")
```

when appropriate.

---

# 19. Why Java Parameter Names Matter Here

Consider:

```java
public OrderService(
        PaymentService stripePaymentService) {
}
```

For Spring to use the parameter name during candidate resolution, the compiled class must retain method parameter metadata.

That commonly means compiling with:

```text
-parameters
```

Spring's current docs explicitly call this requirement out for this matching path. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

This is an excellent **advanced interview detail**.

---

# 20. `@Primary` vs `@Qualifier` — Interview Answer

> `@Primary` defines a preferred candidate when several beans match a single-valued dependency. `@Qualifier` provides more specific selection at the injection point by narrowing candidates using qualifier metadata. I would use `@Primary` when one implementation should be the normal default and `@Qualifier` when a particular consumer needs a specific implementation.

That's a very solid answer.

---

# 21. What if there are Three Implementations?

Suppose:

```text
PaymentService
   |
   ├── Stripe
   ├── Razorpay
   └── PayPal
```

You could do:

```java
@Primary
StripePaymentService
```

and then:

```text
normal injection → Stripe
```

while:

```java
@Qualifier("razorpay")
```

selects Razorpay.

And:

```java
@Qualifier("paypal")
```

selects PayPal.

This scales cleanly.

---

# 22. `@Fallback` — Modern Spring Detail

There is a newer annotation you should know about for current interviews:

```java
@Fallback
```

Spring 6.2 introduced `@Fallback`.

It marks beans as fallback candidates. If there is only one non-fallback candidate remaining, that candidate can effectively act as the default. ([docs.spring.io](https://docs.spring.io/spring/reference/core/beans/annotation-config/autowired-primary.html?utm_source=chatgpt.com))

For example:

```java
@Service
public class StripePaymentService
        implements PaymentService {
}
```

and:

```java
@Service
@Fallback
public class MockPaymentService
        implements PaymentService {
}
```

The normal bean can be selected while the mock remains available as a fallback.

This is newer and less likely to appear in older interview material, but it's worth knowing given current Spring versions.

---

# 23. What if no matching bean exists?

Suppose:

```java
public OrderService(PaymentService paymentService)
```

but the container contains no `PaymentService`.

For a required dependency, Spring fails dependency resolution and application context creation fails.

This is generally a startup error.

Conceptually:

```text
OrderService
     ↓
PaymentService
     ↓
no candidate
     ↓
dependency resolution failure
```

For normal `@Autowired` fields/methods, dependencies are required by default. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

---

# 24. How do we make a dependency optional?

Several mechanisms exist.

For example:

```java
@Autowired(required = false)
private PaymentService paymentService;
```

Spring supports `required = false` for annotated fields and methods. If the dependency is unavailable, the injection is skipped. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

But for modern constructor-based code, I often prefer:

```java
Optional<PaymentService>
```

or:

```java
ObjectProvider<PaymentService>
```

depending on the use case.

---

# 25. Constructor Injection with `Optional`

Example:

```java
@Service
public class OrderService {

    private final Optional<PaymentService> paymentService;

    public OrderService(
            Optional<PaymentService> paymentService) {

        this.paymentService = paymentService;
    }
}
```

Spring supports `Optional` for expressing a non-required dependency. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

Now:

```text
bean exists
   ↓
Optional.of(bean)

bean doesn't exist
   ↓
Optional.empty()
```

This makes the optional nature explicit in the type.

---

# 26. `@Nullable`

Another option:

```java
public OrderService(
        @Nullable PaymentService paymentService) {
}
```

Spring supports parameter-level nullability metadata to express that a dependency is optional. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

Still, from a design perspective:

```java
Optional<T>
```

often communicates optionality more explicitly for Java application code.

---

# 27. `ObjectProvider`

This goes beyond optionality.

Example:

```java
private final ObjectProvider<PaymentService> provider;

public OrderService(
        ObjectProvider<PaymentService> provider) {

    this.provider = provider;
}
```

Then:

```java
PaymentService service = provider.getObject();
```

This is useful for cases where the dependency may need to be resolved lazily, repeatedly, or dynamically.

You already saw this concept in the singleton/prototype scope discussion.

---

# 28. Injecting All Implementations

This is a **very important Spring interview capability**.

Instead of choosing one bean:

```java
PaymentService paymentService
```

you can inject all matching beans:

```java
List<PaymentService> paymentServices
```

For example:

```java
@Autowired
public PaymentProcessor(
        List<PaymentService> paymentServices) {

    this.paymentServices = paymentServices;
}
```

Spring can inject all matching beans into a typed collection. It also supports arrays, `Set`, and typed `Map`s. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

So:

```text
PaymentService
   |
   ├── Stripe
   ├── Razorpay
   └── PayPal

      ↓ List<PaymentService>

[Stripe, Razorpay, PayPal]
```

---

# 29. Why Would We Want All Implementations?

This becomes extremely powerful for **strategy patterns**.

Suppose:

```java
public interface DiscountStrategy {
    boolean supports(String customerType);
    BigDecimal calculate(Order order);
}
```

Then:

```text
DiscountStrategy
   |
   ├── PremiumDiscount
   ├── EmployeeDiscount
   ├── FestivalDiscount
   └── DefaultDiscount
```

Inject all of them:

```java
@Service
public class DiscountService {

    private final List<DiscountStrategy> strategies;

    public DiscountService(
            List<DiscountStrategy> strategies) {

        this.strategies = strategies;
    }
}
```

Now the service can choose a strategy at runtime.

This is often a much cleaner design than a giant:

```java
if (...) {
}
else if (...) {
}
else if (...) {
}
```

---

# 30. Ordering Multiple Beans

Suppose:

```java
@Autowired
private List<PaymentService> paymentServices;
```

What order will the beans appear in?

Spring supports ordering using mechanisms such as:

```java
@Order
```

or:

```java
Ordered
```

for applicable multi-bean injection scenarios. The current documentation notes that `@Order` can influence priority/order at injection points, but does **not** determine singleton startup order. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

For example:

```java
@Service
@Order(1)
public class StripePaymentService
        implements PaymentService {
}
```

```java
@Service
@Order(2)
public class RazorpayPaymentService
        implements PaymentService {
}
```

Then the injected collection can be ordered accordingly.

Remember:

```text
@Order
    ↓
ordering of applicable injected elements

NOT

bean startup dependency ordering
```

For startup dependencies, mechanisms such as `@DependsOn` are a different concern. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-dependson.html?utm_source=chatgpt.com))

---

# 31. Injecting a `Map`

Spring can also inject:

```java
Map<String, PaymentService>
```

Example:

```java
@Autowired
private Map<String, PaymentService> paymentServices;
```

The keys are the bean names and the values are the matching beans. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

Conceptually:

```text
{
    "stripePaymentService"  → StripePaymentService,
    "razorpayPaymentService" → RazorpayPaymentService,
    "paypalPaymentService"   → PayPalPaymentService
}
```

This can be useful for dynamic strategy selection:

```java
PaymentService service =
        paymentServices.get("stripePaymentService");
```

Again, explicit domain-oriented keys may be preferable in real designs rather than spreading framework bean names through business logic.

---

# 32. Multiple Constructor Scenario

This is a common interview trap.

Suppose:

```java
@Service
public class OrderService {

    public OrderService() {
    }

    public OrderService(PaymentService paymentService) {
    }
}
```

If multiple constructors exist, the resolution rules become more involved.

With an `@Autowired` constructor:

```java
@Autowired
public OrderService(PaymentService paymentService) {
}
```

Spring is explicitly told which constructor should be autowired. A single constructor does not need the annotation, while multiple constructors can use `@Autowired` according to Spring's constructor resolution rules. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

---

# 33. Multiple `@Autowired` Constructors

Current Spring rules are worth knowing.

You cannot simply have:

```java
@Autowired
public OrderService(A a) {}

@Autowired
public OrderService(A a, B b) {}
```

with both annotations using the default:

```text
required = true
```

Only one constructor can be the required autowired constructor. If multiple constructors are annotated, they need to be non-required candidates (`required=false`), and Spring chooses the best satisfiable candidate according to its constructor resolution algorithm. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

For normal application code:

> **Prefer one constructor containing all required dependencies.**

It is simpler and easier to understand.

---

# 34. Example of Good Constructor Injection

```java
@Service
public class OrderService {

    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final NotificationService notificationService;

    public OrderService(
            PaymentService paymentService,
            InventoryService inventoryService,
            NotificationService notificationService) {

        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.notificationService = notificationService;
    }
}
```

No:

```java
@Autowired
```

required if this is the only constructor.

It's obvious what the class needs.

---

# 35. A subtle `@Bean` issue

Suppose:

```java
@Configuration
public class AppConfig {

    @Bean
    public PaymentService paymentService() {
        return new StripePaymentService();
    }
}
```

The declared return type matters for autowiring.

For example, if you need to inject a specific interface or concrete type, the `@Bean` method's declared return type should be sufficiently expressive for Spring's type matching. Current Spring documentation explicitly calls this out. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

So:

```java
@Bean
public PaymentService paymentService()
```

means Spring's metadata advertises the bean as a `PaymentService`.

If you need injection by a more specific concrete type, the declaration may need to expose that type appropriately.

This is an advanced point, but useful when debugging:

> **"The bean exists, but Spring says no type match found."**

---

# 36. `@Autowired` vs `@Resource`

Another common interview question.

`@Autowired`:

```text
primarily type-driven
```

`@Resource`:

```text
name-oriented semantics
```

Spring's documentation explicitly contrasts them: `@Autowired` first uses type matching and qualifiers to narrow candidates, while `@Resource` is semantically intended to identify a specific target by name. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

For example:

```java
@Resource(name = "stripePaymentService")
private PaymentService paymentService;
```

is more explicitly name-oriented.

Whereas:

```java
@Autowired
@Qualifier("stripe")
private PaymentService paymentService;
```

is type matching + qualifier semantics.

---

# 37. Custom Qualifiers

This is a powerful advanced feature.

Suppose:

```java
@Qualifier
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Online {
}
```

Then:

```java
@Service
@Online
public class OnlinePaymentService
        implements PaymentService {
}
```

and:

```java
public OrderService(
        @Online PaymentService paymentService) {
}
```

Now you aren't relying on strings:

```java
@Qualifier("online")
```

You have a type-safe semantic qualifier.

Spring supports custom qualifier annotations by building them from `@Qualifier`. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

This can become useful when an application has many implementations.

---

# 38. Example: Payment System

Let's build a realistic design.

```java
public interface PaymentGateway {
    void pay(BigDecimal amount);
}
```

Implementations:

```java
@Service
@Qualifier("stripe")
public class StripePaymentGateway
        implements PaymentGateway {

    @Override
    public void pay(BigDecimal amount) {
        System.out.println("Stripe");
    }
}
```

```java
@Service
@Qualifier("razorpay")
public class RazorpayPaymentGateway
        implements PaymentGateway {

    @Override
    public void pay(BigDecimal amount) {
        System.out.println("Razorpay");
    }
}
```

Consumer:

```java
@Service
public class PaymentService {

    private final PaymentGateway gateway;

    public PaymentService(
            @Qualifier("stripe")
            PaymentGateway gateway) {

        this.gateway = gateway;
    }
}
```

Now the dependency is:

```text
PaymentService
      ↓
PaymentGateway
      ↓
@Qualifier("stripe")
      ↓
StripePaymentGateway
```

Clean and explicit.

---

# 39. Why Field Injection Often Hides Problems

Consider:

```java
@Service
public class OrderService {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private AuditService auditService;
}
```

You might have:

```text
5 dependencies
```

hidden in the class.

This can be a useful design smell:

> This class may be doing too much.

With constructor injection:

```java
public OrderService(
        PaymentService paymentService,
        InventoryService inventoryService,
        NotificationService notificationService,
        PricingService pricingService,
        AuditService auditService) {
}
```

the dependency count becomes obvious.

Constructor injection therefore makes design problems easier to see.

---

# 40. Why Constructor Injection Helps Circular Dependency Detection

Suppose:

```text
A → B
B → A
```

Constructor injection:

```java
class A {
    A(B b) {}
}
```

```java
class B {
    B(A a) {}
}
```

Spring encounters a circular dependency problem while constructing them.

That's useful because the design issue becomes visible.

We'll have a **dedicated circular-dependency topic** later because Spring's behavior around circular references, proxies, lazy beans, and newer framework defaults is worth studying separately.

---

# 41. Important: Spring doesn't "inject by interface name"

Suppose:

```java
public interface PaymentService {
}
```

and:

```java
StripePaymentService implements PaymentService
```

Spring doesn't do:

```text
PaymentService
  ↓
look for class called PaymentServiceImpl
```

There is no requirement for an implementation to be called:

```text
PaymentServiceImpl
```

Spring mainly works from:

```text
required type
+
qualifiers
+
primary/fallback status
+
other candidate-resolution rules
```

This is why interface-based design works so well with DI.

---

# 42. What happens when a dependency is itself a dependency?

Suppose:

```text
OrderService
    ↓
PaymentService
    ↓
PaymentGateway
    ↓
HttpClient
```

Spring recursively resolves the object graph:

```text
Need OrderService
     ↓
Need PaymentService
     ↓
Need PaymentGateway
     ↓
Need HttpClient
     ↓
construct lower-level dependencies
     ↓
construct PaymentGateway
     ↓
construct PaymentService
     ↓
construct OrderService
```

This is why dependency injection is more than simply:

```text
@Autowired
```

The container is effectively building a dependency graph.

---

# 43. What does `@Autowired` actually trigger internally?

A useful advanced answer:

Spring processes `@Autowired` through its bean post-processing infrastructure. The current documentation states that `@Autowired`, `@Inject`, `@Value`, and `@Resource` are handled by `BeanPostProcessor` implementations. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

Conceptually:

```text
Bean created
    ↓
AutowiredAnnotationBeanPostProcessor
    ↓
detect injection points
    ↓
resolve dependencies
    ↓
inject dependencies
```

The specific infrastructure component you should know by name is:

```text
AutowiredAnnotationBeanPostProcessor
```

That's a strong advanced interview answer.

---

# 44. An important distinction

Don't say:

> "`@Autowired` creates the dependency."

More accurate:

> "`@Autowired` identifies an injection point. Spring's container resolves a matching bean and supplies it through its dependency-injection infrastructure."

The dependency itself must already be something Spring can resolve/create as a bean or another supported resolvable dependency.

---

# 45. Special resolvable dependencies

Spring can also inject certain infrastructure objects such as:

```text
BeanFactory
ApplicationContext
Environment
ResourceLoader
ApplicationEventPublisher
MessageSource
```

without you defining ordinary application beans for them in the usual way. Spring documents these as well-known resolvable dependencies. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

For example:

```java
@Service
public class MyService {

    private final Environment environment;

    public MyService(Environment environment) {
        this.environment = environment;
    }
}
```

This is useful, but don't use infrastructure injection as an excuse to access the whole container everywhere.

---

# 46. A realistic resolution algorithm

You don't need to memorize every internal implementation branch, but think approximately like this for a single-valued dependency:

```text
Required type
     ↓
Find eligible beans matching type
     ↓
Any qualifier constraints?
     ↓
Narrow candidates
     ↓
Is there exactly one?
     ↓ yes
Inject it

     ↓ no
Is there exactly one @Primary candidate?
     ↓ yes
Inject it

     ↓ no
Are there other candidate-resolution rules?
     ↓
Possible injection-point/bean-name match,
fallback semantics, etc.

     ↓
Still ambiguous?
     ↓
ApplicationContext fails
```

Current Spring has additional nuances such as `@Fallback`, injection-point name matching, priority/ordering in collection cases, and other resolver rules, so don't present this as the literal internal algorithm in every situation. ([docs.spring.io](https://docs.spring.io/spring/reference/core/beans/annotation-config/autowired-primary.html?utm_source=chatgpt.com))

But it's an excellent interview mental model.

---

# 47. Scenario Question

### Interviewer:

> We have three `PaymentService` implementations. How will you inject one specific implementation?

Answer:

```java
public PaymentService(
        @Qualifier("stripe")
        PaymentService paymentService)
```

Then explain:

> Spring first considers candidates matching the required type and the qualifier narrows them to the intended implementation.

---

# 48. Scenario Question

### Interviewer:

> What if one implementation should be the default everywhere?

Answer:

```java
@Primary
@Service
public class StripePaymentService
        implements PaymentService {
}
```

Then:

> Normal single-valued injections can use the primary implementation unless another resolution mechanism, such as a qualifier, selects a different candidate.

---

# 49. Scenario Question

> What if I want all implementations?

Answer:

```java
public PaymentProcessor(
        List<PaymentService> paymentServices) {
}
```

or:

```java
Set<PaymentService>
```

or:

```java
Map<String, PaymentService>
```

Spring supports all of these styles for matching beans. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

---

# 50. Scenario Question

> Why do you prefer constructor injection?

Strong answer:

> Constructor injection makes required dependencies explicit, allows them to be `final`, improves immutability and testability, prevents constructing the object without its required dependencies, and avoids hidden dependencies that are common with field injection.

---

# 51. Scenario Question

> Can `@Autowired` be used on a constructor without specifying it on a single-constructor class?

Answer:

> Yes. If the bean has only one constructor, Spring can use that constructor without `@Autowired`. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

---

# 52. Scenario Question

> What happens when multiple `@Primary` beans exist?

Answer:

> `@Primary` no longer uniquely resolves the dependency, so Spring needs another candidate-selection mechanism such as a qualifier or an otherwise resolvable distinction. Multiple primary candidates do not create a unique winner. ([docs.spring.io](https://docs.spring.io/spring/reference/core/beans/annotation-config/autowired-primary.html?utm_source=chatgpt.com))

---

# 53. Scenario Question

> Does `@Order` decide which bean gets injected for a normal single-valued dependency?

Generally, no.

`@Order` is primarily relevant when multiple beans are injected as collections/arrays and their ordering matters. It does not function as a general replacement for `@Primary`/`@Qualifier`, and current Spring docs explicitly distinguish injection ordering from singleton startup order. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

That's a good trap to know.

---

# 54. Scenario Question

> Does `@Qualifier` need a unique value across the whole application?

Not necessarily.

Qualifier values act as filtering criteria among type-compatible candidates. Spring's current docs even note that the same qualifier can be associated with multiple beans, particularly for collection injection. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

So:

```text
@Qualifier("action")
```

doesn't inherently mean:

```text
exactly one bean in the entire container
```

It's a qualifier/filtering concept.

---

# 55. EPAM-Level Interview Answer

### "Explain how Spring resolves an `@Autowired` dependency."

A strong answer:

> Spring first identifies the dependency's required type and looks for eligible beans matching that type. If multiple candidates exist, Spring can narrow them using qualifier metadata, or prefer a uniquely marked `@Primary` candidate. Modern Spring also has fallback and injection-point-name resolution rules. If the dependency remains ambiguous or no required candidate exists, context creation or bean creation fails. For collections and maps, Spring can inject all matching beans rather than selecting a single one.

That answer demonstrates substantially more knowledge than:

> "Spring searches for the bean and injects it."

---

# 56. Common mistakes to avoid in interviews

### Mistake 1

> "`@Autowired` always injects by name."

Not correct. It is fundamentally type-driven, with qualifiers and other resolution rules. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-qualifiers.html?utm_source=chatgpt.com))

### Mistake 2

> "`@Primary` means only this bean exists."

No.

It means preferred candidate.

### Mistake 3

> "`@Qualifier` is exactly the same as `@Resource`."

No. Their semantics differ.

### Mistake 4

> "`@Order` chooses the implementation."

Not for normal single-valued injection.

### Mistake 5

> "Field injection is the only way `@Autowired` works."

No. Constructors, setters, and methods can also be injection points. ([docs.spring.io](https://docs.spring.io/spring-framework/reference/7.1/core/beans/annotation-config/autowired.html?utm_source=chatgpt.com))

### Mistake 6

> "A dependency must always be a Spring bean."

For ordinary autowiring, candidates generally need to be resolvable from the container, though Spring also has special resolvable infrastructure dependencies and other mechanisms.

---

# 57. Your Mental Model

At this point, visualize:

```text
                   Injection Point
                         |
                         ↓
                  Required Type
                         |
                         ↓
                Candidate Beans
                         |
             ┌───────────┴───────────┐
             ↓                       ↓
         @Qualifier              @Primary
             |                       |
             └───────────┬───────────┘
                         ↓
                  Candidate Selection
                         |
               ┌─────────┴─────────┐
               ↓                   ↓
            One bean            Multiple
               ↓                   ↓
             inject          resolve further
                                   |
                              still ambiguous
                                   ↓
                               FAIL
```

And for multiple beans:

```text
PaymentService
     |
     ├── Stripe
     ├── Razorpay
     └── PayPal
             |
             ├── PaymentService  → one selected with @Qualifier/@Primary
             |
             └── List<PaymentService> → all
```

---

# 58. The five things to know cold

Before proceeding, make sure these are crystal clear:

### 1. Constructor injection

```java
public OrderService(PaymentService paymentService)
```

Preferred for required dependencies.

### 2. `@Primary`

```java
@Primary
```

Marks a preferred candidate.

### 3. `@Qualifier`

```java
@Qualifier("stripe")
```

Narrows candidate selection for a particular injection point.

### 4. Collection injection

```java
List<PaymentService>
```

Gets all matching implementations.

### 5. `@Autowired` is not DI itself

It's Spring's annotation-based mechanism for identifying injection points.

---

# Where we go next

The next topic is extremely important because we have now introduced:

```text
@Component
@Service
@Repository
@Controller
@RestController
```

but haven't properly explained how these classes become beans.

# Topic 7 — Component Scanning and Stereotype Annotations

We'll go deep into:

```text
@Component
@Service
@Repository
@Controller
@RestController
@ComponentScan
```

including:

> **What is the actual difference between `@Component`, `@Service`, and `@Repository`?**

> **Is `@Service` technically doing anything different from `@Component`?**

> **What does `@Repository` add beyond being a component?**

> **How does `@ComponentScan` find classes?**

> **Why does putting your main class in the wrong package cause beans not to be found?**

> **What happens if two components have the same bean name?**

> **How does Spring generate the default bean name?**

Those are very common Spring Boot interview questions.

