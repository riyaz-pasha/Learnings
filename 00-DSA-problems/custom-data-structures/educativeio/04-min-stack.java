/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Design a custom stack class, Min Stack, allowing us to push, pop, and 
 * retrieve the minimum value in constant time O(1).
 * Implement: Constructor, Pop(), Push(value), Min Number().
 * Constraints: value in [-10^4, 10^4], at most 10^3 calls. 
 * Pop and Min Number are always called on non-empty stacks.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Can I use built-in structures like `java.util.Stack` or `ArrayDeque`, or should I build the stack from scratch?
 *    (Building it via a custom Linked List avoids dynamic resizing overhead and proves fundamental data structure knowledge).
 * 2. Is auxiliary space strictly constrained to O(1), or is O(N) extra space acceptable to achieve the O(1) time constraint?
 *    (O(N) space is the standard expectation. O(1) space is a mathematical trick that risks overflow, though safe with our specific constraints).
 * 3. Does `Pop()` need to return the removed value, or just remove it?
 *    (The prompt explicitly says "removes and returns").
 * 4. Can the stack contain duplicate minimums?
 *    (Yes, e.g., push(2), push(2). If we use a separate min-stack, we must ensure we don't pop the minimum prematurely).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement: A standard stack is blind to its contents. We need a way to 
 *   answer "What is the smallest item?" instantly, without scanning. The binding constraint 
 *   is the LIFO (Last-In-First-Out) nature of stacks: when we pop the current minimum, 
 *   we must instantly know what the *previous* minimum was.
 * 
 * --- Approach 1: Brute Force (Standard Stack + Linear Scan) ---
 * 1. Intuition: Just use a normal stack. When asked for the minimum, look at everything.
 * 2. Try: Use `ArrayList<Integer>`. Push and pop at the end. For `minNumber()`, loop through.
 * 3. Why it works: It perfectly maintains order and data.
 * 4. Why it's too costly: It violates the O(1) time requirement for the min operation.
 * 5. Bottleneck: Re-evaluating the entire history to find the minimum.
 * 6. Observation: Because it's a stack, history never changes. The elements below the top 
 *    element are frozen. The minimum of the bottom 3 elements will always be the same, 
 *    no matter what we push on top of them.
 * 
 * Time Complexity: Push/Pop: O(1). Min Number: O(N) — because we scan all elements.
 * Space Complexity: O(N) — because we store exactly N elements in the stack.
 * 
 * --- Approach 2: The Two-Stack Method (Data Stack + Min Stack) ---
 * 1. Intuition: Since history is frozen, let's keep a parallel record of history's minimums.
 * 2. Try: Maintain `Stack<Integer> data` and `Stack<Integer> mins`.
 *    `Push(x)` -> `data.push(x)`. If `mins` is empty or `x <= mins.peek()`, `mins.push(x)`.
 *    `Pop()` -> `popped = data.pop()`. If `popped == mins.peek()`, `mins.pop()`.
 * 3. Why it works: We only record when the minimum drops, and backtrack when that minimum is removed.
 * 4. Why it's suboptimal: Managing two separate data structures introduces synchronization 
 *    risks. Also, in Java, `Stack<Integer>` uses autoboxing, and `popped == mins.peek()` 
 *    compares memory references for numbers > 127, leading to subtle, lethal bugs unless 
 *    using `.equals()`.
 * 5. Bottleneck: Code complexity, potential autoboxing traps, and redundant parallel structures.
 * 6. Observation: Every element has exactly one "minimum state" at the time it was pushed. 
 *    Why separate them? We can fuse them together.
 * 
 * Time Complexity: O(1) for all methods — parallel pushing/popping.
 * Space Complexity: O(N) extra space — because in the worst case (strictly decreasing input), we store N elements in the min stack.
 * 
 * --- Approach 3: Combined State / Node Class (Optimal for Interviews) ---
 * 1. Intuition: Instead of pushing raw integers, push an object that remembers two things: 
 *    the value itself, and the minimum value of the stack *at the moment it was pushed*.
 * 2. Try: Create a `Node { int val; int currentMin; Node next; }`. 
 *    When pushing `x`, `currentMin = Math.min(x, previousNode.currentMin)`.
 * 3. Why it works: It perfectly encapsulates state. No synchronization needed, no autoboxing 
 *    bugs, and strictly O(1) time.
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It shows object-oriented design, avoids the Java `Integer` caching trap 
 *    entirely by using primitive `int` fields, and demonstrates system-level understanding 
 *    by avoiding dynamically-resizing array overhead.
 * 
 * Time Complexity: O(1) for all methods — pointer reassignment and simple primitive math.
 * Space Complexity: O(N) — because we store exactly N Node objects, carrying the O(1) overhead of the `min` variable per node.
 * 
 * --- Approach 4: The O(1) Space Mathematical Trick (For discussion only) ---
 * 1. Intuition: Can we do this without storing a `min` for every element?
 * 2. Try: Maintain a single `long min` variable. In the stack, store `encoded_val = val - min`.
 *    If `val < min`, we push the negative encoded value and update `min = val`. 
 *    When popping, if the stored value is negative, we know we are popping the minimum, 
 *    and we can reconstruct the *previous* minimum using math.
 * 3. Trade-off: It requires O(1) auxiliary space, but the math can overflow standard 32-bit 
 *    integers if constraints are large. It's brilliant but often over-engineered and risky 
 *    under interview pressure. Approach 3 is universally safer.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Pushing duplicates of the minimum: e.g., Push(2), Push(2). The `currentMin` simply 
 *   stays 2 for both nodes. Popping one doesn't break the min for the other.
 * - Negative inputs: Works identically because the Math.min() logic handles negatives 
 *   perfectly without underflow risks (unlike Approach 4's math tricks).
 * - Ascending sequences: Every node simply points back to the bottom node's value as its min.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & PITFALLS
 * ============================================================================
 * > Key Insight: "In a LIFO structure, the past is immutable." 
 *   The state of the stack (and its minimum) below the top element can NEVER be altered 
 *   by future operations. Therefore, we can permanently cache the answer to "What is the 
 *   minimum?" directly onto the element itself at the moment it arrives.
 * 
 * Diagram: Node Stack State
 * Action: Push(5), Push(7), Push(2), Push(3)
 * 
 * | TOP  | val: 3, min: 2 | -> next: (Node with val 2)
 * |      | val: 2, min: 2 | -> next: (Node with val 7)
 * |      | val: 7, min: 5 | -> next: (Node with val 5)
 * | BTM  | val: 5, min: 5 | -> next: null
 * 
 * Notice that when '3' is popped, the new head becomes the '2' node, which intrinsically 
 * knows its min is 2. When '2' is popped, the new head is '7', which intrinsically knows 
 * its min is 5.
 * 
 * Pitfalls:
 * - The Java Autoboxing Trap: If using two `Stack<Integer>`, writing `if (popped == minStack.peek())` 
 *   will FAIL for numbers outside [-128, 127] because `==` compares object references, 
 *   not integer values. You must use `.equals()`. By building our own `Node` with primitive `int`, 
 *   we completely sidestep this Java-specific landmine.
 * 
 * Pattern Recognition: "When you see 'Min/Max in O(1) with history', think State-Caching Nodes."
 * 
 * Interview Script snippet:
 * "To achieve O(1) time across all operations, I can't search for the minimum dynamically. 
 * Since a stack is LIFO, an element's minimum context never changes once it's pushed. 
 * Therefore, I'll build a custom linked list where each Node stores its value AND the minimum 
 * value of the stack at that exact depth. This gives us strict O(1) time and avoids the 
 * overhead and autoboxing traps of standard Java Collections, keeping space strictly O(N)."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: How would you add a `Max Number()` method in O(1) time?
 * A1: Trivial with the Node approach. We simply add an `int max;` field to the Node class. 
 *     During push, `currentMax = Math.max(x, previousNode.max)`.
 * 
 * Q2: What if memory is extremely constrained and we can't afford the O(N) extra space for nodes?
 * A2: We can use the Delta Encoding trick (Approach 4). We keep a primitive `int[]` stack and a 
 *     single `long currentMin`. We push the difference `val - currentMin`. If the difference is 
 *     negative, we update `currentMin`. On pop, a negative stored value tells us how to restore 
 *     the previous minimum. It takes O(1) auxiliary space, though it risks numerical overflow 
 *     if constraints exceed 32-bit limits.
 */

public class MinStackStudy {

    /**
     * Our custom Node class representing an element in the stack.
     * By storing 'min' directly on the node, we fuse the data and the state.
     */
    private static class Node {
        int val;
        int min;      // The smallest value in the stack from this node down to the bottom
        Node next;

        private Node(int val, int min, Node next) {
            this.val = val;
            this.min = min;
            this.next = next;
        }
    }

    // The top of our stack
    private Node head;

    /**
     * Constructor: Initializes the Min Stack object.
     */
    public MinStackStudy() {
        // Explicitly set to null, though Java does this by default.
        head = null;
    }

    /**
     * Pushes the provided value onto the stack.
     * Time: O(1) - single object instantiation and pointer assignment.
     */
    public void push(int value) {
        if (head == null) {
            // First element: it is intrinsically its own minimum.
            head = new Node(value, value, null);
        } else {
            // Compare the incoming value with the current minimum of the stack.
            // The new node's minimum state is locked in permanently.
            int currentMin = Math.min(value, head.min);
            
            // Push it to the top (linked list prepend)
            head = new Node(value, currentMin, head);
        }
    }

    /**
     * Removes and returns from the stack the value that was most recently pushed onto it.
     * Time: O(1) - simple pointer reassignment.
     */
    public int pop() {
        // The prompt guarantees pop() is always called on non-empty stacks,
        // so we don't need to write 'if (head == null) throw new EmptyStackException();'
        int poppedValue = head.val;
        
        // Move the head pointer down. The garbage collector will clean up the old head.
        // Crucially, the new head already has the correct 'min' state for its level!
        head = head.next;
        
        return poppedValue;
    }

    /**
     * Returns the minimum value in the stack in O(1) time.
     */
    public int minNumber() {
        // The prompt guarantees minNumber() is called on non-empty stacks.
        // We just read the cached state. No scanning required.
        return head.min;
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        MinStackStudy minStack = new MinStackStudy();

        // 1. Basic progression
        minStack.push(-2);
        minStack.push(0);
        minStack.push(-3);
        
        // Stack top -> [-3, min=-3] -> [0, min=-2] -> [-2, min=-2]
        
        System.out.println("Current Min: " + minStack.minNumber()); // Expected: -3
        
        // 2. Popping the minimum restores the previous minimum
        int popped = minStack.pop(); 
        System.out.println("Popped: " + popped); // Expected: -3
        System.out.println("Min after pop: " + minStack.minNumber()); // Expected: -2
        
        // 3. Duplicates Edge Case
        minStack.push(-2);
        System.out.println("Min after pushing duplicate -2: " + minStack.minNumber()); // Expected: -2
        minStack.pop();
        System.out.println("Min after popping duplicate -2: " + minStack.minNumber()); // Expected: -2
        
        // 4. Large values testing boundary
        minStack.push(10000);
        System.out.println("Min after pushing 10000: " + minStack.minNumber()); // Expected: -2
    }
}

/**
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core Pattern: LIFO State Caching (Fused Node).
 * Key Observation: Because the stack is Last-In-First-Out, elements below the top 
 * never change. Therefore, an element's minimum context is permanent from the moment 
 * it is pushed until it is popped. 
 * What to Memorize: `class Node { int val; int min; Node next; }`.
 * Most Common Trap: Using two `Stack<Integer>` objects and writing `popped == mins.peek()`. 
 * This triggers object reference comparison for numbers > 127 and silently fails. 
 * Mental Trigger: "O(1) Min History" -> "Linked List with Min recorded inside every Node".
 * ============================================================================
 */






import java.util.ArrayDeque;
import java.util.Deque;

class MinStackWithRecords {

    // Every entry remembers:
    // 1. The actual value pushed.
    // 2. The minimum of the stack at the time it was pushed.
    private record Entry(int value, int minSoFar) {}

    private final Deque<Entry> stack = new ArrayDeque<>();

    // Constructor: O(1) time and O(1) initial space.
    public MinStackWithRecords() {
    }

    // Push: O(1) time and O(1) additional space for the new entry.
    public void push(int value) {

        // If the stack is empty, the new value is the minimum.
        // Otherwise, compare the new value with the previous top's minimum.
        int minSoFar = stack.isEmpty()
                ? value
                : Math.min(value, stack.peek().minSoFar());

        // Save the value and its minimum together.
        // This preserves the previous minimum information even when
        // the new value is larger than the current minimum.
        stack.push(new Entry(value, minSoFar));
    }

    // Pop: O(1) time.
    // Removing one entry also removes its saved minimum snapshot.
    // The problem guarantees that the stack is non-empty.
    public int pop() {
        return stack.pop().value();
    }

    // getMin: O(1) time.
    // The top entry contains the minimum of the entire current stack.
    public int getMin() {
        return stack.peek().minSoFar();
    }
}

