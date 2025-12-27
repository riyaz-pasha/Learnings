class MaxConsecutiveOnesIII {
    
    /**
     * APPROACH 1: Sliding Window (Optimal Solution)
     * 
     * INTUITION:
     * - We want to find the longest subarray that contains at most k zeros
     * - Use a sliding window with two pointers (left and right)
     * - Expand the window by moving right pointer
     * - When we have more than k zeros, shrink from left
     * 
     * REASONING:
     * - Keep track of the number of zeros in current window
     * - If zeros exceed k, move left pointer until we have ≤ k zeros
     * - The window size at any point represents consecutive 1s (after flipping)
     * 
     * TIME COMPLEXITY: O(n) - each element visited at most twice
     * SPACE COMPLEXITY: O(1) - only using a few variables
     */
    public int longestOnes_SlidingWindow(int[] nums, int k) {
        int left = 0;           // Left pointer of the window
        int right = 0;          // Right pointer of the window
        int zerosCount = 0;     // Count of zeros in current window
        int maxLength = 0;      // Maximum length found so far
        
        // Expand the window by moving right pointer
        while (right < nums.length) {
            // If we encounter a zero, increment the zero count
            if (nums[right] == 0) {
                zerosCount++;
            }
            
            // If we have too many zeros, shrink window from left
            while (zerosCount > k) {
                if (nums[left] == 0) {
                    zerosCount--;
                }
                left++;
            }
            
            // Update maximum length (current window size)
            maxLength = Math.max(maxLength, right - left + 1);
            
            // Move right pointer forward
            right++;
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 2: Optimized Sliding Window (No Shrinking)
     * 
     * INTUITION:
     * - Instead of shrinking the window, we maintain the maximum window size
     * - When zeros exceed k, we slide the entire window forward
     * - This way, the window only grows or maintains size, never shrinks
     * 
     * REASONING:
     * - We only care about the maximum length, not all valid lengths
     * - Once we find a valid window of size n, we only look for larger windows
     * - If current window is invalid, slide it forward (both pointers move)
     * 
     * TIME COMPLEXITY: O(n) - single pass through array
     * SPACE COMPLEXITY: O(1) - constant extra space
     */
    public int longestOnes_OptimizedWindow(int[] nums, int k) {
        int left = 0;
        int zerosCount = 0;
        
        // Use right as the loop variable itself
        for (int right = 0; right < nums.length; right++) {
            // Add current element to window
            if (nums[right] == 0) {
                zerosCount++;
            }
            
            // If too many zeros, slide window forward
            // Note: we use 'if' instead of 'while' to maintain window size
            if (zerosCount > k) {
                if (nums[left] == 0) {
                    zerosCount--;
                }
                left++;
            }
        }
        
        // The final window size is our answer
        return nums.length - left;
    }
    
    /**
     * APPROACH 3: Sliding Window with Queue (Educational)
     * 
     * INTUITION:
     * - Track positions of zeros in the current window using a queue
     * - When we exceed k zeros, remove the leftmost zero from consideration
     * 
     * REASONING:
     * - Queue stores indices of zeros within our window
     * - When queue size > k, the leftmost zero must be outside our window
     * - Move left pointer to position after the leftmost zero
     * 
     * TIME COMPLEXITY: O(n) - each element processed once
     * SPACE COMPLEXITY: O(k) - queue stores at most k+1 zero positions
     */
    public int longestOnes_WithQueue(int[] nums, int k) {
        int left = 0;
        int maxLength = 0;
        java.util.Queue<Integer> zeroPositions = new java.util.LinkedList<>();
        
        for (int right = 0; right < nums.length; right++) {
            // If current element is zero, add its position to queue
            if (nums[right] == 0) {
                zeroPositions.offer(right);
            }
            
            // If we have more than k zeros, shrink window
            if (zeroPositions.size() > k) {
                // Move left pointer past the leftmost zero
                left = zeroPositions.poll() + 1;
            }
            
            // Update maximum length
            maxLength = Math.max(maxLength, right - left + 1);
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 4: Brute Force (For Understanding)
     * 
     * INTUITION:
     * - Try every possible subarray
     * - For each subarray, count zeros and check if ≤ k
     * 
     * REASONING:
     * - Simple but inefficient approach
     * - Helps understand the problem constraints
     * - Good for testing and verification
     * 
     * TIME COMPLEXITY: O(n²) - nested loops
     * SPACE COMPLEXITY: O(1) - no extra space
     */
    public int longestOnes_BruteForce(int[] nums, int k) {
        int maxLength = 0;
        
        // Try every starting position
        for (int start = 0; start < nums.length; start++) {
            int zerosCount = 0;
            
            // Extend to every possible ending position
            for (int end = start; end < nums.length; end++) {
                if (nums[end] == 0) {
                    zerosCount++;
                }
                
                // If valid window (zeros ≤ k), update max length
                if (zerosCount <= k) {
                    maxLength = Math.max(maxLength, end - start + 1);
                } else {
                    // No point extending further from this start
                    break;
                }
            }
        }
        
        return maxLength;
    }
    
    /**
     * Test cases to verify all implementations
     */
    public static void main(String[] args) {
        MaxConsecutiveOnesIII solution = new MaxConsecutiveOnesIII();
        
        // Test Case 1
        int[] nums1 = {1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0};
        int k1 = 2;
        System.out.println("Test Case 1: nums = [1,1,1,0,0,0,1,1,1,1,0], k = 2");
        System.out.println("Expected: 6");
        System.out.println("Sliding Window: " + solution.longestOnes_SlidingWindow(nums1, k1));
        System.out.println("Optimized Window: " + solution.longestOnes_OptimizedWindow(nums1, k1));
        System.out.println("With Queue: " + solution.longestOnes_WithQueue(nums1, k1));
        System.out.println("Brute Force: " + solution.longestOnes_BruteForce(nums1, k1));
        System.out.println();
        
        // Test Case 2
        int[] nums2 = {0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1};
        int k2 = 3;
        System.out.println("Test Case 2: nums = [0,0,1,1,0,0,1,1,1,0,1,1,0,0,0,1,1,1,1], k = 3");
        System.out.println("Expected: 10");
        System.out.println("Sliding Window: " + solution.longestOnes_SlidingWindow(nums2, k2));
        System.out.println("Optimized Window: " + solution.longestOnes_OptimizedWindow(nums2, k2));
        System.out.println("With Queue: " + solution.longestOnes_WithQueue(nums2, k2));
        System.out.println("Brute Force: " + solution.longestOnes_BruteForce(nums2, k2));
        System.out.println();
        
        // Test Case 3: Edge case - all zeros
        int[] nums3 = {0, 0, 0, 0};
        int k3 = 2;
        System.out.println("Test Case 3: nums = [0,0,0,0], k = 2");
        System.out.println("Expected: 2");
        System.out.println("Sliding Window: " + solution.longestOnes_SlidingWindow(nums3, k3));
        System.out.println("Optimized Window: " + solution.longestOnes_OptimizedWindow(nums3, k3));
        System.out.println("With Queue: " + solution.longestOnes_WithQueue(nums3, k3));
        System.out.println("Brute Force: " + solution.longestOnes_BruteForce(nums3, k3));
        System.out.println();
        
        // Test Case 4: Edge case - all ones
        int[] nums4 = {1, 1, 1, 1};
        int k4 = 0;
        System.out.println("Test Case 4: nums = [1,1,1,1], k = 0");
        System.out.println("Expected: 4");
        System.out.println("Sliding Window: " + solution.longestOnes_SlidingWindow(nums4, k4));
        System.out.println("Optimized Window: " + solution.longestOnes_OptimizedWindow(nums4, k4));
        System.out.println("With Queue: " + solution.longestOnes_WithQueue(nums4, k4));
        System.out.println("Brute Force: " + solution.longestOnes_BruteForce(nums4, k4));
        System.out.println();
        
        // Test Case 5: k = 0 with mixed array
        int[] nums5 = {1, 1, 0, 1, 1, 1, 0, 1};
        int k5 = 0;
        System.out.println("Test Case 5: nums = [1,1,0,1,1,1,0,1], k = 0");
        System.out.println("Expected: 3");
        System.out.println("Sliding Window: " + solution.longestOnes_SlidingWindow(nums5, k5));
        System.out.println("Optimized Window: " + solution.longestOnes_OptimizedWindow(nums5, k5));
        System.out.println("With Queue: " + solution.longestOnes_WithQueue(nums5, k5));
        System.out.println("Brute Force: " + solution.longestOnes_BruteForce(nums5, k5));
    }
}

/*
 * KEY INSIGHTS AND SUMMARY:
 * 
 * 1. PROBLEM TRANSFORMATION:
 *    - "Flip at most k zeros" = "Find longest subarray with at most k zeros"
 *    - After flipping, all zeros become ones, creating consecutive ones
 * 
 * 2. SLIDING WINDOW PATTERN:
 *    - Ideal for "longest subarray with constraint" problems
 *    - Two pointers maintain a valid window
 *    - Expand window when valid, shrink when invalid
 * 
 * 3. OPTIMIZATION TECHNIQUES:
 *    - Standard window: shrink completely when invalid (while loop)
 *    - Optimized window: slide forward when invalid (if statement)
 *    - Both achieve O(n) but optimized has cleaner implementation
 * 
 * 4. RECOMMENDED APPROACH:
 *    - Use Approach 1 (Sliding Window) for interviews - most intuitive
 *    - Use Approach 2 (Optimized) for production - cleanest code
 * 
 * 5. COMMON PITFALLS:
 *    - Forgetting to update zerosCount when moving left pointer
 *    - Off-by-one errors in window size calculation
 *    - Not handling edge cases (k=0, all zeros, all ones)
 */
