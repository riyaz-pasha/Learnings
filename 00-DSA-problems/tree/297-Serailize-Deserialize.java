import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/*
 * Serialization is the process of converting a data structure or object into a
 * sequence of bits so that it can be stored in a file or memory buffer, or
 * transmitted across a network connection link to be reconstructed later in the
 * same or another computer environment.
 * 
 * Design an algorithm to serialize and deserialize a binary tree. There is no
 * restriction on how your serialization/deserialization algorithm should work.
 * You just need to ensure that a binary tree can be serialized to a string and
 * this string can be deserialized to the original tree structure.
 * 
 * Clarification: The input/output format is the same as how LeetCode serializes
 * a binary tree. You do not necessarily need to follow this format, so please
 * be creative and come up with different approaches yourself.
 * 
 * Example 1:
 * Input: root = [1,2,3,null,null,4,5]
 * Output: [1,2,3,null,null,4,5]
 * 
 * Example 2:
 * Input: root = []
 * Output: []
 */

class TreeNode {

    int val;
    TreeNode left;
    TreeNode right;

    TreeNode(int x) {
        val = x;
    }

}

class SerailizeDeserialize {

    // Time Complexity: O(n) for both serialization and deserialization
    // Space Complexity: O(n) for the serialized string and O(h) for recursion stack
    // Uses pre-order traversal (root → left → right)

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

// Solution 2: Level-order Traversal (BFS) Approach
class Codec2 {

    // Time Complexity: O(n) for both operations
    // Space Complexity: O(n) for string and O(h) for recursion

    private static final String DELIMITER = ",";
    private static final String NULL_NODE = "null";

    // Encodes a tree to a single string.
    public String serialize(TreeNode root) {
        if (root == null)
            return "";

        StringBuilder sb = new StringBuilder();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node == null) {
                sb.append(NULL_NODE).append(DELIMITER);
            } else {
                sb.append(node.val).append(DELIMITER);
                queue.offer(node.left);
                queue.offer(node.right);
            }
        }

        return sb.toString();
    }

    // Decodes your encoded data to tree.
    public TreeNode deserialize(String data) {
        if (data.isEmpty())
            return null;

        String[] nodes = data.split(DELIMITER);
        TreeNode root = new TreeNode(Integer.parseInt(nodes[0]));
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        int i = 1;
        while (!queue.isEmpty() && i < nodes.length) {
            TreeNode node = queue.poll();

            // Process left child
            if (!NULL_NODE.equals(nodes[i])) {
                node.left = new TreeNode(Integer.parseInt(nodes[i]));
                queue.offer(node.left);
            }
            i++;

            // Process right child
            if (i < nodes.length && !NULL_NODE.equals(nodes[i])) {
                node.right = new TreeNode(Integer.parseInt(nodes[i]));
                queue.offer(node.right);
            }
            i++;
        }

        return root;
    }

}

// LeetCode Style Serialization using List representation
class Codec {

    // Encodes a tree to a list (similar to LeetCode format)
    public List<Integer> serializeToList(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();

            if (node == null) {
                result.add(null);
            } else {
                result.add(node.val);
                queue.offer(node.left);
                queue.offer(node.right);
            }
        }

        // Remove trailing nulls to match LeetCode format
        while (!result.isEmpty() && result.get(result.size() - 1) == null) {
            result.remove(result.size() - 1);
        }

        return result;
    }

    // Decodes list back to tree
    public TreeNode deserializeFromList(List<Integer> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        TreeNode root = new TreeNode(data.get(0));
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        int i = 1;
        while (!queue.isEmpty() && i < data.size()) {
            TreeNode node = queue.poll();

            // Left child
            if (i < data.size()) {
                if (data.get(i) != null) {
                    node.left = new TreeNode(data.get(i));
                    queue.offer(node.left);
                }
                i++;
            }

            // Right child
            if (i < data.size()) {
                if (data.get(i) != null) {
                    node.right = new TreeNode(data.get(i));
                    queue.offer(node.right);
                }
                i++;
            }
        }

        return root;
    }

    // Traditional string serialization (required by LeetCode interface)
    public String serialize(TreeNode root) {
        List<Integer> list = serializeToList(root);
        if (list.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }

            if (list.get(i) == null) {
                sb.append("null");
            } else {
                sb.append(list.get(i));
            }
        }

        sb.append("]");
        return sb.toString();
    }

    // Traditional string deserialization (required by LeetCode interface)
    public TreeNode deserialize(String data) {
        if (data == null || data.equals("[]")) {
            return null;
        }

        // Remove brackets and split by comma
        String content = data.substring(1, data.length() - 1);
        if (content.isEmpty()) {
            return null;
        }

        String[] parts = content.split(",");
        List<Integer> list = new ArrayList<>();

        for (String part : parts) {
            part = part.trim();
            if (part.equals("null")) {
                list.add(null);
            } else {
                list.add(Integer.parseInt(part));
            }
        }

        return deserializeFromList(list);
    }

}

// Alternative implementation with more explicit LeetCode format handling
class CodecLeetCodeExact {

    // Serialize exactly like LeetCode format
    public String serialize(TreeNode root) {
        if (root == null) {
            return "[]";
        }

        List<String> result = new ArrayList<>();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();

            if (node == null) {
                result.add("null");
            } else {
                result.add(String.valueOf(node.val));
                queue.offer(node.left);
                queue.offer(node.right);
            }
        }

        // Remove trailing nulls
        while (!result.isEmpty() && result.get(result.size() - 1).equals("null")) {
            result.remove(result.size() - 1);
        }

        return "[" + String.join(",", result) + "]";
    }

    // Deserialize from LeetCode format
    public TreeNode deserialize(String data) {
        if (data.equals("[]")) {
            return null;
        }

        // Parse the string to extract values
        String[] vals = data.substring(1, data.length() - 1).split(",");
        TreeNode root = new TreeNode(Integer.parseInt(vals[0]));
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        int i = 1;
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();

            // Left child
            if (i < vals.length && !vals[i].equals("null")) {
                node.left = new TreeNode(Integer.parseInt(vals[i]));
                queue.offer(node.left);
            }
            i++;

            // Right child
            if (i < vals.length && !vals[i].equals("null")) {
                node.right = new TreeNode(Integer.parseInt(vals[i]));
                queue.offer(node.right);
            }
            i++;
        }

        return root;
    }
}

// Utility class for working with List representations
class TreeListUtils {

    // Convert array to List (for easier testing)
    public static List<Integer> arrayToList(Integer[] arr) {
        return new ArrayList<>(Arrays.asList(arr));
    }

    // Convert List to array representation
    public static Integer[] listToArray(List<Integer> list) {
        return list.toArray(new Integer[0]);
    }

    // Create tree from array (LeetCode style input)
    public static TreeNode createTreeFromArray(Integer[] arr) {
        if (arr == null || arr.length == 0) {
            return null;
        }

        List<Integer> list = Arrays.asList(arr);
        Codec codec = new Codec();
        return codec.deserializeFromList(list);
    }

    // Convert tree to array (LeetCode style output)
    public static Integer[] treeToArray(TreeNode root) {
        Codec codec = new Codec();
        List<Integer> list = codec.serializeToList(root);
        return list.toArray(new Integer[0]);
    }

    // Print array in LeetCode format
    public static void printArray(Integer[] arr) {
        if (arr.length == 0) {
            System.out.println("[]");
            return;
        }

        System.out.print("[");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0)
                System.out.print(",");
            if (arr[i] == null) {
                System.out.print("null");
            } else {
                System.out.print(arr[i]);
            }
        }
        System.out.println("]");
    }

}

// Test class
class LeetCodeListSerialization {

    public static void main(String[] args) {
        // Test Case 1: [1,2,3,null,null,4,5]
        System.out.println("=== Test Case 1: [1,2,3,null,null,4,5] ===");

        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.right.left = new TreeNode(4);
        root1.right.right = new TreeNode(5);

        Codec codec = new Codec();

        // Test List serialization
        List<Integer> list1 = codec.serializeToList(root1);
        System.out.println("Serialized to List: " + list1);

        TreeNode deserialized1 = codec.deserializeFromList(list1);
        List<Integer> list1_back = codec.serializeToList(deserialized1);
        System.out.println("Deserialized back to List: " + list1_back);
        System.out.println("Round trip successful: " + list1.equals(list1_back));

        // Test String serialization (LeetCode format)
        String str1 = codec.serialize(root1);
        System.out.println("Serialized to String: " + str1);

        TreeNode deserialized1_str = codec.deserialize(str1);
        String str1_back = codec.serialize(deserialized1_str);
        System.out.println("Deserialized back to String: " + str1_back);
        System.out.println("String round trip successful: " + str1.equals(str1_back));

        System.out.println();

        // Test Case 2: Empty tree []
        System.out.println("=== Test Case 2: Empty tree [] ===");

        List<Integer> emptyList = codec.serializeToList(null);
        System.out.println("Empty tree serialized to List: " + emptyList);

        String emptyStr = codec.serialize(null);
        System.out.println("Empty tree serialized to String: " + emptyStr);

        TreeNode emptyDeserialized = codec.deserialize(emptyStr);
        System.out.println("Empty tree deserialized: " + (emptyDeserialized == null ? "null" : "not null"));

        System.out.println();

        // Test Case 3: Using utility methods
        System.out.println("=== Test Case 3: Using Utility Methods ===");

        Integer[] inputArray = { 1, 2, 3, null, null, 4, 5 };
        System.out.print("Input array: ");
        TreeListUtils.printArray(inputArray);

        TreeNode treeFromArray = TreeListUtils.createTreeFromArray(inputArray);
        Integer[] outputArray = TreeListUtils.treeToArray(treeFromArray);
        System.out.print("Output array: ");
        TreeListUtils.printArray(outputArray);

        System.out.println("Arrays equal: " + Arrays.equals(inputArray, outputArray));

        // Test LeetCode exact format
        System.out.println();
        System.out.println("=== LeetCode Exact Format Test ===");
        CodecLeetCodeExact exactCodec = new CodecLeetCodeExact();
        String exactSerialized = exactCodec.serialize(root1);
        System.out.println("LeetCode exact format: " + exactSerialized);

        TreeNode exactDeserialized = exactCodec.deserialize(exactSerialized);
        String exactBack = exactCodec.serialize(exactDeserialized);
        System.out.println("Round trip result: " + exactBack);
        System.out.println("Exact format round trip successful: " + exactSerialized.equals(exactBack));
    }

}
