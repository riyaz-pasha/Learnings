import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class StackTraversal {

    public static void main(String[] args) {
        Stack<Integer> stack = new Stack<>();
        stack.push(1);
        stack.push(2);
        stack.add(3);
        stack.add(4);
        stack.addAll(List.of(5, 6, 7));

        System.out.println("Stack size : " + stack.size());

        indexBasedTraversal(stack);
        iteratorBasedTraversal(stack);
        enhancedForLoopTraversal(stack);
    }

    private static void indexBasedTraversal(Stack<Integer> stack) {
        // Preserves original order: bottom to top.
        System.out.println("Index Based Traversal");
        for (int index = 0; index < stack.size(); index++) {
            System.out.println(stack.get(index));
        }
        System.out.println("=".repeat(50));
    }

    private static void iteratorBasedTraversal(Stack<Integer> stack) {
        // Traverses bottom to top (in insertion order).
        System.out.println("Iterator Based Traversal");
        Iterator<Integer> iterator = stack.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
        System.out.println("=".repeat(50));
    }

    private static void enhancedForLoopTraversal(Stack<Integer> stack) {
        System.out.println("Enhanced for-each loop Based Traversal");
        for (int item : stack) {
            System.out.println(item);
        }
        System.out.println("=".repeat(50));
    }

}

// In modern Java, use ArrayDeque instead of Stack
