import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FileSystem {

    /*
     * Each node represents exactly one filesystem entry:
     * either a directory or a file.
     *
     * Directory node:
     *   - isFile == false
     *   - children contains its immediate entries
     *
     * File node:
     *   - isFile == true
     *   - content contains its data
     *
     * Why use a unified Node?
     * Both files and directories are reached by following the same
     * path-traversal logic. A boolean distinguishes their behavior,
     * avoiding duplicated traversal code across two separate classes.
     *
     * Why use HashMap for children?
     * We need efficient lookup by a child name. Expected O(1) lookup
     * and insertion are appropriate here. Sorting is performed only
     * when ls() actually needs an ordered result.
     */
    private static final class Node {

        private boolean isFile = false;

        private final Map<String, Node> children = new HashMap<>();

        /*
         * StringBuilder is preferable to repeated immutable String
         * concatenation because addContentToFile() may append many
         * chunks. StringBuilder avoids creating a new String for every
         * append, although growing its internal buffer can occasionally
         * require copying existing characters.
         */
        private final StringBuilder content = new StringBuilder();
    }

    /*
     * The root always exists and represents "/".
     *
     * Every other node is reachable by following child-name mappings
     * from this root. This is the central tree invariant.
     */
    private final Node root = new Node();

    /*
     * Resolve an absolute path to its corresponding node.
     *
     * Example:
     *   "/a/b/c" -> root -> a -> b -> c
     *
     * The leading slash creates an empty component when split using
     * "/". split("/") also discards trailing empty components by
     * default, which is compatible with the problem's path constraints.
     *
     * We skip the leading empty component and follow the remaining names.
     *
     * Preconditions:
     * The requested path exists, and the path is valid, as guaranteed
     * by the problem.
     *
     * Let L be the path length and D its number of components.
     *
     * Time: O(L) expected, because every component is looked up once.
     * Auxiliary space: O(D) for the split array.
     */
    private Node resolve(String path) {
        Node current = root;

        for (String component : path.split("/")) {
            if (component.isEmpty()) {
                continue;
            }

            current = current.children.get(component);
        }

        return current;
    }

    /*
     * Return the final component of a non-root absolute path.
     *
     * Examples:
     *   "/a/b/file.txt" -> "file.txt"
     *   "/a"            -> "a"
     *
     * This helper is used only when ls() targets a file. We cannot
     * return the file's children because files are not directories.
     *
     * Time: O(L) for substring extraction in the worst case.
     * Auxiliary space: O(L) for the resulting String.
     */
    private String fileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /*
     * List the requested path.
     *
     * Case 1: The target is a file.
     * Return only its own name, e.g. ls("/a/x.txt") -> ["x.txt"].
     *
     * Case 2: The target is a directory.
     * Return its immediate child names, sorted lexicographically.
     *
     * We sort a new list rather than sorting the underlying HashMap:
     * the map has no meaningful iteration order, and the contract
     * requires the returned list to be sorted.
     *
     * Let:
     *   L = path length
     *   C = number of immediate children in the target directory
     *
     * Time:
     *   O(L + C log C) expected.
     *   O(L) resolves the path; O(C) collects names; O(C log C)
     *   sorts them.
     *
     * Auxiliary space: O(L + C), including path parsing and output.
     */
    public List<String> ls(String path) {
        Node target = resolve(path);

        if (target.isFile) {
            return List.of(fileName(path));
        }

        List<String> names = new ArrayList<>(target.children.keySet());
        names.sort(String::compareTo);

        return names;
    }

    /*
     * Create the requested directory and any missing ancestors.
     *
     * Example:
     *   mkdir("/a/b/c")
     *
     * If only "/" exists, this creates a, then b, then c.
     * If a or b already exists, we reuse the existing node.
     *
     * Why computeIfAbsent?
     * It combines "look up the child; create it if missing" into
     * one clear operation. Existing nodes are never replaced.
     *
     * Each newly created node defaults to isFile == false, so it
     * represents a directory.
     *
     * Let D be the number of path components and L the path length.
     *
     * Time: O(L) expected, including path parsing and traversal.
     * Auxiliary space: O(D) for parsing, plus O(D) new nodes in
     * the worst case when the entire directory chain is missing.
     */
    public void mkdir(String path) {
        Node current = root;

        for (String component : path.split("/")) {
            if (component.isEmpty()) {
                continue;
            }

            current = current.children.computeIfAbsent(
                component,
                ignored -> new Node()
            );
        }
    }

    /*
     * Create a file if needed and append the supplied content.
     *
     * The problem guarantees that the parent directory exists.
     * Therefore, we resolve the parent and handle only the final
     * component as a file.
     *
     * Example:
     *   addContentToFile("/a/x.txt", "hello")
     *   addContentToFile("/a/x.txt", " world")
     *
     * The final content becomes "hello world", not " world".
     *
     * Why mark isFile explicitly?
     * A newly created Node defaults to a directory. Setting this flag
     * distinguishes the file from a directory with the same node shape.
     *
     * Why not use String concatenation?
     * Repeated immutable concatenation can copy all existing content
     * on every write. StringBuilder is designed for mutable appends.
     *
     * Let:
     *   L = file path length
     *   A = number of characters appended in this call
     *
     * Time: O(L + A) amortized, including traversal and append.
     * Auxiliary space: O(L) for path parsing, plus O(A) additional
     * content capacity amortized; a buffer resize may temporarily
     * require copying existing file content.
     */
    public void addContentToFile(String filePath, String content) {
        int lastSlash = filePath.lastIndexOf('/');

        String parentPath = filePath.substring(0, lastSlash);
        String name = filePath.substring(lastSlash + 1);

        /*
         * For a root-level file such as "/notes.txt", parentPath
         * is the empty string. Resolve the root explicitly.
         */
        Node parent = parentPath.isEmpty()
            ? root
            : resolve(parentPath);

        /*
         * Reuse the file node if it already exists; otherwise create it.
         */
        Node file = parent.children.computeIfAbsent(
            name,
            ignored -> new Node()
        );

        file.isFile = true;
        file.content.append(content);
    }

    /*
     * Return the complete content of an existing file.
     *
     * resolve() navigates to the file node, and StringBuilder.toString()
     * creates the immutable String required by the method signature.
     *
     * Let:
     *   L = file path length
     *   S = total number of characters stored in the file
     *
     * Time: O(L + S), because converting the builder to a String
     * requires copying its characters.
     * Auxiliary space: O(L + S), including path parsing and the
     * returned String.
     */
    public String readContentFromFile(String filePath) {
        return resolve(filePath).content.toString();
    }
}


