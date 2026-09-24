/*
 * Given the head of a linked list, reverse the nodes of the list k at a time,
 * and return the modified list.
 * 
 * k is a positive integer and is less than or equal to the length of the linked
 * list. If the number of nodes is not a multiple of k then left-out nodes, in
 * the end, should remain as it is.
 * 
 * You may not alter the values in the list's nodes, only nodes themselves may
 * be changed.
 * 
 * 
 * Example 1:
 * Input: head = [1,2,3,4,5], k = 2
 * Output: [2,1,4,3,5]
 * 
 * Example 2:
 * Input: head = [1,2,3,4,5], k = 3
 * Output: [3,2,1,4,5]
 */

 /**
 * Definition for singly-linked list.
 */
class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}

class Solution {
    /**
     * Reverses nodes of the list k at a time
     * Time Complexity: O(n) where n is the number of nodes
     * Space Complexity: O(1) - only using constant extra space
     */
    public ListNode reverseKGroup(ListNode head, int k) {
        if (head == null || k == 1) {
            return head;
        }
        
        // First, check if we have at least k nodes
        ListNode curr = head;
        int count = 0;
        while (curr != null && count < k) {
            curr = curr.next;
            count++;
        }
        
        // If we have k nodes, reverse them
        if (count == k) {
            // Recursively reverse the rest of the list
            curr = reverseKGroup(curr, k);
            
            // Reverse the current k-group
            while (count > 0) {
                ListNode temp = head.next;
                head.next = curr;
                curr = head;
                head = temp;
                count--;
            }
            head = curr;
        }
        
        return head;
    }

    public ListNode reverseKGroup2(ListNode head, int k) {
        if (head == null || k == 1) {
            return head;
        }

        // Check if there are at least k nodes ahead
        ListNode node = head;
        int count = 0;
        while (node != null && count < k) {
            node = node.next;
            count++;
        }

        // If we have k nodes, reverse them
        if (count == k) {
            // Recursively reverse the remaining list starting from node
            ListNode prev = reverseKGroup(node, k);

            // Reverse the current k nodes
            while (count-- > 0) {
                ListNode next = head.next;
                head.next = prev;
                prev = head;
                head = next;
            }

            head = prev; // New head of the reversed group
        }

        return head;
    }

    /**
     * Alternative iterative solution
     * More intuitive approach using dummy node
     */
    public ListNode reverseKGroupIterative(ListNode head, int k) {
        if (head == null || k == 1) {
            return head;
        }
        
        // Create dummy node to simplify edge cases
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode prevGroupEnd = dummy;
        
        while (true) {
            // Check if we have k nodes remaining
            ListNode kthNode = getKthNode(prevGroupEnd, k);
            if (kthNode == null) {
                break;
            }
            
            ListNode nextGroupStart = kthNode.next;
            
            // Reverse the k-group
            ListNode prev = nextGroupStart;
            ListNode curr = prevGroupEnd.next;
            
            while (curr != nextGroupStart) {
                ListNode temp = curr.next;
                curr.next = prev;
                prev = curr;
                curr = temp;
            }
            
            // Connect with previous group
            ListNode temp = prevGroupEnd.next;
            prevGroupEnd.next = kthNode;
            prevGroupEnd = temp;
        }
        
        return dummy.next;
    }
    
    /**
     * Helper method to get the kth node from a given starting point
     */
    private ListNode getKthNode(ListNode start, int k) {
        ListNode curr = start;
        while (curr != null && k > 0) {
            curr = curr.next;
            k--;
        }
        return curr;
    }
    
    /**
     * Helper method to create a linked list from array (for testing)
     */
    public static ListNode createList(int[] arr) {
        if (arr.length == 0) return null;
        
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
        ListNode curr = head;
        while (curr != null) {
            System.out.print(curr.val);
            if (curr.next != null) {
                System.out.print(" -> ");
            }
            curr = curr.next;
        }
        System.out.println();
    }
    
    /**
     * Test the solution
     */
    public static void main(String[] args) {
        Solution solution = new Solution();
        
        // Test Case 1: [1,2,3,4,5], k = 2
        // Expected: [2,1,4,3,5]
        int[] arr1 = {1, 2, 3, 4, 5};
        ListNode head1 = createList(arr1);
        System.out.println("Original: ");
        printList(head1);
        
        ListNode result1 = solution.reverseKGroup(head1, 2);
        System.out.println("After reversing in groups of 2:");
        printList(result1);
        System.out.println();
        
        // Test Case 2: [1,2,3,4,5], k = 3
        // Expected: [3,2,1,4,5]
        int[] arr2 = {1, 2, 3, 4, 5};
        ListNode head2 = createList(arr2);
        System.out.println("Original: ");
        printList(head2);
        
        ListNode result2 = solution.reverseKGroupIterative(head2, 3);
        System.out.println("After reversing in groups of 3:");
        printList(result2);
        System.out.println();
        
        // Test Case 3: Edge case - k = 1
        int[] arr3 = {1, 2, 3, 4};
        ListNode head3 = createList(arr3);
        System.out.println("Original: ");
        printList(head3);
        
        ListNode result3 = solution.reverseKGroup(head3, 1);
        System.out.println("After reversing in groups of 1:");
        printList(result3);
    }

}

/*
 * 🧠 How to Remember Linked List Reversal: The 3-Pointer Mental Model
 *
 * You only need three pointers to reverse a singly linked list:
 *     prev, curr, and next
 *
 * Visual Example:
 *     Original List:  A -> B -> C -> D -> null
 *
 *     Initial State:
 *         prev = null
 *         curr = A
 *         next = B (saved using curr.next)
 *
 * Step-by-step Process:
 * 1. Save the next node:
 *        next = curr.next   // Keep track of what's next
 *
 * 2. Reverse the link:
 *        curr.next = prev   // Reverse the current pointer
 *
 * 3. Move the pointers forward:
 *        prev = curr        // Move prev to current node
 *        curr = next        // Move curr to next node
 *
 * Repeat these steps until curr becomes null.
 *
 * Final State after reversal:
 *     D -> C -> B -> A -> null
 *
 *     prev = D (new head of reversed list)
 *     curr = null (end of list)
 *
 * This pattern is commonly used in both full list reversal and segment-based reversal (e.g., reverse in groups of K).
 */
