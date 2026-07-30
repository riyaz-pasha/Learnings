# What Did We Learn?

## What have we accomplished?

The problems we solved to build the Stock Scraper's features are also some of the most frequently asked interview questions at top-tier companies. Once you recognize the pattern underneath, you can solve the "real" interview version the same way.

| Stock Scraper Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Traversing DOM Tree | BFS, level by level, with a queue | Binary Tree Level Order Traversal |
| #2 Locating Stock Data | Parent map + ancestor set | Lowest Common Ancestor of a Binary Tree / Lowest Common Ancestor of a Binary Tree III |
| #3 Traversing DOM Tree II | Level-linked next pointers, no queue | Populating Next Right Pointers in Each Node / Populating Next Right Pointers in Each Node II |
| #4 Maximum Profit | Kadane's algorithm | Maximum Subarray / Best Time to Buy and Sell Stock |

This chapter split cleanly into two halves: **tree traversal** (levels, ancestors, and next-pointers over an arbitrarily shaped n-ary or binary tree) and **array scanning** (Kadane's running-max technique, which shows up again and again in "best contiguous run" problems).

The DIY problems ahead restate these same four patterns without the DOM-scraping framing — some patterns get two DIY variants, since each one has a well-known "harder" sibling asked just as often.
