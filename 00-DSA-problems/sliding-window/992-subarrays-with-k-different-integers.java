/**
 * LEETCODE 992: SUBARRAYS WITH K DIFFERENT INTEGERS
 * 
 * PROBLEM STATEMENT:
 * Given an integer array nums and an integer k, return the number of good 
 * subarrays of nums. A good array is an array where the number of different 
 * integers in that array is EXACTLY k.
 * 
 * Examples:
 * Input: nums = [1,2,1,2,3], k = 2
 * Output: 7
 * Explanation: Subarrays with exactly 2 distinct: 
 *              [1,2], [2,1], [1,2], [2,3], [1,2,1], [2,1,2], [1,2,1,2]
 * 
 * Input: nums = [1,2,1,3,4], k = 3
 * Output: 3
 * Explanation: [1,2,1,3], [2,1,3], [1,3,4]
 * 
 * ============================================================================
 * THE KEY INSIGHT - THIS IS BRILLIANT AND COUNTERINTUITIVE!
 * ============================================================================
 * 
 * DIRECT APPROACH DOESN'T WORK WELL:
 * - Finding subarrays with EXACTLY k distinct is hard to track directly
 * - When we add an element, the exact count changes unpredictably
 * - Shrinking the window is complex with "exactly k" constraint
 * 
 * THE MATHEMATICAL TRICK:
 * 
 * subarrays with EXACTLY k distinct = 
 *     subarrays with AT MOST k distinct - subarrays with AT MOST (k-1) distinct
 * 
 * WHY THIS WORKS:
 * 
 * Let's visualize with sets:
 * - AT MOST k = {subarrays with 1, 2, ..., k distinct integers}
 * - AT MOST k-1 = {subarrays with 1, 2, ..., k-1 distinct integers}
 * - EXACTLY k = AT MOST k - AT MOST k-1
 * 
 * Example: k = 2
 * - AT MOST 2 includes: subarrays with 1 or 2 distinct
 * - AT MOST 1 includes: subarrays with 1 distinct
 * - Difference gives: subarrays with exactly 2 distinct
 * 
 * COMPLEXITY:
 * Time: O(n) - two passes of sliding window
 * Space: O(k) - HashMap stores at most k distinct integers
 * 
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SubarraysKDistinct {
    
    /**
     * ============================================================================
     * APPROACH 1: TWO SLIDING WINDOWS (OPTIMAL - THE GENIUS SOLUTION!)
     * ============================================================================
     * 
     * ALGORITHM:
     * 1. Count subarrays with AT MOST k distinct integers
     * 2. Count subarrays with AT MOST (k-1) distinct integers
     * 3. Subtract: exactly k = atMost(k) - atMost(k-1)
     * 
     * WHY "AT MOST" IS EASY:
     * - For each right pointer position, count ALL valid left positions
     * - If window has <= k distinct, ALL subarrays ending at right are valid
     * - As we move right, we accumulate valid subarrays
     * 
     * DETAILED WALKTHROUGH:
     * nums = [1,2,1,2,3], k = 2
     * 
     * AT MOST 2 distinct:
     * right=0: [1] -> 1 subarray (1 distinct)
     * right=1: [1,2], [2] -> 2 subarrays (2 distinct)
     * right=2: [1,2,1], [2,1], [1] -> 3 subarrays (2 distinct)
     * right=3: [1,2,1,2], [2,1,2], [1,2], [2] -> 4 subarrays (2 distinct)
     * right=4: [2,3], [3] -> 2 subarrays (must shrink, now 2 distinct)
     * Total AT MOST 2: 1+2+3+4+2 = 12
     * 
     * AT MOST 1 distinct:
     * right=0: [1] -> 1 subarray
     * right=1: [2] -> 1 subarray (must shrink)
     * right=2: [1] -> 1 subarray (must shrink)
     * right=3: [2] -> 1 subarray (must shrink)
     * right=4: [3] -> 1 subarray (must shrink)
     * Total AT MOST 1: 1+1+1+1+1 = 5
     * 
     * EXACTLY 2: 12 - 5 = 7 ✓
     * 
     * TIME: O(n) - each element visited at most twice in each pass
     * SPACE: O(k) - HashMap stores at most k distinct integers
     */
    public int subarraysWithKDistinct(int[] nums, int k) {
        // Edge case: k = 0 means no valid subarrays
        if (k == 0) {
            return 0;
        }
        
        // The magic formula!
        return atMostKDistinct(nums, k) - atMostKDistinct(nums, k - 1);
    }
    
    /**
     * HELPER FUNCTION: Count subarrays with AT MOST k distinct integers
     * 
     * KEY INSIGHT FOR "AT MOST":
     * - For each position of right pointer, count how many valid left positions exist
     * - If window [left, right] has <= k distinct, then ALL subarrays ending at right 
     *   and starting from any position in [left, right] are valid
     * - Number of such subarrays = (right - left + 1)
     * 
     * EXAMPLE: nums = [1,2,1], right = 2, left = 0, window = [1,2,1]
     * Valid subarrays ending at index 2:
     * - [1,2,1] (start at 0)
     * - [2,1] (start at 1)  
     * - [1] (start at 2)
     * Count = 2 - 0 + 1 = 3
     * 
     * WHY THIS FORMULA WORKS:
     * - We're counting subarrays ending at position 'right'
     * - Any starting position from 'left' to 'right' forms a valid subarray
     * - That's exactly (right - left + 1) possibilities
     */
    private int atMostKDistinct(int[] nums, int k) {
        int count = 0;
        int left = 0;
        Map<Integer, Integer> frequency = new HashMap<>();
        
        for (int right = 0; right < nums.length; right++) {
            // Add right element to window
            int rightNum = nums[right];
            frequency.put(rightNum, frequency.getOrDefault(rightNum, 0) + 1);
            
            // Shrink window while we have more than k distinct integers
            while (frequency.size() > k) {
                int leftNum = nums[left];
                frequency.put(leftNum, frequency.get(leftNum) - 1);
                
                // Remove from map if count becomes 0
                if (frequency.get(leftNum) == 0) {
                    frequency.remove(leftNum);
                }
                
                left++;
            }
            
            // Add all subarrays ending at 'right' with at most k distinct
            // These are subarrays starting from any position in [left, right]
            count += (right - left + 1);
        }
        
        return count;
    }
    
    /**
     * ============================================================================
     * APPROACH 2: SINGLE PASS WITH TWO WINDOWS (OPTIMIZED)
     * ============================================================================
     * 
     * CONCEPT:
     * - Instead of two separate passes, maintain two windows in one pass
     * - One window for "at most k"
     * - One window for "at most k-1"
     * - Calculate difference on the fly
     * 
     * ADVANTAGE:
     * - Single pass through array
     * - Better cache locality
     * - Slightly faster in practice
     * 
     * TRADE-OFF:
     * - More complex code
     * - Harder to understand
     * - Not significantly better for interviews
     * 
     * TIME: O(n), SPACE: O(k)
     */
    public int subarraysWithKDistinct_SinglePass(int[] nums, int k) {
        if (k == 0) return 0;
        
        int count = 0;
        int left1 = 0, left2 = 0; // Two left pointers
        Map<Integer, Integer> freq1 = new HashMap<>(); // For at most k
        Map<Integer, Integer> freq2 = new HashMap<>(); // For at most k-1
        
        for (int right = 0; right < nums.length; right++) {
            int num = nums[right];
            
            // Update both windows
            freq1.put(num, freq1.getOrDefault(num, 0) + 1);
            freq2.put(num, freq2.getOrDefault(num, 0) + 1);
            
            // Shrink first window (at most k)
            while (freq1.size() > k) {
                int leftNum = nums[left1];
                freq1.put(leftNum, freq1.get(leftNum) - 1);
                if (freq1.get(leftNum) == 0) {
                    freq1.remove(leftNum);
                }
                left1++;
            }
            
            // Shrink second window (at most k-1)
            while (freq2.size() > k - 1) {
                int leftNum = nums[left2];
                freq2.put(leftNum, freq2.get(leftNum) - 1);
                if (freq2.get(leftNum) == 0) {
                    freq2.remove(leftNum);
                }
                left2++;
            }
            
            // Count subarrays with exactly k distinct
            // = (subarrays ending at right with at most k) - (at most k-1)
            count += (right - left1 + 1) - (right - left2 + 1);
        }
        
        return count;
    }
    
    /**
     * ============================================================================
     * APPROACH 3: BRUTE FORCE (FOR UNDERSTANDING)
     * ============================================================================
     * 
     * ALGORITHM:
     * - Check all possible subarrays
     * - For each subarray, count distinct integers
     * - If count equals k, increment result
     * 
     * WHY SHOW THIS:
     * - Helps understand what we're actually counting
     * - Baseline for optimization comparison
     * - Good for verifying other approaches
     * 
     * PROBLEM:
     * - Too slow for large inputs: O(n²) or O(n³)
     * - Not suitable for interviews (except as starting point)
     * 
     * TIME: O(n²) with Set, O(n³) with naive counting
     * SPACE: O(k) for Set
     */
    public int subarraysWithKDistinct_BruteForce(int[] nums, int k) {
        int count = 0;
        
        // Try all starting positions
        for (int i = 0; i < nums.length; i++) {
            Set<Integer> distinct = new HashSet<>();
            
            // Try all ending positions from i
            for (int j = i; j < nums.length; j++) {
                distinct.add(nums[j]);
                
                // Check if this subarray has exactly k distinct
                if (distinct.size() == k) {
                    count++;
                } else if (distinct.size() > k) {
                    // No point continuing, will only get more distinct
                    break;
                }
            }
        }
        
        return count;
    }
    
    /**
     * ============================================================================
     * APPROACH 4: USING ARRAY INSTEAD OF HASHMAP (OPTIMIZATION)
     * ============================================================================
     * 
     * CONCEPT:
     * - If input values are bounded (e.g., 1 to n)
     * - Use array instead of HashMap for frequency counting
     * - Faster constant factors
     * 
     * WHEN TO USE:
     * - Values are in range [1, n] or [0, n]
     * - Problem specifies bounded values
     * - Need maximum performance
     * 
     * TRADE-OFF:
     * - Uses O(n) space instead of O(k)
     * - Slightly faster in practice
     * - Less flexible
     * 
     * TIME: O(n), SPACE: O(n)
     */
    public int subarraysWithKDistinct_Array(int[] nums, int k) {
        if (k == 0) return 0;
        
        // Assume values are in range [1, n]
        int maxVal = 0;
        for (int num : nums) {
            maxVal = Math.max(maxVal, num);
        }
        
        return atMostKDistinct_Array(nums, k, maxVal) - 
               atMostKDistinct_Array(nums, k - 1, maxVal);
    }
    
    private int atMostKDistinct_Array(int[] nums, int k, int maxVal) {
        int count = 0;
        int left = 0;
        int distinctCount = 0;
        int[] frequency = new int[maxVal + 1];
        
        for (int right = 0; right < nums.length; right++) {
            // Add right element
            if (frequency[nums[right]] == 0) {
                distinctCount++;
            }
            frequency[nums[right]]++;
            
            // Shrink window
            while (distinctCount > k) {
                frequency[nums[left]]--;
                if (frequency[nums[left]] == 0) {
                    distinctCount--;
                }
                left++;
            }
            
            count += (right - left + 1);
        }
        
        return count;
    }
    
    /**
     * ============================================================================
     * APPROACH 5: WITH DETAILED TRACKING (EDUCATIONAL)
     * ============================================================================
     * 
     * VARIATION:
     * - Track and print all subarrays found
     * - Good for debugging and understanding
     * - Shows exactly what's being counted
     * 
     * USE CASE:
     * - Learning and verification
     * - Debugging
     * - Understanding the problem deeply
     */
    public int subarraysWithKDistinct_Verbose(int[] nums, int k) {
        if (k == 0) return 0;
        
        List<String> subarrays = new ArrayList<>();
        
        for (int i = 0; i < nums.length; i++) {
            Set<Integer> distinct = new HashSet<>();
            
            for (int j = i; j < nums.length; j++) {
                distinct.add(nums[j]);
                
                if (distinct.size() == k) {
                    // Build subarray string for visualization
                    StringBuilder sb = new StringBuilder("[");
                    for (int idx = i; idx <= j; idx++) {
                        sb.append(nums[idx]);
                        if (idx < j) sb.append(",");
                    }
                    sb.append("]");
                    subarrays.add(sb.toString());
                } else if (distinct.size() > k) {
                    break;
                }
            }
        }
        
        // Print all found subarrays
        System.out.println("Found " + subarrays.size() + " subarrays with exactly " + k + " distinct:");
        for (String s : subarrays) {
            System.out.println("  " + s);
        }
        
        return subarrays.size();
    }
    
    /**
     * ============================================================================
     * APPROACH 6: ALTERNATIVE FORMULA UNDERSTANDING
     * ============================================================================
     * 
     * CONCEPT:
     * - Think of it as finding the "range" of valid left pointers
     * - For each right pointer, find leftmost and rightmost valid left pointers
     * 
     * EXPLANATION:
     * - leftMost: smallest left where window has exactly k distinct
     * - rightMost: largest left where window has exactly k distinct
     * - All positions in [leftMost, rightMost] form valid subarrays
     * 
     * This is essentially what the two-window approach computes!
     * - rightMost = left pointer for "at most k"
     * - leftMost = left pointer for "at most k-1" + 1
     * 
     * TIME: O(n), SPACE: O(k)
     */
    public int subarraysWithKDistinct_Alternative(int[] nums, int k) {
        if (k == 0) return 0;
        
        int count = 0;
        int leftMin = 0, leftMax = 0;
        Map<Integer, Integer> freqMin = new HashMap<>();
        Map<Integer, Integer> freqMax = new HashMap<>();
        
        for (int right = 0; right < nums.length; right++) {
            int num = nums[right];
            
            // Window that allows at most k distinct
            freqMin.put(num, freqMin.getOrDefault(num, 0) + 1);
            while (freqMin.size() > k) {
                int leftNum = nums[leftMin];
                freqMin.put(leftNum, freqMin.get(leftNum) - 1);
                if (freqMin.get(leftNum) == 0) {
                    freqMin.remove(leftNum);
                }
                leftMin++;
            }
            
            // Window that allows at most k-1 distinct
            freqMax.put(num, freqMax.getOrDefault(num, 0) + 1);
            while (freqMax.size() >= k) {
                int leftNum = nums[leftMax];
                freqMax.put(leftNum, freqMax.get(leftNum) - 1);
                if (freqMax.get(leftNum) == 0) {
                    freqMax.remove(leftNum);
                }
                leftMax++;
            }
            
            // Number of valid left positions that give exactly k distinct
            count += leftMax - leftMin;
        }
        
        return count;
    }
    
    /**
     * ============================================================================
     * TEST CASES AND DEMONSTRATIONS
     * ============================================================================
     */
    public static void main(String[] args) {
        SubarraysKDistinct solution = new SubarraysKDistinct();
        
        System.out.println("=== TEST CASE 1: Basic Example ===");
        int[] nums1 = {1, 2, 1, 2, 3};
        int k1 = 2;
        System.out.println("Input: nums = " + Arrays.toString(nums1) + ", k = " + k1);
        System.out.println("Two Windows: " + solution.subarraysWithKDistinct(nums1, k1));
        System.out.println("Single Pass: " + solution.subarraysWithKDistinct_SinglePass(nums1, k1));
        System.out.println("Brute Force: " + solution.subarraysWithKDistinct_BruteForce(nums1, k1));
        System.out.println("Array-based: " + solution.subarraysWithKDistinct_Array(nums1, k1));
        System.out.println("\nVerbose output:");
        solution.subarraysWithKDistinct_Verbose(nums1, k1);
        System.out.println("Expected: 7\n");
        
        System.out.println("=== TEST CASE 2: Three Distinct ===");
        int[] nums2 = {1, 2, 1, 3, 4};
        int k2 = 3;
        System.out.println("Input: nums = " + Arrays.toString(nums2) + ", k = " + k2);
        System.out.println("Two Windows: " + solution.subarraysWithKDistinct(nums2, k2));
        System.out.println("\nVerbose output:");
        solution.subarraysWithKDistinct_Verbose(nums2, k2);
        System.out.println("Expected: 3\n");
        
        System.out.println("=== TEST CASE 3: K = 1 (All Same) ===");
        int[] nums3 = {1, 1, 1, 1};
        int k3 = 1;
        System.out.println("Input: nums = " + Arrays.toString(nums3) + ", k = " + k3);
        System.out.println("Two Windows: " + solution.subarraysWithKDistinct(nums3, k3));
        System.out.println("Expected: 10 (n*(n+1)/2 = 4*5/2 = 10)\n");
        
        System.out.println("=== TEST CASE 4: K = Array Length ===");
        int[] nums4 = {1, 2, 3};
        int k4 = 3;
        System.out.println("Input: nums = " + Arrays.toString(nums4) + ", k = " + k4);
        System.out.println("Two Windows: " + solution.subarraysWithKDistinct(nums4, k4));
        System.out.println("\nVerbose output:");
        solution.subarraysWithKDistinct_Verbose(nums4, k4);
        System.out.println("Expected: 1 (only the full array)\n");
        
        System.out.println("=== TEST CASE 5: K = 1 (Different Elements) ===");
        int[] nums5 = {1, 2, 3, 4};
        int k5 = 1;
        System.out.println("Input: nums = " + Arrays.toString(nums5) + ", k = " + k5);
        System.out.println("Two Windows: " + solution.subarraysWithKDistinct(nums5, k5));
        System.out.println("Expected: 4 (each single element)\n");
        
        System.out.println("=== TEST CASE 6: All Different ===");
        int[] nums6 = {1, 2, 3, 4, 5};
        int k6 = 2;
        System.out.println("Input: nums = " + Arrays.toString(nums6) + ", k = " + k6);
        System.out.println("Two Windows: " + solution.subarraysWithKDistinct(nums6, k6));
        System.out.println("\nVerbose output:");
        solution.subarraysWithKDistinct_Verbose(nums6, k6);
        System.out.println("Expected: 4 ([1,2], [2,3], [3,4], [4,5])\n");
        
        System.out.println("=== TEST CASE 7: Complex Pattern ===");
        int[] nums7 = {1, 2, 1, 2, 1, 2};
        int k7 = 2;
        System.out.println("Input: nums = " + Arrays.toString(nums7) + ", k = " + k7);
        System.out.println("Two Windows: " + solution.subarraysWithKDistinct(nums7, k7));
        System.out.println("\nVerbose output:");
        solution.subarraysWithKDistinct_Verbose(nums7, k7);
        
        // Performance comparison
        System.out.println("\n=== PERFORMANCE COMPARISON ===");
        int[] largeArray = new int[10000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = (i % 100) + 1;
        }
        int kLarge = 10;
        
        long start, end;
        
        start = System.nanoTime();
        int result1 = solution.subarraysWithKDistinct(largeArray, kLarge);
        end = System.nanoTime();
        System.out.println("Two Windows: " + result1 + " (Time: " + (end - start) / 1000000.0 + " ms)");
        
        start = System.nanoTime();
        int result2 = solution.subarraysWithKDistinct_SinglePass(largeArray, kLarge);
        end = System.nanoTime();
        System.out.println("Single Pass: " + result2 + " (Time: " + (end - start) / 1000000.0 + " ms)");
        
        start = System.nanoTime();
        int result3 = solution.subarraysWithKDistinct_Array(largeArray, kLarge);
        end = System.nanoTime();
        System.out.println("Array-based: " + result3 + " (Time: " + (end - start) / 1000000.0 + " ms)");
        
        // Manual verification for small example
        System.out.println("\n=== MANUAL VERIFICATION ===");
        int[] test = {1, 2, 1, 2, 3};
        System.out.println("Array: " + Arrays.toString(test) + ", k = 2");
        System.out.println("\nStep-by-step calculation:");
        System.out.println("AT MOST 2: " + solution.atMostKDistinct(test, 2));
        System.out.println("AT MOST 1: " + solution.atMostKDistinct(test, 1));
        System.out.println("EXACTLY 2: " + solution.subarraysWithKDistinct(test, 2));
    }
}

/**
 * ============================================================================
 * COMPREHENSIVE ANALYSIS AND INTERVIEW GUIDE
 * ============================================================================
 * 
 * THE GENIUS INSIGHT - WHY THIS PROBLEM IS HARD:
 * 
 * "EXACTLY k" is much harder than "AT MOST k" in sliding window problems!
 * 
 * Why?
 * - With "AT MOST k": Window is either valid or invalid (binary state)
 * - With "EXACTLY k": Window can be invalid in two ways (too few OR too many)
 * - "AT MOST k": Easy to know when to shrink (when size > k)
 * - "EXACTLY k": Hard to know when to shrink (need to maintain exactly k)
 * 
 * THE MATHEMATICAL TRANSFORMATION:
 * 
 * exactly(k) = atMost(k) - atMost(k-1)
 * 
 * This works because:
 * - atMost(k) includes subarrays with 1, 2, ..., k distinct
 * - atMost(k-1) includes subarrays with 1, 2, ..., k-1 distinct
 * - Subtracting removes all except k distinct
 * 
 * VISUAL PROOF:
 * 
 * Set Theory Perspective:
 * A = {subarrays with ≤ k distinct}
 * B = {subarrays with ≤ k-1 distinct}
 * A - B = {subarrays with exactly k distinct}
 * 
 * Example: k = 3
 * atMost(3) = {1-distinct} ∪ {2-distinct} ∪ {3-distinct}
 * atMost(2) = {1-distinct} ∪ {2-distinct}
 * exactly(3) = atMost(3) - atMost(2) = {3-distinct}
 * 
 * UNDERSTANDING "AT MOST K" COUNTING:
 * 
 * For each position of right pointer, we count subarrays ENDING at right.
 * 
 * If window [left, right] has ≤ k distinct:
 * - Subarray [left, right] is valid
 * - Subarray [left+1, right] is valid
 * - Subarray [left+2, right] is valid
 * - ...
 * - Subarray [right, right] is valid
 * 
 * Total: (right - left + 1) valid subarrays ending at right
 * 
 * APPROACH COMPARISON:
 * 
 * 1. Two Windows (Approach 1) - BEST FOR INTERVIEWS
 *    ✓ Clean separation of concerns
 *    ✓ Easy to understand and explain
 *    ✓ Reusable helper function
 *    ✓ O(n) time, two passes
 * 
 * 2. Single Pass (Approach 2) - OPTIMIZATION
 *    ✓ Faster (single pass)
 *    ✓ Better cache locality
 *    ✗ More complex code
 *    ✗ Harder to debug
 * 
 * 3. Brute Force (Approach 3) - BASELINE
 *    ✗ Too slow O(n²)
 *    + Good for understanding
 *    + Easy to verify
 * 
 * 4. Array-based (Approach 4) - PERFORMANCE BOOST
 *    ✓ Faster constant factors
 *    ✗ Requires bounded values
 *    ✗ Uses more space
 * 
 * INTERVIEW STRATEGY:
 * 
 * 1. Recognize the Pattern (30 sec)
 *    - "Count subarrays" + "exactly k" → Hard to do directly
 *    - Mention the transformation: exactly(k) = atMost(k) - atMost(k-1)
 * 
 * 2. Explain "At Most K" Logic (1 min)
 *    - Sliding window with two pointers
 *    - Expand right, shrink left when invalid
 *    - Count subarrays ending at each position
 * 
 * 3. Walk Through Small Example (1-2 min)
 *    - Show atMost(k) calculation
 *    - Show atMost(k-1) calculation
 *    - Demonstrate subtraction
 * 
 * 4. Code the Solution (3-4 min)
 *    - Implement atMostKDistinct helper
 *    - Main function does subtraction
 * 
 * 5. Verify with Test Case (1 min)
 * 
 * 6. Analyze Complexity (30 sec)
 *    - Time: O(n) - two linear passes
 *    - Space: O(k) - HashMap size
 * 
 * COMMON MISTAKES TO AVOID:
 * 
 * ✗ Trying to solve "exactly k" directly
 *   → Use the transformation!
 * 
 * ✗ Forgetting to remove from map when count = 0
 *   → map.size() will be wrong
 * 
 * ✗ Using wrong formula for counting
 *   → It's (right - left + 1), not something else
 * 
 * ✗ Edge case: k = 0
 *   → Should return 0
 * 
 * ✗ Not using 'while' for shrinking
 *   → Might need multiple shrinks
 * 
 * KEY INSIGHTS TO MENTION:
 * 
 * 1. Transformation Trick:
 *    "Instead of finding exactly k, I'll find at most k and subtract at most k-1"
 * 
 * 2. Counting Logic:
 */
