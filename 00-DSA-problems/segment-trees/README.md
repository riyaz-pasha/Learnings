### 🧠 What is a Segment Tree?

A **Segment Tree** is a **binary tree** used for answering **range queries** (like sum, min, max) and **updating elements** in an array efficiently.

* Time Complexity:

  * Build: `O(n)`
  * Query: `O(log n)`
  * Update: `O(log n)`
* Space Complexity: `O(4 * n)` to safely store tree nodes.

---

### Example Use Case:

Given an array:

```java
int[] arr = {1, 3, 5, 7, 9, 11};
```

You want to:

* **Query the sum** from index `1` to `3` (i.e., `3 + 5 + 7 = 15`)
* **Update** value at index `1` from `3` to `10` efficiently.

---

### ✅ Java Code with Detailed Comments

```java
public class SegmentTree {
    int[] tree; // Segment tree array
    int n;      // Original array size

    public SegmentTree(int[] arr) {
        n = arr.length;
        // Allocate memory for segment tree (4 * n is safe upper bound)
        tree = new int[4 * n];
        buildTree(arr, 0, 0, n - 1);
    }

    /*
        Builds the segment tree recursively.
        node: index in segment tree array
        start, end: range in original array that this node covers
    */
    private void buildTree(int[] arr, int node, int start, int end) {
        if (start == end) {
            // Leaf node — store the actual array value
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;

            // Recursively build left and right subtrees
            buildTree(arr, 2 * node + 1, start, mid);       // Left child
            buildTree(arr, 2 * node + 2, mid + 1, end);     // Right child

            // Internal node stores the sum of both children
            tree[node] = tree[2 * node + 1] + tree[2 * node + 2];
        }
    }

    /*
        Query the sum in the range [l, r]
        Returns the sum in the range using segment tree
    */
    public int queryRange(int l, int r) {
        return query(0, 0, n - 1, l, r);
    }

    private int query(int node, int start, int end, int l, int r) {
        if (r < start || end < l) {
            // No overlap
            return 0;
        }

        if (l <= start && end <= r) {
            // Complete overlap
            return tree[node];
        }

        // Partial overlap
        int mid = (start + end) / 2;
        int leftSum = query(2 * node + 1, start, mid, l, r);
        int rightSum = query(2 * node + 2, mid + 1, end, l, r);
        return leftSum + rightSum;
    }

    /*
        Update the value at index idx to val
    */
    public void updateIndex(int idx, int val) {
        update(0, 0, n - 1, idx, val);
    }

    private void update(int node, int start, int end, int idx, int val) {
        if (start == end) {
            // Update at leaf node
            tree[node] = val;
        } else {
            int mid = (start + end) / 2;
            if (idx <= mid) {
                update(2 * node + 1, start, mid, idx, val);
            } else {
                update(2 * node + 2, mid + 1, end, idx, val);
            }

            // After update, fix the current node’s value
            tree[node] = tree[2 * node + 1] + tree[2 * node + 2];
        }
    }
}
```

---

### 🧪 Sample Usage

```java
public class Main {
    public static void main(String[] args) {
        int[] arr = {1, 3, 5, 7, 9, 11};
        SegmentTree st = new SegmentTree(arr);

        System.out.println(st.queryRange(1, 3)); // Output: 15 (3+5+7)

        st.updateIndex(1, 10); // arr[1] = 10

        System.out.println(st.queryRange(1, 3)); // Output: 22 (10+5+7)
    }
}
```

---

### 🔄 Summary of Operations

| Operation             | Purpose                     | Time Complexity | Explanation                        |
| --------------------- | --------------------------- | --------------- | ---------------------------------- |
| `buildTree()`         | Construct tree from array   | `O(n)`          | Recursively fill sums              |
| `queryRange(l, r)`    | Get sum in range `l` to `r` | `O(log n)`      | Binary search like traversal       |
| `updateIndex(i, val)` | Update index `i` to `val`   | `O(log n)`      | Traverse and update affected nodes |

---

### 🧠 Goal of Build Process

To **construct a binary tree** (in an array format) where:

* Each node stores the **sum of a subrange** of the original array.
* Leaf nodes represent **individual elements**.
* Internal nodes represent **sum of ranges** (left + right).

---

### 📘 Example Input

Let’s say the array is:

```java
arr = {1, 3, 5, 7}
Indexes:   0  1  2  3
```

The segment tree will represent:

* Root: sum of entire array → `1 + 3 + 5 + 7 = 16`
* Children: sum of halves → left = `1+3=4`, right = `5+7=12`
* Leaf nodes = individual values.

---

### 🧱 Tree Structure (Conceptual)

```
               [0-3]
              (1+3+5+7)
               = 16
             /       \
        [0-1]         [2-3]
        (1+3)=4       (5+7)=12
       /    \         /    \
    [0]    [1]     [2]    [3]
     1      3       5      7
```

---

### 📦 Array Representation of Tree (Zero-based Index)

If we use an array `tree[]` to represent this tree:

```
tree[0] = sum(0–3) = 16
tree[1] = sum(0–1) = 4
tree[2] = sum(2–3) = 12
tree[3] = arr[0] = 1
tree[4] = arr[1] = 3
tree[5] = arr[2] = 5
tree[6] = arr[3] = 7
```

---

### 🔄 Step-by-Step Build Process

```java
private void buildTree(int[] arr, int node, int start, int end) {
    if (start == end) {
        // Base case: leaf node => copy the original array value
        tree[node] = arr[start];
    } else {
        // Recursive case: internal node
        int mid = (start + end) / 2;

        // Build left child
        buildTree(arr, 2 * node + 1, start, mid);

        // Build right child
        buildTree(arr, 2 * node + 2, mid + 1, end);

        // Combine results of children to get parent's value
        tree[node] = tree[2 * node + 1] + tree[2 * node + 2];
    }
}
```

---

### 🔍 Explanation in Java-Style Comments

```java
/*
    Segment Tree Build Process:

    For an input array of size n, we use a segment tree array of size 4 * n
    to safely hold all the nodes.

    The buildTree method recursively builds the tree:

    Parameters:
    - arr[]: The original input array
    - node: The index in the segment tree array (tree[])
    - start, end: The current range [start...end] of the original array that this node covers

    Algorithm:
    1. If start == end:
        - This node is a leaf in the segment tree.
        - It represents a single element from the original array.
        - Store arr[start] into tree[node]

    2. If start < end:
        - This node represents a range, not a single element.
        - We divide the range into two halves:
            left: [start ... mid]
            right: [mid+1 ... end]
        - Recursively build left and right children
            Left child is at index: 2 * node + 1
            Right child is at index: 2 * node + 2
        - Store the sum of left and right children into the current node
            tree[node] = tree[leftChild] + tree[rightChild]
*/
```

---

### 🧮 Build Process Walkthrough for `arr = {1, 3, 5, 7}`

Let's simulate the recursive calls:

#### 1. Root Node (node = 0, range = 0–3)

* mid = 1
* Recurse left: node = 1, range = 0–1
* Recurse right: node = 2, range = 2–3

#### 2. Left Subtree (node = 1, range = 0–1)

* mid = 0
* Recurse left: node = 3, range = 0–0 → arr\[0] = 1
* Recurse right: node = 4, range = 1–1 → arr\[1] = 3
* Set `tree[1] = tree[3] + tree[4] = 1 + 3 = 4`

#### 3. Right Subtree (node = 2, range = 2–3)

* mid = 2
* Recurse left: node = 5, range = 2–2 → arr\[2] = 5
* Recurse right: node = 6, range = 3–3 → arr\[3] = 7
* Set `tree[2] = tree[5] + tree[6] = 5 + 7 = 12`

#### 4. Back to Root:

* Set `tree[0] = tree[1] + tree[2] = 4 + 12 = 16`

---

### ✅ Final Tree Array

```java
tree = [16, 4, 12, 1, 3, 5, 7]
```

Each node holds the **sum of a subrange** of the original array.

