package algorithms;

import java.util.Arrays;

/** Minimum-cost rectangular assignment with Double.POSITIVE_INFINITY forbidden edges. */
public class HungarianAlgorithm {
    private static final double FORBIDDEN = 1.0e15;
    private final double[][] costs;
    private final int rows;
    private final int cols;

    public HungarianAlgorithm(double[][] costMatrix) {
        if (costMatrix == null || costMatrix.length == 0 || costMatrix[0] == null
                || costMatrix[0].length == 0) {
            throw new IllegalArgumentException("Cost matrix must have at least one row and one column.");
        }
        rows = costMatrix.length;
        cols = costMatrix[0].length;
        costs = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            if (costMatrix[i] == null || costMatrix[i].length != cols) {
                throw new IllegalArgumentException("Cost matrix must be rectangular.");
            }
            for (int j = 0; j < cols; j++) {
                double cost = costMatrix[i][j];
                if (Double.isNaN(cost) || cost == Double.NEGATIVE_INFINITY) {
                    throw new IllegalArgumentException("Costs must be finite or positive infinity.");
                }
                costs[i][j] = cost;
            }
        }
    }

    /** Returns a column index per row, or -1 when a row has no feasible assignment. */
    public int[] execute() {
        int size = rows + cols;
        double maxFinite = 1.0;
        for (double[] row : costs) {
            for (double value : row) if (Double.isFinite(value)) maxFinite = Math.max(maxFinite, Math.abs(value));
        }
        double unassignedCost = Math.min(FORBIDDEN / 4.0, maxFinite * (size + 1.0) + 1.0);
        double[][] square = new double[size + 1][size + 1];
        for (int i = 1; i <= size; i++) {
            for (int j = 1; j <= size; j++) {
                if (i <= rows && j <= cols) {
                    square[i][j] = Double.isFinite(costs[i - 1][j - 1]) ? costs[i - 1][j - 1] : FORBIDDEN;
                } else if (i <= rows) {
                    square[i][j] = unassignedCost;
                } else {
                    square[i][j] = 0.0;
                }
            }
        }

        // Potentials-based O(n^3) Hungarian algorithm for a square matrix.
        double[] u = new double[size + 1];
        double[] v = new double[size + 1];
        int[] p = new int[size + 1];
        int[] way = new int[size + 1];
        for (int i = 1; i <= size; i++) {
            p[0] = i;
            double[] min = new double[size + 1];
            Arrays.fill(min, Double.POSITIVE_INFINITY);
            boolean[] used = new boolean[size + 1];
            int j0 = 0;
            do {
                used[j0] = true;
                int i0 = p[j0];
                double delta = Double.POSITIVE_INFINITY;
                int j1 = 0;
                for (int j = 1; j <= size; j++) {
                    if (used[j]) continue;
                    double cur = square[i0][j] - u[i0] - v[j];
                    if (cur < min[j]) { min[j] = cur; way[j] = j0; }
                    if (min[j] < delta) { delta = min[j]; j1 = j; }
                }
                for (int j = 0; j <= size; j++) {
                    if (used[j]) { u[p[j]] += delta; v[j] -= delta; }
                    else min[j] -= delta;
                }
                j0 = j1;
            } while (p[j0] != 0);
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] assignment = new int[rows];
        Arrays.fill(assignment, -1);
        for (int j = 1; j <= size; j++) {
            int i = p[j];
            if (i >= 1 && i <= rows && j <= cols && Double.isFinite(costs[i - 1][j - 1])) {
                assignment[i - 1] = j - 1;
            }
        }
        return assignment;
    }
}
