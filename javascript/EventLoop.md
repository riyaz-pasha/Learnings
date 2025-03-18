# Event loop

### What is event loop?

### Event loop phases

1. Timers Phase - Executes `setTimeout()` and `setInterval()` callbacks.
2. I/O callbacks Phase - Executes I/O callbacks (e.g., file system, network).
3. Idle, Prepare Phase - Internal operations by Node.js
4. Poll Phase - Retrieves new I/O events, executes related callbacks.
5. Check Phase - Executes `setImmediate()` callbacks.
6. Close callbacks Phase - Executes cleanup callbacks ( e.g., socket.on('close') ).