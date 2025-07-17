import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Given a list of folders folder, return the folders after removing all
 * sub-folders in those folders. You may return the answer in any order.
 * 
 * If a folder[i] is located within another folder[j], it is called a sub-folder
 * of it. A sub-folder of folder[j] must start with folder[j], followed by a
 * "/". For example, "/a/b" is a sub-folder of "/a", but "/b" is not a
 * sub-folder of "/a/b/c".
 * 
 * The format of a path is one or more concatenated strings of the form: '/'
 * followed by one or more lowercase English letters.
 * 
 * For example, "/leetcode" and "/leetcode/problems" are valid paths while an
 * empty string and "/" are not.
 * 
 * Example 1:
 * Input: folder = ["/a","/a/b","/c/d","/c/d/e","/c/f"]
 * Output: ["/a","/c/d","/c/f"]
 * Explanation: Folders "/a/b" is a subfolder of "/a" and "/c/d/e" is inside of
 * folder "/c/d" in our filesystem.
 * 
 * Example 2:
 * Input: folder = ["/a","/a/b/c","/a/b/d"]
 * Output: ["/a"]
 * Explanation: Folders "/a/b/c" and "/a/b/d" will be removed because they are
 * subfolders of "/a".
 * 
 * Example 3:
 * Input: folder = ["/a/b/c","/a/b/ca","/a/b/d"]
 * Output: ["/a/b/c","/a/b/ca","/a/b/d"]
 * 
 * Constraints:
 * 1 <= folder.length <= 4 * 104
 * 2 <= folder[i].length <= 100
 * folder[i] contains only lowercase letters and '/'.
 * folder[i] always starts with the character '/'.
 * Each folder name is unique.
 */

class Solution {

    // Time Complexity: O(n log n + n²m) where n is the number of folders and m is
    // the average path length
    // Space Complexity: O(n) for the result list
    public List<String> removeSubfolders(String[] folder) {
        // Sort the folders lexicographically
        Arrays.sort(folder);

        List<String> result = new ArrayList<>();

        for (String currentFolder : folder) {
            // Check if current folder is a sub-folder of any folder in result
            boolean isSubfolder = false;

            for (String parentFolder : result) {
                // Check if currentFolder starts with parentFolder + "/"
                if (currentFolder.startsWith(parentFolder + "/")) {
                    isSubfolder = true;
                    break;
                }
            }

            // If not a sub-folder, add to result
            if (!isSubfolder) {
                result.add(currentFolder);
            }
        }

        return result;
    }

    // Test method
    public static void main(String[] args) {
        Solution solution = new Solution();

        // Test case 1
        String[] folder1 = { "/a", "/a/b", "/c/d", "/c/d/e", "/c/f" };
        System.out.println("Input: " + Arrays.toString(folder1));
        System.out.println("Output: " + solution.removeSubfolders(folder1));
        System.out.println();

        // Test case 2
        String[] folder2 = { "/a", "/a/b/c", "/a/b/d" };
        System.out.println("Input: " + Arrays.toString(folder2));
        System.out.println("Output: " + solution.removeSubfolders(folder2));
        System.out.println();

        // Test case 3
        String[] folder3 = { "/a/b/c", "/a/b/ca", "/a/b/d" };
        System.out.println("Input: " + Arrays.toString(folder3));
        System.out.println("Output: " + solution.removeSubfolders(folder3));
    }

}

/*
 * Alternative optimized solution using Set for faster lookups:
 */
class OptimizedSolution {

    // Time Complexity: O(nm) where n is the number of folders and m is the average
    // path length
    // Space Complexity: O(n) for the set and result
    public List<String> removeSubfolders(String[] folder) {
        Set<String> folderSet = new HashSet<>(Arrays.asList(folder));
        List<String> result = new ArrayList<>();

        for (String currentFolder : folder) {
            boolean isSubfolder = false;
            String path = currentFolder;

            // Check all possible parent paths
            while (!path.isEmpty()) {
                int lastSlash = path.lastIndexOf('/');
                if (lastSlash == 0)
                    break; // Reached root level

                path = path.substring(0, lastSlash);
                if (folderSet.contains(path)) {
                    isSubfolder = true;
                    break;
                }
            }

            if (!isSubfolder) {
                result.add(currentFolder);
            }
        }

        return result;
    }

}

class Solution2 {

    static class TrieNode {
        Map<String, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
    }

    // Time: O(N * L), where:
    // N is the number of folders.
    // L is the average number of segments per folder.
    // Space: O(N * L) for the Trie and result storage.

    public List<String> removeSubfolders(String[] folder) {
        Arrays.sort(folder); // Sort lexicographically
        TrieNode root = new TrieNode();
        List<String> result = new ArrayList<>();

        for (String path : folder) {
            String[] parts = path.split("/");
            TrieNode curr = root;
            boolean isSubfolder = false;

            // Traverse the path
            for (int i = 1; i < parts.length; i++) {
                if (curr.isEnd) {
                    isSubfolder = true;
                    break; // Don't insert subfolders of already marked folders
                }
                curr = curr.children.computeIfAbsent(parts[i], k -> new TrieNode());
            }

            if (!isSubfolder) {
                curr.isEnd = true;
                result.add(path);
            }
        }

        return result;
    }

}
