import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * PROBLEM STATEMENT:
 * Given an integer array, nums, and an integer, k, determine and return the kth largest element in the array.
 * Note: The kth largest element is defined with respect to the array’s sorted order (descending), 
 * and does not necessarily correspond to the kth unique value.
 *
 * CONSTRAINTS:
 * 1 <= k <= nums.length <= 10^3
 * -10^4 <= nums[i] <= 10^4
 * 
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record TestCase(...)`): Introduced in Java 14/16 to create immutable data carriers easily.
 * - Local Variable Type Inference (`var`): Introduced in Java 10 for cleaner code.
 * ==========================================================================================
 */
class KthLargestElement {

    // ==========================================================================================
    // SOLUTION 1: Sorting (The Brute-Force/Intuitive Way)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Sorting the array places elements in ascending order. 
     * The 1st largest element is at the last index (N-1).
     * The kth largest element will be at index (N - k).
     *
     * VISUAL:
     * nums = [3, 2, 1, 5, 6, 4], k = 2
     * 
     * 1. Sort the array ascending:
     *    [1, 2, 3, 4, 5, 6]
     *     ^  ^  ^  ^  ^  ^
     * Idx 0  1  2  3  4  5
     * 
     * 2. Target index = N - k = 6 - 2 = 4
     *    Element at index 4 is '5'.
     *
     * COMPLEXITY:
     * - Time: O(N log N) - due to the sorting algorithm (Dual-Pivot Quicksort in Java).
     * - Space: O(1) or O(N) - depending on the JVM's implementation of Arrays.sort for primitives.
     */
    public static int findKthLargest_Sorting(int[] nums, int k) {
        // Cloning array to avoid modifying the original data for our test suite
        var copy = nums.clone();
        Arrays.sort(copy);
        return copy[copy.length - k];
    }

    // ==========================================================================================
    // SOLUTION 2: Min-Heap (Optimal for Streaming Data / Very large N, small k)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We maintain a Min-Heap of size k. 
     * As we iterate through the array, we add elements to the heap. 
     * If the heap size exceeds k, we remove the smallest element (the root).
     * At the end, the heap contains exactly the 'k' largest elements of the array.
     * Since it's a Min-Heap, the smallest among those 'k' elements sits at the root,
     * which is exactly the kth largest element overall!
     *
     * VISUAL:
     * nums = [3, 2, 1, 5, 6, 4], k = 2
     * 
     * Step 1: Add 3 -> Heap: [3]
     * Step 2: Add 2 -> Heap: [2, 3]
     * Step 3: Add 1 -> Heap: [1, 2, 3] -> Size > 2, poll root (1) -> Heap: [2, 3]
     * Step 4: Add 5 -> Heap: [2, 3, 5] -> Size > 2, poll root (2) -> Heap: [3, 5]
     * Step 5: Add 6 -> Heap: [3, 5, 6] -> Size > 2, poll root (3) -> Heap: [5, 6]
     * Step 6: Add 4 -> Heap: [4, 5, 6] -> Size > 2, poll root (4) -> Heap: [5, 6]
     * 
     * Final Root (Peek) = 5.
     *
     * COMPLEXITY:
     * - Time: O(N log k) - N elements, each insertion/deletion takes log(k) time.
     * - Space: O(k) - to store elements in the Priority Queue.
     */
    public static int findKthLargest_MinHeap(int[] nums, int k) {
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        for (var num : nums) {
            minHeap.offer(num);
            if (minHeap.size() > k) {
                minHeap.poll(); // Remove the smallest element
            }
        }
        return minHeap.peek(); // The root is the kth largest
    }

    // ==========================================================================================
    // SOLUTION 3: QuickSelect (Optimal Average Time)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Similar to QuickSort. We pick a 'pivot' and partition the array into:
     * [ Elements < Pivot ] Pivot [ Elements >= Pivot ]
     * 
     * After partitioning, the pivot is in its final sorted position.
     * If the pivot's index matches the index we are looking for (N - k), we are done!
     * If (N - k) is less than the pivot index, we only recursively search the left side.
     * If (N - k) is greater, we only recursively search the right side.
     *
     * VISUAL:
     * nums = [3, 2, 1, 5, 6, 4], k = 2. Target index = 6 - 2 = 4.
     * 
     * Pivot = 4. Partitioning array:
     * [3, 2, 1] 4 [6, 5]
     *  ^        ^   ^
     * Left      Idx Right
     * Pivot ends up at index 3. 
     * Target (4) > 3, so we only search the right side: [6, 5]
     * Next Pivot = 5. 
     * [5] 6 [] -> 5 ends up at index 4.
     * Index 4 == Target (4). We found it: 5!
     *
     * COMPLEXITY:
     * - Time: O(N) average case, O(N^2) worst case (if already sorted and bad pivot picked).
     *         We randomize the pivot to virtually eliminate the worst case.
     * - Space: O(1) auxiliary space (ignoring the recursion stack / O(log N)).
     */
    public static int findKthLargest_QuickSelect(int[] nums, int k) {
        var copy = nums.clone(); // Clone to avoid side-effects
        int targetIndex = copy.length - k;
        return quickSelect(copy, 0, copy.length - 1, targetIndex, new Random());
    }

    private static int quickSelect(int[] nums, int left, int right, int targetIndex, Random random) {
        if (left == right) {
            return nums[left];
        }

        // Random pivot to avoid O(N^2) worst case on already sorted arrays
        int pivotIndex = left + random.nextInt(right - left + 1);
        pivotIndex = partition(nums, left, right, pivotIndex);

        if (pivotIndex == targetIndex) {
            return nums[pivotIndex];
        } else if (pivotIndex < targetIndex) {
            return quickSelect(nums, pivotIndex + 1, right, targetIndex, random);
        } else {
            return quickSelect(nums, left, pivotIndex - 1, targetIndex, random);
        }
    }

    private static int partition(int[] nums, int left, int right, int pivotIndex) {
        int pivotValue = nums[pivotIndex];
        // Move pivot to the end
        swap(nums, pivotIndex, right);
        
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (nums[i] < pivotValue) {
                swap(nums, storeIndex, i);
                storeIndex++;
            }
        }
        // Move pivot to its final place
        swap(nums, storeIndex, right);
        return storeIndex;
    }

    private static void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    // ==========================================================================================
    // SOLUTION 4: Counting Sort (Optimal for strictly bounded constraints)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * Notice the specific constraint: -10^4 <= nums[i] <= 10^4
     * The total range of possible numbers is exactly 20,001 values.
     * We can use an array to count the frequencies of every number.
     * Then, we iterate backwards from the maximum possible value.
     * We decrement 'k' by the frequency of each number.
     * When 'k' becomes <= 0, we have found our kth largest element.
     *
     * VISUAL:
     * nums = [3, 2, 3, 1, 2, 4, 5, 5, 6], k = 4
     * Range assumed small for example (1 to 6).
     * 
     * Frequencies (Value -> Count):
     * 6: 1
     * 5: 2
     * 4: 1
     * 3: 2
     * 2: 2
     * 1: 1
     * 
     * Walk backwards from 6:
     * At 6: k = 4 - 1 = 3
     * At 5: k = 3 - 2 = 1
     * At 4: k = 1 - 1 = 0 --> k <= 0, so 4 is the answer!
     *
     * COMPLEXITY:
     * - Time: O(N + Range), where Range = 20001. effectively O(N).
     * - Space: O(Range) = O(20001) which is exactly 20001 integers (constant space O(1)).
     */
    public static int findKthLargest_Counting(int[] nums, int k) {
        final int OFFSET = 10000;
        final int RANGE = 20001;
        var counts = new int[RANGE];

        // Populate counts
        for (var num : nums) {
            counts[num + OFFSET]++;
        }

        // Iterate from largest possible value backwards
        for (int i = RANGE - 1; i >= 0; i--) {
            k -= counts[i];
            if (k <= 0) {
                return i - OFFSET;
            }
        }
        
        throw new IllegalArgumentException("k is larger than the number of elements");
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    // Using a Java 14+ record to cleanly define test cases
    public record TestCase(int[] nums, int k, int expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(new int[]{3, 2, 1, 5, 6, 4}, 2, 5),
            new TestCase(new int[]{3, 2, 3, 1, 2, 4, 5, 5, 6}, 4, 4),
            new TestCase(new int[]{-10000, 10000, 0, 5, -5}, 1, 10000),
            new TestCase(new int[]{1}, 1, 1),
            new TestCase(new int[]{7, 7, 7, 7, 7, 7}, 3, 7) // Testing duplicates
        };

        System.out.println("Running tests for all approaches...\n");

        for (int i = 0; i < testCases.length(); i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Array: %s | k: %d | Expected: %d\n", Arrays.toString(tc.nums()), tc.k(), tc.expected());

            // Run & Validate Solution 1
            int res1 = findKthLargest_Sorting(tc.nums(), tc.k());
            System.out.printf("  [1. Sorting    ] Result: %d -> %s\n", res1, (res1 == tc.expected() ? "PASS" : "FAIL"));

            // Run & Validate Solution 2
            int res2 = findKthLargest_MinHeap(tc.nums(), tc.k());
            System.out.printf("  [2. Min-Heap   ] Result: %d -> %s\n", res2, (res2 == tc.expected() ? "PASS" : "FAIL"));

            // Run & Validate Solution 3
            int res3 = findKthLargest_QuickSelect(tc.nums(), tc.k());
            System.out.printf("  [3. QuickSelect] Result: %d -> %s\n", res3, (res3 == tc.expected() ? "PASS" : "FAIL"));

            // Run & Validate Solution 4
            int res4 = findKthLargest_Counting(tc.nums(), tc.k());
            System.out.printf("  [4. Counting   ] Result: %d -> %s\n", res4, (res4 == tc.expected() ? "PASS" : "FAIL"));
            
            System.out.println("-".repeat(50));
        }
    }
}
