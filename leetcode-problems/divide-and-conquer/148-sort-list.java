/*
 * Given the head of a linked list, return the list after sorting it in
 * ascending order.
 * 
 * Example 1:
 * Input: head = [4,2,1,3]
 * Output: [1,2,3,4]
 * 
 * Example 2:
 * Input: head = [-1,5,3,4,0]
 * Output: [-1,0,3,4,5]
 * 
 * Example 3:
 * Input: head = []
 * Output: []
 */

/**
 * Definition for singly-linked list.
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

    // Solution 1: Merge Sort (Optimal - O(n log n) time, O(log n) space)
    public ListNode sortList(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        // Split the list into two halves
        ListNode mid = getMid(head);
        ListNode left = head;
        ListNode right = mid.next;
        mid.next = null; // Break the connection

        // Recursively sort both halves
        left = sortList(left);
        right = sortList(right);

        // Merge the sorted halves
        return merge(left, right);
    }

    private ListNode getMid(ListNode head) {
        ListNode slow = head;
        ListNode fast = head;
        ListNode prev = null;

        while (fast != null && fast.next != null) {
            prev = slow;
            slow = slow.next;
            fast = fast.next.next;
        }

        return prev;
    }

    private ListNode merge(ListNode l1, ListNode l2) {
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

    // Solution 2: Convert to Array and Sort (Simple - O(n log n) time, O(n) space)
    public ListNode sortList2(ListNode head) {
        if (head == null)
            return null;

        // Convert linked list to array
        java.util.List<Integer> values = new java.util.ArrayList<>();
        ListNode curr = head;
        while (curr != null) {
            values.add(curr.val);
            curr = curr.next;
        }

        // Sort the array
        java.util.Collections.sort(values);

        // Rebuild the linked list
        ListNode dummy = new ListNode(0);
        curr = dummy;
        for (int val : values) {
            curr.next = new ListNode(val);
            curr = curr.next;
        }

        return dummy.next;
    }

    // Solution 3: Bottom-up Merge Sort (Most Optimal - O(n log n) time, O(1) space)
    public ListNode sortList3(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        // Get the length of the list
        int length = getLength(head);

        ListNode dummy = new ListNode(0);
        dummy.next = head;

        // Bottom-up merge sort
        for (int size = 1; size < length; size *= 2) {
            ListNode prev = dummy;
            ListNode curr = dummy.next;

            while (curr != null) {
                ListNode left = curr;
                ListNode right = split(left, size);
                curr = split(right, size);

                prev = mergeBottomUp(prev, left, right);
            }
        }

        return dummy.next;
    }

    private int getLength(ListNode head) {
        int length = 0;
        while (head != null) {
            length++;
            head = head.next;
        }
        return length;
    }

    private ListNode split(ListNode head, int size) {
        for (int i = 1; i < size && head != null; i++) {
            head = head.next;
        }

        if (head == null)
            return null;

        ListNode next = head.next;
        head.next = null;
        return next;
    }

    private ListNode mergeBottomUp(ListNode prev, ListNode l1, ListNode l2) {
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                prev.next = l1;
                l1 = l1.next;
            } else {
                prev.next = l2;
                l2 = l2.next;
            }
            prev = prev.next;
        }

        prev.next = (l1 != null) ? l1 : l2;

        // Move prev to the end of the merged list
        while (prev.next != null) {
            prev = prev.next;
        }

        return prev;
    }

    // Helper method to create a linked list from array (for testing)
    public static ListNode createList(int[] arr) {
        if (arr.length == 0)
            return null;

        ListNode head = new ListNode(arr[0]);
        ListNode curr = head;

        for (int i = 1; i < arr.length; i++) {
            curr.next = new ListNode(arr[i]);
            curr = curr.next;
        }

        return head;
    }

    // Helper method to print linked list (for testing)
    public static void printList(ListNode head) {
        while (head != null) {
            System.out.print(head.val + " ");
            head = head.next;
        }
        System.out.println();
    }

    // Test the solutions
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1: [4,2,1,3]
        ListNode head1 = createList(new int[] { 4, 2, 1, 3 });
        System.out.print("Input: ");
        printList(head1);

        ListNode sorted1 = solution.sortList(head1);
        System.out.print("Output: ");
        printList(sorted1);

        // Test case 2: [-1,5,3,4,0]
        ListNode head2 = createList(new int[] { -1, 5, 3, 4, 0 });
        System.out.print("Input: ");
        printList(head2);

        ListNode sorted2 = solution.sortList(head2);
        System.out.print("Output: ");
        printList(sorted2);

        // Test case 3: []
        ListNode head3 = createList(new int[] {});
        System.out.print("Input: ");
        printList(head3);

        ListNode sorted3 = solution.sortList(head3);
        System.out.print("Output: ");
        printList(sorted3);
    }

}
