/**
 * ============================================================================
 * SECOND GREATER ELEMENT - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given an array of non-negative integers `nums`, find the "second 
 * greater element" for each element. The second greater element is the second 
 * element to the right of `nums[i]` that is strictly strictly greater than `nums[i]`.
 * If it doesn't exist, return -1.
 * 
 * Simple Explanation for Interviewer:
 * "In a standard 'Next Greater Element' problem, we just look for the FIRST 
 * number to the right that is bigger. Here, we skip the first bigger number 
 * and look for the SECOND bigger number to the right. We need to process the 
 * array and return these second bigger numbers for every position."
 * 
 * Core Idea & Intuition:
 * This problem builds heavily on the Monotonic Stack pattern used for finding 
 * the *First* Greater Element. 
 * - When an element is popped from a standard Monotonic Stack, it means we 
 *   just found its FIRST greater element. 
 * - Instead of resolving it immediately, we move it to a "waiting area" 
 *   (a second stack or a priority queue). 
 * - In this waiting area, the element waits for the NEXT number that is 
 *   greater than it. When that happens, we've found the SECOND greater element!
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can there be duplicate values in the array?"
 *    Why: To clarify if `>=` or strictly `>` is needed.
 *    Impact: Problem specifies strictly greater (`>`). Duplicates don't count 
 *    as a "greater" element.
 * 
 * 2. Q: "Does the second greater element have to be greater than the first 
 *        greater element?"
 *    Why: Essential for understanding the rule. 
 *    Impact: No. E.g., for `4` in `[4, 9, 6]`, the first greater is 9, the 
 *    second greater is 6. Even though 6 < 9, 6 is still > 4, so it counts.
 * 
 * 3. Q: "What are the constraints on the array size?"
 *    Why: If N is up to 10^5, an O(N^2) brute force will Time Out. We must 
 *    find an O(N log N) or O(N) solution.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: nums = [2, 4, 0, 9, 6]
 * Output: [9, 6, 6, -1, -1]
 * 
 * Explanation:
 * - For 2: greater elements to the right are 4, 9, 6. The second is 9.
 * - For 4: greater elements to the right are 9, 6. The second is 6.
 * - For 0: greater elements to the right are 9, 6. The second is 6.
 * - For 9: no greater elements. Returns -1.
 * - For 6: no greater elements. Returns -1.
 * 
 * Visualizing the "Two Stacks" (O(N)) Approach:
 * (Stack 1 holds elements looking for 1st greater. Stack 2 holds elements 
 * looking for 2nd greater. Stacks store INDICES, but we write values for clarity).
 * 
 * Processing [2, 4, 0, 9, 6]:
 * 
 * Num | Stack 2 (Needs 2nd) | Stack 1 (Needs 1st) | Action
 * ----------------------------------------------------------------------------
 *  2  | []                  | [2]                 | Push 2 to S1
 *  4  | []                  | []                  | 4 > 2. Pop 2 from S1. 
 *     | [2]                 | [4]                 | Move 2 to S2. Push 4 to S1.
 *  0  | [2]                 | [4, 0]              | 0 is not > anything. Push 0 to S1.
 *  9  | []                  | []                  | 9 > 2 (in S2). ANS[2]=9. Pop 2.
 *     |                     |                     | 9 > 0, 4 (in S1). Pop 0, 4 from S1.
 *     | [4, 0]              | [9]                 | Move 4, 0 to S2. Push 9 to S1.
 *  6  | []                  | [9, 6]              | 6 > 0, 4 (in S2). ANS[0,4]=6. Pop them.
 *     |                     |                     | 6 < 9 (in S1). Push 6 to S1.
 * 
 * Final ANS mapping applies perfectly!
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Confirm the definition of the *second* greater element.
 * 2. Clarify: Ask about O(N^2) constraints. Mention Brute Force as a baseline.
 * 3. Approach 1 (Min-Heap / PriorityQueue): Explain standard Next Greater Element 
 *    using a stack. When an item is popped, put it in a Min-Heap. The Min-Heap 
 *    checks for the second greater element. (O(N log N) time).
 * 4. Approach 2 (Two Stacks): If interviewer asks for O(N), explain that instead 
 *    of a Min-Heap, a second Monotonic Stack can be used. It relies on a beautiful 
 *    mathematical property of the problem.
 * 5. Code: Write the Two Stacks approach if comfortable, else Priority Queue.
 * 6. Dry-run: Trace the example carefully.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Strictly decreasing array: [5, 4, 3, 2, 1] -> [-1, -1, -1, -1, -1]
 * - Strictly increasing array: [1, 2, 3, 4] -> [3, 4, -1, -1]
 * - Duplicates: [2, 2, 2, 2] -> [-1, -1, -1, -1]. We use strictly `<` for comparisons.
 * 
 * Common Mistakes:
 * - Directly pushing from Stack 1 to Stack 2 without a temporary buffer. 
 *   If you pop from S1 and immediately push to S2, the order gets inverted, 
 *   destroying the monotonic property of S2. You MUST use a temporary stack/list 
 *   to preserve the ordering during the transfer.
 */

import java.util.*;

public class SecondGreaterElement {

    public static void main(String[] args) {
        int[] nums1 = {2, 4, 0, 9, 6};
        int[] nums2 = {3, 3};

        System.out.println("--- PriorityQueue Approach (O(N log N)) ---");
        System.out.println(Arrays.toString(secondGreaterElementPQ(nums1))); // [9, 6, 6, -1, -1]
        System.out.println(Arrays.toString(secondGreaterElementPQ(nums2))); // [-1, -1]

        System.out.println("\n--- Two Stacks Approach (O(N)) ---");
        System.out.println(Arrays.toString(secondGreaterElementOptimal(nums1))); // [9, 6, 6, -1, -1]
        System.out.println(Arrays.toString(secondGreaterElementOptimal(nums2))); // [-1, -1]
    }

    /**
     * SOLUTION 1: PriorityQueue + Stack (O(N log N))
     * ------------------------------------------------------------------------
     * Idea: We use a Monotonic Stack (`s1`) to find the FIRST greater element.
     * When an element finds its first greater element, it gets popped from `s1` 
     * and pushed into a Min-Heap (`pq`). The Min-Heap stores elements waiting 
     * for their SECOND greater element.
     * For every new number `x`, we first check if it resolves any elements in 
     * `pq`. Then we check if it resolves any elements in `s1`.
     * 
     * Time Complexity: O(N log N). Each element is pushed and popped from the 
     * Min-Heap at most once.
     * Space Complexity: O(N) for the stack and heap.
     * 
     * Interview Note: This is an excellent, highly intuitive solution that 
     * most interviewers will gladly accept.
     */
    public static int[] secondGreaterElementPQ(int[] nums) {
        int n = nums.length;
        int[] res = new int[n];
        Arrays.fill(res, -1);

        // s1 stores indices looking for their FIRST greater element
        Deque<Integer> s1 = new ArrayDeque<>();
        
        // pq stores indices looking for their SECOND greater element.
        // It is a Min-Heap ordered by the actual values in the nums array.
        PriorityQueue<Integer> pq = new PriorityQueue<>((a, b) -> Integer.compare(nums[a], nums[b]));

        for (int i = 0; i < n; i++) {
            int x = nums[i];

            // 1. Check if the current number resolves any elements waiting for their 2nd greater
            while (!pq.isEmpty() && nums[pq.peek()] < x) {
                res[pq.poll()] = x;
            }

            // 2. Check if the current number resolves any elements waiting for their 1st greater
            // If it does, move them from s1 to pq.
            while (!s1.isEmpty() && nums[s1.peek()] < x) {
                pq.offer(s1.pop());
            }

            // 3. Current number starts waiting for its 1st greater element
            s1.push(i);
        }

        return res;
    }

    /**
     * SOLUTION 2: Two Stacks (O(N) Ultra-Optimal)
     * ------------------------------------------------------------------------
     * Idea: Instead of a Min-Heap, we can use a second Monotonic Stack (`s2`).
     * `s1` tracks elements looking for their 1st greater element.
     * `s2` tracks elements looking for their 2nd greater element.
     * 
     * The Magic Property: Why does a second stack work without sorting?
     * When elements are popped from `s1` by a number `x`, they all share `x` as 
     * their first greater element. We move them to `s2`. 
     * Elements ALREADY in `s2` were NOT popped by `x`, meaning their value is `>= x`.
     * The new elements being moved to `s2` are strictly `< x`.
     * Therefore, ALL old elements in `s2` are strictly greater than ALL new 
     * elements being pushed to `s2`. This guarantees that if we push them 
     * correctly, `s2` automatically stays perfectly sorted (smallest at the top)!
     * 
     * Time Complexity: O(N). Every index is pushed and popped from `s1`, `temp`, 
     * and `s2` exactly once. No heap overhead!
     * Space Complexity: O(N) for the stacks.
     */
    public static int[] secondGreaterElementOptimal(int[] nums) {
        int n = nums.length;
        int[] res = new int[n];
        Arrays.fill(res, -1);

        Deque<Integer> s1 = new ArrayDeque<>();
        Deque<Integer> s2 = new ArrayDeque<>();
        Deque<Integer> temp = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            int x = nums[i];

            // 1. Resolve elements waiting for their 2nd greater element
            // Since s2 has the smallest elements at the top, we can pop while top < x
            while (!s2.isEmpty() && nums[s2.peek()] < x) {
                res[s2.pop()] = x;
            }

            // 2. Resolve elements waiting for their 1st greater element
            // Pop them from s1 and move them to a temporary stack
            while (!s1.isEmpty() && nums[s1.peek()] < x) {
                temp.push(s1.pop());
            }

            // 3. Move the elements from the temporary stack into s2
            // Why use temp? Because popping from s1 reverses their order.
            // Pushing them to temp and then to s2 reverses them back, maintaining 
            // the monotonic property (smallest at the top of s2).
            while (!temp.isEmpty()) {
                s2.push(temp.pop());
            }

            // 4. The current element starts waiting for its 1st greater element
            s1.push(i);
        }

        return res;
    }

    /**
     * ========================================================================
     * 6. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time     | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * PriorityQueue  | O(NlogN) | O(N)  | Very intuitive extension.  | ⭐ STRONGLY REC.
     * Two Stacks     | O(N)     | O(N)  | Genius use of properties.  | Mention / Flex.
     * 
     * ========================================================================
     * 7. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if we needed the THIRD greater element?"
     * A1: The PriorityQueue approach easily extends. We could use an array of 
     *     Min-Heaps or move elements through 3 distinct stages. The Two Stacks 
     *     approach, however, does NOT cleanly scale to 3 stacks because the 
     *     "magic sorting property" breaks down across multiple independent steps.
     * 
     * Q2: "Can this be solved using a Segment Tree or Fenwick Tree (BIT)?"
     * A2: Yes. We can process the array offline by sorting elements by value and 
     *     using a Segment Tree to query the second available index to the right. 
     *     However, this takes O(N log N) time and is vastly more complex to write 
     *     than the PriorityQueue approach.
     * 
     * ========================================================================
     * 8. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: 'Next Greater Element' problems always use a Monotonic Stack.
     * - Pipeline Pattern: When looking for the N-th occurrence of a condition, 
     *   you can treat data structures as a "pipeline". Stack 1 handles the 1st 
     *   occurrence, then passes the data to Stack/PQ 2 for the 2nd occurrence.
     * - Stack Reversal Trick: If you need to move elements between two stacks 
     *   while preserving their relative order, use a 3rd temporary stack buffer.
     */
}


/**
 * ================================================================
 * SECOND GREATER ELEMENT
 * ================================================================
 *
 * Problem:
 *
 * For every nums[i], find the SECOND element to its right that is
 * strictly greater than nums[i].
 *
 * If there are fewer than two greater elements to the right,
 * answer is -1.
 *
 *
 * Example:
 *
 * nums = [2, 4, 0, 9, 6]
 *
 * For 2:
 *   greater elements to the right = [4, 9, 6]
 *   first greater  = 4
 *   second greater = 9
 *
 * For 4:
 *   greater elements to the right = [9, 6]
 *   first greater  = 9
 *   second greater = -1
 *
 * For 0:
 *   greater elements to the right = [9, 6]
 *   first greater  = 9
 *   second greater = 6
 *
 * Answer:
 *
 * [9, -1, 6, -1, -1]
 *
 *
 * ================================================================
 * THE IMPORTANT PART
 * ================================================================
 *
 * A normal "next greater element" problem needs ONE monotonic stack.
 *
 * But this problem asks for the SECOND greater element.
 *
 * Therefore, we need to remember two different states:
 *
 *     1. Elements that are waiting for their FIRST greater element.
 *
 *     2. Elements that already found their FIRST greater element
 *        and are now waiting for their SECOND greater element.
 *
 *
 * That's why we need TWO stacks.
 *
 *
 * Stack 1 (s1):
 *
 *     "I have not found my FIRST greater element yet."
 *
 *
 * Stack 2 (s2):
 *
 *     "I already found my FIRST greater element.
 *      Now I am waiting for my SECOND greater element."
 *
 *
 * ================================================================
 *
 * WHY NOT JUST USE ONE STACK?
 * ================================================================
 *
 * Consider:
 *
 * nums = [2, 4, 9]
 *
 * When we reach 4:
 *
 *     2 < 4
 *
 * So 4 is the FIRST greater element for 2.
 *
 * Now 2 needs to wait for another greater element.
 *
 * We cannot simply remove 2 from our data structure.
 *
 * Instead:
 *
 *     2 moves from:
 *
 *         s1 -> s2
 *
 * Meaning:
 *
 *     "2 has found its first greater element.
 *      Now it needs its second greater element."
 *
 *
 * ================================================================
 *
 * WHY DO WE PROCESS LEFT -> RIGHT?
 * ================================================================
 *
 * At index i, nums[i] is the NEW element that can potentially
 * become a greater element for elements before it.
 *
 * For example:
 *
 *     [2, 4, 9]
 *      ^  ^  ^
 *
 * When we arrive at 4:
 *
 *     4 can be the first greater element for 2.
 *
 * When we arrive at 9:
 *
 *     9 can be the second greater element for 2.
 *
 *
 * So left -> right naturally allows the CURRENT element to
 * "complete" the requirements of previous elements.
 *
 *
 * ================================================================
 *
 * THE CORE IDEA
 * ================================================================
 *
 * Every index starts in s1.
 *
 * Why?
 *
 * Because initially it has found ZERO greater elements.
 *
 *
 * When a future number is greater:
 *
 *     s1 -> s2
 *
 * This means:
 *
 *     "You found your FIRST greater."
 *
 *
 * When another future number is greater:
 *
 *     s2 -> ANSWER
 *
 * This means:
 *
 *     "You found your SECOND greater."
 *
 *
 * Therefore:
 *
 *              FIRST GREATER       SECOND GREATER
 *
 *     s1 -----------------> s2 -----------------> answer
 *
 *
 * ================================================================
 *
 * BUT THERE IS A PROBLEM...
 * ================================================================
 *
 * Suppose:
 *
 *     s1 = [older ... newer]
 *
 * Several elements in s1 may be smaller than current.
 *
 * Example:
 *
 *     s1 contains:
 *
 *         [2, 3, 4]
 *
 * and current = 10
 *
 * All three are smaller than 10.
 *
 * So all three found their FIRST greater element.
 *
 * We need to move:
 *
 *         2, 3, 4
 *
 * from s1 to s2.
 *
 *
 * But we must preserve their correct order.
 *
 * This is where TEMP comes in.
 *
 *
 * ================================================================
 * WHY DO WE NEED TEMP?
 * ================================================================
 *
 * Java's stack operation:
 *
 *     push()
 *     pop()
 *
 * follows LIFO:
 *
 *     Last In -> First Out
 *
 *
 * Suppose s1 contains:
 *
 * bottom                 top
 *
 *     2    3    4
 *              ^
 *
 * If we simply do:
 *
 *     s2.push(s1.pop());
 *
 * then:
 *
 *     4 moves first
 *     3 moves second
 *     2 moves third
 *
 * So s2 gets:
 *
 *     4, 3, 2
 *
 *
 * But this reverses the order of the elements we removed.
 *
 *
 * Why is preserving order important?
 *
 * Because the stack needs to maintain the appropriate ordering
 * for future greater-element processing.
 *
 *
 * Therefore:
 *
 *     s1 -> temp -> s2
 *
 * instead of:
 *
 *     s1 -> s2
 *
 *
 * ================================================================
 *
 * TEMP IS JUST AN ORDER-PRESERVING BRIDGE
 * ================================================================
 *
 * Suppose:
 *
 *     s1:
 *
 *         bottom              top
 *           2    3    4
 *                    ^
 *
 * current = 10
 *
 * First loop:
 *
 *     4 -> temp
 *     3 -> temp
 *     2 -> temp
 *
 * temp now effectively has:
 *
 *     4, 3, 2
 *
 * Then we pop temp into s2:
 *
 *     2 -> s2
 *     3 -> s2
 *     4 -> s2
 *
 * So the original ordering is restored.
 *
 *
 * Therefore:
 *
 *     s1
 *       |
 *       | pop
 *       v
 *     temp
 *       |
 *       | pop
 *       v
 *     s2
 *
 *
 * ================================================================
 * THE CODE
 * ================================================================
 */
public class Solution {

    public int[] secondGreaterElement(int[] nums) {

        int n = nums.length;

        // By default, assume that no second greater element exists.
        int[] res = new int[n];
        Arrays.fill(res, -1);


        /*
         * ------------------------------------------------------------
         * STACK 1
         * ------------------------------------------------------------
         *
         * s1 contains indices that have NOT found their first
         * greater element yet.
         *
         * Think:
         *
         *     "I am still waiting for my first greater number."
         */
        Deque<Integer> s1 = new ArrayDeque<>();


        /*
         * ------------------------------------------------------------
         * STACK 2
         * ------------------------------------------------------------
         *
         * s2 contains indices that HAVE found their first greater
         * element.
         *
         * They are now waiting for their second greater element.
         *
         * Think:
         *
         *     "I already found one.
         *      Now I am waiting for the second one."
         */
        Deque<Integer> s2 = new ArrayDeque<>();


        /*
         * ------------------------------------------------------------
         * TEMP STACK
         * ------------------------------------------------------------
         *
         * This is NOT another logical state.
         *
         * It exists only because Java stacks are LIFO.
         *
         * We need to move multiple elements from s1 -> s2 while
         * preserving their ordering.
         *
         * So:
         *
         *     s1 -> temp -> s2
         *
         * acts as an order-preserving transfer.
         */
        Deque<Integer> temp = new ArrayDeque<>();


        /*
         * ============================================================
         * PROCESS FROM LEFT TO RIGHT
         * ============================================================
         *
         * current = nums[i]
         *
         * This current value may be:
         *
         *     - the SECOND greater element for something in s2
         *     - the FIRST greater element for something in s1
         */
        for (int i = 0; i < n; i++) {

            int current = nums[i];


            /*
             * ========================================================
             * STEP 1: HANDLE SECOND GREATER ELEMENTS
             * ========================================================
             *
             * s2 contains elements that have already found their
             * FIRST greater element.
             *
             * Therefore, if current > nums[index],
             *
             * current becomes their SECOND greater element.
             *
             * Example:
             *
             *     nums = [2, 4, 9]
             *
             * After seeing 4:
             *
             *     2 has found its FIRST greater.
             *
             *     2 is now in s2.
             *
             * When we reach 9:
             *
             *     2 < 9
             *
             * Therefore:
             *
             *     9 is the SECOND greater element of 2.
             */
            while (!s2.isEmpty() && nums[s2.peek()] < current) {

                int index = s2.pop();

                res[index] = current;
            }


            /*
             * ========================================================
             * STEP 2: FIND FIRST GREATER ELEMENTS
             * ========================================================
             *
             * Now look at s1.
             *
             * s1 contains elements that have not found ANY greater
             * element yet.
             *
             * If:
             *
             *     nums[index] < current
             *
             * then current is their FIRST greater element.
             *
             * They should therefore move:
             *
             *     s1 -> s2
             *
             * because they now need to wait for their SECOND greater.
             *
             * BUT:
             *
             * We cannot directly move them from s1 to s2 because
             * doing so reverses their ordering.
             *
             * Therefore we first move them to temp.
             */
            while (!s1.isEmpty() && nums[s1.peek()] < current) {

                temp.push(s1.pop());
            }


            /*
             * ========================================================
             * STEP 3: MOVE TEMP -> S2
             * ========================================================
             *
             * This restores the ordering that was reversed while
             * popping from s1.
             *
             * So:
             *
             *     s1 -> temp -> s2
             *
             * instead of:
             *
             *     s1 -> s2
             */
            while (!temp.isEmpty()) {

                s2.push(temp.pop());
            }


            /*
             * ========================================================
             * STEP 4: CURRENT ELEMENT ENTERS S1
             * ========================================================
             *
             * nums[i] has just arrived.
             *
             * It has not seen ANY greater element to its right yet.
             *
             * Therefore it starts in s1.
             *
             * Meaning:
             *
             *     "I am waiting for my FIRST greater element."
             */
            s1.push(i);
        }

        return res;
    }
}


/**
 * ==================================================================
 * WALKTHROUGH
 * ==================================================================
 *
 * Let's manually execute:
 *
 *     nums = [2, 4, 9]
 *
 *
 * Initially:
 *
 *     s1 = []
 *     s2 = []
 *     res = [-1, -1, -1]
 *
 *
 * ---------------------------------------------------------------
 * i = 0
 * current = 2
 * ---------------------------------------------------------------
 *
 * s2:
 *
 *     empty
 *
 * s1:
 *
 *     empty
 *
 * Put 2 into s1:
 *
 *     s1 = [2]
 *     s2 = []
 *
 *
 * Meaning:
 *
 *     2 is waiting for its FIRST greater element.
 *
 *
 * ---------------------------------------------------------------
 * i = 1
 * current = 4
 * ---------------------------------------------------------------
 *
 * First check s2:
 *
 *     empty
 *
 *
 * Check s1:
 *
 *     2 < 4
 *
 * So 4 is the FIRST greater element for 2.
 *
 * Move 2:
 *
 *     s1 -> temp -> s2
 *
 * Now:
 *
 *     s1 = []
 *     s2 = [2]
 *
 *
 * Then add current 4 to s1:
 *
 *     s1 = [4]
 *
 *
 * State:
 *
 *     s1 = [4]
 *     s2 = [2]
 *
 *
 * Interpretation:
 *
 *     4 is waiting for FIRST greater.
 *
 *     2 is waiting for SECOND greater.
 *
 *
 * ---------------------------------------------------------------
 * i = 2
 * current = 9
 * ---------------------------------------------------------------
 *
 * First check s2:
 *
 *     2 < 9
 *
 * Therefore:
 *
 *     9 is the SECOND greater element of 2.
 *
 * So:
 *
 *     res[2's index] = 9
 *
 * s2 becomes empty.
 *
 *
 * Then check s1:
 *
 *     4 < 9
 *
 * Therefore:
 *
 *     9 is the FIRST greater element of 4.
 *
 * Move 4:
 *
 *     s1 -> temp -> s2
 *
 * Then add 9 to s1.
 *
 *
 * Final:
 *
 *     res = [9, -1, -1]
 *
 *
 * Which is correct:
 *
 *     2 -> 4 -> 9
 *          ^    ^
 *          1st  2nd
 *
 *
 * ==================================================================
 * MORE INTERESTING EXAMPLE
 * ==================================================================
 *
 * nums = [2, 4, 0, 9, 6]
 *
 *
 * Let's track only the states:
 *
 *
 * Start:
 *
 *     s1 = []
 *     s2 = []
 *
 *
 * After 2:
 *
 *     s1 = [2]
 *     s2 = []
 *
 *
 * After 4:
 *
 *     2 found FIRST greater (4)
 *
 *     s1 = [4]
 *     s2 = [2]
 *
 *
 * After 0:
 *
 *     0 is not greater than anything in s2 or s1.
 *
 *     s1 = [4, 0]
 *     s2 = [2]
 *
 *
 * After 9:
 *
 * First process s2:
 *
 *     2 < 9
 *
 * Therefore:
 *
 *     secondGreater[2] = 9
 *
 * s2 = []
 *
 *
 * Now process s1:
 *
 *     0 < 9
 *     4 < 9
 *
 * Both have found their FIRST greater.
 *
 * So:
 *
 *     s1 -> temp -> s2
 *
 *     s2 = [4, 0]
 *
 * Then 9 enters s1:
 *
 *     s1 = [9]
 *
 *
 * After 6:
 *
 * First process s2:
 *
 *     0 < 6
 *
 * Therefore:
 *
 *     secondGreater[0] = 6
 *
 * But:
 *
 *     4 < 6
 *
 * so 4 also gets 6 as its SECOND greater.
 *
 *
 * Final result:
 *
 *     [9, -1, 6, -1, -1]
 *
 *
 * ==================================================================
 * WHY DOES RIGHT -> LEFT FEEL NATURAL?
 * ==================================================================
 *
 * You might initially think:
 *
 *     "The answer is to the RIGHT.
 *      So shouldn't I process from RIGHT -> LEFT?"
 *
 * For ordinary NEXT GREATER ELEMENT:
 *
 *     YES.
 *
 * Example:
 *
 *     nums = [2, 1, 4]
 *
 * Processing from right to left works beautifully.
 *
 * At 1:
 *
 *     next greater = 4
 *
 * At 2:
 *
 *     next greater = 4
 *
 *
 * ==================================================================
 * BUT SECOND GREATER IS DIFFERENT
 * ==================================================================
 *
 * Consider:
 *
 *     nums = [2, 4, 9]
 *
 * For 2:
 *
 *     first greater = 4
 *     second greater = 9
 *
 *
 * If processing from RIGHT -> LEFT:
 *
 * Start at 9:
 *
 *     What should we remember about 9?
 *
 * Then 4:
 *
 *     What should we remember?
 *
 * Then 2:
 *
 *     We need to know:
 *
 *         1. Which elements are greater than 2?
 *         2. Which one is the first?
 *         3. Which one is the second?
 *
 *
 * You CAN design a right-to-left solution.
 *
 * In fact, there are solutions using:
 *
 *     - monotonic stacks
 *     - ordered sets
 *     - Fenwick trees
 *     - segment trees
 *     - binary search
 *
 * But a simple single monotonic-stack pattern does not directly
 * give us the SECOND greater element.
 *
 *
 * The left-to-right two-stack solution has a very natural state
 * transition:
 *
 *
 *     WAITING FOR FIRST
 *             |
 *             | first greater arrives
 *             v
 *     WAITING FOR SECOND
 *             |
 *             | second greater arrives
 *             v
 *          ANSWER
 *
 *
 * That is why this particular solution processes left -> right.
 *
 *
 * ==================================================================
 * THE DEEPEST WAY TO THINK ABOUT IT
 * ==================================================================
 *
 * Don't think:
 *
 *     "I have two stacks because the problem says second greater."
 *
 *
 * Instead think:
 *
 *     "Each element goes through different STATES."
 *
 *
 * Every element starts as:
 *
 *     STATE 0:
 *     I have found 0 greater elements.
 *
 *     s1
 *
 *
 * After seeing the first greater:
 *
 *     STATE 1:
 *     I have found 1 greater element.
 *
 *     s2
 *
 *
 * After seeing the second greater:
 *
 *     STATE 2:
 *     DONE
 *
 *     res[index]
 *
 *
 * Therefore:
 *
 *
 *             first greater
 *       s1 -----------------> s2
 *                              |
 *                              |
 *                         second greater
 *                              |
 *                              v
 *                            DONE
 *
 *
 * This state-machine view is the key to understanding the problem.
 *
 *
 * ==================================================================
 * WHY ARE THEY MONOTONIC STACKS?
 * ==================================================================
 *
 * Both stacks maintain useful ordering of values.
 *
 * The stacks allow us to discard elements that can no longer
 * block/future-process elements efficiently.
 *
 * When:
 *
 *     nums[s1.peek()] < current
 *
 * the current value is sufficient to move that element to the
 * next state.
 *
 * We don't need to search backwards through the entire array.
 *
 *
 * This gives us amortized O(n) time.
 *
 *
 * ==================================================================
 * TIME COMPLEXITY
 * ==================================================================
 *
 * Every index can:
 *
 *     - enter s1 once
 *     - move from s1 to temp once
 *     - move from temp to s2 once
 *     - move from s2 to result once
 *
 * Therefore each element is processed only a constant number
 * of times.
 *
 * Time:
 *
 *     O(n)
 *
 * Space:
 *
 *     O(n)
 *
 * because s1, s2 and temp can collectively contain O(n) indices.
 *
 *
 * ==================================================================
 * ONE IMPORTANT TAKEAWAY
 * ==================================================================
 *
 * Normal Next Greater:
 *
 *     "Find the FIRST greater."
 *
 *     Usually:
 *
 *         one monotonic stack
 *
 *
 * Second Greater:
 *
 *     "Find the SECOND greater."
 *
 *     We need to remember:
 *
 *         elements waiting for FIRST
 *              +
 *         elements waiting for SECOND
 *
 *     Therefore:
 *
 *         two logical stacks
 *
 *
 * And temp is NOT a third logical stack.
 *
 * It is simply an implementation helper needed to transfer
 * elements from one stack to another without reversing the
 * required ordering.
 *
 * ==================================================================
 */


import java.util.*;

/**
 * ====================================================================================
 * PROBLEM STATEMENT: SECOND GREATER ELEMENT (LeetCode 2454)
 * ====================================================================================
 * Given an array of integers `nums`, for each index `i`, find the value of the 
 * SECOND element to its right that is strictly greater than `nums[i]`.
 * 
 * If no such second greater element exists, set `res[i] = -1`.
 * 
 * Example:
 *   nums = [2, 4, 0, 9, 6]
 * 
 *   Index 0 (val = 2): Greater elements to right -> [4, 9, 6]. 
 *                      1st greater = 4, 2nd greater = 9. -> res[0] = 9
 *   Index 1 (val = 4): Greater elements to right -> [9, 6]. 
 *                      1st greater = 9, 2nd greater = none. -> res[1] = -1
 *   Index 2 (val = 0): Greater elements to right -> [9, 6]. 
 *                      1st greater = 9, 2nd greater = 6. -> res[2] = 6
 *   Index 3 (val = 9): Greater elements to right -> []. -> res[3] = -1
 *   Index 4 (val = 6): Greater elements to right -> []. -> res[4] = -1
 * 
 *   Result: [9, -1, 6, -1, -1]
 * ====================================================================================
 */

public class Solution {

    /**
     * Finds the second greater element for each index in O(N) time and O(N) space.
     *
     * @param nums Array of non-negative/negative integers
     * @return Array containing the second greater element for each corresponding position
     */
    public int[] secondGreaterElement(int[] nums) {

        int n = nums.length;

        // Default-fill output array with -1 (handles cases where 2nd greater does not exist)
        int[] res = new int[n];
        Arrays.fill(res, -1);

        /*
         * ----------------------------------------------------------------------------
         * STATE MACHINE & DATA STRUCTURE DESIGN
         * ----------------------------------------------------------------------------
         * Instead of thinking of this purely as a "stack algorithm", treat each index 
         * as an entity progressing through a 3-State Finite State Machine (FSM):
         *
         *    [State 0: Unprocessed] 
         *              |
         *              | (Pushed upon initial arrival)
         *              v
         *    [State 1: In Stack s1] ---- Waiting for 1st Greater Element
         *              |
         *              | (Found 1st Greater Element = current)
         *              v
         *    [State 2: In Stack s2] ---- Waiting for 2nd Greater Element
         *              |
         *              | (Found 2nd Greater Element = current)
         *              v
         *    [State 3: Completed] ------ Result recorded in res[]
         *
         * ----------------------------------------------------------------------------
         * LOGICAL STACKS:
         * ----------------------------------------------------------------------------
         * - s1 (Stage 1 Waiting Room): 
         *   Stores indices currently in State 1 (waiting for their 1st greater element).
         *   Maintains a monotonic DECREASING sequence of `nums` values from bottom to top.
         *
         * - s2 (Stage 2 Waiting Room): 
         *   Stores indices currently in State 2 (already found 1st greater element, 
         *   now waiting for 2nd greater element).
         *   Also maintains a monotonic DECREASING sequence of `nums` values from bottom to top.
         *
         * - temp (Transfer Bridge):
         *   A temporary stack used exclusively to maintain index relative order 
         *   when moving elements batch-wise from `s1` to `s2`.
         */
        Deque<Integer> s1 = new ArrayDeque<>();
        Deque<Integer> s2 = new ArrayDeque<>();
        Deque<Integer> temp = new ArrayDeque<>();

        /*
         * ============================================================================
         * LEFT-TO-RIGHT PROCESSING
         * ============================================================================
         * We iterate through `nums` from left to right. At iteration `i`, `current = nums[i]`
         * acts as a potential "greater element" for candidates sitting in `s2` and `s1`.
         */
        for (int i = 0; i < n; i++) {

            int current = nums[i];

            /*
             * ------------------------------------------------------------------------
             * STEP 1: RESOLVE SECOND GREATER ELEMENTS (Check s2)
             * ------------------------------------------------------------------------
             * Elements in `s2` are waiting for their SECOND greater element.
             * 
             * Because `s2` is strictly non-increasing from bottom to top, the top of `s2` 
             * (`s2.peek()`) holds the SMALLEST value currently waiting in `s2`.
             * 
             * If `current > nums[s2.peek()]`, then `current` is strictly greater than 
             * the element at `s2.peek()`. Because that element was already in `s2`, 
             * `current` is officially its SECOND greater element!
             * 
             * We pop it from `s2` and store `current` in `res[index]`. We repeat this 
             * in a loop until the top of `s2` is >= `current` or `s2` is empty.
             */
            while (!s2.isEmpty() && nums[s2.peek()] < current) {
                int index = s2.pop();
                res[index] = current;
            }

            /*
             * ------------------------------------------------------------------------
             * STEP 2: PROMOTE FROM STAGE 1 TO STAGE 2 (s1 -> temp)
             * ------------------------------------------------------------------------
             * Elements in `s1` are waiting for their FIRST greater element.
             * 
             * Similar to Step 1, the top of `s1` holds the SMALLEST value waiting in `s1`.
             * If `current > nums[s1.peek()]`, then `current` is the FIRST greater element 
             * for `s1.peek()`.
             * 
             * Therefore, these indices must transition from State 1 (in s1) to State 2 (in s2).
             * 
             * WHY NOT POP DIRECTLY FROM s1 INTO s2?
             * --------------------------------------
             * Standard LIFO operations reverse the sequence:
             *   Suppose s1 top-to-bottom has elements with values: [3, 5, 7] (where 3 is top).
             *   If we pop 3, then 5, then 7 directly into s2:
             *     - 3 goes into s2 first (becomes bottom)
             *     - 5 goes into s2 next
             *     - 7 goes into s2 last (becomes top)
             *   Now s2 top-to-bottom is [7, 5, 3]! 
             *   This DESTROYS the monotonic property of s2 (top should be smallest, not largest).
             * 
             * SOLUTION (The `temp` Bridge):
             *   Popping [3, 5, 7] into `temp` puts 7 at the top of `temp`.
             *   Then popping `temp` into `s2` places 7 at the bottom of s2 and 3 at the top.
             *   Double reversal (s1 -> temp -> s2) preserves the exact monotonic ordering!
             */
            while (!s1.isEmpty() && nums[s1.peek()] < current) {
                temp.push(s1.pop());
            }

            /*
             * ------------------------------------------------------------------------
             * STEP 3: FLUSH TEMP INTO S2 (temp -> s2)
             * ------------------------------------------------------------------------
             * Empty `temp` into `s2`. Because of the double-reversal, the elements arrive 
             * in `s2` maintaining the necessary monotonic non-increasing structure.
             */
            while (!temp.isEmpty()) {
                s2.push(temp.pop());
            }

            /*
             * ------------------------------------------------------------------------
             * STEP 4: ENQUEUE CURRENT INDEX INTO STAGE 1
             * ------------------------------------------------------------------------
             * Index `i` is new. It has found zero greater elements so far.
             * Push index `i` into `s1` so it can look for its 1st greater element 
             * in subsequent iterations.
             */
            s1.push(i);
        }

        return res;
    }
}

/**
 * ====================================================================================
 * DEEP-DIVE TECHNICAL QUESTIONS & ANALYSIS
 * ====================================================================================
 *
 * Q1: WHY DOES A RIGHT-TO-LEFT APPROACH NOT WORK EASILY?
 * ------------------------------------------------------------------------------------
 * In standard "First Next Greater Element", a Right-to-Left pass works because you only 
 * need to maintain a single monotonic stack containing candidate answers to the right.
 * 
 * For "SECOND Next Greater Element", processing Right-to-Left forces you to ask:
 * "Among all elements to my right that are strictly greater than me, which one occurs 
 * SECOND in positional index?"
 * 
 * To answer this from Right-to-Left, a simple monotonic stack is insufficient because:
 * 1. You must maintain ALL greater elements to the right, not just the nearest max.
 * 2. You need to query the element with the 2nd smallest index among those greater values.
 * 3. This requires complex data structures like Segment Trees, Fenwick Trees, or 
 *    Balanced BSTs (e.g., `java.util.TreeSet`) combined with Coordinate Compression,
 *    resulting in O(N log N) time complexity and significant memory/overhead.
 * 
 * Conversely, Left-to-Right naturally models a forward pipeline: elements wait in s1, 
 * advance to s2 when their 1st greater element appears, and complete when their 2nd 
 * greater element appears—achieving pure O(N) time.
 *
 * ====================================================================================
 * Q2: TRACE EXECUTION WALKTHROUGH FOR `nums = [2, 4, 0, 9, 6]`
 * ------------------------------------------------------------------------------------
 *
 * Initialization:
 *   res = [-1, -1, -1, -1, -1]
 *   s1 = [], s2 = [], temp = []
 *
 * ------------------------------------------------------------------------------------
 * Iteration 0: i = 0, current = nums[0] = 2
 * ------------------------------------------------------------------------------------
 * - Step 1 (s2 check): s2 empty.
 * - Step 2 (s1 check): s1 empty.
 * - Step 3 (temp -> s2): temp empty.
 * - Step 4 (push i to s1): s1 = [0]  (value: 2)
 *
 * ------------------------------------------------------------------------------------
 * Iteration 1: i = 1, current = nums[1] = 4
 * ------------------------------------------------------------------------------------
 * - Step 1 (s2 check): s2 empty.
 * - Step 2 (s1 check): top of s1 is index 0 (val 2). Since 2 < 4:
 *     pop index 0 from s1 -> temp.push(0). temp = [0]
 * - Step 3 (temp -> s2): pop index 0 from temp -> s2.push(0). s2 = [0]
 * - Step 4 (push i to s1): s1 = [1]  (value: 4)
 * 
 * State Summary:
 *   s1 = [1] (val 4, waiting for 1st greater)
 *   s2 = [0] (val 2, waiting for 2nd greater; found 1st = 4)
 *
 * ------------------------------------------------------------------------------------
 * Iteration 2: i = 2, current = nums[2] = 0
 * ------------------------------------------------------------------------------------
 * - Step 1 (s2 check): top of s2 is index 0 (val 2). 2 < 0 is FALSE. No action.
 * - Step 2 (s1 check): top of s1 is index 1 (val 4). 4 < 0 is FALSE. No action.
 * - Step 3 (temp -> s2): temp empty.
 * - Step 4 (push i to s1): s1 = [1, 2]  (values: 4, 0)
 *
 * State Summary:
 *   s1 = [1, 2] (vals: 4, 0)
 *   s2 = [0]    (val 2)
 *
 * ------------------------------------------------------------------------------------
 * Iteration 3: i = 3, current = nums[3] = 9
 * ------------------------------------------------------------------------------------
 * - Step 1 (s2 check):
 *     top of s2 is index 0 (val 2). Since 2 < 9:
 *       pop 0 from s2 -> res[0] = 9. s2 becomes [].
 * - Step 2 (s1 check):
 *     top of s1 is index 2 (val 0). Since 0 < 9: pop 2 -> temp.push(2). temp = [2]
 *     top of s1 is index 1 (val 4). Since 4 < 9: pop 1 -> temp.push(1). temp = [2, 1]
 * - Step 3 (temp -> s2):
 *     pop 1 from temp -> s2.push(1)
 *     pop 2 from temp -> s2.push(2)
 *     s2 becomes [1, 2] (top is index 2 with val 0; bottom is index 1 with val 4)
 * - Step 4 (push i to s1): s1 = [3]  (value: 9)
 *
 * State Summary:
 *   res = [9, -1, -1, -1, -1]
 *   s1 = [3]    (val 9)
 *   s2 = [1, 2] (vals: 4, 0)
 *
 * ------------------------------------------------------------------------------------
 * Iteration 4: i = 4, current = nums[4] = 6
 * ------------------------------------------------------------------------------------
 * - Step 1 (s2 check):
 *     top of s2 is index 2 (val 0). Since 0 < 6: pop 2 -> res[2] = 6
 *     top of s2 is index 1 (val 4). Since 4 < 6: pop 1 -> res[1] = 6
 *     s2 becomes [].
 * - Step 2 (s1 check): top of s1 is index 3 (val 9). 9 < 6 is FALSE.
 * - Step 3 (temp -> s2): temp empty.
 * - Step 4 (push i to s1): s1 = [3, 4] (vals: 9, 6)
 *
 * ------------------------------------------------------------------------------------
 * FINAL STATE & RETURN
 * ------------------------------------------------------------------------------------
 * Loop terminates.
 * Return res = [9, 6, 6, -1, -1]
 *
 * ====================================================================================
 * COMPLEXITY ANALYSIS
 * ====================================================================================
 * Time Complexity: O(N)
 * - Each index `i` (from 0 to N-1) is pushed into `s1` exactly once.
 * - Each index is moved from `s1` to `temp` at most once.
 * - Each index is moved from `temp` to `s2` at most once.
 * - Each index is popped from `s2` at most once.
 * - Total stack operations across the entire execution <= 4 * N -> O(N) linear time.
 *
 * Space Complexity: O(N)
 * - Auxiliary space used by `s1`, `s2`, and `temp` is at most N indices combined.
 * - Output array `res` takes O(N) space.
 * ====================================================================================
 */
