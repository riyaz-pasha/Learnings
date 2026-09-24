import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * ConsistentHashing
 *
 * Supports:
 *  - addNode(node)
 *  - removeNode(node)
 *  - getNode(key)
 *
 * Uses:
 *  - Hash ring (TreeMap)
 *  - Virtual nodes to improve distribution
 *
 * Time Complexity:
 *  - addNode(): O(V log N)
 *  - removeNode(): O(V log N)
 *  - getNode(): O(log N)
 *
 * Where:
 *  - V = number of virtual nodes per real node
 *  - N = total nodes on ring
 */
class ConsistentHashing {

    // Hash ring: position -> node
    private final TreeMap<Long, String> ring = new TreeMap<>();

    // Number of virtual nodes per physical node
    private final int virtualNodes;

    public ConsistentHashing(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    /**
     * Add a physical node to the ring.
     * Each physical node gets multiple virtual nodes.
     */
    public void addNode(String node) {

        for (int i = 0; i < virtualNodes; i++) {

            String virtualNodeName = node + "#VN" + i;

            long hash = hash(virtualNodeName);

            ring.put(hash, node);
        }
    }

    /**
     * Remove a physical node and its virtual nodes.
     */
    public void removeNode(String node) {

        for (int i = 0; i < virtualNodes; i++) {

            String virtualNodeName = node + "#VN" + i;

            long hash = hash(virtualNodeName);

            ring.remove(hash);
        }
    }

    /**
     * Get node responsible for a key.
     *
     * Steps:
     * 1) Hash the key
     * 2) Find first node clockwise
     * 3) If none found, wrap around to first entry
     */
    public String getNode(String key) {

        if (ring.isEmpty()) {
            return null;
        }

        long hash = hash(key);

        // Find smallest key >= hash
        Map.Entry<Long, String> entry = ring.ceilingEntry(hash);

        // If no such entry, wrap around
        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    /**
     * Hash function:
     * - Uses MD5
     * - Converts first 8 bytes to long
     */
    private long hash(String key) {

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));

            long hash = 0;

            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }

            return hash & 0x7fffffffffffffffL; // ensure positive

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * For debugging: print ring.
     */
    public void printRing() {
        for (Map.Entry<Long, String> entry : ring.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }
}
