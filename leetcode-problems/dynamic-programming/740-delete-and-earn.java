import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

class DeleteAndEarn {
    public int deleteAndEarn2(int[] nums) {
        SortedMap<Integer, Integer> numMap = new TreeMap<Integer, Integer>();
        for (int i = 0; i < nums.length; i++) {
            numMap.put(nums[i], numMap.getOrDefault(nums[i], 0) + nums[i]);
        }

        Integer maxNum = numMap.lastKey();
        int[] dp = new int[maxNum + 3];
        dp[maxNum + 1] = 0;
        dp[maxNum + 2] = 0;
        for (int i = maxNum; i >= 0; i--) {
            dp[i] = Math.max(numMap.getOrDefault(i, 0) + dp[i + 2], dp[i + 1]);
        }
        return dp[0];
    }

    public int deleteAndEarn1(int[] nums) {
        SortedMap<Integer, Integer> numMap = new TreeMap<Integer, Integer>();
        for (int i = 0; i < nums.length; i++) {
            numMap.put(nums[i], numMap.getOrDefault(nums[i], 0) + nums[i]);
        }

        Integer maxNum = numMap.lastKey();
        int last1 = 0;
        int last2 = 0;
        int current = 0;
        for (int i = maxNum; i >= 0; i--) {
            current = Math.max(numMap.getOrDefault(i, 0) + last2, last1);
            last2 = last1;
            last1 = current;
        }
        return current;
    }

    public int deleteAndEarn3(int[] nums) {
        SortedMap<Integer, Integer> numMap = new TreeMap<Integer, Integer>();
        for (int num : nums) {
            // numMap.put(num, numMap.getOrDefault(num, 0) + num);
            numMap.merge(num, num, Integer::sum);
        }

        int prevKey = -1, take = 0, skip = 0;
        for (int key : numMap.keySet()) {
            if (key == prevKey + 1) {
                int newTake = skip + numMap.get(key);
                int newSkip = Math.max(take, skip);
                take = newTake;
                skip = newSkip;
            } else {
                int prevMax = Math.max(skip, take);
                take = prevMax + numMap.get(key);
                skip = prevMax;
            }
            prevKey = key;
        }
        return Math.max(take, skip);
    }

    public int deleteAndEarn(int[] nums) {
        int maxNum = Integer.MIN_VALUE;
        for (int num : nums) {
            maxNum = Math.max(maxNum, num);
        }

        int[] points = new int[maxNum + 1];
        for (int num : nums) {
            points[num] += num;
        }

        int take = 0, skip = 0, newTake = 0;
        for (int i = 0; i <= maxNum; i++) {
            newTake = skip + points[i];
            skip = Math.max(take, skip);
            take = newTake;
        }

        return Math.max(take, skip);
    }
}
// newTake -> 0+0=0 -> 0 -> 0+2=2 -> 0+3=3 -> 2+4=6
// newSkip -> 0,0=0 -> 0 -> 0     -> 2,0=2 -> 3,2=3
// take    -> 0     -> 0 -> 2     -> 3     -> 6
// skip    -> 0     -> 0 -> 0     -> 2     -> 3

// [2,2,3,3,3,4]
// [2:4,3:9,4:1]
// newTake -> 0 -> 0 -> 0+4=4 -> 0+9=9 -> 4+4=8
// skip    -> 0 -> 0 -> 0,0=0 -> 0,4=4 -> 9
// take    -> 0 -> 0 -> 4     -> 9     -> 8

// [8,10,4,9,1,3,5,9,4,10] -> 37
// [1:1,2:0,3:3,4:8,5:5,6:0,7:0,8:8,9:18,10:20]
// newTake -> 0 -> 0+1=1 -> 0+0=0  -> 1+3=4 -> 1+8=9 -> 4+5=9 -> 9+0=9 -> 9+0=9 -> 9+8=17 -> 9+18=27 -> 17+20=37
// skip    -> 0 -> 0     -> 1      -> 1     -> 4     -> 9     -> 9     -> 9     -> 9      -> 17      -> 27
// take    -> 0 -> 1     -> 0      -> 4     -> 9     -> 9     -> 9     -> 9     -> 17     -> 27      -> 37
