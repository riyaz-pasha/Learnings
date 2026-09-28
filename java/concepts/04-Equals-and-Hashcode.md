# Topic 4 — `equals()` & `hashCode()` ⭐⭐⭐⭐⭐

This topic is **critical** because it directly connects to:

* `HashMap`
* `HashSet`
* `HashTable`
* caching
* entity/object comparison
* collections
* Java interview coding questions

If an interviewer wants to check whether you actually understand Java objects, this is one of the places they'll go.

---

# 1. Start with `Object`

Every Java class ultimately inherits from `Object` (directly or indirectly).

So methods such as:

```java
equals()
hashCode()
toString()
```

come from:

```java
java.lang.Object
```

The default implementations of `equals()` and `hashCode()` are related to **object identity**.

---

# 2. What does `==` do?

For objects:

```java
==
```

checks whether two references refer to the **same object**.

Example:

```java
Employee e1 = new Employee();
Employee e2 = new Employee();

System.out.println(e1 == e2);
```

Output:

```text
false
```

There are two different objects.

Even if:

```java
e1.id = 100;
e2.id = 100;
```

`==` is still:

```text
false
```

because they're different objects.

---

# 3. What does `equals()` do?

`equals()` is intended for **logical equality**.

Suppose we have:

```java
class Employee {
    int id;
    String name;
}
```

Maybe our definition of equality is:

> Two Employees are equal if their IDs are equal.

Then:

```java
Employee e1 = new Employee(1, "John");
Employee e2 = new Employee(1, "John");
```

could logically be considered equal even though they're different objects.

So we can override:

```java
@Override
public boolean equals(Object obj) {
    ...
}
```

---

# 4. Default `equals()` behavior

If you don't override `equals()`:

```java
class Employee {
    int id;
}
```

then `Employee` inherits `Object.equals()`.

Conceptually, it behaves like identity comparison.

So:

```java
Employee e1 = new Employee(1);
Employee e2 = new Employee(1);

System.out.println(e1.equals(e2));
```

will normally be:

```text
false
```

because they're different objects.

---

# 5. Why override `equals()`?

If your domain has a meaningful notion of equality.

For example:

```java
class Employee {

    private int id;
    private String name;

    public Employee(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Employee other)) {
            return false;
        }

        return id == other.id;
    }
}
```

Now:

```java
Employee e1 = new Employee(1, "John");
Employee e2 = new Employee(1, "Bob");

System.out.println(e1.equals(e2));
```

returns:

```text
true
```

because our equality definition uses `id`.

---

# 6. Now comes `hashCode()`

Every Java object has:

```java
hashCode()
```

which returns an integer.

Example:

```java
String s = "Java";

System.out.println(s.hashCode());
```

The exact number isn't important here.

What matters is the **contract**.

---

# 7. The most important `equals()` / `hashCode()` rule

Memorize this:

> **If two objects are equal according to `equals()`, they must have the same `hashCode()`.**

Formally:

```text
a.equals(b) == true
        ↓
a.hashCode() == b.hashCode()
```

But the reverse is **not guaranteed**.

```text
a.hashCode() == b.hashCode()
        ↓
does NOT imply
        ↓
a.equals(b) == true
```

This distinction is extremely important.

---

# 8. Why can two unequal objects have the same hash code?

Because `hashCode()` returns an `int`.

There are potentially vastly more possible objects than possible integer values.

Therefore collisions are inevitable.

Example conceptually:

```text
Object A → hashCode 42
Object B → hashCode 42
```

but:

```text
A.equals(B) → false
```

This is called a **hash collision**.

A good hash function tries to distribute values well, but it cannot guarantee unique hash codes for all objects.

---

# 9. Why do HashMap and HashSet care?

Now we connect this to collections.

Suppose:

```java
Map<Employee, String> map = new HashMap<>();
```

and:

```java
Employee e = new Employee(1, "John");

map.put(e, "Developer");
```

HashMap needs to find where the key belongs.

Conceptually:

```text
key
 ↓
hashCode()
 ↓
bucket
 ↓
equals()
```

So both methods are important.

---

# 10. Simplified HashMap lookup

Suppose:

```java
map.get(employee);
```

Conceptually:

```text
                    employee
                        |
                        ↓
                   hashCode()
                        |
                        ↓
                     bucket
                        |
                        ↓
               candidate entries
                        |
                        ↓
                    equals()
                        |
                        ↓
                  matching key
```

`hashCode()` helps locate the candidate bucket.

`equals()` determines whether a candidate key is actually equal to the lookup key.

This is the key relationship to understand.

---

# 11. What happens if you override `equals()` but not `hashCode()`?

This is a **classic interview question**.

Suppose:

```java
class Employee {

    int id;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Employee other)) {
            return false;
        }

        return id == other.id;
    }
}
```

We have:

```java
Employee e1 = new Employee(1);
Employee e2 = new Employee(1);
```

Now:

```java
e1.equals(e2)
```

is:

```text
true
```

But if we haven't overridden `hashCode()`, the two objects can have different hash codes because they inherit `Object.hashCode()`.

Therefore:

```text
equals → true
hashCode → different
```

violates the contract.

---

# 12. Why does this break HashMap?

Consider:

```java
Map<Employee, String> map = new HashMap<>();

Employee e1 = new Employee(1);

map.put(e1, "John");

Employee e2 = new Employee(1);

System.out.println(map.get(e2));
```

You might expect:

```text
John
```

because:

```java
e1.equals(e2)
```

is true.

But if `hashCode()` isn't correctly overridden, `e1` and `e2` may produce different hashes.

Conceptually:

```text
put(e1)
   ↓
hash(e1)
   ↓
bucket 5
```

but:

```text
get(e2)
   ↓
hash(e2)
   ↓
bucket 12
```

The map looks in bucket 12 and doesn't find the entry stored in bucket 5.

Result can be:

```text
null
```

This is why:

> **Whenever you override `equals()`, you should normally override `hashCode()` consistently with it.**

---

# 13. Correct implementation

If equality is based on `id`:

```java
class Employee {

    private int id;
    private String name;

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Employee other)) {
            return false;
        }

        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
```

Now:

```text
same id
  ↓
equals() = true
  ↓
same hashCode()
```

The contract is preserved.

---

# 14. Better implementation with `Objects`

In real Java code you'll often see:

```java
@Override
public boolean equals(Object obj) {

    if (this == obj) {
        return true;
    }

    if (!(obj instanceof Employee other)) {
        return false;
    }

    return id == other.id
            && Objects.equals(name, other.name);
}

@Override
public int hashCode() {
    return Objects.hash(id, name);
}
```

Import:

```java
import java.util.Objects;
```

This handles null values safely.

---

# 15. The `equals()` contract

Interviewers may ask:

> What are the properties of equals()?

Know these five:

### 1. Reflexive

For a non-null object:

```text
x.equals(x) == true
```

---

### 2. Symmetric

```text
x.equals(y) == y.equals(x)
```

---

### 3. Transitive

If:

```text
x.equals(y)
y.equals(z)
```

then:

```text
x.equals(z)
```

---

### 4. Consistent

Repeated calls should return the same result as long as the relevant state hasn't changed.

---

### 5. Non-null

For:

```java
x.equals(null)
```

the result should be:

```text
false
```

assuming `x` itself isn't null.

---

# 16. HashCode contract

The important rules:

### Rule 1

If:

```text
a.equals(b) == true
```

then:

```text
a.hashCode() == b.hashCode()
```

### Rule 2

If two objects have the same hash code:

```text
a.hashCode() == b.hashCode()
```

they **may still be unequal**.

### Rule 3

During an execution, if the fields used to calculate hash code haven't changed, repeated calls should consistently return the same value.

---

# 17. The mutable key problem ⭐⭐⭐⭐⭐

This is a **very good interview question**.

Consider:

```java
class Employee {

    private int id;

    // equals and hashCode based on id
}
```

Then:

```java
Employee e = new Employee(1);

Map<Employee, String> map = new HashMap<>();

map.put(e, "John");
```

Now:

```java
e.setId(2);
```

What happened?

Originally:

```text
id = 1
hashCode = X
bucket = 5
```

After mutation:

```text
id = 2
hashCode = Y
bucket = 10
```

But the HashMap entry is still physically associated with the original bucket.

Now:

```java
map.get(e);
```

may return:

```text
null
```

even though:

```text
map.containsKey(e)
```

might also fail.

This is why **mutable objects whose equality/hashCode state changes should generally not be used as HashMap keys**.

---

# 18. Why immutable objects make good keys

Consider:

```java
String key = "Java";

Map<String, Integer> map = new HashMap<>();

map.put(key, 100);
```

Strings are immutable.

Therefore:

```text
key's logical value
      ↓
doesn't change
      ↓
hashCode remains consistent
```

That's one reason Strings are excellent HashMap keys.

---

# 19. HashSet connection

A `HashSet` also depends on hashing.

Example:

```java
Set<Employee> employees = new HashSet<>();

employees.add(new Employee(1));
employees.add(new Employee(1));
```

Will it contain one Employee or two?

That depends on correctly implementing:

```text
equals()
hashCode()
```

If both Employees are logically equal and their hash codes match, the set treats them as duplicates.

Conceptually:

```text
add(employee)
      ↓
hashCode()
      ↓
bucket
      ↓
equals()
      ↓
already exists?
   /        \
 yes         no
 ↓            ↓
reject       add
```

---

# 20. Important: HashSet doesn't simply use `equals()`

A common incorrect answer is:

> "HashSet uses equals() to check duplicates."

Incomplete.

It uses hashing to locate the relevant bucket/candidates and equality to distinguish actual equal elements among candidates.

That's why **both** `hashCode()` and `equals()` matter.

---

# 21. What about `String`?

Remember our previous topic:

```java
String a = new String("Java");
String b = new String("Java");
```

Then:

```java
a.equals(b)
```

is:

```text
true
```

and:

```java
a.hashCode() == b.hashCode()
```

is also:

```text
true
```

That's because String correctly overrides both methods based on its content.

---

# 22. A very common interview coding question

Given:

```java
class Employee {

    private int id;
    private String name;

    public Employee(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
```

Interviewer asks:

> "Make Employee work correctly as a HashMap key."

You should immediately think:

```java
@Override
public boolean equals(Object obj) {
    ...
}

@Override
public int hashCode() {
    ...
}
```

For example:

```java
@Override
public boolean equals(Object obj) {

    if (this == obj) {
        return true;
    }

    if (!(obj instanceof Employee other)) {
        return false;
    }

    return id == other.id
            && Objects.equals(name, other.name);
}

@Override
public int hashCode() {
    return Objects.hash(id, name);
}
```

---

# 23. A subtle interview question: Which fields should be used?

Suppose:

```java
Employee {
    id
    name
    salary
}
```

and your domain says:

> Employee identity is based only on `id`.

Then:

```java
equals()
```

and:

```java
hashCode()
```

should consistently use `id`.

Don't arbitrarily use every field.

The definition of equality is a **domain/design decision**.

---

# 24. Another subtle point: inheritance and equals()

This can get surprisingly complicated.

Consider:

```java
class Animal {
    int id;
}

class Dog extends Animal {
}
```

When designing `equals()` across inheritance hierarchies, you need to be careful about preserving symmetry and transitivity.

For example, using `instanceof` can make equality behave differently across parent/subclass types.

For interview purposes, remember:

> Equality implementations involving inheritance need to be designed carefully so that the `equals()` contract—especially symmetry and transitivity—is preserved.

For immutable/value-like classes, `final` classes can simplify equality semantics.

We'll revisit this when we discuss **immutability and object design**.

---

# 25. `Objects.equals()` vs `==`

Another common practical distinction:

```java
Objects.equals(a, b)
```

is null-safe.

For example:

```java
String a = null;
String b = "Java";

Objects.equals(a, b);
```

returns:

```text
false
```

Whereas:

```java
a.equals(b);
```

throws `NullPointerException`.

And:

```java
a == b
```

checks reference identity.

---

# 26. The three-way comparison

Memorize this:

```text
a == b
    ↓
Same object/reference?

a.equals(b)
    ↓
Logically equal?

a.hashCode() == b.hashCode()
    ↓
Same hash bucket candidate?
```

But the last one is not literally "same bucket" because the collection may further transform the hash and calculate the bucket index based on capacity. For interview-level understanding, think of it as helping locate the candidate bucket.

---

# 27. Classic interview puzzle

What happens?

```java
Set<Employee> set = new HashSet<>();

Employee e1 = new Employee(1);
Employee e2 = new Employee(1);

set.add(e1);
set.add(e2);

System.out.println(set.size());
```

### Case 1 — no `equals()` / `hashCode()`

Likely:

```text
2
```

because the two objects are distinct.

### Case 2 — correct `equals()` and `hashCode()` based on ID

```text
1
```

because they're logically equal.

This is a **very useful test of actual understanding**.

---

# 28. One more HashMap puzzle

```java
Map<Employee, String> map = new HashMap<>();

Employee e1 = new Employee(1);
map.put(e1, "John");

Employee e2 = new Employee(1);

System.out.println(map.get(e2));
```

Question:

> What determines whether `"John"` is returned?

Answer:

1. `e2.hashCode()` must lead to the appropriate candidate bucket.
2. Then the map compares keys using equality.
3. `e1.equals(e2)` must be true for the existing entry to match.

This is why the contract matters.

---

# 29. The most important interview statement

If the interviewer asks:

> Why must we override hashCode when overriding equals?

Give this answer:

> Hash-based collections such as HashMap and HashSet use hashCode to locate candidate entries and equals to determine logical equality. If two objects are equal according to equals but produce different hash codes, they can be placed or searched in different buckets, causing incorrect collection behavior. Therefore equal objects must always have the same hash code.

That's a **strong interview answer**.

---

# 30. Mental model

Keep this diagram:

```text
                 Object
                   │
          ┌────────┴────────┐
          ↓                 ↓
       equals()          hashCode()
          │                 │
          ↓                 ↓
   logical equality     hash value
          │                 │
          └────────┬────────┘
                   ↓
              HashMap /
              HashSet
```

And remember:

```text
equals == true
      ↓
hashCode MUST be same

hashCode == same
      ↓
equals MAY be false
```

---

# 31. What you should know before moving on

You should be comfortable with:

* `==` vs `equals()`
* Default `Object.equals()`
* Why we override `equals()`
* `hashCode()`
* `equals()` contract
* `hashCode()` contract
* Hash collision
* Why both are needed by HashMap/HashSet
* What happens if only `equals()` is overridden
* Mutable HashMap keys
* Why immutable keys are safer
* `Objects.equals()`
* `Objects.hash()`

### One sentence to memorize

> **`hashCode()` narrows down where to look; `equals()` determines whether the objects are actually equal.**

That's a very useful mental model.

---

## Quick interview test

Before we move into **Topic 5 — Collections Framework**, answer these mentally:

### Q1

If:

```java
a.equals(b) == true
```

what **must** be true about their hash codes?

### Q2

Can two unequal objects have the same hash code?

### Q3

Why is overriding only `equals()` but not `hashCode()` dangerous?

### Q4

Why can this become a problem?

```java
Map<Employee, String> map = new HashMap<>();
map.put(employee, "John");

employee.setId(999);
```

assuming `id` participates in `hashCode()`.

### Q5

Why can this:

```java
Set<Employee> set = new HashSet<>();

set.add(new Employee(1));
set.add(new Employee(1));
```

produce either **1 or 2 elements**, depending on how `Employee` implements equality?

Once this is clear, we'll go deep into **Topic 5: Collections — List, Set, Map, ArrayList, LinkedList, HashSet, TreeSet, HashMap, LinkedHashMap, TreeMap**, including their **internal workings, complexity, and the questions interviewers actually ask**.

---

In Java, the equals() and hashCode() methods are defined in the java.lang.Object class. They dictate how objects are compared for equality and how they are stored and managed inside hash-based data structures like HashMap, HashSet, and Hashtable. [1, 2, 3] 
When you create custom objects, understanding and implementing the strict mathematical and logical relationship between these two methods—known as the equals() and hashCode() contract—is critical to avoiding subtle, hard-to-debug state errors. [3] 
------------------------------
## 1. The Core Purpose: == vs. equals() vs. hashCode()

* == (Reference Equality): Compares whether two object references point to the exact same memory address location. [3, 4] 
* equals() (Logical Equality): By default, behaves like ==. However, it is designed to be overridden to perform a deep comparison—evaluating whether two distinct objects hold identical values or states. [3, 4, 5] 
* hashCode() (Numeric Fingerprint): Returns a 32-bit signed integer (int) representing the object. It acts as a quick-filtering mechanism used by hash collections to bucket objects efficiently. [1, 6, 7] 

------------------------------
## 2. The equals() and hashCode() Contract
The official Java documentation defines a strict contract between these two methods: [6, 8] 

   1. If A.equals(B) == true, then $A.\text{hashCode()} == B.\text{hashCode()}$.
   2. If $A.\text{hashCode()} == B.\text{hashCode()}$, A.equals(B) does NOT have to be true. This scenario is called a hash collision.
   3. Consistency: Multiple invocations of hashCode() or equals() must return the exact same result during the same application run, provided no data fields used in the comparison have changed. [1, 4, 6, 9, 10] 

## What happens if you break the contract?
If you override equals() but forget to override hashCode(), two objects with identical values will return true for equals(), but generate different hash codes. If you put one object into a HashMap, you will never be able to retrieve it using an identical object key because the map will search the wrong bucket. [4, 11] 
------------------------------
## 3. The Rules of equals()
An overridden equals() method must satisfy five mathematical properties defined by the Java API: [3] 

* Reflexive: For any non-null reference x, x.equals(x) must return true.
* Symmetric: For any references x and y, x.equals(y) must return true if and only if y.equals(x) returns true.
* Transitive: If x.equals(y) is true and y.equals(z) is true, then x.equals(z) must be true.
* Consistent: Repeated invocations must return the same result unless mutable fields change.
* Null-comparison: For any non-null reference x, x.equals(null) must always return false. [9, 12] 

------------------------------
## 4. Anatomy of an Elegant Implementation
Here is how a standard, bulletproof production class overrides both methods using the helper java.util.Objects utility class: [13, 14] 

import java.util.Objects;
public class Employee {
    private int id;
    private String name;
    private String department;

    public Employee(int id, String name, String department) {
        this.id = id;
        this.name = name;
        this.department = department;
    }

    @Override
    public boolean equals(Object obj) {
        // 1. Optimize for identity check
        if (this == obj) return true;
        
        // 2. Reject null and mismatched runtime classes
        if (obj == null || this.getClass() != obj.getClass()) return false;
        
        // 3. Cast to the correct target type
        Employee other = (Employee) obj;
        
        // 4. Compare every state-defining field
        return this.id == other.id &&
               Objects.equals(this.name, other.name) &&
               Objects.equals(this.department, other.department);
    }

    @Override
    public int hashCode() {
        // Generates a composite hash code utilizing the exact same fields
        return Objects.hash(id, name, department);
    }
}

## Why check this.getClass() != obj.getClass() instead of instanceof?
While instanceof works in simple classes, it breaks the Symmetry rule if inheritance is involved. If Manager extends Employee, an employee.equals(manager) using instanceof might return true, but manager.equals(employee) would evaluate to false because the manager checks for manager-specific fields. Using getClass() ensures comparisons occur only between identical types. [12, 13] 
------------------------------
## 5. How Hash-Based Collections Use Them Under the Hood
When you execute map.put(key, value) or map.get(key), Java optimizes performance by executing a multi-step verification pipeline: [1, 15] 

[Target Key] ──► Call hashCode() ──► Determine Bucket Index
                                            │
                                    Is Bucket Empty?
                                    ├── Yes ──► Insert Entry / Return null
                                    └── No  ──► Handle Hash Collision
                                                    │
                                            Traverse Linked List / Tree
                                            For each Node:
                                            Check if (Node.hash == Key.hash) AND 
                                                     (Node.key == Key OR Node.key.equals(Key))
                                                    │
                                                    ├── True  ──► Override Value / Return Value
                                                    └── False ──► Keep Traversing


   1. Bucket Mapping: The HashMap calculates key.hashCode() and applies internal bitwise operations to map the resulting integer to an array bucket index. [1, 15] 
   2. Short-Circuit Equality Check: If multiple keys map to the same bucket index (collision), it loops through the entries in that bucket. It compares their hash values first because matching two int variables is incredibly fast. [1, 6, 7] 
   3. Deep-Equal Verification: If and only if the hash codes match, it executes key.equals(object) to confirm that they are logically identical. [1, 7] 

------------------------------
## 6. Architectural Best Practices

* Always Use Immutable Fields: Base your implementations on fields that do not change (like id or uuid). If you modify an object state while it lives inside a HashSet, its hash value changes, leaving the object "lost" in the wrong bucket forever. [9, 10] 
* Symmetric Field Usage: The exact same fields utilized to build the equality logic in equals() must be included in the hashCode() computation. [4, 13] 
* Leverage Modern Language Features: If you are writing boilerplate data carriers, use Java Records introduced in Java 14. Records automatically synthesize correct, unalterable equals() and hashCode() implementations for all components.

Would you like to explore how Java Records manage this automatically, or see an example of how a mutable object can break a collection?

[1] [https://www.linkedin.com](https://www.linkedin.com/pulse/understanding-hashcode-equals-java-kiran-u-kamath-jnzqc)
[2] [https://www.youtube.com](https://www.youtube.com/shorts/M562APbzsAg)
[3] [https://codefinity.com](https://codefinity.com/blog/How-equals%28%29-and-hashCode%28%29-Work-in-Java-and-Why-Following-Their-Contract-Matters)
[4] [https://www.youtube.com](https://www.youtube.com/watch?v=4X5Kgi2VltI)
[5] [https://www.youtube.com](https://www.youtube.com/watch?v=dCt9sfZV8Sg)
[6] [https://campus.epam.com](https://campus.epam.com/en/blog/339)
[7] [https://kiranukamath.medium.com](https://kiranukamath.medium.com/understanding-hashcode-and-equals-in-java-9041b1c418ef)
[8] [https://medium.com](https://medium.com/the-java-interview-playbook/equals-hashcode-methods-in-java-e99bc1066851)
[9] [https://www.digitalocean.com](https://www.digitalocean.com/community/tutorials/java-equals-hashcode)
[10] [https://www.naukri.com](https://www.naukri.com/code360/library/equals-and-hashcode-in-java)
[11] [https://medium.com](https://medium.com/@iyusubov444/interview-questions-java-equals-hashcode-methods-revisited-7b0cd80aa903)
[12] [https://www.geeksforgeeks.org](https://www.geeksforgeeks.org/java/equals-hashcode-methods-java/)
[13] [https://medium.com](https://medium.com/@nelushgayashan/java-equality-the-complete-guide-to-equals-and-hashcode-1ab6a6ccd54e)
[14] [https://medium.com](https://medium.com/@kamran.babayevv/equals-and-hashcode-in-java-d74d89cb0db0)
[15] [https://justgiveacar.medium.com](https://justgiveacar.medium.com/how-to-use-equals-and-hashcode-in-java-e10b8fd1aba5)

---

