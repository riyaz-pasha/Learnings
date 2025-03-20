const fs = require('node:fs')

fs.readFile(__filename, () => {
    // Here setImmediate Executes first because this is running in I/O phase. ( setImmediate is part of check phase as check phase comes after Pending phase that executes first)
    // check phases.js there setTimeout executes first because timers phase ( setTimeout ) executes first later check phase ( setImmediate )
    setTimeout(() => console.log('setTimeout inside I/O phase'), 0);
    setImmediate(() => console.log('setImmediate inside I/O phase'));
});

