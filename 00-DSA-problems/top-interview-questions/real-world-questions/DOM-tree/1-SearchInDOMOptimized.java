import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

class Node {

    String text;
    List<Node> children;

    // Constructor for easier testing
    public Node(String text) {
        this.text = text;
        this.children = new ArrayList<>();
    }

}

class SearchInDOMOptimized {

    private String pattern;
    private int[] lps;
    private List<List<Node>> result;
    private int patternIndex;
    private LinkedHashSet<Node> currentMatchNodes;

    public List<List<Node>> getMatchingNodes(Node root, String searchText) {
        this.pattern = searchText;
        this.lps = this.buildLPS(searchText);
        this.result = new ArrayList<>();
        this.patternIndex = 0;
        this.currentMatchNodes = new LinkedHashSet<>();

        this.dfs(root);

        return result;
    }

    private void dfs(Node node) {
        if (node.text != null) {
            for (int i = 0; i < node.text.length(); i++) {
                char ch = node.text.charAt(i);
                this.matchChar(ch, node);
            }
        }

        if (node.children != null) {
            for (Node child : node.children) {
                this.dfs(child);
            }
        }
    }

    private void matchChar(char ch, Node node) {
        char target = this.pattern.charAt(this.patternIndex);

        while (this.patternIndex > 0 && ch != target) {
            this.patternIndex = this.lps[this.patternIndex - 1];
            this.trimNodes(this.patternIndex);
            target = this.pattern.charAt(this.patternIndex);
        }

        if (ch == target) {
            this.currentMatchNodes.add(node);
            this.patternIndex++;

            if (this.patternIndex == this.pattern.length()) {
                result.add(new ArrayList<>(this.currentMatchNodes));
                this.patternIndex = this.lps[this.patternIndex - 1];
                this.trimNodes(this.patternIndex);
            }
        } else {
            this.currentMatchNodes.clear();
        }
    }

    private void trimNodes(int requiredSize) {
        while (this.currentMatchNodes.size() > requiredSize) {
            Iterator<Node> iterator = this.currentMatchNodes.iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private int[] buildLPS(String searchText) {
        int searchLength = searchText.length();
        int[] lps = new int[searchLength];

        int length = 0;
        int index = 1;
        lps[0] = 0;

        while (index < searchLength) {
            if (searchText.charAt(index) == searchText.charAt(length)) {
                lps[index++] = ++length;
            } else {
                if (length != 0) {
                    length = lps[length - 1];
                } else {
                    lps[index++] = 0;
                }
            }
        }

        return lps;
    }

}
