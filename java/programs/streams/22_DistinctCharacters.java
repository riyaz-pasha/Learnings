package streams;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class DistinctCharacters {

    public static void main(String[] args) {
        List<String> words = Arrays.asList("hello", "world", "java");

        List<Character> distinctChars = words.stream()
                .flatMap(w -> w.chars().mapToObj(c -> (char) c))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        System.out.println(distinctChars);
        // Output: [a, d, e, h, j, l, o, r, v, w]

    }

}
