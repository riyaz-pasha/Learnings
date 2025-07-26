import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class QueueTraversal {

    public static void main(String[] args) {
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(1);
        queue.offer(2);
        queue.add(3);
        queue.add(4);
        queue.addAll(List.of(5, 6, 7));
        queue.addAll(Set.of(8, 9));

        iteratorBasedTraversal(queue);

    }

    private static void indexBasedTraversal(Queue<Integer> queue) {
        // Unlike List, queues don’t support .get(index) or index-based loops because
        // they don’t guarantee positional access.
    }

    private static void iteratorBasedTraversal(Queue<Integer> queue) {
        // Traverses front to back (FIFO)
        System.out.println("Iterator Based Traversal");
        Iterator<Integer> iterator = queue.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
        System.out.println("=".repeat(50));
    }

    private static void enhancedForLoopTraversal(Queue<Integer> queue) {
        // Traverses front to back (FIFO)
        System.out.println("Enhanced for-each loop Based Traversal");
        for (int item : queue) {
            System.out.println(item);
        }
        System.out.println("=".repeat(50));
    }

}
