# Topic 10 — Access Modifiers in Java

Access modifiers are simple on the surface, but **`protected`**, package access, inheritance, and nested classes create several interview traps.

The four Java access levels are:

```text
public
protected
package-private   ← no keyword
private
```

---

# 1. Why do access modifiers exist?

They control **where a class member can be accessed**.

For example:

```java
class BankAccount {

    private double balance;

    public void deposit(double amount) {
        balance += amount;
    }
}
```

External code cannot directly modify:

```java
account.balance = -1000;
```

because `balance` is private.

Instead, access is controlled through methods.

This is one of the mechanisms behind **encapsulation**.

---

# 2. The four access levels

The easiest way to remember them:

| Modifier        | Same class | Same package | Subclass in different package | Everywhere |
| --------------- | ---------: | -----------: | ----------------------------: | ---------: |
| `private`       |          ✅ |            ❌ |                             ❌ |          ❌ |
| package-private |          ✅ |            ✅ |                            ❌* |          ❌ |
| `protected`     |          ✅ |            ✅ |                           ✅** |          ❌ |
| `public`        |          ✅ |            ✅ |                             ✅ |          ✅ |

Two important footnotes:

* `*` A subclass in another package cannot access a package-private member merely because it is a subclass.
* `**` `protected` across packages has a special inheritance/reference rule that we'll cover below.

---

# 3. `private`

`private` gives the most restrictive access.

```java
class Employee {

    private String name;

    private void calculateSalary() {
        System.out.println("Calculating");
    }
}
```

Only code inside `Employee` can directly access these members.

This won't work:

```java
Employee employee = new Employee();

employee.name; // compilation error
```

---

# 4. Can a subclass access private members?

Directly: **No.**

```java
class Parent {

    private int value = 10;
}

class Child extends Parent {

    void print() {
        System.out.println(value); // compilation error
    }
}
```

However, the private field still exists as part of the `Parent` object's state.

The subclass simply cannot directly access it.

It can access it through a permitted method:

```java
class Parent {

    private int value = 10;

    protected int getValue() {
        return value;
    }
}
```

Then:

```java
class Child extends Parent {

    void print() {
        System.out.println(getValue());
    }
}
```

---

# 5. Can private methods be overridden?

**No.**

This is a very common interview question.

```java
class Parent {

    private void print() {
        System.out.println("Parent");
    }
}
```

```java
class Child extends Parent {

    private void print() {
        System.out.println("Child");
    }
}
```

This is **not method overriding**.

The two methods are unrelated because the parent's method is private and not inherited by the subclass.

---

# 6. Package-private

If you don't specify an access modifier:

```java
class Employee {

    String name;
}
```

then the member has **package-private** access.

Sometimes interviewers call this:

> default access

Be careful: this is **not the same thing as an interface `default` method**.

Package-private means:

> Accessible within the same package.

Example:

```text
com.company.employee
    Employee.java
    EmployeeService.java
```

`EmployeeService` can access package-private members of `Employee`.

But:

```text
com.company.api
    SomeController.java
```

cannot directly access them.

---

# 7. Important: top-level classes

For a **top-level class**, only these access levels are allowed:

```java
public class Employee {
}
```

or:

```java
class Employee {
}
```

The second one is package-private.

You cannot have:

```java
private class Employee {
}
```

or:

```java
protected class Employee {
}
```

for a top-level class.

Why?

Because `private` and `protected` don't make sense as access levels for a top-level class.

---

# 8. `public`

`public` is accessible from anywhere, assuming the class/package is otherwise accessible.

```java
public class Employee {

    public String name;

    public void print() {
        System.out.println(name);
    }
}
```

Other packages can access it:

```java
Employee employee = new Employee();

employee.print();
```

---

# 9. `protected`

This is where interview questions become interesting.

A protected member is accessible:

1. Within the same class
2. Within the same package
3. From subclasses, including subclasses in another package — subject to a special rule

Example:

```java
class Parent {

    protected int value = 10;
}
```

Same package:

```java
class OtherClass {

    void print() {
        Parent p = new Parent();
        System.out.println(p.value);
    }
}
```

Allowed.

---

# 10. Protected across packages

Suppose:

```text
package com.example.parent
```

```java
package com.example.parent;

public class Parent {

    protected int value = 10;
}
```

Now another package:

```text
package com.example.child;

import com.example.parent.Parent;

public class Child extends Parent {

    void print() {
        System.out.println(value);
    }
}
```

This is allowed.

Because `Child` is a subclass.

---

# 11. The `protected` trap

Now suppose:

```java
package com.example.child;

public class Child extends Parent {

    void print(Parent parent) {
        System.out.println(parent.value);
    }
}
```

This can be illegal when `Parent` and `Child` are in different packages.

The cross-package protected rule is **not simply "any subclass can access the protected member through any Parent reference."**

The access must occur through the subclass relationship.

For example:

```java
class Child extends Parent {

    void print() {
        System.out.println(this.value);
    }
}
```

is allowed.

Also:

```java
class Child extends Parent {

    void print(Child child) {
        System.out.println(child.value);
    }
}
```

is allowed.

But using an arbitrary `Parent` reference across the package boundary is not allowed.

This is one of the most frequently misunderstood Java access rules.

---

# 12. Why does Java do this?

Suppose:

```text
Package A

Parent
  protected secret
```

and:

```text
Package B

Child extends Parent
```

Java wants `Child` to inherit and use the protected capability.

But it doesn't want `Child` to become a general-purpose gateway for accessing protected members on arbitrary `Parent` objects.

So cross-package protected access is tied to the subclass relationship.

---

# 13. Access modifier inheritance

Consider:

```java
class Parent {

    protected void process() {
    }
}
```

A subclass can override it:

```java
class Child extends Parent {

    @Override
    protected void process() {
    }
}
```

But you cannot reduce visibility:

```java
class Child extends Parent {

    @Override
    private void process() { // compilation error
    }
}
```

Why?

Because code that can access `Parent.process()` must not suddenly lose access when the runtime object is a `Child`.

---

# 14. Can you increase visibility?

Yes.

Parent:

```java
class Parent {

    protected void process() {
    }
}
```

Child:

```java
class Child extends Parent {

    @Override
    public void process() {
    }
}
```

That's valid.

Visibility can become broader.

Conceptually:

```text
private
   ↓
package-private
   ↓
protected
   ↓
public
```

When overriding, you can move downward toward **more visibility**, but not toward less visibility.

---

# 15. Access modifiers and overriding

Suppose:

```java
class Parent {

    public void print() {
    }
}
```

Then:

```java
class Child extends Parent {

    protected void print() {
    }
}
```

Compilation error.

Why?

Because `protected` is weaker visibility than `public`.

Correct:

```java
class Child extends Parent {

    public void print() {
    }
}
```

---

# 16. What about fields?

Fields can also have access modifiers:

```java
class Employee {

    private String name;
    protected double salary;
    String department;        // package-private
    public int employeeId;
}
```

Their accessibility follows the same basic rules.

But remember something from our OOP discussion:

**Fields are not polymorphically overridden.**

A subclass can declare a field with the same name, but that's field hiding, not method overriding.

---

# 17. Access modifiers and constructors

Constructors can also have access modifiers.

### Public constructor

```java
public class User {

    public User() {
    }
}
```

Anyone who can access the class can construct it.

---

### Private constructor

```java
class Singleton {

    private Singleton() {
    }
}
```

External code cannot do:

```java
new Singleton(); // compilation error
```

Private constructors are commonly used for:

* utility classes
* controlled instance creation
* certain factory patterns
* singleton implementations

---

### Protected constructor

A protected constructor can be accessed by:

* same-package code
* subclasses, subject to the usual protected rules

---

# 18. Private constructor + utility class

For example:

```java
public final class MathUtils {

    private MathUtils() {
        // Prevent instantiation
    }

    public static int add(int a, int b) {
        return a + b;
    }
}
```

Usage:

```java
MathUtils.add(10, 20);
```

No reason to create:

```java
new MathUtils();
```

---

# 19. Nested classes are different

A top-level class cannot be `private`.

But a nested class can:

```java
class Outer {

    private static class Inner {
    }
}
```

This is valid.

Nested classes can use:

```text
public
protected
private
package-private
```

because they're members of another class.

---

# 20. Access modifiers and packages

Imagine:

```text
com.company
    Employee.java
    EmployeeService.java

com.company.api
    EmployeeController.java
```

Suppose:

```java
class Employee {

    String name;
}
```

`name` is package-private.

Then:

```java
EmployeeService
```

can access it because it is in:

```text
com.company
```

But:

```java
EmployeeController
```

cannot because it is in:

```text
com.company.api
```

Important:

> `com.company.api` is a different package from `com.company`.

Package hierarchy does **not** imply access inheritance.

---

# 21. `protected` vs package-private

This is a good interview comparison.

### Package-private

```java
int value;
```

Accessible within the same package.

A subclass in another package **doesn't get access** merely by extending the class.

### Protected

```java
protected int value;
```

Accessible within the same package **and** through subclass inheritance across packages, subject to the protected access rule.

So:

```text
package-private
    → package-focused

protected
    → package + subclass-focused
```

---

# 22. Which modifier should you prefer?

A good design principle is:

> Use the least visibility necessary.

For example, don't automatically make everything:

```java
public
```

Instead:

```java
private
```

for implementation details.

Expose only what callers actually need.

Example:

```java
public class BankAccount {

    private double balance;

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }

    public double getBalance() {
        return balance;
    }
}
```

The caller cannot directly manipulate:

```java
balance
```

This supports encapsulation.

---

# 23. Interview question: Can we override a private method?

No.

Because private methods are not inherited.

---

# 24. Interview question: Can we override a static method?

No.

Static methods are **hidden**, not overridden.

Example:

```java
class Parent {

    public static void print() {
        System.out.println("Parent");
    }
}
```

```java
class Child extends Parent {

    public static void print() {
        System.out.println("Child");
    }
}
```

This is method hiding.

It is resolved based on the reference/class rather than runtime polymorphism.

---

# 25. Interview question: Can we override a final method?

No.

```java
class Parent {

    public final void print() {
    }
}
```

A subclass cannot override it.

```java
class Child extends Parent {

    public void print() { // compilation error
    }
}
```

`final` explicitly prevents overriding.

---

# 26. Access modifiers cheat sheet

Memorize this table:

```text
                  Same      Same       Subclass      Everywhere
                  class     package    other pkg

private             ✓          ✗           ✗             ✗

package-private     ✓          ✓           ✗             ✗

protected           ✓          ✓           ✓*            ✗

public              ✓          ✓           ✓             ✓
```

`✓*` means cross-package access is available through the subclass relationship and is subject to the special protected-reference rule.

---

# 27. EPAM-style questions

You should be comfortable answering:

### Basic

**1. What are Java's four access levels?**

`private`, package-private, `protected`, `public`.

**2. What is package-private?**

When no modifier is specified, access is restricted to the same package.

**3. Can a top-level class be private?**

No.

**4. Can a nested class be private?**

Yes.

---

### Intermediate

**5. Can a subclass access a private field?**

Not directly.

**6. Can a subclass access a protected field?**

Yes, including across packages through inheritance, subject to protected access rules.

**7. Can an overriding method reduce visibility?**

No.

**8. Can an overriding method increase visibility?**

Yes.

**9. Can private methods be overridden?**

No.

**10. Can static methods be overridden?**

No; they are hidden.

**11. Can final methods be overridden?**

No.

---

# 28. One interview scenario

Interviewer:

> "Parent has a protected method. Child is in another package. Can Child call it?"

Answer:

> Yes. A subclass in another package can access an inherited protected member through the subclass relationship. However, protected access across packages is more restrictive than same-package access; the subclass cannot simply use an arbitrary superclass reference to access that protected member.

That's a strong answer because it shows you know the nuance rather than simply saying "protected means subclass."

---

## What to remember

If you remember only these points:

```text
private
→ same class only

package-private
→ same package

protected
→ same package + subclasses
  (special rule across packages)

public
→ everywhere
```

And:

```text
Top-level class:
→ public or package-private

Nested class:
→ can use all four

Override:
→ cannot reduce visibility
→ can increase visibility

private method:
→ not inherited
→ cannot be overridden

static method:
→ hidden, not overridden

final method:
→ cannot be overridden
```

**Next topic: `static` in Java** — static variables, methods, initialization blocks, static vs instance, class loading, inheritance/hiding, and the common `static` interview traps.

