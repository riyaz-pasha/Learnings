# Topological Sort — Complete Mastery Guide (Java)

---

## 1. The Story First (Intuition Before Formalism)

Imagine you're getting dressed. You cannot put on your **shoes** before your **socks**. You cannot wear a **belt** before your **pants**. But your **watch** has nothing to do with any of this — you can put it on whenever.

This is exactly the shape of problem Topological Sort solves: **given a bunch of tasks, some of which must happen before others, produce ONE valid order to do them all.**

Other real disguises of the same idea:
- **Course prerequisites**: Take "Data Structures" before "Algorithms."
- **Build systems** (Maven/Gradle/Make): compile `Utils.java` before `Main.java` if `Main` imports `Utils`.
- **Package managers** (npm/pip): install dependency `A` before package `B` that needs `A`.
- **Spreadsheet formulas**: compute `C1 = A1 + B1` only after `A1` and `B1` are known.
- **Job scheduling in a CI/CD pipeline**: run `build` before `test` before `deploy`.

Notice the common skeleton in every example: **"X must happen before Y."** Anytime you see a problem statement built out of sentences like that, your brain should immediately whisper: *"this smells like topological sort."*

---

## 2. The Formal Definition

A **Topological Sort** (or "topological ordering") of a **Directed Graph** is a linear ordering of its vertices such that for every directed edge `u → v`, vertex `u` comes **before** vertex `v` in the ordering.

Two non-negotiable requirements:

1. The graph must be **Directed**. (Undirected graphs have no "before/after" — makes no sense.)
2. The graph must be **Acyclic** — i.e., it must be a **DAG (Directed Acyclic Graph)**.

If there's a cycle, topological sort is **impossible**. Think about why intuitively: if `A → B → C → A`, then A must come before B, B before C, and C before A. That's a contradiction — you can never satisfy all three at once. This single insight — **"cycle ⇒ no valid ordering exists"** — is the seed from which an entire family of algorithms grows (including cycle detection itself, which you get for free once you understand topo sort).

```
Valid DAG (topo sort exists):        Cyclic graph (topo sort IMPOSSIBLE):

   A                                     A
  / \                                   / \
 v   v                                 v   v
 B   C                                 B   C
  \ /                                   \ /
   v                                     v
   D                                     A   <-- back edge creates a cycle
```

**Important fact**: A DAG can have **more than one** valid topological ordering. Topo sort gives you *a* valid order, not *the* one true order (unless you're asked for something specific like lexicographically smallest — covered later).

---

## 3. How To *Recognize* a Topological Sort Problem (Pattern Detection)

This is the single most valuable skill — recognizing the pattern faster than solving it. Ask yourself these trigger questions when reading a problem:

| Signal in the problem statement | What it's hinting at |
|---|---|
| "A must be completed/done/taken before B" | Directed edge A → B |
| "Dependencies", "prerequisites", "requires" | Build a dependency graph |
| "Find an order to do all tasks" | You need topo sort's output |
| "Is it possible to finish all tasks / courses?" | You need topo sort's cycle-detection side-effect |
| "Find if there's a valid sequence" | Same as above |
| Letters/words with ordering constraints (e.g., "alien dictionary") | Build edges from relative order clues, then topo sort |
| Build/compile order, task scheduler, pipeline stages | Classic topo sort |
| "Find the minimum time to complete all jobs" (with dependencies) | Topo sort + DP over the DAG (longest path) |

**A quick mental checklist:**
1. Can I model this as a **directed graph** where an edge means "this must come before that"?
2. Do I need to output an **order**, check **feasibility** (cycle exists or not), or compute something that depends on doing things in dependency order (like "earliest finish time")?

If yes to both → topological sort (possibly combined with DP).

---

## 4. Graph Representation in Java (The Setup Every Solution Needs)

Before any topo sort code, you need two structures:

1. **Adjacency List**: `u → v` means "u must come before v". Store `List<List<Integer>> adj`, where `adj.get(u)` contains all `v` such that `u → v`.
2. **In-degree array**: `indegree[v]` = number of edges pointing INTO `v` = number of prerequisites `v` still has.

```java
import java.util.*;

public class GraphBuilder {
    public static List<List<Integer>> buildAdjList(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) {
            int u = e[0], v = e[1]; // u must come before v
            adj.get(u).add(v);
        }
        return adj;
    }

    public static int[] buildIndegree(int n, List<List<Integer>> adj) {
        int[] indegree = new int[n];
        for (int u = 0; u < n; u++)
            for (int v : adj.get(u))
                indegree[v]++;
        return indegree;
    }
}
```

Keep this mental picture handy — **every** topo sort algorithm is just a different way of walking this same structure.

---

## 5. Approach #1 — DFS-Based Topological Sort

### 5.1 The Core Insight

Do a DFS. When you finish *fully exploring* a vertex (i.e., all its descendants are done), push it onto a stack. At the end, pop everything off the stack — that's your topological order.

**Why does this work?** Think about it this way: if `u → v`, then when you DFS from `u`, you will recurse into `v` (directly or transitively) before you finish processing `u`. So `v` finishes (gets pushed) *before* `u` finishes (gets pushed). That means in the stack, `v` is below `u`. When you **pop** the stack (reverse order), `u` comes out before `v`. Exactly the property we want!

In one sentence: **"Finish times of a DFS on a DAG, sorted in decreasing order, give a valid topological ordering."**

### 5.2 Step-by-Step Walkthrough

```
Graph:
   5 → 0
   4 → 0
   5 → 2
   2 → 3
   3 → 1
   4 → 1

ASCII view:
   5 --> 0
   5 --> 2 --> 3 --> 1
   4 --> 0
   4 --> 1
```

DFS from 5: visit 5 → visit 0 (no children) → finish 0 (push 0) → visit 2 → visit 3 → visit 1 (no children) → finish 1 (push 1) → finish 3 (push 3) → finish 2 (push 2) → finish 5 (push 5)

Then DFS from 4 (not yet visited): visit 4 → 0 already visited, skip → 1 already visited, skip → finish 4 (push 4)

Stack from bottom to top: `[0, 1, 3, 2, 5, 4]`

Pop everything (reverse): **`4, 5, 2, 3, 1, 0`** — a valid topological order. Check it against every edge — all constraints satisfied.

### 5.3 Java Implementation

```java
import java.util.*;

public class TopoSortDFS {

    public int[] topoSort(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        for (int[] e : edges) adj.get(e[0]).add(e[1]);

        boolean[] visited = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfs(i, adj, visited, stack);
            }
        }

        int[] result = new int[n];
        int idx = 0;
        while (!stack.isEmpty()) {
            result[idx++] = stack.pop();
        }
        return result;
    }

    private void dfs(int node, List<List<Integer>> adj, boolean[] visited, Deque<Integer> stack) {
        visited[node] = true;
        for (int neighbor : adj.get(node)) {
            if (!visited[neighbor]) {
                dfs(neighbor, adj, visited, stack);
            }
        }
        stack.push(node); // push AFTER all descendants are fully processed
    }
}
```

**The one line that matters most**: `stack.push(node)` happens **after** the recursive `for` loop — i.e., on the way *out* of the recursion (post-order), not on the way in. This is the single most common bug beginners introduce: pushing at the wrong time.

### 5.4 Detecting Cycles With DFS (The Three-Color Method)

DFS-based topo sort silently assumes there's no cycle. If there IS a cycle, plain DFS can loop forever or (with just a `visited[]` array) miss the cycle entirely because a node visited via one path looks "already done" even if it's actually still on the current recursion stack.

**Fix: use three states instead of a boolean.**

```
WHITE (0) = not visited yet
GRAY  (1) = currently being explored (on the current DFS recursion path)
BLACK (2) = fully explored (finished, safe)
```

If, during DFS, you reach a neighbor that is **GRAY**, you've found a **back edge** → a cycle exists.

```java
import java.util.*;

public class CycleDetectionDFS {
    private static final int WHITE = 0, GRAY = 1, BLACK = 2;

    public boolean hasCycle(int n, List<List<Integer>> adj) {
        int[] color = new int[n]; // all WHITE by default
        for (int i = 0; i < n; i++) {
            if (color[i] == WHITE) {
                if (dfs(i, adj, color)) return true;
            }
        }
        return false;
    }

    private boolean dfs(int node, List<List<Integer>> adj, int[] color) {
        color[node] = GRAY;
        for (int neighbor : adj.get(node)) {
            if (color[neighbor] == GRAY) return true;        // back edge -> cycle!
            if (color[neighbor] == WHITE && dfs(neighbor, adj, color)) return true;
        }
        color[node] = BLACK;
        return false;
    }
}
```

```
Recursion stack view during DFS (GRAY nodes = currently "on the stack"):

   A(GRAY) -> B(GRAY) -> C(GRAY) -> A  <-- A is GRAY again! CYCLE DETECTED
```

**Mental model**: GRAY = "an ancestor of me in the current DFS path." If you ever point back to an ancestor, that's a cycle, by definition (a path forward + an edge back = a loop).

---

## 6. Approach #2 — Kahn's Algorithm (BFS-Based, In-Degree Method)

### 6.1 The Core Insight

Instead of thinking recursively (DFS), think **greedily, layer by layer**:

> "A node with **zero prerequisites (in-degree 0)** can safely be done right now. Once you 'do' it, remove its outgoing edges — this may free up new nodes to have zero in-degree. Repeat."

This is precisely how you'd manually solve a real dependency graph: start with tasks that need nothing, knock them out, see what opens up next.

### 6.2 Step-by-Step Walkthrough (Same Graph as Before)

```
   5 --> 0
   5 --> 2 --> 3 --> 1
   4 --> 0
   4 --> 1

In-degrees:
  0: 2 (from 5, 4)
  1: 2 (from 3, 4)
  2: 1 (from 5)
  3: 1 (from 2)
  4: 0
  5: 0
```

1. **Queue starts with all in-degree-0 nodes**: `[4, 5]`
2. Pop `4` → add to result. Decrease in-degree of its neighbors (0, 1): `indeg[0]=1, indeg[1]=1`. Neither is 0 yet.
3. Pop `5` → add to result. Decrease in-degree of its neighbors (0, 2): `indeg[0]=0` → push 0! `indeg[2]=0` → push 2!
4. Pop `0` → add to result. No outgoing edges.
5. Pop `2` → add to result. Decrease in-degree of neighbor 3: `indeg[3]=0` → push 3!
6. Pop `3` → add to result. Decrease in-degree of neighbor 1: `indeg[1]=0` → push 1!
7. Pop `1` → add to result. Done.

Result: **`4, 5, 0, 2, 3, 1`** — again, a valid topological order (different from the DFS one — remember, multiple valid answers exist!).

### 6.3 Java Implementation

```java
import java.util.*;

public class TopoSortKahn {

    public int[] topoSort(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        int[] indegree = new int[n];

        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            indegree[e[1]]++;
        }

        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (indegree[i] == 0) queue.offer(i);
        }

        int[] result = new int[n];
        int idx = 0;

        while (!queue.isEmpty()) {
            int node = queue.poll();
            result[idx++] = node;

            for (int neighbor : adj.get(node)) {
                indegree[neighbor]--;
                if (indegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // Cycle check: if we couldn't process all n nodes, a cycle exists.
        if (idx != n) return new int[0]; // or throw an exception, depending on API contract

        return result;
    }
}
```

### 6.4 Cycle Detection — For Free, Automatically

This is the elegant part: **you don't need a separate cycle-detection algorithm for Kahn's.** If a cycle exists, the nodes *inside* the cycle will **never** reach in-degree 0 (each depends on another inside the same loop), so they never enter the queue.

**Rule of thumb**: `if (count of processed nodes != n) → cycle exists.`

```
Cyclic subgraph example:      In-degree of A, B, C all = 1 forever
     A -> B -> C -> A          (each waits on the other -> none ever hits 0)
```

---

## 7. DFS vs Kahn's — When To Use Which

| Aspect | DFS-based | Kahn's (BFS-based) |
|---|---|---|
| Core idea | Post-order + reverse | Peel off in-degree-0 nodes iteratively |
| Data structure | Recursion stack + explicit stack | Queue + in-degree array |
| Cycle detection | Needs 3-color marking | Automatic (compare count to n) |
| Space | O(V) recursion depth (risk of stack overflow on huge/deep graphs) | O(V) queue, no recursion — safer for very large graphs |
| Natural fit for... | When you already have DFS traversal code / love recursion | When you need **level-by-level** processing (e.g., "parallel batches", "minimum time to complete all jobs") |
| Gives lexicographically smallest order easily? | No (order depends on DFS traversal order, tricky to control) | **Yes** — swap `Queue` for a `PriorityQueue` (min-heap) |
| Industry usage | Compilers, some schedulers | Build systems (Maven), task schedulers, most practical systems |

**My honest recommendation**: **default to Kahn's algorithm** in interviews and real systems. It naturally gives you cycle detection, is iterative (no stack overflow risk), and extends cleanly to advanced variants (below). Learn DFS-based too because interviewers sometimes explicitly ask for it, and because the "finish-time" idea reappears elsewhere in graph theory (e.g., Tarjan's SCC algorithm).

---

## 8. Complexity Analysis (Both Approaches)

Let `V` = number of vertices, `E` = number of edges.

- **Time**: `O(V + E)` for both approaches. Each vertex is processed once, each edge is inspected once (during DFS traversal, or during in-degree decrementing).
- **Space**: `O(V + E)` for the adjacency list + `O(V)` for the visited/indegree arrays + `O(V)` for the queue/stack.

This is optimal — you cannot do better than looking at every vertex and edge at least once, since the answer depends on all of them.

---

## 9. Advanced Variant #1 — Lexicographically Smallest Topological Order

**Problem shape**: "Among all valid topological orders, return the smallest one (numerically/alphabetically)."

**Key trick**: Swap the `Queue` in Kahn's algorithm for a **min-heap (`PriorityQueue`)**. At each step, instead of picking *any* in-degree-0 node, you greedily pick the **smallest available** one.

```java
import java.util.*;

public class LexicographicTopoSort {
    public int[] smallestTopoOrder(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        int[] indegree = new int[n];

        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            indegree[e[1]]++;
        }

        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        for (int i = 0; i < n; i++) if (indegree[i] == 0) minHeap.offer(i);

        int[] result = new int[n];
        int idx = 0;

        while (!minHeap.isEmpty()) {
            int node = minHeap.poll(); // always the smallest eligible node
            result[idx++] = node;
            for (int neighbor : adj.get(node)) {
                if (--indegree[neighbor] == 0) minHeap.offer(neighbor);
            }
        }

        return idx == n ? result : new int[0]; // empty => cycle
    }
}
```

**Why is this correct (and not just "probably" correct)?** Greedy correctness argument: at every step, among *all* nodes with satisfied dependencies, choosing the smallest one can never hurt — since it has no unresolved dependency, placing it now is always legal, and placing anything bigger first while a smaller legal option exists would make the result lexicographically bigger. This is a textbook exchange-argument proof, the same style used to justify most greedy algorithms.

This exact pattern appears in **LeetCode "Alien Dictionary"** (build edges from letter-order clues comparing adjacent words, then topo sort; ties broken alphabetically) and **"Course Schedule IV / II variants."**

---

## 10. Advanced Variant #2 — All Possible Topological Orderings

**Problem shape**: "Print/return *every* valid topological order" (used less often — expensive, exponential in the worst case).

**Approach**: Backtracking. At each step, try **every currently-available (in-degree 0) node**, not just one; recurse; then backtrack (undo the choice, restore in-degrees) and try the next option.

```java
import java.util.*;

public class AllTopoOrders {
    private List<List<Integer>> allOrders = new ArrayList<>();

    public List<List<Integer>> findAll(int n, int[][] edges) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        int[] indegree = new int[n];
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            indegree[e[1]]++;
        }

        boolean[] visited = new boolean[n];
        backtrack(n, adj, indegree, visited, new ArrayList<>());
        return allOrders;
    }

    private void backtrack(int n, List<List<Integer>> adj, int[] indegree,
                            boolean[] visited, List<Integer> path) {
        if (path.size() == n) {
            allOrders.add(new ArrayList<>(path));
            return;
        }

        for (int node = 0; node < n; node++) {
            if (!visited[node] && indegree[node] == 0) {
                // choose
                visited[node] = true;
                path.add(node);
                for (int nb : adj.get(node)) indegree[nb]--;

                backtrack(n, adj, indegree, visited, path);

                // un-choose (backtrack)
                visited[node] = false;
                path.remove(path.size() - 1);
                for (int nb : adj.get(node)) indegree[nb]++;
            }
        }
    }
}
```

**Complexity warning**: this can be exponential (worst case: a graph with no edges at all has `n!` orderings). Only use when the problem explicitly asks for all orderings on small `n`.

---

## 11. Advanced Variant #3 — Longest Path in a DAG / Minimum Time to Finish All Jobs

**Problem shape**: "Each task takes some time. Task B can't start until all its prerequisite tasks finish. What is the minimum total time to finish everything?" (This is literally the **critical path method** used in real project management / CPU instruction scheduling.)

**Key insight**: Process nodes in topological order (so by the time you reach a node, all its prerequisites are already finalized), and do simple DP:

```
finishTime[v] = duration[v] + max(finishTime[u] for every prerequisite u of v)
answer = max(finishTime[v] for all v)
```

```java
import java.util.*;

public class MinTimeToFinishAllJobs {
    public int minTime(int n, int[][] edges, int[] duration) {
        List<List<Integer>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
        int[] indegree = new int[n];
        for (int[] e : edges) {
            adj.get(e[0]).add(e[1]);
            indegree[e[1]]++;
        }

        Queue<Integer> queue = new LinkedList<>();
        int[] finishTime = new int[n];
        for (int i = 0; i < n; i++) {
            if (indegree[i] == 0) {
                queue.offer(i);
                finishTime[i] = duration[i]; // no prerequisites -> starts at time 0
            }
        }

        int answer = 0;
        int processed = 0;

        while (!queue.isEmpty()) {
            int node = queue.poll();
            processed++;
            answer = Math.max(answer, finishTime[node]);

            for (int neighbor : adj.get(node)) {
                // neighbor can only start after 'node' finishes
                finishTime[neighbor] = Math.max(finishTime[neighbor], finishTime[node] + duration[neighbor]);
                if (--indegree[neighbor] == 0) queue.offer(neighbor);
            }
        }

        return processed == n ? answer : -1; // -1 if cycle (impossible)
    }
}
```

**This is "topological sort + DP,"** an extremely common advanced combo. Recognize it whenever a topo-sort-shaped problem also has **weights, costs, or durations** attached to nodes/edges and asks for a *min/max* value rather than just an *order*.

---

## 12. Advanced Variant #4 — Checking For a UNIQUE Topological Order

**Problem shape**: "Does this graph have exactly one valid topological order?"

**Key insight**: A topo order is unique **if and only if, at every single step of Kahn's algorithm, the queue never has more than 1 node in it.** If, at any point, two or more nodes are simultaneously eligible (in-degree 0), you could swap their relative order, so it's not unique.

```java
public boolean hasUniqueTopoOrder(int n, int[][] edges) {
    List<List<Integer>> adj = new ArrayList<>();
    for (int i = 0; i < n; i++) adj.add(new ArrayList<>());
    int[] indegree = new int[n];
    for (int[] e : edges) { adj.get(e[0]).add(e[1]); indegree[e[1]]++; }

    Queue<Integer> queue = new LinkedList<>();
    for (int i = 0; i < n; i++) if (indegree[i] == 0) queue.offer(i);

    int processed = 0;
    while (!queue.isEmpty()) {
        if (queue.size() > 1) return false; // more than 1 choice available -> not unique
        int node = queue.poll();
        processed++;
        for (int nb : adj.get(node)) {
            if (--indegree[nb] == 0) queue.offer(nb);
        }
    }
    return processed == n; // also must not have a cycle
}
```

---

## 13. Common Pitfalls (Where People Actually Get Stuck)

1. **Pushing to the stack at the wrong time in DFS** — must be *after* the recursive calls (post-order), not before.
2. **Forgetting to check for disconnected components** — you must loop over *all* nodes and start a fresh DFS/BFS for any unvisited one; a graph isn't always one connected blob.
3. **Confusing "visited" with "on current path"** — leads to missed cycle detection in DFS (fix: 3-color method).
4. **Assuming topo order is unique** — it's usually not; don't hardcode test expectations around one specific valid order unless the problem demands lexicographic smallest.
5. **Forgetting the cycle check** — always verify `processed nodes == n` (Kahn's) or use proper 3-coloring (DFS) before trusting the output.
6. **Edge direction confusion** — double check whether `edges[i] = [a, b]` means "a before b" or "b before a" in the problem statement; this flips your whole adjacency list and is a very common silent bug.
7. **Off-by-one / self-loops** — a self-loop (`u → u`) is technically a cycle of length 1; make sure your cycle detection catches it (it will, naturally, in both methods above).

---

## 14. Real-World Applications (So You See This Everywhere Now)

- **Build systems**: Maven/Gradle/Bazel compute a dependency graph of modules and compile in topological order.
- **Package managers**: `npm install`, `pip install` resolve dependency trees this way.
- **Spreadsheet engines**: Excel/Google Sheets recompute formula cells in dependency order.
- **Course registration systems**: exactly the "Course Schedule" LeetCode problem, for real.
- **CPU instruction scheduling / compilers**: instruction dependency graphs (an instruction can't execute before the registers it reads are written).
- **Makefiles**: `make` decides which targets to rebuild and in what order based on file dependency timestamps + declared dependencies.
- **Task orchestration** (Airflow DAGs, CI/CD pipelines like GitHub Actions `needs:`): literally called "DAGs" in the tool itself.

---

## 15. Classic Practice Problems, Mapped to What You Just Learned

| Problem | Which technique |
|---|---|
| Course Schedule (can you finish all courses?) | Kahn's, just check `processed == n` |
| Course Schedule II (return the order) | Kahn's or DFS, return full order |
| Course Schedule IV (queries: does A depend on B?) | Topo sort + reachability DP over the DAG |
| Alien Dictionary | Build edges from adjacent word comparisons, then topo sort (lexicographically smallest if tie-break needed) |
| Parallel Courses / Minimum semesters | Kahn's, but track "levels" (BFS layers) = number of semesters |
| Parallel Courses III (with durations) | Longest path in DAG (Section 11) |
| Sequence Reconstruction | Check if topo order is unique + matches given sequence |
| Minimum Height Trees | Not quite topo sort but same "peel the leaves" (in-degree 1) BFS idea — cousin technique worth knowing |
| Find Eventual Safe States | Reverse graph + Kahn's / or DFS with coloring for "terminal-reachable" nodes |

**Practical tip on "levels" (batch processing)**: when a problem asks "what's the minimum number of rounds/semesters/batches to finish everything if you can do any number of independent tasks in parallel per round," process Kahn's algorithm **level by level** (like BFS level-order traversal) — pop the *entire current queue* as one batch, then move to the next batch. That's it — same algorithm, just processed level-by-level instead of one node at a time.

```java
// Level-by-level Kahn's (minimum rounds pattern)
int rounds = 0;
while (!queue.isEmpty()) {
    int size = queue.size();
    for (int i = 0; i < size; i++) {
        int node = queue.poll();
        for (int nb : adj.get(node)) {
            if (--indegree[nb] == 0) queue.offer(nb);
        }
    }
    rounds++;
}
```

---

## 16. Mental Model Summary (The "Master's Cheat Sheet")

```
┌─────────────────────────────────────────────────────────────┐
│  IS IT A TOPO SORT PROBLEM?                                  │
│  -> "before/after", "prerequisite", "dependency", "order"    │
│                                                                │
│  BUILD:                                                       │
│  -> adjacency list (u -> v means u before v)                 │
│  -> in-degree array (for Kahn's)                              │
│                                                                │
│  PICK YOUR ALGORITHM:                                         │
│  -> Need it fast & safe from stack overflow -> Kahn's (BFS)   │
│  -> Need lexicographically smallest -> Kahn's + PriorityQueue │
│  -> Need ALL orderings -> Backtracking                        │
│  -> Have durations & need min/max total time -> Kahn's + DP   │
│  -> Need "rounds/batches" count -> Kahn's, level-by-level     │
│  -> Just love recursion / need finish-time semantics -> DFS   │
│                                                                │
│  ALWAYS CHECK FOR CYCLES:                                     │
│  -> Kahn's: processed count != n  => cycle                    │
│  -> DFS: gray node revisited      => cycle                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 17. Suggested Practice Path (Beginner → Advanced)

1. Implement both DFS-based and Kahn's topo sort from scratch, from memory, without looking anything up.
2. Solve: Course Schedule I, then II (LeetCode 207, 210).
3. Solve: Alien Dictionary (LeetCode 269 — or its close variants).
4. Solve: Parallel Courses (levels/rounds variant).
5. Solve: Parallel Courses III (durations + longest path DP).
6. Implement "all topological orders" via backtracking for a small graph (n ≤ 8) and manually verify the count.
7. Implement the "unique topological order" checker.
8. Bonus (stretch): implement **Tarjan's SCC algorithm** — it reuses the DFS finish-time intuition from Section 5 and will deepen your understanding of *why* that trick works.

If you want, I can turn any of steps 2–7 into a hands-on exercise with a deliberately broken implementation for you to debug — that tends to cement this kind of pattern far better than reading code.
