import java.util.Arrays;
import java.util.function.BinaryOperator;

// Basic Segment Tree for range sum queries
class SegmentTree {

    private int[] tree;
    private int n;

    public SegmentTree(int[] arr) {
        n = arr.length;
        tree = new int[4 * n]; // Safe size for segment tree
        build(arr, 0, 0, n - 1);
    }

    // Build the segment tree
    private void build(int[] arr, int node, int start, int end) {
        if (start == end) {
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            build(arr, 2 * node + 1, start, mid);
            build(arr, 2 * node + 2, mid + 1, end);
            tree[node] = tree[2 * node + 1] + tree[2 * node + 2];
        }
    }

    // Update a single element
    public void update(int idx, int value) {
        update(0, 0, n - 1, idx, value);
    }

    private void update(int node, int start, int end, int idx, int value) {
        if (start == end) {
            tree[node] = value;
        } else {
            int mid = (start + end) / 2;
            if (idx <= mid) {
                update(2 * node + 1, start, mid, idx, value);
            } else {
                update(2 * node + 2, mid + 1, end, idx, value);
            }
            tree[node] = tree[2 * node + 1] + tree[2 * node + 2];
        }
    }

    // Query sum in range [l, r]
    public int query(int l, int r) {
        return query(0, 0, n - 1, l, r);
    }

    private int query(int node, int start, int end, int l, int r) {
        if (r < start || end < l) {
            return 0; // Outside range
        }
        if (l <= start && end <= r) {
            return tree[node]; // Complete overlap
        }

        int mid = (start + end) / 2;
        int leftSum = query(2 * node + 1, start, mid, l, r);
        int rightSum = query(2 * node + 2, mid + 1, end, l, r);
        return leftSum + rightSum;
    }
}

// Generic Segment Tree with custom operations
class GenericSegmentTree<T> {
    private T[] tree;
    private int n;
    private BinaryOperator<T> operation;
    private T identity;

    @SuppressWarnings("unchecked")
    public GenericSegmentTree(T[] arr, BinaryOperator<T> op, T identity) {
        this.n = arr.length;
        this.operation = op;
        this.identity = identity;
        this.tree = (T[]) new Object[4 * n];
        build(arr, 0, 0, n - 1);
    }

    private void build(T[] arr, int node, int start, int end) {
        if (start == end) {
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            build(arr, 2 * node + 1, start, mid);
            build(arr, 2 * node + 2, mid + 1, end);
            tree[node] = operation.apply(tree[2 * node + 1], tree[2 * node + 2]);
        }
    }

    public void update(int idx, T value) {
        update(0, 0, n - 1, idx, value);
    }

    private void update(int node, int start, int end, int idx, T value) {
        if (start == end) {
            tree[node] = value;
        } else {
            int mid = (start + end) / 2;
            if (idx <= mid) {
                update(2 * node + 1, start, mid, idx, value);
            } else {
                update(2 * node + 2, mid + 1, end, idx, value);
            }
            tree[node] = operation.apply(tree[2 * node + 1], tree[2 * node + 2]);
        }
    }

    public T query(int l, int r) {
        return query(0, 0, n - 1, l, r);
    }

    private T query(int node, int start, int end, int l, int r) {
        if (r < start || end < l) {
            return identity;
        }
        if (l <= start && end <= r) {
            return tree[node];
        }

        int mid = (start + end) / 2;
        T leftResult = query(2 * node + 1, start, mid, l, r);
        T rightResult = query(2 * node + 2, mid + 1, end, l, r);
        return operation.apply(leftResult, rightResult);
    }
}

// Segment Tree with Lazy Propagation for range updates
class LazySegmentTree {
    private long[] tree;
    private long[] lazy;
    private int n;

    public LazySegmentTree(int[] arr) {
        n = arr.length;
        tree = new long[4 * n];
        lazy = new long[4 * n];
        build(arr, 0, 0, n - 1);
    }

    private void build(int[] arr, int node, int start, int end) {
        if (start == end) {
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            build(arr, 2 * node + 1, start, mid);
            build(arr, 2 * node + 2, mid + 1, end);
            tree[node] = tree[2 * node + 1] + tree[2 * node + 2];
        }
    }

    // Push lazy value down
    private void push(int node, int start, int end) {
        if (lazy[node] != 0) {
            tree[node] += lazy[node] * (end - start + 1);
            if (start != end) {
                lazy[2 * node + 1] += lazy[node];
                lazy[2 * node + 2] += lazy[node];
            }
            lazy[node] = 0;
        }
    }

    // Range update: add value to all elements in [l, r]
    public void updateRange(int l, int r, int value) {
        updateRange(0, 0, n - 1, l, r, value);
    }

    private void updateRange(int node, int start, int end, int l, int r, int value) {
        push(node, start, end);

        if (start > r || end < l) {
            return;
        }

        if (start >= l && end <= r) {
            lazy[node] += value;
            push(node, start, end);
            return;
        }

        int mid = (start + end) / 2;
        updateRange(2 * node + 1, start, mid, l, r, value);
        updateRange(2 * node + 2, mid + 1, end, l, r, value);

        push(2 * node + 1, start, mid);
        push(2 * node + 2, mid + 1, end);
        tree[node] = tree[2 * node + 1] + tree[2 * node + 2];
    }

    // Range query
    public long query(int l, int r) {
        return query(0, 0, n - 1, l, r);
    }

    private long query(int node, int start, int end, int l, int r) {
        if (start > r || end < l) {
            return 0;
        }

        push(node, start, end);

        if (start >= l && end <= r) {
            return tree[node];
        }

        int mid = (start + end) / 2;
        long leftSum = query(2 * node + 1, start, mid, l, r);
        long rightSum = query(2 * node + 2, mid + 1, end, l, r);
        return leftSum + rightSum;
    }

    // Point update
    public void updatePoint(int idx, int value) {
        updatePoint(0, 0, n - 1, idx, value);
    }

    private void updatePoint(int node, int start, int end, int idx, int value) {
        push(node, start, end);

        if (start == end) {
            tree[node] = value;
        } else {
            int mid = (start + end) / 2;
            if (idx <= mid) {
                updatePoint(2 * node + 1, start, mid, idx, value);
            } else {
                updatePoint(2 * node + 2, mid + 1, end, idx, value);
            }

            push(2 * node + 1, start, mid);
            push(2 * node + 2, mid + 1, end);
            tree[node] = tree[2 * node + 1] + tree[2 * node + 2];
        }
    }
}

// Main class for demonstration
public class SegmentTreeDemo {
    public static void main(String[] args) {
        System.out.println("=== Segment Tree Implementation Demo ===\n");

        // Test 1: Basic Segment Tree
        System.out.println("1. Basic Segment Tree (Range Sum Queries):");
        int[] arr = { 1, 3, 5, 7, 9, 11 };
        SegmentTree st = new SegmentTree(arr);

        System.out.println("Original array: " + Arrays.toString(arr));
        System.out.println("Sum of range [1, 3]: " + st.query(1, 3)); // 3 + 5 + 7 = 15
        System.out.println("Sum of range [0, 2]: " + st.query(0, 2)); // 1 + 3 + 5 = 9

        st.update(1, 10); // Change arr[1] from 3 to 10
        System.out.println("After updating index 1 to 10:");
        System.out.println("Sum of range [0, 2]: " + st.query(0, 2)); // 1 + 10 + 5 = 16

        // Test 2: Generic Segment Tree for Range Minimum Query
        System.out.println("\n2. Generic Segment Tree (Range Minimum Query):");
        Integer[] minArr = { 4, 2, 8, 1, 9, 3, 7 };
        GenericSegmentTree<Integer> minST = new GenericSegmentTree<>(
                minArr, Math::min, Integer.MAX_VALUE);

        System.out.println("Array: " + Arrays.toString(minArr));
        System.out.println("Minimum in range [1, 4]: " + minST.query(1, 4)); // min(2,8,1,9) = 1
        System.out.println("Minimum in range [0, 6]: " + minST.query(0, 6)); // min of all = 1

        minST.update(3, 0); // Change arr[3] from 1 to 0
        System.out.println("After updating index 3 to 0:");
        System.out.println("Minimum in range [0, 6]: " + minST.query(0, 6)); // Now 0

        // Test 3: Generic Segment Tree for Range Maximum Query
        System.out.println("\n3. Generic Segment Tree (Range Maximum Query):");
        Integer[] maxArr = { 4, 2, 8, 1, 9, 3, 7 };
        GenericSegmentTree<Integer> maxST = new GenericSegmentTree<>(
                maxArr, Math::max, Integer.MIN_VALUE);

        System.out.println("Array: " + Arrays.toString(maxArr));
        System.out.println("Maximum in range [1, 4]: " + maxST.query(1, 4)); // max(2,8,1,9) = 9
        System.out.println("Maximum in range [2, 6]: " + maxST.query(2, 6)); // max(8,1,9,3,7) = 9

        // Test 4: Lazy Propagation Segment Tree
        System.out.println("\n4. Lazy Propagation Segment Tree (Range Updates):");
        int[] lazyArr = { 1, 2, 3, 4, 5 };
        LazySegmentTree lazyST = new LazySegmentTree(lazyArr);

        System.out.println("Original array: " + Arrays.toString(lazyArr));
        System.out.println("Sum of range [0, 4]: " + lazyST.query(0, 4)); // 1+2+3+4+5 = 15

        lazyST.updateRange(1, 3, 10); // Add 10 to elements at indices 1, 2, 3
        System.out.println("After adding 10 to range [1, 3]:");
        System.out.println("Sum of range [0, 4]: " + lazyST.query(0, 4)); // 1+12+13+14+5 = 45
        System.out.println("Sum of range [1, 3]: " + lazyST.query(1, 3)); // 12+13+14 = 39

        lazyST.updatePoint(0, 100); // Set arr[0] to 100
        System.out.println("After setting index 0 to 100:");
        System.out.println("Sum of range [0, 4]: " + lazyST.query(0, 4)); // 100+12+13+14+5 = 144

        // Test 5: Performance comparison
        System.out.println("\n5. Performance Test:");
        int[] largeArr = new int[100000];
        for (int i = 0; i < largeArr.length; i++) {
            largeArr[i] = i + 1;
        }

        long startTime = System.nanoTime();
        SegmentTree largeST = new SegmentTree(largeArr);
        long buildTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        long sum = largeST.query(10000, 50000);
        long queryTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        largeST.update(25000, 999999);
        long updateTime = System.nanoTime() - startTime;

        System.out.println("Array size: " + largeArr.length);
        System.out.println("Build time: " + buildTime / 1000 + " microseconds");
        System.out.println("Query time: " + queryTime / 1000 + " microseconds");
        System.out.println("Update time: " + updateTime / 1000 + " microseconds");
        System.out.println("Sum of range [10000, 50000]: " + sum);

        // Test 6: Use cases demonstration
        System.out.println("\n6. Real-world Use Cases:");

        // Stock price analysis
        Integer[] stockPrices = { 100, 110, 95, 120, 80, 140, 90, 160 };
        GenericSegmentTree<Integer> stockMinST = new GenericSegmentTree<>(
                stockPrices, Math::min, Integer.MAX_VALUE);
        GenericSegmentTree<Integer> stockMaxST = new GenericSegmentTree<>(
                stockPrices, Math::max, Integer.MIN_VALUE);

        System.out.println("Stock prices: " + Arrays.toString(stockPrices));
        System.out.println("Lowest price in week 1-3: $" + stockMinST.query(1, 3));
        System.out.println("Highest price in week 2-5: $" + stockMaxST.query(2, 5));

        // Range sum for sensor readings with bulk updates
        int[] sensorReadings = { 25, 30, 28, 35, 32, 29, 31 };
        LazySegmentTree sensorST = new LazySegmentTree(sensorReadings);

        System.out.println("\nSensor readings: " + Arrays.toString(sensorReadings));
        System.out.println("Total reading sum: " + sensorST.query(0, 6));

        // Calibration adjustment - subtract 2 from sensors 2-4
        sensorST.updateRange(2, 4, -2);
        System.out.println("After calibration adjustment (-2 to sensors 2-4):");
        System.out.println("Total reading sum: " + sensorST.query(0, 6));
        System.out.println("Sum of adjusted sensors [2-4]: " + sensorST.query(2, 4));
    }

}
