package tech.vineyard.irrigation.optimization.viz;

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Serialize the graph in .dot format for Graphviz library.
 */
public class GraphvizSerializer implements GraphSerializer {

    private final PrintWriter printWriter;

    public GraphvizSerializer(OutputStream outputStream) {
        printWriter = new PrintWriter(outputStream);
    }

    @Override
    public void open() {
        printWriter.println("digraph G {");
    }

    @Override
    public void addNode(Node node) {
        printWriter.println(String.format("  \"%s\" [label=\"%s\" shape=\"%s\" pos=\"%d,%d!\" style=\"filled\" fillcolor=\"%s\"];",
                node.id, node.label, node.shape.value, node.position.i, node.position.j, node.color));
    }

    @Override
    public void addEdge(Edge edge) {
        printWriter.println(String.format("  \"%s\" -> \"%s\" [label=\"%s\" style=\"%s\" arrowsize=\"0.5\"];",
                edge.start, edge.end, edge.label, edge.style.value));
    }

    @Override
    public void close() {
        printWriter.println("}");
        printWriter.close();
    }
}
