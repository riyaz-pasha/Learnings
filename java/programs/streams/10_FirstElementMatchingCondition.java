package streams;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

class FirstElementMatchingCondition {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1, 3, 7, 2, 9, 4);

        Optional<Integer> first = nums.stream()
                .filter(num -> num > 5)
                .findFirst();

        // Output: Optional[7]
        first.ifPresent(System.out::println); // 7

        // findAny() is faster in parallel streams
        Optional<Integer> any = nums.parallelStream()
                .filter(n -> n > 5)
                .findAny();
        any.ifPresent(System.out::println); // 7
    }

}
