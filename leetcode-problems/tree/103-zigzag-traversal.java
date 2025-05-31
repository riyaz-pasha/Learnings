import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

class ZigZagLevelOrderTraversal {

   public List<List<Integer>> zigzagLevelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null)
            return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        boolean leftToRight = true;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> level = new LinkedList<>();
            for (int i = 0; i < levelSize; i++) {
                TreeNode current = queue.poll();
                if (leftToRight)
                    level.addLast(current.val);
                else
                    level.addFirst(current.val);
                if (current.left != null)
                    queue.offer(current.left);
                if (current.right != null)
                    queue.offer(current.right);
            }
            result.add(level);
            leftToRight = !leftToRight;
        }
        return result;
    }


}
