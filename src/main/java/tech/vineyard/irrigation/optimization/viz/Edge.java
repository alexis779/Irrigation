package tech.vineyard.irrigation.optimization.viz;

public class Edge {
    public final String start;
    public final String end;
    public final String label;
    public final EdgeStyle style;

    public Edge(String start, String end, String label, EdgeStyle style) {
        this.start = start;
        this.end = end;
        this.label = label;
        this.style = style;
    }
}
