# Chapter 3 — Objects, Prototypes & Classes

This is one of the most important JavaScript internals topics.

A common mistake is to learn:

```javascript
class User { ... }
```

without understanding what JavaScript is actually doing underneath.

The key mental model is:

```text
Object
  ↓
Properties
  ↓
Prototype
  ↓
Prototype chain
  ↓
Constructor function
  ↓
new
  ↓
class syntax
  ↓
Inheritance
```

---

# 1. What is an Object?

An object is a collection of **properties**.

```javascript
const user = {
    name: "John",
    age: 30
};
```

Conceptually:

```text
user
 │
 ▼
┌──────────────────┐
│ name → "John"    │
│ age  → 30        │
└──────────────────┘
```

Properties can contain:

* primitive values
* objects
* arrays
* functions

For example:

```javascript
const user = {
    name: "John",
    age: 30,

    greet() {
        console.log("Hello");
    }
};
```

A function stored as an object property is commonly called a **method**.

---

# 2. Accessing Properties

Two common forms:

### Dot notation

```javascript
user.name;
```

### Bracket notation

```javascript
user["name"];
```

Bracket notation is useful when the property name is dynamic.

```javascript
const property = "name";

console.log(user[property]);
```

Output:

```text
John
```

This would **not** work the same way:

```javascript
user.property
```

because that looks for a property literally named `"property"`.

---

# 3. Adding and Deleting Properties

JavaScript objects are generally mutable.

```javascript
const user = {
    name: "John"
};

user.age = 30;
```

Now:

```text
user
 ├── name → John
 └── age  → 30
```

You can also delete:

```javascript
delete user.age;
```

---

# 4. Property Keys

Object property keys are generally:

```text
string
symbol
```

For example:

```javascript
const user = {
    name: "John",
    10: "hello"
};
```

The numeric-looking key is effectively represented as a string property key.

```javascript
user[10];
user["10"];
```

refer to the same property.

Symbols are different:

```javascript
const id = Symbol("id");

const user = {
    [id]: 123
};
```

Symbols are useful for creating unique property keys.

---

# 5. Objects Are Dynamic

You don't have to define every property when creating the object.

```javascript
const user = {};

user.name = "John";
user.age = 30;
```

JavaScript objects are dynamically extensible unless restricted using mechanisms such as:

```javascript
Object.preventExtensions()
Object.seal()
Object.freeze()
```

We'll focus on the important distinctions later.

---

# 6. Object References

Consider:

```javascript
const user1 = {
    name: "John"
};

const user2 = user1;

user2.name = "Mike";

console.log(user1.name);
```

Output:

```text
Mike
```

Why?

Both variables contain a reference to the same object.

```text
user1 ─────┐
           │
           ▼
      ┌─────────────┐
      │ name: Mike  │
      └─────────────┘
           ▲
           │
user2 ─────┘
```

This leads to an important interview statement:

> JavaScript is pass-by-value. For objects, the value being passed is a reference to the object.

---

# 7. Object Equality

Consider:

```javascript
const a = { name: "John" };
const b = { name: "John" };

console.log(a === b);
```

Output:

```text
false
```

Why?

They are two different objects.

```text
a ──► Object A { name: John }

b ──► Object B { name: John }
```

Even though their contents are identical:

```text
Object A ≠ Object B
```

Now:

```javascript
const a = { name: "John" };
const b = a;

console.log(a === b);
```

Output:

```text
true
```

Both refer to the same object.

---

# 8. The Prototype

Now we reach the core concept.

Every ordinary JavaScript object has an internal link to another object called its **prototype**.

Conceptually:

```text
object
   │
   │ [[Prototype]]
   ▼
prototype object
```

The prototype is used primarily for **property and method lookup**.

---

# 9. Example: `toString()`

Consider:

```javascript
const user = {
    name: "John"
};

console.log(user.toString());
```

Where did `toString()` come from?

We didn't define it.

```javascript
const user = {
    name: "John"
};
```

Yet this works.

Why?

JavaScript searches the object's prototype chain.

Conceptually:

```text
user
 │
 │ toString not found
 ▼
Object.prototype
 │
 │ toString found
 ▼
Function
```

So:

```javascript
user.toString()
```

works because `toString` exists on `Object.prototype`.

---

# 10. Property Lookup

When you execute:

```javascript
user.name
```

JavaScript conceptually performs:

```text
1. Search user itself
2. If found → return it
3. Otherwise search user.__proto__
4. Continue through prototype chain
5. If nothing found → undefined
```

Example:

```javascript
const user = {
    name: "John"
};

console.log(user.name);
console.log(user.toString);
```

For `name`:

```text
user
 └── name → found
```

For `toString`:

```text
user
 │
 └── not found
      ↓
Object.prototype
 │
 └── toString → found
```

---

# 11. Prototype Chain

The chain can look like:

```text
user
  │
  ▼
User.prototype
  │
  ▼
Object.prototype
  │
  ▼
null
```

The final prototype is `null`.

So the lookup eventually reaches:

```text
null
```

and stops.

---

# 12. `__proto__` vs `prototype`

This is a **very common interview question**.

They are not the same thing.

### `prototype`

A property commonly found on **constructor functions**.

```javascript
function User() {}

console.log(User.prototype);
```

It is an object that can become the prototype of objects created using:

```javascript
new User()
```

---

### `__proto__`

Historically exposed accessor for an object's internal `[[Prototype]]`.

```javascript
const user = {};

console.log(user.__proto__);
```

It points to:

```text
Object.prototype
```

A better modern API for accessing it is:

```javascript
Object.getPrototypeOf(user);
```

and:

```javascript
Object.setPrototypeOf(...)
```

for changing it, although changing prototypes dynamically is generally avoided for performance/design reasons.

---

# 13. The Most Important Prototype Relationship

Consider:

```javascript
function User(name) {
    this.name = name;
}

const user = new User("John");
```

The important relationship is:

```text
User
 │
 └── prototype ─────► User.prototype
                         ▲
                         │
                         │ [[Prototype]]
                         │
                      user
```

So:

```javascript
Object.getPrototypeOf(user) === User.prototype
```

is:

```text
true
```

This is the relationship you should remember.

---

# 14. Constructor Functions

Before `class` syntax became common, JavaScript used constructor functions heavily.

```javascript
function User(name, age) {
    this.name = name;
    this.age = age;
}
```

Then:

```javascript
const user1 = new User("John", 30);
const user2 = new User("Mike", 25);
```

Each call creates a new object.

```text
user1 ──► { name: John, age: 30 }
user2 ──► { name: Mike, age: 25 }
```

---

# 15. What Does `new` Actually Do?

This is an excellent interview question.

When you execute:

```javascript
const user = new User("John");
```

conceptually, `new` performs several steps.

### Step 1 — Create a new object

```text
newObject = {}
```

### Step 2 — Connect it to the constructor's prototype

```text
newObject
    │
    ▼
User.prototype
```

### Step 3 — Call the constructor with `this`

```javascript
User.call(newObject, "John");
```

Conceptually:

```text
this → newObject
```

### Step 4 — Return the object

So:

```javascript
const user = new User("John");
```

roughly means:

```text
create object
     ↓
set prototype
     ↓
bind this
     ↓
execute constructor
     ↓
return object
```

---

# 16. Why Put Methods on the Prototype?

Consider:

```javascript
function User(name) {
    this.name = name;

    this.greet = function () {
        console.log(this.name);
    };
}
```

If you create 1,000 users:

```text
user1 → own greet function
user2 → own greet function
user3 → own greet function
...
```

That's unnecessary duplication.

Instead:

```javascript
function User(name) {
    this.name = name;
}

User.prototype.greet = function () {
    console.log(this.name);
};
```

Now:

```text
User.prototype
       │
       └── greet()
       ▲
       │
 ┌─────┴─────┐
 │           │
user1      user2
```

Both objects reuse the same method.

This is one of the primary purposes of prototypes.

---

# 17. Example

```javascript
function User(name) {
    this.name = name;
}

User.prototype.greet = function () {
    return `Hello ${this.name}`;
};

const user1 = new User("John");
const user2 = new User("Mike");

console.log(user1.greet());
console.log(user2.greet());
```

When:

```javascript
user1.greet()
```

JavaScript searches:

```text
user1
  ↓
not found
  ↓
User.prototype
  ↓
greet found
```

---

# 18. `hasOwnProperty`

You can determine whether a property exists **directly on the object**.

```javascript
const user = {
    name: "John"
};

console.log(user.hasOwnProperty("name"));
```

Output:

```text
true
```

But:

```javascript
console.log(user.hasOwnProperty("toString"));
```

returns:

```text
false
```

because `toString` is inherited from `Object.prototype`.

Modern code can also use:

```javascript
Object.hasOwn(user, "name");
```

which is generally preferable.

---

# 19. Prototype vs Own Property

Consider:

```javascript
function User(name) {
    this.name = name;
}

User.prototype.greet = function () {
    console.log("Hello");
};

const user = new User("John");
```

Now:

```text
user
 └── name        ← own property

User.prototype
 └── greet       ← inherited property
```

Therefore:

```javascript
user.hasOwnProperty("name");  // true
user.hasOwnProperty("greet"); // false
```

---

# 20. Classes

Modern JavaScript provides `class` syntax.

```javascript
class User {
    constructor(name) {
        this.name = name;
    }

    greet() {
        return `Hello ${this.name}`;
    }
}
```

Then:

```javascript
const user = new User("John");
```

This looks object-oriented like Java/C++.

But an important interview point:

> **JavaScript classes are primarily syntactic sugar over JavaScript's prototype-based object model.**

Classes don't replace prototypes.

They provide a cleaner syntax for working with them.

---

# 21. Where Does a Class Method Go?

Consider:

```javascript
class User {
    constructor(name) {
        this.name = name;
    }

    greet() {
        return `Hello ${this.name}`;
    }
}
```

When:

```javascript
const user = new User("John");
```

`name` is an own property:

```text
user
 └── name
```

But `greet()` is on:

```text
User.prototype
```

Conceptually:

```text
user
 │
 ▼
User.prototype
 └── greet()
```

So:

```javascript
user.greet();
```

still uses the prototype chain.

---

# 22. `extends`

JavaScript classes support inheritance.

```javascript
class Animal {
    speak() {
        console.log("Animal sound");
    }
}

class Dog extends Animal {
    bark() {
        console.log("Woof");
    }
}
```

Now:

```javascript
const dog = new Dog();

dog.bark();
dog.speak();
```

Both work.

Prototype relationship:

```text
dog
 │
 ▼
Dog.prototype
 │
 ▼
Animal.prototype
 │
 ▼
Object.prototype
 │
 ▼
null
```

This is the actual prototype chain underneath the class syntax.

---

# 23. `super`

A subclass can call the parent implementation using `super`.

```javascript
class Animal {
    speak() {
        console.log("Animal sound");
    }
}

class Dog extends Animal {
    speak() {
        super.speak();
        console.log("Woof");
    }
}
```

Calling:

```javascript
const dog = new Dog();

dog.speak();
```

produces:

```text
Animal sound
Woof
```

---

# 24. Constructor with `extends`

Important interview rule:

If a derived class has a constructor, it must call:

```javascript
super(...)
```

before using `this`.

Example:

```javascript
class Animal {
    constructor(name) {
        this.name = name;
    }
}

class Dog extends Animal {
    constructor(name, breed) {
        super(name);
        this.breed = breed;
    }
}
```

Why?

The parent constructor is responsible for initializing the derived instance's `this` in the subclass construction process.

---

# 25. `instanceof`

`instanceof` checks whether an object's prototype chain contains a constructor's `prototype`.

```javascript
class User {}

const user = new User();

console.log(user instanceof User);
```

Output:

```text
true
```

Conceptually:

```text
user
 ↓
User.prototype  ← found
```

Therefore:

```text
user instanceof User
```

is true.

---

# 26. `instanceof` and Inheritance

Consider:

```javascript
class Animal {}

class Dog extends Animal {}

const dog = new Dog();
```

Then:

```javascript
dog instanceof Dog
```

is:

```text
true
```

And:

```javascript
dog instanceof Animal
```

is also:

```text
true
```

Because:

```text
dog
 ↓
Dog.prototype
 ↓
Animal.prototype  ← found
```

---

# 27. `Object.create()`

Another important way to create an object:

```javascript
const userPrototype = {
    greet() {
        console.log("Hello");
    }
};

const user = Object.create(userPrototype);
```

Now:

```text
user
 │
 ▼
userPrototype
```

Therefore:

```javascript
user.greet();
```

works.

`Object.create()` is useful when you explicitly want to control the prototype of an object.

---

# 28. `Object.create(null)`

An interesting interview case:

```javascript
const map = Object.create(null);
```

This creates an object with:

```text
[[Prototype]] → null
```

So it doesn't inherit from:

```text
Object.prototype
```

Therefore:

```javascript
map.toString
```

is:

```text
undefined
```

This can be useful for dictionary-like objects where inherited properties are undesirable.

Today, `Map` is often a better choice for general-purpose key/value collections.

---

# 29. `Object.freeze()`, `seal()`, `preventExtensions()`

These are sometimes asked in interviews.

### `Object.preventExtensions()`

Prevents adding new properties.

```javascript
const user = {
    name: "John"
};

Object.preventExtensions(user);
```

You cannot add new properties.

Existing properties can generally still be changed or deleted.

---

### `Object.seal()`

Prevents:

```text
adding properties
deleting properties
```

Existing properties can generally still be modified.

---

### `Object.freeze()`

Prevents modification of existing own data properties and adding/deleting properties.

```javascript
const user = {
    name: "John"
};

Object.freeze(user);

user.name = "Mike";
```

The assignment doesn't change the property in the normal frozen-object behavior.

Important:

> `Object.freeze()` is **shallow**.

Example:

```javascript
const user = {
    address: {
        city: "Hyderabad"
    }
};

Object.freeze(user);

user.address.city = "Bangalore";
```

The nested object can still be changed because only the outer object was frozen.

---

# 30. Object Descriptors

Every property has metadata called a **property descriptor**.

For example:

```javascript
const user = {
    name: "John"
};

console.log(Object.getOwnPropertyDescriptor(user, "name"));
```

Conceptually:

```text
value
writable
enumerable
configurable
```

For a normal object literal property:

```text
writable     → true
enumerable   → true
configurable → true
```

This becomes useful when understanding:

```javascript
Object.defineProperty()
```

and property behavior.

---

# 31. Getters and Setters

Objects can define computed property access.

```javascript
const user = {
    firstName: "John",
    lastName: "Doe",

    get fullName() {
        return `${this.firstName} ${this.lastName}`;
    }
};
```

Then:

```javascript
console.log(user.fullName);
```

Notice:

```text
user.fullName
```

not:

```text
user.fullName()
```

The getter behaves like a property.

Setter:

```javascript
const user = {
    firstName: "John",
    lastName: "Doe",

    set fullName(value) {
        const [first, last] = value.split(" ");
        this.firstName = first;
        this.lastName = last;
    }
};
```

Then:

```javascript
user.fullName = "Mike Smith";
```

---

# 32. Important Interview Output

What does this print?

```javascript
const person = {
    name: "John"
};

console.log(person.toString);
```

It prints a function because:

```text
person
  ↓
Object.prototype
  ↓
toString
```

But:

```javascript
console.log(person.hasOwnProperty("toString"));
```

prints:

```text
false
```

because `toString` isn't an own property.

---

# 33. Another Important Question

```javascript
function User() {}

const user = new User();

console.log(user.__proto__ === User.prototype);
```

Answer:

```text
true
```

More formally, prefer:

```javascript
Object.getPrototypeOf(user) === User.prototype
```

---

# 34. Class vs Prototype — Interview Answer

If asked:

> "Does JavaScript support classes?"

Say:

> Yes. JavaScript provides `class` syntax, but its inheritance model is prototype-based. Class methods are generally placed on the class's prototype, and instances delegate property lookup through the prototype chain.

If asked:

> "Are JavaScript classes real classes?"

A good nuanced answer:

> They are real language syntax with class-specific semantics, but the underlying object inheritance mechanism is still prototype-based.

Don't simply say:

> "Classes are fake."

That's too simplistic.

---

# 35. Most Important Mental Model

When you see:

```javascript
const user = new User("John");

user.greet();
```

think:

```text
                  User
                   │
                   │ prototype
                   ▼
              User.prototype
                   │
                   │
                   ▲
                   │ [[Prototype]]
                   │
                  user
```

Then property lookup:

```text
user.greet
   │
   ├── Is greet on user?
   │      ↓ no
   │
   ├── Is greet on User.prototype?
   │      ↓ yes
   │
   └── use it
```

That's the heart of JavaScript's prototype system.

---

# Interview Cheat Sheet

```text
Object
→ collection of properties

Prototype
→ object used for delegated property/method lookup

Prototype chain
→ object → prototype → prototype → ... → null

prototype
→ property associated with constructor functions/classes

__proto__
→ legacy accessor for an object's [[Prototype]]

Object.getPrototypeOf()
→ modern way to inspect [[Prototype]]

new
→ creates object, links prototype, binds this, runs constructor

class
→ class syntax built on JavaScript's prototype model

extends
→ establishes inheritance relationship

super
→ accesses parent-class behavior

instanceof
→ checks whether constructor.prototype exists in object's prototype chain

Object.freeze()
→ shallowly freezes an object

Object.create()
→ creates an object with a specified prototype
```

### The three relationships to memorize

```text
const user = new User();

user
 │
 │ [[Prototype]]
 ▼
User.prototype
```

```text
User.prototype
 │
 │ [[Prototype]]
 ▼
Object.prototype
```

```text
Object.prototype
 │
 │ [[Prototype]]
 ▼
null
```

These relationships explain a huge amount of JavaScript behavior.

**Next: Chapter 4 — Arrays & Modern JavaScript**, covering destructuring, spread/rest, `map`, `filter`, `reduce`, `find`, `some`, `every`, sorting, copying, and the interview traps around shallow vs deep copying.

