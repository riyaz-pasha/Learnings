/**
 * Stock Spanner - Complete Solutions
 * 
 * Problem: Design a class that returns the span of stock's price for each day.
 * Span = maximum number of consecutive days (going backward) where price <= today's price.
 * 
 * Key Insights:
 * 1. For each price, find how many consecutive previous days have price <= current
 * 2. Use monotonic decreasing stack to efficiently track previous greater prices
 * 3. Stack stores (price, span) pairs
 * 4. When current price >= stack top, we can extend the span
 * 
 * Core Strategy:
 * - Stack maintains prices in decreasing order
 * - Current price "consumes" all smaller/equal prices from stack
 * - Sum up spans of consumed prices to get total span
 * - This is similar to "Next Greater Element" and "Daily Temperatures" problems
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * ============================================================================
 * SOLUTION 1: MONOTONIC STACK WITH SPAN (OPTIMAL)
 * Time Complexity: O(1) amortized per next() call
 * Space Complexity: O(n) where n is number of calls
 * ============================================================================
 */

/**
 * Optimal Solution using Monotonic Stack
 * 
 * Strategy:
 * - Stack stores (price, span) pairs
 * - For each new price:
 *   1. Pop all prices <= current price
 *   2. Sum up spans of popped prices
 *   3. Add 1 for current day
 *   4. Push (current price, total span)
 * 
 * Why this works:
 * - If price[i] <= price[today], all days that price[i] could reach,
 *   today can also reach
 * - We can "inherit" the span from popped prices
 * 
 * Example: prices = [100, 80, 60, 70, 60, 75, 85]
 * 
 * Day 1: price=100, stack=[], span=1
 *   Push (100, 1)
 *   Stack: [(100,1)]
 * 
 * Day 2: price=80, stack=[(100,1)]
 *   80 < 100, cannot pop
 *   span = 1
 *   Push (80, 1)
 *   Stack: [(100,1), (80,1)]
 * 
 * Day 3: price=60, stack=[(100,1), (80,1)]
 *   60 < 80, cannot pop
 *   span = 1
 *   Push (60, 1)
 *   Stack: [(100,1), (80,1), (60,1)]
 * 
 * Day 4: price=70, stack=[(100,1), (80,1), (60,1)]
 *   70 >= 60, pop (60,1), span += 1
 *   70 < 80, stop
 *   span = 1 + 1 = 2
 *   Push (70, 2)
 *   Stack: [(100,1), (80,1), (70,2)]
 * 
 * Day 5: price=60, stack=[(100,1), (80,1), (70,2)]
 *   60 < 70, cannot pop
 *   span = 1
 *   Push (60, 1)
 *   Stack: [(100,1), (80,1), (70,2), (60,1)]
 * 
 * Day 6: price=75, stack=[(100,1), (80,1), (70,2), (60,1)]
 *   75 >= 60, pop (60,1), span += 1
 *   75 >= 70, pop (70,2), span += 2
 *   75 < 80, stop
 *   span = 1 + 1 + 2 = 4
 *   Push (75, 4)
 *   Stack: [(100,1), (80,1), (75,4)]
 * 
 * Day 7: price=85, stack=[(100,1), (80,1), (75,4)]
 *   85 >= 75, pop (75,4), span += 4
 *   85 >= 80, pop (80,1), span += 1
 *   85 < 100, stop
 *   span = 1 + 4 + 1 = 6
 *   Push (85, 6)
 *   Stack: [(100,1), (85,6)]
 */
class StockSpanner {
    private Stack<int[]> stack; // Each element is [price, span]
    
    public StockSpanner() {
        stack = new Stack<>();
    }
    
    public int next(int price) {
        int span = 1; // Start with current day
        
        // Pop all prices <= current price and accumulate their spans
        while (!stack.isEmpty() && stack.peek()[0] <= price) {
            span += stack.pop()[1];
        }
        
        // Push current price with its span
        stack.push(new int[]{price, span});
        
        return span;
    }
}

/**
 * ============================================================================
 * SOLUTION 2: MONOTONIC STACK WITH INDEX (ALTERNATIVE)
 * Time Complexity: O(1) amortized per next() call
 * Space Complexity: O(n)
 * ============================================================================
 */

/**
 * Alternative using indices instead of spans
 * 
 * Strategy:
 * - Stack stores (price, index) pairs
 * - For each new price:
 *   1. Pop all prices <= current price
 *   2. Span = current_index - previous_greater_index
 *   3. Push (current price, current index)
 * 
 * This is conceptually similar but uses indices for calculation.
 */
class StockSpannerWithIndex {
    private Stack<int[]> stack; // Each element is [price, index]
    private int index;
    
    public StockSpannerWithIndex() {
        stack = new Stack<>();
        index = 0;
    }
    
    public int next(int price) {
        // Pop all prices <= current price
        while (!stack.isEmpty() && stack.peek()[0] <= price) {
            stack.pop();
        }
        
        // Calculate span
        int span = stack.isEmpty() ? index + 1 : index - stack.peek()[1];
        
        // Push current price with index
        stack.push(new int[]{price, index});
        index++;
        
        return span;
    }
}

/**
 * ============================================================================
 * SOLUTION 3: BRUTE FORCE (FOR COMPARISON)
 * Time Complexity: O(n) per next() call
 * Space Complexity: O(n)
 * ============================================================================
 */

/**
 * Brute Force Approach
 * 
 * Store all prices and count backward for each new price.
 * Inefficient but straightforward.
 */
class StockSpannerBruteForce {
    private List<Integer> prices;
    
    public StockSpannerBruteForce() {
        prices = new ArrayList<>();
    }
    
    public int next(int price) {
        prices.add(price);
        int span = 1;
        
        // Count backward from current position
        for (int i = prices.size() - 2; i >= 0; i--) {
            if (prices.get(i) <= price) {
                span++;
            } else {
                break; // Stop at first greater price
            }
        }
        
        return span;
    }
}

/**
 * ============================================================================
 * SOLUTION 4: VERBOSE VERSION WITH DETAILED TRACKING
 * Time Complexity: O(1) amortized per next() call
 * Space Complexity: O(n)
 * ============================================================================
 */

/**
 * Verbose version for understanding the algorithm
 */
class StockSpannerVerbose {
    private Stack<int[]> stack;
    private int dayCount;
    private boolean debug;
    
    public StockSpannerVerbose(boolean debug) {
        stack = new Stack<>();
        dayCount = 0;
        this.debug = debug;
    }
    
    public int next(int price) {
        dayCount++;
        
        if (debug) {
            System.out.println("\n=== Day " + dayCount + ": price = " + price + " ===");
            System.out.println("Stack before: " + stackToString());
        }
        
        int span = 1;
        
        while (!stack.isEmpty() && stack.peek()[0] <= price) {
            int[] popped = stack.pop();
            if (debug) {
                System.out.println("  Popping: price=" + popped[0] + 
                                 ", span=" + popped[1] + 
                                 " (price <= " + price + ")");
            }
            span += popped[1];
        }
        
        if (debug) {
            if (stack.isEmpty()) {
                System.out.println("  No greater price found, span covers all " + 
                                 dayCount + " days");
            } else {
                System.out.println("  Stopped at price=" + stack.peek()[0] + 
                                 " (greater than " + price + ")");
            }
            System.out.println("  Total span: " + span);
        }
        
        stack.push(new int[]{price, span});
        
        if (debug) {
            System.out.println("Stack after: " + stackToString());
            System.out.println("Return: " + span);
        }
        
        return span;
    }
    
    private String stackToString() {
        if (stack.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int[] pair : stack) {
            sb.append("(").append(pair[0]).append(",").append(pair[1]).append("), ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("]");
        return sb.toString();
    }
}

/**
 * ============================================================================
 * MAIN TEST CLASS
 * ============================================================================
 */
class StockSpannerSolution {
    
    /**
     * Test the stock spanner with given prices
     */
    public static void testStockSpanner(String testName, int[] prices) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST: " + testName);
        System.out.println("=".repeat(70));
        System.out.println("Prices: " + Arrays.toString(prices));
        
        // Test optimal solution
        StockSpanner spanner1 = new StockSpanner();
        List<Integer> result1 = new ArrayList<>();
        
        // Test brute force
        StockSpannerBruteForce spanner2 = new StockSpannerBruteForce();
        List<Integer> result2 = new ArrayList<>();
        
        // Test index-based
        StockSpannerWithIndex spanner3 = new StockSpannerWithIndex();
        List<Integer> result3 = new ArrayList<>();
        
        for (int price : prices) {
            result1.add(spanner1.next(price));
            result2.add(spanner2.next(price));
            result3.add(spanner3.next(price));
        }
        
        System.out.println("\nResults:");
        System.out.println("Optimal (Stack+Span): " + result1);
        System.out.println("Brute Force:          " + result2);
        System.out.println("Index-based:          " + result3);
        
        boolean match = result1.equals(result2) && result2.equals(result3);
        System.out.println("All solutions match:  " + match);
        
        // Visual representation
        System.out.println("\nDetailed Breakdown:");
        System.out.println("Day | Price | Span | Explanation");
        System.out.println("----+-------+------+" + "-".repeat(50));
        
        for (int i = 0; i < prices.length; i++) {
            System.out.printf("%3d | %5d | %4d | ", i + 1, prices[i], result1.get(i));
            
            // Explain the span
            int span = result1.get(i);
            if (span == 1) {
                System.out.println("Previous price was greater");
            } else {
                System.out.print("Includes days: ");
                for (int j = Math.max(0, i - span + 1); j <= i; j++) {
                    System.out.print((j + 1));
                    if (j < i) System.out.print(", ");
                }
                System.out.println();
            }
        }
        
        // Verbose trace
        System.out.println("\n--- Detailed Algorithm Trace ---");
        StockSpannerVerbose spannerVerbose = new StockSpannerVerbose(true);
        for (int price : prices) {
            spannerVerbose.next(price);
        }
    }
    
    /**
     * Visualize price chart with spans
     */
    public static void visualizeSpans(int[] prices) {
        System.out.println("\nPrice Chart Visualization:");
        
        int maxPrice = 0;
        for (int price : prices) {
            maxPrice = Math.max(maxPrice, price);
        }
        
        // Print chart from top to bottom
        for (int level = maxPrice; level > 0; level -= 10) {
            System.out.printf("%3d |", level);
            for (int price : prices) {
                if (price >= level) {
                    System.out.print(" █ ");
                } else {
                    System.out.print("   ");
                }
            }
            System.out.println();
        }
        
        // Print day numbers
        System.out.print("    +");
        for (int i = 0; i < prices.length; i++) {
            System.out.print("---");
        }
        System.out.println();
        System.out.print("Day  ");
        for (int i = 1; i <= prices.length; i++) {
            System.out.printf("%2d ", i);
        }
        System.out.println();
    }
    
    /**
     * Explain the algorithm
     */
    public static void explainAlgorithm() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("ALGORITHM EXPLANATION");
        System.out.println("=".repeat(70));
        
        System.out.println("\nStock Span Definition:");
        System.out.println("• Number of consecutive days (backward) where price <= today's price");
        System.out.println("• Includes today");
        System.out.println("• Stops at first day with price > today's price");
        
        System.out.println("\nWhy Monotonic Stack Works:");
        System.out.println("1. Stack maintains prices in DECREASING order");
        System.out.println("2. If current price >= stack top:");
        System.out.println("   → Can extend span to include those days");
        System.out.println("   → Pop and accumulate spans");
        System.out.println("3. If current price < stack top:");
        System.out.println("   → That's our boundary (first greater price)");
        System.out.println("   → Stop and push current price");
        
        System.out.println("\nKey Insight:");
        System.out.println("If price[i] <= price[today], then:");
        System.out.println("• All days that day i could reach, today can also reach");
        System.out.println("• We can 'inherit' the span from day i");
        System.out.println("• This is why we sum up spans when popping");
        
        System.out.println("\nTime Complexity:");
        System.out.println("• Each price pushed once: n operations");
        System.out.println("• Each price popped at most once: n operations");
        System.out.println("• Total: 2n operations over all calls");
        System.out.println("• Amortized O(1) per next() call");
    }
    
    /**
     * Main method with comprehensive tests
     */
    public static void main(String[] args) {
        System.out.println("╔" + "═".repeat(68) + "╗");
        System.out.println("║" + " ".repeat(25) + "STOCK SPANNER" + " ".repeat(30) + "║");
        System.out.println("╚" + "═".repeat(68) + "╝");
        
        explainAlgorithm();
        
        // Example 1 from problem
        testStockSpanner("Example 1", new int[]{100, 80, 60, 70, 60, 75, 85});
        
        // Additional test cases
        testStockSpanner("Increasing Prices", new int[]{1, 2, 3, 4, 5});
        testStockSpanner("Decreasing Prices", new int[]{5, 4, 3, 2, 1});
        testStockSpanner("All Same Price", new int[]{3, 3, 3, 3, 3});
        testStockSpanner("Peak Pattern", new int[]{1, 3, 5, 3, 1});
        testStockSpanner("Valley Pattern", new int[]{5, 3, 1, 3, 5});
        testStockSpanner("Single Price", new int[]{42});
        testStockSpanner("Two Prices", new int[]{10, 20});
        testStockSpanner("Alternating", new int[]{5, 10, 5, 10, 5});
        
        // Visualization
        System.out.println("\n" + "=".repeat(70));
        System.out.println("VISUALIZATION");
        System.out.println("=".repeat(70));
        visualizeSpans(new int[]{100, 80, 60, 70, 60, 75, 85});
        
        // Performance test
        System.out.println("\n" + "=".repeat(70));
        System.out.println("PERFORMANCE TEST");
        System.out.println("=".repeat(70));
        
        StockSpanner spanner = new StockSpanner();
        int n = 100000;
        
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            spanner.next((int)(Math.random() * 1000));
        }
        long end = System.nanoTime();
        
        double timeMs = (end - start) / 1_000_000.0;
        double avgPerCall = timeMs / n;
        
        System.out.println("\nNumber of calls: " + n);
        System.out.println("Total time: " + timeMs + " ms");
        System.out.println("Average per call: " + avgPerCall + " ms");
        System.out.println("This confirms O(1) amortized time complexity!");
        
        // Complexity summary
        System.out.println("\n" + "=".repeat(70));
        System.out.println("COMPLEXITY ANALYSIS");
        System.out.println("=".repeat(70));
        
        System.out.println("\n1. Optimal Stack Solution (RECOMMENDED):");
        System.out.println("   Time:  O(1) amortized per next() call");
        System.out.println("   Space: O(n) for n calls");
        System.out.println("   Pros:  Optimal, elegant, constant time per call");
        
        System.out.println("\n2. Brute Force:");
        System.out.println("   Time:  O(n) per next() call");
        System.out.println("   Space: O(n) for n calls");
        System.out.println("   Pros:  Simple but too slow");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("RELATED PROBLEMS");
        System.out.println("=".repeat(70));
        System.out.println("• Next Greater Element");
        System.out.println("• Daily Temperatures");
        System.out.println("• Largest Rectangle in Histogram");
        System.out.println("• Sliding Window Maximum");
        System.out.println("• All use monotonic stack pattern!");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("INTERVIEW TIPS");
        System.out.println("=".repeat(70));
        System.out.println("✓ Start by explaining what 'span' means with examples");
        System.out.println("✓ Mention brute force O(n) solution first");
        System.out.println("✓ Identify that we need to find 'previous greater element'");
        System.out.println("✓ Introduce monotonic stack pattern");
        System.out.println("✓ Explain why we can accumulate spans (key insight!)");
        System.out.println("✓ Walk through example showing stack operations");
        System.out.println("✓ Discuss amortized time complexity");
    }
}

/**
 * ============================================================================
 * DETAILED WALKTHROUGH WITH VISUAL
 * ============================================================================
 * 
 * Example: [100, 80, 60, 70, 60, 75, 85]
 * 
 * Visual representation of spans:
 * 
 * Day 1 (100): [100]                    span=1 (just itself)
 * Day 2 (80):  [100, 80]                span=1 (100 > 80, stops there)
 * Day 3 (60):  [100, 80, 60]            span=1 (80 > 60, stops there)
 * Day 4 (70):  [100, 80, 60, 70]        span=2 (includes 60,70; stops at 80)
 * Day 5 (60):  [100, 80, 60, 70, 60]   span=1 (70 > 60, stops there)
 * Day 6 (75):  [100, 80, 60, 70, 60, 75] span=4 (includes 60,70,60,75; stops at 80)
 * Day 7 (85):  [100, 80, 60, 70, 60, 75, 85] span=6 (includes 80,60,70,60,75,85; stops at 100)
 * 
 * Stack Evolution:
 * 
 * After day 1: [(100,1)]
 * After day 2: [(100,1), (80,1)]
 * After day 3: [(100,1), (80,1), (60,1)]
 * After day 4: [(100,1), (80,1), (70,2)]  ← consumed (60,1)
 * After day 5: [(100,1), (80,1), (70,2), (60,1)]
 * After day 6: [(100,1), (80,1), (75,4)]  ← consumed (60,1) and (70,2)
 * After day 7: [(100,1), (85,6)]          ← consumed (75,4) and (80,1)
 * 
 * Notice how the stack stays in decreasing order of prices!
 * 
 * ============================================================================
 */
