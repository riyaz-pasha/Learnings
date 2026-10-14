# Chapter 3 — Build Throttle from Scratch

In Chapter 2, we built debounce by sharing a timer variable across function calls and cancelling the previous timer whenever another call arrived.

Throttle solves a different problem. This time, we'll start with timestamps, derive a minimal implementation, and then see why timers are useful when we want to preserve the latest event.

## 1. Start with the behavior

Imagine you're tracking the user's scroll position.

```
function updateScrollProgress() {
    console.log("Updating scroll progress");
}
```

Suppose scroll events occur 100 times in one second. We don't want to run this function 100 times.

Let's say we want to execute it at most once every 200 milliseconds, while still responding during scrolling.

Unlike debounce, we don't want to keep postponing execution until scrolling stops.

For example:

```
Time (ms) ─────────────────────────────────────────►

Events:     A  B  C  D  E  F  G  H  I  J  K  L
            │  │  │  │  │  │  │  │  │  │  │  │

Execute:    ✓              ✓              ✓
            0              200            400
```

This is the behavior we want to derive.

## 2. Step 1: What information do we need?

Think about the question our code needs to answer on every call:

> Has enough time passed since the function last executed?

To answer that, we only need to remember one thing: the time of the last execution.

JavaScript provides `Date.now()`, which returns the current time as a number of milliseconds.

```
const now = Date.now();
```

Suppose our last execution occurred at `1000 ms` and the throttle interval is `200 ms`.

| Current time | Time since last execution | Allowed?                                                                                   |
| ------------ | ------------------------- | ------------------------------------------------------------------------------------------ |
| 1050 ms      | 50 ms                     | No                                                                                         |
| 1100 ms      | 100 ms                    | No                                                                                         |
| 1150 ms      | 150 ms                    | No                                                                                         |
| 1200 ms      | 200 ms                    | Yes                                                                                        |
| 1250 ms      | 250 ms                    | Yes, provided the last execution was updated at 1200 ms and the interval has again elapsed |

The calculation is:

```
now - lastExecutionTime >= delay
```

That's the entire decision. If the difference is large enough, we execute. Otherwise, we ignore that call.

## 3. Step 2: Write the condition first

Before writing a reusable throttle function, let's solve the problem with a single function:

```
let lastExecutionTime = -Infinity;
const delay = 200;

function handleScroll() {
    const now = Date.now();

    if (now - lastExecutionTime >= delay) {
        lastExecutionTime = now;

        updateScrollProgress();
    }
}
```

Let's break down the logic.

First:

```
const now = Date.now();
```

We get the current time.

Second:

```
if (now - lastExecutionTime >= delay)
```

We check whether the interval has elapsed.

Third:

```
lastExecutionTime = now;
```

We record this execution's timestamp so another call cannot execute immediately afterward.

Fourth:

```
updateScrollProgress();
```

We perform the actual work.

Why initialize the timestamp to `-Infinity`? Because we want the first call to be allowed immediately, regardless of the chosen delay. This is clearer than relying on an arbitrary initial number.

Notice the order: we update the timestamp before executing the callback. The next invocation must see the updated timestamp, including if the callback indirectly triggers another invocation.

## 4. Step 3: Turn it into a reusable utility

We don't want to repeat that logic for every event handler.

We want to write:

```
const throttledScroll = throttle(updateScrollProgress, 200);

window.addEventListener("scroll", throttledScroll);
```

How do we turn our earlier function into `throttle(callback, delay)`?

Apply the same design principles we used for debounce.

The utility needs to remember `lastExecutionTime` between calls, so it belongs in the outer scope. The utility should return a function that checks the timestamp on every invocation.

```
function throttle(callback, delay) {
    let lastExecutionTime = -Infinity;

    return function (...args) {
        const now = Date.now();

        if (now - lastExecutionTime >= delay) {
            lastExecutionTime = now;

            callback.apply(this, args);
        }
    };
}
```

That's a complete, reusable leading-only throttle implementation.

Let's understand the additional details:

- `callback` is the function we want to control.
- `delay` is the minimum interval between permitted executions.
- `lastExecutionTime` is shared by calls to this particular throttled function.
- `...args` captures arguments passed to the returned function.
- `callback.apply(this, args)` preserves the receiver and arguments from the current invocation.

As with debounce, every call to `throttle()` creates its own closure. Two different throttled functions have independent timestamps.

## 5. Trace the code yourself

Consider:

```
const throttledLog = throttle(
    (value) => console.log(value),
    200
);

throttledLog("A"); // t = 0 ms
throttledLog("B"); // t = 50 ms
throttledLog("C"); // t = 100 ms
throttledLog("D"); // t = 200 ms
throttledLog("E"); // t = 250 ms
throttledLog("F"); // t = 450 ms
```

Assume the calls arrive at approximately those times.

### 0 ms

A executes

This is the first call, so it is allowed. `lastExecutionTime` becomes `0`.

### 50 ms

B is ignored

Only 50 ms have passed. The callback does not execute.

### 100 ms

C is ignored

Only 100 ms have passed since the last execution.

### 200 ms

D executes

The 200 ms interval has elapsed. The timestamp is updated to `200`.

### 250 ms

E is ignored

Only 50 ms have passed since D executed.

### 450 ms

F executes

Another 250 ms have elapsed since D executed.

The callback executes with these values:

```
A
D
F
```

Notice an important consequence: this implementation does not preserve calls that it ignores.

The value `"C"` was discarded, and `"E"` was discarded. The function does not remember them for later.

That's not a bug in this implementation. It is a behavior we chose.

## 6. Leading and trailing execution

This brings us to a crucial interview concept.

The word throttle describes limiting execution frequency, but it doesn't fully specify what happens to calls that arrive during the waiting period. We must decide whether the first call should execute immediately, whether the most recent call should execute afterward, or both.

### Leading execution

Leading means executing at the beginning of an interval, when the function is permitted to run.

```
Events:       A B C D E F
              │ │ │ │ │ │

Execution:    ✓           ✓
              A           E
```

Our timestamp-based implementation performs leading execution.

It executes the current call when enough time has elapsed and ignores the other calls. It does not schedule anything for the end of the interval.

### Trailing execution

Trailing means executing the most recent call at the end of a waiting period.

Suppose the events arrive like this:

```
Time:         0       50      100      200 ms
Events:       A        B        C        D
```

A trailing throttle can remember the latest arguments while a timer is running. When that timer fires, it executes the callback using those latest arguments.

```
Events:       A        B        C
              │        │        │
Timer:        └─────────────────┘
Execution:                       ✓ C
```

The difference is important: the ignored calls are no longer simply discarded. The latest call is retained for a later execution.

### Leading and trailing together

A throttle can support both behaviors:

```
Events:       A B C D E F G H
              │ │ │ │ │ │ │ │

Execution:    ✓       ✓       ✓
              A       E       H
```

Here, the first event executes immediately. Calls during the interval update the pending arguments. At the end of that interval, the most recent call can execute as well.

The exact behavior depends on the implementation and its configuration. For example, a library's default settings may differ from a custom implementation.

## 7. Build a trailing-only throttle with a timer

We already know enough to build another version.

Ask yourself two questions:

1. How can we tell whether a timer is already waiting?
2. Where should we store the latest arguments?

The timer itself can serve as the gate. When it is active, new calls update the saved arguments but do not schedule additional timers.

```
function throttleTrailing(callback, delay) {
    let timerId = null;
    let latestArgs;
    let latestThis;

    return function (...args) {
        // Always remember the most recent call.
        latestArgs = args;
        latestThis = this;

        // A timer is already pending, so don't schedule another.
        if (timerId !== null) {
            return;
        }

        // Schedule one execution after the waiting period.
        timerId = setTimeout(() => {
            const argsToUse = latestArgs;
            const thisToUse = latestThis;

            // Clear the saved state before calling user code.
            timerId = null;
            latestArgs = undefined;
            latestThis = undefined;

            callback.apply(thisToUse, argsToUse);
        }, delay);
    };
}
```

Let's trace the important behavior:

- The first call saves its arguments and starts a timer.
- Further calls update `latestArgs` and `latestThis`. They don't create additional timers.
- When the timer fires, the callback receives the most recently saved arguments.
- The timer state is cleared before invoking the callback, so a reentrant call can schedule new work.

This version is trailing-only: it waits for the timer instead of executing immediately. Its waiting period begins with the first call in each timer window.

For example:

```
const throttledSearch = throttleTrailing(
    (query) => console.log("Searching:", query),
    200
);

throttledSearch("jav");
throttledSearch("java");
throttledSearch("javascript");
```

If these calls happen while the same timer is pending, the callback executes once after the delay with `"javascript"`.

One key difference from debounce: new calls do not restart the timer. They replace the pending arguments instead. This lets throttle run periodically during continued activity.

## 8. Debounce vs. the two throttle implementations

| Behavior                            | Debounce                                          | Leading-only throttle               | Trailing-only throttle      |
| ----------------------------------- | ------------------------------------------------- | ----------------------------------- | --------------------------- |
| Executes immediately on first call  | No                                                | Yes                                 | No                          |
| New calls reset the timer           | Yes                                               | No timer used                       | No                          |
| Stores latest arguments             | For the pending execution                         | No                                  | Yes                         |
| Executes during continuous activity | Usually no, if calls arrive faster than the delay | Yes, when an eligible call arrives  | Yes, at timer boundaries    |
| Executes after activity stops       | After the delay                                   | Only if a new eligible call arrives | After a pending timer fires |

A particularly useful distinction:

- Debounce waits for inactivity.
- Leading-only throttle allows an eligible call immediately.
- Trailing-only throttle schedules execution and keeps the latest call.

## 9. A note on timestamps

`Date.now()` is easy to understand and is appropriate for learning this pattern. Since it reads the system clock, however, clock adjustments can affect elapsed-time comparisons. In browser code that measures elapsed time, `performance.now()` is generally preferable because it is monotonic.

For the timestamp-based version, the only change required is to use `performance.now()` instead of `Date.now()` for the current time.

This doesn't change the algorithm. It only changes the clock used for measurement.

## Practice before Chapter 4

Try answering these without looking back at the implementations.

Question 1

Why does the timestamp-based throttle update `lastExecutionTime` when the callback is allowed to execute?

To remember the latest event, even if it was ignored

To prevent another execution until the minimum interval has elapsed

To cancel pending timers

Correct. The timestamp establishes the start of the next permitted interval.

Question 2

Your leading-only throttle executes at 0 ms. Calls arrive at 50 ms and 100 ms. The delay is 200 ms. No calls arrive after 100 ms. Will the callback automatically execute at 200 ms?

Yes, because the interval ended

No, because this implementation doesn't schedule a timer

Correct. Time passing alone doesn't invoke a function. Another eligible call is needed.

Question 3

In the trailing-only throttle, why don't we call `setTimeout` for every invocation?

Multiple timers would defeat the rate-control behavior

JavaScript allows only one timer per function

Because trailing execution is synchronous

Correct. One pending timer gates the window while newer calls replace the saved arguments.

Question 4

You need a scroll handler that responds immediately and also processes the latest position at the end of an interval. Which behavior do you need?

Debounce

Leading-only throttle

Leading-and-trailing throttle

Correct. Leading handles the immediate update; trailing handles the most recent pending call.

### Next chapter

In Chapter 4, we'll bring the concepts together and examine production-oriented debounce and throttle implementations: `cancel()` and `flush()`, preserving `this` and arguments, leading/trailing options, cleanup, edge cases, and common interview traps. We'll also learn how to decide which version to use instead of choosing an implementation blindly.

