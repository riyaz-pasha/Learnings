/**
 * PROBLEM DESCRIPTION:
 * The task is to reverse the nodes in groups of k in a given linked list, 
 * where k is a positive integer, and at most the length of the linked list. 
 * If any remaining nodes are not part of a group of k, they should remain 
 * in their original order.
 * 
 * It is not allowed to change the values of the nodes in the linked list. 
 * Only the order of the nodes can be modified.
 * 
 * Note: Use only O(1) extra memory space.
 * 
 * CONSTRAINTS:
 * - Let n be the number of nodes in a linked list.
 * - 1 <= k <= n <= 500
 * - 0 <= Node.val <= 1000
 * 
 * EXAMPLES:
 * Example 1:
 * Input: head = [1,2,3,4,5], k = 2
 * Output: [2,1,4,3,5]
 * Visual:
 * Original: [1 -> 2] -> [3 -> 4] -> 5
 * Swapped:  [2 -> 1] -> [4 -> 3] -> 5
 * 
 * Example 2:
 * Input: head = [1,2,3,4,5], k = 3
 * Output: [3,2,1,4,5]
 * Visual:
 * Original: [1 -> 2 -> 3] -> 4 -> 5
 * Swapped:  [3 -> 2 -> 1] -> 4 -> 5
 */

class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
    }
}

public class ReverseKGroupSolutions {

    /**
     * APPROACH 1: Iterative (Optimal & Strictly meets O(1) space constraint)
     * Time Complexity: O(N) where N is the total number of nodes in the list.
     * Space Complexity: O(1) extra space.
     * 
     * EXPLANATION:
     * 1. Count the total number of nodes in the linked list.
     * 2. Use a dummy node to handle edge cases cleanly (e.g., when the head 
     *    itself gets reversed).
     * 3. Loop as long as there are at least 'k' nodes left to reverse.
     * 4. Inside the loop, reverse exactly 'k' nodes using standard linked list 
     *    reversal logic.
     * 5. Reconnect the newly reversed k-group to the previous part of the list 
     *    and the remaining part of the list.
     * 6. Decrement the count by 'k' and move the pointers forward.
     * 
     * VISUALIZATION (k = 3):
     * List: dummy -> 1 -> 2 -> 3 -> 4 -> 5, count = 5
     * 
     * Initial Setup for first group:
     * prevGroupTail = dummy
     * curr = 1
     * 
     * Reversal of first 3 nodes:
     * 1 <- 2 <- 3   (Reversed)
     * 
     * Reconnection:
     * 1) '1' was the start of the group, now it's the tail. We connect it to '4'.
     *    (prevGroupTail.next.next = curr)  -> 1.next = 4
     * 2) 'dummy' needs to connect to the new head of this group, which is '3'.
     *    (dummy.next = prev)               -> dummy.next = 3
     * 
     * After Reconnection:
     * dummy -> 3 -> 2 -> 1 -> 4 -> 5
     * 
     * Update prevGroupTail:
     * prevGroupTail becomes '1' (the tail of the just reversed group).
     * count = count - 3 = 2.
     * Since 2 < k, we stop.
     */
    public ListNode reverseKGroupIterative(ListNode head, int k) {
        if (head == null || k == 1) return head;

        // Step 1: Count total nodes
        int count = 0;
        ListNode curr = head;
        while (curr != null) {
            count++;
            curr = curr.next;
        }

        // Step 2: Set up dummy node
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode prevGroupTail = dummy;

        // Step 3: Reverse in groups of k
        while (count >= k) {
            curr = prevGroupTail.next; // First node of the current k-group
            ListNode prev = null;
            ListNode next = null;
            
            // Reverse exactly k nodes
            for (int i = 0; i < k; i++) {
                next = curr.next;
                curr.next = prev;
                prev = curr;
                curr = next;
            }

            // At this point, 'prev' is the new head of the reversed group,
            // and 'curr' points to the first node of the next group.
            
            // prevGroupTail.next is still pointing to the ORIGINAL first node of this group 
            // (which is now the tail of the reversed group).
            ListNode reversedGroupTail = prevGroupTail.next;
            
            // Connect the newly reversed group to the remaining unreversed nodes
            reversedGroupTail.next = curr;
            
            // Connect the previous part of the list to the new head of the reversed group
            prevGroupTail.next = prev;
            
            // Move prevGroupTail forward for the next iteration
            prevGroupTail = reversedGroupTail;
            
            count -= k;
        }

        return dummy.next;
    }


    /**
     * APPROACH 2: Recursive
     * Time Complexity: O(N)
     * Space Complexity: O(N/k) auxiliary space due to the call stack. 
     * NOTE: Strictly speaking, this violates the O(1) space constraint, 
     * but it is a very common and elegant alternative solution in interviews.
     * 
     * EXPLANATION:
     * 1. Check if there are at least 'k' nodes remaining. If not, return head.
     * 2. Reverse the first 'k' nodes.
     * 3. Recursively call the function for the remaining list.
     * 4. Link the original head of this k-group (which is now the tail) to the 
     *    result of the recursive call.
     * 5. Return the new head of the reversed group.
     */
    public ListNode reverseKGroupRecursive(ListNode head, int k) {
        if (head == null || k == 1) return head;
        
        // Step 1: Check if we have k nodes left
        ListNode curr = head;
        int count = 0;
        while (curr != null && count != k) {
            curr = curr.next;
            count++;
        }
        
        // If we have k nodes, reverse them
        if (count == k) {
            curr = head;
            ListNode prev = null;
            ListNode next = null;
            
            // Reverse k nodes
            for (int i = 0; i < k; i++) {
                next = curr.next;
                curr.next = prev;
                prev = curr;
                curr = next;
            }
            
            // 'head' is now the tail of this reversed group.
            // Link it to the result of recursively processing the rest of the list.
            // Note: 'curr' is pointing to the (k+1)th node.
            if (next != null) {
                head.next = reverseKGroupRecursive(curr, k);
            }
            
            // 'prev' is the new head of the reversed group
            return prev;
        }
        
        // If less than k nodes remain, return head unchanged
        return head;
    }


    /**
     * UTILITY: Create List from Array
     */
    public static ListNode createList(int[] arr) {
        if (arr == null || arr.length == 0) return null;
        ListNode head = new ListNode(arr[0]);
        ListNode curr = head;
        for (int i = 1; i < arr.length; i++) {
            curr.next = new ListNode(arr[i]);
            curr = curr.next;
        }
        return head;
    }

    /**
     * UTILITY: Print List
     */
    public static void printList(ListNode head) {
        if (head == null) {
            System.out.println("[]");
            return;
        }
        ListNode curr = head;
        System.out.print("[");
        while (curr != null) {
            System.out.print(curr.val + (curr.next != null ? ", " : "]\n"));
            curr = curr.next;
        }
    }


    /**
     * MAIN METHOD: Demonstrating the approaches with examples.
     */
    public static void main(String[] args) {
        ReverseKGroupSolutions solver = new ReverseKGroupSolutions();

        // Test Case 1: Iterative Approach (k = 2)
        ListNode head1 = createList(new int[]{1, 2, 3, 4, 5});
        System.out.println("Original List 1: [1, 2, 3, 4, 5] (k = 2)");
        ListNode result1 = solver.reverseKGroupIterative(head1, 2);
        System.out.print("Iterative Output: ");
        printList(result1);
        System.out.println("-------------------------------------------------");

        // Test Case 2: Iterative Approach (k = 3)
        ListNode head2 = createList(new int[]{1, 2, 3, 4, 5});
        System.out.println("Original List 2: [1, 2, 3, 4, 5] (k = 3)");
        ListNode result2 = solver.reverseKGroupIterative(head2, 3);
        System.out.print("Iterative Output: ");
        printList(result2);
        System.out.println("-------------------------------------------------");

        // Test Case 3: Recursive Approach (k = 2)
        ListNode head3 = createList(new int[]{1, 2, 3, 4, 5});
        System.out.println("Original List 3: [1, 2, 3, 4, 5] (k = 2)");
        ListNode result3 = solver.reverseKGroupRecursive(head3, 2);
        System.out.print("Recursive Output: ");
        printList(result3);
    }
}
