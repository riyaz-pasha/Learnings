# Chapter 3 — C# Variables and Data Types

This is one of the most important chapters in C#.

We'll spend some time here because understanding C#'s type system, especially value types vs reference types, will make many advanced concepts easier later.

Since you know Java and JavaScript, we'll compare all three.

By the end of this chapter, you should understand:

1. How variables and types work in C#.
2. Primitive types and their differences from Java.
3. `var` and type inference.
4. Value types vs reference types.
5. How assignment and method arguments work.
6. Stack vs heap and common misconceptions.
7. Structs vs classes.
8. Boxing and unboxing.
9. Nullable types and null safety.

# 3.1 What is a variable in C#?

Let's start with something familiar.

In Java:

```
int age = 25;
String name = "John";
double salary = 5000.50;
```

In JavaScript:

```
let age = 25;
let name = "John";
let salary = 5000.50;
```

In C#:

```
int age = 25;
string name = "John";
double salary = 5000.50;
```

Looks very similar to Java.

The basic syntax is:

```
DataType variableName = value;
```

For example:

```
int count = 10;

string username = "john";

bool isActive = true;
```

Unlike JavaScript, C# is statically typed.

That means the type of a variable is determined at compile time.

Consider:

```
int age = 25;

age = 30;       // Valid

age = "John";   // Compilation error
```

Java behaves similarly.

JavaScript behaves differently:

```
let age = 25;

age = "John";   // Valid
```

JavaScript variables can hold values of different types over their lifetime. C# variables cannot arbitrarily change their declared types.

Important: C# is strongly and statically typed, but it also provides features such as `dynamic` that allow certain operations to be resolved at runtime. We'll explore that later.

# 3.2 Understanding C# data types

Let's look at the commonly used built-in types.

C# provides several fundamental types.

| C# type   | Size                                     | Example              | Java equivalent                |
| --------- | ---------------------------------------- | -------------------- | ------------------------------ |
| `byte`    | 1 byte                                   | `byte x = 10;`       | `byte`                         |
| `short`   | 2 bytes                                  | `short x = 100;`     | `short`                        |
| `int`     | 4 bytes                                  | `int x = 1000;`      | `int`                          |
| `long`    | 8 bytes                                  | `long x = 10000L;`   | `long`                         |
| `float`   | 4 bytes                                  | `float x = 10.5f;`   | `float`                        |
| `double`  | 8 bytes                                  | `double x = 10.5;`   | `double`                       |
| `decimal` | 16 bytes                                 | `decimal x = 10.5m;` | `BigDecimal` (similar purpose) |
| `bool`    | 1 byte in typical runtime representation | `bool x = true;`     | `boolean`                      |
| `char`    | 2 bytes                                  | `char x = 'A';`      | `char`                         |

The sizes above are the documented storage sizes of the respective value types, not necessarily the total memory consumed when they're used inside objects or collections.

Let's understand some important differences.

## A. Integer types

C# supports multiple integer types.

```
byte smallNumber = 100;

short temperature = 250;

int population = 100000;

long distance = 9876543210L;
```

Their approximate ranges:

| Type    | Minimum           | Maximum          |
| ------- | ----------------- | ---------------- |
| `byte`  | 0                 | 255              |
| `short` | -32,768           | 32,767           |
| `int`   | -2.1 billion      | 2.1 billion      |
| `long`  | -9.22 quintillion | 9.22 quintillion |

Unlike Java, C# also supports unsigned integer types:

```
byte number = 255;

uint positiveNumber = 1000;

ulong largePositiveNumber = 1000000;
```

An unsigned integer cannot represent negative numbers.

Java doesn't provide unsigned primitive integer types in the same general-purpose way.

In backend development, you'll mostly use:

```
int
long
```

For example:

```
public class User
{
    public long Id { get; set; }

    public int Age { get; set; }
}
```

## B. Floating-point types

C# provides:

```
float
double
```

Examples:

```
float temperature = 36.5f;

double distance = 12345.6789;
```

Notice the `f` suffix for float.

Without it:

```
float temperature = 36.5; // Compilation error
```

Why?

Because a decimal literal such as `36.5` is treated as a `double` by default.

C# doesn't automatically perform this potentially narrowing conversion.

You can also use:

```
float temperature = 36.5F;
```

Both `f` and `F` are valid.

## C. Decimal — particularly important for backend development

This is something you should pay attention to.

C# provides a type called `decimal`.

```
decimal price = 99.99m;
```

Notice the `m` suffix.

Why does C# need another floating-point type?

Let's understand using a financial example.

Suppose you process payments:

```
double amount = 0.1 + 0.2;

Console.WriteLine(amount);
```

Output:

```
0.30000000000000004
```

This happens because floating-point types represent many decimal fractions approximately using binary floating-point representation.

Now:

```
decimal amount = 0.1m + 0.2m;

Console.WriteLine(amount);
```

Output:

```
0.3
```

`decimal` provides decimal-based arithmetic with substantially higher precision for many financial calculations.

This makes it suitable for things like:

- Payment amounts.
- Product prices.
- Tax calculations.
- Financial balances.
- Invoice totals.

### Java comparison

Java commonly uses:

```
BigDecimal amount = new BigDecimal("0.1")
        .add(new BigDecimal("0.2"));
```

C# can use:

```
decimal amount = 0.1m + 0.2m;
```

Conceptually, `decimal` serves a similar purpose to Java's `BigDecimal`, although their precision, range, and rounding behavior differ.

One important detail: `decimal` is not unlimited precision. It has a finite precision and range. You still need to define appropriate rounding rules for financial calculations.

Practical rule: Use `decimal` for most financial values. Don't use `float` or `double` for exact monetary arithmetic.

# 3.3 The `string` type

In C#:

```
string name = "John";
```

In Java:

```
String name = "John";
```

Notice:

- C#: `string`
- Java: `String`

C# has two ways to refer to strings:

```
string name = "John";

System.String anotherName = "Jane";
```

Both are equivalent.

`string` is a C# keyword alias for `System.String`.

### Is string a value type or reference type?

This is important.

String is a reference type in C#.

Just like Java.

```
string name = "John";
```

The variable holds a reference to a string object.

### String immutability

Like Java, C# strings are immutable.

Consider:

```
string name = "John";

name = name + " Smith";
```

Does this modify the original string?

No.

Conceptually:

1. `"John"` is an existing string object.
2. Concatenation creates a new string object.
3. `name` now refers to the resulting string.

The original string remains unchanged.

This is important when performing many string operations.

For repeated string modifications, C# provides:

```
var builder = new StringBuilder();

builder.Append("Hello");
builder.Append(" ");
builder.Append("World");

string result = builder.ToString();
```

Similar to Java's `StringBuilder`.

We'll study strings in more detail in Chapter 6.

# 3.4 The `char` and `bool` types

### Character

```
char grade = 'A';
```

Use single quotes for a character.

```
char grade = 'A';       // Valid

string grade = "A";     // Also valid, but different type
```

C# `char`, like Java `char`, is a 16-bit UTF-16 code unit.

A `char` doesn't necessarily represent an entire Unicode character. Some Unicode characters require a pair of UTF-16 code units.

You normally won't need to worry about this when doing ordinary backend development, but it matters when working with Unicode text processing.

### Boolean

```
bool isActive = true;

bool isDeleted = false;
```

Java uses:

```
boolean isActive = true;
```

JavaScript uses:

```
let isActive = true;
```

One important difference:

C# doesn't treat numbers as booleans.

For example:

```
int value = 1;

if (value)
{
    Console.WriteLine("True");
}
```

This is invalid.

You must provide an actual boolean expression:

```
if (value > 0)
{
    Console.WriteLine("Positive");
}
```

This is similar to Java but different from JavaScript, which allows truthy and falsy values.

# 3.5 What is `var` in C#?

You might have noticed that our examples sometimes use:

```
var name = "John";
```

Instead of:

```
string name = "John";
```

What exactly is `var`?

Think about Java's local variable type inference.

Java:

```
var name = "John";
```

C#:

```
var name = "John";
```

Both use compile-time type inference.

The compiler determines the type based on the assigned expression.

In C#:

```
var age = 25;

var salary = 5000.50m;

var username = "John";

var isActive = true;
```

The compiler infers:

```
int age = 25;

decimal salary = 5000.50m;

string username = "John";

bool isActive = true;
```

These aren't dynamically typed variables.

The types are determined at compile time.

Consider:

```
var age = 25;

age = 30;       // Valid

age = "John";   // Compilation error
```

Once inferred as `int`, the variable remains an `int`.

### When can we use var?

We can use it when the compiler can infer the type from the initialization expression.

Valid:

```
var name = "John";

var users = new List<string>();
```

Invalid:

```
var name;
```

Why?

There is no initialization expression from which the compiler can infer the type.

### When should we use var?

Consider:

```
Dictionary<string, List<User>> users =
    new Dictionary<string, List<User>>();
```

This is quite verbose.

Instead:

```
var users = new Dictionary<string, List<User>>();
```

Much cleaner.

However, don't use `var` when it significantly reduces readability.

For example:

```
var result = CalculateSomething();
```

If the return type isn't obvious, explicitly declaring the type might make the code easier to understand.

Important mental model:

`var` doesn't mean the type can change.

It simply means the compiler determines the type for you.

# 3.6 The most important concept: Value types vs Reference types

Now we're getting to something fundamental in C#.

This distinction affects:

- Assignment behavior.
- Method arguments.
- Memory management.
- Object copying.
- Collections.
- Performance.
- Concurrency.

Let's understand it properly.

C# types are broadly divided into two categories:

1. Value types.
2. Reference types.

## A. Value types

When you assign a value type variable to another variable, the value is copied.

Consider:

```
int a = 10;

int b = a;

b = 20;

Console.WriteLine(a);
Console.WriteLine(b);
```

Output:

```
10
20
```

Why?

Because:

```
int b = a;
```

copies the value from `a` into `b`.

Now there are two independent variables.

Changing `b` doesn't change `a`.

Let's visualize it.

```
a = 10
b = 10
```

After:

```
b = 20;
```

We have:

```
a = 10
b = 20
```

This is called value semantics.

Some important C# value types:

- `int`
- `long`
- `double`
- `float`
- `decimal`
- `bool`
- `char`
- `struct`
- `enum`

You can think of them as types whose values are copied during ordinary assignment.

## B. Reference types

Now consider:

```
class User
{
    public string Name { get; set; }
}
```

Create two variables:

```
User user1 = new User();

user1.Name = "John";

User user2 = user1;

user2.Name = "Smith";

Console.WriteLine(user1.Name);
Console.WriteLine(user2.Name);
```

Output:

```
Smith
Smith
```

Why?

Because `User` is a class, and classes are reference types.

Let's understand what happens.

```
User user1 = new User();
```

An object is created.

The variable `user1` holds a reference to that object.

Now:

```
User user2 = user1;
```

The reference is copied, not the object itself.

Both variables refer to the same object.

Visualize it:

```
user1 -----------+
                 |
                 v
             +---------+
             |  User   |
             |---------|
             | Name    |
             | John    |
             +---------+
                 ^
                 |
user2 -----------+
```

After:

```
user2.Name = "Smith";
```

The same shared object is modified.

So both variables observe the change.

This is called reference semantics.

### Important examples of reference types

- Classes.
- Strings.
- Arrays.
- Interfaces.
- Delegates.
- Objects created using `new` for class types.

Example:

```
string name = "John";

int[] numbers = { 1, 2, 3 };

User user = new User();
```

All three variables refer to reference-type objects.

# 3.7 Java comparison: Value types vs Reference types

Let's connect this to Java.

Java has primitive types:

```
int a = 10;
int b = a;
```

Java primitives behave similarly to C# value types.

But Java objects are reference types:

```
User user1 = new User();

User user2 = user1;
```

This behaves similarly to C# classes.

The biggest difference is that C# supports user-defined value types through `struct`.

For example, Java:

```
class Point {
    int x;
    int y;
}
```

A `Point` is a reference type.

C# gives you another option:

```
struct Point
{
    public int X;
    public int Y;
}
```

Now `Point` is a value type.

This is a major difference between Java and C#.

We'll explore when to choose a struct versus a class shortly.

# 3.8 Very important: Stack vs Heap

You've probably heard this explanation:

- Value types are stored on the stack.
- Reference types are stored on the heap.

This is a common simplification.

But it is not completely correct.

Let's understand properly.

### First, what is the stack?

Each executing thread generally has its own call stack.

When methods are called, stack frames are created to track method execution, local variables, parameters, and related execution state.

Conceptually:

```
void Main()
{
    int age = 25;

    Calculate();
}

void Calculate()
{
    int result = 10 + 20;
}
```

When `Main()` executes:

```
Call Stack
+----------------------+
| Main()               |
| age = 25             |
+----------------------+
```

When `Calculate()` is called:

```
Call Stack
+----------------------+
| Calculate()          |
| result = 30          |
+----------------------+
| Main()               |
| age = 25             |
+----------------------+
```

When `Calculate()` finishes, its stack frame is removed.

This is a conceptual model of method execution.

### What is the heap?

The managed heap is memory used for dynamically allocated managed objects.

For example:

```
User user = new User();
```

Conceptually:

```
Stack / Method Execution          Managed Heap

user --------------------------> +-------------+
                                 | User Object |
                                 |-------------|
                                 | Name        |
                                 +-------------+
```

The variable contains a reference to the object.

When the object is no longer reachable, the GC can eventually reclaim it.

### Now, here's the important correction

Consider:

```
class Customer
{
    public int Age;
}
```

And:

```
Customer customer = new Customer
{
    Age = 25
};
```

Where is `Age` stored?

The `Age` field is part of the `Customer` object.

The `Customer` object lives on the managed heap, so its `Age` field is stored as part of that object.

Even though `int` is a value type, it isn't necessarily stored on the stack.

Another example:

```
int[] numbers = new int[3];
```

The array is a reference type.

The array object is allocated on the managed heap.

Its integer elements are stored inside the array's allocated storage.

So the integer values are also in the heap.

This is important!

The storage location is not determined solely by whether a type is a value type or reference type.

A better mental model:

| Situation                     | Storage behavior                                               |
| ----------------------------- | -------------------------------------------------------------- |
| Local value-type variable     | Often stored in a stack frame or register; JIT may optimize it |
| Local reference-type variable | Reference may be stored in a stack frame or register           |
| Class object                  | Normally allocated on managed heap                             |
| Value-type field inside class | Stored inline as part of the class object                      |
| Value-type array elements     | Stored inline in the array object                              |
| Boxed value type              | Boxed object allocated on managed heap                         |

The JIT can optimize actual storage locations, so the conceptual model shouldn't be treated as a guaranteed physical memory map.

### What should you remember?

- Value type vs reference type describes type and copying semantics.
- Stack vs heap describes memory storage.
- These are two different concepts.
- Value types can exist inside heap-allocated objects.
- Reference variables do not contain the actual object.

This distinction is extremely important when debugging performance and memory problems.

# 3.9 Understanding structs vs classes

Now let's explore one of C#'s most useful features.

You can define your own value types using `struct`.

Consider this class:

```
class Point
{
    public int X { get; set; }

    public int Y { get; set; }
}
```

We can instantiate it:

```
Point point1 = new Point
{
    X = 10,
    Y = 20
};

Point point2 = point1;

point2.X = 100;
```

Now `point1.X` is also `100`, because both variables refer to the same object.

Let's implement the same idea using a struct.

```
struct Point
{
    public int X { get; set; }

    public int Y { get; set; }
}
```

Now:

```
Point point1 = new Point
{
    X = 10,
    Y = 20
};

Point point2 = point1;

point2.X = 100;

Console.WriteLine(point1.X);
Console.WriteLine(point2.X);
```

Output:

```
10
100
```

Why?

Because `Point` is a struct.

When we assign:

```
Point point2 = point1;
```

The value is copied.

These are independent values.

### Struct vs Class comparison

| Feature                                 | Class            | Struct       |
| --------------------------------------- | ---------------- | ------------ |
| Type category                           | Reference type   | Value type   |
| Assignment                              | Copies reference | Copies value |
| Can be null by default                  | Yes              | No           |
| Supports inheritance from another class | Yes              | No           |
| Can implement interfaces                | Yes              | Yes          |
| Can contain methods                     | Yes              | Yes          |
| Can contain properties                  | Yes              | Yes          |
| Can be mutable                          | Yes              | Yes          |
| Can be immutable                        | Yes              | Yes          |

A struct can have constructors, methods, properties, fields, operators, and interface implementations.

It's not simply a primitive data type.

### When should we use a struct?

Consider a `Money` type:

```
public readonly struct Money
{
    public decimal Amount { get; }

    public string Currency { get; }

    public Money(decimal amount, string currency)
    {
        Amount = amount;
        Currency = currency;
    }
}
```

A `Money` value represents a value rather than an identity-bearing object.

Consider:

```
var first = new Money(100, "INR");

var second = first;
```

We generally expect `second` to represent an independent copy of the same money value.

That is a natural fit for value semantics.

Other possible examples:

- Coordinates.
- Geographic points.
- Small mathematical values.
- Small immutable data structures.

### When should we use a class?

Consider:

```
public class Order
{
    public long Id { get; set; }

    public string Status { get; set; }
}
```

An order has an identity.

Multiple parts of an application may refer to the same order.

We generally want modifications to that particular order to be visible through all references to it.

A class is a natural choice.

### Important warning about structs

Structs are copied by value.

Imagine:

```
public struct LargeData
{
    public long A;
    public long B;
    public long C;
    public long D;
}
```

Copying this value repeatedly can be more expensive than copying a reference.

So don't automatically use structs because you think they are faster.

A practical recommendation:

- Use classes for most domain objects and services.
- Use structs for small value-oriented data structures.
- Prefer immutable structs when possible.
- Avoid unnecessarily large mutable structs.

We'll learn records and immutable data types later.

# 3.10 Understanding boxing and unboxing

This is an important C# runtime concept.

Let's say we have:

```
int number = 10;
```

`int` is a value type.

Now consider:

```
object value = number;
```

How can a value type be assigned to `object`?

Because `object` is the root base type of the .NET type system.

Every C# type is compatible with `object`, including value types.

But something interesting happens here.

The integer value must be represented as an object.

This process is called boxing.

### What exactly happens during boxing?

```
int number = 10;

object value = number;
```

Conceptually:

1. The integer value `10` is taken.
2. A boxed representation is created.
3. The value is stored inside that boxed object.
4. `value` holds a reference to the boxed object.

Visualize it:

```
number = 10

       Boxing
          |
          v

value ----------> +----------------+
                  | Boxed Int32    |
                  |----------------|
                  | Value = 10     |
                  +----------------+
```

Now let's see unboxing.

```
object value = 10;

int number = (int)value;
```

Converting the boxed object back into its original value type is called unboxing.

Important: unboxing requires the correct boxed value type.

For example:

```
object value = 10;

long number = (long)value;
```

This throws `InvalidCastException`.

Why?

Because `value` contains a boxed `int`, not a boxed `long`.

You need:

```
long number = (long)(int)value;
```

The first cast unboxes the `int`; the second converts it to `long`.

### Why does boxing exist?

Because sometimes APIs need to work with different types through a common abstraction.

Historically, non-generic collections made extensive use of `object`.

For example:

```
ArrayList values = new ArrayList();

values.Add(10);
values.Add("John");
values.Add(true);
```

The integer gets boxed when added.

Later, retrieving the integer requires unboxing:

```
int number = (int)values[0];
```

This is one reason C# provides generics.

Instead of:

```
ArrayList values = new ArrayList();
```

We can use:

```
List<int> values = new List<int>();

values.Add(10);
values.Add(20);
```

This is type-safe and avoids boxing for ordinary `int` storage in `List<int>`.

Practical takeaway: Understand boxing and unboxing, but prefer generic collections rather than old non-generic collections for normal application code.

# 3.11 Nullable value types

Let's understand another important C# feature.

Consider:

```
int age = 25;
```

Can `age` be null?

No.

Because `int` is a value type.

But in real backend systems, data can be optional.

Imagine a database record:

```
User
----------------
Id          = 1
Name        = John
Age         = NULL
```

The user's age may not be available.

How do we represent this in C#?

We use nullable value types.

```
int? age = null;
```

Now the variable can hold either:

- An integer value.
- Null.

This is equivalent to:

```
Nullable<int> age = null;
```

`int?` is shorthand for `Nullable<int>`.

### Example

```
int? age = null;

Console.WriteLine(age.HasValue);
```

Output:

```
False
```

Assign a value:

```
age = 25;

Console.WriteLine(age.HasValue);
Console.WriteLine(age.Value);
```

Output:

```
True
25
```

You can also use:

```
int actualAge = age ?? 0;
```

The `??` operator is called the null-coalescing operator.

It means:

> If the left side is not null, use it. Otherwise, use the right side.

Example:

```
int? age = null;

int actualAge = age ?? 18;

Console.WriteLine(actualAge);
```

Output:

```
18
```

### Another useful operator: `?.`

Suppose:

```
User user = null;
```

You want to read the name safely.

Without null-conditional access:

```
string name = user.Name;
```

This would throw a `NullReferenceException`.

Instead:

```
string? name = user?.Name;
```

If `user` is null, the result is null.

We'll explore the null-related operators more thoroughly later.

# 3.12 Nullable reference types

This is slightly different from nullable value types.

Let's revisit:

```
string name = "John";
```

Strings are reference types.

Historically, reference-type variables could contain null without a compiler warning.

```
string name = null;
```

This could eventually lead to:

```
Console.WriteLine(name.Length);
```

A `NullReferenceException`.

C# introduced nullable reference type analysis to help prevent these issues.

When enabled:

```
<Nullable>enable</Nullable>
```

We can write:

```
string name = "John";

string? optionalName = null;
```

The difference:

| Declaration | Meaning                       |
| ----------- | ----------------------------- |
| `string`    | Intended to be non-null       |
| `string?`   | Null is allowed               |
| `int`       | Must contain an integer value |
| `int?`      | Integer or null               |

Now imagine:

```
string? name = null;

Console.WriteLine(name.Length);
```

The compiler warns that `name` could be null.

You can handle it safely:

```
if (name != null)
{
    Console.WriteLine(name.Length);
}
```

Or:

```
Console.WriteLine(name?.Length);
```

### Important distinction

Nullable value types and nullable reference types work differently internally.

For value types:

```
int? age = null;
```

The type is actually `Nullable<int>`.

It is a distinct value-type wrapper that represents either a value or absence of one.

For reference types:

```
string? name = null;
```

The runtime type is still `System.String`.

The `?` primarily affects compiler analysis and does not introduce a separate runtime string type.

This is a subtle but important distinction.

# 3.13 A practical backend example

Let's bring these concepts together.

Imagine you're building a user management application.

You have the following model:

```
public class User
{
    public long Id { get; set; }

    public string Name { get; set; } = string.Empty;

    public string? Email { get; set; }

    public int Age { get; set; }

    public decimal WalletBalance { get; set; }

    public bool IsActive { get; set; }
}
```

Let's understand our choices.

| Property        | Type      | Why?                |
| --------------- | --------- | ------------------- |
| `Id`            | `long`    | Large identifiers   |
| `Name`          | `string`  | Text                |
| `Email`         | `string?` | Email may be absent |
| `Age`           | `int`     | Whole number        |
| `WalletBalance` | `decimal` | Financial value     |
| `IsActive`      | `bool`    | Boolean state       |

Now imagine the user age is optional.

We can change:

```
public int Age { get; set; }
```

to:

```
public int? Age { get; set; }
```

This is a useful distinction when mapping database records.

Similarly, a property declared as `string` communicates that null is not intended, while `string?` communicates that it is allowed.

These annotations help the compiler and developers understand the data contract.

# 3.14 Quick comparison: Java vs C# vs JavaScript

Let's consolidate the differences.

| Concept                  | Java                              | C#                                | JavaScript                          |
| ------------------------ | --------------------------------- | --------------------------------- | ----------------------------------- |
| Static typing            | Yes                               | Yes                               | No (dynamically typed)              |
| Primitive integer        | `int`                             | `int`                             | `number`                            |
| Large integer            | `long`                            | `long`                            | `bigint`                            |
| Decimal financial type   | `BigDecimal`                      | `decimal`                         | No built-in exact decimal primitive |
| Boolean                  | `boolean`                         | `bool`                            | `boolean`                           |
| String                   | `String`                          | `string`                          | `string`                            |
| User-defined value types | No general struct equivalent      | `struct`                          | No                                  |
| Reference types          | Classes, arrays, interfaces, etc. | Classes, arrays, interfaces, etc. | Objects and arrays                  |
| Nullability analysis     | Null annotations/tools available  | Nullable reference types          | Runtime null/undefined              |
| Type inference           | `var`                             | `var`                             | `let`, `const`                      |

# 3.15 Common mistakes to avoid

These are worth remembering.

Mistake 1: Assuming `var` is dynamically typed.

```
var age = 20;

age = "John"; // Compilation error
```

`var` is compile-time type inference.

Mistake 2: Assuming all value types live on the stack.

```
class User
{
    public int Age { get; set; }
}
```

The `Age` field is part of the heap-allocated User object.

Mistake 3: Assuming copying a class variable copies the object.

```
User user2 = user1;
```

It copies the reference.

Mistake 4: Using double for monetary values.

```
double price = 99.99;
```

Prefer:

```
decimal price = 99.99m;
```

Mistake 5: Assuming structs are always faster.

Large structs can be expensive to copy. Use them for suitable value-oriented types, not as a universal performance optimization.

Mistake 6: Confusing nullable value types with nullable reference types.

```
int? age = null;

string? name = null;
```

Both allow null, but their underlying type-system behavior differs.

# 4. Practice exercises

Try these before moving on.

### Exercise 1 — Value vs Reference

What is the output?

```
int a = 10;
int b = a;

b = 20;

Console.WriteLine(a);
Console.WriteLine(b);
```

Now:

```
class Account
{
    public decimal Balance { get; set; }
}

Account a = new Account { Balance = 1000 };

Account b = a;

b.Balance = 500;

Console.WriteLine(a.Balance);
Console.WriteLine(b.Balance);
```

Explain why the behavior differs.

### Exercise 2 — Struct vs Class

Create:

```
struct PointStruct
{
    public int X { get; set; }
}

class PointClass
{
    public int X { get; set; }
}
```

Create two variables of each type, assign one to another, modify the second, and observe the output.

### Exercise 3 — Nullable types

Create a user model:

```
public class User
{
    public string Name { get; set; } = string.Empty;

    public int? Age { get; set; }

    public string? Email { get; set; }
}
```

Create a user without age and email.

Write code to print:

```
Name: John
Age: Not Provided
Email: Not Provided
```

### Exercise 4 — Boxing

What happens here?

```
int number = 100;

object value = number;

number = 200;

Console.WriteLine(value);
```

Why doesn't the output change to 200?

# 5. Knowledge check

Make sure you can explain these without memorizing definitions:

1. What's the difference between static typing and dynamic typing?
2. What's the difference between `var` and explicit type declaration?
3. Why is `decimal` useful in financial applications?
4. What makes a type a value type or reference type?
5. What exactly happens when you assign one class variable to another?
6. Why is it incorrect to say that all value types live on the stack?
7. When would you choose a struct over a class?
8. What is boxing and why does it happen?
9. What's the difference between `int?` and `string?`?
10. What problem do nullable reference types solve?

## Next chapter — Chapter 4: Operators and Control Flow

We'll cover:

- Arithmetic, relational and logical operators.
- Short-circuit evaluation (`&&`, `||`).
- Bitwise operators.
- Ternary operator.
- `if`, `else`, `switch`.
- Modern switch expressions.
- `for`, `while`, `foreach`.
- Pattern matching.
- `break`, `continue`, and `return`.
- Differences between C# and Java/JavaScript control-flow behavior.

We'll also understand some modern C# features that make conditional logic much cleaner.

Say next when ready.
