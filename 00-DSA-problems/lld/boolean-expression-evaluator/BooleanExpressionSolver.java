import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// --- 1. BooleanExpression Interface (AST Node Base) ---

/**
 * Represents a node in the Abstract Syntax Tree (AST) of a boolean expression.
 * All concrete expression types (variables, literals, operators) will implement
 * this.
 */
interface BooleanExpression {
    /**
     * Evaluates the boolean expression represented by this node given a map of
     * variable assignments.
     *
     * @param assignments A map where keys are variable names (String) and values
     *                    are their assigned boolean values.
     * @return The boolean result of evaluating this expression.
     * @throws IllegalArgumentException If a variable in the expression is not found
     *                                  in the assignments map.
     */
    boolean evaluate(Map<String, Boolean> assignments);

    /**
     * Collects all unique variable names present in this boolean expression.
     *
     * @return A set of unique variable names (Strings).
     */
    Set<String> getVariables();

    /**
     * Returns a string representation of the expression.
     * 
     * @return The string representation.
     */
    String toString();
}

// --- 2. Concrete Expression Classes (AST Nodes) ---

/**
 * Represents a boolean variable in the expression (e.g., 'a', 'b').
 */
class VariableExpression implements BooleanExpression {

    private final String name;

    public VariableExpression(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Variable name cannot be null or empty.");
        }
        this.name = name;
    }

    @Override
    public boolean evaluate(Map<String, Boolean> assignments) {
        Boolean value = assignments.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Assignment for variable '" + name + "' not found.");
        }
        return value;
    }

    @Override
    public Set<String> getVariables() {
        Set<String> variables = new HashSet<>();
        variables.add(name);
        return variables;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VariableExpression that = (VariableExpression) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}

/**
 * Represents a boolean literal (true or false).
 */
class LiteralExpression implements BooleanExpression {

    private final boolean value;

    public LiteralExpression(boolean value) {
        this.value = value;
    }

    @Override
    public boolean evaluate(Map<String, Boolean> assignments) {
        return value;
    }

    @Override
    public Set<String> getVariables() {
        return Collections.emptySet(); // Literals have no variables
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LiteralExpression that = (LiteralExpression) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}

/**
 * Represents a logical NOT operation (~operand).
 */
class NotExpression implements BooleanExpression {

    private final BooleanExpression operand;

    public NotExpression(BooleanExpression operand) {
        if (operand == null) {
            throw new IllegalArgumentException("Operand for NOT cannot be null.");
        }
        this.operand = operand;
    }

    @Override
    public boolean evaluate(Map<String, Boolean> assignments) {
        return !operand.evaluate(assignments);
    }

    @Override
    public Set<String> getVariables() {
        return operand.getVariables();
    }

    @Override
    public String toString() {
        return "~(" + operand.toString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NotExpression that = (NotExpression) o;
        return Objects.equals(operand, that.operand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operand);
    }

}

/**
 * Represents a logical AND operation (left && right).
 */
class AndExpression implements BooleanExpression {

    private final BooleanExpression left;
    private final BooleanExpression right;

    public AndExpression(BooleanExpression left, BooleanExpression right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Operands for AND cannot be null.");
        }
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate(Map<String, Boolean> assignments) {
        return left.evaluate(assignments) && right.evaluate(assignments);
    }

    @Override
    public Set<String> getVariables() {
        Set<String> variables = new HashSet<>(left.getVariables());
        variables.addAll(right.getVariables());
        return variables;
    }

    @Override
    public String toString() {
        return "(" + left.toString() + " && " + right.toString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AndExpression that = (AndExpression) o;
        return Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

}

/**
 * Represents a logical OR operation (left || right).
 */
class OrExpression implements BooleanExpression {

    private final BooleanExpression left;
    private final BooleanExpression right;

    public OrExpression(BooleanExpression left, BooleanExpression right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Operands for OR cannot be null.");
        }
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean evaluate(Map<String, Boolean> assignments) {
        return left.evaluate(assignments) || right.evaluate(assignments);
    }

    @Override
    public Set<String> getVariables() {
        Set<String> variables = new HashSet<>(left.getVariables());
        variables.addAll(right.getVariables());
        return variables;
    }

    @Override
    public String toString() {
        return "(" + left.toString() + " || " + right.toString() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrExpression that = (OrExpression) o;
        return Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

}

// --- 3. BooleanExpressionParser ---

/**
 * Parses a string representation of a boolean expression into an AST
 * (BooleanExpression).
 * Supports variables (single lowercase letters), 'true', 'false', '&&', '||',
 * '~', and parentheses.
 * Implements a recursive descent parser.
 *
 * Grammar (simplified EBNF):
 * expression = term ( "||" term )*
 * term = factor ( "&&" factor )*
 * factor = "~" factor | primary
 * primary = variable | literal | "(" expression ")"
 * variable = [a-z]
 * literal = "true" | "false"
 */
class BooleanExpressionParser {

    private String expression;
    private int pos;

    public BooleanExpression parse(String expr) {
        this.expression = expr.replaceAll("\\s+", ""); // Remove all whitespace
        this.pos = 0;
        BooleanExpression result = parseExpression();
        if (pos < expression.length()) {
            throw new IllegalArgumentException(
                    "Unexpected character at end of expression: '" + expression.substring(pos) + "'");
        }
        return result;
    }

    private char peek() {
        return pos < expression.length() ? expression.charAt(pos) : '\0';
    }

    private char consume() {
        return expression.charAt(pos++);
    }

    private boolean match(String s) {
        if (expression.startsWith(s, pos)) {
            pos += s.length();
            return true;
        }
        return false;
    }

    // expression = term ( "||" term )*
    private BooleanExpression parseExpression() {
        BooleanExpression left = parseTerm();
        while (match("||")) {
            left = new OrExpression(left, parseTerm());
        }
        return left;
    }

    // term = factor ( "&&" factor )*
    private BooleanExpression parseTerm() {
        BooleanExpression left = parseFactor();
        while (match("&&")) {
            left = new AndExpression(left, parseFactor());
        }
        return left;
    }

    // factor = "~" factor | primary
    private BooleanExpression parseFactor() {
        if (match("~")) {
            return new NotExpression(parseFactor());
        }
        return parsePrimary();
    }

    // primary = variable | literal | "(" expression ")"
    private BooleanExpression parsePrimary() {
        char current = peek();
        if (current == '(') {
            consume(); // Consume '('
            BooleanExpression expr = parseExpression();
            if (!match(")")) {
                throw new IllegalArgumentException("Expected ')' at position " + pos);
            }
            return expr;
        } else if (match("true")) {
            return new LiteralExpression(true);
        } else if (match("false")) {
            return new LiteralExpression(false);
        } else if (Character.isLetter(current) && Character.isLowerCase(current)) {
            consume(); // Consume the variable character
            return new VariableExpression(String.valueOf(current));
        } else {
            throw new IllegalArgumentException(
                    "Unexpected character or invalid primary expression at position " + pos + ": '" + current + "'");
        }
    }

}

// --- 4. BooleanSatisfiabilityChecker ---

/**
 * Determines if a given boolean expression can be evaluated to TRUE for any
 * combination
 * of its variable assignments.
 * This class uses a brute-force approach by generating all possible truth
 * assignments.
 */
class BooleanSatisfiabilityChecker {

    /**
     * Checks if the given boolean expression is satisfiable (i.e., can evaluate to
     * true).
     *
     * @param expression The boolean expression to check.
     * @return true if the expression is satisfiable, false otherwise.
     */
    public boolean isSatisfiable(BooleanExpression expression) {
        Set<String> variables = expression.getVariables();
        if (variables.isEmpty()) {
            // If no variables, just evaluate the expression directly
            return expression.evaluate(Collections.emptyMap());
        }

        List<String> varList = new ArrayList<>(variables);
        int numVariables = varList.size();

        // Iterate through all 2^numVariables possible truth assignments
        // Each integer from 0 to 2^numVariables - 1 represents a unique assignment
        for (int i = 0; i < (1 << numVariables); i++) {
            Map<String, Boolean> currentAssignments = new HashMap<>();
            for (int j = 0; j < numVariables; j++) {
                // Determine the boolean value for the j-th variable based on the i-th bit
                boolean value = ((i >> j) & 1) == 1;
                currentAssignments.put(varList.get(j), value);
            }

            try {
                if (expression.evaluate(currentAssignments)) {
                    System.out.println("Satisfiable with assignments: " + currentAssignments);
                    return true; // Found a satisfying assignment
                }
            } catch (IllegalArgumentException e) {
                // This should ideally not happen if variables are correctly collected and
                // assigned.
                // Log or handle if unexpected variable issues arise.
                System.err.println("Error during evaluation: " + e.getMessage());
            }
        }
        return false; // No satisfying assignment found after checking all possibilities
    }

}

// --- Main Class for Demonstration ---
public class BooleanExpressionSolver {

    public static void main(String[] args) {
        BooleanExpressionParser parser = new BooleanExpressionParser();
        BooleanSatisfiabilityChecker checker = new BooleanSatisfiabilityChecker();

        // Test Cases
        String[] expressions = {
                "( (a || b ) && c )",
                "a && ~a",
                "a || ~a",
                "true && b",
                "false || a",
                "a && b && c",
                "~a || ~b || ~c",
                "a",
                "true",
                "false",
                "~true",
                "~(a && b) || (~a || ~b)", // De Morgan's Law (tautology)
                "(a && b) && (~a || ~b)", // Contradiction
                "a && (b || c) && ~b",
                "a && (b || c) && ~b && ~a"
        };

        for (String exprStr : expressions) {
            System.out.println("\n--- Evaluating Expression: " + exprStr + " ---");
            try {
                BooleanExpression parsedExpression = parser.parse(exprStr);
                System.out.println("Parsed AST: " + parsedExpression.toString());
                boolean satisfiable = checker.isSatisfiable(parsedExpression);
                System.out.println("Expression is satisfiable: " + satisfiable);
            } catch (IllegalArgumentException e) {
                System.err.println("Error parsing or evaluating: " + e.getMessage());
            }
        }
    }

}
