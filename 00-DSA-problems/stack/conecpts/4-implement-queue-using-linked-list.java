class QueueUsingLinkedList {

    // Node definition
    private static class Node {
        int data;
        Node next;

        Node(int data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node front; // points to first element
    private Node rear;  // points to last element

    // Constructor
    public QueueUsingLinkedList() {
        front = null;
        rear = null;
    }

    // Enqueue operation
    public void enqueue(int value) {
        Node newNode = new Node(value);

        // If queue is empty
        if (rear == null) {
            front = rear = newNode;
            return;
        }

        rear.next = newNode;
        rear = newNode;
    }

    // Dequeue operation
    public int dequeue() {
        if (isEmpty()) {
            System.out.println("Queue Underflow! Cannot dequeue");
            return -1;
        }

        int value = front.data;
        front = front.next;

        // If queue becomes empty
        if (front == null) {
            rear = null;
        }

        return value;
    }

    // Peek operation
    public int peek() {
        if (isEmpty()) {
            System.out.println("Queue is empty");
            return -1;
        }
        return front.data;
    }

    // Check if queue is empty
    public boolean isEmpty() {
        return front == null;
    }

    // Print queue
    public void printQueue() {
        if (isEmpty()) {
            System.out.println("Queue is empty");
            return;
        }

        System.out.print("Queue elements (front → rear): ");
        Node current = front;
        while (current != null) {
            System.out.print(current.data + " ");
            current = current.next;
        }
        System.out.println();
    }
}
