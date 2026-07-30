# What Did We Learn?

## What have we accomplished?

Every feature we built for our operating system turned out to be a well-known interview question wearing a costume. That's the whole point of this course: once you can see through the story to the underlying pattern, the "made up" business problem and the classic interview question become the same problem.

Here's the map from what we built to the pattern it was really testing:

| Operating System Feature | Underlying Pattern | Matching Interview Question(s) |
|---|---|---|
| #1 Allocate Space | Prefix sums with a hashmap counting occurrences | Subarray Sum Equals K |
| #2 Resume Process | Recursive binary search counting missing values per half | Missing Element in Sorted Array |
| #3 Schedule Processes | Kahn's algorithm - BFS topological sort using in-degrees | Course Schedule, Course Schedule II, Sequence Reconstruction |
| #4 Compress File | DFS over every prefix/suffix split, memoized | Concatenated Words |
| #5 Recover Files | Stack of (index, character) pairs to find unmatched brackets | Minimum Remove to Make Valid Parentheses |
| #6 File Management System | Trie with wildcard-aware recursive search | Design Add and Search Words Data Structure |
| #7 Serialize and Deserialize File System | BFS level-order traversal encoding values + child counts | Serialize and Deserialize N-ary Tree |
| #8 Compress File II | In-place run-length encoding on a character list | String Compression |
| #9 File Search | Bottom-up DP over two strings for regex/wildcard matching | Regular Expression Matching, Wildcard Matching |
| #10 Decode a Message | Backtracking over every operator/operand pair until one number remains | 24 Game |
| #11 Directory Iterator | Stack-based lazy flattening of a nested structure | Flatten Nested List Iterator |
| #12 Priority Validation | DP with a hashmap of reachable jump sizes per position | Frog Jump |
| #13 Reverse Commands | Trim, split on whitespace, reverse, join | Reverse Words in a String |
| #14 Releasing Process Lock | Binary search restricted to even indices | Single Element in a Sorted Array |
| #15 Queue Reconstruction by Priority | Sort by priority descending, then insert by position | Queue Reconstruction by Height |

Notice how few *actual* techniques are doing all the work here: BFS/topological sort shows up twice (process scheduling and N-ary tree serialization), binary search shows up twice in different disguises (missing-element counting and even-index-only search), and DP-with-a-hashmap covers both the physically-unrelated-looking Frog Jump and 24 Game problems. The rest are a stack for bracket/lock matching, a trie for prefix search, a two-string bottom-up DP for pattern matching, in-place run-length encoding, and lazy stack-based flattening. These handful of ideas cover a huge fraction of "hard-sounding" interview questions — once you can name the pattern, the rest is implementation detail.

The DIY problems that follow are the same patterns again, stripped of the operating-system story — solve them to prove the pattern really transferred, not just the specific solution.
