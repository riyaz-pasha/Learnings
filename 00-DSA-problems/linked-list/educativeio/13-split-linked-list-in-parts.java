import java.util.ArrayList;
import java.util.List;

/**
 * PROBLEM DESCRIPTION:
 * You are given the head of a singly linked list and an integer, k. 
 * Your task is to split the linked list into k consecutive parts.
 * 
 * - Each part should have a size as equal as possible, with the difference 
 *   between any two parts being at most 1.
 * - If the list cannot be evenly divided, the earlier parts should have 
 *   more nodes than the later ones.
 * - Any parts that cannot be filled with nodes should be represented as NULL.
 * - The parts must appear in the same order as in the input-linked list.
 * 
 * Return an array of the k parts, maintaining the specified conditions.
 * 
 * CONSTRAINTS:
 * - The number of nodes in the list is in the range [0, 1000].
 * - 0 <= Node.val <= 1000
 * - 1 <= k <= 50
 * 
 * EXAMPLES:
 * Example 1:
 * Input: head = [1,2,3], k = 5
 * Output: [[1],[2],[3],[],[]]
 * Visual:
 * Total length = 3. We need 5 parts.
 * Base size = 3 / 5 = 0. Extra nodes = 3 % 5 = 3.
 * The first 3 parts get 1 node (0 + 1). The last 2 parts get 0 nodes.
 * Parts: [1], [2], [3], null, null
 * 
 * Example 2:
 * Input: head = [1,2,3,4,5,6,7,8,9,10], k = 3
 * Output: [[1,2,3,4],[5,6,7],[8,9,10]]
 * Visual:
 * Total length = 10. We need 3 parts.
 * Base size = 10 / 3 = 3. Extra nodes = 10 % 3 = 1.
 * The first part gets 4 nodes (3 + 1). The next 2 parts get 3 nodes.
 * Parts: [1, 2, 3, 4], [5, 6, 7], [8, 9, 10]
 */

class ListNode {
    int val;
    ListNode next;

    ListNode(int val) {
        this.val = val;
    }
}

public class SplitLinkedListInParts {

    /**
     * APPROACH 1: Optimal One-Pass Sizing & Splitting
     * Time Complexity: O(N + k), where N is the number of nodes in the list.
     * Space Complexity: O(k) for the output array (excluding auxiliary space).
     * 
     * EXPLANATION:
     * 1. Traverse the linked list once to find its total length `N`.
     * 2. Determine the sizes of the `k` parts:
     *    - Minimum size of each part (base size) = `N / k`
     *    - Number of parts that get an extra node = `N % k`
     * 3. Iterate `k` times to build the `k` parts.
     * 4. For each part, compute its size: `baseSize + (extraNodes > 0 ? 1 : 0)`.
     * 5. Traverse `size - 1` nodes to find the tail of the current part.
     * 6. Disconnect the tail from the rest of the list (i.e., `tail.next = null`) 
     *    and store the head of the current part into the result array.
     */
    public ListNode[] splitListToPartsOptimal(ListNode head, int k) {
        ListNode[] parts = new ListNode[k];
        
        // Step 1: Count total nodes
        int length = 0;
        ListNode curr = head;
        while (curr != null) {
            length++;
            curr = curr.next;
        }

        // Step 2: Calculate math for distribution
        int baseSize = length / k;
        int extraNodes = length % k;

        curr = head;
        // Step 3: Extract the k parts
        for (int i = 0; i < k; i++) {
            parts[i] = curr;
            
            // Calculate size of the current part
            int currentPartSize = baseSize + (extraNodes > 0 ? 1 : 0);
            extraNodes--; // One extra node consumed (if it was > 0)

            // Step 4: Traverse to the tail of the current part
            // (currentPartSize - 1 because curr is already at the first node)
            for (int j = 0; j < currentPartSize - 1 && curr != null; j++) {
                curr = curr.next;
            }

            // Step 5: Sever the link to isolate the part
            if (curr != null) {
                ListNode nextPartHead = curr.next;
                curr.next = null; // Break the list
                curr = nextPartHead; // Move to the start of the next part
            }
        }

        return parts;
    }


    /**
     * APPROACH 2: Using Extra Memory (Array of Node References)
     * Time Complexity: O(N + k)
     * Space Complexity: O(N + k) to store node references in an ArrayList.
     * 
     * EXPLANATION:
     * 1. Traverse the list and store references to every node in an ArrayList.
     * 2. This grants O(1) random access to any node.
     * 3. We calculate `baseSize` and `extraNodes` similarly.
     * 4. Instead of traversing pointers sequentially to find the tail, we simply 
     *    jump to the correct index in our ArrayList, sever the `.next` pointer, 
     *    and load the chunk into our result array.
     * 
     * NOTE: While easier to conceptually index, this approach violates strict 
     * optimal space requirements because it allocates O(N) auxiliary space.
     */
    public ListNode[] splitListToPartsUsingArray(ListNode head, int k) {
        ListNode[] parts = new ListNode[k];
        List<ListNode> nodes = new ArrayList<>();
        
        // Step 1: Store all nodes
        ListNode curr = head;
        while (curr != null) {
            nodes.add(curr);
            curr = curr.next;
        }

        int n = nodes.size();
        int baseSize = n / k;
        int extraNodes = n % k;
        
        int index = 0;
        
        // Step 2: Slice nodes by index
        for (int i = 0; i < k; i++) {
            if (index < n) {
                parts[i] = nodes.get(index);
                
                int currentPartSize = baseSize + (extraNodes > 0 ? 1 : 0);
                extraNodes--;
                
                // The tail node for this part
                int tailIndex = index + currentPartSize - 1;
                
                // Break the link
                nodes.get(tailIndex).next = null;
                
                // Move index forward for the next iteration
                index += currentPartSize;
            } else {
                // Not enough nodes to fill this part
                parts[i] = null;
            }
        }

        return parts;
    }


    /**
     * UTILITY: Create List from Array
     */
    public static ListNode createList(int[] arr) {
        if (arr == null || arr.length == 0) return null;
        ListNode head = new ListNode(arr[0]);
        ListNode curr = head;
        for (int i = 1; i < arr.length; i++) {
            curr.next = new ListNode(arr[i]);
            curr = curr.next;
        }
        return head;
    }

    /**
     * UTILITY: Print Array of Linked Lists
     */
    public static void printListArray(ListNode[] parts) {
        System.out.print("[");
        for (int i = 0; i < parts.length; i++) {
            System.out.print("[");
            ListNode curr = parts[i];
            while (curr != null) {
                System.out.print(curr.val + (curr.next != null ? ", " : ""));
                curr = curr.next;
            }
            System.out.print("]" + (i < parts.length - 1 ? ", " : ""));
        }
        System.out.println("]");
    }


    /**
     * MAIN METHOD: Demonstrating the approaches with examples.
     */
    public static void main(String[] args) {
        SplitLinkedListInParts solver = new SplitLinkedListInParts();

        // Test Case 1: Elements < k (Requires Empty Parts)
        ListNode head1 = createList(new int[]{1, 2, 3});
        System.out.println("Original List 1: [1 -> 2 -> 3], k = 5");
        ListNode[] result1 = solver.splitListToPartsOptimal(head1, 5);
        System.out.print("Optimal Output:  ");
        printListArray(result1);
        System.out.println("-------------------------------------------------");

        // Test Case 2: Elements > k (Uneven distribution)
        ListNode head2 = createList(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        System.out.println("Original List 2: [1 to 10], k = 3");
        ListNode[] result2 = solver.splitListToPartsUsingArray(head2, 3);
        System.out.print("Array Output:    ");
        printListArray(result2);
        System.out.println("-------------------------------------------------");
        
        // Test Case 3: Elements perfectly divisible by k
        ListNode head3 = createList(new int[]{1, 2, 3, 4, 5, 6});
        System.out.println("Original List 3: [1 to 6], k = 2");
        ListNode[] result3 = solver.splitListToPartsOptimal(head3, 2);
        System.out.print("Optimal Output:  ");
        printListArray(result3);
        System.out.println("-------------------------------------------------");

        // Test Case 4: Edge Case - Empty List
        ListNode head4 = createList(new int[]{});
        System.out.println("Original List 4: [], k = 3");
        ListNode[] result4 = solver.splitListToPartsOptimal(head4, 3);
        System.out.print("Optimal Output:  ");
        printListArray(result4);
    }
}
