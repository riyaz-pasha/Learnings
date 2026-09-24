/*
 * You are given two non-empty linked lists representing two non-negative
 * integers. The digits are stored in reverse order, and each of their nodes
 * contains a single digit. Add the two numbers and return the sum as a linked
 * list.
 * 
 * You may assume the two numbers do not contain any leading zero, except the
 * number 0 itself.
 * 
 * Example 1:
 * Input: l1 = [2,4,3], l2 = [5,6,4]
 * Output: [7,0,8]
 * Explanation: 342 + 465 = 807.
 * 
 * Example 2:
 * Input: l1 = [0], l2 = [0]
 * Output: [0]
 * 
 * Example 3:
 * Input: l1 = [9,9,9,9,9,9,9], l2 = [9,9,9,9]
 * Output: [8,9,9,9,0,0,0,1]
 */

// Definition for singly-linked list
class ListNode {

    int val;
    ListNode next;

    ListNode() {
    }

    ListNode(int val) {
        this.val = val;
    }

    ListNode(int val, ListNode next) {
        this.val = val;
        this.next = next;
    }

}

class Solution {

    // Main solution: Add two numbers represented as linked lists
    // Time Complexity: O(max(m, n)) where m and n are lengths of the lists
    // Space Complexity: O(max(m, n)) for the result list
    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode dummyHead = new ListNode(0);
        ListNode current = dummyHead;
        int carry = 0;

        while (l1 != null || l2 != null || carry != 0) {
            int sum = carry;

            if (l1 != null) {
                sum += l1.val;
                l1 = l1.next;
            }

            if (l2 != null) {
                sum += l2.val;
                l2 = l2.next;
            }

            carry = sum / 10;
            current.next = new ListNode(sum % 10);
            current = current.next;
        }

        return dummyHead.next;
    }

    // Alternative solution with more explicit handling
    public ListNode addTwoNumbersVerbose(ListNode l1, ListNode l2) {
        ListNode dummyHead = new ListNode(0);
        ListNode current = dummyHead;
        int carry = 0;

        while (l1 != null || l2 != null) {
            int x = (l1 != null) ? l1.val : 0;
            int y = (l2 != null) ? l2.val : 0;
            int sum = x + y + carry;

            carry = sum / 10;
            current.next = new ListNode(sum % 10);
            current = current.next;

            if (l1 != null)
                l1 = l1.next;
            if (l2 != null)
                l2 = l2.next;
        }

        // Handle final carry
        if (carry > 0) {
            current.next = new ListNode(carry);
        }

        return dummyHead.next;
    }

    // Recursive solution (elegant but uses more stack space)
    public ListNode addTwoNumbersRecursive(ListNode l1, ListNode l2) {
        return addTwoNumbersHelper(l1, l2, 0);
    }

    private ListNode addTwoNumbersHelper(ListNode l1, ListNode l2, int carry) {
        if (l1 == null && l2 == null && carry == 0) {
            return null;
        }

        int sum = carry;
        if (l1 != null) {
            sum += l1.val;
            l1 = l1.next;
        }
        if (l2 != null) {
            sum += l2.val;
            l2 = l2.next;
        }

        ListNode result = new ListNode(sum % 10);
        result.next = addTwoNumbersHelper(l1, l2, sum / 10);

        return result;
    }
}

// Utility class for testing and demonstration
class AddTwoNumbersTest {

    // Helper method to create a linked list from an array
    public static ListNode createList(int[] nums) {
        ListNode dummyHead = new ListNode(0);
        ListNode current = dummyHead;

        for (int num : nums) {
            current.next = new ListNode(num);
            current = current.next;
        }

        return dummyHead.next;
    }

    // Helper method to print a linked list
    public static void printList(ListNode head) {
        ListNode current = head;
        System.out.print("[");
        while (current != null) {
            System.out.print(current.val);
            if (current.next != null) {
                System.out.print(",");
            }
            current = current.next;
        }
        System.out.println("]");
    }

    // Helper method to convert linked list to array for easy comparison
    public static int[] listToArray(ListNode head) {
        java.util.List<Integer> result = new java.util.ArrayList<>();
        ListNode current = head;

        while (current != null) {
            result.add(current.val);
            current = current.next;
        }

        return result.stream().mapToInt(i -> i).toArray();
    }

    public static void main(String[] args) {
        MiddleOfTheLinkedListSolution solution = new MiddleOfTheLinkedListSolution();

        // Test Case 1: [2,4,3] + [5,6,4] = [7,0,8]
        // Represents: 342 + 465 = 807
        ListNode l1 = createList(new int[] { 2, 4, 3 });
        ListNode l2 = createList(new int[] { 5, 6, 4 });
        ListNode result1 = solution.addTwoNumbers(l1, l2);
        System.out.print("Test 1 - Expected: [7,0,8], Got: ");
        printList(result1);

        // Test Case 2: [0] + [0] = [0]
        ListNode l3 = createList(new int[] { 0 });
        ListNode l4 = createList(new int[] { 0 });
        ListNode result2 = solution.addTwoNumbers(l3, l4);
        System.out.print("Test 2 - Expected: [0], Got: ");
        printList(result2);

        // Test Case 3: [9,9,9,9,9,9,9] + [9,9,9,9] = [8,9,9,9,0,0,0,1]
        // Represents: 9999999 + 9999 = 10009998
        ListNode l5 = createList(new int[] { 9, 9, 9, 9, 9, 9, 9 });
        ListNode l6 = createList(new int[] { 9, 9, 9, 9 });
        ListNode result3 = solution.addTwoNumbers(l5, l6);
        System.out.print("Test 3 - Expected: [8,9,9,9,0,0,0,1], Got: ");
        printList(result3);

        // Additional Test Case: Different lengths
        ListNode l7 = createList(new int[] { 9, 9 });
        ListNode l8 = createList(new int[] { 1 });
        ListNode result4 = solution.addTwoNumbers(l7, l8);
        System.out.print("Test 4 - [9,9] + [1] = [0,0,1], Got: ");
        printList(result4);

        // Test with single carry
        ListNode l9 = createList(new int[] { 5 });
        ListNode l10 = createList(new int[] { 5 });
        ListNode result5 = solution.addTwoNumbers(l9, l10);
        System.out.print("Test 5 - [5] + [5] = [0,1], Got: ");
        printList(result5);
    }

}
