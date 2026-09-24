# What Did We Learn?

## What have we accomplished?

Same lesson as the Netflix chapter: every Facebook feature we built maps directly onto a well-known interview question.

| Facebook Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Friend Circles | Connected components via DFS on an adjacency matrix | Number of Provinces |
| #2 Copy Connections | DFS + HashMap to handle cycles | Clone Graph |
| #3 Find Story ID | Modified binary search (one half always sorted) | Search in Rotated Sorted Array |
| #4 Request Limiter | HashMap of last-seen timestamps | Logger Rate Limiter |
| #5 Flag Words | Two-pointer run-length comparison | Expressive Words |
| #6 Combine Similar Messages | Group by a signature (consecutive-character deltas) | Group Shifted Strings |
| #7 Divide Posts | Binary search on the answer + greedy feasibility check | Divide Chocolate |
| #8 Overlapping Topics | Sliding window with a need/formed counter | Minimum Window Substring |
| #9 Recreating the Decision Tree | Recursive split using a preorder root + inorder position | Construct Binary Tree from Preorder and Inorder Traversal |

Two new tools joined the toolbox this chapter: **graph traversal** (DFS to find connected components, and DFS-with-memoization to clone a graph without getting stuck in cycles), and **binary-search-on-the-answer**, where instead of searching an array directly, you search the space of *possible answers* and use a greedy check to test each candidate.

The DIY problems ahead are these same nine patterns, without the Facebook framing.
