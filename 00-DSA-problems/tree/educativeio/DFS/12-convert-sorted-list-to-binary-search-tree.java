import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * STUDY NOTE: CONVERT SORTED LIST TO BINARY SEARCH TREE
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "Are we allowed to modify or destroy the original linked list pointers?" 
 *   (Crucial: The top-down fast/slow pointer method usually severs the `next` links to isolate sub-lists. If the list must remain intact, we must either repair it, pass boundaries, or use another method).
 * - "Are we allowed to allocate O(N) extra memory, like dumping the list into an array?" 
 *   (Determines if the brute-force array conversion is acceptable, which is often easier to write but fails strict space-complexity constraints).
 * - "Do we know the exact length of the linked list upfront?" 
 *   (If yes, we can instantly use the optimal O(N) bottom-up approach without needing an initial O(N) pre-pass to count the nodes).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * To build a height-balanced BST, the root must be the exact middle element of the sorted data. 
 * For arrays, finding the middle is O(1). For a linked list, finding the middle requires an O(N) 
 * sequential scan. The bottleneck is how we repeatedly find the middle for subtrees without 
 * tanking the time complexity to quadratic levels.
 *
 * Approach 1: Dump to Array (The Brute Force / Extra Space)
 * What I'd naturally try: I know how to convert a sorted array to a BST (just grab `array[mid]`, 
 * then recurse on left and right halves). So, I'll just iterate through the linked list, copy 
 * every integer into a Java `ArrayList`, and then build the BST from the list.
 * Why it works: Arrays allow O(1) random access to the middle element.
 * Why it's costly: We are given a Linked List specifically to constrain our memory footprint.
 * - Time Complexity: O(N) — because reading the list takes O(N), and building the tree takes O(N).
 * - Space Complexity: O(N) — because we allocate a brand new Array/List of size N, completely 
 *   missing the point of the space-constrained Linked List input.
 * 
 * Approach 2: Fast & Slow Pointers (The Standard Interview Choice)
 * What I'd naturally try: I shouldn't use extra arrays. I can find the middle of a linked list 
 * in-place using a slow pointer (moves 1 step) and a fast pointer (moves 2 steps). When `fast` 
 * hits the end, `slow` is at the middle. I make `slow` the root, CUT the linked list in half by 
 * setting the node before `slow` to null, and recurse on the left and right halves.
 * Why it works: It perfectly mirrors the Array approach but uses in-place pointer manipulation.
 * Why it's slightly slow: We do redundant scanning. To find the middle of N, we scan N/2 nodes. 
 * To find the middles of the two halves, we scan N/4 nodes twice, etc.
 * - Time Complexity: O(N log N) — because at each level of the recursion tree (which is log N levels deep), 
 *   we spend O(N) combined time finding the middles via fast/slow pointers.
 * - Space Complexity: O(log N) — because we only use the recursive call stack (no arrays), which 
 *   is strictly bounded by the height of the balanced BST.
 *
 * Approach 3: Bottom-Up In-Order Simulation (The O(N) Masterpiece)
 * What I'd naturally try: Wait... an In-Order traversal (Left -> Node -> Right) of a BST processes 
 * nodes in exact sorted order. Our Linked List is ALSO in exact sorted order! What if we build the 
 * tree *in the same order* we traverse the list?
 * Why it works: We first count the total nodes (N). We pretend we are building a balanced tree of 
 * size N. We recursively go as deep Left as possible. When we hit a null leaf, we pop back up, take 
 * the CURRENT node from our global linked list pointer, make it a TreeNode, advance the global pointer, 
 * and then recurse Right. The tree magically pieces itself together from the bottom up!
 * What property removes the bottleneck: By advancing a single global pointer concurrently with an 
 * In-Order construction, we never have to search for the middle. The recursion implicitly finds it.
 * - Time Complexity: O(N) — because we do one pass to count (O(N)), and one pass to build (O(N)).
 * - Space Complexity: O(log N) — because we only use the balanced recursion stack.
 *
 * The Interview Choice:
 * Write Approach 2 (Fast/Slow Pointers). It is the most universally expected answer for this specific 
 * problem, demonstrating linked-list pointer mastery (especially the vital step of severing the list). 
 * Discuss Approach 3 verbally to show elite algorithmic awareness, but only code it if the interviewer 
 * explicitly demands an O(N) time optimization.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Empty List (head == null) -> Returns null.
 * 2. Single Node -> Returns a single TreeNode. Fast/slow logic must not crash.
 * 3. Two Nodes (e.g., 1 -> 2) -> Root is 2, left is 1 (or root 1, right 2). Both are valid height-balanced trees.
 *
 *
 * 4. DRY RUN (Optimal Bottom-Up In-Order)
 * ----------------------------------------------------------------------------
 * List: 1 -> 2 -> 3
 * Count = 3. Global Pointer `curr` points to `1`.
 * Call build(start=0, end=2)
 *
 * 1. build(0, 2): mid = 1.
 *    - Calls left = build(0, 0)
 * 
 * 2. build(0, 0): mid = 0.
 *    - Calls left = build(0, -1) -> returns null.
 *    - CREATE NODE: value = curr.val (1). `curr` advances to `2`.
 *    - Calls right = build(1, 0) -> returns null.
 *    - Returns Node(1) to parent.
 *
 * 3. Back in build(0, 2):
 *    - left is Node(1).
 *    - CREATE NODE: value = curr.val (2). `curr` advances to `3`. (Notice how 2 is perfectly the root!)
 *    - Root(2).left = Node(1).
 *    - Calls right = build(2, 2)
 *
 * 4. build(2, 2): mid = 2.
 *    - Calls left = build(2, 1) -> returns null.
 *    - CREATE NODE: value = curr.val (3). `curr` advances to null.
 *    - Calls right = build(3, 2) -> returns null.
 *    - Returns Node(3) to parent.
 *
 * 5. Back in build(0, 2):
 *    - right is Node(3).
 *    - Root(2).right = Node(3).
 *    - Returns Root(2).
 * 
 * Final Tree:
 *       2
 *      / \
 *     1   3
 *
 * Pitfalls: 
 * - In Approach 2 (Fast/Slow): Forgetting to track the `prev` node before `slow`. 
 *   If you don't do `prev.next = null;`, the left recursive call will process the entire original 
 *   list, resulting in an infinite StackOverflow loop!
 * - In Approach 3 (Bottom-Up): Trying to pass the `curr` pointer as a method argument. In Java, 
 *   object references passed by value cannot be reassigned (e.g., `curr = curr.next`) in a way that 
 *   the parent stack frame sees. You MUST use an instance variable or a 1-element array.
 *
 * Pattern Recognition: 
 * "When converting any Sorted sequence to a Balanced BST -> The middle element is ALWAYS the root."
 * "When sequential access is slow (Linked List) but structural shape is known -> Think Bottom-Up In-Order."
 *
 * Interview Script:
 * "To balance a BST, the middle of the sorted list must be the root. I could dump this into an array 
 * for O(1) access to the middle, but that costs O(N) space. Instead, I'll use a slow and fast pointer 
 * to find the middle in-place. I'll make the slow pointer the root, sever the list immediately before it, 
 * and recursively do the same for the left and right halves. This takes O(N log N) time due to repeated 
 * scanning, and O(log N) space for the balanced call stack."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: Can we achieve O(N) time without the Bottom-Up approach?
 * A1: Only if we compromise on space and dump the Linked List into an Array or ArrayList first.
 *
 * Q2: What if we were given an unbalanced BST and asked to balance it?
 * A2: We would do an In-Order traversal of the unbalanced BST to extract its elements into an array 
 *     (which naturally sorts them), and then apply the standard `sortedArrayToBST` recursive logic.
 *
 * Q3: What if this was a Doubly Linked List?
 * A3: Finding the middle is slightly cleaner (one pointer from head, one from tail, moving inward until 
 *     they meet), but it still takes O(N) time per split, meaning top-down is still O(N log N). The 
 *     optimal bottom-up approach remains unchanged.
 */

public class SortedListToBSTStudy {

    // Definition for singly-linked list.
    public static class ListNode {
        int val;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    // Definition for a binary tree node.
    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode() {}
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    /**
     * APPROACH 2: FAST & SLOW POINTERS (The Standard Expected Interview Solution)
     * Time: O(N log N) | Space: O(log N)
     */
    public TreeNode sortedListToBST(ListNode head) {
        // Base case: Empty list
        if (head == null) {
            return null;
        }
        
        // Base case: Single node. (Crucial, otherwise fast/slow logic infinite loops)
        if (head.next == null) {
            return new TreeNode(head.val);
        }

        // Fast & Slow pointer setup
        ListNode prev = null;
        ListNode slow = head;
        ListNode fast = head;

        // E.g., for 1 -> 2 -> 3 -> 4 -> 5
        // fast moves 2 steps, slow moves 1 step. 
        // When fast reaches end, slow will be at 3 (the perfect middle).
        while (fast != null && fast.next != null) {
            prev = slow;
            slow = slow.next;
            fast = fast.next.next;
        }

        // SEVER THE LIST: This is the most forgotten step!
        // We must detach the left half from the root (slow), otherwise 
        // the left recursion will traverse into the right half.
        if (prev != null) {
            prev.next = null;
        }

        // The middle element becomes the root
        TreeNode root = new TreeNode(slow.val);

        // Recursively build the left and right subtrees
        // Left half starts at original head
        root.left = sortedListToBST(head);
        
        // Right half starts immediately after the slow pointer
        root.right = sortedListToBST(slow.next);

        return root;
    }

    /**
     * APPROACH 3: BOTTOM-UP IN-ORDER (The O(N) Masterpiece)
     * Time: O(N) | Space: O(log N)
     */
    private ListNode globalCurrent; // Instance variable tracks our progress through the LL

    public TreeNode sortedListToBSTOptimal(ListNode head) {
        if (head == null) return null;
        
        // 1. O(N) pre-pass to find the exact size of the list
        int size = 0;
        ListNode runner = head;
        while (runner != null) {
            size++;
            runner = runner.next;
        }
        
        // 2. Initialize global pointer and begin In-Order construction
        globalCurrent = head;
        return buildBottomUp(0, size - 1);
    }

    private TreeNode buildBottomUp(int start, int end) {
        // Base case: crossed boundaries mean we hit a null leaf
        if (start > end) {
            return null;
        }

        int mid = start + (end - start) / 2;

        // 1. Build the Left Subtree FIRST
        // This recursively dives all the way to the leftmost leaf before doing anything.
        TreeNode leftChild = buildBottomUp(start, mid - 1);

        // 2. Process the Root (Node)
        // By the time we reach here, globalCurrent is perfectly aligned with the correct sorted element!
        TreeNode root = new TreeNode(globalCurrent.val);
        root.left = leftChild;
        
        // Advance the list pointer for the next calls
        globalCurrent = globalCurrent.next;

        // 3. Build the Right Subtree
        root.right = buildBottomUp(mid + 1, end);

        return root;
    }

    /**
     * Helper method to verify trees in main()
     */
    private static void printPreOrder(TreeNode node) {
        if (node == null) return;
        System.out.print(node.val + " ");
        printPreOrder(node.left);
        printPreOrder(node.right);
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        SortedListToBSTStudy study = new SortedListToBSTStudy();

        // Helper to create list: [-10, -3, 0, 5, 9]
        ListNode head1 = new ListNode(-10, 
                         new ListNode(-3, 
                         new ListNode(0, 
                         new ListNode(5, 
                         new ListNode(9)))));
                         
        System.out.println("Standard Case [-10, -3, 0, 5, 9]:");
        
        // Fast/Slow Method (Mutates original list, so we can only use head1 once)
        TreeNode root1 = study.sortedListToBST(head1);
        System.out.print("Fast/Slow PreOrder: ");
        printPreOrder(root1); // Expected: 0 -10 -3 5 9 (or similar balanced shape)
        System.out.println();

        // Recreate list for Optimal Method
        ListNode head2 = new ListNode(-10, 
                         new ListNode(-3, 
                         new ListNode(0, 
                         new ListNode(5, 
                         new ListNode(9)))));

        TreeNode root2 = study.sortedListToBSTOptimal(head2);
        System.out.print("Optimal PreOrder:   ");
        printPreOrder(root2);
        System.out.println();

        // Edge Case: 2 Nodes [1, 2]
        ListNode head3 = new ListNode(1, new ListNode(2));
        System.out.print("\nTwo Nodes PreOrder (Optimal): ");
        printPreOrder(study.sortedListToBSTOptimal(head3)); // Expected: 1 2 (or 2 1)
        System.out.println();
        
        // Edge Case: 1 Node [42]
        ListNode head4 = new ListNode(42);
        System.out.print("Single Node PreOrder: ");
        printPreOrder(study.sortedListToBST(head4)); // Expected: 42
        System.out.println();
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Top-Down Fast/Slow Pointers OR Bottom-Up In-Order Simulation.
 * Key Observation: A height-balanced BST requires the exact middle of a sequence to be the root.
 * Memorize: `prev.next = null;` when using fast/slow pointers to split a linked list. 
 *           Forgetting this causes infinite recursion.
 * Most Common Trap: Passing the Linked List node as a parameter in the bottom-up approach 
 *                   and expecting `node = node.next` to persist across recursive frames. 
 *                   You MUST use an instance variable for the global pointer.
 * One-Line Mental Trigger: "List to BST? Find mid with slow/fast, sever list, recurse left/right."
 * ============================================================================
 */


import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * CONVERT SORTED LINKED LIST TO HEIGHT-BALANCED BST
 * ============================================================================
 *
 * Given:
 *
 *      A sorted singly linked list
 *
 *          -10 -> -3 -> 0 -> 5 -> 9
 *
 * Convert it into a height-balanced Binary Search Tree.
 *
 *
 * One possible answer:
 *
 *              0
 *             / \
 *           -3   9
 *           /   /
 *         -10   5
 *
 *
 * ============================================================================
 * IMPORTANT OBSERVATION
 * ============================================================================
 *
 * The input is already sorted.
 *
 * For a Binary Search Tree:
 *
 *      values smaller than root -> LEFT
 *      values larger than root  -> RIGHT
 *
 * Therefore, the natural choice for the root is the MIDDLE element.
 *
 *
 * Example:
 *
 *      -10  -3   0   5   9
 *               ^
 *             middle
 *
 *             0
 *            / \
 *         -10   9
 *           ...
 *
 *
 * Why the middle?
 *
 * Because choosing the middle gives approximately the same number of
 * nodes to the left and right.
 *
 * That is exactly what we need for a HEIGHT-BALANCED tree.
 *
 *
 * ============================================================================
 * APPROACHES
 * ============================================================================
 *
 * 1. Convert linked list to ArrayList
 *      Time:  O(N)
 *      Space: O(N)
 *
 * 2. Find middle using slow/fast pointers for every subtree
 *      Time:  O(N log N)
 *      Space: O(log N)
 *
 * 3. In-order simulation
 *      Time:  O(N)
 *      Space: O(log N)
 *
 * We'll implement all three.
 *
 * ============================================================================
 */
public class SortedListToBalancedBST {

    // ========================================================================
    // LINKED LIST NODE
    // ========================================================================

    static class ListNode {

        int value;
        ListNode next;

        ListNode(int value) {
            this.value = value;
        }
    }

    // ========================================================================
    // BINARY TREE NODE
    // ========================================================================

    static class TreeNode {

        int value;
        TreeNode left;
        TreeNode right;

        TreeNode(int value) {
            this.value = value;
        }
    }


    // ========================================================================
    // APPROACH 1: LINKED LIST -> ARRAY -> BST
    // ========================================================================

    /**
     * Simple and easy-to-understand solution.
     *
     * ------------------------------------------------------------------------
     * IDEA
     * ------------------------------------------------------------------------
     *
     * First convert:
     *
     *      Linked List
     *
     * into:
     *
     *      ArrayList
     *
     * Now we can access the middle element in O(1).
     *
     *
     * Example:
     *
     *      -10 -> -3 -> 0 -> 5 -> 9
     *
     * becomes:
     *
     *      [-10, -3, 0, 5, 9]
     *
     * Then recursively choose the middle element.
     *
     *
     * ------------------------------------------------------------------------
     * WHY DOES THIS PRODUCE A BST?
     * ------------------------------------------------------------------------
     *
     * Suppose:
     *
     *      [-10, -3, 0, 5, 9]
     *
     * middle = 0
     *
     * Everything before 0 is smaller:
     *
     *      [-10, -3]
     *
     * Everything after 0 is larger:
     *
     *      [5, 9]
     *
     * So:
     *
     *              0
     *             / \
     *        [-10,-3] [5,9]
     *
     * recursively becomes two BSTs.
     *
     *
     * ------------------------------------------------------------------------
     * COMPLEXITY
     * ------------------------------------------------------------------------
     *
     * Converting list to array:
     *      O(N)
     *
     * Building tree:
     *      O(N)
     *
     * Total:
     *      O(N)
     *
     * Extra space:
     *      O(N) for the array
     *      O(log N) recursion stack
     *
     * Overall:
     *      O(N)
     */
    public TreeNode sortedListToBSTUsingArray(ListNode head) {

        List<Integer> sortedValues = new ArrayList<>();

        /*
         * Traverse the linked list once and copy its values into an array.
         */
        ListNode currentNode = head;

        while (currentNode != null) {
            sortedValues.add(currentNode.value);
            currentNode = currentNode.next;
        }

        /*
         * Build a balanced BST from the sorted array.
         */
        return buildBalancedBST(
                sortedValues,
                0,
                sortedValues.size() - 1
        );
    }

    /**
     * Builds a balanced BST from sorted array values.
     *
     * [leftIndex ... rightIndex]
     */
    private TreeNode buildBalancedBST(
            List<Integer> sortedValues,
            int leftIndex,
            int rightIndex) {

        /*
         * No values remain in this range.
         */
        if (leftIndex > rightIndex) {
            return null;
        }

        /*
         * Choose the middle element as the root.
         *
         * Using:
         *
         *      left + (right - left) / 2
         *
         * avoids integer overflow that can occur with:
         *
         *      (left + right) / 2
         */
        int middleIndex =
                leftIndex + (rightIndex - leftIndex) / 2;

        TreeNode root =
                new TreeNode(sortedValues.get(middleIndex));

        /*
         * Everything before middle belongs to the left subtree.
         */
        root.left = buildBalancedBST(
                sortedValues,
                leftIndex,
                middleIndex - 1
        );

        /*
         * Everything after middle belongs to the right subtree.
         */
        root.right = buildBalancedBST(
                sortedValues,
                middleIndex + 1,
                rightIndex
        );

        return root;
    }


    // ========================================================================
    // APPROACH 2: FAST + SLOW POINTER
    // ========================================================================

    /**
     * Avoid converting the linked list to an array.
     *
     * ------------------------------------------------------------------------
     * IDEA
     * ------------------------------------------------------------------------
     *
     * For every linked-list range:
     *
     *      1. Find the middle node.
     *      2. Make it the root.
     *      3. Recursively build the left half.
     *      4. Recursively build the right half.
     *
     *
     * Fast/slow pointers let us find the middle.
     *
     *      slow -> moves one step
     *      fast -> moves two steps
     *
     * When fast reaches the end:
     *
     *      slow is approximately at the middle.
     *
     *
     * Example:
     *
     *      1 -> 2 -> 3 -> 4 -> 5
     *                ^
     *               slow
     *
     * So 3 becomes the root.
     *
     *
     * ------------------------------------------------------------------------
     * COMPLEXITY
     * ------------------------------------------------------------------------
     *
     * At every recursive level we scan the remaining list to find its middle.
     *
     * Level 1: O(N)
     * Level 2: O(N)
     * Level 3: O(N)
     * ...
     *
     * Number of levels = O(log N)
     *
     * Therefore:
     *
     *      Time:  O(N log N)
     *      Space: O(log N)
     *
     * This is better in space than the array solution but slower in time.
     */
    public TreeNode sortedListToBSTUsingMiddle(
            ListNode head) {

        /*
         * Empty list -> empty tree.
         */
        if (head == null) {
            return null;
        }

        /*
         * Single node -> this node becomes the tree root.
         */
        if (head.next == null) {
            return new TreeNode(head.value);
        }

        /*
         * Find the middle node.
         *
         * We also need the node BEFORE the middle because the linked list
         * is singly linked.
         */
        ListNode middleNode = findMiddleNode(head);

        /*
         * The middle node becomes the root.
         */
        TreeNode root = new TreeNode(middleNode.value);

        /*
         * Everything before middle belongs to the left subtree.
         *
         * To make the left half independent, we need to disconnect it
         * from the middle node.
         */
        ListNode leftSubtreeHead = head;

        /*
         * Find the node immediately before middle.
         */
        ListNode nodeBeforeMiddle = head;

        while (nodeBeforeMiddle.next != middleNode) {
            nodeBeforeMiddle = nodeBeforeMiddle.next;
        }

        /*
         * Disconnect:
         *
         *      left half -> middle
         *
         * becomes:
         *
         *      left half -> null
         */
        nodeBeforeMiddle.next = null;

        /*
         * Recursively build left and right subtrees.
         */
        root.left =
                sortedListToBSTUsingMiddle(leftSubtreeHead);

        root.right =
                sortedListToBSTUsingMiddle(middleNode.next);

        return root;
    }

    /**
     * Finds the middle node using slow/fast pointers.
     *
     * slow moves one node at a time.
     * fast moves two nodes at a time.
     *
     * When fast reaches the end, slow is at the middle.
     */
    private ListNode findMiddleNode(ListNode head) {

        ListNode slowPointer = head;
        ListNode fastPointer = head;

        while (fastPointer.next != null
                && fastPointer.next.next != null) {

            slowPointer = slowPointer.next;
            fastPointer = fastPointer.next.next;
        }

        return slowPointer.next;
    }


    // ========================================================================
    // APPROACH 3: IN-ORDER SIMULATION
    // ========================================================================

    /**
     * OPTIMAL SOLUTION
     *
     * Time:  O(N)
     * Space: O(log N)
     *
     *
     * ------------------------------------------------------------------------
     * THE BIG IDEA
     * ------------------------------------------------------------------------
     *
     * This approach reverses our thinking.
     *
     * Instead of:
     *
     *      "Find the middle linked-list node."
     *
     * we say:
     *
     *      "I already know exactly how many nodes should go into
     *       the left subtree."
     *
     *
     * For N nodes:
     *
     *      left subtree gets approximately N / 2 nodes.
     *      root gets 1 node.
     *      right subtree gets the remaining nodes.
     *
     *
     * But how do we connect those nodes to the tree?
     *
     * We use the fact that an in-order traversal of a BST produces
     * values in sorted order.
     *
     *
     * =========================================================================
     * EXAMPLE
     * =========================================================================
     *
     * List:
     *
     *      1 -> 2 -> 3 -> 4 -> 5
     *
     * We know the final BST needs:
     *
     *              3
     *             / \
     *            1   4
     *             \   \
     *              2   5
     *
     * Its in-order traversal is:
     *
     *      1 -> 2 -> 3 -> 4 -> 5
     *
     * which is exactly the linked list.
     *
     *
     * So we can construct the tree while simultaneously walking
     * through the linked list.
     *
     *
     * =========================================================================
     * THE TRICK
     * =========================================================================
     *
     * Maintain a shared pointer:
     *
     *      currentListNode
     *
     * Initially:
     *
     *      currentListNode = head
     *
     * Recursively build the LEFT subtree first.
     *
     * Then:
     *
     *      currentListNode
     *
     * contains the next sorted value.
     *
     * Use it as the root.
     *
     * Advance the list pointer.
     *
     * Then recursively build the RIGHT subtree.
     *
     *
     * This exactly simulates an in-order traversal:
     *
     *      LEFT
     *      NODE
     *      RIGHT
     *
     *
     * =========================================================================
     * WHY IS THIS O(N)?
     * =========================================================================
     *
     * Every linked-list node is read exactly once.
     *
     * We never scan the list looking for a middle.
     *
     * We simply consume:
     *
     *      1 node
     *      1 node
     *      1 node
     *      ...
     *
     * Therefore:
     *
     *      O(N)
     *
     *
     * =========================================================================
     * SPACE
     * =========================================================================
     *
     * The recursion depth is the height of the balanced tree:
     *
     *      O(log N)
     *
     */
    public TreeNode sortedListToBSTOptimal(ListNode head) {

        /*
         * Count the number of nodes first.
         */
        int numberOfNodes = getListLength(head);

        /*
         * This pointer moves through the linked list exactly once
         * while the tree is being constructed.
         */
        ListNode[] currentListNode = {head};

        return buildBSTInOrder(
                currentListNode,
                0,
                numberOfNodes - 1
        );
    }

    /**
     * Builds a balanced BST for the index range:
     *
     *      [leftIndex ... rightIndex]
     *
     * The linked-list pointer is advanced in sorted order.
     */
    private TreeNode buildBSTInOrder(
            ListNode[] currentListNode,
            int leftIndex,
            int rightIndex) {

        /*
         * No nodes belong to this subtree.
         */
        if (leftIndex > rightIndex) {
            return null;
        }

        /*
         * Choose the middle position.
         *
         * This determines how many nodes belong to the left subtree
         * and right subtree.
         */
        int middleIndex =
                leftIndex + (rightIndex - leftIndex) / 2;

        /*
         * IMPORTANT:
         *
         * Build the LEFT subtree first.
         *
         * We do not create the current root yet because the linked-list
         * pointer must first advance through all values that belong
         * to the left subtree.
         */
        TreeNode leftSubtree =
                buildBSTInOrder(
                        currentListNode,
                        leftIndex,
                        middleIndex - 1
                );

        /*
         * Now the linked-list pointer is pointing at the value that
         * belongs to the current root.
         *
         * Example:
         *
         *      List: 1 -> 2 -> 3 -> 4 -> 5
         *
         * When constructing the root of the entire tree,
         * currentListNode points at 3.
         */
        TreeNode root =
                new TreeNode(currentListNode[0].value);

        /*
         * Attach the already-created left subtree.
         */
        root.left = leftSubtree;

        /*
         * Move to the next linked-list node.
         *
         * This node belongs somewhere in the right subtree.
         */
        currentListNode[0] =
                currentListNode[0].next;

        /*
         * Build the RIGHT subtree.
         *
         * The shared linked-list pointer now continues from the
         * correct sorted position.
         */
        root.right =
                buildBSTInOrder(
                        currentListNode,
                        middleIndex + 1,
                        rightIndex
                );

        return root;
    }

    /**
     * Counts the number of nodes in the linked list.
     */
    private int getListLength(ListNode head) {

        int length = 0;

        ListNode currentNode = head;

        while (currentNode != null) {
            length++;
            currentNode = currentNode.next;
        }

        return length;
    }


    // ========================================================================
    // TESTING / DEMO
    // ========================================================================

    public static void main(String[] args) {

        SortedListToBalancedBST solution =
                new SortedListToBalancedBST();

        /*
         * Create:
         *
         *      1 -> 2 -> 3 -> 4 -> 5
         */
        ListNode head =
                new ListNode(1);

        head.next =
                new ListNode(2);

        head.next.next =
                new ListNode(3);

        head.next.next.next =
                new ListNode(4);

        head.next.next.next.next =
                new ListNode(5);

        /*
         * Build using all three approaches.
         */
        TreeNode treeUsingArray =
                solution.sortedListToBSTUsingArray(head);

        TreeNode treeUsingMiddle =
                solution.sortedListToBSTUsingMiddle(head);

        TreeNode treeUsingOptimal =
                solution.sortedListToBSTOptimal(head);

        /*
         * Print in-order traversal.
         *
         * A valid BST must produce the original sorted order.
         */
        System.out.println(
                "Array approach   : "
                        + inOrderTraversal(treeUsingArray)
        );

        System.out.println(
                "Middle approach  : "
                        + inOrderTraversal(treeUsingMiddle)
        );

        System.out.println(
                "Optimal approach : "
                        + inOrderTraversal(treeUsingOptimal)
        );

        /*
         * Expected:
         *
         * [1, 2, 3, 4, 5]
         *
         * for all three approaches.
         */

        // ------------------------------------------------------------
        // Edge case: empty list
        // ------------------------------------------------------------

        System.out.println(
                "Empty list: "
                        + solution.sortedListToBSTOptimal(null)
        );

        // Expected:
        //
        // null

        // ------------------------------------------------------------
        // Edge case: one node
        // ------------------------------------------------------------

        TreeNode singleNodeTree =
                solution.sortedListToBSTOptimal(
                        new ListNode(42)
                );

        System.out.println(
                "Single node: "
                        + inOrderTraversal(singleNodeTree)
        );

        // Expected:
        //
        // [42]
    }

    /**
     * Utility method only for testing.
     *
     * Performs an in-order traversal of the BST.
     *
     * A BST's in-order traversal must produce sorted values.
     */
    private static List<Integer> inOrderTraversal(
            TreeNode root) {

        List<Integer> result = new ArrayList<>();

        collectInOrder(root, result);

        return result;
    }

    private static void collectInOrder(
            TreeNode currentNode,
            List<Integer> result) {

        if (currentNode == null) {
            return;
        }

        collectInOrder(
                currentNode.left,
                result
        );

        result.add(currentNode.value);

        collectInOrder(
                currentNode.right,
                result
        );
    }
}


/**
 * ============================================================================
 * INTERVIEW CHEAT SHEET
 * ============================================================================
 *
 *
 * PROBLEM:
 *
 *      Sorted Linked List
 *              ↓
 *      Height-Balanced BST
 *
 *
 * ============================================================================
 * KEY OBSERVATION
 * ============================================================================
 *
 * Since the linked list is sorted:
 *
 *      middle element = natural root
 *
 *      left half      = left subtree
 *      right half     = right subtree
 *
 *
 * ============================================================================
 * APPROACH 1
 * ============================================================================
 *
 * Linked List
 *      ↓
 * Array
 *      ↓
 * Pick middle recursively
 *
 * Time:
 *      O(N)
 *
 * Space:
 *      O(N)
 *
 * Easiest solution.
 *
 *
 * ============================================================================
 * APPROACH 2
 * ============================================================================
 *
 * Use slow/fast pointers to find the middle.
 *
 *      slow -> 1 step
 *      fast -> 2 steps
 *
 * Then recursively process left and right halves.
 *
 * Time:
 *      O(N log N)
 *
 * Space:
 *      O(log N)
 *
 *
 * ============================================================================
 * APPROACH 3
 * ============================================================================
 *
 * IN-ORDER SIMULATION
 *
 * Count N nodes first.
 *
 * Build:
 *
 *      left subtree
 *      root
 *      right subtree
 *
 * while consuming the linked list from left to right.
 *
 * Why does this work?
 *
 * Because:
 *
 *      In-order traversal of a BST
 *
 * produces:
 *
 *      sorted order
 *
 * which is exactly the order of the linked list.
 *
 *
 * Time:
 *      O(N)
 *
 * Space:
 *      O(log N)
 *
 *
 * ============================================================================
 * MOST IMPORTANT MENTAL MODEL
 * ============================================================================
 *
 * Don't think:
 *
 *      "How do I find the middle node repeatedly?"
 *
 * for the optimal solution.
 *
 * Think:
 *
 *      "A BST's in-order traversal is sorted."
 *
 * Therefore:
 *
 *      Sorted List
 *          ↓
 *      In-order traversal sequence
 *          ↓
 *      Build the tree around that sequence
 *
 *
 * ============================================================================
 * INTERVIEW EXPLANATION
 * ============================================================================
 *
 * "Because the linked list is sorted, the middle element should become
 * the root so that the number of nodes on both sides is approximately
 * equal.
 *
 * A straightforward solution converts the list to an array and recursively
 * chooses the middle element.
 *
 * If I want to avoid the O(N) array, I can repeatedly find the middle
 * using slow and fast pointers, but that takes O(N log N) time because
 * the list is scanned at every recursive level.
 *
 * The optimal approach uses in-order simulation. I first count the nodes.
 * Then I recursively build the left half, use the current linked-list
 * node as the root, advance the linked-list pointer, and recursively build
 * the right half.
 *
 * Because every linked-list node is consumed exactly once, the time
 * complexity is O(N), and the recursion depth is O(log N) because the
 * resulting tree is balanced."
 *
 * ============================================================================
 *
 * MEMORY TRIGGER:
 *
 *      "Sorted list + balanced BST"
 *
 *      -> Middle becomes root
 *
 *      For O(N):
 *
 *      -> Simulate INORDER
 *
 * ============================================================================
 */

