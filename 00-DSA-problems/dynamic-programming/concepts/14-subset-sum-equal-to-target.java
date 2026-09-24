/*
 * We are given an array ‘ARR’ with N positive integers. We need to find if
 * there is a subset in “ARR” with a sum equal to K. If there is, return true
 * else return false.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

class SubsetSumEqualToTargetRec {

    public boolean isSubsetSumEqualToTarget(int[] arr, int target) {
        return this.isSubsetSumEqualToTarget(arr, target, arr.length - 1);
    }

    private boolean isSubsetSumEqualToTarget(int[] arr, int target, int index) {
        if (index < 0) {
            return false;
        }
        if (target == 0) {
            return true;
        }
        if (index == 0) {
            return arr[0] == target;
        }
        boolean skip = this.isSubsetSumEqualToTarget(arr, target, index - 1);
        boolean pick = false;
        if (target >= arr[index]) {
            pick = this.isSubsetSumEqualToTarget(arr, target - arr[index], index - 1);
        }
        return skip || pick;
    }

}

class SubsetSumEqualToTargetRecWithMemo {

    public boolean isSubsetSumEqualToTarget(int[] arr, int target) {
        Boolean[][] memo = new Boolean[arr.length][target + 1];
        return this.isSubsetSumEqualToTarget(arr, memo, target, arr.length - 1);
    }

    private boolean isSubsetSumEqualToTarget(int[] arr, Boolean[][] memo, int target, int index) {
        if (index < 0) {
            return false;
        }
        if (target == 0) {
            return true;
        }
        if (index == 0) {
            return arr[0] == target;
        }
        if (memo[index][target] != null) {
            return memo[index][target];
        }
        boolean skip = this.isSubsetSumEqualToTarget(arr, memo, target, index - 1);
        boolean pick = false;
        if (target >= arr[index]) {
            pick = this.isSubsetSumEqualToTarget(arr, memo, target - arr[index], index - 1);
        }
        return memo[index][target] = skip || pick;
    }

}

class SubsetSumEqualToTargetRecWithMemo2 {

    public boolean isSubsetSumEqualToTarget(int[] arr, int target) {
        if (target < 0)
            return false;
        int n = arr.length;
        if (target == 0)
            return true;
        if (n == 0)
            return false;

        // If target is larger than the sum of all elements, quick false (optional)
        int sum = 0;
        for (int v : arr)
            sum += v;
        if (target > sum)
            return false;

        // IMPORTANT: second dimension must be target+1 so we can index memo[*][target]
        Boolean[][] memo = new Boolean[n][target + 1];
        return isSubsetSumEqualToTarget(arr, memo, target, n - 1);
    }

    private boolean isSubsetSumEqualToTarget(int[] arr, Boolean[][] memo, int target, int index) {
        // base cases
        if (target == 0)
            return true; // found exact sum
        if (index < 0)
            return false; // no items left and target != 0
        if (memo[index][target] != null)
            return memo[index][target];

        if (index == 0) { // only one element to consider
            boolean res = (arr[0] == target);
            memo[index][target] = res;
            return res;
        }

        // skip current element
        boolean skip = isSubsetSumEqualToTarget(arr, memo, target, index - 1);

        // pick current element (only if it doesn't exceed target)
        boolean pick = false;
        if (target >= arr[index]) {
            pick = isSubsetSumEqualToTarget(arr, memo, target - arr[index], index - 1);
        }

        memo[index][target] = skip || pick;
        return memo[index][target];
    }

    // small test
    public static void main(String[] args) {
        SubsetSumEqualToTargetRecWithMemo solver = new SubsetSumEqualToTargetRecWithMemo();
        int[] arr = { 3, 34, 4, 12, 5, 2 };
        System.out.println(solver.isSubsetSumEqualToTarget(arr, 9)); // true (4+5)
        System.out.println(solver.isSubsetSumEqualToTarget(arr, 30)); // false
    }

}

// Solution 1: Brute Force - Generate All Subsets
// Time: O(2^N), Space: O(N) for recursion stack
class Solution1_BruteForce {
    public boolean subsetSumToK(int n, int k, int[] arr) {
        return generateAllSubsets(arr, 0, 0, k);
    }

    private boolean generateAllSubsets(int[] arr, int index, int currentSum, int target) {
        // Base case: processed all elements
        if (index == arr.length) {
            return currentSum == target;
        }

        // Include current element
        if (generateAllSubsets(arr, index + 1, currentSum + arr[index], target)) {
            return true;
        }

        // Exclude current element
        if (generateAllSubsets(arr, index + 1, currentSum, target)) {
            return true;
        }

        return false;
    }
}

// Solution 2: Recursion with Memoization (Top-Down DP)
// Time: O(N*K), Space: O(N*K)
class Solution2_TopDownMemo {
    public boolean subsetSumToK(int n, int k, int[] arr) {
        // memo[index][sum] = result for subproblem
        Boolean[][] memo = new Boolean[n][k + 1];
        return canPartition(arr, 0, k, memo);
    }

    private boolean canPartition(int[] arr, int index, int remainingSum, Boolean[][] memo) {
        // Base cases
        if (remainingSum == 0)
            return true; // Found target sum
        if (index >= arr.length || remainingSum < 0)
            return false; // Invalid state

        // Check memo
        if (memo[index][remainingSum] != null) {
            return memo[index][remainingSum];
        }

        // Include current element
        boolean include = canPartition(arr, index + 1, remainingSum - arr[index], memo);

        // Exclude current element
        boolean exclude = canPartition(arr, index + 1, remainingSum, memo);

        memo[index][remainingSum] = include || exclude;
        return memo[index][remainingSum];
    }
}

// Solution 3: Bottom-Up Dynamic Programming (2D)
// Time: O(N*K), Space: O(N*K)
class Solution3_BottomUp2D {
    public boolean subsetSumToK(int n, int k, int[] arr) {
        // dp[i][j] = true if sum j can be achieved using first i elements
        boolean[][] dp = new boolean[n + 1][k + 1];

        // Base case: sum 0 can always be achieved (empty subset)
        for (int i = 0; i <= n; i++) {
            dp[i][0] = true;
        }

        // Fill the DP table
        for (int i = 1; i <= n; i++) {
            for (int sum = 1; sum <= k; sum++) {
                // Exclude current element
                dp[i][sum] = dp[i - 1][sum];

                // Include current element if possible
                if (sum >= arr[i - 1]) {
                    dp[i][sum] = dp[i][sum] || dp[i - 1][sum - arr[i - 1]];
                }
            }
        }

        return dp[n][k];
    }
}

// Solution 4: Space Optimized Bottom-Up (1D)
// Time: O(N*K), Space: O(K)
class Solution4_SpaceOptimized1D {
    public boolean subsetSumToK(int n, int k, int[] arr) {
        // dp[j] = true if sum j can be achieved
        boolean[] dp = new boolean[k + 1];

        // Base case
        dp[0] = true;

        // Process each element
        for (int i = 0; i < n; i++) {
            // Traverse from right to left to avoid using updated values
            for (int sum = k; sum >= arr[i]; sum--) {
                dp[sum] = dp[sum] || dp[sum - arr[i]];
            }
        }

        return dp[k];
    }
}

// Solution 5: With Subset Reconstruction
// Time: O(N*K), Space: O(N*K)
class Solution5_WithSubset {
    public class Result {
        boolean exists;
        List<Integer> subset;

        Result(boolean exists, List<Integer> subset) {
            this.exists = exists;
            this.subset = new ArrayList<>(subset);
        }

        @Override
        public String toString() {
            if (!exists) {
                return "No subset with target sum exists";
            }
            return "Subset exists: " + subset + " (sum = " +
                    subset.stream().mapToInt(Integer::intValue).sum() + ")";
        }
    }

    public Result subsetSumToKWithSubset(int n, int k, int[] arr) {
        // dp[i][j] = true if sum j can be achieved using first i elements
        boolean[][] dp = new boolean[n + 1][k + 1];

        // Base case
        for (int i = 0; i <= n; i++) {
            dp[i][0] = true;
        }

        // Fill DP table
        for (int i = 1; i <= n; i++) {
            for (int sum = 1; sum <= k; sum++) {
                dp[i][sum] = dp[i - 1][sum];

                if (sum >= arr[i - 1]) {
                    dp[i][sum] = dp[i][sum] || dp[i - 1][sum - arr[i - 1]];
                }
            }
        }

        if (!dp[n][k]) {
            return new Result(false, new ArrayList<>());
        }

        // Reconstruct subset
        List<Integer> subset = new ArrayList<>();
        int i = n, sum = k;

        while (i > 0 && sum > 0) {
            // If current state didn't come from previous row,
            // then current element is included
            if (!dp[i - 1][sum]) {
                subset.add(arr[i - 1]);
                sum -= arr[i - 1];
            }
            i--;
        }

        return new Result(true, subset);
    }
}

// Solution 6: Bitset Optimization (for small sums)
// Time: O(N*K/32), Space: O(K/32)
class Solution6_BitsetOptimized {
    public boolean subsetSumToK(int n, int k, int[] arr) {
        // Use BitSet for space and time optimization
        BitSet dp = new BitSet(k + 1);
        dp.set(0); // Base case: sum 0 is always possible

        for (int num : arr) {
            // Create a copy shifted by num positions
            BitSet shifted = (BitSet) dp.clone();

            // Shift bits to represent adding current number
            for (int i = dp.nextSetBit(0); i >= 0 && i + num <= k; i = dp.nextSetBit(i + 1)) {
                shifted.set(i + num);
            }

            dp = shifted;
        }

        return dp.get(k);
    }
}

// Solution 7: Early Termination Optimizations
// Time: O(N*K), Space: O(K) - with pruning
class Solution7_EarlyTermination {
    public boolean subsetSumToK(int n, int k, int[] arr) {
        // Early termination checks
        int totalSum = Arrays.stream(arr).sum();
        if (totalSum < k)
            return false; // Impossible to achieve k
        if (totalSum == k)
            return true; // All elements sum to k

        // Check if k exists in array
        for (int num : arr) {
            if (num == k)
                return true;
        }

        // Sort array in descending order for better pruning
        Integer[] sortedArr = new Integer[n];
        for (int i = 0; i < n; i++) {
            sortedArr[i] = arr[i];
        }
        Arrays.sort(sortedArr, Collections.reverseOrder());

        boolean[] dp = new boolean[k + 1];
        dp[0] = true;

        for (int num : sortedArr) {
            if (num > k)
                continue; // Skip elements larger than target

            for (int sum = k; sum >= num; sum--) {
                if (dp[sum - num]) {
                    dp[sum] = true;
                    if (sum == k)
                        return true; // Early termination
                }
            }
        }

        return dp[k];
    }
}

// Solution 8: Meet in the Middle (for large N, small K)
// Time: O(N * 2^(N/2)), Space: O(2^(N/2))
class Solution8_MeetInMiddle {
    public boolean subsetSumToK(int n, int k, int[] arr) {
        if (n <= 20) {
            // Use regular DP for small arrays
            return new Solution4_SpaceOptimized1D().subsetSumToK(n, k, arr);
        }

        int mid = n / 2;

        // Generate all possible sums for first half
        Set<Integer> firstHalf = generateAllSums(arr, 0, mid);

        // Generate all possible sums for second half
        Set<Integer> secondHalf = generateAllSums(arr, mid, n);

        // Check if any combination gives target sum
        for (int sum1 : firstHalf) {
            int needed = k - sum1;
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

// Solution 9: Iterative Deepening (for finding small subsets)
// Time: O(N^min_subset_size), Space: O(min_subset_size)
class Solution9_IterativeDeepening {
    public boolean subsetSumToK(int n, int k, int[] arr) {
        // Try subsets of increasing sizes
        for (int subsetSize = 1; subsetSize <= n; subsetSize++) {
            if (hasSubsetOfSize(arr, k, subsetSize, 0, new ArrayList<>())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSubsetOfSize(int[] arr, int target, int size, int start, List<Integer> current) {
        if (current.size() == size) {
            return current.stream().mapToInt(Integer::intValue).sum() == target;
        }

        if (start >= arr.length)
            return false;

        // Include current element
        current.add(arr[start]);
        if (hasSubsetOfSize(arr, target, size, start + 1, current)) {
            return true;
        }
        current.remove(current.size() - 1);

        // Skip current element
        return hasSubsetOfSize(arr, target, size, start + 1, current);
    }
}

// Test class with comprehensive testing
class TestSubsetSum {

    public static void main(String[] args) {
        // Initialize all solutions
        Solution1_BruteForce sol1 = new Solution1_BruteForce();
        Solution2_TopDownMemo sol2 = new Solution2_TopDownMemo();
        Solution3_BottomUp2D sol3 = new Solution3_BottomUp2D();
        Solution4_SpaceOptimized1D sol4 = new Solution4_SpaceOptimized1D();
        Solution5_WithSubset sol5 = new Solution5_WithSubset();
        Solution6_BitsetOptimized sol6 = new Solution6_BitsetOptimized();
        Solution7_EarlyTermination sol7 = new Solution7_EarlyTermination();
        Solution8_MeetInMiddle sol8 = new Solution8_MeetInMiddle();
        Solution9_IterativeDeepening sol9 = new Solution9_IterativeDeepening();

        // Test case 1: Basic example
        int[] arr1 = { 4, 3, 2, 1 };
        int k1 = 5;
        int n1 = arr1.length;

        System.out.println("Test 1 - Array: " + Arrays.toString(arr1) + ", K: " + k1);
        System.out.println("Expected: true (subsets: [4,1], [3,2])");
        System.out.println("Brute Force: " + sol1.subsetSumToK(n1, k1, arr1));
        System.out.println("Top-Down Memo: " + sol2.subsetSumToK(n1, k1, arr1));
        System.out.println("Bottom-Up 2D: " + sol3.subsetSumToK(n1, k1, arr1));
        System.out.println("Space Optimized: " + sol4.subsetSumToK(n1, k1, arr1));
        System.out.println("Bitset Optimized: " + sol6.subsetSumToK(n1, k1, arr1));
        System.out.println("Early Termination: " + sol7.subsetSumToK(n1, k1, arr1));
        System.out.println("Meet in Middle: " + sol8.subsetSumToK(n1, k1, arr1));
        System.out.println("Iterative Deepening: " + sol9.subsetSumToK(n1, k1, arr1));

        // Show actual subset
        Solution5_WithSubset.Result result1 = sol5.subsetSumToKWithSubset(n1, k1, arr1);
        System.out.println(result1);

        // Test case 2: Impossible case
        int[] arr2 = { 1, 2, 3 };
        int k2 = 10;
        int n2 = arr2.length;

        System.out.println("\nTest 2 - Array: " + Arrays.toString(arr2) + ", K: " + k2);
        System.out.println("Expected: false");
        System.out.println("Space Optimized: " + sol4.subsetSumToK(n2, k2, arr2));

        Solution5_WithSubset.Result result2 = sol5.subsetSumToKWithSubset(n2, k2, arr2);
        System.out.println(result2);

        // Test case 3: Single element
        int[] arr3 = { 5 };
        int k3 = 5;
        int n3 = arr3.length;

        System.out.println("\nTest 3 - Array: " + Arrays.toString(arr3) + ", K: " + k3);
        System.out.println("Expected: true");
        System.out.println("Space Optimized: " + sol4.subsetSumToK(n3, k3, arr3));

        // Test case 4: Zero sum
        int[] arr4 = { 1, 2, 3 };
        int k4 = 0;
        int n4 = arr4.length;

        System.out.println("\nTest 4 - Array: " + Arrays.toString(arr4) + ", K: " + k4);
        System.out.println("Expected: true (empty subset)");
        System.out.println("Space Optimized: " + sol4.subsetSumToK(n4, k4, arr4));

        // Test case 5: All elements sum to target
        int[] arr5 = { 1, 2, 3, 4 };
        int k5 = 10;
        int n5 = arr5.length;

        System.out.println("\nTest 5 - Array: " + Arrays.toString(arr5) + ", K: " + k5);
        System.out.println("Expected: true (entire array)");
        System.out.println("Space Optimized: " + sol4.subsetSumToK(n5, k5, arr5));

        Solution5_WithSubset.Result result5 = sol5.subsetSumToKWithSubset(n5, k5, arr5);
        System.out.println(result5);

        // Performance comparison
        int[] largeArr = generateRandomArray(25, 1, 20);
        int largeK = 50;
        int largeN = largeArr.length;

        System.out.println("\nPerformance Test (N=25, K=50):");

        long start, end;

        // Space optimized DP
        start = System.currentTimeMillis();
        boolean result = sol4.subsetSumToK(largeN, largeK, largeArr);
        end = System.currentTimeMillis();
        System.out.println("Space Optimized DP: " + result + " in " + (end - start) + "ms");

        // Meet in middle (for comparison)
        start = System.currentTimeMillis();
        boolean result3 = sol8.subsetSumToK(largeN, largeK, largeArr);
        end = System.currentTimeMillis();
        System.out.println("Meet in Middle: " + result3 + " in " + (end - start) + "ms");
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