/**
 * 0. ## Problem Statement
 * Design an algorithm that accepts a stream of characters one at a time and checks whether
 * any non-empty suffix of the characters received so far matches a word in a given array `words`.
 *
 * Example:
 *   words = ["cd", "f", "kl"]
 *   query('a') -> false  ("a")
 *   query('b') -> false  ("ab")
 *   query('c') -> false  ("abc")
 *   query('d') -> true   ("abcd" ends with "cd")
 *   query('e') -> false  ("abcde")
 *   query('f') -> true   ("abcdef" ends with "f")
 *
 * Implement the `StreamChecker` class:
 * - `StreamChecker(String[] words)`: Initializes the object with the dictionary `words`.
 * - `boolean query(char letter)`: Appends `letter` to the stream and returns `true` if ANY
 *   non-empty suffix of the stream so far is in `words`, and `false` otherwise.
 *
 * Constraints:
 * - 1 <= words.length <= 2000
 * - 1 <= words[i].length <= 200
 * - `words[i]` and `letter` consist strictly of lowercase English letters (`'a'`-`'z'`).
 * - At most 4 * 10^4 calls will be made to `query`.
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "What is the character set — strictly lowercase English `'a'-'z'`, or full ASCII/Unicode?"
 *    -> Determines whether Trie nodes can use a compact `Node[26]` array or must use a `HashMap`.
 * 2. "Can one word in `words` be a suffix or substring of another (e.g., `["b", "ab", "abcd"]`)?"
 *    -> Critical! In a Reversed Trie we must check `node.isEnd` at EVERY step (not only at leaves);
 *       in Aho-Corasick we must propagate `isMatch` along failure links so `"bc"` fires inside `"abc"`.
 * 3. "Can `words` contain duplicate strings?"
 *    -> Ensures our Trie/automaton construction is idempotent on duplicate inserts.
 * 4. "Can the stream grow indefinitely (e.g., millions of `query()` calls in production)?"
 *    -> Prevents storing the entire stream in an unbounded `StringBuilder` (O(Q) memory leak) when
 *       we only ever need the last `maxWordLen` characters!
 * 5. "Is `words` static after construction, or can new words be added dynamically later?"
 *    -> Dynamic additions work naturally with a Reversed Trie, whereas Aho-Corasick would need
 *       to rebuild its failure links.
 * 6. "Do we only return a boolean (`true`/`false`), or do we need to return ALL matching words?"
 *    -> Boolean allows early-exit on the first match in Reversed Trie and a single `boolean isMatch`
 *       flag in Aho-Corasick (instead of storing lists of matched words at each state).
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Up to `Q = 4 * 10^4` calls to `query()`, `W = 2000` words, and max word
 * length `L = 200`. Scanning all `W` words on every query takes `Q * W * L ≈ 1.6 * 10^10`
 * character comparisons (TLE). We need at most O(L) per query — or even O(1)!
 *
 * Let:
 *   W = number of words in `words` (<= 2000)
 *   L = maximum length of a word in `words` (<= 200)
 *   S = total characters across all words (`sum(words[i].length()) <= 4 * 10^5`)
 *   Q = number of `query()` calls (<= 4 * 10^4)
 *
 * --- Approach 1: Brute Force (Stream Buffer + Check Every Word via Suffix Match) ---
 * 0. Idea/Intuition: Append each incoming `letter` to a `StringBuilder`. On every `query()`,
 *    loop through all `W` words and check whether the stream's tail matches `word` character-by-character.
 * 1. What I'd naturally try: Literally translate "does the stream end with any word in `words`?"
 * 2. Why it works: Explicitly checks every candidate word against the current stream tail.
 * 3. Why it's too slow/costly: On every single character, we test up to 2,000 words (each up
 *    to 200 chars), even if 1,999 of those words don't even end with `letter`!
 * 4. What work is being repeated:
 *    (a) Re-checking words that don't share the newest character (or recent suffix).
 *    (b) Re-comparing shared suffixes across multiple words (e.g., `"bcd"`, `"cd"`, `"acd"` all
 *        re-verify `'d'` and `'c'` separately).
 * 5. Observation that removes the bottleneck: Notice that we are matching **suffixes** of the
 *    stream — meaning every match is anchored at the **newest character** (`stream[end]`) and
 *    extends **backward**!
 *    If we reverse every word (`"cd" -> "dc"`) and insert them into a **Trie**, then checking
 *    ALL suffixes of the stream simultaneously is just ONE single walk down the Reversed Trie
 *    starting from `stream[end]`, going backward `stream[end - 1], stream[end - 2], ...`!
 *    (Why not a *Forward* Trie? In a forward Trie, a new character could extend ANY of up to `L`
 *    different in-progress prefixes simultaneously, forcing you to maintain a list of active
 *    pointers per query. Reversing the words collapses `L` parallel walks into ONE deterministic walk!)
 * 6. Leads to Approach 2: Reversed Trie + Bounded Ring Buffer.
 *    Time Complexity:
 *    - Constructor: O(1) — stores reference to `words` (or O(W) to compute `maxLen`).
 *    - `query`: O(W * L) worst-case per call — checks up to W words, comparing up to L chars each.
 *    Space Complexity: O(Q) if using an unbounded `StringBuilder` (or O(L) if bounded to `maxLen`)
 *    auxiliary space, excluding the input `words` array.
 *
 * --- Approach 2: Reversed Trie + Bounded Ring Buffer (Interview Optimal) ---
 * 0. Idea/Intuition:
 *    - Constructor: For each `word` in `words`, insert its characters in REVERSE order
 *      (`word.length() - 1` down to `0`) into a Trie, marking `node.isEnd = true` at the end.
 *      Track `maxLen = max(word.length())`.
 *    - Stream storage: Maintain a fixed-size circular array `char[maxLen]` holding ONLY the
 *      most recent `maxLen` characters (never grows to O(Q)!).
 *    - `query(letter)`: Write `letter` into the ring buffer, then walk from `root` down the
 *      Reversed Trie reading characters backward from the newest position (`0, -1, -2, ...`):
 *        - If `node.children[c - 'a'] == null`, no word matches any longer suffix -> return `false`!
 *        - If `node.isEnd`, we found a matching suffix -> return `true` immediately!
 * 1. What I'd naturally try: "A forward Trie has to track multiple active start positions;
 *    what if I flip the problem backward so every query starts from a single known anchor — `letter`?"
 * 2. Why it works: A string `w` is a suffix of `stream` if and only if `reverse(w)` is a
 *    **prefix** of `reverse(stream)`. Walking backward from the newest character through the
 *    Reversed Trie checks every prefix of `reverse(stream)` (i.e., every suffix of `stream`)
 *    in a single linear path of length at most `maxLen`.
 * 3. Why it's still theoretically suboptimal (when `L` is huge): In the worst case (e.g.,
 *    `words = ["aaa...a"]` of length `L = 10^5` and stream `"aaa...a"`), every single `query('a')`
 *    walks all `L` steps backward! That's O(L) per query (O(Q * L) total).
 *    For this problem's `L <= 200`, `4 * 10^4 * 200 = 8 * 10^6` ops is instant.
 *    What if a follow-up raises `L` to `10^5`?
 * 4. What work is being repeated when `L` is huge: When we walked back `L - 1` `'a'`s on the
 *    previous query and a new `'a'` arrives, we re-walk those exact same `L - 1` `'a'`s again!
 * 5. Observation that removes the bottleneck: We want a **Forward** Trie that tracks ONLY ONE
 *    state pointer (`currState`), yet magically knows what the longest matching suffix is whenever
 *    an edge is missing — just like KMP's failure function, generalized to a Trie!
 *    That exact data structure is the **Aho-Corasick Automaton** (Forward Trie + Failure Links
 *    + precompiled 26-letter DFA transition table).
 * 6. Leads to Approach 3: Aho-Corasick Automaton (DFA).
 *    Time Complexity:
 *    - Constructor: O(S) — inserts all S total characters of `words` into the Reversed Trie.
 *    - `query`: O(L) worst-case per call — walks at most `maxLen` steps backward (and often O(1)
 *      in practice when `letter` has no outgoing edge from `root`).
 *    Space Complexity: O(S * 26 + L) = O(S) — at most S Trie nodes (each with a 26-slot array)
 *    plus the fixed `char[maxLen]` ring buffer (completely independent of stream length Q!).
 *
 * --- Approach 3: Aho-Corasick Automaton / DFA (Strict O(1) Query Optimal) ---
 * 0. Idea/Intuition:
 *    - Build a normal **Forward** Trie of `words`, marking `node.isMatch = true` at word ends.
 *    - Run a single BFS from `root` to compute two things for every node:
 *      (a) `fail` pointer: the longest proper suffix of this node's string that also exists in
 *          the Trie (for depth-1 nodes, `fail = root`).
 *      (b) Complete 26-letter DFA transitions:
 *          - If `curr.next[c]` exists: `child.fail = curr.fail.next[c]`, and
 *            `child.isMatch |= child.fail.isMatch` (propagates matches from proper suffixes!).
 *          - If `curr.next[c]` is null: `curr.next[c] = curr.fail.next[c]` (precomputes the
 *            exact fallback state so `query()` NEVER loops!).
 *    - Keep a single pointer `currState = root`.
 *    - `query(letter)` is literally TWO lines:
 *        `currState = currState.next[letter - 'a'];`
 *        `return currState.isMatch;`
 * 1. Why someone thinks of it: Whenever you need multi-pattern string matching over a stream
 *    in strict O(1) per character regardless of pattern length `L`, Aho-Corasick compiles the
 *    entire dictionary into a single finite-state machine.
 * 2. Why it works: By induction on BFS depth, after reading any stream `T`, `currState` always
 *    equals the Trie node representing the **longest suffix** of `T` that is a prefix of some
 *    word in `words`. Because `isMatch` was OR-propagated down every `fail` chain during BFS,
 *    `currState.isMatch` is `true` iff `currState` OR ANY proper suffix of `currState` is in `words`.
 *    Time Complexity:
 *    - Constructor: O(S * 26) = O(S) — builds Forward Trie in O(S), then one BFS visits each
 *      of the <= S nodes once and fills 26 transitions in O(1) each.
 *    - `query`: **Strict O(1)** worst-case per call — ONE array lookup + ONE boolean read!
 *      Completely independent of `W`, `L`, and `Q`!
 *    Space Complexity: O(S * 26) = O(S) — at most S automaton nodes, and **O(1)** stream buffer
 *    space (we don't store ANY stream characters at all!).
 *
 * Interview Verdict:
 * Write **Approach 2 (Reversed Trie + Bounded Ring Buffer)** in a 45-minute interview!
 * Why: With `L <= 200`, O(L) per query is blazingly fast, the code is ~25 lines, impossible to
 * mess up under pressure, and supports dynamic word additions if asked. Then **verbally mention
 * Approach 3 (Aho-Corasick)** as the strict O(1)-per-query optimization if `L` were 10^5 —
 * interviewers love seeing that you know BOTH the clean practical solution and the theoretical
 * optimum.
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Nested / overlapping suffix words (`words = ["abcd", "bc"]`, stream `"abc"`):
 *   On `'c'`, `"bc"` matches even though `"abc"` is mid-path to `"abcd"`.
 *   Tests checking `isEnd` at EVERY step (Reversed Trie) AND `child.isMatch |= child.fail.isMatch`
 *   (Aho-Corasick — forgetting this line is the #1 Aho-Corasick bug!).
 * - Single-character words (`words = ["f"]`, `maxLen = 1`): Ring buffer has size 1; must wrap
 *   and match immediately on the first `'f'`.
 * - Duplicate words in input (`words = ["ab", "ab"]`): Must not corrupt Trie or automaton.
 * - Repeated identical characters (`words = ["aaa"]`, stream `"aaaaa"`): Must return
 *   `[false, false, true, true, true]` (continuous overlapping matches).
 * - Stream longer than `maxLen`: Ring buffer overwrites old characters; verifies modular index
 *   subtraction `(idx - 1 + maxLen) % maxLen` never goes negative in Java!
 * - First character before any word can finish: `count < maxLen` must bound the backward walk
 *   so we don't read uninitialized `'\0'` slots from the ring buffer!
 * - No matches at all: Every `query()` returns `false`.
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "Why is matching a suffix of a growing stream hard going forward,
 * and what happens if I flip the direction?"
 * Correctness intuition: Going forward, you don't know *where* a matching suffix started — so
 * up to `L` different starting positions could be live at once. Going **backward** from the
 * newest character, every candidate suffix starts at the EXACT same anchor (`stream[end]`).
 * Since `w` is a suffix of `stream` iff `reverse(w)` is a prefix of `reverse(stream)`, a single
 * deterministic walk down a Reversed Trie checks all `W` words simultaneously.
 *
 * -- Examples & ASCII Diagram --
 * words = ["cd", "f", "kl", "bcd"]
 *
 * Reversed Trie (words inserted right-to-left: "dc", "f", "lk", "dcb"):
 *
 *            (root)
 *          /   |   \
 *        'd'  'f'*  'l'       (* means isEnd = true)
 *        /           \
 *      'c'*          'k'*
 *      /
 *    'b'*
 *
 * Stream so far: "a b c d"  (walk backward: 'd' -> 'c' -> 'b' -> 'a')
 *   Step 1: 'd' -> root.children['d'] exists (isEnd=false)
 *   Step 2: 'c' -> node('d').children['c'] exists (isEnd=TRUE -> "cd" matched! Return true!)
 *
 * -- Dry run 1: Reversed Trie + Ring Buffer on words = ["cd", "f", "kl"] (maxLen = 2) --
 *   Call       | Ring After [0,1] | count | Walk Backward (from newest)     | Return
 *   -----------+------------------+-------+---------------------------------+-------
 *   query('a') | ['a',  - ]       | 1     | 'a' -> null                     | false
 *   query('b') | ['a', 'b']       | 2     | 'b' -> null                     | false
 *   query('c') | ['c', 'b'] (w=1) | 2     | 'c' -> null                     | false
 *   query('d') | ['c', 'd'] (w=0) | 2     | 'd' -> node('d'); 'c' -> isEnd! | true
 *   query('e') | ['e', 'd'] (w=1) | 2     | 'e' -> null                     | false
 *   query('f') | ['e', 'f'] (w=0) | 2     | 'f' -> node('f') (isEnd!)       | true
 *
 * -- Dry run 2: Edge Case — Nested Suffix `words = ["abcd", "bc"]` on Stream `"abcd"` --
 * (Shows WHY Aho-Corasick's `child.isMatch |= child.fail.isMatch` is essential!)
 *   Forward Trie paths: `a -> ab -> abc -> abcd*` and `b -> bc*`.
 *   Failure links: `fail(ab) = b`, `fail(abc) = bc*` -> `abc.isMatch` inherits `TRUE` from `bc*`!
 *
 *   Step | Input | Transition `currState = currState.next[c]` | currState.isMatch | Return
 *   -----+-------+--------------------------------------------+-------------------+-------
 *    1   |  'a'  | root  -> node("a")                         | false             | false
 *    2   |  'b'  | "a"   -> node("ab")                        | false             | false
 *    3   |  'c'  | "ab"  -> node("abc") [inherited from "bc"] | TRUE              | true
 *    4   |  'd'  | "abc" -> node("abcd")                      | TRUE              | true
 *
 * -- Pitfalls --
 * 1. Unbounded `StringBuilder` stream storage: Works on LeetCode's `Q = 40,000`, but in a
 *    long-running stream it leaks O(Q) memory forever. A `char[maxLen]` ring buffer caps
 *    stream memory at strictly O(L) = 200 bytes!
 * 2. Java-specific gotcha — Negative modulo when stepping backward in a ring buffer:
 *    In Java, `(0 - 1) % maxLen` evaluates to `-1` (NOT `maxLen - 1`!), throwing an
 *    `ArrayIndexOutOfBoundsException`! Always write `(idx - 1 + maxLen) % maxLen` (or
 *    `if (--idx < 0) idx = maxLen - 1`).
 * 3. Stopping only at Trie leaves in Reversed Trie: If `words = ["cd", "bcd"]`, `"cd"` ends at
 *    an internal node (`'c'`). Always check `if (node.isEnd) return true;` at EVERY step.
 * 4. Forgetting `child.isMatch |= child.fail.isMatch` in Aho-Corasick: Without propagating
 *    matches through failure links, `"bc"` will be missed when the stream is `"abc"` inside `"abcd"`.
 *
 * -- Pattern recognition --
 * When you see: "Match any suffix of a character stream against a dictionary of patterns"
 * -> Think **Reversed Trie** (O(L) per char, dead simple) OR **Aho-Corasick Automaton** (strict
 *    O(1) per char, multi-pattern KMP).
 * Transfers directly to:
 *   - Word Search II / Prefix & Suffix Search (LeetCode 212, 745) — Trie direction flipping
 *   - Multi-pattern intrusion detection / profanity filtering / DNA motif scanning — Aho-Corasick
 *
 * -- Interview script --
 * "Checking every word against the stream tail takes O(W * L) per query, which is too slow for
 * 40,000 queries. A forward Trie is awkward because up to `L` different suffixes can be in
 * progress at once. The key observation is that every match is anchored at the newest character
 * going backward: `w` is a suffix of the stream if and only if `reverse(w)` is a prefix of the
 * reversed stream. So I'll insert all words in reverse into a Trie, keep only the last `maxLen`
 * characters in a fixed-size circular buffer, and on each `query(letter)` walk backward down the
 * Reversed Trie — returning `true` the instant I hit an `isEnd` node, or `false` on a missing
 * edge. That gives us O(S) constructor time, O(L) worst-case time per query (often O(1)), and
 * O(S) space. If `L` were huge (say 10^5), we could instead compile a forward Aho-Corasick
 * automaton via BFS to achieve strict O(1) time per query."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: What if `words[i].length` can be up to `10^5` (so O(L) per query would TLE)?
 * A1: Switch to Approach 3 (**Aho-Corasick Automaton**). Precompiling the failure links and
 *     26-letter DFA transitions in O(S) constructor time makes every `query(letter)` strict O(1)
 *     worst-case — completely independent of word length `L`!
 *
 * Q2: What if instead of a boolean, `query(letter)` must return ALL dictionary words that match
 *     a suffix ending at `letter`?
 * A2: - In the **Reversed Trie**, simply don't return early on the first `node.isEnd` — collect
 *       `node.word` into a result list and keep walking up to `maxLen` steps (still O(L)).
 *     - In **Aho-Corasick**, add an `outputLink` (dictionary suffix link) pointing to the nearest
 *       ancestor along the `fail` chain that is a complete word end, so you jump directly between
 *       matches in O(1 + number of matches) without walking non-matching failure nodes.
 *
 * Q3: What if new words can be added dynamically at runtime (`addWord(String word)`)?
 * A3: The **Reversed Trie** handles `addWord` natively in O(word.length())! (If `word.length()`
 *     exceeds `maxLen`, resize the ring buffer or use a `Deque<Character>` capped at `maxLen`.)
 *     In contrast, Aho-Corasick's DFA transitions depend globally on all words, so dynamic inserts
 *     would require rebuilding the automaton (or using a logarithmic Bentley-Saxe dynamization).
 * =========================================================================================
 */

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class StreamOfCharactersStudyNote {

    // Common interface so our test harness can cross-check all 3 approaches identically.
    interface Checker {
        boolean query(char letter);
    }

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force (O(W * L) per query, O(Q) stream space)
    // ----------------------------------------------------------------------
    static class StreamCheckerBruteForce implements Checker {
        private final String[] words;
        private final StringBuilder stream = new StringBuilder();

        public StreamCheckerBruteForce(String[] words) {
            this.words = words;
        }

        @Override
        public boolean query(char letter) {
            stream.append(letter);
            int streamLen = stream.length();

            // Check every word in the dictionary against the current tail of the stream.
            for (String word : words) {
                int wLen = word.length();
                if (wLen > streamLen) continue;

                boolean match = true;
                for (int i = 0; i < wLen; i++) {
                    if (stream.charAt(streamLen - wLen + i) != word.charAt(i)) {
                        match = false;
                        break;
                    }
                }
                if (match) return true;
            }
            return false;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Reversed Trie + Bounded Ring Buffer (INTERVIEW OPTIMAL)
    // (Constructor: O(S), Query: O(L) worst-case, Space: O(S + L) = O(S))
    // ----------------------------------------------------------------------
    static class StreamCheckerReversedTrie implements Checker {
        private static class TrieNode {
            // Strictly lowercase 'a'-'z', so a 26-slot array gives O(1) zero-boxing lookups.
            final TrieNode[] children = new TrieNode[26];
            boolean isEnd = false;
        }

        private final TrieNode root = new TrieNode();

        // Fixed-size circular buffer holding ONLY the last `maxLen` characters of the stream.
        // Prevents O(Q) unbounded memory growth on long-running streams.
        // Example (maxLen=2) after 'a', 'b', 'c': ring = ['c', 'b'], writePos = 1, count = 2.
        private final char[] ring;
        private final int maxLen;
        private int writePos = 0; // Index where the next incoming character will be written.
        private int count = 0;    // Number of valid characters currently in `ring` (<= maxLen).

        public StreamCheckerReversedTrie(String[] words) {
            int longest = 0;
            for (String word : words) {
                longest = Math.max(longest, word.length());
                insertReversed(word);
            }
            this.maxLen = longest;
            this.ring = new char[maxLen];
        }

        private void insertReversed(String word) {
            TrieNode node = root;
            // Insert right-to-left so suffixes of the stream become prefixes in this Trie.
            for (int i = word.length() - 1; i >= 0; i--) {
                int idx = word.charAt(i) - 'a';
                if (node.children[idx] == null) {
                    node.children[idx] = new TrieNode();
                }
                node = node.children[idx];
            }
            node.isEnd = true;
        }

        @Override
        public boolean query(char letter) {
            // 1. Write incoming letter into the circular buffer and advance writePos.
            int idx = writePos;
            ring[idx] = letter;
            writePos = (writePos + 1 == maxLen) ? 0 : writePos + 1;
            if (count < maxLen) {
                count++;
            }

            // 2. Walk backward from the newest character (`idx`) through the Reversed Trie.
            TrieNode node = root;
            for (int step = 0; step < count; step++) {
                node = node.children[ring[idx] - 'a'];
                if (node == null) {
                    // No word in the dictionary ends with this suffix (or any longer suffix).
                    return false;
                }
                if (node.isEnd) {
                    // Check at EVERY step (not just leaves) to catch nested words like "cd" inside "bcd".
                    return true;
                }
                // Step backward in the ring buffer safely (avoiding Java's negative `%` result!).
                idx = (idx == 0) ? maxLen - 1 : idx - 1;
            }
            return false;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Aho-Corasick Automaton / DFA (STRICT O(1) QUERY OPTIMAL)
    // (Constructor: O(S * 26), Query: Strict O(1), Space: O(S * 26), 0 stream buffer!)
    // ----------------------------------------------------------------------
    static class StreamCheckerAhoCorasick implements Checker {
        private static class AcNode {
            // Precompiled 26-letter DFA transition table: after build(), EVERY slot is non-null!
            final AcNode[] next = new AcNode[26];
            // Failure link: longest proper suffix of this node's string that exists in the Trie.
            AcNode fail;
            // True if this node's string OR ANY of its proper suffixes is a complete word in `words`.
            boolean isMatch = false;
        }

        private final AcNode root = new AcNode();

        // Single live automaton cursor representing the longest suffix of the stream so far
        // that is a prefix of some word in `words`. Zero stream characters need to be stored!
        private AcNode currState = root;

        public StreamCheckerAhoCorasick(String[] words) {
            // Phase 1: Build a standard FORWARD Trie of all words.
            for (String word : words) {
                AcNode node = root;
                for (int i = 0; i < word.length(); i++) {
                    int idx = word.charAt(i) - 'a';
                    if (node.next[idx] == null) {
                        node.next[idx] = new AcNode();
                    }
                    node = node.next[idx];
                }
                node.isMatch = true;
            }

            // Phase 2: BFS to compute failure links, propagate `isMatch`, and complete DFA edges.
            // Example for words = ["abcd", "bc"]:
            //   fail("ab") = "b", fail("abc") = "bc" -> "abc".isMatch becomes TRUE via propagation!
            Queue<AcNode> queue = new ArrayDeque<>();
            root.fail = root;

            for (int c = 0; c < 26; c++) {
                if (root.next[c] != null) {
                    root.next[c].fail = root;
                    queue.offer(root.next[c]);
                } else {
                    // Missing transition from root simply stays at root.
                    root.next[c] = root;
                }
            }

            while (!queue.isEmpty()) {
                AcNode curr = queue.poll();
                for (int c = 0; c < 26; c++) {
                    if (curr.next[c] != null) {
                        AcNode child = curr.next[c];
                        // `curr.fail` has strictly smaller depth, so `curr.fail.next[c]` is already finalized.
                        child.fail = curr.fail.next[c];
                        // Crucial: if any proper suffix of `child` is a word, reaching `child` is a match!
                        child.isMatch = child.isMatch || child.fail.isMatch;
                        queue.offer(child);
                    } else {
                        // Complete the DFA: a missing edge jumps straight to the failure fallback in O(1).
                        curr.next[c] = curr.fail.next[c];
                    }
                }
            }
        }

        @Override
        public boolean query(char letter) {
            // Strict O(1): one array lookup, zero loops, zero stream buffer!
            currState = currState.next[letter - 'a'];
            return currState.isMatch;
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 3 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private static boolean[] runStream(Checker checker, String stream) {
        boolean[] out = new boolean[stream.length()];
        for (int i = 0; i < stream.length(); i++) {
            out[i] = checker.query(stream.charAt(i));
        }
        return out;
    }

    private static void verifyCase(String name, String[] words, String stream, boolean[] expected) {
        boolean[] rBrute = runStream(new StreamCheckerBruteForce(words), stream);
        boolean[] rRev = runStream(new StreamCheckerReversedTrie(words), stream);
        boolean[] rAc = runStream(new StreamCheckerAhoCorasick(words), stream);

        boolean pass = Arrays.equals(rBrute, expected)
                && Arrays.equals(rRev, expected)
                && Arrays.equals(rAc, expected);

        System.out.printf("%-38s | %s%n", name, pass ? "PASS" : "FAIL");
        if (!pass) {
            System.out.println("  Expected:     " + Arrays.toString(expected));
            System.out.println("  Brute:        " + Arrays.toString(rBrute));
            System.out.println("  ReversedTrie: " + Arrays.toString(rRev));
            System.out.println("  AhoCorasick:  " + Arrays.toString(rAc));
        }
    }

    public static void main(String[] args) {
        // 1. LC Example 1
        verifyCase("LC Example 1",
                new String[]{"cd", "f", "kl"},
                "abcdefghijkl",
                new boolean[]{false, false, false, true, false, true, false, false, false, false, false, true});

        // 2. Edge: Nested suffix ("bc" inside "abcd") — tests ReversedTrie mid-path check & AC fail propagation
        verifyCase("Edge: Nested suffix ('bc' in 'abcd')",
                new String[]{"abcd", "bc"},
                "abcdxbc",
                new boolean[]{false, false, true, true, false, false, true});

        // 3. Edge: Single-char words (maxLen = 1 ring buffer wrap-around)
        verifyCase("Edge: Single-char words (maxLen=1)",
                new String[]{"a", "z"},
                "abza",
                new boolean[]{true, false, true, true});

        // 4. Edge: Repeated identical characters (overlapping matches)
        verifyCase("Edge: Overlapping repeated chars",
                new String[]{"aaa"},
                "aaaaa",
                new boolean[]{false, false, true, true, true});

        // 5. Edge: Duplicate dictionary words
        verifyCase("Edge: Duplicate words in dictionary",
                new String[]{"ab", "ab", "ab"},
                "aabb",
                new boolean[]{false, false, true, false});

        // 6. Edge: Stream much longer than maxLen (ring buffer wrap-around stress test)
        verifyCase("Edge: Long stream ring wrap-around",
                new String[]{"cd"},
                "xxxxxxcdxxcd",
                new boolean[]{false, false, false, false, false, false, false, true, false, false, false, true});

        // 7. Edge: No matches at all
        verifyCase("Edge: Zero matches",
                new String[]{"xyz", "mno"},
                "abcxyabmn",
                new boolean[]{false, false, false, false, false, false, false, false, false});
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Reversed Trie + Bounded Ring Buffer (interview optimum) / Aho-Corasick DFA
 *   (theoretical O(1) query optimum).
 * - Key observation: `w` is a suffix of the stream iff `reverse(w)` is a prefix of the reversed
 *   stream — flipping the Trie collapses up to `L` parallel forward searches into ONE walk
 *   anchored at the newest character.
 * - Worth memorizing vs. understanding: Understand *why* reversing the words eliminates multiple
 *   active pointers; memorize bounding the stream buffer to `maxLen` (with safe wrap-around
 *   `idx == 0 ? maxLen - 1 : idx - 1`) and propagating `child.isMatch |= child.fail.isMatch`
 *   in Aho-Corasick.
 * - Most common trap: Storing the whole stream in an unbounded `StringBuilder`, Java's negative
 *   `%` operator on `(0 - 1) % maxLen`, or forgetting mid-path `isEnd` checks on nested words.
 * - One-line mental trigger: "Match stream suffixes against a word list? Reverse the words into a Trie (or Aho-Corasick for O(1))."
 * =========================================================================================
 */














































class StreamChecker {

    /*
     * Each TrieNode represents one character.
     *
     * children[i] represents the next character:
     *
     * 0  -> a
     * 1  -> b
     * ...
     * 25 -> z
     *
     * isEnd indicates whether the current Trie path
     * represents a complete reversed dictionary word.
     */
    private static class TrieNode {

        TrieNode[] children = new TrieNode[26];

        boolean isEnd;
    }

    /*
     * Root of the reversed Trie.
     */
    private final TrieNode root;

    /*
     * Stores all characters received so far.
     *
     * We traverse this StringBuilder backwards
     * whenever a new character arrives.
     */
    private final StringBuilder stream;

    /*
     * No dictionary word is longer than this value.
     *
     * Therefore, examining more than this many
     * stream characters is unnecessary.
     */
    private final int maxWordLength;

    public StreamChecker(String[] words) {

        root = new TrieNode();

        stream = new StringBuilder();

        int maxLength = 0;

        /*
         * STEP 1: Build the reversed Trie.
         *
         * Example:
         *
         * Original word: "abc"
         *
         * Insert:
         *
         * c -> b -> a
         *
         * This is important because queries arrive
         * from left to right, but suffix matching
         * needs to begin at the newest character.
         */
        for (String word : words) {

            maxLength = Math.max(maxLength, word.length());

            TrieNode node = root;

            /*
             * Traverse each word from right to left.
             */
            for (int i = word.length() - 1; i >= 0; i--) {

                int index = word.charAt(i) - 'a';

                /*
                 * Create the Trie node if it doesn't exist.
                 */
                if (node.children[index] == null) {

                    node.children[index] = new TrieNode();
                }

                node = node.children[index];
            }

            /*
             * Mark the last node as terminal.
             *
             * Reaching this node means that the characters
             * traversed backwards form a complete word.
             */
            node.isEnd = true;
        }

        maxWordLength = maxLength;
    }

    public boolean query(char letter) {

        /*
         * STEP 2: Append the newly received character.
         *
         * Example:
         *
         * Previous stream: "abc"
         * New character:   'd'
         *
         * Updated stream:  "abcd"
         */
        stream.append(letter);

        /*
         * STEP 3: Start from the Trie root.
         */
        TrieNode node = root;

        /*
         * STEP 4: Traverse the stream backwards.
         *
         * We inspect at most maxWordLength characters.
         *
         * Example:
         *
         * Stream: "axyz"
         *
         * Traversal:
         *
         * z -> y -> x -> a
         */
        int checked = 0;

        for (int i = stream.length() - 1;
             i >= 0 && checked < maxWordLength;
             i--) {

            /*
             * Convert the current stream character
             * into its Trie array index.
             */
            int index = stream.charAt(i) - 'a';

            /*
             * Follow the corresponding Trie edge.
             */
            node = node.children[index];

            /*
             * If this edge does not exist,
             * no dictionary word can match this suffix.
             *
             * Stop immediately.
             */
            if (node == null) {
                return false;
            }

            checked++;

            /*
             * If we reach a terminal node,
             * the characters examined so far form
             * a complete dictionary word.
             *
             * Therefore, a suffix match exists.
             */
            if (node.isEnd) {
                return true;
            }
        }

        /*
         * No complete dictionary word was found.
         */
        return false;
    }
}

