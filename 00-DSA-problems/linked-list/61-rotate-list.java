/*
 * Given the head of a linked list, rotate the list to the right by k places.
 * 
 * Example 1:
 * Input: head = [1,2,3,4,5], k = 2
 * Output: [4,5,1,2,3]
 * 
 * Example 2:
 * Input: head = [0,1,2], k = 4
 * Output: [2,0,1]
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
    /**
     * Optimal solution: Make circular, then break at correct position
     * Time Complexity: O(n) where n is the number of nodes
     * Space Complexity: O(1) - only using constant extra space
     */
    public ListNode rotateRight(ListNode head, int k) {
        if (head == null || head.next == null || k == 0) {
            return head;
        }

        // Step 1: Find the length and make the list circular
        int length = 1;
        ListNode tail = head;
        while (tail.next != null) {
            tail = tail.next;
            length++;
        }

        // Connect tail to head to make it circular
        tail.next = head;

        // Step 2: Find the new tail position
        // Since we rotate right by k, new tail is at position (length - k % length - 1)
        k = k % length; // Handle cases where k > length
        int stepsToNewTail = length - k;

        // Step 3: Find the new tail and new head
        ListNode newTail = head;
        for (int i = 1; i < stepsToNewTail; i++) {
            newTail = newTail.next;
        }

        ListNode newHead = newTail.next;

        // Step 4: Break the circular connection
        newTail.next = null;

        return newHead;
    }

    /**
     * Two-pass solution: First pass to count, second pass to rotate
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public ListNode rotateRightTwoPass(ListNode head, int k) {
        if (head == null || head.next == null || k == 0) {
            return head;
        }

        // First pass: count the length
        int length = 0;
        ListNode curr = head;
        while (curr != null) {
            length++;
            curr = curr.next;
        }

        // Optimize k
        k = k % length;
        if (k == 0) {
            return head; // No rotation needed
        }

        // Second pass: find the breaking point
        // We need to find the (length - k)th node from the beginning
        ListNode newTail = head;
        for (int i = 1; i < length - k; i++) {
            newTail = newTail.next;
        }

        ListNode newHead = newTail.next;
        newTail.next = null;

        // Connect the original tail to the original head
        curr = newHead;
        while (curr.next != null) {
            curr = curr.next;
        }
        curr.next = head;

        return newHead;
    }

    /**
     * Alternative approach using two pointers with gap
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public ListNode rotateRightTwoPointers(ListNode head, int k) {
        if (head == null || head.next == null || k == 0) {
            return head;
        }

        // First, get the length
        int length = getLength(head);
        k = k % length;

        if (k == 0) {
            return head;
        }

        // Use two pointers with gap of k
        ListNode fast = head;
        ListNode slow = head;

        // Move fast pointer k steps ahead
        for (int i = 0; i < k; i++) {
            fast = fast.next;
        }

        // Move both pointers until fast reaches the last node
        while (fast.next != null) {
            fast = fast.next;
            slow = slow.next;
        }

        // Now slow is pointing to the new tail
        ListNode newHead = slow.next;
        slow.next = null;
        fast.next = head;

        return newHead;
    }

    /**
     * Helper method to get the length of the linked list
     */
    private int getLength(ListNode head) {
        int length = 0;
        ListNode curr = head;
        while (curr != null) {
            length++;
            curr = curr.next;
        }
        return length;
    }

    /**
     * Solution with detailed step-by-step approach
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public ListNode rotateRightDetailed(ListNode head, int k) {
        if (head == null || head.next == null) {
            return head;
        }

        // Step 1: Calculate length and find tail
        ListNode oldTail = head;
        int length = 1;
        while (oldTail.next != null) {
            oldTail = oldTail.next;
            length++;
        }

        // Step 2: Calculate effective rotation
        k = k % length;
        if (k == 0) {
            return head; // No rotation needed
        }

        // Step 3: Find new tail (length - k - 1 steps from head)
        ListNode newTail = head;
        for (int i = 0; i < length - k - 1; i++) {
            newTail = newTail.next;
        }

        // Step 4: Set new head and perform rotation
        ListNode newHead = newTail.next;
        newTail.next = null; // Break the connection
        oldTail.next = head; // Connect old tail to old head

        return newHead;
    }

    /**
     * Helper method to create a linked list from array (for testing)
     */
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

    /**
     * Helper method to print linked list (for testing)
     */
    public static void printList(ListNode head) {
        if (head == null) {
            System.out.println("[]");
            return;
        }

        System.out.print("[");
        ListNode curr = head;
        while (curr != null) {
            System.out.print(curr.val);
            if (curr.next != null) {
                System.out.print(",");
            }
            curr = curr.next;
        }
        System.out.println("]");
    }

    /**
     * Helper method to convert linked list to array (for verification)
     */
    public static int[] listToArray(ListNode head) {
        java.util.List<Integer> result = new java.util.ArrayList<>();
        ListNode curr = head;
        while (curr != null) {
            result.add(curr.val);
            curr = curr.next;
        }
        return result.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Test the solution
     */
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test Case 1: [1,2,3,4,5], k = 2
        // Expected: [4,5,1,2,3]
        System.out.println("Test Case 1:");
        int[] arr1 = { 1, 2, 3, 4, 5 };
        ListNode head1 = createList(arr1);
        System.out.print("Original: ");
        printList(head1);

        ListNode result1 = solution.rotateRight(head1, 2);
        System.out.print("After rotating right by 2: ");
        printList(result1);
        System.out.println();

        // Test Case 2: [0,1,2], k = 4
        // Expected: [2,0,1]
        System.out.println("Test Case 2:");
        int[] arr2 = { 0, 1, 2 };
        ListNode head2 = createList(arr2);
        System.out.print("Original: ");
        printList(head2);

        ListNode result2 = solution.rotateRightTwoPass(head2, 4);
        System.out.print("After rotating right by 4: ");
        printList(result2);
        System.out.println();

        // Test Case 3: Single node [1], k = 1
        // Expected: [1]
        System.out.println("Test Case 3 (Single node):");
        int[] arr3 = { 1 };
        ListNode head3 = createList(arr3);
        System.out.print("Original: ");
        printList(head3);

        ListNode result3 = solution.rotateRight(head3, 1);
        System.out.print("After rotating right by 1: ");
        printList(result3);
        System.out.println();

        // Test Case 4: k = 0 (no rotation)
        System.out.println("Test Case 4 (k = 0):");
        int[] arr4 = { 1, 2, 3, 4 };
        ListNode head4 = createList(arr4);
        System.out.print("Original: ");
        printList(head4);

        ListNode result4 = solution.rotateRight(head4, 0);
        System.out.print("After rotating right by 0: ");
        printList(result4);
        System.out.println();

        // Test Case 5: k equals list length [1,2,3], k = 3
        // Expected: [1,2,3] (no change)
        System.out.println("Test Case 5 (k equals length):");
        int[] arr5 = { 1, 2, 3 };
        ListNode head5 = createList(arr5);
        System.out.print("Original: ");
        printList(head5);

        ListNode result5 = solution.rotateRightTwoPointers(head5, 3);
        System.out.print("After rotating right by 3: ");
        printList(result5);
        System.out.println();

        // Test Case 6: Large k value [1,2], k = 99
        // Expected: [2,1] (k % 2 = 1)
        System.out.println("Test Case 6 (Large k):");
        int[] arr6 = { 1, 2 };
        ListNode head6 = createList(arr6);
        System.out.print("Original: ");
        printList(head6);

        ListNode result6 = solution.rotateRightDetailed(head6, 99);
        System.out.print("After rotating right by 99: ");
        printList(result6);
        System.out.println();

        // Test Case 7: Two nodes, k = 1 [1,2], k = 1
        // Expected: [2,1]
        System.out.println("Test Case 7 (Two nodes, k = 1):");
        int[] arr7 = { 1, 2 };
        ListNode head7 = createList(arr7);
        System.out.print("Original: ");
        printList(head7);

        ListNode result7 = solution.rotateRight(head7, 1);
        System.out.print("After rotating right by 1: ");
        printList(result7);
    }

}
