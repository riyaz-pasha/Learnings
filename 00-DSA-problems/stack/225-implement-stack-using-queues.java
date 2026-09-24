import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

// Approach 1: Push O(n), Pop/Top O(1)
// Make push expensive, keep newest element at front
class MyStack1 {
    private Queue<Integer> q1;
    private Queue<Integer> q2;
    
    public MyStack1() {
        q1 = new LinkedList<>();
        q2 = new LinkedList<>();
    }
    
    // Time: O(n), Space: O(1)
    public void push(int x) {
        // Add new element to empty queue
        q2.offer(x);
        
        // Move all elements from q1 to q2
        while (!q1.isEmpty()) {
            q2.offer(q1.poll());
        }
        
        // Swap references so q1 is always the main queue
        Queue<Integer> temp = q1;
        q1 = q2;
        q2 = temp;
    }
    
    // Time: O(1), Space: O(1)
    public int pop() {
        return q1.poll();
    }
    
    // Time: O(1), Space: O(1)
    public int top() {
        return q1.peek();
    }
    
    // Time: O(1), Space: O(1)
    public boolean empty() {
        return q1.isEmpty();
    }
}

// Approach 2: Push O(1), Pop/Top O(n)
// Make pop/top expensive, keep order as queue
class MyStack2 {
    private Queue<Integer> q1;
    private Queue<Integer> q2;
    private int topElement;
    
    public MyStack2() {
        q1 = new LinkedList<>();
        q2 = new LinkedList<>();
    }
    
    // Time: O(1), Space: O(1)
    public void push(int x) {
        q1.offer(x);
        topElement = x;  // Track top element
    }
    
    // Time: O(n), Space: O(1)
    public int pop() {
        // Move all except last element to q2
        while (q1.size() > 1) {
            topElement = q1.poll();
            q2.offer(topElement);
        }
        
        // Last element is the one to pop
        int result = q1.poll();
        
        // Swap queues
        Queue<Integer> temp = q1;
        q1 = q2;
        q2 = temp;
        
        return result;
    }
    
    // Time: O(1), Space: O(1)
    public int top() {
        return topElement;
    }
    
    // Time: O(1), Space: O(1)
    public boolean empty() {
        return q1.isEmpty();
    }
}

// Approach 3: Using Single Queue
// Rotate queue after each push
class MyStack3 {
    private Queue<Integer> queue;
    
    public MyStack3() {
        queue = new LinkedList<>();
    }
    
    // Time: O(n), Space: O(1)
    public void push(int x) {
        queue.offer(x);
        int size = queue.size();
        
        // Rotate queue so new element is at front
        for (int i = 0; i < size - 1; i++) {
            queue.offer(queue.poll());
        }
    }
    
    // Time: O(1), Space: O(1)
    public int pop() {
        return queue.poll();
    }
    
    // Time: O(1), Space: O(1)
    public int top() {
        return queue.peek();
    }
    
    // Time: O(1), Space: O(1)
    public boolean empty() {
        return queue.isEmpty();
    }
}

// Approach 4: Using Deque (Cheating but efficient)
class MyStack4 {
    private Deque<Integer> deque;
    
    public MyStack4() {
        deque = new LinkedList<>();
    }
    
    // Time: O(1), Space: O(1)
    public void push(int x) {
        deque.addLast(x);
    }
    
    // Time: O(1), Space: O(1)
    public int pop() {
        return deque.removeLast();
    }
    
    // Time: O(1), Space: O(1)
    public int top() {
        return deque.peekLast();
    }
    
    // Time: O(1), Space: O(1)
    public boolean empty() {
        return deque.isEmpty();
    }
}

// Test and visualization class
class StackUsingQueues {
    public static void main(String[] args) {
        System.out.println("=== Approach 1: Push O(n), Pop/Top O(1) ===");
        testStack1();
        
        System.out.println("\n=== Approach 2: Push O(1), Pop/Top O(n) ===");
        testStack2();
        
        System.out.println("\n=== Approach 3: Single Queue ===");
        testStack3();
        
        System.out.println("\n=== Comparison ===");
        compareApproaches();
    }
    
    private static void testStack1() {
        MyStack1 stack = new MyStack1();
        
        System.out.println("Push 1:");
        stack.push(1);
        visualizeQueue("After push(1)", new int[]{1});
        
        System.out.println("\nPush 2:");
        stack.push(2);
        visualizeQueue("After push(2)", new int[]{2, 1});
        
        System.out.println("\nTop: " + stack.top());
        System.out.println("Pop: " + stack.pop());
        visualizeQueue("After pop()", new int[]{1});
        
        System.out.println("\nEmpty: " + stack.empty());
    }
    
    private static void testStack2() {
        MyStack2 stack = new MyStack2();
        
        System.out.println("Operations: push(1), push(2), top(), pop(), empty()");
        stack.push(1);
        stack.push(2);
        System.out.println("Top: " + stack.top());
        System.out.println("Pop: " + stack.pop());
        System.out.println("Empty: " + stack.empty());
    }
    
    private static void testStack3() {
        MyStack3 stack = new MyStack3();
        
        System.out.println("Operations: push(1), push(2), top(), pop(), empty()");
        stack.push(1);
        System.out.println("After push(1) - queue front is now 1");
        stack.push(2);
        System.out.println("After push(2) - rotated, queue front is now 2");
        System.out.println("Top: " + stack.top());
        System.out.println("Pop: " + stack.pop());
        System.out.println("Empty: " + stack.empty());
    }
    
    private static void visualizeQueue(String label, int[] elements) {
        System.out.println(label + ":");
        System.out.print("Queue (front to back): [");
        for (int i = 0; i < elements.length; i++) {
            System.out.print(elements[i]);
            if (i < elements.length - 1) System.out.print(", ");
        }
        System.out.println("]");
        System.out.println("Stack view (top = queue front): " + elements[0] + " <- top");
    }
    
    private static void compareApproaches() {
        System.out.println("Time Complexity Comparison:");
        System.out.println("┌────────────┬──────────┬──────────┬──────────┬──────────┐");
        System.out.println("│ Approach   │ Push     │ Pop      │ Top      │ Space    │");
        System.out.println("├────────────┼──────────┼──────────┼──────────┼──────────┤");
        System.out.println("│ Approach 1 │ O(n)     │ O(1)     │ O(1)     │ O(n)     │");
        System.out.println("│ Approach 2 │ O(1)     │ O(n)     │ O(1)     │ O(n)     │");
        System.out.println("│ Approach 3 │ O(n)     │ O(1)     │ O(1)     │ O(n)     │");
        System.out.println("└────────────┴──────────┴──────────┴──────────┴──────────┘");
        
        System.out.println("\nRecommendation:");
        System.out.println("- Approach 1 or 3: If pop/top called frequently (most common)");
        System.out.println("- Approach 2: If push called much more than pop/top");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM CHALLENGE:
Implement LIFO (Last-In-First-Out) stack using FIFO (First-In-First-Out) queues
- Stack: newest element accessed first
- Queue: oldest element accessed first
- Need to reverse the natural order!

APPROACH 1: MAKE PUSH EXPENSIVE (Recommended)
═══════════════════════════════════════════════

Strategy: Keep newest element at front of queue

push(x):
1. Add x to empty queue (q2)
2. Move all elements from q1 to q2
3. Swap q1 and q2
Result: Newest element is now at front of q1

pop/top():
- Simply poll/peek from q1 (front has newest element)

EXAMPLE: Push 1, 2, 3

Initial: q1=[], q2=[]

Push 1:
  q2: [1]
  Move q1→q2: (nothing to move)
  Swap: q1=[1], q2=[]

Push 2:
  q2: [2]
  Move q1→q2: q2=[2,1]
  Swap: q1=[2,1], q2=[]
  (2 is at front, like stack top!)

Push 3:
  q2: [3]
  Move q1→q2: q2=[3,2,1]
  Swap: q1=[3,2,1], q2=[]
  
Pop: returns 3 (front of q1)
Pop: returns 2 (new front)

Time Complexity:
- push: O(n) - move all elements
- pop: O(1) - remove front
- top: O(1) - peek front
- empty: O(1)

APPROACH 2: MAKE POP EXPENSIVE
═══════════════════════════════════════════════

Strategy: Keep elements in queue order, rearrange on pop

push(x):
- Simply add to q1

pop():
1. Move n-1 elements from q1 to q2
2. Last element in q1 is the one to pop
3. Swap q1 and q2

EXAMPLE: Push 1, 2, 3, then pop

Push 1: q1=[1]
Push 2: q1=[1,2]
Push 3: q1=[1,2,3]

Pop:
  Move 1,2 to q2: q1=[3], q2=[1,2]
  Pop 3 from q1
  Swap: q1=[1,2], q2=[]
  Return 3

Time Complexity:
- push: O(1) - just add
- pop: O(n) - move n-1 elements
- top: O(1) - track separately
- empty: O(1)

APPROACH 3: SINGLE QUEUE WITH ROTATION
═══════════════════════════════════════════════

Strategy: Rotate queue after each push

push(x):
1. Add x to queue
2. Rotate queue size-1 times
Result: New element moves to front

EXAMPLE: Push 1, 2

Push 1:
  queue=[1]
  Rotate 0 times
  queue=[1]

Push 2:
  queue=[1,2]
  Rotate 1 time: poll 1, offer 1
  queue=[2,1]
  (2 is now at front!)

Time Complexity: Same as Approach 1

VISUAL COMPARISON:
═══════════════════════════════════════════════

Stack behavior we want: [3, 2, 1] (3 is top)
                         ↑  ↑  ↑
                         |  |  └─ bottom
                         |  └──── middle
                         └─────── top

Approach 1: q1 = [3, 2, 1] ← front
            (front = top of stack!)

Approach 2: q1 = [1, 2, 3] ← back
            (back = top, need to extract on pop)

DESIGN DECISIONS:
═══════════════════════════════════════════════

Which approach to choose?

Consider usage patterns:
1. General use (pop/top frequent): Approach 1 or 3 ✓✓✓
   - Most applications call pop/top more often
   - Browser back button, undo operations
   
2. Write-heavy (mostly push): Approach 2
   - Rare case where push >> pop
   - Example: Building stack, then processing once

Typical interview answer: Approach 1 or 3

COMPLEXITY SUMMARY:
═══════════════════════════════════════════════

All approaches use O(n) space for n elements

Approach 1 & 3:
- Amortized analysis: Each element moved once per push
- Total for n pushes: O(n²)
- Average per operation: O(1) for pop/top, O(n) for push

Approach 2:
- Total for n pops: O(n²)
- Average per operation: O(1) for push, O(n) for pop

IMPLEMENTATION DETAILS:
═══════════════════════════════════════════════

Queue operations used:
- offer(x): Add to back
- poll(): Remove from front
- peek(): View front
- isEmpty(): Check if empty
- size(): Get count

Key techniques:
1. Queue swapping (Approach 1, 2)
2. Element rotation (Approach 3)
3. Top element tracking (Approach 2)

EDGE CASES:
═══════════════════════════════════════════════

1. Empty stack: pop/top on empty (handle in real implementation)
2. Single element: push, pop, push again
3. Large number of operations
4. Alternating push/pop

PRACTICAL APPLICATIONS:
═══════════════════════════════════════════════

1. Educational: Understanding data structure relationships
2. Resource constraints: Only queue available
3. Distributed systems: Queue-based message systems
4. Interview preparation: Classic problem

FOLLOW-UP QUESTIONS:
═══════════════════════════════════════════════

Q: Can we do better than O(n) for any operation?
A: No, with only queue operations, we must reorder elements

Q: What if we use Deque?
A: Then it's trivial (O(1) all operations) but defeats purpose

Q: Which is better: expensive push or expensive pop?
A: Depends on usage, but usually expensive push is better

COMPARISON WITH REVERSE PROBLEM:
═══════════════════════════════════════════════

Implement Queue using Stacks:
- Push expensive OR pop expensive
- Similar tradeoffs
- Both O(n) worst case per operation

Common pattern: Implement X using Y requires expensive operation

INTERVIEW TIPS:
═══════════════════════════════════════════════

1. Clarify which operations should be optimized
2. Draw diagrams showing queue state
3. Walk through example: push 1,2,3 then pop
4. Mention both approaches (expensive push vs pop)
5. Discuss trade-offs clearly
6. Consider real-world usage patterns
7. Code cleanly with good variable names

COMMON MISTAKES:
═══════════════════════════════════════════════

1. Forgetting to swap queues
2. Off-by-one in rotation count
3. Not tracking top element in Approach 2
4. Using deque operations (not allowed)
5. Inefficient element copying

OPTIMIZATION NOTES:
═══════════════════════════════════════════════

1. Single queue (Approach 3) uses less space
2. Amortized analysis helps understand cost
3. Can optimize with circular buffer internally
4. Real implementations use arrays/lists

This problem teaches:
- Data structure relationships
- Time-space tradeoffs
- Amortized analysis
- Creative problem solving
*/
