class SortedArrayToBST {

    public TreeNode sortedArrayToBST(int[] nums) {
        return constructBST(nums, 0, nums.length - 1);
    }

    private TreeNode constructBST(int[] nums, int start, int end) {
        if (start > end) {
            return null;
        }

        int mid = start + (end - start) / 2;

        TreeNode node = new TreeNode(nums[mid]);

        node.left = constructBST(nums, start, mid - 1);
        node.right = constructBST(nums, mid + 1, end);
        return node;
    }
}
