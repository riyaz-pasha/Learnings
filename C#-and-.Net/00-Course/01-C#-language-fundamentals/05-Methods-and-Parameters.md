# Chapter 5 — Methods and Parameters in C\#

In this chapter, we'll understand how C# methods work and explore some features that are particularly interesting compared to Java and JavaScript.

Most of the basic syntax will feel familiar. However, C# has some useful features such as `ref`, `out`, `in`, optional parameters, named arguments, and extension methods.

We'll pay particular attention to how arguments are passed to methods, because this is one of the most commonly misunderstood areas in C#.

We'll cover:

1. Method declarations and return types.
2. Static vs instance methods.
3. Method overloading.
4. Optional parameters.
5. Named arguments.
6. Pass by value vs pass by reference.
7. `ref`, `out`, and `in`.
8. `params` parameters.
9. Expression-bodied methods.
10. Local functions.
11. Extension methods.
12. Method overloading and overload resolution.

# 5.1 What is a method?

A method is a block of code that performs a particular operation.

You've already worked with methods in Java:

```
public int add(int a, int b) {
    return a + b;
}
```

JavaScript:

```
function add(a, b) {
    return a + b;
}
```

C#:

```
public int Add(int a, int b)
{
    return a + b;
}
```

The syntax is very similar to Java.

Let's understand the structure:

```
public int Add(int a, int b)
{
    return a + b;
}
```

| Part     | Meaning                       |
| -------- | ----------------------------- |
| `public` | Access modifier               |
| `int`    | Return type                   |
| `Add`    | Method name                   |
| `int a`  | First parameter               |
| `int b`  | Second parameter              |
| `return` | Returns a value to the caller |

General syntax:

```
accessModifier returnType MethodName(parameters)
{
    // Method body

    return value;
}
```

For example:

```
public string GetUserName()
{
    return "John";
}
```

A method that doesn't return a value uses `void`:

```
public void PrintMessage()
{
    Console.WriteLine("Hello");
}
```

Compare with Java:

```
public void printMessage() {
    System.out.println("Hello");
}
```

### Important: C# naming conventions

In C#, method names generally use PascalCase.

```
CalculateTotal()
GetUser()
ProcessPayment()
SendNotification()
```

Java commonly uses camelCase:

```
calculateTotal()
getUser()
processPayment()
```

You'll frequently encounter PascalCase for:

- Methods.
- Classes.
- Properties.
- Public members.

Local variables and parameters generally use camelCase:

```
public decimal CalculateTotal(decimal unitPrice, int quantity)
{
    return unitPrice * quantity;
}
```

This is the standard C# naming convention.

# 5.2 Understanding static vs instance methods

This should be familiar from Java.

Consider:

```
public class Calculator
{
    public int Add(int a, int b)
    {
        return a + b;
    }

    public static int Multiply(int a, int b)
    {
        return a * b;
    }
}
```

We have two methods:

- `Add()` is an instance method.
- `Multiply()` is a static method.

### Instance method

To call an instance method, you need an object.

```
var calculator = new Calculator();

int result = calculator.Add(10, 20);
```

Why?

Because instance methods operate in the context of a particular object and can access its instance members.

### Static method

A static method belongs to the type itself rather than a particular object instance.

```
int result = Calculator.Multiply(10, 20);
```

You don't need to create an object.

### Java comparison

Java:

```
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }
}
```

C# behaves similarly.

### When should we use static methods?

Suppose we have a utility operation:

```
public static class MathHelper
{
    public static int Square(int number)
    {
        return number * number;
    }
}
```

Usage:

```
int result = MathHelper.Square(5);
```

This is reasonable because calculating a square doesn't require any particular object's state.

Some common examples:

- Utility methods.
- Pure calculation methods.
- Conversion methods.
- Helper operations that don't depend on instance state.

On the other hand:

```
public class BankAccount
{
    public decimal Balance { get; set; }

    public void Deposit(decimal amount)
    {
        Balance += amount;
    }
}
```

`Deposit()` is naturally an instance method because it modifies a particular bank account.

Practical takeaway: Use instance methods when behavior belongs to an object's state. Use static methods when the operation doesn't need a particular instance.

# 5.3 Method overloading

Method overloading allows us to define multiple methods with the same name but different parameter signatures.

This exists in Java too.

Suppose we have a calculator.

We want to support addition of two integers and three integers.

```
public class Calculator
{
    public int Add(int a, int b)
    {
        return a + b;
    }

    public int Add(int a, int b, int c)
    {
        return a + b + c;
    }

    public double Add(double a, double b)
    {
        return a + b;
    }
}
```

Usage:

```
var calculator = new Calculator();

Console.WriteLine(calculator.Add(10, 20));

Console.WriteLine(calculator.Add(10, 20, 30));

Console.WriteLine(calculator.Add(10.5, 20.5));
```

Output:

```
30
60
31
```

The compiler determines which overload to call based on the arguments.

This is called compile-time overload resolution.

### What makes two methods different?

Methods can be overloaded using different:

1. Number of parameters.
2. Parameter types.
3. Parameter order.

For example:

```
public void Print(int number)
{
}

public void Print(string text)
{
}

public void Print(int number, string text)
{
}

public void Print(string text, int number)
{
}
```

All of these are valid overloads.

### Can we overload based only on return type?

No.

This is invalid:

```
public int Calculate(int number)
{
    return number * 2;
}

public string Calculate(int number)
{
    return number.ToString();
}
```

Why?

When calling:

```
Calculate(10);
```

The compiler has no way to determine which method you intend to call based only on the expected return type.

So return type alone cannot distinguish overloads.

This is also true in Java.

### Can we overload based on `ref`, `out`, and `in`?

There are specific rules here.

C# permits certain overloads involving `ref`, `in`, and `out`, but not simply every combination. In particular, a method cannot be overloaded by changing only between `ref` and `out`.

We'll understand these parameter modifiers shortly.

# 5.4 Optional parameters

This is a very useful feature.

Suppose we have a method:

```
public void SendEmail(string email, string subject)
{
    Console.WriteLine($"Sending to {email}: {subject}");
}
```

Every time we call it, we must provide both arguments:

```
SendEmail("john@example.com", "Welcome");
```

But sometimes we want to use a default subject.

C# allows optional parameters.

```
public void SendEmail(
    string email,
    string subject = "Welcome"
)
{
    Console.WriteLine($"Sending to {email}: {subject}");
}
```

Now both calls are valid:

```
SendEmail("john@example.com");

SendEmail("john@example.com", "Account Created");
```

Output:

```
Sending to john@example.com: Welcome
Sending to john@example.com: Account Created
```

### How does it work?

The compiler allows you to omit an optional argument and uses its declared default value.

Conceptually:

```
SendEmail("john@example.com");
```

behaves like:

```
SendEmail("john@example.com", "Welcome");
```

The method still receives both parameters.

### Important rules

Optional parameters must appear after required parameters in the method signature.

Valid:

```
public void CreateUser(
    string name,
    int age = 18,
    string country = "India"
)
{
}
```

Invalid:

```
public void CreateUser(
    int age = 18,
    string name
)
{
}
```

A required parameter cannot follow an optional parameter in this declaration.

### What can be used as a default value?

Common examples:

```
public void Process(
    int retries = 3,
    bool enabled = true,
    string name = "Unknown"
)
{
}
```

Default values must satisfy C#'s optional-parameter constant-value rules. You generally cannot use an arbitrary object creation or a method call as the default.

For example, this isn't valid:

```
public void Process(DateTime date = DateTime.Now)
{
}
```

For runtime defaults, handle the value inside the method or use a nullable parameter.

### Optional parameters in Java

Java doesn't have the same direct optional-parameter feature.

You might use method overloading:

```
public void sendEmail(String email) {
    sendEmail(email, "Welcome");
}

public void sendEmail(String email, String subject) {
    // Implementation
}
```

C# allows this more directly.

### Important real-world consideration

Optional parameter values are generally embedded into the calling assembly at compile time.

Suppose a library defines:

```
public void Process(int retries = 3)
{
}
```

Another assembly calls:

```
Process();
```

If the library later changes the default from `3` to `5`, an already compiled caller may continue passing `3` until it is recompiled.

This matters when designing public library APIs.

For highly versioned APIs, explicit overloads or options objects may sometimes be better.

# 5.5 Named arguments

This is another useful C# feature.

Consider:

```
public void CreateUser(
    string name,
    int age,
    string country,
    bool isActive
)
{
}
```

Without named arguments:

```
CreateUser("John", 25, "India", true);
```

This works, but imagine a method with many parameters.

It can become difficult to understand which argument represents what.

C# allows named arguments.

```
CreateUser(
    name: "John",
    age: 25,
    country: "India",
    isActive: true
);
```

Much clearer.

You can also provide them in a different order:

```
CreateUser(
    country: "India",
    isActive: true,
    name: "John",
    age: 25
);
```

This is valid because the arguments are explicitly named.

### Named arguments with optional parameters

This is where named arguments become particularly useful.

Consider:

```
public void SendNotification(
    string userId,
    string message,
    bool sendEmail = true,
    bool sendSms = false,
    bool sendPush = true
)
{
}
```

Suppose you want to disable SMS only.

Instead of passing every argument:

```
SendNotification(
    "user-1",
    "Your order is shipped",
    true,
    false,
    true
);
```

You can write:

```
SendNotification(
    userId: "user-1",
    message: "Your order is shipped",
    sendSms: false
);
```

Much cleaner.

Java doesn't provide the same built-in named-argument syntax.

You can achieve similar clarity using builder patterns or dedicated configuration objects.

Practical recommendation: Use named arguments when methods have several parameters, especially optional boolean parameters. It makes call sites much easier to understand.

# 5.6 The most important topic: Pass by value vs pass by reference

Now let's understand the actual behavior of method arguments.

This is where many developers coming from Java or JavaScript get confused.

There are two concepts we need to distinguish:

1. Passing a value.
2. Passing a variable by reference.

These are not the same.

## A. Passing a value type by value

Consider:

```
public void UpdateAge(int age)
{
    age = 30;
}
```

Call it:

```
int userAge = 25;

UpdateAge(userAge);

Console.WriteLine(userAge);
```

Output:

```
25
```

Why?

Because `userAge` is a value type.

When we call:

```
UpdateAge(userAge);
```

The value `25` is copied into the method parameter `age`.

Conceptually:

```
Caller                  Method

userAge = 25

                        age = 25
```

Now:

```
age = 30;
```

Only the method's local parameter changes.

The caller's variable remains unchanged.

This is pass by value.

## B. Passing a reference type by value

Now here's where it becomes interesting.

Consider:

```
public class User
{
    public string Name { get; set; } = string.Empty;
}
```

Method:

```
public void UpdateName(User user)
{
    user.Name = "Smith";
}
```

Call:

```
User user = new User
{
    Name = "John"
};

UpdateName(user);

Console.WriteLine(user.Name);
```

Output:

```
Smith
```

Why did the caller's object change?

Remember what we learned in Chapter 3.

A class variable holds a reference to an object.

When we call:

```
UpdateName(user);
```

The reference stored in the variable `user` is copied into the method parameter.

Both references point to the same object.

Visualize:

```
Caller                         Method

user -----------+               user -----------+
                |                              |
                v                              v
             +-------------------------------+
             |           User Object         |
             |-------------------------------|
             | Name = "John"                 |
             +-------------------------------+
```

Inside the method:

```
user.Name = "Smith";
```

We're modifying the shared object.

Therefore, the caller sees the modification.

But this is still pass by value!

The copied value is a reference.

The reference itself is passed by value.

This is exactly how Java behaves with object arguments.

### Now the important experiment

What happens if we reassign the parameter?

```
public void UpdateName(User user)
{
    user = new User
    {
        Name = "Smith"
    };
}
```

Call:

```
User user = new User
{
    Name = "John"
};

UpdateName(user);

Console.WriteLine(user.Name);
```

Output:

```
John
```

Why?

Let's visualize the execution.

Initially:

```
Caller

user ---------> User Object A
                Name = "John"
```

When passing the argument:

```
Caller                         Method

user ---------> Object A       user ---------> Object A
```

Now execute:

```
user = new User { Name = "Smith" };
```

The method's local parameter now points to a different object.

```
Caller                         Method

user ---------> Object A       user ---------> Object B
                Name = "John"                  Name = "Smith"
```

The caller still refers to Object A.

The method's parameter refers to Object B.

When the method finishes, the caller's variable remains unchanged.

This is a fundamental distinction:

Passing a reference-type variable by value allows you to modify the referenced object, but it doesn't allow you to replace the caller's variable.

This is exactly the same argument-passing behavior as Java.

# 5.7 What is `ref`?

Now we reach an important C# feature.

Sometimes we actually want a method to modify the caller's variable itself.

For this, C# provides `ref`.

Consider:

```
public void UpdateAge(ref int age)
{
    age = 30;
}
```

Call:

```
int userAge = 25;

UpdateAge(ref userAge);

Console.WriteLine(userAge);
```

Output:

```
30
```

Why?

Because `ref` means the method receives the variable by reference rather than receiving an independent copy of its value.

Visualize:

```
Caller                         Method

userAge ----------------------> age
    \                            /
     \                          /
      +------ Same variable ----+
```

Now changes inside the method affect the caller's variable.

### Important syntax

The `ref` keyword must be present in both places:

Method declaration:

```
public void UpdateAge(ref int age)
```

Method invocation:

```
UpdateAge(ref userAge);
```

### Why do we need ref?

Let's consider a simple swap operation.

Without `ref`:

```
public void Swap(int a, int b)
{
    int temp = a;

    a = b;

    b = temp;
}
```

This won't swap the caller's variables.

Because `a` and `b` are copies.

With `ref`:

```
public void Swap(ref int a, ref int b)
{
    int temp = a;

    a = b;

    b = temp;
}
```

Call:

```
int x = 10;
int y = 20;

Swap(ref x, ref y);

Console.WriteLine(x);
Console.WriteLine(y);
```

Output:

```
20
10
```

The method can now modify the original variables.

## `ref` with reference types

Here's something even more interesting.

We can use `ref` with reference types too.

Consider:

```
public void ReplaceUser(ref User user)
{
    user = new User
    {
        Name = "Smith"
    };
}
```

Call:

```
User user = new User
{
    Name = "John"
};

ReplaceUser(ref user);

Console.WriteLine(user.Name);
```

Output:

```
Smith
```

Why?

Without `ref`, the method receives a copy of the object reference.

With `ref`, the method receives access to the caller's reference variable itself.

Therefore, it can replace the reference stored in the caller's variable.

This distinction is extremely important.

| Operation inside method                    | Normal parameter          | `ref` parameter          |
| ------------------------------------------ | ------------------------- | ------------------------ |
| Modify an object's property                | Caller sees modification  | Caller sees modification |
| Reassign parameter to another object       | Caller variable unchanged | Caller variable updated  |
| Assign a new value to an integer parameter | Caller variable unchanged | Caller variable updated  |

### When should we use ref?

Common use cases include:

- Modifying a caller's variable.
- High-performance code involving large structs.
- Certain APIs that need to update an existing value.

Don't use `ref` everywhere simply because it is possible.

Ordinary return values and well-designed result objects are usually preferable for most business logic.

# 5.8 What is `out`?

`out` is another way to pass a variable by reference.

But it serves a slightly different purpose.

With `ref`, the caller must initialize the variable before passing it.

With `out`, the caller doesn't need to initialize the variable first.

The method is responsible for assigning a value before returning normally.

Let's understand through an example.

Suppose we need a method that tries to parse a string into an integer.

JavaScript:

```
function parseNumber(value) {
    const parsed = Number(value);

    return Number.isNaN(parsed) ? null : parsed;
}
```

C# provides a standard parsing API:

```
int number;

bool success = int.TryParse("123", out number);
```

Now:

```
Console.WriteLine(success); // True
Console.WriteLine(number);  // 123
```

What's happening?

`TryParse` needs to communicate two things:

1. Did parsing succeed?
2. What is the parsed integer?

It returns the success status as a `bool`.

It writes the parsed number into the `out` parameter.

### Understanding the declaration

```
public bool TryParse(
    string input,
    out int result
)
```

Inside the method, the implementation must assign `result` before every normal return.

For example:

```
public bool TryParseNumber(
    string input,
    out int result
)
{
    if (int.TryParse(input, out result))
    {
        return true;
    }

    return false;
}
```

Call:

```
bool success = TryParseNumber("123", out int number);
```

Notice something interesting:

We can declare the output variable directly at the invocation site.

This is called an inline `out` variable declaration.

### Another example

```
public void GetUserDetails(
    long userId,
    out string name,
    out int age
)
{
    name = "John";
    age = 25;
}
```

Usage:

```
GetUserDetails(1, out string name, out int age);

Console.WriteLine(name);
Console.WriteLine(age);
```

### ref vs out

| Feature                                        | `ref`                       | `out`                    |
| ---------------------------------------------- | --------------------------- | ------------------------ |
| Caller must initialize variable before calling | Yes                         | No                       |
| Method can read initial value                  | Yes                         | Not before assigning     |
| Method must assign before normal return        | No                          | Yes                      |
| Caller gets modifications                      | Yes                         | Yes                      |
| Used for                                       | Input + output by reference | Output through parameter |

### Why does C# have out when we can simply return an object?

Good question.

Consider:

```
public (bool Success, int Value) ParseNumber(string input)
{
    bool success = int.TryParse(input, out int result);

    return (success, result);
}
```

This is another possible design using a tuple.

C# also has records and result types.

So `out` isn't always necessary in modern application design.

However, you'll encounter it frequently in .NET APIs, especially methods following the `Try...` naming convention.

Examples:

```
int.TryParse(...)
Dictionary<TKey, TValue>.TryGetValue(...)
Enum.TryParse(...)
```

Practical rule: Recognize `out` because it's common in .NET libraries. Use it when appropriate, but don't force it into every method that needs to return multiple values.

# 5.9 What is `in`?

Now let's discuss the third parameter modifier.

`in` passes an argument by reference, but the method is not allowed to modify the referenced variable through that parameter.

Consider:

```
public void PrintNumber(in int number)
{
    Console.WriteLine(number);

    // number = 20; // Compilation error
}
```

Call:

```
int value = 10;

PrintNumber(in value);
```

The method receives a readonly reference to the argument.

### Why do we need it?

Imagine a large struct:

```
public struct LargeData
{
    public long A;
    public long B;
    public long C;
    public long D;
}
```

Passing it normally:

```
public void Process(LargeData data)
{
}
```

The value is copied.

For large structs, copying can be expensive.

We could use:

```
public void Process(in LargeData data)
{
}
```

Now the method receives a readonly reference instead of an ordinary by-value copy.

This can reduce copying.

### ref vs in

| Feature                             | `ref`                | `in`                |
| ----------------------------------- | -------------------- | ------------------- |
| Passed by reference                 | Yes                  | Yes                 |
| Method can modify through parameter | Yes                  | No                  |
| Caller must initialize variable     | Yes                  | Yes                 |
| Intended purpose                    | Read/write reference | Read-only reference |

One important nuance: `in` prevents modification through that parameter. If the referenced value is itself a reference type, the method can still modify the referenced object's mutable properties. `in` protects the parameter variable from reassignment; it doesn't make the entire object graph immutable.

Also, C# allows some implicit readonly-by-reference argument passing without writing `in` at the call site, depending on the method and argument. For clarity, it's useful to recognize the explicit form.

### When should we use `in`?

Primarily when working with potentially expensive value-type copies, particularly large readonly data structures.

For ordinary backend business logic, you won't need it very frequently.

# 5.10 Complete comparison of parameter modifiers

Let's put everything together.

| Parameter        | Passing behavior      | Caller initialization           | Method can change caller's variable? |
| ---------------- | --------------------- | ------------------------------- | ------------------------------------ |
| Normal parameter | By value              | Required value must be supplied | No                                   |
| `ref`            | By reference          | Required                        | Yes                                  |
| `out`            | By reference          | Not required                    | Yes                                  |
| `in`             | Readonly by reference | Required                        | No, not through the parameter        |

Let's use one example for each.

```
public void Normal(int value)
{
    value = 100;
}

public void Update(ref int value)
{
    value = 100;
}

public void Create(out int value)
{
    value = 100;
}

public void Read(in int value)
{
    Console.WriteLine(value);
}
```

Usage:

```
int a = 10;

Normal(a);
// a is still 10

Update(ref a);
// a is now 100

Create(out int b);
// b is now 100

Read(in a);
// Reads a without modifying it
```

### One very important Java comparison

Java doesn't have direct equivalents of C#'s `ref`, `out`, and `in` parameter semantics.

Java passes all method arguments by value.

For primitive values, it copies the primitive value.

For objects, it copies the reference.

That's exactly how normal C# parameter passing works too.

The difference is that C# gives us additional explicit reference-passing mechanisms.

Keep this mental model:

Normal C# parameters behave like Java parameters. `ref`, `out`, and `in` introduce additional capabilities.

# 5.11 The `params` keyword

Now let's discuss another useful feature.

Suppose you want to implement a method that adds multiple numbers.

Without `params`:

```
public int Add(int[] numbers)
{
    int total = 0;

    foreach (int number in numbers)
    {
        total += number;
    }

    return total;
}
```

Usage:

```
int result = Add(new int[] { 10, 20, 30 });
```

This works.

But C# provides `params`.

```
public int Add(params int[] numbers)
{
    int total = 0;

    foreach (int number in numbers)
    {
        total += number;
    }

    return total;
}
```

Now we can write:

```
int result1 = Add(10, 20);

int result2 = Add(10, 20, 30);

int result3 = Add(10, 20, 30, 40, 50);
```

We don't need to explicitly construct the array at the call site.

### How does it work?

The compiler packages the supplied arguments into an array.

Conceptually:

```
Add(10, 20, 30);
```

behaves like:

```
Add(new int[] { 10, 20, 30 });
```

The method receives an array.

### Java comparison

Java has varargs:

```
public int add(int... numbers) {
    int total = 0;

    for (int number : numbers) {
        total += number;
    }

    return total;
}
```

C# `params` serves a similar purpose.

### JavaScript comparison

JavaScript rest parameters:

```
function add(...numbers) {
    return numbers.reduce((sum, number) => sum + number, 0);
}
```

Again, similar behavior.

### Important rule

Traditionally, `params` must be the last parameter.

```
public void Log(string message, params object[] values)
{
}
```

Usage:

```
Log("User details", "John", 25, true);
```

For C# 13 and newer, `params` can also target certain collection types, not just arrays. You'll mostly encounter the array form in existing codebases.

Practical takeaway: Use `params` when the number of arguments is variable and the method naturally accepts a collection of values.

# 5.12 Expression-bodied methods

Now let's look at a small but very common C# feature.

Consider:

```
public int Add(int a, int b)
{
    return a + b;
}
```

For simple methods that contain only one expression, C# provides expression-bodied syntax.

```
public int Add(int a, int b) => a + b;
```

The `=>` operator here means that the method body is the expression to the right.

This is similar to concise arrow-function syntax in JavaScript, but we're still declaring a C# method.

Another example:

```
public string GetFullName(string firstName, string lastName)
    => $"{firstName} {lastName}";
```

You can also use expression-bodied syntax for methods returning `void`:

```
public void PrintMessage(string message)
    => Console.WriteLine(message);
```

### Properties also support expression-bodied syntax

```
public class User
{
    public string FirstName { get; set; }

    public string LastName { get; set; }

    public string FullName => $"{FirstName} {LastName}";
}
```

`FullName` is a computed property.

It evaluates the expression when accessed.

This is a feature you'll encounter frequently in modern C# code.

Practical recommendation: Use expression-bodied members for simple operations. Use a normal method body when there is meaningful branching or multiple statements.

# 5.13 Local functions

C# allows us to define a method inside another method.

These are called local functions.

Example:

```
public void ProcessOrder()
{
    Console.WriteLine("Processing order");

    ValidateOrder();

    void ValidateOrder()
    {
        Console.WriteLine("Validating order");
    }
}
```

`ValidateOrder()` is defined inside `ProcessOrder()`.

It can only be accessed within the containing method.

### Why do we need local functions?

Imagine a method containing a small operation used only within that method.

Without local functions:

```
public class OrderService
{
    public void ProcessOrder()
    {
        ValidateOrder();
    }

    private void ValidateOrder()
    {
        // Validation logic
    }
}
```

This adds another class-level method.

But perhaps the validation helper is only relevant to `ProcessOrder()`.

We can keep it local:

```
public class OrderService
{
    public void ProcessOrder()
    {
        ValidateOrder();

        void ValidateOrder()
        {
            // Validation logic
        }
    }
}
```

### Local functions can access outer variables

Consider:

```
public int CalculateTotal()
{
    int total = 100;

    return AddTax();

    int AddTax()
    {
        return total + 10;
    }
}
```

The local function can access `total` from the containing scope.

This is related to closures and captured variables.

You'll encounter similar behavior in JavaScript closures.

One difference from JavaScript is that C# local functions can also have their own parameter modifiers and generic parameters, and they can be declared `static` to prevent capturing variables from their containing scope.

For example:

```
public int Calculate(int number)
{
    return Double(number);

    static int Double(int value)
    {
        return value * 2;
    }
}
```

Because the local function is `static`, it cannot capture local variables from the containing method.

### When should we use local functions?

Useful for:

- Small helper operations.
- Complex validation logic used by a single method.
- Recursive helper functions.
- Keeping implementation details local.

Don't use them to hide large pieces of application logic.

# 5.14 Extension methods — a very useful C# feature

This is one of my favorite C# features.

Let's understand why it exists.

Suppose you have a class:

```
public class User
{
    public string Name { get; set; } = string.Empty;
}
```

You want to add a method:

```
user.GetDisplayName();
```

But the `User` class belongs to an external library, and you cannot modify it.

In Java, you'd generally create a utility class:

```
public class UserUtils {

    public static String getDisplayName(User user) {
        return user.getName().toUpperCase();
    }
}
```

Usage:

```
UserUtils.getDisplayName(user);
```

JavaScript gives you more flexibility in extending objects, though modifying prototypes is generally not recommended for application code.

C# provides something special: extension methods.

They allow us to call a static method as though it were an instance method of an existing type.

### Let's implement one.

Create a static class:

```
public static class UserExtensions
{
    public static string GetDisplayName(this User user)
    {
        return user.Name.ToUpper();
    }
}
```

Notice this parameter:

```
this User user
```

The `this` keyword indicates that the first parameter is the type being extended.

Now usage:

```
User user = new User
{
    Name = "John"
};

string displayName = user.GetDisplayName();

Console.WriteLine(displayName);
```

Output:

```
JOHN
```

But wait!

We didn't modify the original `User` class.

How can we call:

```
user.GetDisplayName();
```

The compiler translates this into a call to the static extension method.

Conceptually:

```
UserExtensions.GetDisplayName(user);
```

This is a compile-time feature.

It doesn't actually inject a new method into the original class.

### Rules for extension methods

An extension method must:

- Be declared in a static class.
- Be a static method.
- Have the `this` modifier on its first parameter.
- Be available through the relevant namespace.

### Another example

Let's extend `string`.

```
public static class StringExtensions
{
    public static bool IsValidEmail(this string value)
    {
        return value.Contains('@');
    }
}
```

Usage:

```
string email = "john@example.com";

bool isValid = email.IsValidEmail();
```

This example is intentionally simple; real email validation requires more than checking for `@`.

### Can extension methods take additional parameters?

Absolutely.

```
public static class StringExtensions
{
    public static string Repeat(
        this string value,
        int count
    )
    {
        return string.Concat(
            Enumerable.Repeat(value, count)
        );
    }
}
```

Usage:

```
string result = "Hi ".Repeat(3);

Console.WriteLine(result);
```

Output:

```
Hi Hi Hi
```

### Extension methods and namespaces

Suppose the extension method is defined under:

```
namespace LearningDotNet.Extensions;
```

You need:

```
using LearningDotNet.Extensions;
```

Otherwise, that extension method might not be available for invocation through extension-method syntax.

### Important: Instance methods take precedence

Suppose a class already has an instance method with the same name and a matching call signature.

The real instance method takes precedence over an extension method.

Extension methods don't override or replace existing instance methods.

### Where will you encounter extension methods?

Everywhere in modern .NET development.

For example:

LINQ

```
var activeUsers = users.Where(user => user.IsActive);
```

`Where()` is an extension method on `IEnumerable<T>` and `IQueryable<T>`.

ASP.NET Core

You might encounter:

```
builder.Services.AddControllers();
```

Dependency Injection

```
services.AddScoped<IUserService, UserService>();
```

Many APIs in .NET use extension methods to provide additional capabilities without modifying the original types.

This makes extension methods particularly important.

# 5.15 Method overloading and overload resolution

Let's understand one more thing about method calls.

Consider:

```
public void Print(int value)
{
    Console.WriteLine("Integer");
}

public void Print(double value)
{
    Console.WriteLine("Double");
}

public void Print(string value)
{
    Console.WriteLine("String");
}
```

Now:

```
Print(10);

Print(10.5);

Print("Hello");
```

Output:

```
Integer
Double
String
```

The compiler selects the best matching overload.

Let's try something else.

```
Print(10L);
```

What happens?

`10L` is a `long`.

There is no `Print(long)` overload.

But C# can implicitly convert a `long` to a `double`.

So the `Print(double)` overload can be selected.

This is called overload resolution with implicit conversions.

### Ambiguous overloads

Consider:

```
public void Print(long value)
{
}

public void Print(double value)
{
}
```

Now:

```
short number = 10;

Print(number);
```

Both `short` to `long` and `short` to `double` are implicit numeric conversions.

However, the compiler generally prefers the better conversion, which here is `short` to `long`.

Not every overload combination is this simple.

Consider:

```
public void Print(int value)
{
}

public void Print(string value)
{
}
```

Now:

```
Print(null);
```

This resolves to the string overload because `null` cannot be converted to `int`.

More complex combinations can become ambiguous, in which case the compiler produces an error.

You don't need to memorize every overload-resolution rule right now.

Just remember:

Overloading is resolved by the compiler based on argument types, conversions, and applicable method signatures.

# 5.16 Putting everything together — A practical example

Let's build a small `PaymentService`.

```
public class PaymentService
{
    // Basic method
    public decimal CalculateTotal(
        decimal amount,
        decimal taxRate
    )
    {
        return amount + (amount * taxRate);
    }

    // Optional parameter
    public void ProcessPayment(
        decimal amount,
        string currency = "INR"
    )
    {
        Console.WriteLine(
            $"Processing {amount} {currency}"
        );
    }

    // params
    public decimal CalculateTotal(params decimal[] amounts)
    {
        decimal total = 0;

        foreach (decimal amount in amounts)
        {
            total += amount;
        }

        return total;
    }

    // out
    public bool TryGetPaymentStatus(
        long paymentId,
        out string status
    )
    {
        if (paymentId > 0)
        {
            status = "Completed";

            return true;
        }

        status = "Not Found";

        return false;
    }

    // ref
    public void ApplyDiscount(
        ref decimal amount,
        decimal discount
    )
    {
        amount -= discount;
    }

    // Expression-bodied method
    public bool IsValidAmount(decimal amount)
        => amount > 0;
}
```

Usage:

```
var paymentService = new PaymentService();

// Normal method
decimal total = paymentService.CalculateTotal(1000m, 0.18m);

// Optional parameter
paymentService.ProcessPayment(1000m);

// Named argument
paymentService.ProcessPayment(
    amount: 2000m,
    currency: "USD"
);

// params
decimal grandTotal = paymentService.CalculateTotal(
    100m,
    200m,
    300m
);

// out
bool found = paymentService.TryGetPaymentStatus(
    1,
    out string status
);

// ref
decimal amount = 1000m;

paymentService.ApplyDiscount(ref amount, 100m);

// Expression-bodied method
bool valid = paymentService.IsValidAmount(amount);
```

This example combines most of the important concepts from this chapter.

# 5.17 Summary — What should you remember?

| Concept                  | Mental model                                             |
| ------------------------ | -------------------------------------------------------- |
| Method                   | Reusable block of functionality                          |
| Static method            | Operation associated with a type                         |
| Instance method          | Operation associated with an object                      |
| Overloading              | Same method name, different parameter signatures         |
| Optional parameter       | Caller can omit an argument                              |
| Named argument           | Pass an argument using its parameter name                |
| Normal parameter         | Passes the value, including a copied object reference    |
| `ref`                    | Allows the method to modify the caller's variable        |
| `out`                    | Method provides a value through a by-reference parameter |
| `in`                     | Readonly by-reference parameter                          |
| `params`                 | Accepts a variable number of arguments                   |
| Expression-bodied method | Concise method containing a single expression            |
| Local function           | Function defined inside another method                   |
| Extension method         | Static method callable using instance-method syntax      |

The most important concepts from this chapter are:

1. Normal reference-type arguments are still passed by value. The reference is copied, not the object.
2. `ref` allows a method to change the caller's variable itself.
3. `out` is designed for methods that provide output through parameters.
4. Extension methods let us add convenient method-style APIs without modifying the original class.

# 6. Practice exercises

Try these exercises to reinforce what you've learned.

### Exercise 1 — Calculator

Create a calculator class with overloaded methods:

```
Add(int a, int b)

Add(double a, double b)

Add(int a, int b, int c)
```

### Exercise 2 — Swap

Implement:

```
void Swap(ref int a, ref int b)
```

Verify that it swaps the original caller variables.

### Exercise 3 — TryParse

Implement:

```
bool TryParseAge(string input, out int age)
```

Requirements:

- If input represents a valid positive integer, return `true`.
- Otherwise, return `false`.
- Set the output value appropriately.

Try using `int.TryParse()` internally.

### Exercise 4 — Optional and named arguments

Implement:

```
void CreateUser(
    string name,
    string country = "India",
    bool isActive = true
)
```

Call it using different combinations of positional and named arguments.

### Exercise 5 — Extension methods

Create an extension method for strings:

```
bool IsNullOrEmptyOrWhitespace(this string? value)
```

Test it with:

```
"hello"

""

"   "

null
```

You can also compare your implementation with the existing `string.IsNullOrWhiteSpace()` method.

### Exercise 6 — Reference behavior

Predict the output:

```
public class User
{
    public string Name { get; set; } = "";
}

public void Update(User user)
{
    user.Name = "Updated";

    user = new User
    {
        Name = "New User"
    };
}
```

Call:

```
User user = new User
{
    Name = "Original"
};

Update(user);

Console.WriteLine(user.Name);
```

Explain why.

# 7. Knowledge check

You should be able to explain these concepts without memorizing definitions:

1. What is the difference between static and instance methods?
2. What is method overloading? Why can't we overload based only on return type?
3. How do optional parameters work?
4. What is the difference between optional parameters and method overloading?
5. What are named arguments?
6. Does C# pass objects by reference by default?
7. What exactly is copied when we pass a class variable to a method?
8. What problem does `ref` solve?
9. What's the difference between `ref` and `out`?
10. Why would we use `in`?
11. What is the `params` keyword?
12. What are expression-bodied methods?
13. What is a local function?
14. What is an extension method, and how does it work internally?
15. Where do you encounter extension methods in the .NET ecosystem?

## Next chapter — Chapter 6: Strings and Arrays in C\#

We'll explore two types you'll use constantly in backend development.

Strings:

- String immutability.
- String interpolation (`$"..."`).
- Verbatim strings (`@"..."`).
- Raw string literals.
- String comparison and case sensitivity.
- `StringBuilder`.
- Common string manipulation APIs.
- Formatting and parsing.

Arrays:

- Array declaration and initialization.
- Multidimensional and jagged arrays.
- Array copying vs reference assignment.
- Array resizing.
- Passing arrays to methods.
- Array utilities (`Array.Sort`, `Array.Find`, `Array.Copy`, etc.).
- Arrays vs Java collections vs JavaScript arrays.

We'll also understand when to use arrays versus collections in real backend applications.

Say next when you're ready.
