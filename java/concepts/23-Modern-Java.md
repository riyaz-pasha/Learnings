## 23. Modern Java — Java 8 → Java 21

This is an important EPAM interview area because interviewers often ask **“What modern Java features have you used?”** rather than only testing old Java syntax.

We'll go version-by-version, but focus on the features that matter most in interviews and real projects.

---

# 1. Java 8 — The Big Revolution

Java 8 introduced several features that fundamentally changed how Java code is written.

The most important ones are:

* Lambda expressions
* Functional interfaces
* Stream API
* `Optional`
* Default/static methods in interfaces
* Method references
* New Date/Time API

We've already covered lambdas, functional interfaces, Streams and Optional, so let's focus on the remaining pieces.

---

## 2. Default Methods in Interfaces

Before Java 8:

```java
interface Vehicle {
    void start();
}
```

Suppose 100 classes implement `Vehicle`.

If we add:

```java
void stop();
```

every existing implementation breaks because it must implement the new method.

Java 8 introduced **default methods**:

```java
interface Vehicle {

    void start();

    default void stop() {
        System.out.println("Vehicle stopped");
    }
}
```

Now existing implementations don't have to implement `stop()`.

```java
class Car implements Vehicle {

    @Override
    public void start() {
        System.out.println("Car started");
    }
}
```

`Car` automatically gets the default implementation.

```java
Car car = new Car();

car.start();
car.stop();
```

### Why were default methods introduced?

One major reason:

> **Interface evolution without breaking existing implementations.**

This was particularly important for adding methods to widely used Java APIs.

---

# 3. Default Method Conflict

What happens if two interfaces provide the same default method?

```java
interface A {
    default void print() {
        System.out.println("A");
    }
}

interface B {
    default void print() {
        System.out.println("B");
    }
}
```

Now:

```java
class Test implements A, B {
}
```

This doesn't compile.

Java doesn't know which implementation to choose.

You must resolve the conflict:

```java
class Test implements A, B {

    @Override
    public void print() {
        A.super.print();
    }
}
```

Or:

```java
@Override
public void print() {
    B.super.print();
}
```

Or provide completely new behavior.

### Interview question

**What happens if a class inherits the same default method from two interfaces?**

Answer:

> The class must explicitly override the method and resolve the conflict.

---

# 4. Interface Static Methods

Java 8 also allows static methods inside interfaces.

```java
interface MathUtil {

    static int square(int n) {
        return n * n;
    }
}
```

Call it using the interface:

```java
int result = MathUtil.square(5);
```

Not:

```java
MathUtil obj = ...;
obj.square(5); // invalid
```

Static interface methods belong to the interface itself.

---

# 5. Private Interface Methods — Java 9

Java 8 allowed default methods.

But imagine an interface has several default methods sharing common logic:

```java
interface Service {

    default void method1() {
        commonLogic();
    }

    default void method2() {
        commonLogic();
    }
}
```

Java 9 introduced **private methods inside interfaces**:

```java
interface Service {

    default void method1() {
        commonLogic();
    }

    default void method2() {
        commonLogic();
    }

    private void commonLogic() {
        System.out.println("Common logic");
    }
}
```

This allows interfaces to reuse implementation internally without exposing helper methods as part of the public contract.

---

# 6. Java 9 — Factory Methods for Collections

Before Java 9:

```java
List<String> names =
        Arrays.asList("John", "Bob", "Alice");
```

Java 9 introduced:

```java
List<String> names =
        List.of("John", "Bob", "Alice");
```

Similarly:

```java
Set<String> names =
        Set.of("John", "Bob", "Alice");

Map<Integer, String> employees =
        Map.of(
            1, "John",
            2, "Bob"
        );
```

These are extremely common in modern Java.

### Important properties

These collections are **unmodifiable**.

```java
List<String> names = List.of("John", "Bob");

names.add("Alice"); // UnsupportedOperationException
```

They also don't allow `null`.

```java
List<String> names = List.of("John", null);
```

This throws `NullPointerException`.

---

# 7. `List.of()` vs `Arrays.asList()`

Very common interview question.

### `Arrays.asList()`

```java
List<String> list =
        Arrays.asList("A", "B", "C");
```

You cannot change its size:

```java
list.add("D"); // UnsupportedOperationException
```

But you can replace an existing element:

```java
list.set(0, "X"); // allowed
```

### `List.of()`

```java
List<String> list =
        List.of("A", "B", "C");
```

You can't modify it:

```java
list.add("D");     // exception
list.set(0, "X");  // exception
list.remove(0);   // exception
```

### Want a mutable list?

```java
List<String> list =
        new ArrayList<>(List.of("A", "B", "C"));
```

---

# 8. `var` — Java 10

Java 10 introduced local variable type inference.

Instead of:

```java
String name = "John";
```

you can write:

```java
var name = "John";
```

Java infers:

```text
String
```

Similarly:

```java
var number = 10;
```

The compiler knows:

```text
int
```

And:

```java
var employees = new ArrayList<Employee>();
```

The inferred type is:

```text
ArrayList<Employee>
```

### Important

`var` does **not** mean dynamically typed.

This is still statically typed Java.

```java
var name = "John";

name = 10; // compilation error
```

The type is inferred at compile time.

---

# 9. Where can `var` be used?

Primarily for **local variables**:

```java
public void test() {

    var name = "John";
    var age = 30;
}
```

It cannot be used for:

```java
class Employee {

    var name; // invalid
}
```

or:

```java
void test(var name) { } // invalid
```

or:

```java
var method() { } // invalid
```

### Also important

You must initialize it:

```java
var name; // invalid
```

because the compiler has nothing from which to infer the type.

---

# 10. When should you use `var`?

Good:

```java
var employees = new ArrayList<Employee>();
```

The right-hand side already makes the type obvious.

Potentially bad:

```java
var result = process();
```

If it's not obvious what `process()` returns, explicit typing may be more readable:

```java
EmployeeResult result = process();
```

Interview answer:

> `var` reduces verbosity while preserving static typing, but it should be used where the inferred type remains clear.

---

# 11. Java 14 — Switch Expressions

Traditional switch:

```java
String result;

switch (day) {
    case 1:
        result = "Monday";
        break;

    case 2:
        result = "Tuesday";
        break;

    default:
        result = "Unknown";
}
```

Modern Java allows:

```java
String result = switch (day) {

    case 1 -> "Monday";
    case 2 -> "Tuesday";
    default -> "Unknown";
};
```

Much cleaner.

---

# 12. Multiple Cases

You can combine cases:

```java
String type = switch (day) {

    case 1, 2, 3, 4, 5 -> "Weekday";

    case 6, 7 -> "Weekend";

    default -> "Invalid";
};
```

---

# 13. `yield`

If a switch branch needs multiple statements:

```java
String result = switch (day) {

    case 1 -> {
        System.out.println("Processing...");
        yield "Monday";
    }

    default -> "Other";
};
```

`yield` returns the value from a switch expression branch.

---

# 14. Java 15 — Text Blocks

Before Java 15:

```java
String json =
        "{\n" +
        "  \"name\": \"John\",\n" +
        "  \"age\": 30\n" +
        "}";
```

Ugly.

Text blocks:

```java
String json = """
        {
          "name": "John",
          "age": 30
        }
        """;
```

Much easier for:

* JSON
* SQL
* HTML
* XML
* multiline text

Example:

```java
String sql = """
        SELECT *
        FROM employees
        WHERE department = 'IT'
        """;
```

---

# 15. Java 16 — `instanceof` Pattern Matching

Before:

```java
if (obj instanceof String) {

    String str = (String) obj;

    System.out.println(str.length());
}
```

Modern Java:

```java
if (obj instanceof String str) {
    System.out.println(str.length());
}
```

The variable `str` is automatically created after the type check succeeds.

This is called **pattern matching for `instanceof`**.

---

# 16. Java 16/17 — Records

This is one of the **most important modern Java interview topics**.

Suppose you need a simple immutable data carrier:

```java
class Employee {

    private final String name;
    private final int age;

    public Employee(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    // equals()
    // hashCode()
    // toString()
}
```

That's a lot of boilerplate.

A record:

```java
public record Employee(String name, int age) {
}
```

Automatically provides important members such as:

```java
name()
age()
equals()
hashCode()
toString()
```

Notice the accessor syntax:

```java
employee.name()
```

not:

```java
employee.getName()
```

---

# 17. Are Records Immutable?

Records are **shallowly immutable**.

For:

```java
record Employee(String name, int age) {
}
```

you can't change:

```java
employee.name = "Bob"; // impossible
```

But consider:

```java
record Employee(List<String> skills) {
}
```

The record reference is final, but the list itself may still be mutable.

```java
employee.skills().add("Java");
```

may be possible.

So:

> A record does not automatically make every object reachable from its components deeply immutable.

This is an excellent interview distinction.

---

# 18. Compact Constructor in Records

You can validate record data:

```java
record Employee(String name, int age) {

    public Employee {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
    }
}
```

This is called a **compact constructor**.

You don't need to explicitly write:

```java
this.name = name;
this.age = age;
```

The record handles that.

---

# 19. Records vs Normal Classes

Use a record when the primary purpose is:

> **Representing data.**

For example:

```java
record EmployeeDto(
    Long id,
    String name,
    String department
) {}
```

Very useful for DTO-style objects.

A normal class is generally more appropriate when you need richer mutable behavior/state management, inheritance from a class, or other semantics that don't fit a record.

---

# 20. Sealed Classes — Java 17

Sealed classes let you control which classes can extend a class.

```java
public sealed class Payment
        permits CreditCardPayment, CashPayment {
}
```

Only these classes can extend it:

```java
final class CreditCardPayment extends Payment {
}

final class CashPayment extends Payment {
}
```

Trying:

```java
class CryptoPayment extends Payment {
}
```

will fail unless permitted.

---

# 21. Why Sealed Classes?

Normally:

```java
class Payment {
}
```

Any class can extend it.

With sealed classes:

```java
sealed class Payment
        permits CreditCardPayment, CashPayment {
}
```

the hierarchy is controlled.

This is useful when the domain has a **known set of variants**.

For example:

```text
Payment
 ├── CreditCardPayment
 ├── CashPayment
 └── BankTransferPayment
```

You explicitly control the hierarchy.

---

# 22. Subclasses of a Sealed Class

A permitted subclass must declare how its inheritance continues.

It can be:

### `final`

```java
final class CreditCardPayment extends Payment {
}
```

No further subclassing.

### `sealed`

```java
sealed class Payment
        permits CreditCardPayment {

}
```

A subclass can itself control its subclasses.

### `non-sealed`

```java
non-sealed class CreditCardPayment extends Payment {
}
```

This opens the hierarchy again below that class.

---

# 23. Pattern Matching with Sealed Classes

These features work nicely together.

Imagine:

```java
sealed interface Payment
        permits CardPayment, CashPayment {
}

record CardPayment(double amount)
        implements Payment {
}

record CashPayment(double amount)
        implements Payment {
}
```

You can use pattern matching:

```java
String process(Payment payment) {

    if (payment instanceof CardPayment card) {
        return "Card: " + card.amount();
    }

    if (payment instanceof CashPayment cash) {
        return "Cash: " + cash.amount();
    }

    throw new IllegalStateException();
}
```

Modern Java is moving toward more expressive algebraic/data-oriented style while remaining Java.

---

# 24. Java 21 — Virtual Threads

This is **very important for modern Java interviews**.

Traditional Java threads are relatively expensive.

Creating huge numbers of platform threads isn't practical.

Java 21 introduced **virtual threads** as a standard feature.

Example:

```java
Thread.startVirtualThread(() -> {
    System.out.println("Running in virtual thread");
});
```

Or with an executor:

```java
try (var executor =
         Executors.newVirtualThreadPerTaskExecutor()) {

    executor.submit(() -> {
        // blocking I/O
    });
}
```

---

# 25. Why Virtual Threads?

Imagine a web application receiving:

```text
10,000 requests
```

Many requests spend most of their time waiting for:

* database
* HTTP service
* file system
* network

Traditional approach:

```text
Request → Platform Thread → waits for DB
                         ↓
                    thread blocked
```

Virtual threads make it practical to have very large numbers of lightweight threads.

Conceptually:

```text
Request 1 → Virtual Thread
Request 2 → Virtual Thread
Request 3 → Virtual Thread
...
Request 10000 → Virtual Thread
```

When a virtual thread blocks on supported blocking operations, the underlying carrier thread can often execute other virtual threads.

---

# 26. Virtual Threads Don't Make CPU Work Faster

This is a very important interview trap.

Virtual threads are primarily about **scalability for high-concurrency workloads**, particularly blocking I/O.

They don't magically make:

```java
while (true) {
    // heavy CPU calculation
}
```

run faster.

If you have CPU-bound work, the bottleneck is still CPU.

Think:

```text
I/O-bound → virtual threads can be excellent

CPU-bound → more threads don't create more CPU cores
```

---

# 27. Platform Threads vs Virtual Threads

| Platform Thread                 | Virtual Thread                                |
| ------------------------------- | --------------------------------------------- |
| More expensive                  | Lightweight                                   |
| OS-backed execution             | JVM-managed                                   |
| Limited number practical        | Very large numbers possible                   |
| Good for general work           | Excellent for high-concurrency blocking tasks |
| Thread creation has higher cost | Cheap to create                               |
| Java 21 and earlier             | Standard from Java 21                         |

One important nuance:

> Virtual threads are still `Thread` objects and preserve the familiar Java threading model.

---

# 28. The Java Version Timeline You Should Remember

For interviews, remember this:

```text
Java 8
 ├── Lambda
 ├── Functional Interfaces
 ├── Streams
 ├── Optional
 ├── Default Interface Methods
 └── New Date/Time API

Java 9
 ├── Module System
 ├── List.of / Set.of / Map.of
 └── Private Interface Methods

Java 10
 └── var

Java 11
 ├── String improvements
 └── Standard HTTP Client

Java 14
 └── Switch Expressions

Java 15
 └── Text Blocks

Java 16
 └── Pattern Matching for instanceof
 └── Records finalized

Java 17
 └── Sealed Classes
 └── Records / Pattern Matching widely important as LTS-era features

Java 21
 ├── Virtual Threads
 ├── Pattern Matching for switch
 └── Record Patterns
```

For an EPAM interview, **Java 8, 11, 17 and 21** are especially worth knowing because they're major LTS releases.

---

# 29. Top Modern Java Interview Questions

You should be able to answer these quickly:

### Q1. What is `var`?

> Local variable type inference introduced in Java 10. Java remains statically typed; the compiler determines the type at compile time.

### Q2. What are records?

> A concise way to declare classes primarily intended to model immutable data. They automatically provide components/accessors, `equals`, `hashCode`, and `toString`.

### Q3. Are records deeply immutable?

> No. Record components are final references, but referenced mutable objects can still be modified.

### Q4. What are sealed classes?

> Classes/interfaces that restrict which classes can extend or implement them.

### Q5. What are virtual threads?

> Lightweight JVM-managed threads designed to support very high concurrency, especially for blocking I/O workloads.

### Q6. Do virtual threads make CPU-bound operations faster?

> No. They primarily improve concurrency and resource utilization for workloads with lots of waiting/blocking.

### Q7. Difference between `List.of()` and `Arrays.asList()`?

> `List.of()` creates an unmodifiable list and rejects nulls; `Arrays.asList()` creates a fixed-size list backed by the array and allows `set()`.

### Q8. What problem do default methods solve?

> They allow interfaces to evolve by adding implementations without forcing every existing implementation to immediately implement the new method.

---

## The most important mental model

Modern Java has been moving in this direction:

```text
Java 8
   ↓
Functional programming
   ↓
Java 9-11
   ↓
Cleaner APIs / local inference
   ↓
Java 14-17
   ↓
More expressive type/data modeling
   ↓
Records + Sealed Classes + Pattern Matching
   ↓
Java 21
   ↓
Lightweight concurrency with Virtual Threads
```

If an interviewer asks **“What Java version/features have you worked with?”**, don't just list features. Be ready to explain **why each feature exists and when you'd use it**.

**Next topic: Coding Questions & Java Interview Problem-Solving Patterns** — arrays, strings, HashMap, two pointers, sliding window, binary search, stacks/queues, and the Java-specific implementation details interviewers look for.

