# Chapter 2 — Functions, Scope & Closures

This is one of the **most important JavaScript interview chapters**. If you understand this chapter well, many JavaScript output questions become much easier.

We'll build it in this order:

```text
Functions
   ↓
Scope
   ↓
Lexical Scope
   ↓
Scope Chain
   ↓
Hoisting
   ↓
TDZ
   ↓
Closures
   ↓
this
   ↓
call / apply / bind
```

---

# 1. What is a function?

A function is a reusable block of code that can receive inputs and optionally produce an output.

```javascript
function add(a, b) {
    return a + b;
}

const result = add(10, 20);

console.log(result); // 30
```

Conceptually:

```text
              add(10, 20)
                  │
                  ▼
          ┌─────────────────┐
          │ function add    │
          │                 │
          │ a = 10          │
          │ b = 20          │
          │                 │
          │ return a + b    │
          └────────┬────────┘
                   │
                   ▼
                  30
```

---

# 2. Function Declaration

```javascript
function add(a, b) {
    return a + b;
}
```

This is a **function declaration**.

A key property:

```javascript
console.log(add(2, 3));

function add(a, b) {
    return a + b;
}
```

This works.

Output:

```text
5
```

Why?

Function declarations are hoisted in a way that makes the function available before its textual declaration.

We'll examine hoisting shortly.

---

# 3. Function Expression

A function can also be assigned to a variable.

```javascript
const add = function (a, b) {
    return a + b;
};
```

This is a **function expression**.

Now:

```javascript
console.log(add(2, 3));

const add = function (a, b) {
    return a + b;
};
```

does **not** work.

You get a `ReferenceError`.

Why?

Because the variable `add` is declared using `const`, and its binding is in the **Temporal Dead Zone** until initialization.

---

# 4. Arrow Functions

Modern JavaScript commonly uses arrow functions.

```javascript
const add = (a, b) => {
    return a + b;
};
```

If there is a single expression:

```javascript
const add = (a, b) => a + b;
```

These are equivalent in terms of returned value.

---

## Important difference

Arrow functions are **not simply shorter syntax for normal functions**.

The most important difference:

> **Arrow functions do not have their own `this`.**

They also don't have their own:

* `arguments`
* `super`
* `new.target`

We'll focus heavily on `this` later.

---

# 5. Parameters vs Arguments

This is a common interview terminology question.

```javascript
function add(a, b) {
    return a + b;
}

add(10, 20);
```

Here:

```text
a, b
```

are **parameters**.

```text
10, 20
```

are **arguments**.

So:

> Parameters are variables defined by the function. Arguments are the actual values passed to the function.

---

# 6. Default Parameters

JavaScript supports default parameter values.

```javascript
function greet(name = "Guest") {
    return `Hello ${name}`;
}

greet();
```

Result:

```text
Hello Guest
```

But:

```javascript
greet(undefined);
```

also uses the default.

Whereas:

```javascript
greet(null);
```

does not.

```text
undefined → default value used
null      → null remains null
```

---

# 7. Rest Parameters

A function can collect remaining arguments using `...`.

```javascript
function sum(...numbers) {
    return numbers.reduce((total, n) => total + n, 0);
}

sum(1, 2, 3, 4);
```

Here:

```text
numbers = [1, 2, 3, 4]
```

Important:

```javascript
function sum(...numbers) {}
```

`numbers` is a real array.

---

# 8. Scope

Scope answers:

> **Where can a variable be accessed?**

Consider:

```javascript
const globalValue = 10;

function test() {
    const localValue = 20;

    console.log(globalValue); // 10
    console.log(localValue);  // 20
}

test();

console.log(globalValue); // 10
console.log(localValue);  // ReferenceError
```

`globalValue` is accessible inside the function.

`localValue` is not accessible outside.

---

# 9. Types of Scope

For interview purposes, understand these three:

```text
Global Scope
Function Scope
Block Scope
```

---

## Global Scope

```javascript
const name = "John";

function greet() {
    console.log(name);
}
```

`name` is globally accessible within its applicable scope.

---

## Function Scope

Variables declared with `var` are function scoped.

```javascript
function test() {
    var x = 10;

    if (true) {
        var y = 20;
    }

    console.log(y); // 20
}
```

The `if` block does not create a separate `var` scope.

---

## Block Scope

`let` and `const` are block scoped.

```javascript
if (true) {
    let x = 10;
    const y = 20;
}

console.log(x); // ReferenceError
console.log(y); // ReferenceError
```

The `{}` creates the boundary.

---

# 10. Lexical Scope

This is extremely important.

JavaScript uses **lexical scoping**.

That means:

> A function's accessible variables are determined by where the function is **defined**, not where it is called.

Example:

```javascript
const x = 10;

function outer() {
    const x = 20;

    function inner() {
        console.log(x);
    }

    return inner;
}

const fn = outer();

fn();
```

Output:

```text
20
```

Why?

`inner` was defined inside `outer`.

Therefore it can access `outer`'s `x`.

---

# 11. Scope Chain

Suppose we have:

```javascript
const a = 10;

function outer() {
    const b = 20;

    function inner() {
        const c = 30;

        console.log(a);
        console.log(b);
        console.log(c);
    }

    inner();
}
```

When `inner()` tries to access `a`:

```text
inner scope
    │
    │ a not found
    ▼
outer scope
    │
    │ a not found
    ▼
global scope
    │
    ▼
a = 10
```

This is the **scope chain**.

JavaScript searches outward through lexical environments.

```text
inner
  ↓
outer
  ↓
global
```

It does **not** search based on where the function was called.

---

# 12. Scope vs Call Stack

This distinction is frequently tested.

### Scope

Determines:

> Which variables can this code access?

### Call stack

Determines:

> Which functions are currently executing?

They are related but different concepts.

For example:

```javascript
function outer() {
    function inner() {
        console.log("Hello");
    }

    inner();
}

outer();
```

Call stack:

```text
inner()
outer()
global
```

Scope relationship:

```text
inner
 ↓
outer
 ↓
global
```

The call stack is about **execution**.

The scope chain is about **variable lookup**.

---

# 13. Shadowing

A nested scope can define a variable with the same name.

```javascript
const x = 10;

function test() {
    const x = 20;

    console.log(x);
}

test();
```

Output:

```text
20
```

The inner `x` **shadows** the outer `x`.

Conceptually:

```text
global
  x = 10

   ↓

test
  x = 20  ← found first
```

JavaScript stops searching once it finds the variable.

---

# 14. Hoisting

One of the most important JavaScript interview topics.

A simplified explanation:

> During creation of the execution environment, JavaScript establishes bindings for declarations before executing the code.

But **different declarations behave differently**.

---

## `var`

```javascript
console.log(x);

var x = 10;
```

Output:

```text
undefined
```

Conceptually:

```javascript
var x;

console.log(x);

x = 10;
```

The declaration is available, but initialization with `10` happens later.

---

# 15. `let` and `const` Hoisting

Consider:

```javascript
console.log(x);

let x = 10;
```

You get:

```text
ReferenceError
```

Same for:

```javascript
console.log(x);

const x = 10;
```

This sometimes leads to the incorrect statement:

> `let` and `const` are not hoisted.

That's not the best explanation.

A better interview answer:

> `let` and `const` declarations are hoisted in the sense that their bindings are created during environment setup, but they remain uninitialized and inaccessible until execution reaches their declaration. This period is called the Temporal Dead Zone.

---

# 16. Temporal Dead Zone — TDZ

Consider:

```javascript
{
    // TDZ begins

    console.log(x); // ReferenceError

    let x = 10;

    // TDZ ends
}
```

Conceptually:

```text
Block starts
     │
     ▼
x binding exists
but uninitialized
     │
     │ TDZ
     │
     ▼
let x = 10
     │
     ▼
x initialized
```

So:

```text
var
 ↓
binding initialized with undefined

let / const
 ↓
binding exists
 ↓
uninitialized
 ↓
TDZ
 ↓
initialization
```

---

# 17. Function Declaration Hoisting

Consider:

```javascript
sayHello();

function sayHello() {
    console.log("Hello");
}
```

Works.

Function declarations are made available during environment setup.

Compare this:

```javascript
sayHello();

const sayHello = function () {
    console.log("Hello");
};
```

This throws:

```text
ReferenceError
```

because `sayHello` is a `const` binding in the TDZ.

---

# 18. Closures

Now we reach one of the **most important JavaScript concepts**.

A closure happens when a function **retains access to variables from its lexical scope even after the outer function has finished executing**.

Example:

```javascript
function createCounter() {
    let count = 0;

    return function () {
        count++;
        return count;
    };
}

const counter = createCounter();

console.log(counter()); // 1
console.log(counter()); // 2
console.log(counter()); // 3
```

At first this looks surprising.

`createCounter()` has already returned.

So why does `count` still exist?

Because the returned function closes over `count`.

---

# 19. Closure Visualization

Initially:

```text
createCounter()
      │
      ▼
┌──────────────────┐
│ count = 0        │
│                  │
│ returned fn ─────┼──────┐
└──────────────────┘      │
                          │
                          ▼
                     counter
```

After `createCounter()` returns, the normal execution of the function is finished.

But:

```text
counter → function → count
```

The function still has access to `count`.

Therefore:

```javascript
counter();
```

changes the captured value:

```text
count = 1
```

Then:

```javascript
counter();
```

changes it to:

```text
count = 2
```

---

# 20. Why do closures matter?

Closures are heavily used for:

### Data privacy

```javascript
function createBankAccount() {
    let balance = 0;

    return {
        deposit(amount) {
            balance += amount;
        },

        getBalance() {
            return balance;
        }
    };
}
```

`balance` isn't directly exposed.

```javascript
const account = createBankAccount();

account.deposit(100);

console.log(account.getBalance()); // 100
```

There is no:

```javascript
account.balance
```

that directly accesses the internal variable.

---

### Callbacks

Closures are everywhere in callback-based programming.

```javascript
function greet(name) {
    return function () {
        console.log(`Hello ${name}`);
    };
}

const greeting = greet("John");

greeting();
```

The inner function remembers `name`.

---

### Event handlers

```javascript
function setupButton(message) {
    button.addEventListener("click", () => {
        console.log(message);
    });
}
```

The callback retains access to `message`.

---

# 21. Classic Closure Interview Question

```javascript
function createCounter() {
    let count = 0;

    return () => ++count;
}

const a = createCounter();
const b = createCounter();

console.log(a());
console.log(a());
console.log(b());
console.log(a());
```

Output:

```text
1
2
1
3
```

Why?

`a` and `b` have **different closure environments**.

```text
a ──► closure ──► count = 0

b ──► closure ──► count = 0
```

They don't share the same `count`.

---

# 22. Another Classic Closure Question

```javascript
for (var i = 0; i < 3; i++) {
    setTimeout(() => {
        console.log(i);
    }, 100);
}
```

Output:

```text
3
3
3
```

Why?

`var` creates one function-scoped binding.

Conceptually:

```text
             ┌──────────────┐
callbacks ──►│      i       │
             │              │
             │ final = 3    │
             └──────────────┘
```

All callbacks access the same `i`.

---

Now:

```javascript
for (let i = 0; i < 3; i++) {
    setTimeout(() => {
        console.log(i);
    }, 100);
}
```

Output:

```text
0
1
2
```

Why?

`let` provides block-scoped bindings for each iteration.

This is a very common interview question.

---

# 23. What is `this`?

`this` is another major interview topic.

A useful definition:

> **`this` is a special value whose value is determined according to how a function is invoked, with important differences for arrow functions.**

Consider:

```javascript
const user = {
    name: "John",

    greet() {
        console.log(this.name);
    }
};

user.greet();
```

Output:

```text
John
```

Here:

```text
this → user
```

because the function was called as:

```javascript
user.greet()
```

---

# 24. `this` is NOT determined by where the function is defined

This is a common misconception.

```javascript
const user = {
    name: "John",

    greet() {
        console.log(this.name);
    }
};

const fn = user.greet;

fn();
```

`fn()` is no longer called as:

```javascript
user.greet()
```

It is called as:

```javascript
fn()
```

Therefore `this` is different.

In strict mode, it is:

```text
undefined
```

---

# 25. The Four Important `this` Cases

For normal functions, remember these invocation patterns.

### 1. Method call

```javascript
obj.method();
```

Generally:

```text
this → obj
```

---

### 2. Plain function call

```javascript
function test() {
    console.log(this);
}

test();
```

In strict mode:

```text
this → undefined
```

---

### 3. Constructor call

```javascript
function User(name) {
    this.name = name;
}

const user = new User("John");
```

`new` creates a new object and:

```text
this → newly created object
```

---

### 4. Explicit binding

```javascript
function greet() {
    console.log(this.name);
}

greet.call({ name: "John" });
```

Output:

```text
John
```

We'll discuss `call`, `apply`, and `bind` shortly.

---

# 26. Arrow Functions and `this`

Arrow functions behave differently.

They **do not create their own `this`**.

Instead, they capture `this` from the surrounding lexical context.

Example:

```javascript
const user = {
    name: "John",

    greet: () => {
        console.log(this.name);
    }
};
```

Don't use arrow functions for object methods when you need the object's `this`.

Prefer:

```javascript
const user = {
    name: "John",

    greet() {
        console.log(this.name);
    }
};
```

---

# 27. Why Arrow Functions Are Useful

They are particularly useful for callbacks.

```javascript
const user = {
    name: "John",

    greet() {
        setTimeout(() => {
            console.log(this.name);
        }, 1000);
    }
};
```

The arrow function captures `this` from `greet()`.

Conceptually:

```text
greet()
  │
  │ this = user
  │
  ▼
arrow callback
  │
  └── captures same this
```

---

# 28. `call`

`call()` allows you to explicitly specify `this`.

```javascript
function greet() {
    console.log(this.name);
}

const user = {
    name: "John"
};

greet.call(user);
```

Output:

```text
John
```

Syntax:

```javascript
function.call(thisArg, arg1, arg2, ...)
```

Example:

```javascript
function greet(message) {
    console.log(message, this.name);
}

greet.call(user, "Hello");
```

---

# 29. `apply`

`apply()` is similar to `call()`.

The main difference is how arguments are supplied.

```javascript
greet.call(user, "Hello");
```

versus:

```javascript
greet.apply(user, ["Hello"]);
```

### Remember

```text
call
→ arguments individually

apply
→ arguments as array-like collection
```

---

# 30. `bind`

`bind()` doesn't immediately execute the function.

Instead, it creates a **new function with `this` permanently bound** to the supplied value.

```javascript
function greet() {
    console.log(this.name);
}

const user = {
    name: "John"
};

const boundGreet = greet.bind(user);

boundGreet();
```

Output:

```text
John
```

Comparison:

```text
call()
→ executes immediately

apply()
→ executes immediately

bind()
→ returns a new function
```

This distinction is extremely interview-friendly.

---

# 31. The Most Important Mental Model

For normal functions, when you see:

```javascript
something();
```

ask:

> **How was this function called?**

Not:

> Where was this function written?

For example:

```javascript
obj.method();
```

Think:

```text
method called through obj
        ↓
this = obj
```

But:

```javascript
const fn = obj.method;

fn();
```

Think:

```text
plain function call
        ↓
this depends on strict/non-strict mode
```

Arrow functions are the exception because they use lexical `this`.

---

# 32. Interview Questions You Should Be Able to Answer

### Q1. What is lexical scope?

> Lexical scope means variable accessibility is determined by where code is written in the source code, not by where a function is called.

### Q2. What is a closure?

> A closure is a function together with access to variables from its surrounding lexical environment, allowing those variables to remain accessible even after the outer function has returned.

### Q3. Are `let` and `const` hoisted?

> Their bindings are created during environment setup, but they remain uninitialized in the Temporal Dead Zone until execution reaches the declaration.

### Q4. Difference between `var`, `let`, and `const`?

> `var` is function-scoped, while `let` and `const` are block-scoped. `let` can be reassigned, `const` cannot be reassigned. `let` and `const` are subject to the TDZ.

### Q5. Difference between arrow and normal functions?

Key points:

```text
Arrow:
- lexical this
- no own arguments
- cannot be used with new
- no own super/new.target
```

Normal functions can have their own `this` depending on invocation and can be used as constructors.

### Q6. Difference between call/apply/bind?

```text
call  → invoke now, arguments separately
apply → invoke now, arguments as collection
bind  → return new bound function
```

---

# 33. High-Value Output Question

What is the output?

```javascript
var x = 10;

function test() {
    console.log(x);

    var x = 20;

    console.log(x);
}

test();
```

Answer:

```text
undefined
20
```

Why?

Inside `test`, `var x` is function scoped and its declaration is hoisted.

Conceptually:

```javascript
function test() {
    var x;

    console.log(x); // undefined

    x = 20;

    console.log(x); // 20
}
```

The global `x = 10` is not used because the local `x` shadows it.

---

# 34. Another Important Output Question

```javascript
let x = 10;

function test() {
    console.log(x);

    let x = 20;
}

test();
```

Output:

```text
ReferenceError
```

Why?

The local `x` shadows the outer `x`, but it is in the TDZ until:

```javascript
let x = 20;
```

Therefore JavaScript does **not** fall back to the outer `x`.

```text
test scope

x → exists but uninitialized
│
│ TDZ
│
console.log(x) ← ReferenceError
```

---

# Chapter 2 — Interview Cheat Sheet

```text
Function declaration
→ hoisted and callable before declaration

Function expression
→ variable rules apply

let / const
→ block scoped

var
→ function scoped

Lexical scope
→ determined by where code is written

Scope chain
→ current scope → outer scope → global

Closure
→ function retains access to surrounding lexical variables

TDZ
→ period where let/const binding exists but is uninitialized

this
→ depends on invocation for normal functions

Arrow this
→ lexical; inherited from surrounding scope

call
→ invoke with explicit this

apply
→ invoke with explicit this + arguments collection

bind
→ create a new function with bound this
```

**The next chapter is Chapter 3 — Objects, Prototypes & Classes**, where we'll cover one of JavaScript's most important underlying mechanisms:

```text
Objects
   ↓
Property lookup
   ↓
Prototype
   ↓
Prototype chain
   ↓
Object.create()
   ↓
Constructor functions
   ↓
new
   ↓
class
   ↓
extends / super
   ↓
Inheritance
```

This chapter is particularly important for **JavaScript/Frontend interviews** because `class` syntax hides a lot of what is actually happening underneath.

