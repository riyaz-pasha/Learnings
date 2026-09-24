/**
 * ============================================================================
 * 0. PROBLEM STATEMENT
 * ============================================================================
 * You have a lock with 4 circular wheels (digits '0'-'9').
 * The lock starts at "0000". Each move rotates one wheel 1 step forward or backward.
 * You are given a list of `deadends` (if the lock reaches one, it's stuck).
 * Return the minimum moves to reach a `target`, or -1 if impossible.
 * 
 * Constraints:
 * - deadends length: [1, 500]
 * - target and deadends are 4-digit strings.
 * - target is not in deadends.
 * 
 * ============================================================================
 * 1. CLARIFYING QUESTIONS
 * ============================================================================
 * - "Can '0000' be in the deadends list?"
 *   Prevents edge-case bugs: if the start is a deadend, the answer is immediately -1.
 * - "What if the target is '0000'?"
 *   Clarifies whether 0 moves is a valid answer (it is).
 * - "Are we trying to optimize for time, or is memory severely constrained?"
 *   Helps decide if standard BFS is enough, or if we need Bidirectional BFS to save queue space.
 * - "Do we need to return the actual sequence of turns, or just the count?"
 *   Changes the state we store (just counting integers vs. keeping a `Map` of parent pointers).
 * 
 * ============================================================================
 * 2. THE REASONING JOURNEY
 * ============================================================================
 * [Binding Constraint] 
 * We are looking for the *minimum* number of moves in a space of changing states. 
 * This means we are finding the shortest path in an unweighted graph where nodes are 
 * the 4-digit strings and edges are the 1-step rotations.
 * 
 * --- APPROACH 1: Depth-First Search (DFS) (The Intuitive but Wrong Try) ---
 * 1. What I'd naturally try: Start at "0000". Try turning the first dial up, then recursively 
 *    keep turning dials until I hit the target. If I hit a deadend or loop, backtrack.
 * 2. Why it "works": It theoretically explores the lock's state space.
 * 3. Why it falls over: DFS doesn't find the *shortest* path naturally. It will dive deep into 
 *    a 9,999-move sequence before it finds the 2-move sequence. To find the true minimum, DFS 
 *    must exhaustively search *every* non-cyclic path in the 10,000-node graph.
 * 4. What work is repeated: DFS will revisit the same lock states via infinitely many 
 *    different meandering paths, requiring massive backtracking.
 * 5. Time Complexity: O(V!) theoretically — because without a strict level boundary, DFS explores 
 *    all permutations of paths (astronomically slow).
 * 6. Space Complexity: O(V) — because the recursion stack will hold up to all 10,000 possible 
 *    states (where V is total states) in the worst-case meandering path.
 * 
 * --- APPROACH 2: Breadth-First Search (BFS) (The Standard Fix) ---
 * 1. What I'd try next: Instead of going deep, explore level by level. Try all 8 possible 1-step 
 *    moves from "0000" (Level 1). Then all valid moves from those (Level 2). First time we see 
 *    the target, we are guaranteed it's the shortest path.
 * 2. Why it works: BFS naturally expands outward like ripples in a pond, guaranteeing shortest path.
 * 3. Why it's still slightly costly: The "ripple" grows exponentially. Level 1 has 8 nodes, 
 *    Level 2 has 64, Level 3 has 512. The queue gets very wide before reaching the target.
 * 4. Time Complexity: O(N) — where N is the total number of lock states (10^4). For each state, 
 *    we generate 8 neighbors, doing O(1) string/char operations. Max 10,000 nodes visited.
 * 5. Space Complexity: O(N) — because the BFS Queue and the Visited Set will eventually store 
 *    a significant fraction of the 10,000 possible states.
 * 
 * [The Core Observation for Optimal]
 * If I know the exact target (e.g., "7531"), I don't just have to search blindly outward from "0000". 
 * I can drop a pebble at "0000" AND a pebble at "7531", and watch the ripples expand from both 
 * sides. When the two ripples intersect, I've found the shortest path.
 * 
 * --- APPROACH 3: Bidirectional BFS (The Optimal Way) ---
 * 1. How it works: Maintain two sets of states: one expanding from the `start`, one from the `target`. 
 *    In each step, always expand the *smaller* set to minimize the exponential fan-out. If a generated 
 *    neighbor is found in the opposite set, we're done.
 * 2. Time Complexity: O(N) — theoretically the worst case visits all states. But practically, it visits 
 *    O(b^(d/2)) states instead of O(b^d) (where b=8 branching factor, d=depth). It cuts the search 
 *    time by a massive constant factor.
 * 3. Space Complexity: O(N) — theoretically, but again, the peak frontier size (memory footprint) 
 *    is drastically smaller because we never let a single ripple reach the full depth of the graph.
 * 
 * [Which one I'd write in an interview]
 * I would write Approach 2 (Standard BFS) first to ensure I have a working, bug-free solution, 
 * because state-generation in Java takes some typing. If I finish early, or if the interviewer 
 * explicitly asks for optimization, I would upgrade it to Approach 3 (Bidirectional BFS). I have 
 * provided both below.
 * 
 * ============================================================================
 * 3. EDGE CASES
 * ============================================================================
 * - `deadends` contains "0000": Fast fail, return -1.
 * - Target is "0000": Return 0 immediately.
 * - Unreachable target: The target is encased in a "wall" of deadends (e.g. target "0002" with 
 *   deadends "0001", "0012", "0102", "1002", etc.). The Queue exhausts itself, returns -1.
 * 
 * ============================================================================
 * 4. KEY INSIGHT, DIAGRAMS & DRY RUN
 * ============================================================================
 * [Key Insight]
 * Don't use a separate `visited` set and a `deadends` array. Load the `deadends` directly 
 * into your `visited` HashSet before you even start! A deadend is logically identical to 
 * a state you've already visited—you aren't allowed to explore from it.
 * 
 * [Dry Run of a tricky path]
 * Target: "0002", Deadend: "0001" (Blocking the direct path)
 * 
 * Init: Queue = ["0000"], Visited = ["0001", "0000"], Moves = 0
 * 
 * Iteration 1 (Moves = 0):
 *   - Pop "0000".
 *   - Generate neighbors: 
 *     Wheel 0: 1000, 9000
 *     Wheel 1: 0100, 0900
 *     Wheel 2: 0010, 0090
 *     Wheel 3: 0001 (Visited/Dead!), 0009
 *   - Queue becomes: [1000, 9000, 0100, 0900, 0010, 0090, 0009]
 * 
 * Iteration 2 (Moves = 1):
 *   - Pop "1000". Target not found.
 *   - Generates neighbors: 2000, 0000(Vis), 1100, 1900, 1010, 1090, 1001, 1009
 *   - Notice `1001` is generated. It bypassed the deadend!
 * 
 * (Fast forward...)
 * The shortest path routing around the deadend will be:
 * "0000" -> "1000" -> "1001" -> "1002" -> "0002". (4 moves).
 * 
 * [Pitfalls]
 * - Using Integer arrays or Math operations to roll wheels: e.g. `(val + 1) % 10`. While clever, 
 *   converting back and forth between String/Integer in Java creates massive overhead. Manipulating 
 *   `char[]` is vastly faster and avoids formatting bugs like dropping leading zeroes.
 * 
 * [Pattern Recognition]
 * When you see: "Minimum steps to reach state X from state Y" with a small state space.
 * Think: Graph BFS. States = Nodes. Valid transitions = Edges.
 * 
 * ============================================================================
 * 5. FOLLOW-UPS
 * ============================================================================
 * Q: What if the lock had N wheels instead of 4?
 * A: The complexity scales to O(10^N). For large N, standard BFS is unworkable. We would need 
 *    A* Search with a heuristic function, such as the sum of minimum cyclic distances for each 
 *    wheel to the target (Manhattan distance for circular locks).
 * 
 * Q: How would you print the actual path of moves, not just the count?
 * A: Instead of a `Set<String> visited`, I would use a `Map<String, String> parentMap` where the 
 *    key is the child state and the value is the state that generated it. Once the target is hit, 
 *    I trace backwards through the map to "0000" and reverse the resulting list.
 * 
 * ============================================================================
 * 6. JAVA CODE
 * ============================================================================
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class OpenTheLock {

    /**
     * APPROACH 2: Standard BFS (The bedrock, safe solution)
     */
    public static int openLockBFS(String[] deadends, String target) {
        // By dumping deadends straight into the 'visited' set, we kill two birds 
        // with one stone: we avoid cycles AND we bounce off deadends naturally.
        // e.g., visited = ["0201", "0101", "0102", "1212", "2002"]
        Set<String> visited = new HashSet<>(Arrays.asList(deadends));

        // Edge case: Starting state is a deadend.
        if (visited.contains("0000")) return -1;
        // Edge case: We are already at the target.
        if ("0000".equals(target)) return 0;

        // Standard BFS Queue
        Queue<String> queue = new LinkedList<>();
        queue.offer("0000");
        visited.add("0000"); // Mark visited to prevent enqueueing it again

        int moves = 0;

        while (!queue.isEmpty()) {
            // Snapshot the size to process strictly level-by-level
            int levelSize = queue.size();

            for (int i = 0; i < levelSize; i++) {
                String current = queue.poll();

                // If we hit the target, because it's BFS, this is guaranteed 
                // to be the shortest path.
                if (current.equals(target)) {
                    return moves;
                }

                // Generate and enqueue all 8 possible 1-step moves
                for (String nextState : getNextStates(current)) {
                    // Only add if it's not a deadend and hasn't been visited
                    if (!visited.contains(nextState)) {
                        visited.add(nextState);
                        queue.offer(nextState);
                    }
                }
            }
            // Finished expanding all nodes at distance 'moves'. Step outward.
            moves++;
        }

        // Queue exhausted without finding target
        return -1;
    }

    /**
     * APPROACH 3: Bidirectional BFS (The Optimal / Senior Solution)
     */
    public static int openLockBidirectionalBFS(String[] deadends, String target) {
        Set<String> visited = new HashSet<>(Arrays.asList(deadends));
        
        if (visited.contains("0000")) return -1;
        if ("0000".equals(target)) return 0;

        // Instead of a queue, we use Sets because we need fast O(1) intersection checks
        // 'beginSet' ripples outward from "0000"
        Set<String> beginSet = new HashSet<>();
        // 'endSet' ripples outward from the target
        Set<String> endSet = new HashSet<>();
        
        beginSet.add("0000");
        endSet.add(target);
        visited.add("0000"); // Note: Do NOT add target to visited yet, or we'll block the intersection!

        int moves = 0;

        while (!beginSet.isEmpty() && !endSet.isEmpty()) {
            // OPTIMIZATION: Always expand the smaller frontier set. 
            // This drastically cuts down the exponential branching factor.
            if (beginSet.size() > endSet.size()) {
                Set<String> temp = beginSet;
                beginSet = endSet;
                endSet = temp;
            }

            // 'nextSet' collects the nodes for the next level of the current expansion
            Set<String> nextSet = new HashSet<>();

            for (String current : beginSet) {
                // Generate the 8 neighbors
                for (String nextState : getNextStates(current)) {
                    // INTERSECTION FOUND! The two expanding ripples have touched.
                    if (endSet.contains(nextState)) {
                        return moves + 1;
                    }

                    if (!visited.contains(nextState)) {
                        visited.add(nextState);
                        nextSet.add(nextState);
                    }
                }
            }
            
            // Move forward one step in our ripple expansion
            beginSet = nextSet;
            moves++;
        }

        return -1;
    }

    /**
     * Helper to generate the 8 possible string permutations for a single lock state.
     * Uses char[] manipulation to avoid heavy String concatenation overhead.
     */
    private static String[] getNextStates(String lockState) {
        String[] nextStates = new String[8];
        int index = 0;

        // Iterate through all 4 wheels
        for (int i = 0; i < 4; i++) {
            char[] chars = lockState.toCharArray();
            char c = chars[i];

            // 1. Roll the wheel UP (forward)
            // Ternary operator handles the cyclic wrap-around (9 -> 0)
            chars[i] = (c == '9') ? '0' : (char) (c + 1);
            nextStates[index++] = new String(chars);

            // 2. Roll the wheel DOWN (backward)
            // Handles the cyclic wrap-around (0 -> 9)
            chars[i] = (c == '0') ? '9' : (char) (c - 1);
            nextStates[index++] = new String(chars);
        }

        return nextStates;
    }

    // ============================================================================
    // TESTING & CROSS-CHECKING
    // ============================================================================
    public static void main(String[] args) {
        // Test 1: Standard case from problem description
        String[] deadends1 = {"0201","0101","0102","1212","2002"};
        String target1 = "0202";
        System.out.println("Test 1 (Standard BFS): " + openLockBFS(deadends1, target1)); // Exp: 6
        System.out.println("Test 1 (Bi-BFS):       " + openLockBidirectionalBFS(deadends1, target1));

        // Test 2: Deadend at start
        String[] deadends2 = {"0000"};
        String target2 = "8888";
        System.out.println("\nTest 2 (Start deadend BFS): " + openLockBFS(deadends2, target2)); // Exp: -1
        System.out.println("Test 2 (Start deadend Bi-BFS): " + openLockBidirectionalBFS(deadends2, target2));

        // Test 3: The routing-around-obstacle test (from Dry Run)
        String[] deadends3 = {"0001"};
        String target3 = "0002";
        System.out.println("\nTest 3 (Obstacle BFS): " + openLockBFS(deadends3, target3)); // Exp: 4
        System.out.println("Test 3 (Obstacle Bi-BFS): " + openLockBidirectionalBFS(deadends3, target3));

        // Test 4: Impossible path (Target completely enclosed)
        String[] deadends4 = {"0001", "0010", "0100", "1000", "0009", "0090", "0900", "9000"};
        String target4 = "0002";
        System.out.println("\nTest 4 (Enclosed BFS): " + openLockBFS(deadends4, target4)); // Exp: -1
        System.out.println("Test 4 (Enclosed Bi-BFS): " + openLockBidirectionalBFS(deadends4, target4));
    }
}

/**
 * ============================================================================
 * 7. SUMMARY
 * ============================================================================
 * - Core pattern: Shortest Path in Unweighted Graph = Breadth-First Search.
 * - Key observation: 'deadends' are functionally identical to 'visited' nodes.
 * - Most common trap: Doing Math/Modulo on Integers to generate neighbors. Using 
 *   char[] manipulation is significantly safer and faster in Java.
 * - Mental trigger: "Minimum moves" + "Small discrete state space" -> BFS / Bidirectional BFS.
 */


import java.util.*;

/**
 * Problem: Open the Lock (Bi-directional BFS)
 *
 * Core Idea:
 * -------------------------------------------------------
 * Instead of searching from only one side (0000 → target),
 * we search from BOTH sides:
 *
 *   START ("0000")  <----->  TARGET
 *
 * Why?
 * - Reduces search space exponentially
 * - Faster than normal BFS for large graphs
 *
 * -------------------------------------------------------
 * KEY CONCEPT:
 * - At each step, expand the SMALLER frontier set
 * - This keeps the branching factor minimal
 *
 * -------------------------------------------------------
 * GRAPH MODEL:
 * - Each 4-digit string = node
 * - Each move (rotate one wheel ±1) = edge
 * - Total nodes = 10^4 (0000 → 9999)
 */
public class OpenLockBiDirectional {

    public int openLock(String[] deadends, String target) {

        // 🚫 Deadend states (cannot be visited)
        Set<String> dead = new HashSet<>(List.of(deadends));

        // 🚫 If starting point itself is blocked → impossible
        if (dead.contains("0000")) return -1;

        // 🎯 If target is already start
        if ("0000".equals(target)) return 0;

        // 🔁 Two frontiers for bi-directional BFS
        Set<String> begin = new HashSet<>();
        Set<String> end = new HashSet<>();

        // 🧠 Visited states (shared across both directions)
        Set<String> visited = new HashSet<>();

        begin.add("0000");   // start side
        end.add(target);     // target side

        int steps = 0;       // number of moves taken

        // 🔁 Continue while both sides have nodes
        while (!begin.isEmpty() && !end.isEmpty()) {

            // 🔥 Optimization: always expand the smaller frontier
            if (begin.size() > end.size()) {
                Set<String> temp = begin;
                begin = end;
                end = temp;
            }

            // Next level states after expansion
            Set<String> nextLevel = new HashSet<>();

            // 🔄 Expand current frontier
            for (String current : begin) {

                // 🚫 Skip dead states
                if (dead.contains(current)) continue;

                // 🧠 Mark visited
                visited.add(current);

                // 🔄 Generate all 8 possible moves
                for (String neighbor : getNeighbors(current)) {

                    // 🎯 CRITICAL: Check if both searches meet
                    if (end.contains(neighbor)) {
                        return steps + 1;
                    }

                    // ✅ Add valid unvisited states
                    if (!visited.contains(neighbor) && !dead.contains(neighbor)) {
                        nextLevel.add(neighbor);
                    }
                }
            }

            // Move to next level
            begin = nextLevel;

            // Increase distance
            steps++;
        }

        // ❌ No path found
        return -1;
    }

    /**
     * Generate all possible next states (neighbors)
     *
     * For each of 4 wheels:
     * - Rotate forward (+1)
     * - Rotate backward (-1)
     *
     * Total neighbors = 4 * 2 = 8
     *
     * Example:
     *   "0000" →
     *   "1000", "9000", "0100", "0900", ...
     */
    private List<String> getNeighbors(String state) {

        List<String> neighbors = new ArrayList<>(8);

        char[] chars = state.toCharArray();

        // 🔁 Try changing each wheel
        for (int i = 0; i < 4; i++) {

            char original = chars[i];

            // ⬆️ Rotate forward (e.g., 9 → 0)
            chars[i] = (char) ((original - '0' + 1) % 10 + '0');
            neighbors.add(new String(chars));

            // ⬇️ Rotate backward (e.g., 0 → 9)
            chars[i] = (char) ((original - '0' + 9) % 10 + '0');
            neighbors.add(new String(chars));

            // 🔁 Restore original state for next iteration
            chars[i] = original;
        }

        return neighbors;
    }
}


import java.util.*;

/**
 * ============================================================
 * PROBLEM: OPEN THE LOCK
 * ============================================================
 *
 * You are given a lock with 4 circular wheels.
 *
 * Each wheel contains:
 *
 *     '0' '1' '2' '3' '4' '5' '6' '7' '8' '9'
 *
 * The lock starts at:
 *
 *     "0000"
 *
 * In one move, we can rotate ONE wheel by exactly ONE position.
 *
 * A wheel wraps around:
 *
 *     '9' -> '0'   when moving forward
 *
 *     '0' -> '9'   when moving backward
 *
 *
 * Example:
 *
 *     "0000"
 *
 * Rotate first wheel forward:
 *
 *     "1000"
 *
 * Rotate first wheel backward:
 *
 *     "9000"
 *
 *
 * We are also given DEADENDS.
 *
 * If we reach a deadend position, the lock becomes stuck and
 * we cannot continue from that position.
 *
 *
 * Goal:
 *
 *     Starting from "0000", reach TARGET using the minimum
 *     number of moves while avoiding all deadends.
 *
 *
 * Return:
 *
 *     Minimum number of moves
 *
 * or:
 *
 *     -1 if the target cannot be reached.
 *
 *
 * ============================================================
 * EXAMPLE
 * ============================================================
 *
 * Suppose:
 *
 *     deadends = ["0201", "0101", "0102", "1212", "2002"]
 *     target   = "0202"
 *
 *
 * We start:
 *
 *     0000
 *
 *
 * One possible path is:
 *
 *     0000
 *       |
 *       v
 *     0100
 *       |
 *       v
 *     0101
 *
 *
 * But 0101 is a deadend.
 *
 * So we CANNOT continue through it.
 *
 *
 * BFS explores all possible states level by level and avoids
 * deadends automatically.
 *
 *
 * ============================================================
 * FIRST IMPORTANT QUESTION:
 * WHY BFS?
 * ============================================================
 *
 * Every move has the SAME cost:
 *
 *     one wheel rotation = 1 move
 *
 *
 * Therefore, this problem can be represented as an
 * UNWEIGHTED GRAPH.
 *
 *
 * Each lock combination is a NODE.
 *
 * Each valid one-wheel rotation is an EDGE.
 *
 *
 * Example:
 *
 *                     1000
 *                      |
 *                      |
 *              0000 -- 0100
 *              /  \
 *          9000    0010
 *
 *
 * From "0000", we can reach 8 states:
 *
 *     1000
 *     9000
 *     0100
 *     0900
 *     0010
 *     0090
 *     0001
 *     0009
 *
 *
 * Every one of these requires exactly ONE move.
 *
 *
 * Therefore:
 *
 *     BFS
 *
 * gives us the minimum number of moves.
 *
 *
 * ============================================================
 * WHY NOT DFS?
 * ============================================================
 *
 * DFS might find:
 *
 *     0000 -> ... -> target
 *
 * but that path might not be the shortest.
 *
 *
 * Example:
 *
 *              A
 *             / \
 *            B   C
 *            |   |
 *            D   Target
 *
 *
 * DFS could find:
 *
 *     A -> B -> D -> ... -> Target
 *
 * before:
 *
 *     A -> C -> Target
 *
 * even though the second path is shorter.
 *
 *
 * BFS explores:
 *
 *     distance 0
 *     distance 1
 *     distance 2
 *     distance 3
 *     ...
 *
 *
 * Therefore, the FIRST time BFS reaches the target,
 * that distance is guaranteed to be minimum.
 *
 *
 * ============================================================
 * STATE SPACE
 * ============================================================
 *
 * There are 4 wheels.
 *
 * Each wheel has 10 possible values.
 *
 *
 * Total possible states:
 *
 *     10 * 10 * 10 * 10
 *
 *     = 10,000
 *
 *
 * So the entire state space is actually small.
 *
 *
 * Example states:
 *
 *     0000
 *     0001
 *     0002
 *     ...
 *     9999
 *
 *
 * BFS can safely explore this state space.
 *
 *
 * ============================================================
 * HOW MANY NEIGHBORS DOES EACH STATE HAVE?
 * ============================================================
 *
 * We can rotate any ONE of the 4 wheels.
 *
 * For each wheel:
 *
 *     +1
 *     -1
 *
 * Therefore:
 *
 *     4 wheels * 2 directions
 *
 *     = 8 neighbors
 *
 *
 * Example:
 *
 *                 1000
 *                   |
 *                   |
 *         9000 <-- 0000 --> 0100
 *                   |
 *                   |
 *                 0010
 *
 * Plus:
 *
 *     0009
 *
 *
 * So every state has at most 8 neighbors.
 *
 *
 * ============================================================
 * CIRCULAR WHEEL
 * ============================================================
 *
 * This is one of the most important implementation details.
 *
 *
 * For forward rotation:
 *
 *     0 -> 1
 *     1 -> 2
 *     ...
 *     8 -> 9
 *     9 -> 0
 *
 *
 * For backward rotation:
 *
 *     9 -> 8
 *     ...
 *     1 -> 0
 *     0 -> 9
 *
 *
 * We can handle this with:
 *
 *     (digit + 1) % 10
 *
 * for forward.
 *
 *
 * And:
 *
 *     (digit + 9) % 10
 *
 * for backward.
 *
 *
 * Why +9?
 *
 * Because:
 *
 *     -1 mod 10
 *
 * can be represented as:
 *
 *     +9 mod 10
 *
 *
 * Example:
 *
 *     digit = 0
 *
 *     backward:
 *
 *     (0 + 9) % 10
 *
 *     = 9
 *
 *
 * Example:
 *
 *     digit = 5
 *
 *     backward:
 *
 *     (5 + 9) % 10
 *
 *     = 14 % 10
 *
 *     = 4
 *
 *
 * ============================================================
 * DEADENDS
 * ============================================================
 *
 * Deadends are states that cannot be entered.
 *
 * Example:
 *
 *     deadends = ["0201", "0101"]
 *
 *
 * If BFS tries to reach:
 *
 *     0201
 *
 * we simply ignore that state.
 *
 *
 * We should store deadends in a HashSet:
 *
 *     Set<String> dead = new HashSet<>(Arrays.asList(deadends));
 *
 *
 * Why HashSet?
 *
 * Because checking:
 *
 *     dead.contains(state)
 *
 * is O(1) average time.
 *
 *
 * ============================================================
 * VISUAL BFS
 * ============================================================
 *
 * Let's temporarily ignore deadends.
 *
 * Starting state:
 *
 *     0000
 *
 *
 * Distance 0:
 *
 *                    0000
 *
 *
 * Distance 1:
 *
 *        1000   9000   0100   0900
 *
 *        0010   0090   0001   0009
 *
 *
 * Distance 2:
 *
 * BFS expands every valid state from distance 1.
 *
 * For example:
 *
 *     1000
 *
 * can produce:
 *
 *     2000
 *     0000
 *     1100
 *     1900
 *     1010
 *     1090
 *     1001
 *     1009
 *
 *
 * Notice:
 *
 *     0000
 *
 * has already been visited.
 *
 * We should NOT add it again.
 *
 *
 * ============================================================
 * WHY DO WE NEED A VISITED SET?
 * ============================================================
 *
 * The graph contains cycles.
 *
 *
 * Example:
 *
 *     0000
 *       |
 *       v
 *     1000
 *       |
 *       v
 *     0000
 *
 *
 * If we don't track visited states:
 *
 *     0000
 *       -> 1000
 *       -> 0000
 *       -> 1000
 *       -> 0000
 *       -> ...
 *
 *
 * BFS could keep revisiting states forever.
 *
 *
 * Therefore:
 *
 *     visited.add(state)
 *
 * prevents duplicate processing.
 *
 *
 * ============================================================
 * IMPORTANT:
 * WHEN SHOULD WE MARK A STATE VISITED?
 * ============================================================
 *
 * We mark a state visited when we ADD it to the queue,
 * not when we remove it.
 *
 *
 * Example:
 *
 *     current = 0000
 *
 * We generate:
 *
 *     1000
 *
 * Immediately:
 *
 *     visited.add("1000");
 *     queue.offer("1000");
 *
 *
 * Why?
 *
 * Because another state might also generate "1000"
 * before BFS gets around to processing it.
 *
 * Marking it when enqueuing guarantees that it is added
 * only once.
 *
 *
 * ============================================================
 * BFS LEVEL STRUCTURE
 * ============================================================
 *
 * We can process BFS level by level.
 *
 *
 * Distance 0:
 *
 *     [0000]
 *
 *
 * Distance 1:
 *
 *     [1000, 9000, 0100, 0900,
 *      0010, 0090, 0001, 0009]
 *
 *
 * Distance 2:
 *
 *     [all valid states one move away from level 1]
 *
 *
 * Therefore we can maintain:
 *
 *     moves
 *
 * and increment it after processing one complete level.
 *
 *
 * ============================================================
 * WALKTHROUGH
 * ============================================================
 *
 * Let's use a simple example:
 *
 *     deadends = []
 *     target = "0002"
 *
 *
 * Start:
 *
 *     0000
 *
 *
 * ------------------------------------------------------------
 * LEVEL 0
 * ------------------------------------------------------------
 *
 * Queue:
 *
 *     [0000]
 *
 * moves = 0
 *
 *
 * Process 0000.
 *
 * Its 8 neighbors are:
 *
 *     1000
 *     9000
 *     0100
 *     0900
 *     0010
 *     0090
 *     0001
 *     0009
 *
 *
 * None are the target.
 *
 * Add them to the queue.
 *
 *
 * Queue becomes:
 *
 *     [1000, 9000, 0100, 0900,
 *      0010, 0090, 0001, 0009]
 *
 *
 * Increment:
 *
 *     moves = 1
 *
 *
 * ------------------------------------------------------------
 * LEVEL 1
 * ------------------------------------------------------------
 *
 * We process every state that is exactly ONE move away
 * from 0000.
 *
 *
 * Eventually we process:
 *
 *     0001
 *
 *
 * From:
 *
 *     0001
 *
 * we can rotate the last wheel forward:
 *
 *     0002
 *
 *
 * `0002` is the target.
 *
 *
 * Therefore:
 *
 *     answer = 2
 *
 *
 * Path:
 *
 *     0000
 *       |
 *       | +1 on last wheel
 *       v
 *     0001
 *       |
 *       | +1 on last wheel
 *       v
 *     0002
 *
 *
 * Minimum moves:
 *
 *     2
 *
 *
 * ============================================================
 * WALKTHROUGH WITH WRAPAROUND
 * ============================================================
 *
 * Suppose:
 *
 *     target = "0009"
 *
 *
 * We can reach it in ONE move:
 *
 *     0000
 *       |
 *       | rotate last wheel backward
 *       v
 *     0009
 *
 *
 * This demonstrates:
 *
 *     0 backward -> 9
 *
 *
 * Similarly:
 *
 *     0009
 *       |
 *       | rotate last wheel forward
 *       v
 *     0000
 *
 *
 * Therefore the wheel behaves like a circle:
 *
 *
 *                0
 *             /     \
 *            1       9
 *            |       |
 *            2       8
 *            |       |
 *            3       7
 *             \     /
 *                ...
 *
 *
 * ============================================================
 * WALKTHROUGH WITH A DEADEND
 * ============================================================
 *
 * Suppose:
 *
 *     deadends = ["0001"]
 *     target = "0002"
 *
 *
 * Start:
 *
 *     0000
 *
 *
 * One possible move is:
 *
 *     0000 -> 0001
 *
 * But:
 *
 *     0001
 *
 * is a deadend.
 *
 *
 * Therefore we NEVER add it to the queue.
 *
 *
 * Conceptually:
 *
 *
 *                  0001 X
 *                 /
 *                /
 *             0000
 *                \
 *                 \
 *                  0009
 *
 *
 * BFS simply ignores the blocked node.
 *
 *
 * ============================================================
 * SPECIAL CASE: START IS A DEADEND
 * ============================================================
 *
 * The starting state is:
 *
 *     "0000"
 *
 *
 * Suppose:
 *
 *     deadends = ["0000"]
 *
 *
 * Then the lock is already stuck.
 *
 * We cannot make even one move.
 *
 * Therefore:
 *
 *     return -1
 *
 *
 * We check this BEFORE starting BFS.
 *
 *
 * ============================================================
 * SPECIAL CASE: TARGET == START
 * ============================================================
 *
 * Suppose:
 *
 *     target = "0000"
 *
 *
 * If "0000" is not a deadend, we are already at the target.
 *
 * Therefore:
 *
 *     answer = 0
 *
 *
 * This can be checked immediately.
 *
 *
 * ============================================================
 * CORRECTNESS INTUITION
 * ============================================================
 *
 * Think of every combination as a graph node.
 *
 * A one-wheel rotation creates an edge.
 *
 *
 * Example:
 *
 *     0000
 *     |
 *     +---- 1000
 *     |
 *     +---- 9000
 *     |
 *     +---- 0100
 *     |
 *     +---- 0900
 *     |
 *     +---- 0010
 *     |
 *     +---- 0090
 *     |
 *     +---- 0001
 *     |
 *     +---- 0009
 *
 *
 * Every edge costs exactly 1.
 *
 * BFS explores nodes according to their distance from
 * the starting node:
 *
 *     distance 0
 *     distance 1
 *     distance 2
 *     distance 3
 *     ...
 *
 *
 * Therefore, when BFS first encounters the target,
 * it has found a shortest path.
 *
 *
 * Deadends are simply nodes that we are not allowed to enter.
 *
 *
 * ============================================================
 * TIME COMPLEXITY
 * ============================================================
 *
 * There are at most:
 *
 *     10^4 = 10,000
 *
 * possible lock combinations.
 *
 *
 * Each state has at most:
 *
 *     8
 *
 * neighbors.
 *
 *
 * Therefore:
 *
 *     O(10,000 * 8)
 *
 * which simplifies to:
 *
 *     O(10,000)
 *
 *
 * More generally, because the number of wheels and digits
 * are fixed:
 *
 *     O(10^4)
 *
 *
 * We also build the deadend HashSet:
 *
 *     O(D)
 *
 * where D = number of deadends.
 *
 *
 * Overall:
 *
 *     O(10^4 + D)
 *
 *
 * ============================================================
 * SPACE COMPLEXITY
 * ============================================================
 *
 * We store:
 *
 *     1. deadends
 *     2. visited states
 *     3. BFS queue
 *
 *
 * There can be at most 10,000 states.
 *
 *
 * Therefore:
 *
 *     O(10^4)
 *
 * or, more generally:
 *
 *     O(number of possible states)
 *
 *
 * ============================================================
 * KEY INTERVIEW TAKEAWAYS
 * ============================================================
 *
 * 1. This is a SHORTEST PATH problem.
 *
 * 2. Every move costs exactly 1.
 *
 * 3. Therefore use BFS.
 *
 * 4. Each lock combination is a graph state.
 *
 * 5. Each state has 8 neighbors:
 *
 *        4 wheels * 2 directions
 *
 * 6. Use a HashSet for deadends.
 *
 * 7. Use a visited set to prevent cycles.
 *
 * 8. Mark visited when ENQUEUING.
 *
 * 9. Handle circular digits:
 *
 *        forward  = (digit + 1) % 10
 *        backward = (digit + 9) % 10
 *
 * 10. If "0000" is a deadend, return -1.
 *
 * 11. If target is "0000", return 0.
 *
 *
 * ============================================================
 * IMPLEMENTATION
 * ============================================================
 */
public class OpenTheLock {

    /**
     * Returns the minimum number of moves required to reach
     * target from "0000".
     *
     * Returns -1 if target cannot be reached.
     *
     * @param deadends lock combinations that cannot be entered
     * @param target target lock combination
     * @return minimum number of moves, or -1
     */
    public int openLock(String[] deadends, String target) {

        /*
         * Convert deadends into a HashSet.
         *
         * Why?
         *
         * We repeatedly need to ask:
         *
         *     "Is this state a deadend?"
         *
         * HashSet gives O(1) average lookup.
         */
        Set<String> dead = new HashSet<>(Arrays.asList(deadends));

        /*
         * If the starting state itself is blocked,
         * we cannot make any move.
         *
         * Start:
         *
         *     0000 X
         *
         * Therefore:
         *
         *     return -1
         */
        if (dead.contains("0000")) {
            return -1;
        }

        /*
         * If the target is already the starting position,
         * no moves are required.
         *
         *     0000 == target
         *
         * Answer:
         *
         *     0
         */
        if (target.equals("0000")) {
            return 0;
        }

        /*
         * BFS queue.
         *
         * Each element represents one lock state.
         */
        Queue<String> queue = new ArrayDeque<>();

        /*
         * Add starting state.
         *
         * Distance:
         *
         *     0000 -> distance 0
         */
        queue.offer("0000");

        /*
         * Track states that we have already discovered.
         *
         * Without this, we could cycle forever:
         *
         *     0000 -> 1000 -> 0000 -> 1000 -> ...
         */
        Set<String> visited = new HashSet<>();

        /*
         * Mark the starting state as visited immediately.
         */
        visited.add("0000");

        /*
         * `moves` represents the distance of the current BFS
         * level from the starting state.
         *
         * Initially:
         *
         *     0000
         *
         * is distance 0.
         */
        int moves = 0;

        /*
         * Process BFS level by level.
         */
        while (!queue.isEmpty()) {

            /*
             * Number of nodes in the current BFS level.
             *
             * Example:
             *
             * Distance 1 might contain:
             *
             *     1000
             *     9000
             *     0100
             *     ...
             *
             * We process all of them before increasing
             * `moves`.
             */
            int levelSize = queue.size();

            /*
             * Process every state at the current distance.
             */
            for (int i = 0; i < levelSize; i++) {

                /*
                 * Remove the next state from the queue.
                 */
                String current = queue.poll();

                /*
                 * If we reached the target, return the current
                 * number of moves.
                 *
                 * Because BFS processes states level by level,
                 * this is guaranteed to be the minimum distance.
                 */
                if (current.equals(target)) {
                    return moves;
                }

                /*
                 * Generate all 8 possible states:
                 *
                 *     wheel 0: +1, -1
                 *     wheel 1: +1, -1
                 *     wheel 2: +1, -1
                 *     wheel 3: +1, -1
                 */
                for (int wheel = 0; wheel < 4; wheel++) {

                    /*
                     * ------------------------------------------
                     * ROTATE CURRENT WHEEL FORWARD
                     * ------------------------------------------
                     *
                     * Example:
                     *
                     *     0000
                     *
                     * Rotate wheel 0 forward:
                     *
                     *     1000
                     *
                     *
                     * Example of wraparound:
                     *
                     *     9000
                     *
                     * Rotate wheel 0 forward:
                     *
                     *     0000
                     */
                    String next = rotate(current, wheel, 1);

                    /*
                     * Add this state if it is valid.
                     */
                    if (!dead.contains(next) && visited.add(next)) {

                        /*
                         * `visited.add(next)` returns true only if
                         * next was NOT already visited.
                         *
                         * This simultaneously:
                         *
                         *     1. checks whether we visited it
                         *     2. marks it visited
                         */
                        queue.offer(next);
                    }

                    /*
                     * ------------------------------------------
                     * ROTATE CURRENT WHEEL BACKWARD
                     * ------------------------------------------
                     *
                     * Example:
                     *
                     *     0000
                     *
                     * Rotate wheel 0 backward:
                     *
                     *     9000
                     *
                     *
                     * This handles:
                     *
                     *     0 -> 9
                     */
                    next = rotate(current, wheel, -1);

                    /*
                     * Again:
                     *
                     *     - ignore deadends
                     *     - ignore already visited states
                     *     - otherwise add to BFS queue
                     */
                    if (!dead.contains(next) && visited.add(next)) {
                        queue.offer(next);
                    }
                }
            }

            /*
             * We have finished processing all states at the
             * current distance.
             *
             * Move to the next BFS level.
             *
             * Example:
             *
             *     distance 0
             *          ↓
             *     distance 1
             *          ↓
             *     distance 2
             *          ↓
             *         ...
             */
            moves++;
        }

        /*
         * Queue became empty.
         *
         * That means BFS explored every reachable state but
         * never found the target.
         *
         * Therefore the target is unreachable.
         */
        return -1;
    }

    /**
     * Creates a new lock state by rotating ONE wheel.
     *
     * @param state current lock state
     * @param wheel index of wheel to rotate (0 to 3)
     * @param direction +1 for forward, -1 for backward
     * @return new lock state
     */
    private String rotate(String state, int wheel, int direction) {

        /*
         * Convert String to a mutable character array.
         *
         * Example:
         *
         *     "0000"
         *
         * becomes:
         *
         *     ['0', '0', '0', '0']
         */
        char[] chars = state.toCharArray();

        /*
         * Convert the selected character into a numeric digit.
         *
         * Example:
         *
         *     '5' -> 5
         */
        int digit = chars[wheel] - '0';

        /*
         * Move the digit by one position.
         *
         * FORWARD:
         *
         *     (digit + 1) % 10
         *
         *
         * BACKWARD:
         *
         *     (digit + 9) % 10
         *
         *
         * Why +9 for backward?
         *
         * Because:
         *
         *     -1 mod 10
         *
         * can be represented as:
         *
         *     +9 mod 10
         *
         *
         * Examples:
         *
         *     forward from 8:
         *
         *         (8 + 1) % 10 = 9
         *
         *
         *     forward from 9:
         *
         *         (9 + 1) % 10 = 0
         *
         *
         *     backward from 5:
         *
         *         (5 + 9) % 10 = 4
         *
         *
         *     backward from 0:
         *
         *         (0 + 9) % 10 = 9
         */
        digit = (digit + direction + 10) % 10;

        /*
         * Convert numeric digit back to a character.
         *
         * Example:
         *
         *     5 -> '5'
         */
        chars[wheel] = (char) ('0' + digit);

        /*
         * Convert the character array back to String.
         */
        return new String(chars);
    }
}
