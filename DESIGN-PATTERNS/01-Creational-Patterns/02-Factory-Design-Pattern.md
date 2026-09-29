# 2. Factory Design Pattern

The **Factory Pattern** is one of the most commonly discussed creational patterns in Java interviews.

Its central idea is simple:

> **Move object-creation logic away from the client, so the client doesn't need to know exactly which concrete class to instantiate.**

The interesting part is that there are several patterns commonly called “Factory,” so we need to distinguish them carefully.

---

# 1. Start with the problem

Suppose we have:

```java
public interface Notification {
    void send(String message);
}
```

Implementations:

```java
public class EmailNotification implements Notification {

    @Override
    public void send(String message) {
        System.out.println("Sending EMAIL: " + message);
    }
}
```

```java
public class SmsNotification implements Notification {

    @Override
    public void send(String message) {
        System.out.println("Sending SMS: " + message);
    }
}
```

```java
public class PushNotification implements Notification {

    @Override
    public void send(String message) {
        System.out.println("Sending PUSH: " + message);
    }
}
```

Now imagine the client:

```java
public class NotificationService {

    public void notify(String type, String message) {

        Notification notification;

        if ("EMAIL".equals(type)) {
            notification = new EmailNotification();
        } else if ("SMS".equals(type)) {
            notification = new SmsNotification();
        } else if ("PUSH".equals(type)) {
            notification = new PushNotification();
        } else {
            throw new IllegalArgumentException("Unknown notification type");
        }

        notification.send(message);
    }
}
```

It works.

But notice what `NotificationService` knows.

It knows:

```text
EmailNotification
SmsNotification
PushNotification
```

It also knows:

```text
how to create each object
```

and:

```text
which condition corresponds to which implementation
```

That creates **tight coupling**.

---

# 2. What problem is Factory solving?

We want the client to say:

```java
Notification notification = ...;
notification.send(message);
```

without worrying about:

```java
new EmailNotification()
new SmsNotification()
new PushNotification()
```

So we separate:

```text
Object usage
     from
Object creation
```

That is the key idea.

---

# 3. Basic concept

Without Factory:

```text
Client
  │
  ├── new EmailNotification()
  ├── new SmsNotification()
  └── new PushNotification()
```

With Factory:

```text
Client
  │
  ▼
Factory
  │
  ├── EmailNotification
  ├── SmsNotification
  └── PushNotification
```

The client depends primarily on the abstraction:

```java
Notification
```

rather than the concrete implementation.

---

# 4. Simple Factory

The simplest form is often called **Simple Factory**.

Important interview nuance:

> **Simple Factory is not itself one of the original GoF 23 patterns.**

It's a commonly used technique based on encapsulating creation.

Let's implement it.

```java
public class NotificationFactory {

    public static Notification createNotification(String type) {

        if ("EMAIL".equals(type)) {
            return new EmailNotification();
        }

        if ("SMS".equals(type)) {
            return new SmsNotification();
        }

        if ("PUSH".equals(type)) {
            return new PushNotification();
        }

        throw new IllegalArgumentException(
                "Unknown notification type: " + type
        );
    }
}
```

Now the client becomes:

```java
public class NotificationService {

    public void notify(String type, String message) {

        Notification notification =
                NotificationFactory.createNotification(type);

        notification.send(message);
    }
}
```

The client no longer contains:

```java
new EmailNotification()
new SmsNotification()
new PushNotification()
```

Creation logic has been centralized.

---

# 5. Why is this better?

Compare the two designs.

### Before

```text
NotificationService
    │
    ├── knows EmailNotification
    ├── knows SmsNotification
    ├── knows PushNotification
    └── creates objects
```

### After

```text
NotificationService
    │
    ▼
NotificationFactory
    │
    ├── creates EmailNotification
    ├── creates SmsNotification
    └── creates PushNotification
```

Now `NotificationService` focuses on:

> "I need a notification."

The factory focuses on:

> "Which notification object should I create?"

That separation is valuable.

---

# 6. What exactly does "Factory" mean?

This is where interviews get interesting.

There are three commonly encountered concepts:

```text
Factory
├── Simple Factory
├── Factory Method
└── Abstract Factory
```

They solve related but different problems.

---

# 7. Factory Method Pattern

The **Factory Method** is the actual GoF pattern.

Its intent is roughly:

> **Define a method for creating an object, but let subclasses decide which concrete object gets created.**

This is different from the Simple Factory.

Let's build it.

---

# 8. Example: Vehicle Factory

Suppose:

```java
public interface Vehicle {
    void drive();
}
```

Implementations:

```java
public class Car implements Vehicle {

    @Override
    public void drive() {
        System.out.println("Driving car");
    }
}
```

```java
public class Bike implements Vehicle {

    @Override
    public void drive() {
        System.out.println("Riding bike");
    }
}
```

Now create an abstract creator:

```java
public abstract class VehicleFactory {

    public abstract Vehicle createVehicle();
}
```

Concrete factories:

```java
public class CarFactory extends VehicleFactory {

    @Override
    public Vehicle createVehicle() {
        return new Car();
    }
}
```

```java
public class BikeFactory extends VehicleFactory {

    @Override
    public Vehicle createVehicle() {
        return new Bike();
    }
}
```

Client:

```java
VehicleFactory factory = new CarFactory();

Vehicle vehicle = factory.createVehicle();

vehicle.drive();
```

Output:

```text
Driving car
```

Notice something important.

The client doesn't write:

```java
new Car()
```

Instead:

```java
VehicleFactory factory = new CarFactory();
Vehicle vehicle = factory.createVehicle();
```

The concrete factory determines what gets constructed.

---

# 9. Why is that called "Factory Method"?

Because:

```java
createVehicle()
```

is the **factory method**.

The base class declares it:

```java
public abstract Vehicle createVehicle();
```

and subclasses provide the actual creation behavior.

```text
VehicleFactory
       │
       │ createVehicle()
       │
 ┌─────┴──────┐
 ▼            ▼
CarFactory  BikeFactory
     │          │
     ▼          ▼
    Car        Bike
```

---

# 10. Simple Factory vs Factory Method

This is a very common interview question.

### Simple Factory

Usually one class decides:

```java
if (type.equals("CAR")) {
    return new Car();
}
```

```text
One factory
   │
   ├── Car
   ├── Bike
   └── Truck
```

### Factory Method

Different factory subclasses decide what to create:

```text
VehicleFactory
      │
 ┌────┴─────┐
 ▼          ▼
CarFactory  BikeFactory
   │           │
   ▼           ▼
  Car         Bike
```

So the structural difference is significant.

---

# 11. Why use Factory Method?

Suppose you're building a document application.

You support:

```text
PDF
Word
Excel
```

A naive implementation might be:

```java
if (type.equals("PDF")) {
    return new PdfDocument();
} else if (type.equals("WORD")) {
    return new WordDocument();
} else if (type.equals("EXCEL")) {
    return new ExcelDocument();
}
```

As the application grows:

```text
PDF
Word
Excel
PowerPoint
CSV
HTML
...
```

the creation logic can become increasingly centralized and difficult to maintain.

Factory Method lets different creators own their creation decisions.

This can be especially useful when subclasses have different creation behavior.

---

# 12. Factory Method with business logic

Factory Method doesn't have to be just:

```java
return new Something();
```

The creator can have common processing.

For example:

```java
public abstract class ReportService {

    public void generateReport() {

        Report report = createReport();

        report.prepare();
        report.generate();
        report.export();
    }

    protected abstract Report createReport();
}
```

Then:

```java
public class PdfReportService extends ReportService {

    @Override
    protected Report createReport() {
        return new PdfReport();
    }
}
```

```java
public class ExcelReportService extends ReportService {

    @Override
    protected Report createReport() {
        return new ExcelReport();
    }
}
```

This is a more realistic example of Factory Method.

The parent controls the workflow:

```text
generateReport()
    │
    ├── prepare()
    ├── generate()
    └── export()
```

while subclasses control:

```text
which Report object is created
```

---

# 13. Relationship with Template Method

This leads to an important observation.

Factory Method is often used **inside** a workflow similar to the Template Method pattern.

For example:

```java
public abstract class ReportService {

    public final void generateReport() {
        Report report = createReport();
        report.prepare();
        report.generate();
    }

    protected abstract Report createReport();
}
```

The overall algorithm is controlled by the parent.

Creation is delegated to subclasses.

So Factory Method often works together with other patterns.

This is useful to recognize in interviews.

---

# 14. Abstract Factory

Now let's move one level higher.

Suppose we aren't creating just one type of object.

We have **families of related objects**.

For example, a UI library.

We have:

```text
Button
Checkbox
```

and two visual themes:

```text
Windows
Mac
```

We could have:

```text
WindowsButton
WindowsCheckbox

MacButton
MacCheckbox
```

Now we need a factory that creates **related objects belonging to the same family**.

That's where **Abstract Factory** comes in.

---

# 15. Abstract Factory example

First, abstractions:

```java
public interface Button {
    void render();
}
```

```java
public interface Checkbox {
    void render();
}
```

Windows implementations:

```java
public class WindowsButton implements Button {

    @Override
    public void render() {
        System.out.println("Windows button");
    }
}
```

```java
public class WindowsCheckbox implements Checkbox {

    @Override
    public void render() {
        System.out.println("Windows checkbox");
    }
}
```

Mac implementations:

```java
public class MacButton implements Button {

    @Override
    public void render() {
        System.out.println("Mac button");
    }
}
```

```java
public class MacCheckbox implements Checkbox {

    @Override
    public void render() {
        System.out.println("Mac checkbox");
    }
}
```

Now the factory:

```java
public interface GUIFactory {

    Button createButton();

    Checkbox createCheckbox();
}
```

Windows factory:

```java
public class WindowsFactory implements GUIFactory {

    @Override
    public Button createButton() {
        return new WindowsButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new WindowsCheckbox();
    }
}
```

Mac factory:

```java
public class MacFactory implements GUIFactory {

    @Override
    public Button createButton() {
        return new MacButton();
    }

    @Override
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }
}
```

Now the client can do:

```java
GUIFactory factory = new WindowsFactory();

Button button = factory.createButton();
Checkbox checkbox = factory.createCheckbox();
```

Both objects belong to the same product family:

```text
Windows
 ├── WindowsButton
 └── WindowsCheckbox
```

---

# 16. What problem does Abstract Factory solve?

Imagine accidentally doing:

```java
GUIFactory factory = new WindowsFactory();

Button button = factory.createButton();
Checkbox checkbox = new MacCheckbox();
```

Now we have:

```text
Windows button
Mac checkbox
```

Potentially an inconsistent UI.

The Abstract Factory encourages creating related products from the same factory:

```text
             GUIFactory
                 │
        ┌────────┴────────┐
        ▼                 ▼
 WindowsFactory       MacFactory
      │                   │
  ┌───┴───┐           ┌───┴───┐
  ▼       ▼           ▼       ▼
Button Checkbox      Button Checkbox
```

The factory represents an entire product family.

---

# 17. Simple Factory vs Factory Method vs Abstract Factory

This distinction is worth memorizing.

| Pattern          | Main idea                                  |
| ---------------- | ------------------------------------------ |
| Simple Factory   | One factory decides which object to create |
| Factory Method   | Subclasses decide which object to create   |
| Abstract Factory | Creates families of related objects        |

Think:

```text
Simple Factory
    ↓
"Which one object?"

Factory Method
    ↓
"Let subclasses decide the object."

Abstract Factory
    ↓
"Give me a matching family of objects."
```

---

# 18. Factory and Open/Closed Principle

Factory can help with the **Open/Closed Principle**.

Suppose:

```java
if ("EMAIL".equals(type)) {
    return new EmailNotification();
}
```

and later:

```text
WhatsApp
Slack
Teams
```

are added.

A central factory may still need modification.

So simply saying:

> "Factory automatically follows Open/Closed Principle"

would be too simplistic.

The real benefit depends on the specific factory design.

With Factory Method, adding a new product can often involve creating a new creator subclass rather than modifying the existing creator implementation.

However, even Factory Method can require changes elsewhere depending on how the system selects the concrete factory.

This nuance is useful in interviews.

---

# 19. Factory and Dependency Inversion

Factory often helps reduce direct dependence on concrete classes.

Instead of:

```java
EmailNotification notification =
        new EmailNotification();
```

we use:

```java
Notification notification =
        factory.createNotification();
```

The client talks to:

```java
Notification
```

rather than:

```java
EmailNotification
```

So creation and usage become more decoupled.

But remember:

> Factory does not magically eliminate all dependencies.

Somewhere, something still has to know which concrete implementation to construct.

The factory becomes that location.

---

# 20. Factory and Dependency Injection

This is especially important given your Spring Boot preparation.

These two ideas are related but **not identical**.

### Factory

You explicitly ask for an object:

```java
Notification notification =
        factory.createNotification("EMAIL");
```

### Dependency Injection

The framework supplies the object:

```java
@Service
public class NotificationService {

    private final Notification notification;

    public NotificationService(Notification notification) {
        this.notification = notification;
    }
}
```

Spring handles creation and wiring.

So you can think of DI as a broader object-creation/dependency-management mechanism.

---

# 21. Factory in Spring

Spring internally performs a huge amount of object creation and dependency wiring.

For example:

```java
@Service
public class PaymentService {
}
```

You don't write:

```java
PaymentService service =
        new PaymentService();
```

Instead, Spring's container creates and manages it.

Likewise:

```java
@Autowired
private PaymentService paymentService;
```

The framework determines how the dependency should be obtained.

A very important interview statement is:

> **Spring's `BeanFactory` and `ApplicationContext` are central abstractions for managing and obtaining beans.**

So "Factory" isn't merely a textbook concept; factories are deeply connected with IoC containers.

---

# 22. A better Simple Factory implementation

Instead of a chain of `if` statements:

```java
if ("EMAIL".equals(type)) {
    return new EmailNotification();
}
```

we could use a map:

```java
public class NotificationFactory {

    private final Map<String, Supplier<Notification>> creators =
            Map.of(
                    "EMAIL", EmailNotification::new,
                    "SMS", SmsNotification::new,
                    "PUSH", PushNotification::new
            );

    public Notification create(String type) {

        Supplier<Notification> creator = creators.get(type);

        if (creator == null) {
            throw new IllegalArgumentException(
                    "Unknown notification type: " + type
            );
        }

        return creator.get();
    }
}
```

Usage:

```java
Notification notification =
        factory.create("EMAIL");
```

This can make creation logic more extensible.

---

# 23. Why `Supplier<Notification>`?

This:

```java
EmailNotification::new
```

is a constructor reference.

Conceptually:

```java
() -> new EmailNotification()
```

and it matches:

```java
Supplier<Notification>
```

which represents something that can produce a value without arguments.

So:

```java
"EMAIL" → EmailNotification::new
```

means:

> When EMAIL is requested, use this constructor to create the object.

---

# 24. Factory with constructor parameters

Factories become even more useful when object construction is complicated.

Suppose:

```java
public class EmailNotification implements Notification {

    private final String host;
    private final int port;

    public EmailNotification(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void send(String message) {
        // ...
    }
}
```

The client shouldn't have to know:

```java
new EmailNotification(host, port)
```

and perhaps also:

```text
validate host
load configuration
choose credentials
configure connection
initialize dependencies
```

The factory can encapsulate those details.

That's one of the strongest practical reasons to use a factory.

---

# 25. Factory versus Builder

You will later study **Builder**, so keep this distinction in mind.

### Factory

Answers:

> **Which object should I create?**

Example:

```java
VehicleFactory.create("CAR");
```

### Builder

Answers:

> **How should I construct this particular object step by step?**

Example:

```java
User.builder()
    .name("John")
    .age(30)
    .email("...")
    .build();
```

Conceptually:

```text
Factory
   ↓
Choose product

Builder
   ↓
Construct/configure product
```

They can also be combined.

---

# 26. Factory versus Singleton

These patterns solve completely different problems.

### Singleton

Controls:

```text
HOW MANY INSTANCES?
```

Answer:

```text
One
```

### Factory

Controls:

```text
WHICH INSTANCE / IMPLEMENTATION TO CREATE?
```

Answer:

```text
Based on some creation logic
```

For example, you could have:

```text
Singleton Factory
```

where the factory itself is a Singleton.

Patterns can therefore coexist.

---

# 27. Real-world example: Payment system

Imagine:

```java
public interface PaymentProcessor {
    void pay(double amount);
}
```

Implementations:

```text
CreditCardProcessor
PayPalProcessor
UPIProcessor
BankTransferProcessor
```

Without Factory:

```java
if (type.equals("CARD")) {
    processor = new CreditCardProcessor();
} else if (type.equals("PAYPAL")) {
    processor = new PayPalProcessor();
} else if (type.equals("UPI")) {
    processor = new UPIProcessor();
}
```

With Factory:

```java
PaymentProcessor processor =
        paymentProcessorFactory.create(type);
```

Then:

```java
processor.pay(amount);
```

The business logic doesn't care which concrete implementation it received.

That is a very realistic use case.

---

# 28. A more advanced example

Suppose we have:

```java
public interface PaymentProcessor {
    void pay(double amount);
}
```

Factory:

```java
public interface PaymentProcessorFactory {
    PaymentProcessor create();
}
```

Factories:

```java
public class UpiPaymentFactory
        implements PaymentProcessorFactory {

    @Override
    public PaymentProcessor create() {
        return new UpiPaymentProcessor();
    }
}
```

```java
public class CardPaymentFactory
        implements PaymentProcessorFactory {

    @Override
    public PaymentProcessor create() {
        return new CardPaymentProcessor();
    }
}
```

Client:

```java
public class PaymentService {

    private final PaymentProcessorFactory factory;

    public PaymentService(PaymentProcessorFactory factory) {
        this.factory = factory;
    }

    public void process(double amount) {

        PaymentProcessor processor =
                factory.create();

        processor.pay(amount);
    }
}
```

Now the service doesn't know:

```text
UPI
CARD
PAYPAL
```

It only knows:

```text
PaymentProcessorFactory
```

This is much more loosely coupled.

---

# 29. Factory does not necessarily mean inheritance

Another important nuance.

People sometimes assume:

> "Factory Pattern means an abstract class and subclasses."

Not always.

That applies specifically to a classic **Factory Method** implementation.

A Simple Factory can just be:

```java
public class Factory {

    public static Product create(String type) {
        // ...
    }
}
```

No inheritance is required.

Abstract Factory can use interfaces:

```java
public interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
}
```

So don't memorize:

```text
Factory = inheritance
```

Instead memorize:

```text
Factory = encapsulated object creation
```

The exact structure depends on the factory variant.

---

# 30. Common mistake: Factory with one trivial object

Suppose we write:

```java
public class UserFactory {

    public User create() {
        return new User();
    }
}
```

and that's literally all it does.

Then you have:

```text
Client → Factory → new User()
```

with no meaningful decision or encapsulated complexity.

That may add unnecessary abstraction.

Not every `new` needs a factory.

A factory is useful when object creation has:

```text
selection logic
complex construction
configuration
conditional behavior
product families
decoupling requirements
```

---

# 31. Common interview question: "Why not just use `new`?"

Excellent question.

For simple objects:

```java
User user = new User();
```

is perfectly fine.

A factory becomes useful when direct construction causes the client to become coupled to:

```text
specific implementations
complex initialization
creation rules
multiple variants
```

For example:

```java
PaymentProcessor processor =
        PaymentProcessorFactory.create(paymentMethod);
```

The caller doesn't need to know how that processor is built.

---

# 32. Common interview question: "Does Factory create only one object?"

No.

A factory may create:

```text
0 objects
1 object
many objects
```

depending on the design.

For example:

```java
factory.create("EMAIL");
factory.create("SMS");
factory.create("PUSH");
```

may create a new object each time.

Factory is **not** about limiting object count.

That's Singleton's concern.

---

# 33. Common interview question: "Does Factory always return an interface?"

No, but it **often does**.

For example:

```java
public Notification createNotification(...)
```

returns the abstraction.

This is useful because the client can work with:

```java
Notification
```

rather than a concrete implementation.

But technically, a factory can return:

```java
ConcreteClass
```

as well.

---

# 34. Common interview question: "What is the benefit of a factory?"

A strong answer:

> "It encapsulates object-creation logic and reduces coupling between clients and concrete implementations. This is especially useful when creation involves choosing among implementations, complex initialization, or families of related products."

That's a much better answer than:

> "Factory is used to create objects."

---

# 35. Common interview question: "Factory is related to which SOLID principle?"

There isn't a single mandatory answer.

Factory often supports:

### Single Responsibility Principle

Creation responsibilities can be moved out of business classes.

Instead of:

```text
PaymentService
 ├── payment logic
 ├── validation
 └── object creation logic
```

we can separate creation:

```text
PaymentService
 └── payment logic

PaymentFactory
 └── creation logic
```

### Dependency Inversion Principle

Clients can depend on:

```java
PaymentProcessor
```

rather than concrete classes.

### Open/Closed Principle

Depending on the design, adding new products can be done without modifying existing client logic.

Be careful not to claim that simply introducing a factory automatically guarantees OCP.

---

# 36. Factory and polymorphism

Factory works especially well with polymorphism.

Suppose:

```java
Notification
```

has:

```text
EmailNotification
SmsNotification
PushNotification
```

The client writes:

```java
Notification notification =
        factory.create(type);

notification.send(message);
```

The actual implementation is chosen at runtime.

Then polymorphism handles behavior:

```text
                 Notification
                      ▲
          ┌───────────┼───────────┐
          │           │           │
        Email        SMS         Push
```

Factory selects the object.

Polymorphism determines which implementation of:

```java
send()
```

runs.

---

# 37. Factory + Strategy

Since **Strategy** is one of your upcoming patterns, this connection is very useful.

Imagine:

```text
Payment method
```

needs to be chosen dynamically.

Factory:

```text
payment type
    ↓
Factory
    ↓
PaymentStrategy
```

Then Strategy:

```text
PaymentStrategy
    ├── CardStrategy
    ├── UpiStrategy
    └── PayPalStrategy
```

So:

```text
Factory = chooses strategy object
Strategy = defines interchangeable behavior
```

Factories and Strategies are frequently used together.

---

# 38. Factory + Spring

Spring can make this even cleaner.

For example:

```java
public interface PaymentProcessor {
    void pay(double amount);
}
```

Implementations:

```java
@Component("UPI")
public class UpiPaymentProcessor
        implements PaymentProcessor {
}
```

```java
@Component("CARD")
public class CardPaymentProcessor
        implements PaymentProcessor {
}
```

Then Spring can provide a map of implementations:

```java
@Component
public class PaymentProcessorFactory {

    private final Map<String, PaymentProcessor> processors;

    public PaymentProcessorFactory(
            Map<String, PaymentProcessor> processors) {
        this.processors = processors;
    }

    public PaymentProcessor get(String type) {
        PaymentProcessor processor = processors.get(type);

        if (processor == null) {
            throw new IllegalArgumentException(
                    "Unsupported payment type: " + type
            );
        }

        return processor;
    }
}
```

Now:

```java
PaymentProcessor processor =
        factory.get("UPI");

processor.pay(1000);
```

This is a very realistic Spring Boot architecture.

---

# 39. Factory isn't necessarily a design-pattern "replacement" for `new`

This is an important mindset.

Don't think:

```text
new = bad
factory = good
```

That's incorrect.

Think:

```text
new
↓
fine for straightforward creation

Factory
↓
useful when creation itself is a design concern
```

---

# 40. Factory pattern mental model

Keep this picture in your head:

```text
                CLIENT
                   │
                   │ "I need a product"
                   ▼
                FACTORY
                   │
          ┌────────┼────────┐
          ▼        ▼        ▼
       ProductA ProductB ProductC
```

The client knows:

```text
Product abstraction
Factory
```

The factory knows:

```text
Concrete implementations
Creation rules
```

That separation is the core value.

---

# 41. Factory Pattern cheat sheet

### Simple Factory

```java
Notification n =
        NotificationFactory.create("EMAIL");
```

One factory decides what to create.

---

### Factory Method

```java
VehicleFactory factory =
        new CarFactory();

Vehicle v =
        factory.createVehicle();
```

Subclass decides what to create.

---

### Abstract Factory

```java
GUIFactory factory =
        new WindowsFactory();

Button button =
        factory.createButton();

Checkbox checkbox =
        factory.createCheckbox();
```

Creates a **family of related objects**.

---

# 42. The hierarchy to remember

```text
FACTORY CONCEPT
│
├── Simple Factory
│      └── Centralized object selection/creation
│
├── Factory Method
│      └── Subclasses choose the concrete product
│
└── Abstract Factory
       └── Creates related product families
```

And the fundamental question each answers:

```text
Simple Factory
→ Which object?

Factory Method
→ Which subclass gets to decide the object?

Abstract Factory
→ Which compatible family of objects?
```

---

# 43. Interview-ready answer

When an interviewer asks:

> **"Explain Factory Pattern."**

A strong response is:

> "Factory is a creational design approach that encapsulates object creation and keeps clients from being tightly coupled to concrete implementations. Instead of directly constructing different implementations with `new`, the client asks a factory for the required abstraction. In practice, people commonly refer to Simple Factory, Factory Method, and Abstract Factory. Simple Factory centralizes creation logic, Factory Method lets subclasses decide which product to create, and Abstract Factory creates families of related products."

Then give your example:

```java
Notification notification =
        notificationFactory.create("EMAIL");

notification.send(message);
```

And explain:

> "The caller only depends on `Notification`; the creation decision is encapsulated by the factory."

That is interview-quality.

---

# 44. What you should master before moving on

You should be able to explain these without memorizing code:

```text
1. What problem does Factory solve?
2. Why is direct `new` sometimes undesirable?
3. What is Simple Factory?
4. What is Factory Method?
5. What is Abstract Factory?
6. Simple Factory vs Factory Method
7. Factory Method vs Abstract Factory
8. Factory vs Builder
9. Factory vs Singleton
10. Factory + Strategy
11. Factory and Dependency Injection
12. How Spring uses factory/container concepts
13. Why Factory does not automatically guarantee OCP
14. When a factory is unnecessary
```

The most important distinction to lock in is:

```text
                   OBJECT CREATION
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
  Simple Factory   Factory Method   Abstract Factory
        │                │                │
   one factory      subclasses       object family
   chooses          choose           is created
   product          product
```

When you're comfortable with Factory, say **`next`** and we'll go deeply into **Builder Pattern**.

