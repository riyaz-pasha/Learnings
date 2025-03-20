# New Features

## Java 8

* Introduced Functional Programming, stream processing and API enchancements.

#### 1. Lambda Expressions (Functional Interfaces)

* Lambda expressions provide a concise way to represent anonymous functions.
[Lambda Example](./concepts/java8/Lambda.java)

#### 2. Functional Interfaces (@FunctionalInterface)

* Java8 introduced functional interfaces like `Runnable`, `Callable`, `Comparator`, `Predicate` etc.
* A functional interface has exactly 1 abstract method and can have multiple default and static methods.
* [Functional Interface](./concepts/java8/FunctionalInterfaceExample.java)
* [Consumer](./concepts/java8/ConsumerExample.java) - void accept(T t) - forEach
* [Predicate](./concepts/java8/PredicateExample.java) - boolean test(T t) - filter
* [Function](./concepts/java8/FunctionExample.java) - R apply(T t) - map
* [Supplier](./concepts/java8/SupplierExample.java) - T get()
* [Comparator](./concepts/java8/ComparatorExample.java) - int compare(T o1, T o2); ( sort, sorted )
* [Runnable]() - void run()
* [Callable]() - T call() throws Exception

#### 3. Default and Static methods in interfaces.

* Before Java8, interfaces could only have abstract methods. Now default and static methods allow defining method implementation.

```java
interface Vehicle{
    default void start(){
        System.out.println("Started...");
    }

    static void stop(){
        System.out.println("Stopped...");
    }
}
```            

#### 4. Optional

* Optional<T> is a container that avoids null references and NullPointerException.
```java
import java.util.Optional;

public class OptionalExample {
    public static void main(String[] args) {
        Optional<String> optionalValue = Optional.ofNullable(null);

        // If value is present, print it; otherwise, print default
        String result = optionalValue.orElse("Default Value");

        System.out.println(result); // Output: Default Value
    }
}
```

#### 5. Method References (:: Operator)
* Method references are shortcuts for lambda expressions.
* [Example](./concepts/java8/MethodReferenceExample.java)



