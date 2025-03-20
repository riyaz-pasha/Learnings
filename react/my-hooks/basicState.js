function useStateBasic() {
    let state;

    function useState(initialValue) {
        state = state ?? initialValue;

        const setState = (newValue) => {
            state = newValue;
            render();
        }

        return [state, setState];
    }

    return useState
}

function render() {
    console.log("Re rendered");
}

const useState = useStateBasic();
const [count, setCount] = useState(0);

console.log(count);
setCount(1); // Updates state, triggers "re-render"
console.log(count); // Still prints 0! 
// Why? Because count was assigned state before setCount(1) updated it.
// useState() does not re-fetch the updated state after a render.
setCount(2);
console.log(count);
