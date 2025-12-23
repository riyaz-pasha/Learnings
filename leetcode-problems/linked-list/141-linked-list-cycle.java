import java.util.HashSet;

/*
 * Given head, the head of a linked list, determine if the linked list has a
 * cycle in it.
 * 
 * There is a cycle in a linked list if there is some node in the list that can
 * be reached again by continuously following the next pointer. Internally, pos
 * is used to denote the index of the node that tail's next pointer is connected
 * to. Note that pos is not passed as a parameter.
 * 
 * Return true if there is a cycle in the linked list. Otherwise, return false.
 * 
 * Example 1:
 * Input: head = [3,2,0,-4], pos = 1
 * Output: true
 * Explanation: There is a cycle in the linked list, where the tail connects to
 * the 1st node (0-indexed).
 * 
 * Example 2:
 * Input: head = [1,2], pos = 0
 * Output: true
 * Explanation: There is a cycle in the linked list, where the tail connects to
 * the 0th node.
 * 
 * Example 3:
 * Input: head = [1], pos = -1
 * Output: false
 * Explanation: There is no cycle in the linked list.
 */

 // Definition for singly-linked list
class ListNode {

    int val;
    ListNode next;

    ListNode(int x) {
        val = x;
        next = null;
    }

}

class CycleDetectionSolution {
    
    // Solution 1: Floyd's Cycle Detection Algorithm (Two Pointers/Tortoise and Hare)
    // Time Complexity: O(n), Space Complexity: O(1)
    // Most efficient and elegant solution
    public boolean hasCycle(ListNode head) {
        if (head == null || head.next == null) {
            return false;
        }
        
        ListNode slow = head;
        ListNode fast = head.next;
        
        while (slow != fast) {
            if (fast == null || fast.next == null) {
                return false;
            }
            slow = slow.next;
            fast = fast.next.next;
        }
        
        return true;
    }
    
    // Alternative implementation of Floyd's algorithm
    // Starting both pointers at head
    public boolean hasCycleAlt(ListNode head) {
        if (head == null) {
            return false;
        }
        
        ListNode slow = head;
        ListNode fast = head;
        
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            
            if (slow == fast) {
                return true;
            }
        }
        
        return false;
    }
    
    // Solution 2: HashSet approach
    // Time Complexity: O(n), Space Complexity: O(n)
    // Uses extra space but intuitive to understand
    public boolean hasCycleHashSet(ListNode head) {
        if (head == null) {
            return false;
        }
        
        HashSet<ListNode> visited = new HashSet<>();
        ListNode current = head;
        
        while (current != null) {
            if (visited.contains(current)) {
                return true;
            }
            visited.add(current);
            current = current.next;
        }
        
        return false;
    }
    
    // Solution 3: Node modification approach (destructive)
    // Time Complexity: O(n), Space Complexity: O(1)
    // Modifies the original list - not recommended for production
    public boolean hasCycleDestructive(ListNode head) {
        if (head == null) {
            return false;
        }
        
        ListNode current = head;
        
        while (current != null) {
            if (current.val == Integer.MIN_VALUE) {
                return true; // We've seen this node before
            }
            current.val = Integer.MIN_VALUE; // Mark as visited
            current = current.next;
        }
        
        return false;
    }

}

// Test class to demonstrate usage
class CycleDetectionTest {

    public static void main(String[] args) {
        CycleDetectionSolution solution = new CycleDetectionSolution();
        
        // Test Case 1: [3,2,0,-4] with cycle at position 1
        ListNode head1 = new ListNode(3);
        head1.next = new ListNode(2);
        head1.next.next = new ListNode(0);
        head1.next.next.next = new ListNode(-4);
        head1.next.next.next.next = head1.next; // Create cycle
        
        System.out.println("Test 1 - Expected: true, Got: " + solution.hasCycle(head1));
        
        // Test Case 2: [1,2] with cycle at position 0
        ListNode head2 = new ListNode(1);
        head2.next = new ListNode(2);
        head2.next.next = head2; // Create cycle
        
        System.out.println("Test 2 - Expected: true, Got: " + solution.hasCycle(head2));
        
        // Test Case 3: [1] with no cycle
        ListNode head3 = new ListNode(1);
        
        System.out.println("Test 3 - Expected: false, Got: " + solution.hasCycle(head3));
        
        // Test Case 4: Empty list
        System.out.println("Test 4 - Expected: false, Got: " + solution.hasCycle(null));
    }

}


class LinkedListCycleDetector {
    
    // Approach 1: Floyd's Cycle Detection (Two Pointers - Fast & Slow) - OPTIMAL
    // Time: O(n), Space: O(1)
    public boolean hasCycle(ListNode head) {
        if (head == null || head.next == null) {
            return false;
        }
        
        ListNode slow = head;
        ListNode fast = head;
        
        while (fast != null && fast.next != null) {
            slow = slow.next;           // Move 1 step
            fast = fast.next.next;      // Move 2 steps
            
            if (slow == fast) {         // Cycle detected
                return true;
            }
        }
        
        return false;  // No cycle
    }
    
    // Approach 2: Using HashSet
    // Time: O(n), Space: O(n)
    public boolean hasCycleUsingHashSet(ListNode head) {
        java.util.HashSet<ListNode> visited = new java.util.HashSet<>();
        ListNode current = head;
        
        while (current != null) {
            if (visited.contains(current)) {
                return true;  // Found a node we've seen before
            }
            visited.add(current);
            current = current.next;
        }
        
        return false;  // Reached end without cycle
    }
    
    // Approach 3: Using Node Modification (Not recommended in production)
    // Time: O(n), Space: O(1)
    // Modifies the list structure temporarily
    public boolean hasCycleByModification(ListNode head) {
        ListNode dummy = new ListNode(-1);
        ListNode current = head;
        
        while (current != null) {
            if (current.next == dummy) {
                return true;  // Found our marker
            }
            ListNode nextNode = current.next;
            current.next = dummy;  // Mark as visited
            current = nextNode;
        }
        
        return false;
    }
}

// Test helper class
class CycleDetectionTester {
    
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
    
    // Helper method to print list info (without traversing if there's a cycle)
    public static void printListInfo(int[] values, int pos) {
        System.out.print("List: [");
        for (int i = 0; i < values.length; i++) {
            System.out.print(values[i]);
            if (i < values.length - 1) {
                System.out.print(",");
            }
        }
        System.out.print("], pos = " + pos);
    }
    
    public static void main(String[] args) {
        LinkedListCycleDetector detector = new LinkedListCycleDetector();
        
        System.out.println("=== Linked List Cycle Detection ===\n");
        
        // Example 1: Cycle exists at position 1
        System.out.println("Example 1:");
        int[] values1 = {3, 2, 0, -4};
        int pos1 = 1;
        System.out.print("Input: ");
        printListInfo(values1, pos1);
        System.out.println();
        ListNode head1 = createListWithCycle(values1, pos1);
        boolean result1 = detector.hasCycle(head1);
        System.out.println("Output: " + result1);
        System.out.println("Explanation: There is a cycle, tail connects to node at index 1.\n");
        
        // Example 2: Cycle exists at position 0
        System.out.println("Example 2:");
        int[] values2 = {1, 2};
        int pos2 = 0;
        System.out.print("Input: ");
        printListInfo(values2, pos2);
        System.out.println();
        ListNode head2 = createListWithCycle(values2, pos2);
        boolean result2 = detector.hasCycle(head2);
        System.out.println("Output: " + result2);
        System.out.println("Explanation: There is a cycle, tail connects to node at index 0.\n");
        
        // Example 3: No cycle
        System.out.println("Example 3:");
        int[] values3 = {1};
        int pos3 = -1;
        System.out.print("Input: ");
        printListInfo(values3, pos3);
        System.out.println();
        ListNode head3 = createListWithCycle(values3, pos3);
        boolean result3 = detector.hasCycle(head3);
        System.out.println("Output: " + result3);
        System.out.println("Explanation: There is no cycle in the linked list.\n");
        
        System.out.println("=== Additional Test Cases ===\n");
        
        // Test 4: Empty list
        System.out.println("Test 4: Empty list");
        ListNode head4 = null;
        boolean result4 = detector.hasCycle(head4);
        System.out.println("Output: " + result4 + "\n");
        
        // Test 5: List without cycle
        System.out.println("Test 5: List without cycle [1,2,3,4,5]");
        int[] values5 = {1, 2, 3, 4, 5};
        ListNode head5 = createListWithCycle(values5, -1);
        boolean result5 = detector.hasCycle(head5);
        System.out.println("Output: " + result5 + "\n");
        
        System.out.println("=== Testing HashSet Approach ===\n");
        
        System.out.println("HashSet Test with cycle:");
        int[] values6 = {3, 2, 0, -4};
        ListNode head6 = createListWithCycle(values6, 1);
        boolean result6 = detector.hasCycleUsingHashSet(head6);
        System.out.println("Output: " + result6);
        
        System.out.println("\n=== Algorithm Explanation ===");
        System.out.println("Floyd's Cycle Detection (Tortoise and Hare):");
        System.out.println("- Slow pointer moves 1 step at a time");
        System.out.println("- Fast pointer moves 2 steps at a time");
        System.out.println("- If there's a cycle, fast will eventually catch slow");
        System.out.println("- If there's no cycle, fast will reach null");
        System.out.println("\nTime Complexity: O(n)");
        System.out.println("Space Complexity: O(1)");
    }
}

