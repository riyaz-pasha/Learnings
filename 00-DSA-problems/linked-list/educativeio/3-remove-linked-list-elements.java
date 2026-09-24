class Solution {

    static class ListNode {
        int val;
        ListNode next;

        ListNode(int val) {
            this.val = val;
        }
    }

    public ListNode removeElements(ListNode head, int k) {
        ListNode dummy = new ListNode(0); // new list
        ListNode tail = dummy;

        while (head != null) {
            if (head.val != k) {
                tail.next = new ListNode(head.val); // create new node
                tail = tail.next;
            }
            head = head.next;
        }

        return dummy.next;
    }
}

class Solution2 {

    static class ListNode {
        int val;
        ListNode next;

        ListNode(int val) {
            this.val = val;
        }
    }

    public ListNode removeElements(ListNode head, int k) {

        // Dummy node helps handle edge cases like deleting head
        ListNode dummy = new ListNode(0);
        dummy.next = head;

        ListNode prev = dummy;

        while (prev.next != null) {

            if (prev.next.val == k) {
                // Skip the node
                prev.next = prev.next.next;
            } else {
                // Move forward only when no deletion
                prev = prev.next;
            }
        }

        return dummy.next;
    }
}

class Solution3 {

    static class ListNode {
        int val;
        ListNode next;

        ListNode(int val) {
            this.val = val;
        }
    }

    public ListNode removeElements(ListNode head, int k) {
        if (head == null) return null;

        // Process rest of list first
        head.next = removeElements(head.next, k);

        // Decide whether to keep current node
        if (head.val == k) {
            return head.next; // skip current
        }

        return head;
    }
}
