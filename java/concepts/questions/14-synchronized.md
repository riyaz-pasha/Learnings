# Java `synchronized` — In Depth

The best way to understand `synchronized` is not as simply **"a lock"**, but as a combination of:

1. **Mutual exclusion** — only one thread executes a protected critical section at a time.
2. **Memory visibility** — changes made by one thread become visible to another thread through the synchronization relationship.
3. **Ordering / happens-before guarantees** — the JVM cannot freely reorder operations across synchronization boundaries in ways that violate the Java Memory Model (JMM).
4. **Lock ownership** — a monitor is owned by one thread at a time.
5. **Reentrancy** — the same thread can acquire the same monitor multiple times.

Then we'll compare this with `volatile`, because the two solve fundamentally different problems.

---

# 1. The Problem `synchronized` Solves

Suppose we have:

```java
class Counter {

    private int count = 0;

    void increment() {
        count++;
    }
}
```

It looks like one operation:

```text
count++
```

But conceptually it is:

```text
READ count
ADD 1
WRITE count
```

Suppose two threads execute it.

```text
Initial count = 0

Thread A                 Thread B
--------                 --------
read 0
                         read 0
add 1
                         add 1
write 1
                         write 1

Final count = 1
```

We expected:

```text
0 → 1 → 2
```

but got:

```text
0 → 1
```

This is a **race condition**.

The fundamental problem is that:

```java
count++;
```

is a **read-modify-write operation**, and the operation isn't atomic.

---

# 2. `synchronized` Provides Mutual Exclusion

We can write:

```java
class Counter {

    private int count = 0;

    synchronized void increment() {
        count++;
    }
}
```

Now:

```text
Thread A
   |
   | acquire monitor
   v
+----------------+
| count++        |
+----------------+
   |
   | release monitor
   v

Thread B
   |
   | acquire monitor
   v
+----------------+
| count++        |
+----------------+
   |
   | release monitor
```

Only one thread can own that monitor at a time.

Therefore:

```text
Thread A: READ → MODIFY → WRITE
                         |
                         v
                    unlock
                         |
                         v
Thread B:             READ → MODIFY → WRITE
```

No overlapping execution of the critical section.

---

# 3. What Exactly Is Being Locked?

This is one of the most important interview concepts.

Java synchronization is based on **monitors**.

Every Java object can be associated with a monitor.

For example:

```java
Object lock = new Object();
```

Conceptually:

```text
Object
  |
  +---- monitor
          |
          +---- owner
          +---- entry queue
          +---- wait set
```

The monitor isn't something you normally manipulate directly.

You interact with it through:

```java
synchronized
```

---

# 4. `synchronized` Method

Consider:

```java
class Counter {

    synchronized void increment() {
        count++;
    }
}
```

This is approximately equivalent to:

```java
void increment() {
    synchronized (this) {
        count++;
    }
}
```

So the monitor being acquired is:

```java
this
```

Therefore:

```java
Counter counter = new Counter();
```

and:

```java
counter.increment();
```

means conceptually:

```text
acquire counter's monitor

execute method

release counter's monitor
```

---

# 5. Static `synchronized`

Now consider:

```java
class Counter {

    static synchronized void increment() {
        count++;
    }
}
```

This does **not** lock:

```java
this
```

because there isn't necessarily an instance involved.

It locks the monitor associated with the `Class` object:

```java
Counter.class
```

Conceptually:

```java
static void increment() {
    synchronized (Counter.class) {
        count++;
    }
}
```

So:

```text
instance synchronized
        ↓
this

static synchronized
        ↓
Counter.class
```

This distinction is extremely important.

---

# 6. Synchronized Block

You can explicitly specify the lock:

```java
private final Object lock = new Object();

void increment() {

    synchronized (lock) {
        count++;
    }
}
```

Here:

```text
lock
 ↓
monitor
 ↓
critical section
```

The thread must acquire `lock`'s monitor before entering.

---

# 7. Why Use a Dedicated Lock Object?

Consider:

```java
synchronized (this) {
    ...
}
```

Other code can also synchronize on your object:

```java
synchronized (counter) {
    ...
}
```

Now unrelated code can interfere with your synchronization.

Instead:

```java
private final Object lock = new Object();
```

and:

```java
synchronized (lock) {
    ...
}
```

The lock is private.

External code cannot normally acquire it.

This is often a better encapsulation strategy.

---

# 8. How Does the JVM Actually Implement `synchronized`?

This is where things get interesting.

At the Java source level:

```java
synchronized (lock) {
    criticalSection();
}
```

The JVM represents this using monitor operations.

Conceptually the bytecode contains:

```text
monitorenter

    critical section

monitorexit
```

So:

```text
Java source
    ↓
synchronized(lock)
    ↓
bytecode
    ↓
monitorenter
    ↓
critical section
    ↓
monitorexit
```

For a synchronized method, synchronization is represented differently at the bytecode level: the method carries the JVM-level `ACC_SYNCHRONIZED` flag rather than necessarily containing explicit `monitorenter`/`monitorexit` instructions.

But semantically, the JVM still establishes monitor acquisition and release.

---

# 9. What Happens During `monitorenter`?

Suppose:

```java
synchronized (lock) {
    x++;
}
```

Thread A reaches:

```text
monitorenter(lock)
```

The JVM attempts to acquire `lock`'s monitor.

### Case 1 — monitor is free

```text
Thread A
   |
   v
monitorenter
   |
   v
monitor FREE
   |
   v
A becomes owner
```

Then A executes the critical section.

---

### Case 2 — another thread owns it

Suppose:

```text
Thread A
    |
    +---- owns lock

Thread B
    |
    +---- tries to acquire lock
```

B cannot enter the synchronized section.

Conceptually:

```text
             Monitor
                |
          owner = A
                |
       +--------+--------+
       |                 |
      A owns            B waits
```

Eventually A executes:

```text
monitorexit
```

Then another waiting thread can acquire the monitor.

---

# 10. Does the JVM Always Put the Thread to Sleep?

No.

This is an important misconception.

Modern JVMs use optimized synchronization mechanisms.

The JVM may initially use very cheap techniques such as:

* atomic CPU instructions
* spinning
* adaptive spinning
* lightweight locking mechanisms
* eventually blocking/parking threads when contention warrants it

The exact implementation is JVM/version/platform dependent.

Therefore don't say in an interview:

> "`synchronized` always puts the thread into a blocking queue."

That's too simplistic.

Better:

> `synchronized` uses JVM monitor mechanisms. If the monitor is unavailable, the contending thread cannot enter the critical section and may spin briefly or eventually block/park depending on the JVM's locking strategy and contention.

---

# 11. Why Can't Two Threads Own the Same Monitor?

Because monitor ownership is exclusive.

Conceptually the monitor maintains:

```text
owner = Thread A
```

Only the owner can release it.

Therefore:

```text
Thread A → acquire
Thread B → acquire
```

cannot both succeed simultaneously for the same monitor.

This is what gives us **mutual exclusion**.

---

# 12. The Really Important Part: Memory Visibility

Many people learn:

> "`synchronized` prevents two threads from entering simultaneously."

Correct, but incomplete.

It also provides **memory visibility guarantees**.

Consider:

```java
class Example {

    private int value = 0;

    synchronized void write() {
        value = 42;
    }

    synchronized void read() {
        System.out.println(value);
    }
}
```

Suppose:

```text
Thread A
    |
    | synchronized write()
    |
    value = 42
    |
    | unlock
    v

Thread B
    |
    | lock
    |
    read value
```

The synchronization relationship establishes that the write can become visible to the subsequent synchronized acquisition.

This is part of the Java Memory Model's **happens-before** rules.

---

# 13. The Happens-Before Rule

The key rule is:

> An unlock on a monitor happens-before every subsequent lock on that same monitor.

Conceptually:

```text
Thread A

value = 42
   |
   v
unlock(lock)
   |
   | happens-before
   v
lock(lock)
   |
   v
Thread B

read value
```

Therefore:

```text
write
  ↓
unlock
  ↓
happens-before
  ↓
lock
  ↓
read
```

This gives us both:

### Visibility

Thread B can observe Thread A's prior writes.

### Ordering

Operations cannot be reordered in a way that violates the happens-before relationship.

---

# 14. This Is Why `synchronized` Is More Than Mutual Exclusion

Think of it as:

```text
                 synchronized
                       |
             +---------+---------+
             |                   |
       Mutual exclusion      Memory semantics
             |                   |
        one owner             visibility
             |                   |
        critical section      ordering
```

This is one of the most important concepts to remember.

---

# 15. What Happens on Exit?

Consider:

```java
synchronized (lock) {
    x = 10;
    y = 20;
}
```

When the thread exits:

```text
x = 10
y = 20
   |
   v
monitorexit
```

The synchronization semantics ensure that writes performed before the unlock participate correctly in the happens-before relationship with a later acquisition of that same monitor.

So another thread entering the corresponding synchronized section can safely observe the prior synchronized writes.

---

# 16. What If an Exception Happens?

This is another major advantage.

Consider:

```java
synchronized (lock) {
    doSomething();
    throw new RuntimeException();
}
```

The monitor is still released when control leaves the synchronized region.

Conceptually:

```text
monitorenter

try {
    doSomething();
}
finally {
    monitorexit;
}
```

This is why you don't normally need to manually unlock a monitor.

---

# 17. `synchronized` Is Reentrant

Suppose:

```java
class Example {

    synchronized void methodA() {
        methodB();
    }

    synchronized void methodB() {
        System.out.println("hello");
    }
}
```

Now:

```java
methodA()
```

acquires:

```text
this
```

Then it calls:

```java
methodB()
```

`methodB()` also wants:

```text
this
```

Would it deadlock?

No.

Because the same thread already owns the monitor.

Java monitors are **reentrant**.

Conceptually:

```text
Thread A
    |
    | acquire this
    | hold count = 1
    |
    +---- methodA()
              |
              v
          methodB()
              |
              | acquire this again
              |
              v
          hold count = 2
```

Then:

```text
return from methodB
    ↓
hold count = 1

return from methodA
    ↓
hold count = 0
    ↓
monitor released
```

The JVM tracks recursive monitor ownership.

---

# 18. Why Reentrancy Matters

Without reentrancy, this would deadlock:

```java
synchronized void outer() {
    inner();
}

synchronized void inner() {
}
```

The thread would wait for itself.

Reentrancy prevents that.

Therefore:

> A Java monitor can be acquired repeatedly by its owning thread, and the monitor is released only after the corresponding number of releases.

---

# 19. What Is the Difference Between `synchronized(this)` and `synchronized(lock)`?

Example:

```java
synchronized (this) {
    ...
}
```

locks:

```text
this
```

while:

```java
synchronized (lock) {
    ...
}
```

locks:

```text
lock
```

The important thing is:

> Threads synchronize with each other only if they synchronize on the **same monitor object**.

For example:

```java
Object lock1 = new Object();
Object lock2 = new Object();
```

Then:

```java
synchronized (lock1)
```

and:

```java
synchronized (lock2)
```

do not coordinate with each other.

---

# 20. A Common Bug

Consider:

```java
class Counter {

    private int count;

    void increment() {

        synchronized (new Object()) {
            count++;
        }
    }
}
```

This is useless synchronization.

Why?

Every call creates a different object.

```text
Thread A → new Object() → Lock A

Thread B → new Object() → Lock B
```

Therefore:

```text
Lock A ≠ Lock B
```

Both threads can enter.

Correct:

```java
private final Object lock = new Object();

void increment() {

    synchronized (lock) {
        count++;
    }
}
```

Now:

```text
Thread A ──┐
           ├── same lock
Thread B ──┘
```

---

# 21. Multiple Synchronized Methods

Consider:

```java
class Account {

    synchronized void deposit() {
        ...
    }

    synchronized void withdraw() {
        ...
    }
}
```

Both methods lock:

```java
this
```

Therefore:

```text
Thread A
deposit()
   |
   | locks this
   v

Thread B
withdraw()
   |
   | tries this
   v
   waits
```

They cannot execute simultaneously on the same object.

But this is important:

```java
Account a = new Account();
Account b = new Account();
```

Then:

```text
a.deposit() → locks a

b.withdraw() → locks b
```

These can execute concurrently.

Because:

```text
a != b
```

---

# 22. `synchronized` Does Not Mean "Only One Thread in the Entire JVM"

Another common misconception.

This:

```java
synchronized void method() {
}
```

doesn't mean:

> Only one thread in the entire application can execute this method.

It means:

> Only one thread at a time can execute code synchronized on the same monitor.

For instance:

```text
Object A → Thread 1
Object B → Thread 2
Object C → Thread 3
```

All can execute their synchronized methods concurrently because they're using different monitors.

---

# 23. What About `volatile`?

Now we can compare.

Suppose:

```java
private volatile boolean running = true;
```

`volatile` provides **visibility and ordering guarantees**, but not general mutual exclusion.

Example:

```java
while (running) {
    // work
}
```

If another thread does:

```java
running = false;
```

the worker thread can reliably observe the updated value.

But:

```java
volatile int count;

count++;
```

is still unsafe.

Why?

Because:

```text
count++
```

is:

```text
READ
+
MODIFY
+
WRITE
```

`volatile` does not make this whole sequence atomic.

---

# 24. `volatile` vs `synchronized`

The easiest mental model:

| Property                     | `volatile` | `synchronized` |
| ---------------------------- | ---------- | -------------- |
| Visibility                   | ✅          | ✅              |
| Ordering                     | ✅          | ✅              |
| Mutual exclusion             | ❌          | ✅              |
| Compound operation atomicity | ❌          | ✅              |
| Locking                      | ❌          | ✅              |
| Blocking/coordination        | ❌          | ✅              |
| Reentrant                    | N/A        | ✅              |
| Protects invariants          | Usually no | ✅              |
| Suitable for `count++`       | ❌          | ✅              |

---

# 25. Example: `volatile` Is Enough

```java
class Worker {

    private volatile boolean running = true;

    void run() {
        while (running) {
            doWork();
        }
    }

    void stop() {
        running = false;
    }
}
```

Why does this work?

We don't need:

```text
read → modify → write
```

We simply need:

```text
write false
```

and another thread needs to see it.

That's a good use case for `volatile`.

---

# 26. Example: `volatile` Is NOT Enough

```java
class Counter {

    private volatile int count;

    void increment() {
        count++;
    }
}
```

Still broken.

Suppose:

```text
count = 10
```

Two threads:

```text
Thread A             Thread B

read 10              read 10
+1                   +1
write 11             write 11
```

Final:

```text
11
```

Expected:

```text
12
```

`volatile` doesn't prevent this.

---

# 27. `synchronized` Fixes It

```java
class Counter {

    private int count;

    synchronized void increment() {
        count++;
    }
}
```

Now:

```text
Thread A
   |
 acquire
   |
 read 10
   |
 +1
   |
 write 11
   |
 release
   |
   v
Thread B
   |
 acquire
   |
 read 11
   |
 +1
   |
 write 12
   |
 release
```

No lost update.

---

# 28. The Deep Difference

This is the best interview explanation:

### `volatile`

Says approximately:

> "This variable participates in special memory-visibility and ordering rules. Reads/writes are not allowed to behave like ordinary non-volatile accesses."

It does **not** say:

> "Only one thread can modify this variable."

---

### `synchronized`

Says approximately:

> "Before executing this critical section, acquire this monitor. Only the owning thread can enter. On release, establish the required memory-ordering and visibility relationship."

So:

```text
volatile
   ↓
visibility + ordering

synchronized
   ↓
mutual exclusion
+
visibility
+
ordering
```

---

# 29. Atomicity Is a Subtle Concept

Be careful when saying:

> "`synchronized` makes operations atomic."

More precisely:

`synchronized` makes the **protected critical section mutually exclusive**.

For example:

```java
synchronized (lock) {
    balance -= amount;
    auditLog.add(...);
    updateStatistics();
}
```

The entire block behaves as a critical section relative to other code using the same monitor.

That's much stronger than simply making one variable access atomic.

---

# 30. `synchronized` Protects Invariants

This is one of the biggest practical differences.

Suppose:

```java
class BankAccount {

    private int balance;

    void transfer(BankAccount target, int amount) {
        ...
    }
}
```

An invariant might be:

```text
balance >= 0
```

Or a more complicated relationship:

```text
total balance of accounts remains constant
```

A synchronization mechanism can protect a sequence of operations that must be treated as one unit.

`volatile` generally cannot do this.

---

# 31. `synchronized` and CPU Memory

At the hardware level, modern CPUs have:

```text
CPU 1
 ↓
L1 cache
 ↓
L2 cache

CPU 2
 ↓
L1 cache
 ↓
L2 cache

       ↓
    main memory
```

Therefore threads don't simply behave as though they're directly reading and writing one magical shared memory cell.

There are:

* CPU caches
* store buffers
* compiler optimizations
* CPU instruction reordering
* JIT optimizations

The Java Memory Model abstracts all of this.

You should **not** think of `synchronized` simply as:

> "flush everything to RAM."

That's an oversimplification.

The JMM defines the observable guarantees, while the JVM and CPU are free to use much more sophisticated mechanisms underneath as long as those guarantees are respected.

---

# 32. JMM View of `synchronized`

The most important formal rule is:

```text
unlock(monitor)
        happens-before
subsequent lock(monitor)
```

Therefore:

```text
Thread A
----------------

writes
  ↓
unlock
  ↓
==================== happens-before ====================
  ↓
lock
  ↓
reads

Thread B
```

This is the foundation for the visibility guarantee.

---

# 33. Why Can't the Compiler/JIT Break This?

Suppose:

```java
synchronized (lock) {
    x = 10;
    y = 20;
}
```

The JVM can optimize aggressively.

But it cannot perform an optimization that changes behavior in a way observable under the Java Memory Model.

The synchronization operations create ordering constraints.

This is why the JMM is so important.

---

# 34. Lock Contention

Suppose 100 threads execute:

```java
synchronized (lock) {
    expensiveOperation();
}
```

You effectively have:

```text
             lock
              |
      +-------+-------+
      |               |
   Thread 1        Threads 2-100
      |               |
   running           waiting
```

Only one thread makes progress through the critical section at a time.

Therefore large synchronized regions can reduce concurrency.

Bad:

```java
synchronized (lock) {
    queryDatabase();
    callRemoteService();
    calculateSomething();
    writeFile();
}
```

Potentially better:

```java
Data data;

synchronized (lock) {
    data = updateSharedState();
}

callRemoteService(data);
```

Keep the critical section as small as correctness allows.

---

# 35. But Don't Optimize by Removing Necessary Synchronization

This is dangerous:

```java
synchronized (lock) {
    updateMultipleFields();
}
```

Changing it to:

```java
updateMultipleFields();
```

just because synchronization is "slow" can introduce races.

Correct order:

```text
1. Establish correctness
2. Measure contention
3. Optimize if necessary
```

Not:

```text
1. Remove synchronization
2. Hope it works
```

---

# 36. Lock Granularity

Suppose:

```java
class Service {

    synchronized void operationA() {}

    synchronized void operationB() {}

    synchronized void operationC() {}
}
```

All three use:

```text
this
```

Therefore:

```text
A ──┐
B ──┼── same lock
C ──┘
```

Even if A and B don't share state, they block each other.

You might instead use separate locks:

```java
private final Object lockA = new Object();
private final Object lockB = new Object();
```

Now:

```text
A → lockA

B → lockB
```

can potentially execute concurrently.

But more locks also introduce complexity and deadlock risks.

---

# 37. Deadlock

`synchronized` can participate in deadlocks.

Example:

```java
Thread 1:

synchronized (lockA) {
    synchronized (lockB) {
        ...
    }
}
```

Thread 2:

```java
synchronized (lockB) {
    synchronized (lockA) {
        ...
    }
}
```

Now:

```text
Thread 1                    Thread 2

owns A                      owns B
  |                           |
  | wants B                   | wants A
  ↓                           ↓
 WAIT                        WAIT
```

Neither can continue.

That's a deadlock.

---

# 38. How to Prevent Deadlock

One common strategy is consistent lock ordering.

Always:

```text
lockA → lockB
```

Never:

```text
lockB → lockA
```

For example:

```java
synchronized (lockA) {
    synchronized (lockB) {
        ...
    }
}
```

Every thread follows the same order.

---

# 39. `synchronized` and `wait()`

Monitors also support:

```java
wait()
notify()
notifyAll()
```

These are closely tied to synchronization.

For example:

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }

    doSomething();
}
```

Calling:

```java
wait()
```

does something very important:

```text
Thread owns monitor
       ↓
wait()
       ↓
releases monitor
       ↓
thread waits
```

Another thread can then acquire the monitor.

When awakened, the waiting thread must reacquire the monitor before continuing.

---

# 40. Why `wait()` Must Be Called While Synchronized

This is illegal:

```java
lock.wait();
```

unless the current thread owns `lock`'s monitor.

Otherwise:

```text
IllegalMonitorStateException
```

The same applies to:

```java
notify()
notifyAll()
```

They must be called while owning the object's monitor.

---

# 41. `wait()` vs `sleep()`

Very important interview distinction.

### `Thread.sleep()`

```java
Thread.sleep(1000);
```

does **not** release a monitor currently held by the thread.

### `Object.wait()`

```java
lock.wait();
```

releases the monitor associated with `lock` while waiting.

So:

```text
sleep()
    ↓
keeps locks

wait()
    ↓
releases that object's monitor
```

---

# 42. `synchronized` vs `ReentrantLock`

Java also provides:

```java
ReentrantLock
```

Both provide mutual exclusion and memory synchronization semantics.

But `ReentrantLock` gives additional capabilities such as:

```java
lock();
unlock();
tryLock();
lockInterruptibly();
```

and configurable fairness.

Example:

```java
Lock lock = new ReentrantLock();

lock.lock();

try {
    criticalSection();
} finally {
    lock.unlock();
}
```

For ordinary locking, `synchronized` is often simpler and safer because the JVM manages release automatically.

---

# 43. `synchronized` vs `AtomicInteger`

If your problem is simply:

```java
count++;
```

you could use:

```java
AtomicInteger count = new AtomicInteger();

count.incrementAndGet();
```

This uses atomic operations rather than a monitor.

Conceptually:

```text
synchronized
    ↓
mutual exclusion / monitor

AtomicInteger
    ↓
atomic CPU/JVM operations
```

For highly contended simple counters, atomics can be preferable.

For complex multi-variable invariants, synchronization or locking is often more appropriate.

---

# 44. `volatile` + `synchronized`

You can sometimes see both:

```java
private volatile boolean shutdown;
```

and:

```java
synchronized (lock) {
    ...
}
```

They aren't necessarily redundant.

For example, `volatile` may be appropriate for an independent state flag that doesn't need mutual exclusion, while synchronization protects a larger shared-state invariant.

But don't add `volatile` blindly to fields already safely accessed under a consistent lock.

---

# 45. Important Interview Trap

Question:

> If a field is accessed only inside synchronized methods, does it need to be volatile?

Usually **no**, assuming all relevant accesses are properly synchronized using the same monitor.

Example:

```java
private int count;

synchronized void increment() {
    count++;
}

synchronized int getCount() {
    return count;
}
```

The synchronization provides the necessary visibility.

---

# 46. Another Interview Trap

Question:

> Does `synchronized` make all fields in the object thread-safe?

No.

Suppose:

```java
synchronized void update() {
    value = 10;
}

void read() {
    System.out.println(value);
}
```

`update()` is synchronized, but `read()` isn't.

There is no corresponding synchronization relationship for `read()`.

So you cannot simply conclude:

> "Because the writer is synchronized, everything is thread-safe."

The accesses must participate in an appropriate synchronization protocol.

---

# 47. Another Important Trap

This:

```java
synchronized (new Object()) {
    ...
}
```

doesn't protect anything shared because each invocation can use a different monitor.

Similarly:

```java
synchronized ("hello") {
    ...
}
```

is generally a bad idea because interned strings can be shared by unrelated code.

Prefer:

```java
private final Object lock = new Object();
```

---

# 48. `synchronized` and Object Identity

This is fundamental:

```java
synchronized (object)
```

doesn't lock a variable.

It locks the **monitor associated with the object referenced by that expression**.

So:

```java
Object a = new Object();
Object b = a;
```

Then:

```java
synchronized (a)
```

and:

```java
synchronized (b)
```

use the same monitor.

Because:

```text
a ──┐
    ├──> same Object
b ──┘
```

---

# 49. Complete Mental Model

Think about:

```java
synchronized (lock) {
    criticalSection();
}
```

as:

```text
                lock object
                     |
                     v
                  monitor
                     |
          +----------+----------+
          |                     |
       owner                 waiting
          |
      Thread A
          |
          v
   critical section
          |
          v
       unlock
          |
          v
     next thread
```

And at the JMM level:

```text
Thread A

sharedState = newValue;
        |
        v
    unlock(lock)
        |
        | happens-before
        v
    lock(lock)
        |
        v
Thread B

observe sharedState
```

That is the core of Java synchronization.

---

# 50. Final `volatile` vs `synchronized` Mental Model

Remember this:

```text
                    THREAD SAFETY
                         |
             +-----------+-----------+
             |                       |
          volatile              synchronized
             |                       |
      visibility/order         visibility/order
                                     +
                              mutual exclusion
                                     +
                                  reentrant
                                     +
                             critical sections
```

Or even simpler:

```text
volatile:

Thread A ── write ────────────────┐
                                  ↓
                              visible to
                                  ↓
Thread B ── read ────────────────┘


synchronized:

Thread A ── lock ── critical section ── unlock
                                      |
                                      | happens-before
                                      ↓
Thread B ── lock ── critical section ── unlock
```

### The interview answer I'd give

> **`volatile` is primarily a visibility and ordering mechanism. It does not provide mutual exclusion and therefore cannot generally make compound operations such as `count++` thread-safe.**
>
> **`synchronized` provides mutual exclusion through JVM monitors and also establishes the Java Memory Model's happens-before relationship between monitor release and a subsequent acquisition of the same monitor. This gives us both atomicity of the protected critical section and visibility/ordering guarantees. Java monitors are reentrant, and the JVM may use different internal locking optimizations depending on the situation, so we should reason from the JMM guarantees rather than from a particular low-level implementation.**

### One-line rule to remember

> **`volatile` answers "will I see the latest value?" — `synchronized` answers "who is allowed to execute this critical section, and when do their changes become visible?"**


