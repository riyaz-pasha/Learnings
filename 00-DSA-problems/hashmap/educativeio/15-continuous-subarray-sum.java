```java
/**
 * ============================================================================
 * L E E T C O D E   5 2 3 :   C O N T I N U O U S   S U B A R R A Y   S U M
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Can 'k' be zero or negative?" 
 *    (Constraints say k >= 1, saving us from DivisionByZero exceptions. If k could be 0, we'd only be looking for a subarray sum of exactly 0).
 * 2. "Can the array contain negative numbers?" 
 *    (Constraints say nums[i] >= 0, which means the prefix sum is monotonically increasing. If negatives were allowed, we'd need to handle Java's negative modulo behavior carefully).
 * 3. "Is a subarray of length 1 allowed if its value is a multiple of k?" 
 *    (No, the prompt strictly enforces a length of at least 2. We must track distances between indices).
 * 4. "Does a subarray of [0, 0] count as valid?" 
 *    (Yes, the prompt notes that 0 is always considered a multiple of k, e.g., 0 = 0 * k. [0,0] length is 2, sum is 0, so it's valid).
 * 5. "Do we need to return the subarray itself, or just a boolean?"
 *    (Just a boolean. This means we don't need to reconstruct the path, saving space/complexity).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We need to find a contiguous block of numbers (subarray) whose sum is a multiple of `k`. 
 * > Furthermore, the block must be at least 2 elements long. The bottleneck is the sheer number of possible subarrays.
 * 
 * ### Approach 1: The Sliding Window of Sums (Brute Force)
 * 1. What I'd naturally try: Use two nested loops. The outer loop picks the starting index `i`. The inner loop 
 *    picks the ending index `j` (where `j > i` to enforce length >= 2). I calculate the sum of that window 
 *    and check if `sum % k == 0`.
 * 2. Why it works: It tests every single valid contiguous subarray manually.
 * 3. Why it's too costly:
 *    - Time Complexity: O(N^2) — because for each of the N starting positions, we iterate to the end of the 
 *      array. With N = 10^4, N^2 = 10^8 operations, which might just barely pass or TLE depending on the language.
 *    - Space Complexity: O(1) auxiliary — because we only keep a running sum integer.
 * 4. What work is being repeated: We are repeatedly re-calculating the sum of the same segments. But wait, 
 *    we optimized it to a running sum in the inner loop. Is there a better way to check sums of ranges?
 * 5. What property removes the bottleneck: The mathematical property of Prefix Sums. A subarray sum from `i` to `j` 
 *    is just `PrefixSum[j] - PrefixSum[i-1]`.
 * 
 * ### Approach 2: Prefix Sum Array + Nested Loops (Intermediate)
 * 1. What I'd naturally try: Build a `prefix[]` array where `prefix[x]` is the sum of all elements up to `x`. 
 *    Then check all pairs `(i, j)` to see if `(prefix[j] - prefix[i-1]) % k == 0`.
 * 2. Why it works: It formally defines subarrays as the difference between two running totals.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N^2) — because we still check all pairs of (i, j).
 *    - Space Complexity: O(N) — to store the prefix sum array.
 * 4. What work is being repeated: We are mathematically brute-forcing the modulo check. 
 *    We are looking for: `(Prefix[j] - Prefix[i]) % k == 0`.
 *    Algebraically, this means: `Prefix[j] % k == Prefix[i] % k`.
 *    We don't need to check all past prefix sums—we just need to know if we've *ever seen this modulo before*!
 * 5. What property removes the bottleneck: A HashMap. By storing the modulo of the prefix sum and the index 
 *    where we first saw it, we can look back in time instantly (O(1)).
 * 
 * ### Approach 3: Prefix Modulo + HashMap (The Algorithmic Optimum)
 * 1. What I'd naturally try: Keep a running prefix sum. At each step, take `sum % k`. Check a HashMap to see 
 *    if we've seen this exact modulo before. If we have, check if the distance between the current index and 
 *    the saved index is >= 2. If it is, return true!
 * 2. Why it works: Modulo math tells us that if a running sum's remainder is 5 at index 0, and later becomes 
 *    5 again at index 3, the elements added between index 1 and 3 must have a sum perfectly divisible by `k`.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we iterate through the array exactly once, doing O(1) map operations.
 *    - Space Complexity: O(min(N, k)) — because the HashMap stores at most N distinct modulos (if N < k) 
 *      or exactly k modulos (since remainders are bounded from 0 to k-1).
 * 
 * > **Interview Strategy:** Start by explicitly writing out the algebra: `(A - B) % k == 0 => A % k == B % k`. 
 * > This is the "Aha!" moment of the interview. Then write Approach 3. Mention the dummy value `map.put(0, -1)` 
 * > upfront so you don't fall into the most common pitfall.
 * 
 * 
 * ## 3. Edge cases
 * - Zero as a multiple: `[0, 0]`, `k = 1`. Sum is 0. Modulo is 0. Valid.
 * - Array length is 1: Impossible to form length >= 2. Return false.
 * - Entire prefix from index 0 is the multiple: e.g., `[23, 2, 4]`, `k=29`. The sum at index 2 is 29. 29 % 29 = 0.
 *   If we look up 0 in the map and it's not there, we fail! We MUST initialize the map with `(0, -1)` to handle 
 *   subarrays that start at the very beginning of the array.
 * 
 * 
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight: "Modulo is a Time Machine"**
 * Think of modulo as the hand of a clock. If you keep adding numbers and the clock hand lands on "5 o'clock" at 
 * index 1, and then lands on "5 o'clock" again at index 4, it means the time elapsed between index 1 and 4 was 
 * exactly a full rotation of the clock (a multiple of `k`). 
 * 
 * **Dry Run of Approach 3:**
 * `nums = [23, 2, 4, 6, 7]`, `k = 6`
 * Initial State: Map = `{0: -1}`, sum = 0
 * 
 * | idx | num | sum | sum % k (mod) | Map contains mod? | Valid length? (idx - map.get(mod) >= 2) | Map Update |
 * |-----|-----|-----|---------------|-------------------|-----------------------------------------|------------|
 * |  0  | 23  | 23  | 23 % 6 = 5    | No                | -                                       | put(5, 0)  |
 * |  1  |  2  | 25  | 25 % 6 = 1    | No                | -                                       | put(1, 1)  |
 * |  2  |  4  | 29  | 29 % 6 = 5    | YES! (at idx 0)   | idx(2) - idx(0) = 2. 2 >= 2? YES!       | RETURN TRUE|
 * 
 * 
 * ## 5. Follow-ups
 * - **Q:** What if the array could contain negative numbers?
 *   **A:** In Java (and C/C++), the `%` operator calculates the remainder, which can be negative (e.g., `-5 % 3 = -2`). 
 *   To fix this and keep modulos in the positive `0 to k-1` domain, we use `mod = ((sum % k) + k) % k`.
 * - **Q:** What if we needed to find the *longest* valid subarray instead of just returning true/false?
 *   **A:** We would keep a `maxLength` variable. When we see a modulo again, instead of returning true, we do 
 *   `maxLength = Math.max(maxLength, i - map.get(mod))`. Crucially, we would *never* update the map with the new 
 *   index, because keeping the oldest index ensures the longest possible distance.
 * - **Q:** What if memory is extremely tight and `k` is huge, making the HashMap too large?
 *   **A:** If we cannot afford O(N) space, we must fall back to the O(1) space Brute Force running-sum sliding window 
 *   (Approach 1), trading our linear time for quadratic time.
 * 
 * ============================================================================
 */

import java.util.HashMap;
import java.util.Map;

public class ContinuousSubarraySumStudy {

    interface SubarraySumSolver {
        boolean checkSubarraySum(int[] nums, int k);
    }

    // ========================================================================
    // APPROACH 3: MODULO + HASHMAP (The Optimal Interview Solution)
    // ========================================================================
    static class ModuloHashMapSolver implements SubarraySumSolver {
        @Override
        public boolean checkSubarraySum(int[] nums, int k) {
            // Guard clause: Cannot form a subarray of length 2
            if (nums.length < 2) {
                return false;
            }

            // Map stores <Modulo Value, First Seen Index>
            // Concrete example: if running sum modulo 6 is 5 at index 2, map holds {5: 2}.
            Map<Integer, Integer> modSeen = new HashMap<>();
            
            // THE MOST CRITICAL LINE: Dummy value for mod 0.
            // Why? If the very first valid subarray starts at index 0 (e.g., nums=[23, 2, 4], k=29),
            // at index 2 the sum is 29, and 29 % 29 = 0. 
            // We need 0 to exist in the map at an index BEFORE the array started (-1)
            // so the distance calculation (2 - (-1) = 3) evaluates correctly.
            modSeen.put(0, -1);
            
            int runningSum = 0;
            
            for (int i = 0; i < nums.length; i++) {
                runningSum += nums[i];
                
                // Calculate modulo. 
                // Note: If negatives were allowed, we'd use ((runningSum % k) + k) % k
                int mod = runningSum % k;
                
                // Have we seen this exact "clock position" before?
                if (modSeen.containsKey(mod)) {
                    // We found a matching modulo. Check if the elapsed distance is at least 2.
                    int previousIndex = modSeen.get(mod);
                    if (i - previousIndex >= 2) {
                        return true; // We found our good subarray!
                    }
                    // IMPORTANT: If the distance is < 2, we DO NOT update the map with the new index.
                    // We want to keep the oldest (earliest) index to maximize the chance of hitting 
                    // a distance >= 2 on future iterations.
                } else {
                    // First time seeing this modulo. Record the timestamp (index).
                    modSeen.put(mod, i);
                }
            }
            
            return false;
        }
    }

    // ========================================================================
    // APPROACH 1: BRUTE FORCE (Conceptual Baseline)
    // ========================================================================
    static class BruteForceSolver implements SubarraySumSolver {
        @Override
        public boolean checkSubarraySum(int[] nums, int k) {
            if (nums.length < 2) return false;
            
            // Check every possible starting point
            for (int i = 0; i < nums.length - 1; i++) {
                int sum = nums[i];
                // Check every possible ending point (enforcing length >= 2)
                for (int j = i + 1; j < nums.length; j++) {
                    sum += nums[j];
                    if (sum % k == 0) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        SubarraySumSolver[] solvers = {
            new ModuloHashMapSolver(),
            new BruteForceSolver()
        };

        // Format: {nums array}, k, {expected result: 1 for true, 0 for false}
        int[][][] testCases = {
            {{23, 2, 4, 6, 7}, {6}, {1}},    // Standard true case (2+4 = 6)
            {{23, 2, 6, 4, 7}, {6}, {1}},    // True (23+2+6+4+7 = 42)
            {{23, 2, 6, 4, 7}, {13}, {0}},   // Standard false case
            {{0, 0}, {1}, {1}},              // Zero handling ([0,0] is multiple of anything)
            {{5, 0, 0, 0}, {3}, {1}},        // Zeros padded valid case
            {{23, 2, 4}, {29}, {1}},         // Subarray starts exactly at index 0
            {{1}, {1}, {0}}                  // Fails length >= 2 constraint
        };

        for (SubarraySumSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                int[] nums = testCases[i][0];
                int k = testCases[i][1][0];
                boolean expected = testCases[i][2][0] == 1;
                
                boolean result = solver.checkSubarraySum(nums, k);
                
                if (result != expected) {
                    System.out.printf("  [FAIL] Test %d. Expected: %b, Got: %b%n", 
                        i + 1, expected, result);
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
 * - Core pattern: Prefix Sum + Modulo Arithmetic mapped to a HashMap.
 * - Key observation: If `(Sum_j - Sum_i) % k == 0`, then algebraically `Sum_j % k == Sum_i % k`.
 *   This beautifully reduces an O(N^2) range-sum problem to an O(N) frequency/seen-tracking problem.
 * - What's worth memorizing: The "Subarray starting at index 0" initialization trick: `map.put(0, -1)`.
 *   Without this, any valid subarray starting from the beginning of the array will fail to register.
 * - Most common trap: Updating the map value if the modulo is already present but distance < 2. 
 *   You MUST retain the earliest (oldest) index to maximize your future span.
 * - Mental trigger: "Subarray sums divisible by K? Prefix Modulo tracking."
 * ============================================================================
 */

```
