## 22. Serialization & Deserialization in Java

Serialization is a classic Java interview topic. It is less central to modern application design than it once was, but interviewers still commonly ask about it.

The core idea is simple:

> **Serialization converts an object's state into a byte representation that can be stored or transmitted. Deserialization reconstructs an object from that representation.**

---

# 1. What is Serialization?

Suppose you have:

```java
class Employee {
    private String name;
    private int age;
}
```

Normally, an `Employee` exists as an object in memory.

Serialization converts its state into bytes:

```text
Employee object
      ↓
Serialization
      ↓
bytes
      ↓
file / network / storage
```

Later:

```text
bytes
  ↓
Deserialization
  ↓
Employee object
```

---

# 2. How do we make a class Serializable?

Implement:

```java
java.io.Serializable
```

Example:

```java
import java.io.Serializable;

public class Employee implements Serializable {

    private String name;
    private int age;
}
```

`Serializable` is a **marker interface**.

That means:

> It doesn't define methods that your class needs to implement. It tells Java's serialization mechanism that instances of the class are eligible for default serialization.

---

# 3. Serializing an object

Use:

```java
ObjectOutputStream
```

Example:

```java
Employee employee = new Employee("John", 30);

try (ObjectOutputStream out =
         new ObjectOutputStream(
             new FileOutputStream("employee.ser"))) {

    out.writeObject(employee);
}
```

Conceptually:

```text
Employee
   ↓
ObjectOutputStream
   ↓
employee.ser
```

---

# 4. Deserializing

Use:

```java
ObjectInputStream
```

Example:

```java
try (ObjectInputStream in =
         new ObjectInputStream(
             new FileInputStream("employee.ser"))) {

    Employee employee =
        (Employee) in.readObject();
}
```

Flow:

```text
employee.ser
     ↓
ObjectInputStream
     ↓
readObject()
     ↓
Employee
```

Notice:

```java
readObject()
```

returns:

```java
Object
```

so you generally cast it to the expected type.

---

# 5. What happens to fields?

Suppose:

```java
class Employee implements Serializable {

    private String name;
    private int age;
}
```

When serialized, the serializable object's instance state is written according to Java's serialization rules.

Static fields are **not part of an individual object's serialized state**.

For example:

```java
static String company = "ABC";
```

is associated with the class, not the individual object.

---

# 6. `transient`

This is extremely important.

Suppose:

```java
class Employee implements Serializable {

    private String name;

    private transient String password;
}
```

`password` will not be serialized as part of the object's serializable state.

After deserialization, the field gets its default value.

For a reference:

```text
null
```

For primitives:

```text
0
false
...
```

Example:

```java
Employee employee =
    new Employee("John", "secret");

serialize(employee);

Employee restored = deserialize();

System.out.println(restored.getPassword());
```

Result:

```text
null
```

---

# 7. Why use `transient`?

Typical reasons include:

### Sensitive data

```java
private transient String password;
```

### Derived/cache data

```java
private transient int cachedValue;
```

### Non-serializable dependencies

```java
private transient Logger logger;
```

But don't treat `transient` as a complete security mechanism by itself; Java native serialization has broader security considerations.

---

# 8. `serialVersionUID`

This is one of the **most frequently asked serialization questions**.

Example:

```java
class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private int age;
}
```

What is it?

It identifies the serialized form/version of the class for compatibility checking during deserialization.

---

# 9. Why is `serialVersionUID` needed?

Imagine version 1:

```java
class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
}
```

You serialize:

```text
Employee → bytes
```

Later you change the class:

```java
class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private int age;
}
```

You try to deserialize the old data.

Because the `serialVersionUID` remains compatible, Java can potentially deserialize the old representation, with the newly introduced `age` field receiving its default value if it wasn't present in the serialized data.

For an `int`:

```text
age = 0
```

---

# 10. What if serialVersionUID doesn't match?

You can get:

```text
InvalidClassException
```

Conceptually:

```text
Serialized object
      ↓
serialVersionUID = 1

Current class
      ↓
serialVersionUID = 2

Mismatch
      ↓
InvalidClassException
```

This is why explicitly declaring:

```java
private static final long serialVersionUID = 1L;
```

is generally preferable for Serializable classes whose serialized form may persist.

---

# 11. What if you don't declare serialVersionUID?

Java can calculate one based on aspects of the class structure.

That generated value can change when the class changes.

Therefore, explicitly declaring one gives you control over compatibility.

Interview answer:

> `serialVersionUID` is used to verify compatibility between the serialized object's class definition and the current class definition during deserialization.

---

# 12. Constructors and deserialization

This is a very common interview trap.

Suppose:

```java
class Employee implements Serializable {

    private String name;

    public Employee() {
        System.out.println("Constructor called");
    }
}
```

During normal creation:

```java
new Employee();
```

the constructor executes.

But during ordinary Java serialization deserialization:

```java
ObjectInputStream.readObject();
```

the serializable class's constructor is **not invoked in the normal way**.

So don't assume:

```text
deserialize
   ↓
constructor executes
```

That's not how ordinary Serializable deserialization works.

For a Serializable class, object state is reconstructed through the serialization mechanism.

---

# 13. What about the parent constructor?

This is another classic question.

Suppose:

```java
class Parent {
    public Parent() {
        System.out.println("Parent");
    }
}

class Child extends Parent implements Serializable {
}
```

When `Child` is deserialized, the first non-serializable superclass's no-argument constructor is invoked as part of the deserialization process.

So:

> Serializable class constructors aren't invoked normally, but the first non-serializable superclass needs an accessible no-argument constructor.

This is a common interview detail.

---

# 14. Serialization with inheritance

Consider:

```java
class Parent {
    int x = 10;
}

class Child extends Parent implements Serializable {
    int y = 20;
}
```

`Child` is Serializable, but `Parent` isn't.

The state of `Parent` isn't serialized through `Serializable` in the same way.

During deserialization, the non-serializable superclass's no-argument constructor initializes that superclass portion.

Therefore, after deserialization:

```text
Child's serializable state
    → restored from stream

Parent's non-serializable state
    → initialized through parent constructor
```

This can surprise developers.

---

# 15. Static fields

Suppose:

```java
class Employee implements Serializable {

    private String name;

    static String company = "ABC";
}
```

Serialize an Employee when:

```text
company = "ABC"
```

Then change:

```text
company = "XYZ"
```

Deserialize the employee.

You should not expect the serialized object to restore:

```text
company = "ABC"
```

because `company` is static and belongs to the class, not that particular object instance.

---

# 16. `transient` and static

Both have important differences.

```java
private transient String password;
```

means:

> This instance field isn't serialized.

Whereas:

```java
private static String company;
```

means:

> This state belongs to the class, not the serialized instance.

Neither is part of the serialized instance state in the ordinary sense.

---

# 17. What if a field isn't Serializable?

Suppose:

```java
class Employee implements Serializable {

    private String name;

    private Thread thread;
}
```

`Thread` isn't serializable.

If you try to serialize the Employee, you can get:

```text
NotSerializableException
```

because the serialization process encounters a non-serializable object in the reachable instance state.

A common solution, if appropriate, is:

```java
private transient Thread thread;
```

and recreate/reinitialize it after deserialization.

---

# 18. Object graph serialization

Serialization isn't necessarily just about one object.

Suppose:

```java
class Employee implements Serializable {
    Address address;
}

class Address implements Serializable {
    String city;
}
```

When you serialize:

```java
employee
```

Java follows the object graph and serializes the referenced serializable objects as appropriate.

Conceptually:

```text
Employee
   |
   ↓
Address
   |
   ↓
City
```

So all required objects in the graph need to be serializable unless they're excluded or otherwise handled.

---

# 19. Circular references

Java serialization can handle object graphs containing references back to already-seen objects.

For example:

```text
A → B
↑   |
└───┘
```

It doesn't simply recursively duplicate the same object forever.

The serialization mechanism maintains object identity/reference information in the stream.

This is one reason Java serialization is more than merely writing fields one after another.

---

# 20. `writeObject()` and `readObject()`

You can customize serialization.

Example:

```java
private void writeObject(ObjectOutputStream out)
        throws IOException {

    out.defaultWriteObject();

    // custom logic
}
```

And:

```java
private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // custom initialization
}
```

These methods have special significance to Java's serialization mechanism.

---

# 21. Why customize serialization?

For example, suppose:

```java
class User implements Serializable {

    private String username;
    private transient String password;
}
```

You might want to reconstruct some transient state during deserialization.

Example:

```java
private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // recreate derived/transient state
}
```

Be careful with security-sensitive data and custom serialization logic.

---

# 22. `readResolve()`

Another special method is:

```java
private Object readResolve()
```

It can control the object returned after deserialization.

A classic example involves singleton-like patterns.

Suppose:

```java
class Singleton implements Serializable {

    private static final Singleton INSTANCE =
        new Singleton();

    private Singleton() {
    }

    public static Singleton getInstance() {
        return INSTANCE;
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
```

Without appropriate handling, deserialization can create another instance, undermining a singleton design.

`readResolve()` can ensure the canonical instance is returned.

---

# 23. `writeReplace()`

Similarly, there is:

```java
private Object writeReplace()
```

which can substitute an object before serialization.

These methods are less common in everyday Spring development but can appear in deeper Java interviews.

---

# 24. `Externalizable`

Another interface is:

```java
Externalizable
```

It extends:

```java
Serializable
```

and requires:

```java
void writeExternal(ObjectOutput out)
void readExternal(ObjectInput in)
```

Example:

```java
class Employee implements Externalizable {

    @Override
    public void writeExternal(ObjectOutput out)
            throws IOException {
        // explicitly write fields
    }

    @Override
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException {
        // explicitly read fields
    }
}
```

---

# 25. Serializable vs Externalizable

### Serializable

```text
default serialization mechanism
```

You can customize it when necessary.

### Externalizable

```text
you explicitly control what gets written/read
```

|                       | Serializable | Externalizable |
| --------------------- | ------------ | -------------- |
| Marker interface      | Yes          | No             |
| Default serialization | Yes          | No             |
| Full manual control   | Less         | More           |
| Requires methods      | No           | Yes            |
| Complexity            | Lower        | Higher         |

`Externalizable` requires a public no-argument constructor because the deserialization mechanism needs to instantiate the object through that contract.

---

# 26. Serialization security

This is an important modern point.

Java native deserialization can be dangerous when processing **untrusted serialized data**.

Historically, vulnerabilities have arisen from gadget chains where attacker-controlled serialized data triggers dangerous behavior during deserialization.

Therefore:

> **Don't blindly deserialize untrusted Java serialization streams.**

In modern applications, safer formats are often preferred for data exchange.

Examples include:

```text
JSON
Protobuf
Avro
```

depending on the system requirements.

---

# 27. Serialization vs JSON

This distinction is important in Spring Boot interviews.

### Java Serialization

```java
Object
   ↓
Java-specific binary representation
```

### JSON

```text
Java object
   ↓
JSON text
```

For example:

```json
{
  "name": "John",
  "age": 30
}
```

JSON is language-neutral and widely used for REST APIs.

Java native serialization is Java-specific and has security/compatibility concerns.

---

# 28. Serialization vs persistence

Serialization is **not the same thing as database persistence**.

Serialization:

```text
Object → bytes
```

Persistence:

```text
Application state → durable storage
```

A database uses its own representation and consistency mechanisms.

Don't say:

> "Serialization is how Java stores objects permanently."

That's too simplistic.

---

# 29. Common interview question: Can final fields be serialized?

Yes.

For example:

```java
class Employee implements Serializable {

    private final String name;

    Employee(String name) {
        this.name = name;
    }
}
```

The field's value can be part of the serialized state.

Deserialization reconstructs the object according to the serialization mechanism rather than simply executing the normal constructor.

---

# 30. Common interview question: Is Serializable inherited?

If a superclass implements:

```java
Serializable
```

its subclasses are also considered Serializable because the interface is inherited.

You don't have to explicitly write:

```java
class Child extends Parent implements Serializable
```

if `Parent` already implements it.

However, the serialized state and superclass rules still need to be understood.

---

# 31. Common interview question: Can an interface be serialized?

Interfaces themselves aren't serialized as objects.

A **class implementing `Serializable`** can be serialized.

For example:

```java
interface Animal {
}
```

doesn't make:

```java
class Dog implements Animal
```

serializable.

You need:

```java
class Dog implements Animal, Serializable
```

unless a superclass already provides Serializable.

---

# 32. `serialVersionUID` interview answer

If the interviewer asks:

> "What is serialVersionUID?"

A strong answer:

> `serialVersionUID` is a version identifier used during Java serialization/deserialization to check whether the serialized data is compatible with the current class definition. Explicitly declaring it gives the developer control over compatibility instead of relying on a generated value.

---

# 33. `transient` interview answer

> `transient` tells Java's default serialization mechanism not to serialize that instance field. After deserialization, it has its default value unless custom deserialization logic initializes it.

---

# 34. Why modern applications often avoid native Java serialization

There are several reasons:

### Security

Untrusted deserialization can be dangerous.

### Coupling

The serialized format is tightly coupled to Java class structure.

### Interoperability

Other languages don't naturally understand Java's native serialization format.

### Evolution

Changing classes while maintaining serialized compatibility can become complicated.

For distributed systems and APIs, explicit formats such as JSON or Protocol Buffers are often preferred.

---

# 35. Interview cheat sheet

```text
Serializable
    ↓
marker interface

ObjectOutputStream
    ↓
serialization

ObjectInputStream
    ↓
deserialization

transient
    ↓
exclude instance field from default serialization

serialVersionUID
    ↓
version/compatibility check

NotSerializableException
    ↓
non-serializable object encountered

InvalidClassException
    ↓
often serialization compatibility/version problem

Externalizable
    ↓
explicit serialization control

readObject()
    ↓
custom deserialization

writeObject()
    ↓
custom serialization

readResolve()
    ↓
replace object returned after deserialization
```

---

## The 5 questions I'd expect in an interview

**1. What is serialization?**

> Converting an object's state into a byte representation that can be stored or transmitted.

**2. What is `Serializable`?**

> A marker interface indicating that instances can participate in Java's serialization mechanism.

**3. What is `transient`?**

> Excludes an instance field from default serialization.

**4. What is `serialVersionUID`?**

> A version identifier used to check serialized-form compatibility during deserialization.

**5. Why can Java serialization be dangerous?**

> Deserializing untrusted data can trigger security vulnerabilities, so native Java serialization should not be blindly used for untrusted input.

---

### Next topic

**Modern Java — Java 8 → Java 21**: `var`, private interface methods, `List.of`, `Set.of`, `Map.of`, `String` improvements, records, sealed classes, pattern matching, switch expressions, text blocks, and **virtual threads**.

