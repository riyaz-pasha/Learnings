class ValidateBST {

    public boolean isValidBST(TreeNode root) {
        return isValid(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private boolean isValid(TreeNode root, long min, long max) {
        if (root == null) {
            return true;
        }
        if (root.val <= min || root.val >= max) {
            return false;
        }
        return isValid(root.left, min, root.val)
                && isValid(root.right, root.val, max);
    }

}

class ValidateBST {

    private Integer prev;

    public boolean isValidBST(TreeNode root) {
        prev = null;
        return inorder(root);
    }

    private boolean inorder(TreeNode root) {
        if (root == null)
            return true;

        if (!inorder(root.left))
            return false;

        if (prev != null && prev >= root.val) {
            return false;
        }

        prev = root.val;
        return inorder(root.right);
    }

}
