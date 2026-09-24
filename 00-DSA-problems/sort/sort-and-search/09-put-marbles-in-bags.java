/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given an array of marble weights and an integer `k` (number of bags).
 * We need to partition the array of marbles into `k` contiguous, non-empty subsegments 
 * (bags). 
 * 
 * Rules for Bag Cost:
 * - Each bag consists of a contiguous block of marbles, say from index `i` to `j`.
 * - The cost of that bag is defined as weights[i] + weights[j] (the first and 
 *   last elements of that segment).
 * - The total score is the sum of the costs across all `k` bags.
 * 
 * Our Goal:
 * Find the difference between the MAXIMUM possible total score and the MINIMUM 
 * possible total score achievable by any valid partition of the marbles into `k` bags.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW
 * ============================================================================
 * 1. Can `k` be equal to the length of the weights array? 
 *    (Yes, 1 <= k <= weights.length. If k == length, every bag contains exactly one 
 *    marble, so the cost of bag [i, i] is weights[i] + weights[i] = 2 * weights[i]).
 * 2. Can `k` be 1? 
 *    (Yes, if k == 1, all marbles are in a single bag [0, n-1], and the score 
 *    is always weights[0] + weights[n-1]).
 * 3. Can weights be very large? Do we need `long` for calculations?
 *    (Yes, weights[i] can be up to 10^9 and length up to 10^5. The sum of costs can 
 *    easily overflow a standard 32-bit signed integer, so we must use `long` for scores).
 * 4. Are the marbles allowed to be reordered?
 *    (No, rule 2 states that if marbles i and j are in the same bag, everything between 
 *    them must also be in that bag, which implies contiguous subsegments of the original array order).
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY & KEY OBSERVATIONS
 * ============================================================================
 * 1. Understanding the Partitioning (The Contiguous Subsegments):
 *    - If we divide the array into `k` bags, we must choose `k - 1` partition points 
 *      (cuts) between the `n` elements.
 *    - Example array: [w_0, w_1, w_2, w_3, w_4], k = 3
 *      Cuts could be placed after index 1 and index 3:
 *      Bag 1: [w_0, w_1] -> Cost: w_0 + w_1
 *      Bag 2: [w_2, w_3] -> Cost: w_2 + w_3
 *      Bag 3: [w_4]      -> Cost: w_4 + w_4 (since i == j)
 * 
 * 2. Rewriting the Total Score Formula:
 *    Let's look at a partition with cuts at indices: 0, c_1, c_2, ..., c_{k-1}, n-1.
 *    Each bag starts at some index `start` and ends at `end`.
 *    Total Score = Sum over all bags of (weights[start] + weights[end]).
 *    
 *    Notice a magical pattern:
 *    - The very first element of the array `weights[0]` and the very last element 
 *      `weights[n-1]` are ALWAYS included in the total score exactly once (as the 
 *      start of the first bag and the end of the last bag).
 *    - Every internal cut point `c` acts simultaneously as the *end* of one bag 
 *      (contributing weights[c]) and the *start* of the next bag (contributing weights[c]).
 *    - Therefore, EVERY internal cut point adds `weights[c] + weights[c+1]` to the total score!
 * 
 *    General Formula:
 *    Total Score = (weights[0] + weights[n-1]) + Sum of (weights[c_i] + weights[c_i + 1]) 
 *    for all chosen cut points c_i.
 * 
 * 3. Simplifying the Difference:
 *    We want to find `Max Score - Min Score`.
 *    Since `(weights[0] + weights[n-1])` is a constant value that appears in EVERY 
 *    possible valid partition, it completely cancels out in the subtraction!
 *    
 *    Max Score - Min Score = (Sum of top k-1 adjacent pairs) - (Sum of bottom k-1 adjacent pairs).
 * 
 * 4. Algorithm Steps:
 *    - Compute all adjacent pair sums: For each index `i` from 0 to `n-2`, calculate 
 *      `pairSum[i] = weights[i] + weights[i+1]`. There are `n - 1` such pairs.
 *    - Sort these `n - 1` pair sums in ascending order.
 *    - To maximize the total score, we pick the `k - 1` LARGEST pair sums.
 *    - To minimize the total score, we pick the `k - 1` SMALLEST pair sums.
 *    - The answer is the sum of the top `k-1` values minus the sum of the bottom `k-1` values.
 * 
 *    Time Complexity: O(N log N) due to sorting the pair sums array.
 *    Space Complexity: O(N) to store the pair sums.
 * 
 * ============================================================================
 * VISUALIZATION & TRACING
 * ============================================================================
 * Example: weights = [1, 3, 5, 1], k = 2
 * 
 * 1. Find all adjacent pair sums (weights[i] + weights[i+1]):
 *    - Pair 0: weights[0] + weights[1] = 1 + 3 = 4
 *    - Pair 1: weights[1] + weights[2] = 3 + 5 = 8
 *    - Pair 2: weights[2] + weights[3] = 5 + 1 = 6
 *    Array of pair sums = [4, 8, 6]
 * 
 * 2. Sort the pair sums:
 *    Sorted = [4, 6, 8]
 * 
 * 3. We need k = 2 bags, which means we need `k - 1 = 1` cut point.
 * 
 * 4. Max Score configuration picks the largest 1 pair sum -> 8
 *    Min Score configuration picks the smallest 1 pair sum -> 4
 * 
 * 5. Difference = Max Score - Min Score = 8 - 4 = 4.
 */

import java.util.Arrays;

public class PutMarblesInBags {

    public static void main(String[] args) {
        int[] weights1 = {1, 3, 5, 1};
        int k1 = 2;

        int[] weights2 = {1, 3};
        int k2 = 2;

        System.out.println("Weights: [1, 3, 5, 1], k = 2");
        System.out.println("Max-Min Score Difference: " + putMarbles(weights1, k1));
        System.out.println("--------------------------------------------------");

        System.out.println("Weights: [1, 3], k = 2");
        System.out.println("Max-Min Score Difference: " + putMarbles(weights2, k2));
    }

    /**
     * Calculates the difference between the maximum and minimum scores 
     * when distributing marbles into k bags.
     * 
     * @param weights The array of marble weights.
     * @param k       The number of bags to partition into.
     * @return        The difference between max and min possible scores.
     */
    public static long putMarbles(int[] weights, int k) {
        int n = weights.length;
        
        // If k == 1 or k == n, there's only 1 possible partition layout. 
        // Max score equals min score, so the difference is 0.
        if (k == 1 || k == n) {
            return 0;
        }

        // Step 1: Compute all adjacent pair sums
        // There are n - 1 adjacent pairs in an array of length n.
        long[] pairSums = new long[n - 1];
        for (int i = 0; i < n - 1; i++) {
            pairSums[i] = (long) weights[i] + weights[i + 1];
        }

        // Step 2: Sort the pair sums in ascending order
        Arrays.sort(pairSums);

        // Step 3: Calculate the difference between the sum of the largest 
        // (k - 1) pairs and the smallest (k - 1) pairs.
        long maxScoreSum = 0;
        long minScoreSum = 0;

        // We need to pick (k - 1) cuts.
        // The largest (k - 1) elements are at the end of the sorted pairSums array.
        // The smallest (k - 1) elements are at the beginning of the sorted pairSums array.
        for (int i = 0; i < k - 1; i++) {
            minScoreSum += pairSums[i];
            maxScoreSum += pairSums[n - 2 - i];
        }

        return maxScoreSum - minScoreSum;
    }
}

/*
This problem *looks complicated*, but the trick is surprisingly elegant once you see the pattern.

---

# 🔍 Key Observation (Core Insight)

We are dividing the array into **k contiguous subarrays (bags)**.

Each bag contributes:

```
cost = weights[start] + weights[end]
```

So total score = sum of:

* first element of each bag
* last element of each bag

---

## 💡 Reformulation

Instead of thinking about bags…

👉 Think about **cuts (partitions)** between elements.

If we choose `k-1` cuts, we create `k` bags.

---

## 🧠 What changes when we place a cut?

Suppose we cut between index `i` and `i+1`.

Then:

* `weights[i]` becomes **end of a bag**
* `weights[i+1]` becomes **start of next bag**

So this cut contributes:

```
weights[i] + weights[i+1]
```

---

## 🎯 Final Transformation

Let’s define:

```
pairSum[i] = weights[i] + weights[i+1]
```

Now:

* We must pick exactly `k-1` cuts
* Each cut contributes `pairSum[i]`

---

## 🚀 Goal Becomes:

* **Maximum score** → pick largest `k-1` pair sums
* **Minimum score** → pick smallest `k-1` pair sums

---

# ✅ Final Answer

```
answer = sum(max k-1 pair sums) - sum(min k-1 pair sums)
```

---

# 🧾 Java Solution (Clean + Interview Ready)

```java
import java.util.*;

/**
 * ============================================================
 * 🧠 Divide Marbles Into K Bags
 * ============================================================
 *
 * 🔍 Key Idea:
 * Instead of thinking about bags → think about cuts.
 *
 * Each cut between i and i+1 contributes:
 *      weights[i] + weights[i+1]
 *
 * We need exactly (k-1) cuts.
 *
 * So:
 *   - Max score → choose largest (k-1) pair sums
 *   - Min score → choose smallest (k-1) pair sums
 *
 * Answer = maxSum - minSum
 *
 * ============================================================
 */
public class DivideMarbles {

    public long putMarbles(int[] weights, int k) {

        int n = weights.length;

        // Edge case: no cuts needed
        if (k == 1) return 0;

        // Step 1: Compute all adjacent pair sums
        long[] pairSums = new long[n - 1];

        for (int i = 0; i < n - 1; i++) {
            pairSums[i] = (long) weights[i] + weights[i + 1];
        }

        // Step 2: Sort the pair sums
        Arrays.sort(pairSums);

        long minScore = 0;
        long maxScore = 0;

        // Step 3: Pick smallest (k-1) for min score
        for (int i = 0; i < k - 1; i++) {
            minScore += pairSums[i];
        }

        // Step 4: Pick largest (k-1) for max score
        for (int i = 0; i < k - 1; i++) {
            maxScore += pairSums[n - 2 - i];
        }

        return maxScore - minScore;
    }
}
```

---

# 🧠 Intuition (Interview-Friendly)

👉 Every partition boundary contributes independently
👉 So problem reduces to selecting best boundaries

Instead of exponential partitioning:

```
O(2^n) ❌
```

We reduce to:

```
Sort + Greedy selection ✅
```

---

# ⏱ Complexity

| Step            | Complexity |
| --------------- | ---------- |
| Build pair sums | O(n)       |
| Sort            | O(n log n) |
| Pick k elements | O(k)       |

👉 **Total: O(n log n)**
👉 Space: **O(n)**

---

# 🧪 Example Walkthrough

```
weights = [1, 3, 5, 1], k = 2
```

Pair sums:

```
[1+3=4, 3+5=8, 5+1=6]
→ [4, 6, 8]
```

Pick:

* min: 4
* max: 8

Answer:

```
8 - 4 = 4
```

---

# 🔥 Why This Works (Deep Insight)

Total score always includes:

```
weights[0] + weights[n-1]
```

(First bag starts at 0, last bag ends at n-1)

Everything else depends **only on cuts**.

👉 That’s why we can isolate contributions cleanly.

---

If you want, I can also show:

* 🧩 Why greedy is always optimal (proof)
* ⚡ Heap-based optimization for large k
* 🧠 How to derive this pattern in interviews step-by-step

/*
