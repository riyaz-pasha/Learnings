## 20. ExecutorService, Thread Pools, Callable & Future

This is one of the most important practical multithreading topics for Java interviews.

The key idea is:

> **Don't usually create a new thread for every task. Submit tasks to a thread pool and let an executor manage the threads.**

---

# 1. Why not create threads manually?

You can do:

```java
Thread thread = new Thread(() -> {
    System.out.println("Doing work");
});

thread.start();
```

But imagine you have **10,000 tasks**.

Creating 10,000 threads is usually a bad idea because threads have:

* memory overhead
* scheduling overhead
* creation/destruction cost
* context-switching cost

Instead, use a fixed number of worker threads.

```text
10,000 tasks
     ↓
ExecutorService
     ↓
Thread Pool
 ┌────┬────┬────┬────┐
 T1   T2   T3   T4
```

The workers repeatedly take tasks from a queue.

---

# 2. ExecutorService

The main API is:

```java
ExecutorService
```

from:

```java
java.util.concurrent
```

Example:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);
```

Then:

```java
executor.submit(() -> {
    System.out.println("Task running");
});
```

The executor manages the worker threads for you.

---

# 3. Basic architecture

Think of it like:

```text
Your application
      |
      | submit(task)
      ↓
ExecutorService
      |
      ↓
Task Queue
      |
      ↓
Worker Threads
 ┌────┼────┬────┐
 T1   T2   T3   T4
```

If all workers are busy, tasks wait in the executor's queue according to the executor configuration.

---

# 4. `execute()` vs `submit()`

This is a common interview question.

### `execute()`

```java
executor.execute(() -> {
    System.out.println("Hello");
});
```

Used for a `Runnable`.

It doesn't return a result.

---

### `submit()`

```java
Future<?> future = executor.submit(() -> {
    System.out.println("Hello");
});
```

Returns a:

```java
Future
```

which can represent the pending result/completion of the task.

---

# 5. Runnable vs Callable

You already saw this distinction in multithreading.

### Runnable

```java
Runnable task = () -> {
    System.out.println("Processing");
};
```

No return value.

Conceptually:

```java
void run()
```

---

### Callable

```java
Callable<Integer> task = () -> {
    return 10 + 20;
};
```

Returns a value:

```java
Integer
```

and can throw checked exceptions.

Conceptually:

```java
V call() throws Exception
```

---

# 6. Callable + Future

Suppose:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);
```

Submit:

```java
Future<Integer> future = executor.submit(() -> {
    return 10 + 20;
});
```

Then:

```java
Integer result = future.get();
```

Result:

```text
30
```

The flow is:

```text
Callable
   ↓
submit()
   ↓
Future<Integer>
   ↓
get()
   ↓
result
```

---

# 7. What is Future?

A `Future` represents the result of an asynchronous computation.

You can think:

```text
"I submitted some work.
I don't have the result yet,
but here's a handle representing that work."
```

Example:

```java
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(2000);
    return 100;
});
```

The task takes two seconds.

Your current thread doesn't necessarily have to perform the task itself.

Later:

```java
Integer result = future.get();
```

---

# 8. Important: `future.get()` can block

Suppose:

```java
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(5000);
    return 100;
});

Integer result = future.get();
```

If the task isn't finished, `get()` waits.

So:

```text
submit()
   ↓
task running in worker
   ↓
get()
   ↓
wait if necessary
   ↓
result
```

This is why simply using asynchronous execution doesn't automatically make the entire program non-blocking.

---

# 9. `get(timeout)`

You can avoid waiting forever:

```java
Integer result =
        future.get(2, TimeUnit.SECONDS);
```

Possible exception:

```java
TimeoutException
```

if the result isn't available within the timeout.

This is often useful in real systems.

---

# 10. Cancelling a task

You can call:

```java
future.cancel(true);
```

The boolean indicates whether the executor should attempt to interrupt the running task.

You can check:

```java
future.isCancelled();
```

and:

```java
future.isDone();
```

Example:

```java
if (!future.isDone()) {
    future.cancel(true);
}
```

### Important interview point

Cancellation/interruption is a **request**, not necessarily a guarantee that arbitrary code instantly stops.

A task should cooperate with interruption.

---

# 11. Thread interruption

Suppose:

```java
Future<?> future = executor.submit(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        // work
    }
});
```

Then:

```java
future.cancel(true);
```

may interrupt the worker thread.

Good interrupt-aware code should generally respond appropriately rather than swallowing the interruption.

For example:

```java
try {
    Thread.sleep(1000);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return;
}
```

Restoring the interrupt status is important when you're not handling the interruption completely at that layer.

---

# 12. Common thread pools

`Executors` provides factory methods such as:

```java
Executors.newFixedThreadPool(...)
Executors.newSingleThreadExecutor()
Executors.newCachedThreadPool()
Executors.newScheduledThreadPool(...)
```

Let's understand them.

---

# 13. Fixed Thread Pool

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);
```

Creates a pool with a fixed number of worker threads.

Conceptually:

```text
Pool size = 4

T1
T2
T3
T4
```

If you submit 100 tasks:

```text
Task 1 → T1
Task 2 → T2
Task 3 → T3
Task 4 → T4

Task 5 → waits
Task 6 → waits
...
```

As workers finish, they pick up more tasks.

### Good for

Workloads where you want to control concurrency.

---

# 14. Single Thread Executor

```java
ExecutorService executor =
        Executors.newSingleThreadExecutor();
```

Only one worker executes tasks.

So:

```text
Task 1
   ↓
Task 2
   ↓
Task 3
   ↓
Task 4
```

Tasks execute sequentially through that executor.

It can be useful when you want serialized execution while still using the executor abstraction.

---

# 15. Cached Thread Pool

```java
ExecutorService executor =
        Executors.newCachedThreadPool();
```

Designed for workloads with many short-lived asynchronous tasks.

It can create threads as needed and reuse idle threads.

But you should understand an important practical concern:

> An unbounded or poorly bounded concurrency strategy can create resource pressure under heavy load.

That's why production applications often configure `ThreadPoolExecutor` explicitly rather than blindly using `Executors` factory methods.

---

# 16. Scheduled Executor

For delayed or periodic tasks:

```java
ScheduledExecutorService scheduler =
        Executors.newScheduledThreadPool(2);
```

Example:

```java
scheduler.schedule(
        () -> System.out.println("Hello"),
        5,
        TimeUnit.SECONDS
);
```

Runs approximately five seconds later.

Periodic execution:

```java
scheduler.scheduleAtFixedRate(
        () -> System.out.println("Running"),
        0,
        10,
        TimeUnit.SECONDS
);
```

---

# 17. `shutdown()`

This is extremely important.

After you're done:

```java
executor.shutdown();
```

It means:

> Stop accepting new tasks, but allow already submitted tasks to finish.

You can then wait:

```java
executor.awaitTermination(
        10,
        TimeUnit.SECONDS
);
```

---

# 18. `shutdownNow()`

```java
executor.shutdownNow();
```

Attempts to stop currently executing tasks, typically by interrupting worker threads, and returns tasks that were awaiting execution.

Again:

> `shutdownNow()` is a request, not a magical force-stop.

Tasks must cooperate with interruption.

---

# 19. `shutdown()` vs `shutdownNow()`

|                       | `shutdown()`                 | `shutdownNow()`         |
| --------------------- | ---------------------------- | ----------------------- |
| Accept new tasks      | No                           | No                      |
| Existing queued tasks | Generally allowed to execute | Attempts to return them |
| Running tasks         | Allowed to finish            | Attempts interruption   |
| Force kill threads    | No                           | No                      |

---

# 20. Why thread pools improve performance

Suppose processing a task takes:

```text
100 ms
```

Creating/destroying a thread for every tiny task introduces overhead.

With a pool:

```text
Create worker threads
       ↓
Reuse workers
       ↓
Task
       ↓
Task
       ↓
Task
       ↓
Task
```

The thread creation cost is amortized across many tasks.

---

# 21. ThreadPoolExecutor

Under the hood, a lot of executor behavior can be configured using:

```java
ThreadPoolExecutor
```

Example:

```java
ThreadPoolExecutor executor =
        new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
```

Important concepts:

```text
corePoolSize
maximumPoolSize
keepAliveTime
workQueue
threadFactory
rejectedExecutionHandler
```

These are very interview-relevant.

---

# 22. Core Pool Size

Suppose:

```java
corePoolSize = 2
maximumPoolSize = 4
```

The executor tries to maintain core workers.

Conceptually:

```text
Initially:

T1
T2
```

Additional workload can cause additional workers to be created, depending on the queue and executor configuration.

---

# 23. Maximum Pool Size

This is the upper bound on worker threads created by the executor.

For example:

```text
core = 2
max  = 4
```

At most four worker threads can execute tasks concurrently under that executor.

But there's an important interview nuance:

> Whether the executor creates threads beyond `corePoolSize` depends heavily on the queue behavior.

---

# 24. Work Queue

Tasks that can't immediately execute may wait in a queue.

Conceptually:

```text
                    ┌── Worker 1
Tasks → Queue ──────┼── Worker 2
                    ├── Worker 3
                    └── Worker 4
```

Common queues include:

```java
BlockingQueue<Runnable>
```

Examples:

```java
ArrayBlockingQueue
LinkedBlockingQueue
SynchronousQueue
```

Queue choice affects executor behavior significantly.

---

# 25. RejectedExecutionHandler

What happens if the executor cannot accept another task?

The executor can reject it.

Policies include:

```java
AbortPolicy
CallerRunsPolicy
DiscardPolicy
DiscardOldestPolicy
```

Default:

```java
ThreadPoolExecutor.AbortPolicy
```

typically throws:

```java
RejectedExecutionException
```

---

# 26. `CallerRunsPolicy`

Interesting interview example:

```java
new ThreadPoolExecutor.CallerRunsPolicy()
```

If the pool cannot accept the task, the submitting thread may execute the task itself.

Conceptually:

```text
Worker pool overloaded
        ↓
Task rejected from pool
        ↓
Calling thread executes task
```

This can naturally slow down the producer and provide a form of backpressure.

---

# 27. How should you size a thread pool?

There is no universal number.

It depends on the workload.

### CPU-bound

Example:

```text
complex calculations
image processing
CPU-heavy algorithms
```

Generally use a pool around the number of available processors, with tuning based on workload.

You can inspect:

```java
Runtime.getRuntime().availableProcessors();
```

### I/O-bound

Example:

```text
database calls
HTTP calls
file operations
```

Threads may spend substantial time waiting, so more concurrency can sometimes be useful.

But don't blindly use:

```text
CPU count × 100
```

or another fixed formula.

Measure and tune based on:

* latency
* throughput
* CPU utilization
* downstream capacity
* queue length
* memory
* task behavior

---

# 28. A very common interview trap

Consider:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(2);

Future<Integer> f1 =
        executor.submit(() -> slowOperation1());

Future<Integer> f2 =
        executor.submit(() -> slowOperation2());

Integer a = f1.get();
Integer b = f2.get();
```

Are the two operations sequential?

**No.**

They can execute concurrently because both were submitted before calling `get()`.

The current thread waits for the results afterward.

Contrast that with:

```java
Integer a = executor.submit(() -> slowOperation1()).get();

Integer b = executor.submit(() -> slowOperation2()).get();
```

Here you're waiting for the first task before submitting the second, so the operations are effectively sequential from this code's perspective.

---

# 29. Example: parallel processing

Suppose:

```java
List<Integer> numbers = List.of(1, 2, 3, 4);
```

We want to process them concurrently.

Conceptually:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(4);

List<Future<Integer>> futures = new ArrayList<>();

for (Integer number : numbers) {

    futures.add(
        executor.submit(() -> number * number)
    );
}

for (Future<Integer> future : futures) {
    System.out.println(future.get());
}

executor.shutdown();
```

Possible output:

```text
1
4
9
16
```

The tasks can execute concurrently, while `Future` lets us collect their results.

---

# 30. A better modern pattern: `invokeAll`

Instead of manually submitting every `Callable`:

```java
List<Callable<Integer>> tasks = List.of(
    () -> 1 * 1,
    () -> 2 * 2,
    () -> 3 * 3
);
```

You can:

```java
List<Future<Integer>> futures =
        executor.invokeAll(tasks);
```

`invokeAll()` waits for the submitted tasks to complete and returns their futures.

There is also:

```java
invokeAny()
```

which returns the result of one successfully completed task.

---

# 31. `invokeAny()`

Suppose you query multiple equivalent services:

```text
Service A
Service B
Service C
```

and only need one successful result.

Conceptually:

```java
String result = executor.invokeAny(tasks);
```

It returns one successfully completed result and may cancel remaining unfinished tasks.

This can be useful for redundant/alternative computations.

---

# 32. ExecutorService mental model

Remember:

```text
                 ExecutorService
                       |
                 accepts tasks
                       |
                       ↓
                   Queue
                       |
              ┌────────┼────────┐
              ↓        ↓        ↓
             T1       T2       T3
              |        |        |
            Task     Task     Task
```

Your application submits **tasks**, while the executor manages **threads**.

That separation is the major conceptual benefit.

---

# 33. Interview questions you should know

### Q: Why use ExecutorService instead of creating threads manually?

> It separates task submission from thread management and allows thread reuse, controlled concurrency, task queuing, lifecycle management, and result handling.

### Q: `execute()` vs `submit()`?

> `execute()` accepts a `Runnable` and doesn't return a `Future`; `submit()` returns a `Future` and can accept `Runnable` or `Callable`.

### Q: Runnable vs Callable?

> Runnable doesn't return a result and cannot declare checked exceptions through `run()`; Callable returns a result and can throw checked exceptions.

### Q: What is Future?

> A handle representing the result/completion state of an asynchronous computation.

### Q: Does `future.get()` block?

> Yes, if the task hasn't completed.

### Q: `shutdown()` vs `shutdownNow()`?

> `shutdown()` stops accepting new tasks and lets submitted work complete; `shutdownNow()` attempts to interrupt running tasks and returns queued tasks that weren't started.

### Q: Why shouldn't you blindly create unlimited threads?

> Threads consume resources and excessive concurrency causes scheduling overhead, memory pressure, contention, and potentially overloads downstream systems.

### Q: What is a thread pool?

> A reusable set of worker threads that execute submitted tasks.

---

# 34. One important real-world point

In modern Java, you may also encounter **virtual threads**.

Java 21 introduced virtual threads as lightweight threads designed particularly for high-concurrency workloads that spend significant time waiting on I/O.

Example:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {

    Future<String> future = executor.submit(() -> {
        return callRemoteService();
    });

    System.out.println(future.get());
}
```

The important interview distinction is:

```text
Platform thread
    ↓
OS-backed execution resource

Virtual thread
    ↓
lightweight Java-managed thread
    ↓
can be multiplexed onto platform threads
```

Virtual threads don't make CPU-bound work magically faster. Their major benefit is enabling large numbers of concurrent tasks, particularly blocking I/O workloads, with much lower thread overhead.

We'll cover virtual threads more deeply when we reach **modern Java 8 → 21**.

---

## The mental model to remember

```text
Thread
  ↓
manually manages execution

ExecutorService
  ↓
manages worker threads
  ↓
accepts Runnable / Callable
  ↓
returns Future
  ↓
supports cancellation / waiting
```

And:

```text
execute()
   → fire-and-forget task

submit()
   → Future

Callable
   → result

Future.get()
   → retrieve result, may block

shutdown()
   → graceful shutdown

shutdownNow()
   → attempt interruption
```

**Next:** `CompletableFuture` — asynchronous pipelines, `thenApply`, `thenCompose`, `thenCombine`, `exceptionally`, `handle`, and the difference between `thenApply` and `thenCompose`.

