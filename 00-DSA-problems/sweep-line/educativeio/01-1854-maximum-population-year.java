/**
 * 1854. Maximum Population Year
 * 
 * ============================================================================
 * 1. PROBLEM STATEMENT
 * ============================================================================
 * You are given a 2D integer array logs where logs[i] = [birth_i, death_i]
 * indicates the birth and death years of the ith person.
 * The population of some year x is the number of people alive during that year.
 * A person is counted in year x if x is in the inclusive range [birth_i, death_i - 1].
 * Note: They are NOT counted in their death year.
 * Return the earliest year with the maximum population.
 * 
 * Constraints:
 * - 1 <= logs.length <= 100
 * - 1950 <= birth_i < death_i <= 2050
 * 
 * ============================================================================
 * 2. CLARIFYING QUESTIONS (To ask the interviewer)
 * ============================================================================
 * 1. Are the birth and death years strictly bounded between 1950 and 2050 as stated?
 *    (If unbounded, we can't use a fixed-size array and must use a TreeMap/sorting).
 * 2. Does "earliest year" imply we break ties by returning the numerically smallest year?
 *    (Prevents ambiguity if 1960 and 1970 both have a population of 5).
 * 3. Is the death year strictly exclusive? If someone is born in 1960 and dies in 1960, is that valid?
 *    (Clarifies if 0-duration lifespans exist; constraints say birth < death, so no, but good to verify).
 * 4. Can the logs array be empty, and if so, what should we return?
 *    (Prevents NullPointer/IndexOutOfBounds; constraints say length >= 1, but good to show defensive thinking).
 * 
 * ============================================================================
 * 3. THE REASONING JOURNEY
 * ============================================================================
 * > Core Requirement & Constraint: 
 * We need to find the point of maximum overlap among a set of intervals. 
 * The binding constraint is the range of years (1950-2050, 101 years) and the 
 * number of logs (100). Because these constraints are tiny, brute force works, 
 * but it scales horribly if lifespans or the number of people grow large.
 * 
 * --- Approach 1: True Brute Force ---
 * 1. Idea: We can physically check every single valid year. For every year from 
 *    1950 to 2050, we iterate through all the people and count how many are alive.
 * 2. What I'd naturally try: A nested loop. Outer loop = years, Inner loop = people.
 * 3. Why it works: It exhaustively verifies the population for every possible point in time.
 * 4. Why it's too slow/costly: For every year, we re-evaluate every single person.
 * 5. What work is repeated: If a person lives from 1950 to 2000, we do 50 separate 
 *    checks against that same person just to confirm they are still alive.
 * 6. The pivot observation: Instead of asking "who is alive in year X?", we can 
 *    ask "how many years did person Y live?" and just add them to those years.
 *    
 * Time Complexity: O(Y * N) — because we check every single log (N) for every single year (Y).
 * Space Complexity: O(1) — because we only need variables to track the max count and max year; no extra structures.
 * 
 * --- Approach 2: Range Addition (Intermediate) ---
 * 1. Idea: Create an array of size 101 (for 1950-2050). Iterate through each person,
 *    and for every year they are alive, add 1 to that year's bucket in the array.
 * 2. What I'd naturally try: An array `population[]`. For person `[1960, 1970]`, 
 *    run a `for (int y = 1960; y < 1970; y++) population[y - 1950]++`.
 * 3. Why it works: We build a complete histogram of the population per year.
 * 4. Why it's too slow/costly: If the range of years is huge (e.g., historical data
 *    spanning 5000 years) and people live long times, we spend a lot of time iterating 
 *    just to add +1 to contiguous buckets.
 * 5. What work is repeated: Updating `population[y]++` year by year inside a continuous span.
 * 6. The pivot observation: The population *only changes* when someone is born or dies. 
 *    Between a birth and a death, the population is flat. We don't need to add +1 
 *    to every intermediate year.
 *    
 * Time Complexity: O(N * L + Y) — because we iterate through each person (N) and then loop through their specific lifespan (L), then scan the year array (Y).
 * Space Complexity: O(Y) — because we allocate an array of size Y to store the total population per year.
 * 
 * --- Approach 3: Line Sweep / Difference Array (Optimal) ---
 * 1. Idea: We only record the *deltas* (changes) in population. A birth is a +1 
 *    at `birth_year`. A death is a -1 at `death_year`. 
 * 2. What I'd naturally try: An array `deltas[]`. For `[1960, 1970]`, `deltas[1960]++`, 
 *    `deltas[1970]--`. Then iterate through the array keeping a running total.
 * 3. Why it works: The running total (prefix sum) at any year exactly equals the 
 *    number of births minus the number of deaths up to that point—which is the population!
 * 4. Why this is the ceiling: We only process each person's endpoints (O(1) per person), 
 *    and we only scan the timeline once.
 * 
 * Time Complexity: O(N + Y) — because we do exactly two non-nested passes: one over the logs (N) to record changes, and one over the year range (Y) to compute prefix sums.
 * Space Complexity: O(Y) — because we store the timeline of population changes in an array of size Y.
 * 
 * Verdict for 45-minute interview:
 * I would write the Line Sweep (Approach 3). It is practically the same amount 
 * of code as Approach 2, but demonstrates understanding of a powerful algorithmic 
 * pattern (Difference Array / Line Sweep) that scales beautifully if the interviewer 
 * asks "what if they lived for a million years?".
 * 
 * ============================================================================
 * 4. EDGE CASES
 * ============================================================================
 * - Disjoint lifetimes: [1950, 1960] and [1970, 1980]. Max pop is 1, returns 1950.
 * - Exact boundary touch: [1950, 1960] and [1960, 1970]. Pop never hits 2 because
 *   1960 is exclusive for the first person. 
 * - Multiple max peaks: Pop reaches 5 in 1960, drops to 2, hits 5 again in 1990.
 *   Must strictly return 1960 (the earliest).
 * - All duplicates: Everyone born 1950, dies 2050. 
 * 
 * ============================================================================
 * 5. KEY INSIGHT, DIAGRAM & DRY RUN
 * ============================================================================
 * > Key Insight: 
 * "Ask yourself: does the state actually change between the bounds?"
 * The population only fluctuates at the exact moments of birth and death. 
 * Therefore, we only need to track the events, not the steady states in between.
 * 
 * ASCII Diagram: Line Sweep
 * Logs: [1950, 1961], [1960, 1971]
 * 
 * Years:    1950 ..... 1960 ... 1961 ..... 1971
 * Events:    +1         +1       -1         -1
 * 
 * Running    1          2        1          0
 * Total:    (Max 1)    (Max 2)
 * 
 * Dry Run (Optimal Approach):
 * Logs: [[1993,1999],[2000,2010]]
 * 1. Initialize year[101]. 
 * 2. Process [1993, 1999]:
 *    year[1993-1950] = year[43] += 1
 *    year[1999-1950] = year[49] -= 1
 * 3. Process [2000, 2010]:
 *    year[2000-1950] = year[50] += 1
 *    year[2010-1950] = year[60] -= 1
 * 4. Prefix sum iteration:
 *    i=0 to 42: current = 0, max = 0
 *    i=43 (1993): current = 1, max = 1, res = 1993
 *    i=44 to 48: current = 1
 *    i=49 (1999): current = 1 + (-1) = 0
 *    i=50 (2000): current = 0 + 1 = 1. (max is already 1, res stays 1993)
 *    ... Returns 1993.
 * 
 * ============================================================================
 * 6. PITFALLS & PATTERN RECOGNITION
 * ============================================================================
 * Pitfalls:
 * - Off-by-one with deaths: The problem explicitly states death year is exclusive. 
 *   If you try to sweep at `death - 1`, you will double count if someone else is born 
 *   on the death year. -1 goes exactly AT the death year.
 * - Array sizing: 2050 - 1950 = 100. The array needs to be size 101 to safely index 
 *   up to 2050 (since 2050 - 1950 = 100, which is the 101st element).
 * 
 * Pattern Recognition: 
 * "When you see overlapping intervals and need to find the max depth, think Line Sweep."
 * This is the exact same pattern used in:
 * - Meeting Rooms II (finding max concurrent meetings)
 * - Corporate Flight Bookings
 * - My Calendar III
 * 
 * ============================================================================
 * 7. INTERVIEW SCRIPT
 * ============================================================================
 * "Since we're looking for the maximum overlap of intervals, my first thought is 
 * we could just maintain an array representing every year, and for each person, 
 * loop through their life and add 1. But that does redundant work for the years 
 * in the middle of their life. 
 * 
 * Instead, I'll use a Line Sweep. The population only changes when someone is born 
 * or dies. I'll create an array for the years 1950 to 2050. When someone is born, 
 * I add 1 to that year. When they die, I subtract 1. After recording all these 
 * changes, I can just do a single pass over the array, keeping a running total of 
 * the population. Whenever this running total exceeds my recorded max, I'll update 
 * the max and save that year. Because we sweep from left to right, we naturally 
 * get the earliest year for free. That gives us O(N + Y) time and O(Y) space."
 * 
 * ============================================================================
 * 8. FOLLOW-UPS
 * ============================================================================
 * Q: What if the years weren't bounded between 1950 and 2050, but could be any 
 *    integer from -10^9 to 10^9?
 * A: We can't use a fixed-size array anymore because it would take O(10^9) memory. 
 *    Instead, we use a `TreeMap<Integer, Integer>` where the key is the year and 
 *    the value is the delta (+1 or -1). We populate it in O(N log N) time, then 
 *    iterate through the map's entries (which are automatically sorted by year) 
 *    keeping our running total.
 * 
 * Q: What if we needed to return the actual *interval* of maximum population, 
 *    not just the first year?
 * A: We would track the `maxPopulation`. When `currentPopulation` hits `maxPopulation`,
 *    we record the current year as `start`. When `currentPopulation` drops below 
 *    `maxPopulation` (via a death), we record that year as `end`.
 */

import java.util.Arrays;

public class MaximumPopulationYear {

    /**
     * APPROACH 1: True Brute Force
     * Time Complexity: O(Y * N)
     * Space Complexity: O(1)
     */
    public int maximumPopulationBruteForce(int[][] logs) {
        int maxPop = 0;
        int earliestYear = 1950;
        
        // Loop over every possible year
        for (int year = 1950; year <= 2050; year++) {
            int currentPop = 0;
            
            // For this specific year, check every person
            for (int[] log : logs) {
                // inclusive birth, exclusive death
                if (log[0] <= year && year < log[1]) {
                    currentPop++;
                }
            }
            
            if (currentPop > maxPop) {
                maxPop = currentPop;
                earliestYear = year;
            }
        }
        return earliestYear;
    }

    /**
     * APPROACH 2: Range Addition (Intermediate)
     * Time Complexity: O(N * L + Y) 
     * Space Complexity: O(Y) 
     */
    public int maximumPopulationArray(int[][] logs) {
        // e.g., population[10] represents the total people alive in 1960
        int[] population = new int[101]; 
        
        for (int[] log : logs) {
            int birth = log[0];
            int death = log[1];
            
            // Loop through their specific life and increment every year
            for (int year = birth; year < death; year++) {
                population[year - 1950]++;
            }
        }
        
        int maxPop = 0;
        int earliestYear = 1950;
        
        // Find the earliest year with the highest population
        for (int i = 0; i < population.length; i++) {
            if (population[i] > maxPop) {
                maxPop = population[i];
                earliestYear = i + 1950;
            }
        }
        
        return earliestYear;
    }

    /**
     * APPROACH 3: Line Sweep / Difference Array (Optimal)
     * Time Complexity: O(N + Y)
     * Space Complexity: O(Y)
     */
    public int maximumPopulationLineSweep(int[][] logs) {
        // deltas[i] holds the NET change in population during year (i + 1950)
        // e.g., if logs contains [1960, 1970], deltas[10] (1960) gets +1, deltas[20] (1970) gets -1
        int[] deltas = new int[101]; 
        
        // 1. Record the changes (Births add 1, Deaths remove 1)
        for (int[] log : logs) {
            // log[0] is birth, log[1] is death
            // We shift by 1950 to map 1950->0, 2050->100
            deltas[log[0] - 1950]++; 
            deltas[log[1] - 1950]--;
        }
        
        int maxPop = 0;
        int currentPop = 0;
        int earliestYear = 1950;
        
        // 2. Compute the prefix sum to find the actual population at each year
        for (int i = 0; i < deltas.length; i++) {
            // currentPop accumulates the +1s and -1s as we sweep left to right
            currentPop += deltas[i];
            
            // Strictly greater (>), not (>=), guarantees we keep the EARLIEST year
            if (currentPop > maxPop) {
                maxPop = currentPop;
                // Reverse the shift to get the actual calendar year
                earliestYear = i + 1950; 
            }
        }
        
        return earliestYear;
    }

    // ============================================================================
    // MAIN METHOD FOR TESTING
    // ============================================================================
    public static void main(String[] args) {
        MaximumPopulationYear solution = new MaximumPopulationYear();
        
        // Test Case 1: Standard case
        int[][] logs1 = {{1993, 1999}, {2000, 2010}};
        System.out.println("Test 1 (Expected 1993): " + solution.maximumPopulationLineSweep(logs1));
        
        // Test Case 2: Overlapping lifetimes with multiple peaks
        int[][] logs2 = {{1950, 1961}, {1960, 1971}, {1970, 1981}};
        System.out.println("Test 2 (Expected 1960): " + solution.maximumPopulationLineSweep(logs2));
        
        // Test Case 3: Edge Case - Exact boundary touch (death matches birth of next)
        int[][] logs3 = {{1950, 1960}, {1960, 1970}};
        // Pop in 1960 is 1, because first person dies (exclusive), second is born.
        // Highest pop is 1, earliest year is 1950.
        System.out.println("Test 3 (Expected 1950): " + solution.maximumPopulationLineSweep(logs3));
        
        // Test Case 4: Edge Case - All duplicate spans
        int[][] logs4 = {{1980, 2000}, {1980, 2000}, {1980, 2000}};
        System.out.println("Test 4 (Expected 1980): " + solution.maximumPopulationLineSweep(logs4));
        
        // Cross-check all methods for consistency
        System.out.println("\nCross-checking approaches on Test 2:");
        System.out.println("Brute Force:  " + solution.maximumPopulationBruteForce(logs2));
        System.out.println("Array Add:    " + solution.maximumPopulationArray(logs2));
        System.out.println("Line Sweep:   " + solution.maximumPopulationLineSweep(logs2));
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * Core pattern: Line Sweep / Difference Array.
 * 
 * Key observation: For overlapping range queries, you don't need to update the
 * entire range. You only need to record the START (+1) and END (-1) events.
 * 
 * Memorize vs Understand: 
 * UNDERSTAND how prefix sums convert a list of changes (deltas) into a running total.
 * Don't just memorize the code; visualize the +1 and -1 acting as gates opening and closing.
 * 
 * Most common trap: Counting the end-point (death year) inclusively instead of 
 * exclusively. Always check the exact boundary rules in interval problems.
 * 
 * One-line mental trigger: "Find max overlapping ranges? Record start +1, end -1, then sweep."
 */









































import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Solution {

    // Each event records the year and how the population changes in that year.
    // A record is useful because an event is a simple immutable data value.
    private record Event(int year, int delta) {}

    public int maximumPopulation(int[][] logs) {

        List<Event> events = new ArrayList<>(2 * logs.length);

        // Create exactly two events for every person.
        for (int[] log : logs) {
            events.add(new Event(log[0], +1)); // Birth: population increases.
            events.add(new Event(log[1], -1)); // Death: population decreases.
        }

        // Sort all events chronologically.
        events.sort(Comparator.comparingInt(Event::year));

        int population = 0;
        int maxPopulation = 0;
        int answer = 1950;

        int i = 0;

        while (i < events.size()) {
            int year = events.get(i).year();
            int netChange = 0;

            /*
             * Group every event belonging to the same year.
             *
             * This is important because multiple people may be born or die
             * in the same year. Apply their combined change before measuring
             * the population for that year.
             */
            while (i < events.size() && events.get(i).year() == year) {
                netChange += events.get(i).delta();
                i++;
            }

            // Population for 'year' is obtained after applying its net change.
            population += netChange;

            // Strict comparison preserves the earliest year in a tie.
            if (population > maxPopulation) {
                maxPopulation = population;
                answer = year;
            }
        }

        return answer;
    }
}

/*
 * TIME COMPLEXITY:
 *
 * Creating the events takes O(n).
 * Sorting 2n events takes O(n log n).
 * Sweeping and grouping events takes O(n).
 *
 * Total: O(n log n).
 *
 * SPACE COMPLEXITY:
 *
 * We store two event records per person.
 * Total auxiliary space: O(n).
 */










































import java.util.Map;
import java.util.TreeMap;

class Solution {

    public int maximumPopulation(int[][] logs) {

        /*
         * THOUGHT PROCESS:
         *
         * Instead of checking every person for every year, represent
         * each birth and death as a population-change event.
         *
         * Birth year -> +1, because the person becomes alive in that year.
         * Death year -> -1, because the person is excluded starting that year.
         *
         * A TreeMap stores years in sorted order and combines changes
         * automatically when several events occur in the same year.
         *
         * Example:
         * [1950, 1960] produces 1950 -> +1 and 1960 -> -1.
         * [1955, 1965] produces 1955 -> +1 and 1965 -> -1.
         *
         * At 1955, the map's value becomes +1 for each birth in that year.
         */

        // Key = event year; value = net change in population during that year.
        // TreeMap guarantees ascending iteration order of the years.
        Map<Integer, Integer> events = new TreeMap<>();

        for (int[] log : logs) {
            int birth = log[0];
            int death = log[1];

            // A birth adds one person to the population starting in birth year.
            events.merge(birth, 1, Integer::sum);

            // A death removes one person starting in death year.
            // This respects the problem's inclusive-birth, exclusive-death rule.
            events.merge(death, -1, Integer::sum);
        }

        /*
         * SWEEP-LINE:
         *
         * Imagine moving from left to right along the timeline.
         * Between event years, nobody enters or leaves the population,
         * so the population remains constant.
         *
         * We only need to update the population at event years.
         */

        int population = 0;
        int maxPopulation = 0;

        // The constraints guarantee that the earliest possible year is 1950.
        int answer = 1950;

        for (Map.Entry<Integer, Integer> event : events.entrySet()) {
            int year = event.getKey();
            int change = event.getValue();

            // Apply ALL net changes for this year.
            // This gives the population alive during this particular year.
            population += change;

            /*
             * Update only when the population is strictly greater.
             *
             * Why not >=?
             * Events are visited in chronological order. If a later year
             * ties the maximum, keeping the existing answer guarantees
             * that we return the earliest year with maximum population.
             */
            if (population > maxPopulation) {
                maxPopulation = population;
                answer = year;
            }
        }

        return answer;
    }
}

/*
 * TIME COMPLEXITY:
 *
 * Let n = number of people and E = number of distinct event years.
 *
 * 1. Creating events:
 *    Each person creates two events, so there are at most 2n insertions.
 *    Each TreeMap insertion takes O(log E).
 *    Total: O(n log E).
 *
 * 2. Sweeping events:
 *    We visit each distinct event year once.
 *    Total: O(E).
 *
 * Overall: O(n log E + E).
 *
 * Since E <= 2n, this simplifies to O(n log n) in the general case.
 *
 * SPACE COMPLEXITY:
 *
 * The TreeMap stores at most 2n distinct event years.
 * Therefore, auxiliary space is O(E) = O(n).
 *
 * For this problem, years are bounded between 1950 and 2050, so the
 * number of distinct event years is at most 101.
 */

