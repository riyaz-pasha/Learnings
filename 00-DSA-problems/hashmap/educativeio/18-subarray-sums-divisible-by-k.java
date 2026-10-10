/**
 * ============================================================================
 * L E E T C O D E   9 7 4 :   S U B A R R A Y   S U M S   D I V I S I B L E   B Y   K
 * ============================================================================
 *
 * ## 1. Clarifying questions
 * 1. "Can the array contain negative numbers?" 
 *    (Crucial! In Java and C++, negative numbers yield negative modulos, e.g., -2 % 5 = -2, not 3. This changes how we compute the remainder).
 * 2. "Can 'k' be zero?"
 *    (Constraints state k >= 2, which gracefully avoids any DivisionByZero exceptions).
 * 3. "Is a single element considered a valid subarray if it is divisible by k?"
 *    (Yes, the prompt specifies 'non-empty contiguous subarrays', so length 1 is completely valid).
 * 4. "Can the total number of valid subarrays exceed the 32-bit integer limit?"
 *    (Max length is 30,000. Total possible subarrays is N*(N+1)/2 ≈ 4.5 * 10^8. This easily fits inside a standard 32-bit signed `int` (up to ~2.1 billion), so returning an `int` is safe).
 *
 *
 * ## 2. The reasoning journey
 * > Core Constraint: We need to find contiguous blocks of numbers whose sum is perfectly divisible by `k`.
 * > The bottleneck is that checking the sum of every possible block takes O(N^2) time, which will time out for an array of 30,000 elements.
 *
 * ### Approach 1: The Sliding Window of Sums (Brute Force)
 * 1. What I'd naturally try: For every possible starting index `i`, loop through every ending index `j`. 
 *    Keep a running sum, and if `sum % k == 0`, increment a counter.
 * 2. Why it works: It exhaustively tests every possible contiguous subarray manually.
 * 3. Why it's too costly:
 *    - Time Complexity: O(N^2) — because for each of the N starting positions, we iterate up to N times. 
 *      For N=30,000, this is ~450 million operations. Likely to TLE.
 *    - Space Complexity: O(1) auxiliary — we only need integer counters.
 * 4. What work is being repeated: We continuously recalculate segments that we've already summed up. 
 *    If we know the sum from index 0 to 5, we shouldn't have to rebuild it from scratch.
 * 5. What property removes the bottleneck: Prefix Sums. A subarray sum from `i` to `j` is 
 *    `PrefixSum[j] - PrefixSum[i-1]`.
 *
 * ### Approach 2: Prefix Modulo + HashMap (The Algorithmic Pivot)
 * 1. What I'd naturally try: We need `(Prefix[j] - Prefix[i-1]) % k == 0`. 
 *    Algebraically, this means `Prefix[j] % k == Prefix[i-1] % k`. 
 *    This is huge! It means instead of looking back at all past sums, we just need to know *how many times* 
 *    we've seen our current modulo in the past. I'll use a `HashMap<Integer, Integer>` to track these modulo frequencies.
 * 2. Why it works: It transforms a range-query problem into an instant O(1) "have I seen this state before?" lookup.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — because we traverse the array exactly once, updating the map.
 *    - Space Complexity: O(K) — because there are exactly `k` possible remainders (from 0 to k-1). 
 *      The map will hold at most K entries.
 * 4. What work is being repeated: Using a HashMap involves auto-boxing primitives to objects, computing hash 
 *    codes, and dealing with heap memory. 
 * 5. What property removes the bottleneck: The domain of possible modulos is strictly bounded between `0` and `k-1`. 
 *    We don't need a heavy HashMap; we can use a raw integer array!
 *
 * ### Approach 3: Frequency Array (The Hardware-Friendly Optimum)
 * 1. What I'd naturally try: Replace the HashMap with an `int[] modCounts = new int[k]`. 
 *    Increment `modCounts[remainder]` as we traverse the array.
 * 2. Why it works: Array indices directly represent the remainders. This executes the exact same logic as 
 *    Approach 2 but on bare metal, bypassing garbage collection and hashing overhead entirely.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — Single pass, direct memory array access.
 *    - Space Complexity: O(K) auxiliary — We allocate exactly one array of size `k` (at most 10,000 ints, ~40KB).
 *
 * > **Interview Strategy:** Start by writing out the algebraic realization: `(Sum_j - Sum_i) % k == 0` means 
 * > `Sum_j % k == Sum_i % k`. This proves you didn't just memorize the code, but understand the math. 
 * > Explicitly mention Java's negative modulo quirk. Then confidently write Approach 3.
 *
 *
 * ## 3. Edge cases
 * - Subarray starts exactly at index 0: Crucial! If `nums = [5]` and `k = 5`, the running sum modulo is 0. 
 *   If we don't have `0` pre-loaded in our frequency array, we miss this valid subarray! 
 *   We MUST initialize our structure with `{0: 1}` (meaning we've seen a remainder of 0 one time before the array started).
 * - Negative numbers causing negative remainder: `nums = [-2]`, `k = 5`. `-2 % 5` in Java is `-2`. 
 *   We need to map this to the positive equivalent `3` so it aligns properly in our array index.
 * - Array of all zeros: `[0, 0, 0]`. The running remainder stays 0. 
 *   Index 0 adds 1 (base). Index 1 adds 2. Index 2 adds 3. Total = 6. Correct!
 *
 *
 * ## 4. Key Insight & Dry Run
 *
 * **Key Insight: "The Modulo Clock"**
 * Modulo arithmetic acts like a clock. If you start at 12 o'clock, move forward 17 hours, you are at 5 o'clock. 
 * If you move forward again and land on 5 o'clock, the time elapsed between those two moments MUST have been 
 * exactly a multiple of 12 hours (a full rotation). 
 * Every time you see a "clock hand" position you've seen before, every previous occurrence represents a distinct, 
 * valid subarray ending right now.
 *
 * **Pitfall: Java's Negative Modulo Trap**
 * In Python, `-2 % 5` is `3`. In Java/C++, `-2 % 5` is `-2`. 
 * Using `-2` as an array index throws an `ArrayIndexOutOfBoundsException`.
 * **The fix:** `int rem = ((sum % k) + k) % k;`. 
 * If sum is -2: `(-2 % 5) -> -2`. `(-2 + 5) -> 3`. `(3 % 5) -> 3`.
 * If sum is 7: `(7 % 5) -> 2`. `(2 + 5) -> 7`. `(7 % 5) -> 2`. It flawlessly handles both!
 *
 * **Dry Run of Approach 3 (Frequency Array):**
 * `nums = [4, 5, 0, -2, -3, 1]`, `k = 5`
 * Init: `modCounts[0] = 1` (Base case), `count = 0`, `sum = 0`
 *
 * | idx | val | sum | sum % 5 | Fix Negatives | Seen before? | Add to Count | Update Array |
 * |-----|-----|-----|---------|---------------|--------------|--------------|--------------|
 * |  0  |  4  |  4  |    4    |       4       | modCounts[4]=0 | count += 0 | mod[4] = 1   |
 * |  1  |  5  |  9  |    4    |       4       | modCounts[4]=1 | count += 1 | mod[4] = 2   |
 * |  2  |  0  |  9  |    4    |       4       | modCounts[4]=2 | count += 2 | mod[4] = 3   |
 * |  3  | -2  |  7  |    2    |       2       | modCounts[2]=0 | count += 0 | mod[2] = 1   |
 * |  4  | -3  |  4  |    4    |       4       | modCounts[4]=3 | count += 3 | mod[4] = 4   |
 * |  5  |  1  |  5  |    0    |       0       | modCounts[0]=1 | count += 1 | mod[0] = 2   |
 * 
 * Result: count = 1 + 2 + 3 + 1 = 7.
 *
 *
 * ## 5. Follow-ups
 * - **Q:** What if we wanted the *longest* subarray whose sum is divisible by k?
 *   **A:** We would use an array to store the *first index* where we saw a remainder, rather than the frequency. 
 *   If we see a remainder again, we don't update the array; instead we calculate `distance = currentIndex - storedIndex`.
 * - **Q:** What if we need to return the total number of subarrays whose sum *equals exactly* k (LeetCode 560)?
 *   **A:** We can't use modulo arithmetic or an array anymore because sums are unbounded. We must track exact 
 *   prefix sums using a `HashMap<Integer, Integer>` and look for `runningSum - k`.
 * - **Q:** Could this logic be adapted for streaming data where the array size is infinite?
 *   **A:** Yes! The state we carry forward is strictly O(K) space (our modulo array) and O(1) running variables. 
 *   It processes an infinite stream flawlessly without memory leaks.
 *
 * ============================================================================
 */

import java.util.HashMap;
import java.util.Map;

public class SubarraysDivByKStudy {

    interface SubarrayDivByKSolver {
        int subarraysDivByK(int[] nums, int k);
    }

    // ========================================================================
    // APPROACH 3: FREQUENCY ARRAY (The Optimal Hardware-Friendly Solution)
    // ========================================================================
    static class OptimalArraySolver implements SubarrayDivByKSolver {
        @Override
        public int subarraysDivByK(int[] nums, int k) {
            // Array indices map directly to remainder values 0 through k-1.
            // Concrete example: if k=5, modCounts[3] tracks how many times we've seen a remainder of 3.
            int[] modCounts = new int[k];
            
            // Crucial Base Case: Represents the 'empty prefix' before the array starts.
            // If the running sum itself is perfectly divisible by k, its remainder is 0.
            // We need to match this with a prior '0' remainder to chop off the empty prefix.
            modCounts[0] = 1;
            
            int count = 0;
            int runningSum = 0;
            
            for (int num : nums) {
                runningSum += num;
                
                // Fixes Java's negative modulo behavior natively.
                // E.g., if runningSum = -2 and k = 5:
                // -2 % 5 = -2.
                // -2 + 5 = 3.
                // 3 % 5 = 3. (Correct positive remainder mapped safely for our array index).
                int remainder = ((runningSum % k) + k) % k;
                
                // If we've seen this remainder before, every past occurrence defines
                // a valid starting point for a subarray ending at the current element.
                count += modCounts[remainder];
                
                // Record that we have seen this remainder one more time for future elements.
                modCounts[remainder]++;
            }
            
            return count;
        }
    }

    // ========================================================================
    // APPROACH 2: PREFIX MODULO + HASHMAP (The Conceptual Bridge)
    // ========================================================================
    static class HashMapSolver implements SubarrayDivByKSolver {
        @Override
        public int subarraysDivByK(int[] nums, int k) {
            // Functionally identical to Approach 3, but relies on object overhead
            Map<Integer, Integer> modCounts = new HashMap<>();
            modCounts.put(0, 1);
            
            int count = 0;
            int runningSum = 0;
            
            for (int num : nums) {
                runningSum += num;
                int remainder = ((runningSum % k) + k) % k;
                
                if (modCounts.containsKey(remainder)) {
                    count += modCounts.get(remainder);
                }
                
                modCounts.put(remainder, modCounts.getOrDefault(remainder, 0) + 1);
            }
            
            return count;
        }
    }

    // ========================================================================
    // APPROACH 1: BRUTE FORCE (Conceptual Baseline)
    // ========================================================================
    static class BruteForceSolver implements SubarrayDivByKSolver {
        @Override
        public int subarraysDivByK(int[] nums, int k) {
            int count = 0;
            
            // Outer loop sets the starting window edge
            for (int i = 0; i < nums.length; i++) {
                int sum = 0;
                // Inner loop expands the window rightwards
                for (int j = i; j < nums.length; j++) {
                    sum += nums[j];
                    if (sum % k == 0) {
                        count++;
                    }
                }
            }
            
            return count;
        }
    }


    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        SubarrayDivByKSolver[] solvers = {
            new OptimalArraySolver(),
            new HashMapSolver(),
            new BruteForceSolver()
        };

        // Format: {nums array}, k, expected count
        int[][][] testCases = {
            {{4, 5, 0, -2, -3, 1}, {5}, {7}},       // Standard mixed case with negatives
            {{5}, {9}, {0}},                        // Single element, false
            {{5}, {5}, {1}},                        // Single element, true
            {{-2, 2, -2, 2}, {2}, {10}},            // Negative zeroes (All combinations valid)
            {{0, 0, 0}, {3}, {6}},                  // All zeros
            {{2, -2, 2, -4}, {6}, {2}}              // Requires proper modulo sign handling
        };

        for (SubarrayDivByKSolver solver : solvers) {
            System.out.println("Testing " + solver.getClass().getSimpleName() + "...");
            boolean allPassed = true;
            
            for (int i = 0; i < testCases.length; i++) {
                int[] nums = testCases[i][0];
                int k = testCases[i][1][0];
                int expected = testCases[i][2][0];
                
                int result = solver.subarraysDivByK(nums, k);
                
                if (result != expected) {
                    System.out.printf("  [FAIL] Test %d. Expected: %d, Got: %d%n", 
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
 * - Core pattern: Prefix Sum Modulo Tracking using a Frequency Array.
 * - Key observation: If `(Prefix_j - Prefix_i) % k == 0`, then `Prefix_j % k == Prefix_i % k`.
 *   Finding segments divisible by K reduces entirely to counting how many times we've 
 *   seen our current modulo state in the past.
 * - What's worth memorizing: The negative modulo fix `((sum % k) + k) % k`. This idiom 
 *   protects against Java and C++ returning negative remainders, guaranteeing a safe, 
 *   positive array index.
 * - Most common trap: Forgetting `modCounts[0] = 1`. If a subarray begins at index 0 
 *   and is divisible by k, its prefix modulo is 0. If 0 isn't already in the "past seen" 
 *   ledger, you will fail to count it.
 * - Mental trigger: "Subarray sums divisible by K? Prefix Modulo Frequency Array."
 * ============================================================================
 */

