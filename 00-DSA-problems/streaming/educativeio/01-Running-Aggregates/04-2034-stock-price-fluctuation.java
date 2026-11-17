/**
 * 0. ## Problem Statement
 * Design an algorithm to process a stream of stock price records.
 * Records have a `timestamp` and a `price`, but they may arrive out of order, 
 * and later records for the same timestamp CORRECT the previous price.
 * 
 * Implement `StockPrice`:
 * - `StockPrice()`: Initializes the object.
 * - `void update(int timestamp, int price)`: Updates the stock price at the given timestamp.
 * - `int current()`: Returns the price at the latest recorded timestamp.
 * - `int maximum()`: Returns the max price across all current valid records.
 * - `int minimum()`: Returns the min price across all current valid records.
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can timestamps be negative, or are they strictly positive?"
 *    -> Avoids edge cases with tracking the 'maximum timestamp' (0 is a safe initial value if strictly positive).
 * 2. "If an update comes in for the 'current' (latest) timestamp, does it immediately change the current price?"
 *    -> Yes, corrections apply instantly to all stats: current, min, and max.
 * 3. "Are we optimizing for frequent updates, or frequent reads?"
 *    -> Determines our data structure: lazy deletion with heaps vs eager deletion with TreeMaps.
 * 4. "Can prices be updated to the exact same value they already had?"
 *    -> Helps avoid unnecessary remove/add operations if we short-circuit the update logic.
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to 10^5 operations. We need to do updates, point lookups (for current), 
 * and min/max queries efficiently. If min/max takes O(N), we hit 10^10 operations (TLE).
 * 
 * --- Approach 1: The Brute Force HashMap ---
 * Idea: Keep a `HashMap<Integer, Integer> timeToPrice` and a variable `latestTime`. 
 * On `update(time, price)`, update the map and `latestTime`.
 * On `current()`, return `timeToPrice.get(latestTime)`. 
 * On `maximum()` or `minimum()`, iterate through all values in the HashMap to find the extrema.
 * - Why it works: Perfectly models the corrections because HashMap handles overwrites naturally.
 * - Why it's too slow: Finding the min or max scans the entire map every time.
 * Time Complexity: `update`: O(1), `current`: O(1), `maximum`/`minimum`: O(N) — because we scan all N unique timestamps.
 * Space Complexity: O(N) — because we store N timestamp-price pairs.
 * 
 * --- Approach 2: Two Heaps with Lazy Deletion ---
 * Idea: To fix the O(N) min/max lookups, we can use a Min-Heap and a Max-Heap storing `[price, timestamp]`.
 * We still keep the `HashMap<Integer, Integer>` to know the *true* current price of any timestamp.
 * When `update` happens, we just push the new `[price, timestamp]` into both heaps. We do NOT search 
 * the heaps to remove the old price (which takes O(N) in a PriorityQueue).
 * Instead, when `maximum()` is called, we check the top of the Max-Heap. If `heap.peek().price != map.get(heap.peek().timestamp)`, 
 * we know this is a stale record. We pop it and check the next one, until the top is valid.
 * - Why it works: It delays the expensive removal until it actually matters.
 * - Why it's slightly costly: The heaps can grow infinitely large with stale data if timestamps are updated frequently.
 * Time Complexity: `update`: O(log M) (where M is total updates, not unique), `maximum`/`minimum`: amortized O(1) but worst-case O(M) if many stale records pile up.
 * Space Complexity: O(M) — because we store every single update in the heaps, even corrections.
 * 
 * --- Approach 3: HashMap + TreeMap (Eager Deletion - Optimal) ---
 * Idea: Use a `HashMap` for `timestamp -> price` lookups. Use a `TreeMap<Integer, Integer>` for 
 * `price -> frequency`. A TreeMap keeps keys (prices) sorted.
 * On `update(time, price)`: check if `time` already exists in the HashMap. If it does, grab the `oldPrice`, 
 * go to the TreeMap, and decrement its frequency. If frequency hits 0, remove it. Then add `price` to TreeMap.
 * - Why it works: TreeMaps in Java are Red-Black trees. They allow O(log N) insertion, deletion, and 
 *   O(log N) min/max lookups (via `firstKey()` and `lastKey()`). By tracking frequency, we handle duplicate prices perfectly.
 * Time Complexity: `update`: O(log N) — because TreeMap insertion/deletion is logarithmic. 
 * `current`: O(1). `maximum`/`minimum`: O(log N) (or O(1) in some tree implementations, but strictly O(log N) in Java's TreeMap traversal).
 * Space Complexity: O(N) — because we strictly store at most N unique timestamps and N unique prices.
 * 
 * Verdict: I would write Approach 3 (TreeMap) in a 45-minute interview. It explicitly controls space O(N) 
 * preventing heap bloat, and the logic of "remove old, add new" is incredibly clean and intuitive to 
 * explain without diving into the intricacies of lazy-deletion state management.
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Updating a price to the exact same value: We can skip tree operations to save time.
 * - Overwriting the absolute max or min price: The frequency counter hits 0, the node is removed, 
 *   and the TreeMap naturally exposes the *next* highest/lowest as `lastKey()` / `firstKey()`.
 * - Updating an older timestamp: The `latestTime` must NOT be updated. `max(latestTime, newTime)` handles this.
 * - Duplicate prices across different timestamps: Frequency tracking ensures we don't delete a price 
 *   from the TreeMap just because one timestamp changed, while another timestamp still has that price.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * How do we delete an element from a sorted structure quickly? 
 * A standard `PriorityQueue.remove(Object)` is O(N). A `TreeMap` removes by key in O(log N). 
 * Whenever you need to *remove specific elements* from a sorted collection dynamically, a BST/TreeMap 
 * is usually superior to a Heap.
 * 
 * -- ASCII Diagram (TreeMap Frequency Updates) --
 * update(1, 10), update(2, 5), update(1, 3)
 * 
 * State after T=1: 
 *   Map: {1 -> 10}
 *   Tree: [10 (freq: 1)]
 * 
 * State after T=2: 
 *   Map: {1 -> 10, 2 -> 5}
 *   Tree: [5 (freq: 1)] ---> [10 (freq: 1)]
 * 
 * Update T=1 to 3:
 *   1. Look up old price for T=1 -> it's 10.
 *   2. Decrement freq of 10 in Tree -> freq=0 -> REMOVE node.
 *   3. Add 3 to Tree.
 *   Map: {1 -> 3, 2 -> 5}
 *   Tree: [3 (freq: 1)] ---> [5 (freq: 1)]  <-- Max is now 5!
 * 
 * -- Dry run (TreeMap Approach) --
 * Calls: update(1, 10), update(2, 5), maximum(), update(1, 3), maximum()
 * 1. update(1, 10):
 *    - maxTime = max(0, 1) = 1
 *    - oldPrice for 1? None.
 *    - freqMap puts (10, 1). 
 *    - timeMap puts (1, 10).
 * 2. update(2, 5):
 *    - maxTime = max(1, 2) = 2
 *    - freqMap puts (5, 1). 
 *    - timeMap puts (2, 5).
 * 3. maximum():
 *    - freqMap.lastKey() -> 10. (Returns 10).
 * 4. update(1, 3):
 *    - maxTime = max(2, 1) = 2 (Unchanged, 2 is still latest).
 *    - oldPrice for 1 = 10.
 *    - freqMap updates (10) -> count goes 1 to 0 -> Removes 10.
 *    - freqMap puts (3, 1).
 *    - timeMap puts (1, 3).
 * 5. maximum():
 *    - freqMap.lastKey() -> 5. (Returns 5).
 * 
 * -- Pitfalls --
 * 1. Forgetting to track duplicates. If you use a `TreeSet` based strictly on price, two different 
 *    timestamps having the price `10` will collapse into one entry. When one updates, you delete `10`, 
 *    ruining the data for the other. A Frequency Map (`TreeMap<Integer, Integer>`) is mandatory.
 * 2. Using `PriorityQueue.remove(Object)`. It compiles and runs, but it does a linear scan O(N) internally, 
 *    leading to TLE on LeetCode.
 * 
 * -- Pattern recognition --
 * When you see "Stream of data + Updates/Corrections + Need Min/Max", think **TreeMap (Frequency Map)** 
 * or **Two Heaps + Lazy Deletion**. 
 * Transfers to: Sliding Window Median (LeetCode 480).
 * 
 * -- Interview script --
 * "To handle point queries for the latest time, a HashMap is perfect. But we also need the global min and max. 
 * If we use a Heap, finding a corrected price to remove it takes O(N) time. Instead, I'll use a TreeMap to 
 * map Prices to their Frequencies. When an update corrects a price, I can look up the old price in O(1) via the 
 * HashMap, decrement its frequency in the TreeMap in O(log N), and add the new price in O(log N). This keeps 
 * our memory footprint strictly at O(N) and all operations fast."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: How would you handle memory if this ran indefinitely and we only cared about the last 24 hours?
 * A: We would need an eviction mechanism. Since we can't easily query "oldest" by timestamp if they 
 *    come out of order, we could maintain a second `TreeMap<Integer, Integer>` (Time -> Price). 
 *    Periodically, we check `timeTree.firstKey()`, and if it's older than `latestTime - 24h`, we 
 *    pop it and remove its price from the `priceFrequency` map.
 * 
 * Q: What if `current()` is called 1000x more frequently than updates?
 * A: Our solution is already optimal! `current()` is an O(1) lookup on the HashMap using `maxTimestamp`.
 * 
 * Q: What if two different stocks were tracked in this same class?
 * A: We would wrap the internal state (`timeToPrice`, `priceFreq`, `maxTimestamp`) into a `Stock` class, 
 *    and keep a `HashMap<String, Stock>` matching ticker symbols to their respective states.
 * =========================================================================================
 */

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StockPriceStudyNote {

    static class StockPrice {
        
        // Maps a specific Timestamp to its actual, most up-to-date Price.
        // Example: { 1: 10, 2: 5 }
        private Map<Integer, Integer> timeToPrice;
        
        // Maps a Price to how many times it currently exists across all valid timestamps.
        // TreeMap keeps the keys (Prices) sorted so we can grab the min/max in O(log N).
        // Example: { 5: 1, 10: 1 }
        private TreeMap<Integer, Integer> priceFrequency;
        
        // Tracks the absolute latest timestamp we have ever seen.
        // This is strictly used to answer the `current()` query.
        private int latestTimestamp;

        public StockPrice() {
            timeToPrice = new HashMap<>();
            priceFrequency = new TreeMap<>();
            latestTimestamp = 0;
        }
        
        public void update(int timestamp, int price) {
            // 1. Maintain the latest timestamp seen so far.
            // Even if an older record arrives (e.g., updating t=1 when we already have t=5),
            // latestTimestamp must remain 5.
            latestTimestamp = Math.max(latestTimestamp, timestamp);
            
            // 2. Handle Corrections (Eager Deletion)
            // If this timestamp already has a recorded price, we must "undo" that old price's
            // presence in our frequency map.
            if (timeToPrice.containsKey(timestamp)) {
                int oldPrice = timeToPrice.get(timestamp);
                
                // Short-circuit: If the correction is exactly the same price, do nothing.
                if (oldPrice == price) {
                    return;
                }
                
                // Decrement the old price's frequency.
                int oldFreq = priceFrequency.get(oldPrice);
                if (oldFreq == 1) {
                    // If it was the only one, remove the node entirely so it doesn't 
                    // falsely act as a min or max.
                    priceFrequency.remove(oldPrice);
                } else {
                    priceFrequency.put(oldPrice, oldFreq - 1);
                }
            }
            
            // 3. Insert the new price
            timeToPrice.put(timestamp, price);
            priceFrequency.put(price, priceFrequency.getOrDefault(price, 0) + 1);
        }
        
        public int current() {
            // The problem guarantees current is only called after at least one update.
            // We just fetch the price associated with the largest timestamp.
            return timeToPrice.get(latestTimestamp);
        }
        
        public int maximum() {
            // lastKey() returns the largest key in a TreeMap in O(log N) time.
            return priceFrequency.lastKey();
        }
        
        public int minimum() {
            // firstKey() returns the smallest key in a TreeMap in O(log N) time.
            return priceFrequency.firstKey();
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        StockPrice sp = new StockPrice();
        
        System.out.println("Updating t=1, price=10");
        sp.update(1, 10);
        
        System.out.println("Updating t=2, price=5");
        sp.update(2, 5);
        
        System.out.println("Current() -> " + sp.current() + " (Expected: 5, because t=2 is latest)");
        System.out.println("Maximum() -> " + sp.maximum() + " (Expected: 10)");
        
        System.out.println("\n--- CORRECTION INCOMING ---");
        System.out.println("Updating t=1, price=3 (Old price 10 is wrong)");
        sp.update(1, 3);
        
        System.out.println("Maximum() -> " + sp.maximum() + " (Expected: 5, because 10 was deleted)");
        
        System.out.println("\nUpdating t=4, price=2");
        sp.update(4, 2);
        
        System.out.println("Minimum() -> " + sp.minimum() + " (Expected: 2)");
        System.out.println("Current() -> " + sp.current() + " (Expected: 2, because t=4 is latest)");
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: HashMap + TreeMap (Frequency Map).
 * - Key observation: Standard heaps struggle with arbitrary O(1) removals. TreeMaps excel at 
 *   keeping data sorted while allowing O(log N) updates and removals.
 * - Worth memorizing: `TreeMap.firstKey()` and `TreeMap.lastKey()` for O(log N) min/max retrieval.
 * - Most common trap: Using a `TreeSet` of just prices, which collapses duplicates and deletes valid 
 *   data when a different timestamp corrects a shared price. Always track frequency.
 * - One-line mental trigger: "Need Min/Max AND arbitrary removals? HashMap + Frequency TreeMap."
 * =========================================================================================
 */










































import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

class StockPrice {

    /*
     * Maps each timestamp to its CURRENT valid price.
     *
     * This is the source of truth.
     *
     * If a timestamp gets corrected, its old price is replaced.
     *
     * HashMap operations:
     * get()    -> O(1) average
     * put()    -> O(1) average
     * contains -> O(1) average
     */
    private final Map<Integer, Integer> timestampToPrice;

    /*
     * Maintains:
     *
     * price -> number of timestamps currently having that price.
     *
     * TreeMap keeps prices sorted in ascending order.
     *
     * Example:
     * {
     *     5  -> 2,
     *     10 -> 1,
     *     20 -> 3
     * }
     *
     * firstKey() -> minimum price
     * lastKey()  -> maximum price
     *
     * TreeMap operations:
     * get()    -> O(log n)
     * put()    -> O(log n)
     * remove() -> O(log n)
     */
    private final TreeMap<Integer, Integer> priceFrequency;

    /*
     * Stores the largest timestamp encountered so far.
     *
     * We do not need a TreeMap for timestamps because:
     * - We only need the largest timestamp.
     * - A simple variable is sufficient.
     */
    private int latestTimestamp;

    public StockPrice() {

        timestampToPrice = new HashMap<>();
        priceFrequency = new TreeMap<>();

        latestTimestamp = 0;
    }

    public void update(int timestamp, int price) {

        /*
         * STEP 1: Check whether this timestamp already exists.
         *
         * If it exists, we must remove its previous price
         * from the frequency map.
         *
         * Why?
         *
         * Because the old price is no longer valid.
         * Keeping it would produce incorrect minimum/maximum values.
         */
        if (timestampToPrice.containsKey(timestamp)) {

            int oldPrice = timestampToPrice.get(timestamp);

            /*
             * Decrease the frequency of the old price.
             *
             * Example:
             * priceFrequency = {10=2, 20=1}
             *
             * If one timestamp changes from 10 to 30:
             * {10=1, 20=1}
             *
             * We must not blindly remove price 10 because
             * another timestamp might still have price 10.
             */
            int frequency = priceFrequency.get(oldPrice);

            if (frequency == 1) {

                /*
                 * No timestamp currently has this price.
                 *
                 * Removing the key also removes it from the
                 * ordered set maintained by TreeMap.
                 */
                priceFrequency.remove(oldPrice);

            } else {

                /*
                 * Other timestamps still have this price.
                 * Only decrement its frequency.
                 */
                priceFrequency.put(oldPrice, frequency - 1);
            }
        }

        /*
         * STEP 2: Store the new/corrected price.
         */
        timestampToPrice.put(timestamp, price);

        /*
         * STEP 3: Increase the frequency of the new price.
         *
         * If this price already exists, increment its count.
         * Otherwise, create a new entry with frequency 1.
         */
        priceFrequency.merge(price, 1, Integer::sum);

        /*
         * STEP 4: Update the latest timestamp.
         *
         * IMPORTANT:
         *
         * Do not simply assign:
         *
         * latestTimestamp = timestamp;
         *
         * Updates can arrive out of order.
         *
         * Example:
         * update(10, 100)
         * update(5, 50)
         *
         * The latest timestamp must remain 10.
         */
        latestTimestamp = Math.max(latestTimestamp, timestamp);
    }

    public int current() {

        /*
         * The latest stock price is the price associated
         * with the maximum timestamp.
         *
         * latestTimestamp is maintained during updates,
         * so this operation takes O(1) average time.
         */
        return timestampToPrice.get(latestTimestamp);
    }

    public int maximum() {

        /*
         * TreeMap stores keys in ascending order.
         *
         * The last key is the maximum currently valid price.
         *
         * O(log n) because TreeMap.firstKey()/lastKey()
         * have logarithmic navigation cost in Java's
         * NavigableMap interface.
         */
        return priceFrequency.lastKey();
    }

    public int minimum() {

        /*
         * The first key is the minimum currently valid price.
         */
        return priceFrequency.firstKey();
    }
}

/*
 * COMPLEXITY ANALYSIS
 *
 * Let n = number of distinct timestamps currently recorded.
 *
 * update(timestamp, price):
 *   HashMap containsKey()       -> O(1) average
 *   HashMap get()               -> O(1) average
 *   TreeMap get()/put()/remove()-> O(log n)
 *   HashMap put()               -> O(1) average
 *
 *   Overall: O(log n)
 *
 * current():
 *   One HashMap lookup.
 *   Overall: O(1) average.
 *
 * maximum():
 *   TreeMap.lastKey().
 *   Overall: O(log n).
 *
 * minimum():
 *   TreeMap.firstKey().
 *   Overall: O(log n).
 *
 * SPACE:
 *   HashMap stores at most n timestamp-price entries.
 *   TreeMap stores at most n distinct price-frequency entries.
 *
 *   Overall: O(n).
 *
 * This is an eager deletion approach:
 * whenever a timestamp is corrected, its old price
 * is immediately removed from the price frequency map.
 */










































import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class StockPrice {

    /*
     * Immutable heap entry.
     *
     * A record is useful because each entry is just
     * an immutable pair of timestamp and price.
     *
     * We need timestamp to determine whether an entry
     * is stale after a correction.
     */
    private record PriceEntry(int timestamp, int price) {}

    /*
     * Source of truth:
     *
     * timestamp -> current valid price.
     */
    private final Map<Integer, Integer> timestampToPrice;

    /*
     * Max Heap:
     *
     * Highest price appears at the root.
     *
     * If prices are equal, timestamp is used as a
     * deterministic tie-breaker.
     */
    private final PriorityQueue<PriceEntry> maxHeap;

    /*
     * Min Heap:
     *
     * Lowest price appears at the root.
     */
    private final PriorityQueue<PriceEntry> minHeap;

    /*
     * Maximum timestamp encountered so far.
     */
    private int latestTimestamp;

    public StockPrice() {

        timestampToPrice = new HashMap<>();

        /*
         * Reverse price ordering creates a Max Heap.
         *
         * Comparator.comparingInt(...).reversed()
         * avoids integer subtraction overflow.
         */
        maxHeap = new PriorityQueue<>(
            java.util.Comparator
                .comparingInt(PriceEntry::price)
                .reversed()
                .thenComparingInt(PriceEntry::timestamp)
        );

        /*
         * Natural ascending price ordering creates a Min Heap.
         */
        minHeap = new PriorityQueue<>(
            java.util.Comparator
                .comparingInt(PriceEntry::price)
                .thenComparingInt(PriceEntry::timestamp)
        );

        latestTimestamp = 0;
    }

    public void update(int timestamp, int price) {

        /*
         * Update the source of truth.
         *
         * We do not remove old entries from either heap.
         * They will be cleaned up when they reach the root.
         */
        timestampToPrice.put(timestamp, price);

        /*
         * Insert the new entry into both heaps.
         *
         * Even if this timestamp already existed,
         * we simply insert its corrected version.
         */
        PriceEntry entry = new PriceEntry(timestamp, price);

        maxHeap.offer(entry);
        minHeap.offer(entry);

        /*
         * Maintain the largest timestamp.
         */
        latestTimestamp = Math.max(latestTimestamp, timestamp);
    }

    public int current() {

        /*
         * The HashMap contains the latest valid price
         * for every timestamp.
         *
         * The largest timestamp is tracked separately.
         */
        return timestampToPrice.get(latestTimestamp);
    }

    public int maximum() {

        /*
         * Remove stale entries from the Max Heap.
         *
         * An entry is stale if its price no longer matches
         * the current price recorded for its timestamp.
         *
         * We only need to clean the root because
         * non-root entries cannot affect the current answer.
         */
        while (!maxHeap.isEmpty()) {

            PriceEntry entry = maxHeap.peek();

            int currentPrice =
                timestampToPrice.get(entry.timestamp());

            /*
             * This entry is valid.
             *
             * Return its price without removing it.
             */
            if (currentPrice == entry.price()) {
                return entry.price();
            }

            /*
             * This entry represents an outdated price.
             * Remove it and inspect the next highest entry.
             */
            maxHeap.poll();
        }

        /*
         * This situation cannot happen under the problem's
         * constraints because maximum() is called only
         * after at least one update.
         */
        throw new IllegalStateException("No stock prices available");
    }

    public int minimum() {

        /*
         * Same lazy deletion technique as maximum(),
         * but applied to the Min Heap.
         */
        while (!minHeap.isEmpty()) {

            PriceEntry entry = minHeap.peek();

            int currentPrice =
                timestampToPrice.get(entry.timestamp());

            /*
             * Root is still valid.
             */
            if (currentPrice == entry.price()) {
                return entry.price();
            }

            /*
             * Root is stale.
             * Remove it and inspect the next smallest price.
             */
            minHeap.poll();
        }

        throw new IllegalStateException("No stock prices available");
    }
}

/*
 * COMPLEXITY ANALYSIS
 *
 * Let n = number of update operations.
 *
 * update():
 *   HashMap.put()          -> O(1) average
 *   MaxHeap.offer()        -> O(log n)
 *   MinHeap.offer()        -> O(log n)
 *
 *   Overall: O(log n)
 *
 * current():
 *   HashMap.get()          -> O(1) average
 *
 *   Overall: O(1) average
 *
 * maximum():
 *   Each stale entry is removed using poll().
 *   One poll() costs O(log n).
 *
 *   A single call can remove many stale entries.
 *   Therefore, worst-case time for one call is O(n log n).
 *
 *   However, each inserted heap entry is removed at most once.
 *   Across all operations, the total cleanup work is O(n log n).
 *
 *   Amortized: O(log n) per operation.
 *
 * minimum():
 *   Identical amortized analysis.
 *
 * SPACE:
 *   HashMap: O(n) entries.
 *   Max Heap: O(n) entries.
 *   Min Heap: O(n) entries.
 *
 *   Overall: O(n).
 *
 * IMPORTANT:
 *   This implementation can contain stale entries.
 *   Therefore, its heap sizes can grow with the total
 *   number of updates, not merely distinct timestamps.
 *
 *   At most 10^5 updates are allowed, so this is acceptable.
 */

