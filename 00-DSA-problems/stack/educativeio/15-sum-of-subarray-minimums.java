/**
 * ============================================================================
 * SUM OF SUBARRAY MINIMUMS - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: Given an array of integers, find the minimum value of every 
 * possible contiguous subarray, and return the sum of all these minimums. 
 * Because the sum can be huge, return it modulo 10^9 + 7.
 * 
 * Core Idea & Intuition (The "Contribution" Pattern):
 * Generating all possible subarrays takes O(N^2) time, which will Time Out for 
 * N = 30,000. Instead of asking "What is the minimum of this subarray?", we 
 * reverse the perspective and ask: "For a specific element arr[i], in HOW MANY 
 * subarrays is it the absolute minimum?"
 * 
 * If arr[i] is the minimum for `k` different subarrays, its total contribution 
 * to the final sum is simply `arr[i] * k`. 
 * 
 * To find `k`, we find how far we can stretch a subarray to the left and right 
 * before hitting a number strictly smaller than arr[i]. 
 * - If we can stretch `L` steps left and `R` steps right, the total number 
 *   of subarrays where arr[i] is the minimum is `L * R`.
 * - Finding the "Next Smaller Element" and "Previous Smaller Element" is 
 *   a textbook use-case for a Monotonic Increasing Stack.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "How should I handle duplicate values in the array? E.g., [2, 2]"
 *    Why: Duplicates are the hardest part of this problem. If both 2s claim to 
 *    be the minimum of the subarray [2, 2], we double-count the sum!
 *    Impact: We must handle duplicates asymmetrically. We look for strictly smaller 
 *    elements on one side, and smaller-or-equal elements on the other.
 * 
 * 2. Q: "Can the array contain negative numbers?"
 *    Why: Negative numbers might complicate minimum initializations.
 *    Impact: Constraints say 1 <= arr[i] <= 30000. All positive.
 * 
 * 3. Q: "Should the modulo be applied at the very end or at every step?"
 *    Why: Integer overflow. `sum` can exceed 2^63-1 if we wait until the end.
 *    Impact: We must apply `% 1_000_000_007` at every addition step, and use 
 *    `long` for intermediate multiplications.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example: arr = [3, 1, 2, 4]
 * 
 * Element 3 (idx 0):
 * - Previous Smaller: none (L = 1, just itself)
 * - Next Smaller: 1 at idx 1 (R = 1, just itself)
 * - Subarrays: [3] => Count: 1 * 1 = 1. Contribution: 1 * 3 = 3.
 * 
 * Element 1 (idx 1):
 * - Previous Smaller: none (L = 2: [3, 1])
 * - Next Smaller: none (R = 3: [1], [1, 2], [1, 2, 4])
 * - Subarrays: Count: 2 * 3 = 6. Contribution: 6 * 1 = 6.
 * 
 * Element 2 (idx 2):
 * - Previous Smaller: 1 at idx 1 (L = 1: [2])
 * - Next Smaller: none (R = 2: [2], [2, 4])
 * - Subarrays: Count: 1 * 2 = 2. Contribution: 2 * 2 = 4.
 * 
 * Element 4 (idx 3):
 * - Previous Smaller: 2 at idx 2 (L = 1: [4])
 * - Next Smaller: none (R = 1: [4])
 * - Subarrays: Count: 1 * 1 = 1. Contribution: 1 * 4 = 4.
 * 
 * Total Sum = 3 + 6 + 4 + 4 = 17.
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Acknowledge the constraint (N=30,000) prevents O(N^2).
 * 2. Clarify: Ask about duplicates and modulo constraints.
 * 3. Brute Force: Briefly mention using nested loops to find subarray minimums.
 * 4. Reframing: Explain the "Contribution" technique. "Instead of finding the 
 *    min of subarrays, let's find the subarrays for a min."
 * 5. Data Structure: Identify the need for a Monotonic Stack to find bounds.
 * 6. The Duplicate Trick: Explicitly state, "To avoid double counting, I will 
 *    use strict '<' on the left and '<=' on the right."
 * 7. Code: Write the Optimal approach with two passes (Left pass, then Right pass). 
 *    It is much easier to explain and read than the 1-pass optimization.
 * 8. Dry-run: Trace [3, 1, 2, 4].
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - All identical elements: [2, 2, 2]. Strict/Non-strict bounds perfectly 
 *   prevent double counting.
 * - Strictly increasing: [1, 2, 3]. Everyone's right bound spans to the end.
 * - Strictly decreasing: [3, 2, 1]. Everyone's left bound spans to the start.
 * 
 * Common Mistakes:
 * 1. Modulo location: Doing `sum += (L * R * arr[i]) % MOD`. The multiplication 
 *    `L * R * arr[i]` can overflow a 32-bit `int` BEFORE the modulo applies. 
 *    You must cast to `long` during the multiplication.
 * 2. Symmetrical bounds: Popping `>=` on both the left and right stacks. 
 *    For `[2, 2]`, both 2s would claim the subarray `[2, 2]`, counting it twice.
 * 3. Popping `<`: We are building an INCREASING stack to find the next SMALLER 
 *    element. We pop from the stack while the top is LARGER than the current element.
 */

import java.util.ArrayDeque;
import java.util.Deque;

public class SumOfSubarrayMinimums {

    public static void main(String[] args) {
        int[] arr1 = {3, 1, 2, 4}; // Expected: 17
        int[] arr2 = {11, 81, 94, 43, 3}; // Expected: 444
        int[] arr3 = {2, 2, 2}; // Expected: 12

        System.out.println("--- Brute Force Approach ---");
        System.out.println("Result: " + sumSubarrayMinsBruteForce(arr1));

        System.out.println("\n--- Optimal Stack Approach ---");
        System.out.println("Result: " + sumSubarrayMinsOptimal(arr1));
        System.out.println("Result: " + sumSubarrayMinsOptimal(arr2));
        System.out.println("Result: " + sumSubarrayMinsOptimal(arr3));
    }

    /**
     * SOLUTION 1: BRUTE FORCE
     * ------------------------------------------------------------------------
     * Idea: Generate every subarray (i to j). Keep a running minimum for the 
     * current starting point `i`. Add the running minimum to the total sum.
     * 
     * Time Complexity: O(N^2) - Nested loops over the array.
     * Space Complexity: O(1) - Only a few variables used.
     * Note: Will throw Time Limit Exceeded (TLE) for N = 30,000.
     */
    public static int sumSubarrayMinsBruteForce(int[] arr) {
        int MOD = 1_000_000_007;
        long sum = 0;

        for (int i = 0; i < arr.length; i++) {
            int min = arr[i];
            for (int j = i; j < arr.length; j++) {
                min = Math.min(min, arr[j]);
                sum = (sum + min) % MOD;
            }
        }
        return (int) sum;
    }

    /**
     * SOLUTION 2: OPTIMAL (Monotonic Stack)
     * ------------------------------------------------------------------------
     * Idea: We calculate two arrays:
     * - `left[i]`: Distance to the Previous Less Element.
     * - `right[i]`: Distance to the Next Less or Equal Element.
     * 
     * We use a Monotonic Increasing Stack. If we encounter an element smaller 
     * than the top of the stack, the stack top's "run" as the minimum is broken.
     * 
     * Detailed Dry Run for `[2, 2]` to show Duplicate Handling:
     * Left Pass (Strictly Less -> Pop >=):
     * - i=0, arr[0]=2: Stack empty. left[0] = 1. Push 0.
     * - i=1, arr[1]=2: 2 >= 2, Pop 0. Stack empty. left[1] = 2. Push 1.
     * 
     * Right Pass (Less or Equal -> Pop >):
     * - i=1, arr[1]=2: Stack empty. right[1] = 1. Push 1.
     * - i=0, arr[0]=2: 2 is NOT > 2. No pop. right[0] = 1. Push 0.
     * 
     * Contributions:
     * - idx 0: left=1, right=1. 1 * 1 * 2 = 2. (Accounts for subarray `[2]`)
     * - idx 1: left=2, right=1. 2 * 1 * 2 = 4. (Accounts for `[2]` and `[2, 2]`)
     * Total = 6. No double counting!
     * 
     * Time Complexity: O(N) - 3 independent passes over the array. Each element 
     * is pushed and popped at most once.
     * Space Complexity: O(N) - For the stack and the bounds arrays.
     */
    public static int sumSubarrayMinsOptimal(int[] arr) {
        if (arr == null || arr.length == 0) return 0;

        int n = arr.length;
        int MOD = 1_000_000_007;

        int[] left = new int[n];
        int[] right = new int[n];

        // Store INDICES in the stack, not values, to easily calculate distances
        Deque<Integer> stack = new ArrayDeque<>();

        // 1. Calculate Left Bounds (Previous Strictly Smaller)
        for (int i = 0; i < n; i++) {
            // Pop elements that are GREATER OR EQUAL to current element.
            // This ensures our left bound strictly stops at a smaller element.
            while (!stack.isEmpty() && arr[stack.peek()] >= arr[i]) {
                stack.pop();
            }
            
            // If stack is empty, there is no smaller element to the left.
            // Distance is (i - (-1)) = i + 1.
            left[i] = stack.isEmpty() ? i + 1 : i - stack.peek();
            stack.push(i);
        }

        // Clear stack for the next pass
        stack.clear();

        // 2. Calculate Right Bounds (Next Smaller or Equal)
        for (int i = n - 1; i >= 0; i--) {
            // Pop elements that are STRICTLY GREATER than current element.
            // This asymmetric duplicate handling prevents double counting!
            while (!stack.isEmpty() && arr[stack.peek()] > arr[i]) {
                stack.pop();
            }
            
            // If stack is empty, there is no smaller element to the right.
            // Distance is (n - i).
            right[i] = stack.isEmpty() ? n - i : stack.peek() - i;
            stack.push(i);
        }

        // 3. Calculate Final Contribution Sum
        long totalSum = 0;
        for (int i = 0; i < n; i++) {
            // Cast to long FIRST to prevent 32-bit integer overflow 
            // when multiplying large array distances and values.
            long subArrayCount = (long) left[i] * right[i];
            long contribution = (subArrayCount * arr[i]) % MOD;
            
            totalSum = (totalSum + contribution) % MOD;
        }

        return (int) totalSum;
    }

    /**
     * ========================================================================
     * 6. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time | Space | Trade-offs                 | Interview Rec.
     * ------------------------------------------------------------------------
     * Brute Force    | O(N²)| O(1)  | Will Timeout on large inputs| Conceptual only.
     * Monotonic Stack| O(N) | O(N)  | Requires careful logic      | ⭐ STRONGLY REC.
     * 
     * ========================================================================
     * 7. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if we wanted the Sum of Subarray MAXIMUMS instead?"
     * A1: The algorithm is perfectly identical, except we use a Monotonic Decreasing 
     *     stack. We want to find how far an element stretches as the *maximum*. 
     *     We would pop elements `<` and `<=`.
     * 
     * Q2: "Can you do this in exactly ONE pass instead of three loops?"
     * A2: Yes! When processing the stack left-to-right, the moment an element `arr[i]` 
     *     forces an element `arr[pop_idx]` to pop, `arr[i]` is exactly the Next Smaller 
     *     Element for `arr[pop_idx]`. The new top of the stack is the Previous Smaller 
     *     Element. We can calculate the contribution of `arr[pop_idx]` right there! 
     *     (However, the 2-pass approach is vastly easier to explain and is perfectly 
     *     acceptable for an O(N) rating).
     * 
     * ========================================================================
     * 8. FINAL TAKEAWAYS
     * ========================================================================
     * - The "Contribution" pattern: When a problem asks for aggregate stats (sum, max, min) 
     *   across all possible subarrays, stop trying to build the subarrays. Instead, 
     *   calculate the mathematical contribution of each individual element.
     * - Asymmetric Duplicates: Whenever you stretch boundaries left and right in an 
     *   array with duplicates, use `<` on one side and `<=` on the other to prevent 
     *   overlapping claims.
     * - Always multiply using `long` before applying a modulo in competitive/interview 
     *   programming to avoid silent integer overflows.
     */
}

import java.util.*;

/**
 * ====================================================================================
 * PROBLEM STATEMENT: SUM OF SUBARRAY MINIMUMS (LeetCode 907)
 * ====================================================================================
 * Given an array of integers `arr`, find the sum of `min(b)`, where `b` ranges over
 * every (contiguous) subarray of `arr`. Since the answer may be large, return the 
 * answer modulo 10^9 + 7.
 * 
 * Example:
 *   arr = [3, 1, 2, 4]
 * 
 * Subarrays and their minimums:
 *   [3] -> 3        [3, 1] -> 1       [3, 1, 2] -> 1      [3, 1, 2, 4] -> 1
 *   [1] -> 1        [1, 2] -> 1       [1, 2, 4] -> 1
 *   [2] -> 2        [2, 4] -> 2
 *   [4] -> 4
 * 
 * Total Sum = 3 + 1 + 1 + 1 + 1 + 1 + 1 + 2 + 2 + 4 = 17
 * ====================================================================================
 */

public class SumOfSubarrayMinimums {

    /**
     * Calculates the sum of all subarray minimums in a single pass O(N) time and O(N) space.
     *
     * @param arr Array of positive integers
     * @return Sum of subarray minimums modulo 1,000,000,007
     */
    public static int sumSubarrayMinsOnePass(int[] arr) {
        int n = arr.length;
        long result = 0;
        long MOD = 1_000_000_007;

        // Monotonic Increasing Stack storing indices of candidate elements
        Deque<Integer> stack = new ArrayDeque<>();

        /*
         * ============================================================================
         * THE CORE INTUITION: "CONTRIBUTION TECHNIQUE"
         * ============================================================================
         * Brute force evaluates all O(N^2) subarrays and finds their minimums.
         * Instead, we invert the question:
         * 
         *   "For each element arr[mid], HOW MANY SUBARRAYS have arr[mid] 
         *    as their ABSOLUTE MINIMUM?"
         * 
         * If arr[mid] is the minimum for K subarrays, its total contribution to 
         * the final sum is simply: (arr[mid] * K).
         * 
         * ----------------------------------------------------------------------------
         * HOW DO WE FIND 'K' FOR AN ELEMENT AT INDEX 'mid'?
         * ----------------------------------------------------------------------------
         * We need to determine how far to the LEFT and RIGHT `arr[mid]` remains 
         * the minimum element:
         * 
         * 1. `leftIndex`: Index of the Previous Smaller Element (PSE) to the left.
         * 2. `rightIndex`: Index of the Next Smaller Element (NSE) to the right.
         * 
         * ASCII DIAGRAM OF BOUNDARIES FOR `arr[mid]`:
         * 
         *   Indices:   leftIndex ......... mid ......... rightIndex
         *   Values:    [PSE]   (  > arr[mid]  )   [mid]   (  >= arr[mid] )   [NSE]
         *                          <--------- left ---------><------- right ------->
         * 
         * - Number of choices for the START boundary = `left  = mid - leftIndex`
         * - Number of choices for the END boundary   = `right = rightIndex - mid`
         * 
         * Total subarrays where arr[mid] is the minimum:
         *   Total Subarrays (K) = left * right
         *   Contribution        = arr[mid] * left * right
         * 
         * ============================================================================
         * DUPLES AND EQUAL ELEMENTS (Avoiding Double Counting)
         * ============================================================================
         * If `arr` contains duplicate values (e.g., [2, 1, 1, 2]), how do we prevent
         * counting the subarray [1, 1] twice?
         * 
         * Rule:
         * - Strict inequality on one side (`>`): Previous STRICTLY Smaller Element.
         * - Non-strict inequality on the other side (`>=`): Next SMALLER OR EQUAL Element.
         * 
         * This asymmetric handling guarantees that every subarray with duplicate 
         * minimums is assigned uniquely to exactly ONE instance of that minimum!
         * ============================================================================
         */

        /*
         * We run the loop from 0 to n (inclusive):
         * - When `i < n`, `current = arr[i]`.
         * - When `i == n`, `current = 0` (Sentinel/Dummy Element).
         * 
         * WHY THE DUMMY ELEMENT (`i == n`)?
         * ----------------------------------------------------------------------------
         * At the end of the array, some elements may still remain in `stack` because 
         * they never saw a smaller element to their right.
         * By treating `arr[n]` as `0` (a value smaller than any valid `arr` element),
         * we force the `while` loop to pop and compute contributions for ALL remaining 
         * elements in `stack`. This flushes the stack in ONE pass without extra code!
         */
        for (int i = 0; i <= n; i++) {

            int current = (i == n) ? 0 : arr[i];

            /*
             * Maintain a Monotonic INCREASING Stack:
             * While `current` is STRICTLY SMALLER than the element at `stack.peek()`:
             *   -> `current` IS the Next Smaller Element (NSE) for `arr[stack.peek()]`!
             *   -> We pop `mid = stack.pop()` and process its contribution.
             */
            while (!stack.isEmpty() && arr[stack.peek()] > current) {
                
                int mid = stack.pop(); // The element whose contribution we are calculating

                // 1. Previous Smaller Element (PSE) index
                // If stack is empty, there is no smaller element to the left -> virtual index = -1
                int leftIndex = stack.isEmpty() ? -1 : stack.peek();

                // 2. Next Smaller Element (NSE) index
                // `i` is the current index causing `mid` to be popped
                int rightIndex = i;

                // 3. Count available choices on left and right
                long left = mid - leftIndex;   // Distance to PSE
                long right = rightIndex - mid; // Distance to NSE

                // 4. Calculate total contribution of arr[mid]
                long count = (left * right);
                long contribution = (arr[mid] * count);

                result = (result + contribution) % MOD;
            }

            // Push current index to stack to find its own NSE in future iterations
            stack.push(i);
        }

        return (int) result;
    }

    public static void main(String[] args) {
        int[] arr = {3, 1, 2, 4};
        System.out.println("Sum of Subarray Minimums: " + sumSubarrayMinsOnePass(arr)); // Output: 17
    }
}

/**
 * ====================================================================================
 * STEP-BY-STEP EXECUTION WALKTHROUGH FOR `arr = [3, 1, 2, 4]`
 * ====================================================================================
 *
 * Array:   index:  0  1  2  3  (4: Virtual Sentinel = 0)
 *          value:  3  1  2  4   0
 *
 * ------------------------------------------------------------------------------------
 * Iteration i = 0: current = arr[0] = 3
 * ------------------------------------------------------------------------------------
 * - Stack empty. `while` condition false.
 * - `stack.push(0)`.
 * - Stack State (top to bottom): [0] (val 3)
 *
 * ------------------------------------------------------------------------------------
 * Iteration i = 1: current = arr[1] = 1
 * ------------------------------------------------------------------------------------
 * - Check `while`: `stack.peek()` is 0 (val 3). Is 3 > 1? -> TRUE!
 *   
 *   1. `mid = stack.pop()` -> mid = 0 (val = 3)
 *   2. `leftIndex = stack.isEmpty() ? -1` -> leftIndex = -1
 *   3. `rightIndex = 1`
 *   4. `left = 0 - (-1) = 1`
 *   5. `right = 1 - 0 = 1`
 *   6. Subarrays where 3 is min = 1 * 1 = 1 (which is [3])
 *   7. `contribution = 3 * 1 = 3`. `result = 3`
 *
 * - `while` loop ends (stack empty).
 * - `stack.push(1)`.
 * - Stack State (top to bottom): [1] (val 1)
 *
 * ------------------------------------------------------------------------------------
 * Iteration i = 2: current = arr[2] = 2
 * ------------------------------------------------------------------------------------
 * - Check `while`: `stack.peek()` is 1 (val 1). Is 1 > 2? -> FALSE.
 * - `stack.push(2)`.
 * - Stack State (top to bottom): [2, 1] (vals 2, 1)
 *
 * ------------------------------------------------------------------------------------
 * Iteration i = 3: current = arr[3] = 4
 * ------------------------------------------------------------------------------------
 * - Check `while`: `stack.peek()` is 2 (val 2). Is 2 > 4? -> FALSE.
 * - `stack.push(3)`.
 * - Stack State (top to bottom): [3, 2, 1] (vals 4, 2, 1)
 *
 * ------------------------------------------------------------------------------------
 * Iteration i = 4 (FLUSH STAGE): current = sentinel = 0
 * ------------------------------------------------------------------------------------
 * Stack currently holds indices: [3, 2, 1]
 *
 * Pop 1:
 * - `mid = 3` (val = 4)
 * - `leftIndex = stack.peek()` -> 2
 * - `rightIndex = 4`
 * - `left = 3 - 2 = 1`, `right = 4 - 3 = 1` -> count = 1 * 1 = 1 (subarray [4])
 * - `contribution = 4 * 1 = 4`. `result = 3 + 4 = 7`
 *
 * Pop 2:
 * - `mid = 2` (val = 2)
 * - `leftIndex = stack.peek()` -> 1
 * - `rightIndex = 4`
 * - `left = 2 - 1 = 1`, `right = 4 - 2 = 2` -> count = 1 * 2 = 2 (subarrays [2], [2, 4])
 * - `contribution = 2 * 2 = 4`. `result = 7 + 4 = 11`
 *
 * Pop 3:
 * - `mid = 1` (val = 1)
 * - `leftIndex = stack.isEmpty() ? -1` -> -1
 * - `rightIndex = 4`
 * - `left = 1 - (-1) = 2`, `right = 4 - 1 = 3` -> count = 2 * 3 = 6 
 *   (subarrays [1], [1, 2], [1, 2, 4], [3, 1], [3, 1, 2], [3, 1, 2, 4])
 * - `contribution = 1 * 6 = 6`. `result = 11 + 6 = 17`
 *
 * Loop ends. Return (int) (17 % 1,000,000,007) = 17.
 *
 * ====================================================================================
 * COMPLEXITY ANALYSIS
 * ====================================================================================
 * Time Complexity: O(N)
 * - Every index `0` to `n-1` is pushed onto `stack` EXACTLY ONCE.
 * - Every index is popped from `stack` AT MOST ONCE.
 * - The `while` loop runs at most N times total across the entire execution.
 * - Total operations = O(N) linear time.
 *
 * Space Complexity: O(N)
 * - `stack` stores at most N + 1 indices in the worst case (strictly increasing input).
 * ====================================================================================
 */
