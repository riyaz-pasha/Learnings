import java.util.LinkedList;
import java.util.Queue;

/*
 * There are n cities. Some of them are connected, while some are not. If city a
 * is connected directly with city b, and city b is connected directly with city
 * c, then city a is connected indirectly with city c.
 * 
 * A province is a group of directly or indirectly connected cities and no other
 * cities outside of the group.
 * 
 * You are given an n x n matrix isConnected where isConnected[i][j] = 1 if the
 * ith city and the jth city are directly connected, and isConnected[i][j] = 0
 * otherwise.
 * 
 * Return the total number of provinces.
 * 
 * Example 1:
 * Input: isConnected = [[1,1,0],[1,1,0],[0,0,1]]
 * Output: 2
 * 
 * Example 2:
 * Input: isConnected = [[1,0,0],[0,1,0],[0,0,1]]
 * Output: 3
 */

class NumberOfProvinces {
    public int findCircleNum(int[][] isConnected) {
        // Find number of connected components
        // do a dfs traversal and everytime a new start node found increase count
        int n = isConnected.length;
        boolean[] isVisited = new boolean[n];
        int count = 0;
        for (int node = 0; node < n; node++) {
            if (!isVisited[node]) {
                count++;
                dfs(isConnected, isVisited, node);
            }
        }
        return count;
    }

    private void dfs(int[][] isConnected, boolean[] isVisited, int current) {
        if (isVisited[current])
            return;
        isVisited[current] = true;
        for (int neighbor = 0; neighbor < isConnected.length; neighbor++) {
            if (isConnected[current][neighbor] == 1) {
                dfs(isConnected, isVisited, neighbor);
            }
        }
    }

}

// Solution 1: DFS (Depth-First Search)
class Solution {
    public int findCircleNum(int[][] isConnected) {
        int n = isConnected.length;
        boolean[] visited = new boolean[n];
        int provinces = 0;

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                dfs(isConnected, visited, i);
                provinces++;
            }
        }

        return provinces;
    }

    private void dfs(int[][] isConnected, boolean[] visited, int city) {
        visited[city] = true;

        for (int j = 0; j < isConnected.length; j++) {
            if (isConnected[city][j] == 1 && !visited[j]) {
                dfs(isConnected, visited, j);
            }
        }
    }
}

// Solution 2: BFS (Breadth-First Search)
class Solution2 {
    public int findCircleNum(int[][] isConnected) {
        int n = isConnected.length;
        boolean[] visited = new boolean[n];
        int provinces = 0;
        Queue<Integer> queue = new LinkedList<>();

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                queue.offer(i);
                visited[i] = true;

                while (!queue.isEmpty()) {
                    int city = queue.poll();

                    for (int j = 0; j < n; j++) {
                        if (isConnected[city][j] == 1 && !visited[j]) {
                            queue.offer(j);
                            visited[j] = true;
                        }
                    }
                }

                provinces++;
            }
        }

        return provinces;
    }
}

// Solution 3: Union-Find (Disjoint Set Union)
class Solution3 {
    public int findCircleNum(int[][] isConnected) {
        int n = isConnected.length;
        UnionFind uf = new UnionFind(n);

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (isConnected[i][j] == 1) {
                    uf.union(i, j);
                }
            }
        }

        return uf.getCount();
    }

    class UnionFind {
        private int[] parent;
        private int[] rank;
        private int count;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            count = n;

            for (int i = 0; i < n; i++) {
                parent[i] = i;
            }
        }

        public int find(int x) {
            if (parent[x] != x) {
                parent[x] = find(parent[x]); // Path compression
            }
            return parent[x];
        }

        public void union(int x, int y) {
            int rootX = find(x);
            int rootY = find(y);

            if (rootX != rootY) {
                // Union by rank
                if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
                } else if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else {
                    parent[rootY] = rootX;
                    rank[rootX]++;
                }
                count--;
            }
        }

        public int getCount() {
            return count;
        }
    }
}

// Solution 4: Optimized Union-Find (Most Efficient)
class Solution4 {
    private int[] parent;
    private int count;

    public int findCircleNum(int[][] isConnected) {
        int n = isConnected.length;
        parent = new int[n];
        count = n;

        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (isConnected[i][j] == 1) {
                    union(i, j);
                }
            }
        }

        return count;
    }

    private int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    private void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        if (rootX != rootY) {
            parent[rootX] = rootY;
            count--;
        }
    }
}
