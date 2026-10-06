# JavaScript Interview Course

We’ll keep this **compact and interview-focused**, covering the core concepts that actually matter for JavaScript interviews rather than every language feature.

### Course Structure

| Chapter | Topic                         | What we'll focus on                                                  |
| ------- | ----------------------------- | -------------------------------------------------------------------- |
| **1**   | JavaScript Fundamentals       | Runtime, execution, variables, types, equality, coercion             |
| **2**   | Functions, Scope & Closures   | Scope chain, lexical scope, closures, `this`                         |
| **3**   | Objects, Prototypes & Classes | Objects, prototype chain, inheritance, classes                       |
| **4**   | Arrays & Modern JavaScript    | Destructuring, spread/rest, important array methods                  |
| **5**   | Asynchronous JavaScript       | Call stack, Web APIs, event loop, callbacks, Promises, `async/await` |
| **6**   | JavaScript Execution & Memory | Execution contexts, hoisting, TDZ, memory, garbage collection        |
| **7**   | Modules & Error Handling      | ES modules, CommonJS, errors, strict mode                            |
| **8**   | Advanced Interview Topics     | Debouncing, throttling, currying, composition, iterators/generators  |
| **9**   | JavaScript Interview Problems | Output questions, tricky behavior, coding questions, scenarios       |

### How we'll study each chapter

For each topic:

1. **Problem → why the concept exists**
2. **Actual definition**
3. **How it works internally**
4. **Simple example**
5. **Real-world usage**
6. **Common mistakes**
7. **Interview questions**
8. **Tricky/output-based questions**
9. **Key points to remember**

I'll also distinguish between:

> **What JavaScript guarantees** vs **what the browser/runtime happens to implement**

This is particularly important for topics such as the event loop, `this`, prototypes, hoisting, and asynchronous execution.

---

# Chapter 1 — JavaScript Fundamentals

## 1. What exactly is JavaScript?

JavaScript is a **high-level, dynamically typed, garbage-collected programming language** primarily standardized by **ECMAScript**.

A useful interview definition:

> **JavaScript is a dynamically typed programming language whose execution is defined by the ECMAScript specification and whose programs run inside hosts such as browsers or Node.js.**

Notice something important:

**JavaScript ≠ Browser**

The browser provides a **host environment** around JavaScript.

```text
                 Browser
        ┌─────────────────────────┐
        │                         │
        │   JavaScript Engine     │
        │   ┌─────────────────┐   │
        │   │   JavaScript    │   │
        │   │   code          │   │
        │   └─────────────────┘   │
        │                         │
        │   Web APIs              │
        │   DOM                  │
        │   fetch()              │
        │   setTimeout()         │
        │   localStorage         │
        │                         │
        └─────────────────────────┘
```

For example:

```javascript
console.log("Hello");

setTimeout(() => {
    console.log("Done");
}, 1000);
```

`setTimeout()` is **not a core ECMAScript language feature**.

The browser provides it as a Web API.

Node.js provides its own host APIs.

---

# 2. JavaScript Engine

JavaScript source code needs an engine to execute it.

Examples:

* Chrome → **V8**
* Node.js → **V8**
* Firefox → **SpiderMonkey**
* Safari → **JavaScriptCore**

Conceptually:

```text
JavaScript source
       ↓
    Parsing
       ↓
   Compilation
       ↓
   Machine code
       ↓
    Execution
```

Modern engines use techniques such as **JIT compilation** to optimize frequently executed code.

You don't normally need to explain the exact internal pipeline in an interview unless asked.

The important distinction is:

```text
JavaScript language
        ↓
ECMAScript specification

Runtime environment
        ↓
Engine + host APIs
```

---

# 3. Is JavaScript interpreted or compiled?

This is a common interview question.

### Bad answer

> JavaScript is an interpreted language.

### Also incomplete

> JavaScript is a compiled language.

### Better answer

> Modern JavaScript engines use a combination of interpretation, compilation, and runtime optimization. JavaScript source is parsed and executed by the engine, which may compile code to machine code and optimize hot code using JIT techniques.

So don't think:

```text
JavaScript = interpreted
```

Instead:

```text
JavaScript
    ↓
Engine parses code
    ↓
Executes code
    ↓
Compiles/optimizes where beneficial
```

---

# 4. Variables

JavaScript provides:

```javascript
var
let
const
```

Modern JavaScript generally prefers:

```javascript
let
const
```

and avoids `var` unless there is a specific reason.

---

## `let`

```javascript
let age = 25;

age = 26;
```

The variable can be reassigned.

```text
let age
   │
   └── 25

age = 26

   │
   └── 26
```

---

## `const`

```javascript
const age = 25;
```

You cannot reassign the variable:

```javascript
const age = 25;

age = 30; // TypeError
```

But an important interview trap:

### `const` does NOT make an object immutable.

```javascript
const user = {
    name: "John"
};

user.name = "Mike";

console.log(user.name);
```

Output:

```text
Mike
```

Why?

Because `const` prevents **reassignment of the binding**.

It doesn't freeze the object.

```text
user
 │
 └──────► { name: "John" }

user = anotherObject     ❌

user.name = "Mike"       ✅
```

If you want an object to be frozen:

```javascript
Object.freeze(user);
```

But even `Object.freeze()` has important shallow-freeze semantics, which we'll cover later.

---

# 5. `var` vs `let` vs `const`

This is extremely common in interviews.

| Feature                       | `var`       | `let` | `const` |
| ----------------------------- | ----------- | ----- | ------- |
| Function scoped               | ✅           | ❌     | ❌       |
| Block scoped                  | ❌           | ✅     | ✅       |
| Reassign                      | ✅           | ✅     | ❌       |
| Redeclare same scope          | ✅           | ❌     | ❌       |
| Hoisted                       | Yes         | Yes   | Yes     |
| Accessible before declaration | `undefined` | ❌ TDZ | ❌ TDZ   |

Example:

```javascript
{
    let a = 10;
    const b = 20;
    var c = 30;
}

console.log(c); // 30
console.log(a); // ReferenceError
console.log(b); // ReferenceError
```

Why?

`let` and `const` are **block scoped**.

`var` is **function scoped**.

---

# 6. What is a block?

A block is code surrounded by `{}`.

```javascript
{
    let x = 10;
}
```

Common examples:

```javascript
if (condition) {
    // block
}

for (...) {
    // block
}

while (...) {
    // block
}
```

`let` and `const` respect these boundaries.

---

# 7. Data Types

JavaScript has **primitive values** and **objects**.

### Primitive types

There are 7 primitive types:

```text
string
number
bigint
boolean
undefined
symbol
null
```

Example:

```javascript
const name = "John";        // string
const age = 30;             // number
const large = 123n;         // bigint
const active = true;        // boolean
let value;                  // undefined
const id = Symbol("id");    // symbol
const data = null;          // null
```

Everything else is an object.

```javascript
const user = {};
const numbers = [];
const date = new Date();
```

---

# 8. Primitive vs Object

This distinction becomes extremely important later.

### Primitive

```javascript
let a = 10;
let b = a;

b = 20;

console.log(a); // 10
console.log(b); // 20
```

Conceptually:

```text
a ──► 10

b ──► 10

b = 20

a ──► 10
b ──► 20
```

---

### Object

```javascript
const a = {
    value: 10
};

const b = a;

b.value = 20;

console.log(a.value);
```

Output:

```text
20
```

Why?

Both variables refer to the same object.

```text
a ─────┐
       │
       ▼
   ┌──────────┐
   │ value:10 │
   └──────────┘
       ▲
       │
b ─────┘
```

Then:

```javascript
b.value = 20;
```

becomes:

```text
a ─────┐
       │
       ▼
   ┌──────────┐
   │ value:20 │
   └──────────┘
       ▲
       │
b ─────┘
```

This is why saying simply **"JavaScript passes objects by reference"** can be misleading.

A more accurate statement is:

> **JavaScript always passes arguments by value. When the value is an object, that value is a reference to the object.**

We'll cover this in detail when we discuss functions.

---

# 9. `typeof`

`typeof` tells you the type category of a value.

```javascript
typeof "hello";       // "string"
typeof 10;            // "number"
typeof true;          // "boolean"
typeof undefined;     // "undefined"
typeof 10n;           // "bigint"
typeof Symbol();      // "symbol"
typeof {};            // "object"
typeof [];            // "object"
typeof function() {}; // "function"
```

But there is a famous JavaScript historical quirk:

```javascript
typeof null
```

returns:

```text
"object"
```

This is a longstanding language behavior.

It does **not** mean `null` is actually an object.

Interview answer:

> `typeof null === "object"` is a historical behavior in JavaScript and should be treated as a known quirk.

---

# 10. `null` vs `undefined`

Another very common interview question.

### `undefined`

Usually means:

> A value has not been assigned / provided.

Example:

```javascript
let name;

console.log(name);
```

Result:

```text
undefined
```

---

### `null`

Usually means:

> The programmer intentionally represents the absence of a value.

```javascript
const user = null;
```

Conceptually:

```text
undefined
    ↓
value hasn't been provided

null
    ↓
there intentionally is no value
```

---

# 11. `number`

JavaScript has one main numeric primitive:

```javascript
number
```

It represents both integers and floating-point values.

```javascript
const a = 10;
const b = 10.5;
```

Both are:

```javascript
typeof a === "number"
typeof b === "number"
```

There is also:

```javascript
BigInt
```

for integers outside the safe integer range.

```javascript
const value = 123456789012345678901234567890n;
```

Notice the `n`.

---

# 12. `NaN`

`NaN` means:

> Not a Number

Example:

```javascript
const result = Number("hello");

console.log(result);
```

Output:

```text
NaN
```

Interestingly:

```javascript
typeof NaN
```

returns:

```text
"number"
```

Another interview trap:

```javascript
NaN === NaN
```

returns:

```text
false
```

Use:

```javascript
Number.isNaN(value)
```

to test specifically for `NaN`.

---

# 13. Equality

JavaScript has:

```javascript
==
```

and

```javascript
===
```

### `===`

Strict equality.

It compares without performing implicit type conversion.

```javascript
5 === "5"
```

Result:

```text
false
```

---

### `==`

Loose equality.

It can perform type coercion.

```javascript
5 == "5"
```

Result:

```text
true
```

For modern production JavaScript:

> Prefer `===` unless you have a deliberate reason to use `==`.

---

# 14. Type coercion

JavaScript sometimes automatically converts values.

Example:

```javascript
"5" + 2
```

Result:

```text
"52"
```

because `+` can mean string concatenation.

But:

```javascript
"5" - 2
```

Result:

```text
3
```

because `-` requires numeric operands, so JavaScript converts `"5"` to a number.

This is called **type coercion**.

---

# 15. Truthy and Falsy

JavaScript converts values to boolean in certain contexts.

Falsy values include:

```text
false
0
-0
0n
""
null
undefined
NaN
```

Almost everything else is truthy.

Example:

```javascript
if ("hello") {
    console.log("runs");
}
```

It runs because `"hello"` is truthy.

---

# 16. `||` vs `??`

Very common modern JavaScript interview topic.

### OR

```javascript
const value = input || "default";
```

Uses `"default"` if `input` is falsy.

Therefore:

```javascript
0 || 100
```

gives:

```text
100
```

But sometimes `0` is a valid value.

That's where `??` is useful.

---

### Nullish coalescing

```javascript
const value = input ?? "default";
```

It only falls back for:

```text
null
undefined
```

Example:

```javascript
0 ?? 100
```

Result:

```text
0
```

while:

```javascript
0 || 100
```

Result:

```text
100
```

### Interview rule

```text
||  → fallback for falsy values

??  → fallback only for null/undefined
```

---

# 17. Optional chaining

Modern JavaScript provides:

```javascript
?.
```

Instead of:

```javascript
if (user && user.address && user.address.city) {
    ...
}
```

you can write:

```javascript
user?.address?.city
```

If something along the chain is `null` or `undefined`, the expression safely returns `undefined`.

Example:

```javascript
const city = user?.address?.city;
```

This is extremely common in modern applications.

---

# 18. Interview Output Questions

### Question 1

```javascript
console.log(typeof null);
```

Answer:

```text
object
```

Historical JavaScript behavior.

---

### Question 2

```javascript
console.log(typeof []);
```

Answer:

```text
object
```

Arrays are objects.

To check an array:

```javascript
Array.isArray([]);
```

---

### Question 3

```javascript
console.log(5 == "5");
console.log(5 === "5");
```

Output:

```text
true
false
```

---

### Question 4

```javascript
console.log(0 || 10);
console.log(0 ?? 10);
```

Output:

```text
10
0
```

---

### Question 5

```javascript
console.log("5" + 2);
console.log("5" - 2);
```

Output:

```text
52
3
```

---

# 19. Chapter 1 — Interview Cheat Sheet

Remember these:

```text
JavaScript ≠ Browser
JavaScript ≠ Node.js
```

ECMAScript defines the language; environments provide additional APIs.

```text
var   → function scoped
let   → block scoped
const → block scoped
```

```text
Primitive:
string
number
bigint
boolean
undefined
symbol
null
```

```text
typeof null === "object"
```

is a historical quirk.

```text
=== → strict equality
==  → type coercion may occur
```

```text
|| → falsy fallback
?? → null/undefined fallback
```

```text
?. → optional chaining
```

And one particularly important interview statement:

> **JavaScript is dynamically typed, meaning variable types are determined at runtime and a variable can hold values of different types during its lifetime.**

For example:

```javascript
let value = 10;

value = "hello";

value = true;
```

This is valid JavaScript.

---

## What comes next

**Chapter 2 — Functions, Scope & Closures** is one of the **highest-value JavaScript interview chapters**. We'll cover:

```text
Function declarations vs expressions
Arrow functions
Parameters / arguments
Lexical scope
Global / function / block scope
Scope chain
Hoisting
Temporal Dead Zone
Closures
this
call / apply / bind
```

Especially **closures + `this` + hoisting**, which are frequent interview areas.

