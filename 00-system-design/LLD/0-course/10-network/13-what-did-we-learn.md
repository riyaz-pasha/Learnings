# What Did We Learn?

## What have we accomplished?

Every feature we built for our network devices turned out to be a well-known interview question wearing a costume. That's the whole point of this course: once you can see through the story to the underlying pattern, the "made up" business problem and the classic interview question become the same problem.

Here's the map from what we built to the pattern it was really testing:

| Network Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Total Time | BFS over a tree built from parent pointers, tracking maximum arrival time | Time Needed to Inform All Employees |
| #2 TTL Expiry | DFS to build an undirected adjacency list, then layered BFS from an arbitrary node | All Nodes Distance K in Binary Tree |
| #3 Minimum Hops | Greedy interval covering — always extend to the furthest reachable index | Jump Game II |
| #4 Maximum Routers | DFS with memoization on a grid, following strictly increasing values | Longest Increasing Path in a Matrix |
| #5 Update VLAN ID | DFS flood-fill on a grid, following matching values | Flood Fill |
| #6 Transmission Error | Two-pointer palindrome check with one allowed mismatch | Valid Palindrome II |
| #7 Divide Files Over the Network | Greedy interval merging using last-occurrence indices | Partition Labels |
| #8 Maximum Clock Skew | Top-down DFS tracking running max/min along a root-to-node path | Maximum Difference Between Node and Ancestor |
| #9 Update Configuration | Multi-source BFS, level by level, on a grid | Rotting Oranges |
| #10 Minimum Variation | Sliding window with a pair of monotonic deques for O(1) max/min | Longest Subarray With Absolute Diff Less Than or Equal to Limit |
| #11 Weighted Exponential Back-off | Digit-by-digit linked-list addition with carry, least-significant digit first | Add Two Numbers |
| #11 Weighted Exponential Back-off (reversed digit order) | Same addition, but most-significant digit first, so digits are staged on stacks before combining | Add Two Numbers II |
| #12 Peak Signal Strength | Binary search on an array that isn't sorted, but is guaranteed to alternate slopes | Find Peak Element |

Notice how few *actual* techniques are doing all the work here: BFS and DFS cover half the list between them (a tree BFS for propagation time, a graph BFS for distance-K, grid DFS with memoization, grid flood-fill DFS, another tree DFS for ancestor tracking, and multi-source grid BFS for configuration spread). The rest are a two-pointer palindrome check, greedy interval covering (twice, in two different flavors), a monotonic-deque sliding window, linked-list digit addition (in both digit orders), and a binary search that doesn't need a sorted array — just a guaranteed slope. These handful of ideas cover a huge fraction of "hard-sounding" interview questions — once you can name the pattern, the rest is implementation detail.

The DIY problems that follow are the same patterns again, stripped of the networking story — solve them to prove the pattern really transferred, not just the specific solution.
