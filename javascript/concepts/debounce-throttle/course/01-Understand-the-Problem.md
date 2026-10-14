# JavaScript Course: Debounce and Throttle

The goal of this course is not to memorize two utility functions. It is to understand why they work, what state they need to maintain, and how to derive their implementations from scratch.

We'll learn in four chapters:

1. The core problem and mental models — when and why debounce and throttle are needed.
2. Debounce from scratch — build it step by step using `setTimeout` and `clearTimeout`.
3. Throttle from scratch — build it using timestamps and timers, and understand leading vs. trailing execution.
4. Production-level implementation — arguments, `this`, edge cases, cleanup, and interview questions.

# Chapter 1 — Understand the Problem First

## 1. The problem: JavaScript events can fire too frequently

Imagine a search input in an application.

```
searchInput.addEventListener("input", (event) => {
    fetchSearchResults(event.target.value);
});
```

The intention is simple: whenever the user types, fetch matching search results.

But imagine the user types:

`javascript`

This is ten characters. The browser could fire the `input` event ten times in quick succession.

Our function may make requests for:

```
j
ja
jav
java
javas
javasc
javascr
javascri
javascrip
javascript
```

That can mean ten requests even though the user ultimately wanted the results for just one search term.

The problem is not that JavaScript is executing incorrectly. The problem is that we are responding to every event, even when responding to every event is unnecessary.

Other examples include:

| Situation       | Potential problem                         |
| --------------- | ----------------------------------------- |
| Search input    | Too many API requests                     |
| Window resizing | Recalculating layout too often            |
| Scroll events   | Running expensive calculations repeatedly |
| Form validation | Validating on every keystroke             |
| Button clicks   | Repeatedly triggering an operation        |
| Mouse movement  | Excessive tracking or processing          |

Two techniques help control how often an operation executes: debounce and throttle.

## 2. Debounce: wait until activity stops

Suppose the user types three characters, with a short pause between each one.

```
Time ──────────────────────────────────────────►

Input events:      A       B       C
                   │       │       │
                   └─ timer┘       │
                           └─ timer┘
                                   └── wait ──► Execute
```

The important idea is:

Every new event restarts the waiting period. Execute the function only when the waiting period finishes without another event.

For example, suppose the debounce delay is `500 ms`.

- At `0 ms`, the user types `A`. Schedule execution for `500 ms`.
- At `200 ms`, the user types `B`. Cancel the previous schedule and schedule execution for `700 ms`.
- At `400 ms`, the user types `C`. Cancel the previous schedule and schedule execution for `900 ms`.
- No further input occurs, so the function executes at `900 ms`.

Only one execution occurs, with the latest input.

### The debounce mental model

A

Start a timer

B

Cancel the old timer; start a new one

C

Cancel the old timer; start a new one

No new event for 500 ms → execute

### How would you implement this?

Think about what the code must remember.

When another event happens, JavaScript needs a way to identify the previously scheduled execution so it can cancel it.

That tells us we need a variable to store a timer ID.

The key browser APIs are:

```
const timerId = setTimeout(callback, 500);

clearTimeout(timerId);
```

- `setTimeout` schedules a callback to run after a delay.
- `clearTimeout` cancels a timeout that has not already begun executing.

Notice how the implementation follows directly from the behavior. We don't need to memorize a debounce function yet: we already know its two essential operations.

One important detail: the delay is a minimum waiting period, not a guarantee that the callback will execute at the exact millisecond specified.

## 3. Throttle: allow execution at a controlled rate

Now consider a different problem.

An application listens for scrolling and calculates whether certain elements are visible.

```
window.addEventListener("scroll", () => {
    calculateVisibleElements();
});
```

While scrolling, the browser may fire many events in rapid succession. But perhaps we only need to calculate visibility approximately once every `200 ms`.

Unlike debounce, we don't necessarily want to wait until scrolling stops. We want the function to continue running during scrolling, just not on every event.

Throttle limits how frequently a function can execute.

Consider a throttle interval of `200 ms`.

```
Time ─────────────────────────────────────────────►

Events:       A B C D E F G H I J K L M N O

Allowed:      X           X           X
              0 ms        200 ms      400 ms

Other events: ignored or deferred, depending on the implementation
```

The important distinction is that events keep arriving, but the function executes at a controlled rate.

The exact behavior depends on the throttle variant. Some execute at the beginning of a permitted interval, some execute at the end, and some support both.

### How would you implement this?

Ask yourself what the code must know.

It needs to answer:

"Am I allowed to execute now, or must I wait?"

One approach is to store the timestamp of the last execution.

```
const now = Date.now();
```

Then compare the current time with the previous execution time:

```
now - lastExecutionTime >= delay
```

If enough time has passed, execute the function and update `lastExecutionTime`.

That is the core idea behind a timestamp-based throttle. Timer-based versions are also possible, especially when we want to schedule a later execution.

## 4. Debounce vs. throttle

| Question                               | Debounce                                  | Throttle                                         |
| -------------------------------------- | ----------------------------------------- | ------------------------------------------------ |
| Main goal                              | Wait for a pause in activity              | Control the execution rate                       |
| When does it run?                      | After events stop for the specified delay | At permitted intervals, depending on the variant |
| Does each new event affect scheduling? | Resets the timer                          | Usually does not reset the execution window      |
| Common example                         | Search API requests                       | Scroll processing                                |
| Main concept                           | Cancel and reschedule                     | Track time or maintain a timer gate              |

A useful way to remember the difference is not to memorize definitions, but to ask what you want the function to do.

- "Run after the user stops doing something." → Debounce.
- "Run at most once per interval while the user keeps doing something." → Throttle.

For example, for a search box, debounce is often a natural choice because intermediate search terms are no longer useful once the user continues typing.

For scroll-based progress tracking, throttle is often a natural choice because you want periodic updates even while the user continues scrolling.

These are common choices, not absolute rules. The right technique depends on the behavior your application needs.

## 5. Test your understanding

Question 1

A user types continuously for two seconds. You want to send a search request only after they stop typing for 400 ms. Which technique fits best?

Debounce

Throttle

Correct. Each new keystroke restarts the waiting period.

Question 2

A user scrolls continuously. You want to update a progress indicator at controlled intervals, even while scrolling continues.

Debounce

Throttle

Correct. Throttle permits periodic updates during continuous activity.

Question 3

In a debounce implementation, why do we need to store the timeout ID?

To remember how many milliseconds the delay is

To cancel the previously scheduled execution when another event arrives

To remember how many events have occurred

Correct. The timeout ID lets us cancel the old schedule before creating a new one.

### What comes next?

In Chapter 2, we will build debounce from an empty function, one line at a time. We'll derive why it returns a function, why the timer variable must live in a particular scope, and why placing the timer inside the wrong scope breaks the implementation.

The objective is to reach a point where you can write the implementation yourself by reasoning about the behavior, rather than recalling a code snippet.

Say next when you're ready.

