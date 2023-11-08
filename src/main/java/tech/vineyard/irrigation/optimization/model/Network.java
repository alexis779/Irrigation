package tech.vineyard.irrigation.optimization.model;

import tech.vineyard.irrigation.CellType;
import tech.vineyard.irrigation.optimization.model.arcs.PipeArc;
import tech.vineyard.irrigation.optimization.model.arcs.PlantArc;
import tech.vineyard.irrigation.optimization.model.arcs.SourceArc;
import tech.vineyard.irrigation.optimization.model.cells.Cell;
import tech.vineyard.irrigation.optimization.model.cells.Pipe;
import tech.vineyard.irrigation.optimization.model.cells.Plant;
import tech.vineyard.irrigation.optimization.model.cells.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Network {
    /**
     * Source
     */
    public final Map<Source, List<SourceArc>> sourceAdjacency = new HashMap<>();

    /**
     * Pipe
     */
    public final Map<Pipe, List<PipeArc>> pipeAdjacency = new HashMap<>();

    /**
     * Sink
     */
    public final Map<Plant, List<PlantArc>> plantAdjacency = new HashMap<>();

    public final Map<Pipe, List<SourceArc>> pipeSourceAdjacency = new HashMap<>();
    public final Map<Pipe, List<PlantArc>> pipePlantAdjacency = new HashMap<>();

    public Cell createCell(CellType cellType, int i, int j) {
        switch (cellType) {
            case SOURCE:
                return addSource(new Source(i, j));
            case PLANT:
                return addPlant(new Plant(i, j));
            case EMPTY:
                return addPipe(new Pipe(i, j));
            default:
                return null;
        }
    }

    public void createArc(Cell start, Cell end) {
        if (start instanceof Source && end instanceof Pipe) {
            addSourcePipe((Source) start, (Pipe) end);
        } else if (start instanceof Pipe && end instanceof Pipe) {
            addPipePipe((Pipe) start, (Pipe) end);
        } else if (start instanceof Pipe && end instanceof Plant) {
            addPipePlant((Pipe) start, (Plant) end);
        }
    }

    private Source addSource(Source source) {
        sourceAdjacency.put(source, new ArrayList<>());
        return source;
    }

    private Pipe addPipe(Pipe pipe) {
        pipeAdjacency.put(pipe, new ArrayList<>());
        pipeSourceAdjacency.put(pipe, new ArrayList<>());
        pipePlantAdjacency.put(pipe, new ArrayList<>());
        return pipe;
    }

    private Plant addPlant(Plant plant) {
        plantAdjacency.put(plant, new ArrayList<>());
        return plant;
    }

    private void addSourcePipe(Source start, Pipe end) {
        SourceArc sourceArc = new SourceArc(start, end);
        sourceAdjacency.get(start).add(sourceArc);
        pipeSourceAdjacency.get(end).add(sourceArc);
    }

    private void addPipePipe(Pipe start, Pipe end) {
        pipeAdjacency.get(start).add(new PipeArc(start, end));
    }

    private void addPipePlant(Pipe start, Plant end) {
        PlantArc plantArc = new PlantArc(start, end);
        plantAdjacency.get(end).add(plantArc);
        pipePlantAdjacency.get(start).add(plantArc);
    }

    public PipeArc reverseArc(PipeArc pipeArc) {
        return pipeAdjacency.get(pipeArc.end)
                .stream()
                .filter(reverseArc -> reverseArc.end.equals(pipeArc.start))
                .findAny()
                .get();
    }

    public List<SourceArc> sourceArcs(Pipe pipe) {
        return pipeSourceAdjacency.get(pipe);
    }

    public List<PlantArc> plantArcs(Pipe pipe) {
        return pipePlantAdjacency.get(pipe);
    }
}
