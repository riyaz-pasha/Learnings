import java.util.*;
/*
 * Given an array of distinct integers candidates and a target integer target,
 * return a list of all unique combinations of candidates where the chosen
 * numbers sum to target. You may return the combinations in any order.
 * 
 * The same number may be chosen from candidates an unlimited number of times.
 * Two combinations are unique if the frequency of at least one of the chosen
 * numbers is different.
 * 
 * The test cases are generated such that the number of unique combinations that
 * sum up to target is less than 150 combinations for the given input.
 * 
 * Example 1:
 * Input: candidates = [2,3,6,7], target = 7
 * Output: [[2,2,3],[7]]
 * Explanation:
 * 2 and 3 are candidates, and 2 + 2 + 3 = 7. Note that 2 can be used multiple
 * times.
 * 7 is a candidate, and 7 = 7.
 * These are the only two combinations.
 * 
 * Example 2:
 * Input: candidates = [2,3,5], target = 8
 * Output: [[2,2,2,2],[2,3,3],[3,5]]
 *
 * Example 3:
 * Input: candidates = [2], target = 1
 * Output: []
 */

class CombinationSum {

    // Solution 1: Classic Backtracking (Most Common)
    public List<List<Integer>> combinationSum1(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int[] candidates, int target, int start,
            List<Integer> current, List<List<Integer>> result) {
        // Base cases
        if (target == 0) {
            result.add(new ArrayList<>(current));
            return;
        }
        if (target < 0) {
            return;
        }

        // Try each candidate starting from 'start' index
        for (int i = start; i < candidates.length; i++) {
            current.add(candidates[i]);
            // Same number can be used multiple times, so we pass 'i' not 'i+1'
            backtrack(candidates, target - candidates[i], i, current, result);
            current.remove(current.size() - 1); // backtrack
        }
    }

    // Solution 2: Optimized Backtracking with Sorting
    public List<List<Integer>> combinationSum2(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates); // Sort for early termination
        backtrackOptimized(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrackOptimized(int[] candidates, int target, int start,
            List<Integer> current, List<List<Integer>> result) {
        if (target == 0) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < candidates.length; i++) {
            // Early termination: if current candidate > target, skip rest
            if (candidates[i] > target)
                break;

            current.add(candidates[i]);
            backtrackOptimized(candidates, target - candidates[i], i, current, result);
            current.remove(current.size() - 1);
        }
    }

    // Solution 3: Dynamic Programming Approach
    public List<List<Integer>> combinationSum3(int[] candidates, int target) {
        // dp[i] stores all combinations that sum to i
        List<List<List<Integer>>> dp = new ArrayList<>();

        // Initialize dp array
        for (int i = 0; i <= target; i++) {
            dp.add(new ArrayList<>());
        }

        // Base case: empty combination sums to 0
        dp.get(0).add(new ArrayList<>());

        // Fill dp table
        for (int sum = 1; sum <= target; sum++) {
            for (int candidate : candidates) {
                if (candidate <= sum) {
                    // Add current candidate to all combinations that sum to (sum - candidate)
                    for (List<Integer> combination : dp.get(sum - candidate)) {
                        List<Integer> newCombination = new ArrayList<>(combination);
                        newCombination.add(candidate);
                        Collections.sort(newCombination); // Keep sorted to avoid duplicates

                        // Check if this combination already exists
                        boolean exists = false;
                        for (List<Integer> existing : dp.get(sum)) {
                            if (existing.equals(newCombination)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            dp.get(sum).add(newCombination);
                        }
                    }
                }
            }
        }

        return dp.get(target);
    }

    // Solution 4: Iterative BFS Approach
    public List<List<Integer>> combinationSum4(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Queue<CombinationState> queue = new LinkedList<>();

        // Start with empty combination
        queue.offer(new CombinationState(new ArrayList<>(), 0, 0));

        while (!queue.isEmpty()) {
            CombinationState current = queue.poll();

            if (current.sum == target) {
                result.add(new ArrayList<>(current.combination));
                continue;
            }

            if (current.sum > target) {
                continue;
            }

            // Try adding each candidate
            for (int i = current.startIndex; i < candidates.length; i++) {
                List<Integer> newCombination = new ArrayList<>(current.combination);
                newCombination.add(candidates[i]);
                queue.offer(new CombinationState(newCombination,
                        current.sum + candidates[i], i));
            }
        }

        return result;
    }

    // Helper class for BFS approach
    static class CombinationState {
        List<Integer> combination;
        int sum;
        int startIndex;

        CombinationState(List<Integer> combination, int sum, int startIndex) {
            this.combination = combination;
            this.sum = sum;
            this.startIndex = startIndex;
        }
    }

    // Solution 5: Recursive with Memoization
    public List<List<Integer>> combinationSum5(int[] candidates, int target) {
        Map<String, List<List<Integer>>> memo = new HashMap<>();
        Arrays.sort(candidates);
        return backtrackMemo(candidates, target, 0, memo);
    }

    private List<List<Integer>> backtrackMemo(int[] candidates, int target, int start,
            Map<String, List<List<Integer>>> memo) {
        String key = target + "," + start;
        if (memo.containsKey(key)) {
            return memo.get(key);
        }

        List<List<Integer>> result = new ArrayList<>();

        if (target == 0) {
            result.add(new ArrayList<>());
            memo.put(key, result);
            return result;
        }

        for (int i = start; i < candidates.length; i++) {
            if (candidates[i] > target)
                break;

            List<List<Integer>> subResults = backtrackMemo(candidates,
                    target - candidates[i], i, memo);

            for (List<Integer> subResult : subResults) {
                List<Integer> newCombination = new ArrayList<>();
                newCombination.add(candidates[i]);
                newCombination.addAll(subResult);
                result.add(newCombination);
            }
        }

        memo.put(key, result);
        return result;
    }

    // Solution 6: Using DFS with Path Tracking
    public List<List<Integer>> combinationSum6(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(candidates);
        dfs(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void dfs(int[] candidates, int target, int index,
            List<Integer> path, List<List<Integer>> result) {
        if (target == 0) {
            result.add(new ArrayList<>(path));
            return;
        }

        for (int i = index; i < candidates.length; i++) {
            if (candidates[i] > target)
                break;

            path.add(candidates[i]);
            dfs(candidates, target - candidates[i], i, path, result);
            path.remove(path.size() - 1);
        }
    }

    // Solution 7: Bottom-up DP with Set to avoid duplicates
    public List<List<Integer>> combinationSum7(int[] candidates, int target) {
        // Use array of sets to store unique combinations
        Set<List<Integer>>[] dp = new Set[target + 1];

        for (int i = 0; i <= target; i++) {
            dp[i] = new HashSet<>();
        }

        dp[0].add(new ArrayList<>());

        for (int sum = 1; sum <= target; sum++) {
            for (int candidate : candidates) {
                if (candidate <= sum) {
                    for (List<Integer> combination : dp[sum - candidate]) {
                        List<Integer> newCombination = new ArrayList<>(combination);
                        newCombination.add(candidate);
                        Collections.sort(newCombination);
                        dp[sum].add(newCombination);
                    }
                }
            }
        }

        return new ArrayList<>(dp[target]);
    }

    // Test method
    public static void main(String[] args) {
        CombinationSum cs = new CombinationSum();

        System.out.println("Input: candidates = [2,3,6,7], target = 7");
        System.out.println("Output: " + cs.combinationSum1(new int[] { 2, 3, 6, 7 }, 7));

        System.out.println("\nInput: candidates = [2,3,5], target = 8");
        System.out.println("Output: " + cs.combinationSum1(new int[] { 2, 3, 5 }, 8));

        System.out.println("\nInput: candidates = [2], target = 1");
        System.out.println("Output: " + cs.combinationSum1(new int[] { 2 }, 1));

        System.out.println("\nInput: candidates = [1,2], target = 4");
        System.out.println("Output: " + cs.combinationSum1(new int[] { 1, 2 }, 4));

        // Performance comparison
        System.out.println("\n=== Performance Comparison ===");
        int[] testCandidates = { 2, 3, 5, 7 };
        int testTarget = 15;
        long start, end;

        start = System.nanoTime();
        List<List<Integer>> result1 = cs.combinationSum1(testCandidates, testTarget);
        end = System.nanoTime();
        System.out.println("Basic Backtracking: " + result1.size() +
                " combinations in " + (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        List<List<Integer>> result2 = cs.combinationSum2(testCandidates, testTarget);
        end = System.nanoTime();
        System.out.println("Optimized Backtracking: " + result2.size() +
                " combinations in " + (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        List<List<Integer>> result5 = cs.combinationSum5(testCandidates, testTarget);
        end = System.nanoTime();
        System.out.println("Memoized Recursive: " + result5.size() +
                " combinations in " + (end - start) / 1000000.0 + " ms");
    }

}

/*
 * Complexity Analysis:
 * 
 * Time Complexity:
 * - Backtracking solutions: O(N^(T/M)) where N = number of candidates,
 * T = target value, M = minimal value among candidates
 * - DP solutions: O(T * N * result_size)
 * 
 * Space Complexity:
 * - Backtracking: O(T/M) for recursion depth + result storage
 * - DP: O(T * result_size) for storing all intermediate results
 * 
 * Solution Comparison:
 * 1. Classic Backtracking: Most intuitive, standard interview solution
 * 2. Optimized Backtracking: Better performance with sorting and pruning
 * 3. Dynamic Programming: Bottom-up approach, good for understanding DP
 * 4. BFS Iterative: Level-by-level exploration, easier to visualize
 * 5. Memoized Recursive: Combines recursion with caching for efficiency
 * 6. DFS Path Tracking: Similar to backtracking but with clearer path concept
 * 7. DP with Set: Avoids duplicates using HashSet
 * 
 * Key Insights:
 * - The key insight is allowing the same number to be used multiple times
 * - We pass the same index 'i' in recursive calls, not 'i+1'
 * - Sorting helps with early termination in optimized solutions
 * - Start index prevents duplicate combinations like [2,3] and [3,2]
 * 
 * Best for interviews: Solution 1 (Classic Backtracking) or Solution 2
 * (Optimized)
 * Most efficient: Solution 2 with sorting and pruning
 */
