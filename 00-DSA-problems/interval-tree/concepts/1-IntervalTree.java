class Interval {

    int start, end;

    public Interval(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + "]";
    }

}

class IntervalTreeNode {

    Interval interval;
    int maxEnd; // Maximum endpoint in this subtree
    IntervalTreeNode left, right;

    public IntervalTreeNode(Interval interval) {
        this.interval = interval;
        this.maxEnd = interval.end;
        this.left = this.right = null;
    }

}

class IntervalTree {

    private IntervalTreeNode root;

    // Check if two intervals overlap
    private boolean doOverlap(Interval a, Interval b) {
        return a.start <= b.end && b.start <= a.end;
    }

    // Insert an interval into the tree
    public void insert(Interval interval) {
        root = insertHelper(root, interval);
    }

    private IntervalTreeNode insertHelper(IntervalTreeNode node, Interval interval) {
        // Base case: create new node
        if (node == null) {
            return new IntervalTreeNode(interval);
        }

        // Update max endpoint for current node
        node.maxEnd = Math.max(node.maxEnd, interval.end);

        // Insert based on start time (BST property)
        if (interval.start < node.interval.start) {
            node.left = insertHelper(node.left, interval);
        } else {
            node.right = insertHelper(node.right, interval);
        }

        return node;
    }

    // Search for an interval that overlaps with given interval
    public Interval searchOverlap(Interval target) {
        return searchOverlapHelper(root, target);
    }

    private Interval searchOverlapHelper(IntervalTreeNode node, Interval target) {
        // Base case: empty tree
        if (node == null) {
            return null;
        }

        // Check if current interval overlaps
        if (doOverlap(node.interval, target)) {
            return node.interval;
        }

        // Decide which subtree to search
        // Key insight: if left subtree's max endpoint < target.start,
        // then no interval in left subtree can overlap with target
        if (node.left != null && node.left.maxEnd >= target.start) {
            return searchOverlapHelper(node.left, target);
        } else {
            return searchOverlapHelper(node.right, target);
        }
    }

    // Find all overlapping intervals
    public void findAllOverlaps(Interval target) {
        System.out.println("Intervals overlapping with " + target + ":");
        findAllOverlapsHelper(root, target);
    }

    private void findAllOverlapsHelper(IntervalTreeNode node, Interval target) {
        if (node == null)
            return;

        // Check current interval
        if (doOverlap(node.interval, target)) {
            System.out.println("  " + node.interval);
        }

        // Search left subtree if it might contain overlaps
        if (node.left != null && node.left.maxEnd >= target.start) {
            findAllOverlapsHelper(node.left, target);
        }

        // Search right subtree if current interval's start <= target.end
        if (node.right != null && node.interval.start <= target.end) {
            findAllOverlapsHelper(node.right, target);
        }
    }

    // In-order traversal to display tree structure
    public void inOrder() {
        System.out.println("Tree contents (in-order):");
        inOrderHelper(root);
        System.out.println();
    }

    private void inOrderHelper(IntervalTreeNode node) {
        if (node != null) {
            inOrderHelper(node.left);
            System.out.println("  " + node.interval + " (maxEnd: " + node.maxEnd + ")");
            inOrderHelper(node.right);
        }
    }

    // Demo and test the interval tree
    public static void main(String[] args) {
        IntervalTree tree = new IntervalTree();

        // Insert some intervals
        System.out.println("Inserting intervals...");
        tree.insert(new Interval(15, 20));
        tree.insert(new Interval(10, 30));
        tree.insert(new Interval(17, 19));
        tree.insert(new Interval(5, 20));
        tree.insert(new Interval(12, 15));
        tree.insert(new Interval(30, 40));

        // Display tree
        tree.inOrder();

        // Search for overlaps
        System.out.println("=== Search Tests ===");
        Interval target1 = new Interval(14, 16);
        Interval result1 = tree.searchOverlap(target1);
        System.out.println("Searching for overlap with " + target1);
        System.out.println("Found: " + (result1 != null ? result1 : "No overlap"));
        System.out.println();

        Interval target2 = new Interval(21, 23);
        Interval result2 = tree.searchOverlap(target2);
        System.out.println("Searching for overlap with " + target2);
        System.out.println("Found: " + (result2 != null ? result2 : "No overlap"));
        System.out.println();

        // Find all overlaps
        System.out.println("=== Find All Overlaps ===");
        tree.findAllOverlaps(new Interval(14, 16));
        System.out.println();

        tree.findAllOverlaps(new Interval(21, 23));
    }

}
