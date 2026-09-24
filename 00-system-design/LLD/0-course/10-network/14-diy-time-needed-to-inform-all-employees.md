# DIY: Time Needed to Inform All Employees

## Problem statement

A company has `n` employees, uniquely numbered from `0` to `n - 1`. The ID of the head of the company is `headID`. Each employee has exactly one direct manager, given by the `manager` array, where `manager[i]` is the ID of employee `i`'s manager (`manager[headID] = -1`). The reporting structure is guaranteed to form a tree.

The head wants to inform the whole company of urgent news. The head informs their direct subordinates, who inform their own subordinates, and so on — an employee can only relay the message to their immediate subordinates. Employee `i` takes `informTime[i]` minutes to inform all of their direct subordinates (after which those subordinates can start informing theirs). Return the total number of minutes needed to inform every employee.

### Input

```java
n = 6
headID = 2
manager = {2, 2, -1, 2, 2, 2}
informTime = {0, 0, 1, 0, 0, 0}
```

### Output

```
1
```

The head (employee 2) directly manages every other employee and takes 1 minute to inform them all.

## Coding exercise

Implement `numOfMinutes(n, headID, manager, informTime)`, returning the number of minutes needed to inform every employee.

This is the exact same pattern as [Feature #1: Total Time](01-feature-1-total-time.md) — there, we summed delays down a spanning tree to find when the last client received a broadcast; here it's the bare pattern, no networking story. Flip the manager (parent) pointers into a children map, then find the longest root-to-leaf sum of `informTime` values.

## Solution

```java
import java.util.*;

class Solution {
    public static int numOfMinutes(int n, int headID, int[] manager, int[] informTime) {
        Map<Integer, List<Integer>> children = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (manager[i] != -1) {
                children.computeIfAbsent(manager[i], k -> new ArrayList<>()).add(i);
            }
        }
        return dfs(headID, children, informTime);
    }

    private static int dfs(int id, Map<Integer, List<Integer>> children, int[] informTime) {
        if (!children.containsKey(id)) {
            return 0; // a leaf employee has no one left to inform
        }
        int maxSubordinateTime = 0;
        for (int subordinate : children.get(id)) {
            maxSubordinateTime = Math.max(maxSubordinateTime, dfs(subordinate, children, informTime));
        }
        return informTime[id] + maxSubordinateTime;
    }

    public static void main(String[] args) {
        int n = 6, headID = 2;
        int[] manager = {2, 2, -1, 2, 2, 2};
        int[] informTime = {0, 0, 1, 0, 0, 0};
        System.out.println(numOfMinutes(n, headID, manager, informTime));
        // 1
    }
}
```

## Complexity measures

Let **n** be the number of employees.

- **Time:** `O(n)` — building the children map and running the DFS each visit every employee once.
- **Space:** `O(n)` — the children map holds n - 1 entries, and the recursion stack can be as deep as the tree's height, up to n in the worst case.
