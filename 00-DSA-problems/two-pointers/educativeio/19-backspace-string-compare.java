import java.util.ArrayDeque;
import java.util.Deque;

/*
================================================================================
 GOOGLE-STYLE MOCK INTERVIEW: "Backspace String Compare"
 (LeetCode 844 — https://leetcode.com/problems/backspace-string-compare/)
================================================================================
*/

public class BackspaceCompare {

    /*
    ============================================================================
     SECTION 1: RESTATE THE PROBLEM
    ============================================================================
     In plain English:

     I'm given two strings, s and t, made up of lowercase letters and the
     character '#'. I need to simulate typing each string into a text editor
     one character at a time, where '#' acts as a backspace key: it deletes
     the most recently typed character. If the editor is already empty and I
     hit '#', nothing happens (it's a no-op, not an error).

     After "typing" both strings, I compare the two final editor contents.
     I return true if they are identical, false otherwise.

     Inputs:
       - s, t: two strings, each containing lowercase English letters 'a'-'z'
         and the character '#'.

     Output:
       - boolean: true if the final rendered text of s equals the final
         rendered text of t, false otherwise.

     Implicit assumptions to confirm:
       - '#' is the ONLY special/control character; letters are just data.
       - Backspacing on an empty buffer is a no-op (given explicitly in the
         prompt), not an exception or error state.
       - We compare the *rendered result*, not the raw input strings.
    ============================================================================
    */


    /*
    ============================================================================
     SECTION 2: CLARIFYING QUESTIONS (with assumed answers)
    ============================================================================
     1. Q: What is the character set — only lowercase letters and '#', or
           could there be uppercase, digits, or other symbols?
        A (assumed): Only lowercase English letters 'a'-'z' and '#', per the
           classic LeetCode constraints.

     2. Q: What are the length bounds on s and t? Could they be empty?
        A (assumed): 1 <= s.length, t.length <= 200. Either string (or both)
           could theoretically be empty; I'll handle length-0 gracefully
           regardless.

     3. Q: Can the strings contain other editor commands, like Home/End/Delete
           (forward-delete), or is '#' backspace the only special key?
        A (assumed): '#' is the only special key. No forward-delete, no
           cursor movement.

     4. Q: Is the comparison case-sensitive?
        A (assumed): Yes — this is a straightforward character-by-character
           equality check on the final buffer; no case normalization.

     5. Q: Should I return the final strings themselves, or just a boolean?
        A (assumed): Just a boolean, per the problem statement. I'll expose
           an internal helper that builds the final string, useful for
           debugging/testing, but the public contract returns boolean.

     6. Q: Is there a follow-up expectation around space complexity (i.e.,
           should I aim for O(1) extra space instead of building new
           strings)?
        A (assumed): Yes — Google interviewers will very likely ask for the
           O(1) space follow-up, so I should be ready to go from the
           straightforward O(n+m) space simulation to a two-pointer O(1)
           space solution.

     7. Q: Do I need to worry about concurrency or streaming input (i.e.,
           characters arriving one at a time from a live typing session)?
        A (assumed): No — both strings are provided fully upfront as static
           input. No concurrency concerns.

     8. Q: Are there multiple consecutive backspaces I need to worry about,
           e.g., "##" or backspacing past the start of the string?
        A (assumed): Yes, this is exactly the trickiest part of the problem —
           consecutive '#' characters must "stack" their deletion effect, and
           if the count of pending backspaces exceeds the number of letters
           typed so far, the buffer simply bottoms out at empty (no error).
    ============================================================================
    */


    /*
    ============================================================================
     SECTION 3: EXAMPLES & EDGE CASES
    ============================================================================

     Example 1 (Normal case):
       s = "ab#c"   -> type 'a','b', then '#' deletes 'b' -> "a", then 'c' -> "ac"
       t = "ad#c"   -> type 'a','d', then '#' deletes 'd' -> "a", then 'c' -> "ac"
       "ac" == "ac" -> return true

     Example 2 (Edge case — over-backspacing / empty result):
       s = "a#c"    -> 'a' -> "a", '#' deletes 'a' -> "", 'c' -> "c"
       t = "b"      -> "b"
       "c" != "b"   -> return false

       Sub-case: over-backspacing past empty buffer
       s = "###a"   -> three backspaces on an empty buffer are no-ops -> "", then 'a' -> "a"
       t = "a"      -> "a"
       "a" == "a"   -> return true

     Example 3 (Boundary / tie-breaking case — both strings reduce to empty):
       s = "a#b#"   -> 'a' -> "a", '#' -> "", 'b' -> "b", '#' -> ""
       t = "###"    -> all no-ops on empty buffer -> ""
       "" == ""     -> return true

       This confirms that two strings which are structurally very different
       can still be equal if they both fully collapse to the empty string —
       a common trap if a candidate assumes "different lengths => false"
       without actually simulating.
    ============================================================================
    */


    /*
    ============================================================================
     SECTION 4 & 5: ALL POSSIBLE APPROACHES
    ============================================================================
     Paradigm sweep — which categories apply?

       - Brute force / naive simulation .......... YES (Approach 1)
       - Sorting-based ............................ NOT APPLICABLE — order of
             characters is semantically meaningful (typing order); sorting
             destroys the information we need to simulate backspaces.
       - Hashing-based ............................. NOT APPLICABLE (directly)
             — we could hash the two final strings for O(1) comparison, but
             we still have to build the final strings first, so hashing adds
             no algorithmic benefit over direct simulation; not a distinct
             approach.
       - Two pointer / sliding window .............. YES (Approach 3 — optimal)
       - Divide and conquer ........................ NOT NATURALLY APPLICABLE
             — a '#' can cancel a letter from anywhere earlier in the string,
             so the effect of the left half is not independent of how many
             pending backspaces "leak in" from the right half. You'd need to
             pass backspace-carry state across the merge boundary, which
             just reduces to the same sequential scan with extra bookkeeping
             and no asymptotic benefit.
       - Greedy .................................... YES, in the sense that
             Approach 3 greedily resolves each character from the right end
             (the only direction where a character's fate is immediately
             knowable without lookahead).
       - Dynamic programming ........................ NOT APPLICABLE — there
             is no overlapping-subproblem / optimal-substructure question
             being asked (no "count ways" or "min/max cost"); it's a direct
             deterministic simulation.
       - Tree / graph traversal ..................... NOT APPLICABLE — no
             hierarchical or relational structure in the input.
       - Heap / priority queue ...................... NOT APPLICABLE — no
             ordering-by-priority requirement.
       - Binary search ............................... NOT APPLICABLE — no
             monotonic search space to exploit.
       - Monotonic stack / deque .................... YES (Approach 1 — a
             plain stack, since letters are pushed and popped in LIFO order
             exactly matching backspace semantics; not "monotonic" in the
             classic sense, just a stack).
       - Trie / segment tree / advanced structures .. NOT APPLICABLE — no
             prefix-sharing or range-query need.

     So the three approaches worth presenting are:
       Approach 1: Stack Simulation (Deque as an explicit stack)
       Approach 2: StringBuilder Simulation (stack semantics via mutable buffer)
       Approach 3: Two-Pointer From the Right (optimal, O(1) space)
    ============================================================================
    */

    /*
    ----------------------------------------------------------------------------
     Approach 1: Stack Simulation (explicit Deque<Character> as a stack)
    ----------------------------------------------------------------------------
     Core idea:
       Walk each string left to right. Maintain a stack. On a letter, push it.
       On '#', pop the top if the stack is non-empty (no-op if empty). After
       processing both strings into their own stacks, compare stack contents
       for equality.

     Data structure / paradigm: Stack (LIFO), direct simulation — this is the
     most literal translation of "typing into an editor" into code, so it is
     the natural brute-force / naive starting point.

     Time Complexity: O(n + m) — each character of each string is pushed and
       popped at most once.
     Space Complexity: O(n + m) — in the worst case (no backspaces), every
       character ends up on the stack.

     Pros:
       - Extremely easy to reason about and explain; mirrors the problem
         statement almost line for line.
       - Low risk of subtle bugs.

     Cons:
       - Uses extra space proportional to input size for BOTH strings, even
         though we ultimately only need a boolean answer.
       - Deque<Character> in Java boxes each char, adding real constant-factor
         overhead versus primitive-based structures.

     When to use: Good as your first "let me get something correct" solution
       to state out loud in an interview, or when clarity/maintainability
       matters more than squeezing out the last bit of performance (e.g., a
       one-off script, not a hot path).
    ----------------------------------------------------------------------------
    */
    public static boolean isSameStack(String s, String t) {
        return buildFinalStringUsingStack(s).equals(buildFinalStringUsingStack(t));
    }

    // Helper: simulate typing `input` into an editor using an explicit stack,
    // and return the resulting rendered text.
    private static String buildFinalStringUsingStack(String input) {
        Deque<Character> charStack = new ArrayDeque<>();
        for (int index = 0; index < input.length(); index++) {
            char currentChar = input.charAt(index);
            if (currentChar != '#') {
                charStack.push(currentChar);
            } else if (!charStack.isEmpty()) {
                // Backspace: only pop if there is something to delete.
                charStack.pop();
            }
            // If currentChar == '#' and stack is empty, it's a no-op.
        }

        // Stack pops in reverse order, so build the string back-to-front.
        StringBuilder renderedText = new StringBuilder(charStack.size());
        while (!charStack.isEmpty()) {
            renderedText.append(charStack.pop());
        }
        return renderedText.reverse().toString();
    }


    /*
    ----------------------------------------------------------------------------
     Approach 2: StringBuilder Simulation (mutable buffer used as a stack)
    ----------------------------------------------------------------------------
     Core idea:
       Functionally identical to Approach 1, but instead of a boxed
       Deque<Character>, use a StringBuilder as the "stack": appending a
       letter is push, and deleting the last character
       (deleteCharAt(length - 1)) is pop. This keeps everything on a single
       contiguous char[] buffer internally, avoiding per-character boxing
       and node allocation.

     Data structure / paradigm: Same LIFO / stack simulation, just with a
       more cache-friendly, primitive-backed data structure.

     Time Complexity: O(n + m) — same reasoning as Approach 1; each character
       causes at most one append or one deleteCharAt, and deleteCharAt at the
       *end* of a StringBuilder is O(1) (no shifting required).
     Space Complexity: O(n + m) — still building full rendered strings for
       both inputs.

     Pros:
       - Same simplicity as Approach 1 but noticeably faster in practice due
         to avoiding Character boxing and Deque node overhead.
       - The resulting rendered string is directly usable/printable, which is
         nice for debugging.

     Cons:
       - Still O(n + m) auxiliary space — doesn't address the O(1) space
         follow-up an interviewer will likely ask about.

     When to use: A solid "production-quality but simple" middle ground —
       use this over Approach 1 when performance matters a bit but you still
       want very readable, low-risk code and don't need the absolute minimum
       memory footprint.
    ----------------------------------------------------------------------------
    */
    public static boolean isSameStringBuilder(String s, String t) {
        String renderedS = buildFinalStringUsingStringBuilder(s).toString();
        String renderedT = buildFinalStringUsingStringBuilder(t).toString();
        return renderedS.equals(renderedT);
    }

    // Helper: simulate typing `input` using a StringBuilder as a stack.
    private static StringBuilder buildFinalStringUsingStringBuilder(String input) {
        StringBuilder buffer = new StringBuilder(input.length());
        for (int index = 0; index < input.length(); index++) {
            char currentChar = input.charAt(index);
            if (currentChar != '#') {
                buffer.append(currentChar);
            } else if (buffer.length() > 0) {
                // Delete from the end: O(1), no shifting needed.
                buffer.deleteCharAt(buffer.length() - 1);
            }
        }
        return buffer;
    }


    /*
    ----------------------------------------------------------------------------
     Approach 3: Two-Pointer From the Right (OPTIMAL — O(1) extra space)
    ----------------------------------------------------------------------------
     Core idea:
       The key structural insight: a character's fate (does it survive to the
       final rendered text, or does it get deleted?) can only be determined
       by looking at what comes AFTER it, never what comes before it. That
       means if we scan from the RIGHT end of the string, the moment we reach
       a letter we already know — from a running backspace counter — whether
       it survives or gets consumed. This lets us walk both strings
       simultaneously from their right ends, "resolving" one valid character
       at a time from each, and compare them on the fly without ever
       materializing the full rendered strings.

       For each string, maintain a pointer starting at the last index and a
       `pendingBackspaces` counter:
         - If the current character is '#', increment pendingBackspaces and
           move the pointer left.
         - Else if pendingBackspaces > 0, this letter is consumed by a
           pending backspace: decrement pendingBackspaces, move pointer left.
         - Else, this letter survives — it's the next character of the
           rendered string, walking right-to-left.

       Advance both strings' pointers in lockstep to their next surviving
       character and compare those characters. If they differ (or one string
       runs out while the other still has a surviving character), return
       false immediately. If both pointers are exhausted simultaneously,
       the strings matched all the way through -> return true.

     Data structure / paradigm: Two-pointer / greedy right-to-left resolution.
       No auxiliary data structure needed beyond a couple of integer
       counters — this is why it achieves O(1) space.

     Time Complexity: O(n + m) — each pointer moves strictly leftward across
       its own string exactly once; total work is linear in the combined
       input length.
     Space Complexity: O(1) — only a fixed number of integer index/counter
       variables, no matter how long s and t are.

     Pros:
       - Optimal on both time and space; this is the answer a Google
         interviewer wants to see you *arrive at*, even if you start with
         Approach 1/2.
       - Short-circuits on the first mismatch — can be faster in practice
         than building full strings when strings diverge early.

     Cons:
       - Meaningfully trickier to get right on a whiteboard: the "resolve
         next surviving character" logic as its own reusable step is easy to
         get subtly wrong (e.g., forgetting the pending-backspace decrement
         order, or mishandling the loop's terminating condition).
       - Slightly harder to explain/justify correctness quickly compared to
         the very literal stack simulation.

     When to use: This is the version to write as your final answer in an
       interview once you've talked through the simpler simulation first —
       it demonstrates the ability to recognize and exploit problem
       structure to drop auxiliary space from O(n) to O(1).
    ----------------------------------------------------------------------------
    */
    public static boolean isSameTwoPointer(String s, String t) {
        int indexInS = s.length() - 1;
        int indexInT = t.length() - 1;

        while (indexInS >= 0 || indexInT >= 0) {
            // Advance each pointer to its next surviving ("resolved") character.
            indexInS = nextValidCharIndex(s, indexInS);
            indexInT = nextValidCharIndex(t, indexInT);

            boolean sExhausted = indexInS < 0;
            boolean tExhausted = indexInT < 0;

            if (sExhausted && tExhausted) {
                // Both strings fully resolved with no mismatches found -> match.
                return true;
            }
            if (sExhausted != tExhausted) {
                // One string still has a surviving character, the other doesn't.
                return false;
            }
            if (s.charAt(indexInS) != t.charAt(indexInT)) {
                return false;
            }

            // Both surviving characters matched; move past them for next round.
            indexInS--;
            indexInT--;
        }
        return true;
    }

    // Helper: starting at `startIndex` and moving leftward, skip over any
    // character that gets deleted by a backspace, and return the index of
    // the next character that actually survives in the rendered text
    // (or -1 if the string is exhausted with no surviving character left).
    private static int nextValidCharIndex(String text, int startIndex) {
        int pendingBackspaces = 0;
        int currentIndex = startIndex;

        while (currentIndex >= 0) {
            char currentChar = text.charAt(currentIndex);
            if (currentChar == '#') {
                pendingBackspaces++;
                currentIndex--;
            } else if (pendingBackspaces > 0) {
                // This letter is cancelled out by a pending backspace.
                pendingBackspaces--;
                currentIndex--;
            } else {
                // Found a letter that survives -> this is our next char.
                break;
            }
        }
        return currentIndex;
    }


    /*
    ============================================================================
     SECTION 7: APPROACHES COMPARISON TABLE
    ============================================================================
     Approach                     | Time     | Space  | Best For                          | Limitations
     ------------------------------------------------------------------------------------------------------------------------
     1. Stack Simulation          | O(n+m)   | O(n+m) | Clarity; first-pass correctness   | Extra space; Character boxing
        (Deque<Character>)        |          |        | check; teaching/explaining logic  | overhead vs primitives
     ------------------------------------------------------------------------------------------------------------------------
     2. StringBuilder Simulation  | O(n+m)   | O(n+m) | Simple + faster constant factor;  | Still O(n+m) space; doesn't
                                   |          |        | debuggable (prints final string)  | satisfy O(1)-space follow-up
     ------------------------------------------------------------------------------------------------------------------------
     3. Two-Pointer From Right    | O(n+m)   | O(1)   | Optimal production solution;      | Trickier to derive/explain
        (OPTIMAL)                 |          |        | early short-circuit on mismatch   | correctly under time pressure
    ============================================================================
    */


    /*
    ============================================================================
     SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
    ============================================================================
     I would present this in stages, exactly as a strong candidate should:

       1. State Approach 1 (stack simulation) out loud first, in ~20 seconds,
          to prove I understand the problem and have a correct O(n+m)/O(n+m)
          solution ready as a fallback. I would NOT necessarily write all of
          it out if time is tight — a one-line description often suffices to
          signal "I have a working solution in my pocket."
       2. Immediately flag the O(1) space opportunity myself, before the
          interviewer has to ask ("Since a character's survival only depends
          on what comes after it, I can resolve both strings from the right
          end with two pointers and avoid building the rendered strings at
          all") — this is exactly the kind of proactive optimization framing
          Google interviewers reward.
       3. Write Approach 3 (two-pointer from the right) as my final, fully
          coded solution, since it is optimal in both time and space,
          short-circuits early on mismatches, and demonstrates the ability
          to exploit problem structure rather than just pattern-matching to
          "use a stack."

     Why not Approach 2 as the final answer? It's a fine incremental
     improvement over Approach 1, but it doesn't change the asymptotic space
     complexity, so presenting it as my "final" answer would leave the most
     interesting optimization (O(1) space) undiscovered — exactly what a
     Google interviewer is listening for.
    ============================================================================
    */


    /*
    ============================================================================
     SECTION 9: DEEP DIVE — POLISHED, PRODUCTION-QUALITY OPTIMAL SOLUTION
    ============================================================================
     Below is a hardened version of Approach 3 intended to look like something
     I'd actually ship: defensive null-handling, precise variable naming, and
     comments that explain *why*, not just *what*.
    ============================================================================
    */
    public static boolean backspaceCompare(String s, String t) {
        // Defensive handling: treat null as an invalid argument rather than
        // silently misbehaving — fail fast with a clear signal.
        if (s == null || t == null) {
            throw new IllegalArgumentException("Input strings must not be null.");
        }

        // Pointers start at the last index of each string and only ever move
        // leftward. We never allocate any buffer proportional to input size.
        int pointerInS = s.length() - 1;
        int pointerInT = t.length() - 1;

        // Loop until BOTH pointers have been fully consumed. We can't stop
        // as soon as one is exhausted, because we still need to confirm the
        // other string has no more surviving characters either (asymmetric
        // exhaustion means the strings differ in length after backspacing).
        while (pointerInS >= 0 || pointerInT >= 0) {
            pointerInS = advanceToNextSurvivingChar(s, pointerInS);
            pointerInT = advanceToNextSurvivingChar(t, pointerInT);

            boolean sHasNoMoreChars = pointerInS < 0;
            boolean tHasNoMoreChars = pointerInT < 0;

            // Case 1: both strings ran out of surviving characters at the
            // same time -> everything matched so far -> strings are equal.
            if (sHasNoMoreChars && tHasNoMoreChars) {
                return true;
            }

            // Case 2: exactly one string still has a surviving character.
            // Different effective lengths -> strings cannot be equal.
            if (sHasNoMoreChars != tHasNoMoreChars) {
                return false;
            }

            // Case 3: both have a surviving character -> they must match.
            if (s.charAt(pointerInS) != t.charAt(pointerInT)) {
                return false;
            }

            // This position matched; step both pointers past it and continue.
            pointerInS--;
            pointerInT--;
        }

        // Both strings were fully drained without any mismatch.
        return true;
    }

    /**
     * Moves {@code startIndex} leftward through {@code text}, resolving and
     * skipping any character consumed by a backspace, and returns the index
     * of the next character that actually survives into the rendered text.
     *
     * @param text       the raw typed string (letters and '#')
     * @param startIndex the index to begin scanning from, moving leftward
     * @return the index of the next surviving character, or -1 if none remain
     */
    private static int advanceToNextSurvivingChar(String text, int startIndex) {
        int pendingBackspaceCount = 0;
        int scanIndex = startIndex;

        while (scanIndex >= 0) {
            char character = text.charAt(scanIndex);
            if (character == '#') {
                pendingBackspaceCount++;
                scanIndex--;
            } else if (pendingBackspaceCount > 0) {
                pendingBackspaceCount--;
                scanIndex--;
            } else {
                // This character has no pending backspace to consume it.
                return scanIndex;
            }
        }
        // Ran off the start of the string with no surviving character left.
        return -1;
    }


    /*
    ============================================================================
     SECTION 10: DRY RUN / TRACE
    ============================================================================
     Tracing backspaceCompare(s = "ab#c", t = "ad#c") using the optimal
     two-pointer solution.

     Initial: pointerInS = 3 ('c'), pointerInT = 3 ('c')

     --- Round 1 ---
     advanceToNextSurvivingChar(s, 3):
       scanIndex=3, char='c', pendingBackspaceCount=0 -> not '#', no pending
         backspace -> return 3 immediately.
     advanceToNextSurvivingChar(t, 3):
       scanIndex=3, char='c' -> return 3 immediately.
     pointerInS = 3, pointerInT = 3
     sHasNoMoreChars=false, tHasNoMoreChars=false
     s.charAt(3)='c', t.charAt(3)='c' -> match.
     Decrement: pointerInS = 2, pointerInT = 2

     --- Round 2 ---
     advanceToNextSurvivingChar(s, 2):
       scanIndex=2, char='#', pendingBackspaceCount=1, scanIndex=1
       scanIndex=1, char='b', pendingBackspaceCount=1>0 -> consumed,
         pendingBackspaceCount=0, scanIndex=0
       scanIndex=0, char='a', pendingBackspaceCount=0 -> return 0.
     advanceToNextSurvivingChar(t, 2):
       scanIndex=2, char='#', pendingBackspaceCount=1, scanIndex=1
       scanIndex=1, char='d', pendingBackspaceCount=1>0 -> consumed,
         pendingBackspaceCount=0, scanIndex=0
       scanIndex=0, char='a', pendingBackspaceCount=0 -> return 0.
     pointerInS = 0, pointerInT = 0
     s.charAt(0)='a', t.charAt(0)='a' -> match.
     Decrement: pointerInS = -1, pointerInT = -1

     --- Round 3 ---
     Loop condition (pointerInS >= 0 || pointerInT >= 0) is now false
       (-1 >= 0 is false for both) -> loop exits.
     Return true.

     Final state: pointerInS = -1, pointerInT = -1 -> result: true
     This matches our expected rendered strings: s -> "ac", t -> "ac".
    ============================================================================
    */


    /*
    ============================================================================
     SECTION 11: CLOSING SUMMARY
    ============================================================================
     - All three approaches are linear in time, O(n + m); they differ only in
       auxiliary space.
     - Approach 1 (stack) and Approach 2 (StringBuilder) both materialize the
       fully rendered strings, costing O(n + m) space; Approach 2 is simply a
       lower-constant-factor version of the same idea.
     - Approach 3 (two-pointer from the right) is strictly better: same O(n+m)
       time, but O(1) space, plus the ability to short-circuit as soon as a
       mismatch is found, without ever finishing the rendering of either
       string.
     - Known assumptions baked into the final solution: input consists only
       of lowercase letters and '#'; null inputs are rejected explicitly
       rather than silently mishandled; backspacing an empty buffer is a
       no-op, per the problem statement.
     - A known limitation: the recursion-free, iterative two-pointer
       implementation trades a small amount of readability for its O(1)
       space guarantee — worth explicitly calling out in an interview so the
       interviewer knows this is a deliberate, understood trade-off and not
       an accident.
    ============================================================================
    */


    /*
    ============================================================================
     SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
    ============================================================================
     1. "What if the alphabet also included a forward-delete or 'select all
         and delete' command?"
        -> I'd need a richer simulation (likely still a stack-based approach
           for forward-delete-from-cursor semantics is harder to do in O(1)
           space; may need to fall back to full simulation with a real
           cursor position tracked, not just a stack).

     2. "What if s and t could be extremely long (e.g., 10^9 characters) and
         streamed in rather than fully available upfront?"
        -> The two-pointer-from-the-right approach requires random access
           from the end, which doesn't work on a forward-only stream. I'd
           need a different strategy: process the stream left to right with
           an explicit stack (Approach 1's style), but that reintroduces
           O(n) space; true O(1)-space streaming backspace comparison isn't
           generally possible without buffering, since a backspace can
           cancel an arbitrarily distant earlier character.

     3. "Can you do this without extra space AND without random access to
         the string (e.g., if it were a singly-linked list of characters)?"
        -> Right-to-left scanning wouldn't be free on a singly-linked list
           (no O(1) previous-node access), so I'd likely reverse-traverse
           via recursion (implicit stack, O(n) space) or explicitly build a
           doubly-linked structure — worth discussing the trade-off.

     4. "How would you extend this to return the two final rendered strings,
         not just a boolean?"
        -> Use Approach 2 (StringBuilder simulation) directly, since building
           the actual output requires materializing it — the O(1)-space
           trick only helps when the boolean answer alone is needed.

     5. "What if there could be multiple different special characters (e.g.,
         '#' = backspace one char, '@' = clear entire line)?"
        -> Generalize the stack simulation: '#' pops one, '@' clears the
           whole stack. The two-pointer optimization becomes significantly
           harder because '@' invalidates an unbounded, position-dependent
           number of earlier characters in one step, breaking the simple
           "counter" trick.

     6. "Can you prove the two-pointer approach's correctness more formally?"
        -> Argue by induction from the right: the last character of the
           rendered string is exactly the last character of the raw input
           that is not "cancelled" by a net-positive count of trailing '#'
           characters. Each `advanceToNextSurvivingChar` call establishes
           this invariant for the current suffix, and induction extends it
           leftward one resolved character at a time.
    ============================================================================
    */


    /*
    ============================================================================
     SECTION 13: WHAT CANDIDATES TYPICALLY MISS
    ============================================================================
     1. Forgetting that backspacing an empty buffer is a no-op, not an error
        or a "delete the last '#' itself" — candidates sometimes write
        pop-without-a-guard and get ArrayDeque/Stack EmptyStackException-style
        bugs, or (worse) silently corrupt state in an off-by-one way.

     2. Assuming different string lengths automatically mean "not equal"
        without simulating — as Example 3 shows ("a#b#" vs "###"), very
        different-looking raw strings can still render identically.

     3. In the two-pointer approach, exiting the main loop the moment ONE
        pointer goes negative instead of continuing until BOTH are exhausted
        — this misses the case where one string still has a surviving
        character and the other has none, which should be a mismatch (false),
        not a premature "true".

     4. Off-by-one / ordering bugs in the pending-backspace counter logic:
        incrementing pendingBackspaceCount for '#' vs. decrementing it when
        consuming a letter must happen in the right branch order, and the
        pointer must move left in *every* branch of the inner loop (both
        when hitting '#' and when a letter gets cancelled) — forgetting to
        decrement the index in one branch causes an infinite loop.
    ============================================================================
    */


    /*
    ============================================================================
     MAIN METHOD — CROSS-VALIDATE ALL THREE APPROACHES ON MULTIPLE TEST CASES
    ============================================================================
    */
    public static void main(String[] args) {
        String[][] testCases = {
            {"ab#c", "ad#c"},     // Example 1: normal case -> true
            {"a#c", "b"},         // Example 2a: -> false
            {"###a", "a"},        // Example 2b: over-backspacing -> true
            {"a#b#", "###"},      // Example 3: both collapse to empty -> true
            {"", ""},             // both empty -> true
            {"#", ""},            // backspace on empty -> true
            {"ab##", "c#d#"},     // both collapse to empty -> true
            {"a##c", "#a#c"},     // classic LeetCode edge case -> true
            {"bxj##tw", "bxo#j##tw"}, // classic LeetCode edge case -> true
            {"bxj##tw", "bxj###tw"},  // classic LeetCode edge case -> false
            {"xywrrmp", "xywrrmu#p"}, // classic LeetCode edge case -> true
        };

        boolean[] expected = {
            true, false, true, true, true, true, true, true, true, false, true
        };

        for (int i = 0; i < testCases.length; i++) {
            String s = testCases[i][0];
            String t = testCases[i][1];

            boolean resultStack = isSameStack(s, t);
            boolean resultStringBuilder = isSameStringBuilder(s, t);
            boolean resultTwoPointer = isSameTwoPointer(s, t);
            boolean resultOptimal = backspaceCompare(s, t);

            boolean allAgree = resultStack == resultStringBuilder
                    && resultStringBuilder == resultTwoPointer
                    && resultTwoPointer == resultOptimal;

            boolean matchesExpected = resultOptimal == expected[i];

            System.out.printf(
                "Test %2d: s=%-12s t=%-12s | stack=%-5b sb=%-5b twoPtr=%-5b optimal=%-5b | allAgree=%b matchesExpected=%b%n",
                i + 1, "\"" + s + "\"", "\"" + t + "\"",
                resultStack, resultStringBuilder, resultTwoPointer, resultOptimal,
                allAgree, matchesExpected
            );

            if (!allAgree || !matchesExpected) {
                throw new AssertionError("Mismatch detected on test case " + (i + 1));
            }
        }

        System.out.println("\nAll approaches agree on all test cases. ✅");
    }
}
