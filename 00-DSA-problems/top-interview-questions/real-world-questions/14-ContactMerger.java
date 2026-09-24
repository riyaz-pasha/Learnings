import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Contact {
    int id;
    String name;
    String email;
    String phone;

    public Contact(int id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                '}';
    }
}

class UnionFind {
    private int[] parent;

    public UnionFind(int n) {
        parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i; // Each contact is its own parent initially
        }
    }

    // Find the representative (root) of a set with path compression
    public int find(int i) {
        if (parent[i] == i) {
            return i;
        }
        return parent[i] = find(parent[i]);
    }

    // Merge two sets by union by rank or size
    public void union(int i, int j) {
        int rootI = find(i);
        int rootJ = find(j);
        if (rootI != rootJ) {
            parent[rootJ] = rootI; // Simple union, could be optimized
        }
    }
}

class ContactMerger {

    /*
     * Time: O(N * (E + P))
     * Where N is number of contacts, E is average number of emails, P is average
     * number of phones.
     * 
     * Space: O(N + K) — for Union-Find and email/phone maps, where K is unique
     * keys.
     */
    public List<List<Contact>> mergeContacts(List<Contact> contacts) {
        int n = contacts.size();
        if (n == 0) {
            return new ArrayList<>();
        }

        // Initialize Union-Find structure
        UnionFind uf = new UnionFind(n);

        // Maps to track unique emails and phones
        Map<String, Integer> emailToContactId = new HashMap<>();
        Map<String, Integer> phoneToContactId = new HashMap<>();

        // Iterate through contacts and perform union operations
        for (Contact contact : contacts) {
            int currentId = contact.id;

            // Process email
            if (contact.email != null && !contact.email.isEmpty()) {
                if (emailToContactId.containsKey(contact.email)) {
                    int existingId = emailToContactId.get(contact.email);
                    uf.union(currentId, existingId);
                } else {
                    emailToContactId.put(contact.email, currentId);
                }
            }

            // Process phone
            if (contact.phone != null && !contact.phone.isEmpty()) {
                if (phoneToContactId.containsKey(contact.phone)) {
                    int existingId = phoneToContactId.get(contact.phone);
                    uf.union(currentId, existingId);
                } else {
                    phoneToContactId.put(contact.phone, currentId);
                }
            }
        }

        // Group contacts by their final representative
        Map<Integer, List<Contact>> mergedGroups = new HashMap<>();
        for (Contact contact : contacts) {
            int root = uf.find(contact.id);
            mergedGroups.computeIfAbsent(root, k -> new ArrayList<>()).add(contact);
        }

        // Return the final list of groups
        return new ArrayList<>(mergedGroups.values());
    }

    public static void main(String[] args) {
        List<Contact> contacts = new ArrayList<>();
        // Contact 0: A
        contacts.add(new Contact(0, "Alice", "alice@example.com", "123-456-7890"));
        // Contact 1: B (same email as Alice)
        contacts.add(new Contact(1, "Alison", "alice@example.com", "999-999-9999"));
        // Contact 2: C (same phone as Bob)
        contacts.add(new Contact(2, "Charlie", "charlie@example.com", "111-222-3333"));
        // Contact 3: D (no overlaps with others yet)
        contacts.add(new Contact(3, "David", "david@example.com", "444-555-6666"));
        // Contact 4: Bob (new contact, same phone as Charlie)
        contacts.add(new Contact(4, "Bob", "bob@example.com", "111-222-3333"));

        ContactMerger merger = new ContactMerger();
        List<List<Contact>> result = merger.mergeContacts(contacts);

        System.out.println("Merged Contact Groups:");
        for (List<Contact> group : result) {
            System.out.println("--- Group ---");
            for (Contact contact : group) {
                System.out.println(contact);
            }
        }
    }

}
