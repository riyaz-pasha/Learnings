import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * PROBLEM STATEMENT:
 * You need to dynamically track the kth highest test score among applicants in real time.
 * Implement the KthLargest class:
 * - Constructor: Initializes the object with the integer k and the initial stream of test scores nums.
 * - int add(int val): Adds a new test score val to the stream and returns the kth highest score.
 *
 * CONSTRAINTS:
 * 1 <= k <= 10^3
 * 0 <= nums.length <= 10^3
 * -10^3 <= nums[i] <= 10^3
 * -10^3 <= val <= 10^3
 * At most, 10^3 calls will be made to add.
 *
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record TestCase(...)`): For defining clean immutable test scenarios.
 * - Local Variable Type Inference (`var`): To reduce boilerplate while keeping type safety.
 * ==========================================================================================
 */
public class KthLargestStreamSolutions {

    // ==========================================================================================
    // SOLUTION 1: Min-Heap (The Standard & Optimal Approach for Streams)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * A Min-Heap is the perfect data structure for keeping track of the "top K" elements
     * in a stream. We maintain a PriorityQueue of maximum size k. 
     * The smallest element of these "top K" elements will always sit at the root of the heap.
     * When a new element comes in, we add it to the heap. If the heap size exceeds k, 
     * we remove the root (the smallest element). The new root is the kth largest element.
     *
     * VISUAL:
     * k = 3, nums = [4, 5, 8, 2]
     * 
     * Init: 
     * Add 4 -> Heap: [4]
     * Add 5 -> Heap: [4, 5]
     * Add 8 -> Heap: [4, 5, 8]
     * Add 2 -> Heap: [2, 4, 5, 8] -> Size > 3, poll (2) -> Heap: [4, 5, 8]
     * 
     * add(3): 
     * Add 3 -> Heap: [3, 4, 5, 8] -> Size > 3, poll (3) -> Heap: [4, 5, 8] -> Return 4
     * 
     * add(9):
     * Add 9 -> Heap: [4, 5, 8, 9] -> Size > 3, poll (4) -> Heap: [5, 8, 9] -> Return 5
     *
     * COMPLEXITY:
     * - Time: 
     *   - Constructor: O(N log k) where N is the length of nums.
     *   - add(): O(log k) to insert and possibly remove from the heap.
     * - Space: O(k) to store the elements in the PriorityQueue.
     */
    public static class KthLargest_MinHeap {
        private final PriorityQueue<Integer> minHeap;
        private final int k;

        public KthLargest_MinHeap(int k, int[] nums) {
            this.k = k;
            this.minHeap = new PriorityQueue<>(k + 1);
            for (var num : nums) {
                add(num);
            }
        }

        public int add(int val) {
            minHeap.offer(val);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
            // The root of the min-heap is the kth largest element
            return minHeap.peek();
        }
    }

    // ==========================================================================================
    // SOLUTION 2: Counting / Frequency Array (Optimal for tightly constrained values)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We exploit the constraints! The problem states that -1000 <= nums[i], val <= 1000.
     * This means the test scores can only take on 2001 distinct values.
     * Instead of a tree or heap, we can use a fixed-size array to count frequencies.
     * To find the kth largest element, we iterate backwards from the maximum possible score (1000)
     * and subtract the frequency of each score from k until k <= 0.
     *
     * VISUAL:
     * k = 2, nums = [999, 1000, 999]
     * 
     * Frequency Array (mapped by adding 1000 to handle negatives):
     * index 2000 (value 1000) -> count: 1
     * index 1999 (value 999)  -> count: 2
     * 
     * add(998):
     * index 1998 (value 998) -> count: 1
     * Walk backwards from 2000:
     * Is it 1000? k = 2 - 1 = 1. (Not <= 0, continue)
     * Is it 999?  k = 1 - 2 = -1. (<= 0, YES! Return 999)
     *
     * COMPLEXITY:
     * - Time: 
     *   - Constructor: O(N) to populate initial counts.
     *   - add(): O(Range) -> O(2001) -> effectively O(1) constant time since Range is fixed.
     * - Space: O(Range) -> O(2001) -> effectively O(1) constant auxiliary space.
     */
    public static class KthLargest_Counting {
        private final int[] count;
        private final int k;
        private final int OFFSET = 1000;

        public KthLargest_Counting(int k, int[] nums) {
            this.k = k;
            this.count = new int[2001];
            for (var num : nums) {
                count[num + OFFSET]++;
            }
        }

        public int add(int val) {
            count[val + OFFSET]++;
            int currentK = k;
            
            // Scan backwards from the largest possible value
            for (int i = 2000; i >= 0; i--) {
                currentK -= count[i];
                if (currentK <= 0) {
                    return i - OFFSET;
                }
            }
            return -1; // Should not be reached if inputs are valid
        }
    }

    // ==========================================================================================
    // SOLUTION 3: Sorted List with Binary Search (Maintains exact k elements)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * We keep an ArrayList strictly limited to size k, sorted in ascending order.
     * When a new value arrives, if the list has fewer than k elements, we binary search 
     * its correct insertion point and add it.
     * If the list is full (size k), we compare the new value against the smallest element (at index 0).
     * If it's larger, we remove the smallest element and insert the new one in its sorted position.
     *
     * COMPLEXITY:
     * - Time: 
     *   - Constructor: O(N * k) in the worst case due to insertions causing array shifts.
     *   - add(): O(log k) to find position + O(k) to shift elements in ArrayList. Overall O(k).
     * - Space: O(k) for the ArrayList.
     */
    public static class KthLargest_SortedList {
        private final List<Integer> list;
        private final int k;

        public KthLargest_SortedList(int k, int[] nums) {
            this.k = k;
            this.list = new ArrayList<>(k + 1);
            for (var num : nums) {
                add(num);
            }
        }

        public int add(int val) {
            if (list.size() < k) {
                insertSorted(val);
            } else if (val > list.get(0)) {
                list.remove(0); // Remove the smallest to make room
                insertSorted(val);
            }
            // Smallest of the top k elements is at index 0
            return list.get(0);
        }

        private void insertSorted(int val) {
            int pos = Collections.binarySearch(list, val);
            if (pos < 0) {
                pos = -(pos + 1); // Get insertion point
            }
            list.add(pos, val);
        }
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    public record TestCase(int k, int[] nums, int[] streamToAdd, int[] expectedResults) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            // Test Case 1: Standard stream
            new TestCase(
                3, 
                new int[]{4, 5, 8, 2}, 
                new int[]{3, 5, 10, 9, 4}, 
                new int[]{4, 5, 5, 8, 8}
            ),
            // Test Case 2: Negative numbers
            new TestCase(
                2, 
                new int[]{-10, -5, 0, 5}, 
                new int[]{-20, -2, 10}, 
                new int[]{0, 0, 5}
            ),
            // Test Case 3: Empty initial array
            new TestCase(
                1, 
                new int[]{}, 
                new int[]{1, 2, -1, 4}, 
                new int[]{1, 2, 2, 4}
            )
        };

        System.out.println("Running stream tests for all approaches...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Initial k: %d | Initial nums: %s\n", tc.k(), Arrays.toString(tc.nums()));

            // Instantiate all approaches
            var minHeapApp = new KthLargest_MinHeap(tc.k(), tc.nums());
            var countingApp = new KthLargest_Counting(tc.k(), tc.nums());
            var sortedListApp = new KthLargest_SortedList(tc.k(), tc.nums());

            boolean allPass = true;
            for (int j = 0; j < tc.streamToAdd().length; j++) {
                int val = tc.streamToAdd()[j];
                int expected = tc.expectedResults()[j];

                int res1 = minHeapApp.add(val);
                int res2 = countingApp.add(val);
                int res3 = sortedListApp.add(val);

                if (res1 != expected || res2 != expected || res3 != expected) {
                    allPass = false;
                    System.out.printf("  [ERROR] on add(%d). Expected: %d | MinHeap: %d | Counting: %d | SortedList: %d\n",
                                      val, expected, res1, res2, res3);
                }
            }
            System.out.printf("  Result: %s\n", allPass ? "PASS" : "FAIL");
            System.out.println("-".repeat(70));
        }
    }
}
