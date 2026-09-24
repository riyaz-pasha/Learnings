import java.util.LinkedList;
import java.util.Queue;

/*
 * There are n cities. Some of them are connected, while some are not. 
 * If city a is connected directly with city b, and city b is connected directly with city c, then city a is connected indirectly with city c.
 * 
 * A province is a group of directly or indirectly connected cities and no other cities outside of the group.
 * 
 * You are given an n x n matrix isConnected where isConnected[i][j] = 1 if the ith city and the jth city are directly connected, 
 * and isConnected[i][j] = 0 otherwise.
 * 
 * Return the total number of provinces.
 */

class NumberOfProvinces {
    public static void main(String[] args) {
        var solution = new ProvinceBFS();
        System.out.println(solution.noOfProvinces(new int[][] { { 1, 1, 0 }, { 1, 1, 0 }, { 0, 0, 1 } }));
        System.out.println(solution.noOfProvinces(new int[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }));
        System.out.println(
                solution.noOfProvinces(new int[][] { { 1, 0, 0, 1 }, { 0, 1, 1, 0 }, { 0, 1, 1, 1 }, { 1, 0, 1, 1 } }));
    }
}

class ProvincesDFS {

    // Time Complexity: O(n²) where n is the number of cities
    // Space Complexity: O(n) for the visited array and recursion stack
    public int noOfProvinces(int[][] isConnected) {
        int len = isConnected.length;
        boolean[] isCityVisited = new boolean[len];
        int province = 0;

        for (int city = 0; city < len; city++) {
            if (!isCityVisited[city]) {
                findConntectedCitiesDFS(isConnected, city, isCityVisited);
                province++;
            }
        }
        return province;
    }

    private void findConntectedCitiesDFS(int[][] isConntected, int city, boolean[] isCityVisited) {
        int len = isConntected.length;
        isCityVisited[city] = true;
        for (int neighbor = 0; neighbor < len; neighbor++) {
            if (isConntected[city][neighbor] == 1 && !isCityVisited[neighbor]) {
                findConntectedCitiesDFS(isConntected, neighbor, isCityVisited);
            }
        }
    }

}

class ProvinceBFS {

    // Time Complexity: O(n²)
    // Space Complexity: O(n) for the visited array and queue
    public int noOfProvinces(int[][] isConnected) {
        int len = isConnected.length;
        boolean[] isCityVisited = new boolean[len];
        int province = 0;

        for (int city = 0; city < len; city++) {
            if (!isCityVisited[city]) {
                findConntectedCitiesBFS(isConnected, city, isCityVisited);
                province++;
            }
        }
        return province;
    }

    private void findConntectedCitiesBFS(int[][] isConnected, int startCity, boolean[] isCityVisited) {
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(startCity);
        isCityVisited[startCity] = true;

        while (!queue.isEmpty()) {
            int currentCity = queue.poll();
            for (int neighbor = 0; neighbor < isConnected.length; neighbor++) {
                if (isConnected[currentCity][neighbor] == 1 && !isCityVisited[neighbor]) {
                    queue.offer(neighbor);
                    isCityVisited[neighbor] = true;
                }
            }
        }
    }

}

class ProvinceUnionFind {

    // Time Complexity: O(n² × α(n)) where α is the inverse Ackermann function
    // (practically constant)
    // Space Complexity: O(n) for parent and rank arrays
    class UnionFind {

        private final int[] parent;
        private final int[] rank;
        private int count;

        public UnionFind(int n) {
            parent = new int[n];
            rank = new int[n];
            count = n;

            for (int i = 0; i < n; i++) {
                parent[i] = i;
                rank[i] = 1;
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
                if (rank[rootX] > rank[rootY]) {
                    parent[rootY] = rootX;
                } else if (rank[rootX] < rank[rootY]) {
                    parent[rootX] = rootY;
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

}
