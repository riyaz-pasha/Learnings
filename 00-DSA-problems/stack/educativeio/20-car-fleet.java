/**
 * ============================================================================
 * CAR FLEET - COMPLETE INTERVIEW PREPARATION GUIDE
 * ============================================================================
 *
 * 1. PROBLEM UNDERSTANDING
 * ----------------------------------------------------------------------------
 * Statement: You have `n` cars on a single-lane road traveling toward a `target`.
 * Each car has a starting position and a constant speed. Cars cannot pass each 
 * other. If a faster car catches up to a slower car, it must slow down to match 
 * the slower car's speed, and they form a "fleet". Fleets can form exactly at 
 * the target. Return the total number of car fleets that arrive at the target.
 * 
 * Simple Explanation for Interviewer:
 * "Since cars cannot pass each other, a car's arrival time is strictly limited 
 * by any slower car ahead of it. If we calculate how long it takes each car to 
 * reach the target on an empty road, we can determine who catches up to whom. 
 * If a car starting further back has a strictly shorter time-to-target than a 
 * car ahead of it, it will crash into it and join its fleet. If it takes longer, 
 * it forms a completely new fleet."
 * 
 * Core Idea & Intuition:
 * 1. Time to Target: Time = (Target - Position) / Speed.
 * 2. Position matters: A car can only catch up to a car *ahead* of it. Thus, 
 *    processing cars from closest-to-target to furthest-from-target makes sense.
 * 3. Bottlenecks: The car closest to the target sets the initial "bottleneck" 
 *    pace. Any car behind it that has a time <= bottleneck time joins the fleet. 
 *    As soon as a car behind has a time > bottleneck time, a new fleet is born, 
 *    and this new car becomes the new bottleneck.
 * 
 * ============================================================================
 * 2. INTERVIEW CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * 1. Q: "Can two cars start at the exact same position?"
 *    Why: Dealing with overlapping starts could complicate sorting logic.
 *    Impact: Constraint says all positions are unique.
 * 
 * 2. Q: "Should the time calculation use floating-point math?"
 *    Why: (Target 10, Pos 8, Speed 3) -> Time = 2/3. Integer division yields 0.
 *    Impact: YES. We must cast to `double` before dividing: `(double)(T - P) / S`.
 * 
 * 3. Q: "Does a fleet of 1 car count as a fleet?"
 *    Why: Boundary definition.
 *    Impact: Yes, a single car is considered a fleet.
 * 
 * 4. Q: "What if the number of cars is 0?"
 *    Why: Always check edge cases.
 *    Impact: Constraints say 1 <= n <= 10^5, but we should always add a 0-check.
 * 
 * ============================================================================
 * 3. EXAMPLES AND VISUALS
 * ----------------------------------------------------------------------------
 * Example:
 * Target = 12
 * Position = [10, 8, 0, 5, 3]
 * Speed    = [ 2, 4, 1, 1, 3]
 * 
 * Step 1: Pair and calculate time-to-target.
 * Pos 10: (12 - 10) / 2 = 1.0 hr
 * Pos  8: (12 -  8) / 4 = 1.0 hr
 * Pos  5: (12 -  5) / 1 = 7.0 hr
 * Pos  3: (12 -  3) / 3 = 3.0 hr
 * Pos  0: (12 -  0) / 1 = 12.0 hr
 * 
 * Step 2: Sort descending by Position.
 * Pos | Time | Logic (Current bottleneck/maxTime = 0.0)
 * ---------------------------------------------------------------------------
 * 10  | 1.0  | 1.0 > 0.0 -> New fleet! Update maxTime = 1.0
 *  8  | 1.0  | 1.0 <= 1.0 -> Caught up! Joins current fleet.
 *  5  | 7.0  | 7.0 > 1.0 -> Too slow, new fleet! Update maxTime = 7.0
 *  3  | 3.0  | 3.0 <= 7.0 -> Caught up! Joins current fleet.
 *  0  | 12.0 | 12.0 > 7.0 -> Too slow, new fleet! Update maxTime = 12.0
 * 
 * Output: 3 Fleets.
 * 
 * ============================================================================
 * 4. INTERVIEW STRATEGY (Step-by-Step)
 * ----------------------------------------------------------------------------
 * 1. Understand: Confirm the math `Time = Distance / Speed` and the catch-up rule.
 * 2. Clarify: Ask about integer division risks (crucial!).
 * 3. Approach 1 (Stack): "I will pair position and speed, sort by position 
 *    descending, and push arrival times onto a stack. If the next car's time 
 *    is <= the top of the stack, it merges (do not push). Otherwise, push."
 * 4. Optimize Space: "Actually, we only ever compare against the top of the stack 
 *    (the current bottleneck). We don't need a physical stack, just a `maxTime` var."
 * 5. Code: Write the O(N log N) Sorting + No-Stack solution.
 * 6. Dry-run: Trace the target=12 example.
 * 7. Bonus (Ultra-Optimal): Notice `target <= 10^6`. We can use an array of size 
 *    `target` as a bucket-sort for positions to achieve O(N + Target) time!
 * 
 * ============================================================================
 * 5. EDGE CASES & MISTAKES
 * ----------------------------------------------------------------------------
 * Edge Cases:
 * - Single car: Returns 1 trivially.
 * - Extremely fast cars behind slow cars: T_fast < T_slow -> merging works perfectly.
 * - Exact same arrival times: T_A == T_B -> merging works perfectly (uses `<=`).
 * 
 * Common Mistakes:
 * - Integer Division: Doing `(target - pos) / speed` will round down, causing 
 *   incorrect fleet merges (e.g. 2/3 and 1/3 both become 0). Must use `(double)`.
 * - Sorting Ascending instead of Descending: If you process from start (pos 0) 
 *   to end (pos 10), you don't know the bottlenecks yet! Bottlenecks are dictated 
 *   by the cars closest to the target. You must sort descending.
 */

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Deque;

public class CarFleet {

    public static void main(String[] args) {
        int target = 12;
        int[] position = {10, 8, 0, 5, 3};
        int[] speed = {2, 4, 1, 1, 3};

        System.out.println("--- Stack Approach ---");
        System.out.println("Fleets: " + carFleetStack(target, position, speed)); // Expected 3

        System.out.println("\n--- Optimal Approach (No Stack) ---");
        System.out.println("Fleets: " + carFleetOptimal(target, position, speed)); // Expected 3

        System.out.println("\n--- Ultra-Optimal Array Bucket Approach ---");
        System.out.println("Fleets: " + carFleetBucket(target, position, speed)); // Expected 3
    }

    // A simple record to tie a car's position to its arrival time.
    // Records are an excellent modern Java feature to use in interviews instead of 
    // writing verbose inner classes for Data Transfer Objects (DTOs).
    record Car(int position, double time) {}

    /**
     * SOLUTION 1: STACK APPROACH (O(N log N))
     * ------------------------------------------------------------------------
     * Idea: Pair up positions and times. Sort by position descending. 
     * Push the time of a car onto the stack. If the next car's time is <= the 
     * stack's top, it means it catches up, so we don't push it.
     * 
     * Time Complexity: O(N log N) - Dominated by sorting.
     * Space Complexity: O(N) - Storing the object array and the stack.
     */
    public static int carFleetStack(int target, int[] position, int[] speed) {
        if (position.length == 0) return 0;
        int n = position.length;
        
        Car[] cars = new Car[n];
        for (int i = 0; i < n; i++) {
            // CRITICAL: Cast to double before division to avoid losing decimal precision
            double time = (double) (target - position[i]) / speed[i];
            cars[i] = new Car(position[i], time);
        }
        
        // Sort cars descending by position (closest to target comes first)
        Arrays.sort(cars, (a, b) -> Integer.compare(b.position(), a.position()));
        
        Deque<Double> stack = new ArrayDeque<>();
        
        for (Car car : cars) {
            // If stack is empty OR this car takes LONGER than the fleet in front of it
            // it forms a new fleet. (If it's <=, it crashes into the fleet in front).
            if (stack.isEmpty() || car.time() > stack.peek()) {
                stack.push(car.time());
            }
        }
        
        // The size of the stack represents the number of distinct fleets
        return stack.size();
    }

    /**
     * SOLUTION 2: OPTIMAL APPROACH (O(N log N) Time, Reduced Space)
     * ------------------------------------------------------------------------
     * Idea: We don't actually need a Stack! We only ever compare against the 
     * MOST RECENT bottleneck time (the top of the stack). We can just keep a 
     * variable `maxTime` to track the current bottleneck.
     * 
     * Time Complexity: O(N log N) - Still requires sorting.
     * Space Complexity: O(N) - We still need the array to pair and sort cars, 
     * but we eliminate the O(N) Stack overhead.
     */
    public static int carFleetOptimal(int target, int[] position, int[] speed) {
        if (position.length == 0) return 0;
        int n = position.length;
        
        Car[] cars = new Car[n];
        for (int i = 0; i < n; i++) {
            double time = (double) (target - position[i]) / speed[i];
            cars[i] = new Car(position[i], time);
        }
        
        Arrays.sort(cars, (a, b) -> Integer.compare(b.position(), a.position()));
        
        int fleetCount = 0;
        double maxTime = 0.0; // The bottleneck time of the current fleet
        
        for (Car car : cars) {
            // If the car takes strictly longer than the current fleet bottleneck,
            // it cannot catch up. It forms a new fleet and sets the new bottleneck.
            if (car.time() > maxTime) {
                maxTime = car.time();
                fleetCount++;
            }
        }
        
        return fleetCount;
    }

    /**
     * SOLUTION 3: ULTRA-OPTIMAL BUCKET APPROACH (O(N + Target) Time)
     * ------------------------------------------------------------------------
     * Idea: Sorting objects is expensive (O(N log N)). Notice the problem 
     * constraints: `target <= 10^6` and `position < target`. 
     * This means the positions fit perfectly into an array of size `target`!
     * We can map `position -> time` directly using an array. This naturally 
     * sorts the cars by position for free. We then just iterate backward 
     * from `target` to `0`.
     * 
     * Time Complexity: O(N + Target) - O(N) to populate the array, O(Target) to traverse.
     * Space Complexity: O(Target) - An array of size up to 1,000,000 doubles 
     * takes exactly 8MB of memory, which easily passes all constraints.
     * 
     * Interview Strategy: Only write this if you explicitly point out the 
     * `target <= 10^6` constraint to the interviewer and explain the memory 
     * trade-off. It is an incredible "flex" of recognizing constraints.
     */
    public static int carFleetBucket(int target, int[] position, int[] speed) {
        if (position.length == 0) return 0;
        
        // Use an array to map positions directly to their arrival times.
        // Array indices naturally act as "sorted positions".
        double[] timeAtPos = new double[target];
        
        for (int i = 0; i < position.length; i++) {
            timeAtPos[position[i]] = (double) (target - position[i]) / speed[i];
        }
        
        int fleetCount = 0;
        double maxTime = 0.0;
        
        // Traverse backwards (from closest to target -> furthest from target)
        for (int i = target - 1; i >= 0; i--) {
            // If there's a car at this position
            if (timeAtPos[i] > 0) {
                double time = timeAtPos[i];
                if (time > maxTime) {
                    maxTime = time;
                    fleetCount++;
                }
            }
        }
        
        return fleetCount;
    }

    /**
     * ========================================================================
     * 7. SOLUTION COMPARISON
     * ========================================================================
     * Approach       | Time          | Space     | Interview Recommendation
     * ------------------------------------------------------------------------
     * Stack Approach | O(N log N)    | O(N)      | Standard, shows data structures.
     * Optimal No Stck| O(N log N)    | O(N)      | ⭐ STRONGLY REC. Cleaner logic.
     * Bucket Map     | O(N + Target) | O(Target) | Best flex if you notice constraints.
     * 
     * ========================================================================
     * 8. FOLLOW-UP QUESTIONS
     * ========================================================================
     * Q1: "What if the target is 10^9?"
     * A1: We can no longer use the Bucket Map approach (Solution 3) because allocating 
     *     an array of size 1,000,000,000 would require ~8GB of RAM. We must revert 
     *     to the O(N log N) sorting approach (Solution 2), which scales cleanly 
     *     since it only uses O(N) space.
     * 
     * Q2: "Can this problem be solved without casting to `double`?"
     * A2: Yes, using cross-multiplication for fractions. 
     *     T1 = d1/s1, T2 = d2/s2. 
     *     Instead of checking `d1/s1 <= d2/s2` (which causes float precision issues), 
     *     we can check `d1 * s2 <= d2 * s1`. (Be careful of integer overflow here; 
     *     use `long` for the multiplication).
     * 
     * ========================================================================
     * 9. FINAL TAKEAWAYS
     * ========================================================================
     * - Key Pattern: When evaluating elements moving in a line where faster things 
     *   are blocked by slower things ahead, SORT BY POSITION and PROCESS BACKWARDS 
     *   (from front of the line to the back).
     * - Always double-check division in Java. `int / int` drops the remainder. 
     *   Always cast one operand to `double` if you need exact times.
     * - Records (Java 14+) are your best friend in interviews for cleaning up 
     *   Data Transfer Objects. `record Car(int position, double time) {}` is 1 line.
     */
}


import java.util.*;

/**
 * ================================================================
 * 🔥 CAR FLEET — MASTER INTERVIEW FILE
 * ================================================================
 *
 * 🧠 PROBLEM UNDERSTANDING
 * ------------------------------------------------
 * We have:
 *  - target (destination)
 *  - position[i] → starting position of car i
 *  - speed[i]    → speed of car i
 *
 * RULES:
 * - Cars move toward target
 * - NO OVERTAKING allowed
 * - If faster car catches slower → becomes part of SAME FLEET
 *
 * 🎯 GOAL:
 * Count how many fleets reach the target
 *
 * ------------------------------------------------
 *
 * 🔥 CORE INTUITION (INTERVIEW GOLD)
 * ------------------------------------------------
 * Instead of thinking about positions...
 * 👉 THINK ABOUT "TIME TO REACH TARGET"
 *
 * time[i] = (target - position[i]) / speed[i]
 *
 * WHY?
 * Because:
 * - If a car behind reaches earlier → it will catch up
 * - That means → they form a fleet
 *
 * ------------------------------------------------
 *
 * 🧠 KEY OBSERVATION
 * ------------------------------------------------
 * Sort cars by position DESC (nearest to target first)
 *
 * WHY?
 * - We process from front → back
 * - So we know whether a car can catch the fleet ahead
 *
 * ------------------------------------------------
 *
 * 🧩 CORE IDEA
 * ------------------------------------------------
 * - Traverse from nearest → farthest
 * - Maintain:
 *      lastFleetTime
 *
 * - If current car time > lastFleetTime
 *      → NEW fleet
 *
 * - Else:
 *      → merges into existing fleet
 *
 * ------------------------------------------------
 *
 * ⏱ COMPLEXITY
 * ------------------------------------------------
 * Time  : O(N log N)  (sorting)
 * Space : O(N)
 *
 * ------------------------------------------------
 *
 * 🔍 DRY RUN
 * ------------------------------------------------
 *
 * target = 12
 * position = [10, 8, 0, 5, 3]
 * speed    = [2, 4, 1, 1, 3]
 *
 * Step 1: Compute time
 * --------------------------------
 * Car: (position, time)
 * (10, 1)
 * (8, 1)
 * (5, 7)
 * (3, 3)
 * (0, 12)
 *
 * Step 2: Sort by position DESC
 * --------------------------------
 * (10,1), (8,1), (5,7), (3,3), (0,12)
 *
 * Step 3: Traverse
 * --------------------------------
 * lastFleetTime = 0
 *
 * (10,1) → new fleet → last = 1
 * (8,1)  → <=1 → merge
 * (5,7)  → >1 → new fleet → last = 7
 * (3,3)  → <=7 → merge
 * (0,12) → >7 → new fleet → last = 12
 *
 * Answer = 3
 *
 * ------------------------------------------------
 *
 * 🎯 INTERVIEW SUMMARY
 * ------------------------------------------------
 * 👉 Convert to "time to target"
 * 👉 Sort by position DESC
 * 👉 Use greedy merging
 *
 * ================================================================
 */
public class CarFleet {

    public static void main(String[] args) {

        int target = 12;
        int[] position = {10, 8, 0, 5, 3};
        int[] speed = {2, 4, 1, 1, 3};

        int fleets = carFleet(target, position, speed);

        System.out.println(fleets); // Expected: 3
    }

    /**
     * 🔥 OPTIMAL GREEDY SOLUTION
     */
    public static int carFleet(int target, int[] position, int[] speed) {

        int n = position.length;

        /**
         * ------------------------------------------------
         * 🔹 STEP 1: PAIR position + timeToTarget
         * ------------------------------------------------
         */
        record Car(int position, double time) {}

        Car[] cars = new Car[n];

        for (int i = 0; i < n; i++) {

            // Time to reach target
            double time = (double) (target - position[i]) / speed[i];

            cars[i] = new Car(position[i], time);
        }

        /**
         * ------------------------------------------------
         * 🔹 STEP 2: SORT BY POSITION DESC
         * ------------------------------------------------
         */
        Arrays.sort(cars, (a, b) -> Integer.compare(b.position(), a.position()));

        /**
         * ------------------------------------------------
         * 🔹 STEP 3: GREEDY MERGE
         * ------------------------------------------------
         */
        int fleetCount = 0;

        double lastFleetTime = 0;

        for (Car car : cars) {

            /**
             * If current car takes MORE time than fleet ahead
             * → it cannot catch → forms NEW fleet
             */
            if (car.time() > lastFleetTime) {

                fleetCount++;

                // This becomes new fleet leader
                lastFleetTime = car.time();
            }

            /**
             * Else:
             * car.time <= lastFleetTime
             *
             * → It catches up before reaching target
             * → becomes part of existing fleet
             * → NO new fleet
             */
        }

        return fleetCount;
    }
}
