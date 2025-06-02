class RecoverBST {

    private TreeNode first, second, prev;

    public void recoverTree(TreeNode root) {
        first = second = prev = null;

        inorder(root);

        if (first != null && second != null) {
            int temp = first.val;
            first.val = second.val;
            second.val = temp;
        }
    }

    private void inorder(TreeNode node) {
        if (node == null) {
            return;
        }

        inorder(node.left);

        if (prev != null && prev.val > node.val) {
            if (first == null) {
                first = prev;
                second = node;
            } else {
                second = node;
            }
        }
        prev = node;
        inorder(node.right);
    }

}
