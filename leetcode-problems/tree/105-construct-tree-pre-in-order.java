import java.util.HashMap;
import java.util.Map;

class ConstructBinaryTreeFromPreorderAndInorder {

    public TreeNode buildTree(int[] preorder, int[] inorder) {
        Map<Integer, Integer> inorderIndexMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderIndexMap.put(inorder[i], i);
        }
        return this.build(preorder, 0, preorder.length - 1,
                inorder, 0, inorder.length - 1,
                inorderIndexMap);
    }

    private TreeNode build(int[] preorder, int preStart, int preEnd,
            int[] inorder, int inStart, int inEnd,
            Map<Integer, Integer> inorderIndexMap) {

        if (preStart > preEnd || inStart > inEnd) {
            return null;
        }

        int rootVal = preorder[preStart];
        int rootInorderIndex = inorderIndexMap.get(rootVal);
        int leftSize = rootInorderIndex - inStart;

        TreeNode root = new TreeNode(rootVal);

        root.left = this.build(preorder, preStart + 1, (preStart + 1) + (leftSize - 1),
                inorder, inStart, rootInorderIndex - 1,
                inorderIndexMap);

        root.right = this.build(preorder, (preStart + 1) + leftSize, preEnd,
                inorder, rootInorderIndex + 1, inEnd,
                inorderIndexMap);

        return root;
    }

}
