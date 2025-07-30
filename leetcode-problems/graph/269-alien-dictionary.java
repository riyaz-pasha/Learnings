/*
 * You’re given a list of words sorted in an unknown "alien" lexicographical
 * order. You need to derive a valid ordering of the alien alphabet, or return
 * "" if the input is inconsistent (e.g. has cycles or ordering conflicts).
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class AlienDictionary {

    /**
     * ------------------------
     * BFS Topological Sort (Kahn's Algorithm)
     * ------------------------
     * Time Complexity: O(C + E)
     * - C: Number of unique characters
     * - E: Total number of edges (precedence rules)
     * Space Complexity: O(C + E)
     */
    public String alienOrderBFS(String[] words) {
        Map<Character, Set<Character>> graph = new HashMap<>();
        Map<Character, Integer> indegree = new HashMap<>();

        // Initialize graph and indegree with all unique characters
        for (String word : words) {
            for (char c : word.toCharArray()) {
                graph.putIfAbsent(c, new HashSet<>());
                indegree.putIfAbsent(c, 0);
            }
        }

        // Build graph from adjacent word pairs
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i], w2 = words[i + 1];

            // Invalid input: prefix conflict
            if (w1.length() > w2.length() && w1.startsWith(w2))
                return "";

            for (int j = 0; j < Math.min(w1.length(), w2.length()); j++) {
                char from = w1.charAt(j), to = w2.charAt(j);
                if (from != to) {
                    if (!graph.get(from).contains(to)) {
                        graph.get(from).add(to);
                        indegree.put(to, indegree.get(to) + 1);
                    }
                    break; // Only first different character matters
                }
            }
        }

        // Kahn's algorithm: queue for 0 indegree nodes
        Queue<Character> queue = new LinkedList<>();
        for (char c : indegree.keySet()) {
            if (indegree.get(c) == 0)
                queue.offer(c);
        }

        StringBuilder sb = new StringBuilder();
        while (!queue.isEmpty()) {
            char curr = queue.poll();
            sb.append(curr);

            for (char next : graph.get(curr)) {
                indegree.put(next, indegree.get(next) - 1);
                if (indegree.get(next) == 0)
                    queue.offer(next);
            }
        }

        return sb.length() == indegree.size() ? sb.toString() : "";
    }

    /**
     * ------------------------
     * DFS Topological Sort with Cycle Detection
     * ------------------------
     * Time Complexity: O(C + E)
     * Space Complexity: O(C + E)
     */
    public String alienOrderDFS(String[] words) {
        Map<Character, Set<Character>> graph = new HashMap<>();
        Set<Character> allChars = new HashSet<>();

        // Initialize graph
        for (String word : words) {
            for (char c : word.toCharArray()) {
                allChars.add(c);
                graph.putIfAbsent(c, new HashSet<>());
            }
        }

        // Build graph
        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i], w2 = words[i + 1];
            if (w1.length() > w2.length() && w1.startsWith(w2))
                return "";

            for (int j = 0; j < Math.min(w1.length(), w2.length()); j++) {
                char from = w1.charAt(j), to = w2.charAt(j);
                if (from != to) {
                    graph.get(from).add(to);
                    break;
                }
            }
        }

        Map<Character, Integer> visit = new HashMap<>(); // 0 = unvisited, 1 = visiting, 2 = visited
        StringBuilder sb = new StringBuilder();

        for (char c : allChars) {
            if (!visit.containsKey(c) && hasCycleDFS(c, graph, visit, sb)) {
                return ""; // cycle detected
            }
        }

        return sb.reverse().toString();
    }

    // DFS helper to detect cycle and build ordering
    private boolean hasCycleDFS(char c, Map<Character, Set<Character>> graph,
            Map<Character, Integer> visit, StringBuilder sb) {
        visit.put(c, 1); // visiting

        for (char neighbor : graph.getOrDefault(c, new HashSet<>())) {
            if (!visit.containsKey(neighbor)) {
                if (hasCycleDFS(neighbor, graph, visit, sb))
                    return true;
            } else if (visit.get(neighbor) == 1) {
                return true; // cycle detected
            }
        }

        visit.put(c, 2); // visited
        sb.append(c); // post-order append
        return false;
    }

}
