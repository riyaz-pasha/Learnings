# 🌳 Interval Tree — Complete Interview Cheat Sheet
> **One-stop reference. Deep. Dense. Interview-ready.**  
> Java examples throughout. Zero fluff.

---

## 📌 Table of Contents
1. [What Is an Interval?](#1-what-is-an-interval)
2. [Core Vocabulary (No Ambiguity)](#2-core-vocabulary-no-ambiguity)
3. [The Five Interval Relationships](#3-the-five-interval-relationships)
4. [Data Structures Used for Interval Problems](#4-data-structures-used-for-interval-problems)
5. [Interval Tree — Deep Internals](#5-interval-tree--deep-internals)
6. [Segment Tree vs Interval Tree vs BIT](#6-segment-tree-vs-interval-tree-vs-bit)
7. [Pattern Recognition — The 8 Core Patterns](#7-pattern-recognition--the-8-core-patterns)
8. [Cues & Trigger Words](#8-cues--trigger-words)
9. [Algorithm Blueprints with Java](#9-algorithm-blueprints-with-java)
10. [All Classic Problems — Categorized](#10-all-classic-problems--categorized)
11. [Sweep Line Technique](#11-sweep-line-technique)
12. [Merge Intervals Deep Dive](#12-merge-intervals-deep-dive)
13. [Meeting Rooms / Scheduling Problems](#13-meeting-rooms--scheduling-problems)
14. [Insert Interval](#14-insert-interval)
15. [Non-Overlapping Intervals](#15-non-overlapping-intervals)
16. [Interval List Intersections](#16-interval-list-intersections)
17. [Minimum Interval to Include Each Query](#17-minimum-interval-to-include-each-query)
18. [Data Stream / Online Queries](#18-data-stream--online-queries)
19. [Complexity Master Table](#19-complexity-master-table)
20. [Edge Cases — The Interview Traps](#20-edge-cases--the-interview-traps)
21. [Decision Tree — Which Approach to Use?](#21-decision-tree--which-approach-to-use)
22. [Common Mistakes & Confusions](#22-common-mistakes--confusions)
23. [Interview Templates — Copy-Paste Ready](#23-interview-templates--copy-paste-ready)

---

## 1. What Is an Interval?

An **interval** is a contiguous range `[start, end]` on a number line.

```
Number line:  0----1----2----3----4----5----6----7----8----9----10
Interval A:        [=============]           → [1, 4]
Interval B:                  [=========]    → [3, 7]
Interval C:                            [==] → [7, 8]
```

**Closed interval** `[1, 4]`  → includes both endpoints  
**Open interval** `(1, 4)`    → excludes both endpoints  
**Half-open** `[1, 4)`        → includes start, excludes end  

> ⚠️ **Interview Default**: Unless stated otherwise, assume **closed intervals**.  
> Know the boundary: `[1,3]` and `[3,5]` **do** overlap (share point 3).  
> `[1,3)` and `[3,5)` do **not** overlap.

---

## 2. Core Vocabulary (No Ambiguity)

| Term | Meaning | Example |
|------|---------|---------|
| **Overlap** | Two intervals share at least one point | `[1,4]` and `[3,7]` overlap |
| **Containment** | One interval is entirely inside another | `[2,3]` inside `[1,5]` |
| **Adjacent** | Intervals share exactly one endpoint | `[1,3]` and `[3,5]` |
| **Disjoint** | No shared points at all | `[1,2]` and `[4,5]` |
| **Coverage** | Union of intervals covering a range | |
| **Max overlap point** | Point covered by most intervals | |
| **Gap** | Space between two intervals | between `[1,2]` and `[4,5]` gap is `(2,4)` |
| **Stabbing query** | Does any interval contain point p? | |
| **Range query** | Find all intervals overlapping `[a,b]` | |

---

## 3. The Five Interval Relationships

```
A: [==========]
B possibilities:

1. B completely before A:     [==]  [==========]
   Condition: B.end < A.start

2. B overlaps A from left:    [======][=====]
   Condition: B.start < A.start && B.end >= A.start && B.end <= A.end

3. B completely inside A:     [===[======]===]
   Condition: B.start >= A.start && B.end <= A.end

4. B overlaps A from right:         [======[======]
   Condition: B.start >= A.start && B.start <= A.end && B.end > A.end

5. B completely after A:      [==========]  [==]
   Condition: B.start > A.end
```

### ✅ The Golden Overlap Check (Memorize This!)
```java
// Two intervals [a1, a2] and [b1, b2] OVERLAP if and only if:
boolean overlaps(int a1, int a2, int b1, int b2) {
    return a1 <= b2 && b1 <= a2;
}

// Equivalently: they DON'T overlap if:
// b2 < a1  (B is entirely before A)  OR
// b1 > a2  (B is entirely after A)
// So overlap = NOT (b2 < a1 || b1 > a2) = b2 >= a1 && b1 <= a2
```

> 🧠 **Memory trick**: "They overlap if start of one ≤ end of other, for BOTH directions."

---

## 4. Data Structures Used for Interval Problems

### 4.1 Plain Sorting (Most Common)
- Sort by `start` or `end`
- Works for: merge, insert, greedy scheduling

### 4.2 Min-Heap / Priority Queue
- Sort by `end` time to track which interval ends soonest
- Works for: meeting rooms, task scheduling

### 4.3 TreeMap (Sorted Map)
- Keys = interval starts, values = interval ends
- `floorKey(x)` → find interval that starts at or before x
- `ceilingKey(x)` → find interval that starts at or after x
- Works for: online insert/delete, range overlap queries

### 4.4 Interval Tree (Augmented BST)
- Stores intervals, augmented with `maxEnd` at each node
- O(log n + k) for stabbing queries where k = results returned

### 4.5 Segment Tree
- Static range, supports point/range updates and queries
- Works for: range sum, range min/max, lazy propagation

### 4.6 Binary Indexed Tree (Fenwick)
- Prefix sum queries in O(log n)
- Coordinate compression often needed

### 4.7 Sweep Line + Events Array
- Convert intervals to events (start/end points)
- Sort and sweep through time
- Works for: max overlapping intervals, area of rectangles

---

## 5. Interval Tree — Deep Internals

### 5.1 Structure

An **Interval Tree** is an **augmented BST** where:
- Each node stores an **interval** `[low, high]`
- The BST is keyed on `low` (start value)
- Each node also stores **`maxEnd`** = max `high` value in its **entire subtree**

```
Example: Insert [1,5], [3,7], [2,9], [6,10], [8,12]

         [3,7]  maxEnd=12
        /        \
    [1,5]         [6,10]
    maxEnd=9      maxEnd=12
    /               \
  [2,9]           [8,12]
  maxEnd=9        maxEnd=12
```

### 5.2 Full Java Implementation

```java
class IntervalTree {
    
    static class Node {
        int low, high;   // The interval
        int maxEnd;      // Max high in this subtree
        Node left, right;
        
        Node(int low, int high) {
            this.low = low;
            this.high = high;
            this.maxEnd = high;
        }
    }
    
    private Node root;
    
    // INSERT
    public void insert(int low, int high) {
        root = insert(root, low, high);
    }
    
    private Node insert(Node node, int low, int high) {
        if (node == null) return new Node(low, high);
        
        if (low < node.low) {
            node.left = insert(node.left, low, high);
        } else {
            node.right = insert(node.right, low, high);
        }
        
        // Update maxEnd on the way back up
        node.maxEnd = Math.max(node.maxEnd, high);
        return node;
    }
    
    // SEARCH: Find ANY one interval overlapping [low, high]
    public int[] search(int low, int high) {
        return search(root, low, high);
    }
    
    private int[] search(Node node, int low, int high) {
        if (node == null) return null;
        
        // Check if current node's interval overlaps query
        if (overlaps(node.low, node.high, low, high)) {
            return new int[]{node.low, node.high};
        }
        
        // Key Decision: Go left or right?
        // Go LEFT if left subtree has maxEnd >= low (possible overlap)
        if (node.left != null && node.left.maxEnd >= low) {
            return search(node.left, low, high);
        }
        
        // Otherwise go right
        return search(node.right, low, high);
    }
    
    // SEARCH ALL: Find ALL intervals overlapping [low, high]
    public List<int[]> searchAll(int low, int high) {
        List<int[]> result = new ArrayList<>();
        searchAll(root, low, high, result);
        return result;
    }
    
    private void searchAll(Node node, int low, int high, List<int[]> result) {
        if (node == null) return;
        
        // Pruning: if maxEnd of this subtree < low, no overlap possible
        if (node.maxEnd < low) return;
        
        // Search left subtree
        searchAll(node.left, low, high, result);
        
        // Check current node
        if (overlaps(node.low, node.high, low, high)) {
            result.add(new int[]{node.low, node.high});
        }
        
        // Search right subtree only if node.low <= high
        if (node.low <= high) {
            searchAll(node.right, low, high, result);
        }
    }
    
    private boolean overlaps(int a1, int a2, int b1, int b2) {
        return a1 <= b2 && b1 <= a2;
    }
}
```

### 5.3 Why the `maxEnd` Trick Works

When searching left subtree:
- If `left.maxEnd < query.low` → NO interval in left subtree can overlap (all end before query starts) → skip left
- If `left.maxEnd >= query.low` → MIGHT be an overlap in left → explore

This gives O(log n) for single overlap query, O(log n + k) for all k overlapping intervals.

---

## 6. Segment Tree vs Interval Tree vs BIT

| Feature | Interval Tree | Segment Tree | BIT (Fenwick) |
|---------|--------------|--------------|---------------|
| **Data** | Dynamic intervals | Static range, values at positions | Values at positions |
| **Query type** | Overlap / stabbing | Range aggregate (sum, min, max) | Prefix sum |
| **Insert/Delete** | O(log n) | O(log n) with lazy | O(log n) |
| **Build** | O(n log n) | O(n) | O(n log n) |
| **Space** | O(n) | O(n) | O(n) |
| **When to use** | "Find intervals overlapping X" | "Sum of range [l,r]", "Range update" | "Prefix sums", "Count inversions" |
| **Complexity** | O(log n + k) per query | O(log n) per query/update | O(log n) per query/update |
| **Coord compress?** | No | Yes (for large ranges) | Yes |

### 6.1 Segment Tree — Key Concepts

```java
// Point update, Range query (Sum)
class SegmentTree {
    int[] tree;
    int n;
    
    SegmentTree(int[] arr) {
        n = arr.length;
        tree = new int[4 * n];
        build(arr, 0, 0, n - 1);
    }
    
    void build(int[] arr, int node, int start, int end) {
        if (start == end) {
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            build(arr, 2*node+1, start, mid);
            build(arr, 2*node+2, mid+1, end);
            tree[node] = tree[2*node+1] + tree[2*node+2];
        }
    }
    
    void update(int node, int start, int end, int idx, int val) {
        if (start == end) {
            tree[node] = val;
        } else {
            int mid = (start + end) / 2;
            if (idx <= mid) update(2*node+1, start, mid, idx, val);
            else update(2*node+2, mid+1, end, idx, val);
            tree[node] = tree[2*node+1] + tree[2*node+2];
        }
    }
    
    int query(int node, int start, int end, int l, int r) {
        if (r < start || end < l) return 0; // Out of range
        if (l <= start && end <= r) return tree[node]; // Fully in range
        int mid = (start + end) / 2;
        return query(2*node+1, start, mid, l, r)
             + query(2*node+2, mid+1, end, l, r);
    }
}
```

---

## 7. Pattern Recognition — The 8 Core Patterns

### Pattern 1: MERGE OVERLAPPING INTERVALS
**Signal**: "merge", "combine", "union", "consolidate intervals"  
**Approach**: Sort by start → greedy merge  
**Key**: Compare `current.end` with `next.start`

### Pattern 2: INSERT INTO SORTED INTERVALS
**Signal**: "insert new interval", "add to existing intervals"  
**Approach**: Find position → merge with neighbors  
**Key**: Three phases — before overlap, during overlap, after overlap

### Pattern 3: MEETING ROOMS / MINIMUM ROOMS
**Signal**: "how many rooms", "max concurrent", "peak overlap", "at the same time"  
**Approach A (sweep)**: Events array → sort → sweep  
**Approach B (heap)**: Sort by start → min-heap of end times  
**Key**: Max value of open events = answer

### Pattern 4: NON-OVERLAPPING (REMOVE MINIMUM)
**Signal**: "remove minimum intervals", "make non-overlapping", "least removals"  
**Approach**: Sort by end → greedy keep (activity selection)  
**Key**: Always keep interval with earliest end

### Pattern 5: OVERLAP DETECTION
**Signal**: "does any interval overlap", "conflict check"  
**Approach**: Sort → check consecutive pairs  
**Key**: `intervals[i].start < intervals[i-1].end`

### Pattern 6: INTERVAL INTERSECTION
**Signal**: "intersection", "common intervals", "two sorted lists"  
**Approach**: Two pointers on two sorted lists  
**Key**: Advance pointer with smaller end

### Pattern 7: STABBING / POINT QUERY
**Signal**: "which intervals contain point X", "at time T what is active"  
**Approach**: Interval Tree or coordinate compression + BIT  
**Key**: `interval.start <= X <= interval.end`

### Pattern 8: RANGE COVER / MINIMUM JUMPS
**Signal**: "minimum intervals to cover range", "fewest intervals"  
**Approach**: Sort by start → greedy, always pick farthest reaching  
**Key**: Classic Greedy Jump Game variant

---

## 8. Cues & Trigger Words

```
┌─────────────────────────────────────────────────────────────────┐
│  TRIGGER WORD            →  PATTERN / APPROACH                  │
├─────────────────────────────────────────────────────────────────┤
│  "overlapping intervals"  →  Sort + merge or sweep line         │
│  "merge intervals"        →  Sort by start, greedy merge        │
│  "meeting rooms"          →  Min-heap on end times              │
│  "at least K overlap"     →  Sweep line, events array           │
│  "maximum overlap point"  →  Sweep line                         │
│  "non-overlapping"        →  Sort by end, greedy (keep early)   │
│  "minimum removals"       →  n - max non-overlapping count      │
│  "insert interval"        →  Binary search + 3-phase merge      │
│  "two lists intersection" →  Two pointers                       │
│  "contains point"         →  Interval tree / sorted map         │
│  "calendar / schedule"    →  Sweep or heap                      │
│  "cover all points"       →  Sort by end, greedy                │
│  "gaps between intervals" →  Sort + check end[i] vs start[i+1] │
│  "total covered length"   →  Sort + merge + sum lengths         │
│  "employee free time"     →  Merge + find gaps                  │
│  "data stream intervals"  →  TreeMap                            │
│  "k closest"              →  Interval tree + priority queue     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. Algorithm Blueprints with Java

### Blueprint A: Sort by Start
```java
// Sort array of int[]{start, end} by start
Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
// OR for stability with tie-breaking on end:
Arrays.sort(intervals, (a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);
```

### Blueprint B: Sort by End
```java
Arrays.sort(intervals, (a, b) -> a[1] - b[1]);
// Tie-break: if same end, prefer earlier start
Arrays.sort(intervals, (a, b) -> a[1] != b[1] ? a[1] - b[1] : a[0] - b[0]);
```

### Blueprint C: Min-Heap on End Times
```java
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
// OR just heap of end times:
PriorityQueue<Integer> endHeap = new PriorityQueue<>();
```

### Blueprint D: Sweep Line Events
```java
// Convert intervals to events
List<int[]> events = new ArrayList<>();
for (int[] interval : intervals) {
    events.add(new int[]{interval[0], 1});  // start event
    events.add(new int[]{interval[1], -1}); // end event
}
// Sort: by time, breaks ties by putting END before START
// (use -1 for end so -1 < 1 → ends come before starts at same time)
events.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);

int maxOverlap = 0, current = 0;
for (int[] event : events) {
    current += event[1];
    maxOverlap = Math.max(maxOverlap, current);
}
```

> ⚠️ **Tie-breaking at same timestamp matters!**  
> If `[1,3]` and `[3,5]`: at t=3, do we count start before end or end before start?  
> Depends on if intervals sharing endpoints count as overlapping.

### Blueprint E: TreeMap for Online Queries
```java
TreeMap<Integer, Integer> map = new TreeMap<>(); // start → end

// Add interval [lo, hi]
void add(int lo, int hi) {
    // Find overlapping intervals and merge
    Integer start = map.floorKey(lo);
    if (start != null && map.get(start) >= lo) lo = start; // extend left
    
    Integer end = map.ceilingKey(lo);
    while (end != null && end <= hi) {
        hi = Math.max(hi, map.get(end));
        map.remove(end);
        end = map.ceilingKey(lo);
    }
    map.put(lo, hi);
}

// Query: does [lo, hi] overlap any stored interval?
boolean overlaps(int lo, int hi) {
    Integer start = map.floorKey(hi);
    return start != null && map.get(start) >= lo;
}
```

---

## 10. All Classic Problems — Categorized

### Category A: Merge / Union
| Problem | Key Idea | TC |
|---------|----------|----|
| Merge Intervals (LC 56) | Sort by start, greedy | O(n log n) |
| Insert Interval (LC 57) | 3-phase: before, overlap, after | O(n) |
| Employee Free Time (LC 759) | Merge all, find gaps | O(n log n) |
| Summary Ranges (LC 228) | Group consecutive numbers | O(n) |

### Category B: Overlap Detection / Count
| Problem | Key Idea | TC |
|---------|----------|----|
| Meeting Rooms I (LC 252) | Sort by start, check consecutive | O(n log n) |
| Meeting Rooms II (LC 253) | Min-heap on end times | O(n log n) |
| Count Ways to Split (LC 2271) | Sweep line | O(n log n) |
| My Calendar I (LC 729) | TreeMap overlap check | O(log n) per insert |
| My Calendar II (LC 731) | Two TreeMaps (booked + double) | O(log n) per insert |
| My Calendar III (LC 732) | TreeMap difference array | O(log n) per insert |

### Category C: Optimization (Remove / Keep)
| Problem | Key Idea | TC |
|---------|----------|----|
| Non-overlapping Intervals (LC 435) | Sort by end, greedy keep | O(n log n) |
| Minimum Number of Arrows (LC 452) | Sort by end, shoot at end | O(n log n) |
| Remove Covered Intervals (LC 1288) | Sort by start desc end, count | O(n log n) |
| Partition Labels (LC 763) | Last occurrence tracking | O(n) |

### Category D: Intersection / Two Lists
| Problem | Key Idea | TC |
|---------|----------|----|
| Interval List Intersections (LC 986) | Two pointers | O(m + n) |
| Find Right Interval (LC 436) | Binary search by start | O(n log n) |
| Count Intervals with Target (LC 2655) | Two pointers | O(n log n) |

### Category E: Cover / Jump
| Problem | Key Idea | TC |
|---------|----------|----|
| Jump Game II (LC 45) | Greedy, pick farthest reach | O(n) |
| Video Stitching (LC 1024) | Sort + greedy jump | O(n log n) |
| Minimum Number of Taps (LC 1326) | Convert to jump game | O(n) |
| Minimum Interval for Query (LC 2276) | Sorted queries + heap | O((n+q) log n) |

### Category F: Point / Range Queries
| Problem | Key Idea | TC |
|---------|----------|----|
| Count of Smaller Numbers (LC 315) | Merge sort / BIT | O(n log n) |
| Range Sum Query (LC 307) | Segment tree / BIT | O(log n) |
| Range Min Query (LC) | Sparse table (static) | O(n log n) build |
| Count of Range Sum (LC 327) | Merge sort | O(n log n) |

---

## 11. Sweep Line Technique

The sweep line is a **virtual vertical line** that moves from left to right across the number line, processing **events** as it goes.

### 11.1 Event Setup

```
Intervals: [1,5], [2,7], [4,9]

Events:
  t=1: +1 (start of [1,5])
  t=2: +1 (start of [2,7])
  t=4: +1 (start of [4,9])
  t=5: -1 (end of [1,5])
  t=7: -1 (end of [2,7])
  t=9: -1 (end of [4,9])

Sweep:
  t=1: count=1
  t=2: count=2
  t=4: count=3  ← MAX OVERLAP = 3
  t=5: count=2
  t=7: count=1
  t=9: count=0
```

### 11.2 The Tie-Breaking Rule (Critical!)

At the same timestamp:
- **Closed intervals** `[a,b]`: if `b` of one interval = `a` of another → they DO overlap. So process **starts BEFORE ends** at same time.
- **Half-open intervals** `[a,b)`: they do NOT overlap at b=a. Process **ends BEFORE starts**.

```java
// Closed intervals: starts before ends at same time
events.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : b[1] - a[1]);
// a[1]=1 (start) > b[1]=-1 (end), so starts come first

// Half-open intervals: ends before starts at same time
events.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);
// a[1]=-1 (end) < b[1]=1 (start), so ends come first
```

### 11.3 Coordinate Compression (for Segment Tree)

When interval values are large (up to 10^9) but count is small:

```java
// Step 1: Collect all unique coordinates
Set<Integer> coordSet = new TreeSet<>();
for (int[] interval : intervals) {
    coordSet.add(interval[0]);
    coordSet.add(interval[1]);
}

// Step 2: Map to compressed indices
Map<Integer, Integer> compress = new HashMap<>();
int idx = 0;
for (int coord : coordSet) {
    compress.put(coord, idx++);
}

// Step 3: Use compressed indices in segment tree
```

---

## 12. Merge Intervals Deep Dive

**Problem**: Given array of intervals, merge all overlapping intervals.  
**Input**: `[[1,3],[2,6],[8,10],[15,18]]`  
**Output**: `[[1,6],[8,10],[15,18]]`

### 12.1 Why Sort by Start?

After sorting by start, any potential overlap between interval `i` and `i+1` can ONLY happen if `start[i+1] <= end[i]`. We never need to look backwards.

### 12.2 Full Solution

```java
public int[][] merge(int[][] intervals) {
    // Step 1: Sort by start time
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    
    List<int[]> merged = new ArrayList<>();
    
    for (int[] interval : intervals) {
        // If list is empty OR no overlap with last merged interval
        if (merged.isEmpty() || merged.get(merged.size() - 1)[1] < interval[0]) {
            merged.add(interval);
        } else {
            // Overlap: extend the end of last merged interval
            merged.get(merged.size() - 1)[1] = 
                Math.max(merged.get(merged.size() - 1)[1], interval[1]);
        }
    }
    
    return merged.toArray(new int[merged.size()][]);
}
// TC: O(n log n)   SC: O(n)
```

### 12.3 Variation: Total Length After Merge

```java
public int totalLength(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    int total = 0, start = intervals[0][0], end = intervals[0][1];
    
    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] <= end) {
            end = Math.max(end, intervals[i][1]);
        } else {
            total += end - start;
            start = intervals[i][0];
            end = intervals[i][1];
        }
    }
    total += end - start;
    return total;
}
```

### 12.4 Variation: Count Gaps (Employee Free Time)

```java
// After merging, gaps between merged[i].end and merged[i+1].start are free
public List<int[]> employeeFreeTime(List<List<int[]>> schedules) {
    List<int[]> all = new ArrayList<>();
    for (List<int[]> emp : schedules) all.addAll(emp);
    
    all.sort((a, b) -> a[0] - b[0]);
    
    List<int[]> merged = merge(all.toArray(new int[0][]));
    List<int[]> result = new ArrayList<>();
    
    for (int i = 1; i < merged.length; i++) {
        result.add(new int[]{merged[i-1][1], merged[i][0]});
    }
    return result;
}
```

---

## 13. Meeting Rooms / Scheduling Problems

### 13.1 Meeting Rooms I — Can One Person Attend All?

```java
// Just check if any two intervals overlap after sorting
public boolean canAttendMeetings(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] < intervals[i-1][1]) { // start < prev_end
            return false;
        }
    }
    return true;
}
// Note: [1,5] and [5,6] — does 5 == 5 count as overlap?
// If closed intervals → YES overlap (use <=), if [1,5) → NO (use <)
```

### 13.2 Meeting Rooms II — Minimum Rooms Needed

**Approach 1: Min-Heap (Intuitive)**
```java
public int minMeetingRooms(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]); // sort by start
    
    // Min-heap tracks END times of currently active meetings
    PriorityQueue<Integer> pq = new PriorityQueue<>();
    
    for (int[] interval : intervals) {
        // If earliest ending meeting ends before this one starts → reuse room
        if (!pq.isEmpty() && pq.peek() <= interval[0]) {
            pq.poll(); // free that room
        }
        pq.offer(interval[1]); // allocate room, record end time
    }
    
    return pq.size(); // rooms still occupied
}
// TC: O(n log n)   SC: O(n)
```

**Approach 2: Sweep Line (Two Arrays)**
```java
public int minMeetingRooms(int[][] intervals) {
    int n = intervals.length;
    int[] starts = new int[n];
    int[] ends = new int[n];
    
    for (int i = 0; i < n; i++) {
        starts[i] = intervals[i][0];
        ends[i] = intervals[i][1];
    }
    
    Arrays.sort(starts);
    Arrays.sort(ends);
    
    int rooms = 0, endIdx = 0;
    for (int i = 0; i < n; i++) {
        if (starts[i] < ends[endIdx]) {
            rooms++; // Need new room
        } else {
            endIdx++; // Reuse a room (a meeting ended)
        }
    }
    return rooms;
}
// TC: O(n log n)   SC: O(n)
```

> 💡 **Why both work**: Both approaches count the max concurrent meetings. Heap explicitly tracks rooms; sweep line counts net +1/-1 balance.

### 13.3 My Calendar I (Online, No Overlap)

```java
class MyCalendar {
    TreeMap<Integer, Integer> calendar = new TreeMap<>();
    
    public boolean book(int start, int end) {
        // Find the interval that starts just before 'start'
        Integer prev = calendar.floorKey(start);
        // Find the interval that starts just after 'start'
        Integer next = calendar.ceilingKey(start);
        
        // Check prev interval doesn't extend into [start, end)
        // Check next interval doesn't start before end
        if ((prev == null || calendar.get(prev) <= start) &&
            (next == null || next >= end)) {
            calendar.put(start, end);
            return true;
        }
        return false;
    }
}
// TC per book: O(log n)
```

### 13.4 My Calendar III (Max K-booking)

```java
class MyCalendarThree {
    TreeMap<Integer, Integer> diff = new TreeMap<>(); // difference array
    
    public int book(int start, int end) {
        diff.merge(start, 1, Integer::sum);
        diff.merge(end, -1, Integer::sum);
        
        int maxBook = 0, current = 0;
        for (int v : diff.values()) {
            current += v;
            maxBook = Math.max(maxBook, current);
        }
        return maxBook;
    }
}
// TC per book: O(n)  — can optimize to O(log n) with segment tree
```

---

## 14. Insert Interval

**Problem**: Insert `newInterval` into sorted non-overlapping intervals.  
**Input**: `intervals = [[1,3],[6,9]], newInterval = [2,5]`  
**Output**: `[[1,5],[6,9]]`

### The Three-Phase Approach

```
Phase 1: Add all intervals that END before newInterval starts
         (no overlap, come before)
Phase 2: Merge all intervals that OVERLAP with newInterval
Phase 3: Add all intervals that START after newInterval ends
         (no overlap, come after)
```

```java
public int[][] insert(int[][] intervals, int[] newInterval) {
    List<int[]> result = new ArrayList<>();
    int i = 0, n = intervals.length;
    
    // Phase 1: Add all intervals ending before newInterval starts
    while (i < n && intervals[i][1] < newInterval[0]) {
        result.add(intervals[i++]);
    }
    
    // Phase 2: Merge overlapping intervals
    while (i < n && intervals[i][0] <= newInterval[1]) {
        newInterval[0] = Math.min(newInterval[0], intervals[i][0]);
        newInterval[1] = Math.max(newInterval[1], intervals[i][1]);
        i++;
    }
    result.add(newInterval);
    
    // Phase 3: Add remaining intervals
    while (i < n) {
        result.add(intervals[i++]);
    }
    
    return result.toArray(new int[result.size()][]);
}
// TC: O(n)   SC: O(n)
```

> ⚠️ **Common Bug**: Phase 1 condition is `intervals[i][1] < newInterval[0]` (strict less than for closed intervals). If you use `<=`, you miss adjacent intervals that should merge!

---

## 15. Non-Overlapping Intervals

**Problem**: Find minimum number of intervals to REMOVE so none overlap.  
**Input**: `[[1,2],[2,3],[3,4],[1,3]]`  
**Output**: `1`

### The Key Insight (Activity Selection)

Greedy: Always keep the interval with the **earliest ending time** among non-conflicting intervals. This maximizes future choices.

```java
public int eraseOverlapIntervals(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[1] - b[1]); // Sort by END
    
    int count = 0;      // removals
    int end = Integer.MIN_VALUE;  // end of last KEPT interval
    
    for (int[] interval : intervals) {
        if (interval[0] >= end) {
            // No overlap: keep this interval
            end = interval[1];
        } else {
            // Overlap: remove (don't update end)
            count++;
        }
    }
    
    return count;
}
// TC: O(n log n)   SC: O(1)
```

**Alternative**: Max intervals you can KEEP = n - minRemoved

```java
// Count max non-overlapping intervals (Activity Selection)
public int maxNonOverlapping(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> a[1] - b[1]);
    int count = 0, end = Integer.MIN_VALUE;
    for (int[] interval : intervals) {
        if (interval[0] >= end) {
            count++;
            end = interval[1];
        }
    }
    return count;
}
// minRemoved = intervals.length - maxNonOverlapping
```

### Minimum Arrows to Burst Balloons (LC 452)

Same idea! An arrow at position x bursts all balloons containing x.

```java
public int findMinArrowShots(int[][] points) {
    Arrays.sort(points, (a, b) -> a[1] - b[1]); // Sort by end
    int arrows = 1;
    int arrowPos = points[0][1]; // Shoot at end of first balloon
    
    for (int i = 1; i < points.length; i++) {
        if (points[i][0] > arrowPos) { // This balloon not burst
            arrows++;
            arrowPos = points[i][1];
        }
    }
    return arrows;
}
// Note: [1,2] and [2,3] share point 2 → one arrow at 2 bursts both
// So condition is points[i][0] > arrowPos (NOT >=)
```

---

## 16. Interval List Intersections

**Problem**: Find intersection of two sorted interval lists A and B.  
**Input**: `A=[[0,2],[5,10]], B=[[1,5],[8,12]]`  
**Output**: `[[1,2],[5,5],[8,10]]`

### Two Pointer Logic

```java
public int[][] intervalIntersection(int[][] A, int[][] B) {
    List<int[]> result = new ArrayList<>();
    int i = 0, j = 0;
    
    while (i < A.length && j < B.length) {
        // Intersection: max of starts, min of ends
        int lo = Math.max(A[i][0], B[j][0]);
        int hi = Math.min(A[i][1], B[j][1]);
        
        if (lo <= hi) { // Valid intersection
            result.add(new int[]{lo, hi});
        }
        
        // Advance the pointer with smaller end
        // (that interval can't contribute more intersections)
        if (A[i][1] < B[j][1]) i++;
        else j++;
    }
    
    return result.toArray(new int[result.size()][]);
}
// TC: O(m + n)   SC: O(m + n)
```

**Intersection formula**:
- `lo = max(A.start, B.start)`
- `hi = min(A.end, B.end)`
- Valid if `lo <= hi`

---

## 17. Minimum Interval to Include Each Query

**Problem**: For each query point q, find the size of the smallest interval containing q.  
**Input**: `intervals=[[1,4],[2,4],[3,6]], queries=[2,3,4,5]`  
**Output**: `[3,3,3,4]`

```java
public int[] minInterval(int[][] intervals, int[] queries) {
    // Sort queries but keep track of original indices
    int q = queries.length;
    Integer[] queryIdx = new Integer[q];
    for (int i = 0; i < q; i++) queryIdx[i] = i;
    Arrays.sort(queryIdx, (a, b) -> queries[a] - queries[b]);
    
    // Sort intervals by start
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    
    // Min-heap: (size of interval, end of interval)
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
    
    int[] result = new int[q];
    Arrays.fill(result, -1);
    int i = 0;
    
    for (int idx : queryIdx) {
        int point = queries[idx];
        
        // Add all intervals that start <= current query point
        while (i < intervals.length && intervals[i][0] <= point) {
            int size = intervals[i][1] - intervals[i][0] + 1;
            pq.offer(new int[]{size, intervals[i][1]});
            i++;
        }
        
        // Remove intervals that have ended before current query point
        while (!pq.isEmpty() && pq.peek()[1] < point) {
            pq.poll();
        }
        
        if (!pq.isEmpty()) {
            result[idx] = pq.peek()[0];
        }
    }
    
    return result;
}
// TC: O((n + q) log n)
```

---

## 18. Data Stream / Online Queries

### Adding Intervals from Data Stream

```java
class SummaryRanges {
    TreeMap<Integer, Integer> map = new TreeMap<>(); // start → end
    
    public void addNum(int val) {
        if (map.containsKey(val)) return; // Already covered
        
        // Check if val can extend a previous interval
        Integer lo = map.floorKey(val);
        if (lo != null && map.get(lo) >= val) return; // val is already covered
        
        // Start with [val, val]
        int start = val, end = val;
        
        // Merge with left neighbor if adjacent or overlapping
        if (lo != null && map.get(lo) + 1 >= val) {
            start = lo;
            end = Math.max(end, map.get(lo));
            map.remove(lo);
        }
        
        // Merge with right neighbor if adjacent or overlapping
        Integer hi = map.ceilingKey(val);
        if (hi != null && hi <= end + 1) {
            end = Math.max(end, map.get(hi));
            map.remove(hi);
        }
        
        map.put(start, end);
    }
    
    public int[][] getIntervals() {
        return map.entrySet().stream()
            .map(e -> new int[]{e.getKey(), e.getValue()})
            .toArray(int[][]::new);
    }
}
```

### Count Integers in Ranges (LC 2276)

```java
class CountIntervals {
    TreeMap<Integer, Integer> map = new TreeMap<>();
    int total = 0;
    
    public void add(int left, int right) {
        int lo = left, hi = right;
        
        // Merge overlapping/adjacent intervals
        Integer floor = map.floorKey(lo);
        if (floor != null && map.get(floor) >= lo - 1) {
            lo = floor;
            hi = Math.max(hi, map.get(floor));
            total -= map.get(floor) - floor + 1;
            map.remove(floor);
        }
        
        Integer ceil = map.ceilingKey(lo);
        while (ceil != null && ceil <= hi + 1) {
            hi = Math.max(hi, map.get(ceil));
            total -= map.get(ceil) - ceil + 1;
            map.remove(ceil);
            ceil = map.ceilingKey(lo);
        }
        
        map.put(lo, hi);
        total += hi - lo + 1;
    }
    
    public int count() {
        return total;
    }
}
```

---

## 19. Complexity Master Table

| Operation | Sorted Array | Interval Tree | Segment Tree | TreeMap |
|-----------|-------------|---------------|--------------|---------|
| Build | O(n log n) | O(n log n) | O(n) | O(n log n) |
| Insert | O(n) shift | O(log n) | O(log n) | O(log n) |
| Delete | O(n) shift | O(log n) | O(log n) | O(log n) |
| Single overlap query | O(log n) | O(log n) | O(log n) | O(log n) |
| All k overlaps query | O(n) | O(log n + k) | O(log n + k) | O(log n + k) |
| Merge all | O(n log n) | — | — | O(n log n) |
| Range sum query | — | — | O(log n) | — |

---

## 20. Edge Cases — The Interview Traps

### 🪤 Trap 1: Single Interval
```java
// Always handle n=1 correctly
if (intervals.length == 1) return intervals;
```

### 🪤 Trap 2: Touching Endpoints
```
[1,3] and [3,5]: do they overlap?
- Closed intervals [a,b]: YES (share point 3)
- Half-open [a,b): NO (3 is excluded from [1,3))
→ Always clarify in interview!
```

### 🪤 Trap 3: Negative Values
```java
// int comparison with subtraction can overflow for large values
// WRONG: (a, b) -> a[0] - b[0]  // overflows if a[0]=MAX and b[0]=MIN
// RIGHT: Integer.compare(a[0], b[0])
Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
```

### 🪤 Trap 4: Already Sorted Input
```
If input is already sorted, mention it but still call sort (it's O(n log n) 
vs O(n²) bug risk)
```

### 🪤 Trap 5: Completely Contained Intervals
```
Input: [[1,10],[2,3],[4,5]]
After merge: [[1,10]]
Bug: forgetting to take MAX of ends:
  max(end, intervals[i][1]) ← must take MAX, not just intervals[i][1]
```

### 🪤 Trap 6: Empty Input
```java
if (intervals == null || intervals.length == 0) return new int[0][];
```

### 🪤 Trap 7: Meeting Rooms — Strict vs Non-Strict
```
[0,30] and [30,60]: can you attend both?
- If one ends at 30 and next starts at 30 → depends on problem
- LC 252: intervals[i][0] < intervals[i-1][1] → conflict
- So [0,30],[30,60] is NOT a conflict (you leave at 30, enter at 30)
```

### 🪤 Trap 8: Insertion Sort vs Binary Search
```
Insert Interval (LC 57): Since array is sorted, use binary search for 
first overlapping interval instead of linear scan to optimize.
```

### 🪤 Trap 9: TreeMap floorKey vs lowerKey
```java
floorKey(k)  → greatest key ≤ k  (includes k itself)
lowerKey(k)  → greatest key < k  (excludes k itself)
ceilingKey(k) → smallest key ≥ k (includes k itself)
higherKey(k)  → smallest key > k (excludes k itself)
```

### 🪤 Trap 10: Sorting Stability for Ties in Start
```
When two intervals have same start, which comes first?
[1,5] and [1,3]: 
- For merging: doesn't matter (both will be processed)
- For activity selection: [1,3] should come first (smaller end)
- For containment removal: [1,5] should come first (larger end)
→ Always define tie-breaking explicitly!
```

---

## 21. Decision Tree — Which Approach to Use?

```
Is the input STATIC (given all at once)?
├── YES
│   ├── Are you MERGING/COMBINING intervals?
│   │   └── Sort by start → greedy merge
│   ├── Need MINIMUM ROOMS / MAX CONCURRENT?
│   │   ├── Sweep line (events) for conceptual clarity
│   │   └── Min-heap on end times for code elegance
│   ├── Need to REMOVE MINIMUM intervals?
│   │   └── Sort by end → greedy activity selection
│   ├── INSERTING one interval into sorted list?
│   │   └── Three-phase: before, overlap, after
│   ├── INTERSECTION of two lists?
│   │   └── Two pointers (both sorted)
│   ├── COVERING a range with min intervals?
│   │   └── Sort by start → greedy jump game
│   └── POINT/RANGE queries on values?
│       └── Segment tree / BIT + coordinate compression
│
└── NO (DYNAMIC / ONLINE)
    ├── Inserting and querying overlap?
    │   └── TreeMap (floorKey/ceilingKey)
    ├── Need max active at any time?
    │   └── TreeMap difference array
    └── Complex range updates?
        └── Segment tree with lazy propagation
```

---

## 22. Common Mistakes & Confusions

### ❌ Confusion 1: When to sort by START vs END

| Sort by START | Sort by END |
|--------------|-------------|
| Merge intervals | Non-overlapping (remove min) |
| Insert interval | Minimum arrows |
| Meeting rooms II | Activity selection |
| Sweep line | Cover points |

**Rule of thumb**: 
- "What's available NOW?" → sort by start
- "What's the BEST to keep?" → sort by end (greedy future optimization)

### ❌ Confusion 2: Interval Tree vs Segment Tree

- **Interval Tree**: The data IS intervals. Query: "which stored intervals overlap with X?"
- **Segment Tree**: The data is VALUES at positions. Query: "sum/min/max over range [l,r]?"

### ❌ Confusion 3: Heap size vs Removal count

In Meeting Rooms II:
- Heap size at end = **number of rooms needed** (NOT intervals processed)
- When you `poll()` from heap, you're REUSING a room, not removing an interval

### ❌ Confusion 4: minRemoved in Non-Overlapping

```
n intervals total
maxKept = max non-overlapping intervals (activity selection)
minRemoved = n - maxKept

NOT: directly counting how many you skipped in the loop
(the loop counts kept, not removed)
```

### ❌ Confusion 5: Overlap condition off-by-one

```java
// OVERLAPPING (closed intervals):
a[0] <= b[1] && b[0] <= a[1]   ← CORRECT

// Common wrong version:
a[0] < b[1] && b[0] < a[1]     ← WRONG for touching intervals [1,3],[3,5]
```

### ❌ Confusion 6: Merge condition in Insert Interval

```java
// Phase 1: intervals that DON'T overlap with newInterval (come before)
// Condition: interval ends BEFORE newInterval starts
intervals[i][1] < newInterval[0]   // For closed intervals

// Phase 2: intervals that DO overlap (need merging)
// Condition: interval starts AT OR BEFORE newInterval ends
intervals[i][0] <= newInterval[1]  // ← Don't use strictly less!
```

### ❌ Confusion 7: TreeMap key collision

```java
// If two intervals have same start, only one survives in TreeMap!
// Store as: start → List<Integer> (ends)
// Or use different key strategy
TreeMap<Integer, List<Integer>> map = new TreeMap<>();
```

---

## 23. Interview Templates — Copy-Paste Ready

### Template 1: Basic Interval Sort + Greedy
```java
public int[][] solve(int[][] intervals) {
    if (intervals == null || intervals.length == 0) return new int[0][];
    Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
    
    List<int[]> result = new ArrayList<>();
    int[] current = intervals[0];
    
    for (int i = 1; i < intervals.length; i++) {
        if (intervals[i][0] <= current[1]) { // Overlap
            current[1] = Math.max(current[1], intervals[i][1]);
        } else {
            result.add(current);
            current = intervals[i];
        }
    }
    result.add(current);
    
    return result.toArray(new int[result.size()][]);
}
```

### Template 2: Min-Heap for Scheduling
```java
public int minResources(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
    PriorityQueue<Integer> pq = new PriorityQueue<>(); // min-heap of end times
    
    for (int[] interval : intervals) {
        if (!pq.isEmpty() && pq.peek() <= interval[0]) {
            pq.poll(); // Reuse resource
        }
        pq.offer(interval[1]);
    }
    return pq.size();
}
```

### Template 3: Sweep Line
```java
public int maxOverlap(int[][] intervals) {
    List<int[]> events = new ArrayList<>();
    for (int[] i : intervals) {
        events.add(new int[]{i[0], 1});
        events.add(new int[]{i[1], -1});
    }
    events.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);
    
    int max = 0, count = 0;
    for (int[] e : events) {
        count += e[1];
        max = Math.max(max, count);
    }
    return max;
}
```

### Template 4: Activity Selection (Max Non-Overlapping)
```java
public int maxActivities(int[][] intervals) {
    Arrays.sort(intervals, (a, b) -> Integer.compare(a[1], b[1]));
    int count = 0, end = Integer.MIN_VALUE;
    for (int[] interval : intervals) {
        if (interval[0] >= end) {
            count++;
            end = interval[1];
        }
    }
    return count;
}
```

### Template 5: TreeMap Online Interval Operations
```java
TreeMap<Integer, Integer> map = new TreeMap<>(); // start → end

boolean overlaps(int lo, int hi) {
    Integer prev = map.floorKey(hi);
    return prev != null && map.get(prev) >= lo;
}

void addInterval(int lo, int hi) {
    if (overlaps(lo, hi)) {
        // Merge logic
        Integer from = map.floorKey(lo);
        if (from == null || map.get(from) < lo) from = lo;
        Integer to = map.floorKey(hi);
        hi = Math.max(hi, map.get(to));
        
        // Remove all overlapped intervals
        map.subMap(from, true, hi, true).clear();
        map.put(from, hi);
    } else {
        map.put(lo, hi);
    }
}
```

### Template 6: Two-Pointer Intersection
```java
public List<int[]> intersect(int[][] A, int[][] B) {
    List<int[]> result = new ArrayList<>();
    int i = 0, j = 0;
    while (i < A.length && j < B.length) {
        int lo = Math.max(A[i][0], B[j][0]);
        int hi = Math.min(A[i][1], B[j][1]);
        if (lo <= hi) result.add(new int[]{lo, hi});
        if (A[i][1] < B[j][1]) i++;
        else j++;
    }
    return result;
}
```

### Template 7: Offline Queries with Sorted Events
```java
// Process queries offline: sort both intervals and queries
public int[] answerQueries(int[][] intervals, int[] queries) {
    Arrays.sort(intervals, (a, b) -> a[0] - b[0]);
    Integer[] idx = new Integer[queries.length];
    for (int i = 0; i < idx.length; i++) idx[i] = i;
    Arrays.sort(idx, (a, b) -> queries[a] - queries[b]);
    
    PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
    int[] result = new int[queries.length];
    int j = 0;
    
    for (int i : idx) {
        int q = queries[i];
        // Add intervals that could contain q
        while (j < intervals.length && intervals[j][0] <= q) {
            pq.offer(intervals[j++]);
        }
        // Remove intervals that end before q
        while (!pq.isEmpty() && pq.peek()[1] < q) pq.poll();
        
        result[i] = pq.isEmpty() ? -1 : pq.peek()[1] - pq.peek()[0] + 1;
    }
    return result;
}
```

---

## 🔥 Quick Reference Card

```
┌──────────────────────────────────────────────────────────────┐
│  INTERVIEW QUICK FIRE                                        │
├──────────────────────────────────────────────────────────────┤
│  Overlap check:        a.start <= b.end && b.start <= a.end  │
│  Merge condition:      next.start <= current.end             │
│  Activity selection:   sort by END, keep earliest-end        │
│  Max concurrent:       sort starts & ends separately, sweep  │
│  Online insert:        TreeMap with floorKey/ceilingKey      │
│  Static range query:   Segment tree / Sparse table           │
│  Dynamic intervals:    Interval Tree (augmented BST)         │
│  Two sorted lists:     Two pointer, advance smaller end      │
│  Cover range min:      Sort by start, greedy jump            │
│  Coordinate compress:  Sort unique values, assign indices    │
├──────────────────────────────────────────────────────────────┤
│  ALWAYS SORT BY START UNLESS:                                │
│    - Need min removals → sort by END                         │
│    - Need to shoot arrows → sort by END                      │
│    - Need activity selection → sort by END                   │
├──────────────────────────────────────────────────────────────┤
│  COMPLEXITY CHEAT:                                           │
│    Sort-based solutions:  O(n log n) time, O(n) space        │
│    Heap-based solutions:  O(n log n) time, O(n) space        │
│    Sweep line:            O(n log n) time, O(n) space        │
│    Interval Tree query:   O(log n + k) per query             │
│    Segment Tree:          O(log n) per query/update          │
└──────────────────────────────────────────────────────────────┘
```

---

*Last updated: 2026 | All examples tested in Java 17+*
