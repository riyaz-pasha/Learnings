/**
 * 0. ## Problem Statement
 * You are given an array of integers `nums` and an integer `limit`.
 * Return the size of the longest non-empty contiguous subarray such that the absolute
 * difference between EVERY pair of elements in this subarray is less than or equal to `limit`.
 *
 * Examples:
 *   nums = [8, 2, 4, 7], limit = 4         -> Output: 2  (subarrays [2,4] or [4,7])
 *   nums = [10, 1, 2, 4, 7, 2], limit = 5  -> Output: 4  (subarray [2,4,7,2])
 *   nums = [4, 2, 2, 2, 4, 4, 2, 2], lim=0 -> Output: 3  (subarray [2,2,2])
 *
 * Constraints:
 * - 1 <= nums.length <= 10^5
 * - 1 <= nums[i] <= 10^9
 * - 0 <= limit <= 10^9
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can `nums` contain negative numbers or values near `Integer.MIN_VALUE` / `Integer.MAX_VALUE`?"
 *    -> Prevents signed 32-bit overflow in `max - min` (if negatives up to -10^9 or MIN_VALUE are allowed, we must cast to `long`).
 * 2. "Can `limit` be 0 or negative?"
 *    -> `limit == 0` means only contiguous runs of equal elements qualify; a negative `limit` would mean no valid subarray exists (return 0).
 * 3. "Is a single-element subarray always valid (assuming `limit >= 0`)?"
 *    -> Confirms the lower bound of the answer is 1 (since `|x - x| = 0 <= limit`) and rules out empty-subarray ambiguity.
 * 4. "Do we only need the maximum length, or do we also need to return the subarray indices (and how to break ties)?"
 *    -> If only the length is needed, we can even avoid shrinking the window strictly (or just track `bestLen`); returning indices requires storing `bestLeft`.
 * 5. "Must the subarray be contiguous, or can it be a subsequence?"
 *    -> Contiguous enables sliding window; a subsequence would change the problem completely to sorting + two pointers.
 * 6. "Can the input array be empty?"
 *    -> Constraints guarantee `nums.length >= 1`, preventing an unnecessary guard or undefined return value.
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: `nums.length <= 10^5`. Any O(N^2) enumeration of all subarrays performs
 * ~5 * 10^9 operations in the worst case (TLE). We need O(N log N) or O(N).
 *
 * First, simplify the validity condition:
 * Checking "every pair `|a - b| <= limit`" sounds like O(L^2) per subarray of length L.
 * However, the largest possible absolute difference in ANY set of numbers is ALWAYS
 * `max(subarray) - min(subarray)`. So:
 *     (every pair differs by <= limit)  <==>  (subarrayMax - subarrayMin <= limit).
 *
 * --- Approach 1: Brute Force (All Subarrays with Running Min/Max) ---
 * 0. Idea/Intuition: Fix every start index `i`, expand the end index `j` to the right while
 *    maintaining `curMin` and `curMax`. Stop expanding `j` as soon as `curMax - curMin > limit`.
 * 1. What I'd naturally try: Nested loops `(i, j)` with running `min`/`max` and an early `break`.
 * 2. Why it works: Once `curMax - curMin > limit` for `[i..j]`, adding more elements to the
 *    right can only raise `curMax` or lower `curMin`, never fixing the violation. So every
 *    valid subarray starting at `i` is checked before breaking.
 * 3. Why it's too slow/costly: When `limit` is large (e.g., all elements within `limit`), the
 *    inner loop never breaks early and runs `N * (N + 1) / 2` times -> ~5 * 10^9 steps for N = 10^5.
 * 4. What work is being repeated: When `[i..j]` breaks and we move to `i + 1`, we throw away
 *    everything we know about `[i+1 .. j-1]` and recompute its min and max from scratch!
 * 5. Observation that removes the bottleneck: Monotonicity of validity! If `[left..right]` is
 *    invalid, we don't need to restart `right` at `left + 1`; we can keep `right` where it is
 *    and advance `left` until `windowMax - windowMin <= limit` again. That's a **Sliding Window**.
 *    The only thing we need is a structure that tells us the window's `min` and `max` as we add
 *    `nums[right]` and remove `nums[left]`.
 * 6. Leads to Approach 2: Maintain the active window elements in a balanced BST (`TreeMap<val, count>`).
 *    Time Complexity: O(N^2) — because in the worst case (e.g., all elements equal), the inner
 *    loop runs `N - i` times for every `i`.
 *    Space Complexity: O(1) auxiliary — because we only store scalar variables (`curMin`, `curMax`, `best`).
 *
 * --- Approach 2: Sliding Window + TreeMap (Balanced BST Frequency Map) ---
 * 0. Idea/Intuition: Keep a `TreeMap<Integer, Integer>` mapping each value in `[left..right]`
 *    to its frequency. Expand `right`, insert `nums[right]`. While `map.lastKey() - map.firstKey() > limit`,
 *    decrement `nums[left]` (removing the key if its count hits 0) and increment `left`.
 * 1. What I'd naturally try: "I need dynamic insert, arbitrary delete (`nums[left]`), and min/max
 *    queries -> `TreeMap`!" (Note: Two Heaps with Lazy Deletion is a sibling O(N log N) idea,
 *    but `TreeMap` avoids stale heap bloat and handles arbitrary removal cleanly.)
 * 2. Why it works: `TreeMap` keeps all active window values sorted, so `lastKey()` is always the
 *    exact window max and `firstKey()` is always the exact window min, even with duplicates.
 * 3. Why it's still suboptimal: Every `put`, `remove`, `firstKey`, and `lastKey` costs O(log N)
 *    and allocates tree nodes. We are paying a `log N` factor on every single step.
 * 4. What work is being repeated: `TreeMap` maintains the FULL sorted order of every element in
 *    the window — including elements in the middle that can NEVER be the window's max or min!
 *    Suppose `[..., 4, 7]` is in the window. As `left` moves right, `4` will always leave BEFORE `7`.
 *    So `4` can NEVER be the window maximum again (because `7` is both bigger and newer).
 * 5. Observation that removes the bottleneck: Just like Sliding Window Maximum (LC 239), we can
 *    discard dominated candidates immediately! Maintain TWO **Monotonic Deques** of indices:
 *    - `maxDeque` (decreasing values): front is always the window max.
 *    - `minDeque` (increasing values): front is always the window min.
 * 6. Leads to Approach 3: Replace the `TreeMap` with two `ArrayDeque<Integer>`s.
 *    Time Complexity: O(N log N) — because each element is inserted once and removed at most once
 *    from a `TreeMap` of size up to N, and each tree operation takes O(log N).
 *    Space Complexity: O(N) auxiliary — because in the worst case (entire array is valid with all
 *    distinct elements), the `TreeMap` stores all N entries.
 *
 * --- Approach 3: Sliding Window + Two Monotonic Deques (Optimal) ---
 * 0. Idea/Intuition: Maintain `maxDeque` (strictly decreasing values, front = max) and `minDeque`
 *    (strictly increasing values, front = min), both storing **indices**.
 *    For each `right` from `0` to `n - 1`:
 *    (a) Pop back of `maxDeque` while `nums[back] <= nums[right]`; push `right`.
 *    (b) Pop back of `minDeque` while `nums[back] >= nums[right]`; push `right`.
 *    (c) While `nums[maxDeque.peekFirst()] - nums[minDeque.peekFirst()] > limit`:
 *        advance `left++`, and if either deque's front index is now `< left`, pop it from the front.
 *    (d) Update `best = Math.max(best, right - left + 1)`.
 * 1. Why someone thinks of it: We only ever ask two questions of the window — "what is the max?"
 *    and "what is the min?" — and elements only leave in FIFO order from `left`. Two monotonic
 *    deques answer both in O(1) amortized time.
 * 2. Why it works:
 *    - Domination invariant: Any index popped from `maxDeque` back is `<= nums[right]` and older
 *      than `right`, so it can never be the max while `right` is in the window. Symmetrically,
 *      any index popped from `minDeque` back is `>= nums[right]` and older than `right`, so it
 *      can never be the min.
 *    - Expiration invariant: Whenever `left` advances past a deque's front index, that front is
 *      immediately evicted, so both fronts always lie inside `[left, right]`.
 *    Time Complexity: O(N) — because `right` moves from `0` to `N-1`, `left` moves at most `N`
 *    times, and each index is pushed once and popped at most once from each of the two deques
 *    (total <= 4N deque operations).
 *    Space Complexity: O(N) auxiliary — because in the worst case (e.g., `limit = 10^9` on a
 *    strictly decreasing array), `maxDeque` holds all N indices while `minDeque` holds 1.
 *
 * Interview Verdict:
 * Write Approach 3 (Two Monotonic Deques) in a 45-minute interview. It hits the optimal O(N)
 * time bound, is only ~20 lines of code, and directly showcases mastery of the monotonic deque
 * pattern. (If you ever blank on deque eviction under pressure, Approach 2 with `TreeMap` is a
 * rock-solid 15-line fallback that easily passes N = 10^5 in O(N log N) — mention it as the
 * stepping stone, then code Approach 3.)
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Single element (`[42]`, `limit = 0`): Returns 1.
 * - `limit = 0` (`[4, 2, 2, 2, 4]`, `limit = 0`): Only runs of identical elements are valid -> 3.
 * - All elements identical (`[7, 7, 7, 7]`, `limit = 0`): Entire array is valid -> 4.
 * - `limit` larger than global spread (`[1, 100, 50]`, `limit = 1000`): Never shrinks -> returns N.
 * - Duplicate max/min at the shrinking boundary (`[5, 5, 1]`, `limit = 2`): Using `<=` / `>=`
 *   when popping from the back keeps the NEWEST `5` (index 1); when `left` advances from 0 to 1,
 *   `maxDeque.peekFirst()` is 1 (not `< 1`), so `5` is NOT prematurely evicted!
 * - Strictly increasing or decreasing with tight `limit` (`[1, 2, 3, 4, 5]`, `limit = 1`):
 *   Tests constant shrinking on every step -> returns 2.
 * - Extreme values (`[1, 1_000_000_000]`, `limit = 999_999_999`): Exact boundary `max - min == limit`
 *   must be treated as VALID (shrink only on strict `> limit`).
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "What actually determines if a subarray is valid, and which older
 * elements in the window can never affect that condition again?"
 * Correctness intuition: Validity depends ONLY on `max - min <= limit`. When `nums[right]`
 * arrives, any older element `<= nums[right]` can never be the max again (since `nums[right]` is
 * at least as big and leaves later), and any older element `>= nums[right]` can never be the min
 * again (since `nums[right]` is at least as small and leaves later). Discarding them leaves two
 * monotonic deques whose fronts are always the exact window max and min.
 *
 * -- Examples & ASCII Diagram --
 * Example 1 (Typical): nums = [10, 1, 2, 4, 7, 2], limit = 5
 *
 *   Index:   0   1   2   3   4   5
 *   Value: [10,  1,  2,  4,  7,  2]
 *
 *   At right = 4 (val = 7), window tries [1, 2, 4, 7]:
 *     maxDeque (decreasing): [4:7]          -> front max = 7
 *     minDeque (increasing): [1:1, 2:2, 3:4, 4:7] -> front min = 1
 *     Diff = 7 - 1 = 6 > 5 (INVALID!)
 *     Advance left from 1 -> 2:
 *       minDeque front is index 1 (< left=2) -> POP FRONT!
 *       minDeque is now [2:2, 3:4, 4:7]     -> front min = 2
 *     Diff = 7 - 2 = 5 <= 5 (VALID! window [2, 4, 7], len = 3)
 *
 *   At right = 5 (val = 2), window [2, 4, 7, 2]:
 *     maxDeque: [4:7, 5:2]                  -> front max = 7
 *     minDeque: pop 4:7, 3:4, 2:2 (>= 2) -> [5:2] -> front min = 2
 *     Diff = 7 - 2 = 5 <= 5 (VALID! window [2, 4, 7, 2], len = 4) -> BEST!
 *
 * Example 2 (Where a tempting wrong approach fails):
 *   Tempting mistake: "Just check adjacent pairs `|nums[i] - nums[i-1]| <= limit`!"
 *   Input: nums = [2, 4, 7], limit = 4.
 *   - Adjacent diffs: `|4 - 2| = 2 <= 4` and `|7 - 4| = 3 <= 4`.
 *   - Naive adjacent check returns 3 (WRONG!).
 *   - Actual max diff is `|7 - 2| = 5 > 4`, so `[2, 4, 7]` is INVALID; true answer is 2.
 *   Why it fails: Small step-by-step drifts accumulate across non-adjacent elements.
 *
 * -- Dry run 1: Optimal Two-Deque on Typical Case nums = [10, 1, 2, 4, 7, 2], limit = 5 --
 *   R | val | maxDeque (idx:val) | minDeque (idx:val)    | Diff  | Shrink Action             | L | Best
 *   --+-----+--------------------+-----------------------+-------+---------------------------+---+-----
 *   0 |  10 | [0:10]             | [0:10]                | 0<=5  | none                      | 0 | 1
 *   1 |   1 | [0:10, 1:1]        | [1:1]                 | 9>5   | L->1, pop maxD front 0:10 | 1 | 1
 *     |     | [1:1]              | [1:1]                 | 0<=5  | done                      | 1 | 1
 *   2 |   2 | [2:2]              | [1:1, 2:2]            | 1<=5  | none                      | 1 | 2
 *   3 |   4 | [3:4]              | [1:1, 2:2, 3:4]       | 3<=5  | none                      | 1 | 3
 *   4 |   7 | [4:7]              | [1:1, 2:2, 3:4, 4:7]  | 6>5   | L->2, pop minD front 1:1  | 2 | 3
 *     |     | [4:7]              | [2:2, 3:4, 4:7]       | 5<=5  | done                      | 2 | 3
 *   5 |   2 | [4:7, 5:2]         | [5:2]                 | 5<=5  | none                      | 2 | 4
 *
 * -- Dry run 2: Edge Case — Duplicate Max at Boundary nums = [5, 5, 1], limit = 2 --
 *   R | val | maxDeque (idx:val) | minDeque (idx:val) | Diff  | Shrink Action                  | L | Best
 *   --+-----+--------------------+--------------------+-------+--------------------------------+---+-----
 *   0 |   5 | [0:5]              | [0:5]              | 0<=2  | none                           | 0 | 1
 *   1 |   5 | [1:5] (popped 0:5) | [1:5] (popped 0:5) | 0<=2  | none                           | 0 | 2
 *   2 |   1 | [1:5, 2:1]         | [2:1]              | 4>2   | L->1 (front 1 not < 1! keep!)  | 1 | 2
 *     |     | [1:5, 2:1]         | [2:1]              | 4>2   | L->2, pop maxD front 1:5 (1<2) | 2 | 2
 *     |     | [2:1]              | [2:1]              | 0<=2  | done (window [1], len=1)       | 2 | 2
 *   Final answer: 2 (subarray [5, 5]). Notice how storing indices + `< left` avoided prematurely
 *   evicting index 1 when `left` moved from 0 to 1!
 *
 * -- Pitfalls --
 * 1. Checking only adjacent differences (`|nums[i] - nums[i-1]| <= limit`): Fails whenever small
 *    monotone steps accumulate beyond `limit` (e.g., `[2, 4, 7]` with `limit = 4`).
 * 2. Storing VALUES instead of INDICES in the deques + popping `<=` / `>=`: If you store raw
 *    values and pop duplicates from the back, then on `[5, 5, 1]` you lose track of *how many*
 *    5s exist — when `left` passes the first `5`, `maxDeque.peekFirst() == nums[left]` would
 *    wrongly pop the only `5` in the deque! Storing **indices** completely eliminates this bug.
 * 3. Forgetting `Integer` vs `int` equality in Java (Java-specific gotcha): If you ever store
 *    `Integer` objects in a deque/map and compare `deque.peekFirst() == map.firstKey()` using `==`,
 *    Java compares object references outside the cached `[-128, 127]` range and silently returns
 *    `false`! Always unbox to primitive `int` or use `.equals()`.
 * 4. Shrinking on `>= limit` instead of `> limit`: The problem says "less than OR EQUAL TO limit",
 *    so `max - min == limit` is valid and must NOT trigger shrinking.
 *
 * -- Pattern recognition --
 * When you see: "Longest/shortest contiguous subarray where `max - min <= limit`" (or any
 * variable-size sliding window that needs real-time window max/min)
 * -> Think **Sliding Window + Two Monotonic Deques** (one decreasing for max, one increasing for min).
 * Transfers directly to:
 *   - Sliding Window Maximum (LeetCode 239) — fixed-size window with one monotonic deque
 *   - Continuous Subarrays (LeetCode 2762) — COUNT all subarrays with `max - min <= 2`
 *     (exact same two deques, just add `right - left + 1` to `totalCount` each step!)
 *   - Shortest Subarray with Sum at Least K (LeetCode 862) — monotonic deque over prefix sums
 *
 * -- Interview script --
 * "First, checking every pair in a subarray is equivalent to checking `max(subarray) - min(subarray) <= limit`,
 * because the extreme values always produce the largest absolute difference. Second, this condition
 * is monotonic: if a window `[left, right]` violates the limit, expanding `right` can't fix it,
 * so we must advance `left` — giving us a sliding window. A `TreeMap` can track window min and max
 * in O(N log N), but we can eliminate the log factor with two monotonic deques storing indices:
 * `maxDeque` in decreasing value order and `minDeque` in increasing value order. When `nums[right]`
 * arrives, we pop dominated indices from the backs, then while `nums[maxFront] - nums[minFront] > limit`,
 * we advance `left` and evict any front index that falls before `left`. Each index enters and leaves
 * each deque at most once, so that gives us O(N) time and O(N) space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: Instead of the LONGEST valid subarray, what if we want the TOTAL NUMBER of valid subarrays
 *     where `max - min <= limit` (this is LeetCode 2762: Continuous Subarrays)?
 * A1: Use the exact same two-deque sliding window! Once `[left, right]` is valid after the
 *     `while` loop, every suffix ending at `right` — namely `[left..right]`, `[left+1..right]`,
 *     ..., `[right..right]` — is also valid. Add `(right - left + 1)` to a `long count` at each
 *     step. Still O(N) time and O(N) space.
 *
 * Q2: What if we want to return the ACTUAL longest subarray (not just its length)?
 * A2: Track `bestLeft` alongside `bestLen`. Whenever `right - left + 1 > bestLen`, update
 *     `bestLen = right - left + 1` and `bestLeft = left`. At the end, return
 *     `Arrays.copyOfRange(nums, bestLeft, bestLeft + bestLen)`. Still O(N) time.
 *
 * Q3: What if `nums` arrives as an infinite online stream and we only have O(L) memory where L
 *     is the current valid window size (not O(N))?
 * A3: Instead of indexing back into an array `nums[]` (which would grow to O(N)), store
 *     `(value, streamIndex)` pairs directly inside `maxDeque` and `minDeque`. Since indices
 *     before `left` are evicted from the fronts, both deques only ever hold elements from the
 *     active valid window `[left, right]`, keeping memory strictly O(L).
 *
 * Q4: What if the problem asked for the longest SUBSEQUENCE (not contiguous) with max diff <= limit?
 * A4: Order no longer matters! Sort `nums` in O(N log N) and run a simple two-pointer sliding
 *     window checking `nums[right] - nums[left] <= limit`. No deques needed because sorting
 *     already puts min at `left` and max at `right`.
 * =========================================================================================
 */

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TreeMap;

public class LongestSubarrayWithLimitStudyNote {

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force (O(N^2) time, O(1) space)
    // ----------------------------------------------------------------------
    public static int longestSubarrayBruteForce(int[] nums, int limit) {
        int n = nums.length;
        int best = 1; // Constraints guarantee n >= 1 and limit >= 0, so length 1 is always valid.

        for (int i = 0; i < n; i++) {
            int curMin = nums[i];
            int curMax = nums[i];

            for (int j = i + 1; j < n; j++) {
                curMin = Math.min(curMin, nums[j]);
                curMax = Math.max(curMax, nums[j]);

                // Once the spread exceeds limit, extending `j` further can never shrink it.
                if (curMax - curMin > limit) {
                    break;
                }
                best = Math.max(best, j - i + 1);
            }
        }
        return best;
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Sliding Window + TreeMap (O(N log N) time, O(N) space)
    // ----------------------------------------------------------------------
    public static int longestSubarrayTreeMap(int[] nums, int limit) {
        // Maps each value currently in [left, right] -> its frequency in the window.
        // Example for window [2, 4, 7, 2]: windowFreq = {2=2, 4=1, 7=1}
        //   firstKey() = 2 (min), lastKey() = 7 (max), spread = 5.
        TreeMap<Integer, Integer> windowFreq = new TreeMap<>();

        int left = 0;
        int best = 0;

        for (int right = 0; right < nums.length; right++) {
            // 1. Expand window to include nums[right].
            windowFreq.merge(nums[right], 1, Integer::sum);

            // 2. Shrink from the left until max - min <= limit.
            while (windowFreq.lastKey() - windowFreq.firstKey() > limit) {
                int outgoing = nums[left];
                int count = windowFreq.get(outgoing);
                if (count == 1) {
                    windowFreq.remove(outgoing); // Remove key so it can no longer act as min/max.
                } else {
                    windowFreq.put(outgoing, count - 1);
                }
                left++;
            }

            // 3. Window [left, right] is now guaranteed valid.
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Sliding Window + Two Monotonic Deques (OPTIMAL: O(N) time, O(N) space)
    // ----------------------------------------------------------------------
    public static int longestSubarrayMonotonicDeque(int[] nums, int limit) {
        // Both deques store INDICES (never raw values) in strictly increasing index order.
        // Example for nums = [10, 1, 2, 4, 7, 2] at right = 4 (window [2, 4, 7]):
        //   maxDeque = [4]          (values: [7]        — decreasing, front is window max)
        //   minDeque = [2, 3, 4]    (values: [2, 4, 7]  — increasing, front is window min)
        Deque<Integer> maxDeque = new ArrayDeque<>();
        Deque<Integer> minDeque = new ArrayDeque<>();

        int left = 0;
        int best = 0;

        for (int right = 0; right < nums.length; right++) {
            int val = nums[right];

            // 1. Maintain decreasing invariant for maxDeque:
            //    Any older index with value <= val is permanently dominated as a max candidate.
            while (!maxDeque.isEmpty() && nums[maxDeque.peekLast()] <= val) {
                maxDeque.pollLast();
            }
            maxDeque.offerLast(right);

            // 2. Maintain increasing invariant for minDeque:
            //    Any older index with value >= val is permanently dominated as a min candidate.
            while (!minDeque.isEmpty() && nums[minDeque.peekLast()] >= val) {
                minDeque.pollLast();
            }
            minDeque.offerLast(right);

            // 3. Shrink window from the left while the spread exceeds `limit`.
            //    Note: Both deques are guaranteed non-empty here because `right` was just pushed.
            while (nums[maxDeque.peekFirst()] - nums[minDeque.peekFirst()] > limit) {
                left++;

                // Evict front indices that have fallen outside [left, right].
                // Using `< left` (with stored indices) safely handles duplicate boundary values.
                if (maxDeque.peekFirst() < left) {
                    maxDeque.pollFirst();
                }
                if (minDeque.peekFirst() < left) {
                    minDeque.pollFirst();
                }
            }

            // 4. Window [left, right] is now valid; record its length.
            best = Math.max(best, right - left + 1);
        }
        return best;
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 3 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private static void verifyCase(String name, int[] nums, int limit, int expected) {
        int rBrute = longestSubarrayBruteForce(nums, limit);
        int rTree = longestSubarrayTreeMap(nums, limit);
        int rDeque = longestSubarrayMonotonicDeque(nums, limit);

        boolean pass = (rBrute == expected) && (rTree == expected) && (rDeque == expected);

        System.out.printf("%-34s | %s | Output: %d%n",
                name, pass ? "PASS" : "FAIL", rDeque);

        if (!pass) {
            System.out.printf("  Expected=%d, Brute=%d, TreeMap=%d, Deque=%d%n",
                    expected, rBrute, rTree, rDeque);
        }
    }

    public static void main(String[] args) {
        verifyCase("LC Example 1",
                new int[]{8, 2, 4, 7}, 4, 2);

        verifyCase("LC Example 2",
                new int[]{10, 1, 2, 4, 7, 2}, 5, 4);

        verifyCase("LC Example 3 (limit = 0)",
                new int[]{4, 2, 2, 2, 4, 4, 2, 2}, 0, 3);

        verifyCase("Edge: Single element, limit=0",
                new int[]{42}, 0, 1);

        verifyCase("Edge: All identical, limit=0",
                new int[]{7, 7, 7, 7, 7}, 0, 5);

        verifyCase("Edge: Huge limit (whole array)",
                new int[]{1, 1_000_000_000, 500_000_000}, 1_000_000_000, 3);

        verifyCase("Edge: Exact boundary diff == lim",
                new int[]{1, 1_000_000_000}, 999_999_999, 2);

        verifyCase("Edge: Duplicate max at boundary",
                new int[]{5, 5, 1}, 2, 2);

        verifyCase("Edge: Strictly increasing, lim=1",
                new int[]{1, 2, 3, 4, 5}, 1, 2);

        verifyCase("Edge: Adjacent-check trap",
                new int[]{2, 4, 7}, 4, 2);
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Variable-Size Sliding Window + Two Monotonic Deques (one for max, one for min).
 * - Key observation: "Every pair `|a - b| <= limit`" reduces to `windowMax - windowMin <= limit`,
 *   which is monotonic as the window expands/shrinks.
 * - Worth memorizing vs. understanding: Understand *why* older dominated elements can be popped
 *   from the back of each deque (that gives O(N) instead of O(N log N)); memorize storing INDICES
 *   in the deques and evicting fronts with `peekFirst() < left`.
 * - Most common trap: Checking only adjacent differences, or storing raw values in the deques
 *   (which breaks eviction when duplicate max/min values sit at the window boundary).
 * - One-line mental trigger: "Longest subarray with max - min <= limit? Sliding window + maxDeque + minDeque of indices."
 * =========================================================================================
 */











































import java.util.TreeMap;

class TreeMapSolution {

    public int longestSubarray(int[] nums, int limit) {

        /*
         * TreeMap stores:
         *
         * key   = number in the current window
         * value = frequency of that number
         *
         * WHY TreeMap?
         *
         * It maintains elements in sorted order.
         *
         * firstKey() -> Minimum element.
         * lastKey()  -> Maximum element.
         *
         * Both operations take O(log n) or better.
         */
        TreeMap<Integer, Integer> map = new TreeMap<>();

        int left = 0;
        int answer = 0;

        /*
         * Expand the sliding window using right.
         *
         * Each element enters the window exactly once.
         */
        for (int right = 0; right < nums.length; right++) {

            /*
             * Add the current element to the frequency map.
             *
             * merge() increments the frequency.
             */
            map.merge(nums[right], 1, Integer::sum);

            /*
             * Shrink the window until it becomes valid.
             *
             * Current window:
             * nums[left ... right]
             */
            while ((long) map.lastKey() - map.firstKey() > limit) {

                int outgoing = nums[left];

                /*
                 * Remove exactly one occurrence.
                 *
                 * If its frequency becomes zero,
                 * remove the key completely.
                 */
                map.compute(outgoing, (key, frequency) ->
                    frequency == 1 ? null : frequency - 1
                );

                left++;
            }

            /*
             * The current window is now valid.
             *
             * Window length = right - left + 1
             */
            answer = Math.max(
                answer,
                right - left + 1
            );
        }

        return answer;
    }
}









































import java.util.ArrayDeque;
import java.util.Deque;

class Solution {

    public int longestSubarray(int[] nums, int limit) {

        /*
         * maxDeque:
         *
         * Stores indices in decreasing order of their values.
         * The front always represents the maximum element
         * in the current sliding window.
         *
         * minDeque:
         *
         * Stores indices in increasing order of their values.
         * The front always represents the minimum element
         * in the current sliding window.
         *
         * WHY STORE INDICES INSTEAD OF VALUES?
         *
         * We need to know whether an element has moved
         * outside the current sliding window.
         *
         * Its index allows us to check:
         *
         * index < left
         *
         * Deque operations at both ends take O(1).
         */
        Deque<Integer> maxDeque = new ArrayDeque<>();
        Deque<Integer> minDeque = new ArrayDeque<>();

        int left = 0;
        int answer = 0;

        /*
         * Expand the window one element at a time.
         *
         * right represents the newly included element.
         *
         * Every index enters each deque at most once.
         */
        for (int right = 0; right < nums.length; right++) {

            /*
             * -----------------------------------------
             * STEP 1: MAINTAIN MAXIMUM DEQUE
             * -----------------------------------------
             *
             * We want decreasing order of values.
             *
             * Remove all elements from the back whose
             * values are <= nums[right].
             *
             * WHY?
             *
             * The current element is greater than or equal
             * to those elements and is positioned later.
             *
             * Therefore, those older elements can never
             * become the maximum while the current element
             * remains inside the window.
             *
             * After removal, add the current index.
             */
            while (!maxDeque.isEmpty()
                    && nums[maxDeque.peekLast()] <= nums[right]) {

                maxDeque.pollLast();
            }

            maxDeque.offerLast(right);

            /*
             * -----------------------------------------
             * STEP 2: MAINTAIN MINIMUM DEQUE
             * -----------------------------------------
             *
             * We want increasing order of values.
             *
             * Remove all elements from the back whose
             * values are >= nums[right].
             *
             * WHY?
             *
             * The current element is smaller than or equal
             * to those elements and is positioned later.
             *
             * Therefore, those older elements can never
             * become the minimum while the current element
             * remains inside the window.
             */
            while (!minDeque.isEmpty()
                    && nums[minDeque.peekLast()] >= nums[right]) {

                minDeque.pollLast();
            }

            minDeque.offerLast(right);

            /*
             * -----------------------------------------
             * STEP 3: CHECK WINDOW VALIDITY
             * -----------------------------------------
             *
             * The front of maxDeque gives the maximum.
             * The front of minDeque gives the minimum.
             *
             * Valid window condition:
             *
             * maximum - minimum <= limit
             *
             * If the condition is violated, shrink
             * the window from the left.
             *
             * Use long for safe subtraction.
             */
            while ((long) nums[maxDeque.peekFirst()]
                    - nums[minDeque.peekFirst()] > limit) {

                /*
                 * -----------------------------------------
                 * STEP 4: REMOVE EXPIRED INDICES
                 * -----------------------------------------
                 *
                 * The current window is:
                 *
                 * nums[left ... right]
                 *
                 * We are about to move left forward.
                 *
                 * If left is at the front of maxDeque,
                 * it is no longer part of the window.
                 *
                 * Remove it.
                 */
                if (maxDeque.peekFirst() == left) {
                    maxDeque.pollFirst();
                }

                /*
                 * Apply the same logic to minimum deque.
                 */
                if (minDeque.peekFirst() == left) {
                    minDeque.pollFirst();
                }

                /*
                 * Shrink the window.
                 */
                left++;
            }

            /*
             * -----------------------------------------
             * STEP 5: UPDATE ANSWER
             * -----------------------------------------
             *
             * At this point, the current window is valid.
             *
             * Its length is:
             *
             * right - left + 1
             *
             * Keep the maximum length encountered.
             */
            answer = Math.max(
                answer,
                right - left + 1
            );
        }

        return answer;
    }
}


