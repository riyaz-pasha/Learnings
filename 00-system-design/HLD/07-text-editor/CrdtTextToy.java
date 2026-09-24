import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple RGA-style operation-based CRDT for text (toy example).
 *
 * - Each inserted char has an Id (siteId, counter).
 * - Insert references a prev id (the element after which to insert).
 * - Concurrent inserts after the same prev are ordered using Id comparator
 * (counter, siteId).
 * - Delete sets tombstone flag; tombstones are not GC'd in this toy.
 */

/*
 * -----------------------
 * Id class
 * -----------------------
 */
class Id implements Comparable<Id> {

    public final String siteId;
    public final long counter;

    public Id(String siteId, long counter) {
        this.siteId = siteId;
        this.counter = counter;
    }

    @Override
    public int compareTo(Id other) {
        int cmp = Long.compare(this.counter, other.counter);
        if (cmp != 0)
            return cmp;
        return this.siteId.compareTo(other.siteId);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Id))
            return false;
        Id other = (Id) o;
        return this.counter == other.counter && this.siteId.equals(other.siteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, counter);
    }

    @Override
    public String toString() {
        return "(" + siteId + "," + counter + ")";
    }

}

/*
 * -----------------------
 * Element (character)
 * -----------------------
 */
class Element {

    public final Id id;
    public final char value;
    public final Id prev; // id of element after which this was inserted (HEAD if inserted at beginning)
    public boolean deleted = false;

    public Element(Id id, char value, Id prev) {
        this.id = id;
        this.value = value;
        this.prev = prev;
    }

    @Override
    public String toString() {
        return String.format("%s:'%c'%s", id, value, deleted ? "[del]" : "");
    }

}

/*
 * -----------------------
 * Operations
 * -----------------------
 */
abstract class Operation {

    public final String type;
    public final Id causalDot; // optional metadata for causal tracking (not used heavily in this toy)

    protected Operation(String type, Id causalDot) {
        this.type = type;
        this.causalDot = causalDot;
    }

}

class InsertOp extends Operation {

    public final Element element;

    public InsertOp(Element element, Id causalDot) {
        super("insert", causalDot);
        this.element = element;
    }

    @Override
    public String toString() {
        return "InsertOp{" + element + " prev=" + element.prev + "}";
    }

}

class DeleteOp extends Operation {

    public final Id target;

    public DeleteOp(Id target, Id causalDot) {
        super("delete", causalDot);
        this.target = target;
    }

    @Override
    public String toString() {
        return "DeleteOp{" + target + "}";
    }

}

/*
 * -----------------------
 * CRDT Document (replica)
 * -----------------------
 */
class CRDTDocument {

    // Special HEAD id representing start of document
    public static final Id HEAD = new Id("_HEAD_", 0);

    // map id -> element (includes tombstones)
    private final Map<Id, Element> elements = new HashMap<>();

    // children lists: prevId -> sorted list of element ids that were inserted after
    // prevId
    // we store the element references directly for simplicity
    private final Map<Id, List<Element>> children = new HashMap<>();

    // generator for local ids
    private final AtomicLong localCounter = new AtomicLong(1);
    private final String siteId;

    // log of local operations (so we can "exchange" them in the demo)
    private final List<Operation> localOpLog = new ArrayList<>();

    public CRDTDocument(String siteId) {
        this.siteId = siteId;
        // initialize children map for HEAD
        children.put(HEAD, new ArrayList<>());
        // (HEAD itself is not an Element stored in elements map)
    }

    public Id nextId() {
        return new Id(siteId, localCounter.getAndIncrement());
    }

    /* Local API: create and broadcast (here we just record op locally) */
    public InsertOp localInsert(char c, Id prev) {
        Id id = nextId();
        Element e = new Element(id, c, prev);
        InsertOp op = new InsertOp(e, id); // causalDot = this op's id (simple)
        applyInsert(e); // apply locally
        localOpLog.add(op);
        return op;
    }

    public DeleteOp localDelete(Id target) {
        DeleteOp op = new DeleteOp(target, new Id(siteId, localCounter.getAndIncrement()));
        applyDelete(target);
        localOpLog.add(op);
        return op;
    }

    /* Apply remote or local ops (idempotent) */
    public void applyInsert(Element e) {
        // If we've already seen it, ignore
        if (elements.containsKey(e.id))
            return;

        // Ensure prev has children list (if prev unknown, create placeholder list;
        // the element will still be inserted after that prev once prev arrives,
        // but here we keep a children bucket to maintain ordering even if prev
        // missing).
        children.computeIfAbsent(e.prev, k -> new ArrayList<>());

        // store element
        elements.put(e.id, e);

        // add into children[e.prev] in sorted order by element.id
        List<Element> list = children.get(e.prev);
        // insertion point: keep list sorted by id (compare counter then siteId)
        int idx = Collections.binarySearch(list, e, Comparator.comparing(el -> el.id));
        if (idx < 0)
            idx = -idx - 1;
        list.add(idx, e);

        // ensure this element has its own children bucket
        children.computeIfAbsent(e.id, k -> new ArrayList<>());
    }

    public void applyDelete(Id id) {
        Element e = elements.get(id);
        if (e != null) {
            e.deleted = true;
        } else {
            // Received delete before insert. We still need to remember tombstone info.
            // Create a placeholder element with deleted=true and store it so that
            // future inserts referencing this id can still work.
            // For this toy, we will create a placeholder with value '\0' and prev=HEAD
            // (unknown).
            Element placeholder = new Element(id, '\0', HEAD);
            placeholder.deleted = true;
            elements.put(id, placeholder);
            children.computeIfAbsent(id, k -> new ArrayList<>());
            // Also ensure there's a children bucket for HEAD (or placeholder.prev).
            children.computeIfAbsent(placeholder.prev, k -> new ArrayList<>());
        }
    }

    /* Receive operation (simulate network delivery) */
    public void receiveOperation(Operation op) {
        switch (op) {
            case InsertOp insertOp -> applyInsert(insertOp.element);
            case DeleteOp deleteOp -> applyDelete(deleteOp.target);
            default -> throw new IllegalArgumentException("Unknown op type: " + op);
        }
    }

    /* Render visible string by traversing from HEAD */
    public String render() {
        StringBuilder sb = new StringBuilder();
        traverseAndCollect(HEAD, sb);
        return sb.toString();
    }

    private void traverseAndCollect(Id prev, StringBuilder sb) {
        List<Element> childs = children.get(prev);
        if (childs == null)
            return;
        for (Element e : childs) {
            if (!e.deleted && e.value != '\0') {
                sb.append(e.value);
            }
            // recurse into children inserted after this element
            traverseAndCollect(e.id, sb);
        }
    }

    /* Utility: get local op log for exchange */
    public List<Operation> takeLocalOpLog() {
        // return a copy and then clear local log to simulate "sent" ops
        List<Operation> copy = new ArrayList<>(localOpLog);
        localOpLog.clear();
        return copy;
    }

    /* Fast debug string of internal structure */
    public String debugStructure() {
        StringBuilder sb = new StringBuilder();
        sb.append("Elements:\n");
        elements.values().stream()
                .sorted(Comparator.comparing(el -> el.id))
                .forEach(el -> sb.append("  ").append(el).append(" prev=").append(el.prev).append("\n"));
        sb.append("Children lists:\n");
        for (Map.Entry<Id, List<Element>> e : children.entrySet()) {
            sb.append("  after ").append(e.getKey()).append(" -> ");
            e.getValue().forEach(child -> sb.append(child.id).append(child.deleted ? "[del]" : "").append(" "));
            sb.append("\n");
        }
        return sb.toString();
    }
}

public class CrdtTextToy {

    /*
     * -----------------------
     * Demo / main
     * -----------------------
     */
    public static void main(String[] args) {
        // Two replicas
        CRDTDocument replicaA = new CRDTDocument("A");
        CRDTDocument replicaB = new CRDTDocument("B");

        // Replica A inserts 'H' at head
        InsertOp aInsH = replicaA.localInsert('H', CRDTDocument.HEAD);
        // Replica B inserts 'X' at head concurrently (before seeing A's op)
        InsertOp bInsX = replicaB.localInsert('X', CRDTDocument.HEAD);

        // Exchange operations (simple "send ops" simulation)
        // A sends its ops to B, B sends its ops to A
        exchangeOps(replicaA, replicaB);

        System.out.println("After exchanging inserts:");
        System.out.println("A renders: " + replicaA.render());
        System.out.println("B renders: " + replicaB.render());
        System.out.println("--- internal structures A ---");
        System.out.println(replicaA.debugStructure());

        // Now A inserts 'Y' after H
        // Need to find id of H in A
        Id hId = aInsH.element.id;
        replicaA.localInsert('Y', hId);

        // A deletes H
        replicaA.localDelete(hId);

        // Exchange ops again
        exchangeOps(replicaA, replicaB);

        System.out.println("\nAfter A inserts Y and deletes H, and exchange:");
        System.out.println("A renders: " + replicaA.render());
        System.out.println("B renders: " + replicaB.render());
        System.out.println("--- internal structures B ---");
        System.out.println(replicaB.debugStructure());

        // Final expectation: H inserted then deleted => tombstoned; visible string
        // depends on ordering:
        // If tie-break orders (A1) < (B1), initial merged order was H X,
        // After delete of H, final visible should be "YX" or "XY" depending on Y's
        // placement.
    }

    // helper to simulate exchanging local ops between two replicas
    private static void exchangeOps(CRDTDocument a, CRDTDocument b) {
        List<Operation> opsA = a.takeLocalOpLog();
        List<Operation> opsB = b.takeLocalOpLog();

        // deliver A -> B
        for (Operation op : opsA) {
            b.receiveOperation(op);
        }
        // deliver B -> A
        for (Operation op : opsB) {
            a.receiveOperation(op);
        }
    }

}
