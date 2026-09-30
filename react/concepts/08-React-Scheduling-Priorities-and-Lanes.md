# Topic 8 — React Scheduling, Priorities, and Lanes

This is the topic that connects everything we've learned:

```text
setState()
   ↓
update
   ↓
Fiber
   ↓
Lane / priority
   ↓
Scheduler
   ↓
render selected work
   ↓
possibly yield
   ↓
resume / restart
   ↓
commit
```

The important idea is:

> **Modern React doesn't treat every update as equally urgent.**

For example, updating the value of a text input should remain responsive, while recalculating a huge chart can be treated as interruptible background work. React introduced transitions specifically to let applications distinguish urgent updates from non-urgent ones. ([GitHub][1])

---

# 1. Why does React need scheduling?

Imagine this:

```text
User types:
A
```

and your application does something expensive:

```text
10,000 products
+
filtering
+
sorting
+
rendering
+
chart calculations
```

If every keystroke forces React to perform all that work synchronously:

```text
keypress
  ↓
render huge UI
  ↓
finally update input
```

the input can feel laggy.

The user doesn't care whether the product list finishes 20 ms earlier.

They **do** care whether the keyboard responds immediately.

So React needs the ability to distinguish:

```text
urgent work
```

from:

```text
non-urgent work
```

React's concurrent rendering model was designed in part to let rendering work be interruptible; React 18's documentation describes transitions as updates that can be interrupted and restarted when higher-priority work arrives. ([GitHub][1])

---

# 2. Important terminology

There are several words that sound similar:

```text
Scheduling
Priority
Lane
Scheduler
Concurrent rendering
Transition
```

They are **not interchangeable**.

A useful mental model is:

```text
Priority
   ↓
represented internally using lanes
   ↓
React chooses which lanes to work on
   ↓
scheduler/work loop performs that work
```

We'll unpack every part.

---

# 3. Start with ordinary `setState`

Suppose:

```jsx
function App() {
    const [count, setCount] = useState(0);

    return (
        <button onClick={() => setCount(count + 1)}>
            {count}
        </button>
    );
}
```

When you click:

```text
click
 ↓
setCount(1)
```

We've already learned that React doesn't simply mutate:

```text
count = 1
```

inside the current render.

Instead, an update is created and scheduled.

Now we add another question:

> **What priority should this update have?**

That's where lanes enter.

---

# 4. What is a Lane?

A lane is React's internal way of representing a **category of pending work / priority**.

Do not think:

```text
Lane = one update
```

That's too simplistic.

Think:

```text
Lane = a bit in React's bitmask-based representation
       used to group and prioritize pending work
```

React's current source defines lane constants as bitmasks and maintains sets of pending lanes on roots/Fibers. The Fiber structure contains both `lanes` and `childLanes`. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

---

# 5. Why a bitmask?

This is a clever implementation technique.

Imagine we had:

```text
Priority A
Priority B
Priority C
Priority D
```

We could encode them as bits:

```text
0001
0010
0100
1000
```

Then multiple priorities can be combined:

```text
A + C

0001
0100
----
0101
```

That lets React efficiently ask:

> "Which categories of work are pending?"

using bitwise operations.

Conceptually:

```javascript
const pending = laneA | laneC;
```

and:

```javascript
const hasA = pending & laneA;
```

The current React lane implementation is indeed based on bitwise lane masks. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

---

# 6. Why not use a simple integer priority?

You might ask:

```text
priority = 1
priority = 2
priority = 3
```

That's useful for a simple priority queue.

But React frequently needs to represent:

```text
multiple pending updates
multiple categories of work
subtree work
entanglement
suspension/ping behavior
expiration
transitions
```

A bitmask is much more expressive.

For example:

```text
pending lanes:

010101001001
```

can mean:

```text
several categories of work are pending simultaneously
```

This is one of the most important low-level design choices in modern React scheduling.

---

# 7. Fiber stores lanes

Recall our Fiber:

```javascript
{
    child,
    sibling,
    return,

    memoizedState,

    lanes,
    childLanes,

    alternate
}
```

Now we can understand:

```text
lanes
```

as:

> work directly associated with this Fiber.

And:

```text
childLanes
```

as:

> work associated with descendants of this Fiber.

The current Fiber implementation explicitly stores both. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiber.js))

---

# 8. Root also tracks pending work

Imagine:

```text
App
├── Header
├── Main
│   └── Chart
└── Footer
```

Suppose Chart has low-priority work.

React needs some way to know from the root:

> "There is pending work somewhere in this tree."

Conceptually:

```text
Root
 └── pendingLanes
```

The current React lane/root scheduling code maintains sets such as `pendingLanes`, suspended lanes, pinged lanes, expired lanes, and other lane state. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

---

# 9. The root is the scheduling boundary

A state update happens somewhere deep in the tree:

```text
App
└── Dashboard
    └── Chart
         └── ...
```

But React ultimately coordinates rendering at the root.

Conceptually:

```text
Chart update
    ↓
Chart Fiber
    ↓
mark/update parent path
    ↓
Root knows pending work
```

This is why lanes are propagated upward through the Fiber tree.

---

# 10. From update to lane

Let's trace:

```jsx
setCount(1)
```

Conceptually:

```text
setCount
   ↓
create/enqueue update
   ↓
determine update lane
   ↓
attach lane to update
   ↓
mark Fiber/root as having pending work
   ↓
schedule root
```

The exact dispatch code differs depending on the update source and current execution context, but the lane selection and propagation are core pieces of the update path. The current React reconciler has update scheduling functions that request lanes, mark roots updated, and schedule root work. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberWorkLoop.js))

---

# 11. Why does the update need a lane?

Because React may have:

```text
Update A
Update B
Update C
```

at roughly the same time.

It needs to answer:

```text
Which update should I process now?
Which can wait?
Which should be processed together?
Which can interrupt existing work?
```

Lanes provide the bookkeeping needed for those decisions.

---

# 12. Urgent vs non-urgent updates

Consider a search screen.

```jsx
function SearchPage() {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState([]);

    function handleChange(e) {
        setQuery(e.target.value);

        startTransition(() => {
            setResults(filterHugeDataset(e.target.value));
        });
    }

    return ...;
}
```

Here conceptually:

```text
setQuery(...)
   ↓
urgent update

setResults(...)
   ↓
Transition update
   ↓
non-urgent
```

React's transition APIs explicitly allow you to mark updates as non-blocking/interruptible. ([React][2])

---

# 13. Why should typing be urgent?

Suppose the user types:

```text
R
Re
Rea
Reac
React
```

We want:

```text
input
 ↓
character appears immediately
```

Even if the results list is expensive.

So React can conceptually schedule:

```text
Input update
████████████ urgent

Results update
░░░░░░░░░░ background
```

If another keystroke arrives while rendering results:

```text
results render
      ↓
new input update
      ↓
interrupt/restart results work
      ↓
process input
      ↓
continue results
```

This is the behavior React's Transition documentation is describing when it says a transition update can be interrupted by another state update. ([React][3])

---

# 14. What does `startTransition` actually do?

Example:

```jsx
startTransition(() => {
    setPage("/about");
});
```

The important thing is:

> React marks the state updates scheduled during that synchronous action as **Transition updates**.

React's current documentation explicitly says `startTransition` marks synchronous state updates in its callback as Transitions. ([React][3])

So don't think:

```text
startTransition
    ↓
"delay this JavaScript function"
```

That's not the point.

Instead:

```text
startTransition
    ↓
classify the resulting state updates
    ↓
React treats their rendering work as non-blocking/interruptible
```

---

# 15. `startTransition` executes the callback immediately

Very important:

```jsx
startTransition(() => {
    setPage("/about");
});
```

doesn't mean:

```text
wait 1 second
then execute callback
```

The callback runs immediately.

React then treats the state updates inside that callback as transition updates. ([React][3])

So:

```text
startTransition(...)
      ↓
callback executes now
      ↓
updates are marked as Transition
```

---

# 16. Transition does not mean "run on another thread"

Another common mistake.

React runs JavaScript on the JavaScript environment available to the page.

A Transition doesn't create a worker thread.

Instead, React's rendering work can be scheduled and interrupted cooperatively.

So don't say:

> "Transitions run in a background thread."

Say:

> "Transitions mark updates as non-blocking work that React can render concurrently and interrupt when more urgent work arrives."

React describes transitions as non-blocking and interruptible rather than as separate-thread execution. ([React][3])

---

# 17. Concurrent rendering ≠ parallel rendering

This distinction matters a lot.

### Parallel

```text
Task A ────────→ CPU core 1
Task B ────────→ CPU core 2
```

### Concurrent

```text
Task A
 ↓
pause
 ↓
Task B
 ↓
pause
 ↓
Task A
```

React's concurrent rendering model is about **interruptibility and scheduling**, not automatically executing React components on multiple CPU threads.

React's own React 18 documentation describes concurrent rendering as enabling React to prepare multiple versions of the UI and allowing rendering work to be interrupted. ([React][4])

---

# 18. Lanes are not exactly "priority numbers"

This is another subtle point.

It is tempting to say:

```text
Lane 1 = priority 1
Lane 2 = priority 2
Lane 3 = priority 3
```

But lanes are better understood as **sets/categories of work represented as bits**.

React's lane system contains many kinds of lanes, including synchronous/input-related, default, transition, retry, idle, and other categories. The exact constants and groupings are implementation details that can change. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

So:

```text
lane ≠ simple numeric rank
```

A lane is a bit in a richer scheduling model.

---

# 19. Lane groups

React can combine multiple lanes.

Conceptually:

```text
Transition lanes
    = several possible bits

Default-like work
    = another bit/category

Input-related work
    = another category
```

This lets React handle:

```text
one lane
```

or:

```text
several lanes together
```

during a render.

That is why the API contains concepts like:

```text
lanes
lanesToRender
pendingLanes
suspendedLanes
pingedLanes
expiredLanes
```

rather than one global `currentPriority` variable. The current lane implementation exposes these sets and lane-selection operations. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

---

# 20. `getNextLanes`

One especially useful function to know conceptually is:

```text
getNextLanes(...)
```

Its job is essentially:

> Given all the pending work on the root and the current rendering state, which lanes should React work on next?

The current source contains `getNextLanes` and related helpers in `ReactFiberLane.js`. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

Conceptually:

```text
Root:
pending lanes
     ↓
getNextLanes()
     ↓
selected lanes
     ↓
render those lanes
```

This is the bridge between:

```text
"updates exist"
```

and:

```text
"these are the updates React will process now."
```

---

# 21. Example with two updates

Suppose React has:

```text
Lane A = urgent
Lane B = transition
```

Pending:

```text
A + B
```

Conceptually:

```text
pending = 0011
```

React may choose:

```text
next = A
```

and process the urgent work first.

Later:

```text
pending = B
```

and process the transition work.

The exact lane-selection algorithm is more sophisticated because React considers current render state, suspended work, entanglement, expiration, and other conditions. But this illustrates the fundamental idea.

---

# 22. Interruption

Suppose React is processing:

```text
Transition lane
```

and the user types.

Now:

```text
urgent input lane
```

arrives.

React can conceptually do:

```text
Transition rendering
        ↓
new urgent update
        ↓
select urgent lanes
        ↓
interrupt/restart appropriate transition work
        ↓
render urgent update
        ↓
return to transition work
```

React explicitly documents this behavior for transitions: a transition update can be interrupted by other state updates, and React can restart the transition rendering work afterward. ([React][3])

---

# 23. Why can rendering be restarted?

Recall our previous topic.

React has:

```text
Current tree
      ↕
WIP tree
```

Suppose the WIP tree is partially calculated:

```text
WIP:
A
├── B
├── C   ← currently working
└── D
```

An urgent update arrives.

React can retain the current committed tree:

```text
Current:
known-good UI
```

while changing/restarting work on the WIP representation.

This is where:

```text
Fiber
+
alternate
+
lanes
+
work loop
```

all connect.

---

# 24. The architecture we've built

We now have:

```text
                setState()
                    ↓
                  update
                    ↓
              assign a lane
                    ↓
              Fiber/root marked
                    ↓
             schedule root work
                    ↓
              getNextLanes()
                    ↓
              render selected lanes
                    ↓
        ┌───────────┴───────────┐
        │                       │
    continue                  yield
        │                       │
        └───────────┬───────────┘
                    ↓
               complete work
                    ↓
              commit selected result
```

This is modern React's scheduling architecture at a conceptual level.

---

# 25. Where does the Scheduler fit?

This is a subtle architecture question.

React's reconciler decides things like:

```text
what work exists?
what lanes are pending?
what should be rendered?
```

The scheduler infrastructure helps React coordinate when JavaScript work should run/yield.

Don't collapse everything into:

```text
"Scheduler decides React priorities."
```

There's a division between:

```text
React reconciler
```

and:

```text
scheduling/yielding mechanisms
```

The exact boundary has evolved across React versions, so for interviews it's safer to explain the responsibilities rather than claim that one package performs every scheduling decision.

---

# 26. Cooperative scheduling

JavaScript on the browser main thread is fundamentally cooperative.

React can't usually force the browser to preempt arbitrary JavaScript halfway through an instruction.

Instead, React structures rendering as work units and periodically checks whether it should yield.

Conceptually:

```text
perform Fiber
perform Fiber
perform Fiber
   ↓
shouldYield?
   ↓
yes
   ↓
give browser a chance
```

The current work-loop implementation has scheduler-aware paths that check `shouldYield()` while processing concurrent work. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberWorkLoop.js))

---

# 27. This is why Fiber needed explicit work units

Remember Topic 6.

Fiber gives React:

```text
child
sibling
return
```

which lets React traverse the tree incrementally.

Now combine that with scheduling:

```text
Fiber A
 ↓
Fiber B
 ↓
Fiber C
 ↓
shouldYield?
 ↓
pause
```

Without an explicit unit-of-work architecture, that style of rendering would be much harder to implement cleanly.

That's the connection:

```text
Fiber
  ↓
unit of work
  ↓
scheduling
  ↓
interruptibility
```

---

# 28. `startTransition` and lanes

At the API level:

```jsx
startTransition(() => {
    setTab("posts");
});
```

At the conceptual internal level:

```text
startTransition
      ↓
setTab update
      ↓
Transition lane selected
      ↓
Fiber/root marked with transition work
      ↓
React schedules work
```

The exact current lane chosen can depend on React's internal transition/lane allocation logic; don't memorize a specific bit as "the transition lane."

Think:

```text
Transition lane/category
```

rather than:

```text
bit #17 forever
```

React's current lane constants are implementation details and have evolved over time. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

---

# 29. `useTransition`

Now:

```jsx
const [isPending, startTransition] = useTransition();
```

is related to:

```jsx
startTransition(...)
```

but gives you:

```text
isPending
```

to represent transition activity in the UI.

React's documentation describes `useTransition` as returning a boolean indicating whether a Transition is pending plus a function to start one. ([React][2])

Example:

```jsx
const [isPending, startTransition] = useTransition();

function selectTab(tab) {
    startTransition(() => {
        setTab(tab);
    });
}
```

Then:

```jsx
{isPending && <Spinner />}
```

---

# 30. Why do we need `isPending`?

Imagine:

```text
Click Posts
      ↓
transition begins
      ↓
Posts content takes time
```

We want the user to see:

```text
Posts   ← active/pending
```

rather than a frozen-looking application.

So:

```text
startTransition
      +
isPending
```

gives the application a way to show transition progress.

React's docs show `isPending` being used to visually indicate the pending tab. ([React][2])

---

# 31. `useDeferredValue`

Now suppose you don't control the state setter.

You have:

```jsx
function SearchResults({ query }) {
    ...
}
```

but you want the expensive results UI to lag behind the immediately changing query.

React provides:

```jsx
const deferredQuery = useDeferredValue(query);
```

Conceptually:

```text
real query
    ↓
updates immediately

deferred query
    ↓
non-blocking catch-up
```

The current React documentation describes `useDeferredValue` as causing non-blocking re-renders to let the deferred value catch up. ([GitHub][5])

---

# 32. `startTransition` vs `useDeferredValue`

This is a frequent interview question.

### `startTransition`

You control the update:

```jsx
startTransition(() => {
    setResults(results);
});
```

You're telling React:

> "This update is non-urgent."

### `useDeferredValue`

You already have a value:

```jsx
const deferredQuery = useDeferredValue(query);
```

You're telling React:

> "This consumer can lag behind the real value."

So:

```text
startTransition
→ mark the update

useDeferredValue
→ defer the consumption/value
```

---

# 33. Why transitions can't control text inputs

This is an important modern React caveat.

You might try:

```jsx
startTransition(() => {
    setInputValue(e.target.value);
});
```

But React documents that Transition updates cannot be used to control text inputs. ([React][3])

Why?

Because text input updates need to stay synchronously responsive.

So you typically do:

```jsx
setInputValue(value);

startTransition(() => {
    setResults(...);
});
```

Conceptually:

```text
input state
→ urgent

expensive results
→ transition
```

---

# 34. A complete search example

```jsx
function SearchPage() {
    const [query, setQuery] = useState("");
    const [results, setResults] = useState([]);

    function handleChange(e) {
        const value = e.target.value;

        setQuery(value);

        startTransition(() => {
            setResults(searchHugeDataset(value));
        });
    }

    return (
        <>
            <input
                value={query}
                onChange={handleChange}
            />

            <Results results={results} />
        </>
    );
}
```

Conceptually:

```text
keystroke
   ↓
setQuery
   ↓
urgent lane
   ↓
input updates quickly

startTransition
   ↓
setResults
   ↓
transition lane
   ↓
expensive list rendered as interruptible work
```

If the next keystroke arrives while the list is being rendered:

```text
results work
      ↓
new input update
      ↓
input gets priority
      ↓
results work can be interrupted/restarted
```

That is the practical value of concurrent rendering. React's transition documentation uses this exact general pattern for keeping user interactions responsive during slow renders. ([React][3])

---

# 35. Automatic batching vs scheduling

These are related but different.

### Batching

Combines multiple state updates so React can process them together.

```text
setA()
setB()
setC()
 ↓
one coordinated render
```

### Scheduling

Determines:

```text
when?
which work?
what priority?
```

So:

```text
batching ≠ scheduling
```

You can have both:

```text
multiple updates
   ↓
batch
   ↓
lanes determine what work should run
```

React 18 introduced automatic batching alongside concurrent features such as transitions. ([React][4])

---

# 36. Batching vs transitions

Suppose:

```jsx
setA(1);
setB(2);
```

These may be batched.

But:

```jsx
setA(1);

startTransition(() => {
    setB(2);
});
```

now distinguishes the urgency of the work.

Conceptually:

```text
A
→ urgent lane

B
→ transition lane
```

They may therefore be processed with different scheduling behavior.

---

# 37. Lane propagation

Let's visualize:

```text
App
└── Main
    └── Chart
```

Suppose Chart gets:

```text
Transition lane
```

React needs to propagate information toward the root.

Conceptually:

```text
Chart.lanes
    ↓
Main.childLanes
    ↓
App.childLanes
    ↓
Root.pendingLanes
```

This is not a literal "copy this exact value into every field in a simple recursive loop" model, but the important architecture is that pending lane information is propagated upward so ancestors/root scheduling decisions know about descendant work.

The current Fiber structure exposes both `lanes` and `childLanes`, while lane-marking functions update the root's pending lane sets. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiber.js))

---

# 38. Why `childLanes` is so useful

Suppose:

```text
App
├── Header
├── Main
│   └── Chart
└── Footer
```

Only Chart has work.

React reaches:

```text
Header
```

and may be able to say:

```text
nothing relevant here
```

But when it reaches:

```text
Main
```

its `childLanes` tells React:

```text
there's pending work beneath me
```

This connects scheduling with bailout decisions.

The current begin-work implementation checks scheduled lanes and subtree lane information as part of deciding whether work can be skipped. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberBeginWork.js))

---

# 39. `getNextLanes()` is more complicated than "highest priority wins"

This is important for senior interviews.

React must consider things such as:

```text
pending work
suspended work
pinged work
expired work
currently rendering lanes
entangled lanes
warm lanes / retry-related work
transition work
```

The current `ReactFiberLane.js` contains many helpers for these cases, such as determining expired, suspended, pinged, and next lanes. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

So don't describe the algorithm as:

> "React sorts all updates and picks the smallest number."

That's far too simplistic.

---

# 40. Suspended work

Suppose a transition hits Suspense because data isn't ready.

Conceptually:

```text
Transition render
     ↓
suspends
     ↓
lane becomes suspended
```

React doesn't simply forget that work.

It tracks the suspended lane and may later receive a "ping" when the suspended resource is ready.

Then:

```text
promise resolves
     ↓
ping
     ↓
React retries relevant work
```

The current lane implementation explicitly tracks suspended and pinged lanes. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

We'll study Suspense deeply later.

---

# 41. Expiration

Some work shouldn't wait indefinitely.

React's lane system can track work that has become expired.

Conceptually:

```text
pending
   ↓
waits
   ↓
eventually considered expired
   ↓
must receive stronger scheduling treatment
```

The current lane implementation contains expiration-related logic and exposes `markStarvedLanesAsExpired`. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

Again, you don't need to memorize constants; understand the architectural purpose.

---

# 42. Entanglement

This is a more advanced interview topic.

Sometimes React must treat related lanes together rather than independently.

Conceptually:

```text
Lane A
Lane B
```

may become linked:

```text
A ↔ B
```

so that React processes them in a coordinated manner.

React's lane implementation contains entanglement state and helper functions for entangling lanes. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

You generally only need this level of detail for senior/interview internals discussions.

---

# 43. Scheduling example with lanes

Imagine:

```text
Pending:

Input lane       ✓
Transition lane  ✓
Default lane     ✓
```

Conceptually:

```text
pendingLanes
= 1011
```

React asks:

```text
getNextLanes()
```

and chooses an appropriate subset.

Perhaps:

```text
nextLanes = Input
```

Render:

```text
Input work
```

Then:

```text
pending:
Transition + Default
```

React chooses the next suitable work according to scheduling conditions.

This is why the root maintains a **set of pending lanes**, not just one pending priority.

---

# 44. What does "concurrent" really mean here?

A very strong interview definition:

> **Concurrent React means React can work on rendering without treating every render as one indivisible synchronous operation. It can schedule work, prioritize it, interrupt certain rendering work, and continue or restart it later while preserving the currently committed UI.**

That's much better than:

> "Concurrent React uses multiple threads."

React's official documentation describes concurrency as the ability to prepare multiple versions of the UI and interrupt rendering, rather than as multithreaded rendering. ([React][4])

---

# 45. Rendering in the background

React's `startTransition` documentation uses the phrase:

> "render a part of the UI in the background." ([React][3])

But understand what "background" means here.

It means:

```text
non-blocking / lower-urgency rendering
```

not:

```text
Web Worker
```

So:

```text
background React render
≠
separate CPU thread
```

---

# 46. Current vs WIP becomes extremely important now

Suppose current UI:

```text
Tab = Home
```

Transition starts:

```text
Tab = Posts
```

React may construct:

```text
Current tree
   → Home
```

while:

```text
WIP tree
   → Posts
```

If the user clicks:

```text
Contact
```

before Posts finishes:

```text
urgent new update
```

React can prioritize that newer work instead of forcing the user to wait for the unfinished Posts rendering.

This is precisely why:

```text
Fiber
+
alternate
+
lanes
+
work loop
```

are all part of one architecture.

---

# 47. A timeline

Let's visualize:

```text
Time →

User click "Posts"
│
├── Transition update
│
├── Begin rendering Posts
│
├── Fiber A
│
├── Fiber B
│
├── Fiber C
│
│   user types
│
├───────────────┐
│               │
▼               │
Input update    │
urgent          │
                │
render input    │
                │
commit input    │
                │
resume/restart  │
Posts work     ─┘
```

The key point:

> The transition render doesn't have to block the urgent interaction.

That is exactly what the React 18 transition design was intended to enable. ([GitHub][1])

---

# 48. Does this mean the user always sees partial React trees?

Generally, no.

This is another common misconception.

React's rendering can be interruptible, but the commit is coordinated.

You don't normally see:

```text
half-rendered tree
```

because React calculated some Fibers and immediately exposed them.

Instead:

```text
WIP tree
   ↓
complete enough
   ↓
commit
   ↓
new UI becomes visible
```

The currently committed UI remains the stable visible tree while alternate work is prepared.

This is one of the reasons the current/WIP model is important.

---

# 49. What does `shouldYield()` mean?

Conceptually:

```text
React processing concurrent work
       ↓
shouldYield()?
```

means:

> "Should React stop doing more render work right now and give control back to the browser/scheduler?"

The current work loop contains scheduler-aware loops that check for yielding while processing concurrent work. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberWorkLoop.js))

---

# 50. Why yielding matters for UX

Without yielding:

```text
React computation
████████████████████████████████
```

The browser may have little opportunity to respond during that JavaScript execution.

With cooperative work:

```text
React
████████
browser
░░
React
██████
browser
░░░
```

The exact timing is more nuanced than this illustration, but the UX goal is:

```text
long rendering work
      ↓
don't monopolize the main thread unnecessarily
```

---

# 51. Important: React doesn't make expensive computation cheap

A Transition does **not** make this:

```javascript
calculateHugeChart();
```

intrinsically faster.

It changes how the resulting React rendering work is scheduled.

If your component spends:

```text
500 ms
```

doing a synchronous calculation before React can yield, React can't magically interrupt JavaScript in the middle of that function execution.

That's an important practical limitation.

Concurrency helps React schedule its rendering work, but it doesn't transform arbitrary synchronous JavaScript into interruptible pieces.

---

# 52. This is why component architecture still matters

Suppose:

```jsx
function HugeComponent() {
    const result = expensiveCalculation();

    ...
}
```

Even if the surrounding update is a Transition, that calculation still costs CPU time.

So concurrency is **not a replacement** for:

```text
memoization
virtualization
code splitting
algorithmic optimization
avoiding unnecessary renders
```

We'll cover those in the performance section.

---

# 53. `useDeferredValue` in this context

Suppose:

```jsx
function SearchResults({ query }) {
    const deferredQuery = useDeferredValue(query);

    return <Results query={deferredQuery} />;
}
```

Now:

```text
query
 ↓
immediate

deferredQuery
 ↓
non-blocking update
```

This lets:

```text
input UI
```

stay responsive while:

```text
expensive results
```

catch up.

React's current `useDeferredValue` documentation describes the deferred value as triggering non-blocking renders to catch up with the current value. ([GitHub][5])

---

# 54. What happens if the user types rapidly?

Suppose:

```text
R
Re
Rea
Reac
React
```

You can conceptually get:

```text
query:
R → Re → Rea → Reac → React

deferredQuery:
R
   ↓
Re
   ↓
Rea
   ↓
React
```

The deferred result doesn't necessarily render every intermediate state if React can supersede pending work.

This is one of the key user-experience benefits of concurrent rendering.

---

# 55. Transitions can be restarted

Suppose:

```text
Transition:
Home → Posts
```

starts rendering.

Then:

```text
user chooses Contact
```

Now React doesn't necessarily need to finish:

```text
Home → Posts
```

just to immediately start:

```text
Posts → Contact
```

It can abandon/restart the unfinished transition rendering work as appropriate.

React's documentation explicitly says that transition work can be interrupted and restarted when another state update arrives. ([React][3])

---

# 56. This is where "render is pure" becomes essential

Remember our earlier rule:

```text
render must be pure
```

Now it should make much more sense.

React may:

```text
render
 ↓
pause
 ↓
restart
 ↓
render again
 ↓
commit
```

Therefore:

```jsx
function Component() {
    sendEmail(); // dangerous
    return <div />;
}
```

is fundamentally incompatible with this style of rendering.

React's documentation emphasizes render purity precisely because rendering can occur without immediately committing and may be invoked more than once in development. ([React][4])

---

# 57. Current React 19.3 context

One up-to-date point for interviews:

React 19.3, released in September 2026, continues the Transition/concurrent model and additionally integrates transition-driven UI behavior with the new `<ViewTransition>` API. Updates marked as Transitions can participate in View Transition animations. ([React][6])

So if an interviewer asks about modern React, don't treat:

```text
startTransition
```

as an obsolete React 18 experiment.

It remains part of the current React model. ([React][6])

---

# 58. Very important interview distinction: priority vs order

Suppose:

```text
Update A arrives
Update B arrives later
```

It does **not** automatically mean:

```text
A runs first
B runs second
```

React's scheduling can select work based on lane priority/state rather than simple arrival order.

So:

```text
arrival order
≠
render order
```

This is exactly why the lane model exists.

---

# 59. Another interview distinction: scheduling vs batching

Compare:

```text
Batching
```

which answers:

> Which updates can be processed together?

and:

```text
Scheduling
```

which answers:

> Which work should React process now, at what priority, and can it be interrupted?

These are separate concepts.

---

# 60. Another interview distinction: transition vs debounce

A lot of developers confuse:

```text
startTransition
```

with:

```text
debounce
```

They are not the same.

### Debounce

Means:

```text
wait until input stops for N milliseconds
```

Example:

```text
typing → wait 300 ms → API call
```

### Transition

Means:

```text
don't treat this rendering update as urgent
```

So a Transition doesn't inherently:

```text
wait 300 ms
```

It changes scheduling/priority behavior.

---

# 61. Transition vs throttle

Same distinction.

### Throttle

```text
allow execution at most once every N ms
```

### Transition

```text
render this update as non-urgent/interruptible
```

Different problems, different tools.

---

# 62. Lanes + Fiber + work loop

This is the single most important picture from this topic:

```text
                     UPDATE
                       │
                       ▼
                  Fiber update
                       │
                       ▼
                 choose a Lane
                       │
                       ▼
              mark Fiber/root
                       │
                       ▼
               pendingLanes
                       │
                       ▼
                 getNextLanes()
                       │
                       ▼
                selected lanes
                       │
                       ▼
                 Work Loop
                       │
               ┌───────┴───────┐
               ▼               ▼
           process          shouldYield?
             Fiber               │
               │              yes│
               │                 ▼
               │             yield
               │                 │
               └─────────────────┘
                       │
                       ▼
                 completed WIP
                       │
                       ▼
                     commit
                       │
                       ▼
                      DOM
```

That is the architecture you want to have in your head.

---

# 63. Let's connect this to `childLanes`

Imagine:

```text
App
├── Header
├── Main
│   └── Chart
└── Footer
```

Chart gets transition work.

Conceptually:

```text
Chart.lanes = Transition
```

Information about descendant work propagates:

```text
Main.childLanes = Transition
```

and higher ancestors/root know:

```text
pending work exists in Main's subtree
```

Now React can make decisions like:

```text
Header:
nothing relevant

Footer:
nothing relevant

Main:
must inspect subtree
```

This is where scheduling and bailout optimization meet.

---

# 64. What does a bailout look like now?

Suppose current render is for:

```text
Lane = Input
```

but:

```text
Main.childLanes
```

contains only:

```text
Transition
```

Then React may determine:

```text
Transition isn't relevant to this render
```

and avoid processing unnecessary work in that subtree.

That is a conceptual explanation of how lanes can influence bailout behavior; the current `beginWork` implementation checks the current render lanes against pending Fiber/subtree work before deciding whether to continue or bail out. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberBeginWork.js))

---

# 65. Why this matters for performance

Now we can see why performance isn't just:

```text
Virtual DOM is fast.
```

Instead:

```text
Fiber
 +
reconciliation
 +
lanes
 +
bailouts
 +
scheduling
 +
transitions
```

lets React avoid doing unnecessary or poorly prioritized work.

That is a much more technically accurate explanation.

---

# 66. Interview scenario

Interviewer:

> "I have a search box and a huge list. Typing is laggy. How would React's concurrent features help?"

Strong answer:

> Keep the text-input state as an urgent update and mark the expensive result update as a Transition, or use `useDeferredValue` when the expensive component consumes a value you don't directly control. React can then render the expensive update as non-blocking work and interrupt/restart it when a more urgent input update arrives. ([React][3])

Then mention:

> This doesn't make the expensive computation itself faster, so I would still look for unnecessary renders, expensive calculations, virtualization, or memoization opportunities.

That's a strong practical answer.

---

# 67. Interview scenario

Interviewer:

> "What is a lane?"

Answer:

> A lane is React's internal bitmask-based representation of a category of pending work and its scheduling priority. Fibers and roots track lane sets so React can select appropriate work, group compatible updates, reason about suspended or expired work, and coordinate rendering and interruption. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

---

# 68. Interview scenario

> "How does `startTransition()` work internally?"

Strong conceptual answer:

```text
startTransition()
      ↓
run action immediately
      ↓
state updates created inside the action
      ↓
marked as Transition work
      ↓
assigned transition lane(s)
      ↓
root receives pending transition work
      ↓
scheduler/reconciler processes it as non-urgent work
      ↓
work can be interrupted by more urgent updates
```

React's documentation confirms that `startTransition` executes its callback immediately and marks synchronous updates inside it as Transitions. ([React][3])

---

# 69. Interview scenario

> "What is the difference between Fiber and Lanes?"

Perfect distinction:

```text
Fiber
→ data structure / unit of rendering work

Lane
→ priority/category of pending work
```

So:

```text
Fiber = "what node/work?"
Lane  = "what urgency/category?"
```

---

# 70. Fiber + Lane example

Imagine:

```text
Fiber: SearchResults
Lane: Transition
```

This means roughly:

```text
"This particular React tree node has work,
and that work belongs to a transition category."
```

You can therefore think:

```text
Fiber = where
Lane  = what kind of pending work
```

---

# 71. Work loop + Lane example

Suppose root has:

```text
pending lanes:
Input + Transition
```

Then:

```text
getNextLanes()
    ↓
Input
```

React enters:

```text
renderRoot for selected lanes
```

and Fiber traversal processes work relevant to those lanes.

Later:

```text
pending lanes:
Transition
```

and the transition work can continue.

This is the core scheduling loop.

---

# 72. Why lane numbers change across React versions

An important interview caution.

Do not memorize:

```text
"TransitionLane = exactly this hex value."
```

The implementation is internal and can change between React versions.

The stable conceptual knowledge is:

```text
lane bitmasks
lane groups
pending lanes
selection of next lanes
suspended/pinged/expired lanes
transition lanes
```

The current source is your reference for exact bit assignments, not your memory. ([github.com](https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberLane.js))

---

# 73. Where scheduling ends and commit begins

Let's take our transition:

```text
startTransition
      ↓
lane
      ↓
render
      ↓
possibly yield/restart
      ↓
complete WIP
```

Eventually:

```text
commit
```

Once React commits that finished tree, the result becomes visible.

So:

```text
Scheduling controls
"when/what to render"

Commit controls
"what completed result becomes visible"
```

That's a very useful distinction.

---

# 74. React doesn't interrupt arbitrary JavaScript

This deserves repeating.

Suppose your component contains:

```javascript
function Component() {
    const result = hugeSynchronousLoop();

    return <div>{result}</div>;
}
```

React cannot generally say:

```text
pause exactly halfway through hugeSynchronousLoop()
```

just because a high-priority update appeared.

The interruption happens at React's units of rendering work and scheduling boundaries.

Therefore:

```text
Fiber scheduling
≠
preemptive JavaScript runtime
```

---

# 75. Why this topic is hard

React scheduling combines several layers:

```text
Browser/event loop
       ↓
React event/update system
       ↓
lane assignment
       ↓
root scheduling
       ↓
work loop
       ↓
Fiber begin/complete
       ↓
yield/restart
       ↓
commit
```

Most simplified React tutorials skip almost all of this.

For senior interviews, understanding these boundaries is much more valuable than memorizing API definitions.

---

# 76. Final mental model for Topic 8

Remember:

```text
User action
    ↓
setState()
    ↓
Update object
    ↓
Lane assigned
    ↓
Fiber/root marked
    ↓
Root has pending lanes
    ↓
getNextLanes()
    ↓
choose work
    ↓
render selected lanes
    ↓
Fiber work loop
    ↓
maybe yield
    ↓
maybe resume/restart
    ↓
finished WIP tree
    ↓
commit
    ↓
DOM
```

And the three key concepts:

```text
Fiber
→ unit/data structure for work

Lane
→ category/priority of pending work

Scheduler/work loop
→ coordinates when rendering work is performed
```

---

# 77. Revision Sheet

```text
Lane
→ bitmask representing a category of React work.

pendingLanes
→ work pending at the root.

lanes
→ work directly associated with a Fiber.

childLanes
→ work somewhere in the Fiber's subtree.

getNextLanes()
→ chooses the next set of lanes React should work on.

startTransition()
→ marks synchronous state updates inside its callback
   as non-urgent Transition work.

useTransition()
→ same transition mechanism plus isPending.

useDeferredValue()
→ lets a value lag behind through non-blocking rendering.

Concurrent rendering
→ rendering work can be scheduled, interrupted,
   resumed, or restarted.

Yielding
→ React gives control back rather than monopolizing
   the main thread during appropriate concurrent work.

Fiber
→ the unit that makes this scheduling model possible.
```

---

# 78. The React internals we've now connected

```text
                         React
                           │
                           ▼
                       setState
                           │
                           ▼
                         Update
                           │
                           ▼
                          Lane
                           │
                           ▼
                    Root pending lanes
                           │
                           ▼
                     getNextLanes()
                           │
                           ▼
                    selected work
                           │
                           ▼
                     Fiber work loop
                           │
                   ┌───────┴────────┐
                   ▼                ▼
                begin            yield?
                   │                │
                   ▼                ▼
             reconciliation      pause
                   │                │
                   ▼                │
               complete             │
                   └───────┬────────┘
                           ▼
                       Finished WIP
                           │
                           ▼
                         Commit
                           │
                           ▼
                          DOM
```

At this point, you understand the core chain:

**JSX → Elements → Components → Rendering → Reconciliation → Fiber → Work Loop → Lanes → Scheduling → Commit.**

---

# Next Topic — React State Internals & `useState`

Now we can go into one of the **most frequently asked React interview areas**:

```jsx
const [count, setCount] = useState(0);
```

We'll answer:

> **Where does `count` actually live?**

> **How does React know this is the first `useState` rather than the second?**

> **What exactly happens when `setCount()` is called?**

> **What is the Hook linked list?**

> **What is the update queue?**

> **Why does `setCount(count + 1)` behave differently from `setCount(c => c + 1)`?**

> **How does React process multiple queued updates?**

We'll conceptually reconstruct a simplified version of:

```text
Fiber
  ↓
memoizedState
  ↓
Hook
  ↓
queue
  ↓
update
  ↓
dispatchSetState
  ↓
lane
  ↓
scheduleUpdateOnFiber
  ↓
render
  ↓
process update queue
```

That topic will be the bridge from **React architecture** into **actual Hook implementation internals**.

[1]: https://github.com/reactjs/rfcs/blob/main/text/0212-react-18.md?utm_source=chatgpt.com "rfcs/text/0212-react-18.md at main · reactjs/rfcs · GitHub"
[2]: https://react.dev/reference/react/useTransition?utm_source=chatgpt.com "useTransition – React"
[3]: https://ar.react.dev/reference/react/startTransition?utm_source=chatgpt.com "startTransition – React"
[4]: https://react.dev/blog/2022/03/29/react-v18?utm_source=chatgpt.com "React v18.0 – React"
[5]: https://github.com/reactjs/react.dev/blob/main/src/content/reference/react/useTransition.md?utm_source=chatgpt.com "react.dev/src/content/reference/react/useTransition.md at main · reactjs/react.dev · GitHub"
[6]: https://react.dev/blog/2026/09/09/react-19-3?utm_source=chatgpt.com "React 19.3 – React"

