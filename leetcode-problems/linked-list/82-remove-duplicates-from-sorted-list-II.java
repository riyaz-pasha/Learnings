/*
 * Given the head of a sorted linked list, delete all nodes that have duplicate
 * numbers, leaving only distinct numbers from the original list. Return the
 * linked list sorted as well.
 * 
 * Example 1:
 * Input: head = [1,2,3,3,4,4,5]
 * Output: [1,2,5]
 * 
 * Example 2:
 * Input: head = [1,1,1,2,3]
 * Output: [2,3]
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
     * One-pass solution using dummy node and two pointers
     * Time Complexity: O(n) where n is the number of nodes
     * Space Complexity: O(1) - only using constant extra space
     */
    public ListNode deleteDuplicates(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        // Create dummy node to handle edge cases
        ListNode dummy = new ListNode(0);
        dummy.next = head;

        ListNode prev = dummy; // Points to the last node before duplicates
        ListNode curr = head; // Current node being examined

        while (curr != null) {
            // Check if current node has duplicates
            if (curr.next != null && curr.val == curr.next.val) {
                int duplicateVal = curr.val;

                // Skip all nodes with the duplicate value
                while (curr != null && curr.val == duplicateVal) {
                    curr = curr.next;
                }

                // Connect prev to the node after all duplicates
                prev.next = curr;
            } else {
                // No duplicates found, move prev pointer
                prev = curr;
                curr = curr.next;
            }
        }

        return dummy.next;
    }

    /**
     * Alternative solution with explicit duplicate detection
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public ListNode deleteDuplicatesAlternative(ListNode head) {
        if (head == null)
            return head;

        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode prev = dummy;

        while (head != null) {
            if (head.next != null && head.val == head.next.val) {
                // Found duplicates - skip all nodes with this value
                int val = head.val;
                while (head != null && head.val == val) {
                    head = head.next;
                }
                prev.next = head;
            } else {
                // No duplicates - keep this node
                prev = head;
                head = head.next;
            }
        }

        return dummy.next;
    }

    /**
     * Recursive solution
     * Time Complexity: O(n)
     * Space Complexity: O(n) due to recursion stack
     */
    public ListNode deleteDuplicatesRecursive(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        if (head.val == head.next.val) {
            // Skip all nodes with the same value
            int val = head.val;
            while (head != null && head.val == val) {
                head = head.next;
            }
            return deleteDuplicatesRecursive(head);
        } else {
            // Keep this node and recursively process the rest
            head.next = deleteDuplicatesRecursive(head.next);
            return head;
        }
    }

    /**
     * Solution using flag to track duplicates
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public ListNode deleteDuplicatesWithFlag(ListNode head) {
        if (head == null)
            return head;

        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode prev = dummy;
        ListNode curr = head;

        while (curr != null && curr.next != null) {
            if (curr.val == curr.next.val) {
                int duplicateVal = curr.val;

                // Skip all duplicates
                while (curr != null && curr.val == duplicateVal) {
                    curr = curr.next;
                }

                // Remove the duplicate sequence
                prev.next = curr;
            } else {
                prev = curr;
                curr = curr.next;
            }
        }

        return dummy.next;
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

        // Test Case 1: [1,2,3,3,4,4,5]
        // Expected: [1,2,5]
        System.out.println("Test Case 1:");
        int[] arr1 = { 1, 2, 3, 3, 4, 4, 5 };
        ListNode head1 = createList(arr1);
        System.out.print("Original: ");
        printList(head1);

        ListNode result1 = solution.deleteDuplicates(head1);
        System.out.print("After removing duplicates: ");
        printList(result1);
        System.out.println();

        // Test Case 2: [1,1,1,2,3]
        // Expected: [2,3]
        System.out.println("Test Case 2:");
        int[] arr2 = { 1, 1, 1, 2, 3 };
        ListNode head2 = createList(arr2);
        System.out.print("Original: ");
        printList(head2);

        ListNode result2 = solution.deleteDuplicatesAlternative(head2);
        System.out.print("After removing duplicates: ");
        printList(result2);
        System.out.println();

        // Test Case 3: All duplicates [1,1,2,2,3,3]
        // Expected: []
        System.out.println("Test Case 3 (All duplicates):");
        int[] arr3 = { 1, 1, 2, 2, 3, 3 };
        ListNode head3 = createList(arr3);
        System.out.print("Original: ");
        printList(head3);

        ListNode result3 = solution.deleteDuplicatesRecursive(head3);
        System.out.print("After removing duplicates (recursive): ");
        printList(result3);
        System.out.println();

        // Test Case 4: No duplicates [1,2,3,4,5]
        // Expected: [1,2,3,4,5]
        System.out.println("Test Case 4 (No duplicates):");
        int[] arr4 = { 1, 2, 3, 4, 5 };
        ListNode head4 = createList(arr4);
        System.out.print("Original: ");
        printList(head4);

        ListNode result4 = solution.deleteDuplicates(head4);
        System.out.print("After removing duplicates: ");
        printList(result4);
        System.out.println();

        // Test Case 5: Single element [1]
        // Expected: [1]
        System.out.println("Test Case 5 (Single element):");
        int[] arr5 = { 1 };
        ListNode head5 = createList(arr5);
        System.out.print("Original: ");
        printList(head5);

        ListNode result5 = solution.deleteDuplicates(head5);
        System.out.print("After removing duplicates: ");
        printList(result5);
        System.out.println();

        // Test Case 6: Two identical elements [1,1]
        // Expected: []
        System.out.println("Test Case 6 (Two identical elements):");
        int[] arr6 = { 1, 1 };
        ListNode head6 = createList(arr6);
        System.out.print("Original: ");
        printList(head6);

        ListNode result6 = solution.deleteDuplicatesWithFlag(head6);
        System.out.print("After removing duplicates: ");
        printList(result6);
        System.out.println();

        // Test Case 7: Complex case [1,2,2,3,4,4,4,5,5,6]
        // Expected: [1,3,6]
        System.out.println("Test Case 7 (Complex case):");
        int[] arr7 = { 1, 2, 2, 3, 4, 4, 4, 5, 5, 6 };
        ListNode head7 = createList(arr7);
        System.out.print("Original: ");
        printList(head7);

        ListNode result7 = solution.deleteDuplicates(head7);
        System.out.print("After removing duplicates: ");
        printList(result7);
    }

}
