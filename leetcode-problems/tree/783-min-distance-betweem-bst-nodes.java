class MinDistanceBetweenBSTNodes {

    private int min = Integer.MAX_VALUE;
    private Integer prev = null;

    public int minDiffInBST(TreeNode root) {
        inorder(root);
        return min;
    }

    private void inorder(TreeNode node) {
        if (node == null) {
            return;
        }
        inorder(node.left);

        if (prev != null) {
            min = Math.min(min, node.val - prev);
        }
        prev = node.val;

        inorder(node.right);
    }

}
