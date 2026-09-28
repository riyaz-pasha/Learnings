## 15. Exception Handling in Java

Exception handling is a **very common Java interview topic**. You should be comfortable with both the syntax and the underlying flow.

---

# 1. What is an exception?

An exception is an event that disrupts the normal flow of program execution.

For example:

```java
int result = 10 / 0;
```

This causes:

```text
ArithmeticException: / by zero
```

Instead of allowing the application to terminate unexpectedly, we can handle the problem:

```java
try {
    int result = 10 / 0;
} catch (ArithmeticException e) {
    System.out.println("Cannot divide by zero");
}
```

Output:

```text
Cannot divide by zero
```

---

# 2. Exception hierarchy

The basic hierarchy is:

```text
Object
  |
Throwable
  |
  +------------------+
  |                  |
 Error            Exception
                      |
              +-------+--------+
              |                |
       RuntimeException    Other Exceptions
```

The two major categories under `Throwable` are:

### `Error`

Usually represents serious JVM/system-level problems.

Examples:

```text
OutOfMemoryError
StackOverflowError
NoClassDefFoundError
```

Generally, application code shouldn't try to recover from these.

### `Exception`

Represents conditions an application may reasonably handle.

Examples:

```text
IOException
SQLException
ParseException
RuntimeException
```

---

# 3. Checked vs unchecked exceptions

This is one of the **most important interview questions**.

## Checked exceptions

Checked at **compile time**.

Examples:

```java
IOException
SQLException
FileNotFoundException
```

Example:

```java
public void readFile() throws IOException {
    Files.readString(Path.of("data.txt"));
}
```

The compiler forces you to either:

### Handle it

```java
try {
    readFile();
} catch (IOException e) {
    // handle
}
```

or:

### Declare it

```java
public void process() throws IOException {
    readFile();
}
```

---

# 4. Unchecked exceptions

Unchecked exceptions are subclasses of `RuntimeException`.

Examples:

```text
NullPointerException
IllegalArgumentException
IllegalStateException
IndexOutOfBoundsException
ArithmeticException
NumberFormatException
```

For example:

```java
String name = null;

System.out.println(name.length());
```

causes:

```text
NullPointerException
```

You don't have to write:

```java
throws NullPointerException
```

because it is unchecked.

---

# 5. Checked vs unchecked

| Checked                                                | Unchecked                              |
| ------------------------------------------------------ | -------------------------------------- |
| Checked by compiler                                    | Not required to be handled by compiler |
| Subclasses of `Exception` excluding `RuntimeException` | Subclasses of `RuntimeException`       |
| Often external/recoverable conditions                  | Often programming/validation errors    |
| Must catch or declare                                  | Catching/declaring is optional         |

Important:

> "Checked means the exception happens at compile time."

That's **not** what it means.

The exception itself happens at runtime.

"Checked" means the **compiler checks whether you've handled or declared it**.

---

# 6. `try`, `catch`, `finally`

Basic structure:

```java
try {
    // risky code
} catch (Exception e) {
    // handle exception
} finally {
    // cleanup
}
```

Example:

```java
try {
    int result = 10 / 0;
} catch (ArithmeticException e) {
    System.out.println("Division failed");
} finally {
    System.out.println("Cleanup");
}
```

Output:

```text
Division failed
Cleanup
```

---

# 7. Does `finally` always execute?

Normally, yes.

For example:

```java
try {
    System.out.println("try");
} catch (Exception e) {
    System.out.println("catch");
} finally {
    System.out.println("finally");
}
```

Possible output:

```text
try
finally
```

Even if an exception occurs:

```text
try
catch
finally
```

---

## But "always" has exceptions

`finally` may not execute if the JVM/process terminates abruptly.

For example:

```java
System.exit(0);
```

Also, catastrophic JVM/process termination can prevent it.

So the interview-safe answer is:

> `finally` normally executes whether or not an exception occurs, except in cases such as JVM termination or `System.exit()`.

---

# 8. `throw` vs `throws`

This is another very common interview question.

## `throw`

Used to **actually throw an exception**.

```java
throw new IllegalArgumentException("Age cannot be negative");
```

Example:

```java
public void setAge(int age) {

    if (age < 0) {
        throw new IllegalArgumentException("Invalid age");
    }

    this.age = age;
}
```

---

## `throws`

Used in a method signature to **declare that a method may throw exceptions**.

```java
public void readFile() throws IOException {
    // ...
}
```

### Simple distinction

```text
throw  → throws an exception
throws → declares possible exceptions
```

Example:

```java
public void process() throws IOException {

    if (...) {
        throw new IOException("Failed");
    }
}
```

---

# 9. Multiple catch blocks

You can catch different exceptions:

```java
try {
    // risky code
}
catch (IOException e) {
    // IO problem
}
catch (SQLException e) {
    // database problem
}
catch (Exception e) {
    // generic fallback
}
```

The order matters.

This is wrong:

```java
catch (Exception e) {
}
catch (IOException e) {
}
```

Because `IOException` is already covered by `Exception`.

The compiler will complain about unreachable code.

---

# 10. Multi-catch

Java 7 introduced multi-catch:

```java
try {
    // code
}
catch (IOException | SQLException e) {
    System.out.println("Operation failed");
}
```

Useful when multiple exceptions require the same handling.

---

# 11. Exception propagation

Suppose:

```java
void methodA() {
    methodB();
}

void methodB() {
    methodC();
}

void methodC() {
    throw new RuntimeException("Something went wrong");
}
```

The exception propagates backward:

```text
methodC()
   ↓
methodB()
   ↓
methodA()
   ↓
caller
```

If nobody handles it, it eventually reaches the thread's uncaught exception handler and the thread terminates.

---

# 12. Stack trace

Consider:

```java
public static void main(String[] args) {
    methodA();
}

static void methodA() {
    methodB();
}

static void methodB() {
    methodC();
}

static void methodC() {
    throw new RuntimeException("Failed");
}
```

You may see something like:

```text
Exception in thread "main" java.lang.RuntimeException: Failed
    at Test.methodC(Test.java:15)
    at Test.methodB(Test.java:11)
    at Test.methodA(Test.java:7)
    at Test.main(Test.java:3)
```

The stack trace shows where the exception propagated through the call stack.

---

# 13. Custom exceptions

You can create your own exception.

### Checked custom exception

```java
public class InsufficientBalanceException
        extends Exception {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
```

Usage:

```java
public void withdraw(double amount)
        throws InsufficientBalanceException {

    if (amount > balance) {
        throw new InsufficientBalanceException(
            "Insufficient balance"
        );
    }
}
```

Because it extends `Exception` directly, it's checked.

---

### Unchecked custom exception

```java
public class InvalidAgeException
        extends RuntimeException {

    public InvalidAgeException(String message) {
        super(message);
    }
}
```

Now callers don't have to catch or declare it.

---

# 14. When should you use checked vs unchecked?

A common practical guideline:

### Checked

Use when the caller can reasonably be expected to **recover or take an alternative action**.

For example:

```text
File doesn't exist
External resource unavailable
```

### Unchecked

Often appropriate for:

```text
Invalid arguments
Invalid application state
Programming errors
Business validation failures
```

However, this is partly a design decision. Modern Java applications, especially Spring applications, often use unchecked exceptions extensively.

---

# 15. `try-with-resources`

This is very important for modern Java.

Before Java 7, resource cleanup often looked like:

```java
FileInputStream input = null;

try {
    input = new FileInputStream("data.txt");

    // use resource

} finally {

    if (input != null) {
        input.close();
    }
}
```

This is verbose and error-prone.

Java 7 introduced:

```java
try (FileInputStream input =
         new FileInputStream("data.txt")) {

    // use input

} catch (IOException e) {
    // handle
}
```

Java automatically closes the resource.

---

# 16. What can be used with try-with-resources?

The resource must implement:

```java
AutoCloseable
```

`Closeable` extends `AutoCloseable`.

Examples:

```text
InputStream
OutputStream
Reader
Writer
Connection
PreparedStatement
ResultSet
```

---

# 17. Multiple resources

You can have:

```java
try (
    FileInputStream input = new FileInputStream("input.txt");
    FileOutputStream output = new FileOutputStream("output.txt")
) {
    // use resources
}
```

They are closed automatically.

Resources are closed in **reverse order**:

```text
output closes first
input closes second
```

This is similar to stack/LIFO behavior.

---

# 18. Suppressed exceptions

This is a slightly more advanced interview question.

Suppose:

```java
try (Resource resource = ...) {
    // exception occurs here
}
```

and then `resource.close()` also throws an exception.

Which exception is the main one?

The exception from the `try` body is generally the **primary exception**.

The exception from `close()` becomes a **suppressed exception**.

You can inspect them:

```java
for (Throwable t : e.getSuppressed()) {
    System.out.println(t);
}
```

This is one reason try-with-resources is safer than manually handling `close()`.

---

# 19. `finally` with `return`

Classic interview trap:

```java
public int test() {

    try {
        return 10;
    }
    finally {
        return 20;
    }
}
```

What does it return?

```text
20
```

The `finally` block executes before the method completes, and its `return` overrides the earlier return.

### But don't write code like this.

A `return` inside `finally` is generally considered bad practice because it can suppress exceptions and make control flow confusing.

---

# 20. `finally` and exceptions

Consider:

```java
try {
    throw new RuntimeException("Error");
}
finally {
    System.out.println("Finally");
}
```

Output:

```text
Finally
```

Then the `RuntimeException` continues propagating.

---

# 21. Exception wrapping

Suppose a lower-level API throws:

```java
SQLException
```

Your service might want to expose a domain/application exception:

```java
try {
    repository.save(data);
}
catch (SQLException e) {
    throw new DataAccessException("Unable to save data", e);
}
```

The original exception is passed as the **cause**.

This preserves the root cause.

You can inspect:

```java
e.getCause();
```

This pattern is called **exception chaining/wrapping**.

---

# 22. Don't catch `Exception` blindly

This:

```java
try {
    process();
}
catch (Exception e) {
    // ignore
}
```

is usually a bad idea.

Problems:

* hides bugs
* loses useful information
* makes debugging difficult
* may allow invalid application state to continue

Prefer catching an exception when you can actually handle it:

```java
catch (FileNotFoundException e) {
    // meaningful handling
}
```

And at minimum, preserve useful context when logging/wrapping.

---

# 23. Exception handling flow

Think about this:

```java
try {
    operation();
}
catch (IOException e) {
    handleIO();
}
catch (Exception e) {
    handleOther();
}
finally {
    cleanup();
}
```

Flow:

```text
              operation()
                  |
           exception?
             /       \
           no        yes
           |          |
           |     matching catch
           |          |
           +----+-----+
                |
             finally
                |
             continue
```

If there is no matching catch:

```text
try
 ↓
exception
 ↓
finally
 ↓
propagate to caller
```

---

# 24. Very important interview distinctions

### `throw` vs `throws`

```text
throw  → inside method body
throws → method declaration
```

### Checked vs unchecked

```text
Checked:
compiler requires catch/declare

Unchecked:
RuntimeException hierarchy
compiler doesn't require catch/declare
```

### `final` vs `finally` vs `finalize`

These are completely different.

```text
final
  → keyword
  → variable/method/class

finally
  → exception handling block

finalize()
  → old Object method related to GC
  → deprecated and should not be used
```

`finalize()` was deprecated because it has unreliable timing and causes resource-management problems; modern Java code should use explicit resource management, especially try-with-resources.

---

# 25. One very common interview question

### What happens if both `try` and `finally` throw exceptions?

Example:

```java
try {
    throw new RuntimeException("Try exception");
}
finally {
    throw new RuntimeException("Finally exception");
}
```

The exception from `finally` becomes the exception that escapes; the original one can be lost/suppressed depending on the exact construct.

This is another reason **you should generally avoid throwing from `finally`**.

With try-with-resources, Java has explicit suppressed-exception handling.

---

# 26. Interview-ready summary

If asked:

> **Explain exception handling in Java.**

A strong answer:

> Java uses `Throwable` as the root of its exception hierarchy, with `Error` and `Exception` as the major branches. Exceptions can be checked or unchecked. Checked exceptions must be caught or declared, while unchecked exceptions extend `RuntimeException` and don't have that compiler requirement. We use `try`, `catch`, and `finally` for handling and cleanup, `throw` to explicitly throw an exception, and `throws` to declare exceptions from a method. For resource management, Java provides try-with-resources, which automatically closes `AutoCloseable` resources.

---

## What you should memorize

```text
Throwable
 ├── Error
 └── Exception
      └── RuntimeException
```

```text
throw  → actually throw
throws → declare
```

```text
try
catch
finally
```

```text
try-with-resources
        ↓
AutoCloseable
        ↓
automatic close
```

And the most important distinction:

> **Checked exceptions are compiler-checked; unchecked exceptions are RuntimeException subclasses.**

**Next: Generics in Java** — `<T>`, generic classes/methods, bounded types, wildcards (`?`, `extends`, `super`), PECS, type erasure, and the common `List<Object>` vs `List<?>` interview trap.

