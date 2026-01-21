/**
 * LONGEST BITONIC SUBSEQUENCE - COMPREHENSIVE ANALYSIS AND IMPLEMENTATION
 * =========================================================================
 * 
 * PROBLEM UNDERSTANDING:
 * ---------------------
 * A bitonic sequence is one that:
 * 1. First STRICTLY increases
 * 2. Then STRICTLY decreases
 * 
 * Key points:
 * - Elements don't need to be contiguous
 * - We're finding a SUBSEQUENCE (not subarray)
 * - The sequence must have both increasing and decreasing parts
 * 
 * Examples:
 * [1, 11, 2, 10, 4, 5, 2, 1] → Bitonic: [1, 2, 10, 4, 2, 1] or [1, 11, 10, 5, 2, 1]
 * [12, 11, 40, 5, 3, 1] → Bitonic: [12, 40, 5, 3, 1]
 * [80, 60, 30, 40, 20, 10] → Bitonic: [80, 60, 30, 20, 10] or [30, 40, 20, 10]
 * 
 * INTERVIEW APPROACH - HOW TO THINK ABOUT THIS:
 * ==============================================
 * 
 * Step 1: RECOGNIZE THE PATTERN
 * - "Bitonic" = "Increasing then Decreasing"
 * - "Subsequence" = classic DP problem (think LIS - Longest Increasing Subsequence)
 * 
 * Step 2: BREAK DOWN THE PROBLEM
 * - A bitonic sequence has a PEAK element
 * - Everything before peak is increasing
 * - Everything after peak is decreasing
 * 
 * Step 3: CONNECT TO KNOWN PROBLEMS
 * - If we know LIS (Longest Increasing Subsequence), we can use it!
 * - For each element as potential peak:
 *   * Find LIS ending at that element (left part)
 *   * Find LDS (Longest Decreasing Subsequence) starting from that element (right part)
 *   * Bitonic length = LIS[i] + LDS[i] - 1 (subtract 1 because peak is counted twice)
 * 
 * Step 4: WHAT TO SAY IN INTERVIEW
 * "I notice this is similar to Longest Increasing Subsequence. Since bitonic means
 * increase then decrease, I can compute LIS from left for each position, and LIS
 * from right (which gives decreasing). Then for each position as a potential peak,
 * I combine both values."
 * 
 * COMPLEXITY ANALYSIS:
 * -------------------
 * Time Complexity:
 * - Method 1 (Basic DP): O(n²) - two O(n²) LIS computations
 * - Method 2 (Optimized): O(n log n) - using binary search in LIS
 * 
 * Space Complexity: O(n) - for storing LIS and LDS arrays
 * 
 * EDGE CASES TO CONSIDER:
 * ----------------------
 * 1. Array with all increasing elements: [1,2,3,4,5]
 *    - Last element is peak, result = n
 * 2. Array with all decreasing elements: [5,4,3,2,1]
 *    - First element is peak, result = n
 * 3. Single element: [5]
 *    - Result = 1
 * 4. Two elements: [1,2] or [2,1]
 *    - Result = 2
 * 5. All same elements: [5,5,5,5]
 *    - Result = 1 (strictly increasing/decreasing)
 */

import java.util.Arrays;

class LongestBitonicSubsequence {
    
    /**
     * METHOD 1: BASIC DYNAMIC PROGRAMMING APPROACH - O(n²)
     * ===================================================
     * 
     * INTUITION:
     * For each index i, we need to know:
     * 1. Length of longest increasing subsequence ENDING at i (lis[i])
     * 2. Length of longest decreasing subsequence STARTING from i (lds[i])
     * 
     * Then, treating i as the peak:
     * Bitonic length with i as peak = lis[i] + lds[i] - 1
     * 
     * WHY -1? Because element at index i is counted in both lis[i] and lds[i]
     * 
     * ALGORITHM WALKTHROUGH with example [1, 11, 2, 10, 4, 5, 2, 1]:
     * 
     * Step 1: Compute LIS ending at each position
     * Index:  0   1   2   3   4   5   6   7
     * Array: [1, 11,  2, 10,  4,  5,  2,  1]
     * LIS:   [1,  2,  2,  3,  3,  4,  3,  3]
     * 
     * Explanation:
     * - lis[0] = 1 (just [1])
     * - lis[1] = 2 ([1, 11])
     * - lis[2] = 2 ([1, 2])
     * - lis[3] = 3 ([1, 2, 10])
     * - lis[4] = 3 ([1, 2, 4])
     * - lis[5] = 4 ([1, 2, 4, 5])
     * - lis[6] = 3 ([1, 2, 2] - but strictly increasing, so actually [1, 2])
     * - lis[7] = 3 (similar reasoning)
     * 
     * Step 2: Compute LDS starting from each position
     * Index:  0   1   2   3   4   5   6   7
     * Array: [1, 11,  2, 10,  4,  5,  2,  1]
     * LDS:   [1,  5,  2,  4,  3,  3,  2,  1]
     * 
     * Explanation (computing from right to left):
     * - lds[7] = 1 (just [1])
     * - lds[6] = 2 ([2, 1])
     * - lds[5] = 3 ([5, 2, 1])
     * - lds[4] = 3 ([4, 2, 1])
     * - lds[3] = 4 ([10, 4, 2, 1])
     * - lds[2] = 2 ([2, 1])
     * - lds[1] = 5 ([11, 10, 4, 2, 1] or similar)
     * - lds[0] = 1 (just [1])
     * 
     * Step 3: Find maximum (lis[i] + lds[i] - 1)
     * Index:       0   1   2   3   4   5   6   7
     * lis+lds-1:   1   6   3   6   5   6   4   3
     * 
     * Maximum = 6 (at indices 1, 3, or 5)
     */
    public static int longestBitonicSubsequenceBasic(int[] arr) {
        int n = arr.length;
        if (n == 0) return 0;
        if (n == 1) return 1;
        
        // Step 1: Compute LIS ending at each position
        int[] lis = computeLIS(arr);
        
        // Step 2: Compute LDS starting from each position
        // Trick: Reverse array, compute LIS, then reverse result
        // This gives us LDS for original array
        int[] lds = computeLDS(arr);
        
        // Step 3: Find maximum bitonic length
        int maxLength = 0;
        for (int i = 0; i < n; i++) {
            // A valid bitonic sequence must have both increasing and decreasing parts
            // So both lis[i] and lds[i] should be > 1, OR we can just take max
            // Actually, if lis[i] = 1 or lds[i] = 1, it means we have only one part
            // But the problem typically counts this as valid (peak at boundary)
            maxLength = Math.max(maxLength, lis[i] + lds[i] - 1);
        }
        
        return maxLength;
    }
    
    /**
     * COMPUTE LONGEST INCREASING SUBSEQUENCE ENDING AT EACH POSITION
     * 
     * CLASSIC LIS DP APPROACH:
     * lis[i] = length of longest increasing subsequence ending at index i
     * 
     * Recurrence:
     * lis[i] = 1 + max(lis[j]) for all j < i where arr[j] < arr[i]
     * lis[i] = 1 if no such j exists
     * 
     * TIME: O(n²), SPACE: O(n)
     */
    private static int[] computeLIS(int[] arr) {
        int n = arr.length;
        int[] lis = new int[n];
        
        // Base case: every element is an increasing subsequence of length 1
        Arrays.fill(lis, 1);
        
        // Fill lis array
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // If arr[j] < arr[i], we can extend the subsequence ending at j
                if (arr[j] < arr[i]) {
                    lis[i] = Math.max(lis[i], lis[j] + 1);
                }
            }
        }
        
        return lis;
    }
    
    /**
     * COMPUTE LONGEST DECREASING SUBSEQUENCE STARTING FROM EACH POSITION
     * 
     * APPROACH: Compute LIS on reversed array, then reverse the result
     * 
     * Why this works:
     * - LDS starting from i in original array
     * - = LIS ending at (n-1-i) in reversed array
     * 
     * Alternative: Iterate from right to left with same logic as LIS
     * but with arr[j] > arr[i] condition
     */
    private static int[] computeLDS(int[] arr) {
        int n = arr.length;
        int[] lds = new int[n];
        Arrays.fill(lds, 1);
        
        // Compute from right to left
        for (int i = n - 2; i >= 0; i--) {
            for (int j = i + 1; j < n; j++) {
                // If arr[i] > arr[j], we can extend the decreasing subsequence
                if (arr[i] > arr[j]) {
                    lds[i] = Math.max(lds[i], lds[j] + 1);
                }
            }
        }
        
        return lds;
    }
    
    /**
     * METHOD 2: OPTIMIZED APPROACH USING BINARY SEARCH - O(n log n)
     * =============================================================
     * 
     * OPTIMIZATION IDEA:
     * The O(n²) LIS can be optimized to O(n log n) using binary search.
     * 
     * KEY INSIGHT:
     * Maintain an auxiliary array that stores the smallest tail element
     * for all increasing subsequences of length i+1 at index i.
     * 
     * WHY THIS WORKS:
     * If we have two increasing subsequences of same length, the one
     * with smaller ending element is more promising (can be extended more easily).
     * 
     * EXAMPLE: [1, 11, 2, 10, 4, 5, 2, 1]
     * 
     * Processing:
     * i=0, val=1:  tails=[1], len=1
     * i=1, val=11: tails=[1,11], len=2
     * i=2, val=2:  tails=[1,2], len=2 (replace 11 with 2, better for future)
     * i=3, val=10: tails=[1,2,10], len=3
     * i=4, val=4:  tails=[1,2,4], len=3 (replace 10 with 4)
     * i=5, val=5:  tails=[1,2,4,5], len=4
     * i=6, val=2:  tails=[1,2,4,5], len=4 (2 already exists at pos 1)
     * i=7, val=1:  tails=[1,2,4,5], len=4 (1 already exists at pos 0)
     */
    public static int longestBitonicSubsequenceOptimized(int[] arr) {
        int n = arr.length;
        if (n == 0) return 0;
        if (n == 1) return 1;
        
        int[] lis = computeLISOptimized(arr);
        int[] lds = computeLDSOptimized(arr);
        
        int maxLength = 0;
        for (int i = 0; i < n; i++) {
            maxLength = Math.max(maxLength, lis[i] + lds[i] - 1);
        }
        
        return maxLength;
    }
    
    /**
     * OPTIMIZED LIS USING BINARY SEARCH
     * 
     * TIME: O(n log n)
     * SPACE: O(n)
     * 
     * tails[i] = smallest ending element of all increasing subsequences of length i+1
     */
    private static int[] computeLISOptimized(int[] arr) {
        int n = arr.length;
        int[] lis = new int[n];
        int[] tails = new int[n];
        int length = 0;
        
        for (int i = 0; i < n; i++) {
            // Binary search to find position where arr[i] should be placed
            int pos = binarySearch(tails, 0, length, arr[i]);
            
            tails[pos] = arr[i];
            
            if (pos == length) {
                length++;
            }
            
            lis[i] = pos + 1;
        }
        
        return lis;
    }
    
    /**
     * OPTIMIZED LDS - COMPUTE FROM RIGHT TO LEFT
     */
    private static int[] computeLDSOptimized(int[] arr) {
        int n = arr.length;
        int[] lds = new int[n];
        int[] tails = new int[n];
        int length = 0;
        
        // Process from right to left
        for (int i = n - 1; i >= 0; i--) {
            // For decreasing, we want to find position in increasing order
            // of reversed values (which is decreasing in original)
            int pos = binarySearch(tails, 0, length, arr[i]);
            
            tails[pos] = arr[i];
            
            if (pos == length) {
                length++;
            }
            
            lds[i] = pos + 1;
        }
        
        return lds;
    }
    
    /**
     * BINARY SEARCH HELPER
     * 
     * Find the index of the smallest element >= target in tails[0...len-1]
     * If all elements are < target, return len
     * 
     * This is essentially finding the leftmost position to insert target
     * to maintain sorted order
     */
    private static int binarySearch(int[] tails, int left, int right, int target) {
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (tails[mid] < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left;
    }
    
    /**
     * METHOD 3: SPACE-OPTIMIZED WITH DETAILED TRACKING
     * ================================================
     * 
     * If we need to print the actual bitonic subsequence, not just length,
     * we need to track parent pointers.
     * 
     * This is more for educational purposes in interviews.
     */
    public static void printLongestBitonicSubsequence(int[] arr) {
        int n = arr.length;
        if (n == 0) {
            System.out.println("Empty array");
            return;
        }
        
        int[] lis = new int[n];
        int[] lds = new int[n];
        int[] lisParent = new int[n];
        int[] ldsParent = new int[n];
        
        Arrays.fill(lis, 1);
        Arrays.fill(lds, 1);
        Arrays.fill(lisParent, -1);
        Arrays.fill(ldsParent, -1);
        
        // Compute LIS with parent tracking
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (arr[j] < arr[i] && lis[j] + 1 > lis[i]) {
                    lis[i] = lis[j] + 1;
                    lisParent[i] = j;
                }
            }
        }
        
        // Compute LDS with parent tracking
        for (int i = n - 2; i >= 0; i--) {
            for (int j = i + 1; j < n; j++) {
                if (arr[i] > arr[j] && lds[j] + 1 > lds[i]) {
                    lds[i] = lds[j] + 1;
                    ldsParent[i] = j;
                }
            }
        }
        
        // Find peak
        int maxLength = 0;
        int peakIndex = 0;
        for (int i = 0; i < n; i++) {
            if (lis[i] + lds[i] - 1 > maxLength) {
                maxLength = lis[i] + lds[i] - 1;
                peakIndex = i;
            }
        }
        
        System.out.println("Longest Bitonic Subsequence Length: " + maxLength);
        System.out.println("Peak at index " + peakIndex + " (value: " + arr[peakIndex] + ")");
        
        // Print increasing part
        printIncreasingPart(arr, lisParent, peakIndex);
        
        // Print decreasing part (excluding peak)
        if (ldsParent[peakIndex] != -1) {
            printDecreasingPart(arr, ldsParent, ldsParent[peakIndex]);
        }
        System.out.println();
    }
    
    private static void printIncreasingPart(int[] arr, int[] parent, int index) {
        if (index == -1) return;
        printIncreasingPart(arr, parent, parent[index]);
        System.out.print(arr[index] + " ");
    }
    
    private static void printDecreasingPart(int[] arr, int[] parent, int index) {
        if (index == -1) return;
        System.out.print(arr[index] + " ");
        printDecreasingPart(arr, parent, parent[index]);
    }
    
    /**
     * TEST CASES AND MAIN METHOD
     * ==========================
     */
    public static void main(String[] args) {
        // Test Case 1: Standard bitonic sequence
        int[] arr1 = {1, 11, 2, 10, 4, 5, 2, 1};
        System.out.println("=== Test Case 1 ===");
        System.out.println("Array: " + Arrays.toString(arr1));
        System.out.println("Basic Method: " + longestBitonicSubsequenceBasic(arr1));
        System.out.println("Optimized Method: " + longestBitonicSubsequenceOptimized(arr1));
        printLongestBitonicSubsequence(arr1);
        
        // Test Case 2: Decreasing then increasing
        int[] arr2 = {12, 11, 40, 5, 3, 1};
        System.out.println("\n=== Test Case 2 ===");
        System.out.println("Array: " + Arrays.toString(arr2));
        System.out.println("Basic Method: " + longestBitonicSubsequenceBasic(arr2));
        System.out.println("Optimized Method: " + longestBitonicSubsequenceOptimized(arr2));
        
        // Test Case 3: All increasing
        int[] arr3 = {1, 2, 3, 4, 5};
        System.out.println("\n=== Test Case 3 ===");
        System.out.println("Array: " + Arrays.toString(arr3));
        System.out.println("Basic Method: " + longestBitonicSubsequenceBasic(arr3));
        System.out.println("Optimized Method: " + longestBitonicSubsequenceOptimized(arr3));
        
        // Test Case 4: All decreasing
        int[] arr4 = {5, 4, 3, 2, 1};
        System.out.println("\n=== Test Case 4 ===");
        System.out.println("Array: " + Arrays.toString(arr4));
        System.out.println("Basic Method: " + longestBitonicSubsequenceBasic(arr4));
        System.out.println("Optimized Method: " + longestBitonicSubsequenceOptimized(arr4));
        
        // Test Case 5: Complex bitonic
        int[] arr5 = {80, 60, 30, 40, 20, 10};
        System.out.println("\n=== Test Case 5 ===");
        System.out.println("Array: " + Arrays.toString(arr5));
        System.out.println("Basic Method: " + longestBitonicSubsequenceBasic(arr5));
        System.out.println("Optimized Method: " + longestBitonicSubsequenceOptimized(arr5));
        
        // Test Case 6: Single element
        int[] arr6 = {5};
        System.out.println("\n=== Test Case 6 ===");
        System.out.println("Array: " + Arrays.toString(arr6));
        System.out.println("Basic Method: " + longestBitonicSubsequenceBasic(arr6));
        System.out.println("Optimized Method: " + longestBitonicSubsequenceOptimized(arr6));
    }
}

/**
 * INTERVIEW TIPS AND COMMON MISTAKES:
 * ====================================
 * 
 * 1. CLARIFY THE PROBLEM:
 *    - Ask if strictly increasing/decreasing or non-strictly
 *    - Ask if subsequence or subarray (very different!)
 *    - Ask about edge cases (empty array, single element)
 * 
 * 2. START WITH BRUTE FORCE:
 *    - Mention O(2^n) approach of trying all subsequences
 *    - Explain why it's too slow
 *    - This shows you understand the problem space
 * 
 * 3. CONNECT TO KNOWN PROBLEMS:
 *    - "This reminds me of LIS..."
 *    - Shows pattern recognition
 *    - Demonstrates you've practiced similar problems
 * 
 * 4. COMMON MISTAKES:
 *    - Forgetting to subtract 1 when combining lis[i] + lds[i]
 *    - Computing LDS incorrectly (direction matters!)
 *    - Not handling edge cases (n=0, n=1, all same elements)
 *    - Confusing subsequence with subarray
 * 
 * 5. OPTIMIZE GRADUALLY:
 *    - Start with O(n²) solution
 *    - Only move to O(n log n) if interviewer asks or if time permits
 *    - Explain trade-offs clearly
 * 
 * 6. TEST YOUR CODE:
 *    - Walk through a small example
 *    - Test edge cases
 *    - Don't just say "it works"
 * 
 * 7. TIME MANAGEMENT:
 *    - 5 min: Understand problem, clarify, examples
 *    - 10 min: Explain approach, discuss complexity
 *    - 20 min: Code the O(n²) solution
 *    - 5 min: Test and discuss optimizations
 * 
 * VARIATIONS YOU MIGHT ENCOUNTER:
 * ===============================
 * 1. Print the actual sequence (need parent pointers)
 * 2. Count number of bitonic subsequences (different DP)
 * 3. Longest bitonic subarray (different problem - O(n))
 * 4. K-tonic sequence (increases and decreases k times)
 */
