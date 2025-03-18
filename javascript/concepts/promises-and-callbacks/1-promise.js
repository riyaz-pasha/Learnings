console.log(1);

new Promise((resolve, reject) => {
    console.log(2);
    resolve(4);
}).then(result => console.log(result));

console.log(3);
