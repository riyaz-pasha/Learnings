/*
 * =============================================================================
 * TABLE OF CONTENTS  (filled in after final write — see line numbers below)
 * =============================================================================
 *   1. PROBLEM RESTATEMENT ................................. line  TOC_1
 *   2. CLARIFYING QUESTIONS ................................. line  TOC_2
 *   3. EXAMPLES & EDGE CASES ................................ line  TOC_3
 *   4. APPROACH 1: TOKENIZE-THEN-VALIDATE (NAIVE) ........... line  TOC_4
 *   5. APPROACH 2: TWO-POINTER SINGLE PASS (OPTIMAL) ........ line  TOC_5
 *   6. APPROACH 3: REGEX-BASED PATTERN MATCH ................ line  TOC_6
 *   7. PARADIGMS CONSIDERED AND RULED OUT .................... line  TOC_7
 *   8. APPROACHES COMPARISON TABLE ........................... line  TOC_8
 *   9. RECOMMENDED APPROACH FOR INTERVIEW ..................... line  TOC_9
 *  10. DEEP DIVE: OPTIMAL SOLUTION (PRODUCTION QUALITY) ....... line  TOC_10
 *  11. DRY RUN / TRACE ........................................ line  TOC_11
 *  12. CLOSING SUMMARY ........................................ line  TOC_12
 *  13. FOLLOW-UP QUESTIONS .................................... line  TOC_13
 *  14. WHAT CANDIDATES TYPICALLY MISS ......................... line  TOC_14
 *  15. MAIN / DEMO HARNESS .................................... line  TOC_15
 * =============================================================================
 */

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/*
 * =============================================================================
 * SECTION 1: RESTATE THE PROBLEM
 * =============================================================================
 *
 * In my own words:
 *
 *   We are given a target string `word` and a candidate abbreviation `abbr`.
 *   An abbreviation is built by picking zero or more NON-EMPTY, NON-ADJACENT
 *   substrings of `word` and replacing each one with its length (as a decimal
 *   number). "Non-adjacent" means two replaced substrings can never be next
 *   to each other in `word` without at least one un-replaced letter between
 *   them -- otherwise the two numbers would be ambiguous when written next
 *   to each other in `abbr` (e.g. "1" + "2" written back-to-back would look
 *   like the single number "12"). Consequently, any RUN of consecutive
 *   digits in `abbr` is always ONE number, never two numbers concatenated.
 *
 *   Given `abbr`, I must decide: is there a way to have produced `abbr` from
 *   `word` using this replacement rule?
 *
 *   Key constraints / rules called out explicitly by the prompt:
 *     - `abbr` contains only lowercase letters and digits.
 *     - Every letter in `abbr` must match the character at the corresponding
 *       position in `word` exactly.
 *     - Every digit-run in `abbr` represents "skip this many characters in
 *       word" (i.e., that many original characters were replaced).
 *     - A digit-run must NOT have a leading zero. "0" alone is also illegal
 *       (it would mean replacing an empty substring, which is disallowed).
 *     - `abbr` must account for ALL of `word`, left to right -- not too few
 *       characters, not too many. Both pointers must simultaneously reach
 *       the end.
 *
 *   Input:  word (String, lowercase letters), abbr (String, lowercase
 *           letters + digits '0'-'9')
 *   Output: boolean -- true if abbr is a valid abbreviation of word.
 *
 *   This is LeetCode 408, "Valid Word Abbreviation".
 */

/*
 * =============================================================================
 * SECTION 2: CLARIFYING QUESTIONS
 * =============================================================================
 *
 * Q1. What are the size bounds on `word` and `abbr`?
 *     A: Assume 1 <= word.length() <= 20, 1 <= abbr.length() <= 10, matching
 *        LeetCode's constraints. I will still write an algorithm that is
 *        linear in the input size so it scales far beyond that.
 *
 * Q2. Can `word` or `abbr` be empty strings?
 *     A: Per LeetCode constraints both have length >= 1, but I will defend
 *        against empty strings anyway since it's a one-line guard and makes
 *        the solution robust to reuse elsewhere.
 *
 * Q3. Is `word` guaranteed to be lowercase letters only (no digits, spaces,
 *     punctuation)?
 *     A: Yes, assume `word` is purely lowercase English letters.
 *
 * Q4. Can `abbr` contain uppercase letters, symbols, or whitespace?
 *     A: No, assume `abbr` is lowercase letters and digits ('0'-'9') only.
 *
 * Q5. Can a digit-run represent a skip count larger than word.length()?
 *     A: Yes, syntactically it can appear; my solution must simply reject it
 *        (return false) rather than throw, since it can never be valid.
 *
 * Q6. Is "12" always the number twelve, or could it mean "1" then "2"?
 *     A: Per the non-adjacency rule, two replaced substrings can never sit
 *        next to each other in `word`, so their length-numbers can never be
 *        adjacent in `abbr` either. Therefore any consecutive digit run is
 *        unambiguously ONE number (greedy / maximal-munch parsing).
 *
 * Q7. Do we need to return which substrings were abbreviated, or just a
 *     boolean validity check?
 *     A: Just a boolean. No reconstruction of the substrings is required.
 *
 * Q8. Is this called once, or many times against the same `word` with many
 *     different `abbr` candidates (i.e., should I optimize for repeated
 *     queries / preprocessing)?
 *     A: Assume a single one-off query for this problem. I'll mention in the
 *        follow-ups how repeated queries would change my approach.
 */

/*
 * =============================================================================
 * SECTION 3: EXAMPLES & EDGE CASES
 * =============================================================================
 *
 * Example 1 (normal case):
 *   word = "internationalization", abbr = "i12iz4n"
 *   Interpretation: 'i' + (skip 12: "nternational") + "iz" + (skip 4: "atio")
 *                   + "n"
 *   Result: true
 *
 * Example 2 (edge case -- leading zero, must be rejected):
 *   word = "internationalization", abbr = "i5a11o1"
 *   Here "i" matches, skip 5 -> lands mid-word, but suppose a digit run were
 *   "05" instead of "5" -- that is a leading-zero violation and must return
 *   false immediately, regardless of whether the skip count would otherwise
 *   have worked out. A concrete minimal example:
 *   word = "a", abbr = "01"   -> false (leading zero, even though 1 would
 *                                        have been in range).
 *
 * Example 3 (boundary / tie-breaking case -- exact full-length replacement
 * and the "0 is invalid" rule):
 *   word = "word", abbr = "4"     -> true  (whole word replaced by its length)
 *   word = "word", abbr = "0"     -> false (0 means "replace an empty
 *                                            substring", which is illegal)
 *   word = "a",    abbr = "01"    -> false (leading zero)
 *   word = "hi",   abbr = "1i"    -> false (skip 1 -> pointer at 'i', but
 *                                            abbr's letter 'i' must match
 *                                            word.charAt(1) which is 'i' --
 *                                            wait, let's re-check: word="hi",
 *                                            skip 1 consumes 'h', leaving
 *                                            pointer at index 1 = 'i', and
 *                                            abbr's next literal char is
 *                                            'i' -> this actually MATCHES,
 *                                            so the correct false example is
 *                                            word = "hi", abbr = "1o" -> the
 *                                            literal 'o' does not match 'i'
 *                                            at index 1 -> false.
 *   word = "a",    abbr = "2"     -> false (skip count exceeds word length)
 *   word = "ab",   abbr = "a1b"   -> true  ('a' matches, skip 1 lands on
 *                                            'b', 'b' matches, both pointers
 *                                            finish together)
 */

/*
 * =============================================================================
 * SECTION 4: APPROACH 1 -- TOKENIZE-THEN-VALIDATE (NAIVE / BRUTE FORCE)
 * =============================================================================
 *
 * Core idea:
 *   First fully parse `abbr` into an explicit list of "tokens" -- either a
 *   Letter token or a SkipCount token -- performing all the digit/leading-
 *   zero validation up front. Then, in a second pass, walk `word` and
 *   consume it according to the token list.
 *
 * Paradigm / data structures:
 *   Simple string parsing + an auxiliary List used as an intermediate
 *   representation (a classic "two-pass compiler-style" decomposition:
 *   tokenize, then interpret). No sorting, hashing, DP, etc. is warranted.
 *
 * Why it's "naive" relative to Approach 2:
 *   It does the same amount of parsing work, but it materializes an entire
 *   token list in memory before doing anything with `word`, which is extra
 *   space and an extra pass that a single combined loop doesn't need. In an
 *   interview this is a perfectly reasonable "let me get something correct
 *   first" starting point.
 */
final class TokenizeThenValidate {

    // A token is either a run of skip-count digits or a single literal
    // letter. We model both with one class to keep the token list simple.
    private static final class Token {
        final boolean isSkip;   // true => numeric skip token, false => letter
        final int skipCount;    // valid only when isSkip == true
        final char letter;      // valid only when isSkip == false

        Token(int skipCount) {
            this.isSkip = true;
            this.skipCount = skipCount;
            this.letter = '\0';
        }

        Token(char letter) {
            this.isSkip = false;
            this.skipCount = -1;
            this.letter = letter;
        }
    }

    public boolean validWordAbbreviation(String word, String abbr) {
        if (word == null || abbr == null) {
            return false;
        }

        List<Token> tokens = tokenize(abbr);
        if (tokens == null) {
            // Malformed abbr (e.g., leading zero) detected during tokenizing.
            return false;
        }

        int wordIndex = 0;
        for (Token token : tokens) {
            if (token.isSkip) {
                wordIndex += token.skipCount;
                if (wordIndex > word.length()) {
                    return false; // skipped past the end of word
                }
            } else {
                if (wordIndex >= word.length()
                        || word.charAt(wordIndex) != token.letter) {
                    return false; // literal mismatch or ran out of word
                }
                wordIndex++;
            }
        }
        return wordIndex == word.length();
    }

    // Splits abbr into Token objects, validating the "no leading zero" rule
    // as it goes. Returns null if abbr is malformed.
    private List<Token> tokenize(String abbr) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        int length = abbr.length();

        while (index < length) {
            char currentChar = abbr.charAt(index);
            if (Character.isDigit(currentChar)) {
                if (currentChar == '0') {
                    return null; // "0" or any run starting with '0' -> invalid
                }
                int numberStart = index;
                while (index < length && Character.isDigit(abbr.charAt(index))) {
                    index++;
                }
                int skipCount = Integer.parseInt(abbr.substring(numberStart, index));
                tokens.add(new Token(skipCount));
            } else {
                tokens.add(new Token(currentChar));
                index++;
            }
        }
        return tokens;
    }

    /*
     * Time Complexity:  O(n + m) where n = word.length(), m = abbr.length().
     *   Tokenizing abbr is O(m); walking word against the token list is
     *   O(n + numberOfTokens) which is bounded by O(n + m).
     *
     * Space Complexity: O(m) for the intermediate token list -- this is the
     *   entire reason this approach is "naive": it's asymptotically the same
     *   time as Approach 2, but it uses extra auxiliary space and an extra
     *   pass instead of doing everything inline.
     *
     * Pros:
     *   - Very easy to reason about and to unit test each phase separately
     *     (tokenizer correctness vs. matching correctness).
     *   - Natural first design if you're thinking "parse, then interpret."
     *
     * Cons:
     *   - Extra O(m) space that isn't necessary.
     *   - Two logical passes instead of one tight loop.
     *
     * When to use in practice:
     *   Good as a warm-up / first-draft answer, or if the token list would
     *   be reused for multiple different `word`s (amortizes the tokenizing
     *   cost). Not what I'd leave as my final answer in an interview.
     */
}

/*
 * =============================================================================
 * SECTION 5: APPROACH 2 -- TWO-POINTER SINGLE PASS (OPTIMAL)
 * =============================================================================
 *
 * Core idea:
 *   Walk `word` and `abbr` simultaneously with two independent indices.
 *   At each step of `abbr`:
 *     - If the current character is a digit, it must not be '0' (leading
 *       zero / zero-value guard). Consume the full maximal run of digits to
 *       form the skip count, and advance the word pointer by that amount.
 *     - Otherwise it's a literal letter: it must equal word's character at
 *       the current word pointer; advance both pointers by one.
 *   After the loop, the abbreviation is valid only if BOTH pointers have
 *   exactly reached the end of their respective strings.
 *
 * Paradigm / data structures:
 *   Classic two-pointer linear scan. No auxiliary data structure needed --
 *   O(1) extra space. This is the standard, expected interview-optimal
 *   solution for this problem.
 */
final class TwoPointerOptimal {

    public boolean validWordAbbreviation(String word, String abbr) {
        if (word == null || abbr == null) {
            return false;
        }

        int wordIndex = 0;
        int abbrIndex = 0;
        int wordLength = word.length();
        int abbrLength = abbr.length();

        while (wordIndex < wordLength && abbrIndex < abbrLength) {
            char abbrChar = abbr.charAt(abbrIndex);

            if (Character.isDigit(abbrChar)) {
                if (abbrChar == '0') {
                    return false; // leading zero / literal "0" -> invalid
                }
                int skipCount = 0;
                while (abbrIndex < abbrLength && Character.isDigit(abbr.charAt(abbrIndex))) {
                    skipCount = skipCount * 10 + (abbr.charAt(abbrIndex) - '0');
                    abbrIndex++;
                }
                wordIndex += skipCount;
            } else {
                if (word.charAt(wordIndex) != abbrChar) {
                    return false; // literal mismatch
                }
                wordIndex++;
                abbrIndex++;
            }
        }

        // Both must have been fully consumed for abbr to account for
        // every character of word, left to right, with none left over.
        return wordIndex == wordLength && abbrIndex == abbrLength;
    }

    /*
     * Time Complexity:  O(n + m). Each character of `word` is visited at
     *   most once (advanced past via literal match or via a skip), and each
     *   character of `abbr` is visited exactly once.
     *
     * Space Complexity: O(1) extra space -- only a handful of int indices,
     *   no auxiliary lists, no string copies, no recursion stack.
     *
     * Pros:
     *   - Optimal time and space.
     *   - Single pass, easy to explain out loud line by line.
     *   - No intermediate allocations -> good for very large inputs or
     *     hot-path / repeated-call scenarios.
     *
     * Cons:
     *   - Slightly more bookkeeping in one loop body than the split
     *     tokenize/validate design (two responsibilities interleaved), so
     *     it demands careful, deliberate coding to avoid off-by-one bugs.
     *
     * When to use in practice:
     *   Always -- this is the production-grade choice and what I'd write in
     *   an interview once I've talked through the design.
     */
}

/*
 * =============================================================================
 * SECTION 6: APPROACH 3 -- REGEX-BASED PATTERN MATCH (ALTERNATIVE)
 * =============================================================================
 *
 * Core idea:
 *   Translate `abbr` into an equivalent regular expression: every literal
 *   letter is appended as-is, and every digit-run "k" becomes the regex
 *   fragment ".{k}" (meaning "exactly k of any character"). Leading-zero
 *   validation is still done manually while building the pattern, since
 *   regex quantifiers don't have a notion of "invalid numeral format."
 *   Finally, check word.matches(pattern), which implicitly anchors to the
 *   whole string (equivalent to ^...$), giving us the "must account for
 *   every character" requirement for free.
 *
 * Paradigm / data structures:
 *   String-to-pattern translation + the regex engine (an NFA/DFA-based
 *   matcher under the hood). This is really the same linear-scan logic as
 *   Approach 2, just delegated to java.util.regex instead of hand-rolled.
 */
final class RegexBased {

    public boolean validWordAbbreviation(String word, String abbr) {
        if (word == null || abbr == null) {
            return false;
        }

        StringBuilder patternBuilder = new StringBuilder();
        int index = 0;
        int length = abbr.length();

        while (index < length) {
            char currentChar = abbr.charAt(index);
            if (Character.isDigit(currentChar)) {
                if (currentChar == '0') {
                    return false; // leading zero / literal zero -> invalid
                }
                int numberStart = index;
                while (index < length && Character.isDigit(abbr.charAt(index))) {
                    index++;
                }
                int skipCount = Integer.parseInt(abbr.substring(numberStart, index));
                // ".{k}" means "exactly k of any character" in regex.
                patternBuilder.append(".{").append(skipCount).append('}');
            } else {
                // Lowercase letters are not regex metacharacters, so no
                // escaping is required here. In a more general-purpose
                // version I would run currentChar through
                // Pattern.quote(String.valueOf(currentChar)) defensively.
                patternBuilder.append(currentChar);
                index++;
            }
        }

        try {
            // String.matches anchors the pattern to the entire input,
            // which naturally enforces "accounts for every character."
            return word.matches(patternBuilder.toString());
        } catch (PatternSyntaxException malformedPattern) {
            return false;
        }
    }

    /*
     * Time Complexity:  O(n + m) to build the pattern string, plus whatever
     *   the underlying regex engine costs to match it against `word`. For
     *   this restricted pattern shape (only literals and ".{k}" bounded
     *   quantifiers, no branching or backtracking constructs), the engine
     *   effectively behaves like a single linear scan, so it's O(n + m) in
     *   practice, though Java's regex engine does not formally guarantee
     *   linear-time matching for arbitrary patterns the way Approach 2
     *   does by construction.
     *
     * Space Complexity: O(m) for the constructed pattern string, plus
     *   whatever internal automaton state java.util.regex builds (bounded
     *   by pattern size).
     *
     * Pros:
     *   - Concise, and demonstrates comfort with regex.
     *   - The "must consume the whole string" requirement is free via
     *     String.matches semantics.
     *
     * Cons:
     *   - Pulls in a comparatively heavyweight engine (Pattern compilation
     *     overhead) for what is fundamentally a simple linear scan.
     *   - Harder to unit test edge cases like "trailing garbage" because
     *     failures surface as regex non-matches rather than explicit,
     *     debuggable state.
     *   - Riskier in an interview: any subtlety in escaping or in how
     *     Pattern compiles quantifiers can silently produce wrong results,
     *     and it's harder to reason about correctness on a whiteboard.
     *
     * When to use in practice:
     *   Nice to mention as "I could also express this as a regex" to show
     *   range, but I would not lead with it, and I would not want to debug
     *   regex edge cases live in front of an interviewer.
     */
}

/*
 * =============================================================================
 * SECTION 7: PARADIGMS CONSIDERED AND RULED OUT
 * =============================================================================
 *
 * Going through the requested checklist explicitly:
 *
 *  - Sorting-based:        Not applicable -- order of characters is
 *                           semantically meaningful ("left to right"); we
 *                           can never reorder word or abbr.
 *  - Hashing-based:         Not applicable -- there's no "have we seen this
 *                           before" membership question; matching is purely
 *                           positional.
 *  - Divide and conquer:   Not applicable -- there's no natural way to
 *                           split the problem into independent subproblems
 *                           that recombine; the matching is inherently
 *                           sequential and stateful (word pointer position
 *                           depends on everything consumed so far).
 *  - Greedy:                Arguably what Approach 2 already is (greedily
 *                           consume the maximal digit run, greedily advance
 *                           the pointer) -- but there's no "choice" being
 *                           made since parsing is deterministic, so it
 *                           doesn't really rise to a distinct "greedy
 *                           algorithm" design pattern; it's simulation.
 *  - Dynamic programming:  Not applicable -- there is no branching /
 *                           overlapping-subproblem structure. Contrast this
 *                           with LeetCode 320 ("Generalize Abbreviation") or
 *                           regex/wildcard matching with '*' and '?', where
 *                           multiple interpretations exist and DP or
 *                           backtracking is genuinely needed. Here, the
 *                           non-adjacency rule makes every abbr's meaning
 *                           unique, so there's nothing to memoize.
 *  - Tree / graph traversal: Not applicable -- no hierarchical or
 *                           relational structure in the input.
 *  - Heap / priority queue: Not applicable -- no ordering-by-priority or
 *                           "k-th smallest/largest" requirement.
 *  - Binary search:         Not applicable -- there is no monotonic search
 *                           space to binary search over; validity isn't a
 *                           function of a single sortable parameter.
 *  - Monotonic stack/deque: Not applicable -- no "next greater element" /
 *                           nested-interval structure to maintain.
 *  - Trie / segment tree:   Not applicable -- we are not doing repeated
 *                           prefix lookups across many words, nor range
 *                           queries; a single linear pass already suffices.
 *                           (A trie would become relevant only in the
 *                           "many words, one abbr, or vice versa" follow-up
 *                           variant -- see Section 13.)
 *
 * That leaves the genuinely applicable design space as: naive multi-pass
 * simulation (Approach 1), optimal single-pass two-pointer simulation
 * (Approach 2), and simulation-via-regex-engine (Approach 3).
 */

/*
 * =============================================================================
 * SECTION 8: APPROACHES COMPARISON TABLE
 * =============================================================================
 *
 * | Approach                        | Time      | Space | Best For             | Limitations                        |
 * |----------------------------------|-----------|-------|-----------------------|-------------------------------------|
 * | 1. Tokenize-then-Validate        | O(n + m)  | O(m)  | Clear separation of  | Extra pass + extra memory for the  |
 * |    (naive, two-pass)             |           |       | parsing vs. matching;| token list; not needed here.       |
 * |                                   |           |       | reusable token list  |                                     |
 * | 2. Two-Pointer Single Pass       | O(n + m)  | O(1)  | Production code and  | Slightly denser loop body (two     |
 * |    (OPTIMAL)                     |           |       | interview whiteboard | responsibilities interleaved) --   |
 * |                                   |           |       | answer               | needs careful, deliberate coding.  |
 * | 3. Regex-Based Pattern Match      | O(n + m)* | O(m)  | Showing regex range; | Engine overhead; harder to debug   |
 * |                                   | (*engine- |       | quick one-off script | edge cases live; no formal linear  |
 * |                                   | dependent)|       |                       | time guarantee from the JDK.       |
 *
 *   n = word.length(), m = abbr.length()
 */

/*
 * =============================================================================
 * SECTION 9: RECOMMENDED APPROACH FOR INTERVIEW
 * =============================================================================
 *
 * I would present Approach 2 (Two-Pointer Single Pass) as my final answer.
 *
 * Why:
 *   - It is asymptotically optimal in both time (O(n+m)) and space (O(1)),
 *     so there is no "better" answer to reach for -- I can confidently say
 *     this is the ceiling.
 *   - It is fast to code correctly on a whiteboard/IDE under interview time
 *     pressure: one loop, one branch on "is this a digit," clear invariant
 *     ("wordIndex tracks how much of word we've accounted for").
 *   - It avoids pulling in extra machinery (regex engine, intermediate
 *     lists) that adds cognitive overhead without payoff for a problem this
 *     size -- which signals good engineering judgment, not just "knowing an
 *     algorithm."
 *   - It's easy to narrate out loud step by step, which interviewers value
 *     as much as the code itself.
 *
 * I'd start by describing Approach 1 verbally in ~15 seconds as my initial
 * mental model ("parse abbr into tokens, then walk word"), then immediately
 * note that I can merge both passes into one loop with O(1) space, and
 * write Approach 2 directly -- demonstrating the naive-to-optimal thought
 * process without actually spending time coding the naive version.
 */

/*
 * =============================================================================
 * SECTION 10: DEEP DIVE -- OPTIMAL SOLUTION (PRODUCTION-QUALITY)
 * =============================================================================
 *
 * This is a polished, defensively-written version of Approach 2, with full
 * inline commentary on every decision, suitable as a final interview
 * answer or as production code.
 */
public final class ValidWordAbbreviation {

    /**
     * Determines whether {@code abbr} is a valid abbreviation of
     * {@code word}, per the replacement rules described in the problem
     * statement (non-empty, non-adjacent substrings replaced by their
     * lengths, no leading zeros, no bare "0").
     *
     * @param word the original, unabbreviated string (lowercase letters)
     * @param abbr the candidate abbreviation (lowercase letters + digits)
     * @return true if abbr is a valid abbreviation of word; false otherwise
     */
    public static boolean validWordAbbreviation(String word, String abbr) {
        // Defensive null handling: treat null inputs as "not a valid
        // abbreviation" rather than throwing, so this method is safe to
        // call directly from, e.g., a request handler without a separate
        // null check at every call site.
        if (word == null || abbr == null) {
            return false;
        }

        final int wordLength = word.length();
        final int abbrLength = abbr.length();

        // wordPointer: how much of `word` we've accounted for so far.
        // abbrPointer: how much of `abbr` we've consumed so far.
        // Invariant maintained at the top of every loop iteration:
        //   word[0 .. wordPointer) has been fully explained by
        //   abbr[0 .. abbrPointer) (via literal matches and/or skips).
        int wordPointer = 0;
        int abbrPointer = 0;

        while (wordPointer < wordLength && abbrPointer < abbrLength) {
            char abbrChar = abbr.charAt(abbrPointer);

            if (Character.isDigit(abbrChar)) {
                // A digit run must never start with '0':
                //   - "0" alone means "replace an empty substring" -> illegal.
                //   - "0" followed by more digits ("012") is a leading zero
                //     -> also illegal, regardless of what number it would
                //     otherwise represent.
                // Checking this once, at the START of every digit run, is
                // sufficient to catch both cases.
                if (abbrChar == '0') {
                    return false;
                }

                // Greedily consume the full maximal run of digits. Because
                // replaced substrings can never be adjacent in `word`, any
                // consecutive digit run in `abbr` is unambiguously a single
                // number -- there is no alternate parsing to consider.
                int skipCount = 0;
                while (abbrPointer < abbrLength
                        && Character.isDigit(abbr.charAt(abbrPointer))) {
                    skipCount = skipCount * 10 + (abbr.charAt(abbrPointer) - '0');
                    abbrPointer++;
                }

                // Advance past `skipCount` characters of word. We defer the
                // "did we overshoot the end of word" check to the loop
                // condition / final check below rather than special-casing
                // it here, which keeps this branch simple; wordPointer is
                // allowed to end up > wordLength transiently and the final
                // equality check will correctly reject that.
                wordPointer += skipCount;
            } else {
                // Literal letter: it must match word's character at the
                // current position exactly (case-sensitive, since both
                // strings are specified as lowercase-only).
                if (wordPointer >= wordLength || word.charAt(wordPointer) != abbrChar) {
                    return false;
                }
                wordPointer++;
                abbrPointer++;
            }
        }

        // Valid only if BOTH strings were fully consumed. If wordPointer
        // finished early, abbr didn't account for the remaining tail of
        // word. If abbrPointer finished early, word ran out before abbr
        // did (e.g., a skip count overshot, or trailing letters/digits in
        // abbr had nothing left to match against).
        return wordPointer == wordLength && abbrPointer == abbrLength;
    }
}

/*
 * =============================================================================
 * SECTION 11: DRY RUN / TRACE
 * =============================================================================
 *
 * Tracing validWordAbbreviation("internationalization", "i12iz4n"):
 *
 *   word  = "internationalization"   (length 20, indices 0-19)
 *           i  n  t  e  r  n  a  t  i  o  n  a  l  i  z  a  t  i  o  n
 *           0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19
 *   abbr  = "i12iz4n"                (length 7, indices 0-6)
 *
 * Initial state: wordPointer = 0, abbrPointer = 0
 *
 * Step 1: abbrChar = abbr[0] = 'i' (letter)
 *         word[0] = 'i' -> match.
 *         State: wordPointer = 1, abbrPointer = 1
 *
 * Step 2: abbrChar = abbr[1] = '1' (digit, not '0' -> OK to proceed)
 *         Consume the full digit run "12" (abbr[1..2]) -> skipCount = 12
 *         State: wordPointer = 1 + 12 = 13, abbrPointer = 3
 *         (word[1..12] = "nternational", 12 characters, has been skipped)
 *
 * Step 3: abbrChar = abbr[3] = 'i' (letter)
 *         word[13] = 'i' -> match.
 *         State: wordPointer = 14, abbrPointer = 4
 *
 * Step 4: abbrChar = abbr[4] = 'z' (letter)
 *         word[14] = 'z' -> match.
 *         State: wordPointer = 15, abbrPointer = 5
 *
 * Step 5: abbrChar = abbr[5] = '4' (digit, not '0' -> OK)
 *         Consume digit run "4" (abbr[5]) -> skipCount = 4
 *         State: wordPointer = 15 + 4 = 19, abbrPointer = 6
 *         (word[15..18] = "atio", 4 characters, has been skipped)
 *
 * Step 6: abbrChar = abbr[6] = 'n' (letter)
 *         word[19] = 'n' -> match.
 *         State: wordPointer = 20, abbrPointer = 7
 *
 * Loop condition: abbrPointer (7) < abbrLength (7) is false -> loop exits.
 *
 * Final check: wordPointer == wordLength  ->  20 == 20  -> true
 *              abbrPointer == abbrLength  ->  7 == 7    -> true
 *
 * Result: TRUE. Every character of word was accounted for -- either
 * matched literally ('i', 'i', 'z', 'n') or skipped over by a numeric
 * replacement (12, then 4) -- and abbr was fully consumed at the same
 * moment word was, satisfying both halves of the final check.
 *
 * (The demo harness in Section 15 executes this exact case, plus the
 * false/edge/boundary cases from Section 3, and prints the real, verified
 * boolean result for each -- useful to run rather than trust hand
 * arithmetic alone, especially on longer strings.)
 */

/*
 * =============================================================================
 * SECTION 12: CLOSING SUMMARY
 * =============================================================================
 *
 * All three approaches agree on correctness and share the same asymptotic
 * time complexity, O(n + m). They differ only in space and in "where the
 * work happens":
 *
 *   - Approach 1 (Tokenize-then-Validate) trades O(m) extra space for a
 *     clean separation between parsing and matching. Good for clarity /
 *     reuse, not for a final optimal answer.
 *   - Approach 2 (Two-Pointer Single Pass) is the O(1)-space, single-loop,
 *     production-grade answer, and the one I'd commit to on a whiteboard.
 *   - Approach 3 (Regex-Based) is a fun alternative that leans on the JDK's
 *     regex engine to get the same result more concisely, at the cost of
 *     engine overhead and reduced debuggability.
 *
 * Known assumptions / limitations of the final (Approach 2) solution:
 *   - Assumes `word` contains only lowercase letters and `abbr` contains
 *     only lowercase letters and ASCII digits, per the problem statement;
 *     no Unicode / surrogate-pair handling is attempted.
 *   - Uses a plain `int` for skipCount accumulation; an adversarially long
 *     digit run (many digits) could overflow `int`. Given the stated
 *     constraints (abbr.length() <= 10) this is a non-issue, but it's
 *     called out explicitly in Section 13 as something worth mentioning
 *     proactively to an interviewer.
 *   - Treats null `word`/`abbr` as "invalid" rather than throwing.
 */

/*
 * =============================================================================
 * SECTION 13: FOLLOW-UP QUESTIONS
 * =============================================================================
 *
 * 1. "What if we need to check ONE word against a whole DICTIONARY of
 *    abbreviations, or one abbreviation against a whole dictionary of
 *    words, many times?" -> This motivates preprocessing structures (e.g.,
 *    a trie of words, or grouping abbreviations by their expected total
 *    skip+literal length) to avoid repeating O(n+m) work per query.
 *    (This is essentially LeetCode 411/288-style "Unique Word Abbreviation"
 *    territory.)
 *
 * 2. "What if `abbr` could legally contain adjacent numeric replacements
 *    (i.e., the non-adjacency rule is relaxed)?" -> Then consecutive digit
 *    runs become ambiguous ("12" could be 1+2 or 12), and you would need
 *    backtracking/DP over all ways to split the run -- a fundamentally
 *    different, harder problem.
 *
 * 3. "How would you support Unicode / multi-byte characters in `word`?"
 *    -> Switch from `charAt`/`length()` (UTF-16 code unit based) to
 *    iterating by code point (`codePointAt` / `String.codePoints()`) so
 *    that supplementary-plane characters aren't split incorrectly.
 *
 * 4. "What if skip counts can be astronomically large (e.g., abbr can be
 *    megabytes of digits)?" -> Guard against int overflow while
 *    accumulating skipCount (e.g., short-circuit / clamp once the running
 *    value exceeds word.length(), or parse into a long).
 *
 * 5. "Can this run correctly and safely under concurrent/multi-threaded
 *    access?" -> Yes as written: the method is stateless (no shared mutable
 *    fields), operates only on local variables and immutable String
 *    inputs, so it is inherently thread-safe with no synchronization
 *    needed.
 *
 * 6. "Could you generalize this to LeetCode 320, 'Generalize Abbreviation'
 *    (generate ALL valid abbreviations of a word)?" -> That's the inverse,
 *    generative problem, and it genuinely does need backtracking/DFS over
 *    subsets of positions to replace, unlike this validation problem which
 *    has a unique deterministic parse.
 */

/*
 * =============================================================================
 * SECTION 14: WHAT CANDIDATES TYPICALLY MISS
 * =============================================================================
 *
 * 1. Forgetting the leading-zero / bare-zero rule entirely, or checking it
 *    only for multi-digit runs while forgetting that a single "0" is
 *    ALSO invalid (it's not just about leading zeros, "0" itself has no
 *    valid meaning since it would replace an empty substring).
 *
 * 2. Treating consecutive digits in `abbr` as separate single-digit
 *    numbers instead of parsing the FULL maximal digit run as one number
 *    (e.g., mishandling "12" as skip-1-then-skip-2 instead of skip-12).
 *
 * 3. Off-by-one / early-exit bugs: returning true as soon as the loop
 *    naturally ends (one pointer hits its length) without checking that
 *    BOTH pointers reached the end. This silently accepts abbreviations
 *    that either leave a suffix of `word` unaccounted for, or that have
 *    leftover, unconsumed characters in `abbr`.
 *
 * 4. Allowing a skip count to walk `wordPointer` past `word.length()`
 *    without it being caught -- if you don't compare against the actual
 *    length at the end (or clamp/guard during the skip), an oversized skip
 *    can silently make the method return an incorrect result instead of
 *    cleanly failing.
 */

/*
 * =============================================================================
 * SECTION 15: MAIN / DEMO HARNESS
 * =============================================================================
 * A small runnable harness exercising the optimal solution against the
 * examples discussed above, so correctness claims in Section 11 are backed
 * by an actual, verifiable run rather than hand arithmetic alone.
 */
final class ValidWordAbbreviationDemo {
    public static void main(String[] args) {
        Object[][] cases = {
            {"internationalization", "i12iz4n", null},
            {"internationalization", "i5a11o1", null},
            {"word", "4", true},
            {"word", "0", false},
            {"a", "01", false},
            {"hi", "1o", false},
            {"hi", "1i", true},
            {"a", "2", false},
            {"ab", "a1b", true},
        };

        for (Object[] testCase : cases) {
            String word = (String) testCase[0];
            String abbr = (String) testCase[1];
            boolean actual = ValidWordAbbreviation.validWordAbbreviation(word, abbr);
            System.out.printf("word=%-24s abbr=%-10s -> %s%n", word, abbr, actual);
        }

        // Cross-check all three approaches agree with each other on every
        // case, which is a useful sanity habit before declaring "done."
        TokenizeThenValidate naive = new TokenizeThenValidate();
        TwoPointerOptimal optimal = new TwoPointerOptimal();
        RegexBased regex = new RegexBased();

        for (Object[] testCase : cases) {
            String word = (String) testCase[0];
            String abbr = (String) testCase[1];
            boolean resultNaive = naive.validWordAbbreviation(word, abbr);
            boolean resultOptimal = optimal.validWordAbbreviation(word, abbr);
            boolean resultRegex = regex.validWordAbbreviation(word, abbr);
            boolean allAgree = (resultNaive == resultOptimal) && (resultOptimal == resultRegex);
            System.out.printf("agreement check word=%-24s abbr=%-10s allAgree=%s%n",
                    word, abbr, allAgree);
        }
    }
}
