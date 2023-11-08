package tech.vineyard.irrigation.optimization.viz;

public interface GraphSerializer {
    void open();
    void addNode(Node node);
    void addEdge(Edge edge);
    void close();
}
