function createCustomReact() {
    let states = [];
    let index = 0;

    function useState(initialValue) {
        const currentIndex = index;
        states[currentIndex] = states[currentIndex] ?? initialValue;
        index++;

        const setState = (newValue) => {
            states[currentIndex] = newValue;
            // index = 0;
            render();
        }

        return [states[currentIndex], setState];
    }

    function render() {
        console.log(states);
    }

    return { useState, render };
}

const React = createCustomReact();
const { useState, render } = React;

function Counter() {
    const [count, setCount] = useState(0);
    const [text, setText] = useState('Hello');

    console.log(`count: ${count}, text: ${text}`);

    return { count, setCount, text, setText };
}

const { count, setCount, text, setText } = Counter();
setCount(1);
setText('World');
setCount(2);
