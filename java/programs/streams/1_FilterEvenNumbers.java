package streams;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class FilterEvenNumbers {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<Integer> result = nums.stream()
                .filter(num -> num % 2 == 0)
                .collect(Collectors.toList());

        System.out.println(result);

    }

}
