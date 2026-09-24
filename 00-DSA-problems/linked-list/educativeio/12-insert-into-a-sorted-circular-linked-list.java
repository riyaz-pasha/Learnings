/**
 * PROBLEM DESCRIPTION:
 * You are given a reference to a node, head, in a circular linked list, where 
 * the values are sorted in non-decreasing order. The list is circular, so the 
 * last node points to the first node. However, the head can be any node in 
 * the list - it is not guaranteed to be the node with the smallest value.
 * 
 * Your task is to insert a new value, insertVal, into the list so that it 
 * remains sorted and circular after the insertion.
 * 
 * - If the list is empty (head is null), create a new circular list with a 
 *   single node containing insertVal, and return that node.
 * - Otherwise, return the original head node after insertion.
 * 
 * CONSTRAINTS:
 * - The number of nodes in the list is in the range [0, 10^3].
 * - -10^3 <= Node.val, insertVal <= 10^3
 * 
 * EXAMPLES:
 * Example 1:
 * Input: head = [3,4,1], insertVal = 2
 * Output: [3,4,1,2]
 * Visual:
 * Original:
 *      -> 3 -> 4 -> 1 -
 *     |                |
 *      ----------------
 * Insert 2: It belongs between 1 and 3.
 * Result:
 *      -> 3 -> 4 -> 1 -> 2 -
 *     |                     |
 *      ---------------------
 * 
 * Example 2:
 * Input: head = [], insertVal = 1
 * Output: [1] (Circular)
 * 
 * Example 3:
 * Input: head = [1], insertVal = 0
 * Output: [1,0] (Circular)
 */

class Node {
    public int val;
    public Node next;

    public Node() {}

    public Node(int _val) {
        val = _val;
    }

    public Node(int _val, Node _next) {
        val = _val;
        next = _next;
    }
}

public class InsertInSortedCircularList {

    /**
     * APPROACH 1: Optimal One-Pass (Two Pointers)
     * Time Complexity: O(N) where N is the number of nodes in the list.
     * Space Complexity: O(1) extra space.
     * 
     * EXPLANATION:
     * We traverse the list using two pointers, `prev` and `curr`. We need to 
     * find the correct insertion point. There are exactly 3 scenarios where 
     * we can insert the new node between `prev` and `curr`:
     * 
     * Case 1: Normal Sorted Position
     *         prev.val <= insertVal <= curr.val
     *         (e.g., inserting 2 between 1 and 3)
     * 
     * Case 2: Boundary (Turning Point)
     *         We are at the end of the list (max value) transitioning to the 
     *         start of the list (min value). This happens when prev.val > curr.val.
     *         In this case, we insert if the new value is either:
     *         - Greater than or equal to the max (insertVal >= prev.val)
     *         - Less than or equal to the min (insertVal <= curr.val)
     * 
     * Case 3: Uniform Values / Traversed entire list
     *         If we loop entirely back to the starting `head` without triggering 
     *         Case 1 or Case 2 (e.g., all nodes have the same value like 3->3->3, 
     *         and we want to insert 4), we just insert it anywhere.
     */
    public Node insertOptimal(Node head, int insertVal) {
        // Edge Case: Empty List
        if (head == null) {
            Node newNode = new Node(insertVal);
            newNode.next = newNode; // Point to itself to make it circular
            return newNode;
        }

        Node prev = head;
        Node curr = head.next;
        boolean inserted = false;

        // Traverse the circular list
        do {
            // Case 1: Normal insertion within a sorted segment
            if (prev.val <= insertVal && insertVal <= curr.val) {
                inserted = true;
            } 
            // Case 2: Insertion at the turning point (max to min jump)
            else if (prev.val > curr.val) {
                if (insertVal >= prev.val || insertVal <= curr.val) {
                    inserted = true;
                }
            }

            // Perform the insertion if a condition was met
            if (inserted) {
                prev.next = new Node(insertVal, curr);
                return head;
            }

            // Move pointers forward
            prev = curr;
            curr = curr.next;

        } while (prev != head);

        // Case 3: All values are uniform, or we made a full loop.
        // Just insert it after the last visited node (which is now prev)
        prev.next = new Node(insertVal, curr);
        
        return head;
    }


    /**
     * APPROACH 2: Find the Maximum Node First (Two Passes)
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Instead of checking for all conditions simultaneously, we can break it down:
     * 1. Iterate through the list to find the "turning point" (the maximum node 
     *    which points to the minimum node). Let's call it `maxNode`.
     * 2. Once we know where the list "starts" and "ends" logically, we can check:
     *    - If `insertVal` is >= `maxNode.val` or <= `maxNode.next.val`, we insert 
     *      it right after `maxNode`.
     *    - Otherwise, we start a standard traversal from the minimum node 
     *      (`maxNode.next`) and insert it in its normal sorted position.
     * 
     * This approach separates the boundary logic from the standard sorted logic, 
     * which some people find easier to read, though it might traverse the list 
     * up to two times.
     */
    public Node insertFindMaxFirst(Node head, int insertVal) {
        if (head == null) {
            Node newNode = new Node(insertVal);
            newNode.next = newNode;
            return newNode;
        }

        // Step 1: Find the max node (turning point)
        Node curr = head;
        Node maxNode = head;
        do {
            if (curr.val > curr.next.val) {
                maxNode = curr;
                break; // Found the turning point
            }
            curr = curr.next;
        } while (curr != head);

        // maxNode.next is logically the "head" (minimum element) of the sorted sequence
        Node minNode = maxNode.next;

        // Step 2: Check if it belongs at the boundary
        if (insertVal >= maxNode.val || insertVal <= minNode.val || maxNode.val == minNode.val) {
            maxNode.next = new Node(insertVal, minNode);
            return head;
        }

        // Step 3: Otherwise, find its normal sorted position
        Node prev = minNode;
        curr = minNode.next;
        while (true) {
            if (prev.val <= insertVal && insertVal <= curr.val) {
                prev.next = new Node(insertVal, curr);
                return head;
            }
            prev = curr;
            curr = curr.next;
        }
    }


    /**
     * UTILITY: Create Circular List from Array
     */
    public static Node createCircularList(int[] arr) {
        if (arr == null || arr.length == 0) return null;
        Node head = new Node(arr[0]);
        Node curr = head;
        for (int i = 1; i < arr.length; i++) {
            curr.next = new Node(arr[i]);
            curr = curr.next;
        }
        curr.next = head; // Make it circular
        return head;
    }

    /**
     * UTILITY: Print Circular List
     * Prints exactly one full cycle of the circular list.
     */
    public static void printCircularList(Node head) {
        if (head == null) {
            System.out.println("[]");
            return;
        }
        Node curr = head;
        System.out.print("[");
        do {
            System.out.print(curr.val + (curr.next != head ? ", " : "]\n"));
            curr = curr.next;
        } while (curr != head);
    }


    /**
     * MAIN METHOD: Demonstrating the approaches with examples.
     */
    public static void main(String[] args) {
        InsertInSortedCircularList solver = new InsertInSortedCircularList();

        // Test Case 1: Normal insertion (middle)
        Node head1 = createCircularList(new int[]{3, 4, 1});
        System.out.println("Original Circular List 1: [3, 4, 1] (Insert 2)");
        Node result1 = solver.insertOptimal(head1, 2);
        System.out.print("Optimal Output:           ");
        printCircularList(result1);
        System.out.println("-------------------------------------------------");

        // Test Case 2: Boundary insertion (greater than max)
        Node head2 = createCircularList(new int[]{3, 4, 1});
        System.out.println("Original Circular List 2: [3, 4, 1] (Insert 5)");
        Node result2 = solver.insertOptimal(head2, 5);
        System.out.print("Optimal Output:           ");
        printCircularList(result2);
        System.out.println("-------------------------------------------------");
        
        // Test Case 3: Boundary insertion (smaller than min)
        Node head3 = createCircularList(new int[]{3, 4, 1});
        System.out.println("Original Circular List 3: [3, 4, 1] (Insert 0)");
        Node result3 = solver.insertFindMaxFirst(head3, 0);
        System.out.print("FindMax Output:           ");
        printCircularList(result3);
        System.out.println("-------------------------------------------------");

        // Test Case 4: Uniform values
        Node head4 = createCircularList(new int[]{3, 3, 3});
        System.out.println("Original Circular List 4: [3, 3, 3] (Insert 4)");
        Node result4 = solver.insertOptimal(head4, 4);
        System.out.print("Optimal Output:           ");
        printCircularList(result4);
        System.out.println("-------------------------------------------------");

        // Test Case 5: Empty List
        Node head5 = null;
        System.out.println("Original Circular List 5: [] (Insert 1)");
        Node result5 = solver.insertOptimal(head5, 1);
        System.out.print("Optimal Output:           ");
        printCircularList(result5);
    }
}
