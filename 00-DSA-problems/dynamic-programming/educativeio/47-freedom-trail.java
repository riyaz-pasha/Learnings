import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Freedom Trail (Circular Ring String Matching)
 * Given a circular `ring` and a target `key`, find the minimum number of steps 
 * to spell out the `key`. 
 * 
 * - The ring starts aligned at index 0 (12:00).
 * - You can rotate clockwise or counterclockwise. 1 rotation = 1 step.
 * - Pressing the center button to lock in a character = 1 step.
 * 
 * Constraints:
 * 1 <= ring.length, key.length <= 100
 * ring and key consist of only lowercase English letters.
 * It is guaranteed that key can always be spelled.
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, establishing the state space and graph properties 
 * is the most important first step.
 * 
 * Q: "Can the same character appear multiple times in the ring?"
 * A: Yes! This is the core complexity of the problem. If 'a' appears at 
 *    index 2 and index 8, greedy choice (picking the physically closest 'a') 
 *    might lead to a sub-optimal overall path. We must evaluate all valid 
 *    'a' positions to see which sets us up best for the rest of the key.
 * 
 * Q: "Are the distances uniform in both directions?"
 * A: Yes, it's a perfect circle. The distance between index `i` and index `j` 
 *    is `Math.min(abs(i - j), ringLength - abs(i - j))`.
 * 
 * CRITICAL SENIOR INSIGHT - PRECOMPUTATION:
 * "Instead of scanning the `ring` linearly every time we want to find the next 
 * character, we should precompute a frequency/index map. We can store a list of 
 * indices for every character 'a'-'z'. This drops our inner loop from O(R) to 
 * O(K), where K is the frequency of the specific character."
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "At any given moment, our state is defined by two variables:
 *  1. `ringIndex`: Where the ring is currently positioned (what is at 12:00).
 *  2. `keyIndex`: Which character in the key we are currently trying to spell.
 * 
 * To match `key[keyIndex]`, we look at all precomputed `targetIndices` in the 
 * ring that contain this character. For each target, the cost is:
 *  Rotation Distance + 1 (the button press) + Optimal path for the REST of the key.
 * 
 * Because evaluating `keyIndex = 2` from `ringIndex = 5` might happen multiple 
 * times via different initial paths, we have overlapping subproblems -> DP."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: ring = "godding", key = "gd"
 * 
 * Precomputed Indices:
 * 'g': [0, 6]
 * 'o': [1]
 * 'd': [2, 3]
 * 'i': [4]
 * 'n': [5]
 * 
 * Top-Down Trace (Start at ring=0, key=0):
 * match 'g' at key[0]:
 *   Option 1: Move to ring=0 (dist=0). Press (1). Total step = 1.
 *             Recurse for key[1] ('d') from ring=0.
 *             -> match 'd':
 *                Option 1.A: Move to ring=2 (dist=2). Press(1). Total=3.
 *                Option 1.B: Move to ring=3 (dist=3). Press(1). Total=4.
 *             Min from ring=0 is 3. Total for Option 1 = 1 + 3 = 4.
 *   
 *   Option 2: Move to ring=6 (dist=1, wrap around). Press (1). Total step = 2.
 *             Recurse for key[1] ('d') from ring=6.
 *             -> match 'd':
 *                Option 2.A: Move to ring=2 (dist=3, wrap). Press(1). Total=4.
 *                Option 2.B: Move to ring=3 (dist=3). Press(1). Total=4.
 *             Min from ring=6 is 4. Total for Option 2 = 2 + 4 = 6.
 * 
 * Minimum overall cost = 4 steps. (Rotate 0 to match 'g', press. Rotate 2 to match 'd', press).
 */
public class FreedomTrail {

    /**
     * Helper method to precompute the indices of each character in the ring.
     */
    private List<Integer>[] buildCharIndices(String ring) {
        @SuppressWarnings("unchecked")
        List<Integer>[] indices = new ArrayList[26];
        for (int i = 0; i < 26; i++) {
            indices[i] = new ArrayList<>();
        }
        for (int i = 0; i < ring.length(); i++) {
            indices[ring.charAt(i) - 'a'].add(i);
        }
        return indices;
    }

    /**
     * Helper method to calculate the minimum rotation distance on a circular ring.
     */
    private int calcDist(int currIndex, int targetIndex, int ringLen) {
        int diff = Math.abs(currIndex - targetIndex);
        return Math.min(diff, ringLen - diff);
    }

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Branch out to every possible matching character index in the ring.
     * 
     * Time Complexity: O(K^N) - where K is the max frequency of any char in the ring, 
     * and N is the length of the key. Exponential branching.
     * Space Complexity: O(N) - Maximum depth of the recursion tree.
     */
    public int findRotateStepsRecursive(String ring, String key) {
        if (ring == null || key == null) return 0;
        List<Integer>[] charIndices = buildCharIndices(ring);
        return solveRecursive(ring.length(), key, 0, 0, charIndices);
    }

    private int solveRecursive(int ringLen, String key, int ringIndex, int keyIndex, List<Integer>[] charIndices) {
        // BASE CASE REASONING:
        // If we have successfully spelled every character in the key, 
        // the remaining cost is 0.
        if (keyIndex == key.length()) {
            return 0;
        }

        int minSteps = Integer.MAX_VALUE;
        char targetChar = key.charAt(keyIndex);

        // Branch into all possible universes where we align the target character
        for (int nextIndex : charIndices[targetChar - 'a']) {
            int rotationCost = calcDist(ringIndex, nextIndex, ringLen);
            int pressCost = 1;
            
            int totalCostForBranch = rotationCost + pressCost + 
                                     solveRecursive(ringLen, key, nextIndex, keyIndex + 1, charIndices);
            
            minSteps = Math.min(minSteps, totalCostForBranch);
        }

        return minSteps;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache the minimum steps required to spell `key[keyIndex:]` starting 
     * from a specific `ringIndex`.
     * 
     * Time Complexity: O(R * N * K) - Where R is ring length, N is key length, 
     * and K is char frequency. Safely bounded to O(100 * 100 * 100) = 10^6 ops.
     * Space Complexity: O(R * N) - For the memo matrix + call stack.
     */
    public int findRotateStepsMemo(String ring, String key) {
        if (ring == null || key == null) return 0;
        List<Integer>[] charIndices = buildCharIndices(ring);
        
        int[][] memo = new int[ring.length()][key.length()];
        for (int[] row : memo) Arrays.fill(row, -1);
        
        return solveMemo(ring.length(), key, 0, 0, charIndices, memo);
    }

    private int solveMemo(int ringLen, String key, int ringIndex, int keyIndex, 
                          List<Integer>[] charIndices, int[][] memo) {
        if (keyIndex == key.length()) return 0;

        if (memo[ringIndex][keyIndex] != -1) {
            return memo[ringIndex][keyIndex];
        }

        int minSteps = Integer.MAX_VALUE;
        char targetChar = key.charAt(keyIndex);

        for (int nextIndex : charIndices[targetChar - 'a']) {
            int steps = calcDist(ringIndex, nextIndex, ringLen) + 1 + 
                        solveMemo(ringLen, key, nextIndex, keyIndex + 1, charIndices, memo);
            minSteps = Math.min(minSteps, steps);
        }

        memo[ringIndex][keyIndex] = minSteps;
        return minSteps;
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 2D)
     * ========================================================================
     * Idea: Build an (N + 1) x R table. dp[i][j] represents the minimum steps 
     * to spell the suffix `key[i:]` given the ring is currently at `ringIndex` j.
     * 
     * Time Complexity: O(N * R * K)
     * Space Complexity: O(N * R)
     */
    public int findRotateStepsTabulation(String ring, String key) {
        int r = ring.length();
        int n = key.length();
        List<Integer>[] charIndices = buildCharIndices(ring);
        
        int[][] dp = new int[n + 1][r];
        
        // BASE CASE REASONING:
        // dp[n][j] is automatically 0 because spelling 0 remaining characters costs 0.

        // Iterate backward through the key (from the last character to the first)
        for (int i = n - 1; i >= 0; i--) {
            char targetChar = key.charAt(i);
            
            // Iterate through every possible starting position on the ring
            for (int j = 0; j < r; j++) {
                dp[i][j] = Integer.MAX_VALUE;
                
                // --- DETAILED TABULATION EXPLANATION ---
                // Try aligning every valid target character to the 12:00 position
                for (int nextIndex : charIndices[targetChar - 'a']) {
                    
                    int rotationCost = calcDist(j, nextIndex, r);
                    int pressCost = 1;
                    
                    // We look at the row below us (i+1) because we are building 
                    // the solution backwards from the end of the string.
                    int totalCost = rotationCost + pressCost + dp[i + 1][nextIndex];
                    
                    dp[i][j] = Math.min(dp[i][j], totalCost);
                }
            }
        }

        // The answer to the problem is spelling the entire key (index 0) 
        // starting with the ring aligned at index 0.
        return dp[0][0];
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In Tabulation, to calculate row `i` (matching `key[i]`), we ONLY 
     * rely on the values from row `i+1` (`dp[i+1][nextIndex]`).
     * 
     * We can collapse the 2D matrix into a 1D array of size R (`prevRow`).
     * 
     * Time Complexity: O(N * R * K)
     * Space Complexity: O(R) - Massively reduced memory footprint.
     */
    public int findRotateStepsSpaceOptimized(String ring, String key) {
        int r = ring.length();
        int n = key.length();
        List<Integer>[] charIndices = buildCharIndices(ring);
        
        // Represents the optimal paths for the suffix of the key we just processed
        int[] prevRow = new int[r];
        
        // Process the key backwards
        for (int i = n - 1; i >= 0; i--) {
            // Represents the optimal paths for the current character we are matching
            int[] currRow = new int[r];
            Arrays.fill(currRow, Integer.MAX_VALUE);
            
            char targetChar = key.charAt(i);
            
            for (int j = 0; j < r; j++) {
                
                // MAGIC OF THE 1D ARRAY:
                // We use prevRow[nextIndex] to look up the optimally solved subproblem.
                for (int nextIndex : charIndices[targetChar - 'a']) {
                    int cost = calcDist(j, nextIndex, r) + 1 + prevRow[nextIndex];
                    currRow[j] = Math.min(currRow[j], cost);
                }
            }
            
            // Shift the sliding window up
            prevRow = currRow;
        }

        // Return the cost starting from the absolute beginning
        return prevRow[0];
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new FreedomTrail();
        
        record TestCase(String ring, String key, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase("godding", "gd", 4), // Traced in comments
            new TestCase("godding", "godding", 13), 
            new TestCase("abcde", "edcba", 10), // Perfect reverse wrapping check
            new TestCase("aaaaa", "aaaaa", 5)   // Multiple duplicates, zero rotations
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("Ring    : \"" + tc.ring + "\"");
            System.out.println("Key     : \"" + tc.key + "\"");
            System.out.println("Expected: " + tc.expected);
            
            // Pure recursion can TLE on longer keys with many duplicates
            if (tc.key.length() <= 10) {
                System.out.println("Recursive (Brute) : " + solver.findRotateStepsRecursive(tc.ring, tc.key));
            } else {
                System.out.println("Recursive (Brute) : Skipped");
            }
            
            System.out.println("Memoization       : " + solver.findRotateStepsMemo(tc.ring, tc.key));
            System.out.println("Tabulation 2D     : " + solver.findRotateStepsTabulation(tc.ring, tc.key));
            System.out.println("Space Optimized   : " + solver.findRotateStepsSpaceOptimized(tc.ring, tc.key));
            System.out.println();
        }
    }
}

/*
Got it 👍 — let’s slow this down and build intuition **step-by-step**.
Forget DP for a moment. Just understand the **process** first.

---

# 🎯 What are we actually doing?

You have a circular ring:

```
ring = "godding"
         ↑
       pointer (starts at index 0)
```

You want to type:

```
key = "gd"
```

---

# 🧠 Step 1: What actions cost steps?

Two things cost steps:

1. **Rotate** (left or right) → cost = distance
2. **Press button** → cost = +1

---

# 🔥 Step 2: First character

We need `'g'`.

Pointer is already at `'g'` (index 0).

👉 Cost = **0 rotation + 1 press = 1**

---

# 🔥 Step 3: Next character `'d'`

Now it gets interesting.

Where is `'d'` in ring?

```
g o d d i n g
    ↑ ↑
    2 3
```

So we have **2 choices**:

* go to index **2**
* go to index **3**

---

# ⚠️ IMPORTANT IDEA (THIS IS THE WHOLE PROBLEM)

👉 You must **try BOTH choices** and pick minimum.

---

# 🧮 Let's calculate both paths

### Option 1 → go to index 2

From index 0 → index 2

```
distance = 2
other way = 7 - 2 = 5
min = 2
```

Cost:

```
2 rotation + 1 press = 3
```

Total so far:

```
1 (for 'g') + 3 = 4
```

---

### Option 2 → go to index 3

From index 0 → index 3

```
distance = 3
other way = 7 - 3 = 4
min = 3
```

Cost:

```
3 rotation + 1 press = 4
```

Total:

```
1 + 4 = 5
```

---

# ✅ Final Answer

```
min(4, 5) = 4
```

---

# 💡 Why DP is needed?

Imagine:

```
ring = "ababcab"
key  = "aaa"
```

Now for each `'a'`:

* you have **multiple positions**
* each leads to different future costs

👉 This becomes:

```
Try all possibilities → exponential
```

So we store results:

```
dp[currentPosition][keyIndex]
```

---

# 🧠 Simplest Mental Model

Forget formulas. Think like this:

```
At every step:
    I am at index r
    I need key[k]

    → Find ALL matching positions
    → Try going to each
    → Take minimum
```

---

# ⚡ Ultra Simple Recursion (NO DP, just idea)

```java
int solve(int r, int k) {
    if (k == key.length()) return 0;

    int ans = INF;

    for (each index next where ring[next] == key[k]) {

        int rotation = minDistance(r, next);

        int cost = rotation + 1 + solve(next, k + 1);

        ans = Math.min(ans, cost);
    }

    return ans;
}
```

👉 DP just **memoizes this**.

---

# 🚨 The ONE LINE you must remember

```
Answer = min over all matching positions:
         (rotation cost + 1 + future cost)
```

---

# ❓ Where exactly are you stuck?

Tell me one:

1. ❓ Rotation logic confusing
2. ❓ Why multiple choices
3. ❓ DP state not clear
4. ❓ Code recursion confusion

I’ll fix that specific part 🔥
*/
