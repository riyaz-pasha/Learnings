# Java: Runtime Polymorphism (Dynamic Method Dispatch)

Runtime polymorphism is one of the most important concepts in Java OOP and is closely related to **method overriding** and **upcasting**.

---

## 1. What is Runtime Polymorphism?

**Runtime polymorphism** means:

> The method that gets executed is determined at **runtime** based on the **actual object**, not the reference type.

The key idea is:

```java
Parent reference = new Child();
reference.someMethod();
```

Even though the reference is of type `Parent`, if `Child` overrides `someMethod()`, Java executes the **Child's version**.

This is called **dynamic method dispatch**.

---

# 2. Basic Example

```java
class Animal {

    void sound() {
        System.out.println("Animal makes a sound");
    }
}

class Dog extends Animal {

    @Override
    void sound() {
        System.out.println("Dog barks");
    }
}

public class Main {

    public static void main(String[] args) {

        Animal animal = new Dog();

        animal.sound();
    }
}
```

Output:

```text
Dog barks
```

Why?

```text
Reference type             Actual object
-------------              -------------
Animal  -----------------> Dog
```

Java sees:

```java
animal.sound();
```

The reference says:

> `animal` is an `Animal`.

But the actual object is:

> `Dog`.

Since `Dog` overrides `sound()`, Java dispatches the call to:

```java
Dog.sound()
```

---

# 3. The Two Types You Must Understand

Whenever you see:

```java
Animal animal = new Dog();
```

there are **two types** involved.

### Reference type

```java
Animal
```

This determines **what members you are allowed to access through the reference**.

### Object type

```java
Dog
```

This determines **which overridden instance method gets executed**.

So:

```text
Animal animal = new Dog();
       │              │
       │              └── Actual object type
       │
       └── Reference type
```

This distinction is extremely important.

---

# 4. A More Interesting Example

```java
class Animal {

    void sound() {
        System.out.println("Animal sound");
    }

    void eat() {
        System.out.println("Animal eats");
    }
}

class Dog extends Animal {

    @Override
    void sound() {
        System.out.println("Dog barks");
    }

    void fetch() {
        System.out.println("Dog fetches");
    }
}
```

Now:

```java
Animal animal = new Dog();
```

We can do:

```java
animal.sound();
animal.eat();
```

But we cannot do:

```java
animal.fetch(); // Compilation error
```

Why?

Because the **reference type** is `Animal`.

`fetch()` isn't declared in `Animal`.

However:

```java
animal.sound();
```

executes:

```java
Dog.sound()
```

because `sound()` is overridden.

---

# 5. Think of It as Two Questions

For:

```java
Animal animal = new Dog();

animal.sound();
```

Java effectively has two separate questions:

### Question 1: Can I call `sound()`?

Look at the **reference type**:

```java
Animal
```

Does `Animal` have `sound()`?

Yes.

Therefore:

```java
animal.sound();
```

is allowed.

### Question 2: Which `sound()` should execute?

Look at the **actual object**:

```java
new Dog()
```

The object is a `Dog`.

`Dog` overrides `sound()`.

Therefore:

```java
Dog.sound()
```

executes.

---

# 6. Why Is It Called "Dynamic Dispatch"?

**Dispatch** means:

> Deciding which method implementation should handle a method call.

In:

```java
animal.sound();
```

Java has multiple possible implementations:

```text
Animal.sound()
Dog.sound()
Cat.sound()
```

The decision is made based on the actual object at runtime.

Therefore:

```text
Runtime
   ↓
actual object = Dog
   ↓
Dog.sound()
```

Hence:

> **Dynamic Method Dispatch**

---

# 7. The Classic Example

Consider:

```java
class Animal {

    void sound() {
        System.out.println("Animal sound");
    }
}

class Dog extends Animal {

    @Override
    void sound() {
        System.out.println("Dog barks");
    }
}

class Cat extends Animal {

    @Override
    void sound() {
        System.out.println("Cat meows");
    }
}
```

Now:

```java
Animal a1 = new Dog();
Animal a2 = new Cat();

a1.sound();
a2.sound();
```

Output:

```text
Dog barks
Cat meows
```

Same method call:

```java
sound()
```

Same reference type:

```java
Animal
```

Different actual objects:

```text
a1 → Dog
a2 → Cat
```

Therefore different implementations execute.

---

# 8. This Is the Real Power of Polymorphism

Imagine a method:

```java
void makeAnimalSound(Animal animal) {
    animal.sound();
}
```

We can pass different animals:

```java
makeAnimalSound(new Dog());
makeAnimalSound(new Cat());
```

The method doesn't need to know whether it received a `Dog`, `Cat`, or another subclass.

```text
                 Animal
                    │
          ┌─────────┴─────────┐
          ↓                   ↓
        Dog                  Cat
          │                   │
      sound()              sound()
      "Bark"               "Meow"
```

The method:

```java
animal.sound();
```

automatically dispatches to the correct implementation.

This is why polymorphism makes code more extensible.

---

# 9. Upcasting

Runtime polymorphism commonly uses **upcasting**:

```java
Dog dog = new Dog();

Animal animal = dog;
```

Or directly:

```java
Animal animal = new Dog();
```

You're treating a child object as its parent type.

```text
        Animal
          ▲
          │
          │ upcasting
          │
         Dog
```

This is safe because:

> Every Dog is an Animal.

But not every Animal is a Dog.

---

# 10. What Happens Without Overriding?

Consider:

```java
class Animal {

    void sound() {
        System.out.println("Animal sound");
    }
}

class Dog extends Animal {
}
```

Then:

```java
Animal animal = new Dog();

animal.sound();
```

Output:

```text
Animal sound
```

There is no `Dog.sound()` implementation.

So Java uses the inherited:

```java
Animal.sound()
```

Runtime polymorphism becomes particularly visible when the child **overrides** the method.

---

# 11. Overloading vs Overriding

This is a very common interview question.

### Method overloading

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

Which method is selected?

Primarily based on the arguments at **compile time**.

Therefore:

> **Overloading → Compile-time polymorphism**

---

### Method overriding

```java
class Animal {

    void sound() {
        System.out.println("Animal");
    }
}

class Dog extends Animal {

    @Override
    void sound() {
        System.out.println("Dog");
    }
}
```

```java
Animal animal = new Dog();

animal.sound();
```

Which implementation executes?

```text
Dog.sound()
```

It is determined at **runtime**.

Therefore:

> **Overriding → Runtime polymorphism**

---

# 12. Very Important: Fields Do NOT Behave Like Methods

This is a common interview trap.

Consider:

```java
class Parent {

    String name = "Parent";

    void show() {
        System.out.println("Parent");
    }
}

class Child extends Parent {

    String name = "Child";

    @Override
    void show() {
        System.out.println("Child");
    }
}
```

Now:

```java
Parent obj = new Child();

System.out.println(obj.name);
obj.show();
```

Output:

```text
Parent
Child
```

Why?

Fields are resolved using the **reference type**.

Methods are dynamically dispatched using the **actual object type**.

```text
                Reference Type    Actual Object
                --------------    -------------
Field            Parent.name       Child.name
                 ↓
              Parent

Method           Parent.show()     Child.show()
                                      ↓
                                    Child
```

Remember:

> **Methods are polymorphic; fields are not.**

---

# 13. Static Methods Are Also Not Dynamically Dispatched

Consider:

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
```

Now:

```java
Parent obj = new Child();

obj.show();
```

Output:

```text
Parent
```

Because static methods belong to the **class**, not the object.

They are **hidden**, not overridden.

So runtime polymorphism applies to normal **instance methods**, not:

* `static` methods
* fields
* constructors

---

# 14. `private` Methods Are Also Not Overridden

Example:

```java
class Parent {

    private void show() {
        System.out.println("Parent");
    }

    void callShow() {
        show();
    }
}

class Child extends Parent {

    void show() {
        System.out.println("Child");
    }
}
```

Now:

```java
Parent obj = new Child();

obj.callShow();
```

Output:

```text
Parent
```

The `private` method belongs only to `Parent`.

It isn't overridden by `Child`.

---

# 15. Runtime Polymorphism with Interfaces

This is extremely common in real Java applications.

```java
interface Payment {

    void pay();
}
```

Implementations:

```java
class CreditCardPayment implements Payment {

    @Override
    public void pay() {
        System.out.println("Pay using credit card");
    }
}

class UpiPayment implements Payment {

    @Override
    public void pay() {
        System.out.println("Pay using UPI");
    }
}
```

Now:

```java
Payment payment = new UpiPayment();

payment.pay();
```

Output:

```text
Pay using UPI
```

Or:

```java
Payment payment = new CreditCardPayment();

payment.pay();
```

Output:

```text
Pay using credit card
```

The interface reference doesn't care about the implementation.

---

# 16. Real-World Example

Suppose your application has:

```java
interface NotificationService {

    void send(String message);
}
```

Implementations:

```java
class EmailNotification implements NotificationService {

    @Override
    public void send(String message) {
        System.out.println("Sending email");
    }
}

class SmsNotification implements NotificationService {

    @Override
    public void send(String message) {
        System.out.println("Sending SMS");
    }
}
```

Your business logic can simply say:

```java
void notifyUser(NotificationService service) {
    service.send("Your order has shipped");
}
```

Then:

```java
notifyUser(new EmailNotification());
notifyUser(new SmsNotification());
```

The business logic doesn't need:

```java
if (type == EMAIL) {
    ...
} else if (type == SMS) {
    ...
}
```

Instead:

```text
notifyUser()
     │
     │ service.send()
     ↓
┌───────────────┐
│ Actual object │
└───────┬───────┘
        │
   ┌────┴────┐
   ↓         ↓
 Email       SMS
   │         │
   ↓         ↓
send()      send()
```

This is one of the major reasons interfaces + polymorphism are so useful in Java.

---

# 17. A Simple Mental Model

Whenever you see:

```java
Parent ref = new Child();

ref.method();
```

remember:

```text
                 ┌───────────────────────┐
                 │ Reference type        │
                 │ Parent                │
                 └──────────┬────────────┘
                            │
                      What can I call?
                            │
                            ↓
                       Parent API


                 ┌───────────────────────┐
                 │ Actual object         │
                 │ Child                 │
                 └──────────┬────────────┘
                            │
                     Which method runs?
                            │
                            ↓
                       Child.method()
```

That's the essence of runtime polymorphism.

---

# 18. Interview Definition

If an interviewer asks:

> **What is runtime polymorphism in Java?**

A good answer is:

> Runtime polymorphism is the mechanism where an overridden instance method is selected at runtime based on the actual object type rather than the reference type. In Java, this is achieved through method overriding and dynamic method dispatch.

Example:

```java
Animal animal = new Dog();
animal.sound();
```

If `Dog` overrides `sound()`, then:

```java
Dog.sound()
```

is executed.

---

# 19. The Rules to Remember

| Concept                         | Decided using                    |
| ------------------------------- | -------------------------------- |
| Which members can be accessed   | Reference type                   |
| Overridden instance method      | Actual object                    |
| Overloaded method               | Compile time                     |
| Overridden method               | Runtime                          |
| Fields                          | Reference type                   |
| Static methods                  | Reference/class type             |
| Constructors                    | Object creation, not polymorphic |
| Private methods                 | Not overridden                   |
| Interface method implementation | Actual object                    |

The single most important distinction is:

```text
Reference type → What can I access?
Actual object  → Which overridden method runs?
```

That is **runtime polymorphism / dynamic method dispatch**.

