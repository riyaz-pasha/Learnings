import java.util.HashSet;
import java.util.Set;

/**
 * Definition for singly-linked list.
 */
class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
    }
}

public class RemoveDuplicatesFromSortedList {

    /**
     * Approach 1: Iterative (Optimal)
     *
     * Thought Process:
     * - Since list is sorted, duplicates are adjacent
     * - Traverse using a pointer
     * - If current.val == current.next.val → skip next node
     *
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public static ListNode deleteDuplicatesIterative(ListNode head) {
        // Edge case: empty or single node
        if (head == null || head.next == null) {
            return head;
        }

        ListNode current = head;

        while (current != null && current.next != null) {
            if (current.val == current.next.val) {
                // Skip duplicate node
                current.next = current.next.next;
            } else {
                // Move forward only if no duplicate
                current = current.next;
            }
        }

        return head;
    }
}

public class RemoveDuplicatesRecursive {

    /**
     * Approach 2: Recursive
     *
     * Thought Process:
     * - Recursively process the next node
     * - If current.val == next.val → skip current
     *
     * Time Complexity: O(n)
     * Space Complexity: O(n) (due to recursion stack)
     */
    public static ListNode deleteDuplicatesRecursive(ListNode head) {
        // Base case
        if (head == null || head.next == null) {
            return head;
        }

        // Recursively process the rest
        head.next = deleteDuplicatesRecursive(head.next);

        // Check duplicate
        if (head.val == head.next.val) {
            return head.next; // skip current
        }

        return head;
    }
}

public class RemoveDuplicatesUsingSet {

    /**
     * Approach 3: Using HashSet
     *
     * Thought Process:
     * - Keep track of seen values
     * - Remove nodes if already seen
     *
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    public static ListNode deleteDuplicatesWithSet(ListNode head) {
        if (head == null) return null;

        Set<Integer> seen = new HashSet<>();

        ListNode current = head;
        ListNode prev = null;

        while (current != null) {
            if (seen.contains(current.val)) {
                // Remove duplicate
                prev.next = current.next;
            } else {
                seen.add(current.val);
                prev = current;
            }
            current = current.next;
        }

        return head;
    }
}

public class RemoveDuplicatesTwoPointer {

    /**
     * Approach 4: Two Pointer Technique
     *
     * Thought Process:
     * - Slow pointer marks last unique node
     * - Fast pointer scans ahead
     *
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public static ListNode deleteDuplicatesTwoPointer(ListNode head) {
        if (head == null) return null;

        ListNode slow = head;
        ListNode fast = head.next;

        while (fast != null) {
            if (slow.val != fast.val) {
                slow.next = fast;
                slow = slow.next;
            }
            fast = fast.next;
        }

        // Important: terminate list
        slow.next = null;

        return head;
    }
}
