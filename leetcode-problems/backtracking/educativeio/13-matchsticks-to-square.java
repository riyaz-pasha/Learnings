/**
 * ============================================================================
 * MATCHSTICKS TO SQUARE - COMPREHENSIVE GUIDE & SOLUTIONS
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * We are given a pile of matchsticks of various lengths. Our goal is to connect 
 * them end-to-end to form a perfect square. 
 * Rules:
 *   - We MUST use every single matchstick exactly once.
 *   - We CANNOT break any matchsticks.
 *   - A perfect square has 4 sides of equal length.
 * If it's possible to partition the matchsticks into 4 groups where the sum 
 * of lengths in each group is exactly the same, we return true.
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: What if the total length of all matchsticks cannot form a square?
 * A: Since a square has 4 equal sides, the total sum of all matchsticks MUST 
 *    be perfectly divisible by 4. If it's not, it's mathematically impossible, 
 *    and we can immediately return false.
 * 
 * Q: What is the maximum number of matchsticks?
 * A: The constraints state matchsticks.length <= 15. This is the golden clue! 
 *    Small inputs (N <= 20) strongly suggest Depth-First Search (DFS) / 
 *    Backtracking with pruning, or Bitmask DP.
 * 
 * Q: What if a single matchstick is longer than the required side of the square?
 * A: Since we can't break matchsticks, if any single stick is larger than 
 *    (total_sum / 4), we can immediately return false.
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - BUCKET FILLING: Imagine 4 empty buckets, each representing a side of the 
 *   square. Their maximum capacity is `target = total_sum / 4`. We need to 
 *   drop every matchstick into one of these 4 buckets such that no bucket 
 *   overflows.
 * - BACKTRACKING: For each matchstick, we try putting it into Bucket 1. If 
 *   that eventually leads to a dead end, we take it out and try Bucket 2, 
 *   and so on.
 * - THE SORTING TRICK (PRUNING 1): If we try to place the small matchsticks 
 *   first (e.g., length 1), they easily fit into any bucket. We will go deep 
 *   into the recursion tree before realizing the giant matchsticks (e.g., 
 *   length 100) at the end don't fit anywhere! 
 *   -> ALWAYS sort descending. Place the largest matchsticks first to fail fast.
 * - SYMMETRY AVOIDANCE (PRUNING 2): If Bucket 1 and Bucket 2 are currently 
 *   empty (or hold the exact same total), trying to put a matchstick into 
 *   Bucket 2 after it failed in Bucket 1 is a waste of time. They are identical 
 *   states!
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Start with the math. Calculate the total sum, check if it's 
 *   divisible by 4. Calculate the target length.
 * - Step 2: Explain the decision tree (placing stick `i` into one of 4 sides).
 * - Step 3: Write the DFS recursively.
 * - Step 4: Before running the code, highlight that it might Time Out (TLE) 
 *   without pruning. Add the reverse sort and symmetry checks, showing you 
 *   understand how to optimize factorial/exponential searches.
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * matchsticks = [1, 1, 2, 2, 2]
 * Total sum = 8. Target side length = 8 / 4 = 2.
 * 
 * Sorted descending: [2, 2, 2, 1, 1]
 * 
 * Stick 0 (length 2): Put in Bucket 1 -> B1 = 2
 * Stick 1 (length 2): Put in Bucket 2 -> B2 = 2 (Can't go in B1, capacity is 2)
 * Stick 2 (length 2): Put in Bucket 3 -> B3 = 2
 * Stick 3 (length 1): Put in Bucket 4 -> B4 = 1
 * Stick 4 (length 1): Put in Bucket 4 -> B4 = 2
 * 
 * All sticks placed! Result: TRUE.
 */

import java.util.Arrays;

public class MatchsticksToSquare {

    public boolean makesquare(int[] matchsticks) {

        /*
         * ============================================================
         * INITIAL MATH & VALIDATION
         * ============================================================
         */
        if (matchsticks == null || matchsticks.length < 4) {
            return false;
        }

        long totalSum = 0;
        for (int stick : matchsticks) {
            totalSum += stick;
        }

        // If the total sum cannot be split into 4 equal sides, it's impossible.
        if (totalSum % 4 != 0) {
            return false;
        }

        int targetSideLength = (int) (totalSum / 4);

        /*
         * ============================================================
         * OPTIMIZATION 1: SORT DESCENDING
         * ============================================================
         * 
         * By attempting to place the LARGEST matchsticks first, we drastically 
         * reduce the number of branches in our decision tree. 
         * 
         * If a large matchstick cannot fit anywhere, we will discover this 
         * failure very early (near the root of the recursion tree) instead of 
         * wasting time placing dozens of tiny matchsticks first.
         */
        Arrays.sort(matchsticks);
        reverse(matchsticks);

        // Immediate impossible check: If the largest stick is bigger than the 
        // side of the square, we can never form the square.
        if (matchsticks[0] > targetSideLength) {
            return false;
        }

        // Array to track the current filled length of our 4 sides (buckets)
        int[] sides = new int[4];

        // Start backtracking from the 0th matchstick
        return backtrack(matchsticks, sides, 0, targetSideLength);
    }

    private boolean backtrack(int[] matchsticks, int[] sides, int index, int target) {
        
        /*
         * ============================================================
         * BASE CASE
         * ============================================================
         * 
         * If we have successfully found a place for every single matchstick 
         * (index reaches the length of the array), we are done!
         * 
         * (Because we rigorously check that no side exceeds 'target', and the 
         * total sum is exactly 4 * target, we are guaranteed that all 4 sides 
         * equal the target if we place all pieces).
         */
        if (index == matchsticks.length) {
            return true;
        }

        /*
         * ============================================================
         * THE DECISION LOOP
         * ============================================================
         * 
         * For the current matchstick (at 'index'), we have 4 choices:
         * We can try placing it in Side 0, Side 1, Side 2, or Side 3.
         */
        for (int i = 0; i < 4; i++) {
            
            /*
             * --------------------------------------------------------
             * CAPACITY PRUNING
             * --------------------------------------------------------
             * If adding this matchstick makes the side longer than our 
             * target, this bucket is not a valid choice. Skip it.
             */
            if (sides[i] + matchsticks[index] > target) {
                continue;
            }

            /*
             * --------------------------------------------------------
             * CHOOSE
             * --------------------------------------------------------
             * "Let's TRY placing the current matchstick into bucket 'i'."
             */
            sides[i] += matchsticks[index];

            /*
             * --------------------------------------------------------
             * EXPLORE
             * --------------------------------------------------------
             * Recursively try to place the NEXT matchstick (index + 1).
             * If this path leads to a complete square, immediately return 
             * true to stop all other exploration!
             */
            if (backtrack(matchsticks, sides, index + 1, target)) {
                return true;
            }

            /*
             * --------------------------------------------------------
             * UNCHOOSE (BACKTRACK)
             * --------------------------------------------------------
             * If we are here, it means placing the matchstick in bucket 'i'
             * eventually led to a dead end down the line. 
             * Remove it from this bucket so we can try the NEXT bucket.
             */
            sides[i] -= matchsticks[index];

            /*
             * ========================================================
             * OPTIMIZATION 2: SYMMETRY AVOIDANCE (CRITICAL PRUNING)
             * ========================================================
             * 
             * Suppose we just tried placing the current matchstick into 
             * bucket 'i' which was EMPTY (sides[i] == 0), and it FAILED.
             * 
             * If bucket 'i' was empty, and it didn't work, there is absolutely 
             * ZERO reason to try placing this same matchstick into another 
             * empty bucket (like i+1). The buckets are identical!
             * 
             * If placing it in the first empty space fails, placing it in 
             * the second empty space will explore the EXACT same failure tree. 
             * 
             * Thus, if the current bucket was completely empty after we removed 
             * the stick, we break the loop to fail faster.
             */
            if (sides[i] == 0) {
                break;
            }
        }

        /*
         * ============================================================
         * DEAD END REACHED
         * ============================================================
         * If we tried all 4 buckets for the current matchstick and NONE 
         * of them led to a solution, it means an earlier matchstick was 
         * placed incorrectly. Return false to trigger backtracking above us.
         */
        return false;
    }

    /**
     * UTILITY: Reverses an array in-place.
     * Used after Arrays.sort() to achieve descending order.
     */
    private void reverse(int[] nums) {
        int i = 0;
        int j = nums.length - 1;
        while (i < j) {
            int temp = nums[i];
            nums[i] = nums[j];
            nums[j] = temp;
            i++;
            j--;
        }
    }

    /**
     * MAIN METHOD: Executing and testing our code
     */
    public static void main(String[] args) {
        MatchsticksToSquare solver = new MatchsticksToSquare();

        // Test Case 1: Perfect Square possible
        // Total sum = 8, Target = 2
        int[] test1 = {1, 1, 2, 2, 2};
        System.out.println("Test 1: " + Arrays.toString(test1));
        System.out.println("Expected: true | Result: " + solver.makesquare(test1));
        System.out.println();

        // Test Case 2: Not possible (sum is 15, not divisible by 4)
        int[] test2 = {3, 3, 3, 3, 4};
        System.out.println("Test 2: " + Arrays.toString(test2));
        System.out.println("Expected: false | Result: " + solver.makesquare(test2));
        System.out.println();
        
        // Test Case 3: Tricky impossible case 
        // Sum = 16, Target = 4. 
        // Array: [5, 5, 2, 2, 2]. 
        // Wait, largest is 5, target is 4. Will fail immediately.
        int[] test3 = {5, 5, 2, 2, 2};
        System.out.println("Test 3: " + Arrays.toString(test3));
        System.out.println("Expected: false | Result: " + solver.makesquare(test3));
    }
}
