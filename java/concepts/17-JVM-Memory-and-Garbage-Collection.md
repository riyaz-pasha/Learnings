## 17. JVM Memory & Garbage Collection

This is a **high-value EPAM interview topic** because it connects Java fundamentals, performance, objects, threads, and the JVM.

The first thing to understand is that **Java memory is not simply "heap and stack."** There are several JVM runtime areas.

---

# 1. JVM Runtime Memory Areas

A simplified picture:

```text
JVM
│
├── Heap
│   └── Objects / Arrays
│
├── Java Stack
│   └── Per-thread stack frames
│
├── PC Register
│   └── Current instruction per thread
│
├── Method Area
│   └── Class-level/runtime metadata
│
└── Native Method Stack
    └── Native method execution
```

The most important ones for interviews are:

```text
Heap
Stack
Method Area / Metaspace
```

---

# 2. Heap

The **heap** is where objects and arrays are generally allocated.

Example:

```java
Employee employee = new Employee();
```

Conceptually:

```text
Stack                  Heap

employee ───────────► Employee object
                         |
                         | name
                         | age
                         |
```

The local variable `employee` is a reference stored in the current stack frame, while the actual object is allocated on the heap.

### Important

The exact JVM implementation and optimizations can be more complicated than this simplified model. For interview purposes, this is the correct mental model.

---

# 3. What lives in the heap?

Typically:

```java
Employee employee = new Employee();
```

The object:

```text
Employee
```

is on the heap.

Arrays:

```java
int[] numbers = new int[100];
```

are also heap objects.

Objects referenced by fields are also heap objects.

---

# 4. Stack

Each thread has its own Java stack.

For example:

```java
public static void main(String[] args) {
    calculate();
}

static void calculate() {
    int x = 10;
    int y = 20;
    int result = x + y;
}
```

When `main()` calls `calculate()`, a new **stack frame** is created.

Conceptually:

```text
Thread Stack

┌──────────────────┐
│ calculate() frame│
│ x = 10           │
│ y = 20           │
│ result = 30      │
├──────────────────┤
│ main() frame     │
│ args             │
└──────────────────┘
```

When `calculate()` finishes, its frame is removed.

---

# 5. Stack frame

Each method invocation gets a stack frame.

A frame contains things such as:

* local variables
* operand stack
* reference to runtime constant pool information
* return information

The exact JVM specification details are more nuanced, but this is the useful interview model.

---

# 6. Why does each thread have its own stack?

Suppose two threads execute:

```java
calculate();
```

Each thread needs its own method execution state.

So:

```text
Thread A                  Thread B

Stack A                   Stack B
┌───────────┐             ┌───────────┐
│ calculate │             │ calculate │
│ x = 10    │             │ x = 50    │
└───────────┘             └───────────┘
```

They don't share their Java stack frames.

But both threads can access shared heap objects.

This distinction becomes extremely important in multithreading.

---

# 7. Heap vs Stack

| Heap                                    | Stack                                    |
| --------------------------------------- | ---------------------------------------- |
| Objects/arrays generally allocated here | Method frames/local execution state      |
| Shared among threads                    | Each thread has its own                  |
| Managed by GC                           | Frames removed as methods return         |
| Generally larger                        | Usually much smaller                     |
| Object lifetime can outlive method call | Frame lifetime tied to method invocation |

---

# 8. What about local primitive variables?

Consider:

```java
int x = 10;
```

Inside a method, `x` is part of the method's local execution state, represented in its stack frame.

For:

```java
Employee e = new Employee();
```

the reference `e` is part of the local frame, while the Employee object is on the heap in the conventional model.

So:

```text
Stack                    Heap

e ───────────────────► Employee
                         name
                         age
```

---

# 9. What is Metaspace?

Older Java versions commonly described class metadata as being stored in the **PermGen (Permanent Generation)**.

Java 8 removed PermGen and introduced **Metaspace**.

Metaspace stores JVM class metadata and is generally backed by native memory rather than being part of the Java heap.

Conceptually:

```text
JVM
│
├── Heap
│
├── Stacks
│
└── Metaspace
      └── class metadata
```

### Interview question

> What replaced PermGen?

**Metaspace in Java 8.**

---

# 10. Why was PermGen replaced?

PermGen had a fixed/limited region associated with the heap and could be difficult to size appropriately for applications that dynamically loaded many classes.

Metaspace uses native memory and can grow subject to configuration and available memory.

This doesn't mean Metaspace is unlimited.

You can configure limits such as:

```text
-XX:MaxMetaspaceSize
```

---

# 11. Garbage Collection

Now the important part.

Suppose:

```java
Employee e = new Employee();
```

Later:

```java
e = null;
```

If nothing else references the Employee object:

```text
Stack

e ───► null

Heap

Employee object
      ↑
      │
   no references
```

The object has become **unreachable**.

It becomes eligible for garbage collection.

---

# 12. Garbage Collection does NOT mean immediately deleting the object

This is an important distinction.

When an object becomes unreachable:

> It becomes **eligible** for garbage collection.

It doesn't mean:

> The JVM immediately destroys it.

The JVM's garbage collector decides when and how to reclaim the memory.

---

# 13. How does GC know what is garbage?

A simplified model uses **reachability**.

The JVM starts from **GC roots**.

Examples of roots can include:

```text
active thread references
local variables in active stack frames
static references
JNI references
```

Conceptually:

```text
GC Roots
   |
   +----► Object A
            |
            +----► Object B

Object C
   |
   +----► Object D
```

Objects A and B are reachable.

Objects C and D may be unreachable if nothing from the roots reaches them.

Those objects can become eligible for collection.

---

# 14. Circular references

This is a classic question.

Suppose:

```text
A ───► B
▲      │
└──────┘
```

A references B and B references A.

Are they garbage?

**They can be.**

If nothing outside that cycle references either object:

```text
GC Roots

(no reference)

A ◄──► B
```

then the cycle is unreachable.

Modern tracing garbage collectors don't simply rely on reference counting, so circular references don't inherently prevent collection.

---

# 15. Generational Garbage Collection

Most Java garbage collectors use some form of generational approach or have concepts based around object age, although exact implementation varies by collector.

The traditional conceptual model is:

```text
Heap
│
├── Young Generation
│
│   ├── Eden
│   ├── Survivor 0
│   └── Survivor 1
│
└── Old Generation
```

---

# 16. Eden

Most newly created objects are initially allocated in the **young generation**, commonly starting in Eden.

Example:

```java
Employee e = new Employee();
```

Conceptually:

```text
Young Generation

Eden
┌─────────────────┐
│ Employee        │
│ Order           │
│ StringBuilder   │
└─────────────────┘
```

Many objects become unreachable quickly.

For example:

```java
for (...) {
    new TemporaryObject();
}
```

Those temporary objects may not live very long.

---

# 17. Minor/Young GC

When the young generation fills up, the JVM can perform a young-generation collection.

Live objects are retained/moved according to the collector's algorithm, while unreachable objects can have their memory reclaimed.

Objects that survive collections can eventually become candidates for the old generation.

The exact terminology and behavior differs across collectors, so don't overstate that every collector literally follows the same Eden → Survivor → Old process.

---

# 18. Survivor spaces

Traditional generational collectors have:

```text
Eden
Survivor 0
Survivor 1
```

Live objects can move between survivor spaces over multiple collections.

Conceptually:

```text
Eden
  |
  | GC
  ↓
Survivor 0
  |
  | next GC
  ↓
Survivor 1
  |
  | eventually
  ↓
Old Generation
```

This is a conceptual model rather than a universal implementation detail for every collector.

---

# 19. Old Generation

Objects that remain reachable for a relatively long time may eventually be considered old.

Examples:

```text
long-lived cache
application configuration
large service objects
```

These can occupy the old generation.

Old-generation collection can be more expensive depending on the collector and workload.

---

# 20. Stop-the-world

A **stop-the-world (STW)** pause means application threads are paused while the JVM performs some operation.

Garbage collection can involve STW phases.

Important:

> Not every GC operation necessarily means the entire application is paused for the entire collection.

Modern collectors are designed to reduce pause times and perform substantial work concurrently.

---

# 21. Common garbage collectors

You may hear:

```text
Serial GC
Parallel GC
G1 GC
ZGC
Shenandoah
```

For an interview, know the broad idea.

### Serial

Simple collector, often suitable for smaller workloads.

### Parallel

Uses multiple threads for GC work and focuses on throughput.

### G1

**Garbage-First Garbage Collector.**

Divides the heap into regions and aims to balance throughput with predictable pause targets.

### ZGC

Designed for very low pause times, including very large heaps.

### Shenandoah

Also focuses heavily on low pause times through concurrent collection work.

You don't need to memorize implementation internals unless the job specifically focuses on JVM performance.

---

# 22. `System.gc()`

You might see:

```java
System.gc();
```

Does this force garbage collection?

**No.**

It is a request/suggestion to the JVM.

The JVM is not required to perform GC because of this call.

Interview answer:

> `System.gc()` requests garbage collection; it does not guarantee that GC will occur.

---

# 23. `finalize()`

Older Java had:

```java
protected void finalize()
```

It was associated with cleanup before object reclamation.

It is **deprecated and should not be used**.

Why?

Because:

* execution isn't guaranteed at a useful time
* timing is unpredictable
* it can hurt performance
* it complicates GC
* it can create object resurrection problems

Use explicit resource management instead, especially:

```java
try (Resource resource = ...) {
    ...
}
```

---

# 24. Memory leak in Java

A common misconception is:

> "Java has garbage collection, so Java cannot have memory leaks."

False.

You can have a memory leak when objects are **still reachable but no longer useful**.

Example:

```java
static List<Object> cache = new ArrayList<>();

public void process() {
    cache.add(new Object());
}
```

If `cache` keeps growing indefinitely:

```text
GC Root
   |
   ▼
static cache
   |
   ├── Object
   ├── Object
   ├── Object
   ├── Object
   └── ...
```

Those objects remain reachable.

GC therefore cannot reclaim them.

Eventually:

```text
OutOfMemoryError
```

may occur.

---

# 25. `OutOfMemoryError` vs `StackOverflowError`

Very common interview question.

### OutOfMemoryError

Occurs when the JVM cannot allocate required memory.

For example:

```text
Heap exhausted
Metaspace exhausted
```

Possible:

```text
java.lang.OutOfMemoryError
```

### StackOverflowError

Usually occurs when a thread's stack is exhausted.

Classic example:

```java
public static void recurse() {
    recurse();
}
```

Eventually:

```text
StackOverflowError
```

because every recursive call creates another stack frame.

---

# 26. Example of stack overflow

```java
static void test() {
    test();
}
```

Execution:

```text
test()
  ↓
test()
  ↓
test()
  ↓
test()
  ↓
...
```

Stack:

```text
┌─────────────┐
│ test()      │
├─────────────┤
│ test()      │
├─────────────┤
│ test()      │
├─────────────┤
│ test()      │
├─────────────┤
│ ...         │
└─────────────┘
```

Eventually:

```text
StackOverflowError
```

---

# 27. Heap vs Stack interview question

Suppose:

```java
public void method() {

    int x = 10;

    Employee employee = new Employee();
}
```

Simplified model:

```text
Thread Stack
┌────────────────────────┐
│ x = 10                 │
│ employee ──────────────┼──────┐
└────────────────────────┘      │
                                ▼
                              Heap
                         ┌─────────────┐
                         │ Employee    │
                         └─────────────┘
```

When `method()` returns:

```text
stack frame disappears
```

The `employee` object **may become eligible for GC** if there are no other references to it.

---

# 28. Does an object always go to heap?

For interview purposes:

> Objects and arrays are generally allocated on the heap.

But modern JVMs use optimizations such as **escape analysis** and may eliminate allocations or scalar-replace objects in some cases.

So avoid saying:

> "Every object always physically lives on the heap."

The JVM is allowed to optimize the implementation as long as observable Java semantics are preserved.

---

# 29. Escape analysis

Suppose:

```java
public void calculate() {

    Employee e = new Employee();

    e.setAge(30);

    System.out.println(e.getAge());
}
```

If the JVM determines that `e` doesn't escape the method/thread in a relevant way, the JIT may optimize the object allocation.

Possible optimization techniques include:

```text
scalar replacement
allocation elimination
lock elimination
```

This is advanced JVM optimization.

For normal interviews, knowing that **JIT + escape analysis can optimize allocations** is enough.

---

# 30. Strong interview answer

If asked:

> **Explain JVM memory management.**

A good answer:

> The JVM has several runtime memory areas, including the heap, per-thread Java stacks, the program counter, method-area-related class metadata, and native method stacks. Objects and arrays are generally allocated on the heap, while each thread has its own stack containing method frames and local execution state. Garbage collection manages reclaiming heap memory for objects that are no longer reachable from GC roots. Modern JVMs use different garbage collectors and optimizations to balance throughput, memory usage, and pause times.

---

# 31. The important interview questions

Make sure you can answer these:

### Q1. Where are objects stored?

Generally:

> Heap.

### Q2. Where are local variables stored?

As part of the current thread's stack frame in the simplified JVM model.

### Q3. Is heap shared between threads?

Yes.

### Q4. Is stack shared between threads?

No. Each thread has its own Java stack.

### Q5. What replaced PermGen?

> Metaspace in Java 8.

### Q6. Does `System.gc()` guarantee GC?

> No.

### Q7. When is an object eligible for GC?

> When it is no longer reachable from GC roots.

### Q8. Can circular references be garbage collected?

> Yes, if the entire cycle is unreachable from GC roots.

### Q9. Can Java have memory leaks?

> Yes. Objects can remain reachable even though the application no longer needs them.

### Q10. `OutOfMemoryError` vs `StackOverflowError`?

```text
OutOfMemoryError
→ JVM cannot allocate required memory

StackOverflowError
→ thread stack exhausted, commonly due to deep/infinite recursion
```

---

## Final mental model

```text
                         JVM
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
      Heap             Thread Stack     Metaspace
        │                 │                 │
    Objects           Stack Frames      Class metadata
    Arrays                 │
        │              Local state
        │
        ▼
 Garbage Collector
        │
        ▼
Unreachable objects
        │
        ▼
 Reclaimed memory
```

And remember this distinction:

> **Stack is primarily about method execution and per-thread state. Heap is primarily about dynamically allocated objects and shared memory. GC reclaims unreachable heap objects; it doesn't simply "clean the stack."**

**Next topic: Multithreading in Java** — processes vs threads, thread lifecycle, `Thread` vs `Runnable` vs `Callable`, `start()` vs `run()`, race conditions, thread safety, and the Java Memory Model.

