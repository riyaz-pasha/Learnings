package streams;

import java.util.Arrays;

class CountWords {

    public static void main(String[] args) {
        String sentence = "Java Streams are really powerful and fun";

        long count = Arrays.stream(sentence.strip().split("\s*"))
                .count();
        System.out.println(count);
        // Output: 7

        // Count words longer than 4 chars:
        long longWords = Arrays.stream(sentence.trim().split("\s+"))
                .filter(w -> w.length() > 4)
                .count();
        System.out.println(longWords);
        // Output: 3 (Streams, really, powerful)
    }

}
