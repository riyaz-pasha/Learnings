import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a GRAPH TRAVERSAL IN TREE problem testing:
 * 1. Converting tree to undirected graph (add parent pointers)
 * 2. BFS from target node
 * 3. Tracking visited nodes
 * 4. Understanding that distance in tree can go UP and DOWN
 * 5. Multiple approaches with different trade-offs
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. TREES ARE DIRECTIONAL (parent → child)
 *    - But distance can go UP to parent!
 *    - Need to traverse in ALL directions from target
 *    - Solution: Add parent pointers OR build graph
 * 
 * 2. DISTANCE = NUMBER OF EDGES
 *    - Not about values, but about path length
 *    - Use BFS to find all nodes at exact distance K
 * 
 * 3. THIS IS ESSENTIALLY GRAPH BFS
 *    - Treat tree as undirected graph
 *    - Start from target node
 *    - Find all nodes at distance K
 * 
 * VISUALIZATION:
 * --------------
 * 
 * Example 1:
 *           3
 *          / \
 *         5   1
 *        / \ / \
 *       6  2 0  8
 *         / \
 *        7   4
 * 
 * Target = 5, K = 2
 * 
 * From node 5:
 * - Distance 0: [5]
 * - Distance 1: [6, 2, 3] (left child, right child, parent)
 * - Distance 2: [7, 4, 1] (children of 2, right child of 3)
 * 
 * Answer: [7, 4, 1]
 * 
 * Notice: We go DOWN to 7,4 and UP then DOWN to 1!
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Clarify the problem
 *    "So distance K means K edges away from target, and we can
 *     traverse up to parent nodes as well, correct?"
 * 
 * 2. Explain the challenge
 *    "The key challenge is that trees only have child pointers,
 *     but we need to traverse up to parents too."
 * 
 * 3. Propose solution
 *    "I'll add parent pointers in a preprocessing step, then
 *     do BFS from the target node treating it like a graph."
 * 
 * 4. Walk through example
 *    [Draw tree, show BFS expansion]
 * 
 * 5. Discuss alternatives
 *    - DFS with distance tracking
 *    - Two-phase approach (down from target, up and down from ancestors)
 */

// Definition for a binary tree node
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    
    TreeNode(int x) { val = x; }
}

class AllNodesDistanceKSolution {
    
    /**
     * APPROACH 1: BFS WITH PARENT POINTERS - MOST INTUITIVE
     * ======================================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Build parent map: child → parent
     * 2. Find target node in tree
     * 3. BFS from target node with distance tracking
     * 4. Treat tree as undirected graph (visit children AND parent)
     * 5. Track visited nodes to avoid cycles
     * 6. Collect all nodes at distance K
     * 
     * DETAILED STEPS:
     * ---------------
     * Phase 1: Build parent pointers
     * - DFS/BFS through tree
     * - Store parent[child] = parent
     * 
     * Phase 2: Find target node
     * - DFS/BFS to locate node with target value
     * 
     * Phase 3: BFS from target
     * - Start at target with distance 0
     * - Explore: left child, right child, parent
     * - Track visited to avoid revisiting
     * - When distance = K, add to result
     * 
     * VISUALIZATION:
     * --------------
     *           3
     *          / \
     *         5   1
     *        / \ / \
     *       6  2 0  8
     *         / \
     *        7   4
     * 
     * Parent map: {5→3, 1→3, 6→5, 2→5, 0→1, 8→1, 7→2, 4→2}
     * 
     * BFS from node 5 (target), K=2:
     * Distance 0: [5] (visited: {5})
     * Distance 1: [6, 2, 3] (visited: {5,6,2,3})
     *   - 5.left = 6
     *   - 5.right = 2
     *   - parent[5] = 3
     * Distance 2: [7, 4, 1] (visited: {5,6,2,3,7,4,1})
     *   - 2.left = 7
     *   - 2.right = 4
     *   - 3.right = 1 (3.left=5 already visited)
     * 
     * Result: [7, 4, 1]
     * 
     * TIME COMPLEXITY: O(N)
     * - Building parent map: O(N)
     * - Finding target: O(N)
     * - BFS: O(N) in worst case (visit all nodes)
     * - Total: O(N)
     * 
     * SPACE COMPLEXITY: O(N)
     * - Parent map: O(N)
     * - Visited set: O(N)
     * - Queue: O(N) worst case
     * - Total: O(N)
     * 
     * WHY THIS IS INTUITIVE:
     * ----------------------
     * - Treats tree as graph naturally
     * - Standard BFS algorithm
     * - Easy to understand and implement
     * - Clear separation of concerns
     */
    public List<Integer> distanceK(TreeNode root, TreeNode target, int k) {
        List<Integer> result = new ArrayList<>();
        
        // Step 1: Build parent pointers
        Map<TreeNode, TreeNode> parentMap = new HashMap<>();
        buildParentMap(root, null, parentMap);
        
        // Step 2: BFS from target node
        Queue<TreeNode> queue = new LinkedList<>();
        Set<TreeNode> visited = new HashSet<>();
        
        queue.offer(target);
        visited.add(target);
        
        int distance = 0;
        
        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            
            // If we've reached distance K, collect all nodes at this level
            if (distance == k) {
                for (int i = 0; i < levelSize; i++) {
                    result.add(queue.poll().val);
                }
                return result;
            }
            
            // Process current level
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                
                // Explore all three directions: left, right, parent
                
                // Go to left child
                if (node.left != null && !visited.contains(node.left)) {
                    queue.offer(node.left);
                    visited.add(node.left);
                }
                
                // Go to right child
                if (node.right != null && !visited.contains(node.right)) {
                    queue.offer(node.right);
                    visited.add(node.right);
                }
                
                // Go to parent
                TreeNode parent = parentMap.get(node);
                if (parent != null && !visited.contains(parent)) {
                    queue.offer(parent);
                    visited.add(parent);
                }
            }
            
            distance++;
        }
        
        return result; // Empty if K is beyond tree depth
    }
    
    // Helper: Build parent map using DFS
    private void buildParentMap(TreeNode node, TreeNode parent, 
                                Map<TreeNode, TreeNode> parentMap) {
        if (node == null) return;
        
        parentMap.put(node, parent);
        buildParentMap(node.left, node, parentMap);
        buildParentMap(node.right, node, parentMap);
    }
    
    /**
     * APPROACH 2: DFS WITH DISTANCE TRACKING - ONE PASS
     * ==================================================
     * 
     * ALGORITHM:
     * ----------
     * 1. DFS to find target and collect nodes at distance K below it
     * 2. On the way back up, collect nodes at distance K-d from ancestors
     *    where d is the distance from ancestor to target
     * 
     * Key insight: Distance K can be:
     * - In target's subtree (go down)
     * - From target's ancestors (go up, then down other branch)
     * 
     * PHASES:
     * -------
     * Phase 1: Find target and collect nodes K distance below it
     * Phase 2: While returning, for each ancestor at distance d:
     *          - Collect nodes at distance K-d-1 in the OTHER subtree
     * 
     * EXAMPLE:
     *           3
     *          / \
     *         5   1
     *        / \ / \
     *       6  2 0  8
     *         / \
     *        7   4
     * 
     * Target=5, K=2
     * 
     * Phase 1: At node 5, find nodes at distance 2 below
     *   → Go down 2 levels: find 7, 4
     * 
     * Phase 2: Return to ancestors
     *   At node 3 (distance 1 from target):
     *   → Need nodes at distance K-1-1=0 from the OTHER branch
     *   → Other branch is 1 subtree
     *   → Distance 0 from 3 in right subtree: node 1
     * 
     * Result: [7, 4, 1]
     * 
     * TIME COMPLEXITY: O(N)
     * - Single DFS traversal
     * 
     * SPACE COMPLEXITY: O(H)
     * - Recursion stack only
     * - H = height of tree
     * - Better than Approach 1's O(N) space!
     * 
     * WHEN TO USE:
     * - Need to optimize space
     * - Don't mind more complex logic
     * - Want single-pass solution
     */
    public List<Integer> distanceKDFS(TreeNode root, TreeNode target, int k) {
        List<Integer> result = new ArrayList<>();
        dfsFromTarget(root, target, k, result);
        return result;
    }
    
    /**
     * Returns distance from node to target if target is in subtree
     * Returns -1 if target not found
     * 
     * Also collects nodes at distance K from target
     */
    private int dfsFromTarget(TreeNode node, TreeNode target, int k, 
                              List<Integer> result) {
        if (node == null) return -1;
        
        // Found target node
        if (node == target) {
            // Collect all nodes at distance K below target
            collectNodesAtDistance(node, k, result);
            return 0; // Distance from target to itself
        }
        
        // Search in left subtree
        int leftDist = dfsFromTarget(node.left, target, k, result);
        if (leftDist != -1) {
            // Target found in left subtree
            // leftDist = distance from left child to target
            
            // If current node is at distance K from target, add it
            if (leftDist + 1 == k) {
                result.add(node.val);
            } else {
                // Collect nodes in RIGHT subtree at distance K-leftDist-2
                // -1 for edge to current node, -1 for edge to right child
                collectNodesAtDistance(node.right, k - leftDist - 2, result);
            }
            
            return leftDist + 1; // Distance from current node to target
        }
        
        // Search in right subtree
        int rightDist = dfsFromTarget(node.right, target, k, result);
        if (rightDist != -1) {
            // Target found in right subtree
            
            if (rightDist + 1 == k) {
                result.add(node.val);
            } else {
                // Collect nodes in LEFT subtree
                collectNodesAtDistance(node.left, k - rightDist - 2, result);
            }
            
            return rightDist + 1;
        }
        
        // Target not found in this subtree
        return -1;
    }
    
    // Helper: Collect all nodes at exactly distance K from current node (going down only)
    private void collectNodesAtDistance(TreeNode node, int k, List<Integer> result) {
        if (node == null || k < 0) return;
        
        if (k == 0) {
            result.add(node.val);
            return;
        }
        
        collectNodesAtDistance(node.left, k - 1, result);
        collectNodesAtDistance(node.right, k - 1, result);
    }
    
    /**
     * APPROACH 3: BUILD GRAPH EXPLICITLY - MOST STRAIGHTFORWARD
     * ==========================================================
     * 
     * ALGORITHM:
     * ----------
     * 1. Build adjacency list graph from tree
     * 2. Standard BFS from target in graph
     * 
     * PROS:
     * - Very clear and easy to understand
     * - Reuses standard graph BFS
     * 
     * CONS:
     * - Extra space for graph representation
     * - Need to map values to nodes (if using values)
     * 
     * TIME: O(N), SPACE: O(N)
     */
    public List<Integer> distanceKGraph(TreeNode root, TreeNode target, int k) {
        List<Integer> result = new ArrayList<>();
        
        // Build graph (adjacency list)
        Map<TreeNode, List<TreeNode>> graph = new HashMap<>();
        buildGraph(root, null, graph);
        
        // BFS from target
        Queue<TreeNode> queue = new LinkedList<>();
        Set<TreeNode> visited = new HashSet<>();
        
        queue.offer(target);
        visited.add(target);
        
        int distance = 0;
        
        while (!queue.isEmpty() && distance <= k) {
            int size = queue.size();
            
            if (distance == k) {
                for (int i = 0; i < size; i++) {
                    result.add(queue.poll().val);
                }
                return result;
            }
            
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                
                // Visit all neighbors in graph
                for (TreeNode neighbor : graph.getOrDefault(node, new ArrayList<>())) {
                    if (!visited.contains(neighbor)) {
                        queue.offer(neighbor);
                        visited.add(neighbor);
                    }
                }
            }
            
            distance++;
        }
        
        return result;
    }
    
    // Build bidirectional graph from tree
    private void buildGraph(TreeNode node, TreeNode parent, 
                           Map<TreeNode, List<TreeNode>> graph) {
        if (node == null) return;
        
        graph.putIfAbsent(node, new ArrayList<>());
        
        if (parent != null) {
            graph.get(node).add(parent);
            graph.get(parent).add(node);
        }
        
        buildGraph(node.left, node, graph);
        buildGraph(node.right, node, graph);
    }
}

/**
 * COMPLEXITY COMPARISON
 * =====================
 * 
 * Approach        | Time | Space | Difficulty | Best For
 * ----------------|------|-------|------------|------------------------
 * BFS + Parent    | O(N) | O(N)  | Easy       | Interviews, clarity
 * DFS One-Pass    | O(N) | O(H)  | Medium     | Space optimization
 * Explicit Graph  | O(N) | O(N)  | Easy       | Straightforward
 * 
 * RECOMMENDATION:
 * - Interview: Use BFS + Parent (Approach 1)
 * - Production: DFS One-Pass if space matters
 * - Learning: Try all three to understand trade-offs
 */

/**
 * COMMON MISTAKES & EDGE CASES
 * =============================
 * 
 * MISTAKES:
 * 1. Forgetting we can go UP through parent
 * 2. Not tracking visited nodes (causes infinite loop)
 * 3. Off-by-one errors in distance calculation
 * 4. Forgetting K=0 case (return target itself)
 * 5. Not handling when target is root (no parent)
 * 
 * EDGE CASES:
 * 1. K = 0 → Return [target]
 * 2. K > depth of tree → Return []
 * 3. Target is root
 * 4. Target is leaf node
 * 5. Single node tree
 * 6. All nodes at distance K
 */

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        AllNodesDistanceKSolution sol = new AllNodesDistanceKSolution();
        
        System.out.println("=== Test Case 1: Example 1 ===");
        //           3
        //          / \
        //         5   1
        //        / \ / \
        //       6  2 0  8
        //         / \
        //        7   4
        TreeNode root1 = new TreeNode(3);
        root1.left = new TreeNode(5);
        root1.right = new TreeNode(1);
        root1.left.left = new TreeNode(6);
        root1.left.right = new TreeNode(2);
        root1.right.left = new TreeNode(0);
        root1.right.right = new TreeNode(8);
        root1.left.right.left = new TreeNode(7);
        root1.left.right.right = new TreeNode(4);
        
        TreeNode target1 = root1.left; // Node with value 5
        
        System.out.println("BFS:   " + sol.distanceK(root1, target1, 2));
        System.out.println("DFS:   " + sol.distanceKDFS(root1, target1, 2));
        System.out.println("Graph: " + sol.distanceKGraph(root1, target1, 2));
        System.out.println("Expected: [7, 4, 1] (any order)");
        
        System.out.println("\n=== Test Case 2: K = 0 ===");
        System.out.println("Result:   " + sol.distanceK(root1, target1, 0));
        System.out.println("Expected: [5]");
        
        System.out.println("\n=== Test Case 3: K = 1 ===");
        System.out.println("Result:   " + sol.distanceK(root1, target1, 1));
        System.out.println("Expected: [6, 2, 3] (any order)");
        
        System.out.println("\n=== Test Case 4: Single Node ===");
        TreeNode root2 = new TreeNode(1);
        System.out.println("K=0: " + sol.distanceK(root2, root2, 0));
        System.out.println("K=3: " + sol.distanceK(root2, root2, 3));
        System.out.println("Expected: [1], []");
        
        System.out.println("\n=== Test Case 5: Target is Root ===");
        //     1
        //    / \
        //   2   3
        TreeNode root3 = new TreeNode(1);
        root3.left = new TreeNode(2);
        root3.right = new TreeNode(3);
        System.out.println("Result:   " + sol.distanceK(root3, root3, 1));
        System.out.println("Expected: [2, 3] (any order)");
        
        System.out.println("\n=== Test Case 6: Target is Leaf ===");
        TreeNode target6 = root3.left; // Node 2
        System.out.println("Result:   " + sol.distanceK(root3, target6, 1));
        System.out.println("Expected: [1]");
        
        System.out.println("\n=== Test Case 7: K > Max Distance ===");
        System.out.println("Result:   " + sol.distanceK(root3, root3, 10));
        System.out.println("Expected: []");
        
        System.out.println("\n=== Test Case 8: Linear Tree ===");
        //   1
        //  /
        // 2
        //  \
        //   3
        TreeNode root4 = new TreeNode(1);
        root4.left = new TreeNode(2);
        root4.left.right = new TreeNode(3);
        TreeNode target8 = root4.left; // Node 2
        System.out.println("Result:   " + sol.distanceK(root4, target8, 1));
        System.out.println("Expected: [1, 3] (any order)");
        
        System.out.println("\n=== Visual Representation ===");
        System.out.println("For target=5, K=2:");
        System.out.println("        3");
        System.out.println("       / \\");
        System.out.println("      5*  1*     (* = K=2 from target)");
        System.out.println("     / \\ / \\");
        System.out.println("    6  2 0  8");
        System.out.println("      / \\");
        System.out.println("     7*  4*     (* = K=2 from target)");
        System.out.println();
        System.out.println("Distance 0: [5]");
        System.out.println("Distance 1: [6, 2, 3]");
        System.out.println("Distance 2: [7, 4, 1] ✓");
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "The key insight is that distance K can go UP through parents
 *    and back DOWN through siblings. Trees normally only have child
 *    pointers, so I need to add parent information."
 * 
 * 2. "I'll use a two-phase approach: First, build a parent map.
 *    Second, do BFS from the target treating the tree as an
 *    undirected graph."
 *    [Draw tree with bidirectional arrows]
 * 
 * 3. "During BFS, I track visited nodes to avoid cycles, and
 *    explore three directions from each node: left child,
 *    right child, and parent."
 * 
 * 4. "When I reach distance K, I collect all nodes at that level."
 *    [Walk through example step by step]
 * 
 * 5. "Time is O(N) - I visit each node at most twice (once to
 *    build parent map, once in BFS). Space is O(N) for the
 *    parent map and visited set."
 * 
 * 6. "There's also a DFS approach with O(H) space if memory
 *    is tight, but it's more complex."
 * 
 * KEY POINTS TO EMPHASIZE:
 * - Why we need parent pointers
 * - Tree as undirected graph
 * - Visited tracking to prevent cycles
 * - BFS gives us distance naturally
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Can you do it with less space? [DFS approach]
 * - What if tree had parent pointers? [Skip building parent map]
 * - How would you find nodes within distance K? [Similar BFS, collect all ≤ K]
 * - What if K is very large? [Early termination when queue empty]
 * - Can you do it without modifying the tree? [Yes, using map]
 */
