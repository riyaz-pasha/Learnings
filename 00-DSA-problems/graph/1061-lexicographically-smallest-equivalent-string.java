import java.util.ArrayList;
import java.util.List;

/*
 * You are given two strings of the same length s1 and s2 and a string baseStr.
 * We say s1[i] and s2[i] are equivalent characters.
 * For example, if s1 = "abc" and s2 = "cde", then we have 'a' == 'c', 'b' ==
 * 'd', and 'c' == 'e'.
 * Equivalent characters follow the usual rules of any equivalence relation:
 * 
 * Reflexivity: 'a' == 'a'.
 * Symmetry: 'a' == 'b' implies 'b' == 'a'.
 * Transitivity: 'a' == 'b' and 'b' == 'c' implies 'a' == 'c'.
 * For example, given the equivalency information from s1 = "abc" and s2 =
 * "cde", "acd" and "aab" are equivalent strings of baseStr = "eed", and "aab"
 * is the lexicographically smallest equivalent string of baseStr.
 * 
 * Return the lexicographically smallest equivalent string of baseStr by using
 * the equivalency information from s1 and s2.
 * 
 * Example 1:
 * Input: s1 = "parker", s2 = "morris", baseStr = "parser"
 * Output: "makkek"
 * Explanation: Based on the equivalency information in s1 and s2, we can group
 * their characters as [m,p], [a,o], [k,r,s], [e,i].
 * The characters in each group are equivalent and sorted in lexicographical
 * order.
 * So the answer is "makkek".
 * 
 * Example 2:
 * Input: s1 = "hello", s2 = "world", baseStr = "hold"
 * Output: "hdld"
 * Explanation: Based on the equivalency information in s1 and s2, we can group
 * their characters as [h,w], [d,e,o], [l,r].
 * So only the second letter 'o' in baseStr is changed to 'd', the answer is
 * "hdld".
 * 
 * Example 3:
 * Input: s1 = "leetcode", s2 = "programs", baseStr = "sourcecode"
 * Output: "aauaaaaada"
 * Explanation: We group the equivalent characters in s1 and s2 as
 * [a,o,e,r,s,c], [l,p], [g,t] and [d,m], thus all letters in baseStr except 'u'
 * and 'd' are transformed to 'a', the answer is "aauaaaaada".
 */

// SOLUTION 1: Union-Find with Lexicographically Smallest Root
class Solution {

    public String smallestEquivalentString(String s1, String s2, String baseStr) {
        // Union-Find parent array for 26 lowercase letters
        int[] parent = new int[26];

        // Initialize each character as its own parent
        for (int i = 0; i < 26; i++) {
            parent[i] = i;
        }

        // Process equivalence relationships
        for (int i = 0; i < s1.length(); i++) {
            union(parent, s1.charAt(i) - 'a', s2.charAt(i) - 'a');
        }

        // Build result string
        StringBuilder result = new StringBuilder();
        for (char c : baseStr.toCharArray()) {
            // Find the lexicographically smallest character in the same group
            int root = find(parent, c - 'a');
            result.append((char) ('a' + root));
        }

        return result.toString();
    }

    private int find(int[] parent, int x) {
        if (parent[x] != x) {
            parent[x] = find(parent, parent[x]); // Path compression
        }
        return parent[x];
    }

    private void union(int[] parent, int x, int y) {
        int rootX = find(parent, x);
        int rootY = find(parent, y);

        if (rootX != rootY) {
            // Always make the smaller character the root (lexicographically smallest)
            if (rootX < rootY) {
                parent[rootY] = rootX;
            } else {
                parent[rootX] = rootY;
            }
        }
    }
}

// SOLUTION 2: Union-Find with Explicit Root Mapping
class SolutionWithMapping {

    public String smallestEquivalentString(String s1, String s2, String baseStr) {
        UnionFind uf = new UnionFind();

        // Build equivalence relationships
        for (int i = 0; i < s1.length(); i++) {
            uf.union(s1.charAt(i), s2.charAt(i));
        }

        // Transform baseStr using the smallest equivalent character
        StringBuilder result = new StringBuilder();
        for (char c : baseStr.toCharArray()) {
            result.append(uf.getSmallestInGroup(c));
        }

        return result.toString();
    }

    class UnionFind {
        private int[] parent = new int[26];

        public UnionFind() {
            for (int i = 0; i < 26; i++) {
                parent[i] = i;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        public void union(char a, char b) {
            int rootA = find(a - 'a');
            int rootB = find(b - 'a');

            if (rootA != rootB) {
                // Union by making smaller character the root
                if (rootA < rootB) {
                    parent[rootB] = rootA;
                } else {
                    parent[rootA] = rootB;
                }
            }
        }

        public char getSmallestInGroup(char c) {
            int root = find(c - 'a');
            return (char) ('a' + root);
        }
    }
}

// SOLUTION 3: DFS with Adjacency List
class SolutionDFS {

    public String smallestEquivalentString(String s1, String s2, String baseStr) {
        // Build adjacency list for equivalence relationships
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < 26; i++) {
            graph.add(new ArrayList<>());
        }

        // Add bidirectional edges for equivalences
        for (int i = 0; i < s1.length(); i++) {
            int a = s1.charAt(i) - 'a';
            int b = s2.charAt(i) - 'a';
            graph.get(a).add(b);
            graph.get(b).add(a);
        }

        // For each character, find the smallest equivalent character
        char[] smallestEquivalent = new char[26];
        for (int i = 0; i < 26; i++) {
            smallestEquivalent[i] = findSmallestInComponent(graph, i);
        }

        // Build result
        StringBuilder result = new StringBuilder();
        for (char c : baseStr.toCharArray()) {
            result.append(smallestEquivalent[c - 'a']);
        }

        return result.toString();
    }

    private char findSmallestInComponent(List<List<Integer>> graph, int start) {
        boolean[] visited = new boolean[26];
        int smallest = start;

        dfs(graph, start, visited, new int[] { smallest });

        return (char) ('a' + smallest);
    }

    private void dfs(List<List<Integer>> graph, int node, boolean[] visited, int[] smallest) {
        visited[node] = true;
        smallest[0] = Math.min(smallest[0], node);

        for (int neighbor : graph.get(node)) {
            if (!visited[neighbor]) {
                dfs(graph, neighbor, visited, smallest);
            }
        }
    }

}

// SOLUTION 4: Optimized Union-Find with Character Mapping
class SolutionOptimized {

    public String smallestEquivalentString(String s1, String s2, String baseStr) {
        // Direct character mapping approach
        char[] representative = new char[26];

        // Initialize each character as its own representative
        for (int i = 0; i < 26; i++) {
            representative[i] = (char) ('a' + i);
        }

        // Process equivalence relationships
        for (int i = 0; i < s1.length(); i++) {
            union(representative, s1.charAt(i), s2.charAt(i));
        }

        // Ensure path compression for all characters
        for (int i = 0; i < 26; i++) {
            find(representative, (char) ('a' + i));
        }

        // Build result
        StringBuilder result = new StringBuilder();
        for (char c : baseStr.toCharArray()) {
            result.append(find(representative, c));
        }

        return result.toString();
    }

    private char find(char[] representative, char c) {
        if (representative[c - 'a'] != c) {
            representative[c - 'a'] = find(representative, representative[c - 'a']);
        }
        return representative[c - 'a'];
    }

    private void union(char[] representative, char a, char b) {
        char rootA = find(representative, a);
        char rootB = find(representative, b);

        if (rootA != rootB) {
            // Make the lexicographically smaller character the root
            if (rootA < rootB) {
                representative[rootB - 'a'] = rootA;
            } else {
                representative[rootA - 'a'] = rootB;
            }
        }
    }

}

/*
 * DETAILED EXAMPLE WALKTHROUGH:
 * Input: s1 = "parker", s2 = "morris", baseStr = "parser"
 * 
 * STEP 1: Initialize Union-Find
 * parent =
 * [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25]
 * [a,b,c,d,e,f,g,h,i,j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z]
 * 
 * STEP 2: Process equivalence relationships
 * i=0: union('p'(15), 'm'(12)) -> parent[15] = 12 (m < p)
 * i=1: union('a'(0), 'o'(14)) -> parent[14] = 0 (a < o)
 * i=2: union('r'(17), 'r'(17)) -> no change (same character)
 * i=3: union('k'(10), 'r'(17)) -> parent[17] = 10 (k < r)
 * i=4: union('e'(4), 'i'(8)) -> parent[8] = 4 (e < i)
 * i=5: union('r'(17), 's'(18)) -> find(17)=10, find(18)=18, parent[18] = 10 (k
 * < s)
 * 
 * Final parent array after path compression:
 * - Group [a,o]: root = 0 (a)
 * - Group [e,i]: root = 4 (e)
 * - Group [k,r,s]: root = 10 (k)
 * - Group [m,p]: root = 12 (m)
 * 
 * STEP 3: Transform baseStr = "parser"
 * p -> find(15) = 12 -> 'm'
 * a -> find(0) = 0 -> 'a'
 * r -> find(17) = 10 -> 'k'
 * s -> find(18) = 10 -> 'k'
 * e -> find(4) = 4 -> 'e'
 * r -> find(17) = 10 -> 'k'
 * 
 * Result: "makkek"
 * 
 * COMPLEXITY ANALYSIS:
 * Time Complexity: O(n + m × α(26)) ≈ O(n + m)
 * - n = length of s1/s2 for building equivalence relationships
 * - m = length of baseStr for transformation
 * - α is inverse Ackermann function (practically constant)
 * 
 * Space Complexity: O(1)
 * - Fixed size array for 26 lowercase letters
 * - StringBuilder for result (could be considered O(m))
 * 
 * KEY INSIGHTS:
 * 1. Union-Find naturally handles transitive equivalence relationships
 * 2. Always union by making the lexicographically smaller character the root
 * 3. Path compression ensures efficient lookups
 * 4. Each equivalence class is represented by its smallest character
 */
