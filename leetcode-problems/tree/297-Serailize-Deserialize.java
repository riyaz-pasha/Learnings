import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.tree.TreeNode;

class SerailizeDeserialize {
    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializePreorder(root, sb);
        return sb.toString().trim();
    }

    private void serializePreorder(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append("null").append(" ");
            return;
        }
        sb.append(node.val).append(" ");
        serializePreorder(node.left, sb);
        serializePreorder(node.right, sb);
    }

    public TreeNode deserialize(String data) {
        if (data == null || data.isEmpty())
            return null;

        String[] nodes = data.split(" ");
        Queue<String> queue = new LinkedList<>(Arrays.asList(nodes));
        return deserializePreorder(queue);
    }

    private TreeNode deserializePreorder(Queue<String> queue) {
        String val = queue.poll();
        if (val.equals("null")) {
            return null;
        }

        TreeNode node = new TreeNode(Integer.parseInt(val));
        node.left = deserializePreorder(queue);
        node.right = deserializePreorder(queue);

        return node;
    }
}

class SerailizeDeserializeUsingPostOrder {
    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializePostorder(root, sb);
        return sb.toString().trim();
    }

    private void serializePostorder(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append("null").append(" ");
            return;
        }

        // left, right, root
        serializePostorder(node.left, sb);
        serializePostorder(node.right, sb);
        sb.append(node.val).append(" ");
    }

    public TreeNode deserialize(String data) {
        if (data == null || data.isEmpty())
            return null;
        String[] nodes = data.split(" ");
        List<String> nodeList = new ArrayList<>(Arrays.asList(nodes));
        // process in reverse
        Collections.reverse(nodeList);
        Queue<String> queue = new LinkedList<>(nodeList);
        return deserializePostorder(queue);
    }

    private TreeNode deserializePostorder(Queue<String> queue) {
        String val = queue.poll();
        if (val.equals("null")) {
            return null;
        }

        // process in reverse root, right, left
        TreeNode node = new TreeNode(Integer.parseInt(val));
        node.right = deserializePostorder(queue);
        node.left = deserializePostorder(queue);

        return node;
    }
}
