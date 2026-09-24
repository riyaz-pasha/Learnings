/**
 * ============================================================================
 * RECONSTRUCT ITINERARY - STUDY NOTES
 * ============================================================================
 *
 * 1. CLARIFYING QUESTIONS (To ask the interviewer)
 * ------------------------------------------------
 * Q: Are there multiple identical tickets (e.g., two flights from JFK to LHR)?
 *    -> Why: Determines if our graph representation needs to handle parallel edges (multigraph).
 * Q: Is it guaranteed that an itinerary exists utilizing EVERY ticket exactly once?
 *    -> Why: Confirms we are looking for a true Eulerian Path and don't need to return "No solution" or handle disjoint graphs.
 * Q: Can there be cycles in the flight paths?
 *    -> Why: Clarifies graph structure. A naive Depth-First Search might get trapped in an infinite loop if edges aren't strictly consumed.
 * Q: If two destinations are identical in lexical order, does it matter which one we pick first?
 *    -> Why: Confirms that identical tickets are interchangeable.
 * 
 * 
 * 2. THE REASONING JOURNEY
 * ------------------------------------------------
 * The Core Challenge: We need to traverse a directed graph and consume EVERY edge exactly once (an Eulerian Path), 
 * starting at "JFK". Because we must pick the lexicographically smallest path when branching, a greedy choice 
 * might lead us into a dead-end terminal airport BEFORE we've used all our tickets, stranding us.
 * 
 * Approach 1: Greedy DFS with Backtracking
 * - What I'd naturally try: Build an adjacency list where destinations are sorted. Start at JFK, pick the 
 *   first (smallest) destination, remove the ticket, and recurse. If I reach an airport with no outgoing flights 
 *   but I haven't used all tickets, I undo the choice (backtrack) and try the next lexicographical destination.
 * - Why it works: It exhaustively searches all valid paths in lexicographical order. The first one that consumes 
 *   all edges is guaranteed to be the lexicographically smallest valid itinerary.
 * - Why it's too slow: Backtracking discards massive amounts of work. If we make a wrong greedy choice early on, 
 *   we might traverse hundreds of flights perfectly, hit a dead end, and have to unwind the entire journey just 
 *   to swap one early ticket.
 * - Time Complexity: O(E^d) — because in the worst case (a dense graph with many dead ends), we explore an 
 *   exponential number of invalid path permutations before finding the one that uses all E edges.
 * - Space Complexity: O(V + E) — because we store the graph O(E) and the recursion stack can go O(E) deep.
 * 
 * Approach 2: Hierholzer's Algorithm for Eulerian Paths (The Optimum)
 * - What work is being repeated: Exploring valid sub-cycles and backtracking out of them just because they were 
 *   explored in the "wrong" order relative to a dead-end.
 * - What property removes the bottleneck: The graph has a guaranteed Eulerian path. In such a graph, if you get 
 *   stuck at a dead-end airport, it absolutely MUST be the final destination of the entire itinerary! 
 *   Instead of backtracking and saying "I shouldn't have come here yet", we can say "I will process this node 
 *   LAST". We do a Post-Order DFS: traverse all outgoing edges first, and only add the current airport to our 
 *   itinerary list when it has NO MORE outgoing edges.
 * - What I'd try: Build an adjacency list using a `PriorityQueue` for destinations (to handle lexical sorting 
 *   and O(log E) removals). DFS from "JFK". While the current airport has tickets, `poll()` the smallest one 
 *   and recurse. When the while loop finishes, push the airport to the *front* of the final itinerary list.
 * - Time Complexity: O(E log E) — because we process exactly E edges. Inserting and polling each edge from 
 *   a PriorityQueue takes O(log E) time. The traversal itself touches each edge once.
 * - Space Complexity: O(V + E) — because we store V airports and E edges in the hash map, and the DFS 
 *   recursion stack will go exactly E frames deep in the worst case. This is lean enough to be optimal.
 * - Decision: Approach 2 is exactly what I'd write in an interview. It's concise, relies on a beautiful 
 *   graph theory concept, and eliminates the nasty exponential time of backtracking.
 * 
 * 
 * 3. EDGE CASES
 * ------------------------------------------------
 * - The Early Dead-End Trap: JFK goes to A (dead end) and B (cycle back to JFK). Lexicographically, A is first. 
 *   If we add to our list pre-order, we fail. Post-order DFS handles this perfectly by bottoming out at A, 
 *   putting A at the end of the itinerary, and then seamlessly processing the B cycle.
 * - Duplicate Tickets: Multiple `["JFK", "LHR"]` tickets. A PriorityQueue handles duplicates naturally and 
 *   will just pop them sequentially.
 * - A Single Massive Cycle: e.g., JFK -> A -> B -> JFK. Post-order will unwind perfectly back to the start.
 * 
 * 
 * 4. KEY INSIGHT, DIAGRAM & DRY RUN
 * ------------------------------------------------
 * Key Insight: "Dead ends belong at the end." 
 * By using a Post-Order traversal, we let the DFS drill down until it gets stuck. Because a valid path using 
 * all tickets is guaranteed, getting stuck means we've successfully found the final node of the trip. As the 
 * recursion unwinds, we prepend the nodes, naturally arranging the cycles *before* the dead end.
 * 
 * ASCII Diagram (The Early Dead-End Trap):
 * Tickets: [JFK, NRT], [JFK, KUL], [NRT, JFK]
 * Lexical Priority at JFK: KUL (then) NRT.
 * 
 *      +----(2)----+
 *      v           |
 *     JFK --(3)--> NRT
 *      |
 *     (1)
 *      v
 *     KUL (Dead End!)
 * 
 * Dry Run (Hierholzer's Post-Order DFS):
 * Graph: 
 * JFK -> [KUL, NRT] (PriorityQueue orders KUL first)
 * NRT -> [JFK]
 * KUL -> []
 * 
 * dfs("JFK"):
 *   - Polls "KUL". Calls dfs("KUL").
 *     - dfs("KUL"): No outgoing edges. 
 *     - Add "KUL" to front of itinerary. -> List: [KUL]
 *   - Polls "NRT". Calls dfs("NRT").
 *     - dfs("NRT"): Polls "JFK". Calls dfs("JFK").
 *       - dfs("JFK"): No more outgoing edges.
 *       - Add "JFK" to front of itinerary. -> List: [JFK, KUL]
 *     - Add "NRT" to front of itinerary. -> List: [NRT, JFK, KUL]
 *   - Add "JFK" to front of itinerary. -> List: [JFK, NRT, JFK, KUL]
 * 
 * Result: ["JFK", "NRT", "JFK", "KUL"]. (Notice how KUL, the lexical favorite but logical dead-end, 
 * was gracefully pushed to the back!)
 * 
 * Pitfalls:
 * - Sorting a standard `List` and using `.remove(0)`. In Java, removing from the front of an `ArrayList` 
 *   is O(E). This bumps the time complexity up to O(E^2). Always use a `PriorityQueue` or sort descending 
 *   and remove from the back.
 * - Pre-order traversal. Adding the airport to the list *before* exploring its children requires manual 
 *   backtracking to fix dead-end traps, entirely defeating the elegance of the algorithm.
 * 
 * Pattern Recognition: 
 * - When you see: "Use EVERY edge exactly once" in a graph.
 * - Think: Eulerian Path / Hierholzer's Algorithm.
 * - Transfers to: "Valid Arrangement of Pairs", "Crack the Safe".
 * 
 * Interview Script:
 * "Since we need to use every ticket exactly once, this is fundamentally an Eulerian Path problem. If we 
 * just use greedy DFS, we might follow a lexicographically smaller path into a dead end too early, forcing 
 * expensive backtracking. Instead, I'll use Hierholzer's algorithm. I'll map each airport to a PriorityQueue 
 * of destinations to naturally handle lexical sorting. Then, I'll use a post-order DFS: I'll exhaust all 
 * outgoing flights from an airport before adding that airport to the front of my final itinerary. This 
 * guarantees that dead-ends are safely pushed to the end of the trip. That gives us O(E log E) time and O(E) space."
 * 
 * 
 * 5. FOLLOW-UPS
 * ------------------------------------------------
 * F1: What if a valid itinerary is NOT guaranteed? How do we know an Eulerian Path exists?
 *  -> Before traversing, we would calculate the in-degree and out-degree of every node. An Eulerian Path 
 *     exists if and only if at most one node has (out-degree - in-degree == 1) [the start], at most one node 
 *     has (in-degree - out-degree == 1) [the end], and all other nodes have equal in/out degrees.
 * 
 * F2: What if E is absolutely massive (e.g., 10 million) and DFS causes a StackOverflowError?
 *  -> We can convert the recursive DFS into an iterative one using an explicit `Stack<String>`. 
 *     We peek the stack, and if the top airport has tickets, we pop a ticket and push the destination. 
 *     If it has no tickets, we pop the airport from the stack and add it to our final itinerary.
 * 
 * 
 * 6. SUMMARY
 * ------------------------------------------------
 * - Core pattern: Eulerian Path via Hierholzer's Algorithm (Post-Order DFS).
 * - Key observation: Getting stuck in a graph with a guaranteed Eulerian path means you are at the final destination.
 * - Memorize: Post-order DFS + PriorityQueue adjacency list.
 * - Most common trap: Using `ArrayList.remove(0)`, ruining the time complexity.
 */

import java.util.*;

public class ReconstructItinerary {

    /**
     * OPTIMAL APPROACH: Hierholzer's Algorithm (Post-Order DFS)
     * Time Complexity: O(E log E) — E is the number of tickets. Building the graph takes O(E log E) due to PriorityQueue insertions. The DFS visits each edge exactly once, popping from the PQ in O(log E) time.
     * Space Complexity: O(V + E) — The HashMap stores V airports and E edges. The recursion stack reaches a maximum depth of E+1.
     */
    public List<String> findItinerary(List<List<String>> tickets) {
        // Step 1: Build the Adjacency List
        // Map: Source Airport -> PriorityQueue of Destination Airports
        // PriorityQueue automatically sorts the destinations lexicographically.
        Map<String, PriorityQueue<String>> flightGraph = new HashMap<>();
        
        for (List<String> ticket : tickets) {
            String from = ticket.get(0);
            String to = ticket.get(1);
            
            flightGraph.computeIfAbsent(from, k -> new PriorityQueue<>()).offer(to);
        }
        
        // LinkedList allows O(1) insertions at the front (addFirst), perfect for post-order appending.
        LinkedList<String> itinerary = new LinkedList<>();
        
        // Step 2: Traverse using Post-Order DFS
        dfs("JFK", flightGraph, itinerary);
        
        return itinerary;
    }

    private void dfs(String currentAirport, Map<String, PriorityQueue<String>> flightGraph, LinkedList<String> itinerary) {
        // Get the queue of next available destinations from this airport
        PriorityQueue<String> nextAirports = flightGraph.get(currentAirport);
        
        // While there are still outgoing flights from this airport...
        while (nextAirports != null && !nextAirports.isEmpty()) {
            // Greedily pick the lexicographically smallest destination, REMOVE it, and recurse.
            String nextDestination = nextAirports.poll();
            dfs(nextDestination, flightGraph, itinerary);
        }
        
        // POST-ORDER ACTION: 
        // We only add the current airport to the itinerary AFTER exploring all its outgoing flights.
        // By adding to the front, the very first airport to "bottom out" (the dead end) becomes the LAST element in the final list.
        itinerary.addFirst(currentAirport);
    }

    // ============================================================================
    // MAIN METHOD - TEST CASES
    // ============================================================================
    public static void main(String[] args) {
        ReconstructItinerary solver = new ReconstructItinerary();

        System.out.println("--- Testing Reconstruct Itinerary ---");

        // Test Case 1: Simple linear path
        List<List<String>> tickets1 = Arrays.asList(
            Arrays.asList("MUC", "LHR"),
            Arrays.asList("JFK", "MUC"),
            Arrays.asList("SFO", "SJC"),
            Arrays.asList("LHR", "SFO")
        );
        // Expected: ["JFK", "MUC", "LHR", "SFO", "SJC"]
        System.out.println("Test Case 1: " + solver.findItinerary(tickets1));

        // Test Case 2: Lexicographical Tie-breaking
        List<List<String>> tickets2 = Arrays.asList(
            Arrays.asList("JFK", "SFO"),
            Arrays.asList("JFK", "ATL"),
            Arrays.asList("SFO", "ATL"),
            Arrays.asList("ATL", "JFK"),
            Arrays.asList("ATL", "SFO")
        );
        // Expected: ["JFK", "ATL", "JFK", "SFO", "ATL", "SFO"]
        // (JFK -> ATL is smaller than JFK -> SFO)
        System.out.println("Test Case 2: " + solver.findItinerary(tickets2));

        // Test Case 3: The Early Dead-End Trap (From Study Notes Dry Run)
        List<List<String>> tickets3 = Arrays.asList(
            Arrays.asList("JFK", "NRT"),
            Arrays.asList("JFK", "KUL"),
            Arrays.asList("NRT", "JFK")
        );
        // Expected: ["JFK", "NRT", "JFK", "KUL"]
        // If greedy pre-order was used, it would try JFK -> KUL and get stuck.
        System.out.println("Test Case 3: " + solver.findItinerary(tickets3));

        // Test Case 4: Duplicate Tickets
        List<List<String>> tickets4 = Arrays.asList(
            Arrays.asList("JFK", "LHR"),
            Arrays.asList("JFK", "LHR"),
            Arrays.asList("LHR", "JFK")
        );
        // Expected: ["JFK", "LHR", "JFK", "LHR"]
        System.out.println("Test Case 4: " + solver.findItinerary(tickets4));
    }
}

