package tech.vineyard.irrigation.optimization.viz;

/**
 * https://graphviz.org/docs/attr-types/style/
 */
public enum EdgeStyle {
    SOLID("solid"),
    DOTTED("dotted"),
    INVISIBLE("invis"),
    DASHED("dashed");

    public final String value;
    EdgeStyle(String value) {
        this.value = value;
    }
}
