import java.util.*;

/*
 * Given the root of a binary tree, the value of a target node target, and an
 * integer k, return an array of the values of all nodes that have a distance k
 * from the target node.
 * 
 * You can return the answer in any order.
 * 
 */

// Definition for a binary tree node
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int val) {
        this.val = val;
    }

}

class NodesDistanceK {

    // Solution 1: Build Parent Map + BFS (Most Intuitive)
    // Time: O(n), Space: O(n)
    public List<Integer> distanceK(TreeNode root, TreeNode target, int k) {
        List<Integer> result = new ArrayList<>();
        if (root == null || k < 0)
            return result;

        // Build parent map
        Map<TreeNode, TreeNode> parentMap = new HashMap<>();
        buildParentMap(root, null, parentMap);

        // BFS from target node
        Queue<TreeNode> queue = new LinkedList<>();
        Set<TreeNode> visited = new HashSet<>();

        queue.offer(target);
        visited.add(target);

        int distance = 0;
        while (!queue.isEmpty() && distance < k) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                TreeNode curr = queue.poll();

                // Add left child
                if (curr.left != null && !visited.contains(curr.left)) {
                    visited.add(curr.left);
                    queue.offer(curr.left);
                }

                // Add right child
                if (curr.right != null && !visited.contains(curr.right)) {
                    visited.add(curr.right);
                    queue.offer(curr.right);
                }

                // Add parent
                TreeNode parent = parentMap.get(curr);
                if (parent != null && !visited.contains(parent)) {
                    visited.add(parent);
                    queue.offer(parent);
                }
            }
            distance++;
        }

        // Collect all nodes at distance k
        while (!queue.isEmpty()) {
            result.add(queue.poll().val);
        }

        return result;
    }

    private void buildParentMap(TreeNode node, TreeNode parent, Map<TreeNode, TreeNode> parentMap) {
        if (node == null)
            return;

        parentMap.put(node, parent);
        buildParentMap(node.left, node, parentMap);
        buildParentMap(node.right, node, parentMap);
    }

    // Solution 2: DFS with Distance Tracking (One Pass)
    // Time: O(n), Space: O(h) where h is height
    private List<Integer> result;

    public List<Integer> distanceKDFS(TreeNode root, TreeNode target, int k) {
        result = new ArrayList<>();
        dfs(root, target, k);
        return result;
    }

    // Returns distance from current node to target, or -1 if target not in subtree
    private int dfs(TreeNode node, TreeNode target, int k) {
        if (node == null)
            return -1;

        if (node == target) {
            // Found target, collect all nodes at distance k in subtree
            collectNodesAtDistance(node, k);
            return 0;
        }

        // Check left subtree
        int leftDist = dfs(node.left, target, k);
        if (leftDist != -1) {
            // Target found in left subtree
            if (leftDist + 1 == k) {
                result.add(node.val);
            } else if (leftDist + 1 < k) {
                // Look in right subtree for remaining distance
                collectNodesAtDistance(node.right, k - leftDist - 2);
            }
            return leftDist + 1;
        }

        // Check right subtree
        int rightDist = dfs(node.right, target, k);
        if (rightDist != -1) {
            // Target found in right subtree
            if (rightDist + 1 == k) {
                result.add(node.val);
            } else if (rightDist + 1 < k) {
                // Look in left subtree for remaining distance
                collectNodesAtDistance(node.left, k - rightDist - 2);
            }
            return rightDist + 1;
        }

        return -1; // Target not found in this subtree
    }

    private void collectNodesAtDistance(TreeNode node, int distance) {
        if (node == null || distance < 0)
            return;

        if (distance == 0) {
            result.add(node.val);
            return;
        }

        collectNodesAtDistance(node.left, distance - 1);
        collectNodesAtDistance(node.right, distance - 1);
    }

    // Solution 3: Convert Tree to Graph + BFS
    public List<Integer> distanceKGraph(TreeNode root, TreeNode target, int k) {
        List<Integer> result = new ArrayList<>();
        if (root == null)
            return result;

        // Build adjacency list
        Map<Integer, List<Integer>> graph = new HashMap<>();
        buildGraph(root, null, graph);

        // BFS from target
        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.offer(target.val);
        visited.add(target.val);

        int distance = 0;
        while (!queue.isEmpty() && distance < k) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                int curr = queue.poll();

                if (graph.containsKey(curr)) {
                    for (int neighbor : graph.get(curr)) {
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.offer(neighbor);
                        }
                    }
                }
            }
            distance++;
        }

        while (!queue.isEmpty()) {
            result.add(queue.poll());
        }

        return result;
    }

    private void buildGraph(TreeNode node, TreeNode parent, Map<Integer, List<Integer>> graph) {
        if (node == null)
            return;

        graph.putIfAbsent(node.val, new ArrayList<>());

        if (parent != null) {
            graph.get(node.val).add(parent.val);
            graph.get(parent.val).add(node.val);
        }

        buildGraph(node.left, node, graph);
        buildGraph(node.right, node, graph);
    }

    // Solution 4: Optimized DFS with Early Termination
    public List<Integer> distanceKOptimized(TreeNode root, TreeNode target, int k) {
        List<Integer> result = new ArrayList<>();
        findDistance(root, target, k, result);
        return result;
    }

    private int findDistance(TreeNode node, TreeNode target, int k, List<Integer> result) {
        if (node == null)
            return -1;

        if (node == target) {
            collectAtDistance(target, k, result);
            return 0;
        }

        int leftDist = findDistance(node.left, target, k, result);
        int rightDist = findDistance(node.right, target, k, result);

        if (leftDist != -1) {
            if (leftDist + 1 == k) {
                result.add(node.val);
            } else {
                collectAtDistance(node.right, k - leftDist - 2, result);
            }
            return leftDist + 1;
        }

        if (rightDist != -1) {
            if (rightDist + 1 == k) {
                result.add(node.val);
            } else {
                collectAtDistance(node.left, k - rightDist - 2, result);
            }
            return rightDist + 1;
        }

        return -1;
    }

    private void collectAtDistance(TreeNode node, int distance, List<Integer> result) {
        if (node == null || distance < 0)
            return;

        if (distance == 0) {
            result.add(node.val);
            return;
        }

        collectAtDistance(node.left, distance - 1, result);
        collectAtDistance(node.right, distance - 1, result);
    }

    // Utility method to create a test tree
    public static TreeNode createTestTree() {
        // 3
        // / \
        // 5 1
        // / \ / \
        // 6 2 0 8
        // / \
        // 7 4

        TreeNode root = new TreeNode(3);
        root.left = new TreeNode(5);
        root.right = new TreeNode(1);
        root.left.left = new TreeNode(6);
        root.left.right = new TreeNode(2);
        root.right.left = new TreeNode(0);
        root.right.right = new TreeNode(8);
        root.left.right.left = new TreeNode(7);
        root.left.right.right = new TreeNode(4);

        return root;
    }

    // Test method
    public static void main(String[] args) {
        NodesDistanceK solution = new NodesDistanceK();
        TreeNode root = createTestTree();
        TreeNode target = root.left; // Node with value 5
        int k = 2;

        System.out.println("Tree structure:");
        System.out.println("       3");
        System.out.println("      / \\");
        System.out.println("     5   1");
        System.out.println("    / \\ / \\");
        System.out.println("   6  2 0  8");
        System.out.println("     / \\");
        System.out.println("    7   4");
        System.out.println("\nTarget: 5, Distance: " + k);

        // Test Solution 1: Parent Map + BFS
        List<Integer> result1 = solution.distanceK(root, target, k);
        System.out.println("Solution 1 (Parent Map + BFS): " + result1);

        // Test Solution 2: DFS
        List<Integer> result2 = solution.distanceKDFS(root, target, k);
        System.out.println("Solution 2 (DFS): " + result2);

        // Test Solution 3: Graph + BFS
        List<Integer> result3 = solution.distanceKGraph(root, target, k);
        System.out.println("Solution 3 (Graph + BFS): " + result3);

        // Test edge cases
        System.out.println("\nEdge cases:");
        System.out.println("k=0 (target itself): " +
                solution.distanceK(root, target, 0));
        System.out.println("k=1 (direct neighbors): " +
                solution.distanceK(root, target, 1));
    }

}

class NodesWithDistanceK {

    static record Pair(int node, int distanceFromTarget) {
    }

    public List<Integer> distanceK(TreeNode root, TreeNode target, int k) {
        Map<Integer, Set<Integer>> map = new HashMap<>();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node.left != null) {
                map.computeIfAbsent(node.left.val, t -> new HashSet<>()).add(node.val);
                map.computeIfAbsent(node.val, t -> new HashSet<>()).add(node.left.val);
                queue.offer(node.left);
            }
            if (node.right != null) {
                map.computeIfAbsent(node.right.val, t -> new HashSet<>()).add(node.val);
                map.computeIfAbsent(node.val, t -> new HashSet<>()).add(node.right.val);
                queue.offer(node.right);
            }
        }

        List<Integer> res = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Queue<Pair> queue2 = new LinkedList<>();
        queue2.offer(new Pair(target.val, 0));
        while (!queue2.isEmpty()) {
            Pair node = queue2.poll();
            visited.add(node.node);
            if (node.distanceFromTarget == k) {
                res.add(node.node);
                continue;
            }
            for (Integer neighbor : map.getOrDefault(node.node, new HashSet<>())) {
                if (!visited.contains(neighbor)) {
                    queue2.offer(new Pair(neighbor, node.distanceFromTarget + 1));

                }
            }

        }
        return res;
    }

}

class NodesWithDistanceK2 {

    static record Pair(TreeNode node, int distanceFromTarget) {
    }

    public List<Integer> distanceK(TreeNode root, TreeNode target, int k) {
        // Build graph: Map each node to its neighbors
        Map<TreeNode, List<TreeNode>> graph = new HashMap<>();
        buildGraph(root, null, graph);

        // BFS to find all nodes at distance k
        Queue<Pair> queue = new LinkedList<>();
        Set<TreeNode> visited = new HashSet<>();
        List<Integer> result = new ArrayList<>();

        queue.offer(new Pair(target, 0));
        visited.add(target);

        while (!queue.isEmpty()) {
            Pair pair = queue.poll();
            TreeNode current = pair.node;
            int dist = pair.distanceFromTarget;

            if (dist == k) {
                result.add(current.val);
                continue;
            }

            for (TreeNode neighbor : graph.getOrDefault(current, new ArrayList<>())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(new Pair(neighbor, dist + 1));
                }
            }
        }

        return result;
    }

    private void buildGraph(TreeNode node, TreeNode parent, Map<TreeNode, List<TreeNode>> graph) {
        if (node == null)
            return;

        graph.putIfAbsent(node, new ArrayList<>());
        if (parent != null) {
            graph.get(node).add(parent);
            graph.get(parent).add(node);
        }

        buildGraph(node.left, node, graph);
        buildGraph(node.right, node, graph);
    }

}

class NodesWithDistanceK3 {

    public List<Integer> distanceK(TreeNode root, TreeNode target, int k) {
        // Step 1: Map each node to its parent
        Map<TreeNode, TreeNode> parentMap = new HashMap<>();
        buildParentMap(root, null, parentMap);

        // Step 2: BFS from target
        List<Integer> result = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        Set<TreeNode> visited = new HashSet<>();

        queue.offer(target);
        visited.add(target);

        int currentDistance = 0;

        while (!queue.isEmpty()) {
            if (currentDistance == k) {
                // Collect all node values at distance k
                for (TreeNode node : queue) {
                    result.add(node.val);
                }
                break;
            }

            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();

                // Add left, right, and parent if not visited
                if (node.left != null && visited.add(node.left)) {
                    queue.offer(node.left);
                }
                if (node.right != null && visited.add(node.right)) {
                    queue.offer(node.right);
                }
                TreeNode parent = parentMap.get(node);
                if (parent != null && visited.add(parent)) {
                    queue.offer(parent);
                }
            }

            currentDistance++;
        }

        return result;
    }

    private void buildParentMap(TreeNode node, TreeNode parent, Map<TreeNode, TreeNode> map) {
        if (node == null)
            return;
        map.put(node, parent);
        buildParentMap(node.left, node, map);
        buildParentMap(node.right, node, map);
    }

}
