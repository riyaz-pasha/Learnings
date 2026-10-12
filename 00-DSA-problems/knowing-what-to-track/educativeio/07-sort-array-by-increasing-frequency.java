/**
 * STANDALONE STUDY NOTE: SORT ARRAY BY INCREASING FREQUENCY
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Can the input array be empty?" (Constraints say length >= 1, but confirming prevents edge-case oversight).
 * - "What is the range of values in nums?" (Constraints state -100 to 100, which confirms we can use a fixed-size offset array for frequency counting).
 * - "If two numbers have the same frequency, should they be sorted in descending order of their numerical value?" (Confirms the tie-breaker rule: e.g., frequency 2 for both '5' and '3' means '5' comes before '3').
 * - "Should the sorting be done in-place or return a new sorted array?" (The prompt states "Return the sorted array", so returning a modified copy or new array is expected).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to sort an array based on two keys: primary key is frequency (ascending), and secondary key 
 * is the element value itself (descending). The binding constraint is that sorting algorithms natively 
 * only compare elements by their face value, so we must bridge the gap between "value" and "frequency".
 * 
 * --- APPROACH 1: BRUTE FORCE (Re-scanning for Frequencies on the Fly) ---
 * 1. What I'd naturally try: Write a custom sorting comparator where, for every comparison between two elements 
 *    `a` and `b`, I scan the entire array to count how many times `a` and `b` appear.
 * 2. Why it works: It correctly evaluates the frequency of any two elements on demand without extra storage.
 * 3. Why it's too slow/costly: Sorting takes O(N log N) comparisons. If each comparison takes O(N) to scan the array, 
 *    our total time complexity becomes O(N^2 log N), which is unnecessarily sluggish.
 * 4. What work is repeated: We recount the exact same numbers thousands of times across different comparisons.
 * 5. Cost Breakdown:
 *    - Time Complexity: O(N^2 log N) — because comparison takes O(N) linear scans during an O(N log N) sort.
 *    - Space Complexity: O(1) auxiliary space (aside from sorting stack space).
 * 
 * --- APPROACH 2: INTERMEDIATE (Pre-computed HashMap Frequency + Custom Sort) ---
 * 1. The Observation: We shouldn't recount frequencies during sorting. We should compute all frequencies *once* 
 *    up front into a frequency map, and then use that map in our comparator.
 * 2. How it leads to the next step: Pass 1 populates a HashMap with element frequencies. Pass 2 sorts the array 
 *    using a custom comparator that queries the map: if frequencies differ, return ascending difference; 
 *    otherwise, return descending difference of the values.
 * 3. Why it works: Reduces the lookup time inside the comparator from O(N) to O(1).
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N log N) — because building the map takes O(N), and sorting N elements takes O(N log N) 
 *      with O(1) map lookups per comparison.
 *    - Space Complexity: O(U) where U is unique elements — because we store counts in a HashMap, incurring Java 
 *      object boxing overhead (Integer wrappers).
 * 
 * --- APPROACH 3: OPTIMAL (Offset Array Frequency Map + Custom Sort) ---
 * 1. The Observation: The constraints state that `nums[i]` is between -100 and 100. This is a tiny, bounded domain 
 *    of 201 possible values. We can map [-100, 100] to [0, 200] using an offset of +100, avoiding HashMaps entirely.
 * 2. How it leads to the next step: We use an `int[201]` frequency array. Since primitive arrays cannot be sorted 
 *    with a custom comparator directly in Java, we box the array to `Integer[]` (which is trivial given $N \le 100$) 
 *    and sort using a lambda comparator backed by our O(1) offset frequency array.
 * 3. Which one I'd write in an interview: Approach 3 (Offset Array + Boxed Sort). Given the tiny constraint 
 *    ($N \le 100$), boxing overhead is completely negligible, and the offset array provides blistering O(1) lookups.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N log N) — dominated by the sorting step. With $N \le 100$, $N log N \approx 700$ operations, 
 *      making it effectively instantaneous.
 *    - Space Complexity: O(N) — because we box primitive integers into an `Integer[]` wrapper array to utilize 
 *      Java's Collections/Arrays sorting API with a custom comparator.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - All unique elements: `nums = [3, 1, 2]` -> Frequencies are all 1. Tie-breaker applies: sort descending -> `[3, 2, 1]`.
 * - All identical elements: `nums = [5, 5, 5]` -> Frequency is 3. Result -> `[5, 5, 5]`.
 * - Negative and positive mix: `nums = [-1, 1, -6, 4, 5]` -> Offset array handles negative indices safely (`val + 100`).
 * - Minimum length: `nums = [1]` -> Returns `[1]` instantly.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Dual-Criteria Sorting Logic:
 * 
 * Rule 1: Frequency Ascending  (Smaller frequency comes first)
 * Rule 2: Value     Descending (If frequencies match, larger numerical value comes first)
 * 
 * Example: nums = [1, 1, 2, 2, 2, 3]
 * Frequencies: {1: 2, 2: 3, 3: 1}
 * 
 * Sorted by Frequency Ascending:
 * Value 3 (freq 1) -> [3]
 * Value 1 (freq 2) -> [1, 1]
 * Value 2 (freq 3) -> [2, 2, 2]
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: nums = [1, 1, 2, 2, 2, 3]
 * 
 * Step 1: Count frequencies using offset array (`freq[num + 100]`)
 *   freq[101] (for 1) = 2
 *   freq[102] (for 2) = 3
 *   freq[103] (for 3) = 1
 * 
 * Step 2: Sort elements using custom comparator `(a, b)`:
 *   Compare 1 and 2: freq[1] (2) vs freq[2] (3) -> 2 - 3 < 0 -> 1 comes before 2.
 *   Compare 2 and 3: freq[2] (3) vs freq[3] (1) -> 3 - 1 > 0 -> 3 comes before 2.
 *   Compare two 2s: freq[2] (3) vs freq[2] (3) -> frequencies equal! 
 *     Tie-breaker: value descending -> `b - a` -> `2 - 2 = 0`.
 * 
 * Final Result: [3, 1, 1, 2, 2, 2]
 * 
 * =================================================================================================
 * 
 * 6. PITFALLS
 * - Forgetting the offset for negative numbers: Trying to do `freq[nums[i]]` when `nums[i]` is -100 will throw an 
 *   `ArrayIndexOutOfBoundsException`. You must add 100 (`nums[i] + 100`).
 * - Getting the tie-breaker backward: The prompt states "If multiple values share the same frequency, sort those 
 *   values in *decreasing* order." Make sure the tie-breaker is `b - a` or `Integer.compare(b, a)`, not `a - b`.
 * 
 * =================================================================================================
 * 
 * 7. INTERVIEW SCRIPT
 * "Since the values in nums range from -100 to 100, we can use a fixed-size frequency array of size 201 with 
 * an offset of +100 to achieve O(1) frequency lookups. First, we make a single pass over the array to populate 
 * our frequency table. Then, we box the array into an Integer object array and sort it using a custom comparator. 
 * The comparator checks if the frequencies differ; if so, it sorts by frequency in ascending order. If the frequencies 
 * are tied, it sorts by the values themselves in descending order. Given the constraint that N is at most 100, 
 * this takes O(N log N) time and O(N) space, which runs in a fraction of a millisecond."
 * 
 * =================================================================================================
 * 
 * 8. FOLLOW-UPS
 * Q: What if the range of `nums[i]` was expanded to -10^9 to 10^9?
 * A: The fixed-size `int[201]` offset array would fail. I would switch to a standard `HashMap<Integer, Integer>` 
 *    to store frequencies dynamically while maintaining O(N log N) sorting time.
 * 
 * Q: Can this be solved without boxing (`Integer[]`) to avoid garbage collection overhead?
 * A: Yes! Instead of sorting the original numbers directly with a comparator, we can extract the unique elements, 
 *    sort them using primitive parallel/custom routines, and then reconstruct the output array by looping through 
 *    the sorted unique elements and appending them according to their frequency.
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SortArrayByIncreasingFrequency {

    /**
     * APPROACH 3: OPTIMAL (Offset Array Frequency Map + Boxed Custom Sort)
     * Time: O(N log N), Space: O(N)
     */
    public int[] frequencySort(int[] nums) {
        // [Why needed]: Holds frequencies for values ranging from -100 to 100.
        // Size 201 maps -100 to index 0, 0 to index 100, and 100 to index 200.
        int[] freq = new int[201];

        // Phase 1: Build frequency table
        for (int i = 0; i < nums.length; i++) {
            // [What it does]: +100 shifts negative values into valid 0-200 indices.
            freq[nums[i] + 100]++;
        }

        // [Why needed]: Java primitive arrays cannot be sorted with a custom comparator directly.
        // Boxing N <= 100 elements into Integer wrappers has negligible overhead.
        Integer[] boxedNums = new Integer[nums.length];
        for (int i = 0; i < nums.length; i++) {
            boxedNums[i] = nums[i];
        }

        // Phase 2: Custom sort using Dual-Pivot Quicksort
        Arrays.sort(boxedNums, (a, b) -> {
            int freqA = freq[a + 100];
            int freqB = freq[b + 100];

            // Primary Rule: Increasing order of frequency
            if (freqA != freqB) {
                return Integer.compare(freqA, freqB);
            }
            
            // Secondary Rule (Tie-Breaker): Decreasing order of numerical value
            return Integer.compare(b, a);
        });

        // Phase 3: Unbox back into primitive array format
        for (int i = 0; i < nums.length; i++) {
            nums[i] = boxedNums[i];
        }

        return nums;
    }

    /**
     * APPROACH 2: INTERMEDIATE (HashMap Frequency + Custom Sort)
     * Time: O(N log N), Space: O(U) where U is unique elements
     */
    public int[] frequencySortHashMap(int[] nums) {
        Map<Integer, Integer> freqMap = new HashMap<>();
        for (int num : nums) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }

        Integer[] boxedNums = new Integer[nums.length];
        for (int i = 0; i < nums.length; i++) {
            boxedNums[i] = nums[i];
        }

        Arrays.sort(boxedNums, (a, b) -> {
            int freqA = freqMap.get(a);
            int freqB = freqMap.get(b);
            if (freqA != freqB) {
                return Integer.compare(freqA, freqB);
            }
            return Integer.compare(b, a);
        });

        for (int i = 0; i < nums.length; i++) {
            nums[i] = boxedNums[i];
        }

        return nums;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Frequency counting with multi-key custom sorting (Primary: freq asc, Secondary: val desc).
     * 2. Key observation: Bounded input domains (like -100 to 100) allow flat offset arrays (`val + 100`) 
     *    instead of HashMaps.
     * 3. Memorize vs Understand: Understand how to write lambdas for Java's `Arrays.sort()` comparator with tie-breakers.
     * 4. Most common trap: Forgetting the negative offset (+100) or getting the descending tie-breaker flipped (`a - b` instead of `b - a`).
     * 5. Mental trigger: "Sort by frequency" -> Frequency map + Custom Comparator (`freqA != freqB ? freqA - freqB : b - a`).
     */
    public static void main(String[] args) {
        SortArrayByIncreasingFrequency solution = new SortArrayByIncreasingFrequency();

        // Test Case 1: Standard Example
        int[] nums1 = {1, 1, 2, 2, 2, 3};
        // Frequencies: 1 appears 2 times, 2 appears 3 times, 3 appears 1 time.
        // Sorted by freq asc: 3 (freq 1), 1 (freq 2), 2 (freq 3) -> [3, 1, 1, 2, 2, 2]
        int[] res1 = solution.frequencySort(nums1);
        System.out.println("Test 1 (Standard): " + Arrays.toString(res1)); // Expected: [3, 1, 1, 2, 2, 2]

        // Test Case 2: Tie-Breaker Example (Same frequency, decreasing values)
        int[] nums2 = {2, 3, 1, 3, 2};
        // Frequencies: 1:1, 2:2, 3:2. 
        // Freq 1 -> [1]. Freq 2 tie between 2 and 3 -> sort descending -> [3, 3, 2, 2].
        // Combined -> [1, 3, 3, 2, 2]
        int[] res2 = solution.frequencySort(nums2);
        System.out.println("Test 2 (Tie-breaker): " + Arrays.toString(res2)); // Expected: [1, 3, 3, 2, 2]

        // Test Case 3: Negative Numbers
        int[] nums3 = {-1, 1, -6, 4, 5, -6, 1};
        // Frequencies: -6:2, 1:2, -1:1, 4:1, 5:1.
        // Freq 1 tie between -1, 4, 5 -> desc -> [5, 4, -1].
        // Freq 2 tie between -6, 1 -> desc -> [1, 1, -6, -6] (wait, 1 > -6 so 1 comes first).
        int[] res3 = solution.frequencySort(nums3);
        System.out.println("Test 3 (Negative numbers): " + Arrays.toString(res3));
    }
}



import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class Solution {
    public int[] frequencySort(int[] nums) {

        // Step 1: Count the frequency of each number.
        Map<Integer, Integer> frequency = new HashMap<>();

        for (int num : nums) {
            frequency.merge(num, 1, Integer::sum);
        }

        // Step 2: Sort using the two given rules.
        // Rule 1: Lower frequency comes first.
        // Rule 2: For equal frequencies, larger number comes first.
        Integer[] sorted = Arrays.stream(nums)
                .boxed()
                .toArray(Integer[]::new);

        Arrays.sort(sorted, (a, b) -> {
            int frequencyComparison = Integer.compare(
                    frequency.get(a), frequency.get(b)
            );

            if (frequencyComparison != 0) {
                return frequencyComparison;
            }

            return Integer.compare(b, a);
        });

        // Step 3: Convert Integer[] back to int[].
        return Arrays.stream(sorted)
                .mapToInt(Integer::intValue)
                .toArray();
    }
}



import java.util.Arrays;

class Solution {
    public int[] frequencySort(int[] nums) {

        // Map values [-100, 100] to indices [0, 200].
        int[] frequency = new int[201];

        for (int num : nums) {
            frequency[num + 100]++;
        }

        // Box the array because object-array sorting supports a comparator.
        Integer[] sorted = Arrays.stream(nums)
                .boxed()
                .toArray(Integer[]::new);

        Arrays.sort(sorted, (a, b) -> {
            int freqA = frequency[a + 100];
            int freqB = frequency[b + 100];

            // Lower frequency first.
            if (freqA != freqB) {
                return Integer.compare(freqA, freqB);
            }

            // Larger number first when frequencies are equal.
            return Integer.compare(b, a);
        });

        return Arrays.stream(sorted)
                .mapToInt(Integer::intValue)
                .toArray();
    }
}



import java.util.*;

public class Solution {
    public int[] frequencySort(int[] nums) {

        // Step 1: Count how many times each number appears.
        Map<Integer, Integer> freqMap = new HashMap<>();

        for (int num : nums) {
            freqMap.merge(num, 1, Integer::sum);
        }

        // Step 2: Group distinct numbers by their frequency.
        // Example: frequency 2 -> [1, 3]
        Map<Integer, List<Integer>> freqNum = new HashMap<>();

        for (var entry : freqMap.entrySet()) {
            int num = entry.getKey();
            int freq = entry.getValue();

            freqNum.computeIfAbsent(freq, k -> new ArrayList<>())
                   .add(num);
        }

        // Step 3: Process frequencies in ascending order.
        var freqNumSorted = freqNum.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();

        int[] result = new int[nums.length];
        int index = 0;

        for (var entry : freqNumSorted) {
            int freq = entry.getKey();

            // For equal frequencies, larger numbers come first.
            List<Integer> numbers = entry.getValue().stream()
                    .sorted(Comparator.reverseOrder())
                    .toList();

            // Repeat each number according to its frequency.
            for (int num : numbers) {
                for (int i = 0; i < freq; i++) {
                    result[index++] = num;
                }
            }
        }

        return result;
    }
}

