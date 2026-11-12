# Chapter 6 — Strings and Arrays in C\#

Strings and arrays are fundamental building blocks in almost every application.

You'll use them constantly when working with:

- HTTP requests and responses.
- JSON serialization and deserialization.
- Database results.
- Configuration and environment variables.
- File processing.
- API integrations.
- Collections of records and business data.

We'll understand how C# handles these types internally and compare them with Java and JavaScript.

We'll cover:

Strings

1. String fundamentals and immutability.
2. String interpolation.
3. Normal, verbatim, and raw string literals.
4. String comparison.
5. Common string operations.
6. StringBuilder and why it exists.
7. String parsing and formatting.
8. Unicode considerations.

Arrays\
9\. Array fundamentals.\
10\. Array initialization and default values.\
11\. Passing arrays to methods.\
12\. Copying arrays and reference behavior.\
13\. Multidimensional and jagged arrays.\
14\. Common array operations.\
15\. Arrays vs Lists vs Java and JavaScript arrays.

# Part 1 — Strings

## 6.1 Understanding strings in C\#

Let's start with something familiar.

Java:

```
String name = "John";
```

JavaScript:

```
const name = "John";
```

C#:

```
string name = "John";
```

C# `string` is an alias for:

```
System.String
```

Both are equivalent:

```
string name = "John";

System.String anotherName = "Smith";
```

### Is string a value type or reference type?

String is a reference type.

```
string name = "John";
```

The variable refers to a string object.

However, strings behave somewhat differently from ordinary mutable reference types because they're immutable.

Let's understand that.

## 6.2 String immutability

Immutability means that once a string object is created, its contents cannot be changed.

Consider:

```
string name = "John";

name = name + " Smith";

Console.WriteLine(name);
```

Output:

```
John Smith
```

It might look like we're modifying the original string.

But we're not.

Let's understand what happens.

Initially:

```
string name = "John";
```

Conceptually:

```
name --------> String Object A

               "John"
```

Now:

```
name = name + " Smith";
```

The concatenation operation creates a new string object containing the resulting characters.

Conceptually:

```
                 String Object A
name             "John"
 |                 (Unchanged)
 |
 +-------------> String Object B

                 "John Smith"
```

The `name` variable now refers to the new string.

The original string remains unchanged.

If nothing else references the original string, it eventually becomes eligible for garbage collection. String literals may also be stored in the runtime's intern pool, so actual object lifetime can be more nuanced.

### Java comparison

Java strings are also immutable.

```
String name = "John";

name = name + " Smith";
```

This behaves conceptually the same way.

### JavaScript comparison

JavaScript strings are also immutable primitive values.

```
let name = "John";

name = name + " Smith";
```

Again, you get a new string value.

### Why are strings immutable?

There are several advantages.

1\. Thread safety

Multiple threads can safely read the same string without worrying that another thread will modify its contents.

2\. Predictability

Once you receive a string, its contents won't unexpectedly change because another part of the application modified the same object.

3\. Hashing and dictionary keys

Strings are commonly used as keys in hash-based collections.

Immutability makes their hash-based behavior predictable.

4\. String interning

Identical string literals can sometimes share a single stored instance through the string intern pool.

For example:

```
string a = "Hello";
string b = "Hello";
```

The runtime may reuse the same interned string instance.

Don't depend on reference equality for checking string contents. Use value comparison.

Remember: Strings are reference types, but immutable.

## 6.3 String concatenation

The simplest way to combine strings is using `+`.

```
string firstName = "John";
string lastName = "Smith";

string fullName = firstName + " " + lastName;

Console.WriteLine(fullName);
```

Output:

```
John Smith
```

You can also use compound assignment:

```
string message = "Hello";

message += " World";
```

Remember that this creates a resulting string rather than modifying the existing string object.

### The potential problem with repeated concatenation

Consider:

```
string result = "";

for (int i = 0; i < 1000; i++)
{
    result += i;
}
```

What happens?

Each concatenation produces a new resulting string.

Repeatedly copying the existing content can become expensive.

For a long sequence of concatenations, this can cause:

- Many temporary string allocations.
- Additional memory consumption.
- Unnecessary copying.

This is where `StringBuilder` becomes useful.

We'll explore it shortly.

One nuance: the compiler and runtime can optimize some simple string concatenations. Don't assume every use of `+` is inefficient. The concern is repeated modification in loops or complex concatenation workflows.

# 6.4 String interpolation

This is one of the most common features you'll use in C#.

Suppose you want to construct a message.

Without interpolation:

```
string name = "John";
int age = 25;

string message = "My name is " + name +
                 " and I am " + age + " years old.";
```

This works, but can get messy.

C# provides string interpolation.

```
string name = "John";
int age = 25;

string message = $"My name is {name} and I am {age} years old.";

Console.WriteLine(message);
```

Output:

```
My name is John and I am 25 years old.
```

Notice the `$` before the string.

It allows expressions inside `{}`.

### You can evaluate expressions

```
int a = 10;
int b = 20;

string result = $"Sum = {a + b}";
```

Output:

```
Sum = 30
```

### You can call methods

```
string name = "john";

string message = $"Hello, {name.ToUpper()}";
```

Output:

```
Hello, JOHN
```

### You can format values

```
decimal amount = 1234.567m;

string message = $"Amount: {amount:F2}";
```

Output:

```
Amount: 1234.57
```

`F2` means fixed-point formatting with two decimal places.

Other examples:

```
decimal amount = 1234.567m;

Console.WriteLine($"{amount:F2}");
Console.WriteLine($"{amount:N2}");
Console.WriteLine($"{amount:C}");
```

The formats mean:

| Format | Purpose                                       |
| ------ | --------------------------------------------- |
| `F2`   | Fixed-point with two decimal places           |
| `N2`   | Number formatting with two decimal places     |
| `C`    | Currency formatting                           |
| `P2`   | Percentage formatting with two decimal places |

The exact currency symbol and separators depend on the current culture.

For backend systems, this matters.

### Culture-aware formatting

Suppose your application runs in different countries.

The same numeric value can be formatted differently:

```
en-US: $1,234.50
en-IN: ₹1,234.50
de-DE: 1.234,50 €
```

For user-facing output, culture-aware formatting may be appropriate.

For machine-readable data, such as API payloads, database-independent numeric representations, and protocol values, prefer invariant formatting where required.

Example:

```
using System.Globalization;

decimal amount = 1234.50m;

string result = amount.ToString(
    CultureInfo.InvariantCulture
);
```

This avoids formatting the value according to the server's current culture.

Practical recommendation: Use interpolation for readable string construction. Be deliberate about culture when formatting dates, numbers, and monetary values.

# 6.5 Different types of string literals

C# provides several ways to define strings.

These become particularly useful when working with:

- File paths.
- Regular expressions.
- SQL.
- JSON.
- HTML.
- Multiline text.

## A. Normal string literals

```
string message = "Hello World";
```

Normal string literals support escape sequences.

For example:

```
string message = "Hello\nWorld";

Console.WriteLine(message);
```

Output:

```
Hello
World
```

Some commonly used escape sequences:

| Escape | Meaning         |
| ------ | --------------- |
| `\n`   | New line        |
| `\t`   | Tab             |
| `\r`   | Carriage return |
| `\\`   | Backslash       |
| `\"`   | Double quote    |
| `\'`   | Single quote    |
| `\0`   | Null character  |

Example:

```
string message = "Hello \"John\"";

Console.WriteLine(message);
```

Output:

```
Hello "John"
```

## B. Verbatim strings

Suppose you want to store a Windows file path.

```
string path = "C:\\Users\\John\\Documents\\file.txt";
```

Why so many backslashes?

Because `\` is an escape character in normal string literals.

C# provides verbatim strings using `@`.

```
string path = @"C:\Users\John\Documents\file.txt";
```

Much cleaner!

In a verbatim string, backslashes are treated literally.

### Multiline strings

```
string message = @"Hello John,

Welcome to our application.

Your account has been created successfully.";
```

You can write multiline text without explicitly using `\n`.

### How do you escape quotes?

In verbatim strings, double quotes are represented using two double quotes.

```
string message = @"He said ""Hello John""";
```

Output:

```
He said "Hello John"
```

## C. Raw string literals

This is a relatively modern C# feature introduced in C# 11.

Imagine working with JSON.

Without raw string literals:

```
string json = "{ \"name\": \"John\", \"age\": 25 }";
```

Escaping every quote makes it harder to read.

With raw string literals:

```
string json = """
{
    "name": "John",
    "age": 25
}
""";
```

Much cleaner!

Raw string literals use at least three consecutive double quotes.

They allow you to include quotes and backslashes without ordinary escape processing.

### When are raw string literals useful?

For example:

JSON

```
string json = """
{
    "user": {
        "id": 1,
        "name": "John"
    }
}
""";
```

SQL

```
string sql = """
    SELECT *
    FROM users
    WHERE is_active = true
    ORDER BY created_at DESC;
    """;
```

HTML

```
string html = """
    <html>
        <body>
            <h1>Hello World</h1>
        </body>
    </html>
    """;
```

Notice that the indentation of the closing delimiter determines how much indentation is removed from the content.

### Raw strings vs verbatim strings

| Feature                       | Verbatim `@`                         | Raw `"""`                                  |
| ----------------------------- | ------------------------------------ | ------------------------------------------ |
| Backslashes treated literally | Yes                                  | Yes                                        |
| Multiline                     | Yes                                  | Yes                                        |
| Quotes need escaping          | Yes, doubled quotes                  | Generally no                               |
| Introduced                    | C# 1                                 | C# 11                                      |
| Best for                      | File paths and ordinary literal text | JSON, SQL, HTML and complex multiline text |

You can also combine interpolation and raw strings:

```
string name = "John";

string json = $$"""
{
    "name": "{{name}}",
    "active": true
}
""";
```

Here, `$$` means interpolation expressions use double braces.

This is useful when the raw text itself contains braces, such as JSON.

# 6.6 String comparison

This is an important topic for backend developers.

Suppose:

```
string first = "John";
string second = "John";

Console.WriteLine(first == second);
```

Output:

```
True
```

As mentioned earlier, `==` for strings performs content-based comparison.

But what about:

```
string first = "john";
string second = "JOHN";

Console.WriteLine(first == second);
```

Output:

```
False
```

Because the default comparison is case-sensitive.

### Case-insensitive comparison

You could write:

```
bool result = string.Equals(
    first,
    second,
    StringComparison.OrdinalIgnoreCase
);
```

Now the comparison ignores case.

### Understanding StringComparison

C# provides different comparison strategies.

| Comparison                   | Behavior                                                 |
| ---------------------------- | -------------------------------------------------------- |
| `Ordinal`                    | Compares UTF-16 code units using ordinal rules           |
| `OrdinalIgnoreCase`          | Ordinal comparison ignoring case                         |
| `CurrentCulture`             | Uses current culture's linguistic rules                  |
| `CurrentCultureIgnoreCase`   | Current culture, ignoring case                           |
| `InvariantCulture`           | Culture-independent linguistic comparison                |
| `InvariantCultureIgnoreCase` | Culture-independent linguistic comparison, ignoring case |

Which should you use?

This matters more than it might initially seem.

Imagine you are comparing:

- User-facing natural language text.
- Username identifiers.
- API keys.
- Internal system identifiers.
- File extensions.

They may need different comparison rules.

### Practical guidance

For internal identifiers, tokens, keys, protocol values, and many technical comparisons, prefer:

```
StringComparison.Ordinal
```

For case-insensitive technical comparisons:

```
StringComparison.OrdinalIgnoreCase
```

For natural language sorting or user-facing linguistic comparisons, use appropriate culture-aware comparison.

Avoid blindly using `ToLower()` on both strings to perform case-insensitive comparisons. Use the comparison APIs directly.

### Example

Suppose you want to compare HTTP-like status strings:

```
string status = "completed";

bool isCompleted = string.Equals(
    status,
    "COMPLETED",
    StringComparison.OrdinalIgnoreCase
);
```

This is explicit and predictable.

# 6.7 Common string operations

C# provides many useful string methods.

Let's explore the ones you'll frequently use.

Assume:

```
string text = "  Hello CSharp World  ";
```

### A. Length

```
Console.WriteLine(text.Length);
```

Returns the number of UTF-16 code units in the string.

### B. Trim

Removes leading and trailing whitespace.

```
string result = text.Trim();
```

Result:

```
Hello CSharp World
```

You also have:

```
text.TrimStart();

text.TrimEnd();
```

### C. ToUpper and ToLower

```
string upper = text.ToUpper();

string lower = text.ToLower();
```

For culture-independent casing, you can use:

```
string normalized = text.ToLowerInvariant();
```

Be aware that culture-aware casing rules can vary.

For machine identifiers, explicit ordinal comparisons are usually better than normalizing case just to compare strings.

### D. Contains

```
string text = "Hello CSharp World";

bool result = text.Contains("CSharp");
```

Returns `true`.

You can also specify comparison behavior:

```
bool result = text.Contains(
    "csharp",
    StringComparison.OrdinalIgnoreCase
);
```

### E. StartsWith and EndsWith

```
string filename = "report.pdf";

bool isPdf = filename.EndsWith(
    ".pdf",
    StringComparison.OrdinalIgnoreCase
);
```

Similarly:

```
bool startsWithReport = filename.StartsWith(
    "report",
    StringComparison.OrdinalIgnoreCase
);
```

### F. Replace

```
string text = "Hello John";

string result = text.Replace("John", "Smith");
```

Result:

```
Hello Smith
```

Remember: strings are immutable.

`Replace()` returns a new string. It doesn't modify the original.

### G. Split

Suppose:

```
string csv = "John,Smith,25,India";
```

You want individual values.

```
string[] values = csv.Split(',');
```

Now:

```
foreach (string value in values)
{
    Console.WriteLine(value);
}
```

Output:

```
John
Smith
25
India
```

Notice that `Split()` returns an array of strings.

We'll use arrays extensively in the second part of this chapter.

### H. Join

The reverse operation is `string.Join()`.

```
string[] names = { "John", "Smith", "David" };

string result = string.Join(", ", names);
```

Output:

```
John, Smith, David
```

### I. IndexOf

Finds the position of a substring.

```
string text = "Hello World";

int index = text.IndexOf("World");
```

Result:

```
6
```

If the substring isn't found, it returns `-1`.

You can also specify a comparison:

```
int index = text.IndexOf(
    "world",
    StringComparison.OrdinalIgnoreCase
);
```

### J. Substring

Extract part of a string.

```
string text = "Hello World";

string result = text.Substring(0, 5);
```

Result:

```
Hello
```

The arguments are:

```
Substring(startIndex, length)
```

Modern C# also supports ranges and indices:

```
string result = text[0..5];
```

This produces the same substring.

The `^` operator represents an index from the end.

```
char lastCharacter = text[^1];
```

This retrieves the final UTF-16 code unit.

Be careful with empty strings because indexing outside the valid range throws an exception.

### Common string API summary

| Method                 | Purpose                            |
| ---------------------- | ---------------------------------- |
| `Length`               | Get string length                  |
| `Trim()`               | Remove leading/trailing whitespace |
| `ToUpper()`            | Convert to uppercase               |
| `ToLower()`            | Convert to lowercase               |
| `Contains()`           | Check whether text exists          |
| `StartsWith()`         | Check prefix                       |
| `EndsWith()`           | Check suffix                       |
| `Replace()`            | Replace text                       |
| `Split()`              | Split into an array                |
| `Join()`               | Combine multiple strings           |
| `IndexOf()`            | Find occurrence                    |
| `Substring()`          | Extract part of a string           |
| `IsNullOrEmpty()`      | Check null or empty                |
| `IsNullOrWhiteSpace()` | Check null, empty, or whitespace   |

# 6.8 StringBuilder — Why do we need it?

Let's understand this properly.

Remember that strings are immutable.

Consider:

```
string result = "";

for (int i = 0; i < 5; i++)
{
    result += i;
}
```

Each iteration creates a new resulting string.

Conceptually:

```
Iteration 1: ""    + "0" = "0"

Iteration 2: "0"   + "1" = "01"

Iteration 3: "01"  + "2" = "012"

Iteration 4: "012" + "3" = "0123"

Iteration 5: "0123" + "4" = "01234"
```

As the string becomes larger, more existing content may need to be copied.

Now imagine doing this for thousands of elements.

This can generate substantial temporary allocations.

### StringBuilder solves this problem

C# provides:

```
System.Text.StringBuilder
```

It maintains a mutable character buffer.

Instead of creating a completely new string for every append, it can modify its internal buffer.

Example:

```
using System.Text;

var builder = new StringBuilder();

for (int i = 0; i < 5; i++)
{
    builder.Append(i);
}

string result = builder.ToString();

Console.WriteLine(result);
```

Output:

```
01234
```

### How does it work internally?

Conceptually:

```
StringBuilder

Internal Character Buffer
+-------------------------+
| 0 | 1 | 2 | 3 | 4 | ... |
+-------------------------+
```

When you append content, it uses available capacity in its internal storage.

If necessary, it grows its buffer.

This avoids creating a new immutable string object for every append operation.

### Important StringBuilder methods

Append

```
builder.Append("Hello");
builder.Append(" World");
```

AppendLine

```
builder.AppendLine("Hello");
builder.AppendLine("World");
```

Adds a newline after each piece of text.

Insert

```
var builder = new StringBuilder("Hello World");

builder.Insert(6, "Beautiful ");

Console.WriteLine(builder);
```

Output:

```
Hello Beautiful World
```

Replace

```
var builder = new StringBuilder("Hello John");

builder.Replace("John", "Smith");
```

Remove

```
var builder = new StringBuilder("Hello World");

builder.Remove(5, 6);
```

Result:

```
Hello
```

Clear

```
builder.Clear();
```

Removes the current content.

ToString

```
string result = builder.ToString();
```

Returns the resulting string.

### String vs StringBuilder

| Feature              | String                            | StringBuilder                   |
| -------------------- | --------------------------------- | ------------------------------- |
| Mutable              | No                                | Yes                             |
| Content modification | Creates resulting strings         | Modifies internal buffer        |
| Best for             | Normal string operations          | Repeated modifications          |
| Thread safety        | Safe to share for reading         | Not inherently thread-safe      |
| Common use           | API messages, identifiers, values | Building large strings in loops |

### When should we use StringBuilder?

Consider:

```
var builder = new StringBuilder();

foreach (var user in users)
{
    builder.AppendLine(user.Name);
}
```

This is a reasonable use case.

However, don't automatically replace every `+` with `StringBuilder`.

For simple operations:

```
string fullName = firstName + " " + lastName;
```

or:

```
string fullName = $"{firstName} {lastName}";
```

is perfectly fine.

Mental model: Use strings for normal text values. Use StringBuilder when you need to build or modify a string repeatedly.

# 6.9 Parsing and converting strings

In backend development, data frequently arrives as strings.

For example:

```
"25"
"1000.50"
"true"
```

We need to convert these into appropriate types.

### A. Parse

```
string ageText = "25";

int age = int.Parse(ageText);
```

Now `age` is an integer.

But what happens if the input is invalid?

```
string ageText = "John";

int age = int.Parse(ageText);
```

This throws a `FormatException`.

This might be undesirable for user input.

### B. TryParse

C# provides `TryParse` methods.

```
string ageText = "25";

bool success = int.TryParse(ageText, out int age);

if (success)
{
    Console.WriteLine(age);
}
else
{
    Console.WriteLine("Invalid age");
}
```

If parsing fails, the method returns `false` rather than throwing a format-related exception.

This is generally preferable for ordinary external input validation.

### C. Decimal parsing

```
string amountText = "1500.50";

bool success = decimal.TryParse(
    amountText,
    out decimal amount
);
```

For production applications, consider culture explicitly when parsing formatted numeric input.

For example:

```
using System.Globalization;

string input = "1500.50";

bool success = decimal.TryParse(
    input,
    NumberStyles.Number,
    CultureInfo.InvariantCulture,
    out decimal amount
);
```

This ensures the input is interpreted using invariant numeric formatting conventions.

### Practical backend example

Suppose an HTTP query parameter contains an age:

```
GET /users?age=25
```

The raw query parameter is text.

You may need:

```
string ageText = "25";

if (!int.TryParse(ageText, out int age))
{
    Console.WriteLine("Invalid age");
}
else
{
    Console.WriteLine($"Age = {age}");
}
```

This is a very common pattern in backend development.

# 6.10 A note about Unicode and string length

This is a slightly advanced but important concept.

Consider:

```
string text = "Hello";

Console.WriteLine(text.Length);
```

Output:

```
5
```

That's straightforward.

But consider:

```
string text = "😀";

Console.WriteLine(text.Length);
```

Output:

```
2
```

Why?

C# strings use UTF-16 encoding.

A `char` represents a UTF-16 code unit, which is 16 bits.

Some Unicode characters need two UTF-16 code units to represent them.

Therefore:

`string.Length` counts UTF-16 code units, not necessarily human-perceived characters.

There are actually three different concepts:

1. UTF-16 code units.
2. Unicode code points (scalar values).
3. Grapheme clusters (what users perceive as individual characters).

For simple ASCII text, these concepts often align.

For international text, they don't always.

Modern .NET provides `System.Text.Rune` for working with Unicode scalar values.

For example:

```
using System.Text;

string text = "Hello 😀";

foreach (Rune rune in text.EnumerateRunes())
{
    Console.WriteLine(rune);
}
```

This iterates over Unicode scalar values rather than individual UTF-16 code units.

You don't need to use `Rune` in every application, but you should know that string length and displayed character count are not always the same.

# Part 2 — Arrays

Now let's move to arrays.

Arrays are important because many other data structures and collections are built on similar concepts.

# 6.11 What is an array?

An array is a data structure that stores multiple elements of the same type.

Java:

```
int[] numbers = { 10, 20, 30 };
```

JavaScript:

```
const numbers = [10, 20, 30];
```

C#:

```
int[] numbers = { 10, 20, 30 };
```

The syntax is almost identical to Java.

However, there is an important difference from JavaScript.

C# arrays are strongly typed and have a fixed length.

Consider:

```
int[] numbers = { 10, 20, 30 };
```

You cannot add a string:

```
numbers[0] = "John"; // Compilation error
```

You also cannot increase the original array's length.

The array has three elements.

If you need a dynamically growing collection, you generally use `List<T>`, which we'll study in the next chapter.

### How is an array represented internally?

Conceptually:

```
numbers
   |
   v
+-----+-----+-----+
| 10  | 20  | 30  |
+-----+-----+-----+
   0     1     2
```

Elements are accessed through zero-based indexes.

```
Console.WriteLine(numbers[0]); // 10
Console.WriteLine(numbers[1]); // 20
Console.WriteLine(numbers[2]); // 30
```

Trying to access an invalid index:

```
Console.WriteLine(numbers[3]);
```

throws `IndexOutOfRangeException`.

# 6.12 Declaring and initializing arrays

There are multiple ways to create arrays.

### A. Declare and initialize together

```
int[] numbers = { 10, 20, 30, 40 };
```

### B. Use new

```
int[] numbers = new int[] { 10, 20, 30, 40 };
```

Both are valid.

### C. Specify array size

```
int[] numbers = new int[5];
```

This creates an array containing five elements.

What values do these elements contain?

For `int`, the default value is `0`.

```
+---+---+---+---+---+
| 0 | 0 | 0 | 0 | 0 |
+---+---+---+---+---+
```

### D. Use inferred array creation

```
var numbers = new[] { 10, 20, 30 };
```

The compiler infers that this is an `int[]`.

### E. Empty array

You can create an empty array:

```
int[] numbers = Array.Empty<int>();
```

This is useful when you need an empty array without allocating a new empty array every time.

# 6.13 Default values in arrays

This relates to what we learned about value types and reference types.

Consider:

```
int[] numbers = new int[3];
```

Default values:

```
0, 0, 0
```

Now:

```
bool[] flags = new bool[3];
```

Default values:

```
false, false, false
```

Now:

```
string?[] names = new string?[3];
```

Default values:

```
null, null, null
```

Why?

Because reference-type array elements are initially null.

Let's summarize:

| Element type         | Default value |
| -------------------- | ------------- |
| `int`                | `0`           |
| `long`               | `0`           |
| `double`             | `0`           |
| `decimal`            | `0`           |
| `bool`               | `false`       |
| `char`               | `'\0'`        |
| Reference types      | `null`        |
| Nullable value types | `null`        |

This is useful when initializing arrays that will later be populated.

# 6.14 Accessing and modifying array elements

Consider:

```
int[] numbers = { 10, 20, 30, 40 };

numbers[0] = 100;

Console.WriteLine(numbers[0]);
```

Output:

```
100
```

You can access elements using:

```
numbers[index]
```

Some important properties:

```
int[] numbers = { 10, 20, 30, 40 };

Console.WriteLine(numbers.Length);
```

Output:

```
4
```

`Length` gives the total number of elements.

### Iterating over arrays

Traditional `for` loop:

```
for (int i = 0; i < numbers.Length; i++)
{
    Console.WriteLine(numbers[i]);
}
```

Using `foreach`:

```
foreach (int number in numbers)
{
    Console.WriteLine(number);
}
```

When you need the index, use `for`.

When you only need the elements, `foreach` is usually cleaner.

# 6.15 Passing arrays to methods

This connects directly with what we learned in Chapter 5.

Consider:

```
public void UpdateFirstElement(int[] numbers)
{
    numbers[0] = 100;
}
```

Call:

```
int[] values = { 10, 20, 30 };

UpdateFirstElement(values);

Console.WriteLine(values[0]);
```

Output:

```
100
```

Why did the caller's array change?

Remember:

Arrays are reference types.

When we pass the array to the method, the reference is copied.

Both variables refer to the same array object.

```
Caller                         Method

values -----------+             numbers -----------+
                  |                               |
                  v                               v
                 +-------------------------------+
                 | Array                         |
                 |-------------------------------|
                 | 10 | 20 | 30                  |
                 +-------------------------------+
```

Modifying the array elements through either reference affects the same object.

### What happens if we reassign the array variable?

Consider:

```
public void ReplaceArray(int[] numbers)
{
    numbers = new int[] { 100, 200, 300 };
}
```

Call:

```
int[] values = { 10, 20, 30 };

ReplaceArray(values);

Console.WriteLine(values[0]);
```

Output:

```
10
```

Why?

Because the reference is passed by value.

Reassigning the method's local parameter doesn't replace the caller's variable.

### What if we actually want to replace the caller's array?

Use `ref`.

```
public void ReplaceArray(ref int[] numbers)
{
    numbers = new int[] { 100, 200, 300 };
}
```

Call:

```
int[] values = { 10, 20, 30 };

ReplaceArray(ref values);

Console.WriteLine(values[0]);
```

Output:

```
100
```

This is the same distinction we learned for ordinary reference-type parameters.

# 6.16 Copying arrays

This is particularly important.

Consider:

```
int[] first = { 10, 20, 30 };

int[] second = first;
```

Did we create a new array?

No.

We copied the reference.

Both variables point to the same array.

```
first ------------+
                  |
                  v
               +-----+-----+-----+
               | 10  | 20  | 30  |
               +-----+-----+-----+
                  ^
                  |
second -----------+
```

Now:

```
second[0] = 100;

Console.WriteLine(first[0]);
```

Output:

```
100
```

Because there is only one array object.

### How do we create an actual copy?

C# provides several options.

#### A. Array.Copy()

```
int[] first = { 10, 20, 30 };

int[] second = new int[first.Length];

Array.Copy(first, second, first.Length);

second[0] = 100;

Console.WriteLine(first[0]);  // 10

Console.WriteLine(second[0]); // 100
```

Now we have two different arrays.

#### B. Clone()

```
int[] first = { 10, 20, 30 };

int[] second = (int[])first.Clone();
```

This creates a shallow copy of the array.

For an array of integers, this effectively copies all element values.

#### C. ToArray()

If you work with LINQ, you may also see:

```
int[] second = first.ToArray();
```

This creates a new array containing the elements.

We'll understand LINQ in detail in a later chapter.

### Important: Shallow copy vs Deep copy

Let's understand this using reference-type elements.

Consider:

```
public class User
{
    public string Name { get; set; } = string.Empty;
}
```

Create an array:

```
User[] first =
{
    new User { Name = "John" },
    new User { Name = "Smith" }
};
```

Now clone it:

```
User[] second = (User[])first.Clone();
```

Are the user objects also duplicated?

No!

Only the array itself is copied.

The elements are references to the original user objects.

Visualize:

```
first array                      second array

+-----------+                    +-----------+
| Reference | -----------------> | User A    |
+-----------+                    | Name John |
                                 +-----------+

+-----------+                    +-----------+
| Reference |                    | Reference |
+-----------+                    +-----------+
      |                                 |
      +-------------> User B <----------+
                       Name Smith
```

Both arrays contain references to the same user objects.

Therefore:

```
second[0].Name = "Updated";

Console.WriteLine(first[0].Name);
```

Output:

```
Updated
```

This is expected.

### Summary

| Operation        | Result                                                                  |
| ---------------- | ----------------------------------------------------------------------- |
| `second = first` | Same array object                                                       |
| `Array.Copy()`   | New array, element values/references copied                             |
| `Clone()`        | New array, shallow copy                                                 |
| `ToArray()`      | New array, elements copied                                              |
| Deep copy        | Requires explicitly duplicating nested/reference objects as appropriate |

This distinction becomes very important when working with mutable domain objects.

# 6.17 Multidimensional arrays

C# supports multidimensional arrays.

Suppose we want to represent a matrix.

```
1 2 3
4 5 6
7 8 9
```

We can use a two-dimensional array.

```
int[,] matrix =
{
    { 1, 2, 3 },
    { 4, 5, 6 },
    { 7, 8, 9 }
};
```

Notice the syntax:

```
int[,]
```

The comma represents two dimensions.

Access an element:

```
Console.WriteLine(matrix[0, 0]); // 1

Console.WriteLine(matrix[1, 2]); // 6

Console.WriteLine(matrix[2, 1]); // 8
```

### Traversing a multidimensional array

```
for (int row = 0; row < 3; row++)
{
    for (int col = 0; col < 3; col++)
    {
        Console.Write(matrix[row, col] + " ");
    }

    Console.WriteLine();
}
```

Output:

```
1 2 3
4 5 6
7 8 9
```

Instead of hardcoding dimensions, use:

```
int rows = matrix.GetLength(0);

int columns = matrix.GetLength(1);
```

Why not use `matrix.Length`?

Because `Length` returns the total number of elements.

For our matrix:

```
3 × 3 = 9
```

### General rule

| Property               | Purpose                              |
| ---------------------- | ------------------------------------ |
| `Length`               | Total number of elements             |
| `Rank`                 | Number of dimensions                 |
| `GetLength(dimension)` | Number of elements along a dimension |

For example:

```
Console.WriteLine(matrix.Length);       // 9

Console.WriteLine(matrix.Rank);         // 2

Console.WriteLine(matrix.GetLength(0)); // 3

Console.WriteLine(matrix.GetLength(1)); // 3
```

# 6.18 Jagged arrays

Now let's understand another type of array.

A jagged array is an array whose elements are themselves arrays.

Consider:

```
Row 1: 1 2
Row 2: 3 4 5
Row 3: 6
```

Each row has a different number of elements.

A rectangular multidimensional array isn't suitable for this structure.

Instead, use a jagged array.

```
int[][] numbers =
{
    new int[] { 1, 2 },
    new int[] { 3, 4, 5 },
    new int[] { 6 }
};
```

Access elements:

```
Console.WriteLine(numbers[0][1]); // 2

Console.WriteLine(numbers[1][2]); // 5

Console.WriteLine(numbers[2][0]); // 6
```

Notice the difference:

Multidimensional:

```
matrix[row, column]
```

Jagged:

```
numbers[row][column]
```

### Understanding the internal structure

A jagged array is an array containing references to other arrays.

```
numbers
   |
   v
+----------+----------+----------+
| Ref      | Ref      | Ref      |
+----------+----------+----------+
    |          |          |
    v          v          v

+-----+    +-----+-----+    +-----+
|  1  |    |  3  |  4  |    |  6  |
|  2  |    |  5  |     |    +-----+
+-----+    +-----+-----+
```

This is different from a rectangular multidimensional array.

### Multidimensional vs jagged

| Feature                 | Multidimensional              | Jagged                                      |
| ----------------------- | ----------------------------- | ------------------------------------------- |
| Syntax                  | `int[,]`                      | `int[][]`                                   |
| Structure               | Rectangular dimensions        | Array of arrays                             |
| Different row lengths   | No                            | Yes                                         |
| Access                  | `a[row, col]`                 | `a[row][col]`                               |
| Internal representation | Multidimensional array object | Outer array with references to inner arrays |

### When should we use them?

Multidimensional arrays:

- Matrices.
- Fixed-size grids.
- Tabular numeric data.

Jagged arrays:

- Unequal row lengths.
- Graph adjacency lists represented as arrays of neighbors.
- Data where each group has a different number of elements.

# 6.19 Common array operations

C# provides the `Array` class with many useful methods.

Let's explore some.

Assume:

```
int[] numbers = { 5, 2, 8, 1, 9, 3 };
```

### A. Sort

```
Array.Sort(numbers);
```

Now:

```
1, 2, 3, 5, 8, 9
```

Important: `Array.Sort()` modifies the original array.

### B. Reverse

```
Array.Reverse(numbers);
```

Reverses the element order in place.

### C. IndexOf

```
int index = Array.IndexOf(numbers, 8);
```

Returns the index of the first matching element, or `-1` if not found.

### D. Find

Finds the first element matching a condition.

```
int[] numbers = { 5, 2, 8, 1, 9, 3 };

int result = Array.Find(numbers, n => n > 6);

Console.WriteLine(result);
```

Output:

```
8
```

What is this?

```
n => n > 6
```

It's a lambda expression.

It represents a function that accepts a number and returns whether it is greater than six.

We'll study lambdas and delegates in detail later.

### E. FindAll

Returns all matching elements in a new array.

```
int[] result = Array.FindAll(numbers, n => n > 3);
```

Result:

```
5, 8, 9
```

### F. Exists

Checks whether at least one matching element exists.

```
bool exists = Array.Exists(numbers, n => n > 8);
```

Result:

```
true
```

### G. TrueForAll

Checks whether every element satisfies a condition.

```
bool allPositive = Array.TrueForAll(
    numbers,
    n => n > 0
);
```

### H. Copy

We've already seen:

```
int[] destination = new int[numbers.Length];

Array.Copy(numbers, destination, numbers.Length);
```

Copies elements into another array.

### I. Resize

What happens if you need to increase an array's size?

Arrays themselves have fixed sizes.

C# provides:

```
Array.Resize(ref numbers, 10);
```

This may look like the array is being resized.

But internally, a new array is created and the existing elements are copied into it.

The original array is not physically expanded in place.

This method uses `ref` because the caller's array reference needs to be updated to refer to the newly created array.

Example:

```
int[] numbers = { 10, 20, 30 };

Array.Resize(ref numbers, 5);
```

Now:

```
10, 20, 30, 0, 0
```

The additional elements contain the default value.

Similarly, you can shrink:

```
Array.Resize(ref numbers, 2);
```

Now:

```
10, 20
```

Elements beyond the new length are discarded from the new array.

Important: Frequent resizing is inefficient. For dynamically growing collections, use `List<T>` instead.

### J. BinarySearch

```
int[] numbers = { 1, 2, 3, 5, 8, 9 };

int index = Array.BinarySearch(numbers, 8);
```

Returns the index of the matching element.

Important:

The array must already be sorted according to the same ordering used by the search.

Otherwise, the result isn't reliable.

We'll study binary search algorithms separately when appropriate.

### Array API summary

| Method                 | Purpose                                          |
| ---------------------- | ------------------------------------------------ |
| `Array.Sort()`         | Sort elements                                    |
| `Array.Reverse()`      | Reverse element order                            |
| `Array.IndexOf()`      | Find an element's index                          |
| `Array.Find()`         | Find first matching element                      |
| `Array.FindAll()`      | Find all matching elements                       |
| `Array.Exists()`       | Check whether an element matches                 |
| `Array.TrueForAll()`   | Check whether every element matches              |
| `Array.Copy()`         | Copy elements                                    |
| `Array.Resize()`       | Create a resized array and replace the reference |
| `Array.BinarySearch()` | Search a sorted array                            |

# 6.20 Arrays vs Lists

This is an important distinction for backend development.

Suppose you are building a user service.

You want to store multiple users.

Should you use an array or a list?

Let's understand the difference.

### Array

```
User[] users = new User[3];
```

The length is fixed.

You can replace existing elements:

```
users[0] = new User();
```

But you cannot append a fourth element without creating a new array.

### List

```
List<User> users = new List<User>();
```

You can dynamically add elements:

```
users.Add(new User());

users.Add(new User());

users.Add(new User());
```

You can also remove:

```
users.RemoveAt(0);
```

We'll explore Lists and other collections in the next chapter.

### Comparison

\| Feature | Array | List

