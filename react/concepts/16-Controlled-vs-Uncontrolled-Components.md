# Topic 16 — Controlled vs Uncontrolled Components & Forms

Forms look simple in React:

```jsx
<input />
```

But internally, forms are a very interesting area because they sit exactly at the boundary between:

```text
Browser DOM
     ↕
React props/state
     ↕
Fiber
     ↕
Event system
     ↕
Rendering / commit
```

For interviews, you should understand much more than:

> "Controlled means state, uncontrolled means ref."

We’ll build the concept from the browser upward and then look at what React actually does.

---

# 1. First understand the browser

Consider plain HTML:

```html
<input />
```

The browser owns the input's current value.

When the user types:

```text
User types "A"
     ↓
Browser updates input.value
```

The DOM itself contains the current value:

```js
input.value
```

So in a plain HTML application:

```text
Browser
   ↓
DOM input
   ↓
current value
```

React introduces another possible source of truth:

```text
React state
```

That's where controlled and uncontrolled inputs come from.

---

# 2. Uncontrolled input

The simplest React input:

```jsx
function Form() {
  return <input />;
}
```

The browser owns the current value.

You can also specify an initial value:

```jsx
<input defaultValue="John" />
```

The important word is **default**.

```text
defaultValue
    ↓
initial value
```

After the input is mounted:

```text
Browser owns current value
```

React isn't saying:

> "The input must always equal this value."

React's documentation explicitly describes `defaultValue` as specifying the **initial** value for an uncontrolled input. ([React][1])

---

# 3. Controlled input

Now:

```jsx
function Form() {
  const [name, setName] = useState("");

  return (
    <input
      value={name}
      onChange={e => setName(e.target.value)}
    />
  );
}
```

Now React state is the source of truth:

```text
React state
    ↓
 value prop
    ↓
 DOM input
```

Instead of:

```text
Browser
  ↓
owns current value
```

we have:

```text
React
  ↓
owns current value
```

React's current documentation defines an input with a `value` prop as controlled and requires an `onChange` handler that synchronously updates the backing value. ([React][1])

---

# 4. The core difference

### Uncontrolled

```jsx
<input defaultValue="John" />
```

Conceptually:

```text
Initial React render
       ↓
DOM gets "John"

User types
       ↓
DOM changes itself
```

### Controlled

```jsx
<input
  value={name}
  onChange={handleChange}
/>
```

Conceptually:

```text
User types
    ↓
DOM changes
    ↓
React event
    ↓
setName(...)
    ↓
React render
    ↓
value prop
    ↓
React keeps DOM synchronized
```

The second model has React continuously participating in the value flow.

---

# 5. Controlled input is a feedback loop

This is probably the most important diagram in the entire topic.

Suppose:

```jsx
const [name, setName] = useState("");
```

and:

```jsx
<input
  value={name}
  onChange={e => setName(e.target.value)}
/>
```

Then:

```text
              ┌─────────────────────┐
              │     React State     │
              │       name          │
              └──────────┬──────────┘
                         │
                         │ value prop
                         ↓
                 ┌───────────────┐
                 │   DOM input   │
                 └───────┬───────┘
                         │
                    user types
                         │
                         ↓
                 native browser event
                         │
                         ↓
                   React onChange
                         │
                         ↓
                    setName(...)
                         │
                         ↓
                    React update
                         │
                         └──────────→ new render
```

So:

```text
state → DOM
DOM → event → state
```

That's a feedback loop.

---

# 6. Why doesn't React let the DOM be the source of truth?

Sometimes it should.

But controlled components are useful when the application needs to know the value **immediately as part of React state**.

For example:

```jsx
<input
  value={email}
  onChange={e => setEmail(e.target.value)}
/>
```

Now the application can derive:

```jsx
const isValid = email.includes("@");
```

or:

```jsx
<button disabled={!isValid}>
  Submit
</button>
```

or:

```jsx
<p>You typed {email.length} characters</p>
```

The input isn't isolated from React anymore.

Its value is part of the React render state.

---

# 7. Uncontrolled inputs are not "bad"

This is important.

Don't say:

> "Controlled components are better."

That's an evaluative claim that isn't generally correct.

They solve different problems.

### Controlled

Useful when:

```text
React needs to react to every value change
```

### Uncontrolled

Useful when:

```text
The DOM can own the input value
and React only needs it at specific times
```

For example, on submit:

```jsx
function handleSubmit(e) {
  e.preventDefault();

  const formData = new FormData(e.currentTarget);

  console.log(formData.get("name"));
}
```

React's current `<form>` documentation explicitly demonstrates this approach with uncontrolled inputs and `FormData`. ([React][2])

---

# 8. `defaultValue` vs `value`

This is one of the most frequently asked questions.

Consider:

```jsx
<input defaultValue="John" />
```

Changing:

```jsx
<input defaultValue="Peter" />
```

during later renders does not mean:

```text
DOM current value → Peter
```

because `defaultValue` describes initial/default state for an uncontrolled input.

Now:

```jsx
<input value={name} />
```

means:

```text
DOM current value
        should match
React value
```

That's why the distinction is:

```text
value
   → current controlled value

defaultValue
   → initial uncontrolled value
```

React documents these semantics explicitly. ([React][1])

---

# 9. Why `value` without `onChange` is a problem

Consider:

```jsx
<input value="John" />
```

The input is controlled.

React is effectively saying:

```text
input.value must be "John"
```

But there's no way for your application to update `"John"`.

So if the user tries to type:

```text
John
 ↓
Johna
```

React can restore the value back to the controlled value.

React therefore warns that a mutable controlled input needs an `onChange` handler, unless you intentionally make it read-only. ([React][1])

You can intentionally do:

```jsx
<input
  value="John"
  readOnly
/>
```

which is a valid controlled read-only field. ([React][1])

---

# 10. What actually happens when the user types?

This is where our previous event-system lesson comes in.

Take:

```jsx
function NameInput() {
  const [name, setName] = useState("");

  return (
    <input
      value={name}
      onChange={e => setName(e.target.value)}
    />
  );
}
```

User types:

```text
A
```

The browser first changes its internal DOM value.

Then a native input-related event occurs.

React's event system receives that event and dispatches the React `onChange` handler.

Then:

```js
setName(e.target.value);
```

queues a state update.

Then:

```text
state update
   ↓
Fiber scheduling
   ↓
render
```

React calculates:

```jsx
<input value="A" />
```

Then during commit, React keeps the host input synchronized with the controlled value.

So the complete path is:

```text
User types
    ↓
Browser updates DOM input
    ↓
native input event
    ↓
React event system
    ↓
onChange
    ↓
setName("A")
    ↓
Hook update queue
    ↓
Fiber scheduled
    ↓
render
    ↓
value = "A"
    ↓
commit
    ↓
DOM synchronized
```

This is an excellent example of how the event, Hook, Fiber, reconciliation, and commit topics connect.

---

# 11. Why is React's `onChange` famous?

Because:

```jsx
<input onChange={...} />
```

doesn't map conceptually to the old browser behavior people often associate with the native `change` event.

For text inputs, React's `onChange` is designed to fire as the value changes, essentially giving you the behavior developers usually want for controlled text fields. React's docs describe it as firing immediately when the user changes the input's value. ([React][1])

For `<select>`, React similarly documents `onChange` as firing immediately when the selected option changes and notes that it behaves like the browser `input` event. ([React][3])

So don't think:

```text
React onChange = native change event
```

as an exact implementation statement.

---

# 12. React has special input event handling

This is where the internals become interesting.

React DOM has a dedicated event plugin:

```text
ChangeEventPlugin.js
```

The current React source still contains this file as part of the DOM event system. ([GitHub][4])

Its job is related to determining how React's change-like events should be extracted from native events for different input/control types.

Historically this became especially important because browsers did not behave uniformly for all controls and event types.

Modern browsers are much more consistent than older ones, but React still has specialized form-event handling.

---

# 13. Why was this necessary historically?

Imagine older browsers:

```text
Text input
Select
Checkbox
Radio
File input
```

could expose different native event behavior.

React wanted one developer-facing API:

```jsx
onChange
```

with predictable semantics.

So React's DOM event system could observe multiple native events and produce one React-level change event.

The old source makes this especially obvious: `ChangeEventPlugin` depended on several native events for certain input scenarios. ([Gist][5])

Modern source architecture has evolved, but the important conceptual lesson remains:

> **React's form events are an abstraction over native DOM events, not just a direct one-to-one alias.**

---

# 14. Controlled input internals

Now let's look at:

```jsx
<input value={name} onChange={...} />
```

During rendering, the JSX becomes a React element:

```text
type  = "input"
props =
  {
    value: name,
    onChange: handler
  }
```

Reconciliation creates/updates the corresponding Fiber.

Eventually React DOM handles the host component.

Conceptually:

```text
React element
    ↓
HostComponent Fiber
    ↓
ReactDOMInput
    ↓
DOM <input>
```

React has specialized input handling code in:

```text
packages/react-dom-bindings/src/client/ReactDOMInput.js
```

The current file contains the logic for initializing/updating controlled and uncontrolled input state, including `value`, `defaultValue`, `checked`, and `defaultChecked`. ([GitHub][6])

---

# 15. React doesn't just set `value` once

A controlled input must stay synchronized.

Suppose:

```text
React state = "John"
DOM input   = "John"
```

Then:

```text
state → "Peter"
```

React renders:

```jsx
<input value="Peter" />
```

React DOM must update the underlying input.

Conceptually:

```text
new props
   ↓
ReactDOMInput
   ↓
update input value
   ↓
DOM.value = "Peter"
```

The exact implementation is much more nuanced than a literal assignment in every browser/input type, but `ReactDOMInput.js` is responsible for this host-level synchronization. ([GitHub][6])

---

# 16. Why controlled input needs special DOM handling

An `<input>` isn't a normal DOM element.

It has state that lives inside the browser:

```text
value
checked
selection/caret
defaultValue
defaultChecked
```

and different input types behave differently:

```text
text
checkbox
radio
number
password
file
range
...
```

React therefore has specialized logic for inputs rather than treating:

```jsx
<input />
```

as just another generic host element.

---

# 17. Text input

Controlled:

```jsx
<input
  value={name}
  onChange={e => setName(e.target.value)}
/>
```

The controlled property is:

```text
value
```

Uncontrolled:

```jsx
<input defaultValue="John" />
```

The default property is:

```text
defaultValue
```

---

# 18. Checkbox

This is a very common interview trap.

For:

```jsx
<input type="checkbox" />
```

the current selected state is:

```js
e.target.checked
```

not:

```js
e.target.value
```

So controlled checkbox:

```jsx
<input
  type="checkbox"
  checked={enabled}
  onChange={e => setEnabled(e.target.checked)}
/>
```

React's current docs explicitly state that checkboxes need `checked`/`defaultChecked`, and that you should read `e.target.checked` rather than `e.target.value`. ([React][1])

---

# 19. Radio buttons

Same idea:

```jsx
<input
  type="radio"
  checked={selected}
  onChange={...}
/>
```

The controlled property is:

```text
checked
```

not:

```text
value
```

The `value` still matters when submitting form data, but it isn't the property that controls whether the radio is selected. ([React][1])

---

# 20. Select

Controlled:

```jsx
<select
  value={country}
  onChange={e => setCountry(e.target.value)}
>
  <option value="in">India</option>
  <option value="us">USA</option>
</select>
```

Uncontrolled:

```jsx
<select defaultValue="in">
```

React's current docs say `value` makes the select controlled and `defaultValue` is used for an uncontrolled select. They also note that React doesn't use an individual `<option selected>` prop for this purpose. ([React][3])

---

# 21. Textarea

Controlled:

```jsx
<textarea
  value={message}
  onChange={e => setMessage(e.target.value)}
/>
```

Uncontrolled:

```jsx
<textarea defaultValue="Hello" />
```

Again:

```text
value        → controlled
defaultValue → uncontrolled initial value
```

React's current textarea documentation uses exactly this distinction. ([React][7])

---

# 22. File inputs are special

Consider:

```jsx
<input type="file" />
```

You don't control its selected file through a normal:

```jsx
value="..."
```

flow.

The browser controls the selected files for security reasons.

Typically you access files through:

```jsx
function handleChange(e) {
  const file = e.target.files?.[0];
}
```

or via a ref.

So:

```text
file input
   ↓
browser-owned state
   ↓
usually treated as uncontrolled
```

This is a special case worth remembering.

---

# 23. One of the most important rules: don't switch modes

This is invalid:

```jsx
function Input({ data }) {
  return (
    <input value={data.name} />
  );
}
```

when initially:

```text
data.name = undefined
```

and later:

```text
data.name = "John"
```

You've effectively gone from:

```text
uncontrolled
    ↓
controlled
```

React warns about this.

React's current docs explicitly require an input to remain controlled or uncontrolled for its entire lifetime. ([React][1])

---

# 24. The classic API-data bug

Suppose:

```jsx
const [user, setUser] = useState(null);
```

Then:

```jsx
<input value={user?.name} />
```

Initial:

```text
value = undefined
```

Later:

```text
value = "John"
```

That changes the mode.

A common pattern is:

```jsx
<input
  value={user?.name ?? ""}
  onChange={...}
/>
```

Now:

```text
initial → ""
later   → "John"
```

Both are controlled values.

React's current documentation specifically recommends ensuring a controlled text input always receives a string rather than `null`/`undefined`. ([React][1])

---

# 25. Why uncontrolled → controlled is dangerous

React needs to know who owns the current value.

You don't want:

```text
Render 1

DOM owns value
```

then:

```text
Render 2

React owns value
```

because now React has to reconcile two different sources of truth.

So React makes this a stable invariant:

```text
input lifetime
    ↓
controlled
OR
uncontrolled
```

not:

```text
uncontrolled → controlled
```

---

# 26. Controlled input and reconciliation

Suppose:

```jsx
<input value={name} />
```

and state changes:

```text
John → Peter
```

React does **not** recreate the whole DOM node.

Instead:

```text
same Fiber
same host component
same DOM node
new props
```

Then during host update:

```text
old props
   vs
new props
   ↓
input-specific update
   ↓
DOM value synchronization
```

That is reconciliation + host update, not remounting.

---

# 27. Why an input sometimes loses its text

This is another common interview/debugging question.

Suppose:

```jsx
<textarea value={text} onChange={...} />
```

but every keystroke causes it to reset.

One possible reason isn't the input itself.

It may be that you're causing the DOM node to be **remounted**.

For example:

```jsx
<input key={Math.random()} ... />
```

Every render:

```text
new key
   ↓
new identity
   ↓
old input removed
   ↓
new input mounted
```

Then browser state such as selection/caret can be lost.

React's docs specifically mention changing keys and nested component definitions as causes of inputs being removed/re-added on every render. ([React][1])

This ties directly back to reconciliation and keys.

---

# 28. The caret jumping problem

Suppose:

```jsx
function Input() {
  const [name, setName] = useState("");

  return (
    <input
      value={name}
      onChange={e =>
        setName(e.target.value.toUpperCase())
      }
    />
  );
}
```

You are controlling the input with:

```text
DOM "abc"
   ↓
"A..." transformed state
   ↓
React forces transformed value
```

React docs warn that transforming/replacing the input value during `onChange` can cause caret/selection issues. The backing state for a controlled input should be synchronously updated to the input's current value. ([React][1])

The general idea is:

```text
user types
   ↓
input has current browser value
   ↓
React should synchronously acknowledge that value
```

rather than introducing a delayed or unrelated value.

---

# 29. Why asynchronous controlled-input updates can be problematic

Consider:

```jsx
onChange={e => {
  setTimeout(() => {
    setName(e.target.value);
  }, 100);
}}
```

The browser has already moved ahead:

```text
DOM = newest value
React state = old value
```

for some period.

But controlled inputs are fundamentally based on:

```text
React value
     ↓
must match DOM value
```

React's current docs explicitly warn against asynchronously updating a controlled input's backing state because this can cause caret/value problems. ([React][1])

---

# 30. Uncontrolled form with `FormData`

A very useful pattern:

```jsx
function Form() {
  function handleSubmit(e) {
    e.preventDefault();

    const formData =
      new FormData(e.currentTarget);

    console.log(
      formData.get("username")
    );
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        name="username"
        defaultValue=""
      />

      <button type="submit">
        Submit
      </button>
    </form>
  );
}
```

The browser owns the input values.

At submit time:

```text
DOM
 ↓
FormData
 ↓
application
```

No state is required for every keystroke.

React's current form docs explicitly show this approach and explain that `FormData` collects fields by their `name`. ([React][2])

---

# 31. Why `name` matters

For:

```jsx
<input name="email" />
```

the browser's form machinery understands:

```text
email → current value
```

Then:

```js
new FormData(form)
```

can produce corresponding form data.

So:

```jsx
<input name="firstName" />
```

isn't just decoration.

It's the key used by standard form submission/form-data collection.

React currently recommends giving form inputs names for this purpose. ([React][1])

---

# 32. Controlled vs uncontrolled architecture

### Controlled form

```text
                    React State
                        │
                  ┌─────┴─────┐
                  ↓           ↓
               Input 1     Input 2
                  │           │
                  └─────┬─────┘
                        │
                     onChange
                        ↓
                    setState
```

Every value change flows through React.

### Uncontrolled form

```text
                 Browser DOM
                     │
              ┌──────┼──────┐
              ↓      ↓      ↓
           Input1 Input2 Input3
                     │
                  submit
                     ↓
                 FormData
```

The DOM owns the live values.

---

# 33. When controlled forms are useful

Suppose you need live validation:

```jsx
const [email, setEmail] = useState("");

const valid = email.includes("@");
```

or:

```text
disable submit
show character counter
conditionally show fields
format values
enable/disable controls
coordinate multiple inputs
```

Then React state being the source of truth is often useful.

---

# 34. When uncontrolled forms are useful

Suppose you only care about the data on submission:

```text
user fills form
      ↓
submit
      ↓
read FormData
```

There's no need to maintain React state for every keystroke.

An uncontrolled approach can be simple:

```jsx
<input name="email" />
```

then:

```js
const data = new FormData(form);
```

Again, this is not a universal recommendation; it depends on what the UI needs.

---

# 35. Using refs with uncontrolled inputs

Another classic pattern:

```jsx
function Form() {
  const inputRef = useRef(null);

  function handleSubmit() {
    console.log(inputRef.current.value);
  }

  return (
    <>
      <input ref={inputRef} />
      <button onClick={handleSubmit}>
        Submit
      </button>
    </>
  );
}
```

Here:

```text
DOM
 ↓
input.value
```

is the source of truth.

The ref merely gives React code access to that DOM node.

Remember our previous `useRef` lesson:

```text
ref.current mutation
   ↓
doesn't trigger render
```

That is why refs fit naturally with uncontrolled inputs.

---

# 36. Controlled vs ref-based access

### Controlled

```jsx
const [value, setValue] = useState("");

<input
  value={value}
  onChange={e => setValue(e.target.value)}
/>
```

Source of truth:

```text
React state
```

### Uncontrolled + ref

```jsx
const ref = useRef(null);

<input ref={ref} />
```

Source of truth:

```text
DOM
```

Read when necessary:

```js
ref.current.value
```

---

# 37. Why React doesn't use `ref` as state

Imagine:

```jsx
const ref = useRef("");

<input
  ref={ref}
/>
```

The ref doesn't contain:

```text
input value
```

It contains:

```text
DOM node
```

So:

```js
ref.current.value
```

reads the value from the DOM.

And if you change:

```js
ref.current.value = "John";
```

React doesn't automatically rerender.

That's exactly why refs are imperative and state is reactive.

---

# 38. Controlled component beyond DOM elements

"Controlled" isn't only about `<input>`.

Consider a custom component:

```jsx
<Accordion
  isOpen={isOpen}
  onOpenChange={setIsOpen}
/>
```

The parent controls the important state.

The child doesn't own:

```text
isOpen
```

The React docs describe controlled/uncontrolled as broader component-design concepts, not strict terms limited to native inputs. ([React][8])

This is a good interview distinction.

---

# 39. Controlled custom component

Suppose:

```jsx
function Toggle({ value, onChange }) {
  return (
    <button onClick={() => onChange(!value)}>
      {value ? "ON" : "OFF"}
    </button>
  );
}
```

Parent:

```jsx
function App() {
  const [enabled, setEnabled] = useState(false);

  return (
    <Toggle
      value={enabled}
      onChange={setEnabled}
    />
  );
}
```

The data flow is:

```text
Parent state
    ↓
value
    ↓
Child
    ↓
onChange
    ↓
Parent state update
```

That's controlled architecture.

---

# 40. Two-way binding?

React doesn't have traditional automatic two-way binding like some frameworks.

Instead, you explicitly implement:

```text
state → prop
event → setter
```

Example:

```jsx
<input
  value={name}
  onChange={e => setName(e.target.value)}
/>
```

It looks like two-way binding:

```text
state ↔ input
```

but technically React is still doing a unidirectional data flow:

```text
state → rendered UI

user event → update state
```

This is a very common interview question.

---

# 41. React's one-way data flow

The controlled input demonstrates React's model perfectly:

```text
                 state
                   ↓
                 props
                   ↓
                  DOM
                   │
                user action
                   ↓
                 event
                   ↓
               setState
                   ↓
                 state
```

So the overall architecture remains:

```text
data DOWN
events UP
```

This is much more accurate than saying:

> "React has two-way data binding."

---

# 42. Why controlled input can rerender the component on every keystroke

Consider:

```jsx
function Form() {
  const [name, setName] = useState("");

  console.log("Form render");

  return (
    <input
      value={name}
      onChange={e => setName(e.target.value)}
    />
  );
}
```

Each keystroke:

```text
onChange
 ↓
setName
 ↓
Form update
 ↓
Form renders
```

So:

```text
"A"
"B"
"C"
"D"
```

can correspond to multiple React renders.

That's expected.

---

# 43. Performance concern with huge forms

Imagine:

```text
Form
 ├── 100 inputs
 ├── expensive validation
 ├── large preview
 └── complex child tree
```

and one local state update causes the whole form component subtree to be rendered.

Then each keystroke can become expensive.

Possible architectural responses include:

```text
split components
localize state
memoize expensive children when appropriate
uncontrolled fields where appropriate
defer non-urgent work
```

This is where our earlier performance concepts can become relevant.

But don't conclude:

> "Uncontrolled forms are always faster."

That's too broad.

---

# 44. Controlled input and `React.memo`

Suppose:

```jsx
const Field = React.memo(function Field({
  value,
  onChange
}) {
  return (
    <input
      value={value}
      onChange={onChange}
    />
  );
});
```

If the parent updates unrelated state, stable props may allow a bailout.

But if:

```text
value changes
```

then:

```text
Field props changed
```

so it needs to render.

This connects controlled inputs to the `memo` topic.

---

# 45. Why `onChange` should be synchronous

Suppose:

```jsx
function handleChange(e) {
  setValue(e.target.value);
}
```

Excellent.

The relationship is:

```text
current DOM value
       ↓
React state immediately
```

React's docs specifically require synchronous backing updates for controlled inputs. ([React][1])

The reason is conceptual consistency:

```text
DOM value
    ≈
React controlled value
```

If React significantly lags behind the browser's input state, React may force the DOM back toward the older state.

---

# 46. What if you don't need every keystroke?

This is where uncontrolled inputs can be attractive.

Instead of:

```jsx
const [value, setValue] = useState("");
```

and:

```jsx
<input
  value={value}
  onChange={...}
/>
```

you might use:

```jsx
<input name="value" />
```

and only read:

```js
formData.get("value")
```

when submitting.

That eliminates the React state update for every character.

---

# 47. Forms + modern React

React's current `<form>` API also supports a function passed to `action`:

```jsx
<form action={handleSubmit}>
  ...
</form>
```

React documents function-valued `action`/`formAction` as a modern form mechanism. ([React][2])

For your interview foundation, however, don't confuse this with the controlled/uncontrolled distinction.

The fundamental models remain:

```text
controlled input
   → React owns current value

uncontrolled input
   → DOM owns current value
```

Modern form Actions add another mechanism around submission.

---

# 48. Browser validation still exists

Even in React:

```jsx
<input required />
```

uses browser form validation behavior.

React doesn't replace the entire browser form system.

Instead:

```text
React
   ↕
Browser form APIs
```

React adds component-oriented rendering and events while still supporting native HTML semantics.

This is another reason semantic HTML is valuable.

---

# 49. The special nature of `checked`

For checkbox/radio:

```jsx
<input
  checked={enabled}
  onChange={...}
/>
```

React needs to synchronize:

```text
checked state
```

rather than:

```text
value
```

So mentally:

```text
text input
    → value

checkbox/radio
    → checked
```

React's current input docs make this distinction explicit. ([React][1])

---

# 50. `defaultChecked`

Uncontrolled checkbox:

```jsx
<input
  type="checkbox"
  defaultChecked={true}
/>
```

means:

```text
initial checked = true
```

but afterward:

```text
browser owns current checked state
```

Controlled checkbox:

```jsx
<input
  type="checkbox"
  checked={enabled}
  onChange={...}
/>
```

means:

```text
React owns current checked state
```

---

# 51. Important misconception: `defaultValue` isn't "a weaker value"

It's not:

```text
value = strong control
defaultValue = weak control
```

It's really two different ownership models:

```text
value
   → React controls current value

defaultValue
   → React specifies initial value
      then DOM manages current value
```

That's the key.

---

# 52. Input lifecycle

For a controlled input:

```text
Initial render
      ↓
React creates DOM input
      ↓
sets initial controlled value
      ↓
user types
      ↓
event fires
      ↓
state update
      ↓
render
      ↓
React reconciles
      ↓
host input update
      ↓
DOM synchronized
```

For uncontrolled input:

```text
Initial render
      ↓
React creates DOM input
      ↓
sets default initial value
      ↓
user types
      ↓
browser updates value
      ↓
React need not own every keystroke
```

That comparison is extremely useful in interviews.

---

# 53. Where Fiber enters the picture

For controlled:

```text
onChange
   ↓
setState
   ↓
Fiber's Hook queue
   ↓
schedule update
   ↓
work loop
   ↓
new element props
   ↓
existing input Fiber
   ↓
host update
```

For uncontrolled:

```text
user types
   ↓
browser DOM state changes
```

There may be no React state update at all.

That's a major architectural difference.

---

# 54. The DOM isn't "outside React" in a controlled input

People sometimes say:

> "Controlled means React handles the input, uncontrolled means the DOM handles it."

That's useful shorthand, but incomplete.

Even controlled inputs still involve the DOM.

The real distinction is:

```text
Who owns the authoritative current value?
```

Controlled:

```text
React state
```

Uncontrolled:

```text
DOM state
```

Both still render to the DOM.

---

# 55. Source-level mental model

React's current host input implementation can be thought of conceptually as:

```js
function updateInput(
  node,
  value,
  defaultValue,
  checked,
  defaultChecked,
  type,
  ...
) {
  // Determine controlled/uncontrolled behavior
  // Synchronize relevant DOM state
}
```

That's not the actual source signature to memorize.

The point is that React has a dedicated host implementation for input state rather than treating it like an arbitrary `<div>`. The current `ReactDOMInput.js` contains the actual logic. ([GitHub][6])

---

# 56. Why inputs are tricky for React

A normal element:

```jsx
<div className={name} />
```

mostly has:

```text
React props → DOM properties/attributes
```

An input additionally has:

```text
React props
      +
browser-managed mutable state
      +
user interaction
      +
selection/caret
      +
default values
      +
special input types
```

So:

```text
<input>
```

is one of the host components where React has to carefully synchronize two worlds.

---

# 57. Common interview questions

### Q1. What is a controlled component?

> A component whose important current value is driven by props/state supplied by React rather than being independently owned by its internal or DOM state.

For native text inputs:

```jsx
<input value={value} onChange={...} />
```

---

### Q2. What is an uncontrolled component?

> A component where the DOM or the component itself maintains the current value, while React may provide only the initial value or access the current value through a ref/form APIs.

Example:

```jsx
<input defaultValue="John" />
```

---

### Q3. `value` vs `defaultValue`?

> `value` controls the current value; `defaultValue` specifies the initial value for an uncontrolled input.

([React][1])

---

### Q4. Why does controlled input need `onChange`?

> Because React is controlling the value. The `onChange` handler needs to synchronously update the state that supplies the controlled value; otherwise the field becomes effectively read-only.

([React][1])

---

### Q5. Can an input switch from uncontrolled to controlled?

> No. React requires the controlled/uncontrolled mode to remain consistent for the input's lifetime.

([React][1])

---

### Q6. Why does `value={undefined}` cause problems?

Because it can make the input initially uncontrolled and later controlled.

Use:

```jsx
value={value ?? ""}
```

for a controlled text input whose data may initially be absent. ([React][1])

---

### Q7. How do you control a checkbox?

```jsx
<input
  type="checkbox"
  checked={enabled}
  onChange={e => setEnabled(e.target.checked)}
/>
```

Use:

```text
checked
```

not:

```text
value
```

([React][1])

---

### Q8. How do you read an uncontrolled form?

```js
const formData =
  new FormData(form);
```

and give inputs appropriate `name` attributes. ([React][2])

---

### Q9. Why can an input lose its value/caret unexpectedly?

One possible cause is that React is remounting the DOM node rather than updating the existing one, often due to unstable keys or changing component identity. ([React][1])

---

# 58. A strong interview question: "Which is better?"

Don't answer:

> "Controlled is better."

Instead answer:

> **"Neither is universally better. Controlled inputs are useful when React needs to own and react to the current value—for example, live validation or coordinated UI. Uncontrolled inputs let the browser manage the current value and can be convenient when the value is primarily needed at submission time, such as reading it with `FormData` or a ref."**

That's technically stronger.

---

# 59. Controlled vs uncontrolled summary

|                            | Controlled          | Uncontrolled                      |
| -------------------------- | ------------------- | --------------------------------- |
| Current value owned by     | React state/props   | DOM                               |
| Typical prop               | `value` / `checked` | `defaultValue` / `defaultChecked` |
| Change handler             | Usually required    | Not required just to mutate value |
| Read current value         | State               | Ref / DOM / `FormData`            |
| React update per keystroke | Usually yes         | Not necessarily                   |
| Easy live validation       | Yes                 | Requires reading DOM/value        |
| DOM-native behavior        | Still present       | More directly used                |

---

# 60. The most important mental model

Remember:

```text
                 CONTROLLED

React State
    ↓
 value / checked
    ↓
 DOM input
    ↓
 user interaction
    ↓
 React onChange
    ↓
 setState
    ↓
 React render
    ↓
 DOM synchronization
```

versus:

```text
                UNCONTROLLED

React
  ↓
 defaultValue
  ↓
 DOM input
  ↓
 user interaction
  ↓
 DOM owns current value

            later

ref / FormData
      ↓
read current value
```

That's the entire concept.

---

# 61. One particularly important interview statement

When asked:

> **"What does `value` actually mean on a React input?"**

Don't answer:

> "It sets the value."

A stronger answer is:

> **"For a text input, supplying `value` makes the input controlled. The value becomes part of React's rendered state, and React is responsible for keeping the DOM input synchronized with that value. The input therefore needs a synchronous change handler that updates the state supplying the value."** ([React][1])

That demonstrates understanding of **ownership**, rather than just syntax.

---

# 62. Complete connection to everything we've learned

```text
                    USER TYPES
                         │
                         ↓
                     Browser DOM
                         │
                         ↓
                   Native event
                         │
                         ↓
                  React Event System
                         │
                         ↓
                    onChange
                         │
                         ↓
                     setState
                         │
                         ↓
                   Hook Update Queue
                         │
                         ↓
                     Fiber lanes
                         │
                         ↓
                    Fiber work loop
                         │
                         ↓
                       render
                         │
                         ↓
                  new input props
                         │
                         ↓
                ReactDOMInput logic
                         │
                         ↓
                       commit
                         │
                         ↓
                   DOM synchronized
```

This one flow combines:

**Events + Hooks + Fiber + Scheduling + Rendering + Reconciliation + Commit + DOM**

which is exactly why forms are such a useful interview topic.

### Next Topic → `useImperativeHandle`, Refs & Imperative APIs

We'll go deeper into **how refs reach DOM nodes, callback refs vs object refs, `forwardRef`/React 19 ref behavior, `useImperativeHandle`, imperative handles, commit-time ref attachment, and why imperative APIs are deliberately kept outside React's normal declarative data flow.**

[1]: https://react.dev/reference/react-dom/components/input?utm_source=chatgpt.com "<input> – React"
[2]: https://react.dev/reference/react-dom/components/form?utm_source=chatgpt.com "<form> – React"
[3]: https://react.dev/reference/react-dom/components/select?utm_source=chatgpt.com "<select> – React"
[4]: https://github.com/facebook/react/blob/main/packages/react-dom-bindings/src/events/plugins/ChangeEventPlugin.js "react/packages/react-dom-bindings/src/events/plugins/ChangeEventPlugin.js at main · react/react · GitHub"
[5]: https://gist.github.com/pvpshoot/74f1c03ad2008411765f8570e8ec887b?utm_source=chatgpt.com "bundle.js · GitHub"
[6]: https://github.com/facebook/react/blob/main/packages/react-dom-bindings/src/client/ReactDOMInput.js "react/packages/react-dom-bindings/src/client/ReactDOMInput.js at main · react/react · GitHub"
[7]: https://react.dev/reference/react-dom/components/textarea?utm_source=chatgpt.com "<textarea> – React"
[8]: https://react.dev/learn/sharing-state-between-components?utm_source=chatgpt.com "Sharing State Between Components – React"

