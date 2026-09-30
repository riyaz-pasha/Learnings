# Topic 2 — JSX in Depth

JSX looks simple:

```jsx
const element = <h1>Hello</h1>;
```

But for interviews, you should be able to explain what this actually becomes, what React receives, and why JSX has the rules it does.

The key mental model is:

```text
JSX
 ↓
JS transformation
 ↓
React element creation
 ↓
React rendering/reconciliation
 ↓
DOM
```

---

# 1. What is JSX?

**JSX = JavaScript XML-like syntax.**

It is a syntax extension that lets us write UI descriptions using XML/HTML-like syntax inside JavaScript.

Example:

```jsx
const element = <h1>Hello World</h1>;
```

JSX itself is **not HTML**.

It is also **not understood directly by the JavaScript engine**.

The browser's JavaScript engine understands JavaScript, not JSX syntax.

So JSX must first be transformed.

---

# 2. JSX → JavaScript

Consider:

```jsx
const element = <h1>Hello World</h1>;
```

Conceptually, this becomes something like:

```javascript
const element = React.createElement(
    "h1",
    null,
    "Hello World"
);
```

With modern JSX transformation, it is instead conceptually closer to:

```javascript
const element = jsx(
    "h1",
    {
        children: "Hello World"
    }
);
```

The important thing is not the exact generated helper name yet.

The important transformation is:

```text
<h1>Hello World</h1>
```

becomes a JavaScript expression that creates a **React element**.

---

# 3. What is a React element?

This is extremely important.

Take:

```jsx
const element = <h1>Hello</h1>;
```

`element` is **not**:

```text
HTMLElement
```

It is not the real DOM node.

Instead, it is a JavaScript value describing what React should eventually render.

Conceptually:

```javascript
{
    type: "h1",
    props: {
        children: "Hello"
    }
}
```

The actual internal shape contains additional fields and metadata, but this simplified version is enough initially.

So:

```text
JSX
 ↓
React element
 ↓
React interprets it
 ↓
DOM element eventually created/updated
```

---

# 4. React Element vs DOM Element

This distinction is frequently asked.

### React element

```jsx
const element = <button>Click</button>;
```

Think:

```text
JavaScript object
```

### DOM element

Eventually React may create:

```javascript
document.createElement("button");
```

Think:

```text
browser-managed object
```

Therefore:

```text
React element ≠ DOM element
```

A React element is a **description**.

A DOM element is an **actual browser node**.

---

# 5. Why does React use descriptions?

Suppose we have:

```jsx
<div>
    <h1>Hello</h1>
    <button>Click</button>
</div>
```

A JavaScript representation lets React reason about the UI before touching the DOM.

Conceptually:

```text
React elements
      ↓
React can compare / reconcile
      ↓
figure out changes
      ↓
perform DOM operations
```

This separation is fundamental to React's architecture.

---

# 6. Let's break down JSX syntax

Consider:

```jsx
const element = (
    <button
        className="primary"
        disabled={false}
        onClick={handleClick}
    >
        Click me
    </button>
);
```

JSX contains:

```text
element type
properties
JavaScript expressions
children
```

Conceptually:

```text
<button
   │
   ├── className
   ├── disabled
   └── onClick
       │
       ▼
     children
```

---

# 7. JSX attributes become props

This:

```jsx
<button className="primary">
    Click
</button>
```

conceptually becomes something like:

```javascript
jsx("button", {
    className: "primary",
    children: "Click"
});
```

So:

```text
JSX attribute
      ↓
React element props
```

For example:

```jsx
<User name="John" age={25} />
```

becomes conceptually:

```javascript
jsx(User, {
    name: "John",
    age: 25
});
```

This is why we call them **props** when we're talking about React components.

---

# 8. Strings vs JavaScript expressions

Consider:

```jsx
<h1>Hello</h1>
```

`Hello` is JSX text.

But:

```jsx
<h1>{name}</h1>
```

`name` is a JavaScript expression.

The `{}` syntax means:

> "Evaluate JavaScript here."

For example:

```jsx
const name = "Alice";

return <h1>Hello {name}</h1>;
```

Conceptually:

```javascript
jsx("h1", {
    children: [
        "Hello ",
        name
    ]
});
```

---

# 9. Why are `{}` needed?

Because JSX isn't normal JavaScript syntax everywhere.

For example:

```jsx
<h1>name</h1>
```

renders the literal text:

```text
name
```

while:

```jsx
<h1>{name}</h1>
```

evaluates the variable.

So:

```text
"name"
```

and:

```text
{name}
```

are different.

---

# 10. You can put JavaScript expressions inside JSX

For example:

```jsx
<h1>{2 + 3}</h1>
```

Result:

```text
5
```

Or:

```jsx
<h1>{user.name.toUpperCase()}</h1>
```

Or:

```jsx
<h1>{isLoggedIn ? "Dashboard" : "Login"}</h1>
```

Or:

```jsx
<ul>
    {items.map(item => (
        <li key={item.id}>{item.name}</li>
    ))}
</ul>
```

Important:

JSX braces accept **expressions**, not arbitrary statements.

---

# 11. Expression vs statement

This distinction matters.

This works:

```jsx
<div>{count + 1}</div>
```

because:

```javascript
count + 1
```

is an expression.

This works:

```jsx
<div>{condition ? "A" : "B"}</div>
```

because a ternary expression produces a value.

But this doesn't work:

```jsx
<div>
    {
        if (condition) {
            return "A";
        }
    }
</div>
```

because `if` is a statement.

Instead:

```jsx
<div>
    {condition ? "A" : "B"}
</div>
```

Or:

```jsx
if (condition) {
    return <div>A</div>;
}

return <div>B</div>;
```

We'll study conditional rendering later, but this is an important JSX rule.

---

# 12. JSX children

Consider:

```jsx
<div>
    Hello
    <strong>World</strong>
</div>
```

The `div` has children:

```text
"Hello"
<strong>World</strong>
```

Conceptually:

```javascript
jsx("div", {
    children: [
        "Hello",
        jsx("strong", {
            children: "World"
        })
    ]
});
```

So JSX nesting naturally forms a tree.

---

# 13. Nested JSX = tree structure

Example:

```jsx
<div>
    <header>
        <h1>Dashboard</h1>
    </header>

    <main>
        <button>Save</button>
    </main>
</div>
```

Conceptually:

```text
div
├── header
│   └── h1
└── main
    └── button
```

This tree is one of the foundational data structures React works with.

---

# 14. Why does JSX require one parent?

Consider:

```jsx
return (
    <h1>Hello</h1>
    <p>World</p>
);
```

This isn't valid JSX.

Why?

A function must return one JavaScript expression.

You can't have two adjacent JSX elements with no surrounding expression/container.

So we can do:

```jsx
return (
    <div>
        <h1>Hello</h1>
        <p>World</p>
    </div>
);
```

But that adds an actual DOM node.

React gives us another solution.

---

# 15. Fragments

```jsx
return (
    <>
        <h1>Hello</h1>
        <p>World</p>
    </>
);
```

`<>...</>` is shorthand for:

```jsx
<React.Fragment>
    ...
</React.Fragment>
```

The Fragment lets React group children without introducing an extra DOM element.

Conceptually:

```text
Fragment
├── h1
└── p
```

while the DOM can remain:

```html
<h1>Hello</h1>
<p>World</p>
```

---

# 16. Why not always use `<div>`?

Because adding an unnecessary wrapper can affect:

* CSS
* layout
* accessibility
* semantics
* flex/grid structure

For example:

```jsx
<ul>
    <List />
</ul>
```

If `List` introduces:

```html
<div>
    <li>...</li>
</div>
```

you can create invalid HTML structure.

Fragments avoid unnecessary DOM elements.

---

# 17. Component names are significant

This is a classic interview question.

Consider:

```jsx
<MyComponent />
```

versus:

```jsx
<myComponent />
```

These aren't treated the same way.

React convention and JSX transformation distinguish user-defined components from host elements using capitalization.

Conceptually:

```jsx
<MyComponent />
```

means:

```javascript
jsx(MyComponent, {})
```

while:

```jsx
<div />
```

means:

```javascript
jsx("div", {})
```

Notice the difference:

```text
"MyComponent"
     vs
MyComponent
```

The first is a string.

The second is a JavaScript variable referring to the component.

That's why component names start with uppercase letters.

---

# 18. This is actually clever

Consider:

```jsx
function Button() {
    return <button>Click</button>;
}
```

When we write:

```jsx
<Button />
```

the JSX transformation must know:

> Is this a browser element or a React component?

Conceptually:

```text
<button>
   ↓
host element
   ↓
string "button"

<Button>
   ↓
custom component
   ↓
JavaScript reference Button
```

So capitalization gives JSX an easy syntactic distinction.

---

# 19. `className` instead of `class`

Another classic question.

In JSX:

```jsx
<div className="container">
```

instead of:

```jsx
<div class="container">
```

Why?

Because JSX attributes map into JavaScript object properties, and `class` has historically had special meaning in JavaScript syntax.

React's DOM API also uses the property name `className`.

Conceptually:

```jsx
<div className="foo" />
```

becomes:

```javascript
jsx("div", {
    className: "foo"
});
```

---

# 20. Event handlers are props

This:

```jsx
<button onClick={handleClick}>
    Click
</button>
```

doesn't mean:

```html
<button onclick="...">
```

Instead, conceptually React receives:

```javascript
{
    onClick: handleClick
}
```

React later uses that information while setting up event handling.

This is important because React's event system is not simply equivalent to writing:

```javascript
element.onclick = ...
```

on every DOM node.

We'll study React events and delegation separately.

---

# 21. Why `onClick={handleClick}` and `onClick={handleClick()}` are different

Very common interview question.

Correct:

```jsx
<button onClick={handleClick}>
```

This passes the function.

Conceptually:

```text
onClick → function reference
```

But:

```jsx
<button onClick={handleClick()}>
```

calls the function **during rendering**.

So:

```text
handleClick
     ↓
reference

handleClick()
     ↓
execute now
```

Correct:

```jsx
<button onClick={handleClick}>
```

or:

```jsx
<button onClick={() => handleClick()}>
```

The latter creates a new function during render, which has its own performance implications that we'll discuss later.

---

# 22. JSX allows arbitrary nesting

For example:

```jsx
<Card>
    <Avatar />
    <Profile />
    <Actions />
</Card>
```

Everything between:

```jsx
<Card>
...
</Card>
```

becomes the `children` prop.

Conceptually:

```javascript
jsx(Card, {
    children: [
        jsx(Avatar, {}),
        jsx(Profile, {}),
        jsx(Actions, {})
    ]
});
```

This is the foundation of **composition**.

---

# 23. `children` is just a prop

Suppose:

```jsx
function Card(props) {
    return (
        <section>
            {props.children}
        </section>
    );
}
```

And:

```jsx
<Card>
    <h1>Hello</h1>
</Card>
```

Conceptually:

```javascript
Card({
    children: jsx("h1", {
        children: "Hello"
    })
});
```

So `children` isn't magical state.

It's part of the props.

---

# 24. JSX spreads

Example:

```jsx
const props = {
    name: "Alice",
    age: 25
};

<User {...props} />
```

Conceptually similar to:

```jsx
<User
    name={props.name}
    age={props.age}
/>
```

The spread syntax is JavaScript object-spread-like behavior applied to JSX props.

It is convenient, but indiscriminate spreading can make components harder to understand because it becomes less obvious which props are being passed.

---

# 25. Explicit props vs spread props

Compare:

```jsx
<User
    name={user.name}
    email={user.email}
/>
```

with:

```jsx
<User {...user} />
```

The second can accidentally pass:

```text
id
password
internalFlags
metadata
...
```

to the component.

So in production code, explicit props are often easier to reason about.

This is more of a design question than a JSX syntax question.

---

# 26. JSX and `React.createElement`

Historically, developers often learned JSX as:

```jsx
<div>Hello</div>
```

→

```javascript
React.createElement(
    "div",
    null,
    "Hello"
);
```

This is useful for understanding the concept.

But modern React uses the **automatic JSX transform**.

So modern compiled output uses JSX runtime helpers rather than requiring:

```javascript
import React from "react";
```

solely because JSX exists.

This is why modern React code can often be:

```jsx
function App() {
    return <h1>Hello</h1>;
}
```

without:

```javascript
import React from "react";
```

at the top.

---

# 27. JSX runtime

Modern JSX transformation uses functions from React's JSX runtime, conceptually:

```javascript
jsx(...)
jsxs(...)
Fragment
```

For example, a single-child JSX expression may conceptually compile toward `jsx`, while JSX containing multiple children may use `jsxs`.

The exact generated code is a compiler implementation detail and shouldn't be treated as your application's public API.

The interview-level takeaway is:

```text
JSX compiler transform
        ↓
JS function call
        ↓
React element
```

---

# 28. React elements are immutable descriptions

Suppose:

```jsx
const element = <h1>Hello</h1>;
```

You should not think of this as an object that React expects you to mutate:

```javascript
element.props.children = "Bye";
```

React elements represent descriptions of UI for a particular render.

Instead, a new render produces new element descriptions.

Conceptually:

```text
Render #1
<h1>Hello</h1>

Render #2
<h1>Bye</h1>
```

React compares the resulting structure and determines what needs to change.

---

# 29. JSX does not create DOM nodes

This is worth repeating because interviewers love it.

When you write:

```jsx
const element = <div />;
```

you have **not** done:

```javascript
document.createElement("div");
```

The JSX expression creates a React element description.

Later, during rendering/commit to the DOM host environment, React may create or update an actual DOM node.

So:

```text
JSX
    ↓
React element
    ↓
Fiber/reconciliation
    ↓
commit
    ↓
DOM node
```

---

# 30. Why can JSX contain components?

Because JSX doesn't only describe HTML-like host elements.

It can describe:

```jsx
<App />
<User />
<Button />
```

Here the `type` isn't a string.

Conceptually:

```javascript
jsx(App, {})
```

rather than:

```javascript
jsx("App", {})
```

This is a very important distinction.

---

# 31. Host elements vs function components

Think about these:

```jsx
<div />
<Button />
```

Conceptually:

```text
<div />
   ↓
type = "div"
   ↓
host element

<Button />
   ↓
type = Button
   ↓
React component
```

That distinction eventually affects:

* Fiber tags/types
* reconciliation
* component execution
* DOM creation

We'll see this when we study Fiber.

---

# 32. JSX expressions can be stored

Because JSX produces JavaScript values:

```jsx
const title = <h1>Hello</h1>;
```

You can conditionally use them:

```jsx
const content = isLoggedIn
    ? <Dashboard />
    : <Login />;
```

Or:

```jsx
const items = data.map(item => (
    <Item key={item.id} item={item} />
));
```

This works because JSX is fundamentally an expression after transformation.

---

# 33. JSX and JavaScript evaluation

Consider:

```jsx
const count = 10;

const element = (
    <div>
        {count * 2}
    </div>
);
```

JavaScript evaluates:

```javascript
count * 2
```

first.

Conceptually:

```javascript
const value = count * 2;

const element = jsx("div", {
    children: value
});
```

So JSX doesn't introduce a new programming language runtime.

It's syntax integrated into JavaScript.

---

# 34. What does JSX do with `null`, `false`, and `undefined`?

This is a practical interview favorite.

Consider:

```jsx
<div>
    {false}
</div>
```

React doesn't render the text:

```text
false
```

Similarly:

```jsx
<div>{null}</div>
<div>{undefined}</div>
```

don't produce visible text for those values.

That's why this common pattern works:

```jsx
{isLoggedIn && <Dashboard />}
```

When:

```javascript
isLoggedIn === true
```

the expression produces:

```jsx
<Dashboard />
```

When false:

```text
false
```

which React treats as no rendered node.

Be careful with:

```jsx
{count && <Component />}
```

because:

```javascript
count = 0
```

produces `0`, which can actually be rendered.

Safer:

```jsx
{count > 0 && <Component />}
```

---

# 35. JSX comments

Inside JSX:

```jsx
<div>
    {/* This is a comment */}
</div>
```

Not:

```jsx
<div>
    // comment
</div>
```

because the content between JSX tags is not parsed as ordinary JavaScript statement syntax.

---

# 36. JSX and security

React escapes text content by default.

For example:

```jsx
const userInput = "<script>...</script>";

return <div>{userInput}</div>;
```

React treats it as text rather than executing it as HTML.

This is one of React's basic defenses against XSS.

But:

```jsx
dangerouslySetInnerHTML
```

explicitly bypasses normal escaping and therefore requires careful handling.

We'll cover React security in detail later.

---

# 37. A complete transformation example

Let's take a moderately complex component:

```jsx
function App() {
    const name = "Alice";
    const loggedIn = true;

    return (
        <div className="app">
            <h1>Hello {name}</h1>

            {loggedIn && (
                <button onClick={() => console.log("Clicked")}>
                    Logout
                </button>
            )}
        </div>
    );
}
```

Conceptually, JSX gets transformed into JavaScript resembling:

```javascript
function App() {
    const name = "Alice";
    const loggedIn = true;

    return jsx("div", {
        className: "app",
        children: [
            jsx("h1", {
                children: ["Hello ", name]
            }),

            loggedIn &&
                jsx("button", {
                    onClick: () => console.log("Clicked"),
                    children: "Logout"
                })
        ]
    });
}
```

This is simplified, but it gives you the correct architectural picture.

---

# 38. What happens after this?

The result isn't sent directly to the browser.

We have:

```text
JSX
 ↓
React element tree
```

React then uses that information during rendering.

Conceptually:

```text
App()
 ↓
React element tree
 ↓
Fiber work
 ↓
reconciliation
 ↓
commit
 ↓
DOM
```

That's why JSX is only the **front door** into React's rendering model.

---

# 39. Important JSX interview traps

### Trap 1

> "JSX is HTML."

❌ Incorrect.

JSX is syntax that gets transformed into JavaScript.

---

### Trap 2

> "JSX directly creates DOM nodes."

❌ Incorrect.

It creates React element descriptions through transformed JavaScript.

---

### Trap 3

> "Virtual DOM and JSX are the same thing."

❌ Incorrect.

```text
JSX = syntax
React element = resulting description
Fiber = internal work/tree structure
DOM = actual browser structure
```

---

### Trap 4

> "React components must return HTML."

❌ Incorrect.

They return React-compatible values/elements, such as:

```jsx
<div />
```

or:

```jsx
<>
    <A />
    <B />
</>
```

or potentially:

```javascript
null
```

---

### Trap 5

> "Lowercase and uppercase component names are stylistic."

❌ No.

They affect how JSX is interpreted:

```jsx
<div />
```

means host element.

```jsx
<Button />
```

means component reference.

---

# 40. Interview-ready answer: What is JSX?

A strong answer:

> JSX is a JavaScript syntax extension that lets us describe UI using XML-like syntax. JSX itself isn't HTML and isn't directly executed by the JavaScript engine. A compiler transforms it into JavaScript calls that create React elements. React then uses those element descriptions during rendering and reconciliation to determine what should be committed to the host environment such as the browser DOM.

That's a much stronger answer than:

> "JSX lets us write HTML in JavaScript."

---

# 41. Interview-ready answer: What is a React element?

> A React element is a JavaScript representation describing what should appear in the UI. It is not an actual DOM node. JSX is transformed into JavaScript that creates these element descriptions, which React later processes during rendering and reconciliation.

---

# 42. Interview-ready answer: Why are React components capitalized?

> JSX uses capitalization to distinguish user-defined components from host elements. `<div>` is treated as a host element type, while `<Button>` is treated as a JavaScript reference to a React component.

---

# 43. The architecture you should now visualize

After Topic 1 + Topic 2:

```text
                 JSX
                  │
                  ▼
          JSX transformation
                  │
                  ▼
          React element tree
                  │
                  ▼
             React render
                  │
                  ▼
              Fiber tree
                  │
                  ▼
          Reconciliation
                  │
                  ▼
             Commit phase
                  │
                  ▼
               DOM
```

And remember:

```text
JSX
≠
React element
≠
Fiber
≠
DOM
```

They are related, but they are **not the same thing**.

---

# 44. What you should be able to explain now

Given:

```jsx
const element = (
    <Button
        disabled={isLoading}
        onClick={handleClick}
    >
        Save
    </Button>
);
```

You should be able to say:

```text
1. JSX is syntax, not HTML.
2. The JSX compiler transforms it into JavaScript.
3. Button is treated as a component because it starts uppercase.
4. disabled and onClick become props.
5. "Save" becomes children.
6. The result is a React element description.
7. React later processes that element during rendering.
8. Fiber/reconciliation determines the required work.
9. The commit phase applies changes to the host environment.
```

That's the foundation we'll build on.

---

# Next Topic

**Topic 3 — React Components in Depth**

We'll cover function components vs class components, component identity, props, `children`, composition, purity, component execution, and one particularly important question:

> **When React "renders a component", what exactly does React execute, and what does it actually store internally?**

