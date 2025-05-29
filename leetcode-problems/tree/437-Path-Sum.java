import java.util.HashMap;
import java.util.Map;

class PathSum {
    public int pathSum(TreeNode root, int targetSum) {
        // Map to store frequency of prefix sums
        Map<Long, Integer> prefixSumCount = new HashMap<>();
        prefixSumCount.put(0L, 1); // Base case: empty path has sum 0

        return dfs(root, 0L, targetSum, prefixSumCount);
    }

    private int dfs(TreeNode node, long currentSum, int targetSum,
            Map<Long, Integer> prefixSumCount) {
        if (node == null)
            return 0;

        // Update current path sum
        currentSum += node.val;

        // Check how many paths ending at current node have target sum
        // If currentSum - targetSum exists in map, those paths + current node =
        // targetSum
        int pathCount = prefixSumCount.getOrDefault(currentSum - targetSum, 0);

        // Add current sum to map
        prefixSumCount.put(currentSum, prefixSumCount.getOrDefault(currentSum, 0) + 1);

        // Recursively count paths in left and right subtrees
        pathCount += dfs(node.left, currentSum, targetSum, prefixSumCount);
        pathCount += dfs(node.right, currentSum, targetSum, prefixSumCount);

        // Backtrack: remove current sum from map
        prefixSumCount.put(currentSum, prefixSumCount.get(currentSum) - 1);

        return pathCount;
    }
}
