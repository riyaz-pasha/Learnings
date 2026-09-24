import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class LongestArithmeticSubsequence2 {

    // Time: O(n²) because we check all pairs.
    // Space: O(n²) in the worst case, due to DP storage.
    public int longestArithSeqLength(int[] nums) {
        int n = nums.length;
        if (n <= 2)
            return n;

        // dp[i]: Map from difference -> length of arithmetic subsequence ending at i
        Map<Integer, Integer>[] dp = new HashMap[n];
        int maxLen = 2; // Minimum arithmetic subsequence length

        for (int i = 0; i < n; i++) {
            dp[i] = new HashMap<>();

            for (int j = 0; j < i; j++) {
                int diff = nums[i] - nums[j];

                // Length of subsequence with this difference ending at j
                int len = dp[j].getOrDefault(diff, 1);

                // Extend subsequence ending at j by nums[i]
                dp[i].put(diff, len + 1);

                maxLen = Math.max(maxLen, dp[i].get(diff));
            }
        }

        return maxLen;
    }

}

/**
 * LONGEST ARITHMETIC SUBSEQUENCE - ANY DIFFERENCE
 * 
 * Given an array nums of integers, return the length of the longest arithmetic 
 * subsequence in nums.
 * 
 * Unlike the previous problem, here we DON'T know the difference beforehand.
 * We need to consider ALL possible differences between pairs of elements.
 * 
 * ===============================================================================
 * KEY INSIGHT:
 * ===============================================================================
 * For any arithmetic subsequence, we need at least 2 elements to determine the 
 * difference. So we can:
 * 1. Consider every pair of elements (i, j) where i < j
 * 2. The difference = nums[j] - nums[i]
 * 3. Use DP to extend this arithmetic sequence beyond j
 * 
 * DP State: dp[i][diff] = length of longest arithmetic subsequence ending at 
 * index i with difference 'diff'
 * 
 * ===============================================================================
 * ALGORITHM APPROACH:
 * ===============================================================================
 * 1. Use array of HashMaps: dp[i] = Map<difference, length>
 * 2. For each pair (i, j) where i < j:
 *    - Calculate diff = nums[j] - nums[i]
 *    - dp[j][diff] = dp[i][diff] + 1 (or 2 if dp[i][diff] doesn't exist)
 * 3. Track maximum length across all positions and differences
 * 
 * ===============================================================================
 * EXAMPLE TRACE: nums = [3, 6, 9, 12]
 * ===============================================================================
 * 
 * Initial: dp = [{}, {}, {}, {}], maxLen = 2
 * 
 * i=0, j=1: nums[0]=3, nums[1]=6
 * - diff = 6 - 3 = 3
 * - dp[0][3] doesn't exist, so dp[1][3] = 2
 * - dp = [{}, {3: 2}, {}, {}], maxLen = 2
 * 
 * i=0, j=2: nums[0]=3, nums[2]=9
 * - diff = 9 - 3 = 6
 * - dp[0][6] doesn't exist, so dp[2][6] = 2
 * - dp = [{}, {3: 2}, {6: 2}, {}], maxLen = 2
 * 
 * i=1, j=2: nums[1]=6, nums[2]=9
 * - diff = 9 - 6 = 3
 * - dp[1][3] = 2, so dp[2][3] = dp[1][3] + 1 = 3
 * - dp = [{}, {3: 2}, {6: 2, 3: 3}, {}], maxLen = 3
 * 
 * i=0, j=3: nums[0]=3, nums[3]=12
 * - diff = 12 - 3 = 9
 * - dp[0][9] doesn't exist, so dp[3][9] = 2
 * - dp = [{}, {3: 2}, {6: 2, 3: 3}, {9: 2}], maxLen = 3
 * 
 * i=1, j=3: nums[1]=6, nums[3]=12
 * - diff = 12 - 6 = 6
 * - dp[1][6] doesn't exist, so dp[3][6] = 2
 * - dp = [{}, {3: 2}, {6: 2, 3: 3}, {9: 2, 6: 2}], maxLen = 3
 * 
 * i=2, j=3: nums[2]=9, nums[3]=12
 * - diff = 12 - 9 = 3
 * - dp[2][3] = 3, so dp[3][3] = dp[2][3] + 1 = 4
 * - dp = [{}, {3: 2}, {6: 2, 3: 3}, {9: 2, 6: 2, 3: 4}], maxLen = 4
 * 
 * Result: 4 (sequence: [3, 6, 9, 12] with difference = 3)
 * 
 * ===============================================================================
 * EXAMPLE TRACE: nums = [9, 4, 7, 2, 10]
 * ===============================================================================
 * 
 * Let's trace some key pairs:
 * 
 * i=0, j=1: diff = 4-9 = -5, dp[1][-5] = 2
 * i=0, j=2: diff = 7-9 = -2, dp[2][-2] = 2
 * i=1, j=2: diff = 7-4 = 3, dp[2][3] = 2
 * i=1, j=3: diff = 2-4 = -2, dp[3][-2] = 2
 * i=2, j=3: diff = 2-7 = -5, dp[3][-5] = 2
 * 
 * Key insight: i=0,j=2 gives diff=-2, and i=1,j=3 also gives diff=-2
 * But they don't form a sequence: 9->7 (diff=-2), 4->2 (diff=-2)
 * However: i=2,j=4: diff = 10-7 = 3, dp[4][3] = dp[2][3] + 1 = 3
 * This gives sequence: [4, 7, 10] with difference = 3
 * 
 * Maximum length will be 3.
 */

/**
 * LONGEST ARITHMETIC SUBSEQUENCE
 * 
 * PROBLEM UNDERSTANDING:
 * ---------------------
 * Given an array of integers, find the length of the longest subsequence where
 * consecutive elements have the same difference (arithmetic progression).
 * 
 * Key Points:
 * - Subsequence: Can skip elements but maintain order
 * - Arithmetic: All consecutive differences are equal
 * - We need LENGTH, not the actual subsequence
 * 
 * INTERVIEW APPROACH - HOW TO SOLVE THIS:
 * ---------------------------------------
 * 
 * Step 1: UNDERSTAND THE PROBLEM
 * - Draw examples: [3,6,9,12] -> diff=3, length=4
 * - What makes it arithmetic? Same difference throughout
 * - Subsequence vs subarray? Can skip elements!
 * 
 * Step 2: THINK ABOUT BRUTE FORCE
 * - Try all possible subsequences? 2^n subsequences - too slow
 * - For each pair of elements, try to extend with same difference
 * 
 * Step 3: RECOGNIZE THE PATTERN
 * - This is a DYNAMIC PROGRAMMING problem
 * - Why? We have optimal substructure:
 *   * If we know longest arithmetic seq ending at position j with diff d,
 *   * We can extend it to position i if nums[i] - nums[j] == d
 * 
 * Step 4: DEFINE THE STATE
 * - dp[i][diff] = length of longest arithmetic subsequence ending at index i
 *                 with common difference 'diff'
 * - This captures: position + the difference that got us here
 * 
 * Step 5: STATE TRANSITION
 * - For each pair (j, i) where j < i:
 *   * Calculate diff = nums[i] - nums[j]
 *   * If there's already a sequence ending at j with this diff, extend it
 *   * Otherwise, start a new sequence of length 2
 * 
 * Step 6: IMPLEMENTATION CHOICE
 * - Can't use 2D array for diff (range too large: -500 to 500 for each pair)
 * - Use HashMap: Map<Integer, Integer> where key=diff, value=length
 * - Array of HashMaps: dp[i] = HashMap for position i
 */


class LongestArithmeticSubsequence {
    
    /**
     * APPROACH 1: DYNAMIC PROGRAMMING WITH HASHMAP
     * Time Complexity: O(n²)
     * Space Complexity: O(n²) - worst case all different differences
     * 
     * INTUITION:
     * - For each element, track all possible arithmetic sequences ending at it
     * - Key insight: A sequence is defined by its ending position and common difference
     * - dp[i].get(diff) = length of longest arithmetic seq ending at i with difference 'diff'
     * 
     * WHY THIS WORKS:
     * - When we process element i, we've already computed all sequences ending before it
     * - For each previous element j, we can form/extend a sequence with diff = nums[i] - nums[j]
     * - If j already had a sequence with this diff, we extend it
     * - Otherwise, we start a new 2-element sequence
     */
    public int longestArithSeqLength(int[] nums) {
        int n = nums.length;
        if (n <= 2) return n; // Base case: array of length 0,1,2 is arithmetic
        
        // dp[i] = HashMap where key=difference, value=length of sequence ending at i
        Map<Integer, Integer>[] dp = new HashMap[n];
        
        // Initialize each position with an empty map
        for (int i = 0; i < n; i++) {
            dp[i] = new HashMap<>();
        }
        
        int maxLength = 2; // Minimum length is 2 (any two elements form arithmetic seq)
        
        // For each position i
        for (int i = 1; i < n; i++) {
            // Look at all previous positions j
            for (int j = 0; j < i; j++) {
                // Calculate the difference between current and previous element
                int diff = nums[i] - nums[j];
                
                // How long is the arithmetic sequence ending at j with this diff?
                // If it exists, we extend it. Otherwise, start new sequence of length 2.
                int lengthAtJ = dp[j].getOrDefault(diff, 1);
                
                // Extend the sequence to position i
                int lengthAtI = lengthAtJ + 1;
                
                // Store the length for this difference at position i
                dp[i].put(diff, lengthAtI);
                
                // Update global maximum
                maxLength = Math.max(maxLength, lengthAtI);
            }
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 2: OPTIMIZED DP WITH 2D ARRAY (When value range is small)
     * Time Complexity: O(n²)
     * Space Complexity: O(n * maxDiff) where maxDiff is range of differences
     * 
     * WHEN TO USE:
     * - If the problem specifies a small range of values (e.g., 0 to 500)
     * - Array access is faster than HashMap operations
     * - Trade memory for speed
     * 
     * LIMITATION:
     * - Differences can range from (min-max) to (max-min)
     * - For general case, this could be huge (e.g., -10000 to 10000)
     * - Need to offset negative differences
     */
    public int longestArithSeqLength2DArray(int[] nums) {
        int n = nums.length;
        if (n <= 2) return n;
        
        // Find min and max to determine difference range
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int num : nums) {
            min = Math.min(min, num);
            max = Math.max(max, num);
        }
        
        // Difference range: (min - max) to (max - min)
        int diffRange = max - min;
        int offset = max; // To handle negative differences
        
        // dp[i][diff + offset] = length of arithmetic seq ending at i with difference diff
        int[][] dp = new int[n][2 * diffRange + 1];
        
        int maxLength = 2;
        
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                int diff = nums[i] - nums[j];
                int diffIndex = diff + offset;
                
                // If no sequence exists at j with this diff, start with length 2
                int lengthAtJ = dp[j][diffIndex] == 0 ? 1 : dp[j][diffIndex];
                int lengthAtI = lengthAtJ + 1;
                
                dp[i][diffIndex] = lengthAtI;
                maxLength = Math.max(maxLength, lengthAtI);
            }
        }
        
        return maxLength;
    }
    
    /**
     * APPROACH 3: BRUTE FORCE (For Understanding)
     * Time Complexity: O(n³)
     * Space Complexity: O(1)
     * 
     * PURPOSE:
     * - Educational: Shows the naive approach
     * - Helps understand the problem
     * - DO NOT use in interview unless asked for brute force first
     * 
     * IDEA:
     * - For each pair of starting elements
     * - Try to extend the sequence greedily
     * - Count the maximum length
     */
    public int longestArithSeqLengthBruteForce(int[] nums) {
        int n = nums.length;
        if (n <= 2) return n;
        
        int maxLength = 2;
        
        // Try every pair as starting point
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int diff = nums[j] - nums[i];
                int length = 2; // We have at least two elements
                int last = nums[j];
                
                // Try to extend this sequence
                for (int k = j + 1; k < n; k++) {
                    if (nums[k] - last == diff) {
                        length++;
                        last = nums[k];
                    }
                }
                
                maxLength = Math.max(maxLength, length);
            }
        }
        
        return maxLength;
    }
    
    /**
     * COMMON MISTAKES TO AVOID IN INTERVIEWS:
     * ----------------------------------------
     * 
     * 1. CONFUSING SUBARRAY vs SUBSEQUENCE
     *    - Subarray: contiguous elements
     *    - Subsequence: can skip elements (but maintain order)
     *    Example: [1,5,2,3] -> [1,2,3] is valid subsequence but not subarray
     * 
     * 2. FORGETTING EDGE CASES
     *    - Empty array: return 0
     *    - Single element: return 1
     *    - Two elements: always return 2 (any two form arithmetic seq)
     * 
     * 3. OFF-BY-ONE ERRORS
     *    - When initializing: any single element is arithmetic sequence of length 1
     *    - When extending: we add 1 to previous length
     * 
     * 4. NOT CONSIDERING NEGATIVE DIFFERENCES
     *    - [10, 7, 4, 1] has diff = -3
     *    - Your solution must handle negative differences
     * 
     * 5. HASH MAP INITIALIZATION
     *    - Using getOrDefault correctly
     *    - Initial value should be 1, not 0 (single element = length 1)
     * 
     * 6. INTEGER OVERFLOW
     *    - When computing differences, be aware of int limits
     *    - For this problem, constraints usually prevent overflow
     */
    
    /**
     * INTERVIEW COMMUNICATION TIPS:
     * -----------------------------
     * 
     * 1. START WITH EXAMPLES
     *    "Let me trace through example [20,1,15,3,10,5,8]:
     *     - [20,15,10,5] has diff=-5, length=4
     *     - [1,3,5] has diff=2, length=3"
     * 
     * 2. EXPLAIN YOUR THINKING
     *    "I notice this is a subsequence problem with optimal substructure.
     *     If I know the longest sequence ending at position j, I can extend it
     *     to position i if the difference matches."
     * 
     * 3. DISCUSS TRADEOFFS
     *    "I could use a 2D array if the value range is small, but HashMap
     *     is more general and handles any integer range."
     * 
     * 4. MENTION COMPLEXITY UPFRONT
     *    "This will be O(n²) time because we need to check all pairs.
     *     Space is O(n²) worst case for the HashMaps."
     * 
     * 5. TEST WITH EXAMPLES
     *    "Let me verify with [3,6,9,12]:
     *     - At index 1: diff=3 with index 0, length=2
     *     - At index 2: diff=3 with index 1, extend to length=3
     *     - At index 3: diff=3 with index 2, extend to length=4 ✓"
     */
    
    /**
     * FOLLOW-UP QUESTIONS YOU MIGHT GET:
     * ----------------------------------
     * 
     * Q1: Can you optimize space complexity?
     * A: The HashMap approach is already optimal for general case.
     *    We need to store all possible (position, difference) pairs.
     *    Can't reduce below O(n²) without losing information.
     * 
     * Q2: What if we need to return the actual subsequence, not just length?
     * A: Store parent pointers: Map<Integer, Integer> parent[i].get(diff) = j
     *    Then backtrack from the position with max length.
     * 
     * Q3: Can we do better than O(n²)?
     * A: No, we fundamentally need to check all pairs to find all possible
     *    differences. This is optimal for the general case.
     * 
     * Q4: What if array is sorted?
     * A: Still O(n²). Sorting doesn't help because subsequences can skip elements.
     * 
     * Q5: What about arithmetic progressions with more than one difference?
     * A: That would be a different problem. Current problem requires constant difference.
     */
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        LongestArithmeticSubsequence solution = new LongestArithmeticSubsequence();
        
        // Test Case 1: Complete arithmetic sequence
        int[] test1 = {3, 6, 9, 12};
        System.out.println("Test 1: " + Arrays.toString(test1));
        System.out.println("Expected: 4, Got: " + solution.longestArithSeqLength(test1));
        System.out.println("Explanation: Whole array [3,6,9,12] with diff=3\n");
        
        // Test Case 2: Mixed sequence
        int[] test2 = {9, 4, 7, 2, 10};
        System.out.println("Test 2: " + Arrays.toString(test2));
        System.out.println("Expected: 3, Got: " + solution.longestArithSeqLength(test2));
        System.out.println("Explanation: [4,7,10] with diff=3\n");
        
        // Test Case 3: Negative differences
        int[] test3 = {20, 1, 15, 3, 10, 5, 8};
        System.out.println("Test 3: " + Arrays.toString(test3));
        System.out.println("Expected: 4, Got: " + solution.longestArithSeqLength(test3));
        System.out.println("Explanation: [20,15,10,5] with diff=-5\n");
        
        // Test Case 4: All same elements
        int[] test4 = {5, 5, 5, 5};
        System.out.println("Test 4: " + Arrays.toString(test4));
        System.out.println("Expected: 4, Got: " + solution.longestArithSeqLength(test4));
        System.out.println("Explanation: All elements with diff=0\n");
        
        // Test Case 5: No long arithmetic sequence
        int[] test5 = {1, 7, 3, 5};
        System.out.println("Test 5: " + Arrays.toString(test5));
        System.out.println("Expected: 2, Got: " + solution.longestArithSeqLength(test5));
        System.out.println("Explanation: Any two elements form length 2\n");
        
        // Test Case 6: Edge case - two elements
        int[] test6 = {10, 20};
        System.out.println("Test 6: " + Arrays.toString(test6));
        System.out.println("Expected: 2, Got: " + solution.longestArithSeqLength(test6));
        System.out.println("Explanation: Two elements always form arithmetic seq\n");
        
        // Comparing all approaches
        System.out.println("=== COMPARING ALL APPROACHES ===");
        int[] testCompare = {20, 1, 15, 3, 10, 5, 8};
        System.out.println("Array: " + Arrays.toString(testCompare));
        System.out.println("HashMap approach: " + solution.longestArithSeqLength(testCompare));
        System.out.println("2D Array approach: " + solution.longestArithSeqLength2DArray(testCompare));
        System.out.println("Brute Force approach: " + solution.longestArithSeqLengthBruteForce(testCompare));
    }
}

/**
 * FINAL INTERVIEW CHECKLIST:
 * --------------------------
 * ✓ Understand the problem (subsequence, arithmetic, length)
 * ✓ Identify it as DP problem
 * ✓ Define state clearly: dp[i][diff]
 * ✓ Explain state transition logic
 * ✓ Handle edge cases
 * ✓ Discuss time/space complexity
 * ✓ Code cleanly with good variable names
 * ✓ Test with examples
 * ✓ Be ready for follow-up questions
 * 
 * TIME TO SOLVE: Aim for 20-25 minutes
 * - 5 min: Understanding + examples
 * - 10 min: Explaining approach + coding
 * - 5 min: Testing + discussing complexity
 * - 5 min: Follow-up questions
 */
