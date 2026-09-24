import java.util.*;

class SubsetGenerator {

    // Approach 1: Backtracking - MOST INTUITIVE
    // Time: O(n × 2^n), Space: O(n) recursion depth
    public List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrack(nums, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int[] nums, int start, List<Integer> current,
            List<List<Integer>> result) {
        // Add current subset to result (every path is valid!)
        result.add(new ArrayList<>(current));

        // Try adding each remaining element
        for (int i = start; i < nums.length; i++) {
            current.add(nums[i]); // Choose
            backtrack(nums, i + 1, current, result); // Explore
            current.remove(current.size() - 1); // Unchoose (backtrack)
        }
    }

    // Approach 2: Bit Manipulation - MOST CLEVER
    // Time: O(n × 2^n), Space: O(1) excluding output
    public List<List<Integer>> subsetsUsingBits(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        int n = nums.length;
        int totalSubsets = 1 << n; // 2^n subsets

        // Generate all numbers from 0 to 2^n - 1
        for (int mask = 0; mask < totalSubsets; mask++) {
            List<Integer> subset = new ArrayList<>();

            // Check each bit position
            for (int i = 0; i < n; i++) {
                // If ith bit is set, include nums[i]
                if ((mask & (1 << i)) != 0) {
                    subset.add(nums[i]);
                }
            }

            result.add(subset);
        }

        return result;
    }

    // Approach 3: Iterative (Cascading)
    // Time: O(n × 2^n), Space: O(1) excluding output
    public List<List<Integer>> subsetsIterative(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        result.add(new ArrayList<>()); // Start with empty subset

        // For each number, add it to all existing subsets
        for (int num : nums) {
            int size = result.size();
            for (int i = 0; i < size; i++) {
                // Create new subset by adding current number to existing subset
                List<Integer> newSubset = new ArrayList<>(result.get(i));
                newSubset.add(num);
                result.add(newSubset);
            }
        }

        return result;
    }

    // Approach 4: Lexicographic (Binary Sorted) Subsets
    // Time: O(n × 2^n), Space: O(n)
    public List<List<Integer>> subsetsLexicographic(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        int n = nums.length;

        for (int k = 0; k <= n; k++) {
            // Generate all subsets of size k
            generateCombinations(nums, k, 0, new ArrayList<>(), result);
        }

        return result;
    }

    private void generateCombinations(int[] nums, int k, int start,
            List<Integer> current, List<List<Integer>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < nums.length; i++) {
            current.add(nums[i]);
            generateCombinations(nums, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}

// Test and demonstration class
class SubsetTester {

    public static void main(String[] args) {
        SubsetGenerator generator = new SubsetGenerator();

        System.out.println("=== Generate All Subsets (Power Set) ===\n");

        // Example 1: [1,2,3]
        System.out.println("Example 1: nums = [1,2,3]");
        int[] nums1 = { 1, 2, 3 };
        List<List<Integer>> result1 = generator.subsets(nums1);
        System.out.println("Output: " + result1);
        System.out.println("Count: " + result1.size() + " (2^3 = 8)");
        System.out.println();

        // Example 2: [0]
        System.out.println("Example 2: nums = [0]");
        int[] nums2 = { 0 };
        List<List<Integer>> result2 = generator.subsets(nums2);
        System.out.println("Output: " + result2);
        System.out.println("Count: " + result2.size() + " (2^1 = 2)");
        System.out.println();

        // Example 3: [1,2,3,4]
        System.out.println("Example 3: nums = [1,2,3,4]");
        int[] nums3 = { 1, 2, 3, 4 };
        List<List<Integer>> result3 = generator.subsets(nums3);
        System.out.println("Count: " + result3.size() + " (2^4 = 16)");
        System.out.println("First 8 subsets: " + result3.subList(0, 8));
        System.out.println();

        System.out.println("=== Testing All Approaches ===\n");

        int[] test = { 1, 2, 3 };
        System.out.println("Input: [1,2,3]\n");

        System.out.println("1. Backtracking:");
        System.out.println(generator.subsets(test));
        System.out.println();

        System.out.println("2. Bit Manipulation:");
        System.out.println(generator.subsetsUsingBits(test));
        System.out.println();

        System.out.println("3. Iterative (Cascading):");
        System.out.println(generator.subsetsIterative(test));
        System.out.println();

        System.out.println("4. Lexicographic:");
        System.out.println(generator.subsetsLexicographic(test));
        System.out.println();

        System.out.println("=== Algorithm Explanation ===\n");

        System.out.println("Approach 1: Backtracking (Decision Tree)");
        System.out.println("Time: O(n × 2^n), Space: O(n) recursion");
        System.out.println();
        System.out.println("Key insight: For each element, we make a choice:");
        System.out.println("  - Include it in the current subset, OR");
        System.out.println("  - Skip it and move to next element");
        System.out.println();
        System.out.println("Decision tree for [1,2,3]:");
        System.out.println();
        System.out.println("                        []");
        System.out.println("                    /        \\");
        System.out.println("                  [1]         []");
        System.out.println("                /    \\       /    \\");
        System.out.println("            [1,2]   [1]    [2]    []");
        System.out.println("            /  \\    /  \\   /  \\   /  \\");
        System.out.println("        [1,2,3] [1,2] [1,3] [1] [2,3] [2] [3] []");
        System.out.println();
        System.out.println("All 8 subsets: [], [1], [2], [1,2], [3], [1,3], [2,3], [1,2,3]");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Approach 2: Bit Manipulation (Binary Representation)");
        System.out.println("Time: O(n × 2^n), Space: O(1)");
        System.out.println();
        System.out.println("Key insight: Each subset corresponds to a binary number!");
        System.out.println();
        System.out.println("For [1,2,3], we have 3 elements → 2^3 = 8 subsets");
        System.out.println();
        System.out.println("Mask | Binary | Subset      | Interpretation");
        System.out.println("-----|--------|-------------|----------------");
        System.out.println("  0  |  000   | []          | Include nothing");
        System.out.println("  1  |  001   | [1]         | Include element 0");
        System.out.println("  2  |  010   | [2]         | Include element 1");
        System.out.println("  3  |  011   | [1,2]       | Include elements 0,1");
        System.out.println("  4  |  100   | [3]         | Include element 2");
        System.out.println("  5  |  101   | [1,3]       | Include elements 0,2");
        System.out.println("  6  |  110   | [2,3]       | Include elements 1,2");
        System.out.println("  7  |  111   | [1,2,3]     | Include all");
        System.out.println();
        System.out.println("Algorithm:");
        System.out.println("  for mask from 0 to 2^n - 1:");
        System.out.println("    for each bit position i:");
        System.out.println("      if bit i is set in mask:");
        System.out.println("        include nums[i] in subset");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Approach 3: Iterative Cascading");
        System.out.println("Time: O(n × 2^n), Space: O(1)");
        System.out.println();
        System.out.println("Key insight: Build subsets incrementally");
        System.out.println();
        System.out.println("Start with [[]]");
        System.out.println();
        System.out.println("Add 1: [[]] → [[], [1]]");
        System.out.println("  Take each existing subset and add 1 to it");
        System.out.println();
        System.out.println("Add 2: [[], [1]] → [[], [1], [2], [1,2]]");
        System.out.println("  Take each existing subset and add 2 to it");
        System.out.println();
        System.out.println("Add 3: [[], [1], [2], [1,2]] → [[], [1], [2], [1,2], [3], [1,3], [2,3], [1,2,3]]");
        System.out.println("  Take each existing subset and add 3 to it");
        System.out.println();
        System.out.println("Pattern: At each step, size doubles!");
        System.out.println("  After element 0: 2 subsets");
        System.out.println("  After element 1: 4 subsets");
        System.out.println("  After element 2: 8 subsets");
        System.out.println("  After element n: 2^n subsets");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Mathematical Foundation:");
        System.out.println("-----------------------");
        System.out.println("Power Set: P(S) = set of all subsets of S");
        System.out.println();
        System.out.println("Properties:");
        System.out.println("  • |P(S)| = 2^|S| (size of power set = 2^n)");
        System.out.println("  • Empty set ∅ is always in P(S)");
        System.out.println("  • S itself is always in P(S)");
        System.out.println();
        System.out.println("Why 2^n subsets?");
        System.out.println("  For each element, we have 2 choices:");
        System.out.println("    1. Include it");
        System.out.println("    2. Exclude it");
        System.out.println("  With n elements: 2 × 2 × 2 × ... (n times) = 2^n");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Complexity Analysis:");
        System.out.println("-------------------");
        System.out.println("Time: O(n × 2^n)");
        System.out.println("  • Generate 2^n subsets");
        System.out.println("  • Each subset takes O(n) time to create/copy");
        System.out.println("  • Total: 2^n × n = O(n × 2^n)");
        System.out.println();
        System.out.println("Space (excluding output):");
        System.out.println("  • Backtracking: O(n) for recursion stack");
        System.out.println("  • Bit Manipulation: O(1) only variables");
        System.out.println("  • Iterative: O(1) only variables");
        System.out.println();
        System.out.println("Output Space: O(n × 2^n)");
        System.out.println("  • Store 2^n subsets");
        System.out.println("  • Average subset size is n/2");
        System.out.println("  • Total: ~n/2 × 2^n = O(n × 2^n)");
        System.out.println();

        System.out.println("---\n");

        System.out.println("Which Approach to Use?");
        System.out.println("---------------------");
        System.out.println("✓ Backtracking: Most intuitive, easy to understand");
        System.out.println("✓ Bit Manipulation: Most clever, impressive in interviews");
        System.out.println("✓ Iterative: Clear logic, no recursion overhead");
        System.out.println("✓ All have same time complexity O(n × 2^n)");
        System.out.println();

        System.out.println("Interview Tips:");
        System.out.println("  1. Start with backtracking (easiest to explain)");
        System.out.println("  2. Mention bit manipulation as optimization");
        System.out.println("  3. Explain why time is O(n × 2^n), not just O(2^n)");
        System.out.println("  4. Note that we can't do better than O(2^n) - must generate all subsets!");

        System.out.println("\n=== Growth of Power Set ===\n");

        System.out.println("n  | 2^n    | Example");
        System.out.println("---|--------|--------");
        for (int n = 0; n <= 10; n++) {
            int count = 1 << n;
            System.out.printf("%2d | %6d | ", n, count);
            if (n <= 3) {
                int[] arr = new int[n];
                for (int i = 0; i < n; i++)
                    arr[i] = i + 1;
                List<List<Integer>> subsets = generator.subsets(arr);
                System.out.println(subsets.size() + " subsets");
            } else {
                System.out.println("(too many to show)");
            }
        }

        System.out.println("\nNote: Growth is exponential! n=20 → 1 million subsets");

        System.out.println("\n=== Performance Test ===\n");

        for (int n = 1; n <= 15; n++) {
            int[] arr = new int[n];
            for (int i = 0; i < n; i++)
                arr[i] = i;

            long start = System.nanoTime();
            List<List<Integer>> result = generator.subsets(arr);
            long end = System.nanoTime();

            System.out.printf("n=%2d: %6d subsets in %8.2f ms\n",
                    n, result.size(), (end - start) / 1_000_000.0);
        }
    }
}

/*
 * Let’s explain **`Time: O(n · 2ⁿ)`** in a way that’s *intuitive*, *rigorous*,
 * and *interview-ready*.
 * 
 * We’re talking about the **Subsets (Power Set)** problem.
 * 
 * ---
 * 
 * ## Step 1: Why `2ⁿ`?
 * 
 * For each element in the array, you have **two choices**:
 * 
 * ❌ exclude it
 * ✅ include it
 * 
 * So for `n` elements:
 * 
 * ```
 * 2 × 2 × 2 × ... (n times) = 2ⁿ
 * ```
 * 
 * That’s exactly the number of subsets.
 * 
 * Example for `n = 3`:
 * 
 * ```
 * [] [1] [2] [3] [1,2] [1,3] [2,3] [1,2,3] → 8 = 2³
 * ```
 * 
 * 👉 Any algorithm that lists **all subsets** must handle **2ⁿ outputs**.
 * 
 * ---
 * 
 * ## Step 2: Why multiply by `n`?
 * 
 * In your backtracking code, this line matters:
 * 
 * ```java
 * result.add(new ArrayList<>(current));
 * ```
 * 
 * ### What does it cost?
 * 
 * `current` can contain up to `n` elements
 * Copying it takes **O(n)** time
 * 
 * This happens **once per subset**.
 * 
 * So:
 * 
 * ```
 * Cost per subset = O(n)
 * Number of subsets = 2ⁿ
 * ```
 * 
 * Multiply them:
 * 
 * ```
 * Total time = O(n · 2ⁿ)
 * ```
 * 
 * ---
 * 
 * ## Step 3: What about recursion calls?
 * 
 * The recursion tree has:
 * 
 * `2ⁿ` leaf nodes (subsets)
 * ~`2ⁿ⁺¹` total calls
 * 
 * But:
 * 
 * Internal nodes are **constant work**
 * **Only leaf nodes copy lists**
 * 
 * So copying dominates runtime.
 * 
 * ---
 * 
 * ## Visual intuition 🌳
 * 
 * Each level = one element
 * Each node splits into **pick / skip**
 * 
 * ```
 * []
 * / \
 * [] [1]
 * / \ / \
 * [] [2] [1] [1,2]
 * ...
 * ```
 * 
 * Height = `n`
 * Leaves = `2ⁿ`
 * Each leaf costs `O(n)` to output
 * 
 * ---
 * 
 * ## Important interview clarification ⚠️
 * 
 * > Why not just `O(2ⁿ)`?
 * 
 * Because **producing output costs time**.
 * 
 * If an interviewer pushes:
 * 
 * > “Isn’t it just 2ⁿ?”
 * 
 * Answer:
 * 
 * > “There are 2ⁿ subsets, but each subset copy takes up to O(n), so total time
 * is O(n·2ⁿ).”
 * 
 * That’s a **strong answer**.
 * 
 * ---
 * 
 * ## Final Summary
 * 
 * | Term | Reason |
 * | --------- | ------------------------ |
 * | `2ⁿ` | Number of subsets |
 * | `n` | Cost to copy each subset |
 * | **Total** | **O(n · 2ⁿ)** |
 * 
 * ---
 * 
 */
