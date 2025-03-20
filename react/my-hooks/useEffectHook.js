function createCustomReact() {

    let state;
    let rerender = () => { };
    let previousDependencies = [];
    let cleanup;

    function useEffect(callback, dependencies) {
        const hasChanged = !previousDependencies || dependencies.some((dep, index) => dep !== previousDependencies[index]);
        if (!hasChanged) return;
        if (cleanup) cleanup();
        cleanup = callback();
        previousDependencies = dependencies;
    }

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

    return { render, useState, useEffect }
}

const { render, useState, useEffect } = createCustomReact();

function Counter() {
    const [count, setCount] = useState(0);

    console.log(`count: ${count}`);

    useEffect(() => {
        console.log("Effect ran!");

        return () => console.log("Cleanup called before re ran");
    }, [count])

    setTimeout(() => {
        setCount(count + 1); // This will trigger a re-run
    }, 2000);
}

render(Counter);
