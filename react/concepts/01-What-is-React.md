Absolutely. We’ll do this as a **proper React interview course**, not just a list of questions.

As of **September 30, 2026, React 19.3 is the latest major/minor release**, so I’ll teach modern React while also covering older concepts that still appear in interviews. ([React][1])

Recent EPAM interview reports also show that React interviews can go beyond API-level questions into **Hooks internals, Fiber, prioritization, SSR, performance/memoization, rendering, and architectural questions**. ([Glassdoor][2])

We’ll therefore focus on **“how React works”**, not merely “how to use React.”

---

# React Interview Course

We’ll cover these **one by one**. We move to the next topic only when you say **`next`**.

## Part 1 — React Fundamentals

### 1. What is React and why does it exist?

* Problems with traditional DOM manipulation
* Component-based UI
* Declarative vs imperative UI
* React's architecture
* `React` vs `ReactDOM`
* What actually happens when `createRoot()` is called
* React elements
* Component tree
* Render tree

### 2. JSX

* What JSX really is
* JSX → JavaScript transformation
* `jsx()`, `jsxs()`, `Fragment`
* JSX element objects
* Why JSX isn't HTML
* Expressions inside JSX
* Conditional rendering
* Why components must be capitalized
* JSX transform

### 3. Components

* Function components
* Class components
* Component identity
* Composition
* Props
* `children`
* Component purity
* Why React components are expected to behave like functions

### 4. Rendering

* What "render" actually means
* Initial render
* Re-render
* Render phase
* Commit phase
* Browser painting
* Why rendering doesn't necessarily modify the DOM
* What triggers a render

### 5. Virtual DOM

* What it actually is
* React elements vs Virtual DOM vs Fiber
* Why the term is often misunderstood
* Why React doesn't simply "compare two DOM trees"

---

# Part 2 — React Internals

### 6. Reconciliation

* Diffing
* Old tree vs new tree
* Element identity
* Same type / different type
* Host elements
* Component elements
* Lists
* Keys
* Why keys matter

### 7. React Fiber

This is one of the most important interview topics.

* Why Fiber was introduced
* Fiber nodes
* Fiber tree
* `child`
* `sibling`
* `return`
* `alternate`
* `pendingProps`
* `memoizedProps`
* `memoizedState`
* flags
* work-in-progress tree
* current tree
* double buffering
* incremental rendering

React's reconciler is implemented around Fiber, and the official repository describes the reconciler as the part responsible for coordinating rendering with a host environment. ([GitHub][3])

### 8. Scheduling and Concurrent React

* Why synchronous rendering caused problems
* Scheduling
* Priorities
* Lanes
* Interruptible rendering
* Cooperative scheduling
* Urgent vs non-urgent updates
* `startTransition`
* `useTransition`
* `useDeferredValue`
* Suspense
* Concurrent rendering

### 9. Render → Reconcile → Commit

We will trace an update all the way through React:

```text
event
  ↓
state update
  ↓
schedule update
  ↓
render work
  ↓
reconciliation
  ↓
Fiber tree
  ↓
commit
  ↓
DOM
  ↓
browser paint
```

React itself describes the user-visible process as **trigger → render → commit**, with rendering determining what the UI should look like and committing applying necessary changes to the host environment. ([React][4])

---

# Part 3 — State

### 10. `useState`

* Why state isn't simply a local variable
* Hook storage
* State queues
* Update queues
* Functional updates
* Batching
* Multiple `setState` calls
* Stale state
* State snapshots
* How React associates state with a component

### 11. State preservation

* Position in tree
* Component identity
* Keys
* Why state sometimes unexpectedly resets
* Why changing a key resets state

React explicitly associates state with a component's position/identity in the UI tree rather than with the JSX text itself. ([React][5])

### 12. Batching

* React 17 vs React 18+
* Automatic batching
* Event handlers
* Promises
* timers
* multiple state updates
* `flushSync`

---

# Part 4 — Hooks

### 13. Rules of Hooks

* Why hooks can't be called conditionally
* Hook ordering
* How React knows which state belongs to which hook

### 14. How Hooks work internally

One of the most important interview sections.

We'll conceptually implement:

```java
useState(...)
useEffect(...)
useRef(...)
useMemo(...)
useCallback(...)
```

using simplified code.

We'll understand why:

```javascript
useState(0);
useState(false);
useState("");
```

must always execute in the same order.

### 15. `useEffect`

* What it actually does
* Render vs Effect
* Dependency array
* Cleanup
* Mount/update/unmount behavior
* Strict Mode behavior
* Effect lifecycle
* Common mistakes
* When you don't need an Effect

React's current documentation emphasizes that Effects are for synchronizing with **external systems**, rather than being a general mechanism for deriving values during rendering. ([React][6])

### 16. `useLayoutEffect`

* Difference from `useEffect`
* Browser painting
* Measurement
* Timing

### 17. `useRef`

* Mutable container
* DOM references
* Why changing `ref.current` doesn't re-render
* `useRef` vs `useState`
* How refs are stored

### 18. `useMemo`

### 19. `useCallback`

### 20. `React.memo`

And importantly:

```text
memoization
    ≠
performance automatically improves
```

We'll understand when they help and when they just add complexity.

### 21. Context

* Context propagation
* Provider
* Consumers
* Re-render behavior
* Context vs props
* Context vs state management libraries

### 22. Custom Hooks

* What they really are
* Sharing logic vs sharing state
* Hook composition
* Designing reusable hooks

---

# Part 5 — Component Communication & State Architecture

### 23. Props

### 24. Parent → child

### 25. Child → parent

### 26. Sibling communication

### 27. Lifting state

### 28. Derived state

### 29. Controlled vs uncontrolled components

### 30. Local vs global state

---

# Part 6 — Forms & Events

### 31. Controlled forms

### 32. Uncontrolled forms

### 33. React events

### 34. Synthetic events

### 35. Event propagation

### 36. Event delegation

### 37. Form Actions in modern React

React 19 introduced substantial improvements around Actions and forms, including function-based `action` / `formAction`, `useActionState`, and `useOptimistic`. ([React][7])

---

# Part 7 — Performance

### 38. Why React applications become slow

### 39. Unnecessary re-renders

### 40. Component memoization

### 41. Stable references

### 42. Expensive calculations

### 43. Large lists

### 44. Virtualization

### 45. Code splitting

### 46. Lazy loading

### 47. Suspense

### 48. React Profiler

### 49. React Compiler

### 50. Performance architecture

The React Compiler reached stable v1.0 in 2025, so modern React interviews may increasingly test the relationship between compiler-driven optimization and manual memoization. ([React][1])

---

# Part 8 — Advanced Rendering

### 51. Reconciliation in depth

### 52. Keys in depth

### 53. Fragments

### 54. Portals

### 55. Error Boundaries

### 56. Suspense

### 57. Lazy components

### 58. Transitions

### 59. `Activity`

### 60. View Transitions

React 19.2 introduced `<Activity>`, which allows hidden parts of the UI to be kept around while deferring their updates; React 19.3 added features including View Transitions and Fragment Refs. ([React][8])

---

# Part 9 — SSR / Modern React Architecture

### 61. CSR

### 62. SSR

### 63. Hydration

### 64. Hydration mismatch

### 65. Streaming SSR

### 66. Server Components

### 67. Client Components

### 68. `"use client"`

### 69. `"use server"`

### 70. Server Actions

### 71. React Server Components architecture

React's documentation describes Server Components as components that render ahead of time in a separate environment before the client bundle is produced. ([React][9])

---

# Part 10 — React Architecture in Real Applications

### 72. Component architecture

### 73. Container vs presentational concepts

### 74. Feature-based structure

### 75. State management architecture

### 76. API/data-fetching architecture

### 77. Error handling

### 78. Loading states

### 79. Authentication

### 80. Authorization

### 81. Routing

### 82. Caching

### 83. Optimistic UI

### 84. Offline considerations

---

# Part 11 — Testing

### 85. Unit testing React

### 86. React Testing Library

### 87. Component testing

### 88. User-centric testing

### 89. Mocking

### 90. Async UI testing

---

# Part 12 — Security

### 91. XSS

### 92. `dangerouslySetInnerHTML`

### 93. React escaping

### 94. CSRF

### 95. Authentication tokens

### 96. Trusted Types

### 97. Security considerations with Server Components

---

# Part 13 — Practical Interview Questions

We'll finish by doing actual interview-style questions such as:

```text
Why does React re-render?

What exactly happens after setState()?

Why doesn't changing a normal variable update the UI?

Why does useState sometimes appear to have stale data?

Why must hooks be called in the same order?

How does React know which state belongs to which hook?

What is reconciliation?

What is Fiber?

What is the difference between Fiber and Virtual DOM?

Why are keys required?

Why should you not use array index as a key?

What causes unnecessary re-renders?

What is the difference between useMemo and useCallback?

When does React.memo actually work?

What is batching?

What is concurrent rendering?

What does startTransition do?

What is Suspense?

What is hydration?

What is SSR?

What are Server Components?

How does React update the DOM efficiently?
```

And we'll eventually do **implementation questions**, for example:

> "Implement a simplified `useState`."

> "Implement a simplified `useEffect`."

> "Explain how React could reconcile two trees."

> "Design a simplified Fiber tree."

> "Why does this component re-render?"

> "Predict exactly how many times this component renders."

That level of understanding is particularly useful for senior-level React interviews; recent EPAM reports include questions about React Hooks, Fiber, prioritization, performance, SSR, memoization and how React works under the hood. ([Glassdoor][2])

---

# Topic 1 — What Actually Is React?

Let's start from the foundation.

## 1. The problem React was designed to solve

Suppose we have:

```html
<div id="counter">
    <h1>0</h1>
    <button>+</button>
</div>
```

When the user clicks:

```text
+
```

we need the UI to change from:

```html
<h1>0</h1>
```

to:

```html
<h1>1</h1>
```

With plain JavaScript, we could manually manipulate the DOM:

```javascript
button.addEventListener("click", () => {
    count++;

    document
        .querySelector("#counter h1")
        .textContent = count;
});
```

This works.

But imagine a real application:

```text
Application
 ├── Header
 ├── Sidebar
 ├── Products
 │    ├── Product
 │    ├── Product
 │    ├── Product
 │    └── ...
 ├── Cart
 ├── Notifications
 └── Footer
```

Now one user's action can affect:

* product state
* cart state
* totals
* badges
* notifications
* loading states
* animations
* filters
* URLs
* multiple components

Manually deciding **which DOM nodes need to be changed and in what order** becomes difficult.

---

# 2. Imperative vs declarative UI

This is one of the most fundamental React concepts.

### Imperative

You tell the browser:

```text
1. Find this DOM element
2. Change its text
3. Add this class
4. Remove that element
5. Create another element
6. Attach this event listener
```

Example:

```javascript
const title = document.getElementById("title");

title.textContent = "Logged in";
title.classList.add("success");
```

You're describing **how to modify the UI**.

---

### Declarative

With React:

```jsx
function App() {
    return (
        <h1 className="success">
            Logged in
        </h1>
    );
}
```

You describe:

> "For this state, this is what my UI should look like."

Conceptually:

```text
State
  ↓
UI
```

For example:

```javascript
const isLoggedIn = true;
```

produces:

```jsx
return isLoggedIn
    ? <Dashboard />
    : <Login />;
```

You don't explicitly say:

```text
remove Login
create Dashboard
move Dashboard into this DOM location
attach these events
...
```

React handles those details.

---

# 3. React is not the DOM

Very important interview distinction.

React itself is **not the browser DOM**.

Think of the system roughly as:

```text
                 React
                   │
          ┌────────┴─────────┐
          │                  │
      React Core        Reconciler
                             │
                    Host Renderer
                             │
                    ┌────────┴───────┐
                    │                │
                 React DOM       React Native
                    │                │
                  Browser          Mobile
```

The same React concepts can be used with different rendering targets.

For web applications, `react-dom` is responsible for connecting React to the browser DOM.

React's reconciler documentation explicitly describes the renderer as providing the host-environment-specific operations—for example, creating actual DOM nodes. ([GitHub][3])

This is why React can be used for:

```text
React
 ├── Web
 ├── React Native
 ├── custom renderers
 └── other host environments
```

---

# 4. What does a React component actually return?

Consider:

```jsx
function App() {
    return <h1>Hello</h1>;
}
```

Many beginners imagine:

```text
App()
   ↓
real DOM <h1>
```

That's not what happens.

Conceptually, React first gets a **description of the UI**.

For example:

```javascript
{
    type: "h1",
    props: {
        children: "Hello"
    }
}
```

This is a simplified representation.

That object is commonly referred to as a **React element**.

So:

```jsx
<h1>Hello</h1>
```

is not itself a DOM node.

It is syntax that gets transformed into JavaScript which creates a React element.

---

# 5. JSX is not HTML

We'll study JSX properly in Topic 2, but understand this distinction now.

This:

```jsx
<h1>Hello</h1>
```

is essentially transformed into something conceptually like:

```javascript
jsx("h1", {
    children: "Hello"
});
```

The exact generated code depends on the JSX transform.

React 19 requires the modern JSX transform for the new JSX behavior and improvements. ([React][10])

So:

```text
JSX
 ↓
JavaScript
 ↓
React element
```

Not:

```text
JSX
 ↓
DOM
```

---

# 6. React's mental model

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

Conceptually React sees something like:

```text
App
 │
 └── div
      ├── h1
      └── button
```

This forms a tree.

React's documentation explicitly models UI as trees and calls the component relationship a **render tree**. ([React][11])

---

# 7. What happens when React starts?

Typical application startup:

```javascript
const root = ReactDOM.createRoot(
    document.getElementById("root")
);

root.render(<App />);
```

Let's slow down.

---

## Step 1 — Create the root

```javascript
ReactDOM.createRoot(container)
```

React creates/initializes a root associated with the host container.

Conceptually:

```text
DOM container
     │
     ▼
React Root
```

---

## Step 2 — Request rendering

```javascript
root.render(<App />);
```

React receives:

```text
<App />
```

which represents the requested UI tree.

Conceptually:

```text
App
 │
 ├── Header
 ├── Main
 └── Footer
```

---

# 8. React doesn't immediately "change the DOM"

This is one of the most important concepts.

Think of React's process as:

```text
          update requested
                │
                ▼
          Render phase
                │
                ▼
        determine what
        should exist
                │
                ▼
          Reconciliation
                │
                ▼
          Commit phase
                │
                ▼
          Host environment
                │
                ▼
             DOM
```

React's current documentation describes the process as:

```text
Trigger
   ↓
Render
   ↓
Commit
```

and explicitly points out that a render does **not necessarily mean a DOM change**. ([React][4])

---

# 9. Render phase

Suppose:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    return <h1>{count}</h1>;
}
```

Initially:

```text
count = 0
```

React executes the component:

```javascript
Counter()
```

and obtains:

```jsx
<h1>0</h1>
```

Conceptually:

```text
Component execution
        ↓
React elements
        ↓
Fiber work
        ↓
reconciliation
```

The render phase is primarily about figuring out **what the next UI should be**.

It should therefore be pure.

For example, this is problematic:

```jsx
function Counter() {
    document.body.style.background = "red";

    return <h1>Hello</h1>;
}
```

Why?

Because React may perform render work without immediately committing it, especially with modern concurrent rendering.

So rendering must not rely on:

```text
"this render definitely reaches the screen"
```

We'll explore this deeply when we reach rendering and concurrent React.

---

# 10. Commit phase

After React has determined what needs to change, React commits those changes to the host environment.

For React DOM:

```text
React
 ↓
React DOM
 ↓
DOM APIs
```

Conceptually:

```javascript
const node = document.createElement("h1");
node.textContent = "Hello";

container.appendChild(node);
```

The actual React implementation is considerably more sophisticated, but that is the basic idea.

---

# 11. Why have a separate render and commit phase?

This is a key architectural idea.

Imagine React rendering a huge application:

```text
1,000,000 nodes
```

If React had to synchronously:

```text
calculate entire UI
+
modify DOM
```

in one indivisible operation, the browser could become unresponsive.

Modern React separates:

```text
"What should the UI look like?"
```

from:

```text
"Apply these changes."
```

This architectural separation is one of the foundations for interruptible/concurrent rendering.

---

# 12. Where does Fiber enter?

This is where React becomes interesting internally.

React needs a data structure that represents work.

That's the purpose of **Fiber**.

A highly simplified Fiber might look like:

```javascript
const fiber = {
    type: Counter,
    key: null,

    child: ...,
    sibling: ...,
    return: ...,

    pendingProps: ...,
    memoizedProps: ...,
    memoizedState: ...,

    alternate: ...
};
```

Don't worry about memorizing every field yet.

The important idea is:

> A Fiber is an internal unit of work representing a node in React's tree.

---

# 13. Fiber is NOT the same thing as Virtual DOM

This is a very common interview trap.

People often say:

```text
React = Virtual DOM
```

and:

```text
Fiber = Virtual DOM
```

That's an oversimplification.

Think:

```text
React Element
     │
     │ describes UI
     ▼
Fiber
     │
     │ represents React's internal work/tree
     ▼
Host tree
     │
     ▼
DOM
```

The terms solve different problems.

### React Element

A description:

```javascript
{
    type: "button",
    props: {...}
}
```

### Fiber

React's internal representation of work and component identity/state relationships.

### DOM

Actual browser objects:

```javascript
HTMLButtonElement
HTMLDivElement
...
```

---

# 14. Why did Fiber exist?

Before Fiber, React's reconciliation architecture was more synchronous.

The idea was essentially:

```text
start update
   ↓
walk tree
   ↓
finish tree
   ↓
commit
```

For a large tree, that could monopolize the main thread.

Fiber introduced the ability to structure rendering work as smaller units.

Conceptually:

```text
Fiber A
 ↓
Fiber B
 ↓
Fiber C
 ↓
pause
 ↓
browser gets time
 ↓
resume
 ↓
Fiber D
 ↓
Fiber E
```

The important word is:

**incremental rendering.**

The original Fiber architecture documentation describes its goals as incremental rendering, the ability to pause/abort/reuse work, and prioritizing updates. ([Gist][12])

We'll eventually go much deeper into this.

---

# 15. The current tree and the work-in-progress tree

Another interview-level concept.

React can conceptually maintain:

```text
Current Tree
     │
     │ currently committed UI
     ▼
   Browser
```

while constructing:

```text
Work-In-Progress Tree
     │
     │ next UI
     ▼
being calculated
```

Conceptually:

```text
             React
               │
       ┌───────┴────────┐
       │                │
   Current           WIP tree
    tree              tree
       │                │
       ▼                ▼
 current UI        next UI
```

After successful rendering/commit, the trees effectively switch roles.

This is often called **double buffering**.

The `alternate` relationship on Fiber nodes is central to this model.

---

# 16. Re-rendering does NOT mean "reload everything"

Suppose:

```jsx
function App() {
    const [count, setCount] = useState(0);

    return (
        <>
            <h1>{count}</h1>
            <button onClick={() => setCount(count + 1)}>
                +
            </button>
        </>
    );
}
```

Click the button.

We conceptually get:

```text
setCount(1)
   ↓
update scheduled
   ↓
App renders again
   ↓
new React element tree
   ↓
reconciliation
   ↓
React determines required changes
   ↓
commit
```

React does **not** blindly execute:

```text
delete entire DOM
create entire DOM again
```

It determines what actually changed.

For:

```text
<h1>0</h1>
```

to:

```text
<h1>1</h1>
```

the browser doesn't need a brand-new `<h1>` element merely because the component function ran again.

This distinction is critical:

```text
Component re-render
        ≠
DOM replacement
```

---

# 17. Three different "trees" you need to know

This becomes extremely useful later.

### Component/render tree

```text
App
 ├── Header
 ├── ProductList
 │    ├── Product
 │    └── Product
 └── Footer
```

### Fiber tree

React's internal representation of the work/state relationships.

### DOM tree

```text
html
 └── body
      └── div#root
           ├── header
           ├── main
           └── footer
```

Don't mix these together.

React's documentation also distinguishes render trees from module dependency trees; the render tree describes rendered component relationships, while the dependency tree describes imported modules. ([React][11])

---

# 18. Why React state "belongs" to the tree

Consider:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    return <h1>{count}</h1>;
}
```

It is tempting to think:

```text
count belongs to Counter()
```

But a better mental model is:

```text
React owns the state
        │
        ▼
state is associated with
a particular component identity
in a particular tree position
```

That's why this works:

```jsx
<>
    <Counter />
    <Counter />
</>
```

You get:

```text
Counter #1 → state #1
Counter #2 → state #2
```

even though both execute the same component function.

React's documentation explicitly explains state preservation in terms of the component's position/identity in the tree. ([React][5])

This concept becomes essential when we study:

```text
useState
keys
reconciliation
Fiber
hooks
```

---

# 19. The most important mental model

When learning React, stop thinking primarily:

```text
React manipulates the DOM.
```

Instead think:

```text
Application state
      ↓
Component execution
      ↓
React elements
      ↓
Fiber / reconciliation
      ↓
Determine changes
      ↓
Commit
      ↓
DOM
```

Then after a user interaction:

```text
User interaction
      ↓
state update
      ↓
schedule work
      ↓
render
      ↓
reconcile
      ↓
commit
      ↓
updated UI
```

This mental model will make most of React much easier.

---

# 20. Interview questions from Topic 1

These are questions I expect you to be able to answer after this lesson.

### Basic

**Q1. What is React?**

Good answer:

> React is a JavaScript library for building user interfaces using a declarative, component-based programming model. Instead of manually manipulating the DOM for every state change, we describe the UI for a given state and React determines and applies the necessary updates.

---

**Q2. What problem does React solve?**

You should explain:

```text
complex UI state
      ↓
manual DOM synchronization becomes difficult
      ↓
React lets us describe UI declaratively
      ↓
React handles reconciliation and updates
```

---

**Q3. What is the Virtual DOM?**

Don't answer:

> "A lightweight copy of the real DOM."

That answer is incomplete.

A better interview response:

> The term Virtual DOM is commonly used for React's in-memory representation of the UI. React creates React elements during rendering and uses its reconciliation process, backed by Fiber, to determine what changes are needed before committing updates to the host environment.

---

**Q4. What is Fiber?**

> Fiber is React's internal architecture/data structure for representing units of rendering work and maintaining relationships between nodes in the React tree. It enables React to schedule and process rendering work incrementally and supports modern concurrent rendering behavior.

---

**Q5. Does every React render update the DOM?**

**No.**

A render can produce the same result:

```text
previous UI
    =
next UI
```

Then React may have no DOM mutations to commit.

This is why:

```text
render ≠ DOM update
```

React explicitly makes this distinction in its rendering documentation. ([React][4])

---

**Q6. What are the render and commit phases?**

```text
Render phase
    ↓
calculate next UI / reconcile work

Commit phase
    ↓
apply required host environment changes
```

For web applications, that ultimately means DOM updates.

---

**Q7. Is React only for the browser?**

No.

React's core architecture is separated from the host renderer, which is why React can target different environments. React DOM is the renderer for the browser; React Native has its own host environment/rendering implementation. ([GitHub][3])

---

# 21. What I want you to remember from Topic 1

Keep this picture in your head:

```text
                USER
                 │
                 ▼
             interaction
                 │
                 ▼
            state update
                 │
                 ▼
             React work
                 │
                 ▼
          ┌───────────────┐
          │ Render phase  │
          │               │
          │ Components    │
          │   ↓           │
          │ Elements      │
          │   ↓           │
          │ Reconciliation│
          │   ↓           │
          │ Fiber work    │
          └───────┬───────┘
                  │
                  ▼
          ┌───────────────┐
          │ Commit phase  │
          └───────┬───────┘
                  │
                  ▼
                DOM
                  │
                  ▼
              Browser
```

And remember these distinctions:

```text
JSX
 ↓
React Element
 ↓
Fiber
 ↓
DOM
```

with each layer serving a different purpose.

---

## Interview depth target

By the end of this course, I want you to be able to take code such as:

```jsx
function App() {
    const [count, setCount] = useState(0);

    return (
        <div>
            <h1>{count}</h1>
            <button onClick={() => setCount(count + 1)}>
                Increment
            </button>
        </div>
    );
}
```

and explain **exactly what happens internally** from:

```text
click
→ event
→ state update
→ update queue
→ scheduling
→ Fiber
→ render
→ reconciliation
→ commit
→ DOM mutation
→ browser paint
```

That is the level we'll target rather than memorizing definitions.

**Next topic: JSX — including how JSX is transformed into JavaScript and how React elements are actually created.**

[1]: https://react.dev/blog?utm_source=chatgpt.com "React Blog – React"
[2]: https://www.glassdoor.co.in/Interview/EPAM-Systems-Javascript-Developer-Interview-Questions-EI_IE15544.0%2C12_KO13%2C33.htm?utm_source=chatgpt.com "EPAM Systems Javascript Developer Interview Questions | Glassdoor"
[3]: https://github.com/react/react/blob/main/packages/react-reconciler/README.md?utm_source=chatgpt.com "react/packages/react-reconciler/README.md at main · react/react · GitHub"
[4]: https://react.dev/learn/render-and-commit?utm_source=chatgpt.com "Render and Commit – React"
[5]: https://react.dev/learn/preserving-and-resetting-state?utm_source=chatgpt.com "Preserving and Resetting State – React"
[6]: https://react.dev/learn/synchronizing-with-effects?utm_source=chatgpt.com "Synchronizing with Effects – React"
[7]: https://react.dev/blog/2024/12/05/react-19?utm_source=chatgpt.com "React v19 – React"
[8]: https://react.dev/blog/2025/10/01/react-19-2?utm_source=chatgpt.com "React 19.2 – React"
[9]: https://react.dev/reference/rsc/server-components?utm_source=chatgpt.com "Server Components – React"
[10]: https://react.dev/blog/2024/04/25/react-19-upgrade-guide?utm_source=chatgpt.com "React 19 Upgrade Guide – React"
[11]: https://react.dev/learn/understanding-your-ui-as-a-tree?utm_source=chatgpt.com "Understanding Your UI as a Tree – React"
[12]: https://gist.github.com/xyhp915/4b6767cb6f109875b5afc5e14a20e10a?utm_source=chatgpt.com "React Fiber Architecture · GitHub"

