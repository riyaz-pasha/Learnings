import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/*
 * Implement the BSTIterator class that represents an iterator over the in-order
 * traversal of a binary search tree (BST):
 * 
 * BSTIterator(TreeNode root) Initializes an object of the BSTIterator class.
 * The root of the BST is given as part of the constructor. The pointer should
 * be initialized to a non-existent number smaller than any element in the BST.
 * boolean hasNext() Returns true if there exists a number in the traversal to
 * the right of the pointer, otherwise returns false.
 * int next() Moves the pointer to the right, then returns the number at the
 * pointer.
 * Notice that by initializing the pointer to a non-existent smallest number,
 * the first call to next() will return the smallest element in the BST.
 * 
 * You may assume that next() calls will always be valid. That is, there will be
 * at least a next number in the in-order traversal when next() is called.
 * 
 * Example 1:
 * Input
 * ["BSTIterator", "next", "next", "hasNext", "next", "hasNext", "next",
 * "hasNext", "next", "hasNext"]
 * [[[7, 3, 15, null, null, 9, 20]], [], [], [], [], [], [], [], [], []]
 * Output
 * [null, 3, 7, true, 9, true, 15, true, 20, false]
 * Explanation
 * BSTIterator bSTIterator = new BSTIterator([7, 3, 15, null, null, 9, 20]);
 * bSTIterator.next(); // return 3
 * bSTIterator.next(); // return 7
 * bSTIterator.hasNext(); // return True
 * bSTIterator.next(); // return 9
 * bSTIterator.hasNext(); // return True
 * bSTIterator.next(); // return 15
 * bSTIterator.hasNext(); // return True
 * bSTIterator.next(); // return 20
 * bSTIterator.hasNext(); // return False
 */

class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }

}

/**
 * Solution 1: Stack-based Approach (Most Efficient)
 * 
 * Uses a stack to simulate the recursive in-order traversal.
 * This approach achieves O(1) amortized time complexity for next().
 * 
 * Time Complexity:
 * - Constructor: O(h) where h is the height of the tree
 * - next(): O(1) amortized, O(h) worst case
 * - hasNext(): O(1)
 * 
 * Space Complexity: O(h) for the stack
 */
class BSTIterator {

    private Stack<TreeNode> stack;

    public BSTIterator(TreeNode root) {
        stack = new Stack<>();
        // Push all left nodes from root to the leftmost node
        pushAllLeft(root);
    }

    /**
     * Helper method to push all left children onto the stack
     */
    private void pushAllLeft(TreeNode node) {
        while (node != null) {
            stack.push(node);
            node = node.left;
        }
    }

    public int next() {
        TreeNode node = stack.pop();

        // If the popped node has a right child,
        // push all left children of the right subtree
        if (node.right != null) {
            pushAllLeft(node.right);
        }

        return node.val;
    }

    public boolean hasNext() {
        return !stack.isEmpty();
    }

}

/**
 * Solution 2: Pre-computed List Approach
 * 
 * Performs complete in-order traversal during initialization
 * and stores all values in a list.
 * 
 * Time Complexity:
 * - Constructor: O(n)
 * - next(): O(1)
 * - hasNext(): O(1)
 * 
 * Space Complexity: O(n) for storing all values
 */
class BSTIteratorList {

    private List<Integer> inorderList;
    private int index;

    public BSTIteratorList(TreeNode root) {
        inorderList = new ArrayList<>();
        index = 0;
        inorderTraversal(root);
    }

    private void inorderTraversal(TreeNode node) {
        if (node == null)
            return;

        inorderTraversal(node.left);
        inorderList.add(node.val);
        inorderTraversal(node.right);
    }

    public int next() {
        return inorderList.get(index++);
    }

    public boolean hasNext() {
        return index < inorderList.size();
    }

}

/**
 * Solution 3: Morris Traversal Approach (Advanced)
 * 
 * Uses Morris traversal to achieve O(1) space complexity
 * by temporarily modifying the tree structure.
 * 
 * Time Complexity:
 * - Constructor: O(1)
 * - next(): O(1) amortized
 * - hasNext(): O(1)
 * 
 * Space Complexity: O(1)
 */
class BSTIteratorMorris {

    private TreeNode current;
    private Integer nextVal;

    public BSTIteratorMorris(TreeNode root) {
        current = root;
        nextVal = null;
        findNext(); // Pre-compute the first value
    }

    private void findNext() {
        nextVal = null;

        while (current != null && nextVal == null) {
            if (current.left == null) {
                // No left subtree, current is the next value
                nextVal = current.val;
                current = current.right;
            } else {
                // Find the inorder predecessor
                TreeNode predecessor = current.left;
                while (predecessor.right != null && predecessor.right != current) {
                    predecessor = predecessor.right;
                }

                if (predecessor.right == null) {
                    // Make current the right child of its predecessor
                    predecessor.right = current;
                    current = current.left;
                } else {
                    // Revert the changes: remove the link
                    predecessor.right = null;
                    nextVal = current.val;
                    current = current.right;
                }
            }
        }
    }

    public int next() {
        int result = nextVal;
        findNext(); // Prepare the next value
        return result;
    }

    public boolean hasNext() {
        return nextVal != null;
    }

}

/**
 * Solution 4: Stack with Detailed Comments
 * 
 * Same as Solution 1 but with more detailed explanations
 */
class BSTIteratorDetailed {

    private Stack<TreeNode> stack;

    public BSTIteratorDetailed(TreeNode root) {
        stack = new Stack<>();

        // Initialize by pushing all leftmost nodes
        // This ensures the smallest element is at the top
        leftmostInorder(root);
    }

    /**
     * Pushes all leftmost nodes starting from the given node
     * This simulates going as far left as possible in in-order traversal
     */
    private void leftmostInorder(TreeNode root) {
        while (root != null) {
            stack.push(root);
            root = root.left;
        }
    }

    public int next() {
        // The node at the top of stack is the next smallest element
        TreeNode topmostNode = stack.pop();

        // If this node has a right child, we need to process
        // all leftmost nodes of the right subtree
        if (topmostNode.right != null) {
            leftmostInorder(topmostNode.right);
        }

        return topmostNode.val;
    }

    public boolean hasNext() {
        return !stack.isEmpty();
    }

}

/**
 * Test class to verify the implementations
 */
class TestBSTIterator {

    public static void main(String[] args) {
        // Create test BST: [7, 3, 15, null, null, 9, 20]
        // 7
        // / \
        // 3 15
        // / \
        // 9 20

        TreeNode root = new TreeNode(7);
        root.left = new TreeNode(3);
        root.right = new TreeNode(15);
        root.right.left = new TreeNode(9);
        root.right.right = new TreeNode(20);

        System.out.println("Testing Stack-based Iterator:");
        testIterator(new BSTIterator(root));

        System.out.println("\nTesting List-based Iterator:");
        testIterator2(new BSTIteratorList(root));

        System.out.println("\nTesting Morris Iterator:");
        testIterator3(new BSTIteratorMorris(root));
    }

    private static void testIterator(BSTIterator iterator) {
        System.out.println("next(): " + iterator.next()); // 3
        System.out.println("next(): " + iterator.next()); // 7
        System.out.println("hasNext(): " + iterator.hasNext()); // true
        System.out.println("next(): " + iterator.next()); // 9
        System.out.println("hasNext(): " + iterator.hasNext()); // true
        System.out.println("next(): " + iterator.next()); // 15
        System.out.println("hasNext(): " + iterator.hasNext()); // true
        System.out.println("next(): " + iterator.next()); // 20
        System.out.println("hasNext(): " + iterator.hasNext()); // false
    }

    private static void testIterator2(BSTIteratorList iterator) {
        System.out.println("next(): " + iterator.next()); // 3
        System.out.println("next(): " + iterator.next()); // 7
        System.out.println("hasNext(): " + iterator.hasNext()); // true
        System.out.println("next(): " + iterator.next()); // 9
        System.out.println("hasNext(): " + iterator.hasNext()); // true
        System.out.println("next(): " + iterator.next()); // 15
        System.out.println("hasNext(): " + iterator.hasNext()); // true
        System.out.println("next(): " + iterator.next()); // 20
        System.out.println("hasNext(): " + iterator.hasNext()); // false
    }

    private static void testIterator3(BSTIteratorMorris iterator) {
        System.out.println("next(): " + iterator.next()); // 3
        System.out.println("next(): " + iterator.next()); // 7
        System.out.println("hasNext(): " + iterator.hasNext()); // true
        System.out.println("next(): " + iterator.next()); // 9
        System.out.println("hasNext(): " + iterator.hasNext()); // true
        System.out.println("next(): " + iterator.next()); // 15
        System.out.println("hasNext(): " + iterator.hasNext()); // true
        System.out.println("next(): " + iterator.next()); // 20
        System.out.println("hasNext(): " + iterator.hasNext()); // false
    }

}

/**
 * Usage example matching the problem description:
 */
class UsageExample {

    public static void main(String[] args) {
        // Build the tree from the example: [7, 3, 15, null, null, 9, 20]
        TreeNode root = new TreeNode(7);
        root.left = new TreeNode(3);
        root.right = new TreeNode(15);
        root.right.left = new TreeNode(9);
        root.right.right = new TreeNode(20);

        BSTIterator bSTIterator = new BSTIterator(root);

        System.out.println(bSTIterator.next()); // return 3
        System.out.println(bSTIterator.next()); // return 7
        System.out.println(bSTIterator.hasNext()); // return true
        System.out.println(bSTIterator.next()); // return 9
        System.out.println(bSTIterator.hasNext()); // return true
        System.out.println(bSTIterator.next()); // return 15
        System.out.println(bSTIterator.hasNext()); // return true
        System.out.println(bSTIterator.next()); // return 20
        System.out.println(bSTIterator.hasNext()); // return false
    }

}

/**
 * Complexity Analysis Summary:
 * 
 * 1. Stack-based (Recommended):
 * - Space: O(h) where h is height
 * - Time: O(1) amortized for next(), O(1) for hasNext()
 * - Best balance of time and space efficiency
 * 
 * 2. List-based:
 * - Space: O(n) where n is number of nodes
 * - Time: O(1) for both next() and hasNext()
 * - Simple but uses more memory
 * 
 * 3. Morris Traversal:
 * - Space: O(1) constant space
 * - Time: O(1) amortized for next(), O(1) for hasNext()
 * - Most space-efficient but complex to implement
 */
