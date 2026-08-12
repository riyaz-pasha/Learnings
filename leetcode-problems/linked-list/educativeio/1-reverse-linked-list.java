import java.util.Stack;

// Definition for singly-linked list.
class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
        this.next = null;
    }
}

public class ReverseLinkedList {

    /*
     * Approach 1: Iterative
     *
     * Thought Process:
     * - Traverse the list once
     * - Reverse the direction of pointers one by one
     *
     * Steps:
     * 1. Maintain 3 pointers:
     *    prev -> previous node
     *    curr -> current node
     *    next -> next node (to not lose reference)
     *
     * 2. Reverse pointer:
     *    curr.next = prev
     *
     * 3. Move forward:
     *    prev = curr
     *    curr = next
     *
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public static ListNode reverseListIterative(ListNode head) {
        ListNode prev = null;
        ListNode curr = head;

        while (curr != null) {
            ListNode next = curr.next; // save next
            curr.next = prev;          // reverse pointer
            prev = curr;               // move prev
            curr = next;               // move curr
        }

        return prev; // new head
    }
}

public class ReverseLinkedListRecursive {

    /*
     * Approach 2: Recursive
     *
     * Thought Process:
     * - Reverse smaller list
     * - Fix current node after recursion
     *
     * Base Case:
     * - If head is null OR only one node → return head
     *
     * Recursive Step:
     * - Reverse rest of list
     * - Fix current node connections
     *
     * Time Complexity: O(n)
     * Space Complexity: O(n) (recursion stack)
     */
    public static ListNode reverseListRecursive(ListNode head) {
        // base case
        if (head == null || head.next == null) {
            return head;
        }

        // reverse rest
        ListNode newHead = reverseListRecursive(head.next);

        // fix current node
        head.next.next = head;
        head.next = null;

        return newHead;
    }
}

public class ReverseLinkedListStack {

    /*
     * Approach 3: Stack
     *
     * Steps:
     * 1. Push all nodes into stack
     * 2. Pop and rebuild list
     *
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public static ListNode reverseListStack(ListNode head) {
        if (head == null) return null;

        Stack<ListNode> stack = new Stack<>();
        ListNode curr = head;

        while (curr != null) {
            stack.push(curr);
            curr = curr.next;
        }

        ListNode newHead = stack.pop();
        curr = newHead;

        while (!stack.isEmpty()) {
            curr.next = stack.pop();
            curr = curr.next;
        }

        curr.next = null;

        return newHead;
    }
}
