class DecodeWays {

    public int numDecodings(String s) {
        if (s == null || s.length() == 0 || s.charAt(0) == '0')
            return 0;

        int n = s.length();
        int[] dp = new int[n + 1];
        dp[0] = 1; // empty string has one way to decode
        dp[1] = 1; // non-zero single digit has one way to decode

        for (int i = 2; i <= n; i++) {
            // Single digit decode
            if (s.charAt(i - 1) != '0') {
                dp[i] += dp[i - 1];
            }

            // Two digit decode
            int twoDigit = Integer.parseInt(s.substring(i - 2, i));
            if (twoDigit >= 10 && twoDigit <= 26) {
                dp[i] += dp[i - 2];
            }
        }

        return dp[n];
    }
    
}
