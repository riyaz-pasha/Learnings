class ClimbingStairs {

    public int noOfWaysToClimbStairs(int steps) {
        int last2steps = 1;
        int last1step = 2;
        int current = 0;

        for (int i = 3; i <= steps; i++) {
            current = last1step + last2steps;
            last2steps = last1step;
            last1step = current;
        }
        return current;
    }

    //  Dynamic Programming (Bottom-Up Approach)
    public int noOfWaysToClimbStairs1(int steps) {
        int[] cache = new int[steps + 1];
        cache[0] = 0;
        cache[1] = 1;
        cache[2] = 2;
        for (int i = 3; i <= steps; i++) {
            cache[i] = cache[i - 1] + cache[i - 2];
        }
        return cache[steps];
    }


    public int noOfWaysToClimbStairs2(int noOfSteps) {
        if (noOfSteps <= 0) return 0;
        if (noOfSteps == 1) return 1;
        if (noOfSteps == 2) return 2;
        return noOfWaysToClimbStairs2(noOfSteps - 1)
                + noOfWaysToClimbStairs2(noOfSteps - 2);
    }

    public static void main(String[] args) {
        System.out.println(new ClimbingStairs().noOfWaysToClimbStairs(3));
        System.out.println(new ClimbingStairs().noOfWaysToClimbStairs(4));
    }
}
