package tech.vineyard.irrigation.optimization.viz;

public class Node {
    public final String id;
    public final String label;
    public final NodeShape shape;
    public final Position position;
    public final String color;

    public Node(String id, String label, NodeShape shape, Position position, String color) {
        this.id = id;
        this.label = label;
        this.shape = shape;
        this.position = position;
        this.color = color;
    }
}
