const fs = require("fs");

console.log("1️⃣ Sync Start");

// -----------------------------
// Microtasks (run right after sync ends)
// -----------------------------
process.nextTick(() => console.log("2️⃣ process.nextTick (nextTick queue)"));
Promise.resolve().then(() => console.log("3️⃣ Promise.then (microtask queue)"));

// -----------------------------
// Timers Phase
// -----------------------------
setTimeout(() => console.log("4️⃣ setTimeout (Timers Phase)"), 0);

// -----------------------------
// Check Phase
// -----------------------------
setImmediate(() => console.log("5️⃣ setImmediate (Check Phase)"));

// -----------------------------
// Poll Phase (I/O)
// -----------------------------
fs.readFile(__filename, () => {
  console.log("6️⃣ fs.readFile callback (Poll Phase)");

  // Microtasks inside Poll callback (run immediately after callback finishes)
  process.nextTick(() =>
    console.log("7️⃣ nextTick inside I/O (runs immediately after Poll callback)")
  );

  Promise.resolve().then(() =>
    console.log("8️⃣ promise inside I/O (runs after nextTick)")
  );

  // Scheduled again
  setTimeout(() => console.log("9️⃣ setTimeout inside I/O (Next Timers Phase)"), 0);

  setImmediate(() =>
    console.log("🔟 setImmediate inside I/O (Next Check Phase)")
  );
});

console.log("1️⃣1️⃣ Sync End");

/**
 * -----------------------------
 * Expected Output (Typical)
 * -----------------------------
 *
 * 1️⃣ Sync Start
 * 1️⃣1️⃣ Sync End
 * 2️⃣ process.nextTick (nextTick queue)
 * 3️⃣ Promise.then (microtask queue)
 * 4️⃣ setTimeout (Timers Phase)
 * 5️⃣ setImmediate (Check Phase)
 * 6️⃣ fs.readFile callback (Poll Phase)
 * 7️⃣ nextTick inside I/O (runs immediately after Poll callback)
 * 8️⃣ promise inside I/O (runs after nextTick)
 * 🔟 setImmediate inside I/O (Next Check Phase)
 * 9️⃣ setTimeout inside I/O (Next Timers Phase)
 *
 * -----------------------------
 * Key Observation:
 * -----------------------------
 * Inside I/O:
 *   setImmediate runs before setTimeout(0)
 * because Poll -> Check happens before next Timers phase.
 */
