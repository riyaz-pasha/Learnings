import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: CANDY
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "If two adjacent children have the SAME rating, do they need the same 
 *      number of candies?" 
 *      (Assumption: No. The rule only states that a child with a HIGHER rating 
 *      must get more than their neighbor. Equal ratings have no such constraint, 
 *      so a child with an equal rating could theoretically get 1 candy to save costs).
 *    - "Can a child's rating be negative?" 
 *      (Assumption: Based on constraints, ratings are >= 0, but the logic works 
 *      even for negative ratings).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Distribute minimum candies such that every child gets >= 1, and 
 *      higher-rated neighbors get more.
 *    - Observation 1 (The Propagation Problem): If we just iterate left to right, 
 *      giving more candies when the rating goes up, we fail when the rating goes 
 *      down. For example, ratings [3, 2, 1]. A left-to-right pass can't easily 
 *      know how many candies the '3' needs without first processing the '2' and '1'.
 *    - Observation 2 (The Two-Pass Solution): 
 *      Since a child's candy count depends on BOTH their left neighbor and right 
 *      neighbor, we can decouple the rules:
 *      Rule A: Higher rating than left neighbor -> gets more than left neighbor.
 *      Rule B: Higher rating than right neighbor -> gets more than right neighbor.
 *      We can satisfy Rule A with a Left-to-Right pass.
 *      We can satisfy Rule B with a Right-to-Left pass.
 *      The FINAL candy count for a child is simply the maximum of what Rule A 
 *      demands and what Rule B demands!
 * 
 * 3. VISUAL EXPLANATION (Two-Pass):
 *    Ratings: [1, 0, 2]
 *    
 *    Step 1: Everyone gets 1 candy.
 *    Candies: [1, 1, 1]
 *    
 *    Step 2: Left-to-Right Pass (Compare with left neighbor)
 *    Index 1: 0 < 1 (No change)
 *    Index 2: 2 > 0 (Rating is higher! Candies[2] = Candies[1] + 1 = 2)
 *    L2R Array: [1, 1, 2]
 *    
 *    Step 3: Right-to-Left Pass (Compare with right neighbor)
 *    Index 1: 0 < 2 (No change)
 *    Index 0: 1 > 0 (Rating is higher! Candies[0] = max(Candies[0], Candies[1] + 1) = 2)
 *    Final Array: [2, 1, 2]
 *    
 *    Total minimum candies = 2 + 1 + 2 = 5.
 * 
 * ============================================================================
 */
public class Candy {

    /**
     * APPROACH 1: Two-Pass Greedy (Standard & Intuitive)
     * 
     * Time Complexity: O(N) where N is the number of children. We do two linear scans.
     * Space Complexity: O(N) to store the candies array.
     */
    public int candyTwoPass(int[] ratings) {
        int n = ratings.length;
        int[] candies = new int[n];
        
        // Every child must have at least one candy
        Arrays.fill(candies, 1);
        
        // Left-to-Right Pass: Satisfy the left neighbor constraint
        for (int i = 1; i < n; i++) {
            if (ratings[i] > ratings[i - 1]) {
                candies[i] = candies[i - 1] + 1;
            }
        }
        
        // Right-to-Left Pass: Satisfy the right neighbor constraint and calculate total
        int totalCandies = candies[n - 1]; // Start sum with the last element
        for (int i = n - 2; i >= 0; i--) {
            if (ratings[i] > ratings[i + 1]) {
                // We take the max to ensure BOTH left and right constraints are met
                candies[i] = Math.max(candies[i], candies[i + 1] + 1);
            }
            totalCandies += candies[i];
        }
        
        return totalCandies;
    }

    /**
     * APPROACH 2: Single-Pass Slope Tracking (Highly Optimized O(1) Space)
     * 
     * Instead of storing an array, we track the current "slope" (up, down, or flat).
     * - Going UP: We keep adding 1 more candy than the last child.
     * - Going FLAT (equal rating): Reset to 1 candy.
     * - Going DOWN: We count the length of the downward slope. If the downward 
     *   slope length exceeds the height of the preceding peak, the peak itself 
     *   must be raised to satisfy the constraints.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     */
    public int candyOnePass(int[] ratings) {
        if (ratings.length == 0) return 0;
        
        int totalCandies = 1; // First child gets 1 candy
        int upSlope = 0;
        int downSlope = 0;
        int peakHeight = 0;
        
        for (int i = 1; i < ratings.length; i++) {
            if (ratings[i] > ratings[i - 1]) {
                // Slope is UP
                upSlope++;
                peakHeight = upSlope; // New peak established
                downSlope = 0;        // Reset down slope
                totalCandies += 1 + upSlope;
                
            } else if (ratings[i] == ratings[i - 1]) {
                // Slope is FLAT
                upSlope = 0;
                downSlope = 0;
                peakHeight = 0;
                totalCandies += 1; // Give 1 candy to save cost
                
            } else {
                // Slope is DOWN
                downSlope++;
                upSlope = 0;
                totalCandies += downSlope;
                
                // If the downward slope becomes longer than the peak height,
                // the peak needs an extra candy to remain strictly larger 
                // than the child immediately below it on the slope.
                if (downSlope > peakHeight) {
                    totalCandies++;
                }
            }
        }
        
        return totalCandies;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     */
    record TestCase(int[] ratings, int expected) {}

    public static void main(String[] args) {
        Candy solver = new Candy();
        
        var testCases = List.of(
            new TestCase(new int[]{1, 0, 2}, 5),
            new TestCase(new int[]{1, 2, 2}, 4), // The second '2' only needs 1 candy
            new TestCase(new int[]{1, 3, 2, 2, 1}, 7), // Mix of slopes and flats
            new TestCase(new int[]{1, 2, 3, 4, 5}, 15), // Strictly increasing
            new TestCase(new int[]{5, 4, 3, 2, 1}, 15)  // Strictly decreasing
        );
        
        System.out.println("--- Running Approach 1 (Two-Pass O(N) Space) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.candyTwoPass(tc.ratings());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (One-Pass O(1) Space) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.candyOnePass(tc.ratings());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
