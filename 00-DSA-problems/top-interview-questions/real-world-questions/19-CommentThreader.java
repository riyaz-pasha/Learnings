import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Comment {
    String id;
    String parentId;
    String text;

    public Comment(String id, String parentId, String text) {
        this.id = id;
        this.parentId = parentId;
        this.text = text;
    }
}

class CommentNode {
    Comment comment;
    List<CommentNode> replies = new ArrayList<>();

    public CommentNode(Comment comment) {
        this.comment = comment;
    }
}

class CommentThreader {
    public List<CommentNode> buildThreadedTree(List<Comment> flatComments) {
        if (flatComments == null) return Collections.emptyList();

        Map<String, CommentNode> nodeMap = new HashMap<>();
        List<CommentNode> roots = new ArrayList<>();

        // 1. First Pass: Create all nodes and put them in the map
        for (Comment c : flatComments) {
            nodeMap.put(c.id, new CommentNode(c));
        }

        // 2. Second Pass: Establish relationships
        for (Comment c : flatComments) {
            CommentNode currentNode = nodeMap.get(c.id);
            
            if (c.parentId == null || c.parentId.isEmpty()) {
                roots.add(currentNode);
            } else {
                CommentNode parentNode = nodeMap.get(c.parentId);
                if (parentNode != null) {
                    parentNode.replies.add(currentNode);
                } else {
                    // Handle missing parent: Treat as root or ignore
                    roots.add(currentNode); 
                }
            }
        }

        return roots;
    }
}
