# Topic 3 — React Components in Depth

Components are the **core abstraction of React**.

A lot of interview questions that appear to be about Hooks, state, rendering, memoization, or reconciliation eventually reduce to one question:

> **What exactly is a React component, and how does React treat it internally?**

Let's build that from first principles.

---

# 1. What is a React component?

At the simplest level:

```jsx
function Welcome() {
    return <h1>Hello</h1>;
}
```

`Welcome` is a React component.

We can use it as:

```jsx
<Welcome />
```

Conceptually, a component is:

> A reusable piece of UI logic that React can render as part of a UI tree.

But that's still too high-level for an interview.

Internally, you should think more like:

```text
Component definition
       ↓
React uses it during rendering
       ↓
component is invoked
       ↓
returns React elements
       ↓
React builds/updates Fiber
       ↓
host environment is updated
```

---

# 2. A function component is just a JavaScript function

Consider:

```jsx
function Welcome() {
    return <h1>Hello</h1>;
}
```

At the JavaScript level, this is simply a function.

You can technically call it:

```javascript
Welcome();
```

and it returns something.

But there is an important difference between:

```javascript
Welcome();
```

and:

```jsx
<Welcome />
```

This is a very important interview point.

---

# 3. `<Welcome />` does NOT mean "call Welcome yourself"

When React sees:

```jsx
<Welcome />
```

JSX conceptually becomes something like:

```javascript
jsx(Welcome, {})
```

Notice:

```text
type = Welcome
```

React now has a **React element** whose type points to the component function.

Conceptually:

```javascript
{
    type: Welcome,
    props: {}
}
```

React can then process that element during rendering.

So:

```text
<Welcome />
```

means roughly:

```text
"React, render the component whose type is Welcome"
```

rather than:

```text
"JavaScript, immediately execute Welcome()"
```

---

# 4. Why is this distinction important?

Because React controls **when and how** the component function executes.

Suppose:

```jsx
function App() {
    return <Welcome />;
}
```

During rendering, React encounters:

```text
App
 ↓
Welcome
```

React can:

* render `Welcome`
* render it again later
* delay work
* restart render work
* abandon an unfinished render
* prioritize another update

Therefore, your mental model should be:

```text
React owns component execution during rendering.
```

This is one reason component render logic needs to be pure.

---

# 5. Function component execution

Consider:

```jsx
function User() {
    const name = "Alice";

    return <h1>{name}</h1>;
}
```

Conceptually, when React needs to render `User`:

```text
React
  ↓
invoke User
  ↓
User executes
  ↓
returns React element
  ↓
React processes returned element
```

Something conceptually like:

```javascript
const returnedElement = User();
```

But **React's actual implementation is not simply "call the function and stop."**

React wraps component rendering in its own rendering machinery because it needs to handle:

* Hooks
* context
* error handling
* Strict Mode behavior
* profiler information
* render-phase updates
* reconciliation
* priority
* suspension
* development checks

We'll get into those internals later.

---

# 6. Component tree

Suppose:

```jsx
function App() {
    return (
        <Page>
            <Header />
            <Main />
            <Footer />
        </Page>
    );
}
```

We can visualize:

```text
App
 │
 └── Page
      ├── Header
      ├── Main
      └── Footer
```

This is a **component hierarchy** / render tree.

Each component can return:

* a host element
* another component
* multiple children through a Fragment
* text
* `null`
* other renderable React values

---

# 7. Components vs DOM elements

Compare:

```jsx
<div>Hello</div>
```

with:

```jsx
<Welcome />
```

These are fundamentally different.

### `div`

```text
type = "div"
```

React knows this represents a host element.

### `Welcome`

```text
type = Welcome
```

React knows this is a user-defined component.

So conceptually:

```text
<Welcome />
     ↓
Function Component

<div />
     ↓
Host Component
```

The distinction becomes very important inside the Fiber tree.

---

# 8. Function components return descriptions, not DOM nodes

Suppose:

```jsx
function Welcome() {
    return <h1>Hello</h1>;
}
```

The function isn't returning:

```javascript
document.createElement("h1");
```

It returns a React element.

Conceptually:

```javascript
{
    type: "h1",
    props: {
        children: "Hello"
    }
}
```

So:

```text
Welcome()
   ↓
React element
   ↓
React processes it
   ↓
DOM eventually created/updated
```

---

# 9. Components compose other components

One of React's biggest strengths is composition.

```jsx
function App() {
    return (
        <>
            <Header />
            <ProductList />
            <Footer />
        </>
    );
}
```

Each component can focus on a particular responsibility.

Think:

```text
App
 ├── Header
 ├── ProductList
 └── Footer
```

This allows large UIs to be constructed from smaller pieces.

---

# 10. Composition vs inheritance

React strongly favors **composition** rather than component inheritance.

Instead of building:

```text
BaseComponent
    ↓
AdvancedComponent
    ↓
SpecializedComponent
```

you generally compose:

```jsx
<Page>
    <Header />
    <Sidebar />
    <Content />
</Page>
```

This is related to how React uses `children` and props.

---

# 11. `children` and composition

Consider:

```jsx
function Card({ children }) {
    return (
        <div className="card">
            {children}
        </div>
    );
}
```

Usage:

```jsx
<Card>
    <h2>Hello</h2>
    <p>Welcome</p>
</Card>
```

Conceptually:

```text
Card
 └── children
      ├── h2
      └── p
```

The parent controls the structure:

```jsx
<Card>
   ...
</Card>
```

while `Card` controls the outer container.

This is composition.

---

# 12. Props

Props are inputs to a component.

Example:

```jsx
function User({ name, age }) {
    return (
        <div>
            {name} - {age}
        </div>
    );
}
```

Usage:

```jsx
<User name="Alice" age={25} />
```

Conceptually, React supplies:

```javascript
{
    name: "Alice",
    age: 25
}
```

to the component.

You can think of it as:

```text
Parent
   │
   │ props
   ▼
Child component
```

---

# 13. Props are read-only from the component's perspective

Consider:

```jsx
function User(props) {
    props.name = "Bob"; // bad
}
```

A component should not mutate its received props.

Why?

Because props represent input controlled by the parent.

Conceptually:

```text
Parent state
    ↓
props
    ↓
Child
```

The child should use those inputs rather than modify the parent's source of truth.

---

# 14. Props are not state

This is an extremely common interview question.

### Props

Data passed into a component.

```text
parent → child
```

### State

Data managed by React for a component/tree position.

```text
component
   ↕
React-managed state
```

For example:

```jsx
function Counter({ initialValue }) {
    const [count, setCount] = useState(initialValue);
}
```

Here:

```text
initialValue → prop
count        → state
```

They serve different purposes.

---

# 15. Component identity

Now we get into more advanced React behavior.

Suppose:

```jsx
function Counter() {
    const [count, setCount] = useState(0);

    return <h1>{count}</h1>;
}
```

React needs to answer:

> "Which `Counter` instance does this state belong to?"

The answer isn't:

```text
the function Counter itself
```

because we can render it multiple times.

For:

```jsx
<>
    <Counter />
    <Counter />
</>
```

we have:

```text
Counter #1 → state A
Counter #2 → state B
```

So component identity comes from the component's place/identity in the rendered tree, together with its type and key where relevant.

---

# 16. Same component function, different state

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

Then:

```jsx
function App() {
    return (
        <>
            <Counter />
            <Counter />
        </>
    );
}
```

We have:

```text
App
 ├── Counter
 │    └── state = 0
 │
 └── Counter
      └── state = 0
```

Click the first:

```text
App
 ├── Counter
 │    └── state = 1
 │
 └── Counter
      └── state = 0
```

The second isn't affected.

This is because React doesn't think:

```text
"There's only one Counter function, so there's only one state."
```

Instead, React tracks state for the component's specific position/identity in its tree.

This becomes crucial when we study Hooks and Fiber.

---

# 17. Function components are not class instances

With classes:

```jsx
class Counter extends React.Component {
    state = {
        count: 0
    };

    render() {
        return <h1>{this.state.count}</h1>;
    }
}
```

there is an object instance:

```text
Counter instance
     ↓
this.state
     ↓
this.props
```

With function components:

```jsx
function Counter() {
    const [count, setCount] = useState(0);
}
```

there isn't a persistent JavaScript object instance corresponding to:

```text
this
```

for each render.

Instead, React stores the component's persistent state in its internal structures.

This is a fundamental conceptual difference.

---

# 18. A function component executes again

Suppose:

```jsx
function Counter() {
    console.log("render");

    const [count, setCount] = useState(0);

    return (
        <button onClick={() => setCount(count + 1)}>
            {count}
        </button>
    );
}
```

When state changes, React may execute:

```text
Counter()
```

again.

So the JavaScript local variables are recreated:

```text
Render 1:
count = 0

Render 2:
count = 1
```

This often surprises beginners.

They may think:

```text
"How does count survive if the function runs again?"
```

The answer:

> The state doesn't live in the local variable itself. React stores the state outside that particular function invocation and provides the appropriate value during each render.

This is a foundational Hook concept.

---

# 19. Very important distinction

Don't think:

```text
useState()
     ↓
persistent JavaScript variable
```

Instead:

```text
React internal state
      ↓
render component
      ↓
provide current state value
      ↓
function local variable
```

So:

```javascript
const [count, setCount] = useState(0);
```

means conceptually:

```text
React:
"Here's the state value for this component/hook position."
```

We'll eventually implement a simplified version.

---

# 20. Component purity

React components should generally behave like pure functions of their inputs.

Conceptually:

```text
UI = f(props, state, context)
```

For the same inputs, rendering should produce the same result.

For example:

```jsx
function User({ name }) {
    return <h1>{name}</h1>;
}
```

is conceptually pure.

But:

```jsx
function User({ name }) {
    document.title = name;

    return <h1>{name}</h1>;
}
```

performs a side effect during rendering.

That's problematic because React may render more than once, render without committing, or restart rendering.

Side effects should generally happen in appropriate lifecycle mechanisms, event handlers, or Effects depending on what you're synchronizing.

---

# 21. Why purity matters more in modern React

Suppose React starts rendering:

```text
Large update
    ↓
Fiber A
    ↓
Fiber B
    ↓
Fiber C
```

Then an urgent update appears.

React may decide:

```text
pause current work
handle urgent work
resume/restart other work
```

That means render code cannot safely assume:

```text
"This function runs exactly once before its result is displayed."
```

Therefore this is dangerous:

```jsx
function Component() {
    sendPayment();
    return <div />;
}
```

because rendering isn't the place to perform an irreversible action.

---

# 22. Component rendering is not the same as mounting

These terms get confused a lot.

### Render

React calculates UI.

### Mount

A component/host subtree is being introduced into the tree for the first time.

### Update

An existing tree is being reconciled against new inputs/state.

### Unmount

A component/subtree is removed.

So:

```text
render
```

doesn't necessarily mean:

```text
mount
```

A component can render multiple times while staying mounted.

Example:

```text
Mount:
Counter renders

Update:
Counter renders

Update:
Counter renders

Update:
Counter renders

Unmount:
Counter removed
```

---

# 23. Render vs re-render

People often say:

> "React re-renders the DOM."

That's misleading.

A better explanation:

```text
Component re-renders
        ↓
component function executes again
        ↓
React produces next element descriptions
        ↓
reconciliation determines changes
        ↓
commit performs necessary host updates
```

Therefore:

```text
component re-render
≠
DOM fully recreated
```

---

# 24. Parent renders: does child always render?

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

When `count` changes:

```text
Parent renders again
```

What about:

```jsx
<Child />
```

By default, React can re-render the child as part of processing the parent subtree.

This is why unnecessary parent renders can result in unnecessary child work.

Later we'll study:

```text
React.memo
memoization
component boundaries
state placement
context
```

to understand how to control this.

---

# 25. Parent → child data flow

React's normal data flow is:

```text
Parent
   │
   │ props
   ▼
Child
```

Example:

```jsx
function Parent() {
    const name = "Alice";

    return <Child name={name} />;
}

function Child({ name }) {
    return <h1>{name}</h1>;
}
```

So:

```text
Parent state/data
       ↓
      props
       ↓
     Child
```

This is usually called **one-way data flow**.

---

# 26. Child → parent communication

A child doesn't normally mutate parent state directly.

Instead, the parent can pass a function:

```jsx
function Parent() {
    const handleSave = () => {
        console.log("save");
    };

    return <Child onSave={handleSave} />;
}
```

Then:

```jsx
function Child({ onSave }) {
    return (
        <button onClick={onSave}>
            Save
        </button>
    );
}
```

Conceptually:

```text
Parent
  │
  │ function prop
  ▼
Child
  │
  │ invokes function
  ▼
Parent logic
```

This preserves the one-way data flow.

---

# 27. What does React actually store for a component?

Now let's move toward Fiber.

Suppose:

```jsx
function Counter({ step }) {
    const [count, setCount] = useState(0);

    return (
        <button>
            {count + step}
        </button>
    );
}
```

React needs to maintain information about this component, including concepts such as:

```text
type
props
state
hooks
child relationship
parent relationship
sibling relationship
identity
pending updates
effects
rendering flags
```

This information lives in React's internal Fiber representation.

A highly simplified Fiber might resemble:

```javascript
const fiber = {
    type: Counter,

    pendingProps: {
        step: 1
    },

    memoizedProps: {
        step: 1
    },

    memoizedState: /* hook state */,

    child: /* returned subtree */,

    sibling: /* next sibling */,

    return: /* parent */,

    alternate: /* other tree */
};
```

This is **not the exact source structure**, but it's a useful mental model.

---

# 28. Why doesn't React just store everything inside the function?

Because the function invocation is temporary.

Imagine:

```javascript
function Counter() {
    const x = ...
}
```

After:

```text
Counter()
```

returns, that invocation's local variables aren't the mechanism React uses to retain state between renders.

React needs persistent internal structures.

Conceptually:

```text
                    React
                      │
                    Fiber
                      │
              ┌───────┴───────┐
              │               │
           props             state
                              │
                            Hooks
```

The function invocation reads from those structures.

---

# 29. Why can't we use ordinary variables for UI state?

Consider:

```jsx
let count = 0;

function Counter() {
    return (
        <button onClick={() => {
            count++;
            console.log(count);
        }}>
            {count}
        </button>
    );
}
```

The variable changes:

```text
0 → 1
```

but React isn't informed that the component's UI needs updating.

The missing piece is:

```text
schedule React update
```

`useState` gives you an update mechanism:

```jsx
const [count, setCount] = useState(0);
```

Calling:

```jsx
setCount(1);
```

tells React:

```text
"There's an update for this component/tree position."
```

That causes React to schedule rendering work.

---

# 30. Component identity and keys

Consider a list:

```jsx
items.map(item => (
    <Counter key={item.id} />
))
```

The key helps React determine which child corresponds to which existing child between renders.

For example:

```text
Before:

A
B
C
```

then:

```text
B
A
C
```

Keys tell React:

```text
this is the same logical child as before
```

rather than identifying children merely by their current array position.

This is central to reconciliation.

We'll study keys and reconciliation in much greater depth later.

---

# 31. Changing component type can reset state

Consider:

```jsx
{isAdmin ? <Admin /> : <User />}
```

The component at this position changes from:

```text
Admin
```

to:

```text
User
```

These are different component types.

React may therefore treat the previous subtree as one identity and the new subtree as another.

Conceptually:

```text
position X

Admin
 ↓
state associated with Admin

then

User
 ↓
different identity
 ↓
previous Admin state doesn't carry over
```

This is an important consequence of reconciliation and component identity.

---

# 32. Keys can deliberately reset state

Suppose:

```jsx
<Chat key={userId} userId={userId} />
```

If:

```text
userId = 101
```

then:

```text
Chat key = 101
```

Later:

```text
userId = 202
```

The key changes.

React now treats the component as a different identity:

```text
Chat(101)
    ↓
state A

Chat(202)
    ↓
state B
```

This is a useful technique when you intentionally want state to reset.

---

# 33. Component function vs component instance

For function components:

```text
function Counter() {}
```

there isn't a long-lived class instance equivalent to:

```javascript
new Counter()
```

React instead maintains the persistent data associated with that component in its own internal structures.

So avoid saying:

> "React creates one object instance of every function component."

That's not the right mental model.

---

# 34. Class components

We still need to understand them because interviewers may ask about legacy React.

Example:

```jsx
class User extends React.Component {
    render() {
        return <h1>{this.props.name}</h1>;
    }
}
```

Class components have:

```text
instance
    ↓
this.props
this.state
methods
```

React creates/manages the class instance.

Function components instead rely on:

```text
function invocation
+
React-managed internal state
+
Hooks
```

Today, function components and Hooks are the standard approach for new React code, but understanding classes remains useful for maintenance and interview questions.

---

# 35. Why Hooks changed component design

Old style:

```jsx
class User extends React.Component {
    constructor() {
        ...
    }

    componentDidMount() {
        ...
    }

    componentDidUpdate() {
        ...
    }

    render() {
        ...
    }
}
```

Modern style:

```jsx
function User() {
    const [data, setData] = useState(...);

    useEffect(() => {
        ...
    }, []);

    return ...;
}
```

Hooks allow React to associate reusable stateful logic with function components.

But here's the important conceptual point:

> Hooks didn't make the component itself stateful in the JavaScript-function sense. React still owns the persistent state.

---

# 36. Why React components should be small

This isn't a strict technical requirement.

It's an architectural principle.

Instead of:

```text
MassiveComponent
 ├── fetching
 ├── validation
 ├── rendering
 ├── modal
 ├── table
 ├── filtering
 ├── pagination
 └── form
```

we can compose:

```text
Page
 ├── Header
 ├── Search
 ├── Filters
 ├── ProductTable
 │    └── ProductRow
 ├── Pagination
 └── Modal
```

This improves:

* reuse
* testing
* reasoning
* isolation
* performance tuning
* maintainability

We'll later distinguish good component boundaries from arbitrary fragmentation.

---

# 37. Don't split components just because they are long

Another interview/design question.

Suppose:

```jsx
function Checkout() {
    ...
}
```

is 500 lines.

That doesn't automatically mean:

```text
"Create 20 components."
```

A better question is:

> "Is there a meaningful responsibility or reusable boundary here?"

For example:

```text
Checkout
 ├── ShippingAddress
 ├── PaymentMethod
 ├── OrderSummary
 └── SubmitOrder
```

Those boundaries have semantic meaning.

Componentization should help the architecture rather than merely reduce line count.

---

# 38. A component can return another component

Example:

```jsx
function Page() {
    return <Dashboard />;
}
```

This means:

```text
Page
 ↓
Dashboard
```

Then:

```jsx
function Dashboard() {
    return <MainContent />;
}
```

gives:

```text
Page
 ↓
Dashboard
 ↓
MainContent
```

React follows the returned element structure recursively while rendering.

---

# 39. A component can return `null`

Example:

```jsx
function ProtectedContent({ authorized }) {
    if (!authorized) {
        return null;
    }

    return <Dashboard />;
}
```

The component participates in React's tree but renders no host content for that path.

This is useful for conditional rendering.

---

# 40. A component can return a Fragment

```jsx
function User() {
    return (
        <>
            <h1>Alice</h1>
            <p>Engineer</p>
        </>
    );
}
```

Conceptually:

```text
User
 ↓
Fragment
 ├── h1
 └── p
```

No extra DOM wrapper is required.

---

# 41. Props are evaluated before the component receives them

Consider:

```jsx
<User age={20 + 5} />
```

The JavaScript expression:

```javascript
20 + 5
```

evaluates to:

```text
25
```

Then conceptually:

```text
User receives:
{
    age: 25
}
```

Likewise:

```jsx
<User name={user.name} />
```

means the expression is evaluated as part of producing the element.

---

# 42. The component doesn't "own" its props

Suppose:

```jsx
function Parent() {
    const user = {
        name: "Alice"
    };

    return <Child user={user} />;
}
```

The parent owns the `user` value.

The child receives:

```text
props.user
```

The child should not mutate it:

```jsx
props.user.name = "Bob"; // bad design
```

because now you've created shared mutable state and broken the normal data-flow expectations.

---

# 43. Referential identity of props

This becomes very important for performance.

Consider:

```jsx
<Child user={user} />
```

If `user` is the same object:

```text
previous props.user
       ===
next props.user
```

that's useful for shallow/reference comparisons.

But:

```jsx
<Child user={{ name: "Alice" }} />
```

creates a new object each render.

So conceptually:

```text
render 1:
{ name: "Alice" } → object A

render 2:
{ name: "Alice" } → object B
```

Even though the contents are equal:

```text
A !== B
```

This matters for:

```text
React.memo
useMemo
useCallback
dependency arrays
context
performance
```

We'll revisit this carefully rather than prematurely memorizing "new object = re-render."

---

# 44. Component identity vs object identity

Do not confuse:

```text
JavaScript object identity
```

with:

```text
React component identity
```

These are related in some scenarios but are not the same concept.

For example:

```jsx
<Counter />
```

may produce a new React element object every render while React can still preserve the same component state, because the relevant identity of the rendered component has remained compatible.

This is one of the reasons understanding reconciliation matters more than thinking:

> "React compares object references for everything."

It doesn't work that simply.

---

# 45. Interview question: Is a component just a function?

Good answer:

> A function component is defined as a JavaScript function, but in React it is more than just an ordinary function call. React invokes it as part of its rendering process, associates its rendered result with an internal Fiber node, and manages things such as Hooks, state, context, scheduling, and reconciliation around that invocation.

That's considerably better than:

> "Yes, a component is just a function."

---

# 46. Interview question: What happens when React renders a function component?

A strong conceptual answer:

```text
React encounters element:
<Counter />

        ↓

element type points to Counter

        ↓

React creates/updates the corresponding Fiber

        ↓

React invokes Counter during render

        ↓

Hooks access the component's React-managed state

        ↓

Counter returns React elements

        ↓

React reconciles the returned subtree

        ↓

React determines required host changes

        ↓

commit phase applies them
```

That answer connects several React concepts together.

---

# 47. The most important concept from this topic

A function component should be mentally modeled as:

```text
                React
                  │
                  ▼
          Component identity
                  │
                  ▼
         React-managed state
                  │
                  ▼
          render invocation
                  │
                  ▼
          React elements
                  │
                  ▼
             Fiber tree
                  │
                  ▼
            reconciliation
                  │
                  ▼
               commit
```

The JavaScript function itself is only one part of the picture.

---

# 48. Interview traps

### "React creates a new component instance every render."

For function components, don't describe it this way.

The function runs again, but React can preserve the component's identity and state.

---

### "State lives inside the function."

Not really.

The **local variable** exists only for that invocation.

Persistent state is maintained by React.

---

### "Rendering means DOM manipulation."

No.

Rendering primarily calculates what the next UI should be.

---

### "Props and state are basically the same."

No.

```text
Props → inputs from parent
State → React-managed data for a component/tree position
```

---

### "A new JSX object means React destroys the component."

No.

React elements are often recreated every render, while component identity can remain stable across renders.

---

# 49. One complete example

Let's put everything together:

```jsx
function App() {
    const [name, setName] = useState("Alice");

    return (
        <Page>
            <User
                name={name}
                onRename={() => setName("Bob")}
            />
        </Page>
    );
}

function Page({ children }) {
    return (
        <main>
            {children}
        </main>
    );
}

function User({ name, onRename }) {
    return (
        <section>
            <h1>{name}</h1>

            <button onClick={onRename}>
                Rename
            </button>
        </section>
    );
}
```

Conceptual tree:

```text
App
 │
 └── Page
      │
      └── User
           ├── h1
           └── button
```

Data flow:

```text
App state
   │
   ▼
name prop
   │
   ▼
User
```

Event flow:

```text
button click
    ↓
onRename
    ↓
setName("Bob")
    ↓
App update
    ↓
App renders again
    ↓
new User element
    ↓
reconciliation
    ↓
h1 text changes
    ↓
commit
```

Notice how many React concepts are already connected:

```text
Components
Props
State
Events
Rendering
Elements
Fiber
Reconciliation
Commit
```

That's exactly why we're learning them in this order.

---

# 50. Revision sheet

Remember these six statements:

```text
1. A function component is a JavaScript function used by React
   as part of its rendering system.

2. <Component /> becomes a React element whose type points
   to that component.

3. The component returns React elements, not DOM nodes.

4. React manages persistent state outside an individual
   function invocation.

5. Component identity allows React to preserve state across
   renders.

6. Re-rendering a component does not mean recreating the
   entire DOM subtree.
```

The next topic is where these concepts become much more concrete:

# Topic 4 — React Rendering

We'll trace **exactly what happens when React renders for the first time and when `setState()` is called**, including:

```text
trigger
  ↓
schedule update
  ↓
render phase
  ↓
Fiber traversal
  ↓
reconciliation
  ↓
commit phase
  ↓
DOM mutation
  ↓
browser paint
```

We'll also answer the interview questions:

> **What causes a React re-render?**

> **Does calling `setState` immediately change state?**

> **Does rendering a parent always render its children?**

> **Why can React render a component even when the DOM doesn't change?**

