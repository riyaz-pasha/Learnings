/*
 * Given the head of a singly linked list and two integers left and right where
 * left <= right, reverse the nodes of the list from position left to position
 * right, and return the reversed list.
 * 
 * Example 1:
 * Input: head = [1,2,3,4,5], left = 2, right = 4
 * Output: [1,4,3,2,5]
 * 
 * Example 2:
 * Input: head = [5], left = 1, right = 1
 * Output: [5]
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

    // Solution 1: One-pass iterative approach (Recommended)
    // Time Complexity: O(n), Space Complexity: O(1)
    public ListNode reverseBetween(ListNode head, int left, int right) {
        if (head == null || left == right)
            return head;

        // Create dummy node to handle edge case where left = 1
        ListNode dummy = new ListNode(0);
        dummy.next = head;

        // Find the node before the reversal starts
        ListNode prev = dummy;
        for (int i = 0; i < left - 1; i++) {
            prev = prev.next;
        }

        // Start reversing from the left position
        ListNode current = prev.next;

        // Reverse the sublist using the standard reversal technique
        for (int i = 0; i < right - left; i++) {
            ListNode nextNode = current.next;
            current.next = nextNode.next;
            nextNode.next = prev.next;
            prev.next = nextNode;
        }

        return dummy.next;
    }

    // Solution 2: Three-step approach (More intuitive)
    // Time Complexity: O(n), Space Complexity: O(1)
    public ListNode reverseBetweenThreeStep(ListNode head, int left, int right) {
        if (head == null || left == right)
            return head;

        ListNode dummy = new ListNode(0);
        dummy.next = head;

        // Step 1: Find the nodes before and at the start of reversal
        ListNode prevLeft = dummy;
        for (int i = 0; i < left - 1; i++) {
            prevLeft = prevLeft.next;
        }
        ListNode leftNode = prevLeft.next;

        // Step 2: Find the node at the end of reversal
        ListNode rightNode = leftNode;
        for (int i = 0; i < right - left; i++) {
            rightNode = rightNode.next;
        }
        ListNode postRight = rightNode.next;

        // Step 3: Disconnect, reverse, and reconnect
        prevLeft.next = null;
        rightNode.next = null;

        reverseList(leftNode);

        prevLeft.next = rightNode;
        leftNode.next = postRight;

        return dummy.next;
    }

    // Helper method to reverse a complete linked list
    private ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode current = head;

        while (current != null) {
            ListNode nextNode = current.next;
            current.next = prev;
            prev = current;
            current = nextNode;
        }

        return prev;
    }

    // Solution 3: Recursive approach
    // Time Complexity: O(n), Space Complexity: O(n) due to recursion
    private boolean stopReverse = false;
    private ListNode leftPtr;

    public ListNode reverseBetweenRecursive(ListNode head, int left, int right) {
        leftPtr = head;
        recurseAndReverse(head, left, right);
        return head;
    }

    private void recurseAndReverse(ListNode rightPtr, int left, int right) {
        if (right == 1) {
            return;
        }

        rightPtr = rightPtr.next;

        if (left > 1) {
            leftPtr = leftPtr.next;
        }

        recurseAndReverse(rightPtr, left - 1, right - 1);

        if (leftPtr == rightPtr || rightPtr.next == leftPtr) {
            stopReverse = true;
        }

        if (!stopReverse) {
            int temp = leftPtr.val;
            leftPtr.val = rightPtr.val;
            rightPtr.val = temp;

            leftPtr = leftPtr.next;
        }
    }

    // Solution 4: Stack-based approach
    // Time Complexity: O(n), Space Complexity: O(right - left + 1)
    public ListNode reverseBetweenStack(ListNode head, int left, int right) {
        if (head == null || left == right)
            return head;

        java.util.Stack<Integer> stack = new java.util.Stack<>();
        ListNode current = head;

        // Traverse to collect values in the range
        for (int i = 1; i <= right; i++) {
            if (i >= left) {
                stack.push(current.val);
            }
            current = current.next;
        }

        // Traverse again to replace values
        current = head;
        for (int i = 1; i <= right; i++) {
            if (i >= left) {
                current.val = stack.pop();
            }
            current = current.next;
        }

        return head;
    }

    // Solution 5: Array-based approach (for understanding)
    // Time Complexity: O(n), Space Complexity: O(n)
    public ListNode reverseBetweenArray(ListNode head, int left, int right) {
        if (head == null || left == right)
            return head;

        // Convert to array
        java.util.List<Integer> values = new java.util.ArrayList<>();
        ListNode current = head;
        while (current != null) {
            values.add(current.val);
            current = current.next;
        }

        // Reverse the subarray
        int leftIdx = left - 1;
        int rightIdx = right - 1;
        while (leftIdx < rightIdx) {
            int temp = values.get(leftIdx);
            values.set(leftIdx, values.get(rightIdx));
            values.set(rightIdx, temp);
            leftIdx++;
            rightIdx--;
        }

        // Convert back to linked list
        current = head;
        for (int i = 0; i < values.size(); i++) {
            current.val = values.get(i);
            current = current.next;
        }

        return head;
    }
}

// Test class for demonstration and validation
class ReverseBetweenTest {

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

    // Helper method to copy a linked list for testing multiple solutions
    public static ListNode copyList(ListNode head) {
        if (head == null)
            return null;

        ListNode newHead = new ListNode(head.val);
        ListNode current = newHead;
        ListNode original = head.next;

        while (original != null) {
            current.next = new ListNode(original.val);
            current = current.next;
            original = original.next;
        }

        return newHead;
    }

    public static void main(String[] args) {
        MiddleOfTheLinkedListSolution solution = new MiddleOfTheLinkedListSolution();

        // Test Case 1: [1,2,3,4,5], left = 2, right = 4
        System.out.println("=== Test Case 1 ===");
        ListNode list1 = createList(new int[] { 1, 2, 3, 4, 5 });
        System.out.print("Original: ");
        printList(list1);

        ListNode result1 = solution.reverseBetween(copyList(list1), 2, 4);
        System.out.print("Reversed (2,4): ");
        printList(result1);
        System.out.println("Expected: [1,4,3,2,5]");

        // Test Case 2: [5], left = 1, right = 1
        System.out.println("\n=== Test Case 2 ===");
        ListNode list2 = createList(new int[] { 5 });
        System.out.print("Original: ");
        printList(list2);

        ListNode result2 = solution.reverseBetween(copyList(list2), 1, 1);
        System.out.print("Reversed (1,1): ");
        printList(result2);
        System.out.println("Expected: [5]");

        // Test Case 3: Reverse entire list
        System.out.println("\n=== Test Case 3 ===");
        ListNode list3 = createList(new int[] { 1, 2, 3, 4, 5 });
        System.out.print("Original: ");
        printList(list3);

        ListNode result3 = solution.reverseBetween(copyList(list3), 1, 5);
        System.out.print("Reversed (1,5): ");
        printList(result3);
        System.out.println("Expected: [5,4,3,2,1]");

        // Test Case 4: Reverse from beginning
        System.out.println("\n=== Test Case 4 ===");
        ListNode list4 = createList(new int[] { 1, 2, 3, 4, 5 });
        System.out.print("Original: ");
        printList(list4);

        ListNode result4 = solution.reverseBetween(copyList(list4), 1, 3);
        System.out.print("Reversed (1,3): ");
        printList(result4);
        System.out.println("Expected: [3,2,1,4,5]");

        // Test Case 5: Reverse at the end
        System.out.println("\n=== Test Case 5 ===");
        ListNode list5 = createList(new int[] { 1, 2, 3, 4, 5 });
        System.out.print("Original: ");
        printList(list5);

        ListNode result5 = solution.reverseBetween(copyList(list5), 3, 5);
        System.out.print("Reversed (3,5): ");
        printList(result5);
        System.out.println("Expected: [1,2,5,4,3]");

        // Test different approaches on the same input
        System.out.println("\n=== Comparing Different Approaches ===");
        ListNode original = createList(new int[] { 1, 2, 3, 4, 5 });

        ListNode result_iterative = solution.reverseBetween(copyList(original), 2, 4);
        System.out.print("Iterative: ");
        printList(result_iterative);

        ListNode result_threestep = solution.reverseBetweenThreeStep(copyList(original), 2, 4);
        System.out.print("Three-step: ");
        printList(result_threestep);

        ListNode result_stack = solution.reverseBetweenStack(copyList(original), 2, 4);
        System.out.print("Stack-based: ");
        printList(result_stack);

        ListNode result_array = solution.reverseBetweenArray(copyList(original), 2, 4);
        System.out.print("Array-based: ");
        printList(result_array);
    }

}

class Solution2 {

    public ListNode reverseBetween(ListNode head, int left, int right) {
        if (head == null)
            return null;

        ListNode newListPointer = new ListNode(0);
        ListNode dummyHead = newListPointer;
        ListNode current = head;
        int position = 1;

        // Add nodes before the "left" position
        while (current != null && position < left) {
            dummyHead.next = new ListNode(current.val);
            dummyHead = dummyHead.next;
            current = current.next;
            position++;
        }

        // Reverse nodes between "left" and "right" positions
        ListNode reverseHead = null;
        while (current != null && position <= right) {
            ListNode newNode = new ListNode(current.val);
            newNode.next = reverseHead;
            reverseHead = newNode;
            current = current.next;
            position++;
        }
        dummyHead.next = reverseHead;

        // Move to the end of the reversed segment
        while (dummyHead.next != null) {
            dummyHead = dummyHead.next;
        }

        // Add nodes after the "right" position
        while (current != null) {
            dummyHead.next = new ListNode(current.val);
            dummyHead = dummyHead.next;
            current = current.next;
        }

        return newListPointer.next;
    }

}

/*
 * 🧠 How to Remember Linked List Reversal: The 3-Pointer Mental Model
 *
 * You only need three pointers to reverse a singly linked list:
 *     prev, curr, and next
 *
 * Visual Example:
 *     Original List:  A -> B -> C -> D -> null
 *
 *     Initial State:
 *         prev = null
 *         curr = A
 *         next = B (saved using curr.next)
 *
 * Step-by-step Process:
 * 1. Save the next node:
 *        next = curr.next   // Keep track of what's next
 *
 * 2. Reverse the link:
 *        curr.next = prev   // Reverse the current pointer
 *
 * 3. Move the pointers forward:
 *        prev = curr        // Move prev to current node
 *        curr = next        // Move curr to next node
 *
 * Repeat these steps until curr becomes null.
 *
 * Final State after reversal:
 *     D -> C -> B -> A -> null
 *
 *     prev = D (new head of reversed list)
 *     curr = null (end of list)
 *
 * This pattern is commonly used in both full list reversal and segment-based reversal (e.g., reverse in groups of K).
 */
