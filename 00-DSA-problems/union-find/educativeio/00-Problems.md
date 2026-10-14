# Union-Find (DSU): Correct Problem Classification and Solving Order

Your goal should be to learn when Union-Find is the natural solution, not to force every graph or connectivity problem into DSU.

Union-Find, also called Disjoint Set Union (DSU), is especially useful when a problem repeatedly asks you to:

- Determine whether two nodes belong to the same connected component.
- Merge two components when a connection is added.
- Detect whether an edge creates a cycle.
- Count connected components efficiently.
- Process connectivity as edges are added or removed under suitable offline techniques.

For your list, I recommend separating the problems into three groups:

1. Core Union-Find problems: learn and master the pattern.
2. Hybrid problems: DSU is useful, but another technique is also essential.
3. Not primarily Union-Find: solve using a more appropriate algorithm first.

## 1. Problems to solve using Union-Find — recommended order

These are the problems I would prioritize for learning DSU. The order progresses from basic connectivity to component reasoning, graph modeling, and advanced applications.

10 problems · ordered by learning value, not difficulty rating alone

| Order | Problem                                     | Difficulty | DSU fit   | What you learn                                 |
| ----- | ------------------------------------------- | ---------- | --------- | ---------------------------------------------- |
| 1     | Find if Path Exists in Graph                | Easy       | Excellent | Basic connectivity                             |
| 2     | Redundant Connection                        | Medium     | Excellent | Detect cycles using failed unions              |
| 3     | Accounts Merge                              | Medium     | Excellent | Merge entities through shared identifiers      |
| 4     | Most Stones Removed with Same Row or Column | Medium     | Excellent | Model relationships as components              |
| 5     | Regions Cut by Slashes                      | Medium     | Excellent | Transform a grid into a connectivity graph     |
| 6     | Number of Islands II                        | Hard       | Excellent | Incremental connectivity and component counts  |
| 7     | Similar String Groups                       | Hard       | Very good | Build components from pairwise similarity      |
| 8     | Minimize Malware Spread                     | Hard       | Very good | Reason about component sizes and ownership     |
| 9     | Optimize Water Distribution in a Village    | Hard       | Excellent | Kruskal's algorithm and minimum spanning trees |
| 10    | Last Day Where You Can Still Cross          | Hard       | Very good | Reverse-time DSU and boundary connectivity     |

Why this order? It introduces the fundamental operations first, then moves into less obvious graph modeling and finally advanced applications.

### Phase 1 — Learn the DSU fundamentals

[CMSC 341 Graphs 552006 Basic Graph Definitions A](https://images.openai.com/static-rsc-4/od05pUffz2_wBKAa-3uYF1anBwhaPhh1Et3XYCujgtZck6JE6mEIr4oNvpSr_5ICU6h39PS6-Zj7Jyxb6pLqaVpvHNlfbWGNkyeaVSPgy7cxZGMS745JUKL9EUFXTf4gLjImJd-mXalY4QPqV9mbkSTMwIF5vmZsK7GwWqRpSw4?purpose=inline)

1\. Find if Path Exists in Graph

Start here

Learn `find`, `union`, path compression, and union by size/rank. Every edge joins two components; check whether the source and destination end up in the same component.

[Redundant Connection - LeetCode](https://images.openai.com/static-rsc-4/7e3qy6RQ7UWbVEfMPdNgmzQHq1JCIZ_n-mu82ec5A4MhdDZeygiFdxxJ6lQHoGJkCNndQSiwAnQES5ZmbDtRyqFurpqG4rd65USz6sit7A_EIvlHVd1L8OOgd6NsVL3VL6Y4RYfU5N3E_G-rszlE-dqyT2ItEwYVrTAkAWatxfw?purpose=inline)

2\. Redundant Connection

An edge is redundant when its endpoints are already connected. This is the classic application of DSU for cycle detection in an undirected graph.

### Phase 2 — Learn how to model components

[How to Merge Gmail Accounts Without Losing Emails (2026)](https://images.openai.com/static-rsc-4/Go1DYQfJVAKYicluVTkd3esVtoun5S2AM0cCY69iApg30QqlFg_mxQ2Jfa_912vGf_bPUbJX2oBMAjpzoa0ntSbJ1kFCrzVEO2G4Ltr5L8Q9oBusz_PV3PNHbCJQq2vb3d82vPnCGnPmqJjUrJOAfdM_g-1yCToXH79_QSpz1K8?purpose=inline)

3\. Accounts Merge

Different accounts belong together when they share an email. Model emails or accounts as nodes, merge related nodes, and group by their final representative.

Key lesson: DSU can merge entities, not just ordinary graph vertices.

[Most Stones Removed with Same Row or Column - DSU: G-53 - Tutorial](https://images.openai.com/static-rsc-4/6OATDV8Zdwia1qe6fiRsNwXLyxvQJBFzoGeuojU8MNAi5oXMpmBdXq5Zk_CUoE7KsLrVkSBIvKWfBZ0SzoqpGO1PaZrix2FmAvG45WLkHCO0ksbZ3hsFhbrXvatbDQfEQmuU-t3BOICrRuHc1Sx79aaCu9pMNISgmbU1ox3-ges?purpose=inline)

4\. Most Stones Removed with Same Row or Column

Stones connected through a common row or column form components. The key result is that the maximum number removable is \\(n-\text{components}\\).

Learn to connect stones directly or model rows and columns as separate node types.

[Jae Moon | \[LC\] 959. Regions Cut By Slashes](https://images.openai.com/static-rsc-4/pStN0ja2OBl54nmASBAGvLUY5ZzfKeKrUcVRE9SgtCU6w0z3nPu0g250ZffRCE-IGwic1oICAdK7a2auCsfOvJ_exck1IK9seuqcIaJjtijOBPgiMd19pE7yrEhTW3DYjqSkyzTD-u6y2LKkPwigFJ6I5nKh-OLu7cjM2TH5Fs8?purpose=inline)

5\. Regions Cut by Slashes

A slash divides a cell into smaller regions. Represent each cell as four triangles and union the triangles that touch.

Learn advanced graph modeling: sometimes you must redesign the nodes before DSU becomes applicable.

### Phase 3 — Dynamic and non-obvious connectivity

[Program to find island count by adding blocks into grid one by one in Python](https://images.openai.com/static-rsc-4/RytHxJ1pTpJVC3po8WePzDAsQ8zi6gw720AUqoAj5BQER9Anjxbu6l3q3zfbJtBC3f_AhxcCCHG49OJIlE2IyinOMETn33WJr3fZfRzo9rGf39X4293y7cEJ27n6r1LDsEn-VpnbW98VVrYf4SYjInrL0TWI6r9mZoJHCeXNfC0?purpose=inline)

6\. Number of Islands II

Land cells are activated one at a time. Each new cell creates an island, then may merge with up to four neighboring islands.

This is one of the strongest DSU problems because repeated DFS/BFS would be expensive when the grid is large and updates are numerous.

[Similar String Groups. Two strings X and Y are similar if we… | by Omar Faroque | Algorithm and DataStructure | Medium](https://images.openai.com/static-rsc-4/5va8RwiWxKhLwwOSJdrhYjdFpxbfSS9N4ZCQXWM857S-SQIdg2JMACpSMXjL6H8R4k9AbBrbBCYrqehk2lZvBS55dOmBMugXuNvaSPH3S1GQWkb-VbYJVFuu-efWWDbahnGMcyvFO1kzBA8ONOHs65MzTcSfmdh4zTnmhEhUm6s?purpose=inline)

7\. Similar String Groups

Compare strings, union pairs that satisfy the similarity rule, and count components.

Important: DSU handles the grouping, but checking similarity between strings is a separate operation and may dominate the complexity.

[Minimizing outbreak through targeted blocking for disease control: a community-based approach using super-spreader node identification | Scientific Reports](https://images.openai.com/static-rsc-4/7SWFLbW9ug5h56AuOyCQFSABKMn9lKnJx2nVOenJZ7QnNpfEWhzjZ3nLAKNPznANYmWLhaVrtKa2FwuUEO9NSCY__wvb9iGdIy9smBgB3hpSYB9G-l1NFFeaBhdDc-YloCYEKHrTTagrji0mmQ-0FnEwQ72h_u9ydVswyTz1frg?purpose=inline)

8\. Minimize Malware Spread

Use connected components to reason about which infected nodes can contaminate which parts of the graph. Component sizes and the number of initially infected nodes in each component determine the best removal.

Learn that knowing the components is only the first part; the optimization logic is equally important.

### Phase 4 — Advanced DSU applications

[Kruskal's Algorithm and Minimum Spanning Tree](https://images.openai.com/static-rsc-4/6u5506uV5GfcBXL65WL86E0CZeit2ohRF8olOx0azWUL0JxpDb-c8gt2HyOU2jY-ZAX1ubovxW7WYr96zzNBSb8_CUEaFGnMJNW1Lz8iOc4NOGIwmomTCr5GprPyJwcJ_yBGZ7aKfRMdTnfWgxePAaMaU-pNCh5fwlLmLHSynw4?purpose=inline)

9\. Optimize Water Distribution in a Village

This is a minimum spanning tree problem. Add a virtual node representing a water well and connect it to each house with the well cost. Then use Kruskal's algorithm, sorting all edges and accepting only successful unions.

Learn why DSU is a central part of Kruskal's algorithm.

[LeetCode — 1970. Last Day Where You Can Still Cross | by c0D3M | Medium](https://images.openai.com/static-rsc-4/1t5jFokxd1Vi04RpepJTtTsCxVeEFp2UeiXehJdsr_TlHBSMZ4Q7W4hdkTve6Yma4Vqscuf8TIxGa_Y5v1vpvtB2L2s-RNMfAgrLrVbndW4AYSRhWZkk-Z9Kc9_dElJ8MZVx1Z0b8SelgYjhNqI8vcFxCIEdi-zCiYoWxUJUeQ4?purpose=inline)

10\. Last Day Where You Can Still Cross

Process flooding in reverse: restore cells to land and union neighboring land cells. Once the top and bottom boundaries become connected, you can determine the critical crossing day.

Learn reverse-time processing, virtual boundary nodes, and how to convert a difficult forward-time problem into incremental connectivity.

## 2. Problems where another algorithm is preferable

These should not be forced into your primary Union-Find practice list.

| Problem                      | Better primary approach                                | Why DSU is not the first choice                                                                                                                                                         |
| ---------------------------- | ------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Number of Islands            | DFS or BFS                                             | A static grid can be traversed once in \\(O(mn)\\). DSU works, but usually adds unnecessary machinery.                                                                                  |
| Longest Consecutive Sequence | HashSet                                                | The key insight is finding sequence starts and extending consecutive values in expected \\(O(n)\\), without building components.                                                        |
| Evaluate Division            | Graph DFS/BFS; weighted DSU as an advanced alternative | The problem asks for multiplicative ratios along paths. Ordinary DSU does not retain those ratios; weighted DSU can, but graph traversal is simpler for the usual static-query version. |
| The Skyline Problem          | Sweep line + max-heap or ordered multiset              | It asks for the maximum active building height as positions change, not connected components. DSU is not a suitable primary pattern.                                                    |

### Important distinctions

Number of Islands: Keep it in your general graph/grid curriculum, but solve DFS/BFS first. If your goal is specifically to practice DSU, you can implement a second version afterward.

Evaluate Division: Do not confuse weighted DSU with ordinary DSU. A weighted DSU stores a relationship between a node and its parent, such as a ratio. It is useful when queries and equations evolve in ways that make repeated graph traversal less convenient.

The Skyline Problem: Exclude it from your Union-Find list altogether. Learn it under sweep line and active-interval data structures.

## 3. Final separation of all 14 problems

Use this as your checklist.

### A. Core Union-Find practice

Find if Path Exists in Graph

Redundant Connection

Accounts Merge

Most Stones Removed with Same Row or Column

Regions Cut by Slashes

Number of Islands II

Similar String Groups

Minimize Malware Spread

Optimize Water Distribution in a Village

Last Day Where You Can Still Cross

### B. Better solved with another primary pattern

Number of Islands

Longest Consecutive Sequence

Evaluate Division

The Skyline Problem

0 of 14 reviewed

Reset

## 4. What you should master before moving on

For each of the ten DSU problems, be able to answer these questions:

1. What does a node represent? A graph vertex, an account, a stone, a triangle, a cell, or a virtual boundary?
2. When should two nodes be unioned? Identify the exact condition that establishes connectivity.
3. What does `find(x) == find(y)` mean? Both elements belong to the same component.
4. What information must be maintained? Component count, component size, number of infected nodes, or weighted relationships?
5. Is DSU the best approach? Compare it with DFS/BFS, and account for whether the graph changes over time.

The standard DSU operations, with path compression and union by size/rank, have amortized time complexity \\(O(\alpha(n))\\) per operation, where \\(\alpha\\) is the inverse Ackermann function and is effectively constant for practical input sizes.

My recommendation: Start with Find if Path Exists in Graph and Redundant Connection, then move through component modeling, Number of Islands II, and finally Kruskal's algorithm and reverse-time connectivity. That sequence gives you the strongest foundation for recognizing DSU in interviews.

