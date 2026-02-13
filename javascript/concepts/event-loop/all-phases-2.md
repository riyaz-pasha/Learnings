# 🔥 Harder Event Loop Code (Interview Killer)

```js
const fs = require("fs");

console.log("1️⃣ Sync Start");

setTimeout(() => {
  console.log("2️⃣ setTimeout OUTSIDE");

  process.nextTick(() => console.log("3️⃣ nextTick INSIDE timeout"));
  Promise.resolve().then(() => console.log("4️⃣ promise INSIDE timeout"));
}, 0);

setImmediate(() => {
  console.log("5️⃣ setImmediate OUTSIDE");

  process.nextTick(() => console.log("6️⃣ nextTick INSIDE immediate"));
  Promise.resolve().then(() => console.log("7️⃣ promise INSIDE immediate"));
});

fs.readFile(__filename, () => {
  console.log("8️⃣ I/O callback (poll)");

  setTimeout(() => console.log("9️⃣ setTimeout INSIDE I/O"), 0);
  setImmediate(() => console.log("🔟 setImmediate INSIDE I/O"));

  process.nextTick(() => console.log("1️⃣1️⃣ nextTick INSIDE I/O"));
  Promise.resolve().then(() => console.log("1️⃣2️⃣ promise INSIDE I/O"));
});

process.nextTick(() => console.log("1️⃣3️⃣ nextTick OUTSIDE"));
Promise.resolve().then(() => console.log("1️⃣4️⃣ promise OUTSIDE"));

console.log("1️⃣5️⃣ Sync End");
```

---

# 🧠 Now predict the output (Most likely)

### ✅ Output:

```txt
1️⃣ Sync Start
1️⃣5️⃣ Sync End
1️⃣3️⃣ nextTick OUTSIDE
1️⃣4️⃣ promise OUTSIDE
2️⃣ setTimeout OUTSIDE
3️⃣ nextTick INSIDE timeout
4️⃣ promise INSIDE timeout
5️⃣ setImmediate OUTSIDE
6️⃣ nextTick INSIDE immediate
7️⃣ promise INSIDE immediate
8️⃣ I/O callback (poll)
1️⃣1️⃣ nextTick INSIDE I/O
1️⃣2️⃣ promise INSIDE I/O
🔟 setImmediate INSIDE I/O
9️⃣ setTimeout INSIDE I/O
```

⚠️ Again: `I/O callback` vs `setImmediate outside` can sometimes shuffle depending on system timing, but this is the **typical** Node behavior.

---

# ✅ WHY This Output Happens (Deep Breakdown)

---

# Phase 0: Sync code first

```txt
1️⃣ Sync Start
1️⃣5️⃣ Sync End
```

During sync, we scheduled:

* `setTimeout OUTSIDE`
* `setImmediate OUTSIDE`
* `fs.readFile callback`
* `nextTick OUTSIDE`
* `promise OUTSIDE`

---

# Microtasks after sync

### Node runs:

1. `process.nextTick`
2. Promise microtasks

So:

```txt
1️⃣3️⃣ nextTick OUTSIDE
1️⃣4️⃣ promise OUTSIDE
```

---

# Event Loop Tick #1

## ⏱ Timers phase

Timer ready:

```txt
2️⃣ setTimeout OUTSIDE
```

Now inside that timeout callback, we scheduled:

* `nextTick INSIDE timeout`
* `promise INSIDE timeout`

🔥 Important Node rule:

> After every callback execution, Node drains microtasks immediately.

So immediately after printing `2️⃣`:

```txt
3️⃣ nextTick INSIDE timeout
4️⃣ promise INSIDE timeout
```

---

## 🌊 Poll phase

May or may not have I/O ready yet, but usually not instantly.

---

## ✅ Check phase

setImmediate outside runs:

```txt
5️⃣ setImmediate OUTSIDE
```

Inside it we scheduled:

* nextTick
* promise

Again Node drains microtasks immediately after the callback:

```txt
6️⃣ nextTick INSIDE immediate
7️⃣ promise INSIDE immediate
```

---

# Event Loop Tick #2

## 🌊 Poll phase

Now fs.readFile is ready:

```txt
8️⃣ I/O callback (poll)
```

Inside I/O callback we scheduled:

* setTimeout INSIDE I/O
* setImmediate INSIDE I/O
* nextTick INSIDE I/O
* promise INSIDE I/O

Again: microtasks drain immediately after I/O callback finishes:

```txt
1️⃣1️⃣ nextTick INSIDE I/O
1️⃣2️⃣ promise INSIDE I/O
```

---

## ✅ Check phase

Now we enter check phase and run:

```txt
🔟 setImmediate INSIDE I/O
```

---

# Event Loop Tick #3

## ⏱ Timers phase

Now setTimeout inside I/O runs:

```txt
9️⃣ setTimeout INSIDE I/O
```

---

# 🔥 Most Important Rules You Learn Here

## ✅ Rule 1: Microtasks run after *every callback*, not just after sync

So after:

* timeout callback
* immediate callback
* I/O callback

Node always drains:

1. `process.nextTick`
2. Promise microtasks

---

## ✅ Rule 2: Inside I/O callback:

`setImmediate` runs before `setTimeout(0)`

Because:

* setImmediate → check phase (immediately after poll)
* setTimeout → next timers phase (next loop)

So:

```txt
🔟 setImmediate INSIDE I/O
9️⃣ setTimeout INSIDE I/O
```

---

## ✅ Rule 3: nextTick has higher priority than Promises

So you always see nextTick print first.

---

# 🧠 Interview Tip (Golden Line)

If interviewer asks:

> “How to solve event loop output problems?”

Say this:

### **Step 1:** Execute all sync code

### **Step 2:** Drain nextTick queue

### **Step 3:** Drain promise microtasks

### **Step 4:** Enter event loop phases (timers → poll → check)

### **Step 5:** After each callback, drain nextTick + microtasks again

This is literally the algorithm.

---
