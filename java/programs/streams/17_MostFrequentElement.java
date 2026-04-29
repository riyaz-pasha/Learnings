package streams;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class MostFrequentElement {

    public static void main(String[] args) {
        List<String> words = Arrays.asList("a", "b", "a", "c", "b", "a", "d");

        String maxEle = words.stream()
                .collect(Collectors.groupingBy(word -> word, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        System.out.println(maxEle);
    }

}
