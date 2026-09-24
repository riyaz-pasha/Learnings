import java.util.*;

/**
 * =========================================================================================
 * 🔥 HOUSE ROBBER IV — MINIMUM CAPABILITY (BINARY SEARCH ON ANSWER)
 * =========================================================================================
 *
 * 🧠 PROBLEM REFRAME:
 * -----------------------------------------------------------------------------------------
 * We need to rob at least k NON-ADJACENT houses.
 *
 * BUT instead of maximizing money (classic robber problem),
 * we are minimizing the "maximum value" among chosen houses.
 *
 * 👉 Capability = max(nums[i]) among selected houses
 *
 * GOAL:
 * Find MINIMUM capability such that we can pick >= k non-adjacent houses
 *
 *
 * =========================================================================================
 * 🧠 KEY INTUITION (VERY IMPORTANT FOR INTERVIEWS)
 * =========================================================================================
 *
 * ❌ Brute Force Thinking:
 * - Try all subsets of non-adjacent houses → exponential → impossible
 *
 * 💡 Flip the thinking:
 * Instead of choosing houses → FIX a capability
 *
 * 👉 Ask:
 *    "If my capability = X, can I rob at least k houses?"
 *
 *
 * =========================================================================================
 * 🔥 MONOTONIC PROPERTY (WHY BINARY SEARCH WORKS)
 * =========================================================================================
 *
 * Define:
 *    canRob(X) = true if we can pick >= k houses where nums[i] <= X
 *
 * Observation:
 *
 *    If canRob(X) == true
 *    then canRob(X + something bigger) == ALSO true
 *
 * Because:
 *    Bigger capability allows MORE houses (loosens restriction)
 *
 * So we get:
 *
 *    false false false ... true true true
 *                      ↑
 *                first valid X (ANSWER)
 *
 * 👉 Classic "FIRST TRUE" pattern
 *
 *
 * =========================================================================================
 * 🔍 HOW TO CHECK canRob(X) (GREEDY)
 * =========================================================================================
 *
 * We want:
 * - Pick houses with value <= X
 * - Cannot pick adjacent houses
 * - Maximize count
 *
 * GREEDY:
 * Always pick earliest valid house → optimal
 *
 * WHY GREEDY WORKS?
 * - Picking early avoids blocking future choices
 * - This is exactly same as max non-adjacent selection
 *
 *
 * =========================================================================================
 * ⏱️ COMPLEXITY
 * =========================================================================================
 *
 * Binary Search: O(log(max(nums)))
 * Each check: O(n)
 *
 * Total: O(n log max(nums))
 *
 * Space: O(1)
 *
 *
 * =========================================================================================
 * 🎯 EASY WAY TO THINK (INTERVIEW STRATEGY)
 * =========================================================================================
 *
 * 1. "Minimize maximum" → think Binary Search on Answer
 *
 * 2. Convert problem:
 *    Instead of optimizing → CHECK feasibility
 *
 * 3. Ask:
 *    "If max allowed = X, can I form k houses?"
 *
 * 4. Build greedy validator
 *
 *
 * =========================================================================================
 */
class HouseRobberIV_MinCapability {

    public static void main(String[] args) {
        int[] nums = {2, 3, 5, 9};
        int k = 2;

        System.out.println(minCapability(nums, k)); // Expected: 5
    }

    /**
     * 🔥 MAIN FUNCTION
     */
    public static int minCapability(int[] nums, int k) {

        int low = Arrays.stream(nums).min().getAsInt();
        int high = Arrays.stream(nums).max().getAsInt();

        int answer = high; // explicit answer tracking (your preferred style)

        while (low <= high) {

            int mid = low + (high - low) / 2;

            if (canRob(nums, k, mid)) {
                answer = mid;     // possible answer
                high = mid - 1;   // try smaller (minimize)
            } else {
                low = mid + 1;    // need bigger capability
            }
        }

        return answer;
    }

    /**
     * =====================================================================================
     * 🧪 GREEDY CHECK FUNCTION
     * =====================================================================================
     *
     * Check if we can pick >= k non-adjacent houses with value <= maxAllowed
     *
     * Strategy:
     * - Iterate
     * - If nums[i] <= maxAllowed → pick it
     * - Skip next index (i++) to avoid adjacency
     *
     * =====================================================================================
     */
    private static boolean canRob(int[] nums, int k, int maxAllowed) {

        int count = 0;

        for (int i = 0; i < nums.length; i++) {

            if (nums[i] <= maxAllowed) {
                count++;
                i++; // skip next (adjacent)
            }

            if (count >= k) return true; // early exit
        }

        return false;
    }
}

/**
 * Problem Statement:
 * You are given an array `nums` representing money in consecutive houses, and an integer `k` 
 * representing the minimum number of houses you must rob. You cannot rob adjacent houses.
 * Your "capability" is the MAXIMUM amount of money stolen from any single house.
 * Return the MINIMUM possible capability needed to rob at least `k` non-adjacent houses.
 * 
 * Constraints:
 * - 1 <= nums.length <= 10^5
 * - 1 <= nums[i] <= 10^9
 * - 1 <= k <= (nums.length + 1) / 2
 */
class HouseRobberIV {

    /**
     * Helper Method: Greedily checks if it is possible to rob at least `k` non-adjacent 
     * houses without robbing any house that has more money than `capability`.
     */
    private static boolean canRob(int[] nums, int k, int capability) {
        int count = 0;
        
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] <= capability) {
                count++;
                i++; // Skip the next house to satisfy the "non-adjacent" rule
                
                if (count >= k) {
                    return true; // We've successfully robbed at least k houses
                }
            }
        }
        
        return false;
    }

    /**
     * SOLUTION 1: Iterative Binary Search on the Answer Space (Optimal)
     * 
     * Time Complexity: O(N * log(Max(nums) - Min(nums)))
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * We don't binary search indices; we binary search the ACTUAL CAPABILITY.
     * The lowest possible capability is the minimum element in `nums`.
     * The highest possible capability needed is the maximum element in `nums`.
     * 
     * nums = [2, 3, 5, 9], k = 2
     * Search Space: [2, 3, 4, 5, 6, 7, 8, 9]
     * 
     * Iteration 1:
     * L = 2, H = 9. mid (capability) = 5.
     * Can we rob 2 houses with capability <= 5?
     * We can rob nums[0]=2, skip nums[1], rob nums[2]=5. Count = 2.
     * Yes! Save result = 5. Try to find a smaller valid capability. H = 4.
     * 
     * Iteration 2:
     * L = 2, H = 4. mid (capability) = 3.
     * Can we rob 2 houses with capability <= 3?
     * We can rob nums[0]=2, skip nums[1], but nums[2]=5 and nums[3]=9 are > 3. 
     * Count = 1. No! We need a higher capability. L = 4.
     * 
     * Iteration 3:
     * L = 4, H = 4. mid (capability) = 4.
     * Can we rob 2 houses with capability <= 4?
     * Count = 1 (only nums[0]=2). No. L = 5.
     * 
     * Loop Ends. Result is 5.
     */
    public static int minCapabilityIterative(int[] nums, int k) {
        // Use Java Streams to elegantly find the boundaries
        int low = Arrays.stream(nums).min().getAsInt();
        int high = Arrays.stream(nums).max().getAsInt();
        
        int result = high; // Explicit result variable initialized to max valid capability

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (canRob(nums, k, mid)) {
                // We can successfully rob k houses with this capability.
                // Save it, and try to find a strictly smaller valid capability.
                result = mid;
                high = mid - 1;
            } else {
                // Capability is too low, we can't rob k houses. We must increase it.
                low = mid + 1;
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search on Answer Space
     * 
     * Time Complexity: O(N * log(Max - Min))
     * Space Complexity: O(log(Max - Min)) - Call stack overhead.
     * 
     * EXPLANATION:
     * Translates the optimal iterative binary search into a functional recursive model.
     * Uses `currentResult` to explicitly track and propagate the lowest valid capability found.
     */
    public static int minCapabilityRecursiveWrapper(int[] nums, int k) {
        int low = Arrays.stream(nums).min().getAsInt();
        int high = Arrays.stream(nums).max().getAsInt();
        return minCapabilityRecursive(nums, k, low, high, high);
    }

    private static int minCapabilityRecursive(int[] nums, int k, int low, int high, int currentResult) {
        int result = currentResult; // Explicitly track result

        if (low > high) {
            return result; // Base case: binary search range exhausted
        }

        int mid = low + (high - low) / 2;

        if (canRob(nums, k, mid)) {
            // Valid capability, record it and try for a smaller one
            result = minCapabilityRecursive(nums, k, low, mid - 1, mid);
        } else {
            // Invalid capability, must increase it
            result = minCapabilityRecursive(nums, k, mid + 1, high, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: 1D Space-Optimized Dynamic Programming
     * 
     * Time Complexity: O(N * K) 
     * Space Complexity: O(K)
     * 
     * EXPLANATION:
     * Let dp[j] be the minimum capability required to rob `j` houses.
     * To rob `j` houses from the first `i` houses, we can either:
     * 1. Skip the `i-th` house: capability = prev1[j]
     * 2. Rob the `i-th` house: capability = max(prev2[j-1], nums[i-1])
     * dp[j] = min(skip, rob)
     * 
     * Note: This will result in "Time Limit Exceeded" on platforms for large N and K 
     * (e.g., N=10^5, K=5*10^4 -> 5*10^9 operations), but proves correctness mathematically.
     */
    public static int minCapabilityDP(int[] nums, int k) {
        int n = nums.length;
        
        // Arrays to represent dp[i-2], dp[i-1], and dp[i]
        int[] prev2 = new int[k + 1];
        int[] prev1 = new int[k + 1];
        int[] curr = new int[k + 1];
        
        Arrays.fill(prev2, Integer.MAX_VALUE);
        Arrays.fill(prev1, Integer.MAX_VALUE);
        prev2[0] = 0;
        prev1[0] = 0;

        // Base case setup for the first house
        if (n >= 1) {
            prev1[1] = nums[0];
        }

        // Iterate through houses
        for (int i = 2; i <= n; i++) {
            curr[0] = 0;
            for (int j = 1; j <= k; j++) {
                int skip = prev1[j];
                int rob = Integer.MAX_VALUE;
                
                if (prev2[j - 1] != Integer.MAX_VALUE) {
                    rob = Math.max(prev2[j - 1], nums[i - 1]);
                }
                
                curr[j] = Math.min(skip, rob);
            }
            
            // Shift DP states for the next iteration
            System.arraycopy(prev1, 0, prev2, 0, k + 1);
            System.arraycopy(curr, 0, prev1, 0, k + 1);
        }

        return n == 1 ? prev1[k] : curr[k];
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to structure the test cases elegantly.
     */
    public record TestCase(int[] nums, int k, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on standard logic and constraints
        TestCase[] testCases = {
            new TestCase(new int[]{2, 3, 5, 9}, 2, 5),          // Standard Example 1
            new TestCase(new int[]{2, 7, 9, 3, 1}, 2, 2),       // Standard Example 2 (Rob 2 and 1)
            new TestCase(new int[]{2, 7, 9, 3, 1}, 3, 9),       // Rob 3 houses, forced to rob 2, 9, 1
            new TestCase(new int[]{10, 20, 30, 40}, 2, 20),     // Growing array
            new TestCase(new int[]{5}, 1, 5)                    // Single element array
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterativeBS = minCapabilityIterative(tc.nums(), tc.k());
            int resRecursiveBS = minCapabilityRecursiveWrapper(tc.nums(), tc.k());
            
            // Limit DP execution on massive arrays for test snappiness, 
            // Since max size here is small, DP will easily run instantly.
            boolean isSmallTest = tc.nums().length <= 1000;
            int resDP = isSmallTest ? minCapabilityDP(tc.nums(), tc.k()) : tc.expected();

            boolean passed = (resIterativeBS == tc.expected()) &&
                             (resRecursiveBS == tc.expected()) &&
                             (resDP == tc.expected());

            // Neat printing logic
            String arrStr = Arrays.toString(tc.nums());
            if (arrStr.length() > 25) arrStr = arrStr.substring(0, 22) + "...]";

            System.out.printf("Test %d | k: %-2d | Nums: %-25s -> Expected: %-2d | Passed: %b%n",
                    i + 1, tc.k(), arrStr, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] IterBS: %d, RecBS: %d, DP: %d%n",
                        resIterativeBS, resRecursiveBS, resDP);
            }
        }
    }
}

/*
 * ================================================================================================
 * SECTION 1: PROBLEM RESTATEMENT
 * ================================================================================================
 *
 * We are given an array `nums` where nums[i] is the amount of money in house i, and an integer k.
 *
 * We must choose a subset of houses such that:
 *   (a) No two chosen houses are adjacent (standard House Robber adjacency rule).
 *   (b) At least k houses are chosen.
 *
 * Define the "capability" of a chosen subset as the MAXIMUM value among the houses in that
 * subset (not the sum -- this is the key twist versus classic House Robber, which maximizes sum).
 *
 * We want to return the MINIMUM possible capability over all valid subsets (subsets that are
 * non-adjacent and have size >= k).
 *
 * Inputs:
 *   - nums: int[], 1 <= nums.length <= 1e5, 1 <= nums[i] <= 1e9
 *   - k: int, 1 <= k <= floor((nums.length + 1) / 2)   (the max possible non-adjacent picks)
 *
 * Output:
 *   - A single int: the minimum achievable capability.
 *
 * Assumptions stated by the problem:
 *   - It is guaranteed a valid selection of >= k non-adjacent houses always exists (so we never
 *     need to handle an infeasible case).
 *
 * This is LeetCode 2560, "House Robber IV". It is fundamentally a "minimize the maximum" search
 * problem, which is a strong signal for binary search on the answer.
 */


/*
 * ================================================================================================
 * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
 * ================================================================================================
 *
 * 1. Q: Can `nums` contain duplicate values?
 *    A: Yes. Duplicates are common and must be handled correctly (they are, since we compare by
 *       value, not by uniqueness).
 *
 * 2. Q: Is "capability" the maximum single house value among robbed houses, and are we minimizing
 *       that maximum (a minimax objective), not minimizing/maximizing the sum?
 *    A: Correct -- this is explicitly a minimax problem, distinct from classic House Robber.
 *
 * 3. Q: Does the robber need to rob EXACTLY k houses, or AT LEAST k?
 *    A: At least k. This matters because "at least k" is monotonic (if you can hit k, robbing
 *       extra eligible houses never hurts), which is what enables binary search on the answer.
 *
 * 4. Q: Are house indices 0-based, and does "adjacent" strictly mean indices differing by 1?
 *    A: Yes to both.
 *
 * 5. Q: What are the bounds on n and nums[i]? Do we need to worry about integer overflow?
 *    A: n <= 1e5, nums[i] <= 1e9. Both fit comfortably in a 32-bit int; no overflow risk since we
 *       never sum values, only compare and take max.
 *
 * 6. Q: Is it guaranteed that a valid answer (>= k non-adjacent houses) always exists?
 *    A: Yes, explicitly guaranteed by the problem statement -- we don't need infeasibility
 *       handling or a sentinel "impossible" return value.
 *
 * 7. Q: Do we need to return WHICH houses are robbed, or just the capability value?
 *    A: Just the integer capability value.
 *
 * 8. Q: Is extra O(n) space acceptable (e.g., for sorting a copy), or must we solve in O(1) extra
 *       space?
 *    A: O(n) extra space is acceptable, but I'll aim for O(1) extra space in the optimal solution
 *       since it's achievable and strictly better.
 */


/*
 * ================================================================================================
 * SECTION 3: EXAMPLES & EDGE CASES
 * ================================================================================================
 *
 * Example 1 (normal case):
 *   nums = [2, 3, 5, 9], k = 2
 *   Try capability = 3: eligible houses (<=3) are indices 0(2), 1(3) -- adjacent, so greedily we
 *     can only take one of them -> count = 1 < 2. Not feasible.
 *   Try capability = 5: eligible houses (<=5) are indices 0(2), 1(3), 2(5). Greedily take index 0,
 *     skip index 1 (adjacent), take index 2 -> count = 2 >= 2. Feasible.
 *   Answer: 5 (rob houses at index 0 and 2, capability = max(2, 5) = 5).
 *
 * Example 2 (boundary / near-maximum k):
 *   nums = [2, 7, 9, 3, 1], k = 2
 *   Try capability = 1: eligible houses (<=1) are index 4(1) only -> count = 1 < 2. Not feasible.
 *   Try capability = 2: eligible houses (<=2) are index 0(2), index 4(1). They are not adjacent
 *     (distance 4) -> count = 2 >= 2. Feasible.
 *   Answer: 2 (rob houses at index 0 and 4).
 *   This case is a nice tie-breaking illustration: the greedy scan must correctly skip indices 1,
 *   2, 3 (values 7, 9, 3 -- all > 2) without incorrectly disqualifying index 4 just because it's
 *   far from index 0.
 *
 * Example 3 (degenerate edge case):
 *   nums = [5], k = 1
 *   Only one house exists, and k=1 forces us to take it.
 *   Answer: 5. This exercises the n=1 boundary and confirms low/high binary-search bounds start
 *   and end correctly on a single-element array.
 */


class HouseRobberIV {

    /*
     * ============================================================================================
     * SECTION 4 & 5: APPROACH 1 -- Brute Force / Naive (Backtracking over all subsets)
     * ============================================================================================
     * Core idea: Explore every way to pick a non-adjacent subset of houses (skip current house,
     * or take it and jump 2 indices ahead). At each complete traversal of the array, if we picked
     * at least k houses, record the max value seen as a candidate answer, and keep the minimum
     * across all valid leaves.
     *
     * Paradigm: Exhaustive recursion / backtracking (no pruning beyond the adjacency rule itself).
     *
     * Time Complexity: O(2^n) roughly (more precisely O(phi^n), Fibonacci-like branching, since
     *   each call either advances by 1 or by 2) -- exponential.
     * Space Complexity: O(n) recursion stack depth.
     *
     * Pros:
     *   - Trivially correct; a great way to confirm understanding of the problem before optimizing.
     *   - No subtle correctness argument needed (no greedy/monotonicity claims to defend).
     * Cons:
     *   - Exponential blow-up; useless beyond n ~ 20-25 in practice.
     * When to use: Only to sanity-check smaller inputs / as an oracle for testing faster solutions.
     * Never acceptable as a final answer given n up to 1e5.
     */
    static int minCapabilityBruteForce(int[] nums, int k) {
        int[] bestCapability = { Integer.MAX_VALUE };
        backtrack(nums, 0, 0, 0, k, bestCapability);
        return bestCapability[0];
    }

    private static void backtrack(int[] nums, int index, int housesTaken, int currentMax,
                                   int k, int[] bestCapability) {
        if (index == nums.length) {
            if (housesTaken >= k) {
                bestCapability[0] = Math.min(bestCapability[0], currentMax);
            }
            return;
        }
        // Branch 1: skip this house entirely.
        backtrack(nums, index + 1, housesTaken, currentMax, k, bestCapability);
        // Branch 2: take this house, and the next eligible index is index + 2 (adjacency rule).
        backtrack(nums, index + 2, housesTaken + 1, Math.max(currentMax, nums[index]), k, bestCapability);
    }


    /*
     * ============================================================================================
     * APPROACH 2 -- Sorting-Based Candidate Enumeration + Linear Feasibility Scan
     * ============================================================================================
     * Core idea: The final answer MUST be one of the values already present in nums (the
     * capability is literally "the value of some house we robbed"). So instead of guessing
     * arbitrary integers, sort the distinct values and test them in increasing order; the first
     * one for which we can greedily pick >= k non-adjacent eligible houses is the answer.
     *
     * Paradigm: Sorting + greedy feasibility check.
     *
     * Time Complexity: O(n log n) to sort distinct values, then up to O(n) candidates each
     *   requiring an O(n) feasibility scan => O(n^2) worst case (e.g., all distinct values).
     * Space Complexity: O(n) for the sorted distinct-value array.
     *
     * Pros:
     *   - Conceptually simple bridge between brute force and full binary search.
     *   - Demonstrates the key insight (answer is one of nums' values) explicitly.
     * Cons:
     *   - O(n^2) worst case is too slow for n = 1e5 (up to ~1e10 operations).
     * When to use: Only as a stepping stone in explanation, or when n is small/moderate
     *   (e.g., n <= ~3000) and code simplicity matters more than squeezing out log factors.
     */
    static int minCapabilitySortedCandidates(int[] nums, int k) {
        int[] sortedUnique = Arrays.stream(nums).distinct().sorted().toArray();
        for (int candidate : sortedUnique) {
            if (countNonAdjacentAtMost(nums, candidate) >= k) {
                return candidate; // first (smallest) feasible candidate wins
            }
        }
        // Unreachable given the problem's guarantee that a valid selection always exists.
        throw new IllegalStateException("No feasible capability found -- violates problem guarantee");
    }


    /*
     * ============================================================================================
     * APPROACH 3 (OPTIMAL) -- Binary Search on the Answer + Greedy Feasibility Check
     * ============================================================================================
     * Core idea: Binary search directly over the VALUE RANGE [min(nums), max(nums)] instead of
     * over indices into a sorted array. For a candidate capability V, define:
     *     feasible(V) = true if we can greedily pick >= k non-adjacent houses whose value <= V.
     * feasible(V) is monotonic in V: if feasible(V) is true, feasible(V+1) is also true (a larger
     * V only makes MORE houses eligible, never fewer) -- this monotonicity is exactly what
     * licenses binary search on the answer.
     *
     * Exchange argument for greedy correctness (feasibility check):
     *   Claim: scanning left-to-right and greedily taking any eligible house that is not adjacent
     *   to the previously taken one maximizes the count of eligible non-adjacent houses.
     *   Proof sketch: Suppose an optimal solution skips the leftmost eligible house H at index i
     *   in favor of taking nothing there. We can always "shift" the optimal solution to take H
     *   instead of (or in addition to, if nothing conflicts) whatever it does at/after index i,
     *   because taking the earliest possible eligible house only relaxes constraints on future
     *   choices (it can never be adjacent to anything before it that matters, and it forecloses
     *   the fewest future indices, i+1 only). This exchange does not decrease the total count, so
     *   there is always an optimal solution that agrees with the greedy choice at the first
     *   decision point. Induction over remaining indices completes the proof.
     *
     * Paradigm: Binary search on the answer + greedy.
     *
     * Time Complexity: O(n * log(max(nums) - min(nums))). Each feasibility check is O(n); the
     *   binary search runs O(log(1e9)) ~= 30 iterations. Total ~= 3 * 10^6 operations for n=1e5.
     * Space Complexity: O(1) extra space (excluding input).
     *
     * Pros:
     *   - Optimal in practice for the given constraints; simple to implement and reason about.
     *   - O(1) extra space, no sorting required.
     * Cons:
     *   - Requires the (slightly non-obvious) insight to binary search over VALUES rather than
     *     indices, and a correctness argument for the greedy feasibility check.
     * When to use: This is the production-quality answer for the stated constraints (n up to 1e5).
     */
    static int minCapabilityBinarySearch(int[] nums, int k) {
        int low = Arrays.stream(nums).min().getAsInt();
        int high = Arrays.stream(nums).max().getAsInt();
        while (low < high) {
            int mid = low + (high - low) / 2; // avoids overflow, though not a real risk here given nums[i] <= 1e9
            if (countNonAdjacentAtMost(nums, mid) >= k) {
                high = mid; // mid is feasible; try to do better (search left half, inclusive of mid)
            } else {
                low = mid + 1; // mid is infeasible; must go strictly higher
            }
        }
        return low; // low == high: smallest feasible capability
    }

    // Shared helper: greedily counts the maximum number of non-adjacent houses with value <= capability.
    private static int countNonAdjacentAtMost(int[] nums, int capability) {
        int count = 0;
        int index = 0;
        while (index < nums.length) {
            if (nums[index] <= capability) {
                count++;
                index += 2; // take this house, so the very next index is off-limits (adjacency)
            } else {
                index++; // this house is too expensive to be eligible; just move on
            }
        }
        return count;
    }


    /*
     * ============================================================================================
     * APPROACH 4 -- Binary Search on the Answer + DP Feasibility Check (alternate paradigm)
     * ============================================================================================
     * Core idea: Same binary search skeleton as Approach 3, but replace the greedy feasibility
     * check with a classic House-Robber-style DP that counts (rather than sums) eligible houses:
     *     dp[i] = max non-adjacent eligible-house count achievable using the first i houses
     *     dp[i] = max( dp[i-1],                         // skip house i-1
     *                  dp[i-2] + (nums[i-1] <= V ? 1:0)  // take house i-1
     *                )
     * This is useful to demonstrate that the feasibility check itself is a DP paradigm at heart
     * (structurally identical to House Robber I), even though the greedy version is simpler and
     * strictly sufficient here.
     *
     * Paradigm: Binary search on the answer + 1D dynamic programming.
     *
     * Time Complexity: O(n * log(max(nums) - min(nums))) -- same asymptotic complexity as
     *   Approach 3, since the DP feasibility check is also O(n) per call.
     * Space Complexity: O(n) for the dp array (can be reduced to O(1) with two rolling variables,
     *   exactly like House Robber I -- omitted here for clarity of exposition).
     *
     * Pros:
     *   - Reuses a very well-known DP template; low cognitive load if you already know House
     *     Robber I cold.
     *   - Useful as a fallback if you're not 100% confident in the greedy exchange argument
     *     under interview pressure.
     * Cons:
     *   - Strictly more code and more memory than the greedy check for no asymptotic benefit.
     * When to use: Mention it as an alternative/fallback, but prefer Approach 3 for the final
     *   implementation since it's simpler and equally optimal.
     */
    static int minCapabilityBinarySearchDP(int[] nums, int k) {
        int low = Arrays.stream(nums).min().getAsInt();
        int high = Arrays.stream(nums).max().getAsInt();
        while (low < high) {
            int mid = low + (high - low) / 2;
            if (maxNonAdjacentCountDP(nums, mid) >= k) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    private static int maxNonAdjacentCountDP(int[] nums, int capability) {
        int n = nums.length;
        int[] dp = new int[n + 1];
        dp[0] = 0;
        dp[1] = (nums[0] <= capability) ? 1 : 0;
        for (int i = 2; i <= n; i++) {
            int skipCurrent = dp[i - 1];
            int takeCurrent = dp[i - 2] + ((nums[i - 1] <= capability) ? 1 : 0);
            dp[i] = Math.max(skipCurrent, takeCurrent);
        }
        return dp[n];
    }


    /*
     * ============================================================================================
     * SECTION 6: PARADIGMS DELIBERATELY SKIPPED (with one-line justification each)
     * ============================================================================================
     * - Two pointer / sliding window: Not applicable -- there is no contiguous subarray property
     *   being tracked; eligibility is a per-element value threshold, not a window invariant.
     * - Divide and conquer: Not a natural fit -- there's no clean way to combine "max capability
     *   subsets" from two halves without re-deriving essentially the same DP/greedy check.
     * - Heap / priority queue: Not needed -- we never need dynamic top-k extraction or repeated
     *   min/max updates; a single linear scan per feasibility check suffices.
     * - Tree / graph traversal: Not applicable -- the input is a flat array with a simple
     *   adjacency-by-index rule, not a graph or tree structure.
     * - Monotonic stack / deque: Not applicable -- there's no "next greater/smaller element" or
     *   window-min/max pattern here.
     * - Trie / segment tree: Not applicable -- no prefix queries, range queries, or string
     *   structure exist in this problem.
     * - Pure hashing: Not central -- Approach 2 uses `distinct()` (hash-set-backed) merely to
     *   de-duplicate candidates, but hashing isn't the core technique that solves the problem.
     */


    /*
     * ============================================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ============================================================================================
     *
     * | Approach                              | Time                  | Space | Best For              | Limitations                          |
     * |----------------------------------------|-----------------------|-------|------------------------|---------------------------------------|
     * | 1. Brute Force (Backtracking)          | O(2^n) / O(phi^n)     | O(n)  | Correctness oracle,   | Exponential; unusable beyond n ~ 25   |
     * |                                         |                       |       | tiny n, unit tests    |                                        |
     * | 2. Sorted Candidates + Linear Scan     | O(n^2) worst case     | O(n)  | Small/moderate n,     | Quadratic; too slow for n = 1e5       |
     * |                                         | (O(n log n) to sort)  |       | teaching the insight  |                                        |
     * | 3. Binary Search + Greedy (OPTIMAL)    | O(n log(max(nums)))   | O(1)  | Production use at     | Needs exchange-argument justification |
     * |                                         |                       |       | given constraints     | for greedy step                       |
     * | 4. Binary Search + DP                  | O(n log(max(nums)))   | O(n)  | Fallback if unsure    | More code/memory than Approach 3 for  |
     * |                                         |                       |       | of greedy proof       | no asymptotic gain                    |
     */


    /*
     * ============================================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ============================================================================================
     * I would present Approach 3 (Binary Search on the Answer + Greedy Feasibility Check).
     *
     * Why:
     *   - Clarity: the feasibility check is a five-line loop; the binary search is the textbook
     *     "find leftmost true in a monotonic boolean function" template most interviewers
     *     recognize instantly.
     *   - Coding speed: it's short enough to write cleanly in under 10 minutes, leaving time for
     *     the dry run and follow-ups.
     *   - Optimality: O(n log(max(nums))) is essentially optimal for this problem -- you cannot
     *     avoid at least one O(n) pass (every house's value is potentially relevant), and the log
     *     factor from binary search over the value range is minimal.
     *   - Interviewer expectations: for a "minimize the maximum" problem with a guaranteed
     *     monotonic feasibility predicate, binary-search-on-the-answer is exactly the pattern a
     *     Google interviewer is listening for. I'd explicitly call out the monotonicity property
     *     and the greedy exchange argument out loud before coding, to preempt "why does greedy
     *     work here?" as a follow-up.
     */


    /*
     * ============================================================================================
     * SECTION 9: DEEP DIVE -- POLISHED, PRODUCTION-QUALITY OPTIMAL SOLUTION
     * ============================================================================================
     */

    /**
     * Returns the minimum possible "capability" (max value among robbed houses) required to rob
     * at least {@code k} non-adjacent houses from {@code nums}.
     *
     * <p>Algorithm: binary search on the answer over the value range [min(nums), max(nums)].
     * For a candidate capability V, a greedy left-to-right scan determines the maximum number of
     * non-adjacent houses with value &le; V that can be robbed; this count is monotonically
     * non-decreasing as V increases, which is exactly the property binary search exploits.
     *
     * @param nums   house values; 1 &le; nums.length &le; 1e5, 1 &le; nums[i] &le; 1e9
     * @param k      minimum number of non-adjacent houses to rob;
     *               1 &le; k &le; floor((nums.length + 1) / 2)
     * @return the minimum achievable capability
     * @throws IllegalArgumentException if nums is null/empty or k is out of the valid range
     */
    public static int minCapability(int[] nums, int k) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("nums must be non-empty");
        }
        int maxPossiblePicks = (nums.length + 1) / 2;
        if (k < 1 || k > maxPossiblePicks) {
            throw new IllegalArgumentException("k must be in [1, floor((n+1)/2)]");
        }

        // Search space is the VALUE range of house money, not an index range -- the answer is
        // always some house's actual value, and feasibility is monotonic in this value.
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int value : nums) {
            low = Math.min(low, value);
            high = Math.max(high, value);
        }

        // Standard "find the smallest V for which feasible(V) is true" binary search template.
        while (low < high) {
            int mid = low + (high - low) / 2; // no overflow risk: values bounded by 1e9
            if (maxNonAdjacentEligibleCount(nums, mid) >= k) {
                // mid works as a capability; a smaller value might also work, so narrow upward bound.
                high = mid;
            } else {
                // mid is too restrictive; we need a strictly larger capability.
                low = mid + 1;
            }
        }
        // Loop invariant: low == high, and this value is the smallest capability satisfying
        // feasible(V) == true -- i.e., the answer.
        return low;
    }

    /**
     * Greedily computes the maximum number of non-adjacent houses that can be robbed using only
     * houses whose value does not exceed {@code capability}.
     *
     * <p>Greedy correctness: taking the earliest eligible house never disqualifies more future
     * options than any alternative choice would (exchange argument), so a single left-to-right
     * pass is optimal for maximizing count under the non-adjacency constraint.
     */
    private static int maxNonAdjacentEligibleCount(int[] nums, int capability) {
        int robbedCount = 0;
        int currentIndex = 0;
        while (currentIndex < nums.length) {
            if (nums[currentIndex] <= capability) {
                robbedCount++;
                currentIndex += 2; // this house is robbed, so the immediate neighbor is skipped
            } else {
                currentIndex += 1; // this house is too valuable to count toward this capability
            }
        }
        return robbedCount;
    }


    /*
     * ============================================================================================
     * SECTION 10: DRY RUN / TRACE
     * ============================================================================================
     * Using Example 2: nums = [2, 7, 9, 3, 1], k = 2
     *
     * Step 0 (setup):
     *   low  = min(nums) = 1
     *   high = max(nums) = 9
     *
     * --- Binary search iteration 1 ---
     *   mid = 1 + (9 - 1) / 2 = 5
     *   maxNonAdjacentEligibleCount(nums, 5):
     *     index=0, nums[0]=2 <=5 -> take, robbedCount=1, index=2
     *     index=2, nums[2]=9 >5  -> skip, index=3
     *     index=3, nums[3]=3 <=5 -> take, robbedCount=2, index=5 (loop ends, index==n)
     *     result: 2
     *   2 >= k(2) -> feasible. high = mid = 5.
     *   State: low=1, high=5
     *
     * --- Binary search iteration 2 ---
     *   mid = 1 + (5 - 1) / 2 = 3
     *   maxNonAdjacentEligibleCount(nums, 3):
     *     index=0, nums[0]=2 <=3 -> take, robbedCount=1, index=2
     *     index=2, nums[2]=9 >3  -> skip, index=3
     *     index=3, nums[3]=3 <=3 -> take, robbedCount=2, index=5 (loop ends)
     *     result: 2
     *   2 >= k(2) -> feasible. high = mid = 3.
     *   State: low=1, high=3
     *
     * --- Binary search iteration 3 ---
     *   mid = 1 + (3 - 1) / 2 = 2
     *   maxNonAdjacentEligibleCount(nums, 2):
     *     index=0, nums[0]=2 <=2 -> take, robbedCount=1, index=2
     *     index=2, nums[2]=9 >2  -> skip, index=3
     *     index=3, nums[3]=3 >2  -> skip, index=4
     *     index=4, nums[4]=1 <=2 -> take, robbedCount=2, index=6 (loop ends)
     *     result: 2
     *   2 >= k(2) -> feasible. high = mid = 2.
     *   State: low=1, high=2
     *
     * --- Binary search iteration 4 ---
     *   mid = 1 + (2 - 1) / 2 = 1
     *   maxNonAdjacentEligibleCount(nums, 1):
     *     index=0, nums[0]=2 >1  -> skip, index=1
     *     index=1, nums[1]=7 >1  -> skip, index=2
     *     index=2, nums[2]=9 >1  -> skip, index=3
     *     index=3, nums[3]=3 >1  -> skip, index=4
     *     index=4, nums[4]=1 <=1 -> take, robbedCount=1, index=6 (loop ends)
     *     result: 1
     *   1 >= k(2)? No -> infeasible. low = mid + 1 = 2.
     *   State: low=2, high=2 -> loop terminates (low == high)
     *
     * Final answer: 2. Matches the expected result (rob houses at index 0 and 4, values 2 and 1,
     * capability = max(2,1) = 2).
     */


    /*
     * ============================================================================================
     * SECTION 11: CLOSING SUMMARY
     * ============================================================================================
     * - Brute force (backtracking) establishes correctness but is exponential -- fine only as an
     *   oracle for fuzz-testing the faster approaches.
     * - Sorted-candidate linear scan is a useful conceptual stepping stone (answer is always one
     *   of nums' values) but is O(n^2) worst case, too slow at n = 1e5.
     * - Binary search + greedy (recommended) achieves O(n log(max(nums))) time and O(1) extra
     *   space by exploiting two facts: (1) feasibility is monotonic in the candidate capability,
     *   and (2) a single greedy left-to-right scan optimally maximizes the count of eligible
     *   non-adjacent houses for a fixed capability.
     * - Binary search + DP is asymptotically identical to the greedy version and is a fine
     *   fallback if the greedy exchange argument feels shaky under pressure, at the cost of O(n)
     *   extra space and more code.
     * - Known limitation / assumption of the final solution: relies on the problem's guarantee
     *   that a valid selection of >= k non-adjacent houses always exists; if that guarantee were
     *   removed, we would need an explicit infeasibility check (e.g., verify feasibility at
     *   V = max(nums) before searching, and return a sentinel like -1 if even that fails).
     */


    /*
     * ============================================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ============================================================================================
     * 1. "Can you also return the actual indices/houses robbed, not just the capability value?"
     *    (Would require re-running the greedy scan once more at the final answer's capability and
     *    recording indices as they're taken.)
     * 2. "What if the adjacency constraint changed to 'no two robbed houses within distance d'
     *    for a given d, instead of strictly adjacent (d=1)?"
     *    (The greedy check's `index += 2` becomes `index += d + 1`; binary search skeleton is
     *    unchanged.)
     * 3. "What if nums[i] could be negative (e.g., representing debts as well as cash)?"
     *    (Monotonicity of feasibility in V still holds since we're still just comparing values to
     *    a threshold; low/high bounds just shift to include negative values -- algorithm is
     *    otherwise unaffected.)
     * 4. "How would you handle this if nums were streamed / updated dynamically (online queries)?"
     *    (Would likely need a different data structure, e.g., a balanced BST/segment tree over
     *    positions to support efficient re-querying after point updates, rather than a fresh O(n)
     *    scan per query.)
     * 5. "Could you parallelize the feasibility check for very large n?"
     *    (Yes -- the greedy/DP scan can be chunked with boundary handshaking between chunks, or
     *    the DP version can use a parallel prefix-scan formulation since it has an associative
     *    combine step, similar to parallelizing House Robber I.)
     * 6. "What's the largest n and value range you'd trust this approach for before reconsidering
     *    the algorithm?"
     *    (n in the tens of millions and values up to ~2^31 would still comfortably work within
     *    typical time limits; only truly extreme scales would call for a different approach.)
     */


    /*
     * ============================================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ============================================================================================
     * 1. Binary searching over the wrong domain: candidates often try to binary search over
     *    INDICES into a sorted copy of nums instead of directly over the VALUE range. Both can be
     *    made to work, but conflating them (e.g., using sorted-array indices as if they were
     *    house values) is a common source of bugs.
     * 2. Off-by-one in the greedy skip step: using `index += 1` after taking a house (instead of
     *    `index += 2`) silently allows adjacent houses to both be counted, inflating the feasible
     *    count and causing the algorithm to return a capability that's too low. This is the
     *    canonical silent-failure bug here -- it passes on inputs where the "extra" adjacent pick
     *    doesn't happen to matter, and fails only on inputs where it does.
     * 3. Binary search boundary condition mistakes: using `low <= high` with `high = mid - 1` /
     *    `low = mid + 1` (the "search for exact value" template) instead of the correct
     *    "leftmost true" template (`low < high`, `high = mid`, `low = mid + 1`) can produce an
     *    infinite loop or return a non-minimal feasible value.
     * 4. Conflating "at least k" with "exactly k": some candidates try to enforce exact-count
     *    logic in the feasibility check (e.g., stopping as soon as count == k). This breaks
     *    nothing here since "at least k" is what's required, but candidates who assume "exactly
     *    k" often add unnecessary and sometimes incorrect complexity (e.g., trying to avoid
     *    "overshooting" k, which is never actually a problem since taking more eligible houses
     *    never increases the capability beyond what's already counted).
     */


    /*
     * ============================================================================================
     * TEST HARNESS / MAIN -- cross-validates all four approaches against each other and the
     * expected results from the worked examples.
     * ============================================================================================
     */
    public static void main(String[] args) {
        record TestCase(int[] nums, int k, int expected) {}

        List<TestCase> testCases = List.of(
                new TestCase(new int[]{2, 3, 5, 9}, 2, 5),
                new TestCase(new int[]{2, 7, 9, 3, 1}, 2, 2),
                new TestCase(new int[]{5}, 1, 5),
                new TestCase(new int[]{1, 1, 1, 1, 1, 1, 1}, 4, 1),
                new TestCase(new int[]{4, 4, 4, 4}, 2, 4)
        );

        for (TestCase testCase : testCases) {
            int resultOptimal = minCapability(testCase.nums(), testCase.k());
            int resultGreedyBinSearch = minCapabilityBinarySearch(testCase.nums(), testCase.k());
            int resultDpBinSearch = minCapabilityBinarySearchDP(testCase.nums(), testCase.k());
            int resultSortedScan = minCapabilitySortedCandidates(testCase.nums(), testCase.k());
            int resultBruteForce = minCapabilityBruteForce(testCase.nums(), testCase.k());

            boolean allAgree = resultOptimal == testCase.expected()
                    && resultOptimal == resultGreedyBinSearch
                    && resultOptimal == resultDpBinSearch
                    && resultOptimal == resultSortedScan
                    && resultOptimal == resultBruteForce;

            System.out.printf(
                    "nums=%s, k=%d -> expected=%d, optimal=%d, greedyBS=%d, dpBS=%d, sortedScan=%d, bruteForce=%d [%s]%n",
                    Arrays.toString(testCase.nums()), testCase.k(), testCase.expected(),
                    resultOptimal, resultGreedyBinSearch, resultDpBinSearch, resultSortedScan, resultBruteForce,
                    allAgree ? "PASS" : "FAIL"
            );
        }
    }
}
