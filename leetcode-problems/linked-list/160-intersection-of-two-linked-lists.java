// Definition for singly-linked list node
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { 
        this.val = val; 
        this.next = null;
    }
}

class IntersectionFinder {
    
    // Approach 1: Two Pointers (Elegant Solution) - OPTIMAL
    // Time: O(m + n), Space: O(1) ✓ Meets follow-up requirement!
    public ListNode getIntersectionNode(ListNode headA, ListNode headB) {
        if (headA == null || headB == null) {
            return null;
        }
        
        ListNode pointerA = headA;
        ListNode pointerB = headB;
        
        // Key insight: When a pointer reaches end, redirect it to the other list's head
        // After at most 2 iterations, they'll meet at intersection or both be null
        while (pointerA != pointerB) {
            // When pointerA reaches end, redirect to headB
            pointerA = (pointerA == null) ? headB : pointerA.next;
            
            // When pointerB reaches end, redirect to headA
            pointerB = (pointerB == null) ? headA : pointerB.next;
        }
        
        // Either intersection node or null (no intersection)
        return pointerA;
    }
    
    // Approach 2: Calculate Length Difference
    // Time: O(m + n), Space: O(1)
    public ListNode getIntersectionNodeByLength(ListNode headA, ListNode headB) {
        if (headA == null || headB == null) {
            return null;
        }
        
        // Calculate lengths
        int lenA = getLength(headA);
        int lenB = getLength(headB);
        
        // Align both lists to start at same distance from end
        while (lenA > lenB) {
            headA = headA.next;
            lenA--;
        }
        
        while (lenB > lenA) {
            headB = headB.next;
            lenB--;
        }
        
        // Now both are same distance from end, traverse together
        while (headA != headB) {
            headA = headA.next;
            headB = headB.next;
        }
        
        return headA;
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
    
    // Approach 3: Using HashSet
    // Time: O(m + n), Space: O(m) or O(n)
    public ListNode getIntersectionNodeUsingHashSet(ListNode headA, ListNode headB) {
        if (headA == null || headB == null) {
            return null;
        }
        
        java.util.HashSet<ListNode> visited = new java.util.HashSet<>();
        
        // Add all nodes from list A to set
        ListNode current = headA;
        while (current != null) {
            visited.add(current);
            current = current.next;
        }
        
        // Check nodes from list B
        current = headB;
        while (current != null) {
            if (visited.contains(current)) {
                return current;  // First common node
            }
            current = current.next;
        }
        
        return null;  // No intersection
    }
}

// Test helper class
class IntersectionTester {
    
    // Helper to create intersecting lists
    public static ListNode[] createIntersectingLists(
            int[] valsA, int[] valsB, int[] commonVals, int skipA, int skipB) {
        
        // Create list A up to intersection
        ListNode headA = null;
        ListNode tailA = null;
        
        for (int i = 0; i < skipA; i++) {
            if (headA == null) {
                headA = new ListNode(valsA[i]);
                tailA = headA;
            } else {
                tailA.next = new ListNode(valsA[i]);
                tailA = tailA.next;
            }
        }
        
        // Create list B up to intersection
        ListNode headB = null;
        ListNode tailB = null;
        
        for (int i = 0; i < skipB; i++) {
            if (headB == null) {
                headB = new ListNode(valsB[i]);
                tailB = headB;
            } else {
                tailB.next = new ListNode(valsB[i]);
                tailB = tailB.next;
            }
        }
        
        // Create common part
        ListNode commonHead = null;
        ListNode commonTail = null;
        
        if (commonVals != null && commonVals.length > 0) {
            commonHead = new ListNode(commonVals[0]);
            commonTail = commonHead;
            
            for (int i = 1; i < commonVals.length; i++) {
                commonTail.next = new ListNode(commonVals[i]);
                commonTail = commonTail.next;
            }
            
            // Connect to common part
            if (tailA != null) {
                tailA.next = commonHead;
            } else {
                headA = commonHead;
            }
            
            if (tailB != null) {
                tailB.next = commonHead;
            } else {
                headB = commonHead;
            }
        }
        
        return new ListNode[]{headA, headB, commonHead};
    }
    
    // Helper to print list (stopping at a limit to avoid infinite loops)
    public static void printList(ListNode head, int maxNodes) {
        System.out.print("[");
        ListNode current = head;
        int count = 0;
        
        while (current != null && count < maxNodes) {
            System.out.print(current.val);
            if (current.next != null && count < maxNodes - 1) {
                System.out.print(",");
            }
            current = current.next;
            count++;
        }
        System.out.print("]");
    }
    
    public static void main(String[] args) {
        IntersectionFinder finder = new IntersectionFinder();
        
        System.out.println("=== Intersection of Two Linked Lists ===\n");
        
        // Example 1: intersectVal = 8, listA = [4,1,8,4,5], listB = [5,6,1,8,4,5]
        System.out.println("Example 1:");
        int[] valsA1 = {4, 1};
        int[] valsB1 = {5, 6, 1};
        int[] common1 = {8, 4, 5};
        ListNode[] lists1 = createIntersectingLists(valsA1, valsB1, common1, 2, 3);
        
        System.out.print("ListA: ");
        printList(lists1[0], 10);
        System.out.println();
        System.out.print("ListB: ");
        printList(lists1[1], 10);
        System.out.println();
        
        ListNode result1 = finder.getIntersectionNode(lists1[0], lists1[1]);
        if (result1 != null) {
            System.out.println("Output: Intersected at '" + result1.val + "'");
        } else {
            System.out.println("Output: No intersection");
        }
        System.out.println("Explanation: skipA = 2, skipB = 3, intersection at node with value 8\n");
        
        // Example 2: intersectVal = 2, listA = [1,9,1,2,4], listB = [3,2,4]
        System.out.println("Example 2:");
        int[] valsA2 = {1, 9, 1};
        int[] valsB2 = {3};
        int[] common2 = {2, 4};
        ListNode[] lists2 = createIntersectingLists(valsA2, valsB2, common2, 3, 1);
        
        System.out.print("ListA: ");
        printList(lists2[0], 10);
        System.out.println();
        System.out.print("ListB: ");
        printList(lists2[1], 10);
        System.out.println();
        
        ListNode result2 = finder.getIntersectionNode(lists2[0], lists2[1]);
        if (result2 != null) {
            System.out.println("Output: Intersected at '" + result2.val + "'");
        } else {
            System.out.println("Output: No intersection");
        }
        System.out.println("Explanation: skipA = 3, skipB = 1, intersection at node with value 2\n");
        
        // Example 3: No intersection
        System.out.println("Example 3:");
        int[] valsA3 = {2, 6, 4};
        int[] valsB3 = {1, 5};
        ListNode[] lists3 = createIntersectingLists(valsA3, valsB3, null, 3, 2);
        
        System.out.print("ListA: ");
        printList(lists3[0], 10);
        System.out.println();
        System.out.print("ListB: ");
        printList(lists3[1], 10);
        System.out.println();
        
        ListNode result3 = finder.getIntersectionNode(lists3[0], lists3[1]);
        if (result3 != null) {
            System.out.println("Output: Intersected at '" + result3.val + "'");
        } else {
            System.out.println("Output: No intersection");
        }
        System.out.println("Explanation: The two lists do not intersect\n");
        
        System.out.println("=== Testing Different Approaches ===\n");
        
        // Test all approaches on Example 1
        System.out.println("Testing all approaches on Example 1:");
        ListNode[] testLists = createIntersectingLists(valsA1, valsB1, common1, 2, 3);
        
        ListNode res1 = finder.getIntersectionNode(testLists[0], testLists[1]);
        System.out.println("Two Pointers:  " + (res1 != null ? "Intersected at '" + res1.val + "'" : "No intersection"));
        
        testLists = createIntersectingLists(valsA1, valsB1, common1, 2, 3);
        ListNode res2 = finder.getIntersectionNodeByLength(testLists[0], testLists[1]);
        System.out.println("By Length:     " + (res2 != null ? "Intersected at '" + res2.val + "'" : "No intersection"));
        
        testLists = createIntersectingLists(valsA1, valsB1, common1, 2, 3);
        ListNode res3 = finder.getIntersectionNodeUsingHashSet(testLists[0], testLists[1]);
        System.out.println("Using HashSet: " + (res3 != null ? "Intersected at '" + res3.val + "'" : "No intersection"));
        
        System.out.println("\n=== Algorithm Explanation ===\n");
        System.out.println("Approach 1: Two Pointers (Most Elegant!) ⭐");
        System.out.println("Time: O(m + n), Space: O(1)");
        System.out.println();
        System.out.println("Key Insight:");
        System.out.println("  When pointer reaches end, redirect to other list's head");
        System.out.println("  After traversing both lists, pointers will:");
        System.out.println("    - Meet at intersection (if exists)");
        System.out.println("    - Both be null (if no intersection)");
        System.out.println();
        System.out.println("Why it works:");
        System.out.println("  ListA: a1 → a2 → c1 → c2 → c3");
        System.out.println("  ListB: b1 → b2 → b3 → c1 → c2 → c3");
        System.out.println();
        System.out.println("  Pointer A travels: a1 → a2 → c1 → c2 → c3 → b1 → b2 → b3 → c1");
        System.out.println("  Pointer B travels: b1 → b2 → b3 → c1 → c2 → c3 → a1 → a2 → c1");
        System.out.println();
        System.out.println("  Both travel same total distance: (lenA + lenB)");
        System.out.println("  They meet at intersection point!");
        System.out.println();
        System.out.println("Visual Example:");
        System.out.println();
        System.out.println("  A: 4 → 1 → 8 → 4 → 5");
        System.out.println("              ↑");
        System.out.println("  B: 5 → 6 → 1┘");
        System.out.println();
        System.out.println("  Step 1: pA=4, pB=5");
        System.out.println("  Step 2: pA=1, pB=6");
        System.out.println("  Step 3: pA=8, pB=1");
        System.out.println("  Step 4: pA=4, pB=8");
        System.out.println("  Step 5: pA=5, pB=4");
        System.out.println("  Step 6: pA=null→headB=5, pB=5");
        System.out.println("  Step 7: pA=6, pB=null→headA=4");
        System.out.println("  Step 8: pA=1, pB=1");
        System.out.println("  Step 9: pA=8, pB=8 ← MATCH!");
        System.out.println();
        System.out.println("---");
        System.out.println();
        System.out.println("Approach 2: Calculate Length Difference");
        System.out.println("Time: O(m + n), Space: O(1)");
        System.out.println();
        System.out.println("Steps:");
        System.out.println("  1. Calculate length of both lists");
        System.out.println("  2. Move longer list's pointer ahead by difference");
        System.out.println("  3. Traverse both lists together until match");
        System.out.println();
        System.out.println("---");
        System.out.println();
        System.out.println("Approach 3: Using HashSet");
        System.out.println("Time: O(m + n), Space: O(m) or O(n)");
        System.out.println();
        System.out.println("Steps:");
        System.out.println("  1. Add all nodes from first list to HashSet");
        System.out.println("  2. Traverse second list, check if node exists in set");
        System.out.println("  3. First match is the intersection");
        System.out.println();
        System.out.println("---");
        System.out.println();
        System.out.println("Comparison:");
        System.out.println("  Two Pointers:  ✓ O(1) space, ✓ elegant, ✓ best for interviews");
        System.out.println("  By Length:     ✓ O(1) space, intuitive");
        System.out.println("  HashSet:       ✗ O(n) space, but simple to understand");
        System.out.println();
        System.out.println("Important Notes:");
        System.out.println("  - Intersection is by reference, not value!");
        System.out.println("  - Two nodes with same value may not be the intersection");
        System.out.println("  - After intersection, both lists share all remaining nodes");
        System.out.println("  - Original list structure must be preserved");
    }
}
