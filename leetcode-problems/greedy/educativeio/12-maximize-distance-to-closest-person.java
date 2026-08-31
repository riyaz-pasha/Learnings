import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: MAXIMIZE DISTANCE TO CLOSEST PERSON
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Are there always at least one empty seat and at least one occupied seat?" 
 *      (Assumption: Yes, based on the constraints).
 *    - "What happens if there are empty seats at the very beginning or end of the row?" 
 *      (Assumption: These are special cases! Sitting at the absolute edge means 
 *      we only have ONE neighbor to worry about, not two.)
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Find the empty seat (0) that has the maximum minimum distance to 
 *      any occupied seat (1).
 *    - Observation 1 (The Middle Seats): If we choose a seat strictly BETWEEN 
 *      two people, the safest spot is exactly in the middle. 
 *      For `K` consecutive empty seats between two people, the maximum distance 
 *      we can achieve is `(K + 1) / 2`.
 *    - Observation 2 (The Edge Seats): If we choose a seat at the very beginning 
 *      (left edge) or very end (right edge) of the row, there is no person on 
 *      one of the sides. The distance to the closest person is simply the 
 *      number of consecutive zeros! 
 *      Example: `[0, 0, 0, 1]` -> sitting at index 0 yields a distance of 3.
 *    - Strategy: We just need to track three distinct scenarios:
 *      1. Distance from the start of the array to the first '1'.
 *      2. Distance from the last '1' to the end of the array.
 *      3. The largest distance between any two '1's.
 * 
 * 3. VISUAL EXPLANATION:
 *    Seats: [1, 0, 0, 0, 1, 0, 1]
 *    
 *    Scenario 1: Leading zeros? 
 *    None here. First element is a 1. (Distance = 0)
 *    
 *    Scenario 2: Trailing zeros? 
 *    None here. Last element is a 1. (Distance = 0)
 *    
 *    Scenario 3: Zeros between 1s!
 *    - Between index 0 and 4: Three zeros `0, 0, 0`.
 *      Best seat is in the middle: index 2. 
 *      Distance = (4 - 0) / 2 = 2.
 *    - Between index 4 and 6: One zero `0`.
 *      Best seat is index 5.
 *      Distance = (6 - 4) / 2 = 1.
 *    
 *    Max of all scenarios = Math.max(0, 0, Math.max(2, 1)) = 2.
 * 
 * ============================================================================
 */
public class MaximizeDistanceToClosestPerson {

    /**
     * APPROACH 1: One-Pass Tracking (Optimal)
     * 
     * We only need to iterate through the array once, keeping track of the index 
     * of the last person we saw.
     * 
     * Time Complexity: O(N) where N is the length of the seats array.
     * Space Complexity: O(1) auxiliary space.
     */
    public int maxDistToClosestOptimal(int[] seats) {
        int maxDistance = 0;
        int lastPerson = -1;
        int n = seats.length;
        
        for (int i = 0; i < n; i++) {
            if (seats[i] == 1) {
                // If this is the FIRST person we've seen, it covers the leading zeros case
                if (lastPerson == -1) {
                    maxDistance = i; 
                } 
                // Otherwise, calculate the distance between this person and the last person
                else {
                    int distanceBetween = (i - lastPerson) / 2;
                    maxDistance = Math.max(maxDistance, distanceBetween);
                }
                
                // Update the last seen person to the current index
                lastPerson = i;
            }
        }
        
        // Finally, handle the trailing zeros case (from the last person to the end of the array)
        int distanceToEnd = n - 1 - lastPerson;
        maxDistance = Math.max(maxDistance, distanceToEnd);
        
        return maxDistance;
    }

    /**
     * APPROACH 2: Left and Right Arrays (Highly Intuitive DP approach)
     * 
     * In an interview, if the greedy one-pass is tricky to visualize, this is a 
     * bulletproof way to solve it. For every seat, we calculate its distance to 
     * the closest person on the left, and closest on the right. 
     * The true distance for any seat is the minimum of those two!
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(N) for the left and right arrays.
     */
    public int maxDistToClosestLeftRightArrays(int[] seats) {
        int n = seats.length;
        int[] left = new int[n];
        int[] right = new int[n];
        
        // Fill Left Array: distance to the closest person on the left
        // Initialize to a large number assuming no person found yet
        int lastSeen = -n; 
        for (int i = 0; i < n; i++) {
            if (seats[i] == 1) {
                lastSeen = i;
            }
            left[i] = i - lastSeen;
        }
        
        // Fill Right Array: distance to the closest person on the right
        lastSeen = 2 * n; // Arbitrary large number
        for (int i = n - 1; i >= 0; i--) {
            if (seats[i] == 1) {
                lastSeen = i;
            }
            right[i] = lastSeen - i;
        }
        
        // Find the maximum of the minimums
        int maxDistance = 0;
        for (int i = 0; i < n; i++) {
            if (seats[i] == 0) {
                int closestPersonDist = Math.min(left[i], right[i]);
                maxDistance = Math.max(maxDistance, closestPersonDist);
            }
        }
        
        return maxDistance;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] seats, int expected) {}

    public static void main(String[] args) {
        MaximizeDistanceToClosestPerson solver = new MaximizeDistanceToClosestPerson();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[]{1, 0, 0, 0, 1, 0, 1}, 2), // Standard case
            new TestCase(new int[]{1, 0, 0, 0}, 3),          // Trailing zeroes
            new TestCase(new int[]{0, 1}, 1),                // Leading zeroes
            new TestCase(new int[]{0, 0, 0, 1, 0, 0, 0, 1, 0, 0}, 3) // Multiple scenarios
        );
        
        System.out.println("--- Running Approach 1 (One-Pass O(1) Space) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.maxDistToClosestOptimal(tc.seats());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Left/Right Arrays O(N) Space) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.maxDistToClosestLeftRightArrays(tc.seats());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
