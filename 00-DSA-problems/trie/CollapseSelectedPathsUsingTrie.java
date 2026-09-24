import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class CollapseSelectedPathsUsingTrie {

    class TrieNode {
        String name;
        boolean isFile;
        boolean isSelected;
        Map<String, TrieNode> children;

        public TrieNode(String name) {
            this.name = name;
            this.children = new HashMap<>();
        }

    }

    public List<String> getCollapsedPaths(List<String> allPaths, List<String> selectedPaths) {
        TrieNode root = new TrieNode("");

        for (String path : allPaths) {
            insert(root, path, false);
        }

        for (String path : selectedPaths) {
            insert(root, path, true);
        }

        List<String> result = new ArrayList<>();
        traverse(root, "", result);
        return result;

    }

    private boolean traverse(TrieNode node, String currentPath, List<String> result) {
        if (node.isFile) {
            return node.isSelected;
        }

        boolean allChildrenSelected = true;
        List<String> tempResults = new ArrayList<>();

        for (TrieNode child : node.children.values()) {
            boolean childSelected = traverse(child, currentPath + "/" + child.name, result);
            if (childSelected) {
                tempResults.add(currentPath + "/" + child.name);
            } else {
                allChildrenSelected = false;
            }
        }

        if (allChildrenSelected && !tempResults.isEmpty()) {
            result.removeAll(tempResults);
            result.add(currentPath + "/");
            return true;
        }
        return false;
    }

    private void insert(TrieNode root, String path, boolean markSelected) {
        String[] parts = path.split("/");
        TrieNode node = root;

        for (int i = 1; i < parts.length; i++) {
            node.children.putIfAbsent(parts[i], new TrieNode(parts[i]));
            node = node.children.get(parts[i]);
        }
        node.isFile = !path.endsWith("/") && path.contains(".");

        if (markSelected) {
            node.isSelected = true;
        }
    }

}

class DirectoryCompactorTrie {

    static class TrieNode {
        Map<String, TrieNode> children = new HashMap<>();
        boolean isFile = false; // true if this node represents a complete file path
        boolean isSelected = false; // true if this path is selected
        String fullPath = ""; // store the full path for this node

        // Check if all children are selected (for directory compaction)
        boolean allChildrenSelected() {
            if (children.isEmpty())
                return isSelected;
            return children.values().stream().allMatch(child -> child.isSelected || child.allChildrenSelected());
        }

        // Check if this node has any selected descendants
        boolean hasSelectedDescendants() {
            if (isSelected)
                return true;
            return children.values().stream().anyMatch(TrieNode::hasSelectedDescendants);
        }
    }

    public List<String> compactDirectories(List<String> allPaths, List<String> selectedPaths) {
        TrieNode root = buildTrie(allPaths, selectedPaths);
        List<String> result = new ArrayList<>();
        collectCompactPaths(root, result);
        return result;
    }

    private TrieNode buildTrie(List<String> allPaths, List<String> selectedPaths) {
        TrieNode root = new TrieNode();
        Set<String> selectedSet = new HashSet<>(selectedPaths);

        // Insert all paths into trie
        for (String path : allPaths) {
            insertPath(root, path, selectedSet.contains(path));
        }

        return root;
    }

    private void insertPath(TrieNode root, String path, boolean isSelected) {
        String[] parts = path.split("/");
        TrieNode current = root;
        StringBuilder currentPath = new StringBuilder();

        for (int i = 1; i < parts.length; i++) { // Skip empty first part from leading "/"
            String part = parts[i];
            currentPath.append("/").append(part);

            current.children.putIfAbsent(part, new TrieNode());
            current = current.children.get(part);
            current.fullPath = currentPath.toString();
        }

        current.isFile = true;
        current.isSelected = isSelected;
    }

    private void collectCompactPaths(TrieNode node, List<String> result) {
        // If this node is selected and is a file, check if we can compact
        if (node.isFile && node.isSelected) {
            result.add(node.fullPath);
            return;
        }

        // If this is a directory and all children are selected, compact to this
        // directory
        if (!node.children.isEmpty() && node.allChildrenSelected() && node.hasSelectedDescendants()) {
            result.add(node.fullPath);
            return;
        }

        // Otherwise, recursively process children
        for (TrieNode child : node.children.values()) {
            if (child.hasSelectedDescendants()) {
                collectCompactPaths(child, result);
            }
        }
    }

    // Test method
    public static void main(String[] args) {
        DirectoryCompactorTrie compactor = new DirectoryCompactorTrie();

        // Test case from the problem
        List<String> allPaths = Arrays.asList(
                "/a/b/x.txt", "/a/b/p.txt", "/a/c", "/a/d/y.txt", "/a/d/z.txt");
        List<String> selectedPaths = Arrays.asList(
                "/a/d/y.txt", "/a/d/z.txt", "/a/b/p.txt");

        List<String> result = compactor.compactDirectories(allPaths, selectedPaths);
        System.out.println("Input all paths: " + allPaths);
        System.out.println("Input selected: " + selectedPaths);
        System.out.println("Output: " + result);
        System.out.println("Expected: [/a/d, /a/b/p.txt]");

        System.out.println("\n--- Test 2: All files in directory selected ---");
        List<String> allPaths2 = Arrays.asList(
                "/root/docs/file1.txt", "/root/docs/file2.txt", "/root/other/file3.txt");
        List<String> selectedPaths2 = Arrays.asList(
                "/root/docs/file1.txt", "/root/docs/file2.txt");

        List<String> result2 = compactor.compactDirectories(allPaths2, selectedPaths2);
        System.out.println("Output: " + result2);
        System.out.println("Expected: [/root/docs]");

        System.out.println("\n--- Test 3: Mixed selection ---");
        List<String> allPaths3 = Arrays.asList(
                "/x/y/a.txt", "/x/y/b.txt", "/x/z/c.txt", "/x/z/d.txt", "/x/single.txt");
        List<String> selectedPaths3 = Arrays.asList(
                "/x/y/a.txt", "/x/y/b.txt", "/x/z/c.txt", "/x/single.txt");

        List<String> result3 = compactor.compactDirectories(allPaths3, selectedPaths3);
        System.out.println("Output: " + result3);
        System.out.println("Expected: [/x/y, /x/z/c.txt, /x/single.txt]");
    }

}

// Alternative cleaner Trie implementation
class DirectoryCompactorTrieV2 {

    static class Node {
        Map<String, Node> children = new HashMap<>();
        boolean exists = false; // This path exists in allPaths
        boolean selected = false; // This path is selected
        String path = "";

        boolean canCompact() {
            if (children.isEmpty())
                return selected;

            // Can compact if all existing children are selected or can be compacted
            return children.values().stream()
                    .filter(child -> child.exists || !child.children.isEmpty())
                    .allMatch(child -> child.selected || child.canCompact());
        }

        boolean hasSelection() {
            return selected || children.values().stream().anyMatch(Node::hasSelection);
        }
    }

    public List<String> compactDirectories(List<String> allPaths, List<String> selectedPaths) {
        Node root = new Node();
        Set<String> selectedSet = new HashSet<>(selectedPaths);

        // Build trie
        for (String path : allPaths) {
            insert(root, path, selectedSet.contains(path));
        }

        // Collect results using DFS
        List<String> result = new ArrayList<>();
        dfs(root, result);
        return result;
    }

    private void insert(Node root, String path, boolean isSelected) {
        String[] parts = path.substring(1).split("/"); // Remove leading "/"
        Node curr = root;
        StringBuilder sb = new StringBuilder();

        for (String part : parts) {
            sb.append("/").append(part);
            curr.children.putIfAbsent(part, new Node());
            curr = curr.children.get(part);
            curr.path = sb.toString();
        }

        curr.exists = true;
        curr.selected = isSelected;
    }

    private void dfs(Node node, List<String> result) {
        if (node.exists && node.selected) {
            result.add(node.path);
            return;
        }

        // If this directory can be compacted (all children selected)
        if (!node.path.isEmpty() && !node.children.isEmpty() &&
                node.canCompact() && node.hasSelection()) {
            result.add(node.path);
            return;
        }

        // Recurse to children
        for (Node child : node.children.values()) {
            if (child.hasSelection()) {
                dfs(child, result);
            }
        }
    }
}

class DirectoryCompactor {

    public List<String> compactDirectories(List<String> allPaths, List<String> selectedPaths) {
        Set<String> selectedSet = new HashSet<>(selectedPaths);
        Map<String, Set<String>> directoryContents = new HashMap<>();

        // Build directory structure - map each directory to its direct children
        for (String path : allPaths) {
            String parent = getParentDirectory(path);
            String child = getFileName(path);

            directoryContents.computeIfAbsent(parent, k -> new HashSet<>()).add(child);
        }

        // Find the most compact representation
        Set<String> result = new HashSet<>();
        Set<String> processed = new HashSet<>();

        for (String selectedPath : selectedPaths) {
            if (processed.contains(selectedPath)) {
                continue;
            }

            String compactPath = findMostCompactPath(selectedPath, selectedSet, directoryContents, allPaths);
            result.add(compactPath);

            // Mark all paths covered by this compact path as processed
            markProcessedPaths(compactPath, allPaths, processed);
        }

        return new ArrayList<>(result);
    }

    private String findMostCompactPath(String path, Set<String> selectedSet,
            Map<String, Set<String>> directoryContents,
            List<String> allPaths) {
        String currentPath = path;

        // Keep going up the directory tree while all siblings are selected
        while (true) {
            String parent = getParentDirectory(currentPath);
            if (parent.equals(currentPath)) { // Reached root
                break;
            }

            // Get all direct children of parent directory
            Set<String> siblings = new HashSet<>();
            for (String allPath : allPaths) {
                if (getParentDirectory(allPath).equals(parent)) {
                    siblings.add(allPath);
                }
            }

            // Check if all siblings are selected
            boolean allSiblingsSelected = true;
            for (String sibling : siblings) {
                if (!selectedSet.contains(sibling)) {
                    allSiblingsSelected = false;
                    break;
                }
            }

            if (allSiblingsSelected && siblings.size() > 1) {
                // All siblings are selected, so we can represent them with parent directory
                currentPath = parent;
            } else {
                // Not all siblings are selected, current path is the most compact
                break;
            }
        }

        return currentPath;
    }

    private void markProcessedPaths(String compactPath, List<String> allPaths, Set<String> processed) {
        for (String path : allPaths) {
            if (path.startsWith(compactPath + "/") || path.equals(compactPath)) {
                processed.add(path);
            }
        }
    }

    private String getParentDirectory(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return path.substring(0, lastSlash);
    }

    private String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return path.substring(lastSlash + 1);
    }

    // Test method
    public static void main(String[] args) {
        DirectoryCompactor compactor = new DirectoryCompactor();

        // Test case from the problem
        List<String> allPaths = Arrays.asList(
                "/a/b/x.txt", "/a/b/p.txt", "/a/c", "/a/d/y.txt", "/a/d/z.txt");
        List<String> selectedPaths = Arrays.asList(
                "/a/d/y.txt", "/a/d/z.txt", "/a/b/p.txt");

        List<String> result = compactor.compactDirectories(allPaths, selectedPaths);
        System.out.println("Input all paths: " + allPaths);
        System.out.println("Input selected: " + selectedPaths);
        System.out.println("Output: " + result);
        System.out.println("Expected: [/a/d, /a/b/p.txt]");

        // Additional test case
        System.out.println("\n--- Additional Test ---");
        List<String> allPaths2 = Arrays.asList(
                "/root/folder1/file1.txt", "/root/folder1/file2.txt",
                "/root/folder2/file3.txt", "/root/folder2/file4.txt",
                "/root/single.txt");
        List<String> selectedPaths2 = Arrays.asList(
                "/root/folder1/file1.txt", "/root/folder1/file2.txt",
                "/root/folder2/file3.txt", "/root/single.txt");

        List<String> result2 = compactor.compactDirectories(allPaths2, selectedPaths2);
        System.out.println("Input all paths: " + allPaths2);
        System.out.println("Input selected: " + selectedPaths2);
        System.out.println("Output: " + result2);
        System.out.println("Expected: [/root/folder1, /root/folder2/file3.txt, /root/single.txt]");

        // Test case where everything is selected
        System.out.println("\n--- All Selected Test ---");
        List<String> result3 = compactor.compactDirectories(allPaths, allPaths);
        System.out.println("All paths selected output: " + result3);
    }

}

// Alternative cleaner implementation
class DirectoryCompactorV2 {

    public List<String> compactDirectories(List<String> allPaths, List<String> selectedPaths) {
        Set<String> selected = new HashSet<>(selectedPaths);
        Set<String> result = new HashSet<>();
        Set<String> covered = new HashSet<>();

        for (String path : selectedPaths) {
            if (covered.contains(path))
                continue;

            String compact = findCompactPath(path, allPaths, selected);
            result.add(compact);

            // Mark all paths under this compact path as covered
            for (String p : allPaths) {
                if (p.equals(compact) || p.startsWith(compact + "/")) {
                    covered.add(p);
                }
            }
        }

        return new ArrayList<>(result);
    }

    private String findCompactPath(String path, List<String> allPaths, Set<String> selected) {
        while (true) {
            String parent = getParent(path);
            if (parent.equals(path))
                break; // reached root

            // Get all items directly under parent
            List<String> siblings = new ArrayList<>();
            for (String p : allPaths) {
                if (getParent(p).equals(parent)) {
                    siblings.add(p);
                }
            }

            // Check if all siblings are selected
            boolean allSelected = siblings.size() > 1 && siblings.stream().allMatch(selected::contains);

            if (allSelected) {
                path = parent; // Move up one level
            } else {
                break; // Can't compact further
            }
        }

        return path;
    }

    private String getParent(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash <= 0 ? "/" : path.substring(0, lastSlash);
    }

}
