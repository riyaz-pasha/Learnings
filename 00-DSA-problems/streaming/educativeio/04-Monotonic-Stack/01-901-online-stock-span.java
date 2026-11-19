/**
 * 0. ## Problem Statement
 * Design an algorithm that collects daily stock price quotes and returns the "span" of
 * the stock's price for the current day.
 *
 * The span of today's price is the maximum number of consecutive days (starting from today
 * and going backward) for which the stock price was LESS THAN OR EQUAL TO today's price.
 *
 * Implement the `StockSpanner` class:
 * - `StockSpanner()`: Initializes the object.
 * - `int next(int price)`: Returns the span of today's price given today's quote is `price`.
 *
 * Example:
 *   Prices: [100, 80, 60, 70, 60, 75, 85]
 *   Spans:  [  1,  1,  1,  2,  1,  4,  6]
 *
 * Constraints:
 * - 1 <= price <= 10^5
 * - At most 10^4 calls will be made to `next`.
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Is the comparison 'less than or equal to' (<=) or strictly 'less than' (<)?"
 *    -> Prevents an off-by-one bug on equal consecutive prices like [50, 50]; `<=` yields [1, 2], `<` yields [1, 1].
 * 2. "Does today always count as part of its own span?"
 *    -> Confirms the minimum possible return value is 1 (not 0), anchoring our base case.
 * 3. "Is the stream strictly online (must answer each day before seeing tomorrow), or do we get all prices upfront?"
 *    -> Rules out offline sorting/preprocessing; every call to `next()` must maintain incremental state.
 * 4. "Are we optimizing for total/amortized throughput across all calls, or strict worst-case latency per single call?"
 *    -> A monotonic stack is amortized O(1) but worst-case O(N) on a single spike day; strict O(log N) worst-case requires a different structure.
 * 5. "Can prices be negative, zero, or floating-point, or are they strictly positive 32-bit integers?"
 *    -> Confirms primitive `int` is safe and prevents floating-point precision issues (e.g., `<=` with epsilon).
 * 6. "Will we ever need to query the span of a past day or update a historical price?"
 *    -> Confirms we can safely discard dominated past prices instead of retaining full history.
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to 10^4 calls to `next()` (and in real trading feeds, millions).
 * Scanning backward through all previous days on every call degrades to O(N^2) total work
 * on rising markets (e.g., [1, 2, 3, ..., N]).
 *
 * --- Approach 1: Brute Force Backward Scan ---
 * 0. Idea/Intuition: Store every incoming price in an `ArrayList`. On `next(price)`, walk
 *    backward from the end counting consecutive elements `<= price` until hitting one `> price`.
 * 1. What I'd naturally try: Literally translate the problem definition into a `for` loop.
 * 2. Why it works: Every historical price is preserved, so walking backward stops at the exact
 *    nearest strictly greater day.
 * 3. Why it's too slow/costly: On a monotonically increasing stream [1, 2, 3, ..., N], day `i`
 *    walks back `i` steps. Total work is 1 + 2 + ... + N = O(N^2), which falls over at scale.
 * 4. What work is being repeated: Suppose yesterday's price was 75 and we ALREADY proved it beat
 *    the 3 days before it ([60, 70, 60]). When today arrives with price 85, walking back one-by-one
 *    re-checks 60, 70, and 60 even though 85 >= 75 guarantees 85 beats all of them too!
 * 5. Observation that removes the bottleneck: "Less than or equal to" is transitive. If today's
 *    price >= a past day's price `P`, today automatically swallows `P`'s entire span in one leap.
 * 6. Leads to Approach 2: Store each day's computed `span` alongside its `price` so we can jump
 *    directly over swallowed blocks instead of stepping by 1.
 *    Time Complexity: O(N) worst-case per `next()` call, O(N^2) total across N calls — because
 *    an increasing sequence forces each call to inspect every earlier element.
 *    Space Complexity: O(N) — because we store all N historical prices in an `ArrayList`.
 *
 * --- Approach 2: Jump Pointers (Compressed Backward Leap / DP-style) ---
 * 0. Idea/Intuition: Keep two `ArrayList`s: `prices` and `spans`. Start at `i = prevDay`.
 *    While `i >= 0 && prices.get(i) <= price`, add `spans.get(i)` to today's span and jump
 *    `i -= spans.get(i)` straight to the first day that stopped day `i`.
 * 1. What I'd naturally try: "Don't re-walk regions I already summarized — leap over them!"
 * 2. Why it works: By definition of `spans[i]`, every day in `[i - spans[i] + 1 .. i]` has
 *    price `<= prices[i]`. Since `prices[i] <= todayPrice`, all of them are `<= todayPrice`.
 *    The first candidate that could possibly stop today is at `i - spans[i]`.
 * 3. Why it's still suboptimal (space): Even though we skip over swallowed days, we STILL
 *    keep every single swallowed price and span in our `ArrayList`s forever.
 * 4. What work/storage is wasted: Think carefully — once day `i` is jumped over by today,
 *    can ANY future day EVER land on day `i`?
 *    - If tomorrow's price < today's price, tomorrow stops immediately at today (never reaches `i`).
 *    - If tomorrow's price >= today's price, tomorrow jumps over today AND its entire span (skipping `i`).
 *    Day `i` is permanently dead! Keeping it in memory is pure waste.
 * 5. Observation that removes the bottleneck: We only ever need to remember the "un-swallowed"
 *    days — which naturally form a strictly decreasing sequence of prices from oldest to newest.
 *    Whenever today swallows the most recent surviving day, we can just POP and discard it.
 * 6. Leads to Approach 3: Replace the `ArrayList`s + jump index with a LIFO stack storing
 *    only surviving `(price, span)` pairs.
 *    Time Complexity: Amortized O(1) per `next()` call (O(N) total across N calls) — because
 *    each day can be jumped over at most once before becoming permanently unreachable.
 *    Space Complexity: O(N) ALWAYS — because every price and span is appended to `ArrayList`s
 *    and never evicted, even on strictly increasing inputs.
 *
 * --- Approach 3: Monotonic Decreasing Stack (Optimal) ---
 * 0. Idea/Intuition: Maintain a stack of `(price, span)` pairs where prices are strictly
 *    decreasing from bottom to top. On `next(price)`, initialize `span = 1`. While the top
 *    of the stack has `top.price <= price`, pop it and absorb its span (`span += top.span`).
 *    Push `(price, span)` onto the stack and return `span`.
 * 1. Why someone thinks of it: It's literally Approach 2 with dead elements garbage-collected
 *    immediately from the top of the stack.
 * 2. Why it works: Every popped element has `price <= today`, and by induction every element
 *    it previously absorbed also had `price <= today`. The moment the loop stops, the new
 *    stack top is the exact nearest previous day with `price > today` (or the stack is empty).
 * 3. Practical variation note: Instead of storing `(price, span)`, you can store `(price, dayIndex)`
 *    and compute `span = currentDay - stack.peek().dayIndex`. Same O(1) amortized time and O(N)
 *    space, but storing `(price, span)` is self-contained (no global `dayIndex` counter needed
 *    and no empty-stack sentinel `-1` math).
 *    Time Complexity: Amortized O(1) per `next()` call, O(N) total across N calls — because
 *    every price is pushed onto the stack exactly once and popped at most once. (Worst-case
 *    for a single call is O(N) when a huge spike pops all prior elements.)
 *    Space Complexity: O(N) worst-case (strictly decreasing prices where nothing ever pops),
 *    and O(1) best-case (strictly increasing prices where stack size never exceeds 1) —
 *    storing only the currently undominated `(price, span)` records in an `ArrayDeque`.
 *
 * Interview Verdict:
 * Write Approach 3 (Monotonic Decreasing Stack with `Deque<DaySpan>`). It's 10 lines of code,
 * has zero index-math edge cases (unlike `dayIndex` sentinels or jump-pointer bounds), discards
 * dead data immediately, and is the canonical pattern interviewers expect for span/PGE problems.
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - First day ever (empty stack): Must return 1 without throwing `NullPointerException` on `peek()`.
 * - All equal prices (`[10, 10, 10, 10]`): Must return `[1, 2, 3, 4]` — tests `<=` vs `<`.
 * - Strictly increasing prices (`[1, 2, 3, 4, 5]`): Must return `[1, 2, 3, 4, 5]` — stack size stays 1.
 * - Strictly decreasing prices (`[5, 4, 3, 2, 1]`): Must return `[1, 1, 1, 1, 1]` — worst-case O(N) space.
 * - Massive spike after a long decline (`[100, 90, 80, 70, 200]`): Single call pops everything
 *   and returns 5 — tests draining the stack completely.
 * - Valley with equal boundaries (`[50, 10, 20, 50]`): Final 50 must swallow 20, 10, AND the first 50 -> span 4.
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "If today's price beats yesterday's price, do I ever need to know
 * yesterday's individual price again?"
 * Correctness intuition: No! Because `<=` is transitive, any future day that beats today will
 * automatically beat yesterday (and everything yesterday beat), and any future day that loses
 * to today will stop at today before ever reaching yesterday. Therefore, collapsing yesterday's
 * count into today's `span` loses zero information.
 *
 * -- Examples & ASCII Diagram --
 * Example 1 (Typical): [100, 80, 60, 70, 60, 75, 85]
 *
 * Visual of Monotonic Decreasing Stack as days arrive (bottom -> top):
 *
 *   Day 1 (100): [(100,1)]                             -> returns 1
 *   Day 2 ( 80): [(100,1), (80,1)]                     -> returns 1
 *   Day 3 ( 60): [(100,1), (80,1), (60,1)]             -> returns 1
 *   Day 4 ( 70): pop (60,1)! span = 1+1 = 2
 *                [(100,1), (80,1), (70,2)]             -> returns 2
 *   Day 5 ( 60): [(100,1), (80,1), (70,2), (60,1)]     -> returns 1
 *   Day 6 ( 75): pop (60,1) -> span=2
 *                pop (70,2) -> span=4  (leaps over Day 3's 60 automatically!)
 *                [(100,1), (80,1), (75,4)]             -> returns 4
 *   Day 7 ( 85): pop (75,4) -> span=5
 *                pop (80,1) -> span=6
 *                [(100,1), (85,6)]                     -> returns 6
 *
 * Notice how prices in the stack ALWAYS stay strictly decreasing: 100 > 85!
 *
 * Example 2 (Where a tempting wrong approach fails):
 *   Tempting mistake: "Just compare today with yesterday; if today >= yesterday, `span = prevSpan + 1`, else `1`."
 *   Input: [100, 80, 60, 70, 75]
 *   - Day 4 (70): 70 >= 60 -> span = 1 + 1 = 2. (Happens to be right!)
 *   - Day 5 (75): 75 >= 70 -> naive says `prevSpan + 1 = 3` (WRONG! Should be 3? Wait: [60, 70, 75] is 3.
 *     Now add Day 6 (85): 85 >= 75 -> naive says `3 + 1 = 4`, completely missing Day 2's 80! True span is 5.)
 *   Why it fails: Adding 1 only checks yesterday, never jumping PAST yesterday's span to check
 *   what stopped yesterday (Day 2's 80).
 *
 * -- Dry run 1: Optimal Stack on Typical Case [100, 80, 60, 70, 60, 75, 85] --
 *   Step | Input | Popped (price, span)    | Computed Span | Stack After (bottom -> top)
 *   -----+-------+-------------------------+---------------+----------------------------------
 *    1   |  100  | (none)                  | 1             | [(100,1)]
 *    2   |   80  | (none; 100 > 80)        | 1             | [(100,1), (80,1)]
 *    3   |   60  | (none; 80 > 60)         | 1             | [(100,1), (80,1), (60,1)]
 *    4   |   70  | (60,1)                  | 1+1 = 2       | [(100,1), (80,1), (70,2)]
 *    5   |   60  | (none; 70 > 60)         | 1             | [(100,1), (80,1), (70,2), (60,1)]
 *    6   |   75  | (60,1), (70,2)          | 1+1+2 = 4     | [(100,1), (80,1), (75,4)]
 *    7   |   85  | (75,4), (80,1)          | 1+4+1 = 6     | [(100,1), (85,6)]
 *
 * -- Dry run 2: Edge Case — Valley with Equal Boundaries [50, 10, 20, 50] --
 *   Step | Input | Popped (price, span)    | Computed Span | Stack After (bottom -> top)
 *   -----+-------+-------------------------+---------------+----------------------------------
 *    1   |   50  | (none)                  | 1             | [(50,1)]
 *    2   |   10  | (none; 50 > 10)         | 1             | [(50,1), (10,1)]
 *    3   |   20  | (10,1)                  | 1+1 = 2       | [(50,1), (20,2)]
 *    4   |   50  | (20,2), (50,1) [<=!]    | 1+2+1 = 4     | [(50,4)]
 *
 * -- Pitfalls --
 * 1. Strict `<` instead of `<=`: Tempting when thinking "find a taller bar", but equal prices
 *    count toward the span. Using `<` leaves duplicate prices on the stack and undercounts.
 * 2. Storing only `price` on the stack without `span` (or `dayIndex`): Once you pop elements,
 *    you lose count of how many days were already swallowed unless you store the accumulated span.
 * 3. Java-specific gotcha: Using `java.util.Stack` instead of `Deque`/`ArrayDeque`. `Stack`
 *    extends `Vector` and synchronizes every `push`/`pop`/`peek` call, adding lock overhead.
 *    Always prefer `Deque<T> stack = new ArrayDeque<>()`.
 *
 * -- Pattern recognition --
 * When you see: "For each element in a stream/array, find the nearest previous/next GREATER
 * (or SMALLER) element, or count consecutive elements dominated by the current one"
 * -> Think **Monotonic Stack**.
 * Transfers directly to:
 *   - Daily Temperatures (LeetCode 739) — Next Greater Element distance
 *   - Largest Rectangle in Histogram (LeetCode 84) — Previous & Next Smaller Element spans
 *   - Sum of Subarray Minimums (LeetCode 907) — Counting dominated spans in both directions
 *
 * -- Interview script --
 * "A brute-force approach stores all prices and walks backward on every `next()` call, which
 * is O(N) per call and O(N^2) overall on rising prices. The key observation is transitivity:
 * if today's price is greater than or equal to a previous day's price, today also beats every
 * day that previous day already beat — and that previous day can never stop any future day
 * either. So I'll maintain a monotonic decreasing stack of `(price, span)` pairs. On each
 * `next(price)`, I start with `span = 1`, pop any stack top with `price <= today`, add its
 * span to mine, and push `(price, span)`. Because each element is pushed once and popped at
 * most once, that gives us amortized O(1) time per call and O(N) worst-case space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: What if we only care about the span within the last `W` days (bounded sliding window)?
 * A1: A pure `(price, span)` stack loses exact timestamps, so you can't tell when an old peak
 *     falls out of the `W`-day window. Switch to storing `(price, dayIndex)` in a **Monotonic
 *     Deque**: evict from the front when `front.dayIndex <= currentDay - W`, pop from the back
 *     while `back.price <= price`, and compute `span = Math.min(W, currentDay - prevGreaterIndex)`.
 *
 * Q2: What if a real-time trading system cannot tolerate an O(N) worst-case latency spike on a
 *     single `next()` call (needs strict O(log N) worst-case per call, not just amortized O(1))?
 * A2: Keep the monotonic decreasing sequence in an `ArrayList` instead of popping one-by-one —
 *     actually, you can binary search the monotonic array to find the first element `> price`
 *     in O(log N) worst-case time, then truncate the tail in O(1) amortized (or overwrite a
 *     logical `size` pointer in O(1) strict worst-case time!).
 *     Wait: moving a `size` pointer on a pre-allocated array (`top = binarySearchResult + 1`)
 *     gives **strict O(log N) worst-case time per call** with zero O(N) pop loops!
 *
 * Q3: What if we also need `getSpan(int day)` to query any historical day's span later?
 * A3: Simply append each returned span to an `ArrayList<Integer> history` alongside the stack.
 *     `getSpan(day)` becomes O(1) lookup, while keeping the stack for O(1) amortized `next()`.
 * =========================================================================================
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class OnlineStockSpanStudyNote {

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force (O(N) per call, O(N) space)
    // ----------------------------------------------------------------------
    static class StockSpannerBruteForce {
        // Stores every historical price in chronological order.
        // Example after [100, 80, 60, 70]: prices = [100, 80, 60, 70]
        private final List<Integer> prices = new ArrayList<>();

        public int next(int price) {
            prices.add(price);

            // Walk backward from today (inclusive), counting consecutive prices <= today.
            int span = 0;
            for (int i = prices.size() - 1; i >= 0; i--) {
                if (prices.get(i) <= price) {
                    span++;
                } else {
                    // The span must be consecutive; the first strictly greater price stops us.
                    break;
                }
            }
            return span;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Jump Pointers / Compressed Backward Leap
    // (Amortized O(1) time per call, ALWAYS O(N) space)
    // ----------------------------------------------------------------------
    static class StockSpannerJumpPointers {
        // Parallel arrays storing every day's price and its final computed span.
        // Example after [100, 80, 60, 70]:
        //   prices = [100, 80, 60, 70]
        //   spans  = [  1,  1,  1,  2]
        private final List<Integer> prices = new ArrayList<>();
        private final List<Integer> spans = new ArrayList<>();

        public int next(int price) {
            int span = 1; // Today always counts toward its own span.
            int prevIdx = prices.size() - 1;

            // Instead of stepping back by 1, jump directly over any day we beat
            // by skipping its entire already-verified span.
            while (prevIdx >= 0 && prices.get(prevIdx) <= price) {
                int jumpedSpan = spans.get(prevIdx);
                span += jumpedSpan;
                prevIdx -= jumpedSpan; // Land directly on the day that stopped `prevIdx`.
            }

            prices.add(price);
            spans.add(span);
            return span;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Monotonic Decreasing Stack (OPTIMAL)
    // (Amortized O(1) time per call, O(N) worst-case / O(1) best-case space)
    // ----------------------------------------------------------------------
    static class StockSpannerMonotonicStack {
        // Immutable pair representing a surviving peak day and how many consecutive days
        // (including itself) it already dominates.
        private record DaySpan(int price, int span) {}

        // Strictly decreasing stack of surviving peaks (top = most recent, lowest surviving price).
        // Example after [100, 80, 60, 70, 60, 75]:
        //   bottom -> (100, 1), (80, 1), (75, 4) <- top
        // Notice 60, 70, 60 are gone — their counts are already absorbed into (75, 4).
        private final Deque<DaySpan> stack = new ArrayDeque<>();

        public int next(int price) {
            int span = 1; // Every day's span includes at least itself.

            // Absorb every consecutive prior peak whose price is <= today's price.
            // Using `<=` (not `<`) is essential because equal prices extend the span.
            while (!stack.isEmpty() && stack.peek().price() <= price) {
                span += stack.pop().span();
            }

            // Push today as the new top. Because we popped everything <= price,
            // the stack invariant (strictly decreasing prices) is preserved.
            stack.push(new DaySpan(price, span));
            return span;
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 3 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private static void verifyCase(String name, int[] input, int[] expected) {
        StockSpannerBruteForce brute = new StockSpannerBruteForce();
        StockSpannerJumpPointers jump = new StockSpannerJumpPointers();
        StockSpannerMonotonicStack stack = new StockSpannerMonotonicStack();

        int[] resBrute = new int[input.length];
        int[] resJump = new int[input.length];
        int[] resStack = new int[input.length];

        for (int i = 0; i < input.length; i++) {
            resBrute[i] = brute.next(input[i]);
            resJump[i] = jump.next(input[i]);
            resStack[i] = stack.next(input[i]);
        }

        boolean pass = Arrays.equals(resBrute, expected)
                && Arrays.equals(resJump, expected)
                && Arrays.equals(resStack, expected);

        System.out.printf("%-30s | %s | Output: %s%n",
                name, pass ? "PASS" : "FAIL", Arrays.toString(resStack));

        if (!pass) {
            System.out.println("  Expected: " + Arrays.toString(expected));
            System.out.println("  Brute:    " + Arrays.toString(resBrute));
            System.out.println("  Jump:     " + Arrays.toString(resJump));
            System.out.println("  Stack:    " + Arrays.toString(resStack));
        }
    }

    public static void main(String[] args) {
        verifyCase("LC Example 1",
                new int[]{100, 80, 60, 70, 60, 75, 85},
                new int[]{1, 1, 1, 2, 1, 4, 6});

        verifyCase("Edge: Single element",
                new int[]{42},
                new int[]{1});

        verifyCase("Edge: All equal (<= test)",
                new int[]{10, 10, 10, 10},
                new int[]{1, 2, 3, 4});

        verifyCase("Edge: Strictly increasing",
                new int[]{1, 2, 3, 4, 5},
                new int[]{1, 2, 3, 4, 5});

        verifyCase("Edge: Strictly decreasing",
                new int[]{5, 4, 3, 2, 1},
                new int[]{1, 1, 1, 1, 1});

        verifyCase("Edge: Spike drains stack",
                new int[]{100, 90, 80, 70, 200},
                new int[]{1, 1, 1, 1, 5});

        verifyCase("Edge: Valley equal bounds",
                new int[]{50, 10, 20, 50},
                new int[]{1, 1, 2, 4});
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Monotonic Decreasing Stack (`Deque` storing `(value, accumulatedSpan)`).
 * - Key observation: Transitivity of `<=` means once today beats a past day, that past day
 *   is permanently unreachable — absorb its span and discard it.
 * - Worth memorizing vs. understanding: Understand *why* popped elements never matter again
 *   (that's the entire amortized O(1) proof); memorize using `ArrayDeque` instead of `Stack`.
 * - Most common trap: Using strict `<` instead of `<=`, or forgetting to accumulate the popped
 *   element's `span` (adding 1 instead of `popped.span()`).
 * - One-line mental trigger: "Consecutive prior elements <= today? Pop & absorb spans on a monotonic decreasing stack."
 * =========================================================================================
 */











































import java.util.ArrayDeque;
import java.util.Deque;

class StockSpanner {

    // Each entry represents a compressed block of days.
    //
    // price = price of the representative day.
    // span  = number of consecutive days represented.
    //
    // A record is useful because these two values
    // logically belong together and are immutable.
    private record Entry(int price, int span) {}

    // Maintains prices in strictly decreasing order
    // from bottom to top.
    private final Deque<Entry> stack = new ArrayDeque<>();

    public int next(int price) {

        // Today's price always contributes one day.
        int span = 1;

        // Remove all previous blocks whose prices are
        // less than or equal to today's price.
        //
        // Why <= and not < ?
        // Because the problem explicitly includes equal
        // prices in the stock span.
        while (!stack.isEmpty() &&
               stack.peek().price() <= price) {

            // The popped block consists entirely of days
            // that are less than or equal to today's price.
            //
            // Therefore, we can safely include its entire
            // span without examining individual days.
            Entry previous = stack.pop();

            span += previous.span();
        }

        // Push today's price together with its accumulated
        // span. This entry now represents all the days
        // compressed during the current operation.
        stack.push(new Entry(price, span));

        // Return the number of consecutive qualifying days.
        return span;
    }
}

/*
TIME COMPLEXITY:

For one next() call:

    Worst case: O(n)

    A single call might pop every element currently
    present in the stack.

    However, this is NOT the overall complexity.

For n calls:

    Every entry is pushed exactly once.

    Every entry can be popped at most once.

    Therefore:
        Total pushes = n
        Total pops   <= n

    Total stack operations = O(n).

    Amortized time per next() = O(1).

    Total time for n calls = O(n).

SPACE COMPLEXITY:

    O(n) worst case.

    Consider strictly decreasing prices:
        [100, 90, 80, 70, 60]

    No previous entry gets popped, so the stack
    contains all n entries.

WHY THIS IS OPTIMAL:

    We never revisit a compressed block after removing it.

    Every historical block is processed at most twice:
        1. Once when pushed.
        2. Once when popped.

    This eliminates repeated backward scanning.
*/


