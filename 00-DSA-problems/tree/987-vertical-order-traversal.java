import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

class VerticalOrderTraversal {

    class Tuple {
        TreeNode node;
        int hd, depth;

        Tuple(TreeNode n, int h, int d) {
            this.node = n;
            this.hd = h;
            this.depth = d;
        }
    }

    public List<List<Integer>> verticalTraversal(TreeNode root) {
        Map<Integer, List<int[]>> columnMap = new TreeMap<>();
        Queue<Tuple> queue = new LinkedList<>();
        queue.offer(new Tuple(root, 0, 0));

        while (!queue.isEmpty()) {
            Tuple tuple = queue.poll();
            TreeNode node = tuple.node;
            int hd = tuple.hd;
            int depth = tuple.depth;

            columnMap.computeIfAbsent(hd, k -> new ArrayList<>())
                    .add(new int[] { depth, node.val });

            if (node.left != null) {
                queue.offer(new Tuple(node.left, hd - 1, depth + 1));
            }
            if (node.right != null) {
                queue.offer(new Tuple(node.right, hd + 1, depth + 1));
            }
        }

        List<List<Integer>> result = new ArrayList<>();
        for (List<int[]> col : columnMap.values()) {
            Collections.sort(col, (a, b) -> {
                if (a[0] == b[0])
                    return Integer.compare(a[1], b[1]);
                return Integer.compare(a[0], b[0]);
            });

            List<Integer> column = new ArrayList<>();
            for (int[] pair : col) {
                column.add(pair[1]);
            }
            result.add(column);
        }
        return result;
    }

}

class VerticalOrderTraversal2 {
    class Tuple {
        TreeNode node;
        int hd, depth;

        Tuple(TreeNode n, int h, int d) {
            this.node = n;
            this.hd = h;
            this.depth = d;
        }
    }

    class NodeData {
        int depth, val;

        NodeData(int d, int v) {
            this.depth = d;
            this.val = v;
        }
    }

    public List<List<Integer>> verticalTraversal(TreeNode root) {
        Map<Integer, PriorityQueue<NodeData>> columnMap = new TreeMap<>();
        Queue<Tuple> queue = new LinkedList<>();
        queue.offer(new Tuple(root, 0, 0));

        while (!queue.isEmpty()) {
            Tuple tuple = queue.poll();
            TreeNode node = tuple.node;
            int hd = tuple.hd;
            int depth = tuple.depth;

            columnMap
                    .computeIfAbsent(hd,
                            k -> new PriorityQueue<>((a, b) -> a.depth == b.depth ? Integer.compare(a.val, b.val)
                                    : Integer.compare(a.depth, b.depth)))
                    .offer(new NodeData(depth, node.val));

            if (node.left != null) {
                queue.offer(new Tuple(node.left, hd - 1, depth + 1));
            }
            if (node.right != null) {
                queue.offer(new Tuple(node.right, hd + 1, depth + 1));
            }
        }

        List<List<Integer>> result = new ArrayList<>();
        for (PriorityQueue<NodeData> pq : columnMap.values()) {
            List<Integer> column = new ArrayList<>();
            while (!pq.isEmpty()) {
                column.add(pq.poll().val);
            }
            result.add(column);
        }
        return result;
    }

}
