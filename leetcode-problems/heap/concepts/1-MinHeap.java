import java.util.ArrayList;
import java.util.List;

class MinHeap {

    private List<Integer> heap;

    public MinHeap() {
        this.heap = new ArrayList<>();
    }

    public MinHeap(int[] array) {
        this.heap = new ArrayList<>();
        for (int value : array) {
            heap.add(value);
        }
        buildHeap();
    }

    // Get the minimum element (root)
    public int peek() {
        if (isEmpty()) {
            throw new RuntimeException("Heap is empty");
        }
        return heap.get(0);
    }

    // Remove and return the minimum element
    public int poll() {
        if (isEmpty()) {
            throw new RuntimeException("Heap is empty");
        }

        int min = heap.get(0);
        int lastElement = heap.get(heap.size() - 1);
        heap.set(0, lastElement);
        heap.remove(heap.size() - 1);

        if (!isEmpty()) {
            heapifyDown(0);
        }

        return min;
    }

    // Insert a new element
    public void insert(int value) {
        heap.add(value);
        heapifyUp(heap.size() - 1);
    }

    // Remove a specific value (first occurrence)
    public boolean remove(int value) {
        int index = heap.indexOf(value);
        if (index == -1) {
            return false;
        }

        int lastElement = heap.get(heap.size() - 1);
        heap.set(index, lastElement);
        heap.remove(heap.size() - 1);

        if (index < heap.size()) {
            int parent = getParentIndex(index);
            if (parent >= 0 && heap.get(index) < heap.get(parent)) {
                heapifyUp(index);
            } else {
                heapifyDown(index);
            }
        }

        return true;
    }

    // Build heap from existing array (heapify)
    private void buildHeap() {
        for (int i = getParentIndex(heap.size() - 1); i >= 0; i--) {
            heapifyDown(i);
        }
    }

    // Maintain heap property by moving element up
    private void heapifyUp(int index) {
        while (index > 0) {
            int parentIndex = getParentIndex(index);
            if (heap.get(index) >= heap.get(parentIndex)) {
                break;
            }
            swap(index, parentIndex);
            index = parentIndex;
        }
    }

    // Maintain heap property by moving element down
    private void heapifyDown(int index) {
        while (hasLeftChild(index)) {
            int smallerChildIndex = getLeftChildIndex(index);

            if (hasRightChild(index) &&
                    heap.get(getRightChildIndex(index)) < heap.get(smallerChildIndex)) {
                smallerChildIndex = getRightChildIndex(index);
            }

            if (heap.get(index) <= heap.get(smallerChildIndex)) {
                break;
            }

            swap(index, smallerChildIndex);
            index = smallerChildIndex;
        }
    }

    // Helper methods for navigation
    private int getLeftChildIndex(int parentIndex) {
        return 2 * parentIndex + 1;
    }

    private int getRightChildIndex(int parentIndex) {
        return 2 * parentIndex + 2;
    }

    private int getParentIndex(int childIndex) {
        return (childIndex - 1) / 2;
    }

    private boolean hasLeftChild(int index) {
        return getLeftChildIndex(index) < heap.size();
    }

    private boolean hasRightChild(int index) {
        return getRightChildIndex(index) < heap.size();
    }

    // Utility methods
    private void swap(int index1, int index2) {
        int temp = heap.get(index1);
        heap.set(index1, heap.get(index2));
        heap.set(index2, temp);
    }

    public boolean isEmpty() {
        return heap.size() == 0;
    }

    public int size() {
        return heap.size();
    }

    public void printHeap() {
        System.out.println("Heap: " + heap);
    }

    // Convert heap to sorted array (heap sort)
    public int[] heapSort() {
        int[] result = new int[heap.size()];
        MinHeap tempHeap = new MinHeap();

        // Copy all elements to temporary heap
        for (int value : heap) {
            tempHeap.insert(value);
        }

        // Extract elements in sorted order
        for (int i = 0; i < result.length; i++) {
            result[i] = tempHeap.poll();
        }

        return result;
    }

    // Main method for testing
    public static void main(String[] args) {
        System.out.println("=== Min Heap Implementation Demo ===\n");

        // Test 1: Basic operations
        System.out.println("1. Creating empty heap and inserting elements:");
        MinHeap heap = new MinHeap();
        int[] values = { 15, 10, 20, 8, 25, 5, 12 };

        for (int value : values) {
            heap.insert(value);
            System.out.println("Inserted " + value + ": ");
            heap.printHeap();
        }

        System.out.println("\nMinimum element: " + heap.peek());
        System.out.println("Heap size: " + heap.size());

        // Test 2: Polling elements
        System.out.println("\n2. Polling elements:");
        while (!heap.isEmpty()) {
            int min = heap.poll();
            System.out.println("Polled: " + min);
            if (!heap.isEmpty()) {
                heap.printHeap();
            }
        }

        // Test 3: Build heap from array
        System.out.println("\n3. Building heap from array [30, 20, 10, 7, 9, 15, 3]:");
        int[] array = { 30, 20, 10, 7, 9, 15, 3 };
        MinHeap heap2 = new MinHeap(array);
        heap2.printHeap();

        // Test 4: Remove specific element
        System.out.println("\n4. Removing element 9:");
        heap2.remove(9);
        heap2.printHeap();

        // Test 5: Heap sort
        System.out.println("\n5. Heap sort result:");
        int[] sorted = heap2.heapSort();
        System.out.print("Sorted array: ");
        for (int value : sorted) {
            System.out.print(value + " ");
        }
        System.out.println();
    }

}

// Time Complexities:
// Insert: O(log n)
// Poll: O(log n)
// Peek: O(1)
// Remove: O(log n)
// Build heap: O(n)

/*
    ============================
    📌 Min Heap – Theory of Operations
    ============================

    🔸 Structure:
    - A Min Heap is a complete binary tree where every parent node is <= its children.
    - Implemented using an array/list where:
      - Left child index = 2 * i + 1
      - Right child index = 2 * i + 2
      - Parent index = (i - 1) / 2

    ============================
    🔹 1. Insert (add a new element)
    ============================
    - Step 1: Add the element at the end of the heap (maintains complete tree property).
    - Step 2: Perform "heapify up" (also called bubble up or sift up):
        - Compare the inserted element with its parent.
        - If it's smaller than the parent, swap them.
        - Repeat until the element is in the correct position or reaches the root.
    - Time Complexity: O(log n) due to tree height.

    ============================
    🔹 2. Get Min (peek)
    ============================
    - Simply return the root element (at index 0).
    - Since the root is always the smallest element, no traversal is needed.
    - Time Complexity: O(1)

    ============================
    🔹 3. Extract Min (remove the root)
    ============================
    - Step 1: Remove the root (smallest element).
    - Step 2: Replace the root with the last element in the heap (preserves complete tree).
    - Step 3: Perform "heapify down" (also called bubble down or sift down):
        - Compare the new root with its children.
        - Swap it with the smaller child if it violates the heap property.
        - Repeat until it's smaller than both children or reaches a leaf.
    - Time Complexity: O(log n) due to tree height.

    ============================
    🔹 4. Heapify Up (after insertion)
    ============================
    - Used to restore heap property after inserting an element.
    - Compare the current node with its parent.
    - If the current node is smaller, swap them.
    - Repeat until the node is in the correct position or becomes the root.
    - Moves up the tree.

    ============================
    🔹 5. Heapify Down (after removing root)
    ============================
    - Used to restore heap property after extracting the minimum.
    - Compare the current node with its children.
    - Swap it with the smaller child if the heap property is violated.
    - Repeat until it reaches a correct position (i.e., smaller than both children).
    - Moves down the tree.

    ============================
    🔹 6. Size / IsEmpty
    ============================
    - Size: Number of elements in the heap (can be tracked with a variable).
    - IsEmpty: Returns true if the heap contains no elements.
    - Time Complexity: O(1)

    ============================
    🔹 7. Space Complexity
    ============================
    - Space complexity is O(n), where n is the number of elements in the heap.
    - All elements are stored in a single array or list.

    ============================
    🔹 Summary of Time Complexities
    ============================
    - Insert:        O(log n)
    - Extract Min:   O(log n)
    - Get Min:       O(1)
    - Heapify Up/Down: O(log n)
    - Space:         O(n)

*/

/*
    ============================================
    🔸 Remove Specific Element from a Heap
    ============================================

    Goal:
    - Remove an arbitrary element (not just the root) from the heap while maintaining:
        1. Complete Binary Tree structure
        2. Heap Property (Min or Max)

    ============================================
    🔹 Steps to Remove an Element (Value `x`)
    ============================================

    1. 🔍 Find the index of the element to be removed:
       - Search the array to find the index `i` where heap[i] == x.
       - Time Complexity: O(n) — because heaps are not optimized for searching arbitrary elements.

    2. 🔁 Swap the element at index `i` with the **last element** in the heap:
       - This ensures the complete binary tree structure is preserved after removal.

    3. ❌ Remove the last element (which is now the element to be deleted):
       - Simply remove the last element from the array/list (O(1)).

    4. 🛠 Restore the Heap Property:
       - Two options arise:
         a. The swapped element might violate the heap from above → perform `heapify up`
         b. The swapped element might violate the heap below → perform `heapify down`
       - You can run both:
         - First try `heapify down`, if it doesn't move, try `heapify up`.

       - Alternatively:
         - If the new value is smaller than the removed one (in a Min Heap), use `heapify up`
         - If the new value is larger, use `heapify down`

    ============================================
    🔹 Time Complexity Summary
    ============================================

    - Searching for the element:         O(n)
    - Swapping with the last element:    O(1)
    - Removing last element:             O(1)
    - Heapify Up or Down:                O(log n)

    ➤ Overall Time Complexity: **O(n)** (due to linear search)
    ➤ If index is known in advance: **O(log n)**

    ============================================
    🔹 Notes
    ============================================

    - Removing arbitrary elements is uncommon in basic heap use cases.
    - For efficient removals, use a HashMap to store value → index mapping.
    - Java’s PriorityQueue does support removal, but it's also O(n) internally.
    - Most real-world problems prefer removing the root (`extractMin` or `extractMax`), which is O(log n).
*/
