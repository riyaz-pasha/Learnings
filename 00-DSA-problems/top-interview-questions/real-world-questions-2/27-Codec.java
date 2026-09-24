import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * N-ary Tree Codec (Serialize / Deserialize)
 *
 * We serialize the tree using PREORDER traversal.
 *
 * Format:
 *   For every node we store:
 *      nodeValue childCount
 *
 * Example Tree:
 *         1
 *      /  |  \
 *     2   3   4
 *        / \
 *       5   6
 *
 * Serialized:
 *   "1 3 2 0 3 2 5 0 6 0 4 0"
 *
 * Explanation:
 *   - Node 1 has 3 children
 *   - Node 2 has 0 children
 *   - Node 3 has 2 children
 *   - Node 5 has 0 children
 *   - Node 6 has 0 children
 *   - Node 4 has 0 children
 *
 * This representation is uniquely decodable because childCount tells us
 * exactly how many recursive nodes belong to each parent.
 *
 * Time Complexity:
 *   Serialize:   O(N)
 *   Deserialize: O(N)
 *   where N = number of nodes in the tree.
 *
 * Space Complexity:
 *   Serialize:   O(N) output string + O(H) recursion stack
 *   Deserialize: O(N) tree reconstruction + O(H) recursion stack
 *   where H = height of the tree.
 */
class Codec {

    /**
     * Definition of N-ary tree node.
     */
    static class Node {
        int val;
        List<Node> children;

        Node(int val) {
            this.val = val;
            this.children = new ArrayList<>();
        }
    }

    // -----------------------------------------------------------
    // SERIALIZATION
    // -----------------------------------------------------------

    /**
     * Serializes an N-ary tree into a string.
     *
     * Approach:
     *   Preorder DFS traversal.
     *   For each node, store:
     *      (value, number of children)
     *
     * Example output:
     *   "1 3 2 0 3 2 5 0 6 0 4 0"
     */
    public String serialize(Node root) {
        if (root == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        serializeDfs(root, sb);

        // We may end with a trailing space, remove it for cleanliness
        return sb.toString().trim();
    }

    /**
     * DFS helper for serialization.
     *
     * For every node:
     *   1) append node value
     *   2) append number of children
     *   3) recursively serialize children
     */
    private void serializeDfs(Node node, StringBuilder sb) {

        // Store node value
        sb.append(node.val).append(" ");

        // Store number of children
        sb.append(node.children.size()).append(" ");

        // Serialize each child subtree
        for (Node child : node.children) {
            serializeDfs(child, sb);
        }
    }

    // -----------------------------------------------------------
    // DESERIALIZATION
    // -----------------------------------------------------------

    /**
     * Deserializes a string back into an N-ary tree.
     *
     * We read the data in preorder sequence.
     * Each node is represented as:
     *   value childCount
     *
     * Then recursively build exactly childCount children.
     */
    public Node deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        // Split by whitespace (handles multiple spaces safely)
        String[] parts = data.trim().split("\\s+");

        // Use an index pointer wrapped in array (mutable reference)
        int[] idx = new int[1];

        return deserializeDfs(parts, idx);
    }

    /**
     * DFS helper for deserialization.
     *
     * Reads:
     *   parts[idx]     -> node value
     *   parts[idx + 1] -> child count
     *
     * Then recursively builds all children.
     */
    private Node deserializeDfs(String[] parts, int[] idx) {

        // Read node value
        int val = Integer.parseInt(parts[idx[0]++]);

        // Read number of children
        int childCount = Integer.parseInt(parts[idx[0]++]);

        // Create node
        Node node = new Node(val);

        // Build exactly childCount children
        for (int i = 0; i < childCount; i++) {
            node.children.add(deserializeDfs(parts, idx));
        }

        return node;
    }
}



/**
 * PROBLEM ANALYSIS: SERIALIZE & DESERIALIZE N-ARY TREE
 * =====================================================
 * 
 * PROBLEM UNDERSTANDING:
 * - Given an N-ary tree (each node can have 0 to N children)
 * - Need to convert tree to string (serialize)
 * - Need to convert string back to tree (deserialize)
 * - The deserialized tree should be exactly the same as original
 * 
 * KEY CHALLENGES:
 * 1. Variable number of children per node
 * 2. Need to know where one node's children end and next begins
 * 3. Must handle null/empty trees
 * 4. Format should be unambiguous and parseable
 * 
 * INTERVIEW APPROACH - HOW TO THINK THROUGH THIS:
 * ================================================
 * 
 * Step 1: Understand the structure
 * - N-ary tree: unlike binary tree, can have any number of children
 * - Need to encode: node value + number of children + all children
 * 
 * Step 2: Think about encoding schemes
 * - Option 1: Level-order with delimiters
 * - Option 2: Pre-order with child count
 * - Option 3: Parenthetical notation
 * - Option 4: JSON-like format
 * 
 * Step 3: Choose the approach
 * - Pre-order with child count is most efficient
 * - Format: "value,childCount,child1,child2,...,childN"
 * - Easy to parse: read value, read count, recursively read that many children
 * 
 * Step 4: Consider edge cases
 * - Empty tree (null root)
 * - Single node (leaf)
 * - Deep tree vs wide tree
 * - Negative values, large values
 * 
 * Step 5: Think about complexity
 * - Time: O(n) for both serialize and deserialize
 * - Space: O(n) for the string, O(h) for recursion stack
 * 
 * COMMON INTERVIEW MISTAKES:
 * ==========================
 * 1. Not handling the variable number of children properly
 * 2. Forgetting to encode the child count
 * 3. Off-by-one errors when parsing
 * 4. Not handling null root
 * 5. Incorrect delimiter handling
 */

// Definition for a Node.
class Node {
    public int val;
    public List<Node> children;

    public Node() {
        children = new ArrayList<>();
    }

    public Node(int _val) {
        val = _val;
        children = new ArrayList<>();
    }

    public Node(int _val, List<Node> _children) {
        val = _val;
        children = _children;
    }
}

/**
 * APPROACH 1: PRE-ORDER TRAVERSAL WITH CHILD COUNT
 * =================================================
 * 
 * INTUITION:
 * - Store each node as: value + number of children
 * - Then recursively store all children
 * - During deserialization, we know exactly how many children to read
 * 
 * FORMAT: "val,childCount,child1Data,child2Data,..."
 * Example: Tree with root 1 having children [3,2,4]
 *          where 3 has children [5,6]
 * Serialized: "1,3,3,2,5,0,6,0,2,0,4,0"
 * 
 * ADVANTAGES:
 * - Simple and intuitive
 * - Easy to parse
 * - No ambiguity
 */
class Codec1 {
    
    /**
     * SERIALIZE - Convert tree to string
     * 
     * ALGORITHM:
     * 1. If root is null, return empty string
     * 2. Add root value
     * 3. Add number of children
     * 4. Recursively serialize each child
     */
    public String serialize(Node root) {
        if (root == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }
    
    private void serializeHelper(Node node, StringBuilder sb) {
        if (node == null) {
            return;
        }
        
        // Add node value
        sb.append(node.val).append(",");
        
        // Add number of children
        sb.append(node.children.size()).append(",");
        
        // Recursively serialize each child
        for (Node child : node.children) {
            serializeHelper(child, sb);
        }
    }
    
    /**
     * DESERIALIZE - Convert string back to tree
     * 
     * ALGORITHM:
     * 1. If string is empty, return null
     * 2. Split string by delimiter
     * 3. Use index to track current position
     * 4. Recursively build tree:
     *    - Read value
     *    - Read child count
     *    - Recursively build that many children
     */
    public Node deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        String[] tokens = data.split(",");
        int[] index = {0}; // Use array to pass by reference
        return deserializeHelper(tokens, index);
    }
    
    private Node deserializeHelper(String[] tokens, int[] index) {
        if (index[0] >= tokens.length) {
            return null;
        }
        
        // Read node value
        int val = Integer.parseInt(tokens[index[0]++]);
        Node node = new Node(val);
        
        // Read number of children
        int childCount = Integer.parseInt(tokens[index[0]++]);
        
        // Recursively build children
        for (int i = 0; i < childCount; i++) {
            node.children.add(deserializeHelper(tokens, index));
        }
        
        return node;
    }
}

/**
 * APPROACH 2: LEVEL-ORDER TRAVERSAL WITH MARKERS
 * ===============================================
 * 
 * INTUITION:
 * - Use level-order traversal (BFS)
 * - Use '#' to mark end of children for a node
 * - More intuitive for some people, similar to binary tree serialization
 * 
 * FORMAT: "val child1 child2 ... # val child1 # ..."
 * Example: Same tree as before
 * Serialized: "1 3 2 4 # 3 5 6 # 2 # 4 # 5 # 6 #"
 */
class Codec2 {
    
    public String serialize(Node root) {
        if (root == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            sb.append(node.val).append(" ");
            
            // Add all children
            for (Node child : node.children) {
                sb.append(child.val).append(" ");
                queue.offer(child);
            }
            
            // Mark end of this node's children
            sb.append("# ");
        }
        
        return sb.toString().trim();
    }
    
    public Node deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        String[] tokens = data.split(" ");
        Node root = new Node(Integer.parseInt(tokens[0]));
        Queue<Node> queue = new LinkedList<>();
        queue.offer(root);
        
        int i = 1;
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            
            // Read children until we hit '#'
            while (!tokens[i].equals("#")) {
                Node child = new Node(Integer.parseInt(tokens[i++]));
                node.children.add(child);
                queue.offer(child);
            }
            i++; // Skip the '#'
        }
        
        return root;
    }
}

/**
 * APPROACH 3: PARENTHETICAL NOTATION (MOST READABLE)
 * ===================================================
 * 
 * INTUITION:
 * - Represent tree like: value[child1[...] child2[...] ...]
 * - Similar to how we write nested structures
 * - Very readable and intuitive
 * 
 * FORMAT: "val[child1 child2 ...]"
 * Example: Same tree
 * Serialized: "1[3[5 6] 2 4]"
 */
class Codec3 {
    
    public String serialize(Node root) {
        if (root == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }
    
    private void serializeHelper(Node node, StringBuilder sb) {
        sb.append(node.val);
        
        if (!node.children.isEmpty()) {
            sb.append("[");
            for (int i = 0; i < node.children.size(); i++) {
                if (i > 0) sb.append(" ");
                serializeHelper(node.children.get(i), sb);
            }
            sb.append("]");
        }
    }
    
    public Node deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        int[] index = {0};
        return deserializeHelper(data, index);
    }
    
    private Node deserializeHelper(String data, int[] index) {
        if (index[0] >= data.length()) {
            return null;
        }
        
        // Read value (may be multi-digit or negative)
        int start = index[0];
        while (index[0] < data.length() && 
               (Character.isDigit(data.charAt(index[0])) || 
                data.charAt(index[0]) == '-')) {
            index[0]++;
        }
        
        int val = Integer.parseInt(data.substring(start, index[0]));
        Node node = new Node(val);
        
        // Check if there are children
        if (index[0] < data.length() && data.charAt(index[0]) == '[') {
            index[0]++; // Skip '['
            
            while (data.charAt(index[0]) != ']') {
                node.children.add(deserializeHelper(data, index));
                
                // Skip space if present
                if (index[0] < data.length() && data.charAt(index[0]) == ' ') {
                    index[0]++;
                }
            }
            
            index[0]++; // Skip ']'
        }
        
        return node;
    }
}

/**
 * APPROACH 4: OPTIMAL - PREORDER WITH SIZE ENCODING
 * ==================================================
 * 
 * This is typically the best approach for interviews as it's:
 * - Simple to implement
 * - Easy to explain
 * - Efficient
 * - No ambiguity in parsing
 */
class Codec4 {
    
    // Encodes a tree to a single string.
    public String serialize(Node root) {
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }
    
    private void serializeHelper(Node root, StringBuilder sb) {
        if (root == null) {
            return;
        }
        
        // Append value and child count
        sb.append(root.val);
        sb.append(",");
        sb.append(root.children.size());
        sb.append(",");
        
        // Serialize all children
        for (Node child : root.children) {
            serializeHelper(child, sb);
        }
    }
    
    // Decodes your encoded data to tree.
    public Node deserialize(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        String[] nodes = data.split(",");
        int[] index = new int[]{0};
        return deserializeHelper(nodes, index);
    }
    
    private Node deserializeHelper(String[] nodes, int[] index) {
        if (index[0] >= nodes.length) {
            return null;
        }
        
        // Create node with value
        Node root = new Node(Integer.parseInt(nodes[index[0]++]));
        
        // Get number of children
        int childCount = Integer.parseInt(nodes[index[0]++]);
        
        // Deserialize all children
        for (int i = 0; i < childCount; i++) {
            root.children.add(deserializeHelper(nodes, index));
        }
        
        return root;
    }
}

/**
 * COMPLEXITY ANALYSIS
 * ===================
 * 
 * For all approaches:
 * 
 * Serialize:
 * - Time: O(n) where n is number of nodes (visit each node once)
 * - Space: O(n) for the output string, O(h) for recursion stack
 * 
 * Deserialize:
 * - Time: O(n) where n is number of nodes (construct each node once)
 * - Space: O(n) for the tree, O(h) for recursion stack
 * 
 * Where h is the height of the tree.
 * 
 * 
 * INTERVIEW TIPS AND STRATEGY
 * ============================
 * 
 * 1. CLARIFY THE PROBLEM:
 *    - "Can node values be negative?" (Usually yes)
 *    - "Can the tree be empty?" (Usually yes)
 *    - "Do I need to handle very large trees?" (Discuss scalability)
 * 
 * 2. START WITH EXAMPLES:
 *    - Draw a small tree on the whiteboard
 *    - Show what the serialized string looks like
 *    - Walk through deserializing it step by step
 * 
 * 3. CHOOSE YOUR APPROACH:
 *    Recommend: Preorder with child count (Approach 1/Codec)
 *    Why:
 *    - Simplest to implement
 *    - Easiest to explain
 *    - No ambiguity in parsing
 *    - Natural recursive structure
 * 
 * 4. EXPLAIN YOUR DESIGN:
 *    "I'll use preorder traversal where each node stores:
 *     - Its value
 *     - The number of children it has
 *     - Then recursively all its children
 *     
 *     During deserialization, when I read a node, I know
 *     exactly how many children to recursively deserialize."
 * 
 * 5. CODE INCREMENTALLY:
 *    - Start with serialize
 *    - Test with a simple example
 *    - Then implement deserialize
 *    - Test round-trip
 * 
 * 6. DISCUSS EDGE CASES:
 *    - Empty tree (null root)
 *    - Single node (leaf)
 *    - Deep tree (test recursion)
 *    - Wide tree (many children per node)
 *    - Negative values
 * 
 * 7. ALTERNATIVE APPROACHES:
 *    If interviewer asks for alternatives, mention:
 *    - Level-order with markers (Approach 2)
 *    - Parenthetical notation (Approach 3)
 *    - JSON format (if allowed to use libraries)
 * 
 * 8. FOLLOW-UP QUESTIONS TO EXPECT:
 *    - "How would you handle very large trees?" (streaming, chunking)
 *    - "Can you make it more space efficient?" (discuss encoding)
 *    - "How would you serialize to binary format?" (protobuf, etc.)
 *    - "How does this compare to binary tree serialization?"
 * 
 * 9. KEY DIFFERENCES FROM BINARY TREE:
 *    - Binary tree: know there are 0, 1, or 2 children
 *    - N-ary tree: need to explicitly encode child count
 *    - Can't use simple null markers the same way
 * 
 * 10. COMMON MISTAKES TO AVOID:
 *     - Forgetting to handle null root
 *     - Not encoding the number of children
 *     - Off-by-one errors in parsing
 *     - Not handling multi-digit numbers correctly
 *     - Delimiter issues (using delimiter that could be in data)
 */
