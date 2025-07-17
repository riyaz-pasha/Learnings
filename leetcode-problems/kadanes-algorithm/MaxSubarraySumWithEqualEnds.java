import java.util.HashMap;
import java.util.Map;

public class MaxSubarraySumWithEqualEnds {

    public static int[] findMaxSubarrayWithEqualEnds(int[] nums) {
        Map<Integer, Integer> bestStartIndex = new HashMap<>();
        Map<Integer, Integer> bestStartPrefix = new HashMap<>();

        int n = nums.length;
        int[] prefixSum = new int[n + 1];
        for (int i = 0; i < n; i++)
            prefixSum[i + 1] = prefixSum[i] + nums[i];

        int maxSum = Integer.MIN_VALUE;
        int[] result = { -1, -1 };

        for (int j = 0; j < n; j++) {
            int num = nums[j];

            if (bestStartIndex.containsKey(num)) {
                int i = bestStartIndex.get(num);
                int sum = prefixSum[j + 1] - bestStartPrefix.get(num);

                if (sum > maxSum) {
                    maxSum = sum;
                    result[0] = i;
                    result[1] = j;
                }
            }

            // If this is the first time OR a better start
            if (!bestStartIndex.containsKey(num) ||
                    prefixSum[j] < bestStartPrefix.get(num)) {
                bestStartIndex.put(num, j);
                bestStartPrefix.put(num, prefixSum[j]);
            }
        }

        return result;
    }

    public static void main(String[] args) {
        int[] nums = { 2, -1, -5, 2, 1, 4, 2 };
        int[] res = findMaxSubarrayWithEqualEnds(nums);
        System.out.println("Max subarray with equal ends: [" + res[0] + ", " + res[1] + "]");
    }

}
