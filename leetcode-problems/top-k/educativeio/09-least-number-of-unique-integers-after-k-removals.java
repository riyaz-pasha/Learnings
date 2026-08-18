import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * PROBLEM STATEMENT:
 * You are given an integer array, arr, and an integer, k. 
 * Your task is to remove exactly k elements from the array so that the number of 
 * distinct integers remaining in the array is minimized. 
 * Determine the minimum possible count of unique integers after the removals.
 *
 * CONSTRAINTS:
 * 1 <= arr.length <= 10^3
 * 1 <= arr[i] <= 10^5
 * 0 <= k <= arr.length
 * 
 * ==========================================================================================
 * CRITICAL INSIGHT - GREEDY ELIMINATION:
 * To minimize the number of unique integers remaining, we must aim to completely eliminate 
 * certain integers from the array. 
 * 
 * It requires fewer removals to completely eliminate an integer that appears LESS frequently. 
 * Therefore, our optimal (Greedy) strategy is:
 * 1. Count the frequency of each unique integer in the array.
 * 2. Sort the frequencies in ascending order.
 * 3. Starting with the least frequent integers, "spend" our `k` removals to eliminate them.
 * 4. Stop when `k` is no longer large enough to remove all instances of the next integer.
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record TestCase(...)`): For defining clean immutable test scenarios.
 * - Local Variable Type Inference (`var`): To reduce boilerplate while keeping type safety.
 * ==========================================================================================
 */
class LeastNumberOfUniqueIntegers {

    // ==========================================================================================
    // SOLUTION 1: HashMap + Sorting (Intuitive & General Purpose)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. Traverse the array and use a HashMap to count the frequency of each integer.
     * 2. Extract the frequencies into a List.
     * 3. Sort the List in ascending order.
     * 4. Iterate over the sorted frequencies, subtracting them from `k`.
     *    - If k >= frequency, we completely eliminate that integer.
     *    - If k < frequency, we can't eliminate it, so we break out of the loop.
     *
     * VISUAL:
     * arr = [4, 3, 1, 1, 3, 3, 2], k = 3
     * 
     * 1. Frequencies: 
     *    4 -> 1
     *    2 -> 1
     *    1 -> 2
     *    3 -> 3
     * 
     * 2. Sorted Frequencies: [1, 1, 2, 3]  (representing elements 4, 2, 1, 3)
     *    Initial unique count = 4.
     * 
     * 3. Spend k:
     *    - Try to remove freq 1 (element 4). k = 3 - 1 = 2. Unique count = 3.
     *    - Try to remove freq 1 (element 2). k = 2 - 1 = 1. Unique count = 2.
     *    - Try to remove freq 2 (element 1). k = 1. Not enough! Break.
     * 
     * Final unique count = 2.
     *
     * COMPLEXITY:
     * - Time: O(N log N) - N to count, and sorting the unique frequencies takes U log U (where U <= N).
     * - Space: O(N) - to store the Map and the List of frequencies.
     */
    public static int findLeastNumOfUniqueInts_Sorting(int[] arr, int k) {
        var counts = new HashMap<Integer, Integer>();
        for (int num : arr) {
            counts.put(num, counts.getOrDefault(num, 0) + 1);
        }

        var freqs = new ArrayList<>(counts.values());
        Collections.sort(freqs);

        int uniqueCount = freqs.size();
        for (int freq : freqs) {
            if (k >= freq) {
                k -= freq;
                uniqueCount--;
            } else {
                // Not enough k to completely remove the next element
                break;
            }
        }
        return uniqueCount;
    }

    // ==========================================================================================
    // SOLUTION 2: Min-Heap / PriorityQueue (Clean and efficient for early exit)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Instead of fully sorting an array, we throw all the frequencies into a Min-Heap.
     * The smallest frequency is always at the top. We repeatedly pop from the heap as long 
     * as `k` is sufficient to cover the frequency. The remaining size of the heap is our answer.
     *
     * COMPLEXITY:
     * - Time: O(N + U log U) - N to count, U log U to build and extract from the heap.
     * - Space: O(N) - to store the Map and PriorityQueue.
     */
    public static int findLeastNumOfUniqueInts_MinHeap(int[] arr, int k) {
        var counts = new HashMap<Integer, Integer>();
        for (int num : arr) {
            counts.put(num, counts.getOrDefault(num, 0) + 1);
        }

        // Initialize a Min-Heap with all the frequency values
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(counts.values());

        // Process elements while we still have enough 'k'
        while (!minHeap.isEmpty() && k >= minHeap.peek()) {
            k -= minHeap.poll(); // Subtract the frequency and remove it from the heap
        }

        // The remaining elements in the heap are the remaining unique integers
        return minHeap.size();
    }

    // ==========================================================================================
    // SOLUTION 3: Counting Array + Sorting (Optimal for bounded inputs)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * The problem constraints state that arr[i] <= 10^5. This is small enough that we can avoid
     * the overhead of a HashMap entirely by using a fixed-size integer array to count frequencies.
     * We then collect all non-zero frequencies into a small array, sort it, and apply the greedy logic.
     *
     * COMPLEXITY:
     * - Time: O(N + MaxValue) -> O(N + 10^5) -> Effectively O(N). Sorting only happens on the valid frequencies.
     * - Space: O(MaxValue) -> An array of size 100,001 integers is just ~400 KB, which is very fast.
     */
    public static int findLeastNumOfUniqueInts_CountingArray(int[] arr, int k) {
        final int MAX_VAL = 100000;
        var counts = new int[MAX_VAL + 1];
        
        int uniqueCount = 0;
        for (int num : arr) {
            if (counts[num] == 0) {
                uniqueCount++;
            }
            counts[num]++;
        }

        // Gather only the non-zero frequencies
        var freqs = new int[uniqueCount];
        int idx = 0;
        for (int count : counts) {
            if (count > 0) {
                freqs[idx++] = count;
            }
        }

        Arrays.sort(freqs);

        int remainingUnique = uniqueCount;
        for (int freq : freqs) {
            if (k >= freq) {
                k -= freq;
                remainingUnique--;
            } else {
                break;
            }
        }

        return remainingUnique;
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    // Java 14+ Record for grouping test case parameters cleanly
    public record TestCase(int[] arr, int k, int expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[]{5, 5, 4}, 1, 1), 
            // Frequencies: 5:2, 4:1. Remove '4' (k=1). Remaining: [5,5] -> 1 unique.
            
            new TestCase(new int[]{4, 3, 1, 1, 3, 3, 2}, 3, 2), 
            // Frequencies: 3:3, 1:2, 4:1, 2:1. Remove '4', '2', and one '1' (Wait, k=3 means remove 4,2, and we can't fully remove 1. Remaining unique elements: 3, 1. Count = 2.
            
            new TestCase(new int[]{1, 2, 3, 4, 5}, 3, 2), 
            // Remove any 3 elements. Remaining 2 unique elements.
            
            new TestCase(new int[]{100000, 100000, 100000}, 0, 1), 
            // Remove 0 elements. Remaining unique elements: 1.
            
            new TestCase(new int[]{1, 1, 2, 2, 3, 3}, 6, 0)
            // Remove all 6 elements. Remaining unique elements: 0.
        };

        System.out.println("Running Least Number of Unique Integers Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Array: %s | k: %d | Expected: %d\n", Arrays.toString(tc.arr()), tc.k(), tc.expected());

            // Run HashMap + Sorting Solution
            int res1 = findLeastNumOfUniqueInts_Sorting(tc.arr(), tc.k());
            boolean pass1 = (res1 == tc.expected());
            System.out.printf("  [1. Map + Sorting ] Result: %d -> %s\n", res1, pass1 ? "PASS" : "FAIL");

            // Run Min-Heap Solution
            int res2 = findLeastNumOfUniqueInts_MinHeap(tc.arr(), tc.k());
            boolean pass2 = (res2 == tc.expected());
            System.out.printf("  [2. Min-Heap      ] Result: %d -> %s\n", res2, pass2 ? "PASS" : "FAIL");

            // Run Counting Array Solution
            int res3 = findLeastNumOfUniqueInts_CountingArray(tc.arr(), tc.k());
            boolean pass3 = (res3 == tc.expected());
            System.out.printf("  [3. Counting Array] Result: %d -> %s\n", res3, pass3 ? "PASS" : "FAIL");

            System.out.println("-".repeat(60));
        }
    }
}

class Solution {

    public int findLeastNumOfUniqueInts(int[] arr, int k) {

        /*
         * 🎯 Goal:
         * Remove exactly k elements such that number of DISTINCT integers is minimized.
         *
         * 💡 Strategy:
         * Always remove elements with the SMALLEST frequency first.
         *
         * Why?
         * - If a number appears 1 time → removing it costs 1 and reduces 1 unique
         * - If a number appears 5 times → removing it costs 5 but still reduces only 1 unique
         *
         * 👉 So, greedy choice = remove smallest frequency elements first
         */


        // -------------------------------
        // Step 1: Count frequency of each number
        // -------------------------------
        Map<Integer, Integer> freqMap = new HashMap<>();

        for (int num : arr) {
            // Increment frequency
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }


        /*
         * Example:
         * arr = [5,5,4,6,6,6]
         * freqMap = {5:2, 4:1, 6:3}
         */


        // -------------------------------
        // Step 2: Build a Min Heap of frequencies
        // -------------------------------
        /*
         * Min Heap ensures we always process the smallest frequency first
         *
         * Heap content (example): [1, 2, 3]
         */
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(freqMap.values());


        // Total unique elements initially
        int uniqueCount = freqMap.size();


        // -------------------------------
        // Step 3: Greedily remove elements
        // -------------------------------
        /*
         * Keep removing smallest frequency elements until:
         * - we run out of k
         * - OR heap becomes empty
         */
        while (k > 0 && !minHeap.isEmpty()) {

            // Get smallest frequency element
            int freq = minHeap.poll();

            /*
             * If we have enough k to completely remove this number:
             * (i.e., remove ALL occurrences of this number)
             */
            if (k >= freq) {

                // Use k operations to remove it
                k -= freq;

                // One distinct number removed
                uniqueCount--;

            } else {
                /*
                 * If we cannot fully remove this number,
                 * we must stop.
                 *
                 * Why?
                 * Partial removal does NOT reduce distinct count.
                 */
                break;
            }
        }


        // Remaining distinct elements
        return uniqueCount;
    }
}


class Solution {

    public int findLeastNumOfUniqueInts(int[] arr, int k) {

        /*
         * 🎯 Goal:
         * Remove exactly k elements such that DISTINCT integers remaining are minimized.
         *
         * 💡 Core Idea (Greedy):
         * Remove numbers with the SMALLEST frequency first.
         *
         * ⚡ Optimization Insight:
         * Instead of sorting frequencies or using a heap,
         * we use BUCKET SORT because:
         *
         * Frequency range = [1, n]
         * So we can group numbers by frequency.
         */


        // -------------------------------
        // Step 1: Build frequency map
        // -------------------------------
        Map<Integer, Integer> freqMap = new HashMap<>();

        for (int num : arr) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }

        /*
         * Example:
         * arr = [5,5,4,6,6,6]
         * freqMap = {5:2, 4:1, 6:3}
         */


        int n = arr.length;


        // -------------------------------
        // Step 2: Build bucket array
        // -------------------------------
        /*
         * bucket[i] = how many numbers have frequency i
         *
         * Example:
         * freqMap values = [2,1,3]
         *
         * bucket[1] = 1  (one number appears once → 4)
         * bucket[2] = 1  (one number appears twice → 5)
         * bucket[3] = 1  (one number appears thrice → 6)
         */
        int[] bucket = new int[n + 1];

        for (int freq : freqMap.values()) {
            bucket[freq]++;
        }


        // Total distinct elements initially
        int uniqueCount = freqMap.size();


        // -------------------------------
        // Step 3: Remove elements greedily
        // -------------------------------
        /*
         * Start from smallest frequency (1 → n)
         *
         * Why?
         * Because removing smaller frequency numbers is cheaper
         * and reduces distinct count faster.
         */
        for (int freq = 1; freq <= n; freq++) {

            /*
             * bucket[freq] tells how many numbers have this frequency
             *
             * We try to remove all such numbers if possible
             */
            while (bucket[freq] > 0 && k >= freq) {

                /*
                 * Remove one number completely:
                 * - It costs "freq" removals
                 * - Reduces 1 unique number
                 */
                k -= freq;

                // One number of this frequency is removed
                bucket[freq]--;

                // Reduce distinct count
                uniqueCount--;
            }

            /*
             * If k becomes smaller than freq,
             * we cannot remove any more numbers of this frequency
             */
        }


        // Remaining distinct elements
        return uniqueCount;
    }
}
