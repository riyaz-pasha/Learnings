import java.util.HashMap;
import java.util.Map;

/*
 * A linked list of length n is given such that each node contains an additional
 * random pointer, which could point to any node in the list, or null.
 * 
 * Construct a deep copy of the list. The deep copy should consist of exactly n
 * brand new nodes, where each new node has its value set to the value of its
 * corresponding original node. Both the next and random pointer of the new
 * nodes should point to new nodes in the copied list such that the pointers in
 * the original list and copied list represent the same list state. None of the
 * pointers in the new list should point to nodes in the original list.
 * 
 * For example, if there are two nodes X and Y in the original list, where
 * X.random --> Y, then for the corresponding two nodes x and y in the copied
 * list, x.random --> y.
 * 
 * Return the head of the copied linked list.
 * 
 * The linked list is represented in the input/output as a list of n nodes. Each
 * node is represented as a pair of [val, random_index] where:
 * 
 * val: an integer representing Node.val
 * random_index: the index of the node (range from 0 to n-1) that the random
 * pointer points to, or null if it does not point to any node.
 * Your code will only be given the head of the original linked list.
 * 
 * Example 1:
 * Input: head = [[7,null],[13,0],[11,4],[10,2],[1,0]]
 * Output: [[7,null],[13,0],[11,4],[10,2],[1,0]]
 * 
 * Example 2:
 * Input: head = [[1,1],[2,1]]
 * Output: [[1,1],[2,1]]
 * 
 * Example 3:
 * Input: head = [[3,null],[3,0],[3,null]]
 * Output: [[3,null],[3,0],[3,null]]
 */

// Definition for a Node with random pointer
class Node {
    int val;
    Node next;
    Node random;

    public Node(int val) {
        this.val = val;
        this.next = null;
        this.random = null;
    }
}

class Solution {

    // Solution 1: HashMap approach (Most intuitive)
    // Time Complexity: O(n), Space Complexity: O(n)
    public Node copyRandomList(Node head) {
        if (head == null)
            return null;

        Map<Node, Node> nodeMap = new HashMap<>();

        // First pass: Create all new nodes and store mapping
        Node current = head;
        while (current != null) {
            nodeMap.put(current, new Node(current.val));
            current = current.next;
        }

        // Second pass: Set next and random pointers
        current = head;
        while (current != null) {
            Node newNode = nodeMap.get(current);
            newNode.next = nodeMap.get(current.next);
            newNode.random = nodeMap.get(current.random);
            current = current.next;
        }

        return nodeMap.get(head);
    }

    // Solution 2: Interweaving approach (Most elegant - O(1) space)
    // Time Complexity: O(n), Space Complexity: O(1)
    public Node copyRandomListInterweave(Node head) {
        if (head == null)
            return null;

        // Step 1: Create new nodes and interweave them
        Node current = head;
        while (current != null) {
            Node newNode = new Node(current.val);
            newNode.next = current.next;
            current.next = newNode;
            current = newNode.next;
        }

        // Step 2: Set random pointers for new nodes
        current = head;
        while (current != null) {
            if (current.random != null) {
                current.next.random = current.random.next;
            }
            current = current.next.next;
        }

        // Step 3: Separate the two lists
        Node dummy = new Node(0);
        Node newCurrent = dummy;
        current = head;

        while (current != null) {
            newCurrent.next = current.next;
            current.next = current.next.next;

            current = current.next;
            newCurrent = newCurrent.next;
        }

        return dummy.next;
    }

    // Solution 3: Recursive approach with memoization
    // Time Complexity: O(n), Space Complexity: O(n)
    private Map<Node, Node> visited = new HashMap<>();

    public Node copyRandomListRecursive(Node head) {
        if (head == null)
            return null;

        // If we have already processed the current node, return the cloned version
        if (visited.containsKey(head)) {
            return visited.get(head);
        }

        // Create a new node with the same value
        Node node = new Node(head.val);

        // Save this value in the hash map. This is needed since there might be
        // loops during traversal due to randomness of random pointers and this would
        // help us avoid them.
        visited.put(head, node);

        // Recursively copy the remaining linked list starting once from the next
        // pointer
        // and then from the random pointer.
        node.next = copyRandomListRecursive(head.next);
        node.random = copyRandomListRecursive(head.random);

        return node;
    }

    // Solution 4: Two-pass approach with cleaner separation
    public Node copyRandomListTwoPass(Node head) {
        if (head == null)
            return null;

        Map<Node, Node> oldToNew = new HashMap<>();

        // First pass: create nodes and build next pointers
        Node current = head;
        Node newHead = null;
        Node newCurrent = null;

        while (current != null) {
            Node newNode = new Node(current.val);
            oldToNew.put(current, newNode);

            if (newHead == null) {
                newHead = newNode;
                newCurrent = newNode;
            } else {
                newCurrent.next = newNode;
                newCurrent = newNode;
            }

            current = current.next;
        }

        // Second pass: set random pointers
        current = head;
        newCurrent = newHead;

        while (current != null) {
            if (current.random != null) {
                newCurrent.random = oldToNew.get(current.random);
            }
            current = current.next;
            newCurrent = newCurrent.next;
        }

        return newHead;
    }
}

// Test class for demonstration and validation
class CopyRandomListTest {

    // Helper method to create a test list
    public static Node createTestList() {
        // Create nodes: [[7,null],[13,0],[11,4],[10,2],[1,0]]
        Node node1 = new Node(7);
        Node node2 = new Node(13);
        Node node3 = new Node(11);
        Node node4 = new Node(10);
        Node node5 = new Node(1);

        // Set next pointers
        node1.next = node2;
        node2.next = node3;
        node3.next = node4;
        node4.next = node5;

        // Set random pointers
        node1.random = null; // index null
        node2.random = node1; // index 0
        node3.random = node5; // index 4
        node4.random = node3; // index 2
        node5.random = node1; // index 0

        return node1;
    }

    // Helper method to create simple test list
    public static Node createSimpleTestList() {
        // Create nodes: [[1,1],[2,1]]
        Node node1 = new Node(1);
        Node node2 = new Node(2);

        node1.next = node2;
        node1.random = node2; // index 1
        node2.random = node2; // index 1 (self-reference)

        return node1;
    }

    // Helper method to print list structure
    public static void printList(Node head, String title) {
        System.out.println(title);
        Node current = head;
        int index = 0;
        Map<Node, Integer> nodeToIndex = new HashMap<>();

        // First pass: assign indices
        while (current != null) {
            nodeToIndex.put(current, index++);
            current = current.next;
        }

        // Second pass: print with random indices
        current = head;
        System.out.print("[");
        boolean first = true;
        while (current != null) {
            if (!first)
                System.out.print(",");

            Integer randomIndex = current.random != null ? nodeToIndex.get(current.random) : null;
            System.out.print("[" + current.val + "," + randomIndex + "]");

            current = current.next;
            first = false;
        }
        System.out.println("]");
    }

    // Validate that the copy is a deep copy
    public static boolean validateDeepCopy(Node original, Node copy) {
        if (original == null && copy == null)
            return true;
        if (original == null || copy == null)
            return false;

        Map<Node, Node> originalToCopy = new HashMap<>();
        Node origCurrent = original;
        Node copyCurrent = copy;

        // Build mapping and check values
        while (origCurrent != null && copyCurrent != null) {
            if (origCurrent.val != copyCurrent.val)
                return false;
            if (origCurrent == copyCurrent)
                return false; // Should be different objects

            originalToCopy.put(origCurrent, copyCurrent);
            origCurrent = origCurrent.next;
            copyCurrent = copyCurrent.next;
        }

        if (origCurrent != null || copyCurrent != null)
            return false;

        // Check random pointers
        origCurrent = original;
        copyCurrent = copy;

        while (origCurrent != null) {
            if (origCurrent.random == null) {
                if (copyCurrent.random != null)
                    return false;
            } else {
                if (copyCurrent.random != originalToCopy.get(origCurrent.random)) {
                    return false;
                }
            }
            origCurrent = origCurrent.next;
            copyCurrent = copyCurrent.next;
        }

        return true;
    }

    public static void main(String[] args) {
        MiddleOfTheLinkedListSolution solution = new MiddleOfTheLinkedListSolution();

        // Test Case 1: Complex list [[7,null],[13,0],[11,4],[10,2],[1,0]]
        System.out.println("=== Test Case 1 ===");
        Node original1 = createTestList();
        printList(original1, "Original:");

        Node copy1 = solution.copyRandomList(original1);
        printList(copy1, "HashMap Copy:");
        System.out.println("Is valid deep copy: " + validateDeepCopy(original1, copy1));

        Node copy1_interweave = solution.copyRandomListInterweave(createTestList());
        printList(copy1_interweave, "Interweave Copy:");
        System.out.println("Is valid deep copy: " + validateDeepCopy(createTestList(), copy1_interweave));

        // Test Case 2: Simple list [[1,1],[2,1]]
        System.out.println("\n=== Test Case 2 ===");
        Node original2 = createSimpleTestList();
        printList(original2, "Original:");

        Node copy2 = solution.copyRandomList(original2);
        printList(copy2, "Copy:");
        System.out.println("Is valid deep copy: " + validateDeepCopy(original2, copy2));

        // Test Case 3: Empty list
        System.out.println("\n=== Test Case 3 ===");
        Node copy3 = solution.copyRandomList(null);
        System.out.println("Empty list copy: " + (copy3 == null ? "null" : "not null"));

        // Test Case 4: Single node
        System.out.println("\n=== Test Case 4 ===");
        Node single = new Node(42);
        single.random = single; // Self-reference
        printList(single, "Single node original:");

        Node copySingle = solution.copyRandomList(single);
        printList(copySingle, "Single node copy:");
        System.out.println("Is valid deep copy: " + validateDeepCopy(single, copySingle));
        System.out.println("Self-reference preserved: " + (copySingle.random == copySingle));
    }

}
