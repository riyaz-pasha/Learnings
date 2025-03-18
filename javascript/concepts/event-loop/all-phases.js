const fs = require("fs");
const { EventEmitter } = require("events");

const eventEmitter = new EventEmitter();

console.log("1️⃣ Synchronous Code - Start");

// 🔹 Microtasks: `process.nextTick()`
process.nextTick(() => console.log("2️⃣ process.nextTick - Microtask Queue"));

// 🔹 Microtasks: Promise
Promise.resolve().then(() => console.log("3️⃣ Promise.then - Microtask Queue"));

// 🔹 Timers Phase
setTimeout(() => console.log("4️⃣ setTimeout - Timers Phase(1)"), 0);

// 🔹 I/O Operation (Triggers Poll Phase)
fs.readFile(__filename, () => {
    console.log("5️⃣ File Read I/O Callback Phase(2)");

    // // Inside I/O callback (Poll Phase), we queue `setImmediate()` → Check Phase
    // setImmediate(() => console.log("6️⃣ setImmediate Inside I/O - Check Phase - Poll phase(4)"));

    // // Inside I/O callback (Poll Phase), we queue another `setTimeout()` → Timers Phase
    // setTimeout(() => console.log("7️⃣ setTimeout Inside I/O - Timers Phase - Poll Phase(4)"), 0);
});

// 🔹 `setImmediate()` outside I/O - It runs in Check Phase
setImmediate(() => console.log("8️⃣ setImmediate Outside I/O - Check Phase(5)"));

// 🔹 Close Callbacks Phase
eventEmitter.on("close", () => console.log("9️⃣ EventEmitter - Close Callbacks Phase(6)"));

// Trigger Close Event
setTimeout(() => eventEmitter.emit("close"), 10);

console.log("🔟 Synchronous Code - End");
