# Chapter 5 — Asynchronous JavaScript

Asynchronous JavaScript is one of the **most important JavaScript interview topics**.

You should be able to explain not only *how* `Promise` and `async/await` work, but also:

* Call Stack
* Web APIs / runtime APIs
* Callback Queue
* Microtask Queue
* Event Loop
* Promises
* `async/await`
* `setTimeout`
* Execution order
* Error handling
* Promise chaining
* Common interview output questions

---

# 1. Why Do We Need Asynchronous JavaScript?

JavaScript executes code **synchronously by default**.

Consider:

```js
console.log("A");

const result = expensiveOperation();

console.log("B");
```

Execution:

```text
A
|
| expensiveOperation()
|     ↓
|  takes 5 seconds
|
B
```

The problem is that while `expensiveOperation()` is running, JavaScript cannot continue to the next statement on the same thread.

This is called **blocking**.

In real applications, we frequently perform operations that take time:

```text
HTTP request
Database query
File reading
Timer
User interaction
Network request
```

We don't want JavaScript to stop executing everything while waiting.

So asynchronous programming allows us to say:

> "Start this operation. Don't wait here. Continue executing other work. Tell me when the operation is finished."

---

# 2. JavaScript Is Single-Threaded

A very important interview statement:

> JavaScript execution is traditionally single-threaded: one JavaScript execution thread has one call stack.

For example:

```js
console.log("A");
console.log("B");
console.log("C");
```

There is one execution flow:

```text
Call Stack

┌─────────────┐
│ console.log │
├─────────────┤
│ console.log │
├─────────────┤
│ console.log │
└─────────────┘
```

JavaScript doesn't normally execute:

```text
A
B
C
```

simultaneously on the same JS thread.

But then how can JavaScript handle:

```js
setTimeout(...)
fetch(...)
Promise(...)
```

without blocking?

That's where the **JavaScript runtime** and **event loop** come in.

---

# 3. JavaScript Engine vs JavaScript Runtime

This distinction is extremely important.

## JavaScript Engine

The engine executes JavaScript.

Examples:

```text
Chrome       → V8
Node.js      → V8
Firefox      → SpiderMonkey
Safari       → JavaScriptCore
```

The engine contains things such as:

```text
Parser
Execution machinery
Call Stack
Heap
JIT compiler
Garbage collector
```

But things like:

```text
setTimeout()
fetch()
DOM
File APIs
network APIs
```

are generally provided by the **host environment/runtime**, not by the ECMAScript language itself.

---

# 4. Browser Runtime

Conceptually:

```text
┌─────────────────────────────────────┐
│             Browser                 │
│                                     │
│   ┌─────────────────────────────┐   │
│   │      JavaScript Engine      │   │
│   │                             │   │
│   │      Call Stack             │   │
│   │      Heap                   │   │
│   └─────────────────────────────┘   │
│                                     │
│   Browser Web APIs                  │
│   ├── setTimeout                    │
│   ├── fetch                         │
│   ├── DOM                           │
│   ├── Events                        │
│   └── etc.                          │
│                                     │
│   Event Loop + Queues               │
│                                     │
└─────────────────────────────────────┘
```

Node.js has a different runtime implementation, but the conceptual model is similar.

---

# 5. The Call Stack

The **call stack** keeps track of currently executing JavaScript functions.

Example:

```js
function one() {
    two();
}

function two() {
    three();
}

function three() {
    console.log("Hello");
}

one();
```

Execution:

```text
one()
 ↓
two()
 ↓
three()
 ↓
console.log()
```

The stack looks like:

```text
┌──────────────┐
│ console.log  │
├──────────────┤
│ three        │
├──────────────┤
│ two          │
├──────────────┤
│ one          │
└──────────────┘
```

JavaScript executes the function at the top.

When it finishes:

```text
pop
```

and execution returns to the previous function.

---

# 6. What Happens With `setTimeout`?

Consider:

```js
console.log("A");

setTimeout(() => {
    console.log("B");
}, 2000);

console.log("C");
```

Many beginners think:

```text
A
(wait 2 seconds)
B
C
```

That's incorrect.

Actual output:

```text
A
C
B
```

Why?

Let's walk through it.

---

## Step 1

```js
console.log("A");
```

Call stack:

```text
console.log("A")
```

Output:

```text
A
```

Then it leaves the stack.

---

## Step 2

```js
setTimeout(() => {
    console.log("B");
}, 2000);
```

The timer is handled by the runtime.

Conceptually:

```text
JavaScript
    |
    | setTimeout(...)
    ↓
Runtime Timer API
    |
    | wait 2000ms
    ↓
Queue callback
```

JavaScript does **not** sit there for two seconds.

---

## Step 3

JavaScript continues:

```js
console.log("C");
```

Output:

```text
C
```

---

## Step 4

After approximately 2 seconds, the timer callback becomes eligible to run.

It doesn't mean:

> "Run immediately."

It means:

> "The timer has completed; the callback can now be queued for execution."

Eventually:

```js
() => console.log("B")
```

gets onto the appropriate task queue.

The event loop eventually allows it onto the call stack.

Then:

```text
B
```

is printed.

---

# 7. The Event Loop

The event loop is responsible for coordinating:

```text
Call Stack
Queues
Runtime APIs
```

Conceptually:

```text
                JavaScript
                    │
                    ▼
              ┌───────────┐
              │Call Stack │
              └─────┬─────┘
                    │
                    │ empty?
                    ▼
              ┌───────────┐
              │Event Loop │
              └─────┬─────┘
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
   Microtask Queue       Task Queue
   ┌──────────────┐     ┌──────────────┐
   │ Promise.then │     │ setTimeout   │
   │ queueMicrotask│    │ events       │
   └──────────────┘     └──────────────┘
```

The exact scheduling rules are more nuanced than this diagram, but this is an excellent interview mental model.

---

# 8. Task Queue / Macrotask Queue

Callbacks from APIs such as:

```js
setTimeout(...)
setInterval(...)
```

are commonly discussed as **tasks** (often informally called macrotasks).

Example:

```js
setTimeout(() => {
    console.log("Hello");
}, 0);
```

Even with:

```js
0
```

it does **not** mean:

> Execute immediately.

It means roughly:

> Schedule this callback so it can run after the timer's minimum delay has elapsed and the runtime's scheduling rules permit it.

Therefore:

```js
console.log("A");

setTimeout(() => {
    console.log("B");
}, 0);

console.log("C");
```

Output:

```text
A
C
B
```

---

# 9. Why Does `setTimeout(..., 0)` Not Run Immediately?

This is a very common interview question.

Consider:

```js
console.log("A");

setTimeout(() => {
    console.log("B");
}, 0);

console.log("C");
```

The callback cannot execute while the current JavaScript execution is still running.

So:

```text
A
C
```

finish first.

Then the timer callback gets its opportunity to execute.

Therefore:

```text
A
C
B
```

---

# 10. Promises

A `Promise` represents the eventual result of an asynchronous operation.

It has three states:

```text
             ┌──────────────┐
             │   PENDING    │
             └──────┬───────┘
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
     FULFILLED             REJECTED
```

Example:

```js
const promise = new Promise((resolve, reject) => {

    // asynchronous operation

    resolve("Success");
});
```

States:

```text
pending
   ↓
fulfilled
```

Or:

```text
pending
   ↓
rejected
```

A Promise settles only once.

---

# 11. `resolve()` and `reject()`

Example:

```js
const promise = new Promise((resolve, reject) => {

    const success = true;

    if (success) {
        resolve("Operation successful");
    } else {
        reject("Operation failed");
    }
});
```

Then:

```js
promise
    .then(result => {
        console.log(result);
    })
    .catch(error => {
        console.error(error);
    });
```

`resolve()` means:

```text
Promise → fulfilled
```

`reject()` means:

```text
Promise → rejected
```

---

# 12. Promise Callbacks Are Microtasks

This is one of the **most important interview facts**.

Consider:

```js
console.log("A");

Promise.resolve().then(() => {
    console.log("B");
});

console.log("C");
```

Output:

```text
A
C
B
```

Why?

The callback passed to `.then()` is scheduled as a **microtask**.

Conceptually:

```text
Call Stack
    |
    | Promise.resolve()
    ↓
Microtask Queue
    |
    | callback
    ↓
Event Loop
    |
    ↓
Call Stack
```

---

# 13. Microtask Queue

Common sources include:

```js
Promise.then()
Promise.catch()
Promise.finally()
queueMicrotask()
```

Microtasks have special priority.

After the current synchronous JavaScript execution finishes, the runtime processes pending microtasks before moving on to the next task in the normal event-loop cycle.

Simplified:

```text
Execute current task
        ↓
Drain microtask queue
        ↓
Next task
        ↓
Drain microtask queue
        ↓
Next task
```

This is why Promise callbacks generally execute before a timer callback that is already eligible.

---

# 14. The Most Important Interview Example

Consider:

```js
console.log("A");

setTimeout(() => {
    console.log("B");
}, 0);

Promise.resolve().then(() => {
    console.log("C");
});

console.log("D");
```

What is the output?

Answer:

```text
A
D
C
B
```

Let's understand it.

---

## Step 1

```js
console.log("A");
```

Output:

```text
A
```

---

## Step 2

```js
setTimeout(..., 0);
```

Timer callback is scheduled as a task.

```text
Task Queue
──────────
B callback
```

---

## Step 3

```js
Promise.resolve().then(...);
```

The `.then()` callback becomes a microtask.

```text
Microtask Queue
───────────────
C callback
```

---

## Step 4

```js
console.log("D");
```

Output:

```text
D
```

The current synchronous execution has finished.

---

## Step 5

Microtasks are processed first:

```text
C
```

---

## Step 6

Then the timer task runs:

```text
B
```

Final:

```text
A
D
C
B
```

---

# 15. The Golden Rule

For interview purposes, remember:

```text
Synchronous code
      ↓
Microtasks
      ↓
Next task
```

So:

```js
console.log("1");

setTimeout(() => console.log("2"), 0);

Promise.resolve().then(() => console.log("3"));

console.log("4");
```

Output:

```text
1
4
3
2
```

---

# 16. `async` Functions

An `async` function always returns a Promise.

Example:

```js
async function getUser() {
    return "John";
}
```

You can think of:

```js
return "John";
```

as producing a fulfilled Promise containing `"John"`.

Therefore:

```js
const result = getUser();

console.log(result);
```

prints something conceptually like:

```text
Promise { "John" }
```

not:

```text
John
```

---

# 17. `await`

`await` is used inside an `async` function to wait for a Promise's settlement.

Example:

```js
async function getUser() {

    const user = await fetchUser();

    console.log(user);
}
```

A common misconception is:

> `await` blocks JavaScript.

It does **not** block the JavaScript thread in the same way as a synchronous blocking operation.

Instead, the async function's execution is suspended at that `await`, allowing other work to proceed, and it resumes when the awaited Promise settles.

Conceptually:

```text
async function
      │
      ▼
    await
      │
      ├──── suspend this async function
      │
      ▼
 other JavaScript can execute
      │
      ▼
 Promise settles
      │
      ▼
 async function resumes
```

---

# 18. Example of `await`

```js
async function example() {
    console.log("A");

    await Promise.resolve();

    console.log("B");
}

example();

console.log("C");
```

Output:

```text
A
C
B
```

Why?

Execution:

```text
example()
   ↓
"A"
   ↓
await Promise.resolve()
   ↓
pause async function
   ↓
continue outside
   ↓
"C"
   ↓
microtask resumes example()
   ↓
"B"
```

---

# 19. `async/await` Is Built on Promises

This is an important interview answer.

When asked:

> Is `async/await` different from Promises?

Answer:

> `async/await` is syntax built around Promise-based asynchronous operations. An `async` function returns a Promise, and `await` pauses that async function's continuation until the awaited Promise settles.

It makes asynchronous code easier to read.

Promise style:

```js
fetchUser()
    .then(user => fetchOrders(user.id))
    .then(orders => {
        console.log(orders);
    })
    .catch(error => {
        console.error(error);
    });
```

`async/await`:

```js
async function loadOrders() {
    try {
        const user = await fetchUser();
        const orders = await fetchOrders(user.id);

        console.log(orders);
    } catch (error) {
        console.error(error);
    }
}
```

---

# 20. Promise Chaining

This is very important.

```js
fetchUser()
    .then(user => {
        return fetchOrders(user.id);
    })
    .then(orders => {
        console.log(orders);
    });
```

The key concept:

> `.then()` returns a new Promise.

So:

```text
Promise A
   │
   ▼
.then()
   │
   ▼
Promise B
   │
   ▼
.then()
   │
   ▼
Promise C
```

This allows chaining.

---

# 21. Returning vs Not Returning in `.then()`

Consider:

```js
fetchUser()
    .then(user => {
        fetchOrders(user.id);
    })
    .then(orders => {
        console.log(orders);
    });
```

This is probably a bug.

Why?

Because:

```js
fetchOrders(user.id);
```

wasn't returned.

Correct:

```js
fetchUser()
    .then(user => {
        return fetchOrders(user.id);
    })
    .then(orders => {
        console.log(orders);
    });
```

Or concise:

```js
fetchUser()
    .then(user => fetchOrders(user.id))
    .then(orders => console.log(orders));
```

---

# 22. Error Handling With Promises

You can use:

```js
.catch()
```

Example:

```js
fetchUser()
    .then(user => fetchOrders(user.id))
    .then(orders => {
        console.log(orders);
    })
    .catch(error => {
        console.error(error);
    });
```

Errors/rejections propagate down the chain until handled.

Conceptually:

```text
Promise
   │
   ▼
.then()
   │
   ▼
.then()
   │
   X error
   │
   ▼
.catch()
```

---

# 23. Error Handling With `async/await`

Use:

```js
try/catch
```

Example:

```js
async function loadData() {
    try {
        const user = await fetchUser();
        const orders = await fetchOrders(user.id);

        return orders;
    } catch (error) {
        console.error(error);
    }
}
```

---

# 24. `Promise.all()`

Suppose you need three independent operations:

```js
const users = await getUsers();
const products = await getProducts();
const orders = await getOrders();
```

This executes them sequentially.

If each takes:

```text
getUsers     2 sec
getProducts  3 sec
getOrders    2 sec
```

Total could be approximately:

```text
7 seconds
```

if they truly execute sequentially.

If they are independent, you can start them together:

```js
const [users, products, orders] = await Promise.all([
    getUsers(),
    getProducts(),
    getOrders()
]);
```

Conceptually:

```text
getUsers       ────── 2s ───┐
getProducts    ───────── 3s ├──→ results
getOrders      ────── 2s ───┘
```

Total roughly:

```text
3 seconds
```

rather than:

```text
7 seconds
```

---

# 25. `Promise.all()` Failure Behavior

Important interview question.

```js
await Promise.all([
    operation1(),
    operation2(),
    operation3()
]);
```

If one Promise rejects:

```text
Promise 1 → fulfilled
Promise 2 → rejected
Promise 3 → fulfilled
```

`Promise.all()` rejects.

It is commonly described as **fail-fast**: its returned Promise rejects when an input Promise rejects.

It does **not** magically cancel the other underlying operations.

That distinction is important.

---

# 26. Promise Combinators

Know these four:

| Method                 | Behavior                                   |
| ---------------------- | ------------------------------------------ |
| `Promise.all()`        | All must fulfill; rejects when one rejects |
| `Promise.allSettled()` | Waits for all, gives status of each        |
| `Promise.race()`       | Settles when first input settles           |
| `Promise.any()`        | Fulfills when first input fulfills         |

### `Promise.all`

```text
A ── fulfilled
B ── fulfilled
C ── fulfilled

        ↓

fulfilled [A, B, C]
```

If one rejects:

```text
A ── fulfilled
B ── rejected
C ── fulfilled

        ↓

rejected
```

### `Promise.allSettled`

```js
const results = await Promise.allSettled([
    task1(),
    task2(),
    task3()
]);
```

You get results like:

```js
[
    { status: "fulfilled", value: ... },
    { status: "rejected", reason: ... },
    { status: "fulfilled", value: ... }
]
```

Useful when you want **every result regardless of individual failures**.

---

# 27. Sequential vs Parallel Async Operations

This is an important real-world design question.

### Sequential

```js
const a = await taskA();
const b = await taskB();
const c = await taskC();
```

Use when:

```text
B depends on A
C depends on B
```

Example:

```text
Get user
   ↓
Get user's orders
   ↓
Get order details
```

### Concurrent

```js
const [a, b, c] = await Promise.all([
    taskA(),
    taskB(),
    taskC()
]);
```

Use when:

```text
A
B
C
```

are independent.

---

# 28. A Critical Interview Distinction

Don't say:

> JavaScript is asynchronous.

That is incomplete.

Better:

> JavaScript execution itself is primarily single-threaded and synchronous by default. Asynchronous behavior is provided through the host runtime, APIs, queues, and event loop, with Promises providing a standard abstraction for asynchronous results.

That's a much stronger interview answer.

---

# 29. Complete Mental Model

Consider:

```js
console.log("A");

setTimeout(() => {
    console.log("B");
}, 0);

Promise.resolve().then(() => {
    console.log("C");
});

console.log("D");
```

Think:

```text
                 JavaScript Runtime
                        │
                        ▼
                 ┌─────────────┐
                 │ Call Stack  │
                 └──────┬──────┘
                        │
             ┌──────────┴──────────┐
             │                     │
             ▼                     ▼
       Timer / Runtime       Promise machinery
             │                     │
             ▼                     ▼
        Task Queue           Microtask Queue
             │                     │
             └──────────┬──────────┘
                        ▼
                    Event Loop
                        │
                        ▼
                   Call Stack
```

Execution:

```text
Synchronous:
A
D

Then microtasks:
C

Then task:
B
```

Result:

```text
A
D
C
B
```

---

# 30. Common Interview Output Questions

### Question 1

```js
console.log(1);

setTimeout(() => {
    console.log(2);
}, 0);

console.log(3);
```

Output:

```text
1
3
2
```

---

### Question 2

```js
console.log(1);

Promise.resolve().then(() => {
    console.log(2);
});

console.log(3);
```

Output:

```text
1
3
2
```

---

### Question 3

```js
console.log(1);

setTimeout(() => {
    console.log(2);
}, 0);

Promise.resolve().then(() => {
    console.log(3);
});

console.log(4);
```

Output:

```text
1
4
3
2
```

---

### Question 4

```js
async function test() {
    console.log("A");

    await Promise.resolve();

    console.log("B");
}

test();

console.log("C");
```

Output:

```text
A
C
B
```

---

### Question 5

```js
Promise.resolve()
    .then(() => {
        console.log("A");
    })
    .then(() => {
        console.log("B");
    });

console.log("C");
```

Output:

```text
C
A
B
```

Why?

The first `.then()` is a microtask.

When it completes, its returned Promise settles and schedules the next `.then()` as another microtask.

```text
Synchronous
   ↓
C
   ↓
Microtask 1
   ↓
A
   ↓
Microtask 2
   ↓
B
```

---

# 31. Common Misconceptions

### ❌ `setTimeout(fn, 0)` means execute immediately

No.

It means the callback becomes eligible after the timer delay and scheduling rules allow it.

---

### ❌ `await` blocks JavaScript

No.

It suspends the current async function's continuation.

Other work can continue.

---

### ❌ Promise means another thread

No.

A Promise is an abstraction representing an eventual result.

It doesn't itself create a thread.

---

### ❌ JavaScript is multithreaded because of async

No.

Async programming and multithreading are different concepts.

---

### ❌ `Promise.all()` runs things on separate threads

No.

It allows multiple asynchronous operations to be in progress concurrently. The actual underlying work depends on the runtime/API.

---

### ❌ `async` makes a function execute asynchronously from the first line

Not exactly.

An `async` function starts executing synchronously until it reaches an `await` (or otherwise returns/throws).

Example:

```js
async function test() {
    console.log("A");

    await something();

    console.log("B");
}
```

`A` is executed when `test()` is called, before the function suspends at `await`.

---

# 32. Interview Cheat Sheet

```text
JavaScript
│
├── Single-threaded execution
│
├── Call Stack
│
├── Host Runtime
│   ├── Timers
│   ├── Network APIs
│   ├── DOM APIs
│   └── Other platform APIs
│
├── Event Loop
│
├── Task Queue
│   └── timers/events/etc.
│
└── Microtask Queue
    ├── Promise callbacks
    └── queueMicrotask()
```

### Key rules

```text
Synchronous code
      ↓
Microtasks
      ↓
Next task
```

### Promises

```text
pending
   ↓
fulfilled
```

or

```text
pending
   ↓
rejected
```

### `async`

```text
async function → always returns Promise
```

### `await`

```text
await Promise
      ↓
suspend current async function
      ↓
Promise settles
      ↓
resume later
```

### Promise combinators

```text
all         → all must fulfill
allSettled  → wait for all
race        → first to settle
any         → first to fulfill
```

### Most important interview statement

> **JavaScript execution is primarily single-threaded. Asynchronous behavior is coordinated by the host runtime, queues, and event loop. Promise callbacks are microtasks and are processed before moving to the next task once the current synchronous execution completes.**

---

## Chapter 6 — JavaScript Execution & Memory

Next we'll connect everything we've learned to **how JavaScript actually executes code**, including:

* Execution Context
* Global Execution Context
* Function Execution Context
* Creation Phase vs Execution Phase
* Lexical Environment
* Environment Records
* Scope Chain
* Call Stack in depth
* Heap vs Stack
* Closures and memory
* Garbage Collection
* Memory leaks
* Why closures can keep objects alive
* Interview execution-trace questions

