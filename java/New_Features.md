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



#### 6. Completable Future

* is part of java util concurrency package, provides async computation.
* Before Java 8, Future was used for async computations, but it had limitations:
  * No built-in support for chaining multiple computations.
  * No way to manually complete a Future.
  * Methods like get() were blocking.
* CompletableFuture overcomes these limitations by: 
  * ✅ Allowing non-blocking computations.
  * ✅ Supporting chaining and combining tasks.
  * ✅ Providing exception handling.
  * ✅ Supporting parallel execution.

| Method            | Description                                  |
| ----------------- | -------------------------------------------- |
| `runAsync()`      | Runs a task without returning a result       |
| `supplyAsync()`   | Runs a task and returns a result             |
| `thenApply()`     | Transforms a result                          |
| `thenAccept()`    | Consumes a result (no return)                |
| `thenRun()`       | Runs code after completion (no input/output) |
| `thenCombine()`   | Combines two independent futures             |
| `thenCompose()`   | Chains dependent futures                     |
| `exceptionally()` | Handles exceptions                           |
| `handle()`        | Handles both success and failure cases       |
| `allOf()`         | Runs multiple futures in parallel            |
| `anyOf()`         | Returns the first completed future           |


| Feature            | `thenApply()`                       | `thenAccept()`                                     | `thenRun()`                                           |
| ------------------ | ----------------------------------- | -------------------------------------------------- | ----------------------------------------------------- |
| **Purpose**        | Transforms the result of a future   | Consumes the result without transformation         | Runs a task after completion without using the result |
| **Accepts Input?** | ✅ Yes (T → U)                       | ✅ Yes (Consumes T, returns void)                   | ❌ No (Only runs after completion)                     |
| **Returns Value?** | ✅ Yes (`CompletableFuture<U>`)      | ❌ No (`CompletableFuture<Void>`)                   | ❌ No (`CompletableFuture<Void>`)                      |
| **Use Case**       | Modify data after async computation | Log or store results, trigger side effects         | Execute a final step (cleanup, notification, etc.)    |
| **Example**        | `thenApply(result -> result * 2)`   | `thenAccept(result -> System.out.println(result))` | `thenRun(() -> System.out.println("Done!"))`          |



| Feature               | `thenCompose()`                              | `thenCombine()`                              |
| --------------------- | -------------------------------------------- | -------------------------------------------- |
| **Execution Type**    | **Sequential** execution (one after another) | **Parallel** execution (independent tasks)   |
| **When to Use?**      | When one task **depends on another**         | When two tasks are **independent**           |
| **Input**             | Accepts **one CompletableFuture** as input   | Works with **two CompletableFutures**        |
| **Accepts a Future?** | Yes (flattens nested `CompletableFuture`)    | No (operates on two completed values)        |
| **Example Use Case**  | Fetch user → Fetch user’s orders             | Fetch price & discount → Compute final price |

#### 7. Collectors

| Collector Method                            | Description                                       | Example Usage                                                                                           |
| ------------------------------------------- | ------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `toList()`                                  | Collects elements into a `List`.                  | `List<String> list = stream.collect(Collectors.toList());`                                              |
| `toSet()`                                   | Collects elements into a `Set`.                   | `Set<String> set = stream.collect(Collectors.toSet());`                                                 |
| `toMap(keyMapper, valueMapper)`             | Collects elements into a `Map`.                   | `Map<Integer, String> map = stream.collect(Collectors.toMap(Person::getId, Person::getName));`          |
| `joining(delimiter)`                        | Concatenates elements into a single `String`.     | `String joined = stream.collect(Collectors.joining(", "));`                                             |
| `counting()`                                | Counts the number of elements.                    | `long count = stream.collect(Collectors.counting());`                                                   |
| `summarizingInt(ToIntFunction)`             | Generates statistics like sum, min, max, and avg. | `IntSummaryStatistics stats = stream.collect(Collectors.summarizingInt(Person::getAge));`               |
| `averagingInt(ToIntFunction)`               | Computes the average of integer values.           | `double avg = stream.collect(Collectors.averagingInt(Person::getAge));`                                 |
| `groupingBy(classifier)`                    | Groups elements by a classifier function.         | `Map<String, List<Person>> grouped = stream.collect(Collectors.groupingBy(Person::getDepartment));`     |
| `partitioningBy(predicate)`                 | Partitions elements into two groups (true/false). | `Map<Boolean, List<Person>> partitioned = stream.collect(Collectors.partitioningBy(Person::isActive));` |
| `reducing(identity, accumulator, combiner)` | Reduces elements into a single result.            | `int sum = stream.collect(Collectors.reducing(0, Person::getAge, Integer::sum));`                       |


#### 8 New Date & Time API

* Introduces java.time package for better date/time handling (e.g., LocalDate, LocalTime, ZonedDateTime).
