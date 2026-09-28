# Topic 7 — Java 8+: Functional Interfaces & Lambda Expressions

This is one of the **most important Java interview topics** because Java 8 introduced a major shift toward functional-style programming.

For EPAM, you should understand not only the syntax:

```java
x -> x * 2
```

but **why it works, what a functional interface is, how Java infers types, and how lambdas connect to Streams**.

---

# 1. Why were Lambdas introduced?

Before Java 8, if you wanted to pass behavior to a method, you typically needed an anonymous class.

For example, suppose we want to sort employees by salary.

Before Java 8:

```java
Collections.sort(employees, new Comparator<Employee>() {
    @Override
    public int compare(Employee e1, Employee e2) {
        return Double.compare(e1.getSalary(), e2.getSalary());
    }
});
```

Java 8 allows:

```java
employees.sort(
    (e1, e2) -> Double.compare(e1.getSalary(), e2.getSalary())
);
```

Much more concise.

The important idea is:

> A lambda allows us to represent behavior as a value and pass that behavior around.

---

# 2. What is a Lambda Expression?

A lambda is an anonymous function-like expression.

Basic syntax:

```text
(parameters) -> expression
```

or:

```text
(parameters) -> {
    statements;
}
```

Examples:

```java
() -> System.out.println("Hello")
```

```java
x -> x * 2
```

```java
(a, b) -> a + b
```

```java
(name) -> {
    System.out.println("Hello " + name);
}
```

---

# 3. Lambda with one parameter

You can omit parentheses around a single parameter:

```java
x -> x * 2
```

This is equivalent to:

```java
(x) -> x * 2
```

But with multiple parameters:

```java
(a, b) -> a + b
```

parentheses are required.

---

# 4. Lambda with a block

Single expression:

```java
x -> x * 2
```

The result is automatically returned.

But with a block:

```java
x -> {
    int result = x * 2;
    return result;
}
```

You need an explicit `return`.

So:

```java
x -> x * 2
```

is effectively equivalent to:

```java
x -> {
    return x * 2;
}
```

---

# 5. But where can we use a Lambda?

This is the key question.

You cannot simply do:

```java
Object x = a -> a + 1;
```

Java needs to know **what kind of function** the lambda represents.

For example:

```java
Function<Integer, Integer> f = x -> x + 1;
```

Here Java knows:

```text
input  → Integer
output → Integer
```

because of:

```java
Function<Integer, Integer>
```

This leads directly to:

# Functional Interfaces

---

# 6. What is a Functional Interface?

A functional interface is an interface with **exactly one abstract method**.

Example:

```java
@FunctionalInterface
interface Calculator {
    int calculate(int a, int b);
}
```

Now we can write:

```java
Calculator calculator =
        (a, b) -> a + b;
```

And:

```java
System.out.println(
    calculator.calculate(10, 20)
);
```

Output:

```text
30
```

---

# 7. Why does the lambda know what `a` and `b` are?

Because the target type is:

```java
Calculator
```

which says:

```java
int calculate(int a, int b);
```

So Java knows:

```text
a → int
b → int
return → int
```

This is called **target typing**.

The lambda itself doesn't have a standalone type in the same way an object expression does; its target functional interface provides the context.

---

# 8. `@FunctionalInterface`

You can write:

```java
@FunctionalInterface
interface Calculator {
    int calculate(int a, int b);
}
```

The annotation tells the compiler:

> This interface is intended to have exactly one abstract method.

If you accidentally add another abstract method:

```java
@FunctionalInterface
interface Calculator {
    int calculate(int a, int b);

    int multiply(int a, int b);
}
```

the compiler reports an error.

### Important

`@FunctionalInterface` is not what makes an interface functional.

The interface is functional because it has exactly one abstract method.

The annotation simply allows the compiler to verify that intention.

---

# 9. Can a Functional Interface have other methods?

Yes.

This is a common interview question.

It can have:

* exactly one abstract method
* any number of `default` methods
* any number of `static` methods
* methods inherited from `Object` don't count as additional abstract methods for this purpose

Example:

```java
@FunctionalInterface
interface Calculator {

    int calculate(int a, int b);

    default void print() {
        System.out.println("Calculator");
    }

    static void info() {
        System.out.println("Utility");
    }
}
```

Still functional.

---

# 10. Built-in Functional Interfaces

Java provides many functional interfaces in:

```java
java.util.function
```

The four most important ones are:

```text
Predicate
Function
Consumer
Supplier
```

You should know these extremely well.

---

# 11. Predicate

`Predicate<T>` represents:

```text
T → boolean
```

It answers a question.

Example:

```java
Predicate<Integer> isEven =
        x -> x % 2 == 0;
```

Usage:

```java
System.out.println(isEven.test(10));
```

Output:

```text
true
```

The method is:

```java
boolean test(T t)
```

Think:

> Predicate = condition/check.

---

# 12. Function

`Function<T, R>` represents:

```text
T → R
```

It takes one value and produces another.

Example:

```java
Function<String, Integer> length =
        s -> s.length();
```

Usage:

```java
int result = length.apply("Java");
```

Result:

```text
4
```

Method:

```java
R apply(T t)
```

Think:

> Function = transform something.

---

# 13. Consumer

`Consumer<T>` represents:

```text
T → nothing
```

It consumes a value and doesn't return anything.

Example:

```java
Consumer<String> printer =
        s -> System.out.println(s);
```

Usage:

```java
printer.accept("Hello");
```

Method:

```java
void accept(T t)
```

Think:

> Consumer = take something and perform an action.

---

# 14. Supplier

`Supplier<T>` represents:

```text
nothing → T
```

It supplies a value.

Example:

```java
Supplier<Double> random =
        () -> Math.random();
```

Usage:

```java
System.out.println(random.get());
```

Method:

```java
T get()
```

Think:

> Supplier = produce something.

---

# 15. Easy way to memorize

Remember:

```text
Predicate
T → boolean
"Is it valid?"

Function
T → R
"Transform it"

Consumer
T → void
"Do something with it"

Supplier
() → T
"Give me something"
```

This is extremely useful in interviews.

---

# 16. Example combining them

Suppose:

```java
List<String> names =
        List.of("Alice", "Bob", "Andrew");
```

Predicate:

```java
Predicate<String> startsWithA =
        name -> name.startsWith("A");
```

Function:

```java
Function<String, Integer> getLength =
        String::length;
```

Consumer:

```java
Consumer<String> print =
        System.out::println;
```

These concepts become especially powerful with Streams.

---

# 17. Method References

Instead of:

```java
name -> System.out.println(name)
```

you can write:

```java
System.out::println
```

This is called a **method reference**.

It is basically a shorter way of expressing certain lambdas.

---

# 18. Types of Method References

There are four main forms.

### 1. Static method

```java
ClassName::staticMethod
```

Example:

```java
Function<String, Integer> parser =
        Integer::parseInt;
```

Equivalent lambda:

```java
s -> Integer.parseInt(s)
```

---

### 2. Instance method on a particular object

```java
object::method
```

Example:

```java
Consumer<String> printer =
        System.out::println;
```

Equivalent:

```java
s -> System.out.println(s)
```

---

### 3. Instance method on an arbitrary object of a type

```java
ClassName::instanceMethod
```

Example:

```java
Function<String, Integer> length =
        String::length;
```

Equivalent:

```java
s -> s.length()
```

---

### 4. Constructor reference

```java
ClassName::new
```

Example:

```java
Supplier<ArrayList<String>> supplier =
        ArrayList::new;
```

Equivalent:

```java
() -> new ArrayList<>()
```

---

# 19. Lambda vs Anonymous Class

Before Java 8:

```java
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Hello");
    }
};
```

Java 8:

```java
Runnable r =
        () -> System.out.println("Hello");
```

Much shorter.

But there's an important difference involving `this`.

---

# 20. `this` inside Lambda

Consider:

```java
class Employee {

    void process() {

        Runnable r = () -> {
            System.out.println(this);
        };

        r.run();
    }
}
```

Inside the lambda:

```java
this
```

refers to the enclosing `Employee` object.

A lambda does **not create a new `this` context**.

---

# 21. Anonymous class `this`

An anonymous class is different:

```java
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println(this);
    }
};
```

Here:

```java
this
```

refers to the anonymous class instance.

This is a common deeper interview question.

---

# 22. Variable capture

Consider:

```java
int multiplier = 10;

Function<Integer, Integer> f =
        x -> x * multiplier;
```

This works.

The lambda can capture a local variable if that variable is:

> final or effectively final.

---

# 23. What does effectively final mean?

Suppose:

```java
int multiplier = 10;

Function<Integer, Integer> f =
        x -> x * multiplier;
```

We never change:

```java
multiplier
```

so Java considers it **effectively final**.

We could explicitly write:

```java
final int multiplier = 10;
```

and it still works.

But this doesn't:

```java
int multiplier = 10;

multiplier = 20;

Function<Integer, Integer> f =
        x -> x * multiplier;
```

Because `multiplier` is no longer effectively final.

---

# 24. Why can't local variables be freely modified inside lambdas?

For example, this doesn't work:

```java
int count = 0;

list.forEach(x -> {
    count++;
});
```

The local variable `count` must be final or effectively final.

There are workarounds such as mutable holders, but don't use them just to force imperative state into a lambda.

With streams especially, prefer transformations/reductions rather than external mutable state.

---

# 25. Functional interfaces and inheritance

Consider:

```java
interface A {
    void run();
}

interface B extends A {
}
```

`B` is still a functional interface because it has one abstract method.

Also:

```java
interface C extends A {
    @Override
    void run();
}
```

Still functional.

What matters is the effective number of distinct abstract methods.

---

# 26. What if two interfaces have the same abstract method?

This is interesting:

```java
interface A {
    void run();
}

interface B {
    void run();
}

interface C extends A, B {
}
```

`C` can still be functional because the inherited methods represent the same abstract method signature.

---

# 27. What if they have different methods?

```java
interface A {
    void run();
}

interface B {
    void stop();
}

interface C extends A, B {
}
```

Now:

```text
run()
stop()
```

There are two abstract methods.

Therefore `C` isn't a functional interface.

---

# 28. Primitive-specialized functional interfaces

Java provides primitive versions to avoid unnecessary boxing/unboxing.

Examples:

```text
IntPredicate
IntFunction
IntConsumer
IntSupplier
```

And:

```text
ToIntFunction<T>
ToLongFunction<T>
ToDoubleFunction<T>
```

For example:

```java
IntPredicate isEven =
        x -> x % 2 == 0;
```

This avoids needing:

```java
Predicate<Integer>
```

for primitive `int` values.

---

# 29. Bi- variants

There are functional interfaces accepting two arguments.

For example:

```java
BiFunction<T, U, R>
```

Example:

```java
BiFunction<Integer, Integer, Integer> sum =
        (a, b) -> a + b;
```

Also:

```java
BiPredicate<T, U>
BiConsumer<T, U>
```

---

# 30. UnaryOperator and BinaryOperator

These are specialized forms of `Function`/`BiFunction`.

### UnaryOperator

```text
T → T
```

Example:

```java
UnaryOperator<Integer> square =
        x -> x * x;
```

---

### BinaryOperator

```text
(T, T) → T
```

Example:

```java
BinaryOperator<Integer> sum =
        (a, b) -> a + b;
```

So:

```text
Function<T,R>
→ input and output can differ

UnaryOperator<T>
→ input and output same type
```

And:

```text
BiFunction<T,U,R>
→ two inputs, output can differ

BinaryOperator<T>
→ two inputs and output same type
```

---

# 31. `Runnable` and `Callable`

These are also functional interfaces, although they're not in `java.util.function`.

### Runnable

```java
Runnable task =
        () -> System.out.println("Running");
```

Method:

```java
void run()
```

No return value.

---

### Callable

```java
Callable<Integer> task =
        () -> 10 + 20;
```

Method:

```java
V call() throws Exception
```

Returns a value and can throw checked exceptions.

These become important when we study multithreading.

---

# 32. Lambda expressions are not anonymous classes

This is an important conceptual distinction.

A lambda:

```java
x -> x * 2
```

is not simply syntactic replacement for:

```java
new SomeInterface() {
    ...
}
```

The JVM implementation is different.

Java can use mechanisms such as `invokedynamic` and runtime-generated implementation machinery rather than treating every lambda as a conventional anonymous inner class.

For interview purposes:

> A lambda is an implementation of a functional interface, but it is not itself an anonymous class.

---

# 33. Lambda requires a target type

This won't compile:

```java
var x = y -> y * 2;
```

Why?

Because Java needs a target functional-interface type to determine what the lambda represents.

But this works:

```java
Function<Integer, Integer> x =
        y -> y * 2;
```

Because the target type is known.

---

# 34. Type inference

You don't need to write:

```java
Function<Integer, Integer> f =
        (Integer x) -> x * 2;
```

You can simply write:

```java
Function<Integer, Integer> f =
        x -> x * 2;
```

Java infers the parameter type from the target type.

You can explicitly specify it:

```java
Function<Integer, Integer> f =
        (Integer x) -> x * 2;
```

But mixing inferred and explicit parameter types isn't allowed:

```java
// invalid
(Integer x, y) -> x + y
```

Either infer all or explicitly specify all.

---

# 35. Lambda and checked exceptions

Suppose:

```java
Function<String, String> function =
        s -> someMethodThatThrowsIOException(s);
```

This won't work if the functional interface's abstract method doesn't declare `IOException`.

Why?

Because:

```java
Function<T,R>.apply()
```

doesn't declare checked exceptions.

This is one reason APIs sometimes define custom functional interfaces when checked exceptions need to be part of the contract.

---

# 36. `Predicate` composition

Predicates can be combined.

```java
Predicate<Integer> positive =
        x -> x > 0;

Predicate<Integer> even =
        x -> x % 2 == 0;
```

You can do:

```java
Predicate<Integer> positiveEven =
        positive.and(even);
```

Then:

```java
positiveEven.test(10);
```

returns:

```text
true
```

You also have:

```java
positive.or(even)
```

and:

```java
positive.negate()
```

This becomes very useful with Streams.

---

# 37. Function composition

Functions can also be composed.

Suppose:

```java
Function<Integer, Integer> doubleValue =
        x -> x * 2;

Function<Integer, Integer> addTen =
        x -> x + 10;
```

Then:

```java
Function<Integer, Integer> result =
        doubleValue.andThen(addTen);
```

For:

```java
result.apply(5);
```

execution is:

```text
5
 ↓
double → 10
 ↓
add 10 → 20
```

You also have:

```java
addTen.compose(doubleValue)
```

which means the same conceptual ordering:

```text
doubleValue
   ↓
addTen
```

The distinction is about which function you call `compose`/`andThen` on.

---

# 38. A practical example

Suppose we have:

```java
List<String> names =
        List.of(
            "Alice",
            "Bob",
            "Andrew",
            "Charlie"
        );
```

We want:

> names starting with A, converted to uppercase.

Predicate:

```java
Predicate<String> startsWithA =
        name -> name.startsWith("A");
```

Function:

```java
Function<String, String> uppercase =
        String::toUpperCase;
```

Conceptually:

```text
names
 ↓
Predicate
 ↓
filter
 ↓
Function
 ↓
transform
```

This leads directly into Streams.

---

# 39. Why Functional Interfaces matter for Streams

Streams use functional interfaces heavily.

For example:

```java
names.stream()
     .filter(name -> name.startsWith("A"))
     .map(String::toUpperCase)
     .forEach(System.out::println);
```

Here:

```text
filter
→ Predicate

map
→ Function

forEach
→ Consumer
```

So if you understand:

```text
Predicate
Function
Consumer
Supplier
```

Streams become much easier.

---

# 40. EPAM interview questions from this topic

You should be able to answer these:

### Basic

1. What is a lambda expression?
2. Why were lambdas introduced?
3. What is a functional interface?
4. What does `@FunctionalInterface` do?
5. Can a functional interface have default methods?
6. Can it have static methods?
7. Can it inherit methods from another interface?

### Very common

8. Predicate vs Function vs Consumer vs Supplier?
9. What is a method reference?
10. Types of method references?
11. Lambda vs anonymous class?
12. What does `this` mean inside a lambda?
13. What is effectively final?
14. Why must captured local variables be final/effectively final?
15. What is target typing?
16. Why can't `var` generally be used directly with a lambda?

### Deeper

17. `Function` vs `UnaryOperator`?
18. `BiFunction` vs `BinaryOperator`?
19. `Runnable` vs `Callable`?
20. What are primitive-specialized functional interfaces?
21. How do Predicate `and`, `or`, `negate` work?
22. `compose()` vs `andThen()`?
23. Can a lambda throw checked exceptions?
24. How does a lambda differ from an anonymous inner class internally?

---

# The mental model

Remember this:

```text
                    Lambda
                       |
                       v
              Functional Interface
                       |
       +---------------+---------------+
       |               |               |
   Predicate        Function       Consumer
   T → boolean       T → R          T → void
                       |
                    Supplier
                     () → T
```

And:

```text
Predicate
→ "Should I keep it?"

Function
→ "How do I transform it?"

Consumer
→ "What action should I perform?"

Supplier
→ "How do I obtain a value?"
```

The **next part of Topic 7** is **Streams API**, where these concepts come together: `filter`, `map`, `flatMap`, `sorted`, `distinct`, `limit`, `skip`, `reduce`, `collect`, `groupingBy`, `partitioningBy`, `findFirst`, `findAny`, `anyMatch`, `allMatch`, `noneMatch`, lazy evaluation, intermediate vs terminal operations, and parallel streams.

---

Once upon a time in the kingdom of Java, everything had to be a Noun (an Object). There were no verbs allowed to roam free. If you wanted to get anything done, you had to build a machine (a Class), put a gear inside it (a Method), create a physical copy of that machine (an Object), and pass that machine around.
This is the story of how Java developers grew tired of building giant machines just to perform small tasks, and how the language evolved from clunky Anonymous Classes to Functional Interfaces and Lambda Expressions.
------------------------------
## Chapter 1: The Dark Ages of Verbosity (Java 1.1 to 7)
Imagine it is the year 2005. You are building a medieval stock market application. You have a list of Knight objects, and you want to filter them based on a single condition: Find all knights who own a dragon.
In pure Object-Oriented Java, you couldn't just pass a simple condition like knight.hasDragon(). You had to create an explicit contract for filtering.

// The contract machine blueprintinterface KnightFilter {
    boolean test(Knight knight);
}

If you only needed to use this filter once in your entire codebase, creating a separate named class file like DragonKnightFilter.java felt like an absolute waste of time.
## The Savior: Anonymous Classes
To solve this, Java introduced Anonymous Classes. It allowed developers to declare and instantiate a class simultaneously, right where they needed it, without giving it a name.

List<Knight> dragonKnights = filterKnights(allKnights, new KnightFilter() {
    @Override
    public boolean test(Knight knight) {
        return knight.hasDragon();
    }
});

## The Problem with Chapter 1
While Anonymous Classes saved us from creating hundreds of separate .java files, they introduced new headaches:

   1. The "Vertical Problem" (Boilerplate): Look at the code above. To write exactly one line of actual logic (return knight.hasDragon();), you had to write 5 lines of syntactic wrapper code. It was hard to read and cluttered the screen.
   2. Memory Overhead: Every time the JVM compiled an anonymous class, it generated a physical, separate .class file on your hard drive (e.g., KingdomApp$1.class). If you used thousands of these across a massive enterprise application, it increased the deployment size and bloated the JVM's permanent memory (PermGen/Metaspace) because thousands of separate class blueprints had to be loaded.
   3. The this Trap: Inside that anonymous class, the keyword this referred to the KnightFilter instance itself, not the outer enclosing class. If you wanted to access something from your main application class using this, you had to type KingdomApp.this, leading to confusion.

------------------------------
## Chapter 2: The Architectural Shift (Java 8 Design Phase)
By 2014, modern programming languages like JavaScript, Python, and Scala were thriving because they supported Functional Programming. They treated functions as "first-class citizens"—meaning you could pass a function into another function just like an integer or a string.
Java architects wanted this superpower, but they faced a strict constraint: Backward Compatibility. They couldn't just rewrite how Java worked without breaking billions of lines of existing corporate code.
They realized a pattern: Most anonymous classes were being used to implement interfaces that had only one abstract method (like Runnable, Comparator, or our KnightFilter).
## The Breakthrough: Functional Interfaces
Java architects decided to formally weaponize this pattern. They defined a Functional Interface as any interface that contains exactly one abstract method.
To make it official, they introduced the @FunctionalInterface annotation. It didn't change how the code worked, but it forced the compiler to yell at you if someone accidentally tried to add a second abstract method to that interface later.

@FunctionalInterfaceinterface KnightFilter {
    boolean test(Knight knight); // Exactly one abstract method. This is the "target type".
}

Now, Java had a formal way to recognize a "function type" while remaining completely Object-Oriented. The single abstract method became the placeholder for a verb.
------------------------------
## Chapter 3: The Golden Age of Elegance (Java 8 Lambdas)
With Functional Interfaces laying down the law, the compiler became incredibly smart.
The architects realized that if an interface only has one method, the developer shouldn't have to type out the method name, the argument types, or the new InterfaceName() boilerplate. The compiler can infer all of it!
This gave birth to the Lambda Expression.
Instead of writing a massive anonymous class machine, you could strip away the metal wrapper and just provide the raw engine (the parameters and the action):

// Look Mom, no boilerplate!
List<Knight> dragonKnights = filterKnights(allKnights, (knight) -> knight.hasDragon());

## How the Compiler Infers This (The Magic)
When the Java compiler sees (knight) -> knight.hasDragon(), it performs a detective trick called Type Inference:

   1. It looks at the filterKnights method signature and sees it expects a KnightFilter.
   2. It looks at KnightFilter and sees it is a @FunctionalInterface.
   3. It notes that the single abstract method test(Knight knight) expects a Knight object and returns a boolean.
   4. It maps your lambda parameters: (knight) must be a Knight object, and the expression knight.hasDragon() perfectly satisfies the boolean return type.

------------------------------
## Chapter 4: Under the Hood Optimization (Deep Dive)
You might think that a Lambda is just a prettier, shorter way of writing an Anonymous Class, and that the compiler secretly converts it back into an Anonymous Class behind your back. It does not.
If lambdas were just syntax sugar for anonymous classes, they would still suffer from the same performance pitfalls (bloated .class files on disk and heavy memory usage).
Instead, Java 8 introduced a highly optimized JVM bytecode instruction called invokedynamic (Indy).
## The Performance Difference

| Process Step | Anonymous Class Approach | Lambda Expression Approach |
|---|---|---|
| Compilation | Compiles into a separate file on disk: KingdomApp$1.class. | No separate file. The lambda code is packaged into a hidden, private method inside your existing class. |
| Class Loading | The JVM must find, read, and load KingdomApp$1.class from the disk on startup, consuming memory. | The JVM recipe links it dynamically at runtime using invokedynamic. |
| Object Creation | A brand new physical object instance is allocated on the heap every single time the line executes. | Highly optimized. If the lambda doesn't capture variables from the outside (stateless), the JVM creates one instance and reuses it forever. |
| Scope (this) | Creates a new scope. this refers to the anonymous class. | Lexical scoping. this refers entirely to the enclosing outer class. |

------------------------------
## Epilogue: The Modern Era
Today, you rarely see anonymous classes used for single-method behaviors. Lambdas have completely changed how Java developers write code, opening the doors to the powerful Streams API (knights.stream().filter(k -> k.hasDragon()).collect(...)).
However, Anonymous Classes didn't die. They are still vital when:

* You need to implement an interface with multiple abstract methods.
* You need to extend an abstract or concrete class on the fly (Lambdas can only implement Functional Interfaces).
* You need to store internal state (instance fields) that changes over time within that specific object.

Would you like to explore Variable Capture next, to see why variables used inside lambdas must be final or effectively final, or would you like to practice converting an anonymous class to a lambda?


