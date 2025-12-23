// Definition for singly-linked list node
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { 
        this.val = val; 
        this.next = null;
    }
}

class MiddleNodeDeleter {
    
    // Approach 1: Two Pointers (Fast & Slow) - OPTIMAL
    // Time: O(n), Space: O(1)
    public ListNode deleteMiddle(ListNode head) {
        // Edge case: single node
        if (head == null || head.next == null) {
            return null;
        }
        
        // Use slow and fast pointers
        // We need prev pointer to delete middle node
        ListNode slow = head;
        ListNode fast = head;
        ListNode prev = null;
        
        // Move fast 2 steps, slow 1 step
        // When fast reaches end, slow is at middle
        while (fast != null && fast.next != null) {
            prev = slow;
            slow = slow.next;
            fast = fast.next.next;
        }
        
        // Delete middle node (slow is at middle)
        if (prev != null) {
            prev.next = slow.next;
        }
        
        return head;
    }
    
    // Approach 2: Count then Delete
    // Time: O(n), Space: O(1)
    public ListNode deleteMiddleByCount(ListNode head) {
        // Edge case: single node
        if (head == null || head.next == null) {
            return null;
        }
        
        // First pass: count nodes
        int count = 0;
        ListNode current = head;
        while (current != null) {
            count++;
            current = current.next;
        }
        
        // Find middle index
        int middleIndex = count / 2;
        
        // Second pass: delete middle node
        current = head;
        for (int i = 0; i < middleIndex - 1; i++) {
            current = current.next;
        }
        
        // Delete the middle node
        current.next = current.next.next;
        
        return head;
    }
    
    // Approach 3: Using ArrayList
    // Time: O(n), Space: O(n)
    public ListNode deleteMiddleUsingList(ListNode head) {
        if (head == null || head.next == null) {
            return null;
        }
        
        // Store all nodes in list
        java.util.ArrayList<ListNode> nodes = new java.util.ArrayList<>();
        ListNode current = head;
        while (current != null) {
            nodes.add(current);
            current = current.next;
        }
        
        // Find middle index
        int middleIndex = nodes.size() / 2;
        
        // Delete middle node by relinking
        if (middleIndex > 0) {
            nodes.get(middleIndex - 1).next = nodes.get(middleIndex).next;
        }
        
        return head;
    }
}

// Test helper class
class MiddleNodeDeleterTester {
    
    // Helper method to create linked list from array
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
    
    // Helper method to print list
    public static void printList(ListNode head) {
        System.out.print("[");
        ListNode current = head;
        while (current != null) {
            System.out.print(current.val);
            if (current.next != null) {
                System.out.print(",");
            }
            current = current.next;
        }
        System.out.print("]");
    }
    
    // Helper to print with indices
    public static void printListWithIndices(int[] values) {
        System.out.print("Values:  [");
        for (int i = 0; i < values.length; i++) {
            System.out.print(values[i]);
            if (i < values.length - 1) System.out.print(",");
        }
        System.out.println("]");
        
        System.out.print("Indices: [");
        for (int i = 0; i < values.length; i++) {
            System.out.print(i);
            if (i < values.length - 1) System.out.print(",");
        }
        System.out.println("]");
        
        int middleIndex = values.length / 2;
        System.out.println("Middle index: " + middleIndex + " (value = " + values[middleIndex] + ")");
    }
    
    public static void main(String[] args) {
        MiddleNodeDeleter deleter = new MiddleNodeDeleter();
        
        System.out.println("=== Delete Middle Node of Linked List ===\n");
        
        // Example 1: [1,3,4,7,1,2,6]
        System.out.println("Example 1:");
        int[] values1 = {1, 3, 4, 7, 1, 2, 6};
        printListWithIndices(values1);
        System.out.print("\nInput:  head = ");
        printList(createList(values1));
        System.out.println();
        ListNode head1 = createList(values1);
        ListNode result1 = deleter.deleteMiddle(head1);
        System.out.print("Output: ");
        printList(result1);
        System.out.println();
        System.out.println("Explanation: n = 7, middle node at index 3 (value 7) is removed.\n");
        
        // Example 2: [1,2,3,4]
        System.out.println("Example 2:");
        int[] values2 = {1, 2, 3, 4};
        printListWithIndices(values2);
        System.out.print("\nInput:  head = ");
        printList(createList(values2));
        System.out.println();
        ListNode head2 = createList(values2);
        ListNode result2 = deleter.deleteMiddle(head2);
        System.out.print("Output: ");
        printList(result2);
        System.out.println();
        System.out.println("Explanation: n = 4, middle node at index 2 (value 3) is removed.\n");
        
        // Example 3: [2,1]
        System.out.println("Example 3:");
        int[] values3 = {2, 1};
        printListWithIndices(values3);
        System.out.print("\nInput:  head = ");
        printList(createList(values3));
        System.out.println();
        ListNode head3 = createList(values3);
        ListNode result3 = deleter.deleteMiddle(head3);
        System.out.print("Output: ");
        printList(result3);
        System.out.println();
        System.out.println("Explanation: n = 2, middle node at index 1 (value 1) is removed.\n");
        
        System.out.println("=== Additional Test Cases ===\n");
        
        // Test 4: Single node
        System.out.println("Test 4: Single node [1]");
        int[] values4 = {1};
        System.out.print("Input:  ");
        printList(createList(values4));
        System.out.println();
        ListNode head4 = createList(values4);
        ListNode result4 = deleter.deleteMiddle(head4);
        System.out.print("Output: ");
        printList(result4);
        System.out.println(" (empty list)\n");
        
        // Test 5: Three nodes
        System.out.println("Test 5: Three nodes [1,2,3]");
        int[] values5 = {1, 2, 3};
        printListWithIndices(values5);
        System.out.print("\nInput:  ");
        printList(createList(values5));
        System.out.println();
        ListNode head5 = createList(values5);
        ListNode result5 = deleter.deleteMiddle(head5);
        System.out.print("Output: ");
        printList(result5);
        System.out.println("\n");
        
        // Test 6: Five nodes
        System.out.println("Test 6: Five nodes [1,2,3,4,5]");
        int[] values6 = {1, 2, 3, 4, 5};
        printListWithIndices(values6);
        System.out.print("\nInput:  ");
        printList(createList(values6));
        System.out.println();
        ListNode head6 = createList(values6);
        ListNode result6 = deleter.deleteMiddle(head6);
        System.out.print("Output: ");
        printList(result6);
        System.out.println("\n");
        
        System.out.println("=== Middle Index Calculation ===\n");
        System.out.println("Formula: middleIndex = ⌊n / 2⌋ (using 0-based indexing)\n");
        System.out.println("n = 1: middleIndex = 0");
        System.out.println("n = 2: middleIndex = 1");
        System.out.println("n = 3: middleIndex = 1");
        System.out.println("n = 4: middleIndex = 2");
        System.out.println("n = 5: middleIndex = 2");
        System.out.println("n = 6: middleIndex = 3");
        System.out.println("n = 7: middleIndex = 3");
        
        System.out.println("\n=== Algorithm Explanation ===\n");
        System.out.println("Optimal Approach (Two Pointers - Fast & Slow):");
        System.out.println();
        System.out.println("Step 1: Initialize pointers");
        System.out.println("  - slow = head");
        System.out.println("  - fast = head");
        System.out.println("  - prev = null (to track node before middle)");
        System.out.println();
        System.out.println("Step 2: Move pointers");
        System.out.println("  - fast moves 2 steps at a time");
        System.out.println("  - slow moves 1 step at a time");
        System.out.println("  - prev follows slow");
        System.out.println("  - When fast reaches end, slow is at middle");
        System.out.println();
        System.out.println("Step 3: Delete middle node");
        System.out.println("  - prev.next = slow.next");
        System.out.println("  - This removes slow (middle node) from chain");
        System.out.println();
        System.out.println("Visual Example: [1,3,4,7,1,2,6]");
        System.out.println();
        System.out.println("Initial:     1 -> 3 -> 4 -> 7 -> 1 -> 2 -> 6");
        System.out.println("             ↑                   ↑");
        System.out.println("          slow,fast");
        System.out.println();
        System.out.println("Step 1:      1 -> 3 -> 4 -> 7 -> 1 -> 2 -> 6");
        System.out.println("                  ↑         ↑");
        System.out.println("                slow      fast");
        System.out.println();
        System.out.println("Step 2:      1 -> 3 -> 4 -> 7 -> 1 -> 2 -> 6");
        System.out.println("                       ↑              ↑");
        System.out.println("                     slow          fast");
        System.out.println();
        System.out.println("Step 3:      1 -> 3 -> 4 -> 7 -> 1 -> 2 -> 6");
        System.out.println("                            ↑              ↑");
        System.out.println("                          slow          fast (null)");
        System.out.println();
        System.out.println("Delete:      1 -> 3 -> 4 -----> 1 -> 2 -> 6");
        System.out.println("                       ↑         ↑");
        System.out.println("                     prev      slow");
        System.out.println();
        System.out.println("Result:      1 -> 3 -> 4 -> 1 -> 2 -> 6");
        System.out.println();
        System.out.println("Time Complexity: O(n) - single pass");
        System.out.println("Space Complexity: O(1) - only pointers");
    }
}
