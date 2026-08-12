/**
 * PROBLEM DESCRIPTION:
 * Given a singly linked list, swap every two adjacent nodes of the linked list. 
 * After the swap, return the head of the linked list.
 * 
 * Note: Solve the problem without modifying the values in the list's nodes. 
 * In other words, only the nodes themselves can be changed.
 * 
 * CONSTRAINTS:
 * - The number of nodes in the list is in the range [0, 100].
 * - 0 <= Node.val <= 100
 * 
 * EXAMPLES:
 * Example 1:
 * Input: head = [1,2,3,4]
 * Output: [2,1,4,3]
 * Visual:
 * Original: 1 -> 2 -> 3 -> 4 -> null
 * Swapped:  2 -> 1 -> 4 -> 3 -> null
 * 
 * Example 2:
 * Input: head = []
 * Output: []
 * 
 * Example 3:
 * Input: head = [1]
 * Output: [1]
 */

class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
    }
}

public class SwapPairsSolutions {

    /**
     * APPROACH 1: Iterative using a Dummy Node (Optimal for Memory)
     * Time Complexity: O(N) where N is the number of nodes
     * Space Complexity: O(1) auxiliary space
     * 
     * EXPLANATION:
     * To handle the edge case of swapping the head node cleanly, we use a 
     * "dummy" node that points to the head. We use a 'prev' pointer to keep 
     * track of the node just before the pair we are currently swapping.
     * 
     * For every pair of nodes (firstNode and secondNode):
     * 1. Point 'prev' to 'secondNode'.
     * 2. Point 'firstNode' to whatever comes after 'secondNode'.
     * 3. Point 'secondNode' back to 'firstNode'.
     * 4. Move 'prev' forward by two nodes (to 'firstNode') to prep for the next pair.
     * 
     * VISUALIZATION:
     * Initial: dummy -> 1 -> 2 -> 3 -> 4 -> null
     *          (prev)  (1st) (2nd)
     * 
     * Step 1 (Swap 1 and 2):
     * prev.next = 2
     * 1.next = 2.next (which is 3)
     * 2.next = 1
     * 
     * List now: dummy -> 2 -> 1 -> 3 -> 4 -> null
     * Move prev: prev = 1
     * 
     * Step 2 (Swap 3 and 4):
     * prev points at 1, 1st is 3, 2nd is 4.
     * prev.next = 4
     * 3.next = 4.next (null)
     * 4.next = 3
     * 
     * List now: dummy -> 2 -> 1 -> 4 -> 3 -> null
     */
    public ListNode swapPairsIterative(ListNode head) {
        // If list is empty or has only one node, no swaps are needed.
        if (head == null || head.next == null) {
            return head;
        }

        // Dummy node helps us avoid special handling for the head
        ListNode dummy = new ListNode(-1);
        dummy.next = head;
        
        ListNode prev = dummy;

        // Loop as long as there are at least two nodes left to swap
        while (prev.next != null && prev.next.next != null) {
            // Identify the two nodes to swap
            ListNode firstNode = prev.next;
            ListNode secondNode = prev.next.next;

            // Perform the swap
            prev.next = secondNode;
            firstNode.next = secondNode.next;
            secondNode.next = firstNode;

            // Move the prev pointer up for the next pair
            prev = firstNode;
        }

        // The new head of the list is safely stored in dummy.next
        return dummy.next;
    }


    /**
     * APPROACH 2: Recursive
     * Time Complexity: O(N)
     * Space Complexity: O(N) due to the recursion call stack
     * 
     * EXPLANATION:
     * We can define the problem recursively: swap the first two nodes, and then 
     * recursively call the function on the remainder of the list. The first node 
     * will then point to the result of the recursive call.
     * 
     * 1. Base case: If head is null or head.next is null, return head.
     * 2. Store the second node.
     * 3. Set first node's next to the result of swapPairs(second.next).
     * 4. Set second node's next to the first node.
     * 5. Return the second node (which is now the head of this swapped pair).
     * 
     * VISUALIZATION (Call Stack):
     * swapPairs(1 -> 2 -> 3 -> 4)
     * |   firstNode = 1, secondNode = 2
     * |   1.next = swapPairs(3 -> 4)
     * |   |   firstNode = 3, secondNode = 4
     * |   |   3.next = swapPairs(null) -> returns null
     * |   |   4.next = 3
     * |   |   Returns 4 -> 3 -> null
     * |   1.next becomes (4 -> 3 -> null)
     * |   2.next = 1
     * |   Returns 2 -> 1 -> 4 -> 3 -> null
     */
    public ListNode swapPairsRecursive(ListNode head) {
        // Base Case: 0 or 1 node left
        if (head == null || head.next == null) {
            return head;
        }

        // Identify the nodes to swap
        ListNode firstNode = head;
        ListNode secondNode = head.next;

        // Recursively call for the rest of the list and link it to firstNode
        firstNode.next = swapPairsRecursive(secondNode.next);
        
        // Complete the swap for the current pair
        secondNode.next = firstNode;

        // Return the new head of this sub-list
        return secondNode;
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
        SwapPairsSolutions solver = new SwapPairsSolutions();

        // Test Case 1: Iterative Approach
        ListNode head1 = createList(new int[]{1, 2, 3, 4});
        System.out.println("Original List 1 (Iterative): [1, 2, 3, 4]");
        ListNode result1 = solver.swapPairsIterative(head1);
        System.out.print("Output 1:                    ");
        printList(result1);
        System.out.println("-------------------------------------------------");

        // Test Case 2: Recursive Approach
        ListNode head2 = createList(new int[]{1, 2, 3, 4, 5});
        System.out.println("Original List 2 (Recursive): [1, 2, 3, 4, 5] (Odd number of elements)");
        ListNode result2 = solver.swapPairsRecursive(head2);
        System.out.print("Output 2:                    ");
        printList(result2);
        System.out.println("-------------------------------------------------");

        // Test Case 3: Edge Cases (Empty list & Single element)
        ListNode head3 = createList(new int[]{});
        System.out.println("Original List 3 (Empty):     []");
        ListNode result3 = solver.swapPairsIterative(head3);
        System.out.print("Output 3:                    ");
        printList(result3);
        
        ListNode head4 = createList(new int[]{1});
        System.out.println("Original List 4 (Single):    [1]");
        ListNode result4 = solver.swapPairsRecursive(head4);
        System.out.print("Output 4:                    ");
        printList(result4);
    }
}
