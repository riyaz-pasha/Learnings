
class GoodStringsCounter {

    private static final int MOD = 1_000_000_007;

    public int countGoodStrings(int low, int high, int zero, int one) {
        Integer[] memo = new Integer[high + 1];
        return dfs(0, low, high, zero, one, memo);
    }

    private int dfs(int length, int low, int high, int zero, int one, Integer[] memo) {
        if (length > high)
            return 0;
        if (memo[length] != null)
            return memo[length];

        int count = (length >= low) ? 1 : 0;
        count = (count + dfs(length + zero, low, high, zero, one, memo)) % MOD;
        count = (count + dfs(length + one, low, high, zero, one, memo)) % MOD;

        memo[length] = count;
        return count;
    }
}

// class CountWaysToBuildGoodStrings {

// public int countGoodStrings(int low, int high, int zero, int one) {
// Set<String> goodStrings = new HashSet<>();
// helper(low, high, zero, one, "", goodStrings);
// return goodStrings.size();
// }

// private void helper(int low, int high, int zero, int one, String s,
// Set<String> goodStrings) {
// if (s.length() > high || (zero == 0 && one == 0)) {
// return;
// }
// if (s.length() >= low) {
// goodStrings.add(s);
// }

// helper(low, high, zero, one, s + "0".repeat(zero), goodStrings);
// helper(low, high, zero, one, s + "1".repeat(one), goodStrings);
// }

// }

class CountWaysToBuildGoodStrings {

    public int countGoodStrings(int low, int high, int zero, int one) {
        int[] dp = new int[high + 1];
        int count = 0;
        int mod = 1_000_000_007;
        dp[0] = 1;
        for (int i = 1; i <= high; i++) {
            if (i >= zero) {
                dp[i] = (dp[i] + dp[i - zero]) % mod;
            }
            if (i >= one) {
                dp[i] = (dp[i] + dp[i - one]) % mod;
            }
            if (i >= low) {
                count = (count + dp[i]) % mod;
            }
        }
        return count;
    }

}
