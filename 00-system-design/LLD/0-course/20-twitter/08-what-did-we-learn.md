# What Did We Learn?

## What have we accomplished?

The problems that we solved to implement our Twitter features are also asked in the most popular interview questions at top-tier companies. You can now identify problems with the same patterns and can solve them using the techniques you learned here.

Below is a table of Twitter features and the corresponding interview questions:

| Feature | Real interview pattern |
|---|---|
| Feature #1: Add Likes | Add Binary / Add Strings — digit-by-digit arithmetic on numbers kept as strings, carrying between columns |
| Feature #2: Merge Tweets In Twitter Feed | Merge Sorted Array — merging two sorted arrays in place, from the back, using the reserved space at the end |
| Feature #3: Identify Peak Interaction Times | Maximum Sum of Three Non-Overlapping Subarrays — sliding-window sums + prefix/suffix "best index so far" tables |
| Feature #4: Split Users into Two Groups | Is Graph Bipartite? — greedy 2-coloring via DFS, checking every edge crosses colors |
| Feature #5: Drawing a Global Profile of Viral Tweets | The Skyline Problem — divide and conquer, merging two skylines the way merge sort merges two sorted halves |
| Feature #6: Incoming Tweets Predictor | Moving Average from a Data Stream — sliding window with a running sum, held in a deque |
| Feature #7: Trending Hashtags | Find Duplicate Files in System — bucket items by a derived key in a hash map, report buckets with two or more members |

Recognizing these patterns is the real skill: once you see "numbers you can't convert to a numeric type" you should think digit-by-digit column arithmetic, once you see "merge two sorted things using pre-reserved space" you should think fill-from-the-back, once you see "can this graph be 2-colored" you should think bipartite-check-via-DFS, once you see "union of overlapping intervals with a height" you should think skyline via divide and conquer, and so on. The Twitter scenario is just one skin on top of these recurring shapes.
