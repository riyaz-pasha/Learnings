public class IntervalTreeV2 {
    private IntervalNode root;

    public boolean insert(int start, int end) {
        if (this.overlaps(start, end, root)) {
            return false;
        }
        this.root = this.insert(start, end, root);
        return true;
    }

    private boolean overlaps(int start, int end, IntervalNode node) {
        if (node == null) {
            return false;
        }
        if (start < node.end && end > node.start) {
            return true;
        }
        if (node.left != null && node.left.max >= start) {
            return this.overlaps(start, end, node.left);
        }
        return this.overlaps(start, end, node.right);
    }

    private IntervalNode insert(int start, int end, IntervalNode node) {
        if (node == null) {
            return new IntervalNode(start, end);
        }
        if (start < node.start) {
            node.left = this.insert(start, end, node.left);
        } else {
            node.right = this.insert(start, end, node.right);
        }
        node.max = Math.max(node.max, end);
        return node;
    }

}

class IntervalNode {
    int start, end, max;
    IntervalNode left, right;

    public IntervalNode(int start, int end) {
        this.start = start;
        this.end = end;
        this.max = end;
    }

}

//          [15,18] max=45
//        /              \
//    [5,7] max=12    [30,35] max=45
//    /       \          /        \
// [1,3]   [10,12]   [20,25]   [40,45]
// max=3   max=12    max=25    max=45
