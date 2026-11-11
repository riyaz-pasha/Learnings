# .NET and C# — Complete Backend Developer Course

We'll approach this as a course designed for an experienced Java and JavaScript developer, not as a beginner programming course.

You already understand programming, OOP, backend development, APIs, databases, asynchronous programming, and software design. So rather than teaching programming concepts from scratch, we'll build a bridge between what you know and how the .NET ecosystem implements those concepts.

The goal is that you should eventually be able to:

- Understand an existing enterprise .NET codebase without constantly looking up syntax.
- Write idiomatic, maintainable C# code.
- Understand how the .NET runtime works internally.
- Work confidently with asynchronous code, concurrency, memory management, and exception handling.
- Understand the standard libraries and ecosystem.
- Build backend applications using ASP.NET Core, Entity Framework Core, and related technologies.
- Make reasonable architectural and implementation decisions in a real client project.

## 1. How we'll approach learning

We'll use three perspectives for each important concept:

1\. Familiarity: How does this relate to Java or JavaScript?

2\. Internal understanding: Why does C# or .NET work this way? What happens internally?

3\. Practical application: How would you use this in an actual enterprise application?

For example, when learning `async` and `await`, we won't stop at learning the syntax. We'll understand:

- How it compares to Java's `CompletableFuture` and JavaScript Promises.
- What a `Task` represents.
- What happens when execution reaches an `await`.
- Whether a new thread is created.
- How exceptions propagate.
- How cancellation works.
- How to implement asynchronous database and HTTP calls correctly.

This is the depth we'll aim for.

# 2. Course roadmap

We'll divide the learning into three major parts.

## Part 1 — C# language fundamentals

First, we'll learn C# itself. C# is the primary language we'll use to develop applications on .NET.

| Chapter | Topic                          | What you'll understand                                                                |
| ------- | ------------------------------ | ------------------------------------------------------------------------------------- |
| 1       | Introduction to .NET           | .NET ecosystem, C#, CLR, SDK, runtime, compilation, execution                         |
| 2       | C# project structure           | Programs, namespaces, classes, entry points, project files                            |
| 3       | Variables and data types       | Value types, reference types, primitive types, type inference                         |
| 4       | Operators and control flow     | Conditions, loops, switch expressions, pattern matching                               |
| 5       | Methods and parameters         | Methods, overloading, optional parameters, `ref`, `out`, `in`                         |
| 6       | Strings and arrays             | String behavior, immutability, arrays, string manipulation                            |
| 7       | Collections                    | List, Dictionary, HashSet, Queue, Stack and collection internals                      |
| 8       | Object-oriented programming    | Classes, objects, constructors, encapsulation, inheritance, polymorphism              |
| 9       | Interfaces and abstraction     | Interfaces, abstract classes, default implementations, dependency inversion           |
| 10      | Structs, records and enums     | Value semantics, records, immutable data models, enums                                |
| 11      | Generics                       | Generic classes, methods, constraints, variance                                       |
| 12      | Delegates and events           | Delegates, Func, Action, Predicate, lambdas, event handling                           |
| 13      | LINQ                           | Filtering, mapping, grouping, joins, deferred execution, IQueryable                   |
| 14      | Exception handling             | Exceptions, custom exceptions, propagation, exception filters                         |
| 15      | Resource management            | IDisposable, using, deterministic cleanup, finalizers                                 |
| 16      | Asynchronous programming       | Task, async/await, Task.WhenAll, cancellation, exception handling                     |
| 17      | Multithreading and concurrency | Threads, ThreadPool, locks, semaphores, concurrent collections                        |
| 18      | Memory management              | Stack, heap, garbage collection, generations, allocations                             |
| 19      | Advanced C# features           | Attributes, reflection, extension methods, pattern matching, nullable reference types |
| 20      | Working with files and data    | JSON, serialization, streams, file handling, dates, configuration basics              |

We will split particularly large chapters into smaller lessons when necessary.

## Part 2 — .NET platform and developer ecosystem

After understanding C#, we'll learn the infrastructure and tools that make up the .NET development environment.

\| Chapter | Topic |\
\|---|---|\
\| 21 | .NET Runtime internals | CLR, JIT, IL, assemblies, loading |\
\| 22 | .NET SDK and CLI | `dotnet` commands, project creation, build, run, publish |\
\| 23 | Projects and solutions | `.csproj`, `.sln`, project references, build configuration |\
\| 24 | NuGet | Package management, dependency versions, package restoration |\
\| 25 | Testing | xUnit, NUnit, assertions, mocking, test organization |\
\| 26 | Diagnostics | Logging, debugging, tracing, profiling |\
\| 27 | Application configuration | Configuration providers, environment variables, secrets |

## Part 3 — Backend development with .NET

Once the core language and platform are covered, we'll move to backend frameworks.

We'll relate these to your Spring Boot and Express.js experience.

| Chapter | Technology / Topic                | Java / JavaScript comparison                       |
| ------- | --------------------------------- | -------------------------------------------------- |
| 28      | ASP.NET Core fundamentals         | Spring Boot / Express.js                           |
| 29      | Routing and controllers           | Spring MVC Controllers / Express routes            |
| 30      | Middleware pipeline               | Servlet filters / Express middleware               |
| 31      | Dependency Injection              | Spring IoC Container                               |
| 32      | Configuration and options         | Spring Configuration / application.yml             |
| 33      | Validation and exception handling | Bean Validation / Express error middleware         |
| 34      | Authentication and authorization  | Spring Security                                    |
| 35      | Entity Framework Core             | JPA / Hibernate                                    |
| 36      | Database transactions             | Spring Transaction Management                      |
| 37      | Background processing             | Spring Scheduling / Node workers                   |
| 38      | HTTP clients and integrations     | WebClient / Axios / Fetch                          |
| 39      | Caching                           | Spring Cache / Redis integrations                  |
| 40      | Messaging                         | Kafka / RabbitMQ / SQS integrations                |
| 41      | Testing backend applications      | Spring Boot Testing                                |
| 42      | Production readiness              | Logging, health checks, observability, performance |
| 43      | Deployment                        | Docker, containers, cloud deployment               |

We'll also work through a practical backend application as we learn the framework concepts.

# Chapter 1 — Understanding the .NET ecosystem

Before learning C# syntax, let's understand what .NET actually is.

This is important because C# and .NET are not the same thing.

## 1.1 What exactly is .NET?

Think about your Java experience.

You write Java code:

```
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
```

But Java code doesn't independently execute itself.

You need an ecosystem:

- Java language
- Java compiler (`javac`)
- JVM
- Java standard libraries
- JDK
- Build tools such as Maven or Gradle

The JVM executes compiled Java bytecode, manages memory, performs garbage collection, handles threads, and provides other runtime services.

.NET has a very similar ecosystem.

In .NET:

- C# is the programming language.
- .NET is the development platform.
- CLR is the runtime that executes managed code.
- .NET SDK provides development and build tools.
- NuGet is the package management ecosystem.
- ASP.NET Core is a popular web application framework.

Let's map them.

| Java ecosystem              | .NET ecosystem                  |
| --------------------------- | ------------------------------- |
| Java                        | C#                              |
| JVM                         | CLR                             |
| JDK                         | .NET SDK                        |
| JRE                         | .NET Runtime                    |
| Java bytecode               | CIL / IL                        |
| Java compiler (`javac`)     | C# compiler (Roslyn)            |
| JAR                         | .NET assembly (`.dll` / `.exe`) |
| Maven / Gradle dependencies | NuGet packages                  |
| Spring Boot                 | ASP.NET Core                    |
| JPA / Hibernate             | Entity Framework Core           |
| JDBC                        | ADO.NET                         |

These are conceptual equivalents, not necessarily exact implementation equivalents.

The main idea:

When you work with .NET, you're working with an ecosystem, not just a programming language.

## 1.2 Understand the relationship between C# and .NET

Consider this C# program:

```
Console.WriteLine("Hello World");
```

This is C# code.

The compiler understands C# syntax and compiles it into an intermediate representation.

The .NET runtime knows how to execute that representation.

So the relationship looks like this:

\#chatgpt-mermaid-\_r_qi\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_qi\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_qi\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_qi\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_qi\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qi\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_qi\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_qi\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_qi\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_qi\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_qi\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_qi\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qi\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qi\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_qi\_ p{margin:0;}#chatgpt-mermaid-\_r_qi\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qi\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qi\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qi\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_qi\_ .label text,#chatgpt-mermaid-\_r_qi\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qi\_ .node rect,#chatgpt-mermaid-\_r_qi\_ .node circle,#chatgpt-mermaid-\_r_qi\_ .node ellipse,#chatgpt-mermaid-\_r_qi\_ .node polygon,#chatgpt-mermaid-\_r_qi\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_qi\_ .rough-node .label text,#chatgpt-mermaid-\_r_qi\_ .node .label text,#chatgpt-mermaid-\_r_qi\_ .image-shape .label,#chatgpt-mermaid-\_r_qi\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_qi\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_qi\_ .rough-node .label,#chatgpt-mermaid-\_r_qi\_ .node .label,#chatgpt-mermaid-\_r_qi\_ .image-shape .label,#chatgpt-mermaid-\_r_qi\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_qi\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_qi\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qi\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qi\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_qi\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_qi\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_qi\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_qi\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_qi\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_qi\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_qi\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qi\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qi\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_qi\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qi\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_qi\_ .icon-shape,#chatgpt-mermaid-\_r_qi\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_qi\_ .icon-shape p,#chatgpt-mermaid-\_r_qi\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_qi\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_qi\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_qi\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_qi\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_qi\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_qi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_qi\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_qi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_qi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qi\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_qi\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qi\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_qi\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_qi\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_qi\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_qi\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_qi\_ .node rect,#chatgpt-mermaid-\_r_qi\_ .node circle,#chatgpt-mermaid-\_r_qi\_ .node ellipse,#chatgpt-mermaid-\_r_qi\_ .node polygon,#chatgpt-mermaid-\_r_qi\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_qi\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_qi\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_qi\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_qi\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qi\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}C# Source CodeC# CompilerIntermediate Language (IL).NET Runtime (CLR)Machine CodeCPU Executes Instructions

Notice that C# itself doesn't execute the program.

The runtime does.

This is conceptually similar to Java:

\#chatgpt-mermaid-\_r_qp\_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;fill:rgb(237, 237, 237);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-\_r_qp\_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_qp\_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-\_r_qp\_ .error-icon{fill:rgb(48, 48, 48);}#chatgpt-mermaid-\_r_qp\_ .error-text{fill:rgb(237, 237, 237);stroke:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qp\_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-\_r_qp\_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-\_r_qp\_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-\_r_qp\_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-\_r_qp\_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-\_r_qp\_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-\_r_qp\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qp\_ .marker.cross{stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qp\_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:16px;}#chatgpt-mermaid-\_r_qp\_ p{margin:0;}#chatgpt-mermaid-\_r_qp\_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qp\_ .cluster-label text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qp\_ .cluster-label span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qp\_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-\_r_qp\_ .label text,#chatgpt-mermaid-\_r_qp\_ span{fill:rgb(237, 237, 237);color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qp\_ .node rect,#chatgpt-mermaid-\_r_qp\_ .node circle,#chatgpt-mermaid-\_r_qp\_ .node ellipse,#chatgpt-mermaid-\_r_qp\_ .node polygon,#chatgpt-mermaid-\_r_qp\_ .node path{fill:rgb(9, 23, 44);stroke:rgb(31, 78, 148);stroke-width:1px;}#chatgpt-mermaid-\_r_qp\_ .rough-node .label text,#chatgpt-mermaid-\_r_qp\_ .node .label text,#chatgpt-mermaid-\_r_qp\_ .image-shape .label,#chatgpt-mermaid-\_r_qp\_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-\_r_qp\_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-\_r_qp\_ .rough-node .label,#chatgpt-mermaid-\_r_qp\_ .node .label,#chatgpt-mermaid-\_r_qp\_ .image-shape .label,#chatgpt-mermaid-\_r_qp\_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-\_r_qp\_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-\_r_qp\_ .root .anchor path{fill:rgb(175, 175, 175)!important;stroke-width:0;stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qp\_ .arrowheadPath{fill:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qp\_ .edgePath .path{stroke:rgb(175, 175, 175);stroke-width:1px;}#chatgpt-mermaid-\_r_qp\_ .flowchart-link{stroke:rgb(175, 175, 175);fill:none;}#chatgpt-mermaid-\_r_qp\_ .edgeLabel{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_qp\_ .edgeLabel p{background-color:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_qp\_ .edgeLabel rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_qp\_ .labelBkg{background-color:rgba(0, 0, 0, 0.5);}#chatgpt-mermaid-\_r_qp\_ .cluster rect{fill:rgb(48, 48, 48);stroke:rgba(255, 255, 255, 0.15);stroke-width:1px;}#chatgpt-mermaid-\_r_qp\_ .cluster text{fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qp\_ .cluster span{color:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qp\_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;font-size:12px;background:rgb(48, 48, 48);border:1px solid rgba(255, 255, 255, 0.15);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-\_r_qp\_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(237, 237, 237);}#chatgpt-mermaid-\_r_qp\_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-\_r_qp\_ .icon-shape,#chatgpt-mermaid-\_r_qp\_ .image-shape{background-color:rgb(0, 0, 0);text-align:center;}#chatgpt-mermaid-\_r_qp\_ .icon-shape p,#chatgpt-mermaid-\_r_qp\_ .image-shape p{background-color:rgb(0, 0, 0);padding:2px;}#chatgpt-mermaid-\_r_qp\_ .icon-shape .label rect,#chatgpt-mermaid-\_r_qp\_ .image-shape .label rect{opacity:0.5;background-color:rgb(0, 0, 0);fill:rgb(0, 0, 0);}#chatgpt-mermaid-\_r_qp\_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-\_r_qp\_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-\_r_qp\_ .node .neo-node{stroke:rgb(31, 78, 148);}#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].node rect,#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].cluster rect,#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-\_r_qp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-\_r_qp\_-gradient);stroke-width:1px;}#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].node .neo-line path{stroke:rgb(31, 78, 148);filter:none;}#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-\_r_qp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-\_r_qp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qp\_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-\_r_qp\_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-\_r_qp\_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_qp\_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(153, 206, 255);}#chatgpt-mermaid-\_r_qp\_ .node tspan[font-weight="normal"],#chatgpt-mermaid-\_r_qp\_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-\_r_qp\_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-width:1px;}#chatgpt-mermaid-\_r_qp\_ .node rect,#chatgpt-mermaid-\_r_qp\_ .node circle,#chatgpt-mermaid-\_r_qp\_ .node ellipse,#chatgpt-mermaid-\_r_qp\_ .node polygon,#chatgpt-mermaid-\_r_qp\_ .node path{fill:rgb(0, 40, 77);stroke:rgba(255, 255, 255, 0.1);stroke-width:1px;}#chatgpt-mermaid-\_r_qp\_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-\_r_qp\_ .mermaid-decision .label-container{fill:rgb(0, 14, 26);stroke:rgb(26, 62, 95);stroke-dasharray:2px,2px;}#chatgpt-mermaid-\_r_qp\_ .edgePaths .flowchart-link{stroke:rgb(175, 175, 175);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-\_r_qp\_ .marker{fill:rgb(175, 175, 175);stroke:rgb(175, 175, 175);}#chatgpt-mermaid-\_r_qp\_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,Segoe UI,Helvetica,Apple Color Emoji,Arial,sans-serif,Segoe UI Emoji,Segoe UI Symbol;}Java Source CodeJava CompilerJava BytecodeJVMMachine CodeCPU Executes Instructions

Both ecosystems use an intermediate representation that the runtime executes.

This design gives them several benefits:

- Portability across supported operating systems and architectures.
- Runtime-managed memory.
- Exception handling infrastructure.
- Runtime type information.
- Performance optimizations.
- Integration with platform libraries.

## 1.3 What is CLR?

CLR stands for Common Language Runtime.

This is one of the most important concepts in .NET.

Think of CLR as the equivalent of the JVM.

When you execute a .NET application, the CLR provides the environment in which your managed application runs.

It is responsible for several things:

| CLR responsibility        | What it means                                     |
| ------------------------- | ------------------------------------------------- |
| JIT compilation           | Converts IL into native machine instructions      |
| Garbage Collection        | Automatically reclaims eligible managed memory    |
| Exception handling        | Provides runtime support for exceptions           |
| Type safety               | Enforces runtime type-related safety rules        |
| Thread management support | Provides runtime threading infrastructure         |
| Memory management         | Manages managed memory allocation and reclamation |
| Code execution            | Loads and executes managed assemblies             |

Let's understand the most important two.

A. JIT compilation

Suppose you write:

```
int a = 10;
int b = 20;

int result = a + b;
```

The C# compiler doesn't normally translate this directly into CPU instructions when building a regular framework-dependent application.

Instead, it compiles your C# code into IL.

When the application runs, the CLR's Just-In-Time compiler converts the required IL into machine code.

The CPU can execute that machine code.

The interesting part is that JIT compilation can perform runtime optimizations based on execution characteristics.

This is similar to how the JVM uses JIT compilation.

B. Garbage Collection

Consider:

```
public class User
{
    public string Name { get; set; }
}

public void CreateUser()
{
    var user = new User
    {
        Name = "John"
    };
}
```

A `User` object is allocated in managed memory.

When the object becomes unreachable and is no longer needed, the garbage collector can eventually reclaim its memory.

You don't normally need to manually free the memory.

This is similar to Java.

However, remember this distinction:

Garbage collection manages memory. It does not automatically clean up every external resource, such as file handles, database connections, or sockets. We'll study that in detail when we reach `IDisposable`.

## 1.4 What is IL? Is it the same as Java bytecode?

IL means Intermediate Language. It's also called CIL (Common Intermediate Language).

When you compile C# code, the compiler generates IL.

For example:

```
public class Calculator
{
    public int Add(int a, int b)
    {
        return a + b;
    }
}
```

Conceptually, this gets compiled into instructions resembling:

```
load argument 1
load argument 2
add
return
```

This isn't the actual complete IL for the method, just a conceptual representation.

The generated IL is stored in a .NET assembly.

Java has a very similar concept:

```
public int add(int a, int b) {
    return a + b;
}
```

Java compiles this into bytecode.

| Java           | .NET                                 |
| -------------- | ------------------------------------ |
| Bytecode       | IL                                   |
| JVM            | CLR                                  |
| JIT compiler   | JIT compiler                         |
| `.class` files | Types stored in assemblies           |
| JAR files      | Assemblies and packaged applications |

One important detail:

.NET also supports Native AOT compilation, where applications or components can be compiled ahead of time into native machine code. So JIT isn't the only available execution model.

We'll explore these execution modes later.

## 1.5 What is the .NET SDK?

Imagine you want to create and execute a Java application.

You need tools such as:

```
javac Main.java
java Main
```

You also use Maven or Gradle to build projects, resolve dependencies, and package applications.

The equivalent development toolset in .NET is the .NET SDK.

It provides:

- C# compiler.
- Project build system integration.
- .NET CLI.
- Project templates.
- Package restoration.
- Publishing functionality.
- Other development tooling.

You interact with it primarily through the `dotnet` command.

For example:

```
dotnet --version
```

Displays the selected SDK version.

```
dotnet --info
```

Displays information about installed SDKs, runtimes, and the environment.

Create a console application:

```
dotnet new console -n LearningDotNet
```

Move into the project:

```
cd LearningDotNet
```

Run it:

```
dotnet run
```

Build it:

```
dotnet build
```

You'll gradually become comfortable with these commands.

Important distinction: SDK vs Runtime

The SDK is for development and building applications.

The Runtime is what is needed to execute framework-dependent applications.

A production machine doesn't necessarily need the full SDK installed. It can run an application using the appropriate .NET runtime, or a self-contained deployment can include the required runtime components.

## 1.6 Understanding .NET Framework vs modern .NET

You might encounter these names in a client project:

- .NET Framework
- .NET Core
- .NET 5
- .NET 6
- .NET 8
- .NET 9
- .NET 10
- .NET 11

Are these different programming languages?

No. These refer to different generations or versions of the .NET platform.

Historically:

.NET Framework

The original .NET implementation, primarily tied to Windows.

.NET Core

A redesigned, cross-platform implementation of .NET. It supported Windows, Linux, and macOS.

Modern .NET

Starting with .NET 5, Microsoft unified the platform under the name `.NET`.

The naming evolution:

```
.NET Framework
      |
      | Separate implementation
      |
.NET Core 1.0
.NET Core 2.0
.NET Core 3.1
      |
      | Unified platform
      |
.NET 5
.NET 6
.NET 7
.NET 8
.NET 9
.NET 10
.NET 11
```

There are differences in APIs, compatibility, and deployment models across generations. Don't assume that every old .NET Framework application can be moved to modern .NET without changes.

Which version should we learn?

As of November 11, 2026:

- .NET 11 is the latest release, released November 10, 2026.
- .NET 10 is an LTS release.
- Both are currently supported.&#x20;

  [image](https://www.google.com/s2/favicons?domain=https://dotnet.microsoft.com\&sz=32)

  .NET+2



For our learning, we'll focus on modern C# and modern .NET concepts that apply across supported versions.

For your actual client project, we'll follow the version specified in its `.csproj` file rather than assuming it uses the latest release.

## 1.7 What does a typical .NET backend application look like?

Let's compare the ecosystems you already know.

In Java, a typical backend might look like:

```
Spring Boot Application
    |
    |-- Controllers
    |-- Services
    |-- Repositories
    |-- Entities
    |-- Configuration
    |
    |-- Spring Framework
    |-- Hibernate
    |-- JDBC
    |
    |-- JVM
```

In .NET, a similar application might look like:

```
ASP.NET Core Application
    |
    |-- Controllers
    |-- Services
    |-- Repositories
    |-- Models
    |-- Configuration
    |
    |-- ASP.NET Core
    |-- Entity Framework Core
    |-- ADO.NET
    |
    |-- CLR
```

The architecture will feel familiar.

The biggest differences will initially be:

- C# language features and syntax.
- .NET's runtime and standard libraries.
- Dependency injection conventions.
- ASP.NET Core's request-processing pipeline.
- Entity Framework Core's data access model.
- Async programming conventions.

We'll explore these one by one.

## 1.8 Let's run our first C# application

Once you have the .NET SDK installed, execute:

```
dotnet new console -n LearningDotNet
cd LearningDotNet
dotnet run
```

You should see a project created with files similar to:

```
LearningDotNet/
    LearningDotNet.csproj
    Program.cs
    obj/
    bin/
```

Depending on the SDK and project configuration, some files may differ.

Open `Program.cs`.

You'll initially see something similar to:

```
Console.WriteLine("Hello, World!");
```

That's all!

Compare it with Java:

```
public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
    }
}
```

Why doesn't C# require an explicit class and `Main` method here?

Because modern C# supports top-level statements.

The compiler generates the necessary program entry point behind the scenes.

We'll understand the actual program structure, namespaces, classes, and entry points in Chapter 2.

For now, just remember that this is valid C#.

# 3. Important concepts to remember from this chapter

Let's consolidate the mental model.

| Concept               | Remember it as                                             |
| --------------------- | ---------------------------------------------------------- |
| C#                    | Programming language, like Java                            |
| .NET                  | Development platform and ecosystem                         |
| CLR                   | Runtime, similar to JVM                                    |
| IL                    | Intermediate representation, similar to Java bytecode      |
| JIT                   | Converts IL into machine code                              |
| Garbage Collector     | Manages memory reclamation                                 |
| .NET SDK              | Tools needed to develop and build applications             |
| .NET Runtime          | Environment needed to run framework-dependent applications |
| NuGet                 | Dependency/package management ecosystem                    |
| ASP.NET Core          | Backend web framework, similar in purpose to Spring Boot   |
| Entity Framework Core | ORM, similar in purpose to Hibernate                       |

The most important mental model:

```
You write C# code
       |
       v
C# Compiler compiles it
       |
       v
.NET Assembly containing IL
       |
       v
CLR loads and executes it
       |
       v
JIT converts IL into machine code
       |
       v
CPU executes it
```

## 4. Quick knowledge check

Try answering these mentally. You don't need to write code yet.

1. Is C# the same thing as .NET?
2. Is CLR equivalent to JVM?
3. Does the CPU directly understand IL?
4. What is the difference between .NET SDK and .NET Runtime?
5. If C# compiles to IL, when does JIT compilation happen?
6. Does garbage collection automatically close database connections?

We'll revisit these ideas when we explore runtime internals.

Next chapter: Chapter 2 — C# Project Structure and Program Execution

We'll understand how a real C# project is structured, what `.csproj` files do, how `Program.cs` works, what namespaces and `using` statements mean, how classes and entry points work, and how to organize code into multiple files.

Say next when you're ready.
