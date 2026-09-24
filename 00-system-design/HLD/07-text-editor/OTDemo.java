import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

/**
 * Tiny OT demo:
 * - Operation: Insert or Delete
 * - Server: accepts ops, transforms against log suffix, assigns revision,
 * broadcasts canonical op.
 * - Client: applies local op immediately, keeps outstanding list, sends to
 * server,
 * on receive transforms incoming against outstanding ops then applies.
 *
 * This demo runs two scenarios to show convergence.
 */

// ---------- Operation ----------
class Op {

    enum Type {
        INS,
        DEL
    }

    String opId; // unique ID to match ack
    int siteId; // site that created op (tie-breaker)
    int baseRev; // revision client saw when creating op (number of ops known)
    int rev = -1; // assigned by server when accepted
    Type type;
    int pos; // position (0-based)
    String text; // for insert: text to insert. for delete: optional (set when applied)
    int len; // for delete: number of chars to remove (for insert, len = text.length())

    Op(Type type, int pos, String text, int siteId, int baseRev) {
        this.opId = UUID.randomUUID().toString();
        this.type = type;
        this.pos = pos;
        this.text = text;
        this.len = type == Type.DEL ? Integer.parseInt(text) : text.length();
        this.siteId = siteId;
        this.baseRev = baseRev;
    }

    static Op insert(int pos, String text, int siteId, int baseRev) {
        return new Op(Type.INS, pos, text, siteId, baseRev);
    }

    static Op delete(int pos, int len, int siteId, int baseRev) {
        // store len as text for constructor convenience; we'll overwrite text when
        // applied
        return new Op(Type.DEL, pos, String.valueOf(len), siteId, baseRev);
    }

    public String toString() {
        if (type == Type.INS)
            return String.format("Ins(pos=%d, \"%s\", site=%d, base=%d, rev=%d, id=%s)",
                    pos, text, siteId, baseRev, rev, opId.substring(0, 6));
        else
            return String.format("Del(pos=%d, len=%d, site=%d, base=%d, rev=%d, id=%s)",
                    pos, len, siteId, baseRev, rev, opId.substring(0, 6));
    }

    // shallow copy to avoid mutating original when server transforms canonical op
    // reporting
    Op copy() {
        Op o = new Op(this.type,
                this.pos,
                this.type == Type.DEL ? String.valueOf(this.len) : this.text,
                this.siteId,
                this.baseRev);
        o.rev = this.rev;
        o.opId = this.opId; // keep same id
        return o;
    }
}

// ---------- Very small server ----------
class Server {

    StringBuilder doc;
    final List<Op> log = new ArrayList<>(); // canonical ops in order
    final List<Client> clients = new ArrayList<>();

    Server(String initial) {
        doc = new StringBuilder(initial);
    }

    void register(Client c) {
        clients.add(c);
        c.server = this;
    }

    // Accept a client op (op from client). Transform against log suffix and append
    // canonical op.
    synchronized void receiveFromClient(Op clientOp) {
        // Make a working copy (we will publish this canonical form)
        Op incoming = new Op(
                clientOp.type,
                clientOp.pos,
                clientOp.type == Op.Type.DEL ? String.valueOf(clientOp.len) : clientOp.text,
                clientOp.siteId,
                clientOp.baseRev);
        // transform incoming against log entries that the client didn't see (ops with
        // index >= baseRev)
        for (int i = incoming.baseRev; i < log.size(); i++) {
            incoming = Transformer.transform(incoming, log.get(i));
        }
        // assign revision
        incoming.rev = log.size() + 1;
        // For deletes, before applying, capture the text removed into incoming.text for
        // possible undo or for clients to have content
        applyToServerDoc(incoming);
        // append to log (canonical op)
        log.add(incoming);
        // broadcast to clients (including author)
        broadcast(incoming);
    }

    private void applyToServerDoc(Op op) {
        if (op.type == Op.Type.INS) {
            int p = Math.max(0, Math.min(op.pos, doc.length()));
            doc.insert(p, op.text);
        } else {
            int p = Math.max(0, Math.min(op.pos, doc.length()));
            int end = Math.min(p + op.len, doc.length());
            String removed = doc.substring(p, end);
            op.text = removed; // store removed text
            doc.delete(p, end);
        }
    }

    private void broadcast(Op canonicalOp) {
        // send a copy to each client
        for (Client c : clients) {
            c.receiveFromServer(canonicalOp);
        }
    }
}

// ---------- Very small client ----------
class Client {

    final int siteId;
    StringBuilder doc;
    int localRev = 0; // how many canonical ops integrated
    final Deque<Op> outstanding = new ArrayDeque<>(); // local ops not yet acked
    Server server;

    Client(int siteId, String initial) {
        this.siteId = siteId;
        this.doc = new StringBuilder(initial);
    }

    // User performs local edit
    void localInsert(int pos, String text) {
        Op op = Op.insert(pos, text, siteId, localRev);
        // apply locally immediately
        applyLocal(op);
        outstanding.addLast(op);
        // send to server
        server.receiveFromClient(op);
    }

    void localDelete(int pos, int len) {
        Op op = Op.delete(pos, len, siteId, localRev);
        applyLocal(op);
        outstanding.addLast(op);
        server.receiveFromClient(op);
    }

    // Apply locally (optimistic)
    public void applyLocal(Op op) {
        if (op.type == Op.Type.INS) {
            int p = Math.max(0, Math.min(op.pos, doc.length()));
            doc.insert(p, op.text);
        } else {
            int p = Math.max(0, Math.min(op.pos, doc.length()));
            int end = Math.min(p + op.len, doc.length());
            // store removed text in local op for potential undo
            String removed = doc.substring(p, end);
            op.text = removed;
            doc.delete(p, end);
        }
    }

    // Receive canonical op broadcast from server
    synchronized void receiveFromServer(Op incomingCanonical) {
        // If this op was created by me and matches the head outstanding op, it's an
        // ACK.
        if (!outstanding.isEmpty() && outstanding.peekFirst().opId.equals(incomingCanonical.opId)) {
            // pop outstanding head
            outstanding.pollFirst();
            localRev = incomingCanonical.rev;
            // We already applied it locally earlier (optimistic). But the server may have
            // transformed it differently.
            // For simplicity, we assume the local application matches server canonical
            // effect (client transformed incoming as needed previously).
            // If canonical differs, a full rebase would be necessary (not shown here).
            return;
        }

        // Otherwise it's a remote op (or ack for some earlier op not at head)
        // Transform the incoming canonical op against our outstanding list (in order).
        Op op = new Op(
                incomingCanonical.type,
                incomingCanonical.pos,
                incomingCanonical.type == Op.Type.DEL
                        ? String.valueOf(incomingCanonical.len)
                        : incomingCanonical.text,
                incomingCanonical.siteId,
                incomingCanonical.baseRev);
        op.rev = incomingCanonical.rev;
        op.opId = incomingCanonical.opId; // preserve id for matching

        // Transform incoming through each outstanding local op (chronological)
        for (Op myOp : outstanding) {
            op = Transformer.transform(op, myOp);
        }
        // Apply to local doc
        applyIncoming(op);
        localRev = incomingCanonical.rev;
    }

    private void applyIncoming(Op op) {
        if (op.type == Op.Type.INS) {
            int p = Math.max(0, Math.min(op.pos, doc.length()));
            doc.insert(p, op.text);
        } else {
            int p = Math.max(0, Math.min(op.pos, doc.length()));
            int end = Math.min(p + op.len, doc.length());
            // capture removed text if needed
            String removed = doc.substring(p, end);
            op.text = removed;
            doc.delete(p, end);
        }
    }

    String getDoc() {
        return doc.toString();
    }

}

// ---------- Transform utilities (the 4 pair cases) ----------
class Transformer {

    // Transform op against other (other already applied)
    // Returns a NEW Op object representing transformed op (shallow clone where
    // needed).
    // We mutate a copy to keep original created op intact for matching ack by opId.
    static Op transform(Op op, Op other) { // op -> incoming , other -> already applied
        // We'll operate on a copy to avoid side effects.
        Op res = new Op(
                op.type,
                op.pos,
                op.type == Op.Type.DEL ? String.valueOf(op.len) : op.text,
                op.siteId,
                op.baseRev);
        res.rev = op.rev;
        // Decide pair type
        if (op.type == Op.Type.INS && other.type == Op.Type.INS) {
            transformInsIns(res, other);
        } else if (op.type == Op.Type.INS && other.type == Op.Type.DEL) {
            transformInsDel(res, other);
        } else if (op.type == Op.Type.DEL && other.type == Op.Type.INS) {
            transformDelIns(res, other);
        } else if (op.type == Op.Type.DEL && other.type == Op.Type.DEL) {
            transformDelDel(res, other);
        }
        return res;
    }

    // Insert vs Insert
    // op -> incoming , other -> already applied
    private static void transformInsIns(Op op, Op other) {
        int i = op.pos; // incoming position
        int j = other.pos; // already applied position
        int L = other.text.length();
        if (j < i) { //
            op.pos = i + L;
        } else if (j == i) {
            // tie-break by siteId: smaller siteId goes first -> if other.siteId <
            // op.siteId, shift
            if (other.siteId < op.siteId)
                op.pos = i + L;
        }
        // else j > i -> no change
    }

    // Insert vs Delete
    private static void transformInsDel(Op op, Op other) {
        int i = op.pos;
        int j = other.pos;
        int n = other.len;
        if (j < i) {
            int shift = Math.min(n, i - j);
            op.pos = i - shift;
        }
        // else j >= i -> no change
    }

    // Delete vs Insert
    private static void transformDelIns(Op op, Op other) {
        int i = op.pos; // delete position
        int n = op.len; // delete len
        int j = other.pos; // insert position
        int L = other.text.length(); // insert len
        if (j <= i) { // insert is already applied on left side (on server). so we should move delete
                      // by insert length
            op.pos = i + L;
        } else if (j >= i + n) {
            // insert after deletion -> nothing
        } else {
            // insert inside deletion range -> delete the inserted text too (expand)
            op.len = n + L;
        }
    }

    // Delete vs Delete
    private static void transformDelDel(Op op, Op other) {
        int s1 = op.pos;
        int e1 = op.pos + op.len;
        int s2 = other.pos;
        int e2 = other.pos + other.len;

        if (e2 <= s1) {
            // other entirely before
            op.pos = op.pos - other.len;
            return;
        }
        if (s2 >= e1) {
            // other entirely after -> no change
            return;
        }
        // overlap
        int overlap = Math.max(0, Math.min(e1, e2) - Math.max(s1, s2));
        op.len = op.len - overlap;
        if (op.len < 0)
            op.len = 0;
        if (s2 < s1) {
            int shift = Math.min(other.len, s1 - s2);
            op.pos = op.pos - shift;
        }
    }

}

public class OTDemo {

    // ---------- Demo / simulation ----------
    public static void main(String[] args) {
        // Scenario 1: server receives B then A (classic example)
        System.out.println("=== Scenario 1: server receives B then A ===");
        runScenario(/* serverOrder= */ Arrays.asList("B", "A"));

        System.out.println("\n=== Scenario 2: server receives A then B ===");
        runScenario(Arrays.asList("A", "B"));
    }

    static void runScenario(List<String> serverOrder) {
        String initial = "CAT";
        Server server = new Server(initial);
        Client A = new Client(1, initial);
        Client B = new Client(2, initial);
        server.register(A);
        server.register(B);

        // Both clients make local edits concurrently based on baseRev=0
        // A: Insert(3, "!")
        // B: Delete(1, 1)
        Op aLocal = Op.insert(3, "!", A.siteId, A.localRev);
        Op bLocal = Op.delete(1, 1, B.siteId, B.localRev);

        // Clients apply locally (optimistic) and queue outstanding
        A.applyLocal(aLocal);
        A.outstanding.addLast(aLocal);
        B.applyLocal(bLocal);
        B.outstanding.addLast(bLocal);

        // Now send to server in specified order
        for (String name : serverOrder) {
            if (name.equals("A")) {
                server.receiveFromClient(aLocal);
            } else {
                server.receiveFromClient(bLocal);
            }
        }

        // After server processed both, clients will have received broadcasts because
        // server.broadcast calls receiveFromServer.
        // Print final documents
        System.out.println("Server doc: " + server.doc.toString());
        System.out.println("A doc:      " + A.getDoc());
        System.out.println("B doc:      " + B.getDoc());
    }

}

class PieceTable {

    private final String originalBuffer; // immutable
    private final StringBuilder addBuffer; // append-only
    private final List<Piece> pieces;

    static class Piece {
        String buffer; // "original" or "add"
        int start;
        int length;

        Piece(String buffer, int start, int length) {
            this.buffer = buffer;
            this.start = start;
            this.length = length;
        }

        public String toString() {
            return String.format("[%s:%d+%d]", buffer, start, length);
        }
    }

    public PieceTable(String initial) {
        this.originalBuffer = initial;
        this.addBuffer = new StringBuilder();
        this.pieces = new ArrayList<>();
        if (!initial.isEmpty()) {
            pieces.add(new Piece("original", 0, initial.length()));
        }
    }

    // Insert new text at given document position
    public void insert(int pos, String text) {
        if (pos < 0 || pos > length())
            throw new IllegalArgumentException("Bad pos");
        // Append new text to addBuffer
        int start = addBuffer.length();
        addBuffer.append(text);
        int len = text.length();

        // Find where to splice
        int index = 0;
        int offset = pos;
        for (; index < pieces.size(); index++) {
            Piece p = pieces.get(index);
            if (offset <= p.length)
                break;
            offset -= p.length;
        }

        if (index == pieces.size()) {
            // append after all
            pieces.add(new Piece("add", start, len));
        } else {
            Piece cur = pieces.get(index);
            if (offset == 0) {
                // insert before current
                pieces.add(index, new Piece("add", start, len));
            } else if (offset == cur.length) {
                // insert after current
                pieces.add(index + 1, new Piece("add", start, len));
            } else {
                // split current piece
                Piece left = new Piece(cur.buffer, cur.start, offset);
                Piece right = new Piece(cur.buffer, cur.start + offset, cur.length - offset);
                pieces.set(index, left);
                pieces.add(index + 1, new Piece("add", start, len));
                pieces.add(index + 2, right);
            }
        }
    }

    // Delete [pos, pos+len)
    public void delete(int pos, int len) {
        if (pos < 0 || pos + len > length())
            throw new IllegalArgumentException("Bad delete");

        int index = 0;
        int offset = pos;

        // Step 1: find starting piece
        for (; index < pieces.size(); index++) {
            Piece p = pieces.get(index);
            if (offset < p.length)
                break;
            offset -= p.length;
        }

        int remaining = len;
        while (remaining > 0 && index < pieces.size()) {
            Piece cur = pieces.get(index);
            if (offset == 0 && remaining >= cur.length) {
                // remove entire piece
                remaining -= cur.length;
                pieces.remove(index);
                // stay at same index, because list shifted
            } else if (offset == 0) {
                // chop from start
                cur.start += remaining;
                cur.length -= remaining;
                remaining = 0;
            } else if (offset + remaining >= cur.length) {
                // chop from middle to end
                cur.length = offset;
                remaining -= (cur.length - offset);
                index++;
                offset = 0;
            } else {
                // delete from middle (split piece)
                Piece left = new Piece(cur.buffer, cur.start, offset);
                Piece right = new Piece(cur.buffer, cur.start + offset + remaining,
                        cur.length - offset - remaining);
                pieces.set(index, left);
                pieces.add(index + 1, right);
                remaining = 0;
            }
        }
    }

    public int length() {
        return pieces.stream().mapToInt(p -> p.length).sum();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Piece p : pieces) {
            String buf = p.buffer.equals("original") ? originalBuffer : addBuffer.toString();
            sb.append(buf, p.start, p.start + p.length);
        }
        return sb.toString();
    }

    public String debugPieces() {
        return pieces.toString();
    }
}
