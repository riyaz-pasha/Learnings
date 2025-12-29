/**
 * MAXIMUM POINTS YOU CAN OBTAIN FROM CARDS - COMPREHENSIVE ANALYSIS
 * ALL APPROACHES WITH DETAILED EXPLANATIONS
 * 
 * PROBLEM UNDERSTANDING:
 * - We have cards in a row with associated points
 * - We must take exactly k cards
 * - We can only take from the beginning OR end of the row (not middle)
 * - Goal: Maximize the sum of points from selected cards
 * 
 * KEY INSIGHT - THE SLIDING WINDOW TRICK:
 * Instead of thinking about which k cards to take, think about which (n-k) cards to LEAVE.
 * 
 * Why? Because:
 * - If we take k cards from ends, we leave (n-k) consecutive cards in the middle
 * - The cards we leave form a contiguous subarray
 * - To MAXIMIZE our score, we need to MINIMIZE the sum of cards we leave
 * - This transforms the problem into: Find minimum sum subarray of size (n-k)
 * 
 * EXAMPLE WALKTHROUGH:
 * cardPoints = [1,2,3,4,5,6,1], k = 3
 * - Total cards: 7, must take 3, so leave 4
 * - Possible 4-card windows to leave:
 *   [1,2,3,4] -> sum = 10, remaining = 5+6+1 = 12
 *   [2,3,4,5] -> sum = 14, remaining = 1+6+1 = 8
 *   [3,4,5,6] -> sum = 18, remaining = 1+2+1 = 4
 *   [4,5,6,1] -> sum = 16, remaining = 1+2+3 = 6
 * - Minimum window sum is 10, so maximum score is 12
 * 
 * TIME COMPLEXITY: O(n) where n = cardPoints.length
 * SPACE COMPLEXITY: O(1)
 */

class MaximumPointsFromCards {
    
    /**
     * APPROACH 1: SLIDING WINDOW - MINIMIZE MIDDLE CARDS (INVERSE THINKING)
     * 
     * ALGORITHM:
     * 1. Calculate total sum of all cards
     * 2. If k == n, return total sum (take all cards)
     * 3. Find the minimum sum of any subarray of size (n-k)
     * 4. Return totalSum - minSubarraySum
     * 
     * DETAILED REASONING:
     * - We slide a window of size (n-k) across the array
     * - Track the minimum sum window encountered
     * - The answer is: total - minimum_window
     * 
     * EDGE CASES:
     * - k == n: Must take all cards, return sum of all
     * - k == 1: Either take first or last card, whichever is larger
     * - All cards same value: Any combination gives same result
     * 
     * TIME: O(n), SPACE: O(1)
     */
    public int maxScore_MinimizeMiddle(int[] cardPoints, int k) {
        int n = cardPoints.length;
        
        // Calculate total sum of all cards
        int totalSum = 0;
        for (int point : cardPoints) {
            totalSum += point;
        }
        
        // Edge case: if we take all cards, return total sum
        if (k == n) {
            return totalSum;
        }
        
        // We need to find minimum sum subarray of size (n-k)
        int windowSize = n - k;
        
        // Initialize first window
        int currentWindowSum = 0;
        for (int i = 0; i < windowSize; i++) {
            currentWindowSum += cardPoints[i];
        }
        
        // The first window sum is our initial minimum
        int minWindowSum = currentWindowSum;
        
        // Slide the window across the array
        // For each new position, remove leftmost element and add new rightmost element
        for (int i = windowSize; i < n; i++) {
            // Slide window: remove element going out, add element coming in
            currentWindowSum += cardPoints[i] - cardPoints[i - windowSize];
            
            // Update minimum if current window is smaller
            minWindowSum = Math.min(minWindowSum, currentWindowSum);
        }
        
        // Maximum points we can get = Total - Minimum we leave behind
        return totalSum - minWindowSum;
    }
    
    /**
     * APPROACH 2: DIRECT SLIDING WINDOW - THE FORGOTTEN VERSION!
     * 
     * THIS IS THE MOST ELEGANT SOLUTION - DIRECTLY WORK WITH K CARDS
     * 
     * ALGORITHM:
     * 1. Start by taking all k cards from the RIGHT end
     * 2. Calculate initial sum
     * 3. Slide the window: add one from LEFT, remove one from RIGHT
     * 4. Track maximum sum during sliding
     * 
     * VISUALIZATION for [1,2,3,4,5,6,1], k=3:
     * Initial: Take [5,6,1] from right -> sum = 12
     * Step 1:  Take [1] from left, drop [5] -> [1,6,1] -> sum = 8
     * Step 2:  Take [1,2] from left, drop [5,6] -> [1,2,1] -> sum = 4
     * Step 3:  Take [1,2,3] from left, drop all right -> [1,2,3] -> sum = 6
     * Maximum: 12
     * 
     * WHY THIS IS BRILLIANT:
     * - Works directly with the k cards we're taking (not the inverse)
     * - Only k iterations, not n iterations
     * - Extremely clean code
     * - Most intuitive once you see it
     * 
     * TIME: O(k), SPACE: O(1)
     * 
     * THIS IS THE BEST APPROACH FOR INTERVIEWS!
     */
    public int maxScore(int[] cardPoints, int k) {
        int n = cardPoints.length;
        int sum = 0;

        // Step 1: Take k cards from the right
        // Start from index (n-k) and go to end
        for (int i = n - k; i < n; i++) {
            sum += cardPoints[i];
        }

        int max = sum;

        // Step 2: Slide window - take from left, drop from right
        // In iteration i:
        //   - Add cardPoints[i] (from left)
        //   - Remove cardPoints[n-k+i] (from right)
        for (int i = 0; i < k; i++) {
            sum += cardPoints[i] - cardPoints[n - k + i];
            max = Math.max(max, sum);
        }

        return max;
    }
    
    /**
     * APPROACH 3: TWO POINTERS WITH EXPLICIT TRACKING
     * 
     * CONCEPT:
     * - Use two pointers: left and right
     * - Track exactly which cards we're taking
     * - More explicit about the sliding process
     * 
     * ALGORITHM:
     * 1. Start with all k from right (right pointer at end)
     * 2. Move left pointer forward, right pointer backward
     * 3. Track maximum
     * 
     * TIME: O(k), SPACE: O(1)
     */
    public int maxScore_TwoPointers(int[] cardPoints, int k) {
        int n = cardPoints.length;
        int sum = 0;
        
        // Take all k cards from right initially
        int right = n - 1;
        for (int i = 0; i < k; i++) {
            sum += cardPoints[right];
            right--;
        }
        
        int max = sum;
        int left = 0;
        right = n - k; // Reset right to point to leftmost of right-taken cards
        
        // Slide: take from left, drop from right
        for (int i = 0; i < k; i++) {
            sum += cardPoints[left] - cardPoints[right];
            left++;
            right++;
            max = Math.max(max, sum);
        }
        
        return max;
    }
    
    /**
     * APPROACH 4: PREFIX + SUFFIX SUM ARRAYS
     * 
     * CONCEPT:
     * - Pre-compute prefix sums and suffix sums
     * - Try all combinations: i from left + (k-i) from right
     * - Use arrays for O(1) range sum queries
     * 
     * TRADE-OFF:
     * - Uses O(n) space for clarity
     * - Makes the logic very explicit
     * - Good for understanding the problem structure
     * 
     * TIME: O(n), SPACE: O(n)
     */
    public int maxScore_PrefixSuffix(int[] cardPoints, int k) {
        int n = cardPoints.length;
        
        // Build prefix sum array
        // prefix[i] = sum of first i cards
        int[] prefix = new int[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + cardPoints[i];
        }
        
        // Build suffix sum array
        // suffix[i] = sum of last i cards
        int[] suffix = new int[n + 1];
        for (int i = n - 1; i >= 0; i--) {
            suffix[n - i] = suffix[n - i - 1] + cardPoints[i];
        }
        
        int max = 0;
        
        // Try all combinations: i from left, (k-i) from right
        for (int i = 0; i <= k; i++) {
            int leftSum = prefix[i];           // Sum of first i cards
            int rightSum = suffix[k - i];      // Sum of last (k-i) cards
            max = Math.max(max, leftSum + rightSum);
        }
        
        return max;
    }
    
    /**
     * APPROACH 5: DYNAMIC PROGRAMMING (OVERKILL BUT EDUCATIONAL)
     * 
     * CONCEPT:
     * - dp[i][j] = max score taking i cards from left, j cards from right
     * - Constraint: i + j <= k
     * - Final answer: max(dp[i][k-i]) for all valid i
     * 
     * RECURRENCE:
     * dp[i][j] = max(dp[i-1][j] + cardPoints[i-1],    // take from left
     *                dp[i][j-1] + cardPoints[n-j])     // take from right
     * 
     * WHY OVERKILL:
     * - The problem has optimal substructure but doesn't need DP
     * - Sliding window is much simpler and more efficient
     * - Good to mention in interview to show you considered it
     * 
     * TIME: O(k²), SPACE: O(k²)
     */
    public int maxScore_DP(int[] cardPoints, int k) {
        int n = cardPoints.length;
        
        // dp[i][j] represents max score with i cards from left, j from right
        int[][] dp = new int[k + 1][k + 1];
        
        // Fill the DP table
        for (int i = 0; i <= k; i++) {
            for (int j = 0; j <= k - i; j++) {
                if (i == 0 && j == 0) {
                    dp[i][j] = 0;
                } else if (i == 0) {
                    // Only taking from right
                    dp[i][j] = dp[i][j - 1] + cardPoints[n - j];
                } else if (j == 0) {
                    // Only taking from left
                    dp[i][j] = dp[i - 1][j] + cardPoints[i - 1];
                } else {
                    // Can take from either side
                    int takeLeft = dp[i - 1][j] + cardPoints[i - 1];
                    int takeRight = dp[i][j - 1] + cardPoints[n - j];
                    dp[i][j] = Math.max(takeLeft, takeRight);
                }
            }
        }
        
        // Find maximum among all valid combinations summing to k
        int max = 0;
        for (int i = 0; i <= k; i++) {
            max = Math.max(max, dp[i][k - i]);
        }
        
        return max;
    }
    
    /**
     * APPROACH 6: RECURSION WITH MEMOIZATION
     * 
     * CONCEPT:
     * - Recursive approach: at each step choose left or right
     * - State: (left_index, right_index, remaining_picks)
     * - Memoize to avoid recomputation
     * 
     * RECURRENCE:
     * solve(left, right, remaining) = 
     *   max(cardPoints[left] + solve(left+1, right, remaining-1),
     *       cardPoints[right] + solve(left, right-1, remaining-1))
     * 
     * TIME: O(k²), SPACE: O(k²) for memoization
     */
    public int maxScore_Recursion(int[] cardPoints, int k) {
        int n = cardPoints.length;
        // memo[left][remaining] = max score with left pointer at 'left', 'remaining' picks left
        Integer[][] memo = new Integer[n][k + 1];
        return solve(cardPoints, 0, n - 1, k, memo);
    }
    
    private int solve(int[] cards, int left, int right, int remaining, Integer[][] memo) {
        // Base case: no more picks
        if (remaining == 0) {
            return 0;
        }
        
        // Check memo
        if (memo[left][remaining] != null) {
            return memo[left][remaining];
        }
        
        // Choose left card
        int pickLeft = cards[left] + solve(cards, left + 1, right, remaining - 1, memo);
        
        // Choose right card
        int pickRight = cards[right] + solve(cards, left, right - 1, remaining - 1, memo);
        
        // Store and return maximum
        memo[left][remaining] = Math.max(pickLeft, pickRight);
        return memo[left][remaining];
    }
    
    /**
     * APPROACH 7: BRUTE FORCE ENUMERATION
     * 
     * CONCEPT:
     * - Try all possible combinations explicitly
     * - For each valid split (i left, k-i right), calculate sum
     * 
     * TIME: O(k), SPACE: O(1)
     * 
     * GOOD FOR:
     * - Initial understanding
     * - Small k values
     * - Verification of other approaches
     */
    public int maxScore_BruteForce(int[] cardPoints, int k) {
        int n = cardPoints.length;
        int maxScore = 0;
        
        // Try all combinations: i cards from left, (k-i) from right
        for (int i = 0; i <= k; i++) {
            int score = 0;
            
            // Take i cards from left
            for (int j = 0; j < i; j++) {
                score += cardPoints[j];
            }
            
            // Take (k-i) cards from right
            for (int j = 0; j < k - i; j++) {
                score += cardPoints[n - 1 - j];
            }
            
            maxScore = Math.max(maxScore, score);
        }
        
        return maxScore;
    }
    
    /**
     * APPROACH 8: DEQUE-BASED SIMULATION
     * 
     * CONCEPT:
     * - Use a deque to simulate the card picking process
     * - Try all possible sequences of picks
     * - More of a conceptual/educational approach
     * 
     * TIME: O(k), SPACE: O(n) for deque
     */
    public int maxScore_Deque(int[] cardPoints, int k) {
        int n = cardPoints.length;
        
        // Try taking all k from right first
        int sum = 0;
        for (int i = n - k; i < n; i++) {
            sum += cardPoints[i];
        }
        
        int max = sum;
        
        // Now simulate: replace rightmost with leftmost, one at a time
        for (int i = 0; i < k; i++) {
            sum = sum - cardPoints[n - k + i] + cardPoints[i];
            max = Math.max(max, sum);
        }
        
        return max;
    }
    
    /**
     * TEST CASES WITH DETAILED EXPLANATIONS
     */
    public static void main(String[] args) {
        MaximumPointsFromCards solution = new MaximumPointsFromCards();
        
        System.out.println("=== TEST CASE 1: Basic Example ===");
        int[] cards1 = {1, 2, 3, 4, 5, 6, 1};
        int k1 = 3;
        System.out.println("Input: [1,2,3,4,5,6,1], k=3");
        System.out.println("Approach 1 (Minimize Middle): " + solution.maxScore_MinimizeMiddle(cards1, k1));
        System.out.println("Approach 2 (Direct Slide): " + solution.maxScore(cards1, k1));
        System.out.println("Approach 3 (Two Pointers): " + solution.maxScore_TwoPointers(cards1, k1));
        System.out.println("Approach 4 (Prefix/Suffix): " + solution.maxScore_PrefixSuffix(cards1, k1));
        System.out.println("Approach 5 (DP): " + solution.maxScore_DP(cards1, k1));
        System.out.println("Approach 6 (Recursion): " + solution.maxScore_Recursion(cards1, k1));
        System.out.println("Approach 7 (Brute Force): " + solution.maxScore_BruteForce(cards1, k1));
        System.out.println("Approach 8 (Deque): " + solution.maxScore_Deque(cards1, k1));
        System.out.println("Expected: 12 (take [5,6,1] from right)\n");
        
        System.out.println("=== TEST CASE 2: All Same Values ===");
        int[] cards2 = {2, 2, 2};
        int k2 = 2;
        System.out.println("Input: [2,2,2], k=2");
        System.out.println("Direct Slide: " + solution.maxScore(cards2, k2));
        System.out.println("Expected: 4 (any 2 cards)\n");
        
        System.out.println("=== TEST CASE 3: Take All Cards ===");
        int[] cards3 = {9, 7, 7, 9, 7, 7, 9};
        int k3 = 7;
        System.out.println("Input: [9,7,7,9,7,7,9], k=7");
        System.out.println("Direct Slide: " + solution.maxScore(cards3, k3));
        System.out.println("Expected: 55 (sum of all)\n");
        
        System.out.println("=== TEST CASE 4: Mixed Strategy ===");
        int[] cards4 = {100, 40, 17, 9, 73, 75};
        int k4 = 3;
        System.out.println("Input: [100,40,17,9,73,75], k=3");
        System.out.println("Direct Slide: " + solution.maxScore(cards4, k4));
        System.out.println("Expected: 248 (100 from left, 73+75 from right)\n");
        
        System.out.println("=== TEST CASE 5: Large Values at Ends ===");
        int[] cards5 = {1, 79, 80, 1, 1, 1, 200, 1};
        int k5 = 3;
        System.out.println("Input: [1,79,80,1,1,1,200,1], k=3");
        System.out.println("Direct Slide: " + solution.maxScore(cards5, k5));
        System.out.println("Expected: 202 (200+1+1 from right)\n");
        
        // Performance comparison for different approaches
        System.out.println("=== PERFORMANCE COMPARISON ===");
        int[] largeDeck = new int[100000];
        for (int i = 0; i < largeDeck.length; i++) {
            largeDeck[i] = (int)(Math.random() * 100);
        }
        int kLarge = 50000;
        
        long start, end;
        
        start = System.nanoTime();
        solution.maxScore_MinimizeMiddle(largeDeck, kLarge);
        end = System.nanoTime();
        System.out.println("Minimize Middle: " + (end - start) / 1000000.0 + " ms");
        
        start = System.nanoTime();
        solution.maxScore(largeDeck, kLarge);
        end = System.nanoTime();
        System.out.println("Direct Slide: " + (end - start) / 1000000.0 + " ms");
        
        start = System.nanoTime();
        solution.maxScore_PrefixSuffix(largeDeck, kLarge);
        end = System.nanoTime();
        System.out.println("Prefix/Suffix: " + (end - start) / 1000000.0 + " ms");
    }
}

/**
 * ============================================================================
 * COMPREHENSIVE COMPARISON OF ALL APPROACHES
 * ============================================================================
 * 
 * BEST FOR INTERVIEWS (in order):
 * 
 * 1. APPROACH 2 - DIRECT SLIDING WINDOW (maxScore)
 *    ✓ Most elegant and intuitive
 *    ✓ Works directly with k cards (not inverse)
 *    ✓ Only O(k) time complexity
 *    ✓ Clean, short code
 *    ✓ THIS IS THE FORGOTTEN VERSION - MEMORIZE THIS!
 * 
 * 2. APPROACH 1 - MINIMIZE MIDDLE (maxScore_MinimizeMiddle)
 *    ✓ Shows problem transformation skill
 *    ✓ Good for demonstrating inverse thinking
 *    ✓ O(n) time but very clean
 * 
 * 3. APPROACH 4 - PREFIX/SUFFIX (maxScore_PrefixSuffix)
 *    ✓ Very clear logic
 *    ✓ Easy to understand
 *    ✓ Good for explaining to others
 * 
 * EDUCATIONAL VALUE:
 * 
 * - APPROACH 5 (DP): Shows you understand DP but recognize it's overkill
 * - APPROACH 6 (Recursion): Good for recursive thinking practice
 * - APPROACH 7 (Brute Force): Always good to mention as baseline
 * 
 * PRACTICAL CONSIDERATIONS:
 * 
 * When k << n:
 *   - Approach 2 (Direct Slide) is BEST - O(k) beats O(n)
 *   - Approach 3 (Two Pointers) is also excellent
 * 
 * When k ≈ n:
 *   - All approaches perform similarly
 *   - Choose the one you can code fastest
 * 
 * Space-constrained environments:
 *   - Avoid Approach 4 (uses O(n) space)
 *   - Avoid Approach 5 & 6 (use O(k²) space)
 * 
 * CODE INTERVIEW STRATEGY:
 * 
 * 1. Start with brute force explanation (30 seconds)
 * 2. Explain the sliding window insight (1 minute)
 * 3. Code Approach 2 (Direct Slide) - 2-3 minutes
 * 4. Walk through example (1 minute)
 * 5. Analyze time/space complexity (30 seconds)
 * 6. Mention Approach 1 as alternative if time permits
 * 
 * COMMON INTERVIEW FOLLOW-UPS:
 * 
 * Q: "What if cards can have negative values?"
 * A: Algorithm still works! We want max sum of k cards regardless of sign.
 * 
 * Q: "What if we can take cards from middle too?"
 * A: Then it becomes a different problem - likely need DP or greedy with sorting.
 * 
 * Q: "Can you do better than O(k) time?"
 * A: No, we must examine at least k cards to know which to take.
 * 
 * Q: "What if k can be very large?"
 * A: If k > n/2, consider inverse: take (n-k) cards to leave, maximize what's left.
 * 
 * KEY INSIGHTS TO MENTION:
 * 
 * 1. Constraint recognition: "Can only take from ends"
 * 2. Problem transformation: "Taking k = leaving (n-k)"
 * 3. Sliding window pattern: "Contiguous subarray of size (n-k)"
 * 4. Trade-off awareness: "Direct slide O(k) vs minimize middle O(n)"
 * 
 * DEBUGGING TIPS:
 * 
 * - Check edge cases: k=0, k=1, k=n
 * - Verify indices: easy to get off-by-one errors
 * - Test with: all positive, all negative, mixed values
 * - Check: single element, two elements arrays
 * 
 * RELATED PROBLEMS:
 * 
 * - Maximum Subarray (Kadane's algorithm)
 * - Minimum Size Subarray Sum
 * - Longest Substring with K Distinct Characters
 * - Sliding Window Maximum
 * 
 * ============================================================================
 */
