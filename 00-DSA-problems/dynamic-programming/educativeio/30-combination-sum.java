import java.util.*;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Combination Sum
 * Given an array of distinct integers `nums` and a `target` integer, return a 
 * list of all unique combinations of `nums` where the chosen numbers sum to `target`.
 * 
 * An integer from `nums` may be chosen an UNLIMITED number of times.
 * 
 * Constraints:
 * 1 <= nums.length <= 30
 * 2 <= nums[i] <= 40
 * 1 <= target <= 40
 * All integers in nums are unique.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, this problem bridges the gap between pure Combinatorics 
 * (Backtracking) and Dynamic Programming. 
 * 
 * Q: "Does the order of the numbers in the combination matter?"
 * A: No, [2, 2, 3] and [3, 2, 2] are considered the exact same combination. 
 *    This is a massive hint! It means we must process the numbers in a fixed 
 *    sequence. Once we decide to stop using '2' and move to '3', we can NEVER 
 *    go back and add another '2'. This prevents duplicate permutations.
 * 
 * Q: "Can the array contain zeros or negative numbers?"
 * A: The constraints say 2 <= nums[i]. If 0 was allowed, we could pick it 
 *    infinitely for the same sum (infinite loop). If negatives were allowed, 
 *    we would need a bound on the maximum length of the combination.
 * 
 * CRITICAL SENIOR INSIGHT - BACKTRACKING VS. DP:
 * "Because we must return the ACTUAL paths (not just the count of paths), 
 * standard Top-Down Memoization and Bottom-Up Tabulation are highly inefficient 
 * in terms of memory. We have to store massive Lists of Lists at every cell! 
 * 
 * In industry and interviews, Backtracking (Approach 1) is the standard and 
 * most memory-efficient way to solve 'return all paths' problems. However, 
 * modeling this as an Unbounded Knapsack DP (Approaches 3 & 4) is a brilliant 
 * way to demonstrate mastery of state transitions."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given number `nums[i]`, I have two choices to build my target sum:
 *  1. INCLUDE IT: Because I have an infinite supply, I add `nums[i]` to my 
 *     current path, reduce the target by `nums[i]`, and STAY on the same index 
 *     to potentially pick it again.
 *  2. EXCLUDE IT: I decide I am completely done using `nums[i]`. I move to 
 *     the next index `i + 1` and keep the target the same.
 * 
 * This is structurally identical to the 'Coin Change II' (Unbounded Knapsack) 
 * problem, but instead of adding `counts`, we are appending to `Lists`."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: nums = [2, 3, 6, 7], target = 7
 * 
 * Backtracking Trace (Start at index 0, target 7):
 * - Pick 2: Target = 5, Path = [2]
 *   - Pick 2: Target = 3, Path = [2, 2]
 *     - Pick 2: Target = 1, Path = [2, 2, 2]
 *       - Pick 2: Target = -1 (FAIL, backtrack)
 *       - Skip 2 (move to 3): Target = 1, index for 3. (FAIL)
 *     - Skip 2 (move to 3): Target = 3, Path = [2, 2]
 *       - Pick 3: Target = 0 (SUCCESS! Add [2, 2, 3] to result)
 * - ... Fast forward to skipping 2 entirely ...
 * - Skip 2, Skip 3, Skip 6, Pick 7: Target = 0 (SUCCESS! Add [7])
 */
public class CombinationSum {

    /**
     * ========================================================================
     * APPROACH 1: Backtracking (Recursive Brute Force - Industry Standard)
     * ========================================================================
     * Idea: Recursively explore the "Include" and "Exclude" branches, keeping 
     * track of the current path. When we hit 0, we found a valid combination.
     * 
     * Time Complexity: O(N^(Target/Min_Element)) - Exponential branching.
     * Space Complexity: O(Target/Min_Element) - For the recursion stack and path array.
     */
    public List<List<Integer>> combinationSumBacktracking(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(candidates, 0, target, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int[] candidates, int index, int remainingTarget, 
                           List<Integer> currentPath, List<List<Integer>> result) {
        // BASE CASE REASONING:
        // If our target hits exactly 0, the items in our currentPath form a 
        // perfect combination! We must make a deep copy before adding it.
        if (remainingTarget == 0) {
            result.add(new ArrayList<>(currentPath));
            return;
        }

        // BASE CASE REASONING:
        // If our target drops below 0, or we run out of numbers to check, 
        // this path is a dead end. We just return to trigger the backtrack.
        if (remainingTarget < 0 || index >= candidates.length) {
            return;
        }

        // UNIVERSE 1: INCLUDE the current candidate
        // We add it to our path, and importantly, we KEEP the index the same 
        // so we can potentially pick it again.
        currentPath.add(candidates[index]);
        backtrack(candidates, index, remainingTarget - candidates[index], currentPath, result);
        
        // BACKTRACK: Undo the inclusion to explore the other universe.
        currentPath.remove(currentPath.size() - 1);

        // UNIVERSE 2: EXCLUDE the current candidate
        // We move to the next index. Target stays the same.
        backtrack(candidates, index + 1, remainingTarget, currentPath, result);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: True memoization requires us to return the EXACT result for a state 
     * `(index, remainingTarget)` so it can be cached. This means returning a 
     * full List of Lists from the recursive function.
     * 
     * Time Complexity: O(N * Target * Length_of_combinations)
     * Space Complexity: O(N * Target * memory_per_combination) - Massive memory overhead.
     */
    public List<List<Integer>> combinationSumMemo(int[] candidates, int target) {
        // memo[index][target]
        List<List<Integer>>[][] memo = new List[candidates.length][target + 1];
        return solveMemo(candidates, 0, target, memo);
    }

    private List<List<Integer>> solveMemo(int[] candidates, int index, int target, List<List<Integer>>[][] memo) {
        List<List<Integer>> res = new ArrayList<>();
        
        // BASE CASES
        if (target == 0) {
            res.add(new ArrayList<>()); // Add an empty path to signify success
            return res;
        }
        if (target < 0 || index >= candidates.length) {
            return res; // Empty list signifies failure
        }

        if (memo[index][target] != null) {
            return memo[index][target];
        }

        // 1. Get all successful paths from the EXCLUDE universe
        List<List<Integer>> excludePaths = solveMemo(candidates, index + 1, target, memo);
        for (List<Integer> path : excludePaths) {
            res.add(new ArrayList<>(path)); // Deep copy to prevent mutation
        }

        // 2. Get all successful paths from the INCLUDE universe
        List<List<Integer>> includePaths = solveMemo(candidates, index, target - candidates[index], memo);
        for (List<Integer> path : includePaths) {
            List<Integer> newPath = new ArrayList<>();
            newPath.add(candidates[index]); // Prepend our current choice
            newPath.addAll(path);           // Append the rest of the successful path
            res.add(newPath);
        }

        memo[index][target] = res;
        return res;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build a 2D grid where dp[i][j] stores the literal List of combinations 
     * that sum up to `j` using only the first `i` candidates.
     * 
     * Time Complexity: O(N * Target * Length_of_combinations)
     * Space Complexity: O(N * Target * memory_per_combination)
     */
    public List<List<Integer>> combinationSumTabulation(int[] candidates, int target) {
        int n = candidates.length;
        
        // dp[i][j] signifies: "The complete list of all valid combinations 
        // that sum to exactly 'j', using ONLY the first 'i' candidates."
        List<List<Integer>>[][] dp = new List[n + 1][target + 1];

        // Initialize the grid with empty lists
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= target; j++) {
                dp[i][j] = new ArrayList<>();
            }
        }

        // BASE CASE REASONING:
        // dp[i][0] represents making a sum of 0. 
        // There is exactly ONE valid way to make a sum of 0: picking nothing (an empty list).
        for (int i = 0; i <= n; i++) {
            dp[i][0].add(new ArrayList<>());
        }

        // Outer loop: Slowly unlock candidates one by one
        for (int i = 1; i <= n; i++) {
            
            int currentCandidate = candidates[i - 1];

            // Inner loop: Build up every target sum from 1 to 'target'
            for (int j = 1; j <= target; j++) {
                
                // UNIVERSE 1 (Exclude): I completely ignore this candidate.
                // We copy every successful combination from the row directly UP 
                // in our spreadsheet (using previous candidates for the exact same sum).
                for (List<Integer> path : dp[i - 1][j]) {
                    // We don't need a deep copy here because we treat inner lists as immutable
                    dp[i][j].add(path); 
                }

                // UNIVERSE 2 (Include): I force this candidate into my combinations.
                // PHYSICAL CHECK: Does it even fit into sum 'j'?
                if (currentCandidate <= j) {
                    
                    // Look LEFT in the same row. Because we have an infinite supply, 
                    // we look at combinations that successfully made the SMALLER 
                    // remaining sum (j - currentCandidate) using this exact same pool of candidates.
                    for (List<Integer> path : dp[i][j - currentCandidate]) {
                        
                        // We MUST make a deep copy before modifying it
                        List<Integer> newPath = new ArrayList<>(path);
                        
                        // We append our current candidate to this historical path
                        newPath.add(currentCandidate);
                        
                        // Register this brand new valid path into our current cell
                        dp[i][j].add(newPath);
                    }
                }
            }
        }

        return dp[n][target];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (1D Tabulation)
     * ========================================================================
     * Idea: In Tabulation, row `i` only ever looks at row `i-1` (up) and values 
     * to its left in row `i`. We can collapse this into a single 1D array of Lists!
     * 
     * Because this is Unbounded Knapsack, we traverse the array FORWARDS so that 
     * we can reuse the same candidate multiple times.
     * 
     * Time Complexity: O(N * Target * Length_of_combinations)
     * Space Complexity: O(Target * memory_per_combination) - Massively reduced memory!
     */
    public List<List<Integer>> combinationSumSpaceOptimized(int[] candidates, int target) {
        // dp[j] holds the list of combinations that sum to exactly 'j'
        List<List<Integer>>[] dp = new List[target + 1];
        
        for (int j = 0; j <= target; j++) {
            dp[j] = new ArrayList<>();
        }
        
        // BASE CASE REASONING:
        // Exactly 1 way to make a sum of 0 (the empty combination).
        dp[0].add(new ArrayList<>());

        // CRITICAL: The outer loop MUST be the candidates. 
        // If we swapped the loops, we would calculate Permutations (order matters) 
        // instead of Combinations. By locking the candidate on the outside, we 
        // guarantee that once we finish evaluating '2', we never add another '2'.
        for (int currentCandidate : candidates) {
            
            // Traverse FORWARDS from the candidate's value up to the target.
            for (int j = currentCandidate; j <= target; j++) {
                
                // We look back at the smaller sum (j - currentCandidate)
                for (List<Integer> path : dp[j - currentCandidate]) {
                    
                    List<Integer> newPath = new ArrayList<>(path);
                    newPath.add(currentCandidate);
                    
                    // We add this new path directly to dp[j], implicitly merging 
                    // it with the "Exclude" paths that were already sitting in dp[j]
                    // from previous outer loop iterations!
                    dp[j].add(newPath);
                }
            }
        }

        return dp[target];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new CombinationSum();
        
        record TestCase(int[] candidates, int target) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{2, 3, 6, 7}, 7),    // Expected: [[2,2,3],[7]]
            new TestCase(new int[]{2, 3, 5}, 8),       // Expected: [[2,2,2,2],[2,3,3],[3,5]]
            new TestCase(new int[]{2}, 1)              // Expected: [] (Impossible)
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Candidates: " + Arrays.toString(tc.candidates));
            System.out.println("Target    : " + tc.target);
            
            System.out.println("Backtracking (Optimal): " + solver.combinationSumBacktracking(tc.candidates, tc.target));
            System.out.println("Memoization           : " + solver.combinationSumMemo(tc.candidates, tc.target));
            System.out.println("Tabulation 2D         : " + solver.combinationSumTabulation(tc.candidates, tc.target));
            System.out.println("Tabulation 1D         : " + solver.combinationSumSpaceOptimized(tc.candidates, tc.target));
            System.out.println();
        }
    }
}

/*
 * =================================================================================
 * SECTION 1: PROBLEM RESTATEMENT
 * =================================================================================
 *
 * We are given:
 *   - An array `nums` of DISTINCT positive integers (the "candidates").
 *   - A single integer `target`.
 *
 * We must return every UNIQUE combination of numbers from `nums` whose sum is
 * exactly `target`. A number may be reused an unlimited number of times within
 * a single combination (this is the "unbounded knapsack" flavor of the problem,
 * NOT a subset-sum problem where each element is used at most once).
 *
 * "Unique" is defined by MULTISET equality: two combinations are the same
 * combination if and only if every value appears the same number of times in
 * both. Order of elements within a combination does not matter, and order of
 * combinations in the returned list does not matter.
 *
 * Constraints (as given):
 *   1 <= nums.length   <= 30
 *   2 <= nums[i]       <= 40
 *   1 <= target        <= 40
 *   All values in nums are distinct.
 *
 * Key implication of the constraints: since every candidate is >= 2 and the
 * target is <= 40, the maximum depth of any valid combination is bounded
 * (at most 20 elements, using the smallest possible value of 2 repeatedly).
 * This keeps the recursion depth and the theoretical worst-case output size
 * bounded, even though the problem is exponential in the general case.
 *
 * This is LeetCode 39, "Combination Sum".
 */

/*
 * =================================================================================
 * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
 * =================================================================================
 *
 * 1. Q: Can the same number be used more than once in a single combination?
 *    A: Yes — explicitly stated as unlimited reuse.
 *
 * 2. Q: Are all values in `nums` guaranteed positive and distinct, with no
 *       zero or negative values?
 *    A: Yes — constraints guarantee 2 <= nums[i] <= 40 and all distinct.
 *
 * 3. Q: What should be returned if NO combination sums to target?
 *    A: An empty list (not null).
 *
 * 4. Q: Does the order of combinations in the result, or the order of numbers
 *       within a combination, matter for correctness?
 *    A: No — "may be returned in any order." We will normalize to ascending
 *       order within each combination for determinism and easy testing.
 *
 * 5. Q: Given the constraints (target <= 40, min value >= 2), could the
 *       output itself become extremely large (exponential blow-up)?
 *    A: Bounded in practice by these constraints, but conceptually yes —
 *       the number of combinations can still be large, so any complexity
 *       analysis must be output-sensitive.
 *
 * 6. Q: Should the solution defensively validate input (null array, target
 *       <= 0, duplicate/negative candidates), or can we assume clean input
 *       per the stated constraints?
 *    A: For the interview whiteboard solution, assume clean input matching
 *       constraints. The production-quality version will still validate
 *       defensively, since real-world callers may violate assumptions.
 *
 * 7. Q: Is this expected to run in a single-threaded context, or do we need
 *       to worry about concurrent access to shared state?
 *    A: Single-threaded; no concurrency concerns.
 *
 * 8. Q: Is there a preference for iterative vs. recursive solutions, e.g.
 *       due to stack-depth concerns?
 *    A: Recursion depth is bounded by target / min(nums) <= 20, so recursion
 *       is safe here; no need to force an iterative solution, though we will
 *       discuss one for completeness.
 */

/*
 * =================================================================================
 * SECTION 3: EXAMPLES & EDGE CASES
 * =================================================================================
 *
 * Example 1 (normal case):
 *   nums = [2, 3, 6, 7], target = 7
 *   -> [[2, 2, 3], [7]]
 *   Two combinations: three 2's-and-a-3, or a single 7.
 *
 * Example 2 (edge case — no valid combination):
 *   nums = [2], target = 1
 *   -> []
 *   The only candidate (2) already exceeds the target, so nothing sums to it.
 *
 * Example 3 (boundary / "unique by frequency" case):
 *   nums = [2, 3, 5], target = 8
 *   -> [[2, 2, 2, 2], [2, 3, 3], [3, 5]]
 *   This demonstrates the "unique combination" rule directly: [2,3,3] and
 *   [3,5] both use the numbers {2,3,5} but with different multiplicities,
 *   and [2,2,2,2] uses only repeated 2's. All three are distinct outputs
 *   because at least one number's frequency differs between them.
 */

public final class CombinationSum {

    /*
     * =============================================================================
     * SECTION 4: PARADIGM SWEEP
     * =============================================================================
     * Before diving into solutions, we sweep across standard paradigms to decide
     * which genuinely apply here, and reject the rest with a one-line reason.
     *
     *  - Brute force / naive            -> APPLICABLE (Approach 1 below).
     *  - Sorting-based                  -> APPLICABLE (enables early-termination
     *                                       pruning and canonical ordering; used
     *                                       inside Approaches 2-4, not standalone).
     *  - Hashing-based                  -> NOT APPLICABLE STANDALONE: memoizing on
     *                                       (start, remaining) doesn't help because
     *                                       the *output itself* is what's expensive
     *                                       to produce (path-dependent), not a
     *                                       reusable scalar subresult.
     *  - Two pointer / sliding window   -> NOT APPLICABLE: there is no contiguous
     *                                       subarray or monotonic window structure;
     *                                       combinations draw from the whole set
     *                                       with repetition, not a sliding range.
     *  - Divide and conquer             -> NOT APPLICABLE: splitting nums in half
     *                                       and merging combination sets across the
     *                                       split is strictly more complex than
     *                                       backtracking/DP with no asymptotic gain.
     *  - Greedy                         -> NOT APPLICABLE: no greedy-choice property.
     *                                       Picking the largest (or smallest) value
     *                                       first can strand you unable to reach the
     *                                       exact target (e.g. nums=[3,5], target=8:
     *                                       greedily taking 5 first then needing 3
     *                                       happens to work, but nums=[2,5], target=9
     *                                       greedily taking 5 then 2 leaves remainder
     *                                       2, unreachable, even though [2,2,... ] etc.
     *                                       may or may not exist — no exchange
     *                                       argument guarantees correctness here).
     *  - Tree / graph traversal         -> APPLICABLE: backtracking IS an implicit
     *                                       DFS over a decision tree (Approach 2);
     *                                       an explicit BFS variant is Approach 4.
     *  - Heap / priority queue          -> NOT APPLICABLE: no "top-k" / ordering
     *                                       requirement — we need ALL valid combos,
     *                                       not the best ones by some priority.
     *  - Dynamic programming            -> APPLICABLE (Approach 3), though as we'll
     *                                       show, it offers no asymptotic advantage
     *                                       for *enumeration* problems like this one.
     *  - Binary search                  -> NOT APPLICABLE: no sorted search space of
     *                                       answers or monotonic predicate to search
     *                                       over; we already use sorted-array early
     *                                       break, but that's pruning, not searching.
     *  - Monotonic stack / deque        -> NOT APPLICABLE: no need to maintain a
     *                                       monotonic invariant over a sequence.
     *  - Trie / segment tree            -> NOT APPLICABLE: no prefix-matching or
     *                                       range-query structure in this problem.
     */

    /*
     * =============================================================================
     * APPROACH 1: Brute Force — Recursive Count Enumeration
     * =============================================================================
     * IDEA (plain English):
     *   For each candidate number, decide how many times to use it (0, 1, 2, ...
     *   up to target/num), independently of every other number, then check if the
     *   resulting total hits target exactly. This is literally trying every
     *   combination of "multiplicities" (a Cartesian product across all candidates)
     *   rather than making one smart, prunable decision at a time.
     *
     * DATA STRUCTURE / PARADIGM:
     *   Plain recursion over "count vectors" (c_0, c_1, ..., c_{n-1}); no pruning
     *   beyond the trivial bound needed just to keep the loop finite.
     *
     * TIME COMPLEXITY:
     *   O( PRODUCT_i (target / nums[i] + 1) ) in the worst case — this is the size
     *   of the full Cartesian product of possible counts per number, which is far
     *   larger than the number of *valid* combinations, because we don't prune
     *   partial sums against the remaining target as we go across different
     *   numbers — we only bound each number's count independently.
     *
     * SPACE COMPLEXITY:
     *   O(target / min(nums)) for recursion depth (one stack frame per candidate
     *   index) plus O(#combinations * average length) to store the output.
     *
     * PROS:
     *   - Conceptually the simplest possible correct approach; easy to state.
     *   - Good as a correctness ORACLE to validate faster approaches against.
     * CONS:
     *   - Wastes enormous work exploring count combinations whose PARTIAL sums
     *     already exceed target long before reaching the last index.
     *   - Does not scale even to moderate inputs within these constraints.
     * WHEN TO USE:
     *   - Never in production or in an interview as a final answer; only as a
     *     mental warm-up or a brute-force cross-check in tests.
     */
    static final class BruteForceApproach {
        static List<List<Integer>> solve(int[] nums, int target) {
            List<List<Integer>> result = new ArrayList<>();
            int[] sorted = nums.clone();
            Arrays.sort(sorted); // sort purely so output combinations are printed ascending
            enumerateCounts(sorted, 0, target, new ArrayList<>(), result);
            return result;
        }

        private static void enumerateCounts(int[] sortedNums, int index, int remaining,
                                             List<Integer> currentCombo, List<List<Integer>> result) {
            if (index == sortedNums.length) {
                if (remaining == 0) {
                    result.add(new ArrayList<>(currentCombo));
                }
                return;
            }
            int num = sortedNums[index];
            int maxCount = remaining / num; // only bound to keep the loop finite, no smarter pruning
            for (int count = 0; count <= maxCount; count++) {
                for (int k = 0; k < count; k++) currentCombo.add(num);
                enumerateCounts(sortedNums, index + 1, remaining - count * num, currentCombo, result);
                for (int k = 0; k < count; k++) currentCombo.remove(currentCombo.size() - 1);
            }
        }
    }

    /*
     * =============================================================================
     * APPROACH 2: Backtracking with Sorting + Pruning  (RECOMMENDED / OPTIMAL)
     * =============================================================================
     * IDEA (plain English):
     *   Sort the candidates first. Do a DFS where, at each step, we either pick the
     *   current candidate (and stay on it, since reuse is allowed) or move on to
     *   the next candidate. We never revisit an earlier index, which is exactly
     *   what enforces the "non-decreasing sequence" invariant that guarantees each
     *   combination is generated exactly once (no duplicate permutations of the
     *   same multiset). Because the array is sorted, the moment a candidate is
     *   too large for the remaining budget, every later candidate is too large as
     *   well — so we can BREAK out of the loop instead of merely skipping one
     *   candidate, which is the crucial pruning step.
     *
     * DATA STRUCTURE / PARADIGM:
     *   DFS / backtracking over an implicit decision tree; sorting for pruning.
     *
     * TIME COMPLEXITY:
     *   Output-sensitive: O(N^(T/M)) worst case, where N = nums.length,
     *   T = target, M = min(nums) — this bounds the branching factor (N) raised
     *   to the maximum recursion depth (T/M). In practice this closely tracks the
     *   true number of valid combinations times their average length, because we
     *   prune every dead branch the instant a partial sum can no longer reach
     *   the target.
     *
     * SPACE COMPLEXITY:
     *   O(T/M) for the recursion stack / current path, plus O(#combinations *
     *   average length) for the output itself.
     *
     * PROS:
     *   - Only ever explores branches that are still numerically feasible.
     *   - Clean, minimal, easy to reason about and explain live in an interview.
     *   - Matches the output-sensitive lower bound as closely as any approach can.
     * CONS:
     *   - Still exponential in the worst case — but this is inherent to the
     *     problem (the output itself can be exponentially large), not a flaw
     *     of this particular algorithm.
     * WHEN TO USE:
     *   - This is the standard, expected interview solution for "enumerate all
     *     combinations/subsets satisfying property X" problems.
     */
    static final class BacktrackingApproach {
        static List<List<Integer>> solve(int[] nums, int target) {
            int[] sorted = nums.clone();
            Arrays.sort(sorted); // enables the "break" pruning below
            List<List<Integer>> result = new ArrayList<>();
            backtrack(sorted, target, 0, new ArrayDeque<>(), result);
            return result;
        }

        private static void backtrack(int[] sortedNums, int remaining, int start,
                                       Deque<Integer> path, List<List<Integer>> result) {
            if (remaining == 0) {
                result.add(new ArrayList<>(path));
                return;
            }
            for (int i = start; i < sortedNums.length; i++) {
                int candidate = sortedNums[i];
                if (candidate > remaining) {
                    break; // sorted ascending: every later candidate is also too big
                }
                path.addLast(candidate);
                // pass `i` (not i + 1) so this candidate may be reused
                backtrack(sortedNums, remaining - candidate, i, path, result);
                path.removeLast(); // undo choice before trying the next candidate
            }
        }
    }

    /*
     * =============================================================================
     * APPROACH 3: Dynamic Programming — Bottom-Up Sum Table
     * =============================================================================
     * IDEA (plain English):
     *   Build a table dp[s] = "every combination of candidates that sums to s",
     *   for s = 0 .. target, from smaller sums up to larger ones. dp[0] starts as
     *   the single empty combination. For each sum s, for each candidate num <= s,
     *   every combination in dp[s - num] can be extended with one more `num` to
     *   produce a combination summing to s — PROVIDED num is >= the last element
     *   already in that combination, which (as in Approach 2) is what prevents
     *   generating the same multiset in more than one order.
     *
     * DATA STRUCTURE / PARADIGM:
     *   Bottom-up DP where each table cell stores a LIST OF LISTS rather than a
     *   scalar. This is a deliberate contrast with typical DP: normally DP saves
     *   work by memoizing a small summary (a count, a boolean, a min/max) of a
     *   subproblem. Here, because we must materialize every combination, each
     *   cell's "state" is exactly as large as its portion of the final answer.
     *
     * TIME COMPLEXITY:
     *   Same exponential class as Approach 2 in terms of the final output size,
     *   but with a WORSE constant factor: for every sum s and every candidate,
     *   we copy every combination in a smaller cell to build a new list. This
     *   repeated copying is pure overhead that backtracking avoids entirely by
     *   mutating one shared path in place (add/remove) instead of allocating a
     *   fresh list per extension.
     *
     * SPACE COMPLEXITY:
     *   O(target) table entries, each holding potentially many full combination
     *   lists — in the worst case this uses noticeably MORE memory than
     *   backtracking, because intermediate sums s < target also fully store all
     *   of their combinations (even ones that turn out not to be needed for the
     *   final answer, if we only cared about dp[target]).
     *
     * PROS:
     *   - Iterative — no recursion / call stack at all.
     *   - Naturally exposes "how many ways to make every sum up to target," which
     *     is useful if a follow-up asks for combinations for MULTIPLE targets at
     *     once (the table is reusable across targets <= the max precomputed sum).
     * CONS:
     *   - No asymptotic advantage over backtracking for ENUMERATION (as opposed
     *     to COUNTING) problems: since we must output every combination anyway,
     *     the work is lower-bounded by the output size either way, and DP adds
     *     extra list-copying overhead on top.
     *   - Uses more peak memory than backtracking (which uses O(depth) space for
     *     the current path instead of storing full lists for every intermediate
     *     sum).
     * WHEN TO USE:
     *   - Good to mention as an alternative and to demonstrate DP fluency, and
     *     genuinely useful if the interviewer extends the problem to "count the
     *     number of combinations" (dp[s] becomes a single integer — THAT variant
     *     is where DP shines over backtracking). For enumerating actual
     *     combinations, prefer Approach 2.
     */
    static final class DynamicProgrammingApproach {
        static List<List<Integer>> solve(int[] nums, int target) {
            int[] sorted = nums.clone();
            Arrays.sort(sorted); // lets us break early per sum, and keeps combos ascending

            // dp[s] = list of all combinations (each already ascending) summing to s
            List<List<List<Integer>>> dp = new ArrayList<>(target + 1);
            for (int s = 0; s <= target; s++) {
                dp.add(new ArrayList<>());
            }
            dp.get(0).add(new ArrayList<>()); // base case: one way to make 0 — use nothing

            for (int sum = 1; sum <= target; sum++) {
                for (int num : sorted) {
                    if (num > sum) {
                        break; // sorted ascending -> no larger candidate fits either
                    }
                    int remainder = sum - num;
                    for (List<Integer> smallerCombo : dp.get(remainder)) {
                        // only extend if `num` continues a non-decreasing sequence,
                        // which is exactly what prevents duplicate permutations
                        if (smallerCombo.isEmpty() || smallerCombo.get(smallerCombo.size() - 1) <= num) {
                            List<Integer> extended = new ArrayList<>(smallerCombo);
                            extended.add(num);
                            dp.get(sum).add(extended);
                        }
                    }
                }
            }
            return dp.get(target);
        }
    }

    /*
     * =============================================================================
     * APPROACH 4: BFS — Iterative Level-Order Traversal of the Decision Tree
     * =============================================================================
     * IDEA (plain English):
     *   Instead of recursing (implicit DFS call stack), use an explicit queue of
     *   "partial states" — (current combination so far, remaining target, next
     *   candidate index to consider). Repeatedly pop a state, and if remaining
     *   is 0, record it as a completed combination; otherwise push one new state
     *   per still-feasible candidate, same pruning rule (break on sorted array).
     *
     * DATA STRUCTURE / PARADIGM:
     *   Explicit queue-driven traversal of the same implicit tree Approach 2
     *   traverses via recursion — i.e. BFS instead of DFS over identical nodes.
     *
     * TIME COMPLEXITY:
     *   Identical to Approach 2 — same nodes are visited, same pruning applies.
     *
     * SPACE COMPLEXITY:
     *   Can be WORSE than Approach 2 in peak memory: BFS must hold every
     *   in-flight partial combination in the queue simultaneously (the entire
     *   "frontier"), whereas DFS/backtracking only ever holds ONE path (via
     *   add/remove) at O(depth) space at any instant.
     *
     * PROS:
     *   - No recursion / call-stack depth concerns at all (fully iterative).
     *   - Sometimes easier to adapt to level-by-level processing or to add
     *     timeouts/cancellation between levels in a production system.
     * CONS:
     *   - Higher peak memory than DFS backtracking, since many partial paths
     *     coexist in the queue at once instead of one shared mutable path.
     *   - Less idiomatic for this problem family; most interviewers expect DFS.
     * WHEN TO USE:
     *   - When recursion depth is a genuine concern (not the case here, since
     *     depth <= target/min(nums) <= 20), or when a codebase's style strongly
     *     prefers iterative traversal.
     */
    static final class BFSApproach {
        // A single node in the (implicit) decision tree: the path taken so far,
        // how much of the target remains, and which index to branch from next.
        private record State(List<Integer> path, int remaining, int nextIndex) {}

        static List<List<Integer>> solve(int[] nums, int target) {
            int[] sorted = nums.clone();
            Arrays.sort(sorted);

            List<List<Integer>> result = new ArrayList<>();
            Deque<State> queue = new ArrayDeque<>();
            queue.add(new State(List.of(), target, 0));

            while (!queue.isEmpty()) {
                State current = queue.poll();
                if (current.remaining() == 0) {
                    result.add(new ArrayList<>(current.path()));
                    continue;
                }
                for (int i = current.nextIndex(); i < sorted.length; i++) {
                    int candidate = sorted[i];
                    if (candidate > current.remaining()) {
                        break; // sorted ascending -> prune the rest of this level
                    }
                    List<Integer> extendedPath = new ArrayList<>(current.path());
                    extendedPath.add(candidate);
                    queue.add(new State(extendedPath, current.remaining() - candidate, i));
                }
            }
            return result;
        }
    }

    /*
     * =============================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * =============================================================================
     *
     * Approach                         | Time                              | Space                                  | Best For                                   | Limitations
     * ----------------------------------+-----------------------------------+-----------------------------------------+---------------------------------------------+---------------------------------------------------
     * 1. Brute Force (Count Enum.)     | O(PRODUCT(target/nums[i] + 1))    | O(target/min) depth + output            | Baseline correctness oracle in tests        | Explodes even for tiny inputs; no partial pruning
     * 2. Backtracking + Sort + Prune   | O(N^(T/M)), output-sensitive       | O(T/M) depth + output                   | THE standard interview answer               | Still exponential worst case (inherent to problem)
     * 3. Dynamic Programming (bottom-up)| Same class as (2), worse constant | O(T) table entries, each list-heavy     | Reuse across multiple targets; DP fluency   | No asymptotic gain for enumeration; more memory
     * 4. BFS (iterative, queue-based)  | Same as (2)                       | O(frontier width), can exceed (2)       | Avoiding recursion / call-stack depth        | Higher peak memory than DFS; less idiomatic here
     *
     * (N = nums.length, T = target, M = min(nums))
     */

    /*
     * =============================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * =============================================================================
     * Recommendation: APPROACH 2 — Backtracking with Sorting + Pruning.
     *
     * Why:
     *   - Clarity: the "sort, then DFS choosing to reuse-or-advance, prune via
     *     break" pattern is a well-known idiom interviewers immediately recognize
     *     and can follow line-by-line as you narrate it.
     *   - Coding speed: it's ~15-20 lines of code, easy to write correctly under
     *     time pressure, with an obvious base case and an obvious loop invariant.
     *   - Interviewer expectations: for "generate all X satisfying Y" problems,
     *     DFS/backtracking IS the expected paradigm; deviating to DP (Approach 3)
     *     or BFS (Approach 4) without first presenting backtracking can read as
     *     missing the standard technique, even though both are valid to mention
     *     as follow-up alternatives.
     *   - Optimality: its time complexity is output-sensitive — we only do work
     *     proportional to combinations that are actually still numerically
     *     feasible, which is the best any algorithm can hope to achieve, since
     *     every valid combination must be visited at least once to be reported.
     *
     * Interview narrative: present Approach 2 as the anchor solution first,
     * mention Approach 1 briefly only if asked "what's the naive approach,"
     * and proactively offer Approaches 3 and 4 as alternatives with their
     * trade-offs once the core solution is accepted — this demonstrates breadth
     * without burying the interviewer in unnecessary code up front.
     */

    /*
     * =============================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL SOLUTION
     * =============================================================================
     */
    static final class ProductionSolution {

        /**
         * Returns every unique combination of {@code candidates} whose elements
         * sum exactly to {@code target}. A candidate may be used an unlimited
         * number of times within a single combination. Two combinations are
         * considered distinct if and only if the frequency of at least one
         * candidate differs between them.
         *
         * @param candidates a non-null, non-empty array of distinct positive
         *                   integers to draw from
         * @param target     the positive integer sum to reach
         * @return a list of combinations (each combination itself a list of
         *         integers in non-decreasing order); never {@code null}, but
         *         may be empty if no combination sums to {@code target}
         * @throws IllegalArgumentException if {@code candidates} is null/empty,
         *         contains a non-positive or duplicate value, or if
         *         {@code target} is not positive
         */
        static List<List<Integer>> combinationSum(int[] candidates, int target) {
            validateInput(candidates, target);

            // Defensive copy + sort: sorting is what enables the early "break"
            // pruning below, and it also gives every output combination a
            // deterministic, ascending internal order.
            int[] sortedCandidates = candidates.clone();
            Arrays.sort(sortedCandidates);

            List<List<Integer>> result = new ArrayList<>();
            // ArrayDeque used as a stack (addLast/removeLast) for the current
            // path: O(1) amortized push/pop and no boxing overhead beyond the
            // Integer elements themselves.
            Deque<Integer> currentPath = new ArrayDeque<>();

            backtrack(sortedCandidates, target, 0, currentPath, result);
            return result;
        }

        /**
         * Explores every way to reach {@code remainingTarget} using candidates
         * at index {@code startIndex} or later (reuse of the same index is
         * allowed, which is what permits unlimited repetition of a value).
         *
         * @param sortedCandidates candidates sorted ascending
         * @param remainingTarget  how much more we still need to sum to
         * @param startIndex       the earliest index we're allowed to pick from;
         *                         never revisiting an earlier index guarantees
         *                         each multiset of values is generated exactly
         *                         once (no duplicate permutations)
         * @param currentPath      the combination built so far (shared, mutated
         *                         in place via push/pop for efficiency)
         * @param result           accumulator for completed combinations
         */
        private static void backtrack(int[] sortedCandidates, int remainingTarget, int startIndex,
                                       Deque<Integer> currentPath, List<List<Integer>> result) {
            if (remainingTarget == 0) {
                // Found a complete, valid combination — snapshot it, since
                // currentPath will continue to be mutated after we return.
                result.add(new ArrayList<>(currentPath));
                return;
            }

            for (int i = startIndex; i < sortedCandidates.length; i++) {
                int candidate = sortedCandidates[i];

                if (candidate > remainingTarget) {
                    // Because sortedCandidates is ascending, every candidate
                    // from this point on is >= this one, hence also too large.
                    // We can stop scanning this level entirely (not just skip).
                    break;
                }

                currentPath.addLast(candidate);
                // Pass `i` (not i + 1): staying at the same index is precisely
                // what allows this candidate to be reused within the combination.
                backtrack(sortedCandidates, remainingTarget - candidate, i, currentPath, result);
                currentPath.removeLast(); // backtrack: undo before trying the next candidate
            }
        }

        private static void validateInput(int[] candidates, int target) {
            if (candidates == null) {
                throw new IllegalArgumentException("candidates must not be null");
            }
            if (candidates.length == 0) {
                throw new IllegalArgumentException("candidates must not be empty");
            }
            if (target <= 0) {
                throw new IllegalArgumentException("target must be a positive integer");
            }
            Set<Integer> seenValues = new HashSet<>();
            for (int value : candidates) {
                if (value <= 0) {
                    throw new IllegalArgumentException("candidates must be positive, found: " + value);
                }
                if (!seenValues.add(value)) {
                    throw new IllegalArgumentException("candidates must be distinct, duplicate: " + value);
                }
            }
        }
    }

    /*
     * =============================================================================
     * SECTION 10: DRY RUN / TRACE
     * =============================================================================
     * Tracing ProductionSolution.combinationSum(nums = [2, 3, 6, 7], target = 7).
     * sortedCandidates = [2, 3, 6, 7] (already sorted).
     *
     * backtrack(remaining=7, start=0, path=[])
     *   i=0, candidate=2 (<=7): path=[2]
     *     backtrack(remaining=5, start=0, path=[2])
     *       i=0, candidate=2 (<=5): path=[2,2]
     *         backtrack(remaining=3, start=0, path=[2,2])
     *           i=0, candidate=2 (<=3): path=[2,2,2]
     *             backtrack(remaining=1, start=0, path=[2,2,2])
     *               i=0, candidate=2 (>1): BREAK  -> no combination found here
     *           path back to [2,2]
     *           i=1, candidate=3 (<=3): path=[2,2,3]
     *             backtrack(remaining=0, start=1, path=[2,2,3])
     *               remaining==0 -> RECORD [2,2,3]
     *           path back to [2,2]
     *           i=2, candidate=6 (>3): BREAK
     *       path back to [2]
     *       i=1, candidate=3 (<=5): path=[2,3]
     *         backtrack(remaining=2, start=1, path=[2,3])
     *           i=1, candidate=3 (>2): BREAK
     *       path back to [2]
     *       i=2, candidate=6 (>5): BREAK
     *   path back to []
     *   i=1, candidate=3 (<=7): path=[3]
     *     backtrack(remaining=4, start=1, path=[3])
     *       i=1, candidate=3 (<=4): path=[3,3]
     *         backtrack(remaining=1, start=1, path=[3,3])
     *           i=1, candidate=3 (>1): BREAK
     *       path back to [3]
     *       i=2, candidate=6 (>4): BREAK
     *   path back to []
     *   i=2, candidate=6 (<=7): path=[6]
     *     backtrack(remaining=1, start=2, path=[6])
     *       i=2, candidate=6 (>1): BREAK
     *   path back to []
     *   i=3, candidate=7 (<=7): path=[7]
     *     backtrack(remaining=0, start=3, path=[7])
     *       remaining==0 -> RECORD [7]
     *   path back to []
     *
     * Final result: [[2, 2, 3], [7]]  — matches the expected output.
     */

    /*
     * =============================================================================
     * SECTION 11: CLOSING SUMMARY
     * =============================================================================
     * All four approaches are correct; they differ in HOW MUCH WASTED WORK they do
     * relative to the true output size:
     *   - Brute Force (1) explores far more than necessary because it can't prune
     *     across candidates as it builds a combination.
     *   - Backtracking (2) is essentially optimal: it visits exactly the feasible
     *     branches of the decision tree, using minimal O(depth) auxiliary space.
     *   - DP (3) is asymptotically no better than (2) here, and uses more memory,
     *     because ENUMERATION problems don't compress into small subproblem
     *     summaries the way COUNTING problems do.
     *   - BFS (4) visits the same nodes as (2) but trades stack space for queue
     *     space, generally increasing peak memory.
     *
     * Known assumptions / limitations of the final (ProductionSolution) approach:
     *   - Assumes candidates are distinct (validated) — duplicate candidate
     *     values would require an additional "skip duplicate siblings at the
     *     same recursion level" rule to avoid duplicate combinations.
     *   - Assumes all candidates and target are positive; zero or negative
     *     values would break both the termination argument and the pruning
     *     logic (e.g. a candidate of 0 could recurse infinitely without a
     *     separate "each index used at most K times" guard).
     *   - Complexity is inherently output-sensitive/exponential in the worst
     *     case; this is a property of the problem, not a fixable inefficiency.
     */

    /*
     * =============================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * =============================================================================
     * 1. "What if each candidate could only be used ONCE (no repetition), and
     *     nums might contain duplicates?" (-> LeetCode 40, Combination Sum II;
     *     requires advancing start index to i+1 AND skipping duplicate values
     *     at the same recursion depth to avoid duplicate combinations.)
     * 2. "What if we only need to COUNT the number of combinations, not list
     *     them?" (-> classic unbounded-knapsack DP, dp[s] = sum of dp[s-num];
     *     this is exactly where DP outperforms backtracking asymptotically.)
     * 3. "What if target could be as large as 10^6 or candidates.length up to
     *     10^5?" (-> enumeration becomes infeasible in general since output
     *     size itself can be exponential; you'd clarify whether the caller
     *     truly needs every combination or just the count / existence.)
     * 4. "Can you return combinations sorted by length, or by lexicographic
     *     order?" (-> trivial post-processing sort of the result list; could
     *     also be produced in that order directly via iterative deepening.)
     * 5. "How would you parallelize this across multiple threads?" (-> split
     *     the top-level loop over the first candidate into independent tasks,
     *     since each top-level branch is fully independent of the others; each
     *     worker needs its own local path/result to avoid shared mutable state.)
     * 6. "What if nums is extremely large but target is small?" (-> pre-filter
     *     candidates to only those <= target before recursing, which the
     *     current solution already effectively does via the sorted break.)
     */

    /*
     * =============================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * =============================================================================
     * 1. Passing `i + 1` instead of `i` as the next start index — this silently
     *    turns the problem into "each number used at most once" (Combination
     *    Sum II behavior) and produces wrong answers whenever reuse is required.
     * 2. Using `continue` instead of `break` after sorting — this still produces
     *    a CORRECT answer, but candidates often don't realize `break` is a valid,
     *    strictly better optimization once the array is sorted, and fail to
     *    mention/justify it when asked about further optimization.
     * 3. Forgetting to snapshot the path with `new ArrayList<>(currentPath)`
     *    when recording a result — since `currentPath` is a single mutable
     *    object reused via push/pop, storing a reference to it directly means
     *    every recorded "result" ends up referencing the SAME list, which is
     *    empty by the time backtracking finishes.
     * 4. Not sorting nums at all — without sorting, the early-break pruning is
     *    unsound (a later, larger element sitting before a smaller one would
     *    be wrongly skipped), forcing a fallback to `continue`, which still
     *    works but loses the "sorted array -> prune whole tail" optimization
     *    and is easy to get subtly wrong under time pressure.
     */

    /*
     * =============================================================================
     * TEST HARNESS (main): cross-validates all four approaches against each
     * other, using multiset-of-combinations equality (order-independent both
     * within and across combinations), on fixed examples plus a randomized
     * stress test over small inputs.
     * =============================================================================
     */
    public static void main(String[] args) {
        runFixedExample("Example 1 (normal)", new int[]{2, 3, 6, 7}, 7);
        runFixedExample("Example 2 (edge: no valid combination)", new int[]{2}, 1);
        runFixedExample("Example 3 (boundary: frequency tie-break)", new int[]{2, 3, 5}, 8);
        runFixedExample("Single-element exact match", new int[]{5}, 5);
        runFixedExample("Larger target", new int[]{2, 3, 5, 7}, 12);

        runRandomizedStressTest();

        System.out.println("\nAll tests passed across BruteForce, Backtracking, DP, and BFS approaches.");
    }

    private static void runFixedExample(String label, int[] nums, int target) {
        List<List<Integer>> bruteForce = BruteForceApproach.solve(nums, target);
        List<List<Integer>> backtracking = BacktrackingApproach.solve(nums, target);
        List<List<Integer>> dp = DynamicProgrammingApproach.solve(nums, target);
        List<List<Integer>> bfs = BFSApproach.solve(nums, target);
        List<List<Integer>> production = ProductionSolution.combinationSum(nums, target);

        assertSameCombinations(label, bruteForce, backtracking, dp, bfs, production);

        System.out.println(label + " -> nums=" + Arrays.toString(nums) + ", target=" + target
                + " => " + canonicalize(production));
    }

    private static void runRandomizedStressTest() {
        Random random = new Random(42); // fixed seed for reproducibility
        int trialCount = 200;
        for (int trial = 0; trial < trialCount; trial++) {
            int size = 1 + random.nextInt(4);          // 1..4 candidates
            int target = 1 + random.nextInt(15);       // 1..15, kept small so brute force stays feasible
            Set<Integer> valueSet = new LinkedHashSet<>();
            while (valueSet.size() < size) {
                valueSet.add(2 + random.nextInt(9));   // 2..10, distinct
            }
            int[] nums = valueSet.stream().mapToInt(Integer::intValue).toArray();

            List<List<Integer>> bruteForce = BruteForceApproach.solve(nums, target);
            List<List<Integer>> backtracking = BacktrackingApproach.solve(nums, target);
            List<List<Integer>> dp = DynamicProgrammingApproach.solve(nums, target);
            List<List<Integer>> bfs = BFSApproach.solve(nums, target);
            List<List<Integer>> production = ProductionSolution.combinationSum(nums, target);

            String label = "Stress trial " + trial + " (nums=" + Arrays.toString(nums) + ", target=" + target + ")";
            assertSameCombinations(label, bruteForce, backtracking, dp, bfs, production);
        }
        System.out.println("Randomized stress test passed: " + trialCount + " trials.");
    }

    // Sorts each combination ascending (they already are, given sorted-input
    // approaches) and sorts the outer list lexicographically, producing a
    // canonical form so two equivalent result sets compare equal regardless
    // of the order combinations were discovered in.
    private static List<List<Integer>> canonicalize(List<List<Integer>> combinations) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> combo : combinations) {
            List<Integer> sortedCombo = new ArrayList<>(combo);
            Collections.sort(sortedCombo);
            copy.add(sortedCombo);
        }
        copy.sort((a, b) -> {
            int lengthCompare = Integer.compare(a.size(), b.size());
            if (lengthCompare != 0) return lengthCompare;
            for (int index = 0; index < a.size(); index++) {
                int elementCompare = Integer.compare(a.get(index), b.get(index));
                if (elementCompare != 0) return elementCompare;
            }
            return 0;
        });
        return copy;
    }

    @SafeVarargs
    private static void assertSameCombinations(String label, List<List<Integer>>... allResults) {
        List<List<Integer>> reference = canonicalize(allResults[0]);
        for (int index = 1; index < allResults.length; index++) {
            List<List<Integer>> candidate = canonicalize(allResults[index]);
            if (!reference.equals(candidate)) {
                throw new AssertionError(label + ": mismatch at result index " + index
                        + "\nExpected: " + reference + "\nActual:   " + candidate);
            }
        }
    }
}
