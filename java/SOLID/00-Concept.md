# SOLID Principles — Revision Notes

A good way to remember the five:

```text
S → Responsibility
O → Extension
L → Substitution
I → Interfaces
D → Dependencies
```

And the core questions:

```text
S → Does this class have one reason to change?
O → Can I add new behavior without changing stable code?
L → Can a subtype safely replace its parent?
I → Is the client forced to depend on things it doesn't need?
D → Does high-level code depend on abstractions instead of details?
```

---

# 1. S — Single Responsibility Principle (SRP)

### Definition

> **A class should have one reason to change.**

Not:

> “A class should have only one method.”

The methods should belong to one **cohesive responsibility**.

### ❌ Bad

```java
public class InvoiceService {

    public void createInvoice() {
    }

    public void saveToDatabase() {
    }

    public void generatePdf() {
    }

    public void sendEmail() {
    }
}
```

This class can change because of:

```text
Invoice business rules
Database changes
PDF changes
Email changes
```

Multiple reasons to change.

### ✅ Better

```java
public class InvoiceService {
    public void createInvoice() {
    }
}

public class InvoiceRepository {
    public void save(Invoice invoice) {
    }
}

public class InvoicePdfGenerator {
    public byte[] generate(Invoice invoice) {
        return new byte[0];
    }
}

public class InvoiceEmailService {
    public void send(Invoice invoice) {
    }
}
```

### Benefits

```text
✓ High cohesion
✓ Easier testing
✓ Easier maintenance
✓ Safer changes
✓ Smaller classes
✓ Reduced coupling
```

### Interview phrase

> **“SRP is about one reason to change, not one method per class.”**

### Recognize SRP violation

Look for:

```text
Large class
Unrelated methods
Database + business + email + reporting in one class
Multiple independent stakeholders
```

---

# 2. O — Open/Closed Principle (OCP)

### Definition

> **Software should be open for extension but closed for modification.**

Meaning:

```text
New behavior → add/extend
Stable behavior → don't repeatedly modify
```

### ❌ Bad

```java
public class PaymentService {

    public void pay(String type, double amount) {

        if ("CARD".equals(type)) {
            // card
        } else if ("UPI".equals(type)) {
            // UPI
        } else if ("PAYPAL".equals(type)) {
            // PayPal
        }
    }
}
```

Adding Stripe means modifying `PaymentService`.

Then adding:

```text
Razorpay
Apple Pay
Google Pay
```

means modifying it again.

### ✅ Better

```java
public interface PaymentProcessor {
    void pay(double amount);
}
```

```java
public class CardPaymentProcessor
        implements PaymentProcessor {

    public void pay(double amount) {
        // card
    }
}
```

```java
public class UpiPaymentProcessor
        implements PaymentProcessor {

    public void pay(double amount) {
        // UPI
    }
}
```

```java
public class PaymentService {

    private final PaymentProcessor processor;

    public PaymentService(PaymentProcessor processor) {
        this.processor = processor;
    }

    public void pay(double amount) {
        processor.pay(amount);
    }
}
```

Now add:

```java
public class StripePaymentProcessor
        implements PaymentProcessor {

    public void pay(double amount) {
        // Stripe
    }
}
```

No change to `PaymentService`.

### Benefits

```text
✓ Easy extension
✓ Lower regression risk
✓ Less modification of stable code
✓ Better maintainability
✓ Works well with Strategy + polymorphism
```

### Important

`if/else` is **not automatically** an OCP violation.

Ask:

> **“Will this conditional keep growing as new variants are added?”**

### Interview phrase

> **“I identify behavior that is likely to vary and put it behind an abstraction, then add new implementations rather than repeatedly modifying stable code.”**

---

# 3. L — Liskov Substitution Principle (LSP)

### Definition

> **Subtypes should be replaceable for their parent types without breaking program correctness.**

In Java:

```java
Parent p = new Child();
```

The client should be able to use `p` according to the parent's contract.

---

### ❌ Classic example

```java
public class Bird {

    public void fly() {
        System.out.println("Flying");
    }
}
```

```java
public class Penguin extends Bird {

    @Override
    public void fly() {
        throw new UnsupportedOperationException();
    }
}
```

Then:

```java
Bird bird = new Penguin();
bird.fly();   // breaks
```

The problem is not that Penguin is a bird.

The problem is:

> `Bird` promises `fly()`, but Penguin cannot fulfill that contract.

### ✅ Better

```java
public class Bird {
}

public interface Flyable {
    void fly();
}
```

```java
public class Sparrow
        extends Bird
        implements Flyable {

    public void fly() {
    }
}
```

```java
public class Penguin extends Bird {
}
```

---

## Another classic: Square/Rectangle

```java
Rectangle rectangle = new Square();

rectangle.setWidth(5);
rectangle.setHeight(10);
```

A normal rectangle can be:

```text
5 × 10 = 50
```

But a square must keep:

```text
width == height
```

So setting one dimension changes the other.

The subclass changes behavior the client expected from `Rectangle`.

Therefore the abstraction is problematic.

---

## LSP rules to remember

A subtype should:

```text
✓ Honor parent's contract
✓ Accept at least what parent accepts
✓ Provide at least what parent promises
✓ Preserve important invariants
```

Avoid:

```text
✗ UnsupportedOperationException for promised behavior
✗ Stricter validation in child
✗ Weaker results/guarantees
✗ Breaking parent invariants
✗ Clients needing instanceof checks
```

### Interview phrase

> **“LSP is about behavioral substitutability, not simply whether Java allows the inheritance relationship.”**

---

# 4. I — Interface Segregation Principle (ISP)

### Definition

> **Clients should not be forced to depend on methods they don't use.**

Think:

> **Prefer focused interfaces over fat interfaces.**

---

### ❌ Bad

```java
public interface Machine {

    void print();
    void scan();
    void fax();
}
```

Simple printer:

```java
public class SimplePrinter
        implements Machine {

    public void print() {
    }

    public void scan() {
        throw new UnsupportedOperationException();
    }

    public void fax() {
        throw new UnsupportedOperationException();
    }
}
```

Why should a simple printer implement:

```text
scan()
fax()
```

?

It shouldn't.

---

### ✅ Better

```java
public interface Printer {
    void print();
}
```

```java
public interface Scanner {
    void scan();
}
```

```java
public interface Fax {
    void fax();
}
```

Simple printer:

```java
public class SimplePrinter
        implements Printer {

    public void print() {
    }
}
```

All-in-one printer:

```java
public class AllInOnePrinter
        implements Printer, Scanner, Fax {

    public void print() {}
    public void scan() {}
    public void fax() {}
}
```

### Benefits

```text
✓ Lower coupling
✓ Smaller contracts
✓ Easier testing
✓ Easier maintenance
✓ Less impact when interfaces change
✓ Clear capabilities
```

### Important

ISP does **not** mean:

> “Every interface must have one method.”

The correct question is:

> **“Do clients actually need these methods together?”**

### Interview phrase

> **“I design interfaces around cohesive client needs or capabilities, rather than creating large interfaces containing unrelated operations.”**

---

# 5. D — Dependency Inversion Principle (DIP)

### Definition

> **High-level modules should not depend on low-level modules. Both should depend on abstractions.**

And:

> **Abstractions should not depend on details. Details should depend on abstractions.**

---

## ❌ Bad

```java
public class OrderService {

    private final MySqlOrderRepository repository;

    public OrderService() {
        repository = new MySqlOrderRepository();
    }

    public void createOrder(Order order) {
        repository.save(order);
    }
}
```

Dependency:

```text
OrderService
     ↓
MySqlOrderRepository
```

Business logic knows the database implementation.

Switching:

```text
MySQL → MongoDB
```

can force changes to the service.

---

## ✅ Better

```java
public interface OrderRepository {
    void save(Order order);
}
```

```java
public class MySqlOrderRepository
        implements OrderRepository {

    public void save(Order order) {
        // MySQL
    }
}
```

```java
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public void createOrder(Order order) {
        repository.save(order);
    }
}
```

Dependency now:

```text
OrderService
     ↓
OrderRepository
     ↑
     |
MySqlOrderRepository
```

The business service depends on the abstraction.

---

# Dependency Inversion vs Dependency Injection

**Very common interview question.**

### DIP

A **principle**:

```text
Depend on abstractions,
not concrete details.
```

### Dependency Injection

A **technique**:

```text
Give the dependency from outside.
```

Example:

```java
public OrderService(OrderRepository repository) {
    this.repository = repository;
}
```

### Spring

Spring provides dependency injection.

So:

```text
DIP                → Design principle
Dependency Injection → Technique
Spring              → Framework that performs DI
```

### Benefits

```text
✓ Loose coupling
✓ Better testability
✓ Easy implementation swapping
✓ Business logic independent of infrastructure
✓ Better maintainability
```

### Interview phrase

> **“I treat business logic as policy and technologies such as databases, payment providers, and messaging systems as implementation details. The policy depends on abstractions, while the details implement those abstractions.”**

---

# SOLID Together — One Example

Imagine an e-commerce checkout:

```text
                    CheckoutService
                           |
             -----------------------------
             |             |             |
             ↓             ↓             ↓
      OrderRepository PaymentGateway NotificationSender
             ↑             ↑             ↑
             |             |             |
            JPA          Stripe         Email
                       Razorpay          SMS
```

### SRP

Each component has a focused responsibility.

```text
Repository  → persistence
Payment     → payment
Notification → communication
```

### OCP

Add:

```text
PayPalPaymentGateway
```

without changing checkout logic.

### LSP

Every `PaymentGateway` implementation must honor the same contract.

```java
PaymentGateway gateway =
        new StripePaymentGateway();
```

should be safely replaceable with another valid implementation.

### ISP

Don't make:

```java
PaymentGateway
```

also contain:

```text
generatePdf()
sendEmail()
saveToDatabase()
generateReport()
```

Keep the interface focused.

### DIP

`CheckoutService` depends on:

```text
PaymentGateway
OrderRepository
NotificationSender
```

rather than:

```text
StripeClient
MySqlConnection
SmtpClient
```

---

# SOLID — Fast Revision Table

| Principle   | Core Idea                                       | Typical Smell                               | Typical Solution                   |
| ----------- | ----------------------------------------------- | ------------------------------------------- | ---------------------------------- |
| **S — SRP** | One reason to change                            | Huge/mixed class                            | Split responsibilities             |
| **O — OCP** | Extend without repeatedly modifying stable code | Growing `if/switch` for variants            | Polymorphism / Strategy            |
| **L — LSP** | Subtype must honor parent contract              | Unsupported behavior / special cases        | Redesign abstraction               |
| **I — ISP** | Don't force clients to depend on unused methods | Fat interface                               | Smaller role/capability interfaces |
| **D — DIP** | High-level code depends on abstractions         | `new ConcreteClass()` inside business logic | Interfaces + DI                    |

---

# SOLID vs Common Java Concepts

These are worth connecting in interviews:

```text
SRP
 ↓
Cohesion

OCP
 ↓
Polymorphism
Strategy Pattern

LSP
 ↓
Inheritance
Behavioral contracts

ISP
 ↓
Small interfaces
Capability-based design

DIP
 ↓
Abstraction
Dependency Injection
Constructor Injection
Spring
```

---

# The 5 Interview One-Liners

### SRP

> **One class should have one reason to change.**

### OCP

> **New behavior should preferably be added through extension rather than repeatedly modifying stable existing code.**

### LSP

> **A subtype must be behaviorally substitutable for its parent.**

### ISP

> **Clients shouldn't be forced to depend on methods they don't need.**

### DIP

> **High-level business logic should depend on abstractions, not concrete implementation details.**

---

# How to Identify Which SOLID Principle Is Relevant

Given some bad code, ask these questions:

```text
1. Too many unrelated responsibilities?
   → SRP

2. New type requires modifying a growing if/switch?
   → OCP

3. Subclass can't honor parent's behavior?
   → LSP

4. Implementation/client doesn't need many interface methods?
   → ISP

5. Business code directly depends on concrete infrastructure?
   → DIP
```

---

# The MOST Important Relationships

These are especially useful for interviews:

```text
SRP + ISP
→ Focused responsibilities and focused interfaces

OCP + Polymorphism
→ Add new implementations without changing core logic

LSP + OCP
→ New implementations must actually honor the abstraction

ISP + LSP
→ Smaller interfaces make valid substitution easier

DIP + OCP
→ Abstractions allow implementations to be extended/swapped

DIP + Dependency Injection
→ DI is a common technique for implementing DIP
```

---

# One Mental Model for All Five

When designing a class, ask:

```text
                      My Class
                         |
       ----------------------------------------
       |          |          |        |       |
       ↓          ↓          ↓        ↓       ↓
   Responsibility Extension Contract Interface Dependency
       |          |          |        |       |
      SRP        OCP        LSP      ISP      DIP
```

Or, even simpler:

```text
S → "Is this class doing too much?"

O → "Will new types force me to modify this?"

L → "Can this child safely act as the parent?"

I → "Does this client need all these methods?"

D → "Why does this business class know this concrete implementation?"
```

That five-question checklist is probably the **best practical revision tool** for SOLID.

