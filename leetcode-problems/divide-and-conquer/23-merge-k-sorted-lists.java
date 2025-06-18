import java.util.*;
/*
 * You are given an array of k linked-lists lists, each linked-list is sorted in
 * ascending order.
 * 
 * Merge all the linked-lists into one sorted linked-list and return it.
 * 
 * Example 1:
 * Input: lists = [[1,4,5],[1,3,4],[2,6]]
 * Output: [1,1,2,3,4,4,5,6]
 * Explanation: The linked-lists are:
 * [
 * 1->4->5,
 * 1->3->4,
 * 2->6
 * ]
 * merging them into one sorted list:
 * 1->1->2->3->4->4->5->6
 * 
 * Example 2:
 * Input: lists = []
 * Output: []
 * 
 * Example 3:
 * Input: lists = [[]]
 * Output: []
 */

class ListNode {

    int val;
    ListNode next;

    ListNode() {
    }

    ListNode(int val) {
        this.val = val;
    }

    ListNode(int val, ListNode next) {
        this.val = val;
        this.next = next;
    }

}

class Solution {

    // APPROACH 1: Divide and Conquer (Most Efficient)
    // Time: O(N log k), Space: O(log k) for recursion stack
    // where N is total number of nodes, k is number of lists
    public ListNode mergeKLists(ListNode[] lists) {
        if (lists == null || lists.length == 0)
            return null;
        return divideAndConquer(lists, 0, lists.length - 1);
    }

    private ListNode divideAndConquer(ListNode[] lists, int start, int end) {
        if (start == end)
            return lists[start];
        if (start > end)
            return null;

        int mid = start + (end - start) / 2;
        ListNode left = divideAndConquer(lists, start, mid);
        ListNode right = divideAndConquer(lists, mid + 1, end);

        return mergeTwoLists(left, right);
    }

    // Helper method to merge two sorted linked lists
    private ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;

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

        // Attach remaining nodes
        curr.next = (l1 != null) ? l1 : l2;
        return dummy.next;
    }

}

// APPROACH 2: Priority Queue/Min Heap
// Time: O(N log k), Space: O(k)
class SolutionPriorityQueue {

    public ListNode mergeKLists(ListNode[] lists) {
        if (lists == null || lists.length == 0)
            return null;

        // Priority queue to store nodes, ordered by value
        PriorityQueue<ListNode> pq = new PriorityQueue<>((a, b) -> a.val - b.val);

        // Add first node of each list to the queue
        for (ListNode node : lists) {
            if (node != null) {
                pq.offer(node);
            }
        }

        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;

        while (!pq.isEmpty()) {
            ListNode minNode = pq.poll();
            curr.next = minNode;
            curr = curr.next;

            // Add next node from the same list
            if (minNode.next != null) {
                pq.offer(minNode.next);
            }
        }

        return dummy.next;
    }

}

// APPROACH 3: Sequential Merge (Simple but less efficient)
// Time: O(k*N), Space: O(1)
class SolutionSequential {

    public ListNode mergeKLists(ListNode[] lists) {
        if (lists == null || lists.length == 0)
            return null;

        ListNode result = lists[0];
        for (int i = 1; i < lists.length; i++) {
            result = mergeTwoLists(result, lists[i]);
        }
        return result;
    }

    private ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;

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

        curr.next = (l1 != null) ? l1 : l2;
        return dummy.next;
    }

}

// APPROACH 4: Brute Force (Convert to array, sort, rebuild)
// Time: O(N log N), Space: O(N)
class SolutionBruteForce {

    public ListNode mergeKLists(ListNode[] lists) {
        if (lists == null || lists.length == 0)
            return null;

        List<Integer> values = new ArrayList<>();

        // Collect all values
        for (ListNode list : lists) {
            ListNode curr = list;
            while (curr != null) {
                values.add(curr.val);
                curr = curr.next;
            }
        }

        if (values.isEmpty())
            return null;

        // Sort values
        Collections.sort(values);

        // Build new linked list
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;

        for (int val : values) {
            curr.next = new ListNode(val);
            curr = curr.next;
        }

        return dummy.next;
    }

}

// Test class to demonstrate usage
class TestMergeKLists {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1: [[1,4,5],[1,3,4],[2,6]]
        ListNode[] lists = new ListNode[3];

        // Create first list: 1->4->5
        lists[0] = new ListNode(1);
        lists[0].next = new ListNode(4);
        lists[0].next.next = new ListNode(5);

        // Create second list: 1->3->4
        lists[1] = new ListNode(1);
        lists[1].next = new ListNode(3);
        lists[1].next.next = new ListNode(4);

        // Create third list: 2->6
        lists[2] = new ListNode(2);
        lists[2].next = new ListNode(6);

        ListNode result = solution.mergeKLists(lists);

        // Print result
        System.out.print("Merged list: ");
        printList(result);
    }

    private static void printList(ListNode head) {
        while (head != null) {
            System.out.print(head.val);
            if (head.next != null)
                System.out.print("->");
            head = head.next;
        }
        System.out.println();
    }

}
