// Definition for singly-linked list
class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}

class ReverseLinkedListSolution {
    // Approach 1: Iterative - OPTIMAL
    // Time: O(n), Space: O(1)
    public ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode current = head;
        
        while (current != null) {
            ListNode nextTemp = current.next;  // Save next node
            current.next = prev;                // Reverse the link
            prev = current;                     // Move prev forward
            current = nextTemp;                 // Move current forward
        }
        
        return prev;  // prev is the new head
    }
    
    // Approach 2: Recursive
    // Time: O(n), Space: O(n) due to recursion stack
    public ListNode reverseListRecursive(ListNode head) {
        // Base case: empty list or single node
        if (head == null || head.next == null) {
            return head;
        }
        
        // Recursively reverse the rest of the list
        ListNode newHead = reverseListRecursive(head.next);
        
        // Reverse the link
        head.next.next = head;
        head.next = null;
        
        return newHead;
    }
    
    // Approach 3: Using Stack
    // Time: O(n), Space: O(n)
    public ListNode reverseListStack(ListNode head) {
        if (head == null) {
            return null;
        }
        
        java.util.Stack<ListNode> stack = new java.util.Stack<>();
        ListNode current = head;
        
        // Push all nodes onto stack
        while (current != null) {
            stack.push(current);
            current = current.next;
        }
        
        // Pop from stack to create reversed list
        ListNode newHead = stack.pop();
        current = newHead;
        
        while (!stack.isEmpty()) {
            current.next = stack.pop();
            current = current.next;
        }
        current.next = null;
        
        return newHead;
    }
    
    // Helper method to print list
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
        ReverseLinkedListSolution solution = new ReverseLinkedListSolution();
        
        System.out.println("=== Reverse Linked List Examples ===\n");
        
        // Example 1
        System.out.println("Example 1:");
        System.out.print("Input:  head = ");
        ListNode head1 = createList(new int[]{1, 2, 3, 4, 5});
        printList(head1);
        ListNode result1 = solution.reverseList(head1);
        System.out.print("Output: ");
        printList(result1);
        System.out.println();
        
        // Example 2
        System.out.println("Example 2:");
        System.out.print("Input:  head = ");
        ListNode head2 = createList(new int[]{1, 2});
        printList(head2);
        ListNode result2 = solution.reverseList(head2);
        System.out.print("Output: ");
        printList(result2);
        System.out.println();
        
        // Example 3
        System.out.println("Example 3:");
        System.out.print("Input:  head = ");
        ListNode head3 = createList(new int[]{});
        printList(head3);
        ListNode result3 = solution.reverseList(head3);
        System.out.print("Output: ");
        printList(result3);
        System.out.println();
        
        System.out.println("=== Testing Recursive Approach ===\n");
        
        System.out.println("Recursive Test:");
        System.out.print("Input:  head = ");
        ListNode head4 = createList(new int[]{1, 2, 3, 4, 5});
        printList(head4);
        ListNode result4 = solution.reverseListRecursive(head4);
        System.out.print("Output: ");
        printList(result4);
        System.out.println();
        
        System.out.println("=== Additional Test Cases ===\n");
        
        // Single node
        System.out.println("Single node test:");
        System.out.print("Input:  head = ");
        ListNode head5 = createList(new int[]{1});
        printList(head5);
        ListNode result5 = solution.reverseList(head5);
        System.out.print("Output: ");
        printList(result5);
    }
}
