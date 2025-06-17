import java.util.*;
/*
 * Design a stack that supports push, pop, top, and retrieving the minimum
 * element in constant time.
 * 
 * Implement the MinStack class:
 * 
 * MinStack() initializes the stack object.
 * void push(int val) pushes the element val onto the stack.
 * void pop() removes the element on the top of the stack.
 * int top() gets the top element of the stack.
 * int getMin() retrieves the minimum element in the stack.
 * You must implement a solution with O(1) time complexity for each function.
 * 
 * Example 1:
 * Input
 * ["MinStack","push","push","push","getMin","pop","top","getMin"]
 * [[],[-2],[0],[-3],[],[],[],[]]
 * Output
 * [null,null,null,null,-3,null,0,-2]
 * Explanation
 * MinStack minStack = new MinStack();
 * minStack.push(-2);
 * minStack.push(0);
 * minStack.push(-3);
 * minStack.getMin(); // return -3
 * minStack.pop();
 * minStack.top(); // return 0
 * minStack.getMin(); // return -2
 */

/**
 * Approach 1: Two Stacks (Most Intuitive)
 * Time Complexity: O(1) for all operations
 * Space Complexity: O(n) where n is the number of elements
 */
class MinStack {

    private Stack<Integer> dataStack;
    private Stack<Integer> minStack;

    public MinStack() {
        dataStack = new Stack<>();
        minStack = new Stack<>();
    }

    public void push(int val) {
        dataStack.push(val);

        // Push to minStack if it's empty or val is smaller than or equal to current min
        if (minStack.isEmpty() || val <= minStack.peek()) {
            minStack.push(val);
        }
    }

    public void pop() {
        if (dataStack.isEmpty())
            return;

        int popped = dataStack.pop();

        // Pop from minStack only if the popped element was the minimum
        if (!minStack.isEmpty() && popped == minStack.peek()) {
            minStack.pop();
        }
    }

    public int top() {
        return dataStack.peek();
    }

    public int getMin() {
        return minStack.peek();
    }

}

/**
 * Approach 2: Single Stack with Pairs
 * Time Complexity: O(1) for all operations
 * Space Complexity: O(n)
 */
class MinStack2 {

    private Stack<int[]> stack; // Each element is [value, minSoFar]

    public MinStack2() {
        stack = new Stack<>();
    }

    public void push(int val) {
        if (stack.isEmpty()) {
            stack.push(new int[] { val, val });
        } else {
            int currentMin = Math.min(val, stack.peek()[1]);
            stack.push(new int[] { val, currentMin });
        }
    }

    public void pop() {
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public int top() {
        return stack.peek()[0];
    }

    public int getMin() {
        return stack.peek()[1];
    }

}

/**
 * Approach 3: Single Stack with Custom Node
 * Time Complexity: O(1) for all operations
 * Space Complexity: O(n)
 */
class MinStack3 {

    private Node head;

    private class Node {
        int val;
        int min;
        Node next;

        Node(int val, int min, Node next) {
            this.val = val;
            this.min = min;
            this.next = next;
        }
    }

    public MinStack3() {
        head = null;
    }

    public void push(int val) {
        if (head == null) {
            head = new Node(val, val, null);
        } else {
            head = new Node(val, Math.min(val, head.min), head);
        }
    }

    public void pop() {
        if (head != null) {
            head = head.next;
        }
    }

    public int top() {
        return head.val;
    }

    public int getMin() {
        return head.min;
    }

}

/**
 * Approach 4: Space Optimized with Difference Storage
 * Time Complexity: O(1) for all operations
 * Space Complexity: O(n) but more space efficient
 */
class MinStack4 {

    private Stack<Long> stack;
    private long min;

    public MinStack4() {
        stack = new Stack<>();
    }

    public void push(int val) {
        if (stack.isEmpty()) {
            stack.push(0L);
            min = val;
        } else {
            // Store the difference between val and current min
            stack.push((long) val - min);
            if (val < min) {
                min = val;
            }
        }
    }

    public void pop() {
        if (stack.isEmpty())
            return;

        long diff = stack.pop();
        if (diff < 0) {
            // The popped element was the minimum, restore previous min
            min = min - diff;
        }
    }

    public int top() {
        long diff = stack.peek();
        if (diff < 0) {
            return (int) min;
        } else {
            return (int) (min + diff);
        }
    }

    public int getMin() {
        return (int) min;
    }

}

/**
 * Approach 5: ArrayList-based Implementation
 * Time Complexity: O(1) for all operations
 * Space Complexity: O(n)
 */
class MinStack5 {

    private List<Integer> data;
    private List<Integer> mins;

    public MinStack5() {
        data = new ArrayList<>();
        mins = new ArrayList<>();
    }

    public void push(int val) {
        data.add(val);
        if (mins.isEmpty() || val <= mins.get(mins.size() - 1)) {
            mins.add(val);
        }
    }

    public void pop() {
        if (data.isEmpty())
            return;

        int popped = data.remove(data.size() - 1);
        if (!mins.isEmpty() && popped == mins.get(mins.size() - 1)) {
            mins.remove(mins.size() - 1);
        }
    }

    public int top() {
        return data.get(data.size() - 1);
    }

    public int getMin() {
        return mins.get(mins.size() - 1);
    }

}

// Test class
class MinStackTest {

    public static void main(String[] args) {
        // Test Approach 1
        System.out.println("Testing MinStack (Two Stacks Approach):");
        testMinStack1();

        // Test Approach 2
        System.out.println("\nTesting MinStack2 (Single Stack with Pairs):");
        testMinStack2();

        // Test Approach 3
        System.out.println("\nTesting MinStack3 (Custom Node):");
        testMinStack3();

        // Test Approach 4
        System.out.println("\nTesting MinStack4 (Space Optimized):");
        testMinStack4();
    }

    private static void testMinStack1() {
        MinStack minStack = new MinStack();
        minStack.push(-2);
        minStack.push(0);
        minStack.push(-3);
        System.out.println("getMin(): " + minStack.getMin()); // -3
        minStack.pop();
        System.out.println("top(): " + minStack.top()); // 0
        System.out.println("getMin(): " + minStack.getMin()); // -2
    }

    private static void testMinStack2() {
        MinStack2 minStack = new MinStack2();
        minStack.push(-2);
        minStack.push(0);
        minStack.push(-3);
        System.out.println("getMin(): " + minStack.getMin()); // -3
        minStack.pop();
        System.out.println("top(): " + minStack.top()); // 0
        System.out.println("getMin(): " + minStack.getMin()); // -2
    }

    private static void testMinStack3() {
        MinStack3 minStack = new MinStack3();
        minStack.push(-2);
        minStack.push(0);
        minStack.push(-3);
        System.out.println("getMin(): " + minStack.getMin()); // -3
        minStack.pop();
        System.out.println("top(): " + minStack.top()); // 0
        System.out.println("getMin(): " + minStack.getMin()); // -2
    }

    private static void testMinStack4() {
        MinStack4 minStack = new MinStack4();
        minStack.push(-2);
        minStack.push(0);
        minStack.push(-3);
        System.out.println("getMin(): " + minStack.getMin()); // -3
        minStack.pop();
        System.out.println("top(): " + minStack.top()); // 0
        System.out.println("getMin(): " + minStack.getMin()); // -2
    }

}

/**
 * DETAILED EXPLANATION:
 * 
 * The challenge is to implement getMin() in O(1) time. We need to track the
 * minimum
 * element at each level of the stack without scanning through all elements.
 * 
 * APPROACH 1 - Two Stacks (Recommended):
 * - Use one stack for data and another for tracking minimums
 * - Push to minStack only when new element <= current minimum
 * - Pop from minStack only when popped element equals current minimum
 * - This handles duplicates correctly
 * 
 * APPROACH 2 - Single Stack with Pairs:
 * - Store [value, minSoFar] pairs in each stack element
 * - Each element knows its value and minimum up to that point
 * - Simple but uses more memory per element
 * 
 * APPROACH 3 - Custom Node with Linked List:
 * - Similar to approach 2 but uses custom nodes
 * - Each node stores value, minimum, and next pointer
 * - Good for understanding the concept
 * 
 * APPROACH 4 - Space Optimized:
 * - Stores differences between values and current minimum
 * - Uses mathematical trick to encode/decode values
 * - Most space efficient but more complex logic
 * 
 * APPROACH 5 - ArrayList-based:
 * - Uses ArrayList instead of Stack
 * - Similar logic to approach 1
 * - Shows that the concept works with different data structures
 * 
 * Key Insights:
 * 1. We must store minimum information at each level
 * 2. Handle duplicates carefully (use <= for minimum comparison)
 * 3. Maintain sync between data and minimum tracking
 * 4. All operations must be O(1) - no scanning allowed
 * 
 * Time Complexity: O(1) for all operations
 * Space Complexity: O(n) where n is number of elements
 */
