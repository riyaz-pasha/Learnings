import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/*
 * You are given an array of strings equations that represent relationships
 * between variables where each string equations[i] is of length 4 and takes one
 * of two different forms: "xi==yi" or "xi!=yi".Here, xi and yi are lowercase
 * letters (not necessarily different) that represent one-letter variable names.
 * 
 * Return true if it is possible to assign integers to variable names so as to
 * satisfy all the given equations, or false otherwise.
 * 
 * Example 1:
 * Input: equations = ["a==b","b!=a"]
 * Output: false
 * Explanation: If we assign say, a = 1 and b = 1, then the first equation is
 * satisfied, but not the second.
 * There is no way to assign the variables to satisfy both equations.
 *
 * Example 2:
 * Input: equations = ["b==a","a==b"]
 * Output: true
 * Explanation: We could assign a = 1 and b = 1 to satisfy both equations.
 * 
 */

// SOLUTION 1: Union-Find with Array (Most Efficient)
class Solution {
    public boolean equationsPossible(String[] equations) {
        // Union-Find array for 26 lowercase letters
        int[] parent = new int[26];
        
        // Initialize each variable as its own parent
        for (int i = 0; i < 26; i++) {
            parent[i] = i;
        }
        
        // Process all equality equations first
        for (String eq : equations) {
            if (eq.charAt(1) == '=') { // equality equation
                char x = eq.charAt(0);
                char y = eq.charAt(3);
                union(parent, x - 'a', y - 'a');
            }
        }
        
        // Check all inequality equations
        for (String eq : equations) {
            if (eq.charAt(1) == '!') { // inequality equation
                char x = eq.charAt(0);
                char y = eq.charAt(3);
                // If x and y are in same group, inequality is violated
                if (find(parent, x - 'a') == find(parent, y - 'a')) {
                    return false;
                }
            }
        }
        
        return true;
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
            parent[rootX] = rootY;
        }
    }
}

// SOLUTION 2: Union-Find with Rank Optimization
class SolutionWithRank {
    public boolean equationsPossible(String[] equations) {
        UnionFind uf = new UnionFind();
        
        // Process equality equations first
        for (String eq : equations) {
            if (eq.charAt(1) == '=') {
                char x = eq.charAt(0);
                char y = eq.charAt(3);
                uf.union(x, y);
            }
        }
        
        // Check inequality equations
        for (String eq : equations) {
            if (eq.charAt(1) == '!') {
                char x = eq.charAt(0);
                char y = eq.charAt(3);
                if (uf.find(x) == uf.find(y)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    class UnionFind {
        private int[] parent = new int[26];
        private int[] rank = new int[26];
        
        public UnionFind() {
            for (int i = 0; i < 26; i++) {
                parent[i] = i;
                rank[i] = 0;
            }
        }
        
        public int find(char c) {
            return find(c - 'a');
        }
        
        private int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }
        
        public void union(char a, char b) {
            union(a - 'a', b - 'a');
        }
        
        private void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);
            
            if (rootX != rootY) {
                // Union by rank
                if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
            }
        }
    }
}

// SOLUTION 3: DFS/Graph-based Approach
class SolutionDFS {
    public boolean equationsPossible(String[] equations) {
        // Build adjacency list for equality relationships
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < 26; i++) {
            graph.add(new ArrayList<>());
        }
        
        // Add edges for equality equations
        for (String eq : equations) {
            if (eq.charAt(1) == '=') {
                int x = eq.charAt(0) - 'a';
                int y = eq.charAt(3) - 'a';
                graph.get(x).add(y);
                graph.get(y).add(x);
            }
        }
        
        // Check inequality equations using DFS
        for (String eq : equations) {
            if (eq.charAt(1) == '!') {
                int x = eq.charAt(0) - 'a';
                int y = eq.charAt(3) - 'a';
                
                // If x and y are connected by equality relationships
                if (isConnected(graph, x, y)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    private boolean isConnected(List<List<Integer>> graph, int start, int target) {
        if (start == target) return true;
        
        boolean[] visited = new boolean[26];
        return dfs(graph, start, target, visited);
    }
    
    private boolean dfs(List<List<Integer>> graph, int current, int target, boolean[] visited) {
        if (current == target) return true;
        
        visited[current] = true;
        
        for (int neighbor : graph.get(current)) {
            if (!visited[neighbor] && dfs(graph, neighbor, target, visited)) {
                return true;
            }
        }
        
        return false;
    }
}

// SOLUTION 4: Two-Pass with Separate Lists
class SolutionTwoPass {
    public boolean equationsPossible(String[] equations) {
        // Separate equations into equality and inequality
        List<String> equalityEqs = new ArrayList<>();
        List<String> inequalityEqs = new ArrayList<>();
        
        for (String eq : equations) {
            if (eq.charAt(1) == '=') {
                equalityEqs.add(eq);
            } else {
                inequalityEqs.add(eq);
            }
        }
        
        // Union-Find setup
        int[] parent = new int[26];
        for (int i = 0; i < 26; i++) {
            parent[i] = i;
        }
        
        // Process equality equations
        for (String eq : equalityEqs) {
            int x = eq.charAt(0) - 'a';
            int y = eq.charAt(3) - 'a';
            union(parent, x, y);
        }
        
        // Check inequality equations
        for (String eq : inequalityEqs) {
            int x = eq.charAt(0) - 'a';
            int y = eq.charAt(3) - 'a';
            if (find(parent, x) == find(parent, y)) {
                return false;
            }
        }
        
        return true;
    }
    
    private int find(int[] parent, int x) {
        if (parent[x] != x) {
            parent[x] = find(parent, parent[x]);
        }
        return parent[x];
    }
    
    private void union(int[] parent, int x, int y) {
        parent[find(parent, x)] = find(parent, y);
    }
}

/*
COMPLEXITY ANALYSIS:

Time Complexity: O(n * α(26)) ≈ O(n)
- n is the number of equations
- α is the inverse Ackermann function (practically constant for small inputs)
- We process each equation twice in worst case

Space Complexity: O(1)
- Fixed size array of 26 elements for Union-Find
- No additional space that grows with input size

ALGORITHM EXPLANATION:

1. **Two-Phase Approach**:
   - Phase 1: Process all equality equations to build connected components
   - Phase 2: Check if any inequality equation violates the connected components

2. **Union-Find Strategy**:
   - Variables that must be equal are grouped together
   - If an inequality equation connects two variables in the same group, it's impossible

3. **Why This Works**:
   - Equality is transitive: if a==b and b==c, then a==c
   - Union-Find efficiently maintains these transitive relationships
   - Any inequality between variables in the same equivalence class creates a contradiction

EXAMPLE WALKTHROUGH:
Input: ["a==b", "b!=a"]

1. Process equality: union('a', 'b') - now a and b are in same group
2. Process inequality: find('a') == find('b') - both return same root
3. Since they're in same group but have inequality constraint, return false
*/

class EqualityEquationSatisfiability {
    
    public boolean equationsPossible(String[] equations) {
        // Build graph for "==" relationships
        Map<Character, Set<Character>> graph = new HashMap<>();

        for (String eq : equations) {
            if (eq.charAt(1) == '=') {
                char x = eq.charAt(0);
                char y = eq.charAt(3);
                graph.computeIfAbsent(x, k -> new HashSet<>()).add(y);
                graph.computeIfAbsent(y, k -> new HashSet<>()).add(x);
            }
        }

        // For "!=" equations, check if both are in the same component
        for (String eq : equations) {
            if (eq.charAt(1) == '!') {
                char x = eq.charAt(0);
                char y = eq.charAt(3);
                if (x == y)
                    return false; // e.g., "a!=a" is always false
                if (isConnected(x, y, graph))
                    return false;
            }
        }

        return true;
    }

    // BFS to check if x and y are connected in the "==" graph
    private boolean isConnected(char x, char y, Map<Character, Set<Character>> graph) {
        Set<Character> visited = new HashSet<>();
        Queue<Character> queue = new LinkedList<>();
        queue.offer(x);
        visited.add(x);

        while (!queue.isEmpty()) {
            char current = queue.poll();
            if (current == y)
                return true;
            for (char neighbor : graph.getOrDefault(current, new HashSet<>())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }

        return false;
    }

}

class EqualityEquationSatisfiability2 {

    public boolean equationsPossible(String[] equations) {
        UnionFind uf = new UnionFind(26);
        for (String equation : equations) {
            if (equation.charAt(1) == '=') {
                uf.union(equation.charAt(0) - 'a', equation.charAt(3) - 'a');
            }
        }
        for (String equation : equations) {
            if (equation.charAt(1) == '!') {
                if (uf.find(equation.charAt(0) - 'a') == uf.find(equation.charAt(3) - 'a')) {
                    return false;
                }
            }
        }
        return true;
    }

    private class UnionFind {
        private final int[] parent;

        UnionFind(int n) {
            parent = new int[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]);
            }
            return parent[x];
        }

        public void union(int x, int y) {
            parent[find(y)] = find(x);
        }
    }

}
