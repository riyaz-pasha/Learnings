# Event loop

### What is event loop?
The event loop is a mechanism in JavaScript that allows asynchronous code (like setTimeout, fetch, or fs.readFile) to run without blocking the execution of other code.

Think of it like a queue manager that decides:
✅ What should run next?
✅ When should it run?

📌 What Does the Event Loop Do?
It continuously checks if there’s any pending task and decides:
✔️ Execute synchronous code first (top to bottom).
✔️ Execute microtasks (process.nextTick(), Promise.then()) before anything else.
✔️ Execute timers (setTimeout, setInterval) when their time expires.
✔️ Process I/O tasks (file read, database query, network request).
✔️ Run setImmediate() in the Check Phase.
✔️ Finally, handle Close Callbacks (e.g., cleanup actions).

### Event loop phases

1. Timers Phase - Executes `setTimeout()` and `setInterval()` callbacks.
2. Pending callbacks Phase - Executes I/O callbacks (e.g., file system, network).
3. Idle, Prepare Phase - Internal operations by Node.js
4. Poll Phase - Retrieves new I/O events, executes related callbacks.
5. Check Phase - Executes `setImmediate()` callbacks.
6. Close callbacks Phase - Executes cleanup callbacks ( e.g., socket.on('close') ).


Absolutely Riyaz — let’s make the **event loop phases** *crystal clear* (especially Node.js, since phases matter there).

---

# ✅ What are Event Loop Phases?

In Node.js, the event loop runs in **cycles** (iterations).
Each cycle has **fixed phases**, and Node executes callbacks waiting in each phase.

Think of it like:

> Node keeps rotating through phases in a circle, executing whatever is ready.

---

# 🔁 Node.js Event Loop Phases (Order)

Every loop iteration goes like this:

```
┌───────────────┐
│ 1) Timers      │  setTimeout / setInterval
├───────────────┤
│ 2) Pending     │  deferred system callbacks
├───────────────┤
│ 3) Idle/Prepare│  internal (ignore)
├───────────────┤
│ 4) Poll        │  I/O callbacks (fs, network)
├───────────────┤
│ 5) Check       │  setImmediate
├───────────────┤
│ 6) Close       │  close events
└───────────────┘
```

---

# ✅ Phase 1: **Timers Phase**

### What runs here?

* `setTimeout`
* `setInterval`

But only those whose delay has expired.

Example:

```js
setTimeout(() => console.log("timeout"), 0);
```

📌 Important:
Even if delay is `0`, it doesn’t mean immediate.
It means:

> Run in next timers phase when call stack is free.

---

# ✅ Phase 2: **Pending Callbacks Phase**

### What runs here?

* Some internal callbacks that were postponed
* Some TCP / system-level error callbacks

📌 In interviews:
You can say:

> This is mostly internal and rarely affects normal JS code.

---

# ✅ Phase 3: **Idle / Prepare Phase**

### What runs here?

* Node internal housekeeping
* Used by libuv

📌 Ignore for interviews.

---

# ✅ Phase 4: **Poll Phase (Most Important Phase)**

### What runs here?

* File system callbacks (`fs.readFile`)
* Network callbacks (HTTP, sockets)
* Database callbacks
* Most async I/O callbacks

Example:

```js
fs.readFile("a.txt", () => console.log("file read"));
```

📌 Poll phase also decides:

* If there are I/O callbacks → execute them
* If nothing is ready → wait here (block) for I/O
* But if `setImmediate` exists → don’t wait, go to check phase

---

# ✅ Phase 5: **Check Phase**

### What runs here?

* `setImmediate()` callbacks

Example:

```js
setImmediate(() => console.log("immediate"));
```

📌 Key Rule:

> setImmediate always runs in Check phase.

---

# ✅ Phase 6: **Close Callbacks Phase**

### What runs here?

* Close events like:

  * `socket.on("close")`
  * `server.close()`

Example:

```js
socket.on("close", () => console.log("closed"));
```

---

# 🔥 Where do Promises / nextTick run then?

They are NOT part of these phases.

They run in **microtask queues**, executed:

✅ after sync code
✅ after every callback finishes
(before moving to the next phase)

### Priority order:

1. `process.nextTick()` queue
2. Promise microtask queue (`then/catch/finally`)

So:

```js
process.nextTick(() => console.log("nextTick"));
Promise.resolve().then(() => console.log("promise"));
```

Output:

```
nextTick
promise
```

---

# ⭐ Most Important Phase Interaction (Interview Favorite)

## Why does `setImmediate` inside I/O run before `setTimeout(0)` inside I/O?

Because:

* I/O callback runs in **Poll phase**
* After Poll phase finishes, Node moves to **Check phase**
* So `setImmediate()` runs immediately

But `setTimeout(0)` must wait for the next loop’s **Timers phase**

So inside I/O:

```js
fs.readFile("x", () => {
  setTimeout(() => console.log("timeout"), 0);
  setImmediate(() => console.log("immediate"));
});
```

Output:

```
immediate
timeout
```

---

# 🧠 Super Clean Summary

### Macro Task Phase Execution:

* Timers → setTimeout
* Poll → I/O
* Check → setImmediate
* Close → close events

### Microtasks run between everything:

* process.nextTick
* promises

---

# ✅ One-Line Cheat Sheet

📌 Node executes in this order repeatedly:

```
Timers → Poll → Check → Timers → Poll → Check ...
```

(with pending/idle/close in between)

And after every callback:

```
nextTick → promise microtasks
```

---
