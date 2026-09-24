import java.util.*;

/*
 * ==========================================================================================
 *                                   KEYS AND ROOMS
 * ==========================================================================================
 *
 * 1. PROBLEM RESTATEMENT IN SIMPLE TERMS
 * ------------------------------------------------------------------------------------------
 * You are standing outside a building with N rooms, numbered 0 to N-1. 
 * All rooms are locked except for Room 0. 
 * When you enter a room, you might find some keys scattered around. Each key has a number 
 * on it, which tells you exactly which room it unlocks.
 * Your goal is to figure out if it is possible to eventually unlock and visit EVERY SINGLE 
 * room in the building just by picking up the keys you find along the way.
 * Return true if you can visit every room, otherwise return false.
 *
 *
 * 2. IDEA, INTUITION, AND KEY OBSERVATIONS
 * ------------------------------------------------------------------------------------------
 * - Graph Representation: This is a classic Directed Graph problem. 
 *   - Rooms = Nodes (Vertices)
 *   - Keys = Directed Edges (From the room you are in, to the room the key opens)
 * - Reachability: The problem asks, "Are all nodes reachable from Node 0?"
 * - Traversal: If we start at Node 0 and explore every path (by picking up keys and opening 
 *   the corresponding rooms), we just need to keep track of which rooms we've successfully 
 *   entered. 
 * - Stopping Condition: We stop when we run out of new rooms to open. At the end, we count 
 *   how many unique rooms we visited. If it equals N, we succeed. Otherwise, we fail.
 *
 *
 * 3. HOW TO IDENTIFY THE RIGHT APPROACH
 * ------------------------------------------------------------------------------------------
 * - The words "start in room 0", "available keys", and "enter every room" point directly 
 *   to Graph Traversal (DFS or BFS).
 * - Because we just need to determine 'reachability' (not shortest path), both Depth-First 
 *   Search (DFS) and Breadth-First Search (BFS) are equally valid and optimal.
 * - Since the rooms are numbered exactly from 0 to N-1, we can use a simple boolean array 
 *   (boolean[] visited) instead of a HashSet, which drastically improves speed and lowers 
 *   memory overhead.
 *
 *
 * 4. CLARIFYING QUESTIONS TO ASK THE INTERVIEWER (And why they matter)
 * ------------------------------------------------------------------------------------------
 * Q1: Can a room contain a key to itself?
 *     Why: To ensure our logic handles self-loops without getting stuck in infinite recursion. 
 *          (Using a 'visited' array handles this automatically).
 * Q2: Can multiple rooms have the key to the same room?
 *     Why: Tells us that multiple edges can point to the same node. Our traversal must 
 *          safely ignore a key if we've already unlocked that room.
 * Q3: What is the maximum number of rooms (N)?
 *     Why: If N is huge (e.g., 10^5), a recursive DFS might cause a StackOverflowError, 
 *          meaning we should prefer BFS or iterative DFS. The constraints say N <= 1000, 
 *          so recursion is perfectly safe and clean here.
 * Q4: Do we need to return the order of rooms visited?
 *     Why: Clarifies the output type. The problem just asks for a boolean (true/false).
 *
 *
 * 5. ASCII VISUALS / TRACING
 * ------------------------------------------------------------------------------------------
 * Example: rooms = [[1], [2], [3], []]
 * Graph View:
 *     (0) ---> (1) ---> (2) ---> (3)
 *     Start
 * 
 * Trace DFS:
 *   Start at 0: Mark visited. Key found: 1.
 *   Go to 1: Mark visited. Key found: 2.
 *   Go to 2: Mark visited. Key found: 3.
 *   Go to 3: Mark visited. No keys.
 *   Result: 4 rooms visited == N. Return true.
 *
 * Example: rooms = [[1, 3], [3, 0, 1], [2], [0]]
 * Graph View:
 *     (0) <---> (1)
 *      | \     /
 *      |  \   /         (2) <-- 2 (self-loop, isolated!)
 *      v   v v
 *     (3) -/
 * 
 * Trace BFS:
 *   Start at 0: Visited={0}. Queue=[0].
 *   Pop 0: Keys found [1, 3]. Add to Queue. Visited={0, 1, 3}.
 *   Pop 1: Keys found [3, 0, 1]. All already visited. Queue=[3].
 *   Pop 3: Keys found [0]. Already visited. Queue=[].
 *   Result: Rooms visited: {0, 1, 3} (Count = 3). N = 4. 
 *   Room 2 is locked forever! Return false.
 *
 *
 * 6. EXAMPLES AND IMPORTANT EDGE CASES
 * ------------------------------------------------------------------------------------------
 * - Edge Case 1: Room 0 has no keys (rooms = [[], [1, 2], [3], []]).
 *   Only room 0 will be visited. Return false (since N > 1 based on constraints).
 * - Edge Case 2: Keys to already opened rooms (rooms = [[1], [0], []]).
 *   Room 0 -> Room 1 -> Room 0. Visited array prevents endless ping-pong.
 *
 *
 * 7. COMMON MISTAKES AND PITFALLS
 * ------------------------------------------------------------------------------------------
 * 1. Using HashSet<Integer> instead of boolean[]. While a HashSet works, boxing/unboxing 
 *    integers and computing hashes is slower. A boolean array is strictly better when node 
 *    labels are contiguous integers from 0 to N-1.
 * 2. Iterating through the rooms array linearly (e.g., for(int i=0; i<N; i++)). You MUST 
 *    follow the keys (graph edges), not the room numbers sequentially.
 * 3. Failing to count the visited rooms correctly. Some people loop through the `visited` 
 *    array at the end to check for `false`, which is fine but slightly less optimal than 
 *    just maintaining a running `count` variable.
 *
 *
 * 8. INTERVIEW STRATEGY: HOW TO APPROACH AND EXPLAIN
 * ------------------------------------------------------------------------------------------
 * 1. Acknowledge the core concept: "This is a reachability problem in a directed graph. 
 *    Rooms are nodes, keys are edges, and we start at node 0."
 * 2. State your data structures: "I will use a boolean array to track visited rooms so we 
 *    don't get stuck in cycles. I'll also keep a counter of how many rooms we've entered."
 * 3. Propose traversal: "I can solve this with either DFS or BFS. Because we only care about 
 *    visiting everything, not shortest paths, I'll write DFS as it's very clean."
 * 4. Write DFS (Approach 1). It's concise. If the interviewer asks about StackOverflow, 
 *    explain that since N <= 1000, max recursion depth is 1000, which easily fits in memory.
 *    Then briefly write or explain the BFS version.
 * ==========================================================================================
 */
public class KeysAndRooms {

    /*
     * APPROACH 1: DEPTH-FIRST SEARCH (DFS - Recursive) - RECOMMENDED
     * --------------------------------------------------------------------------------------
     * Idea: We start at Room 0. For every key we find, if the room it opens hasn't been 
     * visited yet, we immediately go into that room (recursively) and explore its keys.
     * We use a global/passed-by-reference array to keep track of visited rooms.
     *
     * Complexity:
     * - Time: O(V + E) -> O(N + K) where N is number of rooms and K is total number of keys.
     *   We visit each room at most once, and we look at each key at most once.
     * - Space: O(N) for the boolean `visited` array, plus O(N) for the recursion call stack 
     *   in the worst-case scenario (e.g., a straight line of N rooms: 0->1->2->...->N-1).
     */
    public boolean canVisitAllRoomsDFS(List<List<Integer>> rooms) {
        int n = rooms.size();
        boolean[] visited = new boolean[n];
        
        // Start recursive DFS from room 0
        dfs(rooms, visited, 0);

        // After traversal, verify if every room was visited
        for (boolean wasVisited : visited) {
            if (!wasVisited) {
                return false; // Found a room we couldn't reach
            }
        }
        
        return true;
    }

    private void dfs(List<List<Integer>> rooms, boolean[] visited, int currentRoom) {
        // Mark the current room as visited
        visited[currentRoom] = true;

        // Iterate through all the keys found in the current room
        for (int nextRoomKey : rooms.get(currentRoom)) {
            // If we haven't visited the room this key belongs to, go visit it!
            if (!visited[nextRoomKey]) {
                dfs(rooms, visited, nextRoomKey);
            }
        }
    }


    /*
     * APPROACH 2: BREADTH-FIRST SEARCH (BFS - Iterative)
     * --------------------------------------------------------------------------------------
     * Idea: We use a Queue to explore the rooms level by level. 
     * We start at Room 0, grab all its keys, mark those rooms as visited, and put them in 
     * the queue to explore later. 
     * To make it slightly more optimal, we can keep a `count` of visited rooms. If `count` 
     * hits N early, we can stop immediately!
     *
     * Complexity:
     * - Time: O(N + K). Same as DFS. Each room and key processed once.
     * - Space: O(N) for the visited array and the Queue.
     */
    public boolean canVisitAllRoomsBFS(List<List<Integer>> rooms) {
        int n = rooms.size();
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();

        // Start with room 0
        visited[0] = true;
        queue.add(0);
        int visitedCount = 1; // Track how many unique rooms we've opened

        while (!queue.isEmpty()) {
            int currentRoom = queue.poll();

            // Look at all keys in the current room
            for (int key : rooms.get(currentRoom)) {
                
                if (!visited[key]) {
                    visited[key] = true; // Mark as visited AS SOON AS we find the key
                    queue.add(key);      // Queue it up to explore its keys later
                    visitedCount++;      // Increment our counter
                    
                    // Optimization: If we've reached all rooms, we can stop early!
                    if (visitedCount == n) {
                        return true;
                    }
                }
            }
        }

        // If we exit the loop and didn't hit N, some rooms were unreachable
        return visitedCount == n;
    }


    /*
     * ==========================================================================================
     * INTERVIEWER FOLLOW-UP QUESTIONS & ANSWERS
     * ==========================================================================================
     * 
     * F1: "What if there were MILLIONS of rooms? Would DFS still work?"
     * Ans: If N is millions, the recursion depth in the worst case (a linear chain of rooms) 
     *      would be millions, leading to a StackOverflowError. We should absolutely switch 
     *      to the iterative BFS approach (using a Queue) or an iterative DFS (using a Stack) 
     *      because heap memory is much larger than the thread stack.
     * 
     * F2: "Can we modify your algorithm to tell us WHICH rooms are unreachable?"
     * Ans: Yes! In both approaches, at the end we have a `visited` boolean array. Any index `i` 
     *      where `visited[i] == false` is an unreachable room. We can just iterate through 
     *      the array and add those indices to a List.
     *
     * F3: "What if multiple rooms have the key to room X. Does BFS process X multiple times?"
     * Ans: No, because we have the `if (!visited[key])` check. The very first time we see 
     *      the key to room X, we mark `visited[X] = true` and put it in the queue. 
     *      Subsequent discoveries of key X will hit the if-statement and be ignored.
     *
     * F4: "Is there any advantage of DFS over BFS here?"
     * Ans: Recursive DFS is more concise to write and has slightly less overhead (no Queue 
     *      object allocation, just function calls). However, both have identical Big-O 
     *      complexities. In real-world enterprise software, iterative (BFS) is often preferred 
     *      for graph traversal to avoid blowing up the stack on malformed data.
     * ==========================================================================================
     */


    // --------------------------------------------------------------------------------------
    // TEST RUNNER CODE
    // --------------------------------------------------------------------------------------
    public static void main(String[] args) {
        KeysAndRooms solution = new KeysAndRooms();

        // Test Case 1: Standard success case
        // 0 -> 1 -> 2 -> 3
        List<List<Integer>> rooms1 = new ArrayList<>();
        rooms1.add(Arrays.asList(1));    // Room 0 has key for 1
        rooms1.add(Arrays.asList(2));    // Room 1 has key for 2
        rooms1.add(Arrays.asList(3));    // Room 2 has key for 3
        rooms1.add(Arrays.asList());     // Room 3 is empty
        System.out.println("Test Case 1 (Expected true): " + solution.canVisitAllRoomsDFS(rooms1));

        // Test Case 2: Unreachable room
        // 0 <-> 1 (and 3), 2 is isolated!
        List<List<Integer>> rooms2 = new ArrayList<>();
        rooms2.add(Arrays.asList(1, 3)); // Room 0
        rooms2.add(Arrays.asList(3, 0, 1)); // Room 1
        rooms2.add(Arrays.asList(2));    // Room 2 has a self key, but we can never reach it
        rooms2.add(Arrays.asList(0));    // Room 3
        System.out.println("Test Case 2 (Expected false): " + solution.canVisitAllRoomsBFS(rooms2));

        // Test Case 3: Empty room 0 but graph has other rooms (N >= 2)
        // 0 is a dead end. We can't go anywhere.
        List<List<Integer>> rooms3 = new ArrayList<>();
        rooms3.add(Arrays.asList());     // Room 0 has NO keys
        rooms3.add(Arrays.asList(2));    // Room 1
        rooms3.add(Arrays.asList(1));    // Room 2
        System.out.println("Test Case 3 (Expected false): " + solution.canVisitAllRoomsDFS(rooms3));
        
        // Test Case 4: Complete graph (Lots of redundant keys)
        List<List<Integer>> rooms4 = new ArrayList<>();
        rooms4.add(Arrays.asList(1, 2, 3));
        rooms4.add(Arrays.asList(0, 2, 3));
        rooms4.add(Arrays.asList(0, 1, 3));
        rooms4.add(Arrays.asList(0, 1, 2));
        System.out.println("Test Case 4 (Expected true): " + solution.canVisitAllRoomsBFS(rooms4));
    }
}

