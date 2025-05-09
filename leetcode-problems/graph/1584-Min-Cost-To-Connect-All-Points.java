import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

class MinCostToConnectAllPointsPrims2 {
    public static final int INF = Integer.MAX_VALUE;

    public int minCostConnectPoints(int[][] points) {
        int vertices = points.length;
        int[] minDistances = initMinDistances(vertices);
        boolean[] visited = new boolean[vertices];
        int totalCost = 0;

        for (int i = 0; i < vertices; i++) {
            int minDistanceVertex = findMinDistanceVertex(minDistances, visited);
            visited[minDistanceVertex] = true;
            totalCost += minDistances[minDistanceVertex];
            for (int vertex = 0; vertex < vertices; vertex++) {
                if (!visited[vertex]) {
                    int distance = getDistance(points[minDistanceVertex], points[vertex]);
                    if (distance < minDistances[vertex]) {
                        minDistances[vertex] = distance;
                    }
                }
            }
        }
        return totalCost;
    }

    private int getDistance(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }

    private int findMinDistanceVertex(int[] distances, boolean[] visited) {
        int vertices = distances.length;
        int minDistanceVertex = -1;
        int minDistance = INF;
        for (int vertex = 0; vertex < vertices; vertex++) {
            if (!visited[vertex] && distances[vertex] < minDistance) {
                minDistanceVertex = vertex;
                minDistance = distances[vertex];
            }
        }
        return minDistanceVertex;
    }

    private int[] initMinDistances(int n) {
        int[] minDistances = new int[n];
        Arrays.fill(minDistances, INF);
        minDistances[0] = 0;
        return minDistances;
    }
}

class MinCostToConnectAllPointsPrims {
    public int minCostConnectPoints(int[][] points) {
        int n = points.length;
        PriorityQueue<Edge> queue = new PriorityQueue<>();
        boolean[] visited = new boolean[n];
        int totalCost = 0;
        int edgesUsed = 0;

        visited[0] = true;
        for (int j = 1; j < n; j++) {
            queue.offer(new Edge(0, j, getDistance(points[0], points[j])));
        }

        while (!queue.isEmpty() && edgesUsed < n - 1) {
            Edge edge = queue.poll();
            if (visited[edge.to]) {
                continue;
            }
            visited[edge.to] = true;
            totalCost += edge.weight;
            edgesUsed++;
            for (int next = 0; next < n; next++) {
                if (!visited[next]) {
                    queue.offer(new Edge(edge.to, next, getDistance(points[edge.to], points[next])));
                }
            }
        }
        return totalCost;
    }

    private int getDistance(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }
}

class MinCostToConnectAllPointsKruskals {

    public int minCostConnectPoints(int[][] points) {
        int n = points.length;
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                edges.add(new Edge(i, j, getDistance(points[i], points[j])));
            }
        }
        Collections.sort(edges);
        int totalCost = 0, edgesUsed = 0;
        DisjointSet ds = new DisjointSet(n);
        for (Edge edge : edges) {
            if (ds.union(edge.from, edge.to)) {
                totalCost += edge.weight;
                edgesUsed++;
                if (edgesUsed == n - 1) {
                    break; // MST complete
                }
            }
        }
        return totalCost;
    }

    private int getDistance(int[] a, int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
    }
}

class Edge implements Comparable<Edge> {
    int from, to, weight;

    public Edge(int from, int to, int weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    @Override
    public int compareTo(Edge other) {
        return this.weight - other.weight;
    }
}

class DisjointSet {
    int[] parent, rank;

    public DisjointSet(int n) {
        this.parent = new int[n];
        this.rank = new int[n];
        for (int i = 0; i < n; i++) {
            this.parent[i] = i;
            this.rank[i] = 0;
        }
    }

    public int find(int i) {
        if (this.parent[i] != i) {
            this.parent[i] = this.find(this.parent[i]);
        }
        return this.parent[i];
    }

    public boolean union(int i, int j) {
        int rooI = this.find(i), rootJ = this.find(j);
        if (rooI == rootJ) {
            return false;
        }
        if (this.rank[rooI] < this.rank[rootJ]) {
            this.parent[rooI] = rootJ;
        } else if (this.rank[rooI] > this.rank[rootJ]) {
            this.parent[rootJ] = rooI;
        } else {
            this.parent[rootJ] = rooI;
            this.rank[rooI]++;
        }
        return true;
    }
}