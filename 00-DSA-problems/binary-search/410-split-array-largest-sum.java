class SplitArrayLargestSum {

    // Approach 1: Binary Search on Answer (Optimal)
    // Time: O(n * log(sum)), Space: O(1)
    public int splitArray1(int[] nums, int k) {
        int left = getMax(nums); // Min possible answer: largest element
        int right = getSum(nums); // Max possible answer: sum of all elements

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (canSplit(nums, k, mid)) {
                // Can split with max sum = mid, try smaller
                right = mid;
            } else {
                // Cannot split, need larger max sum
                left = mid + 1;
            }
        }

        return left;
    }

    // Helper: Check if we can split into k subarrays with max sum <= maxSum
    private boolean canSplit(int[] nums, int k, int maxSum) {
        int subarrays = 1;
        int currentSum = 0;

        for (int num : nums) {
            if (currentSum + num > maxSum) {
                // Start new subarray
                subarrays++;
                currentSum = num;

                // Early termination
                if (subarrays > k) {
                    return false;
                }
            } else {
                currentSum += num;
            }
        }

        return subarrays <= k;
    }

    private int getMax(int[] arr) {
        int max = arr[0];
        for (int val : arr) {
            max = Math.max(max, val);
        }
        return max;
    }

    private int getSum(int[] arr) {
        int sum = 0;
        for (int val : arr) {
            sum += val;
        }
        return sum;
    }

    // Approach 2: Binary Search with Detailed Subarray Counting
    // Time: O(n * log(sum)), Space: O(1)
    public int splitArray2(int[] nums, int k) {
        int minPossible = 0;
        int maxPossible = 0;

        for (int num : nums) {
            minPossible = Math.max(minPossible, num);
            maxPossible += num;
        }

        int left = minPossible;
        int right = maxPossible;

        while (left < right) {
            int mid = left + (right - left) / 2;
            int subarraysNeeded = countSubarrays(nums, mid);

            if (subarraysNeeded <= k) {
                right = mid;
            } else {
                left = mid + 1;
            }
        }

        return left;
    }

    private int countSubarrays(int[] nums, int maxSum) {
        int count = 1;
        int sum = 0;

        for (int num : nums) {
            if (sum + num > maxSum) {
                count++;
                sum = num;
            } else {
                sum += num;
            }
        }

        return count;
    }

    // Approach 3: Binary Search with Alternative Logic
    // Time: O(n * log(sum)), Space: O(1)
    public int splitArray3(int[] nums, int k) {
        int left = 0, right = 0;

        for (int num : nums) {
            left = Math.max(left, num);
            right += num;
        }

        int result = right;

        while (left <= right) {
            int mid = left + (right - left) / 2;

            if (isPossible(nums, k, mid)) {
                result = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }

        return result;
    }

    private boolean isPossible(int[] nums, int k, int maxSum) {
        int splits = 1;
        int currentSum = 0;

        for (int num : nums) {
            currentSum += num;

            if (currentSum > maxSum) {
                splits++;
                currentSum = num;

                if (splits > k) {
                    return false;
                }
            }
        }

        return true;
    }

    // Approach 4: Dynamic Programming (Alternative Solution)
    // Time: O(n^2 * k), Space: O(n * k)
    public int splitArray4(int[] nums, int k) {
        int n = nums.length;

        // dp[i][j] = minimum largest sum to split nums[0..i-1] into j subarrays
        int[][] dp = new int[n + 1][k + 1];

        // Initialize with max values
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= k; j++) {
                dp[i][j] = Integer.MAX_VALUE;
            }
        }

        // Prefix sum for quick range sum calculation
        int[] prefixSum = new int[n + 1];
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + nums[i];
        }

        // Base case: 0 elements, 0 subarrays
        dp[0][0] = 0;

        // Fill DP table
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= Math.min(i, k); j++) {
                for (int p = j - 1; p < i; p++) {
                    // Try splitting at position p
                    // nums[0..p-1] into j-1 subarrays, nums[p..i-1] as jth subarray
                    int currentSum = prefixSum[i] - prefixSum[p];
                    int maxSum = Math.max(dp[p][j - 1], currentSum);
                    dp[i][j] = Math.min(dp[i][j], maxSum);
                }
            }
        }

        return dp[n][k];
    }

    // Test cases with detailed visualization
    public static void main(String[] args) {
        SplitArrayLargestSum solution = new SplitArrayLargestSum();

        // Test Case 1
        int[] nums1 = { 7, 2, 5, 10, 8 };
        int k1 = 2;
        int result1 = solution.splitArray1(nums1, k1);
        System.out.println("Test 1: " + result1); // Output: 18
        visualizeSplit(nums1, k1, result1);

        // Test Case 2
        int[] nums2 = { 1, 2, 3, 4, 5 };
        int k2 = 2;
        int result2 = solution.splitArray1(nums2, k2);
        System.out.println("\nTest 2: " + result2); // Output: 9
        visualizeSplit(nums2, k2, result2);

        // Test Case 3: k = n (each element is a subarray)
        int[] nums3 = { 10, 20, 30, 40 };
        int k3 = 4;
        int result3 = solution.splitArray1(nums3, k3);
        System.out.println("\nTest 3: " + result3); // Output: 40

        // Test Case 4: k = 1 (entire array)
        int[] nums4 = { 1, 2, 3, 4, 5 };
        int k4 = 1;
        int result4 = solution.splitArray1(nums4, k4);
        System.out.println("Test 4: " + result4); // Output: 15

        // Test Case 5: Large values
        int[] nums5 = { 1, 4, 4 };
        int k5 = 3;
        int result5 = solution.splitArray1(nums5, k5);
        System.out.println("Test 5: " + result5); // Output: 4

        // Compare Binary Search vs DP for small input
        System.out.println("\nComparing Binary Search vs DP:");
        System.out.println("Binary Search: " + solution.splitArray1(nums2, k2));
        System.out.println("DP: " + solution.splitArray4(nums2, k2));
    }

    private static void visualizeSplit(int[] nums, int k, int maxSum) {
        System.out.println("Array: " + java.util.Arrays.toString(nums));
        System.out.println("Split into " + k + " subarrays");
        System.out.println("Minimized largest sum: " + maxSum);

        // Show the actual split using greedy approach
        System.out.println("Optimal split:");
        int subarrayNum = 1;
        int currentSum = 0;
        StringBuilder subarray = new StringBuilder("[");

        for (int i = 0; i < nums.length; i++) {
            if (currentSum + nums[i] > maxSum) {
                // Print current subarray
                subarray.append("]");
                System.out.println("  Subarray " + subarrayNum + ": " +
                        subarray + " (sum = " + currentSum + ")");

                // Start new subarray
                subarrayNum++;
                subarray = new StringBuilder("[" + nums[i]);
                currentSum = nums[i];
            } else {
                if (subarray.length() > 1) {
                    subarray.append(", ");
                }
                subarray.append(nums[i]);
                currentSum += nums[i];
            }
        }

        // Print last subarray
        subarray.append("]");
        System.out.println("  Subarray " + subarrayNum + ": " +
                subarray + " (sum = " + currentSum + ")");
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * PROBLEM ANALYSIS:
 * - Split array into k non-empty contiguous subarrays
 * - Goal: minimize the maximum sum among all subarrays
 * - Cannot reorder elements (subarrays must be contiguous)
 * 
 * KEY INSIGHTS:
 * 1. Answer range:
 * - Minimum: max(nums) - at least one subarray contains largest element
 * - Maximum: sum(nums) - all elements in one subarray (k=1)
 * 
 * 2. Binary search property (Monotonic):
 * - If we can split with max sum = M, we can also split with max sum > M
 * - If we can't split with max sum = M, we can't split with max sum < M
 * - Pattern: [NO, NO, ..., NO, YES, YES, ..., YES]
 * - We want first YES = minimum valid max sum
 * 
 * 3. Greedy validation:
 * - For each candidate max sum, check if k subarrays suffice
 * - Greedily add elements to current subarray
 * - Start new subarray when next element would exceed max sum
 * - If subarrays needed ≤ k, this max sum works
 * 
 * ALGORITHM:
 * 1. Binary search on max sum in range [max(nums), sum(nums)]
 * 2. For each candidate max sum:
 * - Simulate greedy splitting
 * - Count subarrays needed
 * 3. If subarrays ≤ k: max sum works, try smaller (search left)
 * 4. If subarrays > k: need larger max sum (search right)
 * 
 * EXAMPLE WALKTHROUGH: nums=[7,2,5,10,8], k=2
 * 
 * Initial: left=10 (max), right=32 (sum)
 * 
 * Iteration 1: mid=21
 * Greedy split with max sum 21:
 * - Subarray 1: 7+2+5 = 14 (can't add 10)
 * - Subarray 2: 10+8 = 18
 * - Total: 2 subarrays ≤ 2 ✓
 * Try smaller: right=21
 * 
 * Iteration 2: left=10, right=21, mid=15
 * Greedy split with max sum 15:
 * - Subarray 1: 7+2+5 = 14 (can't add 10)
 * - Subarray 2: 10 (can't add 8, exceeds 15)
 * - Subarray 3: 8
 * - Total: 3 subarrays > 2 ✗
 * Need larger: left=16
 * 
 * Iteration 3: left=16, right=21, mid=18
 * Greedy split with max sum 18:
 * - Subarray 1: 7+2+5 = 14 (can't add 10)
 * - Subarray 2: 10+8 = 18
 * - Total: 2 subarrays ≤ 2 ✓
 * Try smaller: right=18
 * 
 * Iteration 4: left=16, right=18, mid=17
 * Greedy split with max sum 17:
 * - Subarray 1: 7+2+5 = 14 (can't add 10)
 * - Subarray 2: 10 (can't add 8, would be 18 > 17)
 * - Subarray 3: 8
 * - Total: 3 subarrays > 2 ✗
 * Need larger: left=18
 * 
 * Result: left=18, right=18 → Answer: 18
 * 
 * Optimal split: [7,2,5] (sum=14), [10,8] (sum=18)
 * Maximum sum = 18 ✓
 * 
 * EXAMPLE 2: nums=[1,2,3,4,5], k=2
 * 
 * Possible splits:
 * - [1] [2,3,4,5]: max(1, 14) = 14
 * - [1,2] [3,4,5]: max(3, 12) = 12
 * - [1,2,3] [4,5]: max(6, 9) = 9 ← optimal
 * - [1,2,3,4] [5]: max(10, 5) = 10
 * 
 * Binary search finds: 9
 * 
 * COMPLEXITY ANALYSIS:
 * 
 * Binary Search Approach:
 * - Time: O(n * log(sum))
 * - Binary search: log(sum - max) ≈ log(sum) iterations
 * - Each validation: O(n) to traverse array
 * - Space: O(1) - constant extra space
 * 
 * Dynamic Programming Approach:
 * - Time: O(n² * k) - three nested loops
 * - Space: O(n * k) - DP table
 * - More intuitive but slower for large inputs
 * 
 * EDGE CASES:
 * 1. k = 1: answer = sum(nums)
 * 2. k = n: answer = max(nums)
 * 3. All elements equal: answer = ceil(sum / k) * element
 * 4. k > n: impossible (but problem guarantees valid k)
 * 5. Large single element: answer ≥ that element
 * 
 * WHY BINARY SEARCH IS OPTIMAL:
 * 1. Search space is continuous and sorted
 * 2. Validation is monotonic (larger max sum → easier to satisfy)
 * 3. O(log sum) vs O(k*n²) for DP
 * 4. O(1) space vs O(k*n) for DP
 * 
 * GREEDY VALIDATION CORRECTNESS:
 * The greedy approach (fill subarrays as much as possible) is optimal because:
 * - We want to use as few subarrays as possible
 * - Maximizing each subarray's sum (within limit) minimizes total subarrays
 * - If greedy uses ≤ k subarrays, any other strategy also uses ≤ k
 * - If greedy uses > k subarrays, no strategy can use ≤ k
 * 
 * PRACTICAL APPLICATIONS:
 * - Task scheduling: minimize maximum workload per worker
 * - Memory allocation: split data to minimize largest chunk
 * - Network routing: balance load across servers
 * - File distribution: minimize largest file transfer time
 */
