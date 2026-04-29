package streams;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class RemoveDuplicates {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1, 2, 3, 2, 4, 3, 5, 1);

        List<Integer> result1 = nums.stream()
                .distinct()
                .collect(Collectors.toList());
        System.out.println(result1);
        // Output: [1, 2, 3, 4, 5]
        // Note: distinct() uses equals() internally
    }
}
