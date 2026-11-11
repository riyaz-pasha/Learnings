# Chapter 2 — C# Project Structure and Program Execution

In the previous chapter, we understood what .NET, C#, CLR, IL, and the .NET SDK are.

Now let's understand how an actual C# application is structured.

Since you have worked with Java, Spring Boot, and Node.js, we'll frequently compare C# projects with:

- Java Maven/Gradle projects.
- Java package and class structures.
- Node.js modules and `package.json`.
- Express.js application entry points.

By the end of this chapter, you should be comfortable opening an existing .NET project and understanding where everything is.

## 2.1 Create your first .NET project

Let's start by creating a console application.

```
dotnet new console -n LearningDotNet
```

Navigate into it:

```
cd LearningDotNet
```

You'll see something similar to:

```
LearningDotNet/
│
├── LearningDotNet.csproj
│
├── Program.cs
│
├── bin/
│
└── obj/
```

Let's understand each file.

| File / Folder | Purpose                                  | Java / Node.js equivalent               |
| ------------- | ---------------------------------------- | --------------------------------------- |
| `Program.cs`  | Application entry point and initial code | `Main.java` / `index.js`                |
| `.csproj`     | Project configuration and dependencies   | `pom.xml` / `package.json`              |
| `bin/`        | Compiled build outputs                   | `target/` / `dist/`                     |
| `obj/`        | Intermediate build files                 | Build-tool-generated intermediate files |

Two folders are particularly important:

`bin/`

Contains compiled output.

For example:

```
bin/
└── Debug/
    └── net10.0/
        ├── LearningDotNet.dll
        ├── LearningDotNet.deps.json
        ├── LearningDotNet.runtimeconfig.json
        └── LearningDotNet
```

The exact output depends on your target framework and platform.

`obj/`

Contains intermediate build artifacts, generated files, restore information, and compiler-related data.

You generally don't manually modify these folders.

## 2.2 Understanding Program.cs

Open `Program.cs`.

You'll probably see:

```
Console.WriteLine("Hello, World!");
```

Now, something interesting should catch your attention.

In Java, you normally write:

```
public class Main {

    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
```

In JavaScript:

```
console.log("Hello World");
```

But in C#, we don't see any:

- Class declaration.
- `main()` method.
- Explicit entry point.

Why?

Because modern C# supports something called Top-level statements.

Top-level statements were introduced in C# 9.

They allow us to write application code directly without explicitly defining the containing class and entry point.

For example:

```
Console.WriteLine("Hello World");

Console.WriteLine("Learning C#");

int number = 10;

Console.WriteLine(number);
```

This is valid C# code.

### But how does the application actually execute?

The compiler generates the necessary entry-point structure behind the scenes.

Conceptually, your code:

```
Console.WriteLine("Hello World");
```

is equivalent to:

```
internal class Program
{
    private static void Main(string[] args)
    {
        Console.WriteLine("Hello World");
    }
}
```

This is a conceptual equivalent, not necessarily the exact generated code.

The important thing:

Top-level statements are syntactic convenience. They don't eliminate the need for an application entry point.

The compiler still generates one.

### When should we use top-level statements?

For smaller applications, scripts, simple console tools, and application startup code, they are convenient.

For example:

```
var message = "Hello";

Console.WriteLine(message);
```

However, in larger applications, we'll often introduce separate classes and organize the application into multiple files.

We'll see that shortly.

## 2.3 Understanding the Main method

Since you already know Java, let's properly understand the C# entry point.

The traditional C# entry point looks like this:

```
class Program
{
    static void Main(string[] args)
    {
        Console.WriteLine("Application started");
    }
}
```

Compare this with Java:

```
public class Main {

    public static void main(String[] args) {

        System.out.println("Application started");

    }
}
```

They are very similar.

Let's break down C#'s `Main`.

```
static void Main(string[] args)
```

| Part            | Meaning                                               |
| --------------- | ----------------------------------------------------- |
| `static`        | Can be called without creating an instance of Program |
| `void`          | Doesn't return a value                                |
| `Main`          | Recognized application entry-point method             |
| `string[] args` | Command-line arguments passed to the application      |

There are multiple valid entry-point signatures.

For example:

```
static int Main(string[] args)
{
    return 0;
}
```

Returning an integer allows the application to return an exit code to the operating system.

Another possibility:

```
static async Task Main(string[] args)
{
    await Task.Delay(1000);

    Console.WriteLine("Finished");
}
```

This allows asynchronous operations inside the entry point.

We'll study `Task`, `async`, and `await` in depth later.

### Important comparison

| Java                                     | C#                                |
| ---------------------------------------- | --------------------------------- |
| `public static void main(String[] args)` | `static void Main(string[] args)` |
| `String[] args`                          | `string[] args`                   |
| `System.out.println()`                   | `Console.WriteLine()`             |

One difference: Java requires the traditional entry-point method to be `public`. C# does not require `Main` to be public.

My recommendation: Understand both traditional `Main` and top-level statements. You'll encounter both in real projects.

# 2.4 Understanding the .csproj file

This is one of the most important files in a .NET project.

Open:

```
LearningDotNet.csproj
```

You'll see something similar to:

```
<Project Sdk="Microsoft.NET.Sdk">

    <PropertyGroup>

        <OutputType>Exe</OutputType>

        <TargetFramework>net10.0</TargetFramework>

        <ImplicitUsings>enable</ImplicitUsings>

        <Nullable>enable</Nullable>

    </PropertyGroup>

</Project>
```

Let's understand what these configurations mean.

### A. Project SDK

```
<Project Sdk="Microsoft.NET.Sdk">
```

This specifies which SDK and its associated MSBuild build logic should be used to build the project.

Here, we're using the standard .NET SDK.

Think of this as telling the build system which project-building infrastructure to use.

### B. OutputType

```
<OutputType>Exe</OutputType>
```

This tells .NET what kind of output the project should produce.

Common values:

| OutputType | Meaning                |
| ---------- | ---------------------- |
| `Exe`      | Executable application |
| `Library`  | Reusable class library |

For example, suppose we are building a reusable payment library.

We don't necessarily want an executable application.

We want something other applications can reference.

We can create a class library:

```
dotnet new classlib -n PaymentLibrary
```

Its project configuration will use a library output type.

The resulting assembly is generally a `.dll`.

An important distinction: `.dll` doesn't necessarily mean a class library. A normal modern .NET application can also be built into a `.dll` that is executed through the .NET runtime.

### C. TargetFramework

```
<TargetFramework>net10.0</TargetFramework>
```

This specifies the target .NET framework version.

For example:

```
<TargetFramework>net8.0</TargetFramework>
```

or:

```
<TargetFramework>net10.0</TargetFramework>
```

This tells the build system which target framework and reference APIs the application should compile against.

Think of it like selecting the Java version for your application, although .NET also has framework-specific APIs and compatibility rules.

You might encounter:

```
<TargetFramework>net8.0</TargetFramework>
```

in an existing client project.

That doesn't mean you should change it to the newest version.

Always follow the project's target framework unless an upgrade is explicitly planned.

### D. ImplicitUsings

```
<ImplicitUsings>enable</ImplicitUsings>
```

This is an interesting C# feature.

Normally, to use a class from another namespace, we write a `using` directive.

For example:

```
using System;

Console.WriteLine("Hello");
```

But our generated project might not have `using System;`.

Why does `Console` still work?

Because implicit usings automatically make certain commonly used namespaces available through generated global using directives.

For example, depending on the project type, the SDK may generate global imports such as:

```
global using System;
global using System.Collections.Generic;
global using System.IO;
global using System.Linq;
```

This is why you don't always see explicit `using` directives in modern .NET projects.

We will understand namespaces and using directives in detail shortly.

### E. Nullable

```
<Nullable>enable</Nullable>
```

This enables nullable reference type analysis.

For example:

```
string name = null;
```

With nullable reference types enabled, the compiler warns that we're assigning a potentially null value to a non-nullable reference.

Instead, we can write:

```
string? name = null;
```

The `?` indicates that the variable is allowed to contain null.

This is particularly useful in enterprise applications because it helps catch potential null-reference problems during development.

Important: nullable reference types primarily provide compiler analysis and warnings. They don't automatically prevent null values at runtime.

We'll study this properly when we reach data types.

# 2.5 Understanding namespaces

This is where the comparison with Java becomes interesting.

In Java:

```
package com.example.payment;

public class PaymentService {
}
```

The package determines the class's fully qualified name:

```
com.example.payment.PaymentService
```

In C#, we have namespaces.

For example:

```
namespace LearningDotNet.Payment;

public class PaymentService
{
}
```

The fully qualified name is:

```
LearningDotNet.Payment.PaymentService
```

Conceptually, namespaces serve a similar purpose to Java packages.

They help us:

- Organize related types.
- Avoid naming collisions.
- Create logical boundaries.
- Structure large applications.

### Two ways to declare namespaces

1\. Block-scoped namespaces

```
namespace LearningDotNet.Payment
{
    public class PaymentService
    {
        public void ProcessPayment()
        {
            Console.WriteLine("Processing payment");
        }
    }
}
```

2\. File-scoped namespaces

Introduced in C# 10.

```
namespace LearningDotNet.Payment;

public class PaymentService
{
    public void ProcessPayment()
    {
        Console.WriteLine("Processing payment");
    }
}
```

The second form is more compact.

You'll frequently encounter file-scoped namespaces in modern .NET projects.

### Important: Namespace vs Folder

Suppose your project looks like this:

```
LearningDotNet/
│
├── Program.cs
│
├── Services/
│   └── PaymentService.cs
│
└── Models/
    └── Payment.cs
```

You might declare:

```
namespace LearningDotNet.Services;
```

inside `PaymentService.cs`.

This is a naming convention that aligns namespaces with folders.

However, C# does not require namespace names to match physical folder paths.

You could put a class in a folder named `Services` and declare:

```
namespace MyCompany.Billing;
```

It would still work.

This differs from Java's conventional package-to-directory structure.

# 2.6 Understanding using directives

Suppose you have a class:

```
namespace LearningDotNet.Services;

public class PaymentService
{
    public void ProcessPayment()
    {
        Console.WriteLine("Processing payment");
    }
}
```

You want to use it from `Program.cs`.

Without importing the namespace:

```
LearningDotNet.Services.PaymentService service =
    new LearningDotNet.Services.PaymentService();

service.ProcessPayment();
```

That works.

But repeatedly writing the fully qualified name is inconvenient.

Instead:

```
using LearningDotNet.Services;

PaymentService service = new PaymentService();

service.ProcessPayment();
```

The `using` directive makes types from that namespace available by their short names.

Compare this with Java:

```
import com.example.payment.PaymentService;
```

Both improve readability by avoiding fully qualified type names everywhere.

### Important distinction: using doesn't add dependencies

Suppose you write:

```
using System.Linq;
```

This doesn't install a package.

It only brings types from an available namespace into scope.

To add an external NuGet dependency, you use package management.

For example:

```
dotnet add package Newtonsoft.Json
```

We'll study NuGet separately.

### Other forms of using

C# provides additional forms.

Namespace alias:

```
using Payment = LearningDotNet.Services.PaymentService;
```

Now:

```
Payment service = new Payment();
```

This is useful when two namespaces contain types with the same name.

Static using:

```
using static System.Math;

double result = Sqrt(25);
```

Without static using:

```
double result = Math.Sqrt(25);
```

Global using:

```
global using System;
```

This makes the namespace available across all files in the project.

You don't need to repeat the directive in every file.

One more thing: C# also uses the `using` keyword for resource management.

For example:

```
using var file = File.OpenRead("data.txt");
```

That is a completely different use of the keyword. It ensures deterministic cleanup of disposable resources when the scope exits.

We'll study this in our resource-management chapter.

# 2.7 Understanding classes and files

Let's build a small application.

Suppose we have:

```
LearningDotNet/
│
├── Program.cs
│
├── Services/
│   └── PaymentService.cs
│
└── Models/
    └── Payment.cs
```

First, create a model.

Models/Payment.cs

```
namespace LearningDotNet.Models;

public class Payment
{
    public int Id { get; set; }

    public decimal Amount { get; set; }

    public string Status { get; set; } = "Pending";
}
```

Now create a service.

Services/PaymentService.cs

```
using LearningDotNet.Models;

namespace LearningDotNet.Services;

public class PaymentService
{
    public void ProcessPayment(Payment payment)
    {
        Console.WriteLine(
            $"Processing payment of {payment.Amount}"
        );

        payment.Status = "Completed";
    }
}
```

Now use it from the entry point.

Program.cs

```
using LearningDotNet.Models;
using LearningDotNet.Services;

var paymentService = new PaymentService();

var payment = new Payment
{
    Id = 1,
    Amount = 1500,
};

paymentService.ProcessPayment(payment);

Console.WriteLine($"Payment Status: {payment.Status}");
```

Output:

```
Processing payment of 1500
Payment Status: Completed
```

Notice a few things.

### A. C# allows multiple classes in one file

Java traditionally requires a public top-level class's filename to match the class name.

For example:

```
public class PaymentService {
}
```

The source file must be:

```
PaymentService.java
```

C# does not enforce this restriction.

You could declare:

```
public class PaymentService
{
}

public class RefundService
{
}
```

inside the same `.cs` file.

However, in real applications, we generally organize important classes into individual files.

### B. File names are conventions

In C#:

```
PaymentService.cs
```

is a convention.

The compiler doesn't require the file name to match the class name.

### C. Access modifiers

You might have noticed:

```
public class PaymentService
```

C# has access modifiers similar to Java.

Common ones include:

| C#                   | Meaning                                                        | Java comparison      |
| -------------------- | -------------------------------------------------------------- | -------------------- |
| `public`             | Accessible from anywhere permitted by the type's accessibility | `public`             |
| `private`            | Accessible within the containing type                          | `private`            |
| `protected`          | Accessible within the containing type and derived types        | `protected`          |
| `internal`           | Accessible within the same assembly                            | No direct equivalent |
| `protected internal` | Same assembly OR derived types                                 | No direct equivalent |
| `private protected`  | Same assembly AND derived types                                | No direct equivalent |

The `internal` modifier is particularly important.

Imagine you have a class:

```
internal class PaymentService
{
}
```

Other classes in the same assembly can access it.

But another assembly generally cannot access it.

Think of it as a visibility boundary at the compiled application/library level.

Also remember these defaults:

- Top-level types without an access modifier are generally `internal`.
- Class members without an access modifier are generally `private`.

We'll study accessibility in detail during the OOP chapter.

# 2.8 Understanding projects and solutions

This is important when working with enterprise .NET applications.

Let's compare Java and .NET project organization.

In Java, you might have:

```
PaymentApplication/
    pom.xml
    src/
    ...

NotificationService/
    pom.xml
    src/
    ...

CommonLibrary/
    pom.xml
    src/
    ...
```

You might manage these through Maven multi-module projects.

In .NET, we have two important concepts:

Project (`.csproj`)

A project defines a buildable unit.

For example:

```
Payment.API/
    Payment.API.csproj

Payment.Application/
    Payment.Application.csproj

Payment.Infrastructure/
    Payment.Infrastructure.csproj

Payment.Domain/
    Payment.Domain.csproj
```

Each project has its own:

- Source code.
- Dependencies.
- Configuration.
- Build output.
- Target framework.

Solution (`.sln` or `.slnx`)

A solution groups multiple projects together.

For example:

```
PaymentSystem/
│
├── PaymentSystem.sln
│
├── Payment.API/
│   └── Payment.API.csproj
│
├── Payment.Application/
│   └── Payment.Application.csproj
│
├── Payment.Infrastructure/
│   └── Payment.Infrastructure.csproj
│
└── Payment.Domain/
    └── Payment.Domain.csproj
```

A solution helps organize and build multiple related projects together.

It is not itself an executable application.

### Creating a solution

```
dotnet new sln -n PaymentSystem
```

Create projects:

```
dotnet new webapi -n Payment.API

dotnet new classlib -n Payment.Application

dotnet new classlib -n Payment.Domain

dotnet new classlib -n Payment.Infrastructure
```

Add them to the solution:

```
dotnet sln add Payment.API/Payment.API.csproj

dotnet sln add Payment.Application/Payment.Application.csproj

dotnet sln add Payment.Domain/Payment.Domain.csproj

dotnet sln add Payment.Infrastructure/Payment.Infrastructure.csproj
```

Now all four projects are grouped under the solution.

### How do projects communicate with each other?

Through project references.

For example, suppose `Payment.API` needs classes from `Payment.Application`.

You can add a reference:

```
dotnet add Payment.API/Payment.API.csproj reference Payment.Application/Payment.Application.csproj
```

This adds a project reference to the `.csproj` file.

Conceptually, this is similar to adding a Maven module dependency or a Gradle project dependency.

A project reference allows one project to compile against the public types exposed by another referenced project, subject to normal accessibility and compatibility rules.

# 2.9 Understanding the .NET build and execution process

Let's connect everything we've learned.

When you execute:

```
dotnet run
```

the general flow is:

\#chatgpt-mermaid-\_r_29e\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_29e\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_29e\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_29e\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_29e\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_29e\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_29e\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_29e\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_29e\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_29e\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_29e\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_29e\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_29e\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_29e\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_29e\_ p{margin:0;}#chatgpt-mermaid-\_r_29e\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_29e\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_29e\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_29e\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_29e\_ .label text,#chatgpt-mermaid-\_r_29e\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_29e\_ .node rect,#chatgpt-mermaid-\_r_29e\_ .node circle,#chatgpt-mermaid-\_r_29e\_ .node ellipse,#chatgpt-mermaid-\_r_29e\_ .node polygon,#chatgpt-mermaid-\_r_29e\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_29e\_ .rough-node .label text,#chatgpt-mermaid-\_r_29e\_ .node .label text,#chatgpt-mermaid-\_r_29e\_ .image-shape .label,#chatgpt-mermaid-\_r_29e\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_29e\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_29e\_ .rough-node .label,#chatgpt-mermaid-\_r_29e\_ .node .label,#chatgpt-mermaid-\_r_29e\_ .image-shape .label,#chatgpt-mermaid-\_r_29e\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_29e\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_29e\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_29e\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_29e\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_29e\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_29e\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_29e\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_29e\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_29e\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_29e\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_29e\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_29e\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_29e\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_29e\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_29e\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_29e\_ .icon-shape,#chatgpt-mermaid-\_r_29e\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_29e\_ .icon-shape p,#chatgpt-mermaid-\_r_29e\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_29e\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_29e\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_29e\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_29e\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_29e\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_29e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_29e\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_29e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_29e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_29e\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_29e\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_29e\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_29e\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_29e\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_29e\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_29e\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_29e\_ .node rect,#chatgpt-mermaid-\_r_29e\_ .node circle,#chatgpt-mermaid-\_r_29e\_ .node ellipse,#chatgpt-mermaid-\_r_29e\_ .node polygon,#chatgpt-mermaid-\_r_29e\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_29e\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_29e\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_29e\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_29e\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_29e\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}dotnet runMSBuild Project EvaluationRestore DependenciesC# CompilationGenerate .NET AssemblyStart .NET ApplicationCLR Loads AssemblyApplication Entry PointProgram Executes

Let's understand the common commands.

| Command          | Purpose                            | Java comparison                       |
| ---------------- | ---------------------------------- | ------------------------------------- |
| `dotnet new`     | Create a project from a template   | Maven archetype / project template    |
| `dotnet restore` | Resolve and download dependencies  | `mvn dependency:resolve`              |
| `dotnet build`   | Compile the project                | `mvn compile`                         |
| `dotnet run`     | Build and execute the project      | Maven execution plugins / Java run    |
| `dotnet test`    | Execute tests                      | `mvn test`                            |
| `dotnet publish` | Prepare application for deployment | Packaging plus deployment preparation |

One important detail:

`dotnet build` automatically performs restore when needed.

So you don't have to explicitly run restore every time.

### What is publish?

Building and publishing are different.

`dotnet build` primarily produces the build outputs.

`dotnet publish` prepares the application and its required dependencies for deployment.

Example:

```
dotnet publish -c Release
```

There are different deployment models:

- Framework-dependent deployment: the target machine needs a compatible .NET runtime.
- Self-contained deployment: the application includes the required .NET runtime components.
- Native AOT: compilation produces native code ahead of time for supported application scenarios.

We'll study deployment properly later.

# 2.10 The overall mental model

Let's consolidate the hierarchy.

```
.NET Solution
    |
    |-- Payment.API Project
    |       |
    |       |-- Program.cs
    |       |-- Controllers/
    |       |-- Payment.API.csproj
    |
    |-- Payment.Application Project
    |       |
    |       |-- Services/
    |       |-- Payment.Application.csproj
    |
    |-- Payment.Domain Project
    |       |
    |       |-- Entities/
    |       |-- Payment.Domain.csproj
    |
    |-- Payment.Infrastructure Project
            |
            |-- Repositories/
            |-- Payment.Infrastructure.csproj
```

Remember:

- Solution: Groups projects.
- Project: Defines a buildable unit.
- Namespace: Organizes types logically.
- Class: Defines a type.
- File: Holds source code; it doesn't have to correspond one-to-one with a class.
- Assembly: Compiled output containing types and metadata.
- Runtime: Executes the application.

These are different concepts. Don't confuse them.

# 3. Practice exercise

Let's reinforce what we've learned.

Create a console project:

```
dotnet new console -n BankingApplication
```

Create this structure:

```
BankingApplication/
│
├── Program.cs
│
├── Models/
│   └── Account.cs
│
└── Services/
    └── AccountService.cs
```

Account.cs

Create an `Account` class with:

- `Id`
- `AccountHolder`
- `Balance`

AccountService.cs

Create an `AccountService` class with:

- `Deposit(Account account, decimal amount)`
- `Withdraw(Account account, decimal amount)`

For now, simply update the balance. We'll learn proper validation and business logic shortly.

Program.cs

1. Create an account.
2. Deposit 1000.
3. Withdraw 300.
4. Print the final balance.

Try implementing it without copying the earlier example.

This exercise will help you become familiar with navigating a C# project and organizing classes.

# 4. Interview and project knowledge check

Make sure you can explain these concepts:

1. What are top-level statements? How do they work internally?
2. What is the purpose of `Program.cs`?
3. What does a `.csproj` file contain?
4. What is the difference between `TargetFramework` and `OutputType`?
5. What is the difference between a namespace and a folder?
6. Does `using` install a dependency?
7. What is the difference between a project and a solution?
8. What is the difference between an assembly and a project?
9. What is the purpose of `internal`?
10. What happens when you execute `dotnet run`?

The most important thing to take away: A .NET solution can contain multiple projects, each project compiles into an assembly, namespaces organize the types within those assemblies, and the CLR executes the resulting application.

## Next chapter — Chapter 3: C# Variables and Data Types

This is where we'll start getting into the actual C# language.

We'll cover:

- `int`, `long`, `double`, `float`, `decimal`, `bool`, `char`, and `string`.
- `var` vs explicit types.
- Value types vs reference types.
- Stack vs heap (including common misconceptions).
- Structs vs classes.
- Boxing and unboxing.
- Nullable value types.
- Nullable reference types.
- How C# differs from Java and JavaScript in memory and type behavior.

This chapter is particularly important because understanding value types and reference types will make many later C# concepts much easier.

Say next when ready.
