package streams;

import java.util.Arrays;
import java.util.List;

class CountGreater {

    public static void main(String[] args) {
        List<Integer> nums = Arrays.asList(1, 3, 5, 7, 9, 11, 2, 8);

        long count = nums.stream()
                .filter(num -> num > 5)
                .count();

        System.out.println(count);
        // Output: 4 (7, 9, 11, 8)

        List<String> words = Arrays.asList("apple", "banana", "kiwi", "strawberry", "fig");
        long count2 = words.stream()
                .filter(s -> s.length() > 5)
                .count();
        System.out.println(count2);
        // 2 (banana, strawberry)
    }

}
