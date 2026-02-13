Yess 🔥 this is another **interview favorite** (and also the root cause of 50% JS bugs 😄).

Let’s go step-by-step and very clearly.

---

# ✅ Scopes in JavaScript (Types)

JS has mainly these scopes:

## 1️⃣ Global Scope

Declared outside any function/block.

```js
let a = 10;
function f() {
  console.log(a); // 10
}
```

---

## 2️⃣ Function Scope

Variables declared inside a function are only accessible inside it.

```js
function test() {
  var x = 5;
}
console.log(x); // ❌ ReferenceError
```

---

## 3️⃣ Block Scope (`{}`)

Introduced properly with `let` and `const`.

```js
{
  let x = 10;
  const y = 20;
}
console.log(x); // ❌ ReferenceError
console.log(y); // ❌ ReferenceError
```

---

## 4️⃣ Lexical Scope (Very Important)

Inner functions can access outer variables.

```js
function outer() {
  let a = 10;
  function inner() {
    console.log(a); // 10
  }
  inner();
}
outer();
```

This is the foundation of **closures**.

---

# ✅ Now var vs let vs const (Core Differences)

| Feature                    | var                              | let           | const         |
| -------------------------- | -------------------------------- | ------------- | ------------- |
| Scope                      | Function-scoped                  | Block-scoped  | Block-scoped  |
| Hoisting                   | Yes (initialized as `undefined`) | Yes (but TDZ) | Yes (but TDZ) |
| Redeclare allowed?         | ✅ Yes                            | ❌ No          | ❌ No          |
| Reassign allowed?          | ✅ Yes                            | ✅ Yes         | ❌ No          |
| Attached to global object? | ✅ Yes (window/globalThis)        | ❌ No          | ❌ No          |

---

# 🔥 1) Scope Behavior Differences

---

## ✅ var is Function Scoped (NOT block scoped)

```js
if (true) {
  var x = 10;
}
console.log(x); // ✅ 10
```

Because `var` ignores `{}` blocks.

---

## ✅ let/const are Block Scoped

```js
if (true) {
  let x = 10;
  const y = 20;
}

console.log(x); // ❌ ReferenceError
console.log(y); // ❌ ReferenceError
```

Because `let/const` are limited to that block.

---

# 🔥 2) Hoisting Behavior

---

## ✅ var hoisting (initialized with undefined)

```js
console.log(a); // undefined
var a = 10;
```

Internally, JS treats it like:

```js
var a;
console.log(a);
a = 10;
```

---

## ✅ let/const hoisting (TDZ concept)

```js
console.log(a); // ❌ ReferenceError
let a = 10;
```

Even though `let` is hoisted, it is in **Temporal Dead Zone (TDZ)** until initialization.

---

# 🧠 Temporal Dead Zone (TDZ)

TDZ = the time between:

* entering scope
* and variable initialization line

Example:

```js
{
  console.log(x); // ❌ ReferenceError (TDZ)
  let x = 10;
}
```

---

# 🔥 3) Redeclaration Rules

---

## ✅ var allows redeclare (dangerous)

```js
var a = 10;
var a = 20;
console.log(a); // 20
```

This causes many accidental bugs.

---

## ❌ let does not allow redeclare in same scope

```js
let a = 10;
let a = 20; // ❌ SyntaxError
```

---

## ❌ const also does not allow redeclare

```js
const a = 10;
const a = 20; // ❌ SyntaxError
```

---

# 🔥 4) Reassignment Rules

---

## ✅ var allows reassignment

```js
var a = 10;
a = 20; // ok
```

---

## ✅ let allows reassignment

```js
let a = 10;
a = 20; // ok
```

---

## ❌ const does not allow reassignment

```js
const a = 10;
a = 20; // ❌ TypeError
```

---

# 🔥 Important: const with objects/arrays

Many people misunderstand this.

```js
const obj = { name: "Riyaz" };

obj.name = "Pasha"; // ✅ allowed
obj.age = 25;       // ✅ allowed

obj = {};           // ❌ not allowed
```

So:

✅ const prevents changing the reference
❌ does NOT freeze the object

To freeze:

```js
Object.freeze(obj);
```

---

# 🔥 5) Global Scope Behavior (window/globalThis)

In browser:

```js
var a = 10;
let b = 20;

console.log(window.a); // 10
console.log(window.b); // undefined
```

Because:

* `var` becomes a property on global object
* `let/const` do not

---

# 🔥 var in loops (classic closure bug)

---

## ❌ Problem with var

```js
for (var i = 0; i < 3; i++) {
  setTimeout(() => console.log(i), 0);
}
```

Output:

```
3
3
3
```

Why?

Because `var i` is ONE variable shared across loop.
When timeout runs, loop already finished and i=3.

---

## ✅ let fixes it

```js
for (let i = 0; i < 3; i++) {
  setTimeout(() => console.log(i), 0);
}
```

Output:

```
0
1
2
```

Because `let` creates a new `i` for each loop iteration (block scope).

---

# ✅ Scope Summary Table

| Scope Type       | var             | let             | const           |
| ---------------- | --------------- | --------------- | --------------- |
| Global scope     | Works           | Works           | Works           |
| Function scope   | Works           | Works           | Works           |
| Block scope `{}` | ❌ Not respected | ✅ respected     | ✅ respected     |
| Loop scope       | ❌ shared        | ✅ per iteration | ✅ per iteration |

---

# 🎯 Best Practices (Industry Standard)

✅ Use `const` by default
✅ Use `let` only when reassignment is needed
❌ Avoid `var` completely (unless legacy code)

---

# 🔥 Interview One-Liner

If interviewer asks: *"Difference between var, let and const?"*

Say:

> var is function-scoped and hoisted with undefined.
> let and const are block-scoped and hoisted into the temporal dead zone.
> const prevents reassignment, not mutation.

That line is gold.

---

Great question — this is where JS scope becomes *slightly different* from other languages.

JavaScript has these important scope types:

---

# ✅ 1. Global Scope

### Meaning:

Variables declared outside any function/block/module are in **global scope**.

### Example:

```js
var a = 10;
let b = 20;
const c = 30;

console.log(a, b, c); // accessible anywhere
```

### Important:

* In browsers, `var` becomes part of `window`
* `let/const` do NOT become part of `window`

```js
var x = 1;
let y = 2;

console.log(window.x); // 1
console.log(window.y); // undefined
```

---

# ✅ 2. Script Scope (Special JS Scope)

This is a **very important modern concept**.

### Meaning:

When you write JS inside a `<script>` tag (non-module), `let` and `const` are NOT truly global-object properties.

They are scoped to the **script** itself.

Example:

```js
let a = 10;
const b = 20;
```

They are accessible globally in that file, but not as:

```js
window.a // undefined
window.b // undefined
```

But `var` is different:

```js
var x = 5;
window.x // 5
```

📌 So:

* `var` → global object scope
* `let/const` → script scope

---

# ✅ 3. Block Scope

### Meaning:

A block is `{ }`, like in:

* if block
* for loop
* while loop
* try/catch

Only `let` and `const` are block scoped.

Example:

```js
if (true) {
  let a = 10;
  const b = 20;
  var c = 30;
}

console.log(c); // ✅ 30
console.log(a); // ❌ ReferenceError
console.log(b); // ❌ ReferenceError
```

So:

* `let/const` live inside the block
* `var` escapes the block

---

# ✅ 4. Function Scope (Local Scope)

This is what people usually call **local scope**.

### Meaning:

Anything declared inside a function is local to that function.

Example:

```js
function test() {
  var a = 10;
  let b = 20;
  const c = 30;
}

console.log(a); // ❌ error
```

📌 Key point:
Inside a function, all of these are function-local:

* var
* let
* const

---

# ✅ 5. Lexical Scope (Scope Chain)

### Meaning:

Inner functions can access outer function variables.

Example:

```js
function outer() {
  let x = 10;

  function inner() {
    console.log(x); // ✅ 10
  }

  inner();
}
outer();
```

This is the basis of **closures**.

---

# ✅ 6. Module Scope (ES Modules)

If your file is an ES module:

```js
// file.js
let x = 10;
```

This is **NOT global**, it is **module scoped**.

Even `var` does not become global here.

Modules create their own isolated scope.

---

# ✅ 7. Catch Scope (Special Block Scope)

In `try/catch`, the catch variable has its own scope.

```js
try {
  throw new Error("oops");
} catch (err) {
  console.log(err.message); // ok
}

console.log(err); // ❌ ReferenceError
```

---

# ✅ Summary Table (Best Cheat Sheet)

| Scope Type             | Created By            | Applies To         |
| ---------------------- | --------------------- | ------------------ |
| Global Scope           | outside everything    | var/let/const      |
| Script Scope           | `<script>` file scope | let/const          |
| Block Scope            | `{}` blocks           | let/const          |
| Function Scope (Local) | `function(){}`        | var/let/const      |
| Lexical Scope          | nested functions      | scope chain lookup |
| Module Scope           | ES module files       | var/let/const      |
| Catch Scope            | `catch(err)`          | catch variable     |

---
