
import java.util.TreeSet;

/**
 * This class implements an O(N log N) streaming algorithm to find and remove
 * triples of float values from a stream such that all pairwise absolute
 * differences between the three values are less than or equal to a given
 * threshold D.
 *
 * Key idea:
 * - Maintain a sorted TreeSet<Double> of all active values (not yet removed).
 * - For each incoming value x:
 * 1. Insert x into the TreeSet.
 * 2. Use TreeSet's navigable methods to find:
 * - Two predecessors: p1 = lower(x), p2 = lower(p1)
 * - Two successors: s1 = higher(x), s2 = higher(s1)
 * 3. Check three possible combinations:
 * a. (p2, p1, x) → if all exist and (x - p2) <= D
 * b. (x, s1, s2) → if all exist and (s2 - x) <= D
 * c. (p1, x, s1) → if all exist and (s1 - p1) <= D
 * 4. If any combination satisfies the condition, it means the three values
 * have all pairwise differences <= D. Print the triple, remove them
 * from the set, and continue.
 *
 * Time Complexity:
 * - Each insertion and neighbor lookup in TreeSet is O(log N)
 * - Since we do a constant number of operations per value, total time is O(N
 * log N)
 */

public class StreamingTripleFinder {

    private final double D;
    private final TreeSet<Double> set;

    public StreamingTripleFinder(double d) {
        this.D = d;
        this.set = new TreeSet<>();
    }

    public void onNext(double x) {
        this.set.add(x);

        Double p1 = this.set.lower(x);
        Double p2 = p1 == null ? null : this.set.lower(p1);
        Double s1 = this.set.higher(x);
        Double s2 = s1 == null ? null : this.set.higher(s1);

        if (p2 != null && (x - p1) <= D) {
            this.remove(p2, p1, x);
            return;
        }
        if (s2 != null && (s2 - x) <= D) {
            this.remove(x, s1, s2);
            return;
        }
        if (p1 != null && s1 != null && (s1 - p1) <= D) {
            this.remove(p1, x, s1);
        }

    }

    private void remove(double a, double b, double c) {
        this.set.remove(a);
        this.set.remove(b);
        this.set.remove(c);
    }

}
