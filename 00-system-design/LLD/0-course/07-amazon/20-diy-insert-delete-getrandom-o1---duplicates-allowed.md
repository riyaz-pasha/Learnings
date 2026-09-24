# DIY: Insert Delete GetRandom O(1) - Duplicates Allowed

## Problem statement

Implement a set-like data structure that **allows duplicate values**, supporting the following operations, each in average `O(1)` time:

- `insert(val)`: insert `val`. Returns `false` if `val` already existed in the collection before this insert, `true` otherwise. (The value is inserted either way.)
- `remove(val)`: if `val` is present, remove one occurrence and return `true`. Otherwise return `false`.
- `getRandom()`: return a random element from the collection, weighted by how many times each value currently appears.

### Constraints

- `-2^31 <= val <= 2^31 - 1`
- At most `2 * 10^5` calls total across `insert()`, `remove()`, and `getRandom()`.
- There is always at least one element present whenever `getRandom()` is called.

### Input

```java
insert(1)
insert(1)
insert(2)
getRandom()
remove(1)
remove(1)
insert(1)
insert(2)
insert(3)
getRandom()
remove(2)
remove(2)
```

### Output

```java
true
false
true
1 or 2
true
true
true
false
true
1 or 2 or 3
true
true
```

(`insert` returns `true` only when the value wasn't present *before* this call; `remove` returns `true` whenever an occurrence existed to remove.)

## Coding exercise

Implement the `RandomizedCollection` class with `insert(val)`, `remove(val)`, and `getRandom()`.

This is the exact same pattern as [Feature #3: Upselling Products](03-feature-3-upselling-products.md), extended to a variant where the same product value can be suggested more than once. Keep an `ArrayList` of all values (duplicates included) alongside a `HashMap` from value to the *set* of indices where it appears; removal swaps the target index with the last element's index and patches both entries in the index map.

## Solution

```java
import java.util.*;

class RandomizedCollection {
    // value -> set of positions in `values` holding that value.
    private final Map<Integer, Set<Integer>> indices = new HashMap<>();
    private final List<Integer> values = new ArrayList<>();
    private final Random random = new Random();

    public RandomizedCollection() {
        // Nothing to set up beyond the fields above.
    }

    public boolean insert(int val) {
        boolean isNew = !indices.containsKey(val) || indices.get(val).isEmpty();
        indices.computeIfAbsent(val, k -> new LinkedHashSet<>()).add(values.size());
        values.add(val);
        return isNew;
    }

    public boolean remove(int val) {
        Set<Integer> set = indices.get(val);
        if (set == null || set.isEmpty()) return false;

        // Pick any occurrence of val to remove.
        int idx = set.iterator().next();
        set.remove(idx);

        // Move the last element into the freed slot so the list
        // never needs a shift; patch its index bookkeeping too.
        int lastIdx = values.size() - 1;
        int lastVal = values.get(lastIdx);
        values.set(idx, lastVal);
        if (idx != lastIdx) {
            indices.get(lastVal).remove(lastIdx);
            indices.get(lastVal).add(idx);
        }
        values.remove(lastIdx);
        return true;
    }

    public int getRandom() {
        return values.get(random.nextInt(values.size()));
    }

    public static void main(String[] args) {
        RandomizedCollection c = new RandomizedCollection();
        System.out.println(c.insert(1));               // true
        System.out.println(c.insert(1));                // false
        System.out.println(c.insert(2));                // true
        System.out.println("1 or 2 -> " + c.getRandom());
        System.out.println(c.remove(1));                // true
        System.out.println(c.remove(1));                // true
        System.out.println(c.insert(1));                // true
        System.out.println(c.insert(2));                // false
        System.out.println(c.insert(3));                // true
        System.out.println("1 or 2 or 3 -> " + c.getRandom());
        System.out.println(c.remove(2));                // true
        System.out.println(c.remove(2));                // true
    }
}
```

## Complexity measures

- **Time:** `O(1)` average for `insert`, `remove`, and `getRandom` — hash-map lookups plus a swap-with-last-element removal.
- **Space:** `O(n)` — the values list and the index-set map both grow with the number of elements currently stored.
