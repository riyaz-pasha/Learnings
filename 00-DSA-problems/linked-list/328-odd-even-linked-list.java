// Definition for singly-linked list node
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { 
        this.val = val; 
        this.next = null;
    }
}

class OddEvenListReorganizer {
    
    // Approach: Two Pointers - OPTIMAL
    // Time: O(n), Space: O(1)
    public ListNode oddEvenList(ListNode head) {
        // Edge cases
        if (head == null || head.next == null) {
            return head;
        }
        
        // Initialize pointers
        ListNode odd = head;              // Start at 1st node (index 1 - odd)
        ListNode even = head.next;        // Start at 2nd node (index 2 - even)
        ListNode evenHead = even;         // Save head of even list
        
        // Reorganize the list
        while (even != null && even.next != null) {
            // Link odd node to next odd node
            odd.next = even.next;
            odd = odd.next;
            
            // Link even node to next even node
            even.next = odd.next;
            even = even.next;
        }
        
        // Connect odd list with even list
        odd.next = evenHead;
        
        return head;
    }
    
    // Alternative approach with more explicit tracking
    public ListNode oddEvenListVerbose(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        
        ListNode oddPointer = head;
        ListNode evenPointer = head.next;
        ListNode evenHead = evenPointer;
        
        // Process pairs of nodes
        while (evenPointer != null && evenPointer.next != null) {
            // Current odd node points to next odd node (skip even)
            oddPointer.next = oddPointer.next.next;
            oddPointer = oddPointer.next;
            
            // Current even node points to next even node (skip odd)
            evenPointer.next = evenPointer.next.next;
            evenPointer = evenPointer.next;
        }
        
        // Attach even list to the end of odd list
        oddPointer.next = evenHead;
        
        return head;
    }
}

// Test helper class
class OddEvenListTester {
    
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
    
    // Helper method to convert list to array for easy verification
    public static int[] listToArray(ListNode head) {
        java.util.ArrayList<Integer> result = new java.util.ArrayList<>();
        ListNode current = head;
        while (current != null) {
            result.add(current.val);
            current = current.next;
        }
        return result.stream().mapToInt(i -> i).toArray();
    }
    
    public static void main(String[] args) {
        OddEvenListReorganizer reorganizer = new OddEvenListReorganizer();
        
        System.out.println("=== Odd Even Linked List Reorganization ===\n");
        
        // Example 1: [1,2,3,4,5]
        System.out.println("Example 1:");
        int[] values1 = {1, 2, 3, 4, 5};
        System.out.print("Input:  head = ");
        printList(createList(values1));
        System.out.println();
        ListNode head1 = createList(values1);
        ListNode result1 = reorganizer.oddEvenList(head1);
        System.out.print("Output: ");
        printList(result1);
        System.out.println();
        System.out.println("Explanation: Odd indices [1,3,5] followed by even indices [2,4]\n");
        
        // Example 2: [2,1,3,5,6,4,7]
        System.out.println("Example 2:");
        int[] values2 = {2, 1, 3, 5, 6, 4, 7};
        System.out.print("Input:  head = ");
        printList(createList(values2));
        System.out.println();
        ListNode head2 = createList(values2);
        ListNode result2 = reorganizer.oddEvenList(head2);
        System.out.print("Output: ");
        printList(result2);
        System.out.println();
        System.out.println("Explanation: Odd indices [2,3,6,7] followed by even indices [1,5,4]\n");
        
        System.out.println("=== Additional Test Cases ===\n");
        
        // Test 3: Single element
        System.out.println("Test 3: Single element [1]");
        int[] values3 = {1};
        System.out.print("Input:  ");
        printList(createList(values3));
        System.out.println();
        ListNode head3 = createList(values3);
        ListNode result3 = reorganizer.oddEvenList(head3);
        System.out.print("Output: ");
        printList(result3);
        System.out.println("\n");
        
        // Test 4: Two elements
        System.out.println("Test 4: Two elements [1,2]");
        int[] values4 = {1, 2};
        System.out.print("Input:  ");
        printList(createList(values4));
        System.out.println();
        ListNode head4 = createList(values4);
        ListNode result4 = reorganizer.oddEvenList(head4);
        System.out.print("Output: ");
        printList(result4);
        System.out.println("\n");
        
        // Test 5: Even number of nodes
        System.out.println("Test 5: Even number of nodes [1,2,3,4,5,6]");
        int[] values5 = {1, 2, 3, 4, 5, 6};
        System.out.print("Input:  ");
        printList(createList(values5));
        System.out.println();
        ListNode head5 = createList(values5);
        ListNode result5 = reorganizer.oddEvenList(head5);
        System.out.print("Output: ");
        printList(result5);
        System.out.println();
        System.out.println("Expected: [1,3,5,2,4,6]\n");
        
        // Test 6: Three elements
        System.out.println("Test 6: Three elements [1,2,3]");
        int[] values6 = {1, 2, 3};
        System.out.print("Input:  ");
        printList(createList(values6));
        System.out.println();
        ListNode head6 = createList(values6);
        ListNode result6 = reorganizer.oddEvenList(head6);
        System.out.print("Output: ");
        printList(result6);
        System.out.println();
        System.out.println("Expected: [1,3,2]\n");
        
        System.out.println("=== Algorithm Explanation ===\n");
        System.out.println("Two Pointer Approach:");
        System.out.println();
        System.out.println("Step 1: Initialize pointers");
        System.out.println("  - odd = head (1st node)");
        System.out.println("  - even = head.next (2nd node)");
        System.out.println("  - evenHead = even (save even list start)");
        System.out.println();
        System.out.println("Step 2: Reorganize nodes");
        System.out.println("  - Link each odd node to the next odd node");
        System.out.println("  - Link each even node to the next even node");
        System.out.println("  - This separates odd and even indexed nodes");
        System.out.println();
        System.out.println("Step 3: Connect lists");
        System.out.println("  - Attach even list to end of odd list");
        System.out.println();
        System.out.println("Visual Example: [1,2,3,4,5]");
        System.out.println();
        System.out.println("Initial:     1 -> 2 -> 3 -> 4 -> 5");
        System.out.println("             ^    ^");
        System.out.println("            odd  even");
        System.out.println();
        System.out.println("Iteration 1: 1 -----> 3    2 -----> 4");
        System.out.println("                  ^             ^");
        System.out.println("                 odd           even");
        System.out.println();
        System.out.println("Iteration 2: 1 -> 3 -> 5    2 -> 4");
        System.out.println("                      ^           ^");
        System.out.println("                     odd         even");
        System.out.println();
        System.out.println("Connect:     1 -> 3 -> 5 -> 2 -> 4");
        System.out.println();
        System.out.println("Time Complexity: O(n) - single pass through list");
        System.out.println("Space Complexity: O(1) - only pointer manipulation");
        System.out.println();
        System.out.println("Key Points:");
        System.out.println("  - Nodes with odd indices: 1st, 3rd, 5th, etc.");
        System.out.println("  - Nodes with even indices: 2nd, 4th, 6th, etc.");
        System.out.println("  - Relative order preserved within each group");
        System.out.println("  - No extra space used (only relink pointers)");
    }
}
