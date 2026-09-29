# 1. Singleton Design Pattern

## 1. What is the Singleton Pattern?

The **Singleton** pattern ensures that:

> **A class has only one instance throughout the application, and provides a global access point to that instance.**

There are two important parts:

1. **Only one object can exist**
2. **Everyone accesses that same object**

Conceptually:

```text
             ┌─────────────────────┐
             │     Singleton       │
             │                     │
             │  - instance         │
             │                     │
             │  + getInstance()    │
             └──────────┬──────────┘
                        │
             ┌──────────▼──────────┐
             │   Single Object     │
             │      Instance       │
             └─────────────────────┘
                  ▲     ▲     ▲
                  │     │     │
                Client Client Client
```

All clients get the **same object**.

---

# 2. Why do we need Singleton?

Consider this class:

```java
public class Logger {

    public void log(String message) {
        System.out.println(message);
    }
}
```

We can create multiple objects:

```java
Logger logger1 = new Logger();
Logger logger2 = new Logger();
Logger logger3 = new Logger();
```

Now:

```java
logger1 != logger2
```

They are three different objects.

For some classes, this may be perfectly fine.

But imagine a class representing something that should be shared:

```text
Application
   │
   ├── Service A ──> Logger
   │
   ├── Service B ──> Logger
   │
   ├── Service C ──> Logger
   │
   └── Service D ──> Logger
```

You may want all of them to use **one shared logger instance**.

Instead of:

```text
Service A → Logger #1
Service B → Logger #2
Service C → Logger #3
Service D → Logger #4
```

you want:

```text
Service A ─┐
Service B ─┤
Service C ─┼──> Logger #1
Service D ─┘
```

That's the problem Singleton solves.

---

# 3. The basic structure

A Singleton usually has three characteristics.

### Private constructor

Prevent external code from doing:

```java
new Singleton();
```

### Private static instance

Store the single object inside the class.

### Public static access method

Provide a way to obtain that object.

Basic implementation:

```java
public class Singleton {

    private static Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }

        return instance;
    }
}
```

Usage:

```java
Singleton s1 = Singleton.getInstance();
Singleton s2 = Singleton.getInstance();

System.out.println(s1 == s2);
```

Output:

```text
true
```

Both variables refer to the same object.

---

# 4. Let's understand the code line by line

```java
private static Singleton instance;
```

Why `static`?

Because there should be only one instance associated with the **class**, not one instance per object.

If `instance` were not static, we'd have a problem:

```java
Singleton s1 = new Singleton();
```

But the constructor is private, so that isn't possible externally.

The Singleton needs some class-level place to hold its single instance.

Hence:

```java
static
```

---

## Private constructor

```java
private Singleton() {
}
```

This is critical.

Without it:

```java
Singleton s1 = new Singleton();
Singleton s2 = new Singleton();
Singleton s3 = new Singleton();
```

would be possible.

Then we wouldn't have a Singleton.

By making the constructor private:

```java
private Singleton()
```

only the class itself can create the object.

---

## `getInstance()`

```java
public static Singleton getInstance() {
    if (instance == null) {
        instance = new Singleton();
    }

    return instance;
}
```

First call:

```java
Singleton s1 = Singleton.getInstance();
```

At this point:

```text
instance == null
```

so:

```java
instance = new Singleton();
```

creates the object.

Then:

```java
return instance;
```

returns it.

Second call:

```java
Singleton s2 = Singleton.getInstance();
```

Now:

```text
instance != null
```

so no new object is created.

The existing object is returned.

---

# 5. Lazy initialization

The above implementation uses **lazy initialization**.

That means:

> The object is created only when it is actually requested.

For example:

```java
public class DatabaseConnection {

    private static DatabaseConnection instance;

    private DatabaseConnection() {
    }

    public static DatabaseConnection getInstance() {

        if (instance == null) {
            instance = new DatabaseConnection();
        }

        return instance;
    }
}
```

The object doesn't exist immediately when the class is loaded.

It is created when:

```java
DatabaseConnection.getInstance();
```

is called for the first time.

### Advantage

You don't create the object unnecessarily.

### Problem

The implementation is **not thread-safe**.

This is one of the most important Singleton interview topics.

---

# 6. The multithreading problem

Imagine two threads:

```text
Thread A
Thread B
```

Both call:

```java
Singleton.getInstance();
```

Suppose `instance == null`.

Thread A checks:

```java
if (instance == null)
```

True.

Before A creates the object, Thread B checks:

```java
if (instance == null)
```

Also true.

Now:

```text
Thread A → new Singleton()
Thread B → new Singleton()
```

Two objects have been created.

So the Singleton guarantee is broken.

Conceptually:

```text
                 instance == null
                    /       \
                   /         \
              Thread A      Thread B
                  │             │
                  ▼             ▼
           new Singleton()  new Singleton()
                  │             │
                  └──────┬──────┘
                         ▼
                    TWO OBJECTS
```

Therefore this implementation:

```java
if (instance == null) {
    instance = new Singleton();
}
```

is **not safe for concurrent access**.

---

# 7. Thread-safe Singleton — synchronized method

One simple solution:

```java
public class Singleton {

    private static Singleton instance;

    private Singleton() {
    }

    public static synchronized Singleton getInstance() {

        if (instance == null) {
            instance = new Singleton();
        }

        return instance;
    }
}
```

Now only one thread can execute `getInstance()` at a time.

```text
Thread A → enters getInstance()
Thread B → waits

Thread A → creates object
Thread A → returns object

Thread B → enters getInstance()
Thread B → sees instance != null
Thread B → returns same object
```

So this is thread-safe.

---

# 8. But why isn't `synchronized` always ideal?

Because **every call** acquires the lock.

Even after the object has already been created:

```java
Singleton.getInstance();
Singleton.getInstance();
Singleton.getInstance();
Singleton.getInstance();
```

each invocation goes through synchronization.

Conceptually:

```text
First call
   ↓
lock
   ↓
create object
   ↓
unlock

Later calls
   ↓
lock
   ↓
instance already exists
   ↓
unlock
```

The synchronization overhead may be unnecessary after initialization.

That's why another technique exists.

---

# 9. Double-Checked Locking

A common implementation is:

```java
public class Singleton {

    private static volatile Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {

        if (instance == null) {

            synchronized (Singleton.class) {

                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }

        return instance;
    }
}
```

This is called:

> **Double-Checked Locking (DCL)**

Why "double checked"?

Because we check `instance == null` twice.

### First check

```java
if (instance == null)
```

Avoid synchronization once the object already exists.

### Lock

```java
synchronized (Singleton.class)
```

Only one thread can enter the critical section.

### Second check

```java
if (instance == null)
```

This is necessary because another thread might have created the instance while this thread was waiting for the lock.

---

# 10. Why `volatile`?

This is another classic interview question.

Notice:

```java
private static volatile Singleton instance;
```

Why volatile?

Because object creation:

```java
instance = new Singleton();
```

is not conceptually just one indivisible operation.

There are multiple steps involved, including allocating the object, initializing it, and assigning the reference.

Without proper memory visibility/order guarantees, another thread could potentially observe a reference before the object's construction is safely visible.

`volatile` provides the required memory-visibility and ordering guarantees for this pattern.

For interviews, a good explanation is:

> "`volatile` prevents unsafe publication and ensures that threads see the correctly initialized Singleton instance."

---

# 11. The easiest thread-safe Singleton: eager initialization

We can avoid lazy initialization entirely.

```java
public class Singleton {

    private static final Singleton INSTANCE = new Singleton();

    private Singleton() {
    }

    public static Singleton getInstance() {
        return INSTANCE;
    }
}
```

This is **eager initialization**.

The instance is created when the class is initialized.

### Why is it thread-safe?

Java class initialization is guaranteed to happen safely and only once by the JVM.

So:

```java
private static final Singleton INSTANCE = new Singleton();
```

is very simple and thread-safe.

### Downside

The object is created even if nobody ever uses it.

For a cheap object, that may be completely acceptable.

---

# 12. Initialization-on-demand holder idiom

A particularly elegant approach in Java is:

```java
public class Singleton {

    private Singleton() {
    }

    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

This gives us:

* lazy initialization
* thread safety
* no explicit synchronization
* simple implementation

### How does it work?

When `Singleton` is loaded, `Holder` does not necessarily need to be initialized immediately.

When:

```java
getInstance()
```

accesses:

```java
Holder.INSTANCE
```

the JVM initializes `Holder`.

That causes:

```java
new Singleton();
```

to execute exactly once.

This is commonly considered an excellent Java Singleton implementation.

---

# 13. Enum Singleton

Java provides another powerful mechanism:

```java
public enum Singleton {

    INSTANCE;

    public void doSomething() {
        System.out.println("Doing something");
    }
}
```

Usage:

```java
Singleton.INSTANCE.doSomething();
```

This is one of the simplest ways to implement a Singleton in Java.

The Java language/JVM provides strong guarantees around enum instances.

It also handles several serialization-related concerns naturally.

---

# 14. Why can serialization break Singleton?

Consider:

```java
public class Singleton implements Serializable {

    private static final Singleton INSTANCE = new Singleton();

    private Singleton() {
    }

    public static Singleton getInstance() {
        return INSTANCE;
    }
}
```

You may serialize:

```java
Singleton s1 = Singleton.getInstance();
```

and later deserialize it:

```java
Singleton s2 = ...;
```

Without special handling:

```java
s1 == s2
```

can be `false`.

Why?

Because deserialization can create an object rather than simply returning your existing Singleton instance.

A traditional fix is:

```java
protected Object readResolve() {
    return INSTANCE;
}
```

So:

```java
public class Singleton implements Serializable {

    private static final Singleton INSTANCE = new Singleton();

    private Singleton() {
    }

    public static Singleton getInstance() {
        return INSTANCE;
    }

    protected Object readResolve() {
        return INSTANCE;
    }
}
```

Now deserialization returns the existing instance.

---

# 15. Reflection can break Singleton

Even with:

```java
private Singleton() {
}
```

reflection can potentially access the constructor.

For example, reflective code can attempt to make the constructor accessible and create another instance.

So the statement:

> "Singleton can never have more than one object"

is not universally true under every mechanism available in Java.

This is an important nuance.

For normal application code, the Singleton pattern gives the intended restriction. Reflection, serialization, cloning, multiple class loaders, and other mechanisms require additional consideration.

---

# 16. Cloning can also break Singleton

Suppose:

```java
public class Singleton implements Cloneable {

    private static final Singleton INSTANCE = new Singleton();

    private Singleton() {
    }

    public static Singleton getInstance() {
        return INSTANCE;
    }
}
```

Someone could potentially call:

```java
Singleton s2 = (Singleton) s1.clone();
```

and obtain another object.

One defense is to prevent cloning:

```java
@Override
protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
}
```

Again, this illustrates an important point:

> Singleton isn't merely "one static object"; it is a design intended to control object creation.

---

# 17. Real-world examples

Singleton is appropriate when you genuinely want one shared instance representing a single application-level resource or coordination point.

Examples can include:

```text
Configuration manager
Application-wide registry
Certain caches
Some logging abstractions
A shared coordination component
```

However, modern frameworks often provide their own lifecycle management.

For example, in Spring:

```java
@Component
public class PaymentService {
}
```

By default, Spring creates a singleton-scoped bean.

That means you generally **do not need to manually implement**:

```java
private static PaymentService instance;
```

Spring's container manages the lifecycle.

---

# 18. Singleton vs Spring Singleton

This distinction is extremely important for Spring interviews.

A **classic Java Singleton** means:

```text
The class itself controls instance creation.
```

A **Spring singleton bean** means:

```text
The Spring container controls the instance lifecycle.
```

For example:

```java
@Component
public class EmailService {
}
```

By default, the Spring container maintains one bean instance per Spring `ApplicationContext`.

That is different from saying:

> There can only ever be one `EmailService` object in the entire JVM.

Multiple Spring application contexts can have their own bean instances.

So:

```text
Classic Singleton
    ↓
one instance controlled by class

Spring singleton scope
    ↓
one instance per ApplicationContext
```

This distinction is a frequent interview discussion.

---

# 19. Singleton is not the same as "static"

A common misconception is:

> "Singleton means everything should be static."

Not true.

Example:

```java
Singleton singleton = Singleton.getInstance();

singleton.process();
singleton.calculate();
singleton.save();
```

The object still has:

* instance state
* instance methods
* object identity

Static utility classes are different.

Example:

```java
Math.max(10, 20);
```

There isn't an object representing `Math`.

So:

```text
Static utility class ≠ Singleton
```

---

# 20. Singleton vs Utility Class

### Singleton

```java
Singleton.getInstance().doSomething();
```

There is an actual object.

It can contain state:

```java
private String configuration;
```

### Utility class

```java
FileUtils.readFile(...);
```

Typically:

```java
public final class FileUtils {

    private FileUtils() {
    }

    public static void readFile(...) {
    }
}
```

No instance is intended.

The distinction is:

```text
Singleton → one object
Utility class → no object intended
```

---

# 21. When should we use Singleton?

Use it when **one shared instance is genuinely part of the domain/design requirement**.

A good way to think about it:

Ask:

> "Would creating multiple independent instances cause a correctness or coordination problem?"

If yes, Singleton may be appropriate.

If you're merely trying to make an object globally accessible, Singleton may be masking a dependency-design problem.

That's one reason Singleton is often criticized.

---

# 22. Problems with Singleton

Singleton is useful, but it comes with trade-offs.

### Global state

Because the instance is globally accessible:

```java
Singleton.getInstance()
```

it behaves somewhat like global state.

That makes dependencies less explicit.

Compare:

```java
public class OrderService {

    public void process() {
        Logger.getInstance().log("...");
    }
}
```

The dependency on `Logger` is hidden inside the method.

Versus dependency injection:

```java
public class OrderService {

    private final Logger logger;

    public OrderService(Logger logger) {
        this.logger = logger;
    }
}
```

Now the dependency is explicit.

---

## Testing can become harder

Imagine:

```java
PaymentService.getInstance()
```

being used throughout your code.

Replacing that dependency with a mock during testing can be more difficult than replacing an injected dependency.

With dependency injection:

```java
public PaymentService(PaymentGateway gateway) {
    this.gateway = gateway;
}
```

you can easily provide:

```java
new FakePaymentGateway()
```

during tests.

---

## Hidden dependencies

This:

```java
DatabaseManager.getInstance()
```

doesn't tell you from the constructor that `DatabaseManager` is required.

Dependency injection makes that relationship visible.

---

# 23. A practical Java example

Suppose an application has configuration that should be loaded once.

```java
public class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    private String environment;

    private AppConfig() {
        environment = "PROD";
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    public String getEnvironment() {
        return environment;
    }
}
```

Usage:

```java
public class OrderService {

    public void processOrder() {

        String environment =
                AppConfig.getInstance().getEnvironment();

        System.out.println(environment);
    }
}
```

Multiple callers:

```java
AppConfig config1 = AppConfig.getInstance();
AppConfig config2 = AppConfig.getInstance();
AppConfig config3 = AppConfig.getInstance();
```

All point to:

```text
             ┌─────────────────┐
config1 ────►│                 │
config2 ────►│   AppConfig     │
config3 ────►│   ONE OBJECT    │
             │                 │
             └─────────────────┘
```

---

# 24. Singleton implementations compared

| Implementation         | Lazy?                                        | Thread-safe? | Complexity |
| ---------------------- | -------------------------------------------- | ------------ | ---------- |
| Basic lazy             | Yes                                          | No           | Very low   |
| Synchronized method    | Yes                                          | Yes          | Low        |
| Double-checked locking | Yes                                          | Yes          | Medium     |
| Eager initialization   | No                                           | Yes          | Very low   |
| Holder idiom           | Yes                                          | Yes          | Low        |
| Enum                   | Effectively immediate enum instance creation | Yes          | Very low   |

For modern Java code, the **Holder idiom** and **enum** are important patterns to know, while eager initialization is often perfectly adequate when the object is cheap to construct.

---

# 25. What does the JVM have to do with Singleton?

A common interview follow-up is:

> "How does Java class initialization help Singleton?"

The JVM guarantees that class initialization is performed safely and only once for a given class initialization context.

That's why this works:

```java
private static final Singleton INSTANCE = new Singleton();
```

and why the Holder approach works:

```java
private static class Holder {
    private static final Singleton INSTANCE = new Singleton();
}
```

The JVM gives us synchronization guarantees during class initialization, allowing these implementations to avoid manually synchronizing `getInstance()`.

---

# 26. Interview question: "Implement Singleton"

A strong answer could be:

```java
public class Singleton {

    private Singleton() {
    }

    private static class Holder {
        private static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

Then explain:

> "The constructor is private so callers cannot instantiate the class directly. The instance is stored in a static field, and `getInstance()` provides access to it. The Holder idiom gives lazy initialization while relying on JVM class-initialization guarantees for thread safety."

That's much stronger than simply writing the code.

---

# 27. Interview question: "Why is the constructor private?"

Answer:

> "To prevent clients from creating instances using `new`. Instance creation is controlled by the Singleton class itself."

---

# 28. Interview question: "Why is the instance static?"

Answer:

> "Because the single instance belongs to the class rather than to an object of the class. A static field allows all callers to access the same stored instance."

---

# 29. Interview question: "Is this thread-safe?"

```java
private static Singleton instance;

public static Singleton getInstance() {
    if (instance == null) {
        instance = new Singleton();
    }
    return instance;
}
```

Answer:

> **No.**

Two threads can both observe `instance == null` and create separate objects.

A synchronized implementation, Holder idiom, enum, or correctly implemented double-checked locking can address concurrency.

---

# 30. Interview question: "Why volatile in double-checked locking?"

Answer:

> "`volatile` ensures visibility between threads and prevents problematic instruction reordering when publishing the Singleton instance."

That is generally the level expected in a Java interview unless the interviewer asks for a deeper Java Memory Model discussion.

---

# 31. Interview question: "Can Singleton be broken?"

Yes.

Potential mechanisms include:

```text
Reflection
Serialization / deserialization
Cloning
Multiple class loaders
```

A robust implementation needs to account for the mechanisms relevant to the application.

Enum Singleton is particularly strong against several of these concerns.

---

# 32. Singleton and Dependency Injection

This is especially important for your **Spring Boot interviews**.

Traditional Singleton:

```java
Logger.getInstance()
```

Dependency injection:

```java
@Service
public class OrderService {

    private final Logger logger;

    public OrderService(Logger logger) {
        this.logger = logger;
    }
}
```

The second approach makes the dependency explicit.

This is one reason modern frameworks often prefer **container-managed lifecycle/scope** over hand-written Singleton implementations.

---

# 33. The most important idea to remember

Don't memorize:

```java
private static Singleton instance;
```

Memorize the **design problem**:

```text
Problem:
    Multiple instances must not exist.

Solution:
    Restrict construction + maintain one instance + provide access.

Mechanism:
    private constructor
    static instance
    public access method
```

Then remember the major Java-specific concerns:

```text
                Singleton
                    │
       ┌────────────┼────────────┐
       ▼            ▼            ▼
   Creation      Threading      Breaking
       │            │            │
   private       synchronized   reflection
   constructor   volatile       serialization
                               cloning
```

---

# 34. Interview-ready summary

### What

Singleton is a **creational design pattern** that restricts a class to a single instance and provides a global access point.

### Why

Use it when having multiple instances would violate the intended design or create unnecessary coordination/state duplication.

### Core components

```java
private Singleton() {}
private static Singleton instance;
public static Singleton getInstance() {}
```

### Main concerns

```text
Thread safety
Lazy vs eager initialization
Serialization
Reflection
Cloning
Global state
Testability
```

### Common Java implementations

```text
1. Basic lazy initialization
2. Synchronized method
3. Double-checked locking + volatile
4. Eager initialization
5. Initialization-on-demand Holder
6. Enum Singleton
```

### Spring

A Spring singleton bean is:

> **One bean instance per Spring ApplicationContext by default**, rather than necessarily one object for the entire JVM.

---

# 35. One final example: see the pattern, not the syntax

Imagine a company's application has one configuration source:

```text
                   Application
                        │
          ┌─────────────┼─────────────┐
          │             │             │
          ▼             ▼             ▼
     OrderService   UserService   ReportService
          │             │             │
          └─────────────┼─────────────┘
                        ▼
                  AppConfiguration
                     ONE INSTANCE
```

Without Singleton:

```text
OrderService   → Configuration #1
UserService    → Configuration #2
ReportService  → Configuration #3
```

With Singleton:

```text
OrderService   ─┐
UserService    ─┼──→ Configuration #1
ReportService  ─┘
```

That's the essence of the pattern.

## What you should be able to answer before moving on

By the end of Singleton, you should be comfortable explaining:

**Why Singleton?**
**Why private constructor?**
**Why static instance?**
**Why is the naive version not thread-safe?**
**What is double-checked locking?**
**Why `volatile`?**
**What is the Holder idiom?**
**Why enum Singleton?**
**How can reflection/serialization/cloning affect it?**
**How is a Spring singleton different from a classic Singleton?**
**Why might dependency injection be preferable?**

Say **`next`** when you're ready for **Factory Pattern**.

