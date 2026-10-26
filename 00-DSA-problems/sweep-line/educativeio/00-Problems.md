# Sweep Line Algorithm: Complete Problem Roadmap (Beginner to Advanced)

To master **Sweep Line Algorithm for Google, Meta, Amazon, and other senior software engineering interviews**, I recommend studying it as a collection of related techniques rather than memorizing one template.

The important distinction is that **not every interval problem is a genuine sweep-line problem**. Some use a sorted scan that resembles sweep line, some use an event-based sweep as their primary solution, and others require a more advanced combination of sweep line with a heap, segment tree, coordinate compression, or an ordered map.

This roadmap separates those categories so you can learn the actual pattern without incorrectly classifying unrelated problems.

## 1. What you need to master

Sweep line is a technique in which we process events in sorted order along a dimension, maintaining information about what is currently active.

For example, suppose meetings take place during these intervals:

```text
Meeting A: [1, 5)
Meeting B: [2, 6)
Meeting C: [4, 7)
```

We want to determine the maximum number of meetings happening simultaneously.

Instead of checking every possible pair of meetings, we create events:

```text
Time 1: +1    Meeting A starts
Time 2: +1    Meeting B starts
Time 4: +1    Meeting C starts
Time 5: -1    Meeting A ends
Time 6: -1    Meeting B ends
Time 7: -1    Meeting C ends
```

Then we process the events in chronological order.

| Time | Event | Active meetings | Maximum so far |
|---:|---|---:|---:|
| 1 | A starts | 1 | 1 |
| 2 | B starts | 2 | 2 |
| 4 | C starts | 3 | 3 |
| 5 | A ends | 2 | 3 |
| 6 | B ends | 1 | 3 |
| 7 | C ends | 0 | 3 |

The maximum concurrent count is **3**, so we need three meeting rooms.

This illustrates the fundamental transformation:

**Intervals → Events → Sorted events → Maintain active state → Extract the answer.**

The active state changes depending on the problem. It might be a counter, a maximum heap, a minimum heap, an ordered map, or a segment tree.

A standard event sweep takes `O(n log n)` time when sorting dominates, although advanced variants may require additional data structures.

The following roadmap is organized around this progression. [The DSA Handbook](https://dsa.handbook.academy/patterns/sweep-line-archetypes/?utm_source=chatgpt.com)

---

# 2. Classification: which problems genuinely require Sweep Line?

I recommend separating problems into three categories.

| Category | Meaning | What you should learn |
|---|---|---|
| A. Core Sweep Line | Event processing is the natural, central solution | Master the technique |
| B. Sweep Line + Data Structure | Sweep line is central, but another data structure maintains the active state | Master the combination |
| C. Sweep Line Applicable | Sweep line can solve the problem, but a simpler or more natural algorithm usually exists | Learn the alternative first |

There is also an important exclusion category: problems that are frequently grouped with sweep line but are fundamentally solved using a different pattern.

For example, Merge Intervals is closely related to sweep line, but its standard solution is a sort-and-merge scan. Similarly, Interval List Intersections is naturally solved with two pointers because its input lists are already sorted.

The distinction matters in interviews because the best algorithm depends on the exact problem requirements, not merely on whether intervals appear in the input. [Dharmveer Jakhar](https://dharmveerjakhar.in/blog/intervals?utm_source=chatgpt.com)

# 3. Category A — Core Sweep Line Problems

These are the problems I would prioritize when learning the actual pattern. They teach the fundamental event-processing techniques and the major variations you will encounter in harder problems.

## Tier 1: Event creation, overlap counting, and active intervals

Start here. These problems establish the foundation for almost every interval-based sweep.

| Order | Problem | Difficulty | Core technique | Why it matters |
|---:|---|---|---|---|
| 1 | Maximum Population Year | Easy | Difference array / event counting | Understand how increments and decrements accumulate over time |
| 2 | Meeting Rooms II | Medium | Event sorting and active count | The canonical introduction to sweep-line overlap counting |
| 3 | Car Pooling | Medium | Weighted event sweep | Track passenger counts instead of simply counting intervals |
| 4 | Divide Intervals Into Minimum Number of Groups | Medium | Event sorting and maximum concurrency | Handle interval endpoint semantics and minimum group counts |
| 5 | Brightest Position on Street | Medium | Weighted events and prefix accumulation | Track the sum of overlapping contributions |
| 6 | Employee Free Time | Hard | Multi-interval sweep and gap detection | Find regions where the active count is zero |
| 7 | Number of Flowers in Full Bloom | Hard | Start/end events and point queries | Answer how many intervals contain each query point |

### Problem details and links

**1. Maximum Population Year — LeetCode 1854**

Problem: urlMaximum Population Yearhttps://leetcode.com/problems/maximum-population-year/

- Difficulty: Easy.
- Main technique: Difference array or sweep-line counting.
- Input: Birth and death years.
- Goal: Find the year with the maximum population.
- Why solve it: Learn the simplest form of cumulative event counting.
- Complexity: `O(n + R)` using a bounded difference array, where `R` is the year range.

**Classification:** Core concept, but a bounded difference array is usually simpler than explicitly sorting events.

**2. Meeting Rooms II — LeetCode 253**

Problem: urlMeeting Rooms IIhttps://leetcode.com/problems/meeting-rooms-ii/

- Difficulty: Medium.
- Main technique: Event sorting and a running count.
- Input: Meeting start and end times.
- Goal: Find the minimum number of meeting rooms required.
- Key insight: Minimum rooms equal the maximum number of simultaneously active meetings.
- Complexity: `O(n log n)` time and `O(n)` space.

**Classification:** Essential core sweep-line problem.

You should solve it using both:
1. An explicit event list containing `+1` and `-1`.
2. Two sorted arrays of start and end times.

Also solve the heap-based version. Comparing the approaches will help you recognize when a sweep-line counter is sufficient and when tracking individual active intervals is more useful.

**3. Car Pooling — LeetCode 1094**

Problem: urlCar Poolinghttps://leetcode.com/problems/car-pooling/

- Difficulty: Medium.
- Main technique: Weighted sweep line.
- Input: Passenger counts, pickup locations, and drop-off locations.
- Goal: Determine whether the vehicle ever exceeds its capacity.
- Key insight: Pickup contributes a positive change; drop-off contributes a negative change.
- Complexity: `O(n log n)` with sorted events, or `O(n + R)` when using a bounded difference array.

**Classification:** Core sweep-line variation.

The important lesson is that the active state does not always count objects. It can represent a running quantity such as passengers, bandwidth, resource usage, or inventory.

**4. Divide Intervals Into Minimum Number of Groups — LeetCode 2406**

Problem: urlDivide Intervals Into Minimum Number of Groupshttps://leetcode.com/problems/divide-intervals-into-minimum-number-of-groups/

- Difficulty: Medium.
- Main technique: Endpoint events and maximum overlap.
- Goal: Partition intervals into the minimum number of groups such that intervals in the same group do not overlap.
- Key insight: The answer equals the maximum number of overlapping intervals.
- Complexity: `O(n log n)` time and `O(n)` space.

**Classification:** Essential core sweep-line problem.

Pay particular attention to the fact that this problem treats intervals as inclusive. Two intervals that both contain the same endpoint overlap, so the tie-breaking rules differ from those for half-open meeting intervals.

**5. Brightest Position on Street — LeetCode 2021**

Problem: urlBrightest Position on Streethttps://leetcode.com/problems/brightest-position-on-street/

- Difficulty: Medium.
- Main technique: Weighted events and cumulative sums.
- Goal: Find the position with maximum brightness contributed by overlapping lights.
- Key insight: Convert each light's coverage into changes in brightness, then accumulate those changes.
- Complexity: `O(n log n)` time and `O(n)` space.

**Classification:** Core sweep-line variation.

This problem reinforces the difference between counting active intervals and maintaining a weighted aggregate.

**6. Employee Free Time — LeetCode 759**

Problem: urlEmployee-Free-Timehttps://leetcode.com/problems/employee-free-time/

- Difficulty: Hard.
- Main technique: Multi-employee interval processing.
- Goal: Find intervals during which every employee is free.
- Key insight: A gap is free time when no employee's interval is active.
- Complexity: `O(N log N)` time and `O(N)` space for `N` total intervals using a straightforward event-based approach.

**Classification:** Core sweep-line problem, although merging all busy intervals is often the simplest solution.

Learn both approaches and understand why they produce the same free-time gaps.

**7. Number of Flowers in Full Bloom — LeetCode 2251**

Problem: urlNumber of Flowers in Full Bloomhttps://leetcode.com/problems/number-of-flowers-in-full-bloom/

- Difficulty: Hard.
- Main technique: Sweep-line counting and point queries.
- Goal: For every person's arrival time, count how many flowers are blooming.
- Key insight: Process interval changes and answer queries at the appropriate positions.
- Complexity: `O((n + m) log(n + m))` for `n` flower intervals and `m` queries using a combined event sweep.

**Classification:** Core sweep-line application.

This is particularly useful because the problem changes the question from finding the maximum overlap to answering many independent queries about the active count.

---

## Tier 2: Calendar booking and dynamic interval coverage

These problems introduce a new challenge: intervals arrive dynamically, and you must update the state after each operation.

A static sweep can sort all events once. An online booking system must maintain its state as new bookings arrive.

| Order | Problem | Difficulty | Main technique | Classification |
|---:|---|---|---|---|
| 8 | My Calendar I | Medium | Ordered interval map | Sweep-related; simpler online approach |
| 9 | My Calendar II | Medium | Overlap tracking / event deltas | Core dynamic sweep variation |
| 10 | My Calendar III | Hard | Ordered event-delta map | Core dynamic sweep variation |
| 11 | Range Module | Hard | Ordered intervals or segment tree | Advanced dynamic coverage |
| 12 | Amount of New Area Painted Each Day | Hard | Ordered disjoint intervals / interval tracking | Advanced sweep-related problem |

### Problem details

**8. My Calendar I — LeetCode 729**

Problem: urlMy-Calendar-Ihttps://leetcode.com/problems/my-calendar-i/

- Difficulty: Medium.
- Goal: Accept a booking only if it does not overlap an existing booking.
- Core lesson: Maintain intervals and efficiently detect conflicts.
- Recommended approach: `TreeMap` or an ordered interval structure.
- Complexity: Typically `O(n log n)` per booking using a `TreeMap` approach.

**Classification:** Sweep-related, but an ordered interval search is usually the more direct solution.

**9. My Calendar II — LeetCode 731**

Problem: urlMy-Calendar-IIhttps://leetcode.com/problems/my-calendar-ii/

- Difficulty: Medium.
- Goal: Allow double bookings but reject any booking that creates a triple booking.
- Core lesson: Maintain coverage counts and reject operations that violate a maximum overlap constraint.
- Recommended approach: An event-delta `TreeMap` is a useful sweep-line solution.
- Complexity: `O(n log n)` per insertion is achievable with a suitable balanced-tree approach and careful handling of the ordered deltas.

**Classification:** Important dynamic sweep-line variation.

**10. My Calendar III — LeetCode 732**

Problem: urlMy-Calendar-IIIhttps://leetcode.com/problems/my-calendar-iii/

- Difficulty: Hard.
- Goal: After each booking, return the maximum number of simultaneous bookings.
- Core lesson: Maintain event deltas and recompute the maximum prefix sum.
- Recommended approach: `TreeMap` of time-point deltas for a straightforward implementation.
- Complexity: `O(n log n)` per insertion with a `TreeMap` implementation that scans all event coordinates to recompute the maximum; the full sequence can therefore take `O(n² log n)` time.

**Classification:** Core dynamic sweep-line problem.

For advanced mastery, also investigate how an augmented balanced tree can maintain subtree sums and maximum prefix sums, enabling faster updates than rescanning all event points.

**11. Range Module — LeetCode 715**

Problem: urlRange-Modulehttps://leetcode.com/problems/range-module/

- Difficulty: Hard.
- Goal: Add ranges, remove ranges, and query whether a range is fully covered.
- Core lesson: Dynamic interval coverage.
- Recommended approach: `TreeMap` of disjoint intervals or a segment tree.
- Complexity: Depends on the chosen representation and operation.

**Classification:** Advanced interval-coverage problem, not a pure sweep-line problem.

**12. Amount of New Area Painted Each Day — LeetCode 2158**

Problem: urlAmount-of-New-Area-Painted-Each-Dayhttps://leetcode.com/problems/amount-of-new-area-painted-each-day/

- Difficulty: Hard.
- Goal: Determine how much previously unpainted area is painted each day.
- Core lesson: Maintain covered ranges and avoid counting previously covered positions again.
- Recommended approach: Ordered interval tracking or a successor data structure.
- Complexity: Depends on the implementation.

**Classification:** Sweep-related, but dynamic interval tracking is usually the more useful classification.

These calendar and coverage problems are valuable because they teach you when a static event sweep is no longer enough. [Yen.dev](https://yennj12.js.org/CS_basics/cheatsheets/scanning_line.html?utm_source=chatgpt.com)

---

# 4. Category B — Advanced Sweep Line + Data Structures

These are the problems that take you beyond counting overlaps.

The central challenge is maintaining a useful summary of the objects currently intersecting the sweep line.

For example:

- How many intervals are active? Use a counter.
- What is the largest active height? Use a maximum heap or ordered multiset.
- What is the minimum length of an active interval? Use a minimum heap.
- What is the total length covered on another axis? Use a segment tree with coverage information.
- How can we process range updates and queries efficiently? Use coordinate compression and an appropriate range-query data structure.

## Tier 3: Skyline and active maximum

| Order | Problem | Difficulty | Main technique | Priority |
|---:|---|---|---|---|
| 13 | The Skyline Problem | Hard | Sweep line + max heap + lazy deletion | Essential |
| 14 | Falling Squares | Hard | Coordinate compression + segment tree | Essential advanced |
| 15 | Perfect Rectangle | Hard | Rectangle geometry and coverage validation | Recommended |
| 16 | Rectangle Area II | Hard | Sweep line + coordinate compression + segment tree | Essential advanced |

### 13. The Skyline Problem — LeetCode 218

Problem: urlThe-Skyline-Problemhttps://leetcode.com/problems/the-skyline-problem/

This is one of the most important sweep-line problems for interview preparation.

**Problem:** Given buildings represented by `[left, right, height]`, return the key points where the outline of the buildings changes.

Example:

```text
Buildings:
[
    [2, 9, 10],
    [3, 7, 15],
    [5, 12, 12]
]
```

The skyline is:

```text
[
    [2, 10],
    [3, 15],
    [7, 12],
    [12, 0]
]
```

**Why is this a sweep-line problem?**

As we move from left to right, buildings become active at their left boundaries and stop being active at their right boundaries.

At each event coordinate, we need the maximum height among all active buildings.

**Core techniques:**
- Create building start and end events.
- Sort events by their x-coordinate.
- Maintain active building heights in a max heap or ordered multiset.
- Remove expired buildings, potentially using lazy deletion.
- Emit a key point whenever the maximum height changes.

**Complexity:** `O(n log n)` time and `O(n)` space.

**What you must master:**

1. Why a simple counter cannot solve this problem.
2. Why a maximum heap is a suitable active-state structure.
3. How to remove expired buildings efficiently.
4. How to process multiple events at the same x-coordinate without generating incorrect key points.
5. Why the final skyline must return to height zero.

Do not move to Rectangle Area II until you can explain the Skyline Problem without memorizing its code.

### 14. Falling Squares — LeetCode 699

Problem: urlFalling-Squareshttps://leetcode.com/problems/falling-squares/

- Difficulty: Hard.
- Goal: Determine the maximum height after each square falls and stacks on previously landed squares.
- Core technique: Coordinate compression with a segment tree, or an interval-based data structure.
- Complexity: `O(n log n)` with coordinate compression and a lazy segment tree for range maximum and assignment operations.

**Why it belongs here:**

Each new square asks about the maximum height over a horizontal interval. Its landing height depends on previously processed squares.

This is an excellent introduction to sweep-like processing combined with range-query data structures.

**Classification:** Advanced sweep-related problem. The chronological processing is central, but the segment tree is the main mechanism for efficiently maintaining interval state.

### 15. Perfect Rectangle — LeetCode 391

Problem: urlPerfect-Rectanglehttps://leetcode.com/problems/perfect-rectangle/

- Difficulty: Hard.
- Goal: Determine whether a collection of rectangles forms exactly one larger rectangle, with no gaps or overlaps.
- Core techniques: Area equality and corner counting.
- Complexity: `O(n)` expected time and `O(n)` space with an appropriate hash-set solution.

**Classification:** Not a core sweep-line problem.

Although a geometric sweep is possible, the area-and-corners method is usually more direct. Include this problem to learn how to recognize when *not* to use sweep line.

### 16. Rectangle Area II — LeetCode 850

Problem: urlRectangle-Area-IIhttps://leetcode.com/problems/rectangle-area-ii/

- Difficulty: Hard.
- Goal: Calculate the total area covered by multiple rectangles without counting overlapping regions more than once.
- Core techniques: Sweep line, coordinate compression, segment tree, and covered-length tracking.
- Complexity: `O(n log n)` time and `O(n)` space with an appropriately implemented segment tree and coordinate compression.

This is a major milestone in sweep-line mastery.

**The key idea:**

Between two consecutive x-coordinates, the set of active rectangles remains unchanged. If the covered length on the y-axis is `L`, and the horizontal distance to the next event is `Δx`, the area contributed by that slab is:

```text
Area = L × Δx
```

At each x-coordinate, update the y-interval coverage as rectangles start or end.

You must maintain the **length of the union of active y-intervals**, not simply the number of active rectangles.

**Why solve it:**

It teaches the most important two-dimensional generalization of sweep line: reduce a geometric union problem to a sequence of one-dimensional coverage queries.

The Skyline Problem, Falling Squares, and Rectangle Area II are a useful progression from maintaining a maximum to maintaining a range-based aggregate. [The DSA Handbook](https://dsa.handbook.academy/patterns/sweep-line-archetypes/?utm_source=chatgpt.com)

---

## Tier 4: Point queries, active intervals, and heaps

These problems are important because they use a sweep or sweep-like processing while maintaining a more specialized active set.

| Order | Problem | Difficulty | Main technique | Classification |
|---:|---|---|---|---|
| 17 | Minimum Interval to Include Each Query | Hard | Offline query sweep + min heap | Core sweep variation |
| 18 | Meeting Rooms III | Hard | Chronological events + two heaps | Sweep-like scheduling |
| 19 | Maximum Number of Events That Can Be Attended | Medium | Sort by start + min heap | Related scheduling pattern |
| 20 | Single-Threaded CPU | Medium | Event processing + priority queue | Related scheduling pattern |
| 21 | Minimum Time to Complete All Tasks | Hard | Greedy scheduling and ordered intervals | Related; not a canonical sweep |
| 22 | Meeting Scheduler | Medium | Two pointers / interval intersection | Usually not sweep line |

### 17. Minimum Interval to Include Each Query — LeetCode 1851

Problem: urlMinimum-Interval-to-Include-Each-Queryhttps://leetcode.com/problems/minimum-interval-to-include-each-query/

- Difficulty: Hard.
- Goal: For each query point, find the shortest interval containing it.
- Core techniques: Offline queries, sorting, and a minimum heap.
- Complexity: `O((n + m) log(n + m))` for `n` intervals and `m` queries.

**The idea:**

1. Sort the queries while remembering their original indices.
2. Process queries in increasing order.
3. Add every interval whose start is at or before the current query.
4. Remove intervals that end before the query.
5. The shortest remaining active interval answers the query.

The heap is ordered by interval length. A query sweep makes it unnecessary to search all intervals independently for every query.

**Classification:** Essential sweep-line-plus-heap problem.

### 18. Meeting Rooms III — LeetCode 2402

Problem: urlMeeting-Rooms-IIIhttps://leetcode.com/problems/meeting-rooms-iii/

- Difficulty: Hard.
- Goal: Assign meetings to rooms according to availability and scheduling rules, then identify the most frequently used room.
- Core techniques: Chronological processing, a min heap of available rooms, and a min heap of occupied rooms.
- Complexity: `O(n log n)` time and `O(n)` space.

**Classification:** Sweep-like scheduling problem rather than a canonical event-delta sweep.

It is useful for learning how event ordering interacts with resource allocation.

### 19. Maximum Number of Events That Can Be Attended — LeetCode 1353

Problem: urlMaximum-Number-of-Events-That-Can-Be-Attendedhttps://leetcode.com/problems/maximum-number-of-events-that-can-be-attended/

- Difficulty: Medium.
- Core technique: Sort by start time and use a minimum heap of end times.
- Goal: Maximize the number of events attended when only one event can be attended per day.
- Complexity: Typically `O(n log n)`.

**Classification:** Related event-processing pattern, but the essential insight is greedy scheduling using the earliest finishing available event.

### 20. Single-Threaded CPU — LeetCode 1834

Problem: urlSingle-Threaded-CPUhttps://leetcode.com/problems/single-threaded-cpu/

- Difficulty: Medium.
- Core technique: Sort tasks by arrival time and use a priority queue for available tasks.
- Goal: Simulate CPU scheduling under specified tie-breaking rules.
- Complexity: `O(n log n)`.

**Classification:** Related scheduling pattern.

Solve this after Meeting Rooms III to compare two-heap resource allocation with priority-based task selection.

### 21. Minimum Time to Complete All Tasks — LeetCode 2589

Problem: urlMinimum-Time-to-Complete-All-Taskshttps://leetcode.com/problems/minimum-time-to-complete-all-tasks/

- Difficulty: Hard.
- Core technique: Greedy scheduling, interval ordering, and efficient tracking of selected time points.
- Goal: Minimize the number of time units needed to complete tasks subject to their required durations and deadlines.
- Complexity: Depends on the chosen algorithm and constraints.

**Classification:** Related interval scheduling, not a foundational sweep-line problem.

### 22. Meeting Scheduler — LeetCode 1229

Problem: urlMeeting-Schedulerhttps://leetcode.com/problems/meeting-scheduler/

- Difficulty: Medium.
- Core technique: Two pointers over sorted availability intervals.
- Goal: Find the earliest common interval with at least the required duration.
- Complexity: `O(n + m)` when both lists are sorted.

**Classification:** Usually not sweep line.

This is an important contrast problem: because both lists are sorted and the goal is a single intersection, two pointers are simpler than creating and sorting a combined event stream.

---

# 5. Category C — Problems solvable with Sweep Line, but better approached another way

You should know these problems, but don't force them into your sweep-line study list as core problems.

| Problem | LeetCode | Preferred technique | Why sweep line is not the default |
|---|---|---|---|
| Merge Intervals | [LC 56](https://leetcode.com/problems/merge-intervals/?utm_source=chatgpt.com) | Sort + merge | The merged result can be built directly in one scan |
| Insert Interval | [LC 57](https://leetcode.com/problems/insert-interval/?utm_source=chatgpt.com) | Three-phase scan | The input is already sorted |
| Meeting Rooms | [LC 252](https://leetcode.com/problems/meeting-rooms/?utm_source=chatgpt.com) | Sort + adjacent overlap check | Only need to determine whether any overlap exists |
| Non-overlapping Intervals | [LC 435](https://leetcode.com/problems/non-overlapping-intervals/?utm_source=chatgpt.com) | Greedy by end time | The problem is fundamentally interval scheduling |
| Minimum Number of Arrows to Burst Balloons | [LC 452](https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/?utm_source=chatgpt.com) | Greedy by end coordinate | The optimal choice is to shoot at the end of the current overlapping group |
| Interval List Intersections | [LC 986](https://leetcode.com/problems/interval-list-intersections/?utm_source=chatgpt.com) | Two pointers | Both input lists are sorted |
| Remove Covered Intervals | [LC 1288](https://leetcode.com/problems/remove-covered-intervals/?utm_source=chatgpt.com) | Sorting + scan | Coverage can be tracked with the farthest endpoint |
| Rectangle Area | [LC 223](https://leetcode.com/problems/rectangle-area/?utm_source=chatgpt.com) | Direct geometry | Only two rectangles are involved |
| Rectangle Overlap | [LC 836](https://leetcode.com/problems/rectangle-overlap/?utm_source=chatgpt.com) | Direct geometry | A constant-time intersection test is enough |
| Maximum Population Year | [LC 1854](https://leetcode.com/problems/maximum-population-year/?utm_source=chatgpt.com) | Difference array | The year range is small and bounded |
| Corporate Flight Bookings | [LC 1109](https://leetcode.com/problems/corporate-flight-bookings/?utm_source=chatgpt.com) | Difference array + prefix sum | The problem is naturally a range-update problem |
| Range Addition | [LC 370](https://leetcode.com/problems/range-addition/?utm_source=chatgpt.com) | Difference array | Range increments are the direct abstraction |
| Check if All the Integers in a Range Are Covered | [LC 1893](https://leetcode.com/problems/check-if-all-the-integers-in-a-range-are-covered/?utm_source=chatgpt.com) | Bounded marking or interval merging | A full event sweep is unnecessary |

### How to use this category

Do not skip these problems altogether. Solve them while studying interval patterns, but learn the natural approach first.

For example:

- **Merge Intervals:** learn sorting and merging.
- **Meeting Rooms II:** learn sweep-line counting.
- **Interval List Intersections:** learn two pointers.
- **My Calendar III:** learn dynamic event deltas.
- **Rectangle Area II:** learn two-dimensional sweep line.

This separation builds algorithm-selection judgment, which is as important as implementation skill in interviews. [Dharmveer Jakhar](https://dharmveerjakhar.in/blog/intervals?utm_source=chatgpt.com)

# 6. Additional problems I strongly recommend

The previous lists cover the main interview-friendly sweep-line patterns. However, if your goal is to become genuinely strong at this technique rather than just solve the standard LeetCode questions, I recommend adding the following problems.

These introduce important variations: counting intervals at query points, processing two-dimensional geometry, maintaining active ranges, and distinguishing sweep line from related offline algorithms.

## Tier 5: Advanced variations and pattern recognition

| Order | Problem | Difficulty | Main concept | Classification |
|---:|---|---|---|---|
| 23 | Count of Range Sum | Hard | Prefix sums + merge sort or ordered data structure | Related, not core sweep |
| 24 | Reverse Pairs | Hard | Merge sort or Fenwick tree | Related, not core sweep |
| 25 | Count of Smaller Numbers After Self | Hard | Merge sort or Fenwick tree | Related, not core sweep |
| 26 | Contains Duplicate III | Hard | Sliding window + ordered set or buckets | Related, not core sweep |
| 27 | Number of Islands II | Hard | Dynamic connectivity using Union-Find | Not sweep line |
| 28 | Range Module | Hard | Dynamic interval coverage | Advanced related pattern |

Problems 23–26 are worth learning because they develop offline processing, coordinate ordering, and efficient queries over changing data. However, their standard optimal solutions are not sweep-line algorithms in the same sense as Meeting Rooms II or The Skyline Problem.

### Problem links and why they matter

**23. Count of Range Sum — LeetCode 327**

urlCount of Range Sumhttps://leetcode.com/problems/count-of-range-sum/

Learn how prefix sums and sorted processing can count subarrays whose sums lie within a specified range.

The standard divide-and-conquer solution uses merge sort and achieves `O(n log n)` time.

**24. Reverse Pairs — LeetCode 493**

urlReverse Pairshttps://leetcode.com/problems/reverse-pairs/

Learn how to count pairs satisfying a relationship such as `nums[i] > 2 * nums[j]`, where `i < j`.

The standard approach uses modified merge sort in `O(n log n)` time.

**25. Count of Smaller Numbers After Self — LeetCode 315**

urlCount of Smaller Numbers After Selfhttps://leetcode.com/problems/count-of-smaller-numbers-after-self/

Learn how to count elements satisfying an ordering condition efficiently using merge sort or a Fenwick tree.

This is especially useful for understanding offline processing and coordinate compression.

**26. Contains Duplicate III — LeetCode 220**

urlContains Duplicate IIIhttps://leetcode.com/problems/contains-duplicate-iii/

Learn how a sliding window combined with an ordered set or bucket structure can solve a problem involving both index distance and value difference.

This is not a canonical sweep-line problem, but it is valuable for distinguishing active-window maintenance from event-based sweeping.

**27. Number of Islands II — LeetCode 305**

urlNumber of Islands IIhttps://leetcode.com/problems/number-of-islands-ii/

Learn how to maintain connected components as cells are added dynamically.

The central technique is Union-Find, not sweep line. Since you're studying DSA patterns, this belongs in your Union-Find preparation rather than your sweep-line core list.

**28. Range Module — LeetCode 715**

urlRange-Modulehttps://leetcode.com/problems/range-module/

This was already included in the dynamic-coverage category. Revisit it after Rectangle Area II to understand the difference between maintaining a static geometric union and dynamically adding or removing covered ranges.

---

# 7. Advanced computational geometry: beyond standard interview sweep line

If you want mastery beyond typical software engineering interview questions, learn these two geometric applications.

They are more common in computational geometry, competitive programming, and specialized algorithmic interviews than in ordinary backend engineering interviews.

## A. Orthogonal segment intersection

**Problem:** Given horizontal and vertical line segments, determine which pairs intersect.

The sweep-line approach is:

1. Sort all segment endpoints by x-coordinate.
2. When a horizontal segment starts, insert its y-coordinate into an ordered structure.
3. When it ends, remove its y-coordinate.
4. When a vertical segment is encountered, query which active horizontal segments have y-coordinates within the vertical segment's range.

This teaches you how to maintain an active ordered set and perform range queries.

For a standard implementation with an ordered structure and output-sensitive reporting, the target complexity is typically `O((n + k) log n)`, where `n` is the number of segments and `k` is the number of reported intersections, subject to the precise reporting requirements.

**Why learn it:** It is a genuine geometric sweep line, unlike many interval problems that are merely sorted scans.

## B. General line-segment intersection

For arbitrary line segments, the sweep must maintain the order of segments intersecting the current vertical line and process intersection events as well as endpoints.

The classic Bentley–Ottmann algorithm reports `k` intersections among `n` segments in approximately `O((n + k) log n)` time under its standard assumptions and appropriate handling of degeneracies.

This is significantly more advanced than the sweep-line problems commonly asked in Google or Meta coding rounds.

**Recommendation:** Learn the orthogonal case first. Only move to general segment intersection if you want computational geometry depth or expect specialized algorithmic interviews.

---

# 8. The master sweep-line template in Java

Before solving the advanced problems, you should understand the reusable Java implementation pattern.

The following template represents intervals as half-open ranges `[start, end)`. This convention means that an interval includes its start but excludes its end.

It is suitable for problems such as meeting-room overlap counting when meetings that touch at an endpoint do not overlap.

```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SweepLineTemplate {

    // Each event contains:
    // time   : coordinate at which something changes
    // delta  : +1 when an interval starts, -1 when it ends
    record Event(int time, int delta) {}

    public static int maximumOverlap(int[][] intervals) {

        List<Event> events = new ArrayList<>();

        // Step 1: Convert each interval into two events.
        for (int[] interval : intervals) {
            int start = interval[0];
            int end = interval[1];

            events.add(new Event(start, +1));
            events.add(new Event(end, -1));
        }

        // Step 2: Sort events by time.
        // For half-open intervals, process end events before start
        // events when they occur at the same time.
        events.sort(
            Comparator.comparingInt(Event::time)
                      .thenComparingInt(Event::delta)
        );

        int active = 0;
        int maximum = 0;

        // Step 3: Process events in sorted order.
        for (Event event : events) {

            // Apply the change to the number of active intervals.
            active += event.delta();

            // Step 4: Track the largest active count.
            maximum = Math.max(maximum, active);
        }

        return maximum;
    }

    public static void main(String[] args) {

        int[][] intervals = {
            {1, 5},
            {2, 6},
            {4, 7},
            {5, 8}
        };

        System.out.println(maximumOverlap(intervals));
        // Output: 3
    }
}
```

### Why does the comparator work?

The comparator sorts by time first and then by delta.

At the same time coordinate:

```text
-1 comes before +1
```

Therefore, an interval ending at time `5` is removed before another interval starting at time `5` is added.

This implements half-open interval semantics.

For closed intervals `[start, end]`, the interpretation differs: two intervals sharing an endpoint overlap at that point. In that case, the event ordering or the way events are aggregated must reflect that definition.

**Important:** The template is only the foundation. It does not directly solve Skyline, Rectangle Area II, or Minimum Interval to Include Each Query. Those problems require a different active-state data structure.

Also, for production-quality code, use appropriate numeric types for coordinates and aggregates. Avoid subtraction-based comparators such as `(a, b) -> a.time() - b.time()`, because subtraction can overflow.

---

# 9. How to select the right sweep-line variant

This is the decision framework I want you to internalize.

| Problem asks... | Maintain... | Representative problem |
|---|---|---|
| Maximum number of overlapping intervals | Integer counter | Meeting Rooms II |
| Whether a capacity is exceeded | Weighted running sum | Car Pooling |
| Maximum active height | Max heap or ordered multiset | The Skyline Problem |
| Minimum active interval length at each query | Min heap with expiration | Minimum Interval to Include Each Query |
| Gaps where no interval is active | Counter and previous coordinate | Employee Free Time |
| Maximum concurrent bookings after each insertion | Ordered event deltas | My Calendar III |
| Total length covered by active intervals | Segment tree with coverage counts | Rectangle Area II |
| Maximum height over a range | Segment tree | Falling Squares |
| Whether intervals conflict with existing bookings | Ordered interval map | My Calendar I |
| Earliest common availability in two sorted lists | Two pointers | Meeting Scheduler |
| Merge all overlapping intervals | Sorted scan | Merge Intervals |
| Apply range updates on a bounded array | Difference array | Corporate Flight Bookings |

The central question is:

**What information must I maintain about the objects that are currently active?**

Once you can answer that question, you can often derive the appropriate data structure.

---

# 10. My recommended final study order

For your goal of preparing for Google, Meta, and senior backend engineering interviews, I would use the following progression.

This is the order I recommend actually solving the problems, rather than a list sorted by difficulty alone.

| Phase | Problems | Goal |
|---|---|---|
| 1. Foundations | 1854, 253, 1094, 2406 | Master event creation, ordering, and overlap counting |
| 2. Weighted sweeps and gaps | 2021, 759, 2251 | Handle weighted coverage, zero-coverage gaps, and point queries |
| 3. Dynamic event processing | 729, 731, 732 | Understand online booking and maintaining event deltas |
| 4. Active-set sweeps | 218, 1851 | Master heaps, expiration, and lazy deletion |
| 5. Two-dimensional sweeps | 699, 850 | Learn coordinate compression and segment trees |
| 6. Pattern discrimination | 56, 57, 252, 435, 452, 986 | Learn when sorting, greedy, or two pointers is better |
| 7. Advanced interval coverage | 715, 2158 | Maintain covered ranges dynamically |
| 8. Scheduling variants | 1353, 1834, 2402, 2589 | Handle event-driven scheduling and priority queues |
| 9. Geometry specialization | Orthogonal segment intersection, Bentley–Ottmann | Explore advanced computational geometry |

### Which problems are non-negotiable?

If you have limited time, prioritize these eight:

1. **Meeting Rooms II (253)** — event sweep and maximum overlap.
2. **Car Pooling (1094)** — weighted events.
3. **Divide Intervals Into Minimum Number of Groups (2406)** — endpoint semantics.
4. **Employee Free Time (759)** — detecting gaps.
5. **The Skyline Problem (218)** — active maximum with a heap.
6. **Minimum Interval to Include Each Query (1851)** — offline queries and a heap.
7. **My Calendar III (732)** — dynamic event deltas.
8. **Rectangle Area II (850)** — coordinate compression and segment tree.

These eight cover the most important variations of sweep line without spending too much time on related patterns.

## 11. When should you consider yourself proficient?

For each core problem, you should be able to do all of the following:

- Explain why a brute-force solution is inefficient.
- Identify the events and define what each event changes.
- Choose the appropriate active-state data structure.
- Explain what happens when several events share a coordinate.
- Derive the time and space complexities.
- Implement the solution in Java without relying on memorized code.
- Test boundary cases, including touching intervals, nested intervals, identical endpoints, and empty input where applicable.
- Explain why an alternative technique might be simpler.

For the advanced problems, you should additionally be able to explain coordinate compression, lazy deletion, segment-tree coverage counts, and why the maintained state is sufficient to calculate the answer.

**My final recommendation:** Don't try to memorize 30 different solutions. Master the eight core problems, then solve the related interval problems using their natural patterns. This will give you much stronger algorithm-selection skills than treating every problem involving intervals as sweep line.

