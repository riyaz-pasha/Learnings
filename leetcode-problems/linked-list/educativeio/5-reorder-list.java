import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * PROBLEM DESCRIPTION:
 * You are given the head of a singly linked-list. The list can be represented as:
 * L0 → L1 → … → Ln - 1 → Ln
 * 
 * Reorder the list to be on the following form:
 * L0 → Ln → L1 → Ln - 1 → L2 → Ln - 2 → …
 * 
 * You may not modify the values in the list's nodes. Only nodes themselves may be changed.
 * 
 * EXAMPLES:
 * Example 1:
 * Input: head = [1,2,3,4]
 * Output: [1,4,2,3]
 * Visual:
 * 1 -> 2 -> 3 -> 4
 * |---------^    |
 *      |---------|
 * 
 * Example 2:
 * Input: head = [1,2,3,4,5]
 * Output: [1,5,2,4,3]
 */

class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
    }
}

public class ReorderListSolutions {

    /**
     * APPROACH 1: Optimal (Slow/Fast Pointers + Reverse + Merge)
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * This is the most efficient approach as it modifies the list in-place 
     * without requiring any extra memory (like Arrays, Stacks, or Deques).
     * 
     * VISUALIZATION:
     * Input: 1 -> 2 -> 3 -> 4 -> 5
     * 
     * Step 1: Find Middle
     * slow       fast
     *  |          |
     *  v          v
     *  1 -> 2 ->  3  -> 4 -> 5
     * 
     * Step 2: Reverse Second Half
     *  1 -> 2 -> 3 -> null
     *            5 -> 4 -> null  (reversed from 4 -> 5)
     * 
     * Step 3: Merge Alternating
     *  first = 1, second = 5
     *  1 -> 5 -> 2 -> 4 -> 3
     */
    public void reorderListOptimal(ListNode head) {
        if (head == null || head.next == null) return;

        // Step 1: Find middle of the list
        ListNode slow = head;
        ListNode fast = head;

        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }

        // Step 2: Reverse second half
        ListNode second = reverse(slow.next);
        slow.next = null; // break the list to avoid cycles

        // Step 3: Merge both halves
        ListNode first = head;

        while (second != null) {
            // Save next nodes
            ListNode temp1 = first.next;
            ListNode temp2 = second.next;

            // Link first to second, and second to first's next
            first.next = second;
            second.next = temp1;

            // Move pointers forward
            first = temp1;
            second = temp2;
        }
    }

    // Helper method for Approach 1
    private ListNode reverse(ListNode head) {
        ListNode prev = null;
        ListNode curr = head;

        while (curr != null) {
            ListNode next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        return prev;
    }


    /**
     * APPROACH 2: Using a Deque (Double-Ended Queue)
     * Time Complexity: O(N)
     * Space Complexity: O(N) for storing node references
     * 
     * EXPLANATION:
     * A Deque allows us to pop elements from the front AND the back. 
     * We load all nodes into the deque, then rebuild the list by taking 
     * one from the front, then one from the back, repeatedly.
     * 
     * VISUALIZATION:
     * Input: 1 -> 2 -> 3 -> 4 -> 5
     * 
     * Deque: [1, 2, 3, 4, 5]
     * 
     * Iteration 1:
     * popFirst() -> 1
     * popLast()  -> 5
     * Link: 1 -> 5
     * Deque remaining: [2, 3, 4]
     * 
     * Iteration 2:
     * popFirst() -> 2
     * popLast()  -> 4
     * Link: 5 -> 2 -> 4
     * Deque remaining: [3]
     * 
     * Iteration 3:
     * popFirst() -> 3
     * Link: 4 -> 3 -> null
     */
    public void reorderListUsingDeque(ListNode head) {
        if (head == null || head.next == null) return;
        
        Deque<ListNode> deque = new ArrayDeque<>();
        ListNode curr = head;
        
        // Populate the deque
        while (curr != null) {
            deque.addLast(curr);
            curr = curr.next;
        }
        
        // Rebuild the list
        curr = head;
        while (!deque.isEmpty()) {
            // Take from front
            ListNode front = deque.pollFirst();
            // Take from back (if available)
            ListNode back = deque.pollLast();
            
            if (back != null) {
                front.next = back;
                back.next = deque.peekFirst(); // Point to the new front
            } else {
                front.next = null; // Odd number of elements, close the list
            }
        }
        
        // Ensure the last node points to null
        if (curr != null) {
            curr.next = null;
        }
    }


    /**
     * APPROACH 3: Using an ArrayList (Two Pointers)
     * Time Complexity: O(N)
     * Space Complexity: O(N) for the list of nodes
     * 
     * EXPLANATION:
     * Similar to the Deque approach, but we store references in a standard 
     * ArrayList and use two integer pointers (left and right) to weave 
     * the nodes together.
     * 
     * VISUALIZATION:
     * Array: [Node(1), Node(2), Node(3), Node(4), Node(5)]
     *         ^L                                ^R
     * 
     * L=0, R=4 -> Array[0].next = Array[4], Array[4].next = Array[1] (Next L)
     * L=1, R=3 -> Array[1].next = Array[3], Array[3].next = Array[2] (Next L)
     * L=2, R=2 -> Stop, set Array[2].next = null.
     */
    public void reorderListUsingArray(ListNode head) {
        if (head == null || head.next == null) return;
        
        List<ListNode> list = new ArrayList<>();
        ListNode curr = head;
        
        // Populate the list
        while (curr != null) {
            list.add(curr);
            curr = curr.next;
        }
        
        int left = 0;
        int right = list.size() - 1;
        
        // Weave them together
        while (left < right) {
            list.get(left).next = list.get(right);
            left++;
            
            if (left == right) {
                // Odd number of elements crossover point
                break;
            }
            
            list.get(right).next = list.get(left);
            right--;
        }
        
        // Terminate the list to prevent cycles
        list.get(left).next = null;
    }

    
    /**
     * UTILITY: Print List
     * Helper method to print the list and verify the results.
     */
    public static void printList(ListNode head) {
        ListNode curr = head;
        while (curr != null) {
            System.out.print(curr.val + (curr.next != null ? " -> " : " -> null\n"));
            curr = curr.next;
        }
    }

    
    /**
     * MAIN METHOD: Demonstrating the approaches.
     */
    public static void main(String[] args) {
        ReorderListSolutions solver = new ReorderListSolutions();
        
        // Test Case 1: Optimal
        ListNode head1 = new ListNode(1);
        head1.next = new ListNode(2);
        head1.next.next = new ListNode(3);
        head1.next.next.next = new ListNode(4);
        
        System.out.println("Original List 1: 1 -> 2 -> 3 -> 4 -> null");
        solver.reorderListOptimal(head1);
        System.out.print("Optimal Output:  ");
        printList(head1);
        System.out.println("-------------------------------------------------");
        
        // Test Case 2: Deque
        ListNode head2 = new ListNode(1);
        head2.next = new ListNode(2);
        head2.next.next = new ListNode(3);
        head2.next.next.next = new ListNode(4);
        head2.next.next.next.next = new ListNode(5);
        
        System.out.println("Original List 2: 1 -> 2 -> 3 -> 4 -> 5 -> null");
        solver.reorderListUsingDeque(head2);
        System.out.print("Deque Output:    ");
        printList(head2);
        System.out.println("-------------------------------------------------");

        // Test Case 3: ArrayList
        ListNode head3 = new ListNode(10);
        head3.next = new ListNode(20);
        head3.next.next = new ListNode(30);
        head3.next.next.next = new ListNode(40);
        head3.next.next.next.next = new ListNode(50);
        head3.next.next.next.next.next = new ListNode(60);
        
        System.out.println("Original List 3: 10 -> 20 -> 30 -> 40 -> 50 -> 60 -> null");
        solver.reorderListUsingArray(head3);
        System.out.print("Array Output:    ");
        printList(head3);
    }
}
