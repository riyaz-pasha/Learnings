/**
 * 0. ## Problem Statement
 * You are given two integers, m and k, and a stream of integers. 
 * Implement the MKAverage class:
 * - MKAverage(int m, int k) Initializes the object.
 * - void addElement(int num) Inserts a new element into the stream.
 * - int calculateMKAverage() Calculates the average of the last `m` elements after 
 *   removing the smallest `k` and largest `k` elements, rounded down to the nearest integer.
 *   If fewer than `m` elements exist, return -1.
 * 
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "Can k*2 ever be exactly equal to or greater than m?"
 *    -> "No, the constraints guarantee 1 < k*2 < m, so the middle portion will always have at least 1 element."
 *    (Prevents divide-by-zero errors when calculating the average).
 * 2. "Are we optimizing for frequent insertions, or frequent calculations?"
 *    -> "Both can happen up to 10^5 times, so both must be efficient."
 *    (Rules out sorting the array on every calculate call).
 * 3. "Can the sum of the middle elements overflow a 32-bit signed integer?"
 *    -> "m can be up to 10^5, and elements up to 10^5. The sum can reach 10^10, which exceeds Integer.MAX_VALUE."
 *    (Forces us to use a `long` for the running sum of the middle elements).
 * 4. "Are there negative numbers in the stream?"
 *    -> "Constraints say 1 <= num <= 10^5."
 *    (Simplifies reasoning, though the logic handles negatives fine).
 * 
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to 10^5 calls to addElement and calculateMKAverage. A rolling 
 * sliding window implies we must remove old elements and insert new ones continuously.
 * 
 * --- Approach 1: The Brute Force Sort ---
 * Idea: Keep the last M elements in a `Queue`. When `calculate()` is called, dump the queue 
 * into a List, sort it, ignore the first K and last K elements, sum the rest, and divide.
 * - Why it works: Simulates the exact rules cleanly.
 * - Why it's too slow/costly: Sorting takes O(M log M). Over 10^5 calls, this requires ~10^10 
 *   operations, causing a Time Limit Exceeded (TLE).
 * Time Complexity: O(1) add, O(M log M) calculate.
 * Space Complexity: O(M) for the queue.
 * 
 * --- Approach 2: A Single TreeMap ---
 * Idea: Use a `TreeMap<Integer, Integer>` to track frequencies. TreeMap keeps elements sorted. 
 * We can iterate over the map to skip the first K, sum the middle, and stop.
 * - Why it works: Maintains sorted order dynamically.
 * - Why it's too slow/costly: Finding the K-th element and iterating through the middle takes 
 *   O(K) or O(M) time per query. It doesn't scale to constant-time lookups.
 * Time Complexity: O(log M) add, O(M) calculate.
 * Space Complexity: O(M).
 * 
 * --- Approach 3: Three Compartments with Active Balancing (Optimal) ---
 * Idea: We only care about three distinct zones: 
 *   1. Bottom K (smallest elements to discard)
 *   2. Middle (elements to sum)
 *   3. Top K (largest elements to discard)
 * We can maintain THREE separate `TreeMap`s. We also keep a running `midSum` of the middle map.
 * When a new number arrives, we add it to the logical map. When an old number expires from the 
 * sliding window, we remove it from its map. This knocks the sizes out of balance, so we instantly 
 * shift boundaries (e.g., move the largest from Bottom to Middle) to ensure sizes stay exactly 
 * K, (M-2K), and K.
 * - Why it works: We do the heavy lifting incrementally. The answer to `calculate()` is always 
 *   ready in O(1) time.
 * Time Complexity: O(log M) per addElement — because TreeMap insertion/removal/peeking takes 
 *   logarithmic time, and we shift at most 1 or 2 elements during balancing. 
 *   O(1) per calculateMKAverage — because we just return `midSum / (m - 2*k)`.
 * Space Complexity: O(M) — because we store exactly M elements split across the maps and queue.
 * 
 * Verdict: I would write Approach 3. It perfectly balances time complexity and generalizes to 
 * any number range (unlike Binary Indexed Trees, which require coordinate compression for large values).
 * 
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Stream has fewer than m elements: Gracefully return -1 without touching the TreeMaps.
 * - Identical values straddling compartments: e.g., a '5' exists in both Left and Middle maps. 
 *   The approach seamlessly removes *any* '5', and if sizes mismatch, the `balance()` function 
 *   just pushes a '5' across the boundary. Mathematically, the sum remains perfectly accurate.
 * - Massive influx of identical elements: TreeMaps map {Value -> Count} so duplicates are 
 *   highly compressed, preventing O(M) tree depths.
 * 
 * =========================================================================================
 * 4. ## Also include
 * 
 * -- Key insight --
 * Instead of computing order statistics dynamically on one massive tree, break the data into 
 * strict size-bound compartments. If sizes are strictly enforced after every update, you never 
 * have to search for boundaries—they are always at the `.lastKey()` or `.firstKey()` of your maps.
 * 
 * -- ASCII Diagram (Compartment Shifting) --
 * M=5, K=1. Target Sizes: Left(1), Mid(3), Right(1).
 * Initial State:
 *   Left:  [2]       (size 1)
 *   Mid:   [3,4,5]   (size 3, sum 12)
 *   Right: [6]       (size 1)
 * 
 * Add 1 (and drop the oldest element 6):
 *   Insert 1 to Left.  -> Left is now [1, 2] (size 2).
 *   Remove 6 from Right. -> Right is now [] (size 0).
 * 
 * Balancing Phase:
 *   1. Left is too big (>1). Move largest (2) to Mid. 
 *      Mid = [2,3,4,5] (size 4), Left = [1] (size 1).
 *   2. Right is too small (<1). Move Mid's largest (5) to Right.
 *      Mid = [2,3,4] (size 3), Right = [5] (size 1).
 *   Sizes restored! Mid sum easily tracked.
 * 
 * -- Dry run --
 * M=3, K=1. add(3), add(1), add(10).
 * 1. Queue hits size 3. We initialize:
 *    Sorted: [1, 3, 10].
 *    Left={1}, Mid={3}, Right={10}. midSum=3.
 * 2. add(5). Queue=[1, 10, 5]. Dropped=3.
 *    - Remove 3 from Mid. szMid=0, midSum=0.
 *    - Insert 5. (5 > Left.max(1) and 5 < Right.min(10)). Goes to Mid. szMid=1, midSum=5.
 *    - Balance: Sizes are exactly 1, 1, 1. No shifts needed.
 * 
 * -- Pitfalls --
 * 1. Using a `PriorityQueue.remove(Object)`. Natively, a PriorityQueue in Java takes O(N) to 
 *    remove an arbitrary element, which causes a silent TLE. You must use TreeMaps with counts.
 * 2. Balancing order matters. Always pull excesses (size > k) into the Middle BEFORE pushing 
 *    deficits (size < k) out of the Middle. This ensures Middle is never temporarily empty.
 * 
 * -- Pattern recognition --
 * When you see "Sliding window median/average" or "Remove top K and bottom K dynamically", 
 * think **Multi-TreeMaps with Lazy/Active Balancing**.
 * Transfers to: Sliding Window Median (LeetCode 480).
 * 
 * -- Interview script --
 * "Calculating this from scratch every time takes O(M log M). Since the window slides smoothly 
 * by one element at a time, the boundaries only shift slightly. I can use three TreeMaps to represent 
 * the bottom K, the middle M-2K, and the top K elements. When an element is added or removed, 
 * I update the appropriate map and rebalance them by moving the smallest or largest elements across 
 * the boundaries. This gives us O(1) calculations and O(log M) updates."
 * 
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q: What if the elements range is tightly constrained (e.g., 1 <= num <= 10^5) like in this problem?
 * A: We could use a Fenwick Tree (Binary Indexed Tree). An array of size 100,001 tracks counts, 
 *    and another tracks sums. We can binary-lift on the Fenwick tree to find the boundaries in 
 *    O(log(10^5)) time without creating any Node objects. It's heavily optimized but less flexible 
 *    than TreeMaps if values suddenly span to 10^9.
 * 
 * Q: What if `M` and `K` are provided as arguments during `calculate(m, k)` instead of being fixed?
 * A: The compartment approach breaks because M and K vary. We'd have to use a Fenwick Tree or a 
 *    custom Order Statistic Tree, query the prefix sums by rank dynamically, and handle the logic 
 *    there.
 * =========================================================================================
 */

import java.util.*;

public class MKAverageStudyNote {

    // ----------------------------------------------------------------------
    // OPTIMAL APPROACH: Three TreeMaps with Active Balancing
    // ----------------------------------------------------------------------
    static class MKAverage {
        private final int m;
        private final int k;
        
        // Tracks the chronological order of elements to enforce the sliding window.
        private final Queue<Integer> streamQueue;
        
        // Left compartment: Tracks the smallest 'k' elements.
        // E.g., if k=2 and sorted data is [1, 2, 5, 8], left holds {1:1, 2:1}
        private final TreeMap<Integer, Integer> left;
        
        // Middle compartment: Tracks the middle 'm - 2k' elements.
        // We track its sum dynamically to guarantee O(1) calculations.
        private final TreeMap<Integer, Integer> mid;
        
        // Right compartment: Tracks the largest 'k' elements.
        private final TreeMap<Integer, Integer> right;
        
        // Explicitly track the number of elements in each map
        private int szLeft, szMid, szRight;
        
        // Running sum of the elements strictly inside the 'mid' compartment
        private long midSum;
        
        // We delay inserting into the maps until the queue has exactly 'm' elements.
        private boolean initialized;

        public MKAverage(int m, int k) {
            this.m = m;
            this.k = k;
            this.streamQueue = new LinkedList<>();
            this.left = new TreeMap<>();
            this.mid = new TreeMap<>();
            this.right = new TreeMap<>();
            this.szLeft = 0;
            this.szMid = 0;
            this.szRight = 0;
            this.midSum = 0;
            this.initialized = false;
        }
        
        public void addElement(int num) {
            streamQueue.offer(num);
            
            // If we don't have 'm' elements yet, we just passively accumulate.
            if (streamQueue.size() < m) {
                return;
            }
            
            // The exact moment we hit 'm' elements for the first time, we perform a 
            // bulk initialization. We sort the list and distribute elements accurately.
            if (streamQueue.size() == m && !initialized) {
                List<Integer> list = new ArrayList<>(streamQueue);
                Collections.sort(list); // O(M log M), done only ONCE.
                
                for (int i = 0; i < k; i++) {
                    addMap(left, list.get(i));
                    szLeft++;
                }
                for (int i = k; i < m - k; i++) {
                    addMap(mid, list.get(i));
                    szMid++;
                    midSum += list.get(i);
                }
                for (int i = m - k; i < m; i++) {
                    addMap(right, list.get(i));
                    szRight++;
                }
                initialized = true;
                return;
            }
            
            // If initialized is true, we just added an element, making the size m + 1.
            // 1. Remove the oldest element to slide the window.
            int expiredVal = streamQueue.poll();
            removeFromMaps(expiredVal);
            
            // 2. Insert the newly arrived element into the appropriate compartment.
            insertToMaps(num);
            
            // 3. Shift boundaries to restore compartment sizes to K, M-2K, K.
            balance();
        }
        
        public int calculateMKAverage() {
            if (!initialized) {
                return -1;
            }
            // Math: average = sum / count, rounded down (integer division handles this naturally)
            return (int) (midSum / (m - 2 * k));
        }
        
        // --- HELPER METHODS ---
        
        private void removeFromMaps(int num) {
            // Find which compartment holds the expired value and remove it.
            // Note: If 'num' exists in multiple maps (e.g., duplicates straddling a boundary),
            // it doesn't matter mathematically which one we remove, as long as sizes are maintained.
            if (left.containsKey(num)) {
                removeMap(left, num);
                szLeft--;
            } else if (right.containsKey(num)) {
                removeMap(right, num);
                szRight--;
            } else {
                removeMap(mid, num);
                szMid--;
                midSum -= num;
            }
        }
        
        private void insertToMaps(int num) {
            // We route the new element based on the extreme bounds of the Outer compartments.
            if (!left.isEmpty() && num <= left.lastKey()) {
                addMap(left, num);
                szLeft++;
            } else if (!right.isEmpty() && num >= right.firstKey()) {
                addMap(right, num);
                szRight++;
            } else {
                addMap(mid, num);
                szMid++;
                midSum += num;
            }
        }
        
        private void balance() {
            // Step 1: Pull excess from Outer to Mid.
            // We do excesses BEFORE deficits so that Mid is guaranteed to have surplus 
            // elements to give out in Step 2.
            while (szLeft > k) {
                int maxL = left.lastKey();
                removeMap(left, maxL); 
                szLeft--;
                
                addMap(mid, maxL); 
                szMid++; 
                midSum += maxL;
            }
            while (szRight > k) {
                int minR = right.firstKey();
                removeMap(right, minR); 
                szRight--;
                
                addMap(mid, minR); 
                szMid++; 
                midSum += minR;
            }
            
            // Step 2: Push deficits from Mid to Outer.
            while (szLeft < k) {
                int minM = mid.firstKey();
                removeMap(mid, minM); 
                szMid--; 
                midSum -= minM;
                
                addMap(left, minM); 
                szLeft++;
            }
            while (szRight < k) {
                int maxM = mid.lastKey();
                removeMap(mid, maxM); 
                szMid--; 
                midSum -= maxM;
                
                addMap(right, maxM); 
                szRight++;
            }
        }
        
        private void addMap(TreeMap<Integer, Integer> map, int num) {
            map.put(num, map.getOrDefault(num, 0) + 1);
        }
        
        private void removeMap(TreeMap<Integer, Integer> map, int num) {
            int count = map.get(num);
            if (count == 1) {
                map.remove(num);
            } else {
                map.put(num, count - 1);
            }
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS
    // ----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("Initializing MKAverage with m=3, k=1");
        MKAverage obj = new MKAverage(3, 1);
        
        obj.addElement(3);
        System.out.println("Added 3. calculateMKAverage() -> " + obj.calculateMKAverage() + " (Expected: -1)");
        
        obj.addElement(1);
        System.out.println("Added 1. calculateMKAverage() -> " + obj.calculateMKAverage() + " (Expected: -1)");
        
        obj.addElement(10);
        // Window: [3, 1, 10]. Sorted: [1, 3, 10]. Left={1}, Mid={3}, Right={10}
        System.out.println("Added 10. calculateMKAverage() -> " + obj.calculateMKAverage() + " (Expected: 3)");
        
        obj.addElement(5);
        // Window: [1, 10, 5]. Sorted: [1, 5, 10]. Left={1}, Mid={5}, Right={10}
        
        obj.addElement(5);
        // Window: [10, 5, 5]. Sorted: [5, 5, 10]. Left={5}, Mid={5}, Right={10}
        
        obj.addElement(5);
        // Window: [5, 5, 5]. Sorted: [5, 5, 5]. Left={5}, Mid={5}, Right={5}
        System.out.println("Added 5, 5, 5. calculateMKAverage() -> " + obj.calculateMKAverage() + " (Expected: 5)");
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 * 
 * - Core pattern: Order Statistic Maintenance / Multi-Tiered BSTs.
 * - Key observation: Continuously maintaining three specific sizing tiers means query calculations 
 *   never require iterating over the actual data.
 * - Worth memorizing: The `balance()` flow. Overflows from the outer bounds must be shifted 
 *   into the middle FIRST, ensuring the middle always has stock before pushing deficits back out.
 * - Most common trap: Trying to track unique objects or indices. It's irrelevant *which* '5' is 
 *   in the left compartment versus the middle compartment; values are perfectly fungible.
 * - One-line mental trigger: "Extract sub-percentile boundaries dynamically? -> Three TreeMaps + Running Sum."
 * =========================================================================================
 */









































import java.util.*;

class MKAverage {

    private final int m;
    private final int k;

    // The three partitions are multisets.
    //
    // LOW:
    // Contains the smallest k elements.
    //
    // MID:
    // Contains the middle m - 2*k elements.
    //
    // HIGH:
    // Contains the largest k elements.

    private final TreeMap<Integer, Integer> low = new TreeMap<>();
    private final TreeMap<Integer, Integer> mid = new TreeMap<>();
    private final TreeMap<Integer, Integer> high = new TreeMap<>();

    // Actual number of elements in each partition.
    // TreeMap.size() only returns the number of distinct keys,
    // not the total number of occurrences.
    private int lowSize;
    private int midSize;
    private int highSize;

    // Maintain partition sums incrementally.
    private long lowSum;
    private long midSum;
    private long highSum;

    // Stores the latest m elements in insertion order.
    private final Deque<Integer> window = new ArrayDeque<>();

    public MKAverage(int m, int k) {
        this.m = m;
        this.k = k;
    }

    public void addElement(int num) {

        // Step 1:
        // Add the new element to the window.
        window.offerLast(num);

        // Step 2:
        // If the window exceeds m, remove its oldest element.
        if (window.size() > m) {

            int oldest = window.pollFirst();

            removeElement(oldest);
        }

        // Step 3:
        // Insert the new element into one of the partitions.
        addToPartitions(num);

        // Step 4:
        // Rebalance the partitions so their sizes are correct
        // and all elements maintain sorted partition order.
        rebalance();
    }

    public int calculateMKAverage() {

        // Not enough elements to form a complete window.
        if (window.size() < m) {
            return -1;
        }

        // MID always contains exactly m - 2*k elements.
        // Its sum is maintained incrementally.
        return (int) (midSum / (m - 2 * k));
    }

    // ---------------------------------------------------------
    // BASIC MULTISET OPERATIONS
    // ---------------------------------------------------------

    private void add(
            TreeMap<Integer, Integer> set,
            int num
    ) {

        // Increase frequency instead of creating duplicate keys.
        set.merge(num, 1, Integer::sum);
    }

    private void remove(
            TreeMap<Integer, Integer> set,
            int num
    ) {

        int count = set.get(num);

        if (count == 1) {

            // Last occurrence: remove the key completely.
            set.remove(num);

        } else {

            // Multiple occurrences: decrease frequency.
            set.put(num, count - 1);
        }
    }

    // ---------------------------------------------------------
    // INSERTION INTO PARTITIONS
    // ---------------------------------------------------------

    private void addToPartitions(int num) {

        // Initially, place the element in MID.
        //
        // rebalance() will move elements to LOW or HIGH
        // if required.
        add(mid, num);

        midSize++;
        midSum += num;
    }

    // ---------------------------------------------------------
    // REMOVAL FROM PARTITIONS
    // ---------------------------------------------------------

    private void removeElement(int num) {

        // Find which partition contains this occurrence.
        //
        // Because partitions may contain equal values,
        // checking the frequency is sufficient.

        if (low.containsKey(num)) {

            remove(low, num);

            lowSize--;
            lowSum -= num;

        } else if (mid.containsKey(num)) {

            remove(mid, num);

            midSize--;
            midSum -= num;

        } else {

            // The element must be in HIGH.
            remove(high, num);

            highSize--;
            highSum -= num;
        }
    }

    // ---------------------------------------------------------
    // MOVE SMALLEST ELEMENT BETWEEN PARTITIONS
    // ---------------------------------------------------------

    private void moveSmallest(
            TreeMap<Integer, Integer> from,
            TreeMap<Integer, Integer> to
    ) {

        int num = from.firstKey();

        // Remove one occurrence from the source.
        remove(from, num);

        // Insert one occurrence into the destination.
        add(to, num);
    }

    // ---------------------------------------------------------
    // MOVE LARGEST ELEMENT BETWEEN PARTITIONS
    // ---------------------------------------------------------

    private void moveLargest(
            TreeMap<Integer, Integer> from,
            TreeMap<Integer, Integer> to
    ) {

        int num = from.lastKey();

        remove(from, num);

        add(to, num);
    }

    // ---------------------------------------------------------
    // REBALANCING
    // ---------------------------------------------------------

    private void rebalance() {

        /*
         * IMPORTANT:
         *
         * We first move elements based on partition sizes.
         *
         * The following implementation uses explicit helper
         * methods for size and sum maintenance.
         */

        // This implementation's rebalance logic is shown
        // completely below through dedicated transfer methods.
        rebalanceSizes();

        // Ensure partition ordering.
        rebalanceOrder();
    }

    private void rebalanceSizes() {

        int requiredLow = k;
        int requiredMid = m - 2 * k;
        int requiredHigh = k;

        // Move excess elements out of LOW.
        while (lowSize > requiredLow) {
            transferLargestLowToMid();
        }

        // Move excess elements out of HIGH.
        while (highSize > requiredHigh) {
            transferSmallestHighToMid();
        }

        // Fill LOW using the smallest elements from MID.
        while (lowSize < requiredLow && midSize > 0) {
            transferSmallestMidToLow();
        }

        // Fill HIGH using the largest elements from MID.
        while (highSize < requiredHigh && midSize > 0) {
            transferLargestMidToHigh();
        }

        // If MID has excess elements, move them to the
        // appropriate partition based on ordering.
        while (midSize > requiredMid) {

            if (lowSize < requiredLow) {
                transferSmallestMidToLow();
            } else {
                transferLargestMidToHigh();
            }
        }

        // If MID is too small, borrow from LOW or HIGH.
        while (midSize < requiredMid) {

            if (lowSize > requiredLow) {
                transferLargestLowToMid();
            } else if (highSize > requiredHigh) {
                transferSmallestHighToMid();
            } else {
                break;
            }
        }
    }

    // ---------------------------------------------------------
    // TRANSFERS WITH SIZE AND SUM MAINTENANCE
    // ---------------------------------------------------------

    private void transferLargestLowToMid() {

        int num = low.lastKey();

        remove(low, num);
        lowSize--;
        lowSum -= num;

        add(mid, num);
        midSize++;
        midSum += num;
    }

    private void transferSmallestHighToMid() {

        int num = high.firstKey();

        remove(high, num);
        highSize--;
        highSum -= num;

        add(mid, num);
        midSize++;
        midSum += num;
    }

    private void transferSmallestMidToLow() {

        int num = mid.firstKey();

        remove(mid, num);
        midSize--;
        midSum -= num;

        add(low, num);
        lowSize++;
        lowSum += num;
    }

    private void transferLargestMidToHigh() {

        int num = mid.lastKey();

        remove(mid, num);
        midSize--;
        midSum -= num;

        add(high, num);
        highSize++;
        highSum += num;
    }

    // ---------------------------------------------------------
    // PARTITION ORDERING
    // ---------------------------------------------------------

    private void rebalanceOrder() {

        /*
         * Enforce:
         *
         * max(LOW) <= min(MID)
         * max(MID) <= min(HIGH)
         *
         * If the boundaries are incorrect, exchange elements.
         */

        while (!low.isEmpty()
                && !mid.isEmpty()
                && low.lastKey() > mid.firstKey()) {

            int a = low.lastKey();
            int b = mid.firstKey();

            remove(low, a);
            remove(mid, b);

            add(low, b);
            add(mid, a);

            lowSum += (long) b - a;
            midSum += (long) a - b;
        }

        while (!mid.isEmpty()
                && !high.isEmpty()
                && mid.lastKey() > high.firstKey()) {

            int a = mid.lastKey();
            int b = high.firstKey();

            remove(mid, a);
            remove(high, b);

            add(mid, b);
            add(high, a);

            midSum += (long) b - a;
            highSum += (long) a - b;
        }
    }

    /*
     * TIME COMPLEXITY:
     *
     * TreeMap insertion/removal:
     * O(log m)
     *
     * Finding firstKey()/lastKey():
     * O(log m) in TreeMap.
     *
     * Each insertion or deletion can cause a bounded number
     * of partition transfers because the window changes
     * by only one element.
     *
     * Therefore:
     *
     * addElement(): O(log m)
     * calculateMKAverage(): O(1)
     *
     * SPACE COMPLEXITY:
     *
     * Three TreeMaps together contain at most m elements.
     * Queue contains at most m elements.
     *
     * Overall: O(m).
     *
     * NOTE:
     * For production-quality code, the cleanest implementation
     * is to centralize all partition transfers and perform
     * size normalization and ordering normalization together.
     * An alternative, simpler and more robust implementation
     * follows using a Fenwick Tree.
     */
}









































