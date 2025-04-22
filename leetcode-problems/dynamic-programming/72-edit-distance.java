import java.util.Arrays;

class EditDistance {
    public int minDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] memo = new int[m][n];
        for (int i = 0; i < s1.length(); i++) {
            Arrays.fill(memo[i], -1);
        }
        return helper(s1, s2, 0, 0, memo);
    }

    private int helper(String s1, String s2, int i, int j, int[][] memo) {
        if (memo[i][j] != -1)
            return memo[i][j];
        if (i >= s1.length()) {
            return s2.length() - j;
        }
        if (j >= s2.length()) {
            return s1.length() - i;
        }
        if (s1.charAt(i) == s2.charAt(j)) {
            memo[i][j] = helper(s1, s2, i + 1, j + 1, memo);
        } else {
            int insert = helper(s1, s2, i, j + 1, memo);
            int delete = helper(s1, s2, i + 1, j, memo);
            int replace = helper(s1, s2, i + 1, j + 1, memo);
            memo[i][j] = Math.min(insert, Math.min(delete, replace));
        }
        return memo[i][j];
    }
}