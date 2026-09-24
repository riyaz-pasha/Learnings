/*
 * Given the head of a linked list and a value x, partition it such that all
 * nodes less than x come before nodes greater than or equal to x.
 * 
 * You should preserve the original relative order of the nodes in each of the
 * two partitions.
 * 
 * Example 1:
 * Input: head = [1,4,3,2,5,2], x = 3
 * Output: [1,2,2,4,3,5]
 * 
 * Example 2:
 * Input: head = [2,1], x = 2
 * Output: [1,2]
 */

/**
 * Definition for singly-linked list.
 */
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
    /**
     * Two-pointer approach with separate lists (Recommended)
     * Time Complexity: O(n) where n is the number of nodes
     * Space Complexity: O(1) - only using constant extra space
     */
    public ListNode partition(ListNode head, int x) {
        // Create two dummy nodes for the two partitions
        ListNode smallerHead = new ListNode(0); // For nodes < x
        ListNode greaterHead = new ListNode(0); // For nodes >= x

        // Pointers to build the two lists
        ListNode smaller = smallerHead;
        ListNode greater = greaterHead;

        // Traverse the original list
        ListNode current = head;
        while (current != null) {
            if (current.val < x) {
                smaller.next = current;
                smaller = smaller.next;
            } else {
                greater.next = current;
                greater = greater.next;
            }
            current = current.next;
        }

        // Connect the two lists
        greater.next = null; // End the greater list
        smaller.next = greaterHead.next; // Connect smaller list to greater list

        return smallerHead.next;
    }

    /**
     * Alternative implementation with clearer variable names
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public ListNode partitionAlternative(ListNode head, int x) {
        if (head == null)
            return head;

        // Create dummy heads for both partitions
        ListNode beforeDummy = new ListNode(0);
        ListNode afterDummy = new ListNode(0);

        // Track current nodes in both partitions
        ListNode before = beforeDummy;
        ListNode after = afterDummy;

        // Partition the original list
        while (head != null) {
            if (head.val < x) {
                before.next = head;
                before = before.next;
            } else {
                after.next = head;
                after = after.next;
            }
            head = head.next;
        }

        // Terminate the after list and connect the partitions
        after.next = null;
        before.next = afterDummy.next;

        return beforeDummy.next;
    }

    /**
     * In-place partition using insertion approach
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public ListNode partitionInPlace(ListNode head, int x) {
        if (head == null || head.next == null)
            return head;

        ListNode dummy = new ListNode(0);
        dummy.next = head;

        ListNode insertPos = dummy; // Position where next small element should be inserted
        ListNode prev = dummy;
        ListNode curr = head;

        while (curr != null) {
            if (curr.val < x) {
                if (insertPos.next != curr) {
                    // Remove curr from its current position
                    prev.next = curr.next;

                    // Insert curr after insertPos
                    curr.next = insertPos.next;
                    insertPos.next = curr;

                    // Update insertPos but keep prev the same for next iteration
                    insertPos = curr;
                    curr = prev.next;
                } else {
                    // curr is already in the correct position
                    insertPos = curr;
                    prev = curr;
                    curr = curr.next;
                }
            } else {
                prev = curr;
                curr = curr.next;
            }
        }

        return dummy.next;
    }

    /**
     * Recursive solution
     * Time Complexity: O(n)
     * Space Complexity: O(n) due to recursion stack
     */
    public ListNode partitionRecursive(ListNode head, int x) {
        if (head == null)
            return null;

        // Recursively partition the rest of the list
        head.next = partitionRecursive(head.next, x);

        // If current node should be moved to the front
        if (head.val < x) {
            return head;
        }

        // Find the first node that is >= x and insert current node before it
        return insertBeforeGreaterOrEqual(head, x);
    }

    private ListNode insertBeforeGreaterOrEqual(ListNode head, int x) {
        if (head == null || head.val < x) {
            return head;
        }

        // Find the first node < x in the rest of the list
        ListNode curr = head.next;
        ListNode prev = head;

        while (curr != null && curr.val >= x) {
            prev = curr;
            curr = curr.next;
        }

        if (curr != null) {
            // Remove curr and insert it at the beginning
            prev.next = curr.next;
            curr.next = head;
            return curr;
        }

        return head;
    }

    /**
     * Solution with detailed tracking of partition boundaries
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    public ListNode partitionWithBoundaryTracking(ListNode head, int x) {
        if (head == null)
            return head;

        ListNode dummy = new ListNode(0);
        dummy.next = head;

        ListNode smallerTail = dummy; // Tail of nodes < x
        ListNode current = head;
        ListNode prev = dummy;

        // First pass: move all nodes < x to the front
        while (current != null) {
            if (current.val < x) {
                if (prev != smallerTail) {
                    // Remove current from its position
                    prev.next = current.next;

                    // Insert after smallerTail
                    current.next = smallerTail.next;
                    smallerTail.next = current;

                    // Update smallerTail
                    smallerTail = current;

                    // Current moves to prev.next (don't update prev)
                    current = prev.next;
                } else {
                    // Current is already in correct position
                    smallerTail = current;
                    prev = current;
                    current = current.next;
                }
            } else {
                prev = current;
                current = current.next;
            }
        }

        return dummy.next;
    }

    /**
     * Helper method to create a linked list from array (for testing)
     */
    public static ListNode createList(int[] arr) {
        if (arr.length == 0)
            return null;

        ListNode head = new ListNode(arr[0]);
        ListNode curr = head;
        for (int i = 1; i < arr.length; i++) {
            curr.next = new ListNode(arr[i]);
            curr = curr.next;
        }
        return head;
    }

    /**
     * Helper method to print linked list (for testing)
     */
    public static void printList(ListNode head) {
        if (head == null) {
            System.out.println("[]");
            return;
        }

        System.out.print("[");
        ListNode curr = head;
        while (curr != null) {
            System.out.print(curr.val);
            if (curr.next != null) {
                System.out.print(",");
            }
            curr = curr.next;
        }
        System.out.println("]");
    }

    /**
     * Helper method to convert linked list to array (for verification)
     */
    public static int[] listToArray(ListNode head) {
        java.util.List<Integer> result = new java.util.ArrayList<>();
        ListNode curr = head;
        while (curr != null) {
            result.add(curr.val);
            curr = curr.next;
        }
        return result.stream().mapToInt(i -> i).toArray();
    }

    /**
     * Helper method to verify partition correctness
     */
    public static boolean verifyPartition(ListNode head, int x) {
        boolean foundGreaterOrEqual = false;
        ListNode curr = head;

        while (curr != null) {
            if (curr.val >= x) {
                foundGreaterOrEqual = true;
            } else if (foundGreaterOrEqual) {
                // Found a smaller value after a greater/equal value
                return false;
            }
            curr = curr.next;
        }
        return true;
    }

    /**
     * Test the solution
     */
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test Case 1: [1,4,3,2,5,2], x = 3
        // Expected: [1,2,2,4,3,5]
        System.out.println("Test Case 1:");
        int[] arr1 = { 1, 4, 3, 2, 5, 2 };
        ListNode head1 = createList(arr1);
        System.out.print("Original: ");
        printList(head1);

        ListNode result1 = solution.partition(head1, 3);
        System.out.print("After partition (x=3): ");
        printList(result1);
        System.out.println("Valid partition: " + verifyPartition(result1, 3));
        System.out.println();

        // Test Case 2: [2,1], x = 2
        // Expected: [1,2]
        System.out.println("Test Case 2:");
        int[] arr2 = { 2, 1 };
        ListNode head2 = createList(arr2);
        System.out.print("Original: ");
        printList(head2);

        ListNode result2 = solution.partitionAlternative(head2, 2);
        System.out.print("After partition (x=2): ");
        printList(result2);
        System.out.println("Valid partition: " + verifyPartition(result2, 2));
        System.out.println();

        // Test Case 3: All elements less than x [1,2,3], x = 5
        // Expected: [1,2,3]
        System.out.println("Test Case 3 (All elements < x):");
        int[] arr3 = { 1, 2, 3 };
        ListNode head3 = createList(arr3);
        System.out.print("Original: ");
        printList(head3);

        ListNode result3 = solution.partition(head3, 5);
        System.out.print("After partition (x=5): ");
        printList(result3);
        System.out.println("Valid partition: " + verifyPartition(result3, 5));
        System.out.println();

        // Test Case 4: All elements >= x [4,5,6], x = 3
        // Expected: [4,5,6]
        System.out.println("Test Case 4 (All elements >= x):");
        int[] arr4 = { 4, 5, 6 };
        ListNode head4 = createList(arr4);
        System.out.print("Original: ");
        printList(head4);

        ListNode result4 = solution.partitionInPlace(head4, 3);
        System.out.print("After partition (x=3): ");
        printList(result4);
        System.out.println("Valid partition: " + verifyPartition(result4, 3));
        System.out.println();

        // Test Case 5: Single element [1], x = 2
        // Expected: [1]
        System.out.println("Test Case 5 (Single element):");
        int[] arr5 = { 1 };
        ListNode head5 = createList(arr5);
        System.out.print("Original: ");
        printList(head5);

        ListNode result5 = solution.partition(head5, 2);
        System.out.print("After partition (x=2): ");
        printList(result5);
        System.out.println("Valid partition: " + verifyPartition(result5, 2));
        System.out.println();

        // Test Case 6: Already partitioned [1,2,4,5], x = 3
        // Expected: [1,2,4,5]
        System.out.println("Test Case 6 (Already partitioned):");
        int[] arr6 = { 1, 2, 4, 5 };
        ListNode head6 = createList(arr6);
        System.out.print("Original: ");
        printList(head6);

        ListNode result6 = solution.partitionWithBoundaryTracking(head6, 3);
        System.out.print("After partition (x=3): ");
        printList(result6);
        System.out.println("Valid partition: " + verifyPartition(result6, 3));
        System.out.println();

        // Test Case 7: Complex case [3,1,4,1,5,9,2,6], x = 3
        // Expected: [1,1,2,3,4,5,9,6]
        System.out.println("Test Case 7 (Complex case):");
        int[] arr7 = { 3, 1, 4, 1, 5, 9, 2, 6 };
        ListNode head7 = createList(arr7);
        System.out.print("Original: ");
        printList(head7);

        ListNode result7 = solution.partition(head7, 3);
        System.out.print("After partition (x=3): ");
        printList(result7);
        System.out.println("Valid partition: " + verifyPartition(result7, 3));
    }

}
