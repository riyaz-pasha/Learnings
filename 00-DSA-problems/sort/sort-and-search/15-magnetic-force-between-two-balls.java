/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of coordinates representing the positions of `n` baskets
 * and an integer `m` representing the number of balls we have. 
 * We need to distribute all `m` balls into these baskets such that the magnetic 
 * force (absolute difference in position) between any two adjacent balls is 
 * maximized. We want to find the MAXIMUM possible value of this MINIMUM 
 * distance between the balls.
 * 
 * In simpler terms: Place 'm' items in 'n' slots so they are as far apart 
 * from each other as possible, and return that minimum gap.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Are the basket positions provided in sorted order? 
 *    (Assume no, so sorting should be our first step).
 * 2. Can two baskets be at the exact same position? 
 *    (Constraints say all integers in `position` are unique).
 * 3. What if m == 2? 
 *    (We simply place the two balls at the two extremes of the sorted positions. 
 *    The answer is max_pos - min_pos).
 * 4. Can m be greater than the number of baskets? 
 *    (Constraints say 2 <= m <= position.length, so we always have enough baskets).
 * 5. Are there any space constraints or requirements? 
 *    (Typically O(1) auxiliary space is expected for this problem after sorting).
 * 6. Can the positions or calculated distances exceed the 32-bit integer limit? 
 *    (Constraints say position[i] <= 10^4, so max difference is < 10^4. Standard 
 *    `int` is perfectly fine).
 * 7. Is it guaranteed that a valid arrangement exists? 
 *    (Yes, since m <= number of baskets, we can always place all m balls).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Brute Force (Combinations):
 *    - Generate all possible ways to choose `m` baskets out of `n`.
 *    - For each combination, find the minimum distance between any two adjacent balls.
 *    - Keep track of the maximum of these minimum distances.
 *    - Time Complexity: O(C(n, m) * m), Space Complexity: O(m) for recursion stack.
 *    - Conclusion: With n=1000, this is mathematically impossible to compute in time. 
 *      Mention it to show understanding, then immediately transition.
 * 
 * 2. Binary Search on Answer (The "Aha!" Moment / Optimal):
 *    - Instead of guessing combinations, let's guess the ANSWER (the distance).
 *    - What is the possible range of the answer?
 *      - Minimum possible distance = 1 (since positions are unique integers).
 *      - Maximum possible distance = position[n-1] - position[0] (if m=2).
 *    - If I ask: "Can we place `m` balls such that the minimum distance is AT LEAST `d`?", 
 *      we can answer this in O(N) using a Greedy Strategy.
 *    - Greedy verification: Place the first ball in the first basket. Iterate through 
 *      the sorted baskets. Place the next ball in the first basket that is at least `d` 
 *      distance away from the last placed ball. If we can place all `m` balls, `d` is valid!
 *    - Since the validity of `d` is monotonic (if distance `d` works, `d-1` definitely works; 
 *      if `d` fails, `d+1` definitely fails), we can use Binary Search to find the max `d`.
 *    - Time Complexity: O(N log N + N log D), where D = max_pos - min_pos.
 *    - Space Complexity: O(1) (ignoring sorting overhead).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING (Binary Search + Greedy)
 * ============================================================================
 * Example: position = [1, 2, 8, 4, 9], m = 3
 * 
 * 1. Sort position: [1, 2, 4, 8, 9]
 * 
 * 2. Define Search Space:
 *    low = 1
 *    high = 9 - 1 = 8
 * 
 * 3. Binary Search Iteration 1:
 *    mid (guessed distance) = (1 + 8) / 2 = 4
 *    Greedy Check (d=4):
 *    - Place ball 1 at pos 1. (balls placed: 1)
 *    - Next ball needs to be at >= 1 + 4 = 5.
 *    - Pos 2 is < 5 (Skip).
 *    - Pos 4 is < 5 (Skip).
 *    - Pos 8 is >= 5 (Place ball 2!). (balls placed: 2)
 *    - Next ball needs to be at >= 8 + 4 = 12.
 *    - Pos 9 is < 12 (Skip).
 *    Total balls placed = 2. But we need m=3. So d=4 FAILED.
 *    Adjust range: high = mid - 1 = 3.
 * 
 * 4. Binary Search Iteration 2:
 *    low = 1, high = 3.
 *    mid = (1 + 3) / 2 = 2
 *    Greedy Check (d=2):
 *    - Place ball 1 at pos 1. 
 *    - Pos 2 < 1+2 (Skip).
 *    - Pos 4 >= 1+2. Place ball 2!
 *    - Pos 8 >= 4+2. Place ball 3! 
 *    Total balls placed = 3. Success!
 *    Update max_ans = 2.
 *    Adjust range: low = mid + 1 = 3.
 * 
 * 5. Binary Search Iteration 3:
 *    low = 3, high = 3.
 *    mid = (3 + 3) / 2 = 3.
 *    Greedy Check (d=3):
 *    - Place ball 1 at 1.
 *    - Pos 4 >= 1+3. Place ball 2!
 *    - Pos 8 >= 4+3. Place ball 3!
 *    Total balls placed = 3. Success!
 *    Update max_ans = 3.
 *    Adjust range: low = mid + 1 = 4.
 * 
 * 6. low > high (4 > 3). Loop terminates.
 *    Return max_ans = 3.
 */

import java.util.Arrays;

public class MagneticForceBetweenBalls {

    public static void main(String[] args) {
        int[] position1 = {1, 2, 8, 4, 9};
        int m1 = 3;

        int[] position2 = {5, 4, 3, 2, 1, 1000000000};
        int m2 = 2;

        System.out.println("Input: position = [1,2,8,4,9], m = 3");
        // Warning: Brute force works for small inputs only. Included for conceptual completeness.
        System.out.println("Brute Force Solution:   " + maxDistanceBruteForce(position1.clone(), m1));
        System.out.println("Binary Search Solution: " + maxDistanceBinarySearch(position1.clone(), m1));
        System.out.println("--------------------------------------------------");

        System.out.println("Input: position = [5,4,3,2,1,1000000000], m = 2");
        System.out.println("Binary Search Solution: " + maxDistanceBinarySearch(position2.clone(), m2));
    }

    /**
     * SOLUTION 1: Brute Force (Backtracking/Combinations)
     * 
     * Idea: Generate all subsets of size `m` from the `position` array.
     * Calculate the minimum distance for each subset and track the overall maximum.
     * 
     * Time Complexity: O(nCm * m) where n is position.length. Exponential and very slow.
     * Space Complexity: O(m) for the recursion stack and temporary array.
     */
    public static int maxDistanceBruteForce(int[] position, int m) {
        Arrays.sort(position);
        return getCombinations(position, m, 0, new int[m], 0);
    }

    private static int getCombinations(int[] pos, int m, int posIndex, int[] currentCombo, int comboIndex) {
        // Base case: we selected m elements
        if (comboIndex == m) {
            int currentMinDist = Integer.MAX_VALUE;
            // Calculate minimum distance between adjacent chosen balls
            for (int i = 1; i < m; i++) {
                currentMinDist = Math.min(currentMinDist, currentCombo[i] - currentCombo[i - 1]);
            }
            return currentMinDist;
        }

        // Base case: no more elements to pick from
        if (posIndex >= pos.length) {
            return -1;
        }

        int maxMinDist = -1;

        // Option 1: Include the current position in our combination
        currentCombo[comboIndex] = pos[posIndex];
        int includeDist = getCombinations(pos, m, posIndex + 1, currentCombo, comboIndex + 1);

        // Option 2: Exclude the current position
        int excludeDist = getCombinations(pos, m, posIndex + 1, currentCombo, comboIndex);

        // Track the maximum of the minimums we found
        return Math.max(includeDist, excludeDist);
    }

    /**
     * SOLUTION 2: Binary Search + Greedy (Optimal)
     * 
     * Idea: Binary search the target minimum distance. For each guess `mid`, use 
     * a greedy approach to check if we can place `m` balls such that the distance 
     * between any two is AT LEAST `mid`.
     * 
     * Time Complexity: O(N log N + N log D) where N is array length and D is max position diff.
     * Space Complexity: O(1) beyond sorting overhead.
     */
    public static int maxDistanceBinarySearch(int[] position, int m) {
        // 1. Sort the baskets by their positions
        Arrays.sort(position);
        
        int n = position.length;
        
        // 2. Define the search space for the distance
        int low = 1; // Minimum possible distance
        int high = position[n - 1] - position[0]; // Maximum possible distance (if m=2)
        int bestDistance = 0;
        
        // 3. Binary Search on the answer
        while (low <= high) {
            int mid = low + (high - low) / 2;
            
            if (canPlaceBalls(position, m, mid)) {
                // If we CAN place the balls with at least 'mid' distance,
                // we record this as our best answer so far and try for a LARGER distance.
                bestDistance = mid;
                low = mid + 1;
            } else {
                // If we CANNOT place the balls, the guessed distance 'mid' is too large.
                // We must reduce our search space to smaller distances.
                high = mid - 1;
            }
        }
        
        return bestDistance;
    }
    
    /**
     * Helper method for the Greedy check.
     * Determines if we can place `m` balls in the `position` array such that 
     * the distance between any two adjacent balls is at least `minDist`.
     */
    private static boolean canPlaceBalls(int[] position, int m, int minDist) {
        // We always place the first ball in the first available basket to maximize room.
        int ballsPlaced = 1;
        int lastPosition = position[0];
        
        for (int i = 1; i < position.length; i++) {
            // Check if the current basket is far enough from the last placed ball
            if (position[i] - lastPosition >= minDist) {
                ballsPlaced++;
                lastPosition = position[i];
                
                // If we've successfully placed all 'm' balls, this distance is valid
                if (ballsPlaced == m) {
                    return true;
                }
            }
        }
        
        // If we ran out of baskets before placing all 'm' balls, this distance is invalid
        return false;
    }
}
