import java.util.ArrayList;
import java.util.List;

/**
 * PROBLEM DESCRIPTION:
 * Given the head of a linked list and an integer k, return the head of the 
 * linked list after swapping the values of the kth node from the beginning 
 * and the kth node from the end of the linked list.
 * 
 * Note: We'll number the nodes of the linked list starting from 1 to n.
 * 
 * CONSTRAINTS:
 * - The linked list will have n number of nodes.
 * - 1 <= k <= n <= 500
 * - -5000 <= Node.val <= 5000
 * 
 * EXAMPLES:
 * Example 1:
 * Input: head = [1,2,3,4,5], k = 2
 * Output: [1,4,3,2,5]
 * Visual:
 * Original: 1 -> [2] -> 3 -> [4] -> 5
 *                 ^           ^
 *               kth start   kth end
 * Swapped:  1 -> [4] -> 3 -> [2] -> 5
 * 
 * Example 2:
 * Input: head = [7,9,6,6,7,8,3,0,9,5], k = 5
 * Output: [7,9,6,6,8,7,3,0,9,5]
 */

class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
    }
}

public class SwapNodesSolutions {

    /**
     * APPROACH 1: Optimal One-Pass (Two Pointers)
     * Time Complexity: O(N) where N is the number of nodes
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * We can find both nodes in a single pass using two pointers.
     * 1. Traverse to the k-th node from the beginning. Save it as `frontNode`.
     * 2. Initialize a `backNode` pointer at the `head`, and a `curr` pointer 
     *    at `frontNode`.
     * 3. Move both `curr` and `backNode` forward simultaneously. When `curr` 
     *    reaches the last node, `backNode` will exactly be at the k-th node 
     *    from the end.
     * 4. Swap the values of `frontNode` and `backNode`.
     * 
     * VISUALIZATION:
     * List: 1 -> 2 -> 3 -> 4 -> 5, k = 2
     * 
     * Step 1: Find k-th from start (k=2)
     * frontNode = 2
     * 
     * Step 2 & 3: Move curr and backNode
     * backNode starts at 1, curr starts at 2.
     * Move both until curr is at the end (5).
     * curr at 2 -> backNode at 1
     * curr at 3 -> backNode at 2
     * curr at 4 -> backNode at 3
     * curr at 5 -> backNode at 4  <-- backNode is now k-th from end!
     * 
     * Step 4: Swap values of frontNode (2) and backNode (4).
     */
    public ListNode swapNodesOptimal(ListNode head, int k) {
        if (head == null) return null;

        ListNode frontNode = head;
        // Move frontNode to the k-th node (1-indexed)
        for (int i = 1; i < k; i++) {
            frontNode = frontNode.next;
        }

        ListNode curr = frontNode;
        ListNode backNode = head;

        // Move both pointers until curr reaches the end
        while (curr.next != null) {
            curr = curr.next;
            backNode = backNode.next;
        }

        // Swap the values
        int temp = frontNode.val;
        frontNode.val = backNode.val;
        backNode.val = temp;

        return head;
    }


    /**
     * APPROACH 2: Two-Pass Approach (Counting Length)
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * A more intuitive but slightly slower approach. 
     * 1. Traverse the entire list once to find the total length (N).
     * 2. The k-th node from the end is simply the (N - k + 1)-th node from the start.
     * 3. Traverse the list again to find both the k-th node and the (N - k + 1)-th node.
     * 4. Swap their values.
     */
    public ListNode swapNodesTwoPass(ListNode head, int k) {
        if (head == null) return null;

        int length = 0;
        ListNode curr = head;
        
        // Pass 1: Find length
        while (curr != null) {
            length++;
            curr = curr.next;
        }

        ListNode frontNode = null;
        ListNode backNode = null;
        curr = head;

        // Pass 2: Find both nodes
        for (int i = 1; i <= length; i++) {
            if (i == k) {
                frontNode = curr;
            }
            if (i == length - k + 1) {
                backNode = curr;
            }
            curr = curr.next;
        }

        // Swap values
        if (frontNode != null && backNode != null) {
            int temp = frontNode.val;
            frontNode.val = backNode.val;
            backNode.val = temp;
        }

        return head;
    }


    /**
     * APPROACH 3: Array Representation (Extra Space)
     * Time Complexity: O(N)
     * Space Complexity: O(N) for storing node references in the list
     * 
     * EXPLANATION:
     * By converting the linked list into an array list, we gain O(1) random 
     * access to any index. We can easily access the k-th node at index `k-1` 
     * and the k-th node from the end at index `size - k`.
     * 
     * Note: While this approach is easier to write, it is the least efficient 
     * in terms of memory utilization.
     */
    public ListNode swapNodesUsingArray(ListNode head, int k) {
        if (head == null) return null;

        List<ListNode> nodes = new ArrayList<>();
        ListNode curr = head;

        // Store references to all nodes
        while (curr != null) {
            nodes.add(curr);
            curr = curr.next;
        }

        // Find the indices
        int frontIndex = k - 1;
        int backIndex = nodes.size() - k;

        // Swap values
        int temp = nodes.get(frontIndex).val;
        nodes.get(frontIndex).val = nodes.get(backIndex).val;
        nodes.get(backIndex).val = temp;

        return head;
    }


    /**
     * UTILITY: Create List from Array
     * Helper method to quickly generate a linked list for testing.
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
     * Helper method to print the list to the console.
     */
    public static void printList(ListNode head) {
        ListNode curr = head;
        while (curr != null) {
            System.out.print(curr.val + (curr.next != null ? " -> " : " -> null\n"));
            curr = curr.next;
        }
    }


    /**
     * MAIN METHOD: Demonstrating the approaches with examples.
     */
    public static void main(String[] args) {
        SwapNodesSolutions solver = new SwapNodesSolutions();

        // Test Case 1: Optimal One-Pass
        ListNode head1 = createList(new int[]{1, 2, 3, 4, 5});
        System.out.println("Original List 1: 1 -> 2 -> 3 -> 4 -> 5 -> null (k = 2)");
        ListNode result1 = solver.swapNodesOptimal(head1, 2);
        System.out.print("Optimal Output:  ");
        printList(result1);
        System.out.println("-------------------------------------------------");

        // Test Case 2: Two-Pass Approach
        ListNode head2 = createList(new int[]{7, 9, 6, 6, 7, 8, 3, 0, 9, 5});
        System.out.println("Original List 2: 7 -> 9 -> 6 -> 6 -> 7 -> 8 -> 3 -> 0 -> 9 -> 5 -> null (k = 5)");
        ListNode result2 = solver.swapNodesTwoPass(head2, 5);
        System.out.print("Two-Pass Output: ");
        printList(result2);
        System.out.println("-------------------------------------------------");

        // Test Case 3: Array Approach (Edge case: k is exactly in the middle)
        ListNode head3 = createList(new int[]{1, 2, 3});
        System.out.println("Original List 3: 1 -> 2 -> 3 -> null (k = 2)");
        ListNode result3 = solver.swapNodesUsingArray(head3, 2);
        System.out.print("Array Output:    ");
        printList(result3);
    }
}
