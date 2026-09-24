import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
================================================================================
PROBLEM
--------------------------------------------------------------------------------
Merge user contacts where email or phone overlaps.
Each merged group represents ONE unique user.

Example:
Contact 0: [a@gmail.com, 123]
Contact 1: [b@gmail.com, 123]
Contact 2: [c@gmail.com, 999]

Result:
Group 1: Contact 0 + Contact 1
Group 2: Contact 2

================================================================================
INTERVIEW FLOW (WHAT I SAY OUT LOUD)
--------------------------------------------------------------------------------
Step 1: Clarify
- Overlap by email OR phone means same user
- Transitive merging is allowed
- Output groups of merged contacts

Step 2: Observation
- This is a connected-components problem
- Each identifier acts as an edge between contacts

Step 3: Data Structure
- Union-Find (Disjoint Set Union)
- HashMaps to map identifier → contact index

Step 4: Complexity Goal
- Near O(N)
- Handle large datasets efficiently

================================================================================
*/

class Contact {
    String name;
    Set<String> emails;
    Set<String> phones;

    Contact(String name, Set<String> emails, Set<String> phones) {
        this.name = name;
        this.emails = emails;
        this.phones = phones;
    }
}

/* =============================================================================
   UNION FIND (DISJOINT SET)
============================================================================= */
class UnionFind {

    private final int[] parent;
    private final int[] rank;

    UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // path compression
        }
        return parent[x];
    }

    void union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA == rootB) return;

        // union by rank
        if (rank[rootA] < rank[rootB]) {
            parent[rootA] = rootB;
        } else if (rank[rootA] > rank[rootB]) {
            parent[rootB] = rootA;
        } else {
            parent[rootB] = rootA;
            rank[rootA]++;
        }
    }
}

/* =============================================================================
   MAIN SOLUTION
============================================================================= */
class ContactMerger {

    public static List<Set<String>> mergeContacts(List<Contact> contacts) {

        int n = contacts.size();
        UnionFind uf = new UnionFind(n);

        // identifier → contact index
        Map<String, Integer> emailMap = new HashMap<>();
        Map<String, Integer> phoneMap = new HashMap<>();

        /*
         * STEP 1:
         * Union contacts that share emails or phones
         */
        for (int i = 0; i < n; i++) {
            Contact c = contacts.get(i);

            for (String email : c.emails) {
                if (emailMap.containsKey(email)) {
                    uf.union(i, emailMap.get(email));
                } else {
                    emailMap.put(email, i);
                }
            }

            for (String phone : c.phones) {
                if (phoneMap.containsKey(phone)) {
                    uf.union(i, phoneMap.get(phone));
                } else {
                    phoneMap.put(phone, i);
                }
            }
        }

        /*
         * STEP 2:
         * Group contacts by root parent
         */
        Map<Integer, Set<String>> merged = new HashMap<>();

        for (int i = 0; i < n; i++) {
            int root = uf.find(i);
            merged.putIfAbsent(root, new HashSet<>());

            Contact c = contacts.get(i);
            merged.get(root).addAll(c.emails);
            merged.get(root).addAll(c.phones);
        }

        return new ArrayList<>(merged.values());
    }

    /*
     * DEMO
     */
    public static void main(String[] args) {

        List<Contact> contacts = List.of(
            new Contact("A",
                Set.of("a@gmail.com"),
                Set.of("123")),
            new Contact("B",
                Set.of("b@gmail.com"),
                Set.of("123")),
            new Contact("C",
                Set.of("c@gmail.com"),
                Set.of("999"))
        );

        List<Set<String>> result = mergeContacts(contacts);

        for (Set<String> group : result) {
            System.out.println(group);
        }
    }
}

/*
================================================================================
TIME & SPACE COMPLEXITY (INTERVIEW ANSWER)
--------------------------------------------------------------------------------
Let:
N = number of contacts
E = total number of emails
P = total number of phones

Union-Find operations:
- Almost O(1) (amortized inverse Ackermann)

Time:
- O(N + E + P)

Space:
- O(N + E + P)

================================================================================
WHY UNION-FIND IS IDEAL
--------------------------------------------------------------------------------
- Handles transitive merges
- Very fast
- Cleaner than DFS on huge graphs

================================================================================
ONE-LINE SUMMARY (MEMORIZE)
--------------------------------------------------------------------------------
"Contact merging is a connected-components problem solved efficiently using
Union-Find with identifiers acting as edges."

================================================================================
*/
