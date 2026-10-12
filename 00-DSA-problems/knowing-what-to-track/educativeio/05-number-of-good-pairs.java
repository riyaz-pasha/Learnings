/**
 * STANDALONE STUDY NOTE: NUMBER OF GOOD PAIRS
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Since constraints state nums[i] is between 1 and 100, can I assume a fixed-size array is acceptable for frequency counting?" (Unlocks O(1) space optimization).
 * - "Can the count of good pairs exceed the maximum value of a 32-bit signed integer?" (With max length 100, max pairs is 100 * 99 / 2 = 4950, so standard `int` is fine. But asking shows awareness of integer overflow).
 * - "Do we just need the total count, or do we need to return the actual pairs/indices?" (Returning a count allows us to use combinatorics/math instead of storing paths).
 * - "Is the array guaranteed to have at least one element?" (The constraint 1 <= length <= 100 confirms it, preventing empty array checks).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to find how many pairs (i, j) exist where i < j and nums[i] == nums[j]. 
 * The binding constraint is doing this efficiently without redundant comparisons.
 * 
 * --- APPROACH 1: BRUTE FORCE (Nested Loops) ---
 * 1. What I'd naturally try: I'd take the first element, then scan the rest of the array to see if any 
 *    match. Then take the second element, scan the rest, and so on.
 * 2. Why it works: It literally checks every possible combination of (i, j) where i < j.
 * 3. Why it's too slow/costly: We look at the same elements over and over. If the array is [1, 1, 1, 1], 
 *    the first '1' checks the next three. The second '1' checks the next two.
 * 4. What work is repeated: We are repeatedly reading elements instead of remembering we saw them.
 * 5. Cost Breakdown:
 *    - Time Complexity: O(N^2) — because for N elements, we do roughly N/2 comparisons (N * (N-1) / 2 total).
 *    - Space Complexity: O(1) — because we only need a single counter variable.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Frequency Counting) ---
 * 1. The Observation: If I've already seen the number '4' three times, and I see another '4', I can instantly 
 *    form 3 new pairs with the previous ones. I don't need to loop back to find them.
 * 2. How it leads to the next step: We can iterate through the array exactly once. We keep a `HashMap<Integer, Integer>` 
 *    to track how many times we've seen each number *so far*. When we encounter a number, we add its current map 
 *    count to our total, then increment its count in the map.
 * 3. Why it works: It mathematically calculates the pairs on the fly (dynamic counting).
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we visit each element exactly once, doing O(1) map operations.
 *    - Space Complexity: O(U) where U is unique numbers — because we store counts in a Map. In Java, this 
 *      involves Integer autoboxing overhead.
 * 
 * --- APPROACH 3: OPTIMAL (Array-based Frequency Map) ---
 * 1. The Observation: The constraints explicitly say `1 <= nums[i] <= 100`. The domain is tiny and contiguous.
 * 2. How it leads to the next step: We replace the HashMap with an `int[101]` array. The index is the number, 
 *    and the value is the frequency. 
 * 3. Which one I'd write in an interview: Approach 3 (Array). It's O(N) time but avoids all Java object overhead 
 *    from HashMaps, making it practically instantaneous. 
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N) — because we linearly scan the array doing O(1) primitive array lookups.
 *    - Space Complexity: O(1) — because we allocate exactly one array of size 101 (404 bytes), regardless 
 *      of how large the input array gets.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - All unique elements: [1, 2, 3, 4] -> Should return 0.
 * - All identical elements: [1, 1, 1, 1] -> Should return 6 (4 * 3 / 2).
 * - Minimum length: [1] -> Should return 0.
 * 
 * =================================================================================================
 * 
 * 4. DRY RUN (Optimal Approach)
 * Target: nums = [1, 2, 3, 1, 1, 3]
 * 
 * Init: counts = [0, 0, ..., 0] (size 101), totalPairs = 0
 * 
 * i=0, num=1: 
 *   totalPairs += counts[1] (0) -> 0
 *   counts[1]++ -> 1
 * i=1, num=2: 
 *   totalPairs += counts[2] (0) -> 0
 *   counts[2]++ -> 1
 * i=2, num=3: 
 *   totalPairs += counts[3] (0) -> 0
 *   counts[3]++ -> 1
 * i=3, num=1: 
 *   totalPairs += counts[1] (1) -> 1  (This '1' pairs with the first '1')
 *   counts[1]++ -> 2
 * i=4, num=1: 
 *   totalPairs += counts[1] (2) -> 3  (This '1' pairs with the two previous '1's)
 *   counts[1]++ -> 3
 * i=5, num=3: 
 *   totalPairs += counts[3] (1) -> 4  (This '3' pairs with the first '3')
 *   counts[3]++ -> 2
 * 
 * Result: 4 good pairs.
 * 
 * =================================================================================================
 * 
 * 5. FOLLOW-UPS
 * Q: What if the numbers could be up to 10^9?
 * A: The `int[101]` array would cause an OutOfMemoryError. I would fall back to the HashMap approach, 
 *    which maintains O(N) time but scales space based on unique elements rather than maximum value.
 * 
 * Q: What if the array size was 10^6 and all elements were the same?
 * A: The maximum number of pairs would be 10^6 * (10^6 - 1) / 2, which is approx 5 * 10^11. This exceeds 
 *    the maximum value of a 32-bit integer (2.14 * 10^9). We would need to change `totalPairs` to a `long` 
 *    to prevent integer overflow.
 */

import java.util.HashMap;
import java.util.Map;

public class GoodPairs {

    /**
     * APPROACH 3: OPTIMAL (Array Frequency Map)
     * Time: O(N), Space: O(1)
     */
    public int numIdenticalPairsOptimal(int[] nums) {
        // [Why needed]: Holds frequencies of numbers seen so far.
        // Size 101 handles numbers 1 through 100 perfectly (index matches value).
        int[] counts = new int[101];
        int goodPairs = 0;

        for (int i = 0; i < nums.length; i++) {
            int currentNum = nums[i];
            
            // [What it does]: If we've seen this number X times before, this new 
            // instance can form exactly X new pairs.
            goodPairs += counts[currentNum];
            
            // [Why needed]: Register this number so future identical numbers can pair with it.
            counts[currentNum]++;
        }

        return goodPairs;
    }

    /**
     * APPROACH 2: INTERMEDIATE (HashMap Frequency Counting)
     * Time: O(N), Space: O(U) -> High constant overhead
     */
    public int numIdenticalPairsHashMap(int[] nums) {
        Map<Integer, Integer> counts = new HashMap<>();
        int goodPairs = 0;

        for (int num : nums) {
            int previousCount = counts.getOrDefault(num, 0);
            
            goodPairs += previousCount;
            counts.put(num, previousCount + 1);
        }

        return goodPairs;
    }

    /**
     * APPROACH 1: BRUTE FORCE (Nested Loops)
     * Time: O(N^2), Space: O(1)
     */
    public int numIdenticalPairsBruteForce(int[] nums) {
        int goodPairs = 0;
        
        for (int i = 0; i < nums.length; i++) {
            // Inner loop only checks j > i, fulfilling the i < j constraint naturally
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] == nums[j]) {
                    goodPairs++;
                }
            }
        }
        
        return goodPairs;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Running frequency count (combinatorics).
     * 2. Key observation: When you see a number for the K-th time, it forms (K-1) new pairs.
     * 3. Most common trap: Calculating frequencies first and using the combination formula (n * (n-1) / 2) 
     *    in a second pass. While technically O(N), the single-pass addition trick is cleaner and faster.
     * 4. Mental trigger: "Number of pairs where elements match" -> dynamic counting with an array or map.
     */
    public static void main(String[] args) {
        GoodPairs solution = new GoodPairs();

        // Test Case 1: Standard Example
        int[] nums1 = {1, 2, 3, 1, 1, 3};
        System.out.println("Test 1 (Standard): " + solution.numIdenticalPairsOptimal(nums1)); // Expected: 4

        // Test Case 2: All Identical
        int[] nums2 = {1, 1, 1, 1};
        System.out.println("Test 2 (All Identical): " + solution.numIdenticalPairsOptimal(nums2)); // Expected: 6

        // Test Case 3: All Unique
        int[] nums3 = {1, 2, 3, 4};
        System.out.println("Test 3 (All Unique): " + solution.numIdenticalPairsOptimal(nums3)); // Expected: 0
        
        // Test Case 4: Minimum length
        int[] nums4 = {1};
        System.out.println("Test 4 (Single Element): " + solution.numIdenticalPairsOptimal(nums4)); // Expected: 0
    }
}


class Solution {
    public int numIdenticalPairs(int[] nums) {
        int[] frequency = new int[101];
        int count = 0;

        for (int num : nums) {
            // Previously seen occurrences form good pairs.
            count += frequency[num];

            // Include the current occurrence for future pairs.
            frequency[num]++;
        }

        return count;
    }
}
/*
Complexity
- Time: \(O(n)\).
- Space: \(O(1)\) — the frequency array has a fixed size of 101.
*/

