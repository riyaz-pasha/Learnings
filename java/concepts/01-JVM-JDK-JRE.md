Perfect. We'll go **one topic at a time**, and we won't move forward until the current topic is solid.

We'll start with **Java Fundamentals: JVM, JDK, JRE, compilation and execution**. This is foundational because many later interview questions—memory, GC, class loading, performance—build on it.

# Topic 1 — JVM, JDK, JRE & How Java Works

## 1. First: What actually happens when you run Java code?

Suppose you write:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello");
    }
}
```

You save it as:

```text
Hello.java
```

Then:

```bash
javac Hello.java
```

The Java compiler produces:

```text
Hello.class
```

The `.class` file contains **bytecode**.

Then:

```bash
java Hello
```

The JVM loads and executes that bytecode.

The simplified flow is:

```text
             Hello.java
                 |
                 | javac
                 ↓
             Hello.class
             (Bytecode)
                 |
                 | java Hello
                 ↓
                JVM
                 |
        ┌────────┼─────────┐
        ↓        ↓         ↓
   Class Loader  JVM      Execution
                 Memory     Engine
                            |
                       Interpreter
                            +
                           JIT
                            |
                            ↓
                    Machine Code
```

This is one of the most important diagrams to understand.

---

# 2. What is JVM?

**JVM = Java Virtual Machine**

The JVM is the runtime environment that **executes Java bytecode**.

Important distinction:

> The JVM does **not** directly execute `.java` source code.

It executes:

```text
.class → bytecode
```

The JVM takes bytecode and ultimately executes it as machine instructions on the underlying operating system/CPU.

---

# 3. Why is Java platform independent?

This is a very common interview question.

Suppose you compile:

```java
Hello.java
```

on Windows.

You get:

```text
Hello.class
```

That bytecode isn't Windows-specific Java source code. A JVM implementation on the target platform can execute the bytecode.

So:

```text
Java Source
     ↓
   javac
     ↓
 Bytecode
     ↓
 ┌─────────────┬─────────────┬─────────────┐
 ↓             ↓             ↓
Windows JVM   Linux JVM    macOS JVM
 ↓             ↓             ↓
Machine code  Machine code  Machine code
```

Hence the famous idea:

> **Write once, run anywhere.**

But there's an important nuance:

### Java is platform-independent; JVM implementations are platform-dependent.

For example:

```text
Windows → Windows JVM
Linux   → Linux JVM
macOS   → macOS JVM
```

The bytecode can remain the same, while the JVM implementation differs.

### Interview answer

If asked:

> Why is Java platform independent?

A strong answer is:

> Java source code is compiled into platform-independent bytecode. That bytecode is executed by a JVM implementation specific to the underlying operating system and hardware. Therefore the same compiled bytecode can run on different platforms as long as an appropriate JVM is available.

That's much better than simply saying:

> "Because of JVM."

---

# 4. JDK vs JRE vs JVM

This is another **very frequently asked** question.

Think of them as:

```text
JDK
 ├── Development tools
 └── JRE
      └── JVM
```

Conceptually:

```text
JDK
│
├── Compiler (javac)
├── Debugging/development tools
├── Other tools
│
└── Runtime environment
     │
     ├── Java libraries
     │
     └── JVM
```

### JVM

Responsible primarily for:

> Running Java bytecode.

### JRE

Historically:

> JVM + Java runtime libraries/components needed to run Java applications.

### JDK

> Tools needed to develop Java applications + runtime components.

For example:

```text
javac
```

is a development tool provided with the JDK.

You need a compiler to compile:

```text
.java → .class
```

You need the JVM to execute:

```text
.class → execution
```

---

# 5. A common interview trap

Interviewer:

> Is JDK platform independent?

Be careful.

The **Java bytecode** is designed to be platform-independent.

But the **JDK/JVM implementation** you install is platform-specific.

For example, you download an appropriate JDK distribution for your OS/architecture.

---

# 6. What is Bytecode?

Consider:

```java
int a = 10;
int b = 20;
int c = a + b;
```

The compiler converts your source code into JVM bytecode.

You can inspect it with:

```bash
javap -c Hello
```

You might see instructions conceptually like:

```text
iload
istore
iadd
return
```

These aren't native CPU instructions.

They're **JVM instructions**.

That's why the JVM can take the same bytecode and execute it on different operating systems.

---

# 7. What does the JVM contain?

At a high level, think about the JVM in these major parts:

```text
JVM
│
├── Class Loader Subsystem
│
├── Runtime Data Areas
│   ├── Heap
│   ├── Stack
│   ├── PC Register
│   └── Method Area / Metaspace
│
└── Execution Engine
    ├── Interpreter
    ├── JIT Compiler
    └── Garbage Collector
```

Don't worry about mastering every component yet.

We'll cover **JVM memory and GC separately**, because they're substantial interview topics.

For now, understand what each broadly does.

---

# 8. Class Loader

Before the JVM can execute a class, it needs to **load the class**.

For example:

```java
Hello
```

When the JVM needs `Hello`, the class-loading mechanism finds and loads the class definition.

Conceptually:

```text
Hello.class
    ↓
Class Loader
    ↓
Loaded class
    ↓
JVM runtime
```

Class loading is more complicated than simply "reading the `.class` file."

It involves stages such as:

```text
Loading
   ↓
Linking
   ├── Verification
   ├── Preparation
   └── Resolution
   ↓
Initialization
```

This becomes important later when we discuss:

* static variables
* static blocks
* class initialization
* class loaders
* reflection
* Spring

---

# 9. Execution Engine

Once classes are loaded, the JVM needs to execute the bytecode.

Two important mechanisms to understand are:

### Interpreter

The interpreter executes bytecode instructions.

Conceptually:

```text
Bytecode
   ↓
Interpreter
   ↓
Machine instructions
```

The problem is that repeatedly interpreting frequently executed code can be slower.

That's where JIT comes in.

---

# 10. JIT Compiler

**JIT = Just-In-Time compiler**

The JVM can identify frequently executed code and compile it into **native machine code**.

Conceptually:

```text
Bytecode
   ↓
Interpreter
   ↓
Frequently executed code
   ↓
JIT Compiler
   ↓
Native machine code
```

This is one reason Java applications can achieve good runtime performance.

The important interview distinction:

> `javac` compiles Java source code into bytecode.

Whereas:

> JIT compiles suitable bytecode into native machine code at runtime.

Don't confuse the two.

---

# 11. `javac` vs JIT

This is a great interview follow-up.

### `javac`

```text
.java
  ↓
javac
  ↓
.class / bytecode
```

This happens during compilation.

### JIT

```text
bytecode
   ↓
JIT
   ↓
native machine code
```

This happens during JVM execution.

So:

```text
Source code
     ↓
   javac
     ↓
  Bytecode
     ↓
    JVM
     ↓
 Interpreter / JIT
     ↓
Machine code
```

---

# 12. JVM vs JDK vs JRE — interview version

If the interviewer asks:

> Explain JVM, JRE and JDK.

You can answer:

> **JVM** is the virtual machine that executes Java bytecode.
> **JRE** traditionally refers to the runtime environment consisting of the JVM and the libraries/components required to run Java applications.
> **JDK** is the development kit containing tools such as the Java compiler along with the runtime components needed to run Java applications.

Then add:

> In modern Java distributions, the old standalone JRE packaging is no longer generally distributed the way it was in older Java versions, but the JDK/JRE/JVM conceptual distinction is still useful.

That last point demonstrates modern Java awareness.

---

# 13. Very common interview questions from this topic

You should be able to answer these:

### Basic

1. What is JVM?
2. What is JDK?
3. What is JRE?
4. Difference between JDK, JRE and JVM?
5. Why is Java platform independent?
6. What is bytecode?
7. What is `.class`?
8. What does `javac` do?
9. What does `java` command do?

### Intermediate

10. Is JVM platform independent?
11. Why does Java need JVM?
12. What is JIT?
13. Interpreter vs JIT?
14. `javac` vs JIT?
15. What happens when you execute a Java program?
16. What is ClassLoader?
17. What happens during class loading?

### Follow-up questions

18. Where are objects stored?
19. Where are local variables stored?
20. What is heap?
21. What is stack?
22. What is Metaspace?
23. How does garbage collection work?
24. When does a class get initialized?
25. What happens with static variables/static blocks?

We'll tackle **18–25 in the JVM Memory/GC topic**, so don't worry if those aren't fully clear yet.

---

# 14. One interview scenario

Imagine the interviewer says:

> "You developed your application on Windows and compiled it there. How can the same application run on Linux?"

Your reasoning should be:

```text
Windows
   ↓
javac
   ↓
Java bytecode
   ↓
.class file
   ↓
copied to Linux
   ↓
Linux JVM
   ↓
execution
```

The key is that **the JVM abstracts the underlying platform from the Java bytecode**.

---

# 15. One subtle point: "Java is compiled or interpreted?"

This is a classic interview question.

Don't answer simply:

> "Java is compiled."

And don't answer:

> "Java is interpreted."

The better answer:

> Java source code is compiled into bytecode by `javac`. The JVM then executes that bytecode, using interpretation and, where appropriate, JIT compilation into native machine code.

That's the answer I'd use in an interview.

---

# 16. What you should remember from Topic 1

If you remember only this diagram, you're already on the right track:

```text
             SOURCE CODE
              Hello.java
                  │
                  │ javac
                  ▼
              BYTECODE
              Hello.class
                  │
                  │
                  ▼
                 JVM
        ┌─────────┴─────────┐
        │                   │
   Class Loading       Execution Engine
                            │
                    ┌───────┴───────┐
                    │               │
               Interpreter         JIT
                    │               │
                    └───────┬───────┘
                            ▼
                     MACHINE CODE
                            │
                            ▼
                           CPU
```

And:

```text
JDK
 └── Runtime
      └── JVM
```

with the modern caveat that **JRE as a separately packaged product is largely a legacy concept**.

---

## Your turn — interview mode

Before we move to **Topic 2 (OOP)**, let's test this topic.

Answer these **without looking back**:

**Q1.** What is the difference between JDK, JRE and JVM?

**Q2.** Why is Java called platform-independent?

**Q3.** What is bytecode?

**Q4.** What is the difference between `javac` and JIT?

**Q5.** Is Java compiled or interpreted?

I'll review your answers like an interviewer, correct anything that's slightly off, and then give you a few **EPAM-style follow-up questions** before we move on.

---

