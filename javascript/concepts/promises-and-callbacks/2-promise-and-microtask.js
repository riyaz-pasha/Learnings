Promise.resolve()
    .then(() => console.log(2));

setTimeout(() => {
    console.log(5);
}, 1);

queueMicrotask(() => {
    console.log(3);
    queueMicrotask(() => console.log(4));
})

console.log(1);
