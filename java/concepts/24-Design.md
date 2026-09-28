## 25. Java Design & Architecture Concepts

Now we move from **solving individual coding problems** to designing Java code that is maintainable, extensible, and testable.

You mentioned earlier that you'd come back to **SOLID and Spring Boot**, so I’ll keep Spring-specific details for later and focus first on the Java-side design principles that make SOLID easier to understand.

---

# 1. What Does "Good Design" Mean?

Suppose you have this:

```java
class OrderService {

    public void createOrder(Order order) {
        // validate order
        // calculate price
        // save to database
        // send email
        // generate invoice
    }
}
```

It works.

But now imagine requirements change:

* different payment methods
* different notification systems
* different invoice formats
* different database implementations

The class can quickly become huge.

Good design tries to make code:

* easy to understand
* easy to change
* easy to test
* reusable
* loosely coupled
* resistant to changes in unrelated parts

This leads directly to **SOLID**.

---

# 2. SOLID

SOLID stands for:

```text
S → Single Responsibility Principle
O → Open/Closed Principle
L → Liskov Substitution Principle
I → Interface Segregation Principle
D → Dependency Inversion Principle
```

You should know all five very well in a Java interview.

---

# 3. S — Single Responsibility Principle

> A class should have one reason to change.

A common misunderstanding is:

> "A class should have only one method."

No.

It means the class should have **one cohesive responsibility**.

---

## Bad example

```java
class EmployeeService {

    public void calculateSalary(Employee employee) {
        // ...
    }

    public void saveEmployee(Employee employee) {
        // database
    }

    public void sendEmail(Employee employee) {
        // email
    }

    public void generateReport(Employee employee) {
        // report
    }
}
```

There are several independent responsibilities:

```text
Salary calculation
Database persistence
Email
Reporting
```

Changes to any one of those areas can force changes to this class.

---

## Better

```java
class SalaryService {
    public void calculateSalary(Employee employee) {
    }
}
```

```java
class EmployeeRepository {
    public void save(Employee employee) {
    }
}
```

```java
class EmailService {
    public void sendEmail(Employee employee) {
    }
}
```

```java
class EmployeeReportService {
    public void generateReport(Employee employee) {
    }
}
```

Now each class has a more focused responsibility.

---

# 4. O — Open/Closed Principle

> Software entities should be open for extension but closed for modification.

This sounds abstract, so consider this.

### Bad design

```java
class PaymentService {

    public void pay(String type) {

        if (type.equals("CARD")) {
            // card payment
        }
        else if (type.equals("PAYPAL")) {
            // PayPal
        }
        else if (type.equals("UPI")) {
            // UPI
        }
    }
}
```

Every time you add:

```text
APPLE_PAY
BANK_TRANSFER
CRYPTO
```

you modify the existing class.

---

# 5. Use Polymorphism

Define an abstraction:

```java
interface PaymentProcessor {
    void pay(double amount);
}
```

Implementations:

```java
class CardPaymentProcessor
        implements PaymentProcessor {

    @Override
    public void pay(double amount) {
        // card payment
    }
}
```

```java
class PaypalPaymentProcessor
        implements PaymentProcessor {

    @Override
    public void pay(double amount) {
        // PayPal
    }
}
```

Now the service depends on the abstraction:

```java
class PaymentService {

    private final PaymentProcessor processor;

    PaymentService(PaymentProcessor processor) {
        this.processor = processor;
    }

    public void pay(double amount) {
        processor.pay(amount);
    }
}
```

Adding a new payment type doesn't require changing `PaymentService`.

That's the core idea behind OCP.

---

# 6. L — Liskov Substitution Principle

This is one of the most frequently misunderstood SOLID principles.

> A subtype should be usable wherever its base type is expected without breaking the correctness of the program.

Classic example:

```java
class Bird {
    void fly() {
    }
}
```

Then:

```java
class Sparrow extends Bird {
}
```

makes sense.

But:

```java
class Penguin extends Bird {
    @Override
    void fly() {
        throw new UnsupportedOperationException();
    }
}
```

is a design smell.

Why?

Because code expecting:

```java
Bird bird
```

may reasonably expect:

```java
bird.fly();
```

But `Penguin` can't satisfy that contract.

The problem isn't merely that penguins don't fly.

The abstraction itself is wrong.

---

# 7. Better Abstraction

Instead of:

```java
class Bird {
    void fly() {}
}
```

use:

```java
class Bird {
}
```

Then:

```java
interface Flyable {
    void fly();
}
```

Sparrow:

```java
class Sparrow extends Bird
        implements Flyable {

    @Override
    public void fly() {
    }
}
```

Penguin:

```java
class Penguin extends Bird {
}
```

Now the model represents the domain correctly.

---

# 8. LSP Isn't "Child Must Have Exactly Same Methods"

LSP is about **behavioral substitutability**.

A subclass shouldn't:

* violate expectations of the parent contract
* unexpectedly reject valid operations
* strengthen preconditions
* weaken guarantees
* change expected behavior in incompatible ways

This is why simply saying:

> "Child can be substituted for parent"

is incomplete.

The important part is:

> **without breaking the assumptions of code using the parent type.**

---

# 9. I — Interface Segregation Principle

> Clients should not be forced to depend on methods they don't use.

Suppose:

```java
interface Machine {

    void print();

    void scan();

    void fax();
}
```

A simple printer only needs:

```java
print()
```

But it is forced to implement:

```java
scan()
fax()
```

That can lead to:

```java
class SimplePrinter implements Machine {

    @Override
    public void print() {
    }

    @Override
    public void scan() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fax() {
        throw new UnsupportedOperationException();
    }
}
```

That's a design smell.

---

# 10. Split the Interfaces

```java
interface Printer {
    void print();
}
```

```java
interface Scanner {
    void scan();
}
```

```java
interface Fax {
    void fax();
}
```

A multifunction machine:

```java
class MultiFunctionPrinter
        implements Printer, Scanner, Fax {

    public void print() {}
    public void scan() {}
    public void fax() {}
}
```

Simple printer:

```java
class SimplePrinter
        implements Printer {

    public void print() {}
}
```

This is interface segregation.

---

# 11. D — Dependency Inversion Principle

This is particularly important for Java/Spring developers.

> High-level modules should not depend directly on low-level implementation details. Both should depend on abstractions.

Bad:

```java
class OrderService {

    private final MySqlOrderRepository repository =
            new MySqlOrderRepository();
}
```

Now `OrderService` is tightly coupled to MySQL implementation.

---

# 12. Depend on an Interface

```java
interface OrderRepository {
    void save(Order order);
}
```

Implementation:

```java
class MySqlOrderRepository
        implements OrderRepository {

    @Override
    public void save(Order order) {
        // MySQL
    }
}
```

Service:

```java
class OrderService {

    private final OrderRepository repository;

    OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public void createOrder(Order order) {
        repository.save(order);
    }
}
```

Now:

```text
OrderService
      ↓
OrderRepository
      ↑
      |
MySqlOrderRepository
```

The service doesn't care which database implementation exists.

---

# 13. DIP vs Dependency Injection

These are related but **not the same thing**.

### Dependency Inversion Principle

A **design principle**:

> Depend on abstractions rather than concrete implementations.

### Dependency Injection

A **technique** for supplying dependencies from outside.

For example:

```java
class OrderService {

    private final OrderRepository repository;

    OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

The dependency is injected through the constructor.

Spring uses dependency injection heavily, but DI isn't exclusive to Spring.

---

# 14. Constructor Injection

For Java code, constructor injection is usually preferable to field injection.

Good:

```java
class OrderService {

    private final OrderRepository repository;

    OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

Why?

### Dependency is explicit

Anyone constructing the object can see what it requires.

### Can make dependency `final`

```java
private final OrderRepository repository;
```

### Easier unit testing

```java
OrderRepository mockRepository = ...;

OrderService service =
        new OrderService(mockRepository);
```

No framework required.

---

# 15. Composition vs Inheritance

This is another very common interview question.

Inheritance:

```java
class Car extends Vehicle {
}
```

means:

```text
Car IS-A Vehicle
```

Composition:

```java
class Car {

    private Engine engine;
}
```

means:

```text
Car HAS-A Engine
```

---

# 16. Why Prefer Composition?

Suppose:

```java
class Car extends Engine {
}
```

This says:

> A Car is an Engine.

Clearly wrong.

Instead:

```java
class Car {

    private final Engine engine;

    Car(Engine engine) {
        this.engine = engine;
    }
}
```

Now:

> A Car has an Engine.

Composition allows you to change the engine implementation without changing the inheritance hierarchy.

---

# 17. Composition Makes Behavior Replaceable

For example:

```java
interface PaymentProcessor {
    void pay(double amount);
}
```

Then:

```java
class OrderService {

    private final PaymentProcessor paymentProcessor;

    OrderService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}
```

You can provide:

```text
CardPaymentProcessor
PaypalPaymentProcessor
UpiPaymentProcessor
MockPaymentProcessor
```

without changing `OrderService`.

This is a major reason dependency injection and composition work so well together.

---

# 18. High Cohesion + Low Coupling

These two terms are worth remembering.

### High cohesion

A class's responsibilities are closely related.

```text
EmployeeRepository
    ↓
Database operations related to Employee
```

Good.

### Low coupling

Classes don't unnecessarily depend on implementation details of each other.

```text
OrderService
      ↓
OrderRepository
      ↑
MySqlOrderRepository
```

Good.

Ideal design generally aims for:

> **High cohesion + low coupling.**

---

# 19. Common Design Patterns

You don't need to memorize every design pattern ever created.

For Java interviews, know these particularly well:

```text
Creational
 ├── Singleton
 ├── Factory
 └── Builder

Structural
 ├── Adapter
 └── Decorator

Behavioral
 ├── Strategy
 └── Observer
```

---

# 20. Strategy Pattern

This is one of the most useful patterns in backend Java.

Suppose:

```java
class PaymentService {

    public void pay(String type) {

        if (type.equals("CARD")) {
            // ...
        } else if (type.equals("UPI")) {
            // ...
        }
    }
}
```

Replace conditional logic with strategies.

```java
interface PaymentStrategy {
    void pay(double amount);
}
```

```java
class CardPayment
        implements PaymentStrategy {

    public void pay(double amount) {
        // card
    }
}
```

```java
class UpiPayment
        implements PaymentStrategy {

    public void pay(double amount) {
        // UPI
    }
}
```

Then:

```java
class PaymentService {

    private final PaymentStrategy strategy;

    PaymentService(PaymentStrategy strategy) {
        this.strategy = strategy;
    }

    void pay(double amount) {
        strategy.pay(amount);
    }
}
```

The algorithm can vary independently from the client.

---

# 21. Factory Pattern

Factory centralizes object creation.

Instead of:

```java
PaymentProcessor processor;

if (type.equals("CARD")) {
    processor = new CardPaymentProcessor();
} else {
    processor = new UpiPaymentProcessor();
}
```

use:

```java
class PaymentProcessorFactory {

    static PaymentProcessor create(String type) {

        return switch (type) {

            case "CARD" -> new CardPaymentProcessor();
            case "UPI" -> new UpiPaymentProcessor();

            default ->
                throw new IllegalArgumentException(
                    "Unknown payment type"
                );
        };
    }
}
```

Usage:

```java
PaymentProcessor processor =
        PaymentProcessorFactory.create("CARD");
```

Factory is primarily about:

> **Centralizing/abstracting object creation.**

---

# 22. Builder Pattern

Useful when an object has many optional fields.

Without Builder:

```java
new Employee(
    "John",
    30,
    "IT",
    "India",
    null,
    null,
    true
);
```

Hard to understand.

Builder:

```java
Employee employee =
        Employee.builder()
                .name("John")
                .age(30)
                .department("IT")
                .country("India")
                .active(true)
                .build();
```

Builder improves readability and is especially useful for objects with many optional parameters.

---

# 23. Singleton Pattern

Singleton means:

> Only one instance is intended to exist.

Basic example:

```java
public final class Singleton {

    private static final Singleton INSTANCE =
            new Singleton();

    private Singleton() {
    }

    public static Singleton getInstance() {
        return INSTANCE;
    }
}
```

Usage:

```java
Singleton instance =
        Singleton.getInstance();
```

### Interview warning

Don't blindly say:

> "Singleton is always good."

Singleton introduces global shared state and can make testing harder.

Also, in modern Java applications, frameworks often manage object lifecycles themselves, so manually implementing Singleton isn't always necessary.

---

# 24. Adapter Pattern

Suppose your application expects:

```java
interface PaymentService {
    void pay(double amount);
}
```

But an external library provides:

```java
class ThirdPartyPaymentApi {

    void makePayment(double value) {
    }
}
```

You can adapt it:

```java
class PaymentAdapter
        implements PaymentService {

    private final ThirdPartyPaymentApi api;

    PaymentAdapter(ThirdPartyPaymentApi api) {
        this.api = api;
    }

    @Override
    public void pay(double amount) {
        api.makePayment(amount);
    }
}
```

Now your application doesn't need to know the third-party API's interface.

Adapter means:

> **Make incompatible interfaces work together.**

---

# 25. Decorator Pattern

Decorator adds behavior without modifying the original class.

Example:

```java
interface Coffee {
    double cost();
}
```

Base:

```java
class SimpleCoffee implements Coffee {

    public double cost() {
        return 5;
    }
}
```

Decorator:

```java
class MilkDecorator implements Coffee {

    private final Coffee coffee;

    MilkDecorator(Coffee coffee) {
        this.coffee = coffee;
    }

    public double cost() {
        return coffee.cost() + 1;
    }
}
```

Usage:

```java
Coffee coffee =
        new MilkDecorator(
            new SimpleCoffee()
        );
```

You can stack decorators:

```text
SimpleCoffee
      ↓
Milk
      ↓
Sugar
      ↓
WhippedCream
```

---

# 26. Strategy vs Factory

Common interview question.

### Strategy

Answers:

> **Which behavior/algorithm should I use?**

```text
PaymentStrategy
 ├── Card
 ├── UPI
 └── PayPal
```

### Factory

Answers:

> **Which object should I create?**

```text
PaymentProcessorFactory
        ↓
CardPaymentProcessor
```

They can also be used together.

---

# 27. Adapter vs Decorator

Another common question.

### Adapter

Changes the **interface**.

```text
Old Interface
      ↓
   Adapter
      ↓
Expected Interface
```

### Decorator

Keeps the same interface but **adds behavior**.

```text
Coffee
 ↓
MilkDecorator
 ↓
SugarDecorator
```

A concise interview answer:

> Adapter makes incompatible interfaces compatible; Decorator adds behavior while preserving the interface.

---

# 28. A Strong Design Interview Thought Process

Suppose the interviewer asks:

> "Design a notification system supporting email, SMS and push notifications."

Don't immediately create three `if` statements.

Start:

```text
What varies?
        ↓
Notification mechanism
        ↓
Create abstraction
        ↓
NotificationSender
        ↓
EmailSender
SmsSender
PushSender
```

Then:

```text
How does NotificationService receive it?
        ↓
Dependency Injection
```

Then ask:

```text
How are implementations selected?
        ↓
Factory / Strategy / registry
```

Now you're thinking architecturally rather than just writing code.

---

# 29. The Most Important Relationships

Memorize these:

```text
SOLID
 ↓
Good object-oriented design
 ↓
Low coupling + high cohesion
 ↓
Interfaces / abstractions
 ↓
Composition
 ↓
Dependency Injection
 ↓
Testable code
```

And:

```text
Strategy
    → varying behavior

Factory
    → object creation

Builder
    → complex object construction

Adapter
    → incompatible interfaces

Decorator
    → add behavior

Singleton
    → controlled single instance
```

---

# 30. Interview Questions You Should Be Ready For

### SOLID

1. Explain all five SOLID principles.
2. Give a real-world example of SRP.
3. What is the difference between OCP and DIP?
4. Explain LSP with the Rectangle/Square problem.
5. Why is interface segregation useful?
6. DIP vs Dependency Injection?

### OOP Design

7. Composition vs inheritance?
8. Why prefer composition in many cases?
9. Interface vs abstract class?
10. What is loose coupling?
11. What is high cohesion?

### Design Patterns

12. Strategy vs Factory?
13. Factory vs Abstract Factory?
14. Adapter vs Decorator?
15. When would you use Builder?
16. Problems with Singleton?
17. How would you make Singleton thread-safe?

---

## One especially important point

Don't treat SOLID as five independent definitions.

A good design often uses several together.

For example:

```java
interface PaymentProcessor {
    void pay(double amount);
}
```

```java
class PaymentService {

    private final PaymentProcessor processor;

    PaymentService(PaymentProcessor processor) {
        this.processor = processor;
    }
}
```

This demonstrates:

* **DIP** — service depends on abstraction
* **OCP** — new processors can be added
* **LSP** — implementations can substitute the interface
* **ISP** — interface is focused
* **SRP** — payment service isn't responsible for implementing every payment mechanism

That's the level of understanding interviewers usually want.

---

### Next

**Java Concurrency — Advanced Topics:** `synchronized`, `volatile`, `AtomicInteger`, locks, deadlocks, thread pools and `CompletableFuture` are already covered. The next step is to go deeper into **concurrent collections (`ConcurrentHashMap`, CopyOnWriteArrayList, BlockingQueue), locks/conditions, and practical producer-consumer patterns**, which are common backend interview questions.

