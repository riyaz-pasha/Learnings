import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * PROBLEM STATEMENT:
 * You are given an integer array nums and an integer k. 
 * Your task is to find a subsequence of nums of length k that has the largest possible sum.
 * 
 * Note: A subsequence is an array that can be derived from another array by deleting some 
 * or no elements without changing the order of the remaining elements.
 *
 * CONSTRAINTS:
 * 1 <= nums.length <= 1000
 * -10^5 <= nums[i] <= 10^5
 * 1 <= k <= nums.length
 * 
 * ==========================================================================================
 * CRITICAL INSIGHT:
 * The problem requires a SUBSEQUENCE. This means two things:
 * 1. We need the 'k' largest elements to maximize the sum.
 * 2. We MUST preserve their original relative order from the input array.
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record Element(...)`): For clean, immutable data carriers.
 * - Local Variable Type Inference (`var`): Clean syntax.
 * ==========================================================================================
 */
class MaxSumSubsequence {

    // Helper record to store both value and original index
    private record Element(int value, int originalIndex) {}

    // ==========================================================================================
    // SOLUTION 1: Full Sorting (Intuitive & Easy to Implement)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. Create an array of 'Element' records linking each value to its original index.
     * 2. Sort this array in descending order based on the values to find the top 'k' elements.
     * 3. Take those first 'k' elements and re-sort them based on their original index.
     * 4. Extract the values into the result array.
     *
     * VISUAL:
     * nums = [2, 1, 3, 3], k = 2
     * 
     * Step 1: Pair with indices -> [(2,0), (1,1), (3,2), (3,3)]
     * Step 2: Sort by value DESC -> [(3,2), (3,3), (2,0), (1,1)]
     * Step 3: Take top k=2 elements -> [(3,2), (3,3)]
     * Step 4: Re-sort by index ASC -> [(3,2), (3,3)]  (Already sorted in this case)
     * Result: [3, 3]
     *
     * COMPLEXITY:
     * - Time: O(N log N) for the initial sort + O(k log k) for re-sorting the top k.
     * - Space: O(N) to store the elements with their indices.
     */
    public static int[] maxSubsequence_Sorting(int[] nums, int k) {
        int n = nums.length;
        var elements = new Element[n];
        for (int i = 0; i < n; i++) {
            elements[i] = new Element(nums[i], i);
        }

        // Sort descending by value
        Arrays.sort(elements, (a, b) -> Integer.compare(b.value(), a.value()));

        // Take top k elements
        var topK = new Element[k];
        System.arraycopy(elements, 0, topK, 0, k);

        // Sort the top k elements by their original index to maintain subsequence order
        Arrays.sort(topK, Comparator.comparingInt(Element::originalIndex));

        // Extract values
        var result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = topK[i].value();
        }
        
        return result;
    }

    // ==========================================================================================
    // SOLUTION 2: Min-Heap (Optimal for large N and small k)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We use a PriorityQueue (Min-Heap) that compares Elements by their values.
     * We maintain the heap size at 'k'. After processing all elements, the heap contains
     * the 'k' largest elements along with their original indices.
     * We then pop them out, sort them by index, and return the values.
     *
     * VISUAL:
     * nums = [-1, -2, 3, 4], k = 3
     * 
     * Heap progression (size 3 limit):
     * Add (-1,0) -> [(-1,0)]
     * Add (-2,1) -> [(-2,1), (-1,0)]
     * Add (3,2)  -> [(-2,1), (-1,0), (3,2)]
     * Add (4,3)  -> Size > 3! Pop smallest (-2,1) -> Heap: [(-1,0), (4,3), (3,2)]
     * 
     * Remaining elements: (-1,0), (4,3), (3,2).
     * Sorted by index: (-1,0), (3,2), (4,3). Result: [-1, 3, 4].
     *
     * COMPLEXITY:
     * - Time: O(N log k) to build heap + O(k log k) to sort by index.
     * - Space: O(k) for the Priority Queue. (Much better space than full sort if k << N)
     */
    public static int[] maxSubsequence_MinHeap(int[] nums, int k) {
        // Min-Heap based on value
        PriorityQueue<Element> minHeap = new PriorityQueue<>(Comparator.comparingInt(Element::value));

        for (int i = 0; i < nums.length; i++) {
            minHeap.offer(new Element(nums[i], i));
            if (minHeap.size() > k) {
                minHeap.poll(); // Evict the smallest element
            }
        }

        // Transfer heap elements to an array and sort by index
        var topK = new Element[k];
        for (int i = 0; i < k; i++) {
            topK[i] = minHeap.poll();
        }
        
        Arrays.sort(topK, Comparator.comparingInt(Element::originalIndex));

        var result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = topK[i].value();
        }
        
        return result;
    }

    // ==========================================================================================
    // SOLUTION 3: QuickSelect with Single-Pass Reconstruction (Best Theoretical Time)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. We find the kth largest element (let's call it 'threshold') using QuickSelect in O(N) time.
     * 2. We count how many elements in the original array are STRICTLY GREATER than the threshold.
     * 3. The difference between 'k' and this count tells us exactly how many times we need 
     *    to include the threshold value itself to complete our length 'k'.
     * 4. We iterate through the original array left-to-right (automatically preserving order!):
     *    - If num > threshold: Add it.
     *    - If num == threshold and we still need it: Add it and decrement our 'needed' counter.
     *
     * COMPLEXITY:
     * - Time: O(N) average for QuickSelect + O(N) for final pass = O(N) Average Time!
     * - Space: O(N) for the copy array used in QuickSelect.
     */
    public static int[] maxSubsequence_QuickSelect(int[] nums, int k) {
        var copy = nums.clone();
        int threshold = quickSelect(copy, 0, copy.length - 1, copy.length - k, new Random());

        // Count how many are strictly greater than threshold
        int greaterCount = 0;
        for (var num : nums) {
            if (num > threshold) {
                greaterCount++;
            }
        }

        // How many threshold values we need to include to exactly reach size k
        int neededThresholds = k - greaterCount;

        var result = new int[k];
        int idx = 0;

        // Reconstruct maintaining order
        for (var num : nums) {
            if (num > threshold) {
                result[idx++] = num;
            } else if (num == threshold && neededThresholds > 0) {
                result[idx++] = num;
                neededThresholds--;
            }
        }

        return result;
    }

    private static int quickSelect(int[] arr, int left, int right, int targetIndex, Random random) {
        if (left == right) return arr[left];

        int pivotIndex = left + random.nextInt(right - left + 1);
        pivotIndex = partition(arr, left, right, pivotIndex);

        if (pivotIndex == targetIndex) {
            return arr[pivotIndex];
        } else if (pivotIndex < targetIndex) {
            return quickSelect(arr, pivotIndex + 1, right, targetIndex, random);
        } else {
            return quickSelect(arr, left, pivotIndex - 1, targetIndex, random);
        }
    }

    private static int partition(int[] arr, int left, int right, int pivotIndex) {
        int pivotValue = arr[pivotIndex];
        swap(arr, pivotIndex, right);
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (arr[i] < pivotValue) {
                swap(arr, storeIndex, i);
                storeIndex++;
            }
        }
        swap(arr, storeIndex, right);
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
    
    public record TestCase(int[] nums, int k, int[] expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[]{2, 1, 3, 3}, 2, new int[]{3, 3}),
            new TestCase(new int[]{-1, -2, 3, 4}, 3, new int[]{-1, 3, 4}),
            new TestCase(new int[]{3, 4, 3, 3}, 2, new int[]{4, 3}), // 4 and the first 3
            new TestCase(new int[]{50, -50, 50}, 2, new int[]{50, 50}),
            new TestCase(new int[]{1}, 1, new int[]{1})
        };

        System.out.println("Running Subsequence Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Array: %s | k: %d\n", Arrays.toString(tc.nums()), tc.k());

            var res1 = maxSubsequence_Sorting(tc.nums(), tc.k());
            var res2 = maxSubsequence_MinHeap(tc.nums(), tc.k());
            var res3 = maxSubsequence_QuickSelect(tc.nums(), tc.k());

            boolean pass1 = Arrays.equals(res1, tc.expected());
            boolean pass2 = Arrays.equals(res2, tc.expected());
            boolean pass3 = Arrays.equals(res3, tc.expected());

            System.out.printf("  [1. Sorting    ] Result: %s -> %s\n", Arrays.toString(res1), pass1 ? "PASS" : "FAIL");
            System.out.printf("  [2. Min-Heap   ] Result: %s -> %s\n", Arrays.toString(res2), pass2 ? "PASS" : "FAIL");
            System.out.printf("  [3. QuickSelect] Result: %s -> %s\n", Arrays.toString(res3), pass3 ? "PASS" : "FAIL");
            System.out.println("-".repeat(60));
        }
    }
}

class Solution {

    // Record to store value + index (cleaner than int[])
    record Pair(int value, int index) {}

    public int[] maxSubsequence(int[] nums, int k) {

        // Min Heap (smallest value at top)
        // This helps us remove smaller elements when size > k
        PriorityQueue<Pair> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(Pair::value));

        // Step 1: Traverse array and maintain top k elements
        for (int i = 0; i < nums.length; i++) {

            // Add current element with its index
            minHeap.offer(new Pair(nums[i], i));

            // If heap size exceeds k → remove smallest element
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        // Step 2: Convert heap → list (for sorting)
        List<Pair> selected = new ArrayList<>(minHeap);

        // Step 3: Sort by index to maintain original order
        selected.sort(Comparator.comparingInt(Pair::index));

        // Step 4: Build result array
        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = selected.get(i).value();
        }

        return result;
    }
}
