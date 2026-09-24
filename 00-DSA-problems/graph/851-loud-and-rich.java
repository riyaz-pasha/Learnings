import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
/*
 * There is a group of n people labeled from 0 to n - 1 where each person has a
 * different amount of money and a different level of quietness.
 * 
 * You are given an array richer where richer[i] = [ai, bi] indicates that ai
 * has more money than bi and an integer array quiet where quiet[i] is the
 * quietness of the ith person. All the given data in richer are logically
 * correct (i.e., the data will not lead you to a situation where x is richer
 * than y and y is richer than x at the same time).
 * 
 * Return an integer array answer where answer[x] = y if y is the least quiet
 * person (that is, the person y with the smallest value of quiet[y]) among all
 * people who definitely have equal to or more money than the person x.
 * 
 * Example 1:
 * 
 * Input: richer = [[1,0],[2,1],[3,1],[3,7],[4,3],[5,3],[6,3]], quiet =
 * [3,2,5,4,6,1,7,0]
 * Output: [5,5,2,5,4,5,6,7]
 * Explanation:
 * answer[0] = 5.
 * Person 5 has more money than 3, which has more money than 1, which has more
 * money than 0.
 * The only person who is quieter (has lower quiet[x]) is person 7, but it is
 * not clear if they have more money than person 0.
 * answer[7] = 7.
 * Among all people that definitely have equal to or more money than person 7
 * (which could be persons 3, 4, 5, 6, or 7), the person who is the quietest
 * (has lower quiet[x]) is person 7.
 * The other answers can be filled out with similar reasoning.
 * 
 * Example 2:
 * Input: richer = [], quiet = [0]
 * Output: [0]
 */



class LoudestAndQuietest {
    
    /**
     * Solution 1: DFS with Memoization
     * Time Complexity: O(n + m) where n = number of people, m = number of richer relationships
     * Space Complexity: O(n + m) for adjacency list and recursion stack
     */
    public int[] loudAndQuiet1(int[][] richer, int[] quiet) {
        int n = quiet.length;
        
        // Build adjacency list: person -> list of people poorer than them
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        
        for (int[] edge : richer) {
            graph.get(edge[0]).add(edge[1]); // richer person points to poorer person
        }
        
        int[] answer = new int[n];
        Arrays.fill(answer, -1); // -1 indicates not computed yet
        
        for (int i = 0; i < n; i++) {
            dfs(i, graph, quiet, answer);
        }
        
        return answer;
    }
    
    private int dfs(int person, List<List<Integer>> graph, int[] quiet, int[] answer) {
        if (answer[person] != -1) {
            return answer[person]; // Already computed
        }
        
        // Initially, the quietest person is the person themselves
        answer[person] = person;
        
        // Check all people poorer than current person
        for (int poorer : graph.get(person)) {
            int candidate = dfs(poorer, graph, quiet, answer);
            if (quiet[candidate] < quiet[answer[person]]) {
                answer[person] = candidate;
            }
        }
        
        return answer[person];
    }
    
    /**
     * Solution 2: Topological Sort (Kahn's Algorithm)
     * Time Complexity: O(n + m) where n = number of people, m = number of richer relationships
     * Space Complexity: O(n + m) for adjacency list and data structures
     */
    public int[] loudAndQuiet2(int[][] richer, int[] quiet) {
        int n = quiet.length;
        
        // Build reverse graph: poorer person -> list of richer people
        List<List<Integer>> reverseGraph = new ArrayList<>();
        int[] indegree = new int[n]; // number of people richer than person i
        
        for (int i = 0; i < n; i++) {
            reverseGraph.add(new ArrayList<>());
        }
        
        for (int[] edge : richer) {
            reverseGraph.get(edge[1]).add(edge[0]); // poorer -> richer
            indegree[edge[0]]++; // richer person has one more person poorer than them
        }
        
        int[] answer = new int[n];
        for (int i = 0; i < n; i++) {
            answer[i] = i; // Initially, each person is the quietest among themselves
        }
        
        // Start with people who have no one richer than them
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (indegree[i] == 0) {
                queue.offer(i);
            }
        }
        
        while (!queue.isEmpty()) {
            int current = queue.poll();
            
            // Update all people poorer than current
            for (int poorer : reverseGraph.get(current)) {
                // If current person is quieter, update the answer for poorer person
                if (quiet[answer[current]] < quiet[answer[poorer]]) {
                    answer[poorer] = answer[current];
                }
                
                indegree[poorer]--;
                if (indegree[poorer] == 0) {
                    queue.offer(poorer);
                }
            }
        }
        
        return answer;
    }
    
    /**
     * Solution 3: DFS without Memoization (Less Efficient)
     * Time Complexity: O(n * (n + m)) - potentially exponential in worst case
     * Space Complexity: O(n + m) for adjacency list and recursion stack
     */
    public int[] loudAndQuiet3(int[][] richer, int[] quiet) {
        int n = quiet.length;
        
        // Build adjacency list: person -> list of people poorer than them
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }
        
        for (int[] edge : richer) {
            graph.get(edge[0]).add(edge[1]);
        }
        
        int[] answer = new int[n];
        
        for (int i = 0; i < n; i++) {
            answer[i] = findQuietest(i, graph, quiet);
        }
        
        return answer;
    }
    
    private int findQuietest(int person, List<List<Integer>> graph, int[] quiet) {
        int quietest = person;
        
        // Check all people poorer than current person
        for (int poorer : graph.get(person)) {
            int candidate = findQuietest(poorer, graph, quiet);
            if (quiet[candidate] < quiet[quietest]) {
                quietest = candidate;
            }
        }
        
        return quietest;
    }
    
    // Test methods
    public static void main(String[] args) {
        LoudestAndQuietest solution = new LoudestAndQuietest();
        
        // Test Case 1
        int[][] richer1 = {{1,0},{2,1},{3,1},{3,7},{4,3},{5,3},{6,3}};
        int[] quiet1 = {3,2,5,4,6,1,7,0};
        
        System.out.println("Test Case 1:");
        System.out.println("Expected: [5,5,2,5,4,5,6,7]");
        System.out.println("Solution 1: " + Arrays.toString(solution.loudAndQuiet1(richer1, quiet1)));
        System.out.println("Solution 2: " + Arrays.toString(solution.loudAndQuiet2(richer1, quiet1)));
        System.out.println("Solution 3: " + Arrays.toString(solution.loudAndQuiet3(richer1, quiet1)));
        
        // Test Case 2
        int[][] richer2 = {};
        int[] quiet2 = {0};
        
        System.out.println("\nTest Case 2:");
        System.out.println("Expected: [0]");
        System.out.println("Solution 1: " + Arrays.toString(solution.loudAndQuiet1(richer2, quiet2)));
        System.out.println("Solution 2: " + Arrays.toString(solution.loudAndQuiet2(richer2, quiet2)));
        System.out.println("Solution 3: " + Arrays.toString(solution.loudAndQuiet3(richer2, quiet2)));
    }

}

class Solution {
    
    public int[] loudAndRich(int[][] richer, int[] quiet) {
        int n = quiet.length;
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) graph.add(new ArrayList<>());

        // Build the graph: if a is richer than b, b -> a
        for (int[] r : richer) {
            graph.get(r[1]).add(r[0]);
        }

        int[] answer = new int[n];
        Arrays.fill(answer, -1); // -1 means not yet computed

        for (int i = 0; i < n; i++) {
            dfs(i, graph, quiet, answer);
        }

        return answer;
    }

    private int dfs(int person, List<List<Integer>> graph, int[] quiet, int[] answer) {
        if (answer[person] != -1) return answer[person];

        int minQuietPerson = person;
        for (int richerPerson : graph.get(person)) {
            int candidate = dfs(richerPerson, graph, quiet, answer);
            if (quiet[candidate] < quiet[minQuietPerson]) {
                minQuietPerson = candidate;
            }
        }

        answer[person] = minQuietPerson;
        return minQuietPerson;
    }

}


/*
COMPLEXITY ANALYSIS:

Solution 1 - DFS with Memoization (RECOMMENDED):
- Time Complexity: O(n + m)
  * Each person is visited at most once due to memoization
  * Each edge is traversed at most once
  * n = number of people, m = number of richer relationships
- Space Complexity: O(n + m)
  * O(n + m) for adjacency list
  * O(n) for memoization array
  * O(n) for recursion stack in worst case

Solution 2 - Topological Sort:
- Time Complexity: O(n + m)
  * Each person is processed exactly once
  * Each edge is processed exactly once
- Space Complexity: O(n + m)
  * O(n + m) for reverse adjacency list
  * O(n) for indegree array and queue

Solution 3 - DFS without Memoization:
- Time Complexity: O(n * (n + m)) in average case, potentially exponential in worst case
  * For each person, we might traverse the entire subgraph
  * Without memoization, same subproblems are solved multiple times
- Space Complexity: O(n + m)
  * O(n + m) for adjacency list
  * O(n) for recursion stack

RECOMMENDATIONS:
1. Use Solution 1 (DFS with Memoization) - most intuitive and efficient
2. Use Solution 2 (Topological Sort) - good alternative, especially if you're comfortable with topological sorting
3. Avoid Solution 3 - inefficient due to repeated computations

KEY INSIGHTS:
- The problem is essentially finding the minimum in a DAG rooted at each node
- Memoization is crucial for efficiency
- Both DFS and topological sort approaches work well
- The graph represents wealth hierarchy, not direct relationships
*/
