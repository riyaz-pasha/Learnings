# What Did We Learn?

## What have we accomplished?

Same lesson again: every Search Engine feature maps to a well-known interview question.

| Search Engine Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Store and Fetch Words | Prefix tree | Implement Trie (Prefix Tree) |
| #2 Autocomplete System | Trie + DFS + ranked sort | Design Search Autocomplete System |
| #3 Add White Spaces to Create Words | Bottom-up DP over string positions | Word Break |
| #4 Suggest Possible Queries | Memoized recursion returning all splits | Word Break II |
| #5 Search Ranking Factor | Prefix/suffix product sweep | Product of Array Except Self |
| #6 Reorganizing Search Results | Greedy + max heap | Reorganize String |
| #7 Find Searching Time | Stack-based interval accounting | Exclusive Time of Functions |
| #8 Distributed Process Coordinator | Copy-on-write snapshots in a map | Snapshot Array |
| #9 Finding Minimum Servers | Memoized recursion, unbounded supply | Coin Change |

The trie is the star of this chapter — it shows up twice, once for plain word lookup and once bundled with ranking for autocomplete. The other recurring idea is **memoized recursion over string/array positions**: Word Break, Word Break II, and Coin Change all reduce to "solve every sub-position once, cache it, combine."

The DIY problems ahead are these same nine patterns, minus the search-engine framing.
