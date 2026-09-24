import java.util.Arrays;

class SolveQuestionsWithBrainPower {

    public long mostPoints(int[][] questions) {
        int n = questions.length;
        long[] memo = new long[n];
        Arrays.fill(memo, -1);
        return helper(questions, 0, memo);
    }

    private long helper(int[][] questions, int i, long[] memo) {
        if (i >= questions.length) {
            return 0;
        }
        if (memo[i] != -1) {
            return memo[i];
        }
        long solve = questions[i][0] + helper(questions, i + questions[i][1] + 1, memo);
        long skip = helper(questions, i + 1, memo);
        memo[i] = Math.max(solve, skip);
        return memo[i];
    }

}

class SolveQuestionsWithBrainPower2 {

    public long mostPoints(int[][] questions) {
        int n = questions.length;
        long[] dp = new long[n + questions[n - 1][1] + 1];
        long pick, skip;
        long prev;
        for (int i = n - 1; i >= 0; i--) {
            prev = ((i + questions[i][1] + 1) < n) ? dp[i + questions[i][1] + 1] : 0;
            pick = questions[i][0] + prev;
            skip = dp[i + 1];
            dp[i] = Math.max(pick, skip);
        }
        return dp[0];
    }

    // Time: O(n), where n = number of questions
    // Space: O(n)
    public long mostPoints2(int[][] questions) {
        int n = questions.length;
        long[] dp = new long[n + 1];

        for (int i = n - 1; i >= 0; i--) {
            int next = i + questions[i][1] + 1;
            long solve = questions[i][0] + (next < n ? dp[next] : 0);
            long skip = dp[i + 1];
            dp[i] = Math.max(solve, skip);
        }

        return dp[0];
    }

    // questions = {{3,2},{4,3},{4,4},{2,1}}

    // dp array of size 5: [0, 0, 0, 0, 0]
    // dp[i] = max points starting from question i

    // Loop from i = n - 1 down to 0
    // i = 3 (last question: {2,1})
    //   points = questions[3][0] = 2
    //   skip = questions[3][1] = 1
    //   next = i + skip + 1 = 3 + 1 + 1 = 5

    //   Option 1: solve the current question (question 3)
    //     solve = points + (next < n ? dp[next] : 0)
    //     solve = 2 + (5 < 4 ? dp[5] : 0) = 2 + 0 = 2 (since 5 is not < 4, dp[5] is out of bounds, so it's 0)

    //   Option 2: skip the current question (question 3)
    //     skipQ = dp[i + 1] = dp[3 + 1] = dp[4] = 0

    //   Take the max of both options
    //   dp[3] = Math.max(solve, skipQ) = Math.max(2, 0) = 2
    //   dp array: [0, 0, 0, 2, 0]

    // i = 2 (question: {4,4})
    //   points = questions[2][0] = 4
    //   skip = questions[2][1] = 4
    //   next = i + skip + 1 = 2 + 4 + 1 = 7

    //   Option 1: solve the current question (question 2)
    //     solve = points + (next < n ? dp[next] : 0)
    //     solve = 4 + (7 < 4 ? dp[7] : 0) = 4 + 0 = 4

    //   Option 2: skip the current question (question 2)
    //     skipQ = dp[i + 1] = dp[2 + 1] = dp[3] = 2

    //   Take the max of both options
    //   dp[2] = Math.max(solve, skipQ) = Math.max(4, 2) = 4
    //   dp array: [0, 0, 4, 2, 0]

    // i = 1 (question: {4,3})
    //   points = questions[1][0] = 4
    //   skip = questions[1][1] = 3
    //   next = i + skip + 1 = 1 + 3 + 1 = 5

    //   Option 1: solve the current question (question 1)
    //     solve = points + (next < n ? dp[next] : 0)
    //     solve = 4 + (5 < 4 ? dp[5] : 0) = 4 + 0 = 4

    //   Option 2: skip the current question (question 1)
    //     skipQ = dp[i + 1] = dp[1 + 1] = dp[2] = 4

    //   Take the max of both options
    //   dp[1] = Math.max(solve, skipQ) = Math.max(4, 4) = 4
    //   dp array: [0, 4, 4, 2, 0]

    // i = 0 (question: {3,2})
    //   points = questions[0][0] = 3
    //   skip = questions[0][1] = 2
    //   next = i + skip + 1 = 0 + 2 + 1 = 3

    //   Option 1: solve the current question (question 0)
    //     solve = points + (next < n ? dp[next] : 0)
    //     solve = 3 + (3 < 4 ? dp[3] : 0) = 3 + dp[3] = 3 + 2 = 5

    //   Option 2: skip the current question (question 0)
    //     skipQ = dp[i + 1] = dp[0 + 1] = dp[1] = 4

    //   Take the max of both options
    //   dp[0] = Math.max(solve, skipQ) = Math.max(5, 4) = 5
    //   dp array: [5, 4, 4, 2, 0]

}
