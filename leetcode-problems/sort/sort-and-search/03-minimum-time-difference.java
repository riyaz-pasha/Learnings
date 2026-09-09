/**
 * ============================================================================
 * PROBLEM RESTATEMENT
 * ============================================================================
 * We are given a list of string times in standard 24-hour clock format ("HH:MM").
 * We need to determine the smallest time gap (in minutes) between ANY two times 
 * in this list. 
 * 
 * Crucially, a clock is circular. This means the distance between "23:50" and 
 * "00:10" the next day is only 20 minutes, not 1420 minutes. We must account 
 * for this midnight wrap-around.
 * 
 * ============================================================================
 * CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW (Confirm before writing code)
 * ============================================================================
 * 1. Can the list contain duplicate times? (If yes, the minimum difference is 
 *    automatically 0).
 * 2. Are the input strings guaranteed to be valid "HH:MM" formats ranging from 
 *    "00:00" to "23:59"?
 * 3. Does the input list fit in memory? (Constraints say length <= 20,000, 
 *    which easily fits in memory).
 * 4. Is the output always an integer representing minutes? (Yes).
 * 5. Should I prioritize time complexity over space complexity, or vice versa?
 * 6. Can I assume the input list is mutable, or should I avoid modifying it? 
 * 
 * ============================================================================
 * INTERVIEW APPROACH STRATEGY
 * ============================================================================
 * 1. Convert to a Common Unit: Comparing "HH:MM" strings is difficult. It's 
 *    easier to convert all times to the number of minutes elapsed since midnight 
 *    (00:00). For example, "01:30" becomes 1 * 60 + 30 = 90 minutes. 
 *    The max minutes in a day is 24 * 60 = 1440.
 * 
 * 2. Acknowledge the Pigeonhole Principle (The "Aha!" Moment):
 *    There are only 1440 possible unique minutes in a day. If the input list 
 *    has more than 1440 elements, at least two times MUST be identical. In 
 *    this case, we can immediately return 0 without doing any calculations.
 * 
 * 3. Solution 1: Sorting
 *    - Convert all times to integers (minutes).
 *    - Sort the list.
 *    - Iterate through adjacent elements to find the minimum difference.
 *    - Check the wrap-around difference between the first and last element 
 *      (i.e., 1440 - lastElement + firstElement).
 *    - Time: O(N log N). Space: O(N).
 * 
 * 4. Solution 2: Bucket / Boolean Array (Optimal O(N))
 *    - Since the domain of values is strictly limited to 0-1439, we can use a 
 *      boolean array of size 1440 to record which times we've seen.
 *    - While populating, if we see a `true` value, we found a duplicate -> return 0.
 *    - Iterate through the boolean array once to find the gaps between `true` flags.
 *    - Time: O(N) to parse, O(1440) to scan. Overall O(N). Space: O(1440) = O(1).
 * 
 * ============================================================================
 * VISUALIZATION & TRACING
 * ============================================================================
 * Example: timePoints = ["23:59", "00:00", "12:34"]
 * 
 * Step 1: Convert to minutes
 * "23:59" -> 23*60 + 59 = 1439
 * "00:00" -> 0*60 + 0 = 0
 * "12:34" -> 12*60 + 34 = 754
 * Array/List: [1439, 0, 754]
 * 
 * Step 2: Sort (for approach 1)
 * Sorted: [0, 754, 1439]
 * 
 * Step 3: Find adjacent differences
 * Diff 1: 754 - 0 = 754
 * Diff 2: 1439 - 754 = 685
 * 
 * Step 4: Check circular wrap-around (Midnight difference between last and first)
 * Wrap Diff: 1440 - 1439 + 0 = 1
 * 
 * Step 5: Min(754, 685, 1) = 1. Return 1.
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

public class MinimumTimeDifference {

    public static void main(String[] args) {
        List<String> times1 = List.of("23:59", "00:00");
        List<String> times2 = List.of("00:00", "23:59", "00:00");
        List<String> times3 = List.of("01:30", "15:45", "12:00", "22:15");

        System.out.println("Input: " + times1);
        System.out.println("Solution 1 (Sorting): " + findMinDifferenceSorting(times1));
        System.out.println("Solution 2 (Buckets): " + findMinDifferenceBucket(times1));
        System.out.println("--------------------------------------------------");

        System.out.println("Input: " + times2);
        System.out.println("Solution 1 (Sorting): " + findMinDifferenceSorting(times2));
        System.out.println("Solution 2 (Buckets): " + findMinDifferenceBucket(times2));
        System.out.println("--------------------------------------------------");

        System.out.println("Input: " + times3);
        System.out.println("Solution 1 (Sorting): " + findMinDifferenceSorting(times3));
        System.out.println("Solution 2 (Buckets): " + findMinDifferenceBucket(times3));
    }

    /**
     * SOLUTION 1: Sorting Approach
     * 
     * Idea: Convert times to integers, sort them, and compare adjacents plus 
     * the circular gap between the edges.
     * 
     * Time Complexity: O(N log N) - due to sorting the times.
     * Space Complexity: O(N) - storing the converted integer times.
     */
    public static int findMinDifferenceSorting(List<String> timePoints) {
        // Pigeonhole Principle optimization
        if (timePoints.size() > 1440) {
            return 0;
        }

        // Convert strings to minutes using Java Streams for a cleaner look
        List<Integer> minutes = timePoints.stream()
            .map(MinimumTimeDifference::timeToMinutes)
            .sorted()
            .collect(Collectors.toList());

        int minDiff = Integer.MAX_VALUE;

        // Compare adjacent elements
        for (int i = 1; i < minutes.size(); i++) {
            minDiff = Math.min(minDiff, minutes.get(i) - minutes.get(i - 1));
        }

        // Compare last and first element for the midnight wrap-around
        int firstTime = minutes.get(0);
        int lastTime = minutes.get(minutes.size() - 1);
        int wrapAroundDiff = 1440 - lastTime + firstTime;
        
        minDiff = Math.min(minDiff, wrapAroundDiff);

        return minDiff;
    }

    /**
     * SOLUTION 2: Bucket Array Approach (Optimal)
     * 
     * Idea: Since there are only 1440 possible unique minute values in a day,
     * we can use a boolean array of size 1440 to track seen times. This avoids
     * the O(N log N) sorting bottleneck.
     * 
     * Time Complexity: O(N) to process inputs, then O(1440) which simplifies to O(N).
     * Space Complexity: O(1440) which simplifies to O(1) auxiliary space.
     */
    public static int findMinDifferenceBucket(List<String> timePoints) {
        // Pigeonhole Principle: If we have more than 1440 times, 
        // there MUST be at least one duplicate.
        if (timePoints.size() > 1440) {
            return 0;
        }

        boolean[] seen = new boolean[1440];
        
        for (String time : timePoints) {
            int mins = timeToMinutes(time);
            if (seen[mins]) {
                // If we've already seen this exact minute, the difference is 0
                return 0;
            }
            seen[mins] = true;
        }

        int minDiff = Integer.MAX_VALUE;
        int firstSeen = -1;
        int prevSeen = -1;

        // Iterate exactly 1440 times (constant time bound)
        for (int i = 0; i < 1440; i++) {
            if (seen[i]) {
                if (firstSeen == -1) {
                    firstSeen = i; // Remember the very first time for wrap-around
                } else {
                    // Compare current time with the previously seen time
                    minDiff = Math.min(minDiff, i - prevSeen);
                }
                prevSeen = i; // Update prevSeen to current
            }
        }

        // Final check: Circular wrap-around difference
        int wrapAroundDiff = 1440 - prevSeen + firstSeen;
        minDiff = Math.min(minDiff, wrapAroundDiff);

        return minDiff;
    }

    /**
     * Helper method to convert "HH:MM" string to total minutes from midnight.
     * Extracts hours and minutes directly without splitting strings to be more efficient.
     */
    private static int timeToMinutes(String t) {
        // "HH:MM" -> indexes 0,1 are hours. 3,4 are minutes.
        int hours = (t.charAt(0) - '0') * 10 + (t.charAt(1) - '0');
        int minutes = (t.charAt(3) - '0') * 10 + (t.charAt(4) - '0');
        return hours * 60 + minutes;
    }
}
