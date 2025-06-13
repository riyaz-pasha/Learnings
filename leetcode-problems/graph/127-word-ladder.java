/*
 * A transformation sequence from word beginWord to word endWord using a
 * dictionary wordList is a sequence of words beginWord -> s1 -> s2 -> ... -> sk
 * such that:
 * 
 * Every adjacent pair of words differs by a single letter.
 * Every si for 1 <= i <= k is in wordList. Note that beginWord does not need to
 * be in wordList.
 * sk == endWord
 * Given two words, beginWord and endWord, and a dictionary wordList, return the
 * number of words in the shortest transformation sequence from beginWord to
 * endWord, or 0 if no such sequence exists.
 * 
 * Example 1:
 * 
 * Input: beginWord = "hit", endWord = "cog", wordList =
 * ["hot","dot","dog","lot","log","cog"]
 * Output: 5
 * Explanation: One shortest transformation sequence is "hit" -> "hot" -> "dot"
 * -> "dog" -> cog", which is 5 words long.
 * Example 2:
 * 
 * Input: beginWord = "hit", endWord = "cog", wordList =
 * ["hot","dot","dog","lot","log"]
 * Output: 0
 * Explanation: The endWord "cog" is not in wordList, therefore there is no
 * valid transformation sequence.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

class WordLadder {

    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        if (!wordList.contains(endWord))
            return 0;

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(beginWord);
        visited.add(beginWord);

        int ladderLen = 1;
        String currentWord;
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                currentWord = queue.poll();
                for (String word : wordList) {
                    if (!visited.contains(word) && differsByOne(currentWord, word)) {
                        if (word.equals(endWord))
                            return ladderLen + 1;
                        queue.add(word);
                        visited.add(word);
                    }
                }
            }
            ladderLen++;
        }
        return 0;
    }

    private boolean differsByOne(String word1, String word2) {
        int diff = 0;
        for (int i = 0; i < word1.length(); i++) {
            if (word1.charAt(i) != word2.charAt(i)) {
                diff++;
                if (diff > 1)
                    return false;
            }
        }
        return diff == 1;
    }
}

class WordLadder2 {

    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        Set<String> words = new HashSet<>(wordList);
        if (!words.contains(endWord)) {
            return 0;
        }
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(beginWord);
        visited.add(beginWord);

        int ladderLen = 1;
        String currentWord;
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                currentWord = queue.poll();
                if (currentWord.equals(endWord)) {
                    return ladderLen;
                }
                char[] wordChars = currentWord.toCharArray();
                for (int j = 0; j < wordChars.length; j++) {
                    for (char ch = 'a'; ch <= 'z'; ch++) {
                        String newWord = currentWord.substring(0, j) + ch + currentWord.substring(j + 1);
                        if (words.contains(newWord) && !visited.contains(newWord)) {
                            queue.offer(newWord);
                            visited.add(newWord);
                        }
                    }
                }
            }
            ladderLen++;
        }
        return 0;
    }
}

class WordLadder3 {

    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        if (!wordList.contains(endWord)) {
            return 0;
        }

        Map<String, List<String>> map = buildAdjacencyList(new ArrayList<>() {
            {
                addAll(wordList);
                add(beginWord);
            }
        });

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.offer(beginWord);
        visited.add(beginWord);

        int len = 1;
        String currentWord;
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                currentWord = queue.poll();
                if (currentWord.equals(endWord)) {
                    return len;
                }
                List<String> neighbors = map.getOrDefault(currentWord, new ArrayList<>());
                for (String neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        queue.offer(neighbor);
                        visited.add(neighbor);
                    }
                }
            }
            len++;
        }
        return 0;
    }

    private Map<String, List<String>> buildAdjacencyList(List<String> words) {
        int n = words.size();
        Map<String, List<String>> map = new HashMap<>();
        String word1, word2;
        for (int i = 0; i < n; i++) {
            word1 = words.get(i);
            for (int j = i + 1; j < n; j++) {
                word2 = words.get(j);
                if (differByOne(word1, word2)) {
                    map.computeIfAbsent(word1, k -> new ArrayList<>()).add(word2);
                    map.computeIfAbsent(word2, k -> new ArrayList<>()).add(word1);
                }
            }
        }
        return map;
    }

    private boolean differByOne(String word1, String word2) {
        int diff = 0;
        for (int i = 0; i < word2.length(); i++) {
            if (word1.charAt(i) != word2.charAt(i)) {
                diff++;
                if (diff > 1) {
                    return false;
                }
            }
        }
        return diff == 1;
    }

}

class WordLadderBidirectionalBFS {
    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        if (!wordList.contains(endWord)) {
            return 0;
        }

        Set<String> wordSet = new HashSet<>(wordList);
        Queue<String> beginQueue = new LinkedList<>();
        Queue<String> endQueue = new LinkedList<>();
        Set<String> beginVisited = new HashSet<>();
        Set<String> endVisited = new HashSet<>();

        beginQueue.offer(beginWord);
        endQueue.offer(endWord);
        beginVisited.add(beginWord);
        endVisited.add(endWord);

        int length = 1;

        while (!beginQueue.isEmpty() && !endQueue.isEmpty()) {
            // Process from the beginning
            int beginSize = beginQueue.size();
            for (int i = 0; i < beginSize; i++) {
                String currentWord = beginQueue.poll();
                if (endVisited.contains(currentWord)) {
                    return length;
                }
                for (String neighbor : getNeighbors(currentWord, wordSet)) {
                    if (!beginVisited.contains(neighbor)) {
                        beginVisited.add(neighbor);
                        beginQueue.offer(neighbor);
                    }
                }
            }
            length++;

            // Process from the end
            int endSize = endQueue.size();
            for (int i = 0; i < endSize; i++) {
                String currentWord = endQueue.poll();
                if (beginVisited.contains(currentWord)) {
                    return length;
                }
                for (String neighbor : getNeighbors(currentWord, wordSet)) {
                    if (!endVisited.contains(neighbor)) {
                        endVisited.add(neighbor);
                        endQueue.offer(neighbor);
                    }
                }
            }
            length++;
        }

        return 0;
    }

    private List<String> getNeighbors(String word, Set<String> wordSet) {
        List<String> neighbors = new ArrayList<>();
        char[] wordChars = word.toCharArray();
        for (int i = 0; i < wordChars.length; i++) {
            char originalChar = wordChars[i];
            for (char ch = 'a'; ch <= 'z'; ch++) {
                wordChars[i] = ch;
                String neighbor = new String(wordChars);
                if (wordSet.contains(neighbor)) {
                    neighbors.add(neighbor);
                }
            }
            wordChars[i] = originalChar; // Backtrack
        }
        return neighbors;
    }

}

class WordLadder4 {

    /**
     * Find the shortest transformation sequence from beginWord to endWord.
     * 
     * @param beginWord Starting word
     * @param endWord   Target word
     * @param wordList  List of valid words for transformation
     * @return Length of shortest transformation sequence, or 0 if impossible
     */
    public int ladderLength(String beginWord, String endWord, List<String> wordList) {
        // If endWord is not in wordList, no transformation is possible
        Set<String> wordSet = new HashSet<>(wordList);
        if (!wordSet.contains(endWord)) {
            return 0;
        }

        // BFS queue: stores words to explore
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.offer(beginWord);
        visited.add(beginWord);

        int steps = 1;

        while (!queue.isEmpty()) {
            int size = queue.size();

            // Process all words at current level
            for (int i = 0; i < size; i++) {
                String currentWord = queue.poll();

                // If we reached the end word, return the number of steps
                if (currentWord.equals(endWord)) {
                    return steps;
                }

                // Try all possible single-letter transformations
                for (int j = 0; j < currentWord.length(); j++) {
                    char[] wordArray = currentWord.toCharArray();

                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c != wordArray[j]) {
                            wordArray[j] = c;
                            String newWord = new String(wordArray);

                            // If new word is valid and not visited
                            if (wordSet.contains(newWord) && !visited.contains(newWord)) {
                                visited.add(newWord);
                                queue.offer(newWord);
                            }
                        }
                    }
                }
            }
            steps++;
        }

        // No transformation sequence found
        return 0;
    }

    /**
     * Optimized version using bidirectional BFS for better performance.
     * This approach searches from both ends simultaneously.
     */
    public int ladderLengthOptimized(String beginWord, String endWord, List<String> wordList) {
        Set<String> wordSet = new HashSet<>(wordList);
        if (!wordSet.contains(endWord)) {
            return 0;
        }

        // Two sets for bidirectional search
        Set<String> beginSet = new HashSet<>();
        Set<String> endSet = new HashSet<>();
        Set<String> visited = new HashSet<>();

        beginSet.add(beginWord);
        endSet.add(endWord);

        int steps = 1;

        while (!beginSet.isEmpty() && !endSet.isEmpty()) {
            // Always expand the smaller set for efficiency
            if (beginSet.size() > endSet.size()) {
                Set<String> temp = beginSet;
                beginSet = endSet;
                endSet = temp;
            }

            Set<String> nextLevel = new HashSet<>();

            for (String word : beginSet) {
                for (int i = 0; i < word.length(); i++) {
                    char[] wordArray = word.toCharArray();

                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c != wordArray[i]) {
                            wordArray[i] = c;
                            String newWord = new String(wordArray);

                            // If we meet the other end, we found the path
                            if (endSet.contains(newWord)) {
                                return steps + 1;
                            }

                            // If new word is valid and not visited
                            if (wordSet.contains(newWord) && !visited.contains(newWord)) {
                                visited.add(newWord);
                                nextLevel.add(newWord);
                            }
                        }
                    }
                }
            }

            beginSet = nextLevel;
            steps++;
        }

        return 0;
    }

    // Test method
    public static void main(String[] args) {
        WordLadder solution = new WordLadder();

        // Test case 1
        String beginWord1 = "hit";
        String endWord1 = "cog";
        List<String> wordList1 = Arrays.asList("hot", "dot", "dog", "lot", "log", "cog");
        System.out.println("Test 1: " + solution.ladderLength(beginWord1, endWord1, wordList1)); // Expected: 5

        // Test case 2
        String beginWord2 = "hit";
        String endWord2 = "cog";
        List<String> wordList2 = Arrays.asList("hot", "dot", "dog", "lot", "log");
        System.out.println("Test 2: " + solution.ladderLength(beginWord2, endWord2, wordList2)); // Expected: 0

        // Test case 3
        String beginWord3 = "a";
        String endWord3 = "c";
        List<String> wordList3 = Arrays.asList("a", "b", "c");
        System.out.println("Test 3: " + solution.ladderLength(beginWord3, endWord3, wordList3)); // Expected: 2
    }

}
