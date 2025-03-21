# Exceptions

```
java.lang.Throwable
├── java.lang.Error
│   ├── VirtualMachineError
│   │   ├── OutOfMemoryError
│   │   ├── StackOverflowError
│   │   └── InternalError
│   ├── AssertionError
│   └── LinkageError
│       ├── ClassNotFoundError
│       ├── NoClassDefFoundError
│       └── UnsatisfiedLinkError
│
└── java.lang.Exception
    ├── IOException
    ├── SQLException
    ├── ClassNotFoundException
    ├── InterruptedException
    ├── RuntimeException (Unchecked)
    │   ├── NullPointerException
    │   ├── IndexOutOfBoundsException
    │   ├── ArithmeticException
    │   ├── IllegalArgumentException
    │   ├── NumberFormatException
    │   └── ClassCastException
    └── Checked Exceptions (Must be handled)
```

### Errors

* They occurs due to system failures or critical issues like memory leaks.
* Errors indicate serious problem that applications should not try to catch and handle them.
* Happens mostly at run time.

```java
public class StackOverflowExample {
    public static void recursiveMethod() {
        recursiveMethod(); // Infinite recursion
    }

    public static void main(String[] args) {
        recursiveMethod(); // Causes StackOverflowError
    }
}
```

### Exception

* Exceptions are recoverable errors that occurs due to logical issues in the program.
* Can be handled using try catch finally blocks.
* Can happen at both Compile and Run time.
  
**1️⃣ Checked Exceptions (Compile-Time)**

* Must be either caught ( try-catch ) or declared in the method signature using `throws` keyword.
* Other exceptions that are not subclasses of `RuntimeException` are checked exceptions.
  * Examples: `IOException`, `SQLException`, `FileNotFoundException`.

[CheckedExceptions Example](./concepts/exceptions/CheckedExceptions.java)

**2️⃣ Unchecked Exceptions (Runtime)**

* Do not need to be explicitly handled.
* Examples: `NullPointerException`, `ArrayIndexOutOfBoundsException`, `ArithmeticException`, etc.

[UncheckedExceptions Example](./concepts/exceptions/UncheckedExceptions.java)

🔹 Key Takeaways
✔ Unchecked exceptions inherit from `RuntimeException`
✔ They occur due to programming mistakes (null references, invalid indexing, etc.)
✔ They are not required to be handled explicitly (try-catch is optional)
✔ Fix the code rather than handling them in most cases
