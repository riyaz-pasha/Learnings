import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: JUMP GAME
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "What if the array has only one element?" 
 *      (Assumption: We are already at the last index, so return true.)
 *    - "Can the array contain negative numbers?" 
 *      (Assumption: No, based on constraints 0 <= nums[i] <= 1000.)
 *    - "Do I need to find the minimum number of jumps, or just if it's possible?"
 *      (Assumption: Just boolean true/false if it is possible.)
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Observation 1 (Why DP/Recursion is overkill): You could explore every 
 *      possible jump using recursion, but that leads to O(2^N) time. Adding 
 *      memoization brings it to O(N^2). We can do much better: O(N).
 *    - Observation 2 (The Forward Greedy Strategy): As we iterate through the 
 *      array, we only care about the *furthest* index we can currently reach. 
 *      If we are at index 'i', and 'i' is greater than our furthest reach, 
 *      it means we are stuck at a 0 somewhere behind us.
 *    - Observation 3 (The Backward Greedy Strategy): Alternatively, look from 
 *      the destination. Can the second-to-last step reach the end? If yes, 
 *      the second-to-last step becomes our NEW destination. If we can shift 
 *      the destination all the way to index 0, it's solvable.
 * 
 * 3. VISUAL EXPLANATION (Forward Greedy):
 *    Array: [2, 3, 1, 1, 4]
 *    
 *    Index 0 (val=2): maxReach = max(0, 0 + 2) = 2. (Can reach up to index 2)
 *    Index 1 (val=3): maxReach = max(2, 1 + 3) = 4. (Can reach up to index 4)
 *    Index 2 (val=1): maxReach = max(4, 2 + 1) = 4.
 *    Since maxReach (4) >= last index (4), we return TRUE!
 * 
 *    Array: [3, 2, 1, 0, 4]
 *    
 *    Index 0 (val=3): maxReach = max(0, 0 + 3) = 3.
 *    Index 1 (val=2): maxReach = max(3, 1 + 2) = 3.
 *    Index 2 (val=1): maxReach = max(3, 2 + 1) = 3.
 *    Index 3 (val=0): maxReach = max(3, 3 + 0) = 3.
 *    Index 4 (val=4): Wait! Index 4 > maxReach (3). We can't even get here!
 *    Return FALSE.
 * 
 * ============================================================================
 */
class JumpGame {

    /**
     * APPROACH 1: Forward Greedy (Most Intuitive)
     * 
     * Time Complexity: O(N) where N is the length of the array. We visit each element once.
     * Space Complexity: O(1) auxiliary space.
     */
    public boolean canJumpForward(int[] nums) {
        int maxReach = 0;
        
        for (int i = 0; i < nums.length; i++) {
            // If our current index is beyond the maximum reachable index, we are stuck.
            if (i > maxReach) {
                return false;
            }
            
            // Update the maximum reachable index
            maxReach = Math.max(maxReach, i + nums[i]);
            
            // Early exit optimization: If we can already reach the end, stop iterating.
            if (maxReach >= nums.length - 1) {
                return true;
            }
        }
        
        return true;
    }

    /**
     * APPROACH 2: Backward Greedy (Very Elegant)
     * 
     * In an interview, writing this backward approach shows exceptional algorithmic 
     * thinking. Instead of projecting forward, we pull the goalpost closer to the start.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     */
    public boolean canJumpBackward(int[] nums) {
        // Our initial target is the last index
        int targetIndex = nums.length - 1;
        
        // Work backward from the second-to-last element down to index 0
        for (int i = nums.length - 2; i >= 0; i--) {
            // If jumping from this index can reach or surpass the target...
            if (i + nums[i] >= targetIndex) {
                // ...then we only need to figure out how to reach THIS index.
                // Shift the goalpost!
                targetIndex = i;
            }
        }
        
        // If we successfully shifted the goalpost all the way to the start, it's possible.
        return targetIndex == 0;
    }

    public static boolean canJumpDP(int[] nums) {
        Boolean[] dp = new Boolean[nums.length];
        return dfs(0, nums, dp);
    }

    private static boolean dfs(int index, int[] nums, Boolean[] dp) {

        // 🎯 reached last index
        if (index >= nums.length - 1) return true;

        if (dp[index] != null) return dp[index];

        int maxJump = nums[index];

        // Try all possible jumps
        for (int step = 1; step <= maxJump; step++) {
            if (dfs(index + step, nums, dp)) {
                return dp[index] = true;
            }
        }

        return dp[index] = false;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] nums, boolean expected) {}

    public static void main(String[] args) {
        JumpGame solver = new JumpGame();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[]{2, 3, 1, 1, 4}, true),
            new TestCase(new int[]{3, 2, 1, 0, 4}, false),
            new TestCase(new int[]{0}, true),          // Edge case: length 1
            new TestCase(new int[]{2, 0, 0}, true),    // Exact jump to end
            new TestCase(new int[]{1, 0, 1, 0}, false) // Multiple zeroes
        );
        
        System.out.println("--- Running Approach 1 (Forward Greedy) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            boolean result = solver.canJumpForward(tc.nums());
            System.out.printf("Test %d: Expected = %b, Got = %b -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Backward Greedy) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            boolean result = solver.canJumpBackward(tc.nums());
            System.out.printf("Test %d: Expected = %b, Got = %b -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
