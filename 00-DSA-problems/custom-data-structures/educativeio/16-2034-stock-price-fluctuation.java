/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * You are given a stream of records for a stock. Each record has a timestamp 
 * and a price. Records arrive out of order, and subsequent records for the same 
 * timestamp correct the previous price.
 * 
 * Design an algorithm to:
 * - update(timestamp, price): Record or correct a price.
 * - current(): Return the price at the latest timestamp.
 * - maximum(): Return the maximum price currently on record.
 * - minimum(): Return the minimum price currently on record.
 * 
 * Constraints: timestamp, price <= 10^9. Up to 10^5 calls.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. What is the ratio of `update` calls to `current`/`maximum`/`minimum` queries?
 *    (If it's extremely update-heavy with rare queries, lazy evaluation or batching might be better than strictly maintaining sorted structures on every call).
 * 2. Are we memory constrained? 
 *    (Lazy deletion approaches use more memory than strictly bound TreeMaps; helps decide between Heap and BST).
 * 3. Does a correction at the "latest" timestamp also update the `current()` price?
 *    (Yes, `current()` strictly reflects the most up-to-date price of the absolute maximum timestamp seen).
 * 4. Can prices be zero or negative?
 *    (Constraints say >= 1, but confirming prevents bugs with default 0-initializations).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: We need O(1) access to the latest time, O(1) access to a specific 
 *   timestamp's price (to correct it), and fast access to the dynamic minimum and maximum 
 *   prices. The binding constraint is that correcting an old price forces us to remove 
 *   a value from our sorted collection of minimums and maximums.
 * 
 * --- Approach 1: HashMap + Linear Scan (Brute Force) ---
 * 1. Intuition: Just store the truth. A HashMap maps `timestamp -> price`.
 * 2. Try: `update` puts the value in the map and tracks `maxTimestamp`. `current` just 
 *    looks up `map.get(maxTimestamp)`. `maximum` and `minimum` iterate over `map.values()`.
 * 3. Why it works: Perfectly accurately tracks the state.
 * 4. Why it's too costly: `maximum` and `minimum` take O(N) time. With 10^5 queries, 
 *    this scales to O(N^2) and triggers a Time Limit Exceeded.
 * 5. Bottleneck: Re-scanning the entire dataset for every min/max query.
 * 
 * Time Complexity: update: O(1). current: O(1). maximum/minimum: O(N).
 * Space Complexity: O(N).
 * 
 * --- Approach 2: HashMap + Two Priority Queues (Lazy Deletion) ---
 * 1. Intuition: Heaps are built to find min and max fast.
 * 2. Try: Use a `HashMap` for truth. Use a Max-Heap and Min-Heap storing `[price, timestamp]`.
 * 3. The trick: When updating an old price, DO NOT search the heap to delete the old price 
 *    (which takes O(N)). Just push the NEW price to the heap. 
 * 4. Why it works: When querying `maximum()`, we look at the top of the heap. Is the 
 *    price at the top equal to the *true* price for that timestamp in the HashMap? If yes, 
 *    return it. If no, it's stale data—pop it and check the next one.
 * 5. Cost check: Amortized O(log N) for updates and queries. However, space can grow to 
 *    O(U) where U is the total number of updates, because we never eagerly delete.
 * 
 * --- Approach 3: Two Maps (HashMap + TreeMap) [Optimal] ---
 * 1. Intuition: We need a structure that supports fast arbitrary deletion (which Heaps lack) 
 *    and fast min/max queries. A Balanced Binary Search Tree (TreeMap) does exactly this.
 * 2. Try: `HashMap<Integer, Integer> timeToPrice` maintains the truth. 
 *    `TreeMap<Integer, Integer> priceFrequency` tracks how many times each price currently exists.
 * 3. Why it works: 
 *    - `update(t, p)`: Look up old price in HashMap. Decrease its frequency in TreeMap 
 *      (remove if 0). Add new price to TreeMap.
 *    - `maximum()`: `TreeMap.lastKey()`.
 *    - `minimum()`: `TreeMap.firstKey()`.
 * 4. Which to write in a 45-min interview? 
 *    Approach 3. It rigorously bounds space to O(N) distinct timestamps and uses standard, 
 *    predictable library data structures without relying on amortized lazy-deletion arguments.
 * 
 * Time Complexity: 
 * - update: O(log N) — because adding/removing from a TreeMap takes logarithmic time.
 * - current: O(1) — HashMap lookup.
 * - maximum/minimum: O(log N) — fetching extremes from the TreeMap.
 * Space Complexity: O(N) — where N is the number of UNIQUE timestamps.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - Correcting the maximum price downwards: The old maximum must be completely removed 
 *   from the TreeMap so the new, lower maximum can surface.
 * - Correcting the latest timestamp's price: The `maxTimestamp` doesn't change, but 
 *   the value it points to does, which automatically fixes `current()`.
 * - Multiple timestamps with the exact same price: Using a standard `TreeSet` would 
 *   collapse duplicates, meaning correcting one timestamp would delete the price for 
 *   the others. We MUST use a `TreeMap` mapping `price -> frequency`.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Separation of Concerns." 
 *   Use a HashMap to manage the dimension of *Time* (O(1) truth tracking). 
 *   Use a TreeMap to manage the dimension of *Value* (O(log N) min/max tracking).
 * 
 * ASCII DRY RUN (Approach 3)
 * Init: empty
 * 1. update(1, 10)
 *    timeToPrice: {1: 10}
 *    priceFreq: {10: 1}
 *    maxTime: 1
 * 
 * 2. update(2, 5)
 *    timeToPrice: {1: 10, 2: 5}
 *    priceFreq: {5: 1, 10: 1}
 *    maxTime: 2
 *    current() -> timeToPrice[2] = 5
 *    maximum() -> priceFreq.lastKey() = 10
 * 
 * 3. update(1, 3)  // Correction!
 *    Old price for 1 was 10. freq[10] becomes 0 (removed).
 *    timeToPrice: {1: 3, 2: 5}
 *    priceFreq: {3: 1, 5: 1}
 *    maximum() -> priceFreq.lastKey() = 5
 * 
 * Pitfalls:
 * - Over-complicating `maxTimestamp`: You don't need a sorted structure for time. Since 
 *   time only moves forward or updates the past, `maxTimestamp = Math.max(maxTimestamp, t)` 
 *   is strictly sufficient.
 * 
 * Pattern Recognition: "Data stream with corrections + Extremes" -> HashMap + TreeMap (or Lazy Heap).
 * 
 * Interview Script snippet:
 * "Since we need to correct old values and instantly know the new minimums and maximums, 
 * a standard PriorityQueue is tricky because arbitrary deletion takes O(N). Instead, I'll 
 * pair a HashMap with a TreeMap. The HashMap tracks the true price for any timestamp. The 
 * TreeMap tracks the frequencies of all active prices. When an update arrives, I O(1) look up 
 * the old price, O(log N) decrement it in the TreeMap, and O(log N) insert the new one."
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if `current()` is never called, and `maximum()`/`minimum()` are called rarely, 
 *     but `update()` is called millions of times per second?
 * A1: The TreeMap's O(log N) update is too slow. I'd switch to Approach 2 (Lazy Deletion Heaps) 
 *     or even just keep an unsorted list of corrections and only process/sort them when a 
 *     query actually arrives (Deferred processing).
 * 
 * Q2: How would you handle this if timestamps could be deleted entirely?
 * A2: Our TreeMap logic handles deletion naturally (decrement frequency). For the `current()` 
 *     method, tracking `maxTimestamp` with just `Math.max` would break if the max timestamp 
 *     is deleted. We would need a second `TreeMap` or a `Max-Heap` just to track active timestamps.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StockPriceStudy {

    static class StockPrice {
        // Tracks the absolute highest timestamp we've seen so far.
        // It never goes backwards, even if an old timestamp is corrected.
        private int latestTimestamp;
        
        // Maps timestamp -> actual price
        // Provides O(1) truth lookups for corrections.
        private Map<Integer, Integer> timeToPrice;
        
        // Maps price -> frequency of that price across all timestamps
        // Automatically sorts prices, giving O(log N) access to min/max.
        private TreeMap<Integer, Integer> priceFrequency;

        /**
         * Initializes the object with no price records.
         */
        public StockPrice() {
            this.latestTimestamp = 0;
            this.timeToPrice = new HashMap<>();
            this.priceFrequency = new TreeMap<>();
        }

        /**
         * Updates the price of the stock at the given timestamp.
         * Time Complexity: O(log N) due to TreeMap operations.
         */
        public void update(int timestamp, int price) {
            // Update the latest timestamp if this record is newer
            latestTimestamp = Math.max(latestTimestamp, timestamp);
            
            // If this timestamp already has a record, we must "undo" the old price 
            // in our frequency map before adding the new one.
            if (timeToPrice.containsKey(timestamp)) {
                int oldPrice = timeToPrice.get(timestamp);
                
                // Decrement the old price's frequency
                int oldFreq = priceFrequency.get(oldPrice);
                if (oldFreq == 1) {
                    // Completely remove it from the tree to maintain accurate min/max ends
                    priceFrequency.remove(oldPrice);
                } else {
                    priceFrequency.put(oldPrice, oldFreq - 1);
                }
            }
            
            // Record the new truth in the HashMap
            timeToPrice.put(timestamp, price);
            
            // Increment the new price's frequency in the TreeMap
            priceFrequency.put(price, priceFrequency.getOrDefault(price, 0) + 1);
        }

        /**
         * Returns the latest price of the stock.
         * Time Complexity: O(1).
         */
        public int current() {
            // The HashMap gives us O(1) access to the truth for the latest time.
            return timeToPrice.get(latestTimestamp);
        }

        /**
         * Returns the maximum price of the stock.
         * Time Complexity: O(log N).
         */
        public int maximum() {
            // lastKey() on a TreeMap returns the highest key in O(log N) time.
            return priceFrequency.lastKey();
        }

        /**
         * Returns the minimum price of the stock.
         * Time Complexity: O(log N).
         */
        public int minimum() {
            // firstKey() on a TreeMap returns the lowest key in O(log N) time.
            return priceFrequency.firstKey();
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        StockPrice stockPrice = new StockPrice();
        
        System.out.println("Updating t=1 to 10...");
        stockPrice.update(1, 10);
        
        System.out.println("Updating t=2 to 5...");
        stockPrice.update(2, 5);
        
        // Latest time is 2, price is 5
        System.out.println("Current: " + stockPrice.current()); // Expected: 5
        
        // Prices are {5, 10}
        System.out.println("Maximum: " + stockPrice.maximum()); // Expected: 10
        
        // Correction! Timestamp 1 was wrong, changing 10 to 3.
        System.out.println("Correcting t=1 to 3...");
        stockPrice.update(1, 3);
        
        // Prices are {3, 5}
        System.out.println("Maximum after correction: " + stockPrice.maximum()); // Expected: 5
        
        System.out.println("Updating t=4 to 2...");
        stockPrice.update(4, 2);
        
        // Prices are {2, 3, 5}
        System.out.println("Minimum: " + stockPrice.minimum()); // Expected: 2
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: HashMap (Truth mapping) + TreeMap (Frequency tracking).
 * Key Observation: Correcting a stream requires removing specific past elements. Since 
 * standard priority queues don't support O(1) or O(log N) arbitrary deletion, a Balanced 
 * BST (TreeMap) tracking frequencies perfectly bridges the gap between tracking extremes 
 * and allowing surgical updates.
 * What to Memorize: The 3-step update: 1) Lookup old value in Map, 2) Decrement/remove 
 * old value from TreeMap, 3) Put new value in Map and increment in TreeMap.
 * Most Common Trap: Using a `TreeSet` instead of a `TreeMap`. A `TreeSet` treats identical 
 * prices as duplicates and collapses them, meaning correcting one timestamp would delete 
 * the price record for ALL timestamps that shared that price.
 * Mental Trigger: "Stream with corrections + Min/Max" -> "HashMap + TreeMap".
 * ============================================================================
 */

























import java.util.Map;
import java.util.TreeMap;

class StockPriceTreeMap {

    private final Map<Integer, Integer> timestampToPrice = new TreeMap<>();

    // Sorted price -> number of timestamps currently having that price.
    private final TreeMap<Integer, Integer> priceCounts = new TreeMap<>();

    public void update(int timestamp, int price) {
        Integer oldPrice = timestampToPrice.put(timestamp, price);

        // If this timestamp already existed, remove its old price
        // from the frequency map before adding the corrected price.
        if (oldPrice != null) {
            removePrice(oldPrice);
        }

        // Add one occurrence of the new current price.
        priceCounts.merge(price, 1, Integer::sum);
    }

    private void removePrice(int price) {
        int count = priceCounts.get(price);

        if (count == 1) {
            // No timestamp uses this price anymore.
            priceCounts.remove(price);
        } else {
            // Other timestamps still use this price.
            priceCounts.put(price, count - 1);
        }
    }

    public int current() {
        // TreeMap.lastKey() gives the greatest timestamp.
        // The outer map's lookup then gives its current price.
        // This version uses a TreeMap for timestamps as well.
        int latestTimestamp = ((TreeMap<Integer, Integer>) timestampToPrice)
                .lastKey();

        return timestampToPrice.get(latestTimestamp);
    }

    public int maximum() {
        // Sorted prices mean the last key is the maximum current price.
        return priceCounts.lastKey();
    }

    public int minimum() {
        // The first key is the minimum current price.
        return priceCounts.firstKey();
    }
}

/*
COMPLEXITY
Let n = the number of distinct timestamps and p = the number of
distinct current prices, where p <= n.

update:
    O(log n) to update the timestamp TreeMap.
    O(log p) to update the price-frequency TreeMap.
    Total O(log n + log p), which is O(log n).

current:
    O(log n) to locate the largest timestamp in a TreeMap,
    followed by O(log n) to look up its price.
    Total O(log n).

maximum / minimum:
    O(log p) worst-case to access the first or last key.
    Total O(log n).

Space:
    O(n + p) = O(n).

Important:
    This implementation is correct, but the field must be declared
    as TreeMap, not Map, to call lastKey() without a cast. The cast
    above works because the object is a TreeMap, but a cleaner
    declaration is preferable in production code.
*/







































import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class StockPrice {

    /*
     * A heap entry must contain BOTH timestamp and price.
     *
     * Why not store just the price?
     *
     * Suppose timestamp 1 changes from 100 to 10. If a heap contains
     * only 100, we cannot identify which timestamp produced it.
     *
     * With (timestamp, price), we can check whether that exact record
     * still agrees with the authoritative map.
     *
     * A record is valid precisely when:
     *     timestampToPrice.get(record.timestamp()) == record.price()
     *
     * Java 16+ records are useful here because this is an immutable
     * data carrier. Java 24 supports this syntax.
     */
    private record PriceRecord(int timestamp, int price) {}

    /*
     * SOURCE OF TRUTH:
     *
     * This map contains exactly one current price per timestamp.
     * All corrections overwrite this mapping.
     *
     * The heaps may contain stale records, but this map never does.
     */
    private final Map<Integer, Integer> timestampToPrice = new HashMap<>();

    /*
     * MIN-HEAP:
     *
     * PriorityQueue is a min-heap by default, but we explicitly specify
     * the comparator to make the intended ordering clear.
     *
     * The lowest price is at the root.
     *
     * We compare prices, not timestamps, because minimum() asks for
     * the minimum PRICE.
     */
    private final PriorityQueue<PriceRecord> minHeap =
            new PriorityQueue<>(Comparator.comparingInt(PriceRecord::price));

    /*
     * MAX-HEAP:
     *
     * Reverse the price comparator so the largest price is at the root.
     *
     * Use a comparator rather than subtracting prices. Subtraction
     * inside comparators can overflow for unrestricted integer values.
     */
    private final PriorityQueue<PriceRecord> maxHeap =
            new PriorityQueue<>(
                    Comparator.comparingInt(PriceRecord::price).reversed()
            );

    /*
     * Tracks the greatest timestamp ever received.
     *
     * We do not derive this from heap ordering because the heaps are
     * ordered by PRICE, not by TIMESTAMP.
     *
     * Constraints guarantee timestamp >= 1 and queries occur only
     * after at least one update, so zero is a safe initial value.
     */
    private int latestTimestamp = 0;

    public void update(int timestamp, int price) {

        /*
         * STEP 1: Correct the authoritative map.
         *
         * If timestamp already exists, put() replaces its old price.
         * If it does not exist, put() inserts a new timestamp.
         *
         * We intentionally do not remove the old record from either
         * heap. Arbitrary heap removal is O(n) in Java PriorityQueue.
         */
        timestampToPrice.put(timestamp, price);

        /*
         * STEP 2: Add the new record to BOTH heaps.
         *
         * Each heap may now contain obsolete records from earlier
         * corrections. This is intentional: lazy deletion handles them.
         *
         * Inserting one element into a heap costs O(log n) for a heap
         * of size n.
         */
        PriceRecord record = new PriceRecord(timestamp, price);

        minHeap.offer(record);
        maxHeap.offer(record);

        /*
         * STEP 3: Update the latest timestamp.
         *
         * An update can arrive out of order. For example, after
         * update(10, 50), update(3, 100) must NOT change current().
         *
         * Therefore, keep the maximum timestamp seen so far.
         */
        latestTimestamp = Math.max(latestTimestamp, timestamp);
    }

    public int current() {

        /*
         * The latest timestamp determines current(), not the most
         * recently received update.
         *
         * HashMap lookup is expected O(1).
         *
         * Corrections are reflected automatically because the map
         * always contains the current price for each timestamp.
         */
        return timestampToPrice.get(latestTimestamp);
    }

    public int maximum() {

        /*
         * Before returning the root, discard obsolete records that
         * happen to be at the top of the max-heap.
         *
         * We do not need to clean the entire heap. Only its root can
         * affect the answer to this query.
         */
        removeStaleRecords(maxHeap);

        /*
         * After cleanup, the root is the greatest VALID current price.
         * Constraints guarantee that at least one update has occurred.
         */
        return maxHeap.peek().price();
    }

    public int minimum() {

        /*
         * The same logic applies to the min-heap: stale records that
         * could incorrectly appear to be the minimum must be removed.
         */
        removeStaleRecords(minHeap);

        /*
         * The root is now the smallest valid current price.
         */
        return minHeap.peek().price();
    }

    private void removeStaleRecords(PriorityQueue<PriceRecord> heap) {

        /*
         * Repeatedly inspect the top until it represents the current
         * price for its timestamp.
         *
         * A heap does NOT need to contain only valid records. It only
         * needs to have a valid root whenever we return an answer.
         */
        while (!heap.isEmpty()) {

            PriceRecord record = heap.peek();

            /*
             * Look up the CURRENT price for this record's timestamp.
             *
             * If the map contains a different price, the heap record
             * is obsolete and must not be used as an answer.
             *
             * The comparison unboxes Integer to int. The record is
             * guaranteed to refer to a timestamp that was inserted,
             * so the map lookup is non-null.
             */
            int currentPrice = timestampToPrice.get(record.timestamp());

            if (currentPrice == record.price()) {

                /*
                 * This record matches the authoritative map.
                 *
                 * It is valid, and because it is at the root of the
                 * appropriate heap, it is the correct minimum/maximum.
                 */
                break;
            }

            /*
             * This record is stale.
             *
             * Remove it from the root and inspect the next candidate.
             * poll() costs O(log n).
             *
             * Crucially, we remove only from the heap passed to this
             * method. The other heap may still contain its own copy
             * of this stale record; it will discard it when necessary.
             */
            heap.poll();
        }
    }
}

/*
 * ================================================================
 * COMPLEXITY ANALYSIS
 * ================================================================
 *
 * Let:
 *   u = total number of update calls so far.
 *   n = number of distinct timestamps currently stored.
 *
 * The heaps can contain up to O(u) records because corrections append
 * new entries rather than removing old ones immediately.
 *
 * update(timestamp, price):
 *   - HashMap.put(): expected O(1).
 *   - latestTimestamp update: O(1).
 *   - One insertion into each heap: O(log u) per insertion.
 *   Total: O(log u) time.
 *
 * current():
 *   - One HashMap lookup.
 *   Total: expected O(1).
 *
 * minimum() / maximum():
 *   - Each stale root removed costs O(log u).
 *   - A single query may remove many stale records, so its worst-case
 *     time can be O(u log u).
 *   - However, every inserted record is removed from each heap at most
 *     once. Across the entire sequence of operations, the total work
 *     of stale-record cleanup is O(u log u).
 *   - Thus, cleanup is amortized O(log u) per operation over a sequence
 *     of O(u) operations, rather than O(u log u) on every query.
 *
 * Space:
 *   - Map: O(n).
 *   - Both heaps: O(u) in the worst case.
 *   Total: O(n + u) = O(u).
 *
 * IMPORTANT DISTINCTION:
 *   The heap approach has good amortized performance, but it is not
 *   guaranteed to give O(log n) space. Repeated corrections can leave
 *   stale entries in the heaps until they reach the top.
 *
 * The TreeMap solution avoids stale heap entries and uses O(n) space.
 */





