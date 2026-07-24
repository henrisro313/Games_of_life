import java.util.Set;

public interface Tiling<Cell> {
    /** Calculates the neighbourhood, which should not include the cell itself. */
    Set<Cell> neighbours(Cell cell);
    /** Gets an array {xs, ys} of polygon vertices. */
    int[][] bounds(Cell cell);
    /** Starting cell for random generation. This doesn't need to be consistent. */
    Cell initialCell();
    /** Allows exclusion of common oscillations in random generation. */
    boolean isInterestingOscillationPeriod(int period);
    /** Parse command-line input. */
    Set<Cell> parseCells(String[] data);
    /** Inverse of the parse. */
    String format(Set<Cell> cells);

    // Add this declaration here, **no implementation**:
    int distance(Cell a, Cell b);
}