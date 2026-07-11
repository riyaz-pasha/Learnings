/*
====================================================================================================
 GOOGLE-STYLE MOCK INTERVIEW — FULL WALKTHROUGH
 Problem: Split a Circular Linked List into Two Halves
 File: SplitCircularLinkedList.java
 Java version target: 21+ (var, enhanced switch, etc. used where natural)
====================================================================================================
*/

import java.util.ArrayList;
import java.util.List;

/*
====================================================================================================
 SECTION 1: RESTATE THE PROBLEM
====================================================================================================
 In my own words:

   We are given a CIRCULAR singly linked list of positive integers — meaning the last node's
   `next` pointer points back to the head instead of null. We need to split it into exactly two
   circular linked lists:

     - The FIRST list contains the first ceil(n / 2) nodes, in original order, and must itself
       be circular (its last node points back to its own head).
     - The SECOND list contains the remaining floor(n / 2) nodes, in original order, and must
       also be circular.

   We return a 2-element array: [headOfFirstHalf, headOfSecondHalf].

 Key constraints / inputs / outputs / assumptions:
   - Input: head node of a circular singly linked list, list.length = n >= 1.
   - All values are positive integers (value itself isn't really used in the split logic —
     it's just node payload).
   - Output: Node[] of size 2 — new circular heads for each half.
   - "Circular" means we cannot use `null` as a natural traversal-stop condition; we must stop
     when we come back around to a known node (usually the head).
   - Splitting must be done by REWIRING existing nodes (standard expectation for linked-list
     problems) rather than copying values into brand-new nodes, unless told otherwise.
   - If n == 1, the single node must point to itself in BOTH halves? No — with n=1, first half
     gets ceil(1/2)=1 node, second half gets 0 nodes. We need to clarify what "empty circular
     list" means (see clarifying questions).
====================================================================================================
*/

/*
====================================================================================================
 SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
====================================================================================================
 1. Q: What is the range of n (number of nodes)? Could it be 0?
    A (assumed): n >= 1. A "list" implies at least one node exists (per constraints).

 2. Q: When n is odd, does the FIRST half get the extra node, or the second?
    A (assumed): First half gets the extra node — explicitly given as ceil(n/2) in the prompt.

 3. Q: If n == 1, second half has 0 nodes — what should answer[1] be? null, or a self-pointing
       node, or an empty sentinel?
    A (assumed): answer[1] = null when the second half is empty. This is the most common
       convention (matches LeetCode's official spec for this exact problem).

 4. Q: Should the original list be mutated in place (rewiring `next` pointers), or must we
       create brand-new nodes and leave the input untouched?
    A (assumed): In-place rewiring is expected — it's O(1) extra space and is the standard
       expectation for linked-list interview problems unless immutability is explicitly required.

 5. Q: Are node values guaranteed unique? Do we need to worry about duplicate values anywhere
       in the logic?
    A (assumed): Values may repeat; the algorithm never depends on value uniqueness or ordering
       by value — only structural position matters.

 6. Q: Is the list guaranteed to actually be circular (last node's next == head), or could it
       be a "corrupted" circular list (e.g., pointing to some middle node instead of head)?
    A (assumed): Guaranteed properly circular: traversal from head returns to head after
       exactly n steps, with no other cycle shortcuts.

 7. Q: Do we need to worry about concurrent modification (other threads mutating the list while
       we split it)?
    A (assumed): No concurrency concerns — single-threaded, no external mutation during the call.

 8. Q: Is there a preferred trade-off between time and space, or any hint about expected
       complexity (e.g., interviewer wants O(1) extra space)?
    A (assumed): Optimal expected solution is O(n) time, O(1) extra space (i.e., no auxiliary
       array/list of nodes) — but I will discuss an O(n) space brute-force too, since discussing
       trade-offs is expected.
====================================================================================================
*/

/*
====================================================================================================
 SECTION 3: EXAMPLES & EDGE CASES
====================================================================================================
 Example A (Normal case, even n):
   Input circular list: 1 -> 2 -> 3 -> 4 -> (back to 1),  n = 4
   ceil(4/2) = 2 nodes in first half.
   First half:  1 -> 2 -> (back to 1)
   Second half: 3 -> 4 -> (back to 3)
   answer = [Node(1), Node(3)]

 Example B (Edge case, n == 1):
   Input circular list: 5 -> (back to 5), n = 1
   ceil(1/2) = 1 node in first half, 0 nodes in second half.
   First half:  5 -> (back to 5)   (unchanged, self-loop)
   Second half: null (empty — per our clarifying assumption)
   answer = [Node(5), null]

 Example C (Boundary / tie-breaking case, odd n where "extra" node placement matters):
   Input circular list: 1 -> 2 -> 3 -> (back to 1), n = 3
   ceil(3/2) = 2 nodes in first half (the "tie" is broken toward the first half, per spec).
   First half:  1 -> 2 -> (back to 1)
   Second half: 3 -> (back to 3)
   answer = [Node(1), Node(3)]

 These three examples cover: even split, a degenerate single-node list, and an odd split where
 the ceiling rule determines which half is bigger.
====================================================================================================
*/

/*
====================================================================================================
 SECTION 4 & 5: ALL POSSIBLE SOLUTIONS (by paradigm)
====================================================================================================
 Paradigm applicability scan:
   - Sorting-based:              NOT APPLICABLE — order is fixed by original structure; sorting
                                  would destroy required ordering semantics.
   - Hashing-based:               NOT APPLICABLE — there's no lookup/membership problem here;
                                  values aren't used as keys anywhere.
   - Two pointer / sliding window: APPLICABLE — this is exactly the tool for finding the
                                  midpoint in one pass (Floyd's slow/fast).
   - Divide and conquer:          NOT APPLICABLE — no recursive sub-problem structure; this is
                                  a single linear split, not a recursive combine problem.
   - Greedy:                      NOT APPLICABLE — there's no sequence of locally-optimal
                                  choices to make; the split point is deterministic (n/2).
   - Dynamic programming:         NOT APPLICABLE — no overlapping subproblems or optimal
                                  substructure; nothing is being optimized over choices.
   - Tree / graph traversal:      Marginally related (a linked list IS a degenerate graph), but
                                  no BFS/DFS decision-making is needed beyond simple traversal,
                                  so it doesn't add a distinct "approach."
   - Heap / priority queue:       NOT APPLICABLE — no ordering/priority extraction needed.
   - Binary search:               NOT APPLICABLE — we don't have random access (no array index),
                                  so binary search over positions isn't usable on a linked list.
   - Monotonic stack / deque:     NOT APPLICABLE — no notion of monotonic ordering/window here.
   - Trie / segment tree:         NOT APPLICABLE — no prefix/range query being solved.

 Given the above, the MEANINGFUL approaches are:
   Approach 1: Brute Force via Auxiliary List (extra space)
   Approach 2: Two-Pass Counting + Traversal (O(1) space, two logical passes)
   Approach 3 (OPTIMAL): One-Pass Fast/Slow Pointer (Floyd-style), O(1) space
====================================================================================================
*/

/** Singly linked list node used throughout this file. Package-private, not public,
 *  since only one public top-level type is allowed per file. */
class Node {
    int val;
    Node next;
    Node(int val) { this.val = val; }
}

class CircularLinkedListSplitter {

    /*
    ------------------------------------------------------------------------------------------
     APPROACH 1: Brute Force via Auxiliary List
    ------------------------------------------------------------------------------------------
     Core idea:
       Traverse the circular list once, copying references (not values) into an ArrayList.
       Since we now have "random access" via array indices, splitting into two halves and
       re-wiring `next` pointers (and closing each half into its own circle) becomes trivial
       index arithmetic.

     Data structure / paradigm: Array/List used purely as scratch space to gain random access.

     Time complexity: O(n) — one traversal to collect nodes, one pass to rewire pointers.
     Space complexity: O(n) — the auxiliary ArrayList holds references to all n nodes.

     Pros:
       - Very easy to reason about and get correct under interview pressure — index math is
         far less error-prone than juggling raw pointers while circular.
       - Naturally handles the "how do I know when to stop" circularity problem, since we stop
         collecting as soon as we've walked n nodes (tracked by list size), not by pointer
         comparison after the first node.

     Cons:
       - O(n) extra space is wasteful; this is a linked-list problem specifically because O(1)
         space in-place solutions exist and are expected once you've shown you understand the
         structure.
       - Doesn't demonstrate mastery of two-pointer / pointer-rewiring techniques that this
         question is really testing.

     When to use in practice:
       - Good as a first "get something correct on the whiteboard" fallback, or under extreme
         time pressure, but NOT what you'd present as your final answer in a Google interview.
    ------------------------------------------------------------------------------------------
    */
    public Node[] splitBruteForce(Node head) {
        if (head == null) {
            return new Node[] { null, null };
        }

        // Collect all node references by walking the circle exactly once.
        List<Node> allNodes = new ArrayList<>();
        Node current = head;
        do {
            allNodes.add(current);
            current = current.next;
        } while (current != head);

        int totalCount = allNodes.size();
        int firstHalfCount = (totalCount + 1) / 2; // ceil(n / 2)

        // Rewire the first half into its own circle.
        Node firstHalfHead = allNodes.get(0);
        Node firstHalfTail = allNodes.get(firstHalfCount - 1);
        firstHalfTail.next = firstHalfHead;

        // Handle the (possibly empty) second half.
        Node secondHalfHead = null;
        if (firstHalfCount < totalCount) {
            secondHalfHead = allNodes.get(firstHalfCount);
            Node secondHalfTail = allNodes.get(totalCount - 1);
            secondHalfTail.next = secondHalfHead;
        }

        return new Node[] { firstHalfHead, secondHalfHead };
    }

    /*
    ------------------------------------------------------------------------------------------
     APPROACH 2: Two-Pass Counting + Traversal
    ------------------------------------------------------------------------------------------
     Core idea:
       First pass: walk the circle once just to COUNT the number of nodes, n.
       Compute firstHalfCount = ceil(n / 2).
       Second pass: walk (firstHalfCount - 1) steps from head to land exactly on the last node
       of the first half. Rewire it to point back to head (closing the first circle), then
       continue walking the remaining nodes to find the tail of the second half and close
       that circle too.

     Data structure / paradigm: Simple pointer traversal; no auxiliary structure.

     Time complexity: O(n) — one pass to count (n steps) + one pass to rewire (n steps total,
       split across first/second half traversal) = O(n) overall (constant factor ~2).
     Space complexity: O(1) — only a few pointer/counter variables.

     Pros:
       - No extra memory — matches expected optimal space bound.
       - Conceptually simple: "count then cut" is easy to explain and easy to verify correctness
         (ceil division is a single, obvious formula).

     Cons:
       - Requires two logical traversals of the list (walks the data twice), which, while still
         O(n) asymptotically, is a constant-factor cost that the fast/slow approach avoids by
         finding the midpoint in a single traversal.

     When to use in practice:
       - Perfectly acceptable as a "safe, obviously correct" solution, especially if you want to
         de-risk a live-coding session. Great as a stepping stone to Approach 3.
    ------------------------------------------------------------------------------------------
    */
    public Node[] splitTwoPass(Node head) {
        if (head == null) {
            return new Node[] { null, null };
        }

        // --- Pass 1: count total nodes ---
        int totalCount = 1;
        Node current = head.next;
        while (current != head) {
            totalCount++;
            current = current.next;
        }

        int firstHalfCount = (totalCount + 1) / 2; // ceil(n / 2)

        // --- Pass 2: walk to the end of the first half ---
        Node firstHalfTail = head;
        for (int step = 1; step < firstHalfCount; step++) {
            firstHalfTail = firstHalfTail.next;
        }

        Node secondHalfHead = null;

        if (firstHalfCount < totalCount) {
            secondHalfHead = firstHalfTail.next;

            // Walk from secondHalfHead to the true end of the original list to find its tail.
            int secondHalfCount = totalCount - firstHalfCount;
            Node secondHalfTail = secondHalfHead;
            for (int step = 1; step < secondHalfCount; step++) {
                secondHalfTail = secondHalfTail.next;
            }
            secondHalfTail.next = secondHalfHead; // close second circle
        }

        firstHalfTail.next = head; // close first circle

        return new Node[] { head, secondHalfHead };
    }

    /*
    ------------------------------------------------------------------------------------------
     APPROACH 3 (OPTIMAL): One-Pass Fast/Slow Pointer (Floyd-style midpoint finding)
    ------------------------------------------------------------------------------------------
     Core idea:
       Use two pointers, `slow` and `fast`, both starting at head. `fast` advances two nodes
       per step, `slow` advances one node per step. Because the list is circular, we can't
       just check `fast == null`; instead we stop as soon as EITHER fast.next or fast.next.next
       loops back to head — this tells us fast has "run out of room" and slow now sits exactly
       at the last node of the first half (this naturally implements the ceil(n/2) rule without
       ever explicitly counting n).

       Once found, we split at slow: slow is the tail of the first half; slow.next is the head
       of the second half. We then walk `fast` (reusing it) the rest of the way to the true tail
       of the original list to close the second half's circle.

     Data structure / paradigm: Two-pointer / fast-slow pointer technique (classic Floyd variant),
       adapted for a circular structure instead of the usual null-terminated list.

     Time complexity: O(n) — fast pointer traverses at most ~n nodes total (moving 2 steps per
       iteration over ~n/2 iterations), and the final tail-finding walk covers the remaining
       nodes exactly once with no overlap. Total work is linear, single effective pass.
     Space complexity: O(1) — only pointer variables, no counting pass, no auxiliary storage.

     Pros:
       - Single conceptual pass — no separate counting phase.
       - O(1) space, optimal.
       - Elegant demonstration of fast/slow pointer mastery — exactly what interviewers want to
         see for circular/cyclic linked-list structural problems.

     Cons:
       - Trickier to get the loop-termination condition exactly right for a CIRCULAR list
         (must check `fast.next == head` and `fast.next.next == head` both, to correctly handle
         even vs. odd length lists) — easy to introduce an off-by-one bug here under pressure.
       - Slightly less obvious to a reader unfamiliar with the technique; benefits from a comment
         explaining why the loop condition is what it is.

     When to use in practice:
       - This is the version I would write as my final, "production" answer in a real interview.
    ------------------------------------------------------------------------------------------
    */
    public Node[] splitOptimal(Node head) {
        if (head == null) {
            return new Node[] { null, null };
        }
        if (head.next == head) {
            // Single-node list: first half is just this node, second half is empty.
            return new Node[] { head, null };
        }

        Node slow = head;
        Node fast = head;

        // Advance until fast is about to "lap" back to head. This lands `slow` exactly on the
        // last node of the first half, satisfying the ceil(n/2) rule with NO explicit counting:
        //   - If n is even, loop stops when fast.next == head.
        //   - If n is odd, loop stops when fast.next.next == head.
        while (fast.next != head && fast.next.next != head) {
            slow = slow.next;
            fast = fast.next.next;
        }

        Node firstHalfTail = slow;
        Node secondHalfHead = firstHalfTail.next;

        Node secondHalfHead2 = null;
        if (secondHalfHead != head) {
            // There IS a non-empty second half: fast currently points at (or one before) the
            // original tail. Advance fast to the true tail, whichever branch we stopped on.
            Node originalTail = (fast.next == head) ? fast : fast.next;
            originalTail.next = secondHalfHead; // close second circle
            secondHalfHead2 = secondHalfHead;
        }

        firstHalfTail.next = head; // close first circle

        return new Node[] { head, secondHalfHead2 };
    }
}

/*
====================================================================================================
 SECTION 7: APPROACHES COMPARISON TABLE
====================================================================================================

 | Approach                          | Time  | Space | Best For                              | Limitations                                      |
 |------------------------------------|-------|-------|---------------------------------------|---------------------------------------------------|
 | 1. Brute Force (Auxiliary List)    | O(n)  | O(n)  | Fast whiteboard correctness, fallback | Wastes memory; doesn't show pointer-fu mastery     |
 | 2. Two-Pass Counting               | O(n)  | O(1)  | Safe, easy-to-verify optimal solution | Two logical traversals (constant-factor overhead)  |
 | 3. One-Pass Fast/Slow (OPTIMAL)    | O(n)  | O(1)  | Final interview answer, elegance      | Trickier loop-termination logic to get exactly right|

====================================================================================================
 SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
====================================================================================================
 I would present Approach 3 (One-Pass Fast/Slow Pointer) as my final solution, but I would GET
 THERE by first stating Approach 2 out loud as my initial plan (it's easy to state correctly and
 signals I understand the ceil(n/2) requirement), and then say: "I can actually avoid the separate
 counting pass entirely by using a fast/slow pointer, since the point where fast 'runs out of
 room' in a circular list of length n is precisely the ceil(n/2) boundary." This shows both
 correctness-first thinking AND the ability to optimize live — exactly the signal Google
 interviewers look for. I'd write Approach 2 as a mental fallback in case I get stuck mid-way
 through the pointer arithmetic of Approach 3, then finish with Approach 3.

 Why not Approach 1 as final: it trivially works, but leaving it as your final answer signals you
 didn't recognize the O(1)-space expectation that this exact question is designed to test.
====================================================================================================
*/

/*
====================================================================================================
 SECTION 9: DEEP DIVE — OPTIMAL SOLUTION (PRODUCTION QUALITY)
====================================================================================================
*/

/**
 * Production-quality implementation of the optimal (one-pass, O(1) space) solution for
 * splitting a circular singly linked list into two circular halves.
 *
 * <p>Invariants relied upon by this implementation:
 * <ul>
 *   <li>The input list is guaranteed to be properly circular: starting from {@code head} and
 *       following {@code next} pointers returns to {@code head} after exactly n steps, for some
 *       n &gt;= 1.</li>
 *   <li>The input list is non-null (n &gt;= 1); an explicit null-check is still included
 *       defensively.</li>
 * </ul>
 */
final class CircularLinkedListOptimalSplitter {

    private CircularLinkedListOptimalSplitter() {
        // Utility class; not instantiable.
    }

    /**
     * Splits {@code head}'s circular linked list into two circular linked lists.
     *
     * @param head head of a circular singly linked list containing n &gt;= 1 nodes; must not be
     *             null in a well-formed call, but null is handled defensively.
     * @return a 2-element array where index 0 is the head of the first half (always non-null
     *         for a well-formed non-empty input) and index 1 is the head of the second half
     *         (null if n == 1, since the second half is then empty).
     * @throws IllegalArgumentException if {@code head} is null, since a "circular list" of size
     *         zero is not a meaningful input for this API's documented contract.
     */
    public static Node[] split(Node head) {
        if (head == null) {
            throw new IllegalArgumentException("head must not be null: expected a non-empty circular linked list");
        }

        // Special-case n == 1 up front: fast/slow logic below assumes at least 2 distinct nodes
        // exist to avoid immediately misreading head.next.next as a self-reference.
        final boolean isSingleNode = (head.next == head);
        if (isSingleNode) {
            return new Node[] { head, null };
        }

        Node slowPointer = head; // will end up at the tail of the first half
        Node fastPointer = head; // advances two nodes per iteration

        // Loop invariant: we stop the moment advancing `fast` by one more full 2-step hop would
        // either land exactly on head (even-length list) or land one step past head (odd-length
        // list). At that point `slow` is precisely at position ceil(n/2) - 1 (0-indexed), i.e.
        // the correct last node of the first half.
        while (fastPointer.next != head && fastPointer.next.next != head) {
            slowPointer = slowPointer.next;
            fastPointer = fastPointer.next.next;
        }

        Node firstHalfTail = slowPointer;
        Node secondHalfHead = firstHalfTail.next;

        // If secondHalfHead == head, that means the first half consumed the entire list — but
        // this can only happen for n == 1, which we've already special-cased above, so in the
        // n >= 2 branch here, secondHalfHead is always a genuine, distinct second half head.
        // Locate the true original tail node to close the second half's circle.
        Node originalTail = (fastPointer.next == head) ? fastPointer : fastPointer.next;

        originalTail.next = secondHalfHead; // close the second circular list
        firstHalfTail.next = head;          // close the first circular list

        return new Node[] { head, secondHalfHead };
    }

    /** Utility: builds a circular linked list from an array of values, returning its head. */
    public static Node buildCircularList(int[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("values must be non-empty");
        }
        Node head = new Node(values[0]);
        Node tail = head;
        for (int index = 1; index < values.length; index++) {
            Node newNode = new Node(values[index]);
            tail.next = newNode;
            tail = newNode;
        }
        tail.next = head; // close the circle
        return head;
    }

    /** Utility: renders a circular list starting at head as "v1 -> v2 -> ... -> (back to v1)". */
    public static String renderCircularList(Node head) {
        if (head == null) {
            return "(empty)";
        }
        StringBuilder builder = new StringBuilder();
        Node current = head;
        do {
            builder.append(current.val).append(" -> ");
            current = current.next;
        } while (current != head);
        builder.append("(back to ").append(head.val).append(")");
        return builder.toString();
    }
}

/*
====================================================================================================
 SECTION 10: DRY RUN / TRACE
====================================================================================================
 Using the OPTIMAL solution on Example C from Section 3 (odd length, boundary case):

   Input: 1 -> 2 -> 3 -> (back to 1), head = Node(1)

 Initial state:
   slowPointer = Node(1)
   fastPointer = Node(1)

 isSingleNode check: head.next (Node(2)) != head, so NOT single node. Proceed.

 Loop condition check, iteration 1:
   fastPointer.next       = Node(2)   -> is it == head (Node(1))? No.
   fastPointer.next.next  = Node(3)   -> is it == head (Node(1))? No.
   Condition TRUE -> enter loop body.
     slowPointer = slowPointer.next            => slowPointer = Node(2)
     fastPointer = fastPointer.next.next        => fastPointer = Node(3)

 Loop condition check, iteration 2:
   fastPointer.next       = Node(1) (head)     -> is it == head? YES.
   Condition FALSE (short-circuits on first clause) -> exit loop.

 Post-loop state:
   slowPointer (firstHalfTail) = Node(2)
   secondHalfHead = firstHalfTail.next = Node(3)

   originalTail check: fastPointer.next == head? fastPointer = Node(3), fastPointer.next = Node(1)
   which IS head -> TRUE -> originalTail = fastPointer = Node(3)

 Rewiring:
   originalTail.next = secondHalfHead  => Node(3).next = Node(3)   (second circle: 3 -> 3)
   firstHalfTail.next = head           => Node(2).next = Node(1)   (first circle: 1 -> 2 -> 1)

 Result:
   answer[0] = Node(1), list reads: 1 -> 2 -> (back to 1)   [matches expected first half]
   answer[1] = Node(3), list reads: 3 -> (back to 3)         [matches expected second half]

 This matches Example C's expected output exactly, confirming the ceil(n/2) rule is honored
 without ever explicitly computing n.
====================================================================================================
*/

/*
====================================================================================================
 SECTION 11: CLOSING SUMMARY
====================================================================================================
 - All three approaches are O(n) time; they differ in space (O(n) vs O(1)) and in how many
   logical passes over the data they require.
 - The optimal one-pass fast/slow solution is preferred for production and for interview
   presentation: O(1) space, single traversal, and it directly demonstrates the fast/slow pointer
   technique that circular/cyclic list problems are specifically testing.
 - Known assumptions/limitations of the final solution:
     * Assumes the input is a genuinely circular list (no shortcut cycles into the middle).
     * Assumes n >= 1; throws IllegalArgumentException on null input rather than silently
       returning [null, null], since a truly "empty circular list" isn't a well-formed input
       for this problem's contract.
     * Mutates the original list's `next` pointers in place (as clarified/assumed in Section 2).
====================================================================================================
*/

/*
====================================================================================================
 SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
====================================================================================================
 1. "Can you split the list into k roughly-equal circular parts instead of just 2?" — generalizes
    the fast/slow idea to k-way partitioning by node count.
 2. "What if the list is doubly linked instead of singly linked — does that simplify anything?"
 3. "What if you don't know in advance whether the list is circular or null-terminated — how
    would you detect that safely before splitting?" (Floyd's cycle detection.)
 4. "How would you handle this if the list were being concurrently modified by another thread
    while you split it?"
 5. "Can you do this recursively instead of iteratively — what would the space trade-off be?"
 6. "What if n could be up to 10^9 — does anything about your approach change?" (It doesn't
    asymptotically, but discuss iterative-only, no recursion, to avoid stack concerns.)
====================================================================================================
*/

/*
====================================================================================================
 SECTION 13: WHAT CANDIDATES TYPICALLY MISS
====================================================================================================
 1. Off-by-one in the ceil(n/2) computation — candidates often write n/2 instead of (n+1)/2 (or
    equivalent), producing a first half that's too small by one node when n is odd.
 2. Forgetting to re-close BOTH halves into circles — it's common to correctly find the split
    point but forget that the second half also needs its tail wired back to its own head (not
    left dangling or still pointing into the first half).
 3. Infinite-looping on a circular list because of using `while (current != null)` instead of
    `do { ... } while (current != head)` — null is never reached in a circular list, so any
    traversal logic ported from a "normal" linked list without adjusting the stop condition
    will hang forever.
 4. Mishandling n == 1 in the fast/slow approach — since `head.next == head`, naively evaluating
    `fastPointer.next.next` without a guard reads through the self-loop incorrectly and can
    misclassify a single-node list as needing a "second half" traversal. Always special-case it.
====================================================================================================
*/

/*
====================================================================================================
 RUNNABLE TEST HARNESS — cross-validates all three approaches against each other on the same
 inputs, and prints traces for manual inspection.
====================================================================================================
*/
class SplitCircularLinkedList {

    public static void main(String[] args) {
        runAndPrint("Example A (even, n=4)", new int[] {1, 2, 3, 4});
        runAndPrint("Example B (edge, n=1)", new int[] {5});
        runAndPrint("Example C (odd/boundary, n=3)", new int[] {1, 2, 3});
        runAndPrint("Larger case (n=7)", new int[] {10, 20, 30, 40, 50, 60, 70});
    }

    private static void runAndPrint(String label, int[] values) {
        System.out.println("=== " + label + " ===");

        CircularLinkedListSplitter splitter = new CircularLinkedListSplitter();

        // Build three independent copies, since each approach mutates its input in place.
        Node headForBruteForce = CircularLinkedListOptimalSplitter.buildCircularList(values);
        Node headForTwoPass = CircularLinkedListOptimalSplitter.buildCircularList(values);
        Node headForOptimal = CircularLinkedListOptimalSplitter.buildCircularList(values);

        Node[] bruteForceResult = splitter.splitBruteForce(headForBruteForce);
        Node[] twoPassResult = splitter.splitTwoPass(headForTwoPass);
        Node[] optimalResult = CircularLinkedListOptimalSplitter.split(headForOptimal);

        System.out.println("Brute Force -> first: " + CircularLinkedListOptimalSplitter.renderCircularList(bruteForceResult[0])
                + " | second: " + CircularLinkedListOptimalSplitter.renderCircularList(bruteForceResult[1]));
        System.out.println("Two-Pass    -> first: " + CircularLinkedListOptimalSplitter.renderCircularList(twoPassResult[0])
                + " | second: " + CircularLinkedListOptimalSplitter.renderCircularList(twoPassResult[1]));
        System.out.println("Optimal     -> first: " + CircularLinkedListOptimalSplitter.renderCircularList(optimalResult[0])
                + " | second: " + CircularLinkedListOptimalSplitter.renderCircularList(optimalResult[1]));

        System.out.println();
    }
}
