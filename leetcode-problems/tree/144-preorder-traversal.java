import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

class BinaryTreePreorderTraversal {

    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        this.preorderTraversal(root, result);
        return result;
    }

    private void preorderTraversal(TreeNode root, List<Integer> result) {
        if (root == null)
            return;
        result.add(root.val);
        this.preorderTraversal(root.left, result);
        this.preorderTraversal(root.right, result);
    }
}

class BinaryTreePreorderIterativeTraversal {

    public List<Integer> preorderTraversal(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null)
            return result;

        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            TreeNode current = stack.pop();
            result.add(current.val);
            if (current.right != null) {
                stack.push(current.right);
            }
            if (current.left != null) {
                stack.push(current.left);
            }
        }
        return result;
    }

}
