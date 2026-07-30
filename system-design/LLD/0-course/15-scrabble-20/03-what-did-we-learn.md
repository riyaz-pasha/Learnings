# What Did We Learn?

## What have we accomplished?

The problems we solved to implement our Scrabble 2.0 features are also asked in some of the most popular interview questions at top-tier companies. You can now identify problems with the same underlying pattern and solve them using the techniques you learned here.

Here's the map from what we built to the pattern it was really testing:

| Scrabble Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Minimum Moves | BFS shortest path over an implicit graph, using wildcard states to find one-letter-different neighbors without building an explicit adjacency list | Word Ladder I |
| #2 Possible Results | BFS layer-by-layer (to fix the shortest distance) followed by backward DFS over a recorded parent map (to enumerate every path achieving it) | Word Ladder II |

Both features boil down to the same core idea: model a word group as a graph where an edge connects any two words differing by exactly one letter, then run **BFS** to exploit its guarantee of finding shortest paths in an unweighted graph first. Feature #1 asks "how many moves, at minimum?" — a single BFS sweep that stops at the first sighting of the target answers that directly. Feature #2 asks "show me *every* sequence that achieves that minimum" — and once the question changes from a single number to "all shortest paths," pure BFS isn't enough on its own since it doesn't remember paths. The fix is to let BFS fix the *layers* (so we know exactly how far each word truly is), then use DFS purely as a bookkeeping tool to walk backward through a parent map built during those layers. That combination — BFS to bound the search, DFS to reconstruct — is a common upgrade path whenever an interview problem moves from "shortest distance" to "all shortest paths."

The DIY problems that follow are the same two patterns again, stripped of the Scrabble story — solve them to prove the pattern really transferred, not just the specific solution.
