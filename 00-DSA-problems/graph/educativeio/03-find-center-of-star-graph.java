/*
 * ==========================================================================================
 *                                FIND CENTER OF STAR GRAPH
 * ==========================================================================================
 *
 * 1. PROBLEM RESTATEMENT IN SIMPLE TERMS
 * ------------------------------------------------------------------------------------------
 * You are given a collection of edges that form a "star graph". 
 * A star graph is a specific type of graph where exactly ONE special node (the center) 
 * is connected to every single other node in the graph, and the other nodes are ONLY 
 * connected to the center (they don't connect to each other).
 * Your task is to look at the list of edges and figure out which node is this central node.
 *
 *
 * 2. IDEA, INTUITION, AND KEY OBSERVATIONS
 * ------------------------------------------------------------------------------------------
 * - Star Graph Property: The central node must have an edge connecting it to every other 
 *   node. This means the central node will appear in EVERY single edge in our list.
 * - Minimum Effort Required: If a node appears in every edge, it MUST appear in the first 
 *   edge and the second edge. 
 * - The first edge gives us two "candidate" nodes for the center. 
 * - The second edge also gives us two nodes. 
 * - Because only the central node connects to multiple other nodes, the single node that 
 *   appears in BOTH the first and second edges must be the center.
 * - We don't even need to look at the rest of the graph!
 *
 *
 * 3. HOW TO IDENTIFY THE RIGHT APPROACH
 * ------------------------------------------------------------------------------------------
 * - The problem explicitly states the input IS a valid star graph. 
 * - Recognizing guarantees in a problem statement is key. Because it's guaranteed to be a 
 *   star graph, we don't need to validate it. 
 * - When looking for properties common to *all* elements (like appearing in all edges), 
 *   check if checking a small subset (just 2 edges) mathematically guarantees the answer.
 *
 *
 * 4. CLARIFYING QUESTIONS TO ASK THE INTERVIEWER (And why they matter)
 * ------------------------------------------------------------------------------------------
 * Q1: "Are we guaranteed that the input is ALWAYS a valid star graph?"
 *     Why: If the graph could be invalid (e.g., a line graph or disjoint components), 
 *          checking just the first two edges would yield a wrong answer. We'd have to 
 *          validate the entire graph. The prompt guarantees validity.
 * Q2: "Can there be fewer than 3 nodes (meaning fewer than 2 edges)?"
 *     Why: If there is only 1 edge (e.g., [[1, 2]]), both 1 and 2 are technically 
 *          centers. The constraints say N >= 3, so we are guaranteed at least 2 edges, 
 *          avoiding this edge case entirely.
 * Q3: "Can node values be 0 or negative?"
 *     Why: If we were using an array for degree counting (Approach 1), negative values 
 *          or 0 would cause out-of-bounds exceptions unless we offset them. Constraints 
 *          say 1 <= ui, vi <= n.
 * Q4: "Do we need to validate if the graph has cycles?"
 *     Why: In general graphs, cycles complicate traversal. But by definition, a star graph 
 *          is a tree, meaning it has exactly N-1 edges and no cycles.
 *
 *
 * 5. ASCII VISUALS / TRACING
 * ------------------------------------------------------------------------------------------
 * Let edges = [[1, 2], [5, 1], [1, 3], [1, 4]]
 *
 * Graph View:
 *       2
 *       |
 *   5 - 1 - 3
 *       |
 *       4
 *
 * TRACING THE OPTIMAL O(1) APPROACH:
 * - Edge 0: [1, 2] -> Candidates for center: 1 or 2.
 * - Edge 1: [5, 1] -> Nodes in this edge: 5 and 1.
 * - Intersection Check: 
 *     Is 1 (from Edge 0) inside Edge 1? Yes! (It matches the second element '1')
 *     Therefore, 1 is the center.
 *
 *
 * 6. EXAMPLES AND IMPORTANT EDGE CASES
 * ------------------------------------------------------------------------------------------
 * - Example 1: [[1, 2], [2, 3], [4, 2]]
 *   Edge 0 is [1, 2], Edge 1 is [2, 3]. '2' is shared. Return 2.
 * - Common failure case for beginners: Iterating over every edge and building a complex 
 *   Adjacency List or HashMap. It works, but wastes time and memory for a problem 
 *   that can be solved in 3 lines of code.
 *
 *
 * 7. COMMON MISTAKES AND PITFALLS
 * ------------------------------------------------------------------------------------------
 * 1. Over-engineering: Treating this like a standard graph traversal (BFS/DFS) problem. 
 *    There's nothing to traverse. 
 * 2. Wasting space: Creating an array of size N+1 to count frequencies. It uses O(N) memory 
 *    when O(1) is completely sufficient.
 *
 *
 * 8. INTERVIEW STRATEGY: HOW TO APPROACH AND EXPLAIN
 * ------------------------------------------------------------------------------------------
 * 1. Start by mentioning the brute force/standard graph approach (Approach 1). 
 *    Say: "Usually, to find the most connected node, I would count the degree of every 
 *    node using an array, which takes O(N) time and O(N) space."
 * 2. Pivot to the insight: "However, since we are guaranteed this is a star graph, the 
 *    center node must be in every single edge. Thus, it must be the common node between 
 *    just the first two edges."
 * 3. Write Approach 2 (Optimal O(1)). It shows strong analytical skills, math intuition, 
 *    and prevents you from writing unnecessary boilerplate code.
 * ==========================================================================================
 */

public class FindCenterOfStarGraph {

    /*
     * APPROACH 1: DEGREE COUNTING (Sub-optimal but standard)
     * --------------------------------------------------------------------------------------
     * Idea: Traverse all edges. For every edge [u, v], increment the degree of u and v.
     * The center node is the one with a degree of n - 1 (since it connects to all other nodes).
     * 
     * Complexity:
     * - Time: O(N) where N is the number of nodes. We loop through all N-1 edges.
     * - Space: O(N) for the degree array.
     * 
     * Note: I am providing this for completeness, but you should skip this in a real 
     * interview in favor of Approach 2.
     */
    public int findCenterDegreeCounting(int[][] edges) {
        int n = edges.length + 1; // Since edges.length == n - 1
        int[] degree = new int[n + 1]; // 1-indexed based on constraints

        for (int[] edge : edges) {
            degree[edge[0]]++;
            degree[edge[1]]++;
        }

        for (int i = 1; i <= n; i++) {
            if (degree[i] == n - 1) {
                return i;
            }
        }
        return -1; 
    }

    /*
     * APPROACH 2: CONSTANT TIME CHECK (Optimal & Recommended)
     * --------------------------------------------------------------------------------------
     * Idea: We only need to examine edges[0] and edges[1]. 
     * Let edges[0] = [u1, v1] and edges[1] = [u2, v2].
     * If u1 equals u2 or v2, then u1 is the center.
     * Otherwise, by elimination, v1 MUST be the center.
     *
     * Complexity:
     * - Time: O(1) - We perform a maximum of two comparisons, regardless of graph size.
     * - Space: O(1) - No extra data structures are used.
     */
    public int findCenterOptimal(int[][] edges) {
        // Extract the two nodes from the very first edge
        int u1 = edges[0][0];
        int v1 = edges[0][1];

        // Extract the two nodes from the second edge
        int u2 = edges[1][0];
        int v2 = edges[1][1];

        // If the first node of the first edge matches either node in the second edge,
        // it must be the center node.
        if (u1 == u2 || u1 == v2) {
            return u1;
        }

        // If it doesn't match, the OTHER node in the first edge MUST be the center.
        return v1;
    }

    /*
     * ==========================================================================================
     * INTERVIEWER FOLLOW-UP QUESTIONS & ANSWERS
     * ==========================================================================================
     * 
     * F1: "What if the graph is NOT guaranteed to be a valid star graph? How would you find 
     *      out if it is one, and return the center if so?"
     * Ans: I would use the Degree Counting approach (Approach 1). I would count the degrees 
     *      of all nodes. Then I would verify two things:
     *      1. Exactly ONE node has a degree of N - 1.
     *      2. ALL OTHER nodes have a degree of exactly 1.
     *      This takes O(N) time and O(N) space.
     * 
     * F2: "Can we write the optimal solution in a single line of code?"
     * Ans: Yes, using a ternary operator:
     *      return (edges[0][0] == edges[1][0] || edges[0][0] == edges[1][1]) ? edges[0][0] : edges[0][1];
     *      (I would mention this, but write out the readable version above unless asked, 
     *      as clean readability is highly valued in enterprise software).
     *
     * F3: "What if the edges array is generated dynamically and we receive edges as a stream?
     *      How quickly can we identify the center?"
     * Ans: As soon as we receive exactly two edges, we can immediately identify the center 
     *      using the O(1) logic. We don't need to wait for the rest of the stream.
     * ==========================================================================================
     */

    // --------------------------------------------------------------------------------------
    // TEST RUNNER CODE
    // --------------------------------------------------------------------------------------
    public static void main(String[] args) {
        FindCenterOfStarGraph solution = new FindCenterOfStarGraph();

        // Test Case 1: Standard case
        int[][] edges1 = {{1, 2}, {2, 3}, {4, 2}};
        System.out.println("Test Case 1 (Expected 2): " + solution.findCenterOptimal(edges1));

        // Test Case 2: Center is the first element of the first edge
        int[][] edges2 = {{1, 2}, {5, 1}, {1, 3}, {1, 4}};
        System.out.println("Test Case 2 (Expected 1): " + solution.findCenterOptimal(edges2));

        // Test Case 3: Center is the second element of the first edge, 
        //              and matches the second element of the second edge.
        int[][] edges3 = {{10, 5}, {3, 5}, {7, 5}, {5, 2}};
        System.out.println("Test Case 3 (Expected 5): " + solution.findCenterOptimal(edges3));
        
        // Verifying Approach 1 just in case
        System.out.println("Test Case 3 using Degree Counting (Expected 5): " 
                           + solution.findCenterDegreeCounting(edges3));
    }
}

