import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: CAN PLACE FLOWERS
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Can I modify the input array, or should I treat it as read-only?" 
 *      (Assumption: Modifying is usually fine for O(1) space, but we will 
 *      also show a non-modifying approach just in case).
 *    - "What if n is 0?" 
 *      (Assumption: If n is 0, we can always trivially place 0 flowers, return true).
 *    - "Are there any existing violations in the input?" 
 *      (Assumption: The prompt guarantees no two adjacent flowers exist initially).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Plant 'n' flowers such that no two are adjacent.
 *    - Observation 1 (Greedy Strategy): We should plant a flower at the very 
 *      first available valid spot we find. Leaving a valid spot empty never 
 *      increases our chances of placing more flowers later; it only wastes space.
 *    - Observation 2 (The Rule of 3 Zeros): To plant a flower at index 'i', 
 *      we need flowerbed[i-1] == 0, flowerbed[i] == 0, and flowerbed[i+1] == 0.
 *    - Observation 3 (Edge Cases): The first and last elements only need to 
 *      worry about one neighbor. We can treat the out-of-bounds indices 
 *      (i = -1 and i = length) as virtual empty plots (0).
 * 
 * 3. VISUAL EXPLANATION:
 *    Flowerbed: [1, 0, 0, 0, 1], n = 1
 *    
 *    Index 0: '1' (Occupied. Move to next)
 *    Index 1: '0' (Left neighbor is '1'. Cannot plant. Move to next)
 *    Index 2: '0' (Left is '0', Right is '0'. Valid!)
 *             -> Plant flower: Change index 2 to '1'. 
 *             -> n becomes 0.
 *             -> Return true.
 * 
 * ============================================================================
 */
class CanPlaceFlowers {

    /**
     * APPROACH 1: Greedy with Array Modification (Standard)
     * 
     * Time Complexity: O(N) where N is the length of the flowerbed.
     * Space Complexity: O(1) since we modify the array in place.
     */
    public boolean canPlaceFlowersStandard(int[] flowerbed, int n) {
        if (n == 0) return true;
        
        for (int i = 0; i < flowerbed.length; i++) {
            // Check if current plot is empty
            if (flowerbed[i] == 0) {
                // Check left and right neighbors (treating out-of-bounds as 0)
                boolean emptyLeft = (i == 0) || (flowerbed[i - 1] == 0);
                boolean emptyRight = (i == flowerbed.length - 1) || (flowerbed[i + 1] == 0);
                
                if (emptyLeft && emptyRight) {
                    // Plant the flower!
                    flowerbed[i] = 1;
                    n--;
                    
                    if (n == 0) return true; // Early exit optimization
                }
            }
        }
        
        return n <= 0;
    }

    /**
     * APPROACH 2: Optimized Greedy (No Array Modification, Jump Pointers)
     * 
     * If the interviewer asks you NOT to modify the input array, you can use 
     * this approach. It also runs slightly faster because it skips unnecessary checks.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     */
    public boolean canPlaceFlowersOptimized(int[] flowerbed, int n) {
        if (n == 0) return true;
        
        int count = 0;
        for (int i = 0; i < flowerbed.length; i++) {
            if (flowerbed[i] == 0) {
                boolean emptyLeft = (i == 0) || (flowerbed[i - 1] == 0);
                boolean emptyRight = (i == flowerbed.length - 1) || (flowerbed[i + 1] == 0);
                
                if (emptyLeft && emptyRight) {
                    count++;
                    // Skip the next plot because we just planted a flower here,
                    // so the next plot is guaranteed to be invalid.
                    i++; 
                    if (count >= n) return true;
                }
            } else {
                // If we see a '1', we know the next spot 'i+1' is definitely invalid.
                // We can safely jump i by 1 (which combined with the loop's i++ means we jump 2).
                i++;
            }
        }
        
        return count >= n;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     */
    record TestCase(int[] flowerbed, int n, boolean expected) {}

    public static void main(String[] args) {
        CanPlaceFlowers solver = new CanPlaceFlowers();
        
        // Defining test cases using our Record (Immutable data carrier)
        var testCases = List.of(
            new TestCase(new int[]{1, 0, 0, 0, 1}, 1, true),
            new TestCase(new int[]{1, 0, 0, 0, 1}, 2, false),
            new TestCase(new int[]{0, 0, 1, 0, 0}, 1, true),
            new TestCase(new int[]{0}, 1, true),
            new TestCase(new int[]{1}, 0, true), // Edge case: n = 0
            new TestCase(new int[]{0, 0, 0}, 2, true) // Edge case: placing at start and end
        );
        
        System.out.println("--- Running Approach 1 (Standard) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            // Clone the array so we don't ruin it for the second run
            int[] clonedBed = tc.flowerbed().clone(); 
            boolean result = solver.canPlaceFlowersStandard(clonedBed, tc.n());
            System.out.printf("Test %d: Expected = %b, Got = %b -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Optimized) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            boolean result = solver.canPlaceFlowersOptimized(tc.flowerbed(), tc.n());
            System.out.printf("Test %d: Expected = %b, Got = %b -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
