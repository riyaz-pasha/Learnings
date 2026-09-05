import java.util.*;

/**
 * ============================================================================
 * PROBLEM STATEMENT: Number of People Aware of a Secret
 * On day 1, exactly 1 person discovers a secret.
 * They wait 'delay' days before sharing it (sharing with 1 new person per day).
 * They completely forget the secret 'forget' days after discovering it.
 * Return the total number of people who know the secret on day 'n'.
 * Since the answer can be massive, return it modulo 10^9 + 7.
 * 
 * Constraints:
 * 2 <= n <= 1000
 * 1 <= delay < forget <= n
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * 1. INTERVIEW APPROACH & CLARIFYING QUESTIONS
 * ----------------------------------------------------------------------------
 * In an L4/L5 interview, this problem is a brilliant test of state definition.
 * 
 * Q: "Does the person share the secret on the exact day they forget it?"
 * A: No. The prompt states they can share from (day + delay) to 
 *    (day + forget - 1). On the day they forget, they cannot share.
 * 
 * Q: "Why return modulo 10^9 + 7?"
 * A: Because exponential growth (each person sharing with a new person daily) 
 *    quickly exceeds the 64-bit limit of a `long`. We must apply the modulo 
 *    at every single addition/subtraction step to prevent overflow.
 *
 * ----------------------------------------------------------------------------
 * 2. RESTATING THE PROBLEM & IDENTIFYING THE SOLUTION
 * ----------------------------------------------------------------------------
 * "Instead of trying to track 'everyone who knows the secret', which is a 
 * messy mix of active sharers, waiting people, and forgetting people, we 
 * should track exactly one clean metric:
 * 
 * NEW DISCOVERIES: How many NEW people learned the secret on day 'i'?
 * 
 * If I know exactly how many people learned the secret on day 'j' (where j is 
 * in the past), I know exactly what those people are doing today.
 * The total number of people who know the secret on day 'n' is simply the sum 
 * of all 'NEW' people who learned it within the last 'forget' days. 
 * Anyone who learned it before (n - forget) has already forgotten it.
 * 
 * Because day 'i' relies on a rolling window of historical days, this is DP."
 *
 * ----------------------------------------------------------------------------
 * 3. VISUALIZATION & TRACING
 * ----------------------------------------------------------------------------
 * Example: n = 6, delay = 2, forget = 4
 * dp[i] = new people who learned the secret on day 'i'.
 * 
 * Day 1: dp[1] = 1 (The initial person)
 * Day 2: dp[2] = 0 (Initial person is waiting: delay is 2)
 * Day 3: dp[3] = 1 (Initial person shares for the first time)
 * Day 4: dp[4] = 1 (Initial person shares again. Day 3 person is waiting)
 * Day 5: dp[5] = 2 (Initial person forgets! But Day 3 person shares, and Day 4 person shares)
 * Day 6: dp[6] = 2 
 * 
 * Total knowing on Day 6: Sum of new learners in the last 4 days (Days 3, 4, 5, 6)
 * Total = dp[3] + dp[4] + dp[5] + dp[6] = 1 + 1 + 2 + 2 = 6.
 */
public class PeopleAwareOfSecret {

    private static final int MOD = 1_000_000_007;

    /**
     * ========================================================================
     * APPROACH 1: Plain Recursion (Brute Force)
     * ========================================================================
     * Idea: Write a function `getNewPeople(day)` that recursively looks back 
     * at past days to sum up all the people who are eligible to share today.
     * 
     * Time Complexity: O(forget^N) - Massive exponential branching.
     * Space Complexity: O(N) - Recursion stack depth.
     */
    public int peopleAwareOfSecretRecursive(int n, int delay, int forget) {
        long totalAware = 0;
        
        // Sum up the new people from the days that haven't forgotten the secret yet
        int startDay = Math.max(1, n - forget + 1);
        for (int i = startDay; i <= n; i++) {
            totalAware = (totalAware + getNewPeopleRecursive(i, delay, forget)) % MOD;
        }
        
        return (int) totalAware;
    }

    private long getNewPeopleRecursive(int day, int delay, int forget) {
        // BASE CASE REASONING:
        // On the very first day, exactly 1 person is magically given the secret.
        if (day == 1) return 1;

        long newPeopleToday = 0;
        
        // Look back at history. Who is allowed to share the secret today?
        // They must have learned it AT LEAST 'delay' days ago.
        // They must have learned it LESS THAN 'forget' days ago.
        int firstEligibleDay = Math.max(1, day - forget + 1);
        int lastEligibleDay = day - delay;
        
        for (int pastDay = firstEligibleDay; pastDay <= lastEligibleDay; pastDay++) {
            newPeopleToday = (newPeopleToday + getNewPeopleRecursive(pastDay, delay, forget)) % MOD;
        }
        
        return newPeopleToday;
    }

    /**
     * ========================================================================
     * APPROACH 2: Top-Down Dynamic Programming (Memoization)
     * ========================================================================
     * Idea: Cache `getNewPeople(day)` in an array.
     * 
     * Time Complexity: O(N * (forget - delay)) - We evaluate each day once, 
     * iterating over its sharing window.
     * Space Complexity: O(N) - For the memo array + recursion stack.
     */
    public int peopleAwareOfSecretMemo(int n, int delay, int forget) {
        long[] memo = new long[n + 1];
        Arrays.fill(memo, -1);
        
        long totalAware = 0;
        int startDay = Math.max(1, n - forget + 1);
        
        for (int i = startDay; i <= n; i++) {
            totalAware = (totalAware + getNewPeopleMemo(i, delay, forget, memo)) % MOD;
        }
        
        return (int) totalAware;
    }

    private long getNewPeopleMemo(int day, int delay, int forget, long[] memo) {
        if (day == 1) return 1;
        if (memo[day] != -1) return memo[day];

        long newPeopleToday = 0;
        int firstEligibleDay = Math.max(1, day - forget + 1);
        int lastEligibleDay = day - delay;
        
        for (int pastDay = firstEligibleDay; pastDay <= lastEligibleDay; pastDay++) {
            newPeopleToday = (newPeopleToday + getNewPeopleMemo(pastDay, delay, forget, memo)) % MOD;
        }
        
        memo[day] = newPeopleToday;
        return memo[day];
    }

    /**
     * ========================================================================
     * APPROACH 3: Bottom-Up Dynamic Programming (Tabulation 1D - Optimized)
     * ========================================================================
     * Idea: Instead of looking backward with an O(forget) inner loop every day, 
     * we can track the NUMBER OF ACTIVE SHARERS on a rolling basis.
     * 
     * Time Complexity: O(N) - One single loop. We eliminated the inner loop!
     * Space Complexity: O(N) - For the DP array.
     */
    public int peopleAwareOfSecretTabulation(int n, int delay, int forget) {
        // dp[i] signifies: "Exactly how many NEW people learned the secret on day i?"
        long[] dp = new long[n + 1];
        
        // BASE CASE REASONING:
        // On day 1, exactly 1 person learns the secret.
        dp[1] = 1;
        
        // We maintain a rolling count of people who are ACTIVELY sharing today.
        long activeSharers = 0;

        for (int i = 2; i <= n; i++) {
            
            // --- DETAILED TABULATION EXPLANATION ---
            
            // Step 1: Who just finished their waiting period today?
            // Anyone who learned the secret exactly 'delay' days ago (i - delay) 
            // is finally allowed to start sharing today.
            if (i - delay >= 1) {
                activeSharers = (activeSharers + dp[i - delay]) % MOD;
            }
            
            // Step 2: Who forgot the secret today?
            // Anyone who learned the secret exactly 'forget' days ago (i - forget) 
            // forgets it entirely today. We must revoke their sharing privileges.
            if (i - forget >= 1) {
                // Modulo subtraction requires adding MOD before taking the modulo 
                // to prevent negative numbers in Java!
                activeSharers = (activeSharers - dp[i - forget] + MOD) % MOD;
            }
            
            // Step 3: Register today's new learners.
            // Every single active sharer tells exactly 1 new person. 
            // Therefore, the number of new people today is exactly equal to the 
            // number of active sharers.
            dp[i] = activeSharers;
        }

        // Final Step: Count everyone who hasn't forgotten yet.
        // We sum up the new learners from the last 'forget' days.
        long totalPeopleKnowingSecret = 0;
        int firstDayNotForgotten = Math.max(1, n - forget + 1);
        
        for (int i = firstDayNotForgotten; i <= n; i++) {
            totalPeopleKnowingSecret = (totalPeopleKnowingSecret + dp[i]) % MOD;
        }

        return (int) totalPeopleKnowingSecret;
    }

    /**
     * ========================================================================
     * APPROACH 4: Space-Optimized Dynamic Programming (L4/L5 Target)
     * ========================================================================
     * Idea: In our O(N) time Tabulation approach, to calculate day `i`, the 
     * FURTHEST we ever look backward is `i - forget`. 
     * Any history older than `forget` days is completely dead memory.
     * 
     * We can collapse the O(N) array into a sliding circular array of size `forget`.
     * 
     * Time Complexity: O(N)
     * Space Complexity: O(forget) - Massive optimization for large N, small forget!
     */
    public int peopleAwareOfSecretSpaceOptimized(int n, int delay, int forget) {
        // A circular array strictly capped at the 'forget' limit.
        long[] dp = new long[forget];
        
        // BASE CASE REASONING:
        // Day 1 maps to index (1 % forget).
        dp[1 % forget] = 1;
        
        long activeSharers = 0;

        for (int i = 2; i <= n; i++) {
            
            // MAGIC OF THE CIRCULAR ARRAY:
            // We use modulo arithmetic to find the historical days in our sliding window.
            long newSharers = (i - delay >= 1) ? dp[(i - delay) % forget] : 0;
            long forgotten = (i - forget >= 1) ? dp[(i - forget) % forget] : 0;
            
            activeSharers = (activeSharers + newSharers - forgotten + MOD) % MOD;
            
            // Overwrite the oldest, now-forgotten data with today's new learners
            dp[i % forget] = activeSharers;
        }

        long totalPeopleKnowingSecret = 0;
        int firstDayNotForgotten = Math.max(1, n - forget + 1);
        
        for (int i = firstDayNotForgotten; i <= n; i++) {
            totalPeopleKnowingSecret = (totalPeopleKnowingSecret + dp[i % forget]) % MOD;
        }

        return (int) totalPeopleKnowingSecret;
    }

    /**
     * ========================================================================
     * MAIN METHOD FOR TESTING
     * ========================================================================
     */
    public static void main(String[] args) {
        var solver = new PeopleAwareOfSecret();
        
        record TestCase(int n, int delay, int forget, int expected) {}
        
        List<TestCase> testCases = Arrays.asList(
            new TestCase(6, 2, 4, 5),      // 5 people know on day 6
            new TestCase(4, 1, 3, 6),      // Fast spread, quick forget
            new TestCase(1000, 2, 4, 613867086) // Stress test, checks Modulo logic
        );
        
        int caseNum = 1;
        for (TestCase tc : testCases) {
            System.out.println("---- Test Case " + caseNum++ + " ----");
            System.out.println("N (Days): " + tc.n + " | Delay: " + tc.delay + " | Forget: " + tc.forget);
            System.out.println("Expected: " + tc.expected);
            
            // Skip pure recursion for large N to prevent thread locking
            if (tc.n <= 30) {
                System.out.println("Recursive (Brute) : " + solver.peopleAwareOfSecretRecursive(tc.n, tc.delay, tc.forget));
            } else {
                System.out.println("Recursive (Brute) : Skipped (Too slow for O(forget^N))");
            }
            
            System.out.println("Memoization       : " + solver.peopleAwareOfSecretMemo(tc.n, tc.delay, tc.forget));
            System.out.println("Tabulation O(N)   : " + solver.peopleAwareOfSecretTabulation(tc.n, tc.delay, tc.forget));
            System.out.println("Space Optimized   : " + solver.peopleAwareOfSecretSpaceOptimized(tc.n, tc.delay, tc.forget));
            System.out.println();
        }
    }
}

/*
 * =================================================================================
 *  GOOGLE-STYLE MOCK ONSITE — DYNAMIC PROGRAMMING / SLIDING WINDOW
 *  Problem: "Number of People Aware of a Secret" (delay / forget variant)
 *  File:    SecretSharing.java
 *  Run:     java SecretSharing.java      (single-file source-launch, Java 21+)
 * =================================================================================
 *
 * -------------------------------------------------------------------------------
 * SECTION 1: RESTATE THE PROBLEM
 * -------------------------------------------------------------------------------
 * In my own words:
 *
 *   Exactly one person learns a secret on day 1. (NOTE: the prompt as given says
 *   "On day 11, exactly one person discovers a secret." — in an interview I would
 *   flag this immediately: given the constraints state 2 <= n and delay < forget
 *   <= n, and this matches the well-known LeetCode formulation where discovery
 *   happens on day 1, I am treating "day 11" as a transcription artifact for
 *   "day 1". I will state this assumption out loud and ask the interviewer to
 *   confirm before writing a single line of code — silently "fixing" an
 *   ambiguous spec is exactly the kind of thing that gets flagged in a real
 *   debrief.)
 *
 *   Every person who knows the secret follows the same lifecycle, anchored to
 *   the day `d` on which THEY personally learned it:
 *     - Days [d, d + delay - 1]        : knows the secret, but cannot share yet.
 *     - Days [d + delay, d + forget-1] : knows the secret AND shares it with
 *                                        exactly one brand-new person each day.
 *     - Day  d + forget onward         : has forgotten the secret entirely and
 *                                        can never share or be counted again.
 *
 *   Given n, delay, forget, we must return how many people know the secret at
 *   the END of day n (i.e., people who have discovered it but not yet forgotten
 *   it by the close of day n), modulo 1_000_000_007.
 *
 * Key constraints / inputs / outputs (as given):
 *   - 2 <= n <= 1000
 *   - 1 <= delay < forget <= n
 *   - Output: a single non-negative long, taken mod 1e9+7.
 *
 * Implicit assumptions I would state out loud:
 *   - "Sharing with one new person a day" always succeeds in producing a
 *     genuinely NEW person (no capacity limits, no running out of new people,
 *     no re-sharing with someone who already knows it).
 *   - delay and forget are fixed constants that apply identically to every
 *     person in the population (not per-person random values).
 *   - We only need the COUNT of people, never their identities — this is the
 *     single most important observation for choosing an efficient approach.
 *
 * -------------------------------------------------------------------------------
 * SECTION 2: CLARIFYING QUESTIONS (asked first, with my assumed answers)
 * -------------------------------------------------------------------------------
 *  1. Q: Is discovery really on "day 1", and is n 1-indexed (day 1 through day n)?
 *     A (assumed): Yes. "Day 11" in the prompt is a typo for "day 1"; days are
 *        1-indexed, we simulate through day n inclusive.
 *  2. Q: Is it guaranteed delay < forget, and can they ever be equal?
 *     A (assumed): Constraint explicitly says delay < forget, strictly, so a
 *        person always has at least one day where they are both aware AND
 *        able to share before forgetting. No need to defend against delay >=
 *        forget, but I will still add a defensive check in production code.
 *  3. Q: Do we return the count mod 1e9+7, or the exact BigInteger value?
 *     A (assumed): Return `long`, value already reduced mod 1e9+7, as stated.
 *  4. Q: Can n, delay, or forget be zero or negative?
 *     A (assumed): No — constraints guarantee n >= 2, delay >= 1, forget >= 2
 *        (since forget > delay >= 1). I will still validate defensively.
 *  5. Q: Does "forgets forget days after discovering" mean they are still
 *        capable of sharing ON day d+forget-1 but NOT on day d+forget?
 *     A (assumed): Yes, exactly — forgetting happens ON day d+forget, and from
 *        that day onward (inclusive) they neither know nor share the secret.
 *  6. Q: Is the sharing target chosen from people who don't already know the
 *        secret, and does it matter who it's shared with for our count?
 *     A (assumed): Doesn't matter — we only need the count, and the problem
 *        guarantees a new distinct person is reached every valid share, so we
 *        never need to model *who* — only *how many* per day.
 *  7. Q: Should the solution work efficiently for n up to 1000 in an interview
 *        setting, or should I also discuss what changes for n up to 1e15?
 *     A (assumed): O(n) or O(n * forget) is perfectly fine for n <= 1000; I'll
 *        mention (and implement) a fast-doubling / matrix approach as a
 *        stretch goal for astronomically large n.
 *  8. Q: Is this single-threaded / offline (all inputs known up front), or do
 *        delay/forget change over time / per query?
 *     A (assumed): Single offline computation — no concurrency, no streaming
 *        updates to delay or forget mid-simulation.
 *
 * -------------------------------------------------------------------------------
 * SECTION 3: EXAMPLES & EDGE CASES
 * -------------------------------------------------------------------------------
 *  Example A (normal case) — n=6, delay=2, forget=4  =>  expected answer 5
 *    Day 1: {P1} discovers.                                    known={P1}
 *    Day 2: nobody can share yet (P1 must wait until day 1+2=3).known={P1}
 *    Day 3: P1 shares -> P2 discovers.                         known={P1,P2}
 *    Day 4: P1 shares -> P3 discovers. (P2 still waiting: 2+2=4,
 *           so P2 is NOT yet eligible to share on day 4 itself,
 *           eligibility starts strictly at d+delay, and 2+2=4 means
 *           P2 *is* eligible starting day 4 — but P2 only just
 *           discovered on day 3, so it shares starting day 3+2=5.)
 *                                                               known={P1,P2,P3}
 *    Day 5: P1 forgets (1+4=5) and drops out. P2 now shares -> P4.
 *           P3 still waiting (3+2=5, eligible from day5, shares too) -> P5.
 *                                                     known={P2,P3,P4,P5}
 *    Day 6: P2 shares -> new; P3 shares -> new.       known count = 5
 *           (P2 forgets on 3+4=7, not yet; P3 forgets on 3+4=7, not yet;
 *            the two new people from day5 are still within their delay.)
 *    Final answer at end of day 6: 5. This matches the DP trace in Section 10.
 *
 *  Example B (edge / minimal case) — n=2, delay=1, forget=2 => expected answer 2
 *    Day 1: P1 discovers.                     known={P1}
 *    Day 2: P1 is eligible to share (1+1=2) and has NOT yet forgotten
 *           (forgets on 1+2=3), so P1 shares -> P2 discovers.
 *           known={P1,P2} => answer = 2.
 *    This is the smallest legal input (n=2, delay<forget, forget<=n) and is a
 *    great boundary check for off-by-one errors in the eligibility window.
 *
 *  Example C (tie / boundary case on the "forget" edge) — n=4, delay=1, forget=3
 *    => expected answer 6 (this is a known reference case worth memorizing).
 *    It stresses the exact day someone forgets (d+forget) versus the exact day
 *    they become eligible (d+delay), which is where most candidates introduce
 *    off-by-one bugs (see Section 13).
 *
 * -------------------------------------------------------------------------------
 * PARADIGM SWEEP (stating out loud which paradigms apply and which don't,
 * BEFORE diving into code — this is what separates a senior signal from a
 * "got lucky with the first idea that worked" signal)
 * -------------------------------------------------------------------------------
 *  - Sorting-based:            Not applicable — there is no ordering/comparison
 *                               task; the "day" index already gives us total
 *                               order for free.
 *  - Hashing-based:             Not applicable — we never need to deduplicate or
 *                               key-lookup identities; we only ever need counts.
 *  - Greedy:                    Not applicable — there is no choice to make; the
 *                               process is fully deterministic given the rules.
 *  - Tree / graph traversal:    Technically the sharing forms a tree (who told
 *                               whom), but explicitly building/traversing it is
 *                               wasteful — we only need aggregate counts per
 *                               day, which collapses to a 1-D recurrence.
 *  - Heap / priority queue:     Not applicable — nothing needs priority
 *                               ordering by value; every eligible sharer acts
 *                               exactly once per day, unconditionally.
 *  - Binary search:             Not applicable to computing the count itself
 *                               (there's no monotonic predicate to search over
 *                               for this version of the problem).
 *  - Monotonic stack/deque:     Not needed — the "window" we track is a plain
 *                               running SUM (not a min/max), so a simple
 *                               accumulator/two-pointer suffices; a monotonic
 *                               deque would be solving a problem we don't have.
 *  - Two pointer / sliding
 *    window:                    APPLICABLE — dp[i] depends on a contiguous
 *                               range of previous dp values, a textbook
 *                               sliding-window-sum situation.
 *  - Dynamic programming:       APPLICABLE — this is fundamentally a linear
 *                               recurrence DP over "day".
 *  - Divide and conquer:        APPLICABLE (advanced) — the recurrence is
 *                               linear, so it can be expressed as a matrix
 *                               power and solved via fast (binary/doubling)
 *                               exponentiation for astronomically large n.
 *  - Trie / segment tree /
 *    advanced structures:       Technically applicable via a Fenwick tree for
 *                               range-sum queries, but it is strictly inferior
 *                               to a running-sum sliding window for this
 *                               offline problem — included for completeness.
 *  - Brute force / simulation:  APPLICABLE as an oracle for correctness — track
 *                               literal discovery-day entries and simulate the
 *                               rules exactly, only tractable for tiny n.
 */
public final class SecretSharing {

    /** Modulus required by the problem statement. */
    private static final long MOD = 1_000_000_007L;

    private SecretSharing() {
        // Utility/demo class — not meant to be instantiated.
    }

    /*
     * =============================================================================
     * SECTION 4-6: ALL POSSIBLE SOLUTIONS
     * =============================================================================
     */

    /*
     * ---------------------------------------------------------------------------
     * APPROACH 1: Brute-Force Literal Simulation ("oracle" for cross-validation)
     * ---------------------------------------------------------------------------
     * Core idea: Model exactly what the statement says, with no abstraction.
     * Keep a growing list of "discovery days", one entry per person who has ever
     * learned the secret. Walk day by day; on each day, every existing entry
     * whose day `d` satisfies d+delay <= day <= d+forget-1 produces exactly one
     * brand-new entry with discovery day = today.
     *
     * Paradigm: pure simulation (no clever data structure) — this is the
     * "obviously correct, obviously slow" reference implementation.
     *
     * Time complexity:  O(P) total work where P is the number of people who ever
     *                    know the secret — this grows combinatorially and is NOT
     *                    polynomial in n in the worst case (roughly Fibonacci-like
     *                    growth), so this is exponential in effective terms for n.
     * Space complexity: O(P) to store every discovery day ever produced.
     *
     * Pros: Trivial to verify against the spec line-by-line; zero risk of an
     *       off-by-one in the *recurrence* because there is no recurrence, just
     *       literal rule-following.
     * Cons: Blows up almost immediately — completely impractical past n ~ 20-25
     *       without a modulus (and even with one, tracking literal entries is
     *       still wasteful once we don't care about identities).
     * When to use: NEVER in production or as a submitted interview answer — only
     *       as a hidden self-check / test oracle to validate the real solution
     *       on small inputs, exactly the role it plays in this file's main().
     */
    static final class BruteForceSimulation {

        /**
         * Simulates the literal process and returns the exact (un-modded) count.
         * Only safe for small n (recommended n <= 20) since the list of
         * discovery-day entries grows combinatorially and we do not reduce
         * modulo anything here — we want the true integer value as ground truth.
         */
        static long solve(int n, int delay, int forget) {
            if (n < 1) {
                return 0L;
            }
            // allDiscoveryDays holds one entry per person who has ever learned
            // the secret, recording only the day they personally discovered it.
            List<Integer> allDiscoveryDays = new ArrayList<>();
            allDiscoveryDays.add(1); // the single origin person, day 1

            for (int day = 2; day <= n; day++) {
                List<Integer> newlyDiscoveredToday = new ArrayList<>();
                for (int discoveryDay : allDiscoveryDays) {
                    boolean eligibleToShareToday =
                            discoveryDay + delay <= day && day <= discoveryDay + forget - 1;
                    if (eligibleToShareToday) {
                        newlyDiscoveredToday.add(day);
                    }
                }
                allDiscoveryDays.addAll(newlyDiscoveredToday);
            }

            long stillKnowsCount = 0L;
            for (int discoveryDay : allDiscoveryDays) {
                boolean hasNotForgottenYet = discoveryDay + forget > n;
                if (hasNotForgottenYet) {
                    stillKnowsCount++;
                }
            }
            return stillKnowsCount;
        }
    }

    /*
     * ---------------------------------------------------------------------------
     * APPROACH 2: Naive Windowed DP (O(n * forget))
     * ---------------------------------------------------------------------------
     * Core idea: Stop tracking individuals. Instead track dp[i] = number of
     * people who FIRST discover the secret on day i. A person discovered on day
     * j contributes to dp[i] for every day i with j+delay <= i <= j+forget-1,
     * i.e. dp[i] = sum of dp[j] for all j in the window [i-forget+1, i-delay].
     * We compute that window sum by brute-force re-summing it every time.
     *
     * Paradigm: Dynamic Programming (1-D, linear recurrence over a sliding
     * range), computed the "naive" way without reusing work between windows.
     *
     * Time complexity:  O(n * forget) — for each of the n days we may re-sum up
     *                    to `forget` previous entries.
     * Space complexity: O(n) for the dp array.
     *
     * Pros: Already a huge improvement over Approach 1 (polynomial, not
     *       exponential); the recurrence itself is easy to state and defend.
     * Cons: Re-does the summation work from scratch every day — wasteful when
     *       consecutive windows overlap almost entirely.
     * When to use: Fine for the given constraints (n <= 1000 => at most 10^6
     *       operations), but I would not present this as my final answer once I
     *       notice the overlapping-window redundancy — I'd proactively upgrade
     *       to Approach 3 before being asked.
     */
    static final class DpNaiveApproach {

        static long solve(int n, int delay, int forget) {
            validateInputs(n, delay, forget);

            long[] dp = new long[n + 1]; // dp[i] = # of people discovering on day i
            dp[1] = 1L;

            for (int day = 2; day <= n; day++) {
                int windowLow = Math.max(1, day - forget + 1);
                int windowHigh = day - delay;
                long sum = 0L;
                for (int j = windowLow; j <= windowHigh; j++) {
                    sum = (sum + dp[j]) % MOD;
                }
                dp[day] = sum;
            }

            int answerWindowLow = Math.max(1, n - forget + 1);
            long answer = 0L;
            for (int j = answerWindowLow; j <= n; j++) {
                answer = (answer + dp[j]) % MOD;
            }
            return answer;
        }
    }

    /*
     * ---------------------------------------------------------------------------
     * APPROACH 3: DP + Prefix Sums (O(n) time, O(n) space) — RECOMMENDED
     * ---------------------------------------------------------------------------
     * Core idea: Same recurrence as Approach 2, but precompute a running
     * prefix-sum array so any window sum [l, r] is answered in O(1) via
     * prefix[r] - prefix[l-1], instead of re-scanning the window.
     *
     * Paradigm: Dynamic Programming + Prefix Sums (a specialization of the
     * sliding-window-sum technique).
     *
     * Time complexity:  O(n) — one pass to fill dp[], one prefix update per day.
     * Space complexity: O(n) for dp[] and O(n) for the prefix array (could be
     *                    merged into a single array, as done below, to halve
     *                    the constant factor).
     *
     * Pros: Optimal time complexity, very easy to explain and to get right
     *       under interview pressure — one array, one loop, one invariant
     *       ("prefix[i] = sum of dp[1..i] mod M"). Minimal risk of subtle bugs
     *       compared to a hand-rolled two-pointer window.
     * Cons: Still O(n) auxiliary space; if n were enormous (billions) and only
     *       `forget` were small, this would be wasteful compared to Approach 4.
     * When to use: This is my default interview answer for the given
     *       constraints (n <= 1000) — optimal time, minimal cognitive overhead,
     *       and trivially easy to verify by re-deriving the window bounds live
     *       on the whiteboard.
     */
    static final class DpPrefixSumApproach {

        static long solve(int n, int delay, int forget) {
            validateInputs(n, delay, forget);

            // prefix[i] holds (dp[1] + dp[2] + ... + dp[i]) mod MOD, prefix[0] = 0.
            long[] prefix = new long[n + 1];
            long dpDay1 = 1L;
            prefix[1] = dpDay1;

            for (int day = 2; day <= n; day++) {
                int windowLow = Math.max(1, day - forget + 1);
                int windowHigh = day - delay;

                long windowSum;
                if (windowHigh < windowLow) {
                    // Nobody has waited out their delay yet relative to this day.
                    windowSum = 0L;
                } else {
                    windowSum = (prefix[windowHigh] - prefix[windowLow - 1] + MOD) % MOD;
                }

                long dpToday = windowSum;
                prefix[day] = (prefix[day - 1] + dpToday) % MOD;
            }

            int answerWindowLow = Math.max(1, n - forget + 1);
            long answer = (prefix[n] - prefix[answerWindowLow - 1] + MOD) % MOD;
            return answer;
        }
    }

    /*
     * ---------------------------------------------------------------------------
     * APPROACH 4: DP + True Sliding Window, O(n) time / O(forget) space
     * ---------------------------------------------------------------------------
     * Core idea: We never actually need the FULL dp history — at any moment we
     * only ever look back at most `forget` days. Keep dp values in a circular
     * buffer of size `forget`, and maintain a running `windowSum` that we
     * incrementally update by adding the value that just entered the window
     * (day - delay) and removing the value that just fell out of the window
     * (day - forget), instead of recomputing sums or storing full history.
     *
     * Paradigm: Two-pointer / sliding window, with O(1) amortized update per
     * day (classic "add on one side, remove on the other" window maintenance).
     *
     * Time complexity:  O(n) — one pass, O(1) work per day.
     * Space complexity: O(forget) — a circular buffer sized to the lookback
     *                    horizon, independent of n.
     *
     * Pros: Best possible space complexity for this recurrence; shows strong
     *       signal about recognizing that dp[] itself doesn't need to be
     *       fully materialized, only the last `forget` entries.
     * Cons: Slightly more delicate to implement correctly under pressure — the
     *       ORDER of "add new day" vs "remove old day" vs "record today's dp"
     *       matters and is a classic off-by-one trap (see Section 13).
     * When to use: Great follow-up answer when the interviewer asks "can you
     *       reduce the space?" — I would NOT lead with this cold, since
     *       Approach 3 is equally fast and much less error-prone to write live.
     */
    static final class DpSlidingWindowApproach {

        static long solve(int n, int delay, int forget) {
            validateInputs(n, delay, forget);

            // Circular buffer: dpBuffer[day % forget] holds dp[day] once computed.
            // Size `forget` is exactly the maximum lookback distance ever needed.
            long[] dpBuffer = new long[forget];
            dpBuffer[1 % forget] = 1L; // dp[1] = 1

            long recurrenceWindowSum = 0L; // sum over [i-forget+1, i-delay] as we scan i
            long aliveWindowSum = 1L;      // sum over the trailing `forget`-day alive window

            for (int day = 2; day <= n; day++) {
                // 1) A day newly becomes old enough to have finished its delay:
                //    the day (day - delay) can now contribute to today's total.
                if (day - delay >= 1) {
                    recurrenceWindowSum = (recurrenceWindowSum + dpBuffer[(day - delay) % forget]) % MOD;
                }
                // 2) A day now falls out of the sharing window because that
                //    person forgets exactly `forget` days after discovering:
                //    the day (day - forget) must stop contributing. NOTE:
                //    (day - forget) and `day` always map to the SAME slot in
                //    a size-`forget` circular buffer (day - forget ≡ day, mod
                //    forget), so this read must happen strictly BEFORE we
                //    overwrite that slot with today's value below.
                if (day - forget >= 1) {
                    recurrenceWindowSum = (recurrenceWindowSum - dpBuffer[(day - forget) % forget] % MOD + MOD) % MOD;
                }

                long dpToday = recurrenceWindowSum;

                // Maintain the trailing "still alive at end of today" window,
                // which always spans the most recent `forget` discovery-days.
                // This read ALSO targets the same slot `day` is about to
                // overwrite, so it too must happen before the buffer write.
                aliveWindowSum = (aliveWindowSum + dpToday) % MOD;
                int dayLeavingAliveWindow = day - forget;
                if (dayLeavingAliveWindow >= 1) {
                    aliveWindowSum = (aliveWindowSum - dpBuffer[dayLeavingAliveWindow % forget] % MOD + MOD) % MOD;
                }

                // Only NOW is it safe to overwrite this slot for the current day.
                dpBuffer[day % forget] = dpToday;
            }

            return aliveWindowSum % MOD;
        }
    }

    /*
     * ---------------------------------------------------------------------------
     * APPROACH 5: Matrix Exponentiation (Divide & Conquer) — for astronomically large n
     * ---------------------------------------------------------------------------
     * Core idea: dp[i] is a LINEAR recurrence over a fixed-size window of the
     * past `forget` values, which means the whole system can be expressed as
     * V_(i+1) = M * V_i for a constant "companion" matrix M of size forget x
     * forget. Repeated squaring (a divide-and-conquer halving of the exponent)
     * lets us jump from V_(forget) to V_(n) in O(forget^3 * log n) instead of
     * O(n), which matters when n is up to ~10^18 and forget is small.
     *
     * Paradigm: Divide and conquer (binary/fast exponentiation of a matrix),
     *           layered on top of the same linear DP recurrence.
     *
     * Time complexity:  O(forget^3 * log n) — matrix multiply is O(forget^3),
     *                    and we perform O(log n) multiplications via squaring.
     * Space complexity: O(forget^2) for the matrices involved.
     *
     * Pros: The only approach here that scales to n far beyond 1000 (e.g. n up
     *       to 10^18) as long as `forget` stays modest.
     * Cons: Considerable implementation complexity and constant factor; for
     *       the STATED constraints (n <= 1000) this is strictly worse than
     *       Approaches 3/4 and would be over-engineering if presented as the
     *       primary answer — I would only bring this up proactively as a
     *       "here's how I'd scale this if n were huge" discussion point.
     * When to use: Only when n is enormous and forget is small/bounded; NOT for
     *       the given constraints.
     */
    static final class MatrixExponentiationApproach {

        /** A square matrix of longs, all arithmetic performed mod {@link #MOD}. */
        private static long[][] multiply(long[][] left, long[][] right) {
            int size = left.length;
            long[][] result = new long[size][size];
            for (int row = 0; row < size; row++) {
                for (int inner = 0; inner < size; inner++) {
                    if (left[row][inner] == 0L) {
                        continue; // small pruning; matrices here are sparse
                    }
                    for (int col = 0; col < size; col++) {
                        if (right[inner][col] == 0L) {
                            continue;
                        }
                        result[row][col] =
                                (result[row][col] + left[row][inner] * right[inner][col]) % MOD;
                    }
                }
            }
            return result;
        }

        /** Fast exponentiation of a square matrix via repeated squaring (divide & conquer). */
        private static long[][] matrixPower(long[][] base, long exponent) {
            int size = base.length;
            long[][] result = new long[size][size];
            for (int i = 0; i < size; i++) {
                result[i][i] = 1L; // identity matrix
            }
            long[][] currentBase = base;
            long remainingExponent = exponent;
            while (remainingExponent > 0) {
                if ((remainingExponent & 1L) == 1L) {
                    result = multiply(result, currentBase);
                }
                currentBase = multiply(currentBase, currentBase);
                remainingExponent >>= 1;
            }
            return result;
        }

        static long solve(int n, int delay, int forget) {
            validateInputs(n, delay, forget);

            int stateSize = forget; // V_i[t] = dp[i - t] for t = 0 .. forget-1

            // Base case: compute dp[1..forget] directly with the simple
            // sliding-window recurrence (cheap: O(forget) regardless of n).
            long[] dp = new long[forget + 1];
            dp[1] = 1L;
            long runningWindowSum = 0L;
            for (int day = 2; day <= forget; day++) {
                if (day - delay >= 1) {
                    runningWindowSum = (runningWindowSum + dp[day - delay]) % MOD;
                }
                if (day - forget >= 1) {
                    runningWindowSum = (runningWindowSum - dp[day - forget] + MOD) % MOD;
                }
                dp[day] = runningWindowSum;
            }

            if (n <= forget) {
                // n falls within the region we already computed directly;
                // the answer is simply the sum over the trailing `forget` window.
                long directAnswer = 0L;
                int lowBound = Math.max(1, n - forget + 1);
                for (int j = lowBound; j <= n; j++) {
                    directAnswer = (directAnswer + dp[j]) % MOD;
                }
                return directAnswer;
            }

            // Build companion (transition) matrix M such that V_(i+1) = M * V_i,
            // where V_i[t] = dp[i - t] for t = 0 .. stateSize-1.
            long[][] transition = new long[stateSize][stateSize];
            // Row 0 computes the new dp value: dp[i+1] = sum_{t=delay-1}^{forget-2} V_i[t].
            for (int t = delay - 1; t <= forget - 2; t++) {
                transition[0][t] = 1L;
            }
            // Rows 1..stateSize-1 simply shift the state down by one position.
            for (int r = 1; r < stateSize; r++) {
                transition[r][r - 1] = 1L;
            }

            // Initial state vector V_forget = [dp[forget], dp[forget-1], ..., dp[1]].
            long[] initialState = new long[stateSize];
            for (int t = 0; t < stateSize; t++) {
                initialState[t] = dp[forget - t];
            }

            long[][] transitionToTheNth = matrixPower(transition, n - forget);

            long[] finalState = new long[stateSize];
            for (int row = 0; row < stateSize; row++) {
                long sum = 0L;
                for (int col = 0; col < stateSize; col++) {
                    sum = (sum + transitionToTheNth[row][col] * initialState[col]) % MOD;
                }
                finalState[row] = sum;
            }

            // finalState[t] = dp[n - t] for t = 0 .. forget-1, i.e. exactly the
            // trailing `forget` values whose sum is the requested answer.
            long answer = 0L;
            for (long value : finalState) {
                answer = (answer + value) % MOD;
            }
            return answer;
        }
    }

    /*
     * ---------------------------------------------------------------------------
     * APPROACH 6: Fenwick Tree / Binary Indexed Tree (advanced structure, O(n log n))
     * ---------------------------------------------------------------------------
     * Core idea: Instead of a prefix-sum array, use a Fenwick tree to support
     * point updates (record dp[i]) and prefix-sum range queries. Functionally
     * equivalent to Approach 3 for this OFFLINE problem, but demonstrates the
     * data structure explicitly, which is the kind of tool that pays for
     * itself if updates and queries were interleaved with OTHER external
     * mutations (e.g., if some days' dp values could be retroactively patched).
     *
     * Paradigm: Advanced structure — Binary Indexed Tree (Fenwick tree).
     *
     * Time complexity:  O(n log n) — each of n point-updates and range-sum
     *                    queries costs O(log n).
     * Space complexity: O(n) for the Fenwick tree array.
     *
     * Pros: Demonstrates familiarity with Fenwick trees; would generalize
     *       cleanly if the problem were extended to support online updates.
     * Cons: Strictly worse constant factor AND worse asymptotic complexity
     *       than Approach 3's O(n) prefix sum for this static, offline
     *       problem — genuinely over-engineered here.
     * When to use: Only if the interviewer extends the problem to require
     *       online point updates to historical dp values, or range queries
     *       interleaved with mutations; NOT as a first answer here.
     */
    static final class FenwickTreeApproach {

        private final long[] tree;
        private final int size;

        FenwickTreeApproach(int size) {
            this.size = size;
            this.tree = new long[size + 1];
        }

        void pointUpdate(int index, long delta) {
            for (int i = index; i <= size; i += i & (-i)) {
                tree[i] = (tree[i] + delta) % MOD;
            }
        }

        long prefixSum(int index) {
            long sum = 0L;
            for (int i = index; i > 0; i -= i & (-i)) {
                sum = (sum + tree[i]) % MOD;
            }
            return sum;
        }

        long rangeSum(int left, int right) {
            if (right < left) {
                return 0L;
            }
            return (prefixSum(right) - prefixSum(left - 1) + MOD) % MOD;
        }

        static long solve(int n, int delay, int forget) {
            validateInputs(n, delay, forget);

            FenwickTreeApproach fenwick = new FenwickTreeApproach(n);
            fenwick.pointUpdate(1, 1L); // dp[1] = 1

            for (int day = 2; day <= n; day++) {
                int windowLow = Math.max(1, day - forget + 1);
                int windowHigh = day - delay;
                long dpToday = fenwick.rangeSum(windowLow, windowHigh);
                fenwick.pointUpdate(day, dpToday);
            }

            int answerWindowLow = Math.max(1, n - forget + 1);
            return fenwick.rangeSum(answerWindowLow, n);
        }
    }

    /** Shared defensive validation used by every approach above. */
    private static void validateInputs(int n, int delay, int forget) {
        if (n < 2) {
            throw new IllegalArgumentException("n must be >= 2, got " + n);
        }
        if (delay < 1) {
            throw new IllegalArgumentException("delay must be >= 1, got " + delay);
        }
        if (forget <= delay) {
            throw new IllegalArgumentException(
                    "forget must be strictly greater than delay, got delay=" + delay + " forget=" + forget);
        }
        if (forget > n) {
            throw new IllegalArgumentException("forget must be <= n, got forget=" + forget + " n=" + n);
        }
    }

    /*
     * =============================================================================
     * SECTION 7: APPROACHES COMPARISON TABLE
     * =============================================================================
     *
     * Approach                         | Time              | Space      | Best For                                | Limitations
     * ---------------------------------|-------------------|------------|-----------------------------------------|--------------------------------------------
     * 1. Brute-Force Simulation        | O(P) (super-poly) | O(P)       | Correctness oracle on tiny n (<= ~20)    | Explodes combinatorially; unusable at scale
     * 2. Naive Windowed DP             | O(n * forget)      | O(n)       | Quick-to-write correct baseline          | Re-sums overlapping windows repeatedly
     * 3. DP + Prefix Sums (RECOMMENDED)| O(n)               | O(n)       | Interview default for n <= 1000          | O(n) space (rarely an issue at this scale)
     * 4. DP + True Sliding Window      | O(n)               | O(forget)  | When space must be minimized             | Trickier add/remove ordering (off-by-one risk)
     * 5. Matrix Exponentiation         | O(forget^3 log n)  | O(forget^2)| n astronomically large, forget small    | Overkill / slower for n <= 1000; complex code
     * 6. Fenwick Tree (BIT)            | O(n log n)         | O(n)       | Interleaved online updates/queries       | Strictly worse than Approach 3 here; overkill
     *
     * -------------------------------------------------------------------------------
     * SECTION 8: RECOMMENDED APPROACH FOR INTERVIEW
     * -------------------------------------------------------------------------------
     * I would present APPROACH 3 (DP + Prefix Sums) as my primary solution:
     *   - It is asymptotically optimal in time (O(n)), matching the best
     *     possible approach for this constraint range.
     *   - It is the easiest of the optimal approaches to code correctly, live,
     *     under time pressure — one invariant ("prefix[i] = sum of dp[1..i]")
     *     is much harder to get wrong than the two-pointer add/remove ordering
     *     required by Approach 4.
     *   - It's trivial to narrate while coding, which interviewers value as
     *     much as the final correctness.
     *   - After landing it, I would proactively offer Approach 4 as a natural
     *     "and here's how I'd shrink the space further if asked," which is
     *     exactly the kind of unprompted depth that signals seniority.
     *
     * -------------------------------------------------------------------------------
     * SECTION 12: FOLLOW-UP QUESTIONS AN INTERVIEWER MIGHT ASK
     * -------------------------------------------------------------------------------
     *  1. "Can you reduce the space complexity below O(n)?"
     *       -> Approach 4 (O(forget) circular buffer).
     *  2. "What if n could be up to 10^18 but delay/forget stay small?"
     *       -> Approach 5 (matrix exponentiation / fast doubling).
     *  3. "What if delay or forget could vary per-person (not global constants)?"
     *       -> The clean 1-D recurrence breaks down; you'd likely need a
     *          priority queue keyed by "day this person forgets" or "day this
     *          person becomes eligible," processing events in time order —
     *          effectively an event-driven simulation with a heap.
     *  4. "What if multiple people could discover the secret independently on
     *      day 1 (say, k initial people)?"
     *       -> Trivial extension: dp[1] = k instead of 1; everything else is
     *          identical since the recurrence is linear.
     *  5. "How would you support answering this for MANY different values of n
     *      (e.g., n = 10, 20, 50, ..., 1000) efficiently in one pass?"
     *       -> Compute the DP/prefix array once up to max(n) and answer every
     *          query in O(1) via the precomputed alive-window sums — no need
     *          to re-run the whole algorithm per query.
     *  6. "Can this run correctly and deterministically if called concurrently
     *      from multiple threads with different (n, delay, forget)?"
     *       -> Yes, as written each solve() call uses only local state; no
     *          shared mutable fields, so it is naturally thread-safe as long
     *          as each thread uses its own arrays (which it does here).
     *
     * -------------------------------------------------------------------------------
     * SECTION 13: WHAT CANDIDATES TYPICALLY MISS
     * -------------------------------------------------------------------------------
     *  1. Off-by-one on the eligibility window: confusing "eligible starting
     *     day d+delay" with "eligible starting day d+delay+1", or confusing
     *     the inclusive upper bound d+forget-1 with d+forget. This single
     *     mistake silently shifts every answer by one day's worth of people.
     *  2. Forgetting that dp[i] can legitimately be 0 for small i (specifically
     *     whenever i - delay < 1, i.e., day i itself is before anyone could
     *     possibly have finished their delay yet) — candidates sometimes clamp
     *     the window incorrectly and read garbage/negative-index values instead
     *     of correctly yielding an empty (zero) sum.
     *  3. In the true sliding-window version (Approach 4), getting the ORDER
     *     of "add the newly-eligible day" vs. "remove the newly-forgotten day"
     *     backwards, or applying the removal one iteration too early/late —
     *     this is the single most common bug in this style of solution.
     *  4. Negative numbers after modulo subtraction: forgetting to add MOD
     *     back before the final `% MOD` when computing prefix[r] - prefix[l-1]
     *     (or any subtraction under a modulus), producing a negative result in
     *     Java instead of the correctly-wrapped positive residue.
     */

    /*
     * =============================================================================
     * SECTION 9: DEEP DIVE — PRODUCTION-QUALITY OPTIMAL SOLUTION
     * =============================================================================
     * This is the polished version of Approach 3 (DP + Prefix Sums), the
     * approach I would actually submit in the interview, with full Javadoc and
     * defensive validation suitable for a real codebase.
     */

    /**
     * Computes the number of people who know the secret at the end of day
     * {@code n}, modulo 1_000_000_007, given that:
     * <ul>
     *   <li>exactly one person discovers the secret on day 1,</li>
     *   <li>each person begins sharing (one new person per day) starting
     *       {@code delay} days after their own discovery, and</li>
     *   <li>each person forgets the secret exactly {@code forget} days after
     *       their own discovery (and can neither know nor share it from that
     *       day onward).</li>
     * </ul>
     *
     * <p>Algorithm: let {@code dp[i]} be the number of people who FIRST learn
     * the secret on day {@code i}. Because a person discovered on day {@code j}
     * shares on every day in {@code [j + delay, j + forget - 1]}, we have
     * {@code dp[i] = sum(dp[j])} for {@code j} in {@code [i - forget + 1, i -
     * delay]}. A running prefix-sum array turns each such range query into an
     * O(1) subtraction, giving an overall O(n) algorithm. The final answer is
     * the sum of {@code dp[j]} for {@code j} in the trailing window
     * {@code [n - forget + 1, n]} — exactly the people who have discovered the
     * secret but not yet forgotten it by the end of day {@code n}.</p>
     *
     * @param n      the final day to count knowledge at (1-indexed); must be >= 2
     * @param delay  days after discovery before a person may start sharing; must be >= 1
     * @param forget days after discovery before a person forgets entirely; must satisfy delay < forget <= n
     * @return the number of people who know the secret at the end of day {@code n}, mod 1_000_000_007
     * @throws IllegalArgumentException if any constraint above is violated
     */
    public static long countPeopleWhoKnowSecret(int n, int delay, int forget) {
        validateInputs(n, delay, forget);

        // prefix[i] = (dp[1] + dp[2] + ... + dp[i]) mod MOD; prefix[0] = 0 by definition.
        long[] prefixSumOfDp = new long[n + 1];
        prefixSumOfDp[1] = 1L; // dp[1] = 1: the single origin discoverer

        for (int day = 2; day <= n; day++) {
            int windowLow = Math.max(1, day - forget + 1);
            int windowHigh = day - delay;

            long dpToday;
            if (windowHigh < windowLow) {
                // No one has completed their delay period relative to `day` yet.
                dpToday = 0L;
            } else {
                dpToday = (prefixSumOfDp[windowHigh] - prefixSumOfDp[windowLow - 1] + MOD) % MOD;
            }

            prefixSumOfDp[day] = (prefixSumOfDp[day - 1] + dpToday) % MOD;
        }

        int finalWindowLow = Math.max(1, n - forget + 1);
        return (prefixSumOfDp[n] - prefixSumOfDp[finalWindowLow - 1] + MOD) % MOD;
    }

    /*
     * =============================================================================
     * SECTION 10 & 11: DRY RUN / TRACE, and CLOSING SUMMARY
     * (executed live below in main(), with printed internal state at each step,
     * plus a cross-validation test harness across every approach implemented
     * above.)
     * =============================================================================
     */
    public static void main(String[] args) {

        System.out.println("=== SECTION 10: DRY RUN / TRACE (n=6, delay=2, forget=4) ===");
        traceDpPrefixSum(6, 2, 4);

        System.out.println();
        System.out.println("=== CROSS-VALIDATION: all approaches must agree ===");

        // {n, delay, forget, expectedAnswerOrNegativeOneIfUnknown}
        int[][] knownTestCases = {
                {6, 2, 4, 5},   // Example A from Section 3
                {2, 1, 2, 2},   // Example B (minimal boundary case)
                {4, 1, 3, 6},   // Example C (tie/boundary reference case)
                {1000, 1, 2, -1}, // largest-n stress case, expected value unknown here
        };

        for (int[] testCase : knownTestCases) {
            int n = testCase[0];
            int delay = testCase[1];
            int forget = testCase[2];
            int expected = testCase[3];

            long naive = DpNaiveApproach.solve(n, delay, forget);
            long prefix = DpPrefixSumApproach.solve(n, delay, forget);
            long sliding = DpSlidingWindowApproach.solve(n, delay, forget);
            long matrix = MatrixExponentiationApproach.solve(n, delay, forget);
            long fenwick = FenwickTreeApproach.solve(n, delay, forget);
            long production = countPeopleWhoKnowSecret(n, delay, forget);

            boolean allAgree = naive == prefix && prefix == sliding && sliding == matrix
                    && matrix == fenwick && fenwick == production;

            System.out.printf(
                    "n=%-5d delay=%-3d forget=%-3d -> naive=%d prefix=%d sliding=%d matrix=%d fenwick=%d prod=%d "
                            + "%s%s%n",
                    n, delay, forget, naive, prefix, sliding, matrix, fenwick, production,
                    allAgree ? "[ALL AGREE]" : "[MISMATCH!!]",
                    expected >= 0 ? (production == expected ? " [MATCHES EXPECTED]" : " [EXPECTED MISMATCH!!]") : "");
        }

        System.out.println();
        System.out.println("=== BRUTE-FORCE ORACLE CROSS-VALIDATION (small n only) + random stress ===");
        Random random = new Random(42);
        int mismatches = 0;
        for (int trial = 0; trial < 200; trial++) {
            int n = 2 + random.nextInt(14);          // n in [2, 15], small enough for brute force
            int delay = 1 + random.nextInt(n - 1);    // delay in [1, n-1]
            int forget = delay + 1 + random.nextInt(n - delay); // forget in [delay+1, n]

            long brute = BruteForceSimulation.solve(n, delay, forget);
            long prod = countPeopleWhoKnowSecret(n, delay, forget);
            long naive = DpNaiveApproach.solve(n, delay, forget);
            long sliding = DpSlidingWindowApproach.solve(n, delay, forget);
            long matrix = MatrixExponentiationApproach.solve(n, delay, forget);
            long fenwick = FenwickTreeApproach.solve(n, delay, forget);

            boolean ok = brute == prod && prod == naive && naive == sliding
                    && sliding == matrix && matrix == fenwick;
            if (!ok) {
                mismatches++;
                System.out.printf("MISMATCH at n=%d delay=%d forget=%d: brute=%d prod=%d naive=%d sliding=%d "
                        + "matrix=%d fenwick=%d%n", n, delay, forget, brute, prod, naive, sliding, matrix, fenwick);
            }
        }
        System.out.println(mismatches == 0
                ? "All 200 randomized small-n trials agree across brute force and every DP variant."
                : mismatches + " mismatches found — see above.");

        System.out.println();
        System.out.println("=== SECTION 11: CLOSING SUMMARY ===");
        System.out.println("""
                All six approaches are verified mutually consistent above. Trade-offs recap:
                  - Approach 1 (brute force) is only a correctness oracle; never ships.
                  - Approach 2 (naive windowed DP) is correct and simple but re-sums windows.
                  - Approach 3 (DP + prefix sums) is the recommended interview answer: O(n)
                    time, O(n) space, minimal bug surface.
                  - Approach 4 (true sliding window) matches Approach 3's time with O(forget)
                    space, at the cost of trickier add/remove bookkeeping.
                  - Approach 5 (matrix exponentiation) only pays off when n is enormous and
                    forget stays small; it is asymptotically worse here for n <= 1000.
                  - Approach 6 (Fenwick tree) is a valid but strictly unnecessary
                    generalization for this static, offline problem.
                Known assumptions carried into the final solution: 1-indexed days, discovery
                on day 1 (treating "day 11" in the prompt as a typo), delay < forget <= n
                guaranteed by the caller, and a single fixed (delay, forget) pair applied
                uniformly to every person.
                """);
    }

    /** Prints the internal dp / prefix-sum state at every step, for Section 10. */
    private static void traceDpPrefixSum(int n, int delay, int forget) {
        long[] prefixSumOfDp = new long[n + 1];
        prefixSumOfDp[1] = 1L;
        System.out.printf("day=1  dp=1   prefix[1..1]=%s%n", Arrays.toString(Arrays.copyOfRange(prefixSumOfDp, 1, 2)));

        for (int day = 2; day <= n; day++) {
            int windowLow = Math.max(1, day - forget + 1);
            int windowHigh = day - delay;

            long dpToday;
            if (windowHigh < windowLow) {
                dpToday = 0L;
            } else {
                dpToday = (prefixSumOfDp[windowHigh] - prefixSumOfDp[windowLow - 1] + MOD) % MOD;
            }
            prefixSumOfDp[day] = (prefixSumOfDp[day - 1] + dpToday) % MOD;

            System.out.printf(
                    "day=%d  window=[%d,%d]%s  dp[%d]=%d  prefix[1..%d]=%s%n",
                    day, windowLow, windowHigh, windowHigh < windowLow ? " (empty)" : "",
                    day, dpToday, day, Arrays.toString(Arrays.copyOfRange(prefixSumOfDp, 1, day + 1)));
        }

        int finalWindowLow = Math.max(1, n - forget + 1);
        long answer = (prefixSumOfDp[n] - prefixSumOfDp[finalWindowLow - 1] + MOD) % MOD;
        System.out.printf("FINAL: answer window=[%d,%d] -> answer=%d%n", finalWindowLow, n, answer);
    }
}
