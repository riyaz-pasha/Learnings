/**
 * ============================================================================
 * 961. N-REPEATED ELEMENT IN SIZE 2N ARRAY
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Can the array be mutated?" 
 *    (If yes, we could potentially sort in-place. If no, we must use auxiliary space or O(1) read-only algorithms).
 * 2. "Are the values guaranteed to be positive, or can they be negative?" 
 *    (The constraint 0 <= nums[i] <= 10^4 allows us to use a simple boolean array mapped directly to indices).
 * 3. "Is 'n' always at least 2?" 
 *    (Yes, constraints say 2 <= n <= 5000. This guarantees the array size is at least 4, preventing out-of-bounds exceptions when checking gaps of size 2 or 3).
 * 4. "Is this a strict 'majority element' problem?"
 *    (No, the target appears exactly 50% of the time, not strictly greater than 50%. This immediately disqualifies standard majority-vote algorithms).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We have an array of size 2N. One element appears N times, meaning it occupies exactly 
 * > half the array. All other N elements are unique. We need to find the duplicate without doing unnecessary work.
 * 
 * ### Approach 1: The Hash Set (The Standard Developer Way)
 * 1. What I'd naturally try: As I iterate through the array, I just need to find the first element I've seen before. 
 *    I'll throw every element into a `HashSet`. The moment `set.add(num)` returns false, I've found it.
 * 2. Why it works: Since all non-target elements appear exactly once, the *only* element that will ever trigger a duplicate 
 *    check is our target element.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because in the worst case, we traverse the array once, and HashSet lookups are O(1).
 *    - Space Complexity: O(N) — because if the duplicate happens to be at the very end of the array, we store N unique elements in the Set.
 * 4. What work is being repeated: We are using a heavy Object-based data structure, computing hash codes, and allocating 
 *    heap memory for `Integer` wrappers just to remember if we've seen a small integer.
 * 5. What property removes the bottleneck: The problem states `nums[i] <= 10^4`. This domain is incredibly small.
 * 
 * ### Approach 2: Bounded Boolean Array (The Hardware-Friendly Optimum)
 * 1. What I'd naturally try: Replace the `HashSet<Integer>` with a `boolean[10001]`.
 * 2. Why it works: Array indices map directly to the values. `seen[nums[i]] = true`. It is the same logic as Approach 1, 
 *    executed on bare metal without hashing overhead.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we scan the array sequentially.
 *    - Space Complexity: O(1) — because we allocate exactly one 10,001-sized boolean array (about 10 Kilobytes) regardless 
 *      of whether N is 2 or 5000.
 * 4. What work is being repeated: We are allocating memory based on the *maximum possible value*, not the *actual data structure size*. 
 *    If `nums[i]` could be up to 10^9, this approach would fail with an OutOfMemoryError.
 * 5. What property removes the bottleneck: The Pigeonhole Principle! We have N identical elements stuffed into 2N slots. 
 *    They simply *cannot* hide very far from each other.
 * 
 * ### Approach 3: The Gap Check (The Pure Algorithmic Optimum)
 * 1. What I'd naturally try: Instead of tracking what I've seen in memory, I'll just check if the current element matches 
 *    its immediate neighbors.
 * 2. Why it works: If you have N targets in 2N spaces, they must clump together. If they try to spread out to avoid being 
 *    next to each other (e.g., `[X, A, X, B, X, C]`), they are still separated by at most a gap of 2. 
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we iterate the array once doing basic equality checks.
 *    - Space Complexity: O(1) auxiliary — because we literally use zero extra memory structures, just loop pointers. 
 *      This works even if values were bounded up to 2^31-1.
 * 
 * > **Interview Strategy:** In a real interview, I would briefly write Approach 2 (Boolean array) as the fastest practical 
 * > code, but I would immediately follow up by explaining Approach 3 (Gap Check) to prove a deep mathematical understanding 
 * > of the pigeonhole principle.
 * 
 * 
 * ## 3. Edge cases
 * - N = 2, max separation: `[1, 2, 3, 1]`. The gap between the 1s is exactly 3. This is the mathematical maximum gap possible, 
 *   and it ONLY happens when N = 2 and the duplicates are at the extreme ends.
 * - Duplicates at the front: `[1, 1, 2, 3]`. Loop must catch this immediately on iteration 0.
 * - Array element is 0: Handled flawlessly by both Boolean Array and Gap Check, but a pitfall if using an uninitialized `int[]` for frequencies.
 * 
 * 
 * ## 4. Key Insight, Pitfalls & Dry Run
 * 
 * **Key Insight: The Pigeonhole Proof**
 * Why do we only check `nums[i] == nums[i+1]` and `nums[i] == nums[i+2]`?
 * Suppose a sequence has NO identical elements at distance 1 or 2. This means every pair of targets is separated by at least 
 * TWO non-targets (distance 3). If we have N targets, we need (N - 1) gaps.
 * (N - 1) gaps * 2 non-targets per gap = 2N - 2 non-targets required.
 * But we only HAVE N non-targets in the array! 
 * So: `2N - 2 <= N`  --->  `N <= 2`.
 * Therefore, for any N >= 3, a gap of 1 or 2 is MATHEMATICALLY GUARANTEED. 
 * For N = 2, the only way to avoid a gap of 1 or 2 is `[X, A, B, X]`. We can just handle this edge case by returning `nums[0]`.
 * 
 * **Pitfall: The Boyer-Moore Trap**
 * A very common mistake is attempting to use the Boyer-Moore Majority Vote algorithm.
 * Boyer-Moore requires the target to appear strictly MORE than 50% of the time. Here, the target is EXACTLY 50%.
 * If the input is `[X, A, X, B, X, C]`, Boyer-Moore's counter will drop to 0 at the end, and you'll return the wrong answer.
 * 
 * **Dry Run of Approach 3 (Gap Check):**
 * Input: `[2, 1, 2, 5, 3, 2]`. (N = 3, Size = 6).
 * 
 * - i = 0: nums[0] is 2. 
 *   Check i+1: 2 == 1? No.
 *   Check i+2: 2 == 2? YES!
 *   Return 2 immediately.
 * 
 * 
 * ## 5. Follow-ups
 * - **Q:** What if the array is read-only, has 10 billion elements, and values go up to 2^31 - 1? 
 *   **A:** Approach 1 (Set) would run out of RAM. Approach 2 (Boolean Array) would require a 2GB array, which is terrible. 
 *   Approach 3 (Gap Check) solves it flawlessly because it requires exactly O(1) space and scales infinitely.
 * - **Q:** What if the target element appeared N-1 times instead of N?
 *   **A:** The mathematical gap guarantee weakens. The targets could be spread out further. We would have to rely on 
 *   the HashSet approach, or sort the array first (O(N log N)).
 * 
 * ============================================================================
 */

import java.util.HashSet;
import java.util.Set;

public class NRepeatedElementStudy {

    /**
     * Interface to test different implementations uniformly.
     */
    interface NRepeatedSolver {
        int repeatedNTimes(int[] nums);
    }

    // ========================================================================
    // APPROACH 3: THE GAP CHECK (The Algorithmic Optimum)
    // ========================================================================
    static class GapCheckSolver implements NRepeatedSolver {
        @Override
        public int repeatedNTimes(int[] nums) {
            // Check windows of size 3 (distances of 1 and 2).
            // We stop at nums.length - 2 to prevent ArrayIndexOutOfBoundsException.
            for (int i = 0; i < nums.length - 2; i++) {
                // Is the element next to itself? (Gap of 1)
                if (nums[i] == nums[i + 1]) {
                    return nums[i];
                }
                // Is the element separated by one other element? (Gap of 2)
                if (nums[i] == nums[i + 2]) {
                    return nums[i];
                }
            }
            
            // If we finish the loop without finding anything, it means N = 2 
            // and the array is formatted exactly like [X, A, B, X].
            // In this unique mathematical edge case, the answer must be nums[0].
            return nums[0];
        }
    }

    // ========================================================================
    // APPROACH 2: BOUNDED BOOLEAN ARRAY (The Pragmatic Speed Optimum)
    // ========================================================================
    static class BooleanArraySolver implements NRepeatedSolver {
        @Override
        public int repeatedNTimes(int[] nums) {
            // Constraint: 0 <= nums[i] <= 10^4
            // Allocate exactly enough space for the maximum possible value.
            boolean[] seen = new boolean[10001];
            
            for (int num : nums) {
                // If we've flipped this bit to true before, we found our duplicate.
                if (seen[num]) {
                    return num;
                }
                seen[num] = true;
            }
            
            return -1; // Should never be reached given problem constraints
        }
    }

    // ========================================================================
    // APPROACH 1: HASH SET (The General Case)
    // ========================================================================
    static class SetSolver implements NRepeatedSolver {
        @Override
        public int repeatedNTimes(int[] nums) {
            // Standard HashSet handles arbitrary objects and arbitrary sizes
            Set<Integer> seen = new HashSet<>();
            
            for (int num : nums) {
                // set.add() returns false if the element is already present
                if (!seen.add(num)) {
                    return num;
                }
            }
            
            return -1;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        NRepeatedSolver[] solvers = {
            new GapCheckSolver(),
            new BooleanArraySolver(),
            new SetSolver()
        };

        // Format: {input_array, expected_result}
        int[][] testInputs = {
            {1, 2, 3, 3},             // N=2, adjacent at the end
            {2, 1, 2, 5, 3, 2},       // N=3, standard distribution
            {5, 1, 5, 2, 5, 3, 5, 4}, // N=4, widely spaced (gap 2 everywhere)
            {1, 2, 3, 1},             // N=2, edge case [X, A, B, X]
            {1, 1, 2, 3}              // N=2, adjacent at the start
        };
        
        int[] expectedOutputs = {3, 2, 5, 1, 1};

        for (NRepeatedSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testInputs.length; i++) {
                int[] input = testInputs[i];
                int expected = expectedOutputs[i];
                int result = solver.repeatedNTimes(input);
                
                if (result != expected) {
                    System.out.printf("  [FAIL] Test %d. Expected: %d, Got: %d%n", i + 1, expected, result);
                    allPassed = false;
                }
            }
            
            if (allPassed) {
                System.out.println("  [SUCCESS] All test cases passed!\n");
            }
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Fast Duplicate Detection.
 * - Key observation: When placing N identical elements into 2N slots, the Pigeonhole 
 *   Principle guarantees they cannot be separated by more than 2 spaces (except the 
 *   N=2 edge case).
 * - What's worth memorizing: The Gap Check logic (`nums[i] == nums[i+1] || nums[i] == nums[i+2]`). 
 *   It allows O(1) space deduplication without sorting.
 * - Most common trap: Trying to use Boyer-Moore Majority Vote. That algorithm only 
 *   works when a target appears > 50% of the time, not exactly 50%.
 * - Mental trigger: "N elements in 2N array? Pigeonhole distance check."
 * ============================================================================
 */


/*
 * Problem:
 * --------
 * Given an array of size 2n:
 * - There are n + 1 unique elements
 * - n elements appear exactly once
 * - 1 element appears exactly n times
 *
 * Goal:
 * -----
 * Return the element that appears n times.
 *
 * ------------------------------------------------------------
 * 🧠 KEY INTERVIEW INSIGHT:
 * ------------------------------------------------------------
 * The repeated element appears VERY frequently (n times in 2n size array).
 *
 * This creates an important constraint:
 * → It is IMPOSSIBLE to place all occurrences far apart.
 *
 * Why?
 * ----
 * Even if we try to spread them:
 *
 * Example (worst spacing attempt):
 *   x _ x _ x _ x _
 *
 * The maximum gap we can maintain is small (≤ 3).
 *
 * 👉 Therefore:
 * The repeated element MUST appear within a small distance (1, 2, or 3).
 *
 * ------------------------------------------------------------
 * 💡 STRATEGY:
 * ------------------------------------------------------------
 * Instead of using extra space (HashMap/Set),
 * we exploit this observation:
 *
 * Check if any element repeats within:
 *   - distance 1  → nums[i] == nums[i+1]
 *   - distance 2  → nums[i] == nums[i+2]
 *   - distance 3  → nums[i] == nums[i+3]
 *
 * If yes → that's the answer.
 *
 * ------------------------------------------------------------
 * ⏱ COMPLEXITY:
 * ------------------------------------------------------------
 * Time  : O(n)   → 3 passes over array
 * Space : O(1)   → No extra space used
 *
 * ------------------------------------------------------------
 * 🎯 WHY THIS IS GREAT FOR INTERVIEWS:
 * ------------------------------------------------------------
 * - Uses problem constraints cleverly
 * - Avoids unnecessary data structures
 * - Shows strong pattern recognition
 *
 * ------------------------------------------------------------
 */

class Solution {

    public int repeatedNTimes(int[] nums) {

        int n = nums.length;

        // We only need to check gaps up to 3
        // because the repeated element cannot be spaced further apart
        for (int gap = 1; gap <= 3; gap++) {

            // Traverse the array while maintaining valid bounds
            for (int i = 0; i + gap < n; i++) {

                /*
                 * Core Check:
                 * ----------
                 * Compare current element with element 'gap' distance away
                 *
                 * If they are equal → we found the repeated element
                 */
                if (nums[i] == nums[i + gap]) {
                    return nums[i];
                }
            }
        }

        // According to constraints, we should always find the answer
        return -1;
    }
}

