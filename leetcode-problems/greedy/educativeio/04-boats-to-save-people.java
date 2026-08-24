import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 * INTERVIEW GUIDE: BOATS TO SAVE PEOPLE
 * ============================================================================
 * 
 * 1. CLARIFYING QUESTIONS TO ASK:
 *    - "Can a boat carry more than two people if their combined weight is 
 *      still under the limit?" 
 *      (Assumption: No, the prompt explicitly states AT MOST TWO persons.)
 *    - "Will any single person weigh more than the limit?" 
 *      (Assumption: The constraints state people[i] <= limit, so everyone 
 *      can be rescued eventually.)
 *    - "Is the array guaranteed to be sorted?" 
 *      (Assumption: No, we must handle unsorted inputs).
 * 
 * 2. IDEA, INTUITION, & KEY OBSERVATIONS:
 *    - Goal: Minimize the total number of boats.
 *    - Observation 1 (Greedy Pairing): To minimize boats, we should try to pack 
 *      two people into as many boats as possible. 
 *    - Observation 2: The heaviest people are the hardest to pair. If we look 
 *      at the heaviest person, who should they pair with? To maximize our chances, 
 *      they should pair with the LIGHTEST person available.
 *    - Observation 3: If the heaviest person + the lightest person > limit, 
 *      then the heaviest person CANNOT pair with anyone. They must go alone.
 *    - Approach: Sort the array. Use two pointers (left at the lightest, 
 *      right at the heaviest). Check if they can share a boat. If they can, 
 *      move both pointers. If they can't, the heavy person goes alone (move 
 *      only the right pointer). Either way, it costs 1 boat.
 * 
 * 3. VISUAL EXPLANATION:
 *    People: [3, 2, 2, 1], Limit: 3
 *    
 *    Sorted Array: [1, 2, 2, 3]
 *                   ^        ^
 *                 Left(L)  Right(R)
 *    
 *    Step 1: L=1, R=3. (1 + 3) > Limit(3). 
 *            The heaviest person (3) goes alone. 
 *            R moves left. Boats = 1.
 *            
 *    Step 2: L=1, R=2. (1 + 2) <= Limit(3).
 *            They share a boat!
 *            L moves right, R moves left. Boats = 2.
 *            
 *    Step 3: L=2, R=2. (L and R point to the same person, weight 2).
 *            This person goes alone.
 *            L moves right. Boats = 3. 
 *            (Loop ends since L > R). Total Boats = 3.
 * 
 * ============================================================================
 */
class BoatsToSavePeople {

    /**
     * APPROACH 1: Greedy with Sorting and Two Pointers (Standard Optimal)
     * 
     * Time Complexity: O(N log N) where N is the number of people, dominated by the sort.
     * Space Complexity: O(1) or O(log N) depending on the sorting algorithm overhead.
     */
    public int numRescueBoatsOptimal(int[] people, int limit) {
        // Step 1: Sort the people by weight
        Arrays.sort(people);
        
        int left = 0;                  // Lightest person
        int right = people.length - 1; // Heaviest person
        int boats = 0;                 // Boat counter
        
        // Step 2: Greedily pair them
        while (left <= right) {
            // Check if the lightest and heaviest can share a boat
            if (people[left] + people[right] <= limit) {
                // They share a boat, so the lightest person is rescued
                left++;
            }
            // Regardless of whether they shared or not, the heaviest person is rescued.
            // If they didn't share, the heaviest went alone.
            right--;
            
            // One boat was used in this iteration
            boats++;
        }
        
        return boats;
    }

    /**
     * APPROACH 2: Bucket Sort / Counting Sort (Advanced / Optimization)
     * 
     * Notice the constraints: limit <= 3000. 
     * Since the weights are relatively small integers, we can skip the O(N log N) 
     * sorting step entirely and use a frequency array (bucket sort).
     * 
     * Time Complexity: O(N + Limit) -> Effectively O(N)
     * Space Complexity: O(Limit) for the frequency array.
     */
    public int numRescueBoatsBucketSort(int[] people, int limit) {
        // Array to count how many people exist for each specific weight
        int[] weightCounts = new int[limit + 1];
        for (int weight : people) {
            weightCounts[weight]++;
        }
        
        int boats = 0;
        int left = 1;      // Lightest possible weight is 1
        int right = limit; // Heaviest possible weight is the limit
        
        while (left <= right) {
            // Find the next available lightest person
            while (left <= right && weightCounts[left] <= 0) {
                left++;
            }
            // Find the next available heaviest person
            while (left <= right && weightCounts[right] <= 0) {
                right--;
            }
            
            // If pointers crossed, we are done
            if (left > right) {
                break;
            }
            
            // We have a heavy person available to put on a boat
            weightCounts[right]--; 
            boats++; 
            
            // Check if the lightest person can also fit with this heavy person
            // Note: We also ensure left <= right because if left == right, 
            // we must ensure there are actually at least 2 people of that weight to pair them.
            if (left + right <= limit && weightCounts[left] > 0) {
                weightCounts[left]--;
            }
        }
        
        return boats;
    }

    /**
     * Modern Java Feature: Using Records to organize test cases cleanly.
     * Records (introduced in Java 14) provide a concise way to create immutable data carriers.
     */
    record TestCase(int[] people, int limit, int expected) {}

    public static void main(String[] args) {
        BoatsToSavePeople solver = new BoatsToSavePeople();
        
        // Defining test cases using our Record
        var testCases = List.of(
            new TestCase(new int[]{1, 2}, 3, 1),
            new TestCase(new int[]{3, 2, 2, 1}, 3, 3),
            new TestCase(new int[]{3, 5, 3, 4}, 5, 4),
            new TestCase(new int[]{5, 1, 4, 2}, 6, 2),
            // Edge case: everyone is exactly the limit
            new TestCase(new int[]{3, 3, 3}, 3, 3) 
        );
        
        System.out.println("--- Running Approach 1 (Standard O(N log N)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            // Clone array to prevent sort from affecting the second approach's inputs
            int result = solver.numRescueBoatsOptimal(tc.people().clone(), tc.limit());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
        
        System.out.println("\n--- Running Approach 2 (Bucket Sort O(N)) ---");
        for (int i = 0; i < testCases.size(); i++) {
            var tc = testCases.get(i);
            int result = solver.numRescueBoatsBucketSort(tc.people().clone(), tc.limit());
            System.out.printf("Test %d: Expected = %d, Got = %d -> %s%n", 
                i + 1, tc.expected(), result, (result == tc.expected() ? "PASS" : "FAIL"));
        }
    }
}
