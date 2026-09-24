import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/*
 * Sliding Window Median (Time-based Window)
 *
 * Input stream: (timestamp, value)
 * Query: median of all values in the last 10 minutes.
 *
 * Why is this hard?
 * - Median needs sorted structure.
 * - Two heaps solve median for infinite stream.
 * - BUT sliding window requires removing expired values.
 * - Heaps do NOT support removing arbitrary elements efficiently.
 *
 * Solution:
 * - Use 2 heaps for median:
 *      lower = maxHeap (stores smaller half)
 *      upper = minHeap (stores larger half)
 *
 * - Use lazy deletion:
 *      delayed[value] = how many times this value should be deleted later.
 *
 * - Use queue for expiry:
 *      windowQueue keeps events in arrival order so we can expire old ones.
 *
 * Invariant:
 * - lowerSize >= upperSize
 * - lowerSize <= upperSize + 1
 *
 * Median:
 * - if sizes equal => avg(lower.peek, upper.peek)
 * - else => lower.peek
 *
 * Time Complexity:
 * - process(): amortized O(log N)
 * - getMedian(): O(1) amortized
 *
 * Space Complexity:
 * - O(N) where N = number of events in last 10 minutes
 */
class SlidingMedian {

    private static final long WINDOW_SECONDS = 600; // 10 minutes

    private static class Event {
        long timestamp;
        int value;

        Event(long timestamp, int value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    // MaxHeap: stores smaller half
    private final PriorityQueue<Integer> lower =
            new PriorityQueue<>(Collections.reverseOrder());

    // MinHeap: stores larger half
    private final PriorityQueue<Integer> upper =
            new PriorityQueue<>();

    // Keeps events in FIFO order so we can expire old events
    private final Deque<Event> windowQueue = new ArrayDeque<>();

    // Lazy deletion map: value -> how many times it should be removed
    private final Map<Integer, Integer> delayed = new HashMap<>();

    // Logical sizes (exclude delayed elements still inside heaps)
    private int lowerSize = 0;
    private int upperSize = 0;

    /*
     * Process a new event (timestamp, value).
     *
     * Steps:
     * 1) Expire old events (timestamp <= now - WINDOW)
     * 2) Insert new value into heaps
     * 3) Rebalance heaps
     */
    public void process(long timestamp, int value) {

        expireOldEvents(timestamp);

        // add current event into window queue
        windowQueue.offerLast(new Event(timestamp, value));

        addValue(value);

        rebalanceHeaps();
    }

    /*
     * Return current median of last 10 minutes.
     *
     * Always prune heap roots first to ensure peek() is valid.
     */
    public double getMedian() {

        if (lowerSize + upperSize == 0) {
            return 0.0;
        }

        pruneLower();
        pruneUpper();

        if (lowerSize == upperSize) {
            return ((long) lower.peek() + (long) upper.peek()) / 2.0;
        }

        // lower has one extra element when total count is odd
        return lower.peek();
    }

    // ------------------------------
    // Internal Helpers
    // ------------------------------

    /*
     * Expire events that are outside the last 10 minutes.
     */
    private void expireOldEvents(long timestamp) {

        while (!windowQueue.isEmpty() &&
                windowQueue.peekFirst().timestamp <= timestamp - WINDOW_SECONDS) {

            Event expired = windowQueue.pollFirst();
            removeValue(expired.value);
        }
    }

    /*
     * Add value into one of the heaps.
     */
    private void addValue(int value) {

        pruneLower(); // make sure lower.peek() is not stale

        if (lower.isEmpty() || value <= lower.peek()) {
            lower.offer(value);
            lowerSize++;
        } else {
            upper.offer(value);
            upperSize++;
        }
    }

    /*
     * Remove value lazily.
     *
     * We do not remove it immediately from heap.
     * Instead, we record it in delayed map.
     *
     * Later, if that value appears on heap top, we pop it.
     */
    private void removeValue(int value) {

        delayed.put(value, delayed.getOrDefault(value, 0) + 1);

        pruneLower();
        pruneUpper();

        /*
         * Decide which heap "logically" contains this value.
         *
         * Note:
         * lower.peek() is the max of lower half.
         * If value <= lower.peek(), it belongs to lower half.
         */
        if (!lower.isEmpty() && value <= lower.peek()) {
            lowerSize--;
        } else {
            upperSize--;
        }

        // In case root became invalid, prune again
        pruneLower();
        pruneUpper();

        rebalanceHeaps();
    }

    /*
     * Rebalance heaps so that:
     * - lower has either equal size or one extra element.
     */
    private void rebalanceHeaps() {

        pruneLower();
        pruneUpper();

        // lower too big -> move one to upper
        if (lowerSize > upperSize + 1) {
            upper.offer(lower.poll());
            lowerSize--;
            upperSize++;
        }

        // upper too big -> move one to lower
        else if (upperSize > lowerSize) {
            lower.offer(upper.poll());
            upperSize--;
            lowerSize++;
        }

        pruneLower();
        pruneUpper();
    }

    /*
     * Remove invalid elements from top of lower heap.
     *
     * Why?
     * - lower.peek() might be expired but still inside heap.
     * - delayed map tells how many times it must be removed.
     */
    private void pruneLower() {
        while (!lower.isEmpty()) {

            int top = lower.peek();
            Integer cnt = delayed.get(top);

            if (cnt == null) {
                break; // top is valid
            }

            // remove this expired value from heap
            lower.poll();

            // decrease delayed count
            if (cnt == 1) delayed.remove(top);
            else delayed.put(top, cnt - 1);
        }
    }

    /*
     * Remove invalid elements from top of upper heap.
     */
    private void pruneUpper() {
        while (!upper.isEmpty()) {

            int top = upper.peek();
            Integer cnt = delayed.get(top);

            if (cnt == null) {
                break; // top is valid
            }

            upper.poll();

            if (cnt == 1) delayed.remove(top);
            else delayed.put(top, cnt - 1);
        }
    }
}




class SlidingWindowMedian2 {

    /*
     * MultiSet implemented using TreeMap:
     * - key   = number
     * - value = frequency count
     *
     * Supports:
     * add/remove in O(log N)
     * get min/max in O(log N)
     */
    static class MultiSet {

        private final TreeMap<Integer, Integer> map = new TreeMap<>();
        private int size = 0;

        void add(int x) {
            map.put(x, map.getOrDefault(x, 0) + 1);
            size++;
        }

        void remove(int x) {
            int count = map.get(x);
            if (count == 1) {
                map.remove(x);
            } else {
                map.put(x, count - 1);
            }
            size--;
        }

        int min() {
            return map.firstKey();
        }

        int max() {
            return map.lastKey();
        }

        int size() {
            return size;
        }

        boolean isEmpty() {
            return size == 0;
        }
    }

    /*
     * Main Idea:
     *
     * Maintain 2 multisets:
     * left  = smaller half (max side)
     * right = larger half  (min side)
     *
     * Invariant:
     * left.size == right.size
     * OR
     * left.size == right.size + 1
     *
     * So median always comes from left.max()
     */
    public double[] medianSlidingWindow(int[] nums, int k) {

        int n = nums.length;
        double[] ans = new double[n - k + 1];

        MultiSet left = new MultiSet();   // contains smaller half
        MultiSet right = new MultiSet();  // contains larger half

        // -------------------------------
        // Step 1: Build first window
        // -------------------------------
        for (int i = 0; i < k; i++) {
            add(nums[i], left, right);
        }
        ans[0] = getMedian(left, right, k);

        // -------------------------------
        // Step 2: Slide the window
        // -------------------------------
        for (int i = k; i < n; i++) {

            int outgoing = nums[i - k];
            int incoming = nums[i];

            remove(outgoing, left, right);
            add(incoming, left, right);

            ans[i - k + 1] = getMedian(left, right, k);
        }

        return ans;
    }

    /*
     * Add element into correct half:
     * - if num <= left.max() => belongs to left
     * - else => belongs to right
     *
     * Then rebalance.
     */
    private void add(int num, MultiSet left, MultiSet right) {

        if (left.isEmpty() || num <= left.max()) {
            left.add(num);
        } else {
            right.add(num);
        }

        rebalance(left, right);
    }

    /*
     * Remove element from correct half:
     *
     * We decide based on boundary left.max():
     * - if num <= left.max() => should be in left
     * - else => should be in right
     *
     * Then rebalance.
     */
    private void remove(int num, MultiSet left, MultiSet right) {

        if (num <= left.max()) {
            left.remove(num);
        } else {
            right.remove(num);
        }

        rebalance(left, right);
    }

    /*
     * Rebalance condition:
     *
     * We want:
     * left.size == right.size
     * OR
     * left.size == right.size + 1
     *
     * Meaning:
     * - left can have at most 1 extra element.
     * - median always comes from left side.
     */
    private void rebalance(MultiSet left, MultiSet right) {

        // If left is too large, move max(left) -> right
        while (left.size() > right.size() + 1) {
            int val = left.max();
            left.remove(val);
            right.add(val);
        }

        // If right is larger, move min(right) -> left
        while (right.size() > left.size()) {
            int val = right.min();
            right.remove(val);
            left.add(val);
        }
    }

    /*
     * Median logic:
     *
     * If k is odd:
     *   median = max(left)
     *
     * If k is even:
     *   median = (max(left) + min(right)) / 2
     */
    private double getMedian(MultiSet left, MultiSet right, int k) {

        if (k % 2 == 1) {
            return left.max();
        }

        long a = left.max();
        long b = right.min();

        return (a + b) / 2.0;
    }
}
