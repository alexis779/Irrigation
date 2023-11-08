package tech.vineyard.irrigation.optimization.model.arcs;

import tech.vineyard.irrigation.optimization.model.cells.Pipe;
import tech.vineyard.irrigation.optimization.model.cells.Plant;

public class PlantArc extends Arc {
    public final Pipe start;
    public final Plant end;

    public PlantArc(Pipe start, Plant end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return String.format("%s(%s->%s)", getClass().getSimpleName(), start, end);
    }
}
