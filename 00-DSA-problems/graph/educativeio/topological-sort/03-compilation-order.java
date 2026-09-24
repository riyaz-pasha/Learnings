import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * CLASS COMPILATION ORDER (Topological Sort with Characters)
 * ============================================================================
 * 
 * STATEMENT:
 * There are a total of 'n' classes labeled with the English alphabet (A, B, C...).
 * Some classes are dependent on other classes for compilation. 
 * For example, if class B extends class A, then B has a dependency on A. 
 * Therefore, A must be compiled before B.
 * Given a list of dependency pairs, find the order in which the classes should 
 * be compiled.
 * 
 * Constraints:
 * - Class name should be an uppercase character.
 * - 0 <= dependencies.length <= 676
 * - dependencies[i].length == 2
 * - All dependency pairs are unique.
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * We need to determine a sequence in which to compile software classes such that 
 * every class is compiled only *after* all the classes it depends on have been 
 * compiled. This is a classic "Topological Sorting" problem on a Directed Acyclic 
 * Graph (DAG). Here, nodes are characters ('A', 'B', etc.), and a directed edge 
 * from A -> B means B depends on A (A must be compiled first). If there's a circular 
 * dependency (e.g., A depends on B, and B depends on A), compilation is impossible.
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER (4-10):
 * 1. Q: How exactly is the dependency pair formatted? Does [B, A] mean B depends on A?
 *    A: Yes, we will assume [Dependent, Prerequisite] for this implementation (like LeetCode).
 * 2. Q: Are there any classes that aren't listed in the dependencies array?
 *    A: The problem doesn't give a separate 'n' list of nodes. We will assume we only 
 *       need to compile the classes that appear in the dependencies array.
 * 3. Q: What should we return if a circular dependency (cycle) exists?
 *    A: Return an empty array or empty list to signify it's impossible.
 * 4. Q: What if the dependencies array is empty?
 *    A: Return an empty list, as there are no classes to compile based on the input.
 * 5. Q: If multiple valid compilation orders exist, does it matter which one I return?
 *    A: Usually no, returning any valid topological sort is acceptable.
 * 6. Q: Can I assume all inputs are valid uppercase English letters?
 *    A: Yes, per the constraints.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Acknowledge Graph Nature: State that this is a dependency resolution problem, 
 *    which perfectly maps to Topological Sorting on a DAG.
 * 2. Node & Edge Mapping: Clarify what the nodes (Classes) and edges (Dependencies) are.
 *    Say: "An edge from Prerequisite -> Dependent means Prerequisite must come first."
 * 3. Discuss Approaches:
 *    - Approach 1: Kahn's Algorithm (BFS) using In-Degree counting. Very intuitive.
 *    - Approach 2: Depth First Search (DFS) using 3-color cycle detection.
 * 4. Complexity: Time is O(V + E), Space is O(V + E), where V is the unique classes 
 *    and E is the number of dependencies.
 * 
 * ============================================================================
 */
public class CompilationOrder {

    /**
     * ========================================================================
     * SOLUTION 1: KAHN'S ALGORITHM (Breadth-First Search / In-Degree)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * In-degree represents the number of prerequisites a class currently has.
     * If a class has an in-degree of 0, it means it has NO prerequisites, so we 
     * can safely compile it right now.
     * Once compiled, we simulate making it available to other classes by looking 
     * at all classes that depend on it and reducing their in-degree by 1. 
     * If any of those drop to 0, they are now ready to compile!
     * 
     * KEY OBSERVATIONS:
     * - We need a set to track all unique characters to ensure we process everything.
     * - If we finish the BFS queue but haven't compiled every unique class we found,
     *   it means the remaining classes are stuck in a circular dependency.
     * 
     * VISUAL TRACING:
     * dependencies = [['B', 'A'], ['C', 'B'], ['D', 'B']]
     * Meaning: B depends on A; C depends on B; D depends on B.
     * 
     * Graph (Prereq -> Dependent):
     *       -> [C]
     *      /
     * [A] -> [B] 
     *      \
     *       -> [D]
     * 
     * In-degrees: A:0, B:1, C:1, D:1
     * 1. Queue: [A]. Pop A. Result: [A]. 
     *    Decrease neighbors of A (which is B). B in-degree -> 0. Queue: [B].
     * 2. Queue: [B]. Pop B. Result: [A, B].
     *    Decrease neighbors of B (C and D). C in-degree -> 0, D in-degree -> 0. Queue: [C, D].
     * 3. Queue: [C, D]. Pop C. Result: [A, B, C]. (no neighbors to decrease)
     * 4. Queue: [D]. Pop D. Result: [A, B, C, D].
     */
    public List<Character> findCompilationOrderBFS(char[][] dependencies) {
        Map<Character, Integer> inDegree = new HashMap<>();
        Map<Character, List<Character>> adj = new HashMap<>();
        
        // 1. Initialize the graph and in-degrees for all unique classes
        for (char[] dep : dependencies) {
            char dependent = dep[0];
            char prereq = dep[1];
            
            inDegree.putIfAbsent(dependent, 0);
            inDegree.putIfAbsent(prereq, 0);
            adj.putIfAbsent(dependent, new ArrayList<>());
            adj.putIfAbsent(prereq, new ArrayList<>());
        }
        
        // 2. Build the graph (prereq -> dependent)
        for (char[] dep : dependencies) {
            char dependent = dep[0];
            char prereq = dep[1];
            adj.get(prereq).add(dependent);
            inDegree.put(dependent, inDegree.get(dependent) + 1);
        }
        
        // 3. Find all classes with 0 in-degree to start
        Queue<Character> queue = new LinkedList<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        List<Character> compilationOrder = new ArrayList<>();
        
        // 4. Process the graph
        while (!queue.isEmpty()) {
            char current = queue.poll();
            compilationOrder.add(current);
            
            for (char neighbor : adj.get(current)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        // 5. Check for cycles
        if (compilationOrder.size() != inDegree.size()) {
            return new ArrayList<>(); // Cycle detected, return empty
        }
        
        return compilationOrder;
    }

    /**
     * ========================================================================
     * SOLUTION 2: DEPTH-FIRST SEARCH (DFS / Graph Coloring)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * Imagine exploring a maze of dependencies. If we keep following the chain 
     * of "what depends on what", we eventually reach a class that nothing else 
     * depends on (a leaf node). Since it's at the end of the chain, it should 
     * be compiled LAST.
     * DFS naturally reaches the end of paths first. By adding a class to our 
     * result list ONLY AFTER we have fully explored all its dependents, we get 
     * the reversed compilation order. We just reverse it at the end.
     * 
     * Cycle Detection uses a Map of States:
     * UNVISITED (not in map)
     * VISITING (1) - currently exploring its path.
     * VISITED (2) - safely explored.
     */
    public List<Character> findCompilationOrderDFS(char[][] dependencies) {
        Map<Character, List<Character>> adj = new HashMap<>();
        Set<Character> uniqueClasses = new HashSet<>();
        
        for (char[] dep : dependencies) {
            char dependent = dep[0];
            char prereq = dep[1];
            uniqueClasses.add(dependent);
            uniqueClasses.add(prereq);
            adj.putIfAbsent(prereq, new ArrayList<>());
            adj.get(prereq).add(dependent);
        }
        
        // State Map: 1 = Visiting, 2 = Visited
        Map<Character, Integer> states = new HashMap<>();
        List<Character> result = new ArrayList<>();
        
        // Check all unique classes
        for (char cls : uniqueClasses) {
            if (!states.containsKey(cls)) {
                if (hasCycleDFS(cls, adj, states, result)) {
                    return new ArrayList<>(); // Return empty list on cycle
                }
            }
        }
        
        // Since we added nodes at the end of their DFS, we must reverse 
        // to get the correct topological order (Prerequisites first).
        Collections.reverse(result);
        return result;
    }
    
    private boolean hasCycleDFS(char current, Map<Character, List<Character>> adj, 
                                Map<Character, Integer> states, List<Character> result) {
        // If we hit a node that is currently in our recursion stack, it's a cycle
        if (states.getOrDefault(current, 0) == 1) return true;
        // If it's already safely processed, skip it
        if (states.getOrDefault(current, 0) == 2) return false;
        
        // Mark as VISITING
        states.put(current, 1);
        
        if (adj.containsKey(current)) {
            for (char neighbor : adj.get(current)) {
                if (hasCycleDFS(neighbor, adj, states, result)) {
                    return true;
                }
            }
        }
        
        // Mark as VISITED and add to result (post-order)
        states.put(current, 2);
        result.add(current);
        return false;
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING
     * ========================================================================
     * Using modern Java 14+ Records and Streams.
     */
    
    record TestCase(char[][] dependencies, boolean expectSuccess, String description) {}

    public static void main(String[] args) {
        CompilationOrder solver = new CompilationOrder();

        var testCases = List.of(
            new TestCase(
                new char[][]{{'B', 'A'}, {'C', 'B'}}, 
                true, 
                "Linear Dependency (A -> B -> C)"
            ),
            new TestCase(
                new char[][]{{'B', 'A'}, {'C', 'A'}, {'D', 'B'}, {'D', 'C'}}, 
                true, 
                "Diamond Dependency (A -> B,C -> D)"
            ),
            new TestCase(
                new char[][]{{'A', 'B'}, {'B', 'A'}}, 
                false, 
                "Direct Cycle (A <-> B)"
            ),
            new TestCase(
                new char[][]{{'C', 'A'}, {'B', 'C'}, {'A', 'B'}}, 
                false, 
                "Triangular Cycle (A -> C -> B -> A)"
            ),
            new TestCase(
                new char[][]{}, 
                true, 
                "Empty Dependencies"
            )
        );

        System.out.println("Running test cases for Class Compilation Order...");
        System.out.println("-".repeat(70));

        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            var resultBFS = solver.findCompilationOrderBFS(tc.dependencies());
            var resultDFS = solver.findCompilationOrderDFS(tc.dependencies());
            
            boolean bfsPassed = tc.expectSuccess() ? !resultBFS.isEmpty() || tc.dependencies().length == 0 : resultBFS.isEmpty();
            boolean dfsPassed = tc.expectSuccess() ? !resultDFS.isEmpty() || tc.dependencies().length == 0 : resultDFS.isEmpty();
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("BFS Result: %s | Passed? %s\n", resultBFS, bfsPassed ? "✅" : "❌");
            System.out.printf("DFS Result: %s | Passed? %s\n", resultDFS, dfsPassed ? "✅" : "❌");
            System.out.println("-".repeat(70));
        });
    }
}
