/*
 * ═══════════════════════════════════════════════════════════════════════════════
 * FILE : LCAWithParentPointersInterviewGuide.java
 * JAVA : 21 (LTS)
 *
 * PROBLEM : Lowest Common Ancestor — nodes have parent pointers, no root given.
 *           Given two nodes p and q (each carrying a .parent reference),
 *           return their lowest common ancestor (LCA).
 *
 * AUTHOR NOTE : Structured exactly as a senior Google engineer would walk
 *               an L4/L5 candidate through the full interview process.
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                        TABLE OF CONTENTS                                │
 * ├──────┬──────────────────────────────────────────────────────────────────┤
 * │  S0  │ Pattern Classification                              ~  line  55  │
 * │  S1  │ Restate the Problem                                 ~  line  90  │
 * │  S2  │ Constraint → Approach Signal Table                  ~  line 130  │
 * │  S3  │ Clarifying Questions                                ~  line 175  │
 * │  S4  │ Examples & Edge Cases                               ~  line 230  │
 * │  S5  │ All Possible Solutions                              ~  line 310  │
 * │  S6  │ Approaches Comparison Table                         ~  line 570  │
 * │  S7  │ Recommended Approach for Interview                  ~  line 610  │
 * │  S8  │ Deep Dive: Optimal Solution                         ~  line 660  │
 * │  S9  │ Dry Run / Trace                                     ~  line 800  │
 * │ S10  │ Runnable Test Harness                               ~  line 880  │
 * │ S11  │ Wrong Solution Autopsy                              ~  line 1020 │
 * │ S12  │ Closing Summary                                     ~  line 1090 │
 * │ S13  │ Follow-Up Questions                                 ~  line 1130 │
 * │ S14  │ What Candidates Typically Miss                      ~  line 1210 │
 * └──────┴──────────────────────────────────────────────────────────────────┘
 */

// ─────────────────────────────────────────────────────────────────────────────
// Shared node definition used by ALL sections below.
// ─────────────────────────────────────────────────────────────────────────────

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode parent; // ← the key ingredient that makes this problem unique

    TreeNode(int val) { this.val = val; }
}

class LCAWithParentPointersInterviewGuide {

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 0 — PATTERN CLASSIFICATION
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PRIMARY PATTERN FAMILY:
 *   Two-Pointer Convergence on an Implicit Linked List (depth-equalisation)
 *
 *   The structural cue that fires pattern recognition:
 *   Each node carries a parent pointer, so every root-to-leaf path is
 *   simultaneously a singly-linked list terminating at the root (null parent).
 *   Given TWO such lists that share a common suffix (the path to root), the
 *   classic "find intersection of two linked lists" trick applies directly:
 *     • measure the depth of both chains,
 *     • advance the deeper pointer by the depth difference,
 *     • then walk both pointers up in lockstep until they collide.
 *   This is the EXACT algorithm used for "Intersection of Two Linked Lists"
 *   (LeetCode 160), mapped onto tree ancestry.
 *
 * TRIGGERING CUES IN THE PROBLEM STATEMENT:
 *   1. "nodes have a reference to their parent" → implicit linked list upward
 *   2. "root is not provided" → can't do classical recursive LCA; must walk up
 *   3. "lowest common ancestor" → first node shared by both ancestry chains
 *
 * SECONDARY PATTERN FAMILY:
 *   HashSet Ancestor Recording (Hashing)
 *   Walk up from p, store every ancestor in a HashSet<TreeNode>.
 *   Then walk up from q; the first node already in the set is the LCA.
 *   Time O(h), Space O(h).
 *
 *   PREFER HashSet when:
 *   • You want to code the simplest, least error-prone solution fast
 *     under high pressure (no depth arithmetic, no off-by-one risk).
 *
 *   PREFER Two-Pointer when:
 *   • Space must be O(1) — e.g., embedded systems, strict memory limits,
 *     or an interviewer explicitly asks for optimal space.
 *
 * PATTERN HIERARCHY FOR THIS PROBLEM:
 *   Level 1 (always consider): tree traversal via parent pointer
 *   Level 2 (space optimal):   two-pointer depth equalisation
 *   Level 3 (code simplicity): HashSet ancestor recording
 * ═══════════════════════════════════════════════════════════════════════════
 */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 1 — RESTATE THE PROBLEM
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * [As I would say aloud to the interviewer]
 *
 * "OK, let me make sure I understand the problem correctly before I start.
 *
 *  We're given two nodes, p and q, in a binary tree. Unlike the classic LCA
 *  problem, each node here carries a reference to its parent node, and we are
 *  NOT given the root.  Our job is to return the lowest common ancestor —
 *  the deepest node in the tree that is an ancestor of both p and q.
 *  An ancestor includes the node itself, so if p is an ancestor of q,
 *  the answer is p.
 *
 *  INPUT:
 *   • Two non-null TreeNode references, p and q.
 *   • Each TreeNode has: int val, TreeNode left, TreeNode right, TreeNode parent.
 *   • The root of the tree is the unique node where parent == null.
 *   • No explicit size bound given, but let's assume the tree fits in memory;
 *     height h could be up to O(n) in the worst case (degenerate/skewed tree).
 *
 *  OUTPUT:
 *   • A single TreeNode — the LCA of p and q.
 *
 *  KEY CONSTRAINTS I'M NOTING:
 *   • Both p and q are guaranteed to exist in the tree (implied by the problem).
 *   • The tree is a valid binary tree — no cycles via parent pointers.
 *   • We cannot (and need not) use the root directly.
 *   • 'Optimal' here means lowest time and space complexity while still
 *     being clean enough to code correctly under interview pressure.
 *
 *  IMPLICIT ASSUMPTIONS I'M MAKING RIGHT NOW:
 *   • p ≠ null and q ≠ null (I'll ask this in clarifying questions).
 *   • p and q both belong to the same tree (not across separate trees).
 *   • Values may not be unique — I should not rely on val for identity;
 *     I'll use reference equality (==)."
 * ═══════════════════════════════════════════════════════════════════════════
 */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 2 — CONSTRAINT → APPROACH SIGNAL TABLE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Constraint                      | Implication / Signal
 *  ────────────────────────────────|─────────────────────────────────────────
 *  Nodes have parent pointers      | Can walk UPWARD to root; no need for
 *                                  | recursive descent. Changes the entire
 *                                  | solution space vs. classic LCA.
 *  ────────────────────────────────|─────────────────────────────────────────
 *  Root NOT provided               | Cannot start BFS/DFS from root.
 *                                  | Must work purely from p and q upward.
 *  ────────────────────────────────|─────────────────────────────────────────
 *  Tree height h (implicit)        | All upward walks are O(h). In a balanced
 *                                  | tree h = O(log n); skewed tree h = O(n).
 *                                  | Both approaches below are O(h).
 *  ────────────────────────────────|─────────────────────────────────────────
 *  Tree fits in memory             | O(h) auxiliary space is acceptable.
 *                                  | O(n) auxiliary space should be avoided
 *                                  | if a better option exists.
 *  ────────────────────────────────|─────────────────────────────────────────
 *  No explicit n / value range     | No overflow risk; no sort needed;
 *                                  | no arithmetic on values at all.
 *  ────────────────────────────────|─────────────────────────────────────────
 *  Reference equality safe         | Problem is structural, not value-based.
 *                                  | Use (nodeA == nodeB) not .equals().
 *  ────────────────────────────────|─────────────────────────────────────────
 *  Ancestor includes node itself   | If p is an ancestor of q, return p.
 *                                  | Both algorithms handle this naturally
 *                                  | because we include p and q in their
 *                                  | own ancestor sets from the start.
 *  ────────────────────────────────|─────────────────────────────────────────
 *  RULED OUT approaches:           |
 *   • Sorting-based                | No array to sort; tree structure.
 *   • Sliding window               | Not a subarray/sublist problem.
 *   • Binary search                | Tree is not necessarily BST.
 *   • Prefix sums / DP             | Not a summing / counting problem.
 *   • Greedy                       | No local-choice optimisation needed.
 *   • Heap                         | No priority ordering needed.
 *   • Trie / Fenwick / Seg Tree    | Overkill; problem is structurally simple.
 * ═══════════════════════════════════════════════════════════════════════════
 */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 3 — CLARIFYING QUESTIONS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Q1: "Are p and q guaranteed to be non-null?"
 *     ASSUMED ANSWER: Yes. The problem states we are given two nodes.
 *     IMPACT: If null were possible, we'd need null guards at entry.
 *
 * Q2: "Are p and q guaranteed to exist in the same tree?"
 *     ASSUMED ANSWER: Yes. They come from the same tree instance.
 *     IMPACT: If different trees, the two-pointer approach could loop
 *             forever because the chains never converge.
 *
 * Q3: "Can p equal q (i.e., same node reference)?"
 *     ASSUMED ANSWER: Yes, this is possible (p == q). In that case
 *     the LCA is p itself.
 *     IMPACT: Both our approaches return p immediately in this scenario;
 *             no special-casing needed.
 *
 * Q4: "Can p be an ancestor of q (or vice versa)?"
 *     ASSUMED ANSWER: Yes. The problem statement says a node IS a
 *     descendant of itself, so p can be its own ancestor.
 *     IMPACT: The LCA would be p itself. Both approaches handle this
 *             naturally without extra code.
 *
 * Q5: "What is the expected tree size / height constraint?
 *      Is it balanced or can it be arbitrarily skewed?"
 *     ASSUMED ANSWER: No explicit bound. Assume it fits in heap memory.
 *     Height could be O(n) in the worst case (fully skewed / linked list).
 *     IMPACT: O(h) solution is always acceptable.
 *
 * Q6: "Are node values unique, or can there be duplicates?"
 *     ASSUMED ANSWER: Values may not be unique.
 *     IMPACT: Must use reference equality (==) to identify nodes,
 *             never value comparison. This is easy to forget under
 *             pressure and a common source of bugs.
 *
 * Q7: "Is there any memory constraint — do we need O(1) extra space?"
 *     ASSUMED ANSWER: No explicit constraint; O(h) auxiliary space is fine.
 *     IMPACT: If strictly O(1) is required, use the two-pointer approach
 *             (depth-equalisation). Otherwise HashSet is simpler.
 *
 * Q8: "Is this a BST, or a general binary tree?"
 *     ASSUMED ANSWER: General binary tree — no BST ordering.
 *     IMPACT: Rules out using value comparisons to navigate the tree,
 *             confirming we must work via parent pointers only.
 * ═══════════════════════════════════════════════════════════════════════════
 */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 4 — EXAMPLES & EDGE CASES
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ── EXAMPLE A: Normal case ───────────────────────────────────────────────
 *
 *  Tree structure:
 *            3           ← root (parent = null)
 *           / \
 *          5   1
 *         / \ / \
 *        6  2 0  8
 *          / \
 *         7   4
 *
 *  p = node(7),  q = node(4)
 *
 *  Ancestry of 7 (walking up via parent):  7 → 2 → 5 → 3 → null
 *  Ancestry of 4 (walking up via parent):  4 → 2 → 5 → 3 → null
 *
 *  Depth of 7 = 4,  Depth of 4 = 4.  Same depth, so no equalisation needed.
 *  Walk both up in lockstep:
 *    Step 1: curr_p=7, curr_q=4  → not equal
 *    Step 2: curr_p=2, curr_q=2  → EQUAL → LCA = node(2) ✓
 *
 * ── EXAMPLE B: Edge case — p is an ancestor of q ────────────────────────
 *
 *  Same tree.  p = node(5),  q = node(7)
 *
 *  Ancestry of 5: 5 → 3 → null        depth = 2
 *  Ancestry of 7: 7 → 2 → 5 → 3 → null   depth = 4
 *
 *  Depth difference = 2. Advance q's pointer 2 steps: 7 → 2 → 5
 *  Now curr_p = 5, curr_q = 5 → EQUAL → LCA = node(5) ✓
 *  (Correctly returns p itself, since p is an ancestor of q.)
 *
 * ── EXAMPLE C: Tricky case — p == q (same node) ─────────────────────────
 *
 *  p = node(6),  q = node(6)  (same reference)
 *
 *  Both pointers start at the same node. The very first comparison
 *  in the lockstep phase finds curr_p == curr_q immediately.
 *  LCA = node(6) ✓
 *
 *  WHY THIS BREAKS NAIVE SOLUTIONS:
 *  A naive approach might calculate depth of p's chain and depth of
 *  q's chain separately — if it starts by checking p != q before
 *  computing depth, the depth-computation loop might run unnecessarily.
 *  It doesn't break correctness here, but a depth-equalisation solution
 *  that initialises after advancing might incorrectly skip the immediate
 *  return. Verified: the correct two-pointer initialisation handles this
 *  because after 0 advance steps, we enter lockstep and the first check
 *  finds equality.
 *
 * ── EXAMPLE D: Single-node tree — p and q are both the root ─────────────
 *
 *  Tree:  42  (root, no children, parent = null)
 *  p = q = node(42)
 *
 *  Depth of p = 1, depth of q = 1.
 *  No advance needed; lockstep: curr_p == curr_q immediately.
 *  LCA = node(42) ✓
 * ═══════════════════════════════════════════════════════════════════════════
 */

    // ─────────────────────────────────────────────────────────────────────
    // Helper: compute depth of a node (distance from node to root,
    //         where root has depth 0).
    // Used by multiple approaches below.
    // ─────────────────────────────────────────────────────────────────────
    private static int depth(TreeNode node) {
        int d = 0;
        while (node.parent != null) { // walk up until root
            node = node.parent;
            d++;
        }
        return d; // O(h) — one pass up to root
    }

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 5 — ALL POSSIBLE SOLUTIONS
 * ═══════════════════════════════════════════════════════════════════════════
 */

    // ── 1. Brute force / naive exhaustive search ──────────────────────────
    /*
     * Approach 1: Collect full ancestor SET of p, then scan q's ancestry.
     *
     * Core Idea:
     *   Walk from p all the way up to the root, storing every visited node
     *   in a HashSet<TreeNode>. Then walk from q upward; the first node
     *   found in the set is the LCA. This is the conceptually simplest
     *   approach and correct in all cases.
     *
     * Paradigm: Hashing / Set membership
     * Time: O(h) — two separate upward walks, each at most h steps.
     *        h = height of the tree.  Dominant cost: building the set (O(h))
     *        and then scanning q's chain (O(h)).
     * Space: O(h) — the HashSet stores at most h+1 ancestors of p.
     * Pros: Trivially simple to code correctly; handles all edge cases
     *       without any arithmetic.
     * Cons: O(h) extra space. In a skewed tree h = n, so space = O(n).
     * Use when: Space is not constrained, or you want a quick first
     *           solution to validate correctness before optimising.
     * Skip when: Strictly O(1) space is required.
     */
    public static TreeNode lcaHashSet(TreeNode p, TreeNode q) {
        // Collect every ancestor of p (including p itself) into a set.
        var ancestorsOfP = new java.util.HashSet<TreeNode>();
        TreeNode current = p;
        while (current != null) {          // walk up until past the root
            ancestorsOfP.add(current);
            current = current.parent;
        }

        // Now walk up from q; the first node already in the set is the LCA.
        current = q;
        while (current != null) {          // guaranteed to hit the set
            if (ancestorsOfP.contains(current)) {
                return current;            // reference equality inside HashSet
            }
            current = current.parent;
        }

        return null; // unreachable if p and q are in the same tree
    }

    // ── 2. Sorting-based ──────────────────────────────────────────────────
    // NOT APPLICABLE: There is no array or sequence to sort.
    // The problem is purely structural (tree traversal via parent pointers).

    // ── 3. Hashing — already covered as Approach 1 above ─────────────────

    // ── 4. Two Pointers ───────────────────────────────────────────────────
    /*
     * Approach 2: Two-Pointer Depth Equalisation (Optimal)
     *
     * Core Idea:
     *   Think of the root-to-p path and root-to-q path as two linked lists
     *   that SHARE a common suffix (the path from LCA to root). To find the
     *   intersection point, first measure the depth of each node, advance
     *   the deeper pointer up by |depth_p - depth_q| steps, then march
     *   both pointers upward in lockstep until they meet.
     *   This is the standard "intersection of two linked lists" technique.
     *
     * Paradigm: Two-pointer convergence on implicit linked list (parent chain)
     * Time: O(h) — one pass to compute each depth O(h), one equalise pass
     *        O(|depth difference|) ≤ O(h), one lockstep pass O(h). Total O(h).
     * Space: O(1) — only two node pointers and two integer counters. No set.
     * Pros: Optimal space. Clean O(h) time. Elegant and impressive.
     * Cons: Slightly more arithmetic than the HashSet version; off-by-one
     *       risk in the advance loop if you're not careful.
     * Use when: O(1) space required, or you want to show the elegant optimal.
     * Skip when: Under extreme pressure and speed of coding matters more than
     *            space — the HashSet version has fewer moving parts.
     */
    public static TreeNode lcaTwoPointer(TreeNode p, TreeNode q) {
        int depthP = depth(p); // O(h): walk p to root counting steps
        int depthQ = depth(q); // O(h): walk q to root counting steps

        TreeNode currP = p;
        TreeNode currQ = q;

        // Advance the deeper pointer so both are at the same depth.
        // After this, both pointers are at the same level of the tree.
        while (depthP > depthQ) {
            currP = currP.parent;
            depthP--;
        }
        while (depthQ > depthP) {
            currQ = currQ.parent;
            depthQ--;
        }

        // Lockstep walk: march both up together until they meet.
        // The first node where currP == currQ is the LCA.
        while (currP != currQ) {   // reference equality — NOT .equals()
            currP = currP.parent;
            currQ = currQ.parent;
        }

        return currP; // both pointers are on the same node: the LCA
    }

    // ── 5. Sliding Window ─────────────────────────────────────────────────
    // NOT APPLICABLE: No contiguous subarray/substring problem here.

    // ── 6. Binary Search on value ─────────────────────────────────────────
    // NOT APPLICABLE: Not a BST; no ordering of values assumed.

    // ── 7. Prefix sums ────────────────────────────────────────────────────
    // NOT APPLICABLE: No cumulative sum or range-query structure needed.

    // ── 8. Greedy ─────────────────────────────────────────────────────────
    // NOT APPLICABLE: No local-choice optimisation that leads to a global
    // optimum; the LCA is a unique structural fact, not a maximisation problem.

    // ── 9. Divide and Conquer ─────────────────────────────────────────────
    // NOT APPLICABLE (in the classical sense): Classic divide-and-conquer LCA
    // requires the root and recurses down. Since the root is not provided and
    // we have parent pointers, the upward traversal approaches are superior.
    // We include a root-finding variant for completeness:

    /*
     * Approach 3: Find Root First, Then Classic Recursive LCA
     *
     * Core Idea:
     *   Use the parent pointer to walk from p (or q) all the way up to the
     *   root (the node whose parent is null). Then apply the classic O(n)
     *   recursive LCA algorithm that starts from the root. This works but
     *   wastes the parent pointer advantage — it degrades to O(n) time and
     *   O(n) recursion stack space.
     *
     * Paradigm: Tree DFS after root discovery
     * Time: O(n) — must traverse the entire subtree rooted at the discovered
     *        root in the worst case to find p and q.
     * Space: O(n) — recursion stack depth O(h), but h can be n (skewed tree).
     * Pros: Reuses well-known LCA algorithm; straightforward to reason about.
     * Cons: Inefficient — ignores the parent pointer shortcut. For large trees
     *       the recursive stack may overflow; no advantage over the upward walks.
     * Use when: Never preferred here — purely illustrative.
     * Skip when: Always — one of the two upward approaches is strictly better.
     */
    public static TreeNode lcaViaRootDiscovery(TreeNode p, TreeNode q) {
        // Step 1: Walk from p to the root.
        TreeNode root = p;
        while (root.parent != null) { // O(h)
            root = root.parent;
        }
        // Step 2: Apply classic recursive LCA from the discovered root.
        return classicLCA(root, p, q); // O(n)
    }

    private static TreeNode classicLCA(TreeNode node, TreeNode p, TreeNode q) {
        if (node == null || node == p || node == q) return node;
        TreeNode leftResult  = classicLCA(node.left,  p, q);
        TreeNode rightResult = classicLCA(node.right, p, q);
        if (leftResult != null && rightResult != null) return node; // split
        return (leftResult != null) ? leftResult : rightResult;
    }

    // ── 10–13. Memoized DP, Tabulation, Monotonic Stack, Heap ─────────────
    // NOT APPLICABLE: None of these paradigms fit a parent-pointer tree
    // traversal. There is no recurrence to memoize, no array of subproblems,
    // no monotonic ordering to exploit, and no priority ordering needed.

    // ── 14. Tree Traversal ────────────────────────────────────────────────
    // Covered by Approach 2 (two-pointer upward walk) and Approach 3.
    // The "traversal" here is upward via parent pointers, not downward BFS/DFS.

    // ── 15. Graph Traversal / Union-Find ──────────────────────────────────
    /*
     * Approach 4: Cycle-meeting via simultaneous upward walk
     *             (Elegant O(h) / O(1) — alternate two-pointer formulation)
     *
     * Core Idea:
     *   Instead of computing depths explicitly, we use a clever pointer-swap
     *   trick: when a pointer reaches null (past the root), redirect it to the
     *   OTHER node's starting point. Both pointers then travel the same total
     *   distance (depth_p + depth_q) before meeting at the LCA. This is the
     *   same trick used in the "Intersection of Two Linked Lists" problem.
     *   If p == q, the pointers meet immediately on the first iteration.
     *
     * Paradigm: Two-pointer convergence with cross-redirect
     * Time: O(h) — each pointer travels at most depth_p + depth_q steps
     *        before converging. Both depths ≤ h, so total ≤ 2h = O(h).
     * Space: O(1) — only two node references; no auxiliary structures.
     * Pros: No depth arithmetic; fewer lines of code than Approach 2;
     *       handles the p == q case elegantly without any special case.
     * Cons: The redirect logic (null → other starting node) can confuse
     *       interviewers who haven't seen the trick; requires clear explanation.
     * Use when: You know this trick well and can explain it confidently.
     * Skip when: Under pressure and you're not 100% sure of the convergence
     *            argument — Approach 1 (HashSet) is safer to fall back on.
     */
    public static TreeNode lcaPointerSwap(TreeNode p, TreeNode q) {
        TreeNode a = p;
        TreeNode b = q;

        // Each pointer walks up; when it falls off the root (becomes null),
        // it is redirected to the OTHER node's original position.
        // Total steps each pointer takes = depth(p) + depth(q).
        // They must converge at the LCA.
        while (a != b) {
            // If a hasn't reached the root yet, go up; otherwise jump to q's start.
            a = (a == null) ? q : a.parent;
            // If b hasn't reached the root yet, go up; otherwise jump to p's start.
            b = (b == null) ? p : b.parent;
        }

        return a; // a == b == LCA (or both null only if the two nodes are in
                  // different trees, which we ruled out in our assumptions)
    }

    // ── 16. Backtracking ──────────────────────────────────────────────────
    // NOT APPLICABLE: We are not enumerating combinations or permutations;
    // the LCA is a unique node, not a search over a decision space.

    // ── 17. Trie / Segment Tree / Fenwick / Advanced structures ───────────
    // NOT APPLICABLE: These structures solve different problems (string prefix
    // search, range queries). Overkill and irrelevant here.

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 6 — APPROACHES COMPARISON TABLE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 *  Approach                       | Time  | Space | Best For              | Limitations
 *  ───────────────────────────────|───────|───────|───────────────────────|──────────────────────────
 *  1: HashSet Ancestor Recording  | O(h)  | O(h)  | Simplicity, any h     | O(h) extra space
 *  ───────────────────────────────|───────|───────|───────────────────────|──────────────────────────
 *  2: Two-Pointer Depth-Equalise  | O(h)  | O(1)  | Space-optimal, clean  | Depth arithmetic risk
 *  ───────────────────────────────|───────|───────|───────────────────────|──────────────────────────
 *  3: Find Root + Classic LCA     | O(n)  | O(n)  | Reuses known algo     | Ignores parent advantage;
 *                                 |       |       |                       | stack overflow on large n
 *  ───────────────────────────────|───────|───────|───────────────────────|──────────────────────────
 *  4: Pointer-Swap Cross-Redirect | O(h)  | O(1)  | Elegant, fewest lines | Requires explaining the
 *                                 |       |       | Handles p==q cleanly  | convergence argument
 *  ───────────────────────────────|───────|───────|───────────────────────|──────────────────────────
 *
 *  h = height of tree. h = O(log n) for balanced, O(n) for skewed.
 *  n = total number of nodes in the tree.
 *
 *  SUMMARY OF TRADE-OFFS:
 *   • Approach 1 (HashSet) is the easiest to code correctly and explain.
 *   • Approach 2 (Two-Pointer Depth) is the classic interview answer for O(1) space.
 *   • Approach 4 (Pointer-Swap) is the slickest but needs careful verbal justification.
 *   • Approach 3 is a trap — it throws away the parent pointer advantage.
 * ═══════════════════════════════════════════════════════════════════════════
 */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 7 — RECOMMENDED APPROACH FOR INTERVIEW
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * RECOMMENDATION: Lead with Approach 1 (HashSet), then pivot to Approach 2
 *                 (Two-Pointer Depth-Equalisation) when asked to optimise space.
 *
 * RATIONALE:
 *
 * • CLARITY:
 *   The HashSet approach is instantly readable by any engineer: "collect p's
 *   ancestors, then walk up from q until you find a match." This takes < 10
 *   lines of code with zero tricky arithmetic. Even under pressure you can
 *   write this correctly. Once the interviewer confirms it, you can say "I
 *   notice this uses O(h) space — I can reduce that to O(1) with a depth-
 *   equalisation technique." This two-phase answer shows breadth AND depth.
 *
 * • OPTIMALITY:
 *   Approach 2 achieves O(h) time AND O(1) space — you cannot do better
 *   without additional preprocessing (e.g. Euler tour + sparse table for
 *   O(1) LCA query, but that requires O(n) preprocessing and is absurd overkill
 *   here). For a single LCA query with parent pointers, O(h) / O(1) is optimal.
 *
 * • SPEED:
 *   HashSet: ~8 lines, zero arithmetic bugs possible.
 *   Two-Pointer: ~12 lines, requires careful depth counting.
 *   Either can be written in under 5 minutes. Both are fast enough.
 *   Starting with HashSet and upgrading is the best pacing strategy.
 *
 * • SIGNAL TO INTERVIEWER:
 *   Proposing HashSet first signals: "I start simple, confirm correctness."
 *   Then upgrading signals: "I analyse complexity and optimise proactively."
 *   This demonstrates EXACTLY the engineering judgment Google evaluates:
 *   solve → analyse → improve → justify. Many strong candidates jump straight
 *   to O(1) space and make off-by-one errors. Starting with the safe approach
 *   and upgrading is the senior-engineer move.
 *
 * IF THE INTERVIEWER SAYS "Can you do it in O(1) space from the start?":
 *   Go directly to Approach 2 (depth-equalisation) — it is concrete, correct,
 *   and the arithmetic is straightforward to trace through on a whiteboard.
 *   Only present Approach 4 (pointer-swap) if you're extremely comfortable
 *   explaining the convergence proof in real-time.
 * ═══════════════════════════════════════════════════════════════════════════
 */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 8 — DEEP DIVE: OPTIMAL SOLUTION
 * ═══════════════════════════════════════════════════════════════════════════
 */

    // ── PART A — VERBALIZATION SCRIPT ────────────────────────────────────────
    //
    // SAY: "OK, so I'll present the HashSet approach first, then tighten it
    //       to O(1) space with depth equalisation."
    //
    // [Writing the helper method]
    // SAY: "Let me start with a small helper that computes the depth of any
    //       node — that's just the number of parent hops to reach the root.
    //       The root has depth 0, so I start counting at 0 and increment
    //       each time I move up via the parent pointer."
    //
    // [Writing the depth-equalisation method]
    // SAY: "Now for the main method. I compute the depth of both p and q.
    //       The deeper node's pointer needs to be advanced upward first,
    //       so that both pointers sit at the same depth level."
    //
    // SAY: "The while loops that advance depthP and depthQ are depth-difference
    //       loops — they run at most h iterations, so O(h) each. After these
    //       loops, both currP and currQ are at the same depth."
    //
    // SAY: "Then I enter the lockstep phase: I advance BOTH pointers one step
    //       at a time until they point at the same node. Because they're at
    //       the same depth and walking the same tree upward, they MUST meet
    //       at the LCA. I use reference equality — == — not .equals(), because
    //       I care about node identity, not value equality."
    //
    // SAY: "Let me trace through Example A quickly to verify:
    //       p = node(7) depth 4, q = node(4) depth 4. No advance needed.
    //       Lockstep: 7 vs 4 — not equal; go up to 2 vs 2 — equal! Return 2."
    //
    // SAY: "Time complexity is O(h) — three linear passes up the tree.
    //       Space complexity is O(1) — only four variables: two pointers,
    //       two integers. No auxiliary data structures."
    //
    // SAY: "Let me also handle the edge case where p and q are the same node.
    //       If depthP == depthQ (which they will be, since they ARE the same),
    //       we skip both advance loops and enter lockstep. The first check
    //       finds currP == currQ immediately, so we return p. Correct."

    // ── PART B — JAVA API CHOICES ─────────────────────────────────────────
    //
    // DATA STRUCTURES USED:
    //   • TreeNode reference variables (currP, currQ):
    //     Plain Java object references. No boxing. No wrappers. Direct pointer
    //     walk. Using == for reference equality is correct and cheap (O(1)).
    //
    //   • int (depthP, depthQ):
    //     Primitive int over Integer — no autoboxing overhead. Depths fit
    //     easily within int range (tree height << Integer.MAX_VALUE).
    //
    //   • HashSet<TreeNode> (in Approach 1 only):
    //     HashSet chosen over LinkedHashSet because we don't need insertion
    //     order. Lookup is O(1) average. TreeSet would be O(log h) per lookup
    //     and requires a Comparator — pointless overhead here.
    //     HashSet uses reference hashCode() by default (identity hash code),
    //     which is exactly what we want: node identity, not value equality.
    //     Note: we did NOT override hashCode/equals on TreeNode, which is
    //     intentional — the default identity-based hashing is correct.
    //
    // JAVA 21 FEATURES USED:
    //   • var for local type inference (ancestorsOfP) in HashSet approach —
    //     reduces verbosity without sacrificing readability.
    //   • No records, sealed classes, or pattern matching needed here — the
    //     problem is algorithmic, not type-system-oriented.

    // ── PART C — FULL IMPLEMENTATION (Optimal: Two-Pointer Depth-Equalise) ─

    /**
     * Returns the lowest common ancestor of nodes {@code p} and {@code q}
     * in a binary tree where every node carries a parent reference.
     *
     * <p>Algorithm: depth-equalisation two-pointer.
     * <ol>
     *   <li>Compute the depth of p and q by walking each to the root.</li>
     *   <li>Advance the deeper pointer upward until both are at equal depth.</li>
     *   <li>Walk both pointers up in lockstep until they reference the same node.</li>
     * </ol>
     *
     * @param p a non-null node in the tree
     * @param q a non-null node in the same tree
     * @return the LCA of p and q (never null if p and q are in the same tree)
     *
     * Time  : O(h) — three passes up the tree, each ≤ h steps; h = tree height.
     * Space : O(1) — four primitive/reference variables; no auxiliary structure.
     */
    public static TreeNode lowestCommonAncestor(TreeNode p, TreeNode q) {
        // ── Step 1: Compute depths (distance from node to root) ──────────
        int depthP = depth(p); // O(h): walk p → root, counting hops
        int depthQ = depth(q); // O(h): walk q → root, counting hops

        TreeNode currP = p;
        TreeNode currQ = q;

        // ── Step 2: Equalise depths ───────────────────────────────────────
        // Advance the deeper pointer upward until both are at the same depth.
        // This runs at most |depthP - depthQ| ≤ h iterations → O(h).
        while (depthP > depthQ) {
            currP = currP.parent;
            depthP--;
        }
        while (depthQ > depthP) {
            currQ = currQ.parent;
            depthQ--;
        }

        // ── Step 3: Lockstep walk until convergence ───────────────────────
        // Both pointers are now at the same depth level.
        // Walk both up simultaneously; they MUST meet at the LCA.
        // Crucially: use == (reference equality), NOT .equals() or value compare.
        // Runs at most h iterations in the worst case → O(h).
        while (currP != currQ) {
            currP = currP.parent;
            currQ = currQ.parent;
        }

        // currP == currQ == LCA (guaranteed since p and q share the same root)
        return currP;
    }

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 9 — DRY RUN / TRACE
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Using Example A from Section 4:
 *
 *  Tree:        3
 *              / \
 *             5   1
 *            / \ / \
 *           6  2 0  8
 *             / \
 *            7   4
 *
 *  p = node(7),  q = node(4)
 *
 * ── STEP 1: depth(p) where p = node(7) ──────────────────────────────────
 *
 *  Iteration | current node | d (depth counter)
 *  ──────────|──────────────|──────────────────
 *  Start     | node(7)      | 0
 *  1         | node(2)      | 1   [7.parent = 2]
 *  2         | node(5)      | 2   [2.parent = 5]
 *  3         | node(3)      | 3   [5.parent = 3]
 *  4         | null         | 4   [3.parent = null → exit while]
 *  → depthP = 4
 *
 * ── STEP 2: depth(q) where q = node(4) ──────────────────────────────────
 *
 *  Iteration | current node | d (depth counter)
 *  ──────────|──────────────|──────────────────
 *  Start     | node(4)      | 0
 *  1         | node(2)      | 1   [4.parent = 2]
 *  2         | node(5)      | 2   [2.parent = 5]
 *  3         | node(3)      | 3   [5.parent = 3]
 *  4         | null         | 4   [3.parent = null → exit while]
 *  → depthQ = 4
 *
 * ── STEP 3: Equalise depths ──────────────────────────────────────────────
 *
 *  depthP == depthQ == 4 → NEITHER while loop executes.
 *  currP = node(7),  currQ = node(4)  (unchanged)
 *
 * ── STEP 4: Lockstep walk ────────────────────────────────────────────────
 *
 *  Iteration | currP        | currQ        | currP == currQ?
 *  ──────────|──────────────|──────────────|────────────────
 *  Check 1   | node(7)      | node(4)      | NO  → advance both
 *  After adv | node(2)      | node(2)      |
 *  Check 2   | node(2)      | node(2)      | YES → exit while
 *
 *  return currP = node(2) ✓
 *
 * WHY node(2) is correct:
 *   node(2) is the deepest node that appears in BOTH ancestry chains.
 *   Ancestry of 7: 7→2→5→3   (node 2 at depth 3)
 *   Ancestry of 4: 4→2→5→3   (node 2 at depth 3)
 *   node(2) is at depth 3 — deeper than node(5) (depth 2) and node(3) (depth 1).
 *   Therefore node(2) is the LCA. ✓
 * ═══════════════════════════════════════════════════════════════════════════
 */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 10 — RUNNABLE TEST HARNESS
 * ═══════════════════════════════════════════════════════════════════════════
 */

    /*
     * Build the example tree used in Section 4:
     *
     *            3
     *           / \
     *          5   1
     *         / \ / \
     *        6  2 0  8
     *          / \
     *         7   4
     */
    private static TreeNode[] buildExampleTree() {
        TreeNode n3 = new TreeNode(3);
        TreeNode n5 = new TreeNode(5);
        TreeNode n1 = new TreeNode(1);
        TreeNode n6 = new TreeNode(6);
        TreeNode n2 = new TreeNode(2);
        TreeNode n0 = new TreeNode(0);
        TreeNode n8 = new TreeNode(8);
        TreeNode n7 = new TreeNode(7);
        TreeNode n4 = new TreeNode(4);

        // Wire children
        n3.left = n5; n3.right = n1;
        n5.left = n6; n5.right = n2;
        n1.left = n0; n1.right = n8;
        n2.left = n7; n2.right = n4;

        // Wire parents
        n5.parent = n3; n1.parent = n3;
        n6.parent = n5; n2.parent = n5;
        n0.parent = n1; n8.parent = n1;
        n7.parent = n2; n4.parent = n2;
        // n3.parent remains null (it is the root)

        return new TreeNode[]{n3, n5, n1, n6, n2, n0, n8, n7, n4};
        // index:              0   1   2   3   4   5   6   7   8
    }

    // Build a deeply skewed (degenerate) tree: 1 → 2 → 3 → ... → maxSize
    // where each node's left child is the next node. Used for boundary test.
    private static TreeNode[] buildSkewedTree(int maxSize) {
        TreeNode[] nodes = new TreeNode[maxSize];
        for (int i = 0; i < maxSize; i++) nodes[i] = new TreeNode(i + 1);
        for (int i = 0; i < maxSize - 1; i++) {
            nodes[i].left = nodes[i + 1];
            nodes[i + 1].parent = nodes[i];
        }
        return nodes;
    }

    private static void runTest(String testName, TreeNode p, TreeNode q,
                                int expectedVal, TreeNode actualResult) {
        boolean pass = (actualResult != null) && (actualResult.val == expectedVal);
        System.out.printf("%-35s | Expected val=%-3d | Actual val=%-3s | %s%n",
                testName,
                expectedVal,
                (actualResult == null ? "null" : actualResult.val),
                pass ? "PASS ✓" : "FAIL ✗");
    }

    private static void runNullTest(String testName, TreeNode actualResult) {
        boolean pass = (actualResult == null);
        System.out.printf("%-35s | Expected val=null | Actual val=%-3s | %s%n",
                testName,
                (actualResult == null ? "null" : actualResult.val),
                pass ? "PASS ✓" : "FAIL ✗");
    }

    public static void main(String[] args) {

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  LCA WITH PARENT POINTERS — TEST HARNESS");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        TreeNode[] tree = buildExampleTree();
        // Unpack for readability:
        TreeNode n3 = tree[0], n5 = tree[1], n1 = tree[2];
        TreeNode n6 = tree[3], n2 = tree[4], n0 = tree[5];
        TreeNode n8 = tree[6], n7 = tree[7], n4 = tree[8];

        System.out.println("── Testing: lowestCommonAncestor (Optimal: Two-Pointer) ──────");

        // Test 1: Normal case — p=node(7), q=node(4), expected LCA=node(2)
        runTest("T1 Normal: p=7,q=4",
                n7, n4, 2,
                lowestCommonAncestor(n7, n4));

        // Test 2: p is ancestor of q — p=node(5), q=node(7), expected LCA=node(5)
        runTest("T2 Ancestor: p=5,q=7",
                n5, n7, 5,
                lowestCommonAncestor(n5, n7));

        // Test 3: p == q (same node) — expected LCA=node(6)
        runTest("T3 Same node: p=q=6",
                n6, n6, 6,
                lowestCommonAncestor(n6, n6));

        // Test 4: Boundary — skewed tree of 10 000 nodes, p=deepest, q=root's left child
        //         expected LCA = second node (index 1), since its ancestry includes all below.
        TreeNode[] skewed = buildSkewedTree(10_000);
        TreeNode skewedDeepest = skewed[9_999]; // deepest node (val=10000)
        TreeNode skewedSecond  = skewed[1];     // second node (val=2)
        // LCA of deepest and second: must be second (ancestor of deepest)
        runTest("T4 Skewed 10k: p=deepest,q=2nd",
                skewedDeepest, skewedSecond, 2,
                lowestCommonAncestor(skewedDeepest, skewedSecond));

        // Test 5: Tricky — nodes on completely different branches; p=node(6), q=node(8)
        //         Ancestry of 6: 6→5→3. Ancestry of 8: 8→1→3. LCA = node(3)
        runTest("T5 Diff branches: p=6,q=8",
                n6, n8, 3,
                lowestCommonAncestor(n6, n8));

        System.out.println();
        System.out.println("── Testing: lcaHashSet (Approach 1) ─────────────────────────");

        runTest("T1 Normal: p=7,q=4",        n7, n4, 2, lcaHashSet(n7, n4));
        runTest("T2 Ancestor: p=5,q=7",      n5, n7, 5, lcaHashSet(n5, n7));
        runTest("T3 Same node: p=q=6",       n6, n6, 6, lcaHashSet(n6, n6));
        runTest("T4 Skewed 10k: p=deepest,q=2nd",
                skewedDeepest, skewedSecond, 2, lcaHashSet(skewedDeepest, skewedSecond));
        runTest("T5 Diff branches: p=6,q=8", n6, n8, 3, lcaHashSet(n6, n8));

        System.out.println();
        System.out.println("── Testing: lcaPointerSwap (Approach 4) ─────────────────────");

        runTest("T1 Normal: p=7,q=4",        n7, n4, 2, lcaPointerSwap(n7, n4));
        runTest("T2 Ancestor: p=5,q=7",      n5, n7, 5, lcaPointerSwap(n5, n7));
        runTest("T3 Same node: p=q=6",       n6, n6, 6, lcaPointerSwap(n6, n6));
        runTest("T4 Skewed 10k: p=deepest,q=2nd",
                skewedDeepest, skewedSecond, 2, lcaPointerSwap(skewedDeepest, skewedSecond));
        runTest("T5 Diff branches: p=6,q=8", n6, n8, 3, lcaPointerSwap(n6, n8));

        System.out.println();
        System.out.println("── Testing: single-node tree (p == q == root) ───────────────");
        TreeNode solo = new TreeNode(42); // no parent, no children
        runTest("T_Single node p==q==root",   solo, solo, 42, lowestCommonAncestor(solo, solo));

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 11 — WRONG SOLUTION AUTOPSY
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * THE BUGGY SOLUTION: "Value-Comparison Ancestor Walk"
 *
 * WHAT IT LOOKS LIKE ON THE SURFACE:
 *   Many candidates — especially those who've seen BST LCA — reflexively
 *   write code that compares node.val when checking identity. This looks
 *   reasonable: if two variables hold the same value, they're the "same node".
 *
 * WHAT IS WRONG:
 *   Node values may NOT be unique in a general binary tree. If two distinct
 *   nodes share the same value, value comparison will find a WRONG ancestor.
 *
 * EXACT FAILURE CASE:
 *   Build a tree where node(5) appears at two different positions.
 *   Walk p's ancestors and store their VALUES in a HashSet<Integer>.
 *   Walk from q; the first node whose value is in the set is returned.
 *   But if q's ancestor happens to have the same VALUE as a node in p's
 *   chain that is NOT on the shared suffix, we return the wrong node.
 *
 *   Concrete example (duplicate values):
 *         5         ← root (val = 5)
 *        / \
 *       5   3       ← left child also has val = 5
 *      /
 *     7
 *
 *   p = node(7) (left subtree, under the left 5)
 *   q = node(3) (right subtree)
 *
 *   Correct LCA = root (val = 5, the actual root node).
 *
 *   Buggy walk from p: stores vals {7, 5, 5} (both 5s) as integers → set = {7, 5}.
 *   Buggy walk from q: q=node(3), go up to root node(5). Val 5 is in set → RETURNS root. ✓
 *   (This case accidentally gives the right answer.)
 *
 *   A more dangerous failure:
 *   p = node(7), q = node(5) [the LEFT child node(5), not the root]
 *   Correct LCA = node(5) [the left child, since it's an ancestor of p].
 *   Buggy walk stores p's ancestors' vals: {7, 5}. Walk from q=left_node_5:
 *   val 5 is in the set, so returns left_node_5. ✓ (still right by accident)
 *
 *   The truly breaking case: two nodes under different branches with a shared
 *   value where one is NOT an ancestor of the other. Buggy code would find
 *   the FIRST value match even though it's the wrong structural node.
 *
 * THE BUGGY CODE:
 */
    // ⚠ DO NOT USE — incorrect when node values are not unique
    public static TreeNode lcaBuggyValueComparison(TreeNode p, TreeNode q) {
        var ancestorVals = new java.util.HashSet<Integer>();
        TreeNode curr = p;
        while (curr != null) {
            ancestorVals.add(curr.val); // BUG: stores VALUES, not node identity
            curr = curr.parent;
        }
        curr = q;
        while (curr != null) {
            if (ancestorVals.contains(curr.val)) { // BUG: matches by value, not reference
                return curr;                        // wrong node if values not unique
            }
            curr = curr.parent;
        }
        return null;
    }

    /*
     * EXACT FAILURE:
     *   Tree:     5 (root)
     *            / \
     *           5   5    ← THREE nodes all with val = 5
     *
     *   p = right child (val=5), q = left child (val=5).
     *   Correct LCA: root (val=5, depth 0).
     *
     *   Buggy execution:
     *     Walk from p (right child): stores vals {5, 5} → ancestorVals = {5}.
     *     Walk from q (left child): first node has val=5, which IS in set.
     *     Returns LEFT CHILD — not the root. WRONG ANSWER.
     *
     * THE MINIMAL FIX:
     *   Store node REFERENCES (HashSet<TreeNode>), not values.
     *   This leverages Java's default identity-based hashCode/equals
     *   on objects, which is exactly what we need for structural identity.
     *
     *   Fix: ancestorVals.add(curr)  instead of  ancestorVals.add(curr.val)
     *        ancestorVals.contains(curr)  instead of  ancestorVals.contains(curr.val)
     *
     * WHY THE FIX WORKS:
     *   Java's Object.hashCode() is based on object identity (memory address
     *   by default, before any hashCode override). Two distinct TreeNode objects
     *   will NEVER be considered equal by the default HashSet, regardless of
     *   whether their val fields are equal. This is precisely what we want:
     *   node identity, not value equality.
     * ═══════════════════════════════════════════════════════════════════════
     */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 12 — CLOSING SUMMARY
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * • The key insight transforming this problem: parent pointers convert each
 *   root-to-leaf path into a singly-linked list, reducing LCA to "intersection
 *   of two linked lists" — a well-known two-pointer problem.
 *
 * • Approach 1 (HashSet): O(h) time, O(h) space. Simplest to code correctly.
 *   Best for interviews where speed of correct code matters most.
 *
 * • Approach 2 (Depth-Equalisation Two-Pointer): O(h) time, O(1) space.
 *   Optimal in both dimensions. Best when the interviewer asks for O(1) space.
 *
 * • Approach 4 (Pointer-Swap): Also O(h) / O(1) but requires explaining a
 *   non-obvious convergence argument. Only use if you know the trick deeply.
 *
 * • Approach 3 (Find Root + Classic LCA): O(n) time, O(n) space. A trap —
 *   it discards the parent-pointer advantage entirely. Never preferred.
 *
 * • The optimal time lower bound for a single LCA query with parent pointers
 *   is Ω(h): you must visit at least h nodes to determine the ancestor chain.
 *   Both Approach 2 and Approach 4 achieve this lower bound.
 *
 * • CRITICAL ASSUMPTION: node identity is determined by REFERENCE (==), not
 *   value. Do not use .val comparisons unless values are guaranteed unique.
 *
 * • The solution correctly handles all edge cases:
 *   p == q, p is an ancestor of q (or vice versa), single-node tree,
 *   completely separate branches, skewed (worst-case height) trees.
 *
 * • IF THE CONSTRAINT CHANGES — "What if there are no parent pointers?":
 *   Revert to classic recursive LCA from a given root. O(n) time, O(h) space.
 *   Or preprocess with Euler tour + Sparse Table for O(n) preprocessing +
 *   O(1) per query — valuable when there are many LCA queries.
 *
 * • IF THE CONSTRAINT CHANGES — "What if the tree is a BST?":
 *   Use value comparisons to navigate: walk from root, go left if both
 *   nodes are smaller, right if both are larger, else current is LCA.
 *   O(h) time, O(1) space — no parent pointers needed.
 *
 * • IF THE CONSTRAINT CHANGES — "Multiple LCA queries on a static tree?":
 *   Preprocess with LCA in O(n log n) using binary lifting (sparse table
 *   on ancestors). Each query then runs in O(log n).
 * ═══════════════════════════════════════════════════════════════════════════
 */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 13 — FOLLOW-UP QUESTIONS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * FU1: "What if you receive n pairs of (p, q) queries on the same static tree?
 *       Can you do better than O(h) per query?"
 *
 *      ANSWER: Yes. Preprocess the tree with the BINARY LIFTING algorithm.
 *      Build a sparse table: anc[node][k] = 2^k-th ancestor of node.
 *      Preprocessing: O(n log n) time, O(n log n) space. Each LCA query: O(log n).
 *      This is the standard competitive-programming LCA solution.
 *      For very large static trees with many queries, it dominates.
 *
 * FU2: "What if the tree is modified (nodes inserted/deleted) while queries
 *       arrive? How does your approach change?"
 *
 *      ANSWER: The static sparse table is invalidated by every structural
 *      change. We'd need a dynamic LCA structure. One practical approach:
 *      accept O(h) per query with the two-pointer method (no preprocessing
 *      to invalidate). For dynamic trees with guaranteed balance (e.g., AVL,
 *      red-black), h = O(log n) and the simple method is fast enough.
 *      Link-Cut Trees support O(log n) dynamic LCA but are extremely complex.
 *
 * FU3: "What if this runs in a multithreaded environment? Is your solution
 *       thread-safe?"
 *
 *      ANSWER: The two-pointer solution reads only; it does not write to
 *      any shared state. It is naturally thread-safe for concurrent reads.
 *      However, if another thread is MODIFYING the tree (adding/removing
 *      nodes or changing parent pointers) concurrently, the walk could
 *      encounter inconsistent state. In that case, you'd need a read-write
 *      lock (ReadWriteLock) or an immutable snapshot before querying.
 *
 * FU4: "What if n is extremely large (n → 10^9) and the tree doesn't fit
 *       in memory? Can you solve it in a distributed setting?"
 *
 *      ANSWER: In a distributed tree (e.g., a massive file system hierarchy),
 *      we'd materialise the ancestry chains of p and q as streams.
 *      Stream both chains toward the root concurrently; hash-join them on
 *      node ID at a coordinator. This is essentially the MapReduce version
 *      of the HashSet approach. The two-pointer approach doesn't parallelise
 *      naturally since depth comparison requires full chain traversal.
 *
 * FU5: "What if instead of parent pointers, you were given the tree as an
 *       adjacency list (undirected graph)?"
 *
 *      ANSWER: Treat the problem as 'find LCA in a general tree given as
 *      a graph'. BFS/DFS from either p or q to find the path to root (or
 *      to each other). Alternatively, root the tree at any node, run DFS
 *      to compute depths and binary-lifting tables, then answer queries in
 *      O(log n). The key insight: parent pointers in the original problem
 *      are essentially the rooted-tree adjacency list pointing upward.
 *
 * FU6: "Extend this to an N-ary tree (each node can have k children).
 *       Does your algorithm change?"
 *
 *      ANSWER: Not at all. Neither the HashSet approach nor the two-pointer
 *      approach cares about how many children a node has — both only ever
 *      traverse the parent chain upward. The algorithms work identically
 *      for N-ary trees as long as each node has a single parent pointer.
 *      Time and space complexities remain O(h) and O(1) / O(h) respectively.
 * ═══════════════════════════════════════════════════════════════════════════
 */

/* ═══════════════════════════════════════════════════════════════════════════
 * SECTION 14 — WHAT CANDIDATES TYPICALLY MISS
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * MISTAKE 1: Using .equals() or value comparison instead of reference equality
 *
 *   What it looks like:
 *     if (curr.val == ancestorVal) ...   or   if (curr.equals(other)) ...
 *   Why it's easy to make:
 *     Under pressure, candidates familiar with BST problems instinctively
 *     compare values. Java beginners may also assume .equals() is safer
 *     than ==, not knowing that default Object.equals IS reference equality.
 *   Prevention rule:
 *     "For node identity in a general binary tree, ALWAYS use ==.
 *      Store TreeNode references in your HashSet, never int values."
 *
 * MISTAKE 2: Off-by-one in the depth-equalisation loop
 *
 *   What it looks like:
 *     while (depthP - depthQ > 0) { currP = currP.parent; depthP--; }
 *     // accidentally advances one too many or one too few times
 *   Why it's easy to make:
 *     The loop condition involves subtraction; under pressure, candidates
 *     mix up > vs >= or forget to decrement the depth counter alongside
 *     advancing the pointer, causing the loop to run one extra time.
 *   Prevention rule:
 *     "Write it as two separate loops: one while depthP > depthQ (advance p)
 *      and one while depthQ > depthP (advance q). Clear, symmetric, and
 *      impossible to confuse. Always decrement the depth counter inside
 *      the same loop iteration as the pointer advance."
 *
 * MISTAKE 3: Forgetting that a node is its own ancestor
 *
 *   What it looks like:
 *     Candidate starts by adding p.parent (not p itself) to the ancestor set,
 *     or advances BOTH pointers before the first equality check.
 *   Why it's easy to make:
 *     The word "ancestor" sometimes implies a PROPER ancestor (a node strictly
 *     above you). The problem explicitly says a node IS its own descendant
 *     (and thus its own ancestor). If p is an ancestor of q, the correct
 *     LCA is p, not p's parent.
 *   Prevention rule:
 *     "Always initialise the ancestor collection starting with p itself,
 *      not p.parent. Always start the lockstep at the current equalised
 *      positions — check for equality BEFORE advancing, not after."
 *
 * MISTAKE 4: Assuming the pointer-swap approach terminates when p == null
 *
 *   What it looks like:
 *     while (a != b) { a = (a.parent == null) ? q : a.parent; ... }
 *     // Uses a.parent == null instead of a == null for the redirect check
 *   Why it's easy to make:
 *     Candidates confuse "a has reached the root" (a.parent == null)
 *     with "a has gone past the root" (a == null). The redirect happens
 *     AFTER a falls off the end (when a IS null), not when a IS the root.
 *     Checking a.parent == null redirects one step too early — the root
 *     itself is never put into the traversal path of the second chain,
 *     causing the algorithm to miss the LCA when it is the root.
 *   Prevention rule:
 *     "The redirect check is: a == null (AFTER falling off the tree),
 *      not a.parent == null. Always trace through an example where the
 *      LCA is the root to verify the redirect fires at the right moment."
 * ═══════════════════════════════════════════════════════════════════════════
 */

} // end class LCAWithParentPointersInterviewGuide

// End of LCAWithParentPointersInterviewGuide.java
