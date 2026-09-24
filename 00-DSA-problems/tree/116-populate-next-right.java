import java.util.LinkedList;
import java.util.Queue;

class PopulateNextRight {
    public Node connect(Node root) {
        if (root == null)
            return null;

        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int size = queue.size();
            Node prev = null;
            for (int i = 0; i < size; i++) {
                Node current = queue.poll();
                if (prev != null) {
                    prev.next = current;
                }
                prev = current;
                if (current.left != null) {
                    queue.offer(current.left);
                }
                if (current.right != null) {
                    queue.offer(current.right);
                }
            }
        }
        return root;
    }
}

class Solution {

    public Node connect(Node root) {
        if (root == null) {
            return null;
        }
        if (root.left != null) {
            root.left.next = root.right;
        }
        if (root.right != null && root.next != null) {
            root.right.next = root.next.left;
        }
        connect(root.left);
        connect(root.right);
        return root;
    }

}

class PopulateNextRight2 {

    public Node connect(Node root) {
        if (root == null)
            return null;

        Node levelStart = root;

        while (levelStart != null) {
            Node current = levelStart;
            Node nextLevelStart = null;
            Node nextLevelPrev = null;

            while (current != null) {
                if (current.left != null) {
                    if (nextLevelPrev != null) {
                        nextLevelPrev.next = current.left;
                    } else {
                        nextLevelStart = current.left;
                    }
                    nextLevelPrev = current.left;
                }

                if (current.right != null) {
                    if (nextLevelPrev != null) {
                        nextLevelPrev.next = current.right;
                    } else {
                        nextLevelStart = current.right;
                    }
                    nextLevelPrev = current.right;
                }
                current = current.next;
            }

            levelStart = nextLevelStart;
        }
        return root;
    }

}

class PopulateNextRight3 {
    public Node connect(Node root) {
        if (root == null)
            return null;

        Node levelStart = root;

        while (levelStart != null) {
            Node current = levelStart;
            Node nextLevelStart = null;
            Node nextLevelPrev = null;

            while (current != null) {
                if (current.left != null) {
                    if (nextLevelStart == null) {
                        nextLevelStart = current.left;
                    }
                    if (nextLevelPrev != null) {
                        nextLevelPrev.next = current.left;
                    }
                    nextLevelPrev = current.left;
                }

                if (current.right != null) {
                    if (nextLevelStart == null) {
                        nextLevelStart = current.right;
                    }
                    if (nextLevelPrev != null) {
                        nextLevelPrev.next = current.right;
                    }
                    nextLevelPrev = current.right;
                }
                current = current.next;
            }

            levelStart = nextLevelStart;
        }
        return root;
    }
}
