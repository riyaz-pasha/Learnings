import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Solution {

    private List<List<Integer>> children;
    private int n;
    private long maxScore;
    private int count;

    public int countHighestScoreNodes(int[] parents) {
        n = parents.length;
        children = new ArrayList<>();
        maxScore = 0;
        count = 0;

        // Build adjacency list for children
        for (int i = 0; i < n; i++) {
            children.add(new ArrayList<>());
        }

        for (int i = 1; i < n; i++) {
            children.get(parents[i]).add(i);
        }

        // DFS to calculate subtree sizes and scores
        dfs(0);

        return count;
    }

    private int dfs(int node) {
        long score = 1;
        int subtreeSize = 1;

        // Calculate score considering each child subtree
        for (int child : children.get(node)) {
            int childSize = dfs(child);
            score *= childSize;
            subtreeSize += childSize;
        }

        // Consider the remaining part of tree (above current node)
        // This is only relevant if current node is not root
        if (node != 0) {
            int remainingSize = n - subtreeSize;
            score *= remainingSize;
        }

        // Update maximum score and count
        if (score > maxScore) {
            maxScore = score;
            count = 1;
        } else if (score == maxScore) {
            count++;
        }

        return subtreeSize;
    }

}

// Alternative solution with cleaner structure
class SolutionAlternative {

    public int countHighestScoreNodes(int[] parents) {
        int n = parents.length;
        List<List<Integer>> children = new ArrayList<>();

        // Initialize children lists
        for (int i = 0; i < n; i++) {
            children.add(new ArrayList<>());
        }

        // Build the tree
        for (int i = 1; i < n; i++) {
            children.get(parents[i]).add(i);
        }

        // Calculate subtree sizes first
        int[] subtreeSize = new int[n];
        calculateSubtreeSize(0, children, subtreeSize);

        // Calculate scores and find maximum
        long maxScore = 0;
        int count = 0;

        for (int i = 0; i < n; i++) {
            long score = calculateScore(i, children, subtreeSize, n);
            if (score > maxScore) {
                maxScore = score;
                count = 1;
            } else if (score == maxScore) {
                count++;
            }
        }

        return count;
    }

    private int calculateSubtreeSize(int node, List<List<Integer>> children, int[] subtreeSize) {
        int size = 1;
        for (int child : children.get(node)) {
            size += calculateSubtreeSize(child, children, subtreeSize);
        }
        subtreeSize[node] = size;
        return size;
    }

    private long calculateScore(int node, List<List<Integer>> children, int[] subtreeSize, int n) {
        long score = 1;

        // Score from child subtrees
        for (int child : children.get(node)) {
            score *= subtreeSize[child];
        }

        // Score from remaining part (above current node)
        if (node != 0) {
            int remainingSize = n - subtreeSize[node];
            score *= remainingSize;
        }

        return score;
    }

}

// Test class
class TestSolution {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int[] parents1 = { -1, 2, 0, 2, 0 };
        System.out.println("Test 1: " + solution.countHighestScoreNodes(parents1)); // Expected: 3

        // Test case 2
        int[] parents2 = { -1, 2, 0 };
        System.out.println("Test 2: " + solution.countHighestScoreNodes(parents2)); // Expected: 2

        // Test case 3 - single node
        int[] parents3 = { -1 };
        System.out.println("Test 3: " + solution.countHighestScoreNodes(parents3)); // Expected: 1
    }

}

class Solution2 {

    public int countHighestScoreNodes(int[] parents) {
        TreeProcessor processor = new TreeProcessor(parents);
        return processor.countNodesWithHighestScore();
    }

}

class TreeProcessor {

    private final int n;
    private final List<List<Integer>> children;
    private final int[] subtreeSizes;

    public TreeProcessor(int[] parents) {
        this.n = parents.length;
        this.children = buildTree(parents);
        this.subtreeSizes = new int[n];
        calculateAllSubtreeSizes();
    }

    public int countNodesWithHighestScore() {
        ScoreTracker tracker = new ScoreTracker();

        for (int node = 0; node < n; node++) {
            long score = calculateNodeScore(node);
            tracker.update(score);
        }

        return tracker.getCount();
    }

    private List<List<Integer>> buildTree(int[] parents) {
        List<List<Integer>> tree = new ArrayList<>();

        // Initialize empty lists for each node
        for (int i = 0; i < n; i++) {
            tree.add(new ArrayList<>());
        }

        // Build parent-child relationships
        for (int i = 1; i < n; i++) {
            tree.get(parents[i]).add(i);
        }

        return tree;
    }

    private void calculateAllSubtreeSizes() {
        calculateSubtreeSize(0); // Start from root
    }

    private int calculateSubtreeSize(int node) {
        int size = 1; // Count the node itself

        // Add sizes of all child subtrees
        for (int child : children.get(node)) {
            size += calculateSubtreeSize(child);
        }

        subtreeSizes[node] = size;
        return size;
    }

    private long calculateNodeScore(int node) {
        long score = 1;

        // Multiply by size of each child subtree
        score = multiplyChildSubtrees(node, score);

        // Multiply by remaining tree size (if not root)
        score = multiplyRemainingTree(node, score);

        return score;
    }

    private long multiplyChildSubtrees(int node, long score) {
        for (int child : children.get(node)) {
            score *= subtreeSizes[child];
        }
        return score;
    }

    private long multiplyRemainingTree(int node, long score) {
        if (node != 0) { // Not the root
            int remainingSize = n - subtreeSizes[node];
            score *= remainingSize;
        }
        return score;
    }

}

class ScoreTracker {

    private long maxScore = 0;
    private int count = 0;

    public void update(long score) {
        if (score > maxScore) {
            maxScore = score;
            count = 1;
        } else if (score == maxScore) {
            count++;
        }
    }

    public int getCount() {
        return count;
    }

    public long getMaxScore() {
        return maxScore;
    }

}

// Even more functional approach
class FunctionalSolution {

    public int countHighestScoreNodes(int[] parents) {
        TreeAnalyzer analyzer = new TreeAnalyzer(parents);
        Map<Integer, Long> nodeScores = analyzer.calculateAllScores();
        return ScoreAnalyzer.countMaxScoreNodes(nodeScores);
    }

}

class TreeAnalyzer {

    private final int n;
    private final Map<Integer, List<Integer>> adjacencyList;

    public TreeAnalyzer(int[] parents) {
        this.n = parents.length;
        this.adjacencyList = TreeBuilder.buildFromParents(parents);
    }

    public Map<Integer, Long> calculateAllScores() {
        Map<Integer, Integer> subtreeSizes = SubtreeSizeCalculator.calculate(adjacencyList, n);
        return ScoreCalculator.calculateScores(adjacencyList, subtreeSizes, n);
    }

}

class TreeBuilder {

    public static Map<Integer, List<Integer>> buildFromParents(int[] parents) {
        Map<Integer, List<Integer>> tree = new HashMap<>();

        // Initialize
        for (int i = 0; i < parents.length; i++) {
            tree.put(i, new ArrayList<>());
        }

        // Build relationships
        for (int i = 1; i < parents.length; i++) {
            tree.get(parents[i]).add(i);
        }

        return tree;
    }

}

class SubtreeSizeCalculator {

    public static Map<Integer, Integer> calculate(Map<Integer, List<Integer>> tree, int n) {
        Map<Integer, Integer> sizes = new HashMap<>();
        calculateDFS(0, tree, sizes);
        return sizes;
    }

    private static int calculateDFS(int node, Map<Integer, List<Integer>> tree, Map<Integer, Integer> sizes) {
        int size = 1;

        for (int child : tree.get(node)) {
            size += calculateDFS(child, tree, sizes);
        }

        sizes.put(node, size);
        return size;
    }

}

class ScoreCalculator {

    public static Map<Integer, Long> calculateScores(
            Map<Integer, List<Integer>> tree,
            Map<Integer, Integer> subtreeSizes,
            int totalNodes) {

        Map<Integer, Long> scores = new HashMap<>();

        for (int node = 0; node < totalNodes; node++) {
            long score = calculateNodeScore(node, tree, subtreeSizes, totalNodes);
            scores.put(node, score);
        }

        return scores;
    }

    private static long calculateNodeScore(
            int node,
            Map<Integer, List<Integer>> tree,
            Map<Integer, Integer> subtreeSizes,
            int totalNodes) {

        long score = 1;

        // Child subtrees
        for (int child : tree.get(node)) {
            score *= subtreeSizes.get(child);
        }

        // Remaining tree (if not root)
        if (node != 0) {
            int remainingSize = totalNodes - subtreeSizes.get(node);
            score *= remainingSize;
        }

        return score;
    }

}

class ScoreAnalyzer {

    public static int countMaxScoreNodes(Map<Integer, Long> nodeScores) {
        long maxScore = nodeScores.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        return (int) nodeScores.values().stream()
                .mapToLong(Long::longValue)
                .filter(score -> score == maxScore)
                .count();
    }

}

// Test class
class TestRefactoredSolution {

    public static void main(String[] args) {
        Solution solution = new Solution();
        FunctionalSolution functionalSolution = new FunctionalSolution();

        // Test case 1
        int[] parents1 = { -1, 2, 0, 2, 0 };
        System.out.println("OOP Solution - Test 1: " + solution.countHighestScoreNodes(parents1));
        System.out.println("Functional Solution - Test 1: " + functionalSolution.countHighestScoreNodes(parents1));

        // Test case 2
        int[] parents2 = { -1, 2, 0 };
        System.out.println("OOP Solution - Test 2: " + solution.countHighestScoreNodes(parents2));
        System.out.println("Functional Solution - Test 2: " + functionalSolution.countHighestScoreNodes(parents2));
    }

}

// Step 1: Build the Tree
// Convert the parent array into an adjacency list to efficiently find all
// children of a node.
// This helps in traversing the tree using DFS.

// Step 2: DFS Traversal
// Perform a depth-first search starting from the root (usually node 0) to:
// - Calculate the size of each subtree rooted at every node
// - Calculate the score for each node during the traversal

// Step 3: Score Calculation
// For each node, calculate the score as follows:
// - Score = Product of sizes of all child subtrees
// - Multiply by the size of the "remaining tree" (i.e., totalNodes - size of
// current subtree)
// which represents the part of the tree not in the current node's subtree

// Step 4: Track Maximum Score
// During DFS, keep track of:
// - The maximum score found so far
// - How many nodes achieve this maximum score

// Time Complexity: O(n)
// We visit each node exactly once during DFS
// Each node's score calculation is O(degree), which is at most 2 for binary
// trees

// Space Complexity: O(n)
// Space for the adjacency list representation
// Recursion stack depth in worst case
