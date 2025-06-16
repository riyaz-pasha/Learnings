import java.util.*;
/*
 * Given an integer array nums and an integer k, return true if there are two
 * distinct indices i and j in the array such that
 * nums[i] == nums[j] and abs(i - j) <= k.
 * 
 * Example 1:
 * Input: nums = [1,2,3,1], k = 3
 * Output: true
 * 
 * Example 2:
 * Input: nums = [1,0,1,1], k = 1
 * Output: true
 * 
 * Example 3:
 * Input: nums = [1,2,3,1,2,3], k = 2
 * Output: false
 */

class ContainsDuplicateII {

    // Solution 1: Brute Force Approach
    // Time Complexity: O(n²), Space Complexity: O(1)
    public boolean containsNearbyDuplicateBruteForce(int[] nums, int k) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length && j <= i + k; j++) {
                if (nums[i] == nums[j]) {
                    return true;
                }
            }
        }
        return false;
    }

    // Solution 2: HashMap to store value and its most recent index
    // Time Complexity: O(n), Space Complexity: O(n)
    public boolean containsNearbyDuplicate(int[] nums, int k) {
        Map<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            // If we've seen this number before
            if (map.containsKey(nums[i])) {
                int prevIndex = map.get(nums[i]);
                // Check if the distance is within k
                if (i - prevIndex <= k) {
                    return true;
                }
            }
            // Update the most recent index for this number
            map.put(nums[i], i);
        }

        return false;
    }

    // Solution 3: Sliding Window with HashSet
    // Time Complexity: O(n), Space Complexity: O(min(n, k))
    public boolean containsNearbyDuplicateSet(int[] nums, int k) {
        Set<Integer> window = new HashSet<>();

        for (int i = 0; i < nums.length; i++) {
            // If current element is already in the window, we found a duplicate
            if (window.contains(nums[i])) {
                return true;
            }

            window.add(nums[i]);

            // Maintain window size of k
            if (window.size() > k) {
                window.remove(nums[i - k]);
            }
        }

        return false;
    }

    // Solution 4: Using TreeSet for ordered sliding window (if we need sorted
    // order)
    // Time Complexity: O(n log k), Space Complexity: O(min(n, k))
    public boolean containsNearbyDuplicateTreeSet(int[] nums, int k) {
        TreeSet<Integer> window = new TreeSet<>();

        for (int i = 0; i < nums.length; i++) {
            // Check if current element exists in window
            if (window.contains(nums[i])) {
                return true;
            }

            window.add(nums[i]);

            // Maintain window size
            if (window.size() > k) {
                window.remove(nums[i - k]);
            }
        }

        return false;
    }

    // Solution 5: Early termination optimization
    // Time Complexity: O(n), Space Complexity: O(n)
    public boolean containsNearbyDuplicateOptimized(int[] nums, int k) {
        if (k == 0)
            return false; // Edge case: k = 0 means no valid pairs

        Map<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            if (map.containsKey(nums[i])) {
                if (i - map.get(nums[i]) <= k) {
                    return true;
                }
            }
            map.put(nums[i], i);
        }

        return false;
    }

    // Debug version to show the process
    public boolean containsNearbyDuplicateDebug(int[] nums, int k) {
        System.out.println("Array: " + Arrays.toString(nums) + ", k = " + k);
        Map<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            if (map.containsKey(nums[i])) {
                int prevIndex = map.get(nums[i]);
                int distance = i - prevIndex;
                System.out.printf("Found duplicate: nums[%d] = nums[%d] = %d, distance = %d\n",
                        prevIndex, i, nums[i], distance);
                if (distance <= k) {
                    System.out.println("Distance <= k, returning true");
                    return true;
                }
            }
            map.put(nums[i], i);
            System.out.printf("Step %d: map = %s\n", i, map);
        }

        System.out.println("No valid duplicate found, returning false");
        return false;
    }

    // Test method with comprehensive test cases
    public static void main(String[] args) {
        ContainsDuplicateII solution = new ContainsDuplicateII();

        // Test cases from examples
        System.out.println("=== Example Test Cases ===");

        int[] nums1 = { 1, 2, 3, 1 };
        int k1 = 3;
        System.out.printf("Test 1: nums = %s, k = %d\n", Arrays.toString(nums1), k1);
        System.out.println("Result: " + solution.containsNearbyDuplicate(nums1, k1));
        System.out.println("Expected: true\n");

        int[] nums2 = { 1, 0, 1, 1 };
        int k2 = 1;
        System.out.printf("Test 2: nums = %s, k = %d\n", Arrays.toString(nums2), k2);
        System.out.println("Result: " + solution.containsNearbyDuplicate(nums2, k2));
        System.out.println("Expected: true\n");

        int[] nums3 = { 1, 2, 3, 1, 2, 3 };
        int k3 = 2;
        System.out.printf("Test 3: nums = %s, k = %d\n", Arrays.toString(nums3), k3);
        System.out.println("Result: " + solution.containsNearbyDuplicate(nums3, k3));
        System.out.println("Expected: false\n");

        // Additional test cases
        System.out.println("=== Additional Test Cases ===");

        // Edge case: empty array
        int[] nums4 = {};
        int k4 = 1;
        System.out.printf("Test 4 (empty): nums = %s, k = %d\n", Arrays.toString(nums4), k4);
        System.out.println("Result: " + solution.containsNearbyDuplicate(nums4, k4));
        System.out.println("Expected: false\n");

        // Edge case: single element
        int[] nums5 = { 1 };
        int k5 = 1;
        System.out.printf("Test 5 (single): nums = %s, k = %d\n", Arrays.toString(nums5), k5);
        System.out.println("Result: " + solution.containsNearbyDuplicate(nums5, k5));
        System.out.println("Expected: false\n");

        // Edge case: k = 0
        int[] nums6 = { 1, 1 };
        int k6 = 0;
        System.out.printf("Test 6 (k=0): nums = %s, k = %d\n", Arrays.toString(nums6), k6);
        System.out.println("Result: " + solution.containsNearbyDuplicate(nums6, k6));
        System.out.println("Expected: false\n");

        // Large k
        int[] nums7 = { 1, 2, 1 };
        int k7 = 100;
        System.out.printf("Test 7 (large k): nums = %s, k = %d\n", Arrays.toString(nums7), k7);
        System.out.println("Result: " + solution.containsNearbyDuplicate(nums7, k7));
        System.out.println("Expected: true\n");

        // Debug example
        System.out.println("=== Debug Example ===");
        solution.containsNearbyDuplicateDebug(nums3, k3);

        // Performance comparison
        System.out.println("\n=== Performance Comparison ===");
        int[] largeArray = new int[10000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i % 100; // Create duplicates
        }
        int kLarge = 50;

        long startTime, endTime;

        // HashMap approach
        startTime = System.nanoTime();
        boolean result1 = solution.containsNearbyDuplicate(largeArray, kLarge);
        endTime = System.nanoTime();
        System.out.printf("HashMap approach: %s (Time: %d ns)\n", result1, endTime - startTime);

        // Sliding window with HashSet
        startTime = System.nanoTime();
        boolean result2 = solution.containsNearbyDuplicateSet(largeArray, kLarge);
        endTime = System.nanoTime();
        System.out.printf("Sliding window: %s (Time: %d ns)\n", result2, endTime - startTime);

        System.out.println("\n=== Algorithm Comparison ===");
        System.out.println("1. HashMap: O(n) time, O(n) space - stores all elements");
        System.out.println("2. Sliding Window: O(n) time, O(min(n,k)) space - better space for small k");
        System.out.println("3. Brute Force: O(n²) time, O(1) space - simple but slow");
    }

}
