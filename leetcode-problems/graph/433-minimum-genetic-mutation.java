import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

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
