package streams;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

class FindAvg {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(10, 20, 30, 40, 50);

        Double avg1 = nums.stream()
                .collect(Collectors.averagingInt(Integer::intValue));
        System.out.println(avg1);

        OptionalDouble average2 = nums.stream()
                .mapToInt(Integer::intValue)
                .average();
        System.out.println(average2);
        average2.ifPresent(System.out::println);
    }

}
