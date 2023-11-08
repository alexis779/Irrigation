package tech.vineyard.irrigation.optimization.model.arcs;

import tech.vineyard.irrigation.optimization.model.cells.Pipe;

public class PipeArc extends Arc {
    public final Pipe start;
    public final Pipe end;

    public PipeArc(Pipe start, Pipe end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return String.format("%s(%s->%s)", getClass().getSimpleName(), start, end);
    }
}
