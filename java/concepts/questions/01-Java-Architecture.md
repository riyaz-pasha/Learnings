# Concept 1: How Java Runs (JDK, JRE, JVM)

I'll go one concept at a time. This first one is the base for everything else. Questions go from beginner to advanced.

---

## Beginner Level

### Q1. What is the difference between JDK, JRE, and JVM?

**Short answer:** JDK is for building Java programs. JRE is for running them. JVM is the engine that actually executes them.

**Deeper explanation:** Think of them as boxes inside boxes.

```
+---------------------------------------+
| JDK (Java Development Kit)            |
|  - javac (compiler), jar, jdb, etc.   |
|  +---------------------------------+  |
|  | JRE (Java Runtime Environment)  |  |
|  |  - Core libraries (java.lang..) |  |
|  |  +---------------------------+  |  |
|  |  | JVM                       |  |  |
|  |  |  - Class loader           |  |  |
|  |  |  - Memory areas           |  |  |
|  |  |  - Execution engine       |  |  |
|  |  +---------------------------+  |  |
|  +---------------------------------+  |
+---------------------------------------+
```

- **JVM** reads bytecode and runs it on your machine. It does not understand `.java` files, only `.class` files.
- **JRE** = JVM + the standard libraries (like `String`, `ArrayList`, `File`). Your program needs these libraries to work.
- **JDK** = JRE + developer tools. `javac` is the most important one. It turns your code into bytecode.

**Small note for interviews:** Since Java 11, Oracle no longer ships a separate JRE download. You just install a JDK. Tools like `jlink` can also build a small custom runtime with only the modules your app needs. Mentioning this shows you know current Java.

---

### Q2. What does "Write Once, Run Anywhere" mean? How does Java achieve it?

**Short answer:** You compile your code once, and the same compiled file runs on Windows, Linux, or Mac.

**Deeper explanation:** In C or C++, the compiler turns your code into machine code for one specific CPU and OS. Move to another system, and you must compile again.

Java adds a middle step:

```
Hello.java  --javac-->  Hello.class (bytecode)  --JVM-->  machine code
```

Bytecode is not tied to any CPU. It is a set of simple instructions for an imaginary machine. Each OS has its own JVM that knows how to turn that bytecode into real machine code for that system.

So the **bytecode is portable, but the JVM is not**. Each platform needs its own JVM. This is the part people often get wrong. Java itself isn't platform-independent. The bytecode is.

---

### Q3. What is bytecode?

**Short answer:** Bytecode is the intermediate code that `javac` produces. It is stored in `.class` files.

**Example:**

```java
public class Add {
    public static int add(int a, int b) {
        return a + b;
    }
}
```

After compiling, run `javap -c Add` and you'll see something like:

```
iload_0
iload_1
iadd
ireturn
```

This means: load `a`, load `b`, add them, return the result. Each instruction is one byte (that's why it's called *byte*code). The JVM is a **stack-based machine**, so it pushes values on a stack and works on them.

---

### Q4. What is the difference between a compiler and an interpreter? Which one does Java use?

**Short answer:** Java uses both.

**Deeper explanation:**
- A **compiler** translates the whole program before running it. It's fast at runtime but slow to start.
- An **interpreter** translates and runs one line at a time. It starts fast but runs slower.

Java does two things:
1. `javac` **compiles** source code to bytecode (compile time).
2. The JVM **interprets** bytecode at first, and then **compiles hot parts to machine code** using the JIT compiler (runtime).

That's why Java is neither purely compiled nor purely interpreted.

---

## Intermediate Level

### Q5. What are the main components of the JVM?

**Short answer:** Class Loader Subsystem, Runtime Data Areas (memory), and Execution Engine.

**Deeper explanation:**

```
        .class files
             |
             v
   +--------------------+
   |  Class Loader      |  -> loads, links, initializes classes
   +--------------------+
             |
             v
   +--------------------+
   | Runtime Data Areas |  -> Heap, Stack, Metaspace, PC register,
   |                    |     Native method stack
   +--------------------+
             |
             v
   +--------------------+
   | Execution Engine   |  -> Interpreter, JIT compiler, Garbage Collector
   +--------------------+
             |
             v
   Native Method Interface -> talks to C/C++ libraries
```

- **Class Loader:** brings classes into memory when needed.
- **Runtime Data Areas:** where the JVM stores things while your program runs.
- **Execution Engine:** actually runs the code and cleans up memory.

We will go deep into each of these in the next concepts.

---

### Q6. What happens step by step when you run `java Hello`?

**Short answer:** The JVM starts, loads the class, verifies it, prepares it, runs its static initializers, and calls `main`.

**Deeper explanation:**

1. The OS starts the JVM process.
2. The JVM creates the **Bootstrap Class Loader** and loads core classes like `Object` and `String`.
3. The **Application Class Loader** finds and loads `Hello.class`.
4. **Linking** happens:
   - *Verify:* checks the bytecode is safe and valid.
   - *Prepare:* allocates memory for static variables and sets default values (0, null, false).
   - *Resolve:* turns symbolic references into direct references.
5. **Initialization:** static variables get their real values and `static {}` blocks run.
6. The JVM creates the **main thread** and calls `public static void main(String[] args)`.
7. When `main` ends and no non-daemon threads are left, the JVM shuts down.

**Interview tip:** Many people forget that static variables get default values first (in Prepare), and real values only later (in Initialization).

---

### Q7. What is JIT compilation? Why is it needed?

**Short answer:** JIT (Just-In-Time) compiler converts frequently used bytecode into native machine code while the program is running, to make it faster.

**Deeper explanation:** Interpreting bytecode every time is slow. But compiling everything upfront wastes time on code that runs only once.

So the JVM watches your program. It counts how many times each method or loop runs. When something becomes "hot" (runs many times), the JIT compiles it into machine code and saves it. Next time, the JVM uses the fast native version directly.

**Example:** A loop that runs 1 million times gets compiled quickly. A method called only at startup stays interpreted.

This is why Java programs are often **slow at first and faster after some time**. This is called **warm-up**. It matters a lot in benchmarking. If you measure a Java method once, your result is wrong. Tools like JMH exist for this reason.

---

### Q8. What are C1 and C2 compilers? What is tiered compilation?

**Short answer:** C1 is a fast, light compiler. C2 is a slow, heavy compiler that produces highly optimized code. Tiered compilation uses both.

**Deeper explanation:**
- **C1 (Client compiler):** compiles quickly with basic optimizations. Good for fast startup.
- **C2 (Server compiler):** takes more time but applies deep optimizations like inlining and escape analysis. Good for long-running apps.

**Tiered compilation** (default since Java 8) works like this:

```
Interpreter -> C1 (with profiling) -> C2 (fully optimized)
```

Code starts in the interpreter. If it gets warm, C1 compiles it. If it gets very hot, C2 recompiles it with better optimizations using the profile data collected earlier. You get fast startup **and** high peak speed.

---

## Advanced Level

### Q9. What are some optimizations the JIT compiler does?

**Short answer:** Method inlining, escape analysis, loop unrolling, dead code removal, and speculative optimization.

**Deeper explanation:**
- **Inlining:** replaces a method call with the method's body. This removes call overhead and opens more optimization chances. Tiny getters basically become free.
- **Escape analysis:** checks if an object is used only inside one method. If yes, the JIT may skip heap allocation completely (called *scalar replacement*), so there's no garbage to collect.
- **Loop unrolling:** repeats the loop body several times per iteration to reduce loop checks.
- **Dead code elimination:** removes code that has no effect.
- **Speculative optimization:** the JIT guesses based on what it saw so far. For example, if an interface method has always been called on one class, it treats it like a direct call. If the guess turns out wrong, the JVM does a **deoptimization**. It throws away the compiled code and goes back to the interpreter.

That last point is important. It shows the JIT is smart but also careful.

---

### Q10. What is AOT compilation? How is it different from JIT? (GraalVM Native Image)

**Short answer:** AOT (Ahead-Of-Time) compiles Java to native machine code **before** running, so there's no JVM warm-up.

**Deeper explanation:**

| | JIT | AOT (Native Image) |
|---|---|---|
| When compiled | At runtime | At build time |
| Startup | Slower | Very fast (milliseconds) |
| Memory | Higher | Lower |
| Peak speed | Often higher (uses runtime profile) | Usually a bit lower |
| Dynamic features (reflection, dynamic proxies) | Fully supported | Need extra configuration |

AOT is great for serverless functions and microservices where startup time and memory cost matter. JIT is usually better for long-running, high-throughput services.

---

### Q11. Why is the JVM called a "stack-based" machine? What is the alternative?

**Short answer:** Because bytecode instructions work on an operand stack, not on named CPU registers.

**Deeper explanation:** For `a + b`, bytecode pushes `a`, pushes `b`, then `iadd` pops both and pushes the result. No register names appear anywhere.

**Why this design?** Stack-based code is simple, compact, and does not depend on how many registers a CPU has. That helps portability. Real CPUs (and Android's older Dalvik VM) use **register-based** designs, which need fewer instructions but produce larger ones.

The JIT later maps this stack code to real registers when generating native code.

---

### Q12. Is the JVM only for Java? What are its implications?

**Short answer:** No. The JVM runs bytecode, and any language that compiles to bytecode can run on it.

**Deeper explanation:** Kotlin, Scala, Groovy, and Clojure all compile to `.class` files. The JVM does not care where the bytecode came from. This is why the JVM is called a **platform** and not just a "Java runtime". It also means all these languages share the same garbage collector, JIT, and libraries.

---

### Q13. (Tricky) If Java is "compiled," why does a Java app get faster the longer it runs?

**Short answer:** Because the JVM is learning while running.

**Deeper explanation:** In the first few seconds, most code runs in the interpreter. The JVM profiles it: which branches are taken, which types appear, which methods are hot. Then C1 and C2 use this information to generate very well-tuned machine code. A traditional C++ compiler cannot see runtime behavior, so it must guess. This is called **profile-guided optimization**, and the JVM does it automatically. That is also why some Java code can beat C++ in long-running tests, and also why short scripts feel slow in Java.

---

## Quick Recap

- **JDK** ⊃ **JRE** ⊃ **JVM**
- Bytecode is portable, the JVM is platform-specific.
- Java is both compiled (`javac`) and interpreted/JIT-compiled (JVM).
- JIT uses tiered compilation: Interpreter → C1 → C2.
- AOT trades peak speed for fast startup.

---

**Next concept:** *Class Loaders* (Bootstrap, Platform, Application, delegation model, custom class loaders, and common errors like `ClassNotFoundException` vs `NoClassDefFoundError`).

Say **"next"** when you're ready, or ask me to go deeper on anything here.

---
---
---

# Concept 2: Class Loaders

Last time we saw that the JVM loads `.class` files. Now let's see **who** loads them, **when**, and **how**.

---

## Beginner Level

### Q1. What is a class loader?

**Short answer:** A class loader is a part of the JVM that finds `.class` files (or JAR files) and loads them into memory as classes.

**Deeper explanation:** The JVM does not load all classes when it starts. That would be slow and wasteful. It loads a class **only when it is first needed**. This is called **lazy loading**.

**Example:**

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Start");
        Helper h = new Helper();   // Helper is loaded HERE, not at startup
    }
}
```

If `Helper` was never used, it would never be loaded. Class loaders do this job quietly in the background.

---

### Q2. When does a class get loaded?

**Short answer:** When the program uses it for the first time.

Common triggers:
- Creating an object with `new`
- Calling a static method or accessing a static field (not a compile-time constant)
- Using `Class.forName("...")`
- Loading a subclass. The parent class must load first.

**Small trap:** Just declaring a variable like `Helper h;` does **not** load the class. Actually using it does.

---

### Q3. What are the built-in class loaders in Java?

**Short answer:** Three: Bootstrap, Platform, and Application.

```
        Bootstrap Class Loader     (written in native code)
                  ^
                  | parent
        Platform Class Loader      (formerly "Extension")
                  ^
                  | parent
        Application Class Loader   (also called System class loader)
                  ^
                  | parent
        Your custom class loaders  (optional)
```

- **Bootstrap:** loads the core Java classes like `Object`, `String`, and `System`. It's part of the JVM itself, mostly written in C/C++.
- **Platform:** loads some standard modules that are not part of the very core. For example, `java.sql`. In Java 8 and before, this was called the **Extension** class loader and loaded JARs from `lib/ext`.
- **Application:** loads **your** classes and libraries from the classpath (or module path).

---

### Q4. What does `getClassLoader()` return for `String.class`?

**Short answer:** `null`.

```java
System.out.println(String.class.getClassLoader());    // null
System.out.println(Main.class.getClassLoader());      // jdk.internal.loader.ClassLoaders$AppClassLoader@...
```

**Why?** The Bootstrap loader is not a Java object. It is native code inside the JVM. So Java has no object to give you, and it returns `null`. This is a common interview question. `null` here does **not** mean "no loader". It means "the Bootstrap loader".

---

## Intermediate Level

### Q5. What is the Parent Delegation Model?

**Short answer:** Before a class loader tries to load a class itself, it first asks its **parent** to try.

**Deeper explanation:** When the Application loader gets a request to load `java.lang.String`, it does this:

```
Application loader: "Can my parent load it?"
   -> Platform loader: "Can my parent load it?"
        -> Bootstrap loader: "Yes, I have it." (done)
```

Only if all parents say "I can't find it" does the child try to load it itself.

**Why is this good? Two big reasons:**

1. **Security.** Imagine someone writes their own `java.lang.String` class with bad code and puts it in the classpath. Because of delegation, the Bootstrap loader always loads the real `String` first. The fake one is never used.
2. **No duplicates.** A class is loaded once by the top-most loader that can find it, so the same core class isn't loaded many times.

---

### Q6. What are the three phases of class loading?

**Short answer:** Loading, Linking, and Initialization.

1. **Loading:** find the bytecode and create a `Class` object in memory.
2. **Linking:**
   - **Verification:** checks the bytecode is valid and safe (correct format, no illegal stack operations).
   - **Preparation:** allocates memory for static fields and gives them **default values** (0, false, null).
   - **Resolution:** replaces symbolic names with direct memory references (this can happen lazily).
3. **Initialization:** runs static initializers and static blocks, and sets static fields to their **real values**.

**Example:**

```java
class Config {
    static int port = 8080;
    static { System.out.println("Config loaded"); }
}
```

- After **Preparation**, `port` is `0`.
- After **Initialization**, `port` is `8080` and the message prints.

---

### Q7. What is the difference between `ClassNotFoundException` and `NoClassDefFoundError`?

This one is asked in almost every interview.

| | `ClassNotFoundException` | `NoClassDefFoundError` |
|---|---|---|
| Type | Checked **Exception** | **Error** |
| Cause | You **asked by name** at runtime (`Class.forName`, `loadClass`) and the class was not found | The class **existed at compile time**, but is missing (or failed to load) at runtime |
| When | Explicit dynamic loading | Normal usage like `new Foo()` |

**Example of the first:**

```java
Class.forName("com.mysql.Driver");   // wrong or missing JAR -> ClassNotFoundException
```

**Example of the second:** You compile code that uses `Foo`. Then you deploy the app but forget to include `Foo.class`. At runtime, `new Foo()` gives `NoClassDefFoundError`.

**A tricky case:** If a class's `static {}` block throws an exception, you first get `ExceptionInInitializerError`. After that, every later use of that class gives `NoClassDefFoundError`, even though the file is there. The JVM has marked the class as failed. So the file being present doesn't guarantee this error is about a missing file. Always read the earlier stack trace.

---

### Q8. What is the difference between `Class.forName()` and `ClassLoader.loadClass()`?

**Short answer:** `Class.forName()` loads **and initializes** the class. `loadClass()` only loads it. It does not initialize.

```java
Class.forName("Config");                    // "Config loaded" is printed (static block runs)
ClassLoader.getSystemClassLoader().loadClass("Config");   // nothing printed yet
```

**Why does this matter?** Old JDBC code used `Class.forName("com.mysql.jdbc.Driver")` on purpose. The driver's static block registers itself with `DriverManager`. So initialization was the whole point.

---

## Advanced Level

### Q9. How does a class get its identity in the JVM?

**Short answer:** A class is identified by its **full name + the class loader that loaded it**. Not just the name.

**Deeper explanation:** If two different class loaders load the same `com.app.User` class from the same file, the JVM treats them as **two different classes**.

```java
Object u1 = loaderA.loadClass("com.app.User").getDeclaredConstructor().newInstance();
User u2 = (User) u1;   // ClassCastException!
```

The message will look strange: `com.app.User cannot be cast to com.app.User`. This confuses many developers. It usually happens in app servers, plugin systems, and hot-reload tools. The fix is to make sure both sides use the same loader for that class.

---

### Q10. How do you write a custom class loader? Why would you?

**Short answer:** Extend `ClassLoader` and override `findClass()`. Do **not** override `loadClass()` unless you really want to break delegation.

```java
public class MyLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = readBytesFromSomewhere(name); // file, network, database, encrypted file...
        return defineClass(name, bytes, 0, bytes.length);
    }
}
```

**Why does `findClass` work well?** The default `loadClass` already does the parent-first check. It calls `findClass` only if the parents fail. So you keep the safe delegation for free.

**Real reasons to write one:**
- Load classes from a **network or database** instead of files
- **Hot reloading** (replace a class without restarting)
- **Plugin systems** where each plugin is isolated
- **Encrypted class files** that are decrypted while loading
- **Isolation** so different modules can use different versions of the same library

---

### Q9b. How do web servers like Tomcat use class loaders?

**Short answer:** Each web application gets its **own class loader**, so apps do not clash with each other.

```
       Bootstrap
          |
     Platform / App
          |
     Tomcat common loader (Tomcat's own libs)
        /            \
  WebApp1 loader    WebApp2 loader
```

- App 1 can use `library-v1.jar` and App 2 can use `library-v2.jar` in the same JVM without any conflict.
- When you **redeploy** an app, Tomcat drops the old loader and creates a new one. That's how it reloads.

**Interesting point:** Tomcat's web app loader **breaks** the parent-first rule a bit. It checks the web app's own classes first (for non-core classes). This lets an app use its own library version even if the server has a different one.

---

### Q10b. Where does Java break the parent delegation model, and why?

**Short answer:** When a parent-loaded class needs to load something that only a child can see.

**Example: JDBC.** `DriverManager` is a core class loaded by the Bootstrap/Platform loader. But the actual JDBC driver (MySQL, Postgres) sits on your classpath, which only the Application loader can see. A parent cannot look down at its child.

**The solution: the Thread Context Class Loader (TCCL).**

```java
Thread.currentThread().getContextClassLoader();
```

Core code like `ServiceLoader` (used by JDBC, JNDI, and others) uses this loader to "reach down" and find classes on the application classpath. It's a controlled way of bending the rule.

---

### Q11. Can a class be unloaded? What is a class loader memory leak?

**Short answer:** A class can be unloaded only when its **class loader** becomes unreachable and is garbage collected. Never a single class alone.

**Deeper explanation:** The Bootstrap, Platform, and Application loaders live for the whole app. So their classes are basically never unloaded. But custom loaders (like Tomcat's per-app loader) can be collected, and all their classes go away with them, freeing **Metaspace**.

**The leak:** If anything still holds a reference to **any** class, object, or thread from that loader, the whole loader stays alive. Classic causes:
- A static reference in a shared library pointing to an app class
- A thread started by the app that is never stopped
- `ThreadLocal` values left behind
- JDBC drivers still registered in `DriverManager`

After several redeploys, Metaspace fills up and you get `OutOfMemoryError: Metaspace`. This is a well-known problem on servers that redeploy often.

---

### Q12. How did Java 9 modules change class loading?

**Short answer:** The three loaders stay, but they now work with **modules**, and the old `lib/ext` and `-Xbootclasspath` setups are gone or changed.

- Core modules (`java.base`, etc.) are loaded by Bootstrap. Some others (like `java.sql`) by Platform. Your app by Application.
- Modules give **strong encapsulation**. A package can be hidden unless exported. Deep reflection into JDK internals now needs `--add-opens`.
- The extension mechanism (`lib/ext`) was removed.

This is why old code that reflects into `sun.misc` or `java.lang` internals started failing on newer Java versions.

---

## Quick Recap

- Class loaders load classes **lazily**, when first needed.
- Three built-in loaders: **Bootstrap → Platform → Application**.
- **Parent delegation** gives security and avoids duplicates.
- Phases: **Loading → Linking (Verify, Prepare, Resolve) → Initialization**.
- `ClassNotFoundException` = asked by name, not found. `NoClassDefFoundError` = was there at compile time, missing or failed at runtime.
- Class identity = **name + loader**.
- Custom loaders: override `findClass`. Used in app servers, plugins, and hot reload.
- Leaked class loaders cause Metaspace `OutOfMemoryError`.

---

**Next concept:** *JVM Memory Model* (Heap, Stack, Metaspace, PC register, Young/Old generation, and what lives where).

Say **"next"** when you're ready.

---
---
---

# Concept 3: JVM Memory Structure

When your program runs, the JVM needs places to keep classes, objects, method calls, and variables. This concept is about those places. Almost every Java interview has questions on it.

---

## Beginner Level

### Q1. What are the main memory areas of the JVM?

**Short answer:** Heap, Stack, Metaspace, PC Register, and Native Method Stack.

**Deeper explanation:** Some areas are shared by all threads. Some are created separately for each thread.

```
+------------------- Shared by all threads -------------------+
|   HEAP                        METASPACE (native memory)     |
|   (all objects)               (class metadata, methods)     |
+-------------------------------------------------------------+

+---- Thread 1 ----+   +---- Thread 2 ----+   +---- Thread 3 ----+
| Stack            |   | Stack            |   | Stack            |
| PC Register      |   | PC Register      |   | PC Register      |
| Native Stack     |   | Native Stack     |   | Native Stack     |
+------------------+   +------------------+   +------------------+
```

- **Heap:** where all objects live. Shared.
- **Metaspace:** where class information lives. Shared.
- **Stack:** where method calls and local variables live. One per thread.
- **PC Register:** remembers which instruction the thread is running now. One per thread.
- **Native Method Stack:** used when Java calls native (C/C++) code. One per thread.

**Why this split matters:** Shared areas need care with multithreading and need garbage collection. Per-thread areas are private, so they are naturally thread-safe.

---

### Q2. What is the difference between Stack and Heap?

| | Stack | Heap |
|---|---|---|
| Stores | Method frames, local variables, references | Objects and arrays |
| Owned by | One thread | All threads (shared) |
| Cleanup | Automatic when method returns | Garbage Collector |
| Speed | Very fast | Slower |
| Size | Small | Large |
| Error when full | `StackOverflowError` | `OutOfMemoryError` |

**Example:**

```java
void test() {
    int x = 10;                 // x is on the stack
    Person p = new Person();    // p (the reference) is on the stack
                                // the Person object is on the heap
}
```

```
   STACK                    HEAP
+---------+            +--------------+
| x = 10  |            |   Person     |
| p ------|----------> |   object     |
+---------+            +--------------+
```

When `test()` ends, its stack frame disappears. `x` and `p` are gone. The `Person` object stays on the heap until the GC finds that nobody points to it.

**Common mistake:** People say "primitives go on the stack and objects go on the heap." This is only true for *local* variables. A primitive `int` that is a field inside an object lives **inside that object on the heap**.

---

### Q3. What is a stack frame? What is inside it?

**Short answer:** Every method call creates one frame on the thread's stack. It is removed when the method ends.

Each frame has:
- **Local variable array:** method parameters and local variables
- **Operand stack:** the work area for calculations (remember the stack-based machine from Concept 1)
- **Frame data:** a link to the class's constant pool, and info about returning and exceptions

```
main() calls a() calls b()

| b() frame |  <- top (currently running)
| a() frame |
| main()    |  <- bottom
```

When `b()` finishes, its frame is popped, and `a()` continues.

---

### Q4. What is the PC Register?

**Short answer:** It stores the address of the bytecode instruction that the thread is currently running.

**Why is it needed?** The JVM runs many threads, and the CPU switches between them. When a thread comes back after a pause, it must know where to continue. The PC register holds that place. Each thread needs its own.

---

### Q5. What is the difference between `StackOverflowError` and `OutOfMemoryError`?

**`StackOverflowError`:** a thread's stack is full. Usually caused by **infinite or too deep recursion**.

```java
void recurse() { recurse(); }   // every call adds a frame, never returns
```

**`OutOfMemoryError`:** the JVM cannot get more memory for something. It has several types:
- `Java heap space`: heap is full
- `Metaspace`: too many classes loaded
- `GC overhead limit exceeded`: GC is working nonstop but freeing almost nothing
- `unable to create native thread`: OS refused to make another thread
- `Direct buffer memory`: off-heap memory is full

**Interview tip:** Always ask "which OOM message?" because each has a different cause and a different fix.

---

## Intermediate Level

### Q6. How is the Heap divided?

**Short answer:** Into the Young Generation and the Old Generation.

```
+------------------------ HEAP ------------------------+
|  YOUNG GENERATION                |  OLD GENERATION   |
|  +--------+----------+---------+ |                   |
|  |  Eden  | Survivor | Survivor| |  long-living      |
|  |        |   S0     |   S1    | |  objects          |
|  +--------+----------+---------+ |                   |
+--------------------------------------------------------+
```

**Why split?** Because of an observation called the **generational hypothesis**: *most objects die young.* Think of temporary strings, loop variables, and request objects. So the JVM keeps new objects in a small area and cleans it often and cheaply. Objects that survive many cleanups are moved to a bigger area that is cleaned rarely.

**Life of an object:**
1. A new object is created in **Eden**.
2. When Eden fills up, a **Minor GC** runs. Live objects are copied to a Survivor space. Dead ones are simply left behind.
3. Each time an object survives a Minor GC, its **age** goes up by 1. It moves between S0 and S1.
4. When its age reaches a limit (max 15), it is **promoted** to the Old Generation.
5. When the Old Generation fills up, a bigger, slower collection is needed (Major or Full GC).

**Note:** Newer collectors like G1, ZGC, and Shenandoah use *regions* instead of fixed-size areas. The idea of young and old is still similar in G1, but the layout is different. We'll cover this in the GC concept.

---

### Q7. What is Metaspace? How is it different from PermGen?

**Short answer:** Metaspace stores **class metadata**: class structure, method info, the runtime constant pool, and so on. It replaced PermGen in Java 8.

| | PermGen (Java 7 and before) | Metaspace (Java 8+) |
|---|---|---|
| Location | Inside the JVM heap | **Native memory** (outside the heap) |
| Size | Fixed max, easy to run out | Grows by default, limited by OS memory |
| Error | `OutOfMemoryError: PermGen space` | `OutOfMemoryError: Metaspace` |

**Why was it changed?** Sizing PermGen was hard. Too small caused crashes, especially in app servers that load lots of classes. Moving to native memory made it flexible.

**Warning:** "Unlimited" is dangerous. A class loader leak (from the last concept) can eat all the machine's memory. In production, set a limit:

```
-XX:MaxMetaspaceSize=256m
```

---

### Q8. What is the String Pool? Where does it live?

**Short answer:** It is a special area where Java keeps unique string literals so they can be reused. Since Java 7, it lives **in the heap**.

**Example:**

```java
String a = "hello";
String b = "hello";
String c = new String("hello");

System.out.println(a == b);        // true  -> same object from the pool
System.out.println(a == c);        // false -> new String() forces a new object
System.out.println(a == c.intern());  // true -> intern() returns the pooled one
```

```
   STACK          HEAP
  a ------->  [ "hello" ]  (in String Pool)
  b ------->  ^
  c ------->  [ "hello" ]  (separate object)
```

**Why does it exist?** Strings are used everywhere. Reusing them saves memory. It is safe only because Strings are **immutable**. If they could change, one change would affect everyone sharing it.

**Why was it moved to the heap?** In Java 6 it was in PermGen, which was small. Many interned strings caused PermGen errors. In the heap, unused strings can be garbage collected.

**Rule for interviews:** Use `.equals()` to compare string content, never `==`.

---

### Q9. Where are static variables stored?

**Short answer:** This one has two answers, and interviewers like it because of that.

- **Common textbook answer:** in the Method Area / Metaspace, along with class data.
- **Accurate answer for modern HotSpot (Java 8+):** the class *metadata* is in Metaspace, but the actual **static field values live in the heap**, inside the `Class` object (`java.lang.Class`) of that class.

This has a real effect. A static field holding a big collection keeps that collection alive as long as the class is loaded. That is a well-known source of memory leaks.

---

### Q10. Is Java "pass by value" or "pass by reference"?

**Short answer:** Always **pass by value**.

**Deeper explanation:** For objects, what gets copied is the **reference value** (the address), not the object.

```java
void change(Person p) {
    p.name = "Ali";          // works: same object on the heap
    p = new Person();        // only changes the local copy of the reference
}
```

The caller's variable still points to the original object. The first line changes the object both sides can see. The second line only changes the method's own copy of the pointer. This is exactly how stack and heap work together.

---

## Advanced Level

### Q11. How does the JVM allocate objects so quickly? What is a TLAB?

**Short answer:** Each thread gets its own small private slice of Eden called a **Thread-Local Allocation Buffer (TLAB)**. Allocating an object inside it is just moving a pointer forward.

**Deeper explanation:** Many threads create objects at the same time. If they all fought over one shared "next free spot" in Eden, they would need locks, and that would be slow.

With TLABs:
- Each thread allocates inside its own buffer. **No locking.**
- Allocation is a **bump-the-pointer** operation: just move the "next free" pointer by the object's size.
- When the TLAB is full, the thread asks for a new one (this needs some coordination, but happens rarely).

This is why `new` in Java is often as cheap as a few CPU instructions. It's also why short-lived objects are cheap in Java.

---

### Q12. Do all objects always go on the heap?

**Short answer:** Not always. The JIT can remove the allocation.

This connects to **escape analysis** from Concept 1. If the JIT proves an object never escapes the method, it can use **scalar replacement**: it breaks the object into its fields and keeps them in CPU registers or the stack, and never creates the object.

```java
int sum(int a, int b) {
    Point p = new Point(a, b);   // p never leaves this method
    return p.x + p.y;            // JIT may skip creating Point entirely
}
```

So the rule "objects live on the heap" is true by the specification, but in practice the JIT may skip it.

---

### Q13. What is Direct (Off-Heap) Memory?

**Short answer:** Memory allocated outside the Java heap, in native OS memory, and not managed by the garbage collector.

```java
ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 1024);
```

**Why use it?**
- Faster I/O. Data doesn't need to be copied from the heap to a native buffer first.
- Large data does not add GC pressure.
- Used by Netty, Kafka clients, and many databases.

**Risks:**
- Slower to allocate and free.
- Leaks are harder to find, because heap dumps don't show it clearly.
- Limited by `-XX:MaxDirectMemorySize`. If it is exceeded: `OutOfMemoryError: Direct buffer memory`.

**Bigger picture:** JVM total memory is more than `-Xmx`. It is roughly: Heap + Metaspace + Thread stacks + Code cache (JIT output) + Direct memory + GC internal data. This is why a container with a 2 GB limit and `-Xmx2g` gets killed by the OS. Leave headroom.

---

### Q14. What are compressed oops (compressed object pointers)?

**Short answer:** A trick where the JVM uses **32-bit references** instead of 64-bit ones on a 64-bit JVM, to save memory.

**Deeper explanation:** Objects are aligned to 8-byte boundaries, so the lowest 3 bits of any address are always 0. The JVM can drop them and store a smaller number, then shift it back when needed. This lets 32 bits address up to about **32 GB** of heap.

- Heap **below about 32 GB**: references take 4 bytes. This can save a lot of memory, because objects hold many references.
- Heap **above about 32 GB**: the trick stops working and references become 8 bytes.

**Practical result:** A 40 GB heap can hold *less* data than a 30 GB heap in some cases. That's why many teams stay just under 32 GB (or use more JVM instances) instead of going slightly above it.

---

### Q15. What are the important memory flags?

| Flag | Meaning |
|---|---|
| `-Xms` | Initial heap size |
| `-Xmx` | Maximum heap size |
| `-Xss` | Stack size per thread (default is about 1 MB on 64-bit Linux) |
| `-XX:MaxMetaspaceSize` | Limit for Metaspace |
| `-XX:NewRatio` | Old : Young size ratio |
| `-XX:SurvivorRatio` | Eden : Survivor size ratio |
| `-XX:MaxDirectMemorySize` | Limit for off-heap direct buffers |
| `-XX:+HeapDumpOnOutOfMemoryError` | Save a heap dump when heap OOM happens |

**Tip:** Setting `-Xms` equal to `-Xmx` in production avoids heap resizing pauses. In containers, `-XX:MaxRAMPercentage` is often more convenient than a fixed `-Xmx`.

**About `-Xss`:** Making it bigger allows deeper recursion but costs more memory **per thread**. With 1000 threads, a 1 MB stack means 1 GB just for stacks.

---

### Q16. Java has a garbage collector. Can it still have memory leaks?

**Short answer:** Yes. A leak in Java means objects that are **no longer needed but still reachable**, so the GC cannot remove them.

**Common causes:**

1. **Static collections that only grow:**
   ```java
   static Map<String, Object> cache = new HashMap<>();   // never cleared
   ```
2. **Listeners or callbacks** that were registered and never removed.
3. **`ThreadLocal` values** not cleaned up, especially in thread pools where threads live long.
4. **Unclosed resources:** connections, streams, and statements.
5. **Bad `equals`/`hashCode` in map keys.** Entries pile up and can't be found again.
6. **Inner classes** holding a hidden reference to the outer object.
7. **Class loader leaks** (from the last concept).

**How to find them:** Take a heap dump (`jmap`, or the flag above), open it in Eclipse MAT or VisualVM, and look at the "dominator tree" and "path to GC roots" to see *why* an object is still alive.

---

### Q17. Is the "JVM Memory Structure" the same as the "Java Memory Model"?

**Short answer:** No. Different topics with confusingly similar names.

- **JVM Memory Structure** (this concept): *where* data is stored (heap, stack, and so on).
- **Java Memory Model (JMM):** the *rules* about how threads see each other's changes to shared variables (`volatile`, `synchronized`, happens-before).

We will cover the JMM in its own concept later, in the concurrency part.

---

## Quick Recap

- Shared areas: **Heap** and **Metaspace**. Per-thread: **Stack**, **PC Register**, **Native Stack**.
- Stack holds frames and local variables. Heap holds objects.
- Heap = **Young** (Eden + S0 + S1) and **Old**. Most objects die young.
- **Metaspace** (native memory) replaced PermGen in Java 8.
- **String Pool** is in the heap. Use `.equals()`, not `==`.
- Java is always **pass by value**. For objects, the reference is copied.
- **TLAB** makes allocation fast. **Escape analysis** can skip the heap.
- Total JVM memory is more than heap. Leave room for Metaspace, stacks, and direct memory.
- Leaks in Java = unwanted objects that are still reachable.

---

**Next concept:** *Garbage Collection* (how GC finds dead objects, GC roots, Mark-Sweep-Compact, Serial, Parallel, G1, ZGC, Shenandoah, and how to tune and read GC logs).

Say **"next"** when you're ready.

---
---
---

# Concept 4: Garbage Collection (GC)

In the last concept we saw where objects live. Now we see how the JVM cleans them up. This is one of the most asked topics in senior Java interviews.

---

## Beginner Level

### Q1. What is Garbage Collection? Why does Java need it?

**Short answer:** GC is the JVM's automatic process of finding objects that your program can no longer use and freeing their memory.

**Deeper explanation:** In C or C++, you allocate memory with `malloc`/`new` and free it yourself with `free`/`delete`. If you forget, you get a memory leak. If you free twice or use it after freeing, your program crashes or becomes unsafe.

Java removes this burden. You only create objects. The GC decides when an object is no longer needed and frees it.

**Benefits:** fewer memory bugs, safer code, faster development.
**Cost:** the GC uses CPU and can pause your application for some time. Most of GC tuning is about controlling this cost.

---

### Q2. How does the GC decide that an object is garbage?

**Short answer:** If the object is **not reachable** from any GC Root, it is garbage.

**Deeper explanation:** The JVM does not ask "is this object used?" It asks "can I reach this object by following references, starting from some root?"

**GC Roots include:**
- Local variables and parameters in active stack frames
- Static fields of loaded classes
- Active threads
- JNI (native) references

```
GC Root (local var)
    |
    v
  [A] ---> [B] ---> [C]        <- reachable, kept

  [X] ---> [Y]                 <- no path from any root, garbage
   ^        |
   +--------+                  (even a cycle doesn't save them)
```

**Important point:** Java does **not** use reference counting. That's why circular references (X points to Y, Y points to X) are not a problem. If neither can be reached from a root, both are collected.

---

### Q3. Can we force garbage collection?

**Short answer:** No. `System.gc()` is only a **request**. The JVM may ignore it.

**Why avoid it?** It can trigger a full, stop-the-world collection at a bad moment. In production, people often disable it with `-XX:+DisableExplicitGC`. Some libraries that use direct memory call it on purpose, so check before disabling.

**Related:** `finalize()` was deprecated for removal in Java 18. It is unpredictable, slow, and can even bring dead objects back to life. Use `try-with-resources` or `Cleaner` instead.

---

### Q4. What is a "Stop-The-World" (STW) pause?

**Short answer:** A moment when the JVM **pauses all application threads** so the GC can do its work safely.

**Why is it needed?** If your program keeps changing references while the GC is checking them, the GC could make a wrong decision, such as freeing an object that just became reachable again. Pausing gives it a stable picture.

Modern GCs try to keep this pause very short, from a few milliseconds down to under a millisecond, by doing most work while the app is running.

---

## Intermediate Level

### Q5. What are the basic GC algorithms?

**1. Mark-Sweep:**
- *Mark:* start from roots and mark every reachable object.
- *Sweep:* free everything that is not marked.
- **Problem:** leaves holes in memory (**fragmentation**). A big object may not fit even if total free space is enough.

**2. Mark-Sweep-Compact:**
- Same as above, plus *Compact*: move live objects together to remove holes.
- **Cost:** moving objects takes time, and all references must be updated.

**3. Copying (Mark-Copy):**
- Copy live objects to a fresh empty area. Then throw away the whole old area.
- **Good for:** the Young Generation, where most objects die. You copy only the few survivors, so it's fast.
- **Cost:** needs spare space to copy into. That's why Survivor spaces exist.

```
Before:  [A][ ][B][ ][ ][C][ ]   (fragmented, some dead)
After compact/copy:  [A][B][C][ ][ ][ ][ ]   (clean and continuous)
```

---

### Q6. What are Minor GC, Major GC, and Full GC?

**Short answer:**
- **Minor GC:** cleans the Young Generation. Frequent and fast.
- **Major GC:** cleans the Old Generation.
- **Full GC:** cleans the whole heap (Young + Old) and often Metaspace too. Slowest.

**Deeper explanation:** Minor GCs are cheap because most young objects are dead, and copying survivors is quick. Full GCs are expensive because they scan a lot more data. They are also the ones that cause long pauses. **If you see frequent Full GCs in logs, something is wrong.** Common reasons are a memory leak, an undersized heap, or too many objects being promoted early.

**Note:** The terms "Major" and "Full" are used loosely, and different collectors use them differently. In an interview, explain what *you* mean by them.

---

### Q7. What are the main garbage collectors in HotSpot?

| Collector | Flag | Best for |
|---|---|---|
| **Serial** | `-XX:+UseSerialGC` | Tiny apps, small heaps, single CPU |
| **Parallel** | `-XX:+UseParallelGC` | **Throughput** (batch jobs), pauses are acceptable |
| **G1** | `-XX:+UseG1GC` | **Balanced**, the default since Java 9 |
| **ZGC** | `-XX:+UseZGC` | **Very low latency**, large heaps |
| **Shenandoah** | `-XX:+UseShenandoahGC` | **Low latency**, similar goals to ZGC |

The old **CMS** collector was removed in Java 14. G1 replaced it.

**Two goals that fight each other:**
- **Throughput:** how much of the time the app does real work (not GC).
- **Latency:** how long a single pause lasts.

You usually cannot have the best of both. Parallel GC picks throughput. ZGC picks latency.

---

### Q8. How do Serial and Parallel collectors work?

**Serial GC:** one thread does all GC work. Everything stops during GC. Simple and low overhead, so it's good for small heaps or containers with 1 CPU.

**Parallel GC:** same idea, but **many threads** do the GC work together. Pauses are still stop-the-world, but they are shorter than Serial's because of the extra threads. It gives high throughput. It was the default in Java 8.

Use them when pauses of hundreds of milliseconds don't matter, such as nightly batch processing.

---

### Q9. How does G1 (Garbage-First) work?

**Short answer:** G1 splits the heap into many equal-sized **regions** and collects the regions with the most garbage first, within a pause-time goal.

```
+----+----+----+----+----+----+----+----+
| E  | O  | E  | S  | O  | H  | H  | E  |
+----+----+----+----+----+----+----+----+
E = Eden   S = Survivor   O = Old   H = Humongous
```

Instead of two big fixed areas, each region gets a role, and roles can change over time.

**Key ideas:**
- **Pause target:** `-XX:MaxGCPauseMillis=200` (default). G1 tries to stay under it by choosing how many regions to collect each time.
- **Garbage first:** it tracks how much garbage each region has and picks the best ones. That gives the most memory back for the least work.
- **Evacuation:** live objects are *copied* out of chosen regions into new ones. This also compacts memory as a side effect, so there's less fragmentation.
- **Concurrent marking:** a background cycle marks live objects in the Old Generation while the app keeps running. It starts when the heap is about 45% full (`-XX:InitiatingHeapOccupancyPercent`).
- **Humongous objects:** objects bigger than half a region go into special contiguous regions. Many of these can hurt G1, so watch out for big arrays.

**Typical G1 cycle:** Young-only collections → concurrent marking → "mixed" collections (young + some old regions) → repeat.

**Worst case:** If G1 cannot keep up, it falls back to a **Full GC**, which is slow. This is called an evacuation failure or to-space exhausted. It's a warning sign.

---

## Advanced Level

### Q10. How do ZGC and Shenandoah get such short pauses?

**Short answer:** They do almost all the work, **including moving objects**, *while the application is running*. Pause time does not grow with heap size.

**The hard part:** Moving an object while your code is using it is dangerous. Other threads might still hold the old address. The solution is to intercept reads.

- **ZGC** uses **colored pointers** and **load barriers.** Extra bits inside the reference show its state. A small piece of code (the load barrier) runs when your code reads a reference from the heap. If the reference is out of date, the barrier fixes it on the spot. Your thread quietly gets the right address.
- **Shenandoah** uses similar barrier-based techniques to relocate objects concurrently.

**Result:** STW pauses are typically **under 1 millisecond** (often much less), even on multi-terabyte heaps.

**Trade-offs:**
- Barriers add a small cost to every reference read, so **throughput is a bit lower** than Parallel GC.
- They use more CPU in background threads and often need extra memory headroom.

**Generational ZGC:** ZGC first ran without generations. Generational ZGC (added in Java 21) separates young and old objects again, because the "most objects die young" rule still helps. It uses less memory and CPU. In recent Java versions, generational is the way ZGC works (`-XX:+UseZGC` is enough on newer versions, while Java 21 needs `-XX:+ZGenerational`). Check your JDK version's docs.

---

### Q11. What is a write barrier? Why do collectors need it?

**Short answer:** A small piece of code the JVM inserts around reference **writes**, so the GC can track changes.

**Example problem:** During a Minor GC, the GC scans only the Young Generation. But an **Old** object might point to a **Young** object. If the GC doesn't know about that, it will wrongly think the young object is dead.

**Solution:** Track old-to-young pointers. When your code writes a reference, the write barrier records it:
- **Card table (Parallel, older collectors):** the heap is divided into small "cards." A write marks that card as *dirty*. The GC scans only dirty cards.
- **Remembered sets (G1):** each region keeps a set of places outside it that point *into* it.

You never write this code. It happens automatically. But it's why writing references has a small hidden cost, and why ZGC and Shenandoah also need **read** barriers.

---

### Q12. How do you choose a garbage collector?

Ask what matters most.

- **Small app, few CPUs or tiny container:** Serial.
- **Batch jobs, throughput matters, pauses are fine:** Parallel.
- **General web services and APIs:** **G1** (the default is good for most).
- **Latency-sensitive services (trading, real-time, big heaps, strict SLAs):** ZGC or Shenandoah.

**Practical advice:** Do not change the GC just because it sounds better. Measure first. Change one setting at a time. Test under realistic load. Many teams gain more by fixing **allocation rates and leaks** than by switching collectors.

---

### Q13. How do you read GC logs?

**Enable them (Java 9+):**

```
-Xlog:gc*:file=gc.log:time,uptime,level,tags
```

**A simple G1 log line looks like:**

```
[12.345s][info][gc] GC(15) Pause Young (Normal) (G1 Evacuation Pause) 512M->96M(1024M) 8.7ms
```

Read it as:
- `Pause Young (Normal)`: this is a young collection.
- `512M->96M`: heap used went from 512 MB to 96 MB.
- `(1024M)`: the total heap size right now.
- `8.7ms`: how long the pause took.

**What to look for:**
- **Pause times** vs. your target.
- **How often GCs happen.** Very frequent means a high allocation rate.
- **Heap after GC.** If the "after" number keeps growing over time, you probably have a **leak**.
- **Full GCs** or "to-space exhausted" messages.
- **Promotion rate:** how fast data is moving into Old Gen.

**Tools:** GCeasy, GCViewer, JDK Mission Control, and Java Flight Recorder.

---

### Q14. What are common GC problems and how do you fix them?

**1. Frequent Full GCs**
- *Causes:* memory leak, heap too small, Metaspace resizing.
- *Fix:* take a heap dump, look for leaks, then adjust heap size if truly needed.

**2. Long pauses**
- *Causes:* huge heap with an old collector, too many humongous objects, an oversized live set.
- *Fix:* switch to G1 or ZGC, tune the pause goal, avoid giant short-lived arrays.

**3. High allocation rate (GC running constantly)**
- *Causes:* creating too many temporary objects, such as string concatenation in loops, boxing, and unneeded copies.
- *Fix:* profile allocations (JFR or async-profiler), reduce object creation, reuse buffers.

**4. Premature promotion**
- *Cause:* Young Gen too small, so short-lived objects get pushed into Old Gen and clog it.
- *Fix:* give Young Gen more space.

**5. `GC overhead limit exceeded`**
- *Meaning:* the JVM spends about 98% of its time in GC but recovers less than 2% of the heap.
- *Fix:* almost always a leak or a heap that is far too small.

**6. Container problems**
- *Cause:* JVM sees the wrong CPU or memory limits, or `-Xmx` is set too close to the container limit.
- *Fix:* use a container-aware JDK (Java 10+ is), and leave headroom for non-heap memory.

---

### Q15. What are the different types of references in Java? How do they relate to GC?

**Short answer:** Java has four reference strengths. They let you tell the GC how strongly you want to keep an object.

| Type | When collected | Common use |
|---|---|---|
| **Strong** (normal) | Never while reachable | Everyday code |
| **Soft** | Only when memory is low | Memory-sensitive caches |
| **Weak** | At the next GC if no strong refs remain | `WeakHashMap`, canonicalizing maps |
| **Phantom** | After the object is finalized, used only to get notified | Cleanup actions (`Cleaner`) |

**Example:**

```java
WeakReference<Image> ref = new WeakReference<>(loadImage());
Image img = ref.get();   // may return null if GC already collected it
```

**Note:** Soft references for caches sound nice, but they are hard to tune and can behave badly under GC pressure. Real cache libraries like Caffeine give you more control.

---

### Q16. What is the difference between `-Xmx`, heap size, and total process memory?

**Short answer:** `-Xmx` limits only the **Java heap**. The OS sees the whole process, which is bigger.

```
Process memory ≈ Heap
               + Metaspace
               + Thread stacks
               + Code cache (JIT)
               + Direct/native memory
               + GC internal structures
```

**Example:** With `-Xmx2g` in a 2 GB container, the process will likely be killed by the OS (OOMKilled). The heap alone fits, but everything else pushes it over. A safer setup is something like `-XX:MaxRAMPercentage=70` to leave headroom.

This links back to the memory structure concept, so it's a good answer to bring up in system design or production questions.

---

## Quick Recap

- GC frees objects that are **unreachable from GC Roots**. No reference counting, so cycles are fine.
- `System.gc()` is a request. `finalize()` is deprecated. Avoid both.
- Algorithms: **Mark-Sweep**, **Mark-Sweep-Compact**, **Copying**.
- Young collections are frequent and fast. **Full GC is expensive.**
- **Serial** (small), **Parallel** (throughput), **G1** (balanced default), **ZGC / Shenandoah** (low latency).
- G1 uses **regions** and collects the ones with the most garbage first, aiming for a pause target.
- ZGC and Shenandoah use **barriers** to move objects while the app runs.
- **Write barriers** (card tables, remembered sets) let young collections work without scanning the whole heap.
- Read GC logs: watch pause time, frequency, and heap size *after* GC.
- Most GC problems come from **leaks** or **too many allocations**, so fix the code before the flags.
- Four reference types: **Strong, Soft, Weak, Phantom**.

---

**Next concept:** *Java Memory Model & Concurrency Basics* (threads, `volatile`, `synchronized`, happens-before, visibility, and atomicity).

Say **"next"** when you're ready.

---
---
---

# Deep Dive: What Happens When You Run `java Hello`

Let's follow one real program from the moment you press Enter until it exits.

```java
public class Hello {
    static int count = 5;

    static { System.out.println("Static block"); }

    public static void main(String[] args) {
        int a = 10;
        int b = 20;
        int sum = add(a, b);
        System.out.println(sum);
    }

    static int add(int x, int y) {
        return x + y;
    }
}
```

**Output:**
```
Static block
30
```

Notice that "Static block" prints **before** anything in `main`. We'll see why.

**Big picture:**

```
You type command -> OS starts 'java' launcher -> Launcher creates JVM
   -> JVM boots itself -> Main thread created -> Hello loaded
   -> Linked -> Initialized -> main() runs -> JVM exits
```

---

## Step 0: Compile first (before any of this)

`javac Hello.java` produces `Hello.class`. If you run `javap -c -v Hello`, you'll see the bytecode. Here is what matters:

**main:**
```
 0: bipush 10          // push 10
 2: istore_1           // a = 10
 3: bipush 20          // push 20
 5: istore_2           // b = 20
 6: iload_1            // push a
 7: iload_2            // push b
 8: invokestatic add   // call add(a, b)
11: istore_3           // sum = result
12: getstatic System.out
15: iload_3            // push sum
16: invokevirtual println(I)V
19: return
```

**add:**
```
0: iload_0    // push x
1: iload_1    // push y
2: iadd       // add top two
3: ireturn    // return the result
```

The class file also stores two important numbers for each method: **`max_locals`** and **`max_stack`**. For `main`: `max_locals=4` (args, a, b, sum) and `max_stack=2`. For `add`: `max_locals=2`, `max_stack=2`. The JVM uses these to size stack frames. Remember this for Step 7.

---

## Step 1: Who starts it?

**You** start it, and the **operating system** does the real work.

1. You type `java Hello` in the terminal.
2. The shell asks the OS to start the program named `java`.
3. The OS creates a new **process**, gives it its own memory space, and runs the **`java` launcher**. This is a small native (C) program. **The launcher is not the JVM.** It is a starter program.
4. The launcher reads your command: the main class name, any `-Xmx` flags, the classpath, and so on.
5. It finds the JVM library on disk (`libjvm.so` on Linux, `jvm.dll` on Windows) and loads it.
6. It creates a **new OS thread** and, inside that thread, asks the library to create the JVM (`JNI_CreateJavaVM`). It uses a new thread so it can control the stack size (`-Xss`).

---

## Step 2: The JVM boots itself

Before your code even starts, the JVM prepares its own world:

- **Reserves memory** for the heap from the OS. It is reserved first and only used as needed.
- **Sets up the garbage collector** you chose (G1 by default) and starts GC threads.
- **Creates Metaspace** for class data.
- **Creates the Code Cache**, where JIT-compiled code will go later.
- **Starts the compiler threads** (C1 and C2).
- **Generates the interpreter.** HotSpot builds a *template interpreter*: for each bytecode (`iadd`, `iload`, and so on) it pre-generates a small piece of machine code. This is much faster than one giant `switch` statement.
- **Starts helper threads:** Signal Dispatcher, Reference Handler, Service thread, Cleaner, and more.
- **Loads core classes** with the Bootstrap Class Loader: `Object`, `String`, `Class`, `System`, `Thread`, and others. It initializes the module system (`java.base`).

You can see this yourself: `java -verbose:class Hello` will show **hundreds** of classes loaded even for this tiny program.

---

## Step 3: The main thread and its stack

The thread the launcher created in Step 1 is now attached to the JVM as the **Java `main` thread**.

- It is a real **OS thread**, and the OS gives it a native stack. Its size comes from `-Xss` (about 1 MB by default on 64-bit Linux).
- The JVM gives it a **PC register** (which bytecode is next).
- In HotSpot, the Java stack frames live on this same OS thread stack.

At this moment the stack is **empty**. No method has been called yet.

---

## Step 4: Loading `Hello`

The launcher now asks the **Application Class Loader** to load `Hello` (as in Concept 2):

1. The class loader follows **parent delegation**. Bootstrap and Platform say "not ours."
2. The Application loader searches the classpath (the current folder by default) and finds `Hello.class`.
3. It reads the bytes and parses them.
4. The JVM builds the class's internal data in **Metaspace**: method bytecode, the **runtime constant pool**, field info, and so on.
5. It also creates a `java.lang.Class` object for `Hello` in the **heap**.
6. The launcher checks that `public static void main(String[])` exists. If not, you get the famous "main method not found" error.

---

## Step 5: Linking

**Verify:** The JVM checks the bytecode is safe. Is the format valid? Do all instructions use the stack correctly? Does `iadd` always find two ints on the stack? This step is why Java is safe: bad bytecode cannot crash the JVM or read random memory.

**Prepare:** The JVM allocates space for static fields and sets them to **default values**.

```
count = 0      // NOT 5 yet
```

**Resolve:** The class file uses **names** instead of addresses. For example, `invokestatic` points to a constant pool entry like `Hello.add:(II)I`. Resolution turns that name into a real direct pointer to the method. HotSpot usually does this **lazily**, at the moment the instruction runs for the first time.

---

## Step 6: Initialization (why "Static block" prints first)

Now the JVM runs the class's special static initializer, **`<clinit>`**. The compiler builds it by collecting all static field assignments and static blocks **in the order you wrote them**:

```
iconst_5
putstatic count              // count = 5 (finally!)
getstatic System.out
ldc "Static block"
invokevirtual println        // prints "Static block"
return
```

**Who triggers it?** The first "active use" of the class. Here, it is the JVM invoking the static method `main`. So initialization happens **on the main thread, just before `main()` begins**. That's why "Static block" appears first.

**Thread safety:** The JVM takes a lock for each class's initialization. If two threads try to initialize the same class at once, one waits. This is why static initialization is safe by default, and why the lazy holder singleton pattern works.

---

## Step 7: Calling `main` and how stack frames are created

The launcher now calls `main` through JNI (`CallStaticVoidMethod`).

**How is a frame created?** When a method is invoked, the JVM:
1. Looks up `max_locals` and `max_stack` from the method's class data.
2. Reserves that much space on top of the thread's stack for the **local variable array** and the **operand stack**.
3. Adds some frame bookkeeping (a pointer to the constant pool, and info to return to the caller).
4. Copies the arguments into the first local variable slots.
5. Sets the PC to the method's first instruction.

### Trace of our program

**Frame for `main` is created.** `max_locals=4` and `max_stack=2`.

```
main frame
 locals: [ args, _, _, _ ]     operand stack: [ ]
```

**Instructions run one by one:**

| Bytecode | Locals | Operand stack |
|---|---|---|
| `bipush 10` | `[args, _, _, _]` | `[10]` |
| `istore_1` | `[args, 10, _, _]` | `[ ]` |
| `bipush 20` | same | `[20]` |
| `istore_2` | `[args, 10, 20, _]` | `[ ]` |
| `iload_1` | same | `[10]` |
| `iload_2` | same | `[10, 20]` |

**`invokestatic add`:** this is the interesting one.
1. The JVM resolves `add` (first call, so it finds the real method now).
2. It **pops** `10` and `20` from `main`'s operand stack.
3. It creates a **new frame** for `add` (`max_locals=2`), and puts `10` and `20` into its local slots 0 and 1.

```
+------------------------------+
| add frame         (TOP)      |
|  locals: [10, 20]            |
|  operand stack: [ ]          |
+------------------------------+
| main frame                   |
|  locals: [args, 10, 20, _]   |
|  operand stack: [ ]          |
+------------------------------+
```

**Inside `add`:**

| Bytecode | Operand stack of `add` |
|---|---|
| `iload_0` | `[10]` |
| `iload_1` | `[10, 20]` |
| `iadd` | `[30]` (pops two, pushes the sum) |
| `ireturn` | pops `30`, **destroys `add`'s frame**, pushes `30` onto `main`'s operand stack |

**Back in `main`:**

| Bytecode | Result |
|---|---|
| `istore_3` | `sum = 30` |
| `getstatic System.out` | pushes the `PrintStream` reference |
| `iload_3` | pushes `30` |
| `invokevirtual println` | prints `30` (new frame for `println`, which calls deeper code) |
| `return` | `main`'s frame is popped. Stack is empty again. |

Notice that frames are created on each call and destroyed on return. No garbage collector is involved for the stack. It just moves the top pointer.

---

## Step 8: How the execution engine works

Every step above was carried out by the **execution engine**. It has three parts.

### 1. The interpreter

A simple loop:

```
loop:
   read the opcode at the PC
   run its code
   move the PC to the next instruction
   repeat
```

This is what ran our whole program. It needs no waiting and starts instantly, but it is slow for heavy work.

### 2. The profiler (counters)

For each method, the JVM keeps two counters:
- **Invocation counter:** how many times the method was called.
- **Back-edge counter:** how many times a loop inside it jumped back.

### 3. The JIT compilers

When the counters cross a threshold, the method is marked "hot" and added to a **compile queue**.
- A **compiler thread** compiles it in the background. Your program **keeps running** in the interpreter meanwhile.
- The machine code is stored in the **Code Cache**.
- The method's entry point is switched. The **next call** goes straight to native code.

For a loop that is already running, the JVM uses **OSR (On-Stack Replacement)**. It swaps the running interpreted frame for a compiled one in the middle of the loop.

**In our example,** `add` runs only once, so it stays interpreted. But if you called `add` inside a loop one million times, it would quickly get compiled by C1, then C2, and probably get **inlined** into the loop (no call, no frame at all).

**Deoptimization:** If a JIT assumption turns out wrong, the JVM throws away the compiled code and **goes back to the interpreter**, rebuilding the interpreted frames from the compiled state.

---

## Step 9: Shutdown

1. `main` returns. The launcher's thread calls `DetachCurrentThread`, which **waits for all non-daemon threads** to finish.
2. The JVM then runs **shutdown hooks** (`Runtime.addShutdownHook`) and stops its own threads.
3. The process ends with exit code `0`. The OS takes back all memory (heap, Metaspace, stacks).

**Other ways to end:** `System.exit(n)`, or an **uncaught exception** in a non-daemon thread (exit code 1 if it was `main`), or a crash. Daemon threads (like GC helpers) do not keep the JVM alive.

---

## A small correction to my earlier short answer

Earlier I wrote "the JVM creates the main thread" and listed initialization before the main thread. To be more exact:
- The **launcher** creates the OS thread, and the JVM attaches it as the Java `main` thread. This happens **before** your class loads.
- `Hello`'s **initialization runs on that main thread**, just before `main()` starts.

The earlier list was a simple view. This is the real order.

---

## Quick Recap

- **You** start it. The **OS** creates the process and runs the **`java` launcher**, which boots the **JVM**.
- The JVM sets up the heap, GC, Metaspace, code cache, the interpreter, and helper threads before your code runs.
- The **main thread** is an OS thread with its own stack, and it runs `Hello`'s initialization.
- **Loading** → **Linking** (Verify, Prepare, Resolve) → **Initialization** (`<clinit>`) → `main()`.
- A **stack frame** is sized from `max_locals` and `max_stack`. It is created on call and destroyed on return. Arguments move from the caller's operand stack to the callee's locals.
- The **interpreter** runs bytecode one instruction at a time. **Counters** find hot code, and the **JIT** compiles it in the background.
- The JVM exits when `main` ends and no non-daemon threads remain.

---

If this is clear, say **"next"** and we'll move to **Concept 5: Java Memory Model & Concurrency Basics** (threads, `volatile`, `synchronized`, happens-before, visibility, and atomicity). Or tell me if you'd like me to go deeper into any one step here, like JIT inlining or how `invokevirtual` finds the right method.

---
---
---

# Deep Dive: Linking and Initialization

Here is the full picture of where these steps sit:

```
LOADING  ->  LINKING                          ->  INITIALIZATION
             (1) Verify                            runs <clinit>
             (2) Prepare                           (static blocks and
             (3) Resolve*                           static field values)

* Resolve may happen later, even after initialization.
```

The order is not strict. Verify always comes before Prepare, and Initialize always comes after Prepare. But **Resolve is flexible**. HotSpot does it lazily, when an instruction is first executed.

---

## 1. Verification

### What is it?
Verification checks that the bytecode is **valid and safe** before the JVM runs it.

### Why is it needed?
`javac` never produces bad bytecode. But the JVM cannot assume that every `.class` file came from `javac`. A file can be corrupted, hand-edited, produced by a buggy bytecode tool, or built on purpose to attack the JVM. Without verification, bad bytecode could read random memory, break the stack, or crash the process. Verification is a big part of what makes Java "safe."

### What does it check?
There are roughly four kinds of checks:

**1. Format check.** Is this a proper class file?
- It starts with the magic number `0xCAFEBABE`.
- The version is supported. A class compiled with Java 21 on a Java 11 JVM gives `UnsupportedClassVersionError`.
- The constant pool and other structures are well-formed.
- Failure gives `ClassFormatError`.

**2. Class-level rules.**
- A `final` class is not extended.
- A `final` method is not overridden.
- Every class except `Object` has a superclass.

**3. Bytecode check (the big one).** The verifier walks through each method and checks that every instruction is safe.
- The operand stack never underflows or overflows.
- Types match. `iadd` needs two ints, not an object reference.
- Local variables are set before they are used.
- Jumps land on the start of a real instruction, never in the middle of one.
- A method's return instruction matches its return type.
- An object is not used before its constructor has been called.

**4. Reference check.** Does the target of a symbolic reference look valid? This part actually happens during resolution.

### A picture of what it catches

```
iconst_1
aload_0          // pushes an object reference
iadd             // tries to add an int and a reference -> VerifyError
```

Failure gives `java.lang.VerifyError`.

### How is it fast? StackMapTable
Since Java 6, `javac` writes a **StackMapTable** into each method. It says "at this jump target, the locals and stack look like this." The verifier then checks the code in a **single pass**, comparing what it sees to what the table promised. Before this, the verifier had to *infer* all types by repeated analysis, which was slow.

### Good to know
- You will rarely see `VerifyError` in normal code. You'll see it with **bytecode-generating tools** (ASM, CGLIB, Javassist, Java agents) when they have a bug.
- Classes from the **Bootstrap loader** are not verified by default, for speed. Your app classes are.
- The old flag `-Xverify:none` was deprecated in Java 13. Do not turn verification off in production.

---

## 2. Preparation

### What is it?
The JVM allocates memory for **static fields** and gives them **default values**. No code runs here.

### Default values

| Type | Default |
|---|---|
| `byte`, `short`, `int`, `long` | `0` |
| `float`, `double` | `0.0` |
| `char` | `'\u0000'` |
| `boolean` | `false` |
| any object reference | `null` |

### Key points
- Only **static** fields are handled here. **Instance** fields get their defaults later, when an object is created (new memory is zeroed).
- Your assignments like `static int x = 5;` are **not** run here. That is initialization.
- As we saw in the memory concept, the values of static fields live in the class's `Class` object on the heap.
- The JVM also builds the **method tables (vtable and itable)** during linking. These are the lookup tables that make virtual calls fast later.

### The exception: compile-time constants
`static final int MAX = 100;` is a compile-time constant. It has a `ConstantValue` attribute in the class file. So its real value is already in place before any code can read it, and there is no moment where you see `0`. Also, `javac` usually copies the value straight into code that uses it.

### Example: seeing the default value

```java
public class Demo {
    static int a = getB();
    static int b = 10;

    static int getB() { return b; }

    public static void main(String[] args) {
        System.out.println(a);   // prints 0, not 10
        System.out.println(b);   // prints 10
    }
}
```

**Why?**
- After **Prepare:** `a = 0`, `b = 0`.
- During **Initialization**, lines run in order. First `a = getB()` runs. At this point `b` is still `0` (its default). So `a` becomes `0`.
- Then `b = 10` runs.

This is the practical meaning of "defaults first, real values later."

---

## 3. Resolution

### What is it?
Converting **symbolic references** into **direct references**.

### What is a symbolic reference?
In the class file, the compiler doesn't know where anything will be in memory. So when your code calls `add(a, b)`, the class file just stores a **description** in the constant pool. With `javap -v` it looks like:

```
#7 = Methodref   Hello.add:(II)I
```

This means "class `Hello`, method `add`, takes two ints, returns int." That is a **name**, not an address.

### What is a direct reference?
A real pointer, offset, or handle that the JVM can use immediately. Resolution replaces the name with this.

### What does resolving involve?

**Resolving a class reference:**
- Load the class if it is not loaded yet (using class loaders from Concept 2).
- Check that the current class is **allowed** to access it (`public`, same package, and so on). Failure gives `IllegalAccessError`.
- Note: resolving a class does **not** initialize it.

**Resolving a field reference:**
- Find the field in the class, then its interfaces, then its superclass chain.
- Failure gives `NoSuchFieldError`.

**Resolving a method reference:**
- Find the method in the class and its parents (and interfaces).
- Failure gives `NoSuchMethodError`. Other possible errors are `IllegalAccessError` and `IncompatibleClassChangeError`.

### When does it happen? Lazy vs eager
The spec lets the JVM choose. **HotSpot resolves lazily**: only when an instruction runs for the first time.
- First time `invokestatic add` runs: resolve and save the result in the **constant pool cache**.
- Every later time: use the saved result directly. No lookup again.

### Example showing laziness

Imagine `Main` was compiled against a library `Lib` (version 1) that has a method `foo()`. Later you deploy `Lib` version 2, where `foo()` was removed.

```java
public class Main {
    public static void main(String[] args) {
        System.out.println("Started");   // prints fine
        Lib.foo();                        // NoSuchMethodError here
    }
}
```

- `Main` loads and starts without problems.
- The error appears **only when that line runs**, because that is when `Lib.foo` gets resolved.
- If the line was inside an `if` that never runs, you might never see the error.

This is a classic production problem called a **binary incompatibility**. It is usually caused by dependency version mismatches.

### Resolution vs. method selection
For `invokevirtual`, resolution only finds **which method declaration** is meant. Which actual **implementation** runs is decided later, at call time, using the real class of the object (via the vtable). Those are two different steps.

---

## 4. Initialization

### What is it?
The JVM runs the class's **`<clinit>`** method. This is where static fields get their **real values** and `static {}` blocks run.

### What is `<clinit>`?
The compiler builds it automatically. It collects every static assignment and static block **in the order you wrote them**.

```java
static int count = 5;
static { System.out.println("hi"); }
static String name = "abc";
```

becomes one method, `<clinit>`, doing these three things in that order. If a class has none of these, there is no `<clinit>`.

**Do not confuse it with `<init>`.** `<init>` is the **constructor** (runs for each object). `<clinit>` is the **class initializer** (runs once per class).

### What triggers initialization?
The **first active use** of a class:

1. Creating an object with `new`
2. Calling a **static method**
3. Reading or writing a **static field** (except compile-time constants)
4. Running `main` from that class
5. `Class.forName("X")` (the normal version)
6. Initializing a **subclass**, which first initializes the superclass

### What does NOT trigger it?
- Reading a **compile-time constant** (`static final int MAX = 100`):
  ```java
  System.out.println(Consts.MAX);   // "Consts" is NOT initialized
  ```
  The value was copied into the caller at compile time.
- Making an array: `new Foo[10]` does not initialize `Foo`.
- `Foo.class` does not initialize `Foo`.
- `loadClass()` only loads.
- Reading a static field through a **subclass** name, when the field is declared in the parent:
  ```java
  System.out.println(Child.parentField);   // only Parent is initialized
  ```
  The JVM initializes only the class that actually **declares** the field.

**Note:** `static final int X = compute();` is not a compile-time constant, since the value isn't known at compile time. So it **does** trigger initialization.

### Order with inheritance: a full example

```java
class Parent {
    static { System.out.println("Parent static"); }
    { System.out.println("Parent instance block"); }
    Parent() { System.out.println("Parent constructor"); }
}

class Child extends Parent {
    static { System.out.println("Child static"); }
    { System.out.println("Child instance block"); }
    Child() { System.out.println("Child constructor"); }
}

public class Main {
    public static void main(String[] args) {
        new Child();
        new Child();
    }
}
```

**Output:**
```
Parent static
Child static
Parent instance block
Parent constructor
Child instance block
Child constructor
Parent instance block        <- second object: no static blocks again
Parent constructor
Child instance block
Child constructor
```

**Why?**
- **Class initialization** (once): parent first, then child.
- **Object creation** (each time): parent's instance blocks and constructor, then the child's.

### How the JVM makes initialization thread-safe
Each class has an **initialization lock** and a **state**:

| State | Meaning |
|---|---|
| Not initialized | Nobody started yet |
| Being initialized by thread T | T is running `<clinit>` |
| Fully initialized | Done |
| Erroneous | `<clinit>` failed |

When a thread needs to initialize a class:
1. It takes the lock and checks the state.
2. **If another thread is doing it:** wait until it finishes.
3. **If the same thread is already doing it** (a recursive request): continue without waiting. This is why you can see default values in circular cases.
4. **If already done:** return at once.
5. **If erroneous:** throw `NoClassDefFoundError`.
6. Otherwise, mark it "in progress," release the lock, **initialize the superclass first**, then run `<clinit>`.
7. On success, mark it done and wake any waiting threads.

**Why this matters:** Class initialization is **guaranteed to run once, safely**, and its results are visible to all threads after it finishes. The **lazy holder singleton** pattern depends on this:

```java
class Singleton {
    private Singleton() {}
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }
    static Singleton get() { return Holder.INSTANCE; }
}
```

`Holder` is initialized only when `get()` is first called. The JVM's lock guarantees only one instance is made, with no `synchronized` in your code.

### What if initialization fails?

```java
class Bad {
    static int x = 1 / 0;
}
```

- **First use:** `ExceptionInInitializerError` (the real exception is inside as the cause).
- **Every later use:** `NoClassDefFoundError: Could not initialize class Bad`.

The class is stuck in the "erroneous" state forever for that class loader. This is the tricky case I mentioned in Concept 2. Always look for the **first** error in the logs.

### A deadlock risk
Because initialization holds a lock, circular dependencies between classes can deadlock:

```java
class A { static { new B(); } }
class B { static { new A(); } }
```

If thread 1 starts initializing `A` and thread 2 starts initializing `B` at the same time, each waits for the other. Avoid static initializers that reach into other classes that depend back on them.

---

## Putting It All Together: One Field's Life

```java
class Config {
    static int port = 8080;
    static { System.out.println("loaded"); }
}
```

| Phase | What happens | `port` value |
|---|---|---|
| Loading | `Config.class` is read, `Class` object made | not allocated yet |
| Verify | Bytecode checked | not allocated yet |
| Prepare | Space for `port` allocated | `0` |
| Resolve | Symbolic names turned into direct refs (lazily) | `0` |
| Initialize | `<clinit>` runs: `port = 8080`, prints "loaded" | `8080` |

---

## Quick Interview Questions

**Q: What is the difference between Prepare and Initialization?**
Prepare gives static fields their **default** values (0, null, false) with no code run. Initialization runs `<clinit>` to set the **real** values and run static blocks.

**Q: Does declaring `static final int X = 10` trigger class initialization when read?**
No. It is a compile-time constant, and its value is inlined by the compiler.

**Q: When does `VerifyError` happen, and is it common?**
When bytecode breaks the JVM's safety rules. It is rare in normal code and mostly comes from buggy bytecode tools or corrupted files.

**Q: Is resolution done at load time?**
Not in HotSpot. It is lazy, done on the first execution of the instruction.

**Q: Why does `NoSuchMethodError` only appear at runtime?**
Because the method was there at compile time but missing at run time (a version mismatch), and lazy resolution only checks when the line runs.

**Q: Who initializes the superclass?**
The JVM does, automatically, before the subclass's `<clinit>`.

---

## Recap

- **Verify:** checks the bytecode is valid and safe. Failure: `VerifyError`, `ClassFormatError`.
- **Prepare:** allocates static fields with **default** values. Constants are already set.
- **Resolve:** turns names into direct references, **lazily** in HotSpot. Failures: `NoSuchMethodError`, `NoSuchFieldError`, `IllegalAccessError`.
- **Initialize:** runs `<clinit>` once, in **textual order**, **thread-safe**, **superclass first**.
- A failed initialization leaves the class permanently unusable (`NoClassDefFoundError`).

---

Say **"next"** to move on to **Concept 5: Java Memory Model and Concurrency Basics** (threads, `volatile`, `synchronized`, happens-before, visibility, and atomicity).

---
---
---

# Where Static Fields Live During Prepare

**Short answer:** In modern HotSpot (Java 8+), the **values** of static fields live in the **heap**, inside the `java.lang.Class` object of that class. The **description** of the fields (name, type, offset) lives in **Metaspace**.

So it's a split. Both places are involved, but they hold different things.

---

## The Two Parts

```
METASPACE (native memory)              HEAP
+---------------------------+          +---------------------------+
| InstanceKlass for Config  |          | Class object (mirror)     |
|  - field info:            |          |  for Config               |
|     "port": int,          |  <-----> |  ...internal fields...    |
|     static, offset = 120  |  mirror  |  [static area]            |
|  - methods, bytecode      |  pointer |     port = 0  (Prepare)   |
|  - constant pool          |          |     name = null           |
|  - vtable                 |          +---------------------------+
+---------------------------+
```

- **Metaspace** holds the class's internal structure (called `InstanceKlass` in HotSpot). It knows *that* a static field `port` exists, its type, and **at what offset** its value sits.
- **Heap** holds the `Class` object (also called the **mirror**). The actual static values are stored at the end of this object.
- The two point at each other. The metadata has a pointer to the mirror, and the mirror has a pointer to the metadata.

---

## What Happens During Prepare

1. While parsing the class file, the JVM counts how many static fields there are and how much space they need.
2. That space is **reserved at the end of the `Class` object** in the heap.
3. Heap memory comes **zeroed**. A zero bit pattern means `0`, `0.0`, `false`, or `null`. So default values appear automatically. The JVM doesn't loop through fields writing zeros one by one.

So the "default value" step is really just "the memory was zeroed when allocated."

---

## A Small Honest Note About Timing

The spec says the `Class` object exists after **loading**, and static fields are "prepared" in the **linking** phase. In HotSpot, the mirror is created early, at load time, and its static area is already reserved and zeroed then. So the Prepare step is mostly a **logical** step. The memory is already there and zeroed, and Prepare marks the point where the JVM guarantees the defaults are in place.

For interviews, say: *"Conceptually, Prepare allocates static fields with default values. In HotSpot, they sit in the heap inside the Class object."*

---

## Different Kinds of Static Fields

```java
class Config {
    static int port;                       // primitive
    static String name;                    // reference
    static final int MAX = 100;            // compile-time constant
}
```

| Field | What is stored in the `Class` object | Where the rest lives |
|---|---|---|
| `port` | The `int` value itself, directly | Nowhere else |
| `name` | A **reference** (pointer) | The `String` object is elsewhere on the heap, or in the String Pool |
| `MAX` | Its constant value, set from the `ConstantValue` attribute | Also inlined by `javac` into code that uses it |

For a reference field like `name`, the static area holds only the pointer. The object it points to is a normal heap object.

---

## What About Instance Fields?

They are **not** part of Prepare at all.

```java
class Person {
    int age;        // instance field
}
```

- They are created **each time you call `new Person()`**.
- They live **inside that object on the heap**.
- Their default values (`0`, `null`) come from the zeroed memory of the new object.

Only **static** fields are prepared.

---

## How This Changed Over Java Versions

| Java version | Static field values stored in |
|---|---|
| Java 6 and before | **PermGen** (inside the class data) |
| Java 7 onward | **Heap**, inside the `Class` object |
| Java 8+ | Same as Java 7. PermGen was replaced by Metaspace for the metadata only. |

Some older tutorials still say "static variables live in PermGen/Method Area." That was true in old Java. It is not true for modern HotSpot.

---

## Why This Matters

1. **GC can see them.** Since static values are in the heap, inside the `Class` object, the GC treats them as roots as long as the class is loaded. A static `Map` that keeps growing will never be collected. That is the leak pattern we saw earlier.

2. **Class unloading frees them.** When a class loader is collected, its `Class` objects go too, and their static values go with them. The metadata in Metaspace is freed at the same time.

3. **Metaspace errors are not about static values.** `OutOfMemoryError: Metaspace` means too much class *metadata*. Huge static collections cause `Java heap space` errors instead.

---

## Quick Interview Answer

> "Static fields are allocated in the Prepare phase with default values. In HotSpot since Java 7/8, their values are stored in the heap, inside the `java.lang.Class` object of that class. Metaspace only holds the class metadata, like the field's name, type, and offset. The textbook answer 'Method Area or Metaspace' is about the logical area in the spec, not where HotSpot actually stores the values."

---

## Recap

- Static field **values**: **heap**, inside the `Class` mirror object.
- Static field **metadata**: **Metaspace**.
- Default values come from **zeroed heap memory**.
- Instance fields are **not** part of Prepare. They live inside each object.
- Older Java (6 and before) kept statics in PermGen.

---

Say **"next"** when you're ready for **Concept 5: Java Memory Model and Concurrency Basics**.

---
---
---


---
---
---

