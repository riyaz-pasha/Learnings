import java.util.HashMap;
import java.util.Map;

class ConstructBinaryTreeFromPostorderAndInorder {

    public TreeNode buildTree(int[] inorder, int[] postorder) {
        Map<Integer, Integer> inorderIndexMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderIndexMap.put(inorder[i], i);
        }

        return this.build(postorder, 0, postorder.length - 1,
                inorder, 0, inorder.length - 1,
                inorderIndexMap);
    }

    private TreeNode build(int[] postorder, int postStart, int postEnd,
            int[] inorder, int inStart, int inEnd,
            Map<Integer, Integer> inorderIndexMap) {

        if (postStart > postEnd || inStart > inEnd) {
            return null;
        }

        int rootVal = postorder[postEnd];
        int rootInorderIndex = inorderIndexMap.get(rootVal);
        int leftSize = rootInorderIndex - inStart;

        TreeNode root = new TreeNode(rootVal);

        root.left = this.build(postorder, postStart, postStart + (leftSize - 1),
                inorder, inStart, rootInorderIndex - 1,
                inorderIndexMap);

        root.right = this.build(postorder, postStart + leftSize, postEnd - 1,
                inorder, rootInorderIndex + 1, inEnd,
                inorderIndexMap);

        return root;
    }

}
