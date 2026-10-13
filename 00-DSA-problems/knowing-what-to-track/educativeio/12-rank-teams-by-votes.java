/**
 * STANDALONE STUDY NOTE: RANK TEAMS BY VOTES
 * 
 * 1. CLARIFYING QUESTIONS (To ask the interviewer before coding)
 * - "Are team names strictly uppercase English letters?" (Constraints confirm `votes[i][j]` is an uppercase English letter, allowing a fixed-size `int[26][26]` vote matrix).
 * - "What happens if two teams are tied across all vote positions (1st through Nth)? How should alphabetical order break the tie?" (The prompt explicitly specifies sorting them alphabetically by team name as the final tie-breaker).
 * - "Are all voters guaranteed to rank the exact same set of teams?" (Constraints confirm all characters in `votes[0]` are found in every other vote string).
 * - "What should be returned?" (A single string containing the concatenated sorted team characters, e.g., "ACB").
 * 
 * =================================================================================================
 * 
 * 2. THE REASONING JOURNEY
 * 
 * [Core Requirement & Binding Constraint]
 * We need to sort a set of teams based on a multi-level voting hierarchy: primary key is number of 1st place 
 * votes (descending), secondary key is 2nd place votes (descending), and so on, with alphabetical order 
 * as the final tie-breaker. The binding constraint is building an efficient multi-criteria comparison function 
 * without manual brute-force sorting loops.
 * 
 * --- APPROACH 1: BRUTE FORCE (Direct Pairwise Simulation) ---
 * 1. What I'd naturally try: For every pair of teams, compare their vote tallies position by position from 
 *    1st place downwards. If all positions are tied, compare their alphabetical characters. Use this comparison 
 *    logic inside a custom sorting algorithm.
 * 2. Why it works: It directly implements the exact textual instructions given in the problem statement.
 * 3. Why it's too slow/costly: Re-evaluating vote position arrays during every single comparison step adds 
 *    redundant overhead.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(T log T * T) — where T is the number of teams (up to 26), and each comparison takes 
 *      up to O(T) position scans.
 *    - Space Complexity: O(T^2) or O(T) depending on how the vote table is stored.
 * 
 * --- APPROACH 2: INTERMEDIATE (HashMap Vote Matrix + Custom Sort) ---
 * 1. The Observation: We can pre-compute a vote matrix. Since team names are uppercase English letters ('A'-'Z'), 
 *    we can track how many 1st, 2nd, 3rd... place votes each team receives using a 2D table or a `HashMap<Character, int[]>`.
 * 2. How it leads to the next step: Pass 1 populates the vote tallies for each team. Pass 2 extracts the unique teams 
 *    and sorts them using a custom comparator that iterates through the vote position array from index 0 upwards, 
 *    and falls back to alphabetical order if all votes are equal.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(V * T + T log T * T) — where V is number of votes and T is number of teams (at most 26). 
 *      Since T <= 26, T log T is virtually instantaneous (26 log 26 $\approx$ 120 operations).
 *    - Space Complexity: O(T^2) — to store the 26x26 vote frequency matrix.
 * 
 * --- APPROACH 3: OPTIMAL (2D Frequency Array + Custom Comparator + StringBuilder) ---
 * 1. The Observation: Since team names are strictly uppercase English letters, there are at most 26 teams. 
 *    We can use a fixed-size `int[26][26]` vote matrix where row index represents the team (`teamChar - 'A'`) 
 *    and column index represents the ballot position (`0` for 1st place, `1` for 2nd place, etc.).
 * 2. How it leads to the next step: 
 *    - Iterate through all votes and populate the 2D matrix.
 *    - Collect the unique team characters into a character array or list.
 *    - Sort the list using a custom comparator: compare vote counts from column 0 to 25 in descending order. 
 *      If all vote counts match, sort alphabetically ascending by team character.
 *    - Convert the sorted list into a result string using `StringBuilder`.
 * 3. Which one I'd write in an interview: Approach 3 (2D Array + Custom Comparator). It is clean, perfectly aligns 
 *    with Java's sorting APIs, leverages fixed bounds, and runs in a fraction of a millisecond.
 * 4. Cost Breakdown:
 *    - Time Complexity: O(V * T + T log T) — where V is number of votes and T is number of teams. Populating the 
 *      matrix takes O(V * T) time, and sorting at most 26 teams takes O(T log T) time.
 *    - Space Complexity: O(T^2) — for the 26x26 vote matrix, which remains completely constant regardless of input scale.
 * 
 * =================================================================================================
 * 
 * 3. EDGE CASES
 * - Single voter / Single team: `votes = ["A"]` -> Returns `"A"`.
 * - Complete tie across all votes: `votes = ["ABC", "ACB"]` -> Falls back to alphabetical order ("A", "B", "C").
 * - Maximum teams (26): All letters 'A'-'Z' voted on -> Handled seamlessly by the 26x26 matrix.
 * 
 * =================================================================================================
 * 
 * 4. KEY INSIGHT & ASCII DIAGRAM
 * Multi-Level Criteria Sorting Matrix:
 * 
 * Votes: ["ABC","ACB","ABC","ACB","ACB"]
 * 
 * Vote Matrix (`votes[team][position]`):
 *      Pos 0 (1st) | Pos 1 (2nd) | Pos 2 (3rd)
 * A:       5       |      0      |      0
 * B:       0       |      3      |      2
 * C:       0       |      2      |      3
 * 
 * Comparator logic:
 * - Compare A vs B at Pos 0: A has 5, B has 0 -> A comes before B.
 * - Compare B vs C at Pos 0: Both have 0. Move to Pos 1: B has 3, C has 2 -> B comes before C.
 * 
 * Resulting Order: "ABC"
 * 
 * =================================================================================================
 * 
 * 5. DRY RUN (Optimal Approach)
 * Target: votes = ["WXYZ","XYZW"]
 * 
 * Phase 1: Populate Vote Matrix (2 teams/voters example scaled down for clarity, or imagine 4 teams W,X,Y,Z)
 *   - Voter 1 ("WXYZ"): 
 *     - W gets 1st place vote -> count['W'-'A'][0]++
 *     - X gets 2nd place vote -> count['X'-'A'][1]++
 *     - Y gets 3rd place vote -> count['Y'-'A'][2]++
 *     - Z gets 4th place vote -> count['Z'-'A'][3]++
 *   - Voter 2 ("XYZW"):
 *     - X gets 1st place vote -> count['X'-'A'][0]++
 *     - Y gets 2nd place vote -> count['Y'-'A'][1]++
 *     - Z gets 3rd place vote -> count['Z'-'A'][2]++
 *     - W gets 4th place vote -> count['W'-'A'][3]++
 * 
 * Phase 2: Collect teams present in `votes[0]` ('W', 'X', 'Y', 'Z') and sort using comparator.
 * 
 * =================================================================================================
 * 
 * 6. INTERVIEW SCRIPT
 * "Since all team names are uppercase English letters, we can represent the voting results using a 26-by-26 
 * frequency matrix, where row `i` represents the team and column `j` represents the ballot position. 
 * We make a single pass over the votes array to populate this matrix. Then, we extract the competing teams 
 * from the first vote string and sort them using a custom comparator. The comparator checks vote counts 
 * from position 0 to 25 in descending order. If all position counts are tied, it falls back to alphabetical 
 * order ascending. Finally, we join the sorted characters into a string and return it. This runs in O(V * T) 
 * time and O(1) space."
 * 
 * =================================================================================================
 * 
 * 7. FOLLOW-UPS
 * Q: What if team names could be multi-character strings (e.g., "TEAM_A", "TEAM_B") instead of single letters?
 * A: The fixed 26x26 array would fail. We would switch to a `HashMap<String, int[]>` where the key is the team 
 *    string name and the value is its position vote array, while keeping the identical sorting logic.
 * 
 * Q: How would you handle missing votes if some teams were not ranked by certain voters?
 * A: If teams are optional per voter, we would dynamically discover all unique team names across all votes 
 *    into a set first, initialize their vote arrays accordingly, and process each ballot dynamically.
 */

import java.util.Arrays;

public class RankTeamsByVotes {

    /**
     * APPROACH 3: OPTIMAL (2D Vote Frequency Matrix + Custom Comparator)
     * Time: O(V * T + T log T), Space: O(T^2) where T is number of teams (at most 26)
     */
    public String rankTeams(String[] votes) {
        // [Why needed]: Edge case check for empty votes.
        if (votes == null || votes.length == 0) {
            return "";
        }

        int numTeams = votes[0].length();

        // [Why needed]: 26x26 grid. 
        // Row index: team character ('A' -> 0, 'B' -> 1, etc.)
        // Column index: ballot position (0 for 1st place, 1 for 2nd place, up to numTeams-1)
        // Value: number of votes received at that specific position.
        int[][] voteMatrix = new int[26][26];

        // Phase 1: Tally votes across all ballots
        for (String vote : votes) {
            for (int pos = 0; pos < vote.length(); pos++) {
                char team = vote.charAt(pos);
                voteMatrix[team - 'A'][pos]++;
            }
        }

        // [Why needed]: Extract the teams from the first ballot to create our sortable list.
        Character[] teams = new Character[numTeams];
        for (int i = 0; i < numTeams; i++) {
            teams[i] = votes[0].charAt(i);
        }

        // Phase 2: Sort teams using multi-criteria custom comparator
        Arrays.sort(teams, (a, b) => {
            int teamAIndex = a - 'A';
            int teamBIndex = b - 'A';

            // Compare vote counts position by position from 1st place (col 0) downwards
            for (int pos = 0; pos < numTeams; pos++) {
                int votesA = voteMatrix[teamAIndex][pos];
                int votesB = voteMatrix[teamBIndex][pos];

                // Primary / Secondary Rules: Higher votes come first (Descending order)
                if (votesA != votesB) {
                    return Integer.compare(votesB, votesA);
                }
            }

            // Tie-Breaker Rule: If all vote positions are identical, sort alphabetically ascending by team name
            return Character.compare(a, b);
        });

        // Phase 3: Build the final result string from the sorted array
        StringBuilder sb = new StringBuilder();
        for (char team : teams) {
            sb.append(team);
        }

        return sb.toString();
    }

    /**
     * SUMMARY
     * 1. Core pattern: 2D Frequency Matrix with Multi-Criteria Custom Sorting.
     * 2. Key observation: Fixed uppercase alphabet bounds ('A'-'Z') allow dense 26x26 tables to track positional 
     *    votes effortlessly.
     * 3. Memorize vs Understand: Understand how to implement sequential tie-breakers in a custom comparator loop 
     *    (`for pos = 0 to N`).
     * 4. Most common trap: Forgetting the alphabetical fallback tie-breaker when all positional votes are equal.
     * 5. Mental trigger: "Positional ranking with tie-breakers" -> `int[26][26]` matrix + sequential comparator loop.
     */
    public static void main(String[] args) {
        RankTeamsByVotes solution = new RankTeamsByVotes();

        // Test Case 1: Standard Example
        String[] votes1 = {"ABC", "ACB", "ABC", "ACB", "ACB"};
        System.out.println("Test 1 (Standard): " + solution.rankTeams(votes1)); // Expected: "ACB"

        // Test Case 2: Complete Tie (Alphabetical Fallback)
        String[] votes2 = {"WXYZ", "XYZW"};
        System.out.println("Test 2 (Tie Fallback): " + solution.rankTeams(votes2)); // Expected: "XWYZ" (or sorted alphabetically)

        // Test Case 3: Single Voter
        String[] votes3 = {"ZMNAGUEDSJYLBOPHRQICWFXTVK"};
        System.out.println("Test 3 (Single Voter): " + solution.rankTeams(votes3)); // Expected: "ZMNAGUEDSJYLBOPHRQICWFXTVK"

        // Test Case 4: Multiple Tied Positions
        String[] votes4 = {"BCA", "CAB", "CBA", "ABC", "ACB", "BAC"};
        System.out.println("Test 4 (Complex Tiers): " + solution.rankTeams(votes4)); 
    }
}



import java.util.Arrays;

class Solution {

    public String rankTeams(String[] votes) {

        // Number of teams equals the length of each voter's ranking.
        int teamCount = votes[0].length();

        // count[team][position] = number of votes that team received
        // for the given position.
        //
        // Example:
        // count[0][0] = first-place votes received by team 'A'.
        // count[1][1] = second-place votes received by team 'B'.
        int[][] count = new int[26][teamCount];

        // Step 1: Count how many votes each team receives
        // at every ranking position.
        for (String vote : votes) {
            for (int position = 0; position < teamCount; position++) {

                char team = vote.charAt(position);

                // Convert uppercase letter to an array index:
                // 'A' -> 0, 'B' -> 1, ..., 'Z' -> 25.
                count[team - 'A'][position]++;
            }
        }

        // Step 2: Collect the teams that participate.
        Character[] teams = new Character[teamCount];

        for (int i = 0; i < teamCount; i++) {
            teams[i] = votes[0].charAt(i);
        }

        // Step 3: Sort using the problem's ranking rules.
        Arrays.sort(teams, (teamA, teamB) -> {

            // Compare their votes from the highest-priority
            // position to the lowest-priority position.
            for (int position = 0; position < teamCount; position++) {

                int votesA = count[teamA - 'A'][position];
                int votesB = count[teamB - 'A'][position];

                // More votes at the current position means
                // the team must appear earlier in the result.
                if (votesA != votesB) {
                    return Integer.compare(votesB, votesA);
                }
            }

            // All position counts are equal.
            // Resolve the remaining tie alphabetically.
            return Character.compare(teamA, teamB);
        });

        // Step 4: Build the final ranking.
        StringBuilder result = new StringBuilder(teamCount);

        for (char team : toCharArray(teams)) {
            result.append(team);
        }

        return result.toString();
    }

    // Convert Character[] to char[] so the final result can
    // be constructed using a primitive-character loop.
    private char[] toCharArray(Character[] teams) {
        char[] result = new char[teams.length];

        for (int i = 0; i < teams.length; i++) {
            result[i] = teams[i];
        }

        return result;
    }
}



/*
===============================================================================
PROBLEM: Rank Teams by Votes
===============================================================================

1. INTUITION
------------
Every voter ranks all teams from best to worst.

The final ranking follows these priorities:
    1. Maximum number of first-place votes.
    2. If tied, maximum number of second-place votes.
    3. If still tied, maximum number of third-place votes.
    4. Continue position by position until a difference is found.
    5. If every position is tied, choose the team with the
       lexicographically smaller (alphabetically earlier) name.

IMPORTANT:
We must NOT calculate the total number of votes for a team.
A first-place vote has higher priority than any number of
lower-position votes.

For example:
    Team A = [5, 0, 0]
    Team B = [0, 2, 3]
    Team C = [0, 3, 2]

A ranks above both B and C because A has 5 first-place votes.
Between B and C, the first-place counts are tied (0 vs. 0).
Their second-place counts differ (2 vs. 3), so C ranks above B.

Final ranking: ACB


2. HOW TO THINK ABOUT THE SOLUTION
----------------------------------
Ask yourself:
"What information do I need to compare any two teams?"

We need the number of votes each team receives at EVERY position.

Therefore, maintain a frequency matrix:

    count[team][position]

where:
    count[team][position] =
        number of voters who ranked that team at that position.

Since teams are uppercase English letters, we can map each team
to an array index:

    'A' -> 0, 'B' -> 1, ..., 'Z' -> 25

For example:
    count['A' - 'A'][0] = first-place votes for A.
    count['B' - 'A'][1] = second-place votes for B.

This preprocessing allows us to compare teams without scanning
the original votes repeatedly.


3. ALGORITHM / STEP-BY-STEP APPROACH
------------------------------------

STEP 1: COUNT THE VOTES
-----------------------
For every voter's ranking string, visit every position.

If a voter gives ranking "ACB":
    A receives one first-place vote.
    C receives one second-place vote.
    B receives one third-place vote.

Increment the appropriate entry in the frequency matrix.

Time: O(V * T)


STEP 2: COLLECT THE TEAMS
-------------------------
All voters rank the same set of teams.

Therefore, use votes[0] to collect the teams that must be sorted.

Here:
    V = number of voters
    T = number of teams


STEP 3: SORT USING A CUSTOM COMPARATOR
--------------------------------------
Compare two teams, teamA and teamB.

For each position, starting from first place:

    aVotes = count[teamA][position]
    bVotes = count[teamB][position]

Case 1: aVotes > bVotes
    teamA must appear earlier.

Case 2: aVotes < bVotes
    teamB must appear earlier.

Case 3: aVotes == bVotes
    This position cannot decide the ranking.
    Continue to the next position.

If every position has equal counts:
    Compare team names alphabetically.

In Java, this expression sorts vote counts in descending order:

    Integer.compare(bVotes, aVotes)

Why reverse the arguments?
    Integer.compare(x, y) places x before y when x < y.
    Reversing the arguments makes the larger vote count come first.

The comparison stops at the FIRST position where counts differ.
Lower-priority positions must not override a higher-priority result.


STEP 4: BUILD THE ANSWER
------------------------
After sorting, append the teams to a StringBuilder.

The resulting string is the final ranking.


4. EXAMPLE WALKTHROUGH
----------------------
Input:
    votes = ["ABC", "ACB", "ABC", "ACB", "ACB"]

Initially, all vote counts are zero.

Process "ABC":
    A gets a first-place vote.
    B gets a second-place vote.
    C gets a third-place vote.

Process "ACB":
    A gets a first-place vote.
    C gets a second-place vote.
    B gets a third-place vote.

After processing all five voters:

    Team    1st place    2nd place    3rd place
    A           5            0            0
    B           0            2            3
    C           0            3            2

Comparison: A vs. B
    First place: 5 vs. 0
    A wins immediately.

Comparison: B vs. C
    First place: 0 vs. 0 -> Tie; continue.
    Second place: 2 vs. 3 -> C wins immediately.

Sorted teams:
    A, C, B

Output:
    "ACB"


5. TIME COMPLEXITY ANALYSIS
---------------------------

Let:
    V = number of voters
    T = number of teams
    T <= 26

A. COUNTING VOTES
-----------------
There are V voters, and each voter ranks T teams.

We visit every character exactly once.

Time complexity: O(V * T)


B. COLLECTING TEAMS
-------------------
We copy T team characters into an array.

Time complexity: O(T)


C. SORTING TEAMS
----------------
Comparison-based sorting performs O(T log T) comparisons
in the usual worst-case asymptotic bound for object-array sorting.

Each comparison may inspect all T positions.

Therefore:
    Number of comparisons = O(T log T)
    Cost per comparison  = O(T)

Sorting time: O(T^2 log T)


D. BUILDING THE RESULT
----------------------
We append each of the T team characters once.

Time complexity: O(T)


TOTAL TIME COMPLEXITY
---------------------
O(V*T + T^2*log(T) + T)

Simplifying:
    O(V*T + T^2*log(T))

Because T <= 26, the number of teams is bounded by a small
constant in this problem. Consequently, the vote-counting pass
dominates as the number of voters grows.

With the fixed 26-team alphabet, the total time can also be
expressed as O(V) with respect to V alone.


6. SPACE COMPLEXITY ANALYSIS
----------------------------

A. VOTE-COUNT MATRIX
--------------------
The matrix has 26 rows and T columns.

Space = O(26*T) = O(T), because 26 is a fixed alphabet size.

B. TEAM ARRAY
-------------
The sorting array stores T team references.

Space = O(T).

C. RESULT STRING
----------------
StringBuilder stores the final T-character result.

Space = O(T).

D. SORTING TEMPORARY SPACE
--------------------------
Java's object-array sorting may allocate temporary storage
proportional to T.

Space = O(T).

TOTAL AUXILIARY SPACE
---------------------
O(T), excluding the input array.

Including the output string, total additional storage remains O(T).

Because the alphabet size is fixed at 26, this space is bounded
by a small constant for this particular problem.


7. WHY THIS SOLUTION IS CORRECT
-------------------------------
The frequency matrix records the exact number of votes each
team receives at every position.

The comparator examines positions in the exact priority order
specified by the problem. At the first differing position,
the team with more votes is ranked higher.

If no position differs, the comparator uses alphabetical order,
which is precisely the required final tie-breaker.

Therefore, sorting the teams with this comparator produces
the required ranking.


8. INTERVIEW TAKEAWAYS
----------------------
1. Convert ranking rules into a custom comparator.
2. Precompute frequencies instead of repeatedly scanning votes.
3. Compare criteria in priority order and stop at the first
   difference.
4. Reverse comparator arguments to sort vote counts descending.
5. Always implement the final tie-breaker explicitly.

EASY WAY TO REMEMBER:
    Count -> Compare position by position -> Alphabetical tie-break
===============================================================================
*/




/*
================================================================================
PROBLEM: Rank Teams by Votes
================================================================================

INTUITION — HOW TO THINK ABOUT THE PROBLEM
------------------------------------------

Every voter ranks all teams from best to worst.

The final ranking follows these rules:
1. The team with more first-place votes ranks higher.
2. If tied, compare second-place votes.
3. If still tied, compare third-place votes.
4. Continue until a position breaks the tie.
5. If every position is tied, the alphabetically earlier team wins.

IMPORTANT OBSERVATION:
We cannot simply calculate the total votes received by each team.
The position of a vote matters!

For example:

    Team A: [5, 0, 0]
    Team B: [0, 2, 3]
    Team C: [0, 3, 2]

A ranks first because it has five first-place votes.

B and C are tied on first-place votes.
C ranks above B because C has three second-place votes,
while B has only two.

Final ranking: ACB


THE CORE IDEA
-------------

We solve the problem in two major phases:

PHASE 1: COUNT VOTES
    Build a matrix that stores how many votes each team received
    at every position.

PHASE 2: BUILD THE FINAL RANKING
    Instead of using a sorting algorithm, repeatedly find the
    highest-ranked team that has not yet been selected.

    1. Find the best remaining team.
    2. Append it to the answer.
    3. Mark it as selected.
    4. Repeat until every team has been ranked.

This is similar to selection sort, but we directly construct
the final answer instead of rearranging an array.


================================================================================
DETAILED CODE EXPLANATION
================================================================================

class Solution {

    public String rankTeams(String[] votes) {

        int teamCount = votes[0].length();

        WHY?
        ----
        Each string represents one voter's complete ranking.
        The problem guarantees that every voter ranks the same teams.

        Therefore, the length of the first string gives us the
        total number of teams.

        Example:
            votes = ["ABC", "ACB", "ABC"]
            teamCount = 3


        int[][] count = new int[26][teamCount];

        WHY A 2D ARRAY?
        ---------------
        There are 26 possible uppercase English letters.

        count[team][position] stores the number of votes received
        by a team at a particular ranking position.

        Team letters are converted into array indexes:
            'A' - 'A' = 0
            'B' - 'A' = 1
            'C' - 'A' = 2

        Position indexes are zero-based:
            0 = first place
            1 = second place
            2 = third place

        Example:
            count[0][0] = first-place votes for A
            count[1][1] = second-place votes for B
            count[2][2] = third-place votes for C

        Initially, every count is zero.


        ------------------------------------------------------------------------
        STEP 1: COUNT ALL VOTES
        ------------------------------------------------------------------------

        for (String vote : votes) {

            WHY?
            ----
            Process each voter's ranking string exactly once.

            Example:
                vote = "ACB"
        }


        for (int position = 0; position < teamCount; position++) {

            WHY?
            ----
            Visit every ranking position in the current voter's string.

            For "ACB":
                position = 0 -> A is ranked first
                position = 1 -> C is ranked second
                position = 2 -> B is ranked third
        }


        char team = vote.charAt(position);

        WHY?
        ----
        Read the team occupying the current position.

        For vote = "ACB" and position = 1:
            team = 'C'
        }


        count[team - 'A'][position]++;

        WHY?
        ----
        Convert the team character into its array index and
        increment its count at the current position.

        For team = 'C' and position = 1:
            count['C' - 'A'][1]++
            count[2][1]++

        This records one additional second-place vote for C.

        After processing every voter, the matrix contains
        the complete voting statistics.


        ------------------------------------------------------------------------
        STEP 2: TRACK TEAMS ALREADY SELECTED
        ------------------------------------------------------------------------

        boolean[] selected = new boolean[26];

        WHY?
        ----
        We are constructing the answer one team at a time.

        Once a team is added to the answer, we must not select
        that team again.

        selected[team] is:
            false -> team has not been selected
            true  -> team is already in the answer

        Initially, all values are false.


        StringBuilder answer = new StringBuilder();

        WHY?
        ----
        We build the final ranking character by character.

        StringBuilder is appropriate because we repeatedly append
        characters without creating a new String each time.


        ------------------------------------------------------------------------
        STEP 3: SELECT THE BEST REMAINING TEAM
        ------------------------------------------------------------------------

        for (int rank = 0; rank < teamCount; rank++) {

            WHY?
            ----
            The answer needs exactly teamCount characters.

            Each iteration determines one final ranking position:

                rank = 0 -> first-ranked team
                rank = 1 -> second-ranked team
                rank = 2 -> third-ranked team

            We repeat this process until every team has been placed.
        }


        int bestTeam = -1;

        WHY?
        ----
        We need a variable to remember the best team found so far.

        -1 means that no candidate has been selected yet.

        As we inspect eligible teams, the first one becomes the
        initial candidate. Better candidates can replace it.


        for (int team = 0; team < 26; team++) {

            WHY?
            ----
            Examine every possible uppercase English letter.

            However, not every letter necessarily participates in
            the competition, so we must ignore nonexistent teams.
        }


        if (votes[0].indexOf((char) ('A' + team)) == -1) {
            continue;
        }

        WHY?
        ----
        Convert the array index back to a character:

            team = 0 -> 'A'
            team = 1 -> 'B'
            team = 2 -> 'C'

        indexOf() checks whether this character appears in the
        first voter's ranking.

        If it does not appear, this letter is not a participating
        team, so skip it.

        Example:
            If the teams are A, B, and C, we must not consider D
            even though the count matrix has a row for D.
        }


        if (selected[team]) {
            continue;
        }

        WHY?
        ----
        A team already added to the answer cannot be selected again.

        For example, after selecting A:
            selected[0] = true

        Future iterations skip A.
        }


        if (bestTeam == -1 ||
            isBetter(team, bestTeam, count, teamCount)) {
            bestTeam = team;
        }

        WHY?
        ----
        There are two possibilities.

        CASE 1: bestTeam == -1
            No eligible candidate has been examined yet.
            Make the current team the initial candidate.

        CASE 2: isBetter(...) returns true
            The current team ranks higher than the previous candidate.
            Replace the previous candidate.

        After examining all eligible teams, bestTeam holds the
        highest-ranked team that has not already been selected.


        ------------------------------------------------------------------------
        STEP 4: APPEND THE WINNER
        ------------------------------------------------------------------------

        answer.append((char) ('A' + bestTeam));

        WHY?
        ----
        Convert the winning team's array index back to its character
        and append it to the answer.

        Example:
            bestTeam = 2
            ('A' + 2) = 'C'

            answer.append('C')
        }


        selected[bestTeam] = true;

        WHY?
        ----
        Mark the chosen team as selected so that it cannot
        appear in any later ranking position.

        The algorithm can now search for the next-best team.


        return answer.toString();

        WHY?
        ----
        Once all teams have been selected, return the completed
        ranking as a String.
    }


    =============================================================================
    HELPER METHOD: isBetter()
    =============================================================================

    private boolean isBetter(
            int teamA, int teamB, int[][] count, int teamCount) {

        PURPOSE:
        -------
        Return true if teamA must appear before teamB.

        The method implements the exact ranking rules from the problem.


        for (int position = 0; position < teamCount; position++) {

            WHY?
            ----
            Compare the teams starting from the most important position.

            First-place votes have the highest priority, followed by
            second-place votes, third-place votes, and so on.
        }


        int votesA = count[teamA][position];
        int votesB = count[teamB][position];

        WHY?
        ----
        Read both teams' vote counts at the current position.

        These values can be compared directly because both teams
        have the same ranking positions.
        }


        if (votesA != votesB) {
            return votesA > votesB;
        }

        WHY?
        ----
        If the counts differ, the team with more votes at this
        position ranks higher.

        Return immediately because lower-priority positions
        cannot override this decision.

        If the counts are equal, the if condition is false.
        The loop continues to the next position.
        }


        return teamA < teamB;

        WHY?
        ----
        If the loop finishes, the teams have identical vote counts
        at every ranking position.

        Their only remaining tie-breaker is alphabetical order.

        The integer index of A is smaller than B, the index of B
        is smaller than C, and so on.

        Therefore:
            teamA < teamB
        correctly puts the alphabetically earlier team first.
    }
}


================================================================================
COMPLETE DRY RUN
================================================================================

INPUT:
    votes = ["ABC", "ACB", "ABC", "ACB", "ACB"]

PHASE 1: COUNT THE VOTES

    Team A: [5, 0, 0]
    Team B: [0, 2, 3]
    Team C: [0, 3, 2]


PHASE 2: CONSTRUCT THE ANSWER

ROUND 1: FIND THE FIRST-RANKED TEAM
-----------------------------------

Initially:
    selected = {false, false, false, ...}
    answer = ""

Candidates:
    A -> [5, 0, 0]
    B -> [0, 2, 3]
    C -> [0, 3, 2]

A has the most first-place votes.

Select A:
    answer = "A"
    selected[A] = true


ROUND 2: FIND THE SECOND-RANKED TEAM
------------------------------------

A is skipped because it is already selected.

Remaining candidates:
    B -> [0, 2, 3]
    C -> [0, 3, 2]

Compare B and C:

    First place:  B = 0, C = 0 -> Tie.
    Second place: B = 2, C = 3 -> C wins.

Select C:
    answer = "AC"
    selected[C] = true


ROUND 3: FIND THE THIRD-RANKED TEAM
-----------------------------------

A is already selected -> skip.
B is not selected -> candidate.
C is already selected -> skip.

Select B:
    answer = "ACB"
    selected[B] = true


FINAL OUTPUT:
    "ACB"


================================================================================
DETAILED TIME COMPLEXITY ANALYSIS
================================================================================

Let:
    V = number of voters
    T = number of teams
    T <= 26

1. COUNTING VOTES
-----------------
The outer loop processes V vote strings.
The inner loop processes T characters in each string.

Total operations are proportional to V * T.

Time: O(V * T)


2. INITIALIZING THE DATA STRUCTURES
-----------------------------------
The count matrix has 26 * T integer entries.
Initializing it takes O(26 * T) = O(T) time because 26 is fixed.

The selected array contains 26 boolean entries, so initialization
takes O(1) time under the fixed-alphabet assumption.

These costs do not change the overall time bound.


3. CHECKING WHETHER A TEAM EXISTS
---------------------------------
votes[0].indexOf(candidate) scans at most T characters.

This check is inside the team-scanning loop, so one scan of all
26 possible letters can cost O(26 * T) = O(T) per ranking round.

There are T ranking rounds.

Total: O(T^2)


4. FINDING THE BEST REMAINING TEAM
----------------------------------
For each of the T ranking rounds, we inspect up to 26 letters.

For every eligible candidate, isBetter() compares at most T positions.

Therefore, the worst-case cost is:

    O(T rounds * 26 candidates * T positions)
    = O(26 * T^2)
    = O(T^2)

The factor 26 disappears because the alphabet size is fixed.


5. BUILDING THE ANSWER
----------------------
We append exactly T characters to StringBuilder.

Total time: O(T)


TOTAL TIME COMPLEXITY
---------------------
Combining all operations:

    O(V*T) + O(T^2) + O(T)

Therefore:

    O(V*T + T^2)

This is the general expression when treating T as a variable,
with a fixed 26-letter alphabet.

Since T <= 26, the ranking phase has a small constant upper bound.
With respect to the number of voters alone, the overall time is O(V).


================================================================================
DETAILED SPACE COMPLEXITY ANALYSIS
================================================================================

1. COUNT MATRIX
---------------
int[26][T] stores vote counts for every possible team and position.

Space: O(26*T) = O(T), because 26 is fixed.


2. SELECTED ARRAY
-----------------
boolean[26] records whether each team has been selected.

Space: O(1), because the alphabet size is fixed.


3. ANSWER
---------
StringBuilder stores T characters.

Space: O(T).


4. HELPER METHOD
----------------
isBetter() uses only a fixed number of local integer and boolean
variables. It does not create additional arrays.

Auxiliary space per invocation: O(1).


TOTAL AUXILIARY SPACE
---------------------
The count matrix and answer dominate the storage.

Space: O(T), excluding the input array.

Including the output string, total additional storage is still O(T).


================================================================================
WHY THE ALGORITHM IS CORRECT
================================================================================

At every ranking round, we examine all teams that have not yet
been selected.

The isBetter() method compares teams according to the exact
priority rules: first-place votes, second-place votes, and so on,
followed by alphabetical order.

Therefore, bestTeam is always the highest-ranked remaining team.

We append that team and remove it from future consideration.

Repeating this process T times selects every team exactly once
and places each team in its correct position.

Hence, the algorithm returns the required ranking.


INTERVIEW MEMORY TRICK
----------------------

COUNT -> FIND BEST -> APPEND -> MARK SELECTED -> REPEAT

The helper method answers one question:
"Between these two teams, which one should rank higher?"

The outer loop answers another:
"Which team should occupy the next position in the final ranking?"

Keeping these two responsibilities separate makes the solution
much easier to understand and explain.
================================================================================
*/

class Solution {

    public String rankTeams(String[] votes) {
        int teamCount = votes[0].length();

        // count[team][position] = number of votes received at that position.
        int[][] count = new int[26][teamCount];

        // Step 1: Count votes at every position.
        for (String vote : votes) {
            for (int position = 0; position < teamCount; position++) {
                char team = vote.charAt(position);
                count[team - 'A'][position]++;
            }
        }

        // Tracks whether each team has already been added to the answer.
        boolean[] selected = new boolean[26];

        StringBuilder answer = new StringBuilder(teamCount);

        // Step 2: Select the best remaining team for each final rank.
        for (int rank = 0; rank < teamCount; rank++) {
            int bestTeam = -1;

            // Examine all possible teams.
            for (int team = 0; team < 26; team++) {

                // Skip letters that are not part of this competition.
                if (votes[0].indexOf((char) ('A' + team)) == -1) {
                    continue;
                }

                // Skip teams that already have a final position.
                if (selected[team]) {
                    continue;
                }

                // Keep the highest-ranked candidate seen so far.
                if (bestTeam == -1 ||
                    isBetter(team, bestTeam, count, teamCount)) {
                    bestTeam = team;
                }
            }

            // Place the winner at the current rank.
            answer.append((char) ('A' + bestTeam));

            // Prevent this team from being selected again.
            selected[bestTeam] = true;
        }

        return answer.toString();
    }

    // Returns true if teamA must rank above teamB.
    private boolean isBetter(
            int teamA, int teamB, int[][] count, int teamCount) {

        // Compare ranking positions from highest priority to lowest.
        for (int position = 0; position < teamCount; position++) {
            int votesA = count[teamA][position];
            int votesB = count[teamB][position];

            // The first differing position decides the ranking.
            if (votesA != votesB) {
                return votesA > votesB;
            }
        }

        // If all counts are tied, the alphabetically earlier team wins.
        return teamA < teamB;
    }
}


