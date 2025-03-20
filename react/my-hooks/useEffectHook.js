function createCustomReact() {

    let state;
    let rerender = () => { };

    function useState(initialValue) {
        state = state ?? initialValue;
        function setState(newState) {
            state = newState;
            rerender();
        }
        return [state, setState];
    }

    function render(component) {
        rerender = () => {
            component();
        }
        component(); // Initial render
    }

    return { render, useState }
}

const { render, useState } = createCustomReact();

function Counter() {
    const [count, setCount] = useState(0);

    console.log(`count: ${count}`);

    setTimeout(() => {
        setCount(count + 1); // This will trigger a re-run
    }, 1000);
}

render(Counter);
