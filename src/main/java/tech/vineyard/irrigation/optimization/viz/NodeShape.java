package tech.vineyard.irrigation.optimization.viz;

/**
 * https://graphviz.org/doc/info/shapes.html
 */
public enum NodeShape {
    OVAL("oval"),
    BOX("box"),
    CIRCLE("circle"),
    EGG("egg"),
    POINT("point"),
    SQUARE("square"),
    TRIANGLE("triangle"),
    PENTAGON("pentagon"),
    HOUSE("house"),
    DIAMOND("diamond");

    public final String value;
    NodeShape(String value) {
        this.value = value;
    }
}
