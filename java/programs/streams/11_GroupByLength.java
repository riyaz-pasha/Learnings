package streams;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class GroupByLength {

    public static void main(String[] args) {
        List<String> words = Arrays.asList("cat", "dog", "elephant", "ant", "bear", "cow");

        Map<Integer, List<String>> groupByLen = words.stream()
                .collect(Collectors.groupingBy(String::length));
        System.out.println(groupByLen);
        // Output: {3=[cat, dog, ant, cow], 4=[bear], 8=[elephant]}
    }

}
