import java.util.*;

class Person {
    String name;
    Set<Person> parents = new HashSet<>(); // initialize to avoid nulls

    Person(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Person))
            return false;
        return Objects.equals(name, ((Person) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

class FamilyTreeLCA {

    /*
     * Let:
     * - N = total number of nodes (people) in the family tree
     * - E = total number of parent-child edges (typically ≤ 2 × N)
     * - H1 = number of ancestors for person1
     * - H2 = number of ancestors for person2
     * Each person can have up to 2 parents (realistic in genealogy)
     */
    public Person findCommonAncestor(Person person1, Person person2) {
        if (person1 == null || person2 == null)
            return null;

        // Step 1: Collect all ancestors of person1
        /*
         * Time complexity:
         * Worst-case: O(H1) → visit all ancestors of person1
         * For each person: look up and enqueue parents (constant time per edge)
         * Total: O(H1) time
         * 
         * Space complexity:
         * visited1 and queue1: O(H1)
         * ancestors: O(H1)
         */
        Set<Person> ancestors = new HashSet<>();
        Queue<Person> queue1 = new LinkedList<>();
        Set<Person> visited1 = new HashSet<>();
        queue1.offer(person1);
        visited1.add(person1);

        while (!queue1.isEmpty()) {
            Person current = queue1.poll();
            ancestors.add(current);

            if (current.parents != null) {
                for (Person parent : current.parents) {
                    if (visited1.add(parent)) {
                        queue1.offer(parent);
                    }
                }
            }
        }

        // Step 2: Traverse upward from person2 and find first match
        /*
         * Time complexity:
         * Worst-case: O(H2)
         * We stop early if an ancestor is found in ancestors, so actual cost is often
         * less.
         * Total: O(H2) in worst case
         * 
         * Space complexity:
         * visited2, queue2: O(H2)
         */
        Queue<Person> queue2 = new LinkedList<>();
        Set<Person> visited2 = new HashSet<>();
        queue2.offer(person2);
        visited2.add(person2);

        while (!queue2.isEmpty()) {
            Person current = queue2.poll();
            if (ancestors.contains(current)) {
                return current; // Found lowest common ancestor
            }

            if (current.parents != null) {
                for (Person parent : current.parents) {
                    if (visited2.add(parent)) {
                        queue2.offer(parent);
                    }
                }
            }
        }

        return null; // No common ancestor found
    }
}

class FamilyTreeLCABidirectionalBFS {

    public Person findCommonAncestor(Person person1, Person person2) {
        if (person1 == null || person2 == null)
            return null;
        if (person1.equals(person2))
            return person1;

        Set<Person> visitedFrom1 = new HashSet<>();
        Set<Person> visitedFrom2 = new HashSet<>();
        Queue<Person> queue1 = new LinkedList<>();
        Queue<Person> queue2 = new LinkedList<>();

        queue1.offer(person1);
        visitedFrom1.add(person1);

        queue2.offer(person2);
        visitedFrom2.add(person2);

        while (!queue1.isEmpty() || !queue2.isEmpty()) {
            // Expand from person1 side
            Person found = expand(queue1, visitedFrom1, visitedFrom2);
            if (found != null)
                return found;

            // Expand from person2 side
            found = expand(queue2, visitedFrom2, visitedFrom1);
            if (found != null)
                return found;
        }

        return null; // No common ancestor
    }

    private Person expand(Queue<Person> queue, Set<Person> visitedThisSide, Set<Person> visitedOtherSide) {
        int levelSize = queue.size();

        for (int i = 0; i < levelSize; i++) {
            Person current = queue.poll();

            for (Person parent : current.parents) {
                if (visitedThisSide.contains(parent))
                    continue;

                if (visitedOtherSide.contains(parent)) {
                    return parent; // First common ancestor found
                }

                visitedThisSide.add(parent);
                queue.offer(parent);
            }
        }

        return null;
    }

}
