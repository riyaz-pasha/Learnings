import java.util.PriorityQueue;
/*
 * You are given an integer array heights representing the heights of buildings,
 * some bricks, and some ladders.
 * 
 * You start your journey from building 0 and move to the next building by
 * possibly using bricks or ladders.
 * 
 * While moving from building i to building i+1 (0-indexed),
 * 
 * If the current building's height is greater than or equal to the next
 * building's height, you do not need a ladder or bricks.
 * If the current building's height is less than the next building's height, you
 * can either use one ladder or (h[i+1] - h[i]) bricks.
 * Return the furthest building index (0-indexed) you can reach if you use the
 * given ladders and bricks optimally.
 * 
 * Example 1:
 * Input: heights = [4,2,7,6,9,14,12], bricks = 5, ladders = 1
 * Output: 4
 * Explanation: Starting at building 0, you can follow these steps:
 * - Go to building 1 without using ladders nor bricks since 4 >= 2.
 * - Go to building 2 using 5 bricks. You must use either bricks or ladders
 * because 2 < 7.
 * - Go to building 3 without using ladders nor bricks since 7 >= 6.
 * - Go to building 4 using your only ladder. You must use either bricks or
 * ladders because 6 < 9.
 * It is impossible to go beyond building 4 because you do not have any more
 * bricks or ladders.
 * 
 * Example 2:
 * Input: heights = [4,12,2,7,3,18,20,3,19], bricks = 10, ladders = 2
 * Output: 7
 * 
 * Example 3:
 * Input: heights = [14,3,19,3], bricks = 17, ladders = 0
 * Output: 3
 */

class Solution {

    public int furthestBuilding(int[] heights, int bricks, int ladders) {
        // Min heap to store the largest climbs where we use ladders
        PriorityQueue<Integer> ladderClimbs = new PriorityQueue<>();

        for (int i = 0; i < heights.length - 1; i++) {
            int climb = heights[i + 1] - heights[i];

            // If going down or staying same level, no resources needed
            if (climb <= 0) {
                continue;
            }

            // Strategy: Always try to use ladder first for current climb
            ladderClimbs.offer(climb);

            // If we've used more ladders than we have
            if (ladderClimbs.size() > ladders) {
                // Remove the smallest climb from ladder usage (use bricks instead)
                int smallestLadderClimb = ladderClimbs.poll();
                bricks -= smallestLadderClimb;

                // If we don't have enough bricks, we can't proceed
                if (bricks < 0) {
                    return i;
                }
            }
        }

        // If we complete the loop, we reached the last building
        return heights.length - 1;
    }

}

/*
 * ALTERNATIVE APPROACH - Binary Search + Greedy:
 * 
 * class Solution {
 * public int furthestBuilding(int[] heights, int bricks, int ladders) {
 * int left = 0, right = heights.length - 1;
 * int result = 0;
 * 
 * while (left <= right) {
 * int mid = left + (right - left) / 2;
 * 
 * if (canReach(heights, bricks, ladders, mid)) {
 * result = mid;
 * left = mid + 1;
 * } else {
 * right = mid - 1;
 * }
 * }
 * 
 * return result;
 * }
 * 
 * private boolean canReach(int[] heights, int bricks, int ladders, int target)
 * {
 * List<Integer> climbs = new ArrayList<>();
 * 
 * // Collect all positive climbs up to target
 * for (int i = 0; i < target; i++) {
 * int climb = heights[i + 1] - heights[i];
 * if (climb > 0) {
 * climbs.add(climb);
 * }
 * }
 * 
 * // Sort climbs in descending order (use ladders for largest climbs)
 * Collections.sort(climbs, Collections.reverseOrder());
 * 
 * // Use ladders for largest climbs
 * int bricksUsed = 0;
 * for (int i = 0; i < climbs.size(); i++) {
 * if (i < ladders) {
 * // Use ladder for this climb
 * continue;
 * } else {
 * // Use bricks for this climb
 * bricksUsed += climbs.get(i);
 * if (bricksUsed > bricks) {
 * return false;
 * }
 * }
 * }
 * 
 * return true;
 * }
 * }
 * 
 * Time Complexity Analysis:
 * 
 * Approach 1 (Min Heap):
 * - Time: O(n log k) where n = heights.length, k = ladders
 * - We iterate through n-1 buildings
 * - Each heap operation takes O(log k) time
 * - Heap size is at most ladders + 1
 * - Space: O(k) for the priority queue
 * 
 * Approach 2 (Binary Search):
 * - Time: O(n^2 log n) in worst case
 * - Binary search: O(log n) iterations
 * - Each canReach call: O(n log n) for sorting climbs
 * - Space: O(n) for storing climbs
 * 
 * The Min Heap approach is more efficient and is the preferred solution.
 * 
 * Key Insights:
 * 
 * 1. **Greedy Strategy**: Always use ladders for the largest height differences
 * - This minimizes brick usage and maximizes reach
 * 
 * 2. **Min Heap Logic**:
 * - Keep track of climbs where we use ladders
 * - When we exceed ladder count, replace the smallest ladder climb with bricks
 * - This ensures ladders are always used for the largest climbs
 * 
 * 3. **Edge Cases**:
 * - Going down or staying level requires no resources
 * - Running out of bricks stops us immediately
 * - If we can complete the entire array, return last index
 * 
 * Example Walkthrough for [4,2,7,6,9,14,12], bricks=5, ladders=1:
 * 
 * i=0: 4->2, no climb needed
 * i=1: 2->7, climb=5, ladderClimbs=[5], size=1 ≤ ladders=1 ✓
 * i=2: 7->6, no climb needed
 * i=3: 6->9, climb=3, ladderClimbs=[3,5], size=2 > ladders=1
 * Remove min climb=3, use bricks: bricks=5-3=2 ✓
 * i=4: 9->14, climb=5, ladderClimbs=[5], size=1 ≤ ladders=1 ✓
 * i=5: 14->12, no climb needed
 * But wait, let me recalculate...
 * 
 * Actually at i=4: 9->14, climb=5
 * ladderClimbs=[5] (from i=1), add 5: ladderClimbs=[5,5], size=2 > ladders=1
 * Remove min=5, bricks=2-5=-3 < 0, return i=4 ✓
 * 
 * This matches the expected output of 4.
 */