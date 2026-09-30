# Topic 4 — React Rendering: From `setState()` to the DOM

This is one of the **most important React interview topics**.

By the end of this topic, you should be able to explain this entire pipeline:

```text
User interaction
      ↓
event handler
      ↓
setState()
      ↓
update queued
      ↓
React schedules work
      ↓
Render phase
      ↓
Fiber traversal / reconciliation
      ↓
Commit phase
      ↓
DOM mutations
      ↓
Browser paint
```

The official React model describes screen updates as **Trigger → Render → Commit**, and explicitly notes that rendering does not necessarily mean the DOM changes. ([React][1])

---

# 1. First: what does "render" mean in React?

This is probably the biggest source of confusion.

When someone says:

> "React rendered the component"

they do **not** necessarily mean:

> "React changed the DOM."

In React terminology, rendering primarily means:

> **React calls components to determine what the UI should look like.**

For example:

```jsx
function App() {
    return <h1>Hello</h1>;
}
```

Rendering means React evaluates the component and obtains the next React element structure.

Conceptually:

```text
App()
  ↓
<h1>Hello</h1>
```

Then React compares the new result with the previous tree and determines whether anything actually needs to change in the host environment. ([React][1])

So:

```text
render
≠
DOM update
```

---

# 2. The three big stages

Keep this picture in your head:

```text
             ┌─────────┐
             │ Trigger │
             └────┬────┘
                  ↓
             ┌─────────┐
             │ Render  │
             └────┬────┘
                  ↓
             ┌─────────┐
             │ Commit  │
             └────┬────┘
                  ↓
                DOM
```

And then:

```text
DOM
 ↓
Browser
 ↓
Paint
```

React officially documents this three-step model. ([React][1])

Let's now go much deeper.

---

# 3. What triggers an initial render?

Typical application startup:

```jsx
const root = createRoot(
    document.getElementById("root")
);

root.render(<App />);
```

The important call is:

```jsx
root.render(<App />);
```

That requests an initial render.

Conceptually:

```text
root.render(<App />)
        ↓
React receives requested UI
        ↓
schedule work
        ↓
render
        ↓
commit
```

On the first render, React starts from the root and builds the initial tree. ([React][1])

---

# 4. What triggers subsequent renders?

The most common case is:

```jsx
setCount(...)
```

Example:

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

Click:

```text
button
 ↓
onClick
 ↓
setCount(1)
```

This does **not** directly mean:

```text
DOM immediately becomes 1
```

Instead:

```text
setCount(1)
    ↓
queue/update state
    ↓
request another render
```

React describes a state update as queuing another render. ([React][2])

---

# 5. What `setState()` does NOT do

Suppose:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    function handleClick() {
        setCount(1);

        console.log(count);
    }

    return <button onClick={handleClick}>{count}</button>;
}
```

A beginner might expect:

```text
setCount(1)
 ↓
count becomes 1
 ↓
console.log → 1
```

But you'll generally get:

```text
console.log → 0
```

Why?

Because `count` belongs to the **current render**.

React doesn't mutate that render's local JavaScript variable.

Instead:

```text
Current render
count = 0
     │
     │ setCount(1)
     ▼
React queues update
     │
     ▼
Future render
count = 1
```

React's documentation describes state as a **snapshot**: setting state requests another render rather than changing the state variable in the already-running render. ([React][2])

---

# 6. State is a snapshot

This concept is extremely important.

Suppose:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    return (
        <button onClick={() => {
            console.log(count);
            setCount(count + 1);
            console.log(count);
        }}>
            {count}
        </button>
    );
}
```

Both logs refer to the same render's:

```text
count = 0
```

So:

```text
log 1 → 0
setCount(1)
log 2 → 0
```

Then React performs another render:

```text
new render
count = 1
```

Think:

```text
Render #1
count = 0

       ↓ setCount(1)

Render #2
count = 1
```

Not:

```text
Render #1
count = 0 → magically changes to 1
```

This model explains a huge number of React bugs.

---

# 7. Trigger phase

Let's trace our counter.

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

User clicks.

### Step 1

Browser dispatches an event.

```text
Browser
  ↓
click
```

### Step 2

React's event system invokes:

```javascript
() => setCount(count + 1)
```

### Step 3

`setCount` creates/enqueues an update associated with the relevant state.

Conceptually:

```text
Fiber for Counter
        │
        ▼
state update:
"next state should be 1"
```

### Step 4

React schedules work for the root containing that Fiber.

Conceptually:

```text
Counter changed
    ↓
root needs work
```

At this point we enter the scheduling/rendering machinery.

---

# 8. Important: "schedule" doesn't mean "render immediately"

This is especially important in modern React.

When you call:

```jsx
setCount(1);
```

you shouldn't think:

```text
setCount()
 ↓
render immediately
```

Instead:

```text
setCount()
 ↓
enqueue update
 ↓
schedule work
 ↓
React decides when/how to perform that work
```

Scheduling becomes particularly important once we get into:

```text
Concurrent React
Lanes
Transitions
Suspense
```

We'll cover those later.

For now:

> `setState()` requests that React process an update; it doesn't mean "perform DOM changes right now."

---

# 9. Render phase

After React starts processing the update, it enters the rendering/reconciliation work.

Conceptually:

```text
Update
 ↓
Render phase
```

React determines:

> "What should the tree look like after applying these updates?"

For:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    return <h1>{count}</h1>;
}
```

the previous render might produce:

```text
<h1>0</h1>
```

The next render produces:

```text
<h1>1</h1>
```

React now has two conceptual trees:

```text
Previous:
<h1>0</h1>

Next:
<h1>1</h1>
```

It needs to determine the difference.

That's where reconciliation comes in.

---

# 10. Render phase is a calculation

This is the central principle:

```text
Render phase
=
calculate the next UI
```

For example:

```jsx
function User({ loggedIn }) {
    if (loggedIn) {
        return <Dashboard />;
    }

    return <Login />;
}
```

React isn't yet saying:

```text
"Delete login DOM node!"
```

It is first calculating:

```text
Current desired tree:
Dashboard
```

Only later does the commit phase apply host changes.

React emphasizes that rendering should be a pure calculation and that rendering itself does not necessarily mutate the DOM. ([React][1])

---

# 11. What happens during render?

At a high level:

```text
Render phase
     │
     ├── determine which work has priority
     │
     ├── process Fiber
     │
     ├── call components
     │
     ├── process hooks
     │
     ├── compare children
     │
     ├── mark required changes
     │
     └── produce completed work
```

Some of these are implementation-level details and evolve between React versions, but this is the right architectural picture.

The React source separates reconciliation work from the host renderer, while the renderer supplies operations such as creating instances and committing updates. ([GitHub][3])

---

# 12. Fiber enters here

Suppose the tree is:

```text
App
└── Counter
    ├── h1
    └── button
```

React represents its work using Fiber nodes.

Conceptually:

```text
Fiber(App)
    │
    └── Fiber(Counter)
            ├── Fiber(h1)
            └── Fiber(button)
```

React can process these units independently.

A simplified view:

```text
begin Counter
    ↓
process children
    ↓
begin h1
    ↓
complete h1
    ↓
begin button
    ↓
complete button
    ↓
complete Counter
```

This **begin → children → complete** pattern is fundamental to understanding the Fiber architecture.

We will study it much more deeply in the Fiber topic.

---

# 13. Calling the component during render

Suppose:

```jsx
function Counter() {
    console.log("Counter render");

    const [count, setCount] = useState(0);

    return <h1>{count}</h1>;
}
```

During the render phase, React invokes the component:

```text
React
 ↓
Counter()
 ↓
returns <h1>...</h1>
```

So:

```javascript
console.log("Counter render");
```

runs during rendering.

This is why you can use:

```javascript
console.log()
```

to observe component renders.

But remember:

> A render can occur without a DOM mutation.

---

# 14. Parent re-rendering

This is a very common interview question.

Consider:

```jsx
function Parent() {
    const [count, setCount] = useState(0);

    return (
        <>
            <Child />
            <button onClick={() => setCount(count + 1)}>
                {count}
            </button>
        </>
    );
}
```

Clicking the button updates:

```text
Parent state
```

So React needs to process the updated subtree.

Without an applicable bailout such as memoization, React's normal behavior is to process the descendants of the updated component as part of the render work. The React docs describe the default behavior as rendering nested components under the updated component, while also noting that performance optimizations can skip work. ([React][1])

So conceptually:

```text
Parent changes
   ↓
Parent renders
   ↓
Child may render
```

But this does **not** mean:

```text
Child DOM is recreated
```

That's a completely separate question.

---

# 15. Component render vs DOM update

Let's make this distinction explicit.

Suppose:

```jsx
function Parent() {
    const [count, setCount] = useState(0);

    return (
        <>
            <Child />
            <h1>{count}</h1>
        </>
    );
}
```

When:

```text
count: 0 → 1
```

React may:

```text
Parent function executes again
Child may be processed again
new React elements are produced
reconciliation happens
```

But the actual DOM change may be only:

```text
<h1>0</h1>
 ↓
<h1>1</h1>
```

The existing `Child` DOM might not be touched at all.

That distinction is absolutely critical.

---

# 16. Commit phase

Once React finishes the necessary render work, it has a description of what needs to happen.

Then:

```text
Render phase
    ↓
Commit phase
```

The commit phase applies the necessary changes to the host environment.

For React DOM:

```text
React
 ↓
React DOM
 ↓
DOM APIs
```

The official reconciler documentation describes host operations such as `createInstance` for creating nodes and `commitUpdate` for mutating an existing instance to match new props. ([GitHub][3])

---

# 17. Initial commit

On the first render:

```jsx
root.render(<App />);
```

React needs to create the initial DOM.

Conceptually:

```javascript
const div = document.createElement("div");
const h1 = document.createElement("h1");

h1.textContent = "Hello";
div.appendChild(h1);

root.appendChild(div);
```

The real implementation is much more sophisticated, but that's the conceptual job.

React's renderer interface explicitly describes creation of host instances during rendering and placement/mutation in commit-related operations. ([GitHub][3])

---

# 18. Update commit

Suppose the previous UI is:

```html
<h1>0</h1>
```

and the new UI is:

```html
<h1>1</h1>
```

React doesn't need:

```javascript
element.remove();
document.createElement("h1");
```

It can update the existing host node.

Conceptually:

```javascript
h1.textContent = "1";
```

This is why:

```text
Re-render
≠
recreate entire DOM
```

React's documentation explicitly says that on re-renders it applies only the necessary DOM operations calculated from the difference between renders. ([React][1])

---

# 19. Browser paint

After React commits changes:

```text
React commit
   ↓
DOM updated
   ↓
Browser performs rendering work
   ↓
paint
```

React's documentation deliberately calls this browser step **painting** to distinguish it from React's "rendering" terminology. ([React][1])

So:

```text
React render
```

and:

```text
browser render
```

are not the same thing.

In interviews, using "paint" helps avoid ambiguity.

---

# 20. Complete example

Let's trace:

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

Initial render:

```text
createRoot()
   ↓
root.render(<Counter />)
   ↓
schedule initial work
   ↓
render Counter
   ↓
useState → 0
   ↓
return <button>0</button>
   ↓
reconcile/create Fiber work
   ↓
commit
   ↓
create DOM button
   ↓
button displayed
```

Now user clicks.

```text
click
  ↓
React event system
  ↓
setCount(1)
  ↓
enqueue state update
  ↓
schedule root work
  ↓
render Counter again
  ↓
useState processes update
  ↓
count = 1
  ↓
return <button>1</button>
  ↓
reconcile old vs new
  ↓
detect text change
  ↓
commit
  ↓
update DOM text
  ↓
browser paints
```

That's the entire React lifecycle at a useful interview level.

---

# 21. Why does React have render and commit as separate phases?

This is one of the key architectural decisions.

Imagine:

```text
Huge component tree
       ↓
10,000 Fibers
```

During rendering, React may need to perform a lot of calculation.

If React immediately mutated the DOM for each calculation, it would be much harder to:

```text
pause work
restart work
discard work
prioritize another update
```

A separated model allows:

```text
Render
  ↓
figure out what should happen
  ↓
Commit
  ↓
actually make the UI change
```

This separation is foundational to React's modern concurrent architecture.

---

# 22. Render phase should be pure

Consider:

```jsx
function Component() {
    document.body.style.background = "red";

    return <h1>Hello</h1>;
}
```

That's problematic because render is supposed to be a calculation.

React's documentation explicitly states that rendering must be pure: same inputs should produce the same output, and rendering should not modify things that existed before the render. ([React][1])

Why is this so important?

Because modern React may:

```text
start rendering
     ↓
pause
     ↓
render something else
     ↓
resume
```

or potentially abandon/restart unfinished work.

If rendering caused irreversible side effects, the result could be incorrect.

---

# 23. What belongs in render?

Good:

```jsx
function User({ name }) {
    return <h1>{name}</h1>;
}
```

Good:

```jsx
function Price({ amount }) {
    const formatted = amount.toFixed(2);

    return <span>{formatted}</span>;
}
```

These are calculations.

---

# 24. What doesn't belong in render?

Bad:

```jsx
function User() {
    fetch("/api/users");

    return <div>User</div>;
}
```

Bad:

```jsx
function User() {
    localStorage.setItem("x", "y");

    return <div />;
}
```

Bad:

```jsx
function User() {
    sendEmail();

    return <div />;
}
```

These are side effects.

Depending on the use case, such work belongs in:

```text
event handlers
Effects
application/data layer
server
```

We'll have a dedicated `useEffect` topic later.

---

# 25. Strict Mode makes this easier to detect

In development Strict Mode, React may invoke component functions more than once to expose impure rendering behavior. React documents this as a development aid for finding mistakes. ([React][1])

So seeing:

```text
render
render
```

in development does **not automatically mean React has committed the DOM twice**.

This is a common interview trap.

---

# 26. Why might a component render without a DOM change?

Example:

```jsx
function Child() {
    console.log("Child render");

    return <h1>Hello</h1>;
}
```

Suppose it rendered before as:

```text
<h1>Hello</h1>
```

and renders again to:

```text
<h1>Hello</h1>
```

React might determine:

```text
previous output = new output
```

so there is no corresponding DOM mutation needed.

You could see:

```text
Child render
```

in the console without seeing the DOM change.

This is one of React's most important concepts. ([React][1])

---

# 27. "Same output" doesn't mean same JavaScript object

This is a subtle but important distinction.

Suppose on every render:

```jsx
<div>Hello</div>
```

creates a new React element object.

So conceptually:

```text
Render 1:
element A

Render 2:
element B
```

Even though:

```text
A !== B
```

React can still determine that they represent compatible UI and reuse the existing host node.

Therefore:

```text
React element object identity
≠
DOM node identity
```

This will become clearer once we study reconciliation.

---

# 28. Does `setState()` always cause a render?

Be careful here.

A common simplified answer is:

> "Calling `setState` causes a re-render."

More accurately:

> A state setter schedules state-update work, and React may determine during processing that no visible work is necessary.

For example:

```jsx
setCount(1);
```

when the current state is already:

```text
1
```

doesn't necessarily result in meaningful rendering work.

React uses state equality/bailout logic in its update processing.

The interview-safe answer is:

```text
setState → schedules an update
             ↓
        React processes it
             ↓
      render may be skipped/bailout
      if nothing relevant changed
```

Don't claim:

> "Every `setState` always causes the component function to execute."

That is too absolute.

---

# 29. Batching

Now let's discuss one of the most frequently asked React questions.

Suppose:

```jsx
function Counter() {
    const [number, setNumber] = useState(0);

    return (
        <button onClick={() => {
            setNumber(number + 1);
            setNumber(number + 1);
            setNumber(number + 1);
        }}>
            +3
        </button>
    );
}
```

A beginner might expect:

```text
0 → 1 → 2 → 3
```

But that's not what happens.

React sees each expression using the same snapshot:

```text
number = 0
```

So these become conceptually:

```javascript
setNumber(1);
setNumber(1);
setNumber(1);
```

React processes the updates together.

The result:

```text
1
```

React documents that it waits for the event handler's code to finish before processing these queued updates, which is batching. ([React][2])

---

# 30. The correct way to perform repeated updates

Use an updater function:

```jsx
setNumber(n => n + 1);
setNumber(n => n + 1);
setNumber(n => n + 1);
```

Now React conceptually has a queue:

```text
current state = 0

update 1: n => n + 1
update 2: n => n + 1
update 3: n => n + 1
```

Processing:

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

This is why updater functions are important when multiple updates depend on the previous state. ([React][2])

---

# 31. Think of state updates as a queue

This is the bridge into our later `useState` internals topic.

Conceptually:

```text
Fiber
 │
 └── Hook
      │
      ├── current state
      │
      └── update queue
```

For example:

```text
state = 0

queue:
    n => n + 1
    n => n + 1
    n => n + 1
```

During rendering, React processes the queue:

```text
0
 ↓ updater 1
1
 ↓ updater 2
2
 ↓ updater 3
3
```

We'll eventually look at the actual internal data structures and dispatch path.

---

# 32. Batching prevents half-finished UI

Imagine:

```jsx
setFirstName("John");
setLastName("Smith");
setFullName("John Smith");
```

If React committed after every individual setter, users could temporarily see:

```text
John Smith
John
John Smith
```

or other inconsistent intermediate states.

Batching lets React process related updates together.

React explicitly notes that batching avoids unnecessary renders and "half-finished" UI states. ([React][2])

---

# 33. Batching does NOT mean all updates everywhere are merged forever

Separate user interactions are handled separately.

For example:

```text
Click #1
   ↓
updates
   ↓
render

Click #2
   ↓
updates
   ↓
render
```

React does not treat two distinct intentional clicks as one giant update. Its documentation explicitly calls this out. ([React][2])

---

# 34. Why the DOM doesn't update halfway through the event handler

Consider:

```jsx
function App() {
    const [count, setCount] = useState(0);

    function handleClick() {
        setCount(1);

        console.log(
            document.querySelector("h1").textContent
        );

        setCount(2);
    }

    return <h1>{count}</h1>;
}
```

You shouldn't expect the DOM to become:

```text
1
```

between the two setter calls.

React generally processes the batched updates after the relevant event-handler work completes.

So the user should normally see the final committed result rather than every intermediate state.

---

# 35. A very important interview distinction

There are **three different things**:

### 1. State update

```text
setCount(...)
```

### 2. Render

```text
component function executes
```

### 3. DOM mutation

```text
React changes browser DOM
```

They are related but not equivalent.

Think:

```text
setState
  ↓
schedule update

render
  ↓
calculate next UI

commit
  ↓
DOM mutation
```

A lot of interview mistakes happen because candidates collapse these three into:

```text
setState → DOM update
```

---

# 36. Example: state update where DOM doesn't change

Consider:

```jsx
function App() {
    const [count, setCount] = useState(0);

    return (
        <>
            <button onClick={() => setCount(1)}>
                Set to 1
            </button>

            <Child />
        </>
    );
}
```

After:

```text
count = 1
```

clicking the button again:

```text
setCount(1)
```

may result in no meaningful visible change.

React can recognize that the relevant state isn't changing to a different value and bail out.

This is why "setter called" and "DOM changed" are not synonymous.

---

# 37. Parent rendering and children

Consider:

```jsx
function Parent() {
    const [count, setCount] = useState(0);

    return (
        <>
            <Child />
            <button onClick={() => setCount(count + 1)}>
                {count}
            </button>
        </>
    );
}
```

When `Parent` updates:

```text
Parent render
   ↓
Child subtree considered
```

Without a bailout:

```text
Child render
```

may also happen.

But:

```text
Child render
```

doesn't imply:

```text
Child DOM recreated
```

And with:

```jsx
const Child = memo(function Child() {
    ...
});
```

React can often skip rendering `Child` when its props haven't changed. React's current docs describe `memo` specifically as allowing React to skip re-rendering when props are unchanged, while noting that it is an optimization rather than an absolute guarantee. ([React][4])

We'll study this much later in Performance.

---

# 38. The key interview question: "Does React re-render the whole application?"

Answer:

**No.**

Don't say:

> "React redraws the entire DOM."

Also don't oversimplify to:

> "React only renders the component whose state changed."

The actual picture is:

```text
update occurs
    ↓
React identifies affected work
    ↓
renders/reconciles the relevant tree
    ↓
bailouts can skip work
    ↓
commit applies necessary host changes
```

The exact amount of work depends on the tree, update type, priorities, context, memoization, and other factors.

---

# 39. "Virtual DOM makes React fast" is also incomplete

A stronger interview answer is:

> React's performance doesn't come from a single "Virtual DOM" trick. React maintains an internal representation of the UI and uses reconciliation, scheduling, selective updates, and a commit phase to determine and apply the necessary host changes.

Modern React adds even more dimensions:

```text
Fiber
Lanes
scheduling
concurrent rendering
memoization
Suspense
transitions
compiler optimizations
```

We'll eventually connect all of them.

---

# 40. The entire lifecycle with actual code

Take this:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    function handleClick() {
        setCount(count + 1);
    }

    return (
        <button onClick={handleClick}>
            Count: {count}
        </button>
    );
}
```

## Initial render

```text
root.render(<Counter />)
        ↓
schedule initial work
        ↓
render Counter
        ↓
useState → 0
        ↓
returns button
        ↓
reconciliation
        ↓
commit
        ↓
DOM created
        ↓
browser paints
```

DOM is conceptually:

```html
<button>
    Count: 0
</button>
```

---

## User clicks

```text
click
 ↓
handleClick()
 ↓
setCount(1)
```

---

## Update scheduling

```text
state update queued
      ↓
root marked for work
      ↓
React schedules update
```

---

## Render

```text
Counter()
   ↓
useState → 1
   ↓
returns:

<button>
    Count: 1
</button>
```

Now React has:

```text
previous:
Count: 0

next:
Count: 1
```

---

## Commit

React applies the required DOM mutation.

Conceptually:

```javascript
button.textContent = "Count: 1";
```

Then:

```text
DOM updated
   ↓
browser paint
```

---

# 41. Where does the previous UI come from?

This leads directly into Fiber.

React needs to retain enough internal information to understand:

```text
What did the tree look like before?
```

and:

```text
What does it look like now?
```

Conceptually:

```text
Current Fiber tree
       │
       │ previous committed structure
       ▼
Work-in-progress Fiber tree
       │
       │ next render
       ▼
reconciliation
```

React's internal Fiber architecture supports this style of work tracking and tree processing.

This is why our next major internals topic is **Fiber**.

---

# 42. Render work can be interrupted

Here's why modern React's architecture is different from a simple:

```text
call component
modify DOM
done
```

Imagine:

```text
Large update
 ↓
Fiber A
 ↓
Fiber B
 ↓
Fiber C
 ↓
Fiber D
```

React may have other work competing for attention.

Modern React can use scheduling and priority mechanisms so rendering work can be organized rather than treating all work as one giant indivisible operation.

Conceptually:

```text
low-priority rendering
        ↓
      pause
        ↓
high-priority update
        ↓
     process
        ↓
resume/restart work
```

This is one of the motivations behind Fiber and modern concurrent rendering.

We'll cover the actual **Lane model** later.

---

# 43. Render phase must not assume it will commit

This is probably the deepest lesson from this topic.

Suppose:

```jsx
function Component() {
    console.log("render");

    return <div>Hello</div>;
}
```

You should NOT reason:

```text
render happened
 therefore
 DOM is definitely going to change
```

Instead:

```text
render
 ↓
React calculates work
 ↓
work may be completed
 ↓
if appropriate, React commits it
```

That separation is why side effects in render are dangerous.

---

# 44. Commit is where React talks to the host environment

For the browser:

```text
React
 ↓
React DOM
 ↓
DOM
```

For another environment:

```text
React
 ↓
different renderer
 ↓
different host environment
```

The reconciler itself is designed around this host-config boundary. React's own reconciler documentation shows host-specific operations such as `createInstance`, `commitUpdate`, `appendChild`, and related operations. ([GitHub][3])

So an excellent interview insight is:

> **React's reconciliation logic is separated from the browser-specific host operations.**

---

# 45. A useful mental model of the internals

At this point, visualize:

```text
                USER
                 │
                 ▼
             Interaction
                 │
                 ▼
             setState()
                 │
                 ▼
           Update queue
                 │
                 ▼
             Scheduling
                 │
                 ▼
        ┌─────────────────┐
        │   RENDER PHASE  │
        │                 │
        │   Fiber work    │
        │      ↓          │
        │  call component │
        │      ↓          │
        │   Hooks/state   │
        │      ↓          │
        │ reconciliation  │
        │      ↓          │
        │ mark work       │
        └────────┬────────┘
                 │
                 ▼
        ┌─────────────────┐
        │  COMMIT PHASE   │
        │                 │
        │ DOM mutations   │
        │ refs/effects*   │
        └────────┬────────┘
                 │
                 ▼
                DOM
                 │
                 ▼
              Paint
```

`*` We'll carefully distinguish layout/passive effects when we reach `useEffect`.

---

# 46. Interview questions you should now know

## Q1. What happens when `setState()` is called?

Strong answer:

> React schedules an update associated with the component's internal state. It doesn't immediately mutate the state variable from the current render or directly update the DOM. React later processes the update during rendering, reconciles the resulting tree, and if necessary commits the required changes to the host environment.

---

## Q2. Does `setState()` immediately change the state?

No.

It requests an update.

The current render's state value remains the same.

```text
current render → unchanged
future render  → updated state
```

([React][2])

---

## Q3. What is rendering in React?

> Rendering is React calling components and calculating the next UI representation. It is part of the render/reconciliation phase and does not necessarily cause a DOM mutation.

([React][1])

---

## Q4. What are the render and commit phases?

> During render, React calculates what the next UI should look like and determines the work required. During commit, React applies the resulting host changes, such as DOM mutations.

([React][1])

---

## Q5. Does every render update the DOM?

No.

```text
component renders
      ↓
same effective UI
      ↓
no DOM mutation required
```

React explicitly documents this behavior. ([React][1])

---

## Q6. Does a parent re-render automatically mean the DOM of every child is recreated?

No.

Child rendering and DOM mutation are different concepts.

React can process child components while preserving existing DOM nodes where the reconciled result allows it.

---

## Q7. Why is rendering required to be pure?

Because React's rendering work can be processed independently of the commit that eventually makes changes visible. Impure render logic can therefore run at unexpected times or more than once in development and can cause incorrect side effects. ([React][1])

---

# 47. One interview scenario

Suppose the interviewer gives you:

```jsx
function App() {
    const [count, setCount] = useState(0);

    console.log("App render");

    return (
        <>
            <h1>{count}</h1>

            <button onClick={() => {
                setCount(count + 1);
                setCount(count + 1);
            }}>
                Increment
            </button>
        </>
    );
}
```

They ask:

> What happens when I click the button?

You should reason:

```text
Current render snapshot:
count = 0
```

Handler executes:

```text
setCount(1)
setCount(1)
```

React batches the updates.

Then:

```text
new render
count = 1
```

Not:

```text
count = 2
```

because both expressions read the same snapshot. ([React][2])

To get 2:

```jsx
setCount(c => c + 1);
setCount(c => c + 1);
```

Then the update queue is processed sequentially:

```text
0
 ↓
1
 ↓
2
```

---

# 48. Another interview scenario

```jsx
function App() {
    const [count, setCount] = useState(0);

    console.log("App");

    return (
        <>
            <Child />
            <button onClick={() => setCount(count + 1)}>
                {count}
            </button>
        </>
    );
}

function Child() {
    console.log("Child");

    return <div>Hello</div>;
}
```

Click button.

A likely conceptual sequence without memoization/bailouts:

```text
App
Child

click
 ↓
setCount
 ↓
App renders
 ↓
Child is processed
 ↓
reconciliation
 ↓
<h1/button> needs update
 ↓
Child DOM may need no mutation
```

So you can potentially see:

```text
App
Child
```

in the console while only the counter text changes in the actual DOM.

This single example demonstrates the distinction between:

```text
render work
```

and:

```text
DOM mutation
```

---

# 49. Important correction to a common interview answer

Don't say:

> "React compares the old Virtual DOM with the new Virtual DOM and changes the DOM."

It's not terrible as a beginner explanation, but for a serious interview it is too vague.

A better explanation is:

> React processes an update through its reconciliation architecture, using its internal Fiber tree to determine what work needs to be performed. The render phase calculates the next tree and required changes, and the commit phase applies the necessary host operations, such as DOM mutations.

That answer demonstrates that you actually understand React rather than memorizing "Virtual DOM."

---

# 50. What we've established so far

You now have:

```text
JSX
 ↓
React elements
 ↓
Components
 ↓
State
 ↓
setState
 ↓
update scheduling
 ↓
render
 ↓
reconciliation
 ↓
commit
 ↓
DOM
 ↓
paint
```

The pieces we've covered conceptually are:

```text
✅ JSX
✅ React elements
✅ Components
✅ Props
✅ State
✅ Render
✅ Trigger
✅ Commit
✅ Batching
✅ State snapshots
```

The next step is the **core internal data structure that makes much of this possible**.

# Topic 5 — Reconciliation and the Diffing Algorithm

This is where we'll answer:

> **How does React determine what changed?**

We'll go deep into:

```text
old tree
   ↓
new tree
   ↓
reconciliation
   ↓
same type?
same key?
same position?
   ↓
reuse / update / insert / delete
```

We'll use concrete examples like:

```jsx
<div>
    <A />
    <B />
</div>
```

changing to:

```jsx
<div>
    <B />
    <A />
</div>
```

and:

```jsx
<li key={id}>
```

versus:

```jsx
<li key={index}>
```

and then connect the diffing process directly to **Fiber's `beginWork`, child reconciliation, flags, and commit operations**.

[1]: https://react.dev/learn/render-and-commit?utm_source=chatgpt.com "Render and Commit – React"
[2]: https://react.dev/learn/queueing-a-series-of-state-updates?utm_source=chatgpt.com "Queueing a Series of State Updates – React"
[3]: https://github.com/react/react/blob/main/packages/react-reconciler/README.md?utm_source=chatgpt.com "react/packages/react-reconciler/README.md at main · react/react · GitHub"
[4]: https://react.dev/reference/react/memo?utm_source=chatgpt.com "memo – React"

