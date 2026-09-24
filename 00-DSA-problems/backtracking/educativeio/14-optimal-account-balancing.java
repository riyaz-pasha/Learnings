/**
 * ============================================================================
 * OPTIMAL ACCOUNT BALANCING (MINIMUM TRANSACTIONS) - COMPREHENSIVE GUIDE
 * ============================================================================
 * 
 * 1. RESTATING THE PROBLEM IN OUR OWN TERMS:
 * ----------------------------------------------------------------------------
 * A group of friends went on a trip and paid for each other. We are given a 
 * ledger of who paid whom. Our goal is to "settle up" in the smartest way 
 * possible. We want to find the absolute minimum number of direct money 
 * transfers required so that everyone's net balance is resolved to $0.
 * 
 * 
 * 2. CLARIFYING QUESTIONS TO ASK IN AN INTERVIEW:
 * ----------------------------------------------------------------------------
 * Q: Do we care about the actual path of the transactions, or just the count?
 * A: The problem only asks for the minimum NUMBER of transactions. We don't 
 *    need to return who pays whom in the final solution.
 * 
 * Q: How many people are involved?
 * A: The IDs of the people range from 0 to 10 (max 11 people). This is the 
 *    golden clue! With a very small number of actors, finding the absolute 
 *    minimum usually requires exploring all combinations using Backtracking (DFS).
 * 
 * Q: Can people have multiple transactions?
 * A: Yes. Someone could lend $10 to Alice and borrow $5 from Bob. 
 *    Before we do any search, we MUST consolidate these down to a single "net" 
 *    balance per person.
 * 
 * 
 * 3. IDEA, INTUITION, AND KEY OBSERVATIONS:
 * ----------------------------------------------------------------------------
 * - CONSOLIDATION: The individual transactions don't matter. All that matters 
 *   is the NET BALANCE of each person. 
 *   If Alice is owed $5, her balance is +5. If Bob owes $5, his balance is -5.
 * - PEOPLE WITH $0 BALANCE: If someone's net balance is 0, they are already 
 *   settled. We completely ignore them.
 * - THE BACKTRACKING LOGIC: We take the first person who owes/is owed money 
 *   (let's call them A). We must find someone else (B) who has the OPPOSITE 
 *   sign in their balance, and transfer A's entire balance to B. This costs 
 *   1 transaction and perfectly settles A. We then recursively solve for the 
 *   rest of the people.
 * - GREEDY PRUNING (THE SECRET WEAPON): If A owes $10 (-10) and B is owed 
 *   exactly $10 (+10), settling them against each other instantly resolves 
 *   BOTH people in exactly 1 transaction. This is mathematically optimal! 
 *   If we see this exact opposite match, we take it immediately and skip 
 *   trying anyone else for A.
 * 
 * 
 * 4. HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * ----------------------------------------------------------------------------
 * - Step 1: Explain the "Net Balance" concept. Show how 3 messy transactions 
 *   can be reduced to a clean array of balances.
 * - Step 2: Extract only the non-zero balances into a small list to reduce 
 *   the search space for our DFS.
 * - Step 3: Write the DFS. Emphasize the rule: "We only transfer between two 
 *   people if their balances have opposite signs (one positive, one negative)."
 * - Step 4: Add the Greedy perfect-match pruning to show your deep understanding 
 *   of the mathematical optimizations available.
 * 
 * 
 * 5. VISUAL EXAMPLE:
 * ----------------------------------------------------------------------------
 * Transactions: [[0,1,10], [2,0,5]]
 * 
 * Step 1: Net Balances
 * Person 0: Gave 10 to 1 (-10), Received 5 from 2 (+5). Net: -5 (Owes 5)
 * Person 1: Received 10 from 0 (+10). Net: +10 (Is owed 10)
 * Person 2: Gave 5 to 0 (-5). Net: -5 (Owes 5)
 * 
 * Step 2: Non-Zero Balances Array
 * [-5, +10, -5]
 * 
 * Step 3: DFS Backtracking
 * curr = 0 (Balance -5). 
 * Look for an opposite sign. Index 1 has +10. (Signs are opposite! Valid!)
 * Transfer Person 0's debt to Person 1: 
 *   Cost: 1 transaction.
 *   Person 1 new balance: +10 + (-5) = +5.
 *   Person 0 is now considered settled.
 * 
 * Next step: curr = 1 (Balance +5).
 * Look for opposite sign. Index 2 has -5. 
 * EXACT MATCH! (+5 and -5).
 * Transfer Person 1's balance to Person 2:
 *   Cost: 1 transaction.
 *   Both are settled.
 * 
 * Total minimum transactions = 2.
 */

import java.util.*;

public class OptimalAccountBalancing {

    public int minTransfers(int[][] transactions) {

        /*
         * ============================================================
         * STEP 1: CALCULATE NET BALANCES
         * ============================================================
         * The problem says IDs are between 0 and 10. So an array of size 11
         * is perfectly sufficient. 
         * A positive balance means the person is OWED money.
         * A negative balance means the person OWES money.
         */
        int[] netBalances = new int[11];
        for (int[] t : transactions) {
            int from = t[0];
            int to = t[1];
            int amount = t[2];
            
            netBalances[from] -= amount; // 'from' gave money, so their balance drops
            netBalances[to] += amount;   // 'to' received money, so their balance rises
        }

        /*
         * ============================================================
         * STEP 2: FILTER NON-ZERO BALANCES
         * ============================================================
         * People with a net balance of $0 are already settled. 
         * Including them in our DFS would just waste time checking them.
         * We extract only the people who actually need to be settled.
         */
        List<Integer> activeBalances = new ArrayList<>();
        for (int balance : netBalances) {
            if (balance != 0) {
                activeBalances.add(balance);
            }
        }

        /*
         * We convert the list back to an array for the fastest possible 
         * index-based access during our deep recursion.
         */
        int[] debtArray = activeBalances.stream().mapToInt(i -> i).toArray();

        // Step 3: Start the recursive search from the 0th person in our array
        return dfs(debtArray, 0);
    }

    private int dfs(int[] balances, int curr) {
        
        /*
         * ============================================================
         * BASE CASE & AUTOMATIC PROGRESSION
         * ============================================================
         * 
         * If 'curr' reaches the end of the array, everyone is settled!
         * It takes 0 additional transactions from this point.
         */
        if (curr == balances.length) {
            return 0;
        }

        /*
         * If the current person is ALREADY settled (balance == 0), 
         * we don't need to do any transactions for them. 
         * Just move on to the next person.
         */
        if (balances[curr] == 0) {
            return dfs(balances, curr + 1);
        }

        int minTransactions = Integer.MAX_VALUE;

        /*
         * ============================================================
         * THE DECISION LOOP
         * ============================================================
         * 
         * Person 'curr' needs to be settled. 
         * We must find SOMEONE ELSE ahead of them in the array (index 'i') 
         * to absorb their balance.
         */
        for (int i = curr + 1; i < balances.length; i++) {
            
            /*
             * --------------------------------------------------------
             * CORE RULE: OPPOSITE SIGNS ONLY
             * --------------------------------------------------------
             * We ONLY transfer money between a debtor and a creditor.
             * If both have negative balances (both owe money), transferring 
             * debt between them doesn't get either of them closer to zero.
             * 
             * Math check: (balances[curr] * balances[i] < 0) means one is 
             * positive and one is negative.
             */
            if (balances[curr] * balances[i] < 0) {
                
                /*
                 * --------------------------------------------------------
                 * CHOOSE
                 * --------------------------------------------------------
                 * We decide to have person 'i' absorb person 'curr's entire 
                 * balance. This conceptually zeroes out 'curr' and costs 
                 * exactly 1 transaction.
                 */
                balances[i] += balances[curr];

                /*
                 * --------------------------------------------------------
                 * EXPLORE
                 * --------------------------------------------------------
                 * Now that 'curr' is settled, we recursively solve for the 
                 * REST of the array starting at 'curr + 1'.
                 * We add 1 to the result because we just made 1 transaction.
                 */
                int currentPathTransactions = 1 + dfs(balances, curr + 1);
                
                // Track the absolute minimum across all choices we try
                minTransactions = Math.min(minTransactions, currentPathTransactions);

                /*
                 * --------------------------------------------------------
                 * UNCHOOSE (BACKTRACK)
                 * --------------------------------------------------------
                 * We finished exploring what happens if person 'i' absorbed 
                 * the balance. Now we UNDO it, so the loop can try having the 
                 * NEXT person (i + 1) absorb the balance instead.
                 */
                balances[i] -= balances[curr];

                /*
                 * ========================================================
                 * GREEDY PRUNING (CRITICAL OPTIMIZATION)
                 * ========================================================
                 * 
                 * If person 'curr' owed exactly the same amount that person 'i' 
                 * was owed (e.g., -10 and +10), settling them together is 
                 * MATHEMATICALLY OPTIMAL. 
                 * 
                 * Since we just un-chose (balances[i] -= balances[curr]), 
                 * checking if balances[i] + balances[curr] == 0 confirms 
                 * they were perfect opposites.
                 * 
                 * If we find a perfect opposite, there is NO BETTER CHOICE 
                 * in the entire array. We can completely abandon exploring 
                 * any other 'i's for this 'curr'.
                 */
                if (balances[i] + balances[curr] == 0) {
                    break;
                }
            }
        }

        return minTransactions;
    }

    /**
     * MAIN METHOD: Executing and testing our code with explanations
     */
    public static void main(String[] args) {
        OptimalAccountBalancing solver = new OptimalAccountBalancing();

        // Test Case 1: Simple Chain
        // 0 -> 1 -> 2
        // Optimal: 0 pays 2 directly (1 transaction)
        int[][] transactions1 = {{0, 1, 10}, {1, 2, 10}};
        System.out.println("Test Case 1:");
        System.out.println("Expected: 1 | Result: " + solver.minTransfers(transactions1));
        System.out.println();

        // Test Case 2: Common Creditor
        // 0 -> 1, 2 -> 1
        // Optimal: 0 pays 1, 2 pays 1 (2 transactions)
        int[][] transactions2 = {{0, 1, 10}, {2, 1, 5}};
        System.out.println("Test Case 2:");
        System.out.println("Expected: 2 | Result: " + solver.minTransfers(transactions2));
        System.out.println();
        
        // Test Case 3: Circular Debt
        // 0 -> 1 -> 2 -> 0 (all same amount)
        // Optimal: Everyone's net is 0 initially. No transactions needed!
        int[][] transactions3 = {{0, 1, 10}, {1, 2, 10}, {2, 0, 10}};
        System.out.println("Test Case 3:");
        System.out.println("Expected: 0 | Result: " + solver.minTransfers(transactions3));
    }
}

/**
 * ================================================================
 * 🔥 Optimal Account Balancing (LeetCode Hard)
 * ================================================================
 *
 * 🎯 PROBLEM REFRAME (MOST IMPORTANT STEP)
 * ------------------------------------------------
 * Instead of thinking in terms of transactions,
 * think in terms of FINAL NET BALANCE per person.
 *
 * Example:
 *   A gave B 10 → A = -10, B = +10
 *
 * After processing all transactions:
 *   Each person has:
 *     - negative → owes money
 *     - positive → should receive money
 *
 * Now the problem becomes:
 *
 * 👉 "How do we settle these balances with MINIMUM transactions?"
 *
 *
 * ================================================================
 * 🧠 CORE IDEA
 * ================================================================
 *
 * 1. Convert transactions → net balances
 * 2. Remove all zero balances (already settled)
 * 3. Use BACKTRACKING to try all ways of settling debts
 *
 *
 * ================================================================
 * 💡 KEY OBSERVATION
 * ================================================================
 *
 * If person A owes -10 and person B has +10,
 * we can settle in 1 transaction → optimal
 *
 * But if multiple options exist:
 * we must TRY ALL possibilities → backtracking
 *
 *
 * ================================================================
 * 🚀 WHY BACKTRACKING?
 * ================================================================
 *
 * Greedy fails because:
 *   Choosing the locally best pairing doesn't guarantee global optimum.
 *
 * So:
 *   Try all combinations → pick minimum
 *
 *
 * ================================================================
 * ⏱ COMPLEXITY
 * ================================================================
 *
 * Time:  O(n!)  (but heavily pruned)
 * Space: O(n)   (recursion stack)
 *
 */
public class OptimalAccountBalancingDetailed {

    public static void main(String[] args) {

        int[][] transactions = {
                {0, 1, 10},
                {2, 0, 5}
        };

        System.out.println(minTransfers(transactions)); // Expected: 2
    }

    /**
     * Main function
     */
    public static int minTransfers(int[][] transactions) {

        /**
         * ============================================================
         * STEP 1: Compute NET BALANCE of each person
         * ============================================================
         *
         * Map<person, netBalance>
         *
         * Example:
         *   [0→1:10], [2→0:5]
         *
         *   Person 0: -10 + 5 = -5
         *   Person 1: +10
         *   Person 2: -5
         */
        Map<Integer, Integer> balanceMap = new HashMap<>();

        for (int[] t : transactions) {
            int from = t[0];
            int to = t[1];
            int amount = t[2];

            // 'from' loses money → negative
            balanceMap.put(from, balanceMap.getOrDefault(from, 0) - amount);

            // 'to' gains money → positive
            balanceMap.put(to, balanceMap.getOrDefault(to, 0) + amount);
        }

        /**
         * ============================================================
         * STEP 2: Filter only NON-ZERO balances
         * ============================================================
         *
         * Why?
         *   Zero balance people don't participate anymore
         *
         * Example:
         *   [-5, +10, -5]
         */
        List<Integer> debts = new ArrayList<>();

        for (int balance : balanceMap.values()) {
            if (balance != 0) {
                debts.add(balance);
            }
        }

        /**
         * ============================================================
         * STEP 3: Backtracking to settle debts
         * ============================================================
         */
        return dfs(0, debts);
    }

    /**
     * ============================================================
     * 🔥 DFS BACKTRACKING FUNCTION
     * ============================================================
     *
     * @param start → index from where we try to settle
     * @param debts → current state of balances
     *
     * Idea:
     *   Pick one person (start)
     *   Try to settle with every other person having opposite sign
     *
     */
    private static int dfs(int start, List<Integer> debts) {

        /**
         * ============================================================
         * STEP 1: Skip already settled people (balance == 0)
         * ============================================================
         */
        while (start < debts.size() && debts.get(start) == 0) {
            start++;
        }

        /**
         * ============================================================
         * BASE CASE:
         * All debts settled → no transactions needed
         * ============================================================
         */
        if (start == debts.size()) {
            return 0;
        }

        int minTransactions = Integer.MAX_VALUE;

        int currentDebt = debts.get(start);

        /**
         * ============================================================
         * STEP 2: Try to settle this debt with others
         * ============================================================
         *
         * Only pair with opposite sign:
         *   negative with positive OR vice versa
         */
        for (int i = start + 1; i < debts.size(); i++) {

            // Only consider opposite signs
            if (currentDebt * debts.get(i) < 0) {

                /**
                 * ====================================================
                 * TRY THIS TRANSACTION
                 * ====================================================
                 *
                 * We "settle" currentDebt with debts[i]
                 *
                 * Instead of creating a transaction record,
                 * we simulate it by updating balance:
                 *
                 * debts[i] += currentDebt
                 *
                 * Example:
                 *   current = -5
                 *   debts[i] = +10
                 *
                 *   After settlement:
                 *     debts[i] = +5
                 */
                debts.set(i, debts.get(i) + currentDebt);

                /**
                 * ====================================================
                 * RECURSION
                 * ====================================================
                 *
                 * Move to next index (start + 1)
                 *
                 * +1 because we just used one transaction
                 */
                int transactions = 1 + dfs(start + 1, debts);

                // Track minimum
                minTransactions = Math.min(minTransactions, transactions);

                /**
                 * ====================================================
                 * BACKTRACK (UNDO)
                 * ====================================================
                 */
                debts.set(i, debts.get(i) - currentDebt);

                /**
                 * ====================================================
                 * 🔥 PRUNING OPTIMIZATION (VERY IMPORTANT)
                 * ====================================================
                 *
                 * If this transaction perfectly cancels debts[i],
                 * then no need to try other options.
                 *
                 * Why?
                 *   Perfect match is always optimal for this position.
                 *
                 * Example:
                 *   -5 + 5 = 0 → perfect
                 */
                if (debts.get(i) + currentDebt == 0) {
                    break;
                }
            }
        }

        return minTransactions;
    }
}
