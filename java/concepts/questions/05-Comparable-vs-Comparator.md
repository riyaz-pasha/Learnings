# Java `Comparable` vs `Comparator`

Both `Comparable` and `Comparator` are used to **define how objects should be ordered**, especially when using sorting APIs such as `Collections.sort()` and `List.sort()`.

The key difference is:

> **`Comparable` = the class defines its own natural ordering.**
> **`Comparator` = someone outside the class defines a custom ordering.**

---

## 1. The Problem

Suppose we have:

```java
class Student {
    String name;
    int age;
    double marks;
}
```

Now we have:

```java
List<Student> students = ...
```

If we write:

```java
students.sort(...);
```

Java needs to know:

> "How should two `Student` objects be compared?"

For example:

```text
Student A          Student B
---------          ---------
name = "Alice"     name = "Bob"
age = 20           age = 18
marks = 85         marks = 95
```

Should Alice come first because:

```text
name: Alice < Bob
```

Or should Bob come first because:

```text
age: 18 < 20
```

Or:

```text
marks: 95 > 85
```

This is where `Comparable` and `Comparator` come in.

---

# 2. `Comparable`

`Comparable` is used when a class has a **natural/default ordering**.

It belongs to:

```java
java.lang.Comparable
```

Its main method is:

```java
int compareTo(T other);
```

The class itself implements `Comparable`.

```java
class Student implements Comparable<Student> {

    String name;
    int age;

    @Override
    public int compareTo(Student other) {
        return this.age - other.age;
    }
}
```

Now:

```java
List<Student> students = ...;

Collections.sort(students);
```

or:

```java
students.sort(null);
```

can use `Student.compareTo()`.

---

# 3. Understanding `compareTo()`

The return value has three meanings:

```text
compareTo(other)

< 0  → this object comes before other
= 0  → both are considered equal for ordering
> 0  → this object comes after other
```

For example:

```java
@Override
public int compareTo(Student other) {
    return Integer.compare(this.age, other.age);
}
```

Suppose:

```java
Student a = new Student("Alice", 20);
Student b = new Student("Bob", 25);
```

Then:

```java
a.compareTo(b)
```

returns a negative number.

Therefore:

```text
Alice (20)
   ↓
Bob   (25)
```

---

# 4. Don't use subtraction blindly

You will often see:

```java
return this.age - other.age;
```

It works for many small values, but it can cause **integer overflow**.

Prefer:

```java
return Integer.compare(this.age, other.age);
```

For `long`:

```java
return Long.compare(this.value, other.value);
```

For `double`:

```java
return Double.compare(this.score, other.score);
```

This is the safer modern approach.

---

# 5. Complete `Comparable` Example

```java
import java.util.*;

class Student implements Comparable<Student> {

    private final String name;
    private final int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public int compareTo(Student other) {
        return Integer.compare(this.age, other.age);
    }

    @Override
    public String toString() {
        return name + " (" + age + ")";
    }
}

public class Main {

    public static void main(String[] args) {

        List<Student> students = new ArrayList<>();

        students.add(new Student("Alice", 25));
        students.add(new Student("Bob", 20));
        students.add(new Student("Charlie", 22));

        Collections.sort(students);

        System.out.println(students);
    }
}
```

Output:

```text
[Bob (20), Charlie (22), Alice (25)]
```

The important thing is:

```java
class Student implements Comparable<Student>
```

and:

```java
public int compareTo(Student other)
```

The class is saying:

> "Whenever someone asks for the natural ordering of Students, sort them by age."

---

# 6. The Problem with `Comparable`

Imagine later we want to sort the same students by:

```text
name
```

Then:

```text
Alice
Bob
Charlie
```

Or by:

```text
age
```

Or:

```text
age descending
```

Or:

```text
name descending
```

We cannot keep changing `compareTo()` every time.

That's where `Comparator` becomes useful.

---

# 7. `Comparator`

`Comparator` allows us to define an ordering **outside the class**.

It belongs to:

```java
java.util.Comparator
```

Its main method is:

```java
int compare(T o1, T o2);
```

For example:

```java
Comparator<Student> byName =
        (student1, student2) ->
                student1.getName().compareTo(student2.getName());
```

Then:

```java
students.sort(byName);
```

Now students are sorted by name.

---

# 8. Complete `Comparator` Example

```java
import java.util.*;

class Student {

    private final String name;
    private final int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return name + " (" + age + ")";
    }
}

public class Main {

    public static void main(String[] args) {

        List<Student> students = new ArrayList<>();

        students.add(new Student("Charlie", 22));
        students.add(new Student("Alice", 25));
        students.add(new Student("Bob", 20));

        students.sort(
                Comparator.comparing(Student::getName)
        );

        System.out.println(students);
    }
}
```

Output:

```text
[Alice (25), Bob (20), Charlie (22)]
```

Notice something important:

**`Student` does not implement `Comparable`.**

The ordering is provided externally.

---

# 9. Multiple `Comparator`s

This is one of the biggest advantages of `Comparator`.

We can easily create different orderings.

### By name

```java
Comparator<Student> byName =
        Comparator.comparing(Student::getName);
```

### By age

```java
Comparator<Student> byAge =
        Comparator.comparingInt(Student::getAge);
```

### By age descending

```java
Comparator<Student> byAgeDescending =
        Comparator.comparingInt(Student::getAge)
                  .reversed();
```

We can then choose whichever ordering we need:

```java
students.sort(byName);
```

or:

```java
students.sort(byAge);
```

or:

```java
students.sort(byAgeDescending);
```

---

# 10. Multi-Level Sorting

This is another very common interview topic.

Suppose we want:

> Sort by age first. If two students have the same age, sort by name.

We can write:

```java
Comparator<Student> comparator =
        Comparator.comparingInt(Student::getAge)
                  .thenComparing(Student::getName);
```

Example:

```text
Before:

Charlie  20
Alice    25
Bob      20
David    25
```

After:

```text
Bob      20
Charlie  20
Alice    25
David    25
```

Because:

```text
age
 ↓
name
 ↓
```

---

# 11. `Comparable` vs `Comparator`

| Feature                  | `Comparable`     | `Comparator`       |
| ------------------------ | ---------------- | ------------------ |
| Package                  | `java.lang`      | `java.util`        |
| Main method              | `compareTo()`    | `compare()`        |
| Where defined?           | Inside the class | Outside the class  |
| Purpose                  | Natural ordering | Custom ordering    |
| Number of orderings      | Usually one      | Many               |
| Modifies class?          | Yes              | No                 |
| Used by default sorting? | Yes              | Only when supplied |
| Functional interface?    | No               | Yes                |
| Lambda friendly?         | No               | Yes                |

---

# 12. Simple Mental Model

Think about a `Student`.

### `Comparable`

The student says:

> "My default ordering is by age."

```java
class Student implements Comparable<Student> {

    @Override
    public int compareTo(Student other) {
        return Integer.compare(this.age, other.age);
    }
}
```

So:

```java
students.sort(null);
```

means:

```text
Student
   │
   └── compareTo()
          │
          └── age
```

---

### `Comparator`

Someone else says:

> "For this particular situation, I want students sorted by name."

```java
students.sort(
    Comparator.comparing(Student::getName)
);
```

So:

```text
Student
   │
   ├── name Comparator
   ├── age Comparator
   ├── marks Comparator
   └── etc.
```

---

# 13. Real-World Analogy

Imagine employees.

An employee has:

```text
name
age
salary
joiningDate
```

There might be a natural ordering:

```text
employee ID
```

So you could use:

```java
Comparable<Employee>
```

But HR may want:

```text
sort by salary
```

Management may want:

```text
sort by joining date
```

A directory may want:

```text
sort by name
```

Therefore:

```text
Comparable
    ↓
one natural/default ordering

Comparator
    ↓
many possible orderings
```

---

# 14. When Should You Use Which?

### Use `Comparable` when:

The class has an obvious **natural ordering**.

Examples:

```text
String → lexicographical order
Integer → numeric order
LocalDate → chronological order
BigDecimal → numeric order
```

For your own class:

```java
class Employee implements Comparable<Employee>
```

when there is one ordering that naturally represents the object.

---

### Use `Comparator` when:

You need:

* Multiple sorting strategies
* Sorting a class you don't own
* Temporary/custom ordering
* Different ordering depending on the use case

For example:

```java
Comparator<Employee> bySalary;
Comparator<Employee> byName;
Comparator<Employee> byJoiningDate;
```

---

# 15. Important Interview Question

### Can a class implement both?

**Yes.**

For example:

```java
class Employee implements Comparable<Employee> {

    @Override
    public int compareTo(Employee other) {
        return this.id.compareTo(other.id);
    }
}
```

Natural ordering:

```text
Employee → ID
```

But we can still do:

```java
employees.sort(
    Comparator.comparing(Employee::getSalary)
);
```

Now:

```text
Comparable
    ↓
default ordering → ID

Comparator
    ↓
custom ordering → Salary
```

The supplied `Comparator` takes precedence for that sorting operation.

---

# 16. One More Important Concept: `Comparator.comparing`

Modern Java makes `Comparator` very convenient.

Instead of:

```java
Comparator<Student> comparator =
        (a, b) -> Integer.compare(a.getAge(), b.getAge());
```

we can write:

```java
Comparator<Student> comparator =
        Comparator.comparingInt(Student::getAge);
```

For objects:

```java
Comparator<Student> byName =
        Comparator.comparing(Student::getName);
```

For multiple fields:

```java
Comparator<Student> comparator =
        Comparator.comparingInt(Student::getAge)
                  .thenComparing(Student::getName);
```

This is the style you will commonly see in modern Java code.

---

# 17. Interview Answer

If an interviewer asks:

> **What is the difference between Comparable and Comparator?**

A strong concise answer is:

> `Comparable` is used to define the natural ordering of a class and requires the class itself to implement `compareTo()`. `Comparator` is used to define external or custom ordering through `compare()`, allowing us to have multiple different sorting strategies without modifying the class.

Then give an example:

```java
// Natural ordering
class Employee implements Comparable<Employee> {

    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.id, other.id);
    }
}
```

versus:

```java
// Custom ordering
employees.sort(
    Comparator.comparing(Employee::getSalary)
);
```

---

# 18. The Most Important Thing to Remember

```text
                ORDERING
                   │
          ┌────────┴────────┐
          │                 │
     Comparable         Comparator
          │                 │
    "What is my         "How do I want
     natural order?"     to sort now?"
          │                 │
      compareTo()         compare()
          │                 │
     Inside class       Outside class
          │                 │
       One main          Many possible
       ordering           orderings
```

### One-line memory trick

> **Comparable = "I can compare myself."**
> **Comparator = "Someone else compares two objects."**

