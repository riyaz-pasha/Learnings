/*
 * Given a n * n matrix grid of 0's and 1's only. We want to represent grid with
 * a Quad-Tree.
 * 
 * Return the root of the Quad-Tree representing grid.
 * 
 * A Quad-Tree is a tree data structure in which each internal node has exactly
 * four children. Besides, each node has two attributes:
 * 
 * val: True if the node represents a grid of 1's or False if the node
 * represents a grid of 0's. Notice that you can assign the val to True or False
 * when isLeaf is False, and both are accepted in the answer.
 * isLeaf: True if the node is a leaf node on the tree or False if the node has
 * four children.
 * class Node {
 * public boolean val;
 * public boolean isLeaf;
 * public Node topLeft;
 * public Node topRight;
 * public Node bottomLeft;
 * public Node bottomRight;
 * }
 * We can construct a Quad-Tree from a two-dimensional area using the following
 * steps:
 * 
 * If the current grid has the same value (i.e all 1's or all 0's) set isLeaf
 * True and set val to the value of the grid and set the four children to Null
 * and stop.
 * If the current grid has different values, set isLeaf to False and set val to
 * any value and divide the current grid into four sub-grids as shown in the
 * photo.
 * Recurse for each of the children with the proper sub-grid.
 * 
 * If you want to know more about the Quad-Tree, you can refer to the wiki.
 * 
 * Quad-Tree format:
 * 
 * You don't need to read this section for solving the problem. This is only if
 * you want to understand the output format here. The output represents the
 * serialized format of a Quad-Tree using level order traversal, where null
 * signifies a path terminator where no node exists below.
 * 
 * It is very similar to the serialization of the binary tree. The only
 * difference is that the node is represented as a list [isLeaf, val].
 * 
 * If the value of isLeaf or val is True we represent it as 1 in the list
 * [isLeaf, val] and if the value of isLeaf or val is False we represent it as
 * 0.
 * 
 * Example 1:
 * Input: grid = [[0,1],[1,0]]
 * Output: [[0,1],[1,0],[1,1],[1,1],[1,0]]
 * Explanation: The explanation of this example is shown below:
 * Notice that 0 represents False and 1 represents True in the photo
 * representing the Quad-Tree.
 * 
 * Example 2:
 * Input: grid =
 * [[1,1,1,1,0,0,0,0],[1,1,1,1,0,0,0,0],[1,1,1,1,1,1,1,1],[1,1,1,1,1,1,1,1],[1,1
 * ,1,1,0,0,0,0],[1,1,1,1,0,0,0,0],[1,1,1,1,0,0,0,0],[1,1,1,1,0,0,0,0]]
 * Output:
 * [[0,1],[1,1],[0,1],[1,1],[1,0],null,null,null,null,[1,0],[1,0],[1,1],[1,1]]
 * Explanation: All values in the grid are not the same. We divide the grid into
 * four sub-grids.
 * The topLeft, bottomLeft and bottomRight each has the same value.
 * The topRight have different values so we divide it into 4 sub-grids where
 * each has the same value.
 * Explanation is shown in the photo below:
 */

/**
 * Definition for a QuadTree node.
 */
class Node {
    public boolean val;
    public boolean isLeaf;
    public Node topLeft;
    public Node topRight;
    public Node bottomLeft;
    public Node bottomRight;

    public Node() {
        this.val = false;
        this.isLeaf = false;
        this.topLeft = null;
        this.topRight = null;
        this.bottomLeft = null;
        this.bottomRight = null;
    }

    public Node(boolean val, boolean isLeaf) {
        this.val = val;
        this.isLeaf = isLeaf;
        this.topLeft = null;
        this.topRight = null;
        this.bottomLeft = null;
        this.bottomRight = null;
    }

    public Node(boolean val, boolean isLeaf, Node topLeft, Node topRight, Node bottomLeft, Node bottomRight) {
        this.val = val;
        this.isLeaf = isLeaf;
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
    }
}

// Time Complexity: O(n² log n) for optimized version
// Space Complexity: O(log n) for recursion stack in balanced cases
class Solution {

    // Main method to construct Quad-Tree
    public Node construct(int[][] grid) {
        return buildQuadTree(grid, 0, 0, grid.length);
    }

    /**
     * Recursively builds the quad-tree
     * 
     * @param grid - the input grid
     * @param x    - starting row index
     * @param y    - starting column index
     * @param size - size of current sub-grid
     * @return Node representing the quad-tree for this sub-grid
     */
    private Node buildQuadTree(int[][] grid, int x, int y, int size) {
        // Base case: if all values in current sub-grid are the same
        if (isUniform(grid, x, y, size)) {
            // Create leaf node with the uniform value
            return new Node(grid[x][y] == 1, true);
        }

        // If not uniform, create internal node and divide into 4 quadrants
        Node node = new Node(true, false); // val can be any value for internal nodes

        int halfSize = size / 2;

        // Recursively construct four quadrants
        node.topLeft = buildQuadTree(grid, x, y, halfSize);
        node.topRight = buildQuadTree(grid, x, y + halfSize, halfSize);
        node.bottomLeft = buildQuadTree(grid, x + halfSize, y, halfSize);
        node.bottomRight = buildQuadTree(grid, x + halfSize, y + halfSize, halfSize);

        return node;
    }

    /**
     * Checks if all values in the sub-grid are the same
     * 
     * @param grid - the input grid
     * @param x    - starting row index
     * @param y    - starting column index
     * @param size - size of current sub-grid
     * @return true if all values are uniform, false otherwise
     */
    private boolean isUniform(int[][] grid, int x, int y, int size) {
        int val = grid[x][y];

        for (int i = x; i < x + size; i++) {
            for (int j = y; j < y + size; j++) {
                if (grid[i][j] != val) {
                    return false;
                }
            }
        }

        return true;
    }

    // Alternative optimized approach using bit manipulation
    public Node constructOptimized(int[][] grid) {
        return buildQuadTreeOptimized(grid, 0, 0, grid.length);
    }

    private Node buildQuadTreeOptimized(int[][] grid, int x, int y, int size) {
        if (size == 1) {
            return new Node(grid[x][y] == 1, true);
        }

        int halfSize = size / 2;

        // Recursively construct four quadrants
        Node topLeft = buildQuadTreeOptimized(grid, x, y, halfSize);
        Node topRight = buildQuadTreeOptimized(grid, x, y + halfSize, halfSize);
        Node bottomLeft = buildQuadTreeOptimized(grid, x + halfSize, y, halfSize);
        Node bottomRight = buildQuadTreeOptimized(grid, x + halfSize, y + halfSize, halfSize);

        // Check if all four quadrants are leaves with the same value
        if (topLeft.isLeaf && topRight.isLeaf && bottomLeft.isLeaf && bottomRight.isLeaf &&
                topLeft.val == topRight.val && topRight.val == bottomLeft.val && bottomLeft.val == bottomRight.val) {
            // Merge into a single leaf node
            return new Node(topLeft.val, true);
        }

        // Otherwise, create internal node
        return new Node(true, false, topLeft, topRight, bottomLeft, bottomRight);
    }

    // Helper method to print the quad-tree in level order (for testing)
    public void printQuadTree(Node root) {
        if (root == null) {
            System.out.println("[]");
            return;
        }

        java.util.Queue<Node> queue = new java.util.LinkedList<>();
        queue.offer(root);
        java.util.List<String> result = new java.util.ArrayList<>();

        while (!queue.isEmpty()) {
            Node node = queue.poll();

            if (node == null) {
                result.add("null");
                continue;
            }

            // Add current node representation [isLeaf, val]
            result.add("[" + (node.isLeaf ? "1" : "0") + "," + (node.val ? "1" : "0") + "]");

            if (!node.isLeaf) {
                queue.offer(node.topLeft);
                queue.offer(node.topRight);
                queue.offer(node.bottomLeft);
                queue.offer(node.bottomRight);
            }
        }

        // Remove trailing nulls
        while (!result.isEmpty() && result.get(result.size() - 1).equals("null")) {
            result.remove(result.size() - 1);
        }

        System.out.println(result);
    }

    // Helper method to create grid from array (for testing)
    public static int[][] createGrid(int[][] arr) {
        return arr;
    }

    // Test the solution
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1: [[0,1],[1,0]]
        System.out.println("Test Case 1:");
        int[][] grid1 = { { 0, 1 }, { 1, 0 } };
        Node root1 = solution.construct(grid1);
        System.out.print("Output: ");
        solution.printQuadTree(root1);
        System.out.println();

        // Test case 2: 8x8 grid
        System.out.println("Test Case 2:");
        int[][] grid2 = {
                { 1, 1, 1, 1, 0, 0, 0, 0 },
                { 1, 1, 1, 1, 0, 0, 0, 0 },
                { 1, 1, 1, 1, 1, 1, 1, 1 },
                { 1, 1, 1, 1, 1, 1, 1, 1 },
                { 1, 1, 1, 1, 0, 0, 0, 0 },
                { 1, 1, 1, 1, 0, 0, 0, 0 },
                { 1, 1, 1, 1, 0, 0, 0, 0 },
                { 1, 1, 1, 1, 0, 0, 0, 0 }
        };
        Node root2 = solution.construct(grid2);
        System.out.print("Output: ");
        solution.printQuadTree(root2);
        System.out.println();

        // Test case 3: Uniform grid (all 1's)
        System.out.println("Test Case 3 (Uniform grid):");
        int[][] grid3 = { { 1, 1 }, { 1, 1 } };
        Node root3 = solution.construct(grid3);
        System.out.print("Output: ");
        solution.printQuadTree(root3);
        System.out.println();

        // Test optimized version
        System.out.println("Testing Optimized Version:");
        Node root4 = solution.constructOptimized(grid1);
        System.out.print("Optimized Output: ");
        solution.printQuadTree(root4);
    }

}
