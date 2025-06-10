import java.util.Arrays;

class NumberOfLIS {

    // Time: O(n²)
    // Space: O(n)
    public int findNumberOfLIS(int[] nums) {
        int n = nums.length;
        if (n == 0)
            return 0;
        int[] lens = new int[n];
        int[] counts = new int[n];
        Arrays.fill(lens, 1);
        Arrays.fill(counts, 1);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[i] > nums[j]) {
                    if (lens[j] + 1 > lens[i]) {
                        lens[i] = lens[j] + 1;
                        counts[i] = counts[j];
                    } else if (lens[j] + 1 == lens[i]) {
                        counts[i] += counts[j];
                    }
                }
            }
        }
        int max = 0;
        for (int len : lens) {
            max = Math.max(len, max);
        }
        int res = 0;
        for (int i = 0; i < n; i++) {
            if (lens[i] == max)
                res += counts[i];
        }
        return res;
    }

}
