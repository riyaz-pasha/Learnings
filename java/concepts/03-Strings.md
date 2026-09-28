# Topic 3 — `String`, String Pool, `==`, `equals()` & Immutability

This is a **very high-frequency Java interview topic**. Interviewers love giving small code snippets and asking what they print.

We’ll build it from the basics and then get into the tricky cases.

---

## 1. Why is `String` special in Java?

`String` is a class:

```java
String name = "John";
```

But unlike ordinary objects, Java gives strings special treatment through the **String Pool**.

Strings are also **immutable**.

These two concepts are extremely important:

```text
String
  │
  ├── String Pool
  │
  └── Immutability
```

---

# 2. String is immutable

Consider:

```java
String s = "Hello";

s.concat(" World");

System.out.println(s);
```

Output:

```text
Hello
```

Why?

Because `concat()` does **not modify the existing String**.

It creates another String.

Conceptually:

```text
"Hello"
   |
   | concat(" World")
   ↓
"Hello World"
```

But `s` still points to:

```text
"Hello"
```

If you want the new value:

```java
String s = "Hello";

s = s.concat(" World");

System.out.println(s);
```

Now:

```text
Hello World
```

---

# 3. Why is String immutable?

This is an extremely common interview question.

There isn't just one reason.

### Reason 1 — String Pool

Suppose:

```java
String a = "Java";
String b = "Java";
```

Both can refer to the same pooled String.

```text
       String Pool

        "Java"
        /    \
       /      \
      a        b
```

If Strings were mutable:

```java
a.change("Python");
```

then `b` could unexpectedly become `"Python"` too.

Immutability makes sharing safe.

---

### Reason 2 — Security

Strings are commonly used for sensitive/configuration-related values such as:

```text
file paths
class names
URLs
database connection information
authentication-related data
```

If a String could change after being passed somewhere, security checks could become unreliable.

---

### Reason 3 — HashMap / HashSet

Strings are frequently used as keys:

```java
Map<String, Integer> map = new HashMap<>();

map.put("John", 100);
```

If the String's value could change after insertion, its hash code could change, potentially making the entry difficult to locate.

Immutability makes Strings reliable hash keys.

---

### Reason 4 — Thread safety

Immutable objects can be safely shared between threads because their state cannot change.

Therefore, Strings are inherently thread-safe with respect to their immutable state.

---

# 4. String Pool

Now the important part.

Consider:

```java
String a = "Hello";
String b = "Hello";
```

Java can reuse the same pooled String.

Conceptually:

```text
Stack / references

a ──────┐
        │
b ──────┤
        ↓
     String Pool
       "Hello"
```

Therefore:

```java
System.out.println(a == b);
```

prints:

```text
true
```

Why?

Because `==` is comparing the references, and both references can point to the same pooled String object.

---

# 5. String literal vs `new String()`

Now:

```java
String a = "Hello";
String b = new String("Hello");
```

What happens?

The literal `"Hello"` is associated with the String Pool.

`new String("Hello")` explicitly creates a **new String object**.

Conceptually:

```text
String Pool
   |
   └── "Hello"  ← a
                 

Heap
   |
   └── new String("Hello") ← b
```

Therefore:

```java
System.out.println(a == b);
```

prints:

```text
false
```

But:

```java
System.out.println(a.equals(b));
```

prints:

```text
true
```

Because both strings contain the same characters.

---

# 6. `==` vs `equals()`

This is one of the most important distinctions in Java.

For objects:

```java
==
```

checks whether two references refer to the **same object**.

While:

```java
equals()
```

checks **logical equality**, assuming the class has an appropriate implementation.

Example:

```java
String a = new String("Java");
String b = new String("Java");

System.out.println(a == b);
System.out.println(a.equals(b));
```

Output:

```text
false
true
```

Because:

```text
a ──→ String("Java")   ← different object
b ──→ String("Java")   ← different object
```

but their contents are equal.

---

# 7. Important distinction: `==` with primitives

For primitives:

```java
int a = 10;
int b = 10;

System.out.println(a == b);
```

This compares their actual values.

So:

```text
Primitive
    ↓
== → value comparison
```

For object references:

```text
Object
   ↓
== → reference identity
```

This distinction is essential.

---

# 8. One classic interview question

What does this print?

```java
String a = "Java";
String b = "Java";

System.out.println(a == b);
System.out.println(a.equals(b));
```

Answer:

```text
true
true
```

Why?

Both references point to the same pooled String, and obviously their contents are equal.

---

# 9. Another classic

```java
String a = new String("Java");
String b = new String("Java");

System.out.println(a == b);
System.out.println(a.equals(b));
```

Answer:

```text
false
true
```

Different objects, same contents.

---

# 10. The interview trap: `final` doesn't mean immutable

Consider:

```java
final String name = "John";
```

You cannot do:

```java
name = "Bob";
```

because the reference cannot be reassigned.

But `final` itself doesn't magically make an object immutable.

For example:

```java
final List<String> names = new ArrayList<>();

names.add("John");
names.add("Bob");
```

This is perfectly valid.

The reference cannot point to another List:

```java
names = new ArrayList<>(); // ❌
```

but the existing List can still be modified.

So:

> `final` reference ≠ immutable object.

String happens to be immutable because of how the `String` class itself is designed.

---

# 11. Why is String declared `final`?

Conceptually:

```java
public final class String {
    ...
}
```

One important reason is to prevent subclasses from changing String's behavior and violating assumptions around immutability, security, equality, hashing, etc.

If arbitrary subclasses could alter important String behavior, many guarantees around the class would become harder to maintain.

---

# 12. String concatenation

Consider:

```java
String s = "Hello";

s = s + " World";
```

Because Strings are immutable, the original String isn't modified.

A new String is produced.

Conceptually:

```text
"Hello"
   +
" World"
   ↓
"Hello World"
```

For a small number of operations, this is usually fine.

But consider:

```java
String result = "";

for (int i = 0; i < 10000; i++) {
    result += i;
}
```

This can create many intermediate String objects.

That's where `StringBuilder` becomes useful.

---

# 13. StringBuilder

Use:

```java
StringBuilder sb = new StringBuilder();

sb.append("Hello");
sb.append(" ");
sb.append("World");

String result = sb.toString();
```

Unlike String, `StringBuilder` is mutable.

Conceptually:

```text
String
→ immutable
→ modification creates new object

StringBuilder
→ mutable
→ append modifies existing builder
```

---

# 14. StringBuilder vs StringBuffer

Both are mutable character sequences.

### StringBuilder

Generally preferred for ordinary single-threaded code.

```java
StringBuilder sb = new StringBuilder();
```

### StringBuffer

Older synchronized alternative.

```java
StringBuffer sb = new StringBuffer();
```

Main interview distinction:

```text
StringBuilder
→ not synchronized
→ generally better performance

StringBuffer
→ synchronized
→ thread-safe operations
→ more synchronization overhead
```

Don't automatically say "StringBuffer should always be used in multithreading." The actual choice depends on how the object is shared and the required synchronization semantics.

---

# 15. `String.intern()`

This is a slightly more advanced interview topic.

Consider:

```java
String a = new String("Java");
```

You can call:

```java
String b = a.intern();
```

`intern()` returns the canonical pooled representation of the string.

So:

```java
String a = new String("Java");
String b = a.intern();
String c = "Java";

System.out.println(b == c);
```

prints:

```text
true
```

because both `b` and `c` refer to the canonical pooled String.

---

# 16. Compile-time concatenation vs runtime concatenation

Another favorite.

### Example 1

```java
String a = "Hello";
String b = "World";

String c = "HelloWorld";
```

Obviously:

```java
a.equals(...)
```

etc.

But consider:

```java
String x = "Hello" + "World";
String y = "HelloWorld";

System.out.println(x == y);
```

This can be:

```text
true
```

because `"Hello" + "World"` consists entirely of compile-time constants and can be folded into the pooled `"HelloWorld"` constant.

Now consider:

```java
String a = "Hello";
String x = a + "World";
String y = "HelloWorld";

System.out.println(x == y);
```

Here `a` is a variable, so the concatenation isn't simply the same compile-time constant expression.

You should **not rely on `==` for String content comparison**.

Use:

```java
x.equals(y)
```

---

# 17. `equals()` and `hashCode()`

Now we reach an extremely important connection.

Suppose:

```java
String a = "Java";
String b = new String("Java");
```

Then:

```java
a.equals(b)
```

is true.

Their hash codes must also be equal.

Java's general contract says:

> If two objects are equal according to `equals()`, they must have the same `hashCode()`.

But the reverse is not necessarily true:

```text
same hashCode
     ↓
does NOT guarantee
     ↓
equals() == true
```

This becomes extremely important when we get to **HashMap and HashSet**.

---

# 18. Why does `String.equals()` work?

`String` overrides `Object.equals()`.

Conceptually, it compares the characters contained in the two strings.

That's why:

```java
String a = new String("Java");
String b = new String("Java");

a.equals(b)
```

is:

```text
true
```

even though:

```java
a == b
```

is:

```text
false
```

---

# 19. What happens with `null`?

This is another practical interview question.

```java
String a = null;

a.equals("Java");
```

This throws:

```text
NullPointerException
```

because you're invoking `equals()` on a null reference.

Safer:

```java
"Java".equals(a)
```

This returns:

```text
false
```

This pattern is often used when the left-hand value is guaranteed non-null.

---

# 20. A subtle point about String Pool

Don't think:

> "All Strings are stored only in the String Pool."

That's incorrect.

For example:

```java
String s = new String("Java");
```

creates a new String object, while the literal `"Java"` is associated with the pool.

The important idea is:

> String literals are interned/maintained in the String pool, while explicitly created String objects can exist separately.

---

# 21. Classic interview questions

You should be able to solve these immediately.

### Q1

```java
String s1 = "Java";
String s2 = "Java";

System.out.println(s1 == s2);
```

### Q2

```java
String s1 = new String("Java");
String s2 = new String("Java");

System.out.println(s1 == s2);
```

### Q3

```java
String s1 = new String("Java");
String s2 = new String("Java");

System.out.println(s1.equals(s2));
```

### Q4

```java
String s = "Hello";
s.concat(" World");

System.out.println(s);
```

### Q5

```java
String s = "Hello";
s = s.concat(" World");

System.out.println(s);
```

### Q6

```java
String a = "Hello" + "World";
String b = "HelloWorld";

System.out.println(a == b);
```

### Q7

```java
String a = "Hello";
String b = new String("Hello");

System.out.println(a == b);
System.out.println(a.equals(b));
```

---

# 22. Interviewer's deeper question: Why does immutability help HashMap?

Suppose:

```java
Map<String, Integer> map = new HashMap<>();

String key = "Java";

map.put(key, 100);
```

HashMap uses the key's hash code to determine where the entry belongs.

If the key's logical value could change after insertion:

```text
Before:
"Java"
hashCode → X
bucket → 5

After mutation:
"Python"
hashCode → Y
bucket → 12
```

The map could have the object physically sitting in bucket 5 while lookup calculates bucket 12.

String immutability prevents this kind of problem.

This is one reason immutable objects make excellent map keys.

---

# 23. One more important distinction

### String

```text
Immutable
```

### StringBuilder

```text
Mutable
```

### StringBuffer

```text
Mutable + synchronized methods
```

So:

```text
Need immutable text?
       ↓
     String

Building/modifying text repeatedly?
       ↓
  StringBuilder

Need a shared synchronized mutable
character sequence with those semantics?
       ↓
  StringBuffer
```

---

# 24. What I'd expect you to say in an interview

If asked:

> Why is String immutable?

A strong answer:

> String is immutable, meaning once a String object is created, its contents cannot be changed. This enables safe sharing through the String pool, makes Strings reliable as hash keys, provides thread-safety for their immutable state, and helps with security-related guarantees. Operations that appear to modify a String actually create a new String.

If asked:

> Difference between `==` and `equals()`?

Say:

> For primitives, `==` compares values. For object references, `==` compares object identity, meaning whether both references point to the same object. `equals()` is used for logical equality according to the class's implementation. String overrides `equals()` to compare its character content.

That's a very solid interview answer.

---

# 25. The mental model to remember

```text
                    String
                       │
              ┌────────┴────────┐
              ↓                 ↓
        Immutable            String Pool
              │
              │
      ┌───────┴────────┐
      ↓                ↓
     ==             equals()
      ↓                ↓
 identity          logical content
```

And:

```text
"Java"
  ↓
String literal
  ↓
String Pool

new String("Java")
  ↓
new String object
```

---

## Before Topic 4 — quick test

Try these without looking back:

### 1.

What is the output?

```java
String a = "Java";
String b = "Java";

System.out.println(a == b);
System.out.println(a.equals(b));
```

### 2.

```java
String a = new String("Java");
String b = new String("Java");

System.out.println(a == b);
System.out.println(a.equals(b));
```

### 3.

Why does this print `"Hello"` rather than `"Hello World"`?

```java
String s = "Hello";
s.concat(" World");

System.out.println(s);
```

### 4.

Why is `StringBuilder` generally preferred over repeatedly doing:

```java
result = result + value;
```

inside a large loop?

### 5.

Most importantly:

**Explain in your own words why String is immutable and why that is useful.**

Once this is comfortable, the next topic will be **`equals()` + `hashCode()` in depth**, because that leads directly into one of the most heavily tested Java areas: **HashMap, HashSet and the Collections framework**.

---

In Java, the difference between a String Literal and a String Object comes down to how they are stored in memory and how the JVM handles them. While both represent a sequence of characters, they utilize memory completely differently. [1, 2] 
------------------------------
## Direct Comparison

| Feature | String Literal ("abc") | String Object (new String("abc")) |
|---|---|---|
| Syntax | String s = "hello"; | String s = new String("hello"); |
| Memory Location | String Constant Pool (SCP) (inside Heap) | Normal Heap Memory (outside SCP) |
| Object Reusability | Highly reusable; shares instances. | Creates a brand-new object every single time. |
| Objects Created | 0 or 1 (depending on if it already exists in the pool). | 1 or 2 (1 in the Heap, and potentially 1 in the SCP). |
| Performance | Faster; optimized for memory. | Slower; adds unnecessary garbage collection overhead. |

------------------------------
## In-Depth Memory Mechanics## 1. String Literals and the String Constant Pool (SCP)
When you declare a string literal like String s1 = "Java";, the JVM uses a special optimization technique called interning: [2, 3] 

* The JVM looks inside the String Constant Pool. [4] 
* If "Java" already exists, s1 simply points to that existing memory location. No new memory is allocated. [5, 6] 
* If "Java" does not exist, the JVM creates a new object inside the pool and points s1 to it. [2, 5] 

Because of this, if you declare String s2 = "Java";, both s1 and s2 point to the exact same memory address. Therefore, s1 == s2 will evaluate to true. [3, 7] 
## 2. String Objects via new Keyword
When you use the new operator, you are explicitly forcing the JVM to bypass pool optimizations: [6] 

* String s3 = new String("Java");
* The JVM guarantees the allocation of a completely separate, brand-new object in the non-pool Heap memory. [3, 8] 
* Concurrently, the literal argument "Java" is evaluated; if "Java" isn't already in the SCP, it gets added there too. This means the statement can create up to two objects simultaneously. [8] 

Because new always creates a distinct reference address, if you create String s4 = new String("Java");, then s3 == s4 evaluates to false. [7] 
------------------------------
## Example Code Execution
Here is how the comparison operators work under the hood based on memory addresses (==) vs. text content (.equals()): [5, 7] 

public class StringDeepDive {
    public static void main(String[] args) {
        // String Literals (Stored in SCP)
        String s1 = "Alpha";
        String s2 = "Alpha";

        // String Objects (Stored in Heap)
        String s3 = new String("Alpha");
        String s4 = new String("Alpha");

        // Reference Comparisons (Memory Addresses)
        System.out.println(s1 == s2);      // true  -> Both point to the same SCP entry
        System.out.println(s1 == s3);      // false -> SCP vs. Heap memory address
        System.out.println(s3 == s4);      // false -> Two distinct objects in Heap

        // Value Comparisons (Content)
        System.out.println(s1.equals(s3)); // true  -> The text "Alpha" is identical
        System.out.println(s3.equals(s4)); // true  -> The text "Alpha" is identical
    }
}

## The intern() Method Escape Hatch
If you have a standard String Object created in the Heap, you can manually move its reference into the pool by calling the intern() method. [2, 6] 

String sObject = new String("Beta");
String sPolled = sObject.intern(); // Looks up "Beta" in pool, or puts it there
String sLiteral = "Beta";

System.out.println(sPolled == sLiteral); // true -> They now point to the exact same pool object

## Key Takeaway
Always prefer String Literals over using new String(). Literals consume less memory and allow the compiler and garbage collector to run much more efficiently. [4, 7, 9] 
Would you like to explore how immutability connects to the String Pool, or should we look at how StringBuilder and StringBuffer compare to regular Strings?

[1] [https://www.geeksforgeeks.org](https://www.geeksforgeeks.org/java/string-initialization-java-string-literal-vs-string-object/)
[2] [https://www.youtube.com](https://www.youtube.com/watch?v=ykqbCdz6Mk0&t=3)
[3] [https://stackoverflow.com](https://stackoverflow.com/questions/3297867/difference-between-string-object-and-string-literal)
[4] [https://www.icertglobal.com](https://www.icertglobal.com/community/difference-between-string-literal-and-string-object)
[5] [https://www.youtube.com](https://www.youtube.com/watch?v=Cx55RDoKcCg)
[6] [https://www.edureka.co](https://www.edureka.co/community/39584/what-the-difference-between-string-object-and-string-literal)
[7] [https://www.youtube.com](https://www.youtube.com/watch?v=WMnn9Scr9ZY)
[8] [https://stackoverflow.com](https://stackoverflow.com/questions/10223763/string-literals-vs-string-object-in-java)
[9] [https://www.janbasktraining.com](https://www.janbasktraining.com/community/java/what-is-the-difference-between-string-literal-and-string-object-in-the-context-of-java)

----

