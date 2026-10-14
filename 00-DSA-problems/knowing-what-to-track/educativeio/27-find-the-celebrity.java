/**
 * STANDALONE STUDY NOTE: FIND THE CELEBRITY
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "What is the definition of a celebrity in this party context?" (A person who is known by everyone else 
 *   and knows nobody else).
 * - "What should we return if no celebrity exists?" (Return -1).
 * - "What is the constraint on the number of `knows` API calls?" (We need to keep the calls within 3 * n, 
 *   ruling out O(N^2) full pair-checking loops).
 * - "Can we use the provided matrix graph directly, or must we rely on the conceptual `knows` API?" 
 *   (The problem simulates this via a matrix or API function. We will write our solution assuming the standard 
 *   `knows(a, b)` API method format used in interview environments).
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to find a unique celebrity among `n` people using at most 3 * n calls to the `knows(a, b)` API. 
 * The binding constraint is that comparing every person against every other person takes O(N^2) API calls, 
 * which is too costly; we must use a single-pass elimination strategy.
 * 
 * --- APPROACH 1: BRUTE FORCE (All-Pairs Inspection) ---
 * 1. What I'd naturally try: For every person `i`, check if everyone else knows `i` and if `i` knows nobody. 
 *    This requires checking every pair `(i, j)`.
 * 2. Why it works: It directly tests both celebrity conditions for every candidate.
 * 3. Why it's too slow/costly: It makes O(N^2) calls to the `knows` API, failing the 3 * n query budget constraint.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(N^2) API calls and operations.
 *    - Space Complexity: O(1) auxiliary space.
 * 
 * --- APPROACH 2: OPTIMAL (Two-Phase Candidate Elimination & Verification) ---
 * 1. The Observation: When you ask `knows(a, b)`:
 *    - If `a` knows `b`, `a` *cannot* be the celebrity (since a celebrity knows nobody). We can eliminate `a`.
 *    - If `a` does *not* know `b`, `b` *cannot* be the celebrity (since everyone must know the celebrity). We can eliminate `b`.
 *    Thus, every single `knows` call allows us to disqualify one person!
 * 2. How it leads to the next step:
 *    - Phase 1 (Candidate Finding): Start with `candidate = 0`. Iterate `i` from `1` to `n - 1`. If `knows(candidate, i)` 
 *      is true, update `candidate = i`. By the end of this linear scan (taking `n - 1` calls), we are left with a single 
 *      potential celebrity candidate.
 *    - Phase 2 (Verification): Because the candidate was only survivors of elimination and wasn't fully checked against 
 *      everyone else, we must verify them. Run a second loop over all people to ensure:
 *      1. The candidate knows nobody (`knows(candidate, i)` should be false for all `i != candidate`).
 *      2. Everyone knows the candidate (`knows(i, candidate)` should be true for all `i != candidate`).
 *      If they pass, return the candidate; otherwise return -1.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N) — Phase 1 takes `n - 1` calls, and Phase 2 takes at most `2 * (n - 1)` calls, totaling 
 *      at most `3 * n - 3` API calls, strictly within the 3 * n limit.
 *    - Space Complexity: O(1) — constant extra space.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Minimum party size ($n = 2$): Handled correctly by single-pass elimination.
 * - No celebrity exists (e.g., everyone knows someone or forms a cycle): Phase 2 verification catches it and returns -1.
 * - Celebrity is at index 0: Candidate starts at 0 and survives Phase 1.
 * - Celebrity is at the very end ($n - 1$): Candidate shifts incrementally through the loop.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Elimination Property:
 * 
 * Ask: `knows(A, B)`
 * 
 * Path 1: Returns TRUE (A knows B)
 * -> A knows someone, so A CANNOT be a celebrity. Eliminate A, set Candidate = B.
 * 
 * Path 2: Returns FALSE (A does not know B)
 * -> B is not known by A, so B CANNOT be a celebrity. Eliminate B, keep Candidate = A.
 * 
 * Result: Exactly 1 person survives after N - 1 comparisons!
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: n = 3, graph represented via knows API where person 1 is the celebrity.
 * (0 knows 1, 2 knows 1, 1 knows nobody)
 * 
 * - Phase 1: Find Candidate
 *   - Initialize `candidate = 0`.
 *   - i = 1: Check `knows(0, 1)` -> Returns TRUE (0 knows 1). 
     - Update `candidate = 1`.
 *   - i = 2: Check `knows(1, 2)` -> Returns FALSE (1 does not know 2). 
     - Keep `candidate = 1`.
 *   - End of Phase 1. Surviving candidate = 1.
 * 
 * - Phase 2: Verify Candidate (1)
 *   - i = 0: 
     - Check `knows(1, 0)` -> FALSE (correct, celebrity knows no one).
     - Check `knows(0, 1)` -> TRUE (correct, everyone knows celebrity).
 *   - i = 2: 
     - Check `knows(1, 2)` -> FALSE.
     - Check `knows(2, 1)` -> TRUE.
 *   - All checks passed! Return candidate `1`.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "To find the celebrity within the 3 * n API call limit, we can use a two-phase elimination algorithm. 
 * In Phase 1, we find a single potential celebrity candidate in linear time: starting with candidate 0, 
 * we compare them against each person from 1 to n - 1 using the `knows` API. Every query allows us to eliminate 
 * one non-celebrity, taking exactly n - 1 calls. In Phase 2, because our candidate is only a survivor rather 
 * than verified, we loop through all other people to verify that the candidate knows nobody and that everyone 
 * knows the candidate, taking at most 2 * n additional calls. This gives us O(N) time complexity using at most 
 * 3 * n - 3 API calls and O(1) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if there could be *multiple* celebrities at the party?
 * A: By definition of the problem constraints and graph properties, there can be at most one celebrity 
 *    (if two existed, they would have to know each other, violating the condition that a celebrity knows nobody).
 * 
 * Q: Can this be implemented recursively?
 * A: Yes, we can divide and conquer by finding a celebrity in the first half of people and filtering, 
 *    though the iterative elimination approach is cleaner and avoids recursion stack overhead.
 */

public class FindTheCelebrity extends Relation {

    /**
     * APPROACH 2: OPTIMAL (Two-Phase Candidate Elimination & Verification)
     * Time: O(N) API calls, Space: O(1)
     */
    public int findCelebrity(int n) {
        int candidate = 0;

        // Phase 1: Find the single potential celebrity candidate
        // Each query knows(candidate, i) eliminates one person.
        for (int i = 1; i < n; i++) {
            if (knows(candidate, i)) {
                // [What it does]: If candidate knows i, candidate cannot be a celebrity. 
                // So, i becomes our new candidate.
                candidate = i;
            }
        }

        // Phase 2: Verify whether the surviving candidate is a true celebrity
        for (int i = 0; i < n; i++) {
            if (candidate == i) {
                continue; // Do not check if person knows themselves
            }

            // [Why needed]: A celebrity must satisfy two mandatory conditions:
            // 1. The candidate must NOT know person i (`knows(candidate, i)` must be false).
            // 2. Person i MUST know the candidate (`knows(i, candidate)` must be true).
            if (knows(candidate, i) || !knows(i, candidate)) {
                return -1;
            }
        }

        return candidate;
    }

    /**
     * SUMMARY
     * 1. Core pattern: Two-phase candidate elimination (Phase 1: O(N) reduction, Phase 2: O(N) verification).
     * 2. Key observation: Every `knows(a, b)` call mathematically disqualifies either `a` or `b`, reducing the search space instantly.
     * 3. Memorize vs Understand: Understand why verification is mandatory (Phase 1 only guarantees a survivor, not a proven celebrity).
     * 4. Most common trap: Forgetting to skip self-checks (`candidate == i`) during the verification phase.
     * 5. Mental trigger: "Find the celebrity with min API calls" -> Phase 1 elimination loop + Phase 2 verification loop.
     */
    public static void main(String[] args) {
        // Mock test execution structure (Relation class provides knows(a, b))
        FindTheCelebrity solver = new FindTheCelebrity();
        System.out.println("Study note compiled successfully. Ready for test execution.");
    }
}

/**
 * Dummy Relation superclass to satisfy compilation in standalone study notes.
 * In platform environments (like LeetCode), this is provided externally.
 */
class Relation {
    boolean knows(int a, int b) {
        // Placeholder implementation for compilation
        return false;
    }
}


class Solution {

    // The knows(a, b) API is provided by the problem platform.
    // It returns true if person a knows person b.

    public int findCelebrity(int n) {

        // PHASE 1: Find a potential celebrity.
        // Initially, assume person 0 is the celebrity.
        int candidate = 0;

        // Compare the current candidate with every other person.
        for (int person = 1; person < n; person++) {

            // If candidate knows person, candidate cannot be a celebrity.
            // A celebrity must know nobody else.
            if (knows(candidate, person)) {

                // Eliminate the old candidate and select this person.
                candidate = person;
            }

            // Otherwise, the current candidate does not know person.
            // So person cannot be a celebrity: a celebrity must be known
            // by everyone, including the current candidate.
        }

        // PHASE 2: Verify that the remaining candidate is a celebrity.
        for (int person = 0; person < n; person++) {

            // A person does not need to be compared with themselves.
            if (person == candidate) {
                continue;
            }

            // Condition 1: The candidate must know nobody else.
            if (knows(candidate, person)) {
                return -1;
            }

            // Condition 2: Everyone else must know the candidate.
            if (!knows(person, candidate)) {
                return -1;
            }
        }

        // Both conditions hold for every other person.
        return candidate;
    }
}

