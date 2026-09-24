class SortedListToBST {

    private ListNode current;

    public TreeNode sortedListToBST(ListNode head) {
        int size = getSize(head);
        this.current = head;
        return buildBST(0, size - 1);
    }

    private int getSize(ListNode node) {
        int size = 0;
        while (node != null) {
            size++;
            node = node.next;
        }
        return size;
    }

    private TreeNode buildBST(int left, int right) {
        if (left > right) {
            return null;
        }
        int mid = left + (right - left) / 2;
        TreeNode leftChild = buildBST(left, mid - 1);

        TreeNode root = new TreeNode(current.val);
        current = current.next;

        TreeNode rightChild = buildBST(mid + 1, right);

        root.left = leftChild;
        root.right = rightChild;

        return root;
    }
}
