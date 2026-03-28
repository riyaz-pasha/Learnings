import { useState } from "react";

const CONCEPTS = [
  {
    id: "stream-basics",
    title: "Stream Basics",
    icon: "🌊",
    content: `A Stream is a sequence of elements that supports sequential and parallel aggregate operations.

Key traits:
• Not a data structure — doesn't store data
• Functional in nature — doesn't modify source
• Lazily evaluated — intermediate ops run only when terminal op is called
• Can be traversed only ONCE

Stream Pipeline:
  Source → Intermediate Ops (lazy) → Terminal Op (eager)

Creating Streams:
  collection.stream()
  Arrays.stream(arr)
  Stream.of(1, 2, 3)
  Stream.iterate(0, n -> n + 1)
  Stream.generate(Math::random)
  IntStream.range(1, 10)
  IntStream.rangeClosed(1, 10)`
  },
  {
    id: "intermediate",
    title: "Intermediate Operations",
    icon: "⚙️",
    content: `These are LAZY — they return a new Stream.

filter(Predicate)      — keep elements matching condition
map(Function)          — transform each element
flatMap(Function)      — flatten nested streams
distinct()             — remove duplicates (uses equals)
sorted()               — natural order
sorted(Comparator)     — custom order
peek(Consumer)         — debug without consuming
limit(n)               — first n elements
skip(n)                — skip first n elements
mapToInt/Long/Double   — convert to primitive stream

Example:
  list.stream()
      .filter(s -> s.startsWith("A"))
      .map(String::toUpperCase)
      .sorted()
      .collect(Collectors.toList());`
  },
  {
    id: "terminal",
    title: "Terminal Operations",
    icon: "🏁",
    content: `These are EAGER — they trigger the pipeline and produce a result.

collect(Collector)     — gather into collection
forEach(Consumer)      — iterate
forEachOrdered()       — iterate in encounter order
count()                — number of elements
findFirst()            — first element (Optional)
findAny()              — any element (Optional, good for parallel)
anyMatch(Predicate)    — at least one matches
allMatch(Predicate)    — all match
noneMatch(Predicate)   — none match
min(Comparator)        — minimum (Optional)
max(Comparator)        — maximum (Optional)
reduce(identity, BinaryOperator) — fold to single value
toArray()              — convert to array

reduce example:
  int sum = list.stream().reduce(0, Integer::sum);`
  },
  {
    id: "collectors",
    title: "Collectors",
    icon: "🗂️",
    content: `Collectors.toList()
Collectors.toSet()
Collectors.toMap(keyMapper, valueMapper)
Collectors.toUnmodifiableList()
Collectors.joining()
Collectors.joining(", ", "[", "]")
Collectors.counting()
Collectors.summingInt(fn)
Collectors.averagingInt(fn)
Collectors.summarizingInt(fn)  // gives count, sum, min, max, avg
Collectors.groupingBy(classifier)
Collectors.groupingBy(classifier, downstream)
Collectors.partitioningBy(predicate)
Collectors.mapping(fn, downstream)
Collectors.toMap(k, v, mergeFunction)  // handle duplicates

Examples:
  // Group by first letter
  Map<Character, List<String>> grouped = 
    list.stream().collect(groupingBy(s -> s.charAt(0)));

  // Count per group
  Map<String, Long> countByDept = 
    employees.stream().collect(groupingBy(Employee::getDept, counting()));

  // Partition even/odd
  Map<Boolean, List<Integer>> parts = 
    nums.stream().collect(partitioningBy(n -> n % 2 == 0));`
  },
  {
    id: "optional",
    title: "Optional",
    icon: "📦",
    content: `Optional<T> — a container that may or may not hold a non-null value.

Creating:
  Optional.of(value)         // throws NPE if null
  Optional.ofNullable(value) // empty if null
  Optional.empty()

Checking:
  opt.isPresent()
  opt.isEmpty()             // Java 11+

Accessing:
  opt.get()                 // throws if empty
  opt.orElse(default)
  opt.orElseGet(Supplier)
  opt.orElseThrow()
  opt.orElseThrow(Supplier)

Transforming:
  opt.map(fn)
  opt.flatMap(fn)
  opt.filter(predicate)
  opt.ifPresent(Consumer)
  opt.ifPresentOrElse(Consumer, Runnable)  // Java 9+

Best practice: Never use Optional.get() without isPresent() check.
Prefer: opt.orElse(null) or opt.orElseThrow()`
  },
  {
    id: "collections",
    title: "Collections Framework",
    icon: "📚",
    content: `List: ordered, allows duplicates
  ArrayList   — O(1) get, O(n) insert/delete
  LinkedList  — O(n) get, O(1) insert/delete at ends
  Vector      — synchronized ArrayList (legacy)

Set: no duplicates
  HashSet         — O(1) ops, no order
  LinkedHashSet   — insertion order
  TreeSet         — sorted (natural/comparator), O(log n)

Map: key-value pairs
  HashMap         — O(1) ops, no order, allows null key
  LinkedHashMap   — insertion/access order
  TreeMap         — sorted keys, O(log n)
  Hashtable       — synchronized, no null keys (legacy)
  ConcurrentHashMap — thread-safe, no null keys/values

Queue/Deque:
  PriorityQueue   — heap-based, natural order
  ArrayDeque      — stack + queue, faster than Stack
  LinkedList      — implements Queue + Deque

Important: HashMap vs HashSet — HashSet uses HashMap internally!
Initial capacity: 16, Load factor: 0.75`
  },
  {
    id: "comparator",
    title: "Comparator & Comparable",
    icon: "⚖️",
    content: `Comparable (natural ordering — implemented by the class):
  class Employee implements Comparable<Employee> {
    public int compareTo(Employee other) {
      return this.name.compareTo(other.name);
    }
  }

Comparator (external ordering — passed separately):
  // Lambda
  Comparator<Employee> byAge = (a, b) -> a.getAge() - b.getAge();
  
  // Method reference
  Comparator<Employee> byName = Comparator.comparing(Employee::getName);
  
  // Chaining
  Comparator<Employee> comp = 
    Comparator.comparing(Employee::getDept)
              .thenComparing(Employee::getName)
              .thenComparingInt(Employee::getAge);
  
  // Reverse
  comp.reversed()
  Comparator.reverseOrder()
  
  // Null-safe
  Comparator.nullsFirst(Comparator.naturalOrder())
  Comparator.nullsLast(Comparator.naturalOrder())`
  },
  {
    id: "functional",
    title: "Functional Interfaces",
    icon: "λ",
    content: `java.util.function package — all are @FunctionalInterface

Predicate<T>          T → boolean       — test(T)
Function<T,R>         T → R             — apply(T)
BiFunction<T,U,R>     T,U → R           — apply(T,U)
Consumer<T>           T → void          — accept(T)
BiConsumer<T,U>       T,U → void        — accept(T,U)
Supplier<T>           () → T            — get()
UnaryOperator<T>      T → T             — apply(T)
BinaryOperator<T>     T,T → T           — apply(T,T)

Primitive variants (avoid boxing):
  IntPredicate, IntFunction<R>, IntConsumer, IntSupplier
  IntUnaryOperator, IntBinaryOperator
  (same for Long, Double)

Composing:
  Predicate: and(), or(), negate()
  Function:  compose(), andThen()
  Consumer:  andThen()

Example:
  Predicate<String> nonEmpty = s -> !s.isEmpty();
  Predicate<String> longStr = s -> s.length() > 5;
  Predicate<String> both = nonEmpty.and(longStr);`
  },
  {
    id: "parallel",
    title: "Parallel Streams",
    icon: "⚡",
    content: `Parallel streams split work across multiple threads (ForkJoinPool).

collection.parallelStream()
stream.parallel()
stream.sequential()  // convert back

Good for:
  • Large datasets (1000+ elements)
  • CPU-intensive, stateless operations
  • Operations with no ordering requirement

BAD for:
  • Small datasets (overhead > benefit)
  • I/O operations
  • Operations with side effects
  • When order matters

Thread safety:
  // WRONG — race condition
  List<Integer> result = new ArrayList<>();
  list.parallelStream().forEach(result::add); 
  
  // RIGHT
  List<Integer> result = list.parallelStream()
    .collect(Collectors.toList());

Gotchas:
  • forEach in parallel doesn't guarantee order — use forEachOrdered
  • reduce needs associative + stateless operations
  • Avoid shared mutable state`
  }
];

const PROBLEMS = [
  // --- BASIC STREAM OPS ---
  {
    id: 1, category: "Basic Streams", difficulty: "Easy",
    title: "Sum of all elements",
    problem: "Given a list of integers, find the sum using streams.",
    solution: `List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
int sum = nums.stream()
              .mapToInt(Integer::intValue)
              .sum();
// OR
int sum2 = nums.stream().reduce(0, Integer::sum);
System.out.println(sum); // 15`
  },
  {
    id: 2, category: "Basic Streams", difficulty: "Easy",
    title: "Filter even numbers",
    problem: "From a list of integers, collect only even numbers.",
    solution: `List<Integer> nums = Arrays.asList(1,2,3,4,5,6,7,8);
List<Integer> evens = nums.stream()
                          .filter(n -> n % 2 == 0)
                          .collect(Collectors.toList());
// [2, 4, 6, 8]`
  },
  {
    id: 3, category: "Basic Streams", difficulty: "Easy",
    title: "Convert list to uppercase",
    problem: "Given a list of strings, return a new list with all strings in uppercase.",
    solution: `List<String> names = Arrays.asList("alice", "bob", "charlie");
List<String> upper = names.stream()
                          .map(String::toUpperCase)
                          .collect(Collectors.toList());
// [ALICE, BOB, CHARLIE]`
  },
  {
    id: 4, category: "Basic Streams", difficulty: "Easy",
    title: "Find first element starting with 'A'",
    problem: "From a list of strings, find the first one starting with 'A'.",
    solution: `List<String> names = Arrays.asList("Bob","Alice","Anna","Charlie");
Optional<String> first = names.stream()
                              .filter(s -> s.startsWith("A"))
                              .findFirst();
first.ifPresent(System.out::println); // Alice`
  },
  {
    id: 5, category: "Basic Streams", difficulty: "Easy",
    title: "Count strings longer than 5 chars",
    problem: "Count how many strings in a list have length > 5.",
    solution: `List<String> words = Arrays.asList("apple","banana","kiwi","strawberry","fig");
long count = words.stream()
                  .filter(s -> s.length() > 5)
                  .count();
// 2 (banana, strawberry)`
  },
  {
    id: 6, category: "Basic Streams", difficulty: "Easy",
    title: "Remove duplicates and sort",
    problem: "Given a list with duplicates, return a sorted list of unique elements.",
    solution: `List<Integer> nums = Arrays.asList(5,3,1,3,2,5,4,2);
List<Integer> result = nums.stream()
                           .distinct()
                           .sorted()
                           .collect(Collectors.toList());
// [1, 2, 3, 4, 5]`
  },
  {
    id: 7, category: "Basic Streams", difficulty: "Easy",
    title: "Find max and min",
    problem: "Find the max and min values from a list of integers.",
    solution: `List<Integer> nums = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
Optional<Integer> max = nums.stream().max(Integer::compareTo);
Optional<Integer> min = nums.stream().min(Integer::compareTo);
// OR using IntStream
int max2 = nums.stream().mapToInt(i->i).max().getAsInt(); // 9
int min2 = nums.stream().mapToInt(i->i).min().getAsInt(); // 1`
  },
  {
    id: 8, category: "Basic Streams", difficulty: "Easy",
    title: "Check if any/all/none match",
    problem: "Check if any number is negative, all are positive, and none exceed 100.",
    solution: `List<Integer> nums = Arrays.asList(5, 10, 15, 20, 25);
boolean anyNeg    = nums.stream().anyMatch(n -> n < 0);   // false
boolean allPos    = nums.stream().allMatch(n -> n > 0);   // true
boolean noneOver  = nums.stream().noneMatch(n -> n > 100);// true`
  },
  {
    id: 9, category: "Basic Streams", difficulty: "Easy",
    title: "Flatten nested lists (flatMap)",
    problem: "Given a list of lists, flatten them into a single list.",
    solution: `List<List<Integer>> nested = Arrays.asList(
    Arrays.asList(1, 2, 3),
    Arrays.asList(4, 5),
    Arrays.asList(6, 7, 8, 9)
);
List<Integer> flat = nested.stream()
                           .flatMap(Collection::stream)
                           .collect(Collectors.toList());
// [1, 2, 3, 4, 5, 6, 7, 8, 9]`
  },
  {
    id: 10, category: "Basic Streams", difficulty: "Easy",
    title: "Join strings with delimiter",
    problem: "Join a list of strings with ', ' as delimiter, wrapped in [ and ].",
    solution: `List<String> names = Arrays.asList("Alice","Bob","Charlie");
String result = names.stream()
                     .collect(Collectors.joining(", ", "[", "]"));
// [Alice, Bob, Charlie]`
  },
  // --- STRING PROBLEMS ---
  {
    id: 11, category: "Strings", difficulty: "Medium",
    title: "Find duplicate characters in a string",
    problem: "Find all characters that appear more than once in a string.",
    solution: `String str = "programming";
Map<Character, Long> freq = str.chars()
    .mapToObj(c -> (char) c)
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

freq.entrySet().stream()
    .filter(e -> e.getValue() > 1)
    .map(Map.Entry::getKey)
    .forEach(System.out::println);
// r, g, m`
  },
  {
    id: 12, category: "Strings", difficulty: "Medium",
    title: "First non-repeating character",
    problem: "Find the first character in a string that doesn't repeat.",
    solution: `String str = "swiss";
Optional<Character> result = str.chars()
    .mapToObj(c -> (char) c)
    .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()))
    .entrySet().stream()
    .filter(e -> e.getValue() == 1)
    .map(Map.Entry::getKey)
    .findFirst();

result.ifPresent(System.out::println); // w`
  },
  {
    id: 13, category: "Strings", difficulty: "Medium",
    title: "Count vowels and consonants",
    problem: "Count the number of vowels and consonants in a string.",
    solution: `String str = "Hello World";
long vowels = str.toLowerCase().chars()
    .filter(c -> "aeiou".indexOf(c) != -1)
    .count(); // 3

long consonants = str.toLowerCase().chars()
    .filter(Character::isLetter)
    .filter(c -> "aeiou".indexOf(c) == -1)
    .count(); // 7`
  },
  {
    id: 14, category: "Strings", difficulty: "Medium",
    title: "Reverse each word in a sentence",
    problem: "Given a sentence, reverse each word but keep the word order.",
    solution: `String sentence = "Hello World Java";
String result = Arrays.stream(sentence.split(" "))
    .map(word -> new StringBuilder(word).reverse().toString())
    .collect(Collectors.joining(" "));
// olleH dlroW avaJ`
  },
  {
    id: 15, category: "Strings", difficulty: "Medium",
    title: "Check if string is a palindrome",
    problem: "Check if a given string is a palindrome using streams.",
    solution: `String str = "racecar";
String reversed = IntStream.range(0, str.length())
    .mapToObj(i -> String.valueOf(str.charAt(str.length() - 1 - i)))
    .collect(Collectors.joining());
boolean isPalindrome = str.equals(reversed); // true

// Cleaner approach:
boolean isPalin = IntStream.range(0, str.length() / 2)
    .allMatch(i -> str.charAt(i) == str.charAt(str.length()-1-i));`
  },
  {
    id: 16, category: "Strings", difficulty: "Medium",
    title: "Word frequency count",
    problem: "Count the frequency of each word in a sentence.",
    solution: `String sentence = "the quick brown fox jumps over the lazy dog the fox";
Map<String, Long> freq = Arrays.stream(sentence.split(" "))
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
// {the=3, fox=2, quick=1, brown=1, ...}`
  },
  {
    id: 17, category: "Strings", difficulty: "Hard",
    title: "Longest word in a sentence",
    problem: "Find the longest word in a given sentence.",
    solution: `String sentence = "Java streams are very powerful";
Optional<String> longest = Arrays.stream(sentence.split(" "))
    .max(Comparator.comparingInt(String::length));
longest.ifPresent(System.out::println); // powerful

// Get length too:
int maxLen = Arrays.stream(sentence.split(" "))
    .mapToInt(String::length).max().getAsInt(); // 8`
  },
  // --- NUMBERS ---
  {
    id: 18, category: "Numbers", difficulty: "Easy",
    title: "Find average of list",
    problem: "Calculate the average of a list of integers.",
    solution: `List<Integer> nums = Arrays.asList(10, 20, 30, 40, 50);
OptionalDouble avg = nums.stream()
    .mapToInt(Integer::intValue)
    .average();
avg.ifPresent(System.out::println); // 30.0

// Using Collectors:
Double avg2 = nums.stream()
    .collect(Collectors.averagingInt(Integer::intValue)); // 30.0`
  },
  {
    id: 19, category: "Numbers", difficulty: "Medium",
    title: "Fibonacci using iterate",
    problem: "Generate first N Fibonacci numbers using Stream.iterate.",
    solution: `int n = 10;
Stream.iterate(new long[]{0, 1}, f -> new long[]{f[1], f[0] + f[1]})
      .limit(n)
      .map(f -> f[0])
      .forEach(System.out::println);
// 0, 1, 1, 2, 3, 5, 8, 13, 21, 34`
  },
  {
    id: 20, category: "Numbers", difficulty: "Medium",
    title: "Find prime numbers up to N",
    problem: "Find all prime numbers up to N using streams.",
    solution: `int n = 50;
List<Integer> primes = IntStream.rangeClosed(2, n)
    .filter(num -> IntStream.rangeClosed(2, (int) Math.sqrt(num))
                            .allMatch(i -> num % i != 0))
    .boxed()
    .collect(Collectors.toList());
// [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47]`
  },
  {
    id: 21, category: "Numbers", difficulty: "Medium",
    title: "Square of even numbers",
    problem: "From a list, filter even numbers and return list of their squares, sorted descending.",
    solution: `List<Integer> nums = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
List<Integer> result = nums.stream()
    .filter(n -> n % 2 == 0)
    .map(n -> n * n)
    .sorted(Comparator.reverseOrder())
    .collect(Collectors.toList());
// [100, 64, 36, 16, 4]`
  },
  {
    id: 22, category: "Numbers", difficulty: "Medium",
    title: "Second largest number",
    problem: "Find the second largest number in a list.",
    solution: `List<Integer> nums = Arrays.asList(5, 3, 9, 1, 7, 2, 8, 4, 6);
Optional<Integer> second = nums.stream()
    .distinct()
    .sorted(Comparator.reverseOrder())
    .skip(1)
    .findFirst();
second.ifPresent(System.out::println); // 8`
  },
  {
    id: 23, category: "Numbers", difficulty: "Medium",
    title: "Partition numbers into even/odd",
    problem: "Partition a list of integers into even and odd groups.",
    solution: `List<Integer> nums = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
Map<Boolean, List<Integer>> partitioned = nums.stream()
    .collect(Collectors.partitioningBy(n -> n % 2 == 0));
// {true=[2,4,6,8,10], false=[1,3,5,7,9]}`
  },
  {
    id: 24, category: "Numbers", difficulty: "Medium",
    title: "Sum of digits using streams",
    problem: "Find the sum of digits of a given number using streams.",
    solution: `int number = 12345;
int sumOfDigits = String.valueOf(number).chars()
    .map(Character::getNumericValue)
    .sum();
// 15

// Alternative:
int sum2 = Integer.toString(number).chars()
    .map(c -> c - '0')
    .sum();`
  },
  {
    id: 25, category: "Numbers", difficulty: "Hard",
    title: "Running total (prefix sum)",
    problem: "Convert a list of integers into their running totals.",
    solution: `List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
// Using iterate (Java 9+)
int[] running = {0};
List<Integer> prefixSum = nums.stream()
    .map(n -> running[0] += n)
    .collect(Collectors.toList());
// [1, 3, 6, 10, 15]`
  },
  // --- OBJECTS / EMPLOYEES ---
  {
    id: 26, category: "Objects & Employees", difficulty: "Medium",
    title: "Group employees by department",
    problem: "Given a list of Employee(name, dept, salary), group them by department.",
    solution: `// Employee class has: name, dept, salary
Map<String, List<Employee>> byDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDept));

// Count per department:
Map<String, Long> countByDept = employees.stream()
    .collect(Collectors.groupingBy(Employee::getDept, Collectors.counting()));`
  },
  {
    id: 27, category: "Objects & Employees", difficulty: "Medium",
    title: "Highest salary per department",
    problem: "Find the employee with the highest salary in each department.",
    solution: `Map<String, Optional<Employee>> highestPaid = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDept,
        Collectors.maxBy(Comparator.comparingDouble(Employee::getSalary))
    ));

// To get the actual employee (not Optional):
Map<String, Employee> result = employees.stream()
    .collect(Collectors.toMap(
        Employee::getDept,
        Function.identity(),
        BinaryOperator.maxBy(Comparator.comparingDouble(Employee::getSalary))
    ));`
  },
  {
    id: 28, category: "Objects & Employees", difficulty: "Medium",
    title: "Average salary by department",
    problem: "Calculate average salary per department.",
    solution: `Map<String, Double> avgSalaryByDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDept,
        Collectors.averagingDouble(Employee::getSalary)
    ));`
  },
  {
    id: 29, category: "Objects & Employees", difficulty: "Medium",
    title: "Filter and sort employees",
    problem: "Get names of employees with salary > 50000, sorted alphabetically.",
    solution: `List<String> names = employees.stream()
    .filter(e -> e.getSalary() > 50000)
    .sorted(Comparator.comparing(Employee::getName))
    .map(Employee::getName)
    .collect(Collectors.toList());`
  },
  {
    id: 30, category: "Objects & Employees", difficulty: "Medium",
    title: "Top 3 highest paid employees",
    problem: "Find the top 3 employees by salary.",
    solution: `List<Employee> top3 = employees.stream()
    .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
    .limit(3)
    .collect(Collectors.toList());

// Just names:
List<String> top3Names = employees.stream()
    .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
    .limit(3)
    .map(Employee::getName)
    .collect(Collectors.toList());`
  },
  {
    id: 31, category: "Objects & Employees", difficulty: "Hard",
    title: "Department with highest average salary",
    problem: "Find the department name with the highest average salary.",
    solution: `Optional<Map.Entry<String, Double>> topDept = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDept,
        Collectors.averagingDouble(Employee::getSalary)
    ))
    .entrySet().stream()
    .max(Map.Entry.comparingByValue());

topDept.ifPresent(e -> 
    System.out.println(e.getKey() + ": " + e.getValue()));`
  },
  {
    id: 32, category: "Objects & Employees", difficulty: "Hard",
    title: "Employees with same salary",
    problem: "Group employees who share the same salary.",
    solution: `Map<Double, List<String>> sameSalary = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getSalary,
        Collectors.mapping(Employee::getName, Collectors.toList())
    ));

// Filter groups with more than one employee:
sameSalary.entrySet().stream()
    .filter(e -> e.getValue().size() > 1)
    .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));`
  },
  // --- MAP OPERATIONS ---
  {
    id: 33, category: "Maps & Collections", difficulty: "Medium",
    title: "Invert a Map",
    problem: "Given Map<String, Integer>, invert it to Map<Integer, String>.",
    solution: `Map<String, Integer> original = Map.of("a", 1, "b", 2, "c", 3);
Map<Integer, String> inverted = original.entrySet().stream()
    .collect(Collectors.toMap(
        Map.Entry::getValue,
        Map.Entry::getKey
    ));
// {1=a, 2=b, 3=c}`
  },
  {
    id: 34, category: "Maps & Collections", difficulty: "Medium",
    title: "Sort Map by value",
    problem: "Sort a Map<String, Integer> by its values.",
    solution: `Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 85); scores.put("Bob", 72); scores.put("Charlie", 93);

// Sort by value ascending:
Map<String, Integer> sorted = scores.entrySet().stream()
    .sorted(Map.Entry.comparingByValue())
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (e1, e2) -> e1,
        LinkedHashMap::new
    ));
// {Bob=72, Alice=85, Charlie=93}`
  },
  {
    id: 35, category: "Maps & Collections", difficulty: "Medium",
    title: "Merge two maps",
    problem: "Merge two maps; if same key exists, sum the values.",
    solution: `Map<String, Integer> map1 = Map.of("a", 1, "b", 2, "c", 3);
Map<String, Integer> map2 = Map.of("b", 10, "c", 20, "d", 4);

Map<String, Integer> merged = Stream.of(map1, map2)
    .flatMap(m -> m.entrySet().stream())
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        Integer::sum
    ));
// {a=1, b=12, c=23, d=4}`
  },
  {
    id: 36, category: "Maps & Collections", difficulty: "Medium",
    title: "Find most frequent element",
    problem: "Find the most frequently occurring element in a list.",
    solution: `List<String> items = Arrays.asList("apple","banana","apple","cherry","banana","apple");
Optional<String> mostFrequent = items.stream()
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
    .entrySet().stream()
    .max(Map.Entry.comparingByValue())
    .map(Map.Entry::getKey);
mostFrequent.ifPresent(System.out::println); // apple`
  },
  {
    id: 37, category: "Maps & Collections", difficulty: "Hard",
    title: "Group anagrams together",
    problem: "Given a list of strings, group words that are anagrams of each other.",
    solution: `List<String> words = Arrays.asList("eat","tea","tan","ate","nat","bat");
Map<String, List<String>> anagrams = words.stream()
    .collect(Collectors.groupingBy(word -> {
        char[] chars = word.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }));
// {aet=[eat, tea, ate], ant=[tan, nat], abt=[bat]}`
  },
  {
    id: 38, category: "Maps & Collections", difficulty: "Hard",
    title: "Count character frequency in a list of strings",
    problem: "Given a list of strings, count total frequency of each character across all strings.",
    solution: `List<String> words = Arrays.asList("hello", "world", "java");
Map<Character, Long> charFreq = words.stream()
    .flatMapToInt(String::chars)
    .mapToObj(c -> (char) c)
    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
// {h=1, e=1, l=3, o=2, w=1, r=1, d=1, j=1, a=2, v=1}`
  },
  // --- ADVANCED ---
  {
    id: 39, category: "Advanced", difficulty: "Hard",
    title: "Custom Collector — Sum of squares",
    problem: "Use reduce to calculate sum of squares of a list.",
    solution: `List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
int sumOfSquares = nums.stream()
    .reduce(0, (acc, n) -> acc + n * n);
// 55 (1+4+9+16+25)

// Or using mapToInt:
int sos = nums.stream()
    .mapToInt(n -> n * n)
    .sum();`
  },
  {
    id: 40, category: "Advanced", difficulty: "Hard",
    title: "Nested grouping",
    problem: "Group employees first by department, then by gender within each department.",
    solution: `Map<String, Map<String, List<Employee>>> grouped = employees.stream()
    .collect(Collectors.groupingBy(
        Employee::getDept,
        Collectors.groupingBy(Employee::getGender)
    ));
// {IT={Male=[...], Female=[...]}, HR={Male=[...], Female=[...]}}`
  },
  {
    id: 41, category: "Advanced", difficulty: "Hard",
    title: "Collectors.toMap with duplicate key handling",
    problem: "Convert a list to a Map<name, salary>, handling duplicate names by keeping higher salary.",
    solution: `List<Employee> employees = ...; // may have duplicate names
Map<String, Double> empSalaryMap = employees.stream()
    .collect(Collectors.toMap(
        Employee::getName,
        Employee::getSalary,
        Math::max  // merge function: keep higher salary
    ));`
  },
  {
    id: 42, category: "Advanced", difficulty: "Hard",
    title: "Stream with index (enumerate)",
    problem: "Print each element with its index (like Python's enumerate).",
    solution: `List<String> items = Arrays.asList("apple","banana","cherry");
IntStream.range(0, items.size())
    .forEach(i -> System.out.println(i + ": " + items.get(i)));
// 0: apple, 1: banana, 2: cherry

// Collect to map with index as key:
Map<Integer, String> indexed = IntStream.range(0, items.size())
    .boxed()
    .collect(Collectors.toMap(i -> i, items::get));`
  },
  {
    id: 43, category: "Advanced", difficulty: "Hard",
    title: "Transpose a matrix",
    problem: "Transpose a 2D list (matrix) using streams.",
    solution: `int[][] matrix = {{1,2,3},{4,5,6},{7,8,9}};
int rows = matrix.length, cols = matrix[0].length;
int[][] transposed = IntStream.range(0, cols)
    .mapToObj(col -> IntStream.range(0, rows)
                              .map(row -> matrix[row][col])
                              .toArray())
    .toArray(int[][]::new);
// {{1,4,7},{2,5,8},{3,6,9}}`
  },
  {
    id: 44, category: "Advanced", difficulty: "Hard",
    title: "Sliding window maximum",
    problem: "Find max of every k-sized sliding window in a list.",
    solution: `List<Integer> nums = Arrays.asList(1,3,-1,-3,5,3,6,7);
int k = 3;
List<Integer> result = IntStream.range(0, nums.size() - k + 1)
    .mapToObj(i -> nums.subList(i, i + k))
    .map(window -> window.stream().mapToInt(Integer::intValue).max().getAsInt())
    .collect(Collectors.toList());
// [3, 3, 5, 5, 6, 7]`
  },
  {
    id: 45, category: "Advanced", difficulty: "Hard",
    title: "Longest consecutive sequence length",
    problem: "Find the length of the longest consecutive sequence in an unsorted list.",
    solution: `List<Integer> nums = Arrays.asList(100, 4, 200, 1, 3, 2);
Set<Integer> numSet = new HashSet<>(nums);
int maxLen = numSet.stream()
    .filter(n -> !numSet.contains(n - 1)) // start of sequence
    .mapToInt(start -> {
        int len = 0;
        while (numSet.contains(start + len)) len++;
        return len;
    })
    .max().orElse(0);
// 4 (sequence: 1,2,3,4)`
  },
  // --- TRICKY/EPAM FAVORITES ---
  {
    id: 46, category: "EPAM Favorites", difficulty: "Medium",
    title: "List of Strings → Map<length, List<String>>",
    problem: "Group strings by their length.",
    solution: `List<String> words = Arrays.asList("hi","hey","hello","java","is","fun");
Map<Integer, List<String>> byLength = words.stream()
    .collect(Collectors.groupingBy(String::length));
// {2=[hi, is], 3=[hey, fun], 4=[java], 5=[hello]}`
  },
  {
    id: 47, category: "EPAM Favorites", difficulty: "Medium",
    title: "Integer list to comma-separated string",
    problem: "Convert List<Integer> to a comma-separated String.",
    solution: `List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
String result = nums.stream()
    .map(String::valueOf)
    .collect(Collectors.joining(", "));
// "1, 2, 3, 4, 5"`
  },
  {
    id: 48, category: "EPAM Favorites", difficulty: "Hard",
    title: "Find all pairs summing to target",
    problem: "Find all pairs in a list that sum to a target value.",
    solution: `List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
int target = 8;
Set<List<Integer>> pairs = IntStream.range(0, nums.size())
    .boxed()
    .flatMap(i -> IntStream.range(i+1, nums.size())
        .filter(j -> nums.get(i) + nums.get(j) == target)
        .mapToObj(j -> Arrays.asList(nums.get(i), nums.get(j))))
    .collect(Collectors.toSet());
// [[1,7], [2,6], [3,5]]`
  },
  {
    id: 49, category: "EPAM Favorites", difficulty: "Hard",
    title: "Statistics summary",
    problem: "Get count, sum, min, max, and average from a list in one pass.",
    solution: `List<Integer> nums = Arrays.asList(5, 2, 8, 1, 9, 3, 7, 4, 6);
IntSummaryStatistics stats = nums.stream()
    .mapToInt(Integer::intValue)
    .summaryStatistics();

System.out.println("Count: " + stats.getCount());  // 9
System.out.println("Sum: "   + stats.getSum());    // 45
System.out.println("Min: "   + stats.getMin());    // 1
System.out.println("Max: "   + stats.getMax());    // 9
System.out.println("Avg: "   + stats.getAverage()); // 5.0`
  },
  {
    id: 50, category: "EPAM Favorites", difficulty: "Hard",
    title: "Chained Optional operations",
    problem: "Given a user lookup that might return null, safely extract and transform their city name to uppercase.",
    solution: `// userService.findById may return null
// user.getAddress() may return null
// address.getCity() may return null

Optional<String> cityUpper = Optional.ofNullable(userService.findById(id))
    .map(User::getAddress)
    .map(Address::getCity)
    .map(String::toUpperCase);

String city = cityUpper.orElse("UNKNOWN");

// This replaces deeply nested null checks:
// if (user != null && user.getAddress() != null && 
//     user.getAddress().getCity() != null) { ... }`
  },
  {
    id: 51, category: "EPAM Favorites", difficulty: "Hard",
    title: "Infinite stream with limit",
    problem: "Generate an infinite stream of random numbers and collect first 5 that are > 50.",
    solution: `List<Integer> result = Stream.generate(() -> new Random().nextInt(100))
    .filter(n -> n > 50)
    .limit(5)
    .collect(Collectors.toList());

// Generate natural numbers and find first 5 multiples of 7:
List<Integer> mult7 = Stream.iterate(1, n -> n + 1)
    .filter(n -> n % 7 == 0)
    .limit(5)
    .collect(Collectors.toList());
// [7, 14, 21, 28, 35]`
  },
  {
    id: 52, category: "EPAM Favorites", difficulty: "Hard",
    title: "Collect to immutable map with transformation",
    problem: "Given list of employees, create Map<name, String> where value is 'dept-salary' string.",
    solution: `Map<String, String> empInfo = employees.stream()
    .collect(Collectors.toMap(
        Employee::getName,
        e -> e.getDept() + "-" + e.getSalary()
    ));
// {"Alice" -> "IT-75000.0", "Bob" -> "HR-60000.0", ...}

// Using Collectors.toUnmodifiableMap (Java 10+):
Map<String, String> immutable = employees.stream()
    .collect(Collectors.toUnmodifiableMap(
        Employee::getName,
        e -> e.getDept() + "-" + e.getSalary()
    ));`
  }
];

const CATEGORIES = [...new Set(PROBLEMS.map(p => p.category))];
const DIFFICULTIES = ["Easy", "Medium", "Hard"];

const diffColor = { Easy: "#22c55e", Medium: "#f59e0b", Hard: "#ef4444" };
const diffBg = { Easy: "#052e16", Medium: "#451a03", Hard: "#450a0a" };

export default function App() {
  const [view, setView] = useState("home"); // home | concepts | problems
  const [selectedConcept, setSelectedConcept] = useState(null);
  const [selectedProblem, setSelectedProblem] = useState(null);
  const [showSolution, setShowSolution] = useState({});
  const [filterCat, setFilterCat] = useState("All");
  const [filterDiff, setFilterDiff] = useState("All");
  const [solved, setSolved] = useState({});
  const [search, setSearch] = useState("");

  const filteredProblems = PROBLEMS.filter(p => {
    const catOk = filterCat === "All" || p.category === filterCat;
    const diffOk = filterDiff === "All" || p.difficulty === filterDiff;
    const searchOk = !search || p.title.toLowerCase().includes(search.toLowerCase()) || p.problem.toLowerCase().includes(search.toLowerCase());
    return catOk && diffOk && searchOk;
  });

  const toggleSolution = (id) => setShowSolution(s => ({ ...s, [id]: !s[id] }));
  const toggleSolved = (id) => setSolved(s => ({ ...s, [id]: !s[id] }));

  const totalSolved = Object.values(solved).filter(Boolean).length;

  const styles = {
    app: { fontFamily: "'Segoe UI', sans-serif", background: "#0f0f0f", minHeight: "100vh", color: "#e2e8f0", padding: "0" },
    header: { background: "linear-gradient(135deg, #1e1b4b 0%, #312e81 50%, #1e3a5f 100%)", padding: "20px 24px", borderBottom: "1px solid #334155" },
    nav: { display: "flex", gap: "8px", marginTop: "16px", flexWrap: "wrap" },
    navBtn: (active) => ({ background: active ? "#6366f1" : "#1e293b", color: active ? "#fff" : "#94a3b8", border: "1px solid " + (active ? "#6366f1" : "#334155"), padding: "8px 20px", borderRadius: "20px", cursor: "pointer", fontSize: "14px", fontWeight: active ? "600" : "400", transition: "all 0.2s" }),
    container: { padding: "20px 24px", maxWidth: "1000px", margin: "0 auto" },
    grid2: { display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "16px" },
    card: { background: "#1e293b", border: "1px solid #334155", borderRadius: "12px", padding: "20px", cursor: "pointer", transition: "all 0.2s" },
    badge: (diff) => ({ background: diffBg[diff], color: diffColor[diff], border: `1px solid ${diffColor[diff]}`, padding: "2px 10px", borderRadius: "12px", fontSize: "11px", fontWeight: "600" }),
    code: { background: "#0d1117", border: "1px solid #30363d", borderRadius: "8px", padding: "16px", fontFamily: "monospace", fontSize: "13px", overflowX: "auto", lineHeight: "1.6", color: "#c9d1d9", marginTop: "12px" },
    btn: (color) => ({ background: color || "#6366f1", color: "#fff", border: "none", padding: "8px 18px", borderRadius: "8px", cursor: "pointer", fontSize: "13px", fontWeight: "600" }),
    input: { background: "#1e293b", border: "1px solid #334155", borderRadius: "8px", padding: "8px 14px", color: "#e2e8f0", fontSize: "14px", width: "100%", boxSizing: "border-box" },
    select: { background: "#1e293b", border: "1px solid #334155", borderRadius: "8px", padding: "8px 14px", color: "#e2e8f0", fontSize: "14px", cursor: "pointer" },
    progressBar: { background: "#1e293b", borderRadius: "99px", height: "8px", overflow: "hidden", marginTop: "8px" },
    progress: { background: "linear-gradient(90deg, #6366f1, #8b5cf6)", height: "100%", borderRadius: "99px", transition: "width 0.4s" }
  };

  return (
    <div style={styles.app}>
      <div style={styles.header}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h1 style={{ margin: 0, fontSize: "22px", fontWeight: "700", color: "#fff" }}>☕ Java 8 Streams — EPAM Interview Prep</h1>
            <p style={{ margin: "4px 0 0", color: "#94a3b8", fontSize: "13px" }}>8 concept modules · {PROBLEMS.length} problems</p>
          </div>
          <div style={{ textAlign: "right" }}>
            <div style={{ fontSize: "13px", color: "#94a3b8" }}>Progress: {totalSolved}/{PROBLEMS.length} solved</div>
            <div style={styles.progressBar}><div style={{ ...styles.progress, width: `${(totalSolved / PROBLEMS.length) * 100}%` }} /></div>
          </div>
        </div>
        <div style={styles.nav}>
          {["home","concepts","problems"].map(v => (
            <button key={v} style={styles.navBtn(view === v)} onClick={() => { setView(v); setSelectedConcept(null); setSelectedProblem(null); }}>
              {v === "home" ? "🏠 Home" : v === "concepts" ? "📖 Concepts" : "💻 Problems"}
            </button>
          ))}
        </div>
      </div>

      <div style={styles.container}>

        {/* HOME */}
        {view === "home" && (
          <div>
            <div style={{ background: "linear-gradient(135deg, #1e293b, #0f172a)", border: "1px solid #334155", borderRadius: "16px", padding: "28px", marginBottom: "24px" }}>
              <h2 style={{ margin: "0 0 12px", color: "#a5b4fc" }}>🎯 Your Interview Battle Plan</h2>
              <p style={{ margin: "0 0 16px", color: "#94a3b8", lineHeight: "1.7" }}>
                EPAM coding rounds heavily test <strong style={{ color: "#c7d2fe" }}>Java 8 Streams, Collectors, Optional, Comparators, and functional programming</strong>. This guide covers every concept with 52 real problems.
              </p>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "12px" }}>
                {[["📖", "Concepts", "8 modules", "#6366f1"], ["💻", "Problems", "52 problems", "#8b5cf6"], ["⚡", "Easy", `${PROBLEMS.filter(p=>p.difficulty==="Easy").length} problems`, "#22c55e"], ["🔥", "Hard", `${PROBLEMS.filter(p=>p.difficulty==="Hard").length} problems`, "#ef4444"]].map(([icon, label, val, col]) => (
                  <div key={label} style={{ background: "#0f172a", border: `1px solid ${col}33`, borderRadius: "10px", padding: "16px", textAlign: "center" }}>
                    <div style={{ fontSize: "24px" }}>{icon}</div>
                    <div style={{ fontSize: "22px", fontWeight: "700", color: col }}>{val}</div>
                    <div style={{ fontSize: "12px", color: "#64748b" }}>{label}</div>
                  </div>
                ))}
              </div>
            </div>
            <h3 style={{ color: "#a5b4fc", marginBottom: "16px" }}>🗂️ Topics Covered</h3>
            <div style={styles.grid2}>
              {CONCEPTS.map(c => (
                <div key={c.id} style={{ ...styles.card, borderLeft: "3px solid #6366f1" }} onClick={() => { setView("concepts"); setSelectedConcept(c.id); }}>
                  <div style={{ fontSize: "28px" }}>{c.icon}</div>
                  <div style={{ fontWeight: "600", marginTop: "8px", color: "#e2e8f0" }}>{c.title}</div>
                  <div style={{ fontSize: "12px", color: "#64748b", marginTop: "4px" }}>Click to study →</div>
                </div>
              ))}
            </div>
            <div style={{ marginTop: "24px", background: "#1e293b", border: "1px solid #f59e0b33", borderRadius: "12px", padding: "20px" }}>
              <h3 style={{ margin: "0 0 12px", color: "#fbbf24" }}>⚡ Quick Cheat Sheet</h3>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))", gap: "8px", fontSize: "13px", color: "#94a3b8" }}>
                {[["filter()", "Predicate — keep elements"], ["map()", "Function — transform"], ["flatMap()", "Flatten nested streams"], ["collect()", "Terminal — gather results"], ["groupingBy()", "Group into Map"], ["partitioningBy()", "Split into true/false"], ["reduce()", "Fold to single value"], ["Optional.ofNullable()", "Safe null handling"]].map(([k, v]) => (
                  <div key={k} style={{ background: "#0f172a", borderRadius: "8px", padding: "10px 12px" }}>
                    <code style={{ color: "#818cf8" }}>{k}</code><br />
                    <span style={{ fontSize: "12px" }}>{v}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* CONCEPTS */}
        {view === "concepts" && !selectedConcept && (
          <div>
            <h2 style={{ color: "#a5b4fc", marginTop: 0 }}>📖 Concept Modules</h2>
            <div style={styles.grid2}>
              {CONCEPTS.map(c => (
                <div key={c.id} style={{ ...styles.card, borderLeft: "3px solid #6366f1" }} onClick={() => setSelectedConcept(c.id)}>
                  <div style={{ fontSize: "32px" }}>{c.icon}</div>
                  <div style={{ fontWeight: "700", fontSize: "16px", marginTop: "8px", color: "#e2e8f0" }}>{c.title}</div>
                  <div style={{ fontSize: "12px", color: "#64748b", marginTop: "4px" }}>Tap to expand →</div>
                </div>
              ))}
            </div>
          </div>
        )}

        {view === "concepts" && selectedConcept && (() => {
          const idx = CONCEPTS.findIndex(c => c.id === selectedConcept);
          const c = CONCEPTS[idx];
          return (
            <div>
              <button style={{ ...styles.btn("#334155"), marginBottom: "20px" }} onClick={() => setSelectedConcept(null)}>← Back</button>
              <div style={{ display: "flex", gap: "8px", marginBottom: "20px", flexWrap: "wrap" }}>
                {CONCEPTS.map((cc, i) => (
                  <button key={cc.id} style={{ ...styles.navBtn(cc.id === selectedConcept), fontSize: "12px", padding: "6px 14px" }} onClick={() => setSelectedConcept(cc.id)}>
                    {cc.icon} {cc.title}
                  </button>
                ))}
              </div>
              <div style={{ background: "#1e293b", border: "1px solid #334155", borderRadius: "12px", padding: "28px" }}>
                <h2 style={{ margin: "0 0 20px", color: "#a5b4fc" }}>{c.icon} {c.title}</h2>
                <pre style={{ ...styles.code, whiteSpace: "pre-wrap", background: "#0d1117" }}>{c.content}</pre>
              </div>
              <div style={{ display: "flex", gap: "12px", marginTop: "16px" }}>
                {idx > 0 && <button style={styles.btn("#334155")} onClick={() => setSelectedConcept(CONCEPTS[idx-1].id)}>← {CONCEPTS[idx-1].title}</button>}
                {idx < CONCEPTS.length-1 && <button style={styles.btn()} onClick={() => setSelectedConcept(CONCEPTS[idx+1].id)}>{CONCEPTS[idx+1].title} →</button>}
              </div>
            </div>
          );
        })()}

        {/* PROBLEMS */}
        {view === "problems" && !selectedProblem && (
          <div>
            <h2 style={{ color: "#a5b4fc", marginTop: 0 }}>💻 Practice Problems ({filteredProblems.length})</h2>
            <div style={{ display: "flex", gap: "10px", marginBottom: "20px", flexWrap: "wrap" }}>
              <input style={{ ...styles.input, maxWidth: "260px" }} placeholder="🔍 Search problems..." value={search} onChange={e => setSearch(e.target.value)} />
              <select style={styles.select} value={filterCat} onChange={e => setFilterCat(e.target.value)}>
                <option>All</option>
                {CATEGORIES.map(c => <option key={c}>{c}</option>)}
              </select>
              <select style={styles.select} value={filterDiff} onChange={e => setFilterDiff(e.target.value)}>
                <option>All</option>
                {DIFFICULTIES.map(d => <option key={d}>{d}</option>)}
              </select>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              {filteredProblems.map(p => (
                <div key={p.id} style={{ background: solved[p.id] ? "#0d2818" : "#1e293b", border: `1px solid ${solved[p.id] ? "#166534" : "#334155"}`, borderRadius: "10px", padding: "16px 20px", cursor: "pointer", transition: "all 0.15s" }}
                  onClick={() => setSelectedProblem(p.id)}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                      <span style={{ color: solved[p.id] ? "#22c55e" : "#64748b", fontSize: "18px" }}>{solved[p.id] ? "✅" : "⬜"}</span>
                      <div>
                        <span style={{ fontWeight: "600", color: "#e2e8f0" }}>#{p.id} {p.title}</span>
                        <div style={{ fontSize: "12px", color: "#64748b", marginTop: "2px" }}>{p.category}</div>
                      </div>
                    </div>
                    <span style={styles.badge(p.difficulty)}>{p.difficulty}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {view === "problems" && selectedProblem && (() => {
          const p = PROBLEMS.find(x => x.id === selectedProblem);
          const idx = PROBLEMS.findIndex(x => x.id === selectedProblem);
          return (
            <div>
              <button style={{ ...styles.btn("#334155"), marginBottom: "20px" }} onClick={() => setSelectedProblem(null)}>← All Problems</button>
              <div style={{ background: "#1e293b", border: "1px solid #334155", borderRadius: "12px", padding: "28px" }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "12px", marginBottom: "16px" }}>
                  <div>
                    <div style={{ fontSize: "13px", color: "#64748b", marginBottom: "4px" }}>#{p.id} · {p.category}</div>
                    <h2 style={{ margin: 0, color: "#a5b4fc" }}>{p.title}</h2>
                  </div>
                  <span style={styles.badge(p.difficulty)}>{p.difficulty}</span>
                </div>
                <div style={{ background: "#0f172a", border: "1px solid #334155", borderRadius: "8px", padding: "16px", color: "#cbd5e1", lineHeight: "1.7", marginBottom: "20px" }}>
                  {p.problem}
                </div>
                <div style={{ display: "flex", gap: "12px", flexWrap: "wrap" }}>
                  <button style={styles.btn(showSolution[p.id] ? "#374151" : "#6366f1")} onClick={() => toggleSolution(p.id)}>
                    {showSolution[p.id] ? "🙈 Hide Solution" : "💡 Show Solution"}
                  </button>
                  <button style={styles.btn(solved[p.id] ? "#166534" : "#15803d")} onClick={() => toggleSolved(p.id)}>
                    {solved[p.id] ? "✅ Solved!" : "Mark as Solved"}
                  </button>
                </div>
                {showSolution[p.id] && (
                  <pre style={styles.code}>{p.solution}</pre>
                )}
              </div>
              <div style={{ display: "flex", gap: "12px", marginTop: "16px" }}>
                {idx > 0 && <button style={styles.btn("#334155")} onClick={() => { setSelectedProblem(PROBLEMS[idx-1].id); setShowSolution({}); }}>← Prev</button>}
                {idx < PROBLEMS.length-1 && <button style={styles.btn()} onClick={() => { setSelectedProblem(PROBLEMS[idx+1].id); setShowSolution({}); }}>Next →</button>}
              </div>
            </div>
          );
        })()}
      </div>
    </div>
  );
}
