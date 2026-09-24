## 1️⃣ Queue – Concept

### What is a Queue?

A **Queue** is a **FIFO (First In, First Out)** data structure.

📌 **Real-life example**

* People standing in a line
* Printer job queue
* Task scheduling

### Core Operations

| Operation  | Meaning                   |
| ---------- | ------------------------- |
| `offer(e)` | Insert element at rear    |
| `poll()`   | Remove element from front |
| `peek()`   | View front element        |

---

## 2️⃣ Deque – Concept

### What is a Deque?

A **Deque (Double Ended Queue)** allows **insertion and removal from both ends**.

📌 **Real-life example**

* Sliding window problems
* Undo / redo operations
* Browser history
* Palindrome checking

### Core Operations

| Front         | Rear         |
| ------------- | ------------ |
| `addFirst(e)` | `addLast(e)` |
| `pollFirst()` | `pollLast()` |
| `peekFirst()` | `peekLast()` |

---

## 3️⃣ Queue vs Deque – Key Differences

| Aspect         | Queue           | Deque                           |
| -------------- | --------------- | ------------------------------- |
| Order          | FIFO            | Both FIFO & LIFO                |
| Insert         | Rear only       | Front & Rear                    |
| Remove         | Front only      | Front & Rear                    |
| Flexibility    | Limited         | High                            |
| Stack behavior | ❌               | ✅                               |
| Typical usage  | Task scheduling | Sliding window, monotonic stack |

---

## 4️⃣ Java Interface Hierarchy

```
Collection
   └── Queue
         └── Deque
```

📌 **Important**
👉 `Deque` **extends** `Queue`
👉 So a Deque **can act as a Queue or Stack**

---

## 5️⃣ Queue Implementations in Java

### Common Implementations

| Class                   | Notes                |
| ----------------------- | -------------------- |
| `LinkedList`            | Doubly linked list   |
| `ArrayDeque`            | Array-based, fast    |
| `PriorityQueue`         | Heap-based, NOT FIFO |
| `ConcurrentLinkedQueue` | Thread-safe          |

### Example – Queue

```java
Queue<Integer> q = new LinkedList<>();

q.offer(10);
q.offer(20);

System.out.println(q.poll()); // 10
System.out.println(q.peek()); // 20
```

---

## 6️⃣ Deque Implementations in Java

### Common Implementations

| Class                   | Notes                |
| ----------------------- | -------------------- |
| `ArrayDeque`            | Fastest, recommended |
| `LinkedList`            | More memory overhead |
| `ConcurrentLinkedDeque` | Thread-safe          |

### Example – Deque

```java
Deque<Integer> dq = new ArrayDeque<>();

dq.addFirst(10);
dq.addLast(20);

System.out.println(dq.pollFirst()); // 10
System.out.println(dq.pollLast());  // 20
```

---

## 7️⃣ ArrayDeque vs LinkedList (Very Important Interview Point)

| Aspect         | ArrayDeque | LinkedList          |
| -------------- | ---------- | ------------------- |
| Memory         | Less       | More (node objects) |
| Cache-friendly | ✅          | ❌                   |
| Performance    | Faster     | Slower              |
| Null allowed   | ❌          | ✅                   |

📌 **Rule of thumb**

> **Always prefer `ArrayDeque` unless you need nulls or concurrency**

---

## 8️⃣ Using Deque as Stack (Best Practice)

### ❌ Avoid legacy `Stack`

```java
Stack<Integer> stack = new Stack<>();
```

### ✅ Use Deque

```java
Deque<Integer> stack = new ArrayDeque<>();

stack.push(10);  // addFirst
stack.push(20);

System.out.println(stack.pop()); // 20
```

📌 This is **officially recommended by Java Docs**

---

## 9️⃣ When to Use What? (Interview Gold ⭐)

### Use **Queue** when:

✔ Strict FIFO required
✔ Task scheduling
✔ BFS traversal
✔ Producer–consumer problems

### Use **Deque** when:

✔ Need access from both ends
✔ Sliding window maximum/minimum
✔ Monotonic stack/queue
✔ Implement stack efficiently
✔ Palindrome checks

---

## 🔟 DSA Problem Mapping

| Problem               | Preferred       |
| --------------------- | --------------- |
| BFS                   | Queue           |
| Level Order Traversal | Queue           |
| Sliding Window Max    | Deque           |
| Next Greater Element  | Deque           |
| Stock Span            | Deque           |
| LRU Cache             | Deque + HashMap |

---

## 1️⃣1️⃣ Common Interview Traps 🚨

❌ Using `PriorityQueue` thinking it’s FIFO
❌ Using `Stack` instead of `Deque`
❌ Using `LinkedList` blindly
❌ Not knowing `Deque extends Queue`

---

## 1️⃣2️⃣ One-Line Summary (Perfect for Interviews)

> **Queue** is FIFO with operations at one end, while **Deque** is a more powerful structure allowing insertion and deletion at both ends and can act as both a Queue and a Stack.

---
