# What Did We Learn?

## What have we accomplished?

The problems we solved to build these Computational Biology features are, underneath the DNA-and-protein framing, some of the most popular interview questions asked at top-tier companies. Now that we've built them once with a real-world story attached, we can recognize the same patterns anywhere they show up — stripped of the biology, or dressed up in some other domain entirely.

Here's the map from each feature to the interview pattern it's really testing:

| Feature | Real interview question | Core pattern |
|---|---|---|
| #1: Mutate DNA | String Transforms into Another String | Graph modeling of character mappings + cycle detection |
| #2: Detect Virus | Longest Substring with At Most K Distinct Characters | Sliding window with a hash map |
| #3: Locate Protein | Longest Palindromic Substring | Expand around center |
| #4: Identifying Proteins | Valid Palindrome | Two-pointer / recursive palindrome check |
| #5: Mutating a Virus | Next Permutation | In-place array rearrangement |
| #6: Identify a Species | Longest Substring without Repeating Characters | Sliding window with a hash map |
| #7: Detecting a Protein | Palindrome Permutation | Character-frequency parity check |
| #8: Similarity Measure Between DNA Samples | Edit Distance | 2D dynamic programming |
| #9: Kth Missing Gene | Kth Missing Positive Number | Binary search on an implicit "missing count" function |

Two techniques repeat across this chapter — the sliding window (Features #2 and #6) and expand-around-center for palindromes (Feature #3, and again in the DIY-only "Palindromic Substrings" problem) — which is a strong sign these are patterns worth having in muscle memory going into an interview.
