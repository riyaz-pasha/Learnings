import java.util.*;

/**
 * ============================================================================
 * GOOGLE-STYLE MOCK INTERVIEW: MAXIMUM TWIN SUM OF A LINKED LIST
 * (LeetCode 2130)
 * ============================================================================
 *
 * This file is structured as a complete interview walkthrough, from problem
 * restatement to a production-quality optimal solution with a full test
 * harness. Every major step is called out in its own labeled block comment.
 */
class MaximumTwinSum {

    /* ========================================================================
     * SECTION 1: RESTATE THE PROBLEM
     * ========================================================================
     *
     * We are given the head of a singly linked list that contains an EVEN
     * number of nodes, n. Index the nodes 0-based: node_0, node_1, ..., node_{n-1}.
     *
     * Node at index i is "twinned" with the node at index (n - 1 - i), for
     * every i in [0, n/2). This is exactly the pairing you'd get if you
     * folded the list in half: first node with last node, second node with
     * second-to-last node, and so on, meeting in the middle.
     *
     * The "twin sum" of a pair is val(node_i) + val(node_{n-1-i}).
     *
     * GOAL: Return the MAXIMUM twin sum over all n/2 pairs.
     *
     * Example: n = 4 -> pairs are (0,3) and (1,2).
     *
     * Inputs:
     *   - head: ListNode, head of a singly linked list.
     *   - Guaranteed (per problem statement) that the list length n is even
     *     and n >= 2 (LeetCode constraints: 2 <= n <= 10^5, even).
     *   - Node values are integers, typically 0 <= val <= 100 in the
     *     original constraints, but I will not hard-code that assumption
     *     into the algorithm itself.
     *
     * Output:
     *   - A single integer: the maximum twin sum.
     *
     * Assumptions I will state and confirm with the interviewer:
     *   - The list is singly linked (no "prev" pointers available for free).
     *   - n is always even and at least 2, so I don't need to handle odd-length
     *     lists or empty lists as valid inputs (but I will discuss what my
     *     code does if those constraints are violated).
     */

    /* ========================================================================
     * SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
     * ========================================================================
     *
     * 1. Q: Is the list guaranteed to have an even number of nodes, and is it
     *       guaranteed non-empty?
     *    A (assumed): Yes, per constraints n is even and n >= 2. I will add
     *       a defensive check but won't be required to handle odd n.
     *
     * 2. Q: What is the expected range of n (list length)? Do I need to worry
     *       about extremely large lists (memory pressure) or is O(n) extra
     *       space acceptable?
     *    A (assumed): n can be up to 10^5. O(n) extra space would pass, but
     *       the interviewer will likely want to see if I can do O(1) extra
     *       space (excluding the input list itself), since it's a natural
     *       follow-up for a "singly linked list, mirror pairing" problem.
     *
     * 3. Q: Can node values be negative, or only non-negative?
     *    A (assumed): Values fit in a 32-bit int and could theoretically be
     *       negative in a general version of this problem; I'll use `int`
     *       for values and sums (sum of two ints fits safely in int range
     *       for the stated constraints, but I'll note overflow considerations
     *       if values were near Integer.MAX_VALUE).
     *
     * 4. Q: Is the input list allowed to be modified (e.g., can I reverse
     *       half of it), or must the original structure be preserved after
     *       the function returns?
     *    A (assumed): For this problem, temporarily or permanently modifying
     *       the list is acceptable, since we only need to return an integer.
     *       I will mention that if preserving the original list is required,
     *       I'd re-reverse the second half before returning (small cost).
     *
     * 5. Q: Are duplicate twin sums possible, and does that affect the
     *       answer? (i.e., do we need the pair itself, or just the max sum?)
     *    A (assumed): We only need to return the numeric maximum twin sum,
     *       not which pair produced it, so duplicates don't need special
     *       handling.
     *
     * 6. Q: Should I assume this is called once, or repeatedly on a shared
     *       list in a concurrent/multi-threaded context?
     *    A (assumed): Single-threaded, single invocation. No concurrency
     *       concerns for this problem.
     *
     * 7. Q: Is a doubly linked list or array-backed list ever a valid input,
     *       or is it strictly a singly linked list?
     *    A (assumed): Strictly a singly linked ListNode structure, as defined
     *       by LeetCode's standard definition (val, next).
     *
     * 8. Q: What should happen if n is odd or head is null despite the
     *       stated guarantee (defensive coding)?
     *    A (assumed): Not required by constraints, but I will throw an
     *       IllegalArgumentException for null head to fail fast rather than
     *       silently returning a wrong answer.
     */

    /* ========================================================================
     * SECTION 3: EXAMPLES & EDGE CASES
     * ========================================================================
     *
     * Example 1 (Normal case):
     *   List: 5 -> 4 -> 2 -> 1   (n = 4)
     *   Pairs: (5,1) -> sum 6 ; (4,2) -> sum 6
     *   Max twin sum = 6
     *
     * Example 2 (Edge case: smallest possible list, n = 2):
     *   List: 4 -> 2   (n = 2)
     *   Pairs: (4,2) -> sum 6
     *   Max twin sum = 6
     *   This tests that the "middle" logic (fast/slow pointers, reversal)
     *   doesn't break when there is no "true middle" beyond the split point.
     *
     * Example 3 (Boundary / tie-breaking case: multiple pairs tie for max):
     *   List: 1 -> 100 -> 100 -> 1   (n = 4)
     *   Pairs: (1,1) -> sum 2 ; (100,100) -> sum 200
     *   Max twin sum = 200
     *   This confirms we just need the numeric max, not the pair identity,
     *   and that ties (both pairs being equal, or repeated values) don't
     *   require special tie-breaking logic -- we simply take Math.max across
     *   all pair sums.
     */

    // Standard singly linked list node definition (as provided by LeetCode).
    public static class ListNode {
        int val;
        ListNode next;
        ListNode() {}
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }
    }

    /* ========================================================================
     * SECTION 4 & 5: ALL POSSIBLE APPROACHES
     * ========================================================================
     *
     * Paradigm sweep -- which categories are relevant here?
     *
     *   - Brute force / naive           -> APPLICABLE (Approach 1)
     *   - Sorting-based                 -> NOT APPLICABLE: sorting destroys
     *         positional information, which is exactly what defines a twin
     *         pair (index i with index n-1-i). Sorting would give us the
     *         max value pairing, not the twin pairing.
     *   - Hashing-based                 -> NOT MEANINGFULLY APPLICABLE: a
     *         hash map keyed by index would just reinvent random access,
     *         which an array already gives us for free (see Approach 1).
     *         No lookup/frequency problem exists here to justify hashing.
     *   - Two pointer / sliding window   -> APPLICABLE (Approaches 2 and 3
     *         both use two-pointer techniques; Approach 3 is the two-pointer
     *         + reversal optimal solution).
     *   - Divide and conquer             -> NOT APPLICABLE: there's no
     *         recursive subproblem structure to combine; the pairing is a
     *         flat, single-pass structural property of the whole list.
     *   - Greedy                         -> NOT APPLICABLE: there's no
     *         sequence of locally-optimal choices to make; we must compute
     *         every pair sum since any pair could be the max.
     *   - Dynamic programming            -> NOT APPLICABLE: no overlapping
     *         subproblems or optimal substructure to exploit; every twin
     *         sum is independent of the others.
     *   - Tree / graph traversal          -> NOT APPLICABLE: input is a
     *         linear singly linked list, not a tree or graph.
     *   - Heap / priority queue           -> NOT APPLICABLE: we need a max
     *         over exactly n/2 known values we must compute anyway; a heap
     *         would add O(log n) overhead per insert with no benefit over
     *         a running Math.max.
     *   - Binary search                  -> NOT APPLICABLE: there's no
     *         sorted monotonic search space; we're not searching for a
     *         value or boundary condition.
     *   - Monotonic stack / deque        -> APPLICABLE, sort of (Approach 2
     *         uses a plain stack, not a monotonic one, but it's the closest
     *         "stack" paradigm fit worth covering as an alternative).
     *   - Trie / segment tree / advanced structures
     *                                    -> NOT APPLICABLE: no prefix,
     *         range-query, or string-key structure involved.
     *
     * So the approaches worth presenting are:
     *   Approach 1: Brute Force via Array/ArrayList Conversion
     *   Approach 2: Stack-Based Two-Pass Traversal
     *   Approach 3 (OPTIMAL): Fast/Slow Pointer Middle-Find + In-Place
     *               Second-Half Reversal + Simultaneous Two-Pointer Sum
     *   Approach 4: Recursion (Reach-the-End-First) -- included as a
     *               conceptual variant of Approach 2 using the call stack
     *               instead of an explicit Stack object.
     */

    /* ------------------------------------------------------------------
     * Approach 1: Brute Force via Array/ArrayList Conversion
     * ------------------------------------------------------------------
     * Core idea: Traverse the linked list once, copying every value into
     * a resizable array (ArrayList<Integer> or int[]). Once we have random
     * access, the twin-pair structure becomes trivial: use two indices,
     * left = 0 and right = n - 1, walk them toward the middle, and track
     * the maximum of arr[left] + arr[right].
     *
     * Data structure / paradigm: Array conversion + two-pointer scan.
     *
     * Time Complexity: O(n) -- one pass to build the array, one pass
     *   (n/2 iterations) to compute sums.
     * Space Complexity: O(n) -- the array holding all node values.
     *
     * Pros:
     *   - Extremely simple to write correctly under interview pressure.
     *   - Easy to explain and verify by hand.
     * Cons:
     *   - Uses O(n) auxiliary space, which a good interviewer will push
     *     back on given the problem's "linked list" framing invites an
     *     O(1) space follow-up.
     *
     * When to use: Good as an opening "let me get something correct first"
     *   solution, or in production code where clarity matters more than
     *   shaving O(n) space, and n is small/bounded.
     * When NOT to use: When the interviewer explicitly asks for O(1) extra
     *   space, or n is large enough that the extra array is a real concern.
     * ------------------------------------------------------------------ */
    public static int maxTwinSumBruteForce(ListNode head) {
        if (head == null) {
            throw new IllegalArgumentException("head must not be null");
        }

        // Step 1: copy all values into a dynamic array via one linear pass.
        List<Integer> values = new ArrayList<>();
        ListNode currentNode = head;
        while (currentNode != null) {
            values.add(currentNode.val);
            currentNode = currentNode.next;
        }

        // Step 2: two-pointer scan from both ends toward the middle.
        int leftIndex = 0;
        int rightIndex = values.size() - 1;
        int maxTwinSum = Integer.MIN_VALUE;

        while (leftIndex < rightIndex) {
            int twinSum = values.get(leftIndex) + values.get(rightIndex);
            maxTwinSum = Math.max(maxTwinSum, twinSum);
            leftIndex++;
            rightIndex--;
        }

        return maxTwinSum;
    }

    /* ------------------------------------------------------------------
     * Approach 2: Stack-Based Two-Pass Traversal
     * ------------------------------------------------------------------
     * Core idea: Push the values of the first half of the list onto a
     * stack while traversing it (using a fast/slow pointer to detect the
     * midpoint). Then continue traversing the second half; for each node,
     * pop from the stack and add to the current node's value, tracking
     * the max. Because a stack is LIFO, popping while walking the second
     * half naturally pairs node_i with node_{n-1-i}.
     *
     * Data structure / paradigm: Explicit stack (LIFO), fast/slow pointer
     *   midpoint detection.
     *
     * Time Complexity: O(n) -- one pass to find the middle while pushing,
     *   one pass over the second half while popping. Each node visited
     *   a constant number of times.
     * Space Complexity: O(n / 2) = O(n) -- the stack holds the first half
     *   of the values.
     *
     * Pros:
     *   - Conceptually clean "mirror via stack" trick, easy to reason about.
     *   - Avoids manually reversing the list (no pointer-rewiring bugs).
     * Cons:
     *   - Still O(n) space -- doesn't satisfy an O(1) space follow-up.
     *   - Slightly more bookkeeping than the plain array approach for the
     *     same asymptotic cost, so it's not clearly better than Approach 1
     *     except as a stepping stone toward explaining Approach 3.
     *
     * When to use: Useful as a bridge explanation toward the reversal
     *   technique, or if you want to avoid mutating the input list at all
     *   (Approach 3 temporarily mutates list structure).
     * When NOT to use: When O(1) space is required, or when list mutation
     *   is acceptable and Approach 3 is available.
     * ------------------------------------------------------------------ */
    public static int maxTwinSumStack(ListNode head) {
        if (head == null) {
            throw new IllegalArgumentException("head must not be null");
        }

        // Step 1: find the middle node using slow/fast pointers, pushing
        // every value we pass onto the stack along the way.
        Deque<Integer> firstHalfStack = new ArrayDeque<>();
        ListNode slowPointer = head;
        ListNode fastPointer = head;

        while (fastPointer != null && fastPointer.next != null) {
            firstHalfStack.push(slowPointer.val);
            slowPointer = slowPointer.next;
            fastPointer = fastPointer.next.next;
        }

        // At this point, slowPointer is at the first node of the second half.
        int maxTwinSum = Integer.MIN_VALUE;
        ListNode secondHalfNode = slowPointer;

        while (secondHalfNode != null) {
            int twinSum = secondHalfNode.val + firstHalfStack.pop();
            maxTwinSum = Math.max(maxTwinSum, twinSum);
            secondHalfNode = secondHalfNode.next;
        }

        return maxTwinSum;
    }

    /* ------------------------------------------------------------------
     * Approach 3 (OPTIMAL): Fast/Slow Midpoint + In-Place Reversal +
     *                        Simultaneous Two-Pointer Sum
     * ------------------------------------------------------------------
     * Core idea:
     *   1. Use the classic fast/slow pointer technique to find the
     *      midpoint of the list in one pass (fast moves 2 steps, slow
     *      moves 1 step; when fast reaches the end, slow is at the start
     *      of the second half).
     *   2. Reverse the second half of the list in place (standard
     *      iterative linked-list reversal), so its nodes now run from
     *      "last node" to "middle node".
     *   3. Walk the first half (from head) and the reversed second half
     *      (from its new head) simultaneously, adding corresponding
     *      values -- since the second half is reversed, walking both
     *      lists in lockstep naturally pairs node_i with node_{n-1-i}.
     *      Track the running maximum.
     *
     * Data structure / paradigm: Two-pointer technique (multiple flavors:
     *   fast/slow for midpoint, then parallel walk for summation) combined
     *   with in-place linked-list reversal. No auxiliary array/stack needed.
     *
     * Time Complexity: O(n) -- finding the midpoint is O(n/2), reversing
     *   the second half is O(n/2), and the final parallel walk is O(n/2).
     *   All linear passes sum to O(n).
     * Space Complexity: O(1) extra space -- we only use a constant number
     *   of pointers; we never allocate an array, stack, or recursion
     *   depth proportional to n.
     *
     * Pros:
     *   - Best possible space complexity: O(1) extra space.
     *   - Still O(n) time, so no asymptotic trade-off versus the other
     *     approaches -- this strictly dominates them in space.
     *   - Demonstrates strong linked-list fundamentals (a skill Google
     *     interviewers specifically like to see: midpoint-finding and
     *     in-place reversal are two of the most reusable linked-list
     *     primitives).
     * Cons:
     *   - Temporarily mutates the input list's structure (the second half
     *     is reversed during computation). If the original list must be
     *     preserved exactly as-is after the call returns, you must reverse
     *     it back before returning (an easy, cheap fix, shown below).
     *   - Slightly more intricate to code correctly under pressure than
     *     Approach 1 -- reversal off-by-one bugs are a classic trap.
     *
     * When to use: This is the production-quality, interview-optimal
     *   answer whenever O(1) space is desired or expected, which is the
     *   common expectation for this specific LeetCode-style problem.
     * When NOT to use: If the caller cannot tolerate even temporary
     *   mutation of the list (e.g., another thread might read it
     *   concurrently mid-call) -- in that narrow scenario, prefer
     *   Approach 1 or 2 instead.
     * ------------------------------------------------------------------ */
    public static int maxTwinSumOptimal(ListNode head) {
        if (head == null) {
            throw new IllegalArgumentException("head must not be null");
        }

        // --- Step 1: Find the middle of the list (start of second half). ---
        ListNode slowPointer = head;
        ListNode fastPointer = head;
        while (fastPointer != null && fastPointer.next != null) {
            slowPointer = slowPointer.next;
            fastPointer = fastPointer.next.next;
        }
        // slowPointer now points to the first node of the second half.

        // --- Step 2: Reverse the second half in place. ---
        ListNode reversedSecondHalfHead = reverseList(slowPointer);

        // --- Step 3: Walk first half and reversed second half in lockstep. ---
        int maxTwinSum = Integer.MIN_VALUE;
        ListNode firstHalfNode = head;
        ListNode secondHalfNode = reversedSecondHalfHead;

        // Both halves have exactly n/2 nodes, so we can stop when either
        // pointer runs out; we guard on secondHalfNode since it's the
        // freshly reversed segment.
        while (secondHalfNode != null) {
            int twinSum = firstHalfNode.val + secondHalfNode.val;
            maxTwinSum = Math.max(maxTwinSum, twinSum);
            firstHalfNode = firstHalfNode.next;
            secondHalfNode = secondHalfNode.next;
        }

        // NOTE: At this point the list's second half is still reversed.
        // If preserving the original list is required, uncomment the line
        // below to restore it before returning (adds O(n/2) time, O(1) space):
        //
        //   reverseList(reversedSecondHalfHead);

        return maxTwinSum;
    }

    // Standard iterative in-place linked-list reversal, O(n) time, O(1) space.
    // Returns the new head of the reversed segment.
    private static ListNode reverseList(ListNode segmentHead) {
        ListNode previousNode = null;
        ListNode currentNode = segmentHead;
        while (currentNode != null) {
            ListNode nextNode = currentNode.next; // save the rest of the list
            currentNode.next = previousNode;      // reverse the link
            previousNode = currentNode;           // advance previous
            currentNode = nextNode;               // advance current
        }
        return previousNode; // previousNode is the new head after reversal
    }

    /* ------------------------------------------------------------------
     * Approach 4: Recursion (Reach-the-End-First), a Call-Stack Variant
     *             of Approach 2
     * ------------------------------------------------------------------
     * Core idea: Recurse to the end of the list first (or to the midpoint,
     * using a separate fast pointer to detect it), then, as the recursion
     * unwinds, pair each "outer" node with the current "front" node using
     * a shared front-pointer reference. This mimics the stack approach,
     * but uses the implicit call stack instead of an explicit Deque.
     *
     * Data structure / paradigm: Recursion (implicit stack) + fast/slow
     *   pointer for midpoint detection.
     *
     * Time Complexity: O(n) -- each node is visited a constant number of
     *   times across the recursive descent and unwind.
     * Space Complexity: O(n) -- recursion depth is O(n/2), which dominates
     *   over the O(1) pointer bookkeeping, so this is NOT O(1) space; for
     *   n up to 10^5 this also risks a StackOverflowError in Java, which
     *   is a real practical concern worth raising in an interview.
     *
     * Pros:
     *   - Elegant, compact code; nice to mention as "the recursive twin".
     * Cons:
     *   - Same O(n) space as Approach 2, but with worse constants and a
     *     real risk of stack overflow on large inputs -- strictly inferior
     *     to Approach 3 in every practical dimension.
     *
     * When to use: Only to demonstrate breadth of thinking or if explicitly
     *   asked for a recursive solution.
     * When NOT to use: In any production or large-input context -- prefer
     *   Approach 3.
     * ------------------------------------------------------------------ */
    private static ListNode frontPointerForRecursion;
    private static int maxTwinSumRecursiveResult;

    public static int maxTwinSumRecursive(ListNode head) {
        if (head == null) {
            throw new IllegalArgumentException("head must not be null");
        }
        frontPointerForRecursion = head;
        maxTwinSumRecursiveResult = Integer.MIN_VALUE;
        recursiveHelper(head);
        return maxTwinSumRecursiveResult;
    }

    private static void recursiveHelper(ListNode currentNode) {
        if (currentNode == null) {
            return; // base case: reached past the end
        }
        recursiveHelper(currentNode.next); // descend to the tail first

        // On the way back up (unwinding), currentNode is effectively the
        // "back half" node walking backward, and frontPointerForRecursion
        // is the "front half" node walking forward -- together they trace
        // out every twin pair exactly once.
        int twinSum = currentNode.val + frontPointerForRecursion.val;
        maxTwinSumRecursiveResult = Math.max(maxTwinSumRecursiveResult, twinSum);
        frontPointerForRecursion = frontPointerForRecursion.next;
    }

    /* ========================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * ========================================================================
     *
     * | Approach                    | Time  | Space | Best For              | Limitations                          |
     * |------------------------------|-------|-------|-----------------------|---------------------------------------|
     * | 1. Brute Force (Array)       | O(n)  | O(n)  | Fast correct baseline | Not O(1) space; simplest to write     |
     * | 2. Stack-Based Two-Pass      | O(n)  | O(n)  | Bridge to Approach 3  | Same space cost as #1, more bookkeeping|
     * | 3. Reversal + Two-Pointer    | O(n)  | O(1)  | Production/interview  | Temporarily mutates input list        |
     * |    (OPTIMAL)                 |       |       | optimal answer        | (easily reversible if needed)         |
     * | 4. Recursive Unwind          | O(n)  | O(n)  | Showing alternative   | Risk of StackOverflowError on large n |
     * |                              |       | (call | recursive thinking    | strictly worse than #3                |
     * |                              |       | stack)|                       |                                        |
     */

    /* ========================================================================
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * ========================================================================
     *
     * I would present Approach 3 (Fast/Slow Midpoint + In-Place Reversal +
     * Two-Pointer Sum) as my final answer, but I would get there by first
     * verbally mentioning Approach 1 as the "obvious correct baseline" to
     * show I can produce a working solution immediately, then pivoting:
     *
     *   "This works in O(n) time but O(n) space. Since we're dealing with a
     *    singly linked list and a mirrored-pairing structure, I bet we can
     *    do this in O(1) extra space by finding the midpoint, reversing the
     *    second half in place, and then walking both halves together."
     *
     * Why this is the right call:
     *   - Clarity: the algorithm reuses two extremely common, well-understood
     *     linked-list primitives (fast/slow midpoint-finding and in-place
     *     reversal), so it's easy to explain and for the interviewer to follow.
     *   - Coding speed: both primitives are things I've likely written many
     *     times, so I can implement them quickly and correctly.
     *   - Interviewer expectations: for a "linked list twin pairing" problem,
     *     interviewers are specifically testing whether you reach for
     *     O(1)-space linked-list tricks rather than defaulting to "copy to
     *     an array." Presenting Approach 3 signals strong fundamentals.
     *   - Optimality: O(n) time is optimal (we must read every node at least
     *     once), and O(1) extra space is the best space bound achievable
     *     for this problem.
     */

    /* ========================================================================
     * SECTION 9: DEEP DIVE -- OPTIMAL SOLUTION (PRODUCTION QUALITY)
     * ========================================================================
     * See maxTwinSumOptimal(...) and reverseList(...) above for the full
     * implementation; both are already written with Javadoc-style inline
     * reasoning. Below is a restated, standalone "final" version consolidating
     * the production-quality contract (input validation, docs, named
     * constants) for clarity, matching what I'd write on a whiteboard/IDE
     * in the last few minutes of the interview.
     * ------------------------------------------------------------------ */

    /**
     * Computes the maximum twin sum of a singly linked list with an even
     * number of nodes.
     *
     * <p>Algorithm: locate the list's midpoint with the fast/slow pointer
     * technique, reverse the second half in place, then walk the first
     * half and the reversed second half in lockstep, tracking the maximum
     * pairwise sum.</p>
     *
     * @param head head of a singly linked list with an even, positive
     *             number of nodes.
     * @return the maximum twin sum across all twin pairs.
     * @throws IllegalArgumentException if head is null.
     */
    public static int maxTwinSumFinal(ListNode head) {
        final String NULL_HEAD_MESSAGE = "head must not be null; expected a non-empty list with an even node count";
        if (head == null) {
            throw new IllegalArgumentException(NULL_HEAD_MESSAGE);
        }

        ListNode midpointStart = findSecondHalfStart(head);
        ListNode reversedSecondHalf = reverseList(midpointStart);

        int runningMaxTwinSum = Integer.MIN_VALUE;
        ListNode firstHalfCursor = head;
        ListNode secondHalfCursor = reversedSecondHalf;

        while (secondHalfCursor != null) {
            int currentTwinSum = firstHalfCursor.val + secondHalfCursor.val;
            runningMaxTwinSum = Math.max(runningMaxTwinSum, currentTwinSum);
            firstHalfCursor = firstHalfCursor.next;
            secondHalfCursor = secondHalfCursor.next;
        }

        return runningMaxTwinSum;
    }

    // Named helper extracted purely for readability/testability.
    private static ListNode findSecondHalfStart(ListNode head) {
        ListNode slowPointer = head;
        ListNode fastPointer = head;
        while (fastPointer != null && fastPointer.next != null) {
            slowPointer = slowPointer.next;
            fastPointer = fastPointer.next.next;
        }
        return slowPointer;
    }

    /* ========================================================================
     * SECTION 10: DRY RUN / TRACE
     * ========================================================================
     *
     * Using maxTwinSumFinal on Example 1: List = 5 -> 4 -> 2 -> 1  (n = 4)
     *
     * --- Step 1: findSecondHalfStart ---
     *   Initial: slowPointer = node(5), fastPointer = node(5)
     *   Iteration 1: fastPointer.next (node(4)) is not null, so we advance:
     *       slowPointer = node(4)          (moved 1 step)
     *       fastPointer = node(2)          (moved 2 steps, to index 2)
     *   Check loop condition: fastPointer = node(2), fastPointer.next = node(1)
     *       (not null) -> loop again
     *   Iteration 2:
     *       slowPointer = node(2)          (moved to index 2)
     *       fastPointer = node(1).next = null  (moved 2 steps, past the end)
     *   Check loop condition: fastPointer is null -> stop.
     *   Result: slowPointer = node(2) (value 2), i.e., second half starts
     *           at index 2, which is correct: first half = [5,4], second
     *           half = [2,1].
     *
     * --- Step 2: reverseList(node(2)) ---
     *   Segment to reverse: 2 -> 1 -> null
     *   previousNode = null, currentNode = node(2)
     *   Iteration 1: nextNode = node(1); node(2).next = null;
     *                previousNode = node(2); currentNode = node(1)
     *   Iteration 2: nextNode = null; node(1).next = node(2);
     *                previousNode = node(1); currentNode = null
     *   Loop ends (currentNode == null). Return previousNode = node(1).
     *   Reversed second half: 1 -> 2 -> null
     *
     * --- Step 3: Parallel walk ---
     *   firstHalfCursor = node(5) [original head, first half untouched: 5 -> 4]
     *   secondHalfCursor = node(1) [reversed second half: 1 -> 2]
     *   runningMaxTwinSum = Integer.MIN_VALUE
     *
     *   Iteration 1:
     *     currentTwinSum = 5 + 1 = 6
     *     runningMaxTwinSum = max(MIN_VALUE, 6) = 6
     *     firstHalfCursor -> node(4); secondHalfCursor -> node(2)
     *
     *   Iteration 2:
     *     currentTwinSum = 4 + 2 = 6
     *     runningMaxTwinSum = max(6, 6) = 6
     *     firstHalfCursor -> null; secondHalfCursor -> null
     *
     *   Loop ends (secondHalfCursor == null).
     *
     * --- Final Result: 6 --- matches our hand-computed expectation from
     * Section 3 (pairs (5,1) and (4,2), both summing to 6).
     */

    /* ========================================================================
     * SECTION 11: CLOSING SUMMARY
     * ========================================================================
     *
     * - All four approaches share the same O(n) time complexity, since
     *   every node's value must be read at least once to compute any twin
     *   sum.
     * - They differ entirely in space complexity: Approaches 1, 2, and 4
     *   all use O(n) auxiliary space (array, stack, or call stack,
     *   respectively), while Approach 3 achieves O(1) extra space by
     *   exploiting in-place list reversal.
     * - Approach 3 is the recommended final answer: optimal in both time
     *   and space, and built from reusable, well-understood linked-list
     *   primitives.
     * - Known limitation/assumption of the final solution: it temporarily
     *   mutates the second half of the input list's structure during
     *   execution. This is acceptable per our clarifying-question
     *   assumptions (Q4), but if the original list's exact node-linkage
     *   must survive the call, an extra O(n/2) reversal pass at the end
     *   restores it at negligible additional cost.
     * - We also assumed n is even and head is non-null, matching the
     *   stated problem constraints; the code fails fast (throws) on a
     *   null head rather than silently misbehaving.
     */

    /* ========================================================================
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * ========================================================================
     *
     * 1. "What if n could be odd? How would your algorithm need to change,
     *     and what would the 'twin' of the middle element even mean?"
     * 2. "What if the list were doubly linked instead of singly linked --
     *     would that change your preferred approach, and could you avoid
     *     reversal entirely?" (Yes -- with a doubly linked list you could
     *     walk from both ends directly, no reversal needed.)
     * 3. "Can you solve this without ever mutating the input list, using
     *     only O(1) extra space?" (Generally no for a singly linked list --
     *     you need either extra space or temporary mutation, since you
     *     cannot walk backward natively.)
     * 4. "What if instead of the maximum twin sum, we wanted the count of
     *     twin pairs whose sum exceeds a given threshold, or the full list
     *     of twin sums?"
     * 5. "How would you handle extremely large lists that don't fit in
     *     memory (streaming / external storage), where you can't hold
     *     even O(n) space?"
     * 6. "Could you parallelize this computation across multiple threads
     *     or machines, and what would be the challenges given the linked
     *     structure?"
     */

    /* ========================================================================
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * ========================================================================
     *
     * 1. Off-by-one errors in the fast/slow pointer midpoint search --
     *    using `while (fastPointer.next != null && fastPointer.next.next != null)`
     *    instead of `while (fastPointer != null && fastPointer.next != null)`
     *    (or a similar variant) shifts the split point by one node and
     *    silently produces wrong pairings, especially in even-length lists.
     * 2. Forgetting to actually re-link the reversed segment's tail to
     *    null during reversal (a classic bug: forgetting `currentNode.next
     *    = previousNode` entirely, or reversing only conceptually without
     *    updating `.next` pointers), which can create a cycle or lose the
     *    rest of the list.
     * 3. Assuming the two halves after reversal have pointers ending at
     *    exactly the same time -- for lists with an even count this holds
     *    (both halves have n/2 nodes), but candidates sometimes write a
     *    loop condition based on `firstHalfCursor != null` instead of
     *    `secondHalfCursor != null` without verifying both halves are
     *    truly equal length, which becomes a real bug the moment odd-length
     *    inputs are considered.
     * 4. Not initializing the running maximum to Integer.MIN_VALUE (or
     *    the first computed twin sum) -- initializing to 0 silently gives
     *    a wrong answer whenever all twin sums happen to be negative.
     */

    /* ========================================================================
     * TEST HARNESS -- cross-validates all four approaches against each other
     * and against manually verified expected outputs.
     * ========================================================================
     */
    public static void main(String[] args) {
        System.out.println("Running cross-validation test harness...\n");

        // Test 1: Example 1 from Section 3.
        runTest("Example 1: [5,4,2,1]", buildList(5, 4, 2, 1), 6);

        // Test 2: Example 2 from Section 3 (smallest possible list).
        runTest("Example 2: [4,2]", buildList(4, 2), 6);

        // Test 3: Example 3 from Section 3 (tie/boundary case).
        runTest("Example 3: [1,100,100,1]", buildList(1, 100, 100, 1), 200);

        // Test 4: Larger list to sanity-check general correctness.
        runTest("Larger: [1,2,3,4,5,6]", buildList(1, 2, 3, 4, 5, 6), 7);

        // Test 5: All-equal values.
        runTest("All equal: [7,7,7,7]", buildList(7, 7, 7, 7), 14);

        // Test 6: Negative values, to confirm Integer.MIN_VALUE initialization
        // is correctly handled (guards against the "initialize to 0" trap).
        runTest("Negatives: [-5,-1,-3,-2]", buildList(-5, -1, -3, -2), -4);

        // Test 7: Randomized cross-validation across all four approaches.
        System.out.println("Running randomized cross-validation...");
        Random random = new Random(42);
        for (int trial = 0; trial < 1000; trial++) {
            int pairCount = 1 + random.nextInt(50); // n/2 in [1, 50]
            int nodeCount = pairCount * 2;
            int[] values = new int[nodeCount];
            for (int index = 0; index < nodeCount; index++) {
                values[index] = random.nextInt(201) - 100; // range [-100, 100]
            }

            ListNode listForBruteForce = buildList(values);
            ListNode listForStack = buildList(values);
            ListNode listForOptimal = buildList(values);
            ListNode listForRecursive = buildList(values);

            int resultBruteForce = maxTwinSumBruteForce(listForBruteForce);
            int resultStack = maxTwinSumStack(listForStack);
            int resultOptimal = maxTwinSumFinal(listForOptimal);
            int resultRecursive = maxTwinSumRecursive(listForRecursive);

            if (resultBruteForce != resultStack
                    || resultBruteForce != resultOptimal
                    || resultBruteForce != resultRecursive) {
                throw new AssertionError(
                    "Mismatch on trial " + trial + " with values " + Arrays.toString(values)
                    + " -> bruteForce=" + resultBruteForce
                    + " stack=" + resultStack
                    + " optimal=" + resultOptimal
                    + " recursive=" + resultRecursive);
            }
        }
        System.out.println("All 1000 randomized trials passed across all four approaches.\n");

        System.out.println("All tests passed.");
    }

    // Helper: build a linked list from a sequence of int values.
    private static ListNode buildList(int... values) {
        ListNode dummyHead = new ListNode(-1);
        ListNode tail = dummyHead;
        for (int value : values) {
            tail.next = new ListNode(value);
            tail = tail.next;
        }
        return dummyHead.next;
    }

    // Helper: run all four approaches on fresh copies of the same input
    // and assert they all agree with the expected value.
    private static void runTest(String testName, ListNode sampleList, int expected) {
        int[] originalValues = toArray(sampleList);

        int resultBruteForce = maxTwinSumBruteForce(buildList(originalValues));
        int resultStack = maxTwinSumStack(buildList(originalValues));
        int resultOptimal = maxTwinSumFinal(buildList(originalValues));
        int resultRecursive = maxTwinSumRecursive(buildList(originalValues));

        boolean allMatchExpected =
                resultBruteForce == expected
                && resultStack == expected
                && resultOptimal == expected
                && resultRecursive == expected;

        System.out.printf(
            "%-30s expected=%-5d bruteForce=%-5d stack=%-5d optimal=%-5d recursive=%-5d %s%n",
            testName, expected, resultBruteForce, resultStack, resultOptimal, resultRecursive,
            allMatchExpected ? "PASS" : "FAIL");

        if (!allMatchExpected) {
            throw new AssertionError("Test failed: " + testName);
        }
    }

    // Helper: convert a linked list back to an int[] (for building fresh
    // independent copies across the four approaches, since Approach 3
    // mutates its input).
    private static int[] toArray(ListNode head) {
        List<Integer> values = new ArrayList<>();
        ListNode currentNode = head;
        while (currentNode != null) {
            values.add(currentNode.val);
            currentNode = currentNode.next;
        }
        int[] result = new int[values.size()];
        for (int index = 0; index < result.length; index++) {
            result[index] = values.get(index);
        }
        return result;
    }
}
