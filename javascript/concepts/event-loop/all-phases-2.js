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
