import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ============================================================================
 * FIND ALL POSSIBLE RECIPES FROM GIVEN SUPPLIES (LeetCode 2115)
 * ============================================================================
 * 
 * STATEMENT:
 * You have information about 'n' different recipes. Each recipe is listed in 
 * 'recipes', and its ingredients are provided in a 2D array 'ingredients'. 
 * A recipe can be an ingredient for another recipe.
 * You also have a 'supplies' array of initially available ingredients (infinite supply).
 * Return a list of all recipes you can create.
 * 
 * ----------------------------------------------------------------------------
 * RESTATING THE PROBLEM:
 * We are dealing with a dependency resolution problem. We have a set of items 
 * (supplies) that are "unlocked" from the start. We have tasks (recipes) that 
 * require prerequisites (ingredients) to be unlocked. Once a recipe is created, 
 * it becomes a new supply that might unlock further recipes. If a group of 
 * recipes depend on each other in a loop, none of them can be created.
 * We need to simulate this chain reaction and return all unlocked recipes.
 * 
 * ----------------------------------------------------------------------------
 * CLARIFYING QUESTIONS FOR THE INTERVIEWER (4-10):
 * 1. Q: What happens if two recipes depend on each other (circular dependency)?
 *    A: They can never be fulfilled, so they should be excluded from the result.
 * 2. Q: Do we need to worry about the quantity of supplies?
 *    A: No, the problem states we have an infinite supply of any available item.
 * 3. Q: Can an ingredient be completely missing from both supplies and recipes?
 *    A: Yes, if a recipe needs an ingredient that doesn't exist anywhere, it 
 *       can never be made.
 * 4. Q: Is the order of the output list important?
 *    A: The problem allows returning the answer in any order.
 * 5. Q: Are there duplicate strings in a single recipe's ingredient list?
 *    A: Constraints specify each ingredients[i] does not contain duplicates.
 * 
 * ----------------------------------------------------------------------------
 * HOW TO APPROACH THIS PROBLEM IN INTERVIEWS:
 * 1. Acknowledge the core concept: "This is a Directed Graph problem where edges 
 *    represent dependencies. We can solve this using Topological Sorting."
 * 2. Point out the optimization: "Instead of adding standard supplies as nodes 
 *    in our graph, we can treat them as 'already resolved'. A recipe's in-degree 
 *    is only the count of ingredients it needs that are NOT in the initial supplies."
 * 3. Discuss the two standard approaches:
 *    a) BFS / Kahn's Algorithm: Count in-degrees. As recipes reach 0 in-degree, 
 *       add them to a queue, "make" them, and reduce in-degrees of dependent recipes.
 *    b) DFS with Memoization: Recursively check if a recipe can be made. Use 
 *       a state map (Unvisited, Visiting, True, False) to detect cycles and cache results.
 * 4. Time/Space Complexity: Both run in O(V + E) time, where V is recipes and 
 *    E is the total number of ingredient dependencies.
 * 
 * ============================================================================
 */
public class FindAllPossibleRecipes {

    /**
     * ========================================================================
     * SOLUTION 1: KAHN'S ALGORITHM (Breadth-First Search / Topological Sort)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * Think of `in-degree` as the number of ingredients a recipe needs that we 
     * DO NOT CURRENTLY HAVE. 
     * If a recipe needs 3 ingredients, and 2 are in our starting `supplies`, 
     * its in-degree is just 1. We build a graph mapping that 1 missing ingredient 
     * to the recipe.
     * When a recipe's in-degree hits 0, we can make it! We put it in a queue.
     * As we make recipes from the queue, we treat them as newly found supplies, 
     * traverse our graph to find recipes that were waiting for them, and decrease 
     * their in-degrees. 
     * 
     * VISUAL TRACING:
     * recipes = ["bread", "sandwich", "burger"]
     * ingredients = [["yeast","flour"], ["bread","meat"], ["sandwich","meat","bread"]]
     * supplies = ["yeast", "flour", "meat"]
     * 
     * 1. Build Graph & In-Degrees:
     *    - "bread": needs yeast, flour (both in supplies). In-degree = 0.
     *    - "sandwich": needs meat (in supplies), bread (NOT in supplies). 
     *                  Add edge: bread -> sandwich. In-degree = 1.
     *    - "burger": needs meat (in supplies), sandwich (NOT), bread (NOT).
     *                Add edges: sandwich -> burger, bread -> burger. In-degree = 2.
     * 
     * 2. Execution (Queue):
     *    - Queue: ["bread"]
     *    - Pop "bread". Add to result. 
     *      Edges from bread: "sandwich" (in-degree 1 -> 0), "burger" (in-degree 2 -> 1).
     *    - Queue is now ["sandwich"].
     *    - Pop "sandwich". Add to result.
     *      Edges from sandwich: "burger" (in-degree 1 -> 0).
     *    - Queue is now ["burger"].
     *    - Pop "burger". Add to result. No edges.
     * 
     * Output: ["bread", "sandwich", "burger"]
     */
    public List<String> findAllRecipesBFS(String[] recipes, List<List<String>> ingredients, String[] supplies) {
        // Fast lookup for initially available supplies
        Set<String> availableSupplies = new HashSet<>(Arrays.asList(supplies));
        
        // graph: ingredient -> list of recipes that need this ingredient
        Map<String, List<String>> adj = new HashMap<>();
        // inDegree: recipe -> count of missing ingredients
        Map<String, Integer> inDegree = new HashMap<>();
        
        // 1. Build Graph
        for (int i = 0; i < recipes.length; i++) {
            String recipe = recipes[i];
            int missingIngredientsCount = 0;
            
            for (String ingredient : ingredients.get(i)) {
                if (!availableSupplies.contains(ingredient)) {
                    // Ingredient is missing, so this recipe depends on it being made later
                    adj.putIfAbsent(ingredient, new ArrayList<>());
                    adj.get(ingredient).add(recipe);
                    missingIngredientsCount++;
                }
            }
            inDegree.put(recipe, missingIngredientsCount);
        }
        
        // 2. Find all recipes that can be made immediately (0 missing ingredients)
        Queue<String> queue = new LinkedList<>();
        for (String recipe : recipes) {
            if (inDegree.get(recipe) == 0) {
                queue.offer(recipe);
            }
        }
        
        List<String> result = new ArrayList<>();
        
        // 3. Process level-by-level
        while (!queue.isEmpty()) {
            String currentRecipe = queue.poll();
            result.add(currentRecipe); // We successfully made this recipe
            
            // Treat the newly made recipe as an available ingredient for others
            if (adj.containsKey(currentRecipe)) {
                for (String dependentRecipe : adj.get(currentRecipe)) {
                    inDegree.put(dependentRecipe, inDegree.get(dependentRecipe) - 1);
                    
                    // If a dependent recipe now has all ingredients, queue it!
                    if (inDegree.get(dependentRecipe) == 0) {
                        queue.offer(dependentRecipe);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * ========================================================================
     * SOLUTION 2: DEPTH-FIRST SEARCH WITH MEMOIZATION (Modern Java Style)
     * ========================================================================
     * 
     * IDEA & INTUITION:
     * Instead of building the graph from ingredients to recipes, we can work 
     * backward. For a given recipe, can we make all its ingredients?
     * We use a recursive DFS approach. To avoid re-evaluating the same recipes 
     * and to catch circular dependencies (cycles), we use a Memoization Map:
     * - Key present & Value TRUE: We can definitely make this.
     * - Key present & Value FALSE: We cannot make this (or it's currently visiting).
     * 
     * If we see a node that is currently VISITING, it means we looped back to 
     * it -> Cycle detected -> return FALSE.
     */
    public List<String> findAllRecipesDFS(String[] recipes, List<List<String>> ingredients, String[] supplies) {
        Set<String> supplySet = new HashSet<>(Arrays.asList(supplies));
        
        // Map recipes to their ingredient lists for quick lookup
        // Using Java Streams to build the map elegantly
        Map<String, List<String>> recipeMap = IntStream.range(0, recipes.length)
            .boxed()
            .collect(Collectors.toMap(i -> recipes[i], ingredients::get));
            
        // state map acts as both visited set and memoization cache
        // null = unvisited, false = cannot make / visiting, true = can make
        Map<String, Boolean> memo = new HashMap<>();
        
        List<String> result = new ArrayList<>();
        
        for (String recipe : recipes) {
            if (canMake(recipe, recipeMap, supplySet, memo)) {
                result.add(recipe);
            }
        }
        
        return result;
    }
    
    private boolean canMake(String item, Map<String, List<String>> recipeMap, 
                            Set<String> supplies, Map<String, Boolean> memo) {
        
        // Base case 1: It's a raw supply we already have
        if (supplies.contains(item)) return true;
        
        // Base case 2: It's not a supply, and not a recipe we know how to make
        if (!recipeMap.containsKey(item)) return false;
        
        // Base case 3: Already computed or currently in the recursion stack (cycle)
        if (memo.containsKey(item)) return memo.get(item);
        
        // Mark as VISITING (Assume false until proven otherwise to break cycles)
        memo.put(item, false);
        
        // Recursively check all ingredients needed for this recipe
        for (String ingredient : recipeMap.get(item)) {
            if (!canMake(ingredient, recipeMap, supplies, memo)) {
                // If any ingredient fails, the whole recipe fails.
                return false; 
            }
        }
        
        // If we made it here, all ingredients can be successfully made!
        memo.put(item, true);
        return true;
    }

    /**
     * ========================================================================
     * MAIN METHOD & TESTING
     * ========================================================================
     * Using modern Java 14+ Record feature to elegantly structure test cases.
     */
    
    record TestCase(String[] recipes, List<List<String>> ingredients, String[] supplies, String description) {}

    public static void main(String[] args) {
        FindAllPossibleRecipes solver = new FindAllPossibleRecipes();

        var testCases = List.of(
            new TestCase(
                new String[]{"bread"}, 
                List.of(List.of("yeast", "flour")), 
                new String[]{"yeast", "flour", "corn"}, 
                "Single Recipe (All supplies present)"
            ),
            new TestCase(
                new String[]{"bread", "sandwich"}, 
                List.of(List.of("yeast", "flour"), List.of("bread", "meat")), 
                new String[]{"yeast", "flour", "meat"}, 
                "Chained Recipe (Sandwich needs Bread)"
            ),
            new TestCase(
                new String[]{"bread", "sandwich", "burger"}, 
                List.of(List.of("yeast", "flour"), List.of("bread", "meat"), List.of("sandwich", "meat", "bread")), 
                new String[]{"yeast", "flour", "meat"}, 
                "Multi-Dependency Chained Recipes"
            ),
            new TestCase(
                new String[]{"recipe1", "recipe2"}, 
                List.of(List.of("recipe2"), List.of("recipe1")), 
                new String[]{}, 
                "Direct Circular Dependency (Should return empty)"
            ),
            new TestCase(
                new String[]{"impossible"}, 
                List.of(List.of("unicorn_tears")), 
                new String[]{"water"}, 
                "Missing unknown ingredient"
            )
        );

        System.out.println("Running test cases for Find All Possible Recipes...");
        System.out.println("-".repeat(70));

        IntStream.range(0, testCases.size()).forEach(i -> {
            var tc = testCases.get(i);
            var resultBFS = solver.findAllRecipesBFS(tc.recipes(), tc.ingredients(), tc.supplies());
            var resultDFS = solver.findAllRecipesDFS(tc.recipes(), tc.ingredients(), tc.supplies());
            
            System.out.printf("Test Case %d: %s\n", i + 1, tc.description());
            System.out.printf("BFS Result: %s\n", resultBFS);
            System.out.printf("DFS Result: %s\n", resultDFS);
            System.out.println("-".repeat(70));
        });
    }
}
