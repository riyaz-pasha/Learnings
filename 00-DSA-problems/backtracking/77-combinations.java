import java.util.*;
/*
 * Given two integers n and k, return all possible combinations of k numbers
 * chosen from the range [1, n].
 * 
 * You may return the answer in any order.
 * 
 * Example 1:
 * Input: n = 4, k = 2
 * Output: [[1,2],[1,3],[1,4],[2,3],[2,4],[3,4]]
 * Explanation: There are 4 choose 2 = 6 total combinations.
 * Note that combinations are unordered, i.e., [1,2] and [2,1] are considered to
 * be the same combination.
 * 
 * Example 2:
 * Input: n = 1, k = 1
 * Output: [[1]]
 * Explanation: There is 1 choose 1 = 1 total combination.
 */

class Combinations {

    // Solution 1: Classic Backtracking (Most Common)
    public List<List<Integer>> combine1(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(result, new ArrayList<>(), 1, n, k);
        return result;
    }

    private void backtrack(List<List<Integer>> result, List<Integer> current,
            int start, int n, int k) {
        // Base case: if we have k numbers, add to result
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        // Try all numbers from start to n
        for (int i = start; i <= n; i++) {
            current.add(i);
            backtrack(result, current, i + 1, n, k);
            current.remove(current.size() - 1); // backtrack
        }
    }

    // Solution 2: Optimized Backtracking with Pruning
    public List<List<Integer>> combine2(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackOptimized(result, new ArrayList<>(), 1, n, k);
        return result;
    }

    private void backtrackOptimized(List<List<Integer>> result, List<Integer> current,
            int start, int n, int k) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        // Pruning: if remaining numbers aren't enough to complete combination
        int needed = k - current.size();
        int available = n - start + 1;

        for (int i = start; i <= n; i++) {
            // Skip if we don't have enough numbers left
            if (available < needed)
                break;

            current.add(i);
            backtrackOptimized(result, current, i + 1, n, k);
            current.remove(current.size() - 1);
            available--;
        }
    }

    // Solution 3: Iterative Approach
    public List<List<Integer>> combine3(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        if (k == 0 || n == 0 || k > n)
            return result;

        // Start with all single elements
        for (int i = 1; i <= n; i++) {
            List<Integer> combination = new ArrayList<>();
            combination.add(i);
            result.add(combination);
        }

        // Build combinations of size 2, 3, ..., k
        for (int size = 2; size <= k; size++) {
            List<List<Integer>> newResult = new ArrayList<>();

            for (List<Integer> combination : result) {
                int lastElement = combination.get(combination.size() - 1);

                // Add next possible elements
                for (int next = lastElement + 1; next <= n; next++) {
                    List<Integer> newCombination = new ArrayList<>(combination);
                    newCombination.add(next);
                    newResult.add(newCombination);
                }
            }
            result = newResult;
        }

        return result;
    }

    // Solution 4: Using Mathematical Approach (Lexicographic Order)
    public List<List<Integer>> combine4(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        if (k == 0 || n == 0 || k > n)
            return result;

        // Initialize first combination [1, 2, ..., k]
        List<Integer> combination = new ArrayList<>();
        for (int i = 1; i <= k; i++) {
            combination.add(i);
        }
        result.add(new ArrayList<>(combination));

        // Generate next combinations
        while (true) {
            // Find the rightmost element that can be incremented
            int i = k - 1;
            while (i >= 0 && combination.get(i) == n - k + i + 1) {
                i--;
            }

            // If no such element exists, we're done
            if (i < 0)
                break;

            // Increment the found element
            combination.set(i, combination.get(i) + 1);

            // Set subsequent elements
            for (int j = i + 1; j < k; j++) {
                combination.set(j, combination.get(i) + j - i);
            }

            result.add(new ArrayList<>(combination));
        }

        return result;
    }

    // Solution 5: Recursive with Choose Logic
    public List<List<Integer>> combine5(int n, int k) {
        if (k == 0) {
            List<List<Integer>> result = new ArrayList<>();
            result.add(new ArrayList<>());
            return result;
        }

        if (n == k) {
            List<List<Integer>> result = new ArrayList<>();
            List<Integer> combination = new ArrayList<>();
            for (int i = 1; i <= n; i++) {
                combination.add(i);
            }
            result.add(combination);
            return result;
        }

        // Either include n or don't include n
        // Include n: choose k-1 from [1, n-1]
        List<List<Integer>> withN = combine5(n - 1, k - 1);
        for (List<Integer> combination : withN) {
            combination.add(n);
        }

        // Don't include n: choose k from [1, n-1]
        List<List<Integer>> withoutN = combine5(n - 1, k);

        List<List<Integer>> result = new ArrayList<>();
        result.addAll(withN);
        result.addAll(withoutN);
        return result;
    }

    // Solution 6: Using BitSet for Binary Representation
    public List<List<Integer>> combine6(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();

        // Generate all possible k-bit combinations
        generateBinaryCombinations(n, k, 0, 0, new ArrayList<>(), result);
        return result;
    }

    private void generateBinaryCombinations(int n, int k, int pos, int count,
            List<Integer> current, List<List<Integer>> result) {
        if (count == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        if (pos > n)
            return;

        // Include current position
        current.add(pos + 1);
        generateBinaryCombinations(n, k, pos + 1, count + 1, current, result);
        current.remove(current.size() - 1);

        // Skip current position (only if we have enough remaining)
        if (n - pos > k - count) {
            generateBinaryCombinations(n, k, pos + 1, count, current, result);
        }
    }

    // Test method
    public static void main(String[] args) {
        Combinations c = new Combinations();

        System.out.println("Input: n = 4, k = 2");
        System.out.println("Output: " + c.combine1(4, 2));

        System.out.println("\nInput: n = 1, k = 1");
        System.out.println("Output: " + c.combine1(1, 1));

        System.out.println("\nInput: n = 5, k = 3");
        System.out.println("Output: " + c.combine1(5, 3));

        // Compare performance of different solutions
        System.out.println("\n=== Performance Comparison (n=10, k=5) ===");
        long start, end;

        start = System.nanoTime();
        List<List<Integer>> result1 = c.combine1(10, 5);
        end = System.nanoTime();
        System.out.println("Backtracking: " + result1.size() + " combinations in " +
                (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        List<List<Integer>> result2 = c.combine2(10, 5);
        end = System.nanoTime();
        System.out.println("Optimized Backtracking: " + result2.size() + " combinations in " +
                (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        List<List<Integer>> result4 = c.combine4(10, 5);
        end = System.nanoTime();
        System.out.println("Mathematical: " + result4.size() + " combinations in " +
                (end - start) / 1000000.0 + " ms");
    }

}

/*
 * Complexity Analysis:
 * 
 * Time Complexity:
 * - All solutions: O(C(n,k) * k) where C(n,k) = n!/(k!(n-k)!)
 * - The factor k comes from copying each combination to the result
 * 
 * Space Complexity:
 * - Result space: O(C(n,k) * k) to store all combinations
 * - Recursive space: O(k) for the recursion stack depth
 * 
 * Solution Comparison:
 * 1. Classic Backtracking: Most intuitive and commonly used in interviews
 * 2. Optimized Backtracking: Better performance with pruning
 * 3. Iterative: Good for understanding step-by-step building
 * 4. Mathematical: Generates combinations in lexicographic order
 * 5. Choose Logic: Demonstrates the mathematical choose concept
 * 6. Binary Representation: Alternative approach using bit manipulation
 * concepts
 * 
 * Best for interviews: Solution 1 (Classic Backtracking) or Solution 2
 * (Optimized)
 */
