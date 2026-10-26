/**
 * ============================================================================
 * 0. ## Problem Statement
 * ============================================================================
 * Design a Snake game that is played on a device with screen size `width` x `height`.
 * 
 * The snake starts at the top left corner `(0, 0)` with a length of 1.
 * You are given an array of `food` coordinates. When the snake eats food, its 
 * length increases by 1, and the score increases by 1.
 * 
 * Implement the SnakeGame class:
 * - SnakeGame(int width, int height, int[][] food): Initializes the game.
 * - int move(String direction): Moves the snake in the given direction ("U", "D", 
 *   "L", or "R"). Returns the current score, or -1 if the game is over (snake 
 *   hits the boundary or its own body).
 * 
 * Constraints:
 * - 1 <= width, height <= 10^4
 * - 1 <= food.length <= 50
 * - food[i].length == 2
 * - 0 <= food[i][0] < height, 0 <= food[i][1] < width
 * - direction is "U", "D", "L", or "R".
 * - At most 10^4 calls will be made to move.
 * 
 * ============================================================================
 * 1. ## Clarifying questions
 * ============================================================================
 * 1. Can the snake's head move into its own tail's current position?
 *    (Yes! When the snake moves, its tail moves forward simultaneously. The cell 
 *    previously occupied by the tail becomes empty right before the head enters it—
 *    UNLESS the snake just ate food, in which case the tail stays put).
 * 2. Are food items generated dynamically if the snake eats them all?
 *    (No, the food array is fixed. Once all food is eaten, the snake just continues 
 *    moving without growing).
 * 3. Does the snake ever spawn directly on food?
 *    (No, the prompt guarantees the starting position (0,0) does not contain the first food).
 * 
 * ============================================================================
 * 2. ## The reasoning journey
 * ============================================================================
 * > Core Requirement: Track a continuously shifting chain of coordinates. We must 
 *   efficiently add a new head, remove an old tail, and check for body collisions.
 * 
 * --- Approach 1: 2D Grid Simulation ---
 * 1. Intuition: Maintain an `int[][] grid` of size `width * height`.
 * 2. Try: Mark empty cells as 0, body cells as 1. 
 * 3. Why it fails: When the snake moves, we easily know where the new head goes, 
 *    but how do we know where the tail currently is so we can erase it? A grid 
 *    doesn't preserve the *order* of the snake's body segments. 
 * 4. Bottleneck: Requires O(N) scanning to find the tail, or parallel tracking.
 * 
 * --- Approach 2: A Single Queue ---
 * 1. Intuition: Use a `Queue` to track the chronological order of the body.
 * 2. Try: 
 *    - `move`: Calculate new head. Check if `queue.contains(newHead)`.
 *    - If no collision, `queue.offer(newHead)`. 
 *    - If no food eaten, `queue.poll()` to remove the tail.
 * 3. Why it's suboptimal: `queue.contains()` takes O(N) time where N is the length 
 *    of the snake. With a long snake and 10^4 moves, this results in O(N^2) total time.
 * 
 * --- Approach 3: Deque + HashSet (Optimal) ---
 * 1. Intuition: We need O(1) order tracking (Queue) AND O(1) collision checking (HashSet).
 * 2. Try: 
 *    - `Deque<Integer> snake`: Tracks the body for fast head addition (`offerFirst`) 
 *      and tail deletion (`pollLast`).
 *    - `HashSet<Integer> occupied`: Mirrors the Deque for O(1) collision lookups.
 *    - Optimization: Instead of creating `Point` objects (which triggers massive Garbage 
 *      Collection overhead during 10^4 moves), compress 2D coordinates into a 1D integer 
 *      using `row * width + col`.
 * 3. Why it works: Every operation (move, check collision, eat, grow) is strictly O(1).
 * 4. Which to write in an interview?
 *    Approach 3. It flawlessly balances memory efficiency (primitive 1D coordinates) 
 *    with optimal time complexity.
 * 
 * Time Complexity: O(1) per `move()` call.
 * Space Complexity: O(N) where N is the maximum length of the snake.
 * 
 * ============================================================================
 * 3. ## Edge cases
 * ============================================================================
 * - The "Chasing the Tail" maneuver: If the snake wraps around and its head is about 
 *   to land on its own tail, it is VALID. The collision check must temporarily ignore 
 *   the tail, because the tail is technically moving out of the way.
 * - Out of Bounds: Must be checked *before* self-collision.
 * - Exhausting the food array: Must bounds-check `foodIndex < food.length` before 
 *   attempting to read the next food coordinate.
 * 
 * ============================================================================
 * 4. ## Key Insight, Diagrams & Pitfalls
 * ============================================================================
 * > Key Insight: "Temporarily remove the tail before checking collisions."
 *   The most common failing point in this problem is checking `occupied.contains(newHead)` 
 *   before updating the tail. If the head moves to the tail's spot, it will falsely 
 *   trigger a game over. Always remove the tail from the Deque and Set first, check 
 *   collisions, and if food was eaten, put the tail back!
 * 
 * ASCII DRY RUN (Tail Chasing)
 * Snake: Head(0,1) -> Tail(0,0). Length: 2.
 * Move: "L" (Head moves to (0,0)).
 * 
 * Step 1: Calc newHead = (0,0)
 * Step 2: Remove old tail from structures.
 *         snake = [ (0,1) ]
 *         occupied = { (0,1) }
 * Step 3: Check collision. Does occupied contain (0,0)? 
 *         No! It is completely safe.
 * Step 4: Check food. No food eaten.
 *         (If food WAS eaten, we would re-add the old tail (0,0) here, which would 
 *          ironically mean the snake hit itself. But since no food, tail remains deleted).
 * Step 5: Add newHead.
 *         snake = [ (0,0), (0,1) ]
 *         occupied = { (0,0), (0,1) }
 * 
 * Pitfalls:
 * - Instantiating standard Java `Point` or `int[]` objects on every move. In tight 
 *   game loops, object creation causes heap fragmentation and GC pauses. The 1D 
 *   integer mapping `r * width + c` is the professional way to handle grids.
 * 
 * Pattern Recognition: "Moving sliding window / Worm" -> Deque + HashSet.
 * 
 * ============================================================================
 * 5. ## Follow-ups
 * ============================================================================
 * Q1: What if the grid is massive (e.g., 10^9 x 10^9) and food appears randomly?
 * A1: The 1D mapping `r * width + c` would exceed the 32-bit Integer limit. We would 
 *     have to use a `long` for the coordinates: `(long) r * width + c`. The space 
 *     complexity would remain O(N) where N is the snake's length, so the massive grid 
 *     would not cause Memory Limit Exceeded.
 * 
 * Q2: How would you prevent randomly generated food from spawning on the snake's body?
 * A2: We would use a `while(occupied.contains(randomCoord))` loop to reroll the food 
 *     spawn location. If the snake takes up 99% of the board, this while loop becomes 
 *     inefficient (rejection sampling failure). We would instead need to maintain a 
 *     tracked list of "Available Empty Cells" and pick a random index from that list.
 */

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class SnakeGameStudy {

    static class SnakeGame {
        
        // Dimensions of the game board
        private final int width;
        private final int height;
        
        // Food sequence and current target tracker
        private final int[][] food;
        private int foodIndex;
        
        // Deque preserves the chronological order of the snake (Front = Head, Back = Tail)
        private final Deque<Integer> snake;
        
        // HashSet enables O(1) lookups to check for body collisions
        private final Set<Integer> occupied;

        /**
         * Initializes the game with the board size and the sequence of food locations.
         */
        public SnakeGame(int width, int height, int[][] food) {
            this.width = width;
            this.height = height;
            this.food = food;
            this.foodIndex = 0;
            
            this.snake = new LinkedList<>();
            this.occupied = new HashSet<>();
            
            // The snake starts at (0, 0)
            int startPos = 0; // equivalent to 0 * width + 0
            snake.offerFirst(startPos);
            occupied.add(startPos);
        }

        /**
         * Moves the snake in the given direction.
         * Returns the current score (snake length - 1) or -1 if game over.
         * Time Complexity: O(1) per move.
         */
        public int move(String direction) {
            // 1. Determine current head coordinates
            int currentHead = snake.peekFirst();
            int r = currentHead / width;
            int c = currentHead % width;
            
            // 2. Calculate the next proposed head coordinates
            if (direction.equals("U")) {
                r--;
            } else if (direction.equals("D")) {
                r++;
            } else if (direction.equals("L")) {
                c--;
            } else if (direction.equals("R")) {
                c++;
            }
            
            // 3. Boundary check (Hit a wall -> Game Over)
            if (r < 0 || r >= height || c < 0 || c >= width) {
                return -1; 
            }
            
            // Map the 2D proposed head back to a 1D integer
            int newHead = r * width + c;
            
            // 4. Temporarily remove the tail!
            // The tail moves forward simultaneously with the head.
            // If the head lands on the tail's old spot, it is a valid move.
            int tail = snake.peekLast();
            snake.pollLast();
            occupied.remove(tail);
            
            // 5. Self-collision check (Hit its own body -> Game Over)
            if (occupied.contains(newHead)) {
                return -1;
            }
            
            // 6. Food check
            if (foodIndex < food.length && r == food[foodIndex][0] && c == food[foodIndex][1]) {
                // The snake ate food! 
                // This means the snake grows, so the tail didn't actually move out of its cell.
                // We must restore the tail to our tracking structures.
                snake.offerLast(tail);
                occupied.add(tail);
                
                // Advance to the next food target
                foodIndex++;
            }
            
            // 7. Finalize the move by adding the new head
            snake.offerFirst(newHead);
            occupied.add(newHead);
            
            // The score is the length of the snake minus 1 (starting length)
            return snake.size() - 1;
        }
    }

    // ============================================================================
    // MAIN METHOD: Dry Run & Testing
    // ============================================================================
    public static void main(String[] args) {
        // Setup a 3x2 board with food at (1,2) and (0,1)
        int[][] food = {{1, 2}, {0, 1}};
        SnakeGame game = new SnakeGame(3, 2, food);
        
        // Start: S at (0,0). Length: 1.
        System.out.println("Move R: " + game.move("R")); // Head to (0,1). Score: 0.
        System.out.println("Move D: " + game.move("D")); // Head to (1,1). Score: 0.
        
        // Head moves to (1,2) which is the first food!
        System.out.println("Move R: " + game.move("R")); // Eats food. Score: 1. Length: 2.
        
        System.out.println("Move U: " + game.move("U")); // Head to (0,2). Score: 1.
        
        // Head moves to (0,1) which is the second food!
        System.out.println("Move L: " + game.move("L")); // Eats food. Score: 2. Length: 3.
        
        // Snake is now at (0,1), (0,2), (1,2).
        // Head is at (0,1). Moving UP hits the wall.
        System.out.println("Move U (Hit Wall): " + game.move("U")); // Expected: -1
    }
}

/**
 * ============================================================================
 * 7. ## Short Summary
 * ============================================================================
 * Core Pattern: Deque (for order) + HashSet (for O(1) collision lookups).
 * Key Observation: A snake is functionally a sliding window that winds around a 2D grid. 
 * Pushing to the front of a Deque and popping from the back perfectly simulates motion. 
 * Flattening 2D coordinates `(r, c)` into 1D integers `(r * width + c)` eliminates object 
 * creation overhead, crucial for performance in gaming loops.
 * What to Memorize: The operation order! 1) Calc new head. 2) Remove old tail. 
 * 3) Check body collision. 4) If food, restore old tail. 5) Add new head.
 * Most Common Trap: Checking `occupied.contains(newHead)` before temporarily removing 
 * the tail. A snake is legally allowed to chase its own tail; failing to remove it first 
 * results in false-positive collisions.
 * Mental Trigger: "Snake / Slithering path tracking" -> "Deque + HashSet with 1D mapping".
 * ============================================================================
 */










































import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

class SnakeGame {
    // A record gives us immutable coordinates with value-based equals/hashCode.
    // Two Cell(1, 2) instances are equal, making them suitable HashSet keys.
    private record Cell(int row, int col) {}

    private final int width;
    private final int height;
    private final int[][] food;

    // Maintains body order: HEAD at the front, TAIL at the back.
    private final Deque<Cell> snake = new ArrayDeque<>();

    // Mirrors the cells in snake, allowing average O(1) collision checks.
    private final Set<Cell> occupied = new HashSet<>();

    // food[foodIndex] is the next food the snake must eat.
    private int foodIndex = 0;
    private int score = 0;
    private boolean gameOver = false;

    public SnakeGame(int width, int height, int[][] food) {
        this.width = width;
        this.height = height;
        this.food = food;

        Cell start = new Cell(0, 0);
        snake.addFirst(start);
        occupied.add(start);
    }

    public int move(String direction) {
        // After a fatal move, preserve the terminal state.
        if (gameOver) {
            return -1;
        }

        Cell head = snake.peekFirst();

        // A switch expression maps each command to its coordinate delta.
        // Rows increase downward; columns increase toward the right.
        int[] delta = switch (direction) {
            case "U" -> new int[]{-1, 0};
            case "D" -> new int[]{1, 0};
            case "L" -> new int[]{0, -1};
            case "R" -> new int[]{0, 1};
            default -> throw new IllegalArgumentException("Invalid direction");
        };

        Cell next = new Cell(
                head.row() + delta[0],
                head.col() + delta[1]
        );

        // 1. Check the walls before mutating either data structure.
        if (next.row() < 0 || next.row() >= height
                || next.col() < 0 || next.col() >= width) {
            gameOver = true;
            return -1;
        }

        // 2. Only the next uneaten food item matters.
        // The food index advances exactly once for each food eaten.
        boolean eating = foodIndex < food.length
                && food[foodIndex][0] == next.row()
                && food[foodIndex][1] == next.col();

        // 3. Find the current tail. It might be the next head position.
        Cell tail = snake.peekLast();

        // 4. A cell occupied by the body is normally a collision.
        // EXCEPTION: if we are NOT eating and next == tail, the tail
        // vacates that cell during this move, so entering it is legal.
        boolean hitsBody = occupied.contains(next)
                && (eating || !next.equals(tail));

        if (hitsBody) {
            gameOver = true;
            return -1;
        }

        // 5. On a normal move, the tail leaves before the new head is added.
        // Update BOTH structures, keeping them perfectly synchronized.
        // When eating, do not remove the tail: that is how growth occurs.
        if (!eating) {
            Cell removedTail = snake.removeLast();
            occupied.remove(removedTail);
        }

        // 6. Insert the new head at the front of the deque and into the set.
        snake.addFirst(next);
        occupied.add(next);

        // 7. Eating food increases length by one because the tail stayed put.
        // The next food becomes active only after this one is eaten.
        if (eating) {
            score++;
            foodIndex++;
        }

        return score;
    }
}

/*
WHY THIS SOLUTION IS CORRECT

Invariant 1: snake contains every body cell in head-to-tail order.

Invariant 2: occupied contains exactly the same cells as snake.

Invariant 3: score equals foodIndex, because every successfully eaten food
             item increases both by exactly one.

Initialization establishes all three invariants: the body and set both
contain only (0, 0), and both counters start at zero.

For a normal move, removing the tail and adding the new head preserves
the snake's length. For a food move, retaining the tail and adding the
new head increases the length by one. The score and food index then
advance together.

The collision test handles the only special case: the old tail is
considered available only if the snake is not eating. Consequently,
the algorithm rejects every wall collision and every genuine body
collision while permitting valid tail-vacating moves.

TIME COMPLEXITY

Let L be the current snake length and F the number of food items.

Constructor:
  O(1) auxiliary initialization, excluding the supplied food array.
  O(F) if counting the input food storage as part of the total
  space associated with the object.

Each move:
  O(1) average time.
  - Read the head and tail: O(1).
  - Calculate the next position: O(1).
  - Check the next food: O(1).
  - Check occupied.contains(next): O(1) average.
  - Remove the tail if needed: O(1) average for the set removal
    and O(1) for deque removal.
  - Add the new head: O(1) average for the set insertion
    and O(1) for deque insertion.

Thus, for M move calls, total expected time is O(M).

Worst-case hash-table operations can degrade under pathological hashing,
so O(1) is the expected/average bound, not a strict worst-case guarantee.

SPACE COMPLEXITY

  O(L + F) total storage including food.
  O(L) auxiliary space for the deque and occupied set.

The board is never allocated as a height-by-width matrix.
That is important because the dimensions can be large while the
snake occupies only a small number of cells.
*/


