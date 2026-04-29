package streams;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class FreqOfEachCharInString {

    public static void main(String[] args) {
        String s = "programming";

        Map<Character, Long> charFreq = s.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()));

        System.out.println(charFreq);
        // {p=1, r=2, o=1, g=2, a=1, m=2, i=1, n=1}

        // Sorted by frequency:
        charFreq.entrySet().stream()
                .sorted(Map.Entry.<Character, Long>comparingByValue().reversed())
                .forEach(e -> System.out.println(e.getKey() + " : " + e.getValue()));
    }

}
