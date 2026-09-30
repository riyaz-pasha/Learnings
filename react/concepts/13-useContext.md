# Topic 13 — React Context & `useContext` Internals

Context is one of the most frequently asked React interview topics because it connects several things you’ve already learned:

**Fiber → rendering → scheduling → hooks → reconciliation → performance/bailouts**

The important part is not just knowing how to write `useContext`, but understanding **how React knows which components depend on a context and how it updates them**.

---

# 1. What problem does Context solve?

Consider this:

```jsx
function App() {
  const theme = "dark";

  return <Page theme={theme} />;
}

function Page({ theme }) {
  return <Layout theme={theme} />;
}

function Layout({ theme }) {
  return <Sidebar theme={theme} />;
}

function Sidebar({ theme }) {
  return <Button theme={theme} />;
}

function Button({ theme }) {
  return <button className={theme}>Click</button>;
}
```

`Page`, `Layout`, and `Sidebar` don't actually care about `theme`.

They're only forwarding it.

This is called **prop drilling**.

Context allows:

```jsx
const ThemeContext = createContext("light");

function App() {
  return (
    <ThemeContext value="dark">
      <Page />
    </ThemeContext>
  );
}

function Button() {
  const theme = useContext(ThemeContext);

  return <button className={theme}>Click</button>;
}
```

Now the intermediate components don't need to receive or forward `theme`.

React describes context as a mechanism for allowing components to receive information from distant parents without passing it through every intermediate component. ([React][1])

---

# 2. Context is NOT global state

This is an important interview distinction.

People often say:

> "Context is React's global state management."

That's not quite correct.

Context primarily solves:

> **How can a value be made available to a subtree without explicitly passing it through every component?**

For example:

```text
                    Provider
                       │
              ┌────────┴────────┐
              │                 │
            Page             Sidebar
              │
            Layout
              │
            Button
              │
         useContext()
```

The context value is available to consumers somewhere beneath the provider.

The state can live elsewhere:

```jsx
const [theme, setTheme] = useState("dark");

<ThemeContext value={theme}>
  ...
</ThemeContext>
```

So:

```text
State      → stores / changes data
Context    → distributes data through a subtree
```

This distinction is frequently useful in interviews.

---

# 3. Creating a Context

```jsx
const ThemeContext = createContext("light");
```

The argument is the **default value**.

Conceptually:

```text
ThemeContext
     │
     ├── identifies the context
     │
     └── default value = "light"
```

React's documentation explicitly notes that the context object itself doesn't contain the current application value in the ordinary sense; it represents **which context is being provided/read**. The default value is static and is used only when there is no matching provider above the consumer. ([React][2])

---

# 4. Provider

Older React versions commonly use:

```jsx
<ThemeContext.Provider value="dark">
  <App />
</ThemeContext.Provider>
```

Starting with React 19, the context object itself can be rendered as the provider:

```jsx
<ThemeContext value="dark">
  <App />
</ThemeContext>
```

React currently documents `<Context>` as the modern provider syntax, while `<Context.Provider>` remains the legacy form. ([React][2])

For interview purposes, you should recognize both.

---

# 5. What does `useContext()` actually do?

You write:

```jsx
function Button() {
  const theme = useContext(ThemeContext);

  return <button>{theme}</button>;
}
```

Conceptually:

```text
Button Fiber
    │
    │ useContext(ThemeContext)
    ↓
Find current value for ThemeContext
    │
    ↓
"dark"
```

But there's something much more important happening.

React doesn't merely say:

> "Give me the current value."

It also needs to remember:

> "This component depends on this context."

Why?

Because later the provider may change.

For example:

```jsx
<ThemeContext value="dark">
   <Button />
</ThemeContext>
```

then:

```jsx
<ThemeContext value="light">
   <Button />
</ThemeContext>
```

React needs to know:

```text
Button depends on ThemeContext
```

so that Button can receive the new value.

---

# 6. Context dependency is stored on the Fiber

This is where Context connects directly to the Fiber system we studied.

Recall that a Fiber contains things such as:

```text
Fiber
 ├── pendingProps
 ├── memoizedProps
 ├── memoizedState
 ├── updateQueue
 ├── lanes
 ├── childLanes
 ├── flags
 └── dependencies
```

The interesting field here is:

```text
dependencies
```

React uses Fiber dependency information to track context reads.

The current React reconciler source has logic in `ReactFiberNewContext.js` that reads a context value and creates a context-dependency record containing the context and the value observed by that consumer. ([GitHub][3])

Conceptually:

```text
Button Fiber
      │
      └── dependencies
             │
             └── ContextDependency
                    ├── context → ThemeContext
                    ├── memoizedValue → "dark"
                    └── next → ...
```

That means React has effectively recorded:

```text
Button observed ThemeContext = "dark"
```

---

# 7. Why does React need `memoizedValue`?

Suppose:

```text
Previous context value = "dark"
Current context value  = "light"
```

React can detect:

```js
Object.is(previousValue, currentValue)
```

which is false.

Therefore:

```text
Context changed
       ↓
Find consumers that depend on it
       ↓
Schedule/update those consumers
```

React's public documentation explicitly says provider values are compared using `Object.is`. ([React][1])

---

# 8. The most important example

Consider:

```jsx
const ThemeContext = createContext("light");

function App() {
  const [theme, setTheme] = useState("dark");

  return (
    <ThemeContext value={theme}>
      <Page />
    </ThemeContext>
  );
}

function Page() {
  return <Button />;
}

function Button() {
  const theme = useContext(ThemeContext);

  console.log("Button render");

  return <div>{theme}</div>;
}
```

Initially:

```text
theme = "dark"
```

Context relationship:

```text
App
 │
 └── Theme Provider
       │
       └── Page
            │
            └── Button
                  │
                  └── useContext(ThemeContext)
```

React records:

```text
Button → depends on ThemeContext
```

Now:

```js
setTheme("light");
```

The provider value changes:

```text
"dark" → "light"
```

React detects the context change.

It then propagates that change to consumers that read that context.

So:

```text
ThemeContext changed
       ↓
Button depends on ThemeContext
       ↓
Button gets scheduled for new work
       ↓
Button renders
       ↓
useContext() returns "light"
       ↓
new React output
       ↓
commit if host output changed
```

React's current reconciler has dedicated context propagation logic for finding dependent Fibers and scheduling context-related work. ([GitHub][3])

---

# 9. Does every component under a Provider rerender?

This is a classic interview question.

Suppose:

```jsx
<ThemeContext value={theme}>
  <A />
  <B />
  <C />
</ThemeContext>
```

and:

```jsx
function A() {
  const theme = useContext(ThemeContext);
  return <div>{theme}</div>;
}

function B() {
  return <div>Hello</div>;
}

function C() {
  return <div>World</div>;
}
```

When `theme` changes, React does **not conceptually need to treat every descendant as a context consumer**.

The relevant distinction is:

```text
A → consumes ThemeContext
B → doesn't
C → doesn't
```

React tracks context dependencies and propagates context changes to Fibers that depend on the changed context. ([GitHub][3])

However, don't reduce this to:

> "Only consumers can ever execute."

Normal parent updates can still cause traversal/rendering through descendants depending on the tree and bailout opportunities.

So the interview-safe explanation is:

> **A context change schedules components that read that context; non-consumers aren't automatically made context consumers merely because they're descendants. Normal parent rendering and bailouts are a separate concern.**

That's a much more accurate statement.

---

# 10. Context + `React.memo`

This is extremely important.

You learned previously:

```jsx
const Child = React.memo(MyComponent);
```

A common misconception is:

> "memo means the component won't rerender."

Not true.

Consider:

```jsx
const ThemeContext = createContext("light");

const Button = React.memo(function Button() {
  const theme = useContext(ThemeContext);

  console.log("Button render");

  return <button>{theme}</button>;
});
```

Now:

```jsx
<ThemeContext value="dark">
  <Button />
</ThemeContext>
```

and later:

```text
dark → light
```

`Button` can still rerender.

Why?

Because `React.memo` primarily helps with **props-based bailout**.

Context is another source of work.

React's current documentation explicitly states that using `memo` does not prevent children from receiving fresh context values. ([React][1])

This also appears in the reconciler's bailout logic: React checks for scheduled updates/context before deciding that a memoized component can safely bail out. ([Fossies][4])

---

# 11. Why is this necessary?

Imagine:

```jsx
const UserContext = createContext(null);

const UserCard = React.memo(function UserCard() {
  const user = useContext(UserContext);

  return <h1>{user.name}</h1>;
});
```

Suppose:

```text
props never change
```

but:

```text
UserContext:
Alice → Bob
```

If `memo` blocked the render, the UI would still show Alice.

So React must treat:

```text
context update
```

as an independent reason to render.

Think of a component as potentially receiving new information from several sources:

```text
               Component
                  │
       ┌──────────┼──────────┐
       │          │          │
      Props      State     Context
       │          │          │
       └──────────┼──────────┘
                  ↓
             render work
```

---

# 12. Nearest Provider wins

Suppose:

```jsx
<ThemeContext value="dark">
  <Page />

  <ThemeContext value="light">
    <Footer />
  </ThemeContext>
</ThemeContext>
```

Then:

```text
Page   → dark
Footer → light
```

Why?

Because context lookup uses the **closest matching provider above the consumer**. ([React][1])

Think:

```text
ThemeContext("dark")
        │
        ├── Page
        │
        └── ThemeContext("light")
                │
                └── Footer
```

`Footer` sees the inner provider.

---

# 13. Provider below the consumer doesn't work

This surprises people.

```jsx
function Component() {
  const theme = useContext(ThemeContext);

  return (
    <ThemeContext value="dark">
      ...
    </ThemeContext>
  );
}
```

Does `useContext` see `"dark"`?

**No.**

The provider is produced by the component's returned tree.

But `useContext()` runs while that component itself is rendering.

So the provider is conceptually **below** the read.

React's documentation explicitly points this out: a provider returned by the same component does not affect a `useContext()` call in that component. ([React][1])

Visualize:

```text
Component rendering
│
├── useContext()    ← READ HERE
│
└── returns
     │
     └── Provider   ← BELOW THE READ
```

The provider must be above:

```text
Provider
   ↓
Component using useContext
```

---

# 14. Default value

Suppose:

```jsx
const ThemeContext = createContext("light");
```

and:

```jsx
function Button() {
  const theme = useContext(ThemeContext);

  return <button>{theme}</button>;
}
```

with no provider:

```jsx
<Button />
```

Then:

```text
theme = "light"
```

The default is useful as a fallback for situations where no provider exists.

But there's an important subtlety.

This:

```jsx
<ThemeContext value={undefined}>
  <Button />
</ThemeContext>
```

does **not** mean:

```text
use default "light"
```

It means:

```text
value = undefined
```

because a provider exists.

React's docs explicitly distinguish these cases. The default is used only when there is no matching provider at all. ([React][1])

---

# 15. The object identity trap

This is one of the most important Context performance questions.

Consider:

```jsx
function App() {
  const [count, setCount] = useState(0);

  const value = {
    user: "John",
    role: "admin",
  };

  return (
    <AuthContext value={value}>
      <Page />
    </AuthContext>
  );
}
```

Every time `App` renders:

```js
const value = {
   user: "John",
   role: "admin"
};
```

creates a **new object**.

So:

```text
previous value → object A
next value     → object B
```

Even if:

```text
A.user === B.user
A.role === B.role
```

the objects themselves are different:

```js
Object.is(A, B) // false
```

Therefore React treats the context value as changed. React's documented comparison for provider values is `Object.is`. ([React][1])

---

# 16. Stabilizing Context values

You may therefore see:

```jsx
const login = useCallback(() => {
  // ...
}, []);

const value = useMemo(() => ({
  user,
  login,
}), [user, login]);

return (
  <AuthContext value={value}>
    <App />
  </AuthContext>
);
```

Now:

```text
same user
    +
same login
    ↓
same memoized value object
```

So the identity can remain stable across renders.

This is where the topics we just studied connect:

```text
useCallback
     ↓
stable function

useMemo
     ↓
stable object

Context
     ↓
stable provider value identity
```

But don't automatically memoize every context value.

Memoization itself has overhead and complexity, so it should be tied to an actual rendering/performance concern.

---

# 17. A very common interview example

Bad:

```jsx
function AuthProvider({ children }) {
  const [user, setUser] = useState(null);

  const login = () => {
    // login
  };

  return (
    <AuthContext value={{ user, login }}>
      {children}
    </AuthContext>
  );
}
```

Every provider render creates:

```text
new object
new login function
```

Therefore:

```text
context value identity changes
        ↓
context consumers may need new work
```

A more stable version:

```jsx
function AuthProvider({ children }) {
  const [user, setUser] = useState(null);

  const login = useCallback(() => {
    // login
  }, []);

  const value = useMemo(
    () => ({ user, login }),
    [user, login]
  );

  return (
    <AuthContext value={value}>
      {children}
    </AuthContext>
  );
}
```

Again, the important thing is not:

> "Always use useMemo with Context."

It is:

> **Provider value identity matters because React compares context values using `Object.is`.**

---

# 18. Context internals: simplified implementation

Let's construct a simplified mental implementation.

Imagine:

```js
function createContext(defaultValue) {
  return {
    defaultValue,
    currentValue: defaultValue,
    Provider: ...
  };
}
```

A Provider conceptually does:

```js
function Provider({ value, children }) {
  context.currentValue = value;

  return children;
}
```

A consumer conceptually does:

```js
function useContext(context) {
  return context.currentValue;
}
```

But this is **far too simplified** for actual React.

Real React must handle:

```text
nested providers
multiple concurrent renders
different renderers
Fiber trees
context dependencies
context propagation
scheduling lanes
bailouts
interrupted rendering
current/WIP trees
```

So the real algorithm needs substantially more machinery.

---

# 19. The real conceptual flow

The useful internal model is:

```text
createContext()
      ↓
Context object created
      ↓
Provider receives value
      ↓
React makes that value available to descendants
      ↓
Consumer calls useContext()
      ↓
React reads current context value
      ↓
React records dependency on that context in Fiber
      ↓
Later Provider value changes
      ↓
React compares old/new values
      ↓
If changed:
   propagate context change
      ↓
find dependent Fibers
      ↓
mark/schedule work
      ↓
render affected consumers
      ↓
commit resulting changes
```

That is the key internal picture to remember.

---

# 20. Context propagation and Fiber

Let's make it more concrete.

Suppose we have:

```text
Provider Fiber
      │
      ├── A
      │    │
      │    └── B
      │         │
      │         └── C ← useContext(ThemeContext)
      │
      └── D
           │
           └── E
```

React records on `C` something conceptually like:

```text
C.dependencies
   ↓
ContextDependency
   context = ThemeContext
   memoizedValue = "dark"
```

When the provider changes:

```text
ThemeContext
dark → light
```

React propagates that change through the Fiber tree and identifies dependent work.

This is implemented in the reconciler's context code, including operations for reading context, detecting changes, and propagating context changes to dependent Fibers. ([GitHub][3])

---

# 21. Why Context is integrated with scheduling

You already learned about lanes.

Context updates participate in React's scheduling system.

So conceptually:

```text
Provider value changes
        ↓
Context consumers receive work
        ↓
Work gets lanes
        ↓
Root sees pending work
        ↓
Scheduler chooses when to render
        ↓
Fiber work loop processes it
```

Therefore Context isn't some separate mechanism that bypasses the Fiber scheduler.

It ultimately becomes **React work on Fibers**.

This is a very useful connection for interview questions such as:

> "How does a Context update actually cause a consumer to rerender?"

Answer:

> The consumer records a context dependency in its Fiber. When the provider's value changes, React detects the changed context and propagates that change to dependent Fibers, causing the corresponding work to be scheduled and processed during rendering.

---

# 22. `useContext` itself doesn't trigger updates

Another subtle point.

You might think:

```jsx
const value = useContext(MyContext);
```

is like:

```js
subscribe(MyContext);
```

with a manually registered callback.

Conceptually there is subscription-like behavior, but don't imagine a traditional event emitter:

```js
context.listeners.push(component)
```

React integrates context reads with the **Fiber dependency system**.

So rather than:

```text
Context
  ↓
array of callback functions
```

the useful internal picture is:

```text
Fiber
  ↓
dependencies
  ↓
context dependency
```

That fits naturally into React's reconciliation/scheduling architecture.

---

# 23. Context and rendering snapshots

Suppose:

```jsx
function Button() {
  const theme = useContext(ThemeContext);

  console.log(theme);

  return <button>{theme}</button>;
}
```

During one render:

```text
theme = "dark"
```

That render sees a consistent value.

Then another render may see:

```text
theme = "light"
```

So just like state and props:

```text
Render #1 → context snapshot = "dark"
Render #2 → context snapshot = "light"
```

Don't think:

```text
theme variable magically changes underneath the currently executing function
```

The render gets the value applicable to that render.

That connects with the state snapshot model you learned in `useState`.

---

# 24. `useContext` isn't limited to primitive values

Context can contain:

```jsx
<Context value="dark" />
```

or:

```jsx
<Context value={42} />
```

or:

```jsx
<Context value={{ user, role }} />
```

or:

```jsx
<Context value={function login() {}} />
```

or even:

```jsx
<Context value={{
  user,
  settings,
  login,
  logout
}} />
```

Context values can be any type. ([React][2])

---

# 25. Multiple contexts

You can use multiple contexts:

```jsx
function Component() {
  const theme = useContext(ThemeContext);
  const user = useContext(UserContext);
  const language = useContext(LanguageContext);

  // ...
}
```

The Fiber can therefore have multiple context dependencies.

Conceptually:

```text
Fiber
 │
 └── dependencies
       │
       ├── ThemeContext
       ├── UserContext
       └── LanguageContext
```

This becomes important because a change in one context doesn't conceptually mean every context dependency changed.

---

# 26. A powerful optimization technique: split contexts

Suppose you do this:

```jsx
const AppContext = createContext({
  theme: "dark",
  user: null,
  locale: "en",
  notifications: []
});
```

Many unrelated things share one context.

Now:

```text
notifications changed
```

can result in a new context value, which affects consumers of that context even if some of them only care about `theme`.

A common architecture is:

```jsx
<ThemeContext>
<UserContext>
<LocaleContext>
```

instead of putting everything into one giant context.

Conceptually:

```text
Before

AppContext
 ├── theme
 ├── user
 ├── locale
 └── notifications


After

ThemeContext
UserContext
LocaleContext
NotificationContext
```

This can reduce unnecessary context-driven work by making dependencies more granular.

---

# 27. Context vs props

### Props

```jsx
<Child theme={theme} />
```

Dependency is explicit:

```text
Parent → Child
```

### Context

```jsx
<ThemeContext value={theme}>
   ...
</ThemeContext>
```

Consumer:

```jsx
const theme = useContext(ThemeContext);
```

Dependency is implicit in the component interface but explicit in the implementation.

So:

```text
Props:
data flows through explicit component boundaries

Context:
data flows through a provider-defined subtree
```

---

# 28. Context vs state

This is another common interview comparison.

### State

```jsx
const [count, setCount] = useState(0);
```

Purpose:

```text
store changing component/application data
```

### Context

```jsx
const count = useContext(CounterContext);
```

Purpose:

```text
make a value available across a subtree
```

Often they work together:

```jsx
function Provider({ children }) {
  const [count, setCount] = useState(0);

  return (
    <CounterContext value={{ count, setCount }}>
      {children}
    </CounterContext>
  );
}
```

Here:

```text
useState
    ↓
owns the state

Context
    ↓
distributes the state
```

That's an extremely common React architecture.

---

# 29. Context doesn't eliminate rerender problems

You may hear:

> "Context solves prop drilling, so performance is solved."

No.

Context solves one API/data-flow problem.

You can still have:

```text
Provider changes frequently
        ↓
many consumers depend on it
        ↓
large amount of context-driven rendering
```

For example:

```jsx
<AppContext value={{
  mousePosition,
  user,
  theme,
  notifications,
  settings
}}>
```

If `mousePosition` updates 60 times per second, this can become problematic when many consumers depend on the same context.

That's why real applications often use:

```text
split contexts
memoization
selectors / external stores
component boundaries
local state
```

depending on the problem.

---

# 30. React Context and the DOM

Another common misconception:

> "Context is stored in the DOM."

No.

Context is part of **React's internal tree/reconciler model**.

It isn't equivalent to:

```html
<div data-context="dark">
```

Nothing requires a DOM node for context propagation.

Remember:

```text
React Element
       ↓
Fiber
       ↓
Context dependency
```

Context is a React runtime concept.

---

# 31. Context and Server Components

React's modern documentation also has additional behavior around Server Components. For example, React 19.3 allows Server Components to render a context imported from a client module directly as `<Context value={...}>`, although Server Components themselves cannot create Context. ([React][5])

For a core interview, however, focus first on:

```text
createContext
Provider
useContext
dependencies
propagation
Object.is
memo interaction
```

Those are much more fundamental.

---

# 32. `use(Context)` vs `useContext(Context)`

Modern React also has:

```jsx
const theme = use(ThemeContext);
```

The `use` API can read context too. Unlike `useContext`, `use` can be called in conditions and loops, subject to its own rules. ([React][6])

Example:

```jsx
function Component({ enabled }) {
  if (enabled) {
    const theme = use(ThemeContext);
  }

  return ...;
}
```

This is different from:

```jsx
function Component({ enabled }) {
  if (enabled) {
    const theme = useContext(ThemeContext); // invalid Hook usage
  }
}
```

For our current course, keep `useContext` as the primary Context API and treat `use(Context)` as a modern API we'll revisit when we cover React 19/advanced rendering APIs.

---

# 33. Internal pseudocode

Here's a simplified model close to how you should think about the reconciler.

### Reading context

```js
function readContextForConsumer(fiber, context) {
  const value = getCurrentContextValue(context);

  const dependency = {
    context,
    memoizedValue: value,
    next: null
  };

  fiber.dependencies = addDependency(
    fiber.dependencies,
    dependency
  );

  return value;
}
```

Again, this is **conceptual pseudocode**, not a copy of React's implementation.

The current source has corresponding context-reading logic that records the observed value against the consumer Fiber. ([Fossies][7])

### Provider change

Conceptually:

```js
function updateProvider(oldValue, newValue, providerFiber) {
  if (!Object.is(oldValue, newValue)) {
    propagateContextChange(providerFiber, context);
  }
}
```

### Propagation

Conceptually:

```js
function propagateContextChange(providerFiber, context) {
  walkDescendants(providerFiber, fiber => {
    if (fiber.dependsOn(context)) {
      scheduleContextWork(fiber);
    }
  });
}
```

Again, React's real implementation contains much more detail because this must integrate with Fibers, dependencies, lanes, nesting, concurrency, and bailout logic. ([GitHub][3])

---

# 34. Context + Fiber + lanes: the complete picture

At this point you can connect almost everything we've covered:

```text
             Provider
                │
                │ value changed
                ↓
        Object.is comparison
                │
          value changed?
             /      \
           no        yes
           │          │
           │          ↓
           │    propagate context
           │          │
           │          ↓
           │   find dependent Fibers
           │          │
           │          ↓
           │     schedule work
           │          │
           │          ↓
           │        lanes
           │          │
           │          ↓
           └────→ Fiber work loop
                       │
                 beginWork()
                       │
                 component renders
                       │
                reconciliation
                       │
                completeWork()
                       │
                     commit
```

This is the level of understanding interviewers usually want when they ask something like:

> "What actually happens internally when Context changes?"

---

# 35. Most common interview questions

You should be ready for these:

### Q1. What is Context?

> Context is a React mechanism for making a value available to components in a subtree without explicitly passing it through every intermediate component.

### Q2. Is Context global state?

> No. Context is primarily a value propagation mechanism through a React subtree. It is commonly combined with state or reducers to distribute application state.

### Q3. How does `useContext` find its value?

> It reads the value from the nearest matching provider above the component in the React tree.

### Q4. What happens when a Provider's value changes?

> React compares the previous and next context values using `Object.is`. When they differ, React propagates the context change to Fibers that have recorded a dependency on that context, causing the relevant work to be scheduled.

([React][1])

### Q5. Does `React.memo` prevent Context updates?

> No. `memo` can bail out based on props, but a relevant context change is an independent reason for the component to receive new work.

([React][1])

### Q6. Why can Context cause unnecessary renders?

> Because provider values are compared by identity using `Object.is`. Creating a new object or function as the provider value can make the context value appear changed even when its contents are equivalent.

([React][1])

### Q7. Does a provider affect its own component's `useContext` call?

> No. The provider must be above the component doing the read.

([React][1])

### Q8. What's stored internally for a context consumer?

> React records context dependencies on the consumer Fiber, including the context and the value observed during rendering.

([Fossies][7])

---

# 36. The one mental model to remember

Don't memorize dozens of internal function names.

Remember this:

```text
Context = value propagation + dependency tracking
```

More completely:

```text
createContext()
      ↓
Context identity

Provider
      ↓
provides a value to a subtree

useContext()
      ↓
reads nearest value
      +
records dependency on Fiber

Provider value changes
      ↓
Object.is comparison

Changed?
      ↓
propagate to dependent Fibers
      ↓
schedule/render them
      ↓
commit resulting UI
```

That model connects Context directly to everything we've already covered.

---

# 37. What to say in an interview

A strong answer would be:

> **"React Context provides a way to pass values through a component subtree without explicitly forwarding props through every intermediate component. When a component calls `useContext`, React reads the nearest provider's value and records a dependency on that context in the component's Fiber. When the provider value changes, React compares the old and new values using `Object.is`. If the value changed, React propagates the context change to Fibers that depend on that context so they can receive new render work. Context updates are therefore integrated with Fiber scheduling rather than being a separate mechanism. `React.memo` doesn't block a relevant context update because context is an independent source of updates."** ([React][1])

That answer demonstrates much deeper understanding than simply saying:

> "Context avoids prop drilling."

---

## One final distinction

Keep these three concepts separate:

```text
Props
  → explicit data passed between components

Context
  → implicit data available within a provider subtree

State
  → persistent changing data owned by React
```

And internally:

```text
State update
   → Fiber update queue

Context read
   → Fiber context dependency

Context change
   → propagation + scheduled Fiber work
```

That distinction will make the next few topics much easier.

**Next Topic → `useReducer` Internals**
We'll connect it directly to the `useState` update queue you've already learned.

[1]: https://react.dev/reference/react/useContext?utm_source=chatgpt.com "useContext – React"
[2]: https://mn.react.dev/reference/react/createContext?utm_source=chatgpt.com "createContext – React"
[3]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberNewContext.js "react/packages/react-reconciler/src/ReactFiberNewContext.js at main · react/react · GitHub"
[4]: https://fossies.org/diffs/react/19.2.8_vs_19.3.0/packages/react-reconciler/src/ReactFiberBeginWork.js-diff.html?utm_source=chatgpt.com "react: ...r/src/ReactFiberBeginWork.js - 19.2.8 vs. 19.3.0 changes | Fossies Diffs"
[5]: https://react.dev/blog/2026/09/09/react-19-3?utm_source=chatgpt.com "React 19.3 – React"
[6]: https://react.dev/reference/react/use?utm_source=chatgpt.com "use – React"
[7]: https://fossies.org/diffs/react/19.2.8_vs_19.3.0/packages/react-reconciler/src/ReactFiberNewContext.js-diff.html?utm_source=chatgpt.com "react: .../src/ReactFiberNewContext.js - 19.2.8 vs. 19.3.0 changes | Fossies Diffs"

