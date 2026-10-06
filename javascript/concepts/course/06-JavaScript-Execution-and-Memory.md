# Chapter 6 — JavaScript Execution & Memory

This chapter connects many things we've already learned:

```text
Variables
Functions
Scope
Closures
Hoisting
Promises
Call Stack
Garbage Collection
```

The goal is to understand **what JavaScript is actually doing internally when your code runs**.

This is especially useful for interview questions like:

* What is an Execution Context?
* What is the Call Stack?
* How does hoisting work internally?
* What is a Lexical Environment?
* Stack vs Heap?
* How do closures retain variables?
* When does garbage collection happen?
* Can closures cause memory leaks?

---

# 1. The Big Picture

When JavaScript runs:

```js
const x = 10;

function add(a, b) {
    return a + b;
}

const result = add(2, 3);
```

JavaScript doesn't simply read the file line-by-line and execute everything blindly.

Conceptually, it creates **execution contexts**.

```text
JavaScript Program
       │
       ▼
Create Global Execution Context
       │
       ▼
Execute global code
       │
       ▼
Function called
       │
       ▼
Create Function Execution Context
       │
       ▼
Execute function
       │
       ▼
Function returns
       │
       ▼
Function context removed
```

The two most important concepts are:

```text
Execution Context
Call Stack
```

---

# 2. What Is an Execution Context?

An **execution context** is the environment in which JavaScript code is evaluated and executed.

You can think of it as:

> A runtime structure containing the information JavaScript needs to execute a particular piece of code.

It includes things conceptually related to:

```text
Variables
Functions
Scope
`this`
Outer lexical environment
```

There are several kinds of execution contexts, but for normal application code, the important ones are:

```text
Global Execution Context
Function Execution Context
```

Modern JavaScript also has execution contexts associated with modules and other language/runtime mechanisms, but those aren't the first things to focus on for interviews.

---

# 3. Global Execution Context

When your JavaScript program starts, a **Global Execution Context** is created.

Example:

```js
const name = "John";

function greet() {
    console.log("Hello");
}

greet();
```

Initially:

```text
┌────────────────────────────────┐
│ Global Execution Context       │
│                                │
│ name → "John"                  │
│ greet → function               │
│                                │
│ this → global this value       │
│ outer environment → none       │
└────────────────────────────────┘
```

The global context remains for the lifetime of the JavaScript environment.

---

# 4. Function Execution Context

When you call:

```js
function add(a, b) {
    const result = a + b;
    return result;
}

add(10, 20);
```

JavaScript creates a new execution context for that invocation.

```text
Global Context
      │
      │ add(10, 20)
      ▼
Function Context
      │
      ├── a = 10
      ├── b = 20
      └── result = 30
```

When the function returns, its execution context is removed from the call stack.

---

# 5. The Call Stack

Execution contexts are managed using the **call stack**.

Example:

```js
function first() {
    second();
}

function second() {
    third();
}

function third() {
    console.log("Hello");
}

first();
```

The stack evolves like this.

Initially:

```text
┌─────────────────┐
│ Global Context  │
└─────────────────┘
```

Call:

```js
first();
```

```text
┌─────────────────┐
│ first()         │
├─────────────────┤
│ Global Context  │
└─────────────────┘
```

Then:

```js
second();
```

```text
┌─────────────────┐
│ second()        │
├─────────────────┤
│ first()         │
├─────────────────┤
│ Global Context  │
└─────────────────┘
```

Then:

```js
third();
```

```text
┌─────────────────┐
│ third()         │
├─────────────────┤
│ second()        │
├─────────────────┤
│ first()         │
├─────────────────┤
│ Global Context  │
└─────────────────┘
```

Then `third()` finishes:

```text
┌─────────────────┐
│ second()        │
├─────────────────┤
│ first()         │
├─────────────────┤
│ Global Context  │
└─────────────────┘
```

Then `second()` finishes:

```text
┌─────────────────┐
│ first()         │
├─────────────────┤
│ Global Context  │
└─────────────────┘
```

Finally:

```text
┌─────────────────┐
│ Global Context  │
└─────────────────┘
```

This is why function calls behave like **LIFO**:

> Last In, First Out.

---

# 6. Execution Context Creation and Execution

A useful interview model is to think about execution in two broad stages:

```text
Creation Phase
      ↓
Execution Phase
```

For example:

```js
console.log(x);

var x = 10;
```

Before executing the statements, JavaScript establishes the necessary bindings for the scope.

Conceptually:

```text
Creation Phase:

x → undefined
```

Then execution begins:

```text
console.log(x);
```

prints:

```text
undefined
```

Then:

```js
x = 10;
```

updates the binding.

---

# 7. `var` Hoisting

Example:

```js
console.log(x);

var x = 10;
```

Conceptually:

```text
Creation:
x → undefined

Execution:
console.log(x) → undefined
x = 10
```

So:

```text
undefined
```

---

# 8. `let` and `const` Hoisting

This is where interviewers often test your understanding.

Consider:

```js
console.log(x);

let x = 10;
```

Result:

```text
ReferenceError
```

It's incorrect to simply say:

> `let` and `const` are not hoisted.

A better answer is:

> Their bindings are created during environment setup, but they remain uninitialized until execution reaches their declaration. Accessing them before initialization causes a `ReferenceError`. This period is called the Temporal Dead Zone.

Conceptually:

```text
Creation:

x → uninitialized
       │
       │ TDZ
       │
       ▼
let x = 10
       │
       ▼
x → 10
```

---

# 9. Function Declaration Hoisting

Consider:

```js
sayHello();

function sayHello() {
    console.log("Hello");
}
```

This works.

Conceptually, during environment setup:

```text
sayHello → function object
```

So when execution reaches:

```js
sayHello();
```

the function is already available.

---

# 10. Lexical Environment

This is one of the more important internal concepts.

A **Lexical Environment** is a specification-level concept used to represent the association between identifiers and their values, along with access to an outer environment.

Think:

```text
Lexical Environment
│
├── Variables
├── Function declarations
└── Reference to outer environment
```

Example:

```js
const x = 10;

function test() {
    const y = 20;

    console.log(x + y);
}
```

Conceptually:

```text
Global Lexical Environment
│
├── x → 10
├── test → function
│
└── outer → null

        ↓

Function Lexical Environment
│
├── y → 20
│
└── outer → Global Environment
```

Therefore `test()` can find `x`.

---

# 11. Scope Chain

When JavaScript encounters:

```js
console.log(x);
```

it searches for `x` through the lexical environments.

Example:

```js
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

outer();
```

Lookup for `c`:

```text
inner environment
      ↓
found c
```

Lookup for `b`:

```text
inner environment
      ↓
outer environment
      ↓
found b
```

Lookup for `a`:

```text
inner environment
      ↓
outer environment
      ↓
global environment
      ↓
found a
```

This is the **scope chain**.

---

# 12. Scope Chain vs Call Stack

This distinction is extremely important.

### Call Stack

Describes:

> Which functions are currently executing.

### Scope Chain

Describes:

> Where variables can be looked up based on lexical nesting.

They are related but **not the same thing**.

Example:

```js
const x = "global";

function outer() {
    const x = "outer";

    function inner() {
        console.log(x);
    }

    return inner;
}

const fn = outer();

fn();
```

At the time `fn()` executes:

```text
Call Stack:

fn()
Global
```

But its lexical environment still points to:

```text
inner
 ↓
outer
 ↓
global
```

That's the foundation of closures.

---

# 13. Closures

We already introduced closures in Chapter 2. Now let's understand them from the execution/memory perspective.

Consider:

```js
function createCounter() {
    let count = 0;

    return function () {
        count++;
        return count;
    };
}

const counter = createCounter();

console.log(counter());
console.log(counter());
```

Output:

```text
1
2
```

The surprising part:

`createCounter()` has already returned.

Why does `count` still exist?

Because the returned function maintains access to the lexical environment where `count` was defined.

Conceptually:

```text
counter
  │
  ▼
Function
  │
  │ [[Environment]]
  ▼
createCounter Environment
  │
  └── count → 2
```

This is a closure.

---

# 14. Closure ≠ Copy of Variables

A common misconception:

> The inner function gets a copy of `count`.

Not necessarily.

The function retains access to the relevant lexical environment/binding.

So:

```js
const counter = createCounter();

counter();
counter();
```

updates the same captured `count`.

```text
count
 ↓
0
 ↓
1
 ↓
2
```

---

# 15. Multiple Closures Have Separate Environments

Consider:

```js
const counter1 = createCounter();
const counter2 = createCounter();
```

You get:

```text
counter1
   │
   ▼
Environment A
count → 0

counter2
   │
   ▼
Environment B
count → 0
```

Therefore:

```js
counter1(); // 1
counter1(); // 2

counter2(); // 1
```

The counters are independent.

---

# 16. Stack vs Heap

This is one of the most common interview topics, but it needs careful wording.

You'll often hear:

```text
Primitive → Stack
Object → Heap
```

That's a useful simplified mental model, but **it is not an exact ECMAScript rule**.

JavaScript specifies language semantics, not a universal requirement that every primitive must live on a machine stack or every object must live on a heap.

A practical conceptual model is:

```text
Call Stack
────────────────
Execution state
Function calls
Local bindings/references

Heap
────────────────
Objects
Functions
Arrays
Other dynamically allocated data
```

For example:

```js
const user = {
    name: "John"
};
```

Conceptually:

```text
Stack / execution state

user
 │
 └───────────────┐
                 │
                 ▼
              Heap
         ┌──────────────┐
         │ name: "John" │
         └──────────────┘
```

The important interview answer is:

> The ECMAScript specification doesn't mandate a specific stack/heap layout. Engines use implementation-specific memory strategies. The stack/heap distinction is a useful conceptual model.

That answer is much more accurate.

---

# 17. Primitive vs Object References

Consider:

```js
let a = 10;
let b = a;

b = 20;

console.log(a);
console.log(b);
```

Output:

```text
10
20
```

The values are independent.

Now:

```js
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
a ───────┐
         │
         ▼
      Object
      value: 20
         ▲
         │
b ───────┘
```

---

# 18. Functions Are Objects Too

In JavaScript, functions are objects.

Therefore:

```js
function greet() {
    console.log("Hello");
}
```

The function itself can have properties.

```js
greet.message = "hello";

console.log(greet.message);
```

This is possible because functions are first-class objects.

Conceptually:

```text
greet
  │
  ▼
Function Object
├── executable behavior
├── properties
└── prototype-related properties depending on function kind
```

---

# 19. Garbage Collection

JavaScript automatically manages memory using **garbage collection**.

The fundamental idea:

> Memory that is no longer reachable from the program's roots can eventually be reclaimed.

You don't normally manually free objects like in C.

Example:

```js
function createUser() {
    return {
        name: "John"
    };
}

let user = createUser();

user = null;
```

After:

```js
user = null;
```

if nothing else references that object, it becomes eligible for garbage collection.

Conceptually:

```text
Before:

user ───────→ Object
              │
              └── name: "John"


After:

user → null

Object
   ↑
   │
no references
```

The object is now **unreachable**.

---

# 20. Garbage Collection Is About Reachability

Don't say:

> Garbage collection happens when a variable goes out of scope.

That's too simplistic.

A better explanation:

> An object can be reclaimed when it is no longer reachable through the references that the runtime considers roots.

Typical conceptual roots include things such as:

```text
Global references
Currently executing contexts
Active runtime references
Other implementation-defined roots
```

---

# 21. Mark-and-Sweep

A common garbage collection strategy is **mark-and-sweep**.

Conceptually:

### Step 1 — Start from roots

```text
Roots
 │
 ├── global objects
 ├── active execution state
 └── other reachable references
```

### Step 2 — Mark reachable objects

```text
Root
 │
 ▼
Object A
 │
 ▼
Object B
```

Both are marked.

### Step 3 — Sweep unreachable objects

```text
Object C
Object D
```

If nothing can reach them, their memory can be reclaimed.

Diagram:

```text
Roots
  │
  ▼
Object A ──→ Object B

Object C       ← unreachable
Object D       ← unreachable
```

After collection:

```text
Object C
Object D
```

can be reclaimed.

---

# 22. Closures and Garbage Collection

Now connect closures with memory.

Consider:

```js
function createCounter() {
    let count = 0;

    return function () {
        return ++count;
    };
}

const counter = createCounter();
```

Even though `createCounter()` has finished, the captured environment is still needed by `counter`.

Therefore:

```text
counter
   │
   ▼
closure
   │
   ▼
environment
   │
   └── count
```

That environment remains reachable.

Once:

```js
counter = null;
```

assuming there are no other references to the closure/environment, the associated data can eventually become collectible.

---

# 23. Closures Can Cause Memory Leaks

Closures aren't inherently bad.

But unnecessarily retaining large objects can cause memory problems.

Example:

```js
function createHandler() {
    const hugeData = new Array(1_000_000).fill("data");

    return function () {
        console.log(hugeData.length);
    };
}

const handler = createHandler();
```

The closure retains access to `hugeData`.

As long as `handler` remains reachable:

```text
handler
   │
   ▼
closure
   │
   ▼
hugeData
```

the large data may remain reachable too.

The important principle:

> A closure can keep its captured objects reachable for as long as the closure itself remains reachable.

---

# 24. Real-World Memory Leak Example

A classic browser problem is an event listener.

Conceptually:

```js
function setup() {
    const largeData = createLargeData();

    button.addEventListener("click", () => {
        console.log(largeData);
    });
}
```

The event listener closure references:

```text
largeData
```

If the listener remains registered unnecessarily, it may keep captured data alive.

Good lifecycle management matters:

```text
Create listener
      ↓
Use listener
      ↓
Remove listener when no longer needed
```

For example:

```js
button.addEventListener("click", handler);

// later
button.removeEventListener("click", handler);
```

This is one reason cleanup is important in long-lived applications.

---

# 25. Another Common Memory Leak Pattern

Suppose you continually add objects to a global array:

```js
const cache = [];

function addData(data) {
    cache.push(data);
}
```

If the array keeps growing forever:

```text
Global
  │
  ▼
cache
  │
  ├── object
  ├── object
  ├── object
  ├── object
  ├── ...
```

Those objects remain reachable.

Garbage collection cannot reclaim them simply because you aren't actively using them.

This is a **logical memory leak**.

The lesson:

> Garbage collection can only reclaim unreachable objects. If your program accidentally keeps references to objects, GC cannot know that you no longer need them.

---

# 26. Common Causes of Memory Leaks

Interview-friendly list:

```text
1. Unbounded global collections
2. Forgotten event listeners
3. Long-lived timers/intervals
4. Closures retaining unnecessary data
5. Caches without eviction
6. Detached DOM structures still referenced
7. Unnecessary long-lived subscriptions
```

Modern frameworks provide lifecycle mechanisms to help manage these resources, but the underlying principle is always **reachability + lifecycle**.

---

# 27. Execution Trace Example

Consider:

```js
var x = 10;

function test() {
    var x = 20;

    function inner() {
        console.log(x);
    }

    inner();
}

test();
```

Let's trace it.

### Global context

```text
Global
├── x = 10
└── test = function
```

Call:

```js
test();
```

New context:

```text
test context
├── x = 20
└── inner = function
```

Call:

```js
inner();
```

New context:

```text
inner context
```

Lookup:

```text
x?
 ↓
inner environment → not found
 ↓
test environment → x = 20
```

Output:

```text
20
```

Notice:

> Variable lookup follows lexical nesting, not simply the current call stack.

---

# 28. Interview Trick: Function Called From Somewhere Else

Consider:

```js
const x = "global";

function outer() {
    const x = "outer";

    return function inner() {
        console.log(x);
    };
}

const fn = outer();

function another() {
    const x = "another";
    fn();
}

another();
```

What is printed?

```text
outer
```

Not:

```text
another
```

Why?

Because `inner` was **defined inside `outer`**.

Its lexical environment is based on where it was created, not where it is eventually called.

This is lexical scoping.

---

# 29. Execution Context vs Lexical Environment

Interviewers sometimes ask these as if they're interchangeable.

They aren't exactly the same concept.

### Execution Context

The broader runtime context in which code executes.

It includes execution-related state such as:

```text
Lexical environment
Variable environment
`this` binding
Other execution state
```

### Lexical Environment

A specification concept representing:

```text
Identifier bindings
+
Reference to outer lexical environment
```

For everyday interview discussions:

```text
Execution Context
        │
        ├── Lexical Environment
        ├── Variable Environment
        └── other execution state
```

This is a useful mental model.

---

# 30. Important Correction About "Stack Memory"

You may see interview explanations saying:

```text
Primitive → stack
Object → heap
```

Don't present this as a JavaScript language rule.

Instead say:

> JavaScript engines commonly use stack-like structures for execution state and heap-like storage for dynamically allocated objects, but ECMAScript does not prescribe this physical memory layout.

That's technically stronger.

---

# 31. Interview Questions

### Q1. What is an Execution Context?

**Answer:**

> An execution context is the runtime environment in which JavaScript code is evaluated and executed. It contains execution-related state such as lexical environments, variable bindings, and `this` information.

---

### Q2. What is the Call Stack?

> The call stack is a LIFO structure used by the JavaScript execution machinery to keep track of active execution contexts/function calls.

---

### Q3. What is a Lexical Environment?

> A lexical environment is a specification-level structure that associates identifiers with their bindings and maintains a reference to an outer lexical environment.

---

### Q4. What is the Scope Chain?

> The scope chain is the chain of lexical environments JavaScript follows when resolving an identifier.

---

### Q5. What is a Closure?

> A closure is a function together with its retained access to the lexical environment in which it was created.

---

### Q6. Why does a closure retain variables after a function returns?

Because the returned function still references the lexical environment containing those variables.

---

### Q7. What is garbage collection?

> Automatic reclamation of memory occupied by objects that are no longer reachable.

---

### Q8. Can JavaScript have memory leaks despite garbage collection?

Yes.

If the program accidentally retains references to objects that it no longer needs, those objects remain reachable and cannot be reclaimed.

---

### Q9. Does `let` get hoisted?

Best interview answer:

> Its binding is created during environment setup, but it remains uninitialized until execution reaches its declaration. Access during that period causes a `ReferenceError` because of the Temporal Dead Zone.

---

### Q10. Is an object always stored on the heap?

Don't claim that as an ECMAScript guarantee.

Say:

> The specification doesn't define physical memory placement. JavaScript engines generally use heap-like storage for objects, but the exact implementation is engine-dependent.

---

# 32. The Complete Mental Model

At this point, connect the previous chapters:

```text
                    JavaScript Program
                           │
                           ▼
                 Execution Context
                           │
                           ▼
                      Call Stack
                           │
                           ▼
                Lexical Environments
                           │
                ┌──────────┴──────────┐
                ▼                     ▼
             Variables            Functions
                                      │
                                      ▼
                                  Closures
                                      │
                                      ▼
                              Captured Environment
                                      │
                                      ▼
                               Object Reachability
                                      │
                                      ▼
                              Garbage Collection
```

And asynchronous execution:

```text
                     JavaScript
                         │
                         ▼
                    Call Stack
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
        Runtime APIs          Promise machinery
              │                     │
              ▼                     ▼
         Task Queue           Microtask Queue
              │                     │
              └──────────┬──────────┘
                         ▼
                     Event Loop
```

---

# Chapter 6 Cheat Sheet

| Concept             | Key idea                                            |
| ------------------- | --------------------------------------------------- |
| Execution Context   | Environment where code executes                     |
| Global Context      | Created for global code                             |
| Function Context    | Created for function invocation                     |
| Call Stack          | LIFO execution structure                            |
| Lexical Environment | Identifier bindings + outer reference               |
| Scope Chain         | Lexical environment lookup chain                    |
| Closure             | Function retaining lexical access                   |
| Hoisting            | Bindings established during environment setup       |
| TDZ                 | Period where `let`/`const` binding is uninitialized |
| Heap                | Useful conceptual model for dynamic objects         |
| Stack               | Useful conceptual model for execution state         |
| GC                  | Reclaims unreachable objects                        |
| Memory leak         | Unwanted references keep objects reachable          |

### Three rules worth memorizing

```text
1. Scope is determined lexically — where code is defined.

2. Closures retain access to their lexical environment.

3. Garbage collection is based on reachability, not simply
   whether a variable "looks unused".
```

---

## Next — Chapter 7: Modules & Error Handling

We'll cover the core production/interview concepts:

```text
ES Modules
├── import / export
├── named vs default exports
├── CommonJS vs ES Modules
├── module scope
└── dynamic import()

Error Handling
├── try / catch / finally
├── throw
├── Error objects
├── custom errors
├── async error handling
├── Promise rejection
└── common error-handling mistakes
```

Then we'll move into the final **Advanced JavaScript Interview Topics** and output/trick questions.

