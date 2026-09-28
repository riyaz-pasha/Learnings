## 13. Immutability in Java

This is a **very common Java interview topic**, especially around `String`, `final`, collections, and thread safety.

### 1. What is an immutable object?

An object is **immutable** if its state cannot be changed after the object is created.

Example:

```java
String name = "John";

name = name + " Doe";
```

It looks like we changed `name`, but we didn't change the original `"John"` object.

A **new String object** is created:

```text
"John"  → unchanged

name
 ↓
"John Doe"
```

---

## 2. How do you create an immutable class?

Suppose we have:

```java
class Employee {
    private String name;
    private int age;

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
}
```

This is **not necessarily immutable** because the fields aren't `final`, and more importantly, if we have mutable fields, callers could modify the internal state.

A typical immutable class looks like:

```java
public final class Employee {

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
}
```

Important characteristics:

1. Class should generally be `final`
2. Fields should generally be `private`
3. Fields should generally be `final`
4. Initialize fields through the constructor
5. Don't provide setters
6. Don't expose mutable internal objects directly
7. Make defensive copies when necessary

---

# 3. Why `final` alone doesn't make an object immutable

This is an **important interview trap**.

Consider:

```java
final List<String> names = new ArrayList<>();

names.add("John");
names.add("Alice");
```

This is completely legal.

`final` means:

> The variable `names` cannot point to another List.

It does **not** mean:

> The List object cannot change.

So:

```java
final List<String> names = new ArrayList<>();
```

means:

```text
names ───────► ArrayList
                 │
                 ├── "John"
                 └── "Alice"
```

You cannot do:

```java
names = new ArrayList<>(); // ❌
```

But you can do:

```java
names.add("Bob");          // ✅
```

### Interview answer

> `final` prevents reassignment of a reference; it does not make the referenced object immutable.

---

# 4. The defensive copy problem

Consider:

```java
public final class Employee {

    private final List<String> skills;

    public Employee(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getSkills() {
        return skills;
    }
}
```

Looks immutable, but it isn't.

The caller can do:

```java
List<String> skills = new ArrayList<>();
skills.add("Java");

Employee employee = new Employee(skills);

skills.add("Spring");
```

Now `employee.skills` also contains `"Spring"`.

Why?

Because both references point to the same mutable list:

```text
skills ──────────────┐
                     ▼
                  ArrayList
                     ▲
                     │
employee.skills ─────┘
```

---

# 5. Defensive copy

Create a copy inside the constructor:

```java
public final class Employee {

    private final List<String> skills;

    public Employee(List<String> skills) {
        this.skills = new ArrayList<>(skills);
    }

    public List<String> getSkills() {
        return new ArrayList<>(skills);
    }
}
```

Now:

```java
List<String> skills = new ArrayList<>();
skills.add("Java");

Employee employee = new Employee(skills);

skills.add("Spring");
```

The employee's internal list remains unchanged.

And:

```java
employee.getSkills().add("Docker");
```

also doesn't modify the internal list because the getter returns a copy.

---

# 6. Better approach: unmodifiable view/copy

Modern Java provides:

```java
List.copyOf(skills)
```

So:

```java
public final class Employee {

    private final List<String> skills;

    public Employee(List<String> skills) {
        this.skills = List.copyOf(skills);
    }

    public List<String> getSkills() {
        return skills;
    }
}
```

Now the internal list cannot be modified through the returned reference.

For example:

```java
employee.getSkills().add("Docker");
```

throws:

```text
UnsupportedOperationException
```

### Important distinction

There is a subtle difference between:

```java
Collections.unmodifiableList(list)
```

and:

```java
List.copyOf(list)
```

`unmodifiableList` creates an **unmodifiable view** of the original list.

```text
original list
     ▲
     │
unmodifiable view
```

If someone still has the original list and modifies it, the view can reflect those changes.

`List.copyOf()` creates an **unmodifiable copy**, which is generally better for immutable objects.

---

# 7. What about mutable objects inside an immutable class?

This is another common interview question.

Consider:

```java
public final class Employee {

    private final Address address;

    public Employee(Address address) {
        this.address = address;
    }

    public Address getAddress() {
        return address;
    }
}
```

Suppose:

```java
class Address {
    private String city;

    public void setCity(String city) {
        this.city = city;
    }
}
```

Then:

```java
Address address = new Address();
address.setCity("Hyderabad");

Employee employee = new Employee(address);

address.setCity("Mumbai");
```

The `Employee` object's state has effectively changed.

So simply doing:

```java
private final Address address;
```

is **not enough**.

You need to ensure `Address` itself is immutable, or make a defensive copy.

---

# 8. Why is `String` immutable?

This is one of the most frequently asked follow-ups.

There are several important reasons.

### String pool

Java can safely share String objects:

```java
String a = "Java";
String b = "Java";
```

Both can refer to the same pooled object.

If Strings were mutable:

```java
a = "Java";
b = "Java";
```

and someone changed the shared object through `a`, `b` could unexpectedly change too.

Immutability makes sharing safe.

### Security

Strings are commonly used for things like:

```text
file paths
URLs
class names
database connection information
configuration values
```

If a String could change after validation, security checks could become unreliable.

### Hashing

Strings are frequently used as keys:

```java
Map<String, Integer> map = new HashMap<>();

map.put("Java", 10);
```

If the String's contents could change after insertion, its `hashCode()` could change and the HashMap could have trouble locating the key.

### Thread safety

Immutable objects can safely be shared between threads because their state cannot change.

---

# 9. Is an immutable object automatically thread-safe?

For an **effectively immutable object**, yes, generally it can safely be shared between threads without synchronization for its state.

Example:

```java
public final class Employee {

    private final String name;
    private final int age;

    public Employee(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

Once safely constructed and published, its state cannot change.

This is one reason immutable objects are extremely useful in concurrent applications.

---

# 10. Immutable class example — interview-ready

A good answer could be:

```java
public final class Employee {

    private final String name;
    private final int age;
    private final List<String> skills;

    public Employee(String name, int age, List<String> skills) {
        this.name = name;
        this.age = age;
        this.skills = List.copyOf(skills);
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public List<String> getSkills() {
        return skills;
    }
}
```

Here:

* `final class` → prevents subclassing
* `private` fields → encapsulation
* `final` fields → no reassignment
* no setters → no mutation API
* `List.copyOf()` → defensive immutable copy
* `String` itself is immutable

---

## 11. Why make the class `final`?

Suppose:

```java
class Employee {
    private final String name;

    public String getName() {
        return name;
    }
}
```

Someone could extend it:

```java
class SpecialEmployee extends Employee {

    // potentially introduce mutable state
}
```

More importantly, inheritance can complicate the guarantees of immutability.

Therefore, making an immutable class `final` is a common and safe design choice.

---

## 12. Common interview traps

### ❌ "If all fields are final, the class is immutable."

Not necessarily.

```java
private final List<String> names;
```

The list itself can still be mutable.

---

### ❌ "Private fields make the class immutable."

No.

You could still have:

```java
private List<String> names;

public void addName(String name) {
    names.add(name);
}
```

The object is mutable.

---

### ❌ "No setters means immutable."

Not necessarily.

A getter can expose mutable internal state:

```java
public List<String> getNames() {
    return names;
}
```

The caller can modify it.

---

### ❌ "final means constant."

Not always.

```java
final Employee employee = new Employee();
```

The reference can't change, but the Employee object may still be mutable.

---

# Interview question you should remember

If an interviewer asks:

> **How would you create an immutable class in Java?**

A strong answer:

> I would make the class final, keep its fields private and final, initialize them through the constructor, avoid setters, and ensure mutable fields are protected using defensive copies or immutable types. I would also avoid exposing mutable internal state through getters.

And if they ask:

> **Is final enough to make an object immutable?**

Answer:

> No. `final` prevents reassignment of a reference, but the referenced object can still be mutable. Immutability requires that the object's observable state cannot be changed after construction.

### Mental model

```text
final reference
      │
      ▼
   Object
   ┌───────────┐
   │ can change│  ← possible!
   └───────────┘

immutable object
      │
      ▼
   Object
   ┌──────────────┐
   │ never changes│
   └──────────────┘
```

**Next topic: Java is Pass-by-Value** — one of the most commonly misunderstood Java interview questions, especially around objects, references, and method arguments.

