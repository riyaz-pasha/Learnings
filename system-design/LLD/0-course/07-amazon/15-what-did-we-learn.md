# What Did We Learn?

## What have we accomplished?

Every feature we built for Amazon turned out to be a well-known interview question wearing a costume. That's the whole point of this course: once you can see through the story to the underlying pattern, the "made up" business problem and the classic interview question become the same problem.

Here's the map from what we built to the pattern it was really testing:

| Amazon Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Suggest Items for Free Delivery | Hash-map complement lookup for a pair summing to a target | Two Sum |
| #2 Suggest Items for Special Offer | Sorted two-pointer sweep for triplets matching/closest to a target | Three Sum / 3Sum Closest |
| #3 Upselling Products | Array + hash-map index for O(1) insert, delete, and random pick | Insert Delete GetRandom O(1) (Duplicates Allowed) |
| #4 Copy Product Data | Interleaved node cloning to copy a linked structure with cross-links | Copy List with Random Pointer |
| #5 Order Processing Milestones | Binary search for the first/last index of a target in sorted data | Find First and Last Position of Element in Sorted Array |
| #6 Products Frequently Viewed Together | Sliding window with a character/frequency map | Find All Anagrams in a String |
| #7 Optimize Delivery Cost | Prefix-sum remainders + pigeonhole principle to spot a subarray sum divisible by k | Continuous Subarray Sum |
| #8 Merge Recommendations | Union-Find (or graph traversal) to merge overlapping groups | Accounts Merge |
| #9 Products in Price Range | Pruned BST traversal that skips subtrees outside the range | Range Sum of BST |
| #10 Calculate the Total Cost of the Shopping Cart Items | Single-pass expression evaluation using operator precedence | Basic Calculator II / III |
| #11 Ad Serving | Persistent buffer state across repeated calls to a fixed-size reader | Read N Characters Given Read4 II |
| #12 Warehouse and Drop Points | Multi-source BFS expanding outward from all sources at once | Walls and Gates |
| #13 Time-Based Item Price Store | Sorted-by-construction data + binary search for the rightmost valid entry | Time-Based Key-Value Store |
| #14 Find Similar Products | Fixed-size counting array standing in for a hash set | Intersection of Two Arrays |

Notice how few *actual* techniques are doing all the work here: a handful of two-pointer and hashing tricks for "find a pair/triplet matching a target," binary search whenever the data is sorted (or can be kept sorted for free), prefix sums to turn "does some range satisfy X" into an `O(1)` lookup, BFS when a search needs to expand from many sources simultaneously, and a fixed-size array standing in for a hash set whenever the input space is bounded. These same handful of ideas cover a huge fraction of "hard-sounding" interview questions — once you can name the pattern, the rest is implementation detail.

The DIY problems that follow are the same patterns again, stripped of the Amazon story — solve them to prove the pattern really transferred, not just the specific solution.
