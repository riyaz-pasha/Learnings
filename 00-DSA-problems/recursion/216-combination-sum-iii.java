/*
 * Find all valid combinations of k numbers that sum up to n such that the
 * following conditions are true:
 * 
 * Only numbers 1 through 9 are used.
 * Each number is used at most once.
 * Return a list of all possible valid combinations. The list must not contain
 * the same combination twice, and the combinations may be returned in any
 * order.
 * 
 * Example 1:
 * 
 * Input: k = 3, n = 7
 * Output: [[1,2,4]]
 * Explanation:
 * 1 + 2 + 4 = 7
 * There are no other valid combinations.
 * Example 2:
 * 
 * Input: k = 3, n = 9
 * Output: [[1,2,6],[1,3,5],[2,3,4]]
 * Explanation:
 * 1 + 2 + 6 = 9
 * 1 + 3 + 5 = 9
 * 2 + 3 + 4 = 9
 * There are no other valid combinations.
 * Example 3:
 * 
 * Input: k = 4, n = 1
 * Output: []
 * Explanation: There are no valid combinations.
 * Using 4 different numbers in the range [1,9], the smallest sum we can get is
 * 1+2+3+4 = 10 and since 10 > 1, there are no valid combination.
 */

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class CombinationSumIII {

    // Approach 1: Backtracking (Optimal)
    // Time: O(C(9,k)), Space: O(k)
    public List<List<Integer>> combinationSum3_1(int k, int n) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(k, n, 1, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int k, int remaining, int start,
            List<Integer> current, List<List<Integer>> result) {
        // Base case: found valid combination
        if (current.size() == k && remaining == 0) {
            result.add(new ArrayList<>(current));
            return;
        }

        // Pruning: if we already have k numbers or remaining is negative
        if (current.size() == k || remaining < 0) {
            return;
        }

        // Try numbers from start to 9
        for (int i = start; i <= 9; i++) {
            // Pruning: if current number is already larger than remaining
            if (i > remaining) {
                break;
            }

            current.add(i);
            backtrack(k, remaining - i, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    // Approach 2: Backtracking with Early Termination
    // Time: O(C(9,k)), Space: O(k)
    public List<List<Integer>> combinationSum3_2(int k, int n) {
        List<List<Integer>> result = new ArrayList<>();

        // Early pruning: minimum sum with k numbers is 1+2+...+k
        int minSum = k * (k + 1) / 2;
        // Maximum sum with k numbers is (10-k)+...+9
        int maxSum = k * (19 - k) / 2;

        if (n < minSum || n > maxSum) {
            return result;
        }

        backtrackOptimized(k, n, 1, new ArrayList<>(), result);
        return result;
    }

    private void backtrackOptimized(int k, int remaining, int start,
            List<Integer> current, List<List<Integer>> result) {
        if (current.size() == k) {
            if (remaining == 0) {
                result.add(new ArrayList<>(current));
            }
            return;
        }

        // Calculate how many more numbers we need
        int needed = k - current.size();

        for (int i = start; i <= 9; i++) {
            // Pruning: remaining too small
            if (i > remaining) {
                break;
            }

            // Pruning: even if we use largest remaining numbers, can't reach target
            // Sum of largest 'needed' numbers starting from i+1
            int maxPossible = 0;
            for (int j = 0; j < needed; j++) {
                maxPossible += (9 - j);
            }
            if (i + maxPossible < remaining) {
                continue;
            }

            current.add(i);
            backtrackOptimized(k, remaining - i, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    // Approach 3: Bit Manipulation
    // Time: O(2^9 * 9), Space: O(k)
    public List<List<Integer>> combinationSum3_3(int k, int n) {
        List<List<Integer>> result = new ArrayList<>();

        // Try all possible combinations using bitmask
        for (int mask = 0; mask < (1 << 9); mask++) {
            if (Integer.bitCount(mask) == k) {
                List<Integer> combination = new ArrayList<>();
                int sum = 0;

                for (int i = 0; i < 9; i++) {
                    if ((mask & (1 << i)) != 0) {
                        combination.add(i + 1);
                        sum += (i + 1);
                    }
                }

                if (sum == n) {
                    result.add(combination);
                }
            }
        }

        return result;
    }

    // Approach 4: Iterative with Queue
    // Time: O(C(9,k)), Space: O(C(9,k))
    public List<List<Integer>> combinationSum3_4(int k, int n) {
        List<List<Integer>> result = new ArrayList<>();
        Queue<State> queue = new LinkedList<>();

        // Start with each number from 1 to 9
        for (int i = 1; i <= 9 && i <= n; i++) {
            List<Integer> initial = new ArrayList<>();
            initial.add(i);
            queue.offer(new State(initial, n - i, i + 1));
        }

        while (!queue.isEmpty()) {
            State state = queue.poll();

            if (state.combination.size() == k) {
                if (state.remaining == 0) {
                    result.add(state.combination);
                }
                continue;
            }

            // Try adding more numbers
            for (int i = state.nextStart; i <= 9 && i <= state.remaining; i++) {
                List<Integer> newComb = new ArrayList<>(state.combination);
                newComb.add(i);
                queue.offer(new State(newComb, state.remaining - i, i + 1));
            }
        }

        return result;
    }

    static class State {
        List<Integer> combination;
        int remaining;
        int nextStart;

        State(List<Integer> combination, int remaining, int nextStart) {
            this.combination = combination;
            this.remaining = remaining;
            this.nextStart = nextStart;
        }
    }

    // Approach 5: Mathematical Pruning
    // Time: O(C(9,k)), Space: O(k)
    public List<List<Integer>> combinationSum3_5(int k, int n) {
        List<List<Integer>> result = new ArrayList<>();

        // Minimum possible sum: 1+2+...+k
        int minSum = k * (k + 1) / 2;
        // Maximum possible sum: (10-k)+(11-k)+...+9
        int maxSum = (9 + 10 - k) * k / 2;

        if (n < minSum || n > maxSum) {
            return result;
        }

        dfs(k, n, 1, new ArrayList<>(), result);
        return result;
    }

    private void dfs(int k, int target, int start,
            List<Integer> path, List<List<Integer>> result) {
        if (path.size() == k) {
            if (target == 0) {
                result.add(new ArrayList<>(path));
            }
            return;
        }

        for (int i = start; i <= 9; i++) {
            if (i > target)
                break;

            path.add(i);
            dfs(k, target - i, i + 1, path, result);
            path.remove(path.size() - 1);
        }
    }

    // Test cases with detailed visualization
    public static void main(String[] args) {
        CombinationSumIII solution = new CombinationSumIII();

        // Test Case 1
        int k1 = 3, n1 = 7;
        List<List<Integer>> result1 = solution.combinationSum3_1(k1, n1);
        System.out.println("Test 1: k=" + k1 + ", n=" + n1);
        System.out.println("Output: " + result1); // [[1,2,4]]
        visualizeCombinations(k1, n1, result1);

        // Test Case 2
        int k2 = 3, n2 = 9;
        List<List<Integer>> result2 = solution.combinationSum3_1(k2, n2);
        System.out.println("\nTest 2: k=" + k2 + ", n=" + n2);
        System.out.println("Output: " + result2); // [[1,2,6],[1,3,5],[2,3,4]]
        visualizeCombinations(k2, n2, result2);

        // Test Case 3
        int k3 = 4, n3 = 1;
        List<List<Integer>> result3 = solution.combinationSum3_1(k3, n3);
        System.out.println("\nTest 3: k=" + k3 + ", n=" + n3);
        System.out.println("Output: " + result3); // []

        // Test Case 4: Maximum k
        int k4 = 9, n4 = 45;
        List<List<Integer>> result4 = solution.combinationSum3_1(k4, n4);
        System.out.println("\nTest 4: k=" + k4 + ", n=" + n4);
        System.out.println("Output: " + result4); // [[1,2,3,4,5,6,7,8,9]]

        // Test Case 5: No solution
        int k5 = 3, n5 = 30;
        List<List<Integer>> result5 = solution.combinationSum3_1(k5, n5);
        System.out.println("\nTest 5: k=" + k5 + ", n=" + n5);
        System.out.println("Output: " + result5); // []

        // Compare all approaches
        System.out.println("\nComparing approaches for k=3, n=9:");
        System.out.println("Approach 1: " + solution.combinationSum3_1(k2, n2));
        System.out.println("Approach 2: " + solution.combinationSum3_2(k2, n2));
        System.out.println("Approach 3: " + solution.combinationSum3_3(k2, n2));
        System.out.println("Approach 4: " + solution.combinationSum3_4(k2, n2));
        System.out.println("Approach 5: " + solution.combinationSum3_5(k2, n2));
    }

    private static void visualizeCombinations(int k, int n,
            List<List<Integer>> combinations) {
        if (combinations.isEmpty()) {
            System.out.println("No valid combinations found.");

            // Explain why
            int minSum = k * (k + 1) / 2;
            int maxSum = (9 + 10 - k) * k / 2;
            System.out.println("Minimum possible sum with k=" + k + ": " + minSum);
            System.out.println("Maximum possible sum with k=" + k + ": " + maxSum);
            System.out.println("Target n=" + n + " is out of range.");
            return;
        }

        System.out.println("Found " + combinations.size() + " combination(s):");
        for (List<Integer> combo : combinations) {
            int sum = combo.stream().mapToInt(Integer::intValue).sum();
            System.out.println("  " + combo + " → sum = " + sum);
        }

        // Show decision tree for first combination
        if (!combinations.isEmpty()) {
            System.out.println("\nDecision tree for finding " + combinations.get(0) + ":");
            List<Integer> first = combinations.get(0);
            System.out.print("[] → ");
            int sum = 0;
            for (int i = 0; i < first.size(); i++) {
                sum += first.get(i);
                System.out.print("[" + String.join(",",
                        first.subList(0, i + 1).stream()
                                .map(String::valueOf)
                                .toArray(String[]::new))
                        + "]");
                if (i < first.size() - 1) {
                    System.out.print(" → ");
                }
            }
            System.out.println(" (sum=" + sum + ")");
        }
    }

    class Solution {

        public List<List<Integer>> combinationSum3(int k, int n) {
            List<List<Integer>> result = new ArrayList<>();
            backtrack(result, n, k, 0, new ArrayList<>());
            return result;
        }

        private void backtrack(List<List<Integer>> result, int target, int count,
                int lastNum, List<Integer> current) {

            if (count == 0 && target == 0) {
                result.add(new ArrayList<>(current));
                return;
            }

            if (count < 0 || target < 0) {
                return;
            }

            for (int i = lastNum + 1; i <= 9; i++) {
                current.add(i);
                backtrack(result, target - i, count - 1, i, current);
                current.remove(current.size() - 1); // FIX
            }
        }

    }

}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM CONSTRAINTS:
 * 1. Use numbers 1-9 only
 * 2. Each number used at most once
 * 3. Need exactly k numbers
 * 4. Sum must equal n
 * 
 * KEY INSIGHTS:
 * 
 * 1. Minimum possible sum with k numbers: 1+2+...+k = k*(k+1)/2
 * 2. Maximum possible sum with k numbers: (10-k)+...+9 = k*(19-k)/2
 * 3. If n is outside this range, no solution exists
 * 4. Use backtracking to explore all valid combinations
 * 
 * BACKTRACKING APPROACH:
 * 
 * Algorithm:
 * 1. Start with empty combination
 * 2. Try adding numbers from 1 to 9 in order
 * 3. Recursive constraints:
 * - Can only add number if: current.size() < k
 * - Can only add number if: number <= remaining sum
 * - Skip to next number to avoid duplicates (use start pointer)
 * 4. Base case: size == k and remaining == 0
 * 
 * Decision Tree for k=3, n=9:
 * 
 * []
 * / | | \ ...
 * [1] [2] [3] [4]
 * / | \ / \ / \
 * [1,2][1,3]... [2,3][2,4]...
 * 
 * Continue until size = 3:
 * [1,2,6]: 1+2+6=9 ✓
 * [1,3,5]: 1+3+5=9 ✓
 * [2,3,4]: 2+3+4=9 ✓
 * 
 * EXAMPLE WALKTHROUGH: k=3, n=7
 * 
 * Start: current=[], remaining=7, start=1
 * 
 * Try i=1:
 * current=[1], remaining=6, start=2
 * Try i=2:
 * current=[1,2], remaining=4, start=3
 * Try i=3:
 * current=[1,2,3], size=3, remaining=1 → not 0, backtrack
 * Try i=4:
 * current=[1,2,4], size=3, remaining=0 → Found! Add [1,2,4]
 * Try i=5: 5 > 0, break
 * Try i=3:
 * current=[1,3], remaining=3, start=4
 * No valid combinations
 * ...
 * 
 * Result: [[1,2,4]]
 * 
 * PRUNING OPTIMIZATIONS:
 * 
 * 1. Early termination: if i > remaining, break
 * - No point trying larger numbers
 * 
 * 2. Check bounds: if n < minSum or n > maxSum, return empty
 * 
 * 3. Mathematical pruning:
 * - If even with largest remaining numbers we can't reach target
 * - Skip this branch
 * 
 * 4. Size check: if current.size() == k, check sum immediately
 * 
 * EXAMPLE 2: k=3, n=9
 * 
 * Valid combinations:
 * - [1,2,6]: 1+2+6 = 9 ✓
 * - [1,3,5]: 1+3+5 = 9 ✓
 * - [2,3,4]: 2+3+4 = 9 ✓
 * 
 * Why not [1,4,4]? Can't use 4 twice!
 * Why not [3,3,3]? Can't use 3 thrice!
 * 
 * COMPLEXITY ANALYSIS:
 * 
 * Time Complexity: O(C(9,k) * k)
 * - C(9,k) = number of ways to choose k items from 9
 * - For each combination, we do O(k) work
 * - Worst case k=4 or k=5: C(9,4) = 126 combinations
 * 
 * Space Complexity: O(k)
 * - Recursion depth: k levels
 * - Current path: k numbers
 * - Result list not counted (output space)
 * 
 * EDGE CASES:
 * 
 * 1. Impossible (too small): k=4, n=1
 * - Min sum = 1+2+3+4 = 10 > 1
 * 
 * 2. Impossible (too large): k=3, n=30
 * - Max sum = 7+8+9 = 24 < 30
 * 
 * 3. Exact minimum: k=3, n=6
 * - Result: [[1,2,3]]
 * 
 * 4. Exact maximum: k=3, n=24
 * - Result: [[7,8,9]]
 * 
 * 5. Use all numbers: k=9, n=45
 * - Result: [[1,2,3,4,5,6,7,8,9]]
 * 
 * COMPARISON WITH OTHER COMBINATION PROBLEMS:
 * 
 * Combination Sum I:
 * - Can reuse numbers
 * - Any count allowed
 * - Unlimited usage
 * 
 * Combination Sum II:
 * - Each element used once
 * - Input has duplicates
 * - Need to handle duplicates
 * 
 * Combination Sum III (This):
 * - Numbers 1-9 only
 * - Exactly k numbers
 * - Each used once
 * - No input array (implicit 1-9)
 * 
 * PRACTICAL APPLICATIONS:
 * 
 * 1. Number puzzles and games
 * 2. Subset selection with constraints
 * 3. Resource allocation with limits
 * 4. Combination locks with rules
 * 5. Mathematical problem solving
 * 
 * INTERVIEW TIPS:
 * 
 * 1. Clarify constraints: exactly k numbers, range 1-9
 * 2. Mention pruning: check bounds before recursion
 * 3. Explain why we use start pointer (avoid duplicates)
 * 4. Calculate min/max possible sums
 * 5. Draw decision tree for clarity
 * 6. Discuss time complexity: C(9,k)
 * 
 * COMMON MISTAKES:
 * 
 * 1. Not using start pointer → generates duplicates
 * 2. Not checking remaining < 0 → unnecessary recursion
 * 3. Forgetting to create new ArrayList when adding to result
 * 4. Not pruning with i > remaining
 * 5. Off-by-one errors in loop bounds
 * 
 * OPTIMIZATION TRICKS:
 * 
 * 1. Break early if i > remaining
 * 2. Check min/max sums before starting
 * 3. Use mathematical bounds for pruning
 * 4. Short-circuit when size reaches k
 * 5. Reverse iteration for some cases
 */
