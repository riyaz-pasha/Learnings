import java.util.ArrayList;

class QueueUsingArray {

    private final int[] queue;
    private final int capacity;
    private int start;
    private int end;
    private int size;

    public QueueUsingArray(int capacity) {
        this.capacity = capacity;
        this.queue = new int[this.capacity];
        this.end = -1;
        this.start = 0;
        this.size = 0;
    }

    public boolean isFull() {
        return this.size == this.capacity;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public int size() {
        return this.size;
    }

    public void enqueue(int value) {
        if (this.isFull()) {
            System.out.println("Queue Overflow! Cannot enqueue " + value);
            return;
        }
        this.end = (this.end + 1) % this.capacity;
        this.queue[this.end] = value;
        this.size++;
    }

    public Integer dequeue() {
        if (this.isEmpty()) {
            System.out.println("Queue Underflow! Cannot dequeue");
            return null;
        }
        int value = this.queue[this.start];
        this.start = (this.start + 1) % this.capacity;
        this.size--;
        return value;
    }

    public Integer peek() {
        if (this.isEmpty()) {
            System.out.println("Queue is empty");
            return null;
        }
        return this.queue[this.start];
    }

}

class QueueUsingArrayList {

    private final ArrayList<Integer> queue;

    // Constructor
    public QueueUsingArrayList() {
        queue = new ArrayList<>();
    }

    // Enqueue operation
    public void enqueue(int value) {
        queue.add(value); // add at rear
    }

    // Dequeue operation
    public int dequeue() {
        if (isEmpty()) {
            System.out.println("Queue Underflow! Cannot dequeue");
            return -1;
        }
        return queue.remove(0); // remove from front
    }

    // Peek operation
    public int peek() {
        if (isEmpty()) {
            System.out.println("Queue is empty");
            return -1;
        }
        return queue.get(0);
    }

    // Check if queue is empty
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    // Get size of queue
    public int size() {
        return queue.size();
    }

    // Print queue
    public void printQueue() {
        if (isEmpty()) {
            System.out.println("Queue is empty");
            return;
        }

        System.out.print("Queue elements: ");
        for (int val : queue) {
            System.out.print(val + " ");
        }
        System.out.println();
    }
}
