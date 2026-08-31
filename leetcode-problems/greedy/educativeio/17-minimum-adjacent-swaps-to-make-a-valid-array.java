import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: MINIMUM ADJACENT SWAPS TO MAKE A VALID ARRAY
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Are there duplicate elements in the array?" 
 *      (Assumption: Yes. The prompt specifies 'any one of the smallest/largest'. 
 *      We should logically pick the ones that require the fewest swaps).
 *    - "What if the array is already valid?" 
 *      (Assumption: Return 0 swaps).
 *    - "Can the array contain only one element?" 
 *      (Assumption: Yes. If so, it's already valid, return 0).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Move the smallest element to index 0, and the largest to index (n-1).
 *    - Observation 1 (Greedy Selection): To minimize swaps, we should target the 
 *      leftmost smallest element (because it's already closest to index 0) and 
 *      the rightmost largest element (because it's already closest to n-1).
 *    - Observation 2 (Calculating Swaps): 
 *      - Moving the leftmost minimum to index 0 takes `min_idx` swaps.
 *      - Moving the rightmost maximum to index (n-1) takes `(n - 1) - max_idx` swaps.
 *    - Observation 3 (The Overlap Shortcut): If the minimum element happens to 
 *      be to the *right* of the maximum element (`min_idx > max_idx`), their 
 *      paths will cross during the swapping process. When they cross, one swap 
 *      simultaneously moves BOTH elements closer to their destinations. 
 *      Therefore, we can subtract exactly 1 swap from our total!
 * 
 * 3. VISUAL EXPLANATION:
 *    Array: [3, 4, 5, 5, 3, 1]
 *    
 *    Step 1: Find Targets
 *    - Leftmost min is '1' at index 5.
 *    - Rightmost max is '5' at index 3.
 *    
 *    Step 2: Base Swaps Calculation
 *    - Swaps to move '1' to front = 5.
 *    - Swaps to move rightmost '5' to back = (6 - 1) - 3 = 2.
 *    - Base total = 7.
 *    
 *    Step 3: Check for Cross-over
 *    - Does min_idx (5) > max_idx (3)? Yes! 
 *    - As '1' moves left, it will jump past '5', pushing '5' one spot to the right 
 *      for free. 
 *    - We subtract 1 from total. 
 *    
 *    Final Answer: 7 - 1 = 6 swaps.
 * 
 * ============================================================================
 */
public class MinimumAdjacentSwaps {

    /**
     * APPROACH 1: Single Pass Index Tracking (Optimal)
     * 
     * Time Complexity: O(N) where N is the length of the array. We iterate once.
     * Space Complexity: O(1) auxiliary space, just storing variables.
     */
    public int minimumSwapsOptimal(int[] nums) {
        // Edge case: if there's only one element, it's already valid.
        if (nums == null || nums.length <= 1) {
            return 0;
        }
        
        int n = nums.length;
        
        // Variables to track the values and their optimal indices
        int minVal = nums[0];
        int minIdx = 0;
        
        int maxVal = nums[0];
        int maxIdx = 0;
        
        // Single pass to find the leftmost min and rightmost max
        for (int i = 1; i < n; i++) {
            // Strictly less than ensures we get the LEFTMOST minimum
            if (nums[i] < minVal) {
                minVal = nums[i];
                minIdx = i;
            }
            // Greater than or EQUAL ensures we get the RIGHTMOST maximum
            if (nums[i] >= maxVal) {
                maxVal = nums[i];
                maxIdx = i;
            }
        }
        
        // Calculate the base number of swaps
        int swapsToFront = minIdx;
        int swapsToBack = (n - 1) - maxIdx;
        
        // If the paths cross, we save exactly 1 swap
        if (minIdx > maxIdx) {
            return swapsToFront + swapsToBack - 1;
        } else {
            return swapsToFront + swapsToBack;
        }
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] nums, int expected) {}

    public static void main(String[] args) {
        MinimumAdjacentSwaps solver = new MinimumAdjacentSwaps();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[]{3, 4, 5, 5, 3, 1}, 6),
            new TestCase(new int[]{9}, 0),                 // Single element
            new TestCase(new int[]{1, 2, 3, 4, 5}, 0),     // Already valid
            new TestCase(new int[]{5, 4, 3, 2, 1}, 8),     // Fully reversed
            new TestCase(new int[]{1, 1, 5, 5}, 0),        // Duplicates on edges
            new TestCase(new int[]{5, 5, 1, 1}, 4)         // Max on left, min on right
        );
        
        System.out.println("--- Running Approach 1 (Single Pass O(N)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.minimumSwapsOptimal(tc.nums());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
