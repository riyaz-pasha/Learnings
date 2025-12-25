import java.util.Stack;

// Approach 1: Push O(n), Pop/Peek O(1) - AMORTIZED EFFICIENT
// Make pop/peek cheap by maintaining correct order in output stack
class MyQueue1 {
    private Stack<Integer> inStack;   // For push operations
    private Stack<Integer> outStack;  // For pop/peek operations
    
    public MyQueue1() {
        inStack = new Stack<>();
        outStack = new Stack<>();
    }
    
    // Time: O(1), Space: O(1)
    public void push(int x) {
        inStack.push(x);
    }
    
    // Time: O(1) amortized, O(n) worst case
    public int pop() {
        peek(); // Ensure outStack has elements
        return outStack.pop();
    }
    
    // Time: O(1) amortized, O(n) worst case
    public int peek() {
        if (outStack.isEmpty()) {
            // Transfer all elements from inStack to outStack
            // This reverses the order, making oldest element on top
            while (!inStack.isEmpty()) {
                outStack.push(inStack.pop());
            }
        }
        return outStack.peek();
    }
    
    // Time: O(1), Space: O(1)
    public boolean empty() {
        return inStack.isEmpty() && outStack.isEmpty();
    }
}

// Approach 2: Push O(n), Pop/Peek O(1) - TRUE O(1) POP
// Make push expensive by maintaining queue order always
class MyQueue2 {
    private Stack<Integer> stack1;
    private Stack<Integer> stack2;
    
    public MyQueue2() {
        stack1 = new Stack<>();
        stack2 = new Stack<>();
    }
    
    // Time: O(n), Space: O(1)
    public void push(int x) {
        // Move all elements to stack2
        while (!stack1.isEmpty()) {
            stack2.push(stack1.pop());
        }
        
        // Push new element to stack1
        stack1.push(x);
        
        // Move everything back
        while (!stack2.isEmpty()) {
            stack1.push(stack2.pop());
        }
        // Now oldest element is on top of stack1
    }
    
    // Time: O(1), Space: O(1)
    public int pop() {
        return stack1.pop();
    }
    
    // Time: O(1), Space: O(1)
    public int peek() {
        return stack1.peek();
    }
    
    // Time: O(1), Space: O(1)
    public boolean empty() {
        return stack1.isEmpty();
    }
}

// Test and visualization class
class QueueUsingStacks {
    public static void main(String[] args) {
        System.out.println("=== Approach 1: Amortized O(1) (RECOMMENDED) ===");
        testQueue1();
        
        System.out.println("\n=== Approach 2: Push O(n), Pop O(1) ===");
        testQueue2();
        
        System.out.println("\n=== Detailed Walkthrough ===");
        detailedWalkthrough();
        
        System.out.println("\n=== Comparison ===");
        compareApproaches();
    }
    
    private static void testQueue1() {
        MyQueue1 queue = new MyQueue1();
        
        System.out.println("push(1):");
        queue.push(1);
        System.out.println("  inStack: [1], outStack: []");
        
        System.out.println("\npush(2):");
        queue.push(2);
        System.out.println("  inStack: [1,2], outStack: []");
        
        System.out.println("\npeek(): " + queue.peek());
        System.out.println("  (Transferred to outStack)");
        System.out.println("  inStack: [], outStack: [2,1] <- top");
        
        System.out.println("\npop(): " + queue.pop());
        System.out.println("  inStack: [], outStack: [2] <- top");
        
        System.out.println("\nempty(): " + queue.empty());
    }
    
    private static void testQueue2() {
        MyQueue2 queue = new MyQueue2();
        
        queue.push(1);
        System.out.println("After push(1): stack1=[1]");
        
        queue.push(2);
        System.out.println("After push(2): stack1=[1,2] (1 on top)");
        
        System.out.println("peek(): " + queue.peek());
        System.out.println("pop(): " + queue.pop());
        System.out.println("empty(): " + queue.empty());
    }
    
    private static void detailedWalkthrough() {
        System.out.println("APPROACH 1 WALKTHROUGH:");
        System.out.println("Operation sequence: push(1), push(2), push(3), pop(), push(4), pop(), pop()");
        System.out.println();
        
        System.out.println("1. push(1):   in=[1]      out=[]");
        System.out.println("2. push(2):   in=[1,2]    out=[]");
        System.out.println("3. push(3):   in=[1,2,3]  out=[]");
        System.out.println();
        System.out.println("4. pop():");
        System.out.println("   Transfer:  in=[]       out=[3,2,1] <- top");
        System.out.println("   Pop 1:     in=[]       out=[3,2]");
        System.out.println();
        System.out.println("5. push(4):   in=[4]      out=[3,2]");
        System.out.println();
        System.out.println("6. pop():");
        System.out.println("   No transfer needed (outStack not empty)");
        System.out.println("   Pop 2:     in=[4]      out=[3]");
        System.out.println();
        System.out.println("7. pop():");
        System.out.println("   Pop 3:     in=[4]      out=[]");
        System.out.println();
        System.out.println("Notice: Each element moved at most ONCE from in to out!");
    }
    
    private static void compareApproaches() {
        System.out.println("Time Complexity Comparison:");
        System.out.println("┌────────────┬──────────────┬──────────────┬──────────┬──────────┐");
        System.out.println("│ Approach   │ Push         │ Pop          │ Peek     │ Space    │");
        System.out.println("├────────────┼──────────────┼──────────────┼──────────┼──────────┤");
        System.out.println("│ Approach 1 │ O(1)         │ O(1) amort.  │ O(1) am. │ O(n)     │");
        System.out.println("│ Approach 2 │ O(n)         │ O(1)         │ O(1)     │ O(n)     │");
        System.out.println("└────────────┴──────────────┴──────────────┴──────────┴──────────┘");
        
        System.out.println("\nKey Insights:");
        System.out.println("✓ Approach 1 (Recommended): Best overall performance");
        System.out.println("  - Amortized O(1): Each element moved exactly ONCE");
        System.out.println("  - Lazy transfer: Only move when needed");
        System.out.println("  - Most practical for real-world use");
        
        System.out.println("\n✓ Approach 2: Theoretical interest");
        System.out.println("  - True O(1) pop, but expensive push");
        System.out.println("  - Rarely better in practice");
        
        System.out.println("\nAmortized Analysis (Approach 1):");
        System.out.println("- Each element: pushed once to inStack, moved once to outStack");
        System.out.println("- Total cost for n operations: O(n)");
        System.out.println("- Average cost per operation: O(1)");
    }
}

/*
DETAILED EXPLANATION:

PROBLEM CHALLENGE:
Implement FIFO (First-In-First-Out) queue using LIFO (Last-In-First-Out) stacks
- Queue: oldest element accessed first
- Stack: newest element accessed first
- Need to reverse the natural order!

KEY INSIGHT:
═══════════════════════════════════════════════
Reversing twice gets back to original order!

Stack 1: [1,2,3] (3 on top)
         ↓ pop all and push to Stack 2
Stack 2: [3,2,1] (1 on top) ← Queue order!

APPROACH 1: TWO-STACK WITH LAZY TRANSFER (RECOMMENDED)
═══════════════════════════════════════════════

Strategy: Separate input and output stacks

inStack:  For pushing new elements
outStack: For popping/peeking (maintains queue order)

Operations:
1. push(x): Simply push to inStack
2. pop/peek(): 
   - If outStack empty, transfer ALL from inStack
   - Pop/peek from outStack

VISUAL EXAMPLE:
═══════════════════════════════════════════════

push(1), push(2), push(3):
  in: [1,2,3] ← top    out: []
  
pop() - First time:
  Transfer: Move 3→out, 2→out, 1→out
  in: []              out: [3,2,1] ← top
  Pop from out: return 1
  in: []              out: [3,2]

push(4):
  in: [4]             out: [3,2]
  
pop():
  outStack not empty, just pop
  return 2
  in: [4]             out: [3]

pop():
  return 3
  in: [4]             out: []

pop():
  outStack empty, transfer in to out
  in: []              out: [4]
  return 4

AMORTIZED ANALYSIS:
═══════════════════════════════════════════════

Why is this O(1) amortized?

Each element's journey:
1. Pushed to inStack: 1 operation
2. Moved to outStack: 1 operation (happens once!)
3. Popped from outStack: 1 operation

Total per element: 3 operations
For n elements: 3n operations
Average per operation: O(1)

Key: Elements moved at most ONCE between stacks!

APPROACH 2: MAINTAIN ORDER ALWAYS
═══════════════════════════════════════════════

Strategy: Keep oldest element always on top

push(x):
1. Move all from stack1 to stack2
2. Push x to stack1
3. Move all back from stack2 to stack1

Result: x is at bottom, oldest on top

EXAMPLE:

Initial: stack1=[1,2] (2 on top)

push(3):
  Move to s2:  stack1=[]     stack2=[2,1]
  Push 3:      stack1=[3]    stack2=[2,1]
  Move back:   stack1=[3,2,1] stack2=[]
  (1 is now on top - oldest element!)

This is less efficient because we move elements on EVERY push.

COMPARISON WITH DOCUMENT PROBLEM:
═══════════════════════════════════════════════

Document: Stack using Queues
This: Queue using Stacks

Both require reversing order!

Stack using Queue: Keep newest at front
Queue using Stack: Keep oldest on top

Similar patterns, opposite goals!

TIME COMPLEXITY DETAILED:
═══════════════════════════════════════════════

Approach 1 (Amortized):
- push: O(1) always
- pop: O(n) worst case (transfer), O(1) amortized
- peek: O(n) worst case (transfer), O(1) amortized

Worst case occurs when:
- All elements in inStack
- outStack empty
- Need to transfer all

But this happens rarely! Most operations are O(1).

Approach 2 (True O(1) pop):
- push: O(n) always (move all elements twice)
- pop: O(1) always
- peek: O(1) always

DESIGN DECISIONS:
═══════════════════════════════════════════════

Why Approach 1 is better:

1. Lazy evaluation: Only transfer when needed
2. Balanced cost: push is cheap, pop occasionally expensive
3. Real-world usage: Both push and pop are common
4. Total work: Less overall operations

Approach 2 only better if:
- Batch pushes followed by many pops (rare)
- Pop performance critical (unlikely)

IMPLEMENTATION DETAILS:
═══════════════════════════════════════════════

Stack operations used:
- push(x): Add to top
- pop(): Remove from top
- peek(): View top
- isEmpty(): Check if empty

Key techniques:
1. Lazy transfer (Approach 1)
2. Two-stack coordination
3. Proper emptiness checking (both stacks)

EDGE CASES:
═══════════════════════════════════════════════

1. Empty queue: Both stacks empty
2. Single element: Works in both stacks
3. Alternating push/pop: Tests transfer logic
4. Many pushes then pops: Amortized analysis shines

PRACTICAL APPLICATIONS:
═══════════════════════════════════════════════

1. Browser undo/redo: Convert between structures
2. Task scheduling: Stack-based systems needing queue
3. Resource limitations: Only stack available
4. Interview preparation: Classic problem!

INTERVIEW TIPS:
═══════════════════════════════════════════════

1. Start with Approach 1 (most impressive)
2. Explain amortized analysis clearly
3. Draw diagrams showing both stacks
4. Walk through push/pop sequence
5. Mention Approach 2 as alternative
6. Discuss real-world tradeoffs
7. Handle edge cases (empty queue)

COMMON MISTAKES:
═══════════════════════════════════════════════

1. Not checking outStack before transfer
2. Transferring on every operation (inefficient)
3. Wrong emptiness check (must check both stacks)
4. Not understanding amortized analysis
5. Forgetting to return value after pop

FOLLOW-UP QUESTIONS:
═══════════════════════════════════════════════

Q: Can we do better than O(n) worst case?
A: No, with only stack operations, must move elements

Q: What if we use deque?
A: Trivial O(1) but defeats the purpose

Q: Which approach for high-frequency operations?
A: Approach 1 - better average performance

Q: How does this compare to real queue?
A: Real queue is O(1) all operations, this is amortized

This problem teaches:
- Amortized analysis
- Two-data-structure coordination
- Time-space tradeoffs
- Lazy evaluation techniques
*/
