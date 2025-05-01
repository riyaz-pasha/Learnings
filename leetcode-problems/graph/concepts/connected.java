import java.util.ArrayList;
import java.util.List;

class ConnectedComponents {
    private final int numVertices;
    private final List<List<Integer>> adjList;

    public ConnectedComponents(int numVertices) {
        this.numVertices = numVertices;
        adjList = new ArrayList<>();
        for (int i = 0; i < this.numVertices; i++) {
            adjList.add(new ArrayList<>());
        }
    }

    public void addEdge(int source, int destination) {
        adjList.get(source).add(destination);
        adjList.get(destination).add(source); // undirected graph
    }

    public List<List<Integer>> findConnectedComponents() {
        boolean[] visited = new boolean[this.numVertices];
        List<List<Integer>> components = new ArrayList<>();

        List<Integer> component;
        for (int i = 0; i < this.numVertices; i++) {
            if (!visited[i]) {
                component = new ArrayList<>();
                this.dfs(i, visited, component);
                components.add(component);
            }
        }
        return components;
    }

    private void dfs(int i, boolean[] visited, List<Integer> component) {
        visited[i] = true;
        component.add(i);
        for (int neighbor : adjList.get(i)) {
            if (!visited[neighbor]) {
                dfs(neighbor, visited, component);
            }
        }
    }

    public static void main(String[] args) {
        ConnectedComponents g = new ConnectedComponents(7);
        g.addEdge(0, 1);
        g.addEdge(1, 2);
        g.addEdge(2, 0);
        g.addEdge(3, 4);
        g.addEdge(5, 6);

        List<List<Integer>> connectedComponents = g.findConnectedComponents();

        System.out.println("Connected components in the graph:");
        for (List<Integer> component : connectedComponents) {
            System.out.println(component);
        }
    }
}
