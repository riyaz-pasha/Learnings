/**
 * ============================================================================
 * PROBLEM STATEMENT: Subsets II (Subsets with Duplicates)
 * ============================================================================
 * Given an integer array nums that can contain duplicate elements, return all 
 * possible subsets while ensuring that each subset is unique. 
 * 
 * The output must include unique subsets, and you may return them in any order.
 * 
 * Constraints:
 * - 1 <= nums.length <= 10
 * - -10 <= nums[i] <= 10
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS (To ask in an interview):
 * ============================================================================
 * 1. Are the elements in the input array sorted?
 *    (Usually no. Sorting is the first step we must take to easily identify 
 *    and handle duplicates).
 * 2. Can the input array be empty?
 *    (Constraint says length >= 1, but handling empty arrays gracefully is 
 *    always a good practice).
 * 3. Does the order of elements inside the subsets matter?
 *    (No, [1, 2] and [2, 1] are considered the same subset, which is another 
 *    reason we must sort the array first so identical subsets evaluate to the 
 *    same sequence).
 * 
 * ============================================================================
 * INTERVIEW APPROACH:
 * ============================================================================
 * 1. Acknowledge the core challenge: The array has duplicates. If we use the 
 *    standard subsets algorithm, we will generate duplicate subsets (e.g., for 
 *    [1, 2, 2], we'd generate [1, 2] twice, once using the first '2' and once 
 *    using the second '2').
 * 2. State the key strategy: SORT THE ARRAY. When the array is sorted, duplicate 
 *    elements are adjacent. This allows us to easily skip an element if it's the 
 *    same as the previous one *at the same level of recursion*.
 * 3. Discuss approaches:
 *    - Backtracking (Optimal): Standard DFS, but with a condition to skip 
 *      duplicates in the loop.
 *    - Cascading (Iterative): Build subsets progressively, but if a number is 
 *      a duplicate, only add it to the subsets generated in the immediately 
 *      preceding step.
 *    - Bitmasking + HashSet (Naive): Generate all 2^N subsets and throw them 
 *      into a Set to filter duplicates. Good to mention as a fallback, but 
 *      highlight that it does unnecessary work.
 * 
 * ============================================================================
 * TIME & SPACE COMPLEXITY:
 * ============================================================================
 * - Time Complexity: O(N * 2^N) for Backtracking and Cascading. Sorting takes 
 *   O(N log N), which is dominated by generating the 2^N subsets.
 * - Space Complexity: O(N * 2^N) to hold the output. Auxiliary space for the 
 *   backtracking recursion stack is O(N).
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

class SubsetsIISolver {

    public static void main(String[] args) {
        int[] nums = {1, 2, 2};
        
        System.out.println("Input array: [1, 2, 2]\n");

        System.out.println("1. Backtracking (Optimal) Approach:");
        System.out.println(subsetsWithDupBacktracking(nums));
        
        System.out.println("\n2. Cascading (Iterative) Approach:");
        System.out.println(subsetsWithDupCascading(nums));
        
        System.out.println("\n3. Bitmasking + Distinct (Using Streams) Approach:");
        System.out.println(subsetsWithDupBitmasking(nums));
    }

    /**
     * ========================================================================
     * SOLUTION 1: BACKTRACKING (OPTIMAL DFS)
     * ========================================================================
     * Idea & Intuition:
     * Sort the array first. During our DFS traversal, if we are at a step where 
     * we are choosing the next element to include, and the current element is 
     * identical to the previous element we just considered *at this same depth*, 
     * we skip it.
     * 
     * Visual Decision Tree for [1, 2a, 2b]:
     *                        []
     *              /         |         \
     *           [1]        [2a]        [2b] (SKIPPED! Duplicate of 2a)
     *          /   \         |
     *    [1,2a] [1,2b](X)  [2a,2b]
     *       |
     *  [1,2a,2b]
     * 
     * (X) -> Skipped because at depth 1, '2b' is the same as the previous '2a'.
     */
    public static List<List<Integer>> subsetsWithDupBacktracking(int[] nums) {
        var result = new ArrayList<List<Integer>>();
        
        // Step 1: Sort the array to bring duplicates together
        Arrays.sort(nums);
        
        backtrack(0, nums, new ArrayList<>(), result);
        return result;
    }

    private static void backtrack(int startIndex, int[] nums, List<Integer> currentPath, List<List<Integer>> result) {
        // Add a copy of the current subset to the result
        result.add(new ArrayList<>(currentPath));

        for (int i = startIndex; i < nums.length; i++) {
            // PRUNING STEP: Skip duplicates at the same level of the decision tree.
            // i > startIndex ensures we don't skip the *first* occurrence of a 
            // number in the current recursive depth.
            if (i > startIndex && nums[i] == nums[i - 1]) {
                continue;
            }

            // INCLUDE
            currentPath.add(nums[i]);
            
            // EXPLORE
            backtrack(i + 1, nums, currentPath, result);
            
            // EXCLUDE (Backtrack)
            currentPath.remove(currentPath.size() - 1);
        }
    }

    /**
     * ========================================================================
     * SOLUTION 2: CASCADING (ITERATIVE)
     * ========================================================================
     * Idea & Intuition:
     * We build subsets iteratively. When we see a number, we append it to all 
     * existing subsets. However, if the number is a duplicate of the previous 
     * number, we should ONLY append it to the subsets that were created in the 
     * *previous step*. If we append it to older subsets, we will create duplicates.
     * 
     * Visual Example for [1, 2, 2]:
     * 1. Start: [ [] ]
     * 2. Num 1: [ [], [1] ]  (Added to all 1 previous)
     * 3. Num 2: [ [], [1], [2], [1, 2] ] (Added to all 2 previous)
     * 4. Num 2 (Duplicate!): We only add this '2' to the subsets generated in 
     *    step 3 ([2] and [1,2]), producing -> [2, 2], [1, 2, 2].
     *    Final: [ [], [1], [2], [1, 2], [2, 2], [1, 2, 2] ]
     */
    public static List<List<Integer>> subsetsWithDupCascading(int[] nums) {
        // Sort to handle duplicates properly
        Arrays.sort(nums);
        
        var result = new ArrayList<List<Integer>>();
        result.add(new ArrayList<>());
        
        int startIndex = 0;
        int endIndex = 0;

        for (int i = 0; i < nums.length; i++) {
            startIndex = 0;
            
            // If current element is a duplicate, we only append it to the subsets 
            // created in the immediate previous step.
            if (i > 0 && nums[i] == nums[i - 1]) {
                startIndex = endIndex; // Start where the last step left off
            }
            
            endIndex = result.size();
            
            for (int j = startIndex; j < endIndex; j++) {
                var newSubset = new ArrayList<Integer>(result.get(j));
                newSubset.add(nums[i]);
                result.add(newSubset);
            }
        }
        
        return result;
    }

    /**
     * ========================================================================
     * SOLUTION 3: BITMASKING + DISTINCT (MODERN JAVA STREAMS)
     * ========================================================================
     * Idea & Intuition:
     * There are 2^N possible combinations. We can generate all of them using 
     * bit manipulation. Since there are duplicates in the input, some generated 
     * subsets will be identical. By sorting the input array first, identical 
     * subsets will map to exactly equal List objects. We can then use Stream's 
     * `.distinct()` to filter them out.
     * 
     * Note: While elegant and showcasing modern Java (Streams), this is less 
     * optimal than Backtracking/Cascading because it generates the duplicates 
     * first before filtering them out.
     */
    public static List<List<Integer>> subsetsWithDupBitmasking(int[] nums) {
        // Sort to ensure identical subsets have elements in the exact same order
        Arrays.sort(nums);
        
        int n = nums.length;
        int totalSubsets = 1 << n; // 2^n
        
        return IntStream.range(0, totalSubsets)
                .mapToObj(mask -> IntStream.range(0, n)
                        // If the i-th bit is set, include nums[i]
                        .filter(i -> (mask & (1 << i)) != 0)
                        .mapToObj(i -> nums[i])
                        .toList()) // Create immutable list for the subset
                .distinct()        // Filters out duplicate subsets (Requires elements to be sorted)
                .toList();         // Collect to final immutable List of Lists
    }
}
