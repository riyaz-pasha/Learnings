package streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class FindMin {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(3, 1, 7, 2, 9, 4);

        Optional<Integer> optionalMin1 = nums.stream()
                .min(Comparator.naturalOrder());
        optionalMin1.ifPresent(System.out::println);

        int min2 = nums.stream()
                .mapToInt(i -> i)
                .min()
                .getAsInt();
        System.out.println(min2);
    }

}
