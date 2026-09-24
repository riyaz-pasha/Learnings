import java.util.*;

class CombinationSumSolver {

    // Approach 1: Backtracking - OPTIMAL
    // Time: O(N^(T/M)), Space: O(T/M) where N=candidates, T=target,
    // M=min(candidates)
    public List<List<Integer>> combinationSum(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int[] candidates, int remaining, int start,
            List<Integer> current, List<List<Integer>> result) {
        // Base case: found a valid combination
        if (remaining == 0) {
            result.add(new ArrayList<>(current));
            return;
        }

        // Base case: overshot the target
        if (remaining < 0) {
            return;
        }

        // Try each candidate starting from 'start' index
        for (int i = start; i < candidates.length; i++) {
            current.add(candidates[i]); // Choose

            // Explore: same index 'i' allows reusing same element
            backtrack(candidates, remaining - candidates[i], i, current, result);

            current.remove(current.size() - 1); // Unchoose (backtrack)
        }
    }

    // Approach 2: Backtracking with Sorting (slight optimization)
    // Time: O(N log N + N^(T/M)), Space: O(T/M)
    public List<List<Integer>> combinationSumOptimized(int[] candidates, int target) {
        Arrays.sort(candidates); // Sort to enable early termination
        List<List<Integer>> result = new ArrayList<>();
        backtrackOptimized(candidates, target, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrackOptimized(int[] candidates, int remaining, int start,
            List<Integer> current, List<List<Integer>> result) {
        if (remaining == 0) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < candidates.length; i++) {
            // Early termination: if current candidate > remaining, no point continuing
            if (candidates[i] > remaining) {
                break; // Since sorted, all following will also be too large
            }

            current.add(candidates[i]);
            backtrackOptimized(candidates, remaining - candidates[i], i, current, result);
            current.remove(current.size() - 1);
        }
    }

    // Approach 3: Dynamic Programming (for understanding)
    // Time: O(N × T), Space: O(T)
    // Note: This counts combinations but doesn't generate them easily
    public int countCombinations(int[] candidates, int target) {
        int[] dp = new int[target + 1];
        dp[0] = 1; // One way to make 0: use nothing

        // For each candidate
        for (int candidate : candidates) {
            // Update all reachable sums
            for (int i = candidate; i <= target; i++) {
                dp[i] += dp[i - candidate];
            }
        }

        return dp[target];
    }

    // Approach 4: Backtracking with early pruning and sum tracking
    // Time: O(N^(T/M)), Space: O(T/M)
    public List<List<Integer>> combinationSumWithSum(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackWithSum(candidates, target, 0, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrackWithSum(int[] candidates, int target, int start, int sum,
            List<Integer> current, List<List<Integer>> result) {
        if (sum == target) {
            result.add(new ArrayList<>(current));
            return;
        }

        if (sum > target) {
            return; // Pruning
        }

        for (int i = start; i < candidates.length; i++) {
            current.add(candidates[i]);
            backtrackWithSum(candidates, target, i, sum + candidates[i], current, result);
            current.remove(current.size() - 1);
        }
    }

    public List<List<Integer>> combinationSum4(int[] candidates, int target) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(candidates, result, 0, target, new ArrayList<>());
        return result;
    }

    private void backtrack(int[] candidates, List<List<Integer>> result, int index, int target, List<Integer> current) {
        if (target == 0) {
            result.add(new ArrayList<>(current));
            return;
        }

        if (index == candidates.length || target < 0) {
            return;
        }

        // skip
        backtrack(candidates, result, index + 1, target, current);

        // pick
        current.add(candidates[index]);
        backtrack(candidates, result, index, target - candidates[index], current);
        current.removeLast();
    }
}

// Test and demonstration class
class CombinationSumTester {

    public static void main(String[] args) {
        CombinationSumSolver solver = new CombinationSumSolver();

        System.out.println("=== Combination Sum ===\n");

        // Example 1: candidates = [2,3,6,7], target = 7
        System.out.println("Example 1:");
        int[] candidates1 = { 2, 3, 6, 7 };
        int target1 = 7;
        System.out.println("Input: candidates = " + Arrays.toString(candidates1) + ", target = " + target1);
        List<List<Integer>> result1 = solver.combinationSum(candidates1, target1);
        System.out.println("Output: " + result1);
        System.out.println("Explanation:");
        System.out.println("  2 + 2 + 3 = 7 (2 can be used multiple times)");
        System.out.println("  7 = 7");
        System.out.println("  These are the only two combinations.\n");

        // Example 2: candidates = [2,3,5], target = 8
        System.out.println("Example 2:");
        int[] candidates2 = { 2, 3, 5 };
        int target2 = 8;
        System.out.println("Input: candidates = " + Arrays.toString(candidates2) + ", target = " + target2);
        List<List<Integer>> result2 = solver.combinationSum(candidates2, target2);
        System.out.println("Output: " + result2);
        System.out.println("Explanation:");
        System.out.println("  2 + 2 + 2 + 2 = 8");
        System.out.println("  2 + 3 + 3 = 8");
        System.out.println("  3 + 5 = 8\n");

        // Example 3: candidates = [2], target = 1
        System.out.println("Example 3:");
        int[] candidates3 = { 2 };
        int target3 = 1;
        System.out.println("Input: candidates = " + Arrays.toString(candidates3) + ", target = " + target3);
        List<List<Integer>> result3 = solver.combinationSum(candidates3, target3);
        System.out.println("Output: " + result3);
        System.out.println("Explanation: No combination can sum to 1 using only 2.\n");

        System.out.println("=== Additional Examples ===\n");

        // Example 4: Multiple ways
        System.out.println("Example 4: candidates = [2,3,5], target = 5");
        int[] candidates4 = { 2, 3, 5 };
        int target4 = 5;
        List<List<Integer>> result4 = solver.combinationSum(candidates4, target4);
        System.out.println("Output: " + result4);
        System.out.println();

        // Example 5: Large target
        System.out.println("Example 5: candidates = [2,3,5], target = 10");
        int[] candidates5 = { 2, 3, 5 };
        int target5 = 10;
        List<List<Integer>> result5 = solver.combinationSum(candidates5, target5);
        System.out.println("Output: " + result5);
        System.out.println("Count: " + result5.size() + " combinations\n");

        System.out.println("=== Algorithm Explanation ===\n");

        System.out.println("Approach: Backtracking with Repetition Allowed");
        System.out.println("----------------------------------------------");
        System.out.println();
        System.out.println("Key Insight: Elements can be reused unlimited times!");
        System.out.println();
        System.out.println("Difference from regular combinations:");
        System.out.println("  Regular: Each element used at most once");
        System.out.println("  This problem: Each element can be used multiple times");
        System.out.println();
        System.out.println("How to handle repetition:");
        System.out.println("  When we choose candidates[i], we recursively call with:");
        System.out.println("    - Same starting index 'i' (allows reusing same element)");
        System.out.println("    - NOT 'i+1' (which would prevent reuse)");
        System.out.println();

        System.out.println("Decision Tree for candidates=[2,3], target=5:");
        System.out.println();
        System.out.println("                      []");
        System.out.println("                    /    \\");
        System.out.println("                  [2]     [3]");
        System.out.println("                 / \\       \\");
        System.out.println("              [2,2] [2,3]  [3,3]");
        System.out.println("              /       ✓      X");
        System.out.println("          [2,2,2]");
        System.out.println("             X");
        System.out.println();
        System.out.println("Paths that reach target 5:");
        System.out.println("  • [2,3]: 2+3=5 ✓");
        System.out.println();
        System.out.println("Pruned branches (sum > target):");
        System.out.println("  • [2,2,2]: 2+2+2=6 > 5 ✗");
        System.out.println("  • [3,3]: 3+3=6 > 5 ✗");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Code Walkthrough for [2,3,6,7], target=7:");
        System.out.println("------------------------------------------");
        System.out.println();
        System.out.println("Start: backtrack([], remaining=7, start=0)");
        System.out.println();
        System.out.println("Try candidate 2:");
        System.out.println("  [2] → backtrack([2], remaining=5, start=0)");
        System.out.println("    Try 2 again:");
        System.out.println("      [2,2] → backtrack([2,2], remaining=3, start=0)");
        System.out.println("        Try 2 again:");
        System.out.println("          [2,2,2] → backtrack([2,2,2], remaining=1, start=0)");
        System.out.println("            All too large, backtrack");
        System.out.println("        Try 3:");
        System.out.println("          [2,2,3] → backtrack([2,2,3], remaining=0, start=1) ✓");
        System.out.println("          Found solution: [2,2,3]");
        System.out.println();
        System.out.println("...continue exploring other branches...");
        System.out.println();
        System.out.println("Try candidate 7:");
        System.out.println("  [7] → backtrack([7], remaining=0, start=3) ✓");
        System.out.println("  Found solution: [7]");
        System.out.println();
        System.out.println("Final result: [[2,2,3], [7]]");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Why start at index 'i' instead of 'i+1'?");
        System.out.println("----------------------------------------");
        System.out.println();
        System.out.println("Example: candidates = [2,3], target = 7");
        System.out.println();
        System.out.println("If we use 'i+1' (no repetition):");
        System.out.println("  [2] → can only try [3,6,7] next");
        System.out.println("  Can't get [2,2,3] ✗");
        System.out.println();
        System.out.println("If we use 'i' (allow repetition):");
        System.out.println("  [2] → can try [2,3,6,7] next");
        System.out.println("  Can get [2,2,3] ✓");
        System.out.println();
        System.out.println("Key: Passing 'i' (not 'i+1') allows reusing current element!");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Avoiding Duplicates:");
        System.out.println("--------------------");
        System.out.println("Why don't we get duplicate combinations like [2,3] and [3,2]?");
        System.out.println();
        System.out.println("Because we always process candidates in order!");
        System.out.println("  • Once we move past candidate[i], we never go back");
        System.out.println("  • This ensures [2,3] is generated, but [3,2] is not");
        System.out.println("  • The 'start' parameter maintains this ordering");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Time Complexity Analysis:");
        System.out.println("------------------------");
        System.out.println("Time: O(N^(T/M))");
        System.out.println("  N = number of candidates");
        System.out.println("  T = target value");
        System.out.println("  M = minimum value in candidates");
        System.out.println();
        System.out.println("Explanation:");
        System.out.println("  • Maximum depth of recursion tree = T/M");
        System.out.println("  • At each level, we branch up to N times");
        System.out.println("  • Total nodes ≈ N^(T/M)");
        System.out.println();
        System.out.println("Example: candidates=[2,3], target=8");
        System.out.println("  N=2, T=8, M=2");
        System.out.println("  Max depth = 8/2 = 4");
        System.out.println("  Worst case: 2^4 = 16 nodes");
        System.out.println();
        System.out.println("Space: O(T/M) for recursion stack");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Optimization: Sorting for Early Termination");
        System.out.println("-------------------------------------------");
        System.out.println("If we sort candidates first:");
        System.out.println("  [7, 6, 3, 2] → [2, 3, 6, 7]");
        System.out.println();
        System.out.println("When candidate[i] > remaining:");
        System.out.println("  • We can BREAK (not just continue)");
        System.out.println("  • All following candidates will also be too large");
        System.out.println("  • Saves unnecessary recursive calls");
        System.out.println();
        System.out.println("Trade-off:");
        System.out.println("  Cost: O(N log N) for sorting");
        System.out.println("  Benefit: Prunes search tree earlier");
        System.out.println("  Usually worth it for larger inputs!");
        System.out.println();

        System.out.println("=== Comparison with Related Problems ===\n");

        System.out.println("Problem Variants:");
        System.out.println("-----------------");
        System.out.println();
        System.out.println("1. Combination Sum (this problem):");
        System.out.println("   • Elements can be reused unlimited times");
        System.out.println("   • Pass same index 'i' in recursion");
        System.out.println();
        System.out.println("2. Combination Sum II:");
        System.out.println("   • Each element used at most once");
        System.out.println("   • Pass 'i+1' in recursion");
        System.out.println("   • Need to handle duplicate elements");
        System.out.println();
        System.out.println("3. Combination Sum III:");
        System.out.println("   • Use exactly k numbers");
        System.out.println("   • Numbers from 1-9 only");
        System.out.println("   • Each number used at most once");
        System.out.println();

        System.out.println("=== Testing Different Approaches ===\n");

        int[] testCand = { 2, 3, 5 };
        int testTarget = 8;

        System.out.println("Input: candidates = " + Arrays.toString(testCand) + ", target = " + testTarget);
        System.out.println();

        long start, end;

        start = System.nanoTime();
        List<List<Integer>> res1 = solver.combinationSum(testCand, testTarget);
        end = System.nanoTime();
        System.out.println("Standard Backtracking: " + res1);
        System.out.printf("Time: %.3f ms\n\n", (end - start) / 1_000_000.0);

        start = System.nanoTime();
        List<List<Integer>> res2 = solver.combinationSumOptimized(testCand, testTarget);
        end = System.nanoTime();
        System.out.println("Optimized (with sorting): " + res2);
        System.out.printf("Time: %.3f ms\n\n", (end - start) / 1_000_000.0);

        int count = solver.countCombinations(testCand, testTarget);
        System.out.println("DP Count (verification): " + count + " combinations");
    }
}
