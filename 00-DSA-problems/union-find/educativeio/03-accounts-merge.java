/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * Given a 2D array `accounts` where `accounts[i][0]` is a person's name and the 
 * rest are their emails, determine if accounts belong to the same person by 
 * checking if they share at least one email. 
 * Merge these accounts, returning the name followed by their unique, ASCII-sorted emails.
 * Note: Two different people can have the same name.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * 1. Does the same name without shared emails imply the same person?
 *    (No. "John" with email A and "John" with email B remain separate. Name is just a label, emails define identity).
 * 2. Can the input contain duplicate emails within the *same* account array?
 *    (Yes. We must deduplicate emails when building the final merged output).
 * 3. Are email addresses case-sensitive in this context?
 *    (Assuming standard ASCII sort as requested, "A" and "a" would sort differently and be treated as distinct if they appeared, though valid emails are usually lowercased).
 * 4. Can an account have 0 emails (just a name)?
 *    (The constraint says length >= 2, so every account has at least 1 email. Prevents empty-set edge cases).
 * 5. Should the output rows be sorted in any particular order?
 *    (The prompt only asks for the *emails* within a row to be sorted, not the rows themselves).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * The Binding Constraint: Transitive relationships. If Account 0 shares an email with Account 1, 
 * and Account 1 shares an email with Account 2, Account 0 and 2 belong to the same person, 
 * even if they share zero emails directly. We must resolve chains of equivalence.
 * 
 * --- Approach 1: Brute Force (Iterative Set Merging) ---
 * Idea: Convert each account's emails to a Set. Compare every set with every other set. 
 * If Set A and Set B intersect, merge B into A, delete B, and restart the whole comparison 
 * process from the beginning.
 * What I'd naturally try: Double for-loop looking for overlaps, with a boolean `mergedThisPass`.
 * Why it's too slow/costly: If we have N accounts, and they form a single chain 
 * (0 overlaps with 1, 1 overlaps with 2...), we might restart N times. 
 * String hashing and set intersection in a nested loop is brutally slow.
 * What work is repeated: We repeatedly compare sets that we already know don't intersect.
 * Property that fixes this: We can model this as a graph. We just need to find 
 * the "Connected Components", which doesn't require N^3 pairwise comparisons.
 * 
 * Time Complexity: O(N^3 * K) — where N is the number of accounts and K is max emails per account.
 * Space Complexity: O(N * K) — to store the sets of emails.
 * 
 * --- Approach 2: Breadth-First Search (BFS) / Depth-First Search (DFS) ---
 * Idea: Treat each email as a node. Edges exist between the first email of an account 
 * and all other emails in that same account. Build an adjacency list `Map<String, List<String>>`. 
 * Iterate over all emails. If an email is unvisited, launch a DFS/BFS to collect all 
 * connected emails. Sort them, attach the name, and save.
 * Why it works: DFS intrinsically follows the transitive chains to completion.
 * Why it's slightly heavy: Building an explicit adjacency list of Strings requires 
 * significant memory overhead (String objects, ArrayLists inside HashMaps).
 * 
 * Time Complexity: O(N*K \log(N*K)) — building graph is O(N*K), but sorting the components at the end dominates.
 * Space Complexity: O(N*K) — for the adjacency map and visited set.
 * 
 * --- Approach 3: Union-Find (Disjoint Set) over Account Indices ---
 * Idea: Instead of connecting *emails to emails*, connect *Account Indices to Account Indices*. 
 * Give each account an ID (0 to N-1). Map each email to the ID of the first account it appears in. 
 * If we see that email again in a later account, we `union` the current account ID with 
 * the stored account ID. 
 * Finally, group all emails by the "root" parent of their account ID.
 * Why this is optimal: We avoid building an O(E) adjacency list. We just need an array 
 * of size N for the Disjoint Set, and a single Map to remember the first account for each email.
 * 
 * Time Complexity: O(N*K \log(N*K)) — Union-Find operations take amortized O(1). Iterating all emails takes O(N*K). Grouping and sorting the emails via TreeSet/Collections.sort takes O(N*K \log(N*K)) in the worst case where all emails belong to one person.
 * Space Complexity: O(N*K) — to store the `email -> account ID` map and the final output groups. The Union-Find array is only O(N).
 * 
 * Interview Decision: I would write **Union-Find**. It is the absolute gold standard for 
 * "equivalence class" / "dynamic connectivity" problems. It's less prone to stack overflows 
 * than DFS and avoids the boilerplate of building an explicit graph.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * 1. Same Name, Different People: [["John", "a@x.com"], ["John", "b@x.com"]]. Must remain separate.
 * 2. Duplicate emails in the same input account: [["John", "a@x.com", "a@x.com"]]. Output must have "a@x.com" once.
 * 3. A single massive chain where all 100 accounts merge into 1. (UF path compression handles this gracefully).
 * 
 * ============================================================================
 * 4. ALSO INCLUDE
 * ============================================================================
 * --- Key Insight ---
 * Ask yourself: "Am I merging strings, or am I merging the *identities* that hold them?"
 * Correctness intuition: Emails are just properties. The *accounts* (rows) are the nodes. 
 * If row 0 and row 2 share an email, row 0 and row 2 are the same identity. We only need 
 * to union the row indices!
 * 
 * --- Dry Run (Union-Find) ---
 * Input: 
 * 0: ["John", "john@1", "john@2"]
 * 1: ["Mary", "mary@1"]
 * 2: ["John", "john@2", "john@3"]
 * 
 * Init UF: parent = [0, 1, 2]
 * Map emailToId = {}
 * 
 * Process 0:
 * - "john@1" not in map. emailToId.put("john@1", 0)
 * - "john@2" not in map. emailToId.put("john@2", 0)
 * 
 * Process 1:
 * - "mary@1" not in map. emailToId.put("mary@1", 1)
 * 
 * Process 2:
 * - "john@2" IS in map (maps to 0). union(2, 0) -> parent[2] = 0.
 * - "john@3" not in map. emailToId.put("john@3", 2)
 * 
 * Grouping:
 * - Row 0 (root 0): adds "john@1", "john@2"
 * - Row 1 (root 1): adds "mary@1"
 * - Row 2 (root 0): adds "john@2", "john@3"
 * Result Roots: { 0: ["john@1", "john@2", "john@3"], 1: ["mary@1"] }
 * 
 * --- Pitfalls ---
 * 1. Trying to make a Union-Find out of `String` emails directly. You'd need a `Map<String, String>` 
 *    for parents, which adds massive overhead. Mapping to integer account IDs is vastly cleaner.
 * 2. Forgetting to deduplicate emails. (Using a `TreeSet` during aggregation solves both deduplication AND sorting!).
 * 
 * --- Pattern Recognition ---
 * When you see: "Transitive merging", "Groups that share an item", "Equivalence classes".
 * Think: UNION-FIND. 
 * Transfers to: Similar String Groups, Graph Valid Tree, Longest Consecutive Sequence.
 * 
 * --- Interview Script ---
 * "Since an email acting as a bridge merges two accounts, this is a connected components problem. 
 * Rather than building a full graph, I'll use a Disjoint Set (Union-Find) to group the account indices. 
 * I'll iterate through the accounts, mapping each email to its account index. If I see an email 
 * I've mapped before, I union the current account index with the stored one. Finally, I'll aggregate 
 * the emails using the root account index. Using a TreeSet for aggregation will automatically 
 * deduplicate and sort the emails in ASCII order. This gives O(N*K log(N*K)) time."
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q1: What if the accounts list is streaming (added one by one) and we query "Are A and B the same person?"
 * A: Union-Find is strictly superior here. As each account streams in, we just map and union. 
 *    Querying is O(1) amortized via `find(A) == find(B)`. DFS would require a full traversal every time.
 * Q2: How would you handle merging if there were millions of emails, exceeding single-machine memory?
 * A: We'd use a distributed graph processing framework (like Apache Spark / GraphX). We map 
 *    emails to node IDs, output (email_id, account_id) pairs, and run a distributed connected 
 *    components algorithm (like MapReduce connected components).
 * 
 * ============================================================================
 * 7. SHORT SUMMARY
 * ============================================================================
 * Core pattern: Disjoint Set / Union-Find over array indices, not the string values.
 * Key observation: The index of the account uniquely identifies the "node".
 * Memorize: The map-to-index Union-Find pattern. Map<Item, Index> + UnionFind(N).
 * Most common trap: Manual sorting and manual deduplication. Just use TreeSet.
 * Mental trigger: "Merge groups sharing at least one X" -> Union-Find on group indices.
 */

import java.util.*;

public class AccountsMerge {

    public List<List<String>> accountsMerge(List<List<String>> accounts) {
        int n = accounts.size();
        
        // 1. Initialize Union-Find for 'n' accounts
        UnionFind uf = new UnionFind(n);
        
        // Maps an email string to the index of the FIRST account that claimed it.
        // E.g., emailToAccountId: {"john@mail.com" -> 0}
        Map<String, Integer> emailToAccountId = new HashMap<>();
        
        // 2. Build the Disjoint Set
        for (int i = 0; i < n; i++) {
            List<String> account = accounts.get(i);
            
            // Start from index 1 because index 0 is the name
            for (int j = 1; j < account.size(); j++) {
                String email = account.get(j);
                
                // If we've seen this email before, this current account (i) 
                // belongs to the same person as the account that first saw this email.
                if (emailToAccountId.containsKey(email)) {
                    uf.union(i, emailToAccountId.get(email));
                } else {
                    // Otherwise, record that account 'i' is the first to claim this email.
                    emailToAccountId.put(email, i);
                }
            }
        }
        
        // 3. Group emails by their root account ID.
        // We use a TreeSet so emails are automatically deduplicated AND ASCII-sorted.
        // E.g., rootEmailGroups: {0 -> ["john@1", "john@2", "john@3"]}
        Map<Integer, TreeSet<String>> rootEmailGroups = new HashMap<>();
        
        for (int i = 0; i < n; i++) {
            // Find the absolute root identity for account i
            int root = uf.find(i);
            
            // If the root doesn't have a set yet, create one
            rootEmailGroups.putIfAbsent(root, new TreeSet<>());
            
            // Add all emails from account i to its root's set
            List<String> account = accounts.get(i);
            for (int j = 1; j < account.size(); j++) {
                rootEmailGroups.get(root).add(account.get(j));
            }
        }
        
        // 4. Format the final output
        List<List<String>> mergedAccounts = new ArrayList<>();
        
        // Iterate only over the distinct roots (the unique people)
        for (Map.Entry<Integer, TreeSet<String>> entry : rootEmailGroups.entrySet()) {
            int rootId = entry.getKey();
            TreeSet<String> sortedEmails = entry.getValue();
            
            // The name is strictly preserved in accounts[rootId][0]
            String name = accounts.get(rootId).get(0);
            
            // Construct the row: [Name, email1, email2...]
            List<String> mergedAccount = new ArrayList<>();
            mergedAccount.add(name);
            mergedAccount.addAll(sortedEmails); // TreeSet maintains ASCII sorted order
            
            mergedAccounts.add(mergedAccount);
        }
        
        return mergedAccounts;
    }
    
    /**
     * Standard Disjoint Set (Union-Find) helper class.
     * Manages connectivity between integer components (0 to n-1).
     */
    private class UnionFind {
        private int[] parent;
        
        public UnionFind(int n) {
            parent = new int[n];
            // Initially, each account index is its own root
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }
        
        // Find with path compression (keeps the tree completely flat for near O(1) lookups)
        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            // Point this node directly to the ultimate root
            return parent[i] = find(parent[i]); 
        }
        
        // Unites two sets by pointing one's root to the other's root
        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI != rootJ) {
                parent[rootI] = rootJ;
            }
        }
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING / CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        AccountsMerge solution = new AccountsMerge();

        // Test Case 1: Standard transitive merge
        List<List<String>> accounts1 = Arrays.asList(
            Arrays.asList("John", "johnsmith@mail.com", "john_newyork@mail.com"),
            Arrays.asList("John", "johnsmith@mail.com", "john00@mail.com"),
            Arrays.asList("Mary", "mary@mail.com"),
            Arrays.asList("John", "johnnybravo@mail.com")
        );
        // Expected: 
        // ["John", "john00@mail.com", "john_newyork@mail.com", "johnsmith@mail.com"]
        // ["Mary", "mary@mail.com"]
        // ["John", "johnnybravo@mail.com"]
        System.out.println("Test 1:");
        for (List<String> acc : solution.accountsMerge(accounts1)) {
            System.out.println(acc);
        }
        System.out.println();

        // Test Case 2: Same name, no overlap (Should remain separate)
        List<List<String>> accounts2 = Arrays.asList(
            Arrays.asList("Gabe", "Gabe0@m.co", "Gabe3@m.co", "Gabe1@m.co"),
            Arrays.asList("Kevin", "Kevin3@m.co", "Kevin5@m.co", "Kevin0@m.co"),
            Arrays.asList("Ethan", "Ethan5@m.co", "Ethan4@m.co", "Ethan0@m.co"),
            Arrays.asList("Hanzo", "Hanzo3@m.co", "Hanzo1@m.co", "Hanzo0@m.co"),
            Arrays.asList("Fern", "Fern5@m.co", "Fern1@m.co", "Fern0@m.co")
        );
        // Expected: Original lists but emails sorted alphabetically.
        System.out.println("Test 2 (No merges, just sorted):");
        for (List<String> acc : solution.accountsMerge(accounts2)) {
            System.out.println(acc);
        }
        System.out.println();
        
        // Test Case 3: Duplicate emails in same account and across accounts
        List<List<String>> accounts3 = Arrays.asList(
            Arrays.asList("Alex", "Alex@1.com", "Alex@1.com"),
            Arrays.asList("Alex", "Alex@2.com", "Alex@1.com")
        );
        // Expected: ["Alex", "Alex@1.com", "Alex@2.com"] (Duplicates stripped!)
        System.out.println("Test 3 (Duplicates):");
        for (List<String> acc : solution.accountsMerge(accounts3)) {
            System.out.println(acc);
        }
    }
}



import java.util.*;

/**
 * ================================================================
 * PROBLEM: ACCOUNTS MERGE
 * ================================================================
 *
 * Given a list of accounts:
 *
 * accounts[i][0] = person's name
 * accounts[i][1...n] = email addresses associated with that person
 *
 * Two accounts belong to the same person if they share at least
 * one common email address.
 *
 * Important:
 * - Two people can have the same name.
 * - A person can have multiple accounts.
 * - Accounts can be connected indirectly through shared emails.
 * - Each email must appear only once in the output.
 * - Emails within each merged account must be sorted.
 *
 *
 * ================================================================
 * APPROACH: UNION-FIND ON EMAIL ADDRESSES
 * ================================================================
 *
 * Instead of treating account indices as DSU nodes, we treat
 * EMAIL ADDRESSES THEMSELVES as the nodes.
 *
 * Example:
 *
 * Account 1: John, a@mail.com, b@mail.com
 * Account 2: John, b@mail.com, c@mail.com
 * Account 3: Mary, d@mail.com
 *
 * Connect the emails within each account:
 *
 * a@mail.com ---- b@mail.com
 *                 |
 *                 c@mail.com
 *
 * d@mail.com
 *
 * The first three emails belong to one connected component.
 * The fourth email belongs to a separate component.
 *
 * The final output is:
 *
 * [
 *   [John, a@mail.com, b@mail.com, c@mail.com],
 *   [Mary, d@mail.com]
 * ]
 *
 *
 * ================================================================
 * KEY OBSERVATION
 * ================================================================
 *
 * We do not need to connect every email to every other email
 * within an account.
 *
 * Connecting every email to the FIRST email is sufficient.
 *
 * For example:
 *
 * Account: John, a@mail.com, b@mail.com, c@mail.com
 *
 * a@mail.com ---- b@mail.com
 *       |
 *       +-------- c@mail.com
 *
 * All three emails are now in the same connected component.
 *
 *
 * ================================================================
 * STEPS
 * ================================================================
 *
 * 1. For every account:
 *    - Store each email's associated name.
 *    - Union every email with the account's first email.
 *
 * 2. After processing all accounts:
 *    - Find the root of every unique email.
 *    - Group emails having the same root.
 *
 * 3. For every group:
 *    - Sort its email addresses.
 *    - Get the person's name.
 *    - Create the final merged account.
 *
 *
 * ================================================================
 * COMPLEXITY
 * ================================================================
 *
 * Let:
 *
 * M = total number of email occurrences across all accounts.
 * U = number of unique email addresses.
 *
 * Union-Find operations:
 * O(M * alpha(U)) amortized
 *
 * Grouping emails:
 * O(U * alpha(U))
 *
 * Sorting all email groups:
 * O(U * log U) worst case
 *
 * Total time:
 * O(M * alpha(U) + U * log U)
 *
 * Auxiliary space:
 * O(U)
 *
 * alpha(U) is the inverse Ackermann function. With path compression
 * and union by rank, it is effectively constant for practical inputs.
 *
 * The complexity assumes expected constant-time HashMap operations
 * and treats string hashing/comparisons as constant time. If email
 * lengths are considered, those operations also depend on string length.
 */
public class AccountsMerge {

    /**
     * Merges accounts that belong to the same person.
     *
     * @param accounts input accounts, with the name at index 0
     *                 and emails at indices 1 onwards
     * @return merged accounts with unique emails sorted
     */
    public List<List<String>> accountsMerge(
            List<List<String>> accounts) {

        // DSU manages connected components of EMAIL ADDRESSES.
        UnionFind uf = new UnionFind();

        // Maps each unique email to its associated person's name.
        //
        // Example:
        // "john@mail.com" -> "John"
        //
        // We use putIfAbsent() to preserve the first name recorded
        // for an email. The problem guarantees that an email cannot
        // legitimately connect accounts belonging to different people.
        Map<String, String> emailToName = new HashMap<>();


        // =========================================================
        // STEP 1: PROCESS ACCOUNTS AND UNION CONNECTED EMAILS
        // =========================================================

        for (List<String> account : accounts) {

            // The first element is the person's name.
            String name = account.get(0);

            // Use the first email as the connection point for this
            // entire account.
            //
            // Every other email in this account will be unioned
            // with firstEmail.
            String firstEmail = account.get(1);


            // Process every email, including the first email.
            //
            // This loop records all emails in emailToName.
            // It is important not to skip the first email here,
            // even though we will skip its self-union below.
            for (int i = 1; i < account.size(); i++) {

                String email = account.get(i);

                // Associate this email with its person's name.
                //
                // If the email was already recorded, keep the
                // existing name rather than overwriting it.
                emailToName.putIfAbsent(email, name);
            }


            // Connect the remaining emails to the first email.
            //
            // Start at index 2 because:
            // - Index 0 is the name.
            // - Index 1 is firstEmail itself.
            //
            // Starting at index 2 avoids the unnecessary operation:
            // union(firstEmail, firstEmail).
            for (int i = 2; i < account.size(); i++) {

                String email = account.get(i);

                // Both emails belong to the same account.
                // Therefore, they must belong to the same component.
                uf.union(firstEmail, email);
            }
        }


        // =========================================================
        // STEP 2: GROUP UNIQUE EMAILS BY THEIR FINAL ROOT
        // =========================================================

        // Key   = representative/root email of a component
        // Value = all unique emails belonging to that component
        //
        // Example:
        //
        // "a@mail.com" -> ["a@mail.com", "b@mail.com", "c@mail.com"]
        // "d@mail.com" -> ["d@mail.com"]
        //
        // HashMap is sufficient because the ordering of the final
        // account rows is not specified by the problem.
        Map<String, List<String>> groups = new HashMap<>();


        // emailToName.keySet() contains each email only once because
        // emailToName is a HashMap.
        for (String email : emailToName.keySet()) {

            // Find the final representative of this email.
            //
            // IMPORTANT:
            // A root can change as more union operations happen.
            // Therefore, grouping is performed only AFTER all
            // accounts have been processed.
            String root = uf.find(email);

            // Create a group for this root if one does not exist.
            // Then add the current email to that group.
            //
            // computeIfAbsent() creates the ArrayList only when
            // this root is encountered for the first time.
            groups.computeIfAbsent(
                    root,
                    ignored -> new ArrayList<>()
            ).add(email);
        }


        // =========================================================
        // STEP 3: SORT EMAILS AND BUILD THE FINAL RESULT
        // =========================================================

        List<List<String>> result = new ArrayList<>();

        // Each entry represents one connected component/person.
        for (List<String> emails : groups.values()) {

            // Sort the emails lexicographically.
            //
            // Java String natural ordering compares characters
            // lexicographically. For ASCII email characters, this
            // satisfies the problem's ASCII-ordering requirement.
            Collections.sort(emails);

            // Every email in this component belongs to the same
            // person, so we can obtain the name from any email.
            //
            // emails.get(0) is simply the first email after sorting.
            String name = emailToName.get(emails.get(0));

            // The output format requires the name at index 0,
            // followed by the sorted email addresses.
            List<String> mergedAccount = new ArrayList<>();

            mergedAccount.add(name);
            mergedAccount.addAll(emails);

            // Add this person's completed account to the output.
            result.add(mergedAccount);
        }

        return result;
    }


    /**
     * ================================================================
     * UNION-FIND / DISJOINT SET UNION (DSU)
     * ================================================================
     *
     * Each unique email is a node.
     *
     * parent:
     *   Maps an email to its parent email.
     *
     * rank:
     *   Stores an upper bound on the height of the tree representing
     *   a component. Rank is not the number of emails in a component.
     *
     * Two optimizations are used:
     *
     * 1. PATH COMPRESSION
     *    During find(), make each visited node point directly to
     *    the root. This speeds up future lookups.
     *
     * 2. UNION BY RANK
     *    When merging two components, attach the lower-rank tree
     *    below the higher-rank tree.
     *
     * Together, these optimizations make DSU operations nearly O(1)
     * amortized.
     */
    static class UnionFind {

        // parent[email] = parent of this email in the DSU tree.
        //
        // For a root email:
        // parent[email] = email
        private final Map<String, String> parent = new HashMap<>();

        // rank[email] = rank of this email.
        //
        // Rank is meaningful for roots when deciding which tree
        // should become the parent during a union.
        private final Map<String, Integer> rank = new HashMap<>();


        /**
         * Finds the root/representative of the component containing x.
         *
         * Lazy initialization:
         * If x has never appeared before, create a new component
         * containing only x.
         */
        public String find(String x) {

            // If x is new, initialize it as its own root.
            //
            // Initially:
            // parent[x] = x
            // rank[x] = 0
            //
            // This avoids needing to know every email before
            // starting the Union-Find operations.
            if (!parent.containsKey(x)) {
                parent.put(x, x);
                rank.put(x, 0);
            }


            // If x is not its own parent, it is not the root.
            if (!parent.get(x).equals(x)) {

                // Recursively find the root of x's parent.
                //
                // Path compression:
                // After finding the root, directly connect x to it.
                //
                // Before:
                // x -> a@mail.com -> b@mail.com -> root
                //
                // After:
                // x ------------------------------> root
                //
                // Future find() calls on x become faster.
                parent.put(x, find(parent.get(x)));
            }


            // After path compression, return the root of x.
            return parent.get(x);
        }


        /**
         * Merges the components containing x and y.
         *
         * If they already belong to the same component, no work
         * is required.
         */
        public void union(String x, String y) {

            // Find the current root of each email.
            String rootX = find(x);
            String rootY = find(y);


            // If both roots are identical, x and y are already
            // connected, either directly or through other emails.
            if (rootX.equals(rootY)) {
                return;
            }


            // =====================================================
            // UNION BY RANK
            // =====================================================
            //
            // Attach the lower-rank tree below the higher-rank tree.
            //
            // This prevents the DSU tree from becoming unnecessarily
            // tall, keeping find() operations efficient.


            if (rank.get(rootX) < rank.get(rootY)) {

                // rootY's tree is taller.
                // Attach rootX's tree below rootY.
                parent.put(rootX, rootY);

            } else if (rank.get(rootX) > rank.get(rootY)) {

                // rootX's tree is taller.
                // Attach rootY's tree below rootX.
                parent.put(rootY, rootX);

            } else {

                // Both trees have equal rank.
                //
                // Choose rootX as the new root and attach rootY below it.
                parent.put(rootY, rootX);

                // The resulting tree may be one level taller,
                // so increase the rank of the new root.
                rank.put(rootX, rank.get(rootX) + 1);
            }
        }
    }


    /**
     * Example execution.
     *
     * You can run this class directly.
     */
    public static void main(String[] args) {

        AccountsMerge solution = new AccountsMerge();

        List<List<String>> accounts = List.of(

                List.of(
                        "John",
                        "johnsmith@mail.com",
                        "john_newyork@mail.com"
                ),

                List.of(
                        "John",
                        "johnsmith@mail.com",
                        "john00@mail.com"
                ),

                List.of(
                        "Mary",
                        "mary@mail.com"
                ),

                List.of(
                        "John",
                        "johnnybravo@mail.com"
                )
        );

        List<List<String>> result = solution.accountsMerge(accounts);

        result.forEach(System.out::println);
    }
}






import java.util.*;

public class AccountsMerge {

    /**
     * ============================================================
     * PROBLEM: ACCOUNTS MERGE
     * ============================================================
     *
     * Each account contains:
     *   accounts[i][0]     -> Person's name
     *   accounts[i][1..n]  -> Email addresses
     *
     * Two accounts belong to the same person if they share at least
     * one common email address.
     *
     * IMPORTANT OBSERVATIONS:
     *
     * 1. Names are NOT unique identifiers.
     *    Two different people can both be named "John".
     *
     * 2. Emails establish connections between accounts.
     *    If two accounts share an email, they belong to the same person.
     *
     * 3. Connections are TRANSITIVE.
     *    If Account A shares an email with B, and B shares an email
     *    with C, then A, B and C must all be merged.
     *
     * 4. The output must contain:
     *    - One row per distinct person.
     *    - The person's name at index 0.
     *    - Unique email addresses sorted lexicographically.
     *
     *
     * ============================================================
     * APPROACH: UNION-FIND ON ACCOUNT INDICES
     * ============================================================
     *
     * Instead of treating email addresses as DSU nodes, we treat
     * ACCOUNT INDICES as DSU nodes.
     *
     * Example:
     *
     * Account 0: John, a@mail.com, b@mail.com
     * Account 1: John, b@mail.com, c@mail.com
     * Account 2: Mary, d@mail.com
     *
     * Since Accounts 0 and 1 share b@mail.com, we union them.
     *
     * Account 0 -------- Account 1       Account 2
     *    John               John            Mary
     *
     * The final components are:
     *
     * Component 1: Accounts 0 and 1
     * Component 2: Account 2
     *
     * We then collect unique emails belonging to each component.
     *
     *
     * ============================================================
     * ALGORITHM
     * ============================================================
     *
     * STEP 1: CONNECT ACCOUNTS
     *
     * Maintain:
     *   emailToAccount: email -> first account containing that email
     *
     * When an email appears for the first time:
     *   Remember its account index.
     *
     * When the email appears again:
     *   Union the current account with the previously recorded account.
     *
     * STEP 2: GROUP EMAILS
     *
     * After processing every account, find the final DSU root for
     * every unique email and group emails by that root.
     *
     * STEP 3: BUILD THE RESULT
     *
     * Sort each group's emails, prepend the name associated with
     * its root account, and add the merged account to the result.
     *
     *
     * ============================================================
     * COMPLEXITY
     * ============================================================
     *
     * Let:
     *   A = Number of accounts
     *   M = Total number of email occurrences
     *   U = Number of unique email addresses
     *
     * Time:
     *   DSU processing: O(M * alpha(A)) amortized
     *   Grouping:       O(U * alpha(A))
     *   Sorting:        O(U * log U) worst case
     *
     * Overall:
     *   O(A + M * alpha(A) + U * log U)
     *
     * Auxiliary space:
     *   O(A + U)
     *
     * alpha(A) is the inverse Ackermann function, which is effectively
     * constant for practical input sizes.
     *
     * These bounds assume expected constant-time hash operations and
     * treat string hashing/comparisons as constant time. Accounting for
     * email length adds the corresponding string-processing costs.
     */
    public List<List<String>> accountsMerge(List<List<String>> accounts) {

        // Number of accounts.
        // Every account index from 0 to n - 1 will be a DSU node.
        int n = accounts.size();

        // Initially, every account belongs to its own component.
        //
        // Example:
        // parent = [0, 1, 2, 3]
        //
        // No accounts have been merged yet.
        DSU dsu = new DSU(n);

        // Maps each unique email to the FIRST account in which it appeared.
        //
        // Example:
        // "john@mail.com" -> 0
        // "mary@mail.com" -> 2
        //
        // Why do we need this map?
        // It lets us detect when an email appears in multiple accounts.
        // A repeated email tells us which accounts must be connected.
        Map<String, Integer> emailToAccount = new HashMap<>();


        // =========================================================
        // STEP 1: PROCESS ALL ACCOUNTS AND UNION CONNECTED ACCOUNTS
        // =========================================================

        // Visit every account using its index.
        for (int accountIndex = 0; accountIndex < n; accountIndex++) {

            // Get the current account.
            List<String> account = accounts.get(accountIndex);

            // The first element is the person's name.
            // Names are used for the final output, not for identifying
            // whether two accounts belong to the same person.
            String name = account.get(0);


            // Every account contains at least one email because the
            // problem guarantees accounts[i].length >= 2.
            //
            // We use the first email as a connection point for this
            // account, but it is NOT the DSU root necessarily.
            String firstEmail = account.get(1);


            // Process every email in the current account.
            //
            // Start at index 1 because index 0 contains the name.
            for (int j = 1; j < account.size(); j++) {

                String email = account.get(j);

                // putIfAbsent() has two possible outcomes:
                //
                // CASE 1: This email has never appeared before.
                //         Store email -> accountIndex.
                //         Return null.
                //
                // CASE 2: This email has appeared in another account.
                //         Keep the previously stored account index.
                //         Return that previous index.
                //
                // We deliberately preserve the first account index
                // associated with each email.
                Integer previousAccount =
                        emailToAccount.putIfAbsent(email, accountIndex);


                // If previousAccount is not null, this email has
                // appeared in an earlier account.
                //
                // Therefore, both accounts share an email and must
                // belong to the same connected component.
                if (previousAccount != null) {

                    // Merge the components containing these accounts.
                    //
                    // Notice that we do not merge account names or
                    // email strings directly. DSU merges account indices.
                    //
                    // Repeated emails may connect accounts indirectly,
                    // so union operations gradually build the complete
                    // connected components.
                    dsu.union(accountIndex, previousAccount);
                }
            }
        }


        // =========================================================
        // STEP 2: GROUP UNIQUE EMAILS BY THEIR FINAL DSU ROOT
        // =========================================================

        // Key   -> Final root account index of a component.
        // Value -> All unique email addresses belonging to that person.
        //
        // Example:
        //
        // 0 -> [a@mail.com, b@mail.com, c@mail.com]
        // 2 -> [d@mail.com]
        //
        // The exact root indices can vary depending on the order
        // of union operations. Only component membership matters.
        Map<Integer, List<String>> rootToEmails = new HashMap<>();


        // emailToAccount is a HashMap, so every unique email appears
        // exactly once in its key set.
        //
        // We now process emails only after ALL union operations finish.
        // This is important because the root of an account can change
        // when its component is merged with another component.
        for (String email : emailToAccount.keySet()) {

            // Find the account index originally associated with this email.
            //
            // It may not currently be the root of its component.
            int accountIndex = emailToAccount.get(email);

            // Find the FINAL representative of the account's component.
            //
            // Path compression may update parent pointers internally,
            // but it does not change which accounts belong together.
            int root = dsu.find(accountIndex);


            // Get the email list for this root.
            //
            // If this root has not been encountered before, create
            // a new ArrayList.
            //
            // Then add the current email to that component.
            rootToEmails.computeIfAbsent(
                    root,
                    ignored -> new ArrayList<>()
            ).add(email);
        }


        // =========================================================
        // STEP 3: SORT EMAILS AND BUILD THE FINAL OUTPUT
        // =========================================================

        // The required output is a list of merged accounts.
        List<List<String>> result = new ArrayList<>();


        // Each map entry represents one distinct connected component,
        // and therefore one distinct person under the problem's rules.
        for (Map.Entry<Integer, List<String>> entry
                : rootToEmails.entrySet()) {

            // The DSU root identifies the account component.
            int root = entry.getKey();

            // These are all the unique emails belonging to that person.
            List<String> emails = entry.getValue();


            // DSU determines which emails belong together.
            // It does NOT guarantee the order of those emails.
            //
            // Collections.sort() uses String's natural lexicographic
            // ordering, which satisfies the ASCII-ordering requirement
            // for ordinary ASCII email characters.
            Collections.sort(emails);


            // Every account in this connected component belongs to
            // the same person. Therefore, the name from the root
            // account is valid for the entire merged component.
            //
            // Important:
            // The root is an account INDEX, not an email string.
            String name = accounts.get(root).get(0);


            // Construct one merged account in the required format:
            //
            // [name, email1, email2, email3, ...]
            //
            // The emails are already sorted and unique.
            List<String> mergedAccount = new ArrayList<>();

            // The person's name must be the first element.
            mergedAccount.add(name);

            // Append all sorted email addresses.
            mergedAccount.addAll(emails);

            // Add the completed account to the final result.
            result.add(mergedAccount);
        }


        // Every connected component has now been converted into
        // one merged account.
        //
        // The order of the outer result list is unspecified by the
        // problem, so we do not need to sort the merged accounts.
        return result;
    }


    // =============================================================
    // DISJOINT SET UNION (DSU) / UNION-FIND
    // =============================================================
    //
    // Each account index is a node.
    //
    // parent[i] = parent of account i in the DSU tree.
    //
    // If parent[i] == i, account i is the root of its component.
    //
    // size[i] = number of accounts in the component rooted at i.
    // The size is meaningful for roots; non-root entries may be stale.
    //
    // Two optimizations:
    //
    // 1. PATH COMPRESSION:
    //    Make visited nodes point directly to the root during find().
    //
    // 2. UNION BY SIZE:
    //    Attach the smaller component below the larger component.
    //
    // Together, these make DSU operations O(alpha(A)) amortized.
    static class DSU {

        // parent[i] stores the parent account of node i.
        private final int[] parent;

        // size[i] stores the component size when i is a root.
        private final int[] size;


        // Initializes n independent components.
        DSU(int n) {

            parent = new int[n];
            size = new int[n];

            // Initially, every account is its own root.
            //
            // Example for n = 4:
            //
            // parent = [0, 1, 2, 3]
            // size   = [1, 1, 1, 1]
            //
            // Each component contains exactly one account.
            for (int i = 0; i < n; i++) {

                // Every node is initially its own parent.
                parent[i] = i;

                // Every component initially contains one account.
                size[i] = 1;
            }
        }


        /**
         * Finds the representative/root of the component containing x.
         *
         * Path compression is applied during the search.
         */
        int find(int x) {

            // BASE CASE:
            // If x is its own parent, x is already the root.
            if (parent[x] == x) {
                return x;
            }

            // RECURSIVE CASE:
            // x is not the root, so find the root of its parent.
            //
            // Path compression:
            // Update parent[x] to point directly to the root.
            //
            // Before:
            // x -> a -> b -> root
            //
            // After:
            // x --------> root
            //
            // This reduces the number of steps needed by future
            // find() operations.
            parent[x] = find(parent[x]);

            // Return the root after path compression.
            return parent[x];
        }


        /**
         * Merges the components containing accounts a and b.
         *
         * If a and b are already in the same component, do nothing.
         */
        void union(int a, int b) {

            // Always find the current roots before merging.
            //
            // We must merge ROOTS, not blindly attach the original
            // account indices, because a and b may already belong
            // to larger components.
            int rootA = find(a);
            int rootB = find(b);


            // If both roots are equal, a and b are already connected.
            //
            // Repeated emails may cause redundant union operations.
            // This check ensures they do not change the structure.
            if (rootA == rootB) {
                return;
            }


            // UNION BY SIZE:
            //
            // Attach the smaller component to the larger component.
            //
            // Why?
            // Attaching a large tree below a smaller tree can increase
            // tree height unnecessarily. Keeping the larger component
            // as the root helps keep the structure shallow.
            if (size[rootA] < size[rootB]) {

                // Swap roots so rootA always represents the larger
                // component (or one of equal size).
                int temp = rootA;
                rootA = rootB;
                rootB = temp;
            }


            // rootA is now the root of the larger component.
            //
            // Attach rootB's component below rootA.
            parent[rootB] = rootA;


            // Update the size of the resulting component.
            //
            // The size of rootB is no longer needed for future
            // union-by-size decisions because rootB is not a root.
            size[rootA] += size[rootB];
        }
    }


    // =============================================================
    // EXAMPLE EXECUTION
    // =============================================================
    public static void main(String[] args) {

        AccountsMerge solution = new AccountsMerge();

        // Accounts 0 and 1 share johnsmith@mail.com.
        // They must be merged.
        //
        // Account 2 belongs to Mary.
        //
        // Account 3 has the same name as Accounts 0 and 1,
        // but no shared email. It must remain a separate account.
        List<List<String>> accounts = List.of(

                List.of(
                        "John",
                        "johnsmith@mail.com",
                        "john_newyork@mail.com"
                ),

                List.of(
                        "John",
                        "johnsmith@mail.com",
                        "john00@mail.com"
                ),

                List.of(
                        "Mary",
                        "mary@mail.com"
                ),

                List.of(
                        "John",
                        "johnnybravo@mail.com"
                )
        );


        // Run the algorithm and print each merged account.
        List<List<String>> result = solution.accountsMerge(accounts);

        result.forEach(System.out::println);
    }
}




