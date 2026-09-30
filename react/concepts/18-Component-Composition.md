# Topic 18 — Component Composition & Patterns

This is a very important React topic for interviews because it answers a broader architectural question:

> **How do we build reusable React components without creating tightly coupled components or huge components full of conditional logic?**

The key idea is:

> **React favors composition: build larger components by combining smaller components and passing behavior/data into them.**

React's modern documentation emphasizes composing components, passing JSX through `children`, lifting state when components need coordination, and using custom Hooks for sharing stateful logic. ([React][1])

We will cover:

```text
18. Component Composition & Patterns
 │
 ├── Composition vs Inheritance
 ├── children
 ├── Props as slots
 ├── Passing elements vs components
 ├── Render Props
 ├── Compound Components
 ├── Higher-Order Components
 ├── Custom Hooks
 ├── Logic vs UI composition
 └── Choosing the right pattern
```

---

# 1. What is composition?

Suppose you want a page:

```jsx
<Page>
  <Header />
  <Sidebar />
  <Content />
</Page>
```

You don't create:

```text
MegaPage
 ├── header logic
 ├── sidebar logic
 ├── content logic
 ├── user logic
 ├── navigation logic
 ├── ...
```

Instead:

```text
Page
 ├── Header
 ├── Sidebar
 └── Content
```

Each component handles a particular concern.

That's composition.

React's component model is fundamentally based on assembling smaller components into larger UI structures. ([React][2])

---

# 2. Composition vs inheritance

This is one of the most common interview questions:

> **"Why does React prefer composition over inheritance?"**

Let's understand the problem first.

Suppose we have:

```text
BaseComponent
     │
     ├── AdminComponent
     ├── UserComponent
     └── GuestComponent
```

You might attempt to encode UI reuse using class inheritance.

```jsx
class BaseButton extends React.Component {
  // ...
}

class DangerButton extends BaseButton {
  // ...
}
```

This can create tight coupling:

```text
DangerButton
      ↓
BaseButton
      ↓
BaseButton implementation details
```

Changes in the base class can affect many subclasses.

Composition instead says:

```jsx
function Button({ children, className }) {
  return (
    <button className={className}>
      {children}
    </button>
  );
}
```

Then:

```jsx
<Button className="danger">
  Delete
</Button>
```

Or:

```jsx
<Button className="success">
  Save
</Button>
```

The component is reusable because you **configure it**, rather than subclassing it.

React's old documentation explicitly advocated composing components rather than relying on inheritance for UI reuse; current React docs focus heavily on composition and props/children. ([React][3])

---

# 3. Composition is basically "pass dependencies in"

Think of a component as:

```text
Component
   ↓
needs some UI/data/behavior
```

Instead of making the component know how to create everything itself:

```text
Component
 ├── creates header
 ├── creates footer
 ├── creates navigation
 └── creates content
```

you can pass those pieces:

```jsx
<Layout
  header={<Header />}
  footer={<Footer />}
/>
```

Now:

```text
Layout
 ├── receives header
 └── receives footer
```

This is dependency injection at the UI level.

---

# 4. `children` is the simplest composition mechanism

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
  <Avatar />
</Card>
```

Conceptually:

```text
<Card>
   <Avatar />
</Card>
```

becomes a React element whose `children` prop contains the nested React node.

The parent receives:

```js
props.children
```

React's documentation describes `children` as the React nodes nested inside a component and commonly recommends this pattern for wrappers such as cards, panels, and layouts. ([React][1])

---

# 5. Think of `children` as a slot

A useful mental model:

```text
Card

┌─────────────────────┐
│      Card           │
│                     │
│     [ children ]    │
│                     │
└─────────────────────┘
```

The component provides the structure.

The parent supplies the content.

So:

```jsx
<Card>
  <Avatar />
</Card>
```

means roughly:

```text
Card
 └── slot
      └── Avatar
```

This is one of the simplest and most powerful React composition patterns.

---

# 6. Why `children` reduces coupling

Badly coupled version:

```jsx
function UserCard() {
  return (
    <div className="card">
      <img />
      <h2 />
      <p />
    </div>
  );
}
```

Now the wrapper knows everything about the content.

Composition:

```jsx
function Card({ children }) {
  return (
    <div className="card">
      {children}
    </div>
  );
}
```

Then:

```jsx
<Card>
  <UserInfo />
</Card>
```

or:

```jsx
<Card>
  <ProductInfo />
</Card>
```

or:

```jsx
<Card>
  <OrderInfo />
</Card>
```

The wrapper doesn't need to understand those components.

React's docs describe this as allowing a wrapper to remain unaware of what its children actually render. ([React][1])

---

# 7. `children` can be anything renderable

`children` isn't necessarily one component.

It can be:

```jsx
<Card>
  Hello
</Card>
```

or:

```jsx
<Card>
  <h1>Hello</h1>
  <p>World</p>
</Card>
```

or:

```jsx
<Card>
  {items.map(item => (
    <Item key={item.id} item={item} />
  ))}
</Card>
```

It can also be arrays, fragments, portals, strings, numbers, and empty nodes such as `null`/`undefined`. ([React][4])

---

# 8. Multiple slots

A single `children` slot isn't always enough.

Suppose:

```jsx
function Dialog({
  title,
  footer,
  children
}) {
  return (
    <div className="dialog">
      <header>{title}</header>

      <main>{children}</main>

      <footer>{footer}</footer>
    </div>
  );
}
```

Usage:

```jsx
<Dialog
  title={<h2>Delete user?</h2>}
  footer={
    <>
      <button>Cancel</button>
      <button>Delete</button>
    </>
  }
>
  <p>Are you sure?</p>
</Dialog>
```

Now the component has three conceptual slots:

```text
Dialog
 ├── title
 ├── children
 └── footer
```

This is often called a **slot pattern**.

---

# 9. Props can carry JSX

This is an important realization:

```jsx
<Dialog
  footer={<FooterButtons />}
/>
```

Here:

```text
footer
```

is simply a prop whose value happens to be a React element.

There is nothing magical about it.

You can pass:

```text
string
number
object
array
function
React element
React node
```

through props, as appropriate.

React's docs explicitly note that props can contain any JavaScript value, including objects, arrays, and functions, and that nested JSX is passed through `children`. ([React][1])

---

# 10. React element vs component type

This distinction is extremely important.

### Passing an element

```jsx
<Layout
  header={<Header />}
/>
```

You're passing:

```text
React element
```

The parent has already created the element description.

### Passing a component type

```jsx
<Layout
  header={Header}
/>
```

You're passing:

```text
component function/type
```

The `Layout` component can later render:

```jsx
const Header = props.header;

return <Header />;
```

These are fundamentally different.

---

# 11. Why does the distinction matter?

Suppose:

```jsx
function Layout({ header }) {
  return (
    <div>
      {header}
    </div>
  );
}
```

Usage:

```jsx
<Layout header={<Header />} />
```

Here `Layout` receives:

```text
header = React element
```

It doesn't invoke `Header`.

The element already describes:

```text
type = Header
props = ...
```

By contrast:

```jsx
<Layout header={Header} />
```

receives the actual component function.

Then:

```jsx
function Layout({ header: Header }) {
  return <Header />;
}
```

creates the element during `Layout`'s render.

This difference becomes especially important with **state identity, props, and component ownership**.

---

# 12. Passing an element can preserve ownership boundaries

Example:

```jsx
function App() {
  const [count, setCount] = useState(0);

  const content = <Child count={count} />;

  return <Wrapper content={content} />;
}
```

`Wrapper` receives the already-created React element.

The conceptual tree is still:

```text
App
 └── Wrapper
      └── Child
```

The fact that `Wrapper` didn't itself write:

```jsx
<Child />
```

doesn't mean `Child` isn't part of the eventual React tree.

This is one reason element composition is so powerful.

---

# 13. Function-as-child / render prop

Now composition becomes more dynamic.

Instead of:

```jsx
<DataProvider>
  <Display />
</DataProvider>
```

you can pass a function:

```jsx
<DataProvider>
  {data => <Display data={data} />}
</DataProvider>
```

For example:

```jsx
function MouseTracker({ children }) {
  const [position, setPosition] = useState({
    x: 0,
    y: 0
  });

  // track mouse...

  return children(position);
}
```

Usage:

```jsx
<MouseTracker>
  {position => (
    <p>
      {position.x}, {position.y}
    </p>
  )}
</MouseTracker>
```

This is a **render prop**.

React's documentation defines a render prop as a function prop that tells a component how to render some UI; it is just a normal function prop with a rendering purpose. ([React][5])

---

# 14. Why were render props useful?

Imagine a component knows:

```text
where the mouse is
```

but doesn't know:

```text
what UI should represent that
```

So the component owns the logic:

```text
mouse tracking
```

and the consumer owns the presentation:

```text
how to display mouse position
```

Diagram:

```text
MouseTracker
    │
    │ data
    ↓
render prop
    │
    ↓
consumer-defined UI
```

This is separation of:

```text
behavior
+
presentation
```

---

# 15. Render prop with explicit prop

Instead of children:

```jsx
<MouseTracker>
  {position => <Cursor position={position} />}
</MouseTracker>
```

you could write:

```jsx
<MouseTracker
  render={position => (
    <Cursor position={position} />
  )}
/>
```

Implementation:

```jsx
function MouseTracker({ render }) {
  const position = useMousePosition();

  return render(position);
}
```

The important thing:

```text
render
```

is just:

```text
a function prop
```

There is no special React primitive called "render prop."

---

# 16. Render props are a form of inversion of control

This is a useful advanced concept.

Normally:

```text
Component decides what UI to render
```

With a render prop:

```text
Component provides data/behavior
        ↓
Consumer decides what UI to render
```

So control over rendering is inverted.

```text
Normal:

Component
   ↓
UI


Render prop:

Component
   ↓
data
   ↓
consumer function
   ↓
UI
```

---

# 17. Render prop example: data fetching

Conceptually:

```jsx
function DataProvider({ children }) {
  const data = ...;
  const loading = ...;
  const error = ...;

  return children({
    data,
    loading,
    error
  });
}
```

Consumer:

```jsx
<DataProvider>
  {({ data, loading, error }) => {
    if (loading) return <Spinner />;
    if (error) return <Error />;
    return <Table data={data} />;
  }}
</DataProvider>
```

The provider owns:

```text
fetching logic
```

The consumer owns:

```text
rendering decisions
```

---

# 18. The problem with render props

Suppose:

```jsx
<DataProvider>
  {data => <Display data={data} />}
</DataProvider>
```

That function is generally newly created during the parent's render.

This can affect optimization strategies:

```text
new function identity
       ↓
potentially more work
```

and deeply nested render-prop patterns can make component trees harder to read.

Hooks largely replaced many use cases for render props and HOCs because they let logic be reused **without adding wrapper components or extra render-prop nesting**. React's own current materials explicitly describe Hooks as a replacement for many earlier logic-reuse patterns. ([React][6])

---

# 19. Compound Components

Now we get to one of the most useful component-library patterns.

Think about:

```jsx
<Tabs>
  <Tabs.List>
    <Tabs.Tab>Home</Tabs.Tab>
    <Tabs.Tab>Profile</Tabs.Tab>
  </Tabs.List>

  <Tabs.Panel>
    Home content
  </Tabs.Panel>

  <Tabs.Panel>
    Profile content
  </Tabs.Panel>
</Tabs>
```

This is a **compound component** API.

The pieces work together as a coordinated group.

---

# 20. Why compound components?

We want:

```text
Flexible composition
+
shared behavior
+
simple API
```

Instead of:

```jsx
<Tabs
  tabs={[
    {
      title: "Home",
      content: ...
    }
  ]}
/>
```

the consumer writes structure that looks like the actual UI:

```jsx
<Tabs>
  <Tabs.List>
    ...
  </Tabs.List>

  <Tabs.Panel>
    ...
  </Tabs.Panel>
</Tabs>
```

This gives consumers more control over markup.

---

# 21. Compound components commonly use Context

Suppose:

```jsx
const TabsContext = createContext(null);
```

Then:

```jsx
function Tabs({ children }) {
  const [activeTab, setActiveTab] =
    useState("home");

  const value = {
    activeTab,
    setActiveTab
  };

  return (
    <TabsContext value={value}>
      {children}
    </TabsContext>
  );
}
```

Then:

```jsx
function Tab({ id, children }) {
  const {
    activeTab,
    setActiveTab
  } = useContext(TabsContext);

  return (
    <button
      onClick={() => setActiveTab(id)}
      aria-selected={activeTab === id}
    >
      {children}
    </button>
  );
}
```

Now:

```text
Tabs
 │
 ├── Tabs.List
 │
 ├── Tabs.Tab
 │
 └── Tabs.Panel
       │
       └── useContext()
```

Context provides the shared coordination mechanism.

This connects directly to Topic 13.

---

# 22. Compound components are composition + shared state

Think:

```text
Compound component
       │
       ├── Composition
       │      ↓
       │   consumer controls structure
       │
       └── Context/state
              ↓
          pieces coordinate
```

This is a very common pattern in:

```text
Tabs
Accordion
Menu
Dropdown
Select
Dialog
Form controls
```

libraries.

---

# 23. Compound component internals

Suppose:

```jsx
<Tabs>
  <Tabs.Tab id="home" />
  <Tabs.Tab id="profile" />
</Tabs>
```

React still builds normal elements and Fibers:

```text
Tabs Fiber
  │
  ├── Tabs.Tab Fiber
  │
  └── Tabs.Tab Fiber
```

The special behavior comes from:

```text
Context
+
shared state
+
component APIs
```

There isn't a special internal React "compound component" feature.

This is an architectural pattern built using normal React primitives.

---

# 24. Higher-Order Components (HOC)

A Higher-Order Component is different.

A HOC is a function that:

> **takes a component and returns another component.**

Conceptually:

```js
const EnhancedComponent =
  withFeature(Component);
```

where:

```js
function withFeature(Component) {
  return function Enhanced(props) {
    // extra behavior
    return <Component {...props} />;
  };
}
```

The key signature is:

```text
Component
   ↓
HOC
   ↓
new Component
```

Historically, HOCs were an important React pattern for reusing component logic. The React legacy docs describe HOCs this way, but those docs are no longer updated; Hooks are the preferred modern mechanism for many logic-reuse scenarios. ([React][3])

---

# 25. HOC example

Suppose:

```jsx
function UserProfile({ user }) {
  return <h1>{user.name}</h1>;
}
```

HOC:

```jsx
function withUser(Component) {
  return function UserComponent(props) {
    const user = useUser();

    return (
      <Component
        {...props}
        user={user}
      />
    );
  };
}
```

Then:

```jsx
const UserProfileWithUser =
  withUser(UserProfile);
```

Now:

```text
UserProfileWithUser
       ↓
     User
       ↓
UserProfile
```

The wrapper injects behavior/data.

---

# 26. What actually happens to the Fiber tree with a HOC?

This is the interesting internals part.

You originally had:

```jsx
<UserProfile />
```

Now you render:

```jsx
<UserProfileWithUser />
```

where the HOC returns:

```jsx
function UserComponent(props) {
  return <UserProfile {...props} />;
}
```

The Fiber tree becomes conceptually:

```text
UserComponent Fiber
       │
       ↓
UserProfile Fiber
       │
       ↓
DOM
```

You've added another component layer.

That means HOCs increase the React component hierarchy.

This is one reason deeply nested HOCs can make debugging and component trees harder to understand.

---

# 27. HOC does not modify the original component

This is an important HOC principle.

Bad mental model:

```text
withUser(UserProfile)
     ↓
modifies UserProfile
```

Better:

```text
UserProfile
     │
     │ passed to
     ↓
withUser()
     │
     ↓
new wrapper component
```

The original component remains unchanged.

Conceptually:

```js
const Enhanced = withUser(UserProfile);
```

does not mutate:

```js
UserProfile
```

It creates another component.

---

# 28. HOC should pass unrelated props through

Suppose:

```jsx
const Enhanced = withUser(Button);
```

and:

```jsx
<Enhanced
  color="red"
  size="large"
/>
```

The HOC should generally forward props:

```jsx
function withUser(Component) {
  return function Wrapper(props) {
    const user = useUser();

    return (
      <Component
        {...props}
        user={user}
      />
    );
  };
}
```

Otherwise:

```text
color
size
onClick
className
...
```

could disappear.

This is an important HOC implementation detail.

---

# 29. HOC prop collision

Suppose:

```jsx
<Enhanced user="fake" />
```

and the HOC does:

```jsx
<Component
  {...props}
  user={realUser}
/>
```

Then:

```text
realUser wins
```

because it appears later.

But:

```jsx
<Component
  user={realUser}
  {...props}
/>
```

would make:

```text
props.user
```

win.

So HOC design should clearly define which props it owns.

---

# 30. HOC composition

You can stack HOCs:

```jsx
const Enhanced =
  withLoading(
    withUser(
      withPermissions(
        Component
      )
    )
  );
```

Conceptually:

```text
withLoading
   ↓
withUser
   ↓
withPermissions
   ↓
Component
```

The Fiber tree can become:

```text
LoadingWrapper
   ↓
UserWrapper
   ↓
PermissionWrapper
   ↓
Component
```

This is sometimes called **wrapper hell** when taken too far.

Hooks can often flatten this:

```jsx
function Component() {
  const user = useUser();
  const permissions = usePermissions();
  const loading = useLoading();

  // ...
}
```

React's modern guidance reflects this shift toward Hooks for reusable logic. ([React][7])

---

# 31. HOC and refs

This is a classic problem.

Suppose:

```jsx
const Enhanced =
  withFeature(Button);
```

and the parent does:

```jsx
<Enhanced ref={buttonRef} />
```

Where should the ref go?

Ordinary props forwarding:

```jsx
<Component {...props} />
```

doesn't automatically forward `ref` in the same way a normal prop does.

Historically this led to:

```text
forwardRef
```

being important for HOCs.

Modern React 19 also allows `ref` as a prop for function components, which changes how some ref-forwarding designs can be written. This is one of the things we'll cover in Topic 17 in detail.

---

# 32. HOC and static properties

Suppose your original component has:

```js
UserProfile.someStatic = ...
```

Then:

```js
const Enhanced =
  withUser(UserProfile);
```

does not automatically inherit:

```text
someStatic
```

because:

```text
Enhanced !== UserProfile
```

They're different component objects.

This creates an old HOC issue:

```text
static property hoisting
```

Libraries historically used utilities such as `hoist-non-react-statics` to copy non-React static properties.

You should understand the issue, even though you won't usually build HOC-heavy modern application code.

---

# 33. HOC and `displayName`

A wrapper can make DevTools harder to read.

Instead of:

```text
UserProfile
```

you may see:

```text
WithUser(UserProfile)
```

A common pattern is:

```js
Wrapper.displayName =
  `withUser(${getDisplayName(Component)})`;
```

Again, this is about developer experience, not React's rendering correctness.

---

# 34. HOC vs custom Hook

This is an extremely likely interview question.

### HOC

```text
Component
   ↓
HOC
   ↓
new Component
```

Adds a component layer.

### Custom Hook

```text
Component
   ↓
useUser()
usePermissions()
useWhatever()
```

Shares logic without necessarily adding a wrapper component.

React's modern documentation says custom Hooks let you share **stateful logic**, not state itself, and that Hooks are a modern mechanism for reusing behavior between components. ([React][7])

---

# 35. Custom Hooks are not shared state

Consider:

```js
function useCounter() {
  const [count, setCount] = useState(0);

  return {
    count,
    increment: () => setCount(c => c + 1)
  };
}
```

Now:

```jsx
function A() {
  const counter = useCounter();
}

function B() {
  const counter = useCounter();
}
```

Do A and B share the same counter?

**No.**

They each execute the Hook as part of their own component render.

So:

```text
A
 └── useCounter()
       └── state A

B
 └── useCounter()
       └── state B
```

Custom Hooks share **logic**, not the actual state instance. React's documentation explicitly makes this distinction. ([React][8])

---

# 36. Why does that work?

Remember our Hook internals:

```text
Component Fiber
    ↓
Hook linked list
    ↓
useState/useEffect/...
```

When A calls:

```js
useCounter()
```

the Hook calls happen within A's render:

```text
A Fiber
 └── Hooks
```

When B calls:

```js
useCounter()
```

they belong to B's Fiber:

```text
B Fiber
 └── Hooks
```

So the custom Hook is just a way of composing existing Hooks.

This is one of the most important internal facts about custom Hooks.

---

# 37. A custom Hook is not a component

A Hook:

```js
function useUser() {
  const [user, setUser] = useState(null);
  return user;
}
```

isn't:

```jsx
<UseUser />
```

You don't render it.

Instead:

```jsx
function Profile() {
  const user = useUser();
}
```

A custom Hook's code becomes part of the calling component's rendering logic.

React's docs explicitly explain that custom Hook code re-runs when its component re-renders and must follow the Rules of Hooks. ([React][7])

---

# 38. Custom Hook composition

A custom Hook can use other Hooks:

```js
function useOnlineStatus() {
  const [online, setOnline] = useState(...);

  useEffect(() => {
    // subscribe
  }, []);

  return online;
}
```

Then:

```js
function useUserStatus(userId) {
  const online = useOnlineStatus();
  const user = useUser(userId);

  return {
    user,
    online
  };
}
```

So Hooks can form a logic graph:

```text
Component
    ↓
useUserStatus
    ├── useOnlineStatus
    │      ├── useState
    │      └── useEffect
    │
    └── useUser
           └── ...
```

React explicitly describes custom Hooks as composable and says you can pass reactive values between them. ([React][7])

---

# 39. Rules of Hooks become especially important

Because custom Hooks are just functions containing Hooks:

```js
function useUser() {
  const [user, setUser] = useState(null);
}
```

you must follow Hook rules.

Don't:

```js
function useUser(condition) {
  if (condition) {
    const [user, setUser] = useState(null);
  }
}
```

Why?

Because the calling component's Hook sequence must remain stable.

We've already studied this internally:

```text
Fiber
 └── Hook #1
 └── Hook #2
 └── Hook #3
```

If the order changes:

```text
render 1:
Hook #1 = useState
Hook #2 = useEffect

render 2:
Hook #1 = useEffect
Hook #2 = useState
```

React cannot correctly associate state with the Hook calls.

The current React Rules of Hooks documentation explicitly requires Hooks to be called at the top level of components and custom Hooks, not inside conditions, loops, or nested functions. ([React][9])

---

# 40. Composition is not just visual composition

This is an important broader idea.

There are two major kinds of composition:

### UI composition

```text
Card
 +
Avatar
 +
Button
```

using:

```text
children
props
slots
render props
compound components
```

### Logic composition

```text
useUser
 +
usePermissions
 +
useOnlineStatus
```

using:

```text
custom Hooks
```

So:

```text
React composition
        │
        ├── UI composition
        │
        └── Logic composition
```

This is why the term "composition" appears everywhere in React architecture.

---

# 41. The evolution of React patterns

Historically, React applications often used:

```text
Mixin
 ↓
Higher-Order Component
 ↓
Render Prop
 ↓
Hooks
```

This is not a strict replacement chain where each newer mechanism makes every earlier one invalid.

Rather:

```text
Different patterns solved different problems
```

But Hooks removed a lot of the need for:

```text
HOCs
render props
mixins
```

when the goal was **sharing stateful logic**.

React's own historical materials explain that Hooks were motivated partly by the difficulty of sharing stateful logic and that Hooks could replace patterns such as render props and HOCs. ([React][10])

---

# 42. Mixin — historical interview knowledge

You may encounter old code like:

```js
const MyComponent =
  React.createClass({
    mixins: [...]
  });
```

Mixins were an old technique for sharing component behavior.

They had problems such as:

```text
implicit dependencies
name collisions
difficult composition
behavior spread across unrelated code
```

Modern React doesn't use mixins.

For interviews, know:

```text
Mixins → historical
HOCs → still encountered
Render props → still encountered
Hooks → modern logic reuse
```

---

# 43. Composition vs inheritance: the deeper reason

The deeper reason isn't just:

> "Composition is preferred."

It is about **coupling**.

Inheritance creates:

```text
strong structural relationship
```

between base and derived class.

Composition creates:

```text
looser behavioral relationship
```

through props and component boundaries.

For example:

```jsx
<Button icon={<SaveIcon />}>
  Save
</Button>
```

doesn't require:

```text
SaveButton extends Button
```

The component gets what it needs through composition.

That means the consumer can vary:

```text
icon
label
children
behavior
styles
callbacks
```

without creating a new subclass for each combination.

---

# 44. The combinatorial explosion problem

Suppose you have:

```text
Button
 ├── IconButton
 ├── LoadingButton
 ├── DangerButton
 ├── PrimaryButton
 ├── LargeButton
 └── SmallButton
```

Now what if you need:

```text
Loading + Danger + Icon + Large
```

Inheritance can explode:

```text
LoadingDangerIconLargeButton
```

Composition handles combinations more naturally:

```jsx
<Button
  variant="danger"
  size="large"
  loading
  icon={<TrashIcon />}
>
  Delete
</Button>
```

The exact API design can vary, but composition allows independent dimensions to be combined.

---

# 45. Component specialization through props

Composition doesn't mean every component has to be generic.

You can specialize:

```jsx
function Button({ variant, children }) {
  // ...
}
```

Then:

```jsx
<Button variant="danger">
  Delete
</Button>
```

or create a specialized wrapper:

```jsx
function DeleteButton(props) {
  return (
    <Button
      variant="danger"
      {...props}
    />
  );
}
```

This is itself a form of composition.

---

# 46. Wrapper components

A common pattern:

```jsx
function Centered({ children }) {
  return (
    <div className="centered">
      {children}
    </div>
  );
}
```

Usage:

```jsx
<Centered>
  <LoginForm />
</Centered>
```

This is sometimes called a wrapper or layout component.

It has no special React implementation.

It's simply:

```text
component receives children
        ↓
renders them inside structure
```

---

# 47. Layout components

Examples:

```jsx
<PageLayout
  header={<Header />}
  sidebar={<Sidebar />}
>
  <Dashboard />
</PageLayout>
```

This is especially powerful because layout concerns and page concerns can remain separate.

```text
PageLayout
 ├── header
 ├── sidebar
 └── main content
```

while:

```text
Dashboard
 ├── charts
 ├── metrics
 └── table
```

doesn't need to know how the page shell works.

---

# 48. "Slots" can be more explicit

Instead of:

```jsx
<PageLayout>
  <Header />
  <Sidebar />
  <Dashboard />
</PageLayout>
```

you may choose:

```jsx
<PageLayout
  header={<Header />}
  sidebar={<Sidebar />}
  content={<Dashboard />}
/>
```

Advantages:

```text
explicit slots
easy positioning
clear API
```

Potential downside:

```text
many slot props
```

can become verbose.

So this is an API design decision, not a hard React rule.

---

# 49. Don't manipulate `children` unnecessarily

React provides:

```js
Children.map(...)
Children.count(...)
Children.toArray(...)
```

but modern docs explicitly caution that using `Children` to manipulate arbitrary nested JSX can be fragile and uncommon. ([React][5])

For example, this can be brittle:

```jsx
function Tabs({ children }) {
  return Children.map(
    children,
    child => cloneElement(child, {
      ...
    })
  );
}
```

Why?

Because `children` can contain:

```text
fragments
arrays
conditional nodes
nested structures
null
components
```

and your assumptions about its structure can become fragile.

Often a better architecture uses:

```text
explicit props
context
compound component APIs
render props
```

depending on the goal.

---

# 50. Composition vs `cloneElement`

You may encounter:

```jsx
cloneElement(child, {
  active: true
})
```

This allows a component to clone received elements with additional props.

Historically this was used for compound components.

Example:

```jsx
function Tabs({ children }) {
  return Children.map(children, child =>
    cloneElement(child, {
      active: ...
    })
  );
}
```

But this tightly couples the parent to the exact shape/types of children.

A Context-based compound component often scales better:

```text
Tabs
 └── Context
      ├── Tab
      └── Panel
```

We won't make `cloneElement` a central pattern because React's current docs emphasize composition and warn that manipulating arbitrary `children` can be fragile. ([React][5])

---

# 51. Render prop vs children

These can look similar:

### Children as static UI

```jsx
<DataProvider>
  <Display />
</DataProvider>
```

### Function as children

```jsx
<DataProvider>
  {data => <Display data={data} />}
</DataProvider>
```

The difference:

```text
React element
vs
function
```

The function allows the provider to supply dynamic data to the rendering code.

So:

```text
children = UI
```

versus:

```text
children = rendering function
```

---

# 52. HOC vs render prop

Both can reuse logic.

### HOC

```js
const Enhanced =
  withUser(Profile);
```

The HOC injects data:

```text
withUser
  ↓
Enhanced component
  ↓
Profile
```

### Render prop

```jsx
<UserProvider>
  {user => <Profile user={user} />}
</UserProvider>
```

The consumer controls the rendering.

So:

```text
HOC
 → logic wrapped around component

Render prop
 → logic provides data to rendering function
```

---

# 53. HOC vs custom Hook

Modern equivalent:

### HOC

```jsx
function Profile(props) {
  return ...
}

export default withUser(Profile);
```

### Hook

```jsx
function Profile() {
  const user = useUser();

  return ...
}
```

With the Hook:

```text
no wrapper Fiber
no injected prop
logic lives directly in component
```

This usually produces a flatter component hierarchy for this use case.

---

# 54. Custom Hook vs Context

Another common confusion.

Context:

```jsx
const user = useContext(UserContext);
```

solves:

```text
how to distribute a value
```

Custom Hook:

```jsx
const user = useUser();
```

solves:

```text
how to package reusable logic/API
```

The custom Hook may itself use Context:

```jsx
function useUser() {
  return useContext(UserContext);
}
```

Then:

```text
Context
  ↓
stores/provides value

Custom Hook
  ↓
encapsulates how consumers access it
```

This is a very common library pattern.

---

# 55. Component composition + Context

Compound components frequently combine:

```text
composition
+
context
+
custom Hooks
```

For example:

```jsx
function useTabsContext() {
  const context = useContext(TabsContext);

  if (!context) {
    throw new Error(
      "Tabs components must be inside <Tabs>"
    );
  }

  return context;
}
```

Then:

```jsx
function Tab({ id, children }) {
  const { activeTab, setActiveTab } =
    useTabsContext();

  // ...
}
```

This creates a clean API:

```jsx
<Tabs>
  <Tabs.Tab />
  <Tabs.Panel />
</Tabs>
```

while hiding the Context implementation from consumers.

---

# 56. Important architecture principle: separate public API from implementation

A good reusable component shouldn't force users to understand:

```text
internal state
context structure
effect implementation
event handling
DOM details
```

Example:

```jsx
<Tabs defaultValue="home">
  <Tabs.List>
    <Tabs.Trigger value="home">
      Home
    </Tabs.Trigger>

    <Tabs.Trigger value="settings">
      Settings
    </Tabs.Trigger>
  </Tabs.List>

  <Tabs.Content value="home">
    ...
  </Tabs.Content>

  <Tabs.Content value="settings">
    ...
  </Tabs.Content>
</Tabs>
```

The consumer thinks in terms of:

```text
Tabs
Trigger
Content
```

The implementation might use:

```text
useState
Context
custom Hooks
refs
effects
```

The implementation is hidden.

That's excellent component API design.

---

# 57. Composition and controlled/uncontrolled APIs

Remember Topic 16.

A compound component might support both:

```jsx
<Tabs defaultValue="home">
```

and:

```jsx
<Tabs
  value={activeTab}
  onValueChange={setActiveTab}
/>
```

So:

```text
Composition
+
controlled/uncontrolled state
+
context
```

can form a powerful reusable component API.

This is how many sophisticated UI component libraries are designed.

---

# 58. Composition and state ownership

Remember Topic 14 and Topic 13.

Suppose:

```jsx
<Tabs>
  <Tabs.Tab />
  <Tabs.Tab />
</Tabs>
```

Where does active-tab state live?

Usually:

```text
Tabs
 ↓
owns state
 ↓
Context
 ↓
children
```

So:

```text
composition
    ↓
shared parent owns coordination
    ↓
Context distributes it
```

That is basically **lifting state up + Context + compound components**.

React's current guidance emphasizes choosing the component that owns each unique piece of state and moving shared state to a common parent when components need to coordinate. ([React][11])

---

# 59. Composition and purity

There is another important connection to React internals.

All these component patterns still need to obey React's purity rules.

For example:

```jsx
function Wrapper({ children }) {
  // render should calculate UI
  return <div>{children}</div>;
}
```

Don't do:

```jsx
function Wrapper({ children }) {
  globalCounter++;

  return <div>{children}</div>;
}
```

Why?

React may render components multiple times as part of its rendering model, and render must remain pure/idempotent. ([React][12])

So composition doesn't change the fundamental rendering contract.

---

# 60. A major interview question: "Which pattern should I use?"

A useful decision framework:

```text
Need to compose UI?
        ↓
children / slots

Need consumer-controlled rendering?
        ↓
render prop

Need coordinated sibling components?
        ↓
shared parent state
        +
possibly Context

Need reusable stateful logic?
        ↓
custom Hook

Need to augment an existing component
without modifying it?
        ↓
HOC
```

HOCs remain important for understanding existing code and library patterns, but for new application code, Hooks are generally the modern mechanism for sharing stateful logic. ([React][7])

---

# 61. One example using several patterns together

Let's design a dialog.

Public API:

```jsx
<Dialog>
  <Dialog.Title>
    Delete account
  </Dialog.Title>

  <Dialog.Content>
    Are you sure?
  </Dialog.Content>

  <Dialog.Actions>
    <button>Cancel</button>
    <button>Delete</button>
  </Dialog.Actions>
</Dialog>
```

Internally:

```text
Dialog
 ├── owns state
 │
 ├── Context Provider
 │
 ├── Title
 │    └── useDialogContext()
 │
 ├── Content
 │    └── useDialogContext()
 │
 └── Actions
      └── useDialogContext()
```

This uses:

```text
Component composition
+
children
+
compound components
+
Context
+
custom Hook
```

Yet the consumer sees a simple declarative API.

---

# 62. What React itself sees

The JSX:

```jsx
<Dialog>
  <Dialog.Title>
    Delete account
  </Dialog.Title>

  <Dialog.Content>
    Are you sure?
  </Dialog.Content>
</Dialog>
```

produces normal React elements.

Conceptually:

```text
Dialog Element
    │
    ├── Dialog.Title Element
    │
    └── Dialog.Content Element
```

During reconciliation:

```text
Dialog Fiber
   │
   ├── Title Fiber
   │
   └── Content Fiber
```

No special "composition engine" exists.

Composition is primarily an **API/design pattern built on top of React's normal element/Fiber model**.

That is an important internals answer.

---

# 63. Composition is fundamentally about controlling boundaries

A component boundary answers:

```text
What does this component know?
What does it own?
What does it receive?
What does it expose?
```

Good composition tends toward:

```text
small knowledge boundary
clear input/output
independent implementation
reusable behavior
```

Bad composition often produces:

```text
huge prop interfaces
deep wrapper trees
hidden dependencies
components that know too much about children
```

So composition isn't simply:

> "Put components inside other components."

It is really **designing boundaries between responsibilities**.

---

# 64. Common interview traps

### Trap 1: "React supports inheritance for component reuse."

Technically class components support JavaScript/`React.Component` inheritance, but React's component architecture emphasizes composition rather than inheritance for UI reuse. ([React][3])

---

### Trap 2: "`children` is just one React element."

No.

`children` is a React node and can be:

```text
one element
multiple elements
text
array
fragment
portal
null
...
```

([React][4])

---

### Trap 3: "A custom Hook shares state."

No.

It shares **stateful logic**.

Each component calling the Hook gets its own Hook state. ([React][8])

---

### Trap 4: "A render prop is a special React feature."

No.

It's simply a function passed as a prop, used by the component to decide what to render. ([React][5])

---

### Trap 5: "An HOC modifies the original component."

No.

It normally creates a new wrapper component around the original.

---

### Trap 6: "HOCs are obsolete."

Too strong.

They're an older but still relevant pattern, especially when reading existing React libraries/code. Hooks have replaced many HOC use cases for sharing stateful logic. ([React][6])

---

### Trap 7: "Composition means no props."

No.

Composition heavily uses props:

```text
children
slots
functions
elements
data
callbacks
```

---

# 65. Comparison table

| Pattern             | Main purpose                                   |                     Adds wrapper? | Modern relevance               |
| ------------------- | ---------------------------------------------- | --------------------------------: | ------------------------------ |
| `children`          | Compose UI                                     |                No special wrapper | Very high                      |
| Slot props          | Multiple composition points                    |                                No | Very high                      |
| Render prop         | Share behavior + consumer-controlled rendering |                       Usually yes | Medium                         |
| Compound components | Coordinated component family                   | No required wrapper beyond parent | Very high                      |
| HOC                 | Enhance/wrap existing component                |                               Yes | Legacy/common in existing code |
| Custom Hook         | Reuse stateful logic                           |              No component wrapper | Very high                      |
| Context             | Share values through subtree                   |                          Provider | Very high                      |

---

# 66. The evolution you should remember

```text
Composition
    ↓
children / props
    ↓
Render Props
    ↓
Higher-Order Components
    ↓
Hooks
```

But don't interpret this as:

```text
old = wrong
new = right
```

Instead:

```text
Different abstractions for different reuse problems
```

The big modern shift is:

> **When the problem is "I want to reuse stateful behavior," a custom Hook is often simpler than adding another component wrapper.** ([React][7])

---

# 67. Interview-ready answer: "What is composition?"

A strong answer:

> **"Composition in React means building complex components by combining smaller components and passing data, UI, or behavior through props rather than relying on inheritance. The simplest example is `children`, where a wrapper defines the structure and the parent supplies the content. More advanced forms include slot props, render props, and compound components. For sharing stateful logic specifically, custom Hooks provide a modern composition mechanism without adding another component layer."**

---

# 68. Interview-ready answer: "Composition vs inheritance?"

> **"React generally favors composition because it reduces coupling between reusable pieces. With composition, behavior and UI can be combined through props, children, Context, and Hooks without requiring a class hierarchy. Inheritance creates stronger dependencies between a base class and derived components, while composition lets components vary independently."**

---

# 69. Interview-ready answer: "HOC vs Hook?"

> **"A Higher-Order Component takes a component and returns another component, so it normally adds a wrapper to the React tree. A custom Hook is a function that composes Hooks and shares stateful logic with the calling component, without requiring an additional component wrapper. Hooks therefore replace many historical HOC use cases, although HOCs still matter when working with existing code or APIs designed around them."** ([React][7])

---

# 70. Interview-ready answer: "What are compound components?"

> **"Compound components are a component API pattern where several related components work together as one abstraction. A parent component usually owns shared state, and child components access that state through Context or other shared mechanisms. This allows consumers to control the structure while the component family coordinates its behavior."**

---

# 71. The deepest mental model

Think of React abstractions in two dimensions:

```text
                    REUSE
                      │
          ┌───────────┴───────────┐
          │                       │
       UI reuse              Logic reuse
          │                       │
          ↓                       ↓
 children / slots           custom Hooks
          │                       │
 render props               Context + Hooks
          │
 compound components
          │
 HOCs (historically)
```

And underneath all of them:

```text
                React Elements
                     ↓
                   Fiber
                     ↓
              reconciliation
                     ↓
                  commit
```

The patterns don't change the fundamental React engine.

They change **how we design component boundaries and distribute responsibility**.

---

# 72. Final picture

```text
                 COMPONENT COMPOSITION
                         │
        ┌────────────────┼────────────────┐
        │                │                │
       UI              Logic           Coordination
        │                │                │
        ↓                ↓                ↓
   children          Custom Hooks      Context
   slots             useState          shared state
   elements          useEffect
   render props       useContext
        │
        ↓
Compound Components
        │
        ↓
Flexible public API
```

And the principle underneath everything:

```text
Don't make one component know everything.

Compose:
    structure
    behavior
    state
    presentation
```

That is the essence of React component architecture.

### Next Topic → **19. Custom Hooks Internals**

We'll go much deeper specifically into **how custom Hooks work internally, why they don't have their own state, how nested Hooks map onto a Fiber's Hook linked list, Hook composition, Rules of Hooks, custom Hook API design, dependency handling, and how libraries build complex Hooks from smaller Hooks.**

[1]: https://react.dev/learn/passing-props-to-a-component?utm_source=chatgpt.com "Passing Props to a Component – React"
[2]: https://react.dev/learn?utm_source=chatgpt.com "Quick Start – React"
[3]: https://legacy.reactjs.org/?utm_source=chatgpt.com "React – A JavaScript library for building user interfaces"
[4]: https://react.dev/reference/react-dom/components/common?utm_source=chatgpt.com "Common components (e.g. <div>) – React"
[5]: https://react.dev/reference/react/Children?utm_source=chatgpt.com "Children – React"
[6]: https://uk.react.dev/blog/2025/04/23/react-labs-view-transitions-activity-and-more?utm_source=chatgpt.com "React Labs: View Transitions, Activity, and more – React"
[7]: https://react.dev/learn/reusing-logic-with-custom-hooks?utm_source=chatgpt.com "Reusing Logic with Custom Hooks – React"
[8]: https://mn.react.dev/learn/reusing-logic-with-custom-hooks?utm_source=chatgpt.com "Reusing Logic with Custom Hooks – React"
[9]: https://react.dev/warnings/invalid-hook-call-warning?utm_source=chatgpt.com "Rules of Hooks – React"
[10]: https://pl.legacy.reactjs.org/docs/hooks-intro.html?utm_source=chatgpt.com "Wprowadzenie do hooków – React"
[11]: https://react.dev/learn/sharing-state-between-components?utm_source=chatgpt.com "Sharing State Between Components – React"
[12]: https://react.dev/reference/rules/components-and-hooks-must-be-pure?utm_source=chatgpt.com "Components and Hooks must be pure – React"

