import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Number of Longest Increasing Subsequence
 * Given an integer array nums, return the number of longest strictly increasing 
 * subsequences it contains.
 * 
 * Constraints:
 * 1 <= nums.length <= 2000
 * -10^6 <= nums[i] <= 10^6
 * Result is guaranteed to fit in a 32-bit integer.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview at companies like EPAM or Google, they will look for 
 * how quickly you can adapt a known algorithm.
 * 
 * Q: "Are there duplicate numbers, and how do they affect 'strictly increasing'?"
 * A: Yes, array can have duplicates. 'Strictly increasing' means we cannot 
 *    include [2, 2]. We only append if nums[i] > nums[j].
 * 
 * Q: "Can multiple subsequences look identical but use different indices?"
 * A: Yes! If nums = [1, 3, 5, 4, 7], the LIS is [1, 3, 5, 7] and [1, 3, 4, 7]. 
 *    Even if the values are the same, if they come from different indices, 
 *    they count as distinct subsequences.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "This is a direct evolution of the standard Longest Increasing Subsequence (LIS).
 * In standard LIS, we only care about the MAXIMUM length. 
 * Here, we need to know HOW MANY different paths reached that maximum length.
 * 
 * To solve this, at every index 'i', I must track TWO pieces of information:
 * 1. length: The length of the LIS ending exactly at 'i'.
 * 2. count: The number of ways I can form an LIS of that exact length ending at 'i'.
 * 
 * When I look back at a previous index 'j' (where nums[j] < nums[i]):
 * - If appending to 'j' gives me a STRICTLY LONGER sequence than I currently have, 
 *   I completely overwrite my length and count with j's count.
 * - If appending to 'j' gives me a sequence EQUAL to my current max length, 
 *   I just found ANOTHER parallel path! I add j's count to my existing count."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: nums = [1, 3, 5, 4, 7]
 * We maintain two arrays: lengths[] and counts[]. Initially all 1s.
 * 
 * i=0 (1): lengths=[1], counts=[1]
 * i=1 (3): 3 > 1. lengths[1] = lengths[0]+1 = 2. counts[1] = counts[0] = 1.
 * i=2 (5): 5 > 1, 5 > 3. lengths[2] = 3. counts[2] = 1. (Path: 1->3->5)
 * i=3 (4): 4 > 1, 4 > 3. lengths[3] = 3. counts[3] = 1. (Path: 1->3->4)
 * i=4 (7): 
 *   - > 1? length becomes 2, count = 1.
 *   - > 3? length becomes 3, count = 1.
 *   - > 5? length becomes 4, count = 1. (from index 2)
 *   - > 4? length becomes 4! This EQUALS our current max length (4). 
 *          So we ADD counts[3] to counts[4]. counts[4] becomes 1 + 1 = 2.
 * 
 * Max length overall is 4. Sum of counts where length == 4 is 2.
 */
public class NumberOfLIS {

    /**
     * A Java Record to elegantly hold both state values together.
     * Perfect for senior-level clean code structure.
     */
    private record LISState(int length, int count) {}

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Write a recursive function that returns the (length, count) of the 
     * LIS perfectly ending at index i.
     * 
     * Time Complexity: O(2^n) - Exponential branching.
     * Space Complexity: O(n) - Recursion stack depth.
     */
    public int findNumberOfLISRecursive(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        
        int globalMaxLength = 0;
        int globalTotalCount = 0;

        // We must check every index as a potential ENDING point for our LIS
        for (int i = 0; i < nums.length; i++) {
            LISState state = getLISStateRecursive(nums, i);
            
            if (state.length() > globalMaxLength) {
                globalMaxLength = state.length();
                globalTotalCount = state.count();
            } else if (state.length() == globalMaxLength) {
                globalTotalCount += state.count();
            }
        }
        
        return globalTotalCount;
    }

    private LISState getLISStateRecursive(int[] nums, int currentIndex) {
        // BASE CASE REASONING:
        // A single element standing completely alone forms a valid strictly 
        // increasing subsequence of length 1. There is exactly 1 way to form it.
        int maxLength = 1;
        int totalCount = 1;

        // Look back at all previous elements to see if we can append ourselves
        for (int j = 0; j < currentIndex; j++) {
            
            // Only consider it if it strictly increases
            if (nums[j] < nums[currentIndex]) {
                LISState previousState = getLISStateRecursive(nums, j);
                int potentialLength = previousState.length() + 1;
                
                if (potentialLength > maxLength) {
                    // Found a strictly longer path! Discard old paths, adopt this one.
                    maxLength = potentialLength;
                    totalCount = previousState.count();
                } else if (potentialLength == maxLength) {
                    // Found an equally long path! Add its ways to our total.
                    totalCount += previousState.count();
                }
            }
        }

        return new LISState(maxLength, totalCount);
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the LISState for each index so we don't recalculate the same 
     * ending sub-arrays multiple times.
     * 
     * Time Complexity: O(n^2) - We evaluate each index once, looping backward inside.
     * Space Complexity: O(n) - For the memo array + recursion stack.
     */
    public int findNumberOfLISMemo(int[] nums) {
        if (nums == null || nums.length == 0) return 0;
        
        LISState[] memo = new LISState[nums.length];
        int globalMaxLength = 0;
        int globalTotalCount = 0;

        for (int i = 0; i < nums.length; i++) {
            LISState state = getLISStateMemo(nums, i, memo);
            
            if (state.length() > globalMaxLength) {
                globalMaxLength = state.length();
                globalTotalCount = state.count();
            } else if (state.length() == globalMaxLength) {
                globalTotalCount += state.count();
            }
        }
        
        return globalTotalCount;
    }

    private LISState getLISStateMemo(int[] nums, int currentIndex, LISState[] memo) {
        // Return cached state if available
        if (memo[currentIndex] != null) {
            return memo[currentIndex];
        }

        // BASE CASE REASONING (same physical logic as brute force)
        int maxLength = 1;
        int totalCount = 1;

        for (int j = 0; j < currentIndex; j++) {
            if (nums[j] < nums[currentIndex]) {
                LISState previousState = getLISStateMemo(nums, j, memo);
                int potentialLength = previousState.length() + 1;
                
                if (potentialLength > maxLength) {
                    maxLength = potentialLength;
                    totalCount = previousState.count();
                } else if (potentialLength == maxLength) {
                    totalCount += previousState.count();
                }
            }
        }

        memo[currentIndex] = new LISState(maxLength, totalCount);
        return memo[currentIndex];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 1D)
     * ========================================================================
     * Idea: Iteratively build two 1D arrays: lengths[] and counts[].
     * 
     * Time Complexity: O(n^2)
     * Space Complexity: O(n)
     */
    public int findNumberOfLISTabulation(int[] nums) {
        int n = nums.length;
        if (n == 0) return 0;

        // lengths[i] answers: "What is the length of the LIS perfectly ending at i?"
        int[] lengths = new int[n];
        // counts[i] answers: "How many different paths form that LIS ending at i?"
        int[] counts = new int[n];

        // BASE CASE REASONING:
        // By default, every number is a valid subsequence of length 1, 
        // and there is exactly 1 way to form it (by taking just the number itself).
        Arrays.fill(lengths, 1);
        Arrays.fill(counts, 1);

        int maxGlobalLength = 1;

        // Outer loop: We are trying to find the LIS perfectly ending at 'i'.
        for (int i = 1; i < n; i++) {
            
            // Inner loop: We look back at EVERY item 'j' that came before 'i'.
            // We want to see if we can append 'i' to the sequence that ended at 'j'.
            for (int j = 0; j < i; j++) {
                
                // PHYSICAL CHECK: Is my current item strictly larger than the previous item?
                if (nums[j] < nums[i]) {
                    
                    // IT FITS! We calculate the length if we appended ourselves to j.
                    int potentialLength = lengths[j] + 1;

                    // SCENARIO A: A Brand New Record
                    // If appending to 'j' creates a sequence STRICTLY LONGER than 
                    // anything we have seen ending at 'i' so far...
                    if (potentialLength > lengths[i]) {
                        
                        // We overwrite our length to this new maximum.
                        lengths[i] = potentialLength;
                        
                        // Because this is the ONLY way we've found to reach this new 
                        // maximum length, we inherit the exact number of ways 'j' was formed.
                        counts[i] = counts[j];
                    } 
                    
                    // SCENARIO B: Another Parallel Path Discovered
                    // If appending to 'j' creates a sequence EXACTLY EQUAL to the 
                    // maximum length we've already established for 'i'...
                    else if (potentialLength == lengths[i]) {
                        
                        // We found another valid route! We keep our length the same, 
                        // but we ADD the number of ways 'j' was formed to our total count.
                        counts[i] += counts[j];
                    }
                }
            }
            
            // As we compute 'i', we update the absolute longest sequence seen across the whole array.
            maxGlobalLength = Math.max(maxGlobalLength, lengths[i]);
        }

        // Final step: We know the maximum length. Now we must sum the counts of ALL 
        // indices that managed to reach this maximum length.
        int totalNumberOfLIS = 0;
        for (int i = 0; i < n; i++) {
            if (lengths[i] == maxGlobalLength) {
                totalNumberOfLIS += counts[i];
            }
        }

        return totalNumberOfLIS;
    }

    /**
     * Note on L4/L5 Space Optimization Expectation:
     * For LIS problems, the Tabulation array is already optimized down to a 1D array of O(N) space.
     * We cannot optimize this to O(1) space like House Robber or Knapsack because finding the LIS 
     * ending at index 'i' strictly requires scanning ALL previous indices 'j' from 0 to i-1. 
     * We need the full history available at all times.
     * 
     * (Time complexity CAN be optimized to O(N log N) using a Segment Tree or Fenwick Tree 
     * to query maximums and sum counts over ranges, but O(N^2) Tabulation is generally the 
     * targeted answer for DP-specific rounds).
     */

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new NumberOfLIS();
        
        record TestCase(int[] nums, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(new int[]{1, 3, 5, 4, 7}, 2),    // [1,3,5,7] and [1,3,4,7]
            new TestCase(new int[]{2, 2, 2, 2, 2}, 5),    // Each '2' is an LIS of length 1
            new TestCase(new int[]{1, 2, 4, 3, 5, 4, 7, 2}, 3), // Multiple converging paths
            new TestCase(new int[]{1}, 1)
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Array   : " + Arrays.toString(tc.nums));
            System.out.println("Expected: " + tc.expected);
            
            System.out.println("Recursive (Brute) : " + solver.findNumberOfLISRecursive(tc.nums));
            System.out.println("Memoization       : " + solver.findNumberOfLISMemo(tc.nums));
            System.out.println("Tabulation 1D     : " + solver.findNumberOfLISTabulation(tc.nums));
            System.out.println();
        }
    }
}
