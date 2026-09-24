/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an abstract m x n matrix representing a multiplication table.
 * The value at row 'i' and column 'j' is simply 'i * j' (using 1-based indexing).
 * We need to find the k-th smallest element in this entire table.
 * 
 * IMPORTANT: The constraints for m and n are up to 30,000. This means the total 
 * number of elements in the table can be up to 900,000,000.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can we just generate the entire table, sort it, and find the k-th element?
 *    (No, 900 million elements will cause a Memory Limit Exceeded (MLE) error, 
 *     as it requires gigabytes of RAM. We need an O(1) space solution).
 * 2. Can there be duplicate values in the table?
 *    (Yes, e.g., 2 * 3 = 6 and 3 * 2 = 6. The k-th smallest element must account 
 *     for these duplicates as separate entries).
 * 3. Will 'k' always be a valid index?
 *    (Yes, constraints guarantee 1 <= k <= m * n).
 * 4. Is the table 0-indexed or 1-indexed?
 *    (The multiplication values are 1-indexed: i=1..m, j=1..n. But our standard 
 *     code logic can handle loops accordingly).
 * 
 * ============================================================================
 * EDGE CASES & EXAMPLES TO THINK OF
 * ============================================================================
 * 1. k = 1: The smallest element is always 1 * 1 = 1.
 * 2. k = m * n: The largest element is always m * n.
 * 3. 1D Table (m = 1 or n = 1): 
 *    m=1, n=5 -> [1, 2, 3, 4, 5]. The k-th smallest is just k.
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (Generate and Sort):
 *    - Create a 1D array of size m * n.
 *    - Use nested loops to calculate `i * j` and fill the array.
 *    - Sort the array and return the element at index `k - 1`.
 *    - Time: O(M*N log(M*N)), Space: O(M*N).
 *    - Verdict: Guaranteed to fail (Time Limit Exceeded & Memory Limit Exceeded).
 * 
 * 2. Min-Heap / Priority Queue (The Matrix Traversal Approach):
 *    - Similar to finding the k-th smallest in a sorted matrix.
 *    - Push the first element of each row into a Min-Heap.
 *    - Pop the smallest element 'k' times. Each time we pop, if the element 
 *      was from column 'j', push the element from column 'j+1' of the same row.
 *    - Time: O(K log M), Space: O(M).
 *    - Verdict: Will fail with Time Limit Exceeded (TLE) if k is close to 
 *      900,000,000, because popping 900 million times is too slow.
 * 
 * 3. Binary Search on Answer Space (The Most Optimal Approach):
 *    - The smallest possible value in the table is 1. The largest is M * N.
 *    - Since the values are monotonic, we can Binary Search for the ANSWER!
 *    - For a guessed middle value `mid`, how many numbers in the multiplication 
 *      table are less than or equal to `mid`?
 *    - In row `i`, the multiples are `i, 2i, 3i ... n * i`. 
 *    - The number of multiples <= `mid` in row `i` is simply `mid / i`.
 *    - However, a row only has `n` columns, so the count is `min(mid / i, n)`.
 *    - We sum this count across all `m` rows. 
 *    - If total_count >= k, the true answer is `mid` or something smaller. (high = mid)
 *    - If total_count < k, the true answer is strictly greater than `mid`. (low = mid + 1)
 *    - Time: O(M log(M*N)), Space: O(1).
 *    - Verdict: Extremely fast, fully optimal.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Binary Search Approach)
 * ============================================================================
 * m = 3, n = 3, k = 5
 * Multiplication Table:
 * 1  2  3
 * 2  4  6
 * 3  6  9
 * Sorted Table: 1, 2, 2, 3, 3, 4, 6, 6, 9.  (5th smallest is 3).
 * 
 * Search Range: low = 1, high = 9
 * 
 * Iteration 1:
 * - mid = (1 + 9) / 2 = 5
 * - Count elements <= 5:
 *   - Row 1: min(5 / 1, 3) = min(5, 3) = 3
 *   - Row 2: min(5 / 2, 3) = min(2, 3) = 2
 *   - Row 3: min(5 / 3, 3) = min(1, 3) = 1
 *   - Total count = 3 + 2 + 1 = 6.
 * - Since 6 >= 5 (k), our answer is <= 5. Update high = 5.
 * 
 * Iteration 2:
 * - low = 1, high = 5. mid = 3
 * - Count elements <= 3:
 *   - Row 1: min(3 / 1, 3) = 3
 *   - Row 2: min(3 / 2, 3) = 1
 *   - Row 3: min(3 / 3, 3) = 1
 *   - Total count = 3 + 1 + 1 = 5.
 * - Since 5 >= 5 (k), update high = 3.
 * 
 * Iteration 3:
 * - low = 1, high = 3. mid = 2
 * - Count elements <= 2:
 *   - Row 1: min(2 / 1, 3) = 2
 *   - Row 2: min(2 / 2, 3) = 1
 *   - Row 3: min(2 / 3, 3) = 0
 *   - Total count = 2 + 1 + 0 = 3.
 * - Since 3 < 5 (k), update low = mid + 1 = 3.
 * 
 * low == high == 3. Loop breaks. Return 3.
 */

import java.util.*;

public class KthSmallestInMultiplicationTable {

    public static void main(String[] args) {
        int m1 = 3, n1 = 3, k1 = 5;
        int m2 = 2, n2 = 3, k2 = 6;
        int m3 = 30000, n3 = 30000, k3 = 900000000;

        System.out.println("--- Test Case 1 ---");
        System.out.println("m=" + m1 + ", n=" + n1 + ", k=" + k1);
        System.out.println("Result: " + findKthNumberOptimal(m1, n1, k1)); // Expected: 3

        System.out.println("\n--- Test Case 2 ---");
        System.out.println("m=" + m2 + ", n=" + n2 + ", k=" + k2);
        System.out.println("Result: " + findKthNumberOptimal(m2, n2, k2)); // Expected: 6
        
        System.out.println("\n--- Test Case 3 (Extreme Scale) ---");
        System.out.println("m=" + m3 + ", n=" + n3 + ", k=" + k3);
        // This takes milliseconds using Binary Search, but would crash Memory on Brute Force
        System.out.println("Result: " + findKthNumberOptimal(m3, n3, k3)); 
    }

    /**
     * SOLUTION: Binary Search on Answer Space (Optimal)
     * 
     * Idea: We know the answer must be between 1 and M*N. We binary search this 
     * range. For each guess (mid), we calculate exactly how many numbers in the 
     * table are less than or equal to 'mid' by iterating through the rows.
     * 
     * Time Complexity: O(M * log(M * N))
     *                  Binary search takes log(M*N) steps. Each step we do an O(M) loop.
     *                  For 30,000 * 30,000, log(9*10^8) ≈ 30. 
     *                  30 * 30,000 = 900,000 ops. Incredibly fast!
     * Space Complexity: O(1) - Constant auxiliary space.
     */
    public static int findKthNumberOptimal(int m, int n, int k) {
        // The value range of the table is [1, m * n]
        int low = 1;
        int high = m * n;

        // Binary search for the exact value
        while (low < high) {
            int mid = low + (high - low) / 2;
            
            // If the count of numbers <= mid is sufficient (>= k),
            // then 'mid' is a potential answer, but there could be a smaller 
            // valid answer. So we tighten the upper bound.
            if (countElementsLessOrEqual(mid, m, n) >= k) {
                high = mid; 
            } else {
                // If the count is < k, 'mid' is strictly too small to be the 
                // k-th element. We must look higher.
                low = mid + 1;
            }
        }
        
        // When low == high, we have locked onto the exact k-th smallest value.
        return low;
    }

    /**
     * Helper Method for Binary Search.
     * Counts how many elements in the m x n multiplication table are <= target.
     * 
     * Time Complexity: O(M)
     */
    private static int countElementsLessOrEqual(int target, int m, int n) {
        int count = 0;
        
        // Iterate through every row 'i' from 1 to m
        for (int i = 1; i <= m; i++) {
            // In row i, the values are i, 2i, 3i ... 
            // The number of values <= target is target / i.
            // However, a row only has 'n' columns, so we cap it at 'n'.
            int countInRow = Math.min(target / i, n);
            
            // Optimization: If countInRow is 0, it means the very first element 
            // of this row (which is 'i') is strictly greater than the target.
            // Since subsequent rows will start with even larger numbers, we can 
            // safely break out of the loop early!
            if (countInRow == 0) {
                break;
            }
            
            count += countInRow;
        }
        
        return count;
    }

    /**
     * ============================================================================
     * FOLLOW-UPS TO PREPARE FOR
     * ============================================================================
     * 1. How is this different from "Kth Smallest Element in a Sorted Matrix"?
     *    Answer: In a standard sorted matrix, values are arbitrary, so we don't 
     *    have a mathematical formula like `target / i` to count elements in O(1) 
     *    per row. For a sorted matrix, we have to use a Two-Pointer/Staircase 
     *    approach to count elements <= mid, which takes O(M + N) time inside the 
     *    binary search, making the total time O((M+N) * log(Max - Min)).
     * 
     * 2. What if m and n are extremely large (e.g., 10^9), but k is very small?
     *    Answer: If k is very small (e.g., k=100), Binary Search O(M log(M*N)) 
     *    might become inefficient because the M loop runs 10^9 times. In that specific 
     *    case, switching to the Min-Heap approach O(K log K) would be vastly 
     *    superior, because we would only do 100 heap operations.
     */
}




/**
 * =============================================================================
 * 🔥 Kth Smallest Number in Multiplication Table — MASTER INTERVIEW TEMPLATE
 * =============================================================================
 *
 * 🧠 PROBLEM SUMMARY:
 * -----------------------------------------------------------------------------
 * We are given:
 * - A multiplication table of size m × n
 * - mat[i][j] = i * j (1-based indexing)
 *
 * Example (m = 3, n = 3):
 *      1  2  3
 *      2  4  6
 *      3  6  9
 *
 * We need to find:
 * 👉 The k-th smallest number in this table
 *
 * -----------------------------------------------------------------------------
 *
 * 🚨 WHY BRUTE FORCE FAILS:
 * -----------------------------------------------------------------------------
 * Total elements = m * n (can be up to 9e8)
 *
 * ❌ Cannot:
 *   - Generate full matrix
 *   - Sort all elements
 *
 * 👉 We MUST avoid building the matrix
 *
 * -----------------------------------------------------------------------------
 *
 * 🧠 CORE INSIGHT (MOST IMPORTANT):
 * -----------------------------------------------------------------------------
 * We are NOT searching index → we are searching VALUE
 *
 * 👉 Convert problem:
 *
 *   "Find k-th smallest element"
 *          ↓
 *   "Find smallest value x such that there are at least k elements ≤ x"
 *
 * -----------------------------------------------------------------------------
 *
 * 🔥 BINARY SEARCH ON ANSWER (KEY PATTERN)
 * -----------------------------------------------------------------------------
 *
 * WHEN TO USE:
 * ✔ kth smallest / largest
 * ✔ minimize maximum / threshold problems
 * ✔ cannot build full data
 * ✔ can check condition for a value
 *
 * -----------------------------------------------------------------------------
 *
 * 🧠 CRITICAL QUESTION:
 * -----------------------------------------------------------------------------
 * Can we answer:
 *
 *   👉 "How many numbers in the table are ≤ x?"
 *
 * If YES → Binary Search works 🚀
 *
 * -----------------------------------------------------------------------------
 *
 * 💡 COUNT FUNCTION DERIVATION:
 * -----------------------------------------------------------------------------
 * Row i contains:
 *   i×1, i×2, i×3, ..., i×n
 *
 * We want:
 *   i × j ≤ x
 *   → j ≤ x / i
 *
 * So:
 *   count in row i = min(n, x / i)
 *
 * Total:
 *   f(x) = Σ min(n, x / i) for i = 1..m
 *
 * -----------------------------------------------------------------------------
 *
 * 📈 MONOTONIC PROPERTY (WHY BS WORKS):
 * -----------------------------------------------------------------------------
 * f(x) = count of numbers ≤ x
 *
 * As x increases:
 *   f(x) never decreases ❗
 *
 * Example:
 *   x:   1  2  3  4  5  6
 *   f(x):1  3  5  6  6  8
 *
 * 👉 Monotonic Increasing Function
 *
 * -----------------------------------------------------------------------------
 *
 * 🎯 BINARY SEARCH GOAL:
 * -----------------------------------------------------------------------------
 * Find:
 *
 *   smallest x such that f(x) ≥ k
 *
 * Pattern:
 *   ❌ ❌ ❌ ✅ ✅ ✅
 *           ↑
 *       first TRUE
 *
 * -----------------------------------------------------------------------------
 *
 * 🧠 SEARCH SPACE:
 * -----------------------------------------------------------------------------
 * Minimum possible value = 1 × 1 = 1
 * Maximum possible value = m × n
 *
 * 👉 Search in range [1, m * n]
 *
 * -----------------------------------------------------------------------------
 *
 * ⚠️ IMPORTANT EDGE CASES:
 * -----------------------------------------------------------------------------
 * ✔ Duplicates exist (e.g., 2 appears twice)
 * ✔ Some numbers missing (e.g., 5 not in table)
 * ✔ Must return FIRST valid value (not any)
 *
 * -----------------------------------------------------------------------------
 *
 * ⏱ TIME COMPLEXITY:
 * -----------------------------------------------------------------------------
 * Binary Search: log(m * n)
 * Count function: O(m)
 *
 * Total: O(m log(m * n))
 *
 * -----------------------------------------------------------------------------
 *
 * 🧠 INTERVIEW THINKING FLOW:
 * -----------------------------------------------------------------------------
 * 1. Cannot brute force → too large
 * 2. Asked kth smallest → think value space
 * 3. Can I check condition for x? → YES
 * 4. Is function monotonic? → YES
 * 5. Apply Binary Search on Answer 🚀
 *
 * =============================================================================
 */
public class KthSmallestMultiplicationTable {

    public static void main(String[] args) {
        int m = 3, n = 3, k = 5;

        // Expected sorted values:
        // 1 2 2 3 3 4 6 6 9
        // k = 5 → answer = 3

        System.out.println(findKthNumber(m, n, k)); // Output: 3
    }

    /**
     * =============================================================================
     * 🔍 MAIN FUNCTION: Binary Search on Answer
     * =============================================================================
     *
     * Goal:
     * Find smallest number x such that there are ≥ k numbers ≤ x
     *
     * -----------------------------------------------------------------------------
     * INVARIANT:
     * -----------------------------------------------------------------------------
     * low → definitely invalid (too small)
     * high → potential answer zone
     *
     * -----------------------------------------------------------------------------
     * PATTERN:
     * -----------------------------------------------------------------------------
     * if f(mid) ≥ k → possible answer → go LEFT (try smaller)
     * if f(mid) < k → too small → go RIGHT
     *
     * -----------------------------------------------------------------------------
     */
    public static int findKthNumber(int m, int n, int k) {

        int low = 1;           // smallest possible value in table
        int high = m * n;      // largest possible value

        int answer = -1;       // explicit answer tracking (INTERVIEW BEST PRACTICE)

        while (low <= high) {

            // Safe mid calculation (avoids overflow)
            int mid = low + (high - low) / 2;

            // Count how many numbers are ≤ mid
            long count = countLessOrEqual(mid, m, n);

            /**
             * 🎯 DECISION:
             *
             * If count >= k:
             *   → mid is BIG ENOUGH to include k elements
             *   → candidate answer
             *   → BUT maybe smaller answer exists
             *   → move LEFT
             *
             * Else:
             *   → mid is TOO SMALL
             *   → need bigger values
             *   → move RIGHT
             */
            if (count >= k) {
                answer = mid;       // store potential answer
                high = mid - 1;     // try to minimize answer
            } else {
                low = mid + 1;      // increase value
            }
        }

        return answer;
    }

    /**
     * =============================================================================
     * 🔢 COUNT FUNCTION: Core Logic
     * =============================================================================
     *
     * Counts how many numbers in multiplication table are ≤ x
     *
     * -----------------------------------------------------------------------------
     * KEY IDEA:
     * -----------------------------------------------------------------------------
     * Row i:
     *   i×1, i×2, ..., i×n
     *
     * We want:
     *   i × j ≤ x
     *
     * Solve:
     *   j ≤ x / i
     *
     * So:
     *   count in row i = min(n, x / i)
     *
     * -----------------------------------------------------------------------------
     *
     * 💡 WHY min(n, x/i)?
     *
     * Because:
     * - x/i gives how many multiples fit
     * - but max columns = n
     *
     * -----------------------------------------------------------------------------
     *
     * 🧠 EXAMPLE:
     * -----------------------------------------------------------------------------
     * m=3, n=3, x=5
     *
     * Row 1: 1,2,3 → count = min(3,5/1=5) = 3
     * Row 2: 2,4,6 → count = min(3,5/2=2) = 2
     * Row 3: 3,6,9 → count = min(3,5/3=1) = 1
     *
     * Total = 6
     *
     * -----------------------------------------------------------------------------
     */
    private static long countLessOrEqual(int x, int m, int n) {

        long count = 0;

        /**
         * Loop through each row
         *
         * Optimization:
         * We can stop at i <= x because:
         * if i > x → x/i = 0 → no contribution
         */
        for (int i = 1; i <= m; i++) {

            // Count elements ≤ x in row i
            count += Math.min(n, x / i);
        }

        return count;
    }
}
