/**
 * 0. ## Problem Statement
 * Design a class `KthLargest` that maintains a stream of integer test scores and
 * continuously returns the `k`-th largest score (in sorted order, including duplicates)
 * after each new score is added.
 *
 * Implement:
 * - `KthLargest(int k, int[] nums)`: Initializes the object with integer `k` and initial scores `nums`.
 * - `int add(int val)`: Appends `val` to the stream and returns the `k`-th largest element so far.
 *
 * Example:
 *   k = 3, nums = [4, 5, 8, 2]
 *   add(3)  -> 4  (sorted: [2, 3, 4, 5, 8]       -> 3rd largest is 4)
 *   add(5)  -> 5  (sorted: [2, 3, 4, 5, 5, 8]    -> 3rd largest is 5)
 *   add(10) -> 5  (sorted: [2, 3, 4, 5, 5, 8, 10]-> 3rd largest is 5)
 *   add(9)  -> 8
 *   add(4)  -> 8
 *
 * Constraints:
 * - 0 <= nums.length <= 10^4
 * - 1 <= k <= nums.length + 1
 * - -10^4 <= nums[i], val <= 10^4
 * - At most 10^4 calls will be made to `add`.
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Does 'k-th largest' mean the k-th element in sorted order (counting duplicates), or the k-th DISTINCT score?"
 *    -> Prevents using a Set/TreeSet that collapses duplicate scores (Example 2 confirms duplicates count).
 * 2. "Can `nums` initially have fewer than `k` elements (e.g., empty array when `k = 1`)?"
 *    -> Prevents assuming `nums.length >= k` in the constructor; `1 <= k <= nums.length + 1` means `nums` can have `k - 1` elements and only reaches `k` on the first `add()`.
 * 3. "Will scores ever be removed or updated after being added?"
 *    -> Crucial! Append-only means the k-th largest can NEVER decrease, so anything smaller than it can be permanently discarded.
 * 4. "Can `k` change dynamically between `add()` calls?"
 *    -> If `k` were dynamic, we couldn't discard smaller elements and would need an Order Statistic Tree or full sorted structure instead of a size-k heap.
 * 5. "Can scores be negative, or near `Integer.MIN_VALUE` / `Integer.MAX_VALUE`?"
 *    -> Prevents initializing sentinels to `0` and prevents overflow in custom `(a, b) -> a - b` comparators.
 * 6. "Are we allowed to mutate the input `nums` array passed to the constructor?"
 *    -> Clarifies whether we can sort `nums` in-place or must treat it as read-only.
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to `N = 10^4` initial scores and `M = 10^4` calls to `add()`.
 * If every `add()` re-sorts all `S` elements seen so far, total work is O(M * (N + M) log(N + M))
 * (~2.8 * 10^9 operations), which blows past the time budget. And in a real admissions stream
 * with millions of applicants, storing all historical scores wastes O(N + M) memory.
 *
 * --- Approach 1: Brute Force (Append + Full Re-Sort) ---
 * 0. Idea/Intuition: Keep every score in an `ArrayList`. On each `add(val)`, append `val`,
 *    sort the entire list ascending, and return the element at `size - k`.
 * 1. What I'd naturally try: Literally mirror "k-th highest score in the sorted list of all scores."
 * 2. Why it works: Sorting guarantees index `size - k` is always the exact k-th largest element.
 * 3. Why it's too slow/costly: Sorting takes O(S log S) per call where `S` grows up to `N + M`,
 *    and we store every score ever submitted in memory.
 * 4. What work is being repeated: The list was ALREADY sorted before `val` arrived! Re-sorting
 *    all `S` elements just to place ONE new number redoes O(S log S) comparisons unnecessarily.
 * 5. Observation that removes the bottleneck: Inserting one number into an already-sorted list
 *    only requires finding its insertion index via Binary Search.
 * 6. Leads to Approach 2: Sort once in the constructor, then binary-search + insert on each `add()`.
 *    Time Complexity: Constructor O(N) — copies N elements into a list.
 *    `add`: O(S log S) — because `Collections.sort()` sorts all `S` elements seen so far on every call.
 *    Space Complexity: O(S) — because the `ArrayList` retains all `S = N + M` historical scores.
 *
 * --- Approach 2: Maintained Sorted ArrayList (Binary Search + Insert) ---
 * 0. Idea/Intuition: Sort the initial `nums` once. On `add(val)`, binary search for the correct
 *    index in O(log S), insert `val` there, and read index `size - k` in O(1).
 * 1. What I'd naturally try: "Don't re-sort — just binary search where `val` belongs and insert it!"
 * 2. Why it works: Maintaining sorted order as an invariant means `list.get(size - k)` is always
 *    the k-th largest in O(1).
 * 3. Why it's still too slow/costly: Even though finding the position takes O(log S), calling
 *    `ArrayList.add(idx, val)` must shift up to `S` elements to the right in memory — O(S) per call!
 *    Across `M = 10^4` calls, that's O(M * (N + M)) (~2 * 10^8 shifts) and still O(N + M) memory.
 * 4. What work is being repeated / wasted:
 *    - First, we are storing and shifting ALL elements smaller than the current k-th largest.
 *      Ask yourself: If `k = 3` and our top 3 scores are `[4, 5, 8]`, can `2` or `3` EVER become
 *      the 3rd largest score in the future? NO! Scores are only added, never removed, so the 3rd
 *      largest can only stay `4` or move UP. Anything smaller than `4` is permanently dead!
 *    - Second, even among the top `k` scores `[4, 5, 8]`, do we actually need them fully sorted?
 *      NO! We ONLY ever need two operations on the top `k` elements:
 *        (a) Peek the SMALLEST of the top `k` (that's the k-th largest!).
 *        (b) Evict that smallest element whenever a larger score arrives and takes its spot.
 * 5. Observation that removes the bottleneck: "Keep only the top `k` elements, and give me O(1)
 *    access to the MINIMUM of those `k` elements plus O(log K) insert/evict" is the exact
 *    specification of a **Min-Heap of fixed max size `k`**.
 * 6. Leads to Approach 3: Maintain a `PriorityQueue<Integer>` (min-heap) capped at size `k`.
 *    Time Complexity: Constructor O(N log N) — initial sort of N elements.
 *    `add`: O(S) — O(log S) for binary search, dominated by O(S) array shifting on `ArrayList.add(idx, val)`.
 *    Space Complexity: O(S) — because every historical score is stored in the `ArrayList`.
 *
 * --- Approach 3: Min-Heap of Size `k` (Optimal) ---
 * 0. Idea/Intuition: Maintain a min-heap holding ONLY the `k` largest scores seen so far.
 *    The root (`minHeap.peek()`) is the smallest of the top `k` — which IS the `k`-th largest!
 *    When `val` arrives:
 *    - If `minHeap.size() < k`, push `val`.
 *    - Else if `val > minHeap.peek()`, pop the old k-th largest and push `val`.
 *    - Else (`val <= minHeap.peek()`), ignore `val` completely in O(1)!
 *    Return `minHeap.peek()`.
 * 1. Why someone thinks of it: Flip the question from "find k-th largest among ALL elements" to
 *    "find the MINIMUM among the TOP k elements."
 * 2. Why it works: By induction, the heap always contains the exact `k` largest elements seen so
 *    far (once at least `k` elements exist). Any discarded element had at least `k` elements `>=`
 *    it, so it can never be the k-th largest in an append-only stream. The minimum of the top `k`
 *    is therefore the k-th largest.
 *    Time Complexity:
 *    - Constructor: O(N log K) — each of the N initial elements does at most one push/pop on a
 *      heap of size at most K.
 *    - `add`: O(log K) worst-case (when `val` enters the top K and triggers a heap sift), and
 *      O(1) fast-path when `minHeap.size() == k && val <= minHeap.peek()`.
 *    Space Complexity: O(K) — because the min-heap strictly caps its size at `k` elements,
 *    regardless of whether the stream receives 10^4 or 10^9 scores!
 *
 * Interview Verdict:
 * Write Approach 3 (Min-Heap of Size `k`) immediately in a 45-minute interview. It is ~12 lines
 * of crystal-clear Java, achieves optimal O(log K) update time and O(K) space, scales to
 * infinite streams, and has virtually zero bug surface.
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Empty initial array (`k = 1, nums = []`): Heap starts empty; first `add(5)` pushes `5` and returns `5`.
 * - Initial array has `k - 1` elements (`k = 3, nums = [4, 8]`): Heap has size 2 after constructor;
 *   first `add(2)` pushes `2` (size becomes 3) and returns `2` without polling!
 * - `k = 1`: Heap has max size 1, effectively acting as a running maximum tracker.
 * - Duplicate scores (`k = 4, nums = [7, 7, 7, 7, 8, 3]`): Min-heap holds `[7, 7, 7, 8]`; duplicates
 *   are preserved and popped one by one as larger numbers arrive.
 * - All negative scores (`k = 2, nums = [-10, -5, -2]`): Min-heap holds `[-5, -2]`, returns `-5`.
 * - Incoming `val` strictly smaller than `minHeap.peek()` when full: Must NOT alter the heap.
 * - Incoming `val` equal to `minHeap.peek()` when full: Ignoring it leaves the top `k` unchanged
 *   (still the exact same multiset of top `k` scores).
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "To find the `k`-th LARGEST element, why do I want a MIN-heap
 * instead of a MAX-heap?"
 * Correctness intuition: A max-heap gives you the 1st largest, burying the k-th largest deep
 * inside the tree. By keeping ONLY the top `k` elements in a MIN-heap, the k-th largest becomes
 * the *smallest* element in that elite group — sitting right at the root in O(1), ready both to
 * be read and to be evicted the moment a bigger score arrives.
 *
 * -- Examples & ASCII Diagram --
 * Example 1 (Typical): k = 3, nums = [4, 5, 8, 2]
 *
 *   Constructor processes [4, 5, 8, 2] with cap k = 3:
 *     add 4 -> heap: [4]
 *     add 5 -> heap: [4, 5]
 *     add 8 -> heap: [4, 5, 8]
 *     add 2 -> 2 <= peek(4) -> IGNORED! (2 can never enter top 3)
 *
 *   Min-Heap state after constructor (size = 3):
 *           [4]   <-- root = min of top 3 = 3rd largest!
 *          /   \
 *        [5]   [8]
 *
 *   Now add(10): 10 > peek(4) -> poll 4, offer 10:
 *           [5]   <-- new 3rd largest!
 *          /   \
 *        [8]   [10]
 *
 * Example 2 (Where a tempting wrong approach fails):
 *   Tempting mistake: "Use a MAX-heap of size `k`!"
 *   Input: k = 3, nums = [4, 5, 8, 2].
 *   - Push 4, 5, 8 -> Max-Heap root is 8.
 *   - Push 2 -> Heap size becomes 4 > 3 -> poll root -> EVICTS 8 (the largest score!) instead of 2!
 *   Why it fails: A max-heap of size `k` tracks the `k` SMALLEST elements (evicting large ones),
 *   which is the exact opposite of what we want.
 *
 * -- Dry run 1: Optimal Min-Heap on Example 1: k = 3, nums = [4, 5, 8, 2] --
 *   Call    | Action on Min-Heap (cap k=3)       | Heap Contents (sorted view) | Return (peek)
 *   --------+------------------------------------+-----------------------------+--------------
 *   init    | offer 4, 5, 8; ignore 2 (2 <= 4)   | [4, 5, 8]                   | -
 *   add(3)  | full & 3 <= peek(4) -> ignore      | [4, 5, 8]                   | 4
 *   add(5)  | 5 > peek(4) -> poll 4, offer 5     | [5, 5, 8]                   | 5
 *   add(10) | 10 > peek(5) -> poll 5, offer 10   | [5, 8, 10]                  | 5
 *   add(9)  | 9 > peek(5) -> poll 5, offer 9     | [8, 9, 10]                  | 8
 *   add(4)  | full & 4 <= peek(8) -> ignore      | [8, 9, 10]                  | 8
 *
 * -- Dry run 2: Edge Case — Initial size < k: k = 3, nums = [10, 5] --
 *   Call    | Action on Min-Heap (cap k=3)       | Heap Contents (sorted view) | Return (peek)
 *   --------+------------------------------------+-----------------------------+--------------
 *   init    | offer 10, offer 5 (size 2 < 3)     | [5, 10]                     | -
 *   add(2)  | size 2 < 3 -> offer 2 (no poll!)   | [2, 5, 10]                  | 2
 *   add(8)  | full & 8 > peek(2) -> poll 2, +8   | [5, 8, 10]                  | 5
 *
 * -- Pitfalls --
 * 1. Using a Max-Heap instead of a Min-Heap: The #1 classic trap ("Kth Largest -> Max-Heap?").
 *    Remember: **K-th Largest = Min-Heap of size K**; **K-th Smallest = Max-Heap of size K**.
 * 2. Calling `poll()` unconditionally in `add()` before checking `size > k`: Fails on the edge
 *    case where `nums.length == k - 1` (e.g., `nums = []`, `k = 1`).
 * 3. Using `TreeSet` instead of `PriorityQueue`: Silently drops duplicate test scores!
 * 4. Java-specific gotcha: Writing `new PriorityQueue<>((a, b) -> a - b)` instead of default
 *    `new PriorityQueue<>()`. Not only is the comparator redundant (natural order is already a
 *    min-heap), `a - b` overflows on extreme 32-bit integers like `Integer.MIN_VALUE - 1`.
 *
 * -- Pattern recognition --
 * When you see: "Maintain the Top K / K-th Largest (or Smallest) in a stream"
 * -> Think **Fixed-Size Opposite Heap** (Min-Heap of size K for K-th largest; Max-Heap of size K
 * for K-th smallest).
 * Transfers directly to:
 *   - Kth Largest Element in an Array (LeetCode 215) — static array version (Min-Heap or Quickselect)
 *   - Top K Frequent Elements (LeetCode 347) — Min-Heap of size K keyed by frequency
 *   - Find Median from Data Stream (LeetCode 295) — Two opposing heaps where `k = N / 2` grows dynamically
 *
 * -- Interview script --
 * "A brute-force approach keeps all scores and re-sorts on every `add()`, costing O(S log S) per
 * call and O(S) memory. Even maintaining a sorted list with binary search still pays O(S) to shift
 * elements on insertion. The key observation is that scores are never removed, so any score smaller
 * than our current k-th largest can never become the k-th largest in the future. We only need to
 * retain the top `k` scores — and the k-th largest is simply the MINIMUM of those top `k`. So I'll
 * use a min-heap capped at size `k`. In the constructor and in `add()`, if the heap has fewer than
 * `k` elements I push `val`; otherwise if `val > heap.peek()`, I replace the root. Then `heap.peek()`
 * is always the answer. That gives us O(N log K) constructor time, O(log K) time per `add()`
 * (O(1) when `val` doesn't beat the cutoff), and O(K) space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: What if applicants can WITHDRAW their scores (`remove(int val)` is added)?
 * A1: Discarding elements smaller than the k-th largest is no longer safe — if a top-k applicant
 *     withdraws, the (k+1)-th largest score must be promoted! We must retain all scores in two
 *     balanced structures: a `TreeMap` (or Min-Heap) for the top `k` and a `TreeMap` (or Max-Heap
 *     with lazy deletion) for the remaining scores below `k`, rebalancing whenever a removal hits
 *     the top-k set. Space becomes O(N), and operations take O(log N).
 *
 * Q2: What if `k` is passed per query (`getKthLargest(int k)`) instead of fixed at construction?
 * A2: Since any `k` from `1` to `N` could be queried, we cannot evict elements. If values are
 *     bounded (like `[-10^4, 10^4]` here), a **Fenwick Tree (Binary Indexed Tree)** over the
 *     value range `[0..20000]` supports O(log V) `add()` and O(log V) `getKthLargest(k)` via
 *     binary lifting! For unbounded values, use an augmented Order Statistic BST.
 *
 * Q3: What if `nums` has 10 million scores and `k = 100` — can we build the initial heap faster
 *     than O(N log K)?
 * A3: For `k << N`, O(N log K) is already very fast, AND because most incoming elements in random
 *     order are `<= minHeap.peek()`, our `val > minHeap.peek()` guard skips the O(log K) sift on
 *     nearly all elements (expected O(N + K log K log(N/K))). Alternatively, Quickselect finds the
 *     top `k` in O(N) expected time, then Floyd's heapify builds the size-k heap in O(K).
 * =========================================================================================
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public class KthLargestInStreamStudyNote {

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force (O(S log S) per add, O(S) space)
    // ----------------------------------------------------------------------
    static class KthLargestBruteForce {
        private final int k;
        // Stores every score ever seen in unsorted order until sorted in `add()`.
        // Example after init(3, [4, 5, 8, 2]): scores = [4, 5, 8, 2]
        private final List<Integer> scores = new ArrayList<>();

        public KthLargestBruteForce(int k, int[] nums) {
            this.k = k;
            for (int num : nums) {
                scores.add(num);
            }
        }

        public int add(int val) {
            scores.add(val);
            // Re-sort the entire history from scratch on every single call.
            Collections.sort(scores);
            // In an ascending sorted list of size S, the k-th largest sits at index S - k.
            return scores.get(scores.size() - k);
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Maintained Sorted ArrayList (O(S) per add due to shifts, O(S) space)
    // ----------------------------------------------------------------------
    static class KthLargestSortedList {
        private final int k;
        // Always kept in ascending sorted order.
        // Example after init(3, [4, 5, 8, 2]): sorted = [2, 4, 5, 8]
        private final List<Integer> sorted = new ArrayList<>();

        public KthLargestSortedList(int k, int[] nums) {
            this.k = k;
            for (int num : nums) {
                sorted.add(num);
            }
            Collections.sort(sorted); // Sort once upfront: O(N log N).
        }

        public int add(int val) {
            // Binary search finds either an exact match index, or -(insertionPoint) - 1.
            int idx = Collections.binarySearch(sorted, val);
            if (idx < 0) {
                // Convert negative return value back to the exact insertion index.
                idx = -(idx + 1);
            }
            // Bottleneck: ArrayList.add(idx, val) shifts all elements to the right -> O(S).
            sorted.add(idx, val);
            return sorted.get(sorted.size() - k);
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Min-Heap of Size k (OPTIMAL: O(log K) per add, O(K) space)
    // ----------------------------------------------------------------------
    static class KthLargestMinHeap {
        private final int k;

        // Java's PriorityQueue is a MIN-heap by default (smallest element at the root).
        // Invariant: holds ONLY the `k` largest elements seen so far (or all elements if < k).
        // Example for k = 3 after [4, 5, 8, 2]:
        //   minHeap = [4, 5, 8] -> peek() is 4 (the 3rd largest overall!).
        private final PriorityQueue<Integer> minHeap;

        public KthLargestMinHeap(int k, int[] nums) {
            this.k = k;
            // Pre-size capacity to k to avoid internal array resizing.
            this.minHeap = new PriorityQueue<>(k);

            // Reuse the exact same bounded-insertion logic for all initial scores.
            for (int num : nums) {
                offerBounded(num);
            }
        }

        public int add(int val) {
            offerBounded(val);
            // The problem guarantees at least `k` elements exist after every `add()` call,
            // so the root of our size-k min-heap is strictly the k-th largest score.
            return minHeap.peek();
        }

        private void offerBounded(int val) {
            if (minHeap.size() < k) {
                // Haven't reached k elements yet; every incoming score belongs in the top k.
                minHeap.offer(val);
            } else if (val > minHeap.peek()) {
                // Incoming score beats the current k-th largest cutoff:
                // evict the old cutoff and insert `val` into the top-k pool.
                // (If val <= peek(), we do nothing -> O(1) fast-path, zero heap sifts!)
                minHeap.poll();
                minHeap.offer(val);
            }
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 3 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private static void verifyCase(String name, int k, int[] initNums, int[] adds, int[] expected) {
        KthLargestBruteForce brute = new KthLargestBruteForce(k, initNums);
        KthLargestSortedList sorted = new KthLargestSortedList(k, initNums);
        KthLargestMinHeap heap = new KthLargestMinHeap(k, initNums);

        int[] resBrute = new int[adds.length];
        int[] resSorted = new int[adds.length];
        int[] resHeap = new int[adds.length];

        for (int i = 0; i < adds.length; i++) {
            resBrute[i] = brute.add(adds[i]);
            resSorted[i] = sorted.add(adds[i]);
            resHeap[i] = heap.add(adds[i]);
        }

        boolean pass = Arrays.equals(resBrute, expected)
                && Arrays.equals(resSorted, expected)
                && Arrays.equals(resHeap, expected);

        System.out.printf("%-32s | %s | Output: %s%n",
                name, pass ? "PASS" : "FAIL", Arrays.toString(resHeap));

        if (!pass) {
            System.out.println("  Expected: " + Arrays.toString(expected));
            System.out.println("  Brute:    " + Arrays.toString(resBrute));
            System.out.println("  Sorted:   " + Arrays.toString(resSorted));
            System.out.println("  MinHeap:  " + Arrays.toString(resHeap));
        }
    }

    public static void main(String[] args) {
        verifyCase("LC Example 1",
                3, new int[]{4, 5, 8, 2},
                new int[]{3, 5, 10, 9, 4},
                new int[]{4, 5, 5, 8, 8});

        verifyCase("LC Example 2 (duplicates)",
                4, new int[]{7, 7, 7, 7, 8, 3},
                new int[]{2, 10, 9, 9},
                new int[]{7, 7, 7, 8});

        verifyCase("Edge: Empty init, k = 1",
                1, new int[]{},
                new int[]{-3, -2, -4, 0, 4},
                new int[]{-3, -2, -2, 0, 4});

        verifyCase("Edge: Init size == k - 1",
                3, new int[]{10, 5},
                new int[]{2, 8, 12, 1},
                new int[]{2, 5, 8, 8});

        verifyCase("Edge: All negative scores",
                2, new int[]{-10, -5, -2},
                new int[]{-20, -3, -1},
                new int[]{-5, -3, -2});

        verifyCase("Edge: All identical scores",
                3, new int[]{6, 6, 6},
                new int[]{6, 6, 6},
                new int[]{6, 6, 6});
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Fixed-Size Opposite Heap (Min-Heap of size `k` for K-th Largest).
 * - Key observation: In an append-only stream, the k-th largest can only increase; anything
 *   smaller than it is permanently dead, and the k-th largest is simply the MINIMUM of the top `k`.
 * - Worth memorizing vs. understanding: Understand *why* it's a MIN-heap for k-th LARGEST
 *   (so you never flip it under pressure); memorize checking `size < k` before comparing `val > peek()`.
 * - Most common trap: Using a Max-Heap (evicts the largest elements!) or a `TreeSet` (drops duplicates).
 * - One-line mental trigger: "K-th largest in a stream? Cap a MIN-heap at size K and peek the root."
 * =========================================================================================
 */


































import java.util.PriorityQueue;

class KthLargest {

    /*
     * We only maintain the K largest elements.
     *
     * Why PriorityQueue?
     * Java's PriorityQueue implements a Min Heap by default.
     *
     * The smallest element is always available at the root.
     *
     * Our invariant:
     * The heap contains at most K largest elements encountered so far.
     *
     * Therefore:
     * heap.peek() = kth largest element, when heap size >= K.
     */
    private final PriorityQueue<Integer> minHeap;

    /*
     * Fixed rank that we need to maintain.
     */
    private final int k;

    public KthLargest(int k, int[] nums) {

        this.k = k;

        /*
         * Create a Min Heap.
         *
         * We don't need to specify a custom comparator because
         * PriorityQueue<Integer> is already a Min Heap.
         *
         * Initial heap size = 0.
         */
        this.minHeap = new PriorityQueue<>();

        /*
         * Process all initial elements using the same add() logic.
         *
         * This is useful because we don't need separate initialization
         * logic and insertion logic.
         *
         * Every element is processed while preserving our invariant.
         */
        for (int num : nums) {
            add(num);
        }
    }

    public int add(int val) {

        /*
         * Step 1: Insert the incoming value.
         *
         * Insertion into a Min Heap takes O(log K)
         * when the heap contains at most K + 1 elements.
         */
        minHeap.offer(val);

        /*
         * Step 2: Ensure heap size does not exceed K.
         *
         * If heap size becomes K + 1:
         *
         * Remove the smallest element.
         *
         * Why?
         *
         * We only want the K largest elements.
         * The smallest among K + 1 elements cannot belong
         * to the K largest elements.
         *
         * poll() removes the root in O(log K).
         */
        if (minHeap.size() > k) {
            minHeap.poll();
        }

        /*
         * Step 3: Return the root.
         *
         * The heap contains:
         * - All elements, if fewer than K elements exist.
         * - Exactly K largest elements, otherwise.
         *
         * When the heap contains K elements,
         * its minimum is the kth largest element.
         *
         * peek() takes O(1).
         */
        return minHeap.peek();
    }
}







