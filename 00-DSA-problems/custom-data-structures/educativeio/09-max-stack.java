/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a custom stack class, Max Stack, that supports basic stack operations 
 * and can find/remove the maximum element.
 * 
 * Implement the following methods:
 * - Constructor: Initializes the object.
 * - Push(x): Pushes x onto the stack.
 * - Pop(): Removes and returns the top element.
 * - Top(): Returns the top element without removing it.
 * - peekMax(): Returns the maximum element without removing it.
 * - popMax(): Removes and returns the maximum element. If duplicates exist, 
 *   removes the most recently added one (topmost).
 * 
 * Constraints: -1000 <= x <= 1000. Max 100 calls. Pop/Top/peekMax/popMax 
 * called only on non-empty stacks.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. How should we handle multiple instances of the maximum value during `popMax()`?
 *    (The prompt specifies removing the "most recently added one". This means we search from the top down).
 * 2. Is `popMax` called frequently enough to require strict O(1) or O(log N) time?
 *    (With only 100 total operations, an O(N) `popMax` is acceptable and prevents over-engineering, but in production, we'd need O(log N)).
 * 3. Can we use standard Java collections, or must we build the underlying linked list from scratch?
 *    (Standard `Stack` is fine for the array-based approach, but we must understand its limitations).
 * 4. Can the stack contain negative numbers?
 *    (Yes, constraints say down to -1000. We shouldn't use 0 as a default max).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: A standard stack is LIFO. Asking for the "maximum" violates 
 *   LIFO because the maximum can be buried anywhere in the stack. Removing it (`popMax`) 
 *   forces us to extract an element from the middle while preserving the order of everything else.
 * 
 * --- Approach 1: Single Stack + Linear Search (Brute Force) ---
 * 1. Intuition: Just keep a normal stack. When someone asks for the max, scan everything.
 * 2. Try: Use a `LinkedList<Integer>`. `Push` and `Pop` are normal. `peekMax` iterates through 
 *    the list to find the highest value. `popMax` iterates to find it, removes it via iterator, 
 *    and returns it.
 * 3. Why it works: A LinkedList allows removing from the middle once you find the node.
 * 4. Why it's suboptimal: `peekMax` takes O(N) time. We are throwing away the "caching" trick 
 *    that makes standard Min/Max stacks fast.
 * 5. Bottleneck: Re-discovering the maximum element on every single `peekMax` call.
 * 
 * Time Complexity: Push/Pop/Top: O(1). peekMax: O(N). popMax: O(N) — scanning to find max.
 * Space Complexity: O(N) — storing N elements.
 * 
 * --- Approach 2: Two Stacks (Optimal for Small N / Interviews) ---
 * 1. Intuition: We can cache the history of the maximums exactly like a Min Stack.
 * 2. Try: Maintain `Stack<Integer> data` and `Stack<Integer> maxes`. 
 *    `Push(x)` -> push `x` to data. Push `Math.max(x, maxes.peek())` to maxes.
 *    `peekMax()` -> return `maxes.peek()`. O(1)!
 *    `popMax()` -> We know *what* the max is in O(1). But we have to dig it out. We pop elements 
 *    from `data` into a temporary buffer until we hit the max. Pop the max. Then push the buffer 
 *    back onto the stack to restore order.
 * 3. Why it works: It gives strict O(1) for 4 out of 5 operations. Only `popMax` takes a hit.
 * 4. Cost checks: Given the constraint of 100 calls, O(N) for `popMax` is instantaneous.
 * 
 * Time Complexity: Push/Pop/Top/peekMax: O(1). popMax: O(N) — digging through the stack into a buffer and pushing back.
 * Space Complexity: O(N) — maintaining the parallel max stack and temporary buffer.
 * 
 * --- Approach 3: Doubly Linked List + TreeMap (Production Optimal) ---
 * 1. Intuition: If `popMax` is called millions of times, O(N) is too slow. We need O(1) removal 
 *    from the middle (Doubly Linked List) and O(log N) access to the maximum element (Balanced BST).
 * 2. Try: `TreeMap<Integer, List<Node>>` maps a value to a list of its DLL nodes.
 *    `Push` adds to DLL tail and TreeMap (O(log N)).
 *    `popMax` asks TreeMap for the highest key, gets the last node in its list, and unlinks it 
 *    from the DLL directly in O(1).
 * 3. Trade-off: While brilliant, it takes 150+ lines of dense, error-prone code. For N=100, 
 *    Approach 2 is much safer to write in a 45-minute interview while discussing Approach 3 out loud.
 * 
 * Time Complexity (Approach 3): O(log N) for Push, Pop, popMax. O(1) for Top, peekMax.
 * Space Complexity (Approach 3): O(N).
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Duplicate Maximums: `Push(5), Push(5)`. `popMax()` must remove the top 5, leaving the bottom 5. 
 *   (Approach 2 handles this naturally because it stops digging at the first match from the top).
 * - `popMax` on the very bottom element: The buffer will hold everything except the bottom element, 
 *   forcing us to push N-1 elements back. Worst-case O(N).
 * - All Negative Values: Handled correctly since we don't rely on `0` as a default max.
 * 
 * ============================================================================
 * 4. ## Key Insights, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Extracting from a stack requires peeling." 
 *   You cannot extract from the middle of a stack structure without dismantling the top. 
 *   By using a temporary buffer, you can peel away the layers, remove the target, and rebuild the layers.
 * 
 * ASCII DRY RUN (popMax operation via Two Stacks)
 * Stack pushes: 5, 1, 7, 2
 * Data Stack: [5, 1, 7, 2] <- TOP
 * Max Stack:  [5, 5, 7, 7] <- TOP
 * 
 * Call: popMax()
 * 1. peekMax() returns 7.
 * 2. Digging...
 *    - top() is 2. 2 != 7. Pop 2 into buffer.
 *    - Data Stack: [5, 1, 7]
 *    - Buffer: [2]
 *    - top() is 7. 7 == 7. Match found!
 * 3. Pop the 7 and discard it.
 *    - Data Stack: [5, 1]
 * 4. Rebuild from buffer...
 *    - Buffer pops 2. We call standard `Push(2)`.
 *    - Data Stack: [5, 1, 2]
 *    - Max Stack updates naturally via the Push method: [5, 5, 5]
 * 
 * Pitfalls:
 * - Rebuilding the stack manually instead of using your own `Push` method. 
 *   If you just do `data.push(buffer.pop())`, you forget to update the `maxStack`! 
 *   Always call `this.push(val)` when restoring from the buffer.
 * - Comparing Integer wrappers with `==`. In Java, `Integer` values outside [-128, 127] 
 *   compare by reference. If you use `java.util.Stack<Integer>`, use `.equals()` or cast 
 *   to `int` before comparing the popped value with the max.
 * 
 * Pattern Recognition: "Modify standard LIFO" -> Buffer Stack to peel and rebuild.
 * 
 * Interview Script snippet:
 * "Since the constraints are very small (100 calls), implementing a balanced binary search 
 * tree with a doubly linked list is overkill and risks bugs. I'll use the Two Stacks approach. 
 * I'll cache the maximum at every level in a parallel stack, giving O(1) for everything except 
 * `popMax`. For `popMax`, I'll use a temporary stack to peel off the top elements until I hit 
 * the maximum, remove it, and then push the temporary elements back."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: How would you optimize `popMax` to run faster than O(N)?
 * A1: I would abandon standard Stacks. I'd build a Doubly Linked List to represent the stack 
 *     so I can remove nodes in O(1) time. To find the maximums, I'd use a `TreeMap` mapping values 
 *     to lists of DLL nodes. `popMax` would find the max in O(log N), grab its node, and sever 
 *     it from the DLL in O(1).
 * 
 * Q2: What if we want to remove the *bottom-most* maximum instead of the top-most?
 * A2: The Two Stacks peeling method fails because it stops at the first top-most match. 
 *     We'd have to peel all the way to the bottom to track the last seen max, which makes 
 *     every `popMax` strictly O(N) even if the max is at the top. In the TreeMap+DLL approach, 
 *     we'd just pluck the first node from the TreeMap's value list instead of the last.
 */

import java.util.Stack;

public class MaxStackStudy {

    static class MaxStack {
        // Holds the actual elements
        private Stack<Integer> stack;
        // Holds the maximum value seen up to the corresponding depth in 'stack'
        private Stack<Integer> maxStack;

        /** 
         * Constructor: Initializes the Max Stack object. 
         */
        public MaxStack() {
            stack = new Stack<>();
            maxStack = new Stack<>();
        }

        /** 
         * Pushes the provided element, x, onto the stack.
         * Time: O(1)
         */
        public void push(int x) {
            // Determine the new maximum at this depth
            int currentMax = x;
            if (!maxStack.isEmpty()) {
                currentMax = Math.max(x, maxStack.peek());
            }
            
            stack.push(x);
            maxStack.push(currentMax);
        }

        /** 
         * Removes and returns the element on the top of the stack.
         * Time: O(1)
         */
        public int pop() {
            // Keep the parallel stacks in sync
            maxStack.pop();
            return stack.pop();
        }

        /** 
         * Retrieves the most recently added element on the top of the stack without removing it.
         * Time: O(1)
         */
        public int top() {
            return stack.peek();
        }

        /** 
         * Retrieves the maximum element in the stack without removing it.
         * Time: O(1)
         */
        public int peekMax() {
            return maxStack.peek();
        }

        /** 
         * Retrieves the maximum element in the stack and removes it. 
         * If there is more than one maximum, removes the most recently added one.
         * Time: O(N) worst case, O(1) best case.
         */
        public int popMax() {
            // We know the value we are looking for in O(1)
            int max = peekMax();
            
            // Buffer to hold elements peeled off the top
            Stack<Integer> buffer = new Stack<>();
            
            // Peel off elements until we hit the top-most max
            // Note: Using top() == max relies on unboxing to primitive 'int', 
            // completely avoiding the Java Integer object reference '==' trap.
            while (top() != max) {
                buffer.push(pop());
            }
            
            // Remove the actual maximum element
            pop(); 
            
            // Rebuild the stack
            // CRITICAL: We must use our custom push() method, NOT stack.push().
            // This ensures the maxStack recalculates the maximums correctly for the rebuilt elements.
            while (!buffer.isEmpty()) {
                push(buffer.pop());
            }
            
            return max;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        MaxStack maxStack = new MaxStack();

        // 1. Basic pushes
        maxStack.push(5);
        maxStack.push(1);
        maxStack.push(5);
        
        System.out.println("Top: " + maxStack.top());         // Expected: 5
        System.out.println("PeekMax: " + maxStack.peekMax()); // Expected: 5
        
        // 2. Duplicate Max Edge Case
        // Should remove the TOP 5, leaving the bottom 5.
        System.out.println("PopMax: " + maxStack.popMax());   // Expected: 5
        System.out.println("Top after PopMax: " + maxStack.top()); // Expected: 1
        System.out.println("PeekMax after PopMax: " + maxStack.peekMax()); // Expected: 5
        
        // 3. Digging via PopMax
        maxStack.push(7);
        maxStack.push(2);
        // Stack state (top to bottom): 2, 7, 1, 5
        
        System.out.println("PeekMax: " + maxStack.peekMax()); // Expected: 7
        // Removes 7. Buffer catches 2, then pushes 2 back.
        System.out.println("PopMax (digging): " + maxStack.popMax()); // Expected: 7
        
        // Ensure 2 is back on top
        System.out.println("Top after digging: " + maxStack.top()); // Expected: 2
        
        // Ensure max is properly updated to 5
        System.out.println("PeekMax after digging: " + maxStack.peekMax()); // Expected: 5
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Dual Stacks + Buffer Rebuild.
 * Key Observation: Removing from the middle of a stack breaks LIFO. We can simulate 
 * middle-removal by popping into a temporary buffer and restoring, allowing us to leverage 
 * the simplicity of parallel state stacks.
 * What to Memorize: Always use `this.push(buffer.pop())` when rebuilding, NOT `stack.push()`, 
 * so the `maxStack` recalculates correctly.
 * Most Common Trap: Object reference comparison. If comparing objects returned by `peek()`, 
 * `Integer` values outside [-128, 127] will fail `==`. Always unbox to primitive `int`.
 * Mental Trigger: "Extract from middle of stack" -> "Peel into temp buffer and push back".
 * ============================================================================
 */




import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

public class MaxStack {

    // Each node represents one occurrence of a value.
    // A node is a reference to a specific occurrence, not just its value.
    private static final class Node {
        final int value;
        Node prev;
        Node next;

        Node(int value) {
            this.value = value;
        }
    }

    // Doubly linked list sentinels simplify insertion and removal:
    //
    // head <-> bottom ... top <-> tail
    //
    // The actual elements lie between head and tail.
    private final Node head = new Node(0);
    private final Node tail = new Node(0);

    // Keys are sorted, so lastKey() identifies the maximum value.
    //
    // For each value, its deque stores nodes in insertion order.
    // The last node is the most recently pushed remaining occurrence.
    private final TreeMap<Integer, Deque<Node>> nodesByValue =
            new TreeMap<>();

    // Constructor: connect the two sentinels.
    // Time: O(1). Auxiliary space: O(1).
    public MaxStack() {
        head.next = tail;
        tail.prev = head;
    }

    // Insert a new node immediately before tail, which is the stack top.
    // Also append the node to its value's occurrence deque.
    //
    // TreeMap insertion takes O(log d), where d is the number of distinct
    // values; linked-list insertion and deque insertion take O(1).
    // Total time: O(log d). Auxiliary space: O(1) per new element,
    // with O(n) total space for all nodes and index entries.
    public void push(int x) {
        Node node = new Node(x);

        // Insert at the top of the stack.
        node.prev = tail.prev;
        node.next = tail;
        tail.prev.next = node;
        tail.prev = node;

        // Record this occurrence after all earlier occurrences of x.
        nodesByValue
                .computeIfAbsent(x, ignored -> new ArrayDeque<>())
                .addLast(node);
    }

    // Remove and return the current top.
    //
    // The top node is also the newest remaining occurrence of its value,
    // so it must be the last node in that value's deque.
    //
    // Linked-list unlinking and deque removal are O(1); TreeMap removal
    // of a key, when its final occurrence disappears, is O(log d).
    // Total time: O(log d) worst case.
    // Auxiliary space: O(1), excluding internal map rebalancing.
    public int pop() {
        Node node = tail.prev;
        removeNode(node);
        return node.value;
    }

    // Read the node immediately before tail.
    // Time: O(1). Auxiliary space: O(1).
    public int top() {
        return tail.prev.value;
    }

    // Return the greatest key in the sorted map.
    // TreeMap.lastKey() takes O(log d) in the general implementation.
    // Time: O(log d). Auxiliary space: O(1).
    public int peekMax() {
        return nodesByValue.lastKey();
    }

    // Remove the most recently pushed occurrence of the maximum value.
    //
    // 1. Find the greatest value through lastKey().
    // 2. Get the newest node for that value through peekLast().
    // 3. Remove that node from both data structures.
    //
    // Unlike the two-stack approach, no elements above the maximum
    // need to be moved or restored.
    //
    // Time: O(log d) worst case.
    // Auxiliary space: O(1) beyond the existing data structures.
    public int popMax() {
        Map.Entry<Integer, Deque<Node>> maxEntry =
                nodesByValue.lastEntry();

        int maxValue = maxEntry.getKey();
        Node node = maxEntry.getValue().peekLast();

        removeNode(node);

        return maxValue;
    }

    // Remove a known node from both the linked list and the TreeMap index.
    //
    // Because the node is already known, linked-list unlinking takes O(1).
    // The node must be the last occurrence of its value:
    // - pop() removes the global top, which is the newest occurrence
    //   of its value.
    // - popMax() explicitly selects the newest occurrence of the max.
    //
    // Thus pollLast() removes the correct node from the occurrence deque.
    // If no occurrence remains, remove the key from the TreeMap.
    //
    // Time: O(log d) worst case because removing a TreeMap key can rebalance
    // the tree. The list and deque operations themselves are O(1).
    // Auxiliary space: O(1).
    private void removeNode(Node node) {
        // Unlink the node from the stack's order.
        node.prev.next = node.next;
        node.next.prev = node.prev;

        // Remove this occurrence from its value's index.
        Deque<Node> occurrences = nodesByValue.get(node.value);
        occurrences.pollLast();

        // Do not retain keys for values that no longer exist in the stack.
        if (occurrences.isEmpty()) {
            nodesByValue.remove(node.value);
        }
    }
}

