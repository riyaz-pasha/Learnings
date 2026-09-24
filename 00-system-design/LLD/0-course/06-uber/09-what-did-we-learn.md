# What Did We Learn?

## What have we accomplished?

Every feature we built for Uber turned out to be a well-known interview question wearing a costume. That's the whole point of this course: once you can see through the story to the underlying pattern, the "made up" business problem and the classic interview question become the same problem.

Here's the map from what we built to the pattern it was really testing:

| Uber Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Select Closest Drivers | Size-k max-heap over distances | K Closest Points to Origin |
| #2 Path Cost | Left-max / right-max prefix sweep | Trapping Rainwater |
| #3 Plot and Select Path | DFS over a weighted graph | Evaluate Division |
| #4 Fare in Words | Chunk into groups of 3 digits + lookup tables | Integer to English Words |
| #5 Uber Pool | Cumulative sums + binary search | Random Pick with Weight |
| #6 Longest Route | Recursive height + running max at each node | Diameter of Binary Tree |
| #7 Highest Rank | Size-k min-heap over values | Kth Largest Element in an Array |
| #8 Optimal Path | Bottom-up DP on a grid | Minimum Path Sum |

Notice how few *actual* techniques are doing all the work here: a size-k heap (max or min, depending on which end you want) to avoid sorting everything, prefix sweeps to avoid recomputation, DFS to explore a graph without knowing the destination's distance in advance, and grid/tree DP where each cell or node's answer builds directly on its neighbors'. These handful of ideas cover a huge fraction of "hard-sounding" interview questions — once you can name the pattern, the rest is implementation detail.

The DIY problems that follow are the same patterns again, stripped of the Uber story — solve them to prove the pattern really transferred, not just the specific solution.
