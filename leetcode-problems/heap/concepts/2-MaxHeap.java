import java.util.ArrayList;
import java.util.List;

class MaxHeap {

    private List<Integer> heap;

    public MaxHeap() {
        this.heap = new ArrayList<>();
    }

    public MaxHeap(int[] array) {
        this.heap = new ArrayList<>();
        for (int value : array) {
            heap.add(value);
        }
        buildHeap();
    }

    // Get the maximum element (root)
    public int peek() {
        if (isEmpty()) {
            throw new RuntimeException("Heap is empty");
        }
        return heap.get(0);
    }

    // Remove and return the maximum element
    public int poll() {
        if (isEmpty()) {
            throw new RuntimeException("Heap is empty");
        }

        int max = heap.get(0);
        int lastElement = heap.get(heap.size() - 1);
        heap.set(0, lastElement);
        heap.remove(heap.size() - 1);

        if (!isEmpty()) {
            heapifyDown(0);
        }

        return max;
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
            if (parent >= 0 && heap.get(index) > heap.get(parent)) {
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
            if (heap.get(index) <= heap.get(parentIndex)) {
                break;
            }
            swap(index, parentIndex);
            index = parentIndex;
        }
    }

    // Maintain heap property by moving element down
    private void heapifyDown(int index) {
        while (hasLeftChild(index)) {
            int largerChildIndex = getLeftChildIndex(index);

            if (hasRightChild(index) &&
                    heap.get(getRightChildIndex(index)) > heap.get(largerChildIndex)) {
                largerChildIndex = getRightChildIndex(index);
            }

            if (heap.get(index) >= heap.get(largerChildIndex)) {
                break;
            }

            swap(index, largerChildIndex);
            index = largerChildIndex;
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

    // Convert heap to sorted array (heap sort - descending order)
    public int[] heapSort() {
        int[] result = new int[heap.size()];
        MaxHeap tempHeap = new MaxHeap();

        // Copy all elements to temporary heap
        for (int value : heap) {
            tempHeap.insert(value);
        }

        // Extract elements in descending order
        for (int i = 0; i < result.length; i++) {
            result[i] = tempHeap.poll();
        }

        return result;
    }

    // Get the k largest elements
    public int[] getKLargest(int k) {
        if (k <= 0 || k > heap.size()) {
            throw new IllegalArgumentException("Invalid k value");
        }

        int[] result = new int[k];
        MaxHeap tempHeap = new MaxHeap();

        // Copy all elements to temporary heap
        for (int value : heap) {
            tempHeap.insert(value);
        }

        // Extract k largest elements
        for (int i = 0; i < k; i++) {
            result[i] = tempHeap.poll();
        }

        return result;
    }

    // Check if heap property is maintained (for debugging)
    public boolean isValidMaxHeap() {
        return isValidMaxHeapHelper(0);
    }

    private boolean isValidMaxHeapHelper(int index) {
        if (index >= heap.size()) {
            return true;
        }

        int leftChild = getLeftChildIndex(index);
        int rightChild = getRightChildIndex(index);

        // Check left child
        if (leftChild < heap.size() && heap.get(index) < heap.get(leftChild)) {
            return false;
        }

        // Check right child
        if (rightChild < heap.size() && heap.get(index) < heap.get(rightChild)) {
            return false;
        }

        // Recursively check children
        return isValidMaxHeapHelper(leftChild) && isValidMaxHeapHelper(rightChild);
    }

    // Main method for testing
    public static void main(String[] args) {
        System.out.println("=== Max Heap Implementation Demo ===\n");

        // Test 1: Basic operations
        System.out.println("1. Creating empty heap and inserting elements:");
        MaxHeap heap = new MaxHeap();
        int[] values = { 15, 10, 20, 8, 25, 5, 12 };

        for (int value : values) {
            heap.insert(value);
            System.out.println("Inserted " + value + ": ");
            heap.printHeap();
        }

        System.out.println("\nMaximum element: " + heap.peek());
        System.out.println("Heap size: " + heap.size());
        System.out.println("Is valid max heap: " + heap.isValidMaxHeap());

        // Test 2: Get k largest elements
        System.out.println("\n2. Getting 3 largest elements:");
        int[] kLargest = heap.getKLargest(3);
        System.out.print("3 largest elements: ");
        for (int value : kLargest) {
            System.out.print(value + " ");
        }
        System.out.println();

        // Test 3: Polling elements
        System.out.println("\n3. Polling elements:");
        MaxHeap tempHeap = new MaxHeap();
        for (int value : values) {
            tempHeap.insert(value);
        }

        while (!tempHeap.isEmpty()) {
            int max = tempHeap.poll();
            System.out.println("Polled: " + max);
            if (!tempHeap.isEmpty()) {
                tempHeap.printHeap();
            }
        }

        // Test 4: Build heap from array
        System.out.println("\n4. Building heap from array [3, 15, 9, 7, 10, 20, 30]:");
        int[] array = { 3, 15, 9, 7, 10, 20, 30 };
        MaxHeap heap2 = new MaxHeap(array);
        heap2.printHeap();
        System.out.println("Is valid max heap: " + heap2.isValidMaxHeap());

        // Test 5: Remove specific element
        System.out.println("\n5. Removing element 15:");
        heap2.remove(15);
        heap2.printHeap();
        System.out.println("Is valid max heap: " + heap2.isValidMaxHeap());

        // Test 6: Heap sort (descending order)
        System.out.println("\n6. Heap sort result (descending order):");
        int[] sorted = heap2.heapSort();
        System.out.print("Sorted array: ");
        for (int value : sorted) {
            System.out.print(value + " ");
        }
        System.out.println();

        // Test 7: Priority queue simulation
        System.out.println("\n7. Priority Queue Simulation (higher values = higher priority):");
        MaxHeap priorityQueue = new MaxHeap();
        priorityQueue.insert(3); // Low priority task
        priorityQueue.insert(10); // High priority task
        priorityQueue.insert(7); // Medium priority task
        priorityQueue.insert(15); // Highest priority task
        priorityQueue.insert(1); // Lowest priority task

        System.out.println("Tasks in priority queue:");
        priorityQueue.printHeap();

        System.out.println("\nProcessing tasks by priority:");
        while (!priorityQueue.isEmpty()) {
            int priority = priorityQueue.poll();
            System.out.println("Processing task with priority: " + priority);
        }
    }

}

// Insert: O(log n)
// Poll: O(log n)
// Peek: O(1)
// Remove: O(log n)
// Build heap: O(n)
// Get K largest: O(k log n)

/*
    ============================
    📌 Max Heap – Theory of Operations
    ============================

    🔸 Structure:
    - A Max Heap is a complete binary tree where every parent node is >= its children.
    - It ensures the largest element is always at the root.
    - Internally implemented using an array or list with:
        - Left child index = 2 * i + 1
        - Right child index = 2 * i + 2
        - Parent index = (i - 1) / 2

    ============================
    🔹 1. Insert (add a new element)
    ============================
    - Step 1: Add the new element at the end of the heap to maintain complete tree shape.
    - Step 2: Perform "heapify up" (also called bubble up or sift up):
        - Compare the new element with its parent.
        - If it's larger than the parent, swap them.
        - Continue bubbling up until the element is in correct position or becomes the root.
    - Ensures the max heap property is restored after insertion.
    - Time Complexity: O(log n) due to height of the tree.

    ============================
    🔹 2. Get Max (peek)
    ============================
    - Return the root element, which is the maximum in the Max Heap.
    - No traversal needed since the largest element is always at the top.
    - Time Complexity: O(1)

    ============================
    🔹 3. Extract Max (remove the root)
    ============================
    - Step 1: Remove the root (maximum element).
    - Step 2: Move the last element in the heap to the root position.
    - Step 3: Perform "heapify down" (also called bubble down or sift down):
        - Compare the new root with its children.
        - Swap it with the larger child if it’s smaller.
        - Repeat until it’s larger than both children or reaches a leaf.
    - Restores the max heap property.
    - Time Complexity: O(log n)

    ============================
    🔹 4. Heapify Up (used after insertion)
    ============================
    - Used to restore the heap property from bottom to top.
    - Start at the inserted index and compare it with its parent.
    - If the current node is greater than its parent, swap them.
    - Continue until it reaches the root or no swap is needed.

    ============================
    🔹 5. Heapify Down (used after extractMax)
    ============================
    - Used to restore the heap property from top to bottom.
    - Start at the root and compare it with its left and right children.
    - Swap it with the larger child if the current node is smaller.
    - Continue until the heap property is restored or it reaches a leaf node.

    ============================
    🔹 6. Size / IsEmpty
    ============================
    - Size gives the total number of elements in the heap.
    - IsEmpty returns true if the heap is empty (size == 0).
    - Both operations are constant time: O(1)

    ============================
    🔹 7. Space Complexity
    ============================
    - Space complexity is O(n), where n is the number of elements in the heap.
    - All elements are stored in a contiguous array or list.

    ============================
    🔹 Summary of Time Complexities
    ============================
    - Insert:        O(log n)
    - Extract Max:   O(log n)
    - Get Max:       O(1)
    - Heapify Up/Down: O(log n)
    - Space:         O(n)

*/
