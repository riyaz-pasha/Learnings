/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Implement a TimeStamp class to store and retrieve values by key and timestamp.
 * - Init(): Initializes the data structures.
 * - Set Value(key, value, timestamp): Stores the key and value at the given timestamp.
 * - Get Value(key, timestamp): Returns the value set for this key at the exact 
 *   timestamp, or the most recent timestamp BEFORE the query's timestamp. If no 
 *   such timestamp exists, return an empty string "".
 * 
 * Constraints:
 * - key/value lengths <= 20, lowercase English and digits.
 * - timestamp <= 10^3, max 10^3 calls.
 * - CRITICAL: All timestamps in Set Value are strictly increasing.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Are the timestamps globally strictly increasing, or just strictly increasing per key? 
 *    (Globally increasing implies per-key is also increasing, which guarantees append-only sorting).
 * 2. If a query requests a timestamp exactly equal to an existing one, do we return it? 
 *    (Yes, the prompt says "at the specified timestamp" OR "most recent before").
 * 3. Can the same key be assigned a different value at the exact same timestamp? 
 *    (Constraints say timestamps are *strictly* increasing, so no duplicate timestamps will occur).
 * 4. Is the expected read/write ratio heavily skewed? 
 *    (If it's extremely read-heavy, optimizing the Get method to O(1) via dense arrays might be needed, but O(log N) is usually fine).
 * 5. Should we worry about garbage collection for very old timestamps? 
 *    (No TTL or eviction mentioned, so we assume we keep all history).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement: Map a single key to a timeline of values, and efficiently query 
 *   that timeline for the "floor" value (largest timestamp <= target). 
 *   The binding constraint is the search time on the timeline.
 * 
 * --- Approach 1: Brute Force (HashMap + List + Linear Search) ---
 * 1. Intuition: Store all versions of a key in a list. When asked for a value, scan the list.
 * 2. Try: `HashMap<String, ArrayList<Data>>`. 
 *    `Set`: append to the list. 
 *    `Get`: iterate backwards from the end of the list until `item.timestamp <= target`.
 * 3. Why it works: Because timestamps are appended in strictly increasing order, scanning 
 *    backwards guarantees the first match is the closest valid timestamp.
 * 4. Why it's too costly: Time! If a key is updated 1,000 times, a `Get` query for an early 
 *    timestamp requires scanning almost the entire list.
 * 5. Bottleneck: Linear scanning. We are ignoring the fact that the list is sorted.
 * 
 * Time Complexity: Set: O(1) — append to list. Get: O(N) — linear scan backwards.
 * Space Complexity: O(N) — where N is total number of Set operations.
 * 
 * --- Approach 2: TreeMap (Sorted Map per Key) ---
 * 1. Intuition: Java has a built-in data structure that automatically sorts keys and has 
 *    a `floorEntry(target)` method specifically for this!
 * 2. Try: `HashMap<String, TreeMap<Integer, String>>`.
 *    `Set`: `map.get(key).put(timestamp, value)`.
 *    `Get`: `map.get(key).floorEntry(timestamp).getValue()`.
 * 3. Why it works: TreeMap is a Red-Black Tree. `floorEntry` does exactly what the problem asks in O(log N).
 * 4. Why it's suboptimal: A TreeMap takes O(log N) to INSERT. But the constraints explicitly 
 *    tell us timestamps arrive in strictly increasing order! We are paying an O(log N) tree-balancing 
 *    tax for data that is already perfectly sorted.
 * 5. Bottleneck: Unnecessary overhead of tree insertion and node allocation when simple appending would do.
 * 
 * Time Complexity: Set: O(log N) — tree insertion. Get: O(log N) — tree search.
 * Space Complexity: O(N) — plus heavy constant factors for tree nodes (left/right/color pointers).
 * 
 * --- Approach 3: HashMap + List + Binary Search (Optimal) ---
 * 1. Intuition: Combine the O(1) append of Approach 1 with the O(log N) search of Approach 2.
 * 2. Try: `HashMap<String, ArrayList<Data>>`. 
 *    `Set`: Simply append to the ArrayList. (Because inputs are strictly increasing, it's naturally sorted!).
 *    `Get`: Write a custom Binary Search to find the largest timestamp <= target.
 * 3. Why it works: We exploit the "strictly increasing" constraint to get O(1) writes, 
 *    and we use standard Binary Search to get O(log N) reads.
 * 4. Which to write in a 45-min interview?
 *    Approach 3. It shows you paid attention to the "strictly increasing" constraint. Defaulting 
 *    to a TreeMap shows a reliance on black-box APIs rather than algorithmic analysis.
 * 
 * Time Complexity: Set: O(1) amortized — appending to ArrayList. Get: O(log K) — where K is the number of versions for that specific key.
 * Space Complexity: O(N) — where N is total Set operations, stored contiguously in arrays.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - Target timestamp is SMALLER than the very first recorded timestamp: 
 *   Binary search will fail to find a valid floor. We must explicitly return `""`.
 * - Target timestamp EXACTLY MATCHES a recorded timestamp: 
 *   Binary search must include `=` in its condition (`mid <= target`) and return it.
 * - Target timestamp is MUCH LARGER than the latest timestamp:
 *   Binary search naturally converges on the very last element.
 * - Key does not exist: 
 *   Check HashMap first, return `""` immediately to avoid NullPointerException.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & PITFALLS
 * ============================================================================
 * > Key Insight: "Append-only + Monotonicity = Free Sorting." 
 *   Never sort or use a Tree if the data naturally arrives in sorted order. An ArrayList 
 *   with Binary Search is vastly more memory-efficient and cache-friendly than a TreeMap.
 * 
 * ASCII DRY RUN (Binary Search for Floor)
 * List for "foo": [(time:1, "a"), (time:3, "b"), (time:5, "c")]
 * Query: Get("foo", 4)
 * 
 * L = 0, R = 2
 * Iteration 1:
 *   mid = (0 + 2) / 2 = 1. Timestamp at mid is 3.
 *   Is 3 <= 4? Yes! 
 *   Record possible answer: ans = "b". 
 *   To see if there's a closer one, search right: L = mid + 1 = 2.
 * Iteration 2:
 *   L = 2, R = 2
 *   mid = (2 + 2) / 2 = 2. Timestamp at mid is 5.
 *   Is 5 <= 4? No! 
 *   Search left: R = mid - 1 = 1.
 * 
 * Loop terminates (L > R). 
 * Return recorded answer: "b".
 * 
 * Pitfalls:
 * - Using `Arrays.binarySearch()` or `Collections.binarySearch()` in Java. 
 *   These return `-(insertion_point) - 1` if exact match isn't found. You *can* use it, 
 *   but translating that negative index back to the floor index is highly error-prone under 
 *   interview pressure (e.g. `int idx = -res - 2; if (idx < 0) return "";`). 
 *   Writing a custom binary search is safer and clearer.
 * 
 * Pattern Recognition: "Find closest value without going over" -> Binary Search (Upper Bound / Floor).
 * 
 * Interview Script snippet:
 * "Since the problem guarantees timestamps strictly increase, an ArrayList for each key will 
 * naturally be sorted. I don't need a TreeMap, which takes O(log N) to insert. I can append 
 * in O(1) time. For retrieval, I'll use binary search on that key's list to find the largest 
 * timestamp less than or equal to the target, giving us O(log N) read time."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if the timestamps for `Set` were NOT strictly increasing (arrived out of order)?
 * A1: The ArrayList would no longer be sorted. We'd have to switch to Approach 2 (TreeMap) 
 *     or use an ArrayList but manually insert elements in sorted order (which degrades Set to O(N) 
 *     due to array shifting).
 * 
 * Q2: What if we want to query a range of times? e.g. "Get all values for key between t1 and t2".
 * A2: Our binary search approach is perfect for this. We binary search for the ceiling of t1, 
 *     and iterate forward until we hit a timestamp > t2. This would take O(log N + K) where K 
 *     is the number of elements in the range.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeStampStudy {

    /**
     * A simple data class to bind a timestamp and its corresponding value.
     */
    private static class Data {
        int timestamp;
        String value;

        Data(int timestamp, String value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }

    // Core data structure: Maps a string key to its historical timeline (List of Data).
    // Example: "foo" -> [(1, "bar"), (3, "baz")]
    private Map<String, List<Data>> store;

    /**
     * Initializes the values dictionary and timestamp dictionary.
     */
    public TimeStampStudy() {
        store = new HashMap<>();
    }

    /**
     * Stores the key and value at the given timestamp.
     * Time: O(1) amortized
     */
    public void setValue(String key, String value, int timestamp) {
        // computeIfAbsent fetches the list, or creates a new ArrayList if key doesn't exist.
        // Because timestamps strictly increase, we simply append. It remains perfectly sorted.
        store.computeIfAbsent(key, k -> new ArrayList<>()).add(new Data(timestamp, value));
    }

    /**
     * Returns the value set for this key at the specified timestamp.
     * Time: O(log N) where N is the number of versions for this specific key.
     */
    public String getValue(String key, int timestamp) {
        // Edge Case: Key has never been set.
        if (!store.containsKey(key)) {
            return "";
        }

        List<Data> timeline = store.get(key);
        
        // Edge Case: The requested timestamp is BEFORE the very first entry.
        // Since the list is sorted, timeline.get(0) is the absolute minimum timestamp.
        if (timestamp < timeline.get(0).timestamp) {
            return "";
        }

        // Custom Binary Search to find the FLOOR (largest timestamp <= target)
        int left = 0;
        int right = timeline.size() - 1;
        
        // We will store our best valid answer here as we search.
        String closestValue = "";

        while (left <= right) {
            int mid = left + (right - left) / 2;
            Data midData = timeline.get(mid);

            if (midData.timestamp == timestamp) {
                // Exact match found. We can't get any closer than exact.
                return midData.value;
            } else if (midData.timestamp < timestamp) {
                // This is a VALID candidate (it is before the query time).
                // Save it, and search the right half to see if there's a more recent valid one.
                closestValue = midData.value;
                left = mid + 1;
            } else {
                // midData.timestamp > timestamp
                // This is INVALID (it is in the future relative to the query time).
                // Search the left half for an older timestamp.
                right = mid - 1;
            }
        }

        return closestValue;
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        TimeStampStudy ts = new TimeStampStudy();

        // Test 1: Basic functionality
        ts.setValue("foo", "bar", 1);
        System.out.println("Get foo at t=1 (Expect 'bar'): " + ts.getValue("foo", 1)); // bar
        
        // Test 2: Floor querying (Query time > stored time)
        System.out.println("Get foo at t=3 (Expect 'bar'): " + ts.getValue("foo", 3)); // bar
        
        // Test 3: Multiple values on same timeline
        ts.setValue("foo", "bar2", 4);
        System.out.println("Get foo at t=4 (Expect 'bar2'): " + ts.getValue("foo", 4)); // bar2
        System.out.println("Get foo at t=5 (Expect 'bar2'): " + ts.getValue("foo", 5)); // bar2
        
        // Test 4: Floor query falls EXACTLY between two values
        // Stored times for "foo": 1, 4. Target: 3. Should floor down to 1.
        System.out.println("Get foo at t=3 again (Expect 'bar'): " + ts.getValue("foo", 3)); // bar

        // Test 5: Edge Case - Query time is BEFORE the first record
        System.out.println("Get foo at t=0 (Expect ''): '" + ts.getValue("foo", 0) + "'"); // ""

        // Test 6: Edge Case - Key doesn't exist
        System.out.println("Get unknown at t=10 (Expect ''): '" + ts.getValue("unknown", 10) + "'"); // ""
    }
}

/**
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core Pattern: Map + Append-only List + Binary Search (Floor).
 * Key Observation: "Strictly increasing" means we get sorting for free. Don't waste time 
 * and space on a TreeMap when an ArrayList suffices.
 * What to Memorize: The Binary Search Floor template. When `val < target`, save the answer 
 * (`ans = val`), and push `left = mid + 1`. 
 * Most Common Trap: Trying to use `Collections.binarySearch` and failing to properly parse 
 * the negative insertion point into a valid floor index.
 * Mental Trigger: "Time-based history" -> "Append to List + Binary Search".
 * ============================================================================
 */

