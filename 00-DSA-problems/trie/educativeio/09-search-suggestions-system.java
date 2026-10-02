/**
 * ============================================================================
 * CODING PROBLEM STUDY NOTE: SEARCH SUGGESTIONS SYSTEM (Autocomplete)
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ----------------------------------------------------------------------------
 * - "Is the product catalog static, or are new products added continuously?"
 *   (If static, a Sort + Binary Search approach is highly optimal. If dynamic, a Trie is mandatory).
 * - "If the prefix has no matches, should subsequent characters also instantly return empty?"
 *   (Confirms we can short-circuit the search and not waste time checking deeper branches).
 * - "What if there are less than 3 matching products?"
 *   (Confirms we just return however many are available, even 0).
 * - "Instead of lexicographical order, what if we wanted to sort by 'popularity' or 'frequency'?"
 *   (A classic system design follow-up. Changes how we store/sort elements at each node from a simple list to a PriorityQueue).
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ----------------------------------------------------------------------------
 * [Core Requirement & Constraint]
 * As a user types a word character by character, we must return the top 3 lexicographically 
 * smallest matching products. The binding constraint is doing this in real-time (O(1) or O(L) 
 * time per keystroke) without re-scanning or re-sorting the 1000-word product list every time.
 * 
 * APPROACH 1: Brute Force (Filter and Sort on every keystroke)
 * 1. What I'd naturally try: For each character typed, take the current prefix, loop through 
 *    the entire `products` array, collect all matches, sort them, and return the first 3.
 * 2. Why it works: It perfectly satisfies the matching and sorting requirements.
 * 3. Why it's too slow: Sorting strings is expensive. If the user types a 10-character word, 
 *    we perform a full array scan and a full string sort 10 separate times.
 * 4. What work is repeated: We re-evaluate words that were already disqualified by the 
 *    previous keystroke. If "apple" didn't match "m", it definitely won't match "mo".
 * 5. Time Complexity: O(M * N log N) — where M is the search word length, N is total products. 
 *    We sort N items M times.
 * 6. Space Complexity: O(N) — storing the filtered list on every keystroke.
 * 
 * APPROACH 2: Sort Once + Two Pointers (The Competitive Programming Trick)
 * 1. What I'd naturally try next: Sort the `products` array *once* at the very beginning. 
 *    Since it's sorted, all words sharing a prefix will be adjacent in a contiguous block!
 * 2. Why it works: We maintain a `left` and `right` pointer for the valid block. As the 
 *    user types the next character, we just move `left` forward and `right` backward until 
 *    they point to words that match the new, longer prefix. We then take up to 3 words starting from `left`.
 * 3. Why it's brilliant but limited: It is insanely fast and requires no extra data structures. 
 *    However, it degrades heavily if the system needs to support real-time insertions of new products.
 * 4. Time Complexity: 
 *    - Setup: O(N log N * L) to sort the array (L is max string length).
 *    - Search: O(M + N) — over all M keystrokes, the pointers only ever move inward a total of N times.
 * 5. Space Complexity: O(1) auxiliary space — just two integer pointers!
 * 
 * APPROACH 3: Trie with Cached Top 3 (The System Design Optimal)
 * 1. The defining observation: A Prefix Tree (Trie) natively models typing character by character. 
 *    If we sort the `products` array *before* inserting them into the Trie, the words will arrive 
 *    at each Trie node in perfect lexicographical order.
 * 2. Why it works: At every node, we keep a `List<String> top3`. When inserting a word, if a 
 *    node's list has fewer than 3 items, we add the word. When the user types, we just take 
 *    one step down the Trie and instantly return the cached list in O(1) time.
 * 3. Time Complexity:
 *    - Setup: O(N log N * L) to sort + O(N * L) to build the Trie.
 *    - Search: O(M) — To process the M-length search word, we take exactly M steps in the Trie. 
 *      Returning the cache takes O(1) per step!
 * 4. Space Complexity:
 *    - Space: O(N * L) — to store the Trie nodes and the cached strings.
 * 
 * [What I'd write in an interview]
 * For an algorithm-focused interview, the Two Pointers approach is stunningly elegant. But 
 * because the prompt specifically says "design a system", Approach 3 (Trie with caching) is 
 * the expected industry standard. It decouples the heavy write-time work from the read-time, 
 * making reads lightning fast (O(1) per keystroke), which is how real autocomplete systems work.
 * 
 * 
 * 3. EDGE CASES
 * ----------------------------------------------------------------------------
 * - The search path breaks entirely: Typing "z" when no products start with "z". 
 *   (Must ensure all subsequent keystrokes like "ze", "zeb" return empty lists instantly 
 *   without throwing NullPointerExceptions).
 * - Exact matches that are prefixes of longer words: "mouse" vs "mousepad".
 * - Fewer than 3 total products in the catalog.
 * 
 * 
 * 4. KEY INSIGHT, PITFALLS & DRY RUN
 * ----------------------------------------------------------------------------
 * [Key Insight]
 * "Write-time vs Read-time tradeoffs." Searching a Trie for all matches and sorting them 
 * during the `search()` call is too slow for reads. By pre-sorting the input and caching 
 * the answers *inside* the Trie nodes during `insert()`, we shift the O(N log N) cost to 
 * write-time, making read-time strictly O(1) per character.
 * 
 * [Pitfall]
 * Forgetting that `products` is NOT guaranteed to be sorted. If you build the Trie with an 
 * unsorted array and cap the node lists at 3, you will cache the first 3 products *encountered*, 
 * which won't be the lexicographically smallest ones! You MUST sort the array first.
 * 
 * [Dry Run: products=["mobile","mouse","moneypot","monitor","mousepad"], search="mouse"]
 * 1. Sort products: ["mobile", "moneypot", "monitor", "mouse", "mousepad"]
 * 2. Insert into Trie (limiting cache to 3 per node):
 *    - root -> 'm'. Cache: ["mobile", "moneypot", "monitor"]
 *    - 'm' -> 'o'. Cache: ["mobile", "moneypot", "monitor"]
 *    - 'o' -> 'u'. Cache: ["mouse", "mousepad"]
 *    - 'u' -> 's'. Cache: ["mouse", "mousepad"]
 *    - 's' -> 'e'. Cache: ["mouse", "mousepad"]
 * 3. Search "mouse":
 *    - 'm': return root.children['m'].cache -> ["mobile", "moneypot", "monitor"]
 *    - 'o': return 'o'.cache -> ["mobile", "moneypot", "monitor"]
 *    - 'u': return 'u'.cache -> ["mouse", "mousepad"]
 *    - 's': return 's'.cache -> ["mouse", "mousepad"]
 *    - 'e': return 'e'.cache -> ["mouse", "mousepad"]
 * 
 * 
 * 5. FOLLOW-UPS
 * ----------------------------------------------------------------------------
 * Q: How would you handle a system where products have a dynamic "popularity score"?
 * A: I would remove the initial `Arrays.sort()`. Instead, each Trie node would hold a 
 *    Min-Heap (PriorityQueue) of size 3, ordered by score. As products are inserted, 
 *    we push them to the heap and pop the lowest-scored item if size > 3. 
 * 
 * Q: What if the Trie grows too large for a single machine's memory?
 * A: We would shard the Trie. For example, all words starting with 'a' through 'm' go to 
 *    Server 1, and 'n' through 'z' to Server 2. A routing layer directs the user's 
 *    request based on the first character of their search prefix.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchSuggestionsStudyNote {

    // ============================================================================
    // TRIE NODE DESIGN
    // ============================================================================
    static class TrieNode {
        TrieNode[] children;
        
        // Caches the top 3 lexicographically smallest words that pass through this node.
        List<String> top3Suggestions;

        public TrieNode() {
            this.children = new TrieNode[26];
            this.top3Suggestions = new ArrayList<>();
        }
    }

    public List<List<String>> suggestedProducts(String[] products, String searchWord) {
        // 1. Sort the products first. 
        // This guarantees that as we insert them into the Trie, the first 3 strings 
        // to reach any node are inherently the 3 lexicographically smallest!
        Arrays.sort(products);
        
        TrieNode root = new TrieNode();
        
        // 2. Build the Trie
        for (String product : products) {
            insert(root, product);
        }

        // 3. Search character by character
        List<List<String>> result = new ArrayList<>();
        TrieNode curr = root;
        
        for (int i = 0; i < searchWord.length(); i++) {
            int ch = searchWord.charAt(i) - 'a';
            
            // If the path exists, step down and grab the cached list.
            if (curr != null && curr.children[ch] != null) {
                curr = curr.children[ch];
                result.add(curr.top3Suggestions);
            } 
            // If the path breaks, there are no matches for this prefix OR any future prefixes.
            // We set curr to null so future iterations instantly hit this block.
            else {
                curr = null;
                result.add(new ArrayList<>());
            }
        }

        return result;
    }

    /**
     * Inserts a word into the Trie and updates the top3 cache at every node along the path.
     */
    private void insert(TrieNode root, String word) {
        TrieNode curr = root;
        for (int i = 0; i < word.length(); i++) {
            int index = word.charAt(i) - 'a';
            
            if (curr.children[index] == null) {
                curr.children[index] = new TrieNode();
            }
            curr = curr.children[index];
            
            // Since the input array was sorted, we only need to keep the first 3 
            // strings that arrive at this node.
            if (curr.top3Suggestions.size() < 3) {
                curr.top3Suggestions.add(word);
            }
        }
    }

    // ============================================================================
    // TESTING & EDGE CASES
    // ============================================================================
    public static void main(String[] args) {
        SearchSuggestionsStudyNote system = new SearchSuggestionsStudyNote();

        System.out.println("--- Standard Case ---");
        String[] products1 = {"mobile", "mouse", "moneypot", "monitor", "mousepad"};
        String search1 = "mouse";
        List<List<String>> res1 = system.suggestedProducts(products1, search1);
        System.out.println("Search 'mouse':");
        for (int i = 0; i < res1.size(); i++) {
            System.out.println("  Typed '" + search1.substring(0, i+1) + "': " + res1.get(i));
        }
        // Expected:
        // 'm': [mobile, moneypot, monitor]
        // 'mo': [mobile, moneypot, monitor]
        // 'mou': [mouse, mousepad]
        // 'mous': [mouse, mousepad]
        // 'mouse': [mouse, mousepad]

        System.out.println("\n--- Edge Case: Broken Path (No Matches) ---");
        String[] products2 = {"havana"};
        String search2 = "tatiana";
        List<List<String>> res2 = system.suggestedProducts(products2, search2);
        System.out.println("Search 'tatiana':");
        for (int i = 0; i < res2.size(); i++) {
            System.out.println("  Typed '" + search2.substring(0, i+1) + "': " + res2.get(i));
        }
        // Expected: All empty lists [].

        System.out.println("\n--- Edge Case: Less Than 3 Total Products ---");
        String[] products3 = {"bag", "baggage"};
        String search3 = "bags";
        List<List<String>> res3 = system.suggestedProducts(products3, search3);
        System.out.println("Search 'bags':");
        for (int i = 0; i < res3.size(); i++) {
            System.out.println("  Typed '" + search3.substring(0, i+1) + "': " + res3.get(i));
        }
        // Expected:
        // 'b', 'ba', 'bag' -> [bag, baggage]
        // 'bags' -> []
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern:    Sort + Trie with Node Caching.
 * Key observation: Pre-sorting the array allows us to blindly append to a node's 
 *                  cache during insertion, guaranteeing lexicographical order. 
 *                  This offloads O(N log N) work to write-time.
 * To memorize:     `if (curr.cache.size() < 3) curr.cache.add(word);` inside the 
 *                  Trie insertion loop.
 * Common trap:     Forgetting to handle the scenario where the search falls off 
 *                  the Trie (returning `null`). You must push an empty list for 
 *                  that character *and* all remaining characters in the search word.
 * Mental trigger:  "Autocomplete / Suggestions as you type" -> Trie with cached Top K.
 * ============================================================================
 */


import java.util.*;

public class SearchSuggestions_BinarySearch {

    public static List<List<String>> suggestedProducts(String[] products, String searchWord) {

        Arrays.sort(products); // Step 1: sort lexicographically

        List<List<String>> result = new ArrayList<>();
        String prefix = "";

        for (char ch : searchWord.toCharArray()) {
            prefix += ch;

            int start = lowerBound(products, prefix); // find first match

            List<String> suggestions = new ArrayList<>();

            // Collect at most 3 matching products
            for (int i = start; i < products.length && suggestions.size() < 3; i++) {

                // Check prefix match
                if (products[i].startsWith(prefix)) {
                    suggestions.add(products[i]);
                } else {
                    break; // no further matches possible
                }
            }

            result.add(suggestions);
        }

        return result;
    }

    /**
     * Classic lower bound (first index >= target)
     * VERY IMPORTANT pattern for interviews
     */
    private static int lowerBound(String[] products, String target) {

        int low = 0, high = products.length - 1;
        int answer = products.length; // default (not found)

        while (low <= high) {
            int mid = low + (high - low) / 2;

            if (products[mid].compareTo(target) >= 0) {
                answer = mid;     // potential answer
                high = mid - 1;   // move left
            } else {
                low = mid + 1;
            }
        }

        return answer;
    }
}

import java.util.*;

public class SearchSuggestions_Trie {

    static class TrieNode {
        TrieNode[] children = new TrieNode[26];
        List<String> suggestions = new ArrayList<>(); // store top 3
    }

    static class Trie {
        TrieNode root = new TrieNode();

        void insert(String word) {
            TrieNode node = root;

            for (char c : word.toCharArray()) {
                int idx = c - 'a';

                if (node.children[idx] == null) {
                    node.children[idx] = new TrieNode();
                }

                node = node.children[idx];

                // Only store top 3 suggestions
                if (node.suggestions.size() < 3) {
                    node.suggestions.add(word);
                }
            }
        }

        List<List<String>> search(String searchWord) {
            List<List<String>> result = new ArrayList<>();
            TrieNode node = root;

            for (char c : searchWord.toCharArray()) {

                if (node != null) {
                    node = node.children[c - 'a'];
                }

                if (node == null) {
                    result.add(Collections.emptyList());
                } else {
                    result.add(node.suggestions);
                }
            }

            return result;
        }
    }

    public static List<List<String>> suggestedProducts(String[] products, String searchWord) {

        Arrays.sort(products); // important

        Trie trie = new Trie();

        for (String product : products) {
            trie.insert(product);
        }

        return trie.search(searchWord);
    }
}

import java.util.*;

public class SearchSuggestions_Trie_PQ {

    static class TrieNode {
        TrieNode[] children = new TrieNode[26];

        // Max heap: remove largest lexicographically
        PriorityQueue<String> pq = new PriorityQueue<>(
                (a, b) -> b.compareTo(a) // reverse order → max heap
        );
    }

    static class Trie {
        TrieNode root = new TrieNode();

        void insert(String word) {
            TrieNode node = root;

            for (char c : word.toCharArray()) {
                int idx = c - 'a';

                if (node.children[idx] == null) {
                    node.children[idx] = new TrieNode();
                }

                node = node.children[idx];

                // Add word to heap
                node.pq.offer(word);

                // Keep only top 3 smallest
                if (node.pq.size() > 3) {
                    node.pq.poll(); // remove largest
                }
            }
        }

        List<List<String>> search(String searchWord) {
            List<List<String>> result = new ArrayList<>();
            TrieNode node = root;

            for (char c : searchWord.toCharArray()) {

                if (node != null) {
                    node = node.children[c - 'a'];
                }

                if (node == null) {
                    result.add(Collections.emptyList());
                } else {

                    // Convert heap → sorted list
                    List<String> list = new ArrayList<>(node.pq);
                    Collections.sort(list); // needed because heap is unordered

                    result.add(list);
                }
            }

            return result;
        }
    }

    public static List<List<String>> suggestedProducts(String[] products, String searchWord) {

        Trie trie = new Trie();

        for (String product : products) {
            trie.insert(product);
        }

        return trie.search(searchWord);
    }
}
