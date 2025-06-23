class FloydWarshall {

    private final int INF = Integer.MAX_VALUE;

    public int[][] shortestPaths(int numVertices, int[][] graph) {
        int[][] dist = new int[numVertices][numVertices];
        for (int source = 0; source < numVertices; source++) {
            for (int destination = 0; destination < numVertices; destination++) {
                dist[source][destination] = graph[source][destination];
            }
        }

        for (int intermediate = 0; intermediate < numVertices; intermediate++) {
            for (int source = 0; source < numVertices; source++) {
                for (int destination = 0; destination < numVertices; destination++) {
                    if (dist[source][intermediate] != INF
                            && dist[intermediate][destination] != INF
                            && (dist[source][intermediate]
                                    + dist[intermediate][destination]) < dist[source][destination]) {
                        dist[source][destination] = dist[source][intermediate] + dist[intermediate][destination];
                    }
                }
            }
        }

        return dist;
    }

}
