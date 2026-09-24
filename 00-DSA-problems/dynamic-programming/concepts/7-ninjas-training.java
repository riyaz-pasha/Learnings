/*
 * Problem Statement: A Ninja has an ‘N’ Day training schedule. He has to
 * perform one of these three activities (Running, Fighting Practice, or
 * Learning New Moves) each day. There are merit points associated with
 * performing an activity each day. The same activity can’t be performed on two
 * consecutive days. We need to find the maximum merit points the ninja can
 * attain in N Days.
 * 
 * We are given a 2D Array POINTS of size ‘N*3’ which tells us the merit point
 * of specific activity on that particular day. Our task is to calculate the
 * maximum number of merit points that the ninja can earn.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class NinjasTrainingRecursion {

    public int findMaxPoints(int n, int[][] activities) {
        // on each day i have three choices to make
        // either pick running/fighting/newMoves
        // same activity can not be picked next day
        //
        int max = Integer.MIN_VALUE;
        for (int activity = 0; activity < 3; activity++) {
            max = Math.max(max, activities[n - 1][activity] + this.findMaxPoints(activities, n - 2, activity));
        }
        return max;
    }

    private int findMaxPoints(int[][] activities, int currentDay, int lastPickedActivity) {
        if (currentDay < 0) {
            return 0;
        }

        int max = Integer.MIN_VALUE;
        for (int activity = 0; activity < 3; activity++) {
            if (activity == lastPickedActivity)
                continue;
            max = Math.max(max, activities[n - 1][activity] + this.findMaxPoints(activities, currentDay - 1, activity));
        }
        return max;
    }

}

class NinjasTrainingMemo {

    public int findMaxPoints(int n, int[][] activities) {
        int max = Integer.MIN_VALUE;
        Integer[][] memo = new Integer[n][3];
        for (int activity = 0; activity < 3; activity++) {
            max = Math.max(max, activities[n - 1][activity] + this.findMaxPoints(activities, memo, n - 2, activity));
        }
        return max;
    }

    private int findMaxPoints(int[][] activities, Integer[][] memo, int currentDay, int lastPickedActivity) {
        if (currentDay < 0) {
            return 0;
        }

        if (memo[currentDay][lastPickedActivity] != null) {
            return memo[currentDay][lastPickedActivity];
        }

        int max = Integer.MIN_VALUE;
        for (int activity = 0; activity < 3; activity++) {
            if (activity == lastPickedActivity)
                continue;
            max = Math.max(
                    max,
                    activities[currentDay][activity] + this.findMaxPoints(activities, memo, currentDay - 1, activity));
        }
        return memo[currentDay][lastPickedActivity] = max;
    }

}

class NinjaTraining {

    // Solution 1: 2D Dynamic Programming (Most Intuitive)
    public static int ninjaTraining2D(int[][] points) {
        if (points == null || points.length == 0)
            return 0;

        int n = points.length;
        int[][] dp = new int[n][3];

        // Base case: First day
        dp[0][0] = points[0][0]; // Running
        dp[0][1] = points[0][1]; // Fighting
        dp[0][2] = points[0][2]; // Learning

        // Fill the DP table
        for (int day = 1; day < n; day++) {
            // For each activity today, take max from other activities yesterday
            dp[day][0] = points[day][0] + Math.max(dp[day - 1][1], dp[day - 1][2]);
            dp[day][1] = points[day][1] + Math.max(dp[day - 1][0], dp[day - 1][2]);
            dp[day][2] = points[day][2] + Math.max(dp[day - 1][0], dp[day - 1][1]);
        }

        // Return maximum points on the last day
        return Math.max(dp[n - 1][0], Math.max(dp[n - 1][1], dp[n - 1][2]));
    }

    // Solution 2: Space Optimized DP (O(1) space)
    public static int ninjaTrainingOptimized(int[][] points) {
        if (points == null || points.length == 0)
            return 0;

        int n = points.length;

        // Previous day's maximum points for each activity
        int prev0 = points[0][0]; // Running
        int prev1 = points[0][1]; // Fighting
        int prev2 = points[0][2]; // Learning

        for (int day = 1; day < n; day++) {
            int curr0 = points[day][0] + Math.max(prev1, prev2);
            int curr1 = points[day][1] + Math.max(prev0, prev2);
            int curr2 = points[day][2] + Math.max(prev0, prev1);

            // Update previous values for next iteration
            prev0 = curr0;
            prev1 = curr1;
            prev2 = curr2;
        }

        return Math.max(prev0, Math.max(prev1, prev2));
    }

    // Solution 3: Recursive with Memoization
    public static int ninjaTrainingRecursive(int[][] points) {
        if (points == null || points.length == 0)
            return 0;

        int n = points.length;
        Integer[][] memo = new Integer[n][4]; // 4 because lastActivity can be 0,1,2,3

        return solve(points, 0, 3, memo); // Start with day 0, lastActivity = 3 (no activity)
    }

    private static int solve(int[][] points, int day, int lastActivity, Integer[][] memo) {
        // Base case: No more days
        if (day >= points.length)
            return 0;

        if (memo[day][lastActivity] != null) {
            return memo[day][lastActivity];
        }

        int maxPoints = 0;

        // Try all three activities (0: Running, 1: Fighting, 2: Learning)
        for (int activity = 0; activity < 3; activity++) {
            if (activity != lastActivity) { // Can't repeat the same activity
                int currentPoints = points[day][activity] + solve(points, day + 1, activity, memo);
                maxPoints = Math.max(maxPoints, currentPoints);
            }
        }

        memo[day][lastActivity] = maxPoints;
        return maxPoints;
    }

    // Solution 4: Alternative DP approach with activity tracking
    public static int ninjaTrainingWithTracking(int[][] points) {
        if (points == null || points.length == 0)
            return 0;

        int n = points.length;
        // dp[i][j] = max points till day i with last activity j
        int[][] dp = new int[n][4]; // 4th column for "no activity" (initial state)

        // Initialize first day
        dp[0][0] = points[0][0];
        dp[0][1] = points[0][1];
        dp[0][2] = points[0][2];
        dp[0][3] = 0; // No activity on day 0

        for (int day = 1; day < n; day++) {
            for (int lastActivity = 0; lastActivity < 4; lastActivity++) {
                for (int currActivity = 0; currActivity < 3; currActivity++) {
                    if (currActivity != lastActivity) {
                        dp[day][currActivity] = Math.max(dp[day][currActivity],
                                dp[day - 1][lastActivity] + points[day][currActivity]);
                    }
                }
            }
        }

        return Math.max(dp[n - 1][0], Math.max(dp[n - 1][1], dp[n - 1][2]));
    }

    // Method to get the actual activity sequence (for demonstration)
    public static List<String> getActivitySequence(int[][] points) {
        if (points == null || points.length == 0)
            return new ArrayList<>();

        String[] activities = { "Running", "Fighting", "Learning" };
        int n = points.length;
        int[][] dp = new int[n][3];
        int[][] choice = new int[n][3]; // To track choices

        // Base case
        dp[0][0] = points[0][0];
        dp[0][1] = points[0][1];
        dp[0][2] = points[0][2];

        // Fill DP table and track choices
        for (int day = 1; day < n; day++) {
            // Running today
            if (dp[day - 1][1] > dp[day - 1][2]) {
                dp[day][0] = points[day][0] + dp[day - 1][1];
                choice[day][0] = 1; // Previous activity was Fighting
            } else {
                dp[day][0] = points[day][0] + dp[day - 1][2];
                choice[day][0] = 2; // Previous activity was Learning
            }

            // Fighting today
            if (dp[day - 1][0] > dp[day - 1][2]) {
                dp[day][1] = points[day][1] + dp[day - 1][0];
                choice[day][1] = 0; // Previous activity was Running
            } else {
                dp[day][1] = points[day][1] + dp[day - 1][2];
                choice[day][1] = 2; // Previous activity was Learning
            }

            // Learning today
            if (dp[day - 1][0] > dp[day - 1][1]) {
                dp[day][2] = points[day][2] + dp[day - 1][0];
                choice[day][2] = 0; // Previous activity was Running
            } else {
                dp[day][2] = points[day][2] + dp[day - 1][1];
                choice[day][2] = 1; // Previous activity was Fighting
            }
        }

        // Find the best last activity
        int lastActivity = 0;
        if (dp[n - 1][1] > dp[n - 1][0])
            lastActivity = 1;
        if (dp[n - 1][2] > dp[n - 1][lastActivity])
            lastActivity = 2;

        // Backtrack to build sequence
        List<String> sequence = new ArrayList<>();
        int currentActivity = lastActivity;

        for (int day = n - 1; day >= 0; day--) {
            sequence.add(activities[currentActivity]);
            if (day > 0) {
                currentActivity = choice[day][currentActivity];
            }
        }

        Collections.reverse(sequence);
        return sequence;
    }

    public static void main(String[] args) {
        // Test cases
        int[][][] testCases = {
                { { 1, 2, 5 }, { 3, 1, 1 }, { 3, 3, 3 } }, // Expected: 11
                { { 10, 40, 70 }, { 20, 50, 80 }, { 30, 60, 90 } }, // Expected: 210
                { { 10, 50, 1 }, { 5, 100, 11 } }, // Expected: 110
                { { 1, 2, 3 } }, // Expected: 3 (single day)
                { { 5, 1, 3 }, { 2, 4, 6 }, { 7, 8, 2 } }, // Expected: 17
        };

        System.out.println("Ninja Training - Maximum Merit Points Solutions:");
        System.out.println("=".repeat(60));

        for (int i = 0; i < testCases.length; i++) {
            int[][] points = testCases[i];
            System.out.println("Test Case " + (i + 1) + ":");
            System.out.println("Points Matrix:");
            for (int[] day : points) {
                System.out.println(Arrays.toString(day));
            }

            int result1 = ninjaTraining2D(points);
            int result2 = ninjaTrainingOptimized(points);
            int result3 = ninjaTrainingRecursive(points);
            int result4 = ninjaTrainingWithTracking(points);
            List<String> sequence = getActivitySequence(points);

            System.out.println("2D DP Solution: " + result1);
            System.out.println("Optimized DP: " + result2);
            System.out.println("Recursive + Memo: " + result3);
            System.out.println("Alternative DP: " + result4);
            System.out.println("Activity Sequence: " + sequence);

            // Verify all solutions match
            if (result1 == result2 && result2 == result3 && result3 == result4) {
                System.out.println("✓ All solutions match!");
            } else {
                System.out.println("✗ Solutions don't match!");
            }
            System.out.println("-".repeat(40));
        }

        // Edge cases
        System.out.println("\nEdge Cases:");
        System.out.println("Empty matrix: " + ninjaTrainingOptimized(new int[][] {}));
        System.out.println("Null matrix: " + ninjaTrainingOptimized(null));

        // Performance test
        System.out.println("\nPerformance Test (1000 days):");
        int[][] largeMatrix = new int[1000][3];
        Random rand = new Random(42);

        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 3; j++) {
                largeMatrix[i][j] = rand.nextInt(100) + 1;
            }
        }

        long start = System.nanoTime();
        int result = ninjaTrainingOptimized(largeMatrix);
        long end = System.nanoTime();

        System.out.println("Result: " + result);
        System.out.println("Time taken: " + (end - start) / 1_000_000.0 + " ms");

        // Detailed example walkthrough
        System.out.println("\n" + "=".repeat(50));
        System.out.println("DETAILED EXAMPLE WALKTHROUGH:");
        System.out.println("=".repeat(50));

        int[][] example = { { 1, 2, 5 }, { 3, 1, 1 }, { 3, 3, 3 } };
        System.out.println("Points Matrix:");
        System.out.println("Day 0: Running=1, Fighting=2, Learning=5");
        System.out.println("Day 1: Running=3, Fighting=1, Learning=1");
        System.out.println("Day 2: Running=3, Fighting=3, Learning=3");

        System.out.println("\nOptimal Solution:");
        System.out.println("Day 0: Choose Learning (5 points)");
        System.out.println("Day 1: Choose Running (3 points) - can't choose Learning");
        System.out.println("Day 2: Choose Fighting/Learning (3 points) - can't choose Running");
        System.out.println("Total: 5 + 3 + 3 = 11 points");

        List<String> seq = getActivitySequence(example);
        System.out.println("Activity Sequence: " + seq);
    }
}

/*
 * Algorithm Explanation:
 * 
 * The key insight is that on each day, the ninja has 3 choices, but cannot
 * repeat
 * the same activity as the previous day.
 * 
 * State Definition:
 * dp[i][j] = maximum points achievable till day i with last activity j
 * where j = 0 (Running), 1 (Fighting), 2 (Learning)
 * 
 * Recurrence Relation:
 * dp[i][0] = points[i][0] + max(dp[i-1][1], dp[i-1][2])
 * dp[i][1] = points[i][1] + max(dp[i-1][0], dp[i-1][2])
 * dp[i][2] = points[i][2] + max(dp[i-1][0], dp[i-1][1])
 * 
 * Time Complexity: O(n) where n is number of days
 * Space Complexity:
 * - 2D DP: O(n)
 * - Optimized: O(1)
 * - Recursive: O(n) due to memoization
 * 
 * This is a classic example of DP with constraints where the current choice
 * depends on the previous choice.
 */
