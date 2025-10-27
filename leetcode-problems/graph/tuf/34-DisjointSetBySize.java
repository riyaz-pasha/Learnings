
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DisjointSet {

    private int[] parent;
    private int[] size;

    public DisjointSet(int n) {
        parent = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = this.find(parent[x]);
        }
        return parent[x];
    }

    public void union(int x, int y) {
        int rootX = this.find(x);
        int rootY = this.find(y);
        if (rootX == rootY) {
            return;
        }
        if (size[rootX] < size[rootY]) {
            parent[rootX] = rootY;
            size[rootY] += size[rootX];
        } else {
            parent[rootY] = rootX;
            size[rootX] += size[rootY];
        }
    }

}

class DisjointSet2 {

    private List<Integer> parent;
    private List<Integer> size;
    private int count; // number of disjoint sets

    // Constructor: creates n disjoint sets
    public DisjointSet2(int n) {
        parent = new ArrayList<>();
        size = new ArrayList<>();
        count = n;

        // Initially, each element is its own parent with size 1
        for (int i = 0; i < n; i++) {
            parent.add(i);
            size.add(1);
        }
    }

    // Find operation with path compression
    public int find(int x) {
        if (parent.get(x) != x) {
            parent.set(x, find(parent.get(x))); // path compression
        }
        return parent.get(x);
    }

    // Union operation with union by size
    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        // Already in same set
        if (rootX == rootY) {
            return false;
        }

        // Union by size: attach smaller tree under larger tree
        if (size.get(rootX) < size.get(rootY)) {
            parent.set(rootX, rootY);
            size.set(rootY, size.get(rootY) + size.get(rootX));
        } else {
            parent.set(rootY, rootX);
            size.set(rootX, size.get(rootX) + size.get(rootY));
        }

        count--;
        return true;
    }

    // Check if two elements are in the same set
    public boolean connected(int x, int y) {
        return find(x) == find(y);
    }

    // Get the number of disjoint sets
    public int getCount() {
        return count;
    }

    // Get the size of the set containing element x
    public int getSize(int x) {
        return size.get(find(x));
    }

    // Main method with example usage
    public static void main(String[] args) {
        DisjointSet2 ds = new DisjointSet2(10);

        System.out.println("Initial count: " + ds.getCount());

        // Perform some unions
        ds.union(0, 1);
        ds.union(2, 3);
        ds.union(4, 5);
        ds.union(6, 7);
        ds.union(1, 3); // connects {0,1} with {2,3}

        System.out.println("Count after unions: " + ds.getCount());
        System.out.println("Size of set containing 0: " + ds.getSize(0));

        // Check connectivity
        System.out.println("0 and 3 connected? " + ds.connected(0, 3)); // true
        System.out.println("0 and 4 connected? " + ds.connected(0, 4)); // false

        // Connect more sets
        ds.union(0, 4); // connects {0,1,2,3} with {4,5}
        System.out.println("0 and 5 connected? " + ds.connected(0, 5)); // true
        System.out.println("Size of set containing 0: " + ds.getSize(0));

        System.out.println("Final count: " + ds.getCount());
    }

}

class DisjointSetBySize<T> {

    private final Map<T, T> parent = new HashMap<>();
    private final Map<T, Integer> size = new HashMap<>();

    // Initialize a node (if not already present)
    public void makeSet(T item) {
        parent.putIfAbsent(item, item);
        size.putIfAbsent(item, 1);
    }

    // Find operation with path compression
    public T find(T item) {
        if (!parent.containsKey(item)) {
            makeSet(item); // auto-register unknown nodes
        }

        T p = parent.get(item);
        if (!p.equals(item)) {
            parent.put(item, find(p)); // path compression
        }
        return parent.get(item);
    }

    // Union by size
    public void unionBySize(T a, T b) {
        T rootA = find(a);
        T rootB = find(b);

        if (rootA.equals(rootB))
            return; // already in same set

        int sizeA = size.get(rootA);
        int sizeB = size.get(rootB);

        // Attach smaller tree to larger
        if (sizeA < sizeB) {
            parent.put(rootA, rootB);
            size.put(rootB, sizeA + sizeB);
        } else {
            parent.put(rootB, rootA);
            size.put(rootA, sizeA + sizeB);
        }
    }

    // Check if two nodes are in same set
    public boolean isConnected(T a, T b) {
        return find(a).equals(find(b));
    }

    // Get size of component containing an element
    public int getSize(T item) {
        return size.get(find(item));
    }

    // Optional: to visualize parent mapping
    public void printSets() {
        System.out.println("Parent map: " + parent);
        System.out.println("Size map: " + size);
    }
}

/*
 * ⏱️ Time Complexity
 * 
 * find(): ~O(α(n))
 * 
 * unionBySize(): ~O(α(n))
 * (α(n) ≈ constant in practice)
 */
