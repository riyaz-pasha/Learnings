import java.util.ArrayList;
import java.util.List;

// Represents a simplified DOM tree node
class Node {
    String text;
    List<Node> children;

    public Node(String text) {
        this.text = text;
        this.children = new ArrayList<>();
    }
}

// Maps a range of characters in the flattened string to a specific node
class NodeMapping {
    int start;
    int end;
    Node node;

    public NodeMapping(int start, int end, Node node) {
        this.start = start;
        this.end = end;
        this.node = node;
    }
}

public class DomSearchEngine {

    private String flattenedText;
    private List<NodeMapping> nodeMappings;

    public DomSearchEngine() {
        this.flattenedText = "";
        this.nodeMappings = new ArrayList<>();
    }

    // Preprocesses the DOM tree to create a flattened text and an index
    public void preprocess(Node root) {
        StringBuilder sb = new StringBuilder();
        nodeMappings = new ArrayList<>();
        traverseAndIndex(root, sb);
        this.flattenedText = sb.toString();
    }

    private void traverseAndIndex(Node node, StringBuilder sb) {
        if (node == null) {
            return;
        }

        // If it's a leaf node, append its text and create a mapping
        if (node.children.isEmpty() && node.text != null && !node.text.isEmpty()) {
            int start = sb.length();
            sb.append(node.text);
            int end = sb.length() - 1;
            nodeMappings.add(new NodeMapping(start, end, node));
            return;
        }

        // Recursively traverse children
        for (Node child : node.children) {
            traverseAndIndex(child, sb);
        }
    }

    // Searches for a phrase and returns a list of nodes where it's found
    public List<Node> search(String phrase) {
        if (flattenedText == null || phrase == null || phrase.isEmpty()) {
            return new ArrayList<>();
        }

        int startIndex = flattenedText.indexOf(phrase);
        if (startIndex == -1) {
            return new ArrayList<>(); // Phrase not found
        }

        int endIndex = startIndex + phrase.length() - 1;
        List<Node> resultNodes = new ArrayList<>();

        for (NodeMapping mapping : nodeMappings) {
            // Check if the phrase range overlaps with the node's text range
            if (mapping.start <= endIndex && mapping.end >= startIndex) {
                resultNodes.add(mapping.node);
            }
        }
        return resultNodes;
    }

}
