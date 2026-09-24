/*
 * 📌 Problem Summary
 * You are given a 2D grid of 0s and 1s:
 * 
 * 1 represents land
 * 
 * 0 represents water
 * 
 * An island is a group of 1s connected horizontally or vertically.
 * 
 * Two islands are considered distinct if and only if they cannot be transformed
 * into each other via:
 * 
 * Translations
 * 
 * Rotations (90°, 180°, 270°)
 * 
 * Reflections (horizontal or vertical flips)
 * 
 * 🧠 The task is to count the number of distinct islands considering all 8
 * transformations.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Solution {

    // Solution 1: Generate all 8 transformations and find canonical form
    public int numDistinctIslands2(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return 0;
        }

        int m = grid.length;
        int n = grid[0].length;
        Set<String> distinctIslands = new HashSet<>();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    List<int[]> island = new ArrayList<>();
                    dfs(grid, i, j, island);
                    String canonical = getCanonicalForm(island);
                    distinctIslands.add(canonical);
                }
            }
        }

        return distinctIslands.size();
    }

    private void dfs(int[][] grid, int i, int j, List<int[]> island) {
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length ||
                grid[i][j] == 0) {
            return;
        }

        grid[i][j] = 0; // Mark as visited
        island.add(new int[] { i, j });

        // Explore all 4 directions
        dfs(grid, i + 1, j, island);
        dfs(grid, i - 1, j, island);
        dfs(grid, i, j + 1, island);
        dfs(grid, i, j - 1, island);
    }

    private String getCanonicalForm(List<int[]> island) {
        // Generate all 8 possible transformations
        List<List<int[]>> transformations = generateAllTransformations(island);

        // Find the lexicographically smallest representation
        String canonical = null;
        for (List<int[]> transformation : transformations) {
            String signature = normalizeAndSerialize(transformation);
            if (canonical == null || signature.compareTo(canonical) < 0) {
                canonical = signature;
            }
        }

        return canonical;
    }

    private List<List<int[]>> generateAllTransformations(List<int[]> island) {
        List<List<int[]>> transformations = new ArrayList<>();

        // Original
        transformations.add(new ArrayList<>(island));

        // 90, 180, 270 degree rotations
        transformations.add(rotate90(island));
        transformations.add(rotate180(island));
        transformations.add(rotate270(island));

        // Reflections (horizontal and vertical)
        transformations.add(reflectHorizontal(island));
        transformations.add(reflectVertical(island));

        // Reflections + rotations
        transformations.add(rotate90(reflectHorizontal(island)));
        transformations.add(rotate90(reflectVertical(island)));

        return transformations;
    }

    private List<int[]> rotate90(List<int[]> island) {
        List<int[]> rotated = new ArrayList<>();
        for (int[] cell : island) {
            // (x, y) -> (-y, x)
            rotated.add(new int[] { -cell[1], cell[0] });
        }
        return rotated;
    }

    private List<int[]> rotate180(List<int[]> island) {
        List<int[]> rotated = new ArrayList<>();
        for (int[] cell : island) {
            // (x, y) -> (-x, -y)
            rotated.add(new int[] { -cell[0], -cell[1] });
        }
        return rotated;
    }

    private List<int[]> rotate270(List<int[]> island) {
        List<int[]> rotated = new ArrayList<>();
        for (int[] cell : island) {
            // (x, y) -> (y, -x)
            rotated.add(new int[] { cell[1], -cell[0] });
        }
        return rotated;
    }

    private List<int[]> reflectHorizontal(List<int[]> island) {
        List<int[]> reflected = new ArrayList<>();
        for (int[] cell : island) {
            // (x, y) -> (x, -y)
            reflected.add(new int[] { cell[0], -cell[1] });
        }
        return reflected;
    }

    private List<int[]> reflectVertical(List<int[]> island) {
        List<int[]> reflected = new ArrayList<>();
        for (int[] cell : island) {
            // (x, y) -> (-x, y)
            reflected.add(new int[] { -cell[0], cell[1] });
        }
        return reflected;
    }

    private String normalizeAndSerialize(List<int[]> island) {
        if (island.isEmpty())
            return "";

        // Sort points to ensure consistent ordering
        island.sort((a, b) -> {
            if (a[0] != b[0])
                return Integer.compare(a[0], b[0]);
            return Integer.compare(a[1], b[1]);
        });

        // Normalize to start from (0,0)
        int minX = island.get(0)[0];
        int minY = island.get(0)[1];

        for (int[] cell : island) {
            minX = Math.min(minX, cell[0]);
            minY = Math.min(minY, cell[1]);
        }

        StringBuilder sb = new StringBuilder();
        for (int[] cell : island) {
            sb.append(cell[0] - minX).append(",").append(cell[1] - minY).append(";");
        }

        return sb.toString();
    }

}

// Alternative Solution 2: Using Complex Numbers for Transformations
class Solution2 {

    public int numDistinctIslands2(int[][] grid) {
        if (grid == null || grid.length == 0 || grid[0].length == 0) {
            return 0;
        }

        int m = grid.length;
        int n = grid[0].length;
        Set<Set<String>> distinctIslands = new HashSet<>();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] == 1) {
                    List<int[]> island = new ArrayList<>();
                    dfs(grid, i, j, island);
                    Set<String> canonical = getCanonicalFormSet(island);
                    distinctIslands.add(canonical);
                }
            }
        }

        return distinctIslands.size();
    }

    private void dfs(int[][] grid, int i, int j, List<int[]> island) {
        if (i < 0 || i >= grid.length || j < 0 || j >= grid[0].length ||
                grid[i][j] == 0) {
            return;
        }

        grid[i][j] = 0;
        island.add(new int[] { i, j });

        dfs(grid, i + 1, j, island);
        dfs(grid, i - 1, j, island);
        dfs(grid, i, j + 1, island);
        dfs(grid, i, j - 1, island);
    }

    private Set<String> getCanonicalFormSet(List<int[]> island) {
        Set<String> transformations = new HashSet<>();

        // All 8 transformations using multiplication by complex numbers
        int[][] transforms = {
                { 1, 0, 0, 1 }, // identity
                { 0, 1, -1, 0 }, // 90 degree rotation
                { -1, 0, 0, -1 }, // 180 degree rotation
                { 0, -1, 1, 0 }, // 270 degree rotation
                { 1, 0, 0, -1 }, // reflection over x-axis
                { -1, 0, 0, 1 }, // reflection over y-axis
                { 0, 1, 1, 0 }, // reflection over y=x
                { 0, -1, -1, 0 } // reflection over y=-x
        };

        for (int[] transform : transforms) {
            List<int[]> transformed = new ArrayList<>();
            for (int[] cell : island) {
                int x = cell[0], y = cell[1];
                int newX = transform[0] * x + transform[1] * y;
                int newY = transform[2] * x + transform[3] * y;
                transformed.add(new int[] { newX, newY });
            }
            transformations.add(normalizeAndSerialize(transformed));
        }

        // Return the lexicographically smallest transformation
        return Collections.singleton(transformations.stream().min(String::compareTo).orElse(""));
    }

    private String normalizeAndSerialize(List<int[]> island) {
        if (island.isEmpty())
            return "";

        // Sort and normalize
        island.sort((a, b) -> {
            if (a[0] != b[0])
                return Integer.compare(a[0], b[0]);
            return Integer.compare(a[1], b[1]);
        });

        int minX = island.get(0)[0];
        int minY = island.get(0)[1];
        for (int[] cell : island) {
            minX = Math.min(minX, cell[0]);
            minY = Math.min(minY, cell[1]);
        }

        StringBuilder sb = new StringBuilder();
        for (int[] cell : island) {
            sb.append(cell[0] - minX).append(",").append(cell[1] - minY).append(";");
        }

        return sb.toString();
    }

}

// Test class
class TestDistinctIslandsII {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test Case 1
        int[][] grid1 = {
                { 1, 1, 0, 0, 0 },
                { 1, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 1 },
                { 0, 0, 0, 1, 1 }
        };
        System.out.println("Test 1: " + solution.numDistinctIslands2(deepCopy(grid1)));
        // Expected: 1 (second island is 180° rotation of first)

        // Test Case 2
        int[][] grid2 = {
                { 1, 1, 1, 0, 0 },
                { 1, 0, 0, 0, 1 },
                { 0, 1, 0, 0, 1 },
                { 0, 1, 0, 0, 1 }
        };
        System.out.println("Test 2: " + solution.numDistinctIslands2(deepCopy(grid2)));
        // Expected: 2

        // Test Case 3: Same shape with different orientations
        int[][] grid3 = {
                { 1, 1, 0, 0 },
                { 0, 1, 0, 0 },
                { 0, 0, 1, 0 },
                { 0, 0, 1, 1 }
        };
        System.out.println("Test 3: " + solution.numDistinctIslands2(deepCopy(grid3)));
        // Expected: 1 (both L-shapes, second is rotated)

        // Test Case 4: Reflections
        int[][] grid4 = {
                { 1, 0, 1 },
                { 1, 1, 1 },
                { 0, 1, 0 }
        };
        System.out.println("Test 4: " + solution.numDistinctIslands2(deepCopy(grid4)));
        // Expected: 1 (symmetric shape)
    }

    private static int[][] deepCopy(int[][] original) {
        int[][] copy = new int[original.length][];
        for (int i = 0; i < original.length; i++) {
            copy[i] = original[i].clone();
        }
        return copy;
    }

}

/*
 * ### Solution 1: Generate All 8 Transformations
 ** 
 * Time Complexity: O(m × n × k × log k)**
 * - DFS traversal: O(m × n) to visit all cells
 * - For each island of size k: Generate 8 transformations × O(k) each = O(k)
 * - Sorting coordinates for normalization: O(k log k) per transformation
 * - String comparison for canonical form: O(k) per transformation
 * - Total per island: O(k log k)
 * - Overall: O(m × n × k × log k), where k is average island size
 ** 
 * Space Complexity: O(m × n × k)**
 * - HashSet storing canonical forms: O(number of islands × signature length)
 * - Storing transformations: O(8 × k) per island
 * - DFS call stack: O(k) for largest island
 * - Total: O(m × n × k)
 * 
 * ### Solution 2: Complex Number Transformations
 ** 
 * Time Complexity: O(m × n × k × log k)**
 * - Same complexity as Solution 1
 * - Using transformation matrices instead of individual rotation/reflection
 * methods
 ** 
 * Space Complexity: O(m × n × k)**
 * - Similar space requirements as Solution 1
 * 
 * ## Algorithm Explanation
 * 
 * ### Key Challenge
 * Unlike the original problem, two islands are considered the same if one can
 * be transformed into the other through:
 * - **Rotations**: 90°, 180°, 270° clockwise
 * - **Reflections**: Horizontal (over x-axis), Vertical (over y-axis), and
 * diagonal reflections
 * 
 * ### Solution Approach
 * 
 * #### Step 1: Island Collection
 * Use DFS/BFS to collect all cells belonging to each island, storing their
 * coordinates.
 * 
 * #### Step 2: Generate All Transformations
 * For each island, generate all 8 possible transformations:
 * 
 * 1. **Identity**: No change
 * 2. **90° Rotation**: (x, y) → (-y, x)
 * 3. **180° Rotation**: (x, y) → (-x, -y)
 * 4. **270° Rotation**: (x, y) → (y, -x)
 * 5. **Horizontal Reflection**: (x, y) → (x, -y)
 * 6. **Vertical Reflection**: (x, y) → (-x, y)
 * 7. **Diagonal Reflection 1**: (x, y) → (y, x)
 * 8. **Diagonal Reflection 2**: (x, y) → (-y, -x)
 * 
 * #### Step 3: Normalize and Find Canonical Form
 * For each transformation:
 * 1. **Sort coordinates** to ensure consistent ordering
 * 2. **Translate to origin** by subtracting minimum x and y coordinates
 * 3. **Serialize to string** for comparison
 * 4. **Select lexicographically smallest** as canonical form
 * 
 * #### Step 4: Store and Count
 * Add canonical form to HashSet and return its size.
 * 
 * ### Example Walkthrough
 * 
 * Consider these two islands:
 * ```
 * Island 1: Island 2:
 * 11 1
 * 1 11
 * ```
 ** 
 * Island 1 transformations:**
 * - Original: [(0,0), (0,1), (1,0)] → Normalized: "0,0;0,1;1,0;"
 * - 180° rotation: [(0,0), (0,-1), (-1,0)] → Normalized: "0,0;0,1;1,0;"
 ** 
 * Island 2 transformations:**
 * - Original: [(0,0), (1,0), (1,1)] → Normalized: "0,0;1,0;1,1;"
 * - 180° rotation: [(0,0), (-1,0), (-1,-1)] → Normalized: "0,0;1,0;1,1;"
 * 
 * Both islands generate the same canonical form, so they're considered
 * identical.
 * 
 * ### Why 8 Transformations?
 * The dihedral group D₄ (symmetries of a square) has 8 elements:
 * - 4 rotations (0°, 90°, 180°, 270°)
 * - 4 reflections (horizontal, vertical, two diagonals)
 * 
 * This covers all possible ways to transform a shape while preserving its
 * structure.
 * 
 * The algorithm ensures that any two islands with the same shape (regardless of
 * orientation) will have identical canonical representations, making the
 * counting accurate.
 */