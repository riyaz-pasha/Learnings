import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

class LFUCache {

    /* ---------------- Node ---------------- */
    private static class Node {
        int key, value, freq;
        Node prev, next;

        Node(int key, int value) {
            this.key = key;
            this.value = value;
            this.freq = 1;
        }
    }

    /* -------- Doubly Linked List -------- */
    private static class DoublyLinkedList {
        Node head, tail;
        int size;

        DoublyLinkedList() {
            head = new Node(0, 0); // dummy
            tail = new Node(0, 0); // dummy
            head.next = tail;
            tail.prev = head;
            size = 0;
        }

        void addToHead(Node node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
            size++;
        }

        void remove(Node node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            size--;
        }

        Node removeTail() {
            if (size == 0) return null;
            Node node = tail.prev;
            remove(node);
            return node;
        }
    }

    /* ---------------- LFU Cache ---------------- */
    private final int capacity;
    private int minFreq;

    private final Map<Integer, Node> nodeMap;          // key -> node
    private final Map<Integer, DoublyLinkedList> freqMap; // freq -> DLL

    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.minFreq = 0;
        this.nodeMap = new HashMap<>();
        this.freqMap = new HashMap<>();
    }

    /* ---------------- GET ---------------- */
    public int get(int key) {
        if (!nodeMap.containsKey(key)) {
            return -1;
        }
        Node node = nodeMap.get(key);
        updateFrequency(node);
        return node.value;
    }

    /* ---------------- PUT ---------------- */
    public void put(int key, int value) {
        if (capacity == 0) return;

        if (nodeMap.containsKey(key)) {
            Node node = nodeMap.get(key);
            node.value = value;
            updateFrequency(node);
            return;
        }

        if (nodeMap.size() == capacity) {
            DoublyLinkedList minFreqList = freqMap.get(minFreq);
            Node toRemove = minFreqList.removeTail();
            nodeMap.remove(toRemove.key);
        }

        Node newNode = new Node(key, value);
        nodeMap.put(key, newNode);

        freqMap
            .computeIfAbsent(1, f -> new DoublyLinkedList())
            .addToHead(newNode);

        minFreq = 1;
    }

    /* -------- Frequency Update -------- */
    private void updateFrequency(Node node) {
        int freq = node.freq;
        DoublyLinkedList oldList = freqMap.get(freq);
        oldList.remove(node);

        if (freq == minFreq && oldList.size == 0) {
            minFreq++;
        }

        node.freq++;

        freqMap
            .computeIfAbsent(node.freq, f -> new DoublyLinkedList())
            .addToHead(node);
    }

    /* ---------------- MAIN (Test) ---------------- */
    public static void main(String[] args) {
        LFUCache cache = new LFUCache(2);

        cache.put(1, 1);
        cache.put(2, 2);
        System.out.println(cache.get(1)); // 1

        cache.put(3, 3); // evicts key 2
        System.out.println(cache.get(2)); // -1
        System.out.println(cache.get(3)); // 3

        cache.put(4, 4); // evicts key 1
        System.out.println(cache.get(1)); // -1
        System.out.println(cache.get(3)); // 3
        System.out.println(cache.get(4)); // 4
    }
}


/**
 * Simple LFU Cache - evicts least frequently used items
 */
class LFUCache2<K, V> {
    
    class Node {
        K key;
        V value;
        int freq = 1;
    }
    
    private final int capacity;
    private int minFreq = 0;
    private final Map<K, Node> cache = new HashMap<>();
    private final Map<Integer, LinkedHashSet<K>> freqMap = new HashMap<>();
    
    public LFUCache2(int capacity) {
        this.capacity = capacity;
    }
    
    public V get(K key) {
        Node node = cache.get(key);
        if (node == null) return null;
        
        updateFreq(node);
        return node.value;
    }
    
    public void put(K key, V value) {
        if (capacity == 0) return;
        
        Node node = cache.get(key);
        
        if (node != null) {
            node.value = value;
            updateFreq(node);
        } else {
            if (cache.size() >= capacity) {
                evict();
            }
            
            Node newNode = new Node();
            newNode.key = key;
            newNode.value = value;
            cache.put(key, newNode);
            freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
            minFreq = 1;
        }
    }
    
    private void updateFreq(Node node) {
        int oldFreq = node.freq;
        freqMap.get(oldFreq).remove(node.key);
        
        if (freqMap.get(oldFreq).isEmpty()) {
            freqMap.remove(oldFreq);
            if (minFreq == oldFreq) minFreq++;
        }
        
        node.freq++;
        freqMap.computeIfAbsent(node.freq, k -> new LinkedHashSet<>()).add(node.key);
    }
    
    private void evict() {
        K evictKey = freqMap.get(minFreq).iterator().next();
        freqMap.get(minFreq).remove(evictKey);
        cache.remove(evictKey);
    }
    
    public static void main(String[] args) {
        LFUCache2<Integer, String> cache = new LFUCache2<>(2);
        
        cache.put(1, "A");
        cache.put(2, "B");
        System.out.println(cache.get(1));  // A, freq=2
        
        cache.put(3, "C");  // evicts key 2 (freq=1)
        System.out.println(cache.get(2));  // null
        System.out.println(cache.get(3));  // C
        
        cache.put(4, "D");  // evicts key 3 (freq=2, but older)
        System.out.println(cache.get(1));  // A
        System.out.println(cache.get(3));  // null
        System.out.println(cache.get(4));  // D
    }
}
