import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: JUMP GAME II
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "What if the array has only one element?" 
 *      (Assumption: We are already at the last index, so 0 jumps are needed.)
 *    - "Are we guaranteed to always reach the last index?" 
 *      (Assumption: Yes, the problem statement explicitly guarantees this.)
 *    - "Can the array contain negative numbers?" 
 *      (Assumption: No, based on constraints 0 <= nums[i] <= 1000.)
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Find the MINIMUM number of jumps to reach the end.
 *    - Observation 1 (Why simple greedy fails): If we always just jump the 
 *      maximum possible distance from our current spot, we might land on a '0' 
 *      or a small number, missing a massive jump opportunity just one step prior.
 *      Example: [2, 3, 1, 1, 4]. Jumping max (2) lands on '1', then we need 
 *      2 more jumps. Total 3 jumps. But jumping to '3' lets us reach the end. 
 *      Total 2 jumps.
 *    - Observation 2 (The "Window" Strategy - BFS Intuition): 
 *      Instead of choosing exactly *where* to jump immediately, we keep track 
 *      of the entire "window" of indices we can reach with our current number 
 *      of jumps. 
 *    - As we iterate through this window, we calculate the absolute furthest 
 *      point we could reach if we took a jump from ANY of these indices. 
 *      Once we reach the end of our current window, we *must* make a jump, 
 *      and our new window ends at that furthest point we calculated.
 * 
 * 3. VISUAL EXPLANATION:
 *    Array: [2, 3, 1, 1, 4]
 *    
 *    Variables: jumps = 0, currentWindowEnd = 0, farthest = 0
 *    
 *    Index 0 (val=2): 
 *      - Farthest we can reach from here is index 0 + 2 = 2.
 *      - We reached the end of our current window (0). 
 *      - We MUST jump now. jumps = 1. New window ends at 2.
 *      
 *    Index 1 (val=3): 
 *      - Farthest from here is index 1 + 3 = 4. 
 *      - Farthest updates to max(2, 4) = 4.
 *      
 *    Index 2 (val=1): 
 *      - Farthest from here is index 2 + 1 = 3. 
 *      - We reached the end of our current window (2). 
 *      - We MUST jump now. jumps = 2. New window ends at 4.
 *      
 *    Since currentWindowEnd (4) >= last index (4), we are done!
 *    Result: 2 jumps.
 * 
 * ============================================================================
 */
public class JumpGameII {

    /**
     * APPROACH 1: Greedy BFS "Window" Strategy (Optimal)
     * 
     * Time Complexity: O(N) where N is the length of the array. We visit each element once.
     * Space Complexity: O(1) auxiliary space.
     */
    public int jumpOptimal(int[] nums) {
        // If there's only 1 element, we are already there.
        if (nums.length <= 1) return 0;
        
        int jumps = 0;
        int currentWindowEnd = 0;
        int farthestReach = 0;
        
        // We only iterate up to length - 2. 
        // If we process the last element, it might trigger an unnecessary extra jump.
        for (int i = 0; i < nums.length - 1; i++) {
            // Update the furthest we can reach from the current index
            farthestReach = Math.max(farthestReach, i + nums[i]);
            
            // If we have reached the end of the range of our current jump
            if (i == currentWindowEnd) {
                jumps++; // We are forced to make a jump to continue
                currentWindowEnd = farthestReach; // The new range of our next jump
                
                // Early exit optimization: If our new window covers the end, stop.
                if (currentWindowEnd >= nums.length - 1) {
                    break;
                }
            }
        }
        
        return jumps;
    }

    /**
     * APPROACH 2: Dynamic Programming (Alternative / Brute Forceish)
     * 
     * In an interview, it is good to mention this first to show you know how 
     * to solve it systematically, before optimizing to the O(N) greedy solution.
     * 
     * Time Complexity: O(N^2) because for every element, we might check up to N elements ahead.
     * Space Complexity: O(N) for the DP array.
     */
    public int jumpDP(int[] nums) {
        int n = nums.length;
        // dp[i] will store the minimum jumps to reach index i
        int[] dp = new int[n]; 
        
        // Initialize DP array with an arbitrarily large number
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0; // 0 jumps to reach the start
        
        for (int i = 0; i < n; i++) {
            // If we can't even reach index i, we definitely can't jump from it.
            // (Not strictly necessary given the prompt's guarantee, but good practice).
            if (dp[i] == Integer.MAX_VALUE) continue;
            
            // Try jumping to all possible reachable indices from 'i'
            int maxJump = Math.min(n - 1, i + nums[i]);
            for (int j = i + 1; j <= maxJump; j++) {
                // The min jumps to reach 'j' is the minimum of its current known jumps, 
                // or taking 1 jump from 'i'
                dp[j] = Math.min(dp[j], dp[i] + 1);
            }
        }
        
        return dp[n - 1];
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] nums, int expected) {}

    public static void main(String[] args) {
        JumpGameII solver = new JumpGameII();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[]{2, 3, 1, 1, 4}, 2),
            new TestCase(new int[]{2, 3, 0, 1, 4}, 2),
            new TestCase(new int[]{0}, 0),                  // Edge case: already at end
            new TestCase(new int[]{1, 2, 3}, 2),
            new TestCase(new int[]{7, 0, 9, 6, 9, 6, 1, 7, 9, 0, 1, 2, 9, 0, 3}, 2) 
        );
        
        System.out.println("--- Running Approach 1 (Greedy O(N)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.jumpOptimal(tc.nums());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (DP O(N^2)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.jumpDP(tc.nums());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
