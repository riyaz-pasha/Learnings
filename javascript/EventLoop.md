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