package tech.vineyard.irrigation.optimization;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.IntVar;
import tech.vineyard.irrigation.optimization.model.Network;
import tech.vineyard.irrigation.optimization.model.arcs.Arc;
import tech.vineyard.irrigation.optimization.model.arcs.PipeArc;
import tech.vineyard.irrigation.optimization.model.arcs.PlantArc;
import tech.vineyard.irrigation.optimization.model.arcs.SourceArc;
import tech.vineyard.irrigation.optimization.model.cells.Cell;
import tech.vineyard.irrigation.optimization.model.cells.Pipe;
import tech.vineyard.irrigation.optimization.model.cells.Plant;
import tech.vineyard.irrigation.optimization.model.cells.Source;
import tech.vineyard.irrigation.optimization.viz.Edge;
import tech.vineyard.irrigation.optimization.viz.EdgeStyle;
import tech.vineyard.irrigation.optimization.viz.GraphSerializer;
import tech.vineyard.irrigation.optimization.viz.GraphvizSerializer;
import tech.vineyard.irrigation.optimization.viz.Node;
import tech.vineyard.irrigation.optimization.viz.NodeShape;
import tech.vineyard.irrigation.optimization.viz.Position;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class IrrigationGraphSerializer {

    private final Network network;
    private final CpSolver cpSolver;

    private final Map<Pipe, BoolVar> isPipe;
    private final Map<Pipe, BoolVar> isSprinkler;

    private final Map<Arc, IntVar> pipeOutFlow;

    public IrrigationGraphSerializer(Network network, CpSolver cpSolver, Map<Pipe, BoolVar> isPipe, Map<Pipe, BoolVar> isSprinkler, Map<Arc, IntVar> pipeOutFlow) {
        this.network = network;
        this.cpSolver = cpSolver;
        this.isPipe = isPipe;
        this.isSprinkler = isSprinkler;
        this.pipeOutFlow = pipeOutFlow;
    }

    public void buildGraph() throws IOException {
        GraphSerializer graphSerializer = new GraphvizSerializer(Files.newOutputStream(Paths.get("irrigation.dot")));

        graphSerializer.open();

        network.sourceAdjacency
                .forEach(((source, sourceArcs) -> {
                    graphSerializer.addNode(node(source));
                    sourceArcs.forEach(sourceArc -> graphSerializer.addEdge(edge(sourceArc)));
                }));
        network.pipeAdjacency
                .forEach(((pipe, pipeArcs) -> {
                    graphSerializer.addNode(node(pipe));
                    pipeArcs.forEach(pipeArc -> graphSerializer.addEdge(edge(pipeArc)));
                }));

        network.plantAdjacency
                .forEach(((plant, plantArcs) -> {
                    graphSerializer.addNode(node(plant));
                    plantArcs.forEach(plantArc -> graphSerializer.addEdge(edge(plantArc)));
                }));

        graphSerializer.close();
    }

    private Node node(Source source) {
        return new Node(id(source), label(source), NodeShape.DIAMOND, new Position(source.i(), source.j()), "deepskyblue3");
    }

    private Node node(Pipe pipe) {
        return new Node(id(pipe), label(pipe), nodeShape(pipe), new Position(pipe.i(), pipe.j()), "deepskyblue2");
    }

    private Node node(Plant plant) {
        return new Node(id(plant), label(plant), NodeShape.CIRCLE, new Position(plant.i(), plant.j()), "deepskyblue1");
    }

    private NodeShape nodeShape(Pipe pipe) {
        boolean pipeEnabled = cpSolver.booleanValue(isPipe.get(pipe));
        if (!pipeEnabled) {
            return NodeShape.POINT;
        }

        boolean sprinklerEnabled = cpSolver.booleanValue(isSprinkler.get(pipe));
        if (!sprinklerEnabled) {
            return NodeShape.SQUARE;
        }

        return NodeShape.BOX;
    }

    private EdgeStyle edgeStyle(int flow) {
        if (flow <= 0) {
            return EdgeStyle.INVISIBLE;
        }

        return EdgeStyle.SOLID;
    }

    private Edge edge(SourceArc sourceArc) {
        int flow = (int) -cpSolver.value(pipeOutFlow.get(sourceArc));
        return new Edge(id(sourceArc.start), id(sourceArc.end), label(flow), edgeStyle(flow));
    }

    private Edge edge(PipeArc pipeArc) {
        int flow = (int) cpSolver.value(pipeOutFlow.get(pipeArc));
        return new Edge(id(pipeArc.start), id(pipeArc.end), label(flow), edgeStyle(flow));
    }

    private Edge edge(PlantArc plantArc) {
        int flow = (int) cpSolver.value(pipeOutFlow.get(plantArc));
        return new Edge(id(plantArc.start), id(plantArc.end), label(flow), edgeStyle(flow));
    }

    private String label(int flow) {
        return String.format("%d", flow);
    }

    private String label(Cell source) {
        return id(source);
    }

    private String id(Cell source) {
        return String.format("%d_%d", source.i(), source.j());
    }

}
