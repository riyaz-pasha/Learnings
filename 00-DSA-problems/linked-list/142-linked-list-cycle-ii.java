// Definition for singly-linked list node
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { 
        this.val = val; 
        this.next = null;
    }
}

class CycleStartFinder {
    
    // Approach 1: Floyd's Cycle Detection + Find Start - OPTIMAL
    // Time: O(n), Space: O(1)
    public ListNode detectCycle(ListNode head) {
        if (head == null || head.next == null) {
            return null;
        }
        
        // Phase 1: Detect if cycle exists using Floyd's algorithm
        ListNode slow = head;
        ListNode fast = head;
        boolean hasCycle = false;
        
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            
            if (slow == fast) {
                hasCycle = true;
                break;
            }
        }
        
        if (!hasCycle) {
            return null;
        }
        
        // Phase 2: Find the start of the cycle
        // Move one pointer to head, keep other at meeting point
        // Move both one step at a time - they'll meet at cycle start
        slow = head;
        while (slow != fast) {
            slow = slow.next;
            fast = fast.next;
        }
        
        return slow;  // This is the start of the cycle
    }
    
    // Approach 2: Using HashSet
    // Time: O(n), Space: O(n)
    public ListNode detectCycleUsingHashSet(ListNode head) {
        java.util.HashSet<ListNode> visited = new java.util.HashSet<>();
        ListNode current = head;
        
        while (current != null) {
            if (visited.contains(current)) {
                return current;  // First repeated node is cycle start
            }
            visited.add(current);
            current = current.next;
        }
        
        return null;  // No cycle
    }
    
    // Helper method to get node index (for display purposes)
    private int getNodeIndex(ListNode head, ListNode target) {
        if (target == null) {
            return -1;
        }
        
        ListNode current = head;
        int index = 0;
        
        // Prevent infinite loop - limit iterations
        for (int i = 0; i < 10000 && current != null; i++) {
            if (current == target) {
                return index;
            }
            current = current.next;
            index++;
        }
        
        return -1;
    }
}

// Test helper class
class CycleStartTester {
    
    // Helper method to create a linked list with cycle
    public static ListNode createListWithCycle(int[] values, int pos) {
        if (values == null || values.length == 0) {
            return null;
        }
        
        ListNode head = new ListNode(values[0]);
        ListNode current = head;
        ListNode cycleNode = null;
        
        // Store the node at position 'pos'
        if (pos == 0) {
            cycleNode = head;
        }
        
        // Create the rest of the list
        for (int i = 1; i < values.length; i++) {
            current.next = new ListNode(values[i]);
            current = current.next;
            
            if (i == pos) {
                cycleNode = current;
            }
        }
        
        // Create cycle if pos is valid
        if (pos >= 0 && cycleNode != null) {
            current.next = cycleNode;
        }
        
        return head;
    }
    
    // Helper to get index of a node
    public static int getNodeIndex(ListNode head, ListNode target) {
        if (target == null) {
            return -1;
        }
        
        ListNode current = head;
        int index = 0;
        
        // Limit iterations to prevent infinite loop
        for (int i = 0; i < 10000 && current != null; i++) {
            if (current == target) {
                return index;
            }
            current = current.next;
            index++;
        }
        
        return -1;
    }
    
    // Helper method to print list values (without full traversal if cycle exists)
    public static void printListInfo(int[] values, int pos) {
        System.out.print("[");
        for (int i = 0; i < values.length; i++) {
            System.out.print(values[i]);
            if (i < values.length - 1) {
                System.out.print(",");
            }
        }
        System.out.print("], pos = " + pos);
    }
    
    public static void main(String[] args) {
        CycleStartFinder finder = new CycleStartFinder();
        
        System.out.println("=== Find Cycle Start in Linked List ===\n");
        
        // Example 1
        System.out.println("Example 1:");
        int[] values1 = {3, 2, 0, -4};
        int pos1 = 1;
        System.out.print("Input: head = ");
        printListInfo(values1, pos1);
        System.out.println();
        ListNode head1 = createListWithCycle(values1, pos1);
        ListNode result1 = finder.detectCycle(head1);
        int index1 = getNodeIndex(head1, result1);
        if (result1 != null) {
            System.out.println("Output: tail connects to node index " + index1);
            System.out.println("Explanation: There is a cycle, where tail connects to the second node.\n");
        } else {
            System.out.println("Output: no cycle\n");
        }
        
        // Example 2
        System.out.println("Example 2:");
        int[] values2 = {1, 2};
        int pos2 = 0;
        System.out.print("Input: head = ");
        printListInfo(values2, pos2);
        System.out.println();
        ListNode head2 = createListWithCycle(values2, pos2);
        ListNode result2 = finder.detectCycle(head2);
        int index2 = getNodeIndex(head2, result2);
        if (result2 != null) {
            System.out.println("Output: tail connects to node index " + index2);
            System.out.println("Explanation: There is a cycle, where tail connects to the first node.\n");
        } else {
            System.out.println("Output: no cycle\n");
        }
        
        // Example 3
        System.out.println("Example 3:");
        int[] values3 = {1};
        int pos3 = -1;
        System.out.print("Input: head = ");
        printListInfo(values3, pos3);
        System.out.println();
        ListNode head3 = createListWithCycle(values3, pos3);
        ListNode result3 = finder.detectCycle(head3);
        if (result3 != null) {
            int index3 = getNodeIndex(head3, result3);
            System.out.println("Output: tail connects to node index " + index3 + "\n");
        } else {
            System.out.println("Output: no cycle");
            System.out.println("Explanation: There is no cycle in the linked list.\n");
        }
        
        System.out.println("=== Additional Test Cases ===\n");
        
        // Test 4: Longer list with cycle in middle
        System.out.println("Test 4: Cycle at position 2");
        int[] values4 = {1, 2, 3, 4, 5, 6};
        int pos4 = 2;
        System.out.print("Input: head = ");
        printListInfo(values4, pos4);
        System.out.println();
        ListNode head4 = createListWithCycle(values4, pos4);
        ListNode result4 = finder.detectCycle(head4);
        int index4 = getNodeIndex(head4, result4);
        System.out.println("Output: tail connects to node index " + index4 + "\n");
        
        // Test 5: No cycle
        System.out.println("Test 5: No cycle in list");
        int[] values5 = {1, 2, 3, 4, 5};
        int pos5 = -1;
        System.out.print("Input: head = ");
        printListInfo(values5, pos5);
        System.out.println();
        ListNode head5 = createListWithCycle(values5, pos5);
        ListNode result5 = finder.detectCycle(head5);
        if (result5 != null) {
            int index5 = getNodeIndex(head5, result5);
            System.out.println("Output: tail connects to node index " + index5 + "\n");
        } else {
            System.out.println("Output: no cycle\n");
        }
        
        System.out.println("=== Testing HashSet Approach ===\n");
        
        System.out.println("HashSet Test with cycle:");
        int[] values6 = {3, 2, 0, -4};
        ListNode head6 = createListWithCycle(values6, 1);
        ListNode result6 = finder.detectCycleUsingHashSet(head6);
        int index6 = getNodeIndex(head6, result6);
        System.out.println("Output: tail connects to node index " + index6);
        
        System.out.println("\n=== Algorithm Explanation ===");
        System.out.println("Floyd's Cycle Detection - Two Phases:");
        System.out.println();
        System.out.println("Phase 1: Detect cycle");
        System.out.println("  - Use slow (1 step) and fast (2 steps) pointers");
        System.out.println("  - If they meet, cycle exists");
        System.out.println();
        System.out.println("Phase 2: Find cycle start");
        System.out.println("  - Move slow to head, keep fast at meeting point");
        System.out.println("  - Move both 1 step at a time");
        System.out.println("  - They meet at cycle start!");
        System.out.println();
        System.out.println("Mathematical proof:");
        System.out.println("  - If distance from head to cycle start = x");
        System.out.println("  - And cycle length = c");
        System.out.println("  - When they meet in Phase 1, slow traveled: x + some_distance");
        System.out.println("  - Fast traveled: 2 * (x + some_distance)");
        System.out.println("  - Due to cycle properties, moving slow to head");
        System.out.println("    and advancing both will meet at cycle start");
        System.out.println();
        System.out.println("Time Complexity: O(n)");
        System.out.println("Space Complexity: O(1)");
    }
}
