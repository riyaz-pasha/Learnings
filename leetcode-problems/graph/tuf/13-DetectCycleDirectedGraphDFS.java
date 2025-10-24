import java.util.List;

class DetectCycleDirectedGraphDFS {

    public boolean hasCycle(int V, List<List<Integer>> adj) {
        boolean[] visited = new boolean[V];
        boolean[] recStack = new boolean[V]; // Recursion stack

        for (int node = 0; node < V; node++) {
            if (!visited[node]) {
                if (hasCycleDFS(adj, visited, recStack, node)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCycleDFS(List<List<Integer>> adj, boolean[] visited,
            boolean[] recStack, int node) {
        visited[node] = true;
        recStack[node] = true; // Add to current path

        for (int neighbor : adj.get(node)) {
            // If not visited, explore recursively
            if (!visited[neighbor]) {
                if (hasCycleDFS(adj, visited, recStack, neighbor)) {
                    return true;
                }
            }
            // If neighbor is in current path, cycle found
            else if (recStack[neighbor]) {
                return true;
            }
        }

        recStack[node] = false; // Remove from current path (backtrack)
        return false;
    }
}

class DetectCycleDirectedGraphDFS2 {

    public boolean hasCycle(int V, List<List<Integer>> adj) {
        int[] state = new int[V]; // 0: unvisited, 1: visiting, 2: visited

        for (int node = 0; node < V; node++) {
            if (state[node] == 0) {
                if (hasCycleDFS(adj, state, node)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCycleDFS(List<List<Integer>> adj, int[] state, int node) {
        state[node] = 1; // Mark as visiting (in current path)

        for (int neighbor : adj.get(node)) {
            if (state[neighbor] == 1) { // Back edge found - cycle!
                return true;
            }
            if (state[neighbor] == 0) { // Unvisited, explore
                if (hasCycleDFS(adj, state, neighbor)) {
                    return true;
                }
            }
            // If state[neighbor] == 2 (visited), skip (cross edge)
        }

        state[node] = 2; // Mark as visited (done processing)
        return false;
    }
}

/*
 * | Aspect | Undirected Graph | Directed Graph |
 * |--------|------------------|----------------|
 * | Cycle Detection | Single `visited[]` array | Need `visited[]` +
 * `recStack[]` |
 * | Cycle Condition | Visiting an already visited neighbor (except parent) |
 * Visiting a node in current recursion path |
 * | Back Edge | Any edge to visited node | Edge to node in current path |
 * 
 * ## Example Walkthrough
 * ```
 * Graph:
 * 0 → 1 → 2
 * ↓ ↑
 * 3 ──────┘ (cycle: 1 → 2 → 3 → back to 1? NO, 3→2 exists)
 * 
 * Actually: 0→1→2, 0→3
 */

/*
 * Perfect — that’s the most **important and subtle** part of detecting cycles
 * in directed graphs using DFS.
 * Let’s go step by step so you’ll **never forget it again** 👇
 * 
 * ---
 * 
 * ## 🔍 Step 1: Understanding what we’re really detecting
 * 
 * A **cycle in a directed graph** means:
 * 
 * > there exists a path that starts from a node and eventually comes **back to
 * the same node** through directed edges.
 * 
 * That “comes back” part means — while we’re still exploring (not done with
 * recursion) —
 * we find an edge **pointing back into our current path**.
 * 
 * That kind of edge is called a **back edge**.
 * 
 * ---
 * 
 * ## 🧭 Step 2: Why one `visited[]` isn’t enough
 * 
 * `visited[node] = true` only tells us “I’ve seen this node before.”
 * But it doesn’t tell us **“Am I still exploring this node’s descendants?”**
 * 
 * So when DFS visits a node that’s *already visited*, we don’t know:
 * 
 * is it **a finished node** (safe to ignore), or
 * is it **an ancestor in my current DFS path** (which means a cycle)?
 * 
 * We need to distinguish these two cases.
 * 
 * That’s why we add a second array:
 * `pathVisited[node] = true` → “I’m currently exploring this node’s
 * descendants.”
 * When recursion backtracks, we set `pathVisited[node] = false`.
 * 
 * ---
 * 
 * ## ⚙️ Step 3: When is there a back edge?
 * 
 * When you’re doing DFS and find a neighbor that’s:
 * 
 * already **visited**, ✅ and
 * also **still in the recursion stack** (`pathVisited[neighbor] == true`), ⚠️
 * 
 * then you found an edge that points **backwards** in your DFS path.
 * 
 * That’s a **back edge** → which means there’s a **cycle**.
 * 
 * ---
 * 
 * ## 📈 Step 4: Example Walkthrough
 * 
 * Let’s take this graph:
 * 
 * ```
 * 0 → 1 → 2
 * ↑ |
 * | ↓
 * 4 ← 3
 * ```
 * 
 * There’s a **cycle: 1 → 2 → 3 → 4 → 1**.
 * 
 * Let’s run DFS step-by-step.
 * 
 * ---
 * 
 * ### Initial State
 * 
 * ```
 * visited = [F, F, F, F, F]
 * pathVisited = [F, F, F, F, F]
 * ```
 * 
 * ### Start DFS(0)
 * 
 * visit 0 → mark both arrays
 * 
 * ```
 * visited[0]=T, pathVisited[0]=T
 * ```
 * 
 * 0 → 1 → DFS(1)
 * 
 * ---
 * 
 * ### DFS(1)
 * 
 * ```
 * visited[1]=T, pathVisited[1]=T
 * ```
 * 
 * 1 → 2 → DFS(2)
 * 
 * ---
 * 
 * ### DFS(2)
 * 
 * ```
 * visited[2]=T, pathVisited[2]=T
 * ```
 * 
 * 2 → 3 → DFS(3)
 * 
 * ---
 * 
 * ### DFS(3)
 * 
 * ```
 * visited[3]=T, pathVisited[3]=T
 * ```
 * 
 * 3 → 4 → DFS(4)
 * 
 * ---
 * 
 * ### DFS(4)
 * 
 * ```
 * visited[4]=T, pathVisited[4]=T
 * ```
 * 
 * 4 → 1 (neighbor)
 * `visited[1] == true` ✅
 * `pathVisited[1] == true` ✅ ← ***This means 1 is still on recursion stack***
 * 
 * 👉 So there’s a **back edge from 4 → 1**
 * 
 * That’s a **cycle!**
 * 
 * ---
 * 
 * ### 🎯 Intuitive meaning
 * 
 * When DFS(4) sees that 1 is still being explored (has not finished),
 * it realizes: “I reached my ancestor again without finishing the previous
 * chain.”
 * That’s exactly what a **directed cycle** means — a path leading back to
 * itself.
 * 
 * ---
 * 
 * ## 🧩 Step 5: Visual intuition
 * 
 * Think of your DFS path like a stack:
 * 
 * ```
 * DFS Stack:
 * [0, 1, 2, 3, 4]
 * ```
 * 
 * Now, when you find an edge from `4 → 1`,
 * you’re pointing **upward** in that stack (backwards in recursion order).
 * That’s a **back edge → cycle**.
 * 
 * But if you point **downward** (to something that has finished and popped out
 * of the stack),
 * that’s a **forward edge or cross edge → not a cycle**.
 * 
 * ---
 * 
 * ## 🧠 Summary
 * 
 * | Edge type | Meaning | `visited` | `pathVisited` | Cycle? |
 * | ---------------------------------- | ---------------------- | --------- |
 * ------------- | ------ |
 * | Not visited | Tree edge (normal DFS) | false | false | ❌ |
 * | Visited but not in recursion stack | Cross/Forward edge | true | false | ❌
 * |
 * | Visited and in recursion stack | Back edge | true | true | ✅ |
 * 
 * ---
 * 
 * ## ✅ TL;DR
 * 
 * > A **back edge** exists if during DFS we reach a node that is **already
 * visited and still in recursion stack**.
 * > This means there’s a **path from that node back to itself → cycle in a
 * directed graph.**
 * 
 */