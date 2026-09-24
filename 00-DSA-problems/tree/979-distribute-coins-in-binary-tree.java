/*
 * You are given the root of a binary tree with n nodes where each node in the
 * tree has node.val coins. There are n coins in total throughout the whole
 * tree.
 * 
 * In one move, we may choose two adjacent nodes and move one coin from one node
 * to another. A move may be from parent to child, or from child to parent.
 * 
 * Return the minimum number of moves required to make every node have exactly
 * one coin.
 * 
 * 
 * 
 * Example 1:
 * 
 * 
 * Input: root = [3,0,0]
 * Output: 2
 * Explanation: From the root of the tree, we move one coin to its left child,
 * and one coin to its right child.
 * Example 2:
 * 
 * 
 * Input: root = [0,3,0]
 * Output: 3
 * Explanation: From the left child of the root, we move two coins to the root
 * [taking two moves]. Then, we move one coin from the root of the tree to the
 * right child.
 */

 /**
 * Definition for a binary tree node.
 */
class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;
    TreeNode(int val) { this.val = val; }
}

class Solution {

    private int totalMoves = 0;
    
    /**
     * Main function to calculate minimum moves to distribute coins
     * Time Complexity: O(n) - visit each node once
     * Space Complexity: O(h) - recursion stack height, where h is tree height
     */
    public int distributeCoins(TreeNode root) {
        totalMoves = 0;
        dfs(root);
        return totalMoves;
    }
    
    /**
     * DFS helper function that returns the excess/deficit of coins for subtree
     * Positive return value: subtree has excess coins that need to be moved out
     * Negative return value: subtree needs coins from parent/ancestor
     * Zero return value: subtree is perfectly balanced
     */
    private int dfs(TreeNode node) {
        if (node == null) {
            return 0;
        }
        
        // Get excess/deficit from left and right subtrees
        int leftExcess = dfs(node.left);
        int rightExcess = dfs(node.right);
        
        // Add absolute values to total moves
        // We need to move |leftExcess| coins between node and left child
        // We need to move |rightExcess| coins between node and right child
        totalMoves += Math.abs(leftExcess) + Math.abs(rightExcess);
        
        // Calculate current node's contribution to parent
        // node.val - 1: current node keeps 1 coin, rest is excess/deficit
        // + leftExcess + rightExcess: add what children contribute
        return node.val - 1 + leftExcess + rightExcess;
    }
}

// Alternative implementation with cleaner variable names
class SolutionAlternative {
    private int moves = 0;
    
    public int distributeCoins(TreeNode root) {
        moves = 0;
        calculateBalance(root);
        return moves;
    }
    
    /**
     * Returns the net balance of coins for the subtree rooted at node
     * Positive: excess coins, Negative: deficit coins
     */
    private int calculateBalance(TreeNode node) {
        if (node == null) return 0;
        
        int leftBalance = calculateBalance(node.left);
        int rightBalance = calculateBalance(node.right);
        
        // Count moves needed to balance left and right subtrees
        moves += Math.abs(leftBalance) + Math.abs(rightBalance);
        
        // Return net balance: coins at node + balances from children - 1 (keep 1 coin)
        return node.val + leftBalance + rightBalance - 1;
    }

}

// Test cases
class TestCases {

    public static void main(String[] args) {
        Solution solution = new Solution();
        
        // Test Case 1: [3,0,0]
        TreeNode root1 = new TreeNode(3);
        root1.left = new TreeNode(0);
        root1.right = new TreeNode(0);
        System.out.println("Test 1 - Expected: 2, Got: " + solution.distributeCoins(root1));
        
        // Test Case 2: [0,3,0]
        TreeNode root2 = new TreeNode(0);
        root2.left = new TreeNode(3);
        root2.right = new TreeNode(0);
        System.out.println("Test 2 - Expected: 3, Got: " + solution.distributeCoins(root2));
        
        // Test Case 3: [1,0,2]
        TreeNode root3 = new TreeNode(1);
        root3.left = new TreeNode(0);
        root3.right = new TreeNode(2);
        System.out.println("Test 3 - Expected: 2, Got: " + solution.distributeCoins(root3));
        
        // Test Case 4: [1,0,0,null,3]
        TreeNode root4 = new TreeNode(1);
        root4.left = new TreeNode(0);
        root4.right = new TreeNode(0);
        root4.left.right = new TreeNode(3);
        System.out.println("Test 4 - Expected: 4, Got: " + solution.distributeCoins(root4));
    }

}
