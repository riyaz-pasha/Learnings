import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * RUSSIAN DOLL ENVELOPES
 * 
 * PROBLEM UNDERSTANDING:
 * ---------------------
 * Given envelopes with [width, height], find the maximum number of envelopes
 * that can be nested inside each other (Russian doll style).
 * 
 * Rules:
 * - Envelope A fits into B if: A.width < B.width AND A.height < B.height
 * - Both dimensions must be strictly smaller (not equal)
 * - Cannot rotate envelopes
 * 
 * CRITICAL INSIGHT:
 * ----------------
 * This is a 2D version of the "Longest Increasing Subsequence" (LIS) problem!
 * 
 * The challenge: We have TWO dimensions (width and height) instead of one.
 * 
 * INTERVIEW APPROACH - HOW TO SOLVE THIS:
 * ---------------------------------------
 * 
 * Step 1: RECOGNIZE THE PATTERN
 * "I notice this is similar to finding the longest increasing subsequence,
 *  but with 2D constraints instead of 1D."
 * 
 * Step 2: KEY INSIGHT - REDUCE 2D TO 1D
 * "If I can fix one dimension (width), I only need to solve for the other (height)!"
 * 
 * How?
 * - Sort envelopes by width (ascending)
 * - For same width, sort by height (DESCENDING) - this is crucial!
 * - Then find LIS on heights
 * 
 * Step 3: WHY DESCENDING HEIGHT FOR SAME WIDTH?
 * This is the MOST IMPORTANT trick in this problem!
 * 
 * Example: envelopes = [[3,4], [3,5], [6,7]]
 * 
 * Wrong approach (sort both ascending):
 * After sort: [[3,4], [3,5], [6,7]]
 * Heights: [4, 5, 7] -> LIS = 3 (WRONG! Can't nest [3,4] and [3,5])
 * 
 * Correct approach (width asc, height desc for same width):
 * After sort: [[3,5], [3,4], [6,7]]
 * Heights: [5, 4, 7] -> LIS = 2 (CORRECT! [3,4] -> [6,7])
 * 
 * Why? When widths are same, we can't nest them. By sorting heights descending,
 * we prevent them from being in the same increasing subsequence!
 * 
 * Step 4: SOLVE LIS PROBLEM
 * Once sorted correctly, it's just finding LIS on the height values.
 * 
 * TWO APPROACHES FOR LIS:
 * 1. DP: O(n²) - easier to code, good for small n
 * 2. Binary Search + Greedy: O(n log n) - optimal, trickier
 */


class RussianDollEnvelopes {
    
    /**
     * APPROACH 1: OPTIMAL SOLUTION - SORTING + LIS WITH BINARY SEARCH
     * Time Complexity: O(n log n) - sorting O(n log n) + LIS O(n log n)
     * Space Complexity: O(n) - for the LIS array
     * 
     * THIS IS THE SOLUTION YOU SHOULD GIVE IN AN INTERVIEW
     * 
     * ALGORITHM:
     * 1. Sort envelopes by width (ascending), height (descending for same width)
     * 2. Extract heights into array
     * 3. Find LIS on heights using binary search
     * 
     * WHY BINARY SEARCH FOR LIS?
     * - We maintain an array 'lis' where lis[i] = smallest tail element 
     *   of all increasing subsequences of length i+1
     * - For each new height:
     *   * If larger than all elements: extend the longest subsequence
     *   * Otherwise: find the smallest element >= height and replace it
     *     (this potentially creates a better subsequence for future elements)
     */
    public int maxEnvelopes(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) {
            return 0;
        }
        
        int n = envelopes.length;
        
        // Step 1: Sort - CRITICAL PART!
        // Sort by width ascending, and by height descending when widths are equal
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) {
                return b[1] - a[1]; // Same width: sort height DESCENDING
            }
            return a[0] - b[0]; // Different width: sort width ASCENDING
        });
        
        // Step 2: Extract heights
        int[] heights = new int[n];
        for (int i = 0; i < n; i++) {
            heights[i] = envelopes[i][1];
        }
        
        // Step 3: Find LIS on heights
        return lengthOfLIS(heights);
    }
    
    /**
     * LONGEST INCREASING SUBSEQUENCE - BINARY SEARCH APPROACH
     * 
     * INTUITION:
     * Maintain array 'lis' where lis[i] = smallest ending element of all
     * increasing subsequences of length (i+1)
     * 
     * Example: [10, 9, 2, 5, 3, 7, 101, 18]
     * 
     * Process 10: lis = [10]
     * Process 9:  lis = [9]  (replace 10, better to have smaller tail)
     * Process 2:  lis = [2]  (replace 9)
     * Process 5:  lis = [2, 5]  (5 > 2, extend)
     * Process 3:  lis = [2, 3]  (replace 5, better tail for length 2)
     * Process 7:  lis = [2, 3, 7]  (extend)
     * Process 101: lis = [2, 3, 7, 101]  (extend)
     * Process 18: lis = [2, 3, 7, 18]  (replace 101)
     * 
     * Result: length = 4
     * 
     * WHY THIS WORKS:
     * - We want lis[i] to be as small as possible so future elements
     *   have better chance of extending the subsequence
     * - Binary search finds the position to update efficiently
     */
    private int lengthOfLIS(int[] nums) {
        int[] lis = new int[nums.length];
        int length = 0; // Current length of LIS
        
        for (int num : nums) {
            // Binary search for the position to insert/update
            // We want to find the leftmost element >= num
            int index = binarySearch(lis, 0, length, num);
            
            lis[index] = num;
            
            // If we inserted at the end, increase length
            if (index == length) {
                length++;
            }
        }
        
        return length;
    }
    
    /**
     * Binary search to find insertion position
     * Returns the index of the smallest element >= target
     * If all elements < target, returns length (insert at end)
     */
    private int binarySearch(int[] lis, int left, int right, int target) {
        while (left < right) {
            int mid = left + (right - left) / 2;
            if (lis[mid] < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }
        return left;
    }
    
    /**
     * APPROACH 2: DYNAMIC PROGRAMMING (O(n²))
     * Time Complexity: O(n²) - O(n log n) sort + O(n²) DP
     * Space Complexity: O(n)
     * 
     * WHEN TO USE:
     * - Small input size (n < 1000)
     * - Need to track actual envelopes in sequence (can extend to store path)
     * - Easier to understand and explain
     * 
     * ALGORITHM:
     * dp[i] = max number of envelopes ending at position i
     * For each envelope i, check all previous envelopes j:
     *   - If envelope j can fit into i, dp[i] = max(dp[i], dp[j] + 1)
     */
    public int maxEnvelopesDP(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) {
            return 0;
        }
        
        int n = envelopes.length;
        
        // Sort by width ascending, height descending for same width
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) {
                return b[1] - a[1];
            }
            return a[0] - b[0];
        });
        
        // dp[i] = max envelopes ending at i
        int[] dp = new int[n];
        Arrays.fill(dp, 1); // Each envelope is at least a sequence of 1
        
        int maxEnvelopes = 1;
        
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // Can envelope j fit into envelope i?
                // After our sort, we only need to check heights
                // (widths are already in order, with same widths having desc heights)
                if (envelopes[j][1] < envelopes[i][1]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxEnvelopes = Math.max(maxEnvelopes, dp[i]);
        }
        
        return maxEnvelopes;
    }
    
    /**
     * APPROACH 3: BRUTE FORCE WITH 2D DP (NO SORTING OPTIMIZATION)
     * Time Complexity: O(n²)
     * Space Complexity: O(n)
     * 
     * PURPOSE:
     * - Educational: Shows approach without the sorting trick
     * - Less efficient but more straightforward
     * - Good for understanding the base problem
     * 
     * LIMITATION:
     * - Doesn't leverage the sorting optimization
     * - Must check both width AND height for each pair
     */
    public int maxEnvelopesBruteForce(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) {
            return 0;
        }
        
        int n = envelopes.length;
        
        // Sort for consistency (but not using the height trick)
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) {
                return a[1] - b[1]; // Both ascending
            }
            return a[0] - b[0];
        });
        
        int[] dp = new int[n];
        Arrays.fill(dp, 1);
        
        int maxEnvelopes = 1;
        
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // Check BOTH dimensions
                if (envelopes[j][0] < envelopes[i][0] && 
                    envelopes[j][1] < envelopes[i][1]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxEnvelopes = Math.max(maxEnvelopes, dp[i]);
        }
        
        return maxEnvelopes;
    }
    
    /**
     * COMMON MISTAKES TO AVOID IN INTERVIEWS:
     * ----------------------------------------
     * 
     * 1. SORTING BOTH DIMENSIONS ASCENDING
     *    ❌ Arrays.sort(envelopes, (a,b) -> a[0] == b[0] ? a[1] - b[1] : a[0] - b[0])
     *    ✓ Arrays.sort(envelopes, (a,b) -> a[0] == b[0] ? b[1] - a[1] : a[0] - b[0])
     *    The height MUST be descending for same widths!
     * 
     * 2. ALLOWING EQUAL DIMENSIONS
     *    - Problem states BOTH must be strictly greater
     *    - [3,4] cannot fit into [3,5] or [4,4]
     * 
     * 3. FORGETTING EDGE CASES
     *    - Empty array: return 0
     *    - Single envelope: return 1
     *    - All same size: return 1
     * 
     * 4. NOT RECOGNIZING AS LIS PROBLEM
     *    - The key insight is reducing 2D to 1D via sorting
     *    - If you miss this, you'll struggle with the solution
     * 
     * 5. BINARY SEARCH IMPLEMENTATION ERRORS
     *    - Off-by-one errors in binary search
     *    - Wrong comparison (< vs <=)
     *    - Not handling the "insert at end" case
     * 
     * 6. THINKING YOU CAN ROTATE
     *    - Problem explicitly states no rotation
     *    - [3,4] is different from [4,3]
     */
    
    /**
     * INTERVIEW COMMUNICATION STRATEGY:
     * ---------------------------------
     * 
     * PHASE 1: PROBLEM UNDERSTANDING (2-3 min)
     * "Let me make sure I understand:
     *  - We have envelopes with width and height
     *  - One fits in another only if BOTH dimensions are strictly smaller
     *  - We want the maximum nesting depth
     *  - Cannot rotate envelopes
     *  
     *  This reminds me of the Longest Increasing Subsequence problem,
     *  but with 2D constraints instead of 1D."
     * 
     * PHASE 2: KEY INSIGHT (3-4 min)
     * "The key insight is to reduce this 2D problem to 1D:
     *  
     *  If I sort by width, I fix one dimension. Then I just need to find
     *  the longest increasing subsequence in the heights.
     *  
     *  But there's a catch: if two envelopes have the same width, they
     *  can't nest regardless of height. So I need to sort heights in
     *  DESCENDING order for same widths. This prevents them from being
     *  in the same increasing subsequence."
     * 
     * PHASE 3: SOLUTION APPROACH (1-2 min)
     * "My approach:
     *  1. Sort envelopes by width (asc), then height (desc for same width)
     *  2. Find LIS on the height values
     *  3. Use binary search for O(n log n) LIS algorithm
     *  
     *  Time: O(n log n), Space: O(n)"
     * 
     * PHASE 4: CODING (10-12 min)
     * - Write clean, well-commented code
     * - Explain as you go
     * - Handle edge cases upfront
     * 
     * PHASE 5: TESTING (3-4 min)
     * Walk through example: [[5,4],[6,4],[6,7],[2,3]]
     * 
     * After sort: [[2,3],[5,4],[6,7],[6,4]]
     * Heights: [3, 4, 7, 4]
     * LIS process:
     *   - 3: lis=[3]
     *   - 4: lis=[3,4]
     *   - 7: lis=[3,4,7]
     *   - 4: lis=[3,4,7] (4 replaces 7, but length stays 3)
     * Answer: 3 ✓
     */
    
    /**
     * VARIATIONS AND FOLLOW-UP QUESTIONS:
     * -----------------------------------
     * 
     * Q1: What if you need to return the actual sequence of envelopes?
     * A: Modify DP approach to store parent pointers:
     *    parent[i] = j means envelope i comes after envelope j
     *    Backtrack from the position with max length
     * 
     * Q2: What if envelopes could be rotated?
     * A: For each envelope, consider both orientations [w,h] and [h,w]
     *    Then solve as before (but be careful of duplicates)
     * 
     * Q3: What if we have 3D boxes instead of 2D envelopes?
     * A: Much harder! Can't reduce to LIS easily.
     *    Would need O(n²) DP checking all three dimensions.
     * 
     * Q4: Can we optimize space complexity?
     * A: The LIS array is already O(n) and necessary.
     *    Can't do better without losing information.
     * 
     * Q5: Why not just sort by area and find increasing sequence?
     * A: Counterexample: [3,8] has area 24, [4,5] has area 20
     *    [4,5] fits in [3,8]? No! Width 4 > 3.
     *    Area doesn't capture the 2D constraint correctly.
     */
    
    /**
     * TIPS FOR GETTING TO THE OPTIMAL SOLUTION:
     * ------------------------------------------
     * 
     * 1. START WITH BRUTE FORCE
     *    - "I could check every pair and build a graph..."
     *    - "Then find longest path... that's complex"
     * 
     * 2. RECOGNIZE THE PATTERN
     *    - "Wait, this feels like LIS but with extra constraints"
     *    - "How can I leverage what I know about LIS?"
     * 
     * 3. THINK ABOUT SORTING
     *    - "If I fix one dimension, I reduce the problem"
     *    - "Sort by width... then what about height?"
     * 
     * 4. THE KEY TRICK (This is where most people get stuck!)
     *    - "Same width envelopes can't nest"
     *    - "How do I prevent them being in the same sequence?"
     *    - "Aha! Sort heights descending for same width!"
     * 
     * 5. OPTIMIZE THE LIS
     *    - "Standard LIS is O(n²) with DP"
     *    - "Can use binary search to get O(n log n)"
     * 
     * If you can't remember the sorting trick, it's okay to:
     * - Implement the O(n²) DP solution first (still correct!)
     * - Mention the O(n log n) optimization exists
     * - Show you understand the tradeoffs
     */
    
    // ==================== HELPER: LIS WITH PATH RECONSTRUCTION ====================
    
    /**
     * Bonus: Return actual envelope sequence, not just count
     */
    public List<int[]> maxEnvelopesWithPath(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) {
            return new ArrayList<>();
        }
        
        int n = envelopes.length;
        
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) return b[1] - a[1];
            return a[0] - b[0];
        });
        
        int[] dp = new int[n];
        int[] parent = new int[n];
        Arrays.fill(dp, 1);
        Arrays.fill(parent, -1);
        
        int maxLength = 1;
        int maxIndex = 0;
        
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (envelopes[j][1] < envelopes[i][1]) {
                    if (dp[j] + 1 > dp[i]) {
                        dp[i] = dp[j] + 1;
                        parent[i] = j;
                    }
                }
            }
            if (dp[i] > maxLength) {
                maxLength = dp[i];
                maxIndex = i;
            }
        }
        
        // Reconstruct path
        List<int[]> result = new ArrayList<>();
        int curr = maxIndex;
        while (curr != -1) {
            result.add(envelopes[curr]);
            curr = parent[curr];
        }
        Collections.reverse(result);
        
        return result;
    }
    
    // ==================== TEST CASES ====================
    
    public static void main(String[] args) {
        RussianDollEnvelopes solution = new RussianDollEnvelopes();
        
        // Test Case 1: Standard case
        int[][] test1 = {{5,4},{6,4},{6,7},{2,3}};
        System.out.println("Test 1: [[5,4],[6,4],[6,7],[2,3]]");
        System.out.println("Expected: 3");
        System.out.println("Binary Search: " + solution.maxEnvelopes(test1));
        System.out.println("DP: " + solution.maxEnvelopesDP(test1));
        System.out.println("Explanation: [2,3] -> [5,4] -> [6,7]\n");
        
        // Test Case 2: All same size
        int[][] test2 = {{1,1},{1,1},{1,1}};
        System.out.println("Test 2: [[1,1],[1,1],[1,1]]");
        System.out.println("Expected: 1");
        System.out.println("Binary Search: " + solution.maxEnvelopes(test2));
        System.out.println("Explanation: Cannot nest any\n");
        
        // Test Case 3: Already sorted
        int[][] test3 = {{1,2},{2,3},{3,4},{4,5}};
        System.out.println("Test 3: [[1,2],[2,3],[3,4],[4,5]]");
        System.out.println("Expected: 4");
        System.out.println("Binary Search: " + solution.maxEnvelopes(test3));
        System.out.println("Explanation: All can nest\n");
        
        // Test Case 4: Same width, different heights
        int[][] test4 = {{1,3},{1,4},{1,5},{1,6}};
        System.out.println("Test 4: [[1,3],[1,4],[1,5],[1,6]]");
        System.out.println("Expected: 1");
        System.out.println("Binary Search: " + solution.maxEnvelopes(test4));
        System.out.println("Explanation: Same width, cannot nest\n");
        
        // Test Case 5: Decreasing sequence
        int[][] test5 = {{5,5},{4,4},{3,3},{2,2},{1,1}};
        System.out.println("Test 5: [[5,5],[4,4],[3,3],[2,2],[1,1]]");
        System.out.println("Expected: 1");
        System.out.println("Binary Search: " + solution.maxEnvelopes(test5));
        System.out.println("Explanation: Decreasing, cannot nest forward\n");
        
        // Test Case 6: Complex case demonstrating sorting importance
        int[][] test6 = {{4,5},{4,6},{6,7},{2,3},{1,1}};
        System.out.println("Test 6: [[4,5],[4,6],[6,7],[2,3],[1,1]]");
        System.out.println("Expected: 4");
        System.out.println("Binary Search: " + solution.maxEnvelopes(test6));
        System.out.println("Explanation: [1,1] -> [2,3] -> [4,5] -> [6,7]");
        System.out.println("Note: [4,6] is excluded due to same width as [4,5]\n");
        
        // Test with path reconstruction
        System.out.println("=== PATH RECONSTRUCTION ===");
        List<int[]> path = solution.maxEnvelopesWithPath(test1);
        System.out.print("Envelope sequence for test1: ");
        for (int[] env : path) {
            System.out.print("[" + env[0] + "," + env[1] + "] ");
        }
        System.out.println("\n");
        
        // Comparing approaches
        System.out.println("=== PERFORMANCE COMPARISON ===");
        int[][] largTest = new int[1000][2];
        Random rand = new Random(42);
        for (int i = 0; i < 1000; i++) {
            largTest[i][0] = rand.nextInt(1000);
            largTest[i][1] = rand.nextInt(1000);
        }
        
        long start = System.nanoTime();
        int result1 = solution.maxEnvelopes(largTest);
        long time1 = System.nanoTime() - start;
        
        start = System.nanoTime();
        int result2 = solution.maxEnvelopesDP(largTest);
        long time2 = System.nanoTime() - start;
        
        System.out.println("Array size: 1000");
        System.out.println("Binary Search O(n log n): " + result1 + " (" + time1/1_000_000.0 + " ms)");
        System.out.println("DP O(n²): " + result2 + " (" + time2/1_000_000.0 + " ms)");
        System.out.println("Speedup: " + String.format("%.2fx", (double)time2/time1));
    }
}

/**
 * FINAL INTERVIEW CHECKLIST:
 * --------------------------
 * ✓ Recognize as 2D LIS problem
 * ✓ Explain sorting strategy (width asc, height desc for ties)
 * ✓ Explain WHY descending height for same width
 * ✓ Implement LIS correctly (binary search or DP)
 * ✓ Handle edge cases (empty, single, all same)
 * ✓ Test with provided examples
 * ✓ Discuss time/space complexity
 * ✓ Be ready for follow-ups (path reconstruction, rotations, 3D)
 * 
 * TIME TO SOLVE: Aim for 25-30 minutes
 * - 3 min: Understanding problem
 * - 4 min: Recognizing pattern and key insight
 * - 2 min: Explaining approach
 * - 12 min: Coding
 * - 4 min: Testing
 * - 5 min: Follow-up discussion
 * 
 * DIFFICULTY: Hard (but becomes Medium if you know the sorting trick!)
 * 
 * KEY TAKEAWAY:
 * The sorting trick (descending height for same width) is the KEY insight
 * that makes this problem solvable. Without it, you'll struggle to reduce
 * the 2D problem to 1D. This is a great example of how clever preprocessing
 * (sorting) can simplify a complex problem!
 */

class RussianDollEnvelopes2 {

    // Approach 1: Direct 2D DP - O(n^2) time, O(n) space
    public int maxEnvelopes(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) {
            return 0;
        }

        int n = envelopes.length;

        // Sort by width ascending, and if widths are equal, sort by height ascending
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) {
                return a[1] - b[1]; // height ascending when widths are equal
            }
            return a[0] - b[0]; // width ascending
        });

        // dp[i] represents the maximum number of envelopes ending at index i
        int[] dp = new int[n];
        Arrays.fill(dp, 1); // Each envelope can form a sequence of length 1

        int maxCount = 1;

        // For each envelope, check all previous envelopes
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // Check if envelope j can fit into envelope i
                if (envelopes[j][0] < envelopes[i][0] && envelopes[j][1] < envelopes[i][1]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxCount = Math.max(maxCount, dp[i]);
        }

        return maxCount;
    }

    // Approach 2: DP with LIS optimization - O(n log n) time
    public int maxEnvelopesOptimized(int[][] envelopes) {
        if (envelopes == null || envelopes.length == 0) {
            return 0;
        }

        // Sort by width ascending, and if widths are equal, sort by height descending
        Arrays.sort(envelopes, (a, b) -> {
            if (a[0] == b[0]) {
                return b[1] - a[1]; // height descending when widths are equal
            }
            return a[0] - b[0]; // width ascending
        });

        // Extract heights and find LIS using DP with binary search
        int[] heights = new int[envelopes.length];
        for (int i = 0; i < envelopes.length; i++) {
            heights[i] = envelopes[i][1];
        }

        return lengthOfLIS(heights);
    }

    // DP-based LIS with binary search - O(n log n)
    private int lengthOfLIS(int[] nums) {
        List<Integer> tails = new ArrayList<>();

        for (int num : nums) {
            int pos = binarySearch(tails, num);
            if (pos == tails.size()) {
                tails.add(num);
            } else {
                tails.set(pos, num);
            }
        }

        return tails.size();
    }

    // Binary search to find the position where num should be inserted
    private int binarySearch(List<Integer> tails, int target) {
        int left = 0, right = tails.size();

        while (left < right) {
            int mid = left + (right - left) / 2;
            if (tails.get(mid) < target) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

}

// Step 1: Sort the envelopes
// Sort by width in ascending order.
// If two envelopes have the same width, sort by height in descending order.
// This prevents envelopes with the same width from being incorrectly nested by height.

// Example Input: [[5,4], [6,4], [6,7], [2,3]]
// After sorting: [[2,3], [5,4], [6,7], [6,4]]
// Notice that for width 6, (6,7) comes before (6,4) because of height descending.

// Step 2: Extract the heights to apply Longest Increasing Subsequence (LIS) on them.
// heights = [3, 4, 7, 4]

// Step 3: Initialize an array 'lis' to store the end elements of potential increasing subsequences.
// Initialize length = 0

// Iterate through each height:
// 1. height = 3
//    - lis is empty. Place 3 at index 0.
//    - lis = [3], length = 1

// 2. height = 4
//    - 4 > last element in lis (3). Append 4.
//    - lis = [3, 4], length = 2

// 3. height = 7
//    - 7 > last element in lis (4). Append 7.
//    - lis = [3, 4, 7], length = 3

// 4. height = 4
//    - 4 is not greater than last element in lis (7).
//    - Binary search gives index 1 (4 replaces existing 4 to keep LIS minimal).
//    - lis remains [3, 4, 7], length = 3

// Final length of LIS is 3 → Maximum envelopes that can be Russian dolled = 3
