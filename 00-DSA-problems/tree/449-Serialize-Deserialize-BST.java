import java.util.LinkedList;
import java.util.Queue;

class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int x) {
        val = x;
    }

}

class BSTSerailizeDeserialize {

    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializePreorder(root, sb);
        return sb.toString().trim();
    }

    private void serializePreorder(TreeNode node, StringBuilder sb) {
        if (node == null) {
            return;
        }

        sb.append(node.val).append(" ");
        serializePreorder(node.left, sb);
        serializePreorder(node.right, sb);
    }

    public TreeNode deserialize(String data) {
        if (data == null || data.isEmpty())
            return null;
        String[] values = data.split(" ");
        Queue<Integer> queue = new LinkedList<>();
        for (String val : values) {
            queue.offer(Integer.valueOf(val));
        }
        return deserializePreorder(queue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private TreeNode deserializePreorder(Queue<Integer> queue, int min, int max) {
        if (queue.isEmpty()) {
            return null;
        }

        int val = queue.peek();

        if (val < min || val > max) {
            return null;
        }

        val = queue.poll();
        TreeNode node = new TreeNode(val);
        node.left = deserializePreorder(queue, min, val);
        node.right = deserializePreorder(queue, val, max);

        // node.left=deserializePreorder(queue, min, val-1);
        // node.right=deserializePreorder(queue,val+1,max);

        return node;
    }

}

class BSTSerailizeDeserializePostOrder {

    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializePostorder(root, sb);
        return sb.toString().trim();
    }

    private void serializePostorder(TreeNode node, StringBuilder sb) {
        if (node == null)
            return;
        serializePostorder(node.left, sb);
        serializePostorder(node.right, sb);
        sb.append(node.val).append(" ");
    }

    public TreeNode deserialize(String data) {
        if (data == null || data.isEmpty())
            return null;
        String[] values = data.split(" ");
        Queue<Integer> queue = new LinkedList<>();
        for (int i = values.length - 1; i >= 0; i--) {
            queue.offer(Integer.valueOf(values[i]));
        }
        return deserializePostorder(queue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private TreeNode deserializePostorder(Queue<Integer> queue, int min, int max) {
        if (queue.isEmpty()) {
            return null;
        }
        int val = queue.peek();
        if (val < min || val > max) {
            return null;
        }
        val = queue.poll();
        TreeNode node = new TreeNode(val);
        node.right = deserializePostorder(queue, val, max);
        node.left = deserializePostorder(queue, min, val);

        return node;
    }

}
