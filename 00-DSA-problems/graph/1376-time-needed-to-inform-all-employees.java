import java.util.*;

// Solution 1: DFS - Build Tree then Traverse (Most Intuitive)
// Time Complexity: O(N) - each employee visited once
// Space Complexity: O(N) - for adjacency list and recursion stack
class TimeNeededToInformAllEmployees {
    /*
     * APPROACH: Build adjacency list then DFS to find maximum depth
     * 
     * Key Insights:
     * 1. This is a tree rooted at headID
     * 2. Information flows from manager to subordinates (top-down)
     * 3. We need maximum time to reach any leaf (deepest path)
     * 4. Each manager takes informTime[i] to inform ALL direct subordinates
     * 5. Subordinates can start informing simultaneously after manager finishes
     * 
     * Algorithm:
     * 1. Build adjacency list: manager -> list of subordinates
     * 2. DFS from headID to find maximum path sum
     * 3. At each node, add informTime[node] + max(time to inform all subtrees)
     */

    public int numOfMinutes(int n, int headID, int[] manager, int[] informTime) {
        // Build adjacency list: manager -> subordinates
        List<List<Integer>> subordinates = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            subordinates.add(new ArrayList<>());
        }

        // Build the tree structure
        for (int i = 0; i < n; i++) {
            if (manager[i] != -1) { // Skip head (manager[headID] = -1)
                subordinates.get(manager[i]).add(i);
            }
        }

        // DFS to find maximum time from head to any leaf
        return dfs(headID, subordinates, informTime);
    }

    private int dfs(int manager, List<List<Integer>> subordinates, int[] informTime) {
        /*
         * Returns the maximum time needed to inform all employees in this subtree
         * 
         * Logic:
         * 1. If no subordinates, return 0 (leaf node)
         * 2. Otherwise, manager takes informTime[manager] to inform direct subordinates
         * 3. Then all subordinates start informing simultaneously
         * 4. Total time = informTime[manager] + max(time for each subordinate subtree)
         */

        int maxSubtreeTime = 0;

        // Find maximum time among all subordinate subtrees
        for (int subordinate : subordinates.get(manager)) {
            maxSubtreeTime = Math.max(maxSubtreeTime, dfs(subordinate, subordinates, informTime));
        }

        // Current manager's inform time + max time for subordinates
        return informTime[manager] + maxSubtreeTime;
    }

}

// Solution 2: DFS without Building Adjacency List (Space Optimized)
// Time Complexity: O(N²) - for each manager, scan all employees
// Space Complexity: O(N) - only recursion stack
class TimeNeededToInformAllEmployees2 {
    /*
     * APPROACH: Direct DFS using manager array
     * 
     * Instead of building adjacency list, we traverse the manager array
     * to find subordinates for each manager during DFS.
     * Saves space but has higher time complexity.
     */

    public int numOfMinutes(int n, int headID, int[] manager, int[] informTime) {
        return dfs(headID, manager, informTime);
    }

    private int dfs(int managerId, int[] manager, int[] informTime) {
        int maxTime = 0;

        // Find all subordinates of current manager
        for (int i = 0; i < manager.length; i++) {
            if (manager[i] == managerId) { // i is subordinate of managerId
                maxTime = Math.max(maxTime, dfs(i, manager, informTime));
            }
        }

        return informTime[managerId] + maxTime;
    }

}

// Solution 3: BFS Level-by-Level (Alternative Approach)
// Time Complexity: O(N)
// Space Complexity: O(N) - for adjacency list and queue
class TimeNeededToInformAllEmployees3 {
    /*
     * APPROACH: BFS to simulate the information spreading process
     * 
     * Simulates the actual process:
     * 1. Start with head at time 0
     * 2. Each manager informs subordinates after their inform time
     * 3. Track when each employee gets informed
     * 4. Return maximum time across all employees
     */

    public int numOfMinutes(int n, int headID, int[] manager, int[] informTime) {
        // Build adjacency list
        List<List<Integer>> subordinates = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            subordinates.add(new ArrayList<>());
        }

        for (int i = 0; i < n; i++) {
            if (manager[i] != -1) {
                subordinates.get(manager[i]).add(i);
            }
        }

        // BFS with queue storing [employee, timeWhenInformed]
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { headID, 0 });

        int maxTime = 0;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int employee = current[0];
            int currentTime = current[1];

            maxTime = Math.max(maxTime, currentTime);

            // This employee will inform subordinates after informTime[employee]
            int timeToInformSubordinates = currentTime + informTime[employee];

            for (int subordinate : subordinates.get(employee)) {
                queue.offer(new int[] { subordinate, timeToInformSubordinates });
            }
        }

        return maxTime;
    }

}

// Solution 4: Memoized DFS (For optimization if needed)
class TimeNeededToInformAllEmployees4 {
    /*
     * APPROACH: DFS with memoization
     * 
     * Useful if there are overlapping subproblems (though unlikely in tree
     * structure)
     * Demonstrates how to add memoization to DFS approach.
     */

    public int numOfMinutes(int n, int headID, int[] manager, int[] informTime) {
        List<List<Integer>> subordinates = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            subordinates.add(new ArrayList<>());
        }

        for (int i = 0; i < n; i++) {
            if (manager[i] != -1) {
                subordinates.get(manager[i]).add(i);
            }
        }

        int[] memo = new int[n];
        Arrays.fill(memo, -1);

        return dfs(headID, subordinates, informTime, memo);
    }

    private int dfs(int manager, List<List<Integer>> subordinates, int[] informTime, int[] memo) {
        if (memo[manager] != -1) {
            return memo[manager];
        }

        int maxSubtreeTime = 0;
        for (int subordinate : subordinates.get(manager)) {
            maxSubtreeTime = Math.max(maxSubtreeTime, dfs(subordinate, subordinates, informTime, memo));
        }

        memo[manager] = informTime[manager] + maxSubtreeTime;
        return memo[manager];
    }

}

// Test class with examples
class TestInformEmployees {

    public static void main(String[] args) {
        TimeNeededToInformAllEmployees solution = new TimeNeededToInformAllEmployees();

        // Test case 1: n=1, headID=0, manager=[-1], informTime=[0]
        // Expected: 0 (only head, no one to inform)
        int[] manager1 = { -1 };
        int[] informTime1 = { 0 };
        System.out.println("Test 1: " + solution.numOfMinutes(1, 0, manager1, informTime1)); // 0

        // Test case 2: n=6, headID=2, manager=[2,2,-1,2,2,2], informTime=[0,0,1,0,0,0]
        // Expected: 1 (head informs all direct subordinates in 1 minute)
        int[] manager2 = { 2, 2, -1, 2, 2, 2 };
        int[] informTime2 = { 0, 0, 1, 0, 0, 0 };
        System.out.println("Test 2: " + solution.numOfMinutes(6, 2, manager2, informTime2)); // 1

        // Test case 3: n=7, headID=6, manager=[1,2,3,4,5,6,-1],
        // informTime=[0,6,5,4,3,2,1]
        // Expected: 21 (6->5->4->3->2->1->0: 1+2+3+4+5+6 = 21)
        int[] manager3 = { 1, 2, 3, 4, 5, 6, -1 };
        int[] informTime3 = { 0, 6, 5, 4, 3, 2, 1 };
        System.out.println("Test 3: " + solution.numOfMinutes(7, 6, manager3, informTime3)); // 21

        // Test case 4: n=15, headID=0, manager=[-1,0,0,1,1,2,2,3,3,4,4,5,5,6,6]
        // informTime=[1,1,1,1,1,1,1,0,0,0,0,0,0,0,0]
        // Expected: 3 (binary tree structure, depth 3)
        int[] manager4 = { -1, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6 };
        int[] informTime4 = { 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 };
        System.out.println("Test 4: " + solution.numOfMinutes(15, 0, manager4, informTime4)); // 3
    }
}

class Solution {

    public int numOfMinutes(int n, int headID, int[] manager, int[] informTime) {
        // Use array of ArrayList for faster access
        ArrayList<Integer>[] tree = new ArrayList[n];
        for (int i = 0; i < n; i++)
            tree[i] = new ArrayList<>();

        // Build the management tree
        for (int i = 0; i < n; i++) {
            if (manager[i] != -1) {
                tree[manager[i]].add(i);
            }
        }

        // Start DFS from head
        return dfs(headID, tree, informTime);
    }

    private int dfs(int managerID, ArrayList<Integer>[] tree, int[] informTime) {
        if (tree[managerID].isEmpty()) {
            return 0; // No subordinates
        }

        int maxTime = 0;
        for (int subordinate : tree[managerID]) {
            maxTime = Math.max(maxTime, dfs(subordinate, tree, informTime));
        }
        return informTime[managerID] + maxTime;
    }

}
