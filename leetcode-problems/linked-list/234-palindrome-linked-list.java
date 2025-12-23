// Definition for singly-linked list node
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { 
        this.val = val; 
        this.next = null;
    }
}

class PalindromeChecker {
    
    // Approach 1: Find Middle + Reverse Second Half + Compare - OPTIMAL
    // Time: O(n), Space: O(1)
    public boolean isPalindrome(ListNode head) {
        if (head == null || head.next == null) {
            return true;
        }
        
        // Step 1: Find the middle of the linked list
        ListNode slow = head;
        ListNode fast = head;
        
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        
        // Step 2: Reverse the second half
        ListNode secondHalf = reverseList(slow);
        ListNode firstHalf = head;
        
        // Step 3: Compare both halves
        ListNode secondHalfCopy = secondHalf;  // Keep copy to restore later
        boolean isPalin = true;
        
        while (secondHalf != null) {
            if (firstHalf.val != secondHalf.val) {
                isPalin = false;
                break;
            }
            firstHalf = firstHalf.next;
            secondHalf = secondHalf.next;
        }
        
        // Step 4: Restore the list (optional, good practice)
        reverseList(secondHalfCopy);
        
        return isPalin;
    }
    
    // Helper method to reverse a linked list
    private ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode current = head;
        
        while (current != null) {
            ListNode nextTemp = current.next;
            current.next = prev;
            prev = current;
            current = nextTemp;
        }
        
        return prev;
    }
    
    // Approach 2: Using ArrayList
    // Time: O(n), Space: O(n)
    public boolean isPalindromeUsingArrayList(ListNode head) {
        java.util.ArrayList<Integer> values = new java.util.ArrayList<>();
        
        // Copy values to ArrayList
        ListNode current = head;
        while (current != null) {
            values.add(current.val);
            current = current.next;
        }
        
        // Check palindrome using two pointers
        int left = 0;
        int right = values.size() - 1;
        
        while (left < right) {
            if (!values.get(left).equals(values.get(right))) {
                return false;
            }
            left++;
            right--;
        }
        
        return true;
    }
    
    // Approach 3: Using Stack
    // Time: O(n), Space: O(n)
    public boolean isPalindromeUsingStack(ListNode head) {
        java.util.Stack<Integer> stack = new java.util.Stack<>();
        
        // Find middle and push first half to stack
        ListNode slow = head;
        ListNode fast = head;
        
        while (fast != null && fast.next != null) {
            stack.push(slow.val);
            slow = slow.next;
            fast = fast.next.next;
        }
        
        // If odd number of nodes, skip middle
        if (fast != null) {
            slow = slow.next;
        }
        
        // Compare second half with stack
        while (slow != null) {
            if (stack.pop() != slow.val) {
                return false;
            }
            slow = slow.next;
        }
        
        return true;
    }
    
    // Approach 4: Using Recursion
    // Time: O(n), Space: O(n) due to recursion stack
    private ListNode frontPointer;
    
    public boolean isPalindromeRecursive(ListNode head) {
        frontPointer = head;
        return recursiveCheck(head);
    }
    
    private boolean recursiveCheck(ListNode currentNode) {
        if (currentNode != null) {
            if (!recursiveCheck(currentNode.next)) {
                return false;
            }
            if (currentNode.val != frontPointer.val) {
                return false;
            }
            frontPointer = frontPointer.next;
        }
        return true;
    }
}

// Test helper class
class PalindromeTester {
    
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
    
    public static void main(String[] args) {
        PalindromeChecker checker = new PalindromeChecker();
        
        System.out.println("=== Palindrome Linked List Checker ===\n");
        
        // Example 1: [1,2,2,1]
        System.out.println("Example 1:");
        int[] values1 = {1, 2, 2, 1};
        System.out.print("Input: head = ");
        printList(createList(values1));
        System.out.println();
        ListNode head1 = createList(values1);
        boolean result1 = checker.isPalindrome(head1);
        System.out.println("Output: " + result1);
        System.out.println("Explanation: The list is a palindrome.\n");
        
        // Example 2: [1,2]
        System.out.println("Example 2:");
        int[] values2 = {1, 2};
        System.out.print("Input: head = ");
        printList(createList(values2));
        System.out.println();
        ListNode head2 = createList(values2);
        boolean result2 = checker.isPalindrome(head2);
        System.out.println("Output: " + result2);
        System.out.println("Explanation: The list is not a palindrome.\n");
        
        System.out.println("=== Additional Test Cases ===\n");
        
        // Test 3: Single element
        System.out.println("Test 3: Single element [1]");
        int[] values3 = {1};
        System.out.print("Input: head = ");
        printList(createList(values3));
        System.out.println();
        ListNode head3 = createList(values3);
        boolean result3 = checker.isPalindrome(head3);
        System.out.println("Output: " + result3 + "\n");
        
        // Test 4: Odd length palindrome
        System.out.println("Test 4: Odd length palindrome [1,2,3,2,1]");
        int[] values4 = {1, 2, 3, 2, 1};
        System.out.print("Input: head = ");
        printList(createList(values4));
        System.out.println();
        ListNode head4 = createList(values4);
        boolean result4 = checker.isPalindrome(head4);
        System.out.println("Output: " + result4 + "\n");
        
        // Test 5: Even length palindrome
        System.out.println("Test 5: Even length palindrome [1,2,3,3,2,1]");
        int[] values5 = {1, 2, 3, 3, 2, 1};
        System.out.print("Input: head = ");
        printList(createList(values5));
        System.out.println();
        ListNode head5 = createList(values5);
        boolean result5 = checker.isPalindrome(head5);
        System.out.println("Output: " + result5 + "\n");
        
        // Test 6: Not a palindrome
        System.out.println("Test 6: Not a palindrome [1,2,3,4,5]");
        int[] values6 = {1, 2, 3, 4, 5};
        System.out.print("Input: head = ");
        printList(createList(values6));
        System.out.println();
        ListNode head6 = createList(values6);
        boolean result6 = checker.isPalindrome(head6);
        System.out.println("Output: " + result6 + "\n");
        
        // Test 7: Two identical elements
        System.out.println("Test 7: Two identical elements [1,1]");
        int[] values7 = {1, 1};
        System.out.print("Input: head = ");
        printList(createList(values7));
        System.out.println();
        ListNode head7 = createList(values7);
        boolean result7 = checker.isPalindrome(head7);
        System.out.println("Output: " + result7 + "\n");
        
        System.out.println("=== Testing Different Approaches ===\n");
        
        // Compare approaches
        int[] testValues = {1, 2, 3, 2, 1};
        System.out.print("Test list: ");
        printList(createList(testValues));
        System.out.println("\n");
        
        ListNode test1 = createList(testValues);
        System.out.println("Approach 1 (Optimal - Reverse Half): " + 
            checker.isPalindrome(test1));
        
        ListNode test2 = createList(testValues);
        System.out.println("Approach 2 (ArrayList): " + 
            checker.isPalindromeUsingArrayList(test2));
        
        ListNode test3 = createList(testValues);
        System.out.println("Approach 3 (Stack): " + 
            checker.isPalindromeUsingStack(test3));
        
        ListNode test4 = createList(testValues);
        System.out.println("Approach 4 (Recursive): " + 
            checker.isPalindromeRecursive(test4));
        
        System.out.println("\n=== Algorithm Explanation ===");
        System.out.println("Optimal Approach (Find Middle + Reverse Second Half):");
        System.out.println();
        System.out.println("Step 1: Find middle using slow/fast pointers");
        System.out.println("  - Slow moves 1 step, fast moves 2 steps");
        System.out.println("  - When fast reaches end, slow is at middle");
        System.out.println();
        System.out.println("Step 2: Reverse the second half of the list");
        System.out.println("  - Use standard iterative reversal");
        System.out.println();
        System.out.println("Step 3: Compare first half with reversed second half");
        System.out.println("  - If all values match, it's a palindrome");
        System.out.println();
        System.out.println("Step 4: (Optional) Restore the list to original state");
        System.out.println();
        System.out.println("Time Complexity: O(n)");
        System.out.println("Space Complexity: O(1)");
        System.out.println();
        System.out.println("Example: [1,2,3,2,1]");
        System.out.println("  1. Find middle: slow at 3");
        System.out.println("  2. Reverse [3,2,1] → [1,2,3]");
        System.out.println("  3. Compare [1,2] with [1,2] → Match!");
    }
}
