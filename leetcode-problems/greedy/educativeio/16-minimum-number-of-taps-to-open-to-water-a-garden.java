import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: MINIMUM NUMBER OF TAPS TO OPEN TO WATER A GARDEN
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Can a tap's range be 0?" 
 *      (Assumption: Yes, meaning the tap only waters exactly its own position 
 *      and effectively covers a span of 0 distance).
 *    - "Can the garden be partially watered?" 
 *      (Assumption: The prompt states we must water the entire garden [0, n]. 
 *      If impossible, we return -1).
 *    - "What is the maximum size of n?" 
 *      (Assumption: n <= 10^4. This means an O(N^2) solution might be acceptable, 
 *      but an O(N) is highly preferred and expected for an optimal solution).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Find the minimum taps to completely cover the interval [0, n].
 *    - Observation 1 (Interval Conversion): Each tap at index `i` waters from 
 *      `i - ranges[i]` to `i + ranges[i]`. We can bound the left side to 0, 
 *      because watering negative coordinates doesn't help us. So the effective 
 *      interval is `[max(0, i - ranges[i]), i + ranges[i]]`.
 *    - Observation 2 (Transformation to "Jump Game II"): This problem is secretly 
 *      "Jump Game II" in disguise. In Jump Game, you are given an array where 
 *      `nums[i]` is the maximum jump length from index `i`. 
 *      If we create a new array `maxReach` of size `n+1`, where `maxReach[i]` 
 *      stores the *farthest right coordinate* we can reach given that an 
 *      interval started at or before `i`, we can use the exact same greedy 
 *      "sliding window" logic as Jump Game II.
 *    - Approach:
 *      1. Precompute `maxReach`. Iterate through each tap. For an interval 
 *         `[left, right]`, update `maxReach[left] = max(maxReach[left], right)`.
 *      2. Now, iterate from `0` to `n-1`. Keep track of `currentWindowEnd` and 
 *         `farthestReach`. Every time our index reaches `currentWindowEnd`, we 
 *         *must* open another tap (increment answer), and our new window extends 
 *         to `farthestReach`.
 *      3. If `farthestReach` is ever less than or equal to `i` when we are forced 
 *         to jump, it means we are stuck and cannot reach the end. Return -1.
 * 
 * 3. VISUAL EXPLANATION:
 *    n = 5, ranges = [3, 4, 1, 1, 0, 0]
 *    
 *    Intervals:
 *    Tap 0: [-3, 3] -> Left = 0, Right = 3  -> maxReach[0] = 3
 *    Tap 1: [-3, 5] -> Left = 0, Right = 5  -> maxReach[0] = max(3, 5) = 5
 *    Tap 2: [1, 3]  -> Left = 1, Right = 3  -> maxReach[1] = 3
 *    Tap 3: [2, 4]  -> Left = 2, Right = 4  -> maxReach[2] = 4
 *    Tap 4: [4, 4]  -> Left = 4, Right = 4  -> maxReach[4] = 4
 *    Tap 5: [5, 5]  -> Left = 5, Right = 5  -> maxReach[5] = 5
 *    
 *    maxReach Array: [5, 3, 4, 0, 4, 5]
 *    
 *    Greedy Traversal (Goal = 5):
 *    i=0: farthestReach = max(0, 5) = 5. 
 *         We hit currentWindowEnd (0). We must jump! 
 *         taps = 1. currentWindowEnd = 5.
 *         (Since currentWindowEnd >= 5, we can stop early).
 *         
 *    Result: 1 tap (Tap 1 covers the whole thing).
 * 
 * ============================================================================
 */
public class MinimumTapsToWaterGarden {

    /**
     * APPROACH 1: Greedy "Jump Game" Logic (Optimal)
     * 
     * Time Complexity: O(N) where N is the number of taps. We iterate through 
     * the arrays a constant number of times.
     * Space Complexity: O(N) to store the maxReach array.
     */
    public int minTapsGreedy(int n, int[] ranges) {
        // maxReach[i] will store the furthest right point we can reach 
        // from any tap whose left boundary is at or before 'i'
        int[] maxReach = new int[n + 1];
        
        // Step 1: Precompute the maximum right-reach for each left boundary
        for (int i = 0; i <= n; i++) {
            int left = Math.max(0, i - ranges[i]);
            int right = Math.min(n, i + ranges[i]);
            maxReach[left] = Math.max(maxReach[left], right);
        }
        
        // Step 2: Greedy Traversal (Jump Game II logic)
        int taps = 0;
        int currentWindowEnd = 0;
        int farthestReach = 0;
        
        // We only iterate up to n - 1 because if we can't reach 'n' by the 
        // time we are at 'n-1', we have failed anyway.
        for (int i = 0; i < n; i++) {
            farthestReach = Math.max(farthestReach, maxReach[i]);
            
            // If we are stuck at an index and our farthest reach can't go beyond it
            if (farthestReach <= i) {
                return -1; // It's impossible to move forward
            }
            
            // If we reached the end of our current watering window
            if (i == currentWindowEnd) {
                taps++;
                currentWindowEnd = farthestReach;
                
                // Early exit optimization: If our new window covers the end, stop.
                if (currentWindowEnd >= n) {
                    break;
                }
            }
        }
        
        return taps;
    }

    /**
     * APPROACH 2: Dynamic Programming (Highly Intuitive Alternative)
     * 
     * In an interview, it's great to explain this conceptual approach first.
     * We use the exact same maxReach preprocessing, but instead of tracking windows,
     * we build an array where dp[i] is the minimum taps to reach coordinate i.
     * 
     * Time Complexity: O(N^2) in the worst case (e.g., if every tap covers the whole garden).
     * Space Complexity: O(N) for DP and maxReach arrays.
     */
    public int minTapsDP(int n, int[] ranges) {
        int[] maxReach = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            int left = Math.max(0, i - ranges[i]);
            int right = Math.min(n, i + ranges[i]);
            maxReach[left] = Math.max(maxReach[left], right);
        }
        
        int[] dp = new int[n + 1];
        // Fill with infinity equivalent (n + 2 is impossible since max taps is n + 1)
        Arrays.fill(dp, n + 2); 
        dp[0] = 0; // 0 taps needed to reach the start
        
        for (int i = 0; i <= n; i++) {
            if (dp[i] == n + 2) continue; // If we can't reach 'i', we can't jump from it
            
            // For every point this left boundary can reach, update the minimum taps
            for (int j = i + 1; j <= maxReach[i]; j++) {
                dp[j] = Math.min(dp[j], dp[i] + 1);
            }
        }
        
        return dp[n] < n + 2 ? dp[n] : -1;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int n, int[] ranges, int expected) {}

    public static void main(String[] args) {
        MinimumTapsToWaterGarden solver = new MinimumTapsToWaterGarden();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(5, new int[]{3, 4, 1, 1, 0, 0}, 1), // Standard case
            new TestCase(3, new int[]{0, 0, 0, 0}, -1),      // Impossible to water
            new TestCase(7, new int[]{1, 2, 1, 0, 2, 1, 0, 1}, 3), 
            new TestCase(8, new int[]{4, 0, 0, 0, 0, 0, 0, 0, 4}, 2) // Large outer spans
        );
        
        System.out.println("--- Running Approach 1 (Greedy O(N)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.minTapsGreedy(tc.n(), tc.ranges());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (DP O(N^2)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.minTapsDP(tc.n(), tc.ranges());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
