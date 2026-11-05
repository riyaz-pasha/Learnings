/**
 * 699. Falling Squares
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * There are several squares being dropped onto the X-axis of a 2D plane.
 * You are given a 2D integer array positions where positions[i] = [left_i, sideLength_i] 
 * represents the ith square dropped with its left edge aligned with left_i.
 * 
 * Each square falls downwards until it lands on the top of another square or on 
 * the X-axis. A square "brushing" the left/right side of another square does not 
 * count as landing on it. Once it lands, it freezes in place.
 * 
 * Return an integer array ans where ans[i] represents the height of the current 
 * tallest stack of squares after dropping the ith square.
 * 
 * Constraints:
 * - 1 <= positions.length <= 1000
 * - 1 <= left_i <= 10^8
 * - 1 <= sideLength_i <= 10^6
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. If square A ends exactly at X and square B starts exactly at X, do they stack?
 *    (No, the problem states "brushing" doesn't count. We need strict intersection).
 * 2. What is the maximum number of squares (N) dropped?
 *    (Constraints say 1000. This is a massive hint that an O(N^2) approach is perfectly 
 *    acceptable and usually preferred for its simplicity over complex tree structures).
 * 3. How large can the X coordinates get?
 *    (Up to 10^8. This confirms we cannot use a flat array to represent the X-axis, 
 *    as it would cause an OutOfMemoryError).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * For each dropped square, we need to find the maximum height among all previously 
 * dropped squares that it overlaps with. The coordinate space is huge (10^8), but 
 * the number of dropped squares is tiny (N = 1000).
 * 
 * --- Approach 1: Flat Array (Mental Sandbox) ---
 * 1. Idea: Create an array `heights[10^8]`. For each square `[left, right]`, find the 
 *    max value in `heights[left...right-1]`, add `sideLength`, and update that range.
 * 2. Why it fails: Allocating an array of 10^8 integers takes ~400MB, which might OOM. 
 *    Even if it doesn't, iterating millions of indices per square will Time Limit Exceed.
 * 
 * --- Approach 2: List of Intervals (Optimal for N = 1000) ---
 * 1. Idea: Since N is only 1000, there are at most 1000 previous squares to check. 
 *    We can just keep a list of `[left, right, height]` for every square that has landed.
 * 2. What I'd naturally try: For a new square `[L, R]`, loop through all previously 
 *    recorded squares. If a previous square strictly overlaps with `[L, R]`, note its height. 
 *    The new base height is the maximum of these overlapping heights.
 * 3. Why it works: Strict overlap occurs if `max(L1, L2) < min(R1, R2)`. We just check 
 *    up to N previous squares for each drop. 
 * 4. Why this is the ceiling for an interview: For N=1000, N^2/2 operations is ~500,000, 
 *    which runs in ~2 milliseconds in Java. It avoids the immense bug-risk of writing 
 *    advanced data structures under pressure.
 * 
 * Time Complexity: O(N^2) — checking each square against all previously dropped ones.
 * Space Complexity: O(N) — storing the history of the N dropped squares.
 * 
 * --- Approach 3: Coordinate Compression + Segment Tree (Advanced Scalability) ---
 * 1. Idea: What if N was 100,000? O(N^2) would fail. We would need a Range Max Query data structure.
 * 2. What I'd naturally try: We compress all `left` and `right` coordinates into a mapped 
 *    range of `[0, 2N]`. We build a Segment Tree with Lazy Propagation over this compressed 
 *    array. For each drop, we query the range max in O(log N) and lazily update it.
 * 3. Why it works: It drops the check time from O(N) per drop to O(log N).
 * 
 * Time Complexity: O(N log N) — due to sorting coordinates and querying the Segment Tree.
 * Space Complexity: O(N) — the Segment Tree requires space proportional to unique coordinates.
 * 
 * Verdict for 45-minute interview:
 * Start with and write Approach 2 (List of Intervals). It is concise, directly addresses 
 * the constraints, and perfectly captures the overlap logic. Discuss Approach 3 as a 
 * follow-up to show you know how to scale systems. I'll provide both below.
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Exactly touching edges: `[100, 200]` and `[200, 300]`. They must NOT stack. 
 *   The condition `max(L1, L2) < min(R1, R2)` resolves to `200 < 200` (False). Handled cleanly.
 * - Completely Enveloping: A massive square drops over 5 smaller stacks. 
 *   It correctly finds the absolute tallest among those 5.
 * - Entirely Disjoint: Drops in a completely empty region. Base height remains 0.
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Brushing doesn't count means intervals are mathematically open `(L, R)` during comparison."
 * Two intervals `[L1, R1]` and `[L2, R2]` strictly intersect if and only if 
 * `L1 < R2` AND `L2 < R1`.
 * 
 * ASCII Diagram: Stacking overlaps
 * Sq 1: [1, 3] (h=2)
 * Sq 2:    [2, 5] (h=3) -> Overlaps! Stacks on Sq 1. New height = 2 + 3 = 5.
 * Sq 3:          [6, 7] (h=1) -> No overlap with [1,3] or [2,5]. Height = 0 + 1 = 1.
 * 
 * Dry Run:
 * positions = [[1,2],[2,3],[6,1]]
 * 1. Drop [1, 2] -> L=1, R=3, Side=2.
 *    No prev squares. Base height = 0. New height = 2.
 *    History: [ (1, 3, 2) ]. Global Max = 2. Ans = [2].
 * 
 * 2. Drop [2, 3] -> L=2, R=5, Side=3.
 *    Check History:
 *    - (1, 3, 2): L1=1, R1=3. 1 < 5 AND 2 < 3? YES. Base height = max(0, 2) = 2.
 *    New height = Base(2) + Side(3) = 5.
 *    History: [ (1, 3, 2), (2, 5, 5) ]. Global Max = 5. Ans = [2, 5].
 * 
 * 3. Drop [6, 1] -> L=6, R=7, Side=1.
 *    Check History:
 *    - (1, 3, 2): 1 < 7 AND 6 < 3? NO.
 *    - (2, 5, 5): 2 < 7 AND 6 < 5? NO.
 *    Base height = 0. New height = 1.
 *    History: [ (1, 3, 2), (2, 5, 5), (6, 7, 1) ]. Global Max = 5. Ans = [2, 5, 5].
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Confusing `positions[1]` as the right bound. It's the `sideLength`! 
 *   The right bound is `positions[0] + positions[1]`.
 * - Updating the square's height but failing to track the *global maximum* history. 
 *   The answer array needs the highest stack *on the entire board*, not just the 
 *   height of the square that just fell.
 * - Inclusive boundary checking (`<=`). If you stack touching boxes, you will fail Example 2.
 * 
 * Pattern Recognition: 
 * "Small N + Huge Coordinates + Overlap checks = O(N^2) History List."
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "The problem asks us to find the maximum height on the board as squares fall. 
 * Since the X-coordinates can be up to 100 million, a flat array is impossible. 
 * However, since the maximum number of squares dropped is only 1,000, we don't 
 * need a complex data structure. 
 * I can maintain a simple list of the previously dropped squares. For each new square, 
 * I iterate through the history list to find any squares that strictly overlap its 
 * X-interval. The base height for the new square will be the maximum height of those 
 * overlapping squares. I add its side length to get its final height, add it to the 
 * history, and update our running global maximum. This runs in O(N^2) time, which 
 * is exceptionally fast for N=1000."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: How would you handle 100,000 falling squares?
 * A: O(N^2) would cause a Time Limit Exceeded. We would switch to Coordinate 
 *    Compression and a Segment Tree. We collect all unique `left` and `right` 
 *    bounds, sort them, and map them to indices. Then we use a Segment Tree with 
 *    lazy propagation to perform range maximum queries and range updates in O(log N).
 */

import java.util.ArrayList;
import java.util.List;

public class FallingSquares {

    // Simple class to represent a landed square
    static class Square {
        int left;
        int right;
        int height;

        public Square(int left, int right, int height) {
            this.left = left;
            this.right = right;
            this.height = height;
        }
    }

    /**
     * APPROACH 1: History List (Optimal for N = 1000)
     * Time Complexity: O(N^2)
     * Space Complexity: O(N)
     */
    public List<Integer> fallingSquaresBruteForce(int[][] positions) {
        List<Integer> result = new ArrayList<>();
        List<Square> history = new ArrayList<>();
        int globalMaxHeight = 0;

        for (int[] pos : positions) {
            int left = pos[0];
            int sideLength = pos[1];
            int right = left + sideLength;

            int baseHeight = 0;

            // Check against all previously dropped squares
            for (Square prev : history) {
                // Strict overlap condition:
                // max(L1, L2) < min(R1, R2)
                // Equivalently: one starts before the other ends, and vice versa.
                if (prev.left < right && left < prev.right) {
                    baseHeight = Math.max(baseHeight, prev.height);
                }
            }

            // The new height of this square once it lands
            int newHeight = baseHeight + sideLength;

            // Record it in history
            history.add(new Square(left, right, newHeight));

            // Update the global maximum height observed on the board
            globalMaxHeight = Math.max(globalMaxHeight, newHeight);
            
            result.add(globalMaxHeight);
        }

        return result;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        FallingSquares solution = new FallingSquares();
        
        // Test Case 1: Standard stacking (Example 1)
        int[][] pos1 = {{1, 2}, {2, 3}, {6, 1}};
        System.out.println("Test 1 (Expected [2, 5, 5]): " 
            + solution.fallingSquaresBruteForce(pos1));
            
        // Test Case 2: Touching edges (Example 2)
        // Ensure that [100, 200] and [200, 300] do not stack.
        int[][] pos2 = {{100, 100}, {200, 100}};
        System.out.println("Test 2 (Expected [100, 100]): " 
            + solution.fallingSquaresBruteForce(pos2));
            
        // Test Case 3: Complete envelopment
        // Sq 1: [1, 2] h=2. Sq 2: [4, 2] h=2. Sq 3: [1, 5] h=5.
        // Sq 3 covers both, should land on the max of them. Both are height 2.
        // New height = 2 + 5 = 7.
        int[][] pos3 = {{1, 2}, {4, 2}, {1, 5}};
        System.out.println("Test 3 (Expected [2, 2, 7]): " 
            + solution.fallingSquaresBruteForce(pos3));
            
        // Test Case 4: Pyramiding
        int[][] pos4 = {{3, 5}, {4, 3}, {5, 1}};
        // Sq 1: [3, 8] -> h=5
        // Sq 2: [4, 7] -> overlaps [3,8]. Base=5, h=5+3=8.
        // Sq 3: [5, 6] -> overlaps [4,7]. Base=8, h=8+1=9.
        System.out.println("Test 4 (Expected [5, 8, 9]): " 
            + solution.fallingSquaresBruteForce(pos4));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: O(N^2) State Tracking.
 * 
 * Key observation: Look closely at the constraints. When an interval problem 
 * features massive coordinate spaces (10^8) but a tiny number of items (N <= 1000), 
 * the problem is specifically designed for a straightforward N^2 historical scan. 
 * Don't overengineer a Segment Tree unless requested.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND the strict overlap condition. 
 * `L1 < R2 && L2 < R1` is the universally safest, cleanest way to check if two 
 * 1D ranges intersect without writing out clunky "enveloped, overlapping left, 
 * overlapping right" logic.
 * 
 * Most common trap: Missing the `globalMaxHeight`. The problem wants the 
 * highest stack currently on the *entire board*, not just the height of the 
 * specific stack that just landed!
 * 
 * One-line mental trigger: "N=1000 falling intervals? N^2 loop checking L1<R2 && L2<R1."
 */
