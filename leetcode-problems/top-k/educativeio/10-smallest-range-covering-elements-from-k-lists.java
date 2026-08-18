import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * PROBLEM STATEMENT:
 * You are given k sorted lists of integers, nums, where each list is in non-decreasing order. 
 * Your task is to find the smallest range [a, b] that contains at least one element from each of the k lists.
 * 
 * A range [a, b] is considered smaller than another range [c, d] if:
 * 1. b - a < d - c  (Smaller length)
 * 2. or a < c if b - a == d - c  (Same length, but starts with a smaller value)
 *
 * CONSTRAINTS:
 * nums.length == k
 * 1 <= k <= 100
 * 1 <= nums[i].length <= 50
 * -1000 <= nums[i][j] <= 1000
 * nums[i] is sorted in non-decreasing order.
 * 
 * ==========================================================================================
 * LATEST JAVA FEATURES USED:
 * - Records (`record Element(...)`, `record Point(...)`): Clean immutable data carriers.
 * - Local Variable Type Inference (`var`): Cleaner syntax.
 * ==========================================================================================
 */
class SmallestRangeInKLists {

    // ==========================================================================================
    // SOLUTION 1: Min-Heap / PriorityQueue (The Optimal Approach)
    // ==========================================================================================
    /*
     * EXPLANATION:
     * This problem is similar to merging k sorted arrays, but instead of fully merging, 
     * we are maintaining a sliding window across the multiple arrays simultaneously.
     * 
     * 1. We keep exactly ONE element from each of the 'k' lists in our consideration pool.
     * 2. To easily find the minimum in our pool, we use a Min-Heap.
     * 3. We also keep a running variable `currentMax` to track the largest element currently in our pool.
     * 4. The current valid range is always `[MinHeap.peek(), currentMax]`.
     * 5. To potentially find a *smaller* range, we must increase the lower bound. 
     *    We do this by polling the smallest element from the heap and pushing the *next* element 
     *    from that same list into the heap, updating `currentMax` as needed.
     * 6. If any list runs out of elements, we can no longer form a valid range containing 
     *    at least one element from ALL lists, so we stop.
     *
     * VISUAL:
     * nums = [
     *   [4, 10, 15, 24],
     *   [0, 9, 12, 20],
     *   [5, 18, 22, 30]
     * ]
     * 
     * Initial Heap: [0(L1), 4(L0), 5(L2)]. currentMax = 5.
     * Current Range: [0, 5]. Size = 5.
     * 
     * Step 1: Pop minimum 0(L1). Add next from L1: 9. 
     *         Heap: [4(L0), 5(L2), 9(L1)]. currentMax = 9.
     *         New Range: [4, 9]. Size = 5. Tie-breaker: 0 < 4, so [0, 5] stays best.
     * 
     * Step 2: Pop minimum 4(L0). Add next from L0: 10.
     *         Heap: [5(L2), 9(L1), 10(L0)]. currentMax = 10.
     *         New Range: [5, 10]. Size = 5.
     * ... repeats until a list is exhausted.
     *
     * COMPLEXITY:
     * - Time: O(N log k) - where N is the total number of elements across all lists. 
     *         Each element is added/removed from a heap of size k at most once.
     * - Space: O(k) - the PriorityQueue holds exactly k elements.
     */
    
    // Record to hold the context of an element inside the heap
    private record Element(int value, int listIndex, int elementIndex) implements Comparable<Element> {
        @Override
        public int compareTo(Element other) {
            return Integer.compare(this.value, other.value);
        }
    }

    public static int[] smallestRange_MinHeap(List<List<Integer>> nums) {
        int k = nums.size();
        PriorityQueue<Element> minHeap = new PriorityQueue<>(k);
        int currentMax = Integer.MIN_VALUE;

        // Initialize the heap with the first element of each list
        for (int i = 0; i < k; i++) {
            int firstVal = nums.get(i).get(0);
            minHeap.offer(new Element(firstVal, i, 0));
            currentMax = Math.max(currentMax, firstVal);
        }

        // Keep track of the best range found
        int rangeStart = 0;
        int rangeEnd = Integer.MAX_VALUE;

        // Process as long as the heap contains exactly one element from each list
        while (minHeap.size() == k) {
            Element minEl = minHeap.poll();
            int currentMin = minEl.value;

            // Check if we found a smaller range
            if (currentMax - currentMin < rangeEnd - rangeStart) {
                rangeStart = currentMin;
                rangeEnd = currentMax;
            } else if (currentMax - currentMin == rangeEnd - rangeStart && currentMin < rangeStart) {
                rangeStart = currentMin;
                rangeEnd = currentMax;
            }

            // Move to the next element in the list that contained the minimum value
            int nextElementIndex = minEl.elementIndex + 1;
            if (nextElementIndex < nums.get(minEl.listIndex).size()) {
                int nextVal = nums.get(minEl.listIndex).get(nextElementIndex);
                minHeap.offer(new Element(nextVal, minEl.listIndex, nextElementIndex));
                currentMax = Math.max(currentMax, nextVal);
            } else {
                // If any list is exhausted, we can't form a valid range anymore
                break;
            }
        }

        return new int[]{rangeStart, rangeEnd};
    }

    // ==========================================================================================
    // SOLUTION 2: Merged Array + Sliding Window
    // ==========================================================================================
    /*
     * EXPLANATION:
     * 1. Merge all elements from all k lists into a single combined list of (value, list_id) pairs.
     * 2. Sort this combined list purely based on the values.
     * 3. Use a Sliding Window (Two Pointers) over this combined list.
     * 4. Expand the window to the right until it contains at least one element from every list 
     *    (using an array or map to count the frequencies of list_ids in the window).
     * 5. Once the window is valid, record the range. Then shrink the window from the left to 
     *    find smaller valid ranges until the window becomes invalid.
     * 6. Repeat until the right pointer reaches the end.
     *
     * COMPLEXITY:
     * - Time: O(N log N) - N elements total. Sorting the merged array dominates the time complexity.
     * - Space: O(N) - Storing the merged array and frequency counts.
     */
    
    private record Point(int value, int listId) {}

    public static int[] smallestRange_SlidingWindow(List<List<Integer>> nums) {
        int k = nums.size();
        var mergedList = new ArrayList<Point>();

        // Flatten all lists into a single list of Points
        for (int i = 0; i < k; i++) {
            for (int val : nums.get(i)) {
                mergedList.add(new Point(val, i));
            }
        }

        // Sort the merged list by value
        mergedList.sort((a, b) -> Integer.compare(a.value(), b.value()));

        int[] listCounts = new int[k];
        int uniqueListsInWindow = 0;
        
        int left = 0;
        int bestStart = -100000;
        int bestEnd = 100000;

        // Sliding window
        for (int right = 0; right < mergedList.size(); right++) {
            Point rightPoint = mergedList.get(right);
            
            // Add right element to window
            if (listCounts[rightPoint.listId] == 0) {
                uniqueListsInWindow++;
            }
            listCounts[rightPoint.listId]++;

            // When window is valid (contains at least one element from every list)
            while (uniqueListsInWindow == k) {
                Point leftPoint = mergedList.get(left);
                
                int currentStart = leftPoint.value;
                int currentEnd = rightPoint.value;

                // Update best range
                long currentDiff = (long) currentEnd - currentStart;
                long bestDiff = (long) bestEnd - bestStart;

                if (currentDiff < bestDiff || (currentDiff == bestDiff && currentStart < bestStart)) {
                    bestStart = currentStart;
                    bestEnd = currentEnd;
                }

                // Shrink window from the left
                listCounts[leftPoint.listId]--;
                if (listCounts[leftPoint.listId] == 0) {
                    uniqueListsInWindow--;
                }
                left++;
            }
        }

        return new int[]{bestStart, bestEnd};
    }

    // ==========================================================================================
    // TESTING SUITE
    // ==========================================================================================
    
    public record TestCase(List<List<Integer>> nums, int[] expected) {}

    public static void main(String[] args) {
        var testCases = new TestCase[] {
            new TestCase(
                List.of(
                    List.of(4, 10, 15, 24, 26),
                    List.of(0, 9, 12, 20),
                    List.of(5, 18, 22, 30)
                ), 
                new int[]{20, 24}
            ),
            new TestCase(
                List.of(
                    List.of(1, 2, 3),
                    List.of(1, 2, 3),
                    List.of(1, 2, 3)
                ), 
                new int[]{1, 1}
            ),
            new TestCase(
                List.of(
                    List.of(10, 10),
                    List.of(11, 11)
                ), 
                new int[]{10, 11}
            ),
            new TestCase(
                List.of(
                    List.of(1),
                    List.of(2),
                    List.of(3),
                    List.of(4),
                    List.of(5),
                    List.of(6),
                    List.of(7)
                ), 
                new int[]{1, 7}
            )
        };

        System.out.println("Running Smallest Range in K Lists Tests...\n");

        for (int i = 0; i < testCases.length; i++) {
            var tc = testCases[i];
            System.out.printf("Test Case %d:\n", i + 1);
            System.out.printf("Expected Range: %s\n", Arrays.toString(tc.expected()));

            // Run Min-Heap Solution
            int[] res1 = smallestRange_MinHeap(tc.nums());
            boolean pass1 = Arrays.equals(res1, tc.expected());
            System.out.printf("  [1. Min-Heap      ] Result: %s -> %s\n", Arrays.toString(res1), pass1 ? "PASS" : "FAIL");

            // Run Sliding Window Solution
            int[] res2 = smallestRange_SlidingWindow(tc.nums());
            boolean pass2 = Arrays.equals(res2, tc.expected());
            System.out.printf("  [2. Sliding Window] Result: %s -> %s\n", Arrays.toString(res2), pass2 ? "PASS" : "FAIL");

            System.out.println("-".repeat(60));
        }
    }
}

class Solution {

    /**
     * Record instead of class (cleaner, immutable, concise)
     * Represents one element from a list.
     *
     * value        -> actual number
     * listIndex    -> which list this belongs to
     * elementIndex -> position inside that list
     */
    record Node(int value, int listIndex, int elementIndex) {}

    public int[] smallestRange(List<List<Integer>> nums) {

        // 🔹 Min Heap → always gives the smallest value among current elements
        PriorityQueue<Node> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(Node::value));

        int currentMax = Integer.MIN_VALUE;
        int k = nums.size();

        // ============================================================
        // STEP 1: Initialize heap with FIRST element from each list
        // WHY?
        // We must include at least ONE element from each list
        // This forms our initial "window"
        // ============================================================
        for (int i = 0; i < k; i++) {
            int val = nums.get(i).get(0);

            minHeap.offer(new Node(val, i, 0));

            // Track the maximum element in current window
            currentMax = Math.max(currentMax, val);
        }

        // Best answer so far
        int bestStart = 0;
        int bestEnd = Integer.MAX_VALUE;

        // ============================================================
        // STEP 2: Process until any list is exhausted
        // ============================================================
        while (true) {

            // 🔹 Get the smallest element (this defines current range start)
            Node minNode = minHeap.poll();
            int currentMin = minNode.value();

            // ========================================================
            // 🔹 Current range = [currentMin, currentMax]
            // Try to update best range
            // ========================================================
            if (currentMax - currentMin < bestEnd - bestStart ||
                (currentMax - currentMin == bestEnd - bestStart && currentMin < bestStart)) {

                bestStart = currentMin;
                bestEnd = currentMax;
            }

            // ========================================================
            // 🔹 Move forward in the SAME list from which min came
            // WHY?
            // To try increasing the minimum and shrink the range
            // ========================================================
            int nextIndex = minNode.elementIndex() + 1;

            // ❗ If this list is exhausted → we can't include all lists anymore
            if (nextIndex == nums.get(minNode.listIndex()).size()) {
                break;
            }

            int nextValue = nums.get(minNode.listIndex()).get(nextIndex);

            // Add next element into heap
            minHeap.offer(new Node(nextValue, minNode.listIndex(), nextIndex));

            // Update max if needed
            currentMax = Math.max(currentMax, nextValue);
        }

        return new int[]{bestStart, bestEnd};
    }
}
