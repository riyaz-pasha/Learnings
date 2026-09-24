import java.util.ArrayDeque;
import java.util.Deque;

/**
 * ============================================================================
 * STUDY NOTE: SUM ROOT TO LEAF NUMBERS
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * - "The depth can be up to 10. A 10-digit number like 9,999,999,999 exceeds the 32-bit signed 
 *    integer limit (2,147,483,647). Should I use a `long` to prevent overflow, or is it guaranteed 
 *    the total sum fits in an `int`?" 
 *   (Shows deep awareness of system limits and mathematical boundaries).
 * - "What should I return if the tree is completely empty (null root)?" 
 *   (Establishes the base case. Generally 0, as there are no paths).
 * - "Does a node with a value of 0 at the root cause leading zero issues?" 
 *   (Mathematically, `0 * 10 + 5 = 5`, which correctly matches integer parsing behavior, but 
 *   asking this verifies edge-case thoughtfulness).
 * - "Are we counting every node to every other node, or strictly root-to-leaf?" 
 *   (Re-verifies the boundaries of a 'path' to ensure internal nodes aren't mistakenly counted).
 *
 *
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * Core Constraint: 
 * We must construct a number digit-by-digit as we travel downward, but we can only add that 
 * number to our total sum when we hit a definitive end (a leaf). The bottleneck is how we 
 * pass the "accumulated number so far" down the tree without mixing up the state between 
 * branching paths.
 *
 * Approach 1: String Concatenation (The Naive Brute Force)
 * What I'd naturally try: I could pass a `String` down the tree. At each node, I append the 
 * node's digit to the string (`pathStr + node.val`). When I reach a leaf, I parse the string 
 * using `Integer.parseInt(pathStr)` and add it to a global total.
 * Why it works: It perfectly mimics how humans read numbers left-to-right.
 * Why it's too slow/costly: Strings in Java are immutable. Appending a digit creates an entirely 
 * new String object in the heap.
 * - Time Complexity: O(N * H) — because string concatenation copies the existing characters at 
 *   every level of depth (H).
 * - Space Complexity: O(N * H) — because we allocate massive amounts of short-lived String objects 
 *   on the heap, triggering heavy Garbage Collection.
 * 
 * Approach 2: Base-10 Math & Iterative BFS (The Production-Safe Queue)
 * What I'd naturally try: How do I avoid Strings? Math! If I have the number 12, and I want to 
 * append 3, I just do `(12 * 10) + 3 = 123`. I can do a Level-Order traversal using a Queue, 
 * pushing a custom object that holds both the `TreeNode` and the `currentNumber` for that specific node.
 * Why it works: It completely eliminates String allocations, doing standard CPU-level integer math.
 * What property removes the bottleneck: Pairing the node and its state in a Queue means we don't 
 * rely on the thread's call stack, making it immune to StackOverflowErrors on deep trees.
 * - Time Complexity: O(N) — because every node is enqueued, processed with O(1) math, and dequeued once.
 * - Space Complexity: O(N) — because the Queue must hold all the leaves at the bottom level simultaneously. 
 *   In a balanced tree, this is roughly N/2 nodes.
 *
 * Approach 3: Top-Down Recursive DFS (The Optimal & Elegant Choice)
 * What I'd naturally try: If the tree depth is guaranteed to be <= 10 (as per constraints), 
 * a StackOverflow is mathematically impossible. I can ditch the Queue and custom objects, and just 
 * pass the `currentNumber` directly as a parameter in a recursive function. 
 * Why it works: The JVM call stack naturally creates isolated variables for every branch. When 
 * the left child finishes, the stack unwinds, and the right child gets the correct, unmodified 
 * `currentNumber` from the parent.
 * What property removes the bottleneck: The implicit call stack tracks state for us, eliminating 
 * the need for any heap-allocated data structures (Queues or custom Pair objects).
 * - Time Complexity: O(N) — because we visit every node exactly once, doing O(1) arithmetic.
 * - Space Complexity: O(H) — because the recursion stack goes as deep as the tree's height H 
 *   (which the constraints lock at max 10, meaning effectively O(1) space, but conceptually O(H)).
 *
 * The Interview Choice:
 * Always write Approach 3 (Recursive DFS). It is a 5-line masterpiece that demonstrates a core 
 * computer science concept: Top-Down State Passing. Mention the base-10 math trick explicitly, 
 * as it proves you can translate a string-manipulation intuition into an arithmetic optimization.
 *
 *
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * 1. Root is Null -> Must return 0 immediately.
 * 2. Tree with a single node (e.g., 5) -> The root is also the leaf, should just return 5.
 * 3. Internal nodes with 0 -> (e.g., 1 -> 0 -> 2). `1 * 10 = 10`. `10 * 10 + 2 = 102`. Works perfectly.
 * 4. Leading zeros -> (e.g., 0 -> 1 -> 3). `0 * 10 + 1 = 1`. `1 * 10 + 3 = 13`. Mathematically flawless.
 *
 *
 * 4. DRY RUN (Top-Down Recursive DFS)
 * ----------------------------------------------------------------------------
 * Tree:
 *       4
 *      / \
 *     9   0
 *    / \
 *   5   1
 *
 * Call Stack Trace (Initial call: dfs(root, 0)):
 * 1. dfs(node=4, currentNum=0):
 *    - Not a leaf. currentNum = (0 * 10) + 4 = 4.
 *    - Recurse Left: dfs(node=9, 4).
 *    - Recurse Right: dfs(node=0, 4).
 *
 * 2. [Left Branch] dfs(node=9, currentNum=4):
 *    - Not a leaf. currentNum = (4 * 10) + 9 = 49.
 *    - Recurse Left: dfs(node=5, 49).
 *    - Recurse Right: dfs(node=1, 49).
 *
 * 3. [Left-Left] dfs(node=5, currentNum=49):
 *    - IS A LEAF! currentNum = (49 * 10) + 5 = 495.
 *    - Returns 495 up the stack.
 *
 * 4. [Left-Right] dfs(node=1, currentNum=49):
 *    - IS A LEAF! currentNum = (49 * 10) + 1 = 491.
 *    - Returns 491 up the stack.
 *
 * 5. Node(9) receives 495 (left) and 491 (right). Returns 495 + 491 = 986.
 *
 * 6. [Right Branch] dfs(node=0, currentNum=4):
 *    - IS A LEAF! currentNum = (4 * 10) + 0 = 40.
 *    - Returns 40 up the stack.
 *
 * 7. Node(4) (Root) receives 986 (left) and 40 (right). Returns 986 + 40 = 1026.
 * Final Result: 1026.
 *
 * Pitfalls: 
 * - The "Null Node Double Count" Trap: 
 *   If you write the base case as `if (node == null) return currentNum;`, you will fail on trees 
 *   where nodes have only one child. A node like `(1 -> left: 2, right: null)` will pass `12` to 
 *   the left child (returning 12), and it will pass `1` to the null right child (returning 1). 
 *   The sum becomes 13 instead of 12. You MUST evaluate success strictly at leaf nodes 
 *   (`if left == null && right == null`).
 *
 * Pattern Recognition: 
 * "When aggregating a sequence of values along a tree path -> Think Top-Down State Passing in DFS, 
 * avoiding strings by using positional math."
 *
 * Interview Script:
 * "To find the sum of all root-to-leaf paths, I need to track the number being formed as I go down. 
 * Using Strings is too memory-intensive. Instead, I'll use base-10 math: at each node, I'll multiply 
 * the incoming number by 10 and add the node's value. I'll pass this new number down recursively. 
 * When I hit a leaf, I return that number. If it's an internal node, I return the sum of its left 
 * and right recursive calls. This is O(N) time and O(H) space for the stack."
 *
 *
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q1: What if the values are 0 or 1, and the paths represent Binary (Base-2) numbers?
 * A1: The algorithm stays exactly the same, but the math changes from `* 10` to `* 2` (or a bitwise 
 *     left shift `<< 1`). `currentNum = (currentNum << 1) | node.val;`
 *
 * Q2: What if the tree is massively deep, and the resulting number exceeds 64-bit Long limits?
 * A2: If paths form 100-digit numbers, primitives will overflow. We'd have to switch to passing down 
 *     `BigInteger` objects (using `.multiply(10).add(val)`), OR we revert to the String/StringBuilder 
 *     approach and use custom string-addition functions to sum the final strings at the leaf level.
 */

public class SumRootToLeafStudy {

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
     * APPROACH 3: TOP-DOWN RECURSIVE DFS (The optimal, professional choice)
     * Time: O(N) | Space: O(H)
     */
    public int sumNumbersRecursive(TreeNode root) {
        // Defensive check: empty tree yields 0
        if (root == null) {
            return 0;
        }
        
        // Start the recursion passing 0 as the initial 'current number'
        return dfs(root, 0);
    }

    private int dfs(TreeNode node, int currentNumber) {
        // We only call dfs on non-null children, but defensively checking null is standard.
        if (node == null) {
            return 0; 
        }

        // 1. Calculate the new number dynamically
        // e.g., if currentNumber is 49, and node.val is 5 -> 49 * 10 + 5 = 495
        currentNumber = (currentNumber * 10) + node.val;

        // 2. Base Case: Are we at a leaf node?
        // We ONLY register the number when the path definitively ends.
        if (node.left == null && node.right == null) {
            return currentNumber;
        }

        // 3. Recursive Step: Not a leaf, so ask children for their path sums.
        // If a child is null, its dfs() call safely returns 0, adding nothing to the sum.
        int leftSum = dfs(node.left, currentNumber);
        int rightSum = dfs(node.right, currentNumber);

        // The total sum passing through this node is the combination of its branches
        return leftSum + rightSum;
    }

    /**
     * APPROACH 2: ITERATIVE BFS (The robust choice for theoretically unbounded deep trees)
     * Time: O(N) | Space: O(N)
     */
    // Java 14+ record feature securely binds a node to its mathematical state
    public record NodeState(TreeNode node, int currentSum) {}

    public int sumNumbersIterative(TreeNode root) {
        if (root == null) return 0;

        int totalSum = 0;
        Deque<NodeState> queue = new ArrayDeque<>();
        
        queue.offer(new NodeState(root, root.val));

        while (!queue.isEmpty()) {
            NodeState state = queue.poll();
            TreeNode currentNode = state.node();
            int currentNum = state.currentSum();

            // If we hit a leaf, accumulate the path number into the global total
            if (currentNode.left == null && currentNode.right == null) {
                totalSum += currentNum;
            }

            // Enqueue left child with its updated math state
            if (currentNode.left != null) {
                int nextNum = (currentNum * 10) + currentNode.left.val;
                queue.offer(new NodeState(currentNode.left, nextNum));
            }
            
            // Enqueue right child with its updated math state
            if (currentNode.right != null) {
                int nextNum = (currentNum * 10) + currentNode.right.val;
                queue.offer(new NodeState(currentNode.right, nextNum));
            }
        }

        return totalSum;
    }

    /**
     * Main method to cross-check approaches against edge cases.
     */
    public static void main(String[] args) {
        SumRootToLeafStudy study = new SumRootToLeafStudy();

        // Edge Case 1: Empty Tree
        System.out.println("Empty Tree (DFS): " + study.sumNumbersRecursive(null)); // Expected: 0

        // Edge Case 2: Single Node Tree
        TreeNode single = new TreeNode(7);
        System.out.println("Single Node (DFS): " + study.sumNumbersRecursive(single)); // Expected: 7

        // Standard Case: The example from the DRY RUN
        //       4
        //      / \
        //     9   0
        //    / \
        //   5   1
        TreeNode standard = new TreeNode(4,
                new TreeNode(9, new TreeNode(5), new TreeNode(1)),
                new TreeNode(0)
        );
        System.out.println("Standard Tree (DFS): " + study.sumNumbersRecursive(standard)); // Expected: 1026
        System.out.println("Standard Tree (BFS): " + study.sumNumbersIterative(standard)); // Expected: 1026

        // Edge Case 3: The "False Positive Null" Trap (Asymmetric tree)
        //       1
        //      / 
        //     2
        // Path is 1->2 (12). If checking `node == null`, it adds left (12) + right null (1) = 13.
        TreeNode asymmetric = new TreeNode(1, new TreeNode(2), null);
        System.out.println("Asymmetric Trap (DFS): " + study.sumNumbersRecursive(asymmetric)); // Expected: 12

        // Edge Case 4: Leading Zeros
        //       0
        //      / \
        //     1   3
        // Paths: 0->1 (1) and 0->3 (3). Sum = 4.
        TreeNode leadingZero = new TreeNode(0, new TreeNode(1), new TreeNode(3));
        System.out.println("Leading Zeros (DFS): " + study.sumNumbersRecursive(leadingZero)); // Expected: 4
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core Pattern: Top-Down State Passing (DFS).
 * Key Observation: Do not use Strings. Treat path building as base-10 positional arithmetic 
 *                  (previous * 10 + current). Evaluate exclusively at leaf nodes.
 * Memorize: `curr = curr * 10 + node.val; if (leaf) return curr; else return dfs(left) + dfs(right);`
 * Most Common Trap: Writing `if (node == null) return curr;`. This causes a single leaf at 
 *                   the end of a one-sided branch to be counted twice (once for its null left child, 
 *                   and once for its null right child). Always check `node.left == null && node.right == null`.
 * One-Line Mental Trigger: "Root to Leaf Sum = Math (* 10 + val), check leaves."
 * ============================================================================
 */

