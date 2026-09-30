# Topic 10 — `useEffect` Internals & the Effect Lifecycle

We now reach another **very high-frequency React interview topic**.

Most tutorials teach:

> "`useEffect` runs after render."

That's not wrong, but it's far too shallow for the level we're targeting.

We want to understand:

```text
Component render
      ↓
useEffect()
      ↓
Hook created/reused
      ↓
dependencies compared
      ↓
Effect stored on Fiber
      ↓
render completes
      ↓
commit
      ↓
passive effects are scheduled/flushed
      ↓
cleanup / setup
```

And the crucial mental model is:

> **An Effect is not a component lifecycle method. It is a synchronization process whose setup and cleanup can run multiple times as the component's reactive inputs change.**

That is also how the current React documentation describes Effects. ([React][1])

---

# 1. First: what is an Effect?

Consider:

```jsx
useEffect(() => {
    document.title = `Count: ${count}`;
}, [count]);
```

The purpose is not simply:

```text
"run some code after render"
```

The deeper purpose is:

> **Synchronize something outside React with the current React state/props.**

Examples:

```text
React state
   ↓
DOM API
   ↓
document.title
```

or:

```text
React props/state
   ↓
WebSocket
   ↓
connection
```

or:

```text
React state
   ↓
third-party widget
   ↓
map/chart/player
```

React's current documentation explicitly defines `useEffect` as a mechanism for synchronizing a component with an **external system**. ([React][1])

---

# 2. What counts as an external system?

Examples:

```text
Browser APIs
WebSocket
WebRTC
setInterval / setTimeout
DOM APIs
third-party widgets
subscriptions
network connections
non-React libraries
```

For example:

```jsx
useEffect(() => {
    const connection = createConnection(roomId);

    connection.connect();

    return () => {
        connection.disconnect();
    };
}, [roomId]);
```

React owns:

```text
roomId
component tree
state
props
```

The connection is outside React's component tree.

So the Effect synchronizes:

```text
React state
     ↕
external connection
```

This is exactly the use case emphasized in the current docs. ([React][1])

---

# 3. The most important conceptual correction

Don't think of:

```jsx
useEffect(...)
```

as:

```text
"componentDidMount"
+
"componentDidUpdate"
+
"componentWillUnmount"
```

That was a useful historical analogy, but it isn't the best modern mental model.

Think:

```text
Effect
  ↓
START synchronization
  ↓
continue while dependencies remain valid
  ↓
STOP synchronization
  ↓
START again with new values
```

React's current documentation explicitly says that an Effect has a lifecycle different from the component's mount/update/unmount lifecycle. ([React][2])

---

# 4. An Effect has two operations

An Effect can conceptually do two things:

### Setup

```jsx
useEffect(() => {
    connection.connect();
});
```

### Cleanup

```jsx
useEffect(() => {
    connection.connect();

    return () => {
        connection.disconnect();
    };
});
```

So:

```text
Effect
 ├── setup
 └── cleanup
```

React describes this as:

> start synchronizing → later stop synchronizing. ([React][2])

---

# 5. The lifecycle of an Effect

Suppose:

```jsx
useEffect(() => {
    connect(roomId);

    return () => {
        disconnect(roomId);
    };
}, [roomId]);
```

Initial mount:

```text
render
 ↓
commit
 ↓
setup(roomId = 1)
```

Then:

```text
roomId 1 → 2
```

React does:

```text
render
 ↓
commit
 ↓
cleanup(roomId = 1)
 ↓
setup(roomId = 2)
```

Finally:

```text
component removed
 ↓
cleanup(roomId = 2)
```

React's current `useEffect` documentation specifies this exact sequence. ([React][1])

---

# 6. Why cleanup happens before setup

Suppose:

```text
old room = Room A
new room = Room B
```

If React simply did:

```text
connect(B)
disconnect(A)
```

you could temporarily have:

```text
A connected
B connected
```

The intended synchronization model is:

```text
disconnect(A)
      ↓
connect(B)
```

So when dependencies change:

```text
old setup
   ↓
cleanup old synchronization
   ↓
setup new synchronization
```

React documents that the cleanup runs first with the old values, then setup runs with the new values. ([React][1])

---

# 7. Dependency array

Consider:

```jsx
useEffect(() => {
    console.log(roomId);
}, [roomId]);
```

The dependency array tells React:

> "This Effect depends on `roomId`."

React compares each dependency with its previous value using:

```javascript
Object.is(previous, next)
```

The current public API documentation explicitly specifies `Object.is` comparison. ([React][1])

---

# 8. The three dependency-array forms

## No array

```jsx
useEffect(() => {
    ...
});
```

Conceptually:

```text
every commit
   ↓
cleanup
   ↓
setup
```

assuming a cleanup exists.

React documents that omitting the array causes the Effect to run after every commit. ([React][1])

---

## Empty array

```jsx
useEffect(() => {
    ...
}, []);
```

There are no reactive dependencies.

Conceptually:

```text
mount
 ↓
setup

unmount
 ↓
cleanup
```

But don't interpret this as a magical guarantee that the code literally runs exactly once in every development circumstance: Strict Mode intentionally performs an extra setup/cleanup cycle in development. ([React][1])

---

## Dependency array

```jsx
useEffect(() => {
    ...
}, [roomId]);
```

Conceptually:

```text
mount
 → setup

roomId unchanged
 → no new synchronization

roomId changed
 → cleanup old
 → setup new

unmount
 → cleanup
```

---

# 9. Why use `Object.is`?

Suppose:

```jsx
const [count, setCount] = useState(0);

useEffect(() => {
    ...
}, [count]);
```

Previous:

```text
0
```

Next:

```text
0
```

Then:

```javascript
Object.is(0, 0)
```

is true.

No dependency change.

But:

```text
0 → 1
```

gives:

```javascript
Object.is(0, 1)
```

false.

React re-synchronizes the Effect.

---

# 10. Objects and dependency arrays

Now:

```jsx
const options = {
    roomId,
    serverUrl
};

useEffect(() => {
    connect(options);
}, [options]);
```

Every render creates:

```text
new object
```

So:

```text
previous options
      ≠
new options
```

even when:

```text
roomId
serverUrl
```

didn't change.

Therefore:

```text
Object.is(oldOptions, newOptions)
→ false
```

and the Effect can re-run unnecessarily.

React's current documentation explicitly warns that objects and functions created during rendering can cause Effects to re-run more often than needed. ([React][1])

---

# 11. A common fix

Instead of:

```jsx
const options = {
    roomId,
    serverUrl
};

useEffect(() => {
    connect(options);
}, [options]);
```

create the object inside the Effect:

```jsx
useEffect(() => {
    const options = {
        roomId,
        serverUrl
    };

    connect(options);
}, [roomId, serverUrl]);
```

Now the dependency array tracks the actual reactive values.

React explicitly recommends this style when the object itself doesn't need to be reactive. ([React][1])

---

# 12. Function dependencies

Same problem:

```jsx
function createOptions() {
    return {
        roomId
    };
}

useEffect(() => {
    connect(createOptions());
}, [createOptions]);
```

`createOptions` is a new function on every render.

So:

```text
previous function
     ≠
new function
```

and the Effect can re-run every time.

Again, React recommends restructuring the code to remove unnecessary function dependencies before reaching for memoization. ([React][1])

---

# 13. Does React compare dependency arrays by reference?

This is a subtle interview question.

Suppose:

```jsx
useEffect(fn, [a, b]);
```

React doesn't simply do:

```javascript
Object.is(previousArray, nextArray)
```

because the array itself can be a new array each render.

Instead, React compares the **individual dependency entries**:

```text
a previous vs a next
b previous vs b next
```

using `Object.is`.

The public docs explicitly say React compares each dependency with its previous value using `Object.is`. ([React][1])

---

# 14. Conceptual dependency algorithm

You can imagine:

```javascript
function areHookInputsEqual(nextDeps, prevDeps) {
    if (prevDeps === null) {
        return false;
    }

    for (let i = 0; i < nextDeps.length; i++) {
        if (!Object.is(nextDeps[i], prevDeps[i])) {
            return false;
        }
    }

    return true;
}
```

This is a simplified representation of the concept.

The actual React implementation performs development validation and has details around dependency lengths/types, but this is the important algorithmic idea.

---

# 15. The Effect is stored internally

Now let's connect this to our previous Hook discussion.

We learned:

```text
Fiber
 ↓
memoizedState
 ↓
Hook → Hook → Hook
```

For:

```jsx
useEffect(...)
```

React creates a Hook node like other Hooks.

But there's more.

The Effect itself also needs to be stored so React can later execute it during commit/passive-effect processing.

Conceptually:

```text
Fiber
 ├── memoizedState
 │      ↓
 │    Hook
 │
 └── updateQueue
        ↓
      Effects
```

The current Hook implementation stores effect-related information in the Fiber's update queue. The exact current representation has evolved, but `ReactFiberHooks.js` contains the machinery for pushing Effect objects onto the component's effect queue, while commit code later traverses and invokes them.

---

# 16. Function component `updateQueue`

This is an interesting distinction from `useState`.

Recall:

```text
Fiber.memoizedState
```

points to:

```text
Hook list
```

For Effects, React also uses the Fiber's:

```text
updateQueue
```

to retain the component's effect list.

Conceptually:

```text
Fiber
 ├── memoizedState
 │    └── Hook → Hook → Hook
 │
 └── updateQueue
      └── Effect → Effect → Effect
```

The current React codebase uses the component update queue for effect-related data during the function-component render/commit pipeline. The exact structure is internal and can change between versions.

---

# 17. The Effect object

Conceptually, an Effect can be thought of as something like:

```javascript
{
    tag,
    create,
    destroy,
    deps,
    next
}
```

Where:

```text
tag
→ what kind of effect / whether it should run

create
→ setup function

destroy
→ cleanup function

deps
→ dependency values

next
→ next Effect
```

This reflects the architecture used internally, although current React contains additional flags/fields and implementation-specific details.

---

# 18. Effect list

Suppose:

```jsx
function Component() {
    useEffect(effectA, [a]);
    useEffect(effectB, [b]);
    useEffect(effectC, [c]);
}
```

Conceptually React can have:

```text
Effect A
   ↓
Effect B
   ↓
Effect C
```

The current reconciler has Effect-list machinery that links effect objects so commit processing can traverse them.

Older source explanations often call this a circular effect list; the high-level concept remains useful, although exact internal organization evolves. A historical React implementation shows `pushEffect` maintaining a circular list on the component update queue.

---

# 19. Don't confuse the Hook list with Effect list

We now have two different chains.

### Hook chain

```text
Hook → Hook → Hook
```

This is:

```text
Fiber.memoizedState
```

### Effect chain

```text
Effect → Effect → Effect
```

stored through effect/update-queue machinery.

So:

```text
Hook list
≠
Effect list
```

An Effect Hook can reference the Effect object representing its current effect.

This distinction is excellent interview material.

---

# 20. Mounting `useEffect`

Consider:

```jsx
useEffect(() => {
    connect();
    return () => disconnect();
}, []);
```

During render, conceptually:

```text
useEffect
   ↓
mountEffect
   ↓
create Hook
   ↓
create Effect object
   ↓
store Effect
   ↓
mark Fiber as containing passive work
```

A historical/current-source-derived implementation pattern shows `mountEffectImpl` creating the Hook, setting passive-effect flags on the currently rendering Fiber, and attaching the Effect object to `hook.memoizedState`; current React still follows this broad architecture even though the exact flags and implementation have evolved. ([JSer][3])

---

# 21. Why mark the Fiber?

React needs to know:

> "This Fiber contains an Effect that needs processing during commit/passive-effect flushing."

Conceptually:

```text
Fiber.flags
      ↓
Passive
```

Then:

```text
commit machinery
      ↓
there are passive effects
      ↓
schedule/flush them
```

The exact flag names changed across React versions, but the architecture remains: render records that passive work exists, and commit/passive-effect processing later handles it.

---

# 22. Dependency comparison during update

Now suppose:

```jsx
useEffect(() => {
    connect(roomId);

    return () => disconnect(roomId);
}, [roomId]);
```

First render:

```text
previous deps = null
next deps = [1]
```

There is no previous Effect to reuse.

So React marks it to run.

Next render:

```text
previous deps = [1]
next deps = [1]
```

React checks:

```javascript
Object.is(1, 1)
```

true.

Therefore:

```text
dependencies unchanged
```

and React does not need to re-run the synchronization.

---

# 23. Dependency changes

Now:

```text
previous deps = [1]
next deps = [2]
```

Comparison:

```javascript
Object.is(1, 2)
```

false.

So React needs to synchronize again.

Conceptually:

```text
changed dependency
      ↓
mark Effect as needing execution
      ↓
commit/passive phase
      ↓
cleanup old
      ↓
setup new
```

React's documentation specifies that changed dependencies trigger cleanup followed by setup after the commit. ([React][1])

---

# 24. The important point: comparison happens during render

The Effect function itself does **not** run during:

```text
useEffect(...)
```

during render.

Instead, React records:

```text
"this Effect should run later."
```

and compares dependencies while rendering.

That gives:

```text
render
 ↓
register/check Effect
 ↓
finish render
 ↓
commit
 ↓
run Effect later
```

This is a crucial distinction.

---

# 25. Why doesn't React just run the Effect immediately?

Because rendering must stay pure.

Imagine:

```jsx
function Component({ roomId }) {
    useEffect(() => {
        connect(roomId);
    }, [roomId]);

    return <div />;
}
```

If `connect()` ran while the component function was executing:

```text
render
 ↓
connect()
 ↓
render continues
```

then rendering would produce an external side effect.

That would interfere with React's ability to:

```text
pause
restart
discard
replay
```

rendering work.

Instead:

```text
render
 ↓
record effect
 ↓
commit
 ↓
run effect
```

This is one of the fundamental reasons Effects exist as a separate mechanism.

React's docs explicitly say rendering should be pure and Effects are for synchronization outside the render calculation. ([React][1])

---

# 26. Effect belongs to a particular render

This is a subtle but beautiful concept.

Suppose:

```jsx
function Greeting({ name }) {
    useEffect(() => {
        console.log(name);
    }, [name]);

    return <h1>{name}</h1>;
}
```

Render #1:

```text
name = Alice
```

The Effect closure captures:

```text
Alice
```

Render #2:

```text
name = Bob
```

A new Effect closure captures:

```text
Bob
```

So think:

```text
Render #1
  └── Effect #1 → "Alice"

Render #2
  └── Effect #2 → "Bob"
```

Rather than:

```text
one global effect
```

This is why cleanup receives the old render's captured values.

This closure model is consistent with React's documentation, which emphasizes that each Effect synchronizes according to the values from its render. ([React][2])

---

# 27. Cleanup sees old values

Suppose:

```jsx
useEffect(() => {
    console.log("connect", roomId);

    return () => {
        console.log("disconnect", roomId);
    };
}, [roomId]);
```

Transition:

```text
roomId: 1 → 2
```

You'll conceptually get:

```text
disconnect 1
connect 2
```

not:

```text
disconnect 2
connect 2
```

Why?

Because cleanup belongs to the **previous Effect/render**.

It closes over:

```text
roomId = 1
```

That's a very common interview question.

---

# 28. Strict Mode

Now the famous:

```text
"Why is my Effect running twice?"
```

In development Strict Mode:

```text
setup
 ↓
cleanup
 ↓
setup
```

before the normal production-like setup.

React explicitly documents this as a **development-only stress test** designed to verify that cleanup properly mirrors setup. ([React][1])

So:

```text
development:
setup → cleanup → setup

production:
setup
```

for the initial mounting sequence.

---

# 29. Why does Strict Mode do this?

Suppose:

```jsx
useEffect(() => {
    socket.connect();
}, []);
```

with no cleanup.

Strict Mode exposes the problem:

```text
connect
connect
```

Your code is not resilient to setup happening more than once.

The correct version:

```jsx
useEffect(() => {
    socket.connect();

    return () => {
        socket.disconnect();
    };
}, []);
```

Now:

```text
setup
 ↓
cleanup
 ↓
setup
```

still produces a correct external system state.

That's exactly what React's docs mean by "mirroring" setup with cleanup. ([React][1])

---

# 30. Strict Mode does not mean production runs twice

This should be stated clearly.

When you see:

```text
setup
cleanup
setup
```

in development, don't conclude:

> "React runs every Effect twice."

It's an intentional development behavior under Strict Mode.

React documents it as development-only. ([React][1])

---

# 31. Passive Effects

`useEffect` is a **passive Effect**.

This matters because it isn't the same as:

```jsx
useLayoutEffect(...)
```

Conceptually:

```text
useEffect
    ↓
passive effect

useLayoutEffect
    ↓
layout effect
```

React uses different internal effect categories/flags for them.

The current docs distinguish them based on when browser painting and blocking behavior occur. ([React][1])

---

# 32. Why is it called "passive"?

Because the browser generally gets an opportunity to paint before React flushes passive Effects when those Effects aren't interaction-caused.

So conceptually:

```text
commit DOM
    ↓
browser may paint
    ↓
flush passive effects
```

React's current documentation says that if an Effect wasn't caused by an interaction, React will generally let the browser paint the updated screen before running it. ([React][1])

But "generally" matters.

The exact scheduling can vary depending on the situation.

---

# 33. `useEffect` does not mean "always after paint"

This is a major interview trap.

People often memorize:

> "`useEffect` runs after paint."

That's too absolute.

React's current documentation says:

* for effects not caused by interactions, React **will generally** let the browser paint first;
* for interaction-caused effects, React **may** run the Effect before paint. ([React][1])

So the safest answer is:

> `useEffect` runs after commit, and React generally lets the browser paint first for non-interaction-driven Effects, but the exact timing is not an absolute "always after paint" guarantee.

That's a much better senior-level answer.

---

# 34. `useLayoutEffect`

Now compare:

```jsx
useLayoutEffect(() => {
    ...
});
```

This is for work that needs to happen before the browser repaints.

Conceptually:

```text
Render
 ↓
Commit DOM
 ↓
Layout effects
 ↓
Browser paint
```

whereas passive Effects are generally:

```text
Render
 ↓
Commit DOM
 ↓
Browser paint opportunity
 ↓
Passive effects
```

The current `useLayoutEffect` documentation states that its code and state updates block the browser from repainting. ([React][4])

---

# 35. Example: tooltip positioning

Suppose you render:

```jsx
<Tooltip />
```

and after DOM insertion you need to measure:

```javascript
element.getBoundingClientRect()
```

and reposition it before the user sees it.

With `useEffect`:

```text
DOM appears
 ↓
paint
 ↓
measure
 ↓
position
```

could cause visible flicker.

With:

```jsx
useLayoutEffect(() => {
    ...
});
```

the calculation can happen before repaint.

This is exactly the type of visual synchronization React recommends `useLayoutEffect` for. ([React][1])

---

# 36. Why not use `useLayoutEffect` everywhere?

Because it blocks painting.

Suppose you do:

```jsx
useLayoutEffect(() => {
    hugeCalculation();
});
```

for a large amount of work.

The browser has to wait.

So:

```text
useLayoutEffect
→ powerful for pre-paint synchronization

but

useLayoutEffect
→ can hurt responsiveness if overused
```

React's documentation explicitly warns that excessive `useLayoutEffect` usage can make the app slow. ([React][4])

---

# 37. `useEffect` vs `useLayoutEffect`

Remember:

| `useEffect`                                             | `useLayoutEffect`               |
| ------------------------------------------------------- | ------------------------------- |
| Passive Effect                                          | Layout Effect                   |
| Usually after paint opportunity                         | Before repaint                  |
| Doesn't normally block paint                            | Blocks repaint                  |
| Good for subscriptions/network/external synchronization | Good for DOM measurement/layout |
| Preferred by default                                    | Use when timing requires it     |

This is a **timing distinction**, not:

```text
useEffect = asynchronous
useLayoutEffect = synchronous
```

That would be too simplistic.

---

# 38. Internal effect flags

At implementation level, React has multiple layers of flags.

Conceptually:

```text
Fiber flags
    ↓
"this Fiber/subtree has passive/layout work"

Effect flags/tags
    ↓
"this particular Effect should run"
```

Historically you'll see names like:

```text
HookPassive
HookLayout
HookHasEffect
PassiveEffect
LayoutEffect
```

The current implementation has additional static and dev-related flags, and exact names can change.

The important distinction is:

```text
Fiber-level flags
+
Effect-level tags
```

coordinate the later commit processing.

---

# 39. `HookHasEffect`

One especially useful historical/internal concept is:

```text
HookHasEffect
```

Think:

```text
Effect exists
+
Does this Effect need to execute during this commit?
```

If dependencies haven't changed, React can keep the Effect but omit the "run me" marker for that update.

The current source's effect implementation still uses effect flags to distinguish effects that need execution from ones whose dependencies are unchanged, although the surrounding flags have evolved. Historical source analysis illustrates the mechanism clearly. ([JSer][3])

---

# 40. Why keep the Effect even when dependencies don't change?

Because the Effect still belongs to the component.

For:

```jsx
useEffect(fn, [roomId]);
```

when `roomId` doesn't change, React doesn't need to destroy its conceptual Effect record entirely.

It can represent:

```text
same Effect
same deps
not scheduled for execution this time
```

This keeps the Hook structure stable.

---

# 41. The update path

Conceptually:

```text
useEffect(create, nextDeps)
       ↓
updateEffectImpl()
       ↓
get corresponding Hook
       ↓
read previous Effect
       ↓
compare previous deps vs next deps
       ↓
same?
 ┌─────┴─────┐
 yes         no
 │            │
 ▼            ▼
don't mark   mark to run
```

Then commit/passive processing handles only the Effects marked for execution.

---

# 42. Dependency equality and referential identity

Example:

```jsx
const options = { roomId };

useEffect(() => {
    connect(options);
}, [options]);
```

Even:

```text
roomId = 1
```

on both renders doesn't help if:

```text
options₁ !== options₂
```

because:

```javascript
Object.is(options₁, options₂)
```

is false.

So the Effect re-synchronizes.

This is why understanding **JavaScript object identity** is essential for understanding React dependency arrays.

---

# 43. `useMemo` can stabilize an object—but don't jump there first

You could do:

```jsx
const options = useMemo(() => ({
    roomId
}), [roomId]);

useEffect(() => {
    connect(options);
}, [options]);
```

Now:

```text
same roomId
   ↓
same memoized object
```

But React's current docs generally recommend first **removing unnecessary object/function dependencies**, rather than reaching immediately for `useMemo`/`useCallback`. ([React][1])

We'll cover the memoization trade-offs separately.

---

# 44. The Effect closure

Consider:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    useEffect(() => {
        console.log(count);
    }, [count]);

    return ...;
}
```

On render #1:

```text
count = 0
```

The Effect function closes over:

```text
0
```

On render #2:

```text
count = 1
```

a new Effect function closes over:

```text
1
```

So:

```text
Effect #1 → count 0
Effect #2 → count 1
```

This is why an Effect sees the values from the render that created it.

---

# 45. Stale closures

Now:

```jsx
useEffect(() => {
    setInterval(() => {
        console.log(count);
    }, 1000);
}, []);
```

The Effect captured the initial:

```text
count
```

because the dependency list says:

```text
[]
```

The interval callback keeps that closure.

So if you expect:

```text
0
1
2
3
...
```

you may instead repeatedly see:

```text
0
```

This is a **stale closure** problem.

The current React docs demonstrate the related timer/state issue and recommend updater functions when you need to update state from an Effect without making the current state a dependency. ([React][1])

---

# 46. Fix using an updater

Instead of:

```jsx
useEffect(() => {
    const id = setInterval(() => {
        setCount(count + 1);
    }, 1000);

    return () => clearInterval(id);
}, [count]);
```

which recreates the interval whenever `count` changes, use:

```jsx
useEffect(() => {
    const id = setInterval(() => {
        setCount(c => c + 1);
    }, 1000);

    return () => clearInterval(id);
}, []);
```

Now:

```text
Effect
doesn't need current count
```

because:

```text
c => c + 1
```

receives the latest state during update processing.

React's current docs explicitly recommend this pattern. ([React][1])

---

# 47. Effect dependency array is not a "run condition"

This is subtle.

Beginners often think:

```jsx
useEffect(fn, [count]);
```

means:

> "Run this code when count changes."

That's not the best mental model.

Instead:

> **The dependency list describes the reactive values that determine this synchronization.**

Then React uses dependency changes to determine when the synchronization needs to be restarted.

This distinction becomes important for writing correct Effects.

---

# 48. Dependencies are not manually chosen

Suppose:

```jsx
useEffect(() => {
    console.log(user.name);
}, []);
```

The code reads:

```text
user
```

which is a reactive value if it comes from props/state/local component variables.

The dependency list should normally include:

```jsx
[user]
```

or an appropriate reactive value such as:

```jsx
[user.name]
```

depending on the intended synchronization.

React's docs define dependencies as the reactive values used inside the setup/cleanup code and recommend letting the linter verify them. ([React][1])

---

# 49. Why the linter complains

The exhaustive-deps rule is trying to protect this model:

```text
Effect
 ↓
synchronization depends on X
 ↓
X changes
 ↓
Effect must re-synchronize
```

If you omit X:

```text
X changes
 ↓
React doesn't know this Effect needs re-synchronization
```

That can produce stale values.

So the linter is not merely being pedantic.

It's enforcing the synchronization model.

---

# 50. Don't silence the dependency linter blindly

Suppose:

```jsx
useEffect(() => {
    fetchData(userId);
}, []);
```

and the linter says:

```text
userId missing
```

You shouldn't automatically write:

```jsx
// eslint-disable...
```

The important question is:

> **Does this Effect logically synchronize with `userId`?**

If yes:

```jsx
[userId]
```

probably belongs there.

If no, restructure the code so the Effect no longer reads that reactive value.

React's docs explicitly recommend restructuring Effect dependencies rather than fighting the dependency rule. ([React][1])

---

# 51. Effects don't run during SSR

Another important interview point:

```text
Server rendering
    ↓
Effects do not run
```

React's current `useEffect` and `useLayoutEffect` documentation both explicitly state that Effects only run on the client. ([React][1])

So:

```text
SSR
→ render HTML / server component output

Client
→ hydrate
→ Effects can run
```

This will become very important when we study SSR and hydration.

---

# 52. Why this matters for data fetching

You may write:

```jsx
useEffect(() => {
    fetch("/api/users")
}, []);
```

This won't fetch during server rendering.

So:

```text
server response
→ doesn't contain data fetched by that client Effect

browser loads
→ hydrate
→ Effect runs
→ data request
```

That's one reason modern React frameworks often provide dedicated server/data-fetching mechanisms rather than relying entirely on client Effects. React's current docs explicitly note that framework data-fetching mechanisms can be more efficient than manual Effect fetching. ([React][1])

---

# 53. Effect update flow

Let's trace:

```jsx
useEffect(() => {
    subscribe(userId);

    return () => unsubscribe(userId);
}, [userId]);
```

Suppose:

```text
initial userId = 1
```

### Render

```text
useEffect(...)
```

React:

```text
create Hook
create Effect
deps = [1]
mark passive work
```

### Commit

DOM commit completes.

Then passive Effect processing:

```text
setup(1)
```

---

# 54. Update flow

Now:

```text
userId 1 → 2
```

### Render

React encounters the Effect:

```text
old deps = [1]
new deps = [2]
```

Comparison:

```javascript
Object.is(1, 2) === false
```

So:

```text
mark Effect to run
```

---

# 55. Commit/passive processing

After the render/commit work:

```text
cleanup(old Effect)
       ↓
unsubscribe(1)

setup(new Effect)
       ↓
subscribe(2)
```

React's current documentation specifies cleanup-before-setup when dependencies change. ([React][1])

---

# 56. Unmount

Suppose:

```text
User component removed
```

React executes the Effect's cleanup:

```text
unsubscribe(2)
```

The important principle:

```text
setup
must have corresponding cleanup
```

when the external system requires teardown.

---

# 57. Why cleanup isn't only "unmount logic"

This is probably the most common interview trap.

People say:

> "`return () => ...` runs on unmount."

Incomplete.

It can run:

```text
1. before Effect re-runs because dependencies changed
2. when component is removed
3. in development Strict Mode as part of the extra setup/cleanup stress test
```

React's docs explicitly call this out. ([React][1])

---

# 58. The "symmetry" rule

A very useful design rule:

```text
setup:
connect()

cleanup:
disconnect()
```

or:

```text
setup:
addEventListener()

cleanup:
removeEventListener()
```

or:

```text
setup:
setInterval()

cleanup:
clearInterval()
```

or:

```text
setup:
subscribe()

cleanup:
unsubscribe()
```

The cleanup should **undo/stop what setup started**.

React's current documentation repeatedly emphasizes this symmetry. ([React][1])

---

# 59. Bad cleanup

```jsx
useEffect(() => {
    return () => {
        sendAnalyticsEvent();
    };
}, []);
```

That cleanup is not obviously undoing the setup.

React's current docs explicitly say that cleanup without corresponding setup is usually a code smell. ([React][1])

---

# 60. A classic subscription example

Bad:

```jsx
useEffect(() => {
    socket.on("message", handleMessage);
}, []);
```

Every mount adds a subscription, but nothing removes it.

Correct:

```jsx
useEffect(() => {
    socket.on("message", handleMessage);

    return () => {
        socket.off("message", handleMessage);
    };
}, []);
```

Now:

```text
setup
→ subscribe

cleanup
→ unsubscribe
```

is symmetric.

---

# 61. Internal timeline

Now let's connect our Effect topic to Fiber.

```text
Component render
       │
       ▼
renderWithHooks
       │
       ▼
useEffect()
       │
       ▼
Hook list
       │
       ▼
Effect object
       │
       ▼
Fiber flags/updateQueue
       │
       ▼
render completes
       │
       ▼
commit
       │
       ▼
passive effect processing
       │
       ├── cleanup where needed
       │
       └── setup where needed
```

This is the important architecture.

---

# 62. Where exactly does passive flushing fit?

At a high level:

```text
render
 ↓
complete WIP tree
 ↓
commit DOM changes
 ↓
schedule/flush passive effects
```

Historical React internals descriptions show passive cleanup and mount functions such as:

```text
commitPassiveUnmountEffects
commitPassiveMountEffects
```

and describe passive effects as being flushed via `flushPassiveEffects`. ([JSer][5])

The exact implementation has evolved, but this remains a useful architectural model:

```text
passive cleanup
→ passive setup
```

during passive-effect processing.

---

# 63. Why cleanup is separated from DOM commit

Suppose:

```text
DOM update
+
subscription update
```

React first needs a consistent commit of the React tree/host environment.

Passive Effects are not part of the pure tree calculation.

So:

```text
Render:
calculate

Commit:
apply React/host changes

Passive Effects:
synchronize external systems
```

This separation is an important architectural benefit.

---

# 64. `useEffect` versus event handler

Another frequent interview question:

> Should I put this logic in an Effect or an event handler?

Consider:

```jsx
<button onClick={handleBuy}>
    Buy
</button>
```

If the user explicitly causes:

```text
purchase
```

that's event-driven.

Don't do:

```jsx
useEffect(() => {
    if (shouldBuy) {
        buy();
    }
}, [shouldBuy]);
```

unless the purchase genuinely represents synchronization with an external state condition.

React's current documentation encourages keeping event-specific actions in event handlers and using Effects for synchronization with external systems. ([React][1])

---

# 65. Don't use Effects to derive data unnecessarily

Bad:

```jsx
const [firstName, setFirstName] = useState("John");
const [lastName, setLastName] = useState("Smith");
const [fullName, setFullName] = useState("");

useEffect(() => {
    setFullName(firstName + " " + lastName);
}, [firstName, lastName]);
```

This introduces:

```text
render
 ↓
Effect
 ↓
setFullName
 ↓
another render
```

You could simply do:

```jsx
const fullName = firstName + " " + lastName;
```

React's current documentation strongly encourages avoiding Effects when you're just deriving data from existing React state/props. ([React][1])

This is a very common interview/design question.

---

# 66. Why unnecessary Effects create extra renders

Consider:

```text
state
 ↓
render
 ↓
Effect
 ↓
setState
 ↓
render again
```

If the Effect exists only to calculate something that could have been calculated during render:

```text
unnecessary second render
```

So one of the most important modern React skills is:

> **Use Effects for synchronization, not as a generic "run this after rendering" mechanism.**

---

# 67. Data fetching caveat

Fetching with Effects is possible:

```jsx
useEffect(() => {
    fetch(...)
}, []);
```

but modern React documentation notes that manual fetching in Effects can lead to repetitive code and makes optimizations like caching and server rendering harder. ([React][1])

Interview answer:

> You can fetch data in an Effect on the client, but in framework-based applications I'd generally prefer the framework's data-fetching mechanism when available.

---

# 68. `async` directly in `useEffect`

Don't do:

```jsx
useEffect(async () => {
    const data = await fetchData();
}, []);
```

Why?

Because an Effect setup function is expected to return either:

```text
nothing
```

or:

```text
cleanup function
```

An `async` function returns a Promise.

That's a different contract.

Instead:

```jsx
useEffect(() => {
    async function load() {
        const data = await fetchData();
        ...
    }

    load();
}, []);
```

React's docs show this style for manual asynchronous fetching. ([React][1])

---

# 69. Race conditions in fetching

Suppose:

```text
request Alice
```

then quickly:

```text
request Bob
```

Alice may finish after Bob.

Then:

```text
Alice response arrives
 ↓
overwrites Bob's UI
```

A common cleanup pattern is:

```jsx
useEffect(() => {
    let ignore = false;

    fetchBio(person).then(result => {
        if (!ignore) {
            setBio(result);
        }
    });

    return () => {
        ignore = true;
    };
}, [person]);
```

React's current docs demonstrate this exact pattern for avoiding stale responses. ([React][1])

In real applications, cancellation with `AbortController` or a data-fetching library/framework can be preferable depending on the situation.

---

# 70. The dependency array doesn't control stale closures by itself

Suppose:

```jsx
useEffect(() => {
    console.log(count);
}, []);
```

Someone may think:

> "The Effect doesn't need to rerun, so it will see the latest count."

No.

It sees:

```text
count from the render that created the Effect
```

So:

```text
[] 
```

means:

> "This synchronization doesn't depend on changing reactive values."

It does **not** mean:

> "Give me the newest variables whenever I happen to execute."

That's why correct dependencies matter.

---

# 71. Effect and closure together

A useful mental model:

```text
Render #1
count = 0
    │
    └── Effect closure captures 0

Render #2
count = 1
    │
    └── New Effect closure captures 1
```

So each Effect setup/cleanup pair belongs to a particular render's values.

This explains:

```text
stale closures
cleanup seeing old props
dependency arrays
Effect re-synchronization
```

all with one model.

---

# 72. Internal implementation flow

Let's build a simplified internal implementation.

```javascript
function useEffect(create, deps) {
    const hook = updateWorkInProgressHook();

    const prevEffect = hook.memoizedState;

    if (areEqual(prevEffect.deps, deps)) {
        hook.memoizedState = {
            create,
            destroy: prevEffect.destroy,
            deps
        };

        // Don't mark to run
        return;
    }

    const effect = {
        create,
        destroy: undefined,
        deps
    };

    hook.memoizedState = effect;

    currentlyRenderingFiber.flags |= PassiveEffect;

    pushEffect(effect);
}
```

This is conceptual pseudocode.

It demonstrates:

```text
get Hook
   ↓
read previous Effect
   ↓
compare dependencies
   ↓
if changed → mark to run
   ↓
store Effect
   ↓
commit later
```

The real implementation includes significantly more logic and current-version-specific flags.

---

# 73. Commit-side pseudocode

Imagine:

```javascript
function flushPassiveEffects() {
    for (const effect of effects) {
        if (effect.shouldRun) {
            if (effect.destroy) {
                effect.destroy();
            }

            effect.destroy = effect.create();
        }
    }
}
```

Again, not React's source.

But conceptually:

```text
old cleanup
    ↓
new setup
```

is exactly the lifecycle you should remember.

---

# 74. Why cleanup receives no arguments

You write:

```jsx
return () => {
    disconnect(roomId);
};
```

The cleanup function gets the old values through the closure.

React doesn't need to say:

```javascript
cleanup(oldProps)
```

The closure already contains:

```text
roomId from that render
```

This is a nice example of React leveraging normal JavaScript closure semantics.

---

# 75. Why Effect setup shouldn't return arbitrary values

This is invalid:

```jsx
useEffect(() => {
    return 42;
}, []);
```

React expects the setup function's return value to represent cleanup when present.

So conceptually:

```text
setup()
   ↓
undefined
or
cleanup function
```

not arbitrary data.

---

# 76. A very common interview question

> Why does cleanup run before the next Effect?

Answer:

> Because React treats the Effect as a synchronization process. When dependencies change, the old synchronization must be stopped before the new synchronization is started, so React runs the previous cleanup first and then the new setup.

This matches React's documented lifecycle model. ([React][1])

---

# 77. Another interview question

> Why doesn't `useEffect` run during render?

Strong answer:

> Rendering should be a pure calculation and can be interrupted or restarted. Running external side effects during render would make the result dependent on how many times React performed rendering work. Instead, `useEffect` records the synchronization during render and React executes it later during commit/passive-effect processing.

This connects `useEffect` directly to Fiber and concurrent rendering.

---

# 78. Another interview question

> Is `useEffect` always asynchronous?

Don't say simply:

> "Yes."

That's imprecise.

A better answer:

> `useEffect` is a passive effect whose execution happens after the relevant commit, but its exact relationship to browser paint depends on how the update was caused. React generally allows the browser to paint first for non-interaction Effects, but interaction-caused Effects may run before paint. ([React][1])

---

# 79. Another interview question

> What's the difference between `useEffect` and `useLayoutEffect`?

Strong answer:

> Both let a component synchronize with external systems, but they differ in timing. `useLayoutEffect` runs in the commit sequence before the browser repaints and can block painting, making it appropriate for layout measurement or visual positioning. `useEffect` is passive and is generally allowed to run after the browser has a chance to paint. ([React][1])

---

# 80. Another interview question

> Where are Effects stored?

Strong interview-level answer:

> The Effect has a Hook node in the component's Hook list, and React also records effect information in the function component's internal update/effect queue so that commit/passive-effect processing can later traverse and execute the appropriate Effects. The exact internal list structure is implementation-specific and has evolved across React versions.

That's safer than claiming one exact internal field arrangement as a permanent API.

---

# 81. Another interview question

> How does React know whether an Effect should run?

Answer:

```text
previous dependency values
        ↓
current dependency values
        ↓
Object.is each pair
        ↓
any change?
    │
 ┌──┴───┐
no      yes
│        │
▼        ▼
skip    mark/run
```

React's public documentation explicitly defines dependency comparison using `Object.is`. ([React][1])

---

# 82. Another interview question

> Why does an Effect run twice in Strict Mode?

Answer:

> In development Strict Mode, React intentionally performs an extra setup→cleanup cycle before the first real setup to test whether the Effect's cleanup correctly mirrors its setup. This behavior is development-only. ([React][1])

---

# 83. Another interview question

> Why does my Effect run on every render even though I supplied dependencies?

Example:

```jsx
const options = {
    roomId
};

useEffect(() => {
    connect(options);
}, [options]);
```

Answer:

> Because the object is recreated on every render, so its reference changes. React compares dependencies using `Object.is`, and the new object is not the same reference as the previous one. Move the object creation inside the Effect or otherwise stabilize the dependency when that is actually appropriate. ([React][1])

---

# 84. Another interview question

> Is cleanup the same as `componentWillUnmount`?

Answer:

> No. Cleanup runs when an Effect needs to re-synchronize because dependencies changed, and it also runs when the component is removed. In development Strict Mode, React may also perform an extra cleanup as part of its Effect stress test. ([React][1])

---

# 85. The deepest mental model

Don't think:

```text
useEffect
=
run this later
```

Think:

```text
React render
      ↓
determine what external system
the component should currently
be synchronized with
      ↓
store Effect + dependencies
      ↓
commit UI
      ↓
run required synchronization
      ↓
when dependencies change:
    stop old synchronization
    start new synchronization
```

That's the modern React model.

---

# 86. Full example from render to Effect

```jsx
function ChatRoom({ roomId }) {
    useEffect(() => {
        const connection = createConnection(roomId);

        connection.connect();

        return () => {
            connection.disconnect();
        };
    }, [roomId]);

    return <div>Chat</div>;
}
```

Initial render:

```text
ChatRoom Fiber
      ↓
renderWithHooks
      ↓
useEffect
      ↓
Hook created
      ↓
Effect created
      ↓
deps = [1]
      ↓
mark passive work
      ↓
render complete
```

Commit:

```text
DOM committed
      ↓
passive effects processed
      ↓
connection.connect()
```

Then:

```text
roomId 1 → 2
```

Render:

```text
old deps = [1]
new deps = [2]

Object.is(1, 2) → false
      ↓
mark Effect
```

Commit/passive processing:

```text
cleanup old Effect
      ↓
connection.disconnect()

setup new Effect
      ↓
connection.connect(2)
```

Unmount:

```text
cleanup
 ↓
connection.disconnect(2)
```

That is the full lifecycle.

---

# 87. One final architecture diagram

```text
                  Component render
                         │
                         ▼
                  renderWithHooks
                         │
                         ▼
                     useEffect
                         │
                         ▼
                   Hook node
                         │
                         ▼
                    Effect data
                         │
                    ┌────┴────┐
                    │         │
                  create     deps
                    │         │
                    └────┬────┘
                         ▼
               dependency comparison
                         │
               ┌─────────┴─────────┐
               │                   │
           unchanged             changed
               │                   │
               ▼                   ▼
          don't run            mark passive
                                   │
                                   ▼
                           complete render
                                   │
                                   ▼
                                commit
                                   │
                                   ▼
                         passive effect flush
                                   │
                         ┌─────────┴─────────┐
                         ▼                   ▼
                    old cleanup          new setup
```

---

# 88. What you should remember

```text
useEffect
→ synchronize with external systems.

Effect lifecycle
→ setup → cleanup → setup ... as dependencies change.

Dependency comparison
→ individual dependencies compared using Object.is.

No dependency array
→ after every commit.

[]
→ no reactive dependencies; setup on mount and cleanup on removal,
  subject to development Strict Mode behavior.

Cleanup
→ not just unmount; also before re-synchronization.

Effect timing
→ passive; generally after paint opportunity, but not an absolute
  "always after paint" guarantee.

useLayoutEffect
→ before repaint; blocks painting.

SSR
→ Effects don't run on the server.

Fiber
→ Hook lives in Hook list; Effect information is recorded so
  commit/passive processing can execute it later.

Strict Mode
→ extra development setup → cleanup → setup cycle.

Important philosophy
→ don't use Effects merely to derive data that can be calculated
  during render.
```

React's current documentation supports these semantics and caveats. ([React][1])

---

# Current React architecture we've built

```text
                         JSX
                          ↓
                    React Element
                          ↓
                       Fiber
                          ↓
                  renderWithHooks
                          ↓
              ┌───────────┴───────────┐
              │                       │
           useState                useEffect
              │                       │
              ▼                       ▼
         Hook + queue            Hook + Effect
              │                       │
              ▼                       ▼
          state update          dependency compare
              │                       │
              ▼                       ▼
             Lane                passive work
              │                       │
              └───────────┬───────────┘
                          ▼
                      Work Loop
                          ↓
                    reconciliation
                          ↓
                     completeWork
                          ↓
                    Finished WIP
                          ↓
                        Commit
                          │
                 ┌────────┴─────────┐
                 ▼                  ▼
                DOM            passive effects
                                   │
                             cleanup → setup
```

At this point we have covered the major internal chain from **JSX all the way through state, Fiber, scheduling, reconciliation, and Effects**.

## Next Topic — `useRef` Internals

We'll tackle:

```jsx
const ref = useRef(initialValue);
```

and answer:

> Where does the ref object live?

> Why does changing `ref.current` not trigger a render?

> Why does the same ref object survive re-renders?

> How does a DOM ref get attached during commit?

> What's the difference between object refs and callback refs?

> Why can refs be used to access DOM nodes without causing renders?

> How does `useRef` differ internally from `useState`?

This will also lead naturally into **DOM refs, callback refs, React 19 ref behavior, and `useImperativeHandle`**.

[1]: https://react.dev/reference/react/useEffect?utm_source=chatgpt.com "useEffect – React"
[2]: https://react.dev/learn/lifecycle-of-reactive-effects?utm_source=chatgpt.com "Lifecycle of Reactive Effects – React"
[3]: https://jser.dev/2023-07-08-how-does-useeffect-work/?utm_source=chatgpt.com "How does useEffect() work internally in React?"
[4]: https://react.dev/reference/react/useLayoutEffect?utm_source=chatgpt.com "useLayoutEffect – React"
[5]: https://jser.dev/react/2022/01/19/lifecycle-of-effect-hook/?utm_source=chatgpt.com "The lifecycle of effect hooks in React"

