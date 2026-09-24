/**
 * ============================================================================
 * FLATTEN NESTED LIST ITERATOR - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: You are given a nested list of integers. Each element is either 
 * an integer or a list whose elements may also be integers or other lists.
 * You need to implement an iterator to flatten it. The iterator should have 
 * `hasNext()` and `next()` methods.
 * 
 * Simple Explanation for Interviewer:
 * "We are given a structure that looks like a tree, where the leaves are 
 * integers and the internal nodes are lists. Our goal is to traverse this 
 * tree and yield the integers one by one from left to right, exactly like 
 * reading the leaves of a tree in-order."
 * 
 * Core Idea & Intuition:
 * There are two fundamental ways to solve this:
 * 1. Eager Evaluation (Pre-computation): Traverse the entire nested list 
 *    in the constructor and store all the integers in a flat list. Then, 
 *    just iterate over that flat list.
 * 2. Lazy Evaluation (Stack-based): We only flatten what we need, exactly 
 *    when we need it. By using a Stack, we can pause and resume our 
 *    traversal. Since it's a list (which we read front-to-back) but a 
 *    stack is Last-In-First-Out (LIFO), we must push elements onto the 
 *    stack in reverse order.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can the nested list be completely empty, or contain only empty lists? e.g., `[[], [[]]]`"
 *    Why: This is the most critical edge case. If all nested lists are empty, 
 *    `hasNext()` must reliably return `false`.
 * 
 * 2. Q: "Are we allowed to modify the input nested list?"
 *    Why: If yes, we could potentially pop elements off the original lists to 
 *    save space, but usually, we shouldn't mutate input data.
 * 
 * 3. Q: "Is the data extremely large or infinite?"
 *    Why: If the data stream is massive or infinite (like reading a huge JSON), 
 *    the Pre-computation approach will cause an OutOfMemoryError. Lazy evaluation 
 *    is strictly required.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: [[1,1], 2, [1,1]]
 * Output: [1, 1, 2, 1, 1]
 * 
 * Visualizing the Lazy Evaluation (Stack) Approach:
 * Nested List: [[1,1], 2]
 * 
 * Init: Push all elements in REVERSE order.
 * Stack: [ 2, [1,1] ]  <-- Top is [1,1]
 * 
 * call hasNext(): 
 * - Top is [1,1] (a list). Pop it. 
 * - Push its elements in reverse order. 
 * - Stack: [ 2, 1, 1 ] <-- Top is 1 (an integer). Return true.
 * 
 * call next():
 * - Pop top. Returns 1. Stack: [ 2, 1 ]
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Acknowledge the tree-like structure of the nested lists.
 * 2. Clarify: Emphasize the edge case of lists containing empty lists.
 * 3. Brute Force (Eager): Explain flattening the entire structure upfront in the 
 *    constructor. Mention it takes O(N) time initially and O(N) space.
 * 4. Optimize (Lazy): Explain why Pre-computation is bad for large data/streams. 
 *    Propose the Stack approach.
 * 5. Stack Logic: Explain the trick of pushing elements in reverse order to 
 *    preserve the left-to-right reading order in a LIFO structure.
 * 6. Code: Write the Lazy evaluation code. 
 * 7. Dry-run: Trace `[[], 1]` to show why unpacking MUST happen inside `hasNext()` 
 *    and not `next()`.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Empty lists at the start: `[[], [], 1]` -> `hasNext()` must loop and unpack 
 *   until it finds the `1`.
 * - Purely empty structure: `[[[]]]` -> `hasNext()` should return false.
 * - Calling `next()` without calling `hasNext()` first.
 * 
 * Common Mistakes:
 * - Doing the unpacking inside `next()` instead of `hasNext()`. 
 *   If you unpack in `next()`, `hasNext()` might incorrectly return `true` 
 *   just because the stack isn't empty, even if the stack only contains empty lists!
 *   Rule of thumb: `hasNext()` must guarantee the top of the stack is an Integer.
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class FlattenNestedListIterator {

    public static void main(String[] args) {
        // Constructing a test case: [[1,1], 2, [1,1]]
        List<NestedInteger> nestedList = new ArrayList<>();
        
        List<NestedInteger> sublist1 = new ArrayList<>();
        sublist1.add(new MockNestedInteger(1));
        sublist1.add(new MockNestedInteger(1));
        
        List<NestedInteger> sublist2 = new ArrayList<>();
        sublist2.add(new MockNestedInteger(1));
        sublist2.add(new MockNestedInteger(1));
        
        nestedList.add(new MockNestedInteger(sublist1));
        nestedList.add(new MockNestedInteger(2));
        nestedList.add(new MockNestedInteger(sublist2));

        System.out.println("--- Precomputed (Eager) Approach ---");
        NestedIteratorPrecomputed eagerIterator = new NestedIteratorPrecomputed(nestedList);
        while (eagerIterator.hasNext()) {
            System.out.print(eagerIterator.next() + " ");
        }
        System.out.println();

        System.out.println("\n--- Stack-based (Lazy) Approach ---");
        NestedIteratorLazy lazyIterator = new NestedIteratorLazy(nestedList);
        while (lazyIterator.hasNext()) {
            System.out.print(lazyIterator.next() + " ");
        }
        System.out.println();
        
        // Edge Case Test: [[], [[]]] (Completely empty)
        List<NestedInteger> emptyEdgeCase = new ArrayList<>();
        List<NestedInteger> empty1 = new ArrayList<>();
        List<NestedInteger> empty2 = new ArrayList<>();
        empty2.add(new MockNestedInteger(new ArrayList<>())); // [[]]
        emptyEdgeCase.add(new MockNestedInteger(empty1));
        emptyEdgeCase.add(new MockNestedInteger(empty2));
        
        System.out.println("\n--- Edge Case: Purely empty nested lists ---");
        NestedIteratorLazy edgeIterator = new NestedIteratorLazy(emptyEdgeCase);
        System.out.println("Has Next? " + edgeIterator.hasNext()); // Expected: false
    }

    /**
     * SOLUTION 1: PRECOMPUTED (EAGER EVALUATION)
     * ------------------------------------------------------------------------
     * Idea: Traverse the entire nested list in the constructor using DFS.
     * Add all integers to a standard List. The iterator methods just iterate 
     * over this pre-built list.
     * 
     * Time Complexity: 
     * - Constructor: O(N) where N is the total number of integers and lists.
     * - next() / hasNext(): O(1)
     * Space Complexity: O(N) to hold all integers in memory simultaneously.
     * 
     * Trade-offs: Very easy to implement. But terrible if the nested list is 
     * huge, as it loads everything into memory upfront, completely defeating 
     * the purpose of an "iterator".
     */
    static class NestedIteratorPrecomputed implements Iterator<Integer> {
        private List<Integer> flattenedList;
        private int currentIndex;

        public NestedIteratorPrecomputed(List<NestedInteger> nestedList) {
            flattenedList = new ArrayList<>();
            currentIndex = 0;
            flatten(nestedList);
        }

        private void flatten(List<NestedInteger> nestedList) {
            for (NestedInteger ni : nestedList) {
                if (ni.isInteger()) {
                    flattenedList.add(ni.getInteger());
                } else {
                    flatten(ni.getList());
                }
            }
        }

        @Override
        public Integer next() {
            if (!hasNext()) throw new NoSuchElementException();
            return flattenedList.get(currentIndex++);
        }

        @Override
        public boolean hasNext() {
            return currentIndex < flattenedList.size();
        }
    }

    /**
     * SOLUTION 2: STACK-BASED (LAZY EVALUATION) -> OPTIMAL
     * ------------------------------------------------------------------------
     * Idea: We use a Stack to evaluate the list on the fly. We push elements 
     * from the back of the list to the front, so the front element sits at 
     * the top of the stack.
     * 
     * Crucially, the "unpacking" logic lives inside `hasNext()`. `hasNext()` 
     * peeks at the top of the stack. If it's a list, it pops it and pushes 
     * its elements (again, in reverse order). It repeats this until the top 
     * element is an Integer (returning true) or the stack is empty (returning false).
     * 
     * Time Complexity:
     * - Constructor: O(V) where V is the size of the top-level list.
     * - hasNext(): Amortized O(1). We might do multiple pops/pushes for empty lists, 
     *   but each element/list is pushed and popped at most once overall.
     * - next(): O(1)
     * 
     * Space Complexity: O(D) where D is the maximum nesting depth. We only 
     * store the current path of execution, not the entire list of integers.
     */
    static class NestedIteratorLazy implements Iterator<Integer> {
        // Use Deque as a Stack. ArrayDeque is faster than java.util.Stack.
        private Deque<NestedInteger> stack;

        public NestedIteratorLazy(List<NestedInteger> nestedList) {
            stack = new ArrayDeque<>();
            prepareStack(nestedList);
        }

        // Helper to push elements of a list in REVERSE order.
        // Pushing backwards ensures the first element is at the TOP of the stack.
        private void prepareStack(List<NestedInteger> nestedList) {
            for (int i = nestedList.size() - 1; i >= 0; i--) {
                stack.push(nestedList.get(i));
            }
        }

        @Override
        public Integer next() {
            // Guarantee that the top of the stack is an Integer before popping.
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            // hasNext() already guaranteed the top is an integer. Safe to pop.
            return stack.pop().getInteger();
        }

        @Override
        public boolean hasNext() {
            // Loop to unpack lists until we find an integer, or the stack empties.
            while (!stack.isEmpty()) {
                // If top is an integer, we are good to go!
                if (stack.peek().isInteger()) {
                    return true;
                }
                
                // Otherwise, the top is a list. We must unpack it.
                NestedInteger topList = stack.pop();
                prepareStack(topList.getList());
            }
            
            // Stack is empty, meaning no integers exist.
            return false;
        }
    }

    /**
     * ========================================================================
     * 8. SOLUTION COMPARISON
     * ========================================================================
     * Approach         | Space Complexity | When to Use
     * ------------------------------------------------------------------------
     * Precomputed      | O(N)             | Never in interviews. Too simplistic.
     * Stack (Lazy)     | O(Depth)         | ⭐ STRONGLY REC. Handles large data.
     * 
     * ========================================================================
     * 9. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if we wanted to implement a `.remove()` method?"
     * A1: We would need a pointer or reference to the original list and the index 
     *     of the last returned element. The Precomputed approach fails entirely 
     *     because modifying the flat list doesn't modify the source data. The 
     *     Lazy approach can be adapted using Java's internal ListIterators.
     * 
     * Q2: "What if the data isn't a `List<NestedInteger>` but a continuous stream 
     *      of JSON-like data over a network?"
     * A2: The Precomputed approach completely fails (cannot load infinite data). 
     *     The Lazy Stack approach shines because it only loads the current nesting 
     *     level into memory, pausing perfectly to await the next element.
     * 
     * ========================================================================
     * 10. FINAL TAKEAWAYS
     * ========================================================================
     * - The "Iterator Pattern" implies Lazy Evaluation. Never precompute if you 
     *   are asked to build an iterator unless explicitly permitted.
     * - Stacks reverse order. When adding a left-to-right list to a LIFO stack, 
     *   you MUST iterate backwards.
     * - In custom iterators, `hasNext()` handles the heavy lifting (finding the 
     *   next valid element), and `next()` just consumes it.
     */

    // ========================================================================
    // DUMMY INTERFACE & IMPLEMENTATION FOR COMPILATION
    // (This is normally provided by the platform/interviewer)
    // ========================================================================
    public interface NestedInteger {
        boolean isInteger();
        Integer getInteger();
        List<NestedInteger> getList();
    }

    static class MockNestedInteger implements NestedInteger {
        private Integer value;
        private List<NestedInteger> list;

        public MockNestedInteger(Integer value) {
            this.value = value;
            this.list = null;
        }

        public MockNestedInteger(List<NestedInteger> list) {
            this.value = null;
            this.list = list;
        }

        @Override
        public boolean isInteger() {
            return value != null;
        }

        @Override
        public Integer getInteger() {
            return value;
        }

        @Override
        public List<NestedInteger> getList() {
            return list;
        }
    }
}

import java.util.*;

/**
 * ================================================================
 * 🔥 Nested Iterator — LAZY STACK SOLUTION (INTERVIEW GOLD)
 * ================================================================
 *
 * 🔷 PROBLEM UNDERSTANDING
 * ------------------------------------------------
 * We are given a nested structure like:
 *
 *      [1, [4, [6]], 2]
 *
 * We need to iterate it as:
 *
 *      1 → 4 → 6 → 2
 *
 * BUT:
 * - We CANNOT flatten everything upfront (inefficient follow-up)
 * - We should process elements ON-DEMAND (lazy evaluation)
 *
 * ================================================================
 *
 * 🔷 CORE IDEA (MOST IMPORTANT)
 * ------------------------------------------------
 * Use a STACK to simulate recursion (DFS traversal)
 *
 * WHY STACK?
 * - Recursion naturally handles nested lists
 * - Stack mimics recursion iteratively
 *
 * ================================================================
 *
 * 🔷 CRITICAL INVARIANT (INTERVIEW KEY POINT)
 * ------------------------------------------------
 * Before calling next():
 *
 *      👉 TOP OF STACK MUST ALWAYS BE AN INTEGER
 *
 * So:
 * - hasNext() is responsible for "fixing" the stack
 * - next() simply consumes the top element
 *
 * ================================================================
 *
 * 🔷 HOW DO WE MAINTAIN THIS INVARIANT?
 * ------------------------------------------------
 * While top is a LIST:
 *      → remove it
 *      → push its elements in REVERSE ORDER
 *
 * WHY REVERSE?
 * - Stack is LIFO
 * - To process left-to-right, we push right-to-left
 *
 * ================================================================
 */

class NestedIterator {

    // Stack to simulate recursion
    private final Deque<NestedInteger> stack = new ArrayDeque<>();

    /**
     * 🔹 Constructor
     *
     * We initialize stack with given list
     * BUT push in reverse order
     *
     * Example:
     * Input: [1, [2,3]]
     *
     * Stack (top first):
     *      1, [2,3]
     */
    public NestedIterator(List<NestedInteger> nestedList) {
        pushListInReverse(nestedList);
    }

    /**
     * 🔹 Helper: Push list elements in reverse order
     *
     * WHY?
     * - So that first element appears on top of stack
     *
     * Example:
     * [1, 2, 3] → push → stack = 1 (top), then 2, then 3
     */
    private void pushListInReverse(List<NestedInteger> list) {

        // Start from end → move backward
        ListIterator<NestedInteger> it = list.listIterator(list.size());

        while (it.hasPrevious()) {
            stack.push(it.previous());
        }
    }

    /**
     * 🔹 hasNext()
     *
     * PURPOSE:
     * Ensure that top of stack is always an INTEGER
     *
     * PROCESS:
     * 1. If top is integer → we are ready → return true
     * 2. If top is list:
     *      → remove it
     *      → expand it (push its elements)
     * 3. Repeat until:
     *      → integer found OR stack empty
     *
     * KEY POINT:
     * Each element is processed ONLY ONCE → amortized O(1)
     */
    public boolean hasNext() {

        while (!stack.isEmpty()) {

            NestedInteger top = stack.peek();

            // ✅ Case 1: Already an integer → good to go
            if (top.isInteger()) {
                return true;
            }

            // ❌ Case 2: It's a list → expand it
            stack.pop(); // remove the list

            // Push its elements in reverse order
            pushListInReverse(top.getList());
        }

        // No elements left
        return false;
    }

    /**
     * 🔹 next()
     *
     * Since hasNext() guarantees:
     *      TOP = INTEGER
     *
     * We can safely pop and return it
     *
     * Defensive Programming:
     * - Call hasNext() again to avoid invalid access
     */
    public int next() {

        if (!hasNext()) {
            throw new NoSuchElementException("No more elements");
        }

        return stack.pop().getInteger();
    }

    /**
     * ------------------------------------------------------------
     * 🔹 TEST HELPER (DO NOT MODIFY)
     * ------------------------------------------------------------
     */
    public static List<Integer> flattenList(NestedIterator obj){
        List<Integer> result = new ArrayList<>();

        while (obj.hasNext()) {
            result.add(obj.next());
        }

        return result;
    }
}
