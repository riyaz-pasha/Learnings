import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: "LAST STONE WEIGHT" (LeetCode 1046)
 * ============================================================================
 *
 * This single file walks through the full interview lifecycle for this
 * problem: restatement, clarifying questions, examples, every meaningful
 * approach (from brute force to optimal), a comparison table, the
 * recommended approach, a polished production implementation, a manual
 * dry run, a closing summary, follow-ups, and common candidate mistakes.
 *
 * Run with: java LastStoneWeight.java   (Java 21+ single-file source launch)
 * ============================================================================
 */
class LastStoneWeight {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In plain English:
     *   We have a bag of stones, each with a positive integer weight. We
     *   repeatedly grab the two HEAVIEST stones in the bag and smash them
     *   together:
     *     - If they're equal weight, both are destroyed (poof, gone).
     *     - If they differ, the lighter one is destroyed, and the heavier
     *       one survives but its new weight becomes (heavy - light).
     *   We keep doing this until 0 or 1 stones remain, and we return the
     *   weight of the last stone left (or 0 if the bag is empty).
     *
     * Inputs:
     *   - int[] stones : array of stone weights, 1 <= stones.length <= 30,
     *                     1 <= stones[i] <= 1000.
     *
     * Output:
     *   - A single int: the weight of the final remaining stone, or 0 if
     *     none remain.
     *
     * Key observations / assumptions to confirm:
     *   - This is a SIMULATION problem, not an optimization-over-choices
     *     problem. At every step there is exactly one valid move (always
     *     pick the two current maxima), so the final answer is fully
     *     deterministic -- there's no "strategy" to search over. This is
     *     an important distinction from the related problem "Last Stone
     *     Weight II," which allows assigning +/- signs to *all* stones
     *     and asks for the minimum achievable result (that one needs a
     *     subset-sum DP). I want to flag out loud that I recognize this
     *     is the simulation variant, not the optimization variant.
     *   - Weights are bounded and small (<= 1000), and n <= 30 -- this is
     *     a strong signal the intended solution is a heap-based O(n log n)
     *     simulation, and the tiny weight bound even opens the door to a
     *     counting-sort / bucket style O(n + maxWeight) approach.
     */

    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * 1. Q: Can the array be empty?
     *    A: Per constraints, length >= 1, so no empty array. I'll still
     *       defensively handle length 0 by returning 0.
     *
     * 2. Q: Are stone weights always positive integers, never zero or
     *       negative?
     *    A: Yes, 1 <= stones[i] <= 1000 per constraints.
     *
     * 3. Q: When there's a tie for "the two largest," and more than two
     *       stones share the max weight, does it matter which two we pick?
     *    A: No -- since we're only tracking weights (stones are
     *       indistinguishable beyond their weight), picking any two stones
     *       tied for the largest weight yields the same final result.
     *
     * 4. Q: Should the original input array be mutated, or should I treat
     *       it as read-only and use auxiliary structures?
     *    A: Treat it as read-only; copy into an auxiliary structure (heap,
     *       list, etc.) so we don't surprise the caller with side effects.
     *
     * 5. Q: Is the array sorted or otherwise pre-processed already?
     *    A: No, assume arbitrary/unsorted order.
     *
     * 6. Q: Do we need to support this being called repeatedly / concurrently
     *       (e.g., a service handling many requests), or is this a one-shot
     *       single-threaded computation?
     *    A: One-shot, single-threaded call for this problem; no thread-safety
     *       requirements. I'd mention that if this became a shared service,
     *       I'd make sure each call uses its own local heap instance (which
     *       it already does), so it's naturally thread-safe as written.
     *
     * 7. Q: What should happen with a single-element input?
     *    A: No smashing occurs (need at least 2 stones to smash); return
     *       that single stone's weight.
     *
     * 8. Q: Is n small enough (<=30) that I should even worry about
     *       asymptotic complexity, or is clarity prioritized?
     *    A: Given n <= 30, any of the approaches below run essentially
     *       instantly. Still, in a Google interview I'm expected to reason
     *       about complexity as if n could scale, and pick/justify the best
     *       asymptotic approach -- so I will optimize for that, while noting
     *       the practical constraint out loud.
     */

    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case): stones = [2, 7, 4, 1, 8, 1]
     *   Sorted view each round (largest two smashed):
     *     [2,7,4,1,8,1] -> smash 8,7 -> new stone 1  -> [2,4,1,1,1]
     *     [2,4,1,1,1]   -> smash 4,2 -> new stone 2  -> [2,1,1,1]
     *     [2,1,1,1]     -> smash 2,1 -> new stone 1  -> [1,1,1]
     *     [1,1,1]       -> smash 1,1 -> equal, both destroyed -> [1]
     *   Final: 1
     *
     * Example 2 (Edge case: all stones destroy each other, empty result):
     *   stones = [1, 1]
     *     -> smash 1,1 -> equal weights -> both destroyed -> []
     *   Final: 0  (no stones remain)
     *
     * Example 3 (Boundary / tie-breaking case: single stone, and a case
     * with multiple equal maxima to show tie choice doesn't matter):
     *   stones = [5]                 -> no smashing possible -> Final: 5
     *   stones = [3, 3, 3]  (ties at the top)
     *     -> two of the 3's smash (equal) -> both destroyed -> [3]
     *     -> only one stone left, done -> Final: 3
     *     (Doesn't matter WHICH two 3's we pick first -- same result.)
     */

    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Paradigms considered and why most are skipped:
     *   - Divide & Conquer: No natural way to split the problem, since each
     *     smash operation depends on the GLOBAL two largest values at every
     *     step; sub-problem results don't combine cleanly. Not applicable.
     *   - Dynamic Programming: There's no choice/optimization to make -- the
     *     move at each step is forced (always take the two current maxima).
     *     DP solves problems with overlapping subproblems from CHOICES;
     *     there are no choices here. (Contrast with "Last Stone Weight II,"
     *     which DOES use subset-sum DP -- but that's a different problem.)
     *   - Tree / Graph traversal: No graph or tree structure underlies this
     *     problem. Not applicable.
     *   - Binary Search: There's no monotonic search space to binary search
     *     over (we're not searching for a threshold value). Not applicable.
     *   - Trie / Segment Tree: No prefix/range-query structure is needed
     *     here. Not applicable.
     *   - Monotonic Stack/Deque: These shine when we need the "next greater/
     *     smaller element" under a single left-to-right pass; here we need
     *     repeated global-max extraction after inserting new derived values,
     *     which is exactly what a heap is for, not a monotonic stack.
     *
     * Paradigms that ARE applicable, covered below:
     *   - Brute Force (repeated sort)
     *   - Sorting-based (maintain sorted order incrementally)
     *   - Heap / Priority Queue (the standard optimal approach)
     *   - Hashing / Bucket-Counting (exploit the tiny weight bound <= 1000)
     *   - Balanced BST / TreeMap multiset (alternative to heap)
     */

    /*
     * ------------------------------------------------------------------------
     * Approach 1: Brute Force -- Re-sort Every Iteration
     * ------------------------------------------------------------------------
     * Core idea:
     *   Put all stones in a list. On every iteration, fully sort the list,
     *   pull out the last two (largest two), compute the smash result, and
     *   push any surviving stone back in. Repeat until <= 1 stone remains.
     *
     * Data structure / paradigm: plain List + full sort each round.
     *
     * Time Complexity: O(n^2 log n)
     *   - Up to n-1 smash operations occur, and each one re-sorts up to n
     *     elements: O(n log n) per round * O(n) rounds.
     * Space Complexity: O(n) for the working list (ignoring sort's internal
     *   space, which is O(log n) to O(n) depending on implementation).
     *
     * Pros:
     *   - Extremely simple to write and reason about; low bug risk.
     *   - Easy to explain to an interviewer as a warm-up / correctness
     *     baseline.
     * Cons:
     *   - Wasteful: we re-sort the ENTIRE list even though only one or two
     *     elements changed each round.
     *   - Would not scale if n were large (e.g., n = 10^5).
     *
     * When to use: Only as a first-pass "get something correct on the
     * board" solution, or when n is tiny and code simplicity trumps
     * performance. I would explicitly narrate this as my starting point,
     * then immediately propose the optimization.
     */
    public int approach1_BruteForceResort(int[] stones) {
        List<Integer> workingList = new ArrayList<>();
        for (int weight : stones) {
            workingList.add(weight);
        }

        while (workingList.size() > 1) {
            Collections.sort(workingList); // ascending order, O(n log n)
            int largest = workingList.remove(workingList.size() - 1);
            int secondLargest = workingList.remove(workingList.size() - 1);
            if (largest != secondLargest) {
                workingList.add(largest - secondLargest);
            }
            // if equal, both are discarded (already removed, nothing to add back)
        }

        return workingList.isEmpty() ? 0 : workingList.get(0);
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 2: Sorting-based -- Keep the List Sorted Incrementally
     * ------------------------------------------------------------------------
     * Core idea:
     *   Sort once up front. Then, instead of re-sorting every round, use
     *   binary search (Collections.binarySearch) to insert the newly
     *   created stone back into its correct sorted position. This avoids
     *   a full O(n log n) re-sort per round.
     *
     * Data structure / paradigm: sorted ArrayList + binary search insertion.
     *
     * Time Complexity: O(n^2)
     *   - One initial sort: O(n log n).
     *   - Each of the ~n rounds does an O(log n) binary search but an
     *     O(n) shift to insert into an ArrayList (array-backed list
     *     insertion is linear). n rounds * O(n) insertion = O(n^2).
     *     (This dominates the O(n log n) initial sort and the O(log n)
     *     searches.)
     * Space Complexity: O(n).
     *
     * Pros:
     *   - Avoids full re-sorts; conceptually a nice middle-ground
     *     optimization over Approach 1.
     *   - Still fairly easy to reason about (array stays sorted at all
     *     times, easy to inspect/debug).
     * Cons:
     *   - Still O(n^2) overall because of ArrayList's linear-time
     *     insertion/removal in the middle.
     *   - More fiddly to implement correctly (index management for
     *     removing from the end and inserting at an arbitrary index)
     *     than the heap approach, for no asymptotic benefit over heap.
     *
     * When to use: Reasonable stepping stone in an interview narrative,
     * but I would not present this as my final answer -- the heap
     * approach strictly dominates it in both complexity and simplicity.
     */
    public int approach2_SortedListBinaryInsert(int[] stones) {
        List<Integer> sortedList = new ArrayList<>();
        for (int weight : stones) {
            sortedList.add(weight);
        }
        Collections.sort(sortedList); // O(n log n), ascending

        while (sortedList.size() > 1) {
            int largest = sortedList.remove(sortedList.size() - 1);
            int secondLargest = sortedList.remove(sortedList.size() - 1);
            int remainder = largest - secondLargest; // 0 if equal
            if (remainder != 0) {
                // binarySearch returns insertion point encoded as -(ip)-1 if absent
                int searchResult = Collections.binarySearch(sortedList, remainder);
                int insertIndex = (searchResult >= 0) ? searchResult : -(searchResult + 1);
                sortedList.add(insertIndex, remainder);
            }
        }

        return sortedList.isEmpty() ? 0 : sortedList.get(0);
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 3: Max-Heap / Priority Queue (STANDARD OPTIMAL APPROACH)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Push all stones into a max-heap. Repeatedly pop the two largest
     *   elements, compute the smash result, and push any surviving stone
     *   back onto the heap. A heap gives us O(log n) extraction of the
     *   current max and O(log n) insertion, which is exactly the pair of
     *   operations this problem needs, repeated ~n times.
     *
     * Data structure / paradigm: binary heap (PriorityQueue with reverse
     *   comparator to simulate a max-heap).
     *
     * Time Complexity: O(n log n)
     *   - Building the heap from n elements: O(n) (heapify) -- Java's
     *     PriorityQueue constructor from a collection does this in O(n).
     *   - Up to n-1 smash rounds, each doing 2 polls + up to 1 offer,
     *     each O(log n): O(n log n) total.
     * Space Complexity: O(n) for the heap.
     *
     * Pros:
     *   - Optimal asymptotic complexity for this problem.
     *   - Clean, idiomatic Java (java.util.PriorityQueue handles all the
     *     heap mechanics for us).
     *   - Directly mirrors the problem statement ("select the two largest"
     *     = "poll twice from a max-heap"), making it easy to explain and
     *     hard to get wrong.
     * Cons:
     *   - Slightly less intuitive than "just sort" for someone unfamiliar
     *     with heaps (minor -- any Google candidate should know heaps).
     *   - PriorityQueue in Java is a MIN-heap by default; must remember to
     *     supply Comparator.reverseOrder() or negate comparisons.
     *
     * When to use: This is my default choice for "repeatedly need the
     * current max/min while the collection changes" problems. It's the
     * one I'd write in the interview.
     */
    public int approach3_MaxHeap(int[] stones) {
        // Max-heap: reverse natural ordering so the largest weight is at the root.
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        for (int weight : stones) {
            maxHeap.offer(weight);
        }

        while (maxHeap.size() > 1) {
            int heaviest = maxHeap.poll();       // largest
            int secondHeaviest = maxHeap.poll();  // second largest
            int remainder = heaviest - secondHeaviest; // heaviest >= secondHeaviest always
            if (remainder > 0) {
                maxHeap.offer(remainder);
            }
            // remainder == 0 means they were equal; both stay destroyed.
        }

        return maxHeap.isEmpty() ? 0 : maxHeap.poll();
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 4: Bucket / Counting Sort (Exploits weight <= 1000 bound)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Since stones[i] is guaranteed to be in [1, 1000], we can use a
     *   fixed-size counting array `count[1..1000]` where count[w] tracks
     *   how many stones currently have weight w. To find the "current
     *   max," scan downward from index 1000 to find the highest occupied
     *   bucket -- this replaces heap operations with array scans, which
     *   are extremely fast in practice due to the tiny, bounded range.
     *
     * Data structure / paradigm: counting sort / bucket array (a form of
     *   hashing by direct-address table, since weight -> bucket index is
     *   a perfect hash here).
     *
     * Time Complexity: O(n + W) per "find max" scan in the worst case,
     *   where W = 1000 is the max possible weight, repeated up to n times
     *   => O(n * W) worst case = O(30 * 1000) = O(30,000) for this
     *   problem's constraints -- trivially fast, though asymptotically
     *   WORSE than the heap's O(n log n) if W were allowed to grow large
     *   relative to n. (In general this only pays off when W is small
     *   and bounded, as it is here.)
     * Space Complexity: O(W) = O(1000) = O(1) relative to input size
     *   (since W is a fixed constant per the problem constraints).
     *
     * Pros:
     *   - Very fast in practice for this problem's actual constraints
     *     (n <= 30, weight <= 1000); no comparisons, no heap overhead.
     *   - Simple array indexing; easy to trace and debug.
     *   - Great answer to "can you exploit the specific constraints given"
     *     -- shows attention to the bound on stones[i], which many
     *     candidates miss.
     * Cons:
     *   - Not general-purpose: relies entirely on the small, fixed weight
     *     bound. If the constraint were stones[i] <= 10^9, this approach
     *     becomes infeasible (huge or infinite array).
     *   - Downward linear scan for the max, worst case O(W) per
     *     extraction, is asymptotically worse than heap's O(log n) once
     *     W >> n.
     *
     * When to use: Great to MENTION as a constraint-driven optimization
     * ("given weight <= 1000, I could also bucket this"), demonstrating
     * range-awareness, but I'd still implement the heap as my primary
     * answer since it's general and equally simple.
     */
    public int approach4_BucketCounting(int[] stones) {
        final int MAX_WEIGHT = 1000;
        int[] bucketCounts = new int[MAX_WEIGHT + 1]; // index 0 unused (weights are >= 1)
        int stonesRemaining = 0;

        for (int weight : stones) {
            bucketCounts[weight]++;
            stonesRemaining++;
        }

        int currentMaxPointer = MAX_WEIGHT;

        while (stonesRemaining > 1) {
            // Find the current largest occupied bucket.
            while (bucketCounts[currentMaxPointer] == 0) {
                currentMaxPointer--;
            }
            bucketCounts[currentMaxPointer]--;
            stonesRemaining--;
            int heaviest = currentMaxPointer;

            // Find the second-largest occupied bucket (may be same bucket again).
            int scanPointer = currentMaxPointer;
            while (bucketCounts[scanPointer] == 0) {
                scanPointer--;
            }
            bucketCounts[scanPointer]--;
            stonesRemaining--;
            int secondHeaviest = scanPointer;

            int remainder = heaviest - secondHeaviest;
            if (remainder > 0) {
                bucketCounts[remainder]++;
                stonesRemaining++;
            }
            currentMaxPointer = Math.max(currentMaxPointer, remainder); // stay near the top
        }

        if (stonesRemaining == 0) {
            return 0;
        }
        // Find the single remaining stone's weight.
        for (int weight = MAX_WEIGHT; weight >= 1; weight--) {
            if (bucketCounts[weight] > 0) {
                return weight;
            }
        }
        return 0; // unreachable given stonesRemaining == 1, but keeps compiler happy
    }

    /*
     * ------------------------------------------------------------------------
     * Approach 5: TreeMap as a Sorted Multiset (Heap Alternative)
     * ------------------------------------------------------------------------
     * Core idea:
     *   Use a TreeMap<Integer, Integer> mapping weight -> count, giving us
     *   a balanced-BST-backed sorted multiset. `lastKey()` / `lastEntry()`
     *   gives the current max in O(log n); we decrement/remove counts and
     *   merge in the new remainder the same way.
     *
     * Data structure / paradigm: balanced BST (red-black tree) multiset.
     *
     * Time Complexity: O(n log n) -- same asymptotic class as the heap;
     *   each of the ~n rounds does O(log n) TreeMap operations.
     * Space Complexity: O(n) (or O(distinct weights), which is <= n).
     *
     * Pros:
     *   - Same complexity as the heap approach, but ALSO supports
     *     efficient "find the k-th largest," floor/ceiling queries, and
     *     range queries -- useful if the problem were extended (e.g.,
     *     "what if we sometimes need the two SMALLEST instead?").
     *   - Naturally deduplicates/counts equal weights, which can make
     *     reasoning about ties very explicit.
     * Cons:
     *   - More overhead (tree node pointers, autoboxing) than a binary
     *     heap for this specific, narrow use case.
     *   - Overkill: we don't need ordered traversal or range queries here,
     *     just repeated max-extraction, which a heap does with less code
     *     and less constant-factor overhead.
     *
     * When to use: I'd mention this as an alternative worth knowing, and
     * I'd reach for it if follow-up questions asked for additional
     * ordered-set operations. For the problem as stated, it's a lateral
     * move from the heap, not an improvement.
     */
    public int approach5_TreeMapMultiset(int[] stones) {
        TreeMap<Integer, Integer> weightCounts = new TreeMap<>();
        for (int weight : stones) {
            weightCounts.merge(weight, 1, Integer::sum);
        }

        int totalStones = stones.length;

        while (totalStones > 1) {
            int heaviest = removeOneFromTop(weightCounts);
            int secondHeaviest = removeOneFromTop(weightCounts);
            totalStones -= 2;

            int remainder = heaviest - secondHeaviest;
            if (remainder > 0) {
                weightCounts.merge(remainder, 1, Integer::sum);
                totalStones++;
            }
        }

        if (totalStones == 0 || weightCounts.isEmpty()) {
            return 0;
        }
        return weightCounts.lastKey();
    }

    // Helper for Approach 5: removes one occurrence of the current largest key.
    private int removeOneFromTop(TreeMap<Integer, Integer> weightCounts) {
        Map.Entry<Integer, Integer> topEntry = weightCounts.lastEntry();
        int weight = topEntry.getKey();
        int remainingCount = topEntry.getValue();
        if (remainingCount == 1) {
            weightCounts.remove(weight);
        } else {
            weightCounts.put(weight, remainingCount - 1);
        }
        return weight;
    }

    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * Approach                         | Time         | Space  | Best For                                   | Limitations
     * ---------------------------------|--------------|--------|--------------------------------------------|--------------------------------------------
     * 1. Brute Force (re-sort)         | O(n^2 log n) | O(n)   | Warm-up / correctness baseline              | Wasteful full re-sorts every round
     * 2. Sorted List + binary insert   | O(n^2)       | O(n)   | Middle-ground optimization narrative        | O(n) ArrayList shifts negate benefit; fiddly
     * 3. Max-Heap (PriorityQueue)      | O(n log n)   | O(n)   | General-purpose optimal solution            | Must remember reverseOrder() for max-heap
     * 4. Bucket / Counting (W<=1000)   | O(n*W)*      | O(W)   | Exploiting the tiny bounded weight range    | Not general; breaks if W grows large
     * 5. TreeMap multiset              | O(n log n)   | O(n)   | When extra ordered-set ops are also needed  | More overhead than heap for this narrow task
     *
     * * For this problem's actual constraints (n<=30, W<=1000), Approach 4's
     *   O(n*W) is numerically tiny (<=30,000 ops) and runs faster in
     *   practice than you'd guess from the big-O alone, but it does NOT
     *   scale the way Approach 3 does if W were unbounded.
     */

    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 3 (Max-Heap / PriorityQueue) as my primary
     * solution:
     *   - It achieves the best general-purpose asymptotic complexity,
     *     O(n log n), without relying on problem-specific bounds like the
     *     weight cap.
     *   - It's fast to code correctly under interview time pressure --
     *     Java's PriorityQueue handles all heap mechanics, so the risk of
     *     an off-by-one or pointer bug is much lower than Approaches 2 or 4.
     *   - It directly mirrors the problem's own language ("select the two
     *     largest" -> "poll the max-heap twice"), making it trivial to
     *     narrate while coding, which interviewers value.
     *   - It's the answer most interviewers expect for this exact
     *     LeetCode-style problem, and deviating without a strong reason
     *     (e.g., "the interviewer emphasizes the weight bound") isn't
     *     necessary.
     * I would also proactively MENTION Approach 4 (bucket counting) as a
     * constraint-aware optimization after landing the heap solution --
     * this demonstrates I noticed `stones[i] <= 1000` and considered its
     * implications, without spending main coding time implementing it.
     */

    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE -- OPTIMAL SOLUTION (PRODUCTION QUALITY)
     * ========================================================================
     */

    /**
     * Simulates repeatedly smashing the two heaviest stones together until
     * at most one stone remains, and returns that stone's weight (or 0 if
     * no stones remain).
     *
     * <p>Algorithm: max-heap simulation. We maintain a binary max-heap of
     * stone weights. On each iteration we extract the two heaviest stones,
     * compute the smash result, and (if nonzero) reinsert it. This
     * directly implements the problem's operation using the heap's
     * O(log n) extract-max and insert operations.
     *
     * @param stones array of stone weights; must be non-null. Each weight
     *               is expected to satisfy 1 &lt;= stones[i] &lt;= 1000
     *               per problem constraints, but this method does not
     *               strictly require that upper bound to function
     *               correctly -- only non-negativity is assumed.
     * @return the weight of the final remaining stone, or 0 if the input
     *         is empty or all stones fully annihilate each other.
     * @throws IllegalArgumentException if {@code stones} is null or
     *         contains a negative weight.
     */
    public int lastStoneWeight(int[] stones) {
        // Defensive validation: fail fast on malformed input rather than
        // producing a silently wrong answer.
        if (stones == null) {
            throw new IllegalArgumentException("stones array must not be null");
        }
        for (int weight : stones) {
            if (weight < 0) {
                throw new IllegalArgumentException(
                        "stone weights must be non-negative, found: " + weight);
            }
        }

        // Handle trivial sizes without touching a heap at all.
        if (stones.length == 0) {
            return 0;
        }
        if (stones.length == 1) {
            return stones[0];
        }

        // Max-heap: PriorityQueue is a min-heap by default in Java, so we
        // supply a reverse comparator to make the heaviest stone the root.
        // Pre-sizing the queue to stones.length avoids internal resizing.
        PriorityQueue<Integer> maxHeap =
                new PriorityQueue<>(stones.length, Collections.reverseOrder());
        for (int weight : stones) {
            maxHeap.offer(weight);
        }

        // Simulate smashing until 0 or 1 stones remain.
        while (maxHeap.size() > 1) {
            int heaviest = maxHeap.poll();       // O(log n) extract-max
            int secondHeaviest = maxHeap.poll();  // O(log n) extract-max

            // heaviest >= secondHeaviest is guaranteed by max-heap ordering.
            int remainder = heaviest - secondHeaviest;

            if (remainder > 0) {
                maxHeap.offer(remainder); // O(log n) insert
            }
            // remainder == 0: both stones fully destroyed, nothing to reinsert.
        }

        return maxHeap.isEmpty() ? 0 : maxHeap.poll();
    }

    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Tracing lastStoneWeight(stones) with stones = [2, 7, 4, 1, 8, 1]
     * using the optimal max-heap solution.
     *
     * Initial validation passes (no nulls, no negatives). length == 6, so
     * we proceed to build the heap.
     *
     * Step 0 (build heap): offer 2, 7, 4, 1, 8, 1
     *   Heap contents (conceptually, sorted desc for readability): [8,7,4,2,1,1]
     *   maxHeap.size() = 6
     *
     * Step 1: size() = 6 > 1, continue loop.
     *   poll() -> heaviest = 8            heap: [7,4,2,1,1]
     *   poll() -> secondHeaviest = 7      heap: [4,2,1,1]
     *   remainder = 8 - 7 = 1  (> 0, reinsert)
     *   offer(1)                          heap: [4,2,1,1,1]
     *
     * Step 2: size() = 5 > 1, continue loop.
     *   poll() -> heaviest = 4            heap: [2,1,1,1]
     *   poll() -> secondHeaviest = 2      heap: [1,1,1]
     *   remainder = 4 - 2 = 2  (> 0, reinsert)
     *   offer(2)                          heap: [2,1,1,1]
     *
     * Step 3: size() = 4 > 1, continue loop.
     *   poll() -> heaviest = 2            heap: [1,1,1]
     *   poll() -> secondHeaviest = 1      heap: [1,1]
     *   remainder = 2 - 1 = 1  (> 0, reinsert)
     *   offer(1)                          heap: [1,1,1]
     *
     * Step 4: size() = 3 > 1, continue loop.
     *   poll() -> heaviest = 1            heap: [1,1]
     *   poll() -> secondHeaviest = 1      heap: [1]
     *   remainder = 1 - 1 = 0  (equal, both destroyed, do NOT reinsert)
     *
     * Step 5: size() = 1, loop condition (size() > 1) is false, exit loop.
     *
     * Final: maxHeap is not empty, poll() -> 1.
     * Returned value: 1   (matches Example 1's expected result)
     */

    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - This problem is a pure simulation: at every step exactly one move
     *   is legal (smash the two current maxima), so there is no search
     *   space to optimize over -- the job is to execute that simulation as
     *   efficiently as possible.
     * - The max-heap approach (Approach 3 / the production solution) is
     *   the right general-purpose answer: O(n log n) time, O(n) space,
     *   minimal code, and it mirrors the problem statement almost
     *   line-for-line.
     * - The brute-force (Approach 1) and incremental-sort (Approach 2)
     *   approaches are useful narrative stepping stones but are strictly
     *   dominated by the heap in both simplicity and complexity.
     * - The bucket-counting approach (Approach 4) is a nice constraint-
     *   aware alternative given stones[i] <= 1000, but it trades general
     *   applicability for a practical (not asymptotic) speed win at this
     *   problem's specific, tiny bounds.
     * - Known assumptions/limitations of the final solution: weights are
     *   assumed non-negative (validated defensively); the method treats
     *   its input as read-only and does not mutate the caller's array;
     *   thread-safety is not a concern since all state (the heap) is
     *   local to each invocation.
     */

    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if stones[i] could be up to 10^9 instead of 1000?"
     *    -> The bucket-counting approach (Approach 4) becomes infeasible;
     *       the heap approach (Approach 3) is unaffected since it never
     *       depended on the weight bound -- another reason to prefer it
     *       as the primary/general solution.
     *
     * 2. "What if n could be up to 10^5 or 10^6?"
     *    -> Heap approach still scales fine: O(n log n) with n = 10^6 is
     *       about 2*10^7 operations, well within typical time limits.
     *       Approaches 1 and 2 (O(n^2 log n) / O(n^2)) would not scale.
     *
     * 3. "Instead of the two LARGEST, what if we always smashed the two
     *     SMALLEST stones together?"
     *    -> Trivial change: switch to a min-heap (PriorityQueue's default
     *       ordering in Java), everything else is identical.
     *
     * 4. "Can you return the full sequence of intermediate stone-array
     *     states, not just the final weight?"
     *    -> Would need to snapshot the heap's contents (as a sorted list)
     *       after each round and append it to a result list; increases
     *       space to O(n^2) in the worst case to store all snapshots.
     *
     * 5. "What if instead of exactly one final answer, we wanted to know
     *     the minimum possible final weight if we could choose to smash
     *     ANY two stones (not just the two largest)?"
     *    -> That's a different problem (closer to "Last Stone Weight II"
     *       / signed subset-sum): it becomes an optimization problem
     *       solvable via 0/1 knapsack-style DP over achievable sums, since
     *       now there IS a choice to search over.
     *
     * 6. "How would you handle this if stones arrived as a continuous
     *     stream (online), rather than as a fixed array upfront?"
     *    -> Maintain a persistent max-heap across calls; each new stone
     *       is offer()'d in O(log n), and periodically (or on-demand) we
     *       drain pairs via poll()/poll()/offer() as above. The heap-based
     *       design generalizes naturally to a streaming setting, unlike
     *       Approach 1, 2, or 4.
     */

    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Forgetting Java's PriorityQueue is a MIN-heap by default and
     *    submitting code that pulls the two SMALLEST stones instead of the
     *    largest -- always double check Collections.reverseOrder() (or an
     *    equivalent comparator) is applied for max-heap behavior.
     *
     * 2. Mishandling the equal-weight case: forgetting that when
     *    heaviest == secondHeaviest, remainder is 0 and NOTHING should be
     *    reinserted (a common bug is to always offer(remainder), which
     *    silently reinserts spurious 0-weight stones and corrupts the
     *    simulation).
     *
     * 3. Off-by-one on the loop / termination condition: using
     *    `while (maxHeap.size() >= 1)` instead of `> 1` causes a
     *    NoSuchElementException when trying to poll a second stone from a
     *    heap that only has one element left.
     *
     * 4. Not handling the n=0 or n=1 base cases explicitly before entering
     *    the main loop -- while the heap-based loop technically handles
     *    n=1 correctly on its own (loop body never executes), candidates
     *    under pressure sometimes write code that assumes at least two
     *    elements exist and crashes on single-element or empty input.
     *
     * 5. Confusing this problem with "Last Stone Weight II": candidates
     *    sometimes jump straight to a subset-sum/knapsack DP because they
     *    pattern-match on "stones" + "minimize/weight," without noticing
     *    that THIS version has no choice at all -- the move is always
     *    forced to be "the two current largest." Rushing to DP here wastes
     *    significant interview time on an unnecessary approach.
     */

    /*
     * ========================================================================
     * MAIN METHOD: manual verification harness across all approaches
     * ========================================================================
     */
    public static void main(String[] args) {
        LastStoneWeight solver = new LastStoneWeight();

        // Each test case: {input array, expected output}
        Object[][] testCases = {
            {new int[]{2, 7, 4, 1, 8, 1}, 1},   // normal case (Example 1)
            {new int[]{1, 1}, 0},               // edge case: full annihilation
            {new int[]{5}, 5},                  // boundary: single stone
            {new int[]{3, 3, 3}, 3},            // tie-breaking at the top
            {new int[]{1}, 1},                  // minimal input, min weight
            {new int[]{1000, 1000}, 0},         // boundary: max weight, equal pair
            {new int[]{1000, 1}, 999},          // boundary: max weight vs min weight
            {new int[]{}, 0},                   // defensive: empty array
            {new int[]{2, 2}, 0},               // simple equal pair
            {new int[]{10, 4, 2, 10}, 2},        // multiple rounds, repeated value
        };

        int totalTests = testCases.length;
        int passedTests = 0;

        for (Object[] testCase : testCases) {
            int[] input = (int[]) testCase[0];
            int expected = (int) testCase[1];

            int result1 = solver.approach1_BruteForceResort(input.clone());
            int result2 = solver.approach2_SortedListBinaryInsert(input.clone());
            int result3 = solver.approach3_MaxHeap(input.clone());
            int result4 = solver.approach4_BucketCounting(input.clone());
            int result5 = solver.approach5_TreeMapMultiset(input.clone());
            int resultOptimal = solver.lastStoneWeight(input.clone());

            boolean allMatch = result1 == expected && result2 == expected
                    && result3 == expected && result4 == expected
                    && result5 == expected && resultOptimal == expected;

            System.out.printf(
                "Input=%-28s Expected=%-4d Brute=%-4d SortedIns=%-4d Heap=%-4d Bucket=%-4d TreeMap=%-4d Optimal=%-4d %s%n",
                Arrays.toString(input), expected, result1, result2, result3, result4, result5,
                resultOptimal, allMatch ? "PASS" : "FAIL"
            );

            if (allMatch) {
                passedTests++;
            }
        }

        System.out.println();
        System.out.println(passedTests + "/" + totalTests + " test cases passed across all approaches.");
    }
}
