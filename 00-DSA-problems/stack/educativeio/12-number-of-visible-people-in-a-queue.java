/**
 * ============================================================================
 * NUMBER OF VISIBLE PEOPLE IN A QUEUE - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given an array of unique heights representing people in a line, 
 * return an array where answer[i] is the number of people person i can see 
 * to their right.
 * 
 * Person i can see person j (where i < j) if everyone between them is shorter 
 * than both of them.
 * 
 * Core Idea & Intuition:
 * When a person looks to their right, they can see a sequence of people whose 
 * heights are strictly increasing. As soon as they see someone taller than 
 * themselves, their line of sight is completely blocked.
 * 
 * This means taller people "hide" shorter people behind them. If we process 
 * the line from Right-to-Left, we can maintain a Stack of people. If a new 
 * person (moving leftward) is taller than the people in the stack, those 
 * shorter people in the stack get hidden from anyone further to the left. 
 * Therefore, we can pop them off the stack.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Are all heights guaranteed to be unique?"
 *    Why: If heights can be equal, we have to carefully manage `>=` vs `>` 
 *    when popping from the stack to handle visibility correctly. 
 *    Impact: Prompt guarantees unique heights, simplifying the logic.
 * 
 * 2. Q: "Can the input array be empty?"
 *    Why: Determines if we need an early exit for length 0.
 *    Impact: Constraints say n >= 1, but adding a guard clause is good practice.
 * 
 * 3. Q: "What is the maximum size of n?"
 *    Why: If n is 1000 (as per constraints), O(N^2) brute force will pass easily. 
 *    If n was 10^5, O(N^2) would result in a Time Limit Exceeded (TLE).
 *    Impact: We will write the O(N) solution as the optimal approach.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example 1:
 * Input: heights = [10, 6, 8, 5, 11, 9]
 * Output: [3, 1, 2, 1, 1, 0]
 * 
 * Explanation of visibility for 10:
 * 10 looks right:
 * - Sees 6 (Valid, max between is 0)
 * - Sees 8 (Valid, 6 is smaller than min(10, 8))
 * - Sees 11 (Valid, 8 and 6 are smaller than min(10, 11))
 * - Cannot see 9 (Blocked by 11)
 * Total for 10 = 3 people.
 * 
 * Visualizing the Right-to-Left Stack Approach:
 * Process [10, 6, 8, 5, 11, 9] from right to left:
 * 
 * i | Height | Stack (Top->Bottom) | Action (Count visible) & Stack Update
 * ------------------------------------------------------------------------
 * 5 |   9    | []                  | Empty stack -> sees 0. Push 9.
 * 4 |  11    | [9]                 | 11 > 9. Pop 9 (sees 1). Empty -> sees 0 more. Total=1. Push 11.
 * 3 |   5    | [11]                | 5 < 11. No pop. Stack has 11 -> sees 1. Push 5. 
 * 2 |   8    | [5, 11]             | 8 > 5. Pop 5 (sees 1). 8 < 11. Stack has 11 -> sees 1 more. Total=2. Push 8.
 * 1 |   6    | [8, 11]             | 6 < 8. No pop. Stack has 8 -> sees 1. Push 6.
 * 0 |  10    | [6, 8, 11]          | 10 > 6, pop(1). 10 > 8, pop(2). 10 < 11, sees 11(3). Push 10.
 * 
 * Final output: [3, 1, 2, 1, 1, 0]
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Clarify the line-of-sight rules (blocked by taller people).
 * 2. Clarify: Confirm unique heights.
 * 3. Brute Force: Explain O(N^2) solution. Loop i, loop j, keep a running max.
 * 4. Optimize: Notice the LIFO property of line-of-sight. Propose a Monotonic 
 *    Decreasing Stack, processing from right to left.
 * 5. Code: Implement the O(N) Stack approach.
 * 6. Dry-run: Walk through [10, 6, 8, 5, 11, 9].
 * 7. Complexity Analysis: O(N) Time, O(N) Space.
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Strictly decreasing array: [5, 4, 3, 2, 1] -> Everyone sees exactly 1 person 
 *   (except the last person).
 * - Strictly increasing array: [1, 2, 3, 4, 5] -> Everyone sees exactly 1 person 
 *   (except the last person).
 * - Only 2 people: [1, 2] or [2, 1] -> First person sees 1, second sees 0.
 * 
 * Common Mistakes:
 * - Iterating Left-to-Right: It is possible to solve this left-to-right, but you 
 *   have to update answers for elements *in* the stack as you go. Right-to-left 
 *   is much easier because the answer for index `i` is fully resolved exactly 
 *   when you process index `i`.
 * - Forgetting the taller blocker: After popping all people shorter than you, 
 *   if the stack isn't empty, the person at the top is taller. You CAN see them, 
 *   they just block everyone else. Don't forget to add +1 for them.
 */

import java.util.*;

public class VisiblePeopleInQueue {

    public static void main(String[] args) {
        int[] heights1 = {10, 6, 8, 5, 11, 9}; // [3, 1, 2, 1, 1, 0]
        int[] heights2 = {5, 1, 2, 3, 10};     // [4, 1, 1, 1, 0]
        
        System.out.println("--- Brute Force Approach ---");
        System.out.println(Arrays.toString(canSeePersonsCountBruteForce(heights1)));
        
        System.out.println("\n--- Optimal Stack Approach ---");
        System.out.println(Arrays.toString(canSeePersonsCountOptimal(heights1)));
        System.out.println(Arrays.toString(canSeePersonsCountOptimal(heights2)));
    }

    /**
     * SOLUTION 1: BRUTE FORCE
     * ------------------------------------------------------------------------
     * Idea: For every person i, look to the right (j). We keep track of the 
     * maximum height seen so far between i and j. If person j is taller than 
     * this maximum, person i can see them. We stop looking when we hit someone 
     * taller than person i.
     * 
     * Time Complexity: O(N^2) in the worst case (e.g., strictly decreasing array 
     * where we have to scan all elements to the right).
     * Space Complexity: O(1) auxiliary space (excluding output array).
     */
    public static int[] canSeePersonsCountBruteForce(int[] heights) {
        int n = heights.length;
        int[] ans = new int[n];
        
        for (int i = 0; i < n; i++) {
            int maxBetween = 0;
            int visibleCount = 0;
            
            for (int j = i + 1; j < n; j++) {
                // If j is taller than the tallest person between i and j, i can see j
                if (heights[j] > maxBetween) {
                    visibleCount++;
                    maxBetween = heights[j]; // Update the blocker
                }
                
                // If j is taller than i, i's vision is completely blocked for anyone further right
                if (heights[j] > heights[i]) {
                    break;
                }
            }
            ans[i] = visibleCount;
        }
        
        return ans;
    }

    /**
     * SOLUTION 2: OPTIMAL (Monotonic Stack Right-to-Left)
     * ------------------------------------------------------------------------
     * Idea: Iterate from right to left. We use a Stack to maintain the heights 
     * of people who are not hidden by anyone to their left yet. 
     * When processing person i:
     * 1. They can see everyone in the stack shorter than them. We pop these 
     *    shorter people (because person i hides them from anyone further left).
     * 2. After popping shorter people, if there is still someone in the stack, 
     *    that person is taller. Person i can see them, but no one past them.
     * 3. Finally, person i is pushed onto the stack to potentially be seen by 
     *    people further to the left.
     * 
     * Time Complexity: O(N) - Every person is pushed onto the stack exactly once 
     * and popped at most once. The while loop runs at most N times globally.
     * Space Complexity: O(N) - In the worst case (e.g., strictly increasing array 
     * from right to left), all elements are pushed to the stack.
     */
    public static int[] canSeePersonsCountOptimal(int[] heights) {
        if (heights == null || heights.length == 0) return new int[0];
        
        int n = heights.length;
        int[] ans = new int[n];
        
        // Deque is preferred over Stack in Java for performance
        Deque<Integer> stack = new ArrayDeque<>();
        
        // Traverse from right to left
        for (int i = n - 1; i >= 0; i--) {
            int visibleCount = 0;
            
            // Person i can see all people in the stack who are shorter than them
            while (!stack.isEmpty() && heights[i] > stack.peek()) {
                stack.pop();
                visibleCount++;
            }
            
            // If the stack is not empty, it means there is someone taller than person i.
            // Person i can see this taller person, but their vision stops there.
            if (!stack.isEmpty()) {
                visibleCount++;
            }
            
            // Record the answer for person i
            ans[i] = visibleCount;
            
            // Push person i onto the stack for people further left to evaluate
            stack.push(heights[i]);
        }
        
        return ans;
    }

    /**
     * ========================================================================
     * 6. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time   | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Brute Force    | O(N^2) | O(1)  | Easy to write, too slow.   | Explain conceptually.
     * Monotonic Stack| O(N)   | O(N)  | Standard O(N) pattern.     | ⭐ STRONGLY REC.
     * 
     * ========================================================================
     * 7. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if there could be duplicate heights?"
     * A1: If `heights[i] == heights[j]`, person `i` CAN see person `j` (assuming 
     *     no taller people between them), but cannot see past them. We would change 
     *     our stack logic: pop while `heights[i] > stack.peek()`. If `heights[i] == stack.peek()`, 
     *     person i sees them (+1), but we STILL pop them (or we group duplicates in 
     *     an object like `Node(height, count)`). The grouping approach prevents 
     *     losing line of sight information.
     * 
     * Q2: "Can you solve this Left-to-Right?"
     * A2: Yes. Iterate left to right, pushing indices to a monotonic decreasing stack. 
     *     When you encounter a new person `j` who is taller than `stack.peek()`, the 
     *     person at `stack.peek()` can see `j`. We increment `ans[stack.pop()]`. 
     *     If the stack is not empty after popping shorter people, the person 
     *     now at `stack.peek()` can ALSO see `j` (so `ans[stack.peek()]++`), but they 
     *     are NOT popped because `j` is shorter than them. This is trickier to implement 
     *     correctly than Right-to-Left.
     * 
     * ========================================================================
     * 8. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: "Line of Sight" or "Next Greater Element" problems almost 
     *   always require a Monotonic Stack.
     * - Direction Matters: Processing Right-to-Left allows us to resolve the answer 
     *   for index `i` completely in a single step, rather than partially updating it.
     * - Don't forget the Blocker: A strictly smaller/greater check in a stack usually 
     *   leaves a "blocker" element at the top. Remember to account for this blocker 
     *   in your counts!
     */
}

/**
 * ================================================================
 * 🔥 Number of Visible People in a Queue — DO NOT CONFUSE VERSION
 * ================================================================
 *
 * ⚠️ COMMON MISTAKE (VERY IMPORTANT):
 * ------------------------------------------------
 * ❌ WRONG THINKING:
 *   "Let me find next greater element and take distance"
 *
 *   result[i] = nextGreaterIndex - i   ❌ WRONG
 *
 * WHY WRONG?
 *   - Problem is NOT asking distance
 *   - It is asking COUNT of visible people
 *   - Some people in between are NOT visible
 *
 * ------------------------------------------------
 * ✅ CORRECT THINKING:
 *   "Simulate visibility"
 *
 *   - Smaller people → visible → REMOVE THEM (pop)
 *   - First taller person → visible → STOP
 *
 * ================================================================
 *
 * 🧠 CORE IDEA:
 * ------------------------------------------------
 * Traverse from RIGHT → LEFT
 *
 * For each person i:
 *   1. Remove all shorter people → they are visible
 *   2. If someone remains → first taller → also visible
 *
 * ================================================================
 *
 * ⏱️ TIME:  O(n)
 * 📦 SPACE: O(n)
 *
 * ================================================================
 */

public class VisiblePeopleInQueue {

    public static int[] canSeePersonsCount(int[] heights) {

        int n = heights.length;

        // Final result array
        int[] result = new int[n];

        /**
         * Stack stores indices of people
         *
         * WHY indices?
         * - Helps debugging
         * - Helps if we need positions
         *
         * MONOTONIC PROPERTY:
         * - Stack will be DECREASING (top is smallest among remaining visible chain)
         */
        Deque<Integer> stack = new ArrayDeque<>();

        /**
         * Traverse from RIGHT → LEFT
         *
         * WHY?
         * - We want to know "future visibility"
         * - Stack already contains "people to the right"
         */
        for (int i = n - 1; i >= 0; i--) {

            /**
             * This variable is CRUCIAL
             *
             * ❗ THIS is what your previous solution was missing
             *
             * We are NOT computing distance
             * We are COUNTING visible people
             */
            int visibleCount = 0;

            /**
             * STEP 1: Remove all shorter people
             *
             * WHY?
             * - If current person is taller → they can see over them
             * - So each popped person is VISIBLE
             */
            while (!stack.isEmpty() && heights[i] > heights[stack.peek()]) {

                // Remove shorter person
                stack.pop();

                // That person is visible
                visibleCount++;

                /**
                 * 🔥 KEY INSIGHT:
                 * We REMOVE them because:
                 * - They won't block future people
                 * - They are "consumed visibility"
                 */
            }

            /**
             * STEP 2: Check if someone remains
             *
             * If yes:
             * - That person is taller than current
             * - So they are visible
             * - AND they BLOCK everything beyond
             */
            if (!stack.isEmpty()) {

                visibleCount++; // first taller person

                /**
                 * 🔥 KEY:
                 * We do NOT pop this person
                 * Because:
                 * - They block the view
                 * - They must stay for future comparisons
                 */
            }

            /**
             * Store the final answer for index i
             */
            result[i] = visibleCount;

            /**
             * STEP 3: Push current person into stack
             *
             * WHY?
             * - They become candidate for people on left
             */
            stack.push(i);
        }

        return result;
    }

    /**
     * ================================================================
     * 🧠 DRY RUN (VERY IMPORTANT — INTERVIEW GOLD)
     * ================================================================
     *
     * heights = [10, 6, 8, 5, 11, 9]
     *
     * PROCESS:
     *
     * i = 5 (9):
     *   stack = []
     *   visible = 0
     *   push 9
     *
     * i = 4 (11):
     *   pop 9 → visible = 1
     *   stack empty → stop
     *   result = 1
     *
     * i = 3 (5):
     *   no pop
     *   stack has 11 → visible = 1
     *
     * i = 2 (8):
     *   pop 5 → visible = 1
     *   see 11 → visible = 2
     *
     * i = 1 (6):
     *   no pop
     *   see 8 → visible = 1
     *
     * i = 0 (10):
     *   pop 6 → visible = 1
     *   pop 8 → visible = 2
     *   see 11 → visible = 3
     *
     * FINAL:
     * [3, 1, 2, 1, 1, 0]
     *
     * ================================================================
     */
}

import java.util.*;

/**
 * ================================================================
 * 🔥 Number of Visible People — LEFT TO RIGHT VARIANT
 * ================================================================
 *
 * ⚠️ THIS VERSION IS HARDER TO THINK
 *
 * WHY?
 * - We are NOT computing answer for current person directly
 * - We are helping PREVIOUS people (in stack)
 *
 * ------------------------------------------------
 * 🧠 THINK LIKE THIS:
 *
 * "Current person is being seen by people on the LEFT"
 *
 * Stack = people waiting to see someone
 *
 * ================================================================
 *
 * CORE RULE:
 * ------------------------------------------------
 * When we see current person:
 *
 * 1. All smaller people in stack → can see current → pop them
 * 2. First taller person (if exists) → can also see current
 *
 * ================================================================
 *
 * ⏱️ TIME: O(n)
 * 📦 SPACE: O(n)
 *
 * ================================================================
 */

public class VisiblePeopleLeftToRight {

    public static int[] canSeePersonsCount(int[] heights) {

        int n = heights.length;

        // Final result array
        int[] result = new int[n];

        /**
         * Stack stores indices of people
         *
         * These are people who are still "waiting"
         * to find someone they can see
         */
        Deque<Integer> stack = new ArrayDeque<>();

        /**
         * Traverse LEFT → RIGHT
         */
        for (int i = 0; i < n; i++) {

            /**
             * STEP 1: Handle all shorter people
             *
             * If current person is taller than stack top:
             *   → that shorter person can SEE current person
             */
            while (!stack.isEmpty() && heights[i] > heights[stack.peek()]) {

                int shorterIndex = stack.pop();

                // That person can see current
                result[shorterIndex]++;

                /**
                 * 🔥 IMPORTANT:
                 * We POP because:
                 * - This shorter person is now "resolved"
                 * - They cannot see beyond current (blocked now)
                 */
            }

            /**
             * STEP 2: If someone remains in stack
             *
             * That means:
             * - They are taller than current
             * - They can see current person
             */
            if (!stack.isEmpty()) {

                int tallerIndex = stack.peek();

                result[tallerIndex]++;

                /**
                 * 🔥 IMPORTANT:
                 * We DO NOT pop this taller person
                 *
                 * WHY?
                 * - They are still waiting to see future people
                 */
            }

            /**
             * STEP 3: Push current person into stack
             *
             * They will try to see future people
             */
            stack.push(i);
        }

        return result;
    }

    /**
     * ================================================================
     * 🧠 DRY RUN (VERY IMPORTANT)
     * ================================================================
     *
     * heights = [10, 6, 8, 5, 11, 9]
     *
     * PROCESS:
     *
     * i = 0 (10):
     *   stack = []
     *   push 0
     *
     * i = 1 (6):
     *   10 > 6 → 10 sees 6 → result[0]++
     *   push 1
     *
     * i = 2 (8):
     *   pop 6 → result[1]++ (6 sees 8)
     *   10 > 8 → result[0]++ (10 sees 8)
     *   push 2
     *
     * i = 3 (5):
     *   8 > 5 → result[2]++ (8 sees 5)
     *   push 3
     *
     * i = 4 (11):
     *   pop 5 → result[3]++ (5 sees 11)
     *   pop 8 → result[2]++ (8 sees 11)
     *   pop 10 → result[0]++ (10 sees 11)
     *   push 4
     *
     * i = 5 (9):
     *   11 > 9 → result[4]++ (11 sees 9)
     *   push 5
     *
     * FINAL:
     * [3, 1, 2, 1, 1, 0]
     *
     * ================================================================
     */
}
