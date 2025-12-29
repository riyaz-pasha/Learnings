import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * MERGE K SORTED LISTS (LeetCode 23) - COMPREHENSIVE GUIDE
 * 
 * Problem: Merge k sorted linked lists into one sorted linked list.
 * 
 * DIFFICULTY: Hard
 * OPTIMAL TIME: O(N log k) where N = total nodes, k = number of lists
 * OPTIMAL SPACE: O(k) for heap or O(log k) for recursion
 * 
 * KEY INSIGHT: We don't need to compare all N elements - just the k current heads!
 * At each step, we only need to find the minimum among k elements (list heads).
 * 
 * CRITICAL CONCEPTS:
 * 1. Min-Heap (Priority Queue) of size k
 * 2. Divide and Conquer (Merge Sort pattern)
 * 3. Sequential merging optimization
 * 4. In-place pointer manipulation
 * 
 * COMPANIES: Amazon, Microsoft, Google, Facebook, Apple, LinkedIn, Uber
 * 
 * RELATED PROBLEMS:
 * - Merge Two Sorted Lists (LeetCode 21)
 * - Sort K-Sorted Array
 * - Find K Pairs with Smallest Sums (LeetCode 373)
 * - Merge Sorted Array (LeetCode 88)
 * 
 * EDGE CASES:
 * 1. Empty lists array
 * 2. All null lists
 * 3. Single list
 * 4. Lists with different lengths
 * 5. Lists with duplicate values
 */
class MergeKSortedLists {
    
    // Definition for singly-linked list
    public static class ListNode {
        int val;
        ListNode next;
        
        ListNode() {}
        
        ListNode(int val) {
            this.val = val;
        }
        
        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    // ========================================================================
    // APPROACH 1: MIN-HEAP (PRIORITY QUEUE) - OPTIMAL
    // ========================================================================
    /**
     * Use a min-heap to always get the smallest element among k list heads.
     * 
     * ALGORITHM:
     * 1. Add all k list heads to min-heap
     * 2. Extract minimum node from heap
     * 3. Add this node to result
     * 4. If extracted node has next, add next to heap
     * 5. Repeat until heap is empty
     * 
     * WHY THIS WORKS:
     * - At any moment, the minimum among all remaining elements is one of k heads
     * - Heap efficiently maintains these k heads
     * - Extract min in O(log k), not O(k)
     * 
     * TIME: O(N log k)
     * - N total nodes to process
     * - Each heap operation is O(log k)
     * - Total: N × log k
     * 
     * SPACE: O(k) for heap (stores at most k nodes)
     * 
     * INTERVIEW FAVORITE:
     * This is THE expected solution for experienced candidates.
     * Shows understanding of:
     * - Heap applications
     * - Optimization via data structures
     * - Pointer manipulation
     */
    public ListNode mergeKLists(ListNode[] lists) {
        // Edge case: empty or null input
        if (lists == null || lists.length == 0) {
            return null;
        }
        
        // Min-heap based on node values
        PriorityQueue<ListNode> minHeap = new PriorityQueue<>(
            (a, b) -> Integer.compare(a.val, b.val)
        );
        
        // Step 1: Add all k list heads to heap (skip null lists)
        for (ListNode list : lists) {
            if (list != null) {
                minHeap.offer(list);
            }
        }
        
        // Dummy head for easy result construction
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;
        
        // Step 2: Extract min and add its next to heap
        while (!minHeap.isEmpty()) {
            // Get smallest node among k heads
            ListNode minNode = minHeap.poll();
            
            // Add to result
            current.next = minNode;
            current = current.next;
            
            // If this node has next, add to heap
            if (minNode.next != null) {
                minHeap.offer(minNode.next);
            }
        }
        
        return dummy.next;
    }

    // ========================================================================
    // APPROACH 2: DIVIDE AND CONQUER (MERGE SORT PATTERN)
    // ========================================================================
    /**
     * Divide k lists into pairs, merge each pair, repeat until one list remains.
     * 
     * ALGORITHM:
     * 1. Pair up k lists and merge each pair
     * 2. After first round: k/2 lists
     * 3. After second round: k/4 lists
     * 4. Continue until 1 list remains
     * 
     * VISUALIZATION (k=8):
     * Level 0: [L1] [L2] [L3] [L4] [L5] [L6] [L7] [L8]
     * Level 1:   [L1+L2]   [L3+L4]   [L5+L6]   [L7+L8]
     * Level 2:      [L1+L2+L3+L4]      [L5+L6+L7+L8]
     * Level 3:           [L1+L2+L3+L4+L5+L6+L7+L8]
     * 
     * TIME: O(N log k)
     * - log k levels (each halves the number of lists)
     * - Each level processes N total nodes
     * - Total: N × log k
     * 
     * SPACE: O(1) if iterative, O(log k) if recursive (call stack)
     * 
     * PROS:
     * + O(1) space with iterative implementation
     * + Better cache locality than heap
     * + Conceptually simple (divide and conquer)
     * 
     * CONS:
     * - More code than heap approach
     * - Requires merge two lists helper function
     */
    public ListNode mergeKListsDivideConquer(ListNode[] lists) {
        if (lists == null || lists.length == 0) {
            return null;
        }
        
        return mergeHelper(lists, 0, lists.length - 1);
    }
    
    private ListNode mergeHelper(ListNode[] lists, int left, int right) {
        // Base case: single list
        if (left == right) {
            return lists[left];
        }
        
        // Base case: two lists
        if (left + 1 == right) {
            return mergeTwoLists(lists[left], lists[right]);
        }
        
        // Divide
        int mid = left + (right - left) / 2;
        ListNode leftMerged = mergeHelper(lists, left, mid);
        ListNode rightMerged = mergeHelper(lists, mid + 1, right);
        
        // Conquer
        return mergeTwoLists(leftMerged, rightMerged);
    }

    // ========================================================================
    // APPROACH 3: DIVIDE AND CONQUER (ITERATIVE)
    // ========================================================================
    /**
     * Iterative version of divide and conquer - O(1) space.
     * 
     * ALGORITHM:
     * Repeatedly merge pairs of lists until only one remains.
     * 
     * TIME: O(N log k)
     * SPACE: O(1) - no recursion, in-place array modification
     * 
     * WHEN TO USE:
     * - When space is constrained
     * - To avoid recursion overhead
     * - When interviewer asks for O(1) space
     */
    public ListNode mergeKListsIterative(ListNode[] lists) {
        if (lists == null || lists.length == 0) {
            return null;
        }
        
        int interval = 1;
        
        // Keep merging with increasing intervals
        while (interval < lists.length) {
            // Merge pairs: (0,interval), (2*interval, 3*interval), etc.
            for (int i = 0; i < lists.length - interval; i += interval * 2) {
                lists[i] = mergeTwoLists(lists[i], lists[i + interval]);
            }
            interval *= 2;
        }
        
        return lists[0];
    }

    // ========================================================================
    // APPROACH 4: SEQUENTIAL MERGING (SIMPLE BUT SUBOPTIMAL)
    // ========================================================================
    /**
     * Merge lists one by one sequentially.
     * Merge list[0] with list[1], then result with list[2], etc.
     * 
     * TIME: O(N × k)
     * - Merge list[0] and list[1]: 2n operations
     * - Merge result and list[2]: 3n operations
     * - Merge result and list[3]: 4n operations
     * - Total: 2n + 3n + 4n + ... + kn = O(k²×n) = O(N×k) where N=total nodes
     * 
     * SPACE: O(1)
     * 
     * WHEN TO MENTION:
     * - As initial naive approach
     * - To explain why better solutions needed
     * - Shows understanding of complexity analysis
     * 
     * INTERVIEW NOTE:
     * "This works but is suboptimal at O(N×k). Can we do better?"
     */
    public ListNode mergeKListsSequential(ListNode[] lists) {
        if (lists == null || lists.length == 0) {
            return null;
        }
        
        ListNode result = lists[0];
        
        for (int i = 1; i < lists.length; i++) {
            result = mergeTwoLists(result, lists[i]);
        }
        
        return result;
    }

    // ========================================================================
    // HELPER: MERGE TWO SORTED LISTS
    // ========================================================================
    /**
     * Standard two-pointer merge of two sorted linked lists.
     * 
     * TIME: O(n + m) where n, m are list lengths
     * SPACE: O(1) - only pointers
     * 
     * CRITICAL for divide and conquer approaches.
     */
    private ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        // Edge cases
        if (l1 == null) return l2;
        if (l2 == null) return l1;
        
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;
        
        // Merge while both lists have nodes
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
        current.next = (l1 != null) ? l1 : l2;
        
        return dummy.next;
    }

    // ========================================================================
    // APPROACH 5: RECURSIVE MERGE TWO (ALTERNATIVE HELPER)
    // ========================================================================
    /**
     * Recursive version of merge two lists.
     * More elegant but uses O(n+m) space for call stack.
     * 
     * TIME: O(n + m)
     * SPACE: O(n + m) for recursion stack
     */
    private ListNode mergeTwoListsRecursive(ListNode l1, ListNode l2) {
        if (l1 == null) return l2;
        if (l2 == null) return l1;
        
        if (l1.val <= l2.val) {
            l1.next = mergeTwoListsRecursive(l1.next, l2);
            return l1;
        } else {
            l2.next = mergeTwoListsRecursive(l1, l2.next);
            return l2;
        }
    }

    // ========================================================================
    // COMMON MISTAKES & DEBUGGING TIPS
    // ========================================================================
    /**
     * CRITICAL MISTAKES:
     * 
     * 1. WRONG: Not checking for null lists when building heap
     *    RIGHT: Skip null lists when adding to heap
     *    Impact: NullPointerException
     * 
     * 2. WRONG: Forgetting to add minNode.next to heap
     *    RIGHT: After extracting node, check if it has next
     *    Impact: Missing elements in result
     * 
     * 3. WRONG: Not using dummy head
     *    RIGHT: Always use dummy head for easy result construction
     *    Reason: Simplifies code, no special case for first node
     * 
     * 4. WRONG: Comparing ListNode objects instead of values in heap
     *    RIGHT: Use comparator: (a, b) -> Integer.compare(a.val, b.val)
     *    Impact: Incorrect ordering or compilation error
     * 
     * 5. WRONG: Modifying input array destructively without intention
     *    RIGHT: Be aware that iterative D&C modifies input array
     * 
     * 6. WRONG: Off-by-one errors in divide and conquer bounds
     *    RIGHT: Carefully handle mid calculation and base cases
     * 
     * 7. WRONG: Forgetting to return dummy.next
     *    RIGHT: Result is dummy.next, not dummy
     * 
     * 8. WRONG: Not handling empty lists array
     *    RIGHT: Check if lists == null || lists.length == 0
     * 
     * DEBUGGING TIPS:
     * - Print heap contents at each step
     * - Visualize merge process with small example
     * - Check pointer assignments carefully
     * - Test with lists of different lengths
     * - Verify no cycles created (can cause infinite loops)
     */

    // ========================================================================
    // INTERVIEW COMMUNICATION STRATEGY
    // ========================================================================
    /**
     * OPTIMAL INTERVIEW FLOW (30-35 minutes):
     * 
     * PHASE 1: CLARIFICATION (2-3 min)
     * Questions to ask:
     * - "Are all lists sorted in ascending order?" (YES)
     * - "Can lists be empty or null?" (YES)
     * - "Should I handle cycles in lists?" (Usually NO, assume valid)
     * - "Can I modify the input lists?" (Usually YES)
     * - "Are values unique or can there be duplicates?" (Can be duplicates)
     * 
     * PHASE 2: EXAMPLES (2-3 min)
     * Work through example:
     * lists = [[1,4,5], [1,3,4], [2,6]]
     * 
     * Initial heap: [1@L0, 1@L1, 2@L2]
     * Extract 1@L0, add 4@L0: heap = [1@L1, 2@L2, 4@L0]
     * Extract 1@L1, add 3@L1: heap = [2@L2, 3@L1, 4@L0]
     * Extract 2@L2, add 6@L2: heap = [3@L1, 4@L0, 6@L2]
     * ...and so on
     * 
     * Result: 1->1->2->3->4->4->5->6
     * 
     * PHASE 3: APPROACH DISCUSSION (3-5 min)
     * 
     * Naive approach:
     * "Could merge sequentially: merge first two, then result with third, etc.
     * But this is O(N×k) because we process early elements multiple times."
     * 
     * Better approaches:
     * 1. "Min-heap keeps k list heads, extract min in O(log k) → O(N log k)"
     * 2. "Divide and conquer like merge sort → O(N log k)"
     * 
     * Choose approach:
     * "I'll use min-heap - clean, efficient, and easy to explain."
     * OR
     * "I'll use divide and conquer for O(1) space."
     * 
     * PHASE 4: COMPLEXITY ANALYSIS (1-2 min)
     * Min-Heap:
     * - Time: O(N log k) - N nodes, each operation O(log k)
     * - Space: O(k) - heap stores k nodes
     * 
     * Divide & Conquer:
     * - Time: O(N log k) - log k levels, N work per level
     * - Space: O(log k) recursive or O(1) iterative
     * 
     * PHASE 5: CODING (18-20 min)
     * - Start with data structure definition (ListNode)
     * - Write main merge function
     * - Write helper functions (merge two lists)
     * - Handle edge cases
     * - Add comments for clarity
     * 
     * PHASE 6: TESTING (3-5 min)
     * Test cases:
     * 1. Given example
     * 2. Empty array: [] → null
     * 3. Single list: [[1,2,3]] → 1->2->3
     * 4. All nulls: [null, null] → null
     * 5. Different lengths: [[1], [1,3,4], [2,6]] → 1->1->2->3->4->6
     * 6. Duplicates: [[1,1], [1,1]] → 1->1->1->1
     * 
     * PHASE 7: OPTIMIZATION (if time)
     * - Discuss iterative D&C for O(1) space
     * - Mention when sequential might be acceptable (k very small)
     * - Discuss tradeoffs between approaches
     */

    // ========================================================================
    // KEY INSIGHTS & PATTERNS
    // ========================================================================
    /**
     * PATTERN RECOGNITION:
     * 
     * 1. K-WAY MERGE PATTERN:
     *    - Have k sorted sequences
     *    - Need to merge into one sequence
     *    - Min-heap of size k is standard approach
     *    - Examples: Merge k sorted arrays, Find k pairs, External sort
     * 
     * 2. DIVIDE AND CONQUER ON K:
     *    - Reduce k by half each iteration
     *    - log k iterations total
     *    - Each iteration processes all N elements
     *    - Result: O(N log k)
     * 
     * 3. WHY NOT SEQUENTIAL:
     *    - First merge: 2 lists
     *    - Second merge: result + 1 list (growing result)
     *    - Third merge: bigger result + 1 list
     *    - Early elements processed k times → O(N×k)
     * 
     * 4. HEAP VS DIVIDE AND CONQUER:
     *    Heap:
     *    ✓ Cleaner code
     *    ✓ Easier to explain
     *    ✗ O(k) space
     *    
     *    Divide & Conquer:
     *    ✓ O(1) space (iterative)
     *    ✓ Better cache locality
     *    ✗ More code
     *    ✗ Requires helper function
     * 
     * 5. DUMMY HEAD PATTERN:
     *    - Always use dummy head for list construction
     *    - Avoids special casing first element
     *    - Return dummy.next at end
     */

    // ========================================================================
    // RELATED PROBLEMS & VARIATIONS
    // ========================================================================
    /**
     * LEETCODE PROBLEMS USING SAME PATTERN:
     * 
     * 1. LeetCode 21 - Merge Two Sorted Lists
     *    - Base case of this problem
     *    - O(n+m) solution
     * 
     * 2. LeetCode 88 - Merge Sorted Array
     *    - Similar idea but with arrays
     *    - Can do in-place from back
     * 
     * 3. LeetCode 373 - Find K Pairs with Smallest Sums
     *    - K-way merge of sequences
     *    - Use min-heap with pairs
     * 
     * 4. LeetCode 378 - Kth Smallest Element in Sorted Matrix
     *    - Each row is sorted list
     *    - Min-heap to merge rows
     * 
     * 5. LeetCode 632 - Smallest Range Covering Elements from K Lists
     *    - Advanced k-way merge
     *    - Use heap to track current elements
     * 
     * VARIATIONS:
     * 
     * 1. MERGE K SORTED ARRAYS:
     *    - Same approach with arrays instead of lists
     *    - Track indices instead of next pointers
     * 
     * 2. FIND MEDIAN OF K SORTED LISTS:
     *    - Merge to find middle element(s)
     *    - Can optimize to stop at N/2
     * 
     * 3. MERGE K DESCENDING LISTS:
     *    - Use max-heap instead of min-heap
     *    - Or reverse lists first
     * 
     * 4. MERGE WITH K LIMIT:
     *    - Only merge first k elements total
     *    - Extract k times from heap
     * 
     * 5. MERGE K DOUBLY LINKED LISTS:
     *    - Maintain both next and prev pointers
     *    - More pointer management
     */

    // ========================================================================
    // COMPLEXITY ANALYSIS COMPARISON
    // ========================================================================
    /**
     * ┌────────────────────┬──────────────┬──────────────┬─────────────┐
     * │ Approach           │ Time         │ Space        │ Interview   │
     * ├────────────────────┼──────────────┼──────────────┼─────────────┤
     * │ Min-Heap           │ O(N log k)   │ O(k)         │ ⭐⭐⭐⭐⭐  │
     * │ D&C Recursive      │ O(N log k)   │ O(log k)     │ ⭐⭐⭐⭐☆  │
     * │ D&C Iterative      │ O(N log k)   │ O(1)         │ ⭐⭐⭐⭐⭐  │
     * │ Sequential         │ O(N × k)     │ O(1)         │ ⭐⭐☆☆☆  │
     * └────────────────────┴──────────────┴──────────────┴─────────────┘
     * 
     * WHERE:
     * - N = total number of nodes across all lists
     * - k = number of lists
     * 
     * WHEN TO USE WHICH:
     * 
     * Default choice: Min-Heap
     * ✓ Clean, easy to explain
     * ✓ Optimal time complexity
     * ✓ Acceptable space for most cases
     * 
     * Use D&C Iterative when:
     * ✓ Space is critical (O(1) required)
     * ✓ Want better cache performance
     * ✓ Comfortable with more complex code
     * 
     * Use Sequential when:
     * ✓ k is very small (k ≤ 3)
     * ✓ Simplicity matters more than efficiency
     * ✓ For comparison/baseline only
     */

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    // Helper: Create list from array
    public static ListNode createList(int[] values) {
        if (values == null || values.length == 0) {
            return null;
        }
        
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;
        
        for (int val : values) {
            current.next = new ListNode(val);
            current = current.next;
        }
        
        return dummy.next;
    }
    
    // Helper: Convert list to string
    public static String listToString(ListNode head) {
        if (head == null) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        while (head != null) {
            sb.append(head.val);
            if (head.next != null) {
                sb.append(",");
            }
            head = head.next;
        }
        sb.append("]");
        return sb.toString();
    }
    
    // Helper: Compare two lists
    public static boolean areEqual(ListNode l1, ListNode l2) {
        while (l1 != null && l2 != null) {
            if (l1.val != l2.val) {
                return false;
            }
            l1 = l1.next;
            l2 = l2.next;
        }
        return l1 == null && l2 == null;
    }

    // ========================================================================
    // TEST CASES & VALIDATION
    // ========================================================================
    public static void main(String[] args) {
        MergeKSortedLists solution = new MergeKSortedLists();
        
        System.out.println("=".repeat(70));
        System.out.println("MERGE K SORTED LISTS - COMPREHENSIVE TEST SUITE");
        System.out.println("=".repeat(70));
        
        // Test Case 1: Standard example
        System.out.println("\nTest 1: Standard Example");
        ListNode[] lists1 = {
            createList(new int[]{1, 4, 5}),
            createList(new int[]{1, 3, 4}),
            createList(new int[]{2, 6})
        };
        ListNode result1 = solution.mergeKLists(lists1);
        System.out.println("Result: " + listToString(result1));
        System.out.println("Expected: [1,1,2,3,4,4,5,6]");
        
        // Test Case 2: Empty array
        System.out.println("\nTest 2: Empty Array");
        ListNode[] lists2 = {};
        ListNode result2 = solution.mergeKLists(lists2);
        System.out.println("Result: " + listToString(result2));
        System.out.println("Expected: []");
        
        // Test Case 3: Array with empty list
        System.out.println("\nTest 3: Array with Empty List");
        ListNode[] lists3 = {null};
        ListNode result3 = solution.mergeKLists(lists3);
        System.out.println("Result: " + listToString(result3));
        System.out.println("Expected: []");
        
        // Test Case 4: Single list
        System.out.println("\nTest 4: Single List");
        ListNode[] lists4 = {createList(new int[]{1, 2, 3})};
        ListNode result4 = solution.mergeKLists(lists4);
        System.out.println("Result: " + listToString(result4));
        System.out.println("Expected: [1,2,3]");
        
        // Test Case 5: Lists with different lengths
        System.out.println("\nTest 5: Different Lengths");
        ListNode[] lists5 = {
            createList(new int[]{1}),
            createList(new int[]{1, 3, 4}),
            createList(new int[]{2, 6})
        };
        ListNode result5 = solution.mergeKLists(lists5);
        System.out.println("Result: " + listToString(result5));
        System.out.println("Expected: [1,1,2,3,4,6]");
        
        // Test Case 6: All duplicates
        System.out.println("\nTest 6: All Duplicates");
        ListNode[] lists6 = {
            createList(new int[]{1, 1}),
            createList(new int[]{1, 1}),
            createList(new int[]{1, 1})
        };
        ListNode result6 = solution.mergeKLists(lists6);
        System.out.println("Result: " + listToString(result6));
        System.out.println("Expected: [1,1,1,1,1,1]");
        
        // Test Case 7: Mix of null and non-null
        System.out.println("\nTest 7: Mix of Null and Non-null");
        ListNode[] lists7 = {
            null,
            createList(new int[]{1}),
            null,
            createList(new int[]{2}),
            null
        };
        ListNode result7 = solution.mergeKLists(lists7);
        System.out.println("Result: " + listToString(result7));
        System.out.println("Expected: [1,2]");
        
        // Compare all approaches
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPARING ALL APPROACHES");
        System.out.println("=".repeat(70));
        
        ListNode[] testLists = {
            createList(new int[]{1, 4, 5}),
            createList(new int[]{1, 3, 4}),
            createList(new int[]{2, 6})
        };
        
        ListNode[] lists_heap = cloneLists(testLists);
        ListNode[] lists_dc = cloneLists(testLists);
        ListNode[] lists_iter = cloneLists(testLists);
        ListNode[] lists_seq = cloneLists(testLists);
        
        ListNode r_heap = solution.mergeKLists(lists_heap);
        ListNode r_dc = solution.mergeKListsDivideConquer(lists_dc);
        ListNode r_iter = solution.mergeKListsIterative(lists_iter);
        ListNode r_seq = solution.mergeKListsSequential(lists_seq);
        
        System.out.println("Min-Heap:         " + listToString(r_heap));
        System.out.println("Divide & Conquer: " + listToString(r_dc));
        System.out.println("Iterative D&C:    " + listToString(r_iter));
        System.out.println("Sequential:       " + listToString(r_seq));
        
        boolean allMatch = areEqual(r_heap, r_dc) && 
                          areEqual(r_dc, r_iter) && 
                          areEqual(r_iter, r_seq);
        System.out.println("All Match: " + allMatch);
        
        // Performance comparison
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PERFORMANCE COMPARISON (100 lists, 100 nodes each)");
        System.out.println("=".repeat(70));
        
        int numLists = 100;
        int nodesPerList = 100;
        ListNode[] perfLists = new ListNode[numLists];
        Random rand = new Random(42);
        
        for (int i = 0; i < numLists; i++) {
            int[] values = new int[nodesPerList];
            for (int j = 0; j < nodesPerList; j++) {
                values[j] = rand.nextInt(1000);
            }
            Arrays.sort(values);
            perfLists[i] = createList(values);
        }
        
        long start, end;
        
        ListNode[] lists_heap_perf = cloneLists(perfLists);
        start = System.nanoTime();
        solution.mergeKLists(lists_heap_perf);
        end = System.nanoTime();
        System.out.println("Min-Heap:      " + (end - start) / 1_000_000 + " ms");
        
        ListNode[] lists_dc_perf = cloneLists(perfLists);
        start = System.nanoTime();
        solution.mergeKListsDivideConquer(lists_dc_perf);
        end = System.nanoTime();
        System.out.println("D&C Recursive: " + (end - start) / 1_000_000 + " ms");
        
        ListNode[] lists_iter_perf = cloneLists(perfLists);
        start = System.nanoTime();
        solution.mergeKListsIterative(lists_iter_perf);
        end = System.nanoTime();
        System.out.println("D&C Iterative: " + (end - start) / 1_000_000 + " ms");
        
        System.out.println("\n" + "=".repeat(70));
    }
    
    // Helper: Clone array of lists
    private static ListNode[] cloneLists(ListNode[] lists) {
        ListNode[] cloned = new ListNode[lists.length];
        for (int i = 0; i < lists.length; i++) {
            cloned[i] = cloneList(lists[i]);
        }
        return cloned;
    }
    
    // Helper: Clone single list
    private static ListNode cloneList(ListNode head) {
        if (head == null) return null;
        
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;
        
        while (head != null) {
            current.next = new ListNode(head.val);
            current = current.next;
            head = head.next;
        }
        
        return dummy.next;
    }
}

/**
 * ============================================================================
 * FINAL INTERVIEW CHECKLIST
 * ============================================================================
 * 
 * BEFORE CODING:
 * □ Clarified input format and constraints
 * □ Asked about null/empty lists
 * □ Discussed whether to modify input
 * □ Explained chosen approach (heap or D&C)
 * □ Stated time and space complexity
 * □ Drew diagram of algorithm
 * 
 * WHILE CODING:
 * □ Defined ListNode class if needed
 * □ Created min-heap with proper comparator
 * □ Used dummy head for result construction
 * □ Checked for null before adding to heap
 * □ Added next node after extracting from heap
 * □ Returned dummy.next not dummy
 * □ Handled empty input array
 * □ Added meaningful comments
 * 
 * AFTER CODING:
 * □ Traced through example with small input
 * □ Tested with empty/null cases
 * □ Verified no pointer errors
 * □ Confirmed time/space complexity
 * □ Discussed alternative approaches
 * □ Mentioned tradeoffs
 * 
 * KEY TALKING POINTS:
 * ✓ "Min-heap maintains k list heads, extract min in O(log k)"
 * ✓ "Total N nodes, each processed once → O(N log k)"
 * ✓ "Alternative: Divide and conquer like merge sort"
 * ✓ "Sequential merging is O(N×k) because we reprocess elements"
 * ✓ "Dummy head simplifies list construction"
 * 
 * COMMON PITFALLS AVOIDED:
 * ✗ Forgetting to skip null lists when building heap
 * ✗ Not adding next node after extracting from heap
 * ✗ Using wrong comparator (comparing nodes instead of values)
 * ✗ Returning dummy instead of dummy.next
 * ✗ Not handling empty input array
 * 
 * ============================================================================
 */
