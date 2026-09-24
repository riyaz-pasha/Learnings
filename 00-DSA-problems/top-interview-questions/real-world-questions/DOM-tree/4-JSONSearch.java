import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class JSONSearch {

    public List<String> findMatchingPaths(Map<String, Object> json, String searchPhrase) {
        List<String> result = new ArrayList<>();
        this.dfs(json, searchPhrase, result, new StringBuilder(), new ArrayList<>());
        return result;
    }

    private void dfs(Object node, String searchPhrase, List<String> result,
            StringBuilder currentPath, List<String> currentPathList) {

        if (node == null) {
            return;
        }

        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;

            // Check if current path of keys matches the phrase
            String fullPathConcat = currentPath.toString();
            if (fullPathConcat.contains(searchPhrase)) {
                result.add(String.join(".", currentPathList));
            }

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                int prevLen = currentPath.length();

                currentPath.append(entry.getKey().toLowerCase());
                currentPathList.add(entry.getKey());

                this.dfs(entry.getValue(), searchPhrase, result, currentPath, currentPathList);

                currentPath.setLength(prevLen);
                currentPathList.removeLast();
            }
        } else if (node instanceof String || node instanceof Number || node instanceof Boolean) {
            int prevLen = currentPath.length();

            currentPath.append(node.toString().toLowerCase());

            if (currentPath.indexOf(searchPhrase) != -1) {
                result.add(String.join(".", currentPathList));
            }

            currentPath.setLength(prevLen);
        }
    }

    // Example usage
    public void main(String[] args) {
        Map<String, Object> json = Map.of(
                "user", Map.of(
                        "pro", Map.of(
                                "file", Map.of(
                                        "name", "john",
                                        "details", "works at TechCorp"))),
                "meta", Map.of(
                        "info", "This is a test"));

        List<String> matches = findMatchingPaths(json, "profilejohn");

        for (String path : matches) {
            System.out.println(path);
        }
    }

}

class JSONSearch2 {

    public List<String> findMatchingPaths(Map<String, Object> json, String searchPhrase) {
        List<String> result = new ArrayList<>();
        this.dfs(json, searchPhrase, new ArrayList<>(), new StringBuilder(), result);
        return result;
    }

    private void dfs(Object node,
            String searchPhrase,
            List<String> currentPath,
            StringBuilder currentConcat,
            List<String> result) {
        if (node instanceof Map<?, ?> mapNode) {
            for (Map.Entry<?, ?> entry : mapNode.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();

                currentPath.add(key);
                int prevLen = currentConcat.length();
                currentConcat.append(key.toLowerCase());

                if (currentConcat.indexOf(searchPhrase) != -1) {
                    result.add(String.join(".", currentPath));
                }

                dfs(value, searchPhrase, currentPath, currentConcat, result);

                currentPath.remove(currentPath.size() - 1);
                currentConcat.setLength(prevLen);
            }
        } else if (node instanceof List<?> listNode) {
            for (Object child : listNode) {
                dfs(child, searchPhrase, currentPath, currentConcat, result);
            }
        } else {
            // Primitive or null
            int prevLen = currentConcat.length();
            String valueStr = (node == null) ? "null" : node.toString().toLowerCase();

            currentConcat.append(valueStr);

            if (currentConcat.indexOf(searchPhrase) != -1) {
                result.add(String.join(".", currentPath));
            }

            currentConcat.setLength(prevLen);
        }
    }

    /*
     * ✅ Time and Space Complexity
     * Let:
     * - n = number of keys across the JSON
     * - d = maximum depth of the tree
     * - L = max length of concatenated path
     * - k = number of matches
     * - P = length of the search phrase
     * 
     * Metric Complexity
     * Time O(n × (L + P))
     * Space O(d + k × d + L)
     */

}
