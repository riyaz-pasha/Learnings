
import java.util.ArrayList;
import java.util.List;

/*
 * A generic microwave supports cooking times for:
 * 
 * at least 1 second.
 * at most 99 minutes and 99 seconds.
 * To set the cooking time, you push at most four digits. The microwave
 * normalizes what you push as four digits by prepending zeroes. It interprets
 * the first two digits as the minutes and the last two digits as the seconds.
 * It then adds them up as the cooking time. For example,
 * 
 * You push 9 5 4 (three digits). It is normalized as 0954 and interpreted as 9
 * minutes and 54 seconds.
 * You push 0 0 0 8 (four digits). It is interpreted as 0 minutes and 8 seconds.
 * You push 8 0 9 0. It is interpreted as 80 minutes and 90 seconds.
 * You push 8 1 3 0. It is interpreted as 81 minutes and 30 seconds.
 * You are given integers startAt, moveCost, pushCost, and targetSeconds.
 * Initially, your finger is on the digit startAt. Moving the finger above any
 * specific digit costs moveCost units of fatigue. Pushing the digit below the
 * finger once costs pushCost units of fatigue.
 * 
 * There can be multiple ways to set the microwave to cook for targetSeconds
 * seconds but you are interested in the way with the minimum cost.
 * 
 * Return the minimum cost to set targetSeconds seconds of cooking time.
 * 
 * Remember that one minute consists of 60 seconds.
 * 
 * Example 1:
 * Input: startAt = 1, moveCost = 2, pushCost = 1, targetSeconds = 600
 * Output: 6
 * Explanation: The following are the possible ways to set the cooking time.
 * - 1 0 0 0, interpreted as 10 minutes and 0 seconds.
 * The finger is already on digit 1, pushes 1 (with cost 1), moves to 0 (with
 * cost 2), pushes 0 (with cost 1), pushes 0 (with cost 1), and pushes 0 (with
 * cost 1).
 * The cost is: 1 + 2 + 1 + 1 + 1 = 6. This is the minimum cost.
 * - 0 9 6 0, interpreted as 9 minutes and 60 seconds. That is also 600 seconds.
 * The finger moves to 0 (with cost 2), pushes 0 (with cost 1), moves to 9 (with
 * cost 2), pushes 9 (with cost 1), moves to 6 (with cost 2), pushes 6 (with
 * cost 1), moves to 0 (with cost 2), and pushes 0 (with cost 1).
 * The cost is: 2 + 1 + 2 + 1 + 2 + 1 + 2 + 1 = 12.
 * - 9 6 0, normalized as 0960 and interpreted as 9 minutes and 60 seconds.
 * The finger moves to 9 (with cost 2), pushes 9 (with cost 1), moves to 6 (with
 * cost 2), pushes 6 (with cost 1), moves to 0 (with cost 2), and pushes 0 (with
 * cost 1).
 * The cost is: 2 + 1 + 2 + 1 + 2 + 1 = 9.
 * 
 * Example 2:
 * Input: startAt = 0, moveCost = 1, pushCost = 2, targetSeconds = 76
 * Output: 6
 * Explanation: The optimal way is to push two digits: 7 6, interpreted as 76
 * seconds.
 * The finger moves to 7 (with cost 1), pushes 7 (with cost 2), moves to 6 (with
 * cost 1), and pushes 6 (with cost 2). The total cost is: 1 + 2 + 1 + 2 = 6
 * Note other possible ways are 0076, 076, 0116, and 116, but none of them
 * produces the minimum cost.
 */

class Solution {

    public int minCostSetTime(int startAt, int moveCost, int pushCost, int targetSeconds) {
        int minCost = Integer.MAX_VALUE;

        // Try all possible combinations of minutes and seconds
        // that can produce targetSeconds
        for (int m = 0; m <= 99; m++) {
            for (int s = 0; s <= 99; s++) {
                if (m * 60 + s == targetSeconds) {
                    minCost = Math.min(minCost, calculateCost(m, s, startAt, moveCost, pushCost));
                }
            }
        }

        return minCost;
    }

    private int calculateCost(int minutes, int seconds, int startAt, int moveCost, int pushCost) {
        // Convert minutes and seconds to 4-digit string
        String timeStr = String.format("%02d%02d", minutes, seconds);

        // Try all possible ways to input this time (1 to 4 digits)
        int minCost = Integer.MAX_VALUE;

        // Try 4 digits: MMSS
        minCost = Math.min(minCost, getCostForSequence(timeStr, startAt, moveCost, pushCost));

        // Try 3 digits: MSS (if first digit is 0)
        if (timeStr.charAt(0) == '0') {
            minCost = Math.min(minCost, getCostForSequence(timeStr.substring(1), startAt, moveCost, pushCost));
        }

        // Try 2 digits: SS (if first two digits are 0)
        if (timeStr.charAt(0) == '0' && timeStr.charAt(1) == '0') {
            minCost = Math.min(minCost, getCostForSequence(timeStr.substring(2), startAt, moveCost, pushCost));
        }

        // Try 1 digit: S (if first three digits are 0)
        if (timeStr.charAt(0) == '0' && timeStr.charAt(1) == '0' && timeStr.charAt(2) == '0') {
            minCost = Math.min(minCost, getCostForSequence(timeStr.substring(3), startAt, moveCost, pushCost));
        }

        return minCost;
    }

    private int getCostForSequence(String sequence, int startAt, int moveCost, int pushCost) {
        int cost = 0;
        int currentPos = startAt;

        for (char c : sequence.toCharArray()) {
            int digit = c - '0';
            if (digit != currentPos) {
                cost += moveCost;
                currentPos = digit;
            }
            cost += pushCost;
        }

        return cost;
    }

}

// Alternative optimized solution that directly generates valid combinations
class SolutionOptimized {

    public int minCostSetTime(int startAt, int moveCost, int pushCost, int targetSeconds) {
        int minCost = Integer.MAX_VALUE;

        // Generate all valid (minutes, seconds) pairs
        for (int m = 0; m <= 99; m++) {
            int remainingSeconds = targetSeconds - m * 60;
            if (remainingSeconds >= 0 && remainingSeconds <= 99) {
                minCost = Math.min(minCost, calculateMinCost(m, remainingSeconds, startAt, moveCost, pushCost));
            }
        }

        return minCost;
    }

    private int calculateMinCost(int minutes, int seconds, int startAt, int moveCost, int pushCost) {
        // Generate all possible input sequences for this time
        String[] sequences = generateSequences(minutes, seconds);

        int minCost = Integer.MAX_VALUE;
        for (String seq : sequences) {
            if (seq != null) {
                minCost = Math.min(minCost, getCostForSequence(seq, startAt, moveCost, pushCost));
            }
        }

        return minCost;
    }

    private String[] generateSequences(int minutes, int seconds) {
        String fourDigit = String.format("%02d%02d", minutes, seconds);
        String[] sequences = new String[4];

        // 4 digits: MMSS
        sequences[0] = fourDigit;

        // 3 digits: MSS (if M < 10)
        if (minutes < 10) {
            sequences[1] = String.format("%d%02d", minutes, seconds);
        }

        // 2 digits: SS (if minutes == 0)
        if (minutes == 0) {
            sequences[2] = String.format("%02d", seconds);
        }

        // 1 digit: S (if minutes == 0 and seconds < 10)
        if (minutes == 0 && seconds < 10) {
            sequences[3] = String.valueOf(seconds);
        }

        return sequences;
    }

    private int getCostForSequence(String sequence, int startAt, int moveCost, int pushCost) {
        int cost = 0;
        int currentPos = startAt;

        for (char c : sequence.toCharArray()) {
            int digit = c - '0';
            if (digit != currentPos) {
                cost += moveCost;
                currentPos = digit;
            }
            cost += pushCost;
        }

        return cost;
    }

}

// Test class
class TestMicrowave {

    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        int result1 = solution.minCostSetTime(1, 2, 1, 600);
        System.out.println("Test 1 - Expected: 6, Got: " + result1);

        // Test case 2
        int result2 = solution.minCostSetTime(0, 1, 2, 76);
        System.out.println("Test 2 - Expected: 6, Got: " + result2);

        // Additional test cases
        int result3 = solution.minCostSetTime(5, 3, 2, 3661); // 61 minutes 1 second
        System.out.println("Test 3 - Result: " + result3);
    }

}

class MinimumCostToSetCookingTime {

    public int minCostSetTime(int startAt, int moveCost, int pushCost, int targetSeconds) {
        List<List<Integer>> times = getTimes(targetSeconds);
        int minCost = Integer.MAX_VALUE;

        for (List<Integer> time : times) {
            int cost = 0;
            int prevNum = startAt;
            for (int num : time) {
                if (prevNum != num) {
                    cost += moveCost;
                    prevNum = num;
                }
                cost += pushCost;
            }
            minCost = Math.min(minCost, cost);
        }
        return minCost;
    }

    private List<List<Integer>> getTimes(int targetSeconds) {
        List<List<Integer>> result = new ArrayList<>();

        int m1 = targetSeconds / 60;
        int s1 = targetSeconds % 60;
        List<Integer> time1 = getTime(m1, s1);
        if (!time1.isEmpty())
            result.add(time1);

        int m2 = m1 - 1;
        int s2 = s1 + 60;
        List<Integer> time2 = getTime(m2, s2);
        if (!time2.isEmpty())
            result.add(time2);

        return result;
    }

    private List<Integer> getTime(int minutes, int seconds) {
        if (minutes < 0 || minutes > 99 || seconds < 0 || seconds > 99) {
            return List.of();
        }

        int total = minutes * 100 + seconds;
        List<Integer> time = new ArrayList<>();

        // Extract digits from total MMSS, maintaining leading zeroes if < 4 digits
        for (int i = 3; i >= 0; i--) {
            time.add(0, total % 10);
            total /= 10;
        }

        // Trim leading zeros for shortest valid input (simulate digit entry)
        while (time.size() > 1 && time.get(0) == 0) {
            time.remove(0);
        }

        return time;
    }

}
