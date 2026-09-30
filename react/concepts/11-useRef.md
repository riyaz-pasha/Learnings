# Topic 11 — `useRef` Internals, DOM Refs, and Callback Refs

We now move from:

```text id="w2vky3"
useState  → state + update queue + re-render
```

to:

```text id="znr2y7"
useRef   → persistent mutable value + NO re-render
```

This distinction is extremely important in React interviews.

The official React docs define `useRef` as a Hook for holding a value that **isn't needed for rendering**. The ref object has a `current` property, React returns the same object on later renders, and changing `current` does not trigger a re-render. ([React][1])

We'll go from the API all the way into the Fiber/Hook implementation model.

---

# 1. The simplest example

```jsx id="ypkz8s"
function Counter() {
    const countRef = useRef(0);

    function handleClick() {
        countRef.current++;
        console.log(countRef.current);
    }

    return (
        <button onClick={handleClick}>
            Click
        </button>
    );
}
```

Every click changes:

```text id="qvqvpd"
countRef.current
```

but React does **not** render the component again just because of that mutation.

That's the defining behavior:

```text id="0oax86"
ref.current = newValue
        ↓
React is not notified
        ↓
no update scheduled
        ↓
no re-render
```

React explicitly describes refs this way. ([React][1])

---

# 2. Compare `useState` and `useRef`

This is one of the first questions an interviewer may ask.

```jsx id="m3czqj"
const [count, setCount] = useState(0);

const countRef = useRef(0);
```

Think:

```text id="93rkyy"
useState
  ↓
React-managed state
  ↓
setter
  ↓
schedule update
  ↓
render

useRef
  ↓
React-retained object
  ↓
mutate .current
  ↓
no scheduling
  ↓
no render
```

The official docs summarize the same difference: state triggers rendering when updated; refs persist across renders without triggering one. ([React][2])

---

# 3. Why does a ref persist?

This looks strange:

```jsx id="774wzx"
function Component() {
    const ref = useRef(0);

    console.log(ref);

    return ...;
}
```

If the function runs again, why does `ref` not become a brand-new object?

Because:

```text id="3n6w6k"
the local variable is recreated
```

but:

```text id="w0zsbl"
the ref object itself is retained by React
```

Conceptually:

```text id="q0p7qa"
Render #1

ref variable
    ↓
Object A

React stores Object A
```

Next render:

```text id="v2ltq9"
ref variable
    ↓
Object A
```

Again:

```text id="3h6nvj"
same object
```

React's official docs explicitly guarantee that the same object is returned from the same `useRef` call on subsequent renders. ([React][1])

---

# 4. This is exactly like a persistent Hook

We've already learned:

```text id="0k7zc5"
Fiber
 ↓
memoizedState
 ↓
Hook → Hook → Hook
```

`useRef` participates in this same Hook mechanism.

Conceptually:

```text id="db9kbl"
Fiber
  │
  └── memoizedState
          │
          ▼
       Hook
          │
          ▼
    { current: 0 }
```

So the ref doesn't live magically somewhere separate from React.

It is retained as Hook state associated with the component's Fiber.

---

# 5. The important implementation idea

React can conceptually implement:

```jsx id="q91zy4"
useRef(initialValue)
```

like:

```jsx id="54h0bz"
const [ref] = useState({
    current: initialValue
});
```

without using the setter.

The official React documentation explicitly presents this as a mental model for how `useRef` can be thought about internally. ([React][2])

Conceptually:

```javascript id="u0b0j4"
function useRef(initialValue) {
    const [ref] = useState({
        current: initialValue
    });

    return ref;
}
```

That's not literally the implementation, but it reveals something fundamental:

> **A ref is essentially a persistent object whose mutation is intentionally invisible to React's rendering system.**

---

# 6. Why doesn't React use `useState` literally?

Because React can implement a much simpler and more efficient Hook specifically for refs.

We need:

```text id="2d0o0a"
persistent object
+
stable identity
+
mutable current
```

We don't need:

```text id="45z9m4"
update queue
+
render scheduling
+
state reducer
```

The actual current React source has dedicated `mountRef` and `updateRef` Hook implementations. ([React][1])

Conceptually:

```text id="r9m6jl"
mountRef
  ↓
create { current: initialValue }

updateRef
  ↓
return existing object
```

---

# 7. Simplified `mountRef`

Conceptually:

```javascript id="71dtc4"
function mountRef(initialValue) {
    const hook = mountWorkInProgressHook();

    const ref = {
        current: initialValue
    };

    hook.memoizedState = ref;

    return ref;
}
```

That's the key architecture.

So on mount:

```text id="be43de"
Fiber
 ↓
Hook
 ↓
memoizedState
 ↓
ref object
 ↓
{ current: initialValue }
```

The actual React source uses a development-aware Object.freeze step for ref objects in some modes/versions, but the important semantic model remains the same: the ref object itself is retained by the Hook. ([React][1])

---

# 8. Simplified `updateRef`

On the next render:

```javascript id="i5gpx0"
function updateRef() {
    const hook = updateWorkInProgressHook();

    return hook.memoizedState;
}
```

Notice what's missing:

```text id="6y6bk2"
No update queue
No lane
No dispatch
No new ref object
```

Instead:

```text id="7p9ec1"
return existing object
```

This is why:

```jsx id="iz6sag"
const ref1 = useRef(0);
```

and then later:

```jsx id="i2ncls"
const ref2 = useRef(0);
```

within the same component Hook position conceptually yield:

```text id="q0hr8d"
ref1 === ref2
```

across renders.

---

# 9. Why mutation doesn't trigger rendering

This is perhaps the most important internal distinction.

With:

```jsx id="j2ztw1"
const [count, setCount] = useState(0);
```

doing:

```jsx id="5x69ao"
setCount(1);
```

calls React's dispatch/update pipeline:

```text id="h91yqw"
setter
 ↓
Update object
 ↓
Lane
 ↓
queue
 ↓
schedule Fiber
 ↓
render
```

With:

```jsx id="n2qjfv"
const ref = useRef(0);
```

doing:

```jsx id="4avp5r"
ref.current = 1;
```

is simply:

```text id="km8r82"
ordinary JavaScript object mutation
```

React doesn't intercept the assignment.

So:

```text id="7bvsks"
ref.current = 1
```

does not inherently call:

```text id="seu7i3"
scheduleUpdateOnFiber(...)
```

That is why there is no re-render.

React's docs explicitly state that React isn't aware when `ref.current` changes because a ref is a plain JavaScript object. ([React][1])

---

# 10. This gives us a powerful rule

Use:

```text id="5fxhih"
state
```

when a value affects what should be rendered.

Use:

```text id="2h34p9"
ref
```

when a value should survive renders but **doesn't itself determine the visual output**.

React's docs describe refs as an "escape hatch" for values not used for rendering, such as DOM nodes or timer IDs. ([React][2])

---

# 11. Example: timer ID

Suppose:

```jsx id="a3xjvb"
function Timer() {
    const intervalRef = useRef(null);

    function start() {
        const id = setInterval(() => {
            // ...
        }, 1000);

        intervalRef.current = id;
    }

    function stop() {
        clearInterval(intervalRef.current);
    }

    return ...;
}
```

The interval ID needs to survive:

```text id="ktmhzh"
re-renders
```

but doesn't need to appear in the UI.

So:

```text id="f4s3xu"
useRef
```

is appropriate.

React's official docs use this exact kind of example. ([React][1])

---

# 12. Example: latest value

A ref is often used to keep track of a mutable value that event handlers or external callbacks need to read.

For example:

```jsx id="h5t7v8"
const latestValueRef = useRef(value);

useEffect(() => {
    latestValueRef.current = value;
}, [value]);
```

Then some long-lived callback can read:

```jsx id="70a7hp"
latestValueRef.current
```

without recreating the callback purely because the value changed.

This is a useful pattern, but it must be designed carefully because you're intentionally stepping outside React's reactive data flow.

React's docs describe refs as an escape hatch and recommend them when information doesn't participate in rendering. ([React][2])

---

# 13. Most common use case: DOM refs

Now:

```jsx id="1ibhz6"
const inputRef = useRef(null);

return <input ref={inputRef} />;
```

This is different from using `useRef` merely as a value container.

Here you are telling React:

> **Put the host DOM node associated with this element into my ref.**

React's docs explicitly describe this behavior. ([React][3])

---

# 14. What happens to `inputRef.current`?

Initially:

```text id="v3ro0b"
inputRef.current = null
```

After the input has been committed:

```text id="xj49ip"
inputRef.current
      ↓
HTMLInputElement
```

Then:

```jsx id="qly8l5"
inputRef.current.focus();
```

works.

The official DOM refs documentation states that React populates the ref's `current` with the DOM node when the node is created and resets it to `null` when the node is removed. ([React][3])

---

# 15. The DOM ref lifecycle

Conceptually:

```text id="u4h1u9"
Render
 ↓
ref object exists
current = null
 ↓
reconciliation
 ↓
commit
 ↓
DOM node attached
 ↓
ref.current = DOM node
```

On removal:

```text id="m9o1ww"
commit deletion
 ↓
ref.current = null
```

This is why you generally cannot safely use:

```jsx id="c3tn0v"
inputRef.current.focus()
```

during the component's render.

The DOM node hasn't necessarily been attached yet.

---

# 16. Why refs are assigned during commit

Remember our previous topic:

```text id="tkjfo0"
render
 ↓
reconciliation
 ↓
commit
```

The DOM node belongs to the committed host tree.

So React should not expose:

```text id="0em8pw"
inputRef.current = DOM node
```

as though the node were already committed while render is still calculating the future tree.

Instead, ref attachment belongs to the commit process.

This is why refs are safe to read in places such as:

```text id="18cshc"
event handlers
effects
layout effects
```

after the node exists.

---

# 17. Why don't you get the DOM node during render?

Consider:

```jsx id="xuwy1r"
function Input() {
    const ref = useRef(null);

    console.log(ref.current);

    return <input ref={ref} />;
}
```

During render:

```text id="1stjq4"
ref.current
→ null
```

because the ref hasn't been attached to the host node yet.

React's docs recommend accessing DOM refs from event handlers/effects rather than reading the `current` value during render. ([React][1])

---

# 18. When is the DOM ref set?

Conceptually:

```text id="j1pg4q"
Render
    ↓
create/reconcile input Fiber
    ↓
complete work
    ↓
commit
    ↓
attachRef
    ↓
inputRef.current = DOM node
```

The React source contains commit logic for attaching and detaching refs; current versions also need to account for object refs and callback refs.

The precise internal function names can evolve, but the lifecycle is stable:

```text id="z5rrpv"
host node committed
    ↓
ref attached
```

---

# 19. Callback refs

A ref doesn't have to be:

```jsx id="cscgmk"
const ref = useRef(null);
```

You can also use:

```jsx id="4hbyve"
<input ref={node => {
    console.log(node);
}} />
```

This is a **callback ref**.

React calls the function with the node:

```text id="y3v0bg"
node
```

when it attaches it.

And with:

```text id="9l1q6y"
null
```

when the ref is detached.

---

# 20. Callback ref lifecycle

Conceptually:

```text id="4hc8xb"
mount
 ↓
callback(domNode)
```

When detached:

```text id="3rlkzx"
callback(null)
```

This gives you an imperative notification when the host instance changes.

Unlike:

```jsx id="cj6j8i"
const ref = useRef(null);
```

where React updates:

```text id="h8f3q7"
ref.current
```

it calls:

```text id="q7y0w7"
callback(node)
```

for a callback ref.

---

# 21. Callback refs are more than a different syntax

Suppose you need logic to run exactly when a particular DOM node is attached or detached.

Callback refs can be useful:

```jsx id="wy1hbt"
<input
    ref={node => {
        if (node) {
            node.focus();
        }
    }}
/>
```

This avoids depending on an Effect merely to know when that specific ref was populated.

React's current docs include callback refs among the ref APIs, and current React 19 work has also improved ref cleanup behavior; we'll return to that shortly. ([React][4])

---

# 22. Object ref vs callback ref

### Object ref

```jsx id="kglzfu"
const ref = useRef(null);

<input ref={ref} />
```

React does:

```text id="asjc49"
ref.current = node
```

### Callback ref

```jsx id="t0h6up"
<input ref={node => ...} />
```

React does conceptually:

```text id="2n3n7w"
callback(node)
```

Both represent references to committed host instances, but the ownership/lifecycle mechanism differs.

---

# 23. A callback-ref subtlety

Consider:

```jsx id="dw1vqo"
<input ref={node => {
    console.log(node);
}} />
```

The callback function is created on every render.

That can matter because React may need to detach the previous callback and attach the new one if its identity changes.

So:

```text id="j99yfk"
render 1 → callback A
render 2 → callback B
```

React may need to treat them as different ref functions.

This is one reason callback refs often use stable functions when their identity matters.

---

# 24. Ref callbacks and cleanup in modern React

React 19 introduced support for **callback ref cleanup functions**.

For example:

```jsx id="vi1wyz"
<div
    ref={(node) => {
        // setup

        return () => {
            // cleanup
        };
    }}
/>
```

React 19's documentation describes callback refs being able to return a cleanup function, which React calls when the node is removed or the ref callback is replaced. ([React][4])

This is a modern React interview detail worth knowing.

Conceptually:

```text id="vm0e1k"
ref callback(node)
       ↓
returns cleanup
       ↓
later:
cleanup()
```

---

# 25. `ref` as a prop in React 19

This is another important modern change.

Historically, function components receiving refs usually required:

```jsx id="bl76pc"
forwardRef(...)
```

In React 19, `ref` can be accessed as a normal prop by function components:

```jsx id="gw1hpa"
function MyInput({ ref, placeholder }) {
    return (
        <input
            ref={ref}
            placeholder={placeholder}
        />
    );
}
```

Usage:

```jsx id="5s2vlw"
const ref = useRef(null);

<MyInput ref={ref} />
```

React's official React 19 release notes explicitly introduced `ref` as a prop and said new function components no longer need `forwardRef`; `forwardRef` is planned for future deprecation/removal. ([React][4])

---

# 26. Why was `forwardRef` needed historically?

Before React 19, a function component's `ref` was treated specially rather than simply being another ordinary prop in the common function-component model.

You'd write:

```jsx id="jzlb2o"
const MyInput = forwardRef(function MyInput(props, ref) {
    return <input ref={ref} />;
});
```

Now:

```text id="m2s01i"
parent ref
 ↓
forwardRef
 ↓
function receives ref
 ↓
DOM input
```

React 19 simplifies this:

```text id="c97b6w"
parent ref
 ↓
ref prop
 ↓
function component
 ↓
DOM input
```

The official React 19 release post documents this change. ([React][4])

---

# 27. What changed internally with `ref`?

This is important for current interviews.

React 19 also deprecated reading:

```javascript id="l55dpp"
element.ref
```

and instead uses:

```javascript id="w3sjai"
element.props.ref
```

because `ref` is now treated as a regular prop in React 19's element model. The React 19 upgrade guide documents this change. ([React][5])

So modern code should not rely on the historical:

```text id="x8s9g1"
element.ref
```

model.

---

# 28. But class components are different

React's React 19 release notes specifically note that refs passed to classes are not passed as ordinary props because they reference the component instance. ([React][4])

So:

```text id="wf3x3m"
Function component
→ ref can be a prop in React 19

Class component
→ ref still has special instance semantics
```

That's a good interview nuance.

---

# 29. Ref vs state: the classic interview example

Suppose:

```jsx id="zldd9y"
function Counter() {
    const ref = useRef(0);

    function handleClick() {
        ref.current++;
    }

    return (
        <button>
            {ref.current}
        </button>
    );
}
```

Clicking the button:

```text id="ghq1bi"
ref.current:
0 → 1 → 2 → 3
```

but the visible button doesn't necessarily update.

Why?

```text id="txvymt"
mutation
 ≠
render request
```

React isn't informed that:

```text id="4mt9hb"
ref.current
```

changed.

So the JSX isn't recalculated.

React's official documentation demonstrates this exact distinction. ([React][2])

---

# 30. State is for rendering, ref is for remembering

A very useful rule:

```text id="ieuhdu"
Does changing this value require UI recalculation?

        YES → useState

        NO  → useRef may be appropriate
```

Examples:

```text id="hn0pv3"
Displayed count
→ state

DOM node
→ ref

Interval ID
→ ref

Timeout handle
→ ref

Imperative third-party widget instance
→ ref

Displayed username
→ state/props

Mutable cache not used for rendering
→ possibly ref
```

React's docs describe this distinction directly. ([React][2])

---

# 31. Why not store everything in refs?

Because then your UI becomes disconnected from React's rendering model.

Suppose:

```jsx id="cwx3p2"
const userRef = useRef(null);
```

and you mutate:

```jsx id="mxf73d"
userRef.current = newUser;
```

but the UI depends on:

```jsx id="a2i3tl"
userRef.current.name
```

React doesn't know it should render again.

This is why the official docs call refs an **escape hatch** rather than a replacement for state. ([React][2])

---

# 32. Refs and React's one-way data flow

React's normal model is:

```text id="y6s79o"
state/props
    ↓
render
    ↓
UI
```

A ref lets you step outside this:

```text id="84f2ql"
React
  ↕
external imperative API
```

For example:

```text id="2r2o6v"
React
  ↓
DOM ref
  ↓
input.focus()
```

or:

```text id="xq7h4k"
React
  ↓
video element ref
  ↓
video.play()
```

That is why refs are called an escape hatch.

---

# 33. Why a ref object itself is stable

Suppose:

```jsx id="f6ssxy"
const ref = useRef(null);
```

Render #1:

```text id="xr4kqg"
ref → Object A
```

Render #2:

```text id="s4x2f8"
ref → Object A
```

Render #3:

```text id="kfj3z5"
ref → Object A
```

So:

```text id="v91a27"
ref1 === ref2 === ref3
```

across those renders.

This stable identity is explicitly guaranteed by React's API. ([React][1])

---

# 34. Why stable identity matters

Suppose an Effect uses:

```jsx id="kpw3la"
useEffect(() => {
    ...
}, [ref]);
```

The ref object doesn't change:

```text id="s195gp"
Object.is(oldRef, newRef)
→ true
```

so merely using the ref object doesn't force the Effect to re-run.

React's docs explicitly point out that the ref object returned by `useRef` has stable identity across renders. ([React][6])

---

# 35. But `ref.current` is not reactive

This is very important.

Suppose:

```jsx id="4j5e8u"
const ref = useRef(null);

useEffect(() => {
    console.log(ref.current);
}, [ref.current]);
```

This is not a useful reactive dependency.

Why?

Because:

```text id="5a9fu3"
ref.current
```

can change without causing a render.

Therefore React doesn't have a render in which it can observe that change and re-synchronize the Effect.

The official Effects documentation explicitly says mutable values like `ref.current` aren't reactive dependencies. ([React][7])

---

# 36. `ref` vs `ref.current`

This distinction is excellent interview material.

```text id="i0u8n7"
ref
```

is:

```text id="3l6s1f"
stable object
```

while:

```text id="3cmf4n"
ref.current
```

is:

```text id="64rn9f"
mutable property
```

So:

```text id="d4su6l"
ref
→ stable

ref.current
→ mutable
→ non-reactive
```

---

# 37. Why shouldn't you read `ref.current` during render?

Suppose:

```jsx id="g8m1dt"
function Component() {
    const valueRef = useRef(0);

    return <h1>{valueRef.current}</h1>;
}
```

The problem is:

```text id="jvyr5c"
ref.current can change
without triggering a render
```

So the UI could be based on a value that React isn't tracking.

React's docs explicitly say reading or writing `ref.current` during rendering can make component behavior unpredictable and should generally be avoided, except for predictable one-time initialization patterns. ([React][1])

---

# 38. One permitted initialization pattern

React's docs give an important exception:

```jsx id="m2nzip"
function Video() {
    const playerRef = useRef(null);

    if (playerRef.current === null) {
        playerRef.current = new VideoPlayer();
    }

    ...
}
```

Why is this acceptable?

Because the mutation is:

```text id="f2ew3w"
deterministic
+
only during initialization
+
same result every time
```

React's docs explicitly describe this as a valid lazy-ref initialization pattern. ([React][1])

---

# 39. Why `useRef(new VideoPlayer())` can be wasteful

Consider:

```jsx id="swy2lo"
const playerRef = useRef(new VideoPlayer());
```

React only uses that value during initialization, but:

```javascript id="y9xb55"
new VideoPlayer()
```

is still evaluated by JavaScript on every component execution.

So:

```text id="bqnjkj"
Render #1
new VideoPlayer()

Render #2
new VideoPlayer()  ← created but ignored

Render #3
new VideoPlayer()  ← created but ignored
```

This may be wasteful.

The official docs recommend lazy initialization when object creation is expensive. ([React][1])

---

# 40. `useRef` and the Hook list

Let's connect it to our Hook internals again.

Suppose:

```jsx id="21oq3q"
function Component() {
    const [count, setCount] = useState(0);
    const ref = useRef(null);
    const [name, setName] = useState("Alice");
}
```

The Hook list is conceptually:

```text id="d0u0vk"
Fiber.memoizedState
        │
        ▼
Hook #1
state = 0
        │
        ▼
Hook #2
ref = { current: null }
        │
        ▼
Hook #3
state = "Alice"
```

Notice:

```text id="64ovsd"
useRef
```

doesn't require a state-update queue like `useState`.

Its Hook just needs to preserve:

```text id="8rn9v6"
ref object
```

---

# 41. A very simplified internal implementation

```javascript id="byxfnf"
function mountRef(initialValue) {
    const hook = mountWorkInProgressHook();

    const ref = {
        current: initialValue
    };

    hook.memoizedState = ref;

    return ref;
}

function updateRef() {
    const hook = updateWorkInProgressHook();

    return hook.memoizedState;
}
```

Now compare to our previous `useState` architecture:

```text id="x0s4yg"
useState
   ↓
Hook
   ↓
state
   +
queue
   +
dispatch
```

versus:

```text id="yilz8o"
useRef
   ↓
Hook
   ↓
ref object
```

That explains most of the behavioral difference.

---

# 42. No dispatch function

There is no:

```text id="n0i70l"
setRef(...)
```

because the API expects:

```jsx id="7v9yyc"
ref.current = value;
```

The mutation itself is the update.

React doesn't need to know about it.

So:

```text id="i0j6ai"
useState
→ explicit update mechanism

useRef
→ direct mutation
```

---

# 43. DOM refs have a second layer

Now distinguish:

```jsx id="e3qgvm"
const ref = useRef(null);

<div ref={ref} />
```

There are two related mechanisms:

### `useRef`

creates the persistent ref object.

### `ref={ref}`

tells React:

> Attach/detach this ref to the committed host/component target.

So:

```text id="3jyd2q"
useRef
 ↓
object

ref prop
 ↓
React ref attachment machinery
 ↓
object.current = target
```

This is why a ref can be useful even though the React Hook itself doesn't know anything about the DOM.

---

# 44. React can attach refs to more than DOM nodes

Historically, refs can target:

```text id="r6jdcg"
DOM host instances
class component instances
```

And with function components in React 19:

```text id="walvb4"
ref prop
```

can be passed through a component to some child/host target. React 19 explicitly introduced `ref` as an ordinary prop for function components. ([React][4])

---

# 45. Custom components and refs

Modern React:

```jsx id="m5ikrf"
function MyInput({ ref }) {
    return <input ref={ref} />;
}
```

Then:

```jsx id="ozv7om"
const inputRef = useRef(null);

<MyInput ref={inputRef} />
```

Result:

```text id="0x1zfe"
inputRef.current
   ↓
DOM input
```

This eliminates the need for `forwardRef` in new function-component code. ([React][4])

---

# 46. `useImperativeHandle`

Sometimes you don't want to expose the entire DOM node.

Suppose:

```jsx id="svz7ke"
function Input({ ref }) {
    const inputRef = useRef(null);

    useImperativeHandle(ref, () => ({
        focus() {
            inputRef.current.focus();
        }
    }));

    return <input ref={inputRef} />;
}
```

Parent:

```jsx id="i1ir4m"
const ref = useRef(null);

<Input ref={ref} />

ref.current.focus();
```

The parent sees:

```text id="juhp1j"
{
    focus()
}
```

rather than:

```text id="uw6y31"
HTMLInputElement
```

React's ref Hooks documentation describes `useImperativeHandle` as the API for customizing the ref exposed by a component. ([React][8])

We'll give this its own topic later.

---

# 47. Why expose an imperative API?

Suppose you have:

```jsx id="uv3by0"
<VideoPlayer />
```

The parent might need:

```javascript id="zs50nj"
ref.current.play();
ref.current.pause();
ref.current.seek(30);
```

It doesn't necessarily need access to:

```text id="o9gysc"
internal DOM nodes
internal state
implementation details
```

`useImperativeHandle` lets the component expose a deliberately small imperative interface.

This is analogous to encapsulation in object-oriented design.

---

# 48. Object ref lifecycle during unmount

Consider:

```jsx id="2pmrhb"
const ref = useRef(null);

return showInput
    ? <input ref={ref} />
    : null;
```

When mounted:

```text id="2c8lvd"
ref.current → input
```

When removed:

```text id="n826r3"
ref.current → null
```

React's documentation explicitly states this behavior. ([React][3])

---

# 49. Why `ref.current` can be null

Even after defining:

```jsx id="7p0y20"
const inputRef = useRef(null);
```

you cannot assume:

```text id="lm8i0u"
inputRef.current !== null
```

at all times.

It may be:

```text id="1i1trc"
before mount
```

or:

```text id="6azf0o"
after unmount
```

or:

```text id="jd3e7n"
during a render before the relevant commit
```

So code such as:

```jsx id="ujn6az"
inputRef.current.focus();
```

normally belongs in:

```text id="d9m5up"
event handler
layout effect
effect
```

when the node is expected to exist.

---

# 50. Ref attachment and Strict Mode

In development Strict Mode, mount/unmount-related behavior may be exercised to reveal incorrect cleanup and ref assumptions.

You shouldn't build code that assumes:

```text id="p2n07k"
"this ref callback runs once forever."
```

Modern React expects code to correctly handle attach/detach lifecycles.

This becomes especially relevant with callback refs and their cleanup functions.

---

# 51. Callback refs versus Effects

Suppose you want to focus an input when it becomes available.

You could:

```jsx id="62b5in"
const inputRef = useRef(null);

useLayoutEffect(() => {
    inputRef.current?.focus();
}, []);
```

Or, for certain dynamic-node cases, use a callback ref:

```jsx id="4gdzsa"
const setInputRef = node => {
    if (node) {
        node.focus();
    }
};
```

Callback refs can be useful because they are directly tied to the attachment of that specific node.

The correct choice depends on whether you're responding to:

```text id="0tmm2s"
component synchronization
```

or:

```text id="j98o5o"
node attachment/detachment
```

---

# 52. Why callback ref identity matters

Consider:

```jsx id="krpdjz"
<input
    ref={node => {
        ...
    }}
/>
```

Every render creates a new function:

```text id="yq4z2p"
Render #1 → function A
Render #2 → function B
Render #3 → function C
```

React must account for the fact that the ref callback changed.

A stable callback:

```jsx id="fl4ssv"
const handleRef = useCallback(node => {
    ...
}, []);
```

can avoid callback identity changes, but don't use `useCallback blindly; choose it when stable identity has a reason.

The broader principle is the same as with other callback props.

---

# 53. `useRef` and closures

Refs are sometimes useful for escaping stale closures.

Suppose:

```jsx id="rbq4jo"
function Component({ value }) {
    const latestValue = useRef(value);

    latestValue.current = value;

    const handleEvent = useCallback(() => {
        console.log(latestValue.current);
    }, []);

    ...
}
```

Now:

```text id="g7r7d7"
handleEvent
```

can remain stable while:

```text id="45vff6"
latestValue.current
```

tracks the newest value.

This can be useful, but be careful: updating a ref during render this way has purity implications. React's docs generally prohibit reading/writing `ref.current` during render except for predictable initialization. ([React][1])

A safer pattern often updates the ref in an Effect or event boundary depending on what you're trying to achieve.

---

# 54. Why refs are not reactive

Let's define "reactive" in React terms.

A reactive value participates in:

```text id="jgvci7"
render
 ↓
dependency calculation
 ↓
update
 ↓
new render
```

A ref's `current` property doesn't.

So:

```text id="5omx2z"
state
→ reactive

props
→ reactive

ref.current
→ mutable but not reactive
```

The official Effects documentation explicitly makes this distinction. ([React][7])

---

# 55. A ref can contain a function

Nothing requires:

```jsx id="d4v3pd"
useRef(null)
```

or:

```jsx id="k9l4hz"
useRef(0)
```

You can have:

```jsx id="h0uad3"
const callbackRef = useRef(() => {});
```

or:

```jsx id="xv87w8"
const cacheRef = useRef(new Map());
```

or:

```jsx id="z1xyie"
const controllerRef = useRef(null);
```

The ref can hold any JavaScript value.

The important requirement is:

```text id="5h1o0s"
the value doesn't need to drive rendering
```

---

# 56. A ref can hold mutable objects

Example:

```jsx id="nf84c8"
const cacheRef = useRef(new Map());

cacheRef.current.set("user:1", user);
```

No re-render occurs when the Map changes.

That's often useful for:

```text id="1f5i4p"
imperative caches
bookkeeping
third-party instances
timer handles
DOM nodes
```

But if the UI needs to respond to the cache changing:

```text id="3p5sz5"
ref alone isn't enough
```

You need state or another reactive mechanism.

---

# 57. Why ref mutation is immediate

Compare:

```jsx id="6ixgl1"
setCount(5);

console.log(count);
```

to:

```jsx id="u7br4e"
ref.current = 5;

console.log(ref.current);
```

The first:

```text id="x0k0i6"
state snapshot
```

doesn't change immediately.

The second:

```text id="bft77c"
plain JavaScript object mutation
```

does.

React's docs explicitly point out that mutating `ref.current` changes it immediately, unlike state. ([React][2])

---

# 58. Why that doesn't violate React's model

Because refs are deliberately an escape hatch.

You're essentially saying:

> "React doesn't need to use this value to determine my rendered output."

So React doesn't track every mutation.

That's why you can do:

```javascript id="yqz1df"
ref.current = value;
```

freely in event handlers/effects without requiring React's state machinery.

---

# 59. Interview question: Why doesn't changing a ref trigger re-render?

Strong answer:

> `useRef` returns a persistent plain JavaScript object stored by React as Hook state. Mutating `ref.current` does not go through a React state-dispatch function, so React doesn't enqueue an update or schedule rendering. That's why the mutation is immediate but doesn't update the rendered UI. ([React][1])

---

# 60. Interview question: Where does a ref live internally?

Strong answer:

> A `useRef` value is retained in the Hook structure associated with the component's Fiber. On mount React creates a ref object and stores it in the Hook's memoized state; on subsequent renders React returns that same object rather than creating a new one. ([React][2])

---

# 61. Interview question: Why is `useRef` stable across renders?

> Because React stores the ref object as persistent Hook state associated with the component's Fiber and returns the stored object on subsequent renders.

Conceptually:

```text id="y6x4og"
Fiber
 ↓
Hook
 ↓
ref object A

next render
 ↓
same Hook
 ↓
same ref object A
```

---

# 62. Interview question: `useRef` vs `useState`?

A strong answer:

|                                | `useState`           | `useRef`                           |
| ------------------------------ | -------------------- | ---------------------------------- |
| Persists across renders        | Yes                  | Yes                                |
| Changing value triggers render | Yes                  | No                                 |
| Value is a render snapshot     | Yes                  | No                                 |
| Mutable directly               | No, use setter       | Yes, `.current`                    |
| Update queue                   | Yes                  | No state update queue              |
| Typical use                    | UI state             | non-rendering mutable values / DOM |
| Setter/dispatch                | Yes                  | No                                 |
| Stable object identity         | Not the same concept | Yes, ref object                    |

React's official docs make the core state/ref distinction explicit. ([React][2])

---

# 63. Interview question: When would you use a ref?

A strong answer:

> When the value must survive re-renders but changing it doesn't itself need to update the UI. Common cases are DOM nodes, timer IDs, imperative third-party objects, and other mutable values used outside the rendering calculation. ([React][2])

---

# 64. Interview question: Why not use a module variable?

Suppose:

```javascript id="fj79yn"
let value = 0;
```

outside the component.

That value is shared across all instances of the component:

```text id="ys7p3x"
Component A
Component B
Component C
     │
     └── same module variable
```

A ref is local to each component instance/tree identity:

```text id="3i1pqu"
Component A → ref A
Component B → ref B
Component C → ref C
```

React's docs explicitly highlight that refs are local to each copy of the component, unlike variables outside the component. ([React][1])

---

# 65. Interview question: Why can't I put `ref.current` in an Effect dependency array?

Answer:

> Because `ref.current` is mutable but not reactive. Its value can change without causing a render, so React has no render-time update at which to detect the change and re-synchronize the Effect. The ref object itself is stable; `ref.current` is not a reactive dependency. ([React][7])

---

# 66. Interview question: When can a DOM ref be read?

Generally:

```text id="nqx4id"
✅ event handlers
✅ effects
✅ layout effects
```

and not:

```text id="ogf1kl"
❌ normal render logic
```

because render happens before the host node is committed.

React's official refs documentation recommends accessing DOM refs from event handlers and other post-commit mechanisms. ([React][1])

---

# 67. Interview question: What is a callback ref?

> A callback ref is a function passed to the `ref` prop. React calls it with the relevant node/instance when the ref is attached and with `null` when it is detached. Unlike an object ref, you're receiving lifecycle notifications through a function rather than React mutating a `.current` property.

Modern React 19 additionally supports returning a cleanup function from a callback ref. ([React][4])

---

# 68. Interview question: What changed with refs in React 19?

Strong current answer:

> React 19 allows `ref` to be accessed as a prop in function components, so new function components generally don't need `forwardRef`. React 19 also deprecates accessing `element.ref` in favor of `element.props.ref`, and callback refs can return cleanup functions. Classes retain their special instance-ref behavior. ([React][4])

This is a very useful **modern React interview update**.

---

# 69. `useRef` and Fiber — full internal picture

Let's connect everything we've learned:

```text id="6l8hxf"
                    Component
                        │
                        ▼
                 renderWithHooks
                        │
                        ▼
                 currentlyRenderingFiber
                        │
                        ▼
                    Hook list
                        │
                        ▼
                     useRef
                        │
             ┌──────────┴──────────┐
             │                     │
           mount                 update
             │                     │
             ▼                     ▼
       create ref object      return same object
             │                     │
             └──────────┬──────────┘
                        ▼
                 Hook.memoizedState
                        │
                        ▼
               { current: value }
```

When the user does:

```javascript id="5ldq9i"
ref.current = value;
```

we have:

```text id="5lyozl"
ordinary JS mutation
       ↓
no queue
       ↓
no lane
       ↓
no scheduleUpdateOnFiber
       ↓
no re-render
```

---

# 70. DOM ref architecture

Now add:

```jsx id="jmx7gj"
<input ref={inputRef} />
```

The conceptual flow becomes:

```text id="7hy1ot"
useRef()
    ↓
persistent ref object

JSX ref prop
    ↓
Fiber knows it has a ref

render
    ↓
reconciliation

commit
    ↓
host node exists
    ↓
attach ref
    ↓
ref.current = DOM node
```

On removal:

```text id="71f8dv"
commit deletion
    ↓
detach ref
    ↓
ref.current = null
```

React's DOM-ref documentation explicitly describes this attachment/detachment behavior. ([React][3])

---

# 71. Ref vs Effect

Here's an important design distinction.

Suppose:

```jsx id="v9v8tq"
inputRef.current.focus();
```

A ref gives you:

```text id="g0x8j4"
the object
```

An Effect gives you:

```text id="0k70h8"
when to synchronize
```

So:

```text id="wr2va8"
ref
→ what external object?

effect
→ when should I synchronize?
```

For example:

```jsx id="ph1vjm"
useEffect(() => {
    if (isFocused) {
        inputRef.current?.focus();
    }
}, [isFocused]);
```

Here:

```text id="fuua8j"
useRef → holds DOM node

useEffect → decides when to interact with it
```

These two APIs often work together.

---

# 72. Ref vs memoized value

Don't confuse:

```jsx id="4h2q7g"
useRef(value)
```

with:

```jsx id="4zjv7o"
useMemo(() => value, deps)
```

### `useMemo`

is about caching a **calculated value** between renders so React can potentially reuse the calculation result.

### `useRef`

is about retaining a **mutable object/value** that doesn't itself participate in rendering.

Conceptually:

```text id="1d4ri6"
useMemo
→ cached calculation

useRef
→ persistent mutable storage
```

We'll cover `useMemo` later.

---

# 73. Ref vs `useCallback`

Similarly:

```text id="o1uqf6"
useCallback
→ stable function identity

useRef
→ stable object identity
```

A ref can itself hold a function:

```jsx id="jsc9t2"
const callbackRef = useRef(null);
callbackRef.current = someFunction;
```

but that's not the same mechanism as:

```jsx id="4vuf1e"
useCallback(someFunction, deps)
```

Again:

```text id="z1qp1n"
useCallback
→ memoized identity for rendering/props/deps

useRef
→ mutable storage outside rendering
```

---

# 74. A practical example: video control

```jsx id="cbx5u3"
function VideoPlayer({ isPlaying }) {
    const videoRef = useRef(null);

    useEffect(() => {
        if (!videoRef.current) {
            return;
        }

        if (isPlaying) {
            videoRef.current.play();
        } else {
            videoRef.current.pause();
        }
    }, [isPlaying]);

    return (
        <video ref={videoRef} />
    );
}
```

The architecture:

```text id="h1s4va"
videoRef
  ↓
DOM video element

isPlaying
  ↓
React state/prop

useEffect
  ↓
synchronize DOM video API
```

This is a textbook use of:

```text id="l4skz3"
ref + Effect
```

React's Effect documentation uses the same general pattern for synchronizing a video player with `isPlaying`. ([React][6])

---

# 75. One subtle modern detail: ref cleanup

With React 19 callback refs:

```jsx id="4ny2uc"
<div
    ref={(node) => {
        if (!node) return;

        const observer = new ResizeObserver(...);
        observer.observe(node);

        return () => {
            observer.disconnect();
        };
    }}
/>
```

the callback itself can return cleanup.

React 19 explicitly added support for ref cleanup functions. ([React][4])

This is useful because the resource lifecycle can directly follow the node's attachment lifecycle.

---

# 76. Why refs are called an escape hatch

React's normal data flow is:

```text id="k4y4yw"
state
 ↓
render
 ↓
UI
```

Refs let you say:

```text id="z47p2e"
"I need direct access to something
outside that declarative calculation."
```

For example:

```text id="xqf6hm"
DOM focus
scrolling
video playback
third-party widget
timer handle
imperative API
```

The current React docs explicitly describe refs as an escape hatch for non-rendering information and external systems. ([React][2])

---

# 77. A common interview trap

Interviewer:

> "Can I use a ref to force React to update?"

No.

You can mutate:

```javascript id="v3fyis"
ref.current
```

but React isn't notified.

If the UI needs to update:

```text id="d7xv3f"
useState
```

or another reactive mechanism is appropriate.

A ref is not a hidden state setter.

---

# 78. Another interview trap

Interviewer:

> "Does `useRef` cause a re-render when `.current` changes?"

No.

That's literally one of the main reasons to use it.

React says explicitly that changing `ref.current` doesn't trigger rendering. ([React][1])

---

# 79. Another interview trap

Interviewer:

> "Is `useRef` just a DOM reference?"

No.

It can hold:

```text id="qdkd7d"
DOM node
number
string
object
Map
timer ID
function
third-party instance
etc.
```

DOM references are simply the most common use case. React's docs explicitly note that a ref can hold values of any type. ([React][1])

---

# 80. Another interview trap

Interviewer:

> "Is `ref.current` reactive?"

No.

```text id="fbj3u6"
ref object
→ stable

.current
→ mutable
→ not reactive
```

React's Effect documentation explicitly warns that mutable values such as `ref.current` aren't reactive dependencies. ([React][7])

---

# 81. Another interview trap

Interviewer:

> "Can I read a DOM ref in render?"

Usually no.

At render time:

```text id="z7qrw4"
future DOM
```

may not exist yet.

The ref is populated during commit.

So use:

```text id="fyx3fb"
event handler
useEffect
useLayoutEffect
callback ref
```

depending on the job.

React's official refs docs recommend this post-render approach. ([React][3])

---

# 82. One complete lifecycle

Consider:

```jsx id="b3n6ds"
function Form() {
    const inputRef = useRef(null);

    function focusInput() {
        inputRef.current?.focus();
    }

    return (
        <>
            <input ref={inputRef} />
            <button onClick={focusInput}>
                Focus
            </button>
        </>
    );
}
```

## Initial render

```text id="j66tk6"
Form Fiber
   ↓
useRef(null)
   ↓
Hook created
   ↓
ref = { current: null }
   ↓
<input ref={ref}>
   ↓
render/reconciliation
```

At this point:

```text id="t2y5ah"
ref.current = null
```

Then commit:

```text id="bm7sfg"
create/commit input DOM node
   ↓
attach ref
   ↓
ref.current = HTMLInputElement
```

Now user clicks:

```text id="12v5pi"
button
 ↓
focusInput()
 ↓
inputRef.current.focus()
```

No React re-render is required.

If input is later removed:

```text id="n4b8gy"
commit deletion
 ↓
detach ref
 ↓
ref.current = null
```

That's the full lifecycle.

---

# 83. Current React architecture

Putting the last several topics together:

```text id="3jltm9"
                     Component
                         │
                         ▼
                  renderWithHooks
                         │
                         ▼
                      Fiber
                         │
          ┌──────────────┴──────────────┐
          ▼                             ▼
       useState                       useRef
          │                             │
          ▼                             ▼
      Hook state                    Hook state
          │                             │
          ▼                             ▼
    update queue                  { current: X }
          │                             │
          ▼                             ▼
        lane                    direct mutation
          │                             │
          ▼                             ▼
    schedule update                 no update
          │                             │
          ▼                             │
       render                            │
          │                             │
          └─────────────┬───────────────┘
                        ▼
                   reconciliation
                        │
                        ▼
                     commit
                        │
                 ┌──────┴──────┐
                 ▼             ▼
                DOM       attach/detach ref
```

This is the key distinction:

```text id="x0m08g"
useState
→ changing value participates in React rendering

useRef
→ changing .current does not
```

---

# 84. The mental model to retain

Don't think:

```text id="v0k5ga"
useRef = DOM reference
```

Think:

> **`useRef` is persistent mutable storage owned by a component's Hook structure.**

Then:

```text id="gbvrhi"
DOM ref
=
a special use of that storage
```

So:

```text id="ftl1gw"
useRef
    ↓
persistent object
    ↓
{ current: ... }
```

and:

```jsx id="t5l4vy"
<div ref={myRef} />
```

adds React's host/component ref-attachment behavior.

---

# 85. Revision Sheet

```text id="m4zqjp"
useRef(initialValue)
→ returns a persistent object:
   { current: initialValue }

Same object on future renders
→ yes.

Does ref.current mutation trigger render?
→ no.

Why?
→ mutation is ordinary JS; there is no state-dispatch/update queue.

Where is the ref retained?
→ in the Hook data associated with the component's Fiber.

Hook implementation model
→ mount creates ref object
→ update returns existing ref object

Why use ref?
→ persist mutable information that doesn't drive rendering.

Common uses
→ DOM nodes
→ timer IDs
→ imperative objects
→ mutable bookkeeping

ref
→ stable object

ref.current
→ mutable, non-reactive value

DOM ref
→ React populates .current during commit

Unmount
→ React sets object ref.current to null

Callback ref
→ React calls callback with node/null

React 19
→ function components can receive ref as a prop
→ forwardRef no longer needed for new function components
→ callback refs can return cleanup functions
→ element.ref is deprecated; ref is accessed via props.ref

useImperativeHandle
→ customize what a component exposes through its ref.

Don't read/write ref.current during render
→ except predictable initialization patterns.
```

The current React documentation supports these behaviors and React 19 changes. ([React][1])

---

# 86. The important React Hook comparison so far

```text id="u1v4pu"
                 React Hooks
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
      State                      Ref
        │                         │
   memoizedState             memoizedState
        │                         │
   update queue                 object
        │                         │
     dispatch                    │
        │                         │
      Lane                       │
        │                         │
   schedule work                 │
        │                         │
      render                      │
        │                         │
        ▼                         ▼
      reactive               non-reactive
```

And we haven't yet covered:

```text id="1d6kvl"
useMemo
useCallback
useReducer
useContext
useSyncExternalStore
useTransition
useDeferredValue
useId
useImperativeHandle
```

---

# Next Topic — `useMemo`, `useCallback`, and `React.memo`

This is another **extremely common interview area**, but we'll approach it from internals rather than memorized definitions.

We'll answer:

> What exactly does `useMemo` store inside the Hook?

> How does React determine whether to reuse the memoized value?

> Why is `useCallback(fn, deps)` basically related to `useMemo`?

> What exactly does `React.memo` compare?

> Why can a parent re-render a child even if the child's props "look the same"?

> Why does this fail?

```jsx
<Child
    data={{ name: "Alice" }}
/>
```

> Why doesn't `useMemo` automatically make an application faster?

And we'll connect:

```text id="8m31zc"
useMemo
   ↓
Hook.memoizedState
   ↓
[value, dependencies]

useCallback
   ↓
Hook.memoizedState
   ↓
[function, dependencies]

React.memo
   ↓
Fiber bailout
   ↓
props comparison
   ↓
skip child work
```

This will be the bridge from **React internals → performance optimization**.

[1]: https://react.dev/reference/react/useRef?utm_source=chatgpt.com "useRef – React"
[2]: https://react.dev/learn/referencing-values-with-refs?utm_source=chatgpt.com "Referencing Values with Refs – React"
[3]: https://react.dev/learn/manipulating-the-dom-with-refs?utm_source=chatgpt.com "Manipulating the DOM with Refs – React"
[4]: https://react.dev/blog/2024/12/05/react-19?utm_source=chatgpt.com "React v19 – React"
[5]: https://react.dev/blog/2024/04/25/react-19-upgrade-guide?utm_source=chatgpt.com "React 19 Upgrade Guide – React"
[6]: https://react.dev/learn/synchronizing-with-effects?utm_source=chatgpt.com "Synchronizing with Effects – React"
[7]: https://react.dev/learn/lifecycle-of-reactive-effects?utm_source=chatgpt.com "Lifecycle of Reactive Effects – React"
[8]: https://react.dev/reference/react/hooks?utm_source=chatgpt.com "Built-in React Hooks – React"

