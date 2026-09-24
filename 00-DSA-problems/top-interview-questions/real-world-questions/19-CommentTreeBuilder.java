import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Comment {

    int id;
    Integer parentId;
    String content;
    List<Comment> children = new ArrayList<>();

    public Comment(int id, Integer parentId, String content) {
        this.id = id;
        this.parentId = parentId;
        this.content = content;
    }

}

class CommentTreeBuilder {

    /*
     * Time Complexity: O(N), where N is the number of comments. We iterate through
     * the list of comments twice. Hash map operations (insertion and lookup) are,
     * on average, O(1).
     * 
     * Space Complexity: O(N). We are storing all N comments in the commentMap, and
     * the children lists within each comment also consume space proportional to N.
     */
    public List<Comment> buildCommentTree(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Integer, Comment> commentMap = new HashMap<>();

        // Pass 1: Create a lookup map for quick access
        for (Comment comment : comments) {
            commentMap.put(comment.id, comment);
        }

        // Pass 2: Build the tree
        List<Comment> rootComments = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment.parentId == null) {
                rootComments.add(comment);
            } else {
                Comment parentComment = commentMap.get(comment.parentId);
                if (parentComment != null) {
                    parentComment.children.add(comment);
                }
            }
        }

        return rootComments;
    }

}
