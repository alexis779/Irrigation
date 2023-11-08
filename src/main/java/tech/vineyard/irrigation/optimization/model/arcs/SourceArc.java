package tech.vineyard.irrigation.optimization.model.arcs;

import tech.vineyard.irrigation.optimization.model.cells.Pipe;
import tech.vineyard.irrigation.optimization.model.cells.Source;

public class SourceArc extends Arc {
    public final Source start;
    public final Pipe end;

    public SourceArc(Source start, Pipe end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return String.format("%s(%s->%s)", getClass().getSimpleName(), start, end);
    }
}
