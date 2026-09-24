import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * SORT ITEMS BY GROUPS RESPECTING DEPENDENCIES (LeetCode 1203)
 * ============================================================================
 * 
 * STATEMENT:
 * You have 'n' items (0 to n-1) and 'm' groups. The array 'group' specifies 
 * which group an item belongs to. If group[i] == -1, the item belongs to no 
 * existing group and should form its own unique group.
 * You are also given 'beforeItems' where beforeItems[i] lists all items that 
 * MUST come before item 'i'.
 * Return an ordering of items such that:
 * 1. All dependencies are respected (item A comes before item B if required).
 * 2. Group continuity is maintained (all items in the same group appear consecutively).
 * If impossible, return an empty array.
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * Imagine you are scheduling tasks for different projects (groups). 
 * Tasks belonging to the same project must be executed back-to-back (no interleaving 
 * projects). On top of that, some tasks depend on the completion of other tasks, 
 * which might be in the same project or a completely different one.
 * To solve this, we need to sort the projects (groups) themselves into a valid 
 * sequence, and independently sort the tasks (items) within each project.
 * This translates directly to solving TWO Topological Sorts simultaneously:
 * one for the groups, and one for the items.
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER (4-10):
 * 1. Q: How should we treat items with group[i] == -1?
 *    A: They behave entirely as their own isolated group of size 1.
 * 2. Q: Can there be duplicate dependencies in beforeItems?
 *    A: The problem constraints state beforeItems[i] contains no duplicates.
 * 3. Q: Is it possible for a group to be split into two chunks?
 *    A: No, that violates the group continuity rule. If dependencies force a split, 
 *       we must return an empty array.
 * 4. Q: What if a circular dependency exists between items?
 *    A: Return an empty array.
 * 5. Q: What if a circular dependency exists between groups?
 *    A: Return an empty array.
 * 6. Q: Can there be multiple valid arrangements?
 *    A: Yes, any valid sorting that respects all constraints is acceptable.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Acknowledge the Complexity: State clearly that the continuity constraint 
 *    means we cannot just perform a single topological sort on the items. 
 * 2. State the Strategy: "We will perform a Two-Level Topological Sort. First, 
 *    we isolate the '-1' groups by assigning them unique group IDs. Then, we 
 *    build two directed graphs: an Item Graph and a Group Graph."
 * 3. Detail the graphs:
 *    - Item Graph: Tracks standard item-to-item dependencies.
 *    - Group Graph: If Item A (Group 1) must precede Item B (Group 2), then 
 *      Group 1 must precede Group 2.
 * 4. Cycle Detection: Use Kahn's algorithm (BFS) for both. If either sort fails 
 *    to process all nodes, a cycle exists -> return empty.
 * 5. Assembly: Group the topologically sorted items by their group ID. Then, 
 *    iterate through the topologically sorted groups, appending their items to 
 *    the final result array.
 * 
 * ----------------------------------------------------------------------------
 * TIME & SPACE COMPLEXITY ANALYSIS:
 * - Time: O(V + E) where V is the number of items and E is the total number of 
 *   dependencies. We traverse items and edges a constant number of times.
 * - Space: O(V + E) to store the Item Graph, Group Graph, and in-degree arrays.
 * 
 * ============================================================================
 */
public class SortItemsByGroups {

    /**
     * ========================================================================
     * SOLUTION: DOUBLE TOPOLOGICAL SORT (KAHN'S ALGORITHM)
     * ========================================================================
     * 
     * VISUAL TRACING & BREAKDOWN:
     * n = 5, m = 2
     * group = [0, -1, 1, 0, 1]
     * beforeItems = [[], [0], [], [1], [2]]
     * 
     * Step 1: Assign unique groups to '-1'.
     * New groups start at m (which is 2).
     * Item 1 gets group 2.
     * New group array: [0, 2, 1, 0, 1]. Total groups = 3.
     * 
     * Step 2: Build Graphs
     * Dependencies:
     * Item 0 -> Item 1  | Group 0 -> Group 2
     * Item 1 -> Item 3  | Group 2 -> Group 0
     * Item 2 -> Item 4  | Group 1 -> Group 1 (Intra-group, ignore for Group Graph)
     * 
     * Look closely at the Group Graph: 
     * Group 0 -> Group 2 AND Group 2 -> Group 0.
     * THIS IS A CYCLE! Group 0 must finish before Group 2, but Group 2 must 
     * finish before Group 0. 
     * 
     * Because of this cycle, our Group Topo-Sort will fail, correctly causing us 
     * to return an empty array.
     */
    public int[] sortItems(int n, int m, int[] group, List<List<Integer>> beforeItems) {
        // Step 1: Re-assign all items with group == -1 to unique group IDs
        int groupId = m;
        for (int i = 0; i < n; i++) {
            if (group[i] == -1) {
                group[i] = groupId++;
            }
        }
        int numGroups = groupId;

        // Step 2: Initialize Graphs and In-Degree arrays
        // Item Graph uses standard lists.
        List<List<Integer>> itemGraph = new ArrayList<>();
        for (int i = 0; i < n; i++) itemGraph.add(new ArrayList<>());
        int[] itemInDegree = new int[n];

        // Group Graph uses Sets to prevent duplicate edges inflating in-degrees.
        List<Set<Integer>> groupGraph = new ArrayList<>();
        for (int i = 0; i < numGroups; i++) groupGraph.add(new HashSet<>());
        int[] groupInDegree = new int[numGroups];

        // Step 3: Populate Graphs
        for (int currItem = 0; currItem < n; currItem++) {
            for (int prevItem : beforeItems.get(currItem)) {
                // Item Dependency
                itemGraph.get(prevItem).add(currItem);
                itemInDegree[currItem]++;

                // Group Dependency (only if they belong to different groups)
                int prevGroup = group[prevItem];
                int currGroup = group[currItem];
                
                if (prevGroup != currGroup) {
                    // Only increment in-degree if this is a newly discovered relationship
                    if (groupGraph.get(prevGroup).add(currGroup)) {
                        groupInDegree[currGroup]++;
                    }
                }
            }
        }

        // Step 4: Perform Topological Sort on both Items and Groups
        List<Integer> itemOrder = topologicalSort(itemGraph, itemInDegree, n);
        List<Integer> groupOrder = topologicalSort(groupGraph, groupInDegree, numGroups);

        // If either topological sort fails (returns a list smaller than total nodes), 
        // a cycle exists. It is impossible to schedule.
        if (itemOrder.size() < n || groupOrder.size() < numGroups) {
            return new int[0];
        }

        // Step 5: Map Topologically Sorted Items to their respective Groups
        // Using a Map where the value is a List maintains the topological insertion order!
        Map<Integer, List<Integer>> groupedItems = new HashMap<>();
        for (int item : itemOrder) {
            int gId = group[item];
            groupedItems.putIfAbsent(gId, new ArrayList<>());
            groupedItems.get(gId).add(item);
        }

        // Step 6: Assemble final result based on the Group Topological Order
        int[] result = new int[n];
        int index = 0;
        
        for (int gId : groupOrder) {
            // Not every group might have items if it was just an empty placeholder
            if (groupedItems.containsKey(gId)) {
                for (int item : groupedItems.get(gId)) {
                    result[index++] = item;
                }
            }
        }

        return result;
    }

    /**
     * Helper method to perform Kahn's Algorithm for Topological Sorting.
     * Works with both List<List<Integer>> and List<Set<Integer>> due to wildcard bounds.
     * 
     * @param graph The adjacency list representation of the directed graph.
     * @param inDegree The array tracking the number of prerequisites for each node.
     * @param numNodes The total number of nodes in the graph.
     * @return A List containing the topological order. If a cycle exists, the list size will be < numNodes.
     */
    private List<Integer> topologicalSort(List<? extends Collection<Integer>> graph, int[] inDegree, int numNodes) {
        Queue<Integer> queue = new LinkedList<>();
        
        // Add all nodes with 0 in-degree (no prerequisites) to queue
        for (int i = 0; i < numNodes; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        List<Integer> sortedOrder = new ArrayList<>();
        
        while (!queue.isEmpty()) {
            int current = queue.poll();
            sortedOrder.add(current);
            
            // Resolve dependencies for neighbors
            for (int neighbor : graph.get(current)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }
        
        return sortedOrder;
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING (Using modern Java Records & Streams)
     * ========================================================================
     */
    
    // Record to hold structured test case data
    record TestCase(int n, int m, int[] group, List<List<Integer>> beforeItems, boolean expectValid, String description) {}

    public static void main(String[] args) {
        SortItemsByGroups solver = new SortItemsByGroups();

        var testCases = List.of(
            new TestCase(
                8, 
                2, 
                new int[]{-1, -1, 1, 0, 0, 1, 0, -1}, 
                List.of(
                    List.of(),       // 0
                    List.of(6),      // 1
                    List.of(5),      // 2
                    List.of(6),      // 3
                    List.of(3, 6),   // 4
                    List.of(),       // 5
                    List.of(),       // 6
                    List.of()        // 7
                ),
                true, 
                "Standard Valid Group Sorting"
            ),
            new TestCase(
                8, 
                2, 
                new int[]{-1, -1, 1, 0, 0, 1, 0, -1}, 
                List.of(
                    List.of(),       // 0
                    List.of(6),      // 1
                    List.of(5),      // 2
                    List.of(6),      // 3
                    List.of(3, 6),   // 4
                    List.of(),       // 5
                    List.of(4),      // 6 (Cycle introduced here: 4 depends on 6, 6 depends on 4)
                    List.of()        // 7
                ),
                false, 
                "Cyclic Dependency within Items"
            ),
            new TestCase(
                5, 
                2, 
                new int[]{0, -1, 1, 0, 1}, 
                List.of(
                    List.of(),       // 0
                    List.of(0),      // 1
                    List.of(),       // 2
                    List.of(1),      // 3
                    List.of(2)       // 4
                ),
                false, 
                "Cyclic Dependency between Groups (0 -> -1 -> 0)"
            )
        );

        System.out.println("Running test cases for Sort Items by Groups Respecting Dependencies...");
        System.out.println("-".repeat(90));

        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            
            int[] result = solver.sortItems(tc.n(), tc.m(), tc.group(), tc.beforeItems());
            
            boolean isValid = result.length > 0;
            boolean passed = isValid == tc.expectValid();
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("Expected Valid? %b | Got Valid? %b\n", tc.expectValid(), isValid);
            System.out.printf("Status: %s\n", passed ? "✅ PASSED" : "❌ FAILED");
            
            if (isValid) {
                System.out.println("Result Order: " + Arrays.toString(result));
            } else {
                System.out.println("Result Order: [] (Empty, due to cycle or impossibility)");
            }
            System.out.println("-".repeat(90));
        });
    }
}
