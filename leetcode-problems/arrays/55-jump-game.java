/*
 * You are given an integer array nums. You are initially positioned at the
 * array's first index, and each element in the array represents your maximum
 * jump length at that position.
 * 
 * Return true if you can reach the last index, or false otherwise.
 * 
 * Example 1:
 * Input: nums = [2,3,1,1,4]
 * Output: true
 * Explanation: Jump 1 step from index 0 to 1, then 3 steps to the last index.
 * 
 * Example 2:
 * Input: nums = [3,2,1,0,4]
 * Output: false
 * Explanation: You will always arrive at index 3 no matter what. Its maximum
 * jump length is 0, which makes it impossible to reach the last index.
 */

class JumpGame {

    // Solution 1: Greedy - Track farthest reachable position (Optimal)
    // Time: O(n), Space: O(1)
    public boolean canJump1(int[] nums) {
        if (nums == null || nums.length == 0) {
            return false;
        }

        int maxReach = 0; // Farthest index we can reach

        for (int i = 0; i < nums.length; i++) {
            // If current position is beyond our reach, we can't proceed
            if (i > maxReach) {
                return false;
            }

            // Update the farthest we can reach from current position
            maxReach = Math.max(maxReach, i + nums[i]);

            // Early termination: if we can reach the end
            if (maxReach >= nums.length - 1) {
                return true;
            }
        }

        return true;
    }

    // Solution 2: Greedy - Work backwards from end
    // Time: O(n), Space: O(1)
    public boolean canJump2(int[] nums) {
        if (nums == null || nums.length == 0) {
            return false;
        }

        int lastGoodIndex = nums.length - 1;

        // Work backwards from the end
        for (int i = nums.length - 2; i >= 0; i--) {
            // If we can jump from current position to last good index
            if (i + nums[i] >= lastGoodIndex) {
                lastGoodIndex = i;
            }
        }

        return lastGoodIndex == 0;
    }

    // Solution 3: Dynamic Programming - Bottom Up
    // Time: O(n²), Space: O(n)
    public boolean canJump3(int[] nums) {
        if (nums == null || nums.length == 0) {
            return false;
        }

        boolean[] canReach = new boolean[nums.length];
        canReach[0] = true; // We start at index 0

        for (int i = 1; i < nums.length; i++) {
            for (int j = 0; j < i; j++) {
                // If we can reach position j and from j we can reach i
                if (canReach[j] && j + nums[j] >= i) {
                    canReach[i] = true;
                    break; // No need to check further
                }
            }
        }

        return canReach[nums.length - 1];
    }

    // Solution 4: Recursive with Memoization (Top Down DP)
    // Time: O(n²), Space: O(n)
    public boolean canJump4(int[] nums) {
        if (nums == null || nums.length == 0) {
            return false;
        }

        Boolean[] memo = new Boolean[nums.length];
        return canJumpHelper(nums, 0, memo);
    }

    private boolean canJumpHelper(int[] nums, int index, Boolean[] memo) {
        // Base case: reached the end
        if (index >= nums.length - 1) {
            return true;
        }

        // Check memoization
        if (memo[index] != null) {
            return memo[index];
        }

        // Try all possible jumps from current position
        for (int jump = 1; jump <= nums[index]; jump++) {
            if (canJumpHelper(nums, index + jump, memo)) {
                memo[index] = true;
                return true;
            }
        }

        memo[index] = false;
        return false;
    }

    // Solution 5: BFS approach
    // Time: O(n), Space: O(n)
    public boolean canJump5(int[] nums) {
        if (nums == null || nums.length == 0) {
            return false;
        }

        if (nums.length == 1) {
            return true;
        }

        boolean[] visited = new boolean[nums.length];
        java.util.Queue<Integer> queue = new java.util.LinkedList<>();

        queue.offer(0); // Start from index 0
        visited[0] = true;

        while (!queue.isEmpty()) {
            int current = queue.poll();

            // Try all possible jumps from current position
            for (int jump = 1; jump <= nums[current]; jump++) {
                int next = current + jump;

                // If we can reach the end
                if (next >= nums.length - 1) {
                    return true;
                }

                // Add unvisited positions to queue
                if (!visited[next]) {
                    visited[next] = true;
                    queue.offer(next);
                }
            }
        }

        return false;
    }

    // Helper method to show step-by-step analysis
    public boolean canJumpWithExplanation(int[] nums) {
        System.out.println("Array: " + java.util.Arrays.toString(nums));
        System.out.println("Step-by-step analysis:");

        int maxReach = 0;

        for (int i = 0; i < nums.length; i++) {
            if (i > maxReach) {
                System.out.println("Position " + i + " is unreachable (maxReach = " + maxReach + ")");
                return false;
            }

            int newReach = i + nums[i];
            maxReach = Math.max(maxReach, newReach);

            System.out.println("Position " + i + ": value = " + nums[i] +
                    ", can reach up to " + newReach +
                    ", overall maxReach = " + maxReach);

            if (maxReach >= nums.length - 1) {
                System.out.println("Can reach the end!");
                return true;
            }
        }

        return true;
    }

    // Method to find one possible path
    public void findPath(int[] nums) {
        if (!canJump1(nums)) {
            System.out.println("No path exists");
            return;
        }

        System.out.println("Finding a path:");
        java.util.List<Integer> path = new java.util.ArrayList<>();
        int current = 0;
        path.add(current);

        while (current < nums.length - 1) {
            // Find the next position that gives maximum reach
            int bestNext = current + 1;
            int maxFutureReach = bestNext + nums[bestNext];

            for (int jump = 1; jump <= nums[current]; jump++) {
                int next = current + jump;
                if (next >= nums.length - 1) {
                    path.add(nums.length - 1);
                    break;
                }

                int futureReach = next + nums[next];
                if (futureReach > maxFutureReach) {
                    maxFutureReach = futureReach;
                    bestNext = next;
                }
            }

            current = bestNext;
            path.add(current);

            if (current >= nums.length - 1) {
                break;
            }
        }

        System.out.println("Path: " + path);
    }

    public static void main(String[] args) {
        JumpGame solution = new JumpGame();

        // Test case 1
        int[] nums1 = { 2, 3, 1, 1, 4 };
        System.out.println("Example 1: " + java.util.Arrays.toString(nums1));
        System.out.println("Can jump: " + solution.canJump1(nums1));
        solution.canJumpWithExplanation(nums1);
        solution.findPath(nums1);
        System.out.println();

        // Test case 2
        int[] nums2 = { 3, 2, 1, 0, 4 };
        System.out.println("Example 2: " + java.util.Arrays.toString(nums2));
        System.out.println("Can jump: " + solution.canJump1(nums2));
        solution.canJumpWithExplanation(nums2);
        System.out.println();

        // Edge cases
        int[] nums3 = { 0 };
        System.out.println("Single element [0]: " + solution.canJump1(nums3));

        int[] nums4 = { 1 };
        System.out.println("Single element [1]: " + solution.canJump1(nums4));

        int[] nums5 = { 2, 0, 0 };
        System.out.println("Can skip zeros: " + solution.canJump1(nums5));

        int[] nums6 = { 1, 0, 1, 0 };
        System.out.println("Cannot skip zeros: " + solution.canJump1(nums6));

        // Verify all solutions give same result
        System.out.println("\nVerifying all solutions on [2,3,1,1,4]:");
        int[] test = { 2, 3, 1, 1, 4 };
        System.out.println("Greedy Forward: " + solution.canJump1(test));
        System.out.println("Greedy Backward: " + solution.canJump2(test));
        System.out.println("DP Bottom-Up: " + solution.canJump3(test));
        System.out.println("DP Top-Down: " + solution.canJump4(test));
        System.out.println("BFS: " + solution.canJump5(test));

        // Performance comparison
        int[] largeProblem = new int[10000];
        for (int i = 0; i < largeProblem.length; i++) {
            largeProblem[i] = Math.max(1, (int) (Math.random() * 3));
        }

        long start = System.nanoTime();
        boolean result1 = solution.canJump1(largeProblem);
        long time1 = System.nanoTime() - start;

        start = System.nanoTime();
        boolean result3 = solution.canJump3(largeProblem);
        long time3 = System.nanoTime() - start;

        System.out.println("\nPerformance comparison (10,000 elements):");
        System.out.println("Greedy O(n): " + time1 + " ns, Result: " + result1);
        System.out.println("DP O(n²): " + time3 + " ns, Result: " + result3);
        System.out.println("Speedup: " + (time3 / (double) time1) + "x");
    }

}
