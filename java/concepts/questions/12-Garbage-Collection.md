# Java Garbage Collection — Detailed Interview Explanation

Garbage Collection (GC) is the JVM's automatic mechanism for **finding objects that are no longer reachable by the application and reclaiming the memory occupied by those objects**.

A good interview definition is:

> **Garbage Collection is the JVM process of automatically identifying unreachable objects in heap memory and reclaiming their memory so that the memory can be reused by future object allocations.**

The important word is **reachable**.

---

# 1. Why do we need Garbage Collection?

Consider:

```java
public void process() {
    User user = new User("Riyaz");

    // use user
}
```

When this executes:

```text
Stack                         Heap

process() frame
    user ───────────────────> User object
```

After `process()` finishes:

```text
Stack                         Heap

                              User object
                              ↑
                              No reference
```

The `User` object is now **unreachable**.

Without automatic memory management, the programmer would have to explicitly free it:

```text
allocate memory
       ↓
use object
       ↓
free memory
```

Java instead does:

```text
allocate memory
       ↓
use object
       ↓
object becomes unreachable
       ↓
GC eventually identifies it
       ↓
memory is reclaimed
       ↓
memory can be reused
```

---

# 2. Does GC delete variables?

No.

This is an important distinction.

Suppose:

```java
User user = new User();
user = null;
```

There are two different things:

```text
Variable/reference:
user

Object:
new User()
```

After:

```java
user = null;
```

the reference is removed:

```text
Stack              Heap

user = null        User object
                   ↑
                   no reference
```

The object becomes **eligible for garbage collection**.

GC eventually reclaims the object's memory.

---

# 3. What exactly does GC collect?

GC primarily deals with **objects in the Java heap**.

For example:

```java
User user = new User();
```

The object:

```java
new User()
```

is allocated in the heap.

But the local reference:

```java
user
```

is associated with the current stack frame.

So conceptually:

```text
             JVM
              |
      +-------+-------+
      |               |
    Stack            Heap
      |               |
   reference -------> Object
```

GC primarily determines which **heap objects** are still reachable.

---

# 4. The core concept: Reachability

This is the most important concept for understanding GC.

An object is considered **live/reachable** if it can be reached from a set of special references called **GC Roots**.

For example:

```java
public void test() {
    User user = new User();
}
```

While `test()` is running:

```text
GC Root
  |
  ↓
local variable
  |
  ↓
User object
```

Therefore:

```text
User object = reachable
```

After `test()` returns:

```text
GC Root

      X

User object
```

No path exists.

Therefore:

```text
User object = unreachable
```

It becomes eligible for collection.

---

# 5. What are GC Roots?

GC Roots are objects/references that the JVM considers starting points when determining reachability.

Common examples include:

### 1. Local variables

```java
public void method() {
    User user = new User();
}
```

`user` can act as a root while its stack frame is active.

### 2. Active threads

Objects referenced by currently running threads can be reachable.

### 3. Static references

For example:

```java
class Cache {
    static User user = new User();
}
```

Conceptually:

```text
GC Root
   |
   ↓
static field
   |
   ↓
User object
```

As long as that static reference remains reachable, the object can remain alive.

### 4. JNI references

Native code can hold references to Java objects.

---

# 6. Very important: Circular references do NOT cause a memory leak by themselves

Consider:

```java
class A {
    B b;
}

class B {
    A a;
}
```

And:

```java
A a = new A();
B b = new B();

a.b = b;
b.a = a;

a = null;
b = null;
```

Now:

```text
        A
       ↗ ↘
      ↙   ↘
     B ←───
```

There is a cycle:

```text
A → B
↑   ↓
└───┘
```

But neither object is reachable from a GC Root.

Therefore:

```text
GC Root
   |
   X

A ←→ B
```

Both can be collected.

This is one reason Java's reachability-based GC is better than simple reference counting.

---

# 7. How does Garbage Collection actually work?

At a high level, GC goes through concepts such as:

```text
Identify reachable objects
          ↓
Identify unreachable objects
          ↓
Reclaim memory
          ↓
Possibly compact memory
```

Different collectors implement this differently, but the fundamental idea is reachability.

Historically, this is commonly explained using:

```text
Mark → Sweep → Compact
```

and generational collection.

---

# 8. Mark and Sweep

## Mark

The JVM starts from GC Roots and traverses the object graph.

Example:

```text
GC Root
   |
   A
  / \
 B   C
 |
 D

X   Y
```

Reachable:

```text
A
B
C
D
```

Unreachable:

```text
X
Y
```

The JVM marks reachable objects.

```text
A ✓
B ✓
C ✓
D ✓

X ✗
Y ✗
```

---

# 9. Sweep

The JVM then identifies memory occupied by unreachable objects and makes that memory available for reuse.

```text
Before:

[A][X][B][Y][C][Z][D]

After sweep:

[A][free][B][free][C][free][D]
```

But now we have a problem.

---

# 10. Fragmentation

Imagine:

```text
Heap:

[Object A][free][Object B][free][Object C][free]
```

There might be enough total free memory:

```text
free + free + free
```

but it is fragmented into small pieces.

A large object might need:

```text
[        Large Object        ]
```

and there might not be a sufficiently large contiguous region.

This is called **memory fragmentation**.

---

# 11. Compaction

A collector can move live objects together:

```text
Before:

[A][free][B][free][C][free][D]

After:

[A][B][C][D][free][free][free]
```

Now free memory is contiguous.

This is called **compaction**.

But moving objects introduces another problem:

> What happens to references pointing to those objects?

The JVM must update references accordingly.

---

# 12. Why does Java use Generational Garbage Collection?

This is one of the most important GC interview concepts.

The JVM uses the observation that:

> **Most objects die young.**

For example:

```java
public String processRequest(Request request) {
    String temp = request.getData();
    ...
}
```

Objects created while processing a request may become useless shortly afterward.

So instead of treating the entire heap equally, JVMs traditionally divide heap into generations.

Conceptually:

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

# 13. Eden Space

Most newly created objects initially go into **Eden**.

Example:

```java
User user = new User();
Order order = new Order();
Payment payment = new Payment();
```

Conceptually:

```text
Young Generation

Eden:
[User][Order][Payment][...]
```

As more objects are created, Eden fills.

Eventually a collection occurs.

---

# 14. Minor / Young GC

A collection focused primarily on the young generation is traditionally called a:

> **Minor GC** or **Young GC**

Suppose:

```text
Eden:

[A][B][C][D][E]
```

But only:

```text
A
D
```

are still reachable.

After collection:

```text
Survivor:

[A][D]
```

The dead objects:

```text
B
C
E
```

can be reclaimed.

This is relatively efficient because most young objects are expected to be dead.

---

# 15. Survivor Spaces

Young generation traditionally contains:

```text
Eden
Survivor 0
Survivor 1
```

For example:

```text
          Young Generation

       +----------------+
       |     Eden       |
       +----------------+
       |  Survivor 0    |
       +----------------+
       |  Survivor 1    |
       +----------------+
```

Objects initially go into Eden.

After a young collection, surviving objects are copied to a survivor space.

Conceptually:

```text
Eden
[A][B][C][D]

After GC:

Survivor 0
[A][C]
```

Objects that continue surviving multiple collections can eventually be promoted to the old generation.

---

# 16. Object Aging

Imagine:

```text
Object A
```

survives a young GC.

It may be assigned an age.

Conceptually:

```text
GC #1 → age 1
GC #2 → age 2
GC #3 → age 3
...
```

After surviving enough collections, the JVM may promote it:

```text
Young Generation
       |
       | survives repeatedly
       ↓
Old Generation
```

The exact promotion behavior depends on the collector and JVM implementation.

---

# 17. Old Generation

The old generation generally contains objects that have survived for a relatively long time.

For example:

```java
static final UserConfiguration CONFIG =
        new UserConfiguration();
```

This object may remain alive for most or all of the application's lifetime.

Conceptually:

```text
Young
  |
  | surviving objects
  ↓
Old
```

Collections involving old-generation objects are generally more expensive than young-generation collections because there may be many more live objects to process.

---

# 18. Major GC vs Full GC

This terminology needs caution in interviews.

You will often hear:

### Minor GC

Primarily young-generation collection.

### Major GC

Traditionally associated with old-generation collection.

### Full GC

Collection involving much/all of the heap, depending on the collector.

However:

> **These terms are not universally defined across all modern JVM garbage collectors.**

Modern collectors such as G1, ZGC and Shenandoah use different terminology and algorithms.

So in an interview, avoid saying:

> "Major GC always means exactly X."

Better:

> "Historically, Minor GC refers to young-generation collection, while Major GC generally refers to old-generation collection. Full GC generally means a collection involving the entire heap, but the exact terminology and behavior depend on the collector."

That's a stronger answer.

---

# 19. Stop-The-World (STW)

Another extremely important GC concept.

A GC operation may require application threads to pause.

This is called:

> **Stop-The-World (STW)**

Conceptually:

```text
Application threads
─────────────────────────────
RUN RUN RUN RUN | PAUSE | RUN RUN
                ↑
             GC work
```

During the pause, application threads aren't executing Java application code.

Why?

Because the JVM may need a stable view of object references while performing certain GC operations.

---

# 20. Does every GC stop the entire application?

No.

This is a common interview trap.

Modern collectors perform substantial amounts of work **concurrently** with application execution.

For example:

```text
Application: RUN RUN RUN RUN RUN RUN
GC:              RUN RUN RUN
```

But some phases can still require STW pauses.

Therefore:

> **Modern garbage collectors aim to minimize STW pauses, not necessarily eliminate them completely.**

---

# 21. What is a GC Pause?

Suppose your application normally responds in:

```text
20 ms
```

Then GC pauses application threads for:

```text
500 ms
```

A request arriving during that pause may experience significantly higher latency.

For latency-sensitive applications:

```text
GC pause
   ↓
higher latency
   ↓
poor response time
```

Therefore, GC isn't just about memory.

It also affects:

* latency
* throughput
* CPU utilization
* application responsiveness

---

# 22. Throughput vs Latency

Garbage collectors generally involve trade-offs.

### Throughput-oriented

Goal:

> Spend as much time as possible executing application code.

### Latency-oriented

Goal:

> Keep GC pauses short and predictable.

For example:

```text
Collector optimization

Throughput
   ↑
Application gets more CPU time

Latency
   ↑
Shorter GC pauses
```

Modern collectors attempt to provide good balances between these goals.

---

# 23. What happens when the heap becomes full?

Suppose:

```text
Heap:

[Live][Live][Dead][Live][Dead][Live]
```

The JVM tries to reclaim memory.

If enough memory is recovered:

```text
allocation continues
```

If insufficient memory can be recovered and the JVM cannot satisfy an allocation request, eventually you can get:

```text
java.lang.OutOfMemoryError: Java heap space
```

Important:

> **OutOfMemoryError does not simply mean "GC is broken."**

It means the JVM could not satisfy the allocation request with the available/recoverable heap memory.

---

# 24. Can an object be garbage collected immediately after becoming unreachable?

No guarantee.

Example:

```java
User user = new User();

user = null;
```

The object is now eligible for GC.

But:

```text
eligible for GC
       ≠
immediately collected
```

The JVM decides when/how collection occurs.

---

# 25. What does `System.gc()` do?

You may see:

```java
System.gc();
```

Many beginners think:

> "This forces garbage collection."

That's incorrect.

It is a **request/hint** to the JVM that garbage collection would be useful.

The JVM is not required to perform GC simply because `System.gc()` was called.

In production code, you generally should not rely on it.

---

# 26. Can `finalize()` control garbage collection?

Historically Java had:

```java
protected void finalize() throws Throwable {
}
```

The idea was that the JVM could invoke `finalize()` before reclaiming an object.

But finalization has serious problems:

* unpredictable timing
* performance overhead
* object resurrection possibilities
* difficult reasoning
* poor resource-management semantics

Modern Java has deprecated finalization and it should not be used for resource management.

Instead use:

```java
try-with-resources
```

for resources such as files, sockets and database connections.

Example:

```java
try (FileInputStream input = new FileInputStream("data.txt")) {
    // use resource
}
```

GC manages **memory**, while explicit resource management should handle things such as files and sockets.

---

# 27. GC does NOT mean all resources are automatically released

Very important.

Consider:

```java
Connection connection = getConnection();
```

Making:

```java
connection = null;
```

doesn't mean:

> "The database connection has been properly closed."

Memory and external resources are different.

Use:

```java
try (Connection connection = getConnection()) {
    // use connection
}
```

The `close()` operation should happen deterministically.

---

# 28. How does the JVM know whether an object is reachable?

Conceptually, it performs graph traversal.

Imagine:

```text
              GC Root
                 |
                 A
              /     \
             B       C
             |       |
             D       E

             X ←→ Y
```

Starting from GC Roots:

```text
Root → A → B → D
          \
           C → E
```

So:

```text
A B C D E = reachable
X Y       = unreachable
```

The collector can reclaim:

```text
X
Y
```

---

# 29. What about objects referenced by other objects?

Suppose:

```java
class Company {
    Employee employee;
}
```

and:

```java
Company company = new Company();
company.employee = new Employee();
```

Graph:

```text
GC Root
   |
   ↓
company
   |
   ↓
Company
   |
   ↓
Employee
```

Even though there isn't a direct variable pointing to `Employee`, it is reachable through `Company`.

Therefore it remains alive.

---

# 30. Memory Leak in Java

A common misconception:

> "Java has GC, therefore Java cannot have memory leaks."

False.

Java can absolutely have memory leaks.

A Java memory leak usually means:

> Objects are no longer logically needed by the application, but they remain reachable, preventing GC from reclaiming them.

Example:

```java
static List<User> users = new ArrayList<>();
```

Then:

```java
while (true) {
    users.add(new User());
}
```

The list is reachable through the static field.

Therefore:

```text
GC Root
   |
static users
   |
List
   |
User
User
User
User
...
```

Even if your application no longer needs some of those users, GC sees them as reachable.

Therefore:

```text
reachable ≠ useful
```

This is the key distinction.

---

# 31. Common causes of Java memory leaks

### Static collections

```java
static List<Object> cache = new ArrayList<>();
```

without eviction.

### Unbounded caches

```text
cache
 ├── object
 ├── object
 ├── object
 ├── object
 └── ...
```

### Listeners not removed

```java
eventSource.addListener(listener);
```

If the listener is never removed, it may remain reachable.

### ThreadLocal misuse

```java
threadLocal.set(largeObject);
```

If the thread lives for a long time, the object can remain reachable.

### Long-lived collections

```java
Map<Key, Value> map
```

where entries are never removed.

---

# 32. Strong, Weak and Soft References

Java also provides different reference strengths.

The most common is:

```java
User user = new User();
```

This is a **strong reference**.

As long as:

```text
GC Root → user → Object
```

exists, the object normally isn't eligible for collection.

Java also has:

```text
StrongReference
WeakReference
SoftReference
PhantomReference
```

For example:

```java
WeakReference<User> reference =
        new WeakReference<>(new User());
```

The object can become eligible for GC when no strong references remain.

Weak references are useful for certain caches and metadata structures.

---

# 33. How do modern garbage collectors differ?

Java provides multiple garbage collectors.

Some important ones are:

```text
Serial GC
Parallel GC
G1 GC
ZGC
Shenandoah
```

Their goals and implementations differ.

---

## Serial GC

Uses a relatively simple approach and performs GC work using a single GC thread.

Useful for:

* small heaps
* simple applications
* environments where simplicity matters

---

## Parallel GC

Uses multiple GC threads.

Main goal:

> High application throughput.

Conceptually:

```text
GC thread 1 ─┐
GC thread 2 ─┼──→ GC
GC thread 3 ─┤
GC thread 4 ─┘
```

---

# 34. G1 Garbage Collector

G1 means:

> **Garbage-First Garbage Collector**

Instead of relying on one contiguous young/old layout, G1 divides the heap into many regions.

Conceptually:

```text
+----+----+----+----+
| R1 | R2 | R3 | R4 |
+----+----+----+----+
| R5 | R6 | R7 | R8 |
+----+----+----+----+
```

Different regions can play different roles.

G1 tries to identify regions with lots of reclaimable garbage and prioritize them.

Hence:

> Garbage First.

G1 is designed to provide a balance between throughput and predictable pause times.

---

# 35. ZGC

ZGC is designed for:

> **Very low pause times, even with very large heaps.**

A major characteristic is that much of its work happens concurrently with application threads.

Conceptually:

```text
Application: RUN RUN RUN RUN RUN RUN RUN
GC:             RUN RUN RUN RUN RUN
```

It aims to keep pauses very small.

---

# 36. Shenandoah

Shenandoah also focuses heavily on:

> Low pause times through concurrent garbage collection and compaction.

Like ZGC, much of the work is performed concurrently with application execution.

---

# 37. Is GC one single algorithm?

No.

This is an important interview point.

"Garbage Collection" is a **memory-management technique**, not one specific algorithm.

Different collectors can use combinations of techniques such as:

```text
Mark
Sweep
Copy
Compact
Generational collection
Concurrent collection
Parallel collection
Region-based collection
```

The exact implementation depends on the collector.

---

# 38. Copying Collection

Another important technique is copying.

Suppose:

```text
From-space:

[A][dead][B][dead][C]
```

The collector copies live objects:

```text
To-space:

[A][B][C]
```

The old region can then be reused.

This is particularly useful for young-generation collection because many young objects die quickly.

---

# 39. Why is Young GC usually efficient?

Suppose Eden contains:

```text
1 GB objects
```

but only:

```text
50 MB
```

survive.

The collector only needs to preserve the live objects.

```text
Before:

1 GB
████████████████████

After:

50 MB
█
```

This is why the generational hypothesis is so useful:

> Most newly created objects become garbage quickly.

---

# 40. Write Barriers and Remembered Sets

This is a more advanced interview topic.

Suppose an old-generation object references a young-generation object:

```text
Old Object
    |
    ↓
Young Object
```

When performing a young GC, the JVM needs to know about references from outside the young generation.

Otherwise it would have to scan the entire old generation every time.

Collectors use mechanisms such as:

```text
write barriers
card tables
remembered sets
```

to track relevant cross-region references.

Conceptually:

```text
Old Generation
     |
     | tracked reference
     ↓
Young Generation
```

This makes young collections much more efficient.

---

# 41. What happens during allocation?

Suppose:

```java
User user = new User();
```

Conceptually:

```text
1. JVM needs memory
        ↓
2. Allocate object in heap
        ↓
3. Initialize object
        ↓
4. Return reference
```

If there isn't enough suitable memory:

```text
allocation fails
      ↓
GC may be triggered
      ↓
memory reclaimed
      ↓
allocation retried
```

If the JVM still cannot satisfy the allocation:

```text
OutOfMemoryError
```

---

# 42. TLAB

A more advanced JVM concept is:

> **Thread-Local Allocation Buffer**

Instead of every object allocation requiring synchronization over a shared heap structure, a thread can receive a small allocation buffer.

Conceptually:

```text
Heap
│
├── Thread 1 → TLAB
├── Thread 2 → TLAB
├── Thread 3 → TLAB
└── ...
```

Then:

```java
new User()
```

can often be allocated quickly inside the current thread's TLAB.

This improves allocation performance in multithreaded applications.

---

# 43. Does GC collect stack memory?

Normally, when people say:

> "GC collects memory"

they mean heap object memory.

Stack frames have their own lifecycle.

Example:

```java
void method() {
    User user = new User();
}
```

When the method returns:

```text
stack frame
    ↓
removed
```

The local reference disappears with the frame.

That can make the corresponding heap object unreachable.

So:

```text
Stack lifecycle
        ↓
reference disappears
        ↓
Heap object becomes unreachable
        ↓
GC can eventually reclaim it
```

---

# 44. Metaspace and GC

Since you recently asked about JVM memory areas, this is worth connecting.

Class metadata is stored in **Metaspace**, which is native memory rather than the Java heap.

When classes/class loaders become unreachable, class metadata can also eventually be unloaded and its associated metaspace memory reclaimed.

Conceptually:

```text
JVM Memory

Heap
 └── Java objects
      ↑
      GC heavily operates here

Native Memory
 └── Metaspace
      └── class metadata
```

Class unloading is more complicated than ordinary object collection and depends on class-loader reachability and collector behavior.

---

# 45. A complete GC lifecycle example

Consider:

```java
public void process() {

    User user = new User();
    Order order = new Order();

    process(user, order);
}
```

Initially:

```text
Stack
 └── process()
       ├── user ──────→ User
       └── order ─────→ Order

Heap
 ├── User
 └── Order
```

Both are reachable.

Then:

```java
process(...)
```

returns.

The stack frame disappears:

```text
Stack

(no references)
```

Heap:

```text
User     ← unreachable
Order    ← unreachable
```

Eventually GC runs:

```text
GC Roots
   |
   X

User
Order
```

The objects are identified as unreachable.

Their memory can then be reclaimed.

---

# 46. Interview-level end-to-end answer

If an interviewer asks:

> **"How does Garbage Collection work in Java?"**

A strong answer would be:

> Java Garbage Collection is an automatic memory-management mechanism that identifies heap objects that are no longer reachable from GC Roots and reclaims their memory.
>
> The JVM determines reachability by traversing the object graph starting from GC Roots such as active thread references, local variables, static references, and JNI references.
>
> Java traditionally uses generational GC because most objects die young. Newly allocated objects generally start in the young generation, particularly Eden. Objects that survive young collections may move through survivor regions and eventually be promoted to the old generation.
>
> During collection, the JVM identifies live objects and reclaims memory occupied by dead objects. Depending on the collector, this can involve marking, copying, sweeping, and compacting objects. Some GC phases may stop application threads, called Stop-The-World pauses, while modern collectors such as G1, ZGC, and Shenandoah perform significant work concurrently to reduce pause times.
>
> Different collectors optimize for different goals such as throughput, memory efficiency, and latency. If the JVM cannot reclaim enough memory to satisfy an allocation, it can eventually throw `OutOfMemoryError`.
>
> Also, GC does not guarantee immediate collection after an object becomes unreachable, and `System.gc()` is only a request rather than a guarantee.

---

# 47. The mental model to remember

You can remember Java GC using this flow:

```text
              Object created
                    │
                    ↓
              Young Generation
                    │
                    ↓
             Young GC occurs
                    │
          ┌─────────┴─────────┐
          │                   │
       Dead object         Survives
          │                   │
          ↓                   ↓
       Reclaim            Survivor
                              │
                              ↓
                       More collections
                              │
                              ↓
                       Old Generation
                              │
                              ↓
                        Old GC / G1 etc.
                              │
                              ↓
                       Reclaim / Compact
```

And the most important principle is:

```text
                 GC Roots
                    │
                    ↓
              Reachable graph
                    │
             ┌──────┴──────┐
             │             │
          reachable     unreachable
             │             │
             ↓             ↓
          KEEP          COLLECT
```

### The 10 things I'd remember for an interview

1. **GC manages heap object memory automatically.**
2. **GC is based primarily on reachability, not reference counting.**
3. **GC Roots are the starting points for reachability analysis.**
4. **Unreachable objects become eligible for collection.**
5. **Eligible does not mean immediately collected.**
6. **Generational GC exploits the fact that most objects die young.**
7. **Young → Survivor → potentially Old Generation.**
8. **GC can involve STW pauses, although modern collectors do much work concurrently.**
9. **GC does not prevent memory leaks—reachable-but-unused objects can still leak memory.**
10. **Different collectors (G1, ZGC, Shenandoah, etc.) make different throughput/latency trade-offs.**

