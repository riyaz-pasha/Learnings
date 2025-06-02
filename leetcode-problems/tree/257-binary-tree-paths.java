import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class BinaryTreePaths {

    public List<String> binaryTreePaths(TreeNode root) {
        List<String> result = new ArrayList<>();
        List<String> path = new ArrayList<>();
        preorder(root, path, result);
        return result;
    }

    private void preorder(TreeNode node, List<String> path, List<String> result) {
        if (node == null) {
            return;
        }

        path.add(String.valueOf(node.val));
        if (node.left == null && node.right == null) {
            result.add(path.stream().collect(Collectors.joining("->")));
        } else {
            preorder(node.left, path, result);
            preorder(node.right, path, result);
        }
        path.removeLast();
    }

}

class Solution {

    public List<String> binaryTreePaths(TreeNode root) {
        List<String> result = new ArrayList<>();
        StringBuilder path = new StringBuilder();
        dfs(root, path, result);
        return result;
    }

    private void dfs(TreeNode node, StringBuilder path, List<String> result) {
        if (node == null)
            return;

        int len = path.length(); // Save current length to backtrack later

        // Append current node's value
        path.append(node.val);

        if (node.left == null && node.right == null) {
            // Leaf node, add path to result
            result.add(path.toString());
        } else {
            // Not a leaf, explore children
            path.append("->");
            dfs(node.left, path, result);
            dfs(node.right, path, result);
        }

        // Backtrack to previous state
        path.setLength(len);
    }

}
