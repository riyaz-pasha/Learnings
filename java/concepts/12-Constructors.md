# Topic 12 — Constructors, `this`, and `super`

This topic connects several things we've already covered: inheritance, object creation, `static`, instance initialization, and polymorphism.

It's also a **very common Java interview area**.

---

# 1. What is a constructor?

A constructor is a special member used to **initialize a newly created object**.

Example:

```java
class Employee {

    String name;

    Employee(String name) {
        this.name = name;
    }
}
```

When you do:

```java
Employee employee = new Employee("John");
```

the constructor initializes the new object.

---

# 2. Constructor syntax

A constructor:

* has the same name as the class
* has no return type
* is invoked during object creation
* can be overloaded
* is not inherited
* cannot be overridden

Example:

```java
class Employee {

    Employee() {
        System.out.println("Default constructor");
    }
}
```

Notice:

```java
Employee()
```

not:

```java
void Employee()
```

If you write:

```java
void Employee() {
}
```

that's actually a **method**, not a constructor.

---

# 3. Constructor vs method

This is a common interview question.

| Constructor                   | Method                               |
| ----------------------------- | ------------------------------------ |
| Same name as class            | Can have any valid name              |
| No return type                | Has return type or `void`            |
| Initializes object            | Performs behavior                    |
| Called during object creation | Called explicitly                    |
| Cannot be inherited           | Can be inherited depending on access |
| Cannot be overridden          | Can be overridden                    |
| Can be overloaded             | Can be overloaded                    |

---

# 4. Default constructor

If you don't declare **any constructor**, Java provides a no-argument constructor automatically.

Example:

```java
class Employee {

    String name;
}
```

Java effectively provides:

```java
Employee() {
    super();
}
```

So:

```java
Employee employee = new Employee();
```

works.

---

# 5. Important trap: declaring one constructor removes the automatic constructor

Consider:

```java
class Employee {

    Employee(String name) {
        this.name = name;
    }

    String name;
}
```

Now:

```java
new Employee();
```

does **not** compile.

Why?

Because once you declare a constructor, Java no longer provides the automatic no-argument constructor.

If you need both:

```java
class Employee {

    String name;

    Employee() {
    }

    Employee(String name) {
        this.name = name;
    }
}
```

---

# 6. Constructor overloading

You can have multiple constructors with different parameter lists.

```java
class Employee {

    String name;
    int age;

    Employee() {
    }

    Employee(String name) {
        this.name = name;
    }

    Employee(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

Then:

```java
new Employee();
new Employee("John");
new Employee("John", 30);
```

All are valid.

This is **constructor overloading**.

---

# 7. What is `this`?

`this` refers to the **current object**.

Example:

```java
class Employee {

    String name;

    Employee(String name) {
        this.name = name;
    }
}
```

There are two variables called `name`:

```text
this.name
   ↓
instance variable

name
   ↓
constructor parameter
```

Therefore:

```java
this.name = name;
```

means:

> Put the parameter `name` into the current object's `name` field.

---

# 8. Why do we need `this`?

Without `this`:

```java
class Employee {

    String name;

    Employee(String name) {
        name = name;
    }
}
```

Both sides refer to the constructor parameter.

The instance field remains unchanged.

Using:

```java
this.name = name;
```

removes the ambiguity.

---

# 9. `this` can call another constructor

This is called **constructor chaining**.

```java
class Employee {

    String name;
    int age;

    Employee() {
        this("Unknown", 0);
    }

    Employee(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

When:

```java
new Employee();
```

executes:

```text
Employee()
   ↓
this("Unknown", 0)
   ↓
Employee(String, int)
```

---

# 10. Rule: `this()` must be the first statement

This is very important.

Correct:

```java
Employee() {
    this("Unknown");
}
```

Incorrect:

```java
Employee() {
    System.out.println("Hello");

    this("Unknown"); // compilation error
}
```

`this(...)` must be the **first statement** in the constructor.

---

# 11. Why must `this()` be first?

Because Java wants constructor initialization to follow a well-defined chain.

For example:

```java
Employee() {
    this("John");
}
```

The delegated constructor must initialize the object before the current constructor adds any additional initialization.

This prevents ambiguous or repeated constructor initialization paths.

---

# 12. What is `super`?

`super` refers to the **immediate superclass portion** of the current object.

It is commonly used for:

1. Calling the parent constructor
2. Calling the parent method
3. Accessing a parent field

---

# 13. `super()` calls parent constructor

Example:

```java
class Person {

    Person() {
        System.out.println("Person constructor");
    }
}
```

Child:

```java
class Employee extends Person {

    Employee() {
        super();
        System.out.println("Employee constructor");
    }
}
```

Then:

```java
new Employee();
```

prints:

```text
Person constructor
Employee constructor
```

---

# 14. `super()` must also be first

Just like `this()`:

```java
Employee() {
    super();
}
```

is valid.

But:

```java
Employee() {
    System.out.println("Hello");
    super(); // compilation error
}
```

is invalid.

---

# 15. What if you don't write `super()`?

Java implicitly inserts:

```java
super();
```

if there is no explicit constructor invocation as the first statement.

For example:

```java
class Employee extends Person {

    Employee() {
        System.out.println("Employee");
    }
}
```

is conceptually:

```java
class Employee extends Person {

    Employee() {
        super();
        System.out.println("Employee");
    }
}
```

---

# 16. The important problem with implicit `super()`

Suppose parent only has:

```java
class Person {

    Person(String name) {
        System.out.println(name);
    }
}
```

There is **no no-argument constructor**.

Now:

```java
class Employee extends Person {

    Employee() {
    }
}
```

Compilation fails.

Why?

Java tries to insert:

```java
super();
```

but `Person()` doesn't exist.

You must explicitly call:

```java
class Employee extends Person {

    Employee() {
        super("John");
    }
}
```

---

# 17. `this()` vs `super()`

This is worth memorizing.

### `this()`

Calls another constructor of the **same class**.

```java
this(...)
```

### `super()`

Calls a constructor of the **parent class**.

```java
super(...)
```

Think:

```text
this()
 ↓
current class

super()
 ↓
parent class
```

---

# 18. Can `this()` and `super()` both be used in one constructor?

Not directly.

For example:

```java
Employee() {
    this("John");
    super(); // compilation error
}
```

You can't call both because each must be the first statement.

But constructor chaining can indirectly involve both.

Example:

```java
class Employee extends Person {

    Employee() {
        this("John");
    }

    Employee(String name) {
        super(name);
    }
}
```

Flow:

```text
Employee()
   ↓
this("John")
   ↓
Employee(String)
   ↓
super(name)
   ↓
Person(String)
```

---

# 19. `super.method()`

Suppose:

```java
class Person {

    void print() {
        System.out.println("Person");
    }
}
```

Child:

```java
class Employee extends Person {

    @Override
    void print() {
        System.out.println("Employee");
    }

    void printBoth() {
        super.print();
        this.print();
    }
}
```

Calling:

```java
new Employee().printBoth();
```

prints:

```text
Person
Employee
```

`super.print()` explicitly calls the parent implementation.

---

# 20. `super.field`

Suppose:

```java
class Parent {

    String name = "Parent";
}
```

```java
class Child extends Parent {

    String name = "Child";

    void print() {
        System.out.println(name);
        System.out.println(this.name);
        System.out.println(super.name);
    }
}
```

Output:

```text
Child
Child
Parent
```

Because:

```text
name
this.name
   ↓
Child.name

super.name
   ↓
Parent.name
```

This is **field hiding**, not polymorphic overriding.

---

# 21. Constructor execution order

This is extremely important.

Suppose:

```java
class Parent {

    Parent() {
        System.out.println("Parent constructor");
    }
}
```

```java
class Child extends Parent {

    Child() {
        System.out.println("Child constructor");
    }
}
```

Then:

```java
new Child();
```

executes:

```text
Parent constructor
Child constructor
```

Why?

The parent portion of the object must be initialized before the child portion.

---

# 22. Full initialization order

Now let's combine everything.

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
```

```java
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

This combines our previous `static` topic with constructors.

---

# 23. Why does `Parent` static execute first?

Because the parent class must be initialized before the child class.

So:

```text
Parent class initialization
        ↓
Child class initialization
```

Then when creating the object:

```text
Parent instance initialization
        ↓
Parent constructor
        ↓
Child instance initialization
        ↓
Child constructor
```

---

# 24. Instance initializer block

This:

```java
class Employee {

    {
        System.out.println("Instance initializer");
    }
}
```

is an **instance initializer block**.

It runs when an object is created, before the constructor body.

For a simple class:

```java
class Employee {

    {
        System.out.println("Initializer");
    }

    Employee() {
        System.out.println("Constructor");
    }
}
```

Output:

```text
Initializer
Constructor
```

---

# 25. Multiple instance initializer blocks

They execute in source order.

```java
class Employee {

    {
        System.out.println("First");
    }

    {
        System.out.println("Second");
    }

    Employee() {
        System.out.println("Constructor");
    }
}
```

Output:

```text
First
Second
Constructor
```

---

# 26. Complete initialization sequence

For a newly created object, a useful interview-level model is:

```text
Class initialization if required
        ↓
Memory allocated / object state prepared
        ↓
Instance fields / instance initializers
        ↓
Parent constructor
        ↓
Child instance initialization
        ↓
Child constructor
```

More precisely, constructor invocation involves superclass constructor processing first, with each class's instance field initializers and instance initializer blocks running before that class's constructor body.

For interview questions, remember the practical order:

```text
Parent instance initialization
        ↓
Parent constructor
        ↓
Child instance initialization
        ↓
Child constructor
```

---

# 27. Can constructors be inherited?

**No.**

This is important.

If:

```java
class Parent {
    Parent(int x) {}
}
```

and:

```java
class Child extends Parent {
}
```

`Child` does not inherit the `Parent(int)` constructor.

Constructors belong specifically to the class that declares them.

The child must define an appropriate constructor and call the parent constructor.

---

# 28. Can constructors be overridden?

No.

Because constructors aren't inherited.

Therefore:

```text
constructor
→ cannot be inherited
→ cannot be overridden
→ can be overloaded
```

---

# 29. Can a constructor be `final`?

No.

```java
final Employee() {
}
```

is invalid.

`final` prevents overriding, but constructors cannot be overridden anyway.

So `final constructor` doesn't make sense.

---

# 30. Can a constructor be `static`?

No.

Constructors initialize object instances, while `static` means class-level.

So:

```java
static Employee() {
}
```

is invalid.

---

# 31. Can a constructor be `abstract`?

No.

An abstract method has no implementation and must be overridden.

A constructor can't be overridden, so:

```java
abstract Employee() {
}
```

is invalid.

---

# 32. Constructor and polymorphism

Here's an important trap.

Avoid calling overridable methods from constructors.

Example:

```java
class Parent {

    Parent() {
        print();
    }

    void print() {
        System.out.println("Parent");
    }
}
```

```java
class Child extends Parent {

    private String name = "John";

    @Override
    void print() {
        System.out.println(name);
    }
}
```

Now:

```java
new Child();
```

The parent constructor executes before the child fields/constructor have completed initialization.

The overridden `Child.print()` can execute before `Child` is fully initialized.

That can produce unexpected behavior, such as:

```text
null
```

instead of `"John"`.

### Interview rule

> Avoid calling overridable methods from constructors because the subclass may not be fully initialized yet.

This is a very good interview point.

---

# 33. `this` can refer to the current object

Example:

```java
class Employee {

    void print() {
        System.out.println(this);
    }
}
```

If:

```java
Employee e = new Employee();
e.print();
```

then inside `print()`:

```text
this == e
```

So:

```java
this.name
```

means:

> `name` belonging to the current object.

---

# 34. `this` can be passed to another method

```java
class Employee {

    void process() {
        save(this);
    }

    void save(Employee employee) {
        System.out.println(employee);
    }
}
```

Here:

```java
save(this);
```

passes the current object.

---

# 35. `this` can be returned

This is common in fluent APIs.

```java
class Employee {

    String name;

    Employee setName(String name) {
        this.name = name;
        return this;
    }
}
```

Then:

```java
Employee employee = new Employee()
        .setName("John");
```

Because `setName()` returns the current object.

---

# 36. Constructor chaining example

A clean real-world example:

```java
class Employee {

    private String name;
    private int age;
    private String department;

    Employee() {
        this("Unknown", 0, "Unknown");
    }

    Employee(String name) {
        this(name, 0, "Unknown");
    }

    Employee(String name, int age, String department) {
        this.name = name;
        this.age = age;
        this.department = department;
    }
}
```

Instead of duplicating initialization logic across three constructors, all paths eventually reach one constructor.

This is a good use of `this()`.

---

# 37. Common interview output question

What is the output?

```java
class Parent {

    Parent() {
        System.out.println("P");
    }
}

class Child extends Parent {

    Child() {
        System.out.println("C");
    }
}
```

```java
new Child();
```

Answer:

```text
P
C
```

because the parent constructor executes first.

---

# 38. Another common question

What happens here?

```java
class Parent {

    Parent(int x) {
    }
}

class Child extends Parent {

    Child() {
    }
}
```

Compilation error.

Why?

Because Java implicitly tries:

```java
super();
```

but:

```java
Parent()
```

doesn't exist.

Correct:

```java
class Child extends Parent {

    Child() {
        super(10);
    }
}
```

---

# 39. `this()` vs `super()` cheat sheet

```text
this()
   ↓
calls another constructor
in the same class

super()
   ↓
calls constructor
of immediate parent
```

Both:

```text
→ must be first statement
→ cannot both appear directly in same constructor
```

And:

```text
this
   ↓
current object

super
   ↓
parent portion / superclass members
```

---

# EPAM interview questions

Make sure you can answer these:

1. What is a constructor?
2. Constructor vs method?
3. What happens if no constructor is declared?
4. What happens after declaring a parameterized constructor?
5. Can constructors be overloaded?
6. Can constructors be inherited?
7. Can constructors be overridden?
8. Why can't constructors be static/final/abstract?
9. What is `this`?
10. What is `super`?
11. `this()` vs `super()`?
12. Why must `this()`/`super()` be the first statement?
13. What happens if the parent has no no-argument constructor?
14. What is constructor chaining?
15. What is the initialization order in inheritance?
16. What is an instance initializer block?
17. Why should you avoid calling overridable methods from constructors?

### The most important mental model

When you do:

```java
new Child();
```

think:

```text
             CLASS INITIALIZATION
                     ↓
              Parent static
                     ↓
               Child static
                     ↓
             OBJECT INITIALIZATION
                     ↓
        Parent instance initialization
                     ↓
             Parent constructor
                     ↓
        Child instance initialization
                     ↓
             Child constructor
```

And:

```text
this()
→ another constructor in same class

super()
→ constructor in parent class

this
→ current object

super
→ parent members
```

**Next topic: Immutability in Java** — how to create an immutable class, why `String` is immutable, defensive copies, `final`, mutable fields, and the interview question "Is `final` enough to make an object immutable?"

