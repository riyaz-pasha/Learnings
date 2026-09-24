import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class AlienDictionary {

    public String alienOrder(String[] words) {
        Map<Character, Set<Character>> graph = new HashMap<>();
        Map<Character, Integer> indegree = new HashMap<>();

        for (String word : words) {
            for (char ch : word.toCharArray()) {
                graph.putIfAbsent(ch, new HashSet<>());
                indegree.putIfAbsent(ch, 0);
            }
        }

        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i], word2 = words[i + 1];
            if (word1.length() > word2.length() && word1.startsWith(word2)) {
                return "";
            }

            for (int j = 0; j < Math.min(word1.length(), word2.length()); j++) {
                char from = word1.charAt(j), to = word2.charAt(j);
                if (from == to)
                    continue;
                if (!graph.get(from).contains(to)) {
                    graph.get(from).add(to);
                    indegree.put(to, indegree.get(to) + 1);
                }
                break;
            }
        }

        Queue<Character> queue = new LinkedList<>();
        for (Map.Entry<Character, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        StringBuilder sb = new StringBuilder();
        while (!queue.isEmpty()) {
            char ch = queue.poll();
            sb.append(ch);

            for (char neighbor : graph.get(ch)) {
                indegree.put(neighbor, indegree.get(neighbor) - 1);
                if (indegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        return sb.length() == indegree.size() ? sb.toString() : "";
    }

}

class AlienDictionary2 {

    public String alienOrder(String[] words) {
        // Step 1: Initialize graph and in-degree map
        Map<Character, Set<Character>> graph = new HashMap<>();
        Map<Character, Integer> indegree = new HashMap<>();

        for (String word : words) {
            for (char ch : word.toCharArray()) {
                graph.putIfAbsent(ch, new HashSet<>());
                indegree.putIfAbsent(ch, 0);
            }
        }

        // Step 2: Build the graph
        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i + 1];

            // Check invalid case: word1 is longer and word2 is its prefix
            if (word1.length() > word2.length() && word1.startsWith(word2)) {
                return "";
            }

            // Find the first differing character
            int minLen = Math.min(word1.length(), word2.length());
            for (int j = 0; j < minLen; j++) {
                char parent = word1.charAt(j);
                char child = word2.charAt(j);

                if (parent != child) {
                    // Add edge only if it doesn't exist (avoid double-counting in-degree)
                    if (graph.get(parent).add(child)) {
                        indegree.put(child, indegree.get(child) + 1);
                    }
                    break; // Only the first difference matters
                }
            }
        }

        // Step 3: Topological sort using BFS (Kahn's algorithm)
        Queue<Character> queue = new LinkedList<>();
        for (Map.Entry<Character, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        StringBuilder result = new StringBuilder();
        while (!queue.isEmpty()) {
            char current = queue.poll();
            result.append(current);

            // Reduce in-degree for neighbors
            for (char neighbor : graph.get(current)) {
                indegree.put(neighbor, indegree.get(neighbor) - 1);
                if (indegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // Step 4: Check if valid topological order exists
        return result.length() == indegree.size() ? result.toString() : "";
    }
}