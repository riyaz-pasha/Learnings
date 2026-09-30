# Topic 14 — `useReducer` Internals

`useReducer` is one of the most important React Hooks for interviews because it exposes a very clean way to understand **state updates, action queues, reducers, Fiber, Hook linked lists, batching, lanes, and rendering**.

And there is a particularly important connection to what we already learned:

> **`useState` and `useReducer` are not two completely unrelated state mechanisms. `useState` is implemented using the same underlying reducer/update machinery.**

React's own documentation describes `useReducer` as similar to `useState`, with the difference that the state-update logic is moved into a reducer function. ([React][1])

---

# 1. What problem does `useReducer` solve?

Suppose we have a form:

```jsx
function Form() {
  const [name, setName] = useState("");
  const [age, setAge] = useState(0);
  const [isEditing, setIsEditing] = useState(false);
  const [error, setError] = useState(null);

  // ...
}
```

As state logic becomes more complicated, the component can end up containing many event handlers:

```jsx
function handleNameChange(name) {
  setName(name);
}

function handleAgeChange(age) {
  setAge(age);
}

function handleSubmit() {
  // ...
}
```

With a reducer, we move the **state transition logic** into one function:

```jsx
function reducer(state, action) {
  switch (action.type) {
    case "nameChanged":
      return {
        ...state,
        name: action.name
      };

    case "ageChanged":
      return {
        ...state,
        age: action.age
      };

    case "submitted":
      return {
        ...state,
        isEditing: false
      };

    default:
      throw new Error("Unknown action");
  }
}
```

Then:

```jsx
const [state, dispatch] = useReducer(reducer, initialState);
```

And event handlers describe **what happened**:

```jsx
dispatch({
  type: "nameChanged",
  name: "John"
});
```

This distinction is important:

```text
useState style

Event handler
     ↓
"set the state to X"


useReducer style

Event handler
     ↓
"this happened"
     ↓
action
     ↓
reducer
     ↓
new state
```

React's documentation recommends reducers when you want more structure around complex state-transition logic; reducers should be pure because React runs them as part of rendering. ([React][2])

---

# 2. Basic `useReducer`

```jsx
function reducer(state, action) {
  switch (action.type) {
    case "increment":
      return {
        count: state.count + 1
      };

    case "decrement":
      return {
        count: state.count - 1
      };

    default:
      throw new Error("Unknown action");
  }
}

function Counter() {
  const [state, dispatch] = useReducer(
    reducer,
    { count: 0 }
  );

  return (
    <>
      <p>{state.count}</p>

      <button onClick={() => dispatch({ type: "increment" })}>
        +
      </button>

      <button onClick={() => dispatch({ type: "decrement" })}>
        -
      </button>
    </>
  );
}
```

Three important pieces:

```text
state
  ↓
current state

dispatch
  ↓
send an action

reducer
  ↓
calculate next state
```

---

# 3. The reducer is just a function

At its simplest:

```js
function reducer(state, action) {
  return nextState;
}
```

There is nothing magical about the reducer itself.

For example:

```js
function reducer(state, action) {
  if (action.type === "increment") {
    return state + 1;
  }

  return state;
}
```

You could even call it manually:

```js
const nextState = reducer(
  10,
  { type: "increment" }
);

console.log(nextState); // 11
```

React's job is essentially to determine:

> When should this reducer run, with what state, and with which queued actions?

That's where the internals become interesting.

---

# 4. `useReducer` and `useState` are closely related

Recall what we learned for `useState`.

Conceptually:

```jsx
const [count, setCount] = useState(0);
```

When you do:

```jsx
setCount(1);
```

React places an update into a queue.

During a later render React processes that queue.

`useReducer` follows the same broad architecture:

```jsx
const [state, dispatch] = useReducer(
  reducer,
  initialState
);
```

Then:

```jsx
dispatch({ type: "increment" });
```

also creates an update that enters a Hook-associated queue.

So:

```text
useState
   ↓
Hook
   ↓
Update Queue
   ↓
Render
   ↓
Process updates


useReducer
   ↓
Hook
   ↓
Update Queue
   ↓
Render
   ↓
Process updates
   ↓
Reducer(action)
```

This is one of the biggest things to remember.

---

# 5. The surprising part: `useState` uses reducer machinery

Earlier we saw the conceptual implementation:

```js
function basicStateReducer(state, action) {
  return typeof action === "function"
    ? action(state)
    : action;
}
```

Then:

```js
function updateState(initialState) {
  return updateReducer(
    basicStateReducer,
    initialState
  );
}
```

So you can think of:

```jsx
useState(initialState)
```

as using a built-in reducer:

```js
(state, action) =>
  typeof action === "function"
    ? action(state)
    : action
```

Whereas:

```jsx
useReducer(myReducer, initialState)
```

uses:

```js
myReducer
```

instead.

So conceptually:

```text
useState
   ↓
updateReducer(basicStateReducer, ...)


useReducer
   ↓
updateReducer(yourReducer, ...)
```

That's why learning `useReducer` internals helps reinforce `useState`.

The exact internal implementation changes across React versions, so treat this as the architectural relationship rather than assuming a particular private function signature will remain unchanged. The current reconciler continues to implement Hooks in `ReactFiberHooks.js`. ([Gist][3])

---

# 6. The Hook is still stored on the Fiber

You already learned:

```text
Function Component Fiber
        │
        └── memoizedState
               │
               ↓
           Hook linked list
```

Suppose:

```jsx
function Counter() {
  const [state, dispatch] = useReducer(reducer, initialState);

  const [name, setName] = useState("");

  useEffect(() => {
    // ...
  }, []);

  return ...;
}
```

Conceptually:

```text
Counter Fiber
   │
   └── memoizedState
         │
         ▼
       Hook #1
         │
         ▼
       Hook #2
         │
         ▼
       Hook #3
```

And the first Hook might conceptually contain:

```text
Hook #1
 ├── memoizedState → current reducer state
 ├── baseState
 ├── baseQueue
 ├── queue
 └── next
```

The queue contains pending state updates.

So:

```text
Fiber
  ↓
Hook
  ↓
UpdateQueue
  ↓
Updates
```

---

# 7. What does `useReducer()` return?

Exactly two values:

```jsx
const [state, dispatch] = useReducer(
  reducer,
  initialState
);
```

### `state`

Current state for this render.

### `dispatch`

Stable function used to enqueue an action.

React's current documentation specifies that `useReducer` returns the current state and a `dispatch` function. ([React][1])

---

# 8. What happens when `dispatch()` is called?

Suppose:

```jsx
dispatch({
  type: "increment"
});
```

Important:

**The reducer does not necessarily execute immediately inside the event handler.**

Think:

```text
dispatch(action)
      ↓
create/enqueue update
      ↓
associate update with Hook queue
      ↓
schedule Fiber work
      ↓
React renders later
      ↓
process queued updates
      ↓
call reducer
      ↓
calculate next state
```

This is similar to `setState`.

React's documentation explicitly says calling `dispatch` requests another render rather than immediately changing the state variable in the currently executing code. ([React][1])

---

# 9. State is still a snapshot

Consider:

```jsx
function Counter() {
  const [state, dispatch] = useReducer(reducer, {
    count: 0
  });

  function handleClick() {
    console.log(state.count);

    dispatch({ type: "increment" });

    console.log(state.count);
  }

  return (
    <button onClick={handleClick}>
      {state.count}
    </button>
  );
}
```

Both logs from that particular handler can see:

```text
0
0
```

Why?

Because:

```text
current render
      ↓
state.count = 0
```

The dispatch requests a future render.

It doesn't mutate the JavaScript variable belonging to the current render.

React documents this explicitly for `useReducer`: state behaves like a snapshot, and the dispatch updates the state for the next render. ([React][1])

---

# 10. The action is not the next state

This distinction is very important.

You do:

```jsx
dispatch({
  type: "increment"
});
```

The action is:

```js
{
  type: "increment"
}
```

It is **not**:

```js
{
  count: 1
}
```

The reducer determines that:

```text
previous state
      +
action
      ↓
next state
```

For example:

```js
function reducer(state, action) {
  switch (action.type) {
    case "increment":
      return {
        count: state.count + 1
      };

    default:
      return state;
  }
}
```

So:

```text
state = { count: 10 }

action = { type: "increment" }

                    ↓

reducer(state, action)

                    ↓

nextState = { count: 11 }
```

---

# 11. Why actions describe events

React recommends making actions describe what happened rather than issuing low-level state commands.

Prefer:

```js
dispatch({
  type: "itemAdded",
  item
});
```

instead of something like:

```js
dispatch({
  type: "setItems",
  items: [...]
});
```

The first says:

```text
USER EVENT:
an item was added
```

The reducer decides what that means for state.

This creates a useful separation:

```text
UI / event handler
       ↓
WHAT happened?

       ↓

Action

       ↓

Reducer
       ↓
HOW should state change?
```

React's reducer documentation emphasizes this distinction between describing an interaction with an action and putting the state transition logic inside the reducer. ([React][2])

---

# 12. Internally: mount phase

Let's see what happens the first time:

```jsx
useReducer(reducer, initialState)
```

Conceptually:

```text
Component renders for first time
          ↓
React's Hook dispatcher
          ↓
mountReducer()
          ↓
create Hook
          ↓
initialize state
          ↓
create UpdateQueue
          ↓
create dispatch
          ↓
return [state, dispatch]
```

Simplified:

```js
function mountReducer(reducer, initialArg, init) {
  const hook = mountWorkInProgressHook();

  const initialState =
    init !== undefined
      ? init(initialArg)
      : initialArg;

  hook.memoizedState = initialState;
  hook.baseState = initialState;

  const queue = {
    pending: null,
    dispatch: null,
    lastRenderedReducer: reducer,
    lastRenderedState: initialState
  };

  hook.queue = queue;

  const dispatch = /* dispatch function */;

  queue.dispatch = dispatch;

  return [hook.memoizedState, dispatch];
}
```

This is a **conceptual simplification** of the internal architecture.

The key things are:

```text
Hook created
state stored
queue created
dispatch created
```

---

# 13. The update queue

This is one of the most important internals.

Suppose:

```jsx
dispatch({ type: "increment" });
dispatch({ type: "increment" });
dispatch({ type: "increment" });
```

React doesn't conceptually throw away previous actions.

Instead, updates enter a queue associated with the Hook.

Conceptually:

```text
Hook
 │
 └── queue
       │
       └── pending updates
             │
             ├── Update 1
             ├── Update 2
             └── Update 3
```

Then during render React processes them:

```text
initialState = 0

Update #1
reducer(0, increment)
      ↓
1

Update #2
reducer(1, increment)
      ↓
2

Update #3
reducer(2, increment)
      ↓
3
```

Final:

```text
state = 3
```

This is why understanding queues is essential to understanding React state.

---

# 14. Compare that with `useState`

Suppose:

```jsx
setCount(count + 1);
setCount(count + 1);
setCount(count + 1);
```

From a render where:

```text
count = 0
```

the action passed to the underlying state reducer is effectively:

```text
1
1
1
```

So processing:

```text
0 → 1
1 → 1
1 → 1
```

Final:

```text
1
```

But with:

```jsx
setCount(c => c + 1);
setCount(c => c + 1);
setCount(c => c + 1);
```

the queued actions are functions:

```text
f
f
f
```

and:

```text
0
 ↓ f
1
 ↓ f
2
 ↓ f
3
```

That same queue/reducer model is exactly why `useReducer` feels so natural once you understand `useState`.

---

# 15. Processing a reducer queue

Conceptually, React does something like:

```js
let newState = baseState;

for (const update of updates) {
  newState = reducer(
    newState,
    update.action
  );
}
```

For:

```js
const updates = [
  { type: "increment" },
  { type: "increment" },
  { type: "decrement" }
];
```

we get:

```text
0
 ↓ increment
1
 ↓ increment
2
 ↓ decrement
1
```

Final:

```text
1
```

The real reconciler has much more logic around:

```text
lanes
skipped updates
baseQueue
rebasing
render-phase updates
eager state
concurrent rendering
optimistic/revert-related updates
```

but this simple model gives you the core algorithm.

---

# 16. Why `baseState` and `baseQueue` exist

This connects directly to **lanes**.

Imagine React has:

```text
high-priority update
low-priority update
```

and the current render is only processing the relevant lane.

Some updates may need to be skipped temporarily.

React needs to preserve enough information to correctly replay those updates later.

That's why Hook internals have concepts such as:

```text
memoizedState
baseState
baseQueue
```

Think:

```text
memoizedState
    ↓
state produced for this render

baseState
    ↓
state from which remaining updates should be replayed

baseQueue
    ↓
updates that still matter for future processing
```

This is one of the places where:

```text
useReducer
    ↓
Hook queue
    ↓
lanes
    ↓
concurrent rendering
```

all meet.

---

# 17. Why reducers must be pure

Consider:

```jsx
function reducer(state, action) {
  fetch("/api/data");

  return {
    ...state,
    loading: false
  };
}
```

This is wrong.

Why?

Because reducers execute during rendering.

Rendering may be:

```text
started
   ↓
interrupted
   ↓
restarted
```

or may involve more than one invocation in development scenarios.

You don't want:

```text
render starts
 ↓
API request sent

render restarted
 ↓
API request sent again
```

Therefore:

```text
Reducer
  ↓
pure state transition
```

Not:

```text
Reducer
  ↓
API calls
  ↓
timers
  ↓
DOM manipulation
  ↓
side effects
```

React's documentation explicitly says reducers should be pure and should not perform side effects. ([React][2])

---

# 18. Reducer purity

A pure reducer:

```js
function reducer(state, action) {
  switch (action.type) {
    case "increment":
      return {
        ...state,
        count: state.count + 1
      };

    default:
      return state;
  }
}
```

Same input:

```text
state + action
```

should produce the same result.

Bad:

```js
function reducer(state, action) {
  Math.random();

  return {
    ...state,
    value: Math.random()
  };
}
```

Now the same inputs can produce different outputs.

That violates the basic model React relies upon during rendering.

---

# 19. Never mutate reducer state

Bad:

```js
function reducer(state, action) {
  state.count++;

  return state;
}
```

You're returning the exact same object:

```text
oldState
   │
   └── mutated
        │
        ↓
newState

same object
```

React uses `Object.is` comparisons to determine whether a new state value is equal to the previous one; returning the same object after mutating it can therefore prevent the expected update. React explicitly documents this as a common reducer bug. ([React][1])

Correct:

```js
function reducer(state, action) {
  return {
    ...state,
    count: state.count + 1
  };
}
```

Now:

```text
old object
    ↓
new object
```

and:

```js
Object.is(oldState, newState)
```

is:

```text
false
```

---

# 20. `useReducer` with lazy initialization

You can write:

```jsx
const [state, dispatch] = useReducer(
  reducer,
  username,
  createInitialState
);
```

where:

```js
function createInitialState(username) {
  return {
    username,
    tasks: expensiveCalculation(username)
  };
}
```

React calls:

```js
createInitialState(username)
```

to obtain the initial state.

This is useful when initialization is expensive.

Compare:

```jsx
useReducer(
  reducer,
  createInitialState(username)
);
```

with:

```jsx
useReducer(
  reducer,
  username,
  createInitialState
);
```

The first version calls `createInitialState` during each component execution, even though the result is only used for initialization.

The second gives React the initializer function itself. React documents this as the way to avoid recreating expensive initial state calculations on later renders. ([React][1])

---

# 21. `init` vs reducer

Don't confuse:

```text
initializer
```

with:

```text
reducer
```

Initializer:

```js
function init(initialArg) {
  return initialState;
}
```

Runs to establish the initial state.

Reducer:

```js
function reducer(state, action) {
  return nextState;
}
```

Handles state transitions.

So:

```text
initialArg
    ↓
init()
    ↓
initialState


initialState + action
    ↓
reducer()
    ↓
nextState
```

---

# 22. Why `dispatch` identity is stable

Consider:

```jsx
function Counter() {
  const [state, dispatch] =
    useReducer(reducer, initialState);

  // ...
}
```

On subsequent renders, React returns the same dispatch function associated with that Hook's update queue.

Conceptually:

```text
Render #1

Hook.queue
   ↓
dispatch A


Render #2

same Hook.queue
   ↓
dispatch A
```

rather than:

```text
Render #1 → dispatch A
Render #2 → dispatch B
Render #3 → dispatch C
```

This matters when passing dispatch to children or using it in dependency scenarios.

---

# 23. `dispatch` doesn't need the component's current state

This is a beautiful internal design.

Suppose you have:

```jsx
dispatch({
  type: "increment"
});
```

You don't write:

```jsx
dispatch({
  type: "increment",
  currentState: state
});
```

The dispatch function is associated with the Hook's update queue.

Later React uses that queue while rendering the Fiber.

Conceptually:

```text
dispatch
   │
   └── Hook queue
          │
          └── pending updates
```

Then:

```text
next render
    ↓
retrieve Hook
    ↓
retrieve queue
    ↓
process updates
    ↓
reducer
```

This is why dispatch can be stable even though the component renders repeatedly.

---

# 24. `dispatch` doesn't return the new state

This:

```js
const result = dispatch({
  type: "increment"
});
```

doesn't give you:

```text
nextState
```

`dispatch` doesn't return the new state.

The new state becomes available on a future render:

```jsx
function Counter() {
  const [state, dispatch] =
    useReducer(reducer, initialState);

  // state here represents this render's snapshot
}
```

React's API documentation explicitly specifies that dispatch functions have no return value. ([React][4])

---

# 25. Batching works with `useReducer`

Suppose:

```jsx
function handleClick() {
  dispatch({ type: "increment" });
  dispatch({ type: "increment" });
  dispatch({ type: "increment" });
}
```

React doesn't normally need to perform three completely separate screen updates.

The actions are queued and React can process them during the resulting render.

Conceptually:

```text
click
 │
 ├── dispatch A
 ├── dispatch B
 └── dispatch C
       │
       ↓
   queue updates
       │
       ↓
   render once
       │
       ├── reducer(A)
       ├── reducer(B)
       └── reducer(C)
       │
       ↓
   commit
```

React documents that state updates are batched. ([React][4])

---

# 26. `useReducer` + lanes

Now connect it to the scheduling topic.

Each state update has scheduling information internally.

Conceptually:

```text
dispatch(action)
       ↓
Update
 ├── action
 ├── lane
 └── next
```

For example:

```text
Update A → urgent lane
Update B → transition lane
```

A particular render may process one set of lanes and skip/rebase updates belonging to work that isn't currently being rendered.

That is one reason reducer internals are more complicated than:

```js
queue.push(action);
```

React needs to support the scheduling model we've already covered.

---

# 27. Reducer execution happens during render

This is critical.

Imagine:

```jsx
dispatch({ type: "increment" });
```

Then React eventually starts rendering:

```text
beginWork
   ↓
function component
   ↓
renderWithHooks
   ↓
useReducer
   ↓
updateReducer
   ↓
process update queue
   ↓
reducer(state, action)
```

So:

```text
dispatch
```

does not mean:

```text
immediately execute reducer
```

Think:

```text
dispatch
  =
enqueue an action + request React work
```

Then:

```text
render
  =
process queued actions + calculate state
```

That distinction is extremely valuable in interviews.

---

# 28. Why reducer can run more than you expect

Because reducer execution belongs to rendering, you should not treat it as a one-shot event callback.

For example, development behavior can invoke reducer/initializer logic more than once to help expose impurities. React's documentation notes that under Strict Mode, React may call the reducer and initializer twice in development, ignoring one result. ([React][4])

Therefore:

```js
function reducer(state, action) {
  console.log("reducer");

  return nextState;
}
```

should not be treated as:

> "`reducer` runs exactly once per dispatch."

That's not a safe assumption.

---

# 29. A complete example

Let's trace this:

```jsx
const initialState = {
  count: 0
};

function reducer(state, action) {
  switch (action.type) {
    case "increment":
      return {
        ...state,
        count: state.count + 1
      };

    case "decrement":
      return {
        ...state,
        count: state.count - 1
      };

    default:
      throw new Error("Unknown action");
  }
}

function Counter() {
  const [state, dispatch] =
    useReducer(reducer, initialState);

  return (
    <>
      <div>{state.count}</div>

      <button
        onClick={() =>
          dispatch({ type: "increment" })
        }
      >
        +
      </button>
    </>
  );
}
```

Initial render:

```text
Counter Fiber
    ↓
renderWithHooks
    ↓
useReducer
    ↓
mountReducer
    ↓
state = { count: 0 }
    ↓
return [state, dispatch]
```

Then click:

```text
dispatch({type: "increment"})
```

Internally conceptually:

```text
Action
  ↓
Update object
  ↓
Hook queue
  ↓
schedule Fiber
```

Then render:

```text
Counter
   ↓
useReducer
   ↓
retrieve Hook
   ↓
retrieve queue
   ↓
process Update
   ↓
reducer(
    {count: 0},
    {type: "increment"}
   )
   ↓
{count: 1}
```

Then:

```text
render output
   ↓
reconciliation
   ↓
commit
   ↓
DOM displays 1
```

---

# 30. `useState` vs `useReducer`

| `useState`                                | `useReducer`                               |
| ----------------------------------------- | ------------------------------------------ |
| Simple state updates                      | Complex state transitions                  |
| Setter API                                | Dispatch API                               |
| `setCount(10)`                            | `dispatch({type: "set", value: 10})`       |
| Update logic often near event handlers    | Update logic centralized in reducer        |
| React uses basic state reducer internally | You provide reducer                        |
| Good for independent/simple state         | Good when transitions are numerous/related |

The important interview point:

> Don't say "`useReducer` is faster than `useState`."

That's not the reason to choose it.

React's documentation treats them as closely related mechanisms, and notes that you can convert between them; the main distinction is how you organize the state-update logic. ([React][2])

---

# 31. When should you use `useReducer`?

Good example:

```text
Form state
 ├── loading
 ├── error
 ├── name
 ├── email
 └── submitted
```

with transitions such as:

```text
SUBMIT
SUCCESS
FAILURE
RESET
FIELD_CHANGED
```

A reducer creates a state machine-like structure:

```text
           SUBMIT
              ↓
        ┌────────────┐
        │  loading   │
        └────────────┘
          ↙        ↘
     SUCCESS       FAILURE
        ↓             ↓
    success          error
```

This can be much easier to reason about than many loosely connected setters.

---

# 32. Don't overuse reducers

This:

```jsx
const [isOpen, dispatch] = useReducer(
  (state, action) => !state,
  false
);
```

for a simple boolean is unnecessary.

A plain:

```jsx
const [isOpen, setIsOpen] = useState(false);
```

is clearer.

So:

```text
Simple transition
    ↓
useState

Complex related transitions
    ↓
useReducer
```

---

# 33. A reducer can be shared/tested independently

One major architectural benefit:

```js
export function reducer(state, action) {
  // ...
}
```

Then test:

```js
expect(
  reducer(
    { count: 0 },
    { type: "increment" }
  )
).toEqual({ count: 1 });
```

The reducer doesn't require:

```text
DOM
React
Component
Browser
```

because it's just a pure function.

React's documentation specifically calls out that reducers can be tested separately because they don't depend on the component itself. ([React][2])

---

# 34. Context + `useReducer`

Now combine our previous two topics.

This is a very common React architecture:

```jsx
const CounterStateContext = createContext(null);
const CounterDispatchContext = createContext(null);

function CounterProvider({ children }) {
  const [state, dispatch] =
    useReducer(reducer, initialState);

  return (
    <CounterStateContext value={state}>
      <CounterDispatchContext value={dispatch}>
        {children}
      </CounterDispatchContext>
    </CounterStateContext>
  );
}
```

Now:

```text
useReducer
    ↓
owns state transitions

Context
    ↓
distributes state/dispatch
```

Consumers can do:

```jsx
const state = useContext(CounterStateContext);
```

and:

```jsx
const dispatch = useContext(CounterDispatchContext);
```

This leads into more advanced React architecture patterns.

---

# 35. The complete internal architecture

Now put everything together:

```text
                    Component Fiber
                           │
                           ↓
                     Hook linked list
                           │
                           ↓
                    useReducer Hook
                           │
                 ┌─────────┴─────────┐
                 ↓                   ↓
           memoizedState          queue
                                     │
                              ┌──────┴──────┐
                              ↓             ↓
                           Update        Update
                              │             │
                              └──────┬──────┘
                                     ↓
                                  dispatch
                                     │
                                     ↓
                               schedule work
                                     │
                                     ↓
                                  lanes
                                     │
                                     ↓
                               Fiber work loop
                                     │
                                     ↓
                              renderWithHooks
                                     │
                                     ↓
                               updateReducer
                                     │
                                     ↓
                        process pending updates
                                     │
                                     ↓
                              reducer(state, action)
                                     │
                                     ↓
                              new memoizedState
                                     │
                                     ↓
                              reconciliation
                                     │
                                     ↓
                                  commit
```

That's the internal model you should have in your head.

---

# 36. The most important misconception

Don't say:

> "`dispatch()` changes the state."

More precise:

> **`dispatch()` queues an action and schedules React to perform work. During a subsequent render, React processes the queued action(s) through the reducer and derives the next state.**

That distinction demonstrates real understanding.

---

# 37. Interview questions you should know

### "What is `useReducer`?"

> A Hook for managing state through a reducer function. Instead of directly specifying the next state from event handlers, components dispatch actions describing what happened, and the reducer calculates the next state.

### "How is `useReducer` different from `useState`?"

> The main difference is the organization of update logic. `useState` exposes a setter and uses React's basic state reducer internally, while `useReducer` allows the application to provide a reducer that handles actions and state transitions.

### "Does dispatch immediately call the reducer?"

> Not as part of the currently executing event handler. Dispatch queues an update and schedules work; the reducer is used while React processes that update during rendering.

### "Where is reducer state stored?"

> React stores Hook information on the component's Fiber. The Hook contains the current state and an update queue associated with that Hook.

### "Why must reducers be pure?"

> Because reducers execute during rendering, and React's rendering can be restarted or invoked more than once in development. Side effects inside reducers can therefore execute unexpectedly.

### "Why do we return a new object from a reducer?"

> To preserve immutability and ensure the new state has a different identity when the state has changed. React uses `Object.is` to determine whether the resulting state is equal to the previous state. ([React][1])

### "Can multiple dispatches be processed in one render?"

> Yes. React queues the updates and can process multiple actions during the resulting render, consistent with React's batching behavior. ([React][4])

---

# 38. The 30-second interview answer

A strong answer:

> **"`useReducer` is a Hook for managing state through a pure reducer function. The component dispatches actions, and React associates those updates with the Hook's internal update queue stored through the component's Fiber. Dispatching doesn't immediately mutate the current render's state; it queues an update and schedules work. During rendering, React processes the queued updates in order, passing the accumulated state and each action to the reducer to calculate the next state. The resulting state is stored for that render, and React then performs reconciliation and commit. Internally, `useState` uses closely related reducer/update-queue machinery, with a built-in basic state reducer."**

That is an excellent answer for a React internals interview.

---

## The key mental model

Memorize this:

```text
useReducer()
    ↓
Hook on Fiber
    ↓
[state + update queue]
    ↓
dispatch(action)
    ↓
queue update
    ↓
schedule React work
    ↓
render
    ↓
process updates sequentially
    ↓
reducer(previousState, action)
    ↓
new state
    ↓
reconciliation
    ↓
commit
```

And the most important relationship from this lesson:

```text
useState
   ↓
basicStateReducer
   ↓
same general update/reducer machinery

useReducer
   ↓
yourReducer
   ↓
same general update/reducer machinery
```

([React][1])

### Next Topic → React Event System & Synthetic Events

We'll go deep into **how an `onClick` actually travels from the browser event to your React handler**, event delegation, propagation, capture/bubble phases, `SyntheticEvent`, React's modern event system, and how events eventually trigger Fiber updates.

[1]: https://react.dev/reference/react/useReducer?utm_source=chatgpt.com "useReducer – React"
[2]: https://react.dev/learn/extracting-state-logic-into-a-reducer?utm_source=chatgpt.com "Extracting State Logic into a Reducer – React"
[3]: https://gist.github.com/tomtheisen/7a59a1c2e6bffbfae9a799bc368047ae?utm_source=chatgpt.com "React Journey · GitHub"
[4]: https://uk.react.dev/reference/react/useReducer?utm_source=chatgpt.com "useReducer – React"

