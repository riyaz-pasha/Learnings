## 14. Java is Pass-by-Value

This is a **very common interview trap** because people often say:

> "Java is pass-by-reference for objects."

That statement is **incorrect**.

### The correct answer

> **Java is always pass-by-value.**

The confusion happens because when you pass an object to a method, the **value being copied is the reference to that object**.

Let's break this down carefully.

---

# 1. Primitive example

```java
public static void change(int x) {
    x = 100;
}

public static void main(String[] args) {
    int a = 10;

    change(a);

    System.out.println(a);
}
```

Output:

```text
10
```

Why?

Before calling:

```text
a = 10
```

When calling:

```java
change(a);
```

Java copies the value:

```text
a ──► 10

x ──► 10
```

Now:

```java
x = 100;
```

only changes `x`.

```text
a ──► 10
x ──► 100
```

Therefore:

```text
10
```

---

# 2. What happens with objects?

This is where the confusion starts.

Consider:

```java
class Employee {
    String name;
}
```

Then:

```java
public static void change(Employee employee) {
    employee.name = "Bob";
}

public static void main(String[] args) {

    Employee e = new Employee();
    e.name = "John";

    change(e);

    System.out.println(e.name);
}
```

Output:

```text
Bob
```

Some people conclude:

> "Java passed the object by reference."

No.

Java copied the **reference value**.

Before calling:

```text
e
 │
 │ reference
 ▼
Employee
name = "John"
```

When calling:

```java
change(e);
```

Java copies the reference:

```text
e ───────┐
         │
         ▼
      Employee
      name = "John"
         ▲
         │
employee ┘
```

Both `e` and `employee` contain references pointing to the **same object**.

Therefore:

```java
employee.name = "Bob";
```

modifies the shared object.

Afterward:

```text
e
 │
 ▼
Employee
name = "Bob"
 ▲
 │
employee
```

So:

```java
System.out.println(e.name);
```

prints:

```text
Bob
```

---

# 3. The critical distinction

There are two separate things:

### Reference

```java
Employee e
```

The variable contains a reference value.

### Object

```java
new Employee()
```

The actual object exists somewhere in memory.

Java passes the **reference value by value**.

That's why:

> Java is pass-by-value, even when passing objects.

---

# 4. The classic interview question

Consider:

```java
class Employee {
    String name;
}

public static void change(Employee employee) {
    employee = new Employee();
    employee.name = "Bob";
}

public static void main(String[] args) {

    Employee e = new Employee();
    e.name = "John";

    change(e);

    System.out.println(e.name);
}
```

What is the output?

```text
John
```

This is extremely important.

Why?

Initially:

```text
e ───────────────► Object A
                   name = "John"
```

Inside the method:

```java
employee = new Employee();
```

The parameter is reassigned.

Now:

```text
e ───────────────► Object A
                   name = "John"


employee ────────► Object B
                   name = null
```

Then:

```java
employee.name = "Bob";
```

changes Object B.

But `e` still points to Object A.

Therefore:

```text
John
```

---

# 5. Compare the two cases

### Case 1 — modify the object

```java
public static void change(Employee employee) {
    employee.name = "Bob";
}
```

Both references point to the same object:

```text
e ───────► Object
           name = Bob
             ▲
             │
employee ────┘
```

Result:

```text
Bob
```

---

### Case 2 — reassign the parameter

```java
public static void change(Employee employee) {
    employee = new Employee();
    employee.name = "Bob";
}
```

Now:

```text
e ───────► Object A
           name = John

employee ─► Object B
            name = Bob
```

Result:

```text
John
```

### This is the key interview distinction:

> **You can modify the object through the copied reference, but you cannot change the caller's reference by reassigning the parameter.**

---

# 6. Another example

```java
public static void modify(int[] arr) {
    arr[0] = 100;
}

public static void main(String[] args) {

    int[] numbers = {10, 20, 30};

    modify(numbers);

    System.out.println(numbers[0]);
}
```

Output:

```text
100
```

Why?

```text
numbers ──────► [10, 20, 30]
                   ▲
                   │
arr ───────────────┘
```

Both references point to the same array.

So:

```java
arr[0] = 100;
```

changes the shared array.

---

But:

```java
public static void modify(int[] arr) {
    arr = new int[]{100, 200, 300};
}
```

doesn't change what `numbers` points to.

After the assignment:

```text
numbers ──────► [10, 20, 30]

arr ──────────► [100, 200, 300]
```

So the original array remains unchanged.

---

# 7. Does Java have pass-by-reference?

No.

Java does **not** have pass-by-reference semantics like C++ references.

For example, Java does not allow something equivalent to:

```text
void change(reference to caller's variable)
```

where the method can make the caller's variable point to a completely different object.

Instead:

```text
caller variable
      │
      ▼
 reference value
      │
      ▼
    object
```

The **reference value is copied** into the parameter.

---

# 8. What about `String`?

This is a nice interview follow-up.

```java
public static void change(String s) {
    s = "World";
}

public static void main(String[] args) {

    String text = "Hello";

    change(text);

    System.out.println(text);
}
```

Output:

```text
Hello
```

Why?

Strings are immutable, and the parameter is reassigned:

```text
text ───────► "Hello"

s ──────────► "Hello"
```

Inside:

```java
s = "World";
```

Now:

```text
text ───────► "Hello"

s ──────────► "World"
```

The caller's reference wasn't changed.

---

# 9. A mutable object example

Consider:

```java
class Person {
    String name;
}
```

Then:

```java
public static void change(Person p) {
    p.name = "Alice";
}
```

This works because you're modifying the object:

```text
p.name = "Alice";
```

You're **not** changing the reference stored in the caller's variable.

---

# 10. Why do people get confused?

Because this:

```java
change(employee);
```

looks like we're passing the object itself.

Conceptually, however:

```text
employee
   │
   ▼
reference value
   │
   ▼
object
```

Java copies that reference value:

```text
employee ───────┐
                │
                ▼
             Object
                ▲
                │
parameter ──────┘
```

That's why object mutations are visible, but parameter reassignment isn't.

---

# 11. Interview-ready answer

If the interviewer asks:

> **Is Java pass-by-value or pass-by-reference?**

Say:

> **Java is always pass-by-value.** For primitive types, the actual value is copied. For objects, the value of the reference is copied, so both the caller and method parameter can refer to the same object. Therefore, modifications to the object's state are visible to the caller, but reassigning the method parameter does not change the caller's reference.

That's a very strong interview answer.

---

# 12. Quick test

What will this print?

```java
class Test {

    static void change(int x) {
        x = 20;
    }

    static void change(int[] arr) {
        arr[0] = 20;
        arr = new int[]{50};
    }

    public static void main(String[] args) {

        int x = 10;
        int[] arr = {10};

        change(x);
        change(arr);

        System.out.println(x);
        System.out.println(arr[0]);
    }
}
```

Think about it:

```text
x      → ?
arr[0] → ?
```

The answer is:

```text
10
20
```

Because:

```text
x
│
└── primitive value copied → changing parameter doesn't affect x


arr ───────► Array
              │
              └── 10

parameter ───► same Array
                 │
                 └── changed to 20
```

The later:

```java
arr = new int[]{50};
```

only changes the local parameter.

---

## The one-line mental model

```text
Primitive:
caller value ──copy──> parameter

Object:
caller reference ──copy──> parameter
        │                       │
        └──── same object ──────┘
```

**Next topic: Exception Handling** — checked vs unchecked exceptions, `try/catch/finally`, `throw` vs `throws`, custom exceptions, exception propagation, try-with-resources, and common interview traps.

---

