# Topic 11 — `static` in Java

`static` looks simple, but it's connected to **class loading, memory, initialization order, inheritance, method hiding, and JVM behavior**. It's a frequent interview topic.

---

## 1. What does `static` mean?

`static` means the member belongs to the **class**, rather than to each individual object.

For example:

```java
class Employee {

    static String company = "EPAM";
    String name;
}
```

Here:

```java
Employee.company
```

belongs to the class.

While:

```java
employee.name
```

belongs to an individual object.

Think:

```text
Employee
   |
   +── static company        ← one class-level value
   |
   +── name                   ← each object has its own
```

---

# 2. Static variable

Consider:

```java
class Employee {

    static String company = "EPAM";

    String name;

    Employee(String name) {
        this.name = name;
    }
}
```

Create two objects:

```java
Employee e1 = new Employee("John");
Employee e2 = new Employee("Alice");
```

Both refer to the same static variable:

```java
e1.company
e2.company
Employee.company
```

All refer to the same class-level state.

If you change:

```java
Employee.company = "Google";
```

then:

```java
e1.company
e2.company
```

both see:

```text
Google
```

---

# 3. Static vs instance variables

This is fundamental.

```java
class Employee {

    static String company = "EPAM";

    String name;
}
```

Creating:

```java
Employee e1 = new Employee();
Employee e2 = new Employee();
```

Conceptually:

```text
Employee class
    |
    └── company = "EPAM"
             ↑
        shared by objects


e1
 └── name

e2
 └── name
```

Each object has its own `name`.

There is one shared `company`.

---

# 4. Why use static variables?

Use them when the value logically belongs to the class rather than each object.

Examples:

```java
static final double PI = 3.14159;
```

or:

```java
class Employee {

    static int employeeCount = 0;

    Employee() {
        employeeCount++;
    }
}
```

Then:

```java
new Employee();
new Employee();
new Employee();

System.out.println(Employee.employeeCount);
```

prints:

```text
3
```

because the counter is shared.

---

# 5. Static methods

A static method belongs to the class.

```java
class MathUtils {

    static int add(int a, int b) {
        return a + b;
    }
}
```

Call it:

```java
int result = MathUtils.add(10, 20);
```

You don't need:

```java
MathUtils math = new MathUtils();
math.add(10, 20);
```

---

# 6. Why can't a static method directly access instance variables?

Consider:

```java
class Employee {

    String name;

    static void printName() {
        System.out.println(name); // compilation error
    }
}
```

Why?

Because `name` belongs to an **object**.

But `printName()` belongs to the **class**.

Which object's `name` should it use?

There could be:

```text
Employee e1 → name = "John"
Employee e2 → name = "Alice"
```

There is no implicit object associated with:

```java
Employee.printName();
```

Therefore Java doesn't allow direct instance-member access from a static context.

---

# 7. Can a static method access static variables?

Yes.

```java
class Employee {

    static int count = 0;

    static void printCount() {
        System.out.println(count);
    }
}
```

Because both belong to the class.

---

# 8. Can a static method access an instance variable somehow?

Yes, if you explicitly provide an object.

```java
class Employee {

    String name;

    static void printName(Employee employee) {
        System.out.println(employee.name);
    }
}
```

Now:

```java
Employee e = new Employee();
Employee.printName(e);
```

The static method knows which object to use.

---

# 9. Why can't we use `this` in a static method?

Because `this` refers to the **current object**.

A static method doesn't have an implicit current object.

So:

```java
static void print() {
    System.out.println(this);
}
```

is invalid.

Similarly:

```java
static void print() {
    this.name = "John";
}
```

is invalid.

### Interview answer

> `this` represents the current instance, while a static method belongs to the class and doesn't execute in the context of a particular instance.

---

# 10. Can we use `super` in a static method?

No.

`super` refers to the current object's superclass portion.

A static method has no current instance, so:

```java
static void print() {
    super.print();
}
```

is invalid.

---

# 11. Static method vs instance method

Compare:

```java
class Employee {

    static void companyPolicy() {
        System.out.println("Company policy");
    }

    void work() {
        System.out.println("Employee working");
    }
}
```

Static:

```java
Employee.companyPolicy();
```

Instance:

```java
Employee e = new Employee();
e.work();
```

Think:

```text
static method
→ class behavior

instance method
→ object behavior
```

---

# 12. Can a static method be overridden?

**No.**

This is extremely important.

Static methods are **hidden**, not overridden.

Example:

```java
class Parent {

    static void print() {
        System.out.println("Parent");
    }
}
```

```java
class Child extends Parent {

    static void print() {
        System.out.println("Child");
    }
}
```

Now:

```java
Parent p = new Child();
p.print();
```

Output:

```text
Parent
```

Why?

Because static method resolution is based on the **reference/class type**, not runtime polymorphism.

---

# 13. Compare with instance method

Instance method:

```java
class Parent {

    void print() {
        System.out.println("Parent");
    }
}
```

```java
class Child extends Parent {

    @Override
    void print() {
        System.out.println("Child");
    }
}
```

Then:

```java
Parent p = new Child();
p.print();
```

Output:

```text
Child
```

Because instance methods use runtime polymorphism.

So:

```text
Instance method
→ overriding
→ runtime dispatch

Static method
→ method hiding
→ compile-time/reference-based resolution
```

---

# 14. Can a static method be overloaded?

Yes.

This is a common trick question.

```java
class MathUtils {

    static int add(int a, int b) {
        return a + b;
    }

    static double add(double a, double b) {
        return a + b;
    }
}
```

This is perfectly valid.

Remember:

```text
overloading → yes
overriding  → no
```

for static methods.

---

# 15. Static initialization block

Java allows:

```java
class Example {

    static {
        System.out.println("Static block");
    }
}
```

This is called a **static initialization block**.

It executes when the class is initialized.

Example:

```java
class Example {

    static {
        System.out.println("A");
    }

    public static void main(String[] args) {
        System.out.println("B");
    }
}
```

Output:

```text
A
B
```

The static initialization happens before `main()` executes.

---

# 16. Why use static blocks?

They're useful for class-level initialization that requires more than a simple assignment.

For example:

```java
class Configuration {

    static Map<String, String> settings;

    static {
        settings = new HashMap<>();
        settings.put("environment", "prod");
        settings.put("region", "ap-south-1");
    }
}
```

Although in modern Java, simpler initialization mechanisms are often preferable.

---

# 17. Multiple static blocks

You can have multiple:

```java
class Example {

    static {
        System.out.println("First");
    }

    static {
        System.out.println("Second");
    }
}
```

They execute **in source-code order**.

Output:

```text
First
Second
```

---

# 18. Static variable initialization order

Consider:

```java
class Example {

    static int x = 10;

    static {
        x = 20;
    }

    static int y = 30;
}
```

Initialization occurs in textual order:

```text
x = 10
↓
static block
x = 20
↓
y = 30
```

Final values:

```text
x = 20
y = 30
```

---

# 19. Static initialization and class loading

This connects to our first topic: **JVM/class loading**.

When a class is initialized, its static initialization occurs.

A simplified lifecycle is:

```text
Class loading
     ↓
Linking
     ↓
Initialization
     ↓
Static field initialization
     ↓
Static blocks
```

The JVM initializes a class when initialization is triggered, such as through active use.

For example:

```java
Example.doSomething();
```

if `doSomething()` is a static method can trigger class initialization.

Creating an instance:

```java
new Example();
```

also triggers initialization before the constructor runs.

---

# 20. Static initialization happens once per class loader

This is an important interview detail.

Suppose:

```java
class Example {

    static {
        System.out.println("Initialized");
    }
}
```

If you create:

```java
new Example();
new Example();
new Example();
```

the static block doesn't execute three times.

It executes once for that class initialization under that class loader.

Then constructors execute for each object.

Conceptually:

```text
Class initialization
       ↓
static block        ← once

new Example()
       ↓
constructor         ← every object

new Example()
       ↓
constructor         ← every object
```

---

# 21. Static final constants

You'll frequently see:

```java
public static final int MAX_RETRIES = 3;
```

Why all three?

### `public`

Accessible from outside.

### `static`

Belongs to class.

### `final`

Cannot be reassigned.

So:

```java
Config.MAX_RETRIES
```

is a class-level constant.

---

# 22. `static final` vs `final`

Very important.

```java
final int age = 30;
```

means each object can have its own final `age`.

```java
static final int MAX_AGE = 100;
```

means one class-level constant.

Think:

```text
final
→ cannot reassign this variable

static
→ belongs to class

static final
→ one class-level value that cannot be reassigned
```

---

# 23. Is a static variable stored in the heap?

This question often causes confusion.

Don't answer:

> "Static variables are stored in the Method Area."

as an absolute modern JVM rule.

The Java Language Specification defines behavior, while exact memory layout is JVM implementation-specific.

For interview purposes, a good answer is:

> Static fields belong to the class rather than individual objects, and their runtime storage is associated with class-level runtime data managed by the JVM. Exact physical memory placement is JVM implementation-specific.

This is more accurate than memorizing an old "static variables live in Method Area" diagram.

---

# 24. Static nested class

Java allows:

```java
class Outer {

    static class Inner {
        void print() {
            System.out.println("Hello");
        }
    }
}
```

Usage:

```java
Outer.Inner obj = new Outer.Inner();
obj.print();
```

A static nested class does **not** require an instance of `Outer`.

Compare with a non-static inner class:

```java
class Outer {

    class Inner {
    }
}
```

To create it:

```java
Outer outer = new Outer();
Outer.Inner inner = outer.new Inner();
```

---

# 25. Static nested class vs inner class

### Static nested class

```java
static class Inner
```

doesn't implicitly hold an enclosing `Outer` instance.

### Inner class

```java
class Inner
```

implicitly belongs to an enclosing `Outer` instance.

This distinction is frequently useful in design questions.

---

# 26. Can static methods access instance members through an object?

Yes.

```java
class Test {

    int value = 10;

    static void print(Test test) {
        System.out.println(test.value);
    }
}
```

The restriction is specifically against **implicit** instance access.

This is invalid:

```java
static void print() {
    System.out.println(value);
}
```

This is valid:

```java
static void print(Test test) {
    System.out.println(test.value);
}
```

---

# 27. Static imports

Java also supports static imports.

Instead of:

```java
Math.max(10, 20);
```

you can write:

```java
import static java.lang.Math.max;
```

Then:

```java
max(10, 20);
```

Similarly:

```java
import static java.lang.Math.PI;
```

Then:

```java
double circumference = 2 * PI * radius;
```

Use static imports carefully because excessive use can make code less readable.

---

# 28. Static variable shared-state problem

Because static state is shared, it can create problems.

For example:

```java
class Counter {

    static int count;

    static void increment() {
        count++;
    }
}
```

If multiple threads execute:

```java
Counter.increment();
```

simultaneously, `count++` is not automatically thread-safe.

This connects to our future **multithreading/concurrency** topic.

So:

> static does not mean thread-safe.

Very important.

---

# 29. Static does not mean immutable

Another common mistake:

```java
static String name = "John";
```

This is shared, but not immutable as a variable reference.

You can do:

```java
name = "Alice";
```

`static` means:

> class-level

`final` is what prevents reassignment.

Even then, remember:

```java
static final List<String> names = new ArrayList<>();
```

The reference cannot change:

```java
names = new ArrayList<>(); // not allowed
```

but the list itself can still be mutated:

```java
names.add("John"); // allowed
```

---

# 30. Classic interview question

What does this print?

```java
class Parent {

    static {
        System.out.println("Parent static");
    }

    {
        System.out.println("Parent instance");
    }

    Parent() {
        System.out.println("Parent constructor");
    }
}

class Child extends Parent {

    static {
        System.out.println("Child static");
    }

    {
        System.out.println("Child instance");
    }

    Child() {
        System.out.println("Child constructor");
    }
}
```

Then:

```java
new Child();
```

Output:

```text
Parent static
Child static
Parent instance
Parent constructor
Child instance
Child constructor
```

Why?

Class initialization:

```text
Parent static
↓
Child static
```

Then object construction:

```text
Parent instance initializer
↓
Parent constructor
↓
Child instance initializer
↓
Child constructor
```

This is an excellent interview question because it combines:

* inheritance
* class initialization
* static blocks
* instance initializer blocks
* constructors

---

# 31. Another classic question

```java
class Test {

    static int x = 10;

    static {
        x = 20;
    }

    public static void main(String[] args) {
        System.out.println(x);
    }
}
```

Output:

```text
20
```

Because static field initialization and static blocks execute before `main()`.

---

# 32. Can an instance method access static members?

Yes.

```java
class Employee {

    static String company = "EPAM";

    void print() {
        System.out.println(company);
    }
}
```

No problem.

An instance method has access to both:

```text
static members
+
instance members
```

But a static method doesn't automatically have access to instance members.

So:

```text
Instance context
→ static + instance

Static context
→ static directly
→ instance only through an object/reference
```

This is a great mental model.

---

# 33. Static method inheritance nuance

Static methods can be inherited depending on accessibility, but they are resolved statically and can be hidden by a subclass declaration.

Example:

```java
class Parent {
    static void print() {
        System.out.println("Parent");
    }
}

class Child extends Parent {
}
```

You can call:

```java
Child.print();
```

But the inherited method is still the same static method from `Parent`.

If `Child` declares its own:

```java
static void print()
```

then it hides the parent's method.

---

# 34. Interview cheat sheet

Remember:

```text
static variable
→ one class-level variable

static method
→ class-level method

static block
→ class initialization

static nested class
→ doesn't require outer instance

static final
→ class-level constant/reference
```

And:

```text
static method:
❌ this
❌ super
❌ direct instance fields/methods

static method:
✅ static fields
✅ static methods
✅ instance members through an explicit object
```

Most importantly:

```text
static method
→ cannot be overridden
→ can be overloaded
→ can be hidden
```

---

# EPAM interview questions

Make sure you can answer these:

1. What does `static` mean?
2. Static vs instance variable?
3. Why can't a static method directly access instance variables?
4. Why can't `this` be used in a static method?
5. Can static methods be overloaded?
6. Can static methods be overridden?
7. What is method hiding?
8. When does a static block execute?
9. How many times does a static block execute?
10. What is a static nested class?
11. Static nested class vs inner class?
12. Can an instance method access static members?
13. Is a static variable thread-safe?
14. Does `static final` mean the referenced object is immutable?
15. What is the initialization order of parent/child static blocks and constructors?

### The one-line interview answer to remember

> **`static` makes a member belong to the class rather than to individual instances; static methods use class-level context and therefore cannot directly access instance state.**

**Next topic: Constructors, `this`, and `super`** — including constructor chaining, `this()` vs `super()`, initialization order, constructor inheritance, and common interview traps.

