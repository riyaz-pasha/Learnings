// Even though setImmediate and setTimeout have 0 delay, setTimeout ececutes first because the Timers phase runs before Check Phase
setImmediate(() => console.log("setImmediate - Check Phase(5)"));
setTimeout(() => console.log("setTimeout - Timers Phase(1)"), 0);

// micro tasks execute immediately after synchronous code and before the next event loop phase
Promise.resolve().then(() => console.log("Promise.resolve")) // executes after I/O call backs, but before next event loop iteration
process.nextTick(() => console.log("nextTick - executes after synchronous code")) //  Executes immediately after the current operation (Before I/O event)


console.log("Synchronous code");

// 1️⃣ Synchronous Code Runs First:
// 2️⃣ Microtasks Execute Before the Event Loop Continues:
//      process.nextTick() runs immediately after sync code.
//      Promises resolve after process.nextTick() but before event loop.
// 3️⃣ Timers Phase Runs Next: setTimeout().
// 4️⃣ Check Phase Runs Last: setImmediate().
