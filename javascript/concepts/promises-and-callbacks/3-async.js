console.log("First");

// executes after I/O call backs, but before next event loop iteration
setImmediate(() => console.log("Fourth"));

//  Executes immediately after the current operation (Before I/O event)
process.nextTick(() => console.log("Third"));

console.log("Second");
