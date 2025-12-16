import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LongestSubarrayZeroSum {

    // Solution 1: Brute Force (Check all subarrays)
    // Time: O(n²), Space: O(1)
    public int maxLenBruteForce(int[] nums) {
        int maxLen = 0;
        int n = nums.length;

        // Try all possible subarrays
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int j = i; j < n; j++) {
                sum += nums[j];

                // If sum becomes 0, update maxLen
                if (sum == 0) {
                    maxLen = Math.max(maxLen, j - i + 1);
                }
            }
        }

        return maxLen;
    }

    // Solution 2: Optimal - HashMap with Prefix Sum
    // Time: O(n), Space: O(n)
    public int maxLenOptimal(int[] nums) {
        /*
         * Key Insight:
         * If prefixSum[i] == prefixSum[j], then sum from (i+1) to j is 0
         * Store first occurrence of each prefix sum
         * If we see same prefix sum again, we found a zero-sum subarray
         */

        Map<Integer, Integer> prefixSumIndex = new HashMap<>();
        prefixSumIndex.put(0, -1); // Base case: prefix sum 0 at index -1

        int maxLen = 0;
        int prefixSum = 0;

        for (int i = 0; i < nums.length; i++) {
            prefixSum += nums[i];

            if (prefixSumIndex.containsKey(prefixSum)) {
                // Found a subarray with sum 0
                int prevIndex = prefixSumIndex.get(prefixSum);
                int currentLen = i - prevIndex;
                maxLen = Math.max(maxLen, currentLen);
            } else {
                // Store first occurrence of this prefix sum
                prefixSumIndex.put(prefixSum, i);
            }
        }

        return maxLen;
    }

    // Solution 3: Optimal with Detailed Comments
    // Time: O(n), Space: O(n)
    public int maxLenDetailed(int[] nums) {
        /*
         * Example: [15, -2, 2, -8, 1, 7, 10, 23]
         * 
         * Index 0: sum=15, map={0=-1, 15=0}
         * Index 1: sum=13, map={0=-1, 15=0, 13=1}
         * Index 2: sum=15, found at index 0! length = 2-0 = 2
         * Index 3: sum=7, map={0=-1, 15=0, 13=1, 7=3}
         * Index 4: sum=8, map={0=-1, 15=0, 13=1, 7=3, 8=4}
         * Index 5: sum=15, found at index 0! length = 5-0 = 5
         * ...
         * 
         * The subarray from index 1 to 5 has sum 0
         */

        if (nums == null || nums.length == 0) {
            return 0;
        }

        // Map to store: prefixSum -> first index where it occurred
        Map<Integer, Integer> sumIndexMap = new HashMap<>();
        sumIndexMap.put(0, -1); // Empty prefix has sum 0

        int maxLength = 0;
        int cumulativeSum = 0;

        for (int i = 0; i < nums.length; i++) {
            cumulativeSum += nums[i];

            if (sumIndexMap.containsKey(cumulativeSum)) {
                // We've seen this sum before!
                // The subarray between previous occurrence and current index has sum 0
                int firstOccurrence = sumIndexMap.get(cumulativeSum);
                int subarrayLength = i - firstOccurrence;
                maxLength = Math.max(maxLength, subarrayLength);
            } else {
                // First time seeing this prefix sum, store it
                sumIndexMap.put(cumulativeSum, i);
            }
        }

        return maxLength;
    }

    // Solution 4: With Visualization
    // Time: O(n), Space: O(n)
    public int maxLenWithVisualization(int[] nums, boolean showSteps) {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, -1);

        int maxLen = 0;
        int sum = 0;

        if (showSteps) {
            System.out.println("\n=== Step-by-step Execution ===");
            System.out.print("Array: ");
            printArray(nums);
            System.out.println();
        }

        for (int i = 0; i < nums.length; i++) {
            sum += nums[i];

            if (showSteps) {
                System.out.printf("Index %d: num=%d, prefixSum=%d", i, nums[i], sum);
            }

            if (map.containsKey(sum)) {
                int prevIndex = map.get(sum);
                int len = i - prevIndex;
                maxLen = Math.max(maxLen, len);

                if (showSteps) {
                    System.out.printf(" -> Found! Subarray from index %d to %d (length=%d)",
                            prevIndex + 1, i, len);
                }
            } else {
                map.put(sum, i);
            }

            if (showSteps) {
                System.out.println();
            }
        }

        if (showSteps) {
            System.out.println("Max Length: " + maxLen);
        }

        return maxLen;
    }

    // Helper: Print array
    private static void printArray(int[] nums) {
        System.out.print("[");
        for (int i = 0; i < nums.length; i++) {
            System.out.print(nums[i]);
            if (i < nums.length - 1)
                System.out.print(", ");
        }
        System.out.print("]");
    }

    // Helper: Print subarray
    private static void printSubarray(int[] nums, int start, int end) {
        System.out.print("[");
        for (int i = start; i <= end; i++) {
            System.out.print(nums[i]);
            if (i < end)
                System.out.print(", ");
        }
        System.out.print("]");
    }

    // Helper: Find and print all zero-sum subarrays of max length
    private static void findAllMaxLengthSubarrays(int[] nums, int maxLen) {
        System.out.println("\nAll subarrays with zero sum and max length:");

        for (int i = 0; i < nums.length; i++) {
            int sum = 0;
            for (int j = i; j < nums.length; j++) {
                sum += nums[j];
                if (sum == 0 && (j - i + 1) == maxLen) {
                    System.out.print("  Indices " + i + " to " + j + ": ");
                    printSubarray(nums, i, j);
                    System.out.println(" (sum = 0)");
                }
            }
        }
    }

    // Test cases
    public static void main(String[] args) {
        LongestSubarrayZeroSum solution = new LongestSubarrayZeroSum();

        // Test case 1: Basic example
        int[] nums1 = { 15, -2, 2, -8, 1, 7, 10, 23 };
        System.out.print("Example 1 - Input: ");
        printArray(nums1);
        System.out.println();
        int result1 = solution.maxLenOptimal(nums1);
        System.out.println("Output (Optimal): " + result1);
        System.out.println("Output (Brute Force): " + solution.maxLenBruteForce(nums1));
        findAllMaxLengthSubarrays(nums1, result1);

        // Test case 2: Simple case
        int[] nums2 = { 1, -1, 3, 2, -2, -8, 1, 7, 10, 23 };
        System.out.print("\nExample 2 - Input: ");
        printArray(nums2);
        System.out.println();
        int result2 = solution.maxLenOptimal(nums2);
        System.out.println("Output: " + result2);
        findAllMaxLengthSubarrays(nums2, result2);

        // Test case 3: All zeros
        int[] nums3 = { 0, 0, 0, 0 };
        System.out.print("\nAll zeros - Input: ");
        printArray(nums3);
        System.out.println();
        System.out.println("Output: " + solution.maxLenOptimal(nums3));

        // Test case 4: No zero-sum subarray
        int[] nums4 = { 1, 2, 3, 4, 5 };
        System.out.print("\nNo zero-sum - Input: ");
        printArray(nums4);
        System.out.println();
        System.out.println("Output: " + solution.maxLenOptimal(nums4));

        // Test case 5: Entire array sums to zero
        int[] nums5 = { 1, 2, -3 };
        System.out.print("\nEntire array - Input: ");
        printArray(nums5);
        System.out.println();
        System.out.println("Output: " + solution.maxLenOptimal(nums5));

        // Test case 6: Multiple zero-sum subarrays
        int[] nums6 = { 1, -1, 2, -2, 3, -3 };
        System.out.print("\nMultiple subarrays - Input: ");
        printArray(nums6);
        System.out.println();
        System.out.println("Output: " + solution.maxLenOptimal(nums6));

        // Test case 7: With detailed visualization
        int[] nums7 = { 1, 0, -1, 2, -2 };
        System.out.print("\nDetailed walkthrough - Input: ");
        printArray(nums7);
        solution.maxLenWithVisualization(nums7, true);

        // Performance comparison
        System.out.println("\n=== Performance Comparison ===");
        int[] largeArray = new int[1000];
        Random rand = new Random(42);
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = rand.nextInt(21) - 10; // Random numbers from -10 to 10
        }

        long start = System.nanoTime();
        int resultBrute = solution.maxLenBruteForce(largeArray);
        long timeBrute = System.nanoTime() - start;

        start = System.nanoTime();
        int resultOptimal = solution.maxLenOptimal(largeArray);
        long timeOptimal = System.nanoTime() - start;

        System.out.println("Array size: 1000 elements");
        System.out.println("Brute Force: " + resultBrute + " (Time: " + timeBrute / 1000000.0 + " ms)");
        System.out.println("Optimal:     " + resultOptimal + " (Time: " + timeOptimal / 1000000.0 + " ms)");
        System.out.println("Speedup:     " + (timeBrute / (double) timeOptimal) + "x faster");
    }
}
