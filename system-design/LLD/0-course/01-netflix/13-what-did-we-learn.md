# What Did We Learn?

## What have we accomplished?

Every feature we built for Netflix turned out to be a well-known interview question wearing a costume. That's the whole point of this course: once you can see through the story to the underlying pattern, the "made up" business problem and the classic interview question become the same problem.

Here's the map from what we built to the pattern it was really testing:

| Netflix Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Group Similar Titles | Group by a canonical signature | Group Anagrams |
| #2 Fetch Top Movies | Merge k sorted lists | Merge K Sorted Lists |
| #3 Find Median Age | Two heaps (max + min) | Find Median from a Data Stream |
| #4 Popularity Analysis | Single-pass monotonicity check | Monotonic Array |
| #5 Fetch Most Recently Watched Titles | HashMap + doubly linked list | LRU Cache |
| #6 Fetch Most Frequently Watched Titles | HashMap + frequency-bucketed lists | LFU Cache |
| #7 Browse Ratings | Two stacks in lockstep | Min Stack (built as a Max Stack here) |
| #8 Verify User Session | Greedy stack simulation | Validate Stack Sequences |
| #9 Movie Combinations of a Genre | Backtracking over independent choices | Letter Combinations of a Phone Number |
| #10 Calculate Median of Buffering Events | Two heaps + lazy deletion | Sliding Window Median |
| #11 Generate Movie Viewing Orders | Backtracking with in-place swaps | Permutations |
| #12 Maintain Continue Watching Bar | Frequency map + stack-per-bucket | Maximum Frequency Stack |

Notice how few *actual* techniques are doing all the work here: hashing to group things, two heaps to track a running median, a HashMap paired with a linked list to get O(1) eviction, and backtracking to explore all choices. These handful of ideas cover a huge fraction of "hard-sounding" interview questions — once you can name the pattern, the rest is implementation detail.

The DIY problems that follow are the same patterns again, stripped of the Netflix story — solve them to prove the pattern really transferred, not just the specific solution.
