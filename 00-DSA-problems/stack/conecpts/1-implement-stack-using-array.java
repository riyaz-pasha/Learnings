import java.util.ArrayList;

class StackUsingArray {

    private final int[] stack;
    private final int capacity;
    private int topIndex;

    public StackUsingArray(int capacity) {
        this.capacity = capacity;
        this.stack = new int[this.capacity];
        this.topIndex = -1;
    }

    public boolean isFull() {
        return this.topIndex == this.capacity - 1;
    }

    public boolean isEmpty() {
        return this.topIndex == -1;
    }

    public void push(int value) {
        if (this.isFull()) {
            System.out.println("Stack Overflow! Cannot push " + value);
            return;
        }
        this.stack[++this.topIndex] = value;
    }

    public int pop() {
        if (this.isEmpty()) {
            System.out.println("Stack Underflow! Cannot pop");
            return -1;
        }
        return this.stack[this.topIndex--];
    }

    public int peek() {
        if (this.isEmpty()) {
            System.out.println("Stack is empty");
            return -1;
        }
        return this.stack[this.topIndex];
    }
    /*
     * | Operation | Time Complexity |
     * | --------- | --------------- |
     * | Push | O(1) |
     * | Pop | O(1) |
     * | Peek | O(1) |
     * | Space | O(n) |
     */

}

class StackUsingArrayList {

    private final ArrayList<Integer> stack;

    // Constructor
    public StackUsingArrayList() {
        stack = new ArrayList<>();
    }

    // Push operation
    public void push(int value) {
        stack.add(value); // add at end = top of stack
    }

    // Pop operation
    public int pop() {
        if (isEmpty()) {
            System.out.println("Stack Underflow! Cannot pop");
            return -1;
        }
        return stack.remove(stack.size() - 1);
    }

    // Peek operation
    public int peek() {
        if (isEmpty()) {
            System.out.println("Stack is empty");
            return -1;
        }
        return stack.get(stack.size() - 1);
    }

    // Check if stack is empty
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    // Get size of stack
    public int size() {
        return stack.size();
    }

    // Print stack
    public void printStack() {
        if (isEmpty()) {
            System.out.println("Stack is empty");
            return;
        }

        System.out.print("Stack elements: ");
        for (int i = 0; i < stack.size(); i++) {
            System.out.print(stack.get(i) + " ");
        }
        System.out.println();
    }
}
