package tech.vineyard.irrigation;

public class Input {
    /**
     * grid size
     */
    private final int N;

    /**
     * connector cost
     */
    private final int C;

    /**
     * pipe cost
     */
    private final int P;

    /**
     * sprinkler cost
     */
    private final int T;

    /**
     * sprinkler radius
     */
    private final int Z;

    /**
     * Grid definition
     */
    private final CellType[][] cellTypes;

    public Input(int N, int C, int P, int T, int Z, CellType[][] cellTypes) {
        this.N = N;
        this.C = C;
        this.P = P;
        this.T = T;
        this.Z = Z;
        this.cellTypes = cellTypes;
    }

    public int N() {
        return N;
    }

    public int P() {
        return P;
    }

    public int T() {
        return T;
    }

    public int C() {
        return C;
    }
    public int Z() {
        return Z;
    }

    public CellType[][] cells() {
        return cellTypes;
    }
}
