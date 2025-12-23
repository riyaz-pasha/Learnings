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

class Solution {
    
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
        MiddleOfTheLinkedListSolution solution = new MiddleOfTheLinkedListSolution();
        
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
