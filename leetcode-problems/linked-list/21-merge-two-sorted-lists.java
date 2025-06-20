/*
 * You are given the heads of two sorted linked lists list1 and list2.
 * 
 * Merge the two lists into one sorted list. The list should be made by splicing
 * together the nodes of the first two lists.
 * 
 * Return the head of the merged linked list.
 * 
 * Example 1:
 * Input: list1 = [1,2,4], list2 = [1,3,4]
 * Output: [1,1,2,3,4,4]
 * 
 * Example 2:
 * Input: list1 = [], list2 = []
 * Output: []
 * 
 * Example 3:
 * Input: list1 = [], list2 = [0]
 * Output: [0]
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

    // Solution 1: Iterative approach (Recommended)
    // Time Complexity: O(m + n), Space Complexity: O(1)
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // Create a dummy head to simplify edge cases
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;

        // Compare and merge while both lists have nodes
        while (list1 != null && list2 != null) {
            if (list1.val <= list2.val) {
                current.next = list1;
                list1 = list1.next;
            } else {
                current.next = list2;
                list2 = list2.next;
            }
            current = current.next;
        }

        // Append remaining nodes (at most one list will have remaining nodes)
        current.next = (list1 != null) ? list1 : list2;

        return dummy.next;
    }

    // Solution 2: Recursive approach (Elegant but uses stack space)
    // Time Complexity: O(m + n), Space Complexity: O(m + n) due to recursion stack
    public ListNode mergeTwoListsRecursive(ListNode list1, ListNode list2) {
        // Base cases
        if (list1 == null)
            return list2;
        if (list2 == null)
            return list1;

        // Recursive case
        if (list1.val <= list2.val) {
            list1.next = mergeTwoListsRecursive(list1.next, list2);
            return list1;
        } else {
            list2.next = mergeTwoListsRecursive(list1, list2.next);
            return list2;
        }
    }

    // Solution 3: Alternative iterative with explicit null checks
    public ListNode mergeTwoListsVerbose(ListNode list1, ListNode list2) {
        // Handle edge cases
        if (list1 == null)
            return list2;
        if (list2 == null)
            return list1;

        ListNode dummy = new ListNode(0);
        ListNode current = dummy;

        while (list1 != null && list2 != null) {
            if (list1.val <= list2.val) {
                current.next = new ListNode(list1.val);
                list1 = list1.next;
            } else {
                current.next = new ListNode(list2.val);
                list2 = list2.next;
            }
            current = current.next;
        }

        // Add remaining elements
        while (list1 != null) {
            current.next = new ListNode(list1.val);
            list1 = list1.next;
            current = current.next;
        }

        while (list2 != null) {
            current.next = new ListNode(list2.val);
            list2 = list2.next;
            current = current.next;
        }

        return dummy.next;
    }

    // Solution 4: In-place merge without dummy node (more complex but saves one
    // node)
    public ListNode mergeTwoListsInPlace(ListNode list1, ListNode list2) {
        if (list1 == null)
            return list2;
        if (list2 == null)
            return list1;

        // Determine the head of the merged list
        ListNode head, current;
        if (list1.val <= list2.val) {
            head = current = list1;
            list1 = list1.next;
        } else {
            head = current = list2;
            list2 = list2.next;
        }

        // Merge the rest
        while (list1 != null && list2 != null) {
            if (list1.val <= list2.val) {
                current.next = list1;
                list1 = list1.next;
            } else {
                current.next = list2;
                list2 = list2.next;
            }
            current = current.next;
        }

        // Append remaining nodes
        current.next = (list1 != null) ? list1 : list2;

        return head;
    }
}

// Test class for demonstration and validation
class MergeSortedListsTest {

    // Helper method to create a linked list from an array
    public static ListNode createList(int[] nums) {
        if (nums.length == 0)
            return null;

        ListNode head = new ListNode(nums[0]);
        ListNode current = head;

        for (int i = 1; i < nums.length; i++) {
            current.next = new ListNode(nums[i]);
            current = current.next;
        }

        return head;
    }

    // Helper method to print a linked list
    public static void printList(ListNode head) {
        System.out.print("[");
        ListNode current = head;
        boolean first = true;

        while (current != null) {
            if (!first) {
                System.out.print(",");
            }
            System.out.print(current.val);
            current = current.next;
            first = false;
        }
        System.out.println("]");
    }

    // Helper method to convert linked list to array for validation
    public static int[] listToArray(ListNode head) {
        java.util.List<Integer> result = new java.util.ArrayList<>();
        ListNode current = head;

        while (current != null) {
            result.add(current.val);
            current = current.next;
        }

        return result.stream().mapToInt(i -> i).toArray();
    }

    // Helper method to validate if a list is sorted
    public static boolean isSorted(ListNode head) {
        if (head == null || head.next == null)
            return true;

        ListNode current = head;
        while (current.next != null) {
            if (current.val > current.next.val) {
                return false;
            }
            current = current.next;
        }
        return true;
    }

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test Case 1: [1,2,4] + [1,3,4] = [1,1,2,3,4,4]
        ListNode list1 = createList(new int[] { 1, 2, 4 });
        ListNode list2 = createList(new int[] { 1, 3, 4 });
        ListNode result1 = solution.mergeTwoLists(list1, list2);
        System.out.print("Test 1 - Expected: [1,1,2,3,4,4], Got: ");
        printList(result1);
        System.out.println("Is sorted: " + isSorted(result1));

        // Test Case 2: [] + [] = []
        ListNode list3 = createList(new int[] {});
        ListNode list4 = createList(new int[] {});
        ListNode result2 = solution.mergeTwoLists(list3, list4);
        System.out.print("Test 2 - Expected: [], Got: ");
        printList(result2);

        // Test Case 3: [] + [0] = [0]
        ListNode list5 = createList(new int[] {});
        ListNode list6 = createList(new int[] { 0 });
        ListNode result3 = solution.mergeTwoLists(list5, list6);
        System.out.print("Test 3 - Expected: [0], Got: ");
        printList(result3);

        // Additional Test Case 4: Different lengths
        ListNode list7 = createList(new int[] { 1, 3, 5, 7, 9 });
        ListNode list8 = createList(new int[] { 2, 4 });
        ListNode result4 = solution.mergeTwoLists(list7, list8);
        System.out.print("Test 4 - [1,3,5,7,9] + [2,4], Got: ");
        printList(result4);
        System.out.println("Is sorted: " + isSorted(result4));

        // Test Case 5: One list much smaller
        ListNode list9 = createList(new int[] { 5 });
        ListNode list10 = createList(new int[] { 1, 2, 3, 4 });
        ListNode result5 = solution.mergeTwoLists(list9, list10);
        System.out.print("Test 5 - [5] + [1,2,3,4], Got: ");
        printList(result5);
        System.out.println("Is sorted: " + isSorted(result5));

        // Test recursive solution
        System.out.println("\nTesting Recursive Solution:");
        ListNode list11 = createList(new int[] { 1, 2, 4 });
        ListNode list12 = createList(new int[] { 1, 3, 4 });
        ListNode result6 = solution.mergeTwoListsRecursive(list11, list12);
        System.out.print("Recursive - Expected: [1,1,2,3,4,4], Got: ");
        printList(result6);
    }

}
