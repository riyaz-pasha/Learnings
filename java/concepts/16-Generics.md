## 16. Generics in Java

Generics are another **high-frequency Java interview topic**, especially `? extends`, `? super`, PECS, type erasure, and why `List<Object>` is different from `List<?>`.

---

# 1. What are Generics?

Generics allow us to write code that works with different types while maintaining **compile-time type safety**.

Without generics:

```java
List list = new ArrayList();

list.add("Java");
list.add(100);

String value = (String) list.get(0);
```

Problems:

* No compile-time type safety
* Explicit casting required
* Errors can appear at runtime

With generics:

```java
List<String> list = new ArrayList<>();

list.add("Java");
list.add("Spring");
```

Now:

```java
list.add(100); // ❌ compile-time error
```

And:

```java
String value = list.get(0);
```

No cast required.

---

# 2. Why do we need Generics?

Three major benefits:

### Type safety

```java
List<String> names = new ArrayList<>();
```

Only Strings are allowed.

### No unnecessary casting

```java
String name = names.get(0);
```

instead of:

```java
String name = (String) list.get(0);
```

### Reusable code

Instead of writing:

```java
StringBox
IntegerBox
EmployeeBox
```

we can write:

```java
Box<T>
```

and use:

```java
Box<String>
Box<Integer>
Box<Employee>
```

---

# 3. Generic class

Example:

```java
public class Box<T> {

    private T value;

    public Box(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
```

Now:

```java
Box<String> stringBox = new Box<>("Java");

String value = stringBox.getValue();
```

Or:

```java
Box<Integer> integerBox = new Box<>(100);

Integer value = integerBox.getValue();
```

Here:

```text
T
```

is a **type parameter**.

---

# 4. Common generic naming conventions

You'll commonly see:

```text
T → Type
E → Element
K → Key
V → Value
N → Number
R → Return type
```

For example:

```java
Map<K, V>
```

and:

```java
List<E>
```

These are conventions, not language requirements.

You could technically write:

```java
class Box<Something> { }
```

but conventional names make code easier to understand.

---

# 5. Generic methods

A method can have its own type parameter.

```java
public static <T> void print(T value) {
    System.out.println(value);
}
```

Usage:

```java
print("Java");
print(100);
print(true);
```

The important syntax is:

```java
<T> void
```

The `<T>` before the return type declares the method's type parameter.

---

# 6. Generic method returning a value

```java
public static <T> T identity(T value) {
    return value;
}
```

Usage:

```java
String s = identity("Java");
Integer n = identity(100);
```

Java can infer the type.

---

# 7. Multiple type parameters

```java
public class Pair<K, V> {

    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }
}
```

Usage:

```java
Pair<String, Integer> pair =
        new Pair<>("Age", 30);
```

Here:

```text
K → String
V → Integer
```

---

# 8. Bounded type parameters

Sometimes we don't want to allow **any** type.

For example:

```java
public static <T extends Number> double doubleValue(T value) {
    return value.doubleValue();
}
```

Now these work:

```java
doubleValue(10);
doubleValue(10.5);
doubleValue(100L);
```

because:

```text
Integer extends Number
Double extends Number
Long extends Number
```

But:

```java
doubleValue("Java"); // ❌
```

because String isn't a Number.

---

# 9. `extends` in generic bounds

This:

```java
<T extends Number>
```

means:

> T must be Number or a subclass of Number.

Important:

`extends` here isn't limited to classes.

You can also have:

```java
<T extends Comparable<T>>
```

or:

```java
<T extends SomeClass & SomeInterface>
```

For example:

```java
<T extends Number & Comparable<T>>
```

A type parameter can have:

* one class bound
* followed by zero or more interface bounds

---

# 10. Wildcards

Now we reach one of the most important generic concepts.

Consider:

```java
List<Integer>
```

and:

```java
List<Number>
```

You might think:

```text
Integer is-a Number
therefore
List<Integer> is-a List<Number>
```

But this is **false**.

Generics are generally **invariant**.

So:

```java
List<Integer> integers = new ArrayList<>();

List<Number> numbers = integers; // ❌
```

Why doesn't Java allow this?

Because then we could do:

```java
numbers.add(10.5);
```

and we'd have inserted a Double into a list that is supposed to contain Integers.

That would break type safety.

---

# 11. `List<?>`

This means:

> A List of some unknown type.

```java
List<?> list;
```

It could refer to:

```java
List<String>
List<Integer>
List<Employee>
```

For example:

```java
List<String> names = new ArrayList<>();

List<?> list = names;
```

This is allowed.

---

# 12. What can you add to `List<?>`?

Almost nothing.

```java
List<?> list = new ArrayList<String>();

list.add("Java"); // ❌
list.add(100);    // ❌
```

The only generally valid value you can add is:

```java
null
```

Why?

Because Java doesn't know the actual type represented by `?`.

It could be:

```text
String
Integer
Employee
```

So it cannot safely accept an arbitrary object.

---

# 13. Reading from `List<?>`

You can safely read values as `Object`:

```java
List<?> list = List.of("Java", "Spring");

Object value = list.get(0);
```

Because every Java object is an `Object`.

---

# 14. `? extends`

Now consider:

```java
List<? extends Number>
```

This means:

> A list of some unknown type that is Number or a subclass of Number.

It could be:

```text
List<Integer>
List<Double>
List<Long>
```

Example:

```java
List<Integer> integers = List.of(1, 2, 3);

List<? extends Number> numbers = integers;
```

Allowed.

---

# 15. What can you do with `? extends`?

You can safely **read** values:

```java
Number n = numbers.get(0);
```

But generally you cannot add a Number:

```java
numbers.add(10); // ❌
```

Why?

Suppose:

```java
List<? extends Number> numbers
```

actually refers to:

```java
List<Integer>
```

If Java allowed:

```java
numbers.add(10.5);
```

we'd insert a Double into a List<Integer>.

So Java doesn't allow adding arbitrary values.

---

# 16. `? super`

Now:

```java
List<? super Integer>
```

means:

> A list whose element type is Integer or a superclass of Integer.

Possible types include:

```text
List<Integer>
List<Number>
List<Object>
```

Example:

```java
List<Number> numbers = new ArrayList<>();

List<? super Integer> list = numbers;
```

This is valid.

---

# 17. What can you do with `? super`?

You can safely add an Integer:

```java
list.add(10);
```

Because whichever valid type is being used:

```text
Integer
Number
Object
```

can hold an Integer.

But reading is more restrictive:

```java
Object value = list.get(0);
```

You can safely read as `Object`.

You can't assume:

```java
Integer value = list.get(0); // ❌
```

because the actual list could be:

```java
List<Number>
```

and contain a `Double`.

---

# 18. PECS

This is **extremely important for interviews**.

PECS means:

> **Producer Extends, Consumer Super**

### Producer → `extends`

If you're primarily **reading/producing** values:

```java
List<? extends Number>
```

### Consumer → `super`

If you're primarily **adding/consuming** values:

```java
List<? super Integer>
```

---

# 19. Example of PECS

Suppose we want to calculate the sum:

```java
public static double sum(
        List<? extends Number> numbers) {

    double total = 0;

    for (Number number : numbers) {
        total += number.doubleValue();
    }

    return total;
}
```

This accepts:

```java
sum(List.of(1, 2, 3));
sum(List.of(1.5, 2.5));
```

Because the list is a **producer** of Numbers.

---

Now suppose:

```java
public static void addIntegers(
        List<? super Integer> list) {

    list.add(10);
    list.add(20);
}
```

This can accept:

```java
List<Integer>
List<Number>
List<Object>
```

because the list is a **consumer** of Integers.

---

# 20. `List<Object>` vs `List<?>`

This is a very common interview question.

They are **not the same**.

### `List<Object>`

Means:

> A list whose actual element type is Object.

You can add anything:

```java
List<Object> list = new ArrayList<>();

list.add("Java");
list.add(100);
list.add(new Employee());
```

---

### `List<?>`

Means:

> A list of some unknown specific type.

You can read:

```java
Object value = list.get(0);
```

But you can't add arbitrary values.

So:

```text
List<Object>
→ known element type: Object

List<?>
→ unknown element type
```

---

# 21. Type erasure

This is one of the most important advanced generic concepts.

Java generics primarily provide **compile-time type safety**.

At runtime, generic type information is largely removed through **type erasure**.

For example:

```java
List<String>
```

and:

```java
List<Integer>
```

both become essentially:

```text
List
```

at runtime.

This was done largely to maintain compatibility with older Java code written before generics were introduced.

---

# 22. Consequence of type erasure

You cannot normally do:

```java
if (obj instanceof List<String>) {
}
```

This is illegal because runtime doesn't retain the full generic type parameter in the way required for that check.

You can do:

```java
if (obj instanceof List<?>) {
}
```

because `?` doesn't require knowing the specific element type.

---

# 23. You cannot create generic arrays directly

This is illegal:

```java
T[] array = new T[10]; // ❌
```

Because of type erasure, Java can't directly create an array of an unknown runtime generic type.

You often need alternatives such as:

```java
List<T>
```

or carefully use:

```java
Array.newInstance(...)
```

when building more advanced generic infrastructure.

---

# 24. Why doesn't `List<int>` work?

This is another common question.

This is invalid:

```java
List<int> numbers; // ❌
```

Generics work with **reference types**, not primitive types.

Use:

```java
List<Integer> numbers;
```

Java's autoboxing handles conversions:

```java
numbers.add(10);
```

which conceptually boxes:

```text
int → Integer
```

---

# 25. Generic classes and static members

Consider:

```java
class Box<T> {

    private T value;

    static T something; // ❌
}
```

This doesn't work.

Why?

Because `T` belongs to an **instance's generic type**, while static members belong to the class itself.

There isn't one single `T` for:

```text
Box<String>
Box<Integer>
Box<Employee>
```

---

# 26. Generic exception classes

Java doesn't allow:

```java
class MyException<T> extends Exception {
}
```

Generic classes cannot directly extend `Throwable`.

This is another type-erasure-related restriction.

---

# 27. Diamond operator

Instead of:

```java
List<String> names =
    new ArrayList<String>();
```

Java 7 allows:

```java
List<String> names =
    new ArrayList<>();
```

The compiler infers the type.

This is called the **diamond operator**.

---

# 28. Raw types

You can technically write:

```java
List list = new ArrayList();
```

This is called a **raw type**.

It's generally discouraged because you lose generic type safety.

For example:

```java
List list = new ArrayList();

list.add("Java");
list.add(100);

String value = (String) list.get(1);
```

Runtime failure:

```text
ClassCastException
```

Prefer:

```java
List<String> list = new ArrayList<>();
```

---

# 29. Generic interface

You can also define:

```java
interface Repository<T> {

    void save(T entity);

    T findById(Long id);
}
```

Then:

```java
class EmployeeRepository
        implements Repository<Employee> {

    @Override
    public void save(Employee employee) {
    }

    @Override
    public Employee findById(Long id) {
        return null;
    }
}
```

This pattern is heavily used in frameworks such as Spring Data.

---

# 30. Interview-ready answer

If asked:

> **What are Generics in Java?**

A strong answer:

> Generics provide compile-time type safety and allow classes, interfaces, and methods to work with parameterized types. They reduce casting and help detect type errors during compilation. Java generics use type erasure, so generic type information is largely removed at runtime. Wildcards such as `? extends` and `? super` allow us to make APIs more flexible while preserving type safety.

---

# 31. The most important generic concepts to remember

```text
<T>
    Generic type parameter

<T extends Number>
    Upper bound

List<?>
    Unknown type

List<? extends Number>
    Producer

List<? super Integer>
    Consumer

PECS
    Producer Extends
    Consumer Super
```

And:

```text
List<Integer> ≠ List<Number>
```

because generics are invariant.

Also:

```text
List<Object> ≠ List<?>
```

because:

```text
List<Object>
→ specifically List<Object>

List<?>
→ List of some unknown type
```

### Final mental model

```text
                Generics
                   |
        +----------+----------+
        |                     |
   Type safety           Reusable code
        |
        +----------------------------+
        |            |               |
      <T>         extends          super
                   |                |
                Producer         Consumer
                   |                |
                  PECS ← remember this
```

**Next topic: JVM Memory & Garbage Collection** — heap vs stack, method area/metaspace, object allocation, GC roots, generations, reachability, `OutOfMemoryError` vs `StackOverflowError`, and how garbage collection actually works.

---

Here is a comprehensive, step-by-step guide to understanding the wildcard (?) in Java generics from scratch.
## 1. The Core Problem: Why Do We Need ??
To understand wildcards, you first need to look at how inheritance behaves in Java generics.
In standard Java, an Integer is a subtype of Number. You can easily do this:

Number num = Integer.valueOf(5); // Perfectly valid

However, Generics are invariant. This means a List<Integer> is not a subtype of List<Number>, even though Integer extends Number.
If you try to write a method to print a list of numbers like this:

public static void printNumbers(List<Number> list) {
    for (Number n : list) {
        System.out.println(n);
    }
}

If you pass a List<Integer> into printNumbers(), the compiler will throw a compile-time error. The compiler does this to protect type safety (preventing you from accidentally inserting a Double into a List<Integer>).
This is where the Wildcard (?) comes in. It represents an unknown type and restores flexibility.
------------------------------
## 2. The Three Types of Wildcards
Java provides three ways to use wildcards depending on whether you want to read data, write data, or do both.
## A. Unbounded Wildcards (<?>)
An unbounded wildcard represents any type whatsoever. It is equivalent to <? extends Object>.

* 
* Syntax: List<?> list
* When to use: When you are implementing functionality that only uses methods provided in the Object class, or when the code doesn't depend on the generic type parameter.
* Behavior:
* You can READ from it, but you will only get objects of type Object.
   * You CANNOT WRITE anything to it (except null), because the compiler cannot guarantee what specific type the list is holding.
* 

public static void printList(List<?> list) {
    for (Object elem : list) {
        System.out.print(elem + " ");
    }
}// Works for List<Integer>, List<String>, List<Dog>, etc.

## B. Upper Bounded Wildcards (<? extends T>)
An upper bounded wildcard restricts the unknown type to be a specific type T, or a subclass of T.

* 
* Syntax: List<? extends Number> (Read as: "A list of some unknown type that is a subclass of Number")
* When to use: When you want to read data from a collection and use the methods of the parent type.
* Behavior:
* You can READ from it safely as type T (or Number in this case).
   * You CANNOT WRITE to it (except null). If the compiler allowed you to add an Integer to List<? extends Number>, that list might actually be a List<Double> at runtime, breaking type safety.
* 

public static double sumOfList(List<? extends Number> list) {
    double sum = 0.0;
    for (Number n : list) {
        sum += n.doubleValue(); // Safe to read as a Number
    }
    return sum;
}// Works for List<Integer>, List<Double>, List<Float>

## C. Lower Bounded Wildcards (<? super T>)
A lower bounded wildcard restricts the unknown type to be a specific type T, or a superclass of T (all the way up to Object).

* 
* Syntax: List<? super Integer> (Read as: "A list of some unknown type that is a superclass of Integer")
* When to use: When you want to write or insert data into a collection.
* Behavior:
* You can WRITE items of type T (or its subclasses) to it safely.
   * You can READ from it, but the returned type will always be a raw Object, because you don't know which exact superclass it is.
* 

public static void addNumbers(List<? super Integer> list) {
    for (int i = 1; i <= 10; i++) {
        list.add(i); // Safe to write an Integer
    }
}// Works for List<Integer>, List<Number>, List<Object>

------------------------------
## 3. The PECS Rule (How to Remember Which to Use)
The absolute best rule of thumb for deciding between extends and super is the PECS guideline coined by Joshua Bloch in Effective Java:

Producer Extends, Consumer Super


* 
* Producer (extends): If your collection produces data for your code to use (you are reading from the collection), use <? extends T>.
* Consumer (super): If your collection consumes data from your code (you are writing/adding into the collection), use <? super T>.
* Both: If you need to both read from and write to a collection, do not use wildcards. Use an exact type (e.g., List<T>).
* 

------------------------------
## Summary Cheat Sheet

| Wildcard Syntax | Read Operation yields... | Write Operation allows... | Primary Purpose |
|---|---|---|---|
| <?> | Object | Only null | Reading when the specific type doesn't matter |
| <? extends T> | Type T | Only null | Producer: Reading data safely up to type T |
| <? super T> | Object | Type T and its subclasses | Consumer: Writing data safely into a structure |

Would you like to try a few practice exercises to test your understanding of the PECS rule, or look at a specific code example involving custom classes?

