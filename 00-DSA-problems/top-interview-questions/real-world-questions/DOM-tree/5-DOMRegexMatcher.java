import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface Node {

    List<Node> getChildren();

}

class TextNode implements Node {

    String text;

    public TextNode(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    @Override
    public List<Node> getChildren() {
        return Collections.emptyList();
    }

}

class ElementNode implements Node {

    List<Node> children = new ArrayList<>();

    public ElementNode(Node... nodes) {
        children.addAll(List.of(nodes));
    }

    @Override
    public List<Node> getChildren() {
        return this.children;
    }

}

class TextRange {

    TextNode node;
    int start;
    int end;

    TextRange(TextNode node, int start, int end) {
        this.node = node;
        this.start = start;
        this.end = end;
    }

}

class DOMRegexMatcher {

    public List<TextNode> findMatchingTextNodes(Node root, Pattern pattern) {
        List<TextRange> ranges = new ArrayList<>();

        StringBuilder fullText = new StringBuilder();

        // Step 1: DFS and build full text with offset tracking
        this.dfs(root, fullText, ranges);

        Matcher matcher = pattern.matcher(fullText);
        Set<TextNode> result = new LinkedHashSet<>();

        while (matcher.find()) {
            int matchStart = matcher.start();
            int matchEnd = matcher.end();

            for (TextRange range : ranges) {
                if (range.end > matchStart && range.start < matchEnd) {
                    result.add(range.node);
                }
            }
        }

        return new ArrayList<>(result);
    }

    private void dfs(Node node, StringBuilder fullText, List<TextRange> ranges) {
        if (node instanceof TextNode textNode) {
            int start = fullText.length();
            fullText.append(textNode.getText());
            int end = fullText.length();

            ranges.add(new TextRange(textNode, start, end));
        } else {
            for (Node child : node.getChildren()) {
                this.dfs(child, fullText, ranges);
            }
        }
    }

    public void main(String[] args) {
        Node dom = new ElementNode(
                new TextNode("This "),
                new ElementNode(
                        new TextNode("is "),
                        new ElementNode(
                                new TextNode("some "),
                                new TextNode("code."))));

        Pattern pattern = Pattern.compile("is.*code", Pattern.CASE_INSENSITIVE);
        DOMRegexMatcher domRegexMatcher = new DOMRegexMatcher();
        List<TextNode> matchedNodes = domRegexMatcher.findMatchingTextNodes(dom, pattern);

        for (TextNode node : matchedNodes) {
            System.out.println("Matched: " + node);
        }
    }

    /*
     * ✅ Time & Space Complexity
     * Let:
     * - N = total number of nodes
     * - T = total characters in all text nodes
     * - M = number of matches
     * 
     * Time:
     * - DFS traversal: O(N)
     * - Building StringBuilder: O(T)
     * - Regex matching: ~O(T) for most patterns
     * - Mapping match ranges to nodes: O(M * N) worst case
     * 
     * Space:
     * - O(T) for full text
     * - O(N) for node ranges
     * - O(M) for result
     */

    private static void addMatchingTextNodes(List<TextRange> ranges, int matchStart, int matchEnd,
            Set<TextNode> result) {
        int idx = binarySearch(ranges, matchStart);
        // Iterate forward until we go past matchEnd
        while (idx < ranges.size()) {
            TextRange range = ranges.get(idx);
            if (range.start >= matchEnd)
                break;
            if (range.end > matchStart) {
                result.add(range.node);
            }
            idx++;
        }
    }

    // Find the first range where end > matchStart
    private static int binarySearch(List<TextRange> ranges, int matchStart) {
        int left = 0, right = ranges.size() - 1, result = ranges.size();
        while (left <= right) {
            int mid = (left + right) / 2;
            if (ranges.get(mid).end > matchStart) {
                result = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        return result;
    }

    /*
     * ✅ Updated Time Complexity
     * Let:
     * - N = number of text nodes
     * - T = total text length
     * - M = number of regex matches
     * - K = number of contributing nodes
     * 
     * Step - Time
     * DFS - traversal O(N)
     * Build - full text O(T)
     * Regex - match O(T)
     * Map - matches to nodes O(M log N + K) ✅ optimized
     * 
     * So overall:
     * O(T + N + M log N + K)
     * Space: O(T + N + K)
     */

}
