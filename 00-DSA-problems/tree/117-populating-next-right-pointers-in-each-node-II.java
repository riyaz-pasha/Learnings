import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/*
 * Given a binary tree
 * 
 * struct Node {
 * int val;
 * Node *left;
 * Node *right;
 * Node *next;
 * }
 * Populate each next pointer to point to its next right node. If there is no
 * next right node, the next pointer should be set to NULL.
 * 
 * Initially, all next pointers are set to NULL.
 * 
 * Example 1:
 * Input: root = [1,2,3,4,5,null,7]
 * Output: [1,#,2,3,#,4,5,7,#]
 * Explanation: Given the above binary tree (Figure A), your function should
 * populate each next pointer to point to its next right node, just like in
 * Figure B. The serialized output is in level order as connected by the next
 * pointers, with '#' signifying the end of each level.
 * 
 * Example 2:
 * Input: root = []
 * Output: []
 */

class Node {

    public int val;
    public Node left;
    public Node right;
    public Node next;

    public Node() {
    }

    public Node(int _val) {
        val = _val;
    }

    public Node(int _val, Node _left, Node _right, Node _next) {
        val = _val;
        left = _left;
        right = _right;
        next = _next;
    }

}

class Solution {

    // Solution 1: Level Order Traversal (BFS) - Most Intuitive
    // Time: O(n), Space: O(w) where w is maximum width of tree
    public Node connect(Node root) {
        if (root == null)
            return null;

        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();

            // Process all nodes at current level
            for (int i = 0; i < levelSize; i++) {
                Node node = queue.poll();

                // Connect to next node in the same level (except for last node)
                if (i < levelSize - 1) {
                    node.next = queue.peek();
                }

                // Add children to queue for next level
                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
        }

        return root;
    }

    // Solution 2: O(1) Space - Two Pointers Approach
    // Time: O(n), Space: O(1)
    public Node connect2(Node root) {
        if (root == null)
            return null;

        Node levelStart = root;

        while (levelStart != null) {
            Node curr = levelStart;
            Node nextLevelStart = null;
            Node nextLevelPrev = null;

            // Traverse current level and connect next level
            while (curr != null) {
                // Process left child
                if (curr.left != null) {
                    if (nextLevelPrev != null) {
                        nextLevelPrev.next = curr.left;
                    } else {
                        nextLevelStart = curr.left; // First node of next level
                    }
                    nextLevelPrev = curr.left;
                }

                // Process right child
                if (curr.right != null) {
                    if (nextLevelPrev != null) {
                        nextLevelPrev.next = curr.right;
                    } else {
                        nextLevelStart = curr.right; // First node of next level
                    }
                    nextLevelPrev = curr.right;
                }

                // Move to next node in current level
                curr = curr.next;
            }

            // Move to next level
            levelStart = nextLevelStart;
        }

        return root;
    }

    // Solution 3: Recursive DFS with Level Tracking
    // Time: O(n), Space: O(h) where h is height of tree
    public Node connect3(Node root) {
        connectHelper(root, 0, new HashMap<>());
        return root;
    }

    private void connectHelper(Node node, int level, Map<Integer, Node> levelMap) {
        if (node == null)
            return;

        // If we've seen a node at this level before, connect it
        if (levelMap.containsKey(level)) {
            levelMap.get(level).next = node;
        }

        // Update the last seen node at this level
        levelMap.put(level, node);

        // Recursively process children
        connectHelper(node.left, level + 1, levelMap);
        connectHelper(node.right, level + 1, levelMap);
    }

    // Solution 4: O(1) Space with Helper Function
    // Time: O(n), Space: O(1)
    public Node connect4(Node root) {
        Node levelStart = root;

        while (levelStart != null) {
            levelStart = connectLevel(levelStart);
        }

        return root;
    }

    // Helper function to connect one level and return start of next level
    private Node connectLevel(Node levelStart) {
        Node curr = levelStart;
        Node nextLevelStart = null;
        Node nextLevelPrev = null;

        while (curr != null) {
            // Check left child
            if (curr.left != null) {
                if (nextLevelPrev == null) {
                    nextLevelStart = curr.left;
                    nextLevelPrev = curr.left;
                } else {
                    nextLevelPrev.next = curr.left;
                    nextLevelPrev = curr.left;
                }
            }

            // Check right child
            if (curr.right != null) {
                if (nextLevelPrev == null) {
                    nextLevelStart = curr.right;
                    nextLevelPrev = curr.right;
                } else {
                    nextLevelPrev.next = curr.right;
                    nextLevelPrev = curr.right;
                }
            }

            curr = curr.next;
        }

        return nextLevelStart;
    }

    // Solution 5: Using ArrayList for Each Level
    // Time: O(n), Space: O(w) where w is maximum width
    public Node connect5(Node root) {
        if (root == null)
            return null;

        List<List<Node>> levels = new ArrayList<>();
        buildLevels(root, 0, levels);

        // Connect nodes at each level
        for (List<Node> level : levels) {
            for (int i = 0; i < level.size() - 1; i++) {
                level.get(i).next = level.get(i + 1);
            }
        }

        return root;
    }

    private void buildLevels(Node node, int level, List<List<Node>> levels) {
        if (node == null)
            return;

        if (levels.size() <= level) {
            levels.add(new ArrayList<>());
        }

        levels.get(level).add(node);

        buildLevels(node.left, level + 1, levels);
        buildLevels(node.right, level + 1, levels);
    }

    // Utility methods for testing
    public static Node buildTree(Integer[] values) {
        if (values == null || values.length == 0)
            return null;

        Node root = new Node(values[0]);
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);

        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            Node node = queue.poll();

            if (i < values.length && values[i] != null) {
                node.left = new Node(values[i]);
                queue.offer(node.left);
            }
            i++;

            if (i < values.length && values[i] != null) {
                node.right = new Node(values[i]);
                queue.offer(node.right);
            }
            i++;
        }

        return root;
    }

    public static void printWithNextPointers(Node root) {
        if (root == null) {
            System.out.println("[]");
            return;
        }

        List<String> result = new ArrayList<>();
        Node levelStart = root;

        while (levelStart != null) {
            Node curr = levelStart;
            Node nextLevelStart = null;

            // Traverse current level
            while (curr != null) {
                result.add(String.valueOf(curr.val));

                // Find start of next level
                if (nextLevelStart == null) {
                    if (curr.left != null) {
                        nextLevelStart = curr.left;
                    } else if (curr.right != null) {
                        nextLevelStart = curr.right;
                    }
                }

                curr = curr.next;
            }

            result.add("#"); // End of level marker

            // Find next level start if not found yet
            if (nextLevelStart == null) {
                curr = levelStart;
                while (curr != null && nextLevelStart == null) {
                    if (curr.left != null) {
                        nextLevelStart = curr.left;
                        break;
                    }
                    if (curr.right != null) {
                        nextLevelStart = curr.right;
                        break;
                    }
                    curr = curr.next;
                }
            }

            levelStart = nextLevelStart;
        }

        System.out.println(result);
    }

    // More accurate printing method
    public static void printLevelOrderWithNext(Node root) {
        if (root == null) {
            System.out.println("[]");
            return;
        }

        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        List<String> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            int levelSize = queue.size();

            for (int i = 0; i < levelSize; i++) {
                Node node = queue.poll();
                result.add(String.valueOf(node.val));

                if (node.left != null)
                    queue.offer(node.left);
                if (node.right != null)
                    queue.offer(node.right);
            }

            result.add("#");
        }

        System.out.println(result);
    }

    // Test the solutions
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Example 1: [1,2,3,4,5,null,7]
        System.out.println("Example 1:");
        Integer[] values1 = { 1, 2, 3, 4, 5, null, 7 };
        Node root1 = buildTree(values1);
        System.out.println("Input: " + Arrays.toString(values1));

        root1 = sol.connect(root1);
        System.out.print("Output: ");
        printLevelOrderWithNext(root1);

        // Example 2: Empty tree
        System.out.println("\nExample 2:");
        Node root2 = sol.connect2(null);
        System.out.print("Output: ");
        printLevelOrderWithNext(root2);

        // Example 3: More complex tree
        System.out.println("\nExample 3:");
        Integer[] values3 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
        Node root3 = buildTree(values3);
        System.out.println("Input: " + Arrays.toString(values3));

        root3 = sol.connect4(root3);
        System.out.print("Output: ");
        printLevelOrderWithNext(root3);

        // Example 4: Unbalanced tree
        System.out.println("\nExample 4:");
        Integer[] values4 = { 1, 2, null, 3, null, 4, null };
        Node root4 = buildTree(values4);
        System.out.println("Input: " + Arrays.toString(values4));

        root4 = sol.connect5(root4);
        System.out.print("Output: ");
        printLevelOrderWithNext(root4);
    }

}
