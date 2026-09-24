// Definition for singly-linked list node
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { 
        this.val = val; 
        this.next = null;
    }
}

class LinkedListSorter {
    
    // Approach 1: Merge Sort (Top-Down) - OPTIMAL for Follow-up
    // Time: O(n log n), Space: O(log n) due to recursion stack
    public ListNode sortList(ListNode head) {
        // Base case
        if (head == null || head.next == null) {
            return head;
        }
        
        // Step 1: Split the list into two halves
        ListNode mid = getMidAndSplit(head);
        
        // Step 2: Recursively sort both halves
        ListNode left = sortList(head);
        ListNode right = sortList(mid);
        
        // Step 3: Merge the sorted halves
        return merge(left, right);
    }
    
    // Helper: Find middle and split list into two halves
    private ListNode getMidAndSplit(ListNode head) {
        ListNode slow = head;
        ListNode fast = head;
        ListNode prev = null;
        
        // Find middle using slow/fast pointers
        while (fast != null && fast.next != null) {
            prev = slow;
            slow = slow.next;
            fast = fast.next.next;
        }
        
        // Split the list
        if (prev != null) {
            prev.next = null;
        }
        
        return slow;
    }
    
    // Helper: Merge two sorted lists
    private ListNode merge(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;
        
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                current.next = l1;
                l1 = l1.next;
            } else {
                current.next = l2;
                l2 = l2.next;
            }
            current = current.next;
        }
        
        // Attach remaining nodes
        if (l1 != null) {
            current.next = l1;
        }
        if (l2 != null) {
            current.next = l2;
        }
        
        return dummy.next;
    }
    
    // Approach 2: Merge Sort (Bottom-Up) - TRUE O(1) Space
    // Time: O(n log n), Space: O(1)
    public ListNode sortListBottomUp(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        
        // Get the length of the list
        int length = getLength(head);
        
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        
        // Start with size 1 and double each iteration
        for (int size = 1; size < length; size *= 2) {
            ListNode prev = dummy;
            ListNode current = dummy.next;
            
            while (current != null) {
                // Get first sorted sublist
                ListNode left = current;
                ListNode right = split(left, size);
                
                // Get second sorted sublist and remaining list
                current = split(right, size);
                
                // Merge and attach to previous node
                prev = mergeAndAttach(prev, left, right);
            }
        }
        
        return dummy.next;
    }
    
    // Helper: Get length of list
    private int getLength(ListNode head) {
        int length = 0;
        while (head != null) {
            length++;
            head = head.next;
        }
        return length;
    }
    
    // Helper: Split list after n nodes
    private ListNode split(ListNode head, int n) {
        while (head != null && n > 1) {
            head = head.next;
            n--;
        }
        
        if (head == null) {
            return null;
        }
        
        ListNode next = head.next;
        head.next = null;
        return next;
    }
    
    // Helper: Merge two lists and attach to prev
    private ListNode mergeAndAttach(ListNode prev, ListNode l1, ListNode l2) {
        ListNode current = prev;
        
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                current.next = l1;
                l1 = l1.next;
            } else {
                current.next = l2;
                l2 = l2.next;
            }
            current = current.next;
        }
        
        if (l1 != null) {
            current.next = l1;
        }
        if (l2 != null) {
            current.next = l2;
        }
        
        // Move to the end of merged list
        while (current.next != null) {
            current = current.next;
        }
        
        return current;
    }
    
    // Approach 3: Convert to Array, Sort, Rebuild
    // Time: O(n log n), Space: O(n)
    public ListNode sortListUsingArray(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        
        // Convert to array
        java.util.ArrayList<Integer> values = new java.util.ArrayList<>();
        ListNode current = head;
        while (current != null) {
            values.add(current.val);
            current = current.next;
        }
        
        // Sort array
        java.util.Collections.sort(values);
        
        // Rebuild list
        ListNode dummy = new ListNode(0);
        current = dummy;
        for (int val : values) {
            current.next = new ListNode(val);
            current = current.next;
        }
        
        return dummy.next;
    }
}

// Test helper class
class LinkedListSorterTester {
    
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
    
    // Helper to verify if list is sorted
    public static boolean isSorted(ListNode head) {
        if (head == null || head.next == null) {
            return true;
        }
        
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
        LinkedListSorter sorter = new LinkedListSorter();
        
        System.out.println("=== Sort Linked List ===\n");
        
        // Example 1: [4,2,1,3]
        System.out.println("Example 1:");
        int[] values1 = {4, 2, 1, 3};
        System.out.print("Input:  head = ");
        printList(createList(values1));
        System.out.println();
        ListNode head1 = createList(values1);
        ListNode result1 = sorter.sortList(head1);
        System.out.print("Output: ");
        printList(result1);
        System.out.println(" - Sorted: " + isSorted(result1));
        System.out.println();
        
        // Example 2: [-1,5,3,4,0]
        System.out.println("Example 2:");
        int[] values2 = {-1, 5, 3, 4, 0};
        System.out.print("Input:  head = ");
        printList(createList(values2));
        System.out.println();
        ListNode head2 = createList(values2);
        ListNode result2 = sorter.sortList(head2);
        System.out.print("Output: ");
        printList(result2);
        System.out.println(" - Sorted: " + isSorted(result2));
        System.out.println();
        
        // Example 3: []
        System.out.println("Example 3:");
        System.out.print("Input:  head = ");
        printList(null);
        System.out.println();
        ListNode result3 = sorter.sortList(null);
        System.out.print("Output: ");
        printList(result3);
        System.out.println();
        System.out.println();
        
        System.out.println("=== Additional Test Cases ===\n");
        
        // Test 4: Single element
        System.out.println("Test 4: Single element [5]");
        int[] values4 = {5};
        System.out.print("Input:  ");
        printList(createList(values4));
        System.out.println();
        ListNode head4 = createList(values4);
        ListNode result4 = sorter.sortList(head4);
        System.out.print("Output: ");
        printList(result4);
        System.out.println(" - Sorted: " + isSorted(result4));
        System.out.println();
        
        // Test 5: Already sorted
        System.out.println("Test 5: Already sorted [1,2,3,4,5]");
        int[] values5 = {1, 2, 3, 4, 5};
        System.out.print("Input:  ");
        printList(createList(values5));
        System.out.println();
        ListNode head5 = createList(values5);
        ListNode result5 = sorter.sortList(head5);
        System.out.print("Output: ");
        printList(result5);
        System.out.println(" - Sorted: " + isSorted(result5));
        System.out.println();
        
        // Test 6: Reverse sorted
        System.out.println("Test 6: Reverse sorted [5,4,3,2,1]");
        int[] values6 = {5, 4, 3, 2, 1};
        System.out.print("Input:  ");
        printList(createList(values6));
        System.out.println();
        ListNode head6 = createList(values6);
        ListNode result6 = sorter.sortList(head6);
        System.out.print("Output: ");
        printList(result6);
        System.out.println(" - Sorted: " + isSorted(result6));
        System.out.println();
        
        // Test 7: Duplicates
        System.out.println("Test 7: With duplicates [3,1,4,1,5,9,2,6]");
        int[] values7 = {3, 1, 4, 1, 5, 9, 2, 6};
        System.out.print("Input:  ");
        printList(createList(values7));
        System.out.println();
        ListNode head7 = createList(values7);
        ListNode result7 = sorter.sortList(head7);
        System.out.print("Output: ");
        printList(result7);
        System.out.println(" - Sorted: " + isSorted(result7));
        System.out.println();
        
        // Test 8: Negative numbers
        System.out.println("Test 8: Negative numbers [-5,-2,0,3,1,-4]");
        int[] values8 = {-5, -2, 0, 3, 1, -4};
        System.out.print("Input:  ");
        printList(createList(values8));
        System.out.println();
        ListNode head8 = createList(values8);
        ListNode result8 = sorter.sortList(head8);
        System.out.print("Output: ");
        printList(result8);
        System.out.println(" - Sorted: " + isSorted(result8));
        System.out.println();
        
        System.out.println("=== Testing Bottom-Up Merge Sort (True O(1) Space) ===\n");
        
        int[] values9 = {4, 2, 1, 3};
        System.out.print("Input:  ");
        printList(createList(values9));
        System.out.println();
        ListNode head9 = createList(values9);
        ListNode result9 = sorter.sortListBottomUp(head9);
        System.out.print("Output: ");
        printList(result9);
        System.out.println(" - Sorted: " + isSorted(result9));
        System.out.println();
        
        System.out.println("=== Algorithm Explanation ===\n");
        System.out.println("Approach 1: Merge Sort (Top-Down Recursive)");
        System.out.println("Time: O(n log n), Space: O(log n) [recursion stack]");
        System.out.println();
        System.out.println("Step 1: Divide");
        System.out.println("  - Find middle using slow/fast pointers");
        System.out.println("  - Split list into two halves");
        System.out.println();
        System.out.println("Step 2: Conquer");
        System.out.println("  - Recursively sort left half");
        System.out.println("  - Recursively sort right half");
        System.out.println();
        System.out.println("Step 3: Combine");
        System.out.println("  - Merge two sorted halves");
        System.out.println();
        System.out.println("Visual Example: [4,2,1,3]");
        System.out.println();
        System.out.println("         [4,2,1,3]");
        System.out.println("           /    \\");
        System.out.println("       [4,2]    [1,3]");
        System.out.println("       /  \\      /  \\");
        System.out.println("     [4]  [2]  [1]  [3]");
        System.out.println("       \\  /      \\  /");
        System.out.println("       [2,4]    [1,3]");
        System.out.println("           \\    /");
        System.out.println("         [1,2,3,4]");
        System.out.println();
        System.out.println("---");
        System.out.println();
        System.out.println("Approach 2: Merge Sort (Bottom-Up Iterative)");
        System.out.println("Time: O(n log n), Space: O(1) [TRUE constant space!]");
        System.out.println();
        System.out.println("Key difference: No recursion!");
        System.out.println("  - Start with sublists of size 1");
        System.out.println("  - Merge pairs: size 1 → 2 → 4 → 8 ...");
        System.out.println("  - Continue until size >= list length");
        System.out.println();
        System.out.println("Example: [4,2,1,3]");
        System.out.println();
        System.out.println("Size 1: [4] [2] [1] [3]");
        System.out.println("        Merge pairs:");
        System.out.println("        [2,4] [1,3]");
        System.out.println();
        System.out.println("Size 2: [2,4] [1,3]");
        System.out.println("        Merge pairs:");
        System.out.println("        [1,2,3,4]");
        System.out.println();
        System.out.println("---");
        System.out.println();
        System.out.println("Why Merge Sort for Linked Lists?");
        System.out.println("  ✓ O(n log n) time complexity");
        System.out.println("  ✓ Stable sort (preserves order of equal elements)");
        System.out.println("  ✓ Works well with linked lists (no random access needed)");
        System.out.println("  ✓ Bottom-up version achieves O(1) space");
        System.out.println("  ✓ Better than Quick Sort for linked lists");
        System.out.println();
        System.out.println("Comparison with other sorts:");
        System.out.println("  - Quick Sort: O(n log n) avg, but O(n²) worst case");
        System.out.println("  - Heap Sort: Requires random access (not good for lists)");
        System.out.println("  - Insertion Sort: O(n²) time complexity");
    }
}
