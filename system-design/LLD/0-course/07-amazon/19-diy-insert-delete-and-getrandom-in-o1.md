# DIY: Insert Delete and GetRandom in O(1)

## Problem statement

Implement a set data structure supporting the following operations, each in average `O(1)` time:

- `insert(data)`: insert `data` into the set if it isn't already present. Returns `false` if `data` already exists, `true` otherwise.
- `remove(data)`: if `data` is present, remove it and return `true`. Otherwise return `false`.
- `getRandomData()`: return a random element from the set, each element equally likely.

### Input

```java
insert(20)
getRandomData()
remove(20)
```

### Output

```java
true
20
true
```

(With only one element in the set, `getRandomData()` has just one possible answer.)

## Coding exercise

Implement the `RandomSet` class with `insert(data)`, `remove(data)`, and `getRandomData()`.

This is the exact same pattern as [Feature #3: Upselling Products](03-feature-3-upselling-products.md) — there, Amazon needed to insert, remove, and randomly suggest upsell candidates all in constant time; here it's the bare pattern with no story attached. Pair a `HashMap` (value to index) with an `ArrayList` (index to value); deletion swaps the removed element with the last one before popping, so both the map and the list stay `O(1)`.

## Solution

```java
import java.util.*;

class RandomSet {
    private final Map<Integer, Integer> indexOf = new HashMap<>();
    private final List<Integer> values = new ArrayList<>();
    private final Random random = new Random();

    public RandomSet() {
        // Nothing to set up beyond the fields above.
    }

    public boolean insert(int val) {
        if (indexOf.containsKey(val)) return false;
        indexOf.put(val, values.size());
        values.add(val);
        return true;
    }

    public boolean remove(int val) {
        if (!indexOf.containsKey(val)) return false;

        // Move the last element into the removed slot so both
        // the list and the index map stay O(1) to update.
        int idx = indexOf.get(val);
        int last = values.get(values.size() - 1);
        values.set(idx, last);
        indexOf.put(last, idx);

        values.remove(values.size() - 1);
        indexOf.remove(val);
        return true;
    }

    public int getRandomData() {
        return values.get(random.nextInt(values.size()));
    }

    public static void main(String[] args) {
        RandomSet set = new RandomSet();
        System.out.println(set.insert(20));       // true
        System.out.println(set.getRandomData());  // 20
        System.out.println(set.remove(20));       // true
    }
}
```

## Complexity measures

- **Time:** `O(1)` average for `insert`, `remove`, and `getRandomData` — hash-map lookups plus a swap-with-last-element removal, no shifting.
- **Space:** `O(n)` — the map and list each hold up to n elements.
