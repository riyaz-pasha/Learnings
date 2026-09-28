## 19. Synchronization, `volatile`, Atomic Classes & Race Conditions

This is the natural continuation from multithreading, and it is **very commonly asked in Java interviews**.

We'll build the topic from the basic problem → `synchronized` → locks → `volatile` → atomic classes → happens-before.

---

# 1. Why do we need synchronization?

Suppose multiple threads share this variable:

```java
class Counter {

    private int count = 0;

    public void increment() {
        count++;
    }

    public int getCount() {
        return count;
    }
}
```

And two threads execute:

```java
counter.increment();
```

You might expect:

```text
count = 2
```

But `count++` is **not one indivisible operation**.

Conceptually:

```text
1. Read count
2. Add 1
3. Write count
```

So two threads can do:

```text
Thread 1: read count = 0
Thread 2: read count = 0

Thread 1: calculate 1
Thread 2: calculate 1

Thread 1: write 1
Thread 2: write 1
```

Final result:

```text
1
```

instead of:

```text
2
```

This is a **race condition**.

---

# 2. Race Condition

A race condition occurs when:

> Multiple threads access shared mutable state concurrently, and the program's result depends on the timing/interleaving of those operations.

Classic example:

```java
count++;
```

### Important

A race condition isn't simply "multiple threads are running."

It generally requires some combination of:

* shared state
* mutable state
* concurrent access
* insufficient synchronization/coordination

---

# 3. Atomicity

An operation is **atomic** if it happens as one indivisible operation from the perspective of other threads.

For example:

```java
count++;
```

is **not atomic** for a normal `int` variable.

But:

```java
AtomicInteger count = new AtomicInteger();

count.incrementAndGet();
```

provides an atomic increment operation.

### Interview distinction

You should know these three terms very well:

| Concept    | Meaning                                                                      |
| ---------- | ---------------------------------------------------------------------------- |
| Atomicity  | Operation happens indivisibly                                                |
| Visibility | One thread sees another thread's changes                                     |
| Ordering   | Operations are observed in a permitted order according to memory-model rules |

These are related, but **not the same thing**.

---

# 4. `synchronized`

The simplest way to protect shared mutable state is:

```java
public synchronized void increment() {
    count++;
}
```

Now only one thread at a time can execute that synchronized method for the same object monitor.

Another way:

```java
public void increment() {
    synchronized (this) {
        count++;
    }
}
```

Both protect the critical section.

---

# 5. What does `synchronized` actually do?

`Synchronized` provides two major guarantees:

### 1. Mutual exclusion

Only one thread can hold a particular monitor lock at a time.

### 2. Visibility

Changes made by one thread before releasing the monitor become visible to another thread after it acquires the **same monitor**.

So:

```java
synchronized (lock) {
    // critical section
}
```

means:

```text
acquire lock
     ↓
execute critical section
     ↓
release lock
```

Another thread:

```text
acquire same lock
     ↓
can see appropriately published changes
```

---

# 6. What is a monitor?

Every Java object can be associated with an intrinsic monitor.

For:

```java
synchronized (obj) {
    ...
}
```

the thread needs to acquire the monitor associated with `obj`.

If another thread already owns it:

```text
Thread 1
   ↓
acquires obj monitor
   ↓
critical section
```

while:

```text
Thread 2
   ↓
tries to acquire obj monitor
   ↓
BLOCKED
```

Once Thread 1 releases the monitor, Thread 2 can compete to acquire it.

---

# 7. Synchronized instance method

```java
class Counter {

    private int count;

    public synchronized void increment() {
        count++;
    }
}
```

This is essentially locking on:

```java
this
```

So conceptually:

```java
public void increment() {
    synchronized (this) {
        count++;
    }
}
```

---

# 8. Synchronized static method

Consider:

```java
class Counter {

    public static synchronized void increment() {
        ...
    }
}
```

A static synchronized method locks on the **Class object**, conceptually:

```java
synchronized (Counter.class) {
    ...
}
```

This is an important interview distinction.

### Instance synchronized

Locks:

```java
this
```

### Static synchronized

Locks:

```java
ClassName.class
```

---

# 9. Synchronized block

You don't always want to lock an entire method.

Instead:

```java
public void process() {

    doSomethingWithoutLock();

    synchronized (lock) {
        updateSharedState();
    }

    doSomethingElse();
}
```

This is often better because the critical section is smaller.

### Why?

Smaller critical section generally means:

* less time holding the lock
* less contention
* potentially better concurrency

---

# 10. Why not synchronize everything?

Suppose:

```java
public synchronized void process() {

    doExpensiveOperation();

    updateSharedState();

    callExternalService();

    writeToFile();
}
```

You're holding the lock while doing potentially slow work.

Other threads that need the same lock have to wait.

A better design may be:

```java
public void process() {

    doExpensiveOperation();

    synchronized (lock) {
        updateSharedState();
    }

    callExternalService();
}
```

The exact design depends on what state needs protection.

---

# 11. Different locks mean different synchronization

This is important.

```java
synchronized (lock1) {
    ...
}
```

and:

```java
synchronized (lock2) {
    ...
}
```

do **not** block each other if:

```java
lock1 != lock2
```

For example:

```java
Object lock1 = new Object();
Object lock2 = new Object();
```

Two threads can simultaneously execute:

```java
synchronized (lock1)
```

and:

```java
synchronized (lock2)
```

because they're acquiring different monitors.

---

# 12. `volatile`

Now consider:

```java
class Worker {

    private boolean running = true;

    public void stop() {
        running = false;
    }

    public void work() {
        while (running) {
            // work
        }
    }
}
```

One thread calls:

```java
stop();
```

Another thread continuously checks:

```java
while (running)
```

Without appropriate synchronization/visibility guarantees, the worker thread is not guaranteed to observe the update promptly.

We can declare:

```java
private volatile boolean running = true;
```

Now reads/writes of that variable have the visibility/order guarantees provided by `volatile`.

---

# 13. What does `volatile` guarantee?

Think:

> **volatile = visibility + ordering guarantees, but NOT general compound-operation atomicity**

Example:

```java
private volatile boolean running;
```

is a good use case.

But:

```java
private volatile int count;

count++;
```

is still unsafe.

Why?

Because:

```java
count++;
```

is:

```text
read
+
increment
+
write
```

Another thread can interleave between those operations.

So:

```java
volatile int count;
```

does **not** make:

```java
count++;
```

atomic.

---

# 14. `volatile` vs `synchronized`

Very common interview question.

|                            | `volatile` | `synchronized` |
| -------------------------- | ---------- | -------------- |
| Visibility                 | Yes        | Yes            |
| Mutual exclusion           | No         | Yes            |
| Makes `count++` atomic     | No         | Yes            |
| Lock acquired              | No         | Yes            |
| Suitable for simple flags  | Often      | Yes            |
| Protects critical sections | No         | Yes            |

Example suitable for `volatile`:

```java
private volatile boolean shutdown;
```

Example requiring synchronization:

```java
private int count;

public synchronized void increment() {
    count++;
}
```

---

# 15. Atomic Classes

Java provides atomic classes in:

```java
java.util.concurrent.atomic
```

Common ones:

```text
AtomicInteger
AtomicLong
AtomicBoolean
AtomicReference
```

Example:

```java
AtomicInteger count = new AtomicInteger(0);

count.incrementAndGet();
```

Now increment is atomic.

Other useful operations:

```java
count.get();

count.set(10);

count.incrementAndGet();

count.decrementAndGet();

count.getAndIncrement();

count.addAndGet(5);
```

---

# 16. AtomicInteger vs synchronized

With synchronization:

```java
class Counter {

    private int count;

    public synchronized void increment() {
        count++;
    }
}
```

With atomic:

```java
class Counter {

    private final AtomicInteger count = new AtomicInteger();

    public void increment() {
        count.incrementAndGet();
    }
}
```

Both can solve the counter problem.

Atomic classes are particularly useful for **simple atomic state transitions** without needing a traditional monitor lock.

---

# 17. Compare-and-Set (CAS)

Atomic classes commonly rely on a technique called:

> **CAS — Compare And Set**

Conceptually:

```text
Current value = 10

"Change 10 to 11,
but only if the current value is still 10."
```

If another thread changed it first, the operation fails and can retry.

For example:

```java
AtomicInteger count = new AtomicInteger(10);

boolean changed =
        count.compareAndSet(10, 11);
```

If the current value is `10`:

```text
10 → 11
```

and returns:

```java
true
```

Otherwise:

```java
false
```

---

# 18. Why is CAS useful?

CAS enables many concurrent algorithms without using traditional blocking locks.

Conceptually:

```text
Thread A                Thread B

read 10                 read 10

CAS 10 → 11             CAS 10 → 11
success                 failure
                         because value is now 11
```

Thread B can then retry based on the new value if its algorithm requires it.

---

# 19. `AtomicInteger` doesn't solve everything

Suppose you have:

```java
AtomicInteger balance;
AtomicInteger transactionCount;
```

and you need this entire operation to be one indivisible transaction:

```text
decrease balance
AND
increase transaction count
```

Two independent atomic variables don't automatically make the **combination** atomic.

For multi-variable invariants, you may need:

```java
synchronized
```

or:

```java
Lock
```

or a higher-level transactional/concurrency design.

---

# 20. `ReentrantLock`

Java also provides explicit locks:

```java
java.util.concurrent.locks.ReentrantLock
```

Example:

```java
class Counter {

    private int count;

    private final ReentrantLock lock = new ReentrantLock();

    public void increment() {

        lock.lock();

        try {
            count++;
        } finally {
            lock.unlock();
        }
    }
}
```

The `finally` is important.

You want:

```text
lock()
   ↓
work
   ↓
unlock()
```

even if an exception occurs.

---

# 21. `synchronized` vs `ReentrantLock`

` synchronized`:

```java
synchronized (lock) {
    ...
}
```

`ReentrantLock`:

```java
lock.lock();

try {
    ...
} finally {
    lock.unlock();
}
```

`ReentrantLock` provides additional capabilities such as:

```java
tryLock()
```

which allows you to attempt acquiring the lock without waiting indefinitely.

Example:

```java
if (lock.tryLock()) {
    try {
        // work
    } finally {
        lock.unlock();
    }
}
```

It also supports features such as:

* interruptible lock acquisition
* timed acquisition
* multiple `Condition` objects

For ordinary mutual exclusion, `synchronized` is often simpler.

---

# 22. Happens-Before

This is a **very important interview concept**.

The Java Memory Model defines relationships called **happens-before**.

If action A happens-before action B, then the effects of A are guaranteed to be visible to B according to the memory model.

Some important happens-before relationships include:

### Unlock → subsequent lock

If Thread 1:

```java
synchronized (lock) {
    value = 10;
}
```

and Thread 2 subsequently acquires the **same lock**:

```java
synchronized (lock) {
    System.out.println(value);
}
```

the synchronization establishes the relevant happens-before relationship.

---

### Volatile write → subsequent volatile read

If:

```java
volatile boolean ready;
```

Thread 1:

```java
ready = true;
```

and Thread 2 subsequently reads:

```java
if (ready) {
    ...
}
```

the volatile semantics establish visibility/order guarantees.

---

### Thread start

Actions before:

```java
thread.start();
```

happen-before actions in the started thread.

---

### Thread termination and join

Actions in a thread happen-before another thread successfully returns from:

```java
thread.join();
```

This is why `join()` isn't just about waiting for completion; it also has memory-visibility implications.

---

# 23. Three concepts you should separate

Interviewers sometimes deliberately mix these up.

### Atomicity

```text
Can another thread observe the operation halfway through?
```

### Visibility

```text
Will another thread see my updated value?
```

### Ordering

```text
What ordering guarantees exist between operations?
```

For example:

```java
volatile int x;
```

helps with visibility and ordering, but:

```java
x++;
```

is still not a general atomic read-modify-write operation.

---

# 24. Classic interview example

Consider:

```java
class Counter {

    private int count = 0;

    public void increment() {
        count++;
    }

    public int getCount() {
        return count;
    }
}
```

### Is it thread-safe?

**No.**

Why?

Because:

```java
count++;
```

is a compound operation.

### Fix 1 — synchronized

```java
public synchronized void increment() {
    count++;
}
```

### Fix 2 — AtomicInteger

```java
private final AtomicInteger count = new AtomicInteger();

public void increment() {
    count.incrementAndGet();
}
```

### Fix 3 — explicit lock

```java
lock.lock();

try {
    count++;
} finally {
    lock.unlock();
}
```

---

# 25. Another classic example: `volatile`

```java
class Worker {

    private volatile boolean running = true;

    public void run() {
        while (running) {
            // work
        }
    }

    public void stop() {
        running = false;
    }
}
```

Here `volatile` is appropriate because we're essentially communicating:

```text
Thread A:
"Stop!"

       ↓

Thread B:
"Sees stop flag"
```

There isn't a compound operation like:

```java
running++;
```

that needs mutual exclusion.

---

# 26. Deadlock

Synchronization introduces another important problem.

Suppose:

```java
Thread 1:
lock A
then lock B

Thread 2:
lock B
then lock A
```

Possible sequence:

```text
Thread 1 → locks A
Thread 2 → locks B

Thread 1 → waits for B
Thread 2 → waits for A
```

Neither can proceed.

That's a **deadlock**.

Conceptually:

```text
Thread 1
   ↓
holds A
   ↓
waiting for B

Thread 2
   ↓
holds B
   ↓
waiting for A
```

---

# 27. How to prevent deadlocks

A common strategy is to establish a **consistent lock ordering**.

Always acquire:

```text
A → B
```

instead of sometimes:

```text
A → B
```

and elsewhere:

```text
B → A
```

Other strategies include:

* avoid unnecessary nested locks
* keep lock scope small
* use timed `tryLock()` where appropriate
* design ownership carefully

---

# 28. Livelock

Livelock is different.

Threads aren't blocked; they're actively responding to each other but making no progress.

Example conceptually:

```text
Thread A moves aside for B
Thread B moves aside for A
Thread A moves aside again
Thread B moves aside again
...
```

They're active, but the operation never completes.

---

# 29. Starvation

Starvation means a thread doesn't get enough opportunity to execute because other threads continually consume the needed resource.

For example:

```text
Thread A repeatedly gets the lock
Thread B keeps waiting
```

Unlike deadlock, the system may still be making progress overall.

---

# 30. The interview mental model

Remember this:

```text
Race condition
      ↓
shared mutable state
      ↓
need coordination
      ↓
────────────────────────────
synchronized → mutual exclusion + visibility
volatile     → visibility/order, NOT compound atomicity
Atomic*      → atomic operations using concurrency primitives/CAS
Lock         → explicit locking + advanced features
────────────────────────────
```

### Most important interview questions

You should be able to answer these without hesitation:

**Q: Is `count++` atomic?**

> No. It is a read-modify-write operation.

**Q: Does volatile make `count++` thread-safe?**

> No. Volatile provides visibility and ordering guarantees, but doesn't make compound operations atomic.

**Q: Does synchronized provide visibility?**

> Yes, synchronization provides mutual exclusion and establishes the relevant memory-visibility guarantees.

**Q: `sleep()` vs `wait()`?**

> `sleep()` pauses the current thread and does not release monitors it owns. `wait()` releases the object's monitor and waits for notification/timeout.

**Q: `synchronized` vs `AtomicInteger`?**

> `synchronized` protects a critical section with mutual exclusion; `AtomicInteger` provides atomic operations on a single integer without requiring a synchronized block.

**Q: What is CAS?**

> Compare-And-Set atomically changes a value only if it still equals an expected value.

**Q: What is deadlock?**

> Two or more threads wait indefinitely for resources held by each other.

---

### Next topic

**ExecutorService, Thread Pools, Future & Callable** — this is where we'll move from manually creating threads to how Java applications actually manage concurrent work.

