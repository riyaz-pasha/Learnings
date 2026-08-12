/**
 * Definition for singly-linked list.
 */
class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
        this.next = null;
    }
}

public class RemoveNodesPattern {

    /**
     * Removes nodes in pattern:
     * Keep m nodes, delete next n nodes, repeat.
     */
    public static ListNode deleteNodes(ListNode head, int m, int n) {

        // Edge case
        if (head == null) return null;

        ListNode current = head;

        while (current != null) {

            // 🔹 Step 1: Skip m-1 nodes (stay at m-th node)
            for (int i = 1; i < m && current != null; i++) {
                current = current.next;
            }

            // If we reached end, break
            if (current == null) break;

            // 🔹 Step 2: Delete next n nodes
            ListNode temp = current.next;

            for (int i = 0; i < n && temp != null; i++) {
                temp = temp.next;
            }

            // 🔹 Step 3: Connect m-th node to (m+n+1)-th node
            current.next = temp;

            // 🔹 Step 4: Move current to next valid node
            current = temp;
        }

        return head;
    }

    public static ListNode deleteNodesRecursive(ListNode head, int m, int n) {
        if (head == null) return null;

        ListNode current = head;

        // Skip m-1 nodes
        for (int i = 1; i < m && current != null; i++) {
            current = current.next;
        }

        if (current == null) return head;

        // Delete next n nodes
        ListNode temp = current.next;
        for (int i = 0; i < n && temp != null; i++) {
            temp = temp.next;
        }

        // Recursive call for rest
        current.next = deleteNodesRecursive(temp, m, n);

        return head;
    }

}
