import java.util.ArrayList;
import java.util.List;

/**
 * PROBLEM DESCRIPTION:
 * Given the head of a linked list, the nodes in it are assigned to each group 
 * in a sequential manner. The length of these groups follows the sequence of 
 * natural numbers (1, 2, 3, 4...).
 * 
 * - The 1st node is assigned to the first group.
 * - The 2nd and 3rd nodes are assigned to the second group.
 * - The 4th, 5th, and 6th nodes are assigned to the third group, and so on.
 * 
 * Your task is to reverse the nodes in each group with an EVEN number of nodes 
 * and return the head of the modified linked list.
 * 
 * Note: The length of the last group may be less than or equal to 1 + the 
 * length of the second to the last group. We still evaluate its actual length 
 * to determine if it should be reversed.
 * 
 * CONSTRAINTS:
 * - 1 <= Number of nodes <= 500
 * - 0 <= Node.val <= 10^3
 * 
 * EXAMPLES:
 * Example 1:
 * Input: head = [5,2,6,3,9,1,7,3,8,4]
 * Output: [5,6,2,3,9,1,4,8,3,7]
 * Visual:
 * Group lengths:  1     2       3          4
 * Original:      [5] [2, 6] [3, 9, 1] [7, 3, 8, 4]
 *                 |    |        |          |
 * Odd/Even?      Odd  Even     Odd        Even
 * Action?        Keep Reverse  Keep       Reverse
 * Swapped:       [5] [6, 2] [3, 9, 1] [4, 8, 3, 7]
 * 
 * Example 2:
 * Input: head = [1,1,0,6,5]
 * Output: [1,0,1,5,6]
 * Visual:
 * Group lengths:  1     2       3 (Actual remaining is 2)
 * Original:      [1] [1, 0] [6, 5]
 *                 |    |        |
 * Odd/Even?      Odd  Even     Even (Length is 2)
 * Action?        Keep Reverse  Reverse
 * Swapped:       [1] [0, 1] [5, 6]
 */

class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
    }
}

public class ReverseEvenLengthGroups {

    /**
     * APPROACH 1: Optimal Pointer Manipulation (In-Place)
     * Time Complexity: O(N) where N is the total number of nodes.
     * Space Complexity: O(1) extra space.
     * 
     * EXPLANATION:
     * 1. The first group always has a length of 1, which is odd. So, the first 
     *    node is never reversed. We can safely start our processing from the 
     *    second group (expected length = 2).
     * 2. We use a pointer `prev` to track the last node of the previous group. 
     *    This is essential to connect the previous group to the new head of the 
     *    reversed group.
     * 3. For each group, we first count the actual number of nodes available 
     *    (up to the expected `groupLength`).
     * 4. If the actual count is EVEN:
     *    - We reverse those `count` nodes using standard linked list reversal.
     *    - We reconnect `prev` to the new head of this reversed group.
     *    - We reconnect the tail of this reversed group to the remaining list.
     *    - We update `prev` to be the tail of this reversed group.
     * 5. If the actual count is ODD:
     *    - We don't reverse. We simply advance the `prev` pointer to the end 
     *      of the current group.
     * 6. Increment the expected `groupLength` and repeat until the list ends.
     */
    public ListNode reverseEvenLengthGroupsOptimal(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        ListNode prev = head; // End of the previous group
        ListNode curr = head.next; // Start of the current group
        int groupLength = 2; // Expected length of the current group

        while (curr != null) {
            // Step A: Count how many nodes actually exist for the current group
            ListNode temp = curr;
            int count = 0;
            while (count < groupLength && temp != null) {
                count++;
                temp = temp.next;
            }

            // Step B: If the actual length is EVEN, reverse the group
            if (count % 2 == 0) {
                ListNode reversedGroupTail = curr;
                ListNode groupPrev = null;
                ListNode groupNext = null;

                // Standard linked list reversal for 'count' nodes
                for (int i = 0; i < count; i++) {
                    groupNext = curr.next;
                    curr.next = groupPrev;
                    groupPrev = curr;
                    curr = groupNext;
                }

                // Connect the reversed group back to the main list
                prev.next = groupPrev; // 'groupPrev' is the new head of this group
                reversedGroupTail.next = curr; // 'curr' is the start of the next group
                
                // Move 'prev' to the end of the reversed group
                prev = reversedGroupTail;
            } 
            // Step C: If the actual length is ODD, do not reverse
            else {
                for (int i = 0; i < count; i++) {
                    prev = curr;
                    curr = curr.next;
                }
            }

            // Prepare for the next group
            groupLength++;
        }

        return head;
    }


    /**
     * APPROACH 2: Value Swapping using an Array (Extra Space)
     * Time Complexity: O(N) to extract values, O(N) to reverse arrays, O(N) to update. Total: O(N)
     * Space Complexity: O(N) to store the values in an ArrayList.
     * 
     * EXPLANATION:
     * Some variations of this problem permit changing the values within the nodes 
     * instead of changing the actual pointers. If node value modification is allowed, 
     * this approach is much easier to write and reason about.
     * 
     * 1. Traverse the linked list and extract all values into a List.
     * 2. Loop through the list using varying group sizes (1, 2, 3...).
     * 3. For each group, find its actual size. If even, reverse the elements 
     *    in that specific segment of the List.
     * 4. Traverse the linked list one more time, updating the node values 
     *    with the modified values from the List.
     */
    public ListNode reverseEvenLengthGroupsValueSwap(ListNode head) {
        if (head == null || head.next == null) return head;

        // Extract all values
        List<Integer> values = new ArrayList<>();
        ListNode curr = head;
        while (curr != null) {
            values.add(curr.val);
            curr = curr.next;
        }

        // Reverse even length groups in the array
        int i = 0;
        int groupLength = 1;
        while (i < values.size()) {
            // Find actual size of current group
            int actualSize = Math.min(groupLength, values.size() - i);
            
            // If even, reverse the subarray
            if (actualSize % 2 == 0) {
                int left = i;
                int right = i + actualSize - 1;
                while (left < right) {
                    int temp = values.get(left);
                    values.set(left, values.get(right));
                    values.set(right, temp);
                    left++;
                    right--;
                }
            }
            
            // Move to the next group
            i += actualSize;
            groupLength++;
        }

        // Reassign values back to the linked list
        curr = head;
        for (int val : values) {
            curr.val = val;
            curr = curr.next;
        }

        return head;
    }


    /**
     * UTILITY: Create List from Array
     */
    public static ListNode createList(int[] arr) {
        if (arr == null || arr.length == 0) return null;
        ListNode head = new ListNode(arr[0]);
        ListNode curr = head;
        for (int i = 1; i < arr.length; i++) {
            curr.next = new ListNode(arr[i]);
            curr = curr.next;
        }
        return head;
    }

    /**
     * UTILITY: Print List
     */
    public static void printList(ListNode head) {
        if (head == null) {
            System.out.println("[]");
            return;
        }
        ListNode curr = head;
        System.out.print("[");
        while (curr != null) {
            System.out.print(curr.val + (curr.next != null ? ", " : "]\n"));
            curr = curr.next;
        }
    }


    /**
     * MAIN METHOD: Demonstrating the approaches with examples.
     */
    public static void main(String[] args) {
        ReverseEvenLengthGroups solver = new ReverseEvenLengthGroups();

        // Test Case 1: Optimal Pointer Manipulation
        ListNode head1 = createList(new int[]{5, 2, 6, 3, 9, 1, 7, 3, 8, 4});
        System.out.println("Original List 1: [5, 2, 6, 3, 9, 1, 7, 3, 8, 4]");
        ListNode result1 = solver.reverseEvenLengthGroupsOptimal(head1);
        System.out.print("Optimal Output:  ");
        printList(result1);
        System.out.println("-------------------------------------------------");

        // Test Case 2: Array Value Swapping approach
        ListNode head2 = createList(new int[]{1, 1, 0, 6, 5});
        System.out.println("Original List 2: [1, 1, 0, 6, 5]");
        System.out.println("Notice: The last group [6, 5] has length 2 (Even), so it gets reversed.");
        ListNode result2 = solver.reverseEvenLengthGroupsValueSwap(head2);
        System.out.print("Array Output:    ");
        printList(result2);
        System.out.println("-------------------------------------------------");

        // Test Case 3: Edge Case (Only one node)
        ListNode head3 = createList(new int[]{10});
        System.out.println("Original List 3: [10]");
        ListNode result3 = solver.reverseEvenLengthGroupsOptimal(head3);
        System.out.print("Optimal Output:  ");
        printList(result3);
    }
}
