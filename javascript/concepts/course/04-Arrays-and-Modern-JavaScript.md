# Chapter 4 — Arrays & Modern JavaScript

Arrays are used constantly in real JavaScript applications, and interviewers often test whether you understand the **difference between transformation, filtering, searching, mutation, and copying**.

We'll focus only on the high-value concepts.

---

# 1. What is an Array?

An array is an ordered collection of values.

```javascript
const numbers = [10, 20, 30, 40];
```

Indexes start at `0`:

```text
index:     0    1    2    3
           ↓    ↓    ↓    ↓
numbers = [10,  20,  30,  40]
```

Access:

```javascript
numbers[0]; // 10
numbers[2]; // 30
```

Length:

```javascript
numbers.length; // 4
```

---

# 2. Are Arrays Actually Objects?

Yes.

```javascript
typeof [];
```

returns:

```text
"object"
```

Arrays are specialized objects with array-specific behavior.

```javascript
Array.isArray([]);
```

returns:

```text
true
```

Use `Array.isArray()` rather than:

```javascript
typeof value === "array" // ❌
```

because `"array"` is not a `typeof` result.

---

# 3. Arrays Can Contain Different Types

JavaScript arrays don't require all elements to have the same type.

```javascript
const values = [
    10,
    "hello",
    true,
    null,
    { name: "John" }
];
```

This is valid.

In real applications, though, keeping arrays logically homogeneous usually makes code easier to reason about.

---

# 4. `push`, `pop`, `shift`, `unshift`

These are basic mutation methods.

### `push`

Adds to the end.

```javascript
const numbers = [1, 2];

numbers.push(3);

console.log(numbers);
```

```text
[1, 2, 3]
```

---

### `pop`

Removes from the end.

```javascript
numbers.pop();
```

```text
[1, 2]
```

---

### `unshift`

Adds to the beginning.

```javascript
numbers.unshift(0);
```

```text
[0, 1, 2]
```

---

### `shift`

Removes from the beginning.

```javascript
numbers.shift();
```

```text
[1, 2]
```

Important:

```text
push    → add end
pop     → remove end

unshift → add beginning
shift   → remove beginning
```

All four **mutate the array**.

---

# 5. `slice()` vs `splice()`

This is a classic interview question.

## `slice()`

Creates a new array containing a portion of the original.

```javascript
const numbers = [10, 20, 30, 40];

const result = numbers.slice(1, 3);

console.log(result);
```

Result:

```text
[20, 30]
```

The original remains unchanged.

```text
slice → non-mutating
```

---

## `splice()`

Modifies the original array.

```javascript
const numbers = [10, 20, 30, 40];

numbers.splice(1, 2);
```

Now:

```text
numbers = [10, 40]
```

So:

```text
slice  → returns copied portion
splice → modifies original
```

This distinction is very frequently asked.

---

# 6. `map()`

`map()` transforms every element and returns a **new array**.

```javascript
const numbers = [1, 2, 3];

const doubled = numbers.map(n => n * 2);
```

Result:

```text
[2, 4, 6]
```

Conceptually:

```text
1 → 2
2 → 4
3 → 6
```

The original:

```text
[1, 2, 3]
```

remains unchanged.

### Interview definition

> `map()` transforms each element and returns a new array of the same length.

---

# 7. `filter()`

`filter()` selects elements based on a condition.

```javascript
const numbers = [1, 2, 3, 4, 5];

const even = numbers.filter(n => n % 2 === 0);
```

Result:

```text
[2, 4]
```

Unlike `map()`, the resulting array can have a different length.

```text
map
→ transform

filter
→ select
```

---

# 8. `find()`

`find()` returns the **first element** satisfying a condition.

```javascript
const users = [
    { id: 1, name: "John" },
    { id: 2, name: "Mike" },
    { id: 3, name: "Sam" }
];

const user = users.find(user => user.id === 2);
```

Result:

```javascript
{ id: 2, name: "Mike" }
```

If nothing matches:

```text
undefined
```

Important:

```text
find()
→ returns element

filter()
→ returns array
```

---

# 9. `findIndex()`

Similar to `find()`, but returns the index.

```javascript
const numbers = [10, 20, 30];

const index = numbers.findIndex(n => n === 20);
```

Result:

```text
1
```

If not found:

```text
-1
```

---

# 10. `some()`

Checks whether **at least one** element satisfies the condition.

```javascript
const numbers = [1, 3, 4];

const hasEven = numbers.some(n => n % 2 === 0);
```

Result:

```text
true
```

Think:

```text
some → "Does at least one match?"
```

---

# 11. `every()`

Checks whether **all** elements satisfy the condition.

```javascript
const numbers = [2, 4, 6];

const allEven = numbers.every(n => n % 2 === 0);
```

Result:

```text
true
```

Think:

```text
every → "Do all match?"
```

---

# 12. `reduce()`

`reduce()` is one of the most important array methods for interviews.

It reduces an array into a single accumulated result.

Example:

```javascript
const numbers = [1, 2, 3, 4];

const sum = numbers.reduce(
    (total, number) => total + number,
    0
);
```

Result:

```text
10
```

Conceptually:

```text
initial = 0

0 + 1 = 1
1 + 2 = 3
3 + 3 = 6
6 + 4 = 10
```

The second argument:

```javascript
0
```

is the **initial accumulator value**.

---

# 13. `reduce()` Can Produce Objects

For example, count occurrences:

```javascript
const names = ["John", "Mike", "John", "Sam", "Mike"];

const counts = names.reduce((result, name) => {
    result[name] = (result[name] || 0) + 1;
    return result;
}, {});
```

Result:

```javascript
{
    John: 2,
    Mike: 2,
    Sam: 1
}
```

This is a very common interview pattern.

---

# 14. `map()` + `filter()` + `reduce()`

Consider:

```javascript
const numbers = [1, 2, 3, 4, 5, 6];
```

Get the sum of squares of even numbers.

You can write:

```javascript
const result = numbers
    .filter(n => n % 2 === 0)
    .map(n => n * n)
    .reduce((sum, n) => sum + n, 0);
```

Execution:

```text
[1,2,3,4,5,6]
        ↓ filter
[2,4,6]
        ↓ map
[4,16,36]
        ↓ reduce
56
```

This is called **method chaining**.

---

# 15. `forEach()` vs `map()`

Very common interview question.

### `forEach()`

Used when you want to perform an action for each element.

```javascript
numbers.forEach(n => {
    console.log(n);
});
```

It does **not** produce a transformed array.

Its return value is:

```text
undefined
```

---

### `map()`

Used when you want a transformed array.

```javascript
const doubled = numbers.map(n => n * 2);
```

### Rule

```text
forEach → perform side effect
map     → transform data
```

Don't use `map()` just because it looks concise if you don't use the returned array.

---

# 16. `sort()` — Major Interview Trap

Consider:

```javascript
const numbers = [10, 2, 5, 1];

numbers.sort();

console.log(numbers);
```

You may expect:

```text
[1, 2, 5, 10]
```

But you get:

```text
[1, 10, 2, 5]
```

Why?

By default, `sort()` compares values as strings.

Conceptually:

```text
"10"
"2"
"5"
"1"
```

Lexicographical ordering gives:

```text
"1"
"10"
"2"
"5"
```

---

# 17. Numeric Sorting

Use a comparator:

```javascript
numbers.sort((a, b) => a - b);
```

Ascending:

```javascript
(a, b) => a - b
```

Descending:

```javascript
(a, b) => b - a
```

The comparator works conceptually like:

```text
negative → a before b
zero     → equal
positive → b before a
```

---

# 18. `sort()` Mutates

Another interview trap.

```javascript
const numbers = [3, 1, 2];

const sorted = numbers.sort((a, b) => a - b);

console.log(numbers);
```

The original array is also:

```text
[1, 2, 3]
```

because `sort()` mutates the array.

If you want a non-mutating approach in modern JavaScript:

```javascript
const sorted = numbers.toSorted((a, b) => a - b);
```

`toSorted()` returns a new sorted array.

Similarly, modern JavaScript provides non-mutating counterparts such as:

```text
toReversed()
toSpliced()
with()
```

These are useful when following immutable data patterns.

---

# 19. Spread Operator

The spread syntax:

```javascript
...
```

can expand elements.

```javascript
const a = [1, 2, 3];

const b = [...a];
```

Now:

```text
a → [1, 2, 3]
b → [1, 2, 3]
```

They are different array objects.

```javascript
a === b; // false
```

---

# 20. Combining Arrays

```javascript
const a = [1, 2];
const b = [3, 4];

const result = [...a, ...b];
```

Result:

```text
[1, 2, 3, 4]
```

This is often cleaner than:

```javascript
a.concat(b);
```

Both are useful.

---

# 21. Spread Is Shallow

This is extremely important.

```javascript
const original = {
    name: "John",
    address: {
        city: "Hyderabad"
    }
};

const copy = {
    ...original
};
```

The outer object is copied.

But:

```text
original.address
       │
       └────────┐
                ▼
          address object
                ▲
       ┌────────┘
copy.address
```

Therefore:

```javascript
copy.address.city = "Bangalore";

console.log(original.address.city);
```

Output:

```text
Bangalore
```

The nested object is shared.

---

# 22. Shallow Copy

Common shallow-copy techniques:

```javascript
const copy1 = { ...original };
```

```javascript
const copy2 = Object.assign({}, original);
```

For arrays:

```javascript
const copy = [...original];
```

or:

```javascript
const copy = original.slice();
```

But all are **shallow copies**.

---

# 23. Deep Copy

A deep copy means nested objects are copied as well.

Modern JavaScript provides:

```javascript
structuredClone(original);
```

Example:

```javascript
const original = {
    name: "John",
    address: {
        city: "Hyderabad"
    }
};

const copy = structuredClone(original);

copy.address.city = "Bangalore";

console.log(original.address.city);
```

Output:

```text
Hyderabad
```

The nested object is independent.

---

# 24. Why `JSON.parse(JSON.stringify())` Isn't a General Deep Clone

You may see:

```javascript
const copy = JSON.parse(JSON.stringify(original));
```

This can work for simple JSON-compatible data, but it has limitations.

For example, it doesn't preserve values such as:

* `undefined`
* functions
* `Symbol`
* `BigInt`

and it doesn't correctly preserve all object types such as `Date`, `Map`, and `Set`.

So for modern JavaScript:

```javascript
structuredClone(value);
```

is generally the better built-in option when the value is cloneable.

---

# 25. Destructuring

Destructuring extracts values from arrays or objects.

### Array destructuring

```javascript
const numbers = [10, 20, 30];

const [a, b, c] = numbers;
```

Result:

```text
a = 10
b = 20
c = 30
```

You can skip values:

```javascript
const [first, , third] = numbers;
```

---

# 26. Object Destructuring

```javascript
const user = {
    name: "John",
    age: 30
};

const { name, age } = user;
```

Now:

```text
name → "John"
age  → 30
```

---

# 27. Renaming During Destructuring

```javascript
const user = {
    name: "John"
};

const { name: userName } = user;
```

Now:

```text
userName → "John"
```

The syntax:

```text
propertyName: variableName
```

means:

> Read this property and store it in this variable.

---

# 28. Default Values in Destructuring

```javascript
const user = {
    name: "John"
};

const { name, age = 30 } = user;
```

Result:

```text
name = "John"
age  = 30
```

Again, defaults apply when the value is `undefined`.

```javascript
const { age = 30 } = { age: undefined };
```

gives:

```text
30
```

But:

```javascript
const { age = 30 } = { age: null };
```

gives:

```text
null
```

---

# 29. Rest vs Spread

Same syntax:

```javascript
...
```

but the meaning depends on where it appears.

### Spread

Expands values:

```javascript
const result = [...numbers];
```

Think:

```text
spread → unpack
```

### Rest

Collects remaining values:

```javascript
const [first, ...rest] = numbers;
```

Think:

```text
rest → collect
```

Example:

```javascript
const [first, ...rest] = [10, 20, 30, 40];
```

Result:

```text
first = 10
rest  = [20, 30, 40]
```

---

# 30. Optional Chaining with Arrays

You can use:

```javascript
const firstUser = users?.[0];
```

If `users` is `null` or `undefined`, it safely returns:

```text
undefined
```

This is particularly useful when dealing with API responses.

---

# 31. Nullish Coalescing + Arrays

Example:

```javascript
const users = response?.users ?? [];
```

Meaning:

```text
if response.users is null/undefined
        ↓
use []
```

This is a very common production pattern.

---

# 32. Important Array Interview Question

What is the output?

```javascript
const numbers = [1, 2, 3];

const result = numbers.map(n => {
    n * 2;
});

console.log(result);
```

Answer:

```text
[undefined, undefined, undefined]
```

Why?

Because the callback uses `{}` and doesn't explicitly return.

You need:

```javascript
const result = numbers.map(n => {
    return n * 2;
});
```

or:

```javascript
const result = numbers.map(n => n * 2);
```

This is a very common arrow-function mistake.

---

# 33. Another Interview Question

```javascript
const numbers = [1, 2, 3];

const result = numbers.forEach(n => n * 2);

console.log(result);
```

Output:

```text
undefined
```

Because `forEach()` doesn't return the transformed array.

Use:

```javascript
map()
```

if you need a new array.

---

# 34. `filter()` + Objects

Very common in real applications.

```javascript
const users = [
    { name: "John", active: true },
    { name: "Mike", active: false },
    { name: "Sam", active: true }
];

const activeUsers = users.filter(user => user.active);
```

Result:

```text
John
Sam
```

---

# 35. `find()` vs `filter()`

Suppose:

```javascript
const users = [
    { id: 1, name: "John" },
    { id: 2, name: "Mike" },
    { id: 3, name: "Sam" }
];
```

Find one user:

```javascript
users.find(user => user.id === 2);
```

Returns:

```javascript
{ id: 2, name: "Mike" }
```

Filter users:

```javascript
users.filter(user => user.id > 1);
```

Returns:

```javascript
[
    { id: 2, name: "Mike" },
    { id: 3, name: "Sam" }
]
```

Remember:

```text
find
→ one element / undefined

filter
→ array / []
```

---

# 36. Important: `map()` Doesn't Mutate the Array

```javascript
const numbers = [1, 2, 3];

const doubled = numbers.map(n => n * 2);

console.log(numbers);
```

Still:

```text
[1, 2, 3]
```

But be careful with **objects**.

```javascript
const users = [
    { name: "John" }
];

users.map(user => {
    user.name = "Mike";
});
```

The array itself wasn't replaced, but the objects inside it were mutated.

This goes back to **shallow references**.

---

# 37. Array Methods — Interview Table

| Method         | Purpose       | Returns             | Mutates? |
| -------------- | ------------- | ------------------- | -------- |
| `map()`        | Transform     | New array           | ❌        |
| `filter()`     | Select        | New array           | ❌        |
| `find()`       | Find first    | Element/`undefined` | ❌        |
| `findIndex()`  | Find index    | Number              | ❌        |
| `some()`       | Any match?    | Boolean             | ❌        |
| `every()`      | All match?    | Boolean             | ❌        |
| `reduce()`     | Accumulate    | Any value           | ❌*       |
| `forEach()`    | Side effects  | `undefined`         | ❌*       |
| `slice()`      | Copy portion  | New array           | ❌        |
| `splice()`     | Insert/remove | Removed elements    | ✅        |
| `sort()`       | Sort          | Same array          | ✅        |
| `reverse()`    | Reverse       | Same array          | ✅        |
| `toSorted()`   | Sort          | New array           | ❌        |
| `toReversed()` | Reverse       | New array           | ❌        |

`reduce()` and `forEach()` themselves don't mutate the array, but your callback can mutate objects or external state.

---

# 38. High-Value Interview Question

What is the output?

```javascript
const a = [1, 2, 3];
const b = a;

b.push(4);

console.log(a);
```

Output:

```text
[1, 2, 3, 4]
```

Because:

```text
a ─────┐
       ▼
   [1,2,3]
       ▲
       │
b ─────┘
```

Now:

```javascript
const a = [1, 2, 3];
const b = [...a];

b.push(4);

console.log(a);
```

Output:

```text
[1, 2, 3]
```

because the arrays are different objects.

---

# 39. High-Value Interview Question

What is the output?

```javascript
const a = [1, 2, 3];

const b = [...a];

console.log(a === b);
```

Answer:

```text
false
```

But:

```javascript
const a = [[1, 2]];

const b = [...a];

console.log(a[0] === b[0]);
```

Answer:

```text
true
```

Because spread is shallow.

```text
a ──► [ ──► nested array ]
                    ▲
b ──► [ ───────────┘
```

---

# 40. Chapter 4 Interview Cheat Sheet

### Transformation

```javascript
map()
```

### Filtering

```javascript
filter()
```

### Find one

```javascript
find()
```

### Find index

```javascript
findIndex()
```

### At least one

```javascript
some()
```

### All

```javascript
every()
```

### Accumulate

```javascript
reduce()
```

### Side effects

```javascript
forEach()
```

### Copy portion

```javascript
slice()
```

### Modify array

```javascript
splice()
```

### Sort

```javascript
sort()
```

Remember that `sort()`:

```text
1. Mutates the array
2. Uses string comparison by default
```

### Copy

```javascript
[...array]
{...object}
```

but these are:

```text
shallow copies
```

### Deep clone

```javascript
structuredClone(value)
```

when supported by the data being cloned.

### Destructuring

```javascript
const { name, age } = user;
const [first, second] = numbers;
```

### Spread

```text
unpack values
```

### Rest

```text
collect remaining values
```

---

## Next Chapter

**Chapter 5 — Asynchronous JavaScript** is probably the **single most important JavaScript interview chapter** after functions/scope.

We'll build it from the ground up:

```text
Synchronous execution
       ↓
Call Stack
       ↓
Why blocking happens
       ↓
Web APIs / Runtime APIs
       ↓
Callbacks
       ↓
Task Queue
       ↓
Microtask Queue
       ↓
Event Loop
       ↓
Promises
       ↓
async / await
       ↓
Promise chaining
       ↓
Error handling
       ↓
Output-based interview questions
```

The most important part will be understanding **why this outputs what it does**:

```javascript
console.log("A");

setTimeout(() => console.log("B"), 0);

Promise.resolve().then(() => console.log("C"));

console.log("D");
```

and similar interview questions involving **microtasks vs macrotasks**.

