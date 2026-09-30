# Topic 15 — React Event System & Synthetic Events

This topic is worth learning deeply because an interview question like:

> **"What happens internally when I click a React button?"**

can actually connect:

**Browser → native event → React root listener → event priority → Fiber lookup → event extraction → propagation → handler → state update → scheduler → render → commit**

That is a very good React-internals interview question.

---

# 1. Start with normal JavaScript

Without React:

```html
<button id="btn">Click</button>
```

```js
const button = document.getElementById("btn");

button.addEventListener("click", () => {
  console.log("clicked");
});
```

The browser owns the event system.

Conceptually:

```text
User clicks
    ↓
Browser detects native click
    ↓
DOM event propagation
    ↓
registered event listeners
    ↓
callback executes
```

React has to integrate with this browser event system while maintaining its own:

```text
React tree
Fiber tree
component event props
priority/scheduling
portals
multiple roots
hydration
SyntheticEvent
```

So React adds a layer between the browser event and your component handler.

---

# 2. What you write in React

You write:

```jsx
function Button() {
  function handleClick() {
    console.log("clicked");
  }

  return (
    <button onClick={handleClick}>
      Click
    </button>
  );
}
```

It is tempting to imagine React doing:

```js
button.addEventListener("click", handleClick);
```

for every button.

That's **not the right mental model for modern React**.

React attaches event handling infrastructure at the root/container level for delegated events, then determines which React listeners should run for the event. The current source's `listenToAllSupportedEvents()` installs root-level listeners for supported native events, with certain non-delegated events handled directly on target elements. ([GitHub][1])

So conceptually:

```text
React root
    │
    └── one event-system entry point
             │
             ├── Button
             ├── Input
             ├── Link
             └── ...
```

rather than:

```text
Button → native listener
Input  → native listener
Link   → native listener
...
```

---

# 3. Event delegation

This technique is called **event delegation**.

Instead of:

```text
button1 → click listener
button2 → click listener
button3 → click listener
button4 → click listener
```

you can have:

```text
root → click listener
```

and when the browser reports a click, determine where it occurred.

For example:

```html
<div id="root">
  <button>
    <span>Click me</span>
  </button>
</div>
```

The browser fires a native event whose target might be:

```text
<span>
```

React can use that event and the DOM/Fiber association to determine:

```text
span
 ↓
button Fiber
 ↓
parent Fiber
 ↓
root
```

and find the React listeners that belong to that path.

The current React event system explicitly accumulates listeners by walking from the target Fiber toward the root. ([GitHub][1])

---

# 4. Important historical detail: React 16 vs React 17+

You may hear two seemingly contradictory interview answers:

> "React attaches events to `document`."

and:

> "React attaches events to the root container."

Both can make sense depending on the React generation being discussed.

Historically, React's event delegation was largely document-level.

React 17 changed the delegation architecture so events are attached to the **root container**, which improves interoperability with code outside React and makes multiple React roots behave more independently.

For modern React interviews, think:

```text
React root/container
    ↓
event delegation
```

not simply:

```text
document
```

The current source's `listenToAllSupportedEvents(rootContainerElement)` confirms root-container event registration. ([GitHub][1])

---

# 5. When `createRoot()` happens

Suppose:

```jsx
const root = createRoot(
  document.getElementById("root")
);

root.render(<App />);
```

Part of React DOM's setup includes establishing the event-listening infrastructure for that root.

Conceptually:

```text
createRoot(...)
      ↓
React root/container
      ↓
event system setup
      ↓
native listeners attached
```

The current implementation uses `listenToAllSupportedEvents()` and keeps a marker on the root so the listener setup isn't duplicated. ([GitHub][1])

---

# 6. What happens when you click?

Let's trace:

```jsx
function Button() {
  return (
    <button onClick={handleClick}>
      Click
    </button>
  );
}
```

User clicks the button.

The high-level path is:

```text
1. Browser fires native click
             ↓
2. React's native listener receives it
             ↓
3. React determines event priority
             ↓
4. React finds target Fiber
             ↓
5. React extracts/builds React event
             ↓
6. React finds relevant listeners
             ↓
7. React processes capture/bubble order
             ↓
8. Your handler executes
             ↓
9. dispatch/setState may schedule React work
             ↓
10. render
             ↓
11. commit
```

The current React source has explicit functions corresponding to several of these phases. ([GitHub][2])

---

# 7. Native event first

The first event is still a **browser event**.

For:

```jsx
<button onClick={handleClick}>
```

the browser doesn't know about:

```text
handleClick
```

It knows about:

```text
click
```

So:

```text
Browser
  ↓
native MouseEvent
```

React receives that native event.

The React event system then creates the abstraction your handler receives.

---

# 8. Synthetic Event

Your handler:

```jsx
function handleClick(e) {
  console.log(e);
}
```

doesn't receive the raw browser `MouseEvent` directly as its primary event object.

It receives a **React event object**, commonly called a **SyntheticEvent**.

React documents that the event object follows the standard DOM event interface while normalizing browser differences. It also exposes the underlying browser event through `e.nativeEvent`. ([React][3])

Example:

```jsx
function Button() {
  return (
    <button onClick={(e) => {
      console.log(e);
      console.log(e.nativeEvent);
    }}>
      Click
    </button>
  );
}
```

Conceptually:

```text
React event
 ├── type
 ├── target
 ├── currentTarget
 ├── preventDefault()
 ├── stopPropagation()
 ├── nativeEvent
 └── ...
```

and:

```text
nativeEvent
    ↓
actual browser Event
```

---

# 9. Why did React create SyntheticEvent?

Historically, browsers differed significantly in their event APIs and behavior.

React wanted your component code to use a more consistent interface.

So instead of your application having to care about every browser's quirks:

```text
browser A
browser B
browser C
...
```

React presents:

```text
React event abstraction
```

The current source's `SyntheticBaseEvent` copies/normalizes event interface properties and keeps the native event reference. ([GitHub][4])

---

# 10. SyntheticEvent is not a fake event

Don't explain it as:

> "React invents a completely fake event."

That's misleading.

A better explanation:

> **React creates a wrapper/normalized event object around the underlying native event and dispatches that object through React's event system.**

You can access the browser event:

```js
e.nativeEvent
```

React documents that some React events don't map 1:1 to the same native event. For example, `onMouseLeave` can correspond to a native `mouseout`. The mapping is intentionally not part of the public API. ([React][3])

---

# 11. Example: `onMouseLeave`

You write:

```jsx
<div
  onMouseLeave={handleLeave}
/>
```

The underlying browser event is not necessarily:

```text
nativeEvent.type === "mouseleave"
```

React can build its higher-level event semantics from native browser events.

That's why you should generally use:

```jsx
onMouseLeave
```

rather than relying on internal native mappings.

React explicitly warns that these native mappings can change. ([React][3])

---

# 12. React's event plugins

Current React DOM event code uses an event-plugin architecture.

The important conceptual pipeline is:

```text
Native event
   ↓
React event system
   ↓
plugin/event extraction
   ↓
Synthetic event
   ↓
dispatch queue
   ↓
listeners
```

The current `SimpleEventPlugin` maps native event names into React event names and accumulates the appropriate listeners. ([GitHub][5])

You don't normally interact with this plugin system directly.

It's React's internal machinery.

---

# 13. Event extraction

Suppose the browser fires:

```text
click
```

React's plugin system needs to determine:

```text
What React event is this?
Which listeners should be called?
Capture or bubble?
What event object should be created?
```

The current event system does roughly:

```text
native event
    ↓
extractEvents(...)
    ↓
dispatchQueue
    ↓
processDispatchQueue(...)
```

This exact architecture is visible in the current source. ([GitHub][1])

---

# 14. Dispatch queue

Conceptually React builds something like:

```js
dispatchQueue = [
  {
    event,
    listeners
  }
];
```

Where:

```text
event
  ↓
SyntheticEvent

listeners
  ↓
ordered React listeners
```

This is useful because React can determine the entire dispatch path before executing the handlers.

---

# 15. How does React find the component that was clicked?

Remember Fiber:

```text
DOM node
   ↕
Fiber
```

React maintains internal associations that let it map a DOM node to the corresponding Fiber.

Conceptually:

```text
nativeEvent.target
       ↓
DOM node
       ↓
React Fiber
       ↓
component tree path
```

The current event listener code includes logic such as `findInstanceBlockingEvent()` and `getInstanceFromNode()` as part of handling event targets and hydration/blocking scenarios. ([GitHub][2])

This is one of the reasons your previous Fiber lessons matter here.

---

# 16. Fiber path construction

Imagine the React tree:

```text
App Fiber
   │
   └── Toolbar Fiber
          │
          └── Button Fiber
                 │
                 └── HostComponent <button>
```

Browser says:

```text
target = <button>
```

React can conceptually walk:

```text
<button>
   ↑
Button Fiber
   ↑
Toolbar Fiber
   ↑
App Fiber
```

The current implementation's listener accumulation function literally walks the target Fiber toward the root path. ([GitHub][1])

This is where React's tree abstraction becomes extremely powerful.

---

# 17. Capture and bubble phases

React follows the familiar event phases.

Consider:

```jsx
<div onClickCapture={parentCapture}>
  <button onClickCapture={childCapture}>
    Click
  </button>
</div>
```

and:

```text
button → clicked
```

The conceptual order is:

```text
parent capture
      ↓
child capture
      ↓
target/bubble listener
      ↓
parent bubble
```

React's documentation describes event propagation as capture → target → bubble. ([React][6])

---

# 18. Example

```jsx
function App() {
  return (
    <div
      onClickCapture={() => console.log("parent capture")}
      onClick={() => console.log("parent bubble")}
    >
      <button
        onClickCapture={() => console.log("button capture")}
        onClick={() => console.log("button bubble")}
      >
        Click
      </button>
    </div>
  );
}
```

Clicking the button gives conceptually:

```text
parent capture
button capture
button bubble
parent bubble
```

That is the important ordering.

---

# 19. How React constructs this order

The current event system's `accumulateSinglePhaseListeners()` starts at the target Fiber and walks toward the root, collecting matching listeners. ([GitHub][1])

Conceptually:

```text
Target Fiber
    ↑
    │ collect
    │
Parent Fiber
    ↑
    │ collect
    │
Root Fiber
```

Then React uses the appropriate ordering depending on capture vs bubble.

So React isn't simply relying on the DOM to individually invoke every `onClick` prop.

It uses its own dispatch machinery.

---

# 20. `stopPropagation()`

You can write:

```jsx
function Button() {
  return (
    <button
      onClick={(e) => {
        e.stopPropagation();
      }}
    >
      Click
    </button>
  );
}
```

This tells React:

```text
don't continue propagation to ancestors
```

The current dispatch code checks `event.isPropagationStopped()` while processing listeners. ([GitHub][1])

And React's `SyntheticEvent.stopPropagation()` also calls the underlying native event's `stopPropagation()` when available. ([GitHub][4])

So:

```text
React propagation state
       +
native event propagation state
```

are kept aligned.

---

# 21. `preventDefault()` is different

This distinction is asked constantly.

### `stopPropagation()`

Controls:

```text
WHERE the event travels
```

### `preventDefault()`

Controls:

```text
DEFAULT browser behavior
```

Example:

```jsx
<form
  onSubmit={(e) => {
    e.preventDefault();
  }}
>
```

This prevents the browser's default submit behavior.

It does **not** mean:

```text
don't notify parent handlers
```

That's propagation's job.

React documents the distinction explicitly. ([React][6])

---

# 22. `target` vs `currentTarget`

Another classic interview question.

Suppose:

```jsx
<div onClick={handleParent}>
  <button>
    Click
  </button>
</div>
```

Inside `handleParent`:

```js
function handleParent(e) {
  console.log(e.target);
  console.log(e.currentTarget);
}
```

Conceptually:

```text
target
   ↓
where the event actually originated

currentTarget
   ↓
the element whose handler is currently executing
```

If the button was clicked:

```text
target        = button
currentTarget = div
```

React's documented event object exposes both. ([React][3])

---

# 23. Why `currentTarget` is special in React

React's current docs point out something subtle:

> Under the hood, React attaches event handlers at the root, but this is not reflected in the React event object's `currentTarget`.

So:

```js
e.currentTarget
```

represents the React listener's logical target:

```text
<div>
```

not necessarily:

```text
root container
```

where React's delegated native listener is actually attached. ([React][3])

This is a great interview detail.

---

# 24. React event system vs DOM event system

Think of two layers.

### Browser layer

```text
DOM
 ↓
native event
 ↓
browser propagation
 ↓
native listeners
```

### React layer

```text
native event
 ↓
React event entry point
 ↓
target Fiber
 ↓
React listener accumulation
 ↓
SyntheticEvent
 ↓
React propagation
 ↓
component handlers
```

React integrates with the native system, but implements its own dispatching semantics for React handlers.

---

# 25. Why React needs its own propagation logic

Consider a React portal.

We'll study portals separately in depth later, but conceptually:

```text
React tree:

App
 └── Parent
      └── Child

DOM tree:

Root
 ├── Parent DOM
 └── Portal DOM
      └── Child DOM
```

The React parent/child relationship may differ from the DOM parent/child relationship.

If React simply relied on raw DOM bubbling, it couldn't always express React's tree semantics correctly.

So React's own event system walks the **Fiber tree** to find listeners.

That is an important reason the Fiber integration matters.

---

# 26. Nested React roots

Another reason root-level event delegation matters:

```text
DOM root A
   └── React tree A

DOM root B
   └── React tree B
```

React needs event handling to be scoped appropriately to each root.

The modern root-based architecture makes that possible.

So rather than assuming:

```text
one React application = one document-level event system
```

think:

```text
React root
   ↓
its event system integration
```

---

# 27. Event priority

Now we connect events to the **scheduler and lanes** you learned earlier.

Not all user interactions have the same urgency.

React classifies event types into event priorities.

The current source's `createEventListenerWrapperWithPriority()` calls `getEventPriority(domEventName)` and selects wrappers for:

```text
DiscreteEventPriority
ContinuousEventPriority
DefaultEventPriority
```

depending on the event. ([GitHub][2])

---

# 28. Discrete events

Examples include things like:

```text
click
keydown
submit
```

These represent distinct user interactions.

Conceptually:

```text
click
 ↓
high urgency
```

React's current event listener implementation explicitly switches discrete events to `DiscreteEventPriority`. ([GitHub][2])

---

# 29. Continuous events

Examples include interactions that can generate many events continuously, such as:

```text
mousemove
pointermove
scroll-related work
```

Conceptually:

```text
mousemove
mousemove
mousemove
mousemove
...
```

You don't necessarily want React to treat every event exactly like a discrete click.

The current event infrastructure has a `ContinuousEventPriority` path. ([GitHub][2])

---

# 30. Why event priority exists

Imagine:

```text
User clicks "Save"
```

and simultaneously:

```text
mouse is moving continuously
```

React needs to represent the urgency of those updates differently.

So:

```text
Browser event
      ↓
React event priority
      ↓
update priority
      ↓
scheduler / lanes
```

This is the key connection.

---

# 31. Current source: discrete event path

The current implementation essentially does:

```text
native click
    ↓
createEventListenerWrapperWithPriority()
    ↓
getEventPriority("click")
    ↓
DiscreteEventPriority
    ↓
dispatchDiscreteEvent()
    ↓
setCurrentUpdatePriority(...)
    ↓
dispatchEvent(...)
```

The source explicitly sets the current update priority around dispatching the event. ([GitHub][2])

So an event isn't merely:

```text
callback()
```

inside React's architecture.

It also establishes an update-priority context for updates triggered during the callback.

---

# 32. This explains something important

Suppose:

```jsx
function Button() {
  const [count, setCount] = useState(0);

  function handleClick() {
    setCount(c => c + 1);
  }

  return <button onClick={handleClick}>{count}</button>;
}
```

The path is approximately:

```text
click
 ↓
React event system
 ↓
discrete event priority
 ↓
handleClick()
 ↓
setCount(...)
 ↓
update created
 ↓
Fiber scheduled
 ↓
render
 ↓
commit
```

So the event system is one of the entry points through which React's scheduler receives work.

---

# 33. Event handler itself can cause side effects

Remember our rendering lesson:

```text
Render should be pure
```

but event handlers can perform side effects.

For example:

```jsx
function Button() {
  function handleClick() {
    localStorage.setItem("clicked", "true");
    fetch("/api/log");
  }

  return <button onClick={handleClick}>Click</button>;
}
```

That's appropriate because this is:

```text
event-driven side effect
```

rather than:

```text
render-time side effect
```

React's docs specifically recommend event handlers as the place for side effects caused by user interactions. ([React][6])

---

# 34. Event handler closure

Event handlers are functions created during rendering.

Example:

```jsx
function Counter() {
  const [count, setCount] = useState(0);

  function handleClick() {
    console.log(count);
  }

  return <button onClick={handleClick}>Click</button>;
}
```

That handler closes over the `count` from its render.

So:

```text
Render #1
count = 0
handleClick₁ → sees 0

Render #2
count = 1
handleClick₂ → sees 1
```

This is the same snapshot/closure model you learned with Hooks.

The browser eventually triggers the handler React has associated with the current tree.

---

# 35. Why `onClick={handleClick}` is correct

You often see:

```jsx
<button onClick={handleClick}>
```

not:

```jsx
<button onClick={handleClick()}>
```

The first means:

```text
pass function reference
```

The second means:

```text
execute during render
then pass result
```

React's docs explicitly call this out. ([React][6])

Think:

```text
onClick={handleClick}

JSX creation
    ↓
store handler reference
    ↓
future click
    ↓
React calls it
```

---

# 36. Inline handlers

This:

```jsx
<button onClick={() => setCount(c => c + 1)}>
```

is also valid.

During render you're creating a function:

```text
function object
```

and React stores the handler as part of the element's props.

Later the event system looks up the current listener.

This is different from actually attaching a brand-new native DOM listener manually on every render.

---

# 37. Where does React find the handler?

Remember JSX:

```jsx
<button onClick={handleClick}>
```

becomes a React element description whose props contain:

```js
{
  onClick: handleClick
}
```

During reconciliation, React creates/updates the Fiber/host representation.

Later, when the event arrives, the event system can inspect the corresponding Fiber/host node and retrieve the appropriate React listener.

The current `accumulateSinglePhaseListeners()` calls `getListener(instance, reactEventName)` while traversing Fibers. ([GitHub][1])

So there's a beautiful chain:

```text
JSX
 ↓
props.onClick
 ↓
Fiber
 ↓
DOM ↔ Fiber association
 ↓
event system
 ↓
getListener(...)
 ↓
your function
```

---

# 38. Why event handlers don't live in the DOM attribute

In plain HTML you might conceptually think:

```html
<button onclick="...">
```

React isn't just converting:

```jsx
onClick={handleClick}
```

into:

```html
onclick="handleClick"
```

The event handler is part of React's JavaScript/Fiber machinery.

That's another reason React can implement:

```text
Fiber-tree propagation
event priority
portals
synthetic events
root-scoped delegation
```

---

# 39. Event pooling — an old interview trap

You may hear:

> "You must call `event.persist()` because React pools synthetic events."

That was historically relevant.

Modern React DOM **does not use event pooling**.

The current `SyntheticEvent` source explicitly has:

```js
persist() {
  // Modern event system doesn't use pooling.
}
```

and `isPersistent()` returns true. ([GitHub][4])

React's current docs likewise state that `persist()` is not used with React DOM. ([React][3])

So in a modern React interview:

```text
event pooling
```

should be explained as:

> Historical behavior, not the current React DOM event model.

---

# 40. Can you access the native event?

Yes:

```jsx
function Button() {
  return (
    <button
      onClick={(e) => {
        console.log(e.nativeEvent);
      }}
    />
  );
}
```

This gives:

```text
underlying browser event
```

But don't build application logic around undocumented React-to-native mappings.

For example:

```text
React onMouseLeave
      ↓
native mouseout
```

can exist as an implementation detail and isn't guaranteed as public API. ([React][3])

---

# 41. Some events are non-delegated

Here's a subtle internals question.

Most events can be handled using delegated root listeners.

But React has a set of **non-delegated events**.

The current source includes events such as:

```text
beforetoggle
cancel
close
invalid
load
scroll
scrollend
toggle
```

in `nonDelegatedEvents`. React's source comments explain that these aren't delegated to the container because they don't consistently bubble in the DOM. ([GitHub][1])

So the simplified statement:

> "React uses one root listener for every event."

is too absolute.

A better statement:

> **React uses root-level delegation for most supported events, with exceptions for events that are handled directly on target elements or otherwise require special treatment.**

---

# 42. `onScroll` is a common example

React's docs specifically note that `onScroll` behaves differently from most events and only works on the JSX tag you attach it to. ([React][6])

So don't memorize:

```text
EVERY event bubbles through React
```

as an absolute rule.

The exact modern behavior is event-specific.

---

# 43. React propagation is not always identical to raw DOM propagation

This matters in interviews.

React may intentionally normalize or synthesize behavior.

For example, React docs document several event types that bubble in React even where the corresponding browser event behavior differs. ([React][3])

Therefore:

```text
"React events are exactly the browser events"
```

is incorrect.

Better:

```text
React integrates with native DOM events
but provides its own event abstraction and dispatch semantics.
```

---

# 44. Event system and Fiber

Let's connect the architecture.

Suppose:

```text
App
 └── Toolbar
      └── Button
```

Fiber tree:

```text
App Fiber
   │
   └── Toolbar Fiber
          │
          └── Button Fiber
                 │
                 └── HostComponent Fiber
                       │
                       └── <button>
```

Native event:

```text
click(<button>)
```

React:

```text
native target
     ↓
HostComponent Fiber
     ↓
walk Fiber ancestors
     ↓
find onClick listeners
     ↓
create SyntheticEvent
     ↓
dispatch in correct order
```

This is one of the clearest examples of why Fiber isn't just a rendering data structure.

Fiber is used by multiple parts of React's runtime architecture.

---

# 45. Event system and state updates

Now suppose:

```jsx
function Button() {
  const [count, setCount] = useState(0);

  return (
    <button
      onClick={() => {
        setCount(c => c + 1);
      }}
    >
      {count}
    </button>
  );
}
```

Full pipeline:

```text
Browser
  ↓
native click
  ↓
React root listener
  ↓
event priority
  ↓
target Fiber lookup
  ↓
SyntheticEvent
  ↓
find onClick
  ↓
execute handler
  ↓
setCount()
  ↓
enqueue Hook update
  ↓
lane / scheduling
  ↓
Fiber work loop
  ↓
beginWork
  ↓
useState update processing
  ↓
new JSX
  ↓
reconciliation
  ↓
commit
  ↓
DOM updated
```

This single example ties together almost every topic we've covered.

---

# 46. Automatic batching and events

Suppose:

```jsx
function handleClick() {
  setA(a => a + 1);
  setB(b => b + 1);
  setC(c => c + 1);
}
```

React can batch these updates as part of its update processing.

Conceptually:

```text
one event
   ↓
multiple updates
   ↓
queue
   ↓
render
   ↓
commit
```

This is one reason we don't usually see three independent screen commits simply because there were three setters in one event handler.

---

# 47. `stopPropagation()` doesn't undo the handler that already ran

Suppose:

```jsx
<button
  onClick={(e) => {
    e.stopPropagation();
    console.log("button");
  }}
/>
```

Then:

```text
button handler executes
      ↓
stopPropagation()
      ↓
ancestor handlers don't continue
```

It doesn't mean:

```text
"cancel everything that happened before this call"
```

Propagation is about moving through the event path.

React's dispatch queue checks propagation state as it traverses listeners. ([GitHub][1])

---

# 48. `return false` doesn't replace `preventDefault()`

Another common interview trap.

In React:

```jsx
<button
  onClick={() => {
    return false;
  }}
/>
```

does not mean:

```text
prevent default
```

Use:

```js
e.preventDefault();
```

The React event docs explicitly recommend using `preventDefault()` for that purpose. ([React][6])

---

# 49. Capture phase is less commonly used

Normal:

```jsx
onClick
```

means bubble phase.

Capture:

```jsx
onClickCapture
```

means capture phase.

Example:

```jsx
<div onClickCapture={handleCapture}>
```

can observe an event as it moves downward through the tree before it reaches the target.

React recommends capture for specialized cases such as global analytics/routing-like interception; normal application event handling more often uses bubbling. ([React][6])

---

# 50. React events and accessibility

This is not an internals question, but it matters in interviews.

Prefer:

```jsx
<button onClick={handleClick}>
```

rather than:

```jsx
<div onClick={handleClick}>
```

when you are creating a button.

Why?

Because the native `<button>` already provides:

```text
keyboard interaction
focus behavior
semantics
accessibility support
```

React's docs explicitly recommend using appropriate HTML elements for event handlers. ([React][6])

---

# 51. A simplified internal implementation

You can construct a mental model like this:

```js
function handleNativeEvent(nativeEvent) {
  const targetNode = nativeEvent.target;

  const targetFiber =
    findFiberFromDOMNode(targetNode);

  const reactEvent =
    createSyntheticEvent(nativeEvent);

  const listeners =
    collectReactListeners(
      targetFiber,
      nativeEvent.type
    );

  processListeners(
    reactEvent,
    listeners
  );
}
```

And:

```js
function collectReactListeners(
  targetFiber,
  eventName
) {
  const listeners = [];

  let fiber = targetFiber;

  while (fiber !== null) {
    const listener =
      getReactListener(fiber, eventName);

    if (listener) {
      listeners.push(listener);
    }

    fiber = fiber.return;
  }

  return listeners;
}
```

This is deliberately simplified.

The current source has dedicated event plugins, listener accumulation, dispatch queues, priority wrappers, root/container handling, hydration handling, and event-specific behavior. ([GitHub][5])

---

# 52. Actual internal flow worth memorizing

The current source gives us a useful architecture:

```text
Root listener
    ↓
createEventListenerWrapperWithPriority()
    ↓
getEventPriority()
    ↓
dispatchDiscreteEvent /
dispatchContinuousEvent /
dispatchEvent
    ↓
dispatchEventForPluginEventSystem()
    ↓
dispatchEventsForPlugins()
    ↓
extractEvents()
    ↓
processDispatchQueue()
    ↓
execute listeners
```

The names and exact source organization are implementation details and can change, but this is the modern architecture visible in React's current source. ([GitHub][2])

---

# 53. Why the event system needs priority

Now connect directly to our earlier **lanes** lesson.

We learned:

```text
React work
   ↓
lanes
   ↓
priority/scheduling
```

Now:

```text
Browser event
   ↓
event priority
   ↓
state update priority
   ↓
lanes
```

So a user interaction is one of the places where React decides how urgent the resulting work should be.

This is why the event system isn't merely a convenience wrapper around `addEventListener`.

It is integrated into React's scheduling architecture.

---

# 54. Discrete vs continuous — interview answer

If asked:

> **"What's event priority in React?"**

Say:

> React classifies events by urgency. Discrete events such as clicks are handled with discrete event priority, while continuous interactions have continuous event priority. This priority is used when dispatching events and helps React schedule updates triggered from those interactions appropriately.

The current event system explicitly maps event names to priority categories and uses those categories when dispatching. ([GitHub][2])

Don't overclaim that event priority is itself identical to a specific lane constant. React's internal lane/event-priority mapping is implementation detail and evolves.

---

# 55. Important distinction: native propagation vs React propagation

This is advanced but useful.

There can be:

```text
Browser event propagation
```

and:

```text
React listener dispatch through Fiber
```

React uses the native browser event as its entry point, but then determines React listeners based on the React tree.

That's why things like:

```text
portals
multiple roots
SyntheticEvent
React-specific propagation
```

need their own event logic.

---

# 56. Interview traps

### Trap 1

> "React adds an event listener directly to every DOM node."

Modern React:

**Not as a general model.**

React uses delegated root-level listeners for most events, with non-delegated exceptions. ([GitHub][1])

---

### Trap 2

> "React events are completely different from native events."

Not quite.

They are React event objects built around/native-connected to browser events.

Use:

```js
e.nativeEvent
```

for the underlying native event. ([React][3])

---

### Trap 3

> "`event.persist()` is required for async callbacks."

Not in modern React DOM.

Event pooling is no longer used. ([GitHub][4])

---

### Trap 4

> "`stopPropagation()` prevents the default browser behavior."

No.

```text
stopPropagation()
    → stops propagation

preventDefault()
    → prevents default action
```

([React][6])

---

### Trap 5

> "target and currentTarget are the same."

Not necessarily.

```text
target
    = original event target

currentTarget
    = element whose handler is currently executing
```

([React][3])

---

### Trap 6

> "All React events bubble exactly like DOM events."

No.

React normalizes event behavior, and there are event-specific differences and exceptions. ([React][3])

---

# 57. A realistic interview scenario

Interviewer:

> "You have a button with `onClick`. Walk me through what happens when the user clicks it."

A strong answer:

> **"The browser first produces a native click event. React's DOM event system has listeners installed on the React root for delegated events, and the incoming event is routed through React's event infrastructure. React determines the event priority, identifies the target Fiber corresponding to the DOM target, and extracts a React event object. It then walks the Fiber path toward the root to collect the relevant capture or bubble listeners, builds a dispatch queue, and invokes those listeners in the appropriate order. If the handler calls `setState`, an update is queued on the corresponding Hook/Fiber and React schedules work according to its update priority. The normal render/reconciliation/commit pipeline then produces the UI update."** ([GitHub][1])

That's a very strong React internals answer.

---

# 58. The entire architecture in one diagram

```text
                       BROWSER
                          │
                          │ native event
                          ↓
                ┌─────────────────────┐
                │ React Root Listener │
                └──────────┬──────────┘
                           │
                           ↓
                    Event Priority
                           │
             ┌─────────────┼─────────────┐
             ↓             ↓             ↓
          Discrete      Continuous      Default
             │             │             │
             └─────────────┼─────────────┘
                           ↓
                    React Event System
                           │
                           ↓
                     Target Fiber
                           │
                           ↓
               Walk Fiber → root path
                           │
                           ↓
                  Collect listeners
                           │
                           ↓
                  SyntheticEvent
                           │
                           ↓
                  Dispatch Queue
                           │
                           ↓
                 Capture / Bubble
                           │
                           ↓
                   Your handler
                           │
                           ↓
                   setState / dispatch
                           │
                           ↓
                        Update
                           │
                           ↓
                         Lanes
                           │
                           ↓
                    Fiber Work Loop
                           │
                           ↓
                    Render / Reconcile
                           │
                           ↓
                         Commit
                           │
                           ↓
                          DOM
```

That is the mental model I want you to retain.

---

# 59. `SyntheticEvent` vs native `Event`

| React event                          | Native event                |
| ------------------------------------ | --------------------------- |
| React-created event object           | Browser-created event       |
| Normalized API                       | Browser API                 |
| Used by `onClick`, `onChange`, etc.  | Used by `addEventListener`  |
| Has `nativeEvent`                    | Is the actual browser event |
| React propagation semantics          | DOM propagation semantics   |
| Integrated with React's event system | Controlled by browser       |

React's current docs confirm that the React event object exposes the native event via `nativeEvent` and that some event mappings are intentionally implementation-specific. ([React][3])

---

# 60. `onClick` vs `addEventListener`

### React

```jsx
<button onClick={handleClick}>
```

React manages:

```text
event registration infrastructure
listener lookup
propagation
event object
priority
Fiber integration
```

### Native DOM

```js
button.addEventListener(
  "click",
  handleClick
);
```

Browser manages:

```text
native event listener
DOM propagation
native Event object
```

Neither is inherently "better" in every situation; they belong to different layers.

---

# 61. Why this matters for your previous topics

Notice how many topics connect:

### JSX

```jsx
<button onClick={handleClick}>
```

↓

JSX becomes element props.

### Fiber

```text
DOM ↔ Fiber
```

↓

React can find the component represented by the target.

### Scheduling

```text
event priority
   ↓
update scheduling
```

### Hooks

```text
onClick
   ↓
setState / dispatch
   ↓
Hook update queue
```

### Reconciliation

```text
new state
   ↓
new JSX
   ↓
reconciliation
```

### Commit

```text
final changes
   ↓
DOM mutation
```

So this topic isn't isolated.

It's the **bridge from the browser into React's rendering engine**.

---

# 62. Final mental model

The three most important concepts are:

```text
1. Event delegation

Browser event
      ↓
React root
      ↓
React dispatch system
```

```text
2. SyntheticEvent

native browser event
      ↓
React event abstraction
      ↓
your handler
```

```text
3. Event → React update

event
  ↓
handler
  ↓
setState / dispatch
  ↓
Fiber update
  ↓
lanes/scheduling
  ↓
render
  ↓
commit
```

And the interview-quality one-liner:

> **React's event system uses the browser's native events as its entry point, delegates most events through the React root, maps the native event to the target Fiber, creates/dispatches a React event object through the appropriate listener path, and integrates event priority with React's scheduling system.**

([GitHub][1])

---

## Next Topic → Controlled vs Uncontrolled Components & Forms

This is another **very frequently asked React interview topic**. We'll cover `value` vs `defaultValue`, controlled/uncontrolled inputs, refs, `onChange`, form state, why React's `onChange` behaves differently from raw DOM expectations, and the internal path from typing in an `<input>` to a React state update.

[1]: https://github.com/facebook/react/blob/main/packages/react-dom-bindings/src/events/DOMPluginEventSystem.js "react/packages/react-dom-bindings/src/events/DOMPluginEventSystem.js at main · react/react · GitHub"
[2]: https://github.com/facebook/react/blob/main/packages/react-dom-bindings/src/events/ReactDOMEventListener.js "react/packages/react-dom-bindings/src/events/ReactDOMEventListener.js at main · react/react · GitHub"
[3]: https://react.dev/reference/react-dom/components/common?utm_source=chatgpt.com "Common components (e.g. <div>) – React"
[4]: https://github.com/facebook/react/blob/main/packages/react-dom-bindings/src/events/SyntheticEvent.js "react/packages/react-dom-bindings/src/events/SyntheticEvent.js at main · react/react · GitHub"
[5]: https://github.com/facebook/react/blob/main/packages/react-dom-bindings/src/events/plugins/SimpleEventPlugin.js "react/packages/react-dom-bindings/src/events/plugins/SimpleEventPlugin.js at main · react/react · GitHub"
[6]: https://react.dev/learn/responding-to-events?utm_source=chatgpt.com "Responding to Events – React"

