class BalancedBinaryTree {
   public boolean isBalanced(TreeNode root) {
        return checkBalancedAndHeight(root) != -1;
    }

    private int checkBalancedAndHeight(TreeNode node) {
        if (node == null) {
            return 0;
        }

        int left = checkBalancedAndHeight(node.left);
        if (left == -1) {
            return -1;
        }

        int right = checkBalancedAndHeight(node.right);
        if (right == -1) {
            return -1;
        }

        if (Math.abs(left - right) > 1) {
            return -1;
        }

        return 1 + Math.max(left, right);
    }

}
