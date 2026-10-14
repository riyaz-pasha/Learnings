# Chapter 2 — Build Debounce from Scratch

In this chapter, we'll write a debounce implementation without memorizing a template. We'll derive every line from the behavior we want.

By the end, you should be able to start with an empty function and figure out what to write next.

## 1. Start with the behavior

Suppose we have a function that searches for products.

```
function searchProducts(query) {
    console.log("Searching for:", query);
}
```

Normally, calling it executes immediately:

```
searchProducts("jav");
searchProducts("java");
searchProducts("javascript");
```

Output:

```
Searching for: jav
Searching for: java
Searching for: javascript
```

Now imagine we want to execute the search only when no new call has occurred for 500 milliseconds.

```
const debouncedSearch = debounce(searchProducts, 500);

debouncedSearch("jav");
debouncedSearch("java");
debouncedSearch("javascript");
```

If all three calls happen within 500 milliseconds of one another, we want:

```
Searching for: javascript
```

Notice the intended behavior: we delay the execution, and every new call cancels the previous scheduled execution.

Let's derive the implementation.

## 2. Step 1: We know how to delay a function

JavaScript already provides `setTimeout`.

```
setTimeout(() => {
    searchProducts("javascript");
}, 500);
```

This schedules a callback to execute after approximately 500 milliseconds.

But there's a problem. If we schedule one timeout per call, we still execute all three searches:

```
function searchProductsDebounced(query) {
    setTimeout(() => {
        searchProducts(query);
    }, 500);
}
```

Calling it three times creates three independent timers.

```
Call "jav"        → Timer A ──────────────► Search "jav"
Call "java"       → Timer B ──────────────► Search "java"
Call "javascript" → Timer C ──────────────► Search "javascript"
```

Delaying execution is not enough. We must cancel the previous scheduled execution.

## 3. Step 2: Learn to cancel a scheduled execution

The browser and Node.js provide `clearTimeout`.

```
const timerId = setTimeout(() => {
    console.log("Executed");
}, 500);

clearTimeout(timerId);
```

The timeout ID identifies the scheduled timer, allowing us to cancel it before its callback starts executing.

So, for debounce, we need two operations:

1. Cancel the previous timer.
2. Create a new timer.

The logic looks like this:

```
clearTimeout(previousTimer);

previousTimer = setTimeout(() => {
    // Execute the actual function
}, 500);
```

But where should `previousTimer` live?

That question leads us to the most important part of this implementation.

## 4. Step 3: The timer must survive between calls

Consider this implementation:

```
function debounce(callback, delay) {
    return function (...args) {
        let timerId;

        clearTimeout(timerId);

        timerId = setTimeout(() => {
            callback(...args);
        }, delay);
    };
}
```

It looks reasonable at first glance. However, it is broken.

Why?

Look carefully at this line:

```
let timerId;
```

It is inside the returned function. Every time the returned function is called, a new local `timerId` variable is created.

Let's trace the calls.

### What happens with the incorrect scope?

First call

Create local `timerId` → schedule Timer A → lose access to it when the call finishes

Second call

Create a different local `timerId` → it starts as `undefined` → Timer A isn't cancelled → schedule Timer B

Result

Both timers execute. Debouncing fails.

The problem is not with `clearTimeout`. It is with the variable's scope and lifetime.

For debounce to work, the next invocation must be able to access the previous invocation's timer ID.

### Where should we declare it?

Move the variable one level up:

```
function debounce(callback, delay) {
    let timerId;

    return function (...args) {
        clearTimeout(timerId);

        timerId = setTimeout(() => {
            callback(...args);
        }, delay);
    };
}
```

Now, `timerId` is created when `debounce()` is called, not every time the returned function is called.

Every call to the returned function shares that same variable.

This is where closures become essential.

## 5. Step 4: Understand the closure

Look at the function structure:

```
function debounce(callback, delay) {
    let timerId;

    return function (...args) {
        clearTimeout(timerId);

        timerId = setTimeout(() => {
            callback(...args);
        }, delay);
    };
}
```

There are two different function calls to understand.

First:

```
const debouncedSearch = debounce(searchProducts, 500);
```

This calls `debounce()` once. It creates `timerId` and returns an inner function.

Later:

```
debouncedSearch("java");
debouncedSearch("javascript");
```

Each invocation calls the returned function. That function still has access to the `timerId` declared in its outer scope, even though the original `debounce()` call has already finished.

That's a closure: the returned function retains access to variables from the lexical scope in which it was created.

Conceptually:

```
debounce(searchProducts, 500)
│
├── callback = searchProducts
├── delay = 500
├── timerId = current timeout ID
│
└── returns a function
    │
    ├── Call 1: clear old timer → create new timer
    ├── Call 2: clear old timer → create new timer
    └── Call 3: clear old timer → create new timer

        All calls share the same timerId variable.
```

One important detail: each call to `debounce()` creates a separate closure. Consequently, separate debounced functions have separate timers:

```
const debouncedSearch = debounce(searchProducts, 500);
const debouncedSave = debounce(saveDocument, 500);
```

The search timer and save timer do not interfere with each other.

Remember the reasoning: the timer must live in a scope that is shared between invocations of the returned function, but separate for each debounced function.

## 6. Step 5: Why do we use `...args`?

Our function can now cancel previous timers, but we must also pass the arguments to the original function.

Suppose:

```
function searchProducts(query, category) {
    console.log(query, category);
}
```

We might call:

```
debouncedSearch("laptop", "electronics");
```

How can our returned function accept any number of arguments and pass them to the original function later?

Use a rest parameter:

```
return function (...args) {
    // args is an array containing all arguments
};
```

For the call:

```
debouncedSearch("laptop", "electronics");
```

`args` contains:

```
["laptop", "electronics"]
```

Then we pass those arguments to the callback when its timer fires:

```
timerId = setTimeout(() => {
    callback(...args);
}, delay);
```

Why does this work?

Each invocation creates its own `args` binding. When we cancel the old timeout and schedule a new one, the new callback closes over the arguments supplied in the latest invocation.

Therefore, assuming the earlier timer has not already begun executing, the latest scheduled callback receives the latest arguments.

## 7. Step 6: The complete implementation

Let's combine the pieces we've derived.

```

function debounce(callback, delay) {
    let timerId;

    return function (...args) {
        clearTimeout(timerId);

        timerId = setTimeout(() => {
            callback.apply(this, args);
        }, delay);
    };
}

```

This is a reusable trailing-edge debounce implementation.

Let's understand the last detail: `this`.

### Why `callback.apply(this, args)`?

Consider an object method:

```
const productSearch = {
    category: "electronics",

    search(query) {
        console.log(this.category, query);
    }
};
```

We might want to debounce its method:

```
productSearch.debouncedSearch = debounce(
    productSearch.search,
    500
);
```

If the debounced function is called as an object method, it receives the object as `this`.

```
productSearch.debouncedSearch("laptop");
```

The returned function is a normal function, so its `this` depends on how it is called. The arrow function inside `setTimeout` captures that invocation's `this`.

Using:

```
callback.apply(this, args);
```

preserves the receiver from the latest invocation of the debounced function.

By contrast:

```
callback(...args);
```

does not preserve that receiver. It may be perfectly adequate when your callback does not depend on `this`, but `apply` makes the utility more general.

One caveat: extracting an object method and passing it to `debounce` does not bind the method to its original object automatically. The `this` preserved above is the `this` supplied when the returned debounced function is called.

## 8. Test the implementation yourself

Try this code in your browser's developer console or in Node.js.

```
function debounce(callback, delay) {
    let timerId;

    return function (...args) {
        clearTimeout(timerId);

        timerId = setTimeout(() => {
            callback.apply(this, args);
        }, delay);
    };
}

function searchProducts(query) {
    console.log(`${Date.now()}: Searching for "${query}"`);
}

const debouncedSearch = debounce(searchProducts, 500);

debouncedSearch("jav");

setTimeout(() => {
    debouncedSearch("java");
}, 200);

setTimeout(() => {
    debouncedSearch("javascript");
}, 400);
```

The scheduled calls occur at approximately `0 ms`, `200 ms`, and `400 ms`.

### Trace the timers

### 0 ms

Call with `"jav"`

Schedule execution for approximately `500 ms`.

### 200 ms

Call with `"java"`

Cancel the earlier timer and schedule execution for approximately `700 ms`.

### 400 ms

Call with `"javascript"`

Cancel the earlier timer and schedule execution for approximately `900 ms`.

### 900 ms

Execute once

`Searching for "javascript"`

These times are approximate because JavaScript timers can run later than their requested delay.

## 9. Practice: derive it without looking

Now let's check whether you understand the ideas behind the code.

Question 1 — Scope

Why is `let timerId` declared outside the returned function?

So that all debounced functions in the program share one timer

So successive invocations of this particular debounced function can access the same timer ID

Because `clearTimeout` cannot use local variables

Correct. The closure shares the timer between calls, while each debounce invocation has its own timer variable.

Question 2 — Removing `clearTimeout`

What happens if you remove `clearTimeout(timerId)`?

The last invocation still runs only once

Each call schedules its own callback, so multiple executions can occur

The original callback runs immediately

Correct. Without cancellation, each call can leave a pending timer behind.

Question 3 — Latest arguments

Calls arrive at `0 ms` with `"A"` and `300 ms` with `"B"`. The delay is `500 ms`, and no more calls arrive. What does the callback receive?

Both `"A"` and `"B"`

`"A"` at 500 ms

`"B"` at approximately 800 ms

Correct. The first timer is cancelled; the second call starts a fresh 500 ms delay.

Question 4 — Code design

If you want to create two independent debounced functions with different callbacks, where should their timer IDs live?

In a single global variable

Inside each invocation of the returned function

In the outer `debounce` invocation, before returning the inner function

Correct. Each call to debounce creates its own closure and timer state.

## 10. What you should understand before moving on

The entire implementation follows from four decisions:

1. We need a timer because execution must be delayed.
2. We need a timer ID because a new call must cancel the old schedule.
3. We put the timer variable in the outer function's scope because successive calls need shared state.
4. We return a function because the caller needs a reusable version of the original callback with this scheduling behavior.

The rest—arguments, `this`, and timer behavior—builds on these four ideas.

This implementation is a good foundation, but it does not yet provide features such as a `cancel()` method or a `flush()` method to execute pending work immediately. We'll discuss production-level refinements later.

Next chapter: we'll derive throttle from scratch. Instead of cancelling and restarting a timer on each call, we'll reason about how to decide whether enough time has passed to allow another execution.

