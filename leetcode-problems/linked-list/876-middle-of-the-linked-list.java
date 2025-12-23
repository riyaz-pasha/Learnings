// Definition for singly-linked list
class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}

class MiddleOfTheLinkedListSolution {
    // Approach 1: Two-Pass Solution (Count then Find)
    public ListNode middleNodeTwoPass(ListNode head) {
        // First pass: count the nodes
        int count = 0;
        ListNode current = head;
        while (current != null) {
            count++;
            current = current.next;
        }
        
        // Second pass: go to the middle
        int middle = count / 2;
        current = head;
        for (int i = 0; i < middle; i++) {
            current = current.next;
        }
        
        return current;
    }
    
    // Approach 2: Two Pointers (Fast & Slow) - OPTIMAL
    public ListNode middleNode(ListNode head) {
        ListNode slow = head;
        ListNode fast = head;
        
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        
        return slow;
    }
    
    // Helper method to print list from a node
    public static void printList(ListNode node) {
        System.out.print("[");
        while (node != null) {
            System.out.print(node.val);
            if (node.next != null) {
                System.out.print(",");
            }
            node = node.next;
        }
        System.out.println("]");
    }
    
    // Helper method to create list from array
    public static ListNode createList(int[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        
        ListNode head = new ListNode(values[0]);
        ListNode current = head;
        
        for (int i = 1; i < values.length; i++) {
            current.next = new ListNode(values[i]);
            current = current.next;
        }
        
        return head;
    }
    
    public static void main(String[] args) {
        MiddleOfTheLinkedListSolution solution = new MiddleOfTheLinkedListSolution();
        
        System.out.println("Example 1:");
        System.out.println("Input: head = [1,2,3,4,5]");
        ListNode head1 = createList(new int[]{1, 2, 3, 4, 5});
        ListNode result1 = solution.middleNode(head1);
        System.out.print("Output: ");
        printList(result1);
        System.out.println("Explanation: The middle node of the list is node 3.\n");
        
        System.out.println("Example 2:");
        System.out.println("Input: head = [1,2,3,4,5,6]");
        ListNode head2 = createList(new int[]{1, 2, 3, 4, 5, 6});
        ListNode result2 = solution.middleNode(head2);
        System.out.print("Output: ");
        printList(result2);
        System.out.println("Explanation: Since the list has two middle nodes with values 3 and 4, we return the second one.\n");
        
        System.out.println("Additional Test Cases:");
        
        // Test case 3: Single node
        System.out.println("Test 3: Single node [1]");
        ListNode head3 = createList(new int[]{1});
        ListNode result3 = solution.middleNode(head3);
        System.out.print("Output: ");
        printList(result3);
        
        // Test case 4: Two nodes
        System.out.println("\nTest 4: Two nodes [1,2]");
        ListNode head4 = createList(new int[]{1, 2});
        ListNode result4 = solution.middleNode(head4);
        System.out.print("Output: ");
        printList(result4);
    }
}
