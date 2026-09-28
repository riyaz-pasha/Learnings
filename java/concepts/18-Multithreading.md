## 18. Multithreading in Java

This is a **very important EPAM interview topic**. Once you understand this, the next topics—`synchronized`, `volatile`, atomic classes, deadlocks, `ExecutorService`, and `CompletableFuture`—become much easier.

---

# 1. What is a thread?

A **thread is an independent path of execution within a process**.

For example, a Java application might have:

```text
Java Process
│
├── Main Thread
├── Thread-1
├── Thread-2
└── Thread-3
```

All these threads belong to the same JVM process.

They generally share:

* heap
* static/class-level data
* other process resources

But each thread has its own:

* Java stack
* program counter
* execution state

---

# 2. Process vs Thread

### Process

A process is a running program with its own resources/address space.

Example:

```text
Browser process
Java application process
Database process
```

### Thread

A thread is an execution unit within a process.

```text
Process
│
├── Thread A
├── Thread B
└── Thread C
```

Threads within the same process can share memory, which makes communication easier—but also introduces concurrency problems.

---

# 3. Why use multiple threads?

Suppose we have:

```text
Task A → 5 seconds
Task B → 5 seconds
Task C → 5 seconds
```

Sequential execution:

```text
A ───── 5s
      B ───── 5s
            C ───── 5s

Total ≈ 15 seconds
```

If tasks are independent and can execute concurrently:

```text
A ───────── 5s
B ───────── 5s
C ───────── 5s

Total ≈ 5 seconds
```

This is simplified—actual performance depends on CPU cores, I/O, scheduling, synchronization, and workload.

---

# 4. Creating a thread

The basic Java API provides `Thread`.

```java
Thread thread = new Thread(() -> {
    System.out.println("Running in another thread");
});

thread.start();
```

This creates a thread and starts execution.

---

# 5. `start()` vs `run()`

This is a **classic interview question**.

Consider:

```java
Thread thread = new Thread(() -> {
    System.out.println(Thread.currentThread().getName());
});

thread.start();
```

`start()` tells the JVM to start a new thread of execution, which then invokes `run()`.

---

But:

```java
thread.run();
```

does **not** start a new thread.

It simply invokes the method on the current thread.

So:

```text id="7j7n2r"
thread.start()
     ↓
new thread execution
     ↓
run()
```

Whereas:

```text id="34e0fz"
thread.run()
     ↓
normal method call
     ↓
same thread
```

### Interview answer

> `start()` creates/schedules a new thread of execution, while calling `run()` directly is just a normal method invocation on the current thread.

---

# 6. Extending `Thread`

You can create a thread by extending `Thread`:

```java
class MyThread extends Thread {

    @Override
    public void run() {
        System.out.println("Running");
    }
}
```

Then:

```java
MyThread thread = new MyThread();
thread.start();
```

This works, but it's usually not the preferred design because Java supports single class inheritance.

---

# 7. Implementing `Runnable`

A more flexible approach:

```java
class MyTask implements Runnable {

    @Override
    public void run() {
        System.out.println("Running");
    }
}
```

Then:

```java
Thread thread = new Thread(new MyTask());
thread.start();
```

Or with a lambda:

```java
Thread thread = new Thread(() -> {
    System.out.println("Running");
});

thread.start();
```

### Why is `Runnable` generally preferred?

It separates:

```text
Task
```

from:

```text
Thread
```

The task describes **what to execute**.

The thread describes **where/how it executes**.

This becomes especially important when we introduce thread pools.

---

# 8. `Runnable` vs `Callable`

`Runnable`:

```java
Runnable task = () -> {
    System.out.println("Hello");
};
```

It:

* returns nothing
* cannot directly throw checked exceptions from `run()`

Signature:

```java
void run()
```

---

`Callable<T>`:

```java
Callable<Integer> task = () -> {
    return 10 + 20;
};
```

It:

* returns a value
* can throw checked exceptions

Conceptually:

```java
T call() throws Exception
```

This is why `Callable` is commonly used with `Future`.

---

# 9. Thread lifecycle

A Java thread can be in states represented by:

```java
Thread.State
```

The main states are:

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

---

# 10. NEW

After:

```java
Thread thread = new Thread(task);
```

but before:

```java
thread.start();
```

the thread is:

```text
NEW
```

It hasn't started execution.

---

# 11. RUNNABLE

After:

```java
thread.start();
```

the thread enters the `RUNNABLE` state.

Important interview nuance:

Java's `RUNNABLE` state includes a thread that is **ready to run and/or actually running**. Java doesn't expose a separate `RUNNING` state.

The OS/JVM scheduler determines when it actually gets CPU time.

---

# 12. BLOCKED

A thread becomes `BLOCKED` when it is waiting to acquire a monitor lock.

For example:

```java
synchronized (lock) {
    // critical section
}
```

Suppose Thread A owns `lock`.

Thread B tries to enter:

```java
synchronized (lock)
```

Thread B may enter:

```text
BLOCKED
```

until the monitor becomes available.

---

# 13. WAITING

A thread in `WAITING` waits indefinitely for another thread to perform some action.

Examples include:

```java
Object.wait();
Thread.join();
```

without a timeout.

For example:

```java
thread.join();
```

means the current thread waits for `thread` to finish.

---

# 14. TIMED_WAITING

The thread waits for a limited amount of time.

Examples:

```java
Thread.sleep(1000);
```

or:

```java
thread.join(1000);
```

or:

```java
object.wait(1000);
```

---

# 15. TERMINATED

After the `run()` method completes:

```text
RUNNABLE
   ↓
TERMINATED
```

A terminated thread cannot be restarted.

This is illegal:

```java
thread.start();
thread.start(); // ❌
```

It results in:

```text
IllegalThreadStateException
```

---

# 16. `sleep()`

Example:

```java
Thread.sleep(1000);
```

The current thread sleeps for approximately the specified duration.

Important:

> `sleep()` does **not release locks/monitors that the thread already owns.

For example:

```java
synchronized (lock) {
    Thread.sleep(1000);
}
```

The thread sleeps, but still holds `lock`.

---

# 17. `join()`

Suppose:

```java
Thread worker = new Thread(() -> {
    // work
});

worker.start();

worker.join();

System.out.println("Worker finished");
```

The current thread waits for `worker` to terminate.

Conceptually:

```text
Main Thread
    |
    | start worker
    ↓
Worker Thread ──────── work ───────► finished
    |
    |
Main waits
    |
    ↓
continue
```

---

# 18. `sleep()` vs `wait()`

Very common interview question.

### `Thread.sleep()`

```java
Thread.sleep(1000);
```

* belongs to `Thread`
* pauses current thread
* does not release held monitors
* can be called without owning a particular monitor

### `Object.wait()`

```java
synchronized (lock) {
    lock.wait();
}
```

* belongs to `Object`
* releases the monitor associated with that object
* must be called while owning that object's monitor
* waits for notification/interruption/timeout

Simple distinction:

```text
sleep()
→ pause
→ keeps locks

wait()
→ wait for coordination
→ releases object's monitor
```

---

# 19. `wait()`, `notify()`, `notifyAll()`

These are used for thread coordination.

Example:

```java
synchronized (lock) {
    lock.wait();
}
```

Another thread can notify:

```java
synchronized (lock) {
    lock.notify();
}
```

or:

```java
synchronized (lock) {
    lock.notifyAll();
}
```

### `notify()`

Wakes one waiting thread.

### `notifyAll()`

Wakes all threads waiting on that monitor.

The awakened threads still need to reacquire the monitor before continuing.

---

# 20. Why must `wait()` be called inside synchronized?

Because `wait()` operates on the object's monitor.

This is correct:

```java
synchronized (lock) {
    lock.wait();
}
```

This is incorrect:

```java
lock.wait(); // ❌
```

It results in:

```text
IllegalMonitorStateException
```

---

# 21. Race condition

This is one of the most important concurrency concepts.

Suppose:

```java
class Counter {

    private int count = 0;

    public void increment() {
        count++;
    }
}
```

It looks simple.

But:

```java
count++;
```

is not one indivisible operation.

Conceptually:

```text
read count
add 1
write count
```

Suppose two threads execute simultaneously.

Initial:

```text
count = 0
```

Thread A:

```text
read 0
```

Thread B:

```text
read 0
```

Thread A:

```text
write 1
```

Thread B:

```text
write 1
```

Final:

```text
count = 1
```

Expected:

```text
count = 2
```

This is a **race condition**.

---

# 22. Thread safety

Code is thread-safe when it behaves correctly when accessed concurrently according to its intended contract.

For example, a counter can be made thread-safe using synchronization:

```java
class Counter {

    private int count;

    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}
```

Now only one thread at a time can execute those synchronized methods for the same object monitor.

We'll cover synchronization in detail next.

---

# 23. Shared mutable state

The biggest source of many concurrency bugs is:

> **Shared mutable state.**

For example:

```text
Thread A ──┐
           │
           ▼
       shared object
           ▲
           │
Thread B ──┘
```

If both threads modify the same state without appropriate coordination, problems can occur.

This is why immutable objects are so useful in concurrent systems.

---

# 24. Thread-safe vs synchronized

Don't say:

> "Thread-safe means synchronized."

That's incorrect.

Thread safety can be achieved through:

```text
synchronized
volatile
AtomicInteger / atomic classes
locks
immutable objects
concurrent collections
thread confinement
message passing
```

We'll cover these individually.

---

# 25. `volatile`

You should know the basic concept now; we'll go deeper later.

Suppose:

```java
private volatile boolean running = true;
```

`volatile` provides important **visibility and ordering guarantees** between threads.

If one thread changes:

```java
running = false;
```

another thread reading `running` can observe that update according to the Java Memory Model's volatile rules.

But:

```java
volatile int count;
```

does **not** make:

```java
count++;
```

atomic.

This distinction is extremely important.

---

# 26. Atomicity vs visibility

Two separate concepts:

### Visibility

Does one thread see another thread's update?

### Atomicity

Does an operation happen as one indivisible unit?

For example:

```java
volatile int count;
```

helps with visibility.

But:

```java
count++;
```

is still a read-modify-write operation and isn't made atomic merely by `volatile`.

---

# 27. Thread priority

Java allows:

```java
thread.setPriority(...);
```

with values from:

```text
Thread.MIN_PRIORITY
Thread.NORM_PRIORITY
Thread.MAX_PRIORITY
```

However, don't build correctness around thread priority.

Scheduling is ultimately platform/JVM/OS dependent.

---

# 28. Daemon threads

A daemon thread is a background thread.

Example:

```java
Thread thread = new Thread(task);

thread.setDaemon(true);
thread.start();
```

The JVM can terminate when no **non-daemon** threads remain, even if daemon threads are still running.

Typical examples include background/support activities.

Important:

```java
thread.setDaemon(true);
```

must be called **before** `start()`.

---

# 29. `Runnable` does not return a result

Consider:

```java
Runnable task = () -> {
    return 10; // ❌
};
```

If you need a result:

```java
Callable<Integer> task = () -> {
    return 10;
};
```

Then:

```java
Future<Integer> future = executor.submit(task);

Integer result = future.get();
```

This leads directly into `ExecutorService`, which we'll cover separately.

---

# 30. Modern Java approach

In real applications, you generally shouldn't manually create hundreds of threads:

```java
new Thread(...).start();
new Thread(...).start();
new Thread(...).start();
```

Instead, use:

```java
ExecutorService
```

for thread pooling and task management.

For example:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);

executor.submit(() -> {
    System.out.println("Task");
});

executor.shutdown();
```

We'll cover this in detail later.

---

# 31. A common interview scenario

Suppose an interviewer asks:

> Two threads increment the same counter 1,000 times. Is the final result guaranteed to be 2,000?

If:

```java
count++;
```

is unsynchronized:

> **No.**

Because the operation isn't atomic.

Possible solutions include:

```java
synchronized
AtomicInteger
Lock
```

For example:

```java
AtomicInteger count = new AtomicInteger();

count.incrementAndGet();
```

We'll cover atomic classes later.

---

# 32. Another classic question

### What happens when you call `start()`?

Good answer:

> `start()` requests the JVM to start a new thread of execution. The new thread eventually executes the `run()` method. Calling `run()` directly does not create a new thread; it is simply a normal method call.

---

# 33. Thread lifecycle summary

```text
                  start()
NEW ─────────────────────────► RUNNABLE
                                  │
                    ┌─────────────┼─────────────┐
                    │             │             │
                 BLOCKED       WAITING     TIMED_WAITING
                    │             │             │
                    └─────────────┴─────────────┘
                                  │
                                  ▼
                             RUNNABLE
                                  │
                                  │ run() completes
                                  ▼
                             TERMINATED
```

---

# 34. The most important interview distinctions

### `start()` vs `run()`

```text
start()
→ new thread execution

run()
→ normal method call
```

### `sleep()` vs `wait()`

```text
sleep()
→ Thread method
→ doesn't release monitors

wait()
→ Object method
→ releases object's monitor
```

### `Runnable` vs `Callable`

```text
Runnable
→ no return value

Callable<T>
→ returns T
→ can throw checked exceptions
```

### `volatile` vs `synchronized`

```text
volatile
→ visibility/order guarantees
→ doesn't make compound operations atomic

synchronized
→ mutual exclusion + memory visibility/order guarantees
```

### Process vs Thread

```text
Process
→ independent execution environment/resource boundary

Thread
→ execution unit within a process
```

---

# 35. Interview-ready answer

If asked:

> **What is multithreading in Java?**

You can answer:

> Multithreading allows multiple threads of execution to run concurrently within a process. Threads share process-level resources such as heap memory but have their own execution stacks. Java provides `Thread`, `Runnable`, and `Callable` for defining and executing tasks. Because threads can access shared mutable state concurrently, synchronization, volatile variables, atomic classes, locks, immutable objects, and concurrent collections may be required to guarantee thread safety.

---

## Mental model to remember

```text
                  Java Process
                       │
          ┌────────────┼────────────┐
          │            │            │
       Thread A     Thread B     Thread C
          │            │            │
       Stack A      Stack B      Stack C
          │            │            │
          └────────────┼────────────┘
                       │
                    Shared
                     Heap
                       │
                 Shared Objects
                       │
              ┌────────┴────────┐
              │                 │
         Safe access?       Unsafe access?
              │                 │
       synchronization      Race condition
       atomic classes       visibility issue
       locks               data corruption
       immutability
```

**Next topic: Synchronization, `volatile`, Atomic Classes & Race Conditions in depth** — including `synchronized` method vs block, intrinsic locks/monitors, `ReentrantLock`, atomicity vs visibility vs ordering, happens-before, and how to solve the counter problem correctly.

