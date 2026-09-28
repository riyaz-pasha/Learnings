The Java Collections Framework (JCF) is a unified architecture of standard interfaces and classes designed to store, retrieve, and manipulate groups of data efficiently. [1, 2] 
------------------------------
## Core Interfaces & Hierarchy

* Collection: The root interface for individual objects (extends Iterable).
* List: Ordered collection allowing duplicates and index-based access.
* Set: Collection containing unique elements (no duplicates).
* Queue / Deque: Holds elements prior to processing, typically FIFO or double-ended.
* Map: Stores key-value pairs with unique keys (technically separate from the Collection root, but part of JCF). [1, 3, 4, 5, 6, 7, 8, 9, 10] 

------------------------------
## Internal Workings & Complexities

| Collection | Underlying Data Structure | Access / Search | Insertion (End) | Insertion (Middle/Index) | Deletion |
|---|---|---|---|---|---|
| ArrayList | Dynamic Resizable Array | O(1) | O(1) (amortized) | O(n) | O(n) |
| LinkedList | Doubly Linked List | O(n) | O(1) | O(n) (traverse + O(1) link) | O(n) (if searching) / O(1) (if node known) |
| HashSet | Backed by a HashMap | O(1) | O(1) | N/A | O(1) |
| LinkedHashSet | Hash Table + Doubly Linked List | O(1) | O(1) | N/A | O(1) |
| TreeSet | Red-Black Tree (Self-balancing) | $O(\log n)$ | $O(\log n)$ | N/A | $O(\log n)$ |
| HashMap | Array of Buckets (Linked Lists → Trees at capacity ≥ 8) | O(1) (avg) / O(n) (worst) | O(1) (avg) | N/A | O(1) (avg) |
| TreeMap | Red-Black Tree (Sorted by Keys) | $O(\log n)$ | $O(\log n)$ | N/A | $O(\log n)$ |
| ArrayDeque | Resizable Circular Array | O(n) (search) / O(1) (ends) | O(1) | N/A | O(1) (head/tail) |

## Detailed Mechanics of Key Collections:

   1. ArrayList Internal Expansion: Backed by an Object[] array. When it exceeds capacity, a new array is allocated with size increased by roughly 50% (oldCapacity + (oldCapacity >> 1)). Elements are copied over via System.arraycopy(). [11] 
   2. HashMap Collision Handling: Hashes key objects via hashCode(), modified by a secondary mixer (h ^ (h >>> 16)) to index a bucket array. If multiple keys map to the same bucket (collision), Java uses a linked list. If a bucket exceeds 8 elements (TREEIFY_THRESHOLD), it transforms into a balanced Red-Black Tree to keep worst-case lookup at $O(\log n)$ instead of O(n). [3] 

------------------------------
## When to Choose Each Collection

* Choose ArrayList when: You need fast random access by index, and additions mostly occur at the end of the sequence. (Default choice for lists). [3, 7] 
* Choose LinkedList when: You frequently add or remove items at both ends or perform heavy queue/deque manipulations without requiring random index lookups. [3] 
* Choose HashSet when: You need an unordered, fast collection of unique elements where exact duplicate prevention and O(1) checks matter most. [3, 12] 
* Choose LinkedHashSet when: You need uniqueness like a HashSet, but want to preserve the original insertion order.
* Choose TreeSet when: You need elements automatically sorted in natural order or via a custom Comparator. [12] 
* Choose HashMap when: You require fast key-value lookups, updates, or insertions with no ordering guarantees. [3, 12] 
* Choose TreeMap when: You need keys sorted numerically or alphabetically. [12] 
* Choose ArrayDeque when: You are implementing a stack or a queue; it is significantly faster and more memory-efficient than a LinkedList for queue operations.

If you'd like, let me know:

* Are you working on a multi-threaded environment?
* Do you have specific memory or performance bottlenecks in your current project?

I can provide targeted thread-safe alternatives (like ConcurrentHashMap) or optimize your choices further!

[1] [https://www.youtube.com](https://www.youtube.com/watch?v=TT1-qsuHMXs)
[2] [https://www.w3schools.com](https://www.w3schools.com/java/java_collections.asp)
[3] [https://nareshit.com](https://nareshit.com/blogs/java-collections-framework-guide-for-beginners)
[4] [https://utho.com](https://utho.com/blog/java-collection-framework-benefits-types-diagram/)
[5] [https://www.youtube.com](https://www.youtube.com/watch?v=hnGuCIgc5Nk&t=329)
[6] [https://www.geeksforgeeks.org](https://www.geeksforgeeks.org/java/java-collection-tutorial/)
[7] [https://medium.com](https://medium.com/@dngbingc/core-java-collections-deep-dive-04f431f583d9)
[8] [https://medium.com](https://medium.com/@shubhamjha642/mastering-java-collections-in-detail-a-complete-guide-with-code-diagrams-and-use-cases-ec2fc9724c11)
[9] [https://www.geeksforgeeks.org](https://www.geeksforgeeks.org/java/collections-class-in-java/)
[10] [https://www.igmguru.com](https://www.igmguru.com/blog/collections-in-java)
[11] [https://www.youtube.com](https://www.youtube.com/watch?v=fGHQU3air4s&t=38)
[12] [https://medium.com](https://medium.com/@pablofcurty/understanding-the-complexities-of-collections-in-java-a-practical-guide-on-how-to-choose-the-best-3f342a796d92)

---

## Topic 5 — Java Collections Framework

This is **one of the highest-priority Java interview topics**. For EPAM, you should be comfortable not just using collections, but explaining **how they work internally, complexity, and when to choose each one**.

---

# 1. What is the Collections Framework?

The Java Collections Framework provides interfaces and implementations for storing and manipulating groups of objects.

For example:

```java
List<String> names = new ArrayList<>();

names.add("Alice");
names.add("Bob");
names.add("Charlie");
```

Instead of implementing arrays, linked lists, hash tables, trees, etc. yourself, Java provides ready-made implementations.

The major interfaces are:

```text
Iterable
   |
Collection
   |
   +--- List
   |      +--- ArrayList
   |      +--- LinkedList
   |      +--- Vector
   |
   +--- Set
   |      +--- HashSet
   |      +--- LinkedHashSet
   |      +--- SortedSet
   |             +--- TreeSet
   |
   +--- Queue
          +--- PriorityQueue
          +--- Deque
                 +--- ArrayDeque


Map   <-- separate hierarchy
 |
 +--- HashMap
 +--- LinkedHashMap
 +--- SortedMap
        +--- TreeMap
```

### Important interview point

**`Map` is NOT a subtype of `Collection`.**

For example:

```java
Collection<String>
```

stores individual elements.

Whereas:

```java
Map<String, Integer>
```

stores key-value pairs.

---

# 2. `Collection` vs `Collections` vs `Map`

This is a common interview question.

### `Collection`

An interface.

```java
Collection<String> names;
```

It is the parent interface of `List`, `Set`, and `Queue`.

---

### `Collections`

A utility class containing static methods for working with collections.

```java
Collections.sort(list);
Collections.reverse(list);
Collections.max(list);
Collections.min(list);
```

For example:

```java
List<Integer> numbers = new ArrayList<>();

numbers.add(30);
numbers.add(10);
numbers.add(20);

Collections.sort(numbers);
```

Result:

```text
[10, 20, 30]
```

---

### `Map`

Stores key-value pairs.

```java
Map<String, Integer> ages = new HashMap<>();

ages.put("John", 30);
ages.put("Alice", 25);
```

Think:

```text
Collection → individual elements

Map        → key → value
```

---

# 3. List

A `List`:

* Allows duplicates
* Maintains an order
* Allows index-based access

Example:

```java
List<String> names = new ArrayList<>();

names.add("Alice");
names.add("Bob");
names.add("Alice");
```

Result:

```text
[Alice, Bob, Alice]
```

Duplicates are allowed.

Common implementations:

```text
ArrayList
LinkedList
Vector
```

The most important ones are **ArrayList and LinkedList**.

---

# 4. ArrayList

Internally, `ArrayList` uses a **resizable array**.

Conceptually:

```text
ArrayList
    |
    v
[ A ][ B ][ C ][ D ][   ][   ][   ]
```

When the internal array becomes full, a larger array is created and elements are copied.

---

## Why is ArrayList fast for `get()`?

Consider:

```java
list.get(3);
```

An array supports direct indexing.

Conceptually:

```text
address = baseAddress + index * elementSize
```

Therefore:

```java
list.get(index)
```

is generally:

```text
O(1)
```

---

## ArrayList complexity

| Operation        |     Complexity |
| ---------------- | -------------: |
| `get(index)`     |           O(1) |
| `set(index)`     |           O(1) |
| add at end       | O(1) amortized |
| add at beginning |           O(n) |
| add in middle    |           O(n) |
| remove by index  |           O(n) |
| search           |           O(n) |

Why is insertion in the middle O(n)?

Suppose:

```text
[A][B][C][D][E]
```

Insert `X` at index 2:

```text
[A][B][X][C][D][E]
```

`C`, `D`, and `E` need to shift.

---

# 5. What does "amortized O(1)" mean?

This is a good interview follow-up.

Suppose the internal array is full:

```text
[A][B][C][D]
```

You do:

```java
list.add("E");
```

Java needs to resize the backing array and copy elements.

That particular operation costs:

```text
O(n)
```

But resizing doesn't happen every time.

Across many additions, the **average cost per addition** remains approximately constant.

Therefore:

```text
add()
→ O(1) amortized
```

This distinction is important:

> An individual resize can be O(n), while append is O(1) amortized.

---

# 6. Size vs Capacity

Another common question.

Suppose:

```java
ArrayList<String> list = new ArrayList<>();
```

The list has:

```text
size = number of actual elements
capacity = space currently available in backing array
```

For example conceptually:

```text
[A][B][C][ ][ ][ ][ ]
```

Then:

```text
size     = 3
capacity = 7
```

You should not confuse the two.

---

# 7. LinkedList

`LinkedList` internally uses a **doubly linked list**.

Conceptually:

```text
null
  |
  v
[A] <-> [B] <-> [C] <-> [D]
                           |
                          null
```

Each node contains something conceptually like:

```java
class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
}
```

---

# 8. Why is LinkedList `get(index)` slow?

Suppose:

```java
list.get(500);
```

Unlike an array, LinkedList cannot directly jump to index 500.

It has to traverse nodes.

Conceptually:

```text
A → B → C → D → ... → 500
```

Therefore:

```java
get(index) → O(n)
```

---

# 9. LinkedList insertion/removal

This is where people sometimes give an incomplete answer.

You may hear:

> "LinkedList insertion is O(1)."

That's only true **once you already have the node/position**.

For example, if you have an iterator positioned at the correct location:

```text
[A] <-> [B] <-> [C]
```

Insert `X` between B and C:

```text
[A] <-> [B] <-> [X] <-> [C]
```

Only a few links need to change.

So the actual insertion itself is:

```text
O(1)
```

But if you first need to find index 500:

```text
finding position → O(n)
insertion         → O(1)
```

Overall:

```text
O(n)
```

This distinction is excellent for interviews.

---

# 10. ArrayList vs LinkedList

Very common EPAM question.

| Feature              | ArrayList      | LinkedList                           |
| -------------------- | -------------- | ------------------------------------ |
| Internal structure   | Dynamic array  | Doubly linked list                   |
| Random access        | O(1)           | O(n)                                 |
| Add at end           | O(1) amortized | O(1)                                 |
| Insert/remove middle | O(n)           | O(n) to locate, O(1) once positioned |
| Memory               | Lower          | Higher                               |
| Cache locality       | Better         | Worse                                |
| Typical choice       | Most cases     | Specific insertion/removal patterns  |

### Important practical point

Don't automatically choose `LinkedList` just because you have many insertions/deletions.

`ArrayList` is often preferable because of:

* better cache locality
* lower memory overhead
* faster iteration in many real workloads

So the decision should be based on the actual access pattern.

---

# 11. Set

A `Set` stores **unique elements**.

```java
Set<String> names = new HashSet<>();

names.add("Alice");
names.add("Bob");
names.add("Alice");
```

Result:

```text
[Alice, Bob]
```

The second `"Alice"` isn't added.

But how does Java know that two objects are duplicates?

This connects directly to our previous topic:

> `hashCode()` + `equals()`

---

# 12. HashSet

`HashSet` is one of the most important collections to understand.

Conceptually:

```text
HashSet
   |
   v
HashMap
```

A `HashSet` is internally backed by a `HashMap`.

When you do:

```java
set.add("Java");
```

conceptually the element becomes a key in an internal map.

Something like:

```text
"Java" → PRESENT
```

The actual implementation uses an internal dummy value.

---

# 13. How does HashSet prevent duplicates?

Suppose:

```java
set.add(employee1);
set.add(employee2);
```

Java uses:

```text
hashCode()
   ↓
find candidate bucket
   ↓
equals()
   ↓
duplicate or new element?
```

If:

```java
employee1.equals(employee2)
```

is true, the second element is considered a duplicate.

This is why correctly implementing both:

```java
equals()
hashCode()
```

is critical.

---

# 14. HashSet ordering

A `HashSet` does **not guarantee insertion order**.

For example:

```java
Set<Integer> set = new HashSet<>();

set.add(10);
set.add(20);
set.add(30);
```

You should not rely on iteration being:

```text
10
20
30
```

If you need insertion order, use:

```java
LinkedHashSet
```

---

# 15. LinkedHashSet

`LinkedHashSet` maintains insertion order.

```java
Set<String> set = new LinkedHashSet<>();

set.add("C");
set.add("A");
set.add("B");
```

Iteration:

```text
C
A
B
```

Conceptually it combines:

```text
HashSet behavior
+
linked ordering information
```

So compared with `HashSet`, it requires additional memory.

---

# 16. TreeSet

`TreeSet` maintains elements in sorted order.

```java
Set<Integer> numbers = new TreeSet<>();

numbers.add(30);
numbers.add(10);
numbers.add(20);
```

Result:

```text
[10, 20, 30]
```

It is based on a tree structure, specifically a balanced tree in the standard Java implementation.

Operations are generally:

```text
add      → O(log n)
remove   → O(log n)
contains → O(log n)
```

---

# 17. HashSet vs LinkedHashSet vs TreeSet

Remember this table:

| Collection    | Unique? | Order               |
| ------------- | ------- | ------------------- |
| HashSet       | Yes     | No guaranteed order |
| LinkedHashSet | Yes     | Insertion order     |
| TreeSet       | Yes     | Sorted order        |

So:

### Need uniqueness only?

```java
HashSet
```

### Need uniqueness + insertion order?

```java
LinkedHashSet
```

### Need uniqueness + sorted order?

```java
TreeSet
```

---

# 18. Map

Now the most important collection for interviews:

# `HashMap`

A Map stores:

```text
key → value
```

Example:

```java
Map<String, Integer> employees = new HashMap<>();

employees.put("John", 100);
employees.put("Alice", 200);
```

Conceptually:

```text
John  → 100
Alice → 200
```

Keys must be unique.

---

# 19. What happens if we put the same key?

```java
Map<String, Integer> map = new HashMap<>();

map.put("Alice", 100);
map.put("Alice", 200);
```

The second `put` replaces the value.

Result:

```text
Alice → 200
```

The key isn't duplicated.

---

# 20. How does HashMap work internally?

This is **extremely important for interviews**.

Suppose:

```java
map.put("Alice", 100);
```

Conceptually:

```text
"Alice"
   |
   v
hashCode()
   |
   v
hash calculation/spreading
   |
   v
bucket
   |
   v
store key-value entry
```

Later:

```java
map.get("Alice");
```

Conceptually:

```text
"Alice"
   |
   v
hashCode()
   |
   v
same bucket
   |
   v
compare keys using equals()
   |
   v
100
```

The simplified interview answer is:

> HashMap uses the key's hash code to locate a candidate bucket and uses `equals()` to identify the matching key within that bucket.

---

# 21. What happens when two keys have the same hash?

That's called a **collision**.

For example:

```text
Key A → bucket 5
Key B → bucket 5
```

Both need to coexist.

Modern Java's HashMap can represent a heavily-collided bucket using:

```text
linked nodes
```

and, under certain conditions, transform it into a:

```text
balanced tree
```

This improves behavior under heavy collisions.

You don't need to memorize implementation details before understanding the fundamental idea:

```text
hashCode()
     ↓
bucket
     ↓
collision handling
     ↓
equals()
```

---

# 22. HashMap complexity

Average case:

```text
put     → O(1)
get     → O(1)
remove  → O(1)
```

Under severe collision conditions, modern implementations can use tree bins, giving approximately:

```text
O(log n)
```

for operations within a treeified bucket.

Don't say:

> "HashMap is always O(1)."

Better interview answer:

> "HashMap provides expected O(1) average-time lookup, insertion and removal, assuming a good hash distribution. Collision-heavy buckets can have worse behavior, with modern implementations using tree bins to improve heavily collided buckets."

---

# 23. What is load factor?

This is another common interview question.

HashMap needs to decide:

> When should I resize the internal table?

The **load factor** controls this tradeoff.

The commonly used default load factor in Java's `HashMap` is:

```text
0.75
```

Conceptually:

```text
threshold = capacity × loadFactor
```

For example, if:

```text
capacity = 16
loadFactor = 0.75
```

then the resize threshold is approximately:

```text
16 × 0.75 = 12
```

Once the map reaches the relevant threshold, it may resize.

---

# 24. Why resize HashMap?

Imagine:

```text
16 buckets
1000 entries
```

There would likely be many collisions.

Increasing the number of buckets helps distribute entries more effectively.

Conceptually:

```text
Before:

[B][B][B][B]
 |  |  |
 A  B  C
    |
    D


After resizing:

[B][B][B][B][B][B][B][B]
 |     |       |
 A     B       C
```

Resizing itself costs work because entries need to be redistributed/repositioned.

---

# 25. HashMap and mutable keys

This is a **very important interview trap**.

Suppose:

```java
class Employee {
    String name;
}
```

And suppose `name` participates in:

```java
equals()
hashCode()
```

You do:

```java
Employee e = new Employee("John");

Map<Employee, Integer> map = new HashMap<>();

map.put(e, 100);
```

Then:

```java
e.setName("Alice");
```

Now the hash code may change.

The map originally placed the object based on:

```text
hash("John")
```

but you're now looking it up based on:

```text
hash("Alice")
```

Potentially a different bucket.

Therefore:

```java
map.get(e)
```

may fail to find the entry.

### Interview answer

> Keys used in HashMap should ideally be immutable with respect to the fields involved in `equals()` and `hashCode()`.

This is one reason `String` makes an excellent HashMap key.

---

# 26. HashMap vs LinkedHashMap

`LinkedHashMap` extends the basic HashMap behavior by maintaining predictable iteration order.

For example:

```java
Map<String, Integer> map = new LinkedHashMap<>();

map.put("C", 3);
map.put("A", 1);
map.put("B", 2);
```

Iteration maintains insertion order:

```text
C
A
B
```

It can also be configured for **access-order**, which is useful for patterns such as LRU caches.

---

# 27. HashMap vs TreeMap

`TreeMap` maintains keys in sorted order.

```java
Map<Integer, String> map = new TreeMap<>();

map.put(30, "C");
map.put(10, "A");
map.put(20, "B");
```

Iteration:

```text
10 → A
20 → B
30 → C
```

Complexity:

```text
put     → O(log n)
get     → O(log n)
remove  → O(log n)
```

because it is tree-based.

---

# 28. HashMap vs LinkedHashMap vs TreeMap

This is worth memorizing:

| Map           | Ordering                      | Typical get/put |
| ------------- | ----------------------------- | --------------: |
| HashMap       | No guaranteed iteration order |    O(1) average |
| LinkedHashMap | Insertion/access order        |    O(1) average |
| TreeMap       | Sorted by key                 |        O(log n) |

Think:

```text
HashMap
   ↓
Fast lookup

LinkedHashMap
   ↓
Fast lookup + predictable order

TreeMap
   ↓
Sorted keys
```

---

# 29. Queue

A queue generally follows:

```text
FIFO
```

First In, First Out.

Example:

```text
A → B → C

remove()

A leaves first
```

Java provides:

```java
Queue<String> queue = new LinkedList<>();
```

or commonly:

```java
Queue<String> queue = new ArrayDeque<>();
```

Operations include:

```java
offer()
poll()
peek()
```

---

# 30. `add()` vs `offer()`

Both can insert into a queue, but their failure behavior differs for bounded queue implementations.

```java
queue.add(element);
```

can throw an exception if insertion cannot be performed.

```java
queue.offer(element);
```

returns:

```text
true  → inserted
false → couldn't insert
```

For normal unbounded queues such as `ArrayDeque`, this distinction usually doesn't affect everyday usage.

---

# 31. `poll()` vs `remove()`

Both remove the head.

```java
poll()
```

returns:

```text
element
```

or:

```text
null
```

if empty.

Whereas:

```java
remove()
```

throws an exception if the queue is empty.

Similarly:

```java
peek()
```

returns `null` when empty, while:

```java
element()
```

throws an exception when empty.

This is a common interview detail.

---

# 32. Deque

Deque means:

> Double Ended Queue

You can insert/remove from both ends.

```java
Deque<Integer> deque = new ArrayDeque<>();

deque.addFirst(10);
deque.addLast(20);

deque.removeFirst();
deque.removeLast();
```

It can behave like:

```text
Queue
```

or:

```text
Stack
```

This is why `ArrayDeque` is generally preferred over the legacy `Stack` class for stack/Deque-style usage.

---

# 33. PriorityQueue

A `PriorityQueue` doesn't behave like normal FIFO.

The highest/lowest priority element is retrieved according to its ordering.

For example:

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();

pq.add(30);
pq.add(10);
pq.add(20);

System.out.println(pq.poll());
```

Output:

```text
10
```

By default, it's a min-heap-like priority queue.

With a comparator:

```java
PriorityQueue<Integer> pq =
        new PriorityQueue<>(Comparator.reverseOrder());
```

you can make it behave as a max-priority queue.

Typical complexity:

```text
peek → O(1)
offer → O(log n)
poll → O(log n)
```

---

# 34. `Comparable` vs `Comparator`

This becomes especially important with:

```text
TreeSet
TreeMap
PriorityQueue
```

### Comparable

Defines the object's **natural ordering**.

```java
class Employee implements Comparable<Employee> {

    @Override
    public int compareTo(Employee other) {
        return this.id - other.id;
    }
}
```

Then:

```java
Collections.sort(employees);
```

can use that natural ordering.

---

### Comparator

Defines an ordering externally.

```java
employees.sort(
    Comparator.comparing(Employee::getName)
);
```

This is extremely useful when you need different sorting strategies.

For example:

```text
sort by ID
sort by name
sort by salary
```

without changing the `Employee` class.

---

# 35. Important TreeSet trap

`TreeSet` determines uniqueness according to its ordering comparison.

Suppose:

```java
TreeSet<Employee> employees =
        new TreeSet<>(Comparator.comparing(Employee::getName));
```

If two employees have the same name, the comparator may return:

```java
0
```

TreeSet can therefore treat them as equivalent for set ordering purposes even if:

```java
employee1.equals(employee2)
```

is `false`.

This is an important distinction from `HashSet`.

### HashSet

Uses:

```text
hashCode + equals
```

### TreeSet

Uses:

```text
compareTo / Comparator
```

for ordering and determining whether an element is equivalent in the tree.

---

# 36. Iterator

You can traverse collections using:

```java
Iterator<String> iterator = list.iterator();

while (iterator.hasNext()) {
    String value = iterator.next();
}
```

An iterator also provides:

```java
iterator.remove();
```

which is the supported way to remove the last returned element during iteration.

---

# 37. Why can't I remove directly inside a for-each loop?

For example:

```java
for (String name : names) {
    if (name.equals("John")) {
        names.remove(name);
    }
}
```

This can result in:

```text
ConcurrentModificationException
```

for fail-fast collection implementations.

Instead:

```java
Iterator<String> iterator = names.iterator();

while (iterator.hasNext()) {
    String name = iterator.next();

    if (name.equals("John")) {
        iterator.remove();
    }
}
```

For modern Java, another clean option is:

```java
names.removeIf(name -> name.equals("John"));
```

---

# 38. Is ConcurrentModificationException a threading exception?

Not necessarily.

This is a common misconception.

It can happen even in a **single-threaded program** if a collection is structurally modified while being iterated in an unsupported way.

Also:

> Fail-fast behavior is best-effort, not a guarantee of thread safety.

We'll cover concurrent collections in depth when we reach multithreading.

---

# 39. Hashtable

`Hashtable` is an older legacy Map implementation.

Historically it provides synchronized methods.

```java
Hashtable<String, Integer> table;
```

For modern concurrent applications, you generally use:

```java
ConcurrentHashMap
```

rather than choosing `Hashtable` for new code.

This is another likely interview question:

> Why is ConcurrentHashMap preferred over Hashtable?

We'll cover the concurrency details later.

---

# 40. Null behavior

Some common facts:

### HashMap

Allows:

```text
one null key
multiple null values
```

Example:

```java
Map<String, Integer> map = new HashMap<>();

map.put(null, 100);
map.put("A", null);
map.put("B", null);
```

Valid.

### HashSet

Because it is backed by HashMap, it can contain:

```text
one null element
```

### TreeMap / TreeSet

With natural ordering, null generally isn't supported as a key/element because comparison is required.

A custom comparator can change what is accepted, depending on the comparator.

---

# 41. The collection decision tree

This is useful to memorize for interviews.

### Need duplicates?

Yes:

```text
List
```

No:

```text
Set
```

---

### List

Need fast random access?

```text
ArrayList
```

Need specific linked-list/deque behavior?

```text
LinkedList
```

---

### Set

Need ordering?

No:

```text
HashSet
```

Insertion order:

```text
LinkedHashSet
```

Sorted order:

```text
TreeSet
```

---

### Map

Need fastest general-purpose key lookup?

```text
HashMap
```

Need predictable insertion/access order?

```text
LinkedHashMap
```

Need sorted keys?

```text
TreeMap
```

Need concurrent access?

```text
ConcurrentHashMap
```

---

# 42. The most important interview comparison

If EPAM asks:

> "How would you choose between HashMap, LinkedHashMap and TreeMap?"

A strong answer is:

> "I would use HashMap when I primarily need key-value lookup without requiring iteration order. I'd use LinkedHashMap when I need predictable insertion or access order while retaining hash-map style lookup. I'd use TreeMap when I need keys maintained in sorted order and can accept O(log n) operations."

That's much better than simply memorizing definitions.

---

# 43. Another extremely common question

### Why does Map not extend Collection?

Because their abstractions are fundamentally different.

`Collection` represents:

```text
a group of individual elements
```

while `Map` represents:

```text
relationships between keys and values
```

For example:

```java
Collection<String>
```

versus:

```java
Map<String, Integer>
```

A map isn't naturally a collection of individual values because its fundamental elements are **entries**:

```java
Map.Entry<K, V>
```

You can obtain:

```java
map.keySet()
map.values()
map.entrySet()
```

and those are collection-like views.

---

# 44. One more important HashMap interview question

### What happens if two different keys have the same hashCode?

Don't say:

> "They are considered equal."

That's wrong.

Instead:

```text
same hashCode
     ↓
same/candidate bucket
     ↓
equals() comparison
     ↓
if equals == true → same logical key
if equals == false → different keys
```

Therefore:

> Hash collisions are allowed.

This is exactly why the `hashCode()` contract says:

```text
equal objects → same hash code
```

but:

```text
same hash code → NOT necessarily equal
```

---

# 45. Final mental model

For interviews, remember this:

```text
                    COLLECTIONS
                         |
          +--------------+--------------+
          |              |              |
         List           Set           Queue
          |              |              |
      ArrayList       HashSet       PriorityQueue
      LinkedList      LinkedHashSet ArrayDeque
                     TreeSet

                         Map
                          |
              +-----------+-----------+
              |           |           |
           HashMap   LinkedHashMap  TreeMap
```

And the key characteristics:

```text
ArrayList
→ fast index access

LinkedList
→ linked nodes; useful for specific insertion/removal patterns

HashSet
→ unique elements, hash-based

LinkedHashSet
→ unique + insertion order

TreeSet
→ unique + sorted

HashMap
→ key/value + fast average lookup

LinkedHashMap
→ HashMap + predictable order

TreeMap
→ key/value + sorted keys
```

### EPAM-level questions you should be ready for

1. How does `ArrayList` work internally?
2. Why is `ArrayList.get()` O(1)?
3. Why is `LinkedList.get()` O(n)?
4. ArrayList vs LinkedList?
5. How does `HashMap` work internally?
6. What is a collision?
7. What is load factor?
8. What happens during HashMap resize?
9. Why are mutable keys dangerous?
10. HashMap vs LinkedHashMap vs TreeMap?
11. HashSet vs LinkedHashSet vs TreeSet?
12. Why does HashSet need `hashCode()` and `equals()`?
13. Comparable vs Comparator?
14. Why doesn't Map extend Collection?
15. Iterator vs ListIterator?
16. Why can `ConcurrentModificationException` occur?
17. `poll()` vs `remove()`?
18. `peek()` vs `element()`?
19. ArrayDeque vs Stack?
20. HashMap vs ConcurrentHashMap?

**Next topic: Exception Handling in Java** — checked vs unchecked exceptions, `throw` vs `throws`, `try/catch/finally`, custom exceptions, try-with-resources, exception propagation, and the interview traps around `finally` and return values.

