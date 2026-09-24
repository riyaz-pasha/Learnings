/*
 * ✅ Problem Statement
 * Given:
 * - A DOM tree (Node root)
 * - A target subtree (Node target)
 * 
 * Determine if any subtree of root matches exactly with target in:
 * - tag names
 * - attributes (if applicable)
 * - text content
 * - child structure and order
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

enum Tag {
}

class Node {

    Tag tag;
    String text;
    List<Node> children;

}

class DOMMatcher {

    public boolean isSubtree(Node root, Node target) {
        if (root == null) {
            return false;
        }

        if (this.isSameTree(root, target)) {
            return true;
        }

        for (Node child : root.children) {
            if (this.isSubtree(child, target)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSameTree(Node node1, Node node2) {
        if (node1 == null && node2 == null) {
            return true;
        }

        if (node1 == null || node2 == null) {
            return false;
        }

        if (!node1.tag.equals(node2.tag) || !node1.text.equals(node2.text)
                || node1.children.size() != node2.children.size()) {
            return false;
        }

        for (int index = 0; index < node1.children.size(); index++) {
            if (!this.isSameTree(node1.children.get(index), node2.children.get(index))) {
                return false;
            }
        }

        return true;
    }

    /*
     * Time Complexity:
     * - Let n be the number of nodes in the DOM (root)
     * - Let m be the number of nodes in the target
     * - In the worst case, we compare m nodes at each of the n nodes
     * - O(n * m) time
     * - O(h) space due to recursion stack, where h is tree height
     */

}

class DOMMatcherOptimized {

    private Map<String, List<Node>> hashToNodesMap = new HashMap<>();

    public boolean isSubtree(Node root, Node target) {
        if (root == null) {
            return false;
        }

        this.computeHash(root);
        String targetHash = this.computeHash(target);

        for (Node candidate : this.hashToNodesMap.get(targetHash)) {
            if (candidate.equals(target)) {
                continue;
            }
            if (this.isSameTree(candidate, target)) {
                return true;
            }

        }

        return false;
    }

    private boolean isSameTree(Node node1, Node node2) {
        if (node1 == null && node2 == null) {
            return true;
        }
        if (node1 == null || node2 == null) {
            return false;
        }

        if (!node1.tag.equals(node2.tag) || !node1.text.equals(node2)
                || node1.children.size() != node2.children.size()) {
            return false;
        }

        for (int index = 0; index < node1.children.size(); index++) {
            if (!this.isSameTree(node1.children.get(index), node2.children.get(index))) {
                return false;
            }
        }

        return true;
    }

    private String computeHash(Node node) {
        if (node == null) {
            return "#";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<").append(node.tag).append(">");
        sb.append(node.text);

        for (Node child : node.children) {
            sb.append(this.computeHash(child));
        }

        sb.append("</").append(node.tag).append(">");

        String hash = sb.toString();

        this.hashToNodesMap.computeIfAbsent(hash, _ -> new ArrayList<>())
                .add(node);

        return hash;
    }

    /*
     * Time Complexity:
     * - Let n be the number of nodes in the main DOM
     * - Let m be the number of nodes in the target subtree
     * - Hashing time: O(n) for root, O(m) for target
     * - Lookup time: constant + worst-case O(k * m) comparisons if hash collision
     * occurs for k nodes
     * - Total: O(n + m + k * m) — much faster in practice due to pruning via hash
     * 
     * Space Complexity:
     * - O(n) for storing subtree hashes
     */

}

class OptimalMatcher {

    private final Map<Node, Long> nodeHashes = new HashMap<>();

    /**
     * Finds if the small subtree is an exact match for any subtree in the main DOM.
     * 
     * @param domRoot     The root of the main DOM tree.
     * @param snippetRoot The root of the small subtree snippet.
     * @return true if a match is found, false otherwise.
     */
    public boolean findMatch(Node domRoot, Node snippetRoot) {
        // Step 1: Pre-compute hashes for all subtrees in the DOM
        computeHashes(domRoot);

        // Step 2: Compute hash for the snippet
        long snippetHash = computeHash(snippetRoot);

        // Step 3: Iterate and compare hashes
        return nodeHashes.values().stream()
                .anyMatch(hash -> hash == snippetHash);

        // Note: A full node-by-node verification step is recommended
        // to prevent false positives from hash collisions. For simplicity,
        // this example omits the full verification, but it is
        // critical for a production-level solution.
    }

    /**
     * Computes and stores hash for a node and its children recursively (post-order
     * traversal).
     * 
     * @param node The current node to process.
     * @return The hash value of the current subtree.
     */
    private long computeHashes(Node node) {
        if (node == null) {
            return 0;
        }

        // Compute hashes for all children first (post-order traversal)
        List<Long> childHashes = node.children.stream()
                .map(this::computeHashes)
                .toList();

        // Compute and store the hash for the current node
        long hash = simpleHash(node, childHashes);
        nodeHashes.put(node, hash);
        return hash;
    }

    /**
     * Computes the hash for a single subtree recursively.
     * 
     * @param node The root of the subtree.
     * @return The hash value of the subtree.
     */
    private long computeHash(Node node) {
        if (node == null) {
            return 0;
        }

        List<Long> childHashes = node.children.stream()
                .map(this::computeHash)
                .toList();

        return simpleHash(node, childHashes);
    }

    /**
     * A simple rolling hash function combining properties and child hashes.
     * This is a basic example; a more robust one would use cryptographic
     * or polynomial rolling hash for better collision resistance.
     */
    private long simpleHash(Node node, List<Long> childHashes) {
        long result = Objects.hash(node.tag, node.text);
        for (long childHash : childHashes) {
            result = 31 * result + childHash;
        }
        return result;
    }

}
