import java.util.ArrayList;
import java.util.List;
/*
 * You are given two lists of closed intervals, firstList and secondList, where
 * firstList[i] = [starti, endi] and secondList[j] = [startj, endj]. Each list
 * of intervals is pairwise disjoint and in sorted order.
 * 
 * Return the intersection of these two interval lists.
 * 
 * A closed interval [a, b] (with a <= b) denotes the set of real numbers x with
 * a <= x <= b.
 * 
 * The intersection of two closed intervals is a set of real numbers that are
 * either empty or represented as a closed interval. For example, the
 * intersection of [1, 3] and [2, 4] is [2, 3].
 * 
 * Example 1:
 * Input: firstList = [[0,2],[5,10],[13,23],[24,25]], secondList =
 * [[1,5],[8,12],[15,24],[25,26]]
 * Output: [[1,2],[5,5],[8,10],[15,23],[24,24],[25,25]]
 * 
 * Example 2:
 * Input: firstList = [[1,3],[5,9]], secondList = []
 * Output: []
 */



class IntervalIntersectionSolutions {
    
    /**
     * Solution 1: Two Pointers (Optimal)
     * Time Complexity: O(m + n)
     * Space Complexity: O(1) excluding output
     * 
     * Most efficient approach using two pointers to traverse both lists simultaneously.
     * This is the standard and recommended solution.
     */
    public int[][] intervalIntersection1(int[][] firstList, int[][] secondList) {
        List<int[]> result = new ArrayList<>();
        int i = 0, j = 0;
        
        while (i < firstList.length && j < secondList.length) {
            // Get current intervals
            int[] first = firstList[i];
            int[] second = secondList[j];
            
            // Find intersection bounds
            int start = Math.max(first[0], second[0]);
            int end = Math.min(first[1], second[1]);
            
            // If there's a valid intersection
            if (start <= end) {
                result.add(new int[]{start, end});
            }
            
            // Move the pointer of the interval that ends first
            if (first[1] <= second[1]) {
                i++;
            } else {
                j++;
            }
        }
        
        return result.toArray(new int[result.size()][]);
    }
    
    /**
     * Solution 2: Two Pointers with Detailed Logic
     * Time Complexity: O(m + n)
     * Space Complexity: O(1) excluding output
     * 
     * Similar to solution 1 but with more explicit intersection checking.
     * Good for understanding the intersection logic step by step.
     */
    public int[][] intervalIntersection2(int[][] firstList, int[][] secondList) {
        if (firstList.length == 0 || secondList.length == 0) {
            return new int[0][];
        }
        
        List<int[]> intersections = new ArrayList<>();
        int i = 0, j = 0;
        
        while (i < firstList.length && j < secondList.length) {
            int start1 = firstList[i][0], end1 = firstList[i][1];
            int start2 = secondList[j][0], end2 = secondList[j][1];
            
            // Check if intervals overlap
            if (hasOverlap(start1, end1, start2, end2)) {
                // Find intersection
                int intersectionStart = Math.max(start1, start2);
                int intersectionEnd = Math.min(end1, end2);
                intersections.add(new int[]{intersectionStart, intersectionEnd});
            }
            
            // Advance the pointer of the interval that ends earlier
            if (end1 <= end2) {
                i++;
            } else {
                j++;
            }
        }
        
        return intersections.toArray(new int[intersections.size()][]);
    }
    
    private boolean hasOverlap(int start1, int end1, int start2, int end2) {
        return Math.max(start1, start2) <= Math.min(end1, end2);
    }
    
    /**
     * Solution 3: Brute Force (For Educational Purposes)
     * Time Complexity: O(m * n)
     * Space Complexity: O(1) excluding output
     * 
     * Checks every interval in firstList against every interval in secondList.
     * Not optimal but helps understand the problem clearly.
     */
    public int[][] intervalIntersection3(int[][] firstList, int[][] secondList) {
        List<int[]> result = new ArrayList<>();
        
        for (int[] first : firstList) {
            for (int[] second : secondList) {
                // Check for intersection
                int start = Math.max(first[0], second[0]);
                int end = Math.min(first[1], second[1]);
                
                if (start <= end) {
                    result.add(new int[]{start, end});
                }
            }
        }
        
        // Sort result by start time (though it should already be sorted due to input properties)
        result.sort((a, b) -> a[0] - b[0]);
        
        return result.toArray(new int[result.size()][]);
    }
    
    /**
     * Solution 4: Merge-like Approach with State Tracking
     * Time Complexity: O(m + n)
     * Space Complexity: O(1) excluding output
     * 
     * Uses a merge-sort like approach with explicit state tracking.
     * Good for understanding merge operations on sorted arrays.
     */
    public int[][] intervalIntersection4(int[][] firstList, int[][] secondList) {
        List<int[]> result = new ArrayList<>();
        int i = 0, j = 0;
        
        while (i < firstList.length && j < secondList.length) {
            int[] interval1 = firstList[i];
            int[] interval2 = secondList[j];
            
            // Determine the relationship between intervals
            IntervalRelation relation = getIntervalRelation(interval1, interval2);
            
            switch (relation) {
                case OVERLAP:
                    int start = Math.max(interval1[0], interval2[0]);
                    int end = Math.min(interval1[1], interval2[1]);
                    result.add(new int[]{start, end});
                    
                    // Advance based on which interval ends first
                    if (interval1[1] <= interval2[1]) {
                        i++;
                    } else {
                        j++;
                    }
                    break;
                    
                case FIRST_BEFORE_SECOND:
                    i++;
                    break;
                    
                case SECOND_BEFORE_FIRST:
                    j++;
                    break;
            }
        }
        
        return result.toArray(new int[result.size()][]);
    }
    
    private enum IntervalRelation {
        OVERLAP, FIRST_BEFORE_SECOND, SECOND_BEFORE_FIRST
    }
    
    private IntervalRelation getIntervalRelation(int[] interval1, int[] interval2) {
        if (interval1[1] < interval2[0]) {
            return IntervalRelation.FIRST_BEFORE_SECOND;
        } else if (interval2[1] < interval1[0]) {
            return IntervalRelation.SECOND_BEFORE_FIRST;
        } else {
            return IntervalRelation.OVERLAP;
        }
    }
    
    /**
     * Solution 5: Functional Programming Style
     * Time Complexity: O(m + n)
     * Space Complexity: O(1) excluding output
     * 
     * Uses streams and functional programming concepts.
     * More concise but potentially less readable for some developers.
     */
    public int[][] intervalIntersection5(int[][] firstList, int[][] secondList) {
        List<int[]> result = new ArrayList<>();
        
        // Create a merged sorted stream of intervals with their source
        List<IntervalWithSource> allIntervals = new ArrayList<>();
        
        for (int[] interval : firstList) {
            allIntervals.add(new IntervalWithSource(interval, 0));
        }
        for (int[] interval : secondList) {
            allIntervals.add(new IntervalWithSource(interval, 1));
        }
        
        // Sort by start time
        allIntervals.sort((a, b) -> a.interval[0] - b.interval[0]);
        
        // Use two pointers on the merged list
        int i = 0, j = 0;
        while (i < firstList.length && j < secondList.length) {
            int start = Math.max(firstList[i][0], secondList[j][0]);
            int end = Math.min(firstList[i][1], secondList[j][1]);
            
            if (start <= end) {
                result.add(new int[]{start, end});
            }
            
            if (firstList[i][1] <= secondList[j][1]) {
                i++;
            } else {
                j++;
            }
        }
        
        return result.toArray(new int[result.size()][]);
    }
    
    private static class IntervalWithSource {
        int[] interval;
        int source; // 0 for first list, 1 for second list
        
        IntervalWithSource(int[] interval, int source) {
            this.interval = interval;
            this.source = source;
        }
    }
    
    /**
     * Solution 6: Recursive Approach
     * Time Complexity: O(m + n)
     * Space Complexity: O(m + n) due to recursion stack
     * 
     * Recursive implementation for educational purposes.
     * Not recommended for large inputs due to stack overflow risk.
     */
    public int[][] intervalIntersection6(int[][] firstList, int[][] secondList) {
        List<int[]> result = new ArrayList<>();
        findIntersections(firstList, secondList, 0, 0, result);
        return result.toArray(new int[result.size()][]);
    }
    
    private void findIntersections(int[][] firstList, int[][] secondList, 
                                 int i, int j, List<int[]> result) {
        // Base case
        if (i >= firstList.length || j >= secondList.length) {
            return;
        }
        
        int start = Math.max(firstList[i][0], secondList[j][0]);
        int end = Math.min(firstList[i][1], secondList[j][1]);
        
        // If intersection exists
        if (start <= end) {
            result.add(new int[]{start, end});
        }
        
        // Recursive calls
        if (firstList[i][1] <= secondList[j][1]) {
            findIntersections(firstList, secondList, i + 1, j, result);
        } else {
            findIntersections(firstList, secondList, i, j + 1, result);
        }
    }
    
    // Utility method to print intervals
    private void printIntervals(int[][] intervals, String label) {
        System.out.print(label + ": [");
        for (int i = 0; i < intervals.length; i++) {
            System.out.print("[" + intervals[i][0] + "," + intervals[i][1] + "]");
            if (i < intervals.length - 1) System.out.print(",");
        }
        System.out.println("]");
    }
    
    // Test methods
    public static void main(String[] args) {
        IntervalIntersectionSolutions solution = new IntervalIntersectionSolutions();
        
        // Test case 1
        int[][] firstList1 = {{0,2},{5,10},{13,23},{24,25}};
        int[][] secondList1 = {{1,5},{8,12},{15,24},{25,26}};
        
        System.out.println("=== Test Case 1 ===");
        solution.printIntervals(firstList1, "First List");
        solution.printIntervals(secondList1, "Second List");
        
        int[][] result1_1 = solution.intervalIntersection1(firstList1, secondList1);
        int[][] result1_2 = solution.intervalIntersection2(firstList1, secondList1);
        int[][] result1_3 = solution.intervalIntersection3(firstList1, secondList1);
        int[][] result1_4 = solution.intervalIntersection4(firstList1, secondList1);
        int[][] result1_5 = solution.intervalIntersection5(firstList1, secondList1);
        int[][] result1_6 = solution.intervalIntersection6(firstList1, secondList1);
        
        solution.printIntervals(result1_1, "Solution 1");
        solution.printIntervals(result1_2, "Solution 2");
        solution.printIntervals(result1_3, "Solution 3");
        solution.printIntervals(result1_4, "Solution 4");
        solution.printIntervals(result1_5, "Solution 5");
        solution.printIntervals(result1_6, "Solution 6");
        
        // Test case 2: Empty second list
        int[][] firstList2 = {{1,3},{5,9}};
        int[][] secondList2 = {};
        
        System.out.println("\n=== Test Case 2 ===");
        solution.printIntervals(firstList2, "First List");
        solution.printIntervals(secondList2, "Second List");
        
        int[][] result2_1 = solution.intervalIntersection1(firstList2, secondList2);
        solution.printIntervals(result2_1, "Result");
        
        // Test case 3: No intersections
        int[][] firstList3 = {{1,2},{3,4}};
        int[][] secondList3 = {{5,6},{7,8}};
        
        System.out.println("\n=== Test Case 3 ===");
        solution.printIntervals(firstList3, "First List");
        solution.printIntervals(secondList3, "Second List");
        
        int[][] result3_1 = solution.intervalIntersection1(firstList3, secondList3);
        solution.printIntervals(result3_1, "Result");
        
        // Test case 4: Complete overlap
        int[][] firstList4 = {{1,10}};
        int[][] secondList4 = {{2,3},{4,5},{6,7}};
        
        System.out.println("\n=== Test Case 4 ===");
        solution.printIntervals(firstList4, "First List");
        solution.printIntervals(secondList4, "Second List");
        
        int[][] result4_1 = solution.intervalIntersection1(firstList4, secondList4);
        solution.printIntervals(result4_1, "Result");
    }
    
}

/*
COMPLEXITY ANALYSIS:

Solution 1 (Two Pointers - Optimal):
- Time: O(m + n) where m and n are lengths of the two lists
- Space: O(1) excluding output space
- BEST SOLUTION: Most efficient and clean implementation

Solution 2 (Two Pointers with Detailed Logic):
- Time: O(m + n)
- Space: O(1) excluding output space
- Good for understanding intersection logic explicitly

Solution 3 (Brute Force):
- Time: O(m * n) - checks every pair of intervals
- Space: O(1) excluding output space
- Educational only - not efficient for large inputs

Solution 4 (Merge-like with State Tracking):
- Time: O(m + n)
- Space: O(1) excluding output space
- Good for understanding different implementation approaches

Solution 5 (Functional Style):
- Time: O(m + n)
- Space: O(m + n) for creating merged list
- More memory usage but functional programming style

Solution 6 (Recursive):
- Time: O(m + n)
- Space: O(m + n) due to recursion stack
- Educational only - risk of stack overflow

KEY INSIGHTS:

1. **Intersection Condition**: Two intervals [a,b] and [c,d] intersect if max(a,c) <= min(b,d)

2. **Intersection Bounds**: If they intersect, the intersection is [max(a,c), min(b,d)]

3. **Pointer Movement**: Always advance the pointer of the interval that ends first

4. **Sorted Property**: Both input lists are sorted, which allows linear time solution

5. **Edge Cases**: 
   - Empty lists
   - No intersections
   - Complete overlaps
   - Single point intersections

RECOMMENDED SOLUTION:
Solution 1 (Two Pointers) is the optimal approach with O(m + n) time complexity
and minimal space usage. It's the standard solution for this problem.

COMMON MISTAKES TO AVOID:
1. Forgetting to check if intersection is valid (start <= end)
2. Incorrect pointer advancement logic
3. Not handling empty input lists
4. Incorrect intersection bound calculation
*/
