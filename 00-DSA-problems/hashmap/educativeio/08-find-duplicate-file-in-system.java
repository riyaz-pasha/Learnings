/**
 * ============================================================================
 * 609. FIND DUPLICATE FILE IN SYSTEM
 * ============================================================================
 * 
 * ## 1. Clarifying questions
 * 1. "Can file contents contain spaces or parenthesis?" 
 *    (Crucial for string parsing. If contents have spaces, `split(" ")` will break. 
 *    Assuming typical LeetCode constraints, they don't, but in a real system we'd need robust parsing or regex).
 * 2. "In a real-world scenario, can the files be gigabytes in size?" 
 *    (Massively changes the approach. If yes, reading full contents into a HashMap will cause an OutOfMemoryError).
 * 3. "Are we guaranteed that the paths are valid and there are no cyclic symlinks?"
 *    (Prevents infinite loops when traversing real file systems).
 * 4. "Is it possible for all files in the system to be unique?"
 *    (Yes, in which case we return an empty list. Ensures we remember to filter out groups of size 1).
 * 5. "Does the order of the output groups or the files within them matter?"
 *    (Prompt says no, which gives us freedom to use HashMaps without worrying about insertion order or sorting).
 * 
 * 
 * ## 2. The reasoning journey
 * > Core Constraint: We need to group items by their identical contents. The bottleneck is the cost 
 * > of comparing large strings (file contents) against each other repeatedly.
 * 
 * ### Approach 1: The "List of Groups" (Brute Force)
 * 1. What I'd naturally try: I would parse every file and extract its `content` and `fullPath`. 
 *    I'd keep a `List<FileGroup>` where each group holds a unique `content` and a list of paths. 
 *    For every new file, I iterate through all existing groups. If the content matches, I add the path. 
 *    If no group matches, I create a new group.
 * 2. Why it works: It directly simulates the definition of grouping by comparing every file to known unique contents.
 * 3. Why it's too costly: 
 *    - Time Complexity: O(N^2 * L) — where N is the total number of files and L is the average length 
 *      of the content. For every new file, we do string comparisons against potentially all previously seen files.
 *    - Space Complexity: O(N * L) — because we store the content and path for every file in our groups.
 * 4. What work is being repeated: We are repeatedly scanning through unrelated groups and doing full 
 *    string comparisons. If a file's content is "abcd", we shouldn't need to manually check if it equals 
 *    "xyz", "qwe", etc., one by one.
 * 5. What property removes the bottleneck: Hashing. A HashMap can jump instantly to the exact bucket 
 *    for "abcd" without comparing it to every other string.
 * 
 * ### Approach 2: The Content-to-Path Dictionary (HashMap - Optimal for Memory-Resident Data)
 * 1. What I'd naturally try: Parse the strings. Use `HashMap<String, List<String>>` where the key is 
 *    the file's `content` and the value is a list of its `fullPaths`.
 * 2. Why it works: HashMaps provide O(1) average lookup. Instead of asking "Do you match group 1? group 2?", 
 *    we ask "Where is the group for 'abcd'?", and jump right to it.
 * 3. Cost Breakdown:
 *    - Time Complexity: O(N * L) — where N is the number of files and L is the max length of a path/content. 
 *      We visit each character of the input exactly once during parsing, and calculating the hash code 
 *      for the content takes O(L) time.
 *    - Space Complexity: O(N * L) — because the HashMap stores every file's content and its full constructed 
 *      path exactly once.
 * 
 * > **Interview Strategy:** In a standard algorithmic interview, Approach 2 (HashMap) is exactly what they 
 * > want you to code. I would write this approach. However, this specific problem is a famous Trojan Horse 
 * > for Systems Design follow-ups (see Section 5). I would code the HashMap, but verbally note that 
 * > "This assumes contents fit in RAM."
 * 
 * 
 * ## 3. Edge cases
 * - No duplicates at all: `{"root/a 1.txt(a) 2.txt(b)"}` -> The map will have lists of size 1. 
 *   Must filter these out before returning!
 * - Empty contents: `{"root/a 1.txt()"}` -> Valid string, parsed as empty content `""`. Handles normally.
 * - Single directory, multiple identical files: `{"root 1.txt(a) 2.txt(a)"}` -> Outputs `[["root/1.txt", "root/2.txt"]]`.
 * 
 * 
 * ## 4. Key Insight & Dry Run
 * 
 * **Key Insight:** "The identifier IS the Key." 
 * By using the content as the HashMap key, the data structure inherently performs the grouping for us. 
 * No nested loops required.
 * 
 * **Dry Run of Approach 2:**
 * Input: `["root/a 1.txt(abcd) 2.txt(efgh)", "root/c 3.txt(abcd)"]`
 * 
 * Iteration 1: `path = "root/a 1.txt(abcd) 2.txt(efgh)"`
 * - Split: `["root/a", "1.txt(abcd)", "2.txt(efgh)"]`
 * - File 1: name="1.txt", content="abcd". 
 *   Map state: `{"abcd": ["root/a/1.txt"]}`
 * - File 2: name="2.txt", content="efgh". 
 *   Map state: `{"abcd": ["root/a/1.txt"], "efgh": ["root/a/2.txt"]}`
 * 
 * Iteration 2: `path = "root/c 3.txt(abcd)"`
 * - Split: `["root/c", "3.txt(abcd)"]`
 * - File 1: name="3.txt", content="abcd".
 *   Map state: `{"abcd": ["root/a/1.txt", "root/c/3.txt"], "efgh": ["root/a/2.txt"]}`
 * 
 * Final Filter:
 * - "abcd" list has size 2. Add to result.
 * - "efgh" list has size 1. Ignore.
 * Return `[["root/a/1.txt", "root/c/3.txt"]]`.
 * 
 * 
 * ## 5. Follow-ups (CRITICAL FOR THIS PROBLEM)
 * - **Q: What if the files are extremely large (e.g., 10 GB) and you can't load them into memory?**
 *   **A:** We cannot use file content as the map key. Instead, we use a multi-pass approach:
 *   1. Group by file size (files of different sizes can't be duplicates).
 *   2. For files with the same size, hash a small chunk (e.g., first 1KB) using MD5/SHA-256. 
 *   3. If partial hashes match, hash the full file.
 *   4. Only if full hashes match do we do a byte-by-byte comparison to rule out hash collisions.
 * 
 * - **Q: What if you can only read the file by 1KB chunks?**
 *   **A:** We maintain a map of `Hash -> List<Files>`. We read the first 1KB of all suspected duplicates, 
 *   hash them, and group them. If a group has size > 1, we read the *next* 1KB for those files, hash 
 *   the cumulative bytes, and regroup. This progressively eliminates non-duplicates without reading 
 *   whole files unless necessary.
 * 
 * - **Q: What is the time complexity of the real-world hashing approach?**
 *   **A:** Worst-case O(N^2 * L) if every file has the exact same size, same hash, and we must do 
 *   byte-by-byte comparisons (due to malicious collisions). But practically O(N), as size and 
 *   partial hashes aggressively prune the search space.
 * 
 * ============================================================================
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindDuplicateFilesStudy {

    interface DuplicateFileFinder {
        List<List<String>> findDuplicate(String[] paths);
    }

    // ========================================================================
    // APPROACH 2: HASHMAP (The Optimal Algorithmic Solution)
    // ========================================================================
    static class HashMapFinder implements DuplicateFileFinder {
        @Override
        public List<List<String>> findDuplicate(String[] paths) {
            // Maps file content (String) to a List of full file paths.
            // Concrete example: "abcd" -> ["root/a/1.txt", "root/c/3.txt"]
            Map<String, List<String>> contentToPaths = new HashMap<>();

            for (String path : paths) {
                // Split by space. 
                // parts[0] is the directory path. parts[1...N] are the files.
                String[] parts = path.split(" ");
                String rootDir = parts[0];

                // Process each file in the current directory
                for (int i = 1; i < parts.length; i++) {
                    String fileInfo = parts[i];
                    
                    // Format is "filename.txt(content)"
                    // Find where the parenthesis starts to separate name and content.
                    int openBracketIdx = fileInfo.indexOf('(');
                    
                    // Name is everything before '('
                    String fileName = fileInfo.substring(0, openBracketIdx);
                    // Content is everything between '(' and ')'
                    String content = fileInfo.substring(openBracketIdx + 1, fileInfo.length() - 1);
                    
                    // Construct the full absolute path
                    String fullPath = rootDir + "/" + fileName;
                    
                    // Add to our map. computeIfAbsent handles the initialization 
                    // of the ArrayList if this is the first time we've seen this content.
                    contentToPaths
                        .computeIfAbsent(content, k -> new ArrayList<>())
                        .add(fullPath);
                }
            }

            // We only care about DUPLICATES, so we filter groups with less than 2 files.
            List<List<String>> duplicates = new ArrayList<>();
            for (List<String> group : contentToPaths.values()) {
                if (group.size() > 1) {
                    duplicates.add(group);
                }
            }

            return duplicates;
        }
    }

    // ========================================================================
    // APPROACH 1: BRUTE FORCE (Conceptual Implementation)
    // ========================================================================
    static class BruteForceFinder implements DuplicateFileFinder {
        
        // A simple class to hold our grouped state
        static class FileGroup {
            String content;
            List<String> paths = new ArrayList<>();
            FileGroup(String content) { this.content = content; }
        }

        @Override
        public List<List<String>> findDuplicate(String[] paths) {
            List<FileGroup> groups = new ArrayList<>();

            for (String path : paths) {
                String[] parts = path.split(" ");
                String rootDir = parts[0];

                for (int i = 1; i < parts.length; i++) {
                    int openBracketIdx = parts[i].indexOf('(');
                    String fileName = parts[i].substring(0, openBracketIdx);
                    String content = parts[i].substring(openBracketIdx + 1, parts[i].length() - 1);
                    String fullPath = rootDir + "/" + fileName;

                    // The Bottleneck: Linear scan over all known unique contents
                    boolean found = false;
                    for (FileGroup group : groups) {
                        if (group.content.equals(content)) { // O(L) comparison
                            group.paths.add(fullPath);
                            found = true;
                            break;
                        }
                    }

                    // If we didn't find a matching group, create a new one
                    if (!found) {
                        FileGroup newGroup = new FileGroup(content);
                        newGroup.paths.add(fullPath);
                        groups.add(newGroup);
                    }
                }
            }

            // Filter for duplicates
            List<List<String>> duplicates = new ArrayList<>();
            for (FileGroup group : groups) {
                if (group.paths.size() > 1) {
                    duplicates.add(group.paths);
                }
            }

            return duplicates;
        }
    }

    // ========================================================================
    // TESTS AND DRY RUNS
    // ========================================================================
    public static void main(String[] args) {
        DuplicateFileFinder[] finders = {
            new HashMapFinder(),
            new BruteForceFinder()
        };

        String[][] testInputs = {
            // Test 1: Standard case with multiple duplicates
            {"root/a 1.txt(abcd) 2.txt(efgh)", "root/c 3.txt(abcd)", "root/c/d 4.txt(efgh)", "root 4.txt(efgh)"},
            
            // Test 2: Only one file per unique content (Should return empty)
            {"root/a 1.txt(abcd) 2.txt(efgh)", "root/c 3.txt(ijkl)"},
            
            // Test 3: Empty file contents
            {"root 1.txt() 2.txt() 3.txt(a)"}
        };

        for (DuplicateFileFinder finder : finders) {
            System.out.println("Testing " + finder.getClass().getSimpleName() + "...");
            
            for (int i = 0; i < testInputs.length; i++) {
                List<List<String>> result = finder.findDuplicate(testInputs[i]);
                System.out.println("  Test " + (i + 1) + " Groups found: " + result.size());
                for (List<String> group : result) {
                    System.out.println("    -> " + group);
                }
            }
            System.out.println();
        }
    }
}

/**
 * ============================================================================
 * SUMMARY
 * ============================================================================
 * - Core pattern: Dictionary grouping (HashMap bucket aggregation).
 * - Key observation: File content acts perfectly as a hash key to bucket identical files together.
 * - What's worth memorizing: The string parsing logic. Using `.indexOf('(')` is much safer and 
 *   faster than trying to use complex regex for this specific structured format.
 * - Most common trap: Forgetting to filter out the Map entries that only have a list size of 1. 
 *   (Unique files are NOT duplicates).
 * - System Design Trigger: "File Content." If an interview mentions files, *always* question 
 *   if the file sizes fit in RAM, and immediately pivot to SHA-256 Hashing / Chunking if they don't.
 * ============================================================================
 */


import java.util.*;

/**
 * Problem:
 * --------
 * Given directory information with file names and contents,
 * find all duplicate files (files having EXACT SAME CONTENT).
 *
 * Input Example:
 * [
 *  "root/a 1.txt(abcd) 2.txt(efgh)",
 *  "root/c 3.txt(abcd)",
 *  "root/c/d 4.txt(efgh)",
 *  "root 4.txt(efgh)"
 * ]
 *
 * Output:
 * [
 *   ["root/a/1.txt", "root/c/3.txt"],
 *   ["root/a/2.txt", "root/c/d/4.txt", "root/4.txt"]
 * ]
 *
 * ----------------------------------------------------------
 * 💡 CORE IDEA:
 * ----------------------------------------------------------
 * Instead of comparing every file with every other file (O(N^2)),
 * we "GROUP" files by their CONTENT.
 *
 * So:
 *   content → list of file paths
 *
 * Duplicate files = groups where size > 1
 *
 * ----------------------------------------------------------
 * 🧠 INTERVIEW THINKING:
 * ----------------------------------------------------------
 * 1. Duplicate ⇒ grouping problem
 * 2. Grouping ⇒ HashMap
 * 3. Key ⇒ what defines duplicate? → CONTENT
 *
 * ----------------------------------------------------------
 */
public class FindDuplicateFiles {

    public static void main(String[] args) {

        String[] paths = {
            "root/a 1.txt(abcd) 2.txt(efgh)",
            "root/c 3.txt(abcd)",
            "root/c/d 4.txt(efgh)",
            "root 4.txt(efgh)"
        };

        Solution sol = new Solution();
        List<List<String>> result = sol.findDuplicate(paths);

        System.out.println("Duplicate File Groups:");
        result.forEach(System.out::println);
    }
}

/**
 * Solution Class
 */
class Solution {

    public List<List<String>> findDuplicate(String[] paths) {

        /**
         * Map:
         * key   = file content
         * value = list of file paths having that content
         *
         * Example:
         * "abcd" → ["root/a/1.txt", "root/c/3.txt"]
         */
        Map<String, List<String>> contentMap = new HashMap<>();

        /**
         * STEP 1: Iterate through each directory string
         */
        for (String path : paths) {

            /**
             * Example string:
             * "root/a 1.txt(abcd) 2.txt(efgh)"
             *
             * Split by space:
             * ["root/a", "1.txt(abcd)", "2.txt(efgh)"]
             */
            String[] parts = path.split(" ");

            // First part is directory path
            String directory = parts[0];

            /**
             * STEP 2: Process each file in that directory
             */
            for (int i = 1; i < parts.length; i++) {

                String fileInfo = parts[i];

                /**
                 * Example:
                 * "1.txt(abcd)"
                 *
                 * We need:
                 * fileName = "1.txt"
                 * content  = "abcd"
                 */

                int openBracket = fileInfo.indexOf('(');
                int closeBracket = fileInfo.indexOf(')');

                // Extract file name
                String fileName = fileInfo.substring(0, openBracket);

                // Extract content inside parentheses
                String content = fileInfo.substring(openBracket + 1, closeBracket);

                /**
                 * Build full path:
                 * "root/a" + "/" + "1.txt"
                 */
                String fullPath = directory + "/" + fileName;

                /**
                 * STEP 3: Group by content
                 *
                 * If content already exists → append
                 * Else → create new list
                 */
                contentMap
                        .computeIfAbsent(content, k -> new ArrayList<>())
                        .add(fullPath);
            }
        }

        /**
         * STEP 4: Extract only duplicate groups
         * (i.e., groups having more than 1 file)
         */
        List<List<String>> result = new ArrayList<>();

        for (List<String> group : contentMap.values()) {
            if (group.size() > 1) {
                result.add(group);
            }
        }

        return result;
    }
}
