# Topic 5 — Reconciliation & the Diffing Algorithm

This is where the React concepts from the previous topics finally connect.

We previously had:

```text
JSX
  ↓
React elements
  ↓
Components
  ↓
render
```

Now the central question is:

> **When the new React element tree differs from the previous one, how does React decide what to keep, what to update, what to move, and what to delete?**

That process is broadly called **reconciliation**.

The important modern detail is that reconciliation is implemented as part of React's Fiber-based reconciler. React's current source has a dedicated `ReactChildFiber.js` responsible for reconciling children, including matching by keys and creating, reusing, moving, or deleting Fibers. ([GitHub][1])

---

# 1. Why do we need reconciliation?

Suppose your first render produces:

```jsx
<div>
    <h1>Hello</h1>
    <button>Click</button>
</div>
```

Later, state changes and the component returns:

```jsx
<div>
    <h1>Hello World</h1>
    <button>Click</button>
</div>
```

React needs to figure out:

```text
Old tree                         New tree

div                              div
├── h1 "Hello"          →        ├── h1 "Hello World"
└── button "Click"               └── button "Click"
```

Clearly, React doesn't need to destroy:

```text
button
```

and recreate it.

It only needs to update the relevant text.

So reconciliation answers:

```text
What existing work can I reuse?
What changed?
What must be inserted?
What must be deleted?
What must move?
```

---

# 2. Reconciliation is NOT simply "compare two full DOM trees"

This is an important interview correction.

The simplistic description:

> React creates a Virtual DOM, compares the old Virtual DOM to the new Virtual DOM, then updates the real DOM.

is useful for beginners, but isn't a good explanation of modern React internals.

A more accurate model is:

```text
previous Fiber tree
        +
new React element descriptions
        ↓
child reconciliation
        ↓
new/current Work-In-Progress Fibers
        ↓
flags / effects describing required work
        ↓
commit phase
        ↓
host environment
```

The current source shows `reconcileChildren` passing the previous child Fiber and the newly returned children into `reconcileChildFibers`. ([GitHub][2])

---

# 3. First important distinction: element vs Fiber

Suppose:

```jsx
<Button color="red" />
```

The JSX transformation produces an element description.

Conceptually:

```javascript
{
    type: Button,
    key: null,
    props: {
        color: "red"
    }
}
```

During reconciliation, React needs an internal persistent unit corresponding to that element.

That is a Fiber:

```text
React element
      ↓
Fiber
```

On an update, React often **reuses the existing Fiber by cloning it into the work-in-progress tree**, rather than creating a completely unrelated Fiber. The current source's `useFiber` calls `createWorkInProgress` and resets `index`/`sibling` for the new child position. ([GitHub][1])

This is one of the foundations of state preservation.

---

# 4. The matching problem

Suppose the previous children are:

```text
A
B
C
```

and the new children are:

```text
A
B
C
```

React obviously wants:

```text
old A → new A
old B → new B
old C → new C
```

But now consider:

```text
A
B
C
```

becoming:

```text
B
A
C
```

React needs to understand:

```text
old A → new A
old B → new B
old C → new C
```

while recognizing that:

```text
A and B changed positions
```

This is where **keys and identity** become critical.

---

# 5. The basic identity concept

For an element to be reused, React needs to determine that the new child corresponds to the old child.

For a simplified mental model, think:

```text
identity ≈ type + key + position/context
```

The exact rules vary by structure and element type, but two concepts are particularly important:

```text
type
key
```

For example:

```jsx
<User key="42" />
```

has:

```text
type = User
key  = "42"
```

React can use these when determining whether the old Fiber corresponds to the new element.

---

# 6. Same type: often reusable

Suppose:

```jsx
<div className="a" />
```

becomes:

```jsx
<div className="b" />
```

Same type:

```text
div → div
```

React can reuse the existing Fiber/host node and update its props.

The current `updateElement` logic checks whether the existing Fiber's `elementType` matches the new element type, and if so it calls `useFiber(current, element.props)` to create the work-in-progress version. ([GitHub][1])

Conceptually:

```text
old Fiber
   ↓
same identity/type
   ↓
reuse Fiber
   ↓
new props
```

---

# 7. Different type: replace

Now:

```jsx
<div />
```

becomes:

```jsx
<section />
```

Conceptually:

```text
old:
div

new:
section
```

React cannot simply treat these as the same host element.

The current reconciliation code instead creates a new Fiber when the old element type isn't compatible with the new type. ([GitHub][1])

So conceptually:

```text
div
 ↓
delete old

section
 ↓
create new
```

This also affects state below that subtree.

---

# 8. Component type changes

The same principle applies to components.

Suppose:

```jsx
<User />
```

becomes:

```jsx
<Admin />
```

At the same location:

```text
User → Admin
```

These are different component types.

React therefore treats them as different identities rather than preserving `User`'s subtree as though it were still the same component.

This is why switching between component types can reset state. React's documentation explains state preservation in terms of matching the tree structure and component identity. ([React][3])

---

# 9. Same type but different props

Suppose:

```jsx
<User name="Alice" />
```

becomes:

```jsx
<User name="Bob" />
```

The type is still:

```text
User
```

and, assuming identity otherwise matches, React can reuse the existing component Fiber with new props.

Conceptually:

```text
old:
User
  props.name = Alice

new:
User
  props.name = Bob
```

React does **not** create a brand-new component identity merely because the props changed.

The component renders again using the new props.

---

# 10. Same DOM element, changed props

Similarly:

```jsx
<input disabled={false} />
```

becomes:

```jsx
<input disabled={true} />
```

The host type remains:

```text
input
```

so React can keep the existing host Fiber/DOM node and apply the changed property during commit.

The reconciler creates/reuses Fibers; the host renderer is then responsible for applying the actual host update. ([GitHub][1])

---

# 11. Now let's discuss children

This is where reconciliation gets interesting.

Suppose:

```jsx
<div>
    <A />
    <B />
    <C />
</div>
```

The parent has three children.

React's child reconciler needs to match:

```text
old children
     ↓
new children
```

The current implementation has specialized logic for reconciling arrays, single elements, text nodes, fragments, portals, lazy nodes, and other supported child types. ([GitHub][1])

---

# 12. Why arrays are harder

Consider:

```text
Old:
A
B
C
```

New:

```text
X
A
B
C
```

A naive position-based comparison sees:

```text
A → X
B → A
C → B
new C
```

which would appear to require a huge number of replacements.

But a human sees:

```text
insert X at the beginning
keep A
keep B
keep C
```

Keys give React the identity information necessary to make this interpretation possible.

React's documentation specifically explains that keys allow React to identify items when they are inserted, deleted, or reordered. ([React][4])

---

# 13. Keys

Example:

```jsx
items.map(item => (
    <Item
        key={item.id}
        item={item}
    />
))
```

Suppose:

```text
Old:

key=10 A
key=20 B
key=30 C
```

New:

```text
key=40 X
key=10 A
key=20 B
key=30 C
```

React can reason:

```text
40 → new
10 → existing
20 → existing
30 → existing
```

So conceptually:

```text
insert X
keep A
keep B
keep C
```

rather than treating every position as unrelated.

React's docs explicitly say keys tell React which array item each component corresponds to so it can infer insertion, deletion, and movement. ([React][4])

---

# 14. Keys aren't only for arrays

This is frequently misunderstood.

You can use:

```jsx
<Chat key={contact.id} contact={contact} />
```

even when there is only one child.

Why?

Because a key participates in the component's identity.

React's official docs explicitly note that keys can be used to distinguish components even when they are not being rendered from a list. ([React][3])

For example:

```jsx
<Chat key="alice" />
```

and:

```jsx
<Chat key="bob" />
```

are different identities.

---

# 15. Key + type

Consider:

```jsx
<User key="1" />
```

becoming:

```jsx
<Admin key="1" />
```

Same key:

```text
"1"
```

but different type:

```text
User ≠ Admin
```

So React cannot preserve the `User` component as the same component identity.

The key is not an override that says:

> "Everything with this key is the same."

It participates in matching **within the relevant parent/child set**.

---

# 16. Keys are only unique among siblings

Suppose:

```jsx
<div>
    <User key="1" />
    <Product key="1" />
</div>
```

That's not automatically a key collision in the same sense as having duplicate keys among a single homogeneous sibling collection, because keys identify children relative to their parent and sibling set.

React's docs state that keys need to be unique among siblings, not globally unique. ([React][4])

So:

```text
Parent A
 └── key="1"

Parent B
 └── key="1"
```

is fine.

---

# 17. Why index keys are dangerous

Consider:

```jsx
items.map((item, index) => (
    <Item key={index} item={item} />
))
```

Suppose the initial list is:

```text
index 0 → A
index 1 → B
index 2 → C
```

Now insert X at the beginning:

```text
index 0 → X
index 1 → A
index 2 → B
index 3 → C
```

React sees:

```text
key 0 → old A, new X
key 1 → old B, new A
key 2 → old C, new B
key 3 → new
```

The identity information has become misleading.

React's official documentation warns that index keys commonly produce subtle bugs when items are inserted, removed, or reordered. ([React][4])

---

# 18. The input-state bug

This is the classic demonstration.

Suppose each row contains:

```jsx
<input />
```

and React thinks:

```text
index 0 = Alice
index 1 = Bob
```

Now insert:

```text
Charlie
```

at the top.

With index keys, React may match:

```text
old Alice's Fiber → new Charlie's position
old Bob's Fiber   → new Alice's position
```

State and DOM identity can therefore appear to "move to the wrong item."

This isn't because React is randomly broken.

It's because you gave React identity information that described **position**, not **the actual item**.

---

# 19. Stable IDs solve the problem

Use:

```jsx
items.map(item => (
    <Item
        key={item.id}
        item={item}
    />
))
```

Now:

```text
Alice → id 101
Bob   → id 102
```

Reordering doesn't change identity.

```text
Before:
101 Alice
102 Bob

After:
102 Bob
101 Alice
```

React can still match:

```text
102 → Bob
101 → Alice
```

The positions changed, but the identities didn't.

---

# 20. `key={Math.random()}` is terrible

Consider:

```jsx
<Item key={Math.random()} />
```

Every render:

```text
Render 1:
key = 0.381

Render 2:
key = 0.927
```

There is no match.

React sees a different identity every time.

Conceptually:

```text
old Item
   ↓
no matching key
   ↓
delete

new Item
   ↓
create
```

The official React documentation specifically warns that generating keys during rendering causes elements/components to be recreated and can lose user input. ([React][4])

---

# 21. The basic reconciliation decision tree

For a child, a simplified mental model is:

```text
Do I have an existing child?
        │
       yes
        │
        ▼
Does identity match?
(type/key as appropriate)
        │
    ┌───┴────┐
   yes       no
    │         │
    ▼         ▼
 reuse      create
    │        new
    ▼
update props
```

For a missing old child:

```text
new child only
    ↓
insert
```

For an old child that has no matching new child:

```text
old child only
    ↓
delete
```

And for a matched child whose old position is incompatible with the new ordering:

```text
matched identity
       ↓
move
```

---

# 22. A very important implementation detail

The current source has a function:

```text
updateSlot(...)
```

Its comment is essentially:

> update the Fiber if the keys match; otherwise return null. ([GitHub][1])

This is a very useful way to understand the algorithm.

For an existing slot:

```text
old child
+
new child
```

React initially asks:

```text
do the keys match?
```

If they don't, the simple positional matching path cannot reuse that old Fiber.

---

# 23. First pass: try to match in order

A useful conceptual simplification of the array algorithm is:

```text
new[0] ↔ old[0]
new[1] ↔ old[1]
new[2] ↔ old[2]
...
```

while the identity matches.

This is the **fast path**.

Suppose:

```text
Old:
A B C D

New:
A B C D
```

React can efficiently reuse the sequence.

Conceptually:

```text
A → A
B → B
C → C
D → D
```

No complicated lookup is needed.

---

# 24. What if the sequence breaks?

Suppose:

```text
Old:
A B C D

New:
A X B C D
```

At first:

```text
A → A
```

matches.

Then:

```text
X vs B
```

doesn't match.

At this point the algorithm needs to deal with the remaining children more intelligently.

The current source builds a temporary `Map` of remaining old children so they can be found quickly by key (and by index for implicit/no-key children). ([GitHub][1])

---

# 25. The remaining-children Map

Conceptually:

```text
Old remaining:

B
C
D
```

becomes something like:

```text
Map:

B → Fiber(B)
C → Fiber(C)
D → Fiber(D)
```

Then the new children can look for matches.

For keyed elements:

```text
new key "C"
    ↓
Map.get("C")
    ↓
Fiber(C)
```

The current source's `mapRemainingChildren` explicitly constructs this map, using explicit keys when present and indexes for implicitly keyed children. ([GitHub][1])

---

# 26. Why `Map`?

Because without a map, searching for:

```text
new item C
```

through:

```text
B
C
D
E
F
...
```

would repeatedly cost time.

The map gives roughly:

```text
lookup by key → O(1) average
```

which helps keep the common list reconciliation behavior efficient.

The overall algorithm isn't simply "always O(n) no matter what"; the implementation has multiple paths and bookkeeping. But stable keys allow React to efficiently identify matching siblings.

---

# 27. How React represents movement

Let's take:

```text
Old:
A B C
```

and:

```text
New:
B A C
```

Suppose keys are:

```text
A → 1
B → 2
C → 3
```

React can find:

```text
new B → old B
new A → old A
new C → old C
```

So the components themselves can be reused.

But React must also recognize:

```text
B moved
```

or equivalently that a placement/movement operation is required for the new ordering.

The current `placeChild` logic tracks each old index and maintains `lastPlacedIndex`. If a matched child's old index is less than that tracked value, React marks it with a `Placement` flag, identifying it as a move. ([GitHub][1])

---

# 28. `lastPlacedIndex`

This is an excellent interview-level internal.

Suppose old positions are:

```text
A = 0
B = 1
C = 2
```

New order:

```text
B
A
C
```

Process B first:

```text
oldIndex = 1

lastPlacedIndex = 0
```

Because:

```text
1 >= 0
```

B can remain in relative order.

Set:

```text
lastPlacedIndex = 1
```

Now process A:

```text
oldIndex = 0
```

But:

```text
0 < 1
```

Therefore A is considered to have moved.

React marks its Fiber with:

```text
Placement
```

The current source implements exactly this comparison. ([GitHub][1])

---

# 29. Why doesn't React mark B as moved?

Because React only needs enough placement information to transform the old host order into the new order.

The algorithm is not trying to record:

```text
"everyone who looks visually different"
```

It is tracking whether a matched Fiber needs placement based on its prior index relative to already processed children.

This is an algorithmic optimization, not a visual comparison.

---

# 30. Insertions

Suppose:

```text
Old:
A B

New:
A B C
```

C has no existing Fiber match.

React creates a new Fiber.

The current `placeChild` implementation detects when there is no alternate/current Fiber and marks the new Fiber for placement. ([GitHub][1])

Conceptually:

```text
A → reuse
B → reuse
C → insert
```

---

# 31. Deletions

Suppose:

```text
Old:
A B C

New:
A C
```

B no longer appears.

React must identify:

```text
B → deletion
```

The current reconciler has `deleteChild` and `deleteRemainingChildren` helpers that record deletions and set a `ChildDeletion` flag on the parent Fiber. ([GitHub][1])

This is important:

> Reconciliation can record a deletion during render; the actual host removal happens later during commit.

Again:

```text
render/reconciliation
      ↓
record required operation
      ↓
commit
      ↓
actual DOM removal
```

---

# 32. Flags

This is an important bridge between reconciliation and commit.

React doesn't necessarily perform the DOM operation at the exact moment it discovers the difference.

Instead, Fiber can contain flags describing required work.

Conceptually:

```text
Fiber
 ├── Placement
 ├── Update
 ├── ChildDeletion
 └── ...
```

Then commit code can inspect the finished tree and perform the actual host operations.

For example:

```text
Placement
    ↓
insert/move host nodes

ChildDeletion
    ↓
remove host nodes

Update
    ↓
apply changed properties
```

The current child reconciler visibly sets `Placement` and `ChildDeletion` flags during child reconciliation. ([GitHub][1])

---

# 33. This gives us a better architecture diagram

```text
              Component renders
                     │
                     ▼
              New React elements
                     │
                     ▼
              Child reconciliation
                     │
            ┌────────┼─────────┐
            ▼        ▼         ▼
          reuse    insert    delete
            │        │         │
            └────────┼─────────┘
                     ▼
                new Fibers
                     │
                     ▼
                  flags
                     │
                     ▼
               completed tree
                     │
                     ▼
                COMMIT PHASE
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
        update     insert     delete
          │          │          │
          └──────────┼──────────┘
                     ▼
                    DOM
```

This is much more accurate than saying:

```text
Virtual DOM → diff → DOM
```

---

# 34. The most important reconciliation rule

A very useful simplification is:

> **React tries to preserve identity whenever the new child can be matched to an existing child.**

If identity matches:

```text
reuse existing Fiber
```

If identity doesn't match:

```text
create new Fiber
```

If an old child disappears:

```text
delete
```

If a matched child changes position:

```text
placement/move
```

---

# 35. Example: changing text

Old:

```jsx
<h1>Hello</h1>
```

New:

```jsx
<h1>Hello World</h1>
```

Conceptually:

```text
type:
h1 == h1

key:
same/no key

        ↓

reuse Fiber

        ↓

text differs

        ↓

mark host update

        ↓

commit text update
```

The `<h1>` DOM node can stay the same.

---

# 36. Example: changing element type

Old:

```jsx
<h1>Hello</h1>
```

New:

```jsx
<p>Hello</p>
```

Conceptually:

```text
h1 != p
   ↓
cannot reuse as same host element type
   ↓
old subtree removed
   ↓
new subtree created
```

That's fundamentally different from the previous example.

---

# 37. Example: changing component type

Old:

```jsx
<User />
```

New:

```jsx
<Admin />
```

Conceptually:

```text
User != Admin
      ↓
different component identity
      ↓
old component subtree discarded
      ↓
new component subtree created
```

State below the old component isn't automatically transferred.

React's state-preservation docs explain this behavior through tree position and component identity. ([React][3])

---

# 38. Example: changing only props

Old:

```jsx
<User id={1} name="Alice" />
```

New:

```jsx
<User id={1} name="Bob" />
```

Conceptually:

```text
type:
User == User

key:
same

        ↓

reuse Fiber

        ↓

new props

        ↓

User renders using new props
```

The component identity remains.

---

# 39. Example: reorder with stable keys

Old:

```jsx
[
    <Item key="A" />,
    <Item key="B" />,
    <Item key="C" />
]
```

New:

```jsx
[
    <Item key="C" />,
    <Item key="A" />,
    <Item key="B" />
]
```

React can match:

```text
C → old C
A → old A
B → old B
```

rather than creating three new components.

Then placement logic handles the ordering changes. ([GitHub][1])

---

# 40. What happens to component state during reorder?

With stable keys:

```text
A key → 101
B key → 102
C key → 103
```

After:

```text
C
A
B
```

the state follows the keyed identity:

```text
C → state C
A → state A
B → state B
```

even though their positions changed.

This is one of the primary reasons correct keys matter.

React explicitly states that keys help React maintain component identity and therefore preserve state between re-renders. ([React][5])

---

# 41. Without keys

Suppose:

```text
A
B
C
```

becomes:

```text
C
A
B
```

without explicit keys.

React has much less identity information.

For unkeyed children, position/index plays a role in matching.

So React can effectively see:

```text
position 0:
old A → new C

position 1:
old B → new A

position 2:
old C → new B
```

This can produce undesirable state movement.

That's why dynamic/reordered lists should normally use stable keys.

---

# 42. A subtle point: React doesn't physically "move the component"

A component is a logical/internal identity.

The DOM node can be moved.

For example:

```text
React identity:
Item key=A
```

can remain the same while its host DOM node changes position.

So:

```text
component identity
```

and:

```text
DOM position
```

are distinct concepts.

This distinction is important when reasoning about keyed lists.

---

# 43. Nested trees

Consider:

```jsx
<div>
    <User>
        <Avatar />
    </User>
</div>
```

Conceptual render tree:

```text
div
└── User
    └── Avatar
```

If `User` is preserved:

```text
User → User
```

React can reconcile the subtree below it.

If `User` becomes:

```jsx
<Admin>
    <Avatar />
</Admin>
```

then:

```text
User → Admin
```

is a different component identity.

The subtree is reconciled from that new boundary.

---

# 44. Why state gets destroyed

Suppose:

```jsx
{isLoggedIn ? <Dashboard /> : <Login />}
```

At one render:

```text
Dashboard
```

At another:

```text
Login
```

These are different component types at the relevant position.

React removes the old tree and mounts the new one, so state belonging to the removed subtree is destroyed. React's state docs explicitly describe state as being destroyed when the component is removed or replaced with another component at that position. ([React][3])

---

# 45. A key can deliberately force replacement

Suppose:

```jsx
<Chat key={userId} userId={userId} />
```

User changes:

```text
Alice → Bob
```

Then:

```text
key="alice"
    ↓
key="bob"
```

Even though the component type remains:

```text
Chat
```

the identity changes.

Conceptually:

```text
Chat/alice
    ↓
remove

Chat/bob
    ↓
create
```

Therefore the new Chat subtree gets fresh state.

React's docs use exactly this pattern to reset forms/chat drafts when switching identities. ([React][3])

---

# 46. Reconciliation is about identity, not deep equality

Another interview trap.

Suppose:

```jsx
<User data={{name: "Alice"}} />
```

You might think React does:

```text
deep compare every property
```

That isn't the general reconciliation model.

React uses structural identity rules such as:

```text
type
key
position
```

to determine whether a child corresponds to an existing Fiber, and then the component/host update logic handles props.

So don't describe reconciliation as:

> "React deep-compares the entire object graph."

That's inaccurate and also misses the purpose of keys.

---

# 47. `useFiber()` is a useful implementation clue

The current source has:

```text
useFiber(current, pendingProps)
```

which calls:

```text
createWorkInProgress(fiber, pendingProps)
```

and prepares the clone for its new position. ([GitHub][1])

This gives us a very useful picture:

```text
Current Fiber
      │
      ▼
createWorkInProgress
      │
      ▼
WIP Fiber
```

So "reuse" doesn't mean:

> "Modify the current committed Fiber in place."

Instead, React builds its work-in-progress representation.

That becomes **extremely important in our Fiber lesson**.

---

# 48. Reconciliation does not itself mean "update DOM now"

Suppose React determines:

```text
Fiber X needs Placement
```

During reconciliation, React marks:

```text
Placement
```

The actual host insertion happens later during commit.

Likewise:

```text
ChildDeletion
```

records deletion work; commit performs the host removal.

The current child reconciler source demonstrates this separation through its flags and deletion arrays. ([GitHub][1])

---

# 49. The array algorithm at a high level

For an array of children, a useful simplified model is:

```text
                    New children
                         │
                         ▼
             Try matching old children
               from the current index
                         │
              ┌──────────┴──────────┐
              │                     │
           matches               mismatch
              │                     │
              ▼                     ▼
          reuse Fiber          Map remaining
                                  old children
                                     │
                                     ▼
                             lookup by key/index
                                     │
                    ┌────────────────┼────────────┐
                    ▼                ▼            ▼
                  reuse            move         insert
                    │                │            │
                    └────────────────┴────────────┘
                                     │
                                     ▼
                              delete leftovers
```

This is a much better mental model than trying to memorize 2,000+ lines of `ReactChildFiber.js`.

The actual implementation is more nuanced and contains specialized handling for many child types. ([GitHub][1])

---

# 50. Let's manually simulate a list

Old:

```text
A B C D
```

Keys:

```text
A → A
B → B
C → C
D → D
```

New:

```text
A C D E
```

We'll conceptually process it.

### First child

```text
old A
new A
```

Match.

```text
reuse A
```

### Second

```text
old B
new C
```

No key match.

We enter the more general lookup path.

Existing remaining:

```text
B → Fiber B
C → Fiber C
D → Fiber D
```

New C:

```text
lookup C
   ↓
Fiber C
```

Reuse C.

### Third

New D:

```text
lookup D
   ↓
Fiber D
```

Reuse D.

### Fourth

New E:

```text
lookup E
   ↓
nothing
```

Create E.

Remaining old:

```text
B
```

Delete B.

Conceptual result:

```text
reuse A
reuse C
reuse D
insert E
delete B
```

That's reconciliation.

---

# 51. What happens to DOM?

Only after reconciliation and completion do we commit.

So:

```text
Old DOM:
A B C D
```

becomes conceptually:

```text
A C D E
```

through operations such as:

```text
remove B
insert E
```

rather than:

```text
remove everything
create everything
```

---

# 52. Why this is called "diffing"

You will hear:

```text
diff algorithm
```

because React is effectively determining the differences between the existing and requested tree structure.

But "diff" shouldn't make you imagine a generic:

```text
JSON deep-diff
```

algorithm.

React uses a specialized tree reconciliation algorithm with identity, keys, Fiber relationships, and placement/deletion bookkeeping.

---

# 53. Reconciliation is optimized using assumptions

A general optimal tree-editing problem is expensive.

React doesn't attempt to solve arbitrary tree transformation optimally.

Instead, it relies on practical assumptions such as:

```text
different element/component types
→ different subtree identity

developer-provided stable keys
→ stable child identity
```

These assumptions make reconciliation practical for UI workloads.

This is one reason stable keys are so important.

---

# 54. Interview question: Why are keys necessary?

A strong answer:

> Keys give React stable identity for children within their sibling set. They allow React to match a child in the new render with the corresponding child from the previous render even when its position changes due to insertion, deletion, or reordering. This allows React to preserve the correct Fiber/state and perform the appropriate placement or deletion work.

That is much stronger than:

> "Keys remove a warning."

---

# 55. Interview question: Why is index as a key bad?

Strong answer:

> An index represents the current position, not the identity of the underlying item. If items are inserted, removed, or reordered, the same index can refer to a different item, causing React to associate the previous Fiber and its state with the wrong item. Index keys are safe only in limited cases where the list is effectively static and never reordered, inserted into, or deleted from.

React explicitly documents this limitation. ([React][4])

---

# 56. Interview question: What happens when type changes?

Strong answer:

> If the new element at a position has a different identity/type from the existing one, React doesn't reuse the existing Fiber as the same element. It creates the new subtree and deletes the old one, so state associated with the old component/subtree is not preserved.

The current `updateElement` implementation checks type compatibility and creates a new Fiber when it can't reuse the current one. ([GitHub][1])

---

# 57. Interview question: Does changing props recreate the component?

Normally, no.

For:

```jsx
<User name="Alice" />
```

→

```jsx
<User name="Bob" />
```

assuming identity matches:

```text
same type
same key/position
       ↓
reuse Fiber
       ↓
new props
       ↓
render component again
```

---

# 58. Interview question: Why does changing `key` reset state?

Because `key` participates in the child's identity.

So:

```jsx
<Form key="userA" />
```

and:

```jsx
<Form key="userB" />
```

are treated as different component identities even though both have type:

```text
Form
```

React therefore doesn't preserve the previous keyed instance's state. ([React][3])

---

# 59. Interview question: What does reconciliation produce?

This is a more senior-level question.

A good answer:

> Reconciliation produces the next internal Fiber structure and records the work required to transition from the current tree to the next one. That work can include reusing existing Fibers, creating new ones, deleting children, and marking placement or update-related flags. The commit phase later applies the required host operations.

The current source shows child reconciliation creating/updating Fibers and setting flags such as `Placement` and `ChildDeletion`. ([GitHub][1])

---

# 60. One subtle but important point: keys don't "move state" themselves

People often say:

> "Keys move state."

A more accurate statement is:

> Keys allow React to recognize that a new child corresponds to an existing child, so React can reuse the existing Fiber and therefore preserve its associated state.

That's an important distinction.

The key isn't a storage container for state.

It is identity information.

---

# 61. Another subtle point: `key` is not a normal prop

Consider:

```jsx
<Item key={item.id} />
```

Inside:

```jsx
function Item(props) {
    console.log(props.key);
}
```

you should not expect `key` to be available as an ordinary application prop.

React extracts `key` for reconciliation identity.

The official docs explicitly note that components do not receive `key` as a prop; pass a separate prop when the component itself needs the ID. ([React][4])

For example:

```jsx
<Item
    key={item.id}
    id={item.id}
/>
```

---

# 62. The full picture now

We can finally refine our earlier architecture:

```text
                State update
                     │
                     ▼
                React render
                     │
                     ▼
             Component execution
                     │
                     ▼
             React element tree
                     │
                     ▼
            Reconciliation
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
        reuse      insert     delete
          │          │          │
          └──────────┼──────────┘
                     ▼
            Work-in-progress
               Fiber tree
                     │
                     ▼
                  flags
                     │
                     ▼
                completed
                   work
                     │
                     ▼
                 COMMIT
                     │
                     ▼
                   DOM
```

This is the foundation for understanding Fiber.

---

# 63. The most important things to memorize

Don't memorize the entire source code.

Memorize this:

```text
1. React receives new element descriptions.

2. Reconciliation compares the new children against
   the existing Fiber children.

3. Matching identity allows Fiber reuse.

4. Type is important for identity.

5. Key provides explicit child identity.

6. Stable keys preserve state through reordering.

7. Missing children are deleted.

8. New children are inserted.

9. Matched children can be moved.

10. Reconciliation records work; commit performs host mutations.
```

---

# 64. The source-code connections

You don't need to know every line, but these are worth recognizing when reading React source:

```text
ReactFiberBeginWork.js
    ↓
reconcileChildren()

ReactChildFiber.js
    ↓
child reconciliation
    ↓
updateSlot()
updateElement()
updateFromMap()
placeChild()
deleteChild()
useFiber()

Fiber
    ↓
work-in-progress tree

Commit phase
    ↓
host mutations
```

The current React source visibly contains this separation: `beginWork` invokes `reconcileChildren`, while `ReactChildFiber.js` contains the child matching/placement/deletion logic. ([GitHub][2])

---

# 65. One final interview exercise

Suppose we have:

```jsx
function App() {
    return (
        <div>
            <Item key="A" name="A" />
            <Item key="B" name="B" />
            <Item key="C" name="C" />
        </div>
    );
}
```

Next render:

```jsx
function App() {
    return (
        <div>
            <Item key="C" name="C" />
            <Item key="A" name="A" />
            <Item key="D" name="D" />
        </div>
    );
}
```

Reason through it.

### Parent

```text
div → div
```

Reuse.

### First child

```text
old A
new C
```

Key mismatch.

Search remaining old children.

```text
C found
```

Reuse C.

### Second child

```text
new A
```

Find old A.

Reuse A, but its old position indicates a move.

### Third child

```text
new D
```

No existing D.

Create D.

### Old B

No matching new B.

Delete B.

So conceptually:

```text
C → reuse + move
A → reuse + move/placement as required by ordering
D → insert
B → delete
```

That's the type of reasoning you want to be able to perform in an interview.

---

# 66. One thing to correct from our earlier mental model

Previously we used:

```text
old tree
   ↓
new tree
   ↓
diff
```

Keep that as a teaching abstraction.

But the more accurate React internals model is:

```text
Current Fiber tree
       +
new React children
       ↓
child reconciliation
       ↓
Work-in-progress Fibers
       ↓
flags
       ↓
commit
```

This is the mental model you should use when an interviewer starts asking about **Fiber internals**.

---

# Next Topic — Fiber

Now we're ready for the most important React-internals topic:

# **Topic 6 — React Fiber in Depth**

We'll dissect an actual Fiber conceptually:

```javascript
{
    tag,
    key,
    elementType,
    type,

    pendingProps,
    memoizedProps,
    memoizedState,

    child,
    sibling,
    return,

    alternate,

    lanes,
    childLanes,

    flags,
    subtreeFlags
}
```

Then we'll trace:

```text
beginWork
    ↓
child
    ↓
sibling
    ↓
completeWork
    ↓
return
```

and understand **why React represents the tree as a linked structure rather than simply using nested JavaScript arrays/objects**.

We'll also finally explain **double buffering, `current` vs `workInProgress`, `alternate`, incremental rendering, and how Fiber made concurrent React possible**.

[1]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactChildFiber.js "react/packages/react-reconciler/src/ReactChildFiber.js at main · react/react · GitHub"
[2]: https://github.com/facebook/react/blob/main/packages/react-reconciler/src/ReactFiberBeginWork.js "react/packages/react-reconciler/src/ReactFiberBeginWork.js at main · react/react · GitHub"
[3]: https://react.dev/learn/preserving-and-resetting-state?utm_source=chatgpt.com "Preserving and Resetting State – React"
[4]: https://react.dev/learn/rendering-lists?utm_source=chatgpt.com "Rendering Lists – React"
[5]: https://react.dev/learn/tutorial-tic-tac-toe?utm_source=chatgpt.com "Tutorial: Tic-Tac-Toe – React"

