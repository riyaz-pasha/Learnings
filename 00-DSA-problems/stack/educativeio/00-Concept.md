# Stacks — Complete Mastery Guide (Java)
### From first principles to advanced pattern recognition

---

## PART 1 — THE IDEA

### 1.1 What problem does a Stack solve?

Every data structure exists because of an **access pattern**, not because someone invented it for fun.

The access pattern a Stack encodes is:

> "I only ever need the **most recently added** item, and once I'm done with it, I want to go back to whatever was before it."

That's it. That single sentence *is* the entire concept. Everything else — syntax, operations, problems — is a consequence of that one idea.

### 1.2 The core intuition — "undo history"

Think of the **back button** in your browser, or **Ctrl+Z** in a text editor.

```
Visit A -> Visit B -> Visit C -> Visit D
Back button state:  [A, B, C, D]   (D is on top — most recent)

Press Back -> you go to C, and D is discarded from "current" but
              the history still remembers order: [A, B, C]
```

You never jump from D directly to A. You must "peel off" D, then C, then B. That peeling-off-in-reverse-order-of-arrival is **LIFO**: Last In, First Out.

A stack is a physical metaphor made literal:

```
Plates stacked on top of each other:

        [ Plate 4 ]   <- top (last placed, first removed)
        [ Plate 3 ]
        [ Plate 2 ]
        [ Plate 1 ]   <- bottom (first placed, last removed)
        ===========
```

You can only add a plate to the top, and only remove from the top. You cannot pull plate 2 out without disturbing 3 and 4. This constraint is not a limitation — it is the **entire value** of the structure. It guarantees a strict "nesting" or "reversal" relationship between items, and that guarantee is what makes stacks the natural tool for a huge family of problems.

### 1.3 Why does "nesting" matter so much?

Look at where LIFO shows up in the real world:

- Function calls: `main() -> f() -> g() -> h()`. `h()` finishes first, returns to `g()`, which finishes and returns to `f()`. The **call stack**.
- Parentheses: `( [ { } ] )`. The innermost bracket must close before the one enclosing it.
- Undo/redo: the last action you did is the first one undone.
- Browser history, recursion, DFS traversal, backtracking.

The common thread: **"last opened, first closed."** Any time a problem has this "nested/reversal" structure, a stack is the tool.

---

## PART 2 — MECHANICS

### 2.1 Core operations (all O(1))

| Operation | Meaning | Complexity |
|---|---|---|
| `push(x)` | place x on top | O(1) |
| `pop()` | remove and return top | O(1) |
| `peek()` / `top()` | look at top without removing | O(1) |
| `isEmpty()` | check if stack has elements | O(1) |
| `size()` | number of elements | O(1) |

All O(1) because you only ever touch the top — no shifting, no searching.

```
push(1) push(2) push(3)

  [ ]        [ ]        [ ]        [3]
  [ ]   ->   [ ]   ->   [2]   ->   [2]
  [ ]        [1]        [1]        [1]
  ===        ===        ===        ===

pop() -> returns 3

  [2]
  [1]
  ===
```

### 2.2 Implementing a Stack in Java

**Option A — use `Deque` (the recommended, modern way)**

`java.util.Stack` (the old class) is legacy, synchronized (slow), and extends `Vector` — Oracle's own docs recommend `Deque` instead.

```java
import java.util.*;

Deque<Integer> stack = new ArrayDeque<>();

stack.push(1);      // add to top
stack.push(2);
stack.push(3);

int top = stack.peek();   // 3, doesn't remove
int popped = stack.pop(); // 3, removes and returns
boolean empty = stack.isEmpty();
int size = stack.size();
```

**Option B — build it yourself (do this once to internalize the mechanics)**

Array-based:

```java
class ArrayStack {
    private int[] data;
    private int top;      // index of the top element, -1 if empty
    private int capacity;

    public ArrayStack(int capacity) {
        this.capacity = capacity;
        this.data = new int[capacity];
        this.top = -1;
    }

    public void push(int val) {
        if (top == capacity - 1) throw new RuntimeException("Stack Overflow");
        data[++top] = val;
    }

    public int pop() {
        if (isEmpty()) throw new RuntimeException("Stack Underflow");
        return data[top--];
    }

    public int peek() {
        if (isEmpty()) throw new RuntimeException("Stack is empty");
        return data[top];
    }

    public boolean isEmpty() { return top == -1; }
    public int size() { return top + 1; }
}
```

Linked-list based (no fixed capacity):

```java
class LinkedStack {
    private static class Node {
        int val;
        Node next;
        Node(int val, Node next) { this.val = val; this.next = next; }
    }

    private Node head;   // head == top of stack
    private int size;

    public void push(int val) {
        head = new Node(val, head);   // new node points to old head
        size++;
    }

    public int pop() {
        if (isEmpty()) throw new RuntimeException("Stack Underflow");
        int val = head.val;
        head = head.next;
        size--;
        return val;
    }

    public int peek() {
        if (isEmpty()) throw new RuntimeException("Stack is empty");
        return head.val;
    }

    public boolean isEmpty() { return head == null; }
    public int size() { return size; }
}
```

**Why linked-list push/pop at the head, not the tail?** Because inserting/removing at the *head* of a singly linked list is O(1) (no traversal needed). Inserting/removing at the tail would require O(n) traversal (unless you maintain a tail pointer AND make it doubly linked). So "top of stack = head of list" is not arbitrary — it's the only choice that keeps both operations O(1).

```
push(1) -> push(2) -> push(3)

head -> [3] -> [2] -> [1] -> null
        ^top
```

### 2.3 Array vs Linked List implementation — trade-offs

| | Array-based | Linked-list based |
|---|---|---|
| Push/Pop | O(1) amortized (resize occasionally) | O(1) always |
| Memory | Contiguous, cache-friendly, less overhead | Extra pointer per node |
| Fixed size issue | Needs resizing (like ArrayList) | Naturally dynamic |
| Practical use | `ArrayDeque` (Java's default choice) | Rare in practice |

**Rule of thumb:** In interviews and real Java code, just use `Deque<Integer> stack = new ArrayDeque<>();`. Only hand-roll it when explicitly asked to implement from scratch.

---

## PART 3 — HOW TO RECOGNIZE "THIS IS A STACK PROBLEM"

This is the part most people skip, and it's the part that actually makes you fast in interviews. Below are the **signal phrases and structural cues**. When you see them, your brain should immediately go "stack."

### 3.1 Signal #1 — Matching / Nesting / Balancing

> Keywords: "valid parentheses", "matching brackets", "nested", "closing tag must match opening tag", "well-formed"

**Why stack:** Nesting means the most recently opened thing must be the first thing closed. That is *exactly* LIFO.

**How to think about it:**
- See an "opener" → push it.
- See a "closer" → it MUST match the top of the stack (the most recent unmatched opener). Pop and compare.
- At the end, stack must be empty (nothing left unmatched).

```
Input: { [ ( ) ] }

push {        stack: { }
push [        stack: { [
push (        stack: { [ (
see )  -> pop (, matches  stack: { [
see ]  -> pop [, matches  stack: {
see }  -> pop {, matches  stack: (empty)

Empty at end -> VALID
```

**Java:**
```java
public boolean isValid(String s) {
    Deque<Character> stack = new ArrayDeque<>();
    Map<Character, Character> pairs = Map.of(')', '(', ']', '[', '}', '{');

    for (char c : s.toCharArray()) {
        if (pairs.containsValue(c)) {          // it's an opener
            stack.push(c);
        } else if (pairs.containsKey(c)) {     // it's a closer
            if (stack.isEmpty() || stack.pop() != pairs.get(c)) return false;
        }
    }
    return stack.isEmpty();
}
```

**Other problems in this family:** Valid Parentheses, Remove Invalid Parentheses (partially), Generate Parentheses (uses recursion, not stack, but conceptually related), Score of Parentheses, checking balanced HTML/XML tags, Minimum Add to Make Parentheses Valid.

---

### 3.2 Signal #2 — "Next Greater / Smaller Element" → the Monotonic Stack

> Keywords: "next greater element", "next smaller", "previous greater", "span", "how many days until warmer", "largest rectangle", "trapping rain water"

This is the **single most powerful and most tested** stack pattern. Master this and you unlock 20+ interview problems.

**The intuition — build it from scratch:**

Problem: given an array, for every element find the **next element to its right that is greater than it**.

Brute force: for each index, scan right until you find a bigger number → O(n²).

**The key insight:** As you scan left to right, imagine you keep a stack of "candidates that are still waiting for their next greater element." When you see a new number `x`:

- Any candidate on the stack that is **smaller than `x`** now has its answer: it's `x`! Pop it and record the answer.
- Keep popping while the top is smaller than `x`.
- Then push `x` itself (it's now a new candidate waiting for its own answer).

Because you keep only pop-able entries and the stack always increases from bottom to top (before the current push), it's called a **monotonic stack** (monotonically decreasing in this case).

```
arr = [2, 1, 2, 4, 3]

i=0, x=2: stack empty -> push        stack: [2]
i=1, x=1: 1 < top(2), can't pop -> push
                                       stack: [2, 1]
i=2, x=2: top(1) < 2 -> pop, answer[1]=2
          top now 2, 2 is NOT < 2 (equal, stop) -> push 2
                                       stack: [2, 2]
i=3, x=4: top(2) < 4 -> pop, answer[2]=4
          top(2) < 4 -> pop, answer[0]=4
          stack empty -> push 4       stack: [4]
i=4, x=3: top(4) not < 3 -> push 3    stack: [4, 3]

End: whatever remains in stack has no next greater -> answer = -1

Final answer: [4, 2, 4, -1, -1]
```

**Why is this O(n) and not O(n²)?** Because each element is pushed exactly once and popped **at most once**. Total push+pop operations across the whole run ≤ 2n. This "amortized" argument is the proof you should be able to say out loud in an interview.

**Java — Next Greater Element:**
```java
public int[] nextGreaterElement(int[] arr) {
    int n = arr.length;
    int[] result = new int[n];
    Arrays.fill(result, -1);
    Deque<Integer> stack = new ArrayDeque<>();  // stores INDICES

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && arr[stack.peek()] < arr[i]) {
            result[stack.pop()] = arr[i];
        }
        stack.push(i);
    }
    return result;
}
```

**How to decide "increasing" vs "decreasing" monotonic stack, and index vs value:**

| You want | Stack keeps | Pop condition (before pushing arr[i]) |
|---|---|---|
| Next Greater Element (to the right) | decreasing values | pop while `stack.top < arr[i]` |
| Next Smaller Element (to the right) | increasing values | pop while `stack.top > arr[i]` |
| Previous Greater Element | decreasing values, scan left→right, answer = new top after popping | pop while `stack.top <= arr[i]` |
| Previous Smaller Element | increasing values | pop while `stack.top >= arr[i]` |

**Mental shortcut:** "Next **Greater**" → stack must be strictly **decreasing**, because a decreasing stack means every element still waiting is *bigger than everything above it will eventually need to beat*. When a bigger number shows up, it "defeats" everyone smaller below it in the stack. Flip the logic (increasing stack) for "smaller".

**Almost always store *indices*, not values** — because once you find the answer, you usually need to know *where* it applies, and you may need `arr[index]` too. Storing indices gives you both.

### 3.3 Classic monotonic stack problems, explained through the same lens

**(a) Daily Temperatures** — "how many days until a warmer day?" This is literally Next Greater Element, but the answer stored is the **distance** `i - stack.pop()` instead of the value.

```java
public int[] dailyTemperatures(int[] temps) {
    int n = temps.length;
    int[] result = new int[n];
    Deque<Integer> stack = new ArrayDeque<>();

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && temps[stack.peek()] < temps[i]) {
            int idx = stack.pop();
            result[idx] = i - idx;
        }
        stack.push(i);
    }
    return result;
}
```

**(b) Stock Span Problem** — "how many consecutive days (including today) has the price been ≤ today's price?" This is Previous Greater Element in disguise: span = `i - indexOfPreviousGreater`.

```java
public int[] stockSpan(int[] prices) {
    int n = prices.length;
    int[] span = new int[n];
    Deque<Integer> stack = new ArrayDeque<>(); // decreasing stack of indices

    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && prices[stack.peek()] <= prices[i]) {
            stack.pop();
        }
        span[i] = stack.isEmpty() ? (i + 1) : (i - stack.peek());
        stack.push(i);
    }
    return span;
}
```

**(c) Largest Rectangle in Histogram** — the "boss level" of monotonic stack problems.

*Intuition:* For every bar, the largest rectangle **that uses that bar's height** extends left until it hits a shorter bar, and right until it hits a shorter bar. So for each bar `i`, we need: nearest smaller bar to the left, and nearest smaller bar to the right. That's exactly Previous Smaller + Next Smaller — two monotonic stack passes (or one clever pass).

```
heights = [2, 1, 5, 6, 2, 3]

           ___
          |   |___
       ___|   |   |___
      |   |   |   |   |___
   ___|   |   |   |   |   |
  |   |   |   |   |   |   |
  2   1   5   6   2   3

Bar of height 6 (index 3): can extend from index 3 to 3 only
  (left neighbor at index 2 has height 5, still >6? no wait 5<6, so it's bounded)
Bar of height 5 (index 2): extends index 2..3 (width 2) -> area = 5*2=10
Bar of height 2 (index 4) can extend from index 2 to index 5 at height 2
  once bar 6 and bar 5 are "removed" from consideration (they're taller)
  width = 4 (indices 2,3,4,5) -> area = 2*4 = 8
```

The single-pass elegant solution uses one increasing monotonic stack: when a bar shorter than the stack's top appears, that top bar's rectangle is now fully determined (its right boundary is the current index, its left boundary is the new stack top after popping).

```java
public int largestRectangleArea(int[] heights) {
    Deque<Integer> stack = new ArrayDeque<>(); // increasing stack of indices
    int maxArea = 0;
    int n = heights.length;

    for (int i = 0; i <= n; i++) {
        int currHeight = (i == n) ? 0 : heights[i]; // sentinel to flush stack at end
        while (!stack.isEmpty() && heights[stack.peek()] > currHeight) {
            int height = heights[stack.pop()];
            int width = stack.isEmpty() ? i : i - stack.peek() - 1;
            maxArea = Math.max(maxArea, height * width);
        }
        stack.push(i);
    }
    return maxArea;
}
```

**Why the `width` formula works:** when we pop bar at index `poppedIdx` because `heights[poppedIdx] > currHeight`, the right boundary of its rectangle is `i` (first bar shorter than it, exclusive). The left boundary is whatever is now on top of the stack after popping (first bar shorter than it, on the left, exclusive) — or the very start (`0`) if the stack is empty. Width = `i - stack.peek() - 1`, or just `i` if stack is empty.

**(d) Trapping Rain Water** (stack approach) — for each bar, water trapped above it depends on `min(maxLeft, maxRight) - height`. The monotonic-stack version processes bar by bar, and whenever a taller bar appears, it "closes off" a trapped water region between the current bar, the popped (lower) bar, and the new top of stack.

```java
public int trap(int[] height) {
    Deque<Integer> stack = new ArrayDeque<>();
    int water = 0;

    for (int i = 0; i < height.length; i++) {
        while (!stack.isEmpty() && height[stack.peek()] < height[i]) {
            int bottom = stack.pop();
            if (stack.isEmpty()) break;
            int left = stack.peek();
            int boundedHeight = Math.min(height[left], height[i]) - height[bottom];
            int width = i - left - 1;
            water += boundedHeight * width;
        }
        stack.push(i);
    }
    return water;
}
```

---

### 3.4 Signal #3 — Expression Parsing / Evaluation

> Keywords: "evaluate expression", "infix to postfix", "calculator", "reverse polish notation"

**Why stack:** Operator precedence and parenthesization are *nested* structures — same reason as bracket matching, but now you're also tracking operands and operators.

**Postfix (Reverse Polish Notation) evaluation — the easy case:**

Intuition: postfix has no ambiguity, no precedence rules needed. Read left to right: push numbers, and when you hit an operator, pop the last two numbers, apply the operator, push the result back.

```
"2 3 4 * +"   means 2 + (3*4)

push 2        stack: [2]
push 3        stack: [2, 3]
push 4        stack: [2, 3, 4]
see '*' -> pop 4, pop 3 -> 3*4=12 -> push 12
                            stack: [2, 12]
see '+' -> pop 12, pop 2 -> 2+12=14 -> push 14
                            stack: [14]

Result: 14
```

```java
public int evalRPN(String[] tokens) {
    Deque<Integer> stack = new ArrayDeque<>();
    for (String token : tokens) {
        switch (token) {
            case "+", "-", "*", "/" -> {
                int b = stack.pop();
                int a = stack.pop();
                stack.push(switch (token) {
                    case "+" -> a + b;
                    case "-" -> a - b;
                    case "*" -> a * b;
                    default  -> a / b;
                });
            }
            default -> stack.push(Integer.parseInt(token));
        }
    }
    return stack.pop();
}
```

**Infix evaluation (Basic Calculator) — the hard case:**

Infix (`"3 + 5 * 2"`) has precedence rules and possibly parentheses. Two common approaches:

1. **Two stacks** (one for numbers, one for operators) — process left to right, and whenever you see an operator with lower-or-equal precedence than the top of the operator stack, resolve (pop and apply) until it's safe to push.
2. **Convert infix → postfix first (Shunting Yard algorithm)**, then evaluate postfix as above.

**Shunting Yard intuition:** as you scan the infix expression, numbers go straight to output. Operators go onto an operator stack, but before pushing, pop off (into output) any operator on the stack that has **higher or equal precedence** — because that one must be applied first. `(` always pushes; `)` pops everything back to the matching `(`.

```java
public int calculate(String s) {
    Deque<Integer> stack = new ArrayDeque<>();
    int result = 0;
    int number = 0;
    int sign = 1;

    for (char c : s.toCharArray()) {
        if (Character.isDigit(c)) {
            number = number * 10 + (c - '0');
        } else if (c == '+') {
            result += sign * number;
            number = 0; sign = 1;
        } else if (c == '-') {
            result += sign * number;
            number = 0; sign = -1;
        } else if (c == '(') {
            // push current result & sign context, then reset
            stack.push(result);
            stack.push(sign);
            result = 0; sign = 1;
        } else if (c == ')') {
            result += sign * number;
            number = 0;
            result *= stack.pop();      // sign before the '('
            result += stack.pop();      // result before the '('
        }
    }
    return result + sign * number;
}
```

**Other problems in this family:** Basic Calculator I/II/III, Evaluate Reverse Polish Notation, Decode String (`"3[a2[c]]"` → `"accaccacc"`), Infix-to-Postfix conversion, Simplify Path (`/a/./b/../../c/` → `/c`).

**Decode String** deserves its own mini-walkthrough because it's a beautiful "nested nested" stack problem:

```
"3[a2[c]]"  ->  "accaccacc"

Idea: whenever you hit '[', you're "descending" into a nested scope —
push your current (partial string, repeat count) onto the stack,
then start fresh.
When you hit ']', you're "ascending" back out — pop the outer context
and merge: outerString + (innerString repeated k times).
```

```java
public String decodeString(String s) {
    Deque<Integer> countStack = new ArrayDeque<>();
    Deque<StringBuilder> stringStack = new ArrayDeque<>();
    StringBuilder current = new StringBuilder();
    int num = 0;

    for (char c : s.toCharArray()) {
        if (Character.isDigit(c)) {
            num = num * 10 + (c - '0');
        } else if (c == '[') {
            countStack.push(num);
            stringStack.push(current);
            current = new StringBuilder();
            num = 0;
        } else if (c == ']') {
            StringBuilder prev = stringStack.pop();
            int repeat = countStack.pop();
            for (int i = 0; i < repeat; i++) prev.append(current);
            current = prev;
        } else {
            current.append(c);
        }
    }
    return current.toString();
}
```

---

### 3.5 Signal #4 — Simulating Recursion / DFS Iteratively

> Keywords: "convert recursion to iteration", "iterative DFS", "iterative tree traversal", "flatten nested list"

**Why stack:** Recursion *is* a stack, implicitly — the "call stack." Every recursive call pushes a new stack frame (local variables + return address); when the function returns, that frame is popped. If you want to remove recursion (e.g., to avoid stack-overflow on deep inputs, or because an interviewer asks for the iterative version), you simulate that call stack explicitly with your own `Deque`.

```
Recursive DFS on tree:              Iterative DFS with explicit stack:

     1                              stack = [1]
    / \                             pop 1, visit, push right(3), push left(2)
   2   3                            stack = [3, 2]
                                    pop 2, visit, push children (none)
                                    stack = [3]
                                    pop 3, visit
                                    stack = []
```

```java
public List<Integer> preorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    if (root == null) return result;
    Deque<TreeNode> stack = new ArrayDeque<>();
    stack.push(root);

    while (!stack.isEmpty()) {
        TreeNode node = stack.pop();
        result.add(node.val);
        // push right FIRST so left is processed first (LIFO)
        if (node.right != null) stack.push(node.right);
        if (node.left != null) stack.push(node.left);
    }
    return result;
}
```

**Key mental model:** whatever you push last gets processed first. So if you want left-before-right, push right first, then left.

**Iterative Inorder Traversal** (trickier — you must "remember to come back"):

```java
public List<Integer> inorderTraversal(TreeNode root) {
    List<Integer> result = new ArrayList<>();
    Deque<TreeNode> stack = new ArrayDeque<>();
    TreeNode curr = root;

    while (curr != null || !stack.isEmpty()) {
        while (curr != null) {           // go as far left as possible,
            stack.push(curr);            // remembering each node passed
            curr = curr.left;
        }
        curr = stack.pop();              // leftmost unvisited node
        result.add(curr.val);
        curr = curr.right;               // now explore its right subtree
    }
    return result;
}
```

**Other problems in this family:** Iterative Preorder/Inorder/Postorder Traversal, Flatten Nested List Iterator, Binary Tree Zigzag Traversal (uses two stacks), Clone Graph (iterative DFS), N-ary Tree traversals.

---

### 3.6 Signal #5 — "I need to know something about the *previous unmatched* element" (Min/Max Stack family)

> Keywords: "design a stack that also returns the minimum/maximum in O(1)", "min stack"

**Why stack:** You need O(1) access to an aggregate (min/max) *of only the currently active elements* — and "currently active" shrinks and grows exactly like a stack (LIFO), so you track the aggregate in lockstep with a second, parallel stack.

**Design:** maintain two stacks — the main data stack, and a "min stack" where `minStack.peek()` is always the minimum of everything currently in the main stack.

```
push(5): main=[5]          min=[5]
push(3): main=[5,3]        min=[5,3]      (3 <= current min, push)
push(7): main=[5,3,7]      min=[5,3]      (7 > current min, DON'T push; instead push a duplicate of current min OR just don't push at all — see below)
```

Two common implementations:

**Version A — min stack pushes every time (duplicate top when new value isn't a new min):**
```java
class MinStack {
    private Deque<Integer> stack = new ArrayDeque<>();
    private Deque<Integer> minStack = new ArrayDeque<>();

    public void push(int val) {
        stack.push(val);
        if (minStack.isEmpty() || val <= minStack.peek()) {
            minStack.push(val);
        } else {
            minStack.push(minStack.peek()); // duplicate current min
        }
    }

    public void pop() {
        stack.pop();
        minStack.pop();
    }

    public int top() { return stack.peek(); }
    public int getMin() { return minStack.peek(); }
}
```

This "keep both stacks in perfect lockstep, even duplicating values" trick generalizes: you can track max, running sum, running product (careful with 0), etc., the same way — anything that needs to "undo" cleanly when you pop.

**Other problems in this family:** Min Stack, Max Stack, Design a stack with `increment(k, val)` operation, implement Queue using two Stacks (and vice versa).

**Bonus — Queue using two Stacks** (a very common "explain your reasoning" interview question):

```
Idea: one stack for "incoming" pushes, one stack for "outgoing" pops.
When outgoing is empty, dump ALL of incoming into outgoing —
this reverses the order once, turning LIFO into FIFO.
```

```java
class MyQueue {
    private Deque<Integer> inStack = new ArrayDeque<>();
    private Deque<Integer> outStack = new ArrayDeque<>();

    public void push(int x) { inStack.push(x); }

    public int pop() {
        moveIfNeeded();
        return outStack.pop();
    }

    public int peek() {
        moveIfNeeded();
        return outStack.peek();
    }

    public boolean empty() { return inStack.isEmpty() && outStack.isEmpty(); }

    private void moveIfNeeded() {
        if (outStack.isEmpty()) {
            while (!inStack.isEmpty()) outStack.push(inStack.pop());
        }
    }
}
```

*Why this is O(1) amortized, not O(n) per operation:* every element is moved from `inStack` to `outStack` **exactly once** over its lifetime. So across `n` operations, total movement work is O(n), which averages to O(1) per operation — same amortized argument as the monotonic stack.

---

## PART 4 — THE MASTER FRAMEWORK: how to *decide* to use a stack

When you read a new problem, run this checklist in your head, in order:

```
1. Is there explicit nesting?  (brackets, tags, nested function calls,
   nested expressions) -> Stack (matching pattern)

2. Do I need to find, for each element, the nearest element to the
   left/right satisfying some comparison (greater/smaller/equal)?
   -> Monotonic Stack

3. Am I parsing/evaluating an expression with precedence or
   parenthesization? -> Stack (expression evaluation pattern)

4. Am I trying to convert a recursive solution into an iterative one,
   or explicitly simulate DFS / backtracking without recursion?
   -> Stack (explicit call-stack simulation)

5. Do I need O(1) access to "the min/max/aggregate of only what's
   currently active", where "active" grows and shrinks in LIFO order?
   -> Auxiliary stack running in parallel

6. Am I processing a sequence and need to "undo" or "look back" to
   the most recent unresolved/unmatched item?
   -> Stack, generically
```

If none of these fit, it's probably not a stack problem — check queue (FIFO / level-order / sliding window), two pointers, or a different structure instead.

### 4.1 The single unifying mental model

If you want ONE sentence to carry away:

> **A stack is the right tool whenever "the most recent thing that hasn't been resolved yet" is the thing you need next.**

- Bracket matching → most recent unmatched opener
- Monotonic stack → most recent unresolved "candidate" waiting for its next greater/smaller
- Expression eval → most recent unapplied operator / unclosed sub-expression
- DFS simulation → most recent unexplored branch
- Min stack → most recent "context" (previous min) to restore on pop

Every single stack problem is a variation of "track the most recent unresolved thing, and resolve it when new information arrives."

---

## PART 5 — COMPLEXITY & CORRECTNESS TOOLS

### 5.1 The amortized analysis argument (memorize this — it comes up in every monotonic stack interview)

Claim: a loop containing a `while` that pops from a stack, followed by one `push` per outer iteration, is **O(n)** overall, not O(n²).

Proof sketch: every element is pushed **at most once** (one push per outer loop iteration, n iterations → ≤ n pushes total). Every element is popped **at most once** (once popped, it's gone forever, can't be popped again). So total pushes + pops ≤ 2n = O(n). The `while` loop "looks" like nested loops, but the *total* work across ALL iterations of the outer loop is bounded by 2n, not n².

### 5.2 Space complexity

Almost all stack-based solutions use O(n) auxiliary space for the stack itself (worst case: a strictly increasing or decreasing array pushes everything without ever popping). This is usually acceptable and is the trade-off you make for O(n) time instead of O(n²).

### 5.3 Common bugs / pitfalls

```
1. Off-by-one on width calculations (Largest Rectangle in Histogram) —
   always double check with a small hand example.

2. Forgetting to handle the "stack still has leftovers at the end"
   case — e.g., in Next Greater Element, remaining elements get -1;
   in Valid Parentheses, a non-empty stack at the end means invalid.

3. Using '<' vs '<=' (or '>' vs '>=') in the monotonic stack's while
   condition — this determines whether EQUAL elements are treated as
   "greater" or not, which matters for problems with duplicates
   (e.g., "next greater" vs "next greater or equal").

4. Storing values instead of indices when you'll need the index later
   (distance, span, width calculations all need indices).

5. Pushing right child before left child (or vice versa) incorrectly
   in iterative tree traversal — always trace through a 3-node example.

6. Null/empty checks — always check isEmpty() before pop()/peek() to
   avoid NoSuchElementException with ArrayDeque.
```

---

## PART 6 — PRACTICE ROADMAP (ordered by difficulty, same pattern grouped together)

**Tier 1 — Matching/Balancing (do these first, they build the "push opener, pop on closer" reflex)**
1. Valid Parentheses
2. Minimum Add to Make Parentheses Valid
3. Remove Outermost Parentheses
4. Baseball Game (simple stack simulation)

**Tier 2 — Monotonic Stack (the core skill — spend the most time here)**
5. Next Greater Element I & II (II wraps around — circular array trick: iterate `2*n` times using `i % n`)
6. Daily Temperatures
7. Online Stock Span
8. Remove Duplicate Letters / Smallest Subsequence of Distinct Characters (greedy + monotonic stack)
9. 132 Pattern (advanced monotonic stack)
10. Largest Rectangle in Histogram
11. Maximal Rectangle (2D version — run histogram solution per row)
12. Trapping Rain Water (stack version, then compare with two-pointer version)
13. Sum of Subarray Minimums

**Tier 3 — Expression Evaluation**
14. Evaluate Reverse Polish Notation
15. Basic Calculator II (no parens, +,-,*,/)
16. Basic Calculator (with parens, +,-)
17. Decode String
18. Simplify Path

**Tier 4 — Stack-as-call-stack (iterative recursion)**
19. Binary Tree Preorder/Inorder/Postorder Traversal (Iterative)
20. Flatten Nested List Iterator
21. Clone Graph (iterative)
22. Asteroid Collision (a beautiful "two-item-collision" stack problem — each new asteroid may destroy or be destroyed by the stack top, in a loop)

**Tier 5 — Design problems**
23. Min Stack
24. Implement Queue using Stacks
25. Implement Stack using Queues
26. Design a Stack With Increment Operation

**Tier 6 — Hard / mixed patterns**
27. Longest Valid Parentheses (DP-flavored but has an elegant stack solution)
28. Basic Calculator III (parens + precedence + unary minus — combines everything)
29. Maximum Frequency Stack
30. Exclusive Time of Functions (call-stack simulation with timestamps)

**How to practice each one (don't skip this):**
1. Before coding, say out loud which of the 6 signals from Part 4 applies.
2. Draw the stack state by hand (ASCII, like the examples above) for a small input.
3. Code it.
4. After solving, ask: "what's the invariant the stack maintains at every step?" (e.g., "the stack is always strictly decreasing"). Being able to state the invariant is the difference between memorizing and mastering.

---

## PART 7 — QUICK REFERENCE CARD

```
CORE IDEA:        LIFO — most recent unresolved thing, resolved first
OPERATIONS:       push, pop, peek, isEmpty — all O(1)
JAVA:             Deque<T> stack = new ArrayDeque<>();  (NOT java.util.Stack)

SIGNAL -> PATTERN
------------------
nested brackets/tags        -> push opener, pop+match on closer
"next/previous greater/     -> monotonic stack (store indices)
 smaller element"
expression w/ precedence    -> two stacks (operands+ops) or postfix eval
 or parens
convert recursion ->        -> explicit stack simulating call frames
 iteration
O(1) running min/max        -> auxiliary stack in lockstep with main stack
"undo to most recent        -> generic stack
 unresolved state"

MONOTONIC STACK CHEAT SHEET
----------------------------
Next Greater (right)    -> decreasing stack, pop while top < arr[i]
Next Smaller (right)    -> increasing stack, pop while top > arr[i]
Previous Greater (left) -> decreasing stack, pop while top <= arr[i]
Previous Smaller (left) -> increasing stack, pop while top >= arr[i]

AMORTIZED PROOF: each element pushed once, popped once -> O(n) total
```

---

You now have the full arc: **why** stacks exist (LIFO / nesting / "most recent unresolved"), **how** to build and use them in Java, **how to recognize** which of the five major patterns a new problem belongs to, and a **graded problem list** to turn recognition into reflex. The fastest path to mastery from here is deliberate practice on Tier 2 (monotonic stack) — it's the pattern with the highest interview frequency and the steepest "aha" curve once it clicks.

