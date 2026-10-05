# Java JVM Memory Areas

A useful way to understand JVM memory is to stop thinking of it as **one big memory area**.

The JVM divides memory into different areas, and each area has a different responsibility.

```text
                    JVM Process
                         │
        ┌────────────────┴────────────────┐
        │                                 │
   Shared by Threads                Per-Thread Memory
        │                                 │
   ┌────┴────────┐                 ┌──────┴───────┐
   │             │                 │              │
 Heap      Method Area          JVM Stack     PC Register
   │             │                 │
   │             │                 └── Native Method Stack
   │             │
   └─────────────┴────────────────────────────────
```

The **most important distinction** is:

> **Heap = objects**
> **Stack = method execution / local variables / references**
> **Method Area = class-level information**
> **PC Register = where the current thread is executing**
> **Native Method Stack = native code execution**

Let's go through each one.

---

# 1. First: What happens when Java code runs?

Consider:

```java
public class Person {

    static String company = "ABC";

    String name;

    public Person(String name) {
        this.name = name;
    }

    public void sayHello() {
        String message = "Hello " + name;
        System.out.println(message);
    }

    public static void main(String[] args) {

        Person person = new Person("Riyaz");

        person.sayHello();
    }
}
```

When you run:

```bash
java Person
```

the JVM roughly goes through:

```text
.class file
   │
   ▼
Class Loader
   │
   ▼
Class information loaded
   │
   ▼
main() starts
   │
   ├── Stack frame created
   │
   ├── Person object created → Heap
   │
   └── sayHello() called
           │
           └── another stack frame
```

This gives us the basic mental model.

---

# 2. JVM Memory Overview

The JVM specification defines several runtime data areas.

```text
                     JVM
                      │
       ┌──────────────┴──────────────┐
       │                             │
       │        Shared Areas         │
       │                             │
       │   ┌───────────┐             │
       │   │   Heap    │             │
       │   └───────────┘             │
       │                             │
       │   ┌───────────────┐         │
       │   │ Method Area   │         │
       │   └───────────────┘         │
       │                             │
       └─────────────────────────────┘
                      │
          ┌───────────┼───────────┐
          │           │           │
       Thread 1    Thread 2    Thread 3
          │           │           │
       ┌──┴──┐     ┌──┴──┐     ┌──┴──┐
       │Stack│     │Stack│     │Stack│
       │ PC  │     │ PC  │     │ PC  │
       └─────┘     └─────┘     └─────┘
```

There are **two major categories**:

### Shared

Shared by all threads:

* Heap
* Method Area

### Per-thread

Each thread gets its own:

* JVM Stack
* PC Register
* Native Method Stack

---

# 3. Heap

The **Heap** is where Java objects are generally allocated.

For example:

```java
Person person = new Person("Riyaz");
```

The object:

```text
new Person("Riyaz")
```

is created on the heap.

Conceptually:

```text
Stack                         Heap

person ───────────────────►  Person object
                              ├── name ───► "Riyaz"
                              └── ...
```

The variable `person` itself is a **local variable/reference stored in the current stack frame**, while the actual `Person` object lives on the heap.

---

## Example

```java
public static void main(String[] args) {

    Person p1 = new Person("Alice");
    Person p2 = new Person("Bob");
}
```

Conceptually:

```text
             Stack
        ┌───────────────┐
main()  │ p1 ───────────┼──────────┐
frame   │ p2 ───────────┼──────┐   │
        └───────────────┘      │   │
                               ▼   ▼
             Heap          ┌──────┐ ┌──────┐
                           │Person│ │Person│
                           │Alice │ │ Bob  │
                           └──────┘ └──────┘
```

---

# 4. Why do we need the Heap?

Because objects often need to live longer than a single method call.

Consider:

```java
static Person createPerson() {
    Person p = new Person("Alice");
    return p;
}
```

When `createPerson()` finishes:

```text
createPerson() stack frame
        │
        │ p
        ▼
     Person object
        ↑
        │
returned reference
```

The stack frame disappears.

But the object can remain:

```text
Heap
┌──────────────────┐
│ Person("Alice")  │
└──────────────────┘
         ▲
         │
         │ returned reference
```

That's why objects are allocated in heap memory.

---

# 5. Garbage Collection happens mainly in the Heap

Suppose:

```java
Person p = new Person("Alice");

p = null;
```

Initially:

```text
Stack             Heap

p ─────────────► Person
```

After:

```java
p = null;
```

we have:

```text
Stack             Heap

p = null          Person
                  ↑
                  no reference
```

The object is now **eligible for garbage collection**.

Important:

> `p = null` does NOT immediately destroy the object.

It only means there is no longer a reachable reference from `p`.

Later, the **Garbage Collector (GC)** may reclaim that memory.

---

# 6. Heap is Shared Between Threads

Suppose we have:

```java
Thread 1
Thread 2
Thread 3
```

All threads can access objects in the same heap.

```text
                    Heap
             ┌─────────────────┐
             │ Person object   │
             │ Account object  │
             │ Order object    │
             └─────────────────┘
                ▲      ▲     ▲
                │      │     │
             Thread1 Thread2 Thread3
```

This is why shared mutable objects can cause **thread-safety problems**.

For example:

```java
account.balance++;
```

If multiple threads modify the same `account`, synchronization/concurrency mechanisms may be required.

---

# 7. JVM Stack

Now the most commonly misunderstood area.

Every Java thread has its **own JVM stack**.

When a method is called, the JVM creates a **stack frame** for that method.

Example:

```java
public static void main(String[] args) {

    int x = 10;

    calculate(x);
}

static void calculate(int number) {

    int result = number * 2;

    System.out.println(result);
}
```

Execution:

```text
main()
  │
  │ calls calculate()
  ▼
calculate()
```

Stack:

```text
┌──────────────────────┐
│ calculate() frame    │
│                      │
│ number = 10          │
│ result = 20          │
└──────────────────────┘
┌──────────────────────┐
│ main() frame         │
│                      │
│ args                 │
│ x = 10               │
└──────────────────────┘
```

The newest method call is on top.

---

# 8. What is a Stack Frame?

A stack frame represents one method invocation.

Conceptually it contains things such as:

* local variables
* operand stack
* reference to runtime constant pool
* information needed for returning from the method

For example:

```java
static int add(int a, int b) {

    int result = a + b;

    return result;
}
```

When `add()` executes:

```text
add() stack frame

┌──────────────────────┐
│ Local variables      │
│                      │
│ a = 10               │
│ b = 20               │
│ result = 30          │
│                      │
│ Operand stack        │
│                      │
│ ...                  │
└──────────────────────┘
```

---

# 9. Local Variables

Primitive local variables are stored in the method's frame.

Example:

```java
void calculate() {

    int age = 30;
    double salary = 50000.0;
    boolean active = true;
}
```

Conceptually:

```text
Stack frame

┌────────────────────┐
│ age = 30           │
│ salary = 50000     │
│ active = true      │
└────────────────────┘
```

But be careful with references.

```java
Person person = new Person();
```

Conceptually:

```text
Stack                         Heap

person ───────────────────►  Person object
```

So:

> The reference/local variable is in the stack frame, while the object it refers to is on the heap.

This distinction is extremely important in Java interviews.

---

# 10. What happens when a method returns?

Suppose:

```java
main()
  ↓
methodA()
  ↓
methodB()
```

Stack:

```text
┌──────────────┐
│ methodB()    │
├──────────────┤
│ methodA()    │
├──────────────┤
│ main()       │
└──────────────┘
```

When `methodB()` finishes:

```text
┌──────────────┐
│ methodA()    │
├──────────────┤
│ main()       │
└──────────────┘
```

Its stack frame is removed.

Then `methodA()` finishes:

```text
┌──────────────┐
│ main()       │
└──────────────┘
```

This is why stack memory is naturally associated with method execution.

---

# 11. Stack Overflow

Because stack frames are created for method calls, excessive recursion can exhaust stack memory.

Example:

```java
static void infinite() {
    infinite();
}
```

Execution:

```text
infinite()
   ↓
infinite()
   ↓
infinite()
   ↓
infinite()
   ↓
...
```

Stack becomes:

```text
┌──────────────┐
│ infinite()   │
├──────────────┤
│ infinite()   │
├──────────────┤
│ infinite()   │
├──────────────┤
│ infinite()   │
├──────────────┤
│ ...          │
└──────────────┘
        ↓
StackOverflowError
```

This produces:

```text
java.lang.StackOverflowError
```

---

# 12. Heap vs Stack

This is one of the most important Java interview comparisons.

| Heap                                    | Stack                                            |
| --------------------------------------- | ------------------------------------------------ |
| Stores objects                          | Stores method execution frames                   |
| Shared between threads                  | Each thread has its own                          |
| Managed largely by GC                   | Frames automatically removed when methods return |
| Usually much larger                     | Usually smaller                                  |
| Objects can live for arbitrary duration | Frame exists during method invocation            |
| `OutOfMemoryError` can occur            | `StackOverflowError` can occur                   |

A simple mental model:

```text
STACK                         HEAP

method frames                 objects
local variables               instance data
references ─────────────────► objects
```

---

# 13. Method Area

Now let's look at the **Method Area**.

The Method Area stores information related to **classes** rather than individual objects.

When this class is loaded:

```java
class Person {

    static String company = "ABC";

    int age;

    void hello() {
        System.out.println("Hello");
    }
}
```

The JVM needs class-level information such as:

```text
Person class

- class metadata
- method information
- field information
- runtime constant pool
- bytecode-related information
- static fields
```

Conceptually:

```text
Method Area
┌──────────────────────────────┐
│ Person class metadata        │
│                              │
│ fields: age                  │
│ methods: hello()             │
│ class information             │
│ runtime constant pool        │
│ static information           │
└──────────────────────────────┘
```

---

# 14. Method Area vs Metaspace

This is an important Java version distinction.

The **JVM specification** defines the concept of a **Method Area**.

But HotSpot's implementation uses **Metaspace** for class metadata.

Since Java 8:

```text
Method Area
     │
     ▼
HotSpot implementation
     │
     ▼
Metaspace
```

Before Java 8, HotSpot used **PermGen (Permanent Generation)** for much of this information.

So:

```text
Java 7 and earlier:
Method Area → PermGen (HotSpot)

Java 8+:
Method Area → Metaspace (HotSpot)
```

Don't say:

> "Method Area and Metaspace are exactly the same thing."

Better:

> **Method Area is a JVM specification concept; Metaspace is HotSpot's implementation for class metadata.**

---

# 15. What about Static Variables?

Consider:

```java
class Counter {

    static int count = 0;

    int id;
}
```

The important idea is:

```text
Class-level information
        ↓
Method Area / class metadata

Object instance data
        ↓
Heap
```

There are JVM implementation details around where static fields are physically stored, so for interviews it's better not to oversimplify it as:

> "All static variables are stored in Method Area."

Instead:

> Static fields belong to the class rather than individual objects, and their storage is associated with class-level runtime data; exact physical placement is JVM implementation-specific.

---

# 16. Runtime Constant Pool

The Method Area also conceptually includes a **runtime constant pool**.

For example:

```java
String name = "Riyaz";
```

The class file contains constants and symbolic references.

The JVM uses the runtime constant pool for things such as:

* literals
* symbolic references
* class references
* method references
* field references

Conceptually:

```text
.class file
     │
     ▼
Class loading
     │
     ▼
Runtime Constant Pool
```

---

# 17. PC Register

PC means **Program Counter**.

Every JVM thread has its own PC register.

Its purpose is essentially:

> **Keep track of which JVM instruction that thread should execute next.**

Imagine bytecode instructions:

```text
Instruction 0
Instruction 1
Instruction 2
Instruction 3
Instruction 4
```

The PC keeps track of the current execution position.

```text
PC
 │
 ▼
Instruction 3
```

Then:

```text
Instruction 4
```

and so on.

---

# 18. Why does every thread need its own PC?

Consider:

```text
Thread 1                    Thread 2

PC → instruction 20         PC → instruction 45
```

Both threads can execute different instructions simultaneously.

Therefore each thread needs its own execution position.

```text
Thread 1
 ├── Stack
 └── PC

Thread 2
 ├── Stack
 └── PC
```

This is why the PC register is **per-thread**.

---

# 19. Native Method Stack

Java can call native code, usually through mechanisms such as JNI.

For example:

```java
native void someNativeMethod();
```

The method implementation may be written in:

```text
C
C++
Rust / other native integrations
```

The JVM needs native execution support.

That's where the **Native Method Stack** concept comes in.

```text
Java code
    │
    ▼
Native method
    │
    ▼
Native execution
```

Not every JVM implementation necessarily uses exactly the same structure, but the JVM specification defines the concept.

---

# 20. Complete Picture

Now put everything together.

```text
                         JVM
                          │
        ┌─────────────────┴─────────────────┐
        │                                   │
        │           SHARED                  │
        │                                   │
        │    ┌──────────────────────┐       │
        │    │        HEAP          │       │
        │    │                      │       │
        │    │ Objects              │       │
        │    │ Arrays               │       │
        │    │ Instance data        │       │
        │    │                      │       │
        │    │ GC manages this      │       │
        │    └──────────────────────┘       │
        │                                   │
        │    ┌──────────────────────┐       │
        │    │     METHOD AREA      │       │
        │    │                      │       │
        │    │ Class metadata       │       │
        │    │ Runtime constant pool│       │
        │    │ Method information   │       │
        │    │                      │       │
        │    │ HotSpot → Metaspace  │       │
        │    └──────────────────────┘       │
        │                                   │
        └───────────────────────────────────┘

             PER THREAD
                  │
       ┌──────────┼──────────┐
       │          │          │
       ▼          ▼          ▼
    Stack        PC       Native
                         Method Stack
       │
       ▼
  Stack Frames
       │
       ├── local variables
       ├── operand stack
       └── method execution data
```

---

# 21. Let's Trace One Program

This is where the whole thing becomes much clearer.

```java
class Person {

    static String company = "ABC";

    String name;

    Person(String name) {
        this.name = name;
    }

    void greet() {
        String message = "Hello " + name;
        System.out.println(message);
    }

    public static void main(String[] args) {

        Person person = new Person("Riyaz");

        person.greet();
    }
}
```

## Step 1 — Class loading

The JVM loads `Person`.

Conceptually:

```text
Method Area / Metaspace

Person
├── class metadata
├── constructor information
├── greet() information
├── main() information
├── runtime constant pool
└── static class data
```

---

## Step 2 — `main()` starts

A thread executes:

```java
main()
```

A stack frame is created:

```text
Stack

┌──────────────────┐
│ main()           │
│                  │
│ args             │
└──────────────────┘
```

---

## Step 3 — Create Person

Code:

```java
Person person = new Person("Riyaz");
```

The JVM allocates a `Person` object on the heap.

```text
Stack                         Heap

main()                        Person
person ───────────────────►   ├── name ──► "Riyaz"
                              └── ...
```

---

## Step 4 — Call `greet()`

```java
person.greet();
```

A new stack frame is created.

```text
Stack

┌──────────────────────┐
│ greet()              │
│                      │
│ message              │
└──────────────────────┘
┌──────────────────────┐
│ main()               │
│                      │
│ person ──────────────┼───────────────┐
└──────────────────────┘               │
                                       ▼
                                     Heap
                                  ┌──────────┐
                                  │ Person   │
                                  │ Riyaz    │
                                  └──────────┘
```

---

## Step 5 — `greet()` finishes

Its stack frame disappears.

```text
Stack

┌──────────────────────┐
│ main()               │
│ person ──────────────┼──────► Person
└──────────────────────┘
```

The `Person` object is still alive because `person` still references it.

---

## Step 6 — `main()` finishes

The main stack frame disappears.

If nothing else references the object:

```text
Heap

Person("Riyaz")

    ↓

unreachable
```

The object becomes **eligible for GC**.

---

# 22. One Very Important Correction

You'll often hear:

> "Primitive variables are stored in stack and objects are stored in heap."

This is a **useful beginner model**, but it is not a perfect description of the JVM specification.

For example:

```java
class Person {
    int age;
}
```

`age` is part of the `Person` object.

Therefore:

```java
Person p = new Person();
```

conceptually:

```text
Stack                  Heap
─────                  ────

p ─────────────────► Person
                    └── age = 30
```

So `age` is not a separate stack variable just because it is an `int`.

The better rule is:

> **Local variables belong to stack frames; object instance fields belong to objects.**

---

# 23. Another Important Point: Arrays

Arrays are objects too.

```java
int[] numbers = new int[5];
```

Conceptually:

```text
Stack                    Heap

numbers ─────────────►  int[]
                        ├── 0
                        ├── 0
                        ├── 0
                        ├── 0
                        └── 0
```

Even though the array contains primitives, the **array itself is an object on the heap**.

Similarly:

```java
Person[] people = new Person[10];
```

The array is on the heap and contains references.

```text
Stack

people ────────────────► Heap

                         Person[] 
                         ├── ref
                         ├── ref
                         ├── ref
                         └── ...
```

---

# 24. How GC fits into all of this

The Garbage Collector primarily manages heap objects.

```text
                 Heap
                  │
       ┌──────────┼──────────┐
       ▼          ▼          ▼
    Object A   Object B   Object C
       ▲                     │
       │                     │
    reachable             unreachable
                              │
                              ▼
                             GC
                              │
                              ▼
                         memory reclaimed
```

Modern JVMs commonly divide the heap into generations/regions depending on the collector.

For example, with a generational collector:

```text
Heap
│
├── Young Generation
│   ├── Eden
│   └── Survivor
│
└── Old Generation
```

The exact layout depends on the garbage collector.

This is a separate topic, but it becomes much easier once you understand the JVM memory areas.

---

# 25. Common Errors and Their Memory Areas

This is very useful for interviews.

### `StackOverflowError`

Usually caused by excessive stack usage:

```java
void recurse() {
    recurse();
}
```

```text
JVM Stack
    ↓
too many frames
    ↓
StackOverflowError
```

---

### `OutOfMemoryError: Java heap space`

Typically means the JVM couldn't allocate more heap memory.

Example:

```java
List<byte[]> list = new ArrayList<>();

while (true) {
    list.add(new byte[1024 * 1024]);
}
```

Eventually:

```text
Heap
 ↓
full
 ↓
OutOfMemoryError
```

---

### Metaspace OOM

Excessive class metadata/class loading can result in:

```text
OutOfMemoryError: Metaspace
```

For example, applications that continually generate/load classes can cause this.

---

# 26. The Best Mental Model

If you remember only this, remember:

```text
                    JVM
                     │
       ┌─────────────┴──────────────┐
       │                            │
    SHARED                      PER THREAD
       │                            │
   ┌───┴────┐                ┌──────┼──────┐
   │        │                │      │      │
 Heap   Method Area         Stack   PC   Native
   │        │                │             Stack
   │        │                │
   │        │                └── method
   │        │                    frames
   │        │
   │        └── class information
   │
   └── objects / arrays
```

And for everyday Java code:

```text
new Person()
      │
      ▼
    HEAP
      ▲
      │
reference
      │
      │
    STACK
```

While:

```text
class Person
      │
      ▼
Method Area / Metaspace
```

And:

```text
Thread
  │
  ├── Stack → current method calls
  ├── PC    → current execution position
  └── Native Stack → native methods
```

---

# 27. Interview-Level Summary

| Memory area             | Shared? | Main purpose                         |
| ----------------------- | ------: | ------------------------------------ |
| **Heap**                |     Yes | Objects and arrays                   |
| **Method Area**         |     Yes | Class-level runtime information      |
| **JVM Stack**           |      No | Method calls and stack frames        |
| **PC Register**         |      No | Current JVM instruction for a thread |
| **Native Method Stack** |      No | Native method execution              |

### The three most important relationships

```text
Object
  ↓
Heap

Method call
  ↓
Stack frame
  ↓
JVM Stack

Class loaded
  ↓
Class metadata
  ↓
Method Area / Metaspace
```

### And the errors

```text
Too many method calls
        ↓
StackOverflowError


Too many heap objects
        ↓
OutOfMemoryError: Java heap space


Too much class metadata
        ↓
OutOfMemoryError: Metaspace
```

The next concept that naturally follows this is **JVM Heap structure + Garbage Collection (Young Generation, Eden, Survivor, Old Generation, Minor/Young GC, Major/Full GC)**, because that explains *what actually happens to `new Person()` after it is created*.


---
---
---
---









































Yes — this is one of those JVM topics where terminology causes a lot of confusion. The key is to separate **JVM specification terms** from **HotSpot implementation terms**.

## 1. What exactly is Metaspace?

**Metaspace is a memory area used by the HotSpot JVM to store class metadata.**

For example, when the JVM loads:

```java
class Person {

    private String name;

    public void greet() {
        System.out.println("Hello");
    }
}
```

the JVM needs to remember information about the `Person` class:

```text
Person
├── Class metadata
├── Fields
│   └── name
├── Methods
│   └── greet()
├── Method-related information
├── Runtime constant pool
└── Other class-loading information
```

This information needs memory.

In modern HotSpot JVMs, much of this class metadata is stored in **Metaspace**.

---

# 2. Is Metaspace part of the Heap?

### No.

This is the most important point:

```text
JVM Process
│
├── Heap
│   └── Java objects
│
└── Native memory
    └── Metaspace
        └── Class metadata
```

So:

```text
Heap
  ❌ Metaspace is NOT part of this

Native memory
  ✅ Metaspace is here in HotSpot
```

For example:

```java
Person person = new Person("Riyaz");
```

Conceptually:

```text
                JVM
                 │
       ┌─────────┴─────────┐
       │                   │
      Heap              Metaspace
       │                   │
       ▼                   ▼
Person object         Person class
       │              metadata
       │
name = "Riyaz"
```

The **object** is in the heap.

The **class metadata** is in Metaspace.

---

# 3. Then what is Method Area?

This is where the terminology gets tricky.

**Method Area is a JVM specification concept.**

The JVM specification says that there is a runtime data area called the:

> **Method Area**

It is intended to store information related to classes.

But the JVM specification does **not** say:

> "You must implement the Method Area using a thing called Metaspace."

That's an implementation decision.

---

# 4. So is Metaspace the Method Area?

### In HotSpot, approximately yes.

More precisely:

> **Method Area is the JVM specification concept; Metaspace is the HotSpot implementation that stores class metadata associated with the Method Area.**

Think:

```text
JVM Specification
        │
        ▼
   Method Area
   (concept)
        │
        │ implemented by
        ▼
HotSpot JVM
        │
        ▼
   Metaspace
```

So don't think:

```text
Method Area
    └── Metaspace
```

as if Metaspace is a smaller section inside Method Area.

Instead, think:

```text
             JVM Specification
                    │
                    ▼
              Method Area
              (logical concept)
                    │
                    ▼
             HotSpot JVM
                    │
                    ▼
                Metaspace
              (implementation)
```

---

# 5. Why was Metaspace introduced?

This becomes clearer if we look at older Java versions.

## Before Java 8

HotSpot used something called:

**PermGen — Permanent Generation**

Conceptually:

```text
Java Heap
│
├── Young Generation
├── Old Generation
└── Permanent Generation
```

PermGen was part of the JVM's heap management model.

It was used for things such as class metadata.

This could cause problems because PermGen had a fixed/configured size.

You might see:

```text
java.lang.OutOfMemoryError:
PermGen space
```

---

# 6. Java 8 changed this

Java 8 removed PermGen from HotSpot and introduced **Metaspace**.

Conceptually:

```text
Before Java 8

Heap
│
├── Objects
└── PermGen
      └── Class metadata


Java 8+

Heap
│
└── Objects


Native Memory
│
└── Metaspace
      └── Class metadata
```

This is one of the major reasons you'll hear:

> "Metaspace is outside the heap."

---

# 7. Why move class metadata outside the heap?

One important reason was to allow class metadata to use **native memory** rather than being constrained by the Java heap.

Imagine an application dynamically loading lots of classes.

For example:

```text
Application
   │
   ├── Class A
   ├── Class B
   ├── Class C
   ├── Class D
   ├── ...
   └── Class 1,000,000
```

The JVM needs memory for all that class metadata.

With Metaspace:

```text
Native Memory
│
└── Metaspace
      │
      ├── Class A metadata
      ├── Class B metadata
      ├── Class C metadata
      └── ...
```

Metaspace can grow as needed, subject to limits and available native memory.

You can control its maximum size using:

```bash
-XX:MaxMetaspaceSize=...
```

---

# 8. What exactly goes into Metaspace?

Don't think of it as simply:

```text
"all information about the class"
```

There are many JVM implementation details.

But at a high level, class metadata can include information such as:

```text
Person.class
│
├── Class metadata
├── Class hierarchy information
├── Field metadata
├── Method metadata
├── Method bytecode-related information
├── Runtime constant pool information
├── Annotations / related metadata
└── Other JVM internal class information
```

The exact organization is JVM implementation-specific.

---

# 9. What does NOT go into Metaspace?

This is equally important.

Suppose:

```java
Person p1 = new Person("Alice");
Person p2 = new Person("Bob");
Person p3 = new Person("Charlie");
```

You don't get three copies of the `Person` class metadata.

Conceptually:

```text
Metaspace

Person class metadata
       │
       │
       ├────────────┐
       │            │
       ▼            ▼
Heap

Person object    Person object
Alice            Bob
       │
       ▼
Person object
Charlie
```

The class metadata is associated with the loaded `Person` class.

The individual objects are on the heap.

---

# 10. What happens when we create an object?

Let's trace:

```java
Person p = new Person("Riyaz");
```

There are several things happening.

### Step 1 — `Person` class must be loaded

The Class Loader loads `Person`.

```text
.class file
    │
    ▼
Class Loader
    │
    ▼
Person class loaded
```

HotSpot stores the relevant class metadata in Metaspace.

```text
Metaspace

Person
├── class metadata
├── methods
├── fields
└── other JVM metadata
```

---

### Step 2 — JVM creates the object

```java
new Person("Riyaz")
```

The actual object is allocated in the heap.

```text
Heap

┌─────────────────────┐
│ Person object       │
│                     │
│ name ───────────────┼──► "Riyaz"
└─────────────────────┘
```

---

### Step 3 — Reference is stored

The local variable:

```java
Person p
```

is part of the current method's execution state, conceptually in its stack frame.

```text
Stack                         Heap
─────                         ────

p ─────────────────────────► Person object
                              name = "Riyaz"


                              Metaspace
                              ─────────
                              Person class metadata
```

So you have three different things:

```text
Stack
  ↓
reference/local variable

Heap
  ↓
actual Person object

Metaspace
  ↓
Person class metadata
```

This is a **very useful mental model**.

---

# 11. One class, many objects

Suppose:

```java
Person p1 = new Person("A");
Person p2 = new Person("B");
Person p3 = new Person("C");
```

You have:

```text
                    Metaspace
                 ┌──────────────┐
                 │ Person class │
                 │   metadata   │
                 └──────┬───────┘
                        │
                        │
              ┌─────────┴─────────┐
              │                   │
              ▼                   ▼
             Heap                Heap
        ┌────────────┐       ┌────────────┐
        │ Person A   │       │ Person B   │
        └────────────┘       └────────────┘
                  \
                   \
                    ▼
               ┌────────────┐
               │ Person C   │
               └────────────┘
```

The class metadata isn't duplicated for every object.

---

# 12. What about methods?

Suppose:

```java
class Person {

    void greet() {
        System.out.println("Hello");
    }
}
```

There is only one `greet()` method associated with the loaded `Person` class.

You might have:

```java
Person p1 = new Person();
Person p2 = new Person();
Person p3 = new Person();
```

All three objects can invoke:

```java
p1.greet();
p2.greet();
p3.greet();
```

The JVM doesn't need three independent copies of the class's method metadata simply because there are three objects.

Conceptually:

```text
             Metaspace
          Person class
               │
          greet() metadata
               │
       ┌───────┼────────┐
       ▼       ▼        ▼
     p1       p2       p3
     Heap     Heap     Heap
```

---

# 13. Is bytecode stored in Metaspace?

At a high level, you can associate method/class metadata with Metaspace, but be careful with saying:

> "The `.class` file is stored in Metaspace."

That's not correct.

The `.class` file is an input artifact, for example:

```text
Person.class
```

The JVM loads and processes it.

```text
Person.class
     │
     ▼
Class Loader
     │
     ▼
JVM runtime representation
     │
     ▼
Metaspace / other JVM-managed memory
```

The JVM's internal representation isn't simply the original `.class` file copied into Metaspace.

---

# 14. Is Metaspace managed by Garbage Collection?

### Yes, but differently from normal heap objects.

This is another important distinction.

Suppose a class is loaded:

```text
Person
```

and later its class loader becomes unreachable.

If the JVM determines that the class can be unloaded, its associated metadata can eventually be reclaimed.

Conceptually:

```text
ClassLoader
     │
     ▼
Person class
     │
     ▼
Metaspace metadata
```

If the class loader becomes unreachable and the JVM can unload the class:

```text
ClassLoader unreachable
        ↓
Class can be unloaded
        ↓
Class metadata reclaimed
```

This is particularly important in:

* application servers
* plugin systems
* frameworks
* hot deployment
* dynamically generated classes

---

# 15. Why can Metaspace cause OOM?

Because Metaspace uses native memory.

Imagine an application continuously loads/generates classes:

```text
Class 1
Class 2
Class 3
Class 4
...
Class 1,000,000
```

If those classes cannot be unloaded:

```text
Metaspace
████████████████████████████████
████████████████████████████████
████████████████████████████████
```

Eventually:

```text
OutOfMemoryError: Metaspace
```

You can limit it:

```bash
-XX:MaxMetaspaceSize=256m
```

---

# 16. One subtle but important distinction

You asked:

> "is method area created inside metaspace or metaspace itself method area?"

The best answer is:

### Neither phrasing is quite correct.

Think of it as:

```text
Method Area
    ↓
JVM specification concept

Metaspace
    ↓
HotSpot JVM implementation
```

Not:

```text
Method Area
    └── Metaspace
```

and not strictly:

```text
Metaspace
    = Method Area
```

because **Method Area is an abstract/runtime specification concept**, while **Metaspace is a concrete HotSpot implementation detail**.

---

# 17. The easiest analogy

Think about an interface and its implementation.

```java
interface List {
    ...
}
```

and:

```java
class ArrayList implements List {
    ...
}
```

`List` is the abstraction.

`ArrayList` is one implementation.

Similarly:

```text
JVM specification
       │
       ▼
  Method Area
  (concept)
       │
       ▼
 HotSpot JVM
       │
       ▼
   Metaspace
 (implementation)
```

The JVM specification doesn't require every JVM to have a physical memory region literally called "Metaspace."

Another JVM implementation could organize class metadata differently.

---

# 18. Final picture

This is probably the diagram I'd keep in your notes:

```text
                         JVM
                          │
             ┌────────────┴─────────────┐
             │                          │
          HEAP                     NATIVE MEMORY
             │                          │
             │                    ┌─────┴──────┐
             │                    │            │
             │               Metaspace    Other native
             │                    │          memory
             │                    │
             ▼                    ▼
       Java Objects         Class Metadata
       and Arrays           for loaded classes
             ▲
             │
             │ reference
             │
        JVM Stack
        ─────────
        Stack Frame
        ├── local variables
        ├── references
        └── method execution
```

And conceptually:

```text
JVM SPECIFICATION
─────────────────

Method Area
    │
    │ implemented by HotSpot as
    ▼

HOTSPOT
────────

Metaspace
    │
    └── class metadata


Heap
    │
    └── objects / arrays
```

### So, remember these 4 statements:

1. **Metaspace is NOT part of the Java heap.**
2. **Metaspace is HotSpot's native-memory implementation for class metadata associated with the JVM's Method Area.**
3. **Method Area is a JVM specification concept, not necessarily a physical memory region called "Method Area."**
4. **Heap stores objects; Metaspace stores class metadata.**

The next thing that makes this click completely is **Class Loading → Loading, Linking, Initialization → ClassLoader → Metaspace → Heap**, because then you'll see *exactly when Metaspace gets used and what happens when a `.class` file enters the JVM*.



---
---
---









































Yes — this is the next confusing layer of JVM memory because **"constant pool" and "String pool" sound like the same thing, but they are not**.

The most important thing first:

> **Constant Pool ≠ String Pool**

And there are actually multiple things called "constant pool" depending on whether we're talking about the **`.class` file**, the **JVM runtime**, or **strings**.

---

# 1. First, the big picture

Think of it like this:

```text
                         JVM
                          │
        ┌─────────────────┴──────────────────┐
        │                                    │
       HEAP                              NATIVE MEMORY
        │                                    │
        │                              ┌─────┴─────┐
        │                              │ Metaspace │
        │                              └───────────┘
        │
        ├── Objects
        ├── Arrays
        │
        └── String Pool
```

But there's an important nuance:

**The String Pool is on the heap in modern HotSpot JVMs.**

The runtime constant pool is associated with the **class metadata/runtime representation** and is conceptually part of the **Method Area**. In HotSpot, the implementation has details involving Metaspace and heap objects.

Let's separate everything.

---

# 2. What is a Constant Pool?

There are two closely related concepts:

1. **Constant Pool in a `.class` file**
2. **Runtime Constant Pool**

The second one is what you'll encounter when discussing JVM memory.

---

# 3. Constant Pool inside `.class`

Suppose you write:

```java
class Person {

    String name = "Riyaz";

    void hello() {
        System.out.println("Hello");
    }
}
```

When you compile:

```text
Person.java
     │
     ▼
javac
     │
     ▼
Person.class
```

The `.class` file contains a **constant pool**.

It contains symbolic information needed by the JVM, such as references to:

```text
Person
String
name
hello
System.out
println
"Riyaz"
"Hello"
etc.
```

Conceptually:

```text
Person.class

┌──────────────────────────────┐
│ Class metadata               │
├──────────────────────────────┤
│ Constant Pool                │
│                              │
│ #1  Person                   │
│ #2  java/lang/String         │
│ #3  name                     │
│ #4  hello                    │
│ #5  "Riyaz"                  │
│ #6  "Hello"                  │
│ #7  System.out               │
│ #8  println                   │
│ ...                          │
└──────────────────────────────┘
```

This is part of the **class file format**.

---

# 4. What is Runtime Constant Pool?

When the JVM loads a class, it creates a runtime representation of that class.

The JVM specification defines a **runtime constant pool** associated with each class/interface.

It contains things such as:

* numeric constants
* string constants/references
* class references
* field references
* method references
* interface method references
* symbolic references

For example:

```java
System.out.println("Hello");
```

The JVM needs to resolve things like:

```text
java/lang/System
out
java/io/PrintStream
println
"Hello"
```

The runtime constant pool helps with that.

---

# 5. Where is Runtime Constant Pool?

Here's where your previous question about Method Area becomes important.

The JVM specification says:

> The runtime constant pool is part of the **Method Area**.

Conceptually:

```text
Method Area
│
├── Class metadata
├── Method information
├── Field information
├── Runtime Constant Pool
└── Other class-related information
```

In HotSpot:

```text
Method Area
     │
     │ implementation concept
     ▼
Metaspace + other JVM-managed structures
```

However, **do not conclude that every item in the runtime constant pool physically lives inside Metaspace**. HotSpot has implementation-specific representations, and some referenced objects—especially `String` objects—are on the heap.

This distinction matters.

---

# 6. Then what is String Pool?

Now we come to the **String Pool**, also called:

> **String Intern Pool**

Consider:

```java
String s1 = "Hello";
String s2 = "Hello";
```

Java doesn't normally create two separate `String` objects for these identical string literals.

Instead, the JVM maintains a pool of interned strings.

Conceptually:

```text
Heap

String Pool
┌────────────────────┐
│ "Hello"            │
└─────────▲──────────┘
          │
     ┌────┴────┐
     │         │
    s1        s2
```

Both references point to the same interned `String` object.

```java
System.out.println(s1 == s2);
```

Output:

```text
true
```

---

# 7. Is String Pool part of Heap?

### Yes, in modern HotSpot JVMs.

This changed historically.

Before Java 7, the String pool was associated with PermGen.

Since Java 7, interned strings are stored in the **Java heap**.

So for modern Java:

```text
Heap
│
├── Normal Objects
├── Arrays
└── String Pool
      ├── "Hello"
      ├── "Java"
      └── "Riyaz"
```

This is an important interview point.

---

# 8. Constant Pool vs String Pool

Now compare them.

|                        | Constant Pool                                                      | String Pool                         |
| ---------------------- | ------------------------------------------------------------------ | ----------------------------------- |
| Purpose                | JVM/class constants & symbolic references                          | Reuse/intern `String` objects       |
| Associated with        | Class                                                              | JVM-wide string interning mechanism |
| Contains               | Class/method/field refs, literals, etc.                            | `String` objects                    |
| JVM concept            | Runtime Constant Pool                                              | String Intern Pool                  |
| Main location          | Method Area concept                                                | Heap                                |
| HotSpot implementation | Class metadata/runtime structures, with references to heap objects | Heap                                |
| Shared?                | Associated with each loaded class/interface                        | Shared across JVM                   |

The biggest distinction:

```text
Runtime Constant Pool
        ≠
String Pool
```

---

# 9. But what happens with `"Hello"`?

This is where things get interesting.

Suppose:

```java
String s = "Hello";
```

There are **two related things** involved:

```text
.class file
    │
    │ contains symbolic/string constant information
    ▼
Runtime Constant Pool
    │
    │ refers/resolves to
    ▼
String object
    │
    ▼
String Pool on Heap
```

Conceptually:

```text
Metaspace / class runtime structures
┌───────────────────────────────┐
│ Runtime Constant Pool         │
│                               │
│ "Hello" related constant/ref  │
└───────────────┬───────────────┘
                │
                │ resolves to
                ▼
Heap
┌───────────────────────────────┐
│ String Pool                   │
│                               │
│ String object: "Hello"        │
└───────────────────────────────┘
```

This is why people sometimes incorrectly say:

> "String literals are stored in the constant pool."

That's an oversimplification.

The **class file contains a string constant entry**, while the actual interned `String` object is on the heap.

---

# 10. Let's look at `"Hello"` carefully

Consider:

```java
String s = "Hello";
```

The compiled `.class` file has constant-pool information representing `"Hello"`.

When the JVM loads/resolves the relevant constant:

```text
.class file
    │
    ▼
Constant Pool
    │
    ▼
Runtime Constant Pool
    │
    ▼
String "Hello"
    │
    ▼
String Pool
    │
    ▼
Heap
```

So don't imagine that the characters:

```text
H e l l o
```

are simply sitting inside Metaspace as a normal Java `String` object.

The actual `String` object is a heap object.

---

# 11. What about Integer constants?

Consider:

```java
int x = 100;
```

The value `100` can be represented directly in bytecode/instruction operands or class-file constant structures depending on context.

But it does **not** mean:

```text
100 → String Pool
```

Obviously not.

The String Pool is specifically about interned strings.

---

# 12. What about `Integer` Pool?

Java also has another concept that people sometimes call a "pool":

```java
Integer a = 100;
Integer b = 100;

System.out.println(a == b);
```

Usually:

```text
true
```

because Java caches certain `Integer` instances.

Conceptually:

```text
Integer Cache
┌─────┬─────┬─────┬─────┐
│ -128│ ... │ 100 │ ... │
└─────┴─────┴─────┴─────┘
```

But this is **not the Constant Pool** and not the **String Pool**.

It's a library/runtime caching mechanism.

For example:

```java
Integer a = 100;
```

can use cached `Integer` objects through boxing.

---

# 13. Other "Pools" you may hear about

There are many things in Java that are informally called pools.

They are not all JVM memory areas.

### String Pool

```text
String.intern()
```

Purpose:

> Reuse canonical String objects.

Location in modern HotSpot:

```text
Heap
```

---

### Integer Cache

```text
Integer.valueOf(...)
```

Purpose:

> Reuse certain boxed integer objects.

Not a JVM memory area.

---

### Boolean cached objects

```java
Boolean.TRUE
Boolean.FALSE
```

Again:

> Object reuse/caching, not a JVM memory area.

---

### Thread Pool

```java
ExecutorService
```

This is completely different.

```text
Thread Pool
```

is an application concurrency mechanism.

It has nothing to do with the JVM's Constant Pool or String Pool.

---

### Connection Pool

For example:

```text
Database connection pool
HTTP connection pool
```

Again, unrelated to JVM memory pools.

---

# 14. A very useful classification

Instead of memorizing every "pool", classify them.

## JVM runtime memory areas

```text
Heap
Method Area
JVM Stack
PC Register
Native Method Stack
```

---

## Class-file / class metadata structures

```text
Class-file Constant Pool
Runtime Constant Pool
```

---

## Heap-based special/cached structures

```text
String Intern Pool
Integer Cache
other cached objects
```

---

## Application-level pools

```text
Thread Pool
Connection Pool
Object Pool
etc.
```

These are completely different concepts.

---

# 15. One diagram to remember

For your JVM memory notes, I'd use this:

```text
                           JVM
                            │
          ┌─────────────────┴──────────────────┐
          │                                    │
        HEAP                              NATIVE MEMORY
          │                                    │
          │                              ┌─────┴──────┐
          │                              │ Metaspace   │
          │                              │             │
          │                              │ Class       │
          │                              │ metadata    │
          │                              └─────────────┘
          │
          ├── Java Objects
          │
          ├── Arrays
          │
          └── String Intern Pool
                    │
                    ├── "Hello"
                    ├── "Java"
                    └── "Riyaz"


     METHOD AREA (JVM SPECIFICATION CONCEPT)
                    │
                    ├── Class metadata
                    ├── Method information
                    ├── Field information
                    └── Runtime Constant Pool
                              │
                              │ may reference
                              ▼
                           HEAP OBJECTS
```

---

# 16. And the `.class` file fits here

Before the JVM even creates the runtime representation:

```text
                    Person.class
                         │
                         │ contains
                         ▼
                  Class-file Constant Pool
                         │
                         │ class loading
                         ▼
                 Runtime representation
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
         Method Area            Heap objects
         / Metaspace
              │
              └── Runtime Constant Pool
```

So there are **two different "constant pools" you should distinguish**:

### Class-file Constant Pool

Part of the `.class` file format.

### Runtime Constant Pool

Runtime structure associated with a loaded class/interface, defined by the JVM specification as part of the Method Area.

---

# 17. The interview trap

If an interviewer asks:

> **Where is the String Pool stored?**

For modern HotSpot:

**Heap.**

If they ask:

> **Where is the Runtime Constant Pool?**

Answer:

**It is part of the Method Area according to the JVM specification; HotSpot implements class metadata primarily using Metaspace, with implementation details involving other memory structures.**

If they ask:

> **Is the String Pool the same as the Runtime Constant Pool?**

Answer:

**No.**

If they ask:

> **Is Metaspace the Method Area?**

Answer:

**Method Area is the JVM specification concept; Metaspace is HotSpot's implementation for class metadata.**

---

# 18. The complete relationship

This is the mental model I'd recommend:

```text
                  .class file
                      │
                      ▼
             Class-file Constant Pool
                      │
                      │ Class Loading
                      ▼
               Loaded Class
                      │
          ┌───────────┴────────────┐
          │                        │
          ▼                        ▼
 Runtime Constant Pool       Class Metadata
          │                        │
          └──────────┬─────────────┘
                     │
                     ▼
              Method Area
              (JVM concept)
                     │
                     ▼
          HotSpot → Metaspace
```

Separately:

```text
                    Heap
                     │
          ┌──────────┴───────────┐
          │                      │
     Normal objects        String Pool
                              │
                         "Hello"
                         "Java"
                         "Riyaz"
```

And a runtime constant can **refer to** an object in the heap.

---

## The 30-second version

Remember this:

```text
CONSTANT POOL
    ↓
Class-related constants and symbolic references
    ↓
Runtime Constant Pool
    ↓
Method Area concept
    ↓
HotSpot → class metadata largely in Metaspace


STRING POOL
    ↓
Interned String objects
    ↓
Heap
```

And **"pool" does not automatically mean "JVM memory area."**

That's the key distinction.


