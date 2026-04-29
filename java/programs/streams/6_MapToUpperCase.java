package streams;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class MapToUpperCase {

    public static void main(String[] args) {
        List<String> names = Arrays.asList("alice", "bob", "charlie");

        List<String> result1 = names.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        System.out.println(result1);

    }

}
