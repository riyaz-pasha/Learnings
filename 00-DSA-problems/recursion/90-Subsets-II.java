import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class SubsetsII {

    // Approach 1: Backtracking with Skip Duplicates (Optimal)
    // Time: O(n * 2^n), Space: O(n) for recursion
    public List<List<Integer>> subsetsWithDup1(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums); // Sort to handle duplicates
        backtrack(nums, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrack(int[] nums, int start, List<Integer> current,
            List<List<Integer>> result) {
        // Add current subset to result
        result.add(new ArrayList<>(current));

        for (int i = start; i < nums.length; i++) {
            // Skip duplicates: if current element equals previous AND
            // we didn't use the previous element (i > start)
            if (i > start && nums[i] == nums[i - 1]) {
                continue;
            }

            // Include nums[i]
            current.add(nums[i]);
            backtrack(nums, i + 1, current, result);
            // Backtrack
            current.remove(current.size() - 1);
        }
    }

    // Approach 2: Iterative Approach
    // Time: O(n * 2^n), Space: O(1) excluding result
    public List<List<Integer>> subsetsWithDup2(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums);
        result.add(new ArrayList<>()); // Add empty subset

        int startIndex = 0;
        int endIndex = 0;

        for (int i = 0; i < nums.length; i++) {
            startIndex = 0;

            // If current element is duplicate, only add to subsets from last iteration
            if (i > 0 && nums[i] == nums[i - 1]) {
                startIndex = endIndex;
            }

            endIndex = result.size();

            // Add current element to all existing subsets
            for (int j = startIndex; j < endIndex; j++) {
                List<Integer> subset = new ArrayList<>(result.get(j));
                subset.add(nums[i]);
                result.add(subset);
            }
        }

        return result;
    }

    // Approach 3: Using HashSet to Remove Duplicates (Less Efficient)
    // Time: O(n * 2^n), Space: O(n * 2^n)
    public List<List<Integer>> subsetsWithDup3(int[] nums) {
        Set<List<Integer>> resultSet = new HashSet<>();
        Arrays.sort(nums);
        generateSubsets(nums, 0, new ArrayList<>(), resultSet);
        return new ArrayList<>(resultSet);
    }

    private void generateSubsets(int[] nums, int index, List<Integer> current,
            Set<List<Integer>> resultSet) {
        if (index == nums.length) {
            resultSet.add(new ArrayList<>(current));
            return;
        }

        // Include current element
        current.add(nums[index]);
        generateSubsets(nums, index + 1, current, resultSet);
        current.remove(current.size() - 1);

        // Exclude current element
        generateSubsets(nums, index + 1, current, resultSet);
    }

    // Approach 4: Bit Manipulation with Duplicate Check
    // Time: O(n * 2^n), Space: O(n * 2^n)
    public List<List<Integer>> subsetsWithDup4(int[] nums) {
        Arrays.sort(nums);
        Set<List<Integer>> resultSet = new HashSet<>();
        int n = nums.length;

        // Generate all possible subsets using bitmask
        for (int mask = 0; mask < (1 << n); mask++) {
            List<Integer> subset = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) {
                    subset.add(nums[i]);
                }
            }
            resultSet.add(subset);
        }

        return new ArrayList<>(resultSet);
    }

    // Approach 5: Backtracking with Frequency Map
    // Time: O(n * 2^n), Space: O(n)
    public List<List<Integer>> subsetsWithDup5(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        Arrays.sort(nums);

        // Count frequency of each unique number
        List<int[]> freqList = new ArrayList<>();
        int i = 0;
        while (i < nums.length) {
            int count = 1;
            while (i + count < nums.length && nums[i] == nums[i + count]) {
                count++;
            }
            freqList.add(new int[] { nums[i], count });
            i += count;
        }

        backtrackWithFreq(freqList, 0, new ArrayList<>(), result);
        return result;
    }

    private void backtrackWithFreq(List<int[]> freqList, int index,
            List<Integer> current, List<List<Integer>> result) {
        if (index == freqList.size()) {
            result.add(new ArrayList<>(current));
            return;
        }

        int num = freqList.get(index)[0];
        int freq = freqList.get(index)[1];

        // Try including 0, 1, 2, ..., freq copies of current number
        for (int count = 0; count <= freq; count++) {
            for (int i = 0; i < count; i++) {
                current.add(num);
            }
            backtrackWithFreq(freqList, index + 1, current, result);
            for (int i = 0; i < count; i++) {
                current.remove(current.size() - 1);
            }
        }
    }

    // Test cases with visualization
    public static void main(String[] args) {
        SubsetsII solution = new SubsetsII();

        // Test Case 1
        int[] nums1 = { 1, 2, 2 };
        List<List<Integer>> result1 = solution.subsetsWithDup1(nums1);
        System.out.println("Test 1: " + Arrays.toString(nums1));
        System.out.println("Output: " + result1);
        System.out.println("Count: " + result1.size() + " subsets");
        visualizeSubsets(nums1, result1);

        // Test Case 2
        int[] nums2 = { 0 };
        List<List<Integer>> result2 = solution.subsetsWithDup1(nums2);
        System.out.println("\nTest 2: " + Arrays.toString(nums2));
        System.out.println("Output: " + result2);

        // Test Case 3: More duplicates
        int[] nums3 = { 1, 1, 2, 2 };
        List<List<Integer>> result3 = solution.subsetsWithDup1(nums3);
        System.out.println("\nTest 3: " + Arrays.toString(nums3));
        System.out.println("Output: " + result3);
        System.out.println("Count: " + result3.size() + " subsets");

        // Test Case 4: All same
        int[] nums4 = { 2, 2, 2 };
        List<List<Integer>> result4 = solution.subsetsWithDup1(nums4);
        System.out.println("\nTest 4: " + Arrays.toString(nums4));
        System.out.println("Output: " + result4);
        System.out.println("Count: " + result4.size() + " subsets");

        // Test Case 5: No duplicates
        int[] nums5 = { 1, 2, 3 };
        List<List<Integer>> result5 = solution.subsetsWithDup1(nums5);
        System.out.println("\nTest 5: " + Arrays.toString(nums5));
        System.out.println("Output: " + result5);
        System.out.println("Count: " + result5.size() + " subsets (should be 2^3 = 8)");

        // Compare approaches
        System.out.println("\nComparing approaches for [1,2,2]:");
        System.out.println("Approach 1 size: " + solution.subsetsWithDup1(nums1).size());
        System.out.println("Approach 2 size: " + solution.subsetsWithDup2(nums1).size());
        System.out.println("Approach 3 size: " + solution.subsetsWithDup3(nums1).size());
        System.out.println("Approach 4 size: " + solution.subsetsWithDup4(nums1).size());
        System.out.println("Approach 5 size: " + solution.subsetsWithDup5(nums1).size());
    }

    private static void visualizeSubsets(int[] nums, List<List<Integer>> subsets) {
        System.out.println("\nSubsets grouped by size:");
        Map<Integer, List<List<Integer>>> grouped = new TreeMap<>();

        for (List<Integer> subset : subsets) {
            grouped.computeIfAbsent(subset.size(), k -> new ArrayList<>()).add(subset);
        }

        for (Map.Entry<Integer, List<List<Integer>>> entry : grouped.entrySet()) {
            System.out.println("Size " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("\nDecision tree visualization:");
        System.out.println("Starting with: " + Arrays.toString(nums));
        System.out.println("After sorting: " + Arrays.toString(nums));
        System.out.println("\nBuilding subsets by choosing whether to include each element:");
        System.out.println("[] → [1] → [1,2] → [1,2,2]");
        System.out.println("         → [1,2]");
        System.out.println("  → [2] → [2,2]");
        System.out.println("  → []");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM UNDERSTANDING:
 * - Generate all subsets (power set) of an array
 * - Array may contain duplicates
 * - Result must NOT contain duplicate subsets
 * - Key difference from Subsets I: handling duplicates
 * 
 * KEY INSIGHT - HANDLING DUPLICATES:
 * 
 * 1. MUST sort the array first
 * - Groups duplicate elements together
 * - Allows systematic skipping
 * 
 * 2. Skip duplicate elements intelligently:
 * - If nums[i] == nums[i-1] AND i > start
 * - This means we're considering the same element again at the same level
 * - Skip it to avoid duplicate subsets
 * 
 * WHY SORTING IS CRUCIAL:
 * 
 * Without sorting: [2, 1, 2]
 * - Can generate [1, 2] and [2, 1] (same subset, different order)
 * - Can generate [2, 2] multiple times
 * 
 * With sorting: [1, 2, 2]
 * - Clear structure for skipping
 * - Only one way to generate each unique subset
 * 
 * BACKTRACKING APPROACH (Approach 1):
 * 
 * Algorithm:
 * 1. Sort the array
 * 2. Use backtracking to explore choices:
 * - Include current element
 * - Skip current element
 * 3. Skip duplicates: if nums[i] == nums[i-1] and i > start
 * 
 * Decision Tree for [1, 2, 2]:
 * 
 * []
 * / | \
 * [1] [2] skip 2nd 2
 * / \ / \
 * [1,2] [1] [2,2] [2]
 * |
 * [1,2,2]
 * 
 * Key: When at position i with duplicate, skip if we didn't use previous
 * element
 * 
 * EXAMPLE WALKTHROUGH: nums = [1, 2, 2]
 * 
 * After sorting: [1, 2, 2]
 * 
 * Recursion trace:
 * 1. Start: current=[], start=0
 * - Add [] to result
 * - Try i=0 (num=1):
 * - current=[1], start=1
 * - Add [1] to result
 * - Try i=1 (num=2):
 * - current=[1,2], start=2
 * - Add [1,2] to result
 * - Try i=2 (num=2):
 * - current=[1,2,2], start=3
 * - Add [1,2,2] to result
 * - Backtrack to [1,2]
 * - Backtrack to [1]
 * - Try i=2: SKIP (nums[2]==nums[1] and i>start)
 * - Backtrack to []
 * - Try i=1 (num=2):
 * - current=[2], start=2
 * - Add [2] to result
 * - Try i=2 (num=2):
 * - current=[2,2], start=3
 * - Add [2,2] to result
 * - Backtrack to []
 * - Try i=2: SKIP (nums[2]==nums[1] and i>start)
 * 
 * Result: [[], [1], [1,2], [1,2,2], [2], [2,2]]
 * 
 * DUPLICATE SKIPPING LOGIC:
 * 
 * ```java
 * if (i > start && nums[i] == nums[i - 1]) {
 * continue; // Skip duplicate
 * }
 * ```
 * 
 * Why "i > start"?
 * - i = start: First element at this level, must consider
 * - i > start: Already tried this value at this level, skip
 * 
 * Example: At level with [2, 2]:
 * - i = start: Use first 2 (include it)
 * - i > start: Second 2 appears, skip it (already handled by previous
 * recursion)
 * 
 * ITERATIVE APPROACH (Approach 2):
 * 
 * Key idea: When we encounter a duplicate:
 * - Only add it to subsets created in the LAST iteration
 * - Don't add to older subsets (would create duplicates)
 * 
 * Example: [1, 2, 2]
 * Initial: [[]]
 * 
 * Process 1:
 * - Add 1 to all: [[], [1]]
 * 
 * Process first 2:
 * - Add 2 to all: [[], [1], [2], [1,2]]
 * 
 * Process second 2:
 * - This is duplicate! Only add to subsets from last iteration
 * - Last iteration added: [2], [1,2]
 * - Result: [[], [1], [2], [1,2], [2,2], [1,2,2]]
 * 
 * COMPLEXITY ANALYSIS:
 * 
 * Time Complexity: O(n * 2^n)
 * - Total subsets: at most 2^n
 * - Creating each subset: O(n) to copy
 * - Sorting: O(n log n) is negligible
 * 
 * Space Complexity:
 * - Result storage: O(n * 2^n) - store all subsets
 * - Recursion depth: O(n) - maximum depth
 * - Total: O(n * 2^n)
 * 
 * NUMBER OF SUBSETS:
 * 
 * Without duplicates: 2^n subsets
 * With duplicates: fewer subsets
 * 
 * Example: [2, 2, 2, 2]
 * - Without deduplication: 2^4 = 16 subsets
 * - With deduplication: 5 subsets: [], [2], [2,2], [2,2,2], [2,2,2,2]
 * 
 * Formula with duplicates:
 * If element e appears k times: contributes (k+1) choices (0, 1, 2, ..., k
 * copies)
 * Total = product of (frequency + 1) for each unique element
 * 
 * EDGE CASES:
 * 
 * 1. Single element: [0] → [[], [0]]
 * 2. All duplicates: [2,2,2] → [[], [2], [2,2], [2,2,2]]
 * 3. No duplicates: [1,2,3] → 8 subsets (standard power set)
 * 4. Mixed: [1,1,2] → [[], [1], [1,1], [1,1,2], [1,2], [2]]
 * 
 * COMPARISON WITH SUBSETS I:
 * 
 * Subsets I (no duplicates):
 * - No need to sort
 * - No need to skip duplicates
 * - Simpler logic
 * 
 * Subsets II (with duplicates):
 * - MUST sort first
 * - MUST skip duplicates intelligently
 * - Slightly more complex
 * 
 * COMMON MISTAKES:
 * 
 * 1. Forgetting to sort the array
 * 2. Wrong duplicate skipping condition (missing i > start)
 * 3. Not creating new ArrayList when adding to result
 * 4. Using Set to deduplicate (works but inefficient)
 * 
 * INTERVIEW TIPS:
 * 
 * 1. Always ask: "Can the array contain duplicates?"
 * 2. Explain why sorting is necessary
 * 3. Draw the decision tree to show duplicate skipping
 * 4. Mention time/space complexity
 * 5. Consider iterative vs recursive tradeoffs
 * 6. Test with edge cases: all same, no duplicates
 * 
 * PRACTICAL APPLICATIONS:
 * 
 * 1. Combination generation with constraints
 * 2. Feature selection in machine learning
 * 3. Portfolio selection with duplicate assets
 * 4. Database query generation
 * 5. Configuration management systems
 */
