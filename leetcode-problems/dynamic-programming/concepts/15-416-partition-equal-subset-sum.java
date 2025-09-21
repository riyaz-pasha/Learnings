/*
 * You are given an array 'ARR' of 'N' positive integers. Your task is to find
 * if we can partition the given array into two subsets such that the sum of
 * elements in both subsets is equal.
 * 
 * For example, let’s say the given array is [2, 3, 3, 3, 4, 5], then the array
 * can be partitioned as [2, 3, 5], and [3, 3, 4] with equal sum 10.
 * 
 * Follow Up:
 * Can you solve this using not more than O(S) extra space, where S is the sum
 * of all elements of the given array?
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// Solution 1: Brute Force - Generate All Subsets
// Time: O(2^N), Space: O(N) for recursion stack
class Solution1_BruteForce {
    public boolean canPartition(int[] arr) {
        int totalSum = Arrays.stream(arr).sum();

        // If total sum is odd, cannot partition equally
        if (totalSum % 2 != 0) {
            return false;
        }

        int target = totalSum / 2;
        return canMakeSum(arr, 0, 0, target);
    }

    private boolean canMakeSum(int[] arr, int index, int currentSum, int target) {
        // Base case: processed all elements
        if (index == arr.length) {
            return currentSum == target;
        }

        // Include current element in first subset
        if (canMakeSum(arr, index + 1, currentSum + arr[index], target)) {
            return true;
        }

        // Include current element in second subset (or exclude from first)
        if (canMakeSum(arr, index + 1, currentSum, target)) {
            return true;
        }

        return false;
    }
}

// Solution 2: Top-Down with Memoization
// Time: O(N*Sum), Space: O(N*Sum)
class Solution2_TopDownMemo {
    public boolean canPartition(int[] arr) {
        int totalSum = Arrays.stream(arr).sum();

        if (totalSum % 2 != 0) {
            return false;
        }

        int target = totalSum / 2;
        Boolean[][] memo = new Boolean[arr.length][target + 1];
        return canMakeTarget(arr, 0, target, memo);
    }

    private boolean canMakeTarget(int[] arr, int index, int target, Boolean[][] memo) {
        // Base cases
        if (target == 0)
            return true; // Found exact sum
        if (index >= arr.length || target < 0)
            return false; // Invalid state

        // Check memo
        if (memo[index][target] != null) {
            return memo[index][target];
        }

        // Include current element
        boolean include = canMakeTarget(arr, index + 1, target - arr[index], memo);

        // Exclude current element
        boolean exclude = canMakeTarget(arr, index + 1, target, memo);

        memo[index][target] = include || exclude;
        return memo[index][target];
    }
}

// Solution 3: Bottom-Up Dynamic Programming (2D)
// Time: O(N*Sum), Space: O(N*Sum)
class Solution3_BottomUp2D {
    public boolean canPartition(int[] arr) {
        int totalSum = Arrays.stream(arr).sum();

        if (totalSum % 2 != 0) {
            return false;
        }

        int target = totalSum / 2;
        int n = arr.length;

        // dp[i][j] = true if sum j can be achieved using first i elements
        boolean[][] dp = new boolean[n + 1][target + 1];

        // Base case: sum 0 can always be achieved (empty subset)
        for (int i = 0; i <= n; i++) {
            dp[i][0] = true;
        }

        // Fill the DP table
        for (int i = 1; i <= n; i++) {
            for (int sum = 1; sum <= target; sum++) {
                // Exclude current element
                dp[i][sum] = dp[i - 1][sum];

                // Include current element if possible
                if (sum >= arr[i - 1]) {
                    dp[i][sum] = dp[i][sum] || dp[i - 1][sum - arr[i - 1]];
                }
            }
        }

        return dp[n][target];
    }
}

// Solution 4: Space Optimized (1D) - RECOMMENDED
// Time: O(N*Sum), Space: O(Sum)
class Solution4_SpaceOptimized1D {
    public boolean canPartition(int[] arr) {
        int totalSum = Arrays.stream(arr).sum();

        if (totalSum % 2 != 0) {
            return false;
        }

        int target = totalSum / 2;

        // dp[j] = true if sum j can be achieved
        boolean[] dp = new boolean[target + 1];
        dp[0] = true; // Base case: sum 0 is always possible

        // Process each element
        for (int num : arr) {
            // Traverse from right to left to avoid using updated values
            for (int sum = target; sum >= num; sum--) {
                dp[sum] = dp[sum] || dp[sum - num];
            }

            // Early termination optimization
            if (dp[target]) {
                return true;
            }
        }

        return dp[target];
    }
}

// Solution 5: With Partition Reconstruction
// Time: O(N*Sum), Space: O(N*Sum)
class Solution5_WithPartition {
    public class Result {
        boolean canPartition;
        List<Integer> subset1;
        List<Integer> subset2;

        Result(boolean canPartition, List<Integer> subset1, List<Integer> subset2) {
            this.canPartition = canPartition;
            this.subset1 = new ArrayList<>(subset1);
            this.subset2 = new ArrayList<>(subset2);
        }

        @Override
        public String toString() {
            if (!canPartition) {
                return "Cannot partition into equal sum subsets";
            }

            int sum1 = subset1.stream().mapToInt(Integer::intValue).sum();
            int sum2 = subset2.stream().mapToInt(Integer::intValue).sum();

            return String.format("Can partition:\nSubset 1: %s (sum = %d)\nSubset 2: %s (sum = %d)",
                    subset1, sum1, subset2, sum2);
        }
    }

    public Result canPartitionWithSubsets(int[] arr) {
        int totalSum = Arrays.stream(arr).sum();

        if (totalSum % 2 != 0) {
            return new Result(false, new ArrayList<>(), new ArrayList<>());
        }

        int target = totalSum / 2;
        int n = arr.length;

        boolean[][] dp = new boolean[n + 1][target + 1];

        // Base case
        for (int i = 0; i <= n; i++) {
            dp[i][0] = true;
        }

        // Fill DP table
        for (int i = 1; i <= n; i++) {
            for (int sum = 1; sum <= target; sum++) {
                dp[i][sum] = dp[i - 1][sum];

                if (sum >= arr[i - 1]) {
                    dp[i][sum] = dp[i][sum] || dp[i - 1][sum - arr[i - 1]];
                }
            }
        }

        if (!dp[n][target]) {
            return new Result(false, new ArrayList<>(), new ArrayList<>());
        }

        // Reconstruct partition
        List<Integer> subset1 = new ArrayList<>();
        List<Integer> subset2 = new ArrayList<>();

        int i = n, sum = target;
        while (i > 0 && sum > 0) {
            // If current state didn't come from previous row,
            // then current element is included in subset1
            if (!dp[i - 1][sum]) {
                subset1.add(arr[i - 1]);
                sum -= arr[i - 1];
            } else {
                subset2.add(arr[i - 1]);
            }
            i--;
        }

        // Add remaining elements to subset2
        while (i > 0) {
            subset2.add(arr[i - 1]);
            i--;
        }

        return new Result(true, subset1, subset2);
    }
}

// Solution 6: Bitset Optimization
// Time: O(N*Sum/32), Space: O(Sum/32)
class Solution6_BitsetOptimized {
    public boolean canPartition(int[] arr) {
        int totalSum = Arrays.stream(arr).sum();

        if (totalSum % 2 != 0) {
            return false;
        }

        int target = totalSum / 2;

        // Use BitSet for efficient operations
        BitSet dp = new BitSet(target + 1);
        dp.set(0); // Base case

        for (int num : arr) {
            // Shift existing possibilities
            BitSet next = new BitSet(target + 1);

            // Copy current state (exclude current element)
            next.or(dp);

            // Add possibilities with current element included
            for (int i = dp.nextSetBit(0); i >= 0 && i + num <= target; i = dp.nextSetBit(i + 1)) {
                next.set(i + num);
            }

            dp = next;

            // Early termination
            if (dp.get(target)) {
                return true;
            }
        }

        return dp.get(target);
    }
}

// Solution 7: Meet in the Middle (for large arrays)
// Time: O(N * 2^(N/2)), Space: O(2^(N/2))
class Solution7_MeetInMiddle {
    public boolean canPartition(int[] arr) {
        int totalSum = Arrays.stream(arr).sum();

        if (totalSum % 2 != 0) {
            return false;
        }

        int target = totalSum / 2;
        int n = arr.length;

        // For small arrays, use regular DP
        if (n <= 20) {
            return new Solution4_SpaceOptimized1D().canPartition(arr);
        }

        int mid = n / 2;

        // Generate all possible sums for first half
        Set<Integer> firstHalf = generateAllSums(arr, 0, mid);

        // Generate all possible sums for second half
        Set<Integer> secondHalf = generateAllSums(arr, mid, n);

        // Check if any combination gives target sum
        for (int sum1 : firstHalf) {
            int needed = target - sum1;
            if (secondHalf.contains(needed)) {
                return true;
            }
        }

        return false;
    }

    private Set<Integer> generateAllSums(int[] arr, int start, int end) {
        Set<Integer> sums = new HashSet<>();
        int size = end - start;

        // Generate all 2^size subsets
        for (int mask = 0; mask < (1 << size); mask++) {
            int sum = 0;
            for (int i = 0; i < size; i++) {
                if ((mask & (1 << i)) != 0) {
                    sum += arr[start + i];
                }
            }
            sums.add(sum);
        }

        return sums;
    }
}

// Solution 8: Optimized with Early Termination and Sorting
// Time: O(N*Sum), Space: O(Sum)
class Solution8_EarlyTermination {
    public boolean canPartition(int[] arr) {
        int totalSum = Arrays.stream(arr).sum();

        if (totalSum % 2 != 0) {
            return false;
        }

        int target = totalSum / 2;
        int n = arr.length;

        // Quick checks
        int maxElement = Arrays.stream(arr).max().orElse(0);
        if (maxElement > target) {
            return false;
        }

        if (maxElement == target) {
            return true;
        }

        // Sort in descending order for better early termination
        Integer[] sortedArr = new Integer[n];
        for (int i = 0; i < n; i++) {
            sortedArr[i] = arr[i];
        }
        Arrays.sort(sortedArr, Collections.reverseOrder());

        boolean[] dp = new boolean[target + 1];
        dp[0] = true;

        for (int num : sortedArr) {
            for (int sum = target; sum >= num; sum--) {
                if (dp[sum - num]) {
                    dp[sum] = true;
                    if (sum == target) {
                        return true;
                    }
                }
            }
        }

        return dp[target];
    }
}

// Solution 9: Using Recursive Backtracking with Pruning
// Time: O(2^N) worst case, much better with pruning, Space: O(N)
class Solution9_BacktrackingPruned {
    public boolean canPartition(int[] arr) {
        int totalSum = Arrays.stream(arr).sum();

        if (totalSum % 2 != 0) {
            return false;
        }

        int target = totalSum / 2;

        // Sort in descending order for better pruning
        Arrays.sort(arr);
        for (int i = 0; i < arr.length / 2; i++) {
            int temp = arr[i];
            arr[i] = arr[arr.length - 1 - i];
            arr[arr.length - 1 - i] = temp;
        }

        return backtrack(arr, 0, target, 0);
    }

    private boolean backtrack(int[] arr, int index, int target, int currentSum) {
        if (currentSum == target) {
            return true;
        }

        if (index >= arr.length || currentSum > target) {
            return false;
        }

        // Include current element
        if (backtrack(arr, index + 1, target, currentSum + arr[index])) {
            return true;
        }

        // Skip duplicates optimization
        while (index + 1 < arr.length && arr[index] == arr[index + 1]) {
            index++;
        }

        // Exclude current element
        return backtrack(arr, index + 1, target, currentSum);
    }
}

// Test class with comprehensive testing
class TestEqualSumPartition {
    public static void main(String[] args) {
        // Initialize all solutions
        Solution1_BruteForce sol1 = new Solution1_BruteForce();
        Solution2_TopDownMemo sol2 = new Solution2_TopDownMemo();
        Solution3_BottomUp2D sol3 = new Solution3_BottomUp2D();
        Solution4_SpaceOptimized1D sol4 = new Solution4_SpaceOptimized1D();
        Solution5_WithPartition sol5 = new Solution5_WithPartition();
        Solution6_BitsetOptimized sol6 = new Solution6_BitsetOptimized();
        Solution7_MeetInMiddle sol7 = new Solution7_MeetInMiddle();
        Solution8_EarlyTermination sol8 = new Solution8_EarlyTermination();
        Solution9_BacktrackingPruned sol9 = new Solution9_BacktrackingPruned();

        // Test case 1: Can partition [1,5,11,5] -> [1,5,5] and [11]
        int[] arr1 = { 1, 5, 11, 5 };
        System.out.println("Test 1 - Array: " + Arrays.toString(arr1));
        System.out.println("Expected: true (partitions: [1,5,5] and [11])");
        System.out.println("Brute Force: " + sol1.canPartition(arr1.clone()));
        System.out.println("Top-Down Memo: " + sol2.canPartition(arr1.clone()));
        System.out.println("Bottom-Up 2D: " + sol3.canPartition(arr1.clone()));
        System.out.println("Space Optimized: " + sol4.canPartition(arr1.clone()));
        System.out.println("Bitset Optimized: " + sol6.canPartition(arr1.clone()));
        System.out.println("Meet in Middle: " + sol7.canPartition(arr1.clone()));
        System.out.println("Early Termination: " + sol8.canPartition(arr1.clone()));
        System.out.println("Backtracking: " + sol9.canPartition(arr1.clone()));

        // Show actual partition
        Solution5_WithPartition.Result result1 = sol5.canPartitionWithSubsets(arr1.clone());
        System.out.println(result1);

        // Test case 2: Cannot partition [1,2,3,5]
        int[] arr2 = { 1, 2, 3, 5 };
        System.out.println("\nTest 2 - Array: " + Arrays.toString(arr2));
        System.out.println("Expected: false (sum = 11, odd)");
        System.out.println("Space Optimized: " + sol4.canPartition(arr2.clone()));

        Solution5_WithPartition.Result result2 = sol5.canPartitionWithSubsets(arr2.clone());
        System.out.println(result2);

        // Test case 3: Single element
        int[] arr3 = { 5 };
        System.out.println("\nTest 3 - Array: " + Arrays.toString(arr3));
        System.out.println("Expected: false (cannot split single element)");
        System.out.println("Space Optimized: " + sol4.canPartition(arr3.clone()));

        // Test case 4: Two equal elements
        int[] arr4 = { 2, 2 };
        System.out.println("\nTest 4 - Array: " + Arrays.toString(arr4));
        System.out.println("Expected: true ([2] and [2])");
        System.out.println("Space Optimized: " + sol4.canPartition(arr4.clone()));

        Solution5_WithPartition.Result result4 = sol5.canPartitionWithSubsets(arr4.clone());
        System.out.println(result4);

        // Test case 5: Large equal elements
        int[] arr5 = { 100, 100, 100, 100, 100, 100, 100, 100 };
        System.out.println("\nTest 5 - Array: " + Arrays.toString(arr5));
        System.out.println("Expected: true (split into two groups of 4)");
        System.out.println("Space Optimized: " + sol4.canPartition(arr5.clone()));

        // Test case 6: Edge case - all same elements, odd count
        int[] arr6 = { 1, 1, 1 };
        System.out.println("\nTest 6 - Array: " + Arrays.toString(arr6));
        System.out.println("Expected: false (sum = 3, odd)");
        System.out.println("Space Optimized: " + sol4.canPartition(arr6.clone()));

        // Test case 7: Complex case
        int[] arr7 = { 1, 2, 5, 10, 4, 6 };
        System.out.println("\nTest 7 - Array: " + Arrays.toString(arr7));
        System.out.println("Expected: true (multiple valid partitions possible)");
        System.out.println("Space Optimized: " + sol4.canPartition(arr7.clone()));

        Solution5_WithPartition.Result result7 = sol5.canPartitionWithSubsets(arr7.clone());
        System.out.println(result7);

        // Performance comparison
        int[] performanceTest = generateRandomArray(20, 1, 50);
        System.out.println("\nPerformance Test - Array size: 20");
        System.out.println("Array: " + Arrays.toString(performanceTest));

        long start, end;

        // Space optimized DP
        start = System.currentTimeMillis();
        boolean result = sol4.canPartition(performanceTest.clone());
        end = System.currentTimeMillis();
        System.out.println("Space Optimized: " + result + " in " + (end - start) + "ms");

        // Bitset optimized
        start = System.currentTimeMillis();
        boolean result3 = sol6.canPartition(performanceTest.clone());
        end = System.currentTimeMillis();
        System.out.println("Bitset Optimized: " + result3 + " in " + (end - start) + "ms");

        // Meet in middle
        start = System.currentTimeMillis();
        boolean result5 = sol7.canPartition(performanceTest.clone());
        end = System.currentTimeMillis();
        System.out.println("Meet in Middle: " + result5 + " in " + (end - start) + "ms");

        System.out.println("\nAlgorithm Recommendations:");
        System.out.println("• General use: Solution 4 (Space Optimized 1D)");
        System.out.println("• Speed critical: Solution 6 (Bitset) for small sums");
        System.out.println("• Large arrays: Solution 7 (Meet in Middle)");
        System.out.println("• Need actual partition: Solution 5 (With Reconstruction)");
        System.out.println("• Learning: Solution 3 (Bottom-Up 2D) - most intuitive");
    }

    private static int[] generateRandomArray(int n, int min, int max) {
        Random rand = new Random(42); // Fixed seed for reproducibility
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = rand.nextInt(max - min + 1) + min;
        }
        return arr;
    }
}