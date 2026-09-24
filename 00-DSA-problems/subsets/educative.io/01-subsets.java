import java.util.*;
/**
 * ============================================================================
 * PROBLEM STATEMENT: Subsets (Power Set)
 * ============================================================================
 * Given an array of integers, nums, find all possible subsets of nums, 
 * including the empty set.
 * 
 * Note: The solution set must not contain duplicate subsets. 
 * You can return the solution in any order.
 * 
 * Constraints:
 * - 1 <= nums.length <= 10
 * - -10 <= nums[i] <= 10
 * - All the numbers of nums are unique.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS (To ask in an interview):
 * ============================================================================
 * 1. Can the input array be null or empty? 
 *    (Constraint says length >= 1, so no, but always good to check).
 * 2. Do the elements in the subsets need to be sorted?
 *    (Problem says "any order", but clarify if the interviewer has a preference).
 * 3. Does the final list of subsets need to be sorted? 
 *    (Again, problem says any order).
 * 4. Are there duplicate elements in the input array?
 *    (Constraint says all numbers are unique. If they weren't, we'd need to 
 *    handle duplicates by sorting and skipping adjacent duplicates, e.g., Subsets II).
 * 
 * ============================================================================
 * INTERVIEW APPROACH:
 * ============================================================================
 * 1. Acknowledge that the number of subsets for an array of size N is 2^N.
 * 2. State that because N <= 10, 2^10 = 1024, which is very small. Any valid 
 *    approach (O(N * 2^N) time complexity) will easily pass.
 * 3. Mention the three primary ways to solve this:
 *    - Cascading (Iterative): Easy to explain, builds subsets progressively.
 *    - Backtracking (Recursive / DFS): Standard combinatorial search technique.
 *    - Bit Manipulation: Clever, uses the binary representation of numbers from 
 *      0 to (2^N - 1) to pick elements.
 * 4. Start with Backtracking or Cascading as they are the most intuitive for 
 *    most interviewers. Mention Bit Manipulation as a bonus approach.
 * 
 * ============================================================================
 * TIME & SPACE COMPLEXITY (Applies to all solutions):
 * ============================================================================
 * - Time Complexity: O(N * 2^N) to generate all subsets and copy them into 
 *   the final list.
 * - Space Complexity: O(N * 2^N) to hold the output. Auxiliary space for 
 *   backtracking recursion stack is O(N).
 */

class SubsetsSolver {

    public static void main(String[] args) {
        int[] nums = {1, 2, 3};
        
        System.out.println("Input array: [1, 2, 3]\n");

        System.out.println("1. Cascading Approach:");
        System.out.println(cascadingSubsets(nums));
        
        System.out.println("\n2. Backtracking Approach:");
        System.out.println(backtrackingSubsets(nums));
        
        System.out.println("\n3. Bitmasking Approach (using Java Streams):");
        System.out.println(bitmaskSubsets(nums));
    }

    /**
     * ========================================================================
     * SOLUTION 1: CASCADING (ITERATIVE)
     * ========================================================================
     * Idea & Intuition:
     * Start with an empty subset. For each number in the input array, take all 
     * existing subsets and append the current number to them to create new subsets.
     * 
     * Visual Example for nums = [1, 2, 3]:
     * Initial: [ [] ]
     * Num 1:   [ [], [1] ]
     * Num 2:   [ [], [1], [2], [1, 2] ]
     * Num 3:   [ [], [1], [2], [1, 2], [3], [1, 3], [2, 3], [1, 2, 3] ]
     * 
     * Key Observation: At each step, the size of our result array doubles.
     */
    public static List<List<Integer>> cascadingSubsets(int[] nums) {
        // Use 'var' for modern Java type inference
        var result = new ArrayList<List<Integer>>();
        result.add(new ArrayList<>()); // Add empty set

        for (int num : nums) {
            int currentSize = result.size();
            // Iterate over currently existing subsets
            for (int i = 0; i < currentSize; i++) {
                // Copy the existing subset
                var newSubset = new ArrayList<Integer>(result.get(i));
                // Add the new element
                newSubset.add(num);
                // Add the newly formed subset to the result
                result.add(newSubset);
            }
        }
        return result;
    }

    /**
     * ========================================================================
     * SOLUTION 2: BACKTRACKING (DFS / RECURSION)
     * ========================================================================
     * Idea & Intuition:
     * We explore the decision tree of building a subset. At each element, 
     * we have two choices: INCLUDE the element in the current subset, or 
     * EXCLUDE it. (Alternatively, iterate through remaining elements and 
     * explore paths).
     * 
     * Visual Decision Tree for [1, 2]:
     *                      []
     *                   /      \
     *               [1]          []
     *              /   \        /   \
     *          [1,2]   [1]    [2]   []
     * 
     * Key Observation: Every state in the recursion tree represents a valid 
     * subset. We must make a deep copy of the current state before adding it 
     * to the results.
     */
    public static List<List<Integer>> backtrackingSubsets(int[] nums) {
        var result = new ArrayList<List<Integer>>();
        backtrack(0, nums, new ArrayList<>(), result);
        return result;
    }

    private static void backtrack(int startIndex, int[] nums, List<Integer> currentPath, List<List<Integer>> result) {
        // Add a copy of the current path to the result
        result.add(new ArrayList<>(currentPath));

        // Iterate from the start index to the end of the array
        for (int i = startIndex; i < nums.length; i++) {
            // INCLUDE: Add the element
            currentPath.add(nums[i]);
            
            // EXPLORE: Move to the next element
            backtrack(i + 1, nums, currentPath, result);
            
            // EXCLUDE: Backtrack (remove the last added element to try the next branch)
            currentPath.remove(currentPath.size() - 1);
        }
    }

    /**
     * ========================================================================
     * SOLUTION 3: BIT MANIPULATION (BITMASKING)
     * ========================================================================
     * Idea & Intuition:
     * An array of N elements has exactly 2^N subsets. 
     * We can map each subset to a binary number from 0 to (2^N - 1).
     * If the i-th bit is 1, it means nums[i] is in the subset.
     * If the i-th bit is 0, nums[i] is NOT in the subset.
     * 
     * Visual mapping for [1, 2, 3] (N=3):
     * Decimal | Binary | Subset
     * 0       | 000    | []
     * 1       | 001    | [1]
     * 2       | 010    | [2]
     * 3       | 011    | [1, 2]
     * 4       | 100    | [3]
     * 5       | 101    | [1, 3]
     * 6       | 110    | [2, 3]
     * 7       | 111    | [1, 2, 3]
     * 
     * Note: This implementation uses modern Java Streams to make it concise 
     * and highly readable.
     */
    public static List<List<Integer>> bitmaskSubsets(int[] nums) {
        int n = nums.length;
        int totalSubsets = 1 << n; // 2^n
        
        // Use Java IntStream to elegantly build the subsets
        return IntStream.range(0, totalSubsets)
                .mapToObj(mask -> IntStream.range(0, n)
                        // Check if the i-th bit is set in 'mask'
                        .filter(i -> (mask & (1 << i)) != 0)
                        // Map the set bit to the corresponding array element
                        .mapToObj(i -> nums[i])
                        .toList()) // Collect inner stream to an immutable List
                .toList();         // Collect outer stream to an immutable List
    }
}

/**
 * 🔥 Subsets — Backtracking (Most Important Approach)
 *
 * Time Complexity  : O(n * 2^n)
 * Space Complexity : O(n) recursion + O(2^n) output
 *
 * 🧠 Intuition:
 * - At every index, we decide whether to include the element
 * - We build subsets incrementally
 */
class SubsetsBacktracking {

    public static void main(String[] args) {
        int[] nums = {1, 2, 3};

        List<List<Integer>> result = subsets(nums);
        System.out.println(result);
    }

    public static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();

        // Start backtracking
        backtrack(0, nums, new ArrayList<>(), result);

        return result;
    }

    private static void backtrack(int index, int[] nums,
                                 List<Integer> current,
                                 List<List<Integer>> result) {

        /**
         * ✅ Always add current subset
         * Because every node in decision tree is a valid subset
         */
        result.add(new ArrayList<>(current));

        /**
         * Explore further choices
         */
        for (int i = index; i < nums.length; i++) {

            // Choose
            current.add(nums[i]);

            // Explore
            backtrack(i + 1, nums, current, result);

            // Undo (Backtrack)
            current.remove(current.size() - 1);
        }
    }
}

/**
 * 🔥 Subsets — Include / Exclude Style
 *
 * Time Complexity  : O(n * 2^n)
 * Space Complexity : O(n)
 *
 * 🧠 This directly models the decision tree
 */
class SubsetsIncludeExclude {

    public static List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();

        dfs(0, nums, new ArrayList<>(), result);

        return result;
    }

    private static void dfs(int index, int[] nums,
                            List<Integer> current,
                            List<List<Integer>> result) {

        // Base case: reached end
        if (index == nums.length) {
            result.add(new ArrayList<>(current));
            return;
        }

        /**
         * 1️⃣ Include current element
         */
        current.add(nums[index]);
        dfs(index + 1, nums, current, result);

        /**
         * 2️⃣ Exclude current element
         */
        current.remove(current.size() - 1);
        dfs(index + 1, nums, current, result);
    }
}
