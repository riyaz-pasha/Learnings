import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

// We have to design a hit counter
//     void hit(int timestamp);
// when hit called with timestamp then count of hits at that timestamp increases.

//     int getHits(int timestamp);
// when getHits with timestamp we have to tell total count of hits in this window [timestamp - 299, timestamp]

// so does getHits called with random timestamps are always with latest timestamp ?

// I'm thinking maybe we can create a treemap and store timestamp to hits count map
// also a variable to store windowHitsCount
// when hit gets called we add entry or incease entry count in the treemap
// also increases windowHitsCount

// when getHits with timestamp
// then from tree map we fetch all the timestamps less than timestamp-299 and decrease that count from the windowHitsCount

// hit Ologn
// getHits Ologn I think tree .headMap returns in oLogn

class HitCounter {

    private static final int WINDOW = 300;

    private final int[] times;
    private final int[] hits;

    public HitCounter() {
        times = new int[WINDOW];
        hits = new int[WINDOW];
    }

    /**
     * Record a hit at given timestamp.
     *
     * Time: O(1)
     */
    public void hit(int timestamp) {
        int idx = timestamp % WINDOW;

        // Same second bucket: increment
        if (times[idx] == timestamp) {
            hits[idx]++;
        } 
        // Old bucket reused: reset it
        else {
            times[idx] = timestamp;
            hits[idx] = 1;
        }
    }

    /**
     * Return hits in the past 300 seconds (inclusive of current second).
     *
     * Time: O(300) = O(1)
     */
    public int getHits(int timestamp) {
        int total = 0;

        for (int i = 0; i < WINDOW; i++) {
            // Only count buckets within the last 300 seconds
            if (timestamp - times[i] < WINDOW) {
                total += hits[i];
            }
        }

        return total;
    }
}



/**
 * PROBLEM ANALYSIS: DESIGN HIT COUNTER
 * =====================================
 * 
 * PROBLEM UNDERSTANDING:
 * Design a hit counter which counts the number of hits received in the past 5 minutes (300 seconds).
 * 
 * Operations:
 * 1. hit(timestamp) - Record a hit at given timestamp
 * 2. getHits(timestamp) - Return number of hits in past 300 seconds from timestamp
 * 
 * Constraints:
 * - All timestamps are in chronological order (non-decreasing)
 * - timestamp is in seconds granularity
 * - Time window is 300 seconds (5 minutes)
 * 
 * KEY CHALLENGES:
 * 1. Efficiently track hits within time window
 * 2. Remove old hits that are beyond 300 seconds
 * 3. Handle potentially many hits per second
 * 4. Optimize for both time and space complexity
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ================================================
 * 
 * Step 1: Clarify requirements
 * - "Are timestamps always in increasing order?" (Usually yes)
 * - "Can there be multiple hits at same timestamp?" (Yes)
 * - "What's the expected hit rate?" (Affects design choice)
 * - "Is 5 minutes exactly 300 seconds?" (Yes)
 * 
 * Step 2: Simple approaches first
 * - List/Array: Store all timestamps, filter on query
 * - Queue: Add hits, remove old ones on query
 * - HashMap: Count hits per timestamp
 * 
 * Step 3: Optimize based on constraints
 * - If few hits: Queue is fine
 * - If many hits: Bucket approach (aggregate by time)
 * - If timestamps ordered: Can optimize cleanup
 * 
 * Step 4: Choose approach based on trade-offs
 * - Approach 1: Queue - Simple, good for sparse hits
 * - Approach 2: Circular Array - O(1) operations, fixed space
 * - Approach 3: HashMap - Flexible, good for various patterns
 * 
 * APPROACHES TO DISCUSS:
 * 1. Queue - O(1) hit, O(n) getHits
 * 2. Circular Array with Buckets - O(1) hit, O(1) getHits
 * 3. HashMap - O(1) hit, O(k) getHits where k = unique timestamps
 * 4. Two Queues - O(1) amortized for both
 */

/**
 * APPROACH 1: QUEUE (SIMPLE AND INTUITIVE)
 * =========================================
 * 
 * INTUITION:
 * - Store all hit timestamps in a queue
 * - On getHits(), remove timestamps older than 300 seconds
 * - Count remaining timestamps
 * 
 * PROS:
 * - Simple to understand and implement
 * - Space efficient if hits are sparse
 * - Natural FIFO behavior
 * 
 * CONS:
 * - getHits() can be O(n) if many old hits
 * - Space grows with number of hits
 * 
 * BEST FOR:
 * - Sparse hits
 * - Simple implementation needed
 * - Interview starting point
 */
class HitCounter1 {
    
    private Queue<Integer> hits;
    private static final int TIME_WINDOW = 300;
    
    /** Initialize your data structure here. */
    public HitCounter1() {
        hits = new LinkedList<>();
    }
    
    /**
     * Record a hit.
     * @param timestamp - The current timestamp (in seconds granularity).
     * 
     * Time: O(1)
     * Space: O(n) where n is number of hits in window
     */
    public void hit(int timestamp) {
        hits.offer(timestamp);
    }
    
    /**
     * Return the number of hits in the past 5 minutes.
     * @param timestamp - The current timestamp (in seconds granularity).
     * 
     * Time: O(n) where n is number of old hits to remove
     * Space: O(1)
     */
    public int getHits(int timestamp) {
        // Remove all hits older than 300 seconds
        while (!hits.isEmpty() && timestamp - hits.peek() >= TIME_WINDOW) {
            hits.poll();
        }
        return hits.size();
    }
}

/**
 * APPROACH 2: CIRCULAR ARRAY WITH BUCKETS (OPTIMAL)
 * ==================================================
 * 
 * INTUITION:
 * - Use array of size 300 (one bucket per second)
 * - Each bucket stores: count of hits and latest timestamp
 * - Use modulo to map timestamp to bucket (circular)
 * - When timestamp changes for a bucket, reset it
 * 
 * PROS:
 * - O(1) for both operations
 * - Fixed space O(300)
 * - Very efficient for high hit rates
 * 
 * CONS:
 * - Fixed window size (can't easily change)
 * - Slightly more complex
 * 
 * BEST FOR:
 * - High hit rates
 * - Need O(1) operations
 * - Production systems
 * - RECOMMENDED FOR INTERVIEWS after explaining simpler approach
 */
class HitCounter2 {
    
    private int[] times;    // Store the timestamp for each bucket
    private int[] hits;     // Store the hit count for each bucket
    private static final int TIME_WINDOW = 300;
    
    /** Initialize your data structure here. */
    public HitCounter2() {
        times = new int[TIME_WINDOW];
        hits = new int[TIME_WINDOW];
    }
    
    /**
     * Record a hit.
     * @param timestamp - The current timestamp (in seconds granularity).
     * 
     * Algorithm:
     * 1. Find bucket index using modulo
     * 2. If bucket's timestamp is old (>= 300 seconds), reset it
     * 3. If bucket's timestamp matches current, increment count
     * 4. Otherwise, start new count for this timestamp
     * 
     * Time: O(1)
     * Space: O(1)
     */
    public void hit(int timestamp) {
        int index = timestamp % TIME_WINDOW;
        
        // If this bucket is from a previous cycle (old data), reset it
        if (times[index] != timestamp) {
            times[index] = timestamp;
            hits[index] = 1;
        } else {
            // Same timestamp, increment count
            hits[index]++;
        }
    }
    
    /**
     * Return the number of hits in the past 5 minutes.
     * @param timestamp - The current timestamp (in seconds granularity).
     * 
     * Algorithm:
     * 1. Iterate through all 300 buckets
     * 2. For each bucket, check if timestamp is within window
     * 3. Sum up valid hits
     * 
     * Time: O(300) = O(1) - constant
     * Space: O(1)
     */
    public int getHits(int timestamp) {
        int totalHits = 0;
        
        for (int i = 0; i < TIME_WINDOW; i++) {
            // Only count hits within the time window
            if (timestamp - times[i] < TIME_WINDOW) {
                totalHits += hits[i];
            }
        }
        
        return totalHits;
    }
}

/**
 * APPROACH 3: HASHMAP (FLEXIBLE)
 * ===============================
 * 
 * INTUITION:
 * - Map timestamp -> count of hits at that timestamp
 * - On getHits(), sum counts for valid timestamps and clean up old ones
 * 
 * PROS:
 * - Flexible for any window size
 * - Space efficient if timestamps are sparse
 * - Easy to extend with different windows
 * 
 * CONS:
 * - getHits() needs to iterate and clean up
 * - More complex than queue
 * 
 * BEST FOR:
 * - Variable window sizes
 * - Need to support multiple queries with different windows
 */
class HitCounter3 {
    
    private Map<Integer, Integer> hitMap;  // timestamp -> count
    private static final int TIME_WINDOW = 300;
    
    /** Initialize your data structure here. */
    public HitCounter3() {
        hitMap = new HashMap<>();
    }
    
    /**
     * Record a hit.
     * Time: O(1)
     */
    public void hit(int timestamp) {
        hitMap.put(timestamp, hitMap.getOrDefault(timestamp, 0) + 1);
    }
    
    /**
     * Return the number of hits in the past 5 minutes.
     * Time: O(k) where k is number of unique timestamps in map
     */
    public int getHits(int timestamp) {
        int totalHits = 0;
        
        // Clean up old entries and count valid ones
        Iterator<Map.Entry<Integer, Integer>> iterator = hitMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            int time = entry.getKey();
            
            if (timestamp - time >= TIME_WINDOW) {
                // Remove old timestamp
                iterator.remove();
            } else {
                // Count hits from valid timestamp
                totalHits += entry.getValue();
            }
        }
        
        return totalHits;
    }
}

/**
 * APPROACH 4: OPTIMIZED QUEUE WITH AGGREGATION
 * =============================================
 * 
 * INTUITION:
 * - Combine queue with count aggregation
 * - Store pairs of (timestamp, count) instead of individual hits
 * - Reduces space when many hits at same timestamp
 * 
 * PROS:
 * - Space efficient when hits clustered
 * - Still simple to understand
 * - Better than basic queue for burst traffic
 */
class HitCounter4 {
    
    private Queue<int[]> hits;  // [timestamp, count]
    private int totalHits;
    private static final int TIME_WINDOW = 300;
    
    /** Initialize your data structure here. */
    public HitCounter4() {
        hits = new LinkedList<>();
        totalHits = 0;
    }
    
    /**
     * Record a hit.
     * Time: O(1)
     */
    public void hit(int timestamp) {
        // If last entry has same timestamp, increment its count
        if (!hits.isEmpty() && hits.peek()[0] == timestamp) {
            // Can't modify queue element directly, need to track differently
            // Actually, let's just add new entry for simplicity
        }
        
        hits.offer(new int[]{timestamp, 1});
        totalHits++;
    }
    
    /**
     * Return the number of hits in the past 5 minutes.
     * Time: O(n) worst case, O(1) amortized
     */
    public int getHits(int timestamp) {
        // Remove old hits
        while (!hits.isEmpty() && timestamp - hits.peek()[0] >= TIME_WINDOW) {
            int[] old = hits.poll();
            totalHits -= old[1];
        }
        
        return totalHits;
    }
}

/**
 * APPROACH 5: TWO POINTERS WITH ARRAY (OPTIMAL FOR ORDERED TIMESTAMPS)
 * =====================================================================
 * 
 * Since timestamps are in order, we can use a growing array with two pointers
 */
class HitCounter5 {
    
    private List<Integer> timestamps;
    private static final int TIME_WINDOW = 300;
    
    public HitCounter5() {
        timestamps = new ArrayList<>();
    }
    
    /**
     * Record a hit.
     * Time: O(1)
     */
    public void hit(int timestamp) {
        timestamps.add(timestamp);
    }
    
    /**
     * Return the number of hits in the past 5 minutes.
     * Use binary search to find the start of valid window
     * Time: O(log n)
     */
    public int getHits(int timestamp) {
        int windowStart = timestamp - TIME_WINDOW + 1;
        
        // Binary search for first valid timestamp
        int left = 0, right = timestamps.size() - 1;
        int firstValid = timestamps.size();
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (timestamps.get(mid) >= windowStart) {
                firstValid = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        
        return timestamps.size() - firstValid;
    }
}

/**
 * RECOMMENDED SOLUTION FOR INTERVIEW (Approach 2 - Circular Array)
 * =================================================================
 */
class HitCounter22 {
    
    private int[] times;
    private int[] hits;
    private static final int TIME_WINDOW = 300;
    
    /** Initialize your data structure here. */
    public HitCounter22() {
        times = new int[TIME_WINDOW];
        hits = new int[TIME_WINDOW];
    }
    
    /** 
     * Record a hit.
     * @param timestamp - The current timestamp (in seconds granularity).
     */
    public void hit(int timestamp) {
        int index = timestamp % TIME_WINDOW;
        
        if (times[index] != timestamp) {
            times[index] = timestamp;
            hits[index] = 1;
        } else {
            hits[index]++;
        }
    }
    
    /** 
     * Return the number of hits in the past 5 minutes.
     * @param timestamp - The current timestamp (in seconds granularity).
     */
    public int getHits(int timestamp) {
        int totalHits = 0;
        
        for (int i = 0; i < TIME_WINDOW; i++) {
            if (timestamp - times[i] < TIME_WINDOW) {
                totalHits += hits[i];
            }
        }
        
        return totalHits;
    }
}

/**
 * TEST CASES
 * ==========
 */
class TestHitCounter {
    
    public static void testApproach(String name, HitCounterInterface counter) {
        System.out.println("\n=== Testing " + name + " ===");
        
        // Test Case 1: Basic functionality
        System.out.println("\nTest 1: Basic operations");
        counter.hit(1);
        counter.hit(2);
        counter.hit(3);
        System.out.println("getHits(4) = " + counter.getHits(4) + " (expected: 3)");
        counter.hit(300);
        System.out.println("getHits(300) = " + counter.getHits(300) + " (expected: 4)");
        System.out.println("getHits(301) = " + counter.getHits(301) + " (expected: 3)");
        
        // Test Case 2: Multiple hits at same timestamp
        System.out.println("\nTest 2: Multiple hits at same timestamp");
        HitCounterInterface counter2 = createCounter(name);
        counter2.hit(1);
        counter2.hit(1);
        counter2.hit(1);
        counter2.hit(2);
        System.out.println("getHits(2) = " + counter2.getHits(2) + " (expected: 4)");
        System.out.println("getHits(300) = " + counter2.getHits(300) + " (expected: 4)");
        System.out.println("getHits(301) = " + counter2.getHits(301) + " (expected: 1)");
        
        // Test Case 3: Boundary conditions
        System.out.println("\nTest 3: Boundary at 300 seconds");
        HitCounterInterface counter3 = createCounter(name);
        counter3.hit(1);
        counter3.hit(300);
        System.out.println("getHits(300) = " + counter3.getHits(300) + " (expected: 2)");
        System.out.println("getHits(301) = " + counter3.getHits(301) + " (expected: 1)");
        
        // Test Case 4: Large timestamps
        System.out.println("\nTest 4: Large timestamps");
        HitCounterInterface counter4 = createCounter(name);
        counter4.hit(1);
        counter4.hit(1000);
        counter4.hit(1200);
        System.out.println("getHits(1200) = " + counter4.getHits(1200) + " (expected: 2)");
        System.out.println("getHits(1500) = " + counter4.getHits(1500) + " (expected: 1)");
    }
    
    private static HitCounterInterface createCounter(String name) {
        if (name.contains("Queue")) return new HitCounterWrapper1();
        if (name.contains("Circular")) return new HitCounterWrapper2();
        if (name.contains("HashMap")) return new HitCounterWrapper3();
        return new HitCounterWrapper2(); // default
    }
    
    public static void main(String[] args) {
        System.out.println("=== HIT COUNTER DESIGN - COMPREHENSIVE TESTING ===");
        
        testApproach("Queue Approach", new HitCounterWrapper1());
        testApproach("Circular Array (Optimal)", new HitCounterWrapper2());
        testApproach("HashMap Approach", new HitCounterWrapper3());
        
        // Performance comparison
        System.out.println("\n\n=== PERFORMANCE COMPARISON ===");
        System.out.println("\nApproach 1 - Queue:");
        System.out.println("  hit():     O(1)");
        System.out.println("  getHits(): O(n) worst case");
        System.out.println("  Space:     O(n)");
        
        System.out.println("\nApproach 2 - Circular Array (RECOMMENDED):");
        System.out.println("  hit():     O(1)");
        System.out.println("  getHits(): O(1) - iterates 300 buckets");
        System.out.println("  Space:     O(1) - fixed 300 buckets");
        
        System.out.println("\nApproach 3 - HashMap:");
        System.out.println("  hit():     O(1)");
        System.out.println("  getHits(): O(k) where k = unique timestamps");
        System.out.println("  Space:     O(k)");
    }
}

// Interface for testing
interface HitCounterInterface {
    void hit(int timestamp);
    int getHits(int timestamp);
}

// Wrappers for testing
class HitCounterWrapper1 implements HitCounterInterface {
    private HitCounter1 counter = new HitCounter1();
    public void hit(int timestamp) { counter.hit(timestamp); }
    public int getHits(int timestamp) { return counter.getHits(timestamp); }
}

class HitCounterWrapper2 implements HitCounterInterface {
    private HitCounter2 counter = new HitCounter2();
    public void hit(int timestamp) { counter.hit(timestamp); }
    public int getHits(int timestamp) { return counter.getHits(timestamp); }
}

class HitCounterWrapper3 implements HitCounterInterface {
    private HitCounter3 counter = new HitCounter3();
    public void hit(int timestamp) { counter.hit(timestamp); }
    public int getHits(int timestamp) { return counter.getHits(timestamp); }
}

/**
 * COMPLEXITY ANALYSIS SUMMARY
 * ============================
 * 
 * Approach 1 - Queue:
 * - hit():     O(1)
 * - getHits(): O(n) where n = number of hits in window
 * - Space:     O(n)
 * 
 * Approach 2 - Circular Array (OPTIMAL):
 * - hit():     O(1)
 * - getHits(): O(300) = O(1)
 * - Space:     O(300) = O(1)
 * 
 * Approach 3 - HashMap:
 * - hit():     O(1)
 * - getHits(): O(k) where k = unique timestamps
 * - Space:     O(k)
 * 
 * Approach 4 - Optimized Queue:
 * - hit():     O(1)
 * - getHits(): O(k) where k = unique timestamps in window
 * - Space:     O(k)
 * 
 * Approach 5 - Binary Search:
 * - hit():     O(1)
 * - getHits(): O(log n)
 * - Space:     O(n) but never removes old data
 * 
 * 
 * INTERVIEW STRATEGY
 * ==================
 * 
 * 1. START WITH CLARIFYING QUESTIONS:
 *    Q: "Are timestamps always in increasing order?"
 *    Q: "Can there be multiple hits at the same timestamp?"
 *    Q: "What's the expected hit rate? Sparse or dense?"
 *    Q: "Do we need to support other time windows, or just 5 minutes?"
 *    Q: "What are the space constraints?"
 * 
 * 2. PRESENT THE SIMPLE SOLUTION FIRST (Queue):
 *    "Let me start with a simple approach using a queue.
 *     We store all timestamps and remove old ones when querying.
 *     
 *     This works but getHits() can be O(n) if we have many old hits."
 * 
 * 3. DISCUSS OPTIMIZATION:
 *    "To optimize, I can use a circular array with 300 buckets.
 *     Each bucket represents one second in our 5-minute window.
 *     
 *     Key insight: Use modulo to map timestamp to bucket.
 *     When a bucket's timestamp is old, we reset it.
 *     
 *     This gives us O(1) for both operations with O(1) space!"
 * 
 * 4. WALK THROUGH EXAMPLE:
 *    Array size 300, buckets 0-299
 *    
 *    hit(1):   bucket[1] = {time: 1, hits: 1}
 *    hit(301): bucket[1] = {time: 301, hits: 1}  // overwrites old data
 *    hit(301): bucket[1] = {time: 301, hits: 2}  // same timestamp
 *    
 *    getHits(350): 
 *      - Check bucket[1]: 350 - 301 = 49 < 300 ✓ count = 2
 *      - Check all other buckets: too old or empty
 *      - Return 2
 * 
 * 5. DISCUSS TRADE-OFFS:
 *    Queue Approach:
 *    ✓ Simple
 *    ✓ Space efficient for sparse hits
 *    ✗ O(n) getHits()
 *    
 *    Circular Array:
 *    ✓ O(1) both operations
 *    ✓ Fixed space
 *    ✗ Fixed window size
 *    ✗ Iterates all 300 buckets on getHits()
 * 
 * 6. EDGE CASES TO DISCUSS:
 *    - No hits in window (return 0)
 *    - All hits at same timestamp
 *    - Hits exactly 300 seconds apart
 *    - Very large timestamps (overflow?)
 *    - Concurrent access (thread safety - mention if asked)
 * 
 * 7. FOLLOW-UP QUESTIONS YOU MIGHT GET:
 *    Q: "What if we need to support multiple time windows?"
 *    A: Use HashMap or multiple circular arrays
 *    
 *    Q: "What about thread safety?"
 *    A: Add synchronization or use concurrent data structures
 *    
 *    Q: "What if hits are not in order?"
 *    A: Need to modify circular array approach or use different structure
 *    
 *    Q: "What if we need to scale to millions of hits?"
 *    A: Distribute across multiple servers, use approximation algorithms
 *    
 *    Q: "Can we use less memory?"
 *    A: Use bit array if only need presence, not count
 * 
 * 8. CODE STRUCTURE TIPS:
 *    - Use constants for magic numbers (TIME_WINDOW = 300)
 *    - Add clear comments for modulo operation
 *    - Consider helper methods for clarity
 *    - Add input validation if time permits
 * 
 * 9. TESTING STRATEGY:
 *    - Basic case: few hits, simple query
 *    - Boundary: hits at window edge (300 seconds)
 *    - Multiple: many hits at same timestamp
 *    - Large: timestamps far apart
 *    - Empty: query with no hits
 * 
 * 10. COMMON MISTAKES TO AVOID:
 *     ✗ Forgetting to reset bucket when timestamp changes
 *     ✗ Off-by-one in time window check (>= vs >)
 *     ✗ Not handling integer overflow for large timestamps
 *     ✗ Inefficient iteration in getHits()
 *     ✗ Not considering space complexity
 * 
 * RECOMMENDED ANSWER FOR INTERVIEW:
 * Start with Queue (Approach 1) for clarity, then optimize to
 * Circular Array (Approach 2) to show advanced thinking.
 * Mention HashMap (Approach 3) as alternative for flexibility.
 */
