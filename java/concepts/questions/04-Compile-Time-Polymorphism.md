# Java: Compile-Time Polymorphism

Compile-time polymorphism is the other major form of polymorphism in Java.

The simplest definition is:

> **Compile-time polymorphism means that the method to execute is determined by the compiler at compile time, based primarily on the method signature and the arguments provided.**

In Java, this is mainly achieved through **method overloading**.

---

# 1. What is Compile-Time Polymorphism?

Consider:

```java
class Calculator {

    int add(int a, int b) {
        return a + b;
    }

    double add(double a, double b) {
        return a + b;
    }
}
```

We have multiple methods with the same name:

```text
add(int, int)
add(double, double)
```

Now:

```java
Calculator calculator = new Calculator();

calculator.add(10, 20);
calculator.add(10.5, 20.5);
```

The compiler determines which `add()` method should be called.

```text
add(10, 20)
    ↓
add(int, int)

add(10.5, 20.5)
    ↓
add(double, double)
```

This decision happens during **compilation**.

Therefore:

> **Method overloading → Compile-time polymorphism**

---

# 2. Why Is It Called "Polymorphism"?

"Poly" means:

> Many

"Morphism" means:

> Forms

So polymorphism means:

> **One interface/name, multiple forms of behavior.**

For example:

```java
add(10, 20)
add(10.5, 20.5)
add(10, 20, 30)
```

All use:

```java
add()
```

but the method can behave differently depending on the arguments.

---

# 3. Method Overloading

Method overloading means:

> Multiple methods have the **same method name** but different parameter lists.

Example:

```java
class Printer {

    void print(int value) {
        System.out.println("Integer: " + value);
    }

    void print(String value) {
        System.out.println("String: " + value);
    }

    void print(double value) {
        System.out.println("Double: " + value);
    }
}
```

Now:

```java
Printer printer = new Printer();

printer.print(10);
printer.print("Hello");
printer.print(10.5);
```

Output:

```text
Integer: 10
String: Hello
Double: 10.5
```

The compiler knows which method matches each argument.

---

# 4. What Makes Methods "Overloaded"?

The parameter list must be different.

These are valid:

```java
void add(int a, int b)

void add(double a, double b)

void add(int a, int b, int c)

void add(String a, String b)
```

Because their parameter lists differ.

---

# 5. Return Type Alone Is NOT Enough

This is **not valid**:

```java
class Calculator {

    int add(int a, int b) {
        return a + b;
    }

    double add(int a, int b) {
        return a + b;
    }
}
```

Why?

Both methods have exactly the same signature:

```text
add(int, int)
```

Changing only the return type does not create an overload.

The compiler cannot determine which one you mean:

```java
calculator.add(10, 20);
```

Should it return:

```text
int?
```

or:

```text
double?
```

Therefore, Java does not allow it.

### Important rule

> **Method overloading cannot be achieved by changing only the return type.**

---

# 6. What Does the Compiler Actually Look At?

Suppose:

```java
class Calculator {

    void add(int a, int b) {
        System.out.println("int");
    }

    void add(double a, double b) {
        System.out.println("double");
    }
}
```

And:

```java
Calculator calculator = new Calculator();

calculator.add(10, 20);
```

The compiler sees:

```text
Arguments:
10 → int
20 → int
```

It searches for the best matching method:

```text
add(int, int)
```

So it effectively decides:

```text
calculator.add(10, 20)
             ↓
       add(int, int)
```

This happens at compile time.

---

# 7. Another Example

```java
class MessageSender {

    void send(String message) {
        System.out.println("Sending text");
    }

    void send(String message, String recipient) {
        System.out.println("Sending text to " + recipient);
    }

    void send(String message, String recipient, boolean urgent) {
        System.out.println("Sending urgent message");
    }
}
```

Now:

```java
MessageSender sender = new MessageSender();

sender.send("Hello");

sender.send("Hello", "Riyaz");

sender.send("Hello", "Riyaz", true);
```

The compiler sees:

```text
send("Hello")
    ↓
send(String)

send("Hello", "Riyaz")
    ↓
send(String, String)

send("Hello", "Riyaz", true)
    ↓
send(String, String, boolean)
```

---

# 8. Different Number of Parameters

This is one common way to overload methods.

```java
class Calculator {

    int sum(int a, int b) {
        return a + b;
    }

    int sum(int a, int b, int c) {
        return a + b + c;
    }
}
```

Usage:

```java
calculator.sum(10, 20);
calculator.sum(10, 20, 30);
```

The number of arguments distinguishes the methods.

---

# 9. Different Parameter Types

Another common form:

```java
class Printer {

    void print(int value) {
        System.out.println("Integer");
    }

    void print(double value) {
        System.out.println("Double");
    }

    void print(String value) {
        System.out.println("String");
    }
}
```

Usage:

```java
printer.print(10);
printer.print(10.5);
printer.print("Hello");
```

---

# 10. Different Parameter Order

This is also valid.

```java
class Person {

    void create(String name, int age) {
        System.out.println("Name first");
    }

    void create(int age, String name) {
        System.out.println("Age first");
    }
}
```

Both are valid because:

```text
create(String, int)
create(int, String)
```

are different signatures.

---

# 11. Does Overloading Depend on the Object?

This is where compile-time and runtime polymorphism differ.

Consider:

```java
class Parent {

    void show(int value) {
        System.out.println("Parent int");
    }
}

class Child extends Parent {

    void show(String value) {
        System.out.println("Child String");
    }
}
```

Now:

```java
Parent obj = new Child();

obj.show(10);
```

The compiler looks at the **reference type**:

```text
Parent
```

It finds:

```java
show(int)
```

So:

```text
obj.show(10)
     ↓
Parent.show(int)
```

The actual object being `Child` doesn't change the overload selection.

This is an important difference from runtime polymorphism.

---

# 12. Compile-Time vs Runtime Polymorphism

This distinction is extremely important for interviews.

### Compile-time polymorphism

```java
Calculator calculator = new Calculator();

calculator.add(10, 20);
```

The compiler determines which overloaded method matches.

```text
Arguments
   ↓
Compiler
   ↓
Matching overloaded method
```

### Runtime polymorphism

```java
Animal animal = new Dog();

animal.sound();
```

The compiler knows `sound()` exists, but the overridden implementation is selected based on the actual object at runtime.

```text
Actual object
     ↓
Runtime
     ↓
Overridden method
```

---

# 13. Side-by-Side Comparison

|                           | Compile-Time                           | Runtime                           |
| ------------------------- | -------------------------------------- | --------------------------------- |
| Main mechanism            | Overloading                            | Overriding                        |
| Decision                  | Compile time                           | Runtime                           |
| Based primarily on        | Arguments/signature                    | Actual object                     |
| Inheritance required?     | No                                     | Usually yes                       |
| Same method name?         | Yes                                    | Yes                               |
| Same parameters?          | No                                     | Yes                               |
| Different implementation? | Yes                                    | Yes                               |
| Example                   | `add(int,int)` vs `add(double,double)` | `Animal.sound()` vs `Dog.sound()` |

---

# 14. Very Important Example Combining Both

Now let's combine **overloading** and **overriding**.

```java
class Animal {

    void sound() {
        System.out.println("Animal sound");
    }

    void sound(int times) {
        System.out.println("Animal sound " + times + " times");
    }
}

class Dog extends Animal {

    @Override
    void sound() {
        System.out.println("Dog barks");
    }

    @Override
    void sound(int times) {
        System.out.println("Dog barks " + times + " times");
    }
}
```

Now:

```java
Animal animal = new Dog();

animal.sound();
animal.sound(3);
```

First:

```java
animal.sound();
```

There is only:

```java
sound()
```

Then runtime dispatch chooses:

```text
Dog.sound()
```

For:

```java
animal.sound(3);
```

The compiler first resolves the overload:

```text
sound(int)
```

Then runtime dispatch determines the overridden implementation:

```text
Dog.sound(int)
```

So there are actually **two stages**:

```text
animal.sound(3)
      │
      ↓
Compile time
Which signature?
      │
      ↓
sound(int)
      │
      ↓
Runtime
Which object's implementation?
      │
      ↓
Dog.sound(int)
```

This is a very useful mental model.

---

# 15. Method Overloading and Type Conversion

Java can sometimes convert argument types to find a matching overloaded method.

Example:

```java
class Printer {

    void print(int value) {
        System.out.println("int");
    }

    void print(double value) {
        System.out.println("double");
    }
}
```

Now:

```java
Printer printer = new Printer();

short number = 10;

printer.print(number);
```

There isn't:

```java
print(short)
```

But Java can widen:

```text
short
  ↓
int
```

So:

```java
print(int)
```

is selected.

---

# 16. Widening vs Narrowing

Java can automatically perform certain **widening conversions**:

```text
byte
  ↓
short
  ↓
int
  ↓
long
  ↓
float
  ↓
double
```

For example:

```java
void print(long value) {
    System.out.println("long");
}

void print(double value) {
    System.out.println("double");
}
```

Calling:

```java
print(10);
```

can match:

```text
int → long
```

or:

```text
int → float → double
```

Java prefers the closer applicable match.

The details of overload resolution become more complex when boxing, varargs, generics, and `null` are involved, but the basic principle remains:

> The compiler chooses the applicable overload.

---

# 17. Boxing Can Also Affect Overloading

Consider:

```java
class Printer {

    void print(int value) {
        System.out.println("primitive");
    }

    void print(Integer value) {
        System.out.println("wrapper");
    }
}
```

Then:

```java
printer.print(10);
```

Output:

```text
primitive
```

because the argument is already an `int`.

But:

```java
Integer number = 10;

printer.print(number);
```

matches:

```java
print(Integer)
```

---

# 18. Varargs and Overloading

You can also overload with varargs.

```java
class Calculator {

    void calculate(int a, int b) {
        System.out.println("Two arguments");
    }

    void calculate(int... values) {
        System.out.println("Variable arguments");
    }
}
```

Now:

```java
calculator.calculate(10, 20);
```

The fixed-parameter method is preferred:

```text
calculate(int, int)
```

rather than:

```text
calculate(int...)
```

This illustrates another important rule:

> Java's overload resolution tries to choose the most specific/best applicable match.

---

# 19. `null` Can Create Ambiguity

This is a common interview question.

```java
class Printer {

    void print(String value) {
        System.out.println("String");
    }

    void print(Integer value) {
        System.out.println("Integer");
    }
}
```

Now:

```java
printer.print(null);
```

This causes a compilation error.

Why?

`null` can be assigned to both:

```text
String
Integer
```

Neither is more specific than the other.

So the compiler cannot decide.

```text
print(null)
     │
     ├── print(String)   ✓
     │
     └── print(Integer)  ✓
     
     ↓

Ambiguous!
```

---

# 20. Constructor Overloading

Compile-time polymorphism isn't limited to normal methods.

Constructors can also be overloaded.

```java
class User {

    User() {
        System.out.println("Default constructor");
    }

    User(String name) {
        System.out.println("Name: " + name);
    }

    User(String name, int age) {
        System.out.println("Name: " + name + ", Age: " + age);
    }
}
```

Now:

```java
new User();

new User("Riyaz");

new User("Riyaz", 30);
```

The compiler selects the appropriate constructor based on the arguments.

---

# 21. Does Constructor Overloading Count as Polymorphism?

You will sometimes see this discussed as compile-time polymorphism.

The underlying idea is the same:

```text
new User()
        ↓
User()

new User("Riyaz")
        ↓
User(String)

new User("Riyaz", 30)
        ↓
User(String, int)
```

The compiler determines which constructor matches.

However, in interviews, the safest answer is:

> **Compile-time polymorphism in Java is primarily achieved through method overloading. Constructor overloading is also resolved at compile time, but constructors themselves are not inherited or overridden.**

---

# 22. Overloading vs Overriding

This is one of the most important tables to remember.

| Overloading                                      | Overriding                           |
| ------------------------------------------------ | ------------------------------------ |
| Same class or inheritance hierarchy              | Requires inheritance                 |
| Same method name                                 | Same method signature                |
| Parameters must differ                           | Parameters must be same              |
| Compile-time                                     | Runtime                              |
| Return type alone cannot distinguish             | Return type may be covariant         |
| Static methods can be overloaded                 | Static methods cannot be overridden  |
| Private methods can technically be overloaded    | Private methods cannot be overridden |
| Main purpose: multiple ways to call an operation | Main purpose: specialized behavior   |

---

# 23. Simple Mental Model

For **compile-time polymorphism**, think:

```text
                 Method call
                     │
                     ↓
              What arguments?
                     │
                     ↓
                 Compiler
                     │
          ┌──────────┼──────────┐
          ↓          ↓          ↓
       int,int    double,double String
          │          │          │
          ↓          ↓          ↓
        add()      add()      add()
```

The compiler chooses the method.

For **runtime polymorphism**:

```text
                 Method call
                     │
                     ↓
              Reference type
                     │
                     ↓
              Method exists?
                     │
                     ↓
               Actual object
                     │
                     ↓
          Overridden implementation
```

---

# 24. The Most Important Interview Question

### Q: Is method overloading compile-time or runtime polymorphism?

**Answer:**

> Method overloading is compile-time polymorphism because the compiler determines which overloaded method to invoke based on the method signature and the compile-time types of the arguments.

Example:

```java
void print(int value)
void print(String value)
```

For:

```java
print(10);
```

the compiler selects:

```java
print(int)
```

---

# 25. One More Important Distinction

Don't think:

> "Compile-time polymorphism means the method literally executes at compile time."

That's incorrect.

The **decision about which overloaded method will be invoked** is made at compile time.

The actual method obviously executes when the program runs.

```text
COMPILE TIME
     │
     │ Determine method
     ↓
print(int)
     │
     │
     ↓
RUNTIME
     │
     │ Execute selected method
     ↓
print(int)
```

So the phrase **compile-time polymorphism** refers to **when the method selection is determined**, not when the method body executes.

---

# 26. Final Mental Model

You can remember both types with this simple rule:

```text
                    POLYMORPHISM
                         │
             ┌───────────┴───────────┐
             ↓                       ↓
       COMPILE TIME              RUNTIME
             │                       │
        Overloading               Overriding
             │                       │
       Same name                Same signature
       Different                Different
       parameters               implementations
             │                       │
             ↓                       ↓
       Compiler decides          Object decides
```

Or even shorter:

```text
OVERLOADING  → "Which method signature?"
                 ↓
              Compile time

OVERRIDING   → "Which object's implementation?"
                 ↓
              Runtime
```

That distinction is the key to understanding **compile-time vs runtime polymorphism** in Java.

