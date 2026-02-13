/**
 * ============================================================
 *  NODE.JS EVENT LOOP - FINAL BOSS EXAMPLE (WITH FULL COMMENTS)
 * ============================================================
 *
 * This file demonstrates:
 *  - Sync call stack execution
 *  - process.nextTick queue (highest priority microtask)
 *  - Promise microtask queue
 *  - Timers phase (setTimeout)
 *  - Poll phase (fs.readFile callback)
 *  - Check phase (setImmediate)
 *
 * IMPORTANT RULES (Node.js):
 *
 * 1) JS runs synchronous code first (call stack).
 *
 * 2) After call stack becomes empty, Node drains microtasks in this order:
 *      A) process.nextTick queue (highest priority)
 *      B) Promise microtask queue (then/catch/finally)
 *
 * 3) Then Node enters event loop phases (repeats forever):
 *      1. Timers Phase      -> setTimeout / setInterval callbacks
 *      2. Pending Callbacks -> internal stuff (rarely visible)
 *      3. Idle/Prepare      -> internal
 *      4. Poll Phase        -> I/O callbacks (fs, network, etc.)
 *      5. Check Phase       -> setImmediate callbacks
 *      6. Close Callbacks   -> close events (socket close, etc.)
 *
 * 4) After EVERY callback execution (timer callback, I/O callback, immediate callback),
 *    Node again drains microtasks:
 *      process.nextTick -> then Promise microtasks
 *
 * 5) Special rule:
 *    Inside an I/O callback (poll phase):
 *      setImmediate() usually executes BEFORE setTimeout(0)
 *    because:
 *      Poll phase -> Check phase happens immediately,
 *      but setTimeout must wait for next Timers phase.
 *
 * ============================================================
 */

const fs = require("fs");

console.log("1️⃣ sync start");

/**
 * ------------------------------------------------------------
 * TIMER PHASE TASK
 * ------------------------------------------------------------
 * This schedules a timer callback into Timers Phase.
 * It will execute in the next Timers phase once delay is satisfied.
 */
setTimeout(() => {
  console.log("2️⃣ timeout-1");

  /**
   * Promise.then goes into MICROTASK queue (Promise microtask queue).
   * It will NOT run immediately here.
   * It will run only after the current callback finishes,
   * when Node drains microtasks.
   */
  Promise.resolve().then(() => {
    console.log("3️⃣ promise-inside-timeout-1");

    /**
     * This setTimeout(0) is created INSIDE a Promise microtask.
     * It will go to Timers Phase of a future event loop iteration.
     */
    setTimeout(() => console.log("4️⃣ timeout-inside-promise"), 0);

    /**
     * setImmediate goes into CHECK phase queue.
     * It will execute in the Check Phase of a future iteration.
     */
    setImmediate(() => console.log("5️⃣ immediate-inside-promise"));
  });

  /**
   * process.nextTick goes into nextTick queue (highest priority).
   * It will run right after this timeout callback ends
   * BEFORE Promise microtasks.
   */
  process.nextTick(() => console.log("6️⃣ nextTick-inside-timeout-1"));
}, 0);

/**
 * ------------------------------------------------------------
 * CHECK PHASE TASK
 * ------------------------------------------------------------
 * setImmediate callback goes into Check Phase queue.
 * It will execute in the Check Phase.
 */
setImmediate(() => {
  console.log("7️⃣ immediate-1");

  /**
   * nextTick inside immediate callback.
   * After immediate callback ends, Node drains microtasks:
   * nextTick first, then promise microtasks.
   */
  process.nextTick(() => console.log("8️⃣ nextTick-inside-immediate-1"));

  /**
   * Promise microtask inside immediate callback.
   */
  Promise.resolve().then(() => console.log("9️⃣ promise-inside-immediate-1"));
});

/**
 * ------------------------------------------------------------
 * POLL PHASE TASK (I/O)
 * ------------------------------------------------------------
 * fs.readFile triggers an async I/O operation.
 * Once file read completes, its callback will be executed
 * in Poll Phase.
 */
fs.readFile(__filename, () => {
  console.log("🔟 I/O callback");

  /**
   * Timer inside I/O callback:
   * goes to Timers Phase of a future iteration.
   */
  setTimeout(() => console.log("1️⃣1️⃣ timeout-inside-IO"), 0);

  /**
   * Immediate inside I/O callback:
   * goes to Check Phase.
   *
   * IMPORTANT:
   * Since we are currently in Poll Phase,
   * after Poll finishes, Node naturally goes to Check Phase.
   *
   * So setImmediate INSIDE I/O usually runs quickly (often before timer).
   */
  setImmediate(() => console.log("1️⃣2️⃣ immediate-inside-IO"));

  /**
   * Promise microtask inside I/O callback:
   * runs immediately after this I/O callback finishes
   * (after nextTick queue is drained first).
   */
  Promise.resolve().then(() => {
    console.log("1️⃣3️⃣ promise-inside-IO");

    /**
     * nextTick inside Promise microtask:
     * still gets higher priority than other microtasks.
     * Node will run it right after this Promise microtask completes.
     */
    process.nextTick(() =>
      console.log("1️⃣4️⃣ nextTick-inside-promise-inside-IO")
    );
  });
});

/**
 * ------------------------------------------------------------
 * MICROTASKS scheduled from main script
 * ------------------------------------------------------------
 * These will execute immediately after sync code finishes.
 */
process.nextTick(() => console.log("1️⃣5️⃣ nextTick-outside"));
Promise.resolve().then(() => console.log("1️⃣6️⃣ promise-outside"));

console.log("1️⃣7️⃣ sync end");

/**
 * ============================================================
 * EXPECTED OUTPUT (MOST COMMON)
 * ============================================================
 *
 * 1️⃣ sync start
 * 1️⃣7️⃣ sync end
 *
 * ---- microtasks after main script ----
 * 1️⃣5️⃣ nextTick-outside
 * 1️⃣6️⃣ promise-outside
 *
 * ---- event loop tick #1: Timers Phase ----
 * 2️⃣ timeout-1
 *
 * ---- after timeout callback ends, microtasks drain ----
 * 6️⃣ nextTick-inside-timeout-1
 * 3️⃣ promise-inside-timeout-1
 *
 * ---- event loop tick #1 continues: Check Phase ----
 * 7️⃣ immediate-1
 *
 * ---- after immediate callback ends, microtasks drain ----
 * 8️⃣ nextTick-inside-immediate-1
 * 9️⃣ promise-inside-immediate-1
 *
 * ---- Check phase continues (immediate queued earlier from promise) ----
 * 5️⃣ immediate-inside-promise
 *
 * ---- Poll phase later when I/O completes ----
 * 🔟 I/O callback
 *
 * ---- after I/O callback ends, microtasks drain ----
 * 1️⃣3️⃣ promise-inside-IO
 * 1️⃣4️⃣ nextTick-inside-promise-inside-IO
 *
 * ---- then check phase after poll ----
 * 1️⃣2️⃣ immediate-inside-IO
 *
 * ---- future tick: timers phase ----
 * 4️⃣ timeout-inside-promise
 * 1️⃣1️⃣ timeout-inside-IO
 *
 *
 * FINAL OUTPUT (Most likely full order):
 *
 * 1️⃣ sync start
 * 1️⃣7️⃣ sync end
 * 1️⃣5️⃣ nextTick-outside
 * 1️⃣6️⃣ promise-outside
 * 2️⃣ timeout-1
 * 6️⃣ nextTick-inside-timeout-1
 * 3️⃣ promise-inside-timeout-1
 * 7️⃣ immediate-1
 * 8️⃣ nextTick-inside-immediate-1
 * 9️⃣ promise-inside-immediate-1
 * 5️⃣ immediate-inside-promise
 * 🔟 I/O callback
 * 1️⃣3️⃣ promise-inside-IO
 * 1️⃣4️⃣ nextTick-inside-promise-inside-IO
 * 1️⃣2️⃣ immediate-inside-IO
 * 4️⃣ timeout-inside-promise
 * 1️⃣1️⃣ timeout-inside-IO
 *
 * ============================================================
 *
 * NOTE ABOUT NON-DETERMINISM:
 *
 * Some ordering may vary slightly depending on OS scheduling,
 * especially around when fs.readFile completes relative to
 * immediates and timers.
 *
 * But the internal rules remain true:
 *  - nextTick always before promise microtasks
 *  - microtasks always before moving to next phase
 *  - setImmediate inside I/O usually before setTimeout(0) inside I/O
 *
 * ============================================================
 */
