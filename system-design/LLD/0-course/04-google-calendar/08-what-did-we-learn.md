# What Did We Learn?

## What have we accomplished?

Same lesson once more: every Google Calendar feature maps to a well-known interview question.

| Google Calendar Feature | Underlying Pattern | Matching Interview Question |
|---|---|---|
| #1 Find Meeting Rooms | Min heap over end times | Meeting Rooms II |
| #2 Show Busy Schedule | Sort + linear merge | Merge Intervals |
| #3 Check if Meeting is Possible | Interval BST | My Calendar I |
| #4 Schedule a New Meeting | Three-phase linear scan | Insert Interval |
| #5 Find Common Meeting Times | Two-pointer interval sweep | Interval List Intersections |
| #6 Find Two Sets of Consecutive Days | Sliding window + DP over window ends | Two Non-Overlapping Subarrays Each with Target Sum |
| #7 Longest Busy Period | HashSet, count only from sequence starts | Longest Consecutive Sequence |

This chapter was almost entirely about **interval reasoning**: sort by start time, then sweep once (merging, inserting, or intersecting depending on the exact question). Once you recognize a problem is "really" about a set of `(start, end)` ranges, the sort-then-sweep recipe from this chapter covers most of what you'll need.

The DIY problems ahead restate these same seven patterns without the calendar framing.
