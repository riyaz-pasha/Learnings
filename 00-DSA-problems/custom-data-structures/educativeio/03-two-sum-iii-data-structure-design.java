/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Design a data structure that takes in a stream of numbers and can check if 
 * any two numbers add up to a specific value.
 * Implement the TwoSum class:
 * - TwoSum(): Initializes the object.
 * - void add(int number): Adds number to the data structure.
 * - boolean find(int value): Returns true if any two added numbers sum to `value`.
 * Constraints: number in [-10^5, 10^5], value fits in 32-bit int, up to 10^4 calls.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. What is the ratio of `add` calls to `find` calls? 
 *    (Crucial: If it's read-heavy (many `find`s), we optimize `find` to O(1). If mixed, we balance them).
 * 2. Can the target `value` exceed the bounds of a 32-bit integer? 
 *    (The constraint says value fits in 32-bit, but subtracting a negative `number` might overflow during intermediate math).
 * 3. Will we ever need a `remove(int number)` method in the future?
 *    (If yes, caching sums is a bad idea because removing a number invalidates multiple cached sums).
 * 4. Can the two numbers be the same value?
 *    (Yes, if `add(3)` was called twice, `find(6)` should be true. If `add(3)` was called once, false. This means Sets aren't enough; we need counts).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement: Maintain a history of a stream, and efficiently search for a 
 *   matching complement (Target - X) among the stored elements. The binding constraint 
 *   is the continuous arrival of data combined with immediate lookup requests.
 * 
 * --- Approach 1: The Brute Force Log (Append-Only) ---
 * 1. Intuition: Just record everything and figure out the math when asked.
 * 2. Try: Use an `ArrayList<Integer>`. 
 *    `add(val)` -> list.add(val).
 *    `find(target)` -> nested loop over the list to check if list[i] + list[j] == target.
 * 3. Why it works: Perfectly preserves state without extra memory overhead.
 * 4. Why it's too costly: Time! 10^4 calls. If 5,000 are adds and 5,000 are finds, 
 *    each find takes 5000^2 operations. It will Time Out (TLE).
 * 5. Bottleneck: On every `find`, we redundantly pair up every number we've ever seen.
 * 6. Observation: We don't need all pairs. For any number X, we only care if exactly `Target - X` exists.
 * 
 * Time Complexity: `add`: O(1) amortized. `find`: O(N^2) — because we check every possible pair.
 * Space Complexity: O(N) — because we store exact N elements in the list.
 * 
 * --- Approach 2: The Eager Pre-computer (Optimize for Find) ---
 * 1. Intuition: If `find` is too slow, let's do the work during `add`.
 * 2. Try: Maintain an `ArrayList<Integer> nums` and a `HashSet<Integer> allSums`.
 *    `add(val)` -> iterate through `nums`, add `(val + old_num)` to `allSums`, then add `val` to `nums`.
 *    `find(target)` -> return `allSums.contains(target)`.
 * 3. Why it works: Lookups are instantaneous. 
 * 4. Why it's too costly: Space and Write Time. Adding a number requires iterating all past numbers.
 * 5. Bottleneck: Storing every combination. 10,000 numbers produce ~50,000,000 unique sums. Memory blows up.
 * 6. Observation: We need the fast lookup of a Hash structure, but we should hash the *individual numbers*, not the *sums*.
 * 
 * Time Complexity: `add`: O(N) — because we pair the new number with N existing numbers. `find`: O(1) — HashSet lookup.
 * Space Complexity: O(N^2) — because we store all possible pairwise sums.
 * 
 * --- Approach 3: The Frequency Map (Optimal Balance) ---
 * 1. Intuition: Hash the numbers themselves. To `find(target)`, iterate through the distinct 
 *    numbers (keys) and ask the Hash: "Do you contain (Target - Key)?"
 * 2. Try: Use `HashSet<Integer>`. But wait! What if Target is 6, and Key is 3? 
 *    We need to know if we have *two* 3s. A Set can't tell us that.
 * 3. Refinement: Use a `HashMap<Integer, Integer>` to map `Number -> Frequency`.
 * 4. Why it works: We balance the load. Adds are O(1). Finds are O(U) where U is the number of *unique* elements.
 * 5. Which to write in a 45-min interview?
 *    Approach 3. It scales beautifully for mixed read/write workloads and showcases an understanding 
 *    of why a simple HashSet fails the `Target == 2 * Key` edge case.
 * 
 * Time Complexity: `add`: O(1) — HashMap insert. `find`: O(U) — because we iterate through U unique keys in the map.
 * Space Complexity: O(U) — because we store U unique integers and their frequencies (where U <= N).
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - The "Double" Target: Target is 10, we have one 5. Must return false. We need frequency >= 2.
 * - Integer Overflow during complement calculation: If Target is -2^31, and Key is 10^5, 
 *   `Target - Key` goes below Integer.MIN_VALUE, wrapping around to a false positive positive. 
 *   We must cast to `long` before subtracting.
 * - Zero Target: Target is 0, Key is 0. Works exactly like the "Double" target (requires frequency >= 2).
 * - Empty stream: `find` should safely return false.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & PITFALLS
 * ============================================================================
 * > Key Insight: "Algebraic complement lookup." Instead of asking "Does X + Y = Target?", 
 *   reframe it to "I have X. Does the collection contain (Target - X)?" This transforms 
 *   a quadratic search into a linear search over a Hash map.
 * 
 * Diagram: Frequency Map Dry Run
 * Operations: add(3), add(1), add(3), find(6), find(4)
 * 1. add(3) -> map: {3: 1}
 * 2. add(1) -> map: {3: 1, 1: 1}
 * 3. add(3) -> map: {3: 2, 1: 1}
 * 4. find(6) -> 
 *      - key = 3. complement = 6 - 3 = 3.
 *      - complement == key. Is map.get(3) > 1? Yes (2 > 1). Return TRUE.
 * 5. find(4) -> 
 *      - key = 3. complement = 4 - 3 = 1.
 *      - complement != key. Does map have 1? Yes. Return TRUE.
 * 
 * Pitfalls:
 * - Using `HashSet`: Fails when `value = number * 2` and the number was only inserted once.
 * - ConcurrentModificationException: Modifying the map (e.g. `add` from another thread) while 
 *   `find` is iterating over the keys. 
 * 
 * Pattern Recognition: "Stream + Two Sum" -> Frequency Map.
 * Transfers to: Two Sum III, Design Tic-Tac-Toe (O(1) state counting), Subarray Sum Equals K.
 * 
 * Interview Script snippet:
 * "Since it's a data stream, caching all pair sums takes O(N^2) space, which will crash the system. 
 * Instead, I'll store the frequencies of the incoming numbers in a HashMap. When `find` is called, 
 * I iterate through the unique keys, calculate the required complement `Target - Key`, and check 
 * if it exists in the map. I'll also add a specific frequency check for when the complement equals 
 * the key itself. This keeps adds at O(1) and finds at O(U) time, with strictly O(U) space."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if `find` is called 1000x more often than `add`? 
 * A1: The HashMap approach takes O(U) per find. If reads dominate, we SHOULD use Approach 2 
 *     (Pre-computer Set). We eat the O(N) add cost to make `find` strictly O(1), improving overall system throughput.
 * 
 * Q2: How do we handle this in a multi-threaded web server?
 * A2: We'd replace `HashMap` with `ConcurrentHashMap`. However, `find` uses an iterator. 
 *     ConcurrentHashMap iterators are weakly consistent (they won't throw exceptions, but might 
 *     miss concurrent updates). If strict consistency is needed, we'd need Read/Write Locks.
 * 
 * Q3: What if the numbers only ever range from 1 to 100?
 * A3: We'd ditch the HashMap and use a simple `int[] counts = new int[101]`. Lookups become 
 *     incredibly fast due to cache locality and no hashing overhead.
 */

import java.util.HashMap;
import java.util.Map;

public class TwoSumStudy {

    static class TwoSum {
        // Core data structure: Maps a number to the number of times it has appeared.
        // Example: If stream is [3, 2, 3], map = {3 -> 2, 2 -> 1}
        private Map<Integer, Integer> numCounts;

        /** 
         * Constructor: Sets up the TwoSum object with an empty list at the start. 
         */
        public TwoSum() {
            numCounts = new HashMap<>();
        }

        /** 
         * Adds a new number to the data structure.
         * Time: O(1) average.
         * Space: O(1) per add.
         */
        public void add(int number) {
            // merge(key, 1, Integer::sum) is idiomatic Java 8+, but getOrDefault is faster 
            // and more explicit for interviews.
            numCounts.put(number, numCounts.getOrDefault(number, 0) + 1);
        }

        /** 
         * Returns TRUE if any two numbers in the structure add up to the given value.
         * Time: O(U) where U is the number of UNIQUE elements added.
         * Space: O(1) auxiliary.
         */
        public boolean find(int value) {
            // We iterate through all unique keys we have seen.
            for (Map.Entry<Integer, Integer> entry : numCounts.entrySet()) {
                int num = entry.getKey();
                int count = entry.getValue();
                
                // Calculate complement carefully. 
                // Using 'long' prevents Integer underflow/overflow.
                // e.g., if value = -2147483648 and num = 100000, 
                // the complement goes below Integer.MIN_VALUE.
                long complementLong = (long) value - num;
                
                // Optimization: The problem states constraints for 'number' are [-10^5, 10^5].
                // If our required complement is entirely outside this bounds, 
                // there is absolutely zero chance it exists in our map.
                if (complementLong < -100_000 || complementLong > 100_000) {
                    continue;
                }
                
                // Safe to cast down now because we verified it's within the valid constraints.
                int complement = (int) complementLong;
                
                // Case 1: The complement is the same as the number itself.
                // We need to ensure we've seen this number at least twice.
                if (complement == num) {
                    if (count >= 2) {
                        return true;
                    }
                } 
                // Case 2: The complement is a completely different number.
                // We just need to check if it exists in the map.
                else {
                    if (numCounts.containsKey(complement)) {
                        return true;
                    }
                }
            }
            
            // Checked all possibilities, no pair found.
            return false;
        }
    }

    // ============================================================================
    // MAIN METHOD: Testing Approach + Edge Cases
    // ============================================================================
    public static void main(String[] args) {
        TwoSum twoSum = new TwoSum();
        
        // Test 1: Empty Stream
        System.out.println("Find 4 in empty: " + twoSum.find(4)); // Expected: false

        // Test 2: Standard addition and finding
        twoSum.add(1);
        twoSum.add(3);
        twoSum.add(5);
        System.out.println("Find 4 (1+3): " + twoSum.find(4)); // Expected: true
        System.out.println("Find 7 (wait, 7 isn't possible): " + twoSum.find(7)); // Expected: false

        // Test 3: The "Double" Edge Case
        System.out.println("Find 6 with one '3': " + twoSum.find(6)); // Expected: false
        twoSum.add(3); // Now we have two '3's
        System.out.println("Find 6 with two '3's: " + twoSum.find(6)); // Expected: true

        // Test 4: Integer Overflow Protection
        TwoSum overflowTest = new TwoSum();
        overflowTest.add(100000); 
        // Target is Integer.MIN_VALUE. 
        // If we didn't use long, -2147483648 - 100000 = 2147383648 (Positive wrapping)
        // Which might match a false positive if that positive number existed.
        System.out.println("Find MIN_VALUE: " + overflowTest.find(Integer.MIN_VALUE)); // Expected: false
        
        // Test 5: Negative numbers logic
        TwoSum negativesTest = new TwoSum();
        negativesTest.add(-5);
        negativesTest.add(0);
        System.out.println("Find -5 (-5 + 0): " + negativesTest.find(-5)); // Expected: true
    }
}

/**
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core Pattern: Hashing + Algebraic Complement lookup.
 * Key Observation: A set fails because `target = X + X` requires tracking occurrences. 
 * What to Memorize: The loop structure: `if (complement == num) return count >= 2; else return map.contains(complement);`
 * Most Common Trap: Integer overflow when calculating `target - key`. Always use `long` for the intermediate math.
 * Mental Trigger: "Streaming Pairs" -> "Frequency Map".
 * ============================================================================
 */





import java.util.*;

public class TwoSum {

    // Key   = number that has been added.
    // Value = number of occurrences of that number.
    //
    // Example:
    // Added numbers: 2, 5, 2
    // Map: {2=2, 5=1}
    //
    // WHY use frequencies?
    // We need to distinguish one occurrence of a number from two
    // occurrences when the target requires the same number twice.
    private final Map<Integer, Integer> numFreq;

    public TwoSum() {
        // Initially, no numbers have been added.
        this.numFreq = new HashMap<>();
    }

    public void add(int number) {

        // If number is absent, initialize its frequency to 1.
        // Otherwise, increment its existing frequency by 1.
        //
        // Example:
        // add(4) -> {4=1}
        // add(4) -> {4=2}
        //
        // Time: O(1) expected.
        // Space: O(1) expected additional space per call;
        //        O(u) total map storage, where u is the number
        //        of distinct numbers.
        this.numFreq.merge(number, 1, Integer::sum);
    }

    public boolean find(int value) {

        // Iterate over distinct numbers stored in the map.
        // We do not need to examine every occurrence separately.
        for (var entry : this.numFreq.entrySet()) {

            int key = entry.getKey();

            // If one number is key, its required partner is:
            //
            //     partner = target - key
            //
            // Use long to avoid integer overflow during subtraction.
            long rem = (long) value - key;

            // A stored number must fit into the int range.
            // If the complement is outside this range, it cannot exist
            // in our map, because add() accepts only int values.
            if (rem < Integer.MIN_VALUE ||
                rem > Integer.MAX_VALUE) {
                continue;
            }

            int partner = (int) rem;

            // Special case: both numbers in the pair are identical.
            //
            // Example:
            // target = 10, key = 5
            //
            // We need TWO occurrences of 5.
            // One occurrence cannot be used twice.
            if (partner == key) {
                if (entry.getValue() >= 2) {
                    return true;
                }

                // Only one occurrence exists, so this key cannot
                // form a valid pair with itself.
                continue;
            }

            // The partner differs from key.
            // If it exists in the map, one occurrence of each number
            // is enough to form a valid pair.
            if (numFreq.containsKey(partner)) {
                return true;
            }
        }

        // No valid pair was found.
        return false;

        // Time: O(u) expected, where u is the number of distinct numbers.
        // Each complement lookup takes O(1) expected time.
        //
        // Auxiliary space: O(1) for find().
        // Total space: O(u) for the frequency map.
    }
}


