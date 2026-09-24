import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    TreeNode(int val) { this.val = val; }
    TreeNode(int val, TreeNode left, TreeNode right) {
        this.val = val;
        this.left = left;
        this.right = right;
    }
}

class BinaryTreeInfection {
    
    // Approach 1: Convert to Graph + BFS (Most Intuitive)
    // Time: O(n), Space: O(n)
    public int amountOfTimeGraph(TreeNode root, int start) {
        // Build adjacency list
        Map<Integer, List<Integer>> graph = new HashMap<>();
        buildGraph(root, null, graph);
        
        // BFS from start node
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        
        queue.offer(start);
        visited.add(start);
        
        int minutes = -1;  // Start at -1 because we count levels
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            minutes++;
            
            for (int i = 0; i < size; i++) {
                int node = queue.poll();
                
                for (int neighbor : graph.getOrDefault(node, new ArrayList<>())) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
        }
        
        return minutes;
    }
    
    private void buildGraph(TreeNode node, TreeNode parent, Map<Integer, List<Integer>> graph) {
        if (node == null) return;
        
        graph.putIfAbsent(node.val, new ArrayList<>());
        
        if (parent != null) {
            graph.get(node.val).add(parent.val);
            graph.get(parent.val).add(node.val);
        }
        
        buildGraph(node.left, node, graph);
        buildGraph(node.right, node, graph);
    }
    
    // Approach 2: One Pass with Distance Calculation (OPTIMAL)
    // Time: O(n), Space: O(h) where h is height
    public int amountOfTime(TreeNode root, int start) {
        int[] maxDistance = {0};
        dfs(root, start, maxDistance);
        return maxDistance[0];
    }
    
    // Returns distance from current node to start node, or -1 if start not in subtree
    private int dfs(TreeNode node, int start, int[] maxDistance) {
        if (node == null) return -1;
        
        // Found the start node
        if (node.val == start) {
            // Calculate max depth in both subtrees
            int leftDepth = getDepth(node.left);
            int rightDepth = getDepth(node.right);
            maxDistance[0] = Math.max(leftDepth, rightDepth);
            return 0;  // Distance to itself is 0
        }
        
        // Check left subtree
        int leftDist = dfs(node.left, start, maxDistance);
        if (leftDist != -1) {
            // Start is in left subtree
            // Distance to current node from start
            int distFromStart = leftDist + 1;
            
            // Max spread: either continue up or go down right subtree
            int rightDepth = getDepth(node.right);
            maxDistance[0] = Math.max(maxDistance[0], distFromStart + rightDepth);
            
            return distFromStart;
        }
        
        // Check right subtree
        int rightDist = dfs(node.right, start, maxDistance);
        if (rightDist != -1) {
            // Start is in right subtree
            int distFromStart = rightDist + 1;
            
            // Max spread: either continue up or go down left subtree
            int leftDepth = getDepth(node.left);
            maxDistance[0] = Math.max(maxDistance[0], distFromStart + leftDepth);
            
            return distFromStart;
        }
        
        return -1;  // Start not in this subtree
    }
    
    private int getDepth(TreeNode node) {
        if (node == null) return 0;
        return 1 + Math.max(getDepth(node.left), getDepth(node.right));
    }
    
    // Approach 3: Parent Pointers + BFS
    // Time: O(n), Space: O(n)
    public int amountOfTimeParentPointers(TreeNode root, int start) {
        // Build parent map and find start node
        Map<TreeNode, TreeNode> parentMap = new HashMap<>();
        TreeNode startNode = buildParentMap(root, null, start, parentMap);
        
        // BFS from start node
        Queue<TreeNode> queue = new LinkedList<>();
        Set<TreeNode> visited = new HashSet<>();
        
        queue.offer(startNode);
        visited.add(startNode);
        
        int minutes = 0;
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            boolean spread = false;
            
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                
                // Check left child
                if (node.left != null && !visited.contains(node.left)) {
                    visited.add(node.left);
                    queue.offer(node.left);
                    spread = true;
                }
                
                // Check right child
                if (node.right != null && !visited.contains(node.right)) {
                    visited.add(node.right);
                    queue.offer(node.right);
                    spread = true;
                }
                
                // Check parent
                TreeNode parent = parentMap.get(node);
                if (parent != null && !visited.contains(parent)) {
                    visited.add(parent);
                    queue.offer(parent);
                    spread = true;
                }
            }
            
            if (spread) minutes++;
        }
        
        return minutes;
    }
    
    private TreeNode buildParentMap(TreeNode node, TreeNode parent, int start, 
                                     Map<TreeNode, TreeNode> parentMap) {
        if (node == null) return null;
        
        parentMap.put(node, parent);
        
        if (node.val == start) return node;
        
        TreeNode found = buildParentMap(node.left, node, start, parentMap);
        if (found != null) return found;
        
        return buildParentMap(node.right, node, start, parentMap);
    }
    
    // Approach 4: DFS with Distance Tracking
    // Time: O(n), Space: O(h)
    public int amountOfTimeDFSTracking(TreeNode root, int start) {
        Result result = new Result();
        dfsTrack(root, start, result);
        return result.maxTime;
    }
    
    private int dfsTrack(TreeNode node, int start, Result result) {
        if (node == null) return -1;
        
        if (node.val == start) {
            result.maxTime = Math.max(height(node.left), height(node.right));
            return 1;
        }
        
        int leftDist = dfsTrack(node.left, start, result);
        if (leftDist > 0) {
            result.maxTime = Math.max(result.maxTime, leftDist + height(node.right));
            return leftDist + 1;
        }
        
        int rightDist = dfsTrack(node.right, start, result);
        if (rightDist > 0) {
            result.maxTime = Math.max(result.maxTime, rightDist + height(node.left));
            return rightDist + 1;
        }
        
        return -1;
    }
    
    private int height(TreeNode node) {
        if (node == null) return 0;
        return 1 + Math.max(height(node.left), height(node.right));
    }
    
    static class Result {
        int maxTime = 0;
    }
    
    // Approach 5: Two DFS Passes
    // First pass: find path to start, Second pass: calculate max distance
    // Time: O(n), Space: O(h)
    public int amountOfTimeTwoPasses(TreeNode root, int start) {
        List<TreeNode> path = new ArrayList<>();
        findPath(root, start, path);
        
        if (path.isEmpty()) return 0;
        
        int maxTime = 0;
        TreeNode prev = null;
        
        for (int i = path.size() - 1; i >= 0; i--) {
            TreeNode curr = path.get(i);
            int time = path.size() - 1 - i;  // Distance from start to current
            
            // Add depth of the subtree we didn't come from
            if (curr.left != prev) {
                time += getDepth(curr.left);
            }
            if (curr.right != prev) {
                time += getDepth(curr.right);
            }
            
            maxTime = Math.max(maxTime, time);
            prev = curr;
        }
        
        return maxTime;
    }
    
    private boolean findPath(TreeNode node, int target, List<TreeNode> path) {
        if (node == null) return false;
        
        path.add(node);
        
        if (node.val == target) return true;
        
        if (findPath(node.left, target, path) || findPath(node.right, target, path)) {
            return true;
        }
        
        path.remove(path.size() - 1);
        return false;
    }
    
    // Helper: Build tree from array
    public static TreeNode buildTree(Integer[] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null) return null;
        
        TreeNode root = new TreeNode(arr[0]);
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        int i = 1;
        while (!queue.isEmpty() && i < arr.length) {
            TreeNode node = queue.poll();
            
            if (i < arr.length && arr[i] != null) {
                node.left = new TreeNode(arr[i]);
                queue.offer(node.left);
            }
            i++;
            
            if (i < arr.length && arr[i] != null) {
                node.right = new TreeNode(arr[i]);
                queue.offer(node.right);
            }
            i++;
        }
        
        return root;
    }
    
    // Helper: Visualize tree
    private static void visualizeTree(TreeNode root) {
        if (root == null) return;
        
        System.out.println("\n=== Tree Structure ===");
        List<List<String>> levels = new ArrayList<>();
        buildLevels(root, 0, levels);
        
        for (int i = 0; i < levels.size(); i++) {
            System.out.print("Level " + i + ": ");
            System.out.println(String.join(" ", levels.get(i)));
        }
    }
    
    private static void buildLevels(TreeNode node, int level, List<List<String>> levels) {
        if (node == null) return;
        
        if (levels.size() == level) {
            levels.add(new ArrayList<>());
        }
        
        levels.get(level).add(String.valueOf(node.val));
        buildLevels(node.left, level + 1, levels);
        buildLevels(node.right, level + 1, levels);
    }
    
    // Helper: Visualize infection spread
    private static void visualizeInfection(TreeNode root, int start) {
        System.out.println("\n=== Infection Spread Visualization ===");
        System.out.println("Start node: " + start);
        
        // Convert to graph
        Map<Integer, List<Integer>> graph = new HashMap<>();
        buildGraphVis(root, null, graph);
        
        // BFS to simulate infection
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        
        queue.offer(start);
        visited.add(start);
        
        int minute = 0;
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<Integer> infected = new ArrayList<>();
            
            for (int i = 0; i < size; i++) {
                int node = queue.poll();
                infected.add(node);
                
                for (int neighbor : graph.getOrDefault(node, new ArrayList<>())) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
            
            System.out.println("Minute " + minute + ": " + infected);
            minute++;
        }
        
        System.out.println("Total time: " + (minute - 1) + " minutes");
    }
    
    private static void buildGraphVis(TreeNode node, TreeNode parent, 
                                       Map<Integer, List<Integer>> graph) {
        if (node == null) return;
        
        graph.putIfAbsent(node.val, new ArrayList<>());
        
        if (parent != null) {
            graph.get(node.val).add(parent.val);
            graph.get(parent.val).add(node.val);
        }
        
        buildGraphVis(node.left, node, graph);
        buildGraphVis(node.right, node, graph);
    }
    
    // Test cases
    public static void main(String[] args) {
        BinaryTreeInfection solver = new BinaryTreeInfection();
        
        // Test Case 1
        System.out.println("Test Case 1:");
        Integer[] arr1 = {1,5,3,null,4,10,6,9,2};
        TreeNode root1 = buildTree(arr1);
        int start1 = 3;
        System.out.println("Tree: " + Arrays.toString(arr1));
        System.out.println("Start: " + start1);
        visualizeTree(root1);
        System.out.println("\nResult: " + solver.amountOfTime(root1, start1));
        System.out.println("Expected: 4");
        visualizeInfection(root1, start1);
        
        // Test Case 2
        System.out.println("\n\n========================================");
        System.out.println("Test Case 2:");
        Integer[] arr2 = {1};
        TreeNode root2 = buildTree(arr2);
        int start2 = 1;
        System.out.println("Tree: " + Arrays.toString(arr2));
        System.out.println("Start: " + start2);
        System.out.println("Result: " + solver.amountOfTime(root2, start2));
        System.out.println("Expected: 0");
        
        // Test Case 3: Balanced tree
        System.out.println("\n\n========================================");
        System.out.println("Test Case 3: Balanced Tree");
        Integer[] arr3 = {1,2,3,4,5,6,7};
        TreeNode root3 = buildTree(arr3);
        int start3 = 4;
        System.out.println("Tree: " + Arrays.toString(arr3));
        System.out.println("Start: " + start3);
        visualizeTree(root3);
        System.out.println("\nResult: " + solver.amountOfTime(root3, start3));
        visualizeInfection(root3, start3);
        
        // Test Case 4: Skewed tree
        System.out.println("\n\n========================================");
        System.out.println("Test Case 4: Skewed Tree");
        Integer[] arr4 = {1,2,null,3,null,4,null,5};
        TreeNode root4 = buildTree(arr4);
        int start4 = 3;
        System.out.println("Start: " + start4);
        visualizeTree(root4);
        System.out.println("\nResult: " + solver.amountOfTime(root4, start4));
        
        // Algorithm comparison
        System.out.println("\n\n=== Algorithm Comparison ===");
        compareAlgorithms();
        
        // Compare all approaches
        System.out.println("\n\n=== Comparing All Approaches ===");
        compareAllApproaches(root1, start1);
        
        // Detailed explanation
        System.out.println("\n\n=== Detailed Explanation ===");
        explainAlgorithm(root1, start1);
    }
    
    private static void compareAlgorithms() {
        System.out.println("┌──────────────────────┬──────────────┬──────────────┬─────────────────┐");
        System.out.println("│ Approach             │ Time         │ Space        │ Notes           │");
        System.out.println("├──────────────────────┼──────────────┼──────────────┼─────────────────┤");
        System.out.println("│ Graph + BFS          │ O(n)         │ O(n)         │ Most intuitive  │");
        System.out.println("│ One Pass DFS         │ O(n)         │ O(h)         │ Optimal         │");
        System.out.println("│ Parent Ptr + BFS     │ O(n)         │ O(n)         │ Clear logic     │");
        System.out.println("│ DFS Tracking         │ O(n)         │ O(h)         │ Similar to #2   │");
        System.out.println("│ Two DFS Passes       │ O(n)         │ O(h)         │ Two phases      │");
        System.out.println("└──────────────────────┴──────────────┴──────────────┴─────────────────┘");
        System.out.println("\nh = tree height, n = number of nodes");
    }
    
    private static void compareAllApproaches(TreeNode root, int start) {
        BinaryTreeInfection solver = new BinaryTreeInfection();
        
        System.out.println("Start node: " + start);
        System.out.println("\nGraph + BFS:       " + solver.amountOfTimeGraph(root, start));
        System.out.println("One Pass DFS:      " + solver.amountOfTime(root, start));
        System.out.println("Parent Ptr + BFS:  " + solver.amountOfTimeParentPointers(root, start));
        System.out.println("DFS Tracking:      " + solver.amountOfTimeDFSTracking(root, start));
        System.out.println("Two DFS Passes:    " + solver.amountOfTimeTwoPasses(root, start));
    }
    
    private static void explainAlgorithm(TreeNode root, int start) {
        System.out.println("KEY INSIGHTS:\n");
        
        System.out.println("1. INFECTION SPREADS LIKE BFS");
        System.out.println("   - Each minute = one level of spread");
        System.out.println("   - Spreads to all adjacent nodes (parent, left, right)");
        System.out.println("   - Max time = distance to farthest node\n");
        
        System.out.println("2. TREE → UNDIRECTED GRAPH");
        System.out.println("   - Add edges: parent ↔ left, parent ↔ right");
        System.out.println("   - Then run BFS from start node");
        System.out.println("   - Level-order traversal gives time\n");
        
        System.out.println("3. OPTIMAL: ONE PASS DFS");
        System.out.println("   - Find start node while traversing");
        System.out.println("   - Track distance from start to current");
        System.out.println("   - For each node on path: max(dist_up, dist_down)");
        System.out.println("   - No need to build graph!\n");
        
        System.out.println("4. THE TRICK");
        System.out.println("   - Infection can go UP (to parent) or DOWN (to children)");
        System.out.println("   - For node on path from root to start:");
        System.out.println("     * Distance to start = steps upward");
        System.out.println("     * Max spread = dist_to_start + depth_of_other_subtree");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Binary Tree Infection Time

Infection spreads from start node to all adjacent nodes each minute.
Find total time to infect entire tree.

KEY INSIGHT: This is BFS distance problem in disguise!

The infection spreads level by level (like BFS).
Max time = distance from start to farthest node.

EXAMPLE WALKTHROUGH:

Tree:        1
           /   \
          5     3 (start)
           \   / \
            4 10  6
           / \
          9   2

Start: 3

Minute 0: Node 3
Minute 1: Nodes 1, 10, 6 (adjacent to 3)
Minute 2: Node 5 (adjacent to 1)
Minute 3: Node 4 (adjacent to 5)
Minute 4: Nodes 9, 2 (adjacent to 4)

Total: 4 minutes

APPROACH 1: CONVERT TO GRAPH + BFS

Most intuitive approach:
1. Convert tree to undirected graph (adjacency list)
2. Run BFS from start node
3. Count levels

Building graph:
- For each node: add edges to parent, left, right
- Store as: node_val → [neighbor_vals]

BFS:
- Start from start node
- Each level = one minute
- Track visited to avoid cycles

Time: O(n), Space: O(n)

APPROACH 2: ONE PASS DFS (OPTIMAL)

Key insight: We don't need to build the graph!

During DFS:
1. Find the start node
2. Track distance from start to current node
3. For each node on path from root to start:
   - Can spread upward (already tracked)
   - Can spread downward (calculate depth of other subtree)
   - Max time = distance_up + depth_down

Algorithm:
```
dfs(node, start):
    if node == start:
        return 0  // Found it!
        Calculate max depth of left/right subtrees
    
    leftDist = dfs(left, start)
    if leftDist >= 0:  // Start is in left subtree
        distToStart = leftDist + 1
        // Can spread to right subtree
        maxTime = max(maxTime, distToStart + depth(right))
        return distToStart
    
    // Similar for right subtree
```

DETAILED TRACE: Tree from example, start=3

DFS from root (1):
  Check left (5):
    Check left (null): -1
    Check right (4):
      Check left (9): -1
      Check right (2): -1
      Return -1
    Return -1
  
  Check right (3): FOUND!
    depth(left=10) = 1
    depth(right=6) = 1
    maxTime = max(1, 1) = 1
    Return 0
  
  Back at node 1:
    distToStart = 0 + 1 = 1
    depth(left=5):
      depth(left=null) = 0
      depth(right=4):
        depth(left=9) = 1
        depth(right=2) = 1
        return 1 + max(1,1) = 2
      return 1 + max(0, 2) = 3
    maxTime = max(1, 1 + 3) = 4

Result: 4 ✓

WHY THIS WORKS:

For each node on path from root to start:
- Distance to start = steps we've taken upward
- Can spread downward into the "other" subtree
- Total spread = up_distance + down_depth

The max of all these values is our answer!

APPROACH 3: PARENT POINTERS + BFS

Middle ground approach:
1. Build parent map (node → parent)
2. Find start node
3. BFS treating parent as neighbor

Simpler than full graph, but still needs parent map.

Time: O(n), Space: O(n)

COMPLEXITY ANALYSIS:

Graph + BFS:
Time: O(n) - build graph O(n), BFS O(n)
Space: O(n) - adjacency list

One Pass DFS:
Time: O(n) - visit each node once
Space: O(h) - recursion stack
Best approach! Only O(h) space.

Parent Pointers:
Time: O(n)
Space: O(n) - parent map

EDGE CASES:

1. Single node: return 0
2. Start at root: max depth of tree
3. Start at leaf: height of tree
4. Skewed tree: O(n) time
5. Balanced tree: optimal spread

KEY OBSERVATIONS:

1. Infection spreads uniformly in all directions
2. Think of tree as undirected graph
3. Max time = distance to farthest node
4. Can optimize by tracking during traversal

VISUALIZATION:

For tree:     1
            /   \
           2     3 (start)
          / \
         4   5

Distance from 3:
- 3: 0
- 1: 1
- 2: 2
- 6: 1
- 4: 3
- 5: 3

Max distance: 3 (to nodes 4, 5)

INTERVIEW STRATEGY:

1. Recognize as graph distance problem
2. Explain BFS approach first (intuitive)
3. Optimize to one-pass DFS
4. Walk through example carefully
5. Discuss complexity: O(n) time, O(h) space
6. Handle edge cases

COMMON MISTAKES:

1. Forgetting to treat as undirected (can go to parent)
2. Not tracking visited in BFS (infinite loop)
3. Incorrect depth calculation
4. Off-by-one in minute counting
5. Not handling single node case

RELATED PROBLEMS:

1. Maximum Depth of Binary Tree
2. Diameter of Binary Tree
3. All Nodes Distance K
4. Binary Tree Maximum Path Sum
5. Lowest Common Ancestor

This problem beautifully combines:
- Tree traversal
- Graph thinking
- BFS/DFS
- Distance calculation
*/
