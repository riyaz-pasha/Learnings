import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
/*
 * Given the root of a binary tree and an integer targetSum, return true if the
 * tree has a root-to-leaf path such that adding up all the values along the
 * path equals targetSum.
 * 
 * A leaf is a node with no children.
 * 
 * Example 1:
 * Input: root = [5,4,8,11,null,13,4,7,2,null,null,null,1], targetSum = 22
 * Output: true
 * Explanation: The root-to-leaf path with the target sum is shown.
 * 
 * Example 2:
 * Input: root = [1,2,3], targetSum = 5
 * Output: false
 * Explanation: There are two root-to-leaf paths in the tree:
 * (1 --> 2): The sum is 3.
 * (1 --> 3): The sum is 4.
 * There is no root-to-leaf path with sum = 5.
 * 
 * Example 3:
 * Input: root = [], targetSum = 0
 * Output: false
 * Explanation: Since the tree is empty, there are no root-to-leaf paths.
 */

class PathSum {

    public boolean hasPathSum(TreeNode root, int targetSum) {
        if (root == null) {
            return false;
        }

        if (root.left == null && root.right == null) {
            return root.val == targetSum;
        }

        return hasPathSum(root.left, targetSum - root.val)
                || hasPathSum(root.right, targetSum - root.val);
    }

}

class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }

}

class Solution {

    // Solution 1: Recursive DFS (Top-Down) - Most Intuitive
    // Time: O(n), Space: O(h) where h is height of tree
    public boolean hasPathSum(TreeNode root, int targetSum) {
        if (root == null) {
            return false;
        }

        // If it's a leaf node, check if current sum equals target
        if (root.left == null && root.right == null) {
            return root.val == targetSum;
        }

        // Recursively check left and right subtrees with reduced target
        int remainingSum = targetSum - root.val;
        return hasPathSum(root.left, remainingSum) || hasPathSum(root.right, remainingSum);
    }

    // Solution 2: Recursive DFS with Running Sum
    // Time: O(n), Space: O(h)
    public boolean hasPathSum2(TreeNode root, int targetSum) {
        return dfsWithSum(root, 0, targetSum);
    }

    private boolean dfsWithSum(TreeNode node, int currentSum, int targetSum) {
        if (node == null) {
            return false;
        }

        currentSum += node.val;

        // If it's a leaf, check if we reached target sum
        if (node.left == null && node.right == null) {
            return currentSum == targetSum;
        }

        // Check both subtrees
        return dfsWithSum(node.left, currentSum, targetSum) ||
                dfsWithSum(node.right, currentSum, targetSum);
    }

    // Solution 3: Iterative DFS using Stack
    // Time: O(n), Space: O(h)
    public boolean hasPathSum3(TreeNode root, int targetSum) {
        if (root == null) {
            return false;
        }

        Stack<TreeNode> nodeStack = new Stack<>();
        Stack<Integer> sumStack = new Stack<>();

        nodeStack.push(root);
        sumStack.push(root.val);

        while (!nodeStack.isEmpty()) {
            TreeNode node = nodeStack.pop();
            int currentSum = sumStack.pop();

            // If it's a leaf node, check the sum
            if (node.left == null && node.right == null) {
                if (currentSum == targetSum) {
                    return true;
                }
            }

            // Add children to stack if they exist
            if (node.right != null) {
                nodeStack.push(node.right);
                sumStack.push(currentSum + node.right.val);
            }

            if (node.left != null) {
                nodeStack.push(node.left);
                sumStack.push(currentSum + node.left.val);
            }
        }

        return false;
    }

    // Solution 4: Iterative BFS using Queue
    // Time: O(n), Space: O(w) where w is maximum width of tree
    public boolean hasPathSum4(TreeNode root, int targetSum) {
        if (root == null) {
            return false;
        }

        Queue<TreeNode> nodeQueue = new LinkedList<>();
        Queue<Integer> sumQueue = new LinkedList<>();

        nodeQueue.offer(root);
        sumQueue.offer(root.val);

        while (!nodeQueue.isEmpty()) {
            TreeNode node = nodeQueue.poll();
            int currentSum = sumQueue.poll();

            // If it's a leaf node, check the sum
            if (node.left == null && node.right == null) {
                if (currentSum == targetSum) {
                    return true;
                }
            }

            // Add children to queue if they exist
            if (node.left != null) {
                nodeQueue.offer(node.left);
                sumQueue.offer(currentSum + node.left.val);
            }

            if (node.right != null) {
                nodeQueue.offer(node.right);
                sumQueue.offer(currentSum + node.right.val);
            }
        }

        return false;
    }

    // Solution 5: Using Pair class for cleaner code
    // Time: O(n), Space: O(h)
    static class Pair {
        TreeNode node;
        int sum;

        Pair(TreeNode node, int sum) {
            this.node = node;
            this.sum = sum;
        }
    }

    public boolean hasPathSum5(TreeNode root, int targetSum) {
        if (root == null) {
            return false;
        }

        Stack<Pair> stack = new Stack<>();
        stack.push(new Pair(root, root.val));

        while (!stack.isEmpty()) {
            Pair current = stack.pop();
            TreeNode node = current.node;
            int sum = current.sum;

            // If it's a leaf node, check the sum
            if (node.left == null && node.right == null) {
                if (sum == targetSum) {
                    return true;
                }
                continue;
            }

            // Add children to stack
            if (node.right != null) {
                stack.push(new Pair(node.right, sum + node.right.val));
            }

            if (node.left != null) {
                stack.push(new Pair(node.left, sum + node.left.val));
            }
        }

        return false;
    }

    // Solution 6: Morris Traversal (Advanced - O(1) extra space)
    // Time: O(n), Space: O(1)
    public boolean hasPathSum6(TreeNode root, int targetSum) {
        if (root == null)
            return false;

        TreeNode current = root;
        int sum = 0;

        while (current != null) {
            if (current.left == null) {
                sum += current.val;

                // Check if it's a leaf
                if (current.right == null && sum == targetSum) {
                    return true;
                }

                current = current.right;
            } else {
                // Find inorder predecessor
                TreeNode predecessor = current.left;
                int steps = 1;

                while (predecessor.right != null && predecessor.right != current) {
                    predecessor = predecessor.right;
                    steps++;
                }

                if (predecessor.right == null) {
                    // Make thread
                    predecessor.right = current;
                    sum += current.val;
                    current = current.left;
                } else {
                    // Break thread
                    predecessor.right = null;

                    // Check if predecessor is a leaf in the path
                    if (predecessor.left == null && sum == targetSum) {
                        return true;
                    }

                    // Backtrack sum
                    sum -= steps * current.val;
                    current = current.right;
                }
            }
        }

        return false;
    }

    // Utility method to build tree from array
    public static TreeNode buildTree(Integer[] values) {
        if (values == null || values.length == 0)
            return null;

        TreeNode root = new TreeNode(values[0]);
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            TreeNode node = queue.poll();

            if (i < values.length && values[i] != null) {
                node.left = new TreeNode(values[i]);
                queue.offer(node.left);
            }
            i++;

            if (i < values.length && values[i] != null) {
                node.right = new TreeNode(values[i]);
                queue.offer(node.right);
            }
            i++;
        }

        return root;
    }

    // Utility method to find all root-to-leaf paths (for verification)
    public static List<List<Integer>> findAllPaths(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null)
            return result;

        findPathsHelper(root, new ArrayList<>(), result);
        return result;
    }

    private static void findPathsHelper(TreeNode node, List<Integer> currentPath,
            List<List<Integer>> result) {
        if (node == null)
            return;

        currentPath.add(node.val);

        if (node.left == null && node.right == null) {
            result.add(new ArrayList<>(currentPath));
        } else {
            findPathsHelper(node.left, currentPath, result);
            findPathsHelper(node.right, currentPath, result);
        }

        currentPath.remove(currentPath.size() - 1);
    }

    // Utility method to calculate path sums
    public static List<Integer> getPathSums(List<List<Integer>> paths) {
        List<Integer> sums = new ArrayList<>();
        for (List<Integer> path : paths) {
            int sum = path.stream().mapToInt(Integer::intValue).sum();
            sums.add(sum);
        }
        return sums;
    }

    // Test the solutions
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Example 1: [5,4,8,11,null,13,4,7,2,null,null,null,1], targetSum = 22
        System.out.println("Example 1:");
        Integer[] values1 = { 5, 4, 8, 11, null, 13, 4, 7, 2, null, null, null, 1 };
        TreeNode root1 = buildTree(values1);
        int targetSum1 = 22;

        System.out.println("Input: " + Arrays.toString(values1) + ", targetSum = " + targetSum1);
        System.out.println("All paths: " + findAllPaths(root1));
        System.out.println("Path sums: " + getPathSums(findAllPaths(root1)));

        System.out.println("Solution 1 (Recursive): " + sol.hasPathSum(root1, targetSum1));
        System.out.println("Solution 2 (Recursive with sum): " + sol.hasPathSum2(root1, targetSum1));
        System.out.println("Solution 3 (Iterative DFS): " + sol.hasPathSum3(root1, targetSum1));
        System.out.println("Solution 4 (Iterative BFS): " + sol.hasPathSum4(root1, targetSum1));
        System.out.println("Solution 5 (Pair class): " + sol.hasPathSum5(root1, targetSum1));

        // Example 2: [1,2,3], targetSum = 5
        System.out.println("\nExample 2:");
        Integer[] values2 = { 1, 2, 3 };
        TreeNode root2 = buildTree(values2);
        int targetSum2 = 5;

        System.out.println("Input: " + Arrays.toString(values2) + ", targetSum = " + targetSum2);
        System.out.println("All paths: " + findAllPaths(root2));
        System.out.println("Path sums: " + getPathSums(findAllPaths(root2)));
        System.out.println("Result: " + sol.hasPathSum(root2, targetSum2));

        // Example 3: [], targetSum = 0
        System.out.println("\nExample 3:");
        TreeNode root3 = null;
        int targetSum3 = 0;
        System.out.println("Input: [], targetSum = " + targetSum3);
        System.out.println("Result: " + sol.hasPathSum(root3, targetSum3));

        // Example 4: Edge case - single node
        System.out.println("\nExample 4:");
        TreeNode root4 = new TreeNode(1);
        int targetSum4 = 1;
        System.out.println("Input: [1], targetSum = " + targetSum4);
        System.out.println("Result: " + sol.hasPathSum(root4, targetSum4));

        // Example 5: Negative numbers
        System.out.println("\nExample 5:");
        Integer[] values5 = { 1, -2, 3, -4, 5 };
        TreeNode root5 = buildTree(values5);
        int targetSum5 = 0;
        System.out.println("Input: " + Arrays.toString(values5) + ", targetSum = " + targetSum5);
        System.out.println("All paths: " + findAllPaths(root5));
        System.out.println("Path sums: " + getPathSums(findAllPaths(root5)));
        System.out.println("Result: " + sol.hasPathSum(root5, targetSum5));
    }

}
