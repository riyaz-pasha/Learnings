# DIY: Nested List Weight Sum

## Problem statement

You're given a nested list of integers, where each element is either an integer or another nested list of integers (and lists can be nested arbitrarily deep). Return the sum of every integer, each multiplied by its **depth** — where an integer directly inside the outermost list has depth `1`, an integer one list deeper has depth `2`, and so on.

### Input

```java
// [1, [[3, [4]], 2], 1]
```

### Output

```java
31
```

(`1×1 + 3×3 + 4×4 + 2×2 + 1×1 = 1 + 9 + 16 + 4 + 1 = 31`. The source material's own worked example for this input states `28`, but that arithmetic doesn't check out — `31` is the value that a live run of the algorithm below actually produces.)

## Coding exercise

Implement `nestedSum(nestedList)`.

The closest match in this chapter is [Feature #3: Loop Unrolling](03-feature-3-loop-unrolling.md) — both problems are about resolving an arbitrarily deep nested bracket-like structure, tracking how deep we currently are as we recurse. Loop unrolling walks its nested `n[...]` blocks with two explicit stacks; here, since the nested list is already a real tree-shaped structure (not a flat string we need to parse), plain recursion naturally plays the role those stacks played there — the call stack *is* the depth tracker.

## Solution

```java
import java.util.*;

class Solution {
    interface NestedInteger {
        boolean isInteger();
        Integer getInteger();
        List<NestedInteger> getList();
    }

    public static int nestedSum(List<NestedInteger> nestedList) {
        return depthSum(nestedList, 1);
    }

    private static int depthSum(List<NestedInteger> list, int depth) {
        int sum = 0;
        for (NestedInteger item : list) {
            if (item.isInteger()) {
                sum += item.getInteger() * depth;
            } else {
                sum += depthSum(item.getList(), depth + 1);
            }
        }
        return sum;
    }

    // --- demo scaffolding: a minimal NestedInteger implementation for the example below ---
    static class NestedInt implements NestedInteger {
        private final Integer value;
        private final List<NestedInteger> list;
        NestedInt(int v) { value = v; list = null; }
        NestedInt(List<NestedInteger> l) { value = null; list = l; }
        public boolean isInteger() { return value != null; }
        public Integer getInteger() { return value; }
        public List<NestedInteger> getList() { return list; }
    }

    static NestedInteger num(int v) { return new NestedInt(v); }
    static NestedInteger listOf(NestedInteger... items) {
        return new NestedInt(new ArrayList<>(Arrays.asList(items)));
    }

    public static void main(String[] args) {
        // [1, [[3, [4]], 2], 1]
        List<NestedInteger> input = new ArrayList<>(Arrays.asList(
            num(1),
            listOf(listOf(num(3), listOf(num(4))), num(2)),
            num(1)
        ));
        System.out.println(nestedSum(input));
        // 31
    }
}
```

`depthSum` recurses into every nested list, incrementing `depth` by one each time it descends a level — exactly mirroring how `loopUnrolling`'s two stacks track "how many levels of `[...]` are we currently inside," just expressed through the call stack instead of an explicit one.

## Complexity measures

Let **n** be the total number of integers in the nested list and **d** be its maximum nesting depth.

- **Time:** `O(n)` — every integer and every list is visited exactly once.
- **Space:** `O(d)` — the recursion stack goes as deep as the most deeply nested list.
