import java.util.*;
/*
 * There are n items each belonging to zero or one of m groups where group[i] is
 * the group that the i-th item belongs to and it's equal to -1 if the i-th item
 * belongs to no group. The items and the groups are zero indexed. A group can
 * have no item belonging to it.
 * 
 * Return a sorted list of the items such that:
 * 
 * The items that belong to the same group are next to each other in the sorted
 * list.
 * There are some relations between these items where beforeItems[i] is a list
 * containing all the items that should come before the i-th item in the sorted
 * array (to the left of the i-th item).
 * Return any solution if there is more than one solution and return an empty
 * list if there is no solution.
 * 
 * Example 1:
 * Input: n = 8, m = 2, group = [-1,-1,1,0,0,1,0,-1], beforeItems =
 * [[],[6],[5],[6],[3,6],[],[],[]]
 * Output: [6,3,4,1,5,2,0,7]
 * 
 * Example 2:
 * Input: n = 8, m = 2, group = [-1,-1,1,0,0,1,0,-1], beforeItems =
 * [[],[6],[5],[6],[3],[],[4],[]]
 * Output: []
 * Explanation: This is the same as example 1 except that 4 needs to be before 6
 * in the sorted list.
 */

class Solution {

    public int[] sortItems(int n, int m, int[] group, List<List<Integer>> beforeItems) {
        // Assign unique group IDs to items without groups
        int groupId = m;
        for (int i = 0; i < n; i++) {
            if (group[i] == -1) {
                group[i] = groupId++;
            }
        }

        // Build group adjacency list and in-degree count
        Map<Integer, List<Integer>> groupGraph = new HashMap<>();
        Map<Integer, Integer> groupIndegree = new HashMap<>();

        // Build item adjacency list and in-degree count
        Map<Integer, List<Integer>> itemGraph = new HashMap<>();
        Map<Integer, Integer> itemIndegree = new HashMap<>();

        // Initialize graphs
        for (int i = 0; i < groupId; i++) {
            groupGraph.put(i, new ArrayList<>());
            groupIndegree.put(i, 0);
        }

        for (int i = 0; i < n; i++) {
            itemGraph.put(i, new ArrayList<>());
            itemIndegree.put(i, 0);
        }

        // Build graphs based on dependencies
        for (int i = 0; i < n; i++) {
            for (int prev : beforeItems.get(i)) {
                // Add item dependency
                itemGraph.get(prev).add(i);
                itemIndegree.put(i, itemIndegree.get(i) + 1);

                // Add group dependency if items belong to different groups
                if (group[prev] != group[i]) {
                    groupGraph.get(group[prev]).add(group[i]);
                    groupIndegree.put(group[i], groupIndegree.get(group[i]) + 1);
                }
            }
        }

        // Topological sort for groups
        List<Integer> groupOrder = topologicalSort(groupGraph, groupIndegree);
        if (groupOrder.size() != groupId) {
            return new int[0]; // Cycle detected in groups
        }

        // Topological sort for items
        List<Integer> itemOrder = topologicalSort(itemGraph, itemIndegree);
        if (itemOrder.size() != n) {
            return new int[0]; // Cycle detected in items
        }

        // Group items by their group ID
        Map<Integer, List<Integer>> groupToItems = new HashMap<>();
        for (int item : itemOrder) {
            groupToItems.computeIfAbsent(group[item], k -> new ArrayList<>()).add(item);
        }

        // Build final result following group order
        List<Integer> result = new ArrayList<>();
        for (int g : groupOrder) {
            if (groupToItems.containsKey(g)) {
                result.addAll(groupToItems.get(g));
            }
        }

        return result.stream().mapToInt(i -> i).toArray();
    }

    private List<Integer> topologicalSort(Map<Integer, List<Integer>> graph,
            Map<Integer, Integer> indegree) {
        Queue<Integer> queue = new LinkedList<>();
        List<Integer> result = new ArrayList<>();

        // Add all nodes with 0 in-degree to queue
        for (Map.Entry<Integer, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        // Process nodes in topological order
        while (!queue.isEmpty()) {
            int curr = queue.poll();
            result.add(curr);

            // Reduce in-degree of neighbors
            for (int neighbor : graph.get(curr)) {
                indegree.put(neighbor, indegree.get(neighbor) - 1);
                if (indegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        return result;
    }

    // Test method
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int n1 = 8, m1 = 2;
        int[] group1 = { -1, -1, 1, 0, 0, 1, 0, -1 };
        List<List<Integer>> beforeItems1 = Arrays.asList(
                Arrays.asList(), // item 0
                Arrays.asList(6), // item 1
                Arrays.asList(5), // item 2
                Arrays.asList(6), // item 3
                Arrays.asList(3, 6), // item 4
                Arrays.asList(), // item 5
                Arrays.asList(), // item 6
                Arrays.asList() // item 7
        );

        int[] result1 = solution.sortItems(n1, m1, group1, beforeItems1);
        System.out.println("Test 1: " + Arrays.toString(result1));

        // Test case 2 - impossible case
        int n2 = 8, m2 = 2;
        int[] group2 = { -1, -1, 1, 0, 0, 1, 0, -1 };
        List<List<Integer>> beforeItems2 = Arrays.asList(
                Arrays.asList(), // item 0
                Arrays.asList(6), // item 1
                Arrays.asList(5), // item 2
                Arrays.asList(6), // item 3
                Arrays.asList(3), // item 4
                Arrays.asList(), // item 5
                Arrays.asList(4), // item 6 - creates cycle with item 4
                Arrays.asList() // item 7
        );

        int[] result2 = solution.sortItems(n2, m2, group2, beforeItems2);
        System.out.println("Test 2 (should be empty): " + Arrays.toString(result2));
    }
}

/*
 * Alternative solution using Kahn's algorithm with cleaner implementation:
 */
class AlternativeSolution {
    public int[] sortItems(int n, int m, int[] group, List<List<Integer>> beforeItems) {
        // Create unique groups for ungrouped items
        for (int i = 0; i < n; i++) {
            if (group[i] == -1) {
                group[i] = m++;
            }
        }

        // Build dependency graphs
        List<Set<Integer>> itemGraph = new ArrayList<>();
        List<Set<Integer>> groupGraph = new ArrayList<>();
        int[] itemIndegree = new int[n];
        int[] groupIndegree = new int[m];

        // Initialize graphs
        for (int i = 0; i < n; i++) {
            itemGraph.add(new HashSet<>());
        }
        for (int i = 0; i < m; i++) {
            groupGraph.add(new HashSet<>());
        }

        // Build dependencies
        for (int i = 0; i < n; i++) {
            for (int prev : beforeItems.get(i)) {
                // Item dependency
                if (itemGraph.get(prev).add(i)) {
                    itemIndegree[i]++;
                }

                // Group dependency
                int prevGroup = group[prev];
                int currGroup = group[i];
                if (prevGroup != currGroup && groupGraph.get(prevGroup).add(currGroup)) {
                    groupIndegree[currGroup]++;
                }
            }
        }

        // Topological sort
        List<Integer> itemOrder = kahnsAlgorithm(itemGraph, itemIndegree);
        List<Integer> groupOrder = kahnsAlgorithm(groupGraph, groupIndegree);

        if (itemOrder.size() != n || groupOrder.size() != m) {
            return new int[0];
        }

        // Group items by their group
        Map<Integer, List<Integer>> groupItems = new HashMap<>();
        for (int item : itemOrder) {
            groupItems.computeIfAbsent(group[item], k -> new ArrayList<>()).add(item);
        }

        // Build result
        List<Integer> result = new ArrayList<>();
        for (int g : groupOrder) {
            result.addAll(groupItems.getOrDefault(g, new ArrayList<>()));
        }

        return result.stream().mapToInt(i -> i).toArray();
    }

    private List<Integer> kahnsAlgorithm(List<Set<Integer>> graph, int[] indegree) {
        Queue<Integer> queue = new LinkedList<>();
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < indegree.length; i++) {
            if (indegree[i] == 0) {
                queue.offer(i);
            }
        }

        while (!queue.isEmpty()) {
            int curr = queue.poll();
            result.add(curr);

            for (int next : graph.get(curr)) {
                if (--indegree[next] == 0) {
                    queue.offer(next);
                }
            }
        }

        return result;
    }
}

class SortItemsByGroupsRespectingDependencies {

    public int[] sortItems(int n, int m, int[] group, List<List<Integer>> beforeItems) {
        int groupId = m;
        for (int i = 0; i < n; i++) {
            if (group[i] == -1) {
                group[i] = groupId++;
            }
        }

        Map<Integer, Integer> itemIndegree = new HashMap<>();
        Map<Integer, List<Integer>> itemGraph = new HashMap<>();

        Map<Integer, Integer> groupIndegree = new HashMap<>();
        Map<Integer, List<Integer>> groupGraph = new HashMap<>();

        for (int i = 0; i < n; i++) {
            itemGraph.put(i, new ArrayList<>());
            itemIndegree.put(i, 0);
        }
        for (int i = 0; i < groupId; i++) {
            groupGraph.put(i, new ArrayList<>());
            groupIndegree.put(i, 0);
        }

        for (int i = 0; i < n; i++) {
            for (int before : beforeItems.get(i)) {
                itemGraph.get(before).add(i);
                itemIndegree.put(i, itemIndegree.get(i) + 1);

                if (group[i] != group[before]) {
                    groupGraph.get(group[before]).add(group[i]);
                    groupIndegree.put(group[i], groupIndegree.get(group[i]) + 1);
                }
            }
        }

        List<Integer> itemOrder = topoSort(itemGraph, itemIndegree, n);
        if (itemOrder.isEmpty())
            return new int[0];

        List<Integer> groupOrder = topoSort(groupGraph, groupIndegree, groupId);
        if (groupOrder.isEmpty())
            return new int[0];

        Map<Integer, List<Integer>> groupedItems = new HashMap<>();
        for (int i = 0; i < groupId; i++) {
            groupedItems.put(i, new ArrayList<>());
        }
        for (int item : itemOrder) {
            groupedItems.get(group[item]).add(item);
        }

        List<Integer> result = new ArrayList<>();
        for (int g : groupOrder) {
            result.addAll(groupedItems.get(g));
        }

        return result.stream().mapToInt(i -> i).toArray();
    }

    private List<Integer> topoSort(Map<Integer, List<Integer>> graph, Map<Integer, Integer> indegree, int totalNodes) {
        Queue<Integer> queue = new ArrayDeque<>();
        List<Integer> result = new ArrayList<>();
        for (int node : graph.keySet()) {
            if (indegree.get(node) == 0) {
                queue.offer(node);
            }
        }

        while (!queue.isEmpty()) {
            int current = queue.poll();
            result.add(current);
            for (int next : graph.get(current)) {
                indegree.put(next, indegree.get(next) - 1);
                if (indegree.get(next) == 0) {
                    queue.offer(next);
                }
            }
        }
        return result.size() == graph.size() ? result : new ArrayList<>();
    }

}
