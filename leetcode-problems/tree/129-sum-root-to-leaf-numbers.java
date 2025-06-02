import java.util.ArrayList;
import java.util.List;

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

class SumRootToLeafNumbers {

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
