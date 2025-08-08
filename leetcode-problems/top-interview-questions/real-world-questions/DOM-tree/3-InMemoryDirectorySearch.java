import java.util.ArrayList;
import java.util.List;

class PathNode {

    private final String name;
    private final boolean isFile;
    private final List<PathNode> children;

    public PathNode(String name, boolean isFile) {
        this.name = name;
        this.isFile = isFile;
        this.children = new ArrayList<>();
    }

    public void addChild(PathNode child) {
        if (!isFile) {
            children.add(child);
        }
    }

    public String getName() {
        return name;
    }

    public boolean isFile() {
        return isFile;
    }

    public List<PathNode> getChildren() {
        return children;
    }

    public boolean isDirectory() {
        return !isFile;
    }

}

class InMemoryDirectorySearch {

    public List<String> findMatchingPaths(PathNode root, String searchPhrase) {
        List<String> result = new ArrayList<>();
        this.dfs(root, searchPhrase, result, new StringBuilder());
        return result;
    }

    private void dfs(PathNode node, String searchPhrase, List<String> result, StringBuilder currentPath) {
        if (node == null) {
            return;
        }

        int originalLength = currentPath.length();

        if (originalLength > 0) {
            currentPath.append("/");
        }

        currentPath.append(node.getName());

        String path = currentPath.toString();

        if (node.isFile() && path.contains(searchPhrase)) {
            result.add(path);
        }

        if (node.isDirectory()) {
            for (PathNode child : node.getChildren()) {
                this.dfs(child, searchPhrase, result, currentPath);
            }
        }

        currentPath.setLength(originalLength);
    }

    /*
     * ### 🧠 Let:
     * 
     * `n` = total number of nodes in the tree (files + directories)
     * `d` = maximum depth of the tree
     * `L` = average length of the full path string from root to leaf
     * 
     * ---
     * 
     * ### ✅ **Time Complexity**
     * 
     * We visit each node **once** during DFS. For each file node, we:
     * 1. Build the full path using a `StringBuilder` (which takes `O(d)` time
     * worst case).
     * 2. Check if it contains the search phrase —
     * `currentPath.contains(searchPhrase)` — which takes `O(L + P)` where:
     * - `L` = length of the path string,
     * - `P` = length of the search phrase
     * 
     * So for each file node:
     * - Path string creation = `O(d)`
     * - String search = `O(L + P)`
     * 
     * There are `n` total nodes, and only some are files (say `f ≤ n`). So:
     ** 
     * Total Time Complexity** =
     * 
     * O(n) traversal + O(f × (L + P)) for string checks
     * ≈ O(n × (L + P))
     * 
     * ✅ In practice, `L` is bounded (e.g., 256–1024 chars), and `P` is small (e.g.,
     * "app"), so this is nearly linear in `n`.
     * 
     * ---
     * 
     * ### ✅ **Space Complexity**
     * 
     * **Recursive stack space**: in worst case, DFS will go `O(d)` deep (depth of
     * the tree)
     * **Result list**: stores up to `f` matching file paths → each of length up to
     * `L`
     * → `O(f × L)`
     * **PathBuilder** (`StringBuilder`) at each recursive call is reused (mutable),
     * so no multiplicative space cost.
     ** 
     * Total Space Complexity** =
     * O(d) + O(f × L)
     * 
     * ---
     * 
     * ### ✅ Summary
     * 
     * | Metric | Complexity |
     * | --------- | ---------------- |
     * | **Time** | `O(n × (L + P))` |
     * | **Space** | `O(d + f × L)` |
     * 
     */

}

// ----

class SplitPhraseSearch {

    /*
     * Key Insight:
     * - The full concatenated path string is formed by concatenating directory/file
     * names without any separators (e.g., "rootdocumentsreports").
     * 
     * - The search phrase might appear across boundaries, e.g., "documents" ends
     * with "men", "reports" starts with "ts" → phrase "mentsre" is split.
     * 
     * Approach
     * - Traverse the tree using DFS, keeping a StringBuilder of the concatenated
     * path names without any separators.
     * 
     * - When you reach a leaf, check if the concatenated string contains the search
     * phrase.
     * 
     * - To output the path nicely, keep a separate list of node names to
     * reconstruct the path with / separators.
     */

    public List<String> findPathsWithSplitPhrase(PathNode root, String searchPhrase) {
        List<String> result = new ArrayList<>();
        this.dfs(root, searchPhrase, result, new StringBuilder(), new ArrayList<>());
        return result;
    }

    private void dfs(PathNode node, String searchPhrase, List<String> result,
            StringBuilder currentPath, List<String> currentPathList) {

        if (node == null) {
            return;
        }
        int originalLength = currentPath.length();

        currentPath.append(node.getName());
        currentPathList.add(node.getName());

        if (node.isFile() && currentPath.indexOf(searchPhrase) != -1) {
            result.add(String.join("/", currentPathList));
        }

        if (node.isDirectory()) {
            for (PathNode child : node.getChildren()) {
                this.dfs(child, searchPhrase, result, currentPath, currentPathList);
            }
        }

        // Backtrack
        currentPath.setLength(originalLength);
        currentPathList.removeLast();
    }

    /*
     * Complexity
     * Time: O(n × L) where n is number of nodes and L is max length of concatenated
     * path strings at leaves (due to indexOf).
     * 
     * Space: O(d + f × L) for recursion stack, path list, and output.
     */

}
