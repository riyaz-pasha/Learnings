# JAVA

* Java is a platform independent language

### JVM - Java Virtual Machine

* JVM is an abstract machine/engine that provides a runtime environment to execute java byte code.
* It is responsible for converting the compiled byte code into machine code that the underlying OS can understand.
* Think of it as the middleman between java code and the physical machine on the which code runs.


**Responsibilities**

* Loading of class file: Loads `.class` files  dynamically at runtime.
* Bytecode verification: Ensures bytecode is safe and doesn't compromise on security.
* Just In Time (JIT) compilation: Converts bytecode into machine code for better performance.
* Memory Management ( Garbage Collection )
* Provides secure execution environment

##### JVM Components

[jvm-architecture](https://www.geeksforgeeks.org/jvm-works-jvm-architecture/)

1. **ClassLoader Subsystem**
   1. Loads, verifies, and initializes classes dynamically.
2. **RunTime Data Areas**
   1. Method Area: Stores class metadata, static variable, and method bytecode
   2. Heap Area: Stores objects and class instances.
   3. Stack Area: Stores method-specific values, local variables, and partial results.
   4. PC Registers: Holds the address of the next instruction to be executed.
   5. Native Method Stack: Supports native (non-Java) methods.
3. **Execution Engine**
   1. Interpreter: Converts bytecode into machine code line by line and interprets.
   2. JIT Compiler (Just-In-Time Compiler):  It is used to increase the efficiency of an interpreter. It compiles the entire bytecode and changes it to native code so whenever the interpreter sees repeated method calls, JIT provides direct native code for that part so re-interpretation is not required, thus efficiency is improved.
4. **Garbage Collector (GC)**:
   1. Removes unused objects from the heap memory to free up space.

##### JRE- Java Runtime Environment

* JRE is a runtime environments that allows Java applications to run on a system.
* It includes JVM + Core libraries required for execution but doesn't include compilers or debuggers.


##### JDK - Java development kit

* JDK is a complete development kit that includes JRE + development tools such as compiler, debugger and other utilities required to build java applications.

**Key Components of JDK:**
1. JRE (Java Runtime Environment)
2. Development Tools:
   1. javac – Java compiler (compiles .java files into .class files)
   2. java – Java application launcher
   3. jdb – Java Debugger
   4. jar – Java archive utility for packaging
   5. javadoc – Documentation generator
   6. jshell – Interactive Java shell for testing code
3. Additional Libraries & APIs (for XML processing, networking, security, etc.)

