This is a classic **frequency counting + sorting/bucketing** problem.

Because the string contains only:

* `a-z` → 26
* `A-Z` → 26
* `0-9` → 10

there are only **62 possible characters**. That makes a frequency-array solution especially clean.

---

# Approach 1 — Frequency Array + Sorting

### Interview thought process

First ask:

> "I need characters ordered by frequency. What information do I need?"

I only need to know:

```text
character -> frequency
```

Since there are only 62 possible characters, we can count them in an array.

Then:

1. Count frequency of every character.
2. Put the characters that actually occur into a list.
3. Sort that list by frequency descending.
4. Append each character `frequency` times.

### Java 24

```java
import java.util.*;

class Solution {

    public String frequencySort(String s) {

        // There are only 62 possible characters:
        // 26 lowercase + 26 uppercase + 10 digits.
        //
        // We can directly map every character to an index.
        int[] frequency = new int[128];

        // Step 1: Count frequency of every character.
        for (char ch : s.toCharArray()) {
            frequency[ch]++;
        }

        // Step 2: Store only the characters that actually appear.
        List<Character> characters = new ArrayList<>();

        for (char ch : s.toCharArray()) {
            if (frequency[ch] > 0) {
                characters.add(ch);

                // Mark it as already added so that we don't add
                // the same character multiple times.
                frequency[ch] = -frequency[ch];
            }
        }

        // Restore the frequencies because we temporarily made them negative.
        for (char ch : characters) {
            frequency[ch] = -frequency[ch];
        }

        // Step 3: Sort characters by frequency descending.
        characters.sort((a, b) ->
                Integer.compare(frequency[b], frequency[a])
        );

        // Step 4: Build the answer.
        StringBuilder result = new StringBuilder(s.length());

        for (char ch : characters) {
            for (int i = 0; i < frequency[ch]; i++) {
                result.append(ch);
            }
        }

        return result.toString();
    }
}
```

### Complexity

Let:

* `n = s.length()`
* `k = number of distinct characters`

Since `k ≤ 62`:

* Frequency counting: `O(n)`
* Sorting distinct characters: `O(k log k)`
* Building result: `O(n)`

Therefore:

```text
Time:  O(n + k log k)
Space: O(k)
```

Since `k ≤ 62`, practically this is:

```text
Time:  O(n)
Space: O(1)
```

---

# Approach 2 — Frequency Array + Bucket Sort ⭐

This is actually the **best approach for this problem**.

The key observation is:

> Frequencies can range from `1` to `n`.

So instead of sorting characters, we can create:

```text
bucket[frequency] = characters having this frequency
```

For example:

```text
s = "tree"

frequency:

t -> 1
r -> 1
e -> 2
```

Buckets:

```text
bucket[2] -> [e]
bucket[1] -> [t, r]
```

Then simply iterate from `n` down to `1`.

### Java 24

```java
import java.util.*;

class Solution {

    public String frequencySort(String s) {

        // Frequency of each ASCII character.
        // The problem guarantees lowercase, uppercase and digits.
        int[] frequency = new int[128];

        // Step 1: Count frequency of every character.
        for (char ch : s.toCharArray()) {
            frequency[ch]++;
        }

        /*
         * bucket[f] contains all characters that appear exactly f times.
         *
         * Maximum possible frequency is s.length().
         *
         * Example:
         *
         * s = "tree"
         *
         * frequency:
         * e -> 2
         * t -> 1
         * r -> 1
         *
         * bucket[2] -> [e]
         * bucket[1] -> [t, r]
         */
        List<Character>[] buckets = new ArrayList[s.length() + 1];

        // Step 2: Put every character into its frequency bucket.
        for (char ch = 0; ch < frequency.length; ch++) {

            int freq = frequency[ch];

            if (freq == 0) {
                continue;
            }

            if (buckets[freq] == null) {
                buckets[freq] = new ArrayList<>();
            }

            buckets[freq].add(ch);
        }

        // Step 3: Process frequencies from highest to lowest.
        StringBuilder result = new StringBuilder(s.length());

        for (int freq = s.length(); freq >= 1; freq--) {

            if (buckets[freq] == null) {
                continue;
            }

            // Every character in this bucket has the same frequency.
            // Their relative order does not matter.
            for (char ch : buckets[freq]) {

                for (int count = 0; count < freq; count++) {
                    result.append(ch);
                }
            }
        }

        return result.toString();
    }
}
```

### Complexity

We scan the string once:

```text
O(n)
```

Then we scan frequencies from `n` down to `1`:

```text
O(n)
```

And we append exactly `n` characters:

```text
O(n)
```

Therefore:

```text
Time:  O(n)
Space: O(n)
```

The `O(n)` space comes from the buckets.

However, because there are only **62 distinct characters**, we can make the bucket structure more memory-efficient.

---

# Approach 3 — Frequency Array + Frequency-Ordered Character List ⭐⭐

Because there are only **62 possible characters**, we don't actually need `n` buckets.

We can simply sort the at-most-62 characters.

This is probably the **cleanest interview solution**.

Even better, we don't need to scan the entire string again to find distinct characters—we can scan the 128-sized frequency array.

```java
import java.util.*;

class Solution {

    public String frequencySort(String s) {

        // Count how many times each character occurs.
        int[] frequency = new int[128];

        for (char ch : s.toCharArray()) {
            frequency[ch]++;
        }

        /*
         * There are only 62 possible characters:
         *
         * lowercase letters = 26
         * uppercase letters = 26
         * digits            = 10
         *
         * So at most 62 characters will be placed in this list.
         */
        List<Character> characters = new ArrayList<>();

        for (char ch = 0; ch < frequency.length; ch++) {
            if (frequency[ch] > 0) {
                characters.add(ch);
            }
        }

        /*
         * Sort characters by frequency in descending order.
         *
         * Example:
         *
         * a -> 3
         * b -> 1
         * c -> 5
         *
         * becomes:
         *
         * c, a, b
         */
        characters.sort(
                (a, b) -> Integer.compare(frequency[b], frequency[a])
        );

        // Build the final string.
        StringBuilder result = new StringBuilder(s.length());

        for (char ch : characters) {

            // Append the character frequency[ch] times.
            for (int i = 0; i < frequency[ch]; i++) {
                result.append(ch);
            }
        }

        return result.toString();
    }
}
```

### Complexity

Let `k` be the number of distinct characters.

Because:

```text
k <= 62
```

we get:

```text
Counting:       O(n)
Sorting:        O(k log k)
Building:       O(n)
```

Overall:

```text
Time:  O(n + k log k)
Space: O(k)
```

And because `k ≤ 62`:

```text
Time:  O(n)
Space: O(1)
```

**This is the solution I would use in an interview.**

---

# Approach 4 — HashMap + PriorityQueue

This is the more **general-purpose** solution.

If the problem didn't restrict the characters to only 62 possibilities, this approach would be useful.

### Idea

Store:

```text
character -> frequency
```

Then put entries into a **max heap** ordered by frequency.

```text
frequency
    ↓
max heap
    ↓
highest frequency character first
```

### Java 24

```java
import java.util.*;

class Solution {

    public String frequencySort(String s) {

        // Step 1: Count frequency using a HashMap.
        Map<Character, Integer> frequency = new HashMap<>();

        for (char ch : s.toCharArray()) {
            frequency.merge(ch, 1, Integer::sum);
        }

        /*
         * Step 2: Create a max heap.
         *
         * The character with the highest frequency should
         * come out first.
         */
        PriorityQueue<Map.Entry<Character, Integer>> maxHeap =
                new PriorityQueue<>(
                        (a, b) -> Integer.compare(b.getValue(), a.getValue())
                );

        maxHeap.addAll(frequency.entrySet());

        // Step 3: Build the answer.
        StringBuilder result = new StringBuilder(s.length());

        while (!maxHeap.isEmpty()) {

            Map.Entry<Character, Integer> entry = maxHeap.poll();

            char ch = entry.getKey();
            int freq = entry.getValue();

            // Append the character freq times.
            for (int i = 0; i < freq; i++) {
                result.append(ch);
            }
        }

        return result.toString();
    }
}
```

### Complexity

With `k` distinct characters:

```text
HashMap counting: O(n)
Heap construction: O(k)
Heap removals: O(k log k)
Building result: O(n)
```

Therefore:

```text
Time:  O(n + k log k)
Space: O(k)
```

For this particular problem, `k ≤ 62`, so again this is effectively:

```text
Time:  O(n)
Space: O(1)
```

But the implementation has more moving parts than the frequency-array solution.

---

# Which one should you use?

| Approach            |             Time |  Space | When to use                  |
| ------------------- | ---------------: | -----: | ---------------------------- |
| Frequency + sorting | `O(n + k log k)` | `O(k)` | ⭐ Best here                  |
| Frequency buckets   |           `O(n)` | `O(n)` | When frequency range matters |
| HashMap + Heap      | `O(n + k log k)` | `O(k)` | ⭐ General character set      |
| HashMap + TreeMap   | `O(n + k log k)` | `O(k)` | Possible, but less natural   |

## My interview choice

I'd immediately notice:

> **Only 62 possible characters.**

So I'd say:

> "I'll count frequencies using an array. Since there can be at most 62 distinct characters, I'll collect the characters that occur and sort those 62 characters by frequency descending. Finally, I'll append each character according to its frequency."

Then implement **Approach 3**.

The important interview pattern to remember is:

```text
Need ordering based on frequency
        ↓
Count frequency first
        ↓
Do I have a small fixed character set?
        ↓
YES → frequency array
        ↓
Sort only distinct characters
        ↓
Build answer
```

This avoids over-engineering the problem with a heap when the input's character set is tiny.
