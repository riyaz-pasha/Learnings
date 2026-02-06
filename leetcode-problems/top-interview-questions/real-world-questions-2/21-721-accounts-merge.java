import java.util.*;

class AccountsMerge {

    static class UnionFind {
        private final Map<String, String> parent = new HashMap<>();
        private final Map<String, Integer> rank = new HashMap<>();

        public void add(String x) {
            parent.putIfAbsent(x, x);
            rank.putIfAbsent(x, 0);
        }

        public String find(String x) {
            if (!parent.get(x).equals(x)) {
                parent.put(x, find(parent.get(x)));
            }
            return parent.get(x);
        }

        public void union(String a, String b) {
            add(a);
            add(b);

            String pa = find(a);
            String pb = find(b);

            if (pa.equals(pb)) return;

            int ra = rank.get(pa);
            int rb = rank.get(pb);

            if (ra < rb) {
                parent.put(pa, pb);
            } else if (rb < ra) {
                parent.put(pb, pa);
            } else {
                parent.put(pb, pa);
                rank.put(pa, ra + 1);
            }
        }
    }

    public List<List<String>> accountsMerge(List<List<String>> accounts) {

        UnionFind uf = new UnionFind();

        // rootEmail -> name
        Map<String, String> emailToName = new HashMap<>();

        // Step 1: Union emails within same account
        for (List<String> acc : accounts) {
            String name = acc.get(0);
            String firstEmail = acc.get(1);

            uf.add(firstEmail);
            emailToName.put(firstEmail, name);

            for (int i = 2; i < acc.size(); i++) {
                String email = acc.get(i);
                uf.add(email);
                emailToName.put(email, name);

                uf.union(firstEmail, email);
            }
        }

        // Step 2: Group emails by root parent
        Map<String, List<String>> groups = new HashMap<>();

        for (String email : emailToName.keySet()) {
            String root = uf.find(email);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(email);
        }

        // Step 3: Build final result
        List<List<String>> result = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
            String root = entry.getKey();
            List<String> emails = entry.getValue();

            Collections.sort(emails);

            List<String> merged = new ArrayList<>();
            merged.add(emailToName.get(root)); // name
            merged.addAll(emails);

            result.add(merged);
        }

        return result;
    }

    /*
     * ⏱️ Complexity
     * Let E = total emails.
     * Union operations: O(E α(E))
     * Sorting per group: total sorting cost O(E log E) overall
     * Space: O(E)
     */
}

/**
 * PROBLEM: Accounts Merge (LeetCode 721)
 * 
 * ============================================================================
 * INTERVIEW APPROACH - HOW TO RECOGNIZE AND SOLVE THIS PROBLEM
 * ============================================================================
 * 
 * 1. PROBLEM RECOGNITION (First 30 seconds):
 *    Keywords to notice: "merge", "common element", "group together"
 *    This is a GRAPH/UNION-FIND problem in disguise!
 *    
 *    Key insight: If two accounts share an email, they're CONNECTED
 *    We need to find CONNECTED COMPONENTS in a graph
 * 
 * 2. MENTAL MODEL (Explain to interviewer):
 *    "Think of this as a graph problem where:
 *     - Each email is a node
 *     - If two emails belong to same account, draw an edge
 *     - Find all connected components
 *     - Each component = one person's merged account"
 * 
 * 3. TWO MAIN APPROACHES:
 *    a) Union-Find (Disjoint Set Union) - Most efficient
 *    b) DFS/BFS - More intuitive, easier to code in interview
 * 
 * 4. WHAT TO ASK INTERVIEWER:
 *    - Can accounts be empty? (Usually no)
 *    - Can an account have no emails? (Usually no)
 *    - Are emails case-sensitive? (Assume yes)
 *    - Do we need to validate email format? (Usually no)
 *    - Can same person have different names? (No, problem states same name)
 * 
 * 5. EDGE CASES TO CONSIDER:
 *    - Single account
 *    - No common emails (no merging needed)
 *    - All accounts belong to one person
 *    - Circular dependencies (A-B, B-C, C-A)
 *    - Same name, different people
 * 
 * ============================================================================
 */


class Solution {
    
    /**
     * APPROACH 1: DFS (Most Intuitive for Interviews - START HERE)
     * 
     * WHY DFS FIRST IN INTERVIEWS:
     * - Easier to explain and visualize
     * - Natural graph traversal approach
     * - Less chance of implementation bugs
     * - Shows solid understanding of graph fundamentals
     * 
     * ALGORITHM:
     * 1. Build a graph: email -> list of connected emails
     * 2. Build a map: email -> account name
     * 3. DFS from each unvisited email to find connected component
     * 4. Sort emails in each component and add name
     * 
     * Time: O(N * K * log(K)) where N = accounts, K = max emails per account
     *       - Building graph: O(N*K)
     *       - DFS: O(N*K) 
     *       - Sorting: O(N*K*log(K))
     * Space: O(N*K) for graph and visited set
     */
    public List<List<String>> accountsMerge(List<List<String>> accounts) {
        // STEP 1: Build the graph
        // Key insight: Create bidirectional edges between all emails in same account
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, String> emailToName = new HashMap<>();
        
        // Build adjacency list
        for (List<String> account : accounts) {
            String name = account.get(0);
            String firstEmail = account.get(1); // Every account has at least one email
            
            // Process each email in this account
            for (int i = 1; i < account.size(); i++) {
                String email = account.get(i);
                
                // Map email to name
                emailToName.put(email, name);
                
                // Initialize graph entry
                graph.putIfAbsent(email, new HashSet<>());
                
                // CRITICAL: Connect first email with all other emails
                // This creates a connected component for this account
                if (i > 1) {
                    graph.get(firstEmail).add(email);
                    graph.get(email).add(firstEmail);
                }
            }
        }
        
        // STEP 2: DFS to find connected components
        Set<String> visited = new HashSet<>();
        List<List<String>> result = new ArrayList<>();
        
        // Visit each email (potential starting point for a component)
        for (String email : graph.keySet()) {
            if (!visited.contains(email)) {
                // Found a new connected component
                List<String> component = new ArrayList<>();
                dfs(graph, email, visited, component);
                
                // STEP 3: Sort emails and add name
                Collections.sort(component);
                component.add(0, emailToName.get(email)); // Add name at beginning
                result.add(component);
            }
        }
        
        return result;
    }
    
    /**
     * DFS helper to collect all emails in a connected component
     * 
     * @param graph - adjacency list of email connections
     * @param email - current email being processed
     * @param visited - set of visited emails
     * @param component - list collecting emails in this component
     */
    private void dfs(Map<String, Set<String>> graph, String email, 
                     Set<String> visited, List<String> component) {
        // Mark as visited
        visited.add(email);
        component.add(email);
        
        // Visit all neighbors (connected emails)
        if (graph.containsKey(email)) {
            for (String neighbor : graph.get(email)) {
                if (!visited.contains(neighbor)) {
                    dfs(graph, neighbor, visited, component);
                }
            }
        }
    }
    
    /**
     * APPROACH 2: UNION-FIND (Optimal - Show After DFS)
     * 
     * WHY UNION-FIND:
     * - Specifically designed for "merge" operations
     * - Near O(1) union and find operations (with path compression + union by rank)
     * - More efficient for very large inputs
     * - Shows knowledge of advanced data structures
     * 
     * WHEN TO MENTION IN INTERVIEW:
     * "I can also solve this with Union-Find which is optimal for merge operations.
     *  Would you like me to implement that approach?"
     * 
     * Time: O(N*K*α(N*K)) where α is inverse Ackermann (practically constant)
     * Space: O(N*K)
     */
    public List<List<String>> accountsMergeUnionFind(List<List<String>> accounts) {
        UnionFind uf = new UnionFind();
        Map<String, String> emailToName = new HashMap<>();
        
        // STEP 1: Union all emails in the same account
        for (List<String> account : accounts) {
            String name = account.get(0);
            String firstEmail = account.get(1);
            
            for (int i = 1; i < account.size(); i++) {
                String email = account.get(i);
                emailToName.put(email, name);
                
                // Union this email with the first email in account
                // This creates a connected component
                uf.union(firstEmail, email);
            }
        }
        
        // STEP 2: Group emails by their root parent
        // All emails with same root belong to same person
        Map<String, List<String>> components = new HashMap<>();
        for (String email : emailToName.keySet()) {
            String root = uf.find(email);
            components.putIfAbsent(root, new ArrayList<>());
            components.get(root).add(email);
        }
        
        // STEP 3: Build result with sorted emails
        List<List<String>> result = new ArrayList<>();
        for (List<String> emails : components.values()) {
            Collections.sort(emails);
            String name = emailToName.get(emails.get(0));
            emails.add(0, name);
            result.add(emails);
        }
        
        return result;
    }
    
    /**
     * UNION-FIND DATA STRUCTURE (Disjoint Set Union)
     * 
     * INTERVIEW TIP: Know this data structure cold!
     * It's asked frequently and shows strong CS fundamentals
     * 
     * Key operations:
     * - find(x): Find root/representative of x's set
     * - union(x, y): Merge sets containing x and y
     * 
     * Optimizations:
     * 1. Path Compression: Make tree flat during find
     * 2. Union by Rank: Attach smaller tree under larger tree
     */
    class UnionFind {
        private Map<String, String> parent;
        private Map<String, Integer> rank;
        
        public UnionFind() {
            parent = new HashMap<>();
            rank = new HashMap<>();
        }
        
        /**
         * Find root of x's set with PATH COMPRESSION
         * 
         * Path compression: During find, make every node point directly to root
         * This flattens the tree, making future operations faster
         * 
         * Example: 
         * Before: A -> B -> C -> D (root)
         * After:  A -> D, B -> D, C -> D (all point to root)
         */
        public String find(String x) {
            // Initialize if seeing for first time
            if (!parent.containsKey(x)) {
                parent.put(x, x);
                rank.put(x, 0);
                return x;
            }
            
            // Path compression: recursively find root and update parent
            if (!parent.get(x).equals(x)) {
                parent.put(x, find(parent.get(x)));
            }
            
            return parent.get(x);
        }
        
        /**
         * Union two sets with UNION BY RANK
         * 
         * Union by rank: Always attach smaller tree under root of larger tree
         * This keeps tree height logarithmic
         * 
         * Rank is an upper bound on tree height
         */
        public void union(String x, String y) {
            String rootX = find(x);
            String rootY = find(y);
            
            // Already in same set
            if (rootX.equals(rootY)) {
                return;
            }
            
            // Union by rank: attach smaller tree under larger tree
            int rankX = rank.get(rootX);
            int rankY = rank.get(rootY);
            
            if (rankX < rankY) {
                parent.put(rootX, rootY);
            } else if (rankX > rankY) {
                parent.put(rootY, rootX);
            } else {
                // Same rank: choose one as parent, increase rank
                parent.put(rootY, rootX);
                rank.put(rootX, rankX + 1);
            }
        }
    }
    
    /**
     * APPROACH 3: BFS (Alternative to DFS)
     * 
     * WHEN TO USE:
     * - Personal preference over DFS
     * - Interviewer asks for iterative solution
     * - Want to avoid recursion stack overflow (though rare)
     * 
     * Time: O(N*K*log(K))
     * Space: O(N*K)
     */
    public List<List<String>> accountsMergeBFS(List<List<String>> accounts) {
        // Build graph (same as DFS approach)
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, String> emailToName = new HashMap<>();
        
        for (List<String> account : accounts) {
            String name = account.get(0);
            String firstEmail = account.get(1);
            
            for (int i = 1; i < account.size(); i++) {
                String email = account.get(i);
                emailToName.put(email, name);
                graph.putIfAbsent(email, new HashSet<>());
                
                if (i > 1) {
                    graph.get(firstEmail).add(email);
                    graph.get(email).add(firstEmail);
                }
            }
        }
        
        // BFS to find connected components
        Set<String> visited = new HashSet<>();
        List<List<String>> result = new ArrayList<>();
        
        for (String email : graph.keySet()) {
            if (!visited.contains(email)) {
                List<String> component = bfs(graph, email, visited);
                Collections.sort(component);
                component.add(0, emailToName.get(email));
                result.add(component);
            }
        }
        
        return result;
    }
    
    /**
     * BFS helper using queue
     */
    private List<String> bfs(Map<String, Set<String>> graph, String startEmail, 
                            Set<String> visited) {
        List<String> component = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        
        queue.offer(startEmail);
        visited.add(startEmail);
        
        while (!queue.isEmpty()) {
            String email = queue.poll();
            component.add(email);
            
            if (graph.containsKey(email)) {
                for (String neighbor : graph.get(email)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
        }
        
        return component;
    }
    
    /**
     * ========================================================================
     * COMPLETE TEST SUITE - WALK THROUGH THESE IN INTERVIEWS
     * ========================================================================
     */
    public static void main(String[] args) {
        Solution sol = new Solution();
        
        // TEST CASE 1: Basic merge example
        List<List<String>> accounts1 = Arrays.asList(
            Arrays.asList("John", "johnsmith@mail.com", "john_newyork@mail.com"),
            Arrays.asList("John", "johnsmith@mail.com", "john00@mail.com"),
            Arrays.asList("Mary", "mary@mail.com"),
            Arrays.asList("John", "johnnybravo@mail.com")
        );
        System.out.println("Test 1 - Basic merge:");
        System.out.println(sol.accountsMerge(accounts1));
        // Expected: [
        //   ["John", "john00@mail.com", "john_newyork@mail.com", "johnsmith@mail.com"],
        //   ["Mary", "mary@mail.com"],
        //   ["John", "johnnybravo@mail.com"]
        // ]
        System.out.println();
        
        // TEST CASE 2: Transitive merge (A-B, B-C should merge A-B-C)
        List<List<String>> accounts2 = Arrays.asList(
            Arrays.asList("David", "david0@mail.com", "david1@mail.com"),
            Arrays.asList("David", "david1@mail.com", "david2@mail.com"),
            Arrays.asList("David", "david2@mail.com", "david3@mail.com")
        );
        System.out.println("Test 2 - Transitive merge:");
        System.out.println(sol.accountsMerge(accounts2));
        // Expected: [["David", "david0@mail.com", "david1@mail.com", 
        //             "david2@mail.com", "david3@mail.com"]]
        System.out.println();
        
        // TEST CASE 3: Same name, different people (no common emails)
        List<List<String>> accounts3 = Arrays.asList(
            Arrays.asList("John", "john1@mail.com"),
            Arrays.asList("John", "john2@mail.com"),
            Arrays.asList("John", "john3@mail.com")
        );
        System.out.println("Test 3 - Same name, different people:");
        System.out.println(sol.accountsMerge(accounts3));
        // Expected: Three separate accounts
        System.out.println();
        
        // TEST CASE 4: Single account
        List<List<String>> accounts4 = Arrays.asList(
            Arrays.asList("Alice", "alice@mail.com", "alice2@mail.com")
        );
        System.out.println("Test 4 - Single account:");
        System.out.println(sol.accountsMerge(accounts4));
        System.out.println();
        
        // TEST CASE 5: Complex graph (circular dependencies)
        List<List<String>> accounts5 = Arrays.asList(
            Arrays.asList("Kevin", "kevin0@mail.com", "kevin5@mail.com"),
            Arrays.asList("Bob", "bob0@mail.com"),
            Arrays.asList("Kevin", "kevin3@mail.com", "kevin0@mail.com"),
            Arrays.asList("Kevin", "kevin5@mail.com", "kevin3@mail.com")
        );
        System.out.println("Test 5 - Complex circular merge:");
        System.out.println(sol.accountsMerge(accounts5));
        System.out.println();
        
        // Compare all three approaches
        System.out.println("Comparing DFS, Union-Find, and BFS:");
        System.out.println("DFS:        " + sol.accountsMerge(accounts1));
        System.out.println("Union-Find: " + sol.accountsMergeUnionFind(accounts1));
        System.out.println("BFS:        " + sol.accountsMergeBFS(accounts1));
    }
}

/**
 * ============================================================================
 * INTERVIEW STRATEGY - STEP BY STEP
 * ============================================================================
 * 
 * TIMELINE (45 minute interview):
 * 
 * 0-2 min: Clarify problem, ask questions
 *   "Let me make sure I understand... we're merging accounts that share emails"
 *   "Can accounts have the same name but be different people? Yes"
 *   "Do we need to sort the final emails? Yes"
 * 
 * 2-5 min: Discuss approach
 *   "This is a graph connectivity problem. Think of emails as nodes."
 *   "If two emails are in same account, they're connected."
 *   "We need to find connected components."
 *   "I can solve this with DFS or Union-Find. DFS is more intuitive."
 * 
 * 5-7 min: Discuss complexity
 *   "Building graph: O(NK) where N=accounts, K=emails per account"
 *   "DFS: O(NK) to visit all emails"
 *   "Sorting: O(NK log K) dominates"
 *   "Total: O(NK log K)"
 *   "Space: O(NK) for graph"
 * 
 * 7-25 min: Code DFS solution
 *   - Build graph step by step
 *   - Implement DFS carefully
 *   - Add name and sort at end
 * 
 * 25-30 min: Test with examples
 *   - Walk through Test Case 1 manually
 *   - Verify Test Case 2 (transitive merge)
 *   - Check edge case (Test Case 3)
 * 
 * 30-35 min: Discuss optimizations
 *   "We could also use Union-Find which is optimal for merge operations"
 *   "Union-Find with path compression gives nearly O(1) operations"
 * 
 * 35-40 min: Code Union-Find if time permits
 * 
 * 40-45 min: Discuss follow-ups and edge cases
 * 
 * ============================================================================
 * COMMON MISTAKES TO AVOID
 * ============================================================================
 * 
 * 1. GRAPH BUILDING ERRORS:
 *    ❌ Not creating bidirectional edges
 *    ✅ When adding edge A->B, also add B->A
 * 
 * 2. FORGETTING TO SORT:
 *    ❌ Returning unsorted emails
 *    ✅ Collections.sort() before adding to result
 * 
 * 3. NAME HANDLING:
 *    ❌ Using name to determine if same person
 *    ✅ Only use email connections, name is just metadata
 * 
 * 4. OFF-BY-ONE ERRORS:
 *    ❌ Starting loop at i=0 (that's the name!)
 *    ✅ Start at i=1 for emails
 * 
 * 5. UNION-FIND BUGS:
 *    ❌ Forgetting path compression
 *    ❌ Not initializing parent/rank
 *    ✅ Implement both optimizations correctly
 * 
 * 6. EMPTY CHECKS:
 *    ❌ Not checking if graph.containsKey()
 *    ✅ Always check before accessing neighbors
 * 
 * ============================================================================
 * FOLLOW-UP QUESTIONS YOU MIGHT GET
 * ============================================================================
 * 
 * Q1: "What if we need to handle millions of accounts?"
 * A: Union-Find is better due to near-constant time operations
 *    Could also use distributed processing (MapReduce pattern)
 * 
 * Q2: "What if emails can have typos (similarity matching)?"
 * A: Would need fuzzy matching (edit distance)
 *    More complex - might use clustering algorithms
 * 
 * Q3: "How would you handle streaming data (accounts added over time)?"
 * A: Union-Find supports incremental updates naturally
 *    Maintain the data structure and union as new data arrives
 * 
 * Q4: "Can you return which original accounts were merged?"
 * A: Keep track of account indices during graph building
 *    Store mapping from email to original account indices
 * 
 * Q5: "What if we want to detect suspicious activity (same email, different names)?"
 * A: Track conflicts during email-to-name mapping
 *    If email maps to different names, flag it
 * 
 * Q6: "Space optimization - can we do better than O(NK)?"
 * A: Not really - we need to store all emails somewhere
 *    Could use string interning to save space on duplicates
 * 
 * ============================================================================
 * KEY INSIGHTS TO MENTION
 * ============================================================================
 * 
 * 1. This is fundamentally a CONNECTED COMPONENTS problem
 * 2. Graph representation makes the problem much clearer
 * 3. Union-Find is THE data structure for merging/grouping
 * 4. Path compression + union by rank gives near O(1) operations
 * 5. Transitive relationships are handled automatically by both approaches
 * 6. Sorting is required at the end - don't forget!
 * 
 * ============================================================================
 */
