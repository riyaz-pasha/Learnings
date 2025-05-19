class SumSegmentTree {
    private final int[] segmentTree;
    private final int size;

    public SumSegmentTree(int[] inputArray) {
        this.size = inputArray.length;
        int treeSize = 4 * this.size;
        this.segmentTree = new int[treeSize];
        this.buildSegementTree(inputArray, 0, this.size - 1, 0);
    }

    private void buildSegementTree(int[] inputArray, int left, int right, int nodeIndex) {
        if (left == right) {
            this.segmentTree[nodeIndex] = inputArray[left];
            return;
        }
        int mid = (left + right) / 2;
        int leftNodeIndex = (2 * nodeIndex) + 1;
        int rightNodeIndex = (2 * nodeIndex) + 2;
        this.buildSegementTree(inputArray, left, mid, leftNodeIndex);
        this.buildSegementTree(inputArray, mid + 1, right, rightNodeIndex);
        this.segmentTree[nodeIndex] = this.segmentTree[leftNodeIndex] + this.segmentTree[rightNodeIndex];
    }

    public int rangeSumQuery(int queryLeft, int queryRight) {
        return this.rangeSumQueryUtil(0, size - 1, queryLeft, queryRight, 0);
    }

    private int rangeSumQueryUtil(int segLeft, int segRight, int qLeft, int qRight, int nodeIndex) {
        // no overlap
        if (qRight < segLeft || qLeft > segRight) {
            return 0;
        }

        // total overlap
        if (qLeft <= segLeft && segRight <= qRight) {
            return this.segmentTree[nodeIndex];
        }

        // partial overlap
        int mid = (segLeft + segRight) / 2;
        int leftSum = this.rangeSumQueryUtil(segLeft, mid, qLeft, qRight, (2 * nodeIndex) + 1);
        int rightSum = this.rangeSumQueryUtil(mid + 1, segRight, qLeft, qRight, (2 + nodeIndex) + 2);
        return leftSum + rightSum;
    }
}
