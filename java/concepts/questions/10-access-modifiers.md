In Java, **access modifiers** control the visibility of classes, methods, constructors, and variables—i.e., **where they can be accessed from**.

| Modifier    | Same Class | Same Package | Subclass (different package) | Other Packages |
| ----------- | ---------- | ------------ | ---------------------------- | -------------- |
| `private`   | ✅          | ❌            | ❌                            | ❌              |
| *(default)* | ✅          | ✅            | ❌                            | ❌              |
| `protected` | ✅          | ✅            | ✅                            | ❌              |
| `public`    | ✅          | ✅            | ✅                            | ✅              |

### 1. `private`

Accessible **only inside the same class**.

```java
class Student {
    private int age = 20;

    private void showAge() {
        System.out.println(age);
    }
}
```

Other classes cannot directly access `age` or `showAge()`.

### 2. Default (package-private)

If you **don't write any access modifier**, the member is accessible within the **same package**.

```java
class Student {
    int age = 20;

    void showAge() {
        System.out.println(age);
    }
}
```

Here, `age` and `showAge()` are package-private.

> `default` is **not** an access-modifier keyword. You simply omit the modifier.

### 3. `protected`

Accessible from the **same package** and from **subclasses**, including subclasses in other packages.

```java
class Animal {
    protected void makeSound() {
        System.out.println("Animal sound");
    }
}

class Dog extends Animal {
    void bark() {
        makeSound(); // allowed
    }
}
```

### 4. `public`

Accessible **from anywhere**, provided the containing class itself is accessible.

```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
}
```

### Easy way to remember

**Least → most accessible:**

`private` → default → `protected` → `public`

For encapsulation, fields are commonly kept `private`, with controlled access through methods such as getters/setters:

```java
class Employee {
    private double salary;

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public double getSalary() {
        return salary;
    }
}
```

