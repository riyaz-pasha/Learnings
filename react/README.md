# React

### React Element

* Smallest building block of react application
* Immutable, lightweight representation of what should appear on the UI ( Objects describing what the UI should look like at a given time ).
* Do not manage state or lifecycle methods.

```jsx
const element=<h1>Simple Element</h1>;
```
or

```jsx
const element=React.createElement("h1",null,"Simple Element");
```


### React Component
* Function or Class thar returns collection of React Elements.
* Stateful and manages lifecycle methods.
* Accepts data from other(parent) components as props.


# React Element vs React Component

## Key Differences

| Feature           | React Element                 | React Component                                              |
| ----------------- | ----------------------------- | ------------------------------------------------------------ |
| **Definition**    | A simple object describing UI | A function or class returning elements                       |
| **Mutability**    | Immutable                     | Can be stateful (if needed)                                  |
| **Functionality** | Represents UI at a given time | Can contain logic, state, lifecycle methods                  |
| **Usage**         | Static UI representation      | Dynamic, reusable, and interactive UI                        |
| **Example**       | `<h1>Hello</h1>`              | `const Greeting = (props) => <h1>Hello, {props.name}!</h1>;` |


### Props

* Read only input that are passed from parent component to child component
* They are immutable and cannot be changed inside the child component.


### State

* Built in object that holds data or information about a component.
* Determines how a component behaves and renders
* When the state chenges, react re renders the component to reflect the new data.
  

##### Key Characteristics of State

* Mutable: Unlike props, state can change over time.
* Component-specific: Each component manages its own state.
* Triggers Re-renders: When state changes, the component updates.
* Stored in Class Components (this.state) or Functional Components (useState).

### Refs

* Provides a way to directly access and interact with DOM elements or React components without causing a re-render.
* Managing focus, animation, slections, etc.
  
```jsx
import React, { useRef } from "react";

const InputFocus = () => {
  const inputRef = useRef(null); // Create a ref

  const focusInput = () => {
    inputRef.current.focus(); // Access DOM element
  };

  return (
    <div>
      <input ref={inputRef} type="text" placeholder="Type here..." />
      <button onClick={focusInput}>Focus Input</button>
    </div>
  );
};

export default InputFocus;
```

```jsx
import React, { useRef, useState, useEffect } from "react";

const RenderTracker = () => {
  const [count, setCount] = useState(0);
  const renderCount = useRef(1); // Track renders

  useEffect(() => {
    renderCount.current += 1; // Updates ref value (does NOT cause re-render)
  });

  return (
    <div>
      <p>Count: {count}</p>
      <p>Component rendered: {renderCount.current} times</p>
      <button onClick={() => setCount(count + 1)}>Increment</button>
    </div>
  );
};

export default RenderTracker;
```

# Summary Table: Refs vs State in React

| Feature                                            | `useRef`                          | `useState`                   |
| -------------------------------------------------- | --------------------------------- | ---------------------------- |
| **Triggers re-render?**                            | ❌ No                              | ✅ Yes                        |
| **Used for DOM manipulation?**                     | ✅ Yes                             | ❌ No                         |
| **Stores previous values across renders?**         | ✅ Yes                             | ✅ Yes (but causes re-render) |
| **Used for UI updates?**                           | ❌ No                              | ✅ Yes                        |
| **Best for tracking values without re-rendering?** | ✅ Yes                             | ❌ No                         |
| **Best for managing dynamic UI updates?**          | ❌ No                              | ✅ Yes                        |
| **Common Use Cases**                               | Accessing DOM, holding references | Managing state, updating UI  |


### Forward Ref

* By default, React refs do not work on functional components. If we try to pass a ref to a functional component, it won’t attach to the child’s DOM element.
  

```jsx
import React, { useRef, forwardRef } from "react";

// Child Component (Uses forwardRef)
const Input = forwardRef((props, ref) => {
  return <input ref={ref} type="text" {...props} />;
});

// Parent Component
const Parent = () => {
  const inputRef = useRef(null);

  return (
    <div>
      <Input ref={inputRef} placeholder="Type here..." />
      <button onClick={() => inputRef.current.focus()}>Focus Input</button>
    </div>
  );
};

export default Parent;
```

* By default, forwardRef exposes the entire child component’s DOM node. To control what is exposed, use `useImperativeHandle`.

```jsx
import React, { useRef, forwardRef, useImperativeHandle } from "react";

// Child Component
const CustomInput = forwardRef((props, ref) => {
  const inputRef = useRef();

  useImperativeHandle(ref, () => ({
    focus: () => inputRef.current.focus(),
    clear: () => (inputRef.current.value = ""),
  }));

  return <input ref={inputRef} type="text" {...props} />;
});

// Parent Component
const Parent = () => {
  const inputRef = useRef(null);

  return (
    <div>
      <CustomInput ref={inputRef} placeholder="Enter text..." />
      <button onClick={() => inputRef.current.focus()}>Focus</button>
      <button onClick={() => inputRef.current.clear()}>Clear</button>
    </div>
  );
};

export default Parent;
```
## Context

* React Context is a way to pass data through the component tree without having to pass props manually at every level.
* Avoid Prop drilling
* Share global data like theme, auth state, user preferences etc.
* Improves maintainability by centralizing shared data.

```jsx
import React, { createContext, useState } from "react";

// Create a Context
export const ThemeContext = createContext();

export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState("light");

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useThemeContext = () => useContext(ThemeContext);
```

## Virtual DOM

* Lightweight, in-memory representation of actual DOM.
* Real DOM is slow because
  * Changing the DOM triggers reflow and repaint, which are expensive operations.
  * Updaing a single element can cause entire page to recalculate styles, layout, and re-render.
* It minimizes Real DOM updates. Batch updates to reduce unnecessary re-renders.
* Efficient diffing algorithm applies changes only where needed.

#### How Virtual DOM Work?

1. Render: React creates a Virtual DOM tree that mirrors the Real DOM.
2. Change Detection: When state or props change, React creates a new Virtual DOM.
3. Diffing Algorithm (Reconciliation): React compares the old and new Virtual DOM to find the differences.
4. Efficient Updates: React only updates the changed elements in the Real DOM instead of re-rendering everything.

## Reconciliation

* A process React uses to effieciently updates the UI when application state or props changes.
* Instead re rendering the enitre DOM, react calculates the difference between old and new Virtual DOM and rerenders only necessary parts.

### React Fiber

* Is a reconciliation alogorithm  introduced in React 16 to improve UI rendering performance.
* It maked React faster, more responsive, and able to handle complex UI updates efficiently.

##### Why Was React Fiber Introduced?

* Before Fiber, React used stack based reconciliation alogorithm that:
  * Blocked the main thread, causing lag in animations and user interactions.
  * Couldn't prioritize updates, treating all updates equally.
  * Re render everything synchronously
* Fiber
  * Breaks rendering work into smaller chunks (time-slicing).
  * Prioritizing update ( high priority UI updates first ).
  * Making React asynchronous ( pausing & resuming rendering ).

#### Lifecycle Methods in Class Components

🔹 Phase 1: Mounting (When Component is Created)

| Method                                       | Description                                |
| -------------------------------------------- | ------------------------------------------ |
| constructor()                                | Initialises state and binds event handlers |
| static getDerivedStateFromProps(props,state) | Updates state based on props before render |
| render()                                     | render changes on the UI (Required)        |
| componentDidMount()                          | Runs after component is added to the DOM   |

Order of execution (Mounting Phase):
  `constructor` → `getDerivedStateFromProps` → `render` → `componentDidMount`

🔹 Phase 2: Updating (When State or Props Change)

| Method                                       | Description                                                   |
| -------------------------------------------- | ------------------------------------------------------------- |
| static getDerivedStateFromProps(props,state) | Runs before rerendering when props change                     |
| shouldComponentUpdate(nextProps,nextState)   | Controls whether component should re-render (default: true)   |
| render()                                     | Renders JSX                                                   |
| getSnapshotBeforeUpdate(prevProps,prevState) | Captures info before the update (e.g., scroll position)       |
| componentDidUpdate()                         | Runs after the update (useful for API calls based on changes) |

Order of execution (Updating Phase):
  `getDerivedStateFromProps` → `shouldComponentUpdate` → `render` → `getSnapshotBeforeUpdate` → `componentDidUpdate`

🔹 Phase 3: Unmounting (When Component is Removed)

| Method                 | Description                                                                                            |
| ---------------------- | ------------------------------------------------------------------------------------------------------ |
| componentWillUnmount() | Runs before component is removed from DOM (cleanup tasks removing event listeners, unsubscribing etc.) |



# Summary Table: React Component Lifecycle

| Phase          | Class Component Lifecycle    | Functional Component Hook                  |
| -------------- | ---------------------------- | ------------------------------------------ |
| **Mounting**   | `constructor()`              | `useState()`                               |
|                | `getDerivedStateFromProps()` | Not needed                                 |
|                | `render()`                   | JSX return                                 |
|                | `componentDidMount()`        | `useEffect(() => {}, [])`                  |
| **Updating**   | `getDerivedStateFromProps()` | Not needed                                 |
|                | `shouldComponentUpdate()`    | Not needed (handled automatically)         |
|                | `render()`                   | JSX return                                 |
|                | `getSnapshotBeforeUpdate()`  | Not needed                                 |
|                | `componentDidUpdate()`       | `useEffect(() => {}, [dependency])`        |
| **Unmounting** | `componentWillUnmount()`     | `useEffect(() => { return () => {} }, [])` |

