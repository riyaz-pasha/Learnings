/**
 * Asteroid Collision - Complete Solutions
 * 
 * Problem: Given an array of asteroids where absolute value = size and sign = direction
 * (positive = right, negative = left), find state after all collisions.
 * 
 * Collision Rules:
 * 1. Two asteroids moving in same direction never collide
 * 2. When collision occurs: smaller explodes, equal size both explode
 * 3. Only right-moving (+) and left-moving (-) asteroids collide when they meet
 * 
 * Key Insight: Use stack to track surviving asteroids. Only collisions happen when:
 * - Stack has positive asteroid (moving right)
 * - Current asteroid is negative (moving left)
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

class AsteroidCollision {
    
    // ========================================================================
    // SOLUTION 1: STACK APPROACH (OPTIMAL)
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Stack-Based Solution
     * 
     * Strategy:
     * - Use stack to track asteroids that survive
     * - Process each asteroid one by one
     * - Only collision case: top of stack is positive (+) and current is negative (-)
     * 
     * Cases to handle:
     * 1. Current asteroid moving right (+): Always push to stack
     * 2. Current asteroid moving left (-):
     *    a. Stack empty or top is negative: Push (no collision)
     *    b. Top is positive: Collision occurs
     *       - If current is larger: Pop stack, continue checking
     *       - If current is smaller: Current destroyed, stop
     *       - If equal size: Pop stack, current destroyed, stop
     */
    public static int[] asteroidCollision(int[] asteroids) {
        Stack<Integer> stack = new Stack<>();
        
        for (int asteroid : asteroids) {
            boolean alive = true;
            
            // Process collisions if current asteroid is moving left (negative)
            while (alive && asteroid < 0 && !stack.isEmpty() && stack.peek() > 0) {
                // Collision between positive (right) and negative (left)
                int top = stack.peek();
                
                if (top < -asteroid) {
                    // Current asteroid is bigger, destroy the one in stack
                    stack.pop();
                    // Continue checking with remaining asteroids in stack
                } else if (top == -asteroid) {
                    // Both same size, both explode
                    stack.pop();
                    alive = false;
                } else {
                    // Top is bigger, current asteroid destroyed
                    alive = false;
                }
            }
            
            // Add asteroid to stack if it survived
            if (alive) {
                stack.push(asteroid);
            }
        }
        
        // Convert stack to array
        int[] result = new int[stack.size()];
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = stack.pop();
        }
        
        return result;
    }
    
    // ========================================================================
    // SOLUTION 2: ARRAY-BASED STACK (SPACE OPTIMIZED)
    // Time Complexity: O(n)
    // Space Complexity: O(n) but more efficient
    // ========================================================================
    
    /**
     * Array-Based Stack Implementation
     * 
     * Instead of using Stack class, use array as stack for better performance.
     * This avoids boxing/unboxing overhead and is more memory efficient.
     */
    public static int[] asteroidCollisionArrayStack(int[] asteroids) {
        int[] stack = new int[asteroids.length];
        int top = -1; // Stack pointer
        
        for (int asteroid : asteroids) {
            boolean alive = true;
            
            // Check for collisions
            while (alive && asteroid < 0 && top >= 0 && stack[top] > 0) {
                if (stack[top] < -asteroid) {
                    // Current asteroid wins, remove top
                    top--;
                } else if (stack[top] == -asteroid) {
                    // Both destroy each other
                    top--;
                    alive = false;
                } else {
                    // Top wins, current destroyed
                    alive = false;
                }
            }
            
            if (alive) {
                stack[++top] = asteroid;
            }
        }
        
        // Return only the valid portion of stack
        return Arrays.copyOfRange(stack, 0, top + 1);
    }
    
    // ========================================================================
    // SOLUTION 3: SIMULATION WITH DETAILED TRACKING
    // Time Complexity: O(n)
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Detailed Simulation for Understanding
     * 
     * This version includes verbose logging to help understand the collision process.
     * Useful for debugging and learning.
     */
    public static int[] asteroidCollisionVerbose(int[] asteroids, boolean debug) {
        Stack<Integer> stack = new Stack<>();
        
        if (debug) {
            System.out.println("\n=== Processing asteroids: " + Arrays.toString(asteroids) + " ===");
        }
        
        for (int i = 0; i < asteroids.length; i++) {
            int asteroid = asteroids[i];
            boolean alive = true;
            
            if (debug) {
                System.out.println("\nProcessing asteroid[" + i + "] = " + asteroid);
                System.out.println("Stack before: " + stack);
            }
            
            while (alive && asteroid < 0 && !stack.isEmpty() && stack.peek() > 0) {
                int top = stack.peek();
                
                if (debug) {
                    System.out.println("  Collision: " + top + " (right) vs " + asteroid + " (left)");
                }
                
                if (top < -asteroid) {
                    stack.pop();
                    if (debug) System.out.println("  Result: " + asteroid + " wins, " + top + " destroyed");
                } else if (top == -asteroid) {
                    stack.pop();
                    alive = false;
                    if (debug) System.out.println("  Result: Both destroyed (equal size)");
                } else {
                    alive = false;
                    if (debug) System.out.println("  Result: " + top + " wins, " + asteroid + " destroyed");
                }
            }
            
            if (alive) {
                stack.push(asteroid);
                if (debug) System.out.println("  Added to stack: " + asteroid);
            }
            
            if (debug) {
                System.out.println("Stack after: " + stack);
            }
        }
        
        if (debug) {
            System.out.println("\nFinal result: " + stack);
        }
        
        int[] result = new int[stack.size()];
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = stack.pop();
        }
        
        return result;
    }
    
    // ========================================================================
    // SOLUTION 4: RECURSIVE APPROACH
    // Time Complexity: O(n²) worst case
    // Space Complexity: O(n)
    // ========================================================================
    
    /**
     * Recursive Solution (Educational Purpose)
     * 
     * Not optimal but demonstrates the collision logic recursively.
     * Process from left to right, handling collisions as they occur.
     */
    public static int[] asteroidCollisionRecursive(int[] asteroids) {
        List<Integer> result = new ArrayList<>();
        for (int ast : asteroids) {
            result.add(ast);
        }
        
        return recursiveCollide(result).stream().mapToInt(i -> i).toArray();
    }
    
    private static List<Integer> recursiveCollide(List<Integer> asteroids) {
        // Find first collision point
        for (int i = 0; i < asteroids.size() - 1; i++) {
            if (asteroids.get(i) > 0 && asteroids.get(i + 1) < 0) {
                // Collision found
                int left = asteroids.get(i);
                int right = asteroids.get(i + 1);
                
                List<Integer> newList = new ArrayList<>();
                
                // Add elements before collision
                for (int j = 0; j < i; j++) {
                    newList.add(asteroids.get(j));
                }
                
                // Handle collision
                if (left < -right) {
                    // Right wins
                    newList.add(right);
                } else if (left > -right) {
                    // Left wins
                    newList.add(left);
                }
                // If equal, both destroyed (add nothing)
                
                // Add elements after collision
                for (int j = i + 2; j < asteroids.size(); j++) {
                    newList.add(asteroids.get(j));
                }
                
                // Recursively process remaining collisions
                return recursiveCollide(newList);
            }
        }
        
        // No collision found, return as is
        return asteroids;
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Visualize asteroid movements
     */
    public static void visualizeAsteroids(int[] asteroids) {
        System.out.println("\nAsteroid Visualization:");
        System.out.println("Input: " + Arrays.toString(asteroids));
        System.out.println("\nDirection indicators:");
        for (int ast : asteroids) {
            if (ast > 0) {
                System.out.printf("  %d → (moving right)%n", ast);
            } else {
                System.out.printf("  %d ← (moving left)%n", ast);
            }
        }
    }
    
    /**
     * Test a single case with all solutions
     */
    public static void testCase(String name, int[] asteroids) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST CASE: " + name);
        System.out.println("=".repeat(70));
        visualizeAsteroids(asteroids);
        
        int[] result1 = asteroidCollision(asteroids.clone());
        int[] result2 = asteroidCollisionArrayStack(asteroids.clone());
        int[] result3 = asteroidCollisionRecursive(asteroids.clone());
        
        System.out.println("\nResults:");
        System.out.println("Stack Solution:       " + Arrays.toString(result1));
        System.out.println("Array Stack:          " + Arrays.toString(result2));
        System.out.println("Recursive:            " + Arrays.toString(result3));
        
        boolean match = Arrays.equals(result1, result2) && Arrays.equals(result2, result3);
        System.out.println("All solutions match:  " + match);
        
        // Show verbose trace for first solution
        System.out.println("\n--- Detailed Trace ---");
        asteroidCollisionVerbose(asteroids.clone(), true);
    }
    
    // ========================================================================
    // MAIN METHOD WITH TEST CASES
    // ========================================================================
    
    public static void main(String[] args) {
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.println("║" + " ".repeat(20) + "ASTEROID COLLISION" + " ".repeat(30) + "║");
        System.out.println("╚" + "═".repeat(68) + "╝");
        
        // Example 1
        testCase("Example 1", new int[]{5, 10, -5});
        
        // Example 2
        testCase("Example 2", new int[]{8, -8});
        
        // Example 3
        testCase("Example 3", new int[]{10, 2, -5});
        
        // Example 4
        testCase("Example 4", new int[]{3, 5, -6, 2, -1, 4});
        
        // Additional test cases
        testCase("All moving right", new int[]{1, 2, 3, 4});
        testCase("All moving left", new int[]{-4, -3, -2, -1});
        testCase("Single asteroid", new int[]{5});
        testCase("Large vs small", new int[]{10, -1});
        testCase("Chain reaction", new int[]{1, 2, 3, -10});
        testCase("Complex scenario", new int[]{-2, -1, 1, 2});
        testCase("Multiple collisions", new int[]{5, 10, -5, -10});
        
        // Performance test
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PERFORMANCE TEST");
        System.out.println("=".repeat(70));
        
        int[] largeArray = new int[10000];
        for (int i = 0; i < 5000; i++) {
            largeArray[i] = i + 1;
        }
        for (int i = 5000; i < 10000; i++) {
            largeArray[i] = -(i - 4999);
        }
        
        long start = System.nanoTime();
        int[] result = asteroidCollision(largeArray);
        long end = System.nanoTime();
        
        System.out.println("Array size: 10,000");
        System.out.println("Surviving asteroids: " + result.length);
        System.out.println("Time taken: " + (end - start) / 1_000_000.0 + " ms");
        
        // Complexity analysis
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(70));
        System.out.println("\n1. Stack Solution (RECOMMENDED):");
        System.out.println("   Time:  O(n) - each asteroid pushed/popped at most once");
        System.out.println("   Space: O(n) - stack space");
        System.out.println("   Best for: Production code, optimal solution");
        
        System.out.println("\n2. Array-Based Stack:");
        System.out.println("   Time:  O(n) - same as stack");
        System.out.println("   Space: O(n) - but more efficient (no object overhead)");
        System.out.println("   Best for: Performance-critical applications");
        
        System.out.println("\n3. Recursive Solution:");
        System.out.println("   Time:  O(n²) - worst case with multiple passes");
        System.out.println("   Space: O(n) - recursion stack + list");
        System.out.println("   Best for: Educational purposes only");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("KEY INSIGHTS");
        System.out.println("=".repeat(70));
        System.out.println("• Collisions only occur between positive (→) and negative (←)");
        System.out.println("• Stack naturally handles the 'meet in middle' collision pattern");
        System.out.println("• Each asteroid is processed exactly once (O(n) time)");
        System.out.println("• The problem is similar to parentheses matching");
        System.out.println("• Left-moving asteroids act as 'closers' for right-moving ones");
    }
}

/**
 * ============================================================================
 * DETAILED ALGORITHM EXPLANATION
 * ============================================================================
 * 
 * Why Stack Works:
 * 
 * 1. Asteroids moving right (positive) are 'waiting' for left-moving asteroids
 * 2. Stack stores these 'waiting' asteroids in order
 * 3. When a left-moving asteroid arrives, it collides with top of stack
 * 4. Continue popping until current asteroid is destroyed or no more collisions
 * 
 * Example Trace: [3, 5, -6, 2, -1, 4]
 * 
 * Step 1: Process 3 (moving right)
 *   Stack: [3]
 *   No collision, push to stack
 * 
 * Step 2: Process 5 (moving right)
 *   Stack: [3, 5]
 *   No collision, push to stack
 * 
 * Step 3: Process -6 (moving left)
 *   Stack: [3, 5]
 *   Collision with 5: -6 wins (6 > 5), pop 5
 *   Stack: [3]
 *   Collision with 3: -6 wins (6 > 3), pop 3
 *   Stack: []
 *   No more collisions, push -6
 *   Stack: [-6]
 * 
 * Step 4: Process 2 (moving right)
 *   Stack: [-6, 2]
 *   No collision (-6 and 2 both moving away), push to stack
 * 
 * Step 5: Process -1 (moving left)
 *   Stack: [-6, 2]
 *   Collision with 2: 2 wins (2 > 1), -1 destroyed
 *   Stack: [-6, 2]
 * 
 * Step 6: Process 4 (moving right)
 *   Stack: [-6, 2, 4]
 *   No collision, push to stack
 * 
 * Final: [-6, 2, 4]
 * 
 * ============================================================================
 * COLLISION RULES MATRIX
 * ============================================================================
 * 
 * Stack Top | Current | Action
 * ----------|---------|--------------------------------------------------
 * +         | +       | No collision, push current
 * -         | -       | No collision, push current (both moving left)
 * -         | +       | No collision, push current (moving apart)
 * +         | -       | COLLISION! Compare sizes:
 *           |         |   if |top| < |current|: pop top, continue
 *           |         |   if |top| = |current|: pop top, destroy current
 *           |         |   if |top| > |current|: destroy current
 * Empty     | Any     | No collision, push current
 * 
 * ============================================================================
 */
