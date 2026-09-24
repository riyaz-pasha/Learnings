class UnionFindBasic {
    private int[] parent;

    public UnionFindBasic(int n) {
        parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i; // Each element is initially its own set
        }
    }

    public int find(int i) {
        if (parent[i] == i) {
            return i;
        }
        return find(parent[i]);
    }

    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);
        if (rootI != rootJ) {
            parent[rootJ] = rootI;
        }
    }

    public static void main(String[] args) {
        UnionFindBasic uf = new UnionFindBasic(5); // 5 elements: 0, 1, 2, 3, 4
        uf.union(0, 1);
        uf.union(2, 3);
        uf.union(1, 4);

        System.out.println(uf.find(0)); // Output: 4 (or 1, depending on union order)
        System.out.println(uf.find(4)); // Output: 4
        System.out.println(uf.find(2)); // Output: 3
        System.out.println(uf.find(0) == uf.find(2)); // Output: false
    }
}
