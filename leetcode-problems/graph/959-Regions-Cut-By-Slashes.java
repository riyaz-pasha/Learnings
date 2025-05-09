/*
    Problem: Given a grid of n x n cells where each cell contains a '/', '\' or ' ' (blank space),
             determine the number of regions formed by these slashes.

    Approach: Union-Find (Disjoint Set Union) with Cell Subdivision

    ------------------------------------------------------
    Concept:
    ------------------------------------------------------

    Each cell is divided into 4 triangles like this:

                 +-------+
                 |\  0  /|
                 | \   / |
                 |3 \ / 1|
                 |  / \  |
                 | / 2 \ |
                 +-------+

    Triangle Labels:
    - 0: Top triangle
    - 1: Right triangle
    - 2: Bottom triangle
    - 3: Left triangle

    Purpose:
    - This layout helps simulate how '/', '\' and ' ' affect the connectivity inside a cell.
    - For each (row, col), we assign these 4 parts to represent the internal structure.

    Connections:
    - '/' connects triangles 0 ↔ 3 and 1 ↔ 2
    - '\' connects triangles 0 ↔ 1 and 2 ↔ 3
    - ' ' connects all triangles: 0 ↔ 1 ↔ 2 ↔ 3

    Neighbor Cell Connections:
    - Bottom (2) connects to top (0) of the cell below
    - Right (1) connects to left (3) of the cell to the right


    ------------------------------------------------------
    Step 1: Subdivide Each Cell
    ------------------------------------------------------

    - We treat each cell as 4 separate nodes (triangles) in a Union-Find data structure.

    - Based on the character in the cell:
        - ' ' (space): Connect all 4 triangles (fully connected)
        - '/' : Connect top (0) with left (3), and right (1) with bottom (2)
        - '\\': Connect top (0) with right (1), and left (3) with bottom (2)

    ------------------------------------------------------
    Step 2: Connect Adjacent Cells
    ------------------------------------------------------

    - To model continuity across cells, we connect specific triangles from adjacent cells:

        - Connect current cell’s bottom triangle (2) with the top triangle (0) of the cell below (if exists)
        - Connect current cell’s right triangle (1) with the left triangle (3) of the cell to the right (if exists)

    - These cross-cell connections ensure that we treat the grid as a continuous surface.

    ------------------------------------------------------
    Step 3: Count Regions
    ------------------------------------------------------

    - After all unions are made, we count how many unique connected components exist in the Union-Find structure.

    - Each unique root represents a separate region in the final partitioned space.

    ------------------------------------------------------
    Intuition:
    ------------------------------------------------------

    - Slashes divide the space inside a cell, while empty spaces allow free flow.
    - By breaking cells into smaller components and carefully connecting them, we can precisely track the influence of slashes.
    - Union-Find allows efficient grouping and identification of connected regions.

    ------------------------------------------------------
    Time Complexity:
    ------------------------------------------------------

    - O(n^2 * α(n)) where α is the inverse Ackermann function (almost constant in practice),
      due to Union-Find operations over 4 * n * n nodes.

    - Space Complexity: O(n^2) for Union-Find array.
*/


class RegionsCutBySlashesUnionFind {
    private final int TOP = 0, RIGHT = 1, BOTTOM = 2, LEFT = 3;

    public int regionsBySlashes(String[] grid) {
        int n = grid.length;
        DisjointSet ds = new DisjointSet(n * n * 4);
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                char ch = grid[row].charAt(col);
                int root = ((row * n) + col) * 4;

                if (ch == ' ') {
                    ds.union(root + TOP, root + RIGHT);
                    ds.union(root + RIGHT, root + BOTTOM);
                    ds.union(root + BOTTOM, root + LEFT);
                } else if (ch == '/') {
                    ds.union(root + TOP, root + LEFT);
                    ds.union(root + BOTTOM, root + RIGHT);
                } else if (ch == '\\') {
                    ds.union(root + TOP, root + RIGHT);
                    ds.union(root + BOTTOM, root + LEFT);
                }

                if (row + 1 < n) {
                    int bottomCellRoot = ((row + 1) * n + col) * 4;
                    ds.union(root + BOTTOM, bottomCellRoot + TOP);
                }

                if (col + 1 < n) {
                    int rightCellRoot = (row * n + (col + 1)) * 4;
                    ds.union(root + RIGHT, rightCellRoot + LEFT);
                }
            }
        }
        int regionCount = 0;
        for (int i = 0; i < n * n * 4; i++) {
            if (ds.find(i) == i) {
                regionCount++;
            }
        }
        return regionCount;
    }

    class DisjointSet {
        int[] parent;

        public DisjointSet(int n) {
            this.parent = new int[n];
            for (int idx = 0; idx < n; idx++) {
                this.parent[idx] = idx;
            }
        }

        public int find(int i) {
            if (this.parent[i] != i) {
                this.parent[i] = this.find(this.parent[i]);
            }
            return this.parent[i];
        }

        public void union(int i, int j) {
            this.parent[this.find(j)] = this.find(i);
        }
    }
}

class RegionsCutBySlashesBFS {
    int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, 1 }, { 0, -1 } };

    public int regionsBySlashes(String[] grid) {
        int n = grid.length;
        int[][] expandedGrid = new int[3 * n][3 * n];
        expandGrid(grid, expandedGrid);

        boolean[][] visited = new boolean[3 * n][3 * n];
        int regionCount = 0;
        for (int i = 0; i < 3 * n; i++) {
            for (int j = 0; j < 3 * n; j++) {
                if (expandedGrid[i][j] == 0 && !visited[i][j]) {
                    dfs(i, j, expandedGrid, visited);
                    regionCount++;
                }
            }
        }
        return regionCount;
    }

    private void expandGrid(String[] grid, int[][] expandedGrid) {
        int n = grid.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                char currentChar = grid[i].charAt(j);
                if (currentChar == '/') {
                    fillSlackBlock(i, j, expandedGrid);
                } else if (currentChar == '\\') {
                    fillBackslackBlock(i, j, expandedGrid);
                } else {
                    fillOpenBlock(i, j, expandedGrid);
                }
            }
        }
    }

    private void dfs(int row, int col, int[][] expandedGrid, boolean[][] visited) {
        visited[row][col] = true;
        int n = expandedGrid.length;
        for (int[] direction : directions) {
            int newRow = row + direction[0];
            int newCol = col + direction[1];
            if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n && !visited[newRow][newCol]
                    && expandedGrid[newRow][newCol] == 0) {
                dfs(newRow, newCol, expandedGrid, visited);
            }
        }
    }

    private void fillSlackBlock(int row, int col, int[][] expandedGrid) {
        int baseRow = 3 * row;
        int baseCol = 3 * col;

        expandedGrid[baseRow + 0][baseCol + 2] = 1;
        expandedGrid[baseRow + 1][baseCol + 1] = 1;
        expandedGrid[baseRow + 2][baseCol + 0] = 1;
    }

    private void fillBackslackBlock(int row, int col, int[][] expandedGrid) {
        int baseRow = 3 * row;
        int baseCol = 3 * col;

        expandedGrid[baseRow + 0][baseCol + 0] = 1;
        expandedGrid[baseRow + 1][baseCol + 1] = 1;
        expandedGrid[baseRow + 2][baseCol + 2] = 1;
    }

    private void fillOpenBlock(int row, int col, int[][] expandedGrid) {
        int baseRow = 3 * row;
        int baseCol = 3 * col;
        for (int i = baseRow; i < baseRow + 3; i++) {
            for (int j = baseCol; j < baseCol + 3; j++) {
                expandedGrid[i][j] = 0;
            }
        }
    }
}
