# Topic 2 — OOP in Java

Now we move into **OOP (Object-Oriented Programming)**. This is one of the highest-value Core Java topics in interviews.

For EPAM-style interviews, don't just memorize the "4 pillars." You should be able to **explain them with Java examples and distinguish closely related concepts**.

---

## 1. What is OOP?

**Object-Oriented Programming** is a programming approach where we model a system using **objects**, which contain:

* **State** → data/fields
* **Behavior** → methods

For example:

```java
class Employee {

    private String name;
    private double salary;

    public void work() {
        System.out.println(name + " is working");
    }
}
```

An object:

```java
Employee employee = new Employee();
```

has:

```text
Employee
 ├── State
 │    ├── name
 │    └── salary
 │
 └── Behavior
      └── work()
```

---

# 2. Class vs Object

This is a basic question that can still appear as a warm-up.

### Class

A class is a **blueprint/template**.

```java
class Employee {
    String name;
    double salary;

    void work() {
        System.out.println("Working");
    }
}
```

### Object

An object is an **instance of a class**.

```java
Employee employee = new Employee();
```

So:

```text
Class
  ↓
Blueprint

Object
  ↓
Actual instance
```

Example analogy:

```text
Class  → Car design
Object → My actual Toyota
```

One class can create many objects:

```java
Employee e1 = new Employee();
Employee e2 = new Employee();
Employee e3 = new Employee();
```

---

# 3. The Four Pillars of OOP

You should know these extremely well:

```text
             OOP
              │
      ┌───────┼────────┐
      ↓       ↓        ↓
 Encapsulation Inheritance
      ↓       ↓
 Abstraction Polymorphism
```

Let's take them individually.

---

# 4. Encapsulation ⭐⭐⭐⭐⭐

**Encapsulation means bundling data and the operations that work on that data together, while controlling access to the internal state.**

Example:

```java
class BankAccount {

    private double balance;

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }
}
```

Notice:

```java
private double balance;
```

The outside world cannot directly do:

```java
account.balance = -100000;
```

Instead it must go through:

```java
account.deposit(100);
```

The class controls how its state changes.

### Why is this useful?

Because the class can enforce rules.

For example:

```java
public void withdraw(double amount) {

    if (amount <= 0) {
        throw new IllegalArgumentException();
    }

    if (amount > balance) {
        throw new IllegalArgumentException("Insufficient balance");
    }

    balance -= amount;
}
```

The caller doesn't need to know the internal implementation.

---

# 5. Is encapsulation simply "private variables + getters/setters"?

**No.**

This is a common interview trap.

This:

```java
class Employee {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

does provide access control, but blindly generating getters/setters for every field isn't necessarily good encapsulation.

For example:

```java
public void setBalance(double balance) {
    this.balance = balance;
}
```

may completely bypass business rules.

Better:

```java
public void deposit(double amount) {
    // validation
}
```

So a stronger definition is:

> Encapsulation protects an object's internal state and exposes controlled operations through a public interface.

---

# 6. Abstraction ⭐⭐⭐⭐⭐

**Abstraction means exposing essential behavior while hiding unnecessary implementation details.**

Example:

```java
interface PaymentService {

    void pay(double amount);
}
```

The caller knows:

```java
paymentService.pay(1000);
```

but doesn't need to know whether internally it uses:

* credit card
* UPI
* bank transfer
* some external API

Implementation:

```java
class UpiPaymentService implements PaymentService {

    @Override
    public void pay(double amount) {
        // UPI implementation
    }
}
```

The interface provides the abstraction.

---

# 7. Encapsulation vs Abstraction

This is **very commonly asked**.

### Encapsulation

Focuses on:

> **How do we protect/manage internal state?**

### Abstraction

Focuses on:

> **What should the outside world see?**

Think:

```text
Encapsulation
    ↓
Hide/protect internal state

Abstraction
    ↓
Hide implementation complexity
```

Example:

### Encapsulation

```java
private double balance;
```

and controlled methods.

### Abstraction

```java
interface PaymentService {
    void pay(double amount);
}
```

The caller doesn't care how payment happens.

---

# 8. Inheritance ⭐⭐⭐⭐⭐

Inheritance allows one class to acquire properties/behavior from another class.

```java
class Animal {

    void eat() {
        System.out.println("Eating");
    }
}

class Dog extends Animal {

    void bark() {
        System.out.println("Barking");
    }
}
```

Now:

```java
Dog dog = new Dog();

dog.eat();
dog.bark();
```

`Dog` inherits `eat()` from `Animal`.

Relationship:

```text
Animal
   ↑
   |
  Dog
```

This represents an **is-a relationship**:

```text
Dog is an Animal
```

---

# 9. Does Java support multiple inheritance?

Java does **not** support multiple inheritance of classes.

This is invalid:

```java
class C extends A, B {
}
```

Why?

Because of ambiguity.

Suppose:

```java
class A {
    void print() {
        System.out.println("A");
    }
}

class B {
    void print() {
        System.out.println("B");
    }
}
```

If:

```java
class C extends A, B
```

then:

```java
C c = new C();
c.print();
```

Which implementation should execute?

Java avoids this class-based ambiguity.

---

# 10. But Java DOES support multiple inheritance through interfaces

For example:

```java
interface Flyable {
    void fly();
}

interface Swimmable {
    void swim();
}

class Duck implements Flyable, Swimmable {

    @Override
    public void fly() {
    }

    @Override
    public void swim() {
    }
}
```

A class can implement multiple interfaces.

```text
       Flyable
          ↑
          |
          Duck
          |
          ↑
      Swimmable
```

More accurately, Duck implements both contracts.

---

# 11. Default-method conflict

Java interfaces can have `default` methods.

```java
interface A {

    default void print() {
        System.out.println("A");
    }
}
```

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

This creates a conflict.

Java requires `C` to resolve it:

```java
class C implements A, B {

    @Override
    public void print() {
        A.super.print();
    }
}
```

This is a good follow-up question after discussing multiple inheritance.

---

# 12. Polymorphism ⭐⭐⭐⭐⭐

Polymorphism means:

> **One interface/reference can represent different underlying implementations.**

There are two major forms you need to know:

```text
Polymorphism
     │
     ├── Compile-time
     │     └── Method Overloading
     │
     └── Runtime
           └── Method Overriding
```

---

# 13. Compile-time Polymorphism — Overloading

Example:

```java
class Calculator {

    int add(int a, int b) {
        return a + b;
    }

    int add(int a, int b, int c) {
        return a + b + c;
    }

    double add(double a, double b) {
        return a + b;
    }
}
```

Same method name:

```text
add()
```

but different parameter lists.

The compiler determines which method to call.

Therefore:

> Method overloading is compile-time polymorphism.

---

# 14. What counts as method overloading?

These are valid:

```java
void print(int x)

void print(double x)

void print(int x, int y)
```

But this is **not** sufficient:

```java
int print(int x)

double print(int x)
```

Only changing the return type doesn't create an overload.

Why?

Because:

```java
print(10);
```

would be ambiguous.

---

# 15. Runtime Polymorphism — Overriding

Example:

```java
class Animal {

    void sound() {
        System.out.println("Animal sound");
    }
}
```

```java
class Dog extends Animal {

    @Override
    void sound() {
        System.out.println("Bark");
    }
}
```

Now:

```java
Animal animal = new Dog();

animal.sound();
```

Output:

```text
Bark
```

Why?

Because the **actual object** is a `Dog`.

This is runtime polymorphism.

---

# 16. This is extremely important

Understand the difference:

```java
Animal animal = new Dog();
```

There are two types involved:

```text
Reference type → Animal
Object type    → Dog
```

The compiler sees the reference type when checking what methods are accessible.

But for an overridden instance method, Java selects the implementation based on the actual object at runtime.

This is called:

> **Dynamic method dispatch**

---

# 17. Classic interview question

```java
class Parent {

    void print() {
        System.out.println("Parent");
    }
}

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

Output?

```text
Child
```

Because `print()` is overridden and runtime dispatch selects `Child.print()`.

---

# 18. What about fields?

Now:

```java
class Parent {

    int value = 10;
}

class Child extends Parent {

    int value = 20;
}
```

Then:

```java
Parent p = new Child();

System.out.println(p.value);
```

Output:

```text
10
```

This is different from method overriding.

**Fields are not polymorphically overridden.**

Field access depends on the reference type.

That's a classic interview trap.

---

# 19. Can static methods be overridden?

No.

Static methods belong to the **class**, not the object.

They can be **hidden**, not overridden.

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

Then:

```java
Parent p = new Child();
p.print();
```

Output:

```text
Parent
```

because static method resolution is based on the reference/class, not runtime object dispatch.

This distinction is very commonly tested.

---

# 20. Can private methods be overridden?

No.

A private method isn't accessible to subclasses, so it isn't inherited in the normal overriding sense.

Example:

```java
class Parent {

    private void print() {
    }
}
```

A method with the same signature in `Child` is a separate method, not an override.

---

# 21. Can constructors be overridden?

No.

Constructors aren't inherited, so they cannot be overridden.

They can be **overloaded**:

```java
class Employee {

    Employee() {
    }

    Employee(String name) {
    }
}
```

---

# 22. Overloading vs Overriding

This is worth memorizing properly:

|              | Overloading                                   | Overriding     |
| ------------ | --------------------------------------------- | -------------- |
| Same class?  | Usually yes                                   | Parent-child   |
| Method name  | Same                                          | Same           |
| Parameters   | Must differ                                   | Must match     |
| Return type  | Can differ, subject to method signature rules | Same/covariant |
| Binding      | Compile time                                  | Runtime        |
| Polymorphism | Compile-time                                  | Runtime        |

---

# 23. Covariant return type

Suppose:

```java
class Animal {
}
```

```java
class Dog extends Animal {
}
```

Parent:

```java
class Parent {

    Animal getAnimal() {
        return new Animal();
    }
}
```

Child:

```java
class Child extends Parent {

    @Override
    Dog getAnimal() {
        return new Dog();
    }
}
```

This is valid.

The child can return a subtype of the parent's return type.

That's called a **covariant return type**.

---

# 24. Association

Now moving beyond the four pillars.

Association represents a relationship between objects.

Example:

```java
class Teacher {
}

class Student {
}
```

A teacher can teach students.

```text
Teacher ───── teaches ───── Student
```

It doesn't necessarily imply ownership.

---

# 25. Aggregation

Aggregation represents a **weak has-a relationship**.

Example:

```java
class Department {

    List<Teacher> teachers;
}
```

Teachers can exist independently of the department.

```text
Department
    |
    | has
    ↓
 Teachers
```

If the department is removed, the teachers can still exist.

---

# 26. Composition

Composition is a stronger ownership relationship.

Example:

```java
class House {

    private final Room room;

    House() {
        room = new Room();
    }
}
```

Conceptually:

```text
House
  |
  └── Room
```

The room is strongly owned by the house.

This distinction matters when discussing **object lifecycle**.

---

# 27. Composition vs Inheritance

This is an important design discussion.

### Inheritance

```java
class Car extends Vehicle
```

means:

```text
Car IS-A Vehicle
```

### Composition

```java
class Car {

    private Engine engine;
}
```

means:

```text
Car HAS-A Engine
```

A common design principle is:

> **Prefer composition over inheritance when inheritance doesn't represent a genuine subtype relationship.**

This will become much more important when we later cover **SOLID and design patterns**.

---

# 28. EPAM-style follow-up chain

An interviewer might start with:

> What are the four pillars of OOP?

You answer:

> Encapsulation, abstraction, inheritance and polymorphism.

Then they might continue:

**"What's the difference between abstraction and encapsulation?"**

Then:

**"What's runtime polymorphism?"**

Then:

**"Can static methods be overridden?"**

Then:

**"Can private methods be overridden?"**

Then:

**"What about constructors?"**

Then:

**"What's the difference between overloading and overriding?"**

Then:

**"What happens here?"**

```java
Parent p = new Child();
p.method();
```

You should be comfortable following that entire chain.

---

# 29. What I want you to master from OOP

Before moving on, you should be able to explain these **without memorized one-line definitions**:

### Must know

* Class vs Object
* Encapsulation
* Abstraction
* Inheritance
* Polymorphism
* Overloading
* Overriding
* Dynamic method dispatch
* `Parent p = new Child()`
* Static method hiding
* Private methods
* Constructors
* Covariant return types
* Interface vs class inheritance
* Multiple interfaces
* Association
* Aggregation
* Composition
* Composition vs inheritance

---

## Mini interview test

Let's make this practical. Don't worry about getting everything right.

### Q1

What will this print?

```java
class Parent {

    void show() {
        System.out.println("Parent");
    }
}

class Child extends Parent {

    @Override
    void show() {
        System.out.println("Child");
    }
}

public class Main {

    public static void main(String[] args) {

        Parent p = new Child();
        p.show();
    }
}
```

### Q2

What about this?

```java
class Parent {

    static void show() {
        System.out.println("Parent");
    }
}

class Child extends Parent {

    static void show() {
        System.out.println("Child");
    }
}

Parent p = new Child();
p.show();
```

### Q3

Explain the difference between:

```text
Encapsulation
vs
Abstraction
```

### Q4

Explain:

```text
Overloading
vs
Overriding
```

### Q5

What's the difference between:

```text
is-a
vs
has-a
```

Once you're comfortable with these, **we'll move to Topic 3: `String`, String Pool, `==`, `equals()` and immutability**. That topic has a lot of EPAM-style trick questions.

