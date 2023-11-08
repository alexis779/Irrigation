package tech.vineyard.irrigation.optimization;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.Literal;
import tech.vineyard.irrigation.CellType;
import tech.vineyard.irrigation.Input;
import tech.vineyard.irrigation.Output;
import tech.vineyard.irrigation.optimization.model.Network;
import tech.vineyard.irrigation.optimization.model.arcs.Arc;
import tech.vineyard.irrigation.optimization.model.arcs.PipeArc;
import tech.vineyard.irrigation.optimization.model.arcs.PlantArc;
import tech.vineyard.irrigation.optimization.model.arcs.SourceArc;
import tech.vineyard.irrigation.optimization.model.cells.Cell;
import tech.vineyard.irrigation.optimization.model.cells.Pipe;
import tech.vineyard.irrigation.optimization.model.cells.Plant;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Optimizer {
    private static final int[][] OFFSETS = new int[][] {
            { -1, 0 },
            { 0, 1 },
            { 1, 0 },
            { 0, -1 }
    };

    private final Input input;
    private final int N;

    private final Network network = new Network();

    private final CpSolver cpSolver = new CpSolver();
    private final CpModel cpModel = new CpModel();

    private final Map<String, BoolVar> booleanVars = new HashMap<>();
    private final Map<String, IntVar> intVars = new HashMap<>();

    private final Map<Pipe, BoolVar> isPipe = new HashMap<>();
    private final Map<Pipe, BoolVar> isSprinkler = new HashMap<>();
    private final Map<Pipe, BoolVar> noConnectorsRequired = new HashMap<>();
    private final Map<Pipe, BoolVar> isHorizontal = new HashMap<>();
    private final Map<Pipe, BoolVar> isVertical = new HashMap<>();
    private final Map<Pipe, IntVar> connectors = new HashMap<>();

    private final Map<Arc, IntVar> pipeOutFlow = new HashMap<>();
    private final Map<Arc, BoolVar> isPipeOutFlowPositive = new HashMap<>();

    private final Map<Plant, BoolVar> isDry = new HashMap<>();

    private final BoolVar[][] isPipeOrSource;
    private BoolVar alwaysFalse;


    public Optimizer(Input input) {
        this.input = input;
        N = input.N();

        isPipeOrSource = new BoolVar[N][N];
    }

    public Output solution() throws IOException {
        buildNetwork();
        buildVariables();
        buildModel();
        buildCost();
        optimizeCost();
        new IrrigationGraphSerializer(network, cpSolver, isPipe, isSprinkler, pipeOutFlow)
                .buildGraph();

        boolean[][] p = new boolean[N][N];
        boolean[][] s = new boolean[N][N];

        loadPipeBoolean(p, isPipe);
        loadPipeBoolean(s, isSprinkler);

        return new Output(p, s);
    }

    private void buildNetwork() {
        CellType[][] gridTypes = input.cells();
        Cell[][] grid = new Cell[N][N];

        // create nodes
        IntStream.range(0, N)
                .forEach(i -> IntStream.range(0, N)
                        .forEach(j -> grid[i][j] = network.createCell(gridTypes[i][j], i, j)));

        // create arcs
        // source -> pipe OR pipe -> pipe
        IntStream.range(0, N)
                .forEach(i -> IntStream.range(0, N)
                        .filter(j -> gridTypes[i][j] == CellType.SOURCE || gridTypes[i][j] == CellType.EMPTY)
                        .forEach(j -> Arrays.stream(OFFSETS)
                                .filter(n -> validNeighbor(i + n[0], j + n[1]) &&
                                             gridTypes[i + n[0]][j + n[1]] == CellType.EMPTY)
                                .forEach(n -> network.createArc(grid[i][j], grid[i + n[0]][j + n[1]]))));
        // pipe -> plant
        IntStream.range(0, N)
                .forEach(i -> IntStream.range(0, N)
                        .filter(j -> gridTypes[i][j] == CellType.PLANT)
                        .forEach(j -> IntStream.range(0, N)
                                .forEach(i2 -> IntStream.range(0, N)
                                        .filter(j2 -> gridTypes[i2][j2] == CellType.EMPTY &&
                                                cover(i, j, i2, j2))
                                        .forEach(j2 -> network.createArc(grid[i2][j2], grid[i][j])))));
    }

    private void buildVariables() {
        Loader.loadNativeLibraries();
        alwaysFalse = newBoolVar(alwaysFalseVariable());

        IntStream.range(0, N)
                .forEach(i -> IntStream.range(0, N)
                        .forEach(j -> isPipeOrSource[i][j] = newBoolVar(isPipeOrSourceVariable(i, j))));

        network.pipeAdjacency
                .keySet()
                .forEach(pipe -> {
                    isPipe.put(pipe, newBoolVar(isPipeVariable(pipe)));
                    isSprinkler.put(pipe, newBoolVar(isSprinklerVariable(pipe)));
                    noConnectorsRequired.put(pipe, newBoolVar(noConnectorsRequiredVariable(pipe)));
                    isHorizontal.put(pipe, newBoolVar(isHorizontalVariable(pipe)));
                    isVertical.put(pipe, newBoolVar(isVerticalVariable(pipe)));
                    connectors.put(pipe, newIntVar(0, 4, connectorVariable(pipe)));
                });

        network.plantAdjacency
                .keySet()
                .forEach(plant -> isDry.put(plant, newBoolVar(isDryVariable(plant))));

        int plants = network.plantAdjacency
                .size();

        network.sourceAdjacency
                .values()
                .stream()
                .flatMap(List::stream)
                .forEach(sourceArc -> {
                    pipeOutFlow.put(sourceArc, newIntVar(-plants, 0, flowVariable(sourceArc)));
                    isPipeOutFlowPositive.put(sourceArc, newBoolVar(isFlowPositiveVariable(sourceArc)));
                });

        network.pipeAdjacency
                .values()
                .stream()
                .flatMap(List::stream)
                .forEach(pipeArc -> {
                    pipeOutFlow.put(pipeArc, newIntVar(-plants, plants, flowVariable(pipeArc)));
                    isPipeOutFlowPositive.put(pipeArc, newBoolVar(isFlowPositiveVariable(pipeArc)));
                });

        network.plantAdjacency
                .values()
                .stream()
                .flatMap(List::stream)
                .forEach(plantArc -> {
                    pipeOutFlow.put(plantArc, newIntVar(0, 1, flowVariable(plantArc)));
                    isPipeOutFlowPositive.put(plantArc, newBoolVar(isFlowPositiveVariable(plantArc)));
                });
    }

    private void buildModel() {
        cpModel.addEquality(alwaysFalse, 0);

        network.sourceAdjacency
                .keySet()
                .forEach(source -> cpModel.addEquality(isPipeOrSource[source.i()][source.j()], 1));

        network.pipeAdjacency
                .keySet()
                .forEach(pipe -> cpModel.addEquality(isPipeOrSource[pipe.i()][pipe.j()], isPipe.get(pipe)));

        network.sourceAdjacency
                .values()
                .stream()
                .flatMap(List::stream)
                .forEach(arc ->
                        // the flow from source to pipe is >= 0
                        cpModel.addLessOrEqual(pipeOutFlow.get(arc), 0)
                );

        // the sum of the source flows is <= plants
        IntVar[] sourceFlows = network.sourceAdjacency
                .values()
                .stream()
                .flatMap(List::stream)
                .map(pipeOutFlow::get)
                .toArray(IntVar[]::new);
        cpModel.addGreaterOrEqual(LinearExpr.sum(sourceFlows), -network.plantAdjacency.size());

        pipeOutFlow.forEach((arc, arcFlow) -> {
                // flow is >= 0 <= isFlowPositive is true
                cpModel.addGreaterOrEqual(arcFlow, 0)
                        .onlyEnforceIf(isPipeOutFlowPositive.get(arc));
        });

        network.pipeAdjacency
                .forEach((pipe, pipeArcs) -> {
                    List<Arc> allPipeArcs = Stream.concat(
                                pipeArcs.stream(),
                                Stream.concat(
                                        network.sourceArcs(pipe).stream(),
                                        network.plantArcs(pipe).stream()))
                            .collect(Collectors.toList());

                    // the sum of the flows out of the pipe is 0
                    LinearArgument[] outPipeFlow = allPipeArcs
                            .stream()
                            .map(pipeOutFlow::get)
                            .toArray(LinearArgument[]::new);

                    LinearExpr flowSum = LinearExpr.sum(outPipeFlow);
                    cpModel.addEquality(flowSum, 0);

                    // at most one flow out of the pipe is < 0
                    Literal[] negativeFlow = allPipeArcs.stream()
                            .map(isPipeOutFlowPositive::get)
                            .map(BoolVar::not)
                            .toArray(Literal[]::new);
                    LinearExpr negativeFlowSum = LinearExpr.sum(negativeFlow);
                    cpModel.addLessOrEqual(negativeFlowSum, 1);

                    // there is non 0 flow <=> pipe is present
                    cpModel.addGreaterThan(negativeFlowSum, 0)
                            .onlyEnforceIf(isPipe.get(pipe));
                    cpModel.addEquality(negativeFlowSum, 0)
                            .onlyEnforceIf(isPipe.get(pipe).not());

                    // the reverse arc flow is the opposite of the direct arc flow
                    pipeArcs.forEach(pipeArc -> cpModel.addEquality(LinearExpr.newBuilder()
                                    .addTerm(this.pipeOutFlow.get(pipeArc), 1)
                                    .addTerm(this.pipeOutFlow.get(network.reverseArc(pipeArc)), 1)
                                    .build(), 0));

                    // number of pipe connectors
                    IntVar pipeConnectors = connectors.get(pipe);
                    BoolVar pipeNoConnectorsRequired = noConnectorsRequired.get(pipe);

                    BoolVar pipeIsHorizontal = isHorizontal.get(pipe);
                    BoolVar pipeIsVertical = isVertical.get(pipe);

                    BoolVar[] horizontalPipeOrSource = Arrays.stream(OFFSETS)
                            .filter(offset -> offset[1] == 0)
                            .map(offset -> isPipeOrSource(pipe.i() + offset[0], pipe.j() + offset[1]))
                            .toArray(BoolVar[]::new);

                    BoolVar[] verticalPipeOrSource = Arrays.stream(OFFSETS)
                            .filter(offset -> offset[0] == 0)
                            .map(offset -> isPipeOrSource(pipe.i() + offset[0], pipe.j() + offset[1]))
                            .toArray(BoolVar[]::new);

                    Literal[] horizontalQuery = Stream.concat(
                            Arrays.stream(horizontalPipeOrSource),
                            Arrays.stream(verticalPipeOrSource)
                                    .map(BoolVar::not)
                    ).toArray(Literal[]::new);
                    cpModel.addBoolAnd(horizontalQuery)
                            .onlyEnforceIf(pipeIsHorizontal);

                    Literal[] verticalQuery = Stream.concat(
                            Arrays.stream(verticalPipeOrSource),
                            Arrays.stream(horizontalPipeOrSource)
                                    .map(BoolVar::not)
                    ).toArray(Literal[]::new);
                    cpModel.addBoolAnd(verticalQuery)
                            .onlyEnforceIf(pipeIsVertical);

                    // horizontal || vertical <= pipeNoConnectorsRequired
                    cpModel.addBoolOr(new Literal[] {
                            pipeIsHorizontal,
                            pipeIsVertical
                    }).onlyEnforceIf(pipeNoConnectorsRequired);

                    BoolVar[] neighborPipes = pipeArcs
                            .stream()
                            .map(pipArc -> isPipe.get(pipArc.end))
                            .toArray(BoolVar[]::new);
                    cpModel.addEquality(pipeConnectors, LinearExpr.sum(neighborPipes))
                            .onlyEnforceIf(pipeNoConnectorsRequired.not());
                    cpModel.addEquality(pipeConnectors, 0)
                            .onlyEnforceIf(pipeNoConnectorsRequired);
                });

        network.plantAdjacency
                .forEach((plant, plantArcs) -> {
                    plantArcs.forEach(arc -> {
                                Pipe pipe = arc.start;

                                // the flow from pipe to plant is >= 0 <= sprinkler is on
                                cpModel.addGreaterOrEqual(pipeOutFlow.get(arc), 0)
                                        .onlyEnforceIf(isSprinkler.get(pipe));

                                // 0 flow from pipe to plant <= sprinkler is off
                                cpModel.addEquality(pipeOutFlow.get(arc), 0)
                                        .onlyEnforceIf(isSprinkler.get(pipe).not());
                            });

                    // the sum of the flows into the plant is > 0 <= plant is not dry
                    IntVar[] plantFlow = plantArcs.stream()
                            .map(pipeOutFlow::get)
                            .toArray(IntVar[]::new);
                    LinearExpr flowSum = LinearExpr.sum(plantFlow);
                    cpModel.addGreaterThan(flowSum, 0)
                            .onlyEnforceIf(isDry.get(plant).not());
                });

        // sprinkler off <= pipe off
        network.pipeAdjacency
                .keySet()
                .forEach(pipe -> cpModel.addEquality(isSprinkler.get(pipe), 0)
                        .onlyEnforceIf(isPipe.get(pipe).not()));
    }

    private void buildCost() {
        LinearExpr pipeSum = LinearExpr.sum(isPipe.values()
                .stream()
                .toArray(BoolVar[]::new));

        LinearExpr sprinklerSum = LinearExpr.sum(isSprinkler.values()
                .stream()
                .toArray(BoolVar[]::new));

        LinearExpr connectorSum = LinearExpr.sum(connectors.values()
                .stream()
                .toArray(IntVar[]::new));

        LinearExpr drySum = LinearExpr.sum(isDry.values()
                .stream()
                .toArray(BoolVar[]::new));

        // cost variable
        IntVar cost = newIntVar(0, maxCost(), costVariable());
        LinearExpr costExpression = LinearExpr.newBuilder()
                .addTerm(pipeSum, input.P())
                .addTerm(sprinklerSum, input.T())
                .addTerm(connectorSum, input.C())
                .addTerm(drySum, N*N)
                .build();

        cpModel.addEquality(cost, costExpression);
    }

    private int optimizeCost() {
        IntVar cost = getIntVar(costVariable());

        cpModel.minimize(cost);
        CpSolverStatus status = cpSolver.solve(cpModel);
        log(String.format("Solution status %s", status));

        if (status != CpSolverStatus.OPTIMAL) {
            throw new RuntimeException("Can not find optimal solution");
        }

        int optimalCost = (int) cpSolver.value(cost);
        log(String.format("Cost is %d", optimalCost));
        return optimalCost;
    }

    private BoolVar isPipeOrSource(int i, int j) {
        if (! validNeighbor(i, j)) {
            return alwaysFalse;
        }

        return isPipeOrSource[i][j];
    }

    private BoolVar newBoolVar(String name) {
        BoolVar boolVar = cpModel.newBoolVar(name);
        booleanVars.put(name, boolVar);
        return boolVar;
    }

    private IntVar newIntVar(long l, long u, String name) {
        IntVar intVar = cpModel.newIntVar(l, u, name);
        intVars.put(name, intVar);
        return intVar;
    }

    private BoolVar getBoolVar(String name) {
        return booleanVars.get(name);
    }

    private IntVar getIntVar(String name) {
        return intVars.get(name);
    }

    private void loadPipeBoolean(boolean[][] b, Map<Pipe, BoolVar> pipeVars) {
        pipeVars.forEach((pipe, enabled) ->
                b[pipe.i()][pipe.j()] = cpSolver.booleanValue(enabled));
    }

    private int maxCost() {
        return N*N * (input.P() + input.T() + OFFSETS.length * input.C() + isDry.size());
    }

    private void log(String message) {
        System.err.println(message);
    }

    private String alwaysFalseVariable() {
        return "alwaysFalse";
    }

    private String costVariable() {
        return "cost";
    }

    private String isPipeVariable(Pipe pipe) {
        return String.format("isPipe_%d_%d", pipe.i(), pipe.j());
    }

    private String isSprinklerVariable(Pipe pipe) {
        return String.format("isSprinkler_%d_%d", pipe.i(), pipe.j());
    }

    private String noConnectorsRequiredVariable(Pipe pipe) {
        return String.format("noConnectorsRequired_%d_%d", pipe.i(), pipe.j());
    }

    private String isVerticalVariable(Pipe pipe) {
        return String.format("isVertical_%d_%d", pipe.i(), pipe.j());
    }

    private String isHorizontalVariable(Pipe pipe) {
        return String.format("isHorizontal_%d_%d", pipe.i(), pipe.j());
    }

    private String connectorVariable(Pipe pipe) {
        return String.format("connector_%d_%d", pipe.i(), pipe.j());
    }

    private String isPipeOrSourceVariable(int i, int j) {
        return String.format("isPipeOrSource_%d_%d", i, j);
    }

    private String isDryVariable(Plant plant) {
        return String.format("isDry_%d_%d", plant.i(), plant.j());
    }

    private String flowVariable(SourceArc sourceArc) {
        return flowVariable(sourceArc.start.i(), sourceArc.start.j(), sourceArc.end.i(), sourceArc.end.j());
    }

    private String flowVariable(PipeArc pipeArc) {
        return flowVariable(pipeArc.start.i(), pipeArc.start.j(), pipeArc.end.i(), pipeArc.end.j());
    }

    private String isFlowPositiveVariable(SourceArc sourceArc) {
        return isFlowPositiveVariable(sourceArc.start.i(), sourceArc.start.j(), sourceArc.end.i(), sourceArc.end.j());
    }

    private String isFlowPositiveVariable(PipeArc pipeArc) {
        return isFlowPositiveVariable(pipeArc.start.i(), pipeArc.start.j(), pipeArc.end.i(), pipeArc.end.j());
    }

    private String isFlowPositiveVariable(PlantArc plantArc) {
        return isFlowPositiveVariable(plantArc.start.i(), plantArc.start.j(), plantArc.end.i(), plantArc.end.j());
    }

    private String flowVariable(PlantArc plantArc) {
        return flowVariable(plantArc.start.i(), plantArc.start.j(), plantArc.end.i(), plantArc.end.j());
    }

    private String flowVariable(int i1, int j1, int i2, int j2) {
        return String.format("flow_%d_%d_%d_%d", i1, j1, i2, j2);
    }

    private String isFlowPositiveVariable(int i1, int j1, int i2, int j2) {
        return String.format("isFlowPositive_%d_%d_%d_%d", i1, j1, i2, j2);
    }

    private boolean cover(int i, int j, int i2, int j2) {
        int dx = i - i2;
        int dy = j - j2;
        int Z = input.Z();
        return dx*dx + dy*dy <= Z*Z;
    }

    private boolean validNeighbor(int i, int j) {
        return 0 <= i && i < N && 0 <= j && j < N;
    }
}
