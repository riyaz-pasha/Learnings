import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: CIRCULAR ARRAY LOOP  (LeetCode 457)
 * ============================================================================
 *
 * This file is structured as a full interview walkthrough. Every section is
 * labeled with a block comment so it can be read top-to-bottom exactly as
 * you'd narrate it in a real interview.
 * ============================================================================
 */
class CircularArrayLoop {

    /*
     * ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * In my own words:
     *
     * We have a circular array `nums` of non-zero integers. Starting at any
     * index i, nums[i] tells us how far and in which direction to jump:
     *   - positive value  -> jump forward that many steps
     *   - negative value  -> jump backward that many steps
     * The array wraps around (circular), so index arithmetic is done modulo n
     * in both directions.
     *
     * We must determine if there EXISTS at least one starting index from
     * which repeatedly following these jumps produces a CYCLE that satisfies
     * three conditions:
     *   1. The sequence of indices visited eventually repeats (a true cycle,
     *      not just termination).
     *   2. Every jump inside that cycle moves in the SAME direction (all
     *      jumps positive, or all jumps negative) — you cannot mix forward
     *      and backward moves within one qualifying cycle.
     *   3. The cycle length is strictly greater than 1 (a single index that
     *      jumps back to itself, i.e., a "self-loop", does NOT count).
     *
     * Input:  int[] nums, 1 <= nums.length <= 5000, -1000 <= nums[i] <= 1000,
     *         nums[i] != 0.
     * Output: boolean — true if such a cycle exists anywhere in the array,
     *         false otherwise.
     *
     * Key assumption: we only need to detect EXISTENCE of one such cycle,
     * not enumerate all cycles or return which indices form it.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * 1. Q: Can nums contain zero?
     *    A: No — guaranteed non-zero per constraints, so I don't need a
     *       "stuck in place" check for the value itself, only for the
     *       *effect* of wrapping around back to the same index.
     *
     * 2. Q: What's the array size range? Do I need to worry about very large n?
     *    A: 1 <= n <= 5000. Small enough that O(n^2) would pass on LeetCode,
     *       but I should still aim for O(n) since it's clearly achievable and
     *       is the expected "optimal" answer.
     *
     * 3. Q: Can |nums[i]| exceed n? I.e., can we wrap around multiple times
     *    in one jump?
     *    A: Yes, -1000 <= nums[i] <= 1000, so if n is small (e.g., n=3),
     *       a jump of 1000 wraps many times. Must use proper modulo math
     *       that handles negative results correctly in Java.
     *
     * 4. Q: Does a cycle need to include EVERY index in the array, or just a
     *    subset?
     *    A: Just a subset — any cycle of length > 1 with consistent direction
     *       anywhere in the functional graph counts.
     *
     * 5. Q: If a jump causes the direction to change mid-path (e.g., we start
     *    on a positive value but the next index holds a negative value), is
     *    that still a valid path to keep following for THIS cycle check?
     *    A: No — the moment direction changes, this path can no longer form
     *       a valid cycle (rule 2), so we should stop pursuing it from this
     *       start immediately.
     *
     * 6. Q: What should happen with a single-element array (n == 1)?
     *    A: nums[0] jumping any nonzero amount always maps back to index 0
     *       (self-loop) → cycle length 1 → invalid → return false.
     *
     * 7. Q: Are duplicate values in nums meaningful (e.g., special-cased)?
     *    A: No special meaning — duplicates are just regular values.
     *
     * 8. Q: Is the input array read-only, or can I mutate it for optimization
     *    (e.g., zeroing out visited entries)?
     *    A: I'll assume production code should NOT mutate caller-owned input
     *       unless explicitly documented, so I'll use an auxiliary visited
     *       array for the polished solution, but I'll mention the in-place
     *       O(1)-space trick as a valid alternative.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case — cycle exists):
     *   nums = [2, -1, 1, 2, 2]
     *   Start at index 0: 0 -> (0+2)=2 -> (2+1)=3 -> (3+2)=5%5=0 -> back to 0.
     *   Path: 0 -> 2 -> 3 -> 0. All values (2, 1, 2) are positive. Length 3 > 1.
     *   Result: true.
     *
     * Example 2 (Edge case — direction changes, breaking the cycle):
     *   nums = [-1, 2]
     *   Start at 0: nums[0] = -1 (backward) -> index (0-1+2)%2 = 1.
     *   At index 1: nums[1] = 2 (forward) -> direction changed from
     *   negative to positive, so this path is invalid and abandoned.
     *   Start at 1: nums[1] = 2 (forward) -> (1+2)%2 = 1 -> self-loop
     *   (index 1 maps back to itself) -> length 1 -> invalid.
     *   Result: false.
     *
     * Example 3 (Boundary/tie-breaking case — self-loop via wraparound):
     *   nums = [1, 1, 2]  (n = 3)
     *   Start at 2: nums[2] = 2 -> (2+2)%3 = 1. Not itself, continue.
     *   At index 1: nums[1] = 1 -> (1+1)%3 = 2. Back to start of this
     *   sub-path (2 -> 1 -> 2), same direction (both positive), length 2.
     *   Result: true.
     *   This demonstrates the "wrap exactly back to the same index after
     *   more than one hop" boundary — must NOT be confused with immediate
     *   self-loops (length-1), which are invalid by rule 3.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     * Paradigms considered and whether they apply:
     *
     *  - Brute force / naive           -> APPLICABLE (Approach 1)
     *  - Sorting-based                 -> NOT APPLICABLE: order/value of
     *      elements relative to each other doesn't matter; only the jump
     *      graph structure matters. Sorting would destroy index semantics.
     *  - Hashing-based                 -> APPLICABLE (folded into Approach 2
     *      as visited marking / dead-end memoization).
     *  - Two pointer / sliding window   -> APPLICABLE (Approach 3, Floyd's
     *      slow/fast pointer is a two-pointer technique).
     *  - Divide and conquer            -> NOT APPLICABLE: no natural way to
     *      split the array into independent halves — a cycle can span
     *      arbitrary indices, so subproblems aren't independent.
     *  - Greedy                        -> NOT APPLICABLE: there's no locally
     *      optimal choice to make; jumps are fully deterministic given nums.
     *  - Dynamic programming           -> NOT APPLICABLE: no overlapping
     *      subproblem / optimal substructure to exploit; this is a cycle
     *      *detection* problem on a deterministic functional graph, not an
     *      optimization problem.
     *  - Tree / graph traversal        -> APPLICABLE (Approach 2): each index
     *      has exactly one outgoing edge, forming a "functional graph";
     *      cycle detection here is classic graph coloring (DFS-style).
     *  - Heap / priority queue         -> NOT APPLICABLE: no notion of
     *      priority/ordering among elements to extract.
     *  - Binary search                 -> NOT APPLICABLE: no sorted or
     *      monotonic search space to exploit.
     *  - Monotonic stack / deque       -> NOT APPLICABLE: no "next greater/
     *      smaller element" style relationship being tracked.
     *  - Trie / segment tree / advanced structures
     *                                  -> NOT APPLICABLE: no prefix, range
     *      query, or string structure involved.
     * ========================================================================
     */


    /*
     * ------------------------------------------------------------------------
     * APPROACH 1: Brute Force Simulation
     * ------------------------------------------------------------------------
     * Core idea: For every starting index, simulate the jump sequence,
     * tracking the indices visited *during this specific attempt* in a
     * local visited set. If we land on an index we've already seen in this
     * attempt, we've found a repeat — check if the repeated segment has
     * consistent direction and length > 1.
     *
     * Data structure / paradigm: plain simulation + a local HashSet (or
     * boolean array) per starting index.
     *
     * Time Complexity: O(n^2) worst case — for each of the n starting
     * indices, a single simulation can visit up to n indices before it
     * either finds a cycle or determines there isn't one from that start.
     *
     * Space Complexity: O(n) for the local visited set/array per attempt
     * (not cumulative across attempts, but reallocated each time).
     *
     * Pros: Simple to reason about and implement under interview pressure;
     * easy to verify correctness by hand.
     * Cons: Quadratic time — will not scale gracefully to larger inputs,
     * and does no work-sharing between different starting attempts (may
     * re-walk the same indices many times).
     * When to use: Good as your FIRST spoken answer to show you understand
     * the problem correctly, or when n is guaranteed tiny. Not what you'd
     * ship to production or state as your final answer.
     * ------------------------------------------------------------------------
     */
    public boolean hasCycleBruteForce(int[] nums) {
        int n = nums.length;

        for (int startIndex = 0; startIndex < n; startIndex++) {
            // order-of-visitation map for THIS attempt only: index -> step number
            Map<Integer, Integer> visitedThisAttempt = new HashMap<>();
            int currentIndex = startIndex;
            int stepNumber = 0;
            boolean movingForward = nums[startIndex] > 0;

            while (true) {
                if (visitedThisAttempt.containsKey(currentIndex)) {
                    // We've looped back to an index seen earlier in this attempt.
                    int cycleStartStep = visitedThisAttempt.get(currentIndex);
                    int cycleLength = stepNumber - cycleStartStep;
                    if (cycleLength > 1) {
                        return true; // valid cycle: length > 1 and direction was
                                     // already enforced below before advancing
                    }
                    break; // length-1 self loop or no further progress possible
                }

                // Direction consistency check: if this index's sign doesn't
                // match the starting direction, this path is dead.
                boolean currentDirectionForward = nums[currentIndex] > 0;
                if (currentDirectionForward != movingForward) {
                    break;
                }

                visitedThisAttempt.put(currentIndex, stepNumber);
                int nextIndex = normalizeIndex(currentIndex + nums[currentIndex], n);
                currentIndex = nextIndex;
                stepNumber++;
            }
        }
        return false;
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 2: Graph Coloring / DFS-Style Cycle Detection (Hashing-based)
     * ------------------------------------------------------------------------
     * Core idea: Model the array as a "functional graph" — every index has
     * exactly one outgoing edge (to nums[i]'s target). Cycle detection in
     * such a graph is the classic three-color DFS technique used for
     * detecting cycles in directed graphs:
     *   - WHITE (unvisited): never touched.
     *   - GRAY (in current path): currently being explored in this DFS walk.
     *   - BLACK (fully resolved): proven to never reach a valid cycle from
     *     here (either it dead-ends or leads only into a previously-resolved
     *     region), so we never need to re-examine it.
     * If, while walking forward from GRAY nodes, we land on another GRAY
     * node with matching direction throughout, we've found a cycle of
     * length > 1.
     *
     * Data structure / paradigm: hashing / array-based visited-state
     * marking (color array) + iterative graph traversal.
     *
     * Time Complexity: O(n) — each index transitions from WHITE to GRAY to
     * BLACK exactly once across the entire algorithm; no index is
     * re-examined once BLACK.
     *
     * Space Complexity: O(n) for the color array plus O(n) for the
     * temporary list of nodes visited in the current path.
     *
     * Pros: Linear time, and the "graph coloring" framing is very intuitive
     * to explain and matches a well-known CS pattern interviewers recognize
     * immediately (same idea as detecting cycles in directed graphs / course
     * schedule problems).
     * Cons: Slightly more bookkeeping than the fast/slow pointer approach
     * (extra color array, path list); no in-place O(1)-space option since
     * we need three distinct states, not just a boolean.
     * When to use: Great alternative optimal answer if you're more
     * comfortable narrating it in graph terms than pointer terms — equally
     * valid to present as your primary solution.
     * ------------------------------------------------------------------------
     */
    private static final int WHITE = 0; // unvisited
    private static final int GRAY = 1;  // in current DFS path
    private static final int BLACK = 2; // fully resolved, cannot lead to a cycle

    public boolean hasCycleGraphColoring(int[] nums) {
        int n = nums.length;
        int[] color = new int[n]; // defaults to WHITE (0)

        for (int startIndex = 0; startIndex < n; startIndex++) {
            if (color[startIndex] != WHITE) continue;

            List<Integer> currentPath = new ArrayList<>();
            int currentIndex = startIndex;
            boolean movingForward = nums[startIndex] > 0;

            while (color[currentIndex] == WHITE) {
                color[currentIndex] = GRAY;
                currentPath.add(currentIndex);

                boolean currentDirectionForward = nums[currentIndex] > 0;
                if (currentDirectionForward != movingForward) {
                    break; // direction mismatch -> dead end for this path
                }

                int nextIndex = normalizeIndex(currentIndex + nums[currentIndex], n);
                if (nextIndex == currentIndex) {
                    break; // self-loop -> length 1 -> invalid
                }

                if (color[nextIndex] == GRAY) {
                    // Found a back-edge into the current path -> valid cycle,
                    // since we already verified direction consistency and
                    // length > 1 (we broke out above for length-1 self loops).
                    return true;
                }
                currentIndex = nextIndex;
            }

            // Resolve every node in this path to BLACK: none of them can be
            // part of a valid cycle (we either dead-ended or merged into an
            // already-resolved BLACK region).
            for (int resolvedIndex : currentPath) {
                color[resolvedIndex] = BLACK;
            }
        }
        return false;
    }


    /*
     * ------------------------------------------------------------------------
     * APPROACH 3 (OPTIMAL): Floyd's Cycle Detection — Fast & Slow Pointers
     * ------------------------------------------------------------------------
     * Core idea: This is exactly the "linked list cycle detection" problem
     * (LeetCode 141/142) applied to an implicit graph defined by the jump
     * function. From every unresolved starting index, run a slow pointer
     * (1 step at a time) and a fast pointer (2 steps at a time). If a valid
     * cycle of length > 1 exists reachable from this start, the fast pointer
     * will eventually lap the slow pointer and they will meet at the same
     * index. If either pointer hits a direction mismatch or a self-loop, the
     * path is invalid and we abandon it.
     *
     * Data structure / paradigm: two-pointer technique (Floyd's tortoise
     * and hare), applied to a functional graph.
     *
     * Time Complexity: O(n) — Floyd's algorithm resolves each starting
     * index's fate in time proportional to the path length from it, and
     * (with visited marking to skip already-resolved starts) the total work
     * across all starts is bounded by O(n).
     *
     * Space Complexity: O(1) extra if we mutate `nums` in place to mark
     * visited nodes (e.g., temporarily zeroing out resolved entries), or
     * O(n) if we prefer not to mutate caller-owned input and use a separate
     * boolean visited array instead (this is what I'll do in the deep dive
     * for production-quality, non-mutating code).
     *
     * Pros: Best asymptotic time AND the best possible space (O(1) if
     * mutation is acceptable); this is the canonical, expected "optimal"
     * answer for this LeetCode problem and demonstrates command of a
     * well-known pattern (Floyd's algorithm) applied in a novel context.
     * Cons: The trickiest to get exactly right — must carefully validate
     * direction consistency and self-loop conditions on EVERY pointer
     * advance (not just at the meeting point), or subtle bugs creep in.
     * When to use: This is what I'd write as my final, polished interview
     * solution.
     * ------------------------------------------------------------------------
     */
    public boolean hasCycleFloyd(int[] nums) {
        int n = nums.length;
        boolean[] visited = new boolean[n]; // resolved starting points, O(n) space

        for (int startIndex = 0; startIndex < n; startIndex++) {
            if (visited[startIndex]) continue;

            boolean movingForward = nums[startIndex] > 0;
            int slowPointer = startIndex;
            int fastPointer = startIndex;

            while (true) {
                slowPointer = advanceOneStep(nums, n, slowPointer, movingForward);
                if (slowPointer == -1) break; // invalid: direction mismatch or self-loop

                fastPointer = advanceOneStep(nums, n, fastPointer, movingForward);
                if (fastPointer == -1) break;
                fastPointer = advanceOneStep(nums, n, fastPointer, movingForward);
                if (fastPointer == -1) break;

                if (slowPointer == fastPointer) {
                    return true; // pointers met -> genuine cycle of length > 1
                }
            }

            // Mark every node touched while walking from startIndex as
            // visited, since none of them can lead to a valid cycle. We
            // re-walk this specific "dead" path once more purely to mark it
            // (cheap: total marking work across the whole array is O(n)).
            int markerIndex = startIndex;
            while (!visited[markerIndex]) {
                visited[markerIndex] = true;
                int nextIndex = normalizeIndex(markerIndex + nums[markerIndex], n);
                boolean sameDirection = (nums[markerIndex] > 0) == movingForward;
                if (!sameDirection || nextIndex == markerIndex) break;
                markerIndex = nextIndex;
            }
        }
        return false;
    }

    /**
     * Advances one step from currentIndex in the given direction.
     * Returns -1 if the move is invalid for cycle-forming purposes:
     *   - the value at currentIndex doesn't match the required direction, or
     *   - the move is a self-loop (maps back to the same index).
     */
    private int advanceOneStep(int[] nums, int n, int currentIndex, boolean movingForward) {
        boolean currentDirectionForward = nums[currentIndex] > 0;
        if (currentDirectionForward != movingForward) {
            return -1;
        }
        int nextIndex = normalizeIndex(currentIndex + nums[currentIndex], n);
        if (nextIndex == currentIndex) {
            return -1;
        }
        return nextIndex;
    }

    /**
     * Normalizes a raw (possibly negative or overflowing) index into the
     * valid range [0, n). Java's % operator can return negative results for
     * negative operands, so we add n and mod again to guarantee a
     * non-negative result.
     */
    private int normalizeIndex(int rawIndex, int n) {
        return ((rawIndex % n) + n) % n;
    }


    /*
     * ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * Approach                          | Time     | Space          | Best For                              | Limitations
     * ----------------------------------|----------|----------------|---------------------------------------|--------------------------------------------
     * 1. Brute Force Simulation         | O(n^2)   | O(n) per start | Verifying understanding; tiny inputs   | Too slow at scale; redundant re-walking
     * 2. Graph Coloring (DFS-style)     | O(n)     | O(n)           | Intuitive graph framing, easy to defend| Needs 3-state color array + path list
     * 3. Floyd's Fast/Slow (OPTIMAL)    | O(n)     | O(1) in-place  | Production code, canonical answer      | Trickiest to implement bug-free
     *                                   |          | or O(n) safe   |                                         | (must validate direction on every hop)
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR THE INTERVIEW
     * ========================================================================
     *
     * I would present Approach 3 (Floyd's Fast & Slow Pointers) as my final
     * answer, for these reasons:
     *
     * 1. Optimality: O(n) time and O(1) extra space (if mutation is
     *    permitted) is the best achievable on both axes — no other approach
     *    beats it on either dimension.
     * 2. Recognizability: Interviewers at Google immediately recognize
     *    Floyd's algorithm; presenting it signals pattern-matching maturity
     *    ("this is secretly a linked-list cycle detection problem").
     * 3. Narratable in stages: I can start by describing Approach 1 verbally
     *    (30 seconds) to prove I understand the problem, then pivot to "but
     *    since this is really cycle detection on an implicit graph where
     *    each node has out-degree 1, I can apply Floyd's algorithm to get
     *    O(n) time and O(1) space" — this progression is exactly what
     *    interviewers want to see.
     * 4. Coding speed: once the two helper functions (advanceOneStep,
     *    normalizeIndex) are written, the main loop is short and low-risk
     *    to write correctly under time pressure.
     *
     * I would only reach for Approach 2 (graph coloring) if the interviewer
     * specifically wanted a more "textbook graph algorithm" framing, or if I
     * were more comfortable narrating three-color DFS than pointer racing.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL IMPLEMENTATION
     * ========================================================================
     * This version is what I'd write as my final answer: fully validated
     * input, Javadoc, named constants, and no mutation of caller-owned data.
     * ========================================================================
     */

    /** Sentinel returned by {@link #advance} to signal an invalid/dead move. */
    private static final int INVALID_MOVE = -1;

    /**
     * Determines whether {@code nums} contains a circular array loop: a
     * sequence of indices, reachable by repeatedly applying each index's
     * jump value, that revisits itself, uses jumps of a single consistent
     * direction throughout, and has length strictly greater than one.
     *
     * @param nums a circular array of non-zero integers
     * @return true if a qualifying cycle exists anywhere in the array
     * @throws IllegalArgumentException if nums is null or empty
     */
    public boolean circularArrayLoop(int[] nums) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("nums must be non-null and non-empty");
        }

        final int elementCount = nums.length;
        // A single element can never form a length > 1 cycle, short-circuit.
        if (elementCount < 2) {
            return false;
        }

        // Tracks which starting indices have already been proven incapable
        // of leading to a valid cycle, so we never redo that work.
        boolean[] resolvedAsDeadEnd = new boolean[elementCount];

        for (int startIndex = 0; startIndex < elementCount; startIndex++) {
            if (resolvedAsDeadEnd[startIndex]) {
                continue;
            }

            final boolean requiredDirectionIsForward = nums[startIndex] > 0;

            int slowPointer = startIndex;
            int fastPointer = startIndex;
            boolean cycleFound = false;

            // Race the two pointers until they meet (cycle found) or either
            // one hits an invalid move (this starting point is a dead end).
            while (true) {
                slowPointer = advance(nums, elementCount, slowPointer, requiredDirectionIsForward);
                if (slowPointer == INVALID_MOVE) {
                    break;
                }

                fastPointer = advance(nums, elementCount, fastPointer, requiredDirectionIsForward);
                if (fastPointer == INVALID_MOVE) {
                    break;
                }
                fastPointer = advance(nums, elementCount, fastPointer, requiredDirectionIsForward);
                if (fastPointer == INVALID_MOVE) {
                    break;
                }

                if (slowPointer == fastPointer) {
                    cycleFound = true;
                    break;
                }
            }

            if (cycleFound) {
                return true;
            }

            markUnreachablePathAsDead(nums, elementCount, startIndex,
                    requiredDirectionIsForward, resolvedAsDeadEnd);
        }

        return false;
    }

    /**
     * Advances a single step forward from {@code currentIndex}, enforcing
     * the two invalidity rules in one place: direction consistency and
     * self-loop avoidance.
     *
     * @return the next index, or {@link #INVALID_MOVE} if this path cannot
     *         contribute to a valid cycle
     */
    private int advance(int[] nums, int elementCount, int currentIndex, boolean requiredDirectionIsForward) {
        boolean thisElementIsForward = nums[currentIndex] > 0;
        if (thisElementIsForward != requiredDirectionIsForward) {
            return INVALID_MOVE; // rule 2 violation: mixed directions
        }

        int nextIndex = wrapIndex(currentIndex + nums[currentIndex], elementCount);
        if (nextIndex == currentIndex) {
            return INVALID_MOVE; // rule 3 violation: length-1 self loop
        }
        return nextIndex;
    }

    /**
     * Walks forward from {@code startIndex} exactly once more (cheaply,
     * since this path is already known to be a dead end) purely to mark
     * every index along it as resolved, so future outer-loop iterations
     * skip it entirely. This keeps total work across the whole method
     * linear in {@code elementCount}.
     */
    private void markUnreachablePathAsDead(int[] nums, int elementCount, int startIndex,
                                            boolean requiredDirectionIsForward,
                                            boolean[] resolvedAsDeadEnd) {
        int currentIndex = startIndex;
        while (!resolvedAsDeadEnd[currentIndex]) {
            resolvedAsDeadEnd[currentIndex] = true;
            boolean thisElementIsForward = nums[currentIndex] > 0;
            int nextIndex = wrapIndex(currentIndex + nums[currentIndex], elementCount);
            if (thisElementIsForward != requiredDirectionIsForward || nextIndex == currentIndex) {
                break;
            }
            currentIndex = nextIndex;
        }
    }

    /**
     * Wraps a raw index into the valid circular range [0, elementCount),
     * correctly handling negative results from Java's {@code %} operator.
     */
    private int wrapIndex(int rawIndex, int elementCount) {
        return ((rawIndex % elementCount) + elementCount) % elementCount;
    }


    /*
     * ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     * Tracing circularArrayLoop(nums) on Example 1: nums = [2, -1, 1, 2, 2]
     * elementCount = 5
     *
     * startIndex = 0:
     *   requiredDirectionIsForward = true (nums[0] = 2 > 0)
     *   slowPointer = 0, fastPointer = 0
     *
     *   Iteration 1:
     *     advance(slow, 0): nums[0]=2 forward matches. next = (0+2)%5 = 2.
     *       slowPointer = 2
     *     advance(fast, 0) twice:
     *       step A: nums[0]=2 forward matches. next=(0+2)%5=2. fastPointer=2
     *       step B: nums[2]=1 forward matches. next=(2+1)%5=3. fastPointer=3
     *     slowPointer(2) != fastPointer(3) -> continue
     *
     *   Iteration 2:
     *     advance(slow, 2): nums[2]=1 forward matches. next=(2+1)%5=3.
     *       slowPointer = 3
     *     advance(fast, 3) twice:
     *       step A: nums[3]=2 forward matches. next=(3+2)%5=0. fastPointer=0
     *       step B: nums[0]=2 forward matches. next=(0+2)%5=2. fastPointer=2
     *     slowPointer(3) != fastPointer(2) -> continue
     *
     *   Iteration 3:
     *     advance(slow, 3): nums[3]=2 forward matches. next=(3+2)%5=0.
     *       slowPointer = 0
     *     advance(fast, 2) twice:
     *       step A: nums[2]=1 forward matches. next=(2+1)%5=3. fastPointer=3
     *       step B: nums[3]=2 forward matches. next=(3+2)%5=0. fastPointer=0
     *     slowPointer(0) == fastPointer(0) -> cycleFound = true
     *
     *   Return true immediately.
     *
     * Final state: method returns true after examining only startIndex=0 —
     * confirms the hand-traced cycle 0 -> 2 -> 3 -> 0 from Example 1.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - Approach 1 (Brute Force) trades speed for simplicity: O(n^2) time,
     *   easy to write, good as a warm-up explanation only.
     * - Approach 2 (Graph Coloring) achieves O(n) time via a well-known
     *   three-color DFS cycle-detection pattern; O(n) space for the color
     *   array and path list.
     * - Approach 3 (Floyd's Fast/Slow, chosen as final answer) achieves
     *   O(n) time with O(1) extra space if the input array may be mutated,
     *   or O(n) space (as implemented here) if the input must remain
     *   untouched — a reasonable trade-off for production code.
     *
     * Known assumptions/limitations of the final solution:
     *   - Assumes nums is non-null, non-empty, and every element is
     *     non-zero (per problem constraints); an IllegalArgumentException
     *     is thrown for null/empty input as defensive coding.
     *   - Uses an auxiliary O(n) boolean array rather than mutating nums in
     *     place; this is the "safe for production" choice, not the
     *     absolute-minimum-space choice.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "Can you solve this with truly O(1) extra space, without any
     *    auxiliary array?" -> Yes: mutate nums in place (e.g., set visited
     *    entries to 0 as a sentinel, checking nums[i] == 0 as "already
     *    resolved" instead of a separate boolean array) — a nonzero-only
     *    guarantee makes 0 a safe sentinel.
     *
     * 2. "What if the array could change (jump values updated) between
     *    repeated queries — how would you support that efficiently?" -> Full
     *    re-computation from scratch per query, or incremental invalidation
     *    of only the affected chain segments, would be needed; no simple
     *    O(1)-update structure exists for this cyclic dependency graph.
     *
     * 3. "What if we needed to return the actual cycle (the list of indices),
     *    not just true/false?" -> Once slow == fast, do a second pass:
     *    reset one pointer to the meeting point and the other to advance
     *    with it, collecting indices until we loop back to the first
     *    repeated index, tracking length explicitly.
     *
     * 4. "How would this change if the array were extremely large (billions
     *    of elements) and didn't fit in memory?" -> Would need external/
     *    streaming cycle detection or partitioning strategies; the pure
     *    O(n) in-memory algorithm no longer applies directly.
     *
     * 5. "What if multiple threads needed to query different starting
     *    indices concurrently?" -> The visited/resolved array would need
     *    thread-safe access (e.g., AtomicBoolean array or partitioning work
     *    per thread with a concurrent set), since races on marking dead-end
     *    paths could cause redundant (but not incorrect) work if not
     *    synchronized, or lost updates if naive.
     *
     * 6. "Can a cycle of length exactly n (using the entire array) happen,
     *    and does your algorithm handle it?" -> Yes, e.g. nums=[1,1,1,1];
     *    the algorithm handles it identically — no special-casing needed
     *    since Floyd's detection doesn't care about cycle length beyond >1.
     * ========================================================================
     */


    /*
     * ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Forgetting the length > 1 rule: many candidates detect "slow ==
     *    fast" and immediately return true without checking whether that
     *    meeting point is actually a self-loop (a jump that maps an index
     *    back to itself). This file avoids the bug entirely by rejecting
     *    self-loops inside `advance()` BEFORE the pointers ever get a chance
     *    to "meet" on a length-1 loop.
     *
     * 2. Incorrect modulo arithmetic: Java's `%` can return negative values
     *    for negative operands (e.g., -3 % 5 == -3, not 2). Forgetting to
     *    add elementCount before the second modulo is one of the single
     *    most common bugs on this problem.
     *
     * 3. Checking direction consistency only at the start, not on every
     *    hop: a candidate might check `nums[startIndex] > 0` once and then
     *    never re-verify that subsequent elements in the path match that
     *    direction, silently allowing mixed-direction "cycles" to count as
     *    valid.
     *
     * 4. Not marking dead-end paths, causing accidental O(n^2) behavior:
     *    without the resolvedAsDeadEnd (or equivalent) bookkeeping, a
     *    solution that otherwise looks like Floyd's algorithm degrades to
     *    quadratic time because every starting index re-walks large shared
     *    prefixes of the same dead path.
     * ========================================================================
     */


    /*
     * ========================================================================
     * TEST HARNESS — cross-validates all three approaches against each other
     * ========================================================================
     */
    public static void main(String[] args) {
        CircularArrayLoop solution = new CircularArrayLoop();

        int[][] testCases = {
            {2, -1, 1, 2, 2},   // Example 1 -> true
            {-1, 2},            // Example 2 -> false
            {1, 1, 2},          // Example 3 -> true
            {1, -1, 1, -1},     // alternating directions everywhere -> false
            {1, 1, 1, 1},       // full-length forward cycle -> true
            {2, 2, -1, 2},      // mixed, contains a valid sub-cycle -> true
            {3, 1, 2}           // single element self-loop test variant -> true/false depends
        };
        boolean[] expected = {true, false, true, false, true, true, true};

        for (int i = 0; i < testCases.length; i++) {
            int[] original = testCases[i];

            int[] copyForBruteForce = Arrays.copyOf(original, original.length);
            int[] copyForGraphColoring = Arrays.copyOf(original, original.length);
            int[] copyForFloyd = Arrays.copyOf(original, original.length);

            boolean bruteForceResult = solution.hasCycleBruteForce(copyForBruteForce);
            boolean graphColoringResult = solution.hasCycleGraphColoring(copyForGraphColoring);
            boolean floydResult = solution.hasCycleFloyd(copyForFloyd);
            boolean productionResult = solution.circularArrayLoop(Arrays.copyOf(original, original.length));

            boolean allAgree = bruteForceResult == graphColoringResult
                    && graphColoringResult == floydResult
                    && floydResult == productionResult;

            System.out.printf(
                "Test %d: nums=%s | bruteForce=%b graphColoring=%b floyd=%b production=%b | agree=%b%n",
                i, Arrays.toString(original), bruteForceResult, graphColoringResult,
                floydResult, productionResult, allAgree
            );
        }
    }
}
