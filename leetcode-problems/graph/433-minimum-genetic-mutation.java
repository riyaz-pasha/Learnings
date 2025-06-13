import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/*
 * A gene string can be represented by an 8-character long string, with choices
 * from 'A', 'C', 'G', and 'T'.
 * 
 * Suppose we need to investigate a mutation from a gene string startGene to a
 * gene string endGene where one mutation is defined as one single character
 * changed in the gene string.
 * 
 * For example, "AACCGGTT" --> "AACCGGTA" is one mutation.
 * There is also a gene bank bank that records all the valid gene mutations. A
 * gene must be in bank to make it a valid gene string.
 * 
 * Given the two gene strings startGene and endGene and the gene bank bank,
 * return the minimum number of mutations needed to mutate from startGene to
 * endGene. If there is no such a mutation, return -1.
 * 
 * Note that the starting point is assumed to be valid, so it might not be
 * included in the bank.
 * 
 * 
 * 
 * Example 1:
 * 
 * Input: startGene = "AACCGGTT", endGene = "AACCGGTA", bank = ["AACCGGTA"]
 * Output: 1
 * Example 2:
 * 
 * Input: startGene = "AACCGGTT", endGene = "AAACGGTA", bank =
 * ["AACCGGTA","AACCGCTA","AAACGGTA"]
 * Output: 2
 */

class MinimumGeneticMutation {

    public int minMutation(String startGene, String endGene, String[] bank) {
        Set<String> geneBank = new HashSet<>(Arrays.asList(bank));
        if (!geneBank.contains(endGene))
            return -1;

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.offer(startGene);
        visited.add(endGene);

        int mutations = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                String current = queue.poll();
                if (current.equals(endGene))
                    return mutations;
                for (String gene : geneBank) {
                    if (!visited.contains(gene) && differByOne(current, gene)) {
                        queue.offer(gene);
                        visited.add(gene);
                    }
                }
            }
            mutations++;
        }
        return -1;
    }

    private boolean differByOne(String gene1, String gene2) {
        int diff = 0;
        for (int i = 0; i < gene2.length(); i++) {
            if (gene1.charAt(i) != gene2.charAt(i)) {
                diff++;
                if (diff > 1)
                    return false;
            }
        }
        return diff == 1;
    }

}

class Solution {

    /**
     * BFS Solution - Optimal approach for shortest path
     * Time: O(N * M * 4 * M) where N = bank size, M = gene length (8)
     * Space: O(N) for queue and visited set
     */
    public int minMutation(String startGene, String endGene, String[] bank) {
        // Convert bank to set for O(1) lookup
        Set<String> bankSet = new HashSet<>(Arrays.asList(bank));

        // If endGene is not in bank, mutation is impossible
        if (!bankSet.contains(endGene)) {
            return -1;
        }

        // If start and end are the same
        if (startGene.equals(endGene)) {
            return 0;
        }

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        char[] genes = { 'A', 'C', 'G', 'T' };

        queue.offer(startGene);
        visited.add(startGene);

        int mutations = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            mutations++;

            for (int i = 0; i < size; i++) {
                String currentGene = queue.poll();

                // Try mutating each position
                for (int pos = 0; pos < currentGene.length(); pos++) {
                    char originalChar = currentGene.charAt(pos);

                    // Try each possible gene character
                    for (char newChar : genes) {
                        if (newChar == originalChar)
                            continue; // Skip same character

                        // Create mutated gene
                        String mutatedGene = currentGene.substring(0, pos) +
                                newChar +
                                currentGene.substring(pos + 1);

                        // Check if this is our target
                        if (mutatedGene.equals(endGene)) {
                            return mutations;
                        }

                        // If valid mutation and not visited, add to queue
                        if (bankSet.contains(mutatedGene) && !visited.contains(mutatedGene)) {
                            visited.add(mutatedGene);
                            queue.offer(mutatedGene);
                        }
                    }
                }
            }
        }

        return -1; // No valid mutation path found
    }

    /**
     * BFS with StringBuilder (slightly more efficient string building)
     */
    public int minMutationOptimized(String startGene, String endGene, String[] bank) {
        Set<String> bankSet = new HashSet<>(Arrays.asList(bank));

        if (!bankSet.contains(endGene))
            return -1;
        if (startGene.equals(endGene))
            return 0;

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        char[] genes = { 'A', 'C', 'G', 'T' };

        queue.offer(startGene);
        visited.add(startGene);

        int mutations = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            mutations++;

            for (int i = 0; i < size; i++) {
                String current = queue.poll();
                char[] currentArray = current.toCharArray();

                for (int pos = 0; pos < currentArray.length; pos++) {
                    char original = currentArray[pos];

                    for (char gene : genes) {
                        if (gene == original)
                            continue;

                        currentArray[pos] = gene;
                        String mutated = new String(currentArray);

                        if (mutated.equals(endGene)) {
                            return mutations;
                        }

                        if (bankSet.contains(mutated) && !visited.contains(mutated)) {
                            visited.add(mutated);
                            queue.offer(mutated);
                        }
                    }

                    currentArray[pos] = original; // Restore original character
                }
            }
        }

        return -1;
    }

    /**
     * Bidirectional BFS - More efficient for large search spaces
     * Time: O(sqrt(N) * M * 4 * M), Space: O(N)
     */
    public int minMutationBidirectional(String startGene, String endGene, String[] bank) {
        Set<String> bankSet = new HashSet<>(Arrays.asList(bank));

        if (!bankSet.contains(endGene))
            return -1;
        if (startGene.equals(endGene))
            return 0;

        Set<String> beginSet = new HashSet<>();
        Set<String> endSet = new HashSet<>();
        Set<String> visited = new HashSet<>();
        char[] genes = { 'A', 'C', 'G', 'T' };

        beginSet.add(startGene);
        endSet.add(endGene);

        int mutations = 0;

        while (!beginSet.isEmpty() && !endSet.isEmpty()) {
            mutations++;

            // Always expand the smaller set for efficiency
            if (beginSet.size() > endSet.size()) {
                Set<String> temp = beginSet;
                beginSet = endSet;
                endSet = temp;
            }

            Set<String> nextLevel = new HashSet<>();

            for (String gene : beginSet) {
                char[] geneArray = gene.toCharArray();

                for (int i = 0; i < geneArray.length; i++) {
                    char original = geneArray[i];

                    for (char c : genes) {
                        if (c == original)
                            continue;

                        geneArray[i] = c;
                        String mutated = new String(geneArray);

                        if (endSet.contains(mutated)) {
                            return mutations;
                        }

                        if (bankSet.contains(mutated) && !visited.contains(mutated)) {
                            visited.add(mutated);
                            nextLevel.add(mutated);
                        }
                    }

                    geneArray[i] = original;
                }
            }

            beginSet = nextLevel;
        }

        return -1;
    }

    /**
     * DFS Solution (for comparison - not optimal for shortest path)
     * Explores all possible paths
     */
    public int minMutationDFS(String startGene, String endGene, String[] bank) {
        Set<String> bankSet = new HashSet<>(Arrays.asList(bank));

        if (!bankSet.contains(endGene))
            return -1;
        if (startGene.equals(endGene))
            return 0;

        Set<String> visited = new HashSet<>();
        int[] minMutations = { Integer.MAX_VALUE };

        dfs(startGene, endGene, bankSet, visited, 0, minMutations);

        return minMutations[0] == Integer.MAX_VALUE ? -1 : minMutations[0];
    }

    private void dfs(String current, String target, Set<String> bank,
            Set<String> visited, int mutations, int[] minMutations) {
        if (current.equals(target)) {
            minMutations[0] = Math.min(minMutations[0], mutations);
            return;
        }

        if (mutations >= minMutations[0])
            return; // Pruning

        char[] genes = { 'A', 'C', 'G', 'T' };
        char[] currentArray = current.toCharArray();

        for (int i = 0; i < currentArray.length; i++) {
            char original = currentArray[i];

            for (char gene : genes) {
                if (gene == original)
                    continue;

                currentArray[i] = gene;
                String mutated = new String(currentArray);

                if (bank.contains(mutated) && !visited.contains(mutated)) {
                    visited.add(mutated);
                    dfs(mutated, target, bank, visited, mutations + 1, minMutations);
                    visited.remove(mutated); // Backtrack
                }
            }

            currentArray[i] = original;
        }
    }

    /**
     * A* Search Algorithm (advanced optimization)
     * Uses heuristic function (Hamming distance) to guide search
     */
    public int minMutationAStar(String startGene, String endGene, String[] bank) {
        Set<String> bankSet = new HashSet<>(Arrays.asList(bank));

        if (!bankSet.contains(endGene))
            return -1;
        if (startGene.equals(endGene))
            return 0;

        // Priority queue with f(n) = g(n) + h(n)
        // g(n) = actual distance, h(n) = heuristic (Hamming distance)
        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> Integer.compare(a.fScore, b.fScore));

        Set<String> visited = new HashSet<>();
        char[] genes = { 'A', 'C', 'G', 'T' };

        pq.offer(new Node(startGene, 0, hammingDistance(startGene, endGene)));

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            if (current.gene.equals(endGene)) {
                return current.gScore;
            }

            if (visited.contains(current.gene))
                continue;
            visited.add(current.gene);

            char[] geneArray = current.gene.toCharArray();

            for (int i = 0; i < geneArray.length; i++) {
                char original = geneArray[i];

                for (char gene : genes) {
                    if (gene == original)
                        continue;

                    geneArray[i] = gene;
                    String mutated = new String(geneArray);

                    if (bankSet.contains(mutated) && !visited.contains(mutated)) {
                        int gScore = current.gScore + 1;
                        int hScore = hammingDistance(mutated, endGene);
                        pq.offer(new Node(mutated, gScore, gScore + hScore));
                    }
                }

                geneArray[i] = original;
            }
        }

        return -1;
    }

    private int hammingDistance(String s1, String s2) {
        int distance = 0;
        for (int i = 0; i < s1.length(); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

    static class Node {
        String gene;
        int gScore; // Actual distance from start
        int fScore; // gScore + heuristic

        Node(String gene, int gScore, int fScore) {
            this.gene = gene;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    // Test cases
    public static void main(String[] args) {
        Solution sol = new Solution();

        // Test case 1
        String start1 = "AACCGGTT";
        String end1 = "AACCGGTA";
        String[] bank1 = { "AACCGGTA" };
        System.out.println("Test 1: " + sol.minMutation(start1, end1, bank1)); // Expected: 1

        // Test case 2
        String start2 = "AACCGGTT";
        String end2 = "AAACGGTA";
        String[] bank2 = { "AACCGGTA", "AACCGCTA", "AAACGGTA" };
        System.out.println("Test 2: " + sol.minMutation(start2, end2, bank2)); // Expected: 2

        // Test case 3 - No solution
        String start3 = "AAAAACCC";
        String end3 = "AACCCCCC";
        String[] bank3 = { "AAAACCCC", "AAACCCCC", "AACCCCCC" };
        System.out.println("Test 3: " + sol.minMutation(start3, end3, bank3)); // Expected: 3

        // Test case 4 - Same start and end
        String start4 = "AACCGGTT";
        String end4 = "AACCGGTT";
        String[] bank4 = { "AACCGGTA" };
        System.out.println("Test 4: " + sol.minMutation(start4, end4, bank4)); // Expected: 0

        // Test different implementations
        System.out.println("Test 1 (Optimized): " + sol.minMutationOptimized(start1, end1, bank1));
        System.out.println("Test 2 (Bidirectional): " + sol.minMutationBidirectional(start2, end2, bank2));
        System.out.println("Test 2 (A*): " + sol.minMutationAStar(start2, end2, bank2));
    }
}
