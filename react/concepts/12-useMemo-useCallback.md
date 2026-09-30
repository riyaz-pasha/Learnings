# Topic 12 — `useMemo`, `useCallback`, and `React.memo`

This topic is one of the **most frequently misunderstood parts of React**.

A lot of developers memorize:

```text
useMemo → memoize value
useCallback → memoize function
React.memo → memoize component
```

That is correct, but incomplete.

For interviews, you should understand the actual relationship:

```text
useMemo / useCallback
        ↓
Hooks attached to a Fiber
        ↓
cache a value/function + dependencies

React.memo
        ↓
changes the component's reconciliation path
        ↓
compare previous props with next props
        ↓
possibly bail out
        ↓
component function is not called
```

The current React source explicitly has `MemoComponent` and `SimpleMemoComponent` Fiber types, and the `beginWork` path performs prop comparison and can return a bailout rather than rendering the wrapped function. ([GitHub][1])

The official docs also emphasize an important principle:

> **Memoization is a performance optimization, not something your program should need for correctness.** ([React][2])

---

# 1. The problem memoization solves

Imagine:

```jsx
function App() {
    const [count, setCount] = useState(0);

    return (
        <>
            <ExpensiveChild />
            <button onClick={() => setCount(count + 1)}>
                {count}
            </button>
        </>
    );
}
```

Every time `count` changes:

```text
App
 ↓
renders again
 ↓
ExpensiveChild may be processed again
```

If:

```text
ExpensiveChild
```

does expensive work and its inputs haven't changed, repeating that work may be wasteful.

Memoization gives React a way to say:

> "We've already computed this, and the things it depends on haven't changed."

There are **three different memoization mechanisms** we're studying:

```text
useMemo
→ cache a calculation result

useCallback
→ cache a function identity

React.memo
→ skip rendering a component when its props are equal
```

---

# 2. `useMemo`

Example:

```jsx
const visibleTodos = useMemo(
    () => filterTodos(todos, tab),
    [todos, tab]
);
```

The important thing is:

```text
calculate value
      ↓
cache result
```

The official documentation defines `useMemo` as a Hook that caches the result of a calculation between re-renders. React compares dependencies with `Object.is`. ([React][3])

---

# 3. What `useMemo` actually stores

We've already learned how Hooks are represented:

```text
Fiber
 ↓
memoizedState
 ↓
Hook → Hook → Hook
```

A `useMemo` Hook conceptually stores:

```text
[value, dependencies]
```

So:

```jsx
const result = useMemo(
    () => calculate(a, b),
    [a, b]
);
```

can be thought of as:

```text
Hook
 └── memoizedState
       ├── cachedValue
       └── deps
```

Historically/current source implementations use exactly this conceptual shape: the mount path computes a value and stores `[value, deps]`, while the update path compares dependencies and returns the previous value when they are equal. The current React Hook implementation retains this general architecture. ([Gist][4])

---

# 4. Mounting `useMemo`

Suppose:

```jsx
const value = useMemo(() => expensiveCalculation(), [a]);
```

First render:

```text
useMemo
 ↓
create Hook
 ↓
run expensiveCalculation()
 ↓
result = 42
 ↓
store [42, [a]]
 ↓
return 42
```

Conceptually:

```text
Hook.memoizedState

[
    42,
    [a]
]
```

---

# 5. Updating `useMemo`

Next render:

```text
previous:
value = 42
deps = [10]

next:
deps = [10]
```

React compares:

```text
Object.is(10, 10)
```

→ `true`.

So:

```text
return cached 42
```

It doesn't need to execute:

```javascript
expensiveCalculation()
```

again.

That's the core benefit. ([React][3])

---

# 6. Dependency changes

Suppose:

```text
previous deps = [10]
next deps = [20]
```

Then:

```javascript
Object.is(10, 20)
```

is false.

React:

```text
 ↓
run calculation again
 ↓
new value
 ↓
store [newValue, [20]]
```

Conceptually:

```text
old
[42, [10]]

new
[84, [20]]
```

---

# 7. `useMemo` is not a general cache

This is subtle.

You should **not** think:

> "React guarantees this calculation will never run again."

React's current documentation explicitly says that React may throw away the cached value for specific reasons, such as initial-mount suspension, and that you should rely on `useMemo` only as a performance optimization. ([React][3])

So the semantic contract is better understood as:

> "React may reuse this cached calculation result when dependencies are unchanged."

Not:

> "This value is permanent storage."

For permanent mutable storage:

```text
useRef
```

is a better concept.

For state:

```text
useState
```

is appropriate.

---

# 8. `useMemo` does NOT prevent component rendering

This is one of the biggest interview traps.

Suppose:

```jsx
function App() {
    const value = useMemo(
        () => expensiveCalculation(data),
        [data]
    );

    return <Child value={value} />;
}
```

When `App` re-renders:

```text
App function
```

still executes.

`useMemo` only potentially skips:

```text
expensiveCalculation(data)
```

It does **not** mean:

```text
App won't render
```

This distinction is critical.

---

# 9. `React.memo` is different

Consider:

```jsx
const Child = memo(function Child({ value }) {
    console.log("Child render");

    return <div>{value}</div>;
});
```

Now React has an opportunity to say:

```text
previous props
      vs
next props
```

and if they are equal:

```text
don't invoke Child
```

This is fundamentally different from `useMemo`.

So:

```text
useMemo
→ skip recalculation

React.memo
→ potentially skip component rendering
```

---

# 10. `useCallback`

Now:

```jsx
const handleSubmit = useCallback(
    () => submit(productId),
    [productId]
);
```

The official docs define `useCallback` as caching a function definition between re-renders. React returns the same function if the dependencies have not changed. ([React][2])

Conceptually:

```text
Hook
 └── memoizedState
       ├── function
       └── dependencies
```

So it is extremely similar to `useMemo`.

---

# 11. `useCallback` is basically `useMemo` for functions

The React docs explicitly give this simplified mental model:

```javascript
function useCallback(fn, deps) {
    return useMemo(() => fn, deps);
}
```

The difference is mainly convenience and intent. `useMemo` caches the result of calling a calculation; `useCallback` caches the function itself. ([React][2])

So:

```jsx
useMemo(() => calculate(), [deps])
```

means:

```text
execute calculate()
cache result
```

while:

```jsx
useCallback(() => calculate(), [deps])
```

means:

```text
don't execute it
cache the function
```

---

# 12. Important: `useCallback` does not stop function creation

This is a very common misconception.

Suppose:

```jsx
const handleClick = useCallback(() => {
    doSomething();
}, []);
```

During every component execution, JavaScript still creates a function expression.

Conceptually:

```text
Render #1
new function A
React stores A

Render #2
new function B
React sees deps unchanged
React returns cached A
```

So:

```text
useCallback
≠
"don't create a function"
```

It means:

> **React may return the previously cached function instead of the new function.**

The official docs explicitly call this out. ([React][2])

---

# 13. Why does function identity matter?

JavaScript functions are objects.

So:

```javascript
const a = () => {};
const b = () => {};
```

gives:

```text
a !== b
```

even though their code looks identical.

Likewise:

```jsx
<Child onClick={() => doSomething()} />
```

creates a new function on each render.

Therefore:

```text
previous onClick
      ≠
next onClick
```

by reference.

That can matter when:

```text
Child is wrapped in React.memo
```

or when the function itself is a dependency of another Hook.

The official `useCallback` docs specifically identify these as important use cases. ([React][2])

---

# 14. The classic `React.memo` example

Without `useCallback`:

```jsx
const Child = memo(function Child({ onClick }) {
    console.log("Child render");
    return <button onClick={onClick}>Click</button>;
});

function Parent() {
    const [count, setCount] = useState(0);

    const handleClick = () => {
        console.log("clicked");
    };

    return (
        <>
            <Child onClick={handleClick} />

            <button onClick={() => setCount(count + 1)}>
                {count}
            </button>
        </>
    );
}
```

When `Parent` re-renders:

```text
new handleClick function
```

is created.

So:

```text
old props.onClick
      ≠
new props.onClick
```

`React.memo` sees changed props and cannot bail out.

---

# 15. Add `useCallback`

```jsx
function Parent() {
    const [count, setCount] = useState(0);

    const handleClick = useCallback(() => {
        console.log("clicked");
    }, []);

    return (
        <>
            <Child onClick={handleClick} />

            <button onClick={() => setCount(count + 1)}>
                {count}
            </button>
        </>
    );
}
```

Now:

```text
Render #1:
handleClick → Function A

Render #2:
handleClick → cached Function A
```

So:

```text
old onClick === new onClick
```

and `React.memo` can potentially bail out.

This is the standard combined use case documented by React. ([React][2])

---

# 16. `React.memo`

Now let's go inside the component mechanism.

```jsx
const Child = memo(function Child(props) {
    return <div>{props.name}</div>;
});
```

`memo()` doesn't simply change the function itself.

It creates a React element type representing a **memoized component**.

Conceptually:

```javascript
{
    $$typeof: REACT_MEMO_TYPE,
    type: Child,
    compare: null
}
```

The exact internal fields are implementation details, but the important idea is:

```text
memo(...)
 ↓
special React component type
```

React then recognizes this during reconciliation.

The current `beginWork` source has explicit `MemoComponent` and `SimpleMemoComponent` handling. ([GitHub][1])

---

# 17. Why does React need a special Fiber type?

Because React needs to change the normal rendering path.

Normal:

```text
FunctionComponent
 ↓
invoke component
```

Memoized:

```text
MemoComponent
 ↓
compare props
 ↓
if equal → bailout
 ↓
else → render wrapped component
```

So `React.memo` is integrated into the Fiber reconciliation process.

That's much deeper than:

> "It caches the component."

---

# 18. The current source has two memo paths

The current React source contains:

```text
MemoComponent
SimpleMemoComponent
```

The source explains that a plain function component using default shallow comparison can be upgraded to the `SimpleMemoComponent` fast path. ([GitHub][1])

This is an implementation optimization.

Conceptually:

```text
memo(plainFunction + default comparison)
        ↓
SimpleMemoComponent fast path
```

while:

```text
memo(forwardRef / custom compare / other wrapped type)
        ↓
MemoComponent path
```

Don't treat these as public APIs. They're internal Fiber tags.

---

# 19. Default comparison

When you write:

```jsx
const Child = memo(Component);
```

without a custom comparator:

```text
React compares props shallowly
```

The current `beginWork` source explicitly defaults to `shallowEqual` for `MemoComponent`. ([GitHub][1])

So conceptually:

```javascript
if (shallowEqual(prevProps, nextProps)) {
    bailout();
}
```

But there's an important condition:

```text
props equal
AND
no relevant update/context work
AND
ref compatibility
```

then React can bail out.

The current source checks scheduled update/context conditions and ref equality before taking the bailout path. ([GitHub][1])

---

# 20. What does "shallow comparison" mean?

Suppose:

```jsx
<Child
    name="Alice"
    age={25}
/>
```

Previous:

```javascript
{
    name: "Alice",
    age: 25
}
```

Next:

```javascript
{
    name: "Alice",
    age: 25
}
```

Each primitive value is equal.

So shallow comparison says:

```text
same
```

---

# 21. Objects break shallow equality

Suppose:

```jsx
<Child user={{ name: "Alice" }} />
```

Render #1:

```text
user → Object A
```

Render #2:

```text
user → Object B
```

Even:

```javascript
A = { name: "Alice" }
B = { name: "Alice" }
```

we have:

```text
A !== B
```

So shallow comparison:

```text
props.user
old ≠ new
```

and `React.memo` can't bail out.

The official `memo` docs explicitly call out object/function props created during rendering as a reason memoization may fail. ([React][5])

---

# 22. `useMemo` + `React.memo`

Now:

```jsx
const user = useMemo(
    () => ({ name: "Alice" }),
    []
);

<Child user={user} />
```

The object identity is retained:

```text
Render #1:
user → Object A

Render #2:
user → Object A
```

So:

```text
old user === new user
```

and `React.memo` can potentially bail out.

This is why you'll often see:

```text
useMemo
+
memo
```

used together.

---

# 23. Why `useCallback` + `React.memo`

Same principle:

```text
function prop
```

is usually recreated:

```text
render 1 → Function A
render 2 → Function B
```

`useCallback` provides:

```text
render 1 → Function A
render 2 → Function A
```

when dependencies haven't changed.

Then:

```text
React.memo
 ↓
props equal
 ↓
bailout
```

This is exactly the use case described in React's current `useCallback` documentation. ([React][2])

---

# 24. Important: `useMemo` does not memoize the component

This:

```jsx
const result = useMemo(...);
```

doesn't say:

```text
"Don't execute this component."
```

It says:

```text
"Don't recompute this particular calculation if dependencies are unchanged."
```

Similarly:

```jsx
const fn = useCallback(...);
```

doesn't say:

```text
"Don't render this component."
```

It says:

```text
"Reuse this function identity when dependencies are unchanged."
```

Only `React.memo` participates directly in the child-component bailout path.

---

# 25. React.memo does not mean "component will never render"

Very important.

Suppose:

```jsx
const Child = memo(function Child() {
    const [count, setCount] = useState(0);

    return ...;
});
```

If `Child`'s own state changes:

```text
Child state update
```

React must render `Child`.

`memo` primarily helps with **parent-driven prop changes** where the props haven't meaningfully changed.

The current `beginWork` source confirms this: the memo bailout first checks for scheduled update/context work before bailing out. ([GitHub][1])

So:

```text
React.memo
≠
never re-render
```

---

# 26. Context can also defeat the simplistic "props unchanged" story

Suppose:

```jsx
const Child = memo(function Child() {
    const theme = useContext(ThemeContext);

    return ...;
});
```

If the relevant context changes, the component can need to render even if its props remain equal.

That's because React's memo bailout also needs to account for context/other scheduled work. The current `beginWork` source checks `checkScheduledUpdateOrContext` before taking the memo bailout. ([GitHub][1])

This is an important senior-level nuance.

---

# 27. `React.memo` and state

Similarly:

```jsx
const Child = memo(function Child() {
    const [count, setCount] = useState(0);

    ...
});
```

When:

```jsx
setCount(...)
```

runs:

```text
Child has its own update
```

Therefore `React.memo` can't simply say:

```text
props didn't change → skip
```

because the component itself has relevant work.

Again:

```text
memo
=
parent-prop bailout optimization
```

not:

```text
memo
=
"component never executes again"
```

---

# 28. Custom comparator

You can write:

```jsx
const Child = memo(
    Component,
    (prevProps, nextProps) => {
        return prevProps.id === nextProps.id;
    }
);
```

Then React uses your comparator instead of the default shallow comparison for the memoized props path. The current source chooses `Component.compare` when provided; otherwise it uses `shallowEqual`. ([GitHub][1])

Conceptually:

```text
prevProps
    ↓
custom compare
    ↓
true?
 ┌────┴────┐
yes       no
 ↓         ↓
bailout   render
```

---

# 29. Why custom comparators are dangerous

Suppose:

```jsx
memo(Component, (prev, next) => {
    return true;
});
```

You've told React:

> "The component's props are always equivalent."

Even if they aren't.

Then React may skip renders that your component actually needs.

So:

```text
bad comparator
 ↓
stale UI
```

The comparator is not a place to "optimize aggressively."

It has to correctly describe when rendering can be skipped.

---

# 30. Custom comparator can also be expensive

Suppose the comparator does:

```javascript
deepEqual(previousHugeObject, nextHugeObject)
```

on every update.

You might spend:

```text
10 ms comparing
```

to avoid:

```text
5 ms rendering
```

Now your "optimization" made things worse.

So:

```text
memoization benefit
=
work avoided
>
memoization overhead
```

There is no automatic performance win.

---

# 31. `useMemo` also has overhead

Consider:

```jsx
const value = useMemo(
    () => a + b,
    [a, b]
);
```

You're asking React to:

```text
store value
store dependency array
compare dependencies
maintain Hook state
```

just to calculate:

```text
a + b
```

which is essentially free.

So:

```text
useMemo
```

can cost more complexity than the computation it avoids.

The React documentation explicitly says to use `useMemo` as a performance optimization, not by default. ([React][3])

---

# 32. Why React Compiler matters

Modern React changes this discussion significantly.

The official docs now note that **React Compiler automatically memoizes values and functions**, reducing the need for manual `useMemo` and `useCallback`. ([React][2])

There is also a `"use memo"` directive for explicit compiler optimization boundaries in relevant compilation modes. ([React][6])

So an interview answer in 2026 should not sound like:

> "You should put `useMemo` and `useCallback` everywhere."

A more current answer is:

> Manual memoization is still available, but modern React Compiler can automate many memoization opportunities, so manual memoization should be driven by actual performance needs and the project's compiler configuration.

---

# 33. Compiler doesn't mean `memo` disappeared

The compiler can optimize components/values/functions, but `React.memo` remains part of React's API and current docs. ([React][5])

So know both:

```text
manual memoization
```

and:

```text
compiler-driven memoization
```

The practical role of manual APIs may become smaller over time.

---

# 34. `useMemo` calculation runs during rendering

This is important.

Example:

```jsx
const value = useMemo(
    () => expensiveCalculation(data),
    [data]
);
```

The calculation function is executed during the render when React needs to calculate a new memoized value.

The current docs explicitly say React calls the calculation during the initial render and again when dependencies change. ([React][3])

Therefore:

```text
useMemo
≠
post-render work
```

It is render-time computation.

---

# 35. Therefore the `useMemo` calculation should be pure

Bad:

```jsx
const value = useMemo(() => {
    sendAnalytics();
    return calculate();
}, []);
```

Because this is a render-time calculation.

React's docs explicitly state that the function passed to `useMemo` should be pure, and Strict Mode may call it twice in development to detect impurities. ([React][3])

---

# 36. Strict Mode + `useMemo`

In development Strict Mode:

```text
calculate()
calculate()
```

may occur.

React ignores one of the results.

This is a development check for accidental impurities. ([React][3])

So don't use:

```jsx
useMemo(() => {
    incrementGlobalCounter();
    return value;
}, []);
```

and assume:

```text
"it runs once."
```

It is a calculation and should be pure.

---

# 37. Dependency comparison for `useMemo`

Same principle as `useEffect`:

```text
previousDeps
     vs
nextDeps
```

React compares each dependency using:

```javascript
Object.is()
```

If all are equal:

```text
return old cached value
```

otherwise:

```text
recalculate
```

The current documentation explicitly defines this behavior. ([React][3])

---

# 38. Dependency comparison for `useCallback`

Identical idea:

```text
previousDeps
     vs
nextDeps
```

If equal:

```text
return old function
```

If different:

```text
return current render's function
```

The current `useCallback` docs explicitly describe this. ([React][2])

---

# 39. `useMemo` internal shape

Conceptually:

```text
Hook
 └── memoizedState
       ├── value
       └── deps
```

Example:

```text
[value, [a, b]]
```

Update:

```text
if depsEqual:
    return old value
else:
    newValue = calculate()
    store [newValue, deps]
```

This is the essence of the implementation.

---

# 40. `useCallback` internal shape

Conceptually:

```text
Hook
 └── memoizedState
       ├── callback
       └── deps
```

Example:

```text
[handleSubmit, [productId, referrer]]
```

Update:

```text
if depsEqual:
    return old callback
else:
    store current callback
```

This mirrors the Hook architecture we've already learned.

---

# 41. `React.memo` internal shape

This one is different.

You don't have:

```text
Hook
```

for `memo`.

Instead:

```text
memo(Component)
```

creates a special element type.

Then React's Fiber system can create:

```text
MemoComponent
```

or use the optimized:

```text
SimpleMemoComponent
```

path.

The current `beginWork` source explicitly contains this distinction and uses `shallowEqual`/custom `compare` before deciding to bail out. ([GitHub][1])

So:

```text
useMemo
→ Hook mechanism

useCallback
→ Hook mechanism

React.memo
→ Fiber/component reconciliation mechanism
```

This is one of the most important distinctions from this lesson.

---

# 42. Why `React.memo` is more powerful than "caching props"

A naive explanation might be:

> "React.memo stores the old props."

The deeper explanation:

> React.memo changes the reconciliation path so React can compare the previous and next props before invoking the wrapped component. When they are equivalent and there is no other relevant work, React can bail out of rendering that component. ([GitHub][1])

That's the interview answer you want.

---

# 43. What exactly is skipped during a bailout?

Suppose:

```jsx
const Child = memo(function Child() {
    console.log("Child");
    return <div>Hello</div>;
});
```

Parent changes.

React reaches Child's memo Fiber:

```text
prevProps
    ↓
nextProps
    ↓
equal
```

Then:

```text
bailout
```

So React can avoid:

```text
calling Child()
```

and therefore avoid reconciling its returned children for that render.

The current `updateSimpleMemoComponent` path compares props and then calls `bailoutOnAlreadyFinishedWork` when appropriate. ([GitHub][1])

That's the concrete meaning of "memoization prevents a re-render."

---

# 44. What isn't skipped?

Potentially relevant updates within the component can still force work.

For example:

```jsx
const Child = memo(function Child() {
    const [count, setCount] = useState(0);
    ...
});
```

If:

```text
setCount(...)
```

occurs:

```text
Child has scheduled local work
```

so React doesn't simply bail out because props are equal.

Likewise, relevant context updates can force work. The current memo path checks scheduled update/context conditions before bailing out. ([GitHub][1])

---

# 45. Example: same props, Child still renders

```jsx
const Child = memo(function Child() {
    const [count, setCount] = useState(0);

    return (
        <button onClick={() => setCount(count + 1)}>
            {count}
        </button>
    );
});
```

Click Child's button.

Parent props might be:

```text
unchanged
```

but:

```text
Child's own state changed
```

Therefore:

```text
Child renders
```

This is exactly why `memo` isn't a permanent "frozen component."

---

# 46. Example: context

```jsx
const Child = memo(function Child() {
    const theme = useContext(ThemeContext);

    return <div>{theme}</div>;
});
```

If context changes:

```text
theme
light → dark
```

the child needs to react to that context dependency even if props are unchanged.

The current memo implementation's `checkScheduledUpdateOrContext` condition exists precisely because props equality isn't the whole story. ([GitHub][1])

---

# 47. Why JSX children can affect memoization

Suppose:

```jsx
const Wrapper = memo(function Wrapper({ children }) {
    return <div>{children}</div>;
});
```

Parent:

```jsx
<Wrapper>
    <Child />
</Wrapper>
```

If Parent re-renders, the JSX element passed as `children` may be a new React element object.

So:

```text
old children object
    ≠
new children object
```

and shallow prop comparison can see:

```text
children changed
```

Even though the rendered child looks equivalent.

This is an excellent interview example because it demonstrates that `memo` works at the **props identity level**, not by recursively deep-comparing the entire subtree.

---

# 48. Custom comparator and children

You might write:

```jsx
memo(
    Wrapper,
    (prev, next) => {
        return true;
    }
)
```

but then you're declaring:

```text
children always equivalent
```

which can be wrong.

The deeper lesson is:

```text
memo
→ comparator semantics matter
```

not:

```text
memo
→ React magically knows visual equivalence
```

---

# 49. `useMemo` + `React.memo` does not necessarily help

Suppose:

```jsx
const value = useMemo(
    () => expensiveCalculation(a),
    [a]
);

return <Child value={value} />;
```

This only helps when:

```text
a doesn't change
AND
Child memoization can benefit from stable value identity
AND
Child's render is expensive enough to justify the optimization
```

If:

```text
a changes every render
```

then:

```text
useMemo
→ recalculates every render
```

and you get little benefit.

---

# 50. `useCallback` + `React.memo` similarly

Suppose:

```jsx
const handleClick = useCallback(
    () => doSomething(id),
    [id]
);
```

If:

```text
id changes every render
```

then:

```text
new function every render
```

from the consumer's perspective.

So:

```text
useCallback
```

doesn't magically make the callback stable regardless of dependencies.

It makes it stable **while its dependencies remain stable**.

React's docs explicitly state this. ([React][2])

---

# 51. `useCallback` with updater functions

Consider:

```jsx
const handleAdd = useCallback(() => {
    setTodos([...todos, newTodo]);
}, [todos]);
```

Because it reads:

```text
todos
```

the callback must depend on:

```text
[todos]
```

But you can use the updater form:

```jsx
const handleAdd = useCallback(() => {
    setTodos(todos => [...todos, newTodo]);
}, [newTodo]);
```

Now the callback doesn't need to read the current `todos` value from its closure.

The official docs explicitly recommend updater functions as a way to reduce unnecessary `useCallback` dependencies. ([React][2])

This is a subtle but powerful optimization.

---

# 52. Why fewer dependencies can improve stability

Suppose:

```text
callback depends on
A
B
C
D
```

Any change to:

```text
A/B/C/D
```

creates a new callback.

If you can rewrite the logic so it only depends on:

```text
A
```

then the callback remains stable across more renders.

That's why React's docs say you generally want memoized functions to have as few dependencies as legitimately possible. ([React][2])

But:

> **Don't remove dependencies incorrectly just to make a callback stable.**

Correctness first.

---

# 53. `useMemo` should calculate, not mutate

Bad:

```jsx
const result = useMemo(() => {
    array.push(newItem);
    return array;
}, [array]);
```

You're mutating existing data during render.

Better:

```jsx
const result = useMemo(() => {
    return [...array, newItem];
}, [array, newItem]);
```

Memoized calculations should be pure. React's current docs explicitly state this. ([React][3])

---

# 54. `useMemo` and object identity

This is perhaps the most common practical use:

```jsx
const options = useMemo(
    () => ({
        roomId,
        mode
    }),
    [roomId, mode]
);
```

Now:

```text
same roomId/mode
→ same object identity
```

This can help with:

```text
React.memo child
Effect dependency
another memoized calculation
```

But again, don't use it merely because:

> "objects are bad."

Objects aren't bad.

The issue is whether **identity changes unnecessarily in a place where identity matters**.

---

# 55. `useMemo` as an object-stability tool

Example:

```jsx
const Child = memo(function Child({ options }) {
    ...
});

function Parent() {
    const [count, setCount] = useState(0);

    const options = {
        color: "red"
    };

    return <Child options={options} />;
}
```

Every Parent render:

```text
new options object
```

Therefore:

```text
React.memo
→ props changed
→ Child renders
```

Now:

```jsx
const options = useMemo(
    () => ({
        color: "red"
    }),
    []
);
```

Then:

```text
same options object
```

and Child may bail out.

But this is only beneficial when the bailout actually avoids meaningful work.

---

# 56. Why React docs say "don't add memo everywhere"

The current `memo` docs say memoization is useful primarily when:

```text
component renders often with the same props
+
rendering is expensive
```

and unnecessary when the component isn't noticeably expensive or doesn't receive stable props. ([React][5])

That's the right practical rule.

---

# 57. A performance equation

A useful way to think about memoization:

```text
Benefit =
work avoided
-
comparison/storage/complexity overhead
```

If:

```text
render cost = 0.1 ms
compare cost = 0.2 ms
```

then the optimization may actually hurt.

If:

```text
render cost = 50 ms
compare cost = 0.05 ms
```

then memoization can be valuable.

This is why profiling matters.

React's docs explicitly recommend using React Developer Tools Profiler to identify components that benefit from memoization. ([React][2])

---

# 58. One important modern React development

React Compiler can automatically memoize values and functions, according to the current React docs. ([React][2])

So modern interview discussions may ask:

> "Do we still need `useMemo`?"

Correct answer:

> Yes, it remains an API, but React Compiler can automate many memoization optimizations when configured and applicable. Manual memoization should generally be used when needed for performance or when working in a codebase/configuration where compiler optimization doesn't cover the case.

---

# 59. `React.memo` vs React Compiler

Think conceptually:

```text
React.memo
→ explicit component-level memoization API

useMemo/useCallback
→ explicit value/function memoization APIs

React Compiler
→ build-time analysis that can automatically insert/perform
   memoization optimizations
```

The goal is similar:

```text
avoid unnecessary work
```

but the mechanism differs.

The current docs explicitly say Compiler can reduce the need for manual `useMemo` and `useCallback`. ([React][2])

---

# 60. A complete render example

Consider:

```jsx
const Child = memo(function Child({ data, onSave }) {
    console.log("Child render");

    return (
        <button onClick={() => onSave(data)}>
            {data.name}
        </button>
    );
});

function Parent({ user }) {
    const [count, setCount] = useState(0);

    const data = useMemo(
        () => ({
            name: user.name
        }),
        [user.name]
    );

    const onSave = useCallback(
        () => saveUser(user.id),
        [user.id]
    );

    return (
        <>
            <Child
                data={data}
                onSave={onSave}
            />

            <button onClick={() => setCount(count + 1)}>
                {count}
            </button>
        </>
    );
}
```

Now increment `count`.

Parent renders again.

### `data`

Dependencies:

```text
[user.name]
```

unchanged.

Therefore:

```text
same data object
```

### `onSave`

Dependencies:

```text
[user.id]
```

unchanged.

Therefore:

```text
same function
```

### Child props

```text
data
same reference

onSave
same reference
```

So:

```text
React.memo
 ↓
shallow comparison
 ↓
equal
 ↓
bailout
```

Therefore Child's function need not execute.

This is the entire reason these APIs are commonly combined. ([React][2])

---

# 61. Now change `user.name`

Suppose:

```text
Alice → Bob
```

Then:

```text
data dependency changed
```

So:

```text
new data object
```

Now Child receives:

```text
old data !== new data
```

`React.memo` can't bail out.

Child renders.

---

# 62. Now change only `count`

```text
count
0 → 1
```

while:

```text
user.id
same

user.name
same
```

Then:

```text
data stable
onSave stable
```

Child can bail out.

This is the ideal scenario for memoization.

---

# 63. A common mistake: memoizing everything

Bad mental approach:

```jsx
const a = useMemo(...);
const b = useMemo(...);
const c = useMemo(...);
const fn1 = useCallback(...);
const fn2 = useCallback(...);
const fn3 = useCallback(...);

const Child = memo(...);
```

without evidence.

This can make a component:

```text
harder to read
harder to debug
harder to reason about
```

for no measurable benefit.

React's current docs explicitly recommend keeping code simple and profiling before adding memoization. ([React][2])

---

# 64. A bigger performance misconception

Suppose:

```jsx
const Child = memo(...)
```

but parent passes:

```jsx
<Child data={{ x: 1 }} />
```

Memoization won't help because:

```text
new object each render
```

Likewise:

```jsx
<Child onClick={() => doSomething()} />
```

creates a new callback each render.

So:

```text
React.memo
+
unstable props
=
little/no bailout
```

This is why:

```text
useMemo
useCallback
```

often appear together with:

```text
memo
```

The official `memo` docs make this exact point. ([React][5])

---

# 65. `React.memo` and primitive props

Suppose:

```jsx
<Child
    id={10}
    active={true}
    name="Alice"
/>
```

Those values have straightforward stable equality when unchanged.

So `memo` can often work without needing `useMemo`.

You don't need:

```jsx
useMemo(() => 10, [])
```

That would be pointless.

This is why memoization is primarily relevant to:

```text
objects
arrays
functions
```

when their identity would otherwise change.

---

# 66. `useMemo` doesn't make a value immutable

Suppose:

```jsx
const user = useMemo(
    () => ({ name: "Alice" }),
    []
);
```

The object is cached.

That doesn't magically make:

```javascript
user.name = "Bob";
```

safe.

You can still mutate it.

Memoization is about **identity/caching**, not immutability.

---

# 67. `useMemo` doesn't guarantee referential stability forever

Again, current React docs note that React can discard its cache for specific reasons. ([React][3])

So don't use:

```jsx
const value = useMemo(...);
```

as a semantic replacement for:

```text
"this object must exist forever and never change."
```

For state:

```text
useState
```

For stable mutable storage:

```text
useRef
```

For a computation where recomputation is okay:

```text
useMemo
```

---

# 68. `useCallback` doesn't guarantee permanent function identity

Same rule.

React may discard cached callback values in specific situations, according to its current docs. ([React][2])

So:

```text
useCallback
→ optimization

not
→ semantic identity guarantee for application correctness
```

If your logic depends on identity being stable as a correctness invariant, you need to rethink the design.

---

# 69. A useful three-way comparison

| API           | What it caches        | Main question                          |
| ------------- | --------------------- | -------------------------------------- |
| `useMemo`     | Calculation result    | "Can I reuse this computed value?"     |
| `useCallback` | Function itself       | "Can I reuse this function identity?"  |
| `React.memo`  | Component render work | "Can I skip rendering this component?" |

This is the **single most important table from this topic**.

---

# 70. Internally

### `useMemo`

```text
Fiber
 ↓
Hook
 ↓
memoizedState = [value, deps]
```

### `useCallback`

```text
Fiber
 ↓
Hook
 ↓
memoizedState = [callback, deps]
```

### `React.memo`

```text
memo(Component)
 ↓
special element type
 ↓
MemoComponent/SimpleMemoComponent Fiber
 ↓
compare props
 ↓
bailout or render
```

The current React implementation reflects all three architectural patterns. ([GitHub][7])

---

# 71. The relationship between all three

```text
                    Parent render
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
       useMemo       useCallback      normal props
          │              │              │
          ▼              ▼              ▼
     stable value   stable function   possibly new refs
          │              │              │
          └──────────────┼──────────────┘
                         ▼
                     Child props
                         │
                         ▼
                    React.memo
                         │
                         ▼
                   compare props
                         │
                  ┌──────┴──────┐
                  ▼             ▼
                equal         changed
                  │             │
                  ▼             ▼
               bailout       render Child
```

This is exactly the mental model you want for interviews.

---

# 72. Interview question: What is `useMemo`?

Strong answer:

> `useMemo` is a performance optimization Hook that caches the result of a calculation between renders. React stores the cached value and its dependencies as Hook state, compares dependencies using `Object.is`, and returns the cached value when they haven't changed. It does not prevent the component itself from rendering. ([React][3])

---

# 73. Interview question: What is `useCallback`?

> `useCallback` caches a function definition between renders. React returns the previously cached function if the dependencies haven't changed; otherwise it returns the current render's function. It is mainly useful when function identity matters, such as passing a callback to a memoized child or using it as a Hook dependency. ([React][2])

---

# 74. Interview question: Difference between `useMemo` and `useCallback`?

Best answer:

```text
useMemo
→ caches the result of calling a function.

useCallback
→ caches the function itself.
```

And:

```jsx
useCallback(fn, deps)
```

can be thought of conceptually as:

```jsx
useMemo(() => fn, deps)
```

The React docs explicitly make this comparison. ([React][2])

---

# 75. Interview question: What is `React.memo`?

> `React.memo` creates a memoized component type that lets React compare the previous and next props before rendering the wrapped component. With the default shallow comparison, if props are equal and there is no other relevant update or context work, React can bail out and skip rendering the component for that update. ([React][5])

---

# 76. Interview question: Does `React.memo` stop re-renders?

Don't say:

> "Yes."

Better:

> It can skip a parent-driven render when the memoized component's props are equal and there is no other relevant work. The component can still render because of its own state, context, or other scheduled work. ([GitHub][1])

---

# 77. Interview question: Why does `React.memo` fail with objects?

Because default comparison is shallow/reference-based.

```jsx
<Child data={{ name: "Alice" }} />
```

creates a new object each render:

```text
old object !== new object
```

Therefore:

```text
props changed
```

and React can't take the memo bailout.

The official `memo` docs call out objects and functions created during rendering as a common reason memoization becomes ineffective. ([React][5])

---

# 78. Interview question: Why use `useCallback` with `React.memo`?

Because:

```jsx
const handleClick = () => {};
```

creates a new function each render.

Therefore a memoized child sees:

```text
old onClick !== new onClick
```

Using:

```jsx
useCallback(...)
```

can preserve the function reference while its dependencies remain unchanged, allowing the child to pass its memo comparison. ([React][2])

---

# 79. Interview question: Does `useCallback` stop the function from being created?

No.

A new function expression can still be created during the component execution.

React simply may return the previously cached function instead of the newly created one if dependencies are unchanged. ([React][2])

---

# 80. Interview question: Does `useMemo` execute after render?

No.

Its calculation happens during rendering when React needs a new memoized value.

This is why the calculation must be pure. ([React][3])

---

# 81. Interview question: Can `useMemo` be used for side effects?

No.

Bad:

```jsx
useMemo(() => {
    saveToDatabase();
}, []);
```

`useMemo` is for calculation/caching, not effects. React's lint/documentation explicitly describes `useMemo` as being for computing and caching values, not side effects. ([React][8])

Use an appropriate Effect or event handler depending on the operation.

---

# 82. Interview question: Is `useMemo` guaranteed?

No.

It's a cache optimization.

React's current docs explicitly say the cache can be discarded in certain circumstances. ([React][3])

---

# 83. Interview question: Is `useCallback` guaranteed?

Same answer:

> It is a performance optimization; React may discard cached callbacks under documented circumstances. ([React][2])

---

# 84. Interview question: What does React.memo compare?

Default:

```text
shallow comparison of props
```

Custom:

```jsx
memo(Component, compare)
```

uses your comparison function.

The current `beginWork` implementation selects `Component.compare` when supplied, otherwise `shallowEqual`. ([GitHub][1])

---

# 85. Interview question: What's the difference between memoization and bailout?

This is a more senior question.

### Memoization

```text
cache something
```

Examples:

```text
useMemo
useCallback
```

### Bailout

```text
skip rendering/work
```

Example:

```text
React.memo
```

The first provides stable/cached results or identities.

The second allows React to avoid traversing/rendering work when it knows that work isn't needed.

---

# 86. One very important performance insight

Suppose:

```jsx
const result = useMemo(() => expensive(), []);
```

but the parent component itself is re-rendering constantly.

You still get:

```text
Parent()
Parent()
Parent()
Parent()
```

only the `expensive()` calculation is cached.

If the actual problem is that the whole component subtree shouldn't be rendering, `useMemo` is the wrong tool.

You may need:

```text
React.memo
state placement
component boundaries
context architecture
```

This distinction is extremely important.

---

# 87. Another performance insight

Suppose:

```jsx
const Child = memo(...);
```

but:

```jsx
<Child value={Date.now()} />
```

Every render gives a different value.

So:

```text
React.memo
→ no bailout
```

The optimization isn't broken.

The inputs really did change.

---

# 88. Another performance insight

Suppose:

```jsx
const Child = memo(...);

<Child
    a={1}
    b={2}
/>
```

and the child is only:

```jsx
return <span>{a + b}</span>;
```

Memoization may be pointless.

The child render is so cheap that the optimization overhead and code complexity aren't justified.

Again:

> **Optimize the expensive path, not every path.**

React's current docs make this same point. ([React][5])

---

# 89. Modern React interview answer

If an interviewer asks:

> "Should we use `useMemo` and `useCallback` everywhere?"

A current answer:

> No. They are performance optimizations, not requirements for correctness. They are useful when stable identity or avoiding expensive recalculation measurably helps—for example, when a memoized child can skip expensive rendering. React Compiler can also automatically memoize many values and functions in supported configurations, reducing the need for manual memoization. ([React][2])

That is a much stronger 2026 answer.

---

# 90. The deepest mental model

Don't remember:

```text
useMemo = performance
useCallback = performance
memo = performance
```

Remember:

```text
useMemo
    ↓
Hook cache
    ↓
[value, deps]
    ↓
reuse computed result

useCallback
    ↓
Hook cache
    ↓
[function, deps]
    ↓
reuse function identity

React.memo
    ↓
special Fiber/component path
    ↓
compare props
    ↓
bailout
    ↓
skip component work
```

That is the internals-level explanation.

---

# 91. Complete architecture

```text
                         Parent
                           │
                       re-renders
                           │
           ┌───────────────┼────────────────┐
           ▼               ▼                ▼
       useMemo         useCallback       JSX props
           │               │                │
           ▼               ▼                ▼
     cached value     cached function   object/function may
                                           be new
           │               │                │
           └───────────────┼────────────────┘
                           ▼
                      Child props
                           │
                           ▼
                      React.memo
                           │
                    compare prev/next
                           │
                  ┌────────┴────────┐
                  ▼                 ▼
                equal            changed
                  │                 │
                  ▼                 ▼
               bailout          render child
                  │                 │
                  │          ┌──────┴──────┐
                  │          ▼             ▼
                  │       hooks/state    reconciliation
                  │                        │
                  └────────────┬───────────┘
                               ▼
                            commit
```

---

# 92. Revision Sheet

```text
useMemo
→ caches the result of a calculation.

useCallback
→ caches a function identity.

React.memo
→ allows React to skip rendering a component when
  props are equal and no other relevant work exists.

useMemo storage
→ conceptually [value, deps]

useCallback storage
→ conceptually [function, deps]

Dependency comparison
→ Object.is per dependency.

useMemo
→ calculation runs during render.

useMemo calculation
→ should be pure.

useCallback
→ does not prevent creation of the function expression;
  it may return the cached previous function.

React.memo default comparison
→ shallow props comparison.

Objects/arrays/functions
→ new references can defeat memoization.

React.memo does not stop all renders
→ own state/context/relevant updates can still render.

Custom memo comparator
→ replaces default shallow comparison,
  but must be correct and cheap enough.

Memoization
→ optimization, not correctness.

React Compiler
→ can automatically memoize many values/functions,
  reducing the need for manual useMemo/useCallback.
```

The current React docs and source support these behaviors. ([React][2])

---

# Current React map

We've now gone from basic React to some fairly deep internals:

```text
React
 │
 ├── JSX
 │    ↓
 │  React Elements
 │
 ├── Components
 │
 ├── Rendering
 │
 ├── Reconciliation
 │
 ├── Fiber
 │    ├── current/WIP
 │    ├── child/sibling/return
 │    ├── lanes
 │    └── flags
 │
 ├── Hooks
 │    ├── useState
 │    ├── useEffect
 │    ├── useRef
 │    ├── useMemo
 │    └── useCallback
 │
 ├── Scheduling
 │    ├── lanes
 │    ├── transitions
 │    └── yielding
 │
 └── Memoization
      ├── useMemo
      ├── useCallback
      └── React.memo
```

We are now ready to move into the next major React area.

# Next Topic — `useContext` and Context Internals

We'll go deeper than "Context avoids prop drilling."

We'll understand:

```text
createContext()
      ↓
Provider
      ↓
context value
      ↓
Consumer dependency tracking
      ↓
Fiber dependencies
      ↓
context propagation
      ↓
which components become scheduled?
      ↓
why React.memo doesn't automatically save you from
context-driven renders
```

We'll also tackle the very common interview questions:

> Why does changing Context re-render consumers?

> Does every component under a Provider re-render?

> Does `React.memo` stop Context updates?

> Why does creating a new Context value object cause unnecessary renders?

> Context vs props vs global state management — what is actually different internally?

[1]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberBeginWork.js "react/packages/react-reconciler/src/ReactFiberBeginWork.js at main · react/react · GitHub"
[2]: https://react.dev/reference/react/useCallback?utm_source=chatgpt.com "useCallback – React"
[3]: https://react.dev/reference/react/useMemo?utm_source=chatgpt.com "useMemo – React"
[4]: https://gist.github.com/bgauryy/38bd4550bd010a8ec813253aacffa0a4?utm_source=chatgpt.com "React Advanced Concepts Used WIth Octocode MCP · GitHub"
[5]: https://react.dev/reference/react/memo?utm_source=chatgpt.com "memo – React"
[6]: https://react.dev/reference/react-compiler/directives/use-memo?utm_source=chatgpt.com "'use memo' directive – React"
[7]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberHooks.js "react/packages/react-reconciler/src/ReactFiberHooks.js at main · react/react · GitHub"
[8]: https://react.dev/reference/eslint-plugin-react-hooks/lints/use-memo?utm_source=chatgpt.com "use-memo – React"

