Absolutely. We’ll do this **concept by concept**, not as one huge list.

For each React concept, I’ll cover:

1. **What it is** — traditional interview definition.
2. **What it really means** — simple explanation.
3. **Why React needs it**.
4. **How it works internally** — where useful.
5. **Simple example**.
6. **Interview questions** — beginner → intermediate → advanced.
7. **Deep interview answers** — answers you can actually speak in an interview.
8. **Common mistakes / follow-up questions**.
9. **Practical scenarios** — because interviewers often ask “What would you do if…?”

We’ll gradually move from **React basics → core concepts → hooks → rendering → performance → architecture → advanced React**.

# React Interview Preparation — Concept 1

## What is React?

This is usually one of the first React questions in an interview.

---

## 1. Traditional interview definition

> **React is a JavaScript library for building user interfaces, especially for single-page applications. It was developed and is maintained by Meta. React allows developers to build reusable UI components and efficiently update the UI when application data changes.**

That's a technically correct definition.

But if you only memorize that sentence, the interviewer can easily ask:

> "What do you mean by reusable components?"

or

> "Why do you call React a library and not a framework?"

or

> "How does React update the UI efficiently?"

So let's understand what is actually happening.

---

# 2. What is React in simple words?

Think about a website like an e-commerce application.

It may contain:

* Navbar
* Search box
* Product cards
* Shopping cart
* Login form
* Product details
* Payment page
* User profile

Instead of writing the entire website as one large piece of code, React allows us to break the UI into **small, independent pieces called components**.

For example:

```text
App
│
├── Navbar
├── SearchBar
├── ProductList
│   ├── ProductCard
│   ├── ProductCard
│   └── ProductCard
│
└── Footer
```

Each component can have its own:

* UI
* data
* logic
* state
* events

This makes a large application easier to build and maintain.

---

# 3. Why was React created?

Imagine you have a page displaying:

```text
Product: iPhone
Price: ₹80,000
Cart Items: 2
```

Now the user adds another product to the cart.

The UI needs to change:

```text
Product: iPhone
Price: ₹80,000
Cart Items: 3
```

In a traditional approach, developers often had to manually find the relevant HTML element and update it.

For example:

```javascript
document.getElementById("cart-count").innerText = 3;
```

As applications become larger, doing this manually in many places becomes difficult.

React lets us describe **what the UI should look like based on the current data**.

For example:

```jsx
function Cart({ count }) {
  return <div>Cart Items: {count}</div>;
}
```

If:

```javascript
count = 2
```

React shows:

```text
Cart Items: 2
```

If:

```javascript
count = 3
```

React updates the UI accordingly.

The important idea is:

> **We tell React what the UI should look like. React takes care of updating the DOM when the data changes.**

This idea is very important for React interviews.

---

# 4. What does "component-based" mean?

This is one of the most important React concepts.

Suppose you are building YouTube.

You might have:

```text
YouTube
│
├── Header
├── Sidebar
├── SearchBar
├── VideoList
│   ├── VideoCard
│   ├── VideoCard
│   └── VideoCard
└── Footer
```

Instead of creating one giant component, you create smaller components.

Example:

```jsx
function VideoCard() {
  return (
    <div>
      <img src="thumbnail.jpg" />
      <h3>Learn React</h3>
      <p>100K views</p>
    </div>
  );
}
```

Then you can use it multiple times:

```jsx
function VideoList() {
  return (
    <div>
      <VideoCard />
      <VideoCard />
      <VideoCard />
    </div>
  );
}
```

This is **reusability**.

If you change the design of `VideoCard`, you can make the change in one component instead of changing many copies of the same code.

---

# 5. What does React actually manage?

A common beginner misunderstanding is:

> "React builds the entire website."

More accurately:

> **React primarily deals with the UI layer of an application.**

For example, React can help you build:

```text
User clicks button
        ↓
State changes
        ↓
React renders UI
        ↓
DOM is updated
```

But React itself does not provide everything needed for a complete application.

For example, you may use separate libraries for:

* Routing
* API requests
* Global state
* Form management
* Data fetching
* Authentication

This leads to a common interview question.

---

# 6. Interview Question: Is React a library or a framework?

### Short answer

> **React is a JavaScript library for building user interfaces.**

### Deeper answer

A **library** usually gives you tools for a particular part of your application.

React mainly focuses on the UI.

A **framework** usually provides a more complete structure for building an application, including things like routing, data handling, application structure, and other conventions.

React itself mainly gives us the component and rendering model.

For example:

```text
React
  ↓
UI + Components + Rendering
```

You may then add other tools:

```text
React
 ├── React Router → Routing
 ├── Redux/Zustand → State management
 ├── React Query → Server state/data fetching
 └── etc.
```

So in an interview, don't say:

> "React is a framework."

Say:

> **"React is a JavaScript library focused mainly on building user interfaces."**

---

# 7. Interview Question: What are the main features of React?

A good interview answer is:

> React provides a component-based architecture, declarative UI development, reusable components, state management, and an efficient rendering system. It also uses JSX, which allows us to describe UI using a syntax that looks similar to HTML inside JavaScript.

Let's understand each one.

### 1. Components

We break the UI into reusable pieces.

```jsx
function Button() {
  return <button>Click me</button>;
}
```

### 2. Declarative UI

Instead of manually telling the browser:

> "Find this element and change its text."

We describe what the UI should look like.

```jsx
function Greeting({ name }) {
  return <h1>Hello {name}</h1>;
}
```

If `name` changes, React handles the UI update.

### 3. Reusability

A component can be reused:

```jsx
<Button />
<Button />
<Button />
```

### 4. State

Components can remember changing information.

```jsx
const [count, setCount] = useState(0);
```

### 5. JSX

JSX allows us to write UI in a JavaScript-friendly syntax.

```jsx
const element = <h1>Hello</h1>;
```

---

# 8. Interview Question: What is declarative programming in React?

This is a **very important interview question**.

### Traditional definition

> Declarative programming means describing what you want the result to be rather than explicitly describing every step required to produce that result.

That sounds complicated, so let's compare it.

### Imperative approach

You tell the browser **how to do something**.

```javascript
const heading = document.getElementById("heading");

heading.innerText = "Hello Riyaz";
heading.style.color = "red";
```

You are giving instructions:

1. Find the element.
2. Change its text.
3. Change its color.

### Declarative React approach

You describe the desired UI:

```jsx
function App() {
  return (
    <h1 style={{ color: "red" }}>
      Hello Riyaz
    </h1>
  );
}
```

You are basically saying:

> "When this component is rendered, I want this UI."

React handles the necessary DOM operations.

### Interview answer

If an interviewer asks:

> **"What does declarative UI mean in React?"**

You can say:

> "In React, we describe what the UI should look like based on the current state and props, instead of manually manipulating the DOM step by step. React then handles the DOM updates needed to bring the actual UI in line with that description."

That's a strong answer.

---

# 9. Interview Question: What is JSX?

### Traditional definition

> **JSX is a syntax extension for JavaScript that allows us to write HTML-like markup inside JavaScript code.**

Example:

```jsx
const element = <h1>Hello World</h1>;
```

It looks like HTML, but it is not actually HTML.

JSX is eventually transformed into JavaScript.

Conceptually:

```jsx
<h1>Hello World</h1>
```

becomes something similar to:

```javascript
React.createElement("h1", null, "Hello World");
```

Modern React uses the newer JSX transform, so you don't necessarily need to write `React.createElement` yourself.

The important interview point is:

> **JSX is syntax that gets transformed into JavaScript representation of UI.**

---

# 10. Why do we use JSX?

Without JSX, UI code can become harder to read.

For example, conceptually:

```javascript
createElement(
  "div",
  null,
  createElement(
    "h1",
    null,
    "Hello"
  )
);
```

With JSX:

```jsx
<div>
  <h1>Hello</h1>
</div>
```

The second version is much easier for humans to understand.

So JSX is mainly about making UI code **more readable and expressive**.

---

# 11. Interview Question: Is JSX HTML?

### Answer

> **No. JSX is not HTML. It is a syntax extension for JavaScript that looks similar to HTML.**

For example:

```jsx
const element = <h1>Hello</h1>;
```

This is JSX.

The browser does not directly understand JSX.

A build tool/transformation step converts JSX into JavaScript that the browser can execute.

This is why JSX can contain JavaScript expressions:

```jsx
const name = "Riyaz";

function App() {
  return <h1>Hello {name}</h1>;
}
```

The `{name}` part is JavaScript.

---

# 12. Interview Question: What are props?

### Traditional definition

> **Props, short for properties, are read-only values passed from a parent component to a child component.**

Example:

```jsx
function App() {
  return <Welcome name="Riyaz" />;
}
```

Here:

```text
App
 ↓
name="Riyaz"
 ↓
Welcome
```

The child receives the value:

```jsx
function Welcome(props) {
  return <h1>Hello {props.name}</h1>;
}
```

Result:

```text
Hello Riyaz
```

You can also use destructuring:

```jsx
function Welcome({ name }) {
  return <h1>Hello {name}</h1>;
}
```

---

# 13. Why do we need props?

Props allow components to become **reusable**.

Instead of creating three different components:

```jsx
function RiyazCard() {}
function JohnCard() {}
function SarahCard() {}
```

We can create one:

```jsx
function UserCard({ name }) {
  return <div>{name}</div>;
}
```

Then:

```jsx
<UserCard name="Riyaz" />
<UserCard name="John" />
<UserCard name="Sarah" />
```

Same component.

Different data.

That's the real purpose of props.

---

# 14. Important interview question: Can a child modify props?

### Answer

> **No. Props should be treated as read-only by the receiving component.**

For example:

```jsx
function User({ name }) {
  // Don't do this
  name = "John";
}
```

Instead, if a child needs to request a change, the parent can pass a function.

Example:

```jsx
function Parent() {
  const handleClick = () => {
    console.log("Clicked");
  };

  return <Child onClick={handleClick} />;
}
```

Child:

```jsx
function Child({ onClick }) {
  return <button onClick={onClick}>Click</button>;
}
```

This gives us an important React pattern:

```text
Parent
   ↓ props
Child

Child
   ↑ callback/event
Parent
```

This is often described as **one-way data flow**.

---

# 15. Interview Question: What is state?

### Traditional definition

> **State is data managed by a component that can change over time and cause the component to render again when updated.**

Example:

```jsx
const [count, setCount] = useState(0);
```

Here:

```text
count      → current value
setCount   → function used to update it
0          → initial value
```

When we call:

```jsx
setCount(1);
```

React knows that the state changed and schedules the component to render again.

---

# 16. Props vs State

This is almost guaranteed to appear in React interviews.

| Props                                | State                              |
| ------------------------------------ | ---------------------------------- |
| Passed into a component              | Managed by the component           |
| Usually comes from parent            | Belongs to component logic         |
| Read-only from child's point of view | Can be updated using state setters |
| Used to pass data                    | Used to manage changing data       |
| Controlled externally                | Controlled internally              |

Example:

```jsx
function Counter({ title }) {
  const [count, setCount] = useState(0);

  return (
    <>
      <h1>{title}</h1>
      <p>{count}</p>

      <button onClick={() => setCount(count + 1)}>
        Increment
      </button>
    </>
  );
}
```

Here:

```text
title → prop
count → state
```

---

# 17. A very important mental model

You should start thinking about React like this:

```text
             Props
               ↓
        ┌──────────────┐
        │   Component  │
        │              │
        │     State    │
        └──────┬───────┘
               ↓
              UI
```

When state changes:

```text
State changes
     ↓
React schedules an update
     ↓
Component renders again
     ↓
React determines what changed
     ↓
DOM is updated where necessary
```

This mental model will become extremely important when we reach:

* Rendering
* Reconciliation
* Virtual DOM
* Fiber
* `useMemo`
* `useCallback`
* `React.memo`
* Performance optimization

---

# 18. Beginner → Advanced Question Path

For the **"What is React?"** concept, interview questions can gradually become harder:

### Beginner

1. What is React?
2. Why do we use React?
3. Is React a library or framework?
4. What is a component?
5. What is JSX?
6. What are props?
7. What is state?

### Intermediate

8. What is declarative programming?
9. What is one-way data flow?
10. Why are components reusable?
11. Props vs state?
12. Why shouldn't we modify props directly?
13. How does state update the UI?
14. What happens when a component re-renders?
15. Why does React use JSX?

### Advanced

16. What happens internally when state changes?
17. What is reconciliation?
18. What is the Virtual DOM?
19. How does React decide what DOM nodes to update?
20. What is Fiber?
21. What is the difference between rendering and committing?
22. Why doesn't every state update immediately change the DOM?
23. How does React batch state updates?
24. What is concurrent rendering?

We'll cover these **properly later**, instead of trying to memorize everything now.

---

# One interview-ready answer

If the interviewer simply asks:

> **"What is React?"**

Don't give them a 2-minute textbook definition.

A natural answer would be:

> **"React is a JavaScript library mainly used for building user interfaces. The main idea behind React is to break the UI into small reusable components. These components can receive data through props and manage changing data through state. React follows a declarative approach, so instead of manually manipulating the DOM, we describe what the UI should look like for the current state, and React takes care of updating the DOM when the data changes. JSX is commonly used with React to make the UI code easier to write and understand."**

That sounds much more like someone who **understands React**, rather than someone who memorized a definition.

---

## What you should remember from Concept 1

If you remember only these points, you're on the right track:

```text
React
 │
 ├── JavaScript library
 │
 ├── Builds UI
 │
 ├── Component-based
 │
 ├── Declarative
 │
 ├── JSX
 │
 ├── Props → data coming into component
 │
 └── State → changing data managed by component
```

And the most important mental model:

> **UI = a result of the current data/state.**

When the data changes, React works out how the UI needs to change.

---

### Next concept

The natural next step is **Components**, where we'll go much deeper into:

* What exactly is a component?
* Functional components
* Component rules
* Component composition
* Parent/child relationship
* Props
* `children`
* Reusable components
* Component lifecycle at a basic level
* Beginner → advanced component interview questions

**Which depth should we use next: `standard interview`, `deep interview`, or `deep + tricky follow-ups`?**


----
----
----
----
----

# React Interview Preparation — Concept 2

# Components

Components are one of the **most important concepts in React**. Almost everything else in React is built around understanding components properly.

---

## 1. What is a Component?

### Traditional definition

> **A component is a reusable, independent piece of UI that contains its own structure, logic, and behavior.**

In simple words:

> **A component is a small building block of a React application.**

For example, imagine an e-commerce website:

```text
App
│
├── Navbar
├── SearchBar
├── ProductList
│   ├── ProductCard
│   ├── ProductCard
│   └── ProductCard
├── Cart
└── Footer
```

Each of these can be a React component.

```jsx
function Navbar() {
  return <nav>My Store</nav>;
}

function Footer() {
  return <footer>Copyright 2026</footer>;
}
```

Then the main component can use them:

```jsx
function App() {
  return (
    <>
      <Navbar />
      <ProductList />
      <Footer />
    </>
  );
}
```

So instead of building everything in one huge component, we break the application into smaller pieces.

---

# 2. Why do we need components?

Suppose you have a very large application.

Without components, you could end up with something like:

```text
App
 └── 10,000 lines of code
```

That becomes difficult to:

* understand
* modify
* test
* debug
* reuse

With components:

```text
App
│
├── Header
├── Sidebar
├── Dashboard
│   ├── UserStats
│   ├── RevenueChart
│   └── RecentOrders
├── Notifications
└── Footer
```

Now each part has a clear responsibility.

This is one of the main benefits of **component-based architecture**.

---

# 3. Functional Components

Modern React mainly uses **function components**.

Example:

```jsx
function Welcome() {
  return <h1>Hello World</h1>;
}
```

This is a React component.

You can use it like:

```jsx
<Welcome />
```

Notice something important:

```jsx
function Welcome() {
```

is a normal JavaScript function.

But when we use:

```jsx
<Welcome />
```

React treats it as a component.

---

# 4. How does React know that something is a component?

This is a common beginner interview question.

React components conventionally start with a **capital letter**.

Correct:

```jsx
function UserProfile() {
  return <h1>User Profile</h1>;
}
```

Use:

```jsx
<UserProfile />
```

But:

```jsx
function userProfile() {
  return <h1>User Profile</h1>;
}
```

is problematic when used as:

```jsx
<userProfile />
```

Why?

Because JSX treats lowercase names as HTML/custom DOM tags.

For example:

```jsx
<div />
<button />
<input />
```

are treated as DOM elements.

Whereas:

```jsx
<UserProfile />
```

is treated as a React component.

### Interview answer

> "React uses the capitalization convention to distinguish user-defined components from built-in HTML elements. Lowercase JSX tags are treated as DOM elements, while capitalized names are treated as components."

---

# 5. Can a component return HTML?

Technically, we should be careful with that wording.

A component returns **React elements**, usually written using JSX.

Example:

```jsx
function Header() {
  return <h1>My Website</h1>;
}
```

The JSX:

```jsx
<h1>My Website</h1>
```

is not literally HTML sitting inside the JavaScript at runtime.

JSX is transformed into JavaScript.

So a better interview answer is:

> "A component returns React elements, and we commonly use JSX to describe those elements."

---

# 6. Can a component return multiple elements?

Yes.

For example:

```jsx
function User() {
  return (
    <>
      <h1>Riyaz</h1>
      <p>Frontend Developer</p>
    </>
  );
}
```

Here we're using a **Fragment**:

```jsx
<>
...
</>
```

A Fragment lets us group multiple elements without adding an extra DOM element.

Without a Fragment, you could use:

```jsx
function User() {
  return (
    <div>
      <h1>Riyaz</h1>
      <p>Frontend Developer</p>
    </div>
  );
}
```

But now an extra `<div>` appears in the DOM.

---

# 7. Component Composition

This is a very important concept.

### Definition

> **Component composition means building larger components by combining smaller components.**

For example:

```jsx
function Header() {
  return <header>Header</header>;
}

function Sidebar() {
  return <aside>Sidebar</aside>;
}

function Content() {
  return <main>Content</main>;
}
```

We can compose them:

```jsx
function Dashboard() {
  return (
    <>
      <Header />
      <Sidebar />
      <Content />
    </>
  );
}
```

Then:

```jsx
function App() {
  return <Dashboard />;
}
```

So:

```text
App
 ↓
Dashboard
 ├── Header
 ├── Sidebar
 └── Content
```

This is **composition**.

---

# 8. Why is composition important?

Suppose you create a website with:

```text
Navbar
Footer
Modal
Button
Card
Input
```

These components can be combined in different ways.

For example:

```jsx
function LoginPage() {
  return (
    <>
      <Navbar />
      <LoginForm />
      <Footer />
    </>
  );
}
```

And:

```jsx
function ProfilePage() {
  return (
    <>
      <Navbar />
      <Profile />
      <Footer />
    </>
  );
}
```

The same components can be composed differently.

This gives React applications flexibility.

---

# 9. Parent and Child Components

Consider:

```jsx
function App() {
  return <User />;
}
```

Here:

```text
App
 ↓
User
```

`App` is the **parent**.

`User` is the **child**.

Now:

```jsx
function App() {
  return (
    <User name="Riyaz" />
  );
}
```

Data can flow from parent to child using props.

```text
Parent
   │
   │ props
   ↓
Child
```

Example:

```jsx
function App() {
  return <User name="Riyaz" />;
}

function User({ name }) {
  return <h1>Hello {name}</h1>;
}
```

Output:

```text
Hello Riyaz
```

---

# 10. Can data flow from child to parent?

This is a **very common interview question**.

The direct answer is:

> **React's normal data flow is from parent to child. A child doesn't directly modify the parent's state. Instead, the parent can pass a callback function to the child, and the child can call that function to request a change.**

Example:

```jsx
function Parent() {
  const handleMessage = (message) => {
    console.log(message);
  };

  return <Child onMessage={handleMessage} />;
}
```

Child:

```jsx
function Child({ onMessage }) {
  return (
    <button onClick={() => onMessage("Hello Parent")}>
      Send Message
    </button>
  );
}
```

The flow is:

```text
Parent
  │
  │ passes callback
  ↓
Child
  │
  │ calls callback
  ↓
Parent
```

The child isn't directly changing the parent.

It's **communicating an event/request to the parent**.

---

# 11. What is `children`?

This is an important component concept.

Consider:

```jsx
<Card>
  <h1>Hello</h1>
  <p>Welcome to React</p>
</Card>
```

The content between:

```jsx
<Card>
   ...
</Card>
```

is available to the `Card` component through the special `children` prop.

Example:

```jsx
function Card({ children }) {
  return (
    <div className="card">
      {children}
    </div>
  );
}
```

Now:

```jsx
<Card>
  <h1>Hello</h1>
  <p>Welcome to React</p>
</Card>
```

will render the supplied content inside the card.

Conceptually:

```text
Card
└── children
    ├── h1
    └── p
```

---

# 12. Why is `children` useful?

It allows us to create flexible wrapper components.

For example:

```jsx
function Panel({ children }) {
  return (
    <section className="panel">
      {children}
    </section>
  );
}
```

Then:

```jsx
<Panel>
  <UserProfile />
</Panel>
```

Or:

```jsx
<Panel>
  <ProductList />
</Panel>
```

Same `Panel`.

Different content.

This is another example of **composition**.

---

# 13. Component vs Function

An interviewer might ask:

> "What's the difference between a normal JavaScript function and a React component?"

A React function component is still a JavaScript function, but it is intended to be used by React as part of the UI.

For example:

```jsx
function add(a, b) {
  return a + b;
}
```

This is a normal function.

```jsx
function User() {
  return <h1>User</h1>;
}
```

This is a function component.

The component:

* participates in React's rendering system
* can receive props
* can use React Hooks
* returns React elements

For example:

```jsx
function User({ name }) {
  return <h1>{name}</h1>;
}
```

---

# 14. Can a component have state?

Yes.

This is where components become more powerful.

```jsx
import { useState } from "react";

function Counter() {
  const [count, setCount] = useState(0);

  return (
    <>
      <p>{count}</p>

      <button onClick={() => setCount(count + 1)}>
        Increment
      </button>
    </>
  );
}
```

Here the component has state:

```text
Counter
  │
  └── count
```

When `setCount` is called, React schedules an update and the component renders again.

We'll study this properly when we reach **State and `useState`**.

---

# 15. Should every component have state?

No.

This is a common mistake beginners make.

A component can simply display information.

```jsx
function Header() {
  return <header>My Website</header>;
}
```

It doesn't need state.

Another component might need state:

```jsx
function Counter() {
  const [count, setCount] = useState(0);

  // ...
}
```

So don't think:

> "Every component needs state."

Instead:

> **A component should have state when it needs to manage changing information.**

---

# 16. Should every component be very small?

Not necessarily.

You'll sometimes hear:

> "A component should always be tiny."

That's too simplistic.

A component should have a **clear responsibility**.

For example, this can be reasonable:

```text
CheckoutPage
├── AddressSection
├── PaymentSection
├── OrderSummary
└── PlaceOrderButton
```

But splitting every tiny piece into a separate component can also make the code harder to follow.

The goal isn't:

> "Maximum number of components."

The goal is:

> **Clear responsibilities and useful reuse.**

That's a more mature way to think about component design.

---

# 17. Component Reusability

Consider a button.

Instead of writing:

```jsx
<button>Login</button>

<button>Register</button>

<button>Buy Now</button>
```

we can create:

```jsx
function Button({ children }) {
  return <button>{children}</button>;
}
```

Then:

```jsx
<Button>Login</Button>

<Button>Register</Button>

<Button>Buy Now</Button>
```

One component.

Different content.

We can make it more reusable:

```jsx
function Button({ children, onClick, disabled }) {
  return (
    <button onClick={onClick} disabled={disabled}>
      {children}
    </button>
  );
}
```

Now the component can be used in many places.

---

# 18. Interview Question: What makes a good React component?

A good answer:

> "A good component usually has a clear responsibility, is easy to understand, accepts data through props when appropriate, avoids unnecessary dependencies on unrelated parts of the application, and can be reused when reuse actually makes sense. I also try to avoid making components unnecessarily large or unnecessarily fragmented."

Notice this answer doesn't say:

> "Every component must be reusable."

Because not every component needs to be reused.

Sometimes a component exists simply to organize one part of a page.

---

# 19. Interview Question: What is component reusability?

### Simple answer

> **Component reusability means creating a component once and using it in multiple places with different data or behavior.**

Example:

```jsx
function UserCard({ name, role }) {
  return (
    <div>
      <h2>{name}</h2>
      <p>{role}</p>
    </div>
  );
}
```

Use it:

```jsx
<UserCard
  name="Riyaz"
  role="Frontend Developer"
/>

<UserCard
  name="John"
  role="Backend Developer"
/>
```

The component is the same.

The data is different.

---

# 20. Advanced Question: What is component composition vs inheritance?

This is a popular conceptual question.

React generally encourages **composition** rather than using inheritance as the primary way to reuse UI behavior.

Instead of thinking:

```text
ParentComponent
      ↑
ChildComponent
      ↑
AnotherChild
```

you often compose components:

```jsx
<Layout>
  <Sidebar />
  <MainContent />
</Layout>
```

Or:

```jsx
<Card>
  <UserProfile />
</Card>
```

The idea is:

> **Build complex UI by combining smaller components.**

React's documentation and ecosystem strongly favor composition for UI reuse.

---

# 21. Interview Question: What happens when a component renders?

At a high level:

```text
Component
   ↓
React calls/renders component
   ↓
React gets a description of the UI
   ↓
React compares it with the previous result
   ↓
React commits necessary DOM changes
```

For example:

```jsx
function App() {
  return <h1>Hello</h1>;
}
```

React uses the returned element description to determine what should appear in the UI.

When the component's relevant data changes, React may render it again.

**Important:** rendering the component does not automatically mean React destroys and recreates the actual DOM node every time.

That's a very common misconception.

---

# 22. Very Important: Re-render ≠ DOM recreation

Suppose:

```jsx
function Counter() {
  const [count, setCount] = useState(0);

  return <h1>{count}</h1>;
}
```

When `count` changes:

```text
0 → 1
```

the component renders again.

But React does **not** simply throw away the entire DOM and create everything from scratch.

React determines what changed and commits the necessary DOM updates.

So:

> **A component re-rendering does not mean the entire DOM is recreated.**

Remember this.

We'll go much deeper into this when we study **rendering, reconciliation, and the Virtual DOM**.

---

# 23. Tricky Interview Question

### Question:

> "If a parent component re-renders, do all child components re-render?"

The beginner answer:

> "Yes, because the parent rendered again."

That's too simplistic.

By default, when a parent renders, React will generally attempt to render its child components as part of that render tree.

But optimizations such as:

```jsx
React.memo()
```

can allow React to skip rendering a child when its relevant props haven't changed.

There are also other factors involved.

So a better interview answer is:

> "A parent re-render can cause its child components to be rendered again, but that doesn't mean the DOM is recreated. React can also skip some child renders through mechanisms such as memoization when the conditions allow it."

This distinction is important.

---

# 24. Another tricky question

### Question:

> "Does calling a component function mean React immediately updates the DOM?"

No.

There is an important distinction between:

### Render phase

React determines what the UI should look like.

### Commit phase

React applies the necessary changes to the DOM.

Simplified:

```text
State update
     ↓
Render phase
     ↓
React determines changes
     ↓
Commit phase
     ↓
DOM updated
```

We'll study render vs commit in much more detail later.

---

# 25. Common Component Mistakes

### Mistake 1 — Lowercase component names

Bad:

```jsx
function userCard() {
  return <div>User</div>;
}
```

Prefer:

```jsx
function UserCard() {
  return <div>User</div>;
}
```

---

### Mistake 2 — Modifying props

Don't treat props as your own mutable variables.

```jsx
function User({ name }) {
  name = "John";
}
```

Instead, use appropriate state or ask the parent to update its data.

---

### Mistake 3 — Making one giant component

This:

```text
App.jsx
5000 lines
```

can become difficult to maintain.

Break the UI into meaningful components.

---

### Mistake 4 — Creating components for absolutely everything

This:

```text
OneWordComponent
OneIconWrapperComponent
OneTextWrapperComponent
OneDivComponent
...
```

can also make the application unnecessarily complicated.

Component boundaries should have a reason.

---

# 26. Interview Questions — Beginner → Advanced

You should now be able to answer these:

### Beginner

1. What is a React component?
2. What is a functional component?
3. Why do React component names start with capital letters?
4. What are props?
5. What is `children`?
6. What is a parent component?
7. What is a child component?
8. Can a component have state?
9. Can a component return multiple elements?

### Intermediate

10. What is component composition?
11. What is component reusability?
12. How does data flow between components?
13. How can a child communicate with a parent?
14. What is the difference between props and state?
15. Why should props be treated as read-only?
16. What is the purpose of `children`?
17. How do you decide where to split components?

### Advanced

18. What happens when a component re-renders?
19. Does re-rendering mean the DOM is recreated?
20. If a parent re-renders, what happens to its children?
21. What is composition over inheritance in React?
22. What is the render phase?
23. What is the commit phase?
24. How does React decide which components need to render?
25. How can unnecessary child renders be avoided?

---

# The mental model to keep

Think of a React application as a **tree of components**:

```text
                         App
                          │
              ┌───────────┼───────────┐
              ↓           ↓           ↓
           Navbar      Dashboard     Footer
                          │
                    ┌─────┼─────┐
                    ↓     ↓     ↓
                  Card   Card   Card
```

Data generally flows **down**:

```text
Parent
   ↓ props
Child
   ↓ props
Grandchild
```

And when a child needs the parent to do something:

```text
Parent
   ↓ callback
Child
   ↓ calls callback
Parent handles the event
```

This component tree + data-flow model will make many later React concepts much easier.

---

## Next concept: Props

We've mentioned props several times here, but **props deserve their own concept**.

We'll go from basic to advanced:

**Props → passing data → destructuring → default values → functions as props → object/array props → `children` → callback props → prop drilling → composition → advanced prop patterns → common interview traps.**

---
---
---
---


# React Interview Preparation — Concept 3

# Props

Props are one of the **core concepts in React**. If you understand props properly, concepts like component communication, callbacks, prop drilling, composition, and even state management become much easier.

---

# 1. What are Props?

### Traditional interview definition

> **Props, short for properties, are read-only values passed from a parent component to a child component. They are used to pass data and behavior between components.**

Simple version:

> **Props are a way for a parent component to give information to a child component.**

Example:

```jsx
function App() {
  return <User name="Riyaz" />;
}
```

Here:

```text
App
 │
 │ name="Riyaz"
 ↓
User
```

The `User` component receives the prop.

```jsx
function User(props) {
  return <h1>Hello {props.name}</h1>;
}
```

Output:

```text
Hello Riyaz
```

---

# 2. Why do we need Props?

Imagine we have this component:

```jsx
function User() {
  return <h1>Hello Riyaz</h1>;
}
```

It only works for Riyaz.

What if we want:

```text
Hello Riyaz
Hello John
Hello Sarah
```

We don't want to create three components.

Instead:

```jsx
function User({ name }) {
  return <h1>Hello {name}</h1>;
}
```

Now:

```jsx
<User name="Riyaz" />
<User name="John" />
<User name="Sarah" />
```

Same component.

Different data.

That's one of the main purposes of props.

---

# 3. How do Props work?

Let's look at the complete flow.

### Parent:

```jsx
function App() {
  return <User name="Riyaz" />;
}
```

### Child:

```jsx
function User(props) {
  return <h1>Hello {props.name}</h1>;
}
```

React conceptually gives the child something like:

```javascript
{
  name: "Riyaz"
}
```

So inside the component:

```javascript
props.name
```

gives:

```text
Riyaz
```

You can think of props as an **input to a component**.

Similar to a JavaScript function:

```javascript
function greet(name) {
  return `Hello ${name}`;
}
```

You call:

```javascript
greet("Riyaz");
```

Likewise, a React component receives props:

```jsx
<User name="Riyaz" />
```

---

# 4. Props are like function arguments

This is a very useful mental model.

Normal JavaScript:

```javascript
function User(name) {
  return `Hello ${name}`;
}

User("Riyaz");
```

React:

```jsx
function User({ name }) {
  return <h1>Hello {name}</h1>;
}
```

Used as:

```jsx
<User name="Riyaz" />
```

So you can think:

```text
Function arguments
       ≈
Component props
```

They're not exactly the same thing internally, but this comparison is useful for understanding the concept.

---

# 5. Multiple Props

A component can receive multiple props.

```jsx
function User({ name, age, role }) {
  return (
    <div>
      <h2>{name}</h2>
      <p>Age: {age}</p>
      <p>Role: {role}</p>
    </div>
  );
}
```

Parent:

```jsx
<User
  name="Riyaz"
  age={25}
  role="Frontend Developer"
/>
```

The child receives:

```text
name → Riyaz
age  → 25
role → Frontend Developer
```

Notice something important.

Strings can be passed directly:

```jsx
name="Riyaz"
```

JavaScript expressions use `{}`:

```jsx
age={25}
```

---

# 6. Why do we use `{}` with Props?

Consider:

```jsx
<User age={25} />
```

The `{}` tells JSX:

> "Evaluate this as a JavaScript expression."

For example:

```jsx
<User age={25} />
<User age={10 + 15} />
<User age={user.age} />
<User age={getAge()} />
```

These are JavaScript expressions.

But:

```jsx
<User age="25" />
```

passes a string:

```text
"25"
```

not a number:

```text
25
```

This distinction is important.

---

# 7. Example: String vs Number

Consider:

```jsx
<User age="25" />
```

The child receives:

```javascript
typeof age
// "string"
```

But:

```jsx
<User age={25} />
```

gives:

```javascript
typeof age
// "number"
```

This can cause bugs.

For example:

```jsx
<User age="25" />
```

and then:

```javascript
age + 1
```

may result in:

```text
251
```

because `"25"` is a string.

Whereas:

```jsx
<User age={25} />
```

gives:

```text
26
```

So always be aware of the data type you're passing.

---

# 8. Destructuring Props

You can access props like this:

```jsx
function User(props) {
  return (
    <div>
      <h1>{props.name}</h1>
      <p>{props.role}</p>
    </div>
  );
}
```

But you will commonly see:

```jsx
function User({ name, role }) {
  return (
    <div>
      <h1>{name}</h1>
      <p>{role}</p>
    </div>
  );
}
```

This is **JavaScript destructuring**.

It doesn't change how props work.

It simply makes the code shorter and easier to read.

---

# 9. Interview Question: Are Props Mutable?

### Answer:

> **A component should treat its props as read-only. It should not directly modify the props it receives.**

For example, don't do:

```jsx
function User(props) {
  props.name = "John";

  return <h1>{props.name}</h1>;
}
```

The child shouldn't modify the parent's data through the props object.

Instead, if something needs to change, the parent should own that changing data and provide an appropriate way for the child to request the change.

---

# 10. Why are Props Read-Only?

Think about ownership.

Suppose:

```text
Parent owns:
user = {
  name: "Riyaz"
}
```

Parent passes:

```text
user
 ↓
Child
```

The parent owns the data.

The child receives a value.

If the child could freely modify the parent's data, it would become difficult to understand:

```text
Who changed this?
When did it change?
Which component changed it?
Why did it change?
```

React's one-way data flow helps keep this easier to reason about.

---

# 11. Can Props Contain Objects?

Absolutely.

```jsx
function App() {
  const user = {
    name: "Riyaz",
    role: "Frontend Developer"
  };

  return <User user={user} />;
}
```

Child:

```jsx
function User({ user }) {
  return (
    <div>
      <h2>{user.name}</h2>
      <p>{user.role}</p>
    </div>
  );
}
```

Here:

```text
user
 ↓
{
  name: "Riyaz",
  role: "Frontend Developer"
}
```

is passed as one prop.

---

# 12. Can Props Contain Arrays?

Yes.

```jsx
function App() {
  const users = ["Riyaz", "John", "Sarah"];

  return <UserList users={users} />;
}
```

Child:

```jsx
function UserList({ users }) {
  return (
    <ul>
      {users.map(user => (
        <li key={user}>{user}</li>
      ))}
    </ul>
  );
}
```

Props aren't limited to strings.

They can contain:

* Strings
* Numbers
* Booleans
* Arrays
* Objects
* Functions
* React elements
* Other values

---

# 13. Can we pass a Function as a Prop?

Yes.

This is **extremely important**.

For example:

```jsx
function App() {
  function handleClick() {
    console.log("Button clicked");
  }

  return <Button onClick={handleClick} />;
}
```

Child:

```jsx
function Button({ onClick }) {
  return (
    <button onClick={onClick}>
      Click Me
    </button>
  );
}
```

Here:

```text
Parent
   │
   │ function
   ↓
Child
```

The child can call the function:

```jsx
onClick();
```

This is commonly used for communication from child back to parent.

---

# 14. How does Child communicate with Parent?

This is one of the **most frequently asked React interview questions**.

Suppose:

```jsx
function Parent() {
  const handleMessage = (message) => {
    console.log(message);
  };

  return <Child onMessage={handleMessage} />;
}
```

Child:

```jsx
function Child({ onMessage }) {
  return (
    <button onClick={() => onMessage("Hello Parent")}>
      Send
    </button>
  );
}
```

Flow:

```text
Parent
   │
   │ passes function
   ↓
Child
   │
   │ calls function
   ↓
Parent
```

The child doesn't directly modify the parent.

It invokes a function provided by the parent.

### Strong interview answer

> "React normally follows one-way data flow from parent to child. If a child needs to communicate something back to the parent, the parent can pass a callback function as a prop. The child calls that function, and the parent handles the event or updates its state."

That's a good interview answer.

---

# 15. Props Can Pass React Elements

This is where props become more interesting.

You can do:

```jsx
<Card>
  <h1>Hello</h1>
</Card>
```

The content inside `Card` becomes:

```jsx
props.children
```

For example:

```jsx
function Card({ children }) {
  return (
    <div className="card">
      {children}
    </div>
  );
}
```

This is technically a prop.

So:

> **`children` is a special prop provided by JSX nesting.**

---

# 16. What is `children` actually?

Consider:

```jsx
<Card>
  <h1>Hello</h1>
</Card>
```

Conceptually, React passes something like:

```javascript
{
  children: <h1>Hello</h1>
}
```

So:

```jsx
function Card(props) {
  return <div>{props.children}</div>;
}
```

renders:

```html
<div>
  <h1>Hello</h1>
</div>
```

---

# 17. `children` can contain more than one thing

For example:

```jsx
<Card>
  <h1>Hello</h1>
  <p>Welcome</p>
  <button>Click</button>
</Card>
```

Then `children` represents that nested content.

It can also be:

```jsx
<Card>
  <UserProfile />
</Card>
```

or:

```jsx
<Card>
  Hello
</Card>
```

This is why `children` is very useful for reusable layout components.

---

# 18. Default Props / Default Values

What if a prop isn't provided?

```jsx
function User({ name }) {
  return <h1>{name}</h1>;
}
```

If:

```jsx
<User />
```

then:

```text
name = undefined
```

You can provide a default value using JavaScript destructuring:

```jsx
function User({ name = "Guest" }) {
  return <h1>{name}</h1>;
}
```

Now:

```jsx
<User />
```

produces:

```text
Guest
```

This is a useful modern pattern.

---

# 19. Boolean Props

Consider:

```jsx
<Button disabled />
```

This is shorthand for:

```jsx
<Button disabled={true} />
```

And:

```jsx
<Button disabled={false} />
```

explicitly passes false.

Inside:

```jsx
function Button({ disabled }) {
  return (
    <button disabled={disabled}>
      Submit
    </button>
  );
}
```

Boolean props are common in React.

Examples:

```jsx
<Input required />
<Modal open />
<Button disabled />
```

---

# 20. Passing Everything Using Spread Props

You might see:

```jsx
function App() {
  const user = {
    name: "Riyaz",
    age: 25,
    role: "Developer"
  };

  return <User {...user} />;
}
```

This is equivalent to passing:

```jsx
<User
  name="Riyaz"
  age={25}
  role="Developer"
/>
```

The `...` is JavaScript object spread syntax.

### Interview warning

Spread props are convenient, but don't use them blindly.

For example:

```jsx
<User {...hugeObject} />
```

can make it unclear which props the component actually depends on.

Explicit props are sometimes easier to understand:

```jsx
<User
  name={user.name}
  role={user.role}
/>
```

---

# 21. Prop Drilling

Now we reach an important intermediate concept.

Suppose:

```text
App
 ↓
Dashboard
 ↓
UserPanel
 ↓
UserName
```

But only `UserName` needs the user's name.

You might have to pass it through every level:

```jsx
<App user={user} />
```

then:

```jsx
<Dashboard user={user} />
```

then:

```jsx
<UserPanel user={user} />
```

then:

```jsx
<UserName user={user} />
```

This is called **prop drilling**.

---

# 22. Is Prop Drilling Bad?

Not automatically.

This is important.

Beginners sometimes hear:

> "Prop drilling is bad."

That's too extreme.

For a small number of levels, props are often the simplest and clearest solution.

Example:

```text
Parent
 ↓
Child
 ↓
Grandchild
```

Passing a prop through these levels may be perfectly reasonable.

The problem becomes more noticeable when:

```text
A
 ↓
B
 ↓
C
 ↓
D
 ↓
E
 ↓
F
```

and only `F` actually needs the data.

Then you may consider alternatives such as:

* component composition
* Context
* state management libraries

We'll cover those later.

---

# 23. Interview Question: How would you solve Prop Drilling?

Don't immediately say:

> "Use Redux."

That's usually not a good answer.

First ask:

> "Does the data really need to be global?"

Possible solutions include:

### 1. Composition

Sometimes you can restructure components so the data doesn't need to travel through unrelated components.

### 2. Context

If many components need the same value:

```jsx
<UserContext.Provider>
```

then consumers can access it without manually passing the prop through every level.

### 3. State management

For more complex application-wide state, tools such as Redux, Zustand, or other state-management approaches may be appropriate.

The correct choice depends on the problem.

---

# 24. Props vs State

This is one of the most important React interview comparisons.

| Props                                            | State                            |
| ------------------------------------------------ | -------------------------------- |
| Passed into component                            | Managed by component             |
| Usually comes from parent                        | Usually belongs to component     |
| Read-only from receiving component's perspective | Updated through state mechanisms |
| Used to configure a component                    | Used for changing data           |
| External input                                   | Internal/owned data              |

Example:

```jsx
function Counter({ title }) {
  const [count, setCount] = useState(0);

  return (
    <>
      <h1>{title}</h1>
      <p>{count}</p>
    </>
  );
}
```

Here:

```text
title → prop
count → state
```

---

# 25. Advanced Interview Question: What happens if a prop changes?

Suppose:

```jsx
<User name={name} />
```

and the parent's `name` changes.

React may render the parent again.

The child receives the new prop.

For example:

```text
name = "Riyaz"
        ↓
name = "John"
```

The child now receives:

```text
name = "John"
```

React then determines what UI changes are needed.

Important:

> **A prop changing doesn't mean React throws away the entire child DOM.**

The component is rendered with the new inputs, and React determines the necessary updates.

---

# 26. Advanced Question: Are Props Passed by Reference?

This question needs a careful answer.

JavaScript values are passed to functions according to JavaScript's normal value semantics.

For objects and arrays, the value being passed is a **reference to the object**.

Example:

```jsx
const user = {
  name: "Riyaz"
};

<User user={user} />
```

The child receives the same object reference.

So:

```text
Parent
  user ───────────┐
                 ↓
             Object
                 ↑
  Child ─────────┘
```

This is why mutating an object directly can cause problems.

For example:

```jsx
user.name = "John";
```

is generally not how you should manage React state.

Instead, when the object is state, create a new object:

```jsx
setUser({
  ...user,
  name: "John"
});
```

We'll study this deeply when we reach **state and immutability**.

---

# 27. Advanced Question: Why does object identity matter with props?

Consider:

```jsx
const user = {
  name: "Riyaz"
};

<User user={user} />
```

On another render, if you create:

```jsx
const user = {
  name: "Riyaz"
};
```

you have created a **new object**.

Even though the contents look the same:

```text
{ name: "Riyaz" }
```

the references are different.

```javascript
{} === {}
```

is:

```text
false
```

This becomes important with:

* `React.memo`
* `useMemo`
* `useCallback`
* performance optimization

We'll revisit this later.

---

# 28. Tricky Question: Can Props Be Functions?

Yes.

In fact, this is very common.

```jsx
<Button onClick={handleClick} />
```

Here:

```text
onClick
```

is a prop whose value is a function.

The child can call it:

```jsx
onClick();
```

This is how we often pass behavior into reusable components.

---

# 29. Tricky Question: Can Props Be Components?

Yes, indirectly through React elements or component references.

For example:

```jsx
function Layout({ header }) {
  return (
    <div>
      {header}
    </div>
  );
}
```

Use:

```jsx
<Layout header={<Header />} />
```

Here `header` is a React element.

Another pattern is passing a component itself:

```jsx
<Layout Header={Header} />
```

Then:

```jsx
function Layout({ Header }) {
  return <Header />;
}
```

These patterns are useful but should be chosen based on the design of the component.

---

# 30. Props are Inputs

A very useful way to think about components is:

```text
Component
    ↑
    │
   Props
    │
    ↓
UI
```

Or more simply:

```text
Props + State
      ↓
   Component
      ↓
      UI
```

For example:

```jsx
function UserCard({ name, role }) {
  return (
    <div>
      <h2>{name}</h2>
      <p>{role}</p>
    </div>
  );
}
```

The component doesn't need to know where the data came from.

It just receives:

```text
name
role
```

and uses them.

That's what makes components reusable.

---

# 31. Common Interview Mistakes

### ❌ "Props are used only for strings."

No.

Props can contain almost any JavaScript value that makes sense:

```text
string
number
boolean
array
object
function
React element
etc.
```

---

### ❌ "Props can never change."

Not exactly.

The receiving component shouldn't modify its props.

But the **parent can pass a different prop value on a later render**.

So this is better:

> Props are read-only from the receiving component's perspective.

---

### ❌ "Child components can directly change parent state."

Not directly.

The parent can pass a callback to the child.

---

### ❌ "Prop drilling is always bad."

No.

It can be perfectly fine for simple component trees.

---

### ❌ "Use Redux whenever there is prop drilling."

Not necessarily.

First understand the data flow and decide whether composition, Context, local state, or a state-management solution makes sense.

---

# 32. Interview Questions — Beginner → Advanced

### Beginner

1. What are props in React?
2. Why do we use props?
3. How do you pass props?
4. How do you access props?
5. Can a component receive multiple props?
6. Can props contain numbers and booleans?
7. What is destructuring of props?
8. What is the `children` prop?

### Intermediate

9. Can we pass functions as props?
10. How can a child communicate with a parent?
11. What is prop drilling?
12. Is prop drilling always bad?
13. Props vs state?
14. What happens when a prop changes?
15. What are default prop values?
16. What are boolean props?
17. What is the spread syntax with props?

### Advanced

18. Why shouldn't props be mutated?
19. How does object identity affect props?
20. Why can passing a new object cause unnecessary renders?
21. How does `React.memo` interact with props?
22. How can composition reduce prop drilling?
23. When should you use Context instead of props?
24. Can components themselves be passed through props?
25. What's the difference between passing a React element and passing a component?

---

# The most important interview answer

If the interviewer asks:

> **"Explain props in React."**

A strong, natural answer would be:

> **"Props are inputs passed from a parent component to a child component. They allow us to make components reusable because the same component can receive different data. Props can contain values such as strings, numbers, objects, arrays, functions, or React elements. The receiving component should treat props as read-only. If a child needs to communicate something back to the parent, the parent can pass a callback function as a prop. This keeps React's normal data flow going from parent to child."**

That's the answer I'd want you to be comfortable saying in an interview.

---

## Quick mental model

Remember:

```text
                Parent
                  │
                  │ props
                  ↓
                Child
```

Props are **inputs**.

```text
Props
  ↓
Component
  ↓
UI
```

For child → parent communication:

```text
Parent
  │
  │ callback prop
  ↓
Child
  │
  │ calls callback
  ↓
Parent
```

And for deeper trees:

```text
Parent
  ↓
Child
  ↓
Grandchild
  ↓
GreatGrandchild
```

If data has to travel through components that don't actually need it, you may have **prop drilling**.

---

### Next concept: State & `useState`

This is where React becomes much more interesting.

We'll cover **what state really is, why normal variables don't work, `useState`, state updates, batching, functional updates, multiple state variables, objects/arrays in state, immutability, lazy initialization, and the tricky interview questions around state updates**.

---
---
---

# React Interview Preparation — Concept 4

# State & `useState`

If **props are inputs to a component**, then **state is data that a component needs to remember and manage over time**.

This is one of the most important React concepts, and interviewers ask a lot of questions around it.

---

# 1. What is State?

### Traditional interview definition

> **State is data that is managed by a React component and can change over time. When state changes, React can render the component again to reflect the new state in the UI.**

Simple version:

> **State is information that a component remembers and that can change during the component's lifetime.**

For example:

A counter needs to remember:

```text
count = 0
```

When the user clicks:

```text
count = 1
```

Then:

```text
count = 2
```

That changing value is a good example of state.

---

# 2. Simple Example

```jsx
import { useState } from "react";

function Counter() {
  const [count, setCount] = useState(0);

  return (
    <div>
      <p>Count: {count}</p>

      <button onClick={() => setCount(count + 1)}>
        Increment
      </button>
    </div>
  );
}
```

Here:

```text
count
```

is the current state.

And:

```text
setCount
```

is the function we use to request a state update.

Initial value:

```text
0
```

---

# 3. What does `useState(0)` mean?

This line:

```jsx
const [count, setCount] = useState(0);
```

does several things.

### `useState`

`useState` is a React Hook that lets a function component have state.

### `0`

This is the initial state value.

### `count`

This is the current state value.

### `setCount`

This is the state setter function.

So mentally:

```text
useState(0)
    ↓
┌─────────────────────┐
│ current value: 0    │
│ update function      │
└─────────────────────┘
```

---

# 4. Why can't we just use a normal variable?

This is one of the **most important beginner interview questions**.

You might try:

```jsx
function Counter() {
  let count = 0;

  function increment() {
    count = count + 1;
  }

  return (
    <>
      <p>{count}</p>
      <button onClick={increment}>
        Increment
      </button>
    </>
  );
}
```

You might think:

```text
Click
 ↓
count becomes 1
 ↓
UI should show 1
```

But that's not how React works.

Changing:

```javascript
count = count + 1;
```

doesn't tell React:

> "The UI needs to be updated."

Also, when React renders the component again, the local variable is recreated.

So we use state:

```jsx
const [count, setCount] = useState(0);
```

and:

```jsx
setCount(count + 1);
```

Now React knows that the state has changed and can schedule another render.

---

# 5. State gives React a way to remember information

Think about a component render.

A normal local variable:

```jsx
function Counter() {
  let count = 0;
}
```

is recreated when the component function runs.

State is different:

```jsx
const [count, setCount] = useState(0);
```

React maintains the state value across renders.

So conceptually:

```text
First render
count = 0

        ↓ setCount(1)

Second render
count = 1

        ↓ setCount(2)

Third render
count = 2
```

This is why state is useful.

---

# 6. What happens when we call the setter?

Suppose:

```jsx
setCount(1);
```

A beginner might think:

```text
setCount(1)
    ↓
count immediately becomes 1
    ↓
DOM immediately changes
```

That's not the best mental model.

A better model is:

```text
setCount(1)
     ↓
React schedules a state update
     ↓
React renders the component with the new state
     ↓
React determines required UI changes
     ↓
React commits necessary DOM changes
```

This distinction becomes very important later.

---

# 7. Is the state update immediate?

This is a classic interview question.

### Question:

> "Is `setCount()` synchronous?"

A safe interview answer is:

> **"Calling a state setter doesn't immediately change the current render's state variable. React schedules the update and then renders with the new state. React may also batch multiple updates together."**

For example:

```jsx
function Counter() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(1);

    console.log(count);
  }

  // ...
}
```

If this handler runs when `count` is `0`, the `console.log` can still see:

```text
0
```

Why?

Because the current render's `count` is still `0`.

The update will be reflected in a later render.

---

# 8. Very Important Mental Model: State belongs to a Render

This is a deeper React concept.

Suppose:

```jsx
const [count, setCount] = useState(0);
```

During one render:

```text
count = 0
```

That render sees `count` as `0`.

If you call:

```jsx
setCount(1);
```

you're asking React for another render where:

```text
count = 1
```

So don't think of `count` as one ordinary variable that is constantly changing.

A better mental model is:

> **Each render sees a snapshot of state.**

This becomes extremely important when we discuss:

* batching
* closures
* effects
* event handlers
* stale state

---

# 9. Multiple State Updates

Consider:

```jsx
function Counter() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(count + 1);
    setCount(count + 1);
    setCount(count + 1);
  }

  // ...
}
```

A beginner might expect:

```text
0 → 1 → 2 → 3
```

But that's not what happens.

All three expressions use the `count` value from the **same render**.

If the current render has:

```text
count = 0
```

then all three effectively request:

```text
setCount(1)
setCount(1)
setCount(1)
```

So the result is generally:

```text
1
```

This is a very common interview trap.

---

# 10. Functional State Updates

When the next state depends on the previous state, use the functional form.

Instead of:

```jsx
setCount(count + 1);
```

you can write:

```jsx
setCount(prevCount => prevCount + 1);
```

Now:

```jsx
setCount(prev => prev + 1);
setCount(prev => prev + 1);
setCount(prev => prev + 1);
```

React can apply these updates based on the previous state.

Conceptually:

```text
0
 ↓
1
 ↓
2
 ↓
3
```

Result:

```text
3
```

### Interview answer

> "When the next state depends on the previous state, I prefer the functional updater form because React can apply each update using the latest queued state value."

That's a very useful answer.

---

# 11. Why does the Functional Form Work?

Compare:

### Direct value

```jsx
setCount(count + 1);
```

This uses the `count` value captured by the current render.

### Functional updater

```jsx
setCount(prev => prev + 1);
```

This tells React:

> "Take the previous state value and calculate the next value from it."

So:

```text
Current state
     ↓
prev
     ↓
calculate next state
```

This is especially useful when multiple updates can happen before React renders again.

---

# 12. State Can Store Any JavaScript Value

State isn't limited to numbers.

You can have:

### String

```jsx
const [name, setName] = useState("");
```

### Boolean

```jsx
const [isOpen, setIsOpen] = useState(false);
```

### Array

```jsx
const [users, setUsers] = useState([]);
```

### Object

```jsx
const [user, setUser] = useState({
  name: "Riyaz",
  age: 25
});
```

### Number

```jsx
const [count, setCount] = useState(0);
```

---

# 13. Multiple State Variables

You can have multiple pieces of state.

```jsx
function User() {
  const [name, setName] = useState("Riyaz");
  const [age, setAge] = useState(25);
  const [isOnline, setIsOnline] = useState(true);

  // ...
}
```

That's perfectly valid.

Conceptually:

```text
User
│
├── name
├── age
└── isOnline
```

You don't have to put everything into one object.

---

# 14. One State Object vs Multiple State Variables

You might see:

```jsx
const [user, setUser] = useState({
  name: "Riyaz",
  age: 25,
  role: "Developer"
});
```

Or:

```jsx
const [name, setName] = useState("Riyaz");
const [age, setAge] = useState(25);
const [role, setRole] = useState("Developer");
```

Which is better?

There isn't one universal answer.

A useful rule is:

> **Group state when the values naturally belong together and are updated together. Keep state separate when the values have independent meanings or update independently.**

Don't create one giant state object just because the data is related to the same screen.

---

# 15. Updating Object State

Suppose:

```jsx
const [user, setUser] = useState({
  name: "Riyaz",
  age: 25
});
```

You should not do:

```jsx
user.name = "John";
```

That directly mutates the existing object.

Instead:

```jsx
setUser({
  ...user,
  name: "John"
});
```

The spread creates a new object.

Result:

```text
Before:
{
  name: "Riyaz",
  age: 25
}

After:
{
  name: "John",
  age: 25
}
```

---

# 16. Why Don't We Mutate State Directly?

This is a **very important interview topic**.

Bad:

```jsx
user.name = "John";
```

Good:

```jsx
setUser({
  ...user,
  name: "John"
});
```

React state should generally be treated as **immutable**.

Why?

Because React relies heavily on comparing values and references to understand whether things have changed.

If you mutate an existing object:

```text
same object
   ↓
changed internally
```

you make change detection and reasoning more difficult.

Creating a new object:

```text
old object
     ↓
new object
```

makes the update explicit.

It also works correctly with React patterns and optimizations.

---

# 17. Updating Arrays in State

Suppose:

```jsx
const [users, setUsers] = useState([]);
```

To add a user:

```jsx
setUsers([
  ...users,
  newUser
]);
```

Don't do:

```jsx
users.push(newUser);
setUsers(users);
```

because `push()` mutates the existing array.

Instead:

```jsx
setUsers(prevUsers => [
  ...prevUsers,
  newUser
]);
```

---

# 18. Removing an Item from an Array

Suppose:

```jsx
const [users, setUsers] = useState([
  { id: 1, name: "Riyaz" },
  { id: 2, name: "John" }
]);
```

To remove John:

```jsx
setUsers(prevUsers =>
  prevUsers.filter(user => user.id !== 2)
);
```

The `filter()` creates a new array.

---

# 19. Updating an Item in an Array

You can use `map()`:

```jsx
setUsers(prevUsers =>
  prevUsers.map(user =>
    user.id === 1
      ? { ...user, name: "Ahmed" }
      : user
  )
);
```

This creates:

* a new array
* a new object for the changed user
* the existing object for unchanged users

This pattern is very common in React applications.

---

# 20. Lazy State Initialization

Here's a slightly more advanced concept.

Suppose calculating the initial state is expensive:

```jsx
const [data, setData] = useState(expensiveCalculation());
```

The expression is evaluated when the component function runs.

If you want React to initialize state using a function, you can write:

```jsx
const [data, setData] = useState(() => expensiveCalculation());
```

The function is used to calculate the initial state.

This is called **lazy initialization**.

### Important distinction

These are different:

```jsx
useState(expensiveCalculation())
```

and:

```jsx
useState(() => expensiveCalculation())
```

The second one gives React an initializer function.

You don't need lazy initialization for every state variable. It is useful when calculating the initial value is meaningfully expensive.

---

# 21. State Setter Can Also Receive a Function

Don't confuse these two concepts.

### Functional updater

```jsx
setCount(prev => prev + 1);
```

Here the function calculates the **next state from the previous state**.

### Lazy initializer

```jsx
useState(() => expensiveCalculation());
```

Here the function calculates the **initial state**.

They solve different problems.

---

# 22. State and User Input

A common real-world example is a form.

```jsx
function LoginForm() {
  const [email, setEmail] = useState("");

  return (
    <input
      value={email}
      onChange={event => setEmail(event.target.value)}
    />
  );
}
```

Flow:

```text
User types
   ↓
onChange event
   ↓
setEmail(...)
   ↓
state updates
   ↓
component renders
   ↓
input receives new value
```

This is the foundation of **controlled components**.

We'll study forms separately.

---

# 23. State and Conditional Rendering

State can determine what UI is shown.

```jsx
function Modal() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <button onClick={() => setIsOpen(true)}>
        Open
      </button>

      {isOpen && <div>Modal is open</div>}
    </>
  );
}
```

Initially:

```text
isOpen = false
```

So:

```text
Modal is open
```

isn't rendered.

After:

```jsx
setIsOpen(true);
```

the component renders with:

```text
isOpen = true
```

and the modal appears.

This pattern is everywhere in React.

---

# 24. State Does Not Automatically Mean Global State

Another common misunderstanding:

> "If something is state, it should be in Redux."

No.

Most state should initially be kept as close as possible to where it is needed.

For example:

```jsx
function SearchBox() {
  const [query, setQuery] = useState("");
}
```

There's no reason to make a simple search input globally available just because it's state.

A useful principle is:

> **Keep state local unless there is a real reason to share it.**

Later we'll discuss **lifting state up**, Context, and global state management.

---

# 25. Lifting State Up

Suppose two sibling components need the same data.

```text
        Parent
        /    \
       /      \
  Input      Display
```

If the state belongs to `Input`:

```text
Input
 ↓
query
```

then `Display` can't directly access it.

We can move the state to the common parent:

```text
        Parent
          │
        query
        /   \
       ↓     ↓
    Input  Display
```

Parent:

```jsx
function Parent() {
  const [query, setQuery] = useState("");

  return (
    <>
      <Input query={query} setQuery={setQuery} />
      <Display query={query} />
    </>
  );
}
```

This is called:

> **Lifting state up.**

We'll cover it as its own concept later.

---

# 26. What causes a component to render again?

A component can render again for several reasons.

One important reason is:

> **Its state is updated.**

For example:

```jsx
setCount(10);
```

can cause React to render the component again.

Other reasons include:

* parent rendering
* context changes
* external subscriptions or other React mechanisms

We'll study rendering in detail later.

---

# 27. Does Every State Update Cause a DOM Update?

No.

This is an important distinction.

Suppose:

```jsx
const [count, setCount] = useState(0);
```

You update:

```jsx
setCount(1);
```

React may render the component again.

But React then determines what actually changed.

If the UI is:

```jsx
<h1>{count}</h1>
```

the text needs to change.

But if some other part of the DOM didn't change, React doesn't need to recreate that part.

So:

```text
State update
    ↓
Render
    ↓
Compare/reconcile
    ↓
Necessary DOM changes
```

A **render** and a **DOM update** are not the same thing.

---

# 28. What if we set state to the same value?

Suppose:

```jsx
const [count, setCount] = useState(10);
```

Then:

```jsx
setCount(10);
```

React can recognize that the new state is the same value and may avoid unnecessary work.

For primitive values, React uses `Object.is`-style comparison semantics for state equality.

This is one reason immutable updates matter for objects and arrays.

For example:

```jsx
setUser(user);
```

passes the same object reference.

Whereas:

```jsx
setUser({
  ...user
});
```

creates a new object reference.

That difference matters.

---

# 29. A Very Important Interview Question

### Question:

> "Why shouldn't we use `setState` and then immediately expect the state variable to have the new value?"

Because the state variable belongs to the current render.

Example:

```jsx
function Counter() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(count + 1);

    console.log(count);
  }

  // ...
}
```

The log can still be:

```text
0
```

because this event handler is working with the current render's snapshot.

The next render will see:

```text
1
```

If you need to perform something after a state change has been committed, that's a different problem and is often handled with an Effect or another appropriate pattern.

---

# 30. Common State Mistakes

### ❌ Mistake 1: Direct mutation

```jsx
user.name = "John";
```

Prefer:

```jsx
setUser({
  ...user,
  name: "John"
});
```

---

### ❌ Mistake 2: Using a normal variable

```jsx
let count = 0;
```

Use state when the value should persist across renders and affect the UI:

```jsx
const [count, setCount] = useState(0);
```

---

### ❌ Mistake 3: Using stale state for multiple updates

Instead of:

```jsx
setCount(count + 1);
setCount(count + 1);
```

use:

```jsx
setCount(prev => prev + 1);
setCount(prev => prev + 1);
```

when each update depends on the previous result.

---

### ❌ Mistake 4: Making everything global

Not every state value belongs in a global store.

Start with local state when possible.

---

# 31. Beginner → Advanced Interview Questions

### Beginner

1. What is state in React?
2. What is `useState`?
3. Why do we need state?
4. What is the initial state?
5. What does the setter function do?
6. Can state contain objects?
7. Can state contain arrays?
8. Can a component have multiple state variables?

### Intermediate

9. Why can't we use a normal variable instead of state?
10. What happens when state changes?
11. What is a functional state update?
12. Why do we use `prev => prev + 1`?
13. How do you update an object in state?
14. How do you update an array in state?
15. Why shouldn't state be mutated directly?
16. What is lazy initialization?
17. What is lifting state up?
18. When should state be local?

### Advanced

19. Is a state update synchronous?
20. Why doesn't the state variable immediately change after calling the setter?
21. What is batching?
22. Why do multiple `setState` calls sometimes produce one final value?
23. What is a state snapshot?
24. How does React determine whether state changed?
25. Why does object identity matter in state?
26. What happens when setting the same state value?
27. What causes a component to re-render?
28. Does every state update cause a DOM update?
29. What is stale state?
30. How does state interact with closures?

---

# 32. Strong Interview Answer

If the interviewer asks:

> **"What is state in React?"**

A natural answer would be:

> **"State is data that a component needs to remember and that can change over time. We usually manage state in a function component using the `useState` Hook. The state value belongs to a particular render, and when we call its setter, React schedules an update and renders the component again with the new state. We shouldn't directly mutate state objects or arrays; instead, we normally create new values when updating them. If the next state depends on the previous state, I use the functional updater form, such as `setCount(prev => prev + 1)`."**

That's a much stronger answer than simply:

> "State is data that changes."

---

# The Mental Model You Should Remember

Think:

```text
                State
                  ↓
             Component
                  ↓
                  UI
```

When state changes:

```text
setState(...)
     ↓
React schedules update
     ↓
Component renders again
     ↓
React determines what changed
     ↓
React commits necessary DOM changes
```

And remember this very important sentence:

> **State is a snapshot for a render, not a normal variable that changes immediately in place.**

---

## Next Concept: Rendering & Re-rendering

Now that you understand **components → props → state**, the next concept should be **Rendering**.

We'll go deep into:

* What exactly is rendering?
* What does "re-render" actually mean?
* What causes a re-render?
* Does re-render mean DOM update?
* Render phase vs commit phase
* Virtual DOM
* Reconciliation
* Fiber
* Why React doesn't update the whole DOM
* Parent/child rendering
* Batching
* State snapshots
* Common rendering interview traps

This is where React starts becoming **much more interesting from an interview perspective**.


---

---
---
---

# React Interview Preparation — Concept 5

# Rendering & Re-rendering

This is where many React interviews become more interesting.

A lot of developers can explain:

> "State changes and the component re-renders."

But if the interviewer asks:

> **"What exactly happens during a re-render?"**

many people struggle.

So let's build this from the ground up.

---

# 1. What is Rendering in React?

### Traditional definition

> **Rendering is the process in which React calls a component and determines what the UI should look like for the current props and state.**

Simple version:

> **Rendering means React figures out what the UI should be based on the current data.**

For example:

```jsx id="o7w5a3"
function Greeting({ name }) {
  return <h1>Hello {name}</h1>;
}
```

If:

```text id="q0v7p4"
name = "Riyaz"
```

React determines that the UI should represent:

```text id="4xxp3c"
<h1>Hello Riyaz</h1>
```

If `name` later becomes:

```text id="7d9q8a"
John
```

React renders again and determines:

```text id="8q48l5"
<h1>Hello John</h1>
```

---

# 2. Rendering Does NOT Mean Updating the DOM

This distinction is **extremely important**.

Many beginners think:

```text id="y17dpg"
Render
  =
DOM update
```

That's not correct.

React has a process roughly like:

```text id="h2jv9y"
Update happens
     ↓
Render
     ↓
React determines what the UI should be
     ↓
Reconciliation
     ↓
Commit
     ↓
DOM is updated if necessary
```

So:

> **Rendering is about determining the UI. Committing is when React applies the required changes to the DOM.**

This distinction comes up frequently in advanced interviews.

---

# 3. What is a Re-render?

### Definition

> **A re-render is when React renders a component again because something relevant to its rendering may have changed.**

For example:

```jsx id="sm7y9x"
function Counter() {
  const [count, setCount] = useState(0);

  return <h1>{count}</h1>;
}
```

Initially:

```text id="jpw9b7"
count = 0
```

UI:

```text id="u8mmc8"
0
```

Then:

```jsx id="z4f4lw"
setCount(1);
```

React schedules an update.

The component renders again.

Now:

```text id="e8n6j5"
count = 1
```

React determines that the displayed text needs to change.

The DOM is then updated.

---

# 4. What Can Cause a Re-render?

Several things can cause React to render a component again.

The most common ones are:

### 1. State update

```jsx id="0efr3t"
setCount(10);
```

### 2. Parent renders

If a parent component renders again, React may render its children as part of processing that subtree.

### 3. Context changes

If a component consumes a context value and that context value changes, the consumer can render again.

### 4. External subscriptions

Libraries and React APIs can also cause updates through subscriptions.

For beginner interviews, remember:

```text id="0sn4qi"
State changes
     ↓
Possible re-render

Parent renders
     ↓
Child may render

Context changes
     ↓
Consumer may render
```

---

# 5. Does a Parent Re-render Always Mean the Child DOM Changes?

No.

This is a very common interview trap.

Suppose:

```jsx id="0u0x8a"
function Parent() {
  return <Child />;
}
```

If `Parent` renders again, React may process `Child` again.

But that does **not** mean:

```text id="u7d5e9k"
destroy Child DOM
     ↓
create Child DOM again
```

React compares the resulting UI and applies only the necessary changes.

So:

> **Component rendering and DOM manipulation are two different things.**

---

# 6. Example: Parent and Child

Consider:

```jsx id="5h2q5d"
function Parent() {
  const [count, setCount] = useState(0);

  return (
    <>
      <button onClick={() => setCount(count + 1)}>
        Increment
      </button>

      <Child />
    </>
  );
}
```

And:

```jsx id="09x9j7"
function Child() {
  console.log("Child rendered");

  return <p>Hello</p>;
}
```

When the button is clicked:

```text id="q1t3qk"
Parent state changes
       ↓
Parent renders
       ↓
React processes Child
       ↓
Child may render again
```

But the DOM for:

```html id="1d5p5d"
<p>Hello</p>
```

doesn't need to be recreated just because the component rendered again.

---

# 7. Rendering vs DOM Updating

Let's make this very clear.

Suppose the previous UI is:

```text id="2cx4h7"
<h1>Hello</h1>
<p>Age: 25</p>
```

New UI:

```text id="o1f8sk"
<h1>Hello</h1>
<p>Age: 26</p>
```

React determines:

```text id="y04p6v"
<h1>Hello</h1>
→ unchanged

<p>Age: 25</p>
→ changed
```

So only the necessary part needs to be updated.

Conceptually:

```text id="5ob7qj"
Old UI
  ↓
New UI
  ↓
Compare
  ↓
Find differences
  ↓
Apply necessary DOM changes
```

This process is related to **reconciliation**.

We'll discuss it more deeply later.

---

# 8. What is the Virtual DOM?

This is another very common interview question.

### Traditional definition

> **The Virtual DOM is a lightweight JavaScript representation of the UI that React uses as part of its rendering and reconciliation process.**

A simple conceptual example:

Your JSX:

```jsx id="a9x9w6"
<h1>Hello</h1>
```

can be represented as JavaScript data describing:

```text id="d7n9py"
type: h1
children: Hello
```

React works with this representation during rendering.

When the UI changes, React creates a new representation and compares it with the previous one to determine what needs to change.

---

# 9. Is the Virtual DOM an Actual DOM?

No.

This is a common interview question.

> **The Virtual DOM is not the browser's actual DOM.**

The browser has a real DOM:

```text id="d0ymmg"
Browser
   ↓
Actual DOM
```

React uses JavaScript representations of UI during its rendering process.

Conceptually:

```text id="c0f3m4"
React elements / Fiber data
          ↓
    React processing
          ↓
      Actual DOM
```

Don't say:

> "React stores the entire DOM in memory."

That's an oversimplification.

Modern React's implementation involves **Fiber nodes and internal data structures**, not simply a giant duplicate HTML tree.

---

# 10. Why do people say Virtual DOM makes React fast?

Be careful with this interview answer.

A weak answer:

> "Virtual DOM makes React faster than the real DOM."

That's too simplistic.

The DOM isn't simply "slow" in every situation, and Virtual DOM isn't magic.

A better answer:

> **"React uses an in-memory representation of the UI as part of its rendering process. It can compare the previous and next UI representations and determine the necessary DOM updates rather than requiring the application to manually update every affected DOM node."**

That's much more accurate.

---

# 11. What is Reconciliation?

### Traditional definition

> **Reconciliation is React's process of comparing the previous rendered result with the new rendered result to determine what changes are needed.**

Example:

Previous:

```text id="f9a2t7"
<h1>Hello</h1>
```

New:

```text id="4o9k3b"
<h1>Hello Riyaz</h1>
```

React recognizes that the element is still an `h1`, but its text changed.

So it can update the relevant DOM content.

Conceptually:

```text id="d2bg6a"
Previous UI
    ↓
New UI
    ↓
Reconciliation
    ↓
Required changes
```

---

# 12. Reconciliation Is More Than "Comparing HTML"

This is important at advanced levels.

React isn't simply doing a naive:

```text
old HTML string
vs
new HTML string
```

comparison.

It works with React's element tree and internal Fiber architecture.

React considers things such as:

* element type
* keys
* position in the tree
* component identity
* props
* state
* tree structure

We'll get into these details when we cover **Keys and Reconciliation**.

---

# 13. What is Fiber?

This is an advanced React interview topic.

### Traditional definition

> **Fiber is React's internal architecture for representing work on the component tree and managing rendering work.**

You don't need to memorize its entire implementation.

The important idea is:

> **Fiber allows React to represent rendering work in smaller units and gives React more control over scheduling and prioritizing that work.**

Conceptually:

```text id="q2jz5y"
React Tree
   ↓
Fiber nodes
   ↓
Rendering work
   ↓
Commit
```

Fiber became the foundation for many modern React rendering capabilities.

---

# 14. Why was Fiber introduced?

Older React rendering work was more difficult to interrupt.

If React had a large amount of work to perform, it could potentially keep the JavaScript thread busy for too long.

Fiber introduced an architecture where React can represent rendering work in units.

This supports concepts such as:

* interruptible rendering
* prioritization
* scheduling
* concurrent features

So when an interviewer asks:

> "Why does React have Fiber?"

A good answer is:

> **"Fiber is the internal architecture React uses to represent and schedule rendering work. It allows React to break work into units and gives the renderer more control over when and how that work is processed."**

---

# 15. Render Phase vs Commit Phase

This is one of the **most important advanced React concepts**.

React's update process can be thought of in two major stages.

## Render Phase

React determines:

> "What should the UI look like?"

It calculates the next tree.

This phase should be free of side effects.

---

## Commit Phase

React takes the result of the render work and applies the required changes.

For the DOM renderer, this includes things such as:

* inserting DOM nodes
* updating DOM properties
* removing DOM nodes
* running relevant effects at the appropriate point

Simplified:

```text id="x9j4v1"
           Update
              ↓
        Render Phase
              ↓
      Determine next UI
              ↓
        Commit Phase
              ↓
       Update the DOM
```

---

# 16. Why Should Rendering Be Pure?

This is a very important React principle.

Consider:

```jsx id="xw8xij"
function User() {
  console.log("Rendering");

  return <h1>Hello</h1>;
}
```

Logging is generally harmless.

But imagine:

```jsx id="p8e8v6"
function User() {
  sendMoneyToBank();
  
  return <h1>Hello</h1>;
}
```

That's a terrible idea.

Why?

Because rendering can happen more than once.

React needs rendering to be predictable.

So:

> **Components should be pure during rendering: given the same props, state, and relevant context, they should produce the same UI result without causing side effects.**

Side effects belong in appropriate places, such as event handlers or Effects, depending on the operation.

We'll study this deeply with `useEffect`.

---

# 17. Why Can React Render a Component More Than Once?

This is an excellent interview question.

A component may render multiple times because:

* its state changes
* its parent renders
* context changes
* an external subscription causes an update
* development tools/Strict Mode can intentionally invoke rendering patterns to help detect certain problems

The important point is:

> **You should not assume a component function runs exactly once.**

That's why rendering logic should be pure.

---

# 18. Strict Mode and Double Rendering

This is a common interview topic.

In development, React Strict Mode may intentionally invoke certain functions more than once to help identify unsafe side effects and other problems.

For example:

```jsx id="f0j1ob"
<React.StrictMode>
  <App />
</React.StrictMode>
```

A developer might see:

```text id="1cv7vh"
Component rendered
Component rendered
```

and think:

> "React is broken!"

It's not necessarily a bug.

Strict Mode development behavior is designed to expose code that isn't following React's expected patterns.

### Interview answer

> "In development, Strict Mode can intentionally re-run certain rendering-related logic to help detect accidental side effects and unsafe patterns. This behavior is development-only and should not be used as a reason to put side effects inside rendering."

---

# 19. Does Re-render Mean Remount?

**No.**

This is a very important distinction.

### Re-render

The component renders again.

### Remount

React removes the existing component instance/tree and creates a new one.

For example:

```text id="p9g2x3"
Re-render:
Component
   ↓
renders again
```

Whereas:

```text id="2d5v8v"
Remount:
Old component
   ↓
removed
   ↓
new component
```

A remount can reset state.

A normal re-render does not automatically reset the component's state.

---

# 20. Re-render vs Remount

| Re-render                               | Remount                                                |
| --------------------------------------- | ------------------------------------------------------ |
| Component renders again                 | Component is removed and recreated                     |
| State usually preserved                 | State is reset for the new instance                    |
| Can happen frequently                   | Happens when component identity changes/removal occurs |
| Does not necessarily recreate DOM nodes | Existing DOM may be removed/replaced                   |

This distinction is very useful when debugging React applications.

---

# 21. How can a component be Remounted?

One important way is by changing its `key`.

For example:

```jsx id="4b0qg5"
<UserForm key={userId} />
```

If:

```text id="v0ph5v"
userId = 1
```

changes to:

```text id="a2c8m8"
userId = 2
```

React sees a different key and can treat it as a different component identity.

That can cause the previous component to be removed and a new one created.

We'll study keys properly later.

---

# 22. Batching

Another important concept.

Suppose:

```jsx id="e6h7h1"
function handleClick() {
  setCount(prev => prev + 1);
  setName("John");
  setOpen(true);
}
```

React can batch multiple state updates so that it doesn't necessarily perform a separate rendering/commit cycle for every single update.

Conceptually:

```text id="h4k7r8"
Update 1 ─┐
Update 2 ─┼──→ Batch → Render → Commit
Update 3 ─┘
```

This can reduce unnecessary rendering work.

Modern React has automatic batching in many situations, not only traditional browser event handlers.

---

# 23. Why Does Batching Matter?

Imagine:

```jsx id="u8n9on"
setCount(prev => prev + 1);
setName("John");
setIsOpen(true);
setLoading(false);
```

If React independently rendered and committed after every update, it could do unnecessary work.

Batching allows React to group compatible updates.

This is one reason you shouldn't assume:

> "Every call to a state setter immediately causes a complete render and DOM update."

---

# 24. Does React Always Batch Everything?

Be careful with absolute statements.

Modern React performs automatic batching in many common scenarios, but exact behavior can depend on the API, renderer, and context.

For interviews, a good answer is:

> **"Modern React batches many state updates together to reduce unnecessary rendering work. When multiple updates happen within the same batch, React can process them together rather than committing separately for every setter call."**

That's safer than saying:

> "React always batches everything."

---

# 25. A Complete Example

Consider:

```jsx id="z6ihdu"
function Counter() {
  const [count, setCount] = useState(0);

  console.log("render", count);

  function handleClick() {
    setCount(prev => prev + 1);
  }

  return (
    <>
      <h1>{count}</h1>

      <button onClick={handleClick}>
        Increment
      </button>
    </>
  );
}
```

Initial render:

```text id="k9v3d7"
render 0
```

User clicks:

```text id="d9m6at"
setCount(prev => prev + 1)
```

React schedules an update.

Then:

```text id="y6qj4f"
render 1
```

React compares the new UI with the previous result:

```text id="t9r5s8"
<h1>0</h1>

vs

<h1>1</h1>
```

It determines the text needs updating.

Then the commit phase updates the DOM.

---

# 26. The Most Important Rendering Mental Model

Remember this:

```text id="w3t4qk"
           State / Props
                ↓
             Render
                ↓
      What should UI look like?
                ↓
         Reconciliation
                ↓
          Commit phase
                ↓
          Actual DOM
```

And:

> **Rendering is not the same thing as DOM updating.**

This one sentence can save you from several interview mistakes.

---

# 27. Interview Question: What causes a React component to re-render?

A strong answer:

> "A component can render again when its state changes, when its parent renders and React processes that part of the tree, when consumed context changes, or when another subscribed data source causes an update. A re-render means React runs the rendering process again; it doesn't necessarily mean that the DOM will be changed."

That's a strong intermediate-level answer.

---

# 28. Interview Question: Does React update the entire DOM on every render?

### Answer:

> **No.**

React calculates the new UI representation and determines what actually needs to change.

For example:

```text id="b5y7z4"
Old:
<h1>Hello</h1>
<p>Age: 25</p>

New:
<h1>Hello</h1>
<p>Age: 26</p>
```

React doesn't need to recreate the whole page.

Only the necessary change needs to be committed.

---

# 29. Interview Question: What is reconciliation?

Strong answer:

> **"Reconciliation is the process React uses to determine how the current rendered result differs from the previous one and what updates are needed. React uses its internal representation of the component tree, along with things such as element types and keys, to determine how to update the UI efficiently."**

---

# 30. Interview Question: What is the Virtual DOM?

Good answer:

> **"The Virtual DOM is a simplified term for React's in-memory representation of UI elements. React uses this representation during rendering and reconciliation to determine what changes need to be applied to the actual DOM. It isn't the browser DOM itself, and it shouldn't be thought of as simply a complete copy of the DOM."**

That last sentence shows deeper understanding.

---

# 31. Interview Question: What is Fiber?

Good answer:

> **"Fiber is React's internal architecture for representing rendering work. It allows React to break rendering work into units and gives React more control over scheduling and prioritizing that work. Fiber is an important foundation for modern React's rendering capabilities."**

You don't need to explain Fiber's entire source code in an interview.

---

# 32. Beginner → Advanced Questions

### Beginner

1. What is rendering?
2. What is a re-render?
3. What causes a component to re-render?
4. Does state change cause a re-render?
5. Does rendering mean the DOM is updated?

### Intermediate

6. What is the Virtual DOM?
7. What is reconciliation?
8. Does React update the entire DOM?
9. What happens when a parent re-renders?
10. Does a child always re-render when the parent renders?
11. What is batching?
12. Why doesn't state update immediately?

### Advanced

13. What is the render phase?
14. What is the commit phase?
15. What is Fiber?
16. Why was Fiber introduced?
17. Why should render logic be pure?
18. What is the difference between re-render and remount?
19. How do keys affect component identity?
20. What happens internally after a state update?
21. How does React decide which DOM changes to commit?
22. How does concurrent rendering relate to Fiber?

---

# 33. One Important Correction to Common Interview Answers

You may hear:

> "React is fast because Virtual DOM is faster than the real DOM."

Don't repeat this blindly.

A better understanding is:

> **React provides a declarative programming model and manages DOM updates for you. Its rendering and reconciliation system helps determine the necessary updates rather than requiring developers to manually manipulate the DOM throughout the application.**

Performance depends on many things:

* amount of work
* component structure
* state placement
* unnecessary renders
* DOM complexity
* expensive calculations
* network operations
* browser work

There isn't one magic "Virtual DOM makes everything fast" explanation.

---

# What you should remember

If you remember only these six points:

### 1.

> **Rendering means React determines what the UI should look like.**

### 2.

> **A re-render does not automatically mean a DOM update.**

### 3.

> **Reconciliation determines what has changed.**

### 4.

> **The commit phase applies the necessary changes.**

### 5.

> **Fiber is React's internal architecture for managing rendering work.**

### 6.

> **Render logic should be pure because React may render more than once.**

The overall picture:

```text id="f4c4vk"
          State / Props change
                   ↓
             React update
                   ↓
              Render phase
                   ↓
        Determine next UI tree
                   ↓
             Reconciliation
                   ↓
              Commit phase
                   ↓
            Actual DOM update
```

---

## Next Concept: Virtual DOM & Reconciliation — Deep Dive

We've introduced these two concepts, but **don't move past them too quickly for interviews**.

Next we'll take **Virtual DOM + Reconciliation** as a dedicated concept and go deeper into:

* What a React element actually is
* Virtual DOM vs real DOM
* Reconciliation algorithm
* Element type comparison
* Why `<div>` → `<span>` behaves differently
* How React preserves component state
* How `key` affects reconciliation
* Why array indexes as keys can cause bugs
* Reconciliation with lists
* Fiber's role
* Common interview traps and advanced questions


---
---
---

# React Interview Preparation — Concept 5

# Rendering & Re-rendering

This is where many React interviews become more interesting.

A lot of developers can explain:

> "State changes and the component re-renders."

But if the interviewer asks:

> **"What exactly happens during a re-render?"**

many people struggle.

So let's build this from the ground up.

---

# 1. What is Rendering in React?

### Traditional definition

> **Rendering is the process in which React calls a component and determines what the UI should look like for the current props and state.**

Simple version:

> **Rendering means React figures out what the UI should be based on the current data.**

For example:

```jsx id="o7w5a3"
function Greeting({ name }) {
  return <h1>Hello {name}</h1>;
}
```

If:

```text id="q0v7p4"
name = "Riyaz"
```

React determines that the UI should represent:

```text id="4xxp3c"
<h1>Hello Riyaz</h1>
```

If `name` later becomes:

```text id="7d9q8a"
John
```

React renders again and determines:

```text id="8q48l5"
<h1>Hello John</h1>
```

---

# 2. Rendering Does NOT Mean Updating the DOM

This distinction is **extremely important**.

Many beginners think:

```text id="y17dpg"
Render
  =
DOM update
```

That's not correct.

React has a process roughly like:

```text id="h2jv9y"
Update happens
     ↓
Render
     ↓
React determines what the UI should be
     ↓
Reconciliation
     ↓
Commit
     ↓
DOM is updated if necessary
```

So:

> **Rendering is about determining the UI. Committing is when React applies the required changes to the DOM.**

This distinction comes up frequently in advanced interviews.

---

# 3. What is a Re-render?

### Definition

> **A re-render is when React renders a component again because something relevant to its rendering may have changed.**

For example:

```jsx id="sm7y9x"
function Counter() {
  const [count, setCount] = useState(0);

  return <h1>{count}</h1>;
}
```

Initially:

```text id="jpw9b7"
count = 0
```

UI:

```text id="u8mmc8"
0
```

Then:

```jsx id="z4f4lw"
setCount(1);
```

React schedules an update.

The component renders again.

Now:

```text id="e8n6j5"
count = 1
```

React determines that the displayed text needs to change.

The DOM is then updated.

---

# 4. What Can Cause a Re-render?

Several things can cause React to render a component again.

The most common ones are:

### 1. State update

```jsx id="0efr3t"
setCount(10);
```

### 2. Parent renders

If a parent component renders again, React may render its children as part of processing that subtree.

### 3. Context changes

If a component consumes a context value and that context value changes, the consumer can render again.

### 4. External subscriptions

Libraries and React APIs can also cause updates through subscriptions.

For beginner interviews, remember:

```text id="0sn4qi"
State changes
     ↓
Possible re-render

Parent renders
     ↓
Child may render

Context changes
     ↓
Consumer may render
```

---

# 5. Does a Parent Re-render Always Mean the Child DOM Changes?

No.

This is a very common interview trap.

Suppose:

```jsx id="0u0x8a"
function Parent() {
  return <Child />;
}
```

If `Parent` renders again, React may process `Child` again.

But that does **not** mean:

```text id="u7d5e9k"
destroy Child DOM
     ↓
create Child DOM again
```

React compares the resulting UI and applies only the necessary changes.

So:

> **Component rendering and DOM manipulation are two different things.**

---

# 6. Example: Parent and Child

Consider:

```jsx id="5h2q5d"
function Parent() {
  const [count, setCount] = useState(0);

  return (
    <>
      <button onClick={() => setCount(count + 1)}>
        Increment
      </button>

      <Child />
    </>
  );
}
```

And:

```jsx id="09x9j7"
function Child() {
  console.log("Child rendered");

  return <p>Hello</p>;
}
```

When the button is clicked:

```text id="q1t3qk"
Parent state changes
       ↓
Parent renders
       ↓
React processes Child
       ↓
Child may render again
```

But the DOM for:

```html id="1d5p5d"
<p>Hello</p>
```

doesn't need to be recreated just because the component rendered again.

---

# 7. Rendering vs DOM Updating

Let's make this very clear.

Suppose the previous UI is:

```text id="2cx4h7"
<h1>Hello</h1>
<p>Age: 25</p>
```

New UI:

```text id="o1f8sk"
<h1>Hello</h1>
<p>Age: 26</p>
```

React determines:

```text id="y04p6v"
<h1>Hello</h1>
→ unchanged

<p>Age: 25</p>
→ changed
```

So only the necessary part needs to be updated.

Conceptually:

```text id="5ob7qj"
Old UI
  ↓
New UI
  ↓
Compare
  ↓
Find differences
  ↓
Apply necessary DOM changes
```

This process is related to **reconciliation**.

We'll discuss it more deeply later.

---

# 8. What is the Virtual DOM?

This is another very common interview question.

### Traditional definition

> **The Virtual DOM is a lightweight JavaScript representation of the UI that React uses as part of its rendering and reconciliation process.**

A simple conceptual example:

Your JSX:

```jsx id="a9x9w6"
<h1>Hello</h1>
```

can be represented as JavaScript data describing:

```text id="d7n9py"
type: h1
children: Hello
```

React works with this representation during rendering.

When the UI changes, React creates a new representation and compares it with the previous one to determine what needs to change.

---

# 9. Is the Virtual DOM an Actual DOM?

No.

This is a common interview question.

> **The Virtual DOM is not the browser's actual DOM.**

The browser has a real DOM:

```text id="d0ymmg"
Browser
   ↓
Actual DOM
```

React uses JavaScript representations of UI during its rendering process.

Conceptually:

```text id="c0f3m4"
React elements / Fiber data
          ↓
    React processing
          ↓
      Actual DOM
```

Don't say:

> "React stores the entire DOM in memory."

That's an oversimplification.

Modern React's implementation involves **Fiber nodes and internal data structures**, not simply a giant duplicate HTML tree.

---

# 10. Why do people say Virtual DOM makes React fast?

Be careful with this interview answer.

A weak answer:

> "Virtual DOM makes React faster than the real DOM."

That's too simplistic.

The DOM isn't simply "slow" in every situation, and Virtual DOM isn't magic.

A better answer:

> **"React uses an in-memory representation of the UI as part of its rendering process. It can compare the previous and next UI representations and determine the necessary DOM updates rather than requiring the application to manually update every affected DOM node."**

That's much more accurate.

---

# 11. What is Reconciliation?

### Traditional definition

> **Reconciliation is React's process of comparing the previous rendered result with the new rendered result to determine what changes are needed.**

Example:

Previous:

```text id="f9a2t7"
<h1>Hello</h1>
```

New:

```text id="4o9k3b"
<h1>Hello Riyaz</h1>
```

React recognizes that the element is still an `h1`, but its text changed.

So it can update the relevant DOM content.

Conceptually:

```text id="d2bg6a"
Previous UI
    ↓
New UI
    ↓
Reconciliation
    ↓
Required changes
```

---

# 12. Reconciliation Is More Than "Comparing HTML"

This is important at advanced levels.

React isn't simply doing a naive:

```text
old HTML string
vs
new HTML string
```

comparison.

It works with React's element tree and internal Fiber architecture.

React considers things such as:

* element type
* keys
* position in the tree
* component identity
* props
* state
* tree structure

We'll get into these details when we cover **Keys and Reconciliation**.

---

# 13. What is Fiber?

This is an advanced React interview topic.

### Traditional definition

> **Fiber is React's internal architecture for representing work on the component tree and managing rendering work.**

You don't need to memorize its entire implementation.

The important idea is:

> **Fiber allows React to represent rendering work in smaller units and gives React more control over scheduling and prioritizing that work.**

Conceptually:

```text id="q2jz5y"
React Tree
   ↓
Fiber nodes
   ↓
Rendering work
   ↓
Commit
```

Fiber became the foundation for many modern React rendering capabilities.

---

# 14. Why was Fiber introduced?

Older React rendering work was more difficult to interrupt.

If React had a large amount of work to perform, it could potentially keep the JavaScript thread busy for too long.

Fiber introduced an architecture where React can represent rendering work in units.

This supports concepts such as:

* interruptible rendering
* prioritization
* scheduling
* concurrent features

So when an interviewer asks:

> "Why does React have Fiber?"

A good answer is:

> **"Fiber is the internal architecture React uses to represent and schedule rendering work. It allows React to break work into units and gives the renderer more control over when and how that work is processed."**

---

# 15. Render Phase vs Commit Phase

This is one of the **most important advanced React concepts**.

React's update process can be thought of in two major stages.

## Render Phase

React determines:

> "What should the UI look like?"

It calculates the next tree.

This phase should be free of side effects.

---

## Commit Phase

React takes the result of the render work and applies the required changes.

For the DOM renderer, this includes things such as:

* inserting DOM nodes
* updating DOM properties
* removing DOM nodes
* running relevant effects at the appropriate point

Simplified:

```text id="x9j4v1"
           Update
              ↓
        Render Phase
              ↓
      Determine next UI
              ↓
        Commit Phase
              ↓
       Update the DOM
```

---

# 16. Why Should Rendering Be Pure?

This is a very important React principle.

Consider:

```jsx id="xw8xij"
function User() {
  console.log("Rendering");

  return <h1>Hello</h1>;
}
```

Logging is generally harmless.

But imagine:

```jsx id="p8e8v6"
function User() {
  sendMoneyToBank();
  
  return <h1>Hello</h1>;
}
```

That's a terrible idea.

Why?

Because rendering can happen more than once.

React needs rendering to be predictable.

So:

> **Components should be pure during rendering: given the same props, state, and relevant context, they should produce the same UI result without causing side effects.**

Side effects belong in appropriate places, such as event handlers or Effects, depending on the operation.

We'll study this deeply with `useEffect`.

---

# 17. Why Can React Render a Component More Than Once?

This is an excellent interview question.

A component may render multiple times because:

* its state changes
* its parent renders
* context changes
* an external subscription causes an update
* development tools/Strict Mode can intentionally invoke rendering patterns to help detect certain problems

The important point is:

> **You should not assume a component function runs exactly once.**

That's why rendering logic should be pure.

---

# 18. Strict Mode and Double Rendering

This is a common interview topic.

In development, React Strict Mode may intentionally invoke certain functions more than once to help identify unsafe side effects and other problems.

For example:

```jsx id="f0j1ob"
<React.StrictMode>
  <App />
</React.StrictMode>
```

A developer might see:

```text id="1cv7vh"
Component rendered
Component rendered
```

and think:

> "React is broken!"

It's not necessarily a bug.

Strict Mode development behavior is designed to expose code that isn't following React's expected patterns.

### Interview answer

> "In development, Strict Mode can intentionally re-run certain rendering-related logic to help detect accidental side effects and unsafe patterns. This behavior is development-only and should not be used as a reason to put side effects inside rendering."

---

# 19. Does Re-render Mean Remount?

**No.**

This is a very important distinction.

### Re-render

The component renders again.

### Remount

React removes the existing component instance/tree and creates a new one.

For example:

```text id="p9g2x3"
Re-render:
Component
   ↓
renders again
```

Whereas:

```text id="2d5v8v"
Remount:
Old component
   ↓
removed
   ↓
new component
```

A remount can reset state.

A normal re-render does not automatically reset the component's state.

---

# 20. Re-render vs Remount

| Re-render                               | Remount                                                |
| --------------------------------------- | ------------------------------------------------------ |
| Component renders again                 | Component is removed and recreated                     |
| State usually preserved                 | State is reset for the new instance                    |
| Can happen frequently                   | Happens when component identity changes/removal occurs |
| Does not necessarily recreate DOM nodes | Existing DOM may be removed/replaced                   |

This distinction is very useful when debugging React applications.

---

# 21. How can a component be Remounted?

One important way is by changing its `key`.

For example:

```jsx id="4b0qg5"
<UserForm key={userId} />
```

If:

```text id="v0ph5v"
userId = 1
```

changes to:

```text id="a2c8m8"
userId = 2
```

React sees a different key and can treat it as a different component identity.

That can cause the previous component to be removed and a new one created.

We'll study keys properly later.

---

# 22. Batching

Another important concept.

Suppose:

```jsx id="e6h7h1"
function handleClick() {
  setCount(prev => prev + 1);
  setName("John");
  setOpen(true);
}
```

React can batch multiple state updates so that it doesn't necessarily perform a separate rendering/commit cycle for every single update.

Conceptually:

```text id="h4k7r8"
Update 1 ─┐
Update 2 ─┼──→ Batch → Render → Commit
Update 3 ─┘
```

This can reduce unnecessary rendering work.

Modern React has automatic batching in many situations, not only traditional browser event handlers.

---

# 23. Why Does Batching Matter?

Imagine:

```jsx id="u8n9on"
setCount(prev => prev + 1);
setName("John");
setIsOpen(true);
setLoading(false);
```

If React independently rendered and committed after every update, it could do unnecessary work.

Batching allows React to group compatible updates.

This is one reason you shouldn't assume:

> "Every call to a state setter immediately causes a complete render and DOM update."

---

# 24. Does React Always Batch Everything?

Be careful with absolute statements.

Modern React performs automatic batching in many common scenarios, but exact behavior can depend on the API, renderer, and context.

For interviews, a good answer is:

> **"Modern React batches many state updates together to reduce unnecessary rendering work. When multiple updates happen within the same batch, React can process them together rather than committing separately for every setter call."**

That's safer than saying:

> "React always batches everything."

---

# 25. A Complete Example

Consider:

```jsx id="z6ihdu"
function Counter() {
  const [count, setCount] = useState(0);

  console.log("render", count);

  function handleClick() {
    setCount(prev => prev + 1);
  }

  return (
    <>
      <h1>{count}</h1>

      <button onClick={handleClick}>
        Increment
      </button>
    </>
  );
}
```

Initial render:

```text id="k9v3d7"
render 0
```

User clicks:

```text id="d9m6at"
setCount(prev => prev + 1)
```

React schedules an update.

Then:

```text id="y6qj4f"
render 1
```

React compares the new UI with the previous result:

```text id="t9r5s8"
<h1>0</h1>

vs

<h1>1</h1>
```

It determines the text needs updating.

Then the commit phase updates the DOM.

---

# 26. The Most Important Rendering Mental Model

Remember this:

```text id="w3t4qk"
           State / Props
                ↓
             Render
                ↓
      What should UI look like?
                ↓
         Reconciliation
                ↓
          Commit phase
                ↓
          Actual DOM
```

And:

> **Rendering is not the same thing as DOM updating.**

This one sentence can save you from several interview mistakes.

---

# 27. Interview Question: What causes a React component to re-render?

A strong answer:

> "A component can render again when its state changes, when its parent renders and React processes that part of the tree, when consumed context changes, or when another subscribed data source causes an update. A re-render means React runs the rendering process again; it doesn't necessarily mean that the DOM will be changed."

That's a strong intermediate-level answer.

---

# 28. Interview Question: Does React update the entire DOM on every render?

### Answer:

> **No.**

React calculates the new UI representation and determines what actually needs to change.

For example:

```text id="b5y7z4"
Old:
<h1>Hello</h1>
<p>Age: 25</p>

New:
<h1>Hello</h1>
<p>Age: 26</p>
```

React doesn't need to recreate the whole page.

Only the necessary change needs to be committed.

---

# 29. Interview Question: What is reconciliation?

Strong answer:

> **"Reconciliation is the process React uses to determine how the current rendered result differs from the previous one and what updates are needed. React uses its internal representation of the component tree, along with things such as element types and keys, to determine how to update the UI efficiently."**

---

# 30. Interview Question: What is the Virtual DOM?

Good answer:

> **"The Virtual DOM is a simplified term for React's in-memory representation of UI elements. React uses this representation during rendering and reconciliation to determine what changes need to be applied to the actual DOM. It isn't the browser DOM itself, and it shouldn't be thought of as simply a complete copy of the DOM."**

That last sentence shows deeper understanding.

---

# 31. Interview Question: What is Fiber?

Good answer:

> **"Fiber is React's internal architecture for representing rendering work. It allows React to break rendering work into units and gives React more control over scheduling and prioritizing that work. Fiber is an important foundation for modern React's rendering capabilities."**

You don't need to explain Fiber's entire source code in an interview.

---

# 32. Beginner → Advanced Questions

### Beginner

1. What is rendering?
2. What is a re-render?
3. What causes a component to re-render?
4. Does state change cause a re-render?
5. Does rendering mean the DOM is updated?

### Intermediate

6. What is the Virtual DOM?
7. What is reconciliation?
8. Does React update the entire DOM?
9. What happens when a parent re-renders?
10. Does a child always re-render when the parent renders?
11. What is batching?
12. Why doesn't state update immediately?

### Advanced

13. What is the render phase?
14. What is the commit phase?
15. What is Fiber?
16. Why was Fiber introduced?
17. Why should render logic be pure?
18. What is the difference between re-render and remount?
19. How do keys affect component identity?
20. What happens internally after a state update?
21. How does React decide which DOM changes to commit?
22. How does concurrent rendering relate to Fiber?

---

# 33. One Important Correction to Common Interview Answers

You may hear:

> "React is fast because Virtual DOM is faster than the real DOM."

Don't repeat this blindly.

A better understanding is:

> **React provides a declarative programming model and manages DOM updates for you. Its rendering and reconciliation system helps determine the necessary updates rather than requiring developers to manually manipulate the DOM throughout the application.**

Performance depends on many things:

* amount of work
* component structure
* state placement
* unnecessary renders
* DOM complexity
* expensive calculations
* network operations
* browser work

There isn't one magic "Virtual DOM makes everything fast" explanation.

---

# What you should remember

If you remember only these six points:

### 1.

> **Rendering means React determines what the UI should look like.**

### 2.

> **A re-render does not automatically mean a DOM update.**

### 3.

> **Reconciliation determines what has changed.**

### 4.

> **The commit phase applies the necessary changes.**

### 5.

> **Fiber is React's internal architecture for managing rendering work.**

### 6.

> **Render logic should be pure because React may render more than once.**

The overall picture:

```text id="f4c4vk"
          State / Props change
                   ↓
             React update
                   ↓
              Render phase
                   ↓
        Determine next UI tree
                   ↓
             Reconciliation
                   ↓
              Commit phase
                   ↓
            Actual DOM update
```

---

## Next Concept: Virtual DOM & Reconciliation — Deep Dive

We've introduced these two concepts, but **don't move past them too quickly for interviews**.

Next we'll take **Virtual DOM + Reconciliation** as a dedicated concept and go deeper into:

* What a React element actually is
* Virtual DOM vs real DOM
* Reconciliation algorithm
* Element type comparison
* Why `<div>` → `<span>` behaves differently
* How React preserves component state
* How `key` affects reconciliation
* Why array indexes as keys can cause bugs
* Reconciliation with lists
* Fiber's role
* Common interview traps and advanced questions

---
---
---
---
