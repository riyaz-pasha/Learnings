import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

/*
 * You are given the root of a binary tree containing digits from 0 to 9 only.
 * 
 * Each root-to-leaf path in the tree represents a number.
 * 
 * For example, the root-to-leaf path 1 -> 2 -> 3 represents the number 123.
 * Return the total sum of all root-to-leaf numbers. Test cases are generated so
 * that the answer will fit in a 32-bit integer.
 * 
 * A leaf node is a node with no children.
 * 
 * Example 1:
 * Input: root = [1,2,3]
 * Output: 25
 * Explanation:
 * The root-to-leaf path 1->2 represents the number 12.
 * The root-to-leaf path 1->3 represents the number 13.
 * Therefore, sum = 12 + 13 = 25.
 * 
 * Example 2:
 * Input: root = [4,9,0,5,1]
 * Output: 1026
 * Explanation:
 * The root-to-leaf path 4->9->5 represents the number 495.
 * The root-to-leaf path 4->9->1 represents the number 491.
 * The root-to-leaf path 4->0 represents the number 40.
 * Therefore, sum = 495 + 491 + 40 = 1026.
 */

class SumRootToLeafNumbers {

    public int sumNumbers(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        int current = 0;
        dfs(root, current, result);
        return result.stream().reduce(0, (a, b) -> a + b);
    }

    private void dfs(TreeNode node, int num, List<Integer> result) {
        if (node == null)
            return;
        num = (num * 10) + node.val;
        if (node.left == null && node.right == null) {
            result.add(num);
        } else {
            dfs(node.left, num, result);
            dfs(node.right, num, result);
        }
    }

}

class SumRootToLeafNumbers2 {

    public int sumNumbers(TreeNode root) {
        return dfs(root, 0);
    }

    private int dfs(TreeNode node, int num) {
        if (node == null)
            return 0;
        int currentNum = (num * 10) + node.val;
        if (node.left == null && node.right == null) {
            return currentNum;
        }
        return dfs(node.left, currentNum) + dfs(node.right, currentNum);
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

    // Solution 1: Recursive DFS with Current Number - Most Elegant
    // Time: O(n), Space: O(h) where h is height of tree
    public int sumNumbers(TreeNode root) {
        return dfs(root, 0);
    }

    private int dfs(TreeNode node, int currentNumber) {
        if (node == null) {
            return 0;
        }

        // Build the current number by appending current digit
        currentNumber = currentNumber * 10 + node.val;

        // If it's a leaf, return the complete number
        if (node.left == null && node.right == null) {
            return currentNumber;
        }

        // Sum the results from left and right subtrees
        return dfs(node.left, currentNumber) + dfs(node.right, currentNumber);
    }

    // Solution 2: Recursive DFS with Global Sum
    // Time: O(n), Space: O(h)
    private int totalSum = 0;

    public int sumNumbers2(TreeNode root) {
        totalSum = 0; // Reset for multiple calls
        dfsWithGlobalSum(root, 0);
        return totalSum;
    }

    private void dfsWithGlobalSum(TreeNode node, int currentNumber) {
        if (node == null) {
            return;
        }

        currentNumber = currentNumber * 10 + node.val;

        // If it's a leaf, add to total sum
        if (node.left == null && node.right == null) {
            totalSum += currentNumber;
            return;
        }

        // Continue DFS on children
        dfsWithGlobalSum(node.left, currentNumber);
        dfsWithGlobalSum(node.right, currentNumber);
    }

    // Solution 3: Iterative DFS using Stack
    // Time: O(n), Space: O(h)
    public int sumNumbers3(TreeNode root) {
        if (root == null) {
            return 0;
        }

        Stack<TreeNode> nodeStack = new Stack<>();
        Stack<Integer> numberStack = new Stack<>();

        nodeStack.push(root);
        numberStack.push(root.val);

        int totalSum = 0;

        while (!nodeStack.isEmpty()) {
            TreeNode node = nodeStack.pop();
            int currentNumber = numberStack.pop();

            // If it's a leaf, add to total sum
            if (node.left == null && node.right == null) {
                totalSum += currentNumber;
            }

            // Add children to stack with updated numbers
            if (node.right != null) {
                nodeStack.push(node.right);
                numberStack.push(currentNumber * 10 + node.right.val);
            }

            if (node.left != null) {
                nodeStack.push(node.left);
                numberStack.push(currentNumber * 10 + node.left.val);
            }
        }

        return totalSum;
    }

    // Solution 4: Iterative BFS using Queue
    // Time: O(n), Space: O(w) where w is maximum width
    public int sumNumbers4(TreeNode root) {
        if (root == null) {
            return 0;
        }

        Queue<TreeNode> nodeQueue = new LinkedList<>();
        Queue<Integer> numberQueue = new LinkedList<>();

        nodeQueue.offer(root);
        numberQueue.offer(root.val);

        int totalSum = 0;

        while (!nodeQueue.isEmpty()) {
            TreeNode node = nodeQueue.poll();
            int currentNumber = numberQueue.poll();

            // If it's a leaf, add to total sum
            if (node.left == null && node.right == null) {
                totalSum += currentNumber;
            }

            // Add children to queue with updated numbers
            if (node.left != null) {
                nodeQueue.offer(node.left);
                numberQueue.offer(currentNumber * 10 + node.left.val);
            }

            if (node.right != null) {
                nodeQueue.offer(node.right);
                numberQueue.offer(currentNumber * 10 + node.right.val);
            }
        }

        return totalSum;
    }

    // Solution 5: Using Custom Pair Class
    // Time: O(n), Space: O(h)
    static class Pair {
        TreeNode node;
        int number;

        Pair(TreeNode node, int number) {
            this.node = node;
            this.number = number;
        }
    }

    public int sumNumbers5(TreeNode root) {
        if (root == null) {
            return 0;
        }

        Stack<Pair> stack = new Stack<>();
        stack.push(new Pair(root, root.val));

        int totalSum = 0;

        while (!stack.isEmpty()) {
            Pair current = stack.pop();
            TreeNode node = current.node;
            int number = current.number;

            // If it's a leaf, add to total sum
            if (node.left == null && node.right == null) {
                totalSum += number;
                continue;
            }

            // Add children to stack
            if (node.right != null) {
                stack.push(new Pair(node.right, number * 10 + node.right.val));
            }

            if (node.left != null) {
                stack.push(new Pair(node.left, number * 10 + node.left.val));
            }
        }

        return totalSum;
    }

    // Solution 6: String-based approach (Less efficient but educational)
    // Time: O(n * h), Space: O(h)
    public int sumNumbers6(TreeNode root) {
        List<String> paths = new ArrayList<>();
        findAllPaths(root, "", paths);

        int totalSum = 0;
        for (String path : paths) {
            totalSum += Integer.parseInt(path);
        }

        return totalSum;
    }

    private void findAllPaths(TreeNode node, String currentPath, List<String> paths) {
        if (node == null) {
            return;
        }

        currentPath += node.val;

        // If it's a leaf, add the complete path
        if (node.left == null && node.right == null) {
            paths.add(currentPath);
            return;
        }

        // Continue building paths
        findAllPaths(node.left, currentPath, paths);
        findAllPaths(node.right, currentPath, paths);
    }

    // Solution 7: Morris Traversal (Advanced - O(1) extra space)
    // Time: O(n), Space: O(1)
    public int sumNumbers7(TreeNode root) {
        if (root == null)
            return 0;

        int totalSum = 0;
        int currentNumber = 0;
        TreeNode current = root;

        while (current != null) {
            if (current.left == null) {
                // Process current node
                currentNumber = currentNumber * 10 + current.val;

                // If it's a leaf, add to sum
                if (current.right == null) {
                    totalSum += currentNumber;
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
                    currentNumber = currentNumber * 10 + current.val;
                    current = current.left;
                } else {
                    // Remove thread and process leaf if needed
                    predecessor.right = null;

                    if (predecessor.left == null) {
                        totalSum += currentNumber;
                    }

                    // Backtrack the number
                    for (int i = 0; i < steps; i++) {
                        currentNumber /= 10;
                    }

                    current = current.right;
                }
            }
        }

        return totalSum;
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

    // Utility method to find all root-to-leaf paths as numbers
    public static List<Integer> findAllNumbers(TreeNode root) {
        List<Integer> numbers = new ArrayList<>();
        findAllNumbersHelper(root, 0, numbers);
        return numbers;
    }

    private static void findAllNumbersHelper(TreeNode node, int currentNumber, List<Integer> numbers) {
        if (node == null)
            return;

        currentNumber = currentNumber * 10 + node.val;

        if (node.left == null && node.right == null) {
            numbers.add(currentNumber);
            return;
        }

        findAllNumbersHelper(node.left, currentNumber, numbers);
        findAllNumbersHelper(node.right, currentNumber, numbers);
    }

    // Utility method to print tree structure
    public static void printTree(TreeNode root) {
        if (root == null) {
            System.out.println("null");
            return;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        List<String> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node != null) {
                result.add(String.valueOf(node.val));
                queue.offer(node.left);
                queue.offer(node.right);
            } else {
                result.add("null");
            }
        }

        // Remove trailing nulls
        while (!result.isEmpty() && result.get(result.size() - 1).equals("null")) {
            result.remove(result.size() - 1);
        }

        System.out.println(result);
    }

    // Test the solutions
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Example 1: [1,2,3]
        System.out.println("Example 1:");
        Integer[] values1 = { 1, 2, 3 };
        TreeNode root1 = buildTree(values1);
        System.out.println("Input: " + Arrays.toString(values1));
        System.out.print("Tree structure: ");
        printTree(root1);
        System.out.println("All root-to-leaf numbers: " + findAllNumbers(root1));

        System.out.println("Solution 1 (Recursive): " + sol.sumNumbers(root1));
        System.out.println("Solution 2 (Global sum): " + sol.sumNumbers2(root1));
        System.out.println("Solution 3 (Iterative DFS): " + sol.sumNumbers3(root1));
        System.out.println("Solution 4 (Iterative BFS): " + sol.sumNumbers4(root1));
        System.out.println("Solution 5 (Pair class): " + sol.sumNumbers5(root1));
        System.out.println("Solution 6 (String-based): " + sol.sumNumbers6(root1));

        // Example 2: [4,9,0,5,1]
        System.out.println("\nExample 2:");
        Integer[] values2 = { 4, 9, 0, 5, 1 };
        TreeNode root2 = buildTree(values2);
        System.out.println("Input: " + Arrays.toString(values2));
        System.out.print("Tree structure: ");
        printTree(root2);
        System.out.println("All root-to-leaf numbers: " + findAllNumbers(root2));
        System.out.println("Result: " + sol.sumNumbers(root2));

        // Example 3: Single node
        System.out.println("\nExample 3:");
        TreeNode root3 = new TreeNode(5);
        System.out.println("Input: [5]");
        System.out.println("All root-to-leaf numbers: " + findAllNumbers(root3));
        System.out.println("Result: " + sol.sumNumbers(root3));

        // Example 4: More complex tree
        System.out.println("\nExample 4:");
        Integer[] values4 = { 1, 2, 3, 4, 5, 6, 7 };
        TreeNode root4 = buildTree(values4);
        System.out.println("Input: " + Arrays.toString(values4));
        System.out.print("Tree structure: ");
        printTree(root4);
        System.out.println("All root-to-leaf numbers: " + findAllNumbers(root4));
        System.out.println("Result: " + sol.sumNumbers(root4));

        // Example 5: Tree with zeros
        System.out.println("\nExample 5:");
        Integer[] values5 = { 1, 0, 1, 0, 1, 0, 1 };
        TreeNode root5 = buildTree(values5);
        System.out.println("Input: " + Arrays.toString(values5));
        System.out.print("Tree structure: ");
        printTree(root5);
        System.out.println("All root-to-leaf numbers: " + findAllNumbers(root5));
        System.out.println("Result: " + sol.sumNumbers(root5));
    }

}
