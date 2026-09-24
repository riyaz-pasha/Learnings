import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * PROBLEM STATEMENT:
 * Given an array of integers, arr, and an integer, k, return the k most frequent elements.
 * Note: You can return the answer in any order.
 *
 * CONSTRAINTS:
 * 1 <= arr.length <= 10^3
 * -10^4 <= arr[i] <= 10^4
 * 1 <= k <= number of unique elements in an array.
 * It is guaranteed that the answer is unique.
 * 
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record TestCase(...)`): Introduced in Java 14/16 for immutable data carriers.
 * - Local Variable Type Inference (`var`): Introduced in Java 10 for cleaner code.
 * - `Map.getOrDefault` and Lambda Streams for succinctness.
 * ==========================================================================================
 */
class TopKFrequentElements {

    // ==========================================================================================
    // SOLUTION 1: HashMap + Sorting (The Brute-Force/Intuitive Way)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. Count the frequency of each element using a HashMap.
     * 2. Store the unique elements (keys) in a List.
     * 3. Sort the List in descending order based on their frequencies from the HashMap.
     * 4. Take the first 'k' elements from the sorted list.
     *
     * VISUAL:
     * arr = [1, 1, 1, 2, 2, 3], k = 2
     * 
     * Map Counts:
     * 1 -> 3
     * 2 -> 2
     * 3 -> 1
     * 
     * Sort keys by frequency:
     * Keys:  [1, 2, 3]
     * Freqs: [3, 2, 1]
     * 
     * Top k=2: [1, 2]
     *
     * COMPLEXITY:
     * - Time: O(N log N) - where N is the number of unique elements (due to sorting).
     * - Space: O(N) - to store the HashMap and the List.
     */
    public static int[] topKFrequent_Sorting(int[] arr, int k) {
        var countMap = new HashMap<Integer, Integer>();
        for (int num : arr) {
            countMap.put(num, countMap.getOrDefault(num, 0) + 1);
        }

        var uniqueElements = new ArrayList<>(countMap.keySet());
        
        // Sort in descending order based on frequency
        uniqueElements.sort((a, b) -> Integer.compare(countMap.get(b), countMap.get(a)));

        var result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = uniqueElements.get(i);
        }
        return result;
    }

    // ==========================================================================================
    // SOLUTION 2: HashMap + Min-Heap (Optimal for large distinct N, small k)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. Count frequencies using a HashMap.
     * 2. Use a Min-Heap (PriorityQueue) that orders elements by their frequency.
     * 3. Add elements to the heap. If the heap size exceeds 'k', remove the smallest element 
     *    (the one with the lowest frequency so far).
     * 4. By the end, the heap contains exactly the 'k' most frequent elements.
     *
     * VISUAL:
     * arr = [1, 1, 1, 2, 2, 3], k = 2
     * Counts: {1:3, 2:2, 3:1}
     * 
     * Step 1: Add '1' (freq 3) -> Heap: [1]
     * Step 2: Add '2' (freq 2) -> Heap: [2, 1] (2 is root because 2 < 3)
     * Step 3: Add '3' (freq 1) -> Heap: [3, 2, 1] -> Size > 2! Poll root '3'. -> Heap: [2, 1]
     * 
     * Result: Pop remaining from heap: [2, 1]
     *
     * COMPLEXITY:
     * - Time: O(N log k) - N elements, heap operations take log(k).
     * - Space: O(N + k) - N for the map, k for the heap.
     */
    public static int[] topKFrequent_MinHeap(int[] arr, int k) {
        var countMap = new HashMap<Integer, Integer>();
        for (int num : arr) {
            countMap.put(num, countMap.getOrDefault(num, 0) + 1);
        }

        // Min-Heap comparing elements by their frequencies
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(Comparator.comparingInt(countMap::get));

        for (var num : countMap.keySet()) {
            minHeap.offer(num);
            if (minHeap.size() > k) {
                minHeap.poll(); // Evict the least frequent element
            }
        }

        var result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = minHeap.poll();
        }
        return result;
    }

    // ==========================================================================================
    // SOLUTION 3: Bucket Sort (Optimal Time - Best for this specific problem)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. Count frequencies using a HashMap.
     * 2. Create an array of Lists (buckets) where the *INDEX* of the array represents the *FREQUENCY*.
     *    The maximum possible frequency is the length of the array (if all elements are identical).
     * 3. Iterate through the map and place each element into the bucket corresponding to its frequency.
     * 4. Iterate through the buckets backwards (from highest frequency to lowest) and collect 'k' elements.
     *
     * VISUAL:
     * arr = [1, 1, 1, 2, 2, 3], k = 2
     * Counts: {1:3, 2:2, 3:1}
     * 
     * Buckets (Index = Frequency):
     * [0] -> []
     * [1] -> [3]    (Element 3 appears 1 time)
     * [2] -> [2]    (Element 2 appears 2 times)
     * [3] -> [1]    (Element 1 appears 3 times)
     * [4] -> []
     * [5] -> []
     * [6] -> []
     * 
     * Walk backwards from index 6:
     * Collect '1' from bucket 3.
     * Collect '2' from bucket 2.
     * Reached k=2 elements! Stop.
     *
     * COMPLEXITY:
     * - Time: O(N) - N to count, N to put in buckets, N to gather results. Linear Time!
     * - Space: O(N) - N for the map and N for the buckets array.
     */
    public static int[] topKFrequent_BucketSort(int[] arr, int k) {
        var countMap = new HashMap<Integer, Integer>();
        for (int num : arr) {
            countMap.put(num, countMap.getOrDefault(num, 0) + 1);
        }

        // Array of lists where index = frequency
        @SuppressWarnings("unchecked")
        List<Integer>[] buckets = new List[arr.length + 1];
        
        for (var entry : countMap.entrySet()) {
            int num = entry.getKey();
            int freq = entry.getValue();
            if (buckets[freq] == null) {
                buckets[freq] = new ArrayList<>();
            }
            buckets[freq].add(num);
        }

        var result = new int[k];
        int index = 0;
        
        // Traverse buckets from highest possible frequency to lowest
        for (int i = buckets.length - 1; i >= 0 && index < k; i--) {
            if (buckets[i] != null) {
                for (int num : buckets[i]) {
                    result[index++] = num;
                    if (index == k) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    // ==========================================================================================
    // SOLUTION 4: QuickSelect (Optimal Average Time & Space)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We map unique elements to an array, then run QuickSelect based on their frequencies.
     * QuickSelect partitions the unique elements such that the top 'k' frequent ones end up
     * on the right side of our pivot. We don't care about their specific order.
     *
     * COMPLEXITY:
     * - Time: O(N) Average Time, O(N^2) Worst case (highly unlikely with randomized pivot).
     * - Space: O(N) for HashMap and unique elements array.
     */
    public static int[] topKFrequent_QuickSelect(int[] arr, int k) {
        var countMap = new HashMap<Integer, Integer>();
        for (int num : arr) {
            countMap.put(num, countMap.getOrDefault(num, 0) + 1);
        }

        var unique = new int[countMap.size()];
        int idx = 0;
        for (int num : countMap.keySet()) {
            unique[idx++] = num;
        }

        // We want the top k elements. Since we are sorting essentially ascending, 
        // the target index for the pivot to split top k is: length - k
        int n = unique.length;
        quickSelect(unique, countMap, 0, n - 1, n - k, new Random());

        return Arrays.copyOfRange(unique, n - k, n);
    }

    private static void quickSelect(int[] unique, Map<Integer, Integer> countMap, 
                                    int left, int right, int targetIndex, Random random) {
        if (left >= right) return;

        int pivotIndex = left + random.nextInt(right - left + 1);
        pivotIndex = partition(unique, countMap, left, right, pivotIndex);

        if (pivotIndex == targetIndex) {
            return;
        } else if (pivotIndex < targetIndex) {
            quickSelect(unique, countMap, pivotIndex + 1, right, targetIndex, random);
        } else {
            quickSelect(unique, countMap, left, pivotIndex - 1, targetIndex, random);
        }
    }

    private static int partition(int[] unique, Map<Integer, Integer> countMap, 
                                 int left, int right, int pivotIndex) {
        int pivotFreq = countMap.get(unique[pivotIndex]);
        swap(unique, pivotIndex, right); // move pivot to end
        
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (countMap.get(unique[i]) < pivotFreq) {
                swap(unique, storeIndex, i);
                storeIndex++;
            }
        }
        swap(unique, storeIndex, right); // move pivot to its final place
        return storeIndex;
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    public record TestCase(int[] arr, int k, int[] expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[]{1, 1, 1, 2, 2, 3}, 2, new int[]{1, 2}),
            new TestCase(new int[]{1}, 1, new int[]{1}),
            new TestCase(new int[]{4, 4, 4, 4, -1, -1, 3, 3, 3}, 2, new int[]{4, 3}),
            new TestCase(new int[]{10, 10, 20, 20, 30, 30, 40, 40, 40}, 1, new int[]{40})
        };

        System.out.println("Running Top K Frequent Elements Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Array: %s | k: %d\n", Arrays.toString(tc.arr()), tc.k());

            var res1 = topKFrequent_Sorting(tc.arr(), tc.k());
            var res2 = topKFrequent_MinHeap(tc.arr(), tc.k());
            var res3 = topKFrequent_BucketSort(tc.arr(), tc.k());
            var res4 = topKFrequent_QuickSelect(tc.arr(), tc.k());

            // Since answers can be in any order, we sort them before comparing
            Arrays.sort(res1); Arrays.sort(res2); Arrays.sort(res3); Arrays.sort(res4);
            var expectedSorted = tc.expected().clone();
            Arrays.sort(expectedSorted);

            boolean pass1 = Arrays.equals(res1, expectedSorted);
            boolean pass2 = Arrays.equals(res2, expectedSorted);
            boolean pass3 = Arrays.equals(res3, expectedSorted);
            boolean pass4 = Arrays.equals(res4, expectedSorted);

            System.out.printf("  [1. Sorting    ] Result: %s -> %s\n", Arrays.toString(res1), pass1 ? "PASS" : "FAIL");
            System.out.printf("  [2. Min-Heap   ] Result: %s -> %s\n", Arrays.toString(res2), pass2 ? "PASS" : "FAIL");
            System.out.printf("  [3. BucketSort ] Result: %s -> %s\n", Arrays.toString(res3), pass3 ? "PASS" : "FAIL");
            System.out.printf("  [4. QuickSelect] Result: %s -> %s\n", Arrays.toString(res4), pass4 ? "PASS" : "FAIL");
            System.out.println("-".repeat(60));
        }
    }
}

/**
 * Problem:
 * Return the k most frequent elements from the array.
 *
 * Approach:
 * 1. Count frequency using HashMap
 * 2. Use a Min Heap (size = k) based on frequency
 * 3. Keep only top k frequent elements in heap
 * 4. Extract elements from heap
 */
public class FrequentElements {

    /**
     * Record to store (number, frequency)
     * Cleaner than int[] and improves readability
     */
    record Pair(int num, int freq) {}

    public static List<Integer> topKFrequent(int[] arr, int k) {

        // -------------------------------
        // Step 1: Build Frequency Map
        // -------------------------------
        // Key = number, Value = frequency
        Map<Integer, Integer> freqMap = new HashMap<>();

        for (int num : arr) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }

        // -------------------------------
        // Step 2: Min Heap (based on frequency)
        // -------------------------------
        // Smallest frequency stays on top
        // Heap size will be maintained as k
        PriorityQueue<Pair> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(p -> p.freq));

        // -------------------------------
        // Step 3: Process each entry
        // -------------------------------
        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {

            // Add current (number, frequency)
            minHeap.offer(new Pair(entry.getKey(), entry.getValue()));

            // If heap size exceeds k, remove least frequent element
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        // -------------------------------
        // Step 4: Extract result
        // -------------------------------
        // Heap contains k most frequent elements
        List<Integer> result = new ArrayList<>();

        while (!minHeap.isEmpty()) {
            result.add(minHeap.poll().num);
        }

        // Optional: reverse if you want highest freq first
        Collections.reverse(result);

        return result;
    }
}

/**
 * Problem:
 * Return the k most frequent elements from the array.
 *
 * Approach: Bucket Sort (Optimal)
 *
 * Key Idea:
 * Frequency of any element lies between 1 and n (array length).
 * So we can group numbers by their frequency using buckets.
 *
 * Steps:
 * 1. Count frequencies using HashMap
 * 2. Create buckets where index = frequency
 * 3. Place numbers into corresponding buckets
 * 4. Traverse buckets from high → low frequency
 * 5. Collect first k elements
 */
class FrequentElements2 {

    public static List<Integer> topKFrequent(int[] arr, int k) {

        // -------------------------------
        // Step 1: Build Frequency Map
        // -------------------------------
        // Key = number, Value = frequency
        Map<Integer, Integer> freqMap = new HashMap<>();

        for (int num : arr) {
            freqMap.put(num, freqMap.getOrDefault(num, 0) + 1);
        }

        // -------------------------------
        // Step 2: Create Buckets
        // -------------------------------
        // Index = frequency
        // bucket[i] = list of numbers with frequency i
        List<Integer>[] bucket = new List[arr.length + 1];

        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            int num = entry.getKey();
            int freq = entry.getValue();

            if (bucket[freq] == null) {
                bucket[freq] = new ArrayList<>();
            }

            bucket[freq].add(num);
        }

        // -------------------------------
        // Step 3: Collect Top K Elements
        // -------------------------------
        // Traverse from highest frequency to lowest
        List<Integer> result = new ArrayList<>();

        for (int freq = bucket.length - 1; freq >= 0 && result.size() < k; freq--) {

            if (bucket[freq] != null) {

                for (int num : bucket[freq]) {
                    result.add(num);

                    // Stop once we have k elements
                    if (result.size() == k) {
                        return result;
                    }
                }
            }
        }

        return result; // fallback (though we always return inside loop)
    }
}
