# Topic 9 — Interface vs Abstract Class

This is a **very common Java interview topic**, and interviewers often go beyond the basic “interface vs abstract class” table.

We'll build it from fundamentals and then cover the traps.

---

## 1. What is an interface?

An interface defines a **contract** that implementing classes agree to follow.

```java
interface Payment {
    void pay();
}
```

A class implements it:

```java
class CreditCardPayment implements Payment {

    @Override
    public void pay() {
        System.out.println("Paying by credit card");
    }
}
```

The interface says:

> Any `Payment` must provide `pay()`.

It doesn't necessarily dictate how `pay()` works.

---

# 2. What is an abstract class?

An abstract class is a class that can contain both:

* abstract methods
* concrete methods
* instance variables
* constructors
* static methods
* final methods

Example:

```java
abstract class Payment {

    protected double amount;

    Payment(double amount) {
        this.amount = amount;
    }

    abstract void pay();

    void printAmount() {
        System.out.println(amount);
    }
}
```

Subclass:

```java
class CreditCardPayment extends Payment {

    CreditCardPayment(double amount) {
        super(amount);
    }

    @Override
    void pay() {
        System.out.println("Credit card payment");
    }
}
```

---

# 3. The fundamental difference

Think about them like this:

### Interface

> **What can you do?**

```java
interface Flyable {
    void fly();
}
```

A bird can fly:

```java
class Bird implements Flyable
```

A drone can also fly:

```java
class Drone implements Flyable
```

They aren't necessarily related through a common class hierarchy.

---

### Abstract class

> **What are you, and what common state/behavior do you share?**

```java
abstract class Vehicle {
    protected String registrationNumber;

    abstract void start();

    void stop() {
        System.out.println("Vehicle stopped");
    }
}
```

Cars and bikes can inherit common vehicle state and behavior.

---

# 4. Main differences

| Feature                    | Interface                                                                                         | Abstract Class              |
| -------------------------- | ------------------------------------------------------------------------------------------------- | --------------------------- |
| Declared with              | `interface`                                                                                       | `abstract class`            |
| Implemented/extended using | `implements`                                                                                      | `extends`                   |
| Multiple inheritance       | Multiple interfaces                                                                               | Only one class              |
| Instance variables         | No normal instance fields                                                                         | Yes                         |
| Constructor                | No                                                                                                | Yes                         |
| Abstract methods           | Yes                                                                                               | Yes                         |
| Concrete methods           | Yes, including default methods                                                                    | Yes                         |
| Static methods             | Yes                                                                                               | Yes                         |
| Final methods              | Interface methods cannot generally be instance `final`; interface can have static/private methods | Yes                         |
| Access to state            | No instance state                                                                                 | Can maintain instance state |
| Main purpose               | Contract/capability                                                                               | Shared base abstraction     |

But don't memorize this table blindly. There are important Java 8+ details.

---

# 5. Can an interface have method implementations?

Before Java 8, interface methods were essentially abstract.

Java 8 introduced:

### `default` methods

```java
interface Vehicle {

    default void start() {
        System.out.println("Vehicle starting");
    }
}
```

Implementing class gets the implementation automatically:

```java
class Car implements Vehicle {
}
```

Now:

```java
Car car = new Car();
car.start();
```

works.

---

# 6. Why were default methods introduced?

Imagine Java had:

```java
interface Payment {
    void pay();
}
```

Thousands of classes implement it.

Now Java designers want to add:

```java
void refund();
```

If it were abstract:

```java
interface Payment {
    void pay();
    void refund();
}
```

every existing implementation would potentially break because it would need to implement `refund()`.

Default methods allow adding behavior while preserving source compatibility for existing implementations:

```java
interface Payment {

    void pay();

    default void refund() {
        System.out.println("Default refund");
    }
}
```

---

# 7. Can interfaces have static methods?

Yes.

```java
interface Utility {

    static void print() {
        System.out.println("Hello");
    }
}
```

Call it using:

```java
Utility.print();
```

Not:

```java
Utility obj = ...
obj.print(); // invalid
```

Interface static methods belong to the interface itself.

---

# 8. Can interfaces have private methods?

Yes, since Java 9.

Example:

```java
interface Payment {

    default void process() {
        validate();
        System.out.println("Processing");
    }

    private void validate() {
        System.out.println("Validating");
    }
}
```

The private method helps share implementation between default methods.

It is not accessible from implementing classes.

---

# 9. Can an interface have variables?

Yes, but interface fields are implicitly:

```java
public static final
```

For example:

```java
interface Constants {
    int MAX_RETRY = 3;
}
```

is effectively:

```java
public static final int MAX_RETRY = 3;
```

Therefore:

```java
Constants.MAX_RETRY
```

is valid.

But:

```java
MAX_RETRY = 5;
```

is not allowed.

---

# 10. Can an interface have constructors?

No.

This is an important interview question.

Why?

Because an interface doesn't represent an object instance that needs to be initialized through a constructor.

```java
interface Payment {

    Payment() { // compilation error
    }
}
```

An implementing class has the constructor:

```java
class CreditCardPayment implements Payment {

    CreditCardPayment() {
    }
}
```

---

# 11. Can an abstract class have a constructor?

Yes.

```java
abstract class Vehicle {

    Vehicle() {
        System.out.println("Vehicle constructor");
    }
}
```

Even though you cannot directly create:

```java
new Vehicle(); // invalid
```

its constructor executes when a subclass is created.

```java
class Car extends Vehicle {
}
```

Then:

```java
new Car();
```

causes:

```text
Vehicle constructor
```

to execute first.

---

# 12. Can an abstract class have zero abstract methods?

Yes.

This is another common interview trap.

```java
abstract class Utility {

    void print() {
        System.out.println("Hello");
    }
}
```

This is completely valid.

Why make it abstract then?

You may want to prevent direct instantiation:

```java
new Utility(); // not allowed
```

while still providing shared behavior to subclasses.

---

# 13. Can an interface have zero abstract methods?

Yes, especially with Java 8+.

For example:

```java
interface Marker {
}
```

This is called a **marker interface** when used to mark classes for special semantics.

Classic examples include:

```java
Serializable
Cloneable
```

Although `Serializable` has no methods.

---

# 14. Multiple inheritance

This is one of the biggest differences.

Java doesn't allow:

```java
class C extends A, B {
}
```

because multiple class inheritance can create ambiguity.

But Java allows:

```java
class C implements A, B {
}
```

where `A` and `B` are interfaces.

Example:

```java
interface Printable {
    void print();
}

interface Scannable {
    void scan();
}

class Printer implements Printable, Scannable {

    public void print() {
        System.out.println("Printing");
    }

    public void scan() {
        System.out.println("Scanning");
    }
}
```

So:

```text
class
   ↓
single inheritance

interface
   ↓
multiple interfaces
```

---

# 15. But what if two interfaces have the same default method?

This is an **extremely common interview question**.

Suppose:

```java
interface A {

    default void print() {
        System.out.println("A");
    }
}
```

and:

```java
interface B {

    default void print() {
        System.out.println("B");
    }
}
```

Now:

```java
class C implements A, B {
}
```

This causes a compilation error because Java doesn't know which `print()` to use.

---

## Solution

The class must resolve the conflict:

```java
class C implements A, B {

    @Override
    public void print() {
        A.super.print();
    }
}
```

Now:

```java
new C().print();
```

prints:

```text
A
```

You can choose B instead:

```java
B.super.print();
```

Or provide completely new behavior:

```java
@Override
public void print() {
    System.out.println("C");
}
```

---

# 16. What if a class and interface both provide the same method?

Class wins.

Example:

```java
interface A {

    default void print() {
        System.out.println("Interface");
    }
}
```

Class:

```java
class B {

    public void print() {
        System.out.println("Class");
    }
}
```

Then:

```java
class C extends B implements A {
}
```

Calling:

```java
new C().print();
```

prints:

```text
Class
```

### Rule

> A class implementation takes precedence over an interface default method.

This is known as the **class wins rule**.

---

# 17. What if one interface extends another?

Suppose:

```java
interface A {

    default void print() {
        System.out.println("A");
    }
}
```

and:

```java
interface B extends A {

    default void print() {
        System.out.println("B");
    }
}
```

Then:

```java
class C implements B {
}
```

Calling:

```java
new C().print();
```

uses B's implementation.

The more specific interface wins.

---

# 18. Functional interface connection

Remember our previous topic?

A functional interface has exactly one abstract method:

```java
@FunctionalInterface
interface Calculator {
    int calculate(int a, int b);
}
```

It can still have default/static methods:

```java
@FunctionalInterface
interface Calculator {

    int calculate(int a, int b);

    default void print() {
        System.out.println("Calculator");
    }

    static void info() {
        System.out.println("Utility");
    }
}
```

It's still functional because it has only **one abstract method**.

---

# 19. Interface reference and polymorphism

This is very important.

```java
interface Payment {
    void pay();
}
```

```java
class CreditCardPayment implements Payment {

    @Override
    public void pay() {
        System.out.println("Credit card");
    }
}
```

Then:

```java
Payment payment = new CreditCardPayment();
payment.pay();
```

The reference type is:

```text
Payment
```

The actual object is:

```text
CreditCardPayment
```

At runtime, the overridden `pay()` from `CreditCardPayment` executes.

This is runtime polymorphism.

---

# 20. Interface vs abstract class — when should you use which?

A practical way to think about it:

### Use an interface when you want to define a capability/contract.

Examples:

```java
Comparable
Runnable
Serializable
PaymentProcessor
NotificationSender
```

For example:

```java
interface NotificationSender {
    void send(String message);
}
```

Different implementations:

```java
class EmailSender implements NotificationSender
class SmsSender implements NotificationSender
class PushSender implements NotificationSender
```

The classes don't need to share a common state.

---

### Use an abstract class when subclasses genuinely share state or implementation.

Example:

```java
abstract class Employee {

    protected String name;
    protected double salary;

    Employee(String name, double salary) {
        this.name = name;
        this.salary = salary;
    }

    abstract double calculateBonus();

    void printName() {
        System.out.println(name);
    }
}
```

Different employees can inherit common state and behavior.

---

# 21. A very good interview answer

If interviewer asks:

> "When would you choose an interface over an abstract class?"

You can say:

> "I would generally use an interface when I want to define a contract or capability that potentially unrelated classes can implement, especially when multiple inheritance of type is useful. I would use an abstract class when subclasses share common state, constructors, or substantial implementation and there is a genuine common base relationship."

That's a much stronger answer than:

> "Interface has abstract methods and abstract class has concrete methods."

---

# 22. Can an abstract class implement an interface?

Yes.

```java
interface Payment {
    void pay();
}
```

```java
abstract class BasePayment implements Payment {

    void log() {
        System.out.println("Logging");
    }
}
```

It doesn't have to implement `pay()` immediately because it's abstract.

A concrete subclass can implement it:

```java
class CreditCardPayment extends BasePayment {

    @Override
    public void pay() {
        System.out.println("Credit card");
    }
}
```

---

# 23. Can an interface extend another interface?

Yes.

```java
interface Animal {
    void eat();
}
```

```java
interface Dog extends Animal {
    void bark();
}
```

A class implementing Dog must implement both:

```java
class Labrador implements Dog {

    public void eat() {
    }

    public void bark() {
    }
}
```

An interface uses:

```java
extends
```

to inherit from another interface.

A class uses:

```java
implements
```

to implement an interface.

---

# 24. Can an interface extend multiple interfaces?

Yes.

```java
interface A {
    void a();
}

interface B {
    void b();
}

interface C extends A, B {
    void c();
}
```

This is legal.

So:

```text
Class → extends → one class

Class → implements → multiple interfaces

Interface → extends → multiple interfaces
```

---

# 25. Important Java 8 interface rules

Memorize this:

```text
Interface methods:

abstract
    ↓
implicitly public abstract

default
    ↓
has implementation

static
    ↓
belongs to interface

private
    ↓
helper implementation, Java 9+
```

Interface fields:

```text
public static final
```

implicitly.

---

# 26. Common interview traps

### Q: Can an interface have a constructor?

**No.**

### Q: Can an abstract class have a constructor?

**Yes.**

### Q: Can an abstract class have no abstract methods?

**Yes.**

### Q: Can an interface have implemented methods?

**Yes.** Default/static/private methods can have implementations.

### Q: Can an interface have instance variables?

**No.** Its fields are implicitly `public static final`.

### Q: Can a class implement multiple interfaces?

**Yes.**

### Q: Can a class extend multiple classes?

**No.**

### Q: Can an interface extend multiple interfaces?

**Yes.**

### Q: Can an abstract class implement an interface?

**Yes.**

### Q: Can an interface extend a class?

**No.**

---

# 27. The big picture

Think of this example:

```text
                 NotificationSender
                         |
          +--------------+--------------+
          |              |              |
       Email           SMS            Push
```

This is a good interface scenario.

They simply share a capability:

```java
interface NotificationSender {
    void send(String message);
}
```

Now consider:

```text
                 Employee
                    |
          +---------+---------+
          |                   |
       Developer           Manager
```

Suppose they share:

```text
name
salary
employeeId
common logging
common validation
```

An abstract class can make sense:

```java
abstract class Employee {
    protected String name;
    protected double salary;

    abstract void performWork();
}
```

---

# EPAM interview questions to practice

Before moving on, make sure you can explain these:

1. **Interface vs abstract class**
2. Why doesn't Java support multiple inheritance of classes?
3. Can interfaces have method implementations?
4. What are default methods?
5. Why were default methods introduced?
6. Can interfaces have static methods?
7. Can interfaces have private methods?
8. Can an interface have a constructor?
9. Can an abstract class have no abstract methods?
10. What happens when two interfaces have the same default method?
11. What is the class-wins rule?
12. Can an interface extend multiple interfaces?
13. Can an abstract class implement an interface without implementing all methods?
14. What are interface variables implicitly?
15. When would you choose interface vs abstract class?

### The 3 rules I'd memorize most

```text
1. Class can extend only ONE class.
2. Class can implement MULTIPLE interfaces.
3. Interface can extend MULTIPLE interfaces.
```

And for Java 8:

```text
Interface
 ├── abstract methods
 ├── default methods
 ├── static methods
 └── fields = public static final
```

**Next topic: Access Modifiers (`public`, `protected`, package-private, `private`)** — including package behavior, inheritance, and the interview traps around `protected`.

