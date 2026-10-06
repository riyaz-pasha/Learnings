# Java Virtual Threads

Virtual threads are one of the biggest changes to Java's concurrency model since the introduction of `java.util.concurrent`.

The simplest way to think about them is:

> **Virtual threads let you write normal blocking-style Java code while allowing the JVM to run a very large number of concurrent tasks using a small number of OS threads.**

They were introduced as part of **Project Loom** and became a permanent feature in **Java 21**.

---

# 1. First: What problem were virtual threads created to solve?

Before virtual threads, Java applications primarily used **platform threads**.

For example:

```java
Thread thread = new Thread(() -> {
    processRequest();
});

thread.start();
```

A traditional Java `Thread` is closely associated with an **OS thread**.

If your application has:

```text
10,000 requests
        ↓
10,000 Java threads
        ↓
10,000 OS threads
```

that's problematic.

OS threads are relatively expensive.

They consume:

* memory
* OS scheduling resources
* CPU time when being scheduled
* kernel resources

So applications traditionally used **thread pools**.

For example:

```java
ExecutorService executor =
        Executors.newFixedThreadPool(200);
```

Now:

```text
10,000 requests
       ↓
    Queue
       ↓
  200 threads
       ↓
   OS threads
```

This is much more manageable.

But it creates another problem.

---

# 2. The fundamental problem: blocking I/O

Imagine a web request:

```java
void handleRequest() {

    User user = database.findUser();

    Payment payment = paymentService.getPayment();

    Email email = emailService.getEmail();

    return buildResponse(user, payment, email);
}
```

Suppose:

```text
database.findUser()
       ↓
      50 ms
```

During those 50 ms, the thread isn't doing useful CPU work.

It's **waiting**.

Similarly:

```text
Java thread
    │
    ├── execute Java code
    │
    ├── database call
    │       ↓
    │     WAIT
    │
    ├── execute Java code
    │
    ├── HTTP call
    │       ↓
    │     WAIT
    │
    └── execute Java code
```

The thread spends a significant amount of time waiting for external systems.

---

# 3. Why is that a problem?

Imagine your application receives:

```text
10,000 concurrent requests
```

and each request spends most of its time waiting for:

* database
* HTTP services
* Redis
* Kafka
* filesystem
* etc.

With a traditional thread pool:

```text
10,000 requests

        ↓

200 platform threads

        ↓

Only 200 requests can actively occupy threads
```

The rest wait in queues.

You could increase the thread pool:

```text
200
↓
500
↓
1000
↓
5000
```

But eventually the OS and JVM have to manage thousands of expensive threads.

You don't want to solve an I/O concurrency problem by creating enormous numbers of OS threads.

---

# 4. The traditional solution: asynchronous programming

One solution is to avoid blocking.

Instead of:

```java
User user = database.findUser();
```

you could use asynchronous APIs:

```java
database.findUserAsync()
    .thenCompose(user ->
        paymentService.getPaymentAsync(user)
    )
    .thenCompose(payment ->
        ...
    );
```

Now the underlying thread can be released while the operation is waiting.

This is efficient.

But there's a major downside:

## The code becomes harder to write and reason about.

Instead of:

```java
User user = getUser();

Payment payment = getPayment(user);

return createResponse(payment);
```

you end up with:

```java
getUserAsync()
    .thenCompose(this::getPaymentAsync)
    .thenApply(this::createResponse);
```

And complex workflows can become even harder:

```text
callbacks
futures
completion stages
callbacks
error handling
timeouts
composition
```

This is sometimes called **callback/future-style asynchronous programming**.

---

# 5. The dream

Java developers wanted something like:

```java
User user = getUser();

Payment payment = getPayment(user);

return createResponse(payment);
```

while getting concurrency characteristics closer to:

```text
Asynchronous I/O
```

without manually turning the entire application into callback/future code.

That is where **virtual threads** come in.

---

# 6. What is a virtual thread?

A virtual thread is a Java `Thread` that is **managed by the JVM rather than being permanently tied to one OS thread**.

Conceptually:

```text
Platform threads

Java Thread  ─────────── OS Thread
Java Thread  ─────────── OS Thread
Java Thread  ─────────── OS Thread
```

Virtual threads:

```text
Virtual Thread ─┐
Virtual Thread ─┤
Virtual Thread ─┤
Virtual Thread ─┤
Virtual Thread ─┤
Virtual Thread ─┤
                ↓
         JVM scheduler
                ↓
       small number of
       OS/platform threads
```

So you can have:

```text
1,000,000 virtual threads
```

without needing:

```text
1,000,000 OS threads
```

---

# 7. The key idea: mounting and unmounting

This is the most important concept for understanding virtual threads.

Suppose:

```java
Thread.startVirtualThread(() -> {

    User user = database.findUser();

    process(user);
});
```

The virtual thread starts running on a platform thread.

Conceptually:

```text
Virtual Thread V1
       │
       ↓
Platform Thread P1
       │
       ↓
CPU executes Java code
```

Then:

```java
database.findUser();
```

blocks waiting for I/O.

Instead of keeping the underlying platform thread occupied, the JVM can:

```text
Virtual Thread V1
       │
       ↓
    WAITING
```

and free the platform thread:

```text
Platform Thread P1
       │
       ↓
available
```

That platform thread can now execute another virtual thread:

```text
V2
 ↓
P1
```

Later, when the database operation completes:

```text
V1 becomes runnable
       ↓
JVM schedules V1
       ↓
possibly P2
       ↓
continue execution
```

So:

```text
        Virtual threads
       V1 V2 V3 V4 V5
        │  │  │  │  │
        └──┴──┴──┴──┘
               ↓
        JVM scheduler
               ↓
       P1 P2 P3 P4
       platform threads
```

This is the fundamental mechanism.

---

# 8. Virtual threads are still `Thread`

This is important.

You don't learn an entirely new concurrency API.

For example:

```java
Thread.startVirtualThread(() -> {
    System.out.println("Hello");
});
```

Or:

```java
Thread thread = Thread.ofVirtual()
        .start(() -> {
            System.out.println("Hello");
        });
```

You can also use an executor:

```java
try (ExecutorService executor =
         Executors.newVirtualThreadPerTaskExecutor()) {

    executor.submit(() -> processRequest());
}
```

This is a very important API:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

It essentially means:

> Create a new virtual thread for every submitted task.

---

# 9. Why "one thread per task" becomes practical

Traditionally:

```java
Executors.newFixedThreadPool(200)
```

because creating a thread for every task was expensive.

With virtual threads:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

becomes practical.

For example:

```java
try (var executor =
         Executors.newVirtualThreadPerTaskExecutor()) {

    for (int i = 0; i < 10_000; i++) {

        int taskId = i;

        executor.submit(() -> {
            processTask(taskId);
        });
    }
}
```

You can have thousands of concurrent tasks.

The JVM doesn't need thousands of OS threads.

---

# 10. Platform thread vs virtual thread

A useful comparison:

|                         | Platform Thread     | Virtual Thread           |
| ----------------------- | ------------------- | ------------------------ |
| Managed primarily by    | OS/JVM              | JVM                      |
| Backed by OS thread     | Yes                 | Not permanently          |
| Creation cost           | Relatively high     | Very low                 |
| Memory footprint        | Relatively large    | Very small               |
| Millions possible?      | Generally no        | Potentially              |
| Good for blocking I/O   | Limited scalability | Excellent                |
| Good for CPU-heavy work | Yes                 | Doesn't create extra CPU |
| Java `Thread` API       | Yes                 | Yes                      |

The critical distinction:

> **Virtual threads don't make CPU computation faster.**

They make **concurrency with blocking operations much more scalable**.

---

# 11. What virtual threads do NOT solve

This is one of the most important interview points.

Suppose you have:

```java
while (true) {
    calculateHugePrimeNumber();
}
```

and you create:

```text
1,000,000 virtual threads
```

That doesn't magically give you 1,000,000 CPUs.

Your machine might have:

```text
16 CPU cores
```

You still have roughly 16 cores worth of CPU execution capacity.

So:

```text
Virtual threads ≠ more CPU
```

Instead:

```text
Virtual threads
        ↓
better utilization of CPU while other tasks are waiting
```

---

# 12. CPU-bound vs I/O-bound

This distinction is extremely important.

### CPU-bound

Example:

```java
for (long i = 0; i < 10_000_000_000L; i++) {
    calculate(i);
}
```

The task constantly needs CPU.

Virtual threads don't provide a magical advantage.

---

### I/O-bound

Example:

```java
User user = database.getUser();

Payment payment = httpClient.getPayment();

Order order = redis.getOrder();
```

The task spends a lot of time waiting.

Virtual threads are extremely useful here.

Conceptually:

```text
1000 requests

CPU work:      ███
DB waiting:    ███████████████
HTTP waiting:  █████████████
CPU work:      ███
```

Virtual threads allow the JVM to use the underlying platform threads for other runnable tasks during those waiting periods.

---

# 13. How does the JVM schedule virtual threads?

This is another important interview concept.

Virtual threads use a JVM-managed scheduler.

The scheduler uses a pool of platform threads called **carrier threads**.

Conceptually:

```text
Virtual Threads

 V1 V2 V3 V4 V5 V6 V7 V8
        │
        ↓
   JVM Scheduler
        │
        ↓
Carrier Threads

 C1 C2 C3 C4
```

A virtual thread runs on a carrier thread.

For example:

```text
V1 → C1
V2 → C2
V3 → C3
V4 → C4
```

Then V1 blocks on I/O:

```text
V1 → WAITING
```

C1 becomes available:

```text
V5 → C1
```

When V1 becomes runnable:

```text
V1 → scheduler → available carrier
```

It could resume on:

```text
C2
```

It does **not** need to return to the exact same carrier thread.

---

# 14. This means virtual threads are not permanently tied to a carrier

Consider:

```text
V1
 │
 ├── runs on C1
 │
 ├── blocks
 │
 └── resumes on C3
```

That's completely fine.

The important abstraction is:

```text
Virtual Thread
     ↓
logical execution context
```

rather than:

```text
Virtual Thread
     ↓
permanent OS thread
```

---

# 15. What happens to the stack?

Traditional threads have relatively large stacks associated with the OS thread.

Virtual threads use a different mechanism.

Their execution state can be stored in JVM-managed structures, with stack frames represented as **stack chunks**.

Conceptually:

```text
Virtual Thread

+---------------------+
| Execution state      |
| Stack chunks         |
| Local variables      |
| Continuation state   |
+---------------------+
```

When the virtual thread is suspended, the JVM can preserve its execution state without requiring a dedicated OS thread to remain attached to it.

This is a major reason virtual threads can be lightweight.

---

# 16. Continuations

Under the hood, virtual threads rely on the concept of a **continuation**.

You don't normally use continuations directly.

Conceptually:

```text
Virtual Thread
      ↓
Continuation
      ↓
"Where should execution continue?"
```

Suppose:

```java
A();

B();

databaseCall();

C();

D();
```

If the database call causes the virtual thread to suspend, the JVM needs to preserve:

```text
"I was executing here."

databaseCall()
     ↓
suspend
     ↓
resume here
     ↓
C()
```

That's part of the machinery behind virtual threads.

You can think of a continuation as:

> A representation of suspended computation that can later resume from where it stopped.

---

# 17. Why this is better than manually writing async code

Consider traditional asynchronous programming.

You might have:

```java
CompletableFuture<User> userFuture =
        getUserAsync();

return userFuture.thenCompose(user ->
        getPaymentAsync(user)
).thenCompose(payment ->
        getOrderAsync(payment)
).thenApply(order ->
        createResponse(order)
);
```

With virtual threads:

```java
try (var executor =
         Executors.newVirtualThreadPerTaskExecutor()) {

    executor.submit(() -> {

        User user = getUser();

        Payment payment = getPayment(user);

        Order order = getOrder(payment);

        return createResponse(order);
    });
}
```

The second version looks like ordinary sequential code.

Yet blocking operations can release the underlying carrier thread.

This is one of the central goals of Project Loom:

> **Make highly concurrent applications easier to write by allowing straightforward synchronous-style code.**

---

# 18. Why not just use `CompletableFuture` everywhere?

`CompletableFuture` is still useful.

Virtual threads and asynchronous programming solve related but different problems.

### CompletableFuture

You explicitly represent asynchronous computation:

```java
CompletableFuture<User>
```

and compose stages:

```java
.thenCompose(...)
.thenApply(...)
.exceptionally(...)
```

### Virtual threads

You can simply write:

```java
User user = getUser();
Payment payment = getPayment(user);
```

and let the JVM manage the underlying thread resources.

So:

```text
CompletableFuture
     ↓
explicit async programming model

Virtual Thread
     ↓
lightweight synchronous programming model
```

Neither universally replaces the other.

---

# 19. Why were thread pools traditionally necessary?

Before virtual threads:

```java
ExecutorService pool =
    Executors.newFixedThreadPool(100);
```

was common because:

```text
Thread creation
      ↓
expensive

Too many threads
      ↓
memory pressure
      ↓
OS scheduling overhead
      ↓
poor scalability
```

Therefore:

```text
Thread pool
      ↓
reuse a limited number of threads
```

But virtual threads change the economics.

You can often use:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

instead of trying to carefully tune a huge platform-thread pool.

---

# 20. Does this mean thread pools are obsolete?

**No.**

This is a common interview trap.

Virtual threads don't mean:

> "Never use executors or pools."

Instead, they change **what you pool**.

For platform threads:

```text
Pool platform threads
```

For virtual threads:

```text
Usually don't pool virtual threads.
Create one per task.
```

For example:

```java
Executors.newVirtualThreadPerTaskExecutor()
```

is intentionally designed around this model.

However, you may still need to limit access to external resources.

Suppose your database supports only:

```text
100 concurrent connections
```

You shouldn't create:

```text
10,000 concurrent DB operations
```

just because virtual threads make it cheap.

You need **resource concurrency limits**.

For example:

```text
100,000 virtual threads
        ↓
       Semaphore
        ↓
100 DB operations
        ↓
Database
```

This is a very important architectural distinction:

> **Virtual threads remove the need to limit concurrency because of thread scarcity, but you still need to limit concurrency because external resources have finite capacity.**

---

# 21. Virtual threads and database connections

Imagine:

```java
Thread.ofVirtual().start(() -> {

    Connection connection = dataSource.getConnection();

    // query

});
```

Virtual threads make the thread cheap.

But:

```text
Database connections
```

are still expensive and limited.

Suppose:

```text
DB connection pool = 100
Virtual threads = 50,000
```

You still can't execute 50,000 queries simultaneously through a 100-connection database pool.

So:

```text
Virtual thread scalability
            ≠
Unlimited database scalability
```

---

# 22. Virtual threads and `synchronized`

This is a more advanced topic.

Virtual threads generally work well with normal Java synchronization, but there are situations where a virtual thread can become **pinned** to its carrier thread.

For example, certain blocking operations performed while holding a monitor can prevent the virtual thread from being unmounted.

Conceptually:

```text
Virtual Thread
      ↓
enters synchronized block
      ↓
blocking operation
      ↓
may remain mounted
      ↓
carrier thread cannot be freely reused
```

This is called **pinning**.

Modern JDKs have improved this area substantially, but the concept remains important.

The practical lesson is:

> Avoid performing long/blocking operations while holding intrinsic locks when designing code for virtual threads.

For example, avoid:

```java
synchronized (lock) {

    databaseCall(); // potentially problematic design

}
```

Prefer minimizing the synchronized region:

```java
synchronized (lock) {
    updateSharedState();
}

databaseCall();
```

when the application semantics allow it.

---

# 23. `ThreadLocal` considerations

Virtual threads change the scale of concurrency.

Suppose you create:

```text
1,000,000 virtual threads
```

and each thread carries a large amount of `ThreadLocal` state.

Then:

```text
1,000,000 × ThreadLocal data
```

can become expensive.

So applications should be careful with:

```java
ThreadLocal
```

when moving from a small number of platform threads to huge numbers of virtual threads.

This doesn't mean:

> "Never use ThreadLocal."

It means:

> Don't assume thread-local state is cheap simply because the thread itself is cheap.

---

# 24. Virtual threads and `ThreadLocal`

A virtual thread does have its own thread-local context.

That's useful for things such as:

```text
request context
security context
logging context
```

But because there may be vastly more threads, the total memory cost can become significant.

Therefore:

```text
platform threads:
    maybe 200 ThreadLocals

virtual threads:
    potentially 100,000+ ThreadLocals
```

Design carefully.

---

# 25. Virtual threads and `ThreadLocal` vs `ScopedValue`

Modern Java also introduced **Scoped Values** as a better mechanism for some use cases where immutable contextual data needs to be passed down a call tree.

Conceptually:

```text
Request
   │
   ├── service A
   │
   ├── service B
   │
   └── service C
```

Instead of relying heavily on mutable thread-local state, scoped values can provide structured contextual data.

This becomes particularly interesting with virtual threads and structured concurrency.

---

# 26. Structured concurrency

Virtual threads also fit into a broader concurrency model called **structured concurrency**.

Imagine:

```text
Request
 ├── fetchUser()
 ├── fetchOrders()
 └── fetchRecommendations()
```

These tasks belong to the request.

You ideally want:

```text
Request starts
   │
   ├── Task A
   ├── Task B
   └── Task C
   │
   └── Request finishes
```

rather than creating detached background tasks that outlive their parent.

Structured concurrency provides a structured way of managing these relationships.

This is particularly powerful with virtual threads.

---

# 27. Example: traditional approach

Suppose you need:

```text
User
Orders
Recommendations
```

You might write:

```java
CompletableFuture<User> user =
        getUserAsync();

CompletableFuture<List<Order>> orders =
        getOrdersAsync();

CompletableFuture<List<Product>> recommendations =
        getRecommendationsAsync();

return user
        .thenCombine(orders, ...)
        .thenCombine(recommendations, ...);
```

It works.

But the code becomes increasingly complex as workflows grow.

---

# 28. Virtual-thread approach

Conceptually:

```java
try (var executor =
         Executors.newVirtualThreadPerTaskExecutor()) {

    Future<User> user =
        executor.submit(this::getUser);

    Future<List<Order>> orders =
        executor.submit(this::getOrders);

    Future<List<Product>> recommendations =
        executor.submit(this::getRecommendations);

    return buildResponse(
        user.get(),
        orders.get(),
        recommendations.get()
    );
}
```

This is much easier to reason about.

The `get()` calls block the virtual threads, not scarce platform threads in the traditional way.

---

# 29. Important: virtual threads don't make blocking disappear

This is a subtle but important point.

Suppose:

```java
database.query();
```

takes:

```text
5 seconds
```

The database operation still takes 5 seconds.

Virtual threads don't make it:

```text
5 seconds → 5 milliseconds
```

Instead:

```text
Traditional thread:

Thread occupied
     ↓
wait 5 seconds
```

Virtual thread:

```text
Virtual thread waits
     ↓
carrier can run another virtual thread
```

So virtual threads improve **resource utilization and scalability**, not the latency of the underlying operation.

---

# 30. A simple mental model

Think about a restaurant.

### Platform threads

Imagine each customer gets their own waiter.

```text
Customer 1 → Waiter 1
Customer 2 → Waiter 2
Customer 3 → Waiter 3
...
```

If the customer is waiting for food:

```text
Waiter 1 → standing there waiting
```

That's wasteful.

---

### Virtual threads

Instead:

```text
100 customers
       ↓
10 waiters
```

A waiter takes an order:

```text
Customer A → order placed
```

Customer A's food is cooking.

The waiter moves to:

```text
Customer B
```

When A's food is ready:

```text
waiter returns to A
```

That's essentially the idea:

```text
Virtual threads = customers/tasks

Carrier threads = waiters/workers

JVM scheduler = restaurant coordination
```

---

# 31. Virtual threads vs reactive programming

This is a major architectural discussion.

### Reactive

```text
Event loop
   ↓
non-blocking I/O
   ↓
callbacks/operators
```

Examples include:

```text
Reactor
WebFlux
RxJava
```

Advantages:

* very high concurrency
* non-blocking architecture
* efficient resource utilization

Disadvantages:

* more complex programming model
* debugging can be harder
* stack traces can be less intuitive
* requires non-blocking libraries throughout the stack

---

### Virtual threads

```text
normal Java code
      ↓
blocking calls
      ↓
JVM manages lightweight threads
```

Advantages:

* simpler code
* normal imperative programming
* straightforward debugging
* works well with blocking APIs

This is one reason virtual threads are so significant.

They provide another way to achieve high concurrency without forcing the application into a reactive programming model.

---

# 32. Does Spring support virtual threads?

Yes.

Modern Spring applications can use virtual threads, particularly with Spring Boot configurations built on modern JDKs.

For example, Spring Boot provides support for enabling virtual threads with:

```properties
spring.threads.virtual.enabled=true
```

Then appropriate task execution can use virtual threads.

The exact behavior depends on the Spring component and version, so it's important not to assume **every** thread in the entire application automatically becomes virtual.

---

# 33. When should you use virtual threads?

Excellent use cases:

### Web applications

```text
HTTP request
    ↓
DB
    ↓
HTTP service
    ↓
Redis
    ↓
response
```

Especially when many requests are concurrently waiting on I/O.

---

### Microservices

For services doing:

```text
HTTP → service A
HTTP → service B
DB
Redis
Kafka
```

virtual threads can significantly simplify concurrency.

---

### Database-heavy applications

For example:

```text
10,000 concurrent requests
        ↓
database calls
```

Virtual threads can allow a large number of requests to remain concurrently active without requiring one OS thread per request.

Again, the database itself remains the bottleneck.

---

### Blocking legacy APIs

This is particularly attractive.

Suppose you have an old API:

```java
String result = legacyClient.call();
```

You don't necessarily need to rewrite the entire application into reactive code.

A virtual thread can make blocking-style code much more scalable.

---

# 34. When shouldn't you expect much benefit?

CPU-heavy work:

```java
encryptHugeFile();
calculateHugeMatrix();
trainModel();
compressMassiveData();
```

If the workload is CPU-bound, virtual threads don't increase CPU capacity.

Use an appropriately sized platform-thread executor or parallelism strategy based on available CPU.

---

# 35. The most important architectural change

Before virtual threads:

```text
Concurrency is expensive.

Therefore:

Limit number of threads.
```

After virtual threads:

```text
Threads can be cheap.

Therefore:

Allow much more concurrency,
but limit scarce resources instead.
```

That is a profound shift.

Instead of:

```text
Thread pool size = 200
```

being your primary concurrency limit, you might have:

```text
Virtual threads = 50,000

DB connections = 100
HTTP downstream concurrency = 500
Kafka concurrency = 50
CPU workers = 16
```

The limits now correspond more directly to the actual resources.

---

# 36. The biggest misconception

Don't say in an interview:

> "Virtual threads are faster threads."

That's incorrect.

A better answer is:

> **Virtual threads are lightweight JVM-managed threads designed to make high-concurrency applications more scalable, particularly when tasks spend significant time blocked on I/O. They allow developers to use simple synchronous, blocking-style code while the JVM multiplexes many virtual threads over a much smaller number of platform/carrier threads.**

That's a strong interview answer.

---

# 37. Interview-level architecture

You should be able to visualize it like this:

```text
                 APPLICATION
                      │
          ┌───────────┴───────────┐
          │                       │
      Virtual V1              Virtual V2
          │                       │
          └───────────┬───────────┘
                      │
               JVM Scheduler
                      │
          ┌───────────┼───────────┐
          │           │           │
       Carrier C1  Carrier C2  Carrier C3
          │           │           │
       OS Thread    OS Thread    OS Thread
```

And:

```text
V1
 │
 │ database call
 ↓
WAITING
 │
 └──────────────→ C1 becomes available
                       │
                       ↓
                      V3
```

Later:

```text
Database response
       ↓
V1 runnable
       ↓
JVM scheduler
       ↓
C2
       ↓
V1 continues
```

---

# 38. Platform thread vs virtual thread: the core difference

The most important conceptual distinction is:

### Platform thread

```text
Java Thread
     │
     │ tightly associated with
     ↓
OS thread
```

### Virtual thread

```text
Java Thread
     │
     ↓
JVM-managed execution
     │
     ↓
carrier/platform thread
     │
     ↓
OS thread
```

The virtual thread isn't permanently occupying a carrier.

---

# 39. What problems did virtual threads actually solve?

Let's summarize the original problems.

### Problem 1: OS threads are expensive

Solution:

```text
Lightweight virtual threads
```

---

### Problem 2: Blocking I/O wastes platform threads

Solution:

```text
Virtual thread can suspend
while carrier executes other work
```

---

### Problem 3: Thread pools limit concurrency

Traditional:

```text
10,000 requests
      ↓
200 threads
      ↓
queue
```

Virtual threads:

```text
10,000 requests
      ↓
10,000 virtual threads
      ↓
small carrier pool
```

---

### Problem 4: Async programming is complex

Traditional:

```text
Future
CompletableFuture
callbacks
reactive pipelines
```

Virtual threads:

```java
User user = getUser();
Order order = getOrder(user);
return createResponse(order);
```

---

### Problem 5: Blocking legacy APIs don't scale well

Virtual threads let you continue using:

```java
blockingApi.call();
```

while getting much better concurrency characteristics.

---

# 40. What virtual threads do NOT solve

Remember these five:

```text
Virtual threads do NOT:

1. Make CPU faster
2. Create more CPU cores
3. Make database queries faster
4. Remove database connection limits
5. Make every blocking operation magically scalable
```

Their primary benefit is:

```text
cheap concurrency
       +
efficient handling of blocking/waiting
       +
simple programming model
```

---

# 41. Final mental model

If you remember only one diagram, remember this:

```text
                    MANY TASKS
                       │
                       ↓
              ┌─────────────────┐
              │ Virtual Threads  │
              │ V1 V2 V3 ... Vn  │
              └────────┬────────┘
                       │
                       ↓
                JVM Scheduler
                       │
              ┌────────┴────────┐
              ↓                 ↓
        Carrier Thread     Carrier Thread
              ↓                 ↓
          OS Thread         OS Thread
```

When a virtual thread does CPU work:

```text
Virtual Thread
      ↓
Carrier Thread
      ↓
CPU
```

When it performs an operation that allows it to suspend:

```text
Virtual Thread
      ↓
WAIT / SUSPEND

Carrier Thread
      ↓
FREE
      ↓
runs another Virtual Thread
```

When the operation completes:

```text
I/O completed
      ↓
Virtual Thread becomes runnable
      ↓
JVM scheduler
      ↓
Carrier Thread
      ↓
continue execution
```

So the fundamental idea is:

> **Don't make the expensive resource—the OS thread—the unit of concurrency. Make the cheap virtual thread the unit of concurrency, and let the JVM efficiently multiplex those virtual threads over a smaller number of carrier threads.**

And that is why Java can finally make code like:

```java
var user = database.findUser();
var payment = paymentService.getPayment(user);
var order = orderService.getOrder(user);

return buildResponse(user, payment, order);
```

scale to **very high concurrency** without forcing developers to turn every blocking operation into a callback or reactive pipeline.

