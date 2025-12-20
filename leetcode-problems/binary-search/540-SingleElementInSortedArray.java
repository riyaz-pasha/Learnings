class SingleElementInSortedArray {

    // Approach 1: Binary Search - Pattern Detection (Most Elegant)
    // Time: O(log n), Space: O(1)
    public int singleNonDuplicate1(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // Ensure mid is even for consistent comparison
            if (mid % 2 == 1) {
                mid--;
            }

            // Check if pairs are intact
            if (nums[mid] == nums[mid + 1]) {
                // Single element is to the right
                left = mid + 2;
            } else {
                // Single element is at mid or to the left
                right = mid;
            }
        }

        return nums[left];
    }

    // Approach 2: Binary Search - Index Parity Check
    // Time: O(log n), Space: O(1)
    public int singleNonDuplicate2(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // Determine if we're in the left half (normal pattern) or right half
            boolean isEven = (mid % 2 == 0);

            if (isEven) {
                if (nums[mid] == nums[mid + 1]) {
                    left = mid + 2; // Single element is to the right
                } else {
                    right = mid; // Single element is at mid or left
                }
            } else {
                if (nums[mid] == nums[mid - 1]) {
                    left = mid + 1; // Single element is to the right
                } else {
                    right = mid - 1; // Single element is to the left
                }
            }
        }

        return nums[left];
    }

    // Approach 3: XOR with Bit Manipulation
    // Time: O(log n), Space: O(1)
    public int singleNonDuplicate3(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // XOR mid with 1 to get its pair index
            // If mid is even, mid^1 = mid+1
            // If mid is odd, mid^1 = mid-1
            if (nums[mid] == nums[mid ^ 1]) {
                // Pattern is intact, move right
                left = mid + 1;
            } else {
                // Pattern is broken, move left
                right = mid;
            }
        }

        return nums[left];
    }

    // Approach 4: Binary Search with Explicit Pair Checking
    // Time: O(log n), Space: O(1)
    public int singleNonDuplicate4(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            // Find the start of the pair containing mid
            int pairStart = (mid % 2 == 0) ? mid : mid - 1;

            if (nums[pairStart] == nums[pairStart + 1]) {
                // Pair is intact, single element is after this pair
                left = pairStart + 2;
            } else {
                // Pair is broken, single element is at or before pairStart
                right = pairStart;
            }
        }

        return nums[left];
    }

    // Approach 5: Linear XOR (Not O(log n) - for comparison)
    // Time: O(n), Space: O(1)
    public int singleNonDuplicate5(int[] nums) {
        int result = 0;
        for (int num : nums) {
            result ^= num;
        }
        return result;
    }

    // Test cases
    public static void main(String[] args) {
        SingleElementInSortedArray solution = new SingleElementInSortedArray();

        // Test Case 1
        int[] nums1 = { 1, 1, 2, 3, 3, 4, 4, 8, 8 };
        System.out.println("Test 1: " + solution.singleNonDuplicate1(nums1)); // Output: 2

        // Test Case 2
        int[] nums2 = { 3, 3, 7, 7, 10, 11, 11 };
        System.out.println("Test 2: " + solution.singleNonDuplicate1(nums2)); // Output: 10

        // Test Case 3: Single element at start
        int[] nums3 = { 1, 2, 2, 3, 3 };
        System.out.println("Test 3: " + solution.singleNonDuplicate1(nums3)); // Output: 1

        // Test Case 4: Single element at end
        int[] nums4 = { 1, 1, 2, 2, 3 };
        System.out.println("Test 4: " + solution.singleNonDuplicate1(nums4)); // Output: 3

        // Test Case 5: Only one element
        int[] nums5 = { 1 };
        System.out.println("Test 5: " + solution.singleNonDuplicate1(nums5)); // Output: 1

        // Test Case 6: Single element in middle
        int[] nums6 = { 1, 1, 2, 2, 3, 4, 4, 5, 5 };
        System.out.println("Test 6: " + solution.singleNonDuplicate1(nums6)); // Output: 3

        // Verify all approaches give same results
        System.out.println("\nVerifying all approaches:");
        for (int[] nums : new int[][] { nums1, nums2, nums3, nums4, nums5, nums6 }) {
            int r1 = solution.singleNonDuplicate1(nums);
            int r2 = solution.singleNonDuplicate2(nums);
            int r3 = solution.singleNonDuplicate3(nums);
            int r4 = solution.singleNonDuplicate4(nums);
            System.out.println("Results: " + r1 + " " + r2 + " " + r3 + " " + r4);
        }
    }
}

/*
 * DETAILED EXPLANATION:
 * 
 * KEY INSIGHT:
 * In a sorted array where every element appears twice except one:
 * - BEFORE the single element: pairs start at even indices (0,2,4,...)
 * Example: [1,1,2,2,3,...]
 * indices: 0,1,2,3,4,...
 * - AFTER the single element: pairs start at odd indices
 * Example: [...,5,6,6,7,7]
 * indices: ...,5,6,7,8,9
 * 
 * APPROACH 1 (RECOMMENDED - Most Elegant):
 * 1. Always work with even indices by adjusting mid if it's odd
 * 2. Check if nums[mid] == nums[mid+1]
 * - If YES: pattern is intact, single element is to the right
 * - If NO: pattern is broken, single element is at mid or to the left
 * 
 * Time: O(log n), Space: O(1)
 * 
 * WALKTHROUGH for [1,1,2,3,3,4,4,8,8]:
 * 
 * Iteration 1:
 * - left=0, right=8, mid=4 (even)
 * - nums[4]=3, nums[5]=4 → 3≠4 (pattern broken)
 * - right=4
 * 
 * Iteration 2:
 * - left=0, right=4, mid=2 (even)
 * - nums[2]=2, nums[3]=3 → 2≠3 (pattern broken)
 * - right=2
 * 
 * Iteration 3:
 * - left=0, right=2, mid=1 (odd) → adjust to mid=0
 * - nums[0]=1, nums[1]=1 → 1==1 (pattern intact)
 * - left=2
 * 
 * Result: nums[2]=2 ✓
 * 
 * APPROACH 3 - XOR Bit Manipulation Trick:
 * - mid^1 toggles the last bit: even↔odd
 * - If mid is even (e.g., 4): mid^1 = 5 (next index)
 * - If mid is odd (e.g., 5): mid^1 = 4 (previous index)
 * This elegantly gets the pair index regardless of whether mid is even or odd!
 * 
 * COMPLEXITY ANALYSIS:
 * - Time: O(log n) - binary search halves search space each iteration
 * - Space: O(1) - only use a few variables
 * 
 * WHY O(log n)?
 * Array size: n
 * After 1 iteration: n/2
 * After 2 iterations: n/4
 * After k iterations: n/2^k
 * When n/2^k = 1, we're done
 * So 2^k = n → k = log₂(n)
 */
