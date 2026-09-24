import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * ✅ Problem Statement
 * You have a collection of documents, where each document is associated with
 * one or more tags. You need to support efficient boolean queries over tags,
 * such as:
 * - tag1 AND tag2
 * - tag1 OR tag2
 * - tag1 AND NOT tag2
 */

class Document {

    int id;
    List<String> tags;

    public Document(int id, List<String> tags) {
        this.id = id;
        this.tags = tags;
    }

}

class DocumentSearch {

    /*
     * ⏱ Time and Space Complexity
     * 📦 Space:
     * - O(T × D) where T = number of unique tags, D = average documents per tag.
     * 
     * 🧮 Time (for query tag1 AND tag2 OR tag3):
     * Each set operation takes O(min(N1, N2)) where N1 and N2 = sizes of tag sets.
     * 
     * Worst case: O(N) where N is number of documents per tag involved.
     */

    List<Document> documents;
    Map<String, Set<Integer>> tagToDocumentIdMap; // invertedIndex

    public DocumentSearch(List<Document> documents) {
        this.documents = documents;
        this.tagToDocumentIdMap = new HashMap<>();
        this.buildInvertedIndex(documents);
    }

    private void buildInvertedIndex(List<Document> docs) {
        for (Document doc : docs) {
            for (String tag : doc.tags) {
                this.tagToDocumentIdMap
                        .computeIfAbsent(tag, _ -> new HashSet<>())
                        .add(doc.id);
            }
        }
    }

    public Set<Integer> search(String query) {
        String[] parts = query.split(" ");
        Set<Integer> result = new HashSet<>();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (part.equals("AND")) {
                i++;
                result.retainAll(this.getDocs(parts[i]));
            } else if (part.equals("OR")) {
                i++;
                result.addAll(this.getDocs(parts[i]));
            } else if (part.equals("NOT")) {
                i++;
                result.removeAll(this.getDocs(parts[i]));
            } else {
                // first tag
                result = new HashSet<>(this.getDocs(part));
            }
        }

        return result;
    }

    private Set<Integer> getDocs(String tag) {
        return this.tagToDocumentIdMap.getOrDefault(tag, new HashSet<>());
    }

    public static void main(String[] args) {
        List<Document> documents = List.of(
                new Document(1, Arrays.asList("java", "ai")),
                new Document(2, Arrays.asList("python", "ml")),
                new Document(3, Arrays.asList("java", "ml")),
                new Document(4, Arrays.asList("ai", "security")));

        DocumentSearch engine = new DocumentSearch(documents);

        System.out.println(engine.search("java AND ml")); // => [3]
        System.out.println(engine.search("java OR ml")); // => [1, 2, 3]
        System.out.println(engine.search("java AND NOT ml")); // => [1]
    }

}

class OptimizedSearchEngine {

    private final Map<String, BitSet> tagToBitSet = new HashMap<>();
    private int maxDocId = 0;

    // Add a document with tags
    public void addDocument(int docId, List<String> tags) {
        maxDocId = Math.max(maxDocId, docId);
        for (String tag : tags) {
            tagToBitSet.computeIfAbsent(tag, k -> new BitSet()).set(docId);
        }
    }

    // Execute flat queries like: "java AND ml", "ai OR NOT security"
    public Set<Integer> search(String query) {
        String[] tokens = query.split(" ");
        BitSet result = null;

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];

            if (token.equals("AND")) {
                i++;
                result.and(getBitSet(tokens[i]));
            } else if (token.equals("OR")) {
                i++;
                result.or(getBitSet(tokens[i]));
            } else if (token.equals("NOT")) {
                i++;
                BitSet notSet = getBitSet(tokens[i]);
                BitSet allDocs = getAllDocsBitSet();
                notSet.flip(0, maxDocId + 1);
                if (result == null) {
                    result = notSet;
                } else {
                    result.and(notSet);
                }
            } else {
                // first tag
                result = (BitSet) getBitSet(token).clone();
            }
        }

        return bitSetToDocIds(result);
    }

    private BitSet getBitSet(String tag) {
        return tagToBitSet.getOrDefault(tag, new BitSet());
    }

    private BitSet getAllDocsBitSet() {
        BitSet all = new BitSet();
        all.set(0, maxDocId + 1);
        return all;
    }

    private Set<Integer> bitSetToDocIds(BitSet bs) {
        Set<Integer> result = new HashSet<>();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            result.add(i);
        }
        return result;
    }

    public static void main(String[] args) {
        OptimizedSearchEngine engine = new OptimizedSearchEngine();

        engine.addDocument(1, Arrays.asList("java", "ai"));
        engine.addDocument(2, Arrays.asList("python", "ml"));
        engine.addDocument(3, Arrays.asList("java", "ml"));
        engine.addDocument(4, Arrays.asList("ai", "security"));

        System.out.println(engine.search("java AND ml")); // [3]
        System.out.println(engine.search("java OR ml")); // [1, 2, 3]
        System.out.println(engine.search("java AND NOT ml")); // [1]
        System.out.println(engine.search("NOT security")); // [1, 2, 3]
    }

}
