package streams;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class SortList {

    public static void main(String[] args) {
        List<String> names = Arrays.asList("Charlie", "Alice", "Bob", "Dave");

        List<String> ascNames = names.stream()
                .sorted()
                .collect(Collectors.toList());
        System.out.println(ascNames);

        List<String> dscNames = names.stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        System.out.println(dscNames);

        // Remove duplicates and sort
        List<Integer> nums = Arrays.asList(5, 3, 1, 3, 2, 5, 4, 2);
        List<Integer> result = nums.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        System.out.println(result);
        // [1, 2, 3, 4, 5]
    }

}
