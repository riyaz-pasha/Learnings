# Cyclic Sort — Complete Mastery Guide (Java)

---

## 1. The Core Idea (Start Here)

Forget "sorting algorithms" for a second. Cyclic Sort is not really a general-purpose
sorting algorithm like QuickSort or MergeSort. It's a **placement algorithm**.

The entire pattern rests on one observation:

> If you have `n` numbers that are guaranteed to be drawn from a **known, small, contiguous
> range** (almost always `1..n` or `0..n-1`), then each number already knows exactly
> where it should live in the array.

If a number's value is `v`, and the array is 0-indexed and contains `1..n`, then `v`
belongs at index `v - 1`. Period. You don't need to *compare* elements to each other
(like every comparison-based sort does) — you just need to check "is this number
sitting in its own house?" If not, **send it home**, and bring back whatever was
squatting in that house. Repeat until everyone is home.

That's it. That's the entire algorithm. Everything else in this guide is variations on
"what do we do with the leftover clues after everyone who *can* go home has gone home."

### Why "cyclic"?

Because placing one number correctly may kick out another number, which then needs to
be placed correctly, which may kick out another... until eventually the chain of swaps
closes a loop (a cycle) and returns to a position that's now correctly filled. Picture
musical chairs where everyone already has an assigned seat number written on their
shirt.

```
Array: [3, 1, 5, 4, 2]     (values 1..5, should end as [1,2,3,4,5])

index:   0  1  2  3  4
value:   3  1  5  4  2

value 3 belongs at index 2. Swap index0 <-> index2:
index:   0  1  2  3  4
value:   5  1  3  4  2

value 5 (now at index0) belongs at index 4. Swap index0 <-> index4:
index:   0  1  2  3  4
value:   2  1  3  4  5

value 2 (now at index0) belongs at index 1. Swap index0 <-> index1:
index:   0  1  2  3  4
value:   1  2  3  4  5   <-- done, cycle closed
```

Notice: we did **one pass** (with an inner while-loop) and got a fully sorted array in
**O(n)** time using **O(1)** extra space. A comparison sort could never beat O(n log n)
— but Cyclic Sort isn't a comparison sort. It's a **direct-addressing / placement**
sort. It cheats by using the values themselves as index hints. This is the same family
of idea as counting sort and bucket sort — trading "range must be known and bounded" for
"no comparisons needed."

---

## 2. The Intuition, Built From Scratch

Imagine a hotel with `n` numbered rooms (1 to n), and `n` guests, each already holding a
room key with their assigned room number on it. They're all standing in a random line
in the lobby (the array). You want to get everyone into their correct room (correct
index) as fast as possible, doing this yourself, one guest at a time, with no extra
space to store people temporarily.

Your strategy, standing at the front of the line:

1. Look at the guest at the front. Check their key (value).
2. If they're already in the right spot (guest #1 is in position #1), great, move to
   the next position.
3. If not, swap them with whoever is *currently* standing in the room they belong to.
   Now the correct guest is in the correct room.
4. But now a *new* guest is standing in front of you (the one who used to be in that
   room). Repeat step 2 for this new guest — **don't move forward yet**.
5. Only move forward once the guest standing in front of you is correctly placed
   (or you've hit a duplicate — more on that later).

This is exactly a `while` loop nested inside a `for` loop — and it's the single most
important structural fact to memorize about this pattern:

```java
for (int i = 0; i < n; i++) {
    while (/* nums[i] is NOT in its correct home */) {
        swap(nums, i, correctIndexFor(nums[i]));
    }
}
```

The outer `for` loop only ever advances `i` by 1 each time — but the `while` loop can
fire multiple times per index because a single swap can bring in a brand-new
"guest" that also needs relocating.

---

## 3. Why This Is Actually O(n) (Not O(n²))

At first glance, a `while` loop nested in a `for` loop looks suspicious —
"isn't that O(n²)??" This is the #1 thing beginners get wrong, and understanding *why*
it's not O(n²) is what separates someone who memorized the pattern from someone who
understands it.

**The key insight: every single swap places at least one number into its final,
correct position — permanently.** A number, once correctly placed, is never touched
again (we only swap when a number is in the *wrong* spot). Since there are only `n`
numbers total, there can be **at most `n` swaps in the entire algorithm's lifetime**,
no matter how you distribute them across outer-loop iterations.

```
Total swaps across the whole run <= n
=> Total work done inside all while-loops combined <= n
=> Overall time complexity = O(n) + O(n) = O(n)
```

This is a classic "amortized analysis" argument — the same style of reasoning used to
show that dynamic array resizing is O(1) amortized per insertion. Don't count
iterations of the outer loop and inner loop as if they multiply. Count **total swaps**,
because that's the true unit of work, and that total is capped at `n`.

---

## 4. The Prerequisites — When Can You Even Use This Pattern?

Cyclic Sort applies **only** when the input satisfies (or can be massaged into)
these conditions:

1. **The array holds numbers from a known range**, almost always `1..n` or `0..n-1`,
   where `n` is the array's length (or close to it).
2. **You want the numbers sorted or want to detect which numbers are missing,
   duplicated, or misplaced relative to that range** — you don't actually need to sort
   arbitrary numbers.
3. Bonus condition that shows up constantly: **you're told to solve it in O(n) time
   and O(1) extra space.** This is the single loudest signal in an interview. Cyclic
   Sort is basically the only pattern that satisfies both constraints simultaneously
   for "range-bound" array problems. If you see O(n) time + O(1) space + "array
   contains numbers from 1 to n" in the same problem, stop reading the problem and
   start writing `while (nums[i] != nums[nums[i] - 1])`.

### Pattern-Recognition Checklist (memorize this)

Ask these questions when you see a new array problem:

- [ ] Are the array elements integers?
- [ ] Do they fall within a range of size roughly `n` (array length) — e.g., `1..n`,
      `0..n-1`, `0..n`, `1..n+1` (n+1 numbers missing one, etc.)?
- [ ] Is the problem asking about **missing numbers**, **duplicate numbers**,
      **the smallest missing positive**, **finding an element that appears twice/once**,
      **sorting when values ≈ indices**, or "**find the corrupt pair**"?
- [ ] Does it demand O(n) time and O(1) space (no HashSet/HashMap allowed, or at least
      "can you do better than that")?

If you check 3-4 of these boxes, it's Cyclic Sort (or a very close cousin). This
pattern shows up under disguises like "First Missing Positive," "Find All Duplicates,"
"Find the Duplicate Number," "Set Mismatch," "Find All Numbers Disappeared," "Couples
Holding Hands" (loosely), and "Missing Number."

### When it does NOT apply

- Values are unbounded or span a huge range unrelated to array length (e.g., could be
  any 32-bit integer with no relation to `n`) — use sorting, hashing, or bit
  manipulation instead.
- You need a **stable** sort or need to preserve original relative order of equal
  elements — cyclic sort doesn't care about stability, it's placing by value.
  (Not usually an issue since duplicates aren't the "sorted" output typically.)
- Negative numbers or zero mixed in when the range assumption is `1..n` — you'll need
  to explicitly skip/ignore those, or map them into a comparable "out of range" bucket.

---

## 5. The Core Template (Java)

This is the single piece of code to internalize. Every variation below is a small edit
to this skeleton.

```java
public void cyclicSort(int[] nums) {
    int i = 0;
    while (i < nums.length) {
        int correctIndex = nums[i] - 1; // for range 1..n
        if (nums[i] != nums[correctIndex]) {
            swap(nums, i, correctIndex);
        } else {
            i++;
        }
    }
}

private void swap(int[] nums, int a, int b) {
    int temp = nums[a];
    nums[a] = nums[b];
    nums[b] = temp;
}
```

### Why `while (i < nums.length)` and not a `for` loop with nested `while`?

Both work and both are O(n) — I wrote it as a single `while` with manual `i++` inside
the `else` branch because it more clearly shows: **"only advance `i` when the current
slot is already correct."** Some people prefer:

```java
for (int i = 0; i < nums.length; i++) {
    while (nums[i] != nums[nums[i] - 1]) {
        swap(nums, i, nums[i] - 1);
    }
}
```

These are functionally identical. Pick whichever reads clearer to you — I'll use the
`for` + inner `while` form for the rest of this guide since it's the more commonly
seen interview form.

### Dry Run (trace it yourself, then check)

```
nums = [3, 1, 5, 4, 2],  range 1..5

i=0: nums[0]=3, correct home is index 2 (value 5). nums[2]=5 != 3 -> swap
     [5, 1, 3, 4, 2]
     nums[0]=5, correct home index 4 (value 2). nums[4]=2 != 5 -> swap
     [2, 1, 3, 4, 5]
     nums[0]=2, correct home index 1 (value 1). nums[1]=1 != 2 -> swap
     [1, 2, 3, 4, 5]
     nums[0]=1, correct home index 0. nums[0]==1 -> stop inner while, i++

i=1: nums[1]=2, correct home index 1. Already correct -> i++
i=2: nums[2]=3, correct -> i++
i=3: nums[3]=4, correct -> i++
i=4: nums[4]=5, correct -> i++

Result: [1, 2, 3, 4, 5]
```

### The critical guard clause you'll forget at first

What if there are **duplicates**, e.g. `[1, 1, 3, 4]`? Without a guard, this line:

```java
while (nums[i] != nums[nums[i] - 1])
```

...will still correctly terminate! Because once `nums[i]` equals `nums[nums[i]-1]`,
the condition is false and the loop stops — even if `nums[i]` isn't in the "textbook
correct" position. This is actually the **secret weapon** of the pattern for duplicate/
missing-number problems: the while loop naturally halts on duplicates instead of
looping forever, since swapping two equal values is a no-op that doesn't change
anything, so `nums[i] == nums[nums[i]-1]` becomes permanently true. Let's prove it:

```
nums = [1, 1, 3, 4]  (range 1..4, index3 should've held a value but only 4 numbers,
                       so this represents "2 is missing, 1 is duplicated")

i=0: nums[0]=1, correct home index 0. Already matches -> i++
i=1: nums[1]=1, correct home index 0. nums[0]=1 == nums[1]=1 -> condition false,
     stop while (no infinite loop!) -> i++
i=2: nums[2]=3, correct home index 2. Matches -> i++
i=3: nums[3]=4, correct home index 3. Matches -> i++

Final: [1, 1, 3, 4]  (2 is "missing" from its home; a later pass detects this)
```

This is *why* Cyclic Sort is so good at duplicate/missing-number problems: the
algorithm is self-terminating even in the presence of duplicates, and after the sort
pass, any index `i` where `nums[i] != i + 1` is a "clue" — either that value is
missing, or a duplicate is sitting there.

---

## 6. Complexity Summary

| Aspect | Value | Why |
|---|---|---|
| Time | O(n) | Amortized — total swaps across the whole algorithm ≤ n |
| Space | O(1) | In-place swaps only, no auxiliary array/map |
| Stability | Not stable | Doesn't preserve relative order — not a general sort |
| Applicability | Narrow | Only works when values map to indices in a known range |

---

## 7. Visual Mental Model (ASCII)

Think of it as **each index having a "reserved seat" value**, and the algorithm doing
seat-swaps until every seat has its reserved occupant:

```
Index:     0    1    2    3    4
Reserved:  1    2    3    4    5      <-- what SHOULD sit here

Current:  [3,   1,   5,   4,   2]     <-- what IS sitting here (scrambled)

Cyclic sort walks left to right, and for each seat, keeps trading its current
occupant with whoever's actual reserved seat that occupant belongs in — like a
chain of "this isn't your seat, go find your seat, and see who's sitting where
you belong."
```

Cycle diagram of what happens to value 3, 5, 2 (the "cycle" this creates):

```
      +---------------------------+
      |                           |
      v                           |
  idx0 (holds 3) --swap--> idx2 (holds 5) --swap--> idx4 (holds 2) --swap--> idx1
      ^                                                                        |
      |________________________________________________________________ _____|
                          (cycle closes back to idx0, now holds 1 - correct)
```

---

## 8. Variation Catalog — This Is Where Mastery Actually Happens

The base template rarely appears unmodified in interviews. What you'll actually be
asked to do is recognize the *shape* of the problem and adapt the template. Below is
essentially every major variation, from easiest to hardest, each with the "aha" and
full Java code.

### 8.1 Missing Number (range 0 to n, one missing)

**Problem:** Array has `n` distinct numbers from `0` to `n` (n+1 possible numbers, one
is missing). Find the missing one.

**Aha:** Range is `0..n`, so element `v` belongs at index `v` (not `v-1`, since we
start at 0). Since the array only has `n` slots but values go up to `n`, the value `n`
itself has no home in-bounds — handle that as a natural skip. After sorting, scan for
the first index where `nums[i] != i`; that index is the missing number. If none found,
the missing number is `n`.

```java
public int missingNumber(int[] nums) {
    int i = 0, n = nums.length;
    while (i < n) {
        int correct = nums[i]; // belongs at index == its own value
        if (nums[i] < n && nums[i] != nums[correct]) {
            swap(nums, i, correct);
        } else {
            i++;
        }
    }
    for (i = 0; i < n; i++) {
        if (nums[i] != i) return i;
    }
    return n;
}
```

### 8.2 Find All Missing Numbers (range 1 to n, multiple missing)

**Problem:** Array of size `n`, values in `1..n`, some numbers appear, others don't
(no duplicates matter here, but might exist). Return all missing numbers.

**Aha:** Same placement pass, then a linear scan: wherever `nums[i] != i + 1`, the
number `i + 1` is missing.

```java
public List<Integer> findDisappearedNumbers(int[] nums) {
    int i = 0, n = nums.length;
    while (i < n) {
        int correct = nums[i] - 1;
        if (nums[i] != nums[correct]) {
            swap(nums, i, correct);
        } else {
            i++;
        }
    }
    List<Integer> missing = new ArrayList<>();
    for (i = 0; i < n; i++) {
        if (nums[i] != i + 1) missing.add(i + 1);
    }
    return missing;
}
```

### 8.3 Find All Duplicates (range 1 to n, each number appears once or twice)

**Problem:** Same setup, but now some numbers appear exactly twice. Find all of them.

**Aha:** After the placement pass, any index `i` where `nums[i] != i + 1` holds a
value that is a duplicate of whatever's correctly sitting at `nums[i]`'s real home.

```java
public List<Integer> findDuplicates(int[] nums) {
    int i = 0, n = nums.length;
    while (i < n) {
        int correct = nums[i] - 1;
        if (nums[i] != nums[correct]) {
            swap(nums, i, correct);
        } else {
            i++;
        }
    }
    List<Integer> duplicates = new ArrayList<>();
    for (i = 0; i < n; i++) {
        if (nums[i] != i + 1) duplicates.add(nums[i]);
    }
    return duplicates;
}
```

### 8.4 Find the Single Duplicate Number (range 1 to n-1, array size n, one dup)

**Problem:** Array of size `n+1` containing numbers `1..n`, exactly one number
repeated (could repeat more than twice). Classic "Find the Duplicate Number"
(LeetCode 287) — usually asked with the extra constraint "you cannot modify the
array" or "O(1) space" which pushes people toward Floyd's cycle detection instead.
**But if modification is allowed**, cyclic sort is the most intuitive solution:

```java
public int findDuplicate(int[] nums) {
    int i = 0, n = nums.length;
    while (i < n) {
        if (nums[i] != i + 1) {
            int correct = nums[i] - 1;
            if (nums[i] != nums[correct]) {
                swap(nums, i, correct);
            } else {
                // nums[i] == nums[correct] but i != correct => duplicate found
                return nums[i];
            }
        } else {
            i++;
        }
    }
    return -1;
}
```

*Note:* This variant needs the extra check `nums[i] != nums[correct]` to distinguish
"already correctly placed" from "found a duplicate stuck in the wrong slot" — a subtle
but important tweak over the base template.

### 8.5 Set Mismatch (one number duplicated, another missing, both size n)

**Problem:** Array of `n` numbers, `1..n`, one number is duplicated (taking the place
of another), so exactly one number is missing and one is duplicated. Return
`[duplicate, missing]`.

**Aha:** After placement, the one mismatched index tells you both answers at once:
`nums[i]` is the duplicate, `i + 1` is the missing number.

```java
public int[] findErrorNums(int[] nums) {
    int i = 0, n = nums.length;
    while (i < n) {
        int correct = nums[i] - 1;
        if (nums[i] != nums[correct]) {
            swap(nums, i, correct);
        } else {
            i++;
        }
    }
    for (i = 0; i < n; i++) {
        if (nums[i] != i + 1) {
            return new int[]{nums[i], i + 1};
        }
    }
    return new int[]{-1, -1};
}
```

### 8.6 First Missing Positive (the hardest classic — LeetCode 41)

**Problem:** Unsorted array of arbitrary integers (can include negatives, zero, huge
numbers, duplicates). Find the smallest missing positive integer, in O(n) time,
O(1) space.

**Aha:** This is the variation that trips people up because the array *isn't* pre-
constrained to `1..n` — it can contain garbage. The trick: **any answer must be in the
range `1..n+1`** (if array size is `n`, the answer can be at most `n+1`, achieved when
`nums` is exactly `{1,...,n}`). So you only bother placing values that fall within
`1..n` — anything outside that range (negative, zero, or > n) is irrelevant noise and
gets ignored during placement.

```java
public int firstMissingPositive(int[] nums) {
    int i = 0, n = nums.length;
    while (i < n) {
        int correct = nums[i] - 1;
        // Only place values that are within 1..n and not already correctly placed
        if (nums[i] > 0 && nums[i] <= n && nums[i] != nums[correct]) {
            swap(nums, i, correct);
        } else {
            i++;
        }
    }
    for (i = 0; i < n; i++) {
        if (nums[i] != i + 1) return i + 1;
    }
    return n + 1;
}
```

This is the single best problem to fully internalize because it combines *every*
lesson above: range-limiting the placement condition, self-terminating duplicate
handling, and the final linear scan for the first mismatch.

### 8.7 Kth Missing / Smallest Missing Positive Variants

Many follow-up questions ("find the 2nd missing number," "find the smallest missing
positive greater than k") are solved by running the same placement pass, then walking
the final scan with a counter instead of returning on the first mismatch.

---

## 9. The Universal 3-Step Mental Algorithm

Once you've internalized the variations, every cyclic sort problem collapses into the
same three steps. Say this out loud when you see a new problem:

```
STEP 1 — PLACE:
   Walk left to right. At each index, while the current value has a valid,
   knowable "correct home" within array bounds AND it isn't already there,
   swap it there. Advance only when correct-or-unplaceable.

STEP 2 — SCAN:
   Walk left to right again. Compare nums[i] to its expected value (i+1 or i).
   Every mismatch is a "clue" (missing number / duplicate / whatever the
   question wants).

STEP 3 — INTERPRET:
   Translate each clue based on what the question asks:
     - "value expected at i is missing"        -> missing number problems
     - "value sitting at i belongs elsewhere"   -> duplicate problems
     - "first index where mismatch occurs"      -> first-missing-positive style
     - "collect index+1 for every mismatch"     -> find-all-missing style
     - "collect nums[i] for every mismatch"     -> find-all-duplicates style
```

---

## 10. Common Bugs & Pitfalls (things that WILL bite you)

1. **Off-by-one on the range.** `1..n` maps value `v` to index `v-1`. `0..n-1` maps
   value `v` to index `v` directly. Mixing these up is the #1 bug. Always write out
   a tiny 3-element example on scratch paper before coding.

2. **Forgetting to bound-check before indexing.** In "First Missing Positive," if you
   don't check `nums[i] > 0 && nums[i] <= n` before using `nums[i] - 1` as an index,
   you'll get an `ArrayIndexOutOfBoundsException` or corrupt data. Always validate the
   value is in-range *before* using it as an index.

3. **Infinite loop from a bad swap condition.** If your while-condition is
   `nums[i] != nums[i] - 1` (using `i` instead of `correct` on one side, a common typo)
   or you swap and forget one of the numbers can be immediately re-swapped back and
   forth, you'll infinite-loop. The self-terminating guarantee **only holds** if your
   swap condition correctly checks `nums[i] != nums[correctIndex]` — not some other
   comparison.

4. **Not handling duplicates before assuming "sorted means correct."** After a cyclic
   sort pass, don't assume the array is genuinely `1..n` sorted — always re-verify by
   scanning, because duplicates/missing values will leave the array in a "settled but
   not fully sorted" state (as shown in the `[1,1,3,4]` example above).

5. **Trying to use it on unbounded ranges.** If someone says "array of arbitrary
   integers, find the two that sum to target," that's Two-Sum territory (hashing),
   not Cyclic Sort. Don't force-fit the pattern where the range condition doesn't hold.

6. **Using extra space "just to be safe."** If you catch yourself reaching for a
   `HashSet`/`boolean[] seen` in a cyclic-sort-shaped problem, stop — that defeats
   the entire point (O(1) space) and is usually a sign you haven't found the in-place
   placement condition yet.

---

## 11. How To *Recognize* It Under Interview Pressure (Signal Words)

Train yourself to pattern-match on phrasing. These phrases are strong tells:

- "array contains n numbers from 1 to n" / "0 to n-1" / "1 to n+1"
- "find the missing number(s)"
- "find the duplicate number(s)"
- "some numbers appear twice, others don't appear at all"
- "find the smallest missing positive integer"
- "sort the array... but wait, can you do it in O(n) with O(1) extra space?"
- "find the corrupt pair" / "set mismatch"

If you see **any array bounded by its own length in value-range**, your brain should
immediately jump to: *"value tells me its index — cyclic sort."*

---

## 12. Complexity & Pattern Comparison Table

| Problem Type | Best Non-Cyclic-Sort Approach | Cyclic Sort Approach | Why Cyclic Sort Wins |
|---|---|---|---|
| Missing Number | Sum formula `n(n+1)/2 - sum(nums)` | Placement + scan | Sum approach is O(n)/O(1) too, but breaks with duplicates or multiple missing values — cyclic sort generalizes |
| Find Duplicates | HashSet | Placement + scan | HashSet is O(n) space; cyclic sort is O(1) |
| First Missing Positive | Sorting (O(n log n)) or HashSet (O(n) space) | Placement + scan | Meets strict O(n)/O(1) requirement |
| Find the Duplicate (no mutation allowed) | Floyd's cycle detection (linked-list-style) | N/A (needs mutation) | Use Floyd's instead when array is read-only |

---

## 13. Practice Ladder (do these in order)

**Foundational (get the template into muscle memory):**
1. Sort an array containing 1 to n (write the base template from scratch, no peeking)
2. Missing Number (LeetCode 268)
3. Find All Numbers Disappeared in an Array (LeetCode 448)

**Intermediate (learn the "clue interpretation" step):**
4. Find All Duplicates in an Array (LeetCode 442)
5. Set Mismatch (LeetCode 645)

**Advanced (combine everything, handle out-of-range noise):**
6. First Missing Positive (LeetCode 41) — do this one until you can write it cold in
   under 5 minutes
7. Find the Duplicate Number (LeetCode 287) — solve it BOTH with cyclic sort
   (assuming mutation allowed) AND Floyd's cycle detection (assuming it isn't), so you
   understand the boundary of when cyclic sort stops being viable

**Stretch goals (pattern transfer — recognize the "spirit" of cyclic sort elsewhere):**
8. Couples Holding Hands (LeetCode 765) — not pure cyclic sort, but same
   "swap into correct group" placement mentality
9. K-th Missing Positive Number (LeetCode 1539) — solvable via cyclic sort placement +
   counting scan, contrast with the binary search solution to see two different
   valid paradigms for the same problem

---

## 14. Full Worked Example — Building "First Missing Positive" Step-by-Step

Since this is the capstone problem, let's fully trace it once so the reasoning is
airtight.

```
nums = [3, 4, -1, 1],  n = 4
Expected valid answers range: 1..5 (n+1 = 5 is possible if array were {1,2,3,4})

i=0: nums[0]=3. In range (1..4), correct index = 2. nums[2]=-1 != 3 -> swap
     [-1, 4, 3, 1]
     nums[0]=-1. NOT in range (<=0) -> skip placement, i++

i=1: nums[1]=4. In range, correct index = 3. nums[3]=1 != 4 -> swap
     [-1, 1, 3, 4]
     nums[1]=1. In range, correct index = 0. nums[0]=-1 != 1 -> swap
     [1, -1, 3, 4]
     nums[1]=-1. NOT in range -> skip, i++

i=2: nums[2]=3. correct index = 2. nums[2]==nums[2] already -> i++

i=3: nums[3]=4. correct index = 3. Already correct -> i++

Final array: [1, -1, 3, 4]

Scan:
  i=0: nums[0]=1, expected 1 -> match
  i=1: nums[1]=-1, expected 2 -> MISMATCH -> answer = 2
```

Answer: `2`. Verify by inspection: original array had 3,4,-1,1 → positives present are
{1,3,4}, so the smallest missing positive is indeed 2. ✓

---

## 15. One-Paragraph Summary (for spaced-repetition / flash-card review)

> Cyclic Sort works when array values map onto array indices within a known, bounded
> range (typically 1..n or 0..n-1). Walk the array once; at each position, keep
> swapping the current value into its "home" index until either it's home or a
> duplicate blocks it (duplicates self-terminate the swap because swapping equal
> values is a no-op). Because every swap permanently places one value into its final
> home, total swaps are bounded by n, making the whole thing O(n) time despite the
> nested loop appearance, and O(1) space since it's all in-place. After this placement
> pass, do a second linear scan comparing each index to its expected value — every
> mismatch is a clue that answers questions about missing numbers, duplicates, or the
> first missing positive, depending on how the problem asks you to interpret it.

---

## Appendix: All Code in One File (copy-paste to run/test)

```java
import java.util.*;

public class CyclicSortPatterns {

    private static void swap(int[] nums, int a, int b) {
        int temp = nums[a];
        nums[a] = nums[b];
        nums[b] = temp;
    }

    // 1. Base cyclic sort: values 1..n
    public static void cyclicSort(int[] nums) {
        int i = 0, n = nums.length;
        while (i < n) {
            int correct = nums[i] - 1;
            if (nums[i] != nums[correct]) {
                swap(nums, i, correct);
            } else {
                i++;
            }
        }
    }

    // 2. Missing Number: values 0..n, size n
    public static int missingNumber(int[] nums) {
        int i = 0, n = nums.length;
        while (i < n) {
            int correct = nums[i];
            if (nums[i] < n && nums[i] != nums[correct]) {
                swap(nums, i, correct);
            } else {
                i++;
            }
        }
        for (i = 0; i < n; i++) if (nums[i] != i) return i;
        return n;
    }

    // 3. Find All Missing Numbers: values 1..n, size n
    public static List<Integer> findDisappearedNumbers(int[] nums) {
        int i = 0, n = nums.length;
        while (i < n) {
            int correct = nums[i] - 1;
            if (nums[i] != nums[correct]) {
                swap(nums, i, correct);
            } else {
                i++;
            }
        }
        List<Integer> missing = new ArrayList<>();
        for (i = 0; i < n; i++) if (nums[i] != i + 1) missing.add(i + 1);
        return missing;
    }

    // 4. Find All Duplicates: values 1..n, size n
    public static List<Integer> findDuplicates(int[] nums) {
        int i = 0, n = nums.length;
        while (i < n) {
            int correct = nums[i] - 1;
            if (nums[i] != nums[correct]) {
                swap(nums, i, correct);
            } else {
                i++;
            }
        }
        List<Integer> duplicates = new ArrayList<>();
        for (i = 0; i < n; i++) if (nums[i] != i + 1) duplicates.add(nums[i]);
        return duplicates;
    }

    // 5. Find the Duplicate Number (mutation allowed): values 1..n-1, size n
    public static int findDuplicate(int[] nums) {
        int i = 0, n = nums.length;
        while (i < n) {
            if (nums[i] != i + 1) {
                int correct = nums[i] - 1;
                if (nums[i] != nums[correct]) {
                    swap(nums, i, correct);
                } else {
                    return nums[i];
                }
            } else {
                i++;
            }
        }
        return -1;
    }

    // 6. Set Mismatch: values 1..n, size n, one dup + one missing
    public static int[] findErrorNums(int[] nums) {
        int i = 0, n = nums.length;
        while (i < n) {
            int correct = nums[i] - 1;
            if (nums[i] != nums[correct]) {
                swap(nums, i, correct);
            } else {
                i++;
            }
        }
        for (i = 0; i < n; i++) {
            if (nums[i] != i + 1) return new int[]{nums[i], i + 1};
        }
        return new int[]{-1, -1};
    }

    // 7. First Missing Positive: arbitrary integers
    public static int firstMissingPositive(int[] nums) {
        int i = 0, n = nums.length;
        while (i < n) {
            int correct = nums[i] - 1;
            if (nums[i] > 0 && nums[i] <= n && nums[i] != nums[correct]) {
                swap(nums, i, correct);
            } else {
                i++;
            }
        }
        for (i = 0; i < n; i++) if (nums[i] != i + 1) return i + 1;
        return n + 1;
    }

    public static void main(String[] args) {
        int[] a = {3, 1, 5, 4, 2};
        cyclicSort(a);
        System.out.println("cyclicSort: " + Arrays.toString(a)); // [1,2,3,4,5]

        System.out.println("missingNumber: " + missingNumber(new int[]{3,0,1})); // 2

        System.out.println("findDisappearedNumbers: " +
            findDisappearedNumbers(new int[]{4,3,2,7,8,2,3,1})); // [5,6]

        System.out.println("findDuplicates: " +
            findDuplicates(new int[]{4,3,2,7,8,2,3,1})); // [2,3]

        System.out.println("findDuplicate: " +
            findDuplicate(new int[]{1,3,4,2,2})); // 2

        System.out.println("findErrorNums: " +
            Arrays.toString(findErrorNums(new int[]{1,2,2,4}))); // [2,3]

        System.out.println("firstMissingPositive: " +
            firstMissingPositive(new int[]{3,4,-1,1})); // 2
    }
}
```

Run this file (`javac CyclicSortPatterns.java && java CyclicSortPatterns`) and verify
every output against the comments. Then delete the bodies and rewrite each method from
memory using only Section 9's 3-step algorithm as your guide — that's the real test of
mastery.

