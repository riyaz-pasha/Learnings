# What Did We Learn?

## What have we accomplished?

The problems we solved to build our Boggle features are also asked, almost word for word, in some of the most popular coding interview questions at top-tier companies. Once you recognize the pattern, you can spot it under any story.

Here's the map from what we built to the pattern it was really testing:

| Boggle Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Single word search | Backtracking DFS over a grid, marking/unmarking cells as visited | Word Search |
| #2 Maximum words search | Trie-pruned multi-word DFS over a grid | Word Search II |

Both features boil down to the same core idea: a **grid DFS with backtracking**, where a cell is "locked" only for the duration of the path currently exploring it. Feature #1 asks "does one specific word exist?" — a single DFS sweep is enough. Feature #2 asks "which of *many* words exist?" — and once there are many words to check at once, sharing the exploration through a Trie turns "search once per word" into "search once, prune shared prefixes early." That shift — from repeating a search per query to batching queries through a shared prefix structure — is a common upgrade path in interview problems once a single-item version is mastered.

The DIY problems that follow are the same two patterns again, stripped of the Boggle story — solve them to prove the pattern really transferred, not just the specific solution.
