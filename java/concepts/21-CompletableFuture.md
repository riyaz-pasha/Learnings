## 21. CompletableFuture

`CompletableFuture` is one of the most important Java concurrency topics after `ExecutorService`.

The key idea is:

> **`Future` represents an asynchronous result; `CompletableFuture` lets you build and compose asynchronous operations.**

With a normal `Future`, you often end up doing:

```java
Future<User> future = executor.submit(() -> getUser());

User user = future.get(); // blocks
```

With `CompletableFuture`, you can describe a pipeline:

```text
getUser()
   ↓
thenApply(...)
   ↓
thenCompose(...)
   ↓
thenCombine(...)
   ↓
handle(...)
```

---

# 1. What is CompletableFuture?

It implements:

```java
Future<T>
```

and also provides a large API for composing asynchronous computations.

Example:

```java
CompletableFuture<String> future =
        CompletableFuture.supplyAsync(() -> "Hello");

System.out.println(future.join());
```

Output:

```text
Hello
```

---

# 2. `runAsync()` vs `supplyAsync()`

This is the first thing to memorize.

### `runAsync()`

Use when there is **no return value**:

```java
CompletableFuture<Void> future =
        CompletableFuture.runAsync(() -> {
            System.out.println("Processing...");
        });
```

Similar conceptually to:

```java
Runnable
```

---

### `supplyAsync()`

Use when you want a result:

```java
CompletableFuture<Integer> future =
        CompletableFuture.supplyAsync(() -> {
            return 10 + 20;
        });
```

Similar conceptually to:

```java
Callable<Integer>
```

So:

```text
runAsync()
    → Runnable
    → no result

supplyAsync()
    → Supplier
    → result
```

---

# 3. Getting the result

You can use:

```java
future.get();
```

or:

```java
future.join();
```

Both wait if necessary.

The major difference:

### `get()`

Can throw checked exceptions such as:

```text
InterruptedException
ExecutionException
```

### `join()`

Wraps exceptional completion in:

```text
CompletionException
```

So:

```java
String result = future.join();
```

often gives cleaner code when you don't want checked exception handling.

---

# 4. Why CompletableFuture is useful

Imagine you need:

```text
1. Fetch user
2. Fetch user's orders
3. Build response
```

You can write:

```java
CompletableFuture
    .supplyAsync(() -> getUser())
    .thenApply(user -> getOrders(user));
```

But there is an important distinction we'll cover shortly:

```java
thenApply()
```

vs

```java
thenCompose()
```

---

# 5. `thenApply()`

`thenApply()` transforms the result.

Think:

```text
T → R
```

Example:

```java
CompletableFuture<String> future =
        CompletableFuture
            .supplyAsync(() -> "java")
            .thenApply(String::toUpperCase);
```

Result:

```text
JAVA
```

Conceptually:

```text
"java"
   ↓
thenApply
   ↓
"JAVA"
```

This is similar to:

```java
Stream.map(...)
```

---

# 6. Multiple transformations

You can chain:

```java
CompletableFuture<Integer> future =
        CompletableFuture
            .supplyAsync(() -> 10)
            .thenApply(x -> x * 2)
            .thenApply(x -> x + 5);
```

Pipeline:

```text
10
 ↓
20
 ↓
25
```

Result:

```text
25
```

---

# 7. `thenCompose()`

This is one of the **most commonly asked CompletableFuture interview questions**.

Suppose:

```java
CompletableFuture<User> getUser()
```

and:

```java
CompletableFuture<List<Order>> getOrders(User user)
```

Notice:

```text
getUser()
    → CompletableFuture<User>

getOrders(user)
    → CompletableFuture<List<Order>>
```

We want:

```text
CompletableFuture<List<Order>>
```

Use:

```java
CompletableFuture<List<Order>> result =
        getUser()
            .thenCompose(user -> getOrders(user));
```

---

# 8. Why not `thenApply()`?

If you do:

```java
getUser()
    .thenApply(user -> getOrders(user));
```

the result becomes:

```text
CompletableFuture<
    CompletableFuture<List<Order>>
>
```

That's nested.

Conceptually:

```text
CompletableFuture<User>
        ↓
thenApply
        ↓
CompletableFuture<CompletableFuture<List<Order>>>
```

`thenCompose()` flattens it:

```text
CompletableFuture<User>
        ↓
thenCompose
        ↓
CompletableFuture<List<Order>>
```

---

# 9. The easiest way to remember

Exactly like Streams:

```text
Stream.map()
       → transform

Stream.flatMap()
       → transform + flatten
```

For CompletableFuture:

```text
thenApply()
       → transform

thenCompose()
       → transform + flatten
```

This analogy is extremely useful in interviews.

---

# 10. `thenAccept()`

Sometimes you don't need to return another value.

Example:

```java
CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenAccept(result -> {
        System.out.println(result);
    });
```

`thenAccept()` consumes the result.

Conceptually:

```text
T → void
```

Similar to:

```java
Consumer<T>
```

---

# 11. `thenRun()`

If you don't care about the previous result:

```java
CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenRun(() -> {
        System.out.println("Finished");
    });
```

`thenRun()` receives no result from the previous stage.

Think:

```text
previous result
      ↓
ignored
      ↓
run next action
```

---

# 12. Summary of common methods

| Method          | Meaning                                 |
| --------------- | --------------------------------------- |
| `supplyAsync()` | Async computation with result           |
| `runAsync()`    | Async computation without result        |
| `thenApply()`   | Transform result                        |
| `thenCompose()` | Chain another async operation / flatten |
| `thenAccept()`  | Consume result                          |
| `thenRun()`     | Run action without previous result      |

---

# 13. Combining independent futures

Suppose:

```text
Fetch user
Fetch products
```

These operations are independent.

You don't want:

```text
fetch user
    ↓
fetch products
```

You want:

```text
        ┌── fetch user ──┐
start ──┤                ├── combine
        └── fetch products┘
```

Use:

```java
CompletableFuture<User> userFuture =
        getUser();

CompletableFuture<List<Product>> productsFuture =
        getProducts();
```

Then:

```java
CompletableFuture<Result> result =
        userFuture.thenCombine(
            productsFuture,
            (user, products) ->
                buildResult(user, products)
        );
```

---

# 14. `thenCombine()`

`thenCombine()` combines the results of **two independent futures**.

Conceptually:

```text
Future<A> ───────┐
                 ├── thenCombine → Future<R>
Future<B> ───────┘
```

Example:

```java
CompletableFuture<Integer> a =
        CompletableFuture.supplyAsync(() -> 10);

CompletableFuture<Integer> b =
        CompletableFuture.supplyAsync(() -> 20);

CompletableFuture<Integer> result =
        a.thenCombine(b, Integer::sum);
```

Result:

```text
30
```

---

# 15. `allOf()`

What if you have many futures?

```java
CompletableFuture<Void> all =
        CompletableFuture.allOf(
            future1,
            future2,
            future3
        );
```

It completes when all supplied futures complete.

Important:

```java
CompletableFuture.allOf(...)
```

returns:

```text
CompletableFuture<Void>
```

It doesn't automatically give you a `List<T>` of results.

You generally collect the results separately.

---

# 16. `anyOf()`

`anyOf()` completes when **any one** of the supplied futures completes.

```java
CompletableFuture<Object> result =
        CompletableFuture.anyOf(
            future1,
            future2,
            future3
        );
```

Useful when you only need the first completion among several alternatives.

---

# 17. Exception handling

Asynchronous code needs explicit exception handling.

Suppose:

```java
CompletableFuture<Integer> future =
        CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("Something failed");
        });
```

You can handle it using:

```java
.exceptionally(...)
```

Example:

```java
CompletableFuture<Integer> future =
        CompletableFuture
            .supplyAsync(() -> {
                throw new RuntimeException("Failed");
            })
            .exceptionally(ex -> {
                return 0;
            });
```

Now the exceptional stage produces:

```text
0
```

---

# 18. `handle()`

`handle()` receives both:

```text
result
exception
```

Example:

```java
CompletableFuture<Integer> future =
        CompletableFuture
            .supplyAsync(() -> 10)
            .handle((result, exception) -> {

                if (exception != null) {
                    return 0;
                }

                return result * 2;
            });
```

So:

```text
handle(result, exception)
```

can deal with both success and failure.

---

# 19. `whenComplete()`

`whenComplete()` is useful when you want to observe completion but generally preserve the existing result/exception rather than transform it.

Example:

```java
CompletableFuture<Integer> future =
        CompletableFuture
            .supplyAsync(() -> 10)
            .whenComplete((result, exception) -> {
                System.out.println("Finished");
            });
```

Think:

```text
whenComplete
    ↓
observe/log/cleanup
    ↓
original outcome continues
```

---

# 20. `exceptionally` vs `handle` vs `whenComplete`

Very common interview question.

| Method          | Receives result | Receives exception | Can transform result |
| --------------- | --------------: | -----------------: | -------------------: |
| `exceptionally` |              No |                Yes |                  Yes |
| `handle`        |             Yes |                Yes |                  Yes |
| `whenComplete`  |             Yes |                Yes |         Generally no |

Mental model:

```text
exceptionally
    → recover from failure

handle
    → transform success OR failure

whenComplete
    → observe completion
```

---

# 21. Example with a real pipeline

Suppose:

```java
CompletableFuture<User> user =
        getUserAsync();
```

Then:

```java
CompletableFuture<UserDto> result =
        user
            .thenApply(this::toDto)
            .exceptionally(ex -> {
                logError(ex);
                return fallbackUser();
            });
```

Pipeline:

```text
getUserAsync()
      ↓
User
      ↓
toDto()
      ↓
UserDto
      ↓
exceptionally()
      ↓
fallback if needed
```

---

# 22. Async vs non-async methods

This is another interview favorite.

You have:

```java
thenApply(...)
```

and:

```java
thenApplyAsync(...)
```

What's the difference?

### `thenApply()`

The continuation may execute in the thread that completes the previous stage, depending on the circumstances.

### `thenApplyAsync()`

Schedules the continuation asynchronously, using the default executor unless you provide one.

Example:

```java
future.thenApplyAsync(value -> process(value));
```

You can also specify an executor:

```java
future.thenApplyAsync(
    value -> process(value),
    executor
);
```

---

# 23. Why provide your own Executor?

Suppose you have:

```java
CompletableFuture
    .supplyAsync(() -> callDatabase(), databaseExecutor)
    .thenApplyAsync(
        result -> process(result),
        processingExecutor
    );
```

Now you explicitly separate workloads.

For example:

```text
databaseExecutor
    ↓
database calls

processingExecutor
    ↓
CPU/data processing
```

This can prevent unrelated workloads from competing for the same executor resources.

---

# 24. Default executor

For many async CompletableFuture methods without an explicitly supplied executor, Java uses the common `ForkJoinPool` for asynchronous execution.

But don't build your architecture around blindly assuming that all stages run there; synchronous continuation methods and explicit executors behave differently.

For production systems, choosing an appropriate executor for important workload isolation is often preferable.

---

# 25. CompletableFuture is not automatically "parallel"

This is a subtle but important point.

This:

```java
future.thenApply(x -> transform(x));
```

doesn't mean you're automatically creating a new thread.

It describes a dependent stage.

Likewise, this:

```java
future.thenApplyAsync(x -> transform(x));
```

does schedule asynchronous execution, but the actual concurrency depends on the executor.

---

# 26. Sequential vs parallel pipelines

Suppose:

```java
CompletableFuture
    .supplyAsync(() -> task1())
    .thenApply(result -> task2(result))
    .thenApply(result -> task3(result));
```

This is a dependency chain:

```text
task1
  ↓
task2
  ↓
task3
```

But:

```java
CompletableFuture<A> a =
        CompletableFuture.supplyAsync(() -> taskA());

CompletableFuture<B> b =
        CompletableFuture.supplyAsync(() -> taskB());

a.thenCombine(b, (x, y) -> combine(x, y));
```

allows:

```text
taskA ──┐
        ├── combine
taskB ──┘
```

This distinction is critical.

---

# 27. `thenCompose` vs `thenCombine`

Remember:

### `thenCompose`

When B **depends on A**:

```text
A
↓
B
```

Example:

```java
getUser()
    .thenCompose(user -> getOrders(user));
```

### `thenCombine`

When A and B are **independent**:

```text
A ──┐
    ├── C
B ──┘
```

Example:

```java
userFuture.thenCombine(
    productsFuture,
    this::buildResponse
);
```

---

# 28. CompletableFuture and `Future`

### Future

```java
Future<Result> future =
        executor.submit(task);

Result result = future.get();
```

Primarily gives you a handle to an asynchronous result.

### CompletableFuture

```java
CompletableFuture<Result> future =
        CompletableFuture
            .supplyAsync(task)
            .thenApply(...)
            .thenCompose(...)
            .exceptionally(...);
```

Allows you to construct an asynchronous pipeline.

So:

> **Future is primarily about retrieving an async result; CompletableFuture is about composing async computations.**

---

# 29. CompletableFuture can be completed manually

The name "Completable" is important.

You can create:

```java
CompletableFuture<String> future =
        new CompletableFuture<>();
```

Then somewhere else:

```java
future.complete("Hello");
```

Or exceptionally:

```java
future.completeExceptionally(
    new RuntimeException("Failed")
);
```

This can be useful when integrating callback-based or event-driven APIs.

---

# 30. `complete()` only completes once

Suppose:

```java
future.complete("A");
future.complete("B");
```

The first successful completion wins.

Result remains:

```text
A
```

There is also:

```java
completeExceptionally(...)
```

for exceptional completion.

---

# 31. Common interview example

Suppose you have:

```java
CompletableFuture<User> userFuture =
        getUser();

CompletableFuture<List<Order>> ordersFuture =
        getOrders();

CompletableFuture<Dashboard> dashboard =
        userFuture.thenCombine(
            ordersFuture,
            Dashboard::new
        );
```

What happens?

```text
          getUser()
             ↓
        User Future
             │
             ├────────────┐
             │            │
             │            ↓
             │        combine
             │            ↑
             ↓            │
       Orders Future ─────┘
             ↓
        Dashboard
```

The two independent operations can proceed concurrently.

---

# 32. Common mistakes

### Mistake 1: Blocking immediately

```java
User user = getUserAsync().join();

Order order = getOrderAsync(user).join();
```

You've introduced synchronous waiting.

Sometimes that's necessary, but if your goal is an asynchronous pipeline, prefer composition.

---

### Mistake 2: Using `thenApply` for another Future

```java
future.thenApply(x -> getSomethingAsync(x));
```

This creates:

```text
CompletableFuture<CompletableFuture<T>>
```

Usually you want:

```java
thenCompose(...)
```

---

### Mistake 3: Assuming `thenApplyAsync` always means better performance

Async scheduling has overhead.

Use asynchronous boundaries deliberately, especially when they provide useful concurrency or prevent blocking work from occupying inappropriate threads.

---

# 33. Interview cheat sheet

Memorize this table:

| Method           | Purpose                                    |
| ---------------- | ------------------------------------------ |
| `runAsync`       | async task, no result                      |
| `supplyAsync`    | async task with result                     |
| `thenApply`      | transform result                           |
| `thenApplyAsync` | asynchronously transform result            |
| `thenCompose`    | chain dependent async operation            |
| `thenAccept`     | consume result                             |
| `thenRun`        | run action, ignore result                  |
| `thenCombine`    | combine two independent futures            |
| `allOf`          | wait for all                               |
| `anyOf`          | complete when any completes                |
| `exceptionally`  | recover from exception                     |
| `handle`         | transform success/failure                  |
| `whenComplete`   | observe completion                         |
| `join`           | get result, unchecked completion exception |
| `get`            | get result, checked exceptions             |

---

# 34. The five distinctions I'd expect in an EPAM interview

### 1. `thenApply` vs `thenCompose`

```text
thenApply   → T → R
thenCompose → T → CompletableFuture<R>
              + flatten
```

### 2. `thenCompose` vs `thenCombine`

```text
Compose → dependent operations

Combine → independent operations
```

### 3. `get()` vs `join()`

```text
get()  → checked exceptions
join() → CompletionException
```

### 4. `thenApply()` vs `thenApplyAsync()`

```text
thenApply()
    → continuation may execute in completing thread

thenApplyAsync()
    → asynchronous scheduling via executor
```

### 5. `Future` vs `CompletableFuture`

```text
Future
    → represents async result

CompletableFuture
    → represents + composes async computations
```

---

## One final mental picture

```text
                CompletableFuture
                       │
       ┌───────────────┼────────────────┐
       ↓               ↓                ↓
   supplyAsync     runAsync         completedFuture
       │
       ↓
   thenApply
       │
       ↓
   thenCompose ───────→ another async operation
       │
       ↓
   thenCombine ←────── another independent future
       │
       ↓
   exceptionally / handle
       │
       ↓
     Result
```

### Next topic

**Serialization & Deserialization** — `Serializable`, `serialVersionUID`, `transient`, `Externalizable`, serialization pitfalls, and why serialization is often avoided in modern Java applications.

