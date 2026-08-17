import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Problem: Find K Pairs with Smallest Sums
 * 
 * Statement:
 * You are given two integer arrays, list1 and list2, sorted in non-decreasing order, 
 * and an integer, k. Return the k pairs (u, v) whose sum is the smallest among all possible pairs.
 * 
 * Constraints:
 * - 1 <= list1.length, list2.length <= 500
 * - -10^4 <= list1[i], list2[i] <= 10^4
 * - 1 <= k <= 10^3
 * - Input lists are sorted in ascending order.
 * - If k exceeds the total number of valid pairs, return all the pairs.
 */
public class FindKPairsWithSmallestSums {

    /* ============================================================================
     * APPROACH 1: Brute Force (Generate All and Sort)
     * ============================================================================
     * Explanation:
     * The simplest way is to generate every possible pair from list1 and list2, 
     * store their sums, and sort all the pairs based on the sum. After sorting, 
     * we just return the first k elements.
     * 
     * Time Complexity: O(M * N * log(M * N)), where M and N are the lengths of the arrays.
     * Space Complexity: O(M * N) to store all possible pairs.
     */
    
    // A record to hold the pair and its sum for easy sorting
    private record Pair(int u, int v, int sum) implements Comparable<Pair> {
        @Override
        public int compareTo(Pair other) {
            return Integer.compare(this.sum, other.sum);
        }
    }

    public static List<List<Integer>> kSmallestPairsBruteForce(int[] list1, int[] list2, int k) {
        var allPairs = new ArrayList<Pair>();
        
        // Generate all pairs
        for (int u : list1) {
            for (int v : list2) {
                allPairs.add(new Pair(u, v, u + v));
            }
        }
        
        // Sort all pairs by sum
        Collections.sort(allPairs);
        
        // Return up to k pairs
        var result = new ArrayList<List<Integer>>();
        int limit = Math.min(k, allPairs.size());
        for (int i = 0; i < limit; i++) {
            result.add(List.of(allPairs.get(i).u(), allPairs.get(i).v()));
        }
        
        return result;
    }

    /* ============================================================================
     * APPROACH 2: Max-Heap (Keep track of k smallest so far)
     * ============================================================================
     * Explanation:
     * We can use a Max-Heap of size k. As we generate pairs, we add them to the heap.
     * If the heap exceeds size k, we remove the maximum element. 
     * Because the arrays are sorted, if we encounter a pair whose sum is greater than 
     * the maximum sum currently in our heap of size k, we can break the inner loop early,
     * as all subsequent pairs in that row will only be larger.
     * 
     * Time Complexity: O(M * N * log k) in the worst case, but heavily optimized by the early break.
     * Space Complexity: O(k) for the Max-Heap.
     */
    public static List<List<Integer>> kSmallestPairsMaxHeap(int[] list1, int[] list2, int k) {
        // Max-Heap: sorted by sum in descending order
        var maxHeap = new PriorityQueue<Pair>((a, b) -> Integer.compare(b.sum(), a.sum()));
        
        for (int u : list1) {
            for (int v : list2) {
                int currentSum = u + v;
                
                if (maxHeap.size() < k) {
                    maxHeap.offer(new Pair(u, v, currentSum));
                } else if (currentSum < maxHeap.peek().sum()) {
                    maxHeap.poll();
                    maxHeap.offer(new Pair(u, v, currentSum));
                } else {
                    // Because list2 is sorted, currentSum will only increase. 
                    // No need to check the rest of list2 for this 'u'.
                    break;
                }
            }
        }
        
        var result = new ArrayList<List<Integer>>();
        while (!maxHeap.isEmpty()) {
            var p = maxHeap.poll();
            result.add(List.of(p.u(), p.v()));
        }
        // Since we extracted from a max-heap, the largest are extracted first. 
        // We reverse it to return in increasing order.
        Collections.reverse(result);
        
        return result;
    }

    /* ============================================================================
     * APPROACH 3: Optimal Min-Heap (Dijkstra-like BFS)
     * ============================================================================
     * Explanation:
     * Imagine the pairs forming a 2D grid/matrix of sums.
     * list1 = [1, 7, 11], list2 = [2, 4, 6]
     * 
     * Matrix of sums:
     *        2    4    6   (list2)
     *      +--------------
     *   1  | 3    5    7
     *   7  | 9   11   13
     *  11  | 13  15   17
     * (list1)
     * 
     * Since both arrays are sorted, the smallest sum is always at the top-left (0,0).
     * After picking (0,0), the next smallest must be either (0,1) or (1,0). 
     * To prevent adding duplicates to the heap and to optimize space:
     * 1. Initialize the Min-Heap with the first element of list2 paired with 
     *    the first `k` elements of list1. (The first column: 3, 9, 13).
     * 2. Extract the minimum sum from the heap.
     * 3. When extracting pair (i, j), the next potential smallest is (i, j+1).
     *    Push (i, j+1) into the heap if it exists.
     * 4. Repeat k times.
     * 
     * Time Complexity: O(k log k) 
     * Space Complexity: O(k) for the Min-Heap.
     */
    
    // Record to track the indices instead of just values
    private record HeapNode(int sum, int i, int j) implements Comparable<HeapNode> {
        @Override
        public int compareTo(HeapNode other) {
            return Integer.compare(this.sum, other.sum);
        }
    }

    public static List<List<Integer>> kSmallestPairsOptimal(int[] list1, int[] list2, int k) {
        var result = new ArrayList<List<Integer>>();
        if (list1.length == 0 || list2.length == 0 || k == 0) {
            return result;
        }
        
        var minHeap = new PriorityQueue<HeapNode>();
        
        // Step 1: Initialize the heap with the first column (i, 0)
        // We only need at most 'k' elements because we only need 'k' pairs total.
        for (int i = 0; i < Math.min(list1.length, k); i++) {
            minHeap.offer(new HeapNode(list1[i] + list2[0], i, 0));
        }
        
        // Step 2: Extract min and add the next element from the same row (i, j+1)
        while (k > 0 && !minHeap.isEmpty()) {
            var node = minHeap.poll();
            
            // Add the current smallest pair to our result
            result.add(List.of(list1[node.i()], list2[node.j()]));
            k--;
            
            // If there's another element in the current row of list2, add it to the heap
            if (node.j() + 1 < list2.length) {
                int nextJ = node.j() + 1;
                minHeap.offer(new HeapNode(list1[node.i()] + list2[nextJ], node.i(), nextJ));
            }
        }
        
        return result;
    }

    /* ============================================================================
     * TESTING / MAIN METHOD
     * ============================================================================
     */
    
    // Using Java 14+ record for structured test cases
    public record TestCase(int[] list1, int[] list2, int k) {}

    public static void main(String[] args) {
        var testCases = List.of(
            new TestCase(
                new int[]{1, 7, 11}, 
                new int[]{2, 4, 6}, 
                3
            ),
            new TestCase(
                new int[]{1, 1, 2}, 
                new int[]{1, 2, 3}, 
                2
            ),
            new TestCase(
                new int[]{1, 2}, 
                new int[]{3}, 
                3 // k > total pairs possible
            )
        );

        System.out.println("Running tests for all 3 approaches...\n");

        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            System.out.println("Test Case " + (i + 1) + ": (k = " + tc.k + ")");
            System.out.println("list1: " + Arrays.toString(tc.list1));
            System.out.println("list2: " + Arrays.toString(tc.list2));
            
            var ans1 = kSmallestPairsBruteForce(tc.list1, tc.list2, tc.k);
            var ans2 = kSmallestPairsMaxHeap(tc.list1, tc.list2, tc.k);
            var ans3 = kSmallestPairsOptimal(tc.list1, tc.list2, tc.k);

            System.out.println("  Brute Force : " + ans1);
            System.out.println("  Max-Heap    : " + ans2);
            System.out.println("  Optimal     : " + ans3);
            System.out.println("-".repeat(60));
        }
    }
}

/*
Perfect — let’s **simulate this like an animation** so you can *see* the algorithm working.

---

# 🎬 Setup

```text
list1 = [1, 2, 4]
list2 = [1, 3, 5]
k = 5
```

---

# 🎥 Frame 0 — Build the grid in your head

```
        j=0   j=1   j=2
        1     3     5
      -----------------
i=0 |   2     4     6
i=1 |   3     5     7
i=2 |   5     7     9
```

👉 Each cell = `list1[i] + list2[j]`

---

# 🎯 What we want

Pick **k = 5 smallest cells** in order
WITHOUT scanning entire grid.

---

# 🎬 Frame 1 — Initialize Heap

We push only first column `(i,0)`:

```
Heap contains:
(i=0,j=0)=2
(i=1,j=0)=3
(i=2,j=0)=5
```

👉 Think: we are placing **3 sorted rows into heap**

---

# 🎬 Frame 2 — First Extraction

```
POP → (0,0)=2
```

Result:

```
[(1,1)]
```

👉 Now expand RIGHT (same row):

```
Push → (0,1)=4
```

Heap becomes:

```
(1,0)=3
(0,1)=4
(2,0)=5
```

---

# 🎬 Frame 3 — Second Extraction

```
POP → (1,0)=3
```

Result:

```
[(1,1), (2,1)]
```

👉 Expand RIGHT:

```
Push → (1,1)=5
```

Heap:

```
(0,1)=4
(2,0)=5
(1,1)=5
```

---

# 🎬 Frame 4 — Third Extraction

```
POP → (0,1)=4
```

Result:

```
[(1,1), (2,1), (1,3)]
```

👉 Expand RIGHT:

```
Push → (0,2)=6
```

Heap:

```
(2,0)=5
(1,1)=5
(0,2)=6
```

---

# 🎬 Frame 5 — Fourth Extraction

```
POP → (2,0)=5
```

Result:

```
[(1,1), (2,1), (1,3), (4,1)]
```

👉 Expand RIGHT:

```
Push → (2,1)=7
```

Heap:

```
(1,1)=5
(0,2)=6
(2,1)=7
```

---

# 🎬 Frame 6 — Fifth Extraction

```
POP → (1,1)=5
```

Result:

```
[(1,1), (2,1), (1,3), (4,1), (2,3)]
```

✅ Done (k = 5)

---

# 🧠 What just happened? (Critical Insight)

### You NEVER explored entire grid ❌

You only explored:

```
(0,0)
(1,0)
(2,0)
(0,1)
(1,1)
(0,2)
(2,1)
```

👉 Just enough to get k smallest

---

# 🔥 Animation Analogy (Very Important)

Imagine:

* Each row is a **sorted conveyor belt**
* You put the **first item of each belt into heap**
* Each time you pick the smallest item:
  👉 you pull the **next item from that same belt**

---

# 🧠 Why this works (deep intuition)

Because:

```
Row i:
list1[i] + list2[0] ≤ list1[i] + list2[1] ≤ list1[i] + list2[2]
```

👉 So each row is sorted

👉 Heap merges these sorted rows

---

# ⚠️ Most Important Realization

👉 We are NOT doing 2D traversal

👉 We are doing:

```
K-way merge of sorted rows
```

---

# ❌ Why we don’t go DOWN?

You might think:

```
From (0,0), go to (1,0)
```

But we already added:

```
(1,0) at start
```

👉 So DOWN direction is already covered

---

# 🧠 Final Mental Model

```
Each row = sorted list
We merge k sorted lists using heap
```

---

# 🧩 Interview Summary (say this confidently)

> “I treat each row as a sorted list. I initialize the heap with the first element of each row. Then I repeatedly extract the smallest pair and insert the next element from the same row. This is essentially a k-way merge.”

---

# 🚀 If you want next

I can show:
✅ Why this is same as **merge k sorted arrays**
✅ Hard variant: **k-th smallest pair sum**
✅ Visual diagram with arrows (like graph traversal)

Just tell 👍

*/