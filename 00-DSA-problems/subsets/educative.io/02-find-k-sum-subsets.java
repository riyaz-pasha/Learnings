/**
 * ============================================================================
 * PROBLEM STATEMENT: Subsets with Target Sum (Subset Sum / Combination Sum)
 * ============================================================================
 * Given an array of n distinct positive integers, find all possible subsets 
 * of these integers such that the sum of the elements in each subset equals 
 * a given target value k.
 * 
 * Return a 2D array (List of Lists), where each inner array represents a 
 * subset whose sum equals k.
 * 
 * Constraints:
 * - 1 <= n <= 10
 * - 1 <= x <= 100 (where x is any member of the input array)
 * - 1 <= k <= 1000
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS (To ask in an interview):
 * ============================================================================
 * 1. Can we reuse elements? 
 *    (The problem specifies "subsets", which implies each element from the 
 *    input array is either included once or excluded).
 * 2. Does the order of subsets or the order of elements inside the subset matter?
 *    (Usually no, but clarifying shows attention to detail).
 * 3. Are there negative numbers or zeros?
 *    (Constraint says "distinct positive integers". This is a crucial detail 
 *    because positive numbers allow us to stop searching early if our current 
 *    sum exceeds the target. Negatives would prevent this optimization).
 * 
 * ============================================================================
 * INTERVIEW APPROACH:
 * ============================================================================
 * 1. Note the constraints: n <= 10. This means the total number of subsets 
 *    is at most 2^10 = 1024. This is an extremely small search space.
 * 2. Backtracking (DFS) is the most standard and expected solution. We can 
 *    explore building subsets and keep a running sum.
 * 3. Mention Pruning (Optimization): Since all numbers are positive, if our 
 *    current running sum exceeds 'k', we can immediately abandon that path 
 *    because adding more positive numbers will only increase the sum. Sorting 
 *    the array beforehand makes this pruning even more effective.
 * 4. Bit Manipulation is an alternative because of the small N, but backtracking 
 *    is generally preferred as it avoids calculating sums for obviously invalid 
 *    paths.
 * 
 * ============================================================================
 * TIME & SPACE COMPLEXITY:
 * ============================================================================
 * - Time Complexity: O(2^n) in the worst case (e.g., checking all subsets). 
 *   However, with pruning, the actual runtime is significantly faster.
 * - Space Complexity: O(n) for the recursion stack and the temporary path list, 
 *   plus O(V) to store the output (where V is the number of valid subsets).
 */

import java.util.*;

class SubsetSumSolver {

    public static void main(String[] args) {
        int[] nums = {2, 4, 6, 8, 10};
        int target = 10;
        
        System.out.println("Input array: [2, 4, 6, 8, 10], Target: 10\n");

        System.out.println("1. Backtracking Approach (Optimized with Pruning):");
        System.out.println(findSubsetsBacktracking(nums, target));
        
        System.out.println("\n2. Bitmasking Approach (Using Java Streams):");
        System.out.println(findSubsetsBitmask(nums, target));
    }

    /**
     * ========================================================================
     * SOLUTION 1: BACKTRACKING WITH PRUNING (RECOMMENDED)
     * ========================================================================
     * Idea & Intuition:
     * We use a recursive function to build subsets one element at a time.
     * We keep track of the `currentSum`. If `currentSum == target`, we save it.
     * If `currentSum > target`, we stop exploring that branch (pruning).
     * 
     * Key Observation:
     * Sorting the array first allows for an early exit. If the current element 
     * makes the sum exceed the target, all subsequent (larger) elements will 
     * also exceed the target, so we can `break` out of the loop entirely.
     */
    public static List<List<Integer>> findSubsetsBacktracking(int[] nums, int target) {
        var result = new ArrayList<List<Integer>>();
        
        // Sorting helps in optimizing the backtracking (early termination)
        Arrays.sort(nums); 
        
        backtrack(0, nums, target, 0, new ArrayList<>(), result);
        return result;
    }

    private static void backtrack(int startIndex, int[] nums, int target, int currentSum, 
                                  List<Integer> currentPath, List<List<Integer>> result) {
        
        // Base case: Found a valid subset
        if (currentSum == target) {
            // Make a deep copy using modern Java syntax
            result.add(List.copyOf(currentPath));
            return;
        }

        for (int i = startIndex; i < nums.length; i++) {
            // PRUNING: Because the array is sorted and contains only positives, 
            // if adding the current number exceeds target, adding any subsequent 
            // numbers will definitely exceed it too. We can safely break.
            if (currentSum + nums[i] > target) {
                break; 
            }

            // INCLUDE the element
            currentPath.add(nums[i]);
            
            // EXPLORE the next elements
            backtrack(i + 1, nums, target, currentSum + nums[i], currentPath, result);
            
            // EXCLUDE the element (backtrack)
            currentPath.remove(currentPath.size() - 1);
        }
    }

    /**
     * ========================================================================
     * SOLUTION 2: BIT MANIPULATION (BITMASKING)
     * ========================================================================
     * Idea & Intuition:
     * With N elements, there are 2^N possible subsets, representable by binary 
     * numbers from 0 to (2^N - 1). We can evaluate every single possible subset, 
     * sum its elements, and keep the ones that equal 'k'.
     * 
     * While conceptually simple and heavily utilizing modern Java Streams, 
     * it evaluates all 2^N combinations regardless of the target, meaning it 
     * doesn't benefit from the pruning optimization present in backtracking.
     */
    public static List<List<Integer>> findSubsetsBitmask(int[] nums, int target) {
        int n = nums.length;
        int totalSubsets = 1 << n; // 2^n
        
        // Generate all possible masks from 0 to (2^n - 1)
        return IntStream.range(0, totalSubsets)
                .mapToObj(mask -> {
                    // Build the subset for the current mask
                    return IntStream.range(0, n)
                            .filter(i -> (mask & (1 << i)) != 0) // If i-th bit is set
                            .mapToObj(i -> nums[i])
                            .toList();
                })
                // Filter only subsets whose sum matches the target
                .filter(subset -> subset.stream().mapToInt(Integer::intValue).sum() == target)
                .toList();
    }
}

class SubsetSumK {

    public static void main(String[] args) {
        int[] nums = {1, 2, 3, 4};
        int k = 5;

        List<List<Integer>> result = subsetsWithSumK(nums, k);
        System.out.println(result);
    }

    public static List<List<Integer>> subsetsWithSumK(int[] nums, int k) {
        List<List<Integer>> result = new ArrayList<>();

        // Start backtracking
        backtrack(0, nums, k, 0, new ArrayList<>(), result);

        return result;
    }

    /**
     * index -> current position in array
     * currentSum -> sum so far
     * currentSubset -> elements chosen till now
     */
    private static void backtrack(int index,
                                  int[] nums,
                                  int target,
                                  int currentSum,
                                  List<Integer> currentSubset,
                                  List<List<Integer>> result) {

        // ✅ BASE CASE: Found valid subset
        if (currentSum == target) {
            result.add(new ArrayList<>(currentSubset));
            return; // Important: don't continue further
        }

        // ❌ Pruning conditions
        if (index == nums.length || currentSum > target) {
            return;
        }

        // ================================
        // 1️⃣ INCLUDE current element
        // ================================
        currentSubset.add(nums[index]);

        backtrack(index + 1, nums, target,
                  currentSum + nums[index],
                  currentSubset, result);

        // BACKTRACK (undo choice)
        currentSubset.remove(currentSubset.size() - 1);

        // ================================
        // 2️⃣ EXCLUDE current element
        // ================================
        backtrack(index + 1, nums, target,
                  currentSum,
                  currentSubset, result);
    }
}
