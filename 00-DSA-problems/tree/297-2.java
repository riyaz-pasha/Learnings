import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

/**
 * PROBLEM ANALYSIS & INTERVIEW APPROACH
 * =====================================
 * 
 * This is a TREE SERIALIZATION problem testing:
 * 1. Understanding of tree traversals
 * 2. Ability to encode/decode data structures
 * 3. Handling null nodes correctly
 * 4. String parsing and generation
 * 5. Multiple creative approaches
 * 
 * KEY INSIGHTS:
 * -------------
 * 1. SERIALIZATION = Tree → String
 *    - Must preserve structure (including nulls)
 *    - Must be reversible
 * 
 * 2. DESERIALIZATION = String → Tree
 *    - Must reconstruct exact same tree
 *    - Must handle edge cases (null tree, single node)
 * 
 * 3. MULTIPLE VALID APPROACHES:
 *    - Preorder: Natural, easy to deserialize recursively
 *    - Level-order: Similar to LeetCode format
 *    - Inorder + Preorder: Two traversals needed (complex)
 *    - Postorder: Less common but works
 * 
 * 4. KEY DECISION: How to represent null?
 *    - Use special marker ("null", "X", "#")
 *    - Use delimiter between values (",", " ")
 * 
 * VISUALIZATION:
 * --------------
 * 
 * Tree:       1
 *            / \
 *           2   3
 *              / \
 *             4   5
 * 
 * Preorder serialization: "1,2,null,null,3,4,null,null,5,null,null"
 * - Visit root: 1
 * - Visit left: 2, null, null
 * - Visit right: 3, 4, null, null, 5, null, null
 * 
 * Level-order serialization: "1,2,3,null,null,4,5,null,null,null,null"
 * - Level 0: 1
 * - Level 1: 2, 3
 * - Level 2: null, null, 4, 5
 * - Level 3: null, null, null, null
 * 
 * INTERVIEW STRATEGY:
 * -------------------
 * 1. Clarify requirements
 *    "Can I choose any format? Should it be space-efficient?"
 * 
 * 2. Propose approach
 *    "I'll use preorder traversal because it's natural for
 *     recursive deserialization - we process root first."
 * 
 * 3. Explain null handling
 *    "I'll use 'null' as marker for null nodes and comma
 *     as delimiter."
 * 
 * 4. Walk through example
 *    [Draw tree, show serialization, show deserialization]
 * 
 * 5. Discuss alternatives
 *    - Level-order, different delimiters, compression
 */

// Definition for a binary tree node
class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    
    TreeNode(int x) { val = x; }
}

/**
 * APPROACH 1: PREORDER TRAVERSAL - MOST ELEGANT
 * ==============================================
 * 
 * WHY PREORDER?
 * -------------
 * - Root comes first → natural for reconstruction
 * - Recursive structure matches tree structure
 * - Easy to deserialize: read root, recurse left, recurse right
 * 
 * FORMAT: "root,left_subtree,right_subtree"
 * 
 * Example:
 *     1
 *    / \
 *   2   3
 *      / \
 *     4   5
 * 
 * Serialization: "1,2,null,null,3,4,null,null,5,null,null"
 * 
 * Reading this:
 * - 1: root
 * - 2,null,null: left subtree (leaf node 2)
 * - 3,4,null,null,5,null,null: right subtree
 * 
 * ADVANTAGES:
 * - Natural recursive structure
 * - Easy to implement
 * - Efficient deserialization (single pass)
 * 
 * TIME COMPLEXITY:
 * - Serialize: O(N) - visit each node once
 * - Deserialize: O(N) - process each token once
 * 
 * SPACE COMPLEXITY:
 * - Serialize: O(N) for result string + O(H) recursion
 * - Deserialize: O(N) for string + O(H) recursion
 */
class Codec {
    
    // Marker for null nodes
    private static final String NULL_MARKER = "null";
    private static final String DELIMITER = ",";
    
    /**
     * SERIALIZE: Tree → String using PREORDER
     * 
     * Algorithm:
     * 1. If node is null, add "null"
     * 2. Otherwise, add node value
     * 3. Recursively serialize left subtree
     * 4. Recursively serialize right subtree
     * 
     * Preorder: ROOT → LEFT → RIGHT
     */
    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }
    
    private void serializeHelper(TreeNode node, StringBuilder sb) {
        // Base case: null node
        if (node == null) {
            sb.append(NULL_MARKER).append(DELIMITER);
            return;
        }
        
        // Preorder: ROOT → LEFT → RIGHT
        sb.append(node.val).append(DELIMITER);      // Process root
        serializeHelper(node.left, sb);             // Serialize left
        serializeHelper(node.right, sb);            // Serialize right
    }
    
    /**
     * DESERIALIZE: String → Tree using PREORDER
     * 
     * Algorithm:
     * 1. Split string into tokens
     * 2. Use queue/index to track current position
     * 3. Recursively build tree:
     *    - Read current token
     *    - If "null", return null
     *    - Otherwise, create node with value
     *    - Recursively build left subtree
     *    - Recursively build right subtree
     * 
     * The beauty: Tokens are in preorder, so we read them in order!
     */
    public TreeNode deserialize(String data) {
        Queue<String> tokens = new LinkedList<>(Arrays.asList(data.split(DELIMITER)));
        return deserializeHelper(tokens);
    }
    
    private TreeNode deserializeHelper(Queue<String> tokens) {
        // Read next token
        String token = tokens.poll();
        
        // Base case: null node
        if (NULL_MARKER.equals(token)) {
            return null;
        }
        
        // Create node with current value
        TreeNode node = new TreeNode(Integer.parseInt(token));
        
        // Recursively build subtrees (preorder: left then right)
        node.left = deserializeHelper(tokens);
        node.right = deserializeHelper(tokens);
        
        return node;
    }
}

/**
 * APPROACH 2: LEVEL-ORDER TRAVERSAL (BFS)
 * ========================================
 * 
 * WHY LEVEL-ORDER?
 * ----------------
 * - Similar to LeetCode's format
 * - More intuitive for some people
 * - Can visualize tree level by level
 * 
 * FORMAT: "root,level1_left,level1_right,level2_children,..."
 * 
 * Example:
 *     1
 *    / \
 *   2   3
 *      / \
 *     4   5
 * 
 * Serialization: "1,2,3,null,null,4,5,null,null,null,null"
 * 
 * ADVANTAGES:
 * - Matches common tree representation
 * - Easy to visualize
 * 
 * DISADVANTAGES:
 * - More complex deserialization
 * - Need to track parent-child relationships
 * 
 * TIME: O(N), SPACE: O(W) where W = max width
 */
class CodecLevelOrder {
    
    private static final String NULL_MARKER = "null";
    private static final String DELIMITER = ",";
    
    /**
     * SERIALIZE using LEVEL-ORDER (BFS)
     */
    public String serialize(TreeNode root) {
        if (root == null) return "";
        
        StringBuilder sb = new StringBuilder();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            
            if (node == null) {
                sb.append(NULL_MARKER).append(DELIMITER);
                continue;
            }
            
            sb.append(node.val).append(DELIMITER);
            queue.offer(node.left);   // Add even if null
            queue.offer(node.right);  // Add even if null
        }
        
        return sb.toString();
    }
    
    /**
     * DESERIALIZE using LEVEL-ORDER (BFS)
     * 
     * Key insight: Process nodes level by level
     * Each non-null node creates two children (possibly null)
     */
    public TreeNode deserialize(String data) {
        if (data.isEmpty()) return null;
        
        String[] tokens = data.split(DELIMITER);
        TreeNode root = new TreeNode(Integer.parseInt(tokens[0]));
        
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        int index = 1; // Start from second token
        
        while (!queue.isEmpty() && index < tokens.length) {
            TreeNode node = queue.poll();
            
            // Process left child
            if (!NULL_MARKER.equals(tokens[index])) {
                node.left = new TreeNode(Integer.parseInt(tokens[index]));
                queue.offer(node.left);
            }
            index++;
            
            // Process right child
            if (index < tokens.length && !NULL_MARKER.equals(tokens[index])) {
                node.right = new TreeNode(Integer.parseInt(tokens[index]));
                queue.offer(node.right);
            }
            index++;
        }
        
        return root;
    }
}

/**
 * APPROACH 3: COMPACT ENCODING
 * =============================
 * 
 * OPTIMIZATION: Use special encoding for space efficiency
 * 
 * Ideas:
 * - Bracket notation: "1(2()(3(4()())(5()())))"
 * - Length-prefixed: "5,1,2,null,null,3,4,null,null,5,null,null"
 * - Binary format: More compact but less readable
 * 
 * Example with parentheses:
 *     1
 *    / \
 *   2   3
 *      / \
 *     4   5
 * 
 * Format: "1(2)(3(4)(5))"
 * - () represents null
 * - Nested () shows structure
 * 
 * WHEN TO USE:
 * - Need human-readable format
 * - Want to minimize string length
 * - Willing to handle complex parsing
 */
class CodecCompact {
    
    public String serialize(TreeNode root) {
        if (root == null) return "()";
        
        StringBuilder sb = new StringBuilder();
        sb.append(root.val);
        
        // Only add subtrees if they exist
        if (root.left != null || root.right != null) {
            sb.append("(").append(serialize(root.left)).append(")");
            sb.append("(").append(serialize(root.right)).append(")");
        }
        
        return sb.toString();
    }
    
    public TreeNode deserialize(String data) {
        if (data.equals("()")) return null;
        
        // Find root value (before first '(' or end of string)
        int i = 0;
        while (i < data.length() && data.charAt(i) != '(') {
            i++;
        }
        
        TreeNode root = new TreeNode(Integer.parseInt(data.substring(0, i)));
        
        if (i < data.length()) {
            // Find matching parentheses for left and right subtrees
            int[] indices = findSubtrees(data, i);
            root.left = deserialize(data.substring(indices[0] + 1, indices[1]));
            root.right = deserialize(data.substring(indices[2] + 1, indices[3]));
        }
        
        return root;
    }
    
    // Helper to find positions of left and right subtree strings
    private int[] findSubtrees(String s, int start) {
        int leftStart = start;
        int count = 0;
        int leftEnd = -1;
        
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '(') count++;
            else if (s.charAt(i) == ')') count--;
            
            if (count == 0 && leftEnd == -1) {
                leftEnd = i;
            } else if (count == 0) {
                return new int[]{leftStart, leftEnd, leftEnd + 1, i};
            }
        }
        
        return new int[]{leftStart, s.length() - 1, s.length(), s.length()};
    }
}

/**
 * COMPARISON OF APPROACHES
 * =========================
 * 
 * Approach     | Clarity | Efficiency | Space | Best For
 * -------------|---------|------------|-------|------------------
 * Preorder     | High    | Excellent  | O(N)  | Interviews, default
 * Level-order  | Medium  | Good       | O(W)  | Visual understanding
 * Compact      | Low     | Best       | <O(N) | Space optimization
 * 
 * RECOMMENDATION:
 * - Interview: Use Preorder (Approach 1)
 * - Production: Preorder or Level-order based on needs
 * - Optimization: Compact if space critical
 */

/**
 * COMMON MISTAKES & EDGE CASES
 * =============================
 * 
 * MISTAKES:
 * 1. Forgetting to handle null nodes
 * 2. Not using delimiter (ambiguous parsing: "12" vs "1,2")
 * 3. Off-by-one errors in level-order deserialization
 * 4. Not handling empty tree
 * 5. Integer parsing errors (negative numbers, large numbers)
 * 
 * EDGE CASES:
 * 1. Null tree → ""
 * 2. Single node → "1,null,null"
 * 3. Left-skewed tree
 * 4. Right-skewed tree
 * 5. Complete binary tree
 * 6. Negative values → "-1,-2,null,null,3,null,null"
 * 7. Large values → "1000000,null,null"
 */

// Comprehensive test suite
class Main {
    public static void main(String[] args) {
        Codec codec = new Codec();
        CodecLevelOrder codecLO = new CodecLevelOrder();
        CodecCompact codecCompact = new CodecCompact();
        
        System.out.println("=== Test Case 1: Normal Tree ===");
        //     1
        //    / \
        //   2   3
        //      / \
        //     4   5
        TreeNode root1 = new TreeNode(1);
        root1.left = new TreeNode(2);
        root1.right = new TreeNode(3);
        root1.right.left = new TreeNode(4);
        root1.right.right = new TreeNode(5);
        
        String serialized1 = codec.serialize(root1);
        System.out.println("Preorder: " + serialized1);
        TreeNode deserialized1 = codec.deserialize(serialized1);
        System.out.println("Verify: " + codec.serialize(deserialized1));
        System.out.println("Match: " + serialized1.equals(codec.serialize(deserialized1)));
        
        System.out.println("\nLevel-order: " + codecLO.serialize(root1));
        System.out.println("Compact: " + codecCompact.serialize(root1));
        
        System.out.println("\n=== Test Case 2: Null Tree ===");
        TreeNode root2 = null;
        String serialized2 = codec.serialize(root2);
        System.out.println("Serialized: '" + serialized2 + "'");
        TreeNode deserialized2 = codec.deserialize(serialized2);
        System.out.println("Deserialized is null: " + (deserialized2 == null));
        
        System.out.println("\n=== Test Case 3: Single Node ===");
        TreeNode root3 = new TreeNode(1);
        String serialized3 = codec.serialize(root3);
        System.out.println("Serialized: " + serialized3);
        TreeNode deserialized3 = codec.deserialize(serialized3);
        System.out.println("Value: " + deserialized3.val);
        System.out.println("Left is null: " + (deserialized3.left == null));
        System.out.println("Right is null: " + (deserialized3.right == null));
        
        System.out.println("\n=== Test Case 4: Left-Skewed ===");
        //     1
        //    /
        //   2
        //  /
        // 3
        TreeNode root4 = new TreeNode(1);
        root4.left = new TreeNode(2);
        root4.left.left = new TreeNode(3);
        String serialized4 = codec.serialize(root4);
        System.out.println("Serialized: " + serialized4);
        TreeNode deserialized4 = codec.deserialize(serialized4);
        System.out.println("Verify: " + codec.serialize(deserialized4));
        
        System.out.println("\n=== Test Case 5: Complete Binary Tree ===");
        //       1
        //      / \
        //     2   3
        //    / \ / \
        //   4  5 6  7
        TreeNode root5 = new TreeNode(1);
        root5.left = new TreeNode(2);
        root5.right = new TreeNode(3);
        root5.left.left = new TreeNode(4);
        root5.left.right = new TreeNode(5);
        root5.right.left = new TreeNode(6);
        root5.right.right = new TreeNode(7);
        String serialized5 = codec.serialize(root5);
        System.out.println("Serialized: " + serialized5);
        TreeNode deserialized5 = codec.deserialize(serialized5);
        System.out.println("Verify: " + codec.serialize(deserialized5));
        
        System.out.println("\n=== Test Case 6: Negative Values ===");
        TreeNode root6 = new TreeNode(-1);
        root6.left = new TreeNode(-2);
        root6.right = new TreeNode(3);
        String serialized6 = codec.serialize(root6);
        System.out.println("Serialized: " + serialized6);
        TreeNode deserialized6 = codec.deserialize(serialized6);
        System.out.println("Root value: " + deserialized6.val);
        System.out.println("Left value: " + deserialized6.left.val);
        
        System.out.println("\n=== Visualization ===");
        System.out.println("Tree:     1");
        System.out.println("         / \\");
        System.out.println("        2   3");
        System.out.println("           / \\");
        System.out.println("          4   5");
        System.out.println();
        System.out.println("Preorder serialization:");
        System.out.println("  Visit order: 1 → 2 → null → null → 3 → 4 → null → null → 5 → null → null");
        System.out.println("  String: " + serialized1);
        System.out.println();
        System.out.println("Deserialization process:");
        System.out.println("  1. Read '1' → Create root");
        System.out.println("  2. Read '2' → Create root.left");
        System.out.println("  3. Read 'null' → root.left.left = null");
        System.out.println("  4. Read 'null' → root.left.right = null");
        System.out.println("  5. Read '3' → Create root.right");
        System.out.println("  ... and so on");
        
        // Test all three approaches give correct results
        System.out.println("\n=== Verify All Approaches ===");
        TreeNode t1 = codec.deserialize(codec.serialize(root1));
        TreeNode t2 = codecLO.deserialize(codecLO.serialize(root1));
        TreeNode t3 = codecCompact.deserialize(codecCompact.serialize(root1));
        
        System.out.println("Preorder works: " + treesEqual(root1, t1));
        System.out.println("Level-order works: " + treesEqual(root1, t2));
        System.out.println("Compact works: " + treesEqual(root1, t3));
    }
    
    // Helper to compare two trees
    private static boolean treesEqual(TreeNode t1, TreeNode t2) {
        if (t1 == null && t2 == null) return true;
        if (t1 == null || t2 == null) return false;
        return t1.val == t2.val && 
               treesEqual(t1.left, t2.left) && 
               treesEqual(t1.right, t2.right);
    }
}

/**
 * INTERVIEW TALKING POINTS
 * =========================
 * 
 * When presenting this solution in an interview:
 * 
 * 1. "I'll use preorder traversal for serialization because it's
 *    natural for reconstruction - we process the root first, then
 *    recursively handle left and right subtrees."
 * 
 * 2. "For null nodes, I'll use 'null' as a marker and comma as
 *    delimiter. This ensures unambiguous parsing."
 *    [Write example on board]
 * 
 * 3. "Serialization is straightforward preorder DFS. Deserialization
 *    reads tokens sequentially - each recursive call consumes its
 *    subtree's tokens."
 *    [Walk through example]
 * 
 * 4. "Time complexity is O(N) for both operations - we visit each
 *    node exactly once. Space is O(N) for the string plus O(H)
 *    for recursion stack."
 * 
 * 5. "Alternative approaches include level-order (BFS) which is
 *    closer to LeetCode's format, or compact encodings using
 *    parentheses for better space efficiency."
 * 
 * 6. "Edge cases: empty tree, single node, negative values, and
 *    ensuring delimiter prevents ambiguous parsing like '12' vs '1,2'."
 * 
 * KEY POINTS TO EMPHASIZE:
 * - Why preorder (root first for reconstruction)
 * - Importance of null markers
 * - Delimiter for unambiguous parsing
 * - Symmetric encode/decode process
 * 
 * FOLLOW-UP QUESTIONS TO EXPECT:
 * - Why preorder instead of inorder? [Inorder alone loses structure]
 * - Can you use two traversals? [Yes, inorder+preorder but complex]
 * - How would you optimize for space? [Compact encoding, binary]
 * - What if values can be "null" string? [Use different marker]
 * - Can you do it iteratively? [Yes, with explicit stack]
 * - How to handle very large trees? [Streaming, chunked processing]
 */




// Approach 1: Level Order (BFS) - LeetCode Style
class CodecBFS {
    // Encodes a tree to a single string.
    public String serialize(TreeNode root) {
        if (root == null) return "";
        
        StringBuilder sb = new StringBuilder();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            
            if (node == null) {
                sb.append("null,");
            } else {
                sb.append(node.val).append(",");
                queue.offer(node.left);
                queue.offer(node.right);
            }
        }
        
        return sb.toString();
    }

    // Decodes your encoded data to tree.
    public TreeNode deserialize(String data) {
        if (data.isEmpty()) return null;
        
        String[] vals = data.split(",");
        TreeNode root = new TreeNode(Integer.parseInt(vals[0]));
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        int i = 1;
        while (!queue.isEmpty() && i < vals.length) {
            TreeNode node = queue.poll();
            
            // Process left child
            if (!vals[i].equals("null")) {
                node.left = new TreeNode(Integer.parseInt(vals[i]));
                queue.offer(node.left);
            }
            i++;
            
            // Process right child
            if (i < vals.length && !vals[i].equals("null")) {
                node.right = new TreeNode(Integer.parseInt(vals[i]));
                queue.offer(node.right);
            }
            i++;
        }
        
        return root;
    }
}

// Approach 2: Preorder DFS (Recursive) - OPTIMAL
class CodecDFS {
    private static final String NULL = "null";
    private static final String SEP = ",";
    
    // Encodes a tree to a single string.
    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }
    
    private void serializeHelper(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append(NULL).append(SEP);
            return;
        }
        
        sb.append(node.val).append(SEP);
        serializeHelper(node.left, sb);
        serializeHelper(node.right, sb);
    }

    // Decodes your encoded data to tree.
    public TreeNode deserialize(String data) {
        Queue<String> queue = new LinkedList<>(Arrays.asList(data.split(SEP)));
        return deserializeHelper(queue);
    }
    
    private TreeNode deserializeHelper(Queue<String> queue) {
        String val = queue.poll();
        
        if (val.equals(NULL)) {
            return null;
        }
        
        TreeNode node = new TreeNode(Integer.parseInt(val));
        node.left = deserializeHelper(queue);
        node.right = deserializeHelper(queue);
        return node;
    }
}

// Approach 3: Preorder with Brackets
class CodecBrackets {
    public String serialize(TreeNode root) {
        if (root == null) return "X";
        
        return "(" + root.val + 
               serialize(root.left) + 
               serialize(root.right) + ")";
    }

    public TreeNode deserialize(String data) {
        int[] index = {0};
        return deserializeHelper(data, index);
    }
    
    private TreeNode deserializeHelper(String data, int[] index) {
        if (index[0] >= data.length() || data.charAt(index[0]) == 'X') {
            index[0]++;
            return null;
        }
        
        index[0]++; // Skip '('
        
        // Parse number
        int start = index[0];
        while (index[0] < data.length() && 
               (Character.isDigit(data.charAt(index[0])) || 
                data.charAt(index[0]) == '-')) {
            index[0]++;
        }
        
        int val = Integer.parseInt(data.substring(start, index[0]));
        TreeNode node = new TreeNode(val);
        
        node.left = deserializeHelper(data, index);
        node.right = deserializeHelper(data, index);
        
        index[0]++; // Skip ')'
        return node;
    }
}

// Approach 4: Using Pre-order and In-order
class CodecPreIn {
    public String serialize(TreeNode root) {
        List<Integer> preorder = new ArrayList<>();
        List<Integer> inorder = new ArrayList<>();
        
        preorderTraversal(root, preorder);
        inorderTraversal(root, inorder);
        
        return listToString(preorder) + "|" + listToString(inorder);
    }
    
    private void preorderTraversal(TreeNode node, List<Integer> list) {
        if (node == null) return;
        list.add(node.val);
        preorderTraversal(node.left, list);
        preorderTraversal(node.right, list);
    }
    
    private void inorderTraversal(TreeNode node, List<Integer> list) {
        if (node == null) return;
        inorderTraversal(node.left, list);
        list.add(node.val);
        inorderTraversal(node.right, list);
    }
    
    private String listToString(List<Integer> list) {
        return list.stream()
                   .map(String::valueOf)
                   .reduce((a, b) -> a + "," + b)
                   .orElse("");
    }

    public TreeNode deserialize(String data) {
        if (data.isEmpty()) return null;
        
        String[] parts = data.split("\\|");
        String[] preVals = parts[0].split(",");
        String[] inVals = parts[1].split(",");
        
        int[] preorder = new int[preVals.length];
        int[] inorder = new int[inVals.length];
        
        for (int i = 0; i < preVals.length; i++) {
            preorder[i] = Integer.parseInt(preVals[i]);
            inorder[i] = Integer.parseInt(inVals[i]);
        }
        
        return buildTree(preorder, inorder);
    }
    
    private TreeNode buildTree(int[] preorder, int[] inorder) {
        Map<Integer, Integer> inMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inMap.put(inorder[i], i);
        }
        return buildHelper(preorder, 0, preorder.length - 1,
                          inorder, 0, inorder.length - 1, inMap);
    }
    
    private TreeNode buildHelper(int[] preorder, int preStart, int preEnd,
                                  int[] inorder, int inStart, int inEnd,
                                  Map<Integer, Integer> inMap) {
        if (preStart > preEnd || inStart > inEnd) return null;
        
        TreeNode root = new TreeNode(preorder[preStart]);
        int inRoot = inMap.get(root.val);
        int leftSize = inRoot - inStart;
        
        root.left = buildHelper(preorder, preStart + 1, preStart + leftSize,
                                inorder, inStart, inRoot - 1, inMap);
        root.right = buildHelper(preorder, preStart + leftSize + 1, preEnd,
                                 inorder, inRoot + 1, inEnd, inMap);
        
        return root;
    }
}

// Approach 5: Postorder DFS
class CodecPostorder {
    private static final String NULL = "#";
    private static final String SEP = ",";
    
    public String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        postorder(root, sb);
        return sb.toString();
    }
    
    private void postorder(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append(NULL).append(SEP);
            return;
        }
        
        postorder(node.left, sb);
        postorder(node.right, sb);
        sb.append(node.val).append(SEP);
    }

    public TreeNode deserialize(String data) {
        String[] vals = data.split(SEP);
        Stack<String> stack = new Stack<>();
        for (String val : vals) {
            stack.push(val);
        }
        return buildTree(stack);
    }
    
    private TreeNode buildTree(Stack<String> stack) {
        String val = stack.pop();
        
        if (val.equals(NULL)) {
            return null;
        }
        
        TreeNode node = new TreeNode(Integer.parseInt(val));
        node.right = buildTree(stack);  // Right first for postorder
        node.left = buildTree(stack);
        return node;
    }
}

// Test and Visualization
class SerializationDemo {
    
    public static void main(String[] args) {
        // Build test tree:    1
        //                   /   \
        //                  2     3
        //                       / \
        //                      4   5
        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(3);
        root.right.left = new TreeNode(4);
        root.right.right = new TreeNode(5);
        
        System.out.println("Original Tree:");
        visualizeTree(root);
        System.out.println("\nInorder: " + inorderString(root));
        
        // Test all approaches
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APPROACH 1: Level Order (BFS)");
        System.out.println("=".repeat(60));
        testCodec(new CodecBFS(), root);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APPROACH 2: Preorder DFS (Optimal)");
        System.out.println("=".repeat(60));
        testCodec(new Codec(), root);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APPROACH 3: Preorder with Brackets");
        System.out.println("=".repeat(60));
        testCodec(new CodecBrackets(), root);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APPROACH 4: Pre-order + In-order");
        System.out.println("=".repeat(60));
        testCodec(new CodecPreIn(), root);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("APPROACH 5: Postorder DFS");
        System.out.println("=".repeat(60));
        testCodec(new CodecPostorder(), root);
        
        // Edge cases
        System.out.println("\n" + "=".repeat(60));
        System.out.println("EDGE CASES");
        System.out.println("=".repeat(60));
        testEdgeCases();
        
        // Comparison
        System.out.println("\n" + "=".repeat(60));
        System.out.println("COMPARISON");
        System.out.println("=".repeat(60));
        compareApproaches();
        
        // Detailed explanation
        System.out.println("\n" + "=".repeat(60));
        System.out.println("DETAILED EXPLANATION");
        System.out.println("=".repeat(60));
        explainApproaches();
    }
    
    private static void testCodec(Object codec, TreeNode root) {
        String serialized = null;
        TreeNode deserialized = null;
        
        if (codec instanceof CodecBFS c) {
            serialized = c.serialize(root);
            deserialized = c.deserialize(serialized);
        } else if (codec instanceof CodecDFS c) {
            serialized = c.serialize(root);
            deserialized = c.deserialize(serialized);
        } else if (codec instanceof CodecBrackets c) {
            serialized = c.serialize(root);
            deserialized = c.deserialize(serialized);
        } else if (codec instanceof CodecPreIn c) {
            serialized = c.serialize(root);
            deserialized = c.deserialize(serialized);
        } else if (codec instanceof CodecPostorder c) {
            serialized = c.serialize(root);
            deserialized = c.deserialize(serialized);
        }
        
        System.out.println("Serialized: " + serialized);
        System.out.println("Length: " + serialized.length() + " characters");
        
        String originalInorder = inorderString(root);
        String deserializedInorder = inorderString(deserialized);
        
        System.out.println("Original inorder:     " + originalInorder);
        System.out.println("Deserialized inorder: " + deserializedInorder);
        System.out.println("Match: " + originalInorder.equals(deserializedInorder) + " ✓");
    }
    
    private static void testEdgeCases() {
        Codec codec = new Codec();
        
        // Null tree
        System.out.println("\n1. Null Tree:");
        TreeNode nullTree = null;
        String s1 = codec.serialize(nullTree);
        TreeNode d1 = codec.deserialize(s1);
        System.out.println("   Serialized: \"" + s1 + "\"");
        System.out.println("   Deserialized: " + (d1 == null ? "null" : "not null"));
        System.out.println("   Match: " + (d1 == null) + " ✓");
        
        // Single node
        System.out.println("\n2. Single Node:");
        TreeNode single = new TreeNode(1);
        String s2 = codec.serialize(single);
        TreeNode d2 = codec.deserialize(s2);
        System.out.println("   Serialized: \"" + s2 + "\"");
        System.out.println("   Match: " + (d2.val == 1) + " ✓");
        
        // Left skewed
        System.out.println("\n3. Left Skewed:");
        TreeNode leftSkewed = new TreeNode(1);
        leftSkewed.left = new TreeNode(2);
        leftSkewed.left.left = new TreeNode(3);
        String s3 = codec.serialize(leftSkewed);
        TreeNode d3 = codec.deserialize(s3);
        System.out.println("   Serialized: \"" + s3 + "\"");
        System.out.println("   Match: " + inorderString(leftSkewed).equals(inorderString(d3)) + " ✓");
        
        // Right skewed
        System.out.println("\n4. Right Skewed:");
        TreeNode rightSkewed = new TreeNode(1);
        rightSkewed.right = new TreeNode(2);
        rightSkewed.right.right = new TreeNode(3);
        String s4 = codec.serialize(rightSkewed);
        TreeNode d4 = codec.deserialize(s4);
        System.out.println("   Serialized: \"" + s4 + "\"");
        System.out.println("   Match: " + inorderString(rightSkewed).equals(inorderString(d4)) + " ✓");
        
        // Negative values
        System.out.println("\n5. Negative Values:");
        TreeNode negative = new TreeNode(-1);
        negative.left = new TreeNode(-2);
        negative.right = new TreeNode(-3);
        String s5 = codec.serialize(negative);
        TreeNode d5 = codec.deserialize(s5);
        System.out.println("   Serialized: \"" + s5 + "\"");
        System.out.println("   Match: " + inorderString(negative).equals(inorderString(d5)) + " ✓");
    }
    
    private static void compareApproaches() {
        System.out.println("\n┌──────────────────────┬──────────────┬──────────────┬─────────────────┐");
        System.out.println("│ Approach             │ Serialize    │ Deserialize  │ Notes           │");
        System.out.println("├──────────────────────┼──────────────┼──────────────┼─────────────────┤");
        System.out.println("│ BFS (Level Order)    │ O(n)         │ O(n)         │ Intuitive       │");
        System.out.println("│ Preorder DFS         │ O(n)         │ O(n)         │ Optimal/Clean   │");
        System.out.println("│ Brackets             │ O(n)         │ O(n)         │ Self-delimiting │");
        System.out.println("│ Pre+In order         │ O(n)         │ O(n log n)   │ Unique values!  │");
        System.out.println("│ Postorder DFS        │ O(n)         │ O(n)         │ Alternative     │");
        System.out.println("└──────────────────────┴──────────────┴──────────────┴─────────────────┘");
        
        System.out.println("\nSpace Complexity: All O(n)");
        System.out.println("\nBest Choice: Preorder DFS (clean, simple, efficient)");
    }
    
    private static void explainApproaches() {
        System.out.println("\n1. BFS (LEVEL ORDER)");
        System.out.println("   - Serialize: Level-by-level like [1,2,3,null,null,4,5]");
        System.out.println("   - Deserialize: Build level-by-level using queue");
        System.out.println("   - Pros: Matches LeetCode format, intuitive");
        System.out.println("   - Cons: Many trailing nulls");
        
        System.out.println("\n2. PREORDER DFS (OPTIMAL)");
        System.out.println("   - Serialize: Root, Left, Right with nulls");
        System.out.println("   - Deserialize: Reconstruct in same order");
        System.out.println("   - Pros: Clean, minimal, no index tracking");
        System.out.println("   - Cons: Must mark null nodes explicitly");
        System.out.println("   - Example: \"1,2,null,null,3,4,null,null,5,null,null,\"");
        
        System.out.println("\n3. BRACKETS");
        System.out.println("   - Serialize: Use parentheses for structure");
        System.out.println("   - Deserialize: Parse like expression tree");
        System.out.println("   - Pros: Self-delimiting, handles any integers");
        System.out.println("   - Cons: More characters needed");
        System.out.println("   - Example: \"(1(2XX)(3(4XX)(5XX)))\"");
        
        System.out.println("\n4. PREORDER + INORDER");
        System.out.println("   - Serialize: Two traversals");
        System.out.println("   - Deserialize: Reconstruct from two arrays");
        System.out.println("   - Pros: No need for null markers");
        System.out.println("   - Cons: Requires unique values, more complex");
        
        System.out.println("\n5. POSTORDER");
        System.out.println("   - Serialize: Left, Right, Root");
        System.out.println("   - Deserialize: Build from end using stack");
        System.out.println("   - Pros: Alternative to preorder");
        System.out.println("   - Cons: Slightly less intuitive");
    }
    
    private static void visualizeTree(TreeNode root) {
        if (root == null) {
            System.out.println("null");
            return;
        }
        
        List<List<String>> levels = new ArrayList<>();
        buildLevels(root, 0, levels);
        
        for (int i = 0; i < levels.size(); i++) {
            System.out.print("Level " + i + ": ");
            System.out.println(String.join(" ", levels.get(i)));
        }
    }
    
    private static void buildLevels(TreeNode node, int level, List<List<String>> levels) {
        if (node == null) return;
        
        if (levels.size() == level) {
            levels.add(new ArrayList<>());
        }
        
        levels.get(level).add(String.valueOf(node.val));
        buildLevels(node.left, level + 1, levels);
        buildLevels(node.right, level + 1, levels);
    }
    
    private static String inorderString(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        inorder(root, result);
        return result.toString();
    }
    
    private static void inorder(TreeNode node, List<Integer> result) {
        if (node == null) return;
        inorder(node.left, result);
        result.add(node.val);
        inorder(node.right, result);
    }
}

/*
DETAILED EXPLANATION:

PROBLEM: Serialize and Deserialize Binary Tree

Convert tree ↔ string such that we can reconstruct exact structure.

KEY CHALLENGE: Capture STRUCTURE, not just values!

APPROACH 1: BFS (LEVEL ORDER) - LeetCode Style
==============================================

Serialize: [1,2,3,null,null,4,5]

Tree:    1
       /   \
      2     3
           / \
          4   5

Level by level:
Level 0: [1]
Level 1: [2, 3]
Level 2: [null, null, 4, 5]

String: "1,2,3,null,null,4,5,"

Deserialize:
- Use queue to track parents
- Read values pair-by-pair (left, right)
- Build children and add to queue

Time: O(n), Space: O(n)

APPROACH 2: PREORDER DFS - OPTIMAL
===================================

Serialize: Root → Left → Right

Tree:    1
       /   \
      2     3
           / \
          4   5

Preorder: 1, 2, null, null, 3, 4, null, null, 5, null, null

String: "1,2,null,null,3,4,null,null,5,null,null,"

WHY THIS WORKS:
- Preorder uniquely defines tree structure with null markers
- Can reconstruct by consuming values in same order

Deserialize:
```java
TreeNode deserialize(Queue<String> queue) {
    String val = queue.poll();
    if (val.equals("null")) return null;
    
    TreeNode node = new TreeNode(val);
    node.left = deserialize(queue);   // Recursively build left
    node.right = deserialize(queue);  // Then right
    return node;
}
```

Key: Process in SAME ORDER as serialization!

Time: O(n), Space: O(h) for recursion

APPROACH 3: BRACKETS
====================

Self-delimiting format using parentheses:

Tree:    1
       /   \
      2     3

Serialize: "(1(2XX)(3XX))"

Each node: (value left right)
X = null marker

Advantage: Handles multi-digit numbers naturally
Disadvantage: More verbose

APPROACH 4: PREORDER + INORDER
===============================

Use two traversals to uniquely identify tree:

Tree:    1
       /   \
      2     3

Preorder: [1, 2, 3]
Inorder:  [2, 1, 3]

From these two, we can reconstruct:
- Preorder[0] is root (1)
- Find root in inorder: left=[2], right=[3]
- Recursively build left and right

IMPORTANT: Only works if all values are UNIQUE!

APPROACH 5: POSTORDER
=====================

Left → Right → Root

Tree:    1
       /   \
      2     3

Postorder: null, null, 2, null, null, 3, 1

Deserialize: Build from END, using stack

COMPARISON:

BFS:
+ Matches LeetCode format
+ Intuitive level-by-level
- Many trailing nulls

Preorder DFS:
+ Clean and simple
+ Minimal markers
+ Easy recursion
- Null markers needed

Brackets:
+ Self-delimiting
+ No ambiguity
- More verbose

Pre+In:
+ No null markers
- Requires unique values
- More complex logic

Postorder:
+ Alternative to preorder
- Less intuitive
- Needs stack/reverse

BEST CHOICE: PREORDER DFS
=========================

Why?
1. Clean recursive code
2. Minimal overhead
3. Natural tree traversal
4. Easy to understand
5. O(n) time, O(h) space

CODE:
```java
String serialize(TreeNode root) {
    if (root == null) return "null,";
    return root.val + "," + 
           serialize(root.left) + 
           serialize(root.right);
}

TreeNode deserialize(Queue<String> queue) {
    String val = queue.poll();
    if (val.equals("null")) return null;
    
    TreeNode node = new TreeNode(Integer.parseInt(val));
    node.left = deserialize(queue);
    node.right = deserialize(queue);
    return node;
}
```

KEY INSIGHTS:

1. STRUCTURE MATTERS
   - Values alone don't define tree
   - Need to encode null children

2. TRAVERSAL ORDER
   - Any traversal works WITH null markers
   - Pre/Post/In-order all valid

3. RECURSION POWER
   - Same order for serialize and deserialize
   - Natural fit for tree problems

4. UNIQUE IDENTIFICATION
   - One traversal + nulls = unique
   - Two traversals (pre+in or post+in) = unique IF values unique

EDGE CASES:

1. Null tree: "null,"
2. Single node: "1,null,null,"
3. Skewed tree: Works perfectly
4. Negative values: Handle carefully in parsing
5. Large values: String length considerations

INTERVIEW STRATEGY:

1. Start with BFS (most intuitive)
2. Optimize to preorder DFS
3. Explain why it works (same order)
4. Code cleanly with recursion
5. Test with examples
6. Discuss complexity: O(n) time, O(h) space

COMMON MISTAKES:

1. Forgetting null markers
2. Wrong traversal order in deserialize
3. Index management in BFS
4. Not handling empty string
5. Integer parsing errors

This problem tests:
- Tree traversal understanding
- Recursion mastery
- String manipulation
- Design thinking
*/
