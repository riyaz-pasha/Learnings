# Topic 7 — The Fiber Work Loop: `beginWork` → `completeWork` → Commit

Now we are going one level deeper.

We know:

```text
React Element
   ↓
Fiber
   ↓
Reconciliation
```

But **how does React actually walk the Fiber tree?**

The central mechanism is a work loop that repeatedly processes a Fiber, usually by:

```text
beginWork
    ↓
process children
    ↓
completeWork
    ↓
move to sibling / parent
```

The current React source contains this work-loop machinery in `ReactFiberWorkLoop.js`, with `beginWork` and `completeWork` implemented in their corresponding modules. ([GitHub][1])

This topic is particularly valuable for interviews because it lets you explain **exactly how React traverses a tree without simply relying on recursive JavaScript calls**.

---

# 1. The tree we'll use

Let's use this application:

```jsx
function App() {
    return (
        <div>
            <Header />
            <Main />
        </div>
    );
}

function Header() {
    return <h1>Hello</h1>;
}

function Main() {
    return (
        <main>
            <button>Save</button>
            <p>Welcome</p>
        </main>
    );
}
```

Conceptual tree:

```text
App
└── div
    ├── Header
    │   └── h1
    └── Main
        └── main
            ├── button
            └── p
```

The corresponding Fiber relationships are conceptually:

```text
App
 │
 └─ child → div
              │
              ├─ child → Header
              │            │
              │            └─ child → h1
              │
              └─ sibling → Main
                           │
                           └─ child → main
                                      │
                                      ├─ child → button
                                      └─ sibling → p
```

Remember:

```text
child   → first child
sibling → next sibling
return  → parent
```

---

# 2. What is the work loop?

At a high level, React repeatedly asks:

> "What's the next Fiber I should work on?"

Conceptually:

```javascript
while (workInProgress !== null) {
    performUnitOfWork(workInProgress);
}
```

The exact current implementation has additional complexity for synchronous work, concurrent work, errors, suspended work, profiling, and other cases, but this is the essential idea. The current React source contains separate scheduler-aware loops and `performUnitOfWork` machinery. ([Fossies][2])

Think of:

```text
workInProgress
```

as:

> **the Fiber currently being processed.**

---

# 3. `performUnitOfWork`

Conceptually:

```javascript
function performUnitOfWork(unitOfWork) {
    const current = unitOfWork.alternate;

    const next = beginWork(
        current,
        unitOfWork,
        renderLanes
    );

    if (next !== null) {
        workInProgress = next;
    } else {
        completeUnitOfWork(unitOfWork);
    }
}
```

Again, this is teaching pseudocode rather than a verbatim copy of today's source.

The important behavior is:

```text
beginWork()
    ↓
returns next child?
    │
    ├── yes → process that child
    │
    └── no  → complete current Fiber
```

The current work-loop source follows this same conceptual pattern. ([Fossies][2])

---

# 4. `beginWork` — the downward phase

Think:

> **`beginWork` asks what this Fiber needs to produce.**

For different Fiber types, this means different things.

For a function component:

```text
beginWork
   ↓
run component
   ↓
process Hooks/state
   ↓
obtain returned React elements
   ↓
reconcile children
```

For a host element like:

```jsx
<div />
```

it processes the host component's children and associated work.

The current `ReactFiberBeginWork.js` contains the component-specific update functions and child reconciliation paths. ([GitHub][3])

---

# 5. `beginWork` doesn't necessarily finish the Fiber

This is subtle.

Suppose:

```text
App
└── div
    ├── Header
    └── Main
```

When `beginWork(App)` runs, App may return a child:

```text
div
```

So conceptually:

```javascript
const next = beginWork(App);

next === App.child;
```

which is:

```text
div
```

React then makes:

```text
workInProgress = div
```

and continues.

So:

```text
beginWork
```

is often a way of saying:

> "I've processed this Fiber enough to know what child work comes next."

---

# 6. The downward journey

Starting at the root:

```text
App
```

React follows children:

```text
App
 ↓
div
 ↓
Header
 ↓
h1
```

This is depth-first traversal.

Visually:

```text
App
 ↓
div
 ↓
Header
 ↓
h1
```

At `h1`, there is no child.

That's where something changes.

---

# 7. What happens when `beginWork` returns `null`?

Suppose:

```text
h1
```

has no child.

Then:

```text
beginWork(h1)
    ↓
null
```

There is no next child to process.

So React must now:

```text
complete(h1)
```

That is why the work loop transitions from:

```text
begin phase
```

to:

```text
complete phase
```

The current work-loop architecture uses this exact conceptual transition: when `beginWork` yields no next Fiber, React enters the completion path. ([Fossies][2])

---

# 8. `completeWork` — the upward phase

Think:

> **`completeWork` runs after a Fiber's child work has been completed.**

For example:

```text
begin(h1)
complete(h1)
```

Then:

```text
begin(Header)
```

has no more child after its child is done, so:

```text
complete(Header)
```

Then eventually:

```text
complete(div)
complete(App)
```

The current `ReactFiberCompleteWork.js` contains the Fiber-type-specific completion logic. ([GitHub][4])

---

# 9. Why do we need `completeWork`?

Because once all children have been processed, React now has information about the subtree.

For a host component, completion can involve things such as:

```text
creating/preparing host instances
finalizing host properties
appending completed children
bubbling subtree information
```

depending on whether it is a mount, update, hydration, and other conditions.

For function components, completion is less about creating a DOM node and more about finishing the Fiber and bubbling relevant information upward.

The current `completeWork` implementation handles these cases by Fiber tag/type. ([GitHub][4])

---

# 10. The complete traversal

Let's do the whole tree manually.

Tree:

```text
App
└── div
    ├── Header
    │   └── h1
    └── Main
        └── main
            ├── button
            └── p
```

Traversal:

```text
begin(App)
    ↓
begin(div)
    ↓
begin(Header)
    ↓
begin(h1)
    ↓
complete(h1)
    ↓
complete(Header)
    ↓
begin(Main)
    ↓
begin(main)
    ↓
begin(button)
    ↓
complete(button)
    ↓
begin(p)
    ↓
complete(p)
    ↓
complete(main)
    ↓
complete(Main)
    ↓
complete(div)
    ↓
complete(App)
```

This is the fundamental **depth-first Fiber work traversal**.

---

# 11. Why does `Header` complete before `Main` begins?

Because of the tree:

```text
div
├── Header
└── Main
```

React goes down the first branch first:

```text
div
 ↓
Header
 ↓
h1
```

Once `Header` has no more work:

```text
h1 complete
 ↓
Header complete
```

then React sees:

```text
Header.sibling → Main
```

and moves to Main.

That is why the `sibling` field matters.

---

# 12. `completeUnitOfWork`

This is one of the cleverest parts of Fiber.

Suppose we have:

```text
h1
```

and it has:

```text
no child
no sibling
```

React completes it.

Then asks:

```text
Does the Fiber have a sibling?
```

No.

Then:

```text
Does it have a parent?
```

Yes:

```text
Header
```

So React goes upward:

```text
h1
 ↑
return
 │
Header
```

Now it checks Header's sibling:

```text
Header.sibling → Main
```

Therefore the next work is:

```text
Main
```

Conceptually:

```text
complete(h1)
   ↓
up to Header
   ↓
find sibling
   ↓
Main
```

This is the "backtracking" part of Fiber traversal. The current work-loop source contains this completion/backtracking logic, while secondary technical descriptions show the same child/sibling/parent traversal pattern. ([DeepWiki][5])

---

# 13. The traversal algorithm in plain English

At every Fiber:

```text
1. Start/Begin this Fiber.
2. If it has a child, go to the child.
3. Otherwise complete the Fiber.
4. After completion, if it has a sibling, go to the sibling.
5. Otherwise go to the parent.
6. Continue until the root is completely finished.
```

That's essentially the algorithm.

---

# 14. Think "down first, then sideways, then up"

A nice interview mnemonic:

```text
             DOWN
              ↓
           child
              ↓
          complete
              ↓
         sideways
          sibling
              ↓
             UP
          return
```

So:

```text
child   = go down
sibling = go sideways
return  = go up
```

This is an excellent mental model for the Fiber tree.

---

# 15. Why not simply use recursion?

Suppose we wrote:

```javascript
function traverse(node) {
    begin(node);

    for (const child of node.children) {
        traverse(child);
    }

    complete(node);
}
```

This works for ordinary traversal.

But it relies heavily on the JavaScript call stack to remember:

```text
where we came from
what remains
which sibling comes next
```

Fiber explicitly stores relationships:

```text
child
sibling
return
```

so React can manage the traversal itself.

That becomes especially useful when rendering work is schedulable/interrupted.

---

# 16. Pausing work

Suppose React has processed:

```text
App
 ↓
div
 ↓
Header
 ↓
h1
```

and wants to yield.

It can preserve the current work state conceptually as:

```text
workInProgress = h1
```

Later it can continue.

This is one of the architectural advantages Fiber provides.

The current work loop has scheduler-aware concurrent paths that perform units of work until React's scheduling logic says to yield. The React 19.3 source-diff shows a loop that continues while `workInProgress !== null && !shouldYield()`, then calls `performUnitOfWork`. ([Fossies][2])

---

# 17. Very important: yielding happens during render, not arbitrary commit

The important distinction is:

```text
Render work
    ↓
can be scheduled/interrupted depending on update/render mode
```

versus:

```text
Commit
    ↓
must apply the completed result consistently
```

React's documentation maintains the conceptual separation:

```text
render
    ↓
commit
```

and the reconciler architecture keeps host mutations in the commit path. ([React][6])

We'll go deeper into scheduling later.

---

# 18. `beginWork` for function components

Let's look more closely.

Suppose:

```jsx
function User({ name }) {
    const [age, setAge] = useState(25);

    return (
        <div>
            <h1>{name}</h1>
            <p>{age}</p>
        </div>
    );
}
```

Conceptually:

```text
User Fiber
    ↓
beginWork(User)
```

React needs to:

```text
read current props
process pending state updates
run Hooks
invoke User()
```

Then:

```javascript
User()
```

returns:

```jsx
<div>
    <h1>{name}</h1>
    <p>{age}</p>
</div>
```

React reconciles those children and produces child Fibers.

So:

```text
User Fiber
   ↓
beginWork
   ↓
invoke function
   ↓
returned elements
   ↓
reconcile children
   ↓
child Fiber(s)
```

The current begin-work implementation contains the function-component update path. ([GitHub][3])

---

# 19. What about a host component?

Suppose:

```jsx
<div>
    <h1>Hello</h1>
</div>
```

At the `div` Fiber:

```text
beginWork(div)
```

React primarily needs to reconcile its children.

The `div` itself doesn't execute JavaScript like a function component.

So:

```text
FunctionComponent
   ↓
invoke function

HostComponent
   ↓
process/reconcile children
```

This distinction is important.

---

# 20. `completeWork` for host components

Now suppose React finishes:

```text
<h1>Hello</h1>
```

The host Fiber must be completed.

For an initial mount, completion can involve creating the actual host instance and assembling its children before the commit places it into the host tree.

The reconciler's host-config abstraction provides operations such as creating instances, while React DOM supplies the browser-specific implementation. The reconciler documentation explicitly describes host instance creation and the separation between render-time preparation and commit-time placement. ([ReadMex][7])

Conceptually:

```text
completeWork(h1)
    ↓
create <h1> instance
    ↓
set/prepare initial properties
```

Then the parent may incorporate that completed child.

---

# 21. Parent completion can assemble children

Suppose:

```text
div
├── h1
└── button
```

After:

```text
complete(h1)
complete(button)
```

React completes:

```text
complete(div)
```

At this point it knows that the child host nodes are ready.

Conceptually:

```text
h1 instance
button instance
     ↓
complete div
     ↓
assemble subtree
```

This is part of why completion happens bottom-up.

The current `completeWork` implementation contains host-specific completion logic including child append/instance handling. ([GitHub][4])

---

# 22. Important: assembling is not the same as inserting into the document

This is an excellent interview distinction.

During render/completion, React can prepare host instances.

But:

```text
prepare/build subtree
```

is not equivalent to:

```text
insert subtree into live DOM
```

The latter belongs to commit.

Conceptually:

```text
Render
 ├── calculate
 ├── reconcile
 └── prepare host instances
          ↓
Commit
 └── place/update/remove in host tree
```

React's reconciler documentation explicitly distinguishes creating host instances during render from placing them into the host environment during commit. ([ReadMex][7])

---

# 23. Mutation flags

Suppose during render React discovers:

```text
button text changed
```

It doesn't necessarily immediately execute the final DOM mutation.

Instead, the Fiber can contain information indicating that an update is required.

Conceptually:

```text
button Fiber
    ↓
flags = Update
```

Then later:

```text
commit
   ↓
inspect Update
   ↓
perform host mutation
```

This separation is fundamental.

---

# 24. Flags bubble upward

Now something subtle.

Suppose:

```text
App
└── div
    └── button
```

Only the button requires an update.

The parent `div` itself may not have a direct update.

But React still needs an efficient way to know:

> "There is work somewhere in this subtree."

Completion can bubble subtree information upward.

Conceptually:

```text
button
 flags = Update

      ↓ bubble

div
 subtreeFlags = Update-related work

      ↓ bubble

App
 subtreeFlags = Update-related work
```

The current Fiber structure contains both `flags` and `subtreeFlags`, and `completeWork` participates in bubbling properties upward. ([GitHub][4])

---

# 25. Why bubble flags?

Imagine:

```text
App
├── Header
├── Main
│   ├── ProductList
│   │   ├── Product
│   │   └── Product
│   └── Cart
└── Footer
```

Only one deep `Product` changed.

During commit, React benefits from knowing:

```text
Header subtree → nothing
Footer subtree → nothing
Main subtree → some work
ProductList subtree → some work
Product → Update
```

Then it doesn't need to blindly treat every subtree as needing work.

This supports efficient commit traversal.

---

# 26. `subtreeFlags` vs `flags`

Think:

```text
flags
   ↓
work on THIS Fiber

subtreeFlags
   ↓
work somewhere BELOW this Fiber
```

For example:

```text
Main Fiber

flags = NoFlags
subtreeFlags = Update
```

means:

> Main itself isn't necessarily being directly mutated, but something underneath it needs commit work.

This is a useful internal distinction.

---

# 27. `beginWork` can bail out

Suppose a subtree hasn't got relevant work.

React can sometimes avoid redoing all the rendering work.

Conceptually:

```text
beginWork
   ↓
Can I reuse previous work?
   ↓
yes
   ↓
bail out
```

The current `ReactFiberBeginWork.js` contains bailout paths such as `bailoutOnAlreadyFinishedWork`, which consider whether there is relevant scheduled work/context before deciding whether to continue. ([GitHub][3])

This is one place where:

```text
lanes
childLanes
memoization
props
context
```

eventually meet.

We'll study it deeply in the performance and scheduling topics.

---

# 28. What does a bailout do?

Conceptually:

```text
Parent
├── A
└── B
```

Suppose an update affects A but not B.

React may process:

```text
A → work
```

while determining:

```text
B → no relevant work
```

and reuse its existing subtree rather than executing all of B's render work again.

So:

```text
Fiber
 ↓
bailout
 ↓
reuse existing subtree
```

Again, bailout is about **rendering work**, not deleting or ignoring the subtree.

---

# 29. `beginWork` is where components actually execute

This gives us a strong answer to an interview question:

> "Where does React call my function component?"

Conceptually:

```text
render phase
   ↓
beginWork
   ↓
function-component update path
   ↓
invoke component
```

So this is the connection:

```text
Component
   ↓
Fiber
   ↓
beginWork
   ↓
component function execution
```

---

# 30. `completeWork` is not where ordinary components execute

A common mistake is to think:

```text
beginWork = render component
completeWork = render component
```

No.

A better model:

```text
beginWork
    ↓
process this Fiber
produce/reconcile children

completeWork
    ↓
children already processed
finish this Fiber
prepare host work
bubble information
```

---

# 31. Full example: initial mount

Take:

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

### Step 1

```text
workInProgress = App
```

### Step 2

```text
beginWork(App)
```

App returns:

```text
div
```

### Step 3

```text
workInProgress = div
```

### Step 4

```text
beginWork(div)
```

Children:

```text
h1
button
```

### Step 5

```text
workInProgress = h1
```

### Step 6

```text
beginWork(h1)
```

No children:

```text
null
```

### Step 7

```text
completeWork(h1)
```

### Step 8

No sibling from h1? Actually:

```text
h1.sibling → button
```

So:

```text
workInProgress = button
```

### Step 9

```text
beginWork(button)
```

No children.

### Step 10

```text
completeWork(button)
```

### Step 11

No more button siblings.

Return upward:

```text
button.return → div
```

### Step 12

```text
completeWork(div)
```

### Step 13

Return upward:

```text
div.return → App
```

### Step 14

```text
completeWork(App)
```

Now the root has a completed tree.

Then commit occurs.

---

# 32. Notice the shape of the traversal

It isn't:

```text
App
div
h1
button
```

only.

The actual conceptual operations are:

```text
begin App
  begin div
    begin h1
    complete h1
    begin button
    complete button
  complete div
complete App
```

That's much closer to what's really happening internally.

---

# 33. Why completion happens after children

Suppose:

```text
div
├── h1
└── button
```

Could we complete `div` first?

Not for the bottom-up work it needs to perform.

React needs to know about:

```text
h1
button
```

before finalizing the parent subtree.

Hence:

```text
children first
parent second
```

This is classic post-order traversal behavior during the complete phase.

---

# 34. Begin = preorder-ish, complete = postorder-ish

You can relate this to algorithms:

### Going down

```text
begin
```

resembles preorder processing.

### Coming back up

```text
complete
```

resembles postorder processing.

For:

```text
A
├── B
└── C
```

you can visualize:

```text
begin A
begin B
complete B
begin C
complete C
complete A
```

This is a useful algorithmic way to remember it.

---

# 35. Why is this an iterative traversal?

Because the Fiber structure stores:

```text
child
sibling
return
```

React can implement traversal with a work pointer rather than recursively calling:

```javascript
render(child)
```

for arbitrary depth.

This is one of the foundational ideas behind Fiber.

---

# 36. Work loop + Fiber = explicit continuation

Imagine we're here:

```text
App
└── Main
    └── ProductList
        └── Product #100
```

Suppose Product #100 is the current work.

The Fiber graph tells React:

```text
Product #100
   return → ProductList
   sibling → Product #101
```

If work finishes:

```text
Product #100
   ↓
sibling
   ↓
Product #101
```

If it has no sibling:

```text
Product #100
   ↓
return
   ↓
ProductList
```

That's effectively a stored continuation.

This is one of the deepest reasons the Fiber data structure is useful.

---

# 37. What happens if rendering throws/suspends?

Now we're reaching advanced React.

Suppose a component:

```jsx
function User() {
    throw promise;
}
```

or otherwise suspends.

React cannot simply continue as though everything succeeded.

The work loop has mechanisms for:

```text
suspension
error handling
unwinding
replaying
retrying
```

The current `ReactFiberWorkLoop.js` contains substantial logic around thrown values, suspension, replay, and unwinding. Recent React source/issues also show these mechanisms being actively maintained in the current codebase. ([GitHub][1])

We will cover Suspense later rather than mixing its complexities into the basic traversal.

---

# 38. The commit phase

Once the render work completes:

```text
workInProgress = null
```

for the root's render.

React has a finished tree.

Conceptually:

```text
WIP tree
   ↓
finished work
   ↓
commitRoot(...)
```

The work loop source contains the root commit machinery, while the render/commit split is also documented by React itself. ([GitHub][1])

---

# 39. What happens during commit?

At a high level, commit handles things such as:

```text
DOM mutations
refs
layout-related lifecycle work
passive-effect scheduling/processing
```

For example:

```text
Placement
    ↓
insert DOM node

Update
    ↓
update DOM properties/text

ChildDeletion
    ↓
remove DOM subtree
```

React's docs state that the commit stage is where React modifies the DOM, while refs are also attached during commit rather than render. ([React][6])

---

# 40. Commit is not another reconciliation

This is another good interview distinction.

Render:

```text
calculate
reconcile
prepare
```

Commit:

```text
apply
```

So:

```text
Render:
"What should happen?"

Commit:
"Make it happen."
```

---

# 41. Actual DOM mutation example

Suppose:

```text
Old:
<h1>Hello</h1>

New:
<h1>Hello World</h1>
```

Render phase identifies:

```text
Update required
```

Then:

```text
commit
   ↓
host update
   ↓
DOM text becomes "Hello World"
```

React's renderer API separates preparation/reconciliation from host updates, with host-specific commit operations applying the actual mutation. ([React][6])

---

# 42. Placement example

Suppose new:

```jsx
<div>
    <span>Hello</span>
</div>
```

didn't previously contain that `span`.

Reconciliation marks:

```text
span Fiber
flags = Placement
```

Then commit does conceptually:

```javascript
parent.appendChild(span);
```

The exact DOM operation depends on surrounding host structure, but the conceptual separation is:

```text
render → decide "Placement"
commit → perform insertion
```

---

# 43. Deletion example

Old:

```jsx
<div>
    <A />
    <B />
</div>
```

New:

```jsx
<div>
    <A />
</div>
```

Reconciliation determines:

```text
B → deletion
```

and records deletion work.

Commit later removes B's host subtree.

The current child reconciler explicitly records deletions through its deletion helpers/flags. ([GitHub][8])

---

# 44. Why render must be pure, revisited

Now you can see exactly why:

```text
beginWork
 ↓
component executes
 ↓
may be interrupted/restarted
 ↓
complete
 ↓
commit
```

Suppose you wrote:

```jsx
function App() {
    sendEmail();

    return <div>Hello</div>;
}
```

React could invoke the render logic under circumstances where that work does not directly correspond to one committed screen update.

Then:

```text
sendEmail()
```

could happen multiple times or happen for work that isn't committed.

That's why React requires render logic to be pure. The official documentation explicitly says render should be a pure calculation and notes development Strict Mode may invoke component functions twice to expose impurities. ([React][6])

---

# 45. The really important interview diagram

You should now be able to draw this:

```text
                  Fiber tree
                      │
                      ▼
                workInProgress
                      │
                      ▼
                performUnitOfWork
                      │
                      ▼
                  beginWork
                      │
             ┌────────┴────────┐
             │                 │
        has child?          no child
             │                 │
            yes                ▼
             │            completeWork
             ▼                 │
          child                 │
             │                 ▼
       repeat process      sibling?
                               │
                        ┌──────┴──────┐
                       yes            no
                        │              │
                        ▼              ▼
                     sibling        return
                                        │
                                        ▼
                                      parent
                                        │
                                        ▼
                                    complete
```

This is the mental model I want you to retain.

---

# 46. One subtle correction

Earlier we described:

```text
beginWork
 ↓
completeWork
```

as though every Fiber follows exactly:

```text
begin → immediately complete
```

That's not always true.

For a Fiber with children:

```text
beginWork
 ↓
child
 ↓
grandchild
 ↓
...
 ↓
completeWork
```

So completion happens only after its descendants are done.

That's why the actual sequence is:

```text
begin parent
begin child
begin grandchild
complete grandchild
complete child
complete parent
```

---

# 47. Another subtle correction: `completeWork` does not mean "commit"

This distinction is extremely important.

```text
completeWork
```

belongs to the **render phase**.

```text
commit
```

belongs to the **commit phase**.

So:

```text
completeWork ≠ commit
```

The flow is:

```text
beginWork
   ↓
children
   ↓
completeWork
   ↓
finished WIP tree
   ↓
commit
```

The reconciler documentation makes this render/commit separation explicit. ([ReadMex][7])

---

# 48. Another subtle correction: DOM may be created before commit

This surprises many people.

During completion of a host component, React can create/prep a host instance during render.

That doesn't necessarily mean the node is already attached to the live document.

Conceptually:

```text
Render:
create DOM instance
    ↓
prepare subtree
    ↓
store in Fiber

Commit:
attach it to live host tree
```

This distinction is especially useful when explaining why render is not equivalent to "the browser is already updated." The reconciler's host API documentation describes creation during render and placement during commit. ([ReadMex][7])

---

# 49. Another useful visualization: current vs WIP

During rendering:

```text
Current tree
────────────
App
└── div
    └── h1 "Hello"


Work-in-progress tree
─────────────────────
App
└── div
    └── h1 "Hello World"
```

Render work happens against the WIP tree.

When complete:

```text
WIP tree
   ↓
finished
   ↓
commit
```

After commit, the new tree becomes the current tree.

That's how Topic 6's `alternate` concept connects to this work loop.

---

# 50. A realistic update trace

Let's now combine everything.

Initial state:

```text
count = 0
```

Tree:

```text
App
└── Counter
    └── button
```

User clicks:

```text
setCount(1)
```

### Update scheduling

```text
state update queued
       ↓
Fiber/root scheduled
```

### Render

```text
workInProgress = App/affected path
```

### `beginWork`

React reaches:

```text
Counter
```

and invokes it.

```text
Counter()
```

Hook processing gives:

```text
count = 1
```

Counter returns:

```jsx
<button>1</button>
```

### Reconciliation

React matches:

```text
old button Fiber
      ↕
new button element
```

and detects a text/value update.

### Completion

```text
complete(button)
complete(Counter)
...
```

Flags/subtree information are completed/bubbled.

### Commit

```text
Update flag
   ↓
DOM mutation
   ↓
button now shows "1"
```

### Browser

```text
DOM updated
   ↓
browser paints
```

This is the complete lifecycle.

---

# 51. A simplified implementation

To cement the idea, imagine we write our own tiny renderer:

```javascript
function performUnitOfWork(fiber) {
    const next = beginWork(fiber);

    if (next) {
        return next;
    }

    return completeUnitOfWork(fiber);
}
```

Then:

```javascript
function completeUnitOfWork(fiber) {
    let node = fiber;

    while (node) {
        completeWork(node);

        if (node.sibling) {
            return node.sibling;
        }

        node = node.return;
    }

    return null;
}
```

Now:

```javascript
while (workInProgress) {
    workInProgress = performUnitOfWork(workInProgress);
}
```

This tiny model already captures the core Fiber traversal architecture:

```text
child
 ↓
begin
 ↓
complete
 ↓
sibling
 ↓
parent
```

The real React code adds many additional concerns, including errors, Suspense, hydration, profiling, lanes, host configuration, and scheduling. ([GitHub][1])

---

# 52. Interview question: What is `beginWork`?

Strong answer:

> `beginWork` is part of React's render-phase Fiber processing. It processes a Fiber according to its type, handles things such as component updates and reconciliation of its children, and returns the next child Fiber to process when there is one. ([GitHub][3])

---

# 53. Interview question: What is `completeWork`?

> `completeWork` runs after the Fiber's child work has completed. Depending on the Fiber type and whether it is mounting/updating/hydrating, it performs completion/preparation work such as host-instance handling and then bubbles relevant subtree information upward. It is still part of the render phase, not the commit phase. ([GitHub][4])

---

# 54. Interview question: Explain the Fiber traversal

Excellent answer:

> React processes the Fiber tree using an explicit depth-first work loop. It begins a Fiber with `beginWork`. If `beginWork` returns a child, React continues downward. Once a Fiber has no further child work, React completes it with `completeWork`. After completion, React proceeds to the Fiber's sibling if one exists; otherwise it follows the `return` pointer back to the parent. This continues until the root's work is completed.

That answer demonstrates genuine understanding.

---

# 55. Interview question: Why `child`, `sibling`, and `return`?

Answer:

```text
child
→ first descendant to process

sibling
→ next child at the same level

return
→ parent to go back to
```

Together they let React perform an explicit depth-first traversal without relying purely on recursive JavaScript call-stack state.

---

# 56. Interview question: Is `completeWork` the commit phase?

**No.**

This is a common trap.

```text
beginWork
   ↓
completeWork
```

are render-phase operations.

Then:

```text
finished tree
   ↓
commitRoot
```

is the transition into commit.

---

# 57. Interview question: When is the DOM modified?

The general answer is:

> React calculates and prepares the next tree during render. The commit phase performs the required host mutations, such as inserting, updating, or removing DOM nodes. ([React][6])

There are nuanced host-instance preparation details during render, but the browser-visible mutation/placement is performed during commit.

---

# 58. Interview question: Can React pause between Fibers?

For appropriate concurrent rendering work, yes.

The Fiber architecture gives React an explicit unit-of-work representation, and the current work loop has scheduler-aware paths that process units while checking whether it should yield. ([Fossies][2])

But don't say:

> "React always pauses after every Fiber."

That's false.

React decides based on the kind of work and scheduling conditions.

---

# 59. The entire rendering architecture

At this point, you should be able to visualize:

```text
                     UPDATE
                        │
                        ▼
               schedule root work
                        │
                        ▼
                Work-in-progress
                  Fiber tree
                        │
                        ▼
                ┌──────────────┐
                │   Work Loop  │
                └──────┬───────┘
                       │
                       ▼
                  beginWork
                       │
                       ├── execute component
                       ├── process state/hooks
                       └── reconcile children
                       │
                       ▼
                     child
                       │
                      ...
                       │
                       ▼
                  completeWork
                       │
                       ├── prepare/finish host work
                       ├── bubble subtree info
                       └── record completion
                       │
                       ▼
                Finished WIP tree
                       │
                       ▼
                    commit
                       │
            ┌──────────┼──────────┐
            ▼          ▼          ▼
         Placement   Update    Deletion
            │          │          │
            └──────────┼──────────┘
                       ▼
                      DOM
                       │
                       ▼
                     Paint
```

That is a **very strong React internals mental model**.

---

# 60. The 10 things you should remember

```text
1. Fiber is React's internal unit of work.

2. workInProgress points to the Fiber currently being processed.

3. beginWork processes a Fiber and usually returns its child.

4. A child means "go downward."

5. When no child remains, completeWork runs.

6. After completion, sibling means "go sideways."

7. If there is no sibling, return means "go upward."

8. completeWork is still render phase, not commit.

9. Render prepares a finished tree and records work;
   commit applies host mutations.

10. The explicit Fiber traversal is one of the foundations
    that allows React to schedule/manage rendering work.
```

---

# Where We Are Now

You now have the fundamental React rendering internals:

```text
                 JSX
                  ↓
            React Element
                  ↓
              Fiber
                  ↓
          ┌───────────────┐
          │  Render Phase │
          │               │
          │ beginWork     │
          │     ↓         │
          │   child       │
          │     ↓         │
          │ completeWork  │
          │     ↓         │
          │ sibling/up    │
          └───────┬───────┘
                  ↓
            Finished WIP
                  ↓
               Commit
                  ↓
                DOM
```

The next topic is where React becomes **significantly more interesting**:

# Topic 8 — React Scheduling & Lanes

We'll answer:

> **If many updates happen at the same time, how does React decide which one to process first?**

We'll go deep into:

```text
Update
  ↓
Lane
  ↓
Priority
  ↓
Root pending lanes
  ↓
getNextLanes()
  ↓
render selected lanes
  ↓
possibly yield
  ↓
resume later
```

And we'll connect it to:

```text
setState()
startTransition()
useTransition()
urgent vs non-urgent updates
concurrent rendering
automatic batching
why typing stays responsive while expensive UI updates can be deferred
```

This is the topic that makes **Fiber + Concurrent React** finally click.

[1]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberWorkLoop.js "react/packages/react-reconciler/src/ReactFiberWorkLoop.js at main · react/react · GitHub"
[2]: https://fossies.org/diffs/react/19.2.8_vs_19.3.0/packages/react-reconciler/src/ReactFiberWorkLoop.js-diff.html?utm_source=chatgpt.com "react: ...er/src/ReactFiberWorkLoop.js - 19.2.8 vs. 19.3.0 changes | Fossies Diffs"
[3]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberBeginWork.js "react/packages/react-reconciler/src/ReactFiberBeginWork.js at main · react/react · GitHub"
[4]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberCompleteWork.js "react/packages/react-reconciler/src/ReactFiberCompleteWork.js at main · react/react · GitHub"
[5]: https://deepwiki.com/react/react/2.1-fiber-data-model-and-work-loop?utm_source=chatgpt.com "Fiber Data Model and Work Loop | react/react | DeepWiki"
[6]: https://react.dev/learn/render-and-commit?utm_source=chatgpt.com "Render and Commit – React"
[7]: https://readmex.com/en-US/facebook/react/page-57f9be061-da09-4a99-9e20-ac657d991f66?utm_source=chatgpt.com "爱獭知识社区"
[8]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactChildFiber.js "react/packages/react-reconciler/src/ReactChildFiber.js at main · react/react · GitHub"

