import java.util.ArrayList;
import java.util.List;

class PathSumII {

    public List<List<Integer>> pathSum(TreeNode root, int targetSum) {
        List<List<Integer>> result = new ArrayList<>();
        List<Integer> path = new ArrayList<>();
        preorder(root, targetSum, path, result);
        return result;
    }

    public void preorder(TreeNode node, int target,
            List<Integer> path, List<List<Integer>> result) {

        if (node == null)
            return;

        path.add(node.val);

        if (node.left == null && node.right == null && node.val == target) {
            result.add(new ArrayList<>(path));
        } else {
            preorder(node.left, target - node.val, path, result);
            preorder(node.right, target - node.val, path, result);
        }

        path.removeLast();
    }

}
