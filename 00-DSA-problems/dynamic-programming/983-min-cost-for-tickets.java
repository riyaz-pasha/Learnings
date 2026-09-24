class MinCostForTickets {

    // ✅ Time and Space Complexity
    // Time: O(365) = O(1) practically
    // Space: O(365) = O(1) practically
    public int mincostTickets(int[] days, int[] costs) {
        int lastDay = days[days.length - 1];
        int[] dp = new int[lastDay + 1];
        boolean[] travelDays = new boolean[lastDay + 1];
        for (int day : days) {
            travelDays[day] = true;
        }

        for (int day = 1; day <= lastDay; day++) {
            if (!travelDays[day]) {
                dp[day] = dp[day - 1]; // no travel, no extra cost
            } else {
                int cost1 = dp[Math.max(0, day - 1)] + costs[0];
                int cost7 = dp[Math.max(0, day - 7)] + costs[1];
                int cost30 = dp[Math.max(0, day - 30)] + costs[2];
                dp[day] = Math.min(cost1, Math.min(cost7, cost30));
            }
        }

        return dp[lastDay];
    }

}
