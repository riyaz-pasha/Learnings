/**
 * 0. ## Problem Statement
 * Design a search autocomplete system for a search engine. Users may input a sentence
 * (at least one word and end with a special character `'#'`).
 *
 * You are given a string array `sentences` and an integer array `times` both of length `n`,
 * where `sentences[i]` is a previously typed sentence and `times[i]` is the corresponding
 * number of times the sentence was typed.
 *
 * Implement the `AutocompleteSystem` class:
 * - `AutocompleteSystem(String[] sentences, int[] times)`: Initializes the system with
 *   historical data.
 * - `List<String> input(char c)`: Indicates that the user typed the character `c`:
 *   - If `c == '#'`, the current sentence is finished and saved into the system (incrementing
 *     its frequency by 1 if it already exists, or adding it with frequency 1). Reset the
 *     current input buffer and return an empty list `[]`.
 *   - Otherwise, return the top 3 historical hot sentences that have the exact same prefix
 *     as the part already typed:
 *     1. Sorted by hot degree (frequency) DESCENDING.
 *     2. Ties broken by ASCII-code order (lexicographically smaller string first).
 *     3. If fewer than 3 matching sentences exist, return all of them.
 *
 * Constraints:
 * - `n == sentences.length == times.length`, `1 <= n <= 100`
 * - `1 <= sentences[i].length <= 100`, `1 <= times[i] <= 50`
 * - `c` is a lowercase English letter `'a'-'z'`, a space `' '`, or `'#'`.
 * - At most 5000 calls will be made to `input(c)`.
 *
 * =========================================================================================
 * 1. ## Clarifying questions
 * 1. "What characters can appear in sentences — only lowercase English letters and spaces, or
 *    also uppercase, digits, punctuation, and Unicode?"
 *    -> Determines whether Trie children can be a compact `TrieNode[27]` array (`'a'-'z'` + `' '`)
 *       or must use a `HashMap<Character, TrieNode>`.
 * 2. "How are ties broken — standard ASCII order where space `' '` (32) comes BEFORE `'a'` (97)?"
 *    -> Confirms Java's `String.compareTo()` works out-of-the-box without a custom character order.
 * 3. "Is the system read-heavy (many keystrokes per completed search) or write-heavy?"
 *    -> Drives the core architecture choice: caching top-3 lists at every Trie node gives O(1)
 *       reads at the cost of slightly heavier writes on `'#'`.
 * 4. "What happens if a user types `'#'` immediately on an empty buffer, or types a prefix with
 *    zero historical matches and then keeps typing more characters before `'#'`?"
 *    -> Prevents a classic bug where you stop appending to the buffer once the Trie pointer hits
 *       `null`, which would lose the new sentence when `'#'` finally arrives!
 * 5. "Can the initial `sentences` array contain duplicate sentences?"
 *    -> Using `freq.merge(s, t, Integer::sum)` instead of `put` safely handles duplicates if any exist.
 * 6. "Is `K = 3` (number of suggestions) fixed, or could it be a configurable parameter?"
 *    -> Caching a top-K list per Trie node is great when `K` is tiny (3-10), but becomes memory-heavy
 *       if `K` is large (e.g., 1,000).
 *
 * =========================================================================================
 * 2. ## The reasoning journey (core requirement)
 * Binding constraint: Users type one character at a time (up to 5,000 `input()` calls, and in a
 * real search engine, millions of keystrokes per second across millions of historical queries).
 * Each keystroke must feel instantaneous — scanning all historical sentences on every keystroke
 * falls over as the dictionary grows.
 *
 * Let:
 *   N = total number of unique sentences stored
 *   L = average (or max) length of a sentence
 *   P = length of the current typed prefix
 *   M = number of sentences matching the current prefix
 *   K = 3 (number of top suggestions returned)
 *
 * --- Approach 1: Brute Force HashMap Scan ---
 * 0. Idea/Intuition: Store `Map<String, Integer> freq` and a `StringBuilder prefix`.
 *    On normal `c`: append `c`, loop over EVERY sentence in `freq`, keep those where
 *    `sentence.startsWith(prefix)`, pick the top 3 using a min-heap of size 3 (or sort).
 *    On `'#'`: increment `freq` for `prefix.toString()`, clear `prefix`, return `[]`.
 * 1. What I'd naturally try: Literally store `(sentence -> count)` and filter on every keystroke.
 * 2. Why it works: Every sentence's exact frequency is always up-to-date, and `startsWith` +
 *    sorting guarantees the exact top 3.
 * 3. Why it's too slow/costly: Every single keystroke scans ALL `N` sentences in the dictionary
 *    and re-compares the entire prefix of length `P` from character 0! Even if 99.9% of sentences
 *    don't start with `'i'`, we still touch every single one of them.
 * 4. What work is being repeated:
 *    (a) Re-checking sentences that already failed to match earlier characters of the prefix.
 *    (b) Re-comparing characters `0 .. P-2` of the prefix on every new keystroke.
 * 5. Observation that removes the bottleneck: Group sentences by shared prefixes in a **Trie**,
 *    and keep a live cursor `currNode` that steps down ONE edge per keystroke (`currNode = currNode.children[c]`).
 *    Now we never touch non-matching sentences and never re-verify earlier prefix characters!
 * 6. Leads to Approach 2: Trie + Subtree DFS.
 *    Time Complexity:
 *    - Constructor: O(N * L) — hashes N strings of length L into the `HashMap`.
 *    - `input(c)` (normal): O(N * P + M * L log K) — scans all N keys checking `startsWith(prefix)`
 *      in O(P), then maintains a size-3 heap over M matches (string comparison costs up to O(L)).
 *    - `input('#')`: O(P) — builds string from `StringBuilder` and hashes it into `freq`.
 *    Space Complexity: O(N * L) — stores all N sentences of length L in the `HashMap`.
 *
 * --- Approach 2: Trie + Subtree DFS (Standard Trie) ---
 * 0. Idea/Intuition: Store all sentences in a Trie where terminal nodes hold `(sentence, freq)`.
 *    Maintain a live pointer `currNode` starting at `root`.
 *    On normal `c`: step `currNode` to `currNode.children[idx]` in O(1). If `currNode == null`,
 *    return `[]` immediately! Otherwise, DFS `currNode`'s subtree to collect all terminal sentences
 *    and keep the top 3 in a size-3 min-heap.
 *    On `'#'`: insert/increment the finished sentence in the Trie, reset `currNode = root` and buffer.
 * 1. What I'd naturally try: "Prefix matching -> Trie!" Advance one node per keystroke, DFS below.
 * 2. Why it works: `currNode`'s subtree contains *exclusively* sentences sharing the exact current
 *    prefix — zero wasted checks on unrelated branches, and zero re-checking of prefix chars `0..P-1`.
 * 3. Why it's still too slow/costly: Think about what happens on the VERY FIRST keystroke (e.g., `'i'`
 *    or `'t'`): a huge fraction of the entire Trie sits underneath that single character! Doing a full
 *    DFS of thousands (or millions) of nodes on every early keystroke is still O(T_sub) per keystroke,
 *    where `T_sub` is the total number of nodes in `currNode`'s subtree.
 * 4. What work is being repeated: Every time a user types `'i'`, we re-traverse the entire `'i'`
 *    subtree and re-rank all matching sentences — even though nothing changed since the last time
 *    someone typed `'i'`, and we ONLY ever return the top 3!
 * 5. Observation that removes the bottleneck: Autocomplete is **read-heavy** and `K = 3` is tiny.
 *    Instead of computing the top 3 on every READ (`input(c)`), precompute and cache a sorted
 *    `List<String> top3` (max size 3) directly on EVERY Trie node during WRITES (constructor + `'#'`)!
 *    Then every normal keystroke `input(c)` does ZERO traversal and ZERO sorting — just one pointer
 *    hop and returning the cached list!
 * 6. Leads to Approach 3: Trie with Cached Top-3 List per Node.
 *    Time Complexity:
 *    - Constructor: O(N * L) — inserts N sentences of length L into the Trie.
 *    - `input(c)` (normal): O(T_sub + M * L log K) — steps 1 edge, DFS traverses all `T_sub` nodes
 *      under `currNode`, and ranks M matches via a size-3 heap.
 *    - `input('#')`: O(P) — walks P characters from root to insert/increment the finished sentence.
 *    Space Complexity: O(N * L * 27) = O(N * L) — at most `N * L` Trie nodes, each with a fixed
 *    27-slot child array (plus terminal sentence strings).
 *
 * --- Approach 3: Trie with Cached Top-3 at Every Node (Optimal for Reads) ---
 * 0. Idea/Intuition: Combine a global `Map<String, Integer> freq` with a Trie where EVERY node
 *    caches `List<String> top3` — the exact top 3 hottest sentences in that node's subtree,
 *    already sorted by `(freq DESC, sentence ASC)`.
 *    - On normal `c`: append `c` to `prefix`. If `currNode != null`, advance `currNode = currNode.children[idx]`.
 *      If `currNode == null`, return `[]`; else return `new ArrayList<>(currNode.top3)`!
 *    - On `'#'`: increment `freq` for `s = prefix.toString()`, walk `s` from `root` (length `P`),
 *      and at each node along the path: ensure `s` is in `node.top3`, re-sort the tiny list
 *      (at most 4 items!), and trim to size 3. Reset `currNode = root` and `prefix`.
 * 1. Why someone thinks of it: Classic read/write trade-off — shift work from the hot path
 *    (every single keystroke) to the cold path (pressing Enter/`'#'`), exploiting that `K = 3` is tiny.
 * 2. Why it works: Whenever a sentence `s` is added or its frequency increases, `s` can ONLY
 *    affect the top-3 rankings of Trie nodes that are prefixes of `s` — i.e., the exact `P` nodes
 *    on the path from `root` to `s`! No other Trie node's subtree contains `s`. And because `s`'s
 *    frequency only ever increases (never decreases), a sentence outside a node's `top3` can never
 *    become top-3 unless it is the one currently being updated!
 *    Time Complexity:
 *    - Constructor: O(N * L^2) — inserts N sentences of length L; at each of the L nodes, sorting
 *      up to 4 strings compares up to O(L) characters on ties (in practice O(N * L) since most
 *      comparisons resolve on `freq` or first differing char).
 *    - `input(c)` (normal keystroke): **O(1)** — one array lookup `currNode.children[idx]` and
 *      copying a list of at most 3 string references!
 *    - `input('#')`: O(P * L) worst-case (O(P) typical) — walks P nodes along the sentence's path,
 *      updating a <= 4-element `top3` list at each node.
 *    Space Complexity: O(N * L) — at most `N * L` Trie nodes; each node stores at most 3 `String`
 *    *references* (not copies!) pointing to the canonical strings in `freq`, so `top3` adds only
 *    O(1) pointer overhead per node.
 *
 * Interview Verdict:
 * Write Approach 3 (Trie + Cached Top-3 per Node) in a 45-minute interview. It delivers strict
 * O(1) latency per keystroke (which is the entire engineering point of an autocomplete system),
 * cleanly separates frequency state (`freq` map) from prefix routing (`Trie`), and avoids the
 * recursion/DFS boilerplate of Approach 2. Keep the `top3` update helper to ~6 lines by sorting
 * the <= 4-element list with a single shared comparator.
 *
 * =========================================================================================
 * 3. ## Edge cases (required)
 * - Dead prefix followed by more typing then `'#'`: User types `'z'`, `'x'`, `'#'` where `'z'`
 *   isn't in the Trie. `currNode` becomes `null` on `'z'`, stays `null` on `'x'`, AND `prefix`
 *   still collects `"zx"` so `'#'` inserts `"zx"` with freq 1!
 * - Space character `' '` in prefix and ASCII tie-breaking: `' '` (ASCII 32) < `'a'` (ASCII 97),
 *   so `"i a"` < `"i love"` and `"a b"` < `"aa"`.
 * - Fewer than 3 matches (0, 1, or 2 matches): Must return only the existing matches, never pad
 *   with nulls or throw `IndexOutOfBoundsException`.
 * - Existing sentence typed again (frequency bump): `"i a"` starts at freq 1, gets typed again
 *   -> freq 2 -> must NOT appear twice in `top3`, and must bubble up past lower/tied entries.
 * - Sentence is a prefix of another sentence (e.g., `"a"` and `"ab"`): Both are valid matches
 *   for prefix `"a"`, and `"a"` < `"ab"` on ties.
 * - Multiple consecutive sentences in one session: `'#'` MUST reset BOTH `currNode = root` AND
 *   `prefix.setLength(0)` so the next search starts cleanly.
 *
 * =========================================================================================
 * 4. ## Also include
 *
 * -- Key insight --
 * Question to ask yourself: "When a sentence's frequency increases, WHICH prefixes' top-3 lists
 * could possibly change — and does anything ever get demoted from outside the top-3?"
 * Correctness intuition: A sentence `s` only belongs to the subtrees of its own prefixes (the
 * `L` nodes on the path from `root` to `s`). And because frequencies only increase (never decrease),
 * no other hidden sentence outside a node's `top3` can suddenly overtake a current top-3 member.
 * Therefore, maintaining `top3` incrementally on just those `L` nodes is 100% exact and turns
 * every keystroke read into O(1).
 *
 * -- Examples & ASCII Diagram --
 * Init: ["i love you" (5), "island" (3), "iroman" (2), "i love leetcode" (2)]
 *
 * Trie with Cached Top-3 Lists (showing key nodes):
 *
 *   (root)
 *     |
 *    'i' --> top3: ["i love you"(5), "island"(3), "i love leetcode"(2)]
 *     |              (note: "i love leetcode" beats "iroman" because ' '(32) < 'r'(114))
 *     +--- ' ' --> top3: ["i love you"(5), "i love leetcode"(2)]
 *     |     |
 *     |    'l' --> top3: ["i love you"(5), "i love leetcode"(2)] ...
 *     |
 *     +--- 's' --> top3: ["island"(3)] ...
 *     |
 *     +--- 'r' --> top3: ["iroman"(2)] ...
 *
 * User types 'i' -> currNode moves to 'i' -> instant O(1) return of 'i'.top3!
 * User types ' ' -> currNode moves to ' ' -> instant O(1) return of ' '.top3!
 * User types 'a' -> children['a'] is null -> currNode = null -> return []!
 * User types '#' -> insert "i a" (freq 1):
 *   - 'i'.top3: ["i love you"(5), "island"(3), "i love leetcode"(2), "i a"(1)] -> trimmed to top 3
 *   - ' '.top3: ["i love you"(5), "i love leetcode"(2), "i a"(1)] -> "i a" enters top 3!
 *   - 'a'.top3: ["i a"(1)]
 *   - reset currNode = root, prefix = ""
 *
 * -- Dry run 1: Optimal Cached-Top3 Trie on LC Example 1 + Round 2 --
 *   Step | Input | Buffer After | currNode | Action / Return
 *   -----+-------+--------------+----------+-------------------------------------------------
 *    1   |  'i'  | "i"          | node('i')| return ["i love you", "island", "i love leetcode"]
 *    2   |  ' '  | "i "         | node(' ')| return ["i love you", "i love leetcode"]
 *    3   |  'a'  | "i a"        | null     | no child 'a' -> return []
 *    4   |  '#'  | ""           | root     | save "i a"(freq=1) along i->' '->a; return []
 *    5   |  'i'  | "i"          | node('i')| return ["i love you", "island", "i love leetcode"]
 *    6   |  ' '  | "i "         | node(' ')| return ["i love you", "i love leetcode", "i a"]
 *    7   |  'a'  | "i a"        | node('a')| return ["i a"]
 *    8   |  '#'  | ""           | root     | bump "i a"(freq=2) along i->' '->a; return []
 *    9   |  'i'  | "i"          | node('i')| "i a"(2) beats "i love leetcode"(2) ('a'<'l')!
 *        |       |              |          | return ["i love you", "island", "i a"]
 *
 * -- Dry run 2: Edge Case — Dead Prefix with Multiple Trailing Chars ("zx#") --
 *   Step | Input | Buffer After | currNode | Action / Return
 *   -----+-------+--------------+----------+-------------------------------------------------
 *    1   |  'z'  | "z"          | null     | root.children['z'] == null -> return []
 *    2   |  'x'  | "zx"         | null     | STILL append 'x' to buffer! currNode stays null -> []
 *    3   |  '#'  | ""           | root     | save "zx"(freq=1) into Trie; reset currNode=root -> []
 *    4   |  'z'  | "z"          | node('z')| now exists! return ["zx"]
 *
 * -- Pitfalls --
 * 1. Early-returning before appending to `prefix` when `currNode == null`: If you write
 *    `if (currNode == null) return Collections.emptyList();` BEFORE `prefix.append(c)`, you
 *    drop characters typed after the dead branch and corrupt the sentence saved on `'#'`!
 * 2. Adding duplicate strings into `node.top3`: When an existing sentence's frequency increases,
 *    check `if (!node.top3.contains(sentence))` before adding; otherwise `"i a"` appears twice!
 * 3. Returning `currNode.top3` directly instead of a copy (`new ArrayList<>(currNode.top3)`):
 *    If the caller mutates the returned list (or if our next `'#'` mutates `top3` in place while
 *    the caller still holds a reference to an earlier return value), previously returned results
 *    get silently corrupted!
 * 4. Java-specific gotcha — Forgetting `' '` in array indexing: Mapping `'a'-'z'` via `c - 'a'`
 *    gives `-65` for `' '` (`32 - 97`), causing an instant `ArrayIndexOutOfBoundsException`!
 *    Always map `' '` explicitly to index `26`.
 *
 * -- Pattern recognition --
 * When you see: "Prefix search + Top-K hot/frequent items + live character-by-character stream"
 * -> Think **Trie + Cached Top-K List per Node** (when K is small and frequencies don't decrease).
 * Transfers directly to:
 *   - Search Suggestions System (LeetCode 1268) — static dictionary, lexicographical top-3 per prefix
 *   - Design In-Memory File System / Prefix-based Routing — Trie with metadata cached at intermediate nodes
 *
 * -- Interview script --
 * "A brute-force `HashMap` scans every stored sentence on every keystroke, which is O(N * P) per
 * character. Moving to a standard Trie lets us step one node per keystroke, but on short prefixes
 * like `'i'`, DFSing the entire subtree still touches thousands of nodes on every keystroke.
 * Since autocomplete is heavily read-dominated and we only ever need the top 3 results — and
 * sentence frequencies only increase — I can cache a pre-sorted `top3` list directly at every
 * Trie node, backed by a single global frequency map. Each normal keystroke just advances a
 * `currNode` pointer and returns a copy of `currNode.top3` in O(1) time. On `'#'`, I increment
 * the sentence's frequency and update the <= 4-element `top3` list on each of the `P` nodes
 * along its path. That gives us O(1) time per keystroke, O(P * L) worst-case on `'#'`, and
 * O(N * L) space."
 *
 * =========================================================================================
 * 5. ## Follow-ups (required)
 * Q1: What if sentences can EXPIRE or have their frequencies DECREASED (e.g., trending searches
 *     over the last 24 hours)?
 * A1: Caching only `top3` per node breaks if a top-3 item's score drops below the hidden 4th-best
 *     item (we wouldn't know who #4 is without re-scanning!). To handle decreases, either:
 *     (a) Cache a `TreeSet` (or indexed heap) of ALL sentences in each node's subtree (higher
 *         memory, O(log M) update, O(1) top-3 read), OR
 *     (b) Periodically rebuild the cached `top3` lists offline in a background batch job (which
 *         is how real production search engines like Google Suggest handle trending decay!).
 *
 * Q2: What if the dictionary has 100 million sentences and won't fit on a single machine?
 * A2: Shard the Trie across multiple servers by prefix (e.g., range-sharding `'a'-'c'`, `'d'-'f'`,
 *     or dynamic prefix-based sharding so hot prefixes like `'how to '` get dedicated shards),
 *     and serve cached top-K lists from memory/CDN. Because each node already stores its `top3`,
 *     a single shard lookup answers a keystroke without cross-shard scatter-gather.
 *
 * Q3: What if users can press Backspace (delete the last typed character)?
 * A3: Instead of a single `currNode` pointer, maintain a `Deque<TrieNode> pathStack` recording
 *     the node visited for each character in `prefix` (pushing `null` once we enter a dead branch).
 *     On Backspace, pop both `prefix` and `pathStack` in O(1) to restore the exact previous node!
 *
 * Q4: What if we need fuzzy matching (1-character typos) in addition to exact prefixes?
 * A4: A single `currNode` pointer only tracks exact matches. We'd run a bounded edit-distance
 *     DFS/BFS (Levenshtein automaton with distance <= 1) over the Trie from `root`, pruning any
 *     branch whose remaining edit budget drops below 0.
 * =========================================================================================
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class SearchAutocompleteStudyNote {

    // ----------------------------------------------------------------------
    // APPROACH 1: Brute Force HashMap Scan
    // (O(N * P + M * L log 3) per normal keystroke, O(P) on '#', O(N * L) space)
    // ----------------------------------------------------------------------
    static class AutocompleteBruteForce {
        // Maps every known sentence -> its total typed count.
        // Example: {"i love you"=5, "island"=3, "iroman"=2, "i love leetcode"=2}
        private final Map<String, Integer> freq = new HashMap<>();
        private final StringBuilder currentPrefix = new StringBuilder();

        public AutocompleteBruteForce(String[] sentences, int[] times) {
            for (int i = 0; i < sentences.length; i++) {
                freq.merge(sentences[i], times[i], Integer::sum);
            }
        }

        public List<String> input(char c) {
            if (c == '#') {
                freq.merge(currentPrefix.toString(), 1, Integer::sum);
                currentPrefix.setLength(0);
                return Collections.emptyList();
            }

            currentPrefix.append(c);
            String prefix = currentPrefix.toString();

            // Min-heap of size 3: WORST candidate sits at root so we can evict it in O(log 3).
            // "Worst" means lower frequency, or on a tie, lexicographically LARGER string.
            PriorityQueue<String> minHeap = new PriorityQueue<>((a, b) -> {
                int cmp = Integer.compare(freq.get(a), freq.get(b));
                return cmp != 0 ? cmp : b.compareTo(a);
            });

            // Bottleneck: scans EVERY sentence in the entire dictionary on every keystroke.
            for (String sentence : freq.keySet()) {
                if (sentence.startsWith(prefix)) {
                    minHeap.offer(sentence);
                    if (minHeap.size() > 3) {
                        minHeap.poll(); // Evict the 4th-best candidate.
                    }
                }
            }

            // Pop from min-heap (worst to best), then reverse to get best to worst.
            List<String> result = new ArrayList<>(minHeap.size());
            while (!minHeap.isEmpty()) {
                result.add(minHeap.poll());
            }
            Collections.reverse(result);
            return result;
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 2: Standard Trie + Subtree DFS
    // (O(T_sub + M * L log 3) per normal keystroke, O(P) on '#', O(N * L) space)
    // ----------------------------------------------------------------------
    static class AutocompleteTrieDFS {
        private static class TrieNode {
            // Fixed 27-slot alphabet: 0..25 for 'a'..'z', 26 for space ' '.
            final TrieNode[] children = new TrieNode[27];
            // Non-null only at terminal nodes representing a complete historical sentence.
            String sentence;
            int count;
        }

        private final TrieNode root = new TrieNode();
        // Live cursor pointing to the node for `currentPrefix`, or `null` on a dead branch.
        private TrieNode currNode = root;
        private final StringBuilder currentPrefix = new StringBuilder();

        public AutocompleteTrieDFS(String[] sentences, int[] times) {
            for (int i = 0; i < sentences.length; i++) {
                insert(sentences[i], times[i]);
            }
        }

        public List<String> input(char c) {
            if (c == '#') {
                insert(currentPrefix.toString(), 1);
                currentPrefix.setLength(0);
                currNode = root; // Reset cursor for the next search session.
                return Collections.emptyList();
            }

            // Always append to buffer BEFORE checking dead branch so '#' saves the full sentence.
            currentPrefix.append(c);
            if (currNode != null) {
                currNode = currNode.children[charToIdx(c)];
            }
            if (currNode == null) {
                return Collections.emptyList(); // Dead prefix: O(1) fast-path!
            }

            // Min-heap of size 3 storing terminal TrieNodes (worst at root).
            PriorityQueue<TrieNode> minHeap = new PriorityQueue<>((a, b) -> {
                int cmp = Integer.compare(a.count, b.count);
                return cmp != 0 ? cmp : b.sentence.compareTo(a.sentence);
            });

            // Bottleneck on short prefixes: DFS must visit every node in currNode's subtree.
            dfsCollect(currNode, minHeap);

            List<String> result = new ArrayList<>(minHeap.size());
            while (!minHeap.isEmpty()) {
                result.add(minHeap.poll().sentence);
            }
            Collections.reverse(result);
            return result;
        }

        private void insert(String sentence, int delta) {
            TrieNode node = root;
            for (int i = 0; i < sentence.length(); i++) {
                int idx = charToIdx(sentence.charAt(i));
                if (node.children[idx] == null) {
                    node.children[idx] = new TrieNode();
                }
                node = node.children[idx];
            }
            node.sentence = sentence;
            node.count += delta;
        }

        private void dfsCollect(TrieNode node, PriorityQueue<TrieNode> minHeap) {
            if (node.sentence != null) {
                minHeap.offer(node);
                if (minHeap.size() > 3) {
                    minHeap.poll();
                }
            }
            for (TrieNode child : node.children) {
                if (child != null) {
                    dfsCollect(child, minHeap);
                }
            }
        }

        private static int charToIdx(char c) {
            return c == ' ' ? 26 : c - 'a';
        }
    }

    // ----------------------------------------------------------------------
    // APPROACH 3: Trie with Cached Top-3 List per Node (OPTIMAL)
    // (O(1) per normal keystroke, O(P * L) worst-case on '#', O(N * L) space)
    // ----------------------------------------------------------------------
    static class AutocompleteTrieCachedTop3 {
        private static class TrieNode {
            // Fixed 27-slot array: 0..25 for 'a'..'z', 26 for space ' '.
            final TrieNode[] children = new TrieNode[27];

            // Pre-sorted top 3 hottest sentences in this node's subtree (max size 3).
            // Example at node('i'): ["i love you", "island", "i love leetcode"].
            // Stores String references (not copies), so extra memory is just 3 pointers/node.
            final List<String> top3 = new ArrayList<>(4);
        }

        // Single source of truth for sentence frequencies, referenced by `hotterFirst`.
        // Example: {"i love you"=5, "island"=3, "iroman"=2, "i love leetcode"=2}
        private final Map<String, Integer> freq = new HashMap<>();

        // Canonical ranking: higher frequency first; ties broken by ASCII order ascending.
        private final Comparator<String> hotterFirst = (a, b) -> {
            int cmp = Integer.compare(freq.get(b), freq.get(a));
            return cmp != 0 ? cmp : a.compareTo(b);
        };

        private final TrieNode root = new TrieNode();
        private TrieNode currNode = root;
        private final StringBuilder currentPrefix = new StringBuilder();

        public AutocompleteTrieCachedTop3(String[] sentences, int[] times) {
            for (int i = 0; i < sentences.length; i++) {
                addSentence(sentences[i], times[i]);
            }
        }

        public List<String> input(char c) {
            if (c == '#') {
                // Save the completed sentence (incrementing its frequency and updating top3
                // along its prefix path), then reset state for the next search.
                addSentence(currentPrefix.toString(), 1);
                currentPrefix.setLength(0);
                currNode = root;
                return Collections.emptyList();
            }

            // Always record `c` in the buffer even if `currNode` is already null (dead branch).
            currentPrefix.append(c);
            if (currNode != null) {
                currNode = currNode.children[charToIdx(c)];
            }
            if (currNode == null) {
                return Collections.emptyList();
            }

            // O(1) read: return a defensive copy of the <= 3 pre-sorted suggestions so
            // future '#' mutations to `currNode.top3` never corrupt previously returned lists.
            return new ArrayList<>(currNode.top3);
        }

        private void addSentence(String sentence, int delta) {
            // 1. Update global frequency first so `hotterFirst` sees the new count immediately.
            freq.merge(sentence, delta, Integer::sum);

            // 2. Walk every prefix node of `sentence` and refresh its cached `top3` list.
            //    No other node in the Trie contains `sentence`, so no other `top3` can change.
            TrieNode node = root;
            for (int i = 0; i < sentence.length(); i++) {
                int idx = charToIdx(sentence.charAt(i));
                if (node.children[idx] == null) {
                    node.children[idx] = new TrieNode();
                }
                node = node.children[idx];

                // Avoid duplicates when an existing top-3 sentence gets a frequency bump.
                if (!node.top3.contains(sentence)) {
                    node.top3.add(sentence);
                }
                // Sorting at most 4 elements is O(1) comparisons (each string tie-break <= O(L)).
                node.top3.sort(hotterFirst);
                if (node.top3.size() > 3) {
                    node.top3.remove(3); // Keep strictly the top 3.
                }
            }
        }

        private static int charToIdx(char c) {
            return c == ' ' ? 26 : c - 'a';
        }
    }

    // ----------------------------------------------------------------------
    // TEST HARNESS — Cross-checking all 3 approaches on representative + edge cases
    // ----------------------------------------------------------------------
    private static void verifyCase(
            String name,
            String[] initSentences,
            int[] initTimes,
            String keystrokes,
            List<List<String>> expected) {

        AutocompleteBruteForce brute = new AutocompleteBruteForce(initSentences, initTimes);
        AutocompleteTrieDFS dfs = new AutocompleteTrieDFS(initSentences, initTimes);
        AutocompleteTrieCachedTop3 cached = new AutocompleteTrieCachedTop3(initSentences, initTimes);

        List<List<String>> rBrute = new ArrayList<>();
        List<List<String>> rDfs = new ArrayList<>();
        List<List<String>> rCached = new ArrayList<>();

        for (int i = 0; i < keystrokes.length(); i++) {
            char c = keystrokes.charAt(i);
            rBrute.add(brute.input(c));
            rDfs.add(dfs.input(c));
            rCached.add(cached.input(c));
        }

        boolean pass = rBrute.equals(expected)
                && rDfs.equals(expected)
                && rCached.equals(expected);

        System.out.printf("%-38s | %s%n", name, pass ? "PASS" : "FAIL");
        if (!pass) {
            System.out.println("  Expected: " + expected);
            System.out.println("  Brute:    " + rBrute);
            System.out.println("  TrieDFS:  " + rDfs);
            System.out.println("  Cached:   " + rCached);
        }
    }

    public static void main(String[] args) {
        // 1. LC Example 1 + Round 2 (tests dead branch insert, freq bump, and ASCII tie-break!)
        verifyCase(
                "LC Example 1 + Round 2 freq bump",
                new String[]{"i love you", "island", "iroman", "i love leetcode"},
                new int[]{5, 3, 2, 2},
                "i a#i a#i",
                List.of(
                        List.of("i love you", "island", "i love leetcode"), // 'i'
                        List.of("i love you", "i love leetcode"),           // ' '
                        List.of(),                                          // 'a' (dead branch)
                        List.of(),                                          // '#' -> saves "i a"(1)
                        List.of("i love you", "island", "i love leetcode"), // 'i'
                        List.of("i love you", "i love leetcode", "i a"),    // ' ' -> "i a" appears!
                        List.of("i a"),                                     // 'a'
                        List.of(),                                          // '#' -> bumps "i a"(2)
                        List.of("i love you", "island", "i a")              // 'i' -> "i a"(2) beats "i love leetcode"(2)!
                ));

        // 2. Edge: Dead prefix with multiple trailing characters before '#'
        verifyCase(
                "Edge: Multi-char dead prefix -> '#'",
                new String[]{"abc"},
                new int[]{3},
                "zx#z",
                List.of(
                        List.of(),      // 'z' (null)
                        List.of(),      // 'x' (still null, must keep buffering!)
                        List.of(),      // '#' -> inserts "zx"(1)
                        List.of("zx")   // 'z' -> now found!
                ));

        // 3. Edge: Space (' ' = 32) beats letter ('a' = 97) in ASCII order, plus prefix-of-prefix
        verifyCase(
                "Edge: ASCII space vs letter & prefix",
                new String[]{"a", "a b", "aa", "ab"},
                new int[]{2, 2, 2, 2},
                "a#",
                List.of(
                        List.of("a", "a b", "aa"), // "a" < "a b" (space 32) < "aa" (a 97) < "ab"
                        List.of()
                ));

        // 4. Edge: Repeatedly typing an existing top-3 sentence (must not duplicate in top3)
        verifyCase(
                "Edge: No duplicates on repeated hits",
                new String[]{"cat", "car"},
                new int[]{1, 1},
                "cat#cat#c",
                List.of(
                        List.of("car", "cat"), // 'c' (tie: "car" < "cat")
                        List.of("car", "cat"), // 'a'
                        List.of("cat"),        // 't'
                        List.of(),             // '#' -> "cat" now 2
                        List.of("cat", "car"), // 'c' -> "cat"(2) now beats "car"(1)
                        List.of("cat", "car"), // 'a'
                        List.of("cat"),        // 't'
                        List.of(),             // '#' -> "cat" now 3
                        List.of("cat", "car")  // 'c' -> strictly 2 items, no duplicate "cat"
                ));
    }
}

/**
 * =========================================================================================
 * 7. ## Close with a short summary
 *
 * - Core pattern: Trie + Cached Top-K List per Node (backed by a global frequency HashMap).
 * - Key observation: Because `K = 3` is tiny and frequencies only increase, precomputing `top3`
 *   on the `P` prefix nodes during `'#'` turns every keystroke read into a strict O(1) pointer hop.
 * - Worth memorizing vs. understanding: Understand *why* caching `top3` only works when frequencies
 *   are monotonic (no decreases/evictions); memorize buffering `c` BEFORE checking `currNode == null`,
 *   returning a defensive copy of `top3`, and mapping `' '` to index `26`.
 * - Most common trap: Returning early when `currNode == null` without appending `c` to `prefix`,
 *   which silently drops new sentences when `'#'` is pressed.
 * - One-line mental trigger: "Live prefix autocomplete with top-K hot items? Trie + cached top-K list at every node."
 * =========================================================================================
 */









































import java.util.*;

class AutocompleteSystem {

    private static class TrieNode {

        Map<Character, TrieNode> children = new HashMap<>();

        Set<String> sentences = new HashSet<>();
    }

    private record Candidate(String sentence, int frequency) {}

    private final TrieNode root = new TrieNode();

    private final Map<String, Integer> frequency = new HashMap<>();

    private final StringBuilder currentInput = new StringBuilder();

    private TrieNode currentNode = root;

    /*
     * Best-first comparator:
     * Higher frequency first, then lexicographically smaller.
     */
    private static final Comparator<Candidate> BEST_FIRST =
            Comparator.comparingInt(Candidate::frequency)
                    .reversed()
                    .thenComparing(Candidate::sentence);

    /*
     * Worst-first comparator:
     * Lower frequency first, then lexicographically larger.
     *
     * The heap root will be the worst among the selected
     * three candidates.
     */
    private static final Comparator<Candidate> WORST_FIRST =
            Comparator.comparingInt(Candidate::frequency)
                    .thenComparing(
                            Candidate::sentence,
                            Comparator.reverseOrder()
                    );

    public AutocompleteSystem(String[] sentences, int[] times) {

        for (int i = 0; i < sentences.length; i++) {

            frequency.merge(sentences[i], times[i], Integer::sum);
        }

        for (String sentence : frequency.keySet()) {

            insert(sentence);
        }
    }

    private void insert(String sentence) {

        TrieNode node = root;

        for (char c : sentence.toCharArray()) {

            node = node.children.computeIfAbsent(
                    c,
                    ignored -> new TrieNode()
            );

            node.sentences.add(sentence);
        }
    }

    public List<String> input(char c) {

        if (c == '#') {

            String sentence = currentInput.toString();

            frequency.merge(sentence, 1, Integer::sum);

            insert(sentence);

            currentInput.setLength(0);

            currentNode = root;

            return List.of();
        }

        currentInput.append(c);

        if (currentNode == null) {

            return List.of();
        }

        currentNode = currentNode.children.get(c);

        if (currentNode == null) {

            return List.of();
        }

        /*
         * Maintain only the best three candidates.
         *
         * The root contains the worst candidate among
         * the currently selected top three.
         */
        PriorityQueue<Candidate> heap =
                new PriorityQueue<>(WORST_FIRST);

        for (String sentence : currentNode.sentences) {

            Candidate candidate = new Candidate(
                    sentence,
                    frequency.get(sentence)
            );

            heap.offer(candidate);

            /*
             * If more than three candidates exist,
             * remove the worst one.
             */
            if (heap.size() > 3) {

                heap.poll();
            }
        }

        /*
         * Extract at most three candidates.
         */
        List<Candidate> result = new ArrayList<>(heap);

        /*
         * Heap iteration does not guarantee sorted order.
         *
         * Therefore, explicitly sort the final three.
         */
        result.sort(BEST_FIRST);

        return result.stream()
                .map(Candidate::sentence)
                .toList();
    }
}








































import java.util.*;

class AutocompleteSystem {

    /*
     * Immutable ranking information.
     *
     * Why use a record?
     *
     * 1. Concise representation of ranking data.
     * 2. Immutable fields.
     * 3. Automatically generated equals() and hashCode().
     *
     * We must not modify a ranking entry while it is inside
     * a TreeSet because that would violate its ordering.
     */
    private record Entry(String sentence, int frequency) {}

    private static class TrieNode {

        /*
         * Each child represents the next character.
         *
         * Supports lowercase English letters and spaces.
         */
        Map<Character, TrieNode> children = new HashMap<>();

        /*
         * Maintains ALL sentences matching this prefix,
         * ordered according to their ranking.
         *
         * The first element is always the best sentence.
         *
         * We maintain all matching sentences rather than
         * only three because a frequency update can change
         * which sentences belong to the top three.
         */
        TreeSet<Entry> rankedSentences = new TreeSet<>(

                Comparator.comparingInt(Entry::frequency)
                        .reversed()

                        /*
                         * When frequencies are equal,
                         * lexicographically smaller sentence wins.
                         */
                        .thenComparing(Entry::sentence)
        );
    }

    private final TrieNode root = new TrieNode();

    /*
     * Global source of truth for sentence frequencies.
     */
    private final Map<String, Integer> frequency = new HashMap<>();

    /*
     * Characters entered by the user so far.
     */
    private final StringBuilder currentInput = new StringBuilder();

    /*
     * Pointer to the Trie node representing the current prefix.
     */
    private TrieNode currentNode = root;

    /*
     * Constructor
     *
     * Time Complexity:
     * O(N * L * log N)
     *
     * Every sentence is inserted along its L-character path.
     * Each TreeSet insertion costs O(log N).
     */
    public AutocompleteSystem(String[] sentences, int[] times) {

        for (int i = 0; i < sentences.length; i++) {

            /*
             * Aggregate duplicate initial sentences.
             */
            frequency.merge(sentences[i], times[i], Integer::sum);
        }

        /*
         * Build the Trie and ranking sets.
         */
        for (var entry : frequency.entrySet()) {

            insert(entry.getKey(), entry.getValue());
        }
    }

    /*
     * Insert a new sentence into the Trie.
     *
     * This method assumes that the sentence is not already
     * present in the Trie.
     *
     * The sentence is added to the TreeSet at every node
     * along its prefix path.
     */
    private void insert(String sentence, int count) {

        TrieNode node = root;

        /*
         * Create the Trie path character by character.
         */
        for (char c : sentence.toCharArray()) {

            node = node.children.computeIfAbsent(
                    c,
                    ignored -> new TrieNode()
            );

            /*
             * Insert the sentence with its current frequency.
             *
             * TreeSet automatically maintains the ranking.
             */
            node.rankedSentences.add(
                    new Entry(sentence, count)
            );
        }
    }

    /*
     * Update the frequency of an existing sentence.
     *
     * This is the most important part of this design.
     *
     * Suppose:
     *
     * "i love you" has frequency 5.
     *
     * After another search:
     *
     * "i love you" has frequency 6.
     *
     * We must:
     *
     * 1. Remove Entry("i love you", 5) from every node
     *    along its Trie path.
     *
     * 2. Insert Entry("i love you", 6) into every node
     *    along the same path.
     *
     * Why remove before adding?
     *
     * TreeSet uses its comparator to maintain ordering.
     * We cannot simply mutate the frequency of an existing
     * element because the TreeSet would not automatically
     * reposition that element.
     */
    private void updateSentence(String sentence, int oldCount, int newCount) {

        TrieNode node = root;

        /*
         * Phase 1: Remove the old ranking entry.
         *
         * The path already exists because this is an
         * existing sentence.
         *
         * Time: O(L * log N)
         */
        if (oldCount > 0) {

            Entry oldEntry = new Entry(sentence, oldCount);

            for (char c : sentence.toCharArray()) {

                node = node.children.get(c);

                node.rankedSentences.remove(oldEntry);
            }
        }

        /*
         * Phase 2: Update the global frequency.
         */
        frequency.put(sentence, newCount);

        /*
         * Phase 3: Insert the new ranking entry.
         *
         * For a new sentence, missing Trie nodes are created.
         *
         * For an existing sentence, the existing path is reused.
         *
         * Time: O(L * log N)
         */
        insert(sentence, newCount);
    }

    public List<String> input(char c) {

        /*
         * Case 1: User completes the sentence.
         */
        if (c == '#') {

            String completedSentence = currentInput.toString();

            /*
             * Retrieve the previous frequency.
             *
             * If this is a new sentence, oldCount = 0.
             */
            int oldCount = frequency.getOrDefault(
                    completedSentence,
                    0
            );

            /*
             * Every completed search increases frequency by one.
             */
            updateSentence(
                    completedSentence,
                    oldCount,
                    oldCount + 1
            );

            /*
             * Reset the input state.
             */
            currentInput.setLength(0);

            currentNode = root;

            return List.of();
        }

        /*
         * Case 2: Normal character input.
         */

        /*
         * Always append the character.
         *
         * Even if the Trie path is missing, we must preserve
         * the complete input for insertion after '#'.
         */
        currentInput.append(c);

        /*
         * If an earlier prefix had no Trie path,
         * subsequent characters cannot produce a match.
         */
        if (currentNode == null) {

            return List.of();
        }

        /*
         * Traverse exactly one Trie edge.
         *
         * No need to restart from the root.
         *
         * Time: O(1) average.
         */
        currentNode = currentNode.children.get(c);

        /*
         * Prefix does not exist.
         */
        if (currentNode == null) {

            return List.of();
        }

        /*
         * Case 3: Return top three suggestions.
         *
         * TreeSet already maintains the correct order.
         *
         * Therefore, no sorting or heap construction
         * is necessary during a query.
         */
        List<String> result = new ArrayList<>(3);

        int count = 0;

        for (Entry entry : currentNode.rankedSentences) {

            if (count == 3) {
                break;
            }

            result.add(entry.sentence());

            count++;
        }

        /*
         * Return at most three matching sentences.
         */
        return result;
    }
}
