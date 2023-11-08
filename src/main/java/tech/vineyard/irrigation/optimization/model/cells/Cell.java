package tech.vineyard.irrigation.optimization.model.cells;

public abstract class Cell {
    private final int i;
    private final int j;

    Cell(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public int i() {
        return i;
    }

    public int j() {
        return j;
    }

    @Override
    public String toString() {
        return String.format("%s(%d,%d)", getClass().getSimpleName(), i, j);
    }
}
