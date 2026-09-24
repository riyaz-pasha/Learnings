/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of "banned" integers, an upper bound 'n', and a 'maxSum'.
 * We need to pick as many unique integers as possible from the range [1, n] 
 * such that:
 * 1. None of the picked integers are in the 'banned' array.
 * 2. The sum of the picked integers is less than or equal to 'maxSum'.
 * 
 * Our goal is to return the maximum count of numbers we can pick under these rules.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can the 'banned' array contain duplicates? 
 *    (Yes, it can. Our data structure should handle duplicates gracefully).
 * 2. Are there any numbers in the 'banned' array that are greater than 'n'?
 *    (Yes, constraints say banned[i] can be up to 10^3. If a banned number 
 *    is > n, we can just ignore it because we only pick numbers from [1, n]).
 * 3. Can 'n' be extremely large, like 10^9?
 *    (No, constraints say n <= 10^3. This means O(N) approaches are optimal. 
 *    If N were 10^9, we would need a more complex mathematical + binary search approach).
 * 4. Could the sum of picked integers exceed the 32-bit signed integer limit?
 *    (Max n is 1000. The sum of 1 to 1000 is ~500,500, which easily fits in a 
 *    standard int. 'maxSum' is also up to 10^6, so 'int' is perfectly safe).
 * 5. Do we need to return the actual numbers chosen or just the count?
 *    (Just the count).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY (GREEDY)
 * ============================================================================
 * Key Intuition (Greedy Choice Property):
 * To maximize the *count* of items without exceeding a *sum capacity*, we must 
 * always pick the smallest items first. 
 * A smaller number consumes less of our 'maxSum' budget, leaving more room for 
 * subsequent numbers.
 * 
 * 1. Solution 1: HashSet (Standard/General Approach)
 *    - Store all banned numbers in a HashSet for O(1) lookups.
 *    - Iterate from 1 to n. If the number is not in the set, and adding it 
 *      doesn't exceed maxSum, add it to our running sum and increment count.
 *    - Stop when the running sum exceeds maxSum.
 *    - Time: O(B + N), Space: O(B) where B is the length of the banned array.
 * 
 * 2. Solution 2: Boolean Array (Constraint-Optimized Approach)
 *    - Since 'n' is very small (<= 1000), we don't need the overhead of a HashSet.
 *    - Use a simple boolean array of size 'n + 1' to mark banned numbers.
 *    - Time: O(B + N), Space: O(N). This is significantly faster in practice 
 *      due to cache locality and no hashing overhead.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING
 * ============================================================================
 * Example: banned = [1, 6, 5], n = 5, maxSum = 6
 * 
 * 1. Mark Banned:
 *    [1, 6, 5] -> We only care about numbers <= n (5).
 *    Banned set/array marks: {1, 5} (we ignore 6).
 * 
 * 2. Greedy Selection (Iterate i from 1 to 5):
 *    - i = 1: Banned. (Skip)
 *    - i = 2: Not banned. sum = 0 + 2 = 2. 2 <= 6. count = 1.
 *    - i = 3: Not banned. sum = 2 + 3 = 5. 5 <= 6. count = 2.
 *    - i = 4: Not banned. Try adding: sum = 5 + 4 = 9. 
 *             9 > 6! We cannot add 4.
 *             Because we process in increasing order, any future number > 4 
 *             will also exceed maxSum. So we can break early.
 * 
 * Final count = 2 (We picked 2 and 3).
 */

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.IntStream;

public class MaximumNumberOfIntegersToChoose {

    public static void main(String[] args) {
        int[] banned1 = {1, 6, 5};
        int n1 = 5;
        int maxSum1 = 6;

        int[] banned2 = {11};
        int n2 = 7;
        int maxSum2 = 50;

        System.out.println("Test Case 1: banned = [1,6,5], n = 5, maxSum = 6");
        System.out.println("HashSet Solution: " + maxCountHashSet(banned1, n1, maxSum1));
        System.out.println("Boolean Array Solution: " + maxCountBooleanArray(banned1, n1, maxSum1));
        System.out.println("--------------------------------------------------");

        System.out.println("Test Case 2: banned = [11], n = 7, maxSum = 50");
        System.out.println("HashSet Solution: " + maxCountHashSet(banned2, n2, maxSum2));
        System.out.println("Boolean Array Solution: " + maxCountBooleanArray(banned2, n2, maxSum2));
    }

    /**
     * SOLUTION 1: HashSet + Greedy Selection
     * 
     * Idea: Dump banned numbers into a HashSet. Loop from 1 to n, greedily 
     * picking the smallest numbers that aren't in the HashSet until we run 
     * out of sum budget.
     * 
     * Time Complexity: O(B + N) - where B is length of banned array, N is 'n'.
     * Space Complexity: O(B) - to store the banned elements in the Set.
     */
    public static int maxCountHashSet(int[] banned, int n, int maxSum) {
        Set<Integer> bannedSet = new HashSet<>();
        for (int b : banned) {
            bannedSet.add(b);
        }

        int count = 0;
        int currentSum = 0;

        for (int i = 1; i <= n; i++) {
            if (bannedSet.contains(i)) {
                continue; // Skip banned numbers
            }
            
            currentSum += i;
            
            if (currentSum > maxSum) {
                break; // Stop immediately once we exceed maxSum
            }
            
            count++;
        }

        return count;
    }

    /**
     * SOLUTION 2: Boolean Array + Greedy (Highly Optimal for these constraints)
     * 
     * Idea: Since n <= 1000, we don't need a dynamically sized HashSet. 
     * A boolean array of size n + 1 provides O(1) lookups with zero hashing 
     * overhead and perfect memory contiguity.
     * 
     * Time Complexity: O(B + N) - One pass over banned, one pass up to N.
     * Space Complexity: O(N) - For the boolean array.
     */
    public static int maxCountBooleanArray(int[] banned, int n, int maxSum) {
        // Boolean array to act as a direct access table
        boolean[] isBanned = new boolean[n + 1];
        
        for (int num : banned) {
            // We only care about marking numbers within our target range [1, n]
            if (num >= 1 && num <= n) {
                isBanned[num] = true;
            }
        }

        int count = 0;
        int currentSum = 0;

        for (int i = 1; i <= n; i++) {
            if (!isBanned[i]) {
                currentSum += i;
                
                // If adding this number exceeds the limit, we can't take it 
                // (or any larger numbers), so we terminate early.
                if (currentSum > maxSum) {
                    break;
                }
                
                count++;
            }
        }

        return count;
    }
}
