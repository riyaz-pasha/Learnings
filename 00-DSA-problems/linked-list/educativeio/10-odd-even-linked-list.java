import java.util.ArrayList;
import java.util.List;

/**
 * PROBLEM DESCRIPTION:
 * Given the head of a singly linked list, rearrange the nodes so that all nodes 
 * at odd indexes appear first, followed by all nodes at even indexes.
 * 
 * - The first node in the list is considered at odd index 1, the second at even index 2, etc.
 * - Within the odd group and the even group, the relative order of the nodes must 
 *   remain the same as in the original list.
 * 
 * Note: You must solve the problem in O(1) extra space complexity and O(n) time complexity.
 * 
 * CONSTRAINTS:
 * - The number of nodes in the linked list is in the range [0, 10^3].
 * - -10^3 <= Node.val <= 10^3
 * 
 * EXAMPLES:
 * Example 1:
 * Input: head = [1,2,3,4,5]
 * Output: [1,3,5,2,4]
 * Visual:
 * Indices:  1(O)  2(E)  3(O)  4(E)  5(O)
 * Original: [1] -> [2] -> [3] -> [4] -> [5] -> null
 * Odd List:  1 -> 3 -> 5
 * Even List: 2 -> 4
 * Merged:    1 -> 3 -> 5 -> 2 -> 4 -> null
 * 
 * Example 2:
 * Input: head = [2,1,3,5,6,4,7]
 * Output: [2,3,6,7,1,5,4]
 */

class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
    }
}

public class OddEvenLinkedListSolutions {

    /**
     * APPROACH 1: Optimal Two Pointers (Strictly meets constraints)
     * Time Complexity: O(N) where N is the number of nodes. We traverse the list once.
     * Space Complexity: O(1) extra space. We only use a few pointers.
     * 
     * EXPLANATION:
     * We can split the original list into two separate lists simultaneously: 
     * one for odd-indexed nodes and one for even-indexed nodes.
     * 
     * 1. Initialize `odd` pointer to the first node (head).
     * 2. Initialize `even` pointer to the second node (head.next).
     * 3. Keep a reference to the head of the even list (`evenHead`) so we can 
     *    attach it to the end of the odd list later.
     * 4. Iterate through the list:
     *    - Connect the current odd node to the next odd node (`odd.next = even.next`).
     *    - Move the `odd` pointer forward.
     *    - Connect the current even node to the next even node (`even.next = odd.next`).
     *    - Move the `even` pointer forward.
     * 5. Finally, attach the `evenHead` to the end of the newly formed odd list 
     *    (`odd.next = evenHead`).
     * 
     * VISUALIZATION:
     * Initial: 1 -> 2 -> 3 -> 4 -> 5 -> null
     * 
     * Step 1:
     * odd = 1, even = 2, evenHead = 2
     * 
     * Iteration 1:
     * odd.next = even.next (1 connects to 3)
     * odd = 3
     * even.next = odd.next (2 connects to 4)
     * even = 4
     * 
     * Iteration 2:
     * odd.next = even.next (3 connects to 5)
     * odd = 5
     * even.next = odd.next (4 connects to null)
     * even = null
     * 
     * End Loop (even is null).
     * Attach evenHead to end of odd:
     * odd.next = evenHead (5 connects to 2)
     * 
     * Result: 1 -> 3 -> 5 -> 2 -> 4 -> null
     */
    public ListNode oddEvenListOptimal(ListNode head) {
        // If the list is empty or has only one/two nodes, it's already sorted.
        if (head == null || head.next == null) {
            return head;
        }

        ListNode odd = head;
        ListNode even = head.next;
        ListNode evenHead = even; // Save the start of the even list

        // Traverse and rewire the nodes
        while (even != null && even.next != null) {
            // Link odd to the next odd node
            odd.next = even.next;
            // Move odd pointer forward
            odd = odd.next;
            
            // Link even to the next even node
            even.next = odd.next;
            // Move even pointer forward
            even = even.next;
        }

        // Attach the even list to the end of the odd list
        odd.next = evenHead;

        return head;
    }


    /**
     * APPROACH 2: Using Extra Lists (Sub-optimal Space)
     * Time Complexity: O(N)
     * Space Complexity: O(N) due to storing nodes in auxiliary lists.
     * 
     * EXPLANATION:
     * Note: This approach violates the O(1) space constraint of the problem, 
     * but it is provided here to demonstrate a simpler, alternative thought process.
     * 
     * 1. Traverse the linked list while keeping track of an index counter.
     * 2. If the index is odd, add the node to an `oddList`.
     * 3. If the index is even, add the node to an `evenList`.
     * 4. After processing all nodes, iterate through the `oddList` and link them together.
     * 5. Connect the last node of the `oddList` to the first node of the `evenList`.
     * 6. Iterate through the `evenList` and link them together, ensuring the last 
     *    node points to null.
     */
    public ListNode oddEvenListUsingExtraSpace(ListNode head) {
        if (head == null) return null;

        List<ListNode> odds = new ArrayList<>();
        List<ListNode> evens = new ArrayList<>();

        ListNode curr = head;
        int index = 1;

        // Separate nodes into two lists based on index
        while (curr != null) {
            if (index % 2 != 0) {
                odds.add(curr);
            } else {
                evens.add(curr);
            }
            curr = curr.next;
            index++;
        }

        // Rebuild the linked list
        ListNode newHead = odds.get(0);
        curr = newHead;

        // Link odd nodes
        for (int i = 1; i < odds.size(); i++) {
            curr.next = odds.get(i);
            curr = curr.next;
        }

        // Link even nodes
        for (int i = 0; i < evens.size(); i++) {
            curr.next = evens.get(i);
            curr = curr.next;
        }

        // Terminate the list
        curr.next = null;

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
        OddEvenLinkedListSolutions solver = new OddEvenLinkedListSolutions();

        // Test Case 1: Optimal Approach
        ListNode head1 = createList(new int[]{1, 2, 3, 4, 5});
        System.out.println("Original List 1: [1, 2, 3, 4, 5]");
        ListNode result1 = solver.oddEvenListOptimal(head1);
        System.out.print("Optimal Output:  ");
        printList(result1);
        System.out.println("-------------------------------------------------");

        // Test Case 2: Optimal Approach with more complex numbers
        ListNode head2 = createList(new int[]{2, 1, 3, 5, 6, 4, 7});
        System.out.println("Original List 2: [2, 1, 3, 5, 6, 4, 7]");
        ListNode result2 = solver.oddEvenListOptimal(head2);
        System.out.print("Optimal Output:  ");
        printList(result2);
        System.out.println("-------------------------------------------------");

        // Test Case 3: Using Extra Space (Sub-optimal approach)
        ListNode head3 = createList(new int[]{10, 20, 30, 40});
        System.out.println("Original List 3: [10, 20, 30, 40]");
        ListNode result3 = solver.oddEvenListUsingExtraSpace(head3);
        System.out.print("Extra Sp Output: ");
        printList(result3);
    }
}
