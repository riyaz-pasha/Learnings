# Course Conversion Progress

Source: `/Users/riyaz/Downloads/Decode the Coding Interview in Java: Real-World Examples`
Target: `system-design/LLD/0-course/`
Plan: `/Users/riyaz/.claude/plans/we-have-a-new-quiet-moth.md`

Convention: story-flow narrative + simple English (keep technical terms) + a real-world example + a Mermaid diagram where the original had a reconstructible diagram (else save image to chapter `assets/`) + complete, readable, idiomatic Java code (reconstruct anything the scrape truncated). Filenames keep the original numeric order as a prefix (e.g. `03-feature-2-...md`).

**Resume instructions:** find the first chapter below without a trailing ✅ and continue from there, one chapter at a time, in order. Do not skip ahead.

**Established workflow (chapters 05+):** delegate each chapter to one `Agent` (subagent_type: general-purpose), since a full chapter's extraction+writing+verification is too large to do lesson-by-lesson in the main conversation without exhausting context. Give the subagent: this file's path (to read its own chapter's pre-scaffolded lesson list/filenames — never recompute), the plan file path, the source HTML folder for that chapter, 2-3 already-completed chapter folders to copy style from, and explicit instructions to (1) reconstruct any Monaco-textarea-truncated code from the surrounding prose, (2) verify non-trivial algorithms by actually compiling/running Java (not just inspection) before writing an example's output into a lesson, (3) update PROGRESS.md by flipping only that chapter's own `- [ ]` lines to `- [x]` — never append/duplicate lines, always anchor Edit calls with enough surrounding context since titles like "What Did We Learn?" repeat verbatim across chapters, (4) do a whole-chapter review (scrape-artifact grep, broken-link check, one mermaid diagram per feature lesson) before adding the trailing ✅, and (5) sanity-check `grep -c '^- \[' PROGRESS.md` is unchanged before/after (389 total across the whole course — any change means a duplication bug slipped in and must be fixed before finishing). After each subagent reports back, spot-check its claims yourself (`ls` the folder, grep for artifacts, `Read` one output file) before trusting it and moving to the next chapter — subagents have self-reported issues in every chapter so far (real ones, plus occasionally overstated fixes), so verify rather than take the summary at face value.

## 00 Getting Started (2/2) ✅
Folder: `00-getting-started/`

- [x] 00 Course Overview — `00-course-overview.md`
- [x] 01 Who Should Take This Course? — `01-who-should-take-this-course.md`

## 01 Netflix (26/26) ✅
Folder: `01-netflix/`

- [x] 00 Project Description For Netflix — `00-project-description-for-netflix.md`
- [x] 01 Feature #1: Group Similar Titles — `01-feature-1-group-similar-titles.md`
- [x] 02 Feature #2: Fetch Top Movies — `02-feature-2-fetch-top-movies.md`
- [x] 03 Feature #3: Find Median Age — `03-feature-3-find-median-age.md`
- [x] 04 Feature #4: Popularity Analysis — `04-feature-4-popularity-analysis.md`
- [x] 05 Feature #5: Fetch Most Recently Watched Titles — `05-feature-5-fetch-most-recently-watched-titles.md`
- [x] 06 Feature #6: Fetch Most Frequently Watched Titles — `06-feature-6-fetch-most-frequently-watched-titles.md`
- [x] 07 Feature #7: Browse Ratings — `07-feature-7-browse-ratings.md`
- [x] 08 Feature #8: Verify User Session — `08-feature-8-verify-user-session.md`
- [x] 09 Feature #9: Movie Combinations of a Genre — `09-feature-9-movie-combinations-of-a-genre.md`
- [x] 10 Feature #10: Calculate Median of Buffering Events — `10-feature-10-calculate-median-of-buffering-events.md`
- [x] 11 Feature #11: Generate Movie Viewing Orders — `11-feature-11-generate-movie-viewing-orders.md`
- [x] 12 Feature #12: Maintain Continue Watching Bar — `12-feature-12-maintain-continue-watching-bar.md`
- [x] 13 What Did We Learn? — `13-what-did-we-learn.md`
- [x] 14 DIY: Group Anagrams — `14-diy-group-anagrams.md`
- [x] 15 DIY: Merge K Sorted Lists — `15-diy-merge-k-sorted-lists.md`
- [x] 16 DIY: Find Median from a Data Stream — `16-diy-find-median-from-a-data-stream.md`
- [x] 17 DIY: Monotonic Array — `17-diy-monotonic-array.md`
- [x] 18 DIY: LRU Cache — `18-diy-lru-cache.md`
- [x] 19 DIY: LFU Cache — `19-diy-lfu-cache.md`
- [x] 20 DIY: Min Stack — `20-diy-min-stack.md`
- [x] 21 DIY: Validate Stack Sequences — `21-diy-validate-stack-sequences.md`
- [x] 22 DIY: Letter Combinations of a Phone Number — `22-diy-letter-combinations-of-a-phone-number.md`
- [x] 23 DIY: Sliding Window Median — `23-diy-sliding-window-median.md`
- [x] 24 DIY: Permutations — `24-diy-permutations.md`
- [x] 25 DIY: Maximum Frequency Stack — `25-diy-maximum-frequency-stack.md`

## 02 Facebook (22/22) ✅
Folder: `02-facebook/`

- [x] 00 Project Description for Facebook — `00-project-description-for-facebook.md`
- [x] 01 Feature #1: Friend Circles — `01-feature-1-friend-circles.md`
- [x] 02 Feature #2: Copy Connections — `02-feature-2-copy-connections.md`
- [x] 03 Feature #3: Find Story ID — `03-feature-3-find-story-id.md`
- [x] 04 Feature #4: Request Limiter — `04-feature-4-request-limiter.md`
- [x] 05 Feature #5: Flag Words — `05-feature-5-flag-words.md`
- [x] 06 Feature #6: Combine Similar Messages — `06-feature-6-combine-similar-messages.md`
- [x] 07 Feature #7: Divide Posts — `07-feature-7-divide-posts.md`
- [x] 08 Feature #8: Overlapping Topics — `08-feature-8-overlapping-topics.md`
- [x] 09 Feature #9: Recreating the Decision Tree — `09-feature-9-recreating-the-decision-tree.md`
- [x] 10 What Did We Learn? — `10-what-did-we-learn.md`
- [x] 11 DIY: Number of Islands — `11-diy-number-of-islands.md`
- [x] 12 DIY: Number of Provinces — `12-diy-number-of-provinces.md`
- [x] 13 DIY: Number of Connected Components in an Undirected Graph — `13-diy-number-of-connected-components-in-an-undirected-graph.md`
- [x] 14 DIY: Clone Directed Graph — `14-diy-clone-directed-graph.md`
- [x] 15 DIY: Search in Rotated Sorted Array — `15-diy-search-in-rotated-sorted-array.md`
- [x] 16 DIY: Logger Rate Limiter — `16-diy-logger-rate-limiter.md`
- [x] 17 DIY: Expressive words — `17-diy-expressive-words.md`
- [x] 18 DIY: Group Shifted Strings — `18-diy-group-shifted-strings.md`
- [x] 19 DIY: Divide Chocolate — `19-diy-divide-chocolate.md`
- [x] 20 DIY: Minimum Window Substring — `20-diy-minimum-window-substring.md`
- [x] 21 DIY: Construct a Binary Tree from Preorder and Inorder Traversal — `21-diy-construct-a-binary-tree-from-preorder-and-inorder-traversal.md`

## 03 Search Engine (22/22) ✅
Folder: `03-search-engine/`

- [x] 00 Project Description for Search Engine — `00-project-description-for-search-engine.md`
- [x] 01 Feature #1: Store and Fetch Words — `01-feature-1-store-and-fetch-words.md`
- [x] 02 Feature #2: Design Search Autocomplete System — `02-feature-2-design-search-autocomplete-system.md`
- [x] 03 Feature #3: Add White Spaces to Create Words — `03-feature-3-add-white-spaces-to-create-words.md`
- [x] 04 Feature #4: Suggest Possible Queries After Adding White Spaces — `04-feature-4-suggest-possible-queries-after-adding-white-spaces.md`
- [x] 05 Feature #5: Calculate the Search Ranking Factor — `05-feature-5-calculate-the-search-ranking-factor.md`
- [x] 06 Feature #6: Reorganizing Search Results — `06-feature-6-reorganizing-search-results.md`
- [x] 07 Feature #7: Find Searching Time — `07-feature-7-find-searching-time.md`
- [x] 08 Feature #8: Distributed Process Coordinator — `08-feature-8-distributed-process-coordinator.md`
- [x] 09 Feature #9: Finding Minimum Servers — `09-feature-9-finding-minimum-servers.md`
- [x] 10 What Did We Learn? — `10-what-did-we-learn.md`
- [x] 11 DIY:  Implement Trie — `11-diy-implement-trie.md`
- [x] 12 DIY: Suggest Relevant Sentences — `12-diy-suggest-relevant-sentences.md`
- [x] 13 DIY: Word Break — `13-diy-word-break.md`
- [x] 14 DIY: Word Break II — `14-diy-word-break-ii.md`
- [x] 15 DIY: Product of Array Elements Except Itself — `15-diy-product-of-array-elements-except-itself.md`
- [x] 16 DIY: Reorganizing a String — `16-diy-reorganizing-a-string.md`
- [x] 17 DIY: Exclusive Time of Functions — `17-diy-exclusive-time-of-functions.md`
- [x] 18 DIY: Snapshot Array — `18-diy-snapshot-array.md`
- [x] 19 DIY: Coin Change — `19-diy-coin-change.md`
- [x] 20 DIY: Coin Change 2 — `20-diy-coin-change-2.md`
- [x] 21 DIY: Combination Sum — `21-diy-combination-sum.md`

## 04 Google Calendar (17/17) ✅
Folder: `04-google-calendar/`

- [x] 00 Project Description for Google Calendar — `00-project-description-for-google-calendar.md`
- [x] 01 Feature #1: Find Meeting Rooms — `01-feature-1-find-meeting-rooms.md`
- [x] 02 Feature #2: Show Busy Schedule — `02-feature-2-show-busy-schedule.md`
- [x] 03 Feature #3: Check if Meeting is Possible — `03-feature-3-check-if-meeting-is-possible.md`
- [x] 04 Feature #4: Schedule a New Meeting — `04-feature-4-schedule-a-new-meeting.md`
- [x] 05 Feature #5: Find Common Meeting Times — `05-feature-5-find-common-meeting-times.md`
- [x] 06 Feature #6: Find Two Sets of Consecutive Days — `06-feature-6-find-two-sets-of-consecutive-days.md`
- [x] 07 Feature #7: Longest Busy Period — `07-feature-7-longest-busy-period.md`
- [x] 08 What Did We Learn? — `08-what-did-we-learn.md`
- [x] 09 DIY: Find Interval Sets — `09-diy-find-interval-sets.md`
- [x] 10 DIY: Merge Intervals — `10-diy-merge-intervals.md`
- [x] 11 DIY: My Calendar — `11-diy-my-calendar.md`
- [x] 12 DIY: Insert Interval — `12-diy-insert-interval.md`
- [x] 13 DIY: Interval Lists Intersection — `13-diy-interval-lists-intersection.md`
- [x] 14 DIY: Employee Free Time — `14-diy-employee-free-time.md`
- [x] 15 DIY: Find Two Non-Overlapping Subarrays Each with Target Sum — `15-diy-find-two-non-overlapping-subarrays-each-with-target-sum.md`
- [x] 16 DIY: Longest Consecutive Sequence — `16-diy-longest-consecutive-sequence.md`

## 05 Stock Scraper (13/13) ✅
Folder: `05-stock-scraper/`

- [x] 00 Project Description for Stock Scraper — `00-project-description-for-stock-scraper.md`
- [x] 01 Feature #1: Traversing DOM Tree — `01-feature-1-traversing-dom-tree.md`
- [x] 02 Feature #2: Locating Stock Data — `02-feature-2-locating-stock-data.md`
- [x] 03 Feature #3: Traversing DOM Tree II — `03-feature-3-traversing-dom-tree-ii.md`
- [x] 04 Feature #4: Maximum Profit — `04-feature-4-maximum-profit.md`
- [x] 05 What Did We Learn? — `05-what-did-we-learn.md`
- [x] 06 DIY: Binary Tree Level Order Traversal — `06-diy-binary-tree-level-order-traversal.md`
- [x] 07 DIY: Lowest Common Ancestor of a Binary Tree — `07-diy-lowest-common-ancestor-of-a-binary-tree.md`
- [x] 08 DIY: Lowest Common Ancestor of a Binary Tree III — `08-diy-lowest-common-ancestor-of-a-binary-tree-iii.md`
- [x] 09 DIY: Populating Next Right Pointers in Each Node — `09-diy-populating-next-right-pointers-in-each-node.md`
- [x] 10 DIY: Populating Next Right Pointers in Each Node II — `10-diy-populating-next-right-pointers-in-each-node-ii.md`
- [x] 11 DIY: Maximum Subarray — `11-diy-maximum-subarray.md`
- [x] 12 DIY: Best Time to Buy and Sell Stock — `12-diy-best-time-to-buy-and-sell-stock.md`

## 06 UBER (18/18) ✅
Folder: `06-uber/`

- [x] 00 Project Description for Uber — `00-project-description-for-uber.md`
- [x] 01 Feature #1: Select Closest Drivers — `01-feature-1-select-closest-drivers.md`
- [x] 02 Feature #2: Path Cost — `02-feature-2-path-cost.md`
- [x] 03 Feature #3: Plot and Select Path — `03-feature-3-plot-and-select-path.md`
- [x] 04 Feature #4: Fare in Words — `04-feature-4-fare-in-words.md`
- [x] 05 Feature #5: Uber Pool — `05-feature-5-uber-pool.md`
- [x] 06 Feature #6: Longest Route — `06-feature-6-longest-route.md`
- [x] 07 Feature #7: Highest Rank — `07-feature-7-highest-rank.md`
- [x] 08 Feature #8: Optimal Path — `08-feature-8-optimal-path.md`
- [x] 09 What Did We Learn? — `09-what-did-we-learn.md`
- [x] 10 DIY: K Closest Points to Origin — `10-diy-k-closest-points-to-origin.md`
- [x] 11 DIY: Trapping Rainwater — `11-diy-trapping-rainwater.md`
- [x] 12 DIY: Evaluate Division — `12-diy-evaluate-division.md`
- [x] 13 DIY: Integer to English Words — `13-diy-integer-to-english-words.md`
- [x] 14 DIY: Random Pick with Weight — `14-diy-random-pick-with-weight.md`
- [x] 15 DIY: Diameter of Binary Tree — `15-diy-diameter-of-binary-tree.md`
- [x] 16 DIY: Kth Largest Element in an Array — `16-diy-kth-largest-element-in-an-array.md`
- [x] 17 DIY: Minimum Path Sum — `17-diy-minimum-path-sum.md`

## 07 Amazon (34/34) ✅
Folder: `07-amazon/`

- [x] 00 Project Description for Amazon — `00-project-description-for-amazon.md`
- [x] 01 Feature #1: Suggest Items for Free Delivery — `01-feature-1-suggest-items-for-free-delivery.md`
- [x] 02 Feature #2: Suggest Items for Special Offer — `02-feature-2-suggest-items-for-special-offer.md`
- [x] 03 Feature #3: Upselling Products — `03-feature-3-upselling-products.md`
- [x] 04 Feature #4: Copy Product Data — `04-feature-4-copy-product-data.md`
- [x] 05 Feature #5: Order Processing Milestones — `05-feature-5-order-processing-milestones.md`
- [x] 06 Feature #6: Products Frequently Viewed Together — `06-feature-6-products-frequently-viewed-together.md`
- [x] 07 Feature #7: Optimize Delivery Cost — `07-feature-7-optimize-delivery-cost.md`
- [x] 08 Feature #8: Merge Recommendations — `08-feature-8-merge-recommendations.md`
- [x] 09 Feature #9: Products in Price Range — `09-feature-9-products-in-price-range.md`
- [x] 10 Feature #10: Calculate the Total Cost of the Shopping Cart Items — `10-feature-10-calculate-the-total-cost-of-the-shopping-cart-items.md`
- [x] 11 Feature #11: Ad Serving — `11-feature-11-ad-serving.md`
- [x] 12 Feature #12: Warehouse and Drop Points — `12-feature-12-warehouse-and-drop-points.md`
- [x] 13 Feature #13: Time-Based Item Price Store — `13-feature-13-time-based-item-price-store.md`
- [x] 14 Feature #14: Find Similar Products — `14-feature-14-find-similar-products.md`
- [x] 15 What Did We Learn? — `15-what-did-we-learn.md`
- [x] 16 DIY: Two Sum — `16-diy-two-sum.md`
- [x] 17 DIY: Three Sum — `17-diy-three-sum.md`
- [x] 18 DIY: 3Sum Closest — `18-diy-3sum-closest.md`
- [x] 19 DIY: Insert, Delete, and GetRandom in O(1) — `19-diy-insert-delete-and-getrandom-in-o1.md`
- [x] 20 DIY: Insert Delete GetRandom O(1) - Duplicates Allowed — `20-diy-insert-delete-getrandom-o1---duplicates-allowed.md`
- [x] 21 DIY: Copy List with Random Pointer — `21-diy-copy-list-with-random-pointer.md`
- [x] 22 DIY: Find First and Last Position of an Element in Sorted Array — `22-diy-find-first-and-last-position-of-an-element-in-sorted-array.md`
- [x] 23 DIY: Find All Anagrams in a String — `23-diy-find-all-anagrams-in-a-string.md`
- [x] 24 DIY: Random Pick Index — `24-diy-random-pick-index.md`
- [x] 25 DIY: Continuous Subarray Sum — `25-diy-continuous-subarray-sum.md`
- [x] 26 DIY: Accounts Merge — `26-diy-accounts-merge.md`
- [x] 27 DIY: Range Sum of BST — `27-diy-range-sum-of-bst.md`
- [x] 28 DIY: Basic Calculator II — `28-diy-basic-calculator-ii.md`
- [x] 29 DIY: Basic Calculator III — `29-diy-basic-calculator-iii.md`
- [x] 30 DIY: Read N Characters Given Read4 II — Call Multiple Times — `30-diy-read-n-characters-given-read4-ii-call-multiple-times.md`
- [x] 31 DIY: Walls and Gates — `31-diy-walls-and-gates.md`
- [x] 32 DIY: Time-Based Key-Value Store — `32-diy-time-based-key-value-store.md`
- [x] 33 DIY: Intersection of Two Arrays — `33-diy-intersection-of-two-arrays.md`

## 08 Zoom (12/12) ✅
Folder: `08-zoom/`

- [x] 00 Project Description for Zoom — `00-project-description-for-zoom.md`
- [x] 01 Feature #1: Display Meeting Lobby — `01-feature-1-display-meeting-lobby.md`
- [x] 02 Feature #2: Serialize and Deserialize Participant Data — `02-feature-2-serialize-and-deserialize-participant-data.md`
- [x] 03 Feature #3: Meeting Activity — `03-feature-3-meeting-activity.md`
- [x] 04 Feature #4: Validate Sorted Participants Data — `04-feature-4-validate-sorted-participants-data.md`
- [x] 05 Feature #5: Auto Rotate in Mobile Devices — `05-feature-5-auto-rotate-in-mobile-devices.md`
- [x] 06 What Did We Learn? — `06-what-did-we-learn.md`
- [x] 07 DIY: Binary Search Tree Iterator — `07-diy-binary-search-tree-iterator.md`
- [x] 08 DIY: Serialize and Deserialize Binary Tree — `08-diy-serialize-and-deserialize-binary-tree.md`
- [x] 09 DIY: Jump Game IV — `09-diy-jump-game-iv.md`
- [x] 10 DIY: Validate the Binary Search Tree — `10-diy-validate-the-binary-search-tree.md`
- [x] 11 DIY: Rotate Image — `11-diy-rotate-image.md`

## 09 Plagiarism Checker (6/6) ✅
Folder: `09-plagiarism-checker/`

- [x] 00 Project Description for Plagiarism Checker — `00-project-description-for-plagiarism-checker.md`
- [x] 01 Feature #1: Possible Matches — `01-feature-1-possible-matches.md`
- [x] 02 Feature #2: Return Match — `02-feature-2-return-match.md`
- [x] 03 What Did We Learn? — `03-what-did-we-learn.md`
- [x] 04 DIY: Number of Matching Subsequences — `04-diy-number-of-matching-subsequences.md`
- [x] 05 DIY: Minimum Window Subsequence — `05-diy-minimum-window-subsequence.md`

## 10 Network (27/27) ✅
Folder: `10-network/`

- [x] 00 Project Description for Network — `00-project-description-for-network.md`
- [x] 01 Feature #1: Total Time — `01-feature-1-total-time.md`
- [x] 02 Feature #2: TTL Expiry — `02-feature-2-ttl-expiry.md`
- [x] 03 Feature #3: Minimum Hops — `03-feature-3-minimum-hops.md`
- [x] 04 Feature #4: Maximum Routers — `04-feature-4-maximum-routers.md`
- [x] 05 Feature #5: Update VLAN ID — `05-feature-5-update-vlan-id.md`
- [x] 06 Feature #6: Transmission Error — `06-feature-6-transmission-error.md`
- [x] 07 Feature #7: Divide Files Over the Network — `07-feature-7-divide-files-over-the-network.md`
- [x] 08 Feature #8: Maximum Clock Skew — `08-feature-8-maximum-clock-skew.md`
- [x] 09 Feature #9: Update Configuration — `09-feature-9-update-configuration.md`
- [x] 10 Feature #10: Minimum Variation — `10-feature-10-minimum-variation.md`
- [x] 11 Feature #11: Weighted Exponential Back-off — `11-feature-11-weighted-exponential-back-off.md`
- [x] 12 Feature #12: Peak Signal Strength — `12-feature-12-peak-signal-strength.md`
- [x] 13 What Did We Learn? — `13-what-did-we-learn.md`
- [x] 14 DIY: Time Needed to Inform All Employees — `14-diy-time-needed-to-inform-all-employees.md`
- [x] 15 DIY: All Nodes Distance K in Binary Tree — `15-diy-all-nodes-distance-k-in-binary-tree.md`
- [x] 16 DIY: Jump Game II — `16-diy-jump-game-ii.md`
- [x] 17 DIY: Longest Increasing Path in a Matrix — `17-diy-longest-increasing-path-in-a-matrix.md`
- [x] 18 DIY: Flood Fill — `18-diy-flood-fill.md`
- [x] 19 DIY: Valid Palindrome II — `19-diy-valid-palindrome-ii.md`
- [x] 20 DIY: Partition Labels — `20-diy-partition-labels.md`
- [x] 21 DIY: Maximum Difference Between Node and Ancestor — `21-diy-maximum-difference-between-node-and-ancestor.md`
- [x] 22 DIY: Rotting Oranges — `22-diy-rotting-oranges.md`
- [x] 23 DIY: Longest Subarray With Absolute Diff Less Than Equal to Limit — `23-diy-longest-subarray-with-absolute-diff-less-than-equal-to-limit.md`
- [x] 24 DIY: Add Two Numbers — `24-diy-add-two-numbers.md`
- [x] 25 DIY: Add Two Numbers II — `25-diy-add-two-numbers-ii.md`
- [x] 26 DIY: Find Peak Element — `26-diy-find-peak-element.md`

## 11 Cyber Security (12/12) ✅
Folder: `11-cyber-security/`

- [x] 00 Project Description for Cyber Security — `00-project-description-for-cyber-security.md`
- [x] 01 Feature #1: Validate Packet Structure — `01-feature-1-validate-packet-structure.md`
- [x] 02 Feature #2: Verify Message Integrity — `02-feature-2-verify-message-integrity.md`
- [x] 03 Feature #3: Find Dictionary — `03-feature-3-find-dictionary.md`
- [x] 04 Feature #4: Ways to Decode Message — `04-feature-4-ways-to-decode-message.md`
- [x] 05 Feature #5: Eligible Candidates — `05-feature-5-eligible-candidates.md`
- [x] 06 What Did We Learn? — `06-what-did-we-learn.md`
- [x] 07 DIY: UTF-8 Validation — `07-diy-utf-8-validation.md`
- [x] 08 DIY: Verifying an Alien Dictionary — `08-diy-verifying-an-alien-dictionary.md`
- [x] 09 DIY: Alien Dictionary — `09-diy-alien-dictionary.md`
- [x] 10 DIY: Decode Ways — `10-diy-decode-ways.md`
- [x] 11 DIY: Find K Closest Elements — `11-diy-find-k-closest-elements.md`

## 12 Operating System (35/35) ✅
Folder: `12-operating-system/`

- [x] 00 Project Description for Operating System — `00-project-description-for-operating-system.md`
- [x] 01 Feature #1: Allocate Space — `01-feature-1-allocate-space.md`
- [x] 02 Feature #2: Resume Process — `02-feature-2-resume-process.md`
- [x] 03 Feature #3: Schedule Processes — `03-feature-3-schedule-processes.md`
- [x] 04 Feature #4: Compress File — `04-feature-4-compress-file.md`
- [x] 05 Feature #5: Recover Files — `05-feature-5-recover-files.md`
- [x] 06 Feature #6: File Management System — `06-feature-6-file-management-system.md`
- [x] 07 Feature #7: Serialize and Deserialize File System — `07-feature-7-serialize-and-deserialize-file-system.md`
- [x] 08 Feature #8: Compress File II — `08-feature-8-compress-file-ii.md`
- [x] 09 Feature #9: File Search — `09-feature-9-file-search.md`
- [x] 10 Feature #10: Decode a Message — `10-feature-10-decode-a-message.md`
- [x] 11 Feature #11: Directory Iterator — `11-feature-11-directory-iterator.md`
- [x] 12 Feature #12: Priority Validation — `12-feature-12-priority-validation.md`
- [x] 13 Feature #13: Reverse Commands — `13-feature-13-reverse-commands.md`
- [x] 14 Feature #14: Releasing Process Lock — `14-feature-14-releasing-process-lock.md`
- [x] 15 Feature #15: Queue Reconstruction by Priority — `15-feature-15-queue-reconstruction-by-priority.md`
- [x] 16 What Did We Learn? — `16-what-did-we-learn.md`
- [x] 17 DIY: Subarray Sum Equals K — `17-diy-subarray-sum-equals-k.md`
- [x] 18 DIY: Missing Element in a Sorted Array — `18-diy-missing-element-in-a-sorted-array.md`
- [x] 19 DIY: Course Schedule — `19-diy-course-schedule.md`
- [x] 20 DIY: Course Schedule II — `20-diy-course-schedule-ii.md`
- [x] 21 DIY: Sequence Reconstruction — `21-diy-sequence-reconstruction.md`
- [x] 22 DIY: Concatenated Words — `22-diy-concatenated-words.md`
- [x] 23 DIY: Minimum Remove to Make Valid Parentheses — `23-diy-minimum-remove-to-make-valid-parentheses.md`
- [x] 24 DIY: Design Add and Search Words Data Structure — `24-diy-design-add-and-search-words-data-structure.md`
- [x] 25 DIY: Serialize and Deserialize N-ary Tree — `25-diy-serialize-and-deserialize-n-ary-tree.md`
- [x] 26 DIY: String Compression — `26-diy-string-compression.md`
- [x] 27 DIY: Regular Expression Matching — `27-diy-regular-expression-matching.md`
- [x] 28 DIY: Wildcard Matching — `28-diy-wildcard-matching.md`
- [x] 29 DIY: 24 Game — `29-diy-24-game.md`
- [x] 30 DIY: Flatten Nested List Iterator — `30-diy-flatten-nested-list-iterator.md`
- [x] 31 DIY: Frog Jump — `31-diy-frog-jump.md`
- [x] 32 DIY: Queue Reconstruction by Height — `32-diy-queue-reconstruction-by-height.md`
- [x] 33 DIY: Reverse Words in a String — `33-diy-reverse-words-in-a-string.md`
- [x] 34 DIY: Single Element in a Sorted Array — `34-diy-single-element-in-a-sorted-array.md`

## 13 Language Compiler (22/22) ✅
Folder: `13-language-compiler/`

- [x] 00 Project Description for Language Compiler — `00-project-description-for-language-compiler.md`
- [x] 01 Feature #1: Remove Comments — `01-feature-1-remove-comments.md`
- [x] 02 Feature #2: Evaluate the Arithmetic Expression — `02-feature-2-evaluate-the-arithmetic-expression.md`
- [x] 03 Feature #3: Loop Unrolling — `03-feature-3-loop-unrolling.md`
- [x] 04 Feature #4: Optimization by Replacement — `04-feature-4-optimization-by-replacement.md`
- [x] 05 Feature #5:  Compilation Step Failure — `05-feature-5-compilation-step-failure.md`
- [x] 06 Feature #6: Most Common Token — `06-feature-6-most-common-token.md`
- [x] 07 Feature #7: Exponentiation for Mobile Devices — `07-feature-7-exponentiation-for-mobile-devices.md`
- [x] 08 Feature #8: Divide in Power Save Mode — `08-feature-8-divide-in-power-save-mode.md`
- [x] 09 Feature #9: Validate Program Brackets — `09-feature-9-validate-program-brackets.md`
- [x] 10 What Did We Learn? — `10-what-did-we-learn.md`
- [x] 11 DIY: Count a Word in the Comments — `11-diy-count-a-word-in-the-comments.md`
- [x] 12 DIY: Basic Calculator — `12-diy-basic-calculator.md`
- [x] 13 DIY: Decoding a String — `13-diy-decoding-a-string.md`
- [x] 14 DIY: Find and Replace in a String — `14-diy-find-and-replace-in-a-string.md`
- [x] 15 DIY: First Bad Version — `15-diy-first-bad-version.md`
- [x] 16 DIY: Nested List Weight Sum — `16-diy-nested-list-weight-sum.md`
- [x] 17 DIY: Most Common Word — `17-diy-most-common-word.md`
- [x] 18 DIY: Pow(x, n) — `18-diy-powx-n.md`
- [x] 19 DIY: Divide Two Integers — `19-diy-divide-two-integers.md`
- [x] 20 DIY: Valid Parentheses — `20-diy-valid-parentheses.md`
- [x] 21 DIY: Valid Parenthesis String — `21-diy-valid-parenthesis-string.md`

## 14 Boggle (6/6) ✅
Folder: `14-boggle/`

- [x] 00 Project Description for Boggle — `00-project-description-for-boggle.md`
- [x] 01 Feature #1: Search for a Single Word in the Boggle Grid — `01-feature-1-search-for-a-single-word-in-the-boggle-grid.md`
- [x] 02 Feature #2: Search for Maximum Number of Words in the Boggle Grid — `02-feature-2-search-for-maximum-number-of-words-in-the-boggle-grid.md`
- [x] 03 What Did We Learn? — `03-what-did-we-learn.md`
- [x] 04 DIY: Word Search I — `04-diy-word-search-i.md`
- [x] 05 DIY: Word Search II — `05-diy-word-search-ii.md`

## 15 Scrabble 2.0 (6/6) ✅
Folder: `15-scrabble-20/`

- [x] 00 Project Description for Scrabble — `00-project-description-for-scrabble.md`
- [x] 01 Feature #1: Minimum Moves — `01-feature-1-minimum-moves.md`
- [x] 02 Feature #2: Possible Results — `02-feature-2-possible-results.md`
- [x] 03 What Did We Learn? — `03-what-did-we-learn.md`
- [x] 04 DIY: Word Ladder I — `04-diy-word-ladder-i.md`
- [x] 05 DIY: Word Ladder II — `05-diy-word-ladder-ii.md`

## 16 Game (9/9) ✅
Folder: `16-game/`

- [x] 00 Project Description for Game — `00-project-description-for-game.md`
- [x] 01 Feature #1: Hand of Straights — `01-feature-1-hand-of-straights.md`
- [x] 02 Feature #2: Maximum Points You Can Obtain from Cards — `02-feature-2-maximum-points-you-can-obtain-from-cards.md`
- [x] 03 Feature #3: Balloon Splash — `03-feature-3-balloon-splash.md`
- [x] 04 What Did We Learn? — `04-what-did-we-learn.md`
- [x] 05 DIY: Divide Array in Sets of K Consecutive Numbers — `05-diy-divide-array-in-sets-of-k-consecutive-numbers.md`
- [x] 06 DIY: Find Maximum Sum from Either End of an Array — `06-diy-find-maximum-sum-from-either-end-of-an-array.md`
- [x] 07 DIY: Remove All Adjacent Duplicates In String — `07-diy-remove-all-adjacent-duplicates-in-string.md`
- [x] 08 DIY: Remove All Adjacent Duplicates in String II — `08-diy-remove-all-adjacent-duplicates-in-string-ii.md`

## 17 Stocks (19/19) ✅
Folder: `17-stocks/`

- [x] 00 Project Description for Stocks — `00-project-description-for-stocks.md`
- [x] 01 Feature #1: Validate Price — `01-feature-1-validate-price.md`
- [x] 02 Feature #2: Settling Period — `02-feature-2-settling-period.md`
- [x] 03 Feature #3: Goals Fulfilled — `03-feature-3-goals-fulfilled.md`
- [x] 04 Feature #4: Milestone Reached — `04-feature-4-milestone-reached.md`
- [x] 05 Feature #5: Top Brokers — `05-feature-5-top-brokers.md`
- [x] 06 Feature #6: Assign Transactions — `06-feature-6-assign-transactions.md`
- [x] 07 Feature #7: Process Transactions — `07-feature-7-process-transactions.md`
- [x] 08 Feature #8: Find Intervals — `08-feature-8-find-intervals.md`
- [x] 09 What Did We Learn? — `09-what-did-we-learn.md`
- [x] 10 DIY: Valid Number — `10-diy-valid-number.md`
- [x] 11 DIY: Task Scheduler — `11-diy-task-scheduler.md`
- [x] 12 DIY: Split Array into Consecutive Subsequences — `12-diy-split-array-into-consecutive-subsequences.md`
- [x] 13 DIY: Searching a 2D Matrix — `13-diy-searching-a-2d-matrix.md`
- [x] 14 DIY: Top K Frequent Elements — `14-diy-top-k-frequent-elements.md`
- [x] 15 DIY: Reverse Nodes in k-Group — `15-diy-reverse-nodes-in-k-group.md`
- [x] 16 DIY: Reverse Linked List II — `16-diy-reverse-linked-list-ii.md`
- [x] 17 DIY: String to Integer (atoi) — `17-diy-string-to-integer-atoi.md`
- [x] 18 DIY: Daily Temperatures — `18-diy-daily-temperatures.md`

## 18 Computational Biology (21/21) ✅
Folder: `18-computational-biology/`

- [x] 00 Project Description for Computational Biology — `00-project-description-for-computational-biology.md`
- [x] 01 Feature #1: Mutate DNA — `01-feature-1-mutate-dna.md`
- [x] 02 Feature #2: Detect Virus — `02-feature-2-detect-virus.md`
- [x] 03 Feature #3: Locate Protein — `03-feature-3-locate-protein.md`
- [x] 04 Feature #4: Identifying Proteins — `04-feature-4-identifying-proteins.md`
- [x] 05 Feature #5: Mutating a Virus — `05-feature-5-mutating-a-virus.md`
- [x] 06 Feature #6: Identify a Species — `06-feature-6-identify-a-species.md`
- [x] 07 Feature #7: Detecting a Protein — `07-feature-7-detecting-a-protein.md`
- [x] 08 Feature #8: Similarity Measure Between DNA Samples — `08-feature-8-similarity-measure-between-dna-samples.md`
- [x] 09 Feature #9: Kth Missing Gene — `09-feature-9-kth-missing-gene.md`
- [x] 10 What Did We Learn? — `10-what-did-we-learn.md`
- [x] 11 DIY: String Transforms into Another String — `11-diy-string-transforms-into-another-string.md`
- [x] 12 DIY: Longest Substring with At Most K Distinct Characters — `12-diy-longest-substring-with-at-most-k-distinct-characters.md`
- [x] 13 DIY: Longest Palindromic Substring — `13-diy-longest-palindromic-substring.md`
- [x] 14 DIY: Valid Palindrome — `14-diy-valid-palindrome.md`
- [x] 15 DIY: Next Permutation — `15-diy-next-permutation.md`
- [x] 16 DIY: Longest Substring without Repeating Characters — `16-diy-longest-substring-without-repeating-characters.md`
- [x] 17 DIY: Palindrome Permutation — `17-diy-palindrome-permutation.md`
- [x] 18 DIY: Palindromic Substrings — `18-diy-palindromic-substrings.md`
- [x] 19 DIY: Edit Distance — `19-diy-edit-distance.md`
- [x] 20 DIY: Kth Missing Positive Number — `20-diy-kth-missing-positive-number.md`

## 19 Cellular Operator(AT&T) (20/20) ✅
Folder: `19-cellular-operatoratt/`

- [x] 00 Project Description for Cellular Operator — `00-project-description-for-cellular-operator.md`
- [x] 01 Feature #1: Determine Location — `01-feature-1-determine-location.md`
- [x] 02 Feature #2: Low Coverage Area — `02-feature-2-low-coverage-area.md`
- [x] 03 Feature #3: Power Up the Station — `03-feature-3-power-up-the-station.md`
- [x] 04 Feature #4: Query Peak Users — `04-feature-4-query-peak-users.md`
- [x] 05 Feature #5: Densest Deployment — `05-feature-5-densest-deployment.md`
- [x] 06 Feature #6: Maximum Users — `06-feature-6-maximum-users.md`
- [x] 07 Feature #7: Maximum Contiguous Area — `07-feature-7-maximum-contiguous-area.md`
- [x] 08 Feature #8: Maximum Signal Strength — `08-feature-8-maximum-signal-strength.md`
- [x] 09 What Did We Learn? — `09-what-did-we-learn.md`
- [x] 10 DIY: Search a 2D Matrix II — `10-diy-search-a-2d-matrix-ii.md`
- [x] 11 DIY: Largest Rectangle in Histogram — `11-diy-largest-rectangle-in-histogram.md`
- [x] 12 DIY: Maximal rectangle — `12-diy-maximal-rectangle.md`
- [x] 13 DIY: Open Lock — `13-diy-open-lock.md`
- [x] 14 DIY: Range Sum Query 2D — Immutable — `14-diy-range-sum-query-2d-immutable.md`
- [x] 15 DIY: Minimum Area Rectangle — `15-diy-minimum-area-rectangle.md`
- [x] 16 DIY: Sliding Window Maximum — `16-diy-sliding-window-maximum.md`
- [x] 17 DIY: Max Area of an Island — `17-diy-max-area-of-an-island.md`
- [x] 18 DIY: Max Consecutive Ones III — `18-diy-max-consecutive-ones-iii.md`
- [x] 19 DIY: Shortest Bridge — `19-diy-shortest-bridge.md`

## 20 Twitter (17/17) ✅
Folder: `20-twitter/`

- [x] 00 Project Description for Twitter — `00-project-description-for-twitter.md`
- [x] 01 Feature #1: Add Likes — `01-feature-1-add-likes.md`
- [x] 02 Feature #2: Merge Tweets In Twitter Feed — `02-feature-2-merge-tweets-in-twitter-feed.md`
- [x] 03 Feature #3: Identify Peak Interaction Times — `03-feature-3-identify-peak-interaction-times.md`
- [x] 04 Feature #4: Split Users into Two Groups — `04-feature-4-split-users-into-two-groups.md`
- [x] 05 Feature #5: Drawing a Global Profile of Viral Tweets — `05-feature-5-drawing-a-global-profile-of-viral-tweets.md`
- [x] 06 Feature #6:  Incoming Tweets Predictor — `06-feature-6-incoming-tweets-predictor.md`
- [x] 07 Feature #7: Trending Hashtags — `07-feature-7-trending-hashtags.md`
- [x] 08 What Did We Learn? — `08-what-did-we-learn.md`
- [x] 09 DIY: Add Binary — `09-diy-add-binary.md`
- [x] 10 DIY: Multiply Strings — `10-diy-multiply-strings.md`
- [x] 11 DIY: Merge Sorted Arrays — `11-diy-merge-sorted-arrays.md`
- [x] 12 DIY: Maximum Sum of Three Non-Overlapping Arrays — `12-diy-maximum-sum-of-three-non-overlapping-arrays.md`
- [x] 13 DIY: Is Graph Bipartite? — `13-diy-is-graph-bipartite.md`
- [x] 14 DIY: The Skyline Problem — `14-diy-the-skyline-problem.md`
- [x] 15 DIY: Moving Average from a Data Stream — `15-diy-moving-average-from-a-data-stream.md`
- [x] 16 DIY: Find Duplicate Files in System — `16-diy-find-duplicate-files-in-system.md`

## 21 Trees (8/8) ✅
Folder: `21-trees/`

- [x] 00 Invert Binary Tree — `00-invert-binary-tree.md`
- [x] 01 Flip Equivalent Binary Trees — `01-flip-equivalent-binary-trees.md`
- [x] 02 Binary Tree Maximum Path Sum — `02-binary-tree-maximum-path-sum.md`
- [x] 03 Binary Tree Zigzag Level Order Traversal — `03-binary-tree-zigzag-level-order-traversal.md`
- [x] 04 Binary Tree Right Side View — `04-binary-tree-right-side-view.md`
- [x] 05 Binary Tree Vertical Order Traversal — `05-binary-tree-vertical-order-traversal.md`
- [x] 06 Boundary of Binary Tree — `06-boundary-of-binary-tree.md`
- [x] 07 Flatten Binary Tree to Linked List — `07-flatten-binary-tree-to-linked-list.md`

## 22 Miscellaneous (14/14) ✅
Folder: `22-miscellaneous/`

- [x] 00 Minimum Knight Moves — `00-minimum-knight-moves.md`
- [x] 01 Sparse Matrix Multiplication — `01-sparse-matrix-multiplication.md`
- [x] 02 Design In-Memory File System — `02-design-in-memory-file-system.md`
- [x] 03 Spiral Matrix — `03-spiral-matrix.md`
- [x] 04 Design Tic-Tac-Toe — `04-design-tic-tac-toe.md`
- [x] 05 The Maze — `05-the-maze.md`
- [x] 06 Restore IP Addresses — `06-restore-ip-addresses.md`
- [x] 07 Reconstruct Itinerary — `07-reconstruct-itinerary.md`
- [x] 08 Text Justification — `08-text-justification.md`
- [x] 09 Gas Station — `09-gas-station.md`
- [x] 10 Strong Password Checker — `10-strong-password-checker.md`
- [x] 11 Sudoku Solver — `11-sudoku-solver.md`
- [x] 12 Pacific Atlantic Water Flow — `12-pacific-atlantic-water-flow.md`
- [x] 13 Angle Between the Hands of a Clock — `13-angle-between-the-hands-of-a-clock.md`

## 23 Conclusion (1/1) ✅
Folder: `23-conclusion/`

- [x] 00 Where to Go from Here? — `00-where-to-go-from-here.md`
