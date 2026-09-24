# What Did We Learn?

## What have we accomplished?

Every feature we built for Zoom turned out to be a well-known interview question wearing a costume. That's the whole point of this course: once you can see through the story to the underlying pattern, the "made up" business problem and the classic interview question become the same problem.

Here's the map from what we built to the pattern it was really testing:

| Zoom Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Display Meeting Lobby | Iterative in-order BST traversal with a manual stack | Binary Search Tree Iterator |
| #2 Serialize/Deserialize Participant Data | Pre-order traversal + BST re-insertion | Serialize and Deserialize Binary Tree |
| #3 Meeting Activity | BFS on an implicit graph of array indices | Jump Game IV |
| #4 Validate Sorted Participants Data | Adjacent-pair order check on an in-order sequence | Validate the Binary Search Tree |
| #5 Auto Rotate in Mobile Devices | In-place ring-by-ring matrix rotation | Rotate Image |

Notice how few *actual* techniques are doing all the work here: a manual stack to pause and resume a traversal, a traversal order chosen specifically so it can be reversed without extra bookkeeping, BFS to find a shortest path when the "graph" isn't drawn anywhere but implied by the data, a single linear scan to check ordering, and layer-by-layer swaps to rotate a matrix without extra memory. These handful of ideas cover a huge fraction of "hard-sounding" interview questions — once you can name the pattern, the rest is implementation detail.

The DIY problems that follow are the same patterns again, stripped of the Zoom story — solve them to prove the pattern really transferred, not just the specific solution.
