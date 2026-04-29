package streams;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class GroupAnagrams {

    public static void main(String[] args) {
        List<String> words = Arrays.asList("eat", "tea", "tan", "ate", "nat", "bat");

        Map<String, List<String>> anagrams = words.stream()
                .collect(Collectors.groupingBy(
                        word -> {
                            char[] chars = word.toCharArray();
                            Arrays.sort(chars);
                            return new String(chars); // sorted chars = anagram key
                        }));

        System.out.println(anagrams);

        Map<String, List<String>> anagrams2 = words.stream()
                .collect(Collectors.groupingBy(
                        word -> word.chars().sorted()
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                                .toString()));
        System.out.println(anagrams2);
    }

}
