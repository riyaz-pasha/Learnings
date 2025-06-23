class MinHeapMain {
    public static void main(String[] args) {
        MinHeap minHeap = new MinHeap(10);
        minHeap.insert(20);
        minHeap.insert(15);
        minHeap.insert(30);
        minHeap.insert(5);

        System.out.println("Extracted: " + minHeap.extractMin()); // 5
    }
}

class MinHeap {

    private int[] heap;
    private int size;
    private int capacity;

    public MinHeap(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.heap = new int[capacity];
    }

    public void insert(int value) {
        this.ensureCapacity();
        this.heap[size] = value;
        size++;
        this.heapifyUp();
    }

    public int extractMin() {
        if (this.size == 0) {
            throw new IllegalStateException("Heap is empty");
        }
        int min = this.heap[0];
        this.heap[0] = this.heap[this.size - 1];
        size--;
        this.heapifyDown();
        return min;
    }

    private void heapifyUp() {
        int index = size - 1;
        int parentIndex = getParentIndex(index);
        while (index > 0 && this.heap[index] < this.heap[parentIndex]) {
            this.swap(index, parentIndex);
            index = parentIndex;
        }
    }

    private void heapifyDown() {
        this.heapifyDown(0);
    }

    private void heapifyDown(int index) {
        while (true) {
            int leftChildIndex = this.getLeftChildIndex(index);
            int rightChildIndex = this.getRightChildIndex(index);
            int smallest = index;
            if (leftChildIndex < this.size && this.heap[leftChildIndex] < this.heap[smallest]) {
                smallest = leftChildIndex;
            }
            if (rightChildIndex < this.size && this.heap[rightChildIndex] < this.heap[smallest]) {
                smallest = rightChildIndex;
            }
            if (smallest != index) {
                this.swap(index, smallest);
                index = smallest;
            } else {
                break;
            }
        }
    }

    // private void heapifyDown2() {
    // int index = 0;
    // while (getLeftChildIndex(index) < size) {
    // int smallerChildIndex = getLeftChildIndex(index);
    // if (getRightChildIndex(index) < size && heap[getRightChildIndex(index)] <
    // heap[smallerChildIndex]) {
    // smallerChildIndex = getRightChildIndex(index);
    // }
    // if (heap[index] <= heap[smallerChildIndex])
    // break;
    // swap(index, smallerChildIndex);
    // index = smallerChildIndex;
    // }
    // }

    private int getParentIndex(int index) {
        return (index - 1) / 2;
    }

    private int getLeftChildIndex(int index) {
        return (2 * index) + 1;
    }

    private int getRightChildIndex(int index) {
        return (2 * index) + 2;
    }

    private void swap(int index1, int index2) {
        int temp = this.heap[index1];
        this.heap[index1] = this.heap[index2];
        this.heap[index2] = temp;
    }

    private void ensureCapacity() {
        if (this.size == this.capacity) {
            this.capacity *= 2;
            int[] newHeap = new int[this.capacity];
            System.arraycopy(this.heap, 0, newHeap, 0, this.size);
            this.heap = newHeap;
        }
    }

}
