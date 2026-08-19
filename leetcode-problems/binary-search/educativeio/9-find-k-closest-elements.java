import java.util.*;

class KClosest {

    /*
     * ================================================================
     * PROBLEM, RESTATED IN MY OWN WORDS
     * ================================================================
     * Given a SORTED array `nums`, find the `k` elements closest to
     * `target`, and return them in sorted order. On a distance tie,
     * prefer the smaller value.
     *
     * ================================================================
     * STEP 1 — WHY BRUTE FORCE ISN'T GOOD ENOUGH (but is worth saying)
     * ================================================================
     * Naive idea: sort all n elements by (distance to target, value),
     * take the first k, then re-sort those k back into array order.
     * That's O(n log n). It works, but it throws away the fact that
     * `nums` is ALREADY sorted. We should be able to use that.
     *
     * ================================================================
     * STEP 2 — THE KEY INSIGHT: the answer is always a CONTIGUOUS WINDOW
     * ================================================================
     * Because nums is sorted, if some value `a` and some value `c`
     * (with a < c) both belong in our answer, then every value `b`
     * strictly between them (a < b < c) must be at LEAST as close to
     * target as a or c is. So we'd never "skip over" b while keeping
     * both a and c. That means the k closest elements always sit
     * next to each other in the array — a window of size k.
     *
     * This reframes the problem:
     *   "pick k closest elements"   -->   "pick the best window of size k"
     *
     * ================================================================
     * STEP 3 — HOW MANY WINDOWS ARE THERE?
     * ================================================================
     * A window is defined entirely by its starting index.
     * Valid starting indices range from 0 to (n - k), inclusive.
     * That's only (n - k + 1) windows — way fewer than "all subsets."
     * We binary search over THIS range (starting indices), not over
     * the array values themselves.
     *
     * ================================================================
     * STEP 4 — COMPARING TWO ADJACENT WINDOWS (the heart of the trick)
     * ================================================================
     * Take window starting at `mid` vs window starting at `mid + 1`:
     *
     *   Window A (start = mid):    nums[mid]   ... nums[mid+k-1]
     *   Window B (start = mid+1):  nums[mid+1] ... nums[mid+k]
     *
     * They share (k-1) elements in the middle. The ONLY difference:
     *   - A has one extra element on the LEFT   -> nums[mid]
     *   - B has one extra element on the RIGHT  -> nums[mid+k]
     *
     * So deciding "is A or B better" collapses into ONE comparison:
     *   is nums[mid] (left outsider) farther from target,
     *   or is nums[mid+k] (right outsider) farther from target?
     *   Whichever is farther is the "weak link" we drop.
     *
     * Since the region we search keeps nums[mid] <= target <= nums[mid+k]
     * (true once we're near the optimal window), we can compare plain
     * differences instead of absolute values:
     *
     *   leftGap  = target - nums[mid]      (how far target is from left edge)
     *   rightGap = nums[mid + k] - target  (how far target is from right outsider)
     *
     *   if leftGap > rightGap  -> left edge is the weak link -> shift window right
     *   else                   -> right side is the weak link (or tie) -> keep left,
     *                              try to shrink further left
     *
     * TIE-BREAK CHECK: on leftGap == rightGap, we fall into the "else"
     * branch, i.e. we KEEP the smaller value (nums[mid], the left edge).
     * That's exactly the tie-break rule the problem wants — it falls
     * out for free, we don't need special-case code for it.
     *
     * ================================================================
     * STEP 5 — WHY BINARY SEARCH IS VALID HERE (monotonicity)
     * ================================================================
     * As `mid` increases, the window slides right. The answer to
     * "is the left edge the weak link?" flips from false -> true at
     * most once and then STAYS true (once the window has drifted far
     * enough right that left is clearly worse, sliding further right
     * only makes that more true). That single monotonic flip is
     * exactly the condition binary search needs to be correct.
     *
     * ================================================================
     * MENTAL MODEL (say this out loud in an interview)
     * ================================================================
     * "At each step, I'm not comparing whole windows — I'm asking
     *  one question: which edge of my current window is the weak
     *  link, and can I trade it away for something better?"
     *
     * ================================================================
     * COMPLEXITY
     * ================================================================
     * Time:  O(log(n - k))  for the search   +  O(k) to copy out the window
     * Space: O(k) for the output list (O(1) extra beyond that)
     */
    public static List<Integer> findClosestElements(int[] nums, int k, int target) {

        int n = nums.length;

        // low/high range over WINDOW START INDICES, not array values.
        // Highest valid start is (n - k) so the window nums[start .. start+k-1]
        // never runs off the end of the array.
        int low = 0;
        int high = n - k;

        // Track the best starting index found so far. We keep updating
        // this every time we find a window that's "good enough" and
        // then keep trying to find an even better (further left) one.
        int answerStart = 0;

        while (low <= high) {

            int mid = low + (high - low) / 2;

            // Left outsider of the CURRENT window (its own leftmost element)
            // Right outsider = first element just past the current window
            int leftGap = target - nums[mid];         // distance from left edge to target
            int rightGap = nums[mid + k] - target;     // distance from target to right outsider

            if (leftGap > rightGap) {
                // Left edge is farther from target than the right outsider is.
                // That means shifting the window one step right (dropping the
                // left edge, picking up the right outsider) gets us closer.
                low = mid + 1;
            } else {
                // Right outsider is farther (or exactly tied). This window,
                // starting at mid, is at least as good as anything we've
                // seen -> record it as a candidate, then keep searching
                // LEFT in case an even better (or equally good but smaller)
                // window exists.
                answerStart = mid;
                high = mid - 1;
            }
        }

        // Build the final answer from the best starting index found.
        List<Integer> result = new ArrayList<>();
        for (int i = answerStart; i < answerStart + k; i++) {
            result.add(nums[i]);
        }
        return result;
    }

    /*
     * ================================================================
     * DRY RUN — traced fully, matching the code above line-by-line
     * ================================================================
     *
     * nums = [-20, -15, -5, 3, 8, 10, 12, 15, 25, 40]
     * idx  =    0    1   2  3  4  5   6   7   8   9
     * n = 10, k = 4, target = 11
     *
     * Valid window starts: 0 .. (n - k) = 0 .. 6
     * low = 0, high = 6, answerStart = 0
     *
     * ---- Iteration 1 ----
     * mid = (0 + 6) / 2 = 3
     * window at mid=3 -> nums[3..6] = [3, 8, 10, 12]
     * right outsider   -> nums[mid + k] = nums[7] = 15
     *
     * leftGap  = target - nums[mid] = 11 - 3  = 8
     * rightGap = nums[mid+k] - target = 15 - 11 = 4
     *
     * leftGap(8) > rightGap(4)?  YES
     * -> left edge (3) is the weak link -> shift right
     * -> low = mid + 1 = 4
     * (answerStart stays 0 for now, will be overwritten later)
     *
     * ---- Iteration 2 ----
     * low = 4, high = 6
     * mid = (4 + 6) / 2 = 5
     * window at mid=5 -> nums[5..8] = [10, 12, 15, 25]
     * right outsider   -> nums[mid + k] = nums[9] = 40
     *
     * leftGap  = 11 - nums[5] = 11 - 10 = 1
     * rightGap = nums[9] - 11 = 40 - 11 = 29
     *
     * leftGap(1) > rightGap(29)? NO
     * -> right outsider (40) is the weak link -> keep left, try to shrink further
     * -> answerStart = 5
     * -> high = mid - 1 = 4
     *
     * ---- Iteration 3 ----
     * low = 4, high = 4
     * mid = (4 + 4) / 2 = 4
     * window at mid=4 -> nums[4..7] = [8, 10, 12, 15]
     * right outsider   -> nums[mid + k] = nums[8] = 25
     *
     * leftGap  = 11 - nums[4] = 11 - 8 = 3
     * rightGap = nums[8] - 11 = 25 - 11 = 14
     *
     * leftGap(3) > rightGap(14)? NO
     * -> right outsider (25) is the weak link -> keep left, try to shrink further
     * -> answerStart = 4
     * -> high = mid - 1 = 3
     *
     * ---- Loop check ----
     * low = 4, high = 3 -> low > high -> loop ends
     *
     * Final answerStart = 4
     * Result window: nums[4..7] = [8, 10, 12, 15]
     *
     * ---- Sanity check against brute force distances from target=11 ----
     *   -20 -> 31   -15 -> 26   -5 -> 16   3 -> 8
     *    8  -> 3     10 -> 1    12 -> 1   15 -> 4
     *    25 -> 14    40 -> 29
     * 4 smallest distances (tie-break smaller value first): 10, 12, 8, 15
     * sorted back into array order -> [8, 10, 12, 15]  ✅ matches
     *
     * ================================================================
     * NOTE on this loop style vs the "low < high, high = mid" style
     * ================================================================
     * This version uses `while (low <= high)` with `high = mid - 1` and
     * an explicit `answerStart` that gets overwritten with better and
     * better candidates as the search narrows. It's functionally
     * equivalent to the `while (low < high) { high = mid; }` style —
     * just a different bookkeeping convention. Pick ONE convention and
     * be consistent; mixing them is the most common source of bugs
     * (infinite loops or off-by-one misses) in this pattern.
     */

    /*
     * ================================================================
     * ALTERNATIVE APPROACH — MAX-HEAP OF SIZE k
     * ================================================================
     *
     * STEP 1 — WHERE THIS APPROACH FITS
     * ------------------------------------------------------------
     * The binary search solution above LEANS HARD on the array being
     * sorted (that's what makes "the answer is a contiguous window"
     * true in the first place). If nums were NOT sorted, that entire
     * argument falls apart, and binary search on window-start doesn't
     * even make sense.
     *
     * The heap approach doesn't care whether nums is sorted. It's the
     * general tool for "give me the k best items out of a stream by
     * some score" — so it's worth having in your pocket even though
     * it's asymptotically worse here, because THIS problem happens to
     * hand you a sorted array for free.
     *
     * Say this explicitly in an interview:
     *   "Since nums is sorted, binary search on the window gets me
     *    O(log(n-k) + k). If it weren't sorted, or if elements were
     *    arriving as a stream, I'd reach for a heap instead, at
     *    O(n log k)."
     *
     * STEP 2 — THE CORE IDEA
     * ------------------------------------------------------------
     * Maintain a MAX-heap (by distance to target) of size at most k.
     *
     *   - Push every element in.
     *   - The moment the heap exceeds size k, pop the WORST element
     *     (largest distance from target — and on a tie, the larger
     *     value, since we prefer to keep smaller values).
     *
     * After processing all n elements, whatever remains in the heap
     * is exactly the k closest elements — because at every point we
     * only ever evicted the single worst element seen so far, and the
     * heap always had room for a genuinely better replacement.
     *
     * Why a MAX-heap and not a MIN-heap? Because we want O(log k) access
     * to the WORST element currently kept, so we can throw it out fast
     * when something better shows up. A min-heap would give us fast
     * access to the BEST element, which isn't what we need to evict.
     *
     * STEP 3 — TIE-BREAK REASONING
     * ------------------------------------------------------------
     * Problem rule: on equal distance, prefer the SMALLER value.
     * That means, among equally-bad-distance elements, the LARGER
     * value is the one we want to evict first. So in the heap
     * ordering: same distance -> larger value should sort as "worse"
     * (closer to the top, first to be popped).
     *
     * STEP 4 — WHY A record INSTEAD OF RAW Integer
     * ------------------------------------------------------------
     * The version that heaps raw Integers and recomputes
     * Math.abs(value - target) inside the comparator recalculates
     * that distance on EVERY comparison during heap sift-up/down —
     * wasted work, since the distance for a given value never changes.
     *
     * Precomputing distance once per element and storing (value, distance)
     * together removes that redundant work, and using a `record` here
     * is a clean fit: this is exactly the "small immutable bundle of
     * data with no behavior" case records exist for.
     *
     * STEP 5 — COMPLEXITY
     * ------------------------------------------------------------
     * Time:  O(n log k)   — n elements, each heap op is O(log k)
     * Space: O(k)          — heap never holds more than k elements
     *
     * Compare to binary search: O(log(n-k) + k) time, O(k) space.
     * For large n and small k, binary search wins by a lot — this is
     * a good number to say out loud if asked "which would you pick."
     *
     * STEP 6 — EDGE CASE WORTH MENTIONING
     * ------------------------------------------------------------
     * Math.abs(Integer.MIN_VALUE) overflows (there's no positive
     * counterpart to Integer.MIN_VALUE in a 32-bit int). Given this
     * problem's constraints (values bounded to +/-10^4), it can't
     * actually happen here — but naming the edge case unprompted is
     * the kind of thing that separates "solved the problem" from
     * "understands the data types."
     */

    // Small immutable bundle: a value paired with its precomputed
    // distance to target, so we never recompute Math.abs mid-heap-op.
    private record NumDist(int value, int distance) {}

    public static List<Integer> findClosestElementsHeap(int[] nums, int k, int target) {

        // MAX-heap ordered so the WORST element (to evict) is always
        // at the head:
        //   - larger distance  -> worse -> comes first
        //   - on tie, larger value -> worse -> comes first
        PriorityQueue<NumDist> maxHeap = new PriorityQueue<>(
            (a, b) -> {
                if (a.distance() == b.distance()) {
                    return b.value() - a.value();   // larger value evicted first on tie
                }
                return b.distance() - a.distance();  // larger distance evicted first
            }
        );

        for (int num : nums) {
            maxHeap.offer(new NumDist(num, Math.abs(num - target)));
            if (maxHeap.size() > k) {
                maxHeap.poll();   // evict the current worst element
            }
        }

        // Heap now holds exactly the k closest elements, but in heap
        // order (not array order) -> extract values and sort.
        List<Integer> result = new ArrayList<>();
        for (NumDist nd : maxHeap) {
            result.add(nd.value());
        }
        Collections.sort(result);
        return result;
    }

    /*
     * ================================================================
     * DRY RUN — heap approach, same example as before
     * ================================================================
     * nums = [-20, -15, -5, 3, 8, 10, 12, 15, 25, 40], k = 4, target = 11
     *
     * distances: -20->31  -15->26  -5->16  3->8  8->3
     *             10->1    12->1    15->4  25->14  40->29
     *
     * Walk left to right, heap capped at size 4 (showing heap CONTENTS
     * as a set of (value, distance) — exact internal tree order doesn't
     * matter, only "what's in vs out"):
     *
     *  process -20 (d=31): heap = {-20}
     *  process -15 (d=26): heap = {-20,-15}
     *  process  -5 (d=16): heap = {-20,-15,-5}
     *  process   3 (d=8):  heap = {-20,-15,-5,3}          size==4, full
     *  process   8 (d=3):  push 8 -> size 5 -> evict worst.
     *                      worst = largest distance = -20 (d=31) -> evict
     *                      heap = {-15,-5,3,8}
     *  process  10 (d=1):  push 10 -> size 5 -> evict worst.
     *                      worst = -15 (d=26) -> evict
     *                      heap = {-5,3,8,10}
     *  process  12 (d=1):  push 12 -> size 5 -> evict worst.
     *                      worst = -5 (d=16) -> evict
     *                      heap = {3,8,10,12}
     *  process  15 (d=4):  push 15 -> size 5 -> evict worst.
     *                      worst = 3 (d=8) -> evict
     *                      heap = {8,10,12,15}
     *  process  25 (d=14): push 25 -> size 5 -> evict worst.
     *                      worst = 25 itself (d=14, largest among
     *                      {8,10,12,15,25} = {3,1,1,4,14}) -> evict 25
     *                      heap = {8,10,12,15}   (unchanged)
     *  process  40 (d=29): push 40 -> size 5 -> evict worst.
     *                      worst = 40 (d=29) -> evict
     *                      heap = {8,10,12,15}   (unchanged)
     *
     * Final heap contents: {8, 10, 12, 15}
     * Sorted -> [8, 10, 12, 15]  ✅ matches binary search result
     *
     * Notice this approach DID touch every element (all 10), unlike
     * binary search which skipped the far-left region entirely after
     * one comparison. That's the O(n log k) vs O(log(n-k)) difference
     * made concrete.
     */

    // Quick manual test harness
    public static void main(String[] args) {
        int[] nums = {-20, -15, -5, 3, 8, 10, 12, 15, 25, 40};
        int k = 4;
        int target = 11;

        System.out.println("Binary search: " + findClosestElements(nums, k, target));
        System.out.println("Heap approach: " + findClosestElementsHeap(nums, k, target));
        // Both expected: [8, 10, 12, 15]
    }
}

/**
 * Problem Statement:
 * Given a sorted integer array `nums`, two integers `k` and `target`.
 * Return `k` number of integers that are closest to the `target` value.
 * The output must be sorted in ascending order.
 * 
 * Rules for "closer":
 * - |a - target| < |b - target|
 * - If |a - target| == |b - target|, `a` is closer if a < b.
 * 
 * Constraints:
 * - 1 <= k <= nums.length <= 10^3
 * - -10^4 <= nums[i], target <= 10^4
 * - nums is sorted in ascending order.
 */
class KClosest2 {

    /**
     * SOLUTION 1: Binary Search for the Window Start (Optimal)
     * 
     * Time Complexity: O(log(N - K) + K)
     * Space Complexity: O(1) (excluding the output list)
     * 
     * VISUAL EXPLANATION:
     * Instead of searching for the element, we search for the STARTING INDEX of our k-sized window.
     * The valid range for the starting index is [0, nums.length - k].
     * 
     * Array: [1, 2, 3, 4, 5], k = 4, target = 3
     * 
     * Let's say mid = 0. Window would be [1, 2, 3, 4]. The next potential element is at mid + k = 4 (value 5).
     * We compare the element dropping out of the window (nums[mid] = 1) with the element coming in (nums[mid+k] = 5).
     * Distance of 1 to target 3 is 2. Distance of 5 to target 3 is 2.
     * Since distances are equal, the smaller value (1) wins. So window starting at 0 is better.
     * 
     * Mathematical trick: 
     * `target - nums[mid] > nums[mid + k] - target` 
     * This automatically handles absolute distances because the array is sorted.
     */
    public static List<Integer> findClosestElements(int[] nums, int k, int target) {
        int low = 0;
        int high = nums.length - k;
        
        // Explicit result variable to store the starting index of our k-window
        int result = high; 

        while (low <= high) {
            int mid = low + (high - low) / 2;

            // Check if the element at mid + k is strictly closer to target than the element at mid
            if (mid + k < nums.length && (target - nums[mid] > nums[mid + k] - target)) {
                // The right side is better, shift window start to the right
                low = mid + 1;
            } else {
                // The left side (mid) is better or equal. Save it, and try to find an even earlier valid window
                result = mid;
                high = mid - 1;
            }
        }

        // Collect the k elements starting from our determined 'result' index
        List<Integer> closestElements = new ArrayList<>(k);
        for (int i = result; i < result + k; i++) {
            closestElements.add(nums[i]);
        }
        return closestElements;
    }

    /**
     * SOLUTION 2: Binary Search + Expand Around Center
     * 
     * Time Complexity: O(log N + K)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * 1. Use Binary Search to find the insertion point of `target`.
     * 2. Set two pointers: left = insert_point - 1, right = insert_point.
     * 3. Expand the window outward `k` times by comparing the left and right elements.
     */
    public static List<Integer> findClosestElementsExpand(int[] nums, int k, int target) {
        int low = 0;
        int high = nums.length - 1;
        int insertPos = nums.length; // Explicit result variable for BS

        // Step 1: Find the insertion position of target
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (nums[mid] >= target) {
                insertPos = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }

        // Step 2 & 3: Expand outward from the insertion position
        int left = insertPos - 1;
        int right = insertPos;

        while (k > 0) {
            if (left < 0) {
                right++;
            } else if (right >= nums.length) {
                left--;
            } else {
                int leftDist = Math.abs(nums[left] - target);
                int rightDist = Math.abs(nums[right] - target);

                // Tie goes to the left (smaller value)
                if (leftDist <= rightDist) {
                    left--;
                } else {
                    right++;
                }
            }
            k--;
        }

        // Collect elements from left+1 to right-1
        List<Integer> resultList = new ArrayList<>();
        for (int i = left + 1; i < right; i++) {
            resultList.add(nums[i]);
        }
        return resultList;
    }

    /**
     * SOLUTION 3: Two Pointers (Shrinking Window)
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(1)
     * 
     * EXPLANATION:
     * Put pointers at the start and end of the array. The window size is N.
     * We need a window size of K.
     * Simply compare the start and end elements, and shrink the window from the side 
     * that has the element furthest from the target.
     */
    public static List<Integer> findClosestElementsShrink(int[] nums, int k, int target) {
        int low = 0;
        int high = nums.length - 1;

        // Shrink the bounds until the distance between them is exactly k - 1
        while (high - low >= k) {
            int leftDist = Math.abs(nums[low] - target);
            int rightDist = Math.abs(nums[high] - target);

            // If right element is further or equal distance, drop it (because tie favors smaller element)
            if (leftDist <= rightDist) {
                high--;
            } else {
                low++;
            }
        }

        List<Integer> resultList = new ArrayList<>(k);
        for (int i = low; i <= high; i++) {
            resultList.add(nums[i]);
        }
        return resultList;
    }

    /**
     * SOLUTION 4: Java Streams / Custom Sorting
     * 
     * Time Complexity: O(N log N)
     * Space Complexity: O(N)
     * 
     * EXPLANATION:
     * Using Java's stream API, we can define a custom comparator that ranks elements
     * primarily by distance to target, and secondarily by value.
     * We slice the top `k` elements, and then sort them normally to fulfill the return format.
     */
    public static List<Integer> findClosestElementsStream(int[] nums, int k, int target) {
        return Arrays.stream(nums)
                .boxed() // Convert int to Integer
                .sorted((a, b) -> {
                    int diffA = Math.abs(a - target);
                    int diffB = Math.abs(b - target);
                    if (diffA == diffB) {
                        return Integer.compare(a, b); // Tie breaker
                    }
                    return Integer.compare(diffA, diffB); // Sort by closeness
                })
                .limit(k) // Take the k closest
                .sorted() // Sort ascending for final output
                .toList(); // Available in Java 16+
    }

    // ==========================================
    // TESTING FRAMEWORK USING JAVA RECORDS
    // ==========================================

    /**
     * Java Record to cleanly map input arrays, targets, k values, and expected outputs.
     */
    public record TestCase(int[] nums, int k, int target, List<Integer> expected) {}

    public static void main(String[] args) {
        // Defining test cases based on constraints
        TestCase[] testCases = {
            new TestCase(new int[]{1, 2, 3, 4, 5}, 4, 3, List.of(1, 2, 3, 4)),
            new TestCase(new int[]{1, 2, 3, 4, 5}, 4, -1, List.of(1, 2, 3, 4)), // Target way to the left
            new TestCase(new int[]{1, 2, 3, 4, 5}, 4, 10, List.of(2, 3, 4, 5)), // Target way to the right
            new TestCase(new int[]{1, 1, 1, 10, 10, 10}, 1, 9, List.of(10)),    // Close to cluster on right
            new TestCase(new int[]{10, 20, 30, 40, 50}, 2, 35, List.of(30, 40)) // Exact middle, favors smaller
        };

        System.out.println("--- Running Tests ---");

        for (int i = 0; i < testCases.length; i++) {
            TestCase tc = testCases[i];
            
            List<Integer> resWindowBS = findClosestElements(tc.nums(), tc.k(), tc.target());
            List<Integer> resExpand   = findClosestElementsExpand(tc.nums(), tc.k(), tc.target());
            List<Integer> resShrink   = findClosestElementsShrink(tc.nums(), tc.k(), tc.target());
            List<Integer> resStream   = findClosestElementsStream(tc.nums(), tc.k(), tc.target());

            boolean passed = resWindowBS.equals(tc.expected()) &&
                             resExpand.equals(tc.expected()) &&
                             resShrink.equals(tc.expected()) &&
                             resStream.equals(tc.expected());

            System.out.printf("Test %d | Array: %-18s | k: %d | Target: %-2d -> Expected: %s | Passed: %b%n",
                    i + 1, Arrays.toString(tc.nums()), tc.k(), tc.target(), tc.expected(), passed);
            
            if (!passed) {
                System.out.printf("   [Failed] WindowBS: %s, Expand: %s, Shrink: %s, Stream: %s%n",
                        resWindowBS, resExpand, resShrink, resStream);
            }
        }
    }
}
