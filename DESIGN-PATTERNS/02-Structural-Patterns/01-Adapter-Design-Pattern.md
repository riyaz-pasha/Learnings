# 4. Adapter Design Pattern

Now we move into the **Structural Design Patterns**.

The **Adapter Pattern** is one of the easiest patterns to understand once you see the problem it solves.

Its central idea:

> **Allow two incompatible interfaces to work together by introducing an adapter between them.**

Think of a physical power adapter:

```text
Your device
   ↓
Different plug
   ↓
Power Adapter
   ↓
Wall socket
```

The device and socket don't directly match.

The adapter converts one interface into the form the other expects.

The same idea applies in software.

---

# 1. The Problem

Suppose your application expects this interface:

```java
public interface PaymentGateway {

    void pay(double amount);
}
```

Your application code uses it:

```java
public class OrderService {

    private final PaymentGateway paymentGateway;

    public OrderService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public void checkout(double amount) {
        paymentGateway.pay(amount);
    }
}
```

Everything is clean.

Now the business wants to integrate with an external payment library.

The external library provides:

```java
public class ThirdPartyPayment {

    public void makePayment(double amountInRupees) {
        System.out.println(
            "Payment of ₹" + amountInRupees
        );
    }
}
```

Problem:

Your application expects:

```java
pay(double)
```

but the external system provides:

```java
makePayment(double)
```

The two interfaces don't match.

---

# 2. The naive solution

You might change your application:

```java
ThirdPartyPayment payment =
        new ThirdPartyPayment();

payment.makePayment(amount);
```

But now `OrderService` is coupled directly to the third-party class.

```text
OrderService
     │
     ▼
ThirdPartyPayment
```

That means your internal code now depends on the external API.

If tomorrow the external provider changes:

```text
ThirdPartyPayment
        ↓
AnotherPaymentLibrary
```

you potentially have to modify business logic.

---

# 3. Adapter to the rescue

We keep our application's expected interface:

```java
public interface PaymentGateway {

    void pay(double amount);
}
```

Then create an adapter:

```java
public class PaymentAdapter
        implements PaymentGateway {

    private final ThirdPartyPayment thirdPartyPayment;

    public PaymentAdapter(
            ThirdPartyPayment thirdPartyPayment) {
        this.thirdPartyPayment = thirdPartyPayment;
    }

    @Override
    public void pay(double amount) {
        thirdPartyPayment.makePayment(amount);
    }
}
```

Now:

```java
ThirdPartyPayment thirdPartyPayment =
        new ThirdPartyPayment();

PaymentGateway paymentGateway =
        new PaymentAdapter(thirdPartyPayment);

OrderService orderService =
        new OrderService(paymentGateway);

orderService.checkout(1000);
```

The architecture becomes:

```text
OrderService
      │
      ▼
PaymentGateway
      ▲
      │
PaymentAdapter
      │
      ▼
ThirdPartyPayment
```

The adapter translates:

```text
pay()
  ↓
makePayment()
```

---

# 4. The most important idea

The client expects:

```text
Target interface
```

The existing class provides:

```text
Adaptee interface
```

The Adapter connects them.

The terminology is:

```text
Client
  ↓
Target
  ↑
Adapter
  ↓
Adaptee
```

Let's map our example:

```text
Client
    = OrderService

Target
    = PaymentGateway

Adapter
    = PaymentAdapter

Adaptee
    = ThirdPartyPayment
```

This terminology is extremely useful in interviews.

---

# 5. Formal definition

A good interview definition:

> **Adapter is a structural design pattern that allows objects with incompatible interfaces to collaborate by converting the interface of an existing class into the interface expected by the client.**

Notice that we're not changing the third-party class.

Instead:

```text
Existing class
      +
Adapter
      =
Compatible interface
```

---

# 6. Why is Adapter a Structural pattern?

Remember your categories:

```text
Creational
→ how objects are created

Structural
→ how objects/classes are composed

Behavioral
→ how objects communicate/behave
```

Adapter is structural because we're arranging existing classes so that they can collaborate.

We aren't primarily changing:

```text
how objects are created
```

or:

```text
the business algorithm
```

We're changing how their interfaces fit together.

---

# 7. A very simple example

Suppose your code expects:

```java
public interface Printer {

    void print(String text);
}
```

Existing legacy system:

```java
public class LegacyPrinter {

    public void printText(String text) {
        System.out.println(text);
    }
}
```

Adapter:

```java
public class PrinterAdapter
        implements Printer {

    private final LegacyPrinter legacyPrinter;

    public PrinterAdapter(LegacyPrinter legacyPrinter) {
        this.legacyPrinter = legacyPrinter;
    }

    @Override
    public void print(String text) {
        legacyPrinter.printText(text);
    }
}
```

Now the client doesn't care that the old system uses:

```java
printText()
```

It sees:

```java
print()
```

---

# 8. Adapter is a translator

A useful mental model:

```text
Client speaks English
        │
        ▼
     Adapter
        │
        ▼
Legacy system speaks French
```

The adapter translates between interfaces.

For example:

```text
Client:
pay(amount)

Adapter:
pay(amount)
    ↓
makePayment(amount)

Third party:
makePayment(amount)
```

So:

> **Adapter is an interface translator.**

That's one of the best ways to remember it.

---

# 9. Object Adapter

The example we've built is called an **Object Adapter**.

Why?

Because the adapter **contains an instance** of the adaptee:

```java
private final ThirdPartyPayment thirdPartyPayment;
```

So:

```text
Adapter
   │
   └── has-a → Adaptee
```

This is composition.

Structure:

```text
       Client
          │
          ▼
        Target
          ▲
          │
       Adapter
          │
       has-a
          │
          ▼
       Adaptee
```

This is the most common form in modern Java.

---

# 10. Class Adapter

The other classic implementation is the **Class Adapter**.

Conceptually:

```text
Adapter inherits from Adaptee
and implements Target
```

For example:

```java
public class PrinterAdapter
        extends LegacyPrinter
        implements Printer {

    @Override
    public void print(String text) {
        printText(text);
    }
}
```

Now:

```text
PrinterAdapter
     │
     ├── extends LegacyPrinter
     │
     └── implements Printer
```

This uses inheritance rather than composition.

---

# 11. Object Adapter vs Class Adapter

| Object Adapter             | Class Adapter                      |
| -------------------------- | ---------------------------------- |
| Uses composition           | Uses inheritance                   |
| `Adapter has Adaptee`      | `Adapter extends Adaptee`          |
| More flexible              | More restrictive                   |
| Common in Java             | Less commonly used                 |
| Works well with interfaces | Depends on inheritance limitations |

In Java, **Object Adapter is generally more practical** because Java has single class inheritance.

For example, if your adapter extends:

```java
LegacyPrinter
```

it can't also extend:

```java
AnotherBaseClass
```

But with composition, you can hold whatever dependencies you need.

---

# 12. Why composition is often preferred

Suppose:

```java
public class PaymentAdapter
        implements PaymentGateway {

    private final ThirdPartyPayment payment;
}
```

You can inject:

```java
ThirdPartyPayment
```

from outside.

You can also replace it:

```java
AnotherPaymentImplementation
```

without changing the adapter interface.

This is another example of:

> **Favor composition over inheritance.**

That principle appears repeatedly throughout design-pattern discussions.

---

# 13. Adapter doesn't have to rename methods only

The adapter may perform real transformation.

Suppose our system expects:

```java
public interface TemperatureService {

    double getTemperatureInCelsius();
}
```

Third-party API gives:

```java
public class WeatherApi {

    public double getTemperatureInFahrenheit() {
        return 86;
    }
}
```

Adapter:

```java
public class WeatherAdapter
        implements TemperatureService {

    private final WeatherApi api;

    public WeatherAdapter(WeatherApi api) {
        this.api = api;
    }

    @Override
    public double getTemperatureInCelsius() {

        double fahrenheit =
                api.getTemperatureInFahrenheit();

        return (fahrenheit - 32) * 5 / 9;
    }
}
```

Now the adapter does:

```text
API format
   ↓
conversion
   ↓
application format
```

So Adapter can transform:

* method names
* parameter order
* data types
* units
* response structures
* exceptions
* authentication details
* protocol differences

---

# 14. Real-world example: Legacy systems

Imagine your modern application expects:

```java
public interface CustomerRepository {

    Customer findById(Long id);
}
```

But your legacy system has:

```java
public class LegacyCustomerSystem {

    public LegacyCustomerRecord fetchCustomer(String customerId) {
        // ...
    }
}
```

Adapter:

```java
public class LegacyCustomerRepositoryAdapter
        implements CustomerRepository {

    private final LegacyCustomerSystem legacySystem;

    public LegacyCustomerRepositoryAdapter(
            LegacyCustomerSystem legacySystem) {
        this.legacySystem = legacySystem;
    }

    @Override
    public Customer findById(Long id) {

        LegacyCustomerRecord record =
                legacySystem.fetchCustomer(
                        String.valueOf(id)
                );

        return mapToCustomer(record);
    }

    private Customer mapToCustomer(
            LegacyCustomerRecord record) {

        // conversion logic
        return ...;
    }
}
```

Your application can continue using:

```java
CustomerRepository
```

without knowing the legacy system's API.

This is a very realistic use of Adapter.

---

# 15. Adapter is particularly useful when you cannot modify the existing class

Suppose:

```text
ThirdPartyPayment
```

comes from a library.

You cannot change:

```java
makePayment()
```

to:

```java
pay()
```

because you don't own the source code.

You could create a wrapper:

```java
PaymentAdapter
```

that exposes the interface your application needs.

This is one of the most common real-world scenarios.

---

# 16. Adapter vs changing the original class

You might say:

> "Why not simply modify `ThirdPartyPayment`?"

Often you can't.

Or you shouldn't.

Reasons:

```text
Third-party code
Legacy code
Shared library
External vendor
Already-used API
Risk of breaking existing clients
```

Adapter allows you to integrate without modifying the existing implementation.

---

# 17. Adapter vs Decorator

This is one of the most important comparisons because both often contain another object.

### Adapter

Changes the **interface**.

```text
Old interface
      ↓
Adapter
      ↓
New interface
```

### Decorator

Keeps the **same interface** but adds behavior.

```text
Same interface
      ↓
Decorator
      ↓
Same interface + extra behavior
```

Example:

```text
Adapter:
PaymentGateway
    ↓
PaymentAdapter
    ↓
ThirdPartyPayment
```

The interface is translated.

Decorator:

```text
Notification
    ↓
LoggingDecorator
    ↓
Notification
```

The client still sees `Notification`.

We'll study Decorator next, so remember this distinction.

---

# 18. Adapter vs Facade

Another important interview comparison.

### Adapter

> Makes **one incompatible interface** compatible with another.

```text
Expected interface
        ↕
     Adapter
        ↕
Existing interface
```

### Facade

> Provides a **simplified interface** over a complex subsystem.

Suppose:

```text
OrderService
   ↓
PaymentService
InventoryService
ShippingService
NotificationService
```

Facade:

```java
public class OrderFacade {

    public void placeOrder(Order order) {

        inventory.reserve(order);
        payment.charge(order);
        shipping.ship(order);
        notification.sendConfirmation(order);
    }
}
```

The facade isn't necessarily adapting incompatible interfaces.

It's simplifying access to many components.

Remember:

```text
Adapter
→ compatibility

Facade
→ simplification
```

---

# 19. Adapter vs Proxy

Another common interview question.

### Adapter

Changes the interface.

```text
A interface
    ↓
Adapter
    ↓
B interface
```

### Proxy

Usually keeps the same interface and controls access to the real object.

```text
Client
   ↓
Proxy
   ↓
Real Object
```

Examples of Proxy behavior:

```text
lazy loading
access control
remote access
caching
logging
```

The proxy generally tries to appear like the real object.

Adapter instead tries to **look like the interface the client expects**.

---

# 20. Adapter and SOLID

Adapter can be useful for maintaining the **Dependency Inversion Principle**.

Suppose your application has:

```java
public interface PaymentGateway {
    void pay(double amount);
}
```

The business layer depends on that abstraction.

Then:

```text
Business layer
      ↓
PaymentGateway
      ↑
PaymentAdapter
      ↓
Third-party SDK
```

This lets the third-party dependency remain at the edge of the application.

This is very common in **hexagonal architecture / ports and adapters architecture**.

---

# 21. Adapter and Hexagonal Architecture

This is a particularly valuable connection for backend interviews.

In Hexagonal Architecture:

```text
           Application Core
                 │
          ┌──────┴──────┐
          ▼             ▼
        Ports         Ports
          ▲             ▲
          │             │
       Adapters       Adapters
          │             │
          ▼             ▼
      Database      External API
```

The application defines interfaces representing what it needs.

Adapters connect those interfaces to external technologies.

For example:

```java
public interface PaymentGateway {
    void pay(double amount);
}
```

Then:

```java
StripePaymentAdapter
```

may translate:

```text
Application's PaymentGateway
          ↓
       Adapter
          ↓
     Stripe SDK
```

This is essentially Adapter thinking at an architectural level.

---

# 22. Adapter in Spring Boot

In Spring Boot, this pattern appears frequently at integration boundaries.

Suppose:

```java
public interface NotificationClient {

    void send(String recipient, String message);
}
```

External library:

```java
public class ExternalSmsClient {

    public void sendSms(
            String phoneNumber,
            String text) {

        // external API
    }
}
```

Adapter:

```java
@Component
public class SmsNotificationAdapter
        implements NotificationClient {

    private final ExternalSmsClient client;

    public SmsNotificationAdapter(
            ExternalSmsClient client) {
        this.client = client;
    }

    @Override
    public void send(
            String recipient,
            String message) {

        client.sendSms(recipient, message);
    }
}
```

Your business service:

```java
@Service
public class NotificationService {

    private final NotificationClient client;

    public NotificationService(
            NotificationClient client) {
        this.client = client;
    }

    public void notifyUser(
            String recipient,
            String message) {

        client.send(recipient, message);
    }
}
```

Architecture:

```text
NotificationService
        │
        ▼
NotificationClient
        ▲
        │
SmsNotificationAdapter
        │
        ▼
ExternalSmsClient
```

Spring Dependency Injection handles the wiring.

This is a very realistic combination:

```text
Adapter + Interface + Dependency Injection
```

---

# 23. Adapter can also normalize multiple vendors

This is where the pattern becomes extremely powerful.

Suppose you have:

```text
Stripe
PayPal
Razorpay
Adyen
```

Each has a different SDK/API.

Your application defines:

```java
public interface PaymentGateway {

    PaymentResult charge(
            Money amount,
            Customer customer);
}
```

Then:

```text
              PaymentGateway
                    ▲
          ┌─────────┼─────────┐
          │         │         │
          │         │         │
      Stripe      PayPal   Razorpay
      Adapter     Adapter   Adapter
          │         │         │
        SDK         SDK       SDK
```

Now business logic doesn't need to understand four different vendor APIs.

Each Adapter translates:

```text
Internal contract
       ↕
Vendor-specific API
```

This is one of the strongest practical uses of the pattern.

---

# 24. Adapter and exception translation

Adapters can translate errors as well.

Suppose an external API throws:

```java
ThirdPartyPaymentException
```

while your application expects:

```java
PaymentException
```

The adapter can do:

```java
@Override
public void pay(double amount) {

    try {
        thirdPartyPayment.makePayment(amount);
    } catch (ThirdPartyPaymentException ex) {
        throw new PaymentException(
                "Payment failed",
                ex
        );
    }
}
```

Now the application doesn't need to know about vendor-specific exception types.

That's good encapsulation.

---

# 25. Adapter and data transformation

Suppose an external API gives:

```json
{
    "first_name": "John",
    "last_name": "Doe",
    "mobile_no": "1234567890"
}
```

Your domain expects:

```java
public class Customer {
    private String firstName;
    private String lastName;
    private String phone;
}
```

An adapter can transform:

```text
ExternalCustomerDto
       ↓
CustomerAdapter
       ↓
Customer
```

This is often more than a simple method rename.

---

# 26. One Adapter vs multiple Adapters

Usually you create adapters around distinct incompatible dependencies.

For example:

```text
PaymentGateway
    ├── StripeAdapter
    ├── PayPalAdapter
    └── RazorpayAdapter
```

This lets you isolate vendor-specific code.

A useful architectural principle is:

> **Keep external-system knowledge at the boundary.**

Your application core should ideally not be littered with:

```java
StripePaymentIntent
PayPalOrderRequest
RazorpayResponse
```

Instead:

```text
Core
 ↓
your interface
 ↓
Adapter
 ↓
vendor SDK
```

---

# 27. An important distinction: Adapter vs Wrapper

People often say:

> "Adapter is just a wrapper."

That's not completely wrong, but it's incomplete.

A wrapper is a broad programming concept:

```text
Object A wraps Object B
```

An Adapter specifically exists to:

> **make an existing interface usable through another expected interface.**

So:

```text
Every Adapter may be implemented as a wrapper,
but not every wrapper is an Adapter.
```

---

# 28. When should you NOT use Adapter?

Don't create an Adapter simply because another class exists.

For example:

```java
public class UserService {
    private final UserRepository repository;
}
```

If `UserRepository` already has exactly the interface you need, there's no reason to make:

```text
UserRepositoryAdapter
      ↓
UserRepository
```

That just adds indirection.

Use Adapter when there is an actual mismatch or boundary worth isolating.

---

# 29. Advantages of Adapter

### Reuse existing code

You can use a class even though its interface doesn't match.

### Reduce coupling

The client doesn't need to know the adaptee's API.

### Isolate third-party dependencies

Vendor-specific code stays inside the adapter.

### Support legacy code

You can integrate old systems without rewriting them.

### Enable gradual migration

You can introduce a clean internal interface while keeping the old implementation temporarily.

---

# 30. Disadvantages

Adapter also introduces another layer:

```text
Client
 ↓
Adapter
 ↓
Adaptee
```

That means:

* more classes
* more indirection
* another abstraction to maintain

If there's no genuine incompatibility, it can be unnecessary complexity.

---

# 31. A complete example

Let's build one from scratch.

### Our application contract

```java
public interface ShippingService {

    void ship(String address);
}
```

### External library

```java
public class FedExClient {

    public void createShipment(String destination) {
        System.out.println(
                "FedEx shipment created for " + destination
        );
    }
}
```

### Adapter

```java
public class FedExShippingAdapter
        implements ShippingService {

    private final FedExClient fedExClient;

    public FedExShippingAdapter(FedExClient fedExClient) {
        this.fedExClient = fedExClient;
    }

    @Override
    public void ship(String address) {
        fedExClient.createShipment(address);
    }
}
```

### Client

```java
public class OrderService {

    private final ShippingService shippingService;

    public OrderService(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    public void placeOrder(String address) {
        // order logic...

        shippingService.ship(address);
    }
}
```

### Wiring

```java
FedExClient fedExClient =
        new FedExClient();

ShippingService shippingService =
        new FedExShippingAdapter(fedExClient);

OrderService orderService =
        new OrderService(shippingService);

orderService.placeOrder("Hyderabad");
```

The important architectural property:

```text
OrderService
    knows
       ↓
ShippingService

OrderService does NOT know
       ↓
FedExClient
```

That separation is the real value.

---

# 32. Multiple adapters

Tomorrow we add DHL:

```java
public class DHLClient {

    public void bookDelivery(String location) {
        System.out.println(
                "DHL delivery booked for " + location
        );
    }
}
```

Adapter:

```java
public class DHLShippingAdapter
        implements ShippingService {

    private final DHLClient dhlClient;

    public DHLShippingAdapter(DHLClient dhlClient) {
        this.dhlClient = dhlClient;
    }

    @Override
    public void ship(String address) {
        dhlClient.bookDelivery(address);
    }
}
```

Now:

```text
                ShippingService
                      ▲
               ┌──────┴──────┐
               │             │
               │             │
        FedExAdapter     DHLAdapter
               │             │
               ▼             ▼
         FedExClient      DHLClient
```

The application uses the same interface:

```java
shippingService.ship(address);
```

regardless of which vendor is behind it.

---

# 33. Adapter vs Strategy

Another subtle but important distinction.

### Adapter

The implementation already exists.

You are making it compatible with your interface.

```text
Existing API
    ↓
Adapter
    ↓
Your API
```

### Strategy

You're intentionally defining multiple interchangeable behaviors.

```text
Strategy
 ├── CreditCardStrategy
 ├── UpiStrategy
 └── PayPalStrategy
```

So:

```text
Adapter
→ compatibility problem

Strategy
→ behavior selection problem
```

---

# 34. Adapter vs Decorator

This deserves another look because the distinction is frequently asked.

Suppose:

```java
public interface Notification {
    void send(String message);
}
```

### Adapter

External implementation:

```java
ExternalNotifier.sendMessage()
```

Adapter:

```java
send()
   ↓
sendMessage()
```

Interface changes.

### Decorator

Existing object:

```java
Notification
```

Decorator:

```java
public class LoggingNotificationDecorator
        implements Notification {

    private final Notification notification;

    @Override
    public void send(String message) {

        System.out.println("Sending...");

        notification.send(message);
    }
}
```

Interface remains:

```java
Notification
```

but behavior changes.

So:

```text
Adapter:
different interface → compatible interface

Decorator:
same interface → enhanced behavior
```

---

# 35. Adapter and Open/Closed Principle

Adapter can help you integrate new implementations without changing core business code.

Suppose:

```text
Application
   ↓
PaymentGateway
```

Then adding:

```text
NewPaymentProvider
```

could involve:

```text
NewPaymentAdapter
```

while the core continues depending on:

```java
PaymentGateway
```

This is a practical example of designing around stable abstractions.

Again, don't say:

> "Adapter automatically guarantees OCP."

Patterns help support principles; they don't magically guarantee them.

---

# 36. Interview question: "What are the participants in Adapter?"

Classic terminology:

### Target

The interface expected by the client.

```java
PaymentGateway
```

### Client

The code that uses the Target.

```java
OrderService
```

### Adaptee

The existing class with the incompatible interface.

```java
ThirdPartyPayment
```

### Adapter

The bridge between them.

```java
PaymentAdapter
```

Architecture:

```text
Client
  ↓
Target
  ↑
Adapter
  ↓
Adaptee
```

Memorize this structure.

---

# 37. Interview question: "Why is Adapter useful?"

Strong answer:

> "Adapter allows us to reuse an existing class whose interface doesn't match what our client expects. It encapsulates the translation between the two interfaces, which is particularly useful for integrating third-party libraries, legacy systems, or multiple external providers."

---

# 38. Interview question: "Object Adapter vs Class Adapter?"

Strong answer:

> "Object Adapter uses composition: the adapter contains an instance of the adaptee. Class Adapter uses inheritance: the adapter extends the adaptee and implements the target interface. Object Adapter is generally more flexible, especially in Java because Java supports single class inheritance."

---

# 39. Interview question: "Does Adapter modify the adaptee?"

Usually no.

Instead:

```text
Adaptee
   +
Adapter
```

The original class remains unchanged.

That's especially useful when it's:

```text
third-party
legacy
shared
not under your control
```

---

# 40. Interview question: "Can Adapter translate data?"

Absolutely.

It can translate:

```text
method names
parameters
data structures
units
exceptions
protocols
formats
```

For example:

```text
Fahrenheit
   ↓
Adapter
   ↓
Celsius
```

or:

```text
VendorCustomerDto
   ↓
Adapter
   ↓
Domain Customer
```

---

# 41. Interview question: "Is Adapter the same as Facade?"

No.

A good answer:

> "Adapter makes an incompatible interface compatible with the interface expected by a client. Facade provides a simplified interface over a complex subsystem."

Remember:

```text
Adapter → compatibility
Facade  → simplification
```

---

# 42. Interview question: "Is Adapter the same as Decorator?"

No.

> "Adapter changes or translates an interface. Decorator preserves the interface while adding responsibilities or behavior."

Remember:

```text
Adapter
A interface → B interface

Decorator
A interface → A interface + behavior
```

---

# 43. Adapter in one diagram

This is the diagram I'd remember for interviews:

```text
                     CLIENT
                        │
                        │ expects
                        ▼
                 ┌─────────────┐
                 │   TARGET    │
                 │  interface  │
                 └──────▲──────┘
                        │
                        │ implements
                        │
                 ┌──────┴──────┐
                 │   ADAPTER   │
                 └──────┬──────┘
                        │
                        │ delegates/translates
                        ▼
                 ┌─────────────┐
                 │   ADAPTEE   │
                 │ existing    │
                 │ class       │
                 └─────────────┘
```

---

# 44. The simplest mental model

Whenever you see:

```text
"My application needs interface A,
but this existing library gives me interface B."
```

think:

> **Adapter.**

For example:

```text
I need:

PaymentGateway.pay()

But vendor gives:

ThirdPartyPayment.makePayment()

             ↓

          Adapter

             ↓

PaymentGateway.pay()
```

---

# 45. Adapter vs all patterns we've covered so far

You now have four patterns:

```text
Singleton
→ control number of instances

Factory
→ control/encapsulate creation

Builder
→ construct/configure complex objects

Adapter
→ make incompatible interfaces work together
```

A useful memory map:

```text
        DESIGN PROBLEM
             │
   ┌─────────┼──────────┬─────────────┐
   ▼         ▼          ▼             ▼
Singleton  Factory    Builder       Adapter
   │         │          │             │
How many? Which one?  How build?  How connect?
```

---

# 46. One realistic Spring Boot architecture

This is worth understanding because it ties together your interview preparation.

Suppose your business layer defines:

```java
public interface PaymentGateway {
    PaymentResult pay(PaymentRequest request);
}
```

External provider:

```text
Stripe SDK
```

You implement:

```java
@Component
public class StripePaymentAdapter
        implements PaymentGateway {

    private final StripeClient stripeClient;

    public StripePaymentAdapter(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    @Override
    public PaymentResult pay(PaymentRequest request) {

        StripeResponse response =
                stripeClient.createPayment(...);

        return map(response);
    }
}
```

Then:

```java
@Service
public class CheckoutService {

    private final PaymentGateway paymentGateway;

    public CheckoutService(
            PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public void checkout(PaymentRequest request) {
        paymentGateway.pay(request);
    }
}
```

The architecture:

```text
                 APPLICATION
                      │
                      ▼
              CheckoutService
                      │
                      ▼
               PaymentGateway
                      ▲
                      │
             StripePaymentAdapter
                      │
                      ▼
                  Stripe SDK
```

This is an excellent practical example of Adapter + Dependency Injection + abstraction.

---

# 47. Important interview nuance

Don't say:

> "Adapter is used to hide implementation."

That's too vague.

A better statement is:

> **"Adapter converts one interface into another interface expected by the client."**

And don't say:

> "Adapter and Decorator are the same because both wrap objects."

The key difference is **purpose**:

```text
Adapter   → interface compatibility
Decorator → behavior enhancement
```

And:

```text
Adapter   → existing incompatible object
Factory   → object creation
Builder   → object construction/configuration
Singleton → instance control
```

---

# 48. Final interview cheat sheet

### Adapter

**Category:** Structural

**Intent:**

> Convert an existing interface into another interface expected by the client.

**Participants:**

```text
Client
Target
Adapter
Adaptee
```

**Common use cases:**

```text
Third-party integrations
Legacy systems
Multiple vendor APIs
Data format conversion
API normalization
Exception translation
```

**Object Adapter:**

```java
class Adapter implements Target {
    private final Adaptee adaptee;
}
```

**Class Adapter:**

```java
class Adapter extends Adaptee
              implements Target {
}
```

**Main distinction:**

```text
Adapter
→ makes things compatible
```

---

## The key comparison table

| Pattern   | Main problem it solves                       |
| --------- | -------------------------------------------- |
| Singleton | Need controlled instance count               |
| Factory   | Need controlled/encapsulated object creation |
| Builder   | Need flexible/complex object construction    |
| Adapter   | Existing interfaces don't match              |

And within Structural patterns:

```text
Adapter
→ "These interfaces don't match."

Decorator
→ "I want to add behavior without changing the interface."
```

That distinction will become especially important in the next pattern.

Say **`next`** when you're ready for **Decorator**.

