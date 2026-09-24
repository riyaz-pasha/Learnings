/**
 * ============================================================================
 * COMBINATIONS (nCr) - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We have a bucket filled with unique numbers from 1 up to 'n'. We need to 
 * reach in and grab exactly 'k' numbers at a time. Because grabbing [1, 2] 
 * leaves us with the exact same handful as grabbing [2, 1], order does not 
 * matter. Our job is to write down every possible unique handful we can pull 
 * from the bucket.
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Can 'k' be greater than 'n'?
 * A: The constraints explicitly say 1 <= k <= n, so we don't have to worry 
 *    about this impossible scenario. If it were possible, we would just return 
 *    an empty list.
 * 
 * Q: Can we use a number more than once in the same combination?
 * A: No, we are choosing from the range [1, n] like distinct balls from a 
 *    bucket. No replacements.
 * 
 * Q: How do we prevent duplicate sets like [1, 2] and [2, 1]?
 * A: By strictly enforcing an order. We only ever pick numbers that are 
 *    GREATER than the last number we picked. If we start with 1, we can pick 
 *    2 next. But if we start with 2, we can never look back at 1.
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - DECISION TREE: This problem naturally forms a decision tree where at each 
 *   step, we choose the next number to add to our growing combination.
 * - BACKTRACKING: We travel down a path (e.g., 1 -> 2). Once our combination 
 *   hits size 'k', we record it. Then we take a step back (backtrack by 
 *   removing '2') and try the next path (e.g., 1 -> 3).
 * - PRUNING (OPTIMIZATION): If we are building a combination of size 4, but 
 *   we only have 1 number in our current list, and the remaining numbers in 
 *   the bucket are only [19, 20], we physically cannot finish building the 
 *   combination (1 + 2 = 3 total numbers, but we need 4). We should stop 
 *   searching this path immediately!
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Draw the decision tree for n=4, k=2 to show the interviewer you 
 *   understand how to avoid duplicates (always pick greater numbers).
 * - Step 2: Write out the standard Backtracking (DFS) solution. It's universally 
 *   recognized as the right way to solve this.
 * - Step 3: Stop and ask, "I can make this faster by pruning dead-end branches. 
 *   Would you like me to add that optimization?" (They will almost always say yes).
 * - Step 4: Add the pruning logic to the loop boundary.
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Input: n = 4, k = 2
 * Valid choices: [1, 2, 3, 4]
 * 
 * Decision Tree:
 *                      (empty)
 *           /         |         |      \
 *         [1]        [2]       [3]     [4]
 *        / | \       / \        |       | 
 *     [1,2][1,3][1,4][2,3][2,4][3,4]  (Dead end: can't reach size 2)
 * 
 * Result: [[1,2], [1,3], [1,4], [2,3], [2,4], [3,4]]
 */

import java.util.*;

class Combinations {

    /**
     * SOLUTION 1: Standard Backtracking
     * ------------------------------------------------------------------------
     * Pros: Classic, intuitive, and easy to read.
     * Cons: Explores some branches that are guaranteed to fail (e.g., starting
     * with 4 when we need k=3 elements).
     * 
     * Time Complexity: O(k * C(n, k)) where C(n, k) is the binomial coefficient.
     * We have C(n, k) total combinations, and copying each takes O(k) time.
     * Space Complexity: O(k) for the recursion stack and current combination list.
     */
    public List<List<Integer>> combineStandard(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(1, n, k, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int start, int n, int k, List<Integer> currentComb, List<List<Integer>> result) {
        // Base case: If the combination is of the required size, add it to the result
        if (currentComb.size() == k) {
            // Modern Java: List.copyOf creates an unmodifiable snapshot of the list
            result.add(List.copyOf(currentComb));
            return;
        }

        // Try adding each number from 'start' up to 'n'
        for (int i = start; i <= n; i++) {
            currentComb.add(i);                            // Choose
            backtrack(i + 1, n, k, currentComb, result);   // Explore (next number must be strictly greater)
            currentComb.remove(currentComb.size() - 1);    // Un-choose (Backtrack)
        }
    }

    /**
     * SOLUTION 2: Optimized Backtracking with Pruning
     * ------------------------------------------------------------------------
     * Pros: Fails fast. If the remaining numbers aren't enough to fill the 
     * required size 'k', it immediately halts the loop, saving significant time 
     * for large 'n'.
     * 
     * Time Complexity: O(k * C(n, k)). Same theoretical upper bound, but drastically 
     * smaller constant factor in practice.
     * Space Complexity: O(k) for the recursion stack.
     */
    public List<List<Integer>> combineOptimized(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackOptimized(1, n, k, new ArrayList<>(), result);
        return result;
    }

    private void backtrackOptimized(int start, int n, int k, List<Integer> currentComb, List<List<Integer>> result) {
        if (currentComb.size() == k) {
            result.add(List.copyOf(currentComb));
            return;
        }

        // PRUNING OPTIMIZATION: 
        // We currently have 'currentComb.size()' elements.
        // We need 'k - currentComb.size()' more elements.
        // The remaining pool of numbers is from 'i' to 'n'.
        // If 'n - i + 1' (available numbers) is strictly less than what we need, 
        // there is no point in continuing the loop.
        // Therefore, we only loop up to: n - (k - currentComb.size()) + 1
        
        int need = k - currentComb.size();
        int maxStart = n - need + 1;
        
        for (int i = start; i <= maxStart; i++) {
            currentComb.add(i);
            backtrackOptimized(i + 1, n, k, currentComb, result);
            currentComb.remove(currentComb.size() - 1);
        }
    }

    /**
     * UTILITY: Print combinations beautifully.
     */
    private static void printCombinations(List<List<Integer>> combs) {
        System.out.println("Total combinations: " + combs.size());
        for (List<Integer> comb : combs) {
            System.out.print(comb + " ");
        }
        System.out.println("\n");
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        Combinations solver = new Combinations();

        // Test Case 1: Standard case
        int n1 = 4, k1 = 2;
        System.out.println("--- Test Case 1: n=" + n1 + ", k=" + k1 + " ---");
        System.out.println("Standard Backtracking:");
        printCombinations(solver.combineStandard(n1, k1));
        System.out.println("Optimized Backtracking:");
        printCombinations(solver.combineOptimized(n1, k1));

        // Test Case 2: Edge case where k == n
        int n2 = 5, k2 = 5;
        System.out.println("--- Test Case 2: n=" + n2 + ", k=" + k2 + " ---");
        System.out.println("Standard Backtracking:");
        printCombinations(solver.combineStandard(n2, k2));
        System.out.println("Optimized Backtracking:");
        printCombinations(solver.combineOptimized(n2, k2));
        
        // Test Case 3: Larger constraint showing value of pruning
        // By running this, the optimized version bypasses hundreds of dead-end recursions.
        int n3 = 6, k3 = 4;
        System.out.println("--- Test Case 3: n=" + n3 + ", k=" + k3 + " ---");
        System.out.println("Optimized Backtracking:");
        printCombinations(solver.combineOptimized(n3, k3));
    }
}
