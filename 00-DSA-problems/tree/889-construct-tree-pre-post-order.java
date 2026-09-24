import java.util.HashMap;
import java.util.Map;

class ConstructFromPreorderAndPostorder {

    public TreeNode constructFromPrePost(int[] preorder, int[] postorder) {
        Map<Integer, Integer> postIndexMap = new HashMap<>();
        for (int i = 0; i < postorder.length; i++) {
            postIndexMap.put(postorder[i], i);
        }

        return this.build(preorder, 0, preorder.length - 1,
                postorder, 0, postorder.length - 1,
                postIndexMap);
    }

    private TreeNode build(int[] preorder, int preStart, int preEnd,
            int[] postorder, int postStart, int postEnd,
            Map<Integer, Integer> postIndexMap) {

        if (preStart > preEnd)
            return null;

        TreeNode root = new TreeNode(preorder[preStart]);
        if (preStart == preEnd)
            return root;

        int leftRootVal = preorder[preStart + 1];
        int postLeftRootIndex = postIndexMap.get(leftRootVal);
        int leftSize = postLeftRootIndex - postStart + 1;

        root.left = this.build(preorder, preStart + 1, (preStart + 1) + (leftSize - 1),
                postorder, postStart, postLeftRootIndex,
                postIndexMap);
        root.right = this.build(preorder, (preStart + 1) + leftSize, preEnd,
                postorder, postLeftRootIndex + 1, postEnd - 1,
                postIndexMap);

        return root;
    }

}
