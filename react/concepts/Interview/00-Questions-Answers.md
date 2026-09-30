# React Interview Prep: Roadmap

We'll go one concept at a time, from easy to hard:

1. **React Basics** (React, Virtual DOM, JSX, Components, Props, State) ← *we start here*
2. Lists, Keys, Events, Conditional Rendering
3. Hooks: `useState`, `useEffect`
4. More Hooks: `useRef`, `useMemo`, `useCallback`, `useContext`, custom hooks
5. Forms and Controlled vs Uncontrolled components
6. Rendering, Reconciliation, and Performance
7. State Management (Context, Redux)
8. Routing, Code Splitting, Lazy Loading
9. Advanced Patterns (HOC, Render Props, Error Boundaries, Portals)
10. React 18/19 features (Concurrent Rendering, Suspense, Server Components)

---

# Concept 1: React Basics

## Q1. What is React?

**Short definition:** React is a JavaScript library for building user interfaces, made by Facebook (Meta).

**Deeper explanation:**
Think of a web page as many small pieces: a navbar, a button, a product card, a comment box. React lets you build each piece separately, and then join them like Lego blocks.

The big idea is that **your UI is a function of your data**. You don't tell the browser "go find this element and change its text." Instead, you say "when the data looks like this, the screen should look like this." When the data changes, React updates the screen for you.

This style is called **declarative** programming. You describe *what* you want, not *how* to do it step by step.

**Interview tip:** Say "library, not framework." React only handles the view layer. Routing, data fetching, and so on come from other tools.

---

## Q2. What is the Virtual DOM? How does it work?

**Short definition:** The Virtual DOM is a lightweight copy of the real DOM, kept in memory as a JavaScript object.

**Deeper explanation:**
Changing the real DOM is slow, because the browser may need to recalculate layout and repaint the screen. React avoids touching it more than needed.

Here's what happens when your data changes:

1. React creates a **new** Virtual DOM tree with the updated data.
2. React **compares** it with the **previous** Virtual DOM tree. This is called **diffing**.
3. React finds exactly what changed.
4. React updates **only those parts** in the real DOM. This is called **reconciliation**.

**Example:** You have a list of 100 items, and only one item's text changes. React doesn't rebuild all 100 items. It updates just that one text node.

**Common mistake to avoid:** Don't say "Virtual DOM is faster than the real DOM." It isn't magic. It's just a smart way to avoid unnecessary real DOM work. The better answer: *"It makes updates efficient by batching and minimizing real DOM changes."*

---

## Q3. What is JSX?

**Short definition:** JSX (JavaScript XML) is a syntax extension that lets you write HTML-like code inside JavaScript.

**Deeper explanation:**
Browsers can't understand JSX. A tool like Babel converts it into normal JavaScript before it runs.

This JSX:

```jsx
const element = <h1 className="title">Hello, Ravi</h1>;
```

becomes this:

```js
const element = React.createElement("h1", { className: "title" }, "Hello, Ravi");
```

So JSX is just a friendlier way to write `React.createElement()` calls.

**Key rules of JSX:**
- Use `className` instead of `class` (because `class` is a reserved word in JavaScript).
- Use `{}` to put JavaScript expressions inside: `<p>{2 + 2}</p>`
- Every component must return **one parent element**. Use a Fragment `<>...</>` if you don't want an extra `div`.
- Close all tags, even `<img />` and `<input />`.

**Common follow-up:** *"Is JSX required to use React?"* No. You can use `React.createElement` directly, but nobody wants to.

---

## Q4. What is a Component? What types are there?

**Short definition:** A component is a reusable, independent piece of UI.

**Deeper explanation:**
A component is basically a function that takes some input (props) and returns what should appear on screen (JSX).

**Functional component (modern, preferred):**

```jsx
function Greeting({ name }) {
  return <h1>Hello, {name}!</h1>;
}
```

**Class component (older style):**

```jsx
class Greeting extends React.Component {
  render() {
    return <h1>Hello, {this.props.name}!</h1>;
  }
}
```

**Why functional components won:**
Before React 16.8, only class components could hold state and use lifecycle methods. Then **Hooks** arrived, and functional components could do everything classes could, with less code and less confusion around `this`.

**Rule:** Component names must start with a **capital letter**. React treats `<greeting />` as an HTML tag and `<Greeting />` as a component.

---

## Q5. What are Props?

**Short definition:** Props (short for properties) are inputs passed from a parent component to a child component.

**Deeper explanation:**
Props work like function arguments. They let you reuse the same component with different data.

```jsx
function UserCard({ name, age }) {
  return <p>{name} is {age} years old</p>;
}

function App() {
  return (
    <>
      <UserCard name="Asha" age={25} />
      <UserCard name="Kiran" age={30} />
    </>
  );
}
```

**Two important points:**
- **Props are read-only.** A child must never change its own props. This is what makes data flow predictable.
- **Data flows one way**, from parent to child. This is called *one-way data flow*. If a child needs to talk back to the parent, the parent passes down a **function** as a prop, and the child calls it.

---

## Q6. What is State?

**Short definition:** State is data that belongs to a component and can change over time. When it changes, React re-renders the component.

**Deeper explanation:**
Props come from outside. State lives inside. Use state for things that change because of user actions, like a counter, a typed input, or whether a modal is open.

```jsx
import { useState } from "react";

function Counter() {
  const [count, setCount] = useState(0);

  return (
    <button onClick={() => setCount(count + 1)}>
      Clicked {count} times
    </button>
  );
}
```

**Why can't we just use a normal variable?**
If you write `let count = 0` and change it, React doesn't know anything happened, so the screen won't update. Also, normal variables reset on every render. Calling `setCount` does two things: it **saves the new value** and **tells React to re-render**.

**Never change state directly:**

```jsx
count = count + 1;      // ❌ Wrong. React won't notice.
setCount(count + 1);    // ✅ Correct
```

---

## Q7. What is the difference between Props and State?

| | Props | State |
|---|---|---|
| Who owns it? | Parent | The component itself |
| Can it change? | No (read-only) | Yes (via setter) |
| Purpose | Pass data down | Track changing data |
| Causes re-render? | Yes, when the parent passes new values | Yes, when updated |

**Simple way to remember:** Props are like the settings a caller gives you. State is your own memory.

---

**That wraps up Concept 1.** Reply **"next"** and I'll move to **Concept 2: Lists, Keys, Events, and Conditional Rendering**. If you'd like, I can also give you a few practice questions on this concept first.

---

# Concept 2: Lists, Keys, Events, and Conditional Rendering

## Q1. How do you render a list in React?

**Short definition:** You use the JavaScript `map()` method to turn an array of data into an array of JSX elements.

**Deeper explanation:**
React has no special loop syntax like `v-for` or `ngFor`. You just use plain JavaScript. Since JSX accepts arrays of elements, `map()` fits naturally.

```jsx
function FruitList() {
  const fruits = ["Apple", "Banana", "Mango"];

  return (
    <ul>
      {fruits.map((fruit) => (
        <li key={fruit}>{fruit}</li>
      ))}
    </ul>
  );
}
```

**Interview tip:** Use `map()`, not `forEach()`. `forEach()` returns nothing (`undefined`), so nothing would render. `map()` returns a new array, and that array is what React shows.

---

## Q2. What are keys in React? Why are they important?

**Short definition:** A key is a special string or number attribute that helps React identify which items in a list have changed, been added, or been removed.

**Deeper explanation:**
Remember the Virtual DOM diffing from Concept 1? When React compares an old list with a new one, it needs a way to match old items to new items. Keys are those name tags.

**Example:** Your list is `[A, B, C]`, and you add `D` at the **top**: `[D, A, B, C]`.

- **With good keys:** React sees that A, B, and C still exist, just moved down. It only creates D.
- **Without keys (or with index keys):** React compares by position. Position 0 was A, now it's D, so it "updates" everything. That's more work, and it can cause real bugs.

**What kind of bugs?**
If each list item has an input box (or its own state), React may attach that state to the wrong item after reordering. You type in the first input, delete the first item, and suddenly the text shows up in the wrong row.

**Rules for keys:**
- Must be **unique among siblings** (not globally).
- Must be **stable**. Don't generate them during render, like `key={Math.random()}`. That gives a new key every time, so React destroys and recreates every item.
- Keys are **not passed as props**. Inside the child, `props.key` is `undefined`.

---

## Q3. Why is using the array index as a key a bad idea?

**Short definition:** Index keys tie an item's identity to its position, not to the item itself. When the list order changes, the identity breaks.

**Deeper explanation:**
Index keys are fine only if **all three** are true:
1. The list is static (never reordered).
2. Items are never added or removed in the middle.
3. Items have no state or inputs.

Otherwise, use a unique ID from your data:

```jsx
{users.map((user) => (
  <UserRow key={user.id} user={user} />
))}
```

**Interview tip:** If the interviewer asks "what if my data has no ID?", answer: generate an ID **once** when the data is created (for example with `crypto.randomUUID()`), not during render.

---

## Q4. How do you handle events in React?

**Short definition:** You attach event handlers using camelCase props like `onClick` and pass a **function** to them.

```jsx
function Button() {
  function handleClick() {
    alert("Clicked!");
  }

  return <button onClick={handleClick}>Click me</button>;
}
```

**Differences from plain HTML:**
- camelCase: `onClick`, not `onclick`.
- You pass a function, not a string: `onClick={handleClick}`, not `onclick="handleClick()"`.

**A very common mistake:**

```jsx
<button onClick={handleClick()}>   {/* ❌ calls it immediately on render */}
<button onClick={handleClick}>     {/* ✅ passes the function */}
<button onClick={() => handleClick(5)}>  {/* ✅ when you need arguments */}
```

`handleClick()` with brackets runs the function right away, while React is still drawing the screen. You want React to run it **later**, when the click happens.

---

## Q5. What is a Synthetic Event?

**Short definition:** A SyntheticEvent is React's wrapper around the browser's native event. It gives you the same interface on every browser.

**Deeper explanation:**
Different browsers used to handle events slightly differently. React wraps them so `event.target`, `event.preventDefault()`, and `event.stopPropagation()` work the same everywhere.

```jsx
function Form() {
  function handleSubmit(e) {
    e.preventDefault(); // stops the page from reloading
    console.log("Submitted");
  }

  return <form onSubmit={handleSubmit}>...</form>;
}
```

**Good to know:** React uses **event delegation**. It doesn't attach a listener to every button. It attaches one listener at the root (since React 17, the root container, before that it was `document`) and figures out which component the event belongs to. This saves memory.

**Older note:** Before React 17, synthetic events were "pooled" and you couldn't use them in async code. That was removed. You can ignore it now, but interviewers sometimes ask.

---

## Q6. What is conditional rendering? What are the ways to do it?

**Short definition:** Showing different UI depending on a condition, like whether a user is logged in.

There is no special React syntax for this. You use normal JavaScript.

**1. `if` statement (good for bigger logic):**

```jsx
function Greeting({ isLoggedIn }) {
  if (isLoggedIn) {
    return <h1>Welcome back!</h1>;
  }
  return <h1>Please log in.</h1>;
}
```

**2. Ternary operator (good for either/or, inside JSX):**

```jsx
{isLoggedIn ? <Dashboard /> : <Login />}
```

**3. `&&` operator (good for "show or show nothing"):**

```jsx
{hasNotifications && <Badge />}
```

**4. Returning `null` (render nothing):**

```jsx
if (!show) return null;
```

---

## Q7. What is the `&&` trap with the number 0?

**Short definition:** If the left side of `&&` is `0`, React renders the number `0` on screen instead of nothing.

**Deeper explanation:**

```jsx
{items.length && <List items={items} />}
```

When `items` is empty, `items.length` is `0`. `0 && anything` gives `0`, and React **does render numbers**. So users see a lonely `0` on the page.

Why doesn't this happen with `false`, `null`, or `undefined`? React skips those. But `0` is a valid number, so it shows.

**Fix it by making the condition a real boolean:**

```jsx
{items.length > 0 && <List items={items} />}
{Boolean(items.length) && <List items={items} />}
{items.length ? <List items={items} /> : null}
```

This is a very popular interview question because it tests whether you really understand how React renders values.

---

## Q8. How do you pass a function to a child and handle events from the child?

**Short definition:** The parent defines the function and passes it down as a prop. The child calls it when something happens.

This is how a child "talks back" to its parent, since data only flows down.

```jsx
function Parent() {
  const [message, setMessage] = useState("");

  return (
    <>
      <Child onSend={setMessage} />
      <p>Child said: {message}</p>
    </>
  );
}

function Child({ onSend }) {
  return <button onClick={() => onSend("Hello!")}>Say Hello</button>;
}
```

Naming convention: props that receive functions usually start with `on` (like `onSend`, `onChange`). The function inside the parent usually starts with `handle` (like `handleSend`).

---

**That's Concept 2 done.** Reply **"next"** for **Concept 3: Hooks (`useState` and `useEffect`)**. This is where interviews get serious, so I'll go deeper there.

---

# Concept 3: Hooks (`useState` and `useEffect`)

## Q1. What are Hooks? Why were they introduced?

**Short definition:** Hooks are special functions (they start with `use`) that let functional components use React features like state and lifecycle behavior.

**Deeper explanation:**
Before React 16.8, a functional component was "dumb." It could only take props and return JSX. If you needed state or lifecycle methods, you had to write a class.

Classes had real problems:
- `this` was confusing. You had to bind methods or use arrow functions.
- Related logic was split across different lifecycle methods. For example, you started a timer in `componentDidMount` and cleaned it up in `componentWillUnmount`, far apart.
- Sharing logic between components needed tricky patterns like HOCs and render props.

Hooks fixed all of this. You can now group related logic together and reuse it easily.

---

## Q2. What are the Rules of Hooks? Why do they exist?

**Short definition:**
1. Only call Hooks at the **top level** of a component. Not inside loops, conditions, or nested functions.
2. Only call Hooks from **React function components** or **custom hooks**.

**Deeper explanation:**
React does not know your hooks by name. It remembers them by **the order in which they are called**. Think of it like a row of numbered boxes: the first `useState` is box 1, the second is box 2, and so on.

```jsx
function Profile({ show }) {
  const [name, setName] = useState("");      // box 1

  if (show) {
    const [age, setAge] = useState(0);       // ❌ box 2, but only sometimes
  }

  const [city, setCity] = useState("");      // box 2 or box 3?
}
```

If `show` changes between renders, the order changes. React gets confused and gives `city` the value that belonged to `age`. That's why the order must be the same on every render.

**Interview tip:** The ESLint plugin `eslint-plugin-react-hooks` catches these mistakes for you.

---

## Q3. How does `useState` work?

**Short definition:** `useState` gives a component a piece of state. It returns an array with two items: the current value and a function to update it.

```jsx
const [count, setCount] = useState(0);
```

- `count` is the current value.
- `setCount` updates it and triggers a re-render.
- `0` is the initial value. It is used **only on the first render**. After that, React remembers the latest value.

**Deeper explanation:**
Each time your component re-renders, the function runs again from top to bottom. But `useState` doesn't reset to `0`. React stores the value outside your function and hands it back.

---

## Q4. Why doesn't state update immediately after calling `setState`?

**Short definition:** State is a **snapshot** for each render. Calling `setState` schedules a new render. It doesn't change the variable you already have.

```jsx
function Counter() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(count + 1);
    console.log(count); // still 0, not 1
  }

  return <button onClick={handleClick}>{count}</button>;
}
```

**Deeper explanation:**
In this render, `count` is `0`. It is a fixed value, like a photo. `setCount(1)` says "please render again with 1." Only in the **next** render will `count` be `1`.

**Related question: what does this do?**

```jsx
setCount(count + 1);
setCount(count + 1);
setCount(count + 1);
```

The count goes up by **1**, not 3. All three lines see the same snapshot (`count = 0`), so all three say "set it to 1."

---

## Q5. What is the functional update form of `setState`? When do you need it?

**Short definition:** Instead of passing a value, you pass a function that receives the **latest** state and returns the new state.

```jsx
setCount((prev) => prev + 1);
setCount((prev) => prev + 1);
setCount((prev) => prev + 1);
// count goes up by 3
```

**Deeper explanation:**
React puts these updates in a queue and runs them in order. `prev` is always the result of the previous update, so nothing is lost.

**Use it when the new state depends on the old state**, especially inside:
- `setTimeout` or `setInterval`
- async code
- `useEffect`

```jsx
useEffect(() => {
  const id = setInterval(() => {
    setCount((c) => c + 1); // ✅ always fresh
  }, 1000);
  return () => clearInterval(id);
}, []);
```

If you wrote `setCount(count + 1)` here, `count` would be stuck at `0` forever, because the interval remembers the old snapshot. This is called a **stale closure**, and it's a favorite interview topic.

---

## Q6. What is batching?

**Short definition:** React groups multiple state updates into a **single re-render** for better performance.

```jsx
function handleClick() {
  setName("Asha");
  setAge(25);
  setCity("Pune");
  // only ONE re-render, not three
}
```

**Deeper explanation:**
Before React 18, batching only worked inside React event handlers. Updates inside `setTimeout`, promises, or native events caused separate re-renders. **React 18 introduced automatic batching**, so it now works everywhere.

---

## Q7. How do you update objects and arrays in state?

**Short definition:** Never change them directly (mutate). Always create a **new** object or array.

**Why?** React checks if state changed by comparing the old and new values with `Object.is`. If you mutate the same object, the reference is the same, so React thinks nothing changed and skips the re-render.

```jsx
// ❌ Wrong: same object reference
user.name = "Ravi";
setUser(user);

// ✅ Correct: new object
setUser({ ...user, name: "Ravi" });
```

**Arrays:**

```jsx
// Add
setItems([...items, newItem]);

// Remove
setItems(items.filter((item) => item.id !== id));

// Update one item
setItems(items.map((item) =>
  item.id === id ? { ...item, done: true } : item
));
```

Avoid `push`, `splice`, `sort`, and `reverse` directly on state, because they mutate the original array.

---

## Q8. What is lazy initialization in `useState`?

**Short definition:** If the initial value is expensive to compute, pass a **function** to `useState`. React will call it only once, on the first render.

```jsx
// ❌ runs on EVERY render (the result is ignored after the first)
const [data, setData] = useState(expensiveCalculation());

// ✅ runs only once
const [data, setData] = useState(() => expensiveCalculation());
```

**Deeper explanation:**
In the first version, `expensiveCalculation()` runs on every render because it's a normal function call inside your component. React just ignores the result after the first time. Wasted work. Passing a function fixes that.

A common real example is reading from `localStorage`:

```jsx
const [theme, setTheme] = useState(() => localStorage.getItem("theme") || "light");
```

---

## Q9. What is `useEffect`? Why do we need it?

**Short definition:** `useEffect` lets you run **side effects** after the component renders.

**What is a side effect?** Anything that reaches outside your component's render logic: fetching data, setting timers, subscribing to events, changing `document.title`, or touching `localStorage`.

**Why not do these directly in the component body?** Because the body runs on every render and must be **pure** (same input, same output, no surprises). A fetch call right in the body would fire on every render.

```jsx
useEffect(() => {
  document.title = `You clicked ${count} times`;
});
```

React runs this **after** the screen has been updated, so it doesn't block the user from seeing the UI.

---

## Q10. Explain the dependency array in `useEffect`.

**Short definition:** The dependency array tells React **when** to re-run the effect.

| Code | When it runs |
|---|---|
| `useEffect(() => {...})` | After **every** render |
| `useEffect(() => {...}, [])` | Only **once**, after the first render |
| `useEffect(() => {...}, [a, b])` | After first render, and whenever `a` or `b` changes |

**Deeper explanation:**
React compares each dependency with its value from the last render. If any of them changed, it runs the effect again.

**Golden rule:** Put **every** value from your component (props, state, functions) that the effect uses into the array. Don't lie to React to stop it from re-running. If the effect runs too often, fix the cause. Don't hide it.

**Don't say** "`[]` is like `componentDidMount`." It's close, but not the same. Effects are about **synchronizing** with something, not about "moments in life." Interviewers who know React well like hearing that.

---

## Q11. What is the cleanup function in `useEffect`?

**Short definition:** It's a function you return from the effect. React runs it **before the effect runs again** and **when the component unmounts**.

```jsx
useEffect(() => {
  const id = setInterval(() => console.log("tick"), 1000);

  return () => clearInterval(id); // cleanup
}, []);
```

**Deeper explanation:**
Without cleanup, you'd leave things running: timers keep ticking, event listeners pile up, WebSocket connections stay open. This is called a **memory leak**.

Another example:

```jsx
useEffect(() => {
  function handleResize() {
    console.log(window.innerWidth);
  }
  window.addEventListener("resize", handleResize);

  return () => window.removeEventListener("resize", handleResize);
}, []);
```

**Order of events when a dependency changes:**
1. Render with new values
2. Run cleanup from the **previous** effect
3. Run the new effect

---

## Q12. How do you fetch data with `useEffect`? What is a race condition here?

**Basic version:**

```jsx
function User({ id }) {
  const [user, setUser] = useState(null);

  useEffect(() => {
    fetch(`/api/users/${id}`)
      .then((res) => res.json())
      .then((data) => setUser(data));
  }, [id]);

  return <p>{user?.name}</p>;
}
```

**The problem:** Say `id` changes quickly from 1 to 2. Two requests are sent. If request 1 is slow and finishes **after** request 2, the screen ends up showing user 1's data while `id` is 2. That's a **race condition**.

**Fix with a cleanup flag:**

```jsx
useEffect(() => {
  let ignore = false;

  fetch(`/api/users/${id}`)
    .then((res) => res.json())
    .then((data) => {
      if (!ignore) setUser(data);
    });

  return () => {
    ignore = true; // old request's result is ignored
  };
}, [id]);
```

**Better fix:** Use `AbortController` to actually cancel the request. In real projects, many teams use libraries like **React Query** or **SWR**, which handle this (plus caching and retries) for you.

Note: `useEffect` can't take an `async` function directly, because async functions return a Promise, and React expects either nothing or a cleanup function. Define an async function **inside** the effect and call it.

---

## Q13. Why does my `useEffect` run twice in development?

**Short definition:** It's `React.StrictMode`. In development only, React mounts, unmounts, and mounts your component again on purpose.

**Why?** To check that your cleanup logic is correct. If your effect works fine when run twice, it's safe. If something breaks (like a duplicate subscription), you forgot the cleanup.

This **does not happen in production**. The fix is not to remove StrictMode. The fix is to write proper cleanup.

---

## Q14. What causes an infinite loop in `useEffect`?

**Short definition:** Updating state inside an effect that depends on that same state, or missing the dependency array.

```jsx
// ❌ Loop: effect sets state → re-render → effect runs → sets state...
useEffect(() => {
  setCount(count + 1);
});
```

**Another sneaky cause: objects and functions as dependencies.**

```jsx
function App() {
  const options = { page: 1 };   // new object on every render

  useEffect(() => {
    fetchData(options);
  }, [options]);                 // ❌ "changed" every render
}
```

Even though `{ page: 1 }` looks the same, it's a **new object** each time, and `Object.is` compares references. So React thinks it changed.

**Fixes:**
- Move the object outside the component, or inside the effect.
- Use primitive values (`[page]`) as dependencies.
- Use `useMemo` or `useCallback` (coming in Concept 4).

---

## Q15. Do you always need `useEffect`?

**Short definition:** No. Many people overuse it.

**Don't use an effect for:**

**1. Calculating values from props or state.** Just calculate during render.

```jsx
// ❌ Unnecessary effect
const [fullName, setFullName] = useState("");
useEffect(() => {
  setFullName(first + " " + last);
}, [first, last]);

// ✅ Just calculate it
const fullName = first + " " + last;
```

**2. Handling user events.** If something happens because of a click, do it in the click handler, not in an effect.

**Use an effect when** you need to sync with something **outside React**: the network, browser APIs, timers, third-party libraries.

Interviewers love this answer because it shows real-world experience.

---

## Q16. How do class lifecycle methods map to `useEffect`?

| Class | Hooks equivalent |
|---|---|
| `componentDidMount` | `useEffect(() => {...}, [])` |
| `componentDidUpdate` | `useEffect(() => {...}, [dep])` |
| `componentWillUnmount` | cleanup function returned from `useEffect` |

This mapping is handy for beginners. But remember from Q10: it's an approximation. A single effect with a cleanup handles "setup and teardown" together, which is the real idea behind it.

---

**That's Concept 3 done.** Reply **"next"** for **Concept 4: More Hooks (`useRef`, `useMemo`, `useCallback`, `useContext`, and custom hooks)**.

---
---
---
---


# Concept 4: More Hooks (`useRef`, `useMemo`, `useCallback`, `useContext`, Custom Hooks)

## Q1. What is `useRef`? What are its uses?

**Short definition:** `useRef` gives you a box that holds a value. You can change the value inside the box, and **changing it does not cause a re-render**.

```jsx
const ref = useRef(0);
console.log(ref.current); // 0
ref.current = 5;          // no re-render
```

**Deeper explanation:**
`useRef` returns an object like `{ current: 0 }`. React keeps the **same object** across all renders. So it's like state in one way (it remembers its value), but unlike state, changing it doesn't update the screen.

**Two main uses:**

**1. Getting access to a DOM element:**

```jsx
function SearchBox() {
  const inputRef = useRef(null);

  return (
    <>
      <input ref={inputRef} />
      <button onClick={() => inputRef.current.focus()}>Focus input</button>
    </>
  );
}
```

**2. Storing a value that must survive renders but doesn't affect the UI**, like a timer ID, a previous value, or a "has this run already" flag:

```jsx
const timerRef = useRef(null);

function start() {
  timerRef.current = setInterval(() => console.log("tick"), 1000);
}
function stop() {
  clearInterval(timerRef.current);
}
```

**Interview tip:** Don't read or write `ref.current` during rendering (except for the first-time setup). Do it in event handlers or effects. Render should stay pure.

---

## Q2. What is the difference between `useState` and `useRef`?

| | `useState` | `useRef` |
|---|---|---|
| Changing it re-renders? | Yes | No |
| Value survives re-renders? | Yes | Yes |
| Update timing | Next render (snapshot) | Immediately (`ref.current` is mutable) |
| Use for | Data shown on screen | Data *not* shown on screen, or DOM access |

**Simple rule:** If the user should **see** the change, use state. If it's just something you need to **remember**, use a ref.

---

## Q3. How can you get the previous value of a prop or state?

**Short definition:** Store the current value in a ref, and update it **after** each render using an effect.

```jsx
function usePrevious(value) {
  const ref = useRef();

  useEffect(() => {
    ref.current = value;
  }, [value]);

  return ref.current;
}
```

**Deeper explanation:**
Order matters here. During render, `ref.current` still holds the **old** value, so we return that. Only **after** the render finishes does the effect run and save the new value for next time. That's why this trick works.

---

## Q4. What is `useMemo`?

**Short definition:** `useMemo` remembers (caches) the **result of a calculation** and only recalculates it when its dependencies change.

```jsx
const filtered = useMemo(() => {
  return products.filter((p) => p.name.includes(search));
}, [products, search]);
```

**Deeper explanation:**
Your component function runs on every render. If you have a heavy calculation inside, like filtering 50,000 items, it repeats every time, even when nothing related to it changed. `useMemo` says: "Use the saved result unless `products` or `search` changed."

**Important:** `useMemo` is a **performance hint**, not a guarantee. React may throw away the cache in some situations. Your code must still work correctly without it.

---

## Q5. What is `useCallback`?

**Short definition:** `useCallback` remembers a **function** itself, so you get the **same function reference** between renders (unless dependencies change).

```jsx
const handleClick = useCallback(() => {
  console.log("Clicked", id);
}, [id]);
```

**Deeper explanation:**
In JavaScript, every time a component renders, functions written inside it are created **again as new functions**. Two functions that look identical are **not equal** (`() => {} === () => {}` is `false`).

Usually that's harmless. But it becomes a problem in two cases:

1. The function is passed to a child wrapped in `React.memo`. A new function each time means the child thinks its props changed and re-renders anyway.
2. The function is used as a dependency in `useEffect`, so the effect re-runs every render (remember the infinite-loop issue from Concept 3).

**Fun fact:** `useCallback(fn, deps)` is just `useMemo(() => fn, deps)`. It's a shortcut.

---

## Q6. What is `React.memo`? How does it work with `useCallback`?

**Short definition:** `React.memo` wraps a component so it **skips re-rendering** if its props are the same as last time.

```jsx
const Child = React.memo(function Child({ onClick }) {
  console.log("Child rendered");
  return <button onClick={onClick}>Click</button>;
});

function Parent() {
  const [count, setCount] = useState(0);

  // Without useCallback, Child re-renders every time Parent does
  const handleClick = useCallback(() => {
    console.log("clicked");
  }, []);

  return (
    <>
      <button onClick={() => setCount(count + 1)}>{count}</button>
      <Child onClick={handleClick} />
    </>
  );
}
```

**Deeper explanation:**
`React.memo` does a **shallow comparison** of props. If you pass a function, it compares the function's reference. A new function on each render breaks the memo. `useCallback` keeps the reference stable, so `React.memo` can do its job.

**Key point:** `useCallback` alone does nothing useful. It only helps when something else (`React.memo` or an effect dependency) cares about the reference.

---

## Q7. What is the difference between `useMemo` and `useCallback`?

| | `useMemo` | `useCallback` |
|---|---|---|
| Caches | The **result** of calling a function | The **function itself** |
| Returns | A value | A function |

```jsx
const value = useMemo(() => compute(a, b), [a, b]);   // value = compute's result
const fn = useCallback(() => compute(a, b), [a, b]);  // fn = the function, not called yet
```

---

## Q8. Should we wrap everything in `useMemo` and `useCallback`?

**Short answer:** No. It's a common mistake.

**Why not?**
- Memoization has a cost. React must store the value and compare dependencies on every render.
- For cheap calculations, the memo costs more than the work it saves.
- It makes code harder to read.

**Use them when:**
- The calculation is truly expensive (you measured it).
- You need a stable reference for `React.memo` or an effect dependency.

**Good interview answer:** "I first write simple code. If I see a real performance problem, I measure with React DevTools Profiler, then add memoization where it helps."

**Bonus:** The newer **React Compiler** can add much of this memoization automatically at build time. So manual `useMemo` and `useCallback` are needed less than before, though the ideas are still worth knowing.

---

## Q9. What is Context? What problem does it solve?

**Short definition:** Context lets you share data with many components **without passing props through every level**.

**The problem, called "prop drilling":**
Imagine `App → Layout → Sidebar → UserMenu → Avatar`. Only `Avatar` needs the `user`. Without context, you pass `user` through `Layout`, `Sidebar`, and `UserMenu`, even though they don't use it.

**Context fixes this in three steps:**

```jsx
// 1. Create
const ThemeContext = createContext("light");

// 2. Provide (wrap the part of the tree that needs it)
function App() {
  const [theme, setTheme] = useState("dark");
  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      <Page />
    </ThemeContext.Provider>
  );
}

// 3. Consume (any component below can read it)
function Button() {
  const { theme } = useContext(ThemeContext);
  return <button className={theme}>Click</button>;
}
```

**Good use cases:** theme, current user, language, and other data that many components need and that doesn't change very often.

---

## Q10. Does Context cause performance problems?

**Short definition:** Yes. When the Provider's `value` changes, **every component that uses that context re-renders**, even if it only cares about one small part of the value.

**Deeper explanation:**
There's a hidden trap here:

```jsx
<ThemeContext.Provider value={{ theme, setTheme }}>
```

The `{ theme, setTheme }` is a **new object on every render** of `App`. So all consumers re-render each time `App` renders, even if `theme` didn't change.

**Fixes:**
- Wrap the value in `useMemo`: `const value = useMemo(() => ({ theme, setTheme }), [theme]);`
- **Split contexts** by concern (one for user, one for theme).
- Keep frequently changing data (like input text) out of context.

**Interview tip:** Say "Context is for **passing** data, not a full state management tool." For complex, fast-changing state, tools like Redux or Zustand handle updates more efficiently (we'll cover this in Concept 7).

---

## Q11. What is a custom hook? Why create one?

**Short definition:** A custom hook is a normal JavaScript function whose name starts with `use` and that calls other hooks inside it.

**Deeper explanation:**
It lets you **reuse logic** (not UI) across components. Before hooks, we needed HOCs or render props for this. Now it's just a function.

**Example: `useLocalStorage`**

```jsx
function useLocalStorage(key, initialValue) {
  const [value, setValue] = useState(() => {
    const saved = localStorage.getItem(key);
    return saved ? JSON.parse(saved) : initialValue;
  });

  useEffect(() => {
    localStorage.setItem(key, JSON.stringify(value));
  }, [key, value]);

  return [value, setValue];
}

// Usage
function Settings() {
  const [theme, setTheme] = useLocalStorage("theme", "light");
  // ...
}
```

**Example: `useFetch`**

```jsx
function useFetch(url) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;
    setLoading(true);

    fetch(url)
      .then((res) => res.json())
      .then((json) => {
        if (!ignore) {
          setData(json);
          setLoading(false);
        }
      });

    return () => { ignore = true; };
  }, [url]);

  return { data, loading };
}
```

---

## Q12. Do two components using the same custom hook share state?

**Short definition:** No. Each call to a custom hook creates its **own separate** state.

**Deeper explanation:**
A custom hook shares **logic**, not **data**. If `ComponentA` and `ComponentB` both call `useLocalStorage("theme")`, each gets its own independent `useState` inside. Think of it like calling the same function twice, where each call has its own local variables.

If you actually want to share the data, use Context, lift state up to a common parent, or use a state library.

---

## Q13. Why must custom hook names start with `use`?

**Short definition:** It's a convention that lets React tools (and the ESLint plugin) know this function may call hooks, so they can check the Rules of Hooks for you.

Without the `use` prefix, the linter can't tell that your function has hooks inside, and it won't warn you if you call it inside a loop or condition. React itself doesn't check the name at runtime. The `use` prefix is for tooling and for humans reading the code.

---

## Q14. What is `forwardRef`? (Quick note)

**Short definition:** It lets a component pass a `ref` down to a DOM element inside it.

Normally, `ref` isn't a regular prop, so this used to fail:

```jsx
<MyInput ref={inputRef} />  // ref didn't reach the <input> inside
```

The old fix was wrapping the component in `forwardRef`. **In React 19, `ref` is a normal prop for function components**, so you can just write:

```jsx
function MyInput({ ref, ...props }) {
  return <input ref={ref} {...props} />;
}
```

If your project uses React 18 or older, you still need `forwardRef`.

---

**That's Concept 4 done.** Reply **"next"** for **Concept 5: Forms and Controlled vs Uncontrolled Components**.

---
---
---
---

# Concept 5: Forms, Controlled vs Uncontrolled Components

## Q1. What is a controlled component?

**Short definition:** A controlled component is a form input whose value is stored in React state. React is the "single source of truth."

```jsx
function NameForm() {
  const [name, setName] = useState("");

  return (
    <input
      value={name}
      onChange={(e) => setName(e.target.value)}
    />
  );
}
```

**Deeper explanation:**
Here is the flow, step by step:

1. The user types a letter.
2. `onChange` fires.
3. You call `setName` with the new text.
4. React re-renders, and the input shows the new `value` from state.

So the input never decides its own text. React does. The input just displays whatever state says.

**Why is this useful?**
- You can validate as the user types.
- You can change the input's value (for example, force uppercase, or clear it after submit).
- Other parts of the UI can read the value at any time.

**Common bug:** If you give an input a `value` but forget `onChange`, the input becomes **read-only**. You type and nothing happens. React will also show a warning in the console.

---

## Q2. What is an uncontrolled component?

**Short definition:** An uncontrolled component is a form input where the **DOM itself** keeps the value. You read it only when you need it, usually with a `ref`.

```jsx
function NameForm() {
  const inputRef = useRef(null);

  function handleSubmit(e) {
    e.preventDefault();
    alert(inputRef.current.value);
  }

  return (
    <form onSubmit={handleSubmit}>
      <input ref={inputRef} defaultValue="Asha" />
      <button>Submit</button>
    </form>
  );
}
```

**Deeper explanation:**
This works like plain HTML. The browser handles typing. React doesn't re-render on each keystroke. You only "ask" for the value at the end.

Notice the `defaultValue` prop. It sets the **starting** value only. Use `value` for controlled inputs and `defaultValue` for uncontrolled ones. Never mix both on the same input.

---

## Q3. Controlled vs Uncontrolled: what is the difference and when should I use each?

| | Controlled | Uncontrolled |
|---|---|---|
| Who holds the value? | React state | The DOM |
| Re-render on each keystroke? | Yes | No |
| Read value | From state | From ref (or `FormData`) |
| Initial value | `value` | `defaultValue` |
| Instant validation / formatting | Easy | Hard |

**Use controlled when:** you need live validation, disabled buttons based on input, formatting (like phone numbers), or dependent fields.

**Use uncontrolled when:** the form is simple, you only need values on submit, or you work with file inputs (`<input type="file" />` is always uncontrolled, because only the user can set a file).

**Interview tip:** Don't say "controlled is always better." Say "controlled gives more power, uncontrolled gives simplicity and less re-rendering. I choose based on need."

---

## Q4. How do you handle multiple inputs with one `onChange`?

**Short definition:** Give each input a `name` attribute, keep all values in one state object, and use `e.target.name` to know which field changed.

```jsx
function SignupForm() {
  const [form, setForm] = useState({ name: "", email: "" });

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  return (
    <form>
      <input name="name" value={form.name} onChange={handleChange} />
      <input name="email" value={form.email} onChange={handleChange} />
    </form>
  );
}
```

**Deeper explanation:**
Two things to notice:

- `[name]: value` is a **computed property name**. It uses the variable's value as the key. If `name` is `"email"`, it becomes `{ email: value }`.
- We spread `...prev` so the other fields are kept. Remember from Concept 3: never mutate state, always create a new object.

---

## Q5. How do you handle checkboxes, radio buttons, and dropdowns?

The idea is the same, but the property to read is different.

**Checkbox:** use `checked` and `e.target.checked`.

```jsx
<input
  type="checkbox"
  checked={agree}
  onChange={(e) => setAgree(e.target.checked)}
/>
```

**Radio buttons:** compare each option to the selected value.

```jsx
<input
  type="radio"
  value="male"
  checked={gender === "male"}
  onChange={(e) => setGender(e.target.value)}
/>
```

**Select (dropdown):** in React, put `value` on the `<select>` itself, not `selected` on the `<option>` like in HTML.

```jsx
<select value={country} onChange={(e) => setCountry(e.target.value)}>
  <option value="in">India</option>
  <option value="us">USA</option>
</select>
```

**Textarea:** in HTML you write text between the tags. In React, it works like an input, with a `value` prop.

```jsx
<textarea value={text} onChange={(e) => setText(e.target.value)} />
```

---

## Q6. How do you submit a form and validate it?

**Short definition:** Use the `onSubmit` handler on the `<form>`, call `e.preventDefault()`, then validate the state.

```jsx
function LoginForm() {
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");

  function handleSubmit(e) {
    e.preventDefault(); // stop page reload

    if (!email.includes("@")) {
      setError("Please enter a valid email");
      return;
    }

    setError("");
    console.log("Submitting", email);
  }

  return (
    <form onSubmit={handleSubmit}>
      <input value={email} onChange={(e) => setEmail(e.target.value)} />
      {error && <p style={{ color: "red" }}>{error}</p>}
      <button type="submit">Login</button>
    </form>
  );
}
```

**Deeper explanation:**
Use `onSubmit` on the form, not `onClick` on the button. This way, pressing **Enter** inside an input also submits the form, and it works better for accessibility.

**When to validate:**
- **On submit:** simple and not annoying.
- **On blur** (when the user leaves the field): good balance.
- **On every change:** instant feedback, but can annoy users who haven't finished typing.

---

## Q7. Why not use `useState` for every field in big forms? What are the alternatives?

**Short definition:** In a large form, controlled inputs re-render the whole form on every keystroke. Form libraries reduce this work and remove the repeated code.

**Deeper explanation:**
With 20 fields, writing state, handlers, and error logic for each is long and slow to maintain. Popular libraries:

- **React Hook Form:** uses uncontrolled inputs under the hood, so it re-renders very little. Very popular for performance.
- **Formik:** controlled approach, easy to learn, but heavier.
- **Zod or Yup:** not form libraries, but **schema validation** tools. You define the rules once (email must be valid, password at least 8 characters) and they check the data.

```jsx
const { register, handleSubmit, formState: { errors } } = useForm();

<input {...register("email", { required: "Email is required" })} />
{errors.email && <p>{errors.email.message}</p>}
```

**Interview tip:** Knowing *why* React Hook Form is fast (it uses refs and uncontrolled inputs) shows you understand this concept, not just the library.

---

## Q8. What are Actions and `useActionState` in React 19? (Modern note)

**Short definition:** React 19 lets you pass a **function** to a form's `action` prop. React runs it on submit, and gives you tools to track pending state and results.

```jsx
function Signup() {
  async function signup(formData) {
    const email = formData.get("email");
    await saveUser(email);
  }

  return (
    <form action={signup}>
      <input name="email" />
      <button>Sign up</button>
    </form>
  );
}
```

**Deeper explanation:**
Notice there is no `useState`, no `onChange`, and no `preventDefault`. The form works like an uncontrolled form, and React hands you a `FormData` object with all the values. After the action finishes, React also resets uncontrolled fields for you.

Related hooks:
- **`useActionState`:** gives you the latest result (like an error message) and a pending flag.
- **`useFormStatus`:** lets a child component (like a submit button) know if the parent form is submitting, so it can show "Saving...".
- **`useOptimistic`:** shows the expected result instantly while the request is still in progress.

If your project uses React 18 or older, use the classic `onSubmit` approach from Q6.

---

## Q9. Why should inputs have `label` and `htmlFor`?

**Short definition:** It connects the label text to the input. Clicking the label focuses the input, and screen readers read it aloud.

```jsx
<label htmlFor="email">Email</label>
<input id="email" />
```

Notice `htmlFor`, not `for`. Like `className`, this is because `for` is a reserved word in JavaScript.

You can also wrap the input inside the label, and then you don't need `id` and `htmlFor`:

```jsx
<label>
  Email
  <input />
</label>
```

Interviewers sometimes ask about this to test whether you care about **accessibility**.

---

**That's Concept 5 done.** Reply **"next"** for **Concept 6: Rendering, Reconciliation, and Performance**. This one is a favorite for senior-level rounds.

---
---
---

# Concept 6: Rendering, Reconciliation, and Performance

## Q1. What triggers a re-render in React?

**Short definition:** A component re-renders when its **state changes**, when its **parent re-renders**, or when a **context it uses changes**.

**Deeper explanation:**
Many beginners think a component re-renders only when its props change. That's not true. By default, when a parent renders, **all of its children render too**, even if their props are exactly the same.

```jsx
function Parent() {
  const [count, setCount] = useState(0);
  return (
    <>
      <button onClick={() => setCount(count + 1)}>{count}</button>
      <Child />  {/* re-renders on every click, even with no props */}
    </>
  );
}
```

`React.memo` (from Concept 4) is the tool that changes this default behavior.

---

## Q2. What is the difference between "render" and "commit"? Does a re-render mean the DOM changes?

**Short definition:** **Render** means React calls your component function to figure out what the UI should look like. **Commit** means React applies the changes to the real DOM. A re-render does **not** always change the DOM.

**Deeper explanation:**
React works in two phases:

1. **Render phase:** React calls your components and builds the new Virtual DOM. This must be **pure**, with no side effects. In concurrent mode, React may pause, restart, or throw away this work.
2. **Commit phase:** React updates the real DOM with only the differences, then runs effects.

If your component re-renders but produces the exact same output, React finds no differences and touches nothing in the DOM. So "my component re-rendered" does not mean "the browser repainted."

**Interview tip:** A re-render is not automatically a performance problem. It only matters if the render itself is slow (heavy calculations, huge lists).

---

## Q3. What is reconciliation? How does React's diffing work?

**Short definition:** Reconciliation is the process where React compares the old Virtual DOM tree with the new one and decides the smallest set of changes to make in the real DOM.

**Deeper explanation:**
Comparing two trees perfectly is very expensive. So React uses a fast algorithm built on **two assumptions**:

**1. Elements of different types produce different trees.**
If `<div>` becomes `<span>`, or `<Login />` becomes `<Dashboard />`, React does not try to compare inside. It **destroys the old one** (state lost, effects cleaned up) and **builds a new one**.

**2. Keys tell React which children are stable.**
For lists, `key` helps React match old and new items (we saw this in Concept 2).

**If the type is the same,** React keeps the same DOM node and component instance, and only updates what changed (props, attributes). **State is preserved.**

```jsx
// Same type at same position → state is kept
{isAdmin ? <Panel color="red" /> : <Panel color="blue" />}
```

Here, `Panel` keeps its state when `isAdmin` flips, because React sees the same component type in the same position.

---

## Q4. How can you reset a component's state using `key`?

**Short definition:** Change the `key`. React sees a new key as a brand new component, so it throws away the old one and creates a fresh one.

```jsx
<UserProfile key={userId} userId={userId} />
```

**Deeper explanation:**
Without the key, switching from user 1 to user 2 keeps the same `UserProfile` instance, so old state (like half-typed form text) stays. With `key={userId}`, each user gets a fresh component with fresh state.

This is often much cleaner than writing an effect that resets state when a prop changes.

---

## Q5. Why is defining a component inside another component a bad idea?

**Short definition:** Every time the outer component renders, the inner component is a **new function**, so React thinks it's a different type and **remounts it**. State is lost and performance suffers.

```jsx
function Parent() {
  // ❌ new function on every render
  function Child() {
    const [text, setText] = useState("");
    return <input value={text} onChange={(e) => setText(e.target.value)} />;
  }

  return <Child />;
}
```

Every time `Parent` re-renders, `Child` is a different type (from Q3, assumption 1). React destroys and recreates it. The input loses focus and text as you type.

**Fix:** Define components at the top level of the file, outside other components.

---

## Q6. What is the Fiber architecture?

**Short definition:** Fiber is React's internal engine (introduced in React 16) that breaks rendering work into small units, so React can pause, resume, and prioritize work.

**Deeper explanation:**
Before Fiber, React rendered everything in one go. A big update could block the main thread, and the page would freeze. With Fiber, React can:

- Split rendering into small chunks
- Pause work to handle something urgent, like a keystroke
- Give different updates different **priorities**

Fiber is what makes **concurrent features** possible (like `useTransition`, coming in Concept 10). You don't use Fiber directly. It's good to know the idea for interviews.

---

## Q7. Does calling `setState` with the same value cause a re-render?

**Short definition:** Usually no. React compares the new value with the old using `Object.is`. If they're the same, React **bails out**.

```jsx
const [count, setCount] = useState(0);
setCount(0); // same value → no meaningful re-render
```

**Deeper explanation:**
React may sometimes call your component one extra time before it decides to bail out, but it will **not** re-render the children or run effects. This is also why mutating an object and passing it back fails (Concept 3): the reference is the same, so React thinks nothing changed.

---

## Q8. How do you avoid unnecessary re-renders? (Key techniques)

Here are the techniques, from **most useful to least**. Notice that memoization is not first.

**1. Colocate state (move it down).**
Keep state as close as possible to where it's used.

```jsx
// ❌ typing re-renders the whole App
function App() {
  const [text, setText] = useState("");
  return (
    <>
      <input value={text} onChange={(e) => setText(e.target.value)} />
      <ExpensiveTree />
    </>
  );
}

// ✅ state lives in a small component
function SearchBox() {
  const [text, setText] = useState("");
  return <input value={text} onChange={(e) => setText(e.target.value)} />;
}

function App() {
  return (
    <>
      <SearchBox />
      <ExpensiveTree />
    </>
  );
}
```

**2. Pass content as `children`.**
When a wrapper's state changes, elements passed as `children` were created by the parent, so React can reuse them.

```jsx
function ColorWrapper({ children }) {
  const [color, setColor] = useState("red");
  return <div style={{ color }}>{children}</div>;
}

<ColorWrapper>
  <ExpensiveTree />   {/* does not re-render when color changes */}
</ColorWrapper>
```

**3. `React.memo`** for components that render often with the same props.

**4. `useMemo` / `useCallback`** to keep values and functions stable for memoized children.

**5. Split context** so consumers only re-render for data they care about.

---

## Q9. What is list virtualization (windowing)?

**Short definition:** Rendering only the items **visible on screen** (plus a few extra), instead of the whole list.

**Deeper explanation:**
If you render 10,000 rows, the browser creates 10,000 DOM nodes. That's slow and uses a lot of memory. With virtualization, only about 20 to 30 rows exist in the DOM at a time. As you scroll, React reuses those rows with new data.

Popular libraries: **react-window**, **react-virtualized**, **TanStack Virtual**.

Use it for long lists, big tables, and chat histories. It's often a much bigger win than `useMemo`.

---

## Q10. What is the difference between `useEffect` and `useLayoutEffect`?

**Short definition:** `useEffect` runs **after** the browser paints the screen. `useLayoutEffect` runs **before** the paint, right after React updates the DOM.

**Deeper explanation:**

| | `useEffect` | `useLayoutEffect` |
|---|---|---|
| Runs | After paint | After DOM update, before paint |
| Blocks painting? | No | Yes |
| Use for | Fetching, subscriptions, most things | Measuring the DOM, avoiding visual flicker |

**Example use case:** You want to measure a tooltip's size and position it correctly. If you use `useEffect`, the user may see the tooltip appear in the wrong place for a split second, then jump. With `useLayoutEffect`, you measure and fix the position **before** the user sees anything.

**Rule:** Start with `useEffect`. Switch to `useLayoutEffect` only if you see flicker. Because it blocks painting, heavy work inside it makes the app feel slow.

---

## Q11. How do you find performance problems in a React app?

**Short definition:** **Measure first.** Use the **React DevTools Profiler**.

**Steps:**
1. Open React DevTools and go to the **Profiler** tab.
2. Click record, do the slow action, then stop.
3. Look at which components rendered and how long each took.
4. Use "Why did this render?" to see the reason (props changed, state changed, parent rendered).

You can also use the browser's **Performance tab** for the bigger picture, and the `<Profiler>` component for measuring in code.

**Good interview answer:** "I don't guess. I profile, find the slow component, and fix that one thing. Then I profile again to confirm it helped."

---

## Q12. What are other common React performance tips?

- **Code splitting and lazy loading:** load code only when needed (Concept 8).
- **Debounce or throttle** expensive handlers, such as a search input calling an API on every keystroke.
- **Avoid heavy work in render.** Move it out, or use `useMemo` if it's truly expensive.
- **Use stable, unique keys** in lists.
- **Optimize images:** correct sizes, modern formats, lazy loading.
- **Keep bundle size small:** avoid importing giant libraries for one small function.
- **Use `useTransition` or `useDeferredValue`** to keep typing smooth while heavy updates happen in the background (Concept 10).

---

## Q13. What does it mean that components must be "pure"?

**Short definition:** A pure component gives the **same output for the same props, state, and context**, and does not change anything outside itself while rendering.

```jsx
let counter = 0;

// ❌ Impure: changes an outside variable during render
function Bad() {
  counter++;
  return <p>{counter}</p>;
}

// ✅ Pure
function Good({ value }) {
  return <p>{value}</p>;
}
```

**Deeper explanation:**
React may render a component more than once (StrictMode does it on purpose, and concurrent rendering may pause and restart). If render has side effects, you get unpredictable bugs. Side effects belong in **event handlers** and **effects**.

This is also why StrictMode double-renders in development: to help you catch impure code.

---

**That's Concept 6 done.** Reply **"next"** for **Concept 7: State Management (Context and Redux)**.

---
---
---
---

# Concept 7: State Management (Context, Redux, and Alternatives)

## Q1. What is "lifting state up"?

**Short definition:** When two components need the same data, you move the state to their **closest common parent**, and pass it down as props.

**Deeper explanation:**
Say you have two sibling components: a temperature input in Celsius and another in Fahrenheit. They must stay in sync. Neither can own the state, because the other can't see it. So the parent owns it.

```jsx
function Converter() {
  const [celsius, setCelsius] = useState(0);

  return (
    <>
      <CelsiusInput value={celsius} onChange={setCelsius} />
      <FahrenheitInput value={celsius * 9 / 5 + 32} onChange={(f) => setCelsius((f - 32) * 5 / 9)} />
    </>
  );
}
```

This is the **first tool** to reach for. Before Context or Redux, ask: "Can I just lift the state up?"

---

## Q2. What is prop drilling? What are its solutions?

**Short definition:** Passing props through many middle components that don't use them, just to reach a deep child.

**Solutions, in order of simplicity:**
1. **Composition:** pass the deep component itself as `children` or a prop, so the middle layers don't need to know about the data.
2. **Context:** for data many components need (theme, user, language).
3. **State library:** Redux, Zustand, etc., for complex shared state.

**Interview tip:** Prop drilling over 2 or 3 levels is fine. Don't rush to add a library for a small problem.

---

## Q3. When is Context enough, and when do you need something like Redux?

**Context is enough when:**
- The data changes rarely (theme, logged-in user, language).
- The app is small or medium.

**Consider Redux or similar when:**
- Many parts of the app read and write the same state.
- State changes often, and Context would re-render too many components (see Concept 4, Q10).
- You need strong debugging tools, such as time-travel and action logs.
- Many developers work on the same codebase and need clear rules.

**Key point:** Context is a way to **deliver** data. It doesn't come with tools for updating, caching, or optimizing that data. State libraries do.

---

## Q4. What is Redux? What are its core principles?

**Short definition:** Redux is a **predictable state container**. It keeps your whole app's state in **one place** (a store), and changes it only through a strict, clear process.

**Three principles:**
1. **Single source of truth:** one store holds all global state.
2. **State is read-only:** you can't change it directly. You send an **action** describing what happened.
3. **Changes are made by pure functions:** a **reducer** takes the old state and an action, and returns the new state.

**The flow (one-way):**

```
UI → dispatch(action) → reducer → new state in store → UI updates
```

**Simple example of a reducer:**

```js
function counterReducer(state = { count: 0 }, action) {
  switch (action.type) {
    case "increment":
      return { count: state.count + 1 };
    default:
      return state;
  }
}
```

Notice the reducer **returns a new object** and never mutates. Same immutability rule as before.

---

## Q5. What is Redux Toolkit (RTK)? Why is it recommended?

**Short definition:** Redux Toolkit is the **official, modern way** to write Redux. It removes most of the boilerplate.

Old Redux needed separate action types, action creators, and switch statements. RTK simplifies this with `createSlice`:

```jsx
import { createSlice, configureStore } from "@reduxjs/toolkit";

const counterSlice = createSlice({
  name: "counter",
  initialState: { count: 0 },
  reducers: {
    increment(state) {
      state.count += 1; // looks like mutation, but it's safe
    },
    add(state, action) {
      state.count += action.payload;
    },
  },
});

export const { increment, add } = counterSlice.actions;

export const store = configureStore({
  reducer: { counter: counterSlice.reducer },
});
```

**Wait, isn't mutating state forbidden?**
Inside `createSlice`, RTK uses a library called **Immer**. Immer gives you a draft copy. You "mutate" the draft, and Immer produces a correct new immutable object behind the scenes. So you write simple code, and it stays safe.

**Using it in components:**

```jsx
import { useSelector, useDispatch } from "react-redux";

function Counter() {
  const count = useSelector((state) => state.counter.count);
  const dispatch = useDispatch();

  return <button onClick={() => dispatch(increment())}>{count}</button>;
}
```

- `useSelector` reads data from the store.
- `useDispatch` gives you the function to send actions.

---

## Q6. How does `useSelector` avoid unnecessary re-renders?

**Short definition:** It re-renders your component only when the **selected value** changes (compared by reference).

```jsx
// ✅ Re-renders only when count changes
const count = useSelector((state) => state.counter.count);

// ❌ Returns a new object every time → re-renders on every store update
const data = useSelector((state) => ({ count: state.counter.count }));
```

**Tip:** Select the **smallest piece** you need. Select primitives when possible. This is the big advantage over Context, where every consumer re-renders when the value changes.

---

## Q7. What is middleware in Redux? What is a thunk?

**Short definition:** Middleware sits between **dispatching an action** and the **reducer**. It can log, delay, or run async code.

**Reducers must be pure, so they can't fetch data.** That's where a **thunk** comes in. A thunk is a function you dispatch instead of a plain action. It can do async work, then dispatch normal actions.

```js
export const fetchUser = (id) => async (dispatch) => {
  dispatch({ type: "user/loading" });
  const res = await fetch(`/api/users/${id}`);
  const data = await res.json();
  dispatch({ type: "user/loaded", payload: data });
};
```

Redux Toolkit includes thunk by default and offers `createAsyncThunk` to reduce the boilerplate. For data fetching specifically, **RTK Query** is now recommended (see Q9).

---

## Q8. How does `useReducer` work? How is it different from `useState`?

**Short definition:** `useReducer` is a built-in hook for managing state with a reducer function, like a mini Redux inside one component.

```jsx
function reducer(state, action) {
  switch (action.type) {
    case "increment": return { count: state.count + 1 };
    case "reset":     return { count: 0 };
    default:          return state;
  }
}

function Counter() {
  const [state, dispatch] = useReducer(reducer, { count: 0 });

  return (
    <>
      <p>{state.count}</p>
      <button onClick={() => dispatch({ type: "increment" })}>+</button>
      <button onClick={() => dispatch({ type: "reset" })}>Reset</button>
    </>
  );
}
```

**When to prefer `useReducer` over `useState`:**
- State is an object with many related fields.
- The next state depends on complex logic.
- Many different actions change the same state.

**Bonus:** The `dispatch` function has a **stable reference**, so passing it to children doesn't break `React.memo`.

**Popular pattern:** `useReducer` + Context gives you a small Redux-like setup without any library.

---

## Q9. What is the difference between client state and server state?

**Short definition:**
- **Client state:** data that only exists in the browser (is the modal open? which tab is selected? what is typed in the input?).
- **Server state:** data that lives on a server and you only hold a **copy** (users, products, orders).

**Deeper explanation:**
Server state has special problems: caching, refetching when stale, loading and error states, retries, and keeping the copy in sync. Putting all of this into Redux by hand is a lot of work.

That's why tools like **TanStack Query (React Query)**, **SWR**, and **RTK Query** exist. They manage server state for you.

```jsx
const { data, isLoading, error } = useQuery({
  queryKey: ["user", id],
  queryFn: () => fetch(`/api/users/${id}`).then((r) => r.json()),
});
```

**Good interview answer:** "I use React Query for server data, and keep only real client state in `useState`, Context, or a small store. This removes most of what we used to put in Redux."

---

## Q10. What are other state management options besides Redux?

| Library | Idea | Good for |
|---|---|---|
| **Zustand** | Tiny store, no Provider, simple hooks | Most small to medium apps |
| **Jotai / Recoil** | Small pieces of state called atoms | Fine-grained updates |
| **MobX** | Observable state that updates automatically | Teams who like OOP style |
| **Context + useReducer** | Built-in, no library | Small apps |

**Zustand example:**

```jsx
import { create } from "zustand";

const useStore = create((set) => ({
  count: 0,
  increment: () => set((s) => ({ count: s.count + 1 })),
}));

function Counter() {
  const count = useStore((s) => s.count);
  const increment = useStore((s) => s.increment);
  return <button onClick={increment}>{count}</button>;
}
```

**Interview tip:** No library is "the best." A strong answer explains the **trade-offs** and how you choose based on app size and team.

---

## Q11. What is a selector? Why use `createSelector` (reselect)?

**Short definition:** A selector is a function that reads and/or calculates data from the store.

**Problem:** If a selector does heavy calculation (like filtering a big list), it runs on every store update.

**Fix:** `createSelector` **memoizes** the result. It recalculates only if its inputs change.

```js
import { createSelector } from "@reduxjs/toolkit";

const selectTodos = (state) => state.todos;
const selectFilter = (state) => state.filter;

const selectVisibleTodos = createSelector(
  [selectTodos, selectFilter],
  (todos, filter) => todos.filter((t) => t.status === filter)
);
```

It works like `useMemo`, but at the store level and shared across components.

---

## Q12. What should go in global state and what should stay local?

**Rule of thumb:** Keep state **as local as possible**. Move it up or out only when needed.

**Local (`useState`):**
- Form input text
- Is a dropdown open?
- Hover, focus, small UI toggles

**Global (Context or store):**
- Logged-in user
- Theme, language
- Shopping cart
- Data many distant components share

Putting everything in Redux is a common beginner mistake. It adds complexity and extra re-renders for no benefit.

---

## Q13. Redux vs Context API: what is the difference?

| | Context API | Redux |
|---|---|---|
| What it is | A way to pass data down | A full state management system |
| Setup | Very small, built-in | Needs a library (RTK) |
| Updates and logic | You build them yourself | Reducers, actions, middleware |
| Re-render control | All consumers re-render on value change | Selectors limit re-renders |
| Dev tools | None built-in | Redux DevTools (time-travel, action log) |
| Best for | Low-frequency data (theme, auth) | Large apps with complex, frequent updates |

**Good interview answer:** "They solve different problems. Context removes prop drilling. Redux manages complex state changes in a predictable way. I often use both: Context for things like theme, and a store or React Query for heavier data."

---

**That's Concept 7 done.** Reply **"next"** for **Concept 8: Routing, Code Splitting, and Lazy Loading**.

---
---
---
---

# Concept 8: Routing, Code Splitting, and Lazy Loading

## Q1. What is routing in React? How does React Router work?

**Short definition:** Routing means showing different components for different URLs, without reloading the page. React itself has no router, so most apps use **React Router**.

**Deeper explanation:**
React apps are usually **Single Page Applications (SPAs)**. The browser loads one HTML page. When you click a link, JavaScript changes the URL (using the browser's History API) and React swaps the components on screen. No full page reload happens, so navigation feels fast.

```jsx
import { BrowserRouter, Routes, Route, Link } from "react-router-dom";

function App() {
  return (
    <BrowserRouter>
      <nav>
        <Link to="/">Home</Link>
        <Link to="/about">About</Link>
      </nav>

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/about" element={<About />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  );
}
```

- `BrowserRouter` enables routing for the whole app.
- `Routes` looks at the URL and picks the matching `Route`.
- `path="*"` catches every URL that didn't match, so it works as a 404 page.

**Note:** In newer versions (v6.4+), React Router also offers `createBrowserRouter` and `RouterProvider`, which support data loading and actions. Also, in v7 you import from `react-router` instead of `react-router-dom`. Check which version your project uses.

---

## Q2. What is the difference between `<Link>` and `<a>`?

**Short definition:** `<a>` reloads the whole page. `<Link>` changes the URL and updates the UI **without** a reload.

With `<a href="/about">`, the browser asks the server for a new page, and all React state is lost. With `<Link to="/about">`, React Router intercepts the click, updates the URL, and renders the new component. State in components that stay mounted (like a navbar) is kept.

**`NavLink`** is a special `Link` that knows if it's the current page, so you can style the active link.

---

## Q3. How do you read URL parameters and query strings?

**URL params** (like `/users/42`):

```jsx
<Route path="/users/:id" element={<User />} />

function User() {
  const { id } = useParams();
  return <p>User ID: {id}</p>;
}
```

**Query strings** (like `/search?q=react&page=2`):

```jsx
import { useSearchParams } from "react-router-dom";

function Search() {
  const [params, setParams] = useSearchParams();
  const q = params.get("q");

  return <button onClick={() => setParams({ q: "hooks" })}>{q}</button>;
}
```

**Remember:** Values from the URL are always **strings**. If you need a number, convert it: `Number(id)`.

**Tip:** Putting filters and page numbers in the query string is great. Users can share the link or refresh, and the same view returns.

---

## Q4. How do you navigate programmatically?

**Short definition:** Use the `useNavigate` hook.

```jsx
function Login() {
  const navigate = useNavigate();

  async function handleSubmit() {
    await loginUser();
    navigate("/dashboard");
  }
  // ...
}
```

- `navigate(-1)` goes back one page.
- `navigate("/home", { replace: true })` replaces the current history entry, so the back button won't return to the login page.

For redirecting during render, use `<Navigate to="/login" />`.

---

## Q5. How do you create nested routes and layouts?

**Short definition:** Put `Route` elements inside other `Route` elements, and use `<Outlet />` in the parent to show the child.

```jsx
<Routes>
  <Route path="/dashboard" element={<DashboardLayout />}>
    <Route index element={<Overview />} />
    <Route path="settings" element={<Settings />} />
    <Route path="profile" element={<Profile />} />
  </Route>
</Routes>

function DashboardLayout() {
  return (
    <>
      <Sidebar />
      <main>
        <Outlet />  {/* child route renders here */}
      </main>
    </>
  );
}
```

**Deeper explanation:**
The `Sidebar` stays on screen while only the `Outlet` area changes. This avoids repeating the same layout in every page. The `index` route is what shows at the parent's exact path (`/dashboard`).

---

## Q6. How do you protect routes (private routes)?

**Short definition:** Create a wrapper component that checks if the user is logged in. If yes, show the page. If not, redirect to login.

```jsx
function ProtectedRoute({ children }) {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

<Route
  path="/dashboard"
  element={
    <ProtectedRoute>
      <Dashboard />
    </ProtectedRoute>
  }
/>
```

**Important interview point:** This is only **UI protection**. A clever user can bypass frontend checks. The real security must be on the **server**, which must verify the token on every API request.

---

## Q7. What is code splitting? Why do we need it?

**Short definition:** Code splitting means breaking your JavaScript bundle into smaller pieces, and loading each piece only when needed.

**Deeper explanation:**
By default, tools like Vite or Webpack combine all your code into one big file. A user visiting only the home page still downloads code for the admin panel, charts, and settings. That makes the first load slow.

With code splitting, the home page loads only home page code. Other parts are downloaded later, when the user goes there.

Result: **faster first load** and better user experience, especially on slow networks.

---

## Q8. How do `React.lazy` and `Suspense` work?

**Short definition:** `React.lazy` loads a component only when it is first rendered. `Suspense` shows a fallback (like a spinner) while it loads.

```jsx
import { lazy, Suspense } from "react";

const Dashboard = lazy(() => import("./Dashboard"));

function App() {
  return (
    <Suspense fallback={<p>Loading...</p>}>
      <Dashboard />
    </Suspense>
  );
}
```

**How it works step by step:**
1. `import("./Dashboard")` is a **dynamic import**. It returns a Promise.
2. The bundler puts `Dashboard` in its own separate file (a "chunk").
3. When React first tries to render `Dashboard`, it starts downloading the chunk.
4. Until it arrives, `Suspense` shows the fallback.
5. Once loaded, the real component appears.

**Rules:**
- The lazy component must be a **default export**. For named exports, wrap it: `lazy(() => import("./X").then(m => ({ default: m.X })))`.
- `Suspense` must be **above** the lazy component in the tree.

---

## Q9. Where should you apply lazy loading?

**Best place: routes.** Each page becomes its own chunk.

```jsx
const Home = lazy(() => import("./pages/Home"));
const Admin = lazy(() => import("./pages/Admin"));

<Suspense fallback={<Spinner />}>
  <Routes>
    <Route path="/" element={<Home />} />
    <Route path="/admin" element={<Admin />} />
  </Routes>
</Suspense>
```

**Other good candidates:**
- Heavy components that are not visible at first (modals, charts, rich text editors)
- Large libraries used in one place

**Not good for:** small components or things needed immediately. Splitting too much creates many small requests, and it can hurt more than help.

---

## Q10. What if the lazy chunk fails to load?

**Short definition:** Wrap it in an **Error Boundary**.

The download can fail (network drop, or a new deployment removed the old chunk file). If nothing handles the error, the whole app crashes with a white screen.

```jsx
<ErrorBoundary fallback={<p>Something went wrong. Please refresh.</p>}>
  <Suspense fallback={<Spinner />}>
    <Dashboard />
  </Suspense>
</ErrorBoundary>
```

We'll cover Error Boundaries in detail in Concept 9.

---

## Q11. How can you preload a lazy component?

**Short definition:** Start the import **before** the user needs it, so it feels instant.

```jsx
const loadAdmin = () => import("./pages/Admin");
const Admin = lazy(loadAdmin);

<Link to="/admin" onMouseEnter={loadAdmin}>Admin</Link>
```

When the user hovers over the link, the chunk starts downloading. By the time they click, it's often ready. Calling `import()` again later does not download twice, because the browser caches the module.

---

## Q12. What is the difference between Client-Side Rendering (CSR), Server-Side Rendering (SSR), and Static Site Generation (SSG)?

| | CSR | SSR | SSG |
|---|---|---|---|
| Where HTML is built | In the browser | On the server, per request | At build time |
| First load | Slower (blank until JS runs) | Faster, content visible early | Fastest |
| SEO | Weaker (though search engines have improved) | Good | Good |
| Data freshness | Fresh | Fresh | Can be stale until rebuilt |
| Server cost | Low | Higher | Low |

**Deeper explanation:**
- **CSR:** The server sends an almost empty HTML file plus JavaScript. The browser runs React and builds the page. Plain Create React App or Vite apps work this way.
- **SSR:** The server runs React, builds the HTML, and sends it ready. The browser shows it quickly, then React **hydrates** it (attaches event listeners to make it interactive).
- **SSG:** Pages are built once during deployment and served as plain files. Great for blogs and docs.

Frameworks like **Next.js**, **Remix**, and **React Router v7** support these modes. Many apps mix them page by page.

---

## Q13. What is hydration?

**Short definition:** Hydration is when React takes server-rendered HTML and **attaches JavaScript behavior** (event handlers, state) to it, without rebuilding the DOM.

**Analogy:** The server sends a fully built house (HTML). Hydration is switching on the electricity so lights and switches work.

**Common problem: hydration mismatch.** If the HTML from the server differs from what React renders on the client (for example, using `Date.now()`, `Math.random()`, or `window` during render), React shows a warning and may re-render. Fix it by keeping render output the same on both sides, and moving browser-only logic into `useEffect`.

---

**That's Concept 8 done.** Reply **"next"** for **Concept 9: Advanced Patterns (HOC, Render Props, Error Boundaries, Portals)**.

---
---
---


# Concept 9: Advanced Patterns (HOC, Render Props, Error Boundaries, Portals)

## Q1. What is a Higher-Order Component (HOC)?

**Short definition:** A HOC is a function that takes a component and returns a **new, improved component**.

```jsx
const EnhancedComponent = withSomething(OriginalComponent);
```

**Deeper explanation:**
It's the same idea as a higher-order function in JavaScript (a function that takes or returns another function). A HOC lets you **reuse logic** across many components without repeating it.

**Example: `withAuth`**

```jsx
function withAuth(WrappedComponent) {
  return function AuthGuard(props) {
    const { user } = useAuth();

    if (!user) return <Navigate to="/login" />;
    return <WrappedComponent {...props} user={user} />;
  };
}

const ProtectedDashboard = withAuth(Dashboard);
```

**Rules of using HOCs:**
- **Don't mutate** the original component. Wrap it.
- **Pass through props** (`{...props}`) so the wrapped component still gets what it expects.
- **Don't create a HOC inside render.** A new component type is created each time, and React remounts it (same problem as Concept 6, Q5).
- Copy static methods if needed, and set a `displayName` for easier debugging.

**Real-world examples:** `connect()` from old Redux, `withRouter` from old React Router, `React.memo` (which is a kind of HOC).

---

## Q2. What is the Render Props pattern?

**Short definition:** A component receives a **function as a prop**, and calls it to decide what to render. This lets the component share its logic while you control the UI.

```jsx
function MouseTracker({ render }) {
  const [pos, setPos] = useState({ x: 0, y: 0 });

  return (
    <div onMouseMove={(e) => setPos({ x: e.clientX, y: e.clientY })}>
      {render(pos)}
    </div>
  );
}

<MouseTracker render={(pos) => <p>Mouse at {pos.x}, {pos.y}</p>} />
```

The prop doesn't have to be named `render`. Passing a function as `children` is also common:

```jsx
<MouseTracker>
  {(pos) => <p>{pos.x}, {pos.y}</p>}
</MouseTracker>
```

**Deeper explanation:**
`MouseTracker` owns the **logic** (tracking the mouse). The caller owns the **UI** (what to show). That's a clean split.

---

## Q3. HOC vs Render Props vs Custom Hooks: which should I use today?

| | HOC | Render Props | Custom Hook |
|---|---|---|---|
| Style | Wraps a component | Function as a prop | Plain function |
| Readability | Can get confusing | Can cause "callback nesting" | Very clean |
| Naming clashes | Props may collide | No | No |
| Debug tree | Extra wrapper layers | Extra wrapper layers | No extra layers |

**Short answer:** In modern React, **custom hooks are the first choice** for sharing logic. They are simpler and don't add extra layers to the component tree.

Still, HOCs and render props are useful when:
- You work with **class components** (they can't use hooks).
- The logic also needs to **render UI or wrap it** (for example, a layout or guard).
- You maintain older codebases or libraries that use them.

**Interview tip:** Interviewers often ask this to see if you know the *history*. A good answer: "Patterns evolved from mixins, to HOCs, to render props, to hooks. Each solved problems with the previous one."

---

## Q4. What is an Error Boundary?

**Short definition:** An Error Boundary is a component that **catches JavaScript errors in its child components** during rendering, and shows a fallback UI instead of crashing the whole app.

**Why needed?** Without it, an error in one small component (like reading `undefined.name`) can unmount the **entire React tree**, giving the user a blank white screen.

**It must be a class component** (there is no hook for this yet). It uses two special methods:

```jsx
class ErrorBoundary extends React.Component {
  state = { hasError: false };

  static getDerivedStateFromError(error) {
    return { hasError: true };  // switch to fallback UI
  }

  componentDidCatch(error, info) {
    logToService(error, info.componentStack);  // report the error
  }

  render() {
    if (this.state.hasError) return <h2>Something went wrong.</h2>;
    return this.props.children;
  }
}

<ErrorBoundary>
  <Profile />
</ErrorBoundary>
```

- `getDerivedStateFromError` runs during render and updates state to show the fallback.
- `componentDidCatch` runs after, and is for **side effects like logging**.

**Practical tip:** Most people don't write this by hand. They use the **`react-error-boundary`** library, which gives a ready component with reset support and works with function components.

---

## Q5. What errors do Error Boundaries NOT catch?

This is a **very common interview question.**

Error Boundaries do **not** catch errors in:
1. **Event handlers** (like `onClick`). Use normal `try/catch`.
2. **Asynchronous code** (`setTimeout`, promises, `fetch`).
3. **Server-side rendering.**
4. **Errors thrown in the boundary itself** (only errors from its children).

**Why?** Event handlers don't run during rendering, so React isn't in the middle of building the UI when they fail. React doesn't need to decide what to show, so it leaves them to you.

```jsx
function Button() {
  function handleClick() {
    try {
      riskyThing();
    } catch (e) {
      setError(e.message);  // handle it yourself
    }
  }
  return <button onClick={handleClick}>Go</button>;
}
```

**Trick:** To send an async error to the nearest boundary, catch it and rethrow it during render (for example, save it in state and `throw` it when it's set).

---

## Q6. Where should you place Error Boundaries?

**Short answer:** At more than one level.

- **One at the top** of the app, as a last safety net ("Something went wrong, please refresh").
- **Around big sections** (sidebar, main content, a widget) so a crash in one part doesn't kill the rest.
- **Around risky or third-party components,** like charts or lazy-loaded chunks (as we saw in Concept 8).

Think of them like **circuit breakers** in a house. One faulty appliance trips its own breaker, and the rest of the house keeps its power.

**Bonus:** A good fallback offers a **"Try again"** button that resets the boundary.

---

## Q7. What are Portals? Why do we need them?

**Short definition:** A Portal lets a component render its children into a **different place in the DOM**, outside its parent's DOM node, while staying in the same place in the **React tree**.

```jsx
import { createPortal } from "react-dom";

function Modal({ children }) {
  return createPortal(
    <div className="modal">{children}</div>,
    document.body
  );
}
```

**Deeper explanation:**
Imagine a modal inside a parent with `overflow: hidden` or a low `z-index`. The modal gets **clipped or hidden behind other elements**. CSS can't easily escape that. With a Portal, the modal's HTML is placed directly in `document.body` (or a special `<div id="modal-root">`), so it sits above everything.

**Great use cases:** modals, tooltips, dropdowns, toasts, and popovers.

---

## Q8. Do events and context work through Portals?

**Short answer:** Yes. Even though the DOM node is elsewhere, the Portal behaves like a normal child in the **React tree**.

- **Events bubble** up through the React parents, not the DOM parents. A click inside the modal reaches the `onClick` of the component that rendered the Portal.
- **Context** works too. The modal can read a `ThemeContext` from its React ancestors.

```jsx
<div onClick={() => console.log("Parent got the click")}>
  <Modal>
    <button>Click me</button>   {/* click still bubbles to the div above */}
  </Modal>
</div>
```

This is useful, but can surprise you. If you don't want it, call `e.stopPropagation()`.

---

## Q9. What is the Compound Components pattern?

**Short definition:** A group of components that **work together and share hidden state**, giving the user a flexible, clean API. Think of HTML's `<select>` and `<option>`.

```jsx
<Tabs>
  <Tabs.List>
    <Tabs.Tab id="a">Profile</Tabs.Tab>
    <Tabs.Tab id="b">Settings</Tabs.Tab>
  </Tabs.List>
  <Tabs.Panel id="a">Profile content</Tabs.Panel>
  <Tabs.Panel id="b">Settings content</Tabs.Panel>
</Tabs>
```

**How it works inside:** The parent `Tabs` holds the state (which tab is active) and shares it with the children using **Context**. The user doesn't pass any props between them.

**Why it's nice:** The user controls the layout and order, and doesn't need a long list of props such as `tabs={[...]}` and `renderItem={...}`. Libraries like **Radix UI**, **Headless UI**, and **Reach UI** use this pattern heavily.

---

## Q10. What is the Controlled Props / State Reducer pattern? (Quick note)

These are patterns for building **reusable component libraries**.

- **Controlled props:** The component can work on its own (uncontrolled), or the parent can take over its state by passing `value` and `onChange`. This is the same idea as controlled inputs in Concept 5.
- **State reducer:** The component lets the user **customize how its internal state changes** by passing their own reducer. It gives power without adding many props.

You don't need these daily, but mentioning them in a senior interview shows design thinking.

---

## Q11. What is `children` prop and the "Container/Presentational" pattern?

**`children`:** Whatever you put between a component's opening and closing tags is available as `props.children`. This is the base of composition in React.

```jsx
function Card({ children }) {
  return <div className="card">{children}</div>;
}

<Card><h2>Title</h2><p>Body</p></Card>
```

**Container/Presentational (older pattern):**
- **Presentational:** only shows UI, gets everything by props.
- **Container:** fetches data, holds state, and passes it down.

Today, **custom hooks** often replace the container. The idea (separating logic from UI) is still good.

**Interview tip:** Say "React prefers **composition over inheritance**." We build complex UI by combining small components, not by extending classes.

---

## Q12. What is `React.PureComponent` vs `React.Component`? (Class components)

**Short definition:** `PureComponent` automatically implements `shouldComponentUpdate` with a **shallow comparison** of props and state. It skips re-rendering if nothing changed.

- `React.memo` is the function-component version of this idea.
- `Component` re-renders whenever the parent renders or `setState` is called.

**Warning:** The comparison is **shallow**. If you mutate an object or array and pass the same reference, `PureComponent` will not update. This links back to the immutability rule from Concept 3.

---

## Q13. What are Refs and `useImperativeHandle`?

**Short definition:** `useImperativeHandle` controls **what a parent can access** through a ref to a child. Instead of giving the parent the whole DOM node, you expose only a few methods.

```jsx
function FancyInput({ ref }) {
  const inputRef = useRef();

  useImperativeHandle(ref, () => ({
    focus() {
      inputRef.current.focus();
    },
  }));

  return <input ref={inputRef} />;
}

// Parent can only call ref.current.focus(), nothing else
```

**Deeper explanation:**
It keeps a clean boundary: the parent gets a small, safe API and can't mess with the internals. Use it rarely. In most cases, passing props is the better React way. (On React 18 or older, wrap the component in `forwardRef`, as we saw in Concept 4.)

---

**That's Concept 9 done.** Reply **"next"** for the final one, **Concept 10: React 18/19 Features (Concurrent Rendering, Suspense, Server Components)**.

---
---
---

# Concept 10: React 18/19 Features (Concurrent Rendering, Suspense, Server Components)

## Q1. What is Concurrent Rendering?

**Short definition:** Concurrent rendering lets React **pause, resume, or throw away** a render in progress, so urgent things like typing and clicking are never blocked by slow work.

**Deeper explanation:**
Before React 18, once React started rendering, it had to finish. If the render was heavy, the page froze. Now React can work on a big update in small pieces. If the user types something, React pauses the big update, handles the keystroke first, and then continues.

Think of a cook making a big meal. If a customer asks for a glass of water, the cook stops chopping, serves the water, then goes back to chopping.

Concurrency is not something you turn on with a flag. You get it by using `createRoot` (React 18+) and then using features built on it, like `useTransition` and `useDeferredValue`.

```jsx
import { createRoot } from "react-dom/client";
createRoot(document.getElementById("root")).render(<App />);
```

This is also why render must be **pure** (Concept 6). React may run your component several times, or abandon a render halfway.

---

## Q2. What is `useTransition`?

**Short definition:** `useTransition` lets you mark a state update as **non-urgent**. React keeps the UI responsive and does that update in the background.

```jsx
function Search() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [isPending, startTransition] = useTransition();

  function handleChange(e) {
    setQuery(e.target.value);               // urgent: input must feel instant

    startTransition(() => {
      setResults(filterHugeList(e.target.value));  // not urgent
    });
  }

  return (
    <>
      <input value={query} onChange={handleChange} />
      {isPending && <p>Updating...</p>}
      <ResultList items={results} />
    </>
  );
}
```

**Deeper explanation:**
Without the transition, typing in a big filtered list feels laggy because each keystroke waits for the heavy render. With it:
- The input updates right away (urgent).
- The list update runs at low priority. If the user types again, React **drops the old list render** and starts a new one.
- `isPending` tells you when the background work is running.

**Good to know:** In React 19, `startTransition` also accepts **async functions**. These are called "Actions", and React tracks the pending state for you until the async work finishes.

---

## Q3. What is `useDeferredValue`? How is it different from `useTransition`?

**Short definition:** `useDeferredValue` gives you a **delayed copy** of a value. It "lags behind" during heavy updates, and catches up when React has time.

```jsx
function Search() {
  const [query, setQuery] = useState("");
  const deferredQuery = useDeferredValue(query);

  return (
    <>
      <input value={query} onChange={(e) => setQuery(e.target.value)} />
      <SlowList filter={deferredQuery} />
    </>
  );
}
```

The input uses `query` (instant). The slow list uses `deferredQuery` (delayed).

| | `useTransition` | `useDeferredValue` |
|---|---|---|
| You wrap | The **code that sets state** | The **value** you receive |
| Use when | You control the `setState` call | The value comes from props or something you can't wrap |
| Gives you | `isPending` flag | A delayed value |

**Simple rule:** If you own the update, use `useTransition`. If you only receive a value, use `useDeferredValue`.

**Compare with debounce:** Debounce waits a fixed time (say 300ms). `useDeferredValue` has no fixed delay. On a fast device, it updates almost at once. On a slow one, it lags more. It also can be interrupted.

---

## Q4. How does Suspense work beyond lazy loading?

**Short definition:** Suspense lets a component say "I'm not ready yet." React then shows the nearest `Suspense` fallback until it is ready.

In Concept 8, we used it for `React.lazy`. But it also works for **data loading**, with frameworks and libraries that support it (React Query, Relay, Next.js, and so on).

```jsx
<Suspense fallback={<Spinner />}>
  <Comments />   {/* may "suspend" while loading data */}
</Suspense>
```

**Deeper explanation:**
Instead of writing `if (loading) return <Spinner />` inside every component, you declare loading states **in one place, above** the component. You can place several boundaries to load parts of the page independently:

```jsx
<Suspense fallback={<HeaderSkeleton />}>
  <Header />
</Suspense>
<Suspense fallback={<FeedSkeleton />}>
  <Feed />
</Suspense>
```

The header can show up while the feed is still loading.

**Nice combo:** Suspense + transitions. If you wrap a navigation in `startTransition`, React keeps showing the **old** screen instead of flashing a fallback, until the new one is ready.

---

## Q5. What is the `use` hook (React 19)?

**Short definition:** `use` reads a **Promise** or a **Context** inside a component. If it's a pending Promise, the component suspends until it resolves.

```jsx
function Comments({ commentsPromise }) {
  const comments = use(commentsPromise);   // suspends until ready
  return comments.map((c) => <p key={c.id}>{c.text}</p>);
}

<Suspense fallback={<p>Loading...</p>}>
  <Comments commentsPromise={fetchComments()} />
</Suspense>
```

**Two special things about `use`:**
1. Unlike other hooks, you **can call it inside `if` statements and loops**. (`useContext` can't do that, so `use(Context)` is more flexible.)
2. It must be given a Promise that is **stable across renders**, for example one created in a Server Component, or cached by a library. If you create a new Promise inside a Client Component's render, it will restart on every render.

---

## Q6. What is Streaming SSR and Selective Hydration?

**Short definition:** With React 18, the server can **send HTML in pieces** (streaming), and the browser can **hydrate parts of the page independently** (selective hydration).

**The old problem with SSR:**
1. Server fetches **all** data, then renders **all** HTML.
2. Browser downloads **all** JavaScript.
3. React hydrates **everything**.

Each step had to finish before the next started. One slow part (like comments) blocked the whole page.

**With Suspense boundaries:**
- The server sends the fast parts immediately and a placeholder for the slow part. When the slow data is ready, the server streams that HTML in.
- Each `Suspense` section can hydrate on its own. If a user clicks on a section that isn't hydrated yet, React **hydrates that section first**.

The page becomes visible and usable much sooner.

---

## Q7. What are React Server Components (RSC)?

**Short definition:** Server Components are components that run **only on the server**. They send their output to the browser, but **no JavaScript** for the component itself.

**Deeper explanation:**
Normally, every component's code is downloaded and run in the browser. With Server Components, some components never reach the browser as code. This gives you:

- **Smaller bundles:** heavy libraries (like a markdown parser) stay on the server.
- **Direct data access:** a Server Component can read a database or file system directly.
- **Better security:** secrets and API keys stay on the server.

```jsx
// Server Component (default in frameworks like Next.js App Router)
async function ProductPage({ id }) {
  const product = await db.products.find(id);   // runs on the server
  return <h1>{product.name}</h1>;
}
```

Notice the component is `async` and uses `await` directly. This is only allowed in Server Components.

**Server Components cannot:**
- Use `useState`, `useEffect`, or other state and effect hooks
- Use event handlers like `onClick`
- Use browser-only APIs like `window` or `localStorage`

**Important:** RSC is **not the same as SSR.** SSR renders components to HTML on the server, but still sends all component code to the browser to hydrate. Server Components **never** send their code. Many apps use both together.

---

## Q8. What is `"use client"`? How do Server and Client Components work together?

**Short definition:** `"use client"` at the top of a file marks the **boundary** where code starts running in the browser too.

```jsx
"use client";

import { useState } from "react";

export default function LikeButton() {
  const [liked, setLiked] = useState(false);
  return <button onClick={() => setLiked(!liked)}>{liked ? "♥" : "♡"}</button>;
}
```

**Rules of thumb:**
- Components are **Server Components by default** in RSC frameworks.
- Add `"use client"` **only** when you need state, effects, event handlers, or browser APIs.
- Push `"use client"` **as far down the tree as possible**, so only small interactive parts are shipped as JavaScript.
- A Server Component **can import and render** a Client Component.
- A Client Component **cannot import** a Server Component, but it can receive one as `children` or a prop (composition again).

Props passed from Server to Client must be **serializable** (strings, numbers, plain objects, and so on). You can't pass a regular function.

---

## Q9. What are Server Actions?

**Short definition:** Server Actions are **functions that run on the server**, but that you can call from the client, usually from a form.

```jsx
// actions.js
"use server";

export async function addTodo(formData) {
  const text = formData.get("text");
  await db.todos.insert({ text });
}
```

```jsx
import { addTodo } from "./actions";

<form action={addTodo}>
  <input name="text" />
  <button>Add</button>
</form>
```

This connects to the React 19 form Actions we saw in Concept 5.

**Security warning (very important in interviews):**
A Server Action is really a **public HTTP endpoint**. Anyone can call it, not just your form. So always:
- **Validate** the input.
- **Check authentication and permissions** inside the action.
- Never trust the data just because "my own form sent it."

Also, keep React and your framework **up to date**. Server Components have had serious security patches, so staying on the latest patched version is important.

---

## Q10. What are the main new features in React 19?

| Feature | What it does |
|---|---|
| **Actions** | Async functions in transitions and forms, with automatic pending and error handling |
| **`useActionState`** | Tracks the result and pending state of an action |
| **`useFormStatus`** | Lets a child (like a submit button) know the parent form is submitting |
| **`useOptimistic`** | Shows the expected result instantly, then corrects it if the request fails |
| **`use`** | Reads a Promise or Context, and can be called conditionally |
| **`ref` as a prop** | No more `forwardRef` for function components |
| **`<Context>` as provider** | Write `<ThemeContext value={...}>` instead of `<ThemeContext.Provider>` |
| **Document metadata** | `<title>`, `<meta>`, `<link>` can be written in any component, and React moves them to `<head>` |
| **Ref cleanup functions** | A ref callback can return a cleanup function |
| **Better error reporting** | Clearer hydration errors and single, cleaner error messages |

**`useOptimistic` example:**

```jsx
function Messages({ messages, sendMessage }) {
  const [optimistic, addOptimistic] = useOptimistic(
    messages,
    (current, newText) => [...current, { text: newText, sending: true }]
  );

  async function action(formData) {
    const text = formData.get("text");
    addOptimistic(text);            // shows instantly
    await sendMessage(text);        // real request
  }

  return (
    <>
      {optimistic.map((m, i) => (
        <p key={i}>{m.text} {m.sending && <small>(sending...)</small>}</p>
      ))}
      <form action={action}><input name="text" /></form>
    </>
  );
}
```

The message appears at once. If the request fails, React rolls back to the real state.

---

## Q11. What is the React Compiler?

**Short definition:** The React Compiler is a **build-time tool** that automatically adds memoization to your components, so you rarely need to write `useMemo`, `useCallback`, or `React.memo` by hand.

**Deeper explanation:**
It reads your code and figures out which values and functions can be safely cached. This works well only if your code follows the **Rules of React** (pure render, no mutation of props or state). It's a good reminder of why those rules matter.

**Interview note:** It doesn't make manual memoization useless to learn. You still need to understand *why* re-renders happen, and you'll still see `useMemo` and `useCallback` in many existing codebases. Also, it is opt-in and depends on your build setup, so check your project.

---

## Q12. What is `useSyncExternalStore`?

**Short definition:** A hook (added in React 18) for **subscribing to data that lives outside React**, in a way that is safe with concurrent rendering.

```jsx
function useOnlineStatus() {
  return useSyncExternalStore(
    (callback) => {
      window.addEventListener("online", callback);
      window.addEventListener("offline", callback);
      return () => {
        window.removeEventListener("online", callback);
        window.removeEventListener("offline", callback);
      };
    },
    () => navigator.onLine,     // read the current value
    () => true                  // value to use on the server
  );
}
```

**Why it exists:** Using `useEffect` + `useState` to subscribe to an external store can show **inconsistent data** ("tearing") when React renders in pieces. This hook prevents that. Libraries like Redux and Zustand use it internally. Most app developers won't write it directly, but it's a nice senior-level detail.

---

## Q13. What is `useId`?

**Short definition:** `useId` generates a **unique ID** that is the same on the server and the client. It's used to link things like labels and inputs.

```jsx
function EmailField() {
  const id = useId();
  return (
    <>
      <label htmlFor={id}>Email</label>
      <input id={id} />
    </>
  );
}
```

**Why not use `Math.random()` or a counter?** They give different values on server and client, causing **hydration mismatches** (Concept 8). `useId` gives matching results. Don't use it for list keys, though. Keys should come from your data.

---

## Q14. What are `useEffectEvent` and `<Activity>`? (Newer additions, React 19.2)

**`useEffectEvent`**
Sometimes an effect needs to read the **latest** value of something (like a theme) but should **not re-run** when that value changes. This hook solves it.

```jsx
function Chat({ roomId, theme }) {
  const onConnected = useEffectEvent(() => {
    showToast("Connected!", theme);     // always sees latest theme
  });

  useEffect(() => {
    const conn = connect(roomId);
    conn.on("connected", () => onConnected());
    return () => conn.disconnect();
  }, [roomId]);                          // theme not needed here
}
```

It removes the temptation to "lie" in the dependency array (Concept 3, Q10). Don't use it to hide real dependencies. It's only for logic that is truly an "event" fired from inside an effect.

**`<Activity>`**
It lets you **hide part of the UI while keeping its state**, and pause its effects while hidden.

```jsx
<Activity mode={tab === "settings" ? "visible" : "hidden"}>
  <Settings />
</Activity>
```

Compare with conditional rendering, which **destroys** state when a component disappears. This is handy for tabs and back-navigation, where you want half-typed forms to still be there when the user returns.

Since these are newer, always check your project's React version before using them.

---

## Q15. What changed in React 18 that every interviewer expects you to know?

A quick revision list:

1. **`createRoot`** replaces `ReactDOM.render`, and it enables concurrent features.
2. **Automatic batching** works everywhere (timeouts, promises, native events).
3. **Transitions:** `useTransition`, `useDeferredValue`.
4. **Suspense on the server:** streaming SSR and selective hydration.
5. **New hooks:** `useId`, `useSyncExternalStore`, `useInsertionEffect` (for CSS-in-JS libraries).
6. **StrictMode** now also simulates unmount and remount in development to test effect cleanup (Concept 3, Q13).

---

# 🎉 You Finished the Roadmap!

Here is a quick way to use everything you learned:

**How to revise:**
- **Day 1 to 2:** Concepts 1 to 3. These are asked in almost **every** interview (state, props, keys, `useEffect`, stale closures).
- **Day 3 to 4:** Concepts 4 to 6 (hooks, forms, rendering, and performance). Great for mid-level rounds.
- **Day 5:** Concepts 7 to 9 (state management, routing, patterns).
- **Day 6:** Concept 10, then re-read the "Interview tip" lines from every concept.

**The habits that make answers sound senior:**
1. Give the **short definition first**, then the **why**, then a **tiny example**.
2. Mention **trade-offs**, not just features ("useMemo helps, but it has a cost").
3. Say "**I would measure first**" for any performance question.
4. Link ideas together (immutability, purity, and keys come back again and again).
5. It's okay to say "I haven't used that in production, but here's how I understand it."

If you'd like, I can now:
- Run a **mock interview** where I ask you questions one by one and give feedback on your answers
- Give you **top 30 rapid-fire questions** for last-minute revision
- Give you **machine-coding round practice** (like building a todo list, a search with debounce, or an infinite scroll)

Just tell me which one you want.


