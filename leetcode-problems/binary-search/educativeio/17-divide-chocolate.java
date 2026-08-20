import java.util.*;

/**
 * =====================================================================
 * 🍫 DIVIDE CHOCOLATE — MASTER REVISION TEMPLATE
 * =====================================================================
 *
 * 🧠 PROBLEM SUMMARY:
 * -------------------
 * We have an array of sweetness values.
 * We must make k cuts → k+1 pieces.
 *
 * We will take the piece with MINIMUM sweetness.
 * Our goal: MAXIMIZE this minimum sweetness.
 *
 * ---------------------------------------------------------------
 * 🎯 CORE IDEA (VERY IMPORTANT — INTERVIEW GOLD)
 * ---------------------------------------------------------------
 *
 * This is a:
 *
 *      👉 MAXIMIZE THE MINIMUM problem
 *
 * Whenever you see:
 *   - maximize minimum
 *   - minimize maximum
 *
 * 👉 Think: "Binary Search on Answer"
 *
 * ---------------------------------------------------------------
 * 🔁 TRANSFORMATION (CRITICAL STEP)
 * ---------------------------------------------------------------
 *
 * Instead of directly maximizing, we ask:
 *
 *   ❓ Can we achieve minimum sweetness = X ?
 *
 * If YES → try bigger
 * If NO  → reduce
 *
 * This creates a MONOTONIC FUNCTION:
 *
 *   X increases → harder to satisfy
 *
 *   true true true false false
 *
 * 👉 Use Binary Search to find LAST TRUE
 *
 * ---------------------------------------------------------------
 * 🔍 SEARCH SPACE
 * ---------------------------------------------------------------
 *
 * Minimum possible sweetness = 1
 * Maximum possible sweetness = total sum
 *
 * ---------------------------------------------------------------
 * 🧩 GREEDY CHECK FUNCTION
 * ---------------------------------------------------------------
 *
 * Goal:
 *   Can we create ≥ (k+1) pieces such that
 *   each piece has sum ≥ target ?
 *
 * Strategy:
 *   - Keep accumulating
 *   - When sum ≥ target → cut immediately
 *
 * Why greedy works?
 *   - We want MAXIMUM number of pieces
 *   - Cutting early gives more pieces
 *   - More pieces = better chance to satisfy k+1
 *
 * ---------------------------------------------------------------
 * 🧪 DRY RUN
 * ---------------------------------------------------------------
 *
 * sweetness = [1,2,3,4,5,6,7,8,9], k = 5
 *
 * Try target = 6
 *
 * [1,2,3] → 6 → piece 1
 * [4,5]   → 9 → piece 2
 * [6]     → 6 → piece 3
 * [7]     → 7 → piece 4
 * [8]     → 8 → piece 5
 * [9]     → 9 → piece 6
 *
 * pieces = 6 ≥ (k+1 = 6) → VALID
 *
 * ---------------------------------------------------------------
 * ⏱ COMPLEXITY
 * ---------------------------------------------------------------
 *
 * Time:  O(n log(sum))
 * Space: O(1)
 *
 * ---------------------------------------------------------------
 * 🧠 INTERVIEW TRICK (MEMORY HOOK)
 * ---------------------------------------------------------------
 *
 * "Maximize minimum → Binary search answer
 *  Check via greedy partitioning"
 *
 * =====================================================================
 */
class DivideChocolate {

    public static void main(String[] args) {

        int[] sweetness = {1,2,3,4,5,6,7,8,9};
        int k = 5;

        System.out.println(maximizeSweetness(sweetness, k)); // 6
    }

    /**
     * ============================================================
     * 🔍 BINARY SEARCH ON ANSWER (LAST TRUE PATTERN)
     * ============================================================
     *
     * We try all possible minimum sweetness values.
     *
     * If valid → move RIGHT (maximize)
     * If invalid → move LEFT
     *
     * We explicitly track answer (important for interviews)
     */
    public static int maximizeSweetness(int[] sweetness, int k) {

        int low = 1;                              // minimum possible sweetness
        int high = Arrays.stream(sweetness).sum();// maximum possible

        int answer = 0; // stores best valid result

        while (low <= high) {

            int mid = low + (high - low) / 2;

            // Check if we can achieve minimum sweetness = mid
            if (canDivide(sweetness, k, mid)) {

                answer = mid;     // ✅ valid → store result
                low = mid + 1;    // try bigger (maximize)

            } else {

                high = mid - 1;   // ❌ not possible → reduce
            }
        }

        return answer;
    }

    /**
     * ============================================================
     * 🧩 GREEDY VALIDATION FUNCTION
     * ============================================================
     *
     * Can we create ≥ (k+1) pieces where each piece ≥ target ?
     *
     * Strategy:
     *   - Traverse array
     *   - Keep accumulating
     *   - When sum ≥ target → cut
     *
     * Why this works:
     *   - We maximize number of pieces
     *   - Early cutting ensures best utilization
     *
     * ------------------------------------------------------------
     * 🧪 INTERNAL TRACE (Example)
     * ------------------------------------------------------------
     *
     * target = 6
     *
     * currentSum = 0
     *
     * add 1 → 1
     * add 2 → 3
     * add 3 → 6 → CUT → pieces = 1
     *
     * add 4 → 4
     * add 5 → 9 → CUT → pieces = 2
     *
     * ...
     */
    private static boolean canDivide(int[] sweetness, int k, int target) {

        int pieces = 0;
        int currentSum = 0;

        for (int val : sweetness) {

            currentSum += val;

            // Once we reach target → form a piece
            if (currentSum >= target) {
                pieces++;
                currentSum = 0; // reset for next piece
            }
        }

        // We need at least k+1 pieces
        return pieces >= k + 1;
    }
}

/**
 * Problem Statement:
 * You have a chocolate bar consisting of chunks, with `sweetness[i]` representing the sweetness 
 * of the i-th chunk. You want to share it with `k` friends by making `k` cuts (resulting in k + 1 pieces).
 * You will always take the piece with the MINIMUM total sweetness.
 * Maximize the total sweetness of the piece you receive.
 * 
 * Constraints:
 * - 0 <= k < sweetness.length <= 10^3
 * - 1 <= sweetness[i] <= 10^3
 */
class DivideChocolate {

    /**
     * Helper Method: Greedily checks if it's possible to divide the chocolate into at least 
     * `targetPieces` (which is k + 1), such that EVERY piece has a sweetness >= `minSweetness`.
     */
    private static boolean canDivide(int[] sweetness, int targetPieces, int minSweetness) {
        int piecesFormed = 0;
        int currentSweetness = 0;

        for (int sweet : sweetness) {
            currentSweetness += sweet;
            // Once the current contiguous piece reaches the required minimum sweetness,
            // we cut it and start forming the next piece.
            if (currentSweetness >= minSweetness) {
                piecesFormed++;
                currentSweetness = 0;
            }
        }
        
        return piecesFormed >= targetPieces;
    }

    /**
     * SOLUTION 1: Iterative Binary Search on the Answer Space (Optimal)
     * 
     * Time Complexity: O(N * log(Sum / (K+1)))
     * Space Complexity: O(1)
     * 
     * VISUAL EXPLANATION & LOGIC:
     * Instead of figuring out where to place the cuts, we guess the ANSWER (the sweetness of our piece).
     * Our piece is the minimum of all pieces. We want to maximize this minimum.
     * 
     * sweetness = [1, 2, 3, 4, 5, 6, 7, 8, 9], k = 5 (Total pieces = 6)
     * Range of possible sweetness for our piece:
     * - Minimum possible: 1 (the minimum element in the array)
     * - Maximum possible: sum(sweetness) / 6 = 45 / 6 = 7 (If perfectly distributed)
     * 
     * Iteration 1:
     * L = 1, H = 7. mid = 4.
     * Can we make 6 pieces where EACH piece has sum >= 4?
     * [1, 2, 3] (6) | [4] (4) | [5] (5) | [6] (6) | [7] (7) | [8] (8) | [9] (9) 
     * We made 7 pieces! 7 >= 6. This means 4 is valid.
     * Save result = 4. Can we get an even sweeter piece? L = mid + 1 = 5.
     * 
     * Iteration 2:
     * L = 5, H = 7. mid = 6.
     * Can we make 6 pieces where EACH piece has sum >= 6?
     * [1, 2, 3] (6) | [4, 5] (9) | [6] (6) | [7] (7) | [8] (8) | [9] (9)
     * We made 6 pieces! 6 >= 6. 
     * Save result = 6. Try for more: L = mid + 1 = 7.
     * 
     * Iteration 3:
     * L = 7, H = 7. mid = 7.
     * Can we make 6 pieces where EACH piece has sum >= 7?
     * [1, 2, 3, 4] (10) | [5, 6] (11) | [7] (7) | [8] (8) | [9] (9)
     * We only made 5 pieces. 5 < 6. 7 is invalid.
     * H = mid - 1 = 6.
     * 
     * Loop Ends. Result is 6.
     */
    public static int maximizeSweetnessIterativeBS(int[] sweetness, int k) {
        int targetPieces = k + 1;
        
        // Find sum and min using Java Streams
        int sum = Arrays.stream(sweetness).sum();
        int min = Arrays.stream(sweetness).min().orElse(1);
        
        int low = min;
        int high = sum / targetPieces; // Theoretical max possible minimum piece
        
        int result = low; // Explicit result variable initialized to lowest possible answer

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (canDivide(sweetness, targetPieces, mid)) {
                // If we can successfully divide the chocolate such that everyone gets at least `mid`,
                // `mid` is a valid answer. Record it, and try to push for a higher minimum.
                result = mid;
                low = mid + 1;
            } else {
                // `mid` is too high, we can't form enough pieces. Reduce the target minimum sweetness.
                high = mid - 1;
            }
        }

        return result;
    }

    /**
     * SOLUTION 2: Recursive Binary Search on Answer Space
     * 
     * Time Complexity: O(N * log(Sum / (K+1)))
     * Space Complexity: O(log(Sum / (K+1))) - Call stack overhead
     * 
     * EXPLANATION:
     * Translates the optimal iterative binary search into a functional recursive model.
     * Uses `currentResult` to explicitly track and propagate the highest valid sweetness found.
     */
    public static int maximizeSweetnessRecursiveBSWrapper(int[] sweetness, int k) {
        int targetPieces = k + 1;
        int sum = Arrays.stream(sweetness).sum();
        int min = Arrays.stream(sweetness).min().orElse(1);
        
        return maximizeSweetnessRecursiveBS(sweetness, targetPieces, min, sum / targetPieces, min);
    }

    private static int maximizeSweetnessRecursiveBS(int[] sweetness, int targetPieces, int low, int high, int currentResult) {
        int result = currentResult; // Explicit result tracking

        if (low > high) {
            return result; // Base case: search space exhausted
        }

        int mid = low + (high - low) / 2;

        if (canDivide(sweetness, targetPieces, mid)) {
            // Valid target sweetness. Update result and search higher.
            result = maximizeSweetnessRecursiveBS(sweetness, targetPieces, mid + 1, high, mid);
        } else {
            // Target sweetness too high. Search lower.
            result = maximizeSweetnessRecursiveBS(sweetness, targetPieces, low, mid - 1, result);
        }

        return result;
    }

    /**
     * SOLUTION 3: Top-Down Dynamic Programming (Memoization)
     * 
     * Time Complexity: O(N^2 * K)
     * Space Complexity: O(N * K) for memoization table + Call Stack
     * 
     * EXPLANATION:
     * Let dp(i, parts) be the maximum possible minimum sweetness we can get by splitting 
     * the subarray `sweetness[i...N-1]` into `parts` pieces.
     * We try placing a cut at every index `j` from `i` to `N - parts`.
     * The minimum of a specific split is: min(sum(i to j), dp(j + 1, parts - 1)).
     * We want to MAXIMIZE this minimum across all possible cut locations `j`.
     */
    public static int maximizeSweetnessTopDownDP(int[] sweetness, int k) {
        int n = sweetness.length;
        int targetPieces = k + 1;
        
        int[][] memo = new int[n][targetPieces + 1];
        for (int[] row : memo) {
            Arrays.fill(row, -1);
        }
        
        // Precompute prefix sums for O(1) subarray sum queries
        int[] prefixSum = new int[n + 1];
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + sweetness[i];
        }

        return dfs(0, targetPieces, sweetness, prefixSum, memo);
    }

    private static int dfs(int startIndex, int piecesRemaining, int[] sweetness, int[] prefixSum, int[][] memo) {
        int n = sweetness.length;
        
        // Base case: 1 piece remaining means we just take the rest of the chocolate bar
        if (piecesRemaining == 1) {
            return prefixSum[n] - prefixSum[startIndex];
        }

        if (memo[startIndex][piecesRemaining] != -1) {
            return memo[startIndex][piecesRemaining];
        }

        int maxMinSweetness = 0;

        // Try every possible cut position for the current piece
        // We must leave at least (piecesRemaining - 1) chunks for the rest of the pieces
        for (int i = startIndex; i <= n - piecesRemaining; i++) {
            int currentPieceSweetness = prefixSum[i + 1] - prefixSum[startIndex];
            
            // The minimum sweetness if we cut here is the minimum of this piece 
            // and the optimally divided remaining pieces
            int minOfThisSplit = Math.min(currentPieceSweetness, dfs(i + 1, piecesRemaining - 1, sweetness, prefixSum, memo));
            
            // We want to maximize the minimum piece we get across all cut combinations
            maxMinSweetness = Math.max(maxMinSweetness, minOfThisSplit);
        }

        return memo[startIndex][piecesRemaining] = maxMinSweetness;
    }

    /**
     * SOLUTION 4: Bottom-Up Dynamic Programming
     * 
     * Time Complexity: O(N^2 * K)
     * Space Complexity: O(N * K)
     * 
     * EXPLANATION:
     * dp[i][j] = the maximum possible minimum sweetness by dividing `sweetness[0...i-1]` into `j` parts.
     * We build the table from smaller chunks up to the full array.
     */
    public static int maximizeSweetnessBottomUpDP(int[] sweetness, int k) {
        int n = sweetness.length;
        int targetPieces = k + 1;
        
        int[][] dp = new int[n + 1][targetPieces + 1];
        
        int[] prefixSum = new int[n + 1];
        for (int i = 0; i < n; i++) {
            prefixSum[i + 1] = prefixSum[i] + sweetness[i];
        }
        
        // Base case: splitting into 1 piece is just the sum of the array up to that point
        for (int i = 1; i <= n; i++) {
            dp[i][1] = prefixSum[i];
        }

        // Fill DP table
        for (int j = 2; j <= targetPieces; j++) { // For every number of pieces
            for (int i = j; i <= n; i++) { // For every array length up to n
                // Try making the last cut at every position p before i
                for (int p = j - 1; p < i; p++) { 
                    int lastPieceSweetness = prefixSum[i] - prefixSum[p];
                    
                    // The minimum sweetness of this specific configuration
                    int currentConfigurationMin = Math.min(dp[p][j - 1], lastPieceSweetness);
                    
                    // We want to maximize the minimum piece across all possible cuts
                    dp[i][j] = Math.max(dp[i][j], currentConfigurationMin);
                }
            }
        }

        return dp[n][targetPieces];
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to structure the test cases elegantly.
     */
    public record TestCase(int[] sweetness, int k, int expected) {}

    public static void main(String[] args) {
        // Defined Test Cases based on standard examples and boundaries
        TestCase[] testCases = {
            new TestCase(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9}, 5, 6),  // Standard Example 1
            new TestCase(new int[]{5, 6, 7, 8, 9, 1, 2, 3, 4}, 8, 1),  // k = n - 1 (Every chunk is its own piece)
            new TestCase(new int[]{1, 2, 2, 1, 2, 2, 1, 2, 2}, 2, 5),  // Standard Example 2
            new TestCase(new int[]{10, 20, 30, 40}, 1, 30),            // Divide into 2 pieces: [10, 20] (30) | [30, 40] (70) -> min is 30
            new TestCase(new int[]{100, 100, 100, 100}, 3, 100),       // Uniform pieces
            new TestCase(IntStream.rangeClosed(1, 50).toArray(), 4, 235) // Stress test
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            int resIterativeBS = maximizeSweetnessIterativeBS(tc.sweetness(), tc.k());
            int resRecursiveBS = maximizeSweetnessRecursiveBSWrapper(tc.sweetness(), tc.k());
            int resTopDownDP   = maximizeSweetnessTopDownDP(tc.sweetness(), tc.k());
            
            // Limit Bottom-Up DP for massive arrays to keep testing snappy. 
            // For N=50, O(N^2 * K) is trivial and will execute instantly.
            int resBottomUpDP  = maximizeSweetnessBottomUpDP(tc.sweetness(), tc.k());

            boolean passed = (resIterativeBS == tc.expected()) &&
                             (resRecursiveBS == tc.expected()) &&
                             (resTopDownDP == tc.expected()) &&
                             (resBottomUpDP == tc.expected());

            // Limit array printing length for neat terminal output
            String arrStr = Arrays.toString(tc.sweetness());
            if (arrStr.length() > 25) arrStr = arrStr.substring(0, 22) + "...]";

            System.out.printf("Test %d | k: %-2d | Sweetness: %-25s -> Expected: %-4d | Passed: %b%n",
                    i + 1, tc.k(), arrStr, tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] IterBS: %d, RecBS: %d, TopDP: %d, BotDP: %d%n",
                        resIterativeBS, resRecursiveBS, resTopDownDP, resBottomUpDP);
            }
        }
    }
}
