/*
 * We have:
 * - An XML-like tree — each node can have children and some text.
 * - We want to check if a word (e.g. "machine") is a subsequence of the
 * concatenated leaf text content (DFS order).
 * - Subsequence means characters appear in order, but not necessarily
 * contiguously.
 * 
 * xml Tree:
 * <root>
 * - <p>ma</p>
 * - <div>
 * - - <span>ch</span>
 * - - <span>i</span>
 * - </div>
 * - <p>ne</p>
 * </root>
 * 
 * Leaf text concatenated: "ma" + "ch" + "i" + "ne" = "machine"
 * Target word: "machine"
 * It’s a subsequence since every letter appears in order.
 */

import java.util.ArrayList;
import java.util.List;

class Node {

    String text;
    List<Node> children;

    public boolean isLeaf() {
        return children == null || children.isEmpty();
    }

}

class SubsequenceInXMLTree {

    public boolean isSubsequence(Node root, String searchWord) {
        String leafText = this.collectLeafText(root);
        return this.isSubsequence(leafText, searchWord);
    }

    private boolean isSubsequence(String text, String target) {
        int i = 0, j = 0;
        while (i < text.length() && j < target.length()) {
            if (text.charAt(i) == target.charAt(j)) {
                j++;
            }
            i++;
        }
        return j == target.length();
    }

    private String collectLeafText(Node root) {
        StringBuilder leafText = new StringBuilder();
        this.dfs(root, leafText);
        return leafText.toString();
    }

    private void dfs(Node node, StringBuilder leafText) {
        if (node.isLeaf()) {
            if (node.text != null) {
                leafText.append(node.text);
            }
            return;
        }
        for (Node child : node.children) {
            this.dfs(child, leafText);
        }
    }

    /*
     * Complexity Analysis (Brute Force)
     * Let:
     * - n = total number of characters in all leaf texts
     * - m = length of word
     * - L = number of leaf nodes
     * 
     * Steps:
     * - DFS collection → O(n) time, O(n) space (string builder)
     * - Subsequence check → O(n + m) time, O(1) space
     * 
     * Total Time: O(n + m)
     * Space: O(n) extra for concatenated string
     */

}

class SubsequenceCheckerOptimal {

    private int targetIndex = 0;

    public boolean isSubsequence(Node root, String target) {
        this.targetIndex = 0;
        this.findSubsequence(root, target);
        return this.targetIndex == target.length();
    }

    private void findSubsequence(Node node, String target) {
        if (node == null || targetIndex == target.length()) {
            return;
        }

        if (node.isLeaf()) {
            if (node.text != null) {
                for (char ch : node.text.toCharArray()) {
                    if (targetIndex < target.length() && ch == target.charAt(targetIndex)) {
                        targetIndex++;
                    }
                }
            }
            return;
        }

        for (Node child : node.children) {
            this.findSubsequence(child, target);
        }
    }

    /*
     * Complexity Analysis (Optimal)
     * - Time: O(n) — each character of leaves visited once, stop early if matched.
     * - Space: O(h) recursion stack, where h is tree height(no large string build).
     * 
     * Compared to brute force:
     * - Memory is better: No full concatenated string stored.
     * - Early stopping: Saves time if match is found early.
     */

}

class SubsequenceCheckerWithNodes {

    private static int targetIndex = 0;

    public static List<Node> findMatchedNodes(Node root, String target) {
        targetIndex = 0;
        List<Node> matchedNodes = new ArrayList<>();
        findSubsequenceWithNodes(root, target, matchedNodes);
        if (targetIndex == target.length()) {
            return matchedNodes;
        }
        return new ArrayList<>(); // Return empty list if no full match
    }

    private static void findSubsequenceWithNodes(Node node, String target, List<Node> matchedNodes) {
        if (node == null || targetIndex == target.length()) {
            return;
        }

        if (node.children.isEmpty()) {
            boolean nodeAdded = false;
            for (char c : node.text.toCharArray()) {
                if (targetIndex < target.length() && c == target.charAt(targetIndex)) {
                    targetIndex++;
                    if (!nodeAdded) {
                        matchedNodes.add(node);
                        nodeAdded = true;
                    }
                }
            }
            return;
        }

        for (Node child : node.children) {
            findSubsequenceWithNodes(child, target, matchedNodes);
        }
    }

}