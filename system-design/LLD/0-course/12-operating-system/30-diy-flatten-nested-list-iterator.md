# DIY: Flatten Nested List Iterator

## Problem statement

Given a nested list of integers `nestedList`, where each element is either an integer or another nested list, implement `NestedIterator` with:

- `NestedIterator(List<NestedInteger> nestedList)` — initializes the iterator.
- `boolean hasNext()` — `true` if there's at least one more integer.
- `int next()` — returns the next integer.

### Input

```java
nestedList = [[1, 2], 3, [4, 5]]
```

### Output

```java
// Calling next() repeatedly until hasNext() is false:
1
2
3
4
5
```

## Coding exercise

Implement `NestedIterator`.

This is the exact same pattern as [Feature #11: Directory Iterator](11-feature-11-directory-iterator.md) — there, the OS needed to iterate over every file in a nested directory structure; here it's the bare pattern with plain integers instead of file names. The approach is identical: a stack holding the not-yet-flattened structure, unwrapped lazily — pushed in reverse order so the first element ends up on top, and any nested list found at the top gets popped and replaced by its own contents (again reversed) until an integer surfaces.

## Solution

```java
import java.util.*;

interface NestedInteger {
    boolean isInteger();
    Integer getInteger();
    List<NestedInteger> getList();
}

class NestedIterator {
    private final Deque<NestedInteger> stack;

    public NestedIterator(List<NestedInteger> nestedList) {
        stack = new ArrayDeque<>();
        for (int i = nestedList.size() - 1; i >= 0; i--) {
            stack.push(nestedList.get(i));
        }
    }

    public Integer next() {
        hasNext(); // Ensures the top of the stack is unwrapped down to an integer.
        return stack.pop().getInteger();
    }

    public boolean hasNext() {
        while (!stack.isEmpty()) {
            NestedInteger top = stack.peek();
            if (top.isInteger()) return true;
            stack.pop();
            List<NestedInteger> nested = top.getList();
            for (int i = nested.size() - 1; i >= 0; i--) {
                stack.push(nested.get(i));
            }
        }
        return false;
    }
}
```

The constructor pushes the top-level elements onto a stack in reverse order, so popping happens in the correct left-to-right sequence. `hasNext()` does the actual flattening: whenever the top of the stack is a nested list rather than an integer, it's popped and replaced by its own elements (again reversed), repeating until either an integer surfaces or the stack empties. `next()` just calls `hasNext()` first to guarantee that unwrapping, then pops the integer sitting on top.

## Complexity measures

Let **n** be the total count of integers and **l** be the total count of nested lists across the whole structure.

- **Time:** `O(n + l)` overall across the iterator's lifetime — every integer is pushed/popped once, and every nested list is unwrapped exactly once.
- **Space:** `O(n + l)` — the stack can hold as many entries as the most deeply-unwrapped point of the structure.
