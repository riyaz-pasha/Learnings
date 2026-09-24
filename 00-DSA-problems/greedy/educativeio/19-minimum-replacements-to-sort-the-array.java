import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: MINIMUM REPLACEMENTS TO SORT THE ARRAY
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Are there any negative numbers or zeros in the array?" 
 *      (Assumption: No, the problem specifies positive integers).
 *    - "Can an element be split into more than two pieces at once?" 
 *      (Assumption: The operation says we split an element into TWO positive 
 *      integers. But functionally, splitting into 3 pieces is just 2 operations. 
 *      We just count the total number of operations required).
 *    - "Could the number of operations exceed the standard 32-bit integer limit?"
 *      (Assumption: Yes, since numbers can be up to 10^5 and the array length 
 *      up to 10^5. The total operations could easily exceed 2 billion, so we 
 *      MUST return a `long`).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Sort the array in non-decreasing order with minimum splits.
 *    - Observation 1 (Right-to-Left Traversal): The last element in the array 
 *      has no upper bound restricting it. We want it to be as large as possible 
 *      so that the elements before it have a larger target to stay under. Thus, 
 *      we must process the array from right to left, keeping track of the 
 *      "current bound".
 *    - Observation 2 (When to Split): If `nums[i] <= currentBound`, it's already 
 *      valid! We just update our `currentBound` to `nums[i]`. If `nums[i] > currentBound`, 
 *      we MUST split it.
 *    - Observation 3 (How to Split Optimally): To minimize operations, we split 
 *      `nums[i]` into the *fewest possible pieces* such that no piece exceeds `currentBound`.
 *      - Number of pieces needed: `k = ceil(nums[i] / currentBound)`.
 *      - Number of operations (splits): `k - 1`.
 *    - Observation 4 (Maximizing the New Bound): When we split `nums[i]` into 
 *      `k` pieces, the smallest piece becomes the NEW bound for the elements to 
 *      its left. To maximize this new bound, we must distribute `nums[i]` as 
 *      evenly as possible among the `k` pieces. 
 *      - The new bound will be: `floor(nums[i] / k)`.
 * 
 * 3. VISUAL EXPLANATION:
 *    Array: [3, 9, 3]
 *    
 *    Variables: operations = 0, currentBound = 3 (last element)
 *    
 *    Step 1 (Index 1): Value = 9.
 *      - Is 9 > currentBound (3)? Yes, we must split.
 *      - Pieces needed = ceil(9 / 3) = 3 pieces.
 *      - Operations added = (3 - 1) = 2.
 *      - The best way to split 9 into 3 pieces is [3, 3, 3].
 *      - New currentBound = floor(9 / 3) = 3.
 *      
 *    Step 2 (Index 0): Value = 3.
 *      - Is 3 > currentBound (3)? No. It's valid!
 *      - New currentBound = 3.
 *      
 *    Total operations = 2. (Array becomes [3, 3, 3, 3]).
 * 
 * ============================================================================
 */
public class MinimumReplacementsToSortArray {

    /**
     * APPROACH 1: Greedy Right-to-Left with Math (Optimal)
     * 
     * Time Complexity: O(N) where N is the length of the array. We iterate once.
     * Space Complexity: O(1) auxiliary space.
     */
    public long minimumReplacement(int[] nums) {
        long operations = 0;
        int n = nums.length;
        
        // The last element sets the initial upper bound for the element before it
        int currentBound = nums[n - 1];
        
        // Traverse right to left
        for (int i = n - 2; i >= 0; i--) {
            if (nums[i] <= currentBound) {
                // No split needed, just lower the bound for the next element
                currentBound = nums[i];
            } else {
                // Calculate how many pieces we need. 
                // Math trick for ceiling division: (A + B - 1) / B
                long pieces = (nums[i] + currentBound - 1) / currentBound;
                
                // Add the number of splits (operations)
                operations += pieces - 1;
                
                // Maximize the new bound by dividing the value as evenly as possible
                // among the required number of pieces.
                currentBound = nums[i] / (int) pieces;
            }
        }
        
        return operations;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] nums, long expected) {}

    public static void main(String[] args) {
        MinimumReplacementsToSortArray solver = new MinimumReplacementsToSortArray();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[]{3, 9, 3}, 2),
            new TestCase(new int[]{1, 2, 3, 4, 5}, 0),   // Already sorted
            new TestCase(new int[]{12, 9, 7, 6}, 6),     // Cascading splits needed
            new TestCase(new int[]{2, 10, 20, 19, 1}, 47)// Extreme splits required
        );
        
        System.out.println("--- Running Optimal O(N) Approach ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            long result = solver.minimumReplacement(tc.nums());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
