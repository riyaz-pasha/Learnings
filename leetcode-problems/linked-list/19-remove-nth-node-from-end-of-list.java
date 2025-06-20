/*
 * Given the head of a linked list, remove the nth node from the end of the list
 * and return its head.
 * 
 * Example 1:
 * Input: head = [1,2,3,4,5], n = 2
 * Output: [1,2,3,5]
 * 
 * Example 2:
 * Input: head = [1], n = 1
 * Output: []
 * 
 * Example 3:
 * Input: head = [1,2], n = 1
 * Output: [1]
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
     * One-pass solution using two pointers (fast and slow)
     * Time Complexity: O(n) where n is the number of nodes
     * Space Complexity: O(1) - only using constant extra space
     */
    public ListNode removeNthFromEnd(ListNode head, int n) {
        // Create dummy node to handle edge case where we remove the first node
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        
        ListNode fast = dummy;
        ListNode slow = dummy;
        
        // Move fast pointer n+1 steps ahead
        // This creates a gap of n nodes between fast and slow
        for (int i = 0; i <= n; i++) {
            fast = fast.next;
        }
        
        // Move both pointers until fast reaches the end
        // When fast is null, slow will be pointing to the node before the target
        while (fast != null) {
            fast = fast.next;
            slow = slow.next;
        }
        
        // Remove the nth node from end
        slow.next = slow.next.next;
        
        return dummy.next;
    }
    
    /**
     * Two-pass solution: first pass to count nodes, second pass to remove
     * Time Complexity: O(n) - two passes through the list
     * Space Complexity: O(1)
     */
    public ListNode removeNthFromEndTwoPass(ListNode head, int n) {
        // First pass: count the total number of nodes
        int length = 0;
        ListNode curr = head;
        while (curr != null) {
            length++;
            curr = curr.next;
        }
        
        // Edge case: removing the first node
        if (length == n) {
            return head.next;
        }
        
        // Second pass: find the node before the target node
        curr = head;
        for (int i = 0; i < length - n - 1; i++) {
            curr = curr.next;
        }
        
        // Remove the target node
        curr.next = curr.next.next;
        
        return head;
    }
    
    /**
     * Recursive solution
     * Time Complexity: O(n)
     * Space Complexity: O(n) due to recursion stack
     */
    public ListNode removeNthFromEndRecursive(ListNode head, int n) {
        int[] index = {0}; // Use array to pass by reference
        return removeHelper(head, n, index);
    }
    
    private ListNode removeHelper(ListNode node, int n, int[] index) {
        if (node == null) {
            return null;
        }
        
        node.next = removeHelper(node.next, n, index);
        index[0]++;
        
        // If this is the nth node from end, skip it
        if (index[0] == n) {
            return node.next;
        }
        
        return node;
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
     * Helper method to convert linked list to array (for testing)
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
     * Test the solution
     */
    public static void main(String[] args) {
        Solution solution = new Solution();
        
        // Test Case 1: [1,2,3,4,5], n = 2
        // Expected: [1,2,3,5]
        System.out.println("Test Case 1:");
        int[] arr1 = {1, 2, 3, 4, 5};
        ListNode head1 = createList(arr1);
        System.out.print("Original: ");
        printList(head1);
        
        ListNode result1 = solution.removeNthFromEnd(head1, 2);
        System.out.print("After removing 2nd from end: ");
        printList(result1);
        System.out.println();
        
        // Test Case 2: [1], n = 1
        // Expected: []
        System.out.println("Test Case 2:");
        int[] arr2 = {1};
        ListNode head2 = createList(arr2);
        System.out.print("Original: ");
        printList(head2);
        
        ListNode result2 = solution.removeNthFromEnd(head2, 1);
        System.out.print("After removing 1st from end: ");
        printList(result2);
        System.out.println();
        
        // Test Case 3: [1,2], n = 1
        // Expected: [1]
        System.out.println("Test Case 3:");
        int[] arr3 = {1, 2};
        ListNode head3 = createList(arr3);
        System.out.print("Original: ");
        printList(head3);
        
        ListNode result3 = solution.removeNthFromEnd(head3, 1);
        System.out.print("After removing 1st from end: ");
        printList(result3);
        System.out.println();
        
        // Test Case 4: Remove first node [1,2,3], n = 3
        // Expected: [2,3]
        System.out.println("Test Case 4 (Remove first node):");
        int[] arr4 = {1, 2, 3};
        ListNode head4 = createList(arr4);
        System.out.print("Original: ");
        printList(head4);
        
        ListNode result4 = solution.removeNthFromEndTwoPass(head4, 3);
        System.out.print("After removing 3rd from end: ");
        printList(result4);
        System.out.println();
        
        // Test Case 5: Using recursive solution
        System.out.println("Test Case 5 (Recursive solution):");
        int[] arr5 = {1, 2, 3, 4, 5};
        ListNode head5 = createList(arr5);
        System.out.print("Original: ");
        printList(head5);
        
        ListNode result5 = solution.removeNthFromEndRecursive(head5, 3);
        System.out.print("After removing 3rd from end (recursive): ");
        printList(result5);
    }

}
