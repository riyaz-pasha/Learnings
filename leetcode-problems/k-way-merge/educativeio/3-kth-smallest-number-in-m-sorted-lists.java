import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Problem: Kth Smallest Element in M Sorted Lists
 * 
 * Statement:
 * Given a list, lists, containing m sorted lists of integers in ascending order, 
 * and an integer k, find the kth smallest element among all the lists.
 * If k exceeds the total number of elements across all lists, return the largest element.
 * If the lists are empty, return 0.
 * 
 * Constraints:
 * - 1 <= m <= 50
 * - 0 <= lists[i].length <= 50
 * - -10^9 <= lists[i][j] <= 10^9
 * - 1 <= k <= 10^9
 */
class KthSmallestInMSortedLists {

    /* ============================================================================
     * Helper Method: Edge Cases Setup
     * ============================================================================
     * To prevent duplicate code across approaches, we handle the common edge 
     * cases defined in the problem constraints (empty lists, k > total elements).
     */
    private static int getTotalElements(List<List<Integer>> lists) {
        return lists.stream().mapToInt(List::size).sum();
    }

    private static int getMaximumElement(List<List<Integer>> lists) {
        int max = Integer.MIN_VALUE;
        for (var list : lists) {
            if (!list.isEmpty()) {
                max = Math.max(max, list.get(list.size() - 1));
            }
        }
        return max;
    }

    /* ============================================================================
     * APPROACH 1: Brute Force (Flatten and Sort)
     * ============================================================================
     * Explanation:
     * Extract all elements from all m lists into a single 1D list. Sort this 
     * list in ascending order. If k is greater than the total number of elements, 
     * we cap k to the list's size. Finally, return the element at index k - 1.
     * 
     * Time Complexity: O(N log N) where N is the total number of elements.
     * Space Complexity: O(N) to store the flattened list.
     */
    public static int kthSmallestBruteForce(List<List<Integer>> lists, int k) {
        var flatList = new ArrayList<Integer>();
        
        for (var list : lists) {
            flatList.addAll(list);
        }
        
        if (flatList.isEmpty()) {
            return 0;
        }
        
        Collections.sort(flatList);
        
        // If k exceeds total elements, return the largest element
        int targetIndex = Math.min(k, flatList.size()) - 1;
        return flatList.get(targetIndex);
    }

    /* ============================================================================
     * APPROACH 2: Min-Heap (K-Way Merge)
     * ============================================================================
     * Explanation:
     * We use a Min-Heap to perform a k-way merge across the m lists. 
     * We initialize the heap with the first element of each non-empty list.
     * Then, we extract the minimum element from the heap. Whenever an element 
     * is extracted, we insert the next element from the same list into the heap.
     * We repeat this k times to find the kth smallest element.
     * 
     * Time Complexity: O(K log M) where K is the target rank, M is the number of lists.
     * Space Complexity: O(M) for the Priority Queue.
     */
    
    private record HeapNode(int val, int listIdx, int elemIdx) implements Comparable<HeapNode> {
        @Override
        public int compareTo(HeapNode other) {
            return Integer.compare(this.val, other.val);
        }
    }

    public static int kthSmallestMinHeap(List<List<Integer>> lists, int k) {
        int totalElements = getTotalElements(lists);
        if (totalElements == 0) return 0;
        
        // Cap k if it exceeds total elements
        k = Math.min(k, totalElements);
        
        var minHeap = new PriorityQueue<HeapNode>();
        
        // Add the first element of each non-empty list
        for (int i = 0; i < lists.size(); i++) {
            if (!lists.get(i).isEmpty()) {
                minHeap.offer(new HeapNode(lists.get(i).get(0), i, 0));
            }
        }
        
        int count = 0;
        int result = 0;
        
        while (!minHeap.isEmpty() && count < k) {
            var node = minHeap.poll();
            result = node.val();
            count++;
            
            // Push the next element from the same list if it exists
            var currentList = lists.get(node.listIdx());
            if (node.elemIdx() + 1 < currentList.size()) {
                minHeap.offer(new HeapNode(
                    currentList.get(node.elemIdx() + 1), 
                    node.listIdx(), 
                    node.elemIdx() + 1
                ));
            }
        }
        
        return result;
    }

    /* ============================================================================
     * APPROACH 3: Optimal Binary Search on Value Range
     * ============================================================================
     * Explanation:
     * We determine the absolute minimum and maximum values across all lists. 
     * We then binary search over this value range. For a chosen 'mid' value, 
     * we count how many numbers in all lists are <= 'mid'. 
     * Since each individual list is sorted, we can use binary search (or upper bound) 
     * to efficiently count the numbers <= 'mid' in O(log L) time per list.
     * 
     * ASCII Visual for Counting Strategy:
     * List 1: [1, 4, 7]
     * List 2: [2, 5, 8]
     * List 3: [3, 6, 9]
     * 
     * Range: low = 1, high = 9. Let mid = 5.
     * Count <= 5 in List 1: [1, 4] -> 2
     * Count <= 5 in List 2: [2, 5] -> 2
     * Count <= 5 in List 3: [3]    -> 1
     * Total count = 5.
     * 
     * Time Complexity: O(M * log L * log(Max - Min)), where L is the max length of a list.
     * Space Complexity: O(1) auxiliary space.
     */
    public static int kthSmallestBinarySearch(List<List<Integer>> lists, int k) {
        int totalElements = getTotalElements(lists);
        if (totalElements == 0) return 0;
        
        if (k >= totalElements) {
            return getMaximumElement(lists);
        }

        long low = Integer.MAX_VALUE;
        long high = Integer.MIN_VALUE;

        // Find the absolute minimum and maximum values to set the search range
        for (var list : lists) {
            if (!list.isEmpty()) {
                low = Math.min(low, list.get(0));
                high = Math.max(high, list.get(list.size() - 1));
            }
        }

        // Binary search on the value space
        while (low < high) {
            long mid = low + (high - low) / 2;
            int count = 0;

            for (var list : lists) {
                count += countLessOrEqual(list, mid);
            }

            if (count < k) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }

        return (int) low;
    }

    // Helper method to find how many elements in a sorted list are <= target
    private static int countLessOrEqual(List<Integer> list, long target) {
        int left = 0;
        int right = list.size() - 1;
        int count = 0;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (list.get(mid) <= target) {
                count = mid + 1;
                left = mid + 1; // Look for higher values that might still be <= target
            } else {
                right = mid - 1;
            }
        }
        return count;
    }

    /* ============================================================================
     * TESTING / MAIN METHOD
     * ============================================================================
     */
    
    // Using Java 14+ record for structured test cases
    public record TestCase(List<List<Integer>> lists, int k, int expected) {}

    public static void main(String[] args) {
        var testCases = List.of(
            new TestCase(
                List.of(
                    List.of(1, 5, 9),
                    List.of(2, 6, 10),
                    List.of(3, 7, 11)
                ), 
                5, 5
            ),
            new TestCase(
                List.of(
                    List.of(1, 4, 7),
                    List.of(2, 5, 8),
                    List.of(3, 6, 9)
                ), 
                15, 9 // k (15) > total elements (9), should return max (9)
            ),
            new TestCase(
                List.of(
                    List.of(),
                    List.of()
                ), 
                3, 0 // All lists empty, should return 0
            ),
            new TestCase(
                List.of(
                    List.of(-5, 0, 5),
                    List.of(-10, -3, 8)
                ), 
                2, -5
            )
        );

        System.out.println("Running tests for all 3 approaches...\n");

        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            System.out.println("Test Case " + (i + 1) + ": (k = " + tc.k + ")");
            System.out.println("Input Lists: " + tc.lists);
            
            int ans1 = kthSmallestBruteForce(tc.lists, tc.k);
            int ans2 = kthSmallestMinHeap(tc.lists, tc.k);
            int ans3 = kthSmallestBinarySearch(tc.lists, tc.k);

            boolean pass1 = (ans1 == tc.expected);
            boolean pass2 = (ans2 == tc.expected);
            boolean pass3 = (ans3 == tc.expected);

            System.out.println("  Brute Force   : " + (pass1 ? "PASS" : "FAIL") + " -> Result: " + ans1);
            System.out.println("  Min-Heap      : " + (pass2 ? "PASS" : "FAIL") + " -> Result: " + ans2);
            System.out.println("  Binary Search : " + (pass3 ? "PASS" : "FAIL") + " -> Result: " + ans3);
            System.out.println("-".repeat(60));
        }
    }
}

import java.util.*;

class Solution {

    /**
     * Record to represent an element inside the heap.
     *
     * value         -> actual number
     * listIndex     -> which list this number belongs to
     * elementIndex  -> index inside that list
     *
     * Why do we need this?
     * Because when we pop an element, we need to know
     * where it came from so we can push the next element
     * from the SAME list.
     */
    record Node(int value, int listIndex, int elementIndex) {}

    public int kthSmallest(List<List<Integer>> lists, int k) {

        /**
         * Min Heap:
         * Always gives us the smallest element among current candidates.
         *
         * Heap size will be at most = number of lists (m)
         */
        PriorityQueue<Node> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(Node::value));

        int totalElements = 0;

        /**
         * STEP 1: Initialize heap
         *
         * Add the FIRST element from each list.
         *
         * Why only first?
         * Because each list is sorted, so:
         * → first element is the smallest in that list
         * → next elements will be added later lazily
         */
        for (int i = 0; i < lists.size(); i++) {
            List<Integer> list = lists.get(i);

            totalElements += list.size();

            if (!list.isEmpty()) {
                minHeap.offer(new Node(list.get(0), i, 0));
            }
        }

        /**
         * EDGE CASE 1: All lists are empty
         */
        if (totalElements == 0) return 0;

        /**
         * EDGE CASE 2: k > total elements
         * Return the largest element across all lists
         */
        if (k > totalElements) {
            int max = Integer.MIN_VALUE;

            for (List<Integer> list : lists) {
                if (!list.isEmpty()) {
                    max = Math.max(max, list.get(list.size() - 1));
                }
            }

            return max;
        }

        /**
         * STEP 2: Extract min k times
         */
        int count = 0;
        int answer = 0;

        while (!minHeap.isEmpty()) {

            /**
             * Get smallest element among all lists
             */
            Node current = minHeap.poll();
            answer = current.value();
            count++;

            /**
             * If we've reached kth element → done
             */
            if (count == k) return answer;

            /**
             * STEP 3: Push next element from same list
             *
             * This is the KEY idea:
             * We only expand the list from which we took an element.
             */
            int nextIndex = current.elementIndex() + 1;

            List<Integer> currentList = lists.get(current.listIndex());

            if (nextIndex < currentList.size()) {
                minHeap.offer(
                        new Node(
                                currentList.get(nextIndex),
                                current.listIndex(),
                                nextIndex
                        )
                );
            }
        }

        return answer; // fallback (shouldn't normally hit)
    }
}
