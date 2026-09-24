class LongestSubarrayDeleteOne {
    
    /**
     * APPROACH 1: Sliding Window (Optimal Solution)
     * 
     * INTUITION:
     * - This is similar to "Max Consecutive Ones III" with k=1
     * - We can have at most 1 zero in our window (representing the deleted element)
     * - The result is window_size - 1 (since we must delete one element)
     * 
     * REASONING:
     * - Use sliding window to find longest subarray with at most 1 zero
     * - The zero in the window represents the element we'll delete
     * - If array has no zeros, we still must delete one element (return length-1)
     * - Final answer = max_window_size - 1
     * 
     * TIME COMPLEXITY: O(n) - single pass through array
     * SPACE COMPLEXITY: O(1) - only using a few variables
     */
    public int longestSubarray_SlidingWindow(int[] nums) {
        int left = 0;           // Left pointer of window
        int zerosCount = 0;     // Count of zeros in current window
        int maxLength = 0;      // Maximum window size found
        
        for (int right = 0; right < nums.length; right++) {
            // Expand window by including nums[right]
            if (nums[right] == 0) {
                zerosCount++;
            }
            
            // Shrink window if we have more than 1 zero
            while (zerosCount > 1) {
                if (nums[left] == 0) {
                    zerosCount--;
                }
                left++;
            }
            
            // Update max window size
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        // We must delete one element, so return maxLength - 1
        // This handles both cases:
        // - If there was a zero, it gets deleted (included in maxLength)
        // - If no zeros, we still must delete a 1
        return maxLength - 1;
    }
    
    /**
     * APPROACH 2: Optimized Sliding Window (No Shrinking)
     * 
     * INTUITION:
     * - Maintain a window that only grows or slides forward
     * - Never shrink the window size once we reach a certain length
     * - This works because we only care about the maximum length
     * 
     * REASONING:
     * - When zerosCount > 1, slide the window forward (both pointers move)
     * - The window size represents max consecutive 1s + 1 deletable element
     * - Final answer accounts for the mandatory deletion
     * 
     * TIME COMPLEXITY: O(n) - single pass
     * SPACE COMPLEXITY: O(1) - constant space
     */
    public int longestSubarray_OptimizedWindow(int[] nums) {
        int left = 0;
        int zerosCount = 0;
        
        for (int right = 0; right < nums.length; right++) {
            if (nums[right] == 0) {
                zerosCount++;
            }
            
            // Use 'if' instead of 'while' to maintain window size
            if (zerosCount > 1) {
                if (nums[left] == 0) {
                    zerosCount--;
                }
                left++;
            }
        }
        
        // Return window size - 1 (mandatory deletion)
        return nums.length - left - 1;
    }
    
    /**
     * APPROACH 3: Track Consecutive Ones Groups
     * 
     * INTUITION:
     * - Split array into groups of consecutive 1s
     * - Deleting a 0 merges two adjacent groups
     * - Deleting a 1 from a group reduces that group by 1
     * 
     * REASONING:
     * - Count consecutive 1s in each group
     * - For each zero, check if we can merge surrounding groups
     * - Maximum result is either:
     *   a) Sum of two adjacent groups (delete 0 between them)
     *   b) Single group - 1 (delete a 1 from that group)
     * 
     * TIME COMPLEXITY: O(n) - two passes through array
     * SPACE COMPLEXITY: O(n) - store groups of 1s
     */
    public int longestSubarray_GroupTracking(int[] nums) {
        java.util.List<Integer> groups = new java.util.ArrayList<>();
        int count = 0;
        
        // Count consecutive 1s and store as groups
        for (int num : nums) {
            if (num == 1) {
                count++;
            } else {
                if (count > 0) {
                    groups.add(count);
                    count = 0;
                }
                groups.add(0); // Mark zero position
            }
        }
        if (count > 0) {
            groups.add(count);
        }
        
        // Edge case: all 1s - must delete one
        if (groups.size() == 1 && groups.get(0) > 0) {
            return groups.get(0) - 1;
        }
        
        // Edge case: empty or all 0s
        if (groups.isEmpty() || (groups.size() == 1 && groups.get(0) == 0)) {
            return 0;
        }
        
        int maxLength = 0;
        
        // Try merging adjacent groups by deleting zeros between them
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i) == 0) { // Found a zero
                int leftGroup = (i > 0) ? groups.get(i - 1) : 0;
                int rightGroup = (i < groups.size() - 1) ? groups.get(i + 1) : 0;
                maxLength = Math.max(maxLength, leftGroup + rightGroup);
            } else {
                // Single group - must delete one element from it
                maxLength = Math.max(maxLength, groups.get(i) - 1);
            }
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 4: Dynamic Programming - Track Left and Right Consecutive 1s
     * 
     * INTUITION:
     * - For each position, track consecutive 1s to the left and right
     * - When we delete position i, we merge left[i] and right[i]
     * 
     * REASONING:
     * - left[i] = consecutive 1s ending at position i
     * - right[i] = consecutive 1s starting at position i
     * - Deleting i gives us left[i-1] + right[i+1]
     * - Handle edge cases for boundaries
     * 
     * TIME COMPLEXITY: O(n) - three passes through array
     * SPACE COMPLEXITY: O(n) - two arrays for left and right counts
     */
    public int longestSubarray_DP(int[] nums) {
        int n = nums.length;
        int[] left = new int[n];  // Consecutive 1s ending at i
        int[] right = new int[n]; // Consecutive 1s starting at i
        
        // Build left array - consecutive 1s ending at each position
        left[0] = (nums[0] == 1) ? 1 : 0;
        for (int i = 1; i < n; i++) {
            if (nums[i] == 1) {
                left[i] = left[i - 1] + 1;
            } else {
                left[i] = 0;
            }
        }
        
        // Build right array - consecutive 1s starting at each position
        right[n - 1] = (nums[n - 1] == 1) ? 1 : 0;
        for (int i = n - 2; i >= 0; i--) {
            if (nums[i] == 1) {
                right[i] = right[i + 1] + 1;
            } else {
                right[i] = 0;
            }
        }
        
        int maxLength = 0;
        
        // Try deleting each position
        for (int i = 0; i < n; i++) {
            int leftOnes = (i > 0) ? left[i - 1] : 0;
            int rightOnes = (i < n - 1) ? right[i + 1] : 0;
            maxLength = Math.max(maxLength, leftOnes + rightOnes);
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 5: Brute Force (For Understanding)
     * 
     * INTUITION:
     * - Try deleting each element one by one
     * - For each deletion, find longest consecutive 1s
     * - Return the maximum found
     * 
     * REASONING:
     * - Simple and straightforward
     * - Helps understand the problem clearly
     * - Good for verification
     * 
     * TIME COMPLEXITY: O(n²) - O(n) deletions × O(n) to find max
     * SPACE COMPLEXITY: O(1) - no extra space needed
     */
    public int longestSubarray_BruteForce(int[] nums) {
        int maxLength = 0;
        
        // Try deleting each position
        for (int deleteIdx = 0; deleteIdx < nums.length; deleteIdx++) {
            int currentLength = 0;
            int longestInThisTry = 0;
            
            // Count consecutive 1s after deleting deleteIdx
            for (int i = 0; i < nums.length; i++) {
                if (i == deleteIdx) continue; // Skip deleted element
                
                if (nums[i] == 1) {
                    currentLength++;
                    longestInThisTry = Math.max(longestInThisTry, currentLength);
                } else {
                    currentLength = 0;
                }
            }
            
            maxLength = Math.max(maxLength, longestInThisTry);
        }
        
        return maxLength;
    }
    
    /**
     * Test cases with detailed explanations
     */
    public static void main(String[] args) {
        LongestSubarrayDeleteOne solution = new LongestSubarrayDeleteOne();
        
        // Test Case 1: Delete a zero to merge groups
        int[] nums1 = {1, 1, 0, 1};
        System.out.println("Test Case 1: nums = [1,1,0,1]");
        System.out.println("Expected: 3 (delete the 0)");
        System.out.println("Sliding Window: " + solution.longestSubarray_SlidingWindow(nums1));
        System.out.println("Optimized Window: " + solution.longestSubarray_OptimizedWindow(nums1));
        System.out.println("Group Tracking: " + solution.longestSubarray_GroupTracking(nums1));
        System.out.println("DP: " + solution.longestSubarray_DP(nums1));
        System.out.println("Brute Force: " + solution.longestSubarray_BruteForce(nums1));
        System.out.println();
        
        // Test Case 2: Delete a zero in the middle
        int[] nums2 = {0, 1, 1, 1, 0, 1, 1, 0, 1};
        System.out.println("Test Case 2: nums = [0,1,1,1,0,1,1,0,1]");
        System.out.println("Expected: 5 (delete the 0 at position 4)");
        System.out.println("Sliding Window: " + solution.longestSubarray_SlidingWindow(nums2));
        System.out.println("Optimized Window: " + solution.longestSubarray_OptimizedWindow(nums2));
        System.out.println("Group Tracking: " + solution.longestSubarray_GroupTracking(nums2));
        System.out.println("DP: " + solution.longestSubarray_DP(nums2));
        System.out.println("Brute Force: " + solution.longestSubarray_BruteForce(nums2));
        System.out.println();
        
        // Test Case 3: All ones - must delete one
        int[] nums3 = {1, 1, 1};
        System.out.println("Test Case 3: nums = [1,1,1]");
        System.out.println("Expected: 2 (must delete one element)");
        System.out.println("Sliding Window: " + solution.longestSubarray_SlidingWindow(nums3));
        System.out.println("Optimized Window: " + solution.longestSubarray_OptimizedWindow(nums3));
        System.out.println("Group Tracking: " + solution.longestSubarray_GroupTracking(nums3));
        System.out.println("DP: " + solution.longestSubarray_DP(nums3));
        System.out.println("Brute Force: " + solution.longestSubarray_BruteForce(nums3));
        System.out.println();
        
        // Test Case 4: All zeros
        int[] nums4 = {0, 0, 0};
        System.out.println("Test Case 4: nums = [0,0,0]");
        System.out.println("Expected: 0");
        System.out.println("Sliding Window: " + solution.longestSubarray_SlidingWindow(nums4));
        System.out.println("Optimized Window: " + solution.longestSubarray_OptimizedWindow(nums4));
        System.out.println("Group Tracking: " + solution.longestSubarray_GroupTracking(nums4));
        System.out.println("DP: " + solution.longestSubarray_DP(nums4));
        System.out.println("Brute Force: " + solution.longestSubarray_BruteForce(nums4));
        System.out.println();
        
        // Test Case 5: Single element
        int[] nums5 = {1};
        System.out.println("Test Case 5: nums = [1]");
        System.out.println("Expected: 0 (must delete the only element)");
        System.out.println("Sliding Window: " + solution.longestSubarray_SlidingWindow(nums5));
        System.out.println("Optimized Window: " + solution.longestSubarray_OptimizedWindow(nums5));
        System.out.println("Group Tracking: " + solution.longestSubarray_GroupTracking(nums5));
        System.out.println("DP: " + solution.longestSubarray_DP(nums5));
        System.out.println("Brute Force: " + solution.longestSubarray_BruteForce(nums5));
        System.out.println();
        
        // Test Case 6: Alternating pattern
        int[] nums6 = {1, 0, 1, 0, 1, 0, 1};
        System.out.println("Test Case 6: nums = [1,0,1,0,1,0,1]");
        System.out.println("Expected: 2 (delete any 0 to get 2 consecutive 1s)");
        System.out.println("Sliding Window: " + solution.longestSubarray_SlidingWindow(nums6));
        System.out.println("Optimized Window: " + solution.longestSubarray_OptimizedWindow(nums6));
        System.out.println("Group Tracking: " + solution.longestSubarray_GroupTracking(nums6));
        System.out.println("DP: " + solution.longestSubarray_DP(nums6));
        System.out.println("Brute Force: " + solution.longestSubarray_BruteForce(nums6));
        System.out.println();
        
        // Test Case 7: Multiple consecutive groups
        int[] nums7 = {1, 1, 1, 0, 0, 1, 1, 1, 1};
        System.out.println("Test Case 7: nums = [1,1,1,0,0,1,1,1,1]");
        System.out.println("Expected: 4 (delete a 1 from the larger group)");
        System.out.println("Sliding Window: " + solution.longestSubarray_SlidingWindow(nums7));
        System.out.println("Optimized Window: " + solution.longestSubarray_OptimizedWindow(nums7));
        System.out.println("Group Tracking: " + solution.longestSubarray_GroupTracking(nums7));
        System.out.println("DP: " + solution.longestSubarray_DP(nums7));
        System.out.println("Brute Force: " + solution.longestSubarray_BruteForce(nums7));
    }
}

/*
 * KEY INSIGHTS AND SUMMARY:
 * 
 * 1. PROBLEM TRANSFORMATION:
 *    - "Delete one element" = "Find longest subarray with at most 1 zero"
 *    - Then subtract 1 from the result (mandatory deletion)
 *    - Special case: all 1s means we must delete a 1
 * 
 * 2. RELATIONSHIP TO PREVIOUS PROBLEM:
 *    - This is "Max Consecutive Ones III" with k=1
 *    - Same sliding window technique applies
 *    - Key difference: must subtract 1 from final answer
 * 
 * 3. EDGE CASES TO HANDLE:
 *    - All 1s: return length - 1
 *    - All 0s: return 0
 *    - Single element: return 0
 *    - No zeros: still must delete one element
 * 
 * 4. WHY "maxLength - 1"?
 *    - maxLength includes the element we'll delete
 *    - If we found window with 1 zero: that zero gets deleted
 *    - If no zeros in array: we must delete a 1
 *    - Either way, result = maxLength - 1
 * 
 * 5. APPROACH COMPARISON:
 *    - Sliding Window (Approach 1): Most intuitive, clear logic
 *    - Optimized Window (Approach 2): Cleanest code, best for production
 *    - Group Tracking (Approach 3): Good for understanding problem structure
 *    - DP (Approach 4): Educational, shows left/right merging concept
 *    - Brute Force (Approach 5): Simple verification
 * 
 * 6. RECOMMENDED FOR INTERVIEW:
 *    - Start with Approach 1 (Sliding Window)
 *    - Explain the k=1 connection to Max Consecutive Ones III
 *    - Clearly explain why we subtract 1 at the end
 * 
 * 7. COMMON MISTAKES:
 *    - Forgetting to subtract 1 for mandatory deletion
 *    - Not handling all-1s case correctly
 *    - Off-by-one errors in window size calculation
 */
