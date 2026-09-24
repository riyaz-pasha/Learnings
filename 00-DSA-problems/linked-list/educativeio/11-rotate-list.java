import java.util.ArrayList;
import java.util.List;

/**
 * PROBLEM DESCRIPTION:
 * You are given the head of a singly linked list and a non-negative integer k. 
 * Your task is to rotate the list to the right by k positions.
 * 
 * A right rotation by 1 means the last node becomes the new head, and every 
 * other node shifts one position to the right.
 * 
 * Return the head of the rotated linked list.
 * 
 * CONSTRAINTS:
 * - The number of nodes in the list is in the range [0, 500].
 * - -100 <= Node.val <= 100
 * - 0 <= k <= 2 * 10^9
 * 
 * EXAMPLES:
 * Example 1:
 * Input: head = [1,2,3,4,5], k = 2
 * Output: [4,5,1,2,3]
 * Visual:
 * Original:     1 -> 2 -> 3 -> 4 -> 5 -> null
 * Rotate 1:     5 -> 1 -> 2 -> 3 -> 4 -> null
 * Rotate 2:     4 -> 5 -> 1 -> 2 -> 3 -> null
 * 
 * Example 2:
 * Input: head = [0,1,2], k = 4
 * Output: [2,0,1]
 * Visual:
 * length = 3. k = 4. 4 % 3 = 1. Rotating by 4 is the same as rotating by 1.
 * Rotate 1:     2 -> 0 -> 1 -> null
 */

class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
    }
}

public class RotateRightSolutions {

    /**
     * APPROACH 1: Optimal (Make Circular & Break)
     * Time Complexity: O(N) where N is the number of nodes. We traverse the list to find the length, 
     *                  and then traverse partially to find the new breaking point.
     * Space Complexity: O(1) extra space.
     * 
     * EXPLANATION:
     * Since 'k' can be much larger than the list's length (up to 2 billion), 
     * rotating by 'k' is mathematically equivalent to rotating by 'k % length'.
     * 
     * 1. Traverse the list to find its length and locate the `tail` node.
     * 2. Calculate the effective rotations needed: `effectiveK = k % length`.
     *    If `effectiveK == 0`, the list remains unchanged, so return `head`.
     * 3. Connect the `tail` to the `head` to make the list a circular ring.
     * 4. The new tail of the list will be at position `length - effectiveK - 1` 
     *    from the start. Traverse to this node.
     * 5. The new head will be `newTail.next`.
     * 6. Break the circle by setting `newTail.next = null`.
     * 
     * VISUALIZATION (k = 2):
     * List: 1 -> 2 -> 3 -> 4 -> 5, length = 5, k = 2
     * effectiveK = 2 % 5 = 2
     * 
     * Step 3 (Make Circular):
     * 1 -> 2 -> 3 -> 4 -> 5
     * ^                   |
     * |___________________|
     * 
     * Step 4 (Find New Tail):
     * jumps to new tail = length - effectiveK - 1 = 5 - 2 - 1 = 2 jumps.
     * start at 1, jump 1 -> 2, jump 2 -> 3.
     * newTail = 3
     * 
     * Step 5 & 6 (Break Circle):
     * newHead = newTail.next = 4
     * newTail.next = null (3 connects to null)
     * 
     * Result: 4 -> 5 -> 1 -> 2 -> 3 -> null
     */
    public ListNode rotateRightOptimal(ListNode head, int k) {
        // Base cases: empty list, single node, or no rotations requested
        if (head == null || head.next == null || k == 0) {
            return head;
        }

        // Step 1: Find the length and the original tail
        int length = 1;
        ListNode tail = head;
        while (tail.next != null) {
            length++;
            tail = tail.next;
        }

        // Step 2: Calculate effective rotations
        int effectiveK = k % length;
        if (effectiveK == 0) {
            return head; // No rotation needed
        }

        // Step 3: Make the list circular
        tail.next = head;

        // Step 4: Find the new tail
        int stepsToNewTail = length - effectiveK - 1;
        ListNode newTail = head;
        for (int i = 0; i < stepsToNewTail; i++) {
            newTail = newTail.next;
        }

        // Step 5 & 6: Break the circle and set new head
        ListNode newHead = newTail.next;
        newTail.next = null;

        return newHead;
    }


    /**
     * APPROACH 2: Array Rebuilding (Extra Space)
     * Time Complexity: O(N)
     * Space Complexity: O(N) due to storing nodes in an ArrayList.
     * 
     * EXPLANATION:
     * This approach uses an ArrayList to store the nodes, allowing O(1) access to any index. 
     * We calculate the new position of each node mathematically and link them together.
     * While this violates the implicit O(1) space expectation of linked list problems, 
     * it acts as a robust conceptual alternative if pointer manipulation is confusing.
     */
    public ListNode rotateRightUsingArray(ListNode head, int k) {
        if (head == null || head.next == null || k == 0) {
            return head;
        }

        // Store all nodes in a list
        List<ListNode> nodes = new ArrayList<>();
        ListNode curr = head;
        while (curr != null) {
            nodes.add(curr);
            curr = curr.next;
        }

        int n = nodes.size();
        int effectiveK = k % n;
        
        if (effectiveK == 0) return head;

        // The node at index (n - effectiveK) becomes the new head
        int splitIndex = n - effectiveK;
        ListNode newHead = nodes.get(splitIndex);
        
        // Link the end of the array back to the start
        nodes.get(n - 1).next = nodes.get(0);
        
        // Break the link right before the new head
        nodes.get(splitIndex - 1).next = null;

        return newHead;
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
        RotateRightSolutions solver = new RotateRightSolutions();

        // Test Case 1: Standard Rotation (k < length)
        ListNode head1 = createList(new int[]{1, 2, 3, 4, 5});
        System.out.println("Original List 1: [1, 2, 3, 4, 5], k = 2");
        ListNode result1 = solver.rotateRightOptimal(head1, 2);
        System.out.print("Optimal Output:  ");
        printList(result1);
        System.out.println("-------------------------------------------------");

        // Test Case 2: Rotation where k > length
        ListNode head2 = createList(new int[]{0, 1, 2});
        System.out.println("Original List 2: [0, 1, 2], k = 4");
        ListNode result2 = solver.rotateRightUsingArray(head2, 4);
        System.out.print("Array Output:    ");
        printList(result2);
        System.out.println("-------------------------------------------------");

        // Test Case 3: Rotation where k is a multiple of length (List remains unchanged)
        ListNode head3 = createList(new int[]{7, 8, 9});
        System.out.println("Original List 3: [7, 8, 9], k = 6");
        ListNode result3 = solver.rotateRightOptimal(head3, 6);
        System.out.print("Optimal Output:  ");
        printList(result3);
        System.out.println("-------------------------------------------------");
        
        // Test Case 4: Edge Case (Empty List)
        ListNode head4 = createList(new int[]{});
        System.out.println("Original List 4: [], k = 1");
        ListNode result4 = solver.rotateRightOptimal(head4, 1);
        System.out.print("Optimal Output:  ");
        printList(result4);
    }
}
