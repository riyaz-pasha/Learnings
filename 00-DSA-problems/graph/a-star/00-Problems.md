# **A\* (A-Star) Algorithm — Complete Problem Roadmap: Beginner to Advanced**

If your goal is to master A\* for Google, Meta, and other DSA interviews, I recommend studying it as a combination of **graph traversal, shortest-path algorithms, heuristic search, and state-space search**.

There is one important distinction to understand before building the problem list:

*Not every shortest-path problem is an A problem.*\* A problem may be solvable using A\*, but that does not mean A\* is its intended or most appropriate solution.

I'll separate the roadmap into:

1. *Core A problems:*\* Problems where A\* is a natural, central, or explicitly expected solution.
2. *A applicable problems:*\* Problems conventionally solved using BFS, Dijkstra, or other algorithms, but where A\* can be applied meaningfully.
3. **Advanced problems:** Problems that develop heuristic design, state-space search, and A\* optimizations.
4. **Final priority list:** What you should actually solve to achieve mastery.

One more interview-related point: A\* is much less frequently tested directly than BFS, DFS, Dijkstra, Union-Find, and Dynamic Programming. Nevertheless, mastering it gives you a useful advantage in shortest-path problems with large search spaces.

# **1. Understanding the A\* problem landscape**

Before looking at individual problems, let's establish what makes a problem an A\* problem.

A\* uses the following evaluation function:

\\[ f(n)=g(n)+h(n) \\]

Where:

- \\(g(n)\\): Actual cost from the starting node to the current node.
- \\(h(n)\\): Estimated minimum remaining cost from the current node to the goal.
- \\(f(n)\\): Estimated total cost of a path through the current node.

A\* expands the node with the smallest \\(f(n)\\).

For example, suppose you are navigating a grid:

```
S . . . .
. # # . .
. . . . .
# # . . G
```

You want to reach `G` from `S`.

A normal BFS explores nodes level by level.

Dijkstra explores nodes based on the actual cost accumulated so far.

A\* uses both actual cost and estimated remaining cost. It prioritizes nodes that appear more promising.

### **Important heuristic functions to master**

| Heuristic                   | Suitable problem                                             |
| --------------------------- | ------------------------------------------------------------ |
| Manhattan Distance          | Four-directional grid movement                               |
| Euclidean Distance          | Continuous/geometric movement                                |
| Chebyshev Distance          | Eight-directional movement with equal diagonal cost          |
| Octile Distance             | Eight-directional movement with diagonal cost \\(\sqrt{2}\\) |
| Misplaced Tiles             | Sliding puzzle                                               |
| Manhattan + Linear Conflict | 8-puzzle / 15-puzzle                                         |
| Landmark / ALT              | Large road networks and graphs                               |
| Pattern Database            | Complex state-space search                                   |

Two properties are particularly important:

- **Admissibility:** The heuristic never overestimates the actual remaining optimal cost.
- **Consistency:** \\(h(n) \leq c(n,n') + h(n')\\) for every edge.

For standard optimal A\*, an admissible heuristic is essential. Consistency makes graph-search implementations simpler because settled states do not need to be reopened.

# **2. Core A\* problems — Truly solved using A\***

These are the problems I would prioritize when learning the actual algorithm rather than simply solving shortest-path problems.

One qualification: A\* is a natural solution for these problems, but some online judges also accept BFS or other algorithms. I distinguish the algorithmic fit from the judge's intended solution.

## **A. Beginner — Basic A\* implementation**

| # | Problem                                                                                                                                                            | Difficulty | What you learn                                | Priority |
| - | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---------- | --------------------------------------------- | -------- |
| 1 | [A\* on a 4-directional Grid](https://senior-stack.uz/Roadmap/Data/datastructures-and-algorithms/11-graphs/10-a-star/tasks/?utm_source=chatgpt.com)                | Easy       | Basic A\*, Manhattan heuristic, PriorityQueue | P1       |
| 2 | [Grid Pathfinding — Path Reconstruction](https://senior-stack.uz/Roadmap/Data/datastructures-and-algorithms/11-graphs/10-a-star/tasks/?utm_source=chatgpt.com)     | Easy       | Parent tracking, reconstructing optimal paths | P1       |
| 3 | [A\* with h(n) = 0](https://senior-stack.uz/Roadmap/Data/datastructures-and-algorithms/11-graphs/10-a-star/tasks/?utm_source=chatgpt.com)                          | Easy       | Understanding why A\* becomes Dijkstra        | P1       |
| 4 | [Choosing Heuristics for 8-Directional Grid](https://senior-stack.uz/Roadmap/Data/datastructures-and-algorithms/11-graphs/10-a-star/tasks/?utm_source=chatgpt.com) | Easy       | Manhattan, Chebyshev, Octile                  | P1       |
| 5 | [Weighted Terrain Grid Pathfinding](https://senior-stack.uz/Roadmap/Data/datastructures-and-algorithms/11-graphs/10-a-star/tasks/?utm_source=chatgpt.com)          | Medium     | A\* with different movement costs             | P1       |

These are structured implementation exercises rather than conventional LeetCode problems.

**Why are they important?**

You should be able to implement A\* from scratch before attempting complicated state-space problems.

In particular, understand:

- Open set (PriorityQueue).
- Closed set (visited/settled states).
- `gScore` tracking.
- `fScore` calculation.
- Parent tracking.
- Stale priority-queue entries.
- Goal termination conditions.

## **B. Intermediate — Puzzle and state-space search**

These are particularly valuable because they force you to design meaningful heuristics.

| #  | Problem                                                                                                                                                                           | Difficulty | A\* technique                                | Priority |
| -- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------- | -------------------------------------------- | -------- |
| 6  | [Sliding Puzzle — LeetCode 773](https://leetcode.com/problems/sliding-puzzle/?utm_source=chatgpt.com)                                                                             | Hard       | Manhattan distance, state-space search       | P1       |
| 7  | [8-Puzzle Problem](https://www.uni-weimar.de/fileadmin/user/fak/medien/professuren/Webis/teaching/ws14/search-algorithms/exercises-en-search-theory-2.pdf?utm_source=chatgpt.com) | Medium     | Misplaced tiles vs Manhattan heuristic       | P1       |
| 8  | [15-Puzzle Problem](https://github.com/AhmedElag/puzzle-solver-a-star?utm_source=chatgpt.com)                                                                                     | Hard       | Manhattan distance, Linear Conflict          | P1       |
| 9  | [Minimum Knight Moves](https://senior-stack.uz/Roadmap/Data/datastructures-and-algorithms/11-graphs/10-a-star/tasks/?utm_source=chatgpt.com)                                      | Medium     | Coordinate-based heuristic                   | P2       |
| 10 | [Word Ladder — LeetCode 127](https://leetcode.com/problems/word-ladder/?utm_source=chatgpt.com)                                                                                   | Hard       | Heuristic-guided search over implicit graphs | P2       |

### **Particularly important: Sliding Puzzle**

Sliding Puzzle (LeetCode 773)

This is one of the best interview-oriented problems for learning A\*.

You have a 2 × 3 board:

```
4 1 2
5 0 3
```

Target:

```
1 2 3
4 5 0
```

You can move the empty cell (`0`) by swapping it with an adjacent tile.

Find the minimum number of moves.

*Why A is useful here:*\*

Instead of exploring all possible board configurations equally, we estimate how far each configuration is from the target.

For every tile:

\\[ h(n)=\sum\_{tile=1}^{5} (|r\_{current}-r\_{target}|+ |c\_{current}-c\_{target}|) \\]

This is Manhattan distance.

The board becomes a state, and each valid swap becomes an edge.

The important lessons:

1. Representing a board as a state.
2. Generating neighboring states.
3. Designing admissible heuristics.
4. Avoiding repeated state exploration.
5. Comparing BFS and A\*.

**Important:** LeetCode 773 officially supports heuristic search/A\* as a topic, but its standard hint recommends BFS. Solve it with BFS first, then implement A\* to understand the difference.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://leetcode.com\&sz=32)

LeetCode+1



### **Advanced puzzle extension: 15-Puzzle**

The 15-puzzle is a 4 × 4 version of the sliding puzzle.

This is a classic A\* benchmark.

You should experiment with:

- Misplaced tiles.
- Manhattan distance.
- Manhattan distance + Linear Conflict.
- Pattern Database heuristics.
- IDA\* (Iterative Deepening A\*).

The interesting part is that better heuristics can dramatically reduce the number of explored states while preserving optimality.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://ieeexplore.ieee.org\&sz=32)

IEEE Xplore+2



## **C. Advanced — K-th shortest path using A\***

| #  | Problem                                                                                            | Difficulty | A\* technique          | Priority |
| -- | -------------------------------------------------------------------------------------------------- | ---------- | ---------------------- | -------- |
| 11 | [Remmarguts' Date — POJ 2449](https://acm.taifua.com/archives/poj2449.html?utm_source=chatgpt.com) | Hard       | A\* + Reverse Dijkstra | P1       |

This is one of the most important problems in the entire A\* roadmap.

### **Why is it important?**

Normal shortest-path algorithms find the shortest path.

But what if the question asks:

**Find the K-th shortest path between two vertices.**

For example:

```
S -------- A -------- G     Cost = 10
 \          \
  \          B -------- G   Cost = 12
   \
    C ---------------- G   Cost = 15
```

You need the second or third shortest path, not just the shortest.

A\* can help solve this efficiently.

The important technique is:

1. Reverse the graph.
2. Run Dijkstra from the target.
3. Calculate the exact shortest remaining distance from every node to the target.
4. Use that distance as the heuristic.
5. Run A\* from the source, enumerating paths in increasing estimated total cost.

Here, the heuristic is particularly powerful because it is the exact remaining shortest distance.

This problem teaches a very important combination:

*A + Dijkstra + Reverse Graph + K-th Shortest Path.*\*

I strongly recommend solving it after understanding basic A\*.

[Cite: POJ 2449 problem statement and explanation](https://acm.taifua.com/archives/poj2449.html?utm_source=chatgpt.com).&#x20;

[image](https://www.google.com/s2/favicons?domain=https://acm.taifua.com\&sz=32)

太傅博客



# **3. Problems that CAN be solved using A\***

This category is equally important, but don't confuse algorithm applicability with the intended solution.

These problems are generally solved using BFS, Dijkstra, 0-1 BFS, or other graph algorithms. You can design A\* heuristics for them, but A\* is not necessarily the best choice.

## **A. Grid shortest-path problems**

| # | Problem                                            | LeetCode                                                                                                             | Standard solution        | Possible A\* heuristic              | Priority |
| - | -------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- | ------------------------ | ----------------------------------- | -------- |
| 1 | Shortest Path in Binary Matrix                     | [1091](https://leetcode.com/problems/shortest-path-in-binary-matrix/?utm_source=chatgpt.com)                         | BFS                      | Chebyshev                           | P1       |
| 2 | Shortest Path in a Grid with Obstacles Elimination | [1293](https://leetcode.com/problems/shortest-path-in-a-grid-with-obstacles-elimination/?utm_source=chatgpt.com)     | BFS                      | Manhattan                           | P1       |
| 3 | Minimum Obstacle Removal to Reach Corner           | [2290](https://leetcode.com/problems/minimum-obstacle-removal-to-reach-corner/?utm_source=chatgpt.com)               | 0-1 BFS                  | Lower bound on obstacle removals    | P1       |
| 4 | Minimum Cost to Make at Least One Valid Path       | [1368](https://leetcode.com/problems/minimum-cost-to-make-at-Least-One-Valid-Path-in-a-Grid/?utm_source=chatgpt.com) | 0-1 BFS                  | Lower bound on direction changes    | P2       |
| 5 | Path With Minimum Effort                           | [1631](https://leetcode.com/problems/path-with-minimum-effort/?utm_source=chatgpt.com)                               | Dijkstra / Binary Search | Lower bound on bottleneck effort    | P2       |
| 6 | Swim in Rising Water                               | [778](https://leetcode.com/problems/swim-in-rising-water/?utm_source=chatgpt.com)                                    | Dijkstra / Union-Find    | Lower bound on required water level | P2       |
| 7 | Cut Off Trees for Golf Event                       | [675](https://leetcode.com/problems/cut-off-trees-for-golf-event/?utm_source=chatgpt.com)                            | BFS                      | Manhattan distance between targets  | P2       |

### **Which ones are most valuable?**

**1091 — Shortest Path in Binary Matrix**

A good starting point for comparing BFS and A\*.

- BFS explores all reachable cells level by level.
- A\* prioritizes cells closer to the destination.
- For eight-directional movement with equal movement cost, Chebyshev distance is appropriate.

**1293 — Shortest Path in a Grid with Obstacles Elimination**

This is particularly interesting.

The state is not simply:

\\[ (row,col) \\]

It is:

\\[ (row,col,remainingObstacles) \\]

A\* must account for both position and remaining elimination capacity.

Manhattan distance provides a lower bound on the number of remaining moves.

This problem is excellent for understanding A\* with augmented states. The conventional solution is BFS with state tracking.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://leetcode.doocs.org\&sz=32)

LeetCode Wiki+1



## **B. Complex state-space problems**

| #  | Problem                                              | Link                                                                                                                        | Standard solution       | A\* applicability          | Priority |
| -- | ---------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- | ----------------------- | -------------------------- | -------- |
| 8  | Shortest Path to Get All Keys                        | [LeetCode 864](https://leetcode.com/problems/shortest-path-to-get-all-keys/?utm_source=chatgpt.com)                         | BFS + Bitmask           | Strong                     | P1       |
| 9  | Shortest Path Visiting All Nodes                     | [LeetCode 847](https://leetcode.com/problems/shortest-path-visiting-all-nodes/?utm_source=chatgpt.com)                      | BFS + Bitmask           | Strong                     | P1       |
| 10 | Minimum Moves to Move a Box to Their Target Location | [LeetCode 1263](https://leetcode.com/problems/minimum-moves-to-move-a-box-to-their-target-location/?utm_source=chatgpt.com) | BFS / 0-1 BFS variants  | Strong                     | P1       |
| 11 | Escape a Large Maze                                  | [LeetCode 1036](https://leetcode.com/problems/escape-a-large-maze/?utm_source=chatgpt.com)                                  | BFS / Bidirectional BFS | Limited benefit            | P2       |
| 12 | Minimum Jumps to Reach Home                          | [LeetCode 1654](https://leetcode.com/problems/minimum-jumps-to-reach-home/?utm_source=chatgpt.com)                          | BFS                     | Manhattan-like lower bound | P2       |

### **Why these matter**

These problems teach an important A\* concept:

**A search node does not have to represent a physical location. It can represent an entire configuration of the problem.**

For example, in LeetCode 864, your state is:

\\[ (row,col,keyMask) \\]

In LeetCode 847:

\\[ (node,visitedMask) \\]

In LeetCode 1263:

\\[ (playerPosition,boxPosition) \\]

This is an important transition from basic grid pathfinding to advanced state-space search.

For 864, for example, a possible heuristic is a lower bound on the remaining distance needed to collect all uncollected keys. A minimum spanning tree over the remaining keys, using relaxed shortest-path distances, is one possible construction.

However, designing an admissible heuristic is considerably more challenging than simply using Manhattan distance.&#x20;

[image](https://www.google.com/s2/favicons?domain=https://leetcode.doocs.org\&sz=32)

LeetCode Wiki+2



# **4. Advanced A\* topics and unique problems worth studying**

If you want to go beyond basic A\*, I recommend learning the following extensions.

These are not all separate LeetCode problems. Some are algorithmic techniques that you should deliberately practise.

| #  | Topic                        | What you should learn                             | Recommended practice     |
| -- | ---------------------------- | ------------------------------------------------- | ------------------------ |
| 1  | Weighted A\*                 | Trade-off between optimality and search speed     | Weighted Terrain Grid    |
| 2  | IDA\*                        | Iterative Deepening A\*, memory-efficient search  | 15-Puzzle                |
| 3  | Bidirectional A\*            | Search from both source and destination           | Grid Pathfinding         |
| 4  | Multi-Heuristic A\*          | Combining multiple heuristics                     | 15-Puzzle                |
| 5  | Pattern Database             | Precomputed lower-bound heuristics                | 15-Puzzle                |
| 6  | Landmark A\* (ALT)           | Landmark-based shortest-path heuristics           | Large Graph              |
| 7  | Memory-Bounded A\* (SMA\*)   | Managing memory limitations                       | Large state-space search |
| 8  | Dynamic A\* (D\* / D\* Lite) | Pathfinding when the environment changes          | Dynamic Grid Navigation  |
| 9  | Any-angle pathfinding        | Paths that are not restricted to grid edges       | Theta\*                  |
| 10 | K-th Shortest Path           | A\* combined with reverse shortest-path distances | POJ 2449                 |

### **Recommended additional resources**

These are useful for understanding advanced techniques and experimenting with actual pathfinding instances.

1. [A\* Search Practice Tasks](https://senior-stack.uz/Roadmap/Data/datastructures-and-algorithms/11-graphs/10-a-star/tasks/?utm_source=chatgpt.com) — Structured beginner-to-advanced exercises.
2. [Pathfinding Benchmarks](https://benchmarks.pathfinding.ai/2d-benchmarks/grid-maps/?utm_source=chatgpt.com) — Real-world and synthetic grid maps for testing pathfinding algorithms.
3. [15-Puzzle A\* Implementation](https://github.com/AhmedElag/puzzle-solver-a-star?utm_source=chatgpt.com) — Compare Manhattan distance, misplaced tiles, and Linear Conflict.
4. [15-Puzzle Heuristic Search Research](https://ai.dmi.unibas.ch/papers/haslum-et-al-aaai07.pdf?utm_source=chatgpt.com) — Pattern databases and advanced heuristics.

[image](https://www.google.com/s2/favicons?domain=https://senior-stack.uz\&sz=32)

Senior Stack+3

# **5. Problems I would NOT classify as core A\***

This distinction is important because you don't want to waste time solving hundreds of shortest-path problems under the A\* label.

| Problem                               | Primary algorithm                    | Why A\* isn't necessary                         |
| ------------------------------------- | ------------------------------------ | ----------------------------------------------- |
| Network Delay Time (743)              | Dijkstra                             | General weighted shortest path                  |
| Cheapest Flights Within K Stops (787) | Modified Bellman-Ford / BFS variants | Limited number of stops                         |
| Path With Minimum Effort (1631)       | Dijkstra / Binary Search             | A\* requires a useful bottleneck-cost heuristic |
| Minimum Obstacle Removal (2290)       | 0-1 BFS                              | Edge costs are only 0 or 1                      |
| Word Ladder (127)                     | BFS / Bidirectional BFS              | Smallest-depth search is sufficient             |
| Shortest Path to Get All Keys (864)   | BFS + Bitmask                        | Small bounded state space                       |
| Swim in Rising Water (778)            | Dijkstra / Union-Find                | A\* does not provide a necessary advantage      |

You can experiment with A\* on these problems, but don't count them as A\* mastery problems.

The goal is to recognize when A\* actually provides an advantage.

# **6. My recommended final problem-solving roadmap**

I would organize your preparation into four phases.

## **Phase 1 — A\* fundamentals**

**Goal:** Become comfortable implementing A\* from scratch.

| Order | Problem                       | Priority   |
| ----- | ----------------------------- | ---------- |
| 1     | A\* on 4-directional Grid     | Must Solve |
| 2     | Grid Path Reconstruction      | Must Solve |
| 3     | A\* with h(n) = 0             | Must Solve |
| 4     | 8-directional Grid Heuristics | Must Solve |
| 5     | Weighted Terrain Grid         | Must Solve |

At the end of this phase, you should be able to implement A\* without referring to a template.

## **Phase 2 — Heuristic design**

**Goal:** Understand how to construct good admissible heuristics.

| Order | Problem                       | Priority    |
| ----- | ----------------------------- | ----------- |
| 6     | LeetCode 773 — Sliding Puzzle | Must Solve  |
| 7     | 8-Puzzle                      | Must Solve  |
| 8     | 15-Puzzle                     | Must Solve  |
| 9     | Minimum Knight Moves          | Recommended |
| 10    | LeetCode 127 — Word Ladder    | Recommended |

This is probably the most important phase for understanding the real power of A\*.

## **Phase 3 — Advanced graph search**

**Goal:** Combine A\* with other graph algorithms.

| Order | Problem                                         | Priority    |
| ----- | ----------------------------------------------- | ----------- |
| 11    | POJ 2449 — K-th Shortest Path                   | Must Solve  |
| 12    | LeetCode 1293 — Obstacles Elimination           | Recommended |
| 13    | LeetCode 1091 — Shortest Path in Binary Matrix  | Recommended |
| 14    | LeetCode 864 — Shortest Path to Get All Keys    | Recommended |
| 15    | LeetCode 847 — Shortest Path Visiting All Nodes | Recommended |
| 16    | LeetCode 1263 — Minimum Moves to Move a Box     | Recommended |

## **Phase 4 — Advanced A\* variations**

**Goal:** Understand the algorithm beyond typical interview implementations.

| Order | Topic                     | Priority   |
| ----- | ------------------------- | ---------- |
| 17    | IDA\*                     | Must Learn |
| 18    | Bidirectional A\*         | Must Learn |
| 19    | Weighted A\*              | Must Learn |
| 20    | Linear Conflict Heuristic | Must Learn |
| 21    | Pattern Database          | Advanced   |
| 22    | Landmark / ALT Heuristic  | Advanced   |
| 23    | Memory-Bounded A\*        | Advanced   |
| 24    | D\* Lite                  | Advanced   |

# **7. Final must-solve list**

If I had to reduce the entire roadmap to a compact list, these would be my recommendations.

| #  | Problem                                         | Category              | Priority    |
| -- | ----------------------------------------------- | --------------------- | ----------- |
| 1  | A\* on 4-directional Grid                       | Basic A\*             | Essential   |
| 2  | A\* with Path Reconstruction                    | Basic A\*             | Essential   |
| 3  | A\* with Zero Heuristic                         | A\* vs Dijkstra       | Essential   |
| 4  | Weighted Terrain Grid                           | Weighted A\*          | Essential   |
| 5  | LeetCode 773 — Sliding Puzzle                   | State-space A\*       | Essential   |
| 6  | 8-Puzzle                                        | Heuristic Design      | Essential   |
| 7  | 15-Puzzle                                       | Advanced Heuristics   | Essential   |
| 8  | POJ 2449 — K-th Shortest Path                   | A\* + Dijkstra        | Essential   |
| 9  | LeetCode 1293 — Obstacles Elimination           | A\* + Augmented State | Recommended |
| 10 | LeetCode 1091 — Shortest Path in Binary Matrix  | Grid A\*              | Recommended |
| 11 | LeetCode 864 — Shortest Path to Get All Keys    | A\* + Bitmask         | Recommended |
| 12 | LeetCode 847 — Shortest Path Visiting All Nodes | A\* + Bitmask         | Recommended |
| 13 | LeetCode 1263 — Minimum Moves to Move a Box     | State-space Search    | Recommended |
| 14 | Minimum Knight Moves                            | Coordinate Heuristic  | Recommended |
| 15 | 15-Puzzle with IDA\*                            | Advanced A\*          | Advanced    |

**My recommendation:** Don't solve every problem in the applicable category using A\*. Solve the standard solution first, then implement A\* where it offers a meaningful comparison.

# **8. What you must master to claim A\* mastery**

Use this checklist to evaluate yourself.

### **A. Algorithm fundamentals**

- Understand \\(f(n)=g(n)+h(n)\\).
- Implement A\* using Java `PriorityQueue`.
- Understand Open Set and Closed Set.
- Maintain `gScore` and parent pointers.
- Handle stale PriorityQueue entries.
- Reconstruct the shortest path.
- Understand when a state needs reopening.

### **B. Heuristic design**

- Manhattan Distance.
- Euclidean Distance.
- Chebyshev Distance.
- Octile Distance.
- Misplaced Tiles.
- Linear Conflict.
- Landmark Heuristics.
- Pattern Database Heuristics.

### **C. Correctness and optimization**

- Admissibility.
- Consistency.
- Optimality guarantees.
- A\* vs Dijkstra.
- A\* vs BFS.
- A\* vs Greedy Best-First Search.
- Time and space complexity.
- When A\* becomes inefficient.
- How to choose an effective heuristic.

### **D. Advanced variations**

- IDA\*.
- Bidirectional A\*.
- Weighted A\*.
- Memory-Bounded A\*.
- D\* and D\* Lite.
- K-th Shortest Path using A\*.

# **9. Important interview perspective**

Since your preparation targets demanding DSA interviews, I would put A\* in the following context:

| Algorithm        | Interview importance                                | Preparation priority |
| ---------------- | --------------------------------------------------- | -------------------- |
| BFS / DFS        | Very High                                           | Highest              |
| Dijkstra         | Very High                                           | Highest              |
| Topological Sort | Very High                                           | Highest              |
| Union-Find       | Very High                                           | Highest              |
| Bellman-Ford     | High                                                | High                 |
| Floyd-Warshall   | Medium                                              | Medium               |
| A\*              | Lower direct frequency, valuable advanced knowledge | Medium               |
| IDA\*            | Low direct frequency                                | Advanced             |

A\* is particularly useful when:

- The search space is huge.
- You know the destination.
- You can construct a meaningful lower-bound heuristic.
- Exploring every reachable state is too expensive.
- You need an optimal solution and can use an admissible heuristic.

It is less useful when the graph is small, the search space is already manageable, or you cannot construct a useful heuristic.

**One important warning:** A\* is not automatically faster than Dijkstra or BFS. A poor heuristic can make it explore nearly as many states, while adding additional implementation complexity.

## **My final advice**

For your DSA preparation, I would consider these five problems the most valuable starting point:

1. *A Grid Pathfinding*\* — Understand the actual algorithm.
2. **LeetCode 773 — Sliding Puzzle** — Understand state-space search.
3. **8-Puzzle / 15-Puzzle** — Master heuristic design.
4. **POJ 2449 — K-th Shortest Path** — Master A\* combined with Dijkstra.
5. **LeetCode 1293 — Obstacles Elimination** — Practise augmented states and heuristic search.

These cover the most important dimensions of A\* without unnecessarily repeating conventional shortest-path problems.

The next logical step would be to build an *A Master Template in Latest Java*\*, covering generic graphs, grids, state-space search, path reconstruction, admissible heuristics, and the important implementation pitfalls.








































-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------
-----------------------------------------------------------------------------------------------------------------------


















# A* Algorithm: Master Problem List

**Reality check first.** Almost no interview problem *requires* A*. Nearly every problem below can be solved with BFS or Dijkstra, and A* is an optimization that adds a heuristic `h(n)` to prune the search. Interviewers rarely ask for A* by name. They ask problems where "shortest path in a state space" is the core, and A* is your "make it fast" follow-up. To master this pattern, master BFS → 0-1 BFS → Dijkstra → A* as one ladder.

## 0. The pattern in one block

```
f(n) = g(n) + h(n)
g = actual cost from start, h = estimated cost to goal
Priority queue ordered by f. Pop the lowest f. Stop when the goal is popped.
```

- **Admissible**: h never overestimates, so the result is optimal.
- **Consistent**: h(a) ≤ cost(a,b) + h(b). With it, a node is finalized on its first pop (no re-opening).
- h = 0 gives Dijkstra. h = 0 with unit costs gives BFS.

**Java skeleton**

```java
int aStar(int[] start, int[] goal, int[][] grid) {
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[2] - b[2]); // {r, c, f}
    int[][] g = new int[R][C];
    for (int[] row : g) Arrays.fill(row, Integer.MAX_VALUE);
    g[start[0]][start[1]] = 0;
    pq.offer(new int[]{start[0], start[1], h(start, goal)});
    while (!pq.isEmpty()) {
        int[] cur = pq.poll();
        int r = cur[0], c = cur[1];
        if (r == goal[0] && c == goal[1]) return g[r][c];
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (!valid(nr, nc)) continue;
            int ng = g[r][c] + 1;
            if (ng < g[nr][nc]) {
                g[nr][nc] = ng;
                pq.offer(new int[]{nr, nc, ng + h(new int[]{nr, nc}, goal)});
            }
        }
    }
    return -1;
}
```

**Heuristic cheat sheet**

| Movement | Heuristic |
|---|---|
| 4-directional grid | Manhattan: `|dx|+|dy|` |
| 8-directional grid, diagonal cost 1 | Chebyshev: `max(|dx|,|dy|)` |
| 8-directional, diagonal √2 | Octile: `(dx+dy) + (√2-2)·min(dx,dy)` |
| Knight moves | `max(⌈dx/2⌉, ⌈dy/2⌉, ⌈(dx+dy)/3⌉)` |
| Sliding puzzle | Sum of Manhattan distances of tiles |
| String transformation | Number of mismatched positions |
| Multi-target (keys, boxes) | Max or MST over remaining targets |

## 1. Tier A: A* is the natural fit (a good heuristic exists and pays off)

| # | Problem | Difficulty | Link | Heuristic / why A* |
|---|---|---|---|---|
| 1 | Shortest Path in Binary Matrix | Medium | https://leetcode.com/problems/shortest-path-in-binary-matrix/ | Chebyshev (8-dir). The cleanest first A* problem |
| 2 | Sliding Puzzle | Hard | https://leetcode.com/problems/sliding-puzzle/ | Manhattan tile sum. Classic 8-puzzle |
| 3 | Minimum Knight Moves | Medium | https://leetcode.com/problems/minimum-knight-moves/ (premium) | Knight formula above. Infinite board makes the heuristic essential |
| 4 | Minimum Moves to Move a Box to Target | Hard | https://leetcode.com/problems/minimum-moves-to-move-a-box-to-their-target-location/ | Nested search: box state (box, player) with Manhattan on the box. A famous A* example |
| 5 | Cut Off Trees for Golf Event | Hard | https://leetcode.com/problems/cut-off-trees-for-golf-event/ | Repeated point-to-point shortest paths with Manhattan |
| 6 | Shortest Path to Get All Keys | Hard | https://leetcode.com/problems/shortest-path-to-get-all-keys/ | State = (r, c, keyMask). Heuristic = remaining keys |
| 7 | The Maze III | Hard | https://leetcode.com/problems/the-maze-iii/ (premium) | Rolling-ball Dijkstra/A* with distance and lexicographic tie-break |
| 8 | K-Similar Strings | Hard | https://leetcode.com/problems/k-similar-strings/ | The classic "A* on strings" problem. Heuristic = (mismatches+1)/2 |
| 9 | Race Car | Hard | https://leetcode.com/problems/race-car/ | State = (pos, speed). Search-based, with a DP alternative |
| 10 | Word Ladder II | Hard | https://leetcode.com/problems/word-ladder-ii/ | BFS plus backtracking is standard. A* or bidirectional is the optimization |

## 2. Tier B: Solvable with A*, but BFS/Dijkstra is the expected answer

| # | Problem | Difficulty | Link | Note |
|---|---|---|---|---|
| 11 | Word Ladder | Hard | https://leetcode.com/problems/word-ladder/ | Bidirectional BFS is the best-known optimization |
| 12 | Open the Lock | Medium | https://leetcode.com/problems/open-the-lock/ | Wheel-distance heuristic |
| 13 | Nearest Exit from Entrance in Maze | Medium | https://leetcode.com/problems/nearest-exit-from-entrance-in-maze/ | Multiple goals, so the heuristic is weak |
| 14 | Shortest Path in a Grid with Obstacles Elimination | Hard | https://leetcode.com/problems/shortest-path-in-a-grid-with-obstacles-elimination/ | State (r, c, k). Manhattan is admissible |
| 15 | Shortest Path Visiting All Nodes | Hard | https://leetcode.com/problems/shortest-path-visiting-all-nodes/ | Bitmask BFS. A* with remaining-node heuristic |
| 16 | The Maze II | Medium | https://leetcode.com/problems/the-maze-ii/ (premium) | Dijkstra with a Manhattan heuristic |
| 17 | The Maze | Medium | https://leetcode.com/problems/the-maze/ (premium) | Reachability |
| 18 | Shortest Bridge | Medium | https://leetcode.com/problems/shortest-bridge/ | Multi-source BFS |
| 19 | Shortest Distance from All Buildings | Hard | https://leetcode.com/problems/shortest-distance-from-all-buildings/ (premium) | BFS from each building |

## 3. Tier C: Dijkstra-family (the foundation A* is built on)

You must be fluent here before A* feels natural.

| # | Problem | Difficulty | Link |
|---|---|---|---|
| 20 | Network Delay Time | Medium | https://leetcode.com/problems/network-delay-time/ |
| 21 | Path With Minimum Effort | Medium | https://leetcode.com/problems/path-with-minimum-effort/ |
| 22 | Swim in Rising Water | Hard | https://leetcode.com/problems/swim-in-rising-water/ |
| 23 | Cheapest Flights Within K Stops | Medium | https://leetcode.com/problems/cheapest-flights-within-k-stops/ |
| 24 | Path with Maximum Probability | Medium | https://leetcode.com/problems/path-with-maximum-probability/ |
| 25 | Minimum Obstacle Removal to Reach Corner | Hard | https://leetcode.com/problems/minimum-obstacle-removal-to-reach-corner/ (0-1 BFS) |
| 26 | Minimum Cost to Make at Least One Valid Path in a Grid | Hard | https://leetcode.com/problems/minimum-cost-to-make-at-least-one-valid-path-in-a-grid/ (0-1 BFS) |
| 27 | Number of Ways to Arrive at Destination | Medium | https://leetcode.com/problems/number-of-ways-to-arrive-at-destination/ |
| 28 | Second Minimum Time to Reach Destination | Hard | https://leetcode.com/problems/second-minimum-time-to-reach-destination/ |
| 29 | Minimum Time to Visit a Cell in a Grid | Hard | https://leetcode.com/problems/minimum-time-to-visit-a-cell-in-a-grid/ |
| 30 | Minimum Cost to Reach Destination in Time | Hard | https://leetcode.com/problems/minimum-cost-to-reach-destination-in-time/ |
| 31 | Reachable Nodes In Subdivided Graph | Hard | https://leetcode.com/problems/reachable-nodes-in-subdivided-graph/ |
| 32 | Design Graph With Shortest Path Calculator | Hard | https://leetcode.com/problems/design-graph-with-shortest-path-calculator/ |

## 4. Important problems outside LeetCode (recommended)

Yes, I recommend these. They teach what LeetCode barely covers:

1. **8-Puzzle / 15-Puzzle**: the canonical A* problem. Implement it with Manhattan, then Linear Conflict, and compare nodes expanded. Reference: https://en.wikipedia.org/wiki/15_puzzle
2. **Grid pathfinding with obstacles (Amit Patel / Red Blob Games)**: the best visual tutorial: https://www.redblobgames.com/pathfinding/a-star/introduction.html
3. **Rubik's Cube / Pancake Sorting (IDA\*)**: learn IDA\*, A*'s memory-efficient variant.
4. **Bidirectional A\***: extends bidirectional BFS from Word Ladder.
5. **Jump Point Search (JPS)**: grid-specific A* acceleration, a strong bonus topic.

## 5. Study path (4 stages)

1. **Week 1: Foundations.** Solve #20, 21, 22, 25 with Dijkstra and 0-1 BFS. Prove to yourself why Dijkstra is A* with h=0.
2. **Week 2: First A*.** Solve #1, 3, 12. Write each twice (BFS, then A*) and count nodes expanded.
3. **Week 3: State-space A*.** Solve #2, 6, 8, 14. Practice designing states: `(position, mask)`, `(position, k)`, board string.
4. **Week 4: Hard.** Solve #4, 5, 7, 9, 10, plus implement the 15-puzzle from scratch.

## 6. What interviewers actually test

- Can you **identify the state** (position alone is often not enough: add keys, remaining removals, speed)?
- Can you **justify admissibility** of your heuristic in one sentence?
- Do you know **when A\* is worse** (weak heuristic, many goals, negative edges)?
- Can you **switch** BFS → Dijkstra → A* when the problem changes (weights, scale)?

If you master Tier A and Tier C, plus the state-modelling skill, you can handle essentially any shortest-path round. I can write full Java solutions for any of these. Which tier would you like to start with?
