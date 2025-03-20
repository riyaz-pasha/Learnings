function createCustomReact() {
    let state;
    let rerender = () => { }; // Function to trigger a re-run

    function useState(initialValue) {
        state = state ?? initialValue;

        const setState = (newValue) => {
            state = newValue;
            rerender(); // Trigger the "re-render"
        }

        return [state, setState];
    }

    function render(component) {
        rerender = () => {
            component(); // Re-run the component (like React)
        };
        component(); // Initial render
    }

    return { useState, render };
}

const { useState, render } = createCustomReact();

function Counter() {
    const [count, setCount] = useState(0);

    console.log(`count: ${count}`);

    setTimeout(() => {
        setCount(count + 1); // This will trigger a re-run
    }, 1000);

    return { count, setCount, };
}

render(Counter);
