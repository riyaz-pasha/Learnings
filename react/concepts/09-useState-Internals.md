# Topic 9 — `useState` Internals: How React Actually Stores and Updates State

This is one of the **highest-value React interview topics**.

We have already said:

```jsx
const [count, setCount] = useState(0);
```

but now we're going to answer the questions behind it:

> Where does `count` actually live?

> How does React know which state belongs to this particular `useState` call?

> Why does Hook order matter?

> What exactly is inside a Hook?

> What happens internally when `setCount(1)` is called?

> Why does `setCount(count + 1)` differ from `setCount(c => c + 1)`?

The current React source makes the architecture quite explicit: Hooks are stored as a linked list on the Fiber's `memoizedState`, and each Hook contains `memoizedState`, `baseState`, `baseQueue`, `queue`, and `next`. React also maintains the currently rendering Fiber and current/work-in-progress Hook pointers while a component is being rendered. ([GitHub][1])

---

# 1. Start with the mystery

Consider:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    return (
        <button onClick={() => setCount(count + 1)}>
            {count}
        </button>
    );
}
```

It looks like:

```text
useState
 ↓
some variable
```

But that's not what's happening.

Remember that the component executes again on every render:

```text
Render #1
Counter()
   ↓
count = 0

Render #2
Counter()
   ↓
count = 1
```

The function invocation itself doesn't preserve the local variable.

So React needs persistent storage **outside the function invocation**.

That storage is associated with the component's Fiber.

Conceptually:

```text
Fiber
  │
  └── memoizedState
          │
          ▼
        Hook
          │
          ▼
        state
```

The current source literally comments that Hooks are stored as a linked list on the Fiber's `memoizedState` field. ([GitHub][1])

---

# 2. The first major discovery: Hooks form a linked list

Suppose you have:

```jsx
function Profile() {
    const [name, setName] = useState("Alice");
    const [age, setAge] = useState(25);
    const ref = useRef(null);

    return ...;
}
```

React conceptually stores:

```text
Profile Fiber
     │
     └── memoizedState
             │
             ▼
          Hook #1
          name = "Alice"
             │
             ▼ next
          Hook #2
          age = 25
             │
             ▼ next
          Hook #3
          ref = ...
             │
             ▼
            null
```

The current `Hook` type contains:

```text
memoizedState
baseState
baseQueue
queue
next
```

and `next` points to the next Hook. ([GitHub][1])

This is the key to understanding the Rules of Hooks.

---

# 3. Why does React need a list?

Because this:

```jsx
useState(0);
useState(false);
useRef(null);
```

doesn't provide React with a variable name that identifies each call.

At runtime, React has to answer:

```text
first useState → which state?
second useState → which state?
useRef → which Hook data?
```

It solves this by maintaining Hook positions.

Conceptually:

```text
Hook #1
Hook #2
Hook #3
```

And during every render, React walks those Hooks in the same order.

---

# 4. This is why the Rules of Hooks exist

Invalid:

```jsx
function Component() {
    if (condition) {
        useState(0);
    }

    useEffect(() => {});
}
```

Why?

Imagine first render:

```text
condition = true
```

React sees:

```text
Hook #1 → useState
Hook #2 → useEffect
```

Next render:

```text
condition = false
```

React sees:

```text
Hook #1 → useEffect
```

Now the correspondence is broken.

React could end up treating:

```text
old Hook #1 = state
```

as:

```text
new Hook #1 = effect
```

That's disastrous.

So Hooks must be called in the same order every render.

The current development implementation explicitly tracks Hook types from the initial render and compares subsequent Hook ordering; it warns when the order changes. ([GitHub][1])

---

# 5. Mount vs update

React effectively has two different Hook paths:

```text
First render
    ↓
mount logic

Later renders
    ↓
update logic
```

During `renderWithHooks`, React chooses a Mount or Update dispatcher based on whether the current Fiber has existing Hook state. The production code selects `HooksDispatcherOnMount` when there is no current Fiber or no existing stateful Hook list, otherwise `HooksDispatcherOnUpdate`. ([GitHub][1])

Conceptually:

```text
             renderWithHooks
                    │
             ┌──────┴──────┐
             │             │
           mount         update
             │             │
         create hooks   reuse hooks
```

---

# 6. `renderWithHooks`

This is one of the most important functions to know.

The current source does roughly this at the beginning:

```text
renderLanes = nextRenderLanes
currentlyRenderingFiber = workInProgress
```

It then prepares the Fiber's Hook-related state and selects the appropriate dispatcher before invoking the component. ([GitHub][1])

Conceptually:

```javascript
function renderWithHooks(
    current,
    workInProgress,
    Component,
    props,
    lanes
) {
    currentlyRenderingFiber = workInProgress;

    selectMountOrUpdateDispatcher();

    const children = Component(props);

    finishHooks();

    return children;
}
```

That's simplified, but architecturally accurate.

---

# 7. What's `currentlyRenderingFiber`?

The current source has:

```javascript
let currentlyRenderingFiber;
```

This identifies:

> **The Fiber whose function component is currently executing.**

During `renderWithHooks`:

```text
currentlyRenderingFiber
        ↓
     Counter
```

This matters because when your component calls:

```jsx
useState(0)
```

React needs to know:

> "Which component's Hook list am I modifying?"

The current source sets `currentlyRenderingFiber = workInProgress` before running the component. ([GitHub][1])

---

# 8. There is also `workInProgressHook`

React maintains another internal pointer:

```javascript
let workInProgressHook;
```

Conceptually:

```text
currentlyRenderingFiber
        │
        ▼
Hook #1 → Hook #2 → Hook #3
        ↑
workInProgressHook
```

As Hooks are called, the pointer advances.

The current source initializes both `currentHook` and `workInProgressHook` bookkeeping, and `mountWorkInProgressHook` appends new Hook nodes to the WIP Fiber's Hook list. ([GitHub][1])

---

# 9. First render: creating the Hook list

Consider:

```jsx
function Counter() {
    const [count, setCount] = useState(0);
    const [name, setName] = useState("Alice");

    return ...;
}
```

During the initial render:

```text
Counter Fiber
    │
    └── memoizedState = null
```

React calls the first `useState`.

Conceptually:

```text
mountWorkInProgressHook()
```

creates:

```text
Hook #1
```

Then the second `useState` creates:

```text
Hook #2
```

Result:

```text
Counter Fiber
     │
     ▼
Hook #1
  memoizedState = 0
     │
     ▼
Hook #2
  memoizedState = "Alice"
     │
     ▼
   null
```

The actual `mountWorkInProgressHook` implementation creates the Hook object and either stores it as the Fiber's first `memoizedState` or appends it to the current WIP Hook's `next`. ([GitHub][1])

---

# 10. Simplified `mountWorkInProgressHook`

The actual current source is essentially:

```javascript
function mountWorkInProgressHook() {
    const hook = {
        memoizedState: null,
        baseState: null,
        baseQueue: null,
        queue: null,
        next: null
    };

    if (workInProgressHook === null) {
        currentlyRenderingFiber.memoizedState =
            workInProgressHook = hook;
    } else {
        workInProgressHook =
            workInProgressHook.next = hook;
    }

    return workInProgressHook;
}
```

This mirrors the current implementation's core structure. ([GitHub][1])

Notice something extremely important:

```text
React isn't storing:

state1
state2
state3
```

It's storing:

```text
Hook → Hook → Hook
```

---

# 11. What does each Hook contain?

Let's inspect:

```javascript
{
    memoizedState,
    baseState,
    baseQueue,
    queue,
    next
}
```

### `memoizedState`

The state/result associated with that Hook for the current processed render.

For `useState`:

```text
memoizedState = current state
```

### `baseState`

Base state used when React needs to process queued updates while some updates may have been skipped for the current render.

### `baseQueue`

Base update information retained for later renders.

### `queue`

The queue/dispatch information for updates to this Hook.

### `next`

Next Hook in the linked list.

These fields are directly present in the current `Hook` type. ([GitHub][1])

---

# 12. `useState` has another queue

Don't confuse:

```text
Hook linked list
```

with:

```text
State update queue
```

These are different structures.

Conceptually:

```text
Fiber
  │
  └── Hook list
       │
       ├── Hook #1
       │     └── queue → updates
       │
       ├── Hook #2
       │     └── queue → updates
       │
       └── Hook #3
```

So:

```text
Hook list
```

identifies the individual Hooks.

And:

```text
queue
```

stores updates for one particular state/reducer Hook.

The current source's `UpdateQueue` includes `pending`, `lanes`, `dispatch`, `lastRenderedReducer`, and `lastRenderedState`. ([GitHub][1])

---

# 13. The `useState` update queue

Suppose:

```jsx
const [count, setCount] = useState(0);
```

Then conceptually:

```text
Hook #1
 ├── memoizedState = 0
 └── queue
      ├── dispatch
      ├── pending
      ├── lastRenderedReducer
      └── lastRenderedState
```

And when:

```jsx
setCount(...)
```

is called, React creates an update object.

The current `Update` type contains:

```text
lane
revertLane
action
hasEagerState
eagerState
next
gesture
```

in the current source. ([GitHub][1])

Some of these are internal optimizations/features we don't need to use directly.

---

# 14. The simplest update object

Conceptually, imagine:

```javascript
const update = {
    action: 1,
    lane: someLane,
    next: null
};
```

or:

```javascript
const update = {
    action: count => count + 1,
    lane: someLane,
    next: null
};
```

The critical field for understanding `useState` is:

```text
action
```

because it represents the requested state update.

---

# 15. Direct value update

When you write:

```jsx
setCount(5);
```

the action is conceptually:

```text
action = 5
```

During state processing, React's basic state reducer does:

```javascript
function basicStateReducer(state, action) {
    return typeof action === "function"
        ? action(state)
        : action;
}
```

So:

```text
action = 5
```

means:

```text
nextState = 5
```

The current React implementation uses this reducer semantics for `useState`. ([React][2])

---

# 16. Updater function

When you write:

```jsx
setCount(c => c + 1);
```

the action is:

```text
action = function
```

Then:

```javascript
basicStateReducer(state, action)
```

does:

```javascript
action(state)
```

So:

```text
state = 5

action = c => c + 1

result = 6
```

This is why updater functions compose correctly when multiple updates are queued. React's official documentation explains that updater functions are placed in the queue and then applied in order during the next render. ([React][3])

---

# 17. This explains the famous `+3` example

Suppose:

```jsx
const [number, setNumber] = useState(0);

function handleClick() {
    setNumber(number + 1);
    setNumber(number + 1);
    setNumber(number + 1);
}
```

The current render has:

```text
number = 0
```

Therefore:

```text
setNumber(0 + 1)
setNumber(0 + 1)
setNumber(0 + 1)
```

becomes:

```text
action = 1
action = 1
action = 1
```

The queue effectively contains:

```text
[replace with 1]
[replace with 1]
[replace with 1]
```

Final:

```text
1
```

React's official documentation explains exactly this behavior. ([React][3])

---

# 18. Updater-function version

Now:

```jsx
setNumber(n => n + 1);
setNumber(n => n + 1);
setNumber(n => n + 1);
```

Queue:

```text
n => n + 1
n => n + 1
n => n + 1
```

Starting from:

```text
0
```

React processes:

```text
0
 ↓
1
 ↓
2
 ↓
3
```

Final state:

```text
3
```

This is precisely the behavior documented by React. ([React][3])

---

# 19. What does "queue" actually mean?

Here's a simplified model.

Suppose:

```text
current state = 0
```

After:

```jsx
setCount(c => c + 1);
setCount(c => c + 1);
```

we could imagine:

```text
Hook
 ├── memoizedState = 0
 └── queue
      │
      ▼
   update1 → update2
```

Each update contains:

```text
action
lane
next
```

The actual current implementation is more sophisticated because queues support priority, skipped updates, optimistic/revert behavior, eager state, and concurrent processing. ([GitHub][1])

But this simplified model is excellent for interviews.

---

# 20. Why is the queue circular?

This is an implementation detail worth knowing.

React's current update queues for Hooks use a pending representation where updates can be linked in a circular fashion. The current `UpdateQueue` has a `pending` pointer, and the state-processing code splices pending updates into the base queue during rendering. ([GitHub][1])

Conceptually:

```text
pending
  │
  ▼
Update A → Update B → Update C
  ↑                    │
  └────────────────────┘
```

Why circular?

It makes insertion at the end and merging pending updates convenient.

This is one of those implementation details that interviewers sometimes ask after you've explained the normal queue.

---

# 21. Why use a queue rather than just storing the latest value?

Because multiple updates may happen before the next render.

Example:

```jsx
setCount(c => c + 1);
setCount(c => c + 1);
setCount(c => c + 1);
```

React needs all three operations.

If it merely stored:

```text
state = ?
```

it could lose the sequence.

The queue preserves the operations:

```text
update1
update2
update3
```

and React processes them in order.

That's why:

```text
state
```

and:

```text
pending updates
```

are conceptually different things.

---

# 22. State update flow

Now we can trace:

```jsx
setCount(5)
```

from beginning to end.

Conceptually:

```text
setCount(5)
    ↓
dispatch function
    ↓
create Update
    ↓
assign lane
    ↓
enqueue update into Hook queue
    ↓
mark Fiber/root with pending work
    ↓
schedule rendering
    ↓
render component
    ↓
process Hook queue
    ↓
calculate next state = 5
    ↓
store new Hook state
    ↓
component sees count = 5
    ↓
reconciliation
    ↓
commit
```

The current source imports `requestUpdateLane`, `scheduleUpdateOnFiber`, and Hook-concurrent enqueue functions directly into `ReactFiberHooks.js`, reflecting this connection between Hook dispatch, lanes, queueing, and scheduling. ([GitHub][1])

---

# 23. Where does the setter function come from?

When you write:

```jsx
const [count, setCount] = useState(0);
```

`setCount` is not an ordinary function you wrote.

React creates a dispatch function and stores it on the Hook's queue.

The current source's `UpdateQueue` contains:

```text
dispatch
```

and the Hook implementation assigns the dispatch associated with that queue. The dispatch eventually routes through React's update scheduling machinery. ([GitHub][1])

Conceptually:

```text
Hook queue
   │
   └── dispatch
          ↓
       setCount
```

---

# 24. Why does `setCount` remember which state it belongs to?

This is a beautiful implementation detail.

Suppose:

```jsx
const [count, setCount] = useState(0);
const [age, setAge] = useState(25);
```

You get:

```text
Hook #1
 └── queue.dispatch → setCount

Hook #2
 └── queue.dispatch → setAge
```

Each dispatch is associated with its specific Hook queue.

So calling:

```jsx
setAge(30);
```

doesn't update Hook #1.

Conceptually:

```text
setCount
   ↓
queue #1
   ↓
Hook #1

setAge
   ↓
queue #2
   ↓
Hook #2
```

This association is a key part of how React manages independent state variables.

---

# 25. But the setter does NOT contain "the state value"

This is subtle.

Don't imagine:

```javascript
setCount = {
    value: 0
}
```

Rather, think:

```text
setCount
   ↓
dispatch associated with Hook queue
   ↓
enqueue update
   ↓
React later processes queue
```

The state is stored in React's Hook/Fiber structures.

The setter is primarily an update mechanism.

---

# 26. A closure is involved

Conceptually, React can create something similar to:

```javascript
const dispatch = action => {
    dispatchSetState(
        fiber,
        queue,
        action
    );
};
```

So the setter has access to the relevant:

```text
Fiber
queue
```

The exact implementation uses React's current dispatch/binding machinery rather than this simplified closure. But the conceptual purpose is:

```text
setCount
   knows
      ↓
which Fiber?
which Hook queue?
```

---

# 27. Current source: update object

The current source's `Update` type contains:

```javascript
{
    lane,
    revertLane,
    action,
    hasEagerState,
    eagerState,
    next,
    gesture
}
```

and the `UpdateQueue` contains:

```javascript
{
    pending,
    lanes,
    dispatch,
    lastRenderedReducer,
    lastRenderedState
}
```

These definitions are directly visible in the current `ReactFiberHooks.js`. ([GitHub][1])

For interview purposes, you should particularly understand:

```text
action
lane
next
pending
dispatch
lastRenderedState
```

---

# 28. `lastRenderedState`

Why might React need:

```text
lastRenderedState
```

in addition to:

```text
Hook.memoizedState
```

It supports update processing/dispatch optimizations.

One interesting optimization is **eager state calculation**.

Before React fully enters render work, it can sometimes calculate:

```text
"What would the next state be?"
```

using the last rendered reducer/state.

If that result is equal to the current state, React may avoid scheduling a full render. The current dispatch implementation contains an eager-state bailout path, and the React source's Hook queue explicitly stores `lastRenderedReducer` and `lastRenderedState`. ([Gist][4])

---

# 29. Eager bailout

Suppose:

```jsx
const [count, setCount] = useState(5);
```

and you call:

```jsx
setCount(5);
```

React may determine eagerly:

```text
current state = 5
requested state = 5
```

and:

```text
Object.is(5, 5) === true
```

so it may avoid scheduling unnecessary work.

React's public documentation states that React will ignore a state update when the next state is equal to the previous state according to `Object.is`. ([React][2])

This is why:

```jsx
setCount(5);
```

when `count` is already `5` need not cause useful UI work.

---

# 30. Why `Object.is` matters

Consider objects:

```jsx
const [user, setUser] = useState({
    name: "Alice"
});
```

Then:

```jsx
setUser(user);
```

uses the exact same reference.

Conceptually:

```text
old === new
```

so React can regard the state as unchanged.

But:

```jsx
setUser({
    name: "Alice"
});
```

creates a new object:

```text
old object !== new object
```

even if the contents are identical.

React's public docs explicitly describe `Object.is` equality for deciding that a state update can be ignored. ([React][2])

---

# 31. This is why state should be treated as immutable

Bad:

```jsx
user.name = "Bob";
setUser(user);
```

Now:

```text
old reference
    =
new reference
```

React may see:

```text
Object.is(old, new) === true
```

and regard the update as unchanged.

Correct:

```jsx
setUser({
    ...user,
    name: "Bob"
});
```

Now:

```text
new object reference
```

and React can detect the state replacement.

React's official `useState` documentation recommends replacing objects/arrays rather than mutating existing state objects. ([React][2])

---

# 32. Now let's understand the update render

Suppose current Hook:

```text
memoizedState = 0
```

Queue:

```text
pending:
    c => c + 1
    c => c + 1
```

React begins another render.

It reaches:

```jsx
useState(0)
```

but this is now **not a mount**.

It uses the update Hook path.

Conceptually:

```text
updateWorkInProgressHook()
        ↓
retrieve corresponding previous Hook
        ↓
clone/reuse it for WIP
        ↓
process queue
        ↓
calculate state
        ↓
return [newState, dispatch]
```

---

# 33. `currentHook` vs `workInProgressHook`

This is a very important internal distinction.

Think:

```text
Current tree
   │
   └── currentHook
```

and:

```text
WIP tree
   │
   └── workInProgressHook
```

During an update:

```text
currentHook
    ↓
previous Hook

workInProgressHook
    ↓
new WIP Hook
```

So React reads from the old Hook list while building the new Hook list.

The current source explicitly tracks these two pointers and describes `currentHook` as the list belonging to the current Fiber and `workInProgressHook` as the new list added to the WIP Fiber. ([GitHub][1])

---

# 34. Why clone the Hook list?

Recall:

```text
Current Fiber
       ↕
WIP Fiber
```

React shouldn't destroy the current committed state while calculating the next state.

So conceptually:

```text
Current Hook
   ↓
state = 0

WIP Hook
   ↓
state = 1
```

If the render succeeds:

```text
WIP
 ↓
committed/current
```

This matches the current/WIP Fiber architecture we studied earlier.

---

# 35. Hook order is the lookup mechanism

This is the beautiful part.

Suppose:

```jsx
function Component() {
    const [a, setA] = useState(1); // Hook #1
    const [b, setB] = useState(2); // Hook #2
    const ref = useRef(null);      // Hook #3
}
```

On the next render, React doesn't ask:

```text
"Which variable was called a?"
```

It can't.

It simply walks:

```text
current Hook #1 → current Hook #2 → current Hook #3
```

as your component calls:

```text
useState
useState
useRef
```

So the mapping is:

```text
call position #1 → Hook #1
call position #2 → Hook #2
call position #3 → Hook #3
```

This is why order is fundamental.

---

# 36. This explains the Rules of Hooks perfectly

The common rule:

> **Only call Hooks at the top level.**

isn't arbitrary style guidance.

It's required by the implementation model.

Invalid:

```jsx
if (loggedIn) {
    useState(...)
}
```

because the sequence can change.

Valid:

```jsx
useState(...);

if (loggedIn) {
    ...
}
```

because the Hook sequence stays:

```text
Hook #1
Hook #2
Hook #3
```

every render.

---

# 37. Why loops are also problematic

Invalid:

```jsx
for (let i = 0; i < count; i++) {
    useState(0);
}
```

Because if:

```text
count = 2
```

the sequence is:

```text
Hook #1
Hook #2
```

but if:

```text
count = 3
```

the sequence becomes:

```text
Hook #1
Hook #2
Hook #3
```

The positions no longer represent stable Hook identities.

---

# 38. Why nested functions are problematic

Invalid:

```jsx
function Component() {
    function helper() {
        useState(0);
    }

    helper();
}
```

The Hook is no longer necessarily being called in the component's direct Hook call structure expected by React's dispatcher/rules.

The public Rules of Hooks prohibit Hooks inside nested functions and control-flow constructs for precisely this reason.

---

# 39. The dispatcher

Now let's answer a very interesting question:

> How does `useState()` know whether it is mounting or updating?

When your code calls:

```jsx
useState(0)
```

the public Hook API resolves React's current dispatcher.

The internal Hook system installs a dispatcher appropriate for the current rendering context.

Conceptually:

```text
renderWithHooks
      ↓
set dispatcher
      ↓
Component()
      ↓
useState()
      ↓
dispatcher.useState()
```

The current `renderWithHooks` source explicitly sets `ReactSharedInternals.H` to the Mount or Update dispatcher. ([GitHub][1])

---

# 40. So `useState` is not itself choosing mount/update

The public API essentially delegates.

Conceptually:

```javascript
function useState(initialState) {
    const dispatcher = resolveDispatcher();

    return dispatcher.useState(initialState);
}
```

Then:

```text
Mount:
dispatcher.useState → mountState

Update:
dispatcher.useState → updateState
```

The current source's `renderWithHooks` is what establishes which dispatcher is active for the component render. ([GitHub][1])

---

# 41. Why would React use a dispatcher?

Because the same public API:

```jsx
useState()
```

needs different behavior depending on context.

For example:

```text
Mount dispatcher
    ↓
create Hook

Update dispatcher
    ↓
find/reuse Hook
process queue
```

So the public API remains simple:

```jsx
useState(...)
```

while internal behavior varies by rendering phase.

---

# 42. Simplified architecture

```text
                    Component render
                           │
                           ▼
                    renderWithHooks
                           │
                   choose dispatcher
                           │
             ┌─────────────┴─────────────┐
             │                           │
          Mount                        Update
             │                           │
       mountState()                updateState()
             │                           │
      create Hook                 find/reuse Hook
             │                           │
       initialize state           process queue
             │                           │
             └─────────────┬─────────────┘
                           ▼
                    return [state, dispatch]
```

That's the Hook runtime in one picture.

---

# 43. The current source also protects Hook ordering

In development, React stores the Hook types from the initial render:

```text
[
    useState,
    useState,
    useEffect
]
```

Then on later renders it compares the order.

The current source contains `hookTypesDev` and `hookTypesUpdateIndexDev` specifically for this validation and emits a warning if Hook order changes. ([GitHub][1])

This is a great example of:

```text
public rule
```

being backed by:

```text
internal data structure
```

---

# 44. What happens during `setCount()`?

Let's go into the update path more concretely.

Suppose:

```jsx
const [count, setCount] = useState(0);
```

Then:

```jsx
setCount(1);
```

Conceptually:

```text
setCount
  ↓
dispatch
  ↓
request update lane
  ↓
create Update:
{
   action: 1,
   lane: ...
}
  ↓
enqueue in Hook queue
  ↓
schedule Fiber/root
```

The current source's dispatch path requests an update lane and creates an `Update` object containing the lane and action, then uses concurrent Hook queueing and scheduling helpers. ([Gist][4])

---

# 45. Why does the update have a lane?

Because React now has:

```text
Fiber
+
Update
+
Lane
```

So React knows:

```text
which component?
which Hook?
what update?
what scheduling category?
```

For example:

```text
Counter Fiber
    ↓
Hook #1
    ↓
Update action = c => c + 1
    ↓
Transition lane
```

This connects our previous Scheduling topic directly to `useState`.

---

# 46. State update and queue processing

Suppose:

```text
memoizedState = 0
```

Queue:

```text
U1 = c => c + 1
U2 = c => c + 1
U3 = 10
```

Conceptually React performs:

```text
state = 0

U1:
0 → 1

U2:
1 → 2

U3:
2 → 10
```

Result:

```text
memoizedState = 10
```

The public React docs explicitly demonstrate this same replace/updater interaction. ([React][3])

---

# 47. Example mixing replacement and updater

```jsx
setNumber(number + 5);
setNumber(n => n + 1);
setNumber(42);
```

Current render:

```text
number = 0
```

Queue becomes conceptually:

```text
replace with 5
updater: n => n + 1
replace with 42
```

Process:

```text
0
 ↓ replace
5
 ↓ updater
6
 ↓ replace
42
```

Final:

```text
42
```

This exact queue behavior is documented by React. ([React][3])

---

# 48. Why does `setState` appear asynchronous?

People often say:

> "React state updates are asynchronous."

That's not quite precise.

The deeper explanation is:

```text
setState
    ↓
doesn't mutate current render's local state
    ↓
queues an update
    ↓
React processes it later as part of rendering
```

The callback itself executes immediately.

So:

```jsx
setCount(1);
console.log(count);
```

still sees the current render's:

```text
count = old value
```

React's official docs describe state as a snapshot and explicitly state that setting state requests another render rather than modifying the existing render's state variable. ([React][5])

---

# 49. Why does a timeout still see the old value?

Consider:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    function handleClick() {
        setCount(1);

        setTimeout(() => {
            console.log(count);
        }, 5000);
    }
}
```

The callback closes over the render's:

```text
count = 0
```

So after five seconds, that callback can still see:

```text
0
```

even though the UI has moved to:

```text
1
```

This isn't primarily a React queue issue.

It's JavaScript closure + React's render snapshot model.

React's official "State as a Snapshot" documentation demonstrates this behavior directly. ([React][5])

---

# 50. This is why each render has its own event handlers

Suppose:

```text
Render #1
count = 0
```

React creates:

```text
onClick handler H1
```

where H1 closes over:

```text
count = 0
```

After update:

```text
Render #2
count = 1
```

React creates:

```text
onClick handler H2
```

where H2 closes over:

```text
count = 1
```

So:

```text
Render #1 → handlers using snapshot #1
Render #2 → handlers using snapshot #2
```

This is a crucial piece of React's mental model.

---

# 51. What survives between renders?

Not your local variables.

For example:

```jsx
function Component() {
    let x = 0;

    x++;

    ...
}
```

Each render starts with:

```text
x = 0
```

again.

What survives is React-managed information:

```text
Fiber
 └── memoizedState
      └── Hooks
           ├── state
           ├── queues
           └── other Hook data
```

That's why:

```jsx
useState()
```

works while:

```jsx
let count = 0
```

doesn't provide persistent UI state.

---

# 52. A simplified custom `useState`

Now let's implement a toy version.

First, the simplest possible version:

```javascript
let state;
let initialized = false;

function useState(initialValue) {
    if (!initialized) {
        state = initialValue;
        initialized = true;
    }

    function setState(nextValue) {
        state = nextValue;
        render();
    }

    return [state, setState];
}
```

This teaches one thing:

```text
state must live outside the component function
```

But it fails immediately for multiple Hooks.

---

# 53. Why the global-state version fails

Suppose:

```jsx
const [count, setCount] = useState(0);
const [name, setName] = useState("Alice");
```

We need:

```text
state #1 = count
state #2 = name
```

not:

```text
one global state
```

So we need a list/array.

---

# 54. Toy Hook implementation using an array

We could do:

```javascript
let hooks = [];
let hookIndex = 0;

function useState(initialValue) {
    const index = hookIndex;

    if (hooks[index] === undefined) {
        hooks[index] = initialValue;
    }

    function setState(nextValue) {
        hooks[index] = nextValue;
        render();
    }

    hookIndex++;

    return [hooks[index], setState];
}
```

Before rendering:

```javascript
hookIndex = 0;
```

Then:

```jsx
function Component() {
    const [count, setCount] = useState(0);
    const [name, setName] = useState("Alice");
}
```

maps to:

```text
hooks[0] → count
hooks[1] → name
```

This toy implementation demonstrates why Hook order matters.

---

# 55. But real React uses a linked list, not a simple array

Real React's current Hook structure is:

```text
Hook
 ├── memoizedState
 ├── baseState
 ├── baseQueue
 ├── queue
 └── next → Hook
```

and:

```text
Fiber.memoizedState
```

points to the first Hook. ([GitHub][1])

So:

```text
Fiber
 ↓
Hook → Hook → Hook → null
```

rather than:

```text
Fiber
 ↓
[Hook, Hook, Hook]
```

---

# 56. Why is a linked list useful?

React needs to:

```text
walk the Hook sequence
create WIP Hook nodes
reuse previous Hooks
associate queues
```

The list provides:

```text
next
```

to navigate from one Hook to the next.

The current `mountWorkInProgressHook` simply appends each newly created Hook through its `next` pointer. ([GitHub][1])

---

# 57. Simplified real architecture

A better toy implementation is:

```javascript
function createHook() {
    return {
        memoizedState: null,
        queue: null,
        next: null
    };
}
```

Fiber:

```javascript
fiber.memoizedState = firstHook;
```

Then:

```text
firstHook
   ↓
secondHook
   ↓
thirdHook
```

During render:

```text
workInProgressHook
```

moves through the list.

---

# 58. Mount algorithm

Simplified:

```text
renderWithHooks
      ↓
currentlyRenderingFiber = WIP Fiber
      ↓
mount dispatcher
      ↓
Component()
      ↓
useState()
      ↓
mountWorkInProgressHook()
      ↓
create Hook
      ↓
append to WIP Fiber
      ↓
initialize state
```

For a component with:

```jsx
useState(0);
useState(false);
```

we get:

```text
Fiber.memoizedState
      ↓
Hook #1 → Hook #2 → null
   state=0   state=false
```

---

# 59. Update algorithm

On the next render:

```text
renderWithHooks
      ↓
update dispatcher
      ↓
Component()
      ↓
first useState()
      ↓
get corresponding previous Hook
      ↓
create/reuse WIP Hook
      ↓
process queue
      ↓
return current state
```

Then:

```text
second useState()
      ↓
move to next Hook
      ↓
process its queue
```

Therefore:

```text
Hook order
```

is how React finds the correct state.

---

# 60. This answers the famous question

> How does React know which `useState` belongs to which state?

Answer:

> During rendering, React associates Hooks with the currently rendering Fiber and processes them in call order. The Fiber contains a linked list of Hook nodes, and each render walks the current Hook list while building the work-in-progress Hook list. Therefore the first Hook call maps to the first Hook node, the second to the second, and so on. ([GitHub][1])

That's an excellent interview answer.

---

# 61. What if Hook order changes?

Suppose previous render:

```text
useState
useState
useEffect
```

Next:

```text
useState
useEffect
```

Conceptually:

```text
Old:
Hook #1 → state
Hook #2 → state
Hook #3 → effect

New:
Hook #1 → state
Hook #2 → effect
```

Now Hook #2 means something different.

That's why React warns in development about mismatched Hook order. The current source explicitly tracks Hook types and reports this situation. ([GitHub][1])

---

# 62. What happens if you render too few Hooks?

The current source has a check:

```text
currentHook !== null && currentHook.next !== null
```

after rendering.

If Hooks remain in the previous sequence that weren't consumed, React throws an error indicating that fewer Hooks were rendered than expected. ([GitHub][1])

For example:

```jsx
function Component({ condition }) {
    useState(0);

    if (condition) {
        useEffect(() => {});
    }
}
```

One render:

```text
state
effect
```

Another:

```text
state
```

The sequence changed.

---

# 63. What happens if you call too many Hooks?

Similarly, React's update Hook traversal expects a corresponding current Hook.

If your new render tries to consume more Hooks than exist in the current structure, React detects the mismatch rather than silently assigning arbitrary state.

This is another reason Hook structure has to remain stable.

---

# 64. `memoizedState` is overloaded

This is another subtle point.

We often say:

```text
Fiber.memoizedState → state
```

but that's too simplistic.

At the Fiber level:

```text
Fiber.memoizedState
```

points to the Hook list for function components.

At the Hook level:

```text
Hook.memoizedState
```

contains the state/data appropriate to that particular Hook.

For `useState`:

```text
Hook.memoizedState = state
```

For other Hooks:

```text
Hook.memoizedState
```

has different meaning.

So:

```text
Fiber.memoizedState
```

and:

```text
Hook.memoizedState
```

are different levels.

---

# 65. `updateQueue` on Fiber vs Hook `queue`

Another common confusion.

We previously learned:

```text
Fiber.updateQueue
```

Now we have:

```text
Hook.queue
```

They are not necessarily the same thing.

For a function component:

```text
Fiber
 ├── memoizedState → Hook list
 │
 └── updateQueue → component-level update/effect-related structures
```

while:

```text
Hook
 └── queue → updates for that Hook
```

The current Fiber/Hook source distinguishes these fields explicitly. ([GitHub][1])

---

# 66. Why `useReducer` and `useState` are so closely related

`useState` is implemented in terms of the same reducer machinery.

Conceptually:

```text
useState(initial)
     ↓
state updater
     ↓
basicStateReducer
```

where:

```javascript
basicStateReducer(state, action) {
    return typeof action === "function"
        ? action(state)
        : action;
}
```

This is why `useState` supports both:

```jsx
setState(value)
```

and:

```jsx
setState(prev => next)
```

React's current implementation's Hook queue type and reducer structure reflect this common mechanism. ([React][2])

---

# 67. `useState` is conceptually a special case of a reducer

Think:

```text
useReducer
    ↓
user supplies reducer

useState
    ↓
React supplies basicStateReducer
```

So:

```text
useState
→ update reducer = "replace value, unless action is a function"
```

This relationship is extremely useful in senior interviews.

---

# 68. Update processing with lanes

Now combine our previous Scheduling topic.

Suppose there are two pending updates:

```text
Update A → Input lane
Update B → Transition lane
```

React renders only some selected lanes.

During Hook queue processing, React determines which updates are relevant to the current render and which need to remain for a later render.

That is why the Hook has:

```text
baseState
baseQueue
```

rather than just:

```text
state
```

The current Hook structure explicitly stores both `baseState` and `baseQueue`, and update queues also carry lane information. ([GitHub][1])

---

# 69. Why do we need `baseState` and `baseQueue`?

This is one of the deepest internals.

Imagine:

```text
State = 0
```

Pending updates:

```text
U1 → urgent
U2 → transition
```

Suppose the current render is only processing urgent work.

React may process:

```text
U1
```

but skip:

```text
U2
```

because U2's lane isn't part of this render.

Then React must retain enough information to later say:

```text
"Start from the right base state and apply U2 when its lane is rendered."
```

That's the conceptual reason `baseState` and `baseQueue` exist.

The current Hook type has these fields specifically for this update-queue/base-state machinery. ([GitHub][1])

---

# 70. This is where lanes meet state

Now our architecture is:

```text
Fiber
 │
 └── Hook
      │
      ├── memoizedState
      ├── baseState
      ├── baseQueue
      │
      └── queue
           │
           ├── pending updates
           └── lanes
```

So state isn't just:

```text
state = value
```

It's:

```text
state
+
pending operations
+
priority
+
base information
```

This is why React can support concurrent updates.

---

# 71. Render phase processing

A simplified model of queue processing:

```javascript
let newState = hook.baseState;

for (const update of updates) {
    if (updateLaneIsRelevant(update)) {
        newState = reducer(newState, update.action);
    } else {
        keepForLater(update);
    }
}
```

Then:

```text
hook.memoizedState = newState
```

This is teaching pseudocode, but it captures the core idea behind lane-aware state processing.

---

# 72. Why state updates can be replayed

Now we can revisit concurrent rendering.

Suppose:

```text
Current state = 0

Transition update:
+100

Urgent update:
+1
```

React may process only the urgent update for one render:

```text
0 → 1
```

and later process the transition work.

Because updates are represented as operations in a queue rather than simply overwriting one mutable variable, React can reason about which updates have been processed and which remain.

That queue-based model is essential to concurrent rendering.

---

# 73. Render-phase updates

There is even a special case where a state setter is called **during rendering itself**:

```jsx
function Component() {
    const [count, setCount] = useState(0);

    if (count === 0) {
        setCount(1);
    }

    return <div>{count}</div>;
}
```

This is generally a pattern to avoid except for specific React-supported cases.

Internally, React has special handling for "render phase updates."

The current source has `didScheduleRenderPhaseUpdate` / `didScheduleRenderPhaseUpdateDuringThisPass` and a `renderWithHooksAgain` loop for these cases. ([GitHub][1])

---

# 74. Why `renderWithHooksAgain` exists

The current source explains that `renderWithHooksAgain` can be used when:

```text
setState called during render
```

or for specific development Strict Mode behavior.

React re-runs the component until the render-phase updates settle, with a hard re-render limit to prevent infinite loops. ([GitHub][1])

The current source sets:

```text
RE_RENDER_LIMIT = 25
```

for this protection. ([GitHub][1])

---

# 75. Infinite render loop

Consider:

```jsx
function Component() {
    const [count, setCount] = useState(0);

    setCount(count + 1);

    return <div>{count}</div>;
}
```

Conceptually:

```text
render
 ↓
setState
 ↓
render again
 ↓
setState
 ↓
render again
 ↓
...
```

React detects excessive render-phase rerenders rather than allowing infinite rendering forever. The current source throws after its configured re-render limit. ([GitHub][1])

---

# 76. Strict Mode and Hooks

In development Strict Mode, React may invoke component functions twice to help expose impure rendering.

The current source explicitly implements a second render pass for Strict Mode in development. It also explains that Hook state from the first invocation is reused during the second invocation. ([GitHub][1])

So:

```text
development Strict Mode
    ↓
component function may execute twice
```

This doesn't mean:

```text
state is initialized from scratch twice
```

in the simplistic sense.

React is deliberately reusing the Hook state during the relevant second invocation.

---

# 77. Initializer function

Consider:

```jsx
const [state] = useState(() => expensiveCalculation());
```

The function form can be used as a lazy initializer.

Conceptually:

```text
mount
 ↓
initialize state from function
```

rather than:

```jsx
useState(expensiveCalculation())
```

where JavaScript evaluates the calculation before calling `useState`.

This distinction is about when the initialization work occurs.

React's `useState` API documents that the initial-state argument can be a value or an initializer function. ([React][2])

---

# 78. Setter identity

Another practical interview question:

> Does `setCount` change every render?

React guarantees that the setter function returned by a particular `useState` call has stable identity across renders, allowing it to be omitted from Effect dependency arrays in common cases. The public `useState` documentation documents this stability. ([React][2])

Conceptually:

```text
Render #1:
setCount → function A

Render #2:
setCount → function A
```

The association is with the Hook queue rather than being a brand-new unrelated callback each render.

---

# 79. Why is setter stability useful?

Suppose:

```jsx
useEffect(() => {
    ...
}, [setCount]);
```

The setter itself doesn't need to cause the Effect to re-run merely because the component rendered.

Its stable identity is part of React's Hook API behavior.

More importantly, you can think of:

```text
dispatch
```

as belonging to the persistent Hook queue.

---

# 80. A simplified end-to-end implementation

Let's make a toy React Hook runtime.

### Hook

```javascript
class Hook {
    constructor(initialState) {
        this.memoizedState = initialState;
        this.queue = [];
        this.next = null;
    }
}
```

### Fiber

```javascript
class Fiber {
    constructor() {
        this.memoizedState = null;
    }
}
```

### Mounting hooks

```javascript
let currentlyRenderingFiber;
let workInProgressHook;

function mountWorkInProgressHook() {
    const hook = new Hook(null);

    if (workInProgressHook === null) {
        currentlyRenderingFiber.memoizedState = hook;
    } else {
        workInProgressHook.next = hook;
    }

    workInProgressHook = hook;

    return hook;
}
```

---

# 81. Toy `useState`

```javascript
function useState(initialState) {
    const hook = mountWorkInProgressHook();

    hook.memoizedState = initialState;

    const setState = action => {
        hook.queue.push(action);
        render();
    };

    return [hook.memoizedState, setState];
}
```

Still incomplete, but we've now represented:

```text
Fiber
 ↓
Hook
 ↓
state
 ↓
update queue
```

---

# 82. Toy update logic

On the next render:

```javascript
function updateWorkInProgressHook() {
    const hook = currentHook;

    currentHook = currentHook.next;

    return hook;
}
```

Then:

```javascript
function useState(initialState) {
    const hook = updateWorkInProgressHook();

    let state = hook.memoizedState;

    for (const action of hook.queue) {
        state =
            typeof action === "function"
                ? action(state)
                : action;
    }

    hook.memoizedState = state;
    hook.queue = [];

    return [state, dispatch];
}
```

This isn't React's code.

But the architecture now looks remarkably familiar.

---

# 83. Toy runtime vs actual React

Our toy version:

```text
Fiber
 ↓
Hook
 ↓
array queue
 ↓
state
```

Actual React:

```text
Fiber
 ↓
Hook linked list
 ↓
UpdateQueue
 ↓
pending updates
 ↓
lanes
 ↓
baseQueue/baseState
 ↓
reducer
 ↓
WIP Hook
```

The toy implementation helps you understand the architecture without becoming overwhelmed by React's many edge cases.

---

# 84. Why React's actual implementation is much more complex

The current `ReactFiberHooks.js` is thousands of lines long because Hook processing now needs to deal with:

```text
multiple Hook types
concurrent updates
lanes
transitions
optimistic state
Suspense interactions
render-phase updates
Strict Mode
effects
external stores
thenables/use
development validation
profiling/devtools
hydration-related behavior
```

The current file itself is over 5,000 lines in the GitHub representation. ([GitHub][1])

So don't try to memorize it.

Understand the architecture.

---

# 85. The exact `useState` mental model

When you write:

```jsx
const [count, setCount] = useState(0);
```

think:

```text
currentlyRenderingFiber
       │
       ▼
find/create Hook #N
       │
       ├── memoizedState = current count
       │
       └── queue
             │
             └── dispatch → setCount
```

When:

```jsx
setCount(c => c + 1)
```

runs:

```text
setCount
   ↓
create Update
   ↓
action = c => c + 1
   ↓
lane assigned
   ↓
enqueue
   ↓
schedule Fiber/root
```

On the next render:

```text
useState
   ↓
find same Hook position
   ↓
process queued actions
   ↓
memoizedState = new value
   ↓
return [newState, same dispatch]
```

That is the complete conceptual model.

---

# 86. The entire `useState` pipeline

```text
                    COMPONENT RENDER
                           │
                           ▼
                    renderWithHooks
                           │
                           ▼
                 currentlyRenderingFiber
                           │
                           ▼
                     useState()
                           │
                 ┌─────────┴─────────┐
                 │                   │
               Mount                Update
                 │                   │
                 ▼                   ▼
        mountWorkInProgressHook  find current Hook
                 │                   │
                 ▼                   ▼
             create Hook        WIP Hook
                 │                   │
                 ▼                   ▼
          initialize state       process queue
                 │                   │
                 └─────────┬─────────┘
                           ▼
                   [state, dispatch]
```

Then later:

```text
dispatch
   ↓
Update
   ↓
Lane
   ↓
Hook queue
   ↓
scheduleUpdateOnFiber
   ↓
render
   ↓
process queue
   ↓
new state
   ↓
reconciliation
   ↓
commit
```

---

# 87. Interview answer: How does `useState` work internally?

A strong answer:

> `useState` is implemented through React's Hook dispatcher and Fiber Hook data structures. During a function component render, React sets the currently rendering Fiber and chooses a mount or update Hook dispatcher. Each Hook call corresponds to a Hook node in a linked list stored from the Fiber's `memoizedState`. On mount, `useState` creates a Hook and initializes its state and update queue. The returned setter dispatches an update containing the action and lane, queues it, and schedules work. On the next render, React finds the corresponding Hook by call order, processes its queued updates with the basic state reducer, stores the resulting state, and returns it to the component. ([GitHub][1])

That is a **very strong senior-level answer**.

---

# 88. Interview answer: Why can't Hooks be conditional?

> React associates Hook calls with Hook nodes in a linked list based on call order. If the order or number of Hook calls changes between renders, a given Hook call can point to the wrong Hook node, causing state/effect data to become mismatched. React's development implementation explicitly tracks Hook order to detect this. ([GitHub][1])

---

# 89. Interview answer: Where does state live?

Don't say:

> "Inside `useState`."

Better:

> Persistent state is stored in React's internal Hook/Fiber structures rather than in the JavaScript local variable from a particular component invocation. For function components, the Fiber's `memoizedState` points to the Hook list, and each state Hook stores its state and update queue. ([GitHub][1])

---

# 90. Interview answer: Why is `setCount(c => c + 1)` different?

> A direct value such as `setCount(5)` queues a replacement value, while a function such as `setCount(c => c + 1)` queues an updater function. During the next render, React processes queued updates in order. The updater function receives the state produced by the previous queued update, so multiple updater functions compose correctly. ([React][3])

---

# 91. Interview answer: Is state stored in the Fiber?

Careful answer:

> For function components, the Fiber's `memoizedState` field points to the component's Hook list, and state associated with individual Hooks is stored on those Hook nodes. So saying "state lives in the Fiber" is a useful shorthand, but the more precise statement is that the Fiber owns the Hook list through `memoizedState`. ([GitHub][1])

Excellent distinction.

---

# 92. Interview answer: Why does React use a linked list for Hooks?

> The linked list gives React a stable sequence of Hook nodes corresponding to the order in which Hooks are called. On each render, React can walk the current Hook list and build/reuse the work-in-progress Hook list in the same order. This lets the same `useState`/`useEffect` call position map to the same persistent Hook state. ([GitHub][1])

---

# 93. One final deep example

Consider:

```jsx
function Example() {
    const [count, setCount] = useState(0);
    const [name, setName] = useState("Alice");

    function increment() {
        setCount(c => c + 1);
        setCount(c => c + 1);
    }

    return (
        <div>
            <h1>{name}</h1>
            <button onClick={increment}>
                {count}
            </button>
        </div>
    );
}
```

Initial internal picture:

```text
Example Fiber
      │
      ▼
Hook #1
 ├── memoizedState = 0
 └── queue
      └── dispatch → setCount

      ↓ next

Hook #2
 ├── memoizedState = "Alice"
 └── queue
      └── dispatch → setName
```

Click button.

Two updates are queued:

```text
Hook #1 queue

Update A
 action = c => c + 1

Update B
 action = c => c + 1
```

React schedules the Fiber.

Next render:

```text
Hook #1
state = 0
```

process:

```text
A:
0 → 1

B:
1 → 2
```

So:

```text
Hook #1.memoizedState = 2
```

`Hook #2` remains:

```text
"Alice"
```

Component receives:

```text
count = 2
name = "Alice"
```

and returns a new element tree.

Then:

```text
reconciliation
    ↓
only button text needs updating
    ↓
commit
    ↓
DOM
```

Nothing about this requires React to recreate the entire component or entire DOM tree.

---

# 94. The biggest conceptual breakthrough

Before this topic, you could think:

```text
useState = magic
```

Now replace that with:

```text
useState
   ↓
Hook dispatcher
   ↓
currentlyRenderingFiber
   ↓
Hook list
   ↓
specific Hook node
   ↓
memoizedState
   +
update queue
   ↓
dispatch
   ↓
lane
   ↓
schedule update
   ↓
next render
   ↓
process queue
   ↓
new state
```

Once this makes sense, **most Hook behavior stops feeling magical**.

---

# 95. Revision Sheet

```text
Fiber.memoizedState
→ points to the Hook list for a function component.

Hook
→ {
     memoizedState,
     baseState,
     baseQueue,
     queue,
     next
   }

Hook list
→ Hook → Hook → Hook

currentHook
→ current/previous Hook being read during update rendering.

workInProgressHook
→ WIP Hook currently being built/processed.

currentlyRenderingFiber
→ Fiber whose component is executing.

Mount
→ create Hook nodes and initialize state.

Update
→ find corresponding Hook nodes and process queues.

queue
→ pending state updates for a Hook.

Update
→ contains action + lane + linked-list information
   plus additional current implementation fields.

action = value
→ replace state with that value.

action = function
→ call updater with current/pending state.

Lane
→ scheduling category for the update.

memoizedState
→ current processed Hook state.

baseState/baseQueue
→ preserve information needed when not all queued work
   is processed in the current render.

Rules of Hooks
→ keep Hook order stable because identity is positional.

setState
→ queues an update; it does not mutate the current render's
   local state variable.

Object.is
→ React can ignore state updates whose next state is equal
   to the previous state.
```

React's current source and documentation support these mechanics and semantics. ([GitHub][1])

---

# The React architecture now looks like this

```text
                         React
                           │
                           ▼
                       Component
                           │
                           ▼
                    renderWithHooks
                           │
                           ▼
                    Fiber / WIP Fiber
                           │
                           ▼
                    Hook linked list
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
           Hook #1       Hook #2      Hook #3
              │
              ▼
        memoizedState
              │
              ▼
            queue
              │
              ▼
           Update
              │
              ▼
            Lane
              │
              ▼
       schedule update
              │
              ▼
           Work loop
              │
              ▼
        render/reconcile
              │
              ▼
         new Hook state
              │
              ▼
            commit
              │
              ▼
             DOM
```

This is one of the most important diagrams in the entire course.

---

# Next Topic — `useEffect` Internals & the Effect Lifecycle

Now we'll tackle another **extremely common interview area**:

```jsx
useEffect(() => {
    ...
}, [dependency]);
```

But we'll go well beyond:

> "`useEffect` runs after render."

We'll understand:

```text
render
  ↓
create Effect Hook
  ↓
dependency comparison
  ↓
effect flags
  ↓
Fiber.updateQueue
  ↓
commit
  ↓
layout/passive effect timing
  ↓
cleanup
  ↓
next effect
```

We'll also answer the classic questions:

> Why does `useEffect` run after the component renders?

> Why doesn't changing a dependency immediately execute the Effect during render?

> Where does React store Effects?

> Why does cleanup run before the next Effect?

> Why can Strict Mode run an Effect setup/cleanup sequence twice in development?

> What's the difference between `useEffect` and `useLayoutEffect` internally?

> Why does the dependency array use `Object.is` comparison?

[1]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberHooks.js "react/packages/react-reconciler/src/ReactFiberHooks.js at main · react/react · GitHub"
[2]: https://react.dev/reference/react/useState?utm_source=chatgpt.com "useState – React"
[3]: https://react.dev/learn/queueing-a-series-of-state-updates?utm_source=chatgpt.com "Queueing a Series of State Updates – React"
[4]: https://gist.github.com/pshaddel/d69ab19491a69ca3d860d3903b946ce0?utm_source=chatgpt.com "dispatchSetState · GitHub"
[5]: https://react.dev/learn/state-as-a-snapshot?utm_source=chatgpt.com "State as a Snapshot – React"

