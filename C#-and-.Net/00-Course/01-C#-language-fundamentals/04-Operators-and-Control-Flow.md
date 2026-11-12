# Chapter 4 — C# Operators and Control Flow

In this chapter, we'll learn how to write conditional logic, perform calculations, and control program execution in C#.

Most of these concepts will feel familiar because they exist in Java and JavaScript. However, C# has some interesting features, particularly pattern matching and switch expressions, that make certain types of business logic much cleaner.

We'll cover:

1. Arithmetic and assignment operators.
2. Comparison and logical operators.
3. Short-circuit evaluation.
4. Ternary operators.
5. Bitwise operators.
6. Null-related operators.
7. Conditional statements (`if`, `else`, `switch`).
8. Switch expressions.
9. Pattern matching.
10. Loops (`for`, `while`, `do-while`, `foreach`).
11. `break`, `continue`, and `return`.
12. Integer overflow and checked arithmetic.

# 4.1 Arithmetic operators

These are probably familiar.

```
int a = 20;
int b = 10;

Console.WriteLine(a + b); // 30
Console.WriteLine(a - b); // 10
Console.WriteLine(a * b); // 200
Console.WriteLine(a / b); // 2
Console.WriteLine(a % b); // 0
```

| Operator | Meaning             | Example |
| -------- | ------------------- | ------- |
| `+`      | Addition            | `a + b` |
| `-`      | Subtraction         | `a - b` |
| `*`      | Multiplication      | `a * b` |
| `/`      | Division            | `a / b` |
| `%`      | Modulus (remainder) | `a % b` |

### Important: Integer division

Consider:

```
int result = 10 / 3;

Console.WriteLine(result);
```

Output:

```
3
```

Why?

Because both operands are integers, so the division produces an integer result, with the fractional part discarded (truncated towards zero).

This is similar to Java.

In JavaScript:

```
console.log(10 / 3);
```

Output:

```
3.3333333333333335
```

JavaScript's `number` type is floating-point, so division behaves differently.

In C#:

```
double result = 10.0 / 3;

Console.WriteLine(result);
```

Now you'll get an approximate floating-point result.

Practical lesson: Be conscious of operand types when performing calculations. Assigning an integer division result to a `double` doesn't restore the lost fractional part.

For example:

```
double result = 10 / 3; // 3.0
```

The division happens first using integer arithmetic.

Instead:

```
double result = 10.0 / 3;
```

# 4.2 Assignment operators

These are also familiar.

```
int count = 10;

count += 5;  // count = count + 5

count -= 2;  // count = count - 2

count *= 2;  // count = count * 2

count /= 2;  // count = count / 2

count %= 3;  // count = count % 3
```

Increment and decrement:

```
int count = 10;

count++;

count--;

++count;

--count;
```

### Prefix vs postfix

This behaves like Java and JavaScript.

Consider:

```
int a = 10;

int b = a++;

Console.WriteLine(a);
Console.WriteLine(b);
```

Output:

```
11
10
```

Postfix (`a++`):

1. The current value is used.
2. Then the variable is incremented.

Now:

```
int a = 10;

int b = ++a;

Console.WriteLine(a);
Console.WriteLine(b);
```

Output:

```
11
11
```

Prefix (`++a`):

1. The variable is incremented.
2. Then the updated value is used.

In normal application code, separate increment operations from more complicated expressions when readability matters.

# 4.3 Comparison operators

Comparison operators return a `bool`.

```
int age = 25;

Console.WriteLine(age == 25);  // True

Console.WriteLine(age != 25);  // False

Console.WriteLine(age > 18);   // True

Console.WriteLine(age < 18);   // False

Console.WriteLine(age >= 25);  // True

Console.WriteLine(age <= 25);  // True
```

| Operator | Meaning               |
| -------- | --------------------- |
| `==`     | Equal                 |
| `!=`     | Not equal             |
| `>`      | Greater than          |
| `<`      | Less than             |
| `>=`     | Greater than or equal |
| `<=`     | Less than or equal    |

### Comparing strings

In C#:

```
string name1 = "John";
string name2 = "John";

Console.WriteLine(name1 == name2);
```

Output:

```
True
```

This is an important difference from how you might initially think about Java.

In Java:

```
String name1 = "John";
String name2 = "John";

System.out.println(name1 == name2);
```

`==` generally checks whether both references refer to the same object, not whether the strings have the same content.

You typically use:

```
name1.equals(name2);
```

In C#, the `==` operator on strings performs value-based string comparison (ordinal comparison), so it checks whether the string contents are equal.

You can also use:

```
string.Equals(name1, name2);
```

For culture-sensitive or case-insensitive string comparisons, explicitly choose an appropriate `StringComparison` value.

For example:

```
string.Equals(
    name1,
    name2,
    StringComparison.OrdinalIgnoreCase
);
```

This is useful when comparing identifiers, usernames, or other case-insensitive values.

Practical recommendation: For programmatic identifiers and keys, prefer ordinal comparison rather than culture-dependent comparison unless the business requirement specifically calls for linguistic comparison.

# 4.4 Logical operators

Logical operators are used to combine boolean expressions.

C# provides:

\| Operator | Meaning |\
\|---|---|\
\| `&&` | Logical AND |\
\| `||` | Logical OR |\
\| `!` | Logical NOT |\
\| `&` | Boolean AND (also bitwise AND) |\
\| `\|` | Boolean OR (also bitwise OR) |\
\| `^` | Boolean XOR (also bitwise XOR) |

Examples:

```
int age = 25;
bool hasLicense = true;

bool canDrive = age >= 18 && hasLicense;

Console.WriteLine(canDrive);
```

Output:

```
True
```

### Truth table

AND (`&&`)

| A     | B     | A && B |
| ----- | ----- | ------ |
| true  | true  | true   |
| true  | false | false  |
| false | true  | false  |
| false | false | false  |

OR (`||`)

| A     | B     | A \|\| B |
| ----- | ----- | -------- |
| true  | true  | true     |
| true  | false | true     |
| false | true  | true     |
| false | false | false    |

NOT (`!`)

| A     | !A    |
| ----- | ----- |
| true  | false |
| false | true  |

# 4.5 Short-circuit evaluation

This is particularly important when writing safe business logic.

Consider:

```
int age = 25;
bool hasLicense = true;

if (age >= 18 && hasLicense)
{
    Console.WriteLine("Allowed to drive");
}
```

How does `&&` work internally?

The left expression is evaluated first.

If it evaluates to `false`, the overall AND result must be false regardless of the right expression.

Therefore, C# doesn't evaluate the right expression.

This is called short-circuit evaluation.

Java and JavaScript behave similarly with `&&` and `||`.

### Why is this useful?

Consider:

```
string? name = null;

if (name != null && name.Length > 5)
{
    Console.WriteLine("Long name");
}
```

Execution:

1. Evaluate `name != null`.
2. Result is `false`.
3. Because of short-circuiting, the second expression isn't evaluated.
4. No null-reference exception occurs.

Without short-circuiting, evaluating `name.Length` would cause a problem.

### What about `||`?

Consider:

```
bool isAdmin = true;

if (isAdmin || CheckPermission())
{
    Console.WriteLine("Access allowed");
}
```

If `isAdmin` is already true, the right expression isn't evaluated.

The result is already known to be true.

### Difference between `&&` and `&`

For boolean operands:

```
bool result = false && SomeMethod();
```

`SomeMethod()` is not executed.

But:

```
bool result = false & SomeMethod();
```

`SomeMethod()` is executed because `&` doesn't short-circuit boolean evaluation.

The same distinction applies to `||` and `|`.

Practical rule: Use `&&` and `||` for ordinary conditional logic. Use `&` and `|` when you specifically need non-short-circuit evaluation or bitwise operations.

# 4.6 Ternary operator

The ternary operator is a compact alternative to a simple `if-else`.

Java:

```
String result = age >= 18 ? "Adult" : "Minor";
```

JavaScript:

```
const result = age >= 18 ? "Adult" : "Minor";
```

C#:

```
int age = 25;

string result = age >= 18 ? "Adult" : "Minor";
```

Syntax:

```
condition ? valueIfTrue : valueIfFalse;
```

Example in a real application:

```
decimal balance = 1000;

string accountStatus =
    balance > 0 ? "Active" : "Inactive";
```

You can also nest ternary expressions:

```
int score = 85;

string grade =
    score >= 90 ? "A" :
    score >= 75 ? "B" :
    score >= 50 ? "C" :
    "F";
```

This works, but nested ternary expressions can become difficult to read.

For more complex conditions, use `if-else` or a switch expression.

# 4.7 Bitwise operators

Bitwise operators work on individual bits of integer values.

These are useful in:

- Permission systems.
- Feature flags.
- Network protocols.
- Binary file processing.
- Low-level programming.

Let's understand the intuition.

An integer is represented internally using bits.

For example:

```
5 = 00000101
3 = 00000011
```

### A. Bitwise AND (`&`)

```
int result = 5 & 3;

Console.WriteLine(result);
```

Calculation:

```
  00000101
& 00000011
-----------
  00000001
```

Result:

```
1
```

A bit is set in the result only when the corresponding bit is set in both operands.

### B. Bitwise OR (`|`)

```
int result = 5 | 3;
```

Calculation:

```
  00000101
| 00000011
-----------
  00000111
```

Result:

```
7
```

A bit is set if it is set in either operand.

### C. XOR (`^`)

```
int result = 5 ^ 3;
```

Calculation:

```
  00000101
^ 00000011
-----------
  00000110
```

Result:

```
6
```

A bit is set if the corresponding bits differ.

### D. Bitwise NOT (`~`)

```
int result = ~5;
```

This inverts every bit.

For a 32-bit signed integer, the result is:

```
-6
```

### E. Shift operators

Left shift:

```
int result = 5 << 1;
```

```
00000101 << 1

00001010
```

Result:

```
10
```

Right shift:

```
int result = 8 >> 1;
```

```
00001000 >> 1

00000100
```

Result:

```
4
```

These operations are useful when you need to manipulate individual bits.

## Practical example: Permission management

Imagine a system that manages permissions.

We have:

```
const int Read = 1;
const int Write = 2;
const int Delete = 4;
```

Why these values?

```
Read   = 001
Write  = 010
Delete = 100
```

Each permission gets its own bit.

Now assign multiple permissions:

```
int permissions = Read | Write;
```

Result:

```
001 | 010 = 011
```

Now how do we check whether a permission exists?

```
bool canRead = (permissions & Read) != 0;

bool canDelete = (permissions & Delete) != 0;
```

Results:

```
canRead   = true
canDelete = false
```

Why?

```
permissions = 011
Read        = 001

011 & 001 = 001
```

At least one relevant bit is set.

Now let's add Delete permission:

```
permissions |= Delete;
```

And remove Write permission:

```
permissions &= ~Write;
```

This is a common pattern for representing multiple boolean flags compactly.

C# also provides a cleaner abstraction using `[Flags]` enums. We'll revisit this when we study enums.

# 4.8 Null-related operators

These operators are particularly useful when working with optional data.

We briefly saw them in the previous chapter. Now let's understand them together.

## A. Null-coalescing operator (`??`)

Suppose:

```
string? name = null;
```

You want to use a default value when `name` is null.

You could write:

```
string result;

if (name != null)
{
    result = name;
}
else
{
    result = "Unknown";
}
```

But C# provides a more concise way:

```
string result = name ?? "Unknown";
```

Meaning:

> Use the left value if it is not null; otherwise, use the right value.

Example:

```
int? age = null;

int actualAge = age ?? 18;
```

Result:

```
18
```

### Null-coalescing assignment (`??=`)

Suppose:

```
string? name = null;
```

You want to assign a default value only if it's null.

Instead of:

```
if (name == null)
{
    name = "Unknown";
}
```

Write:

```
name ??= "Unknown";
```

Now `name` contains `"Unknown"`.

If `name` already contains a value, it remains unchanged.

This is particularly useful for optional configuration values and default initialization.

## B. Null-conditional operator (`?.`)

Consider:

```
User? user = null;
```

You want to access:

```
user.Name
```

But this would throw a `NullReferenceException`.

Instead:

```
string? name = user?.Name;
```

If `user` is null, the whole expression evaluates to null.

You can chain it:

```
string? city = user?.Address?.City;
```

Each access is performed only if the preceding object is non-null.

This is conceptually similar to optional chaining in JavaScript:

```
const city = user?.address?.city;
```

## C. Null-forgiving operator (`!`)

Consider:

```
string? name = GetName();
```

Suppose you know that the returned value cannot be null, but the compiler cannot determine that.

You can write:

```
string actualName = name!;
```

The `!` tells the compiler:

> Treat this expression as non-null.

Important: This does not check whether the value is null at runtime.

It doesn't prevent a `NullReferenceException`.

It only suppresses the relevant nullable warning.

Use it carefully. It is better to perform actual validation whenever possible.

# 4.9 Conditional statements — if, else if, else

Let's move to control flow.

These work almost identically in Java and C#.

```
int age = 25;

if (age >= 18)
{
    Console.WriteLine("Adult");
}
else
{
    Console.WriteLine("Minor");
}
```

Multiple conditions:

```
int score = 85;

if (score >= 90)
{
    Console.WriteLine("Grade A");
}
else if (score >= 75)
{
    Console.WriteLine("Grade B");
}
else if (score >= 50)
{
    Console.WriteLine("Grade C");
}
else
{
    Console.WriteLine("Fail");
}
```

Execution stops at the first matching branch.

### Important difference from JavaScript

In JavaScript:

```
if (1) {
    console.log("True");
}
```

This is valid because JavaScript performs truthiness conversion.

In C#:

```
if (1)
{
    Console.WriteLine("True");
}
```

This is a compilation error.

C# requires a boolean expression:

```
if (number > 0)
{
    Console.WriteLine("Positive");
}
```

This strictness helps prevent accidental conditions.

# 4.10 Switch statements

Suppose we have an order status.

```
string status = "Paid";
```

We want to perform different operations depending on the status.

Using `if-else`:

```
if (status == "Pending")
{
    Console.WriteLine("Payment pending");
}
else if (status == "Paid")
{
    Console.WriteLine("Payment completed");
}
else if (status == "Failed")
{
    Console.WriteLine("Payment failed");
}
else
{
    Console.WriteLine("Unknown status");
}
```

This works.

But when comparing a single value against multiple possible values, a switch statement is often cleaner.

```
switch (status)
{
    case "Pending":
        Console.WriteLine("Payment pending");
        break;

    case "Paid":
        Console.WriteLine("Payment completed");
        break;

    case "Failed":
        Console.WriteLine("Payment failed");
        break;

    default:
        Console.WriteLine("Unknown status");
        break;
}
```

Java has a similar switch statement.

### Why do we need break?

In C#, ordinary non-empty switch sections don't implicitly fall through to the next section.

`break` exits the switch.

Unlike traditional Java switch statements, C# doesn't allow ordinary fall-through from one non-empty section into another.

However, multiple labels can share the same section:

```
switch (status)
{
    case "Pending":
    case "Processing":
        Console.WriteLine("Payment is in progress");
        break;

    case "Paid":
        Console.WriteLine("Payment completed");
        break;

    default:
        Console.WriteLine("Unknown status");
        break;
}
```

Both `"Pending"` and `"Processing"` execute the same section.

# 4.11 Switch expressions — an important modern C# feature

Now let's look at something more interesting.

Suppose you want to map an order status to a message.

Using a switch statement:

```
string message;

switch (status)
{
    case "Pending":
        message = "Payment pending";
        break;

    case "Paid":
        message = "Payment completed";
        break;

    case "Failed":
        message = "Payment failed";
        break;

    default:
        message = "Unknown status";
        break;
}
```

C# provides a switch expression.

```
string message = status switch
{
    "Pending" => "Payment pending",

    "Paid" => "Payment completed",

    "Failed" => "Payment failed",

    _ => "Unknown status"
};
```

Much cleaner!

Let's understand its syntax.

```
variable switch
{
    pattern1 => result1,
    pattern2 => result2,
    _ => defaultResult
};
```

The `_` is a discard pattern.

It matches anything that hasn't matched the earlier arms.

### Switch statement vs switch expression

| Switch statement                            | Switch expression                     |
| ------------------------------------------- | ------------------------------------- |
| Controls execution flow                     | Produces a value                      |
| Uses `case`                                 | Uses pattern arms                     |
| Usually uses `break`                        | No `break` required                   |
| Can execute multiple statements per section | Each arm produces a result expression |
| Good for actions                            | Good for value selection              |

### Practical backend example

Imagine calculating a discount based on customer type.

```
string customerType = "Premium";

decimal discount = customerType switch
{
    "Regular" => 0.05m,

    "Premium" => 0.15m,

    "Enterprise" => 0.25m,

    _ => 0m
};
```

This is much cleaner than multiple `if-else` statements when you're mapping values to results.

# 4.12 Pattern matching — one of C#'s powerful features

This is one of the most useful modern C# features.

You may initially think of pattern matching as just another switch statement feature.

But it goes beyond that.

Pattern matching allows us to check:

- Whether a value has a particular type.
- Whether a value matches a constant.
- Whether a value falls within a range.
- Whether an object's properties satisfy conditions.
- Whether a value matches a particular structure.

Let's explore progressively.

## A. Type pattern

Suppose:

```
object value = "Hello";
```

We want to determine whether the object contains a string.

Traditional approach:

```
if (value is string)
{
    Console.WriteLine("It's a string");
}
```

C# supports a cleaner approach:

```
if (value is string text)
{
    Console.WriteLine(text);
}
```

What happened?

The expression:

```
value is string text
```

does two things:

1. Checks whether `value` is a string.
2. If it is, creates a variable `text` containing that string reference.

This is called a declaration pattern.

Java has similar capabilities with modern pattern matching for `instanceof`:

```
if (value instanceof String text) {
    System.out.println(text);
}
```

C# calls this general family of features pattern matching.

## B. Constant pattern

You can directly match a value.

```
int statusCode = 200;

if (statusCode is 200)
{
    Console.WriteLine("Success");
}
```

You can even combine patterns:

```
if (statusCode is 200 or 201 or 204)
{
    Console.WriteLine("Successful response");
}
```

This is more expressive than repeatedly writing equality checks.

## C. Relational patterns

Suppose:

```
int age = 25;
```

You can write:

```
if (age is >= 18 and < 60)
{
    Console.WriteLine("Adult working age");
}
```

Notice:

```
>= 18 and < 60
```

This is pattern matching.

It is not the same as ordinary boolean operators, although it expresses a similar logical condition.

You could also use:

```
if (age >= 18 && age < 60)
{
    Console.WriteLine("Adult working age");
}
```

Both are valid.

### Using relational patterns in switch expressions

```
int score = 85;

string grade = score switch
{
    >= 90 => "A",

    >= 75 => "B",

    >= 50 => "C",

    _ => "F"
};
```

Notice that we don't need to specify upper bounds for every condition.

The switch expression evaluates its arms in order.

For `score = 85`:

1. Is it at least 90? No.
2. Is it at least 75? Yes.
3. Return `"B"`.

This is a nice way to express ranges.

## D. Property patterns

Now let's make things more interesting.

Consider:

```
public class Order
{
    public string Status { get; set; } = string.Empty;

    public decimal Amount { get; set; }
}
```

Suppose we need to check:

- Status is `"Paid"`.
- Amount is greater than 1000.

Traditional approach:

```
if (order.Status == "Paid" && order.Amount > 1000)
{
    Console.WriteLine("Large paid order");
}
```

C# property patterns:

```
if (order is
    {
        Status: "Paid",
        Amount: > 1000
    })
{
    Console.WriteLine("Large paid order");
}
```

What's happening?

We're matching the object's properties against a pattern.

The condition checks that:

```
order.Status == "Paid"
AND
order.Amount > 1000
```

This can make more complex object checks easier to read.

## E. Switch expressions with object patterns

Consider:

```
public class Payment
{
    public string Status { get; set; } = string.Empty;

    public decimal Amount { get; set; }
}
```

We want to generate a message based on payment state.

```
string GetPaymentMessage(Payment payment)
{
    return payment switch
    {
        {
            Status: "Paid",
            Amount: > 1000
        } => "Large payment completed",

        {
            Status: "Paid"
        } => "Payment completed",

        {
            Status: "Failed"
        } => "Payment failed",

        _ => "Payment is pending or unknown"
    };
}
```

This checks the properties and selects the matching result.

Notice the order:

The more specific pattern comes first.

The more general `"Paid"` pattern comes later.

Otherwise, the general pattern could match before the more specific one.

Practical takeaway: Pattern matching is particularly useful when processing different object shapes, states, and conditions without deeply nested `if-else` blocks.

# 4.13 Loops

Now let's explore loops.

C# provides the standard loops you're familiar with:

- `for`
- `while`
- `do-while`
- `foreach`

## A. For loop

Java:

```
for (int i = 0; i < 5; i++) {
    System.out.println(i);
}
```

C#:

```
for (int i = 0; i < 5; i++)
{
    Console.WriteLine(i);
}
```

Output:

```
0
1
2
3
4
```

Syntax:

```
for (initialization; condition; increment)
{
    // Statements
}
```

Execution:

1. Initialize variable.
2. Check condition.
3. Execute body.
4. Execute increment.
5. Check condition again.
6. Repeat until false.

This is identical to Java's basic for-loop behavior.

## B. While loop

```
int count = 0;

while (count < 5)
{
    Console.WriteLine(count);

    count++;
}
```

The condition is checked before executing the loop body.

If the condition is initially false, the body won't execute even once.

## C. Do-while loop

```
int count = 0;

do
{
    Console.WriteLine(count);

    count++;

} while (count < 5);
```

The condition is checked after executing the body.

Therefore, the body executes at least once.

This is useful when the operation must happen before the condition is checked.

For example, a menu that needs to be displayed at least once.

## D. Foreach loop

This is particularly important in everyday C# development.

Suppose:

```
int[] numbers = { 10, 20, 30, 40 };
```

You want to process every element.

Using a traditional for loop:

```
for (int i = 0; i < numbers.Length; i++)
{
    Console.WriteLine(numbers[i]);
}
```

Using foreach:

```
foreach (int number in numbers)
{
    Console.WriteLine(number);
}
```

Much cleaner.

Java equivalent:

```
for (int number : numbers) {
    System.out.println(number);
}
```

JavaScript equivalent:

```
for (const number of numbers) {
    console.log(number);
}
```

### What exactly happens internally in foreach?

This is worth understanding.

Consider:

```
foreach (int number in numbers)
{
    Console.WriteLine(number);
}
```

Conceptually, C# uses an enumerator to traverse the collection.

The process looks like:

```
Get Enumerator
      |
      v
Move to Next Element
      |
      v
Is Another Element Available?
      |
   Yes|       No
      |        |
      v        v
Read Element  Exit Loop
      |
      v
Execute Body
      |
      +------> Move to Next Element
```

This is why many different collection types support `foreach`.

They expose the necessary enumeration pattern.

For example:

```
List<int> numbers = new() { 1, 2, 3 };

foreach (int number in numbers)
{
    Console.WriteLine(number);
}
```

You don't need to know how the list internally stores its elements.

You only need its enumeration behavior.

We'll understand collections and enumerators in depth in Chapter 7.

### Can we modify the collection inside foreach?

Be careful.

Consider:

```
var numbers = new List<int> { 1, 2, 3 };

foreach (var number in numbers)
{
    numbers.Add(number * 2);
}
```

This generally throws `InvalidOperationException` because a `List<T>` detects structural modification during enumeration.

Why?

The enumerator expects the collection to remain structurally unchanged while iterating.

A common solution is to use a separate collection:

```
var numbers = new List<int> { 1, 2, 3 };

var additionalNumbers = new List<int>();

foreach (var number in numbers)
{
    additionalNumbers.Add(number * 2);
}

numbers.AddRange(additionalNumbers);
```

If you need to modify elements by index, a `for` loop may be more appropriate.

# 4.14 break, continue, and return

These control how execution leaves or skips parts of a program.

## A. break

`break` terminates the nearest enclosing loop or switch.

```
for (int i = 0; i < 10; i++)
{
    if (i == 5)
    {
        break;
    }

    Console.WriteLine(i);
}
```

Output:

```
0
1
2
3
4
```

When `i == 5`, the loop terminates.

It also works inside a switch statement.

## B. continue

`continue` skips the remainder of the current iteration and proceeds to the next iteration.

```
for (int i = 0; i < 5; i++)
{
    if (i == 2)
    {
        continue;
    }

    Console.WriteLine(i);
}
```

Output:

```
0
1
3
4
```

When `i == 2`, the remaining statements in that iteration are skipped.

## C. return

`return` exits the current method.

Consider:

```
int Calculate(int number)
{
    if (number < 0)
    {
        return 0;
    }

    return number * 2;
}
```

When `number` is negative, the method immediately returns zero.

Unlike `break`, which exits a loop or switch, `return` exits the entire current method.

# 4.15 Integer overflow and checked arithmetic

This is an interesting C# feature that you should know.

Consider:

```
int number = int.MaxValue;

number++;

Console.WriteLine(number);
```

In the default unchecked arithmetic context, integer arithmetic can overflow and wrap around.

For a 32-bit signed integer, this can produce:

```
-2147483648
```

Why?

A signed integer has a finite range:

```
-2,147,483,648 to 2,147,483,647
```

You cannot represent a value greater than its maximum.

C# provides `checked` and `unchecked` contexts.

### Checked

```
int number = int.MaxValue;

checked
{
    number++;
}
```

Now an `OverflowException` is thrown.

This lets us detect unexpected arithmetic overflow.

You can also write:

```
int result = checked(int.MaxValue + 1);
```

### Unchecked

```
int number = int.MaxValue;

unchecked
{
    number++;
}

Console.WriteLine(number);
```

This explicitly allows overflow behavior for supported integral operations.

### Why does this matter in backend systems?

Imagine calculating:

```
int totalAmount = quantity * price;
```

Suppose both values are large.

The calculation may overflow before you assign the result to another variable.

For financial calculations, use suitable numeric types such as `decimal`, and apply proper business validation and precision rules.

For safety-critical integer calculations, checked arithmetic may be appropriate.

One more detail: floating-point arithmetic, such as `double`, behaves differently and generally produces infinity for sufficiently large magnitudes rather than throwing `OverflowException`.

# 4.16 Putting everything together — a practical example

Let's implement a simple order-processing decision.

Our requirements:

- If the order is null, return an error.
- If the order is cancelled, reject processing.
- If the order is paid and amount exceeds 10,000, classify it as high-value.
- If the order is paid, process normally.
- Otherwise, wait for payment.

Our model:

```
public class Order
{
    public long Id { get; set; }

    public string Status { get; set; } = string.Empty;

    public decimal Amount { get; set; }
}
```

Implementation:

```
string ProcessOrder(Order? order)
{
    if (order is null)
    {
        return "Invalid order";
    }

    if (order.Status is "Cancelled")
    {
        return "Order cancelled";
    }

    return order switch
    {
        {
            Status: "Paid",
            Amount: > 10000
        } => "High-value order processing",

        {
            Status: "Paid"
        } => "Order processing",

        {
            Status: "Pending"
        } => "Waiting for payment",

        _ => "Unknown order status"
    };
}
```

Notice how we combined:

- Null checks.
- Pattern matching.
- Relational patterns.
- Switch expressions.
- Early returns.

This is a reasonably clean way to express business rules.

Of course, a production system might use enums instead of strings for order status, and more sophisticated domain logic. We'll learn enums in a later chapter.

# 4.17 Summary: What should you remember?

\| Concept | Important takeaway |\
\|---|---|\
\| Arithmetic | Integer division truncates the fractional part |\
\| Comparison | `==` on strings compares string contents |\
\| Logical operators | `&&` and `||` short-circuit |\
\| Ternary | Compact conditional value selection |\
\| Bitwise | Manipulates individual bits |\
\| `??` | Provides a fallback for null |\
\| `?.` | Safely accesses members of potentially null objects |\
\| `??=` | Assigns only when the left side is null |\
\| `if-else` | General conditional execution |\
\| `switch` | Selects a statement section based on a value/pattern |\
\| Switch expression | Selects and returns a value |\
\| Pattern matching | Tests values, types, ranges, and object properties |\
\| `foreach` | Iterates over enumerable elements |\
\| `break` | Exits the nearest loop or switch |\
\| `continue` | Skips the current iteration |\
\| `return` | Exits the current method |\
\| `checked` | Detects integer arithmetic overflow |

# 5. Practice exercises

Try implementing these without looking at the examples above.

Exercise 1 — Grade Calculator

Write a method:

```
string GetGrade(int score)
```

Requirements:

- 90–100: A
- 75–89: B
- 60–74: C
- 40–59: D
- Below 40: F
- Invalid scores: return `"Invalid"`

Try implementing it using:

1. `if-else`.
2. A switch expression with relational patterns.

Exercise 2 — Order Status

Create an `Order` class with:

```
long Id;
string Status;
decimal Amount;
```

Use a switch expression to return:

- `"High Value"` for paid orders above 10,000.
- `"Normal Payment"` for other paid orders.
- `"Awaiting Payment"` for pending orders.
- `"Payment Failed"` for failed orders.
- `"Unknown"` otherwise.

Exercise 3 — Permission Flags

Using bitwise operators:

```
const int Read = 1;
const int Write = 2;
const int Delete = 4;
```

Implement:

```
bool HasPermission(int permissions, int permission);

int GrantPermission(int permissions, int permission);

int RevokePermission(int permissions, int permission);
```

Exercise 4 — Find duplicates

Given:

```
int[] numbers = { 1, 2, 3, 2, 4, 5, 3, 6 };
```

Use loops to identify duplicate elements.

We'll learn the more idiomatic collection-based solutions in upcoming chapters.

# 6. Knowledge check

Make sure you can explain:

1. What's the difference between `&&` and `&`?
2. Why does C# use short-circuit evaluation?
3. What's the difference between a switch statement and a switch expression?
4. What is pattern matching, and why is it useful?
5. How do relational patterns work?
6. How does `foreach` iterate over a collection internally?
7. What's the difference between `break`, `continue`, and `return`?
8. What happens when an integer overflows?
9. What do `checked` and `unchecked` do?
10. Why doesn't C# allow numbers directly inside `if` conditions?

## Next chapter — Chapter 5: Methods and Parameters in C\#

This is where we'll start understanding some important differences between Java and C#.

We'll cover:

- Method declarations and return types.
- Method overloading.
- Optional and named parameters.
- `ref`, `out`, and `in` parameters.
- Pass by value vs pass by reference.
- `params` keyword (similar to JavaScript rest parameters / Java varargs).
- Expression-bodied methods.
- Local functions.
- Extension methods (very useful in modern C#).
- Method scope and variable lifetime.

Particularly important: We'll understand exactly how C# passes arguments to methods, including the difference between passing a reference-type variable by value and using the `ref` keyword. This is one of the most commonly misunderstood C# concepts.

Say next when ready.
