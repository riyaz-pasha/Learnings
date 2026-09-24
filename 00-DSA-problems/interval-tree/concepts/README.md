An **Interval Tree** is a specialized binary search tree designed to efficiently store and query intervals (ranges like [start, end]). Think of it like organizing meeting schedules - you want to quickly find which meetings overlap with a given time slot.

## Core Concept

Each node stores:
1. An interval [low, high]
2. The **maximum endpoint** in its subtree (this is the key insight!)
3. Left and right child pointers

The tree is ordered by the **low** (start) values of intervals, but each node also tracks the maximum **high** (end) value in its entire subtree.

## Why Track Maximum Endpoint?

This is the clever part! By storing the maximum endpoint, we can quickly decide whether to search left or right subtrees. If we're looking for overlaps with interval [a, b]:
- If left subtree's max endpoint < a, then NO intervals in the left subtree can overlap with [a, b]
- This lets us prune entire subtrees during search

## Easy Memory Trick

Think of it as a **"Smart Schedule Manager"**:
- **Structure**: BST ordered by meeting start times
- **Smart Info**: Each manager knows the latest end time of all meetings under them
- **Search Strategy**: Skip entire departments if their latest meeting ends before your meeting starts

## Key Operations

**Search for Overlapping Interval:**
```java
// Two intervals [a,b] and [c,d] overlap if: a ≤ d AND c ≤ b
public Interval searchOverlap(Node root, Interval target) {
    if (root == null) return null;
    
    // Check if current interval overlaps
    if (doOverlap(root.interval, target)) {
        return root.interval;
    }
    
    // Decide which subtree to search
    if (root.left != null && root.left.maxEnd >= target.start) {
        return searchOverlap(root.left, target);  // Left might have overlap
    } else {
        return searchOverlap(root.right, target); // Try right
    }
}
```

**Insert Operation:**
```java
public Node insert(Node root, Interval interval) {
    if (root == null) {
        return new Node(interval);
    }
    
    // Update max endpoint
    root.maxEnd = Math.max(root.maxEnd, interval.end);
    
    // Insert based on start time
    if (interval.start < root.interval.start) {
        root.left = insert(root.left, interval);
    } else {
        root.right = insert(root.right, interval);
    }
    
    return root;
}
```

## Complete Implementation## Time Complexity

- **Insert**: O(log n) - same as BST
- **Search**: O(log n) - we eliminate half the search space at each step
- **Find all overlaps**: O(k + log n) where k is the number of overlapping intervals

## Memory Technique: "The Meeting Scheduler"

1. **BST by start time**: "Meetings are sorted by when they begin"
2. **Max endpoint**: "Each manager knows when their department's last meeting ends"  
3. **Smart search**: "Skip departments that finish before your meeting starts"
4. **Overlap check**: "Two meetings clash if one starts before the other ends"

## Common Pitfalls to Avoid

1. **Forgetting to update maxEnd**: Always update when inserting
2. **Wrong overlap condition**: Remember it's `a.start ≤ b.end AND b.start ≤ a.end`
3. **Incorrect search logic**: Check if left subtree's maxEnd ≥ target.start

The key insight is that the maximum endpoint lets you make smart decisions about which subtrees to explore, turning a potentially O(n) search into O(log n).
