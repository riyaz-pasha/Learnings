/**
 * Interval Tree implementation.
 *
 * Supports:
 *  - Inserting intervals (overlaps allowed)
 *  - Searching for any overlapping interval
 *
 * Core idea:
 *  - BST ordered by interval.start
 *  - Each node stores maxEnd = maximum end value in its subtree
 *
 * Why maxEnd?
 *  - It allows us to skip (prune) entire subtrees that cannot
 *    possibly contain an overlapping interval.
 *
 * Time Complexity:
 *  - Insert: O(log N) average, O(N) worst-case (unbalanced)
 *  - Overlap search: O(log N) average, O(N) worst-case
 *  - Space: O(N)
 */
class IntervalTree {

    /* ======================= INTERVAL ======================= */

    /**
     * Half-open interval [start, end)
     * end is NOT included.
     */
    record Interval(int start, int end) { }

    /* ======================= NODE ======================= */

    /**
     * A node in the Interval Tree.
     */
    private static class IntervalNode {

        Interval interval;   // interval stored at this node
        int maxEnd;          // maximum end in this subtree

        IntervalNode left;
        IntervalNode right;

        IntervalNode(Interval interval) {
            this.interval = interval;
            this.maxEnd = interval.end(); // initial maxEnd
        }
    }

    /* ======================= TREE ======================= */

    private IntervalNode root;

    /* ======================= INSERT ======================= */

    /**
     * Inserts a new interval into the tree.
     *
     * Average Time: O(log N)
     * Worst Time:   O(N)  (if tree becomes skewed)
     */
    public void insert(Interval interval) {
        root = insert(root, interval);
    }

    private IntervalNode insert(IntervalNode node, Interval interval) {

        // Base case: empty position in tree
        if (node == null) {
            return new IntervalNode(interval);
        }

        // BST ordering by interval.start
        if (interval.start() < node.interval.start()) {
            node.left = insert(node.left, interval);
        } else {
            node.right = insert(node.right, interval);
        }

        /*
         * Recompute maxEnd correctly.
         *
         * maxEnd must represent:
         *  - this node's interval end
         *  - left subtree maxEnd
         *  - right subtree maxEnd
         *
         * This is CRITICAL for safe pruning during queries.
         */
        node.maxEnd = Math.max(
                node.interval.end(),
                Math.max(
                        node.left  != null ? node.left.maxEnd  : Integer.MIN_VALUE,
                        node.right != null ? node.right.maxEnd : Integer.MIN_VALUE
                )
        );

        return node;
    }

    /* ======================= SEARCH OVERLAP ======================= */

    /**
     * Searches for ANY interval that overlaps with the target.
     *
     * Returns:
     *  - an overlapping Interval if found
     *  - null if no overlap exists
     *
     * Average Time: O(log N)
     * Worst Time:   O(N)
     */
    public Interval searchOverlap(Interval target) {
        return searchOverlap(root, target);
    }

    private Interval searchOverlap(IntervalNode node, Interval target) {

        // Base case: reached empty subtree
        if (node == null) {
            return null;
        }

        // Case 1: current node overlaps with target
        if (overlaps(node.interval, target)) {
            return node.interval;
        }

        /*
         * Case 2: check left subtree only if it MAY contain overlap.
         *
         * If left.maxEnd <= target.start:
         *  - all intervals in left subtree end before target starts
         *  - overlap is impossible
         *
         * Otherwise, overlap MAY exist → search left subtree.
         */
        if (node.left != null && node.left.maxEnd > target.start()) {
            return searchOverlap(node.left, target);
        }

        // Case 3: otherwise, search right subtree
        return searchOverlap(node.right, target);
    }

    /* ======================= OVERLAP CHECK ======================= */

    /**
     * Checks overlap between two half-open intervals [start, end).
     *
     * Overlap condition:
     *  a.start < b.end AND b.start < a.end
     */
    private boolean overlaps(Interval a, Interval b) {
        return a.start() < b.end() && b.start() < a.end();
    }

    /* ======================= DEMO ======================= */

    public static void main(String[] args) {

        IntervalTree tree = new IntervalTree();

        // Insert intervals
        tree.insert(new Interval(15, 20));
        tree.insert(new Interval(10, 30));
        tree.insert(new Interval(17, 19));
        tree.insert(new Interval(5, 20));
        tree.insert(new Interval(12, 15));
        tree.insert(new Interval(30, 40));

        // Query overlap
        Interval target = new Interval(14, 16);
        Interval result = tree.searchOverlap(target);

        System.out.println(
                result != null
                        ? "Overlaps with: " + result
                        : "No overlap found"
        );
    }
}
