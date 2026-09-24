import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.List;

/**
 * Problem: Merge k Sorted Lists
 * 
 * Statement:
 * You are given an array, lists, containing k singly linked lists. 
 * Each of these linked lists is individually sorted in ascending order.
 * Merge all k linked lists into a single sorted linked list and return it.
 * 
 * Constraints:
 * - k == lists.length
 * - 0 <= k <= 10^3
 * - 0 <= lists[i].length <= 500
 * - -10^3 <= lists[i][j] <= 10^3
 * - Each lists[i] is sorted in ascending order.
 * - The sum of all lists[i].length will not exceed 10^4.
 */
class MergeKSortedLists {

    /* ============================================================================
     * SINGLY LINKED LIST DEFINITION
     * ============================================================================
     */
    public static class ListNode {
        int val;
        ListNode next;
        
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    /* ============================================================================
     * APPROACH 1: Brute Force (Extract, Sort, and Rebuild)
     * ============================================================================
     * Explanation:
     * We traverse all k linked lists, extract every single value into a dynamic 
     * array (ArrayList), sort the array, and then create a brand new linked 
     * list from the sorted values.
     * 
     * Time Complexity: O(N log N) where N is the total number of nodes.
     * Space Complexity: O(N) to store the values and create the new list.
     */
    public static ListNode mergeKListsBruteForce(ListNode[] lists) {
        if (lists == null || lists.length == 0) return null;
        
        var values = new ArrayList<Integer>();
        
        // Extract all values
        for (var node : lists) {
            while (node != null) {
                values.add(node.val);
                node = node.next;
            }
        }
        
        // Sort the values
        Collections.sort(values);
        
        // Build the new linked list
        var dummy = new ListNode(-1);
        var current = dummy;
        for (int val : values) {
            current.next = new ListNode(val);
            current = current.next;
        }
        
        return dummy.next;
    }

    /* ============================================================================
     * APPROACH 2: Min-Heap (Priority Queue)
     * ============================================================================
     * Explanation:
     * We use a Min-Heap (Priority Queue) to keep track of the smallest current 
     * element across all k lists. 
     * 1. Insert the head node of each list into the Min-Heap.
     * 2. Extract the smallest node from the heap and append it to our result list.
     * 3. If the extracted node has a 'next' node, insert that 'next' node into the heap.
     * 4. Repeat until the heap is empty.
     * 
     * ASCII Visual:
     * lists = [[1->4->5], [1->3->4], [2->6]]
     * 
     * Heap initialization (adds heads): [1 (from L1), 1 (from L2), 2 (from L3)]
     * Step 1: Poll 1 (L1), Heap -> [1(L2), 2(L3)]. Add 1's next: 4 (L1). Heap -> [1(L2), 2(L3), 4(L1)]
     * Step 2: Poll 1 (L2), Heap -> [2(L3), 4(L1)]. Add 1's next: 3 (L2). Heap -> [2(L3), 3(L2), 4(L1)]
     * ... and so on.
     * 
     * Time Complexity: O(N log K) where N is total nodes and K is the number of lists.
     * Space Complexity: O(K) for the priority queue.
     */
    public static ListNode mergeKListsMinHeap(ListNode[] lists) {
        if (lists == null || lists.length == 0) return null;
        
        // Priority Queue comparing node values
        var minHeap = new PriorityQueue<ListNode>((a, b) -> Integer.compare(a.val, b.val));
        
        // Add the head of each non-empty list
        for (var head : lists) {
            if (head != null) {
                minHeap.offer(head);
            }
        }
        
        var dummy = new ListNode(-1);
        var current = dummy;
        
        while (!minHeap.isEmpty()) {
            var smallestNode = minHeap.poll();
            current.next = smallestNode; // Append to result
            current = current.next;      // Move pointer forward
            
            // If there's a next node in the extracted node's list, add it to the heap
            if (smallestNode.next != null) {
                minHeap.offer(smallestNode.next);
            }
        }
        
        return dummy.next;
    }

    /* ============================================================================
     * APPROACH 3: Divide and Conquer (Merge Pairs)
     * ============================================================================
     * Explanation:
     * We can merge the lists in pairs. We merge list 0 and list 1, list 2 and list 3, 
     * and so on. We repeat this process until only one list remains. 
     * This avoids the overhead of a priority queue and modifies the lists entirely in-place.
     * 
     * ASCII Visual:
     * Iteration 0:
     * L0 & L1 -> merged(0,1)
     * L2 & L3 -> merged(2,3)
     * L4      -> untouched
     * 
     * Iteration 1:
     * merged(0,1) & merged(2,3) -> merged(0..3)
     * L4                        -> untouched
     * 
     * Time Complexity: O(N log K). We merge lists log K times.
     * Space Complexity: O(1) strictly auxiliary space.
     */
    public static ListNode mergeKListsDivideAndConquer(ListNode[] lists) {
        if (lists == null || lists.length == 0) return null;
        
        int interval = 1;
        int k = lists.length;
        
        while (interval < k) {
            for (int i = 0; i < k - interval; i += interval * 2) {
                lists[i] = mergeTwoLists(lists[i], lists[i + interval]);
            }
            interval *= 2;
        }
        
        return lists[0];
    }
    
    // Helper function to merge two sorted lists (Standard algorithm)
    private static ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        var dummy = new ListNode(-1);
        var curr = dummy;
        
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                curr.next = l1;
                l1 = l1.next;
            } else {
                curr.next = l2;
                l2 = l2.next;
            }
            curr = curr.next;
        }
        
        // Attach remainder
        if (l1 != null) curr.next = l1;
        if (l2 != null) curr.next = l2;
        
        return dummy.next;
    }

    /* ============================================================================
     * TESTING / UTILITIES
     * ============================================================================
     */
    
    // Utility to build a linked list from an array
    private static ListNode buildList(int[] arr) {
        if (arr.length == 0) return null;
        var dummy = new ListNode(-1);
        var curr = dummy;
        for (int val : arr) {
            curr.next = new ListNode(val);
            curr = curr.next;
        }
        return dummy.next;
    }
    
    // Utility to extract list to array for easy verification
    private static List<Integer> toArrayList(ListNode head) {
        var list = new ArrayList<Integer>();
        while (head != null) {
            list.add(head.val);
            head = head.next;
        }
        return list;
    }
    
    // Deep clone array of linked lists for testing multiple algorithms without interference
    private static ListNode[] cloneListsArray(int[][] arrays) {
        var cloned = new ListNode[arrays.length];
        for (int i = 0; i < arrays.length; i++) {
            cloned[i] = buildList(arrays[i]);
        }
        return cloned;
    }

    // Using Java 14+ record for structured test cases
    public record TestCase(int[][] inputArrays, List<Integer> expected) {}

    public static void main(String[] args) {
        var testCases = List.of(
            new TestCase(
                new int[][]{
                    {1, 4, 5},
                    {1, 3, 4},
                    {2, 6}
                }, 
                List.of(1, 1, 2, 3, 4, 4, 5, 6)
            ),
            new TestCase(
                new int[][]{}, 
                List.of()
            ),
            new TestCase(
                new int[][]{
                    {}
                }, 
                List.of()
            ),
            new TestCase(
                new int[][]{
                    {-2, -1, 3, 4},
                    {},
                    {-3, 1, 4}
                }, 
                List.of(-3, -2, -1, 1, 3, 4, 4)
            )
        );

        System.out.println("Running tests for all 3 approaches...\n");

        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            System.out.println("Test Case " + (i + 1) + ":");
            System.out.println("Input arrays: " + Arrays.deepToString(tc.inputArrays));
            
            // Create fresh copies of the linked lists for each approach
            var listsForApproach1 = cloneListsArray(tc.inputArrays);
            var listsForApproach2 = cloneListsArray(tc.inputArrays);
            var listsForApproach3 = cloneListsArray(tc.inputArrays);
            
            // Execute
            var ans1 = toArrayList(mergeKListsBruteForce(listsForApproach1));
            var ans2 = toArrayList(mergeKListsMinHeap(listsForApproach2));
            var ans3 = toArrayList(mergeKListsDivideAndConquer(listsForApproach3));

            // Verify
            boolean pass1 = ans1.equals(tc.expected);
            boolean pass2 = ans2.equals(tc.expected);
            boolean pass3 = ans3.equals(tc.expected);

            System.out.println("  Brute Force      : " + (pass1 ? "PASS" : "FAIL") + " -> Result: " + ans1);
            System.out.println("  Min-Heap         : " + (pass2 ? "PASS" : "FAIL") + " -> Result: " + ans2);
            System.out.println("  Divide & Conquer : " + (pass3 ? "PASS" : "FAIL") + " -> Result: " + ans3);
            System.out.println("-".repeat(60));
        }
    }
}

class Solution {

    /**
     * Definition for singly-linked list.
     */
    static class ListNode {
        int val;
        ListNode next;

        ListNode(int val) {
            this.val = val;
        }
    }

    /**
     * Wrapper record for heap elements.
     * 
     * Why use a record?
     * - Cleaner syntax
     * - Immutable by default
     * - Helps if we want to extend metadata later (like list index)
     */
    record NodeEntry(ListNode node) {}

    public ListNode mergeKLists(ListNode[] lists) {

        // Edge case: empty input
        if (lists == null || lists.length == 0) {
            return null;
        }

        /**
         * Min Heap (Priority Queue)
         * 
         * Comparator:
         * - Always extracts the smallest node value
         * - Use Comparator.comparingInt to avoid overflow
         */
        PriorityQueue<NodeEntry> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(e -> e.node.val));

        /**
         * Step 1: Initialize heap
         * 
         * Add the first node (head) of each list
         * Why?
         * - These are the smallest candidates from each list
         */
        for (ListNode head : lists) {
            if (head != null) {
                minHeap.offer(new NodeEntry(head));
            }
        }

        /**
         * Dummy node simplifies edge cases
         * (like empty result or first insertion)
         */
        ListNode dummy = new ListNode(-1);
        ListNode tail = dummy;

        /**
         * Step 2: Process heap
         * 
         * Always extract the smallest element among k lists
         */
        while (!minHeap.isEmpty()) {

            // Get the smallest node
            NodeEntry entry = minHeap.poll();
            ListNode smallestNode = entry.node;

            /**
             * Attach to result list
             * 
             * IMPORTANT:
             * We reuse existing nodes → no extra memory
             */
            tail.next = smallestNode;
            tail = tail.next;

            /**
             * Step 3: Add next element of the same list
             * 
             * Why?
             * - After removing current smallest,
             *   next element from that list becomes candidate
             */
            if (smallestNode.next != null) {
                minHeap.offer(new NodeEntry(smallestNode.next));
            }
        }

        // Final merged sorted list
        return dummy.next;
    }
}

class Solution2 {

    /**
     * Definition for singly-linked list.
     */
    static class ListNode {
        int val;
        ListNode next;

        ListNode(int val) {
            this.val = val;
        }
    }

    /**
     * Merges k sorted linked lists into one sorted list.
     *
     * Core Idea:
     * - Always pick the smallest node among the current heads of all lists.
     * - Use a Min Heap (PriorityQueue) to efficiently get the smallest element.
     *
     * @param lists Array of k sorted linked lists
     * @return Head of merged sorted linked list
     */
    public ListNode mergeKLists(ListNode[] lists) {

        // Edge case: no lists provided
        if (lists == null || lists.length == 0) {
            return null;
        }

        /**
         * Min Heap (Priority Queue)
         *
         * Stores ListNode directly.
         * Comparator ensures smallest value node is always at the top.
         *
         * WHY comparator.comparingInt?
         * - Prevents integer overflow (better than a.val - b.val)
         */
        PriorityQueue<ListNode> minHeap =
                new PriorityQueue<>(Comparator.comparingInt(node -> node.val));

        /**
         * Step 1: Initialize heap with the head of each list
         *
         * WHY only heads?
         * - Each list is already sorted.
         * - The head is the smallest element of that list.
         * - So, we only need k candidates at a time.
         */
        for (ListNode head : lists) {
            if (head != null) {
                minHeap.offer(head);
            }
        }

        /**
         * Dummy node to simplify result list construction
         *
         * WHY dummy?
         * - Avoid handling special case for first node
         * - Makes code cleaner
         */
        ListNode dummy = new ListNode(-1);
        ListNode tail = dummy;

        /**
         * Step 2: Process the heap
         *
         * At each step:
         * - Extract the smallest node
         * - Attach it to result
         * - Push its next node into heap (if exists)
         */
        while (!minHeap.isEmpty()) {

            // Get the smallest node among current k candidates
            ListNode smallest = minHeap.poll();

            /**
             * Attach this node to the merged list
             *
             * IMPORTANT:
             * - We reuse the existing node (no new node created)
             * - This keeps space optimal
             */
            tail.next = smallest;
            tail = tail.next;

            /**
             * If there is a next node in the same list,
             * add it to the heap
             *
             * WHY?
             * - After removing current node,
             *   the next node becomes the new candidate from that list
             */
            if (smallest.next != null) {
                minHeap.offer(smallest.next);
            }
        }

        /**
         * Return the merged list (skip dummy node)
         */
        return dummy.next;
    }
}
