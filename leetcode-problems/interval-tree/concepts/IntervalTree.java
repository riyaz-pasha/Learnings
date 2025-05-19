public class IntervalTree {
    private IntervalTreeNode root;

    public IntervalTree() {
        this.root = null;
    }

    public boolean book(int start, int end) {
        if (this.canInsert(root, start, end)) {
            this.root = this.insert(root, start, end);
            return true;
        }
        return false;
    }

    private boolean canInsert(IntervalTreeNode node, int start, int end) {
        if (node == null) {
            return true;
        }
        if (end <= node.start) {
            return this.canInsert(node.left, start, end);
        } else if (start >= node.end) {
            return this.canInsert(node.right, start, end);
        } else {
            return false;
        }
    }

    private IntervalTreeNode insert(IntervalTreeNode node, int start, int end) {
        if (node == null) {
            return new IntervalTreeNode(start, end);
        }
        if (end <= node.start) {
            node.left = this.insert(node.left, start, end);
        } else {
            node.right = this.insert(node.right, start, end);
        }
        return node;
    }

}

class IntervalTreeNode {
    int start, end;
    IntervalTreeNode left, right;

    public IntervalTreeNode(int start, int end) {
        this.start = start;
        this.end = end;
    }
}
