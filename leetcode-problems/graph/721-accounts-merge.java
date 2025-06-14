import java.util.*;
/*
 * Given a list of accounts where each element accounts[i] is a list of strings,
 * where the first element accounts[i][0] is a name, and the rest of the
 * elements are emails representing emails of the account.
 * 
 * Now, we would like to merge these accounts. Two accounts definitely belong to
 * the same person if there is some common email to both accounts. Note that
 * even if two accounts have the same name, they may belong to different people
 * as people could have the same name. A person can have any number of accounts
 * initially, but all of their accounts definitely have the same name.
 * 
 * After merging the accounts, return the accounts in the following format: the
 * first element of each account is the name, and the rest of the elements are
 * emails in sorted order. The accounts themselves can be returned in any order.
 * 
 * Example 1:
 * 
 * Input: accounts =
 * [["John","johnsmith@mail.com","john_newyork@mail.com"],["John",
 * "johnsmith@mail.com","john00@mail.com"],["Mary","mary@mail.com"],["John",
 * "johnnybravo@mail.com"]]
 * Output:
 * [["John","john00@mail.com","john_newyork@mail.com","johnsmith@mail.com"],[
 * "Mary","mary@mail.com"],["John","johnnybravo@mail.com"]]
 * Explanation:
 * The first and second John's are the same person as they have the common email
 * "johnsmith@mail.com".
 * The third John and Mary are different people as none of their email addresses
 * are used by other accounts.
 * We could return these lists in any order, for example the answer [['Mary',
 * 'mary@mail.com'], ['John', 'johnnybravo@mail.com'],
 * ['John', 'john00@mail.com', 'john_newyork@mail.com', 'johnsmith@mail.com']]
 * would still be accepted.
 * 
 * Example 2:
 * 
 * Input: accounts =
 * [["Gabe","Gabe0@m.co","Gabe3@m.co","Gabe1@m.co"],["Kevin","Kevin3@m.co",
 * "Kevin5@m.co","Kevin0@m.co"],["Ethan","Ethan5@m.co","Ethan4@m.co",
 * "Ethan0@m.co"],["Hanzo","Hanzo3@m.co","Hanzo1@m.co","Hanzo0@m.co"],["Fern",
 * "Fern5@m.co","Fern1@m.co","Fern0@m.co"]]
 * Output:
 * [["Ethan","Ethan0@m.co","Ethan4@m.co","Ethan5@m.co"],["Gabe","Gabe0@m.co",
 * "Gabe1@m.co","Gabe3@m.co"],["Hanzo","Hanzo0@m.co","Hanzo1@m.co","Hanzo3@m.co"
 * ],["Kevin","Kevin0@m.co","Kevin3@m.co","Kevin5@m.co"],["Fern","Fern0@m.co",
 * "Fern1@m.co","Fern5@m.co"]]
 * 
 */

class Solution {
    
    /**
     * Approach 1: Union-Find (Most Efficient)
     * Time: O(N * α(N)) where N is total number of emails
     * Space: O(N)
     */
    public List<List<String>> accountsMerge(List<List<String>> accounts) {
        UnionFind uf = new UnionFind();
        Map<String, String> emailToName = new HashMap<>();
        
        // Process each account
        for (List<String> account : accounts) {
            String name = account.get(0);
            String firstEmail = account.get(1);
            
            // Map each email to the account name
            for (int i = 1; i < account.size(); i++) {
                String email = account.get(i);
                emailToName.put(email, name);
                
                // Union current email with the first email of this account
                uf.union(firstEmail, email);
            }
        }
        
        // Group emails by their root parent
        Map<String, List<String>> groups = new HashMap<>();
        for (String email : emailToName.keySet()) {
            String root = uf.find(email);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(email);
        }
        
        // Build result
        List<List<String>> result = new ArrayList<>();
        for (List<String> emails : groups.values()) {
            Collections.sort(emails);
            String name = emailToName.get(emails.get(0));
            
            List<String> account = new ArrayList<>();
            account.add(name);
            account.addAll(emails);
            result.add(account);
        }
        
        return result;
    }
    
    /**
     * Union-Find with Path Compression and Union by Rank
     */
    class UnionFind {
        private Map<String, String> parent = new HashMap<>();
        private Map<String, Integer> rank = new HashMap<>();
        
        public String find(String x) {
            if (!parent.containsKey(x)) {
                parent.put(x, x);
                rank.put(x, 0);
            }
            
            if (!parent.get(x).equals(x)) {
                parent.put(x, find(parent.get(x))); // Path compression
            }
            return parent.get(x);
        }
        
        public void union(String x, String y) {
            String px = find(x), py = find(y);
            if (px.equals(py)) return;
            
            // Union by rank
            if (rank.get(px) < rank.get(py)) {
                parent.put(px, py);
            } else if (rank.get(px) > rank.get(py)) {
                parent.put(py, px);
            } else {
                parent.put(py, px);
                rank.put(px, rank.get(px) + 1);
            }
        }
    }
    
    /**
     * Approach 2: DFS (Graph-based approach)
     * Time: O(N log N) for sorting, Space: O(N)
     */
    public List<List<String>> accountsMergeV2(List<List<String>> accounts) {
        Map<String, String> emailToName = new HashMap<>();
        Map<String, List<String>> graph = new HashMap<>();
        
        // Build graph: each email connects to all other emails in same account
        for (List<String> account : accounts) {
            String name = account.get(0);
            
            for (int i = 1; i < account.size(); i++) {
                String email = account.get(i);
                emailToName.put(email, name);
                graph.putIfAbsent(email, new ArrayList<>());
                
                // Connect current email to first email (and vice versa)
                if (i > 1) {
                    String firstEmail = account.get(1);
                    graph.get(email).add(firstEmail);
                    graph.get(firstEmail).add(email);
                }
            }
        }
        
        Set<String> visited = new HashSet<>();
        List<List<String>> result = new ArrayList<>();
        
        // DFS to find connected components
        for (String email : emailToName.keySet()) {
            if (!visited.contains(email)) {
                List<String> component = new ArrayList<>();
                dfs(email, graph, visited, component);
                
                Collections.sort(component);
                String name = emailToName.get(component.get(0));
                
                List<String> account = new ArrayList<>();
                account.add(name);
                account.addAll(component);
                result.add(account);
            }
        }
        
        return result;
    }
    
    private void dfs(String email, Map<String, List<String>> graph, 
                     Set<String> visited, List<String> component) {
        visited.add(email);
        component.add(email);
        
        if (graph.containsKey(email)) {
            for (String neighbor : graph.get(email)) {
                if (!visited.contains(neighbor)) {
                    dfs(neighbor, graph, visited, component);
                }
            }
        }
    }
    
    /**
     * Approach 3: BFS (Alternative graph approach)
     * Time: O(N log N), Space: O(N)
     */
    public List<List<String>> accountsMergeV3(List<List<String>> accounts) {
        Map<String, String> emailToName = new HashMap<>();
        Map<String, Set<String>> graph = new HashMap<>();
        
        // Build adjacency list
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
        
        Set<String> visited = new HashSet<>();
        List<List<String>> result = new ArrayList<>();
        
        for (String email : emailToName.keySet()) {
            if (!visited.contains(email)) {
                List<String> component = bfs(email, graph, visited);
                Collections.sort(component);
                
                String name = emailToName.get(component.get(0));
                List<String> account = new ArrayList<>();
                account.add(name);
                account.addAll(component);
                result.add(account);
            }
        }
        
        return result;
    }
    
    private List<String> bfs(String start, Map<String, Set<String>> graph, Set<String> visited) {
        List<String> component = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();
        
        queue.offer(start);
        visited.add(start);
        
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
     * Test the solution
     */
    public static void main(String[] args) {
        Solution sol = new Solution();
        
        // Test case 1
        List<List<String>> accounts1 = Arrays.asList(
            Arrays.asList("John", "johnsmith@mail.com", "john_newyork@mail.com"),
            Arrays.asList("John", "johnsmith@mail.com", "john00@mail.com"),
            Arrays.asList("Mary", "mary@mail.com"),
            Arrays.asList("John", "johnnybravo@mail.com")
        );
        System.out.println("Test 1: " + sol.accountsMerge(accounts1));
        
        // Test case 2
        List<List<String>> accounts2 = Arrays.asList(
            Arrays.asList("Gabe", "Gabe0@m.co", "Gabe3@m.co", "Gabe1@m.co"),
            Arrays.asList("Kevin", "Kevin3@m.co", "Kevin5@m.co", "Kevin0@m.co"),
            Arrays.asList("Ethan", "Ethan5@m.co", "Ethan4@m.co", "Ethan0@m.co"),
            Arrays.asList("Hanzo", "Hanzo3@m.co", "Hanzo1@m.co", "Hanzo0@m.co"),
            Arrays.asList("Fern", "Fern5@m.co", "Fern1@m.co", "Fern0@m.co")
        );
        System.out.println("Test 2: " + sol.accountsMerge(accounts2));
    }
}

/**
 * ALGORITHM COMPARISON:
 * 
 * 1. UNION-FIND APPROACH (RECOMMENDED):
 *    - Most efficient for this problem
 *    - Time: O(N * α(N)) where α is inverse Ackermann function
 *    - Space: O(N)
 *    - Key insight: Union all emails within same account, then group by root
 * 
 * 2. DFS APPROACH:
 *    - Build graph where emails in same account are connected
 *    - Find connected components using DFS
 *    - Time: O(N log N) due to sorting
 *    - Space: O(N)
 * 
 * 3. BFS APPROACH:
 *    - Similar to DFS but uses BFS for connected components
 *    - Same complexity as DFS
 * 
 * UNION-FIND STRATEGY:
 * 1. For each account, union all emails with the first email
 * 2. This creates connected components of emails belonging to same person
 * 3. Group emails by their root parent in Union-Find
 * 4. Sort emails within each group and add account name
 * 
 * The Union-Find approach is most elegant because it directly models the
 * "merging" operation we want to perform.
 */

class AccountsMerge {

    public List<List<String>> accountsMerge(List<List<String>> accounts) {

        Map<String, String> parent = new HashMap<>();
        Map<String, String> emailToName = new HashMap<>();

        for (List<String> account : accounts) {
            String name = account.get(0);
            for (int i = 1; i < account.size(); i++) {
                String email = account.get(i);
                parent.putIfAbsent(email, email);
                emailToName.put(email, name);
                union(parent, account.get(1), email);
            }
        }

        Map<String, Set<String>> rootToEmails = new HashMap<>();
        for (String email : parent.keySet()) {
            String root = find(parent, email);
            rootToEmails.computeIfAbsent(root, x -> new HashSet<>()).add(email);
        }

        List<List<String>> merged = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : rootToEmails.entrySet()) {
            String name = emailToName.get(entry.getKey());
            List<String> emails = new ArrayList<>(entry.getValue());
            Collections.sort(emails);
            List<String> account = new ArrayList<>();
            account.add(name);
            account.addAll(emails);
            merged.add(account);
        }
        return merged;
    }

    private void union(Map<String, String> parent, String email1, String email2) {
        String rootEmail1 = find(parent, email1);
        String rootEmail2 = find(parent, email2);
        if (!rootEmail1.equals(rootEmail2)) {
            parent.put(rootEmail2, rootEmail1);
        }
    }

    private String find(Map<String, String> parent, String email) {
        if (!parent.get(email).equals(email)) {
            parent.put(email, find(parent, parent.get(email)));
        }
        return parent.get(email);
    }

}

/*
DETAILED TRACING OF ACCOUNTS MERGE ALGORITHM

Example Input:
accounts = [
    ["John", "johnsmith@mail.com", "john_newyork@mail.com"],
    ["John", "johnsmith@mail.com", "john00@mail.com"], 
    ["Mary", "mary@mail.com"],
    ["John", "johnnybravo@mail.com"]
]

STEP 1: INITIALIZATION
- Create UnionFind instance: uf = new UnionFind()
- Create emailToName map: emailToName = {}

STEP 2: PROCESS EACH ACCOUNT

Processing Account 0: ["John", "johnsmith@mail.com", "john_newyork@mail.com"]
- name = "John"
- firstEmail = "johnsmith@mail.com"
- Loop through emails (i=1 to 2):
  
  i=1: email = "johnsmith@mail.com"
  - emailToName.put("johnsmith@mail.com", "John")
  - emailToName = {"johnsmith@mail.com" -> "John"}
  - uf.union("johnsmith@mail.com", "johnsmith@mail.com")
    * find("johnsmith@mail.com"): not in parent map, so parent["johnsmith@mail.com"] = "johnsmith@mail.com", rank["johnsmith@mail.com"] = 0
    * find("johnsmith@mail.com"): returns "johnsmith@mail.com"
    * px = py = "johnsmith@mail.com", so union returns early (same root)
  
  i=2: email = "john_newyork@mail.com"
  - emailToName.put("john_newyork@mail.com", "John")
  - emailToName = {"johnsmith@mail.com" -> "John", "john_newyork@mail.com" -> "John"}
  - uf.union("johnsmith@mail.com", "john_newyork@mail.com")
    * find("johnsmith@mail.com"): returns "johnsmith@mail.com"
    * find("john_newyork@mail.com"): not in parent map, so parent["john_newyork@mail.com"] = "john_newyork@mail.com", rank["john_newyork@mail.com"] = 0
    * px = "johnsmith@mail.com", py = "john_newyork@mail.com"
    * rank[px] == rank[py] (both 0), so parent["john_newyork@mail.com"] = "johnsmith@mail.com", rank["johnsmith@mail.com"] = 1
    * UnionFind state: parent = {"johnsmith@mail.com" -> "johnsmith@mail.com", "john_newyork@mail.com" -> "johnsmith@mail.com"}
                      rank = {"johnsmith@mail.com" -> 1, "john_newyork@mail.com" -> 0}

Processing Account 1: ["John", "johnsmith@mail.com", "john00@mail.com"]
- name = "John"
- firstEmail = "johnsmith@mail.com"
- Loop through emails (i=1 to 2):
  
  i=1: email = "johnsmith@mail.com"
  - emailToName already contains this key, overwrites with same value "John"
  - emailToName = {"johnsmith@mail.com" -> "John", "john_newyork@mail.com" -> "John"}
  - uf.union("johnsmith@mail.com", "johnsmith@mail.com"): same email, returns early
  
  i=2: email = "john00@mail.com"
  - emailToName.put("john00@mail.com", "John")
  - emailToName = {"johnsmith@mail.com" -> "John", "john_newyork@mail.com" -> "John", "john00@mail.com" -> "John"}
  - uf.union("johnsmith@mail.com", "john00@mail.com")
    * find("johnsmith@mail.com"): returns "johnsmith@mail.com"
    * find("john00@mail.com"): not in parent map, so parent["john00@mail.com"] = "john00@mail.com", rank["john00@mail.com"] = 0
    * px = "johnsmith@mail.com", py = "john00@mail.com"
    * rank[px] = 1 > rank[py] = 0, so parent["john00@mail.com"] = "johnsmith@mail.com"
    * UnionFind state: parent = {"johnsmith@mail.com" -> "johnsmith@mail.com", "john_newyork@mail.com" -> "johnsmith@mail.com", "john00@mail.com" -> "johnsmith@mail.com"}
                      rank = {"johnsmith@mail.com" -> 1, "john_newyork@mail.com" -> 0, "john00@mail.com" -> 0}

Processing Account 2: ["Mary", "mary@mail.com"]
- name = "Mary"
- firstEmail = "mary@mail.com"
- Loop through emails (i=1 to 1):
  
  i=1: email = "mary@mail.com"
  - emailToName.put("mary@mail.com", "Mary")
  - emailToName = {"johnsmith@mail.com" -> "John", "john_newyork@mail.com" -> "John", "john00@mail.com" -> "John", "mary@mail.com" -> "Mary"}
  - uf.union("mary@mail.com", "mary@mail.com"): same email, returns early after initializing parent and rank

Processing Account 3: ["John", "johnnybravo@mail.com"]
- name = "John"
- firstEmail = "johnnybravo@mail.com"
- Loop through emails (i=1 to 1):
  
  i=1: email = "johnnybravo@mail.com"
  - emailToName.put("johnnybravo@mail.com", "John")
  - emailToName = {"johnsmith@mail.com" -> "John", "john_newyork@mail.com" -> "John", "john00@mail.com" -> "John", "mary@mail.com" -> "Mary", "johnnybravo@mail.com" -> "John"}
  - uf.union("johnnybravo@mail.com", "johnnybravo@mail.com"): same email, returns early after initializing parent and rank

FINAL UNIONFIND STATE:
parent = {
  "johnsmith@mail.com" -> "johnsmith@mail.com",
  "john_newyork@mail.com" -> "johnsmith@mail.com", 
  "john00@mail.com" -> "johnsmith@mail.com",
  "mary@mail.com" -> "mary@mail.com",
  "johnnybravo@mail.com" -> "johnnybravo@mail.com"
}

STEP 3: GROUP EMAILS BY ROOT PARENT
- Initialize groups = {}
- For each email in emailToName.keySet():

  email = "johnsmith@mail.com"
  - root = uf.find("johnsmith@mail.com") = "johnsmith@mail.com"
  - groups.computeIfAbsent("johnsmith@mail.com", k -> new ArrayList<>()).add("johnsmith@mail.com")
  - groups = {"johnsmith@mail.com" -> ["johnsmith@mail.com"]}

  email = "john_newyork@mail.com"
  - root = uf.find("john_newyork@mail.com") = "johnsmith@mail.com" (follows parent pointer)
  - groups.get("johnsmith@mail.com").add("john_newyork@mail.com")
  - groups = {"johnsmith@mail.com" -> ["johnsmith@mail.com", "john_newyork@mail.com"]}

  email = "john00@mail.com"
  - root = uf.find("john00@mail.com") = "johnsmith@mail.com"
  - groups.get("johnsmith@mail.com").add("john00@mail.com")
  - groups = {"johnsmith@mail.com" -> ["johnsmith@mail.com", "john_newyork@mail.com", "john00@mail.com"]}

  email = "mary@mail.com"
  - root = uf.find("mary@mail.com") = "mary@mail.com"
  - groups.computeIfAbsent("mary@mail.com", k -> new ArrayList<>()).add("mary@mail.com")
  - groups = {"johnsmith@mail.com" -> ["johnsmith@mail.com", "john_newyork@mail.com", "john00@mail.com"], "mary@mail.com" -> ["mary@mail.com"]}

  email = "johnnybravo@mail.com"
  - root = uf.find("johnnybravo@mail.com") = "johnnybravo@mail.com"
  - groups.computeIfAbsent("johnnybravo@mail.com", k -> new ArrayList<>()).add("johnnybravo@mail.com")
  - groups = {"johnsmith@mail.com" -> ["johnsmith@mail.com", "john_newyork@mail.com", "john00@mail.com"], "mary@mail.com" -> ["mary@mail.com"], "johnnybravo@mail.com" -> ["johnnybravo@mail.com"]}

STEP 4: BUILD RESULT
- Initialize result = []
- For each email group in groups.values():

  emails = ["johnsmith@mail.com", "john_newyork@mail.com", "john00@mail.com"]
  - Collections.sort(emails) -> ["john00@mail.com", "john_newyork@mail.com", "johnsmith@mail.com"]
  - name = emailToName.get("john00@mail.com") = "John"
  - account = ["John"]
  - account.addAll(emails) -> account = ["John", "john00@mail.com", "john_newyork@mail.com", "johnsmith@mail.com"]
  - result.add(account)
  - result = [["John", "john00@mail.com", "john_newyork@mail.com", "johnsmith@mail.com"]]

  emails = ["mary@mail.com"]
  - Collections.sort(emails) -> ["mary@mail.com"] (already sorted)
  - name = emailToName.get("mary@mail.com") = "Mary"
  - account = ["Mary"]
  - account.addAll(emails) -> account = ["Mary", "mary@mail.com"]
  - result.add(account)
  - result = [["John", "john00@mail.com", "john_newyork@mail.com", "johnsmith@mail.com"], ["Mary", "mary@mail.com"]]

  emails = ["johnnybravo@mail.com"]
  - Collections.sort(emails) -> ["johnnybravo@mail.com"] (already sorted)
  - name = emailToName.get("johnnybravo@mail.com") = "John"
  - account = ["John"]
  - account.addAll(emails) -> account = ["John", "johnnybravo@mail.com"]
  - result.add(account)
  - result = [["John", "john00@mail.com", "john_newyork@mail.com", "johnsmith@mail.com"], ["Mary", "mary@mail.com"], ["John", "johnnybravo@mail.com"]]

FINAL RESULT:
[
  ["John", "john00@mail.com", "john_newyork@mail.com", "johnsmith@mail.com"],
  ["Mary", "mary@mail.com"], 
  ["John", "johnnybravo@mail.com"]
]

KEY INSIGHTS:
- UnionFind successfully groups emails that belong to the same person by connecting them through shared emails
- The first three John accounts share "johnsmith@mail.com", so they get merged into one account
- The fourth John account has no shared emails, so it remains separate
- Mary's account has no shared emails with anyone, so it remains separate
- Final result has 3 accounts instead of original 4, with the first account containing all merged emails sorted alphabetically
*/