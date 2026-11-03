import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

interface HashFunction {

    long hash(String key);

}

class MD5HashFunction implements HashFunction {

    private final MessageDigest md5;

    public MD5HashFunction() {
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }

    @Override
    public long hash(String key) {
        this.md5.reset();
        this.md5.update(key.getBytes());
        byte[] digest = md5.digest();
        // // Take the first 4 bytes and convert to an integer
        // return ((digest[3] & 0xFF) << 24) | ((digest[2] & 0xFF) << 16) |
        // ((digest[1] & 0xFF) << 8) | (digest[0] & 0xFF);
        long h = 0;
        for (int i = 0; i < 8; i++) {
            h <<= 8;
            h |= ((int) digest[i]) & 0xFF;
        }
        return h & 0x7FFFFFFFFFFFFFFFL; // make it positive
    }
}

class ConsistentHashing<T> {

    private final int numberOfReplicas;
    private final HashFunction hashFunction;
    private final SortedMap<Long, T> hashRing = new TreeMap<>();

    public ConsistentHashing(int numberOfReplicas, Collection<T> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        this.hashFunction = new MD5HashFunction();
        for (T node : nodes) {
            this.add(node);
        }
    }

    // Adds a physical node and its virtual nodes to the ring
    public void add(T node) {
        for (int i = 0; i < this.numberOfReplicas; i++) {
            // A virtual node is represented by a hash of "node_ip:i"
            this.hashRing.put(this.hashFunction.hash(node.toString() + i), node);
        }
    }

    // Removes a physical node and its virtual nodes from the ring
    public void remove(T node) {
        for (int i = 0; i < this.numberOfReplicas; i++) {
            this.hashRing.remove(this.hashFunction.hash(node.toString() + i));
        }
    }

    public T get(Object key) {
        if (this.hashRing.isEmpty()) {
            return null;
        }

        long hash = this.hashFunction.hash(key.toString());

        // If the key's hash is greater than all existing hashes, it wraps around
        SortedMap<Long, T> tailMap = this.hashRing.tailMap(hash);
        if (tailMap.isEmpty()) {
            hash = this.hashRing.firstKey();
        } else {
            hash = tailMap.firstKey();
        }
        return this.hashRing.get(hash);
    }

}

// ---

class ConsistentHashingMurmur<T> {

    private final int numberOfReplicas;
    private final SortedMap<Long, T> circle = new TreeMap<>();

    public ConsistentHashingMurmur(int numberOfReplicas, Collection<T> nodes) {
        this.numberOfReplicas = numberOfReplicas;
        for (T node : nodes) {
            add(node);
        }
    }

    public void add(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            long hash = murmurHash3(node.toString() + "#" + i);
            circle.put(hash, node);
        }
    }

    public void remove(T node) {
        for (int i = 0; i < numberOfReplicas; i++) {
            long hash = murmurHash3(node.toString() + "#" + i);
            circle.remove(hash);
        }
    }

    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        long hash = murmurHash3(key.toString());
        if (!circle.containsKey(hash)) {
            SortedMap<Long, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }

    // MurmurHash3 32-bit -> cast to long
    private long murmurHash3(String key) {
        byte[] data = key.getBytes();
        int length = data.length;
        int seed = 0x9747b28c; // Random seed
        int c1 = 0xcc9e2d51;
        int c2 = 0x1b873593;
        int h1 = seed;
        int roundedEnd = (length & 0xfffffffc); // round down to 4 byte block

        for (int i = 0; i < roundedEnd; i += 4) {
            int k1 = (data[i] & 0xff) |
                    ((data[i + 1] & 0xff) << 8) |
                    ((data[i + 2] & 0xff) << 16) |
                    (data[i + 3] << 24);
            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;

            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
        }

        int k1 = 0;
        switch (length & 0x03) {
            case 3:
                k1 = (data[roundedEnd + 2] & 0xff) << 16;
            case 2:
                k1 |= (data[roundedEnd + 1] & 0xff) << 8;
            case 1:
                k1 |= (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= c2;
                h1 ^= k1;
        }

        h1 ^= length;
        h1 ^= (h1 >>> 16);
        h1 *= 0x85ebca6b;
        h1 ^= (h1 >>> 13);
        h1 *= 0xc2b2ae35;
        h1 ^= (h1 >>> 16);

        return h1 & 0x00000000ffffffffL; // Make unsigned
    }

    public static void main(String[] args) {
        List<String> nodes = Arrays.asList("NodeA", "NodeB", "NodeC", "NodeD");
        ConsistentHashingMurmur<String> ch = new ConsistentHashingMurmur<>(100, nodes);

        // Simulate key distribution
        Map<String, Integer> load = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            String node = ch.get("Key" + i);
            load.put(node, load.getOrDefault(node, 0) + 1);
        }

        System.out.println("Initial distribution:");
        load.forEach((n, count) -> System.out.println(n + " -> " + count));

        // Remove a node
        ch.remove("NodeB");
        load.clear();
        for (int i = 0; i < 10000; i++) {
            String node = ch.get("Key" + i);
            load.put(node, load.getOrDefault(node, 0) + 1);
        }

        System.out.println("\nAfter removing NodeB:");
        load.forEach((n, count) -> System.out.println(n + " -> " + count));
    }

}
