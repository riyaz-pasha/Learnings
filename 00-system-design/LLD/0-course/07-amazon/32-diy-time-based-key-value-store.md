# DIY: Time-Based Key-Value Store

## Problem statement

Design a data structure that stores multiple values for the same key at different timestamps and can retrieve the value that was current as of a given timestamp.

Implement `set(key, value, timestamp)`, which stores `value` for `key` at the given `timestamp`, and `get(key, timestamp)`, which returns the value that was set at the largest timestamp `<= timestamp`. If multiple values qualify, return the one at the largest such timestamp. If none qualify, return `""`. Timestamps passed to `set` for a given key are strictly increasing.

### Input

```java
set("price", "10", 1)
set("price", "15", 5)
get("price", 3)
get("price", 5)
get("price", 10)
get("price", 0)
```

### Output

```java
"10"
"15"
"15"
""
```

(At timestamp `3`, the most recent `set` at or before it was `("10", 1)`. At timestamp `10`, it's still `("15", 5)` since nothing was set after that. At timestamp `0`, nothing qualifies yet, so the result is `""`.)

## Coding exercise

Implement the `set` and `get` functions.

This is the exact same pattern as [Feature #13: Time-Based Item Price Store](13-feature-13-time-based-item-price-store.md) — there, Amazon needed to track an item's price history and look up its price as of any past moment; here it's the bare pattern with no story attached. Store each key's `(timestamp, value)` pairs in an append-only list (timestamps arrive in increasing order, so the list is already sorted). Then `get` becomes a binary search for the rightmost timestamp that is `<=` the query timestamp.

## Solution

```java
import java.util.*;

class Solution {
    static class Entry {
        int timestamp;
        String value;
        Entry(int timestamp, String value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    private final Map<String, List<Entry>> store = new HashMap<>();

    public void set(String key, String value, int timestamp) {
        // Timestamps arrive in increasing order per key, so this list stays sorted.
        store.computeIfAbsent(key, k -> new ArrayList<>()).add(new Entry(timestamp, value));
    }

    public String get(String key, int timestamp) {
        List<Entry> entries = store.get(key);
        if (entries == null) {
            return "";
        }

        // Binary search for the rightmost entry with timestamp <= the query.
        int lo = 0, hi = entries.size() - 1, resultIndex = -1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (entries.get(mid).timestamp <= timestamp) {
                resultIndex = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return resultIndex == -1 ? "" : entries.get(resultIndex).value;
    }

    public static void main(String[] args) {
        Solution timeMap = new Solution();
        timeMap.set("price", "10", 1);
        timeMap.set("price", "15", 5);

        System.out.println(timeMap.get("price", 3));  // 10
        System.out.println(timeMap.get("price", 5));  // 15
        System.out.println(timeMap.get("price", 10)); // 15
        System.out.println(timeMap.get("price", 0));  // ""
    }
}
```

## Complexity measures

Let **n** be the number of values stored for a given key.

- **Time:** `O(1)` amortized for `set` (append to a list); `O(log n)` for `get` (binary search).
- **Space:** `O(n)` per key — every `set` call adds one entry that's kept for future lookups.
