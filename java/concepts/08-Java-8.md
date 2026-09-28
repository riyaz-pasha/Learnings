# Topic 8 — `Optional` in Java 8

`Optional` is a very common Java interview topic, especially in Java 8 + Streams discussions.

---

## 1. What is `Optional`?

`Optional<T>` is a container that may or may not contain a value.

Instead of returning:

```java
return null;
```

you can return:

```java
return Optional.ofNullable(value);
```

The idea is to make the possibility of **absence of a value explicit**.

### Example

Without `Optional`:

```java
public User findUser(Long id) {
    return userRepository.findById(id); // might return null
}
```

Caller:

```java
User user = findUser(1L);

if (user != null) {
    System.out.println(user.getName());
}
```

With `Optional`:

```java
public Optional<User> findUser(Long id) {
    return Optional.ofNullable(user);
}
```

Caller:

```java
Optional<User> user = findUser(1L);

if (user.isPresent()) {
    System.out.println(user.get().getName());
}
```

But we'll see that `isPresent()` + `get()` is often **not the best way** to use Optional.

---

# 2. Why was `Optional` introduced?

Primarily to make absence explicit and reduce accidental null handling.

The classic problem:

```java
User user = getUser();

System.out.println(user.getAddress().getCity().getName());
```

If any intermediate value is `null`:

```text
NullPointerException
```

Optional allows you to express the possibility of absence in the API.

---

# 3. Creating Optional

There are three important methods.

### `Optional.of()`

Use when you **know the value is not null**.

```java
String name = "John";

Optional<String> optional = Optional.of(name);
```

If you do:

```java
Optional.of(null);
```

you get:

```text
NullPointerException
```

---

### `Optional.ofNullable()`

Use when the value **may be null**.

```java
String name = getName();

Optional<String> optional = Optional.ofNullable(name);
```

If `name == null`:

```java
Optional.empty()
```

is returned.

This is probably the most commonly used creation method.

---

### `Optional.empty()`

Creates an empty Optional.

```java
Optional<String> optional = Optional.empty();
```

Conceptually:

```text
Optional
 ├── value
 └── or empty
```

---

# 4. `isPresent()`

Checks whether a value exists.

```java
Optional<String> name = Optional.of("John");

if (name.isPresent()) {
    System.out.println(name.get());
}
```

Output:

```text
John
```

For an empty Optional:

```java
Optional<String> name = Optional.empty();

System.out.println(name.isPresent());
```

Output:

```text
false
```

---

# 5. `get()` — important interview trap

You can retrieve the value using:

```java
optional.get();
```

Example:

```java
Optional<String> name = Optional.of("John");

String value = name.get();
```

But:

```java
Optional<String> name = Optional.empty();

name.get();
```

throws:

```text
NoSuchElementException
```

Therefore:

```java
if (optional.isPresent()) {
    optional.get();
}
```

is sometimes valid, but it often defeats the purpose of using Optional.

Prefer methods such as:

```java
ifPresent()
orElse()
orElseGet()
orElseThrow()
```

---

# 6. `orElse()`

Provides a default value if the Optional is empty.

```java
Optional<String> name = Optional.empty();

String result = name.orElse("Unknown");
```

Result:

```text
Unknown
```

If value exists:

```java
Optional<String> name = Optional.of("John");

String result = name.orElse("Unknown");
```

Result:

```text
John
```

---

# 7. `orElse()` vs `orElseGet()` — VERY important

This is a common interview question.

Consider:

```java
String result = optional.orElse(getDefaultValue());
```

The argument to `orElse()` is evaluated **even if the Optional contains a value**.

For example:

```java
private String getDefaultValue() {
    System.out.println("Creating default");
    return "Unknown";
}
```

Then:

```java
Optional<String> name = Optional.of("John");

String result = name.orElse(getDefaultValue());
```

Output:

```text
Creating default
```

even though `"John"` exists.

---

### `orElseGet()`

```java
String result = name.orElseGet(() -> getDefaultValue());
```

The supplier is executed **only when the Optional is empty**.

So:

```java
Optional.of("John")
```

means:

```text
orElse()     → default expression evaluated
orElseGet()  → default expression NOT evaluated
```

### Interview answer

> `orElse()` eagerly evaluates its argument, while `orElseGet()` evaluates the supplier lazily only when the Optional is empty.

This distinction becomes important when the fallback operation is expensive or has side effects.

---

# 8. `orElseThrow()`

Use when absence should result in an exception.

Modern Java:

```java
String name = optional.orElseThrow();
```

If empty:

```text
NoSuchElementException
```

You can also specify an exception:

```java
String name = optional.orElseThrow(
        () -> new IllegalArgumentException("User not found")
);
```

This is particularly useful in service-layer code:

```java
User user = userRepository.findById(id)
        .orElseThrow(() -> new UserNotFoundException(id));
```

This is one of the most common real-world uses of Optional in Spring applications.

---

# 9. `ifPresent()`

Execute something only when a value exists.

```java
Optional<String> name = Optional.of("John");

name.ifPresent(value -> System.out.println(value));
```

Or using method reference:

```java
name.ifPresent(System.out::println);
```

This avoids:

```java
if (name.isPresent()) {
    System.out.println(name.get());
}
```

Prefer:

```java
name.ifPresent(System.out::println);
```

---

# 10. `ifPresentOrElse()`

Available since Java 9.

```java
optional.ifPresentOrElse(
        value -> System.out.println(value),
        () -> System.out.println("No value")
);
```

So:

```text
value exists
    ↓
first lambda

value absent
    ↓
second lambda
```

---

# 11. `map()`

This is where Optional becomes especially powerful.

Suppose:

```java
Optional<String> name = Optional.of("john");
```

You want the length.

You could do:

```java
Optional<Integer> length = name.map(String::length);
```

Result:

```text
Optional[4]
```

The important idea:

```text
Optional<T>
    ↓ map(Function<T,R>)
Optional<R>
```

Example:

```java
Optional<String> name = Optional.of("John");

Optional<Integer> length =
        name.map(String::length);
```

---

## What happens if Optional is empty?

```java
Optional<String> name = Optional.empty();

Optional<Integer> length =
        name.map(String::length);
```

Result:

```text
Optional.empty()
```

The mapping function is not executed.

---

# 12. `filter()`

You can filter the value inside an Optional.

```java
Optional<Integer> age = Optional.of(25);

Optional<Integer> result =
        age.filter(a -> a >= 18);
```

Result:

```text
Optional[25]
```

If:

```java
Optional<Integer> age = Optional.of(15);

Optional<Integer> result =
        age.filter(a -> a >= 18);
```

Result:

```text
Optional.empty()
```

Think:

```text
Optional
   ↓
filter(predicate)
   ↓
value remains OR Optional.empty()
```

---

# 13. `map()` vs `filter()`

Very important distinction:

### `map()`

Transforms the value.

```java
Optional<String> name = Optional.of("John");

Optional<Integer> result =
        name.map(String::length);
```

```text
String → Integer
```

### `filter()`

Decides whether to keep the existing value.

```java
Optional<String> name = Optional.of("John");

Optional<String> result =
        name.filter(n -> n.length() > 3);
```

```text
String → String or empty
```

Easy way to remember:

> `map` = transform
> `filter` = keep/remove

---

# 14. `flatMap()`

This is another common interview question.

Suppose:

```java
Optional<User> user;
```

And:

```java
Optional<Address> getAddress(User user)
```

If you use `map()`:

```java
Optional<Optional<Address>> result =
        user.map(User::getAddress);
```

You get:

```text
Optional<Optional<Address>>
```

That's usually undesirable.

Instead:

```java
Optional<Address> result =
        user.flatMap(User::getAddress);
```

`flatMap()` prevents nested Optional.

### Remember:

```text
map:
Optional<T> → Optional<R>

flatMap:
Optional<T> → Optional<R>
```

where the mapping function itself returns an Optional.

---

# 15. Example: chaining Optional

Consider:

```java
class User {
    Address getAddress() {
        ...
    }
}

class Address {
    String getCity() {
        ...
    }
}
```

You can write:

```java
Optional<User> user = findUser();

Optional<String> city =
        user
            .map(User::getAddress)
            .map(Address::getCity);
```

If:

```text
User exists
    ↓
Address exists
    ↓
City exists
```

you get the city.

If User is absent:

```text
Optional.empty()
```

The chain stops safely.

---

# 16. `map()` vs `flatMap()` — interview example

Suppose:

```java
Optional<User> user;
```

Method:

```java
Optional<Address> getAddress(User user)
```

Then:

```java
user.map(User::getAddress)
```

produces:

```java
Optional<Optional<Address>>
```

while:

```java
user.flatMap(User::getAddress)
```

produces:

```java
Optional<Address>
```

### Interview answer

> `map()` is used when the mapping function returns a normal value. `flatMap()` is used when the mapping function already returns an Optional, so it avoids nested Optionals.

---

# 17. Optional with Streams

This is another important connection.

Suppose:

```java
List<String> names = List.of("John", "Alice", "Bob");
```

You can find someone:

```java
Optional<String> result =
        names.stream()
             .filter(name -> name.startsWith("A"))
             .findFirst();
```

Result:

```text
Optional[Alice]
```

If nobody matches:

```text
Optional.empty()
```

That's why methods such as:

```java
findFirst()
findAny()
min()
max()
```

often return Optional.

Because there may be **no result**.

---

# 18. Optional should generally not be used everywhere

This is a subtle but important interview point.

Don't blindly replace every field with:

```java
Optional<String>
```

For example:

```java
class User {
    private Optional<String> name;
}
```

This is generally not the intended primary use of Optional.

Optional was mainly designed for **return types representing possible absence**, rather than being a universal replacement for `null`.

A common practical guideline:

```java
Optional<User> findUser(...)
```

is reasonable.

But:

```java
void process(Optional<User> user)
```

is often unnecessary API complexity.

---

# 19. Don't serialize Optional as your domain model by default

Similarly, using Optional everywhere in entities/DTOs can create unnecessary complexity, especially with frameworks and serialization.

For example, don't assume:

```java
private Optional<String> name;
```

is automatically better than:

```java
private String name;
```

Optional's strongest use case is communicating absence at API boundaries, especially return values.

---

# 20. Optional is not a replacement for every null check

Bad reasoning:

> "Java has Optional now, so I should never use null."

That's not correct.

You can still encounter:

```java
null
```

in Java applications.

Optional provides an API design mechanism for representing **potential absence explicitly**.

---

# 21. Common interview question: Why not just return null?

Suppose:

```java
User findUser(Long id)
```

returns:

```java
null
```

The caller has to know:

> "This method might return null."

That's implicit.

With:

```java
Optional<User> findUser(Long id)
```

the API communicates:

> "A User may or may not exist."

That's explicit in the type.

---

# 22. Important methods to memorize

For interviews, remember these:

| Method          | Purpose                                  |
| --------------- | ---------------------------------------- |
| `of()`          | Create Optional from non-null value      |
| `ofNullable()`  | Create Optional from possibly-null value |
| `empty()`       | Empty Optional                           |
| `isPresent()`   | Check whether value exists               |
| `get()`         | Retrieve value; throws if empty          |
| `orElse()`      | Default value                            |
| `orElseGet()`   | Lazily generate default                  |
| `orElseThrow()` | Throw if empty                           |
| `ifPresent()`   | Execute action if present                |
| `map()`         | Transform value                          |
| `flatMap()`     | Transform without nested Optional        |
| `filter()`      | Keep value if condition matches          |

---

# 23. Classic interview trap

What does this print?

```java
Optional<String> optional = Optional.of("Java");

String result = optional
        .map(String::toUpperCase)
        .filter(s -> s.length() > 3)
        .orElse("Unknown");

System.out.println(result);
```

Step-by-step:

```text
Optional["Java"]
      ↓
map
      ↓
Optional["JAVA"]
      ↓
filter length > 3
      ↓
Optional["JAVA"]
      ↓
orElse
      ↓
"JAVA"
```

Output:

```text
JAVA
```

---

## 24. Another classic trap

```java
Optional<String> optional = Optional.of("Java");

String result = optional.orElse(getDefault());
```

Even though `"Java"` exists, `getDefault()` is evaluated.

With:

```java
String result = optional.orElseGet(() -> getDefault());
```

`getDefault()` isn't executed because the value exists.

**This is one of the Optional questions I'd expect you to know very well for an interview.**

---

# 25. EPAM-style interview questions

You should be able to answer these without hesitation:

### Basic

**Q1. What is Optional?**

> `Optional<T>` is a container that may contain a value or be empty, commonly used to explicitly represent an absent return value instead of returning null.

**Q2. Difference between `of()` and `ofNullable()`?**

> `of()` expects a non-null value and throws NullPointerException for null. `ofNullable()` accepts null and creates an empty Optional.

**Q3. What happens if you call `get()` on an empty Optional?**

> `NoSuchElementException`.

---

### Intermediate

**Q4. `orElse()` vs `orElseGet()`?**

> `orElse()` eagerly evaluates its argument; `orElseGet()` lazily invokes the supplier only when the Optional is empty.

**Q5. `map()` vs `flatMap()`?**

> `map()` transforms a value and wraps the result in Optional. `flatMap()` is used when the mapping function already returns Optional and avoids nested Optionals.

**Q6. Why do `findFirst()` and `findAny()` return Optional?**

> Because the stream may contain no matching element.

---

### Advanced

**Q7. Should Optional be used as a field in every class?**

No. Its primary purpose is representing possible absence, especially in return types. Using it indiscriminately for fields, parameters, entities, or DTOs can add unnecessary complexity.

**Q8. Is Optional completely null-safe?**

No. For example:

```java
Optional.of(null)
```

throws `NullPointerException`.

Use:

```java
Optional.ofNullable(value)
```

when the value may be null.

---

# Interview cheat sheet

Remember this mental model:

```text
             Optional<T>
                  |
       +----------+----------+
       |                     |
     value                 empty
       |                     |
     map()                orElse()
     filter()             orElseGet()
     ifPresent()          orElseThrow()
     flatMap()
```

And the **four most important distinctions**:

```text
of()          → value must NOT be null
ofNullable()  → value MAY be null

orElse()      → eager
orElseGet()   → lazy

map()         → transform
flatMap()     → transform when function returns Optional

isPresent()+get() → works, but often avoid
```

### One final rule

Don't use Optional just to make code look "Java 8-ish."

Use it when **absence is a meaningful part of the API contract**.

---

**Next topic: Interface vs Abstract Class** — including default/static methods, multiple inheritance, constructor behavior, when to choose each, and the interview traps around Java 8 interfaces.

---

Under the hood, java.util.Optional<T> is a final, immutable value-based class that wraps a single reference type. It is surprisingly simple, relying on a single field and private constructors to enforce its safety guarantees.
Here is a breakdown of the deep internals of Optional in Java 8:
------------------------------
## 1. The Core Fields and State
The entire state of an Optional object is governed by just two fields:

public final class Optional<T> {
    // 1. The single shared instance for empty Optionals
    private static final Optional<?> EMPTY = new Optional<>();

    // 2. The actual reference to the wrapped value (can be null)
    private final T value;
}


* value: This is a generic final reference. It is the only data field in the object, meaning an Optional has a very low memory footprint (it just wraps the object reference).
* EMPTY: Instead of instantiating a new object every time you need an empty container, Java utilizes a single, static Singleton instance (EMPTY).

------------------------------
## 2. Private Constructors Enforce Immutability
You cannot use the new keyword to create an Optional. Java restricts object creation through two private constructors:

// Used solely to initialize the static EMPTY singletonprivate Optional() {
    this.value = null;
}
// Used to wrap a value; throws NPE immediately if the value is nullprivate Optional(T value) {
    this.value = Objects.requireNonNull(value);
}

Because the value field is final and constructors are private, Optional instances are completely immutable and thread-safe.
------------------------------
## 3. Internal Mechanics of Factory Methods
When you call the public static factory methods, they route directly to these private constructors or the static singleton:

* Optional.empty(): Simply casts and returns the shared EMPTY singleton. No new object allocation occurs.
* Optional.of(T value): Calls the private constructor which invokes Objects.requireNonNull(value). This is why it throws a NullPointerException instantly if the input is null.
* Optional.ofNullable(T value): Evaluates the reference. If it is null, it routes to empty(). Otherwise, it routes to of(value):

public static <T> Optional<T> ofNullable(T value) {
    return value == null ? empty() : of(value);
}


------------------------------
## 4. Performance: orElse vs. orElseGet Internals
The internal difference between these two methods highlights why orElseGet is preferred for heavy computations.

* orElse(T other):

public T orElse(T other) {
    return value != null ? value : other;
}

Even if value is present, the expression passed into orElse(defaultValue) is always evaluated beforehand by the JVM because it is passed by value.
* orElseGet(Supplier<? extends T> other):

public T orElseGet(Supplier<? extends T> other) {
    return value != null ? value : other.get();
}

This method takes a functional interface (Supplier). The lambda expression other.get() is lazily evaluated inside the conditional branch only if value == null.

------------------------------
## 5. Intermediate Stream-like Operations
Methods like filter, map, and flatMap mimic stream behavior internally by checking the state and returning either this or a newly computed Optional.

* filter: If the value is present and matches the predicate, it returns this (reusing the same instance). If not, it drops the reference and returns empty().
* map: If a value is present, it applies the mapper function and wraps the result using Optional.ofNullable().
* flatMap: Unlike map, the mapper function for flatMap must already return an Optional. Internally, it extracts the inner Optional directly without double-wrapping it:

public<U> Optional<U> flatMap(Function<? extends T, Optional<U>> mapper) {
    Objects.requireNonNull(mapper);
    if (!isPresent())
        return empty();
    else {
        return Objects.requireNonNull(mapper.apply(value));
    }
}


------------------------------
## 6. Identity and equals()
Because Optional is a value-based class, you should never rely on reference equality (==) between two Optional containers. Its equals method is overridden to compare the wrapped values, not the containers themselves:

@Overridepublic boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Optional)) return false;
    Optional<?> other = (Optional<?>) obj;
    return Objects.equals(value, other.value);
}

Would you like to explore how the Just-In-Time (JIT) compiler optimizes Optional objects via Escape Analysis, or do you have questions about how Optional behaves during Serialization?


---
