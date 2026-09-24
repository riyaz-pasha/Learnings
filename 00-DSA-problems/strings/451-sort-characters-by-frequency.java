import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

class SortCharactersByFrequency {

    /**
     * Approach 1: HashMap + Sorting
     * Time Complexity: O(n log n), Space Complexity: O(n)
     * Most straightforward approach
     */
    public String frequencySort1(String s) {
        // Count frequency of each character
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : s.toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }

        // Create list of characters and sort by frequency
        List<Character> chars = new ArrayList<>(freqMap.keySet());
        chars.sort((a, b) -> freqMap.get(b) - freqMap.get(a));

        // Build result string
        StringBuilder result = new StringBuilder();
        for (char c : chars) {
            int freq = freqMap.get(c);
            for (int i = 0; i < freq; i++) {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Approach 2: HashMap + Priority Queue (Max Heap)
     * Time Complexity: O(n log k) where k is unique characters, Space Complexity:
     * O(n)
     * Efficient using heap for sorting
     */
    public String frequencySort2(String s) {
        // Count frequency
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : s.toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }

        // Use max heap (priority queue) to sort by frequency
        PriorityQueue<Map.Entry<Character, Integer>> maxHeap = new PriorityQueue<>(
                (a, b) -> b.getValue() - a.getValue());

        maxHeap.addAll(freqMap.entrySet());

        // Build result
        StringBuilder result = new StringBuilder();
        while (!maxHeap.isEmpty()) {
            Map.Entry<Character, Integer> entry = maxHeap.poll();
            char c = entry.getKey();
            int freq = entry.getValue();
            for (int i = 0; i < freq; i++) {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Approach 3: Bucket Sort
     * Time Complexity: O(n), Space Complexity: O(n)
     * Most efficient - uses bucket sort based on frequency
     */
    public String frequencySort3(String s) {
        // Count frequency
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : s.toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }

        // Create buckets where index = frequency
        List<Character>[] buckets = new List[s.length() + 1];
        for (char c : freqMap.keySet()) {
            int freq = freqMap.get(c);
            if (buckets[freq] == null) {
                buckets[freq] = new ArrayList<>();
            }
            buckets[freq].add(c);
        }

        // Build result from highest frequency to lowest
        StringBuilder result = new StringBuilder();
        for (int freq = buckets.length - 1; freq > 0; freq--) {
            if (buckets[freq] != null) {
                for (char c : buckets[freq]) {
                    for (int i = 0; i < freq; i++) {
                        result.append(c);
                    }
                }
            }
        }

        return result.toString();
    }

    /**
     * Approach 4: Array Counter + Custom Class
     * Time Complexity: O(n log k), Space Complexity: O(n)
     * Good for understanding the sorting process
     */
    public String frequencySort4(String s) {
        // Count frequency using array for ASCII characters
        int[] count = new int[128]; // ASCII characters
        for (char c : s.toCharArray()) {
            count[c]++;
        }

        // Create list of character-frequency pairs
        List<CharFreq> list = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            if (count[i] > 0) {
                list.add(new CharFreq((char) i, count[i]));
            }
        }

        // Sort by frequency in descending order
        Collections.sort(list, (a, b) -> b.freq - a.freq);

        // Build result
        StringBuilder result = new StringBuilder();
        for (CharFreq cf : list) {
            for (int i = 0; i < cf.freq; i++) {
                result.append(cf.c);
            }
        }

        return result.toString();
    }

    // Helper class for Approach 4
    static class CharFreq {
        char c;
        int freq;

        CharFreq(char c, int freq) {
            this.c = c;
            this.freq = freq;
        }
    }

    /**
     * Approach 5: Stream API (Modern Java)
     * Time Complexity: O(n log n), Space Complexity: O(n)
     * Functional programming style
     */
    public String frequencySort5(String s) {
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : s.toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }

        return freqMap.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .map(entry -> String.valueOf(entry.getKey()).repeat(entry.getValue()))
                .collect(Collectors.joining());
    }

    /**
     * Approach 6: Optimized with StringBuilder repeat
     * Time Complexity: O(n log n), Space Complexity: O(n)
     * Clean and readable
     */
    public String frequencySort6(String s) {
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char c : s.toCharArray()) {
            freqMap.put(c, freqMap.getOrDefault(c, 0) + 1);
        }

        List<Map.Entry<Character, Integer>> entries = new ArrayList<>(freqMap.entrySet());
        entries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        StringBuilder result = new StringBuilder();
        for (Map.Entry<Character, Integer> entry : entries) {
            char c = entry.getKey();
            int freq = entry.getValue();
            result.append(String.valueOf(c).repeat(freq));
        }

        return result.toString();
    }

    // Test method
    public static void main(String[] args) {
        SortCharactersByFrequency solution = new SortCharactersByFrequency();

        // Test Case 1
        System.out.println("Test Case 1:");
        System.out.println("Input: s = \"tree\"");
        System.out.println("Output: " + solution.frequencySort3("tree"));
        System.out.println("Expected: \"eert\" or \"eetr\"");

        // Test Case 2
        System.out.println("\nTest Case 2:");
        System.out.println("Input: s = \"cccaaa\"");
        System.out.println("Output: " + solution.frequencySort3("cccaaa"));
        System.out.println("Expected: \"aaaccc\" or \"cccaaa\"");

        // Test Case 3
        System.out.println("\nTest Case 3:");
        System.out.println("Input: s = \"Aabb\"");
        System.out.println("Output: " + solution.frequencySort3("Aabb"));
        System.out.println("Expected: \"bbAa\" or \"bbaA\"");

        // Test Case 4
        System.out.println("\nTest Case 4:");
        System.out.println("Input: s = \"loveleetcode\"");
        System.out.println("Output: " + solution.frequencySort3("loveleetcode"));

        // Performance comparison
        System.out.println("\n--- Performance Comparison ---");
        String testStr = "aabbbbcccccddddddeeeeeee";

        long start = System.nanoTime();
        String result1 = solution.frequencySort1(testStr);
        long time1 = System.nanoTime() - start;

        start = System.nanoTime();
        String result2 = solution.frequencySort2(testStr);
        long time2 = System.nanoTime() - start;

        start = System.nanoTime();
        String result3 = solution.frequencySort3(testStr);
        long time3 = System.nanoTime() - start;

        System.out.println("HashMap + Sort: " + time1 + " ns");
        System.out.println("Priority Queue: " + time2 + " ns");
        System.out.println("Bucket Sort: " + time3 + " ns (O(n) - fastest!)");
    }
}
