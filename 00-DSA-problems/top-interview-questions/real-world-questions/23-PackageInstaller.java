import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class PackageInstaller {

    /*
     * ✅ Time & Space Complexity:
     * Time: O(V + E)
     * Space: O(V + E)
     */
    public List<String> findInstallOrder(Map<String, List<String>> dependencies) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> graph = new HashMap<>();

        for (String pkg : dependencies.keySet()) {
            inDegree.putIfAbsent(pkg, 0);
            for (String dep : dependencies.get(pkg)) {
                inDegree.put(dep, inDegree.getOrDefault(dep, 0) + 1);
                graph.computeIfAbsent(dep, k -> new ArrayList<>()).add(pkg);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<String> installOrder = new ArrayList<>();
        while (!queue.isEmpty()) {
            String currentPkg = queue.poll();
            installOrder.add(currentPkg);

            for (String dependentPackage : graph.getOrDefault(currentPkg, Collections.emptyList())) {
                inDegree.put(dependentPackage, inDegree.get(dependentPackage) - 1);
                if (inDegree.get(dependentPackage) == 0) {
                    queue.offer(dependentPackage);
                }
            }
        }

        if (installOrder.size() != inDegree.size()) {
            throw new RuntimeException("Cycle detected! No valid install order.");
        }
        return installOrder;
    }

}

class PackageInstallerDFS {

    /*
     * ✅ Time & Space Complexity:
     * Time: O(V + E)
     * Space: O(V + E) for graph + call stack
     */
    public List<String> findInstallOrder(Map<String, List<String>> dependencies) {
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        LinkedList<String> result = new LinkedList<>();

        for (String pkg : dependencies.keySet()) {
            if (!visited.contains(pkg)) {
                if (!this.dfs(pkg, dependencies, visited, visiting, result)) {
                    throw new RuntimeException("Cycle detected! No valid install order.");

                }
            }
        }

        return result;
    }

    private boolean dfs(String pkg, Map<String, List<String>> dependencies, Set<String> visited, Set<String> visiting,
            LinkedList<String> result) {
        if (visiting.contains(pkg)) {
            return false; // Cycle detected
        }
        if (visited.contains(pkg)) {
            return true; // Already processed
        }

        visiting.add(pkg);
        for (String dep : dependencies.getOrDefault(pkg, Collections.emptyList())) {
            if (!this.dfs(dep, dependencies, visited, visiting, result)) {
                return false;
            }
        }

        visiting.remove(pkg);
        visited.add(pkg);
        result.addFirst(pkg);

        return true;
    }

}
