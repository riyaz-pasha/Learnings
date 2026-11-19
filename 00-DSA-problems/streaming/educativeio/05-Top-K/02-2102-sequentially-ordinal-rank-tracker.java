/**
 * 0. ## Problem Statement
 * A scenic location is represented by a unique `name` (lowercase English letters) and an
 * integer attractiveness `score`. Locations are ranked from best to worst:
 * - Higher `score` is better.
 * - If two locations have the same `score`, the lexicographically SMALLER `name` is better.
 *
 * Implement the `SORTracker` class:
 * - `SORTracker()`: Initializes an empty tracker.
 * - `void add(String name, int score)`: Adds a scenic location to the system.
 * - `String get()`: Returns the `i`-th best location among all locations added so far,
 *   where `i` is the total number of times `get()` has been called (including this call).
 *
 * Constraints:
 * - `name` is unique among all locations, `1 <= name.length() <= 10`, lowercase letters only.
 * - `1 <= score <= 10^5`
 * - At any time, number of `get()` calls <= number of `add()` calls.
 * - At most `4 * 10^4` total calls to `add` and `get`.
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can the same location `name` be added twice with an updated score, or are names strictly unique?"
 *    -> Prevents needing a HashMap + lazy deletion to update existing scores; unique names mean append-only.
 * 2. "In a score tie, is 'lexicographically smaller' standard ASCII/dictionary order (e.g., 'alps' before 'bradford')?"
 *    -> Confirms `a.name.compareTo(b.name)` ascending for ties while `score` is descending — easy to flip by mistake.
 * 3. "Is `get()` ALWAYS sequential (1st, 2nd, 3rd, ...), or could a caller ask for an arbitrary rank `k`?"
 *    -> The entire O(log N) two-heap trick relies on the queried rank `i` only ever increasing by 1! Arbitrary `k` would require an Order Statistic Tree or Fenwick Tree.
 * 4. "Can the same location be returned on multiple consecutive `get()` calls?"
 *    -> Yes! If a better location is inserted between the 3rd and 4th `get()`, the old 3rd-best shifts down to 4th-best and is returned again. Rules out "pop and discard" approaches.
 * 5. "Will `get()` ever be called when fewer than `i` locations have been added?"
 *    -> Constraints guarantee `gets <= adds` at all times, avoiding null/exception return contracts.
 * 6. "Can scores be negative or near `Integer.MAX_VALUE`?"
 *    -> Prevents overflow in `b.score - a.score` comparators (always use `Integer.compare`).
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to `4 * 10^4` total calls to `add()` and `get()`.
 * Any approach costing O(N) or O(N log N) per call reaches `~10^9` operations (TLE).
 * We need O(log N) (or better) per operation.
 *
 * Notice what makes this problem special compared to "Kth Largest in a Stream" (LC 703):
 * In LC 703, `k` is FIXED forever, so elements worse than `k` can be thrown away.
 * Here, the queried rank `i` GROWS by 1 on every `get()`, so we CANNOT discard worse locations —
 * today's 10th-best location will eventually be queried on the 10th `get()`!
 *
 * --- Approach 1: Brute Force (Unsorted List + Re-Sort on `get`) ---
 * 0. Idea/Intuition: Append every `(name, score)` to an `ArrayList`. On the `i`-th `get()`,
 *    sort the entire list from best to worst and return `list.get(i - 1)`.
 * 1. What I'd naturally try: Literally translate "sort all locations so far and pick index `i - 1`."
 * 2. Why it works: Re-sorting before every query guarantees the exact global order is fresh.
 * 3. Why it's too slow/costly: With 20,000 `add`s followed by 20,000 `get`s, each `get` sorts
 *    20,000 items -> `2 * 10^4 * 2 * 10^4 * log(2 * 10^4) ≈ 5.7 * 10^9` comparisons (TLE).
 * 4. What work is being repeated: The list was ALREADY sorted on the previous `get()`!
 *    Re-sorting all N items just because 1 or 2 new locations arrived throws away all prior ordering.
 * 5. Observation that removes the bottleneck: Keep the list sorted at all times by inserting each
 *    new location into its exact sorted position via binary search.
 * 6. Leads to Approach 2: Maintained Sorted `ArrayList`.
 *    Time Complexity: `add`: O(1) — appends to `ArrayList`.
 *    `get`: O(N log N) — sorts all N locations added so far on every query.
 *    Space Complexity: O(N) — stores all N `Location` objects in an `ArrayList`.
 *
 * --- Approach 2: Maintained Sorted `ArrayList` (Binary Search + Shift) ---
 * 0. Idea/Intuition: Keep `ArrayList<Location>` sorted best-to-worst at all times.
 *    On `add()`, binary search for the insertion index in O(log N) and insert there.
 *    On `get()`, simply return `list.get(queryCount++)` in O(1).
 * 1. What I'd naturally try: "Don't re-sort on `get()` — maintain sorted order on `add()`!"
 * 2. Why it works: Every `get()` is now an instant O(1) array index lookup.
 * 3. Why it's still too slow/costly: Finding the index takes O(log N), but `ArrayList.add(idx, loc)`
 *    must shift up to N elements in memory on every `add()`. With 20,000 `add`s in ascending score
 *    order, every insert hits index 0 -> `~2 * 10^8` array shifts (borderline/slow at scale).
 * 4. What work is being repeated: We are maintaining the EXACT sorted position of every single
 *    element from rank 1 to rank N — even though `get()` ONLY ever asks for ONE boundary:
 *    the exact split between the top `i` best locations and the remaining `N - i` locations!
 * 5. Observation that removes the bottleneck: Look closely at how the rank `i` moves:
 *    `i` ONLY ever increments by 1 (1 -> 2 -> 3 -> ...).
 *    We don't need random access to rank 47 or rank 999 right now! We ONLY need two partitions
 *    around the current query count `i`:
 *      - `bestPartition`: holds the exact `i` best locations seen so far.
 *        What do we ever need from it? Its WORST element (in case a new `add()` bumps it out!).
 *      - `restPartition`: holds all remaining `N - i` locations.
 *        What do we ever need from it? Its BEST element (that's literally the `(i+1)`-th best
 *        location — the answer to the next `get()`!).
 * 6. Leads to Approach 3: Two Opposing Heaps (just like Find Median from Data Stream, LC 295,
 *    except the split point is `i` instead of `N / 2`!).
 *    Time Complexity: `add`: O(N) — O(log N) binary search, dominated by O(N) `ArrayList` element shifting.
 *    `get`: O(1) — direct index access `list.get(queryCount++)`.
 *    Space Complexity: O(N) — stores all N locations in the `ArrayList`.
 *
 * --- Approach 3: Two Opposing Heaps (Optimal) ---
 * 0. Idea/Intuition: Maintain two `PriorityQueue<Location>`s around the completed query count `i`:
 *    - `topBest` (size == `i`): ordered WORST-at-root, holding the `i` best locations so far.
 *    - `remaining` (size == `N - i`): ordered BEST-at-root, holding all other locations.
 *    Now watch how absurdly clean both operations become:
 *    - `add(loc)`: Offer `loc` into `topBest` (size becomes `i + 1`), then immediately poll the
 *      WORST element of `topBest` and offer it to `remaining`!
 *      - If `loc` belonged in the top `i`, it stays in `topBest` and the old `i`-th best is
 *        demoted to `remaining`.
 *      - If `loc` did NOT belong in the top `i`, `loc` itself is the worst in `topBest` and
 *        is immediately bounced to `remaining`.
 *      - Even when `i == 0`, `loc` enters empty `topBest` and bounces straight to `remaining`!
 *      Zero `if` statements needed!
 *    - `get()`: The `(i + 1)`-th best location is guaranteed to be `remaining.peek()`.
 *      Poll it from `remaining`, offer it into `topBest` (so `topBest.size()` becomes `i + 1`),
 *      and return its name!
 * 1. Why someone thinks of it: Whenever a stream splits dynamically at a moving boundary `k`
 *    that changes by at most `+/- 1` per step, two opposing heaps maintain that boundary in O(log N).
 * 2. Why it works: By induction on the two invariants:
 *    (a) `topBest.size() == i` (number of completed `get()` calls).
 *    (b) Every location in `topBest` is strictly better than every location in `remaining`.
 *    Therefore, `remaining.peek()` is ALWAYS the `(i + 1)`-th best location.
 *    Time Complexity:
 *    - `add`: O(log N) — one `offer` + one `poll` on `topBest` (size `i`) + one `offer` on `remaining` (size `N - i`).
 *    - `get`: O(log N) — one `poll` on `remaining` + one `offer` on `topBest`.
 *    Space Complexity: O(N) — every added `Location` lives in exactly one of the two heaps.
 *
 * --- Approach 4: Balanced BST (`TreeSet`) + Moving Rank Cursor (Distinct O(log N) Alternative) ---
 * 0. Idea/Intuition: Store all locations in a single `TreeSet<Location>` sorted best-to-worst,
 *    and keep a single reference `cursor` pointing to the location that will be returned by the
 *    NEXT `get()` (or `null` if all added locations have already been queried).
 *    - On `add(loc)`: Insert into `TreeSet`.
 *      - If `cursor == null`, the newly unqueried tail element is `set.last()` -> set `cursor = set.last()`.
 *      - Else if `loc` is strictly better than `cursor`, inserting `loc` ahead of `cursor` pushed
 *        `cursor`'s rank from `i+1` to `i+2` -> step `cursor` back by 1 via `cursor = set.lower(cursor)`.
 *    - On `get()`: Save `ans = cursor`, advance `cursor = set.higher(cursor)` (which is `null` if
 *      we just returned the last element), and return `ans.name()`.
 * 1. Why it works: Stepping a pointer by 1 in a Red-Black Tree (`lower`/`higher`) takes O(log N)
 *    without ever needing subtree sizes.
 * 2. Trade-off vs. Two Heaps: Same O(log N) time and O(N) space, and `TreeSet` easily extends to
 *    support deletions or range queries. However, `TreeSet` has higher constant factor (tree node
 *    allocations + rebalancing) and trickier `null` cursor edge cases than Two Heaps.
 *    Time Complexity: `add`: O(log N) (`TreeSet.add` + at most one `lower`/`last`).
 *    `get`: O(log N) (`TreeSet.higher`).
 *    Space Complexity: O(N) — one `TreeSet` node per added location.
 *
 * Interview Verdict:
 * Write Approach 3 (Two Opposing Heaps) in a 45-minute interview. The entire implementation is
 * literally 5 lines of heap calls with ZERO `if/else` branches and ZERO `null`-pointer edge cases,
 * making it nearly impossible to mess up under interview pressure. Mention Approach 4 (`TreeSet`
 * + cursor) if the interviewer asks about supporting arbitrary removals or score updates.
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - First `get()` after a single `add()` (`i = 1, N = 1`): `topBest` starts empty; `add()` bounces
 *   the element through `topBest` into `remaining`; `get()` moves it to `topBest` leaving `remaining` empty.
 * - Multiple consecutive `get()` calls with no `add()` in between: `remaining` drains smoothly
 *   one-by-one into `topBest` (tests `remaining` becoming empty after the final `get()`).
 * - Returning the SAME location multiple times in a row: Adding better locations between `get()`
 *   calls demotes the old `i`-th best back into `remaining`, so the next `get()` returns it again!
 * - Score ties with prefix names (e.g., `"alp"` vs `"alps"` vs `"alpine"`): Lexicographical order
 *   puts `"alp"` < `"alpine"` < `"alps"`.
 * - Inserting a location BETTER than everything when `remaining` is currently empty (`i == N`):
 *   In Two Heaps, `topBest.offer(newBest)` pushes it in and `topBest.poll()` demotes the old
 *   worst of `topBest` into `remaining` automatically! (In `TreeSet`, this is the `cursor == null` case.)
 * - Inserting a location WORSE than everything: Bounces straight to `remaining` without disturbing `topBest`.
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "Since `get()` only asks for ranks `1, 2, 3, ...` one step at a time,
 * do I need to know the full sorted order, or just the single moving boundary between rank `i`
 * and rank `i + 1`?"
 * Correctness intuition: If `topBest` holds the exact `i` best locations (with the WORST of them
 * at the root) and `remaining` holds the rest (with the BEST of them at the root), then:
 *   - Any new location can be routed in O(log N) by pushing into `topBest` and demoting `topBest`'s
 *     worst to `remaining` (preserving both size `i` and the partition invariant!).
 *   - The `(i+1)`-th best location is ALWAYS sitting right at `remaining.peek()`.
 *
 * -- Examples & ASCII Diagram --
 * Partition view of Two Opposing Heaps after 3 `get()` calls (`i = 3`):
 *
 *     BEST  <------------------------------------------>  WORST
 *     [ branford:3, alps:2, bradford:2 ] | [ orland:2 ]
 *     \_______________ ________________/   \____ _____/
 *                     v                         v
 *            topBest (size = i = 3)      remaining (size = N - i)
 *            root = WORST of topBest     root = BEST of remaining
 *                 = bradford:2                = orland:2
 *
 *   Now `add("orlando", 3)` arrives:
 *     1. Push `orlando:3` into `topBest` -> {branford:3, orlando:3, alps:2, bradford:2} (size 4)
 *     2. Poll WORST of `topBest` (`bradford:2`) -> push into `remaining`!
 *
 *     [ branford:3, orlando:3, alps:2 ] | [ bradford:2, orland:2 ]
 *                                                ^
 *                                       4th `get()` returns bradford:2 AGAIN!
 *
 * -- Dry run 1: Optimal Two Heaps on LC Example 1 --
 *   Step | Operation          | topBest (worst at root)           | remaining (best at root)     | Output
 *   -----+--------------------+-----------------------------------+------------------------------+---------
 *    1   | add(bradford, 2)   | [] (bounced via topBest)          | [bradford:2]                 | -
 *    2   | add(branford, 3)   | []                                | [branford:3, bradford:2]     | -
 *    3   | get() (#1)         | [branford:3]                      | [bradford:2]                 | branford
 *    4   | add(alps, 2)       | [branford:3]                      | [alps:2, bradford:2]         | -
 *    5   | get() (#2)         | [alps:2, branford:3]              | [bradford:2]                 | alps
 *    6   | add(orland, 2)     | [alps:2, branford:3]              | [bradford:2, orland:2]       | -
 *    7   | get() (#3)         | [bradford:2, alps:2, branford:3]  | [orland:2]                   | bradford
 *    8   | add(orlando, 3)    | [alps:2, orlando:3, branford:3]   | [bradford:2, orland:2]       | -
 *    9   | get() (#4)         | [bradford:2, alps:2, ...] (sz 4)  | [orland:2]                   | bradford
 *   10   | add(alpine, 2)     | [alps:2, alpine:2, ...]   (sz 4)  | [bradford:2, orland:2]       | -
 *   11   | get() (#5)         | [bradford:2, alps:2, ...] (sz 5)  | [orland:2]                   | bradford
 *   12   | get() (#6)         | [orland:2, bradford:2, ...] (sz 6)| []                           | orland
 *
 * -- Dry run 2: Edge Case — `add()` of New Global Best When `remaining` is Empty --
 *   Calls: add(b, 5), get(), add(a, 10), get()
 *   Step | Operation   | topBest (worst at root) | remaining (best at root) | Output
 *   -----+-------------+-------------------------+--------------------------+-------
 *    1   | add(b, 5)   | []                      | [b:5]                    | -
 *    2   | get() (#1)  | [b:5]                   | []                       | b
 *    3   | add(a, 10)  | [a:10] (b:5 demoted!)   | [b:5]                    | -
 *    4   | get() (#2)  | [b:5, a:10]             | []                       | b
 *   Notice how `add(a, 10)` smoothly demotes `b:5` into the empty `remaining` heap, so the 2nd
 *   `get()` returns `b` (now 2nd best behind `a`) with zero special-case code!
 *
 * -- Pitfalls --
 * 1. Flipping the tie-breaker comparator: Higher `score` is better (`b.score` vs `a.score`),
 *    but SMALLER `name` is better (`a.name.compareTo(b.name)`). Reversing the whole comparator
 *    manually instead of calling `.reversed()` is a super common bug source! Always define ONE
 *    canonical `BETTER_FIRST` comparator and derive `WORST_FIRST = BETTER_FIRST.reversed()`.
 * 2. Flipping which heap is `topBest` vs `remaining`: `topBest` needs its WORST element at the
 *    root (`WORST_FIRST`); `remaining` needs its BEST element at the root (`BETTER_FIRST`).
 *    Mental check: the two roots face each other across the rank `i | i+1` boundary!
 * 3. Using `b.score - a.score` instead of `Integer.compare(b.score, a.score)`: Safe for `[1, 10^5]`,
 *    but an overflow trap in Java if scores ever span full 32-bit `int` range.
 * 4. Discarding elements after `get()` (treating it like a single Max-Heap): Fails as soon as
 *    a higher-scoring location is added after a `get()`, because the previously returned element
 *    shifted down a rank and needs to be returned again!
 *
 * -- Pattern recognition --
 * When you see: "Dynamic stream + maintain a single moving rank boundary `k` that only changes
 * by `+/- 1` per operation"
 * -> Think **Two Opposing Heaps** (roots facing each other at the boundary).
 * Transfers directly to:
 *   - Find Median from Data Stream (LeetCode 295) — boundary `k = N / 2` moves by 0 or +1 per `add`
 *   - Kth Largest Element in a Stream (LeetCode 703) — degenerate case where `k` is fixed so
 *     the `remaining` heap isn't even needed!
 *
 * -- Interview script --
 * "A brute-force list re-sorts on every `get()`, taking O(N log N) per query, and a maintained
 * sorted `ArrayList` still pays O(N) to shift elements on `add()`. The key observation is that
 * `get()` doesn't query arbitrary ranks — it strictly queries rank 1, 2, 3, ..., `i` sequentially.
 * So we only need to maintain ONE moving partition boundary between the top `i` best locations
 * and the rest. I'll use two opposing heaps: `topBest` holding the `i` best locations with the
 * worst of them at the root, and `remaining` holding all others with the best of them at the root.
 * On `add()`, I push the new location into `topBest` and immediately move `topBest`'s worst into
 * `remaining` — keeping `topBest`'s size fixed at `i` with zero branching. On `get()`, the
 * `(i+1)`-th best is always `remaining.peek()`, so I move it to `topBest` and return its name.
 * That gives us O(log N) time for both `add` and `get`, and O(N) space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: What if we also support `remove(String name)` or `updateScore(String name, int newScore)`?
 * A1: Two Heaps struggle with arbitrary mid-heap removals (`PriorityQueue.remove` is O(N)).
 *     Switch to Approach 4 (`TreeSet<Location>` + `cursor`) paired with a `HashMap<String, Location>`
 *     mapping `name -> Location`. On `remove(name)`, look up the `Location` in O(1), adjust
 *     `cursor` by one step (`higher` or `lower`) if the removed element was at or better than
 *     `cursor`, and remove from the `TreeSet` in O(log N). `updateScore` is just `remove` + `add`.
 *
 * Q2: What if `get(int k)` can query ANY arbitrary rank `k` (not strictly `1, 2, 3, ...`)?
 * A2: Two Heaps can only shift the boundary by 1 in O(log N); jumping from rank 1 to rank 10,000
 *     would take O(N log N). For arbitrary `k`-th order statistics, use an **Order Statistic Tree**
 *     (augmented Red-Black Tree / Treap storing subtree sizes) for O(log N) `add` and `get(k)`.
 *     (Alternatively, since `1 <= score <= 10^5`, a Fenwick Tree over scores finds the score bucket
 *     in O(log S), plus a balanced BST per score for tie-breaking by `name`.)
 *
 * Q3: What if `get()` is called 100x more frequently than `add()`, or vice-versa?
 * A3: Both Two Heaps and `TreeSet` are O(log N) for both ops. If `add()` is tiny (e.g., 100 items)
 *     and `get()` is massive, Approach 2 (Sorted `ArrayList`) actually gives O(1) `get()` with
 *     superior cache locality. Conversely, if `add()` vastly outnumbers `get()` (e.g., 10^6 adds
 *     and only 5 `get()`s), `topBest` stays tiny (`size <= 5`), so `add()` spends almost all its
 *     time in `remaining` — or you can even keep `topBest` empty until `get()` is called!
 * =========================================================================================
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeSet;

public class SequentiallyOrdinalRankTrackerStudyNote {

    // Shared immutable domain object representing a scenic location.
    private record Location(String name, int score) {}

    // Canonical comparator: BETTER location comes FIRST (smaller in comparator order).
    // 1. Higher score is better -> compare b.score vs a.score (descending).
    // 2. On score tie, lexicographically smaller name is better -> a.name vs b.name (ascending).
    // Example: (branford, 3) < (alps, 2) < (bradford, 2) < (orland, 2).
    private static final Comparator<Location> BETTER_FIRST = (a, b) -> {
        if (a.score() != b.score()) {
            return Integer.compare(b.score(), a.score());
        }
        return a.name().compareTo(b.name());
    };

    // Exact reverse: WORST location comes FIRST. Derived via .reversed() so tie-breaking
    // logic can never drift out of sync with BETTER_FIRST!
    private static final Comparator<Location> WORST_FIRST = BETTER_FIRST.reversed();

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force (O(1) add, O(N log N) get, O(N) space)
    // ----------------------------------------------------------------------
    static class SORTrackerBruteForce {
        private final List<Location> locations = new ArrayList<>();
        private int queryCount = 0; // Number of get() calls completed so far.

        public void add(String name, int score) {
            locations.add(new Location(name, score));
        }

        public String get() {
            // Re-sort the entire list from best to worst on every single query.
            locations.sort(BETTER_FIRST);
            return locations.get(queryCount++).name();
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Maintained Sorted ArrayList (O(N) add due to shift, O(1) get, O(N) space)
    // ----------------------------------------------------------------------
    static class SORTrackerSortedList {
        // Always kept sorted in BETTER_FIRST order.
        // Example after adding bradford:2, branford:3, alps:2:
        //   sorted = [branford:3, alps:2, bradford:2]
        private final List<Location> sorted = new ArrayList<>();
        private int queryCount = 0;

        public void add(String name, int score) {
            Location loc = new Location(name, score);
            // Names are unique, so binarySearch always returns -(insertionPoint) - 1.
            int idx = Collections.binarySearch(sorted, loc, BETTER_FIRST);
            if (idx < 0) {
                idx = -(idx + 1);
            }
            // Bottleneck: shifting up to N elements right in the backing array costs O(N).
            sorted.add(idx, loc);
        }

        public String get() {
            return sorted.get(queryCount++).name();
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Two Opposing Heaps (OPTIMAL: O(log N) add, O(log N) get, O(N) space)
    // ----------------------------------------------------------------------
    static class SORTrackerTwoHeaps {
        // Holds the exact `i` best locations seen so far (where `i` = completed get() calls).
        // Ordered WORST_FIRST so topBest.peek() is the i-th best (the boundary element).
        // Example after 2 gets on {branford:3, alps:2, bradford:2}:
        //   topBest   = [alps:2 (root), branford:3]
        private final PriorityQueue<Location> topBest = new PriorityQueue<>(WORST_FIRST);

        // Holds all remaining (N - i) locations.
        // Ordered BETTER_FIRST so remaining.peek() is the (i + 1)-th best (next get() answer!).
        // Example continuing above:
        //   remaining = [bradford:2 (root)]
        private final PriorityQueue<Location> remaining = new PriorityQueue<>(BETTER_FIRST);

        public void add(String name, int score) {
            // Route every new location through `topBest` and demote `topBest`'s worst to `remaining`.
            // - Preserves `topBest.size() == i` (even when i == 0!).
            // - Preserves the invariant that every item in `topBest` is better than every item in `remaining`.
            topBest.offer(new Location(name, score));
            remaining.offer(topBest.poll());
        }

        public String get() {
            // The (i + 1)-th best location sits right at the root of `remaining`.
            // Promote it into `topBest` so `topBest` now has size `i + 1` for the next query.
            Location ithBest = remaining.poll();
            topBest.offer(ithBest);
            return ithBest.name();
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 4: TreeSet + Moving Cursor (OPTIMAL ALTERNATIVE: O(log N) add & get, O(N) space)
    // ----------------------------------------------------------------------
    static class SORTrackerTreeSet {
        // Stores all locations in strict BETTER_FIRST order.
        // Example: [branford:3, alps:2, bradford:2, orland:2]
        private final TreeSet<Location> sortedSet = new TreeSet<>(BETTER_FIRST);

        // Points directly to the (i + 1)-th best location (the answer to the NEXT get() call),
        // or `null` if all currently added locations have already been queried (i == N).
        private Location nextToReturn = null;

        public void add(String name, int score) {
            Location loc = new Location(name, score);
            sortedSet.add(loc);

            if (nextToReturn == null) {
                // Previously i == N (all elements queried). Adding 1 element makes size N + 1,
                // so the (i + 1)-th best is guaranteed to be the worst element in the set!
                nextToReturn = sortedSet.last();
            } else if (BETTER_FIRST.compare(loc, nextToReturn) < 0) {
                // `loc` landed strictly AHEAD of `nextToReturn`, pushing `nextToReturn` down
                // from rank (i + 1) to rank (i + 2). Step the cursor back by 1 to stay at rank (i + 1).
                nextToReturn = sortedSet.lower(nextToReturn);
            }
            // If `loc` landed AFTER `nextToReturn`, ranks 1..(i + 1) didn't change -> cursor stays put.
        }

        public String get() {
            Location result = nextToReturn;
            // Advance cursor to rank (i + 2), or `null` if we just returned the last element.
            nextToReturn = sortedSet.higher(nextToReturn);
            return result.name();
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 4 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private record Op(String type, String name, int score) {
        static Op add(String name, int score) { return new Op("add", name, score); }
        static Op get() { return new Op("get", null, 0); }
    }

    private static void verifyCase(String caseName, List<Op> ops, List<String> expected) {
        SORTrackerBruteForce brute = new SORTrackerBruteForce();
        SORTrackerSortedList sorted = new SORTrackerSortedList();
        SORTrackerTwoHeaps heaps = new SORTrackerTwoHeaps();
        SORTrackerTreeSet tree = new SORTrackerTreeSet();

        List<String> rBrute = new ArrayList<>();
        List<String> rSorted = new ArrayList<>();
        List<String> rHeaps = new ArrayList<>();
        List<String> rTree = new ArrayList<>();

        for (Op op : ops) {
            if ("add".equals(op.type())) {
                brute.add(op.name(), op.score());
                sorted.add(op.name(), op.score());
                heaps.add(op.name(), op.score());
                tree.add(op.name(), op.score());
            } else {
                rBrute.add(brute.get());
                rSorted.add(sorted.get());
                rHeaps.add(heaps.get());
                rTree.add(tree.get());
            }
        }

        boolean pass = rBrute.equals(expected)
                && rSorted.equals(expected)
                && rHeaps.equals(expected)
                && rTree.equals(expected);

        System.out.printf("%-36s | %s | Output: %s%n",
                caseName, pass ? "PASS" : "FAIL", rHeaps);

        if (!pass) {
            System.out.println("  Expected: " + expected);
            System.out.println("  Brute:    " + rBrute);
            System.out.println("  Sorted:   " + rSorted);
            System.out.println("  TwoHeaps: " + rHeaps);
            System.out.println("  TreeSet:  " + rTree);
        }
    }

    public static void main(String[] args) {
        verifyCase("LC Example 1",
                List.of(
                        Op.add("bradford", 2), Op.add("branford", 3), Op.get(),
                        Op.add("alps", 2), Op.get(),
                        Op.add("orland", 2), Op.get(),
                        Op.add("orlando", 3), Op.get(),
                        Op.add("alpine", 2), Op.get(), Op.get()
                ),
                List.of("branford", "alps", "bradford", "bradford", "bradford", "orland"));

        verifyCase("Edge: Single add then get",
                List.of(Op.add("solo", 10), Op.get()),
                List.of("solo"));

        verifyCase("Edge: Add best when remaining empty",
                List.of(Op.add("b", 5), Op.get(), Op.add("a", 10), Op.get(), Op.add("c", 20), Op.get()),
                List.of("b", "b", "b"));

        verifyCase("Edge: All ties (prefix names)",
                List.of(
                        Op.add("alps", 5), Op.add("alpine", 5), Op.add("alp", 5),
                        Op.get(), Op.get(), Op.get()
                ),
                List.of("alp", "alpine", "alps"));

        verifyCase("Edge: Strictly worsening additions",
                List.of(
                        Op.add("a", 100), Op.get(),
                        Op.add("b", 50), Op.get(),
                        Op.add("c", 10), Op.get()
                ),
                List.of("a", "b", "c"));

        verifyCase("Edge: Batch adds then drain all gets",
                List.of(
                        Op.add("z", 1), Op.add("a", 1), Op.add("m", 5), Op.add("b", 5),
                        Op.get(), Op.get(), Op.get(), Op.get()
                ),
                List.of("b", "m", "a", "z"));
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Two Opposing Heaps around a moving rank boundary (`topBest` worst-at-root
 *   of size `i`, `remaining` best-at-root of size `N - i`).
 * - Key observation: Because `get()` only queries ranks `1, 2, 3, ...` sequentially (step +1),
 *   we never need full random-access indexing — only the single split between rank `i` and `i + 1`.
 * - Worth memorizing vs. understanding: Understand *why* `topBest.offer(loc); remaining.offer(topBest.poll());`
 *   maintains both size `i` and partition order without a single `if` statement; memorize defining
 *   one canonical `BETTER_FIRST` comparator and deriving `WORST_FIRST = BETTER_FIRST.reversed()`.
 * - Most common trap: Flipping the tie-breaker direction (score DESC, name ASC) or assuming a
 *   location can be discarded after `get()` returns it.
 * - One-line mental trigger: "Querying 1st, 2nd, 3rd... best in a stream? Two opposing heaps: bounce adds through `topBest` into `remaining`, promote `remaining.poll()` on `get()`."
 * =========================================================================================
 */










































/*
private static final Comparator<Location> BEST_FIRST =
        Comparator.comparingInt(Location::score)
                  .reversed()
                  .thenComparing(Location::name);
*/
import java.util.*;

class SORTracker {

    /*
     * Immutable representation of a scenic location.
     *
     * Records are useful here because:
     * 1. We only need to store name and score.
     * 2. Both fields are immutable.
     * 3. Records automatically provide accessors.
     */
    record Location(String name, int score) {}

    /*
     * Comparator representing best-to-worst ranking.
     *
     * Ranking rules:
     *
     * 1. Higher score is better.
     * 2. If scores are equal, lexicographically smaller name is better.
     *
     * Example:
     *
     * (branford, 3)
     * (alps, 2)
     * (bradford, 2)
     *
     * Ranking:
     * branford -> alps -> bradford
     *
     * Java PriorityQueue places the smallest element according
     * to the comparator at its head.
     *
     * Therefore, this comparator makes the BEST location the root.
     */
    private static final Comparator<Location> BEST_FIRST =
            (a, b) -> {

                /*
                 * Reverse score comparison:
                 * Higher score should come first.
                 *
                 * Use Integer.compare instead of subtraction
                 * to avoid potential integer overflow.
                 */
                int scoreComparison =
                        Integer.compare(b.score(), a.score());

                if (scoreComparison != 0) {
                    return scoreComparison;
                }

                /*
                 * Equal scores:
                 * Lexicographically smaller name comes first.
                 */
                return a.name().compareTo(b.name());
            };

    /*
     * HEAP 1: best
     *
     * Contains the first k best locations, where k is the number
     * of get() calls performed so far.
     *
     * We reverse BEST_FIRST because we want the WORST location
     * among these k locations at the root.
     *
     * Why?
     *
     * Suppose best contains:
     *
     * A -> B -> C
     *
     * where A is best and C is worst.
     *
     * We want C at the root because C represents the current
     * kth best location.
     *
     * This is effectively a max-heap with respect to BEST_FIRST.
     */
    private final PriorityQueue<Location> best =
            new PriorityQueue<>(BEST_FIRST.reversed());

    /*
     * HEAP 2: remaining
     *
     * Contains all locations ranked after the first k locations.
     *
     * BEST_FIRST ensures that the BEST remaining location
     * is always at the root.
     */
    private final PriorityQueue<Location> remaining =
            new PriorityQueue<>(BEST_FIRST);

    /*
     * Number of get() calls made so far.
     *
     * This is also the number of locations that must be stored
     * inside the best heap.
     */
    private int queryCount = 0;

    public SORTracker() {

        /*
         * Both priority queues are initially empty.
         *
         * Initially:
         * best      = []
         * remaining = []
         * queryCount = 0
         */
    }

    public void add(String name, int score) {

        /*
         * Step 1:
         *
         * Every newly added location initially goes into remaining.
         *
         * Why?
         *
         * We already have queryCount locations in best.
         * Those locations represent the first queryCount ranks.
         *
         * A new location may or may not belong to those ranks.
         * Therefore, insert it into remaining first.
         */
        remaining.offer(new Location(name, score));

        /*
         * Step 2:
         *
         * If best is empty, there is no partition boundary yet.
         * No rebalancing is required.
         */
        if (best.isEmpty()) {
            return;
        }

        /*
         * Step 3:
         *
         * Examine the two boundary elements.
         *
         * best.peek():
         * Worst location among the first queryCount locations.
         *
         * remaining.peek():
         * Best location among all remaining locations.
         *
         * If the best remaining location is better than
         * the worst selected location, our partition is invalid.
         *
         * Example:
         *
         * best      = [A, B]
         * remaining = [C, D]
         *
         * Suppose X is newly added and ranking becomes:
         *
         * X, A, B, C, D
         *
         * Correct partition:
         *
         * best      = [X, A]
         * remaining = [B, C, D]
         *
         * So we must swap the boundary elements.
         */
        if (BEST_FIRST.compare(
                remaining.peek(),
                best.peek()
        ) < 0) {

            /*
             * Remove the best element from remaining.
             */
            Location better = remaining.poll();

            /*
             * Remove the worst element from best.
             */
            Location worse = best.poll();

            /*
             * Move the better element into best.
             *
             * This restores the invariant that best contains
             * the first queryCount best locations.
             */
            best.offer(better);

            /*
             * Move the displaced worse element into remaining.
             */
            remaining.offer(worse);
        }

        /*
         * Why is only one swap necessary?
         *
         * Before add(), both heaps already satisfied the partition
         * invariant.
         *
         * We insert exactly one new location.
         *
         * Therefore, at most one new location can cross
         * the existing partition boundary.
         */
    }

    public String get() {

        /*
         * Step 1:
         *
         * The next requested rank is queryCount + 1.
         *
         * We need to move the best remaining location into best.
         *
         * remaining.peek() is exactly the best location
         * that has not yet been returned.
         */
        Location next = remaining.poll();

        /*
         * Step 2:
         *
         * Add the selected location to best.
         *
         * best now contains the first queryCount + 1 best locations.
         */
        best.offer(next);

        /*
         * Step 3:
         *
         * Increment the number of queries.
         */
        queryCount++;

        /*
         * Step 4:
         *
         * Return the worst element inside best.
         *
         * Since best contains exactly the first queryCount
         * best locations, its worst element is the
         * queryCount-th best location.
         *
         * Therefore, this is exactly the requested answer.
         */
        return best.peek().name();
    }
}

/*
 * ============================================================
 * COMPLEXITY ANALYSIS
 * ============================================================
 *
 * Let N = number of locations added so far.
 *
 * Let Q = number of get() calls so far.
 *
 * INVARIANTS:
 *
 * 1. best.size() == Q.
 *
 * 2. best contains the Q best locations overall.
 *
 * 3. remaining contains all other locations.
 *
 * 4. Every element in best is at least as good as every
 *    element in remaining.
 *
 * 5. best.peek() is the worst element in best.
 *
 * 6. remaining.peek() is the best element in remaining.
 *
 *
 * add():
 *
 * 1. remaining.offer():
 *    O(log N).
 *
 * 2. Comparing heap roots:
 *    O(1).
 *
 * 3. If rebalancing is necessary:
 *    Two poll() operations and two offer() operations.
 *    Each takes O(log N).
 *
 * Total: O(log N).
 *
 * Importantly, only one swap is required per add().
 *
 *
 * get():
 *
 * 1. remaining.poll():
 *    O(log N).
 *
 * 2. best.offer():
 *    O(log N).
 *
 * 3. best.peek():
 *    O(1).
 *
 * 4. Increment queryCount:
 *    O(1).
 *
 * Total: O(log N).
 *
 *
 * SPACE:
 *
 * Every location exists in exactly one of the two heaps.
 *
 * best.size() + remaining.size() == N.
 *
 * Therefore:
 *
 * Total auxiliary space = O(N).
 *
 *
 * FINAL COMPLEXITIES:
 *
 * add()  -> O(log N)
 * get()  -> O(log N)
 * Space  -> O(N)
 */
