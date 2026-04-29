package streams;

import java.util.Arrays;
import java.util.List;

class SumList {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        Integer sum1 = nums.stream()
                .reduce(0, (a, b) -> a + b);
        System.out.println(sum1);

        Integer sum2 = nums.stream()
                .reduce(0, Integer::sum);
        System.out.println(sum2);

        int sum3 = nums.stream()
                .mapToInt(Integer::intValue)
                .sum();
        System.out.println(sum3);
    }

}
