# Topic 6 — React Fiber in Depth

Now we get into the **React internals that interviewers often use to distinguish someone who has used React from someone who understands how React works**.

We'll build Fiber from the problem it was designed to solve.

The current React source has a `FiberNode` structure containing fields such as `tag`, `key`, `elementType`, `type`, `stateNode`, `return`, `child`, `sibling`, `pendingProps`, `memoizedProps`, `updateQueue`, `memoizedState`, `flags`, `lanes`, `childLanes`, and `alternate`. ([GitHub][1])

And the current reconciler still uses a work-in-progress/alternate model; `createWorkInProgress` explicitly describes the structure as using a **double-buffering pooling technique** for two versions of the tree. ([GitHub][1])

---

# 1. First: why was Fiber needed?

Before getting into the data structure, let's start with the problem.

Imagine a UI tree:

```text
App
├── Header
├── Sidebar
├── Main
│   ├── ProductList
│   │   ├── Product
│   │   ├── Product
│   │   ├── Product
│   │   └── ...
│   └── Recommendations
└── Footer
```

A rendering update may require React to process thousands of nodes.

If React treats the entire render as one giant synchronous operation:

```text
start
  ↓
process everything
  ↓
finish
  ↓
commit
```

then JavaScript can occupy the main thread for too long.

That means the browser may have difficulty responding to:

```text
clicks
typing
scrolling
animations
layout/paint work
```

The fundamental architectural problem was:

> **How can React represent rendering work so that it can be broken into manageable units and scheduled?**

That is what Fiber was designed to solve.

---

# 2. What is Fiber?

The simplest useful definition is:

> **A Fiber is React's internal data structure representing a unit of work for a node in the rendered tree.**

Don't interpret "unit of work" as necessarily meaning:

```text
one DOM operation
```

It could represent:

```text
a function component
a host element
a context boundary
a Suspense boundary
a fragment
etc.
```

A Fiber stores both identity and work-related information.

The current `FiberNode` constructor makes this very concrete: it stores the node's type/identity, tree links, props/state, update information, effects/flags, scheduling lanes, and an alternate Fiber. ([GitHub][1])

---

# 3. Fiber is a data structure, not a DOM node

Suppose:

```jsx
function User() {
    return <div>Hello</div>;
}
```

You can conceptually have:

```text
Fiber(User)
     │
     ▼
Fiber(div)
```

and separately:

```text
Fiber(div)
     │
     ▼
DOMElement(<div>)
```

So:

```text
Fiber ≠ DOM
```

A Fiber is React's internal representation.

The `stateNode` field is where the relationship to an actual host instance or other associated state can be stored; for a DOM host Fiber, it can point toward the actual host instance. The current Fiber constructor initializes this field to `null`, and host creation later fills in the appropriate value. ([GitHub][1])

---

# 4. Why is Fiber called a "unit of work"?

Consider:

```text
App
├── Header
├── Main
│   ├── Products
│   └── Cart
└── Footer
```

Conceptually React can process:

```text
App
 ↓
Header
 ↓
Main
 ↓
Products
 ↓
Cart
 ↓
Footer
```

as separate Fiber work units.

Instead of thinking:

```text
"render the entire application"
```

think:

```text
"process this Fiber"
"process its children"
"complete this Fiber"
"move to the next Fiber"
```

That's what allows React to reason about progress through a tree.

---

# 5. The traditional recursive approach

Before understanding Fiber, imagine implementing a tree renderer using ordinary recursion:

```javascript
function render(node) {
    process(node);

    for (const child of node.children) {
        render(child);
    }

    finish(node);
}
```

The call stack might look like:

```text
render(App)
  render(Main)
    render(ProductList)
      render(Product)
        ...
```

The problem is that once you've entered a deep synchronous call stack, JavaScript doesn't naturally give you a convenient point to stop and resume the algorithm from the exact location later.

Fiber changes the representation so React can explicitly keep track of:

```text
where am I?
what is next?
who is my parent?
who is my sibling?
```

instead of relying entirely on the JavaScript call stack.

---

# 6. Fiber converts the tree into linked nodes

This is one of the most important Fiber concepts.

Instead of representing:

```text
Parent
 ├── Child1
 ├── Child2
 └── Child3
```

only as nested arrays, Fiber uses linked relationships.

The important fields are:

```javascript
fiber.child
fiber.sibling
fiber.return
```

The current Fiber source initializes:

```text
return
child
sibling
index
```

as part of each Fiber node. ([GitHub][1])

---

# 7. `child`

Suppose:

```text
App
├── Header
├── Main
└── Footer
```

Then conceptually:

```text
App.child → Header
```

The first child is pointed to by:

```javascript
fiber.child
```

So:

```text
App
 │
 └── child → Header
```

---

# 8. `sibling`

How do we reach `Main` and `Footer`?

Through siblings.

Conceptually:

```text
App
 │
 └── child
      ↓
    Header
      │
      └── sibling → Main
                      │
                      └── sibling → Footer
```

So the structure is roughly:

```text
App
 ↓
Header → Main → Footer
```

This is essentially a linked-list representation of siblings.

---

# 9. `return`

How does a child find its parent?

Through:

```javascript
fiber.return
```

Conceptually:

```text
Header.return → App
Main.return   → App
Footer.return → App
```

So each Fiber can navigate:

```text
child
sibling
parent
```

through:

```text
child
sibling
return
```

This is sometimes described as a **child-sibling-parent** representation.

---

# 10. Visualizing the Fiber links

Take:

```text
App
├── Header
└── Main
    ├── Sidebar
    └── Content
```

Conceptually:

```text
                 App
                  │
                child
                  ▼
               Header
                  │
               sibling
                  ▼
                Main
                │
              child
                ▼
             Sidebar
                │
             sibling
                ▼
             Content
```

And:

```text
Header.return  → App
Main.return    → App
Sidebar.return → Main
Content.return → Main
```

This lets React walk the entire tree without requiring nested recursive JavaScript calls to represent the traversal state.

---

# 11. Why not just use an array?

You might ask:

> Why not do this?

```javascript
{
    children: [
        header,
        main,
        footer
    ]
}
```

You could.

But Fiber's linked representation is useful because React's work loop needs to frequently answer:

```text
What child should I process?
What sibling comes next?
If there is no sibling, which parent should I return to?
```

The linked structure gives React those relationships directly.

More importantly, the work loop can maintain its own explicit traversal state instead of depending entirely on the JavaScript call stack.

---

# 12. The Fiber traversal pattern

This gives us one of the most important interview concepts:

```text
begin
 ↓
child
 ↓
child
 ↓
...
complete
 ↓
sibling
 ↓
complete
 ↓
return to parent
```

A simplified traversal:

```text
App
 ↓ begin
Header
 ↓ begin
Header
 ↓ complete
Main
 ↓ begin
Sidebar
 ↓ complete
Content
 ↓ complete
Main
 ↓ complete
App
```

The current reconciler source is organized around this kind of "begin work" / "complete work" model, with `ReactFiberBeginWork.js` responsible for beginning/reconciling work and `ReactFiberCompleteWork.js` handling completion of Fiber work. ([GitHub][2])

---

# 13. `beginWork`

When React starts processing a Fiber, conceptually:

```text
beginWork(fiber)
```

asks:

> What should happen for this Fiber?

For a function component, this can involve:

```text
process props
process state/hooks
invoke component
get returned children
reconcile children
```

For other Fiber types, it can perform different work.

The current `beginWork` implementation contains bailout logic and invokes component-specific update paths; for example, its function-component path ultimately calls `updateFunctionComponent`. ([GitHub][2])

---

# 14. Example: function component

Suppose:

```jsx
function App() {
    return <Header />;
}
```

During the beginning of `App` work, conceptually:

```text
App Fiber
   ↓
run component
   ↓
App()
   ↓
returns <Header />
   ↓
reconcile children
   ↓
Header Fiber
```

So the output of a component becomes the input to child reconciliation.

---

# 15. `completeWork`

After React has finished processing the children of a Fiber, it needs to complete the Fiber.

Conceptually:

```text
beginWork(App)
      ↓
beginWork(Header)
      ↓
completeWork(Header)
      ↓
completeWork(App)
```

For host components, completion can involve preparing/creating host instances and other host-specific work.

The reconciler documentation says host instance creation, such as a DOM renderer calling `document.createElement`, occurs in the render phase, while actual placement into the tree is commit-related. ([GitHub][3])

---

# 16. Begin vs complete

Think of them like this:

### `beginWork`

```text
"What does this node need to render?"
```

### `completeWork`

```text
"I've finished its subtree; what needs to be finalized?"
```

Together:

```text
begin
 ↓
process children
 ↓
complete
```

This distinction is fundamental to Fiber traversal.

---

# 17. Let's walk through a small tree

Suppose:

```jsx
function App() {
    return (
        <div>
            <h1>Hello</h1>
            <button>Click</button>
        </div>
    );
}
```

Conceptual Fiber tree:

```text
App
└── div
    ├── h1
    └── button
```

Traversal:

```text
begin(App)
   ↓
begin(div)
   ↓
begin(h1)
   ↓
complete(h1)
   ↓
begin(button)
   ↓
complete(button)
   ↓
complete(div)
   ↓
complete(App)
```

This is a depth-first traversal.

---

# 18. Why does `sibling` matter?

Suppose React finishes:

```text
h1
```

There is no child left to process.

It asks:

> Does this Fiber have a sibling?

Yes:

```text
button
```

So:

```text
complete(h1)
   ↓
sibling
   ↓
button
```

After `button` is completed, there are no more siblings.

React then moves back to:

```text
div
```

through:

```text
return
```

This produces the traversal:

```text
h1 → button → div
```

without recursively unwinding a huge JavaScript call stack.

---

# 19. This is why `return` has a weird name

If you're coming from ordinary application code, you might think:

```javascript
return
```

means:

> return a value.

But on a Fiber, the field:

```javascript
fiber.return
```

means roughly:

> **the parent Fiber to return to after finishing this Fiber's work.**

This is a common interview detail.

So:

```text
child → first child
sibling → next sibling
return → parent
```

---

# 20. Now the big idea: explicit traversal state

A recursive tree traversal depends on the JavaScript call stack:

```text
render(A)
 └─ render(B)
     └─ render(C)
         └─ render(D)
```

Fiber stores the traversal relationships in the Fiber objects themselves.

Conceptually:

```text
Fiber D
  ↑
return
  │
Fiber C
  │
return
...
```

So React can maintain:

```text
"Where am I?"
```

outside the normal call stack.

This is what makes the work loop architecture possible.

---

# 21. But Fiber isn't "the scheduler"

Another common interview mistake.

Don't say:

> "Fiber is React's scheduler."

Better:

> Fiber is the internal data structure and architecture used to represent React work and the tree, enabling React to perform scheduling, prioritization, incremental work, reconciliation, and related rendering behavior.

The scheduler and reconciler are related but distinct pieces of the architecture.

We will later study **Lanes and scheduling** separately.

---

# 22. A Fiber contains much more than tree links

Let's inspect the current `FiberNode` structure.

The current source initializes fields including:

```text
tag
key
elementType
type
stateNode

return
child
sibling
index

ref
pendingProps
memoizedProps

updateQueue
memoizedState
dependencies

flags
subtreeFlags
deletions

lanes
childLanes

alternate
```

These fields are directly visible in the current source. ([GitHub][1])

Let's understand the important groups.

---

# 23. `tag`

`tag` tells React what kind of Fiber this is.

Conceptually:

```text
FunctionComponent
ClassComponent
HostRoot
HostComponent
HostText
Fragment
Suspense
MemoComponent
...
```

Think:

```text
tag = category of Fiber
```

It lets React choose appropriate processing logic.

---

# 24. `type`

`type` tells React the actual type associated with the Fiber.

For example:

```jsx
function User() {}
```

might conceptually produce:

```text
type = User
```

For a DOM element:

```jsx
<div />
```

the type can correspond to:

```text
type = "div"
```

The constructor explicitly stores both `elementType` and `type`. ([GitHub][1])

---

# 25. `elementType` vs `type`

This is a more advanced distinction.

You don't need to memorize the implementation details yet.

The useful mental model is:

```text
elementType
    ↓
the type associated with the original React element

type
    ↓
the resolved/current type React actually uses
```

This becomes relevant with things like:

```text
lazy
memo
hot reloading
wrappers
```

The exact resolution behavior is an implementation detail and can evolve, so in an interview don't overstate a simplified definition as an invariant.

But know that React intentionally stores both.

---

# 26. `key`

We've already studied this.

```text
key
```

helps establish child identity during reconciliation.

Example:

```jsx
<Item key={item.id} />
```

Fiber stores that key.

So Fiber combines:

```text
identity
+
state
+
tree position
+
pending work
```

That is why Fiber is much more than a "Virtual DOM node."

---

# 27. `stateNode`

This one is especially interesting.

For a host Fiber:

```jsx
<div />
```

React eventually needs an actual host instance.

Conceptually:

```text
Fiber(div)
   │
   └── stateNode → DOM <div>
```

For a class component, `stateNode` can relate to its class instance.

For some other Fiber types, its meaning differs.

The important idea:

> `stateNode` connects an internal Fiber to associated host/runtime state where applicable.

The field is part of the Fiber's actual current implementation. ([GitHub][1])

---

# 28. `pendingProps`

These are the props associated with the work currently being processed.

Think:

```text
pendingProps
=
props being considered for this render
```

For example:

```jsx
<User name="Alice" />
```

might have conceptually:

```text
pendingProps = {
    name: "Alice"
}
```

---

# 29. `memoizedProps`

This represents props from the last completed/rendered state of that Fiber.

Conceptually:

```text
pendingProps
      ↓
currently being processed

memoizedProps
      ↓
previously processed/completed
```

That distinction gives React information for deciding whether work can be reused/bypassed.

The Fiber constructor stores both fields. ([GitHub][1])

---

# 30. `memoizedState`

This is one of the most important fields.

For function components, Hooks are associated with the Fiber's state structures.

Conceptually:

```text
Fiber
 │
 └── memoizedState
        ↓
      Hooks
        ↓
state/effects/etc.
```

So when we later study:

```jsx
useState(...)
useEffect(...)
useMemo(...)
```

we'll come back to:

```text
memoizedState
```

This is one reason a normal function-local variable is not where persistent React state lives.

The Fiber constructor initializes `memoizedState` to `null`. ([GitHub][1])

---

# 31. `updateQueue`

Fibers can have pending update information.

Conceptually:

```text
Fiber
 │
 └── updateQueue
      ├── update
      ├── update
      └── update
```

For stateful components, this is central to processing state updates.

For class components, update queues have a particular structure; function components use Hook queues and related internals.

The important interview takeaway:

> **State updates don't simply overwrite a JavaScript variable; React maintains update information in internal structures and processes it during rendering.**

The Fiber itself contains an `updateQueue` field. ([GitHub][1])

---

# 32. `flags`

These describe work that needs to happen.

We've already seen concepts such as:

```text
Placement
Update
ChildDeletion
```

Think:

```text
Fiber
 │
 └── flags
       ↓
"what commit-related work is required?"
```

The current Fiber constructor initializes `flags` and `subtreeFlags` to `NoFlags`. ([GitHub][1])

---

# 33. `subtreeFlags`

Suppose:

```text
App
└── Main
    └── Button
```

and only Button needs an update.

React can propagate information indicating that relevant work exists within a subtree.

Conceptually:

```text
App
 └── subtreeFlags
       ↓
     "some descendant has work"
```

This helps commit traversal avoid blindly examining every possible node for every operation.

The exact optimizations evolve, but the current Fiber data structure explicitly contains both `flags` and `subtreeFlags`. ([GitHub][1])

---

# 34. `deletions`

If reconciliation determines:

```text
old child B
```

has no corresponding new child, deletion work needs to be recorded.

The Fiber structure has:

```text
deletions
```

and the child reconciler uses deletion bookkeeping. This is part of how render-time reconciliation communicates removal work to commit-time processing. ([GitHub][1])

---

# 35. `lanes`

Now we're reaching scheduling.

Fiber contains:

```text
lanes
```

Think:

> **What priority categories of work are pending on this Fiber?**

Don't worry about the exact bit patterns yet.

We'll have an entire topic on Lanes.

For now:

```text
Fiber
 ├── lanes
 └── childLanes
```

allows React to reason about pending work and its priorities across the tree.

The current Fiber constructor initializes both to `NoLanes`. ([GitHub][1])

---

# 36. `childLanes`

`childLanes` means work may be pending somewhere **below** this Fiber.

Conceptually:

```text
App
 └── childLanes
       ↓
   "there is pending work
    somewhere in my subtree"
```

This becomes extremely useful when React decides whether a subtree can be skipped/bypassed for the current render.

You'll see this again when we study bailouts and scheduling.

---

# 37. The most important field: `alternate`

This is one of the biggest React interview questions.

A Fiber has:

```javascript
fiber.alternate
```

Conceptually:

```text
Current Fiber
      ↕
Alternate Fiber
```

The alternate is the corresponding Fiber in the other tree/version used by React's double-buffering system.

The current source explicitly initializes `alternate` to `null`, and `createWorkInProgress` uses the alternate to obtain/reuse the other Fiber. ([GitHub][1])

---

# 38. Current tree vs work-in-progress tree

Imagine the UI is currently:

```text
Current tree

App
└── Counter
    └── h1 "0"
```

The user clicks.

React wants to calculate:

```text
Next tree

App
└── Counter
    └── h1 "1"
```

React conceptually has:

```text
Current tree
     │
     │ committed
     ▼
 browser-visible UI
```

while constructing:

```text
Work-in-progress tree
     │
     │ being rendered
     ▼
 next UI
```

---

# 39. Why not modify the current tree directly?

Because the current tree represents the **currently committed UI**.

During render, React may need to:

```text
calculate
pause
restart
abandon
retry
continue
```

If it mutated the committed tree directly and then abandoned the render, React could lose its clean representation of what is currently on screen.

Instead:

```text
Current
  ↓
keep it intact

Work-in-progress
  ↓
build next version
```

This provides a clean separation.

---

# 40. Double buffering

The current React source comment says `createWorkInProgress` uses a **double buffering pooling technique** because React expects at most two versions of a tree: the current one and the work-in-progress one. ([GitHub][1])

Conceptually:

```text
             ┌─────────────┐
             │   Current   │
             │    Tree     │
             └──────┬──────┘
                    ↕
              alternate
                    ↕
             ┌──────┴──────┐
             │     WIP     │
             │    Tree     │
             └─────────────┘
```

After a successful commit, their roles switch conceptually.

---

# 41. "Roles switch" rather than "copy entire tree"

This is important.

You shouldn't imagine:

```text
copy entire tree
     ↓
throw old tree away
```

on every render.

React reuses the alternate structure where possible.

The current `createWorkInProgress` logic first looks for:

```javascript
current.alternate
```

and reuses it when available; otherwise it creates one. ([GitHub][1])

This is one reason the Fiber system is efficient.

---

# 42. Simplified `createWorkInProgress`

Conceptually, we can imagine:

```javascript
function createWorkInProgress(current, pendingProps) {
    let wip = current.alternate;

    if (wip === null) {
        wip = createFiber(...);
        wip.alternate = current;
        current.alternate = wip;
    }

    wip.pendingProps = pendingProps;

    return wip;
}
```

This is deliberately simplified.

The current implementation copies/retains many fields, resets specific ones, and handles additional modes and development behavior. ([GitHub][1])

But the idea is correct:

```text
current ↔ alternate
```

---

# 43. Why is this useful?

Suppose:

```text
Current:
count = 0
```

React can have:

```text
Current Fiber:
memoizedState → 0

WIP Fiber:
processing update → 1
```

During rendering:

```text
Current remains stable
WIP changes
```

If rendering succeeds:

```text
WIP becomes committed
```

If rendering is interrupted/abandoned:

```text
Current remains the known-good UI
```

That's a huge architectural advantage.

---

# 44. Fiber is not a persistent immutable tree in the simple sense

You may hear:

> "React uses immutable trees."

Be careful.

React's implementation uses mutable Fiber objects as its working data structures.

The important guarantee is not:

```text
"every Fiber object is immutable"
```

but rather:

```text
"React maintains separate current and work-in-progress representations
so committed state remains distinct from in-progress work."
```

The current implementation explicitly mutates work-in-progress fields and clones selected data from current. ([GitHub][1])

---

# 45. Why Fiber makes interruption possible

Now connect everything.

Without Fiber:

```text
recursive rendering
   ↓
deep synchronous call stack
```

With Fiber:

```text
Fiber A
   ↓
Fiber B
   ↓
Fiber C
```

and React can maintain:

```text
next unit of work
```

outside the normal recursive stack.

Conceptually:

```text
process Fiber A
   ↓
process Fiber B
   ↓
yield/pause
   ↓
browser gets control
   ↓
resume Fiber C
```

This is the fundamental architectural ability that Fiber enabled.

---

# 46. But don't say "Fiber automatically makes rendering asynchronous"

This is a common interview mistake.

Fiber itself provides the architecture for organizing work.

It does **not** mean:

```text
every render = asynchronous
```

or:

```text
every update = interruptible
```

React can perform work synchronously when appropriate.

What Fiber gives React is the ability to represent and manage rendering work in a more flexible way.

Then scheduling/lanes/concurrent features decide how that capability is used.

---

# 47. Fiber and reconciliation are related but different

Think:

```text
Fiber
=
data structure + work architecture
```

while:

```text
Reconciliation
=
algorithm for matching old and new children/trees
```

Together:

```text
new React elements
        +
current Fibers
        ↓
reconciliation
        ↓
work-in-progress Fibers
```

So:

```text
Fiber ≠ reconciliation
```

but the reconciler operates on Fibers.

---

# 48. Fiber and Virtual DOM are also different

This is worth reinforcing.

### JSX

```text
syntax
```

### React element

```text
description of requested UI
```

### Fiber

```text
React internal node/work structure
```

### DOM

```text
browser's actual UI tree
```

So:

```text
JSX
 ↓
React element
 ↓
Fiber
 ↓
DOM
```

is the useful conceptual pipeline.

---

# 49. A Fiber tree isn't necessarily identical to the DOM tree

Very important.

Suppose:

```jsx
<App />
```

returns:

```jsx
<>
    <Header />
    <Main />
</>
```

The Fiber tree can contain:

```text
App Fiber
 └── Fragment Fiber
      ├── Header Fiber
      └── Main Fiber
```

while the DOM doesn't have to contain a corresponding Fragment element.

Similarly, React can have Fibers for things that don't correspond one-to-one to DOM nodes.

Therefore:

```text
Fiber tree ≠ DOM tree
```

---

# 50. A complete example

Let's use:

```jsx
function App() {
    return (
        <main>
            <Counter />
            <Footer />
        </main>
    );
}

function Counter() {
    const [count, setCount] = useState(0);

    return (
        <button onClick={() => setCount(count + 1)}>
            {count}
        </button>
    );
}

function Footer() {
    return <footer>Footer</footer>;
}
```

Conceptual Fiber structure:

```text
App
└── main
    ├── Counter
    │   └── button
    └── Footer
        └── footer
```

Links:

```text
App.child → main

main.child → Counter

Counter.sibling → Footer

Counter.child → button

Footer.child → footer
```

And:

```text
main.return → App
Counter.return → main
Footer.return → main
button.return → Counter
footer.return → Footer
```

---

# 51. Initial render traversal

Conceptually:

```text
begin(App)
  ↓
begin(main)
  ↓
begin(Counter)
  ↓
begin(button)
  ↓
complete(button)
  ↓
complete(Counter)
  ↓
begin(Footer)
  ↓
begin(footer)
  ↓
complete(footer)
  ↓
complete(Footer)
  ↓
complete(main)
  ↓
complete(App)
```

At the end:

```text
completed WIP tree
```

can be committed.

---

# 52. Now click the counter

State update:

```text
setCount(1)
```

React schedules work.

During rendering:

```text
Counter
 ↓
useState processes update
 ↓
count = 1
 ↓
Counter returns new button output
```

Now:

```text
Old:
button → "0"

New:
button → "1"
```

Reconciliation can reuse the existing Fiber because the relevant identity still matches.

The Fiber gets work describing the required host update.

Eventually:

```text
commit
 ↓
DOM button text becomes "1"
```

---

# 53. Notice what was preserved

The following could remain logically the same:

```text
App Fiber
main Fiber
Counter Fiber
button Fiber
```

while their work-in-progress versions are updated.

The important identity relationship is maintained.

That's how React can preserve:

```text
component identity
state
DOM node identity
```

across ordinary updates.

---

# 54. Why `alternate` matters for state preservation

Suppose current:

```text
Counter Fiber
memoizedState → hook state = 0
```

WIP:

```text
Counter WIP Fiber
memoizedState → hook state being processed
```

React can render against the current state and build the new state representation.

This is the bridge between:

```text
Fiber
```

and later:

```text
Hooks
```

When we study Hooks, you'll see why React needs a stable Fiber identity to associate Hook state with a component.

---

# 55. Bailouts

Now we get another important Fiber concept.

Suppose React encounters a subtree where nothing relevant needs to change.

Instead of doing all the work again, it can sometimes **bail out**.

Conceptually:

```text
Fiber
 ↓
no relevant work?
 ↓
reuse existing subtree
```

The current `beginWork` source contains explicit bailout logic such as `bailoutOnAlreadyFinishedWork`, including cases where React determines there is no scheduled update/context relevant to the current render. ([GitHub][2])

This is one reason:

```text
Fiber + lanes + memoization
```

are so important for performance.

---

# 56. Bailout does not mean "DOM skipped forever"

A bailout means:

> **React can skip rendering work that isn't relevant to the current render.**

It doesn't mean:

```text
"ignore this subtree permanently."
```

If future work targets that subtree, React can process it again.

---

# 57. `childLanes` helps subtree decisions

Suppose:

```text
App
└── Main
    └── Counter
```

The update occurs in:

```text
Counter
```

React may need to know:

> Does `Main` have relevant work somewhere underneath it?

That's one of the jobs of:

```text
childLanes
```

Conceptually:

```text
Main.childLanes
      ↓
"something below Main has pending work"
```

This lets React avoid treating the entire tree as equally urgent/equally active.

The current Fiber structure stores `childLanes`, and the current reconciliation/update code uses lane information when deciding whether to bail out or continue work. ([GitHub][1])

---

# 58. Why the Fiber structure is so powerful

One internal structure gives React a place to associate:

```text
identity
state
props
tree relationships
pending updates
priority
effects/flags
host instance
alternate
```

So instead of separate unrelated systems, React gets a central representation:

```text
                      Fiber
                        │
        ┌───────────────┼────────────────┐
        ▼               ▼                ▼
      Identity         State           Work
        │               │                │
      type/key      Hooks/updates     lanes/flags
        │                                │
        └───────────────┬────────────────┘
                        ▼
                   Tree links
              child/sibling/return
                        │
                        ▼
                   alternate
                        │
                        ▼
                current ↔ WIP
```

That's the real significance of Fiber.

---

# 59. Why linked structure helps interruption

Imagine React is at:

```text
Product #742
```

and pauses.

Because each Fiber contains:

```text
child
sibling
return
```

React can preserve enough information about where it is in the traversal.

Conceptually:

```text
current Fiber = Product #742
```

and later continue using the Fiber graph rather than restarting from the root merely because the JavaScript stack was gone.

That's one of the deepest reasons the Fiber representation differs from a straightforward recursive tree walker.

---

# 60. Important correction: Fiber doesn't mean one DOM node per Fiber

There isn't a strict one-to-one mapping.

For example:

```text
FunctionComponent Fiber
```

may represent an entire logical component but have no direct DOM node.

A:

```text
Fragment Fiber
```

doesn't represent a DOM element.

A:

```text
HostComponent Fiber
```

such as `div` can correspond to a DOM host instance.

So:

```text
Fiber nodes
```

and:

```text
DOM nodes
```

have different cardinalities and purposes.

---

# 61. Interview question: What is Fiber?

A strong answer:

> Fiber is React's internal data structure and rendering architecture for representing units of work in the React tree. A Fiber stores information such as component/element identity, props, state, update information, tree relationships, scheduling lanes, effect flags, and an alternate. This representation lets React process rendering work incrementally, preserve current committed state separately from work-in-progress state, and support modern scheduling and concurrent rendering behavior.

That is an excellent senior-level answer.

---

# 62. Interview question: Why did React introduce Fiber?

Strong answer:

> The previous rendering architecture was more synchronous and relied heavily on recursive traversal. Fiber introduced an explicit representation of rendering work so React could break work into units, track progress through the tree, prioritize work, pause or restart rendering, and maintain current and work-in-progress trees separately.

This captures the architectural motivation.

---

# 63. Interview question: What are `child`, `sibling`, and `return`?

Perfect concise answer:

```text
child   → first child
sibling → next sibling
return  → parent
```

Together they allow React to represent and traverse the tree using linked relationships.

The current Fiber constructor directly defines these fields. ([GitHub][1])

---

# 64. Interview question: What is `alternate`?

> `alternate` points to the corresponding Fiber in the other version of the tree—typically the current Fiber and its work-in-progress counterpart. React uses this double-buffering model to build the next tree without directly corrupting the currently committed tree. The current `createWorkInProgress` implementation explicitly reuses the alternate when available. ([GitHub][1])

---

# 65. Interview question: What is double buffering in React?

> React maintains two logical versions of the Fiber tree: the current committed tree and the work-in-progress tree being prepared. The `alternate` field connects corresponding Fibers. Once the work-in-progress tree is successfully committed, it becomes the current tree. React's current source explicitly describes this as a double-buffering technique. ([GitHub][1])

---

# 66. Interview question: What is `memoizedState`?

Don't answer simply:

> "It's the state."

Better:

> `memoizedState` is a Fiber field containing the state information associated with the Fiber's previously processed state. For function components, this participates in React's internal representation of Hook state and related information.

We'll see the Hook structure itself in detail later.

---

# 67. Interview question: What are lanes?

For now:

> Lanes are React's internal priority representation for pending work. Fibers track their own lanes and descendant `childLanes`, allowing the renderer to determine which work is relevant to a particular render.

We'll spend an entire topic on this because the real answer is deeper.

---

# 68. Interview question: What is the relationship between Fiber and reconciliation?

Strong answer:

```text
React elements
      ↓
reconciliation
      ↓
Fiber tree/work-in-progress Fibers
      ↓
flags/lanes
      ↓
commit
```

So:

> Reconciliation is the process/algorithm that determines how the new element structure corresponds to existing work, while Fiber is the internal data structure used to represent and process that work.

---

# 69. Interview question: Does Fiber replace the Virtual DOM?

Careful answer:

> It's better not to frame them as direct replacements. "Virtual DOM" is a broad conceptual term for React's in-memory UI representation, while Fiber is React's internal architecture and data structure for representing tree nodes and rendering work. Modern React uses Fiber internally to reconcile and schedule updates.

---

# 70. The entire React architecture so far

We now have enough pieces to construct a much more accurate picture:

```text
                         JSX
                          ↓
                  React element
                          ↓
             ┌───────────────────────┐
             │       Fiber           │
             │                       │
             │ type / key            │
             │ props                 │
             │ state / hooks         │
             │ child/sibling/return  │
             │ lanes                 │
             │ flags                 │
             │ alternate             │
             └───────────┬───────────┘
                         ↓
                    beginWork
                         ↓
                 reconcile children
                         ↓
                   child Fibers
                         ↓
                  completeWork
                         ↓
                finished WIP tree
                         ↓
                      commit
                         ↓
                        DOM
```

And beside it:

```text
Current Fiber tree
       ↕
   alternate
       ↕
Work-in-progress tree
```

That is the foundation of modern React rendering.

---

# 71. A simplified Fiber implementation

To make this concrete, imagine we were designing React ourselves:

```javascript
class Fiber {
    constructor(tag, type, key, props) {
        this.tag = tag;
        this.type = type;
        this.key = key;

        this.pendingProps = props;
        this.memoizedProps = null;
        this.memoizedState = null;
        this.updateQueue = null;

        this.return = null;
        this.child = null;
        this.sibling = null;

        this.flags = 0;
        this.lanes = 0;
        this.childLanes = 0;

        this.stateNode = null;

        this.alternate = null;
    }
}
```

Then:

```javascript
const current = new Fiber(
    "FunctionComponent",
    Counter,
    null,
    {}
);

const workInProgress = createWorkInProgress(
    current,
    {}
);
```

and:

```text
current
   ↕
WIP
```

This simplified implementation captures the central architecture of the real Fiber structure.

The actual React implementation has significantly more fields and version-specific logic, which you can see directly in the current `FiberNode` constructor. ([GitHub][1])

---

# 72. Simplified Fiber work loop

You can also imagine React's traversal conceptually as:

```javascript
function performUnitOfWork(fiber) {
    const next = beginWork(fiber);

    if (next !== null) {
        return next;
    }

    return completeUnitOfWork(fiber);
}
```

Then conceptually:

```javascript
function completeUnitOfWork(fiber) {
    while (true) {
        completeWork(fiber);

        if (fiber.sibling) {
            return fiber.sibling;
        }

        if (fiber.return) {
            fiber = fiber.return;
            continue;
        }

        return null;
    }
}
```

This is **teaching pseudocode**, not a copy of the React source.

It demonstrates why these three links matter:

```text
child
sibling
return
```

---

# 73. How this relates to interruption

Now suppose:

```text
work = App → Main → ProductList → Product
```

and React pauses after:

```text
Product #50
```

Because the work is represented explicitly:

```text
current unit = Product #50
```

React doesn't conceptually need the original recursive JavaScript call stack to remember:

```text
where it was
```

The Fiber tree encodes the relationships necessary to continue processing.

This is the heart of incremental rendering.

---

# 74. But commit remains separate

Even though Fiber allows rendering to be incremental, React still needs a controlled commit.

Conceptually:

```text
WIP tree
   ↓
render/reconcile
   ↓
completed tree
   ↓
commit
```

The browser-visible UI remains based on the last committed tree until React commits the new result.

This gives us an important invariant:

```text
Current committed UI
      ≠
in-progress calculation
```

until the commit succeeds.

---

# 75. The most important mental model

When an interviewer says **Fiber**, don't think:

```text
"some replacement for Virtual DOM"
```

Think:

```text
Fiber
 =
React's internal work node
       +
tree links
       +
component identity
       +
state
       +
updates
       +
priority
       +
effect information
       +
current/WIP relationship
```

And:

```text
current Fiber
      ↕ alternate
WIP Fiber
```

This single picture explains a huge portion of React internals.

---

# 76. What we've learned

At this point, our React internals map is:

```text
Topic 1
React fundamentals
       ↓
Topic 2
JSX / React elements
       ↓
Topic 3
Components / props / state
       ↓
Topic 4
Render / commit
       ↓
Topic 5
Reconciliation
       ↓
Topic 6
Fiber
```

We now understand:

```text
✅ Fiber node
✅ child
✅ sibling
✅ return
✅ type
✅ key
✅ pendingProps
✅ memoizedProps
✅ memoizedState
✅ updateQueue
✅ flags
✅ lanes
✅ childLanes
✅ alternate
✅ current tree
✅ work-in-progress tree
✅ double buffering
✅ beginWork
✅ completeWork
✅ incremental rendering
✅ bailout concept
```

---

# 77. The next crucial topic

Now that you understand **what Fiber is**, we need to understand **how React actually walks the Fiber tree**.

## Topic 7 — Fiber Work Loop: `beginWork` → `completeWork` → Commit

We'll take:

```jsx
<App />
```

and literally walk through the Fiber tree:

```text
            App
             │
           child
             ↓
            div
          ┌───┴───┐
        child   sibling
         ↓         ↓
        h1       button
```

and trace:

```text
begin App
 ↓
begin div
 ↓
begin h1
 ↓
complete h1
 ↓
begin button
 ↓
complete button
 ↓
complete div
 ↓
complete App
```

Then we'll examine **what `beginWork` actually does, what `completeWork` actually does, when React yields, where flags are collected, how completed Fibers bubble information to parents, and how the finished WIP tree reaches the commit phase**.

That will make the React rendering pipeline much easier to visualize.

[1]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiber.js "react/packages/react-reconciler/src/ReactFiber.js at main · react/react · GitHub"
[2]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberBeginWork.js "react/packages/react-reconciler/src/ReactFiberBeginWork.js at main · react/react · GitHub"
[3]: https://github.com/react/react/blob/main/packages/react-reconciler/README.md?utm_source=chatgpt.com "react/packages/react-reconciler/README.md at main · react/react · GitHub"

