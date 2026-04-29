package streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

class FindMax {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);

        Optional<Integer> optionalMax = nums.stream()
                .max(Comparator.naturalOrder());
        optionalMax.ifPresent(System.out::println); // 5

        OptionalInt optionalMax2 = nums.stream()
                .mapToInt(Integer::intValue)
                .max();
        optionalMax2.ifPresent(System.out::println); // 5

        int optionalMax3 = nums.stream()
                .mapToInt(Integer::intValue)
                .max()
                .getAsInt();
        System.out.println(optionalMax3);

    }

}
